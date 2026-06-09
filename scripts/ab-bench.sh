#!/usr/bin/env bash
#
# A/B engine-speed comparison between two branches — cross-platform (Linux, macOS,
# Windows Git Bash). Companion to scripts/ab-bench.ps1 (Windows/PowerShell).
#
# It interleaves the two branches (A,B,A,B,...) so laptop thermal drift averages
# out, repeats each branch N times, aggregates each branch's runs to a MEDIAN with
# the cross-platform Java tool, then diffs the two medians. The heavy lifting
# (median, diff, parsing) is done by the already-cross-platform benchmark tools
# (BenchmarkMedianTool, BenchmarkDiffTool) so this script stays thin and portable.
#
# Scope: the current-speed suite — per-scenario latency (avg/p95/docs-per-sec/heap)
# plus parallel throughput. That is the primary "did branch B get faster or slower
# than branch A" signal. The PowerShell ab-bench.ps1 additionally reports
# scalability / stress / comparative on Windows.
#
# Both branches must (a) be checkout-able refs and (b) carry the benchmark tooling
# (any recent main/develop does). The working tree must have no uncommitted TRACKED
# changes (branch switching would carry them).
#
# Usage:
#   ./scripts/ab-bench.sh                                   # main vs develop, 3 repeats
#   ./scripts/ab-bench.sh -a main -b develop -r 3
#   ./scripts/ab-bench.sh --branch-a develop --branch-b feature/x --repeat 5 --cooldown 45
#   ./scripts/ab-bench.sh -a main -b origin/anothertree -r 3   # remote-only ref (detached)
#
# Options:
#   -a, --branch-a REF     baseline branch (default: main)
#   -b, --branch-b REF     candidate branch (default: develop); deltas are B relative to A
#   -r, --repeat N         runs per branch, median compared (default: 3; >=2 recommended)
#   -c, --cooldown SEC     pause between runs to shed heat (default: 45)
#   -p, --profile NAME     current-speed profile: full | smoke (default: full)
#       --keep-probes      do not move/restore untracked benchmark .java probes
#   -h, --help             show this help

set -euo pipefail

BRANCH_A=main
BRANCH_B=develop
REPEAT=3
COOLDOWN=45
PROFILE=full
KEEP_PROBES=0

die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

while [ $# -gt 0 ]; do
  case "$1" in
    -a|--branch-a) BRANCH_A="$2"; shift 2 ;;
    -b|--branch-b) BRANCH_B="$2"; shift 2 ;;
    -r|--repeat)   REPEAT="$2";   shift 2 ;;
    -c|--cooldown) COOLDOWN="$2"; shift 2 ;;
    -p|--profile)  PROFILE="$2";  shift 2 ;;
    --keep-probes) KEEP_PROBES=1; shift ;;
    -h|--help)     sed -n '2,40p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) die "unknown option: $1 (use --help)" ;;
  esac
done

case "$REPEAT" in ''|*[!0-9]*) die "--repeat must be a positive integer" ;; esac
[ "$REPEAT" -ge 1 ] || die "--repeat must be >= 1"

# Repo root (this script lives in <root>/scripts).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

# Classpath separator: ';' on Windows shells (Git Bash / MSYS / Cygwin), ':' elsewhere.
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) SEP=';' ;;
  *) SEP=':' ;;
esac

MVNW="./mvnw"
[ -x "$MVNW" ] || die "unix maven wrapper not found/executable at $MVNW"
command -v java >/dev/null 2>&1 || die "java not on PATH"
command -v git  >/dev/null 2>&1 || die "git not on PATH"

CS_DIR="$REPO_ROOT/target/benchmarks/current-speed"
OUT_DIR="$REPO_ROOT/target/ab-compare"
mkdir -p "$OUT_DIR"

# Preflight: no uncommitted TRACKED changes (branch switching would carry them).
if [ -n "$(git status --porcelain --untracked-files=no)" ]; then
  git status --porcelain --untracked-files=no >&2
  die "working tree has uncommitted TRACKED changes; commit/stash them before A/B."
fi

# Symbolic branch name, or the commit SHA when started from a detached HEAD,
# so the EXIT trap returns exactly where we began.
START_BRANCH="$(git symbolic-ref -q --short HEAD || git rev-parse HEAD)"
printf 'A/B: %s vs %s | repeats=%s | cooldown=%ss | profile=%s | start=%s\n' \
  "$BRANCH_A" "$BRANCH_B" "$REPEAT" "$COOLDOWN" "$PROFILE" "$START_BRANCH"

# Move untracked benchmark .java aside: probes may reference branch-only symbols and
# break the bench compile on the other branch. Detected via git so new probes are handled.
# Portable temp dir: GNU `mktemp -d` works bare; BSD/macOS needs a template (-t).
STASH_DIR="$(mktemp -d 2>/dev/null || mktemp -d -t ab-bench 2>/dev/null || true)"
[ -n "$STASH_DIR" ] || STASH_DIR="${TMPDIR:-/tmp}/ab-bench.$$"
mkdir -p "$STASH_DIR"
MOVED=()   # entries: "relpath|stashedpath"
if [ "$KEEP_PROBES" -eq 0 ]; then
  while IFS= read -r rel; do
    [ -z "$rel" ] && continue
    [ -f "$rel" ] || continue
    stashed="$STASH_DIR/$(printf '%s' "$rel" | tr '/\\' '__')"
    mv -f "$rel" "$stashed"
    MOVED+=("$rel|$stashed")
  done < <(git status --porcelain --untracked-files=all -- benchmarks \
             | sed -n 's/^?? //p' | grep '\.java$' || true)
  [ "${#MOVED[@]}" -gt 0 ] && printf 'Moved %s untracked benchmark .java aside -> %s\n' "${#MOVED[@]}" "$STASH_DIR"
fi

restore() {
  # Restore probes FIRST (they are untracked, so they survive a checkout) so a
  # checkout hiccup can never strand them, then return to the start branch.
  if [ "$KEEP_PROBES" -eq 0 ] && [ "${#MOVED[@]}" -gt 0 ]; then
    for m in "${MOVED[@]}"; do
      rel="${m%%|*}"; src="${m##*|}"
      if [ -f "$src" ]; then
        mkdir -p "$(dirname "$rel")"
        mv -f "$src" "$rel"
      fi
    done
    printf 'Restored %s probe(s) to working tree\n' "${#MOVED[@]}"
  fi
  rmdir "$STASH_DIR" 2>/dev/null || true
  git checkout -q "$START_BRANCH" 2>/dev/null || true
  printf 'Back on branch %s\n' "$START_BRANCH"
}
trap restore EXIT

CP=""
build_engine_and_classpath() {
  # Install the engine for the current branch so the benchmark runs against IT,
  # then (re)build the benchmark classpath.
  "$MVNW" -B -ntp -DskipTests install -pl . >/dev/null
  "$MVNW" -B -ntp -f benchmarks/pom.xml test-compile dependency:build-classpath \
          -DincludeScope=test -Dmdep.outputFile=target/benchmark.classpath >/dev/null
  CP="benchmarks/target/test-classes${SEP}benchmarks/target/classes${SEP}$(cat benchmarks/target/benchmark.classpath)"
}

run_current_speed() {
  java "-Dgraphcompose.benchmark.profile=$PROFILE" -cp "$CP" com.demcha.compose.CurrentSpeedBenchmark >/dev/null
  # Newest run-*.json without `ls | head` (which SIGPIPEs ls under `set -o pipefail`).
  local newest="" f
  for f in "$CS_DIR"/run-*.json; do
    [ -f "$f" ] || continue
    if [ -z "$newest" ] || [ "$f" -nt "$newest" ]; then newest="$f"; fi
  done
  printf '%s\n' "$newest"
}

A_RUNS=()
B_RUNS=()
rep=1
while [ "$rep" -le "$REPEAT" ]; do
  for br in "$BRANCH_A" "$BRANCH_B"; do
    printf '\n=== repeat %s/%s | branch %s ===\n' "$rep" "$REPEAT" "$br"
    git checkout -q "$br" || die "git checkout $br failed"
    build_engine_and_classpath
    produced="$(run_current_speed)"
    [ -n "$produced" ] || die "no current-speed run produced on $br"
    if [ "$br" = "$BRANCH_A" ]; then A_RUNS+=("$produced"); else B_RUNS+=("$produced"); fi
    printf '  -> %s\n' "$(basename "$produced")"
    # Cooldown unless this was the very last run.
    if ! { [ "$rep" -eq "$REPEAT" ] && [ "$br" = "$BRANCH_B" ]; }; then
      printf '  cooldown %ss...\n' "$COOLDOWN"
      sleep "$COOLDOWN"
    fi
  done
  rep=$((rep + 1))
done

# We are currently on BRANCH_B with its CP set; its compiled benchmark tooling
# (Median/Diff) plus the JSON run files (under target/, untouched by checkout) are
# all we need — no extra install.
resolve_median() {
  # args: out_label run1 [run2 ...]; echoes the median JSON path (or the single run).
  local label="$1"; shift
  if [ "$#" -lt 2 ]; then printf '%s\n' "$1"; return; fi
  local log raw
  log="$(java -cp "$CP" com.demcha.compose.BenchmarkMedianTool current-speed "$@")" \
    || die "median aggregation failed for branch $label"
  printf '%s\n' "$log" >&2
  raw="$(printf '%s\n' "$log" | sed -n 's/.*Saved JSON [a-z]* report to //p' | tr -d '\r')"
  [ -n "$raw" ] || die "could not locate median JSON for branch $label"
  cp -f "$raw" "$OUT_DIR/median-$label.json"
  printf '%s\n' "$OUT_DIR/median-$label.json"
}

printf '\n=== aggregating medians ===\n'
MED_A="$(resolve_median A "${A_RUNS[@]}")"
sleep 1   # cosmetic; median-A is already copied to OUT_DIR above (the real guard)
MED_B="$(resolve_median B "${B_RUNS[@]}")"

printf '\n'
printf '==============================================================================\n'
printf 'DIFF  (%s n=%s  ->  %s n=%s)   [deltas are %s relative to %s; lower=better, docs/s higher=better]\n' \
  "$BRANCH_A" "${#A_RUNS[@]}" "$BRANCH_B" "${#B_RUNS[@]}" "$BRANCH_B" "$BRANCH_A"
printf '==============================================================================\n'
java -cp "$CP" com.demcha.compose.BenchmarkDiffTool "$MED_A" "$MED_B"

printf '\nbaseline (%s) median: %s\ncandidate (%s) median: %s\n' "$BRANCH_A" "$MED_A" "$BRANCH_B" "$MED_B"
if [ "$REPEAT" -lt 2 ]; then
  printf 'NOTE: --repeat 1 compares single runs (noisy). Use --repeat >=3 for a real decision.\n'
fi
printf 'NOTE: laptop results are noisy; treat <~5-10%% deltas as inconclusive.\n'
