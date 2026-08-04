#!/usr/bin/env bash
#
# Release smoke harness — runs every external consumer project against the
# published GraphCompose coordinates on Maven Central. See README.md.
#
# Isolation model: a dedicated local repository under target/ that never receives
# an `mvn install` of GraphCompose, plus an isolated settings.xml whose mirror
# forces ALL resolution through Maven Central. Before each scenario the GraphCompose
# artifacts (io/github/demchaav/**) are EVICTED — and the harness hard-fails if the
# eviction does not take — so every scenario must re-resolve graph-compose-* from
# Central. Maven's own plugins and third-party libraries stay cached: re-downloading
# Maven's core plugins on an empty repo is heavy, flaky, and tests Maven, not this
# release.
#
# Usage:
#   ./scripts/release-smoke/run.sh                 # isolated, tests gc.version=2.1.1
#   ./scripts/release-smoke/run.sh --version 2.0.1 # test a different published version
#   ./scripts/release-smoke/run.sh --warm          # keep everything cached (fast dev iteration)
#
set -u

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$HERE/../.." && pwd)"
MVNW="$REPO_ROOT/mvnw"
SETTINGS="$HERE/settings.xml"

SCENARIOS=(s1-graph-compose s2-core-only s3-core-render-pdf s4-templates s5-testing s6-bundle s7-core-render-pptx s8-core-render-docx)
REPO="$REPO_ROOT/target/release-smoke-m2/repo"

# Default version under test: the currently published release. Release smoke must
# test PUBLISHED artifacts — never a -SNAPSHOT.
GC_VERSION="2.1.1"
WARM=0
while [ $# -gt 0 ]; do
  case "$1" in
    --warm) WARM=1; shift ;;
    --version) GC_VERSION="${2:?--version needs a value}"; shift 2 ;;
    --version=*) GC_VERSION="${1#*=}"; shift ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done
mkdir -p "$REPO"

pass=0
fail=0
declare -a results

for s in "${SCENARIOS[@]}"; do
  if [ "$WARM" = "0" ]; then
    # Evict only the GraphCompose coordinates so they must come from Central,
    # then hard-fail if the eviction did not take (a stale cache would mask a
    # broken publish).
    gc_dir="$REPO/io/github/demchaav"
    rm -rf "$gc_dir"
    if [ -d "$gc_dir" ]; then
      echo "FATAL: could not remove the GraphCompose cache directory: $gc_dir" >&2
      exit 3
    fi
  fi
  echo ""
  echo "=================================================================="
  echo "=== SMOKE $s   (version=$GC_VERSION, repo=$REPO, evicted=$([ "$WARM" = "0" ] && echo yes || echo no))"
  echo "=================================================================="
  "$MVNW" -B -ntp -s "$SETTINGS" -Dgc.version="$GC_VERSION" \
    -f "$HERE/$s/pom.xml" -Dmaven.repo.local="$REPO" clean verify
  if [ $? -eq 0 ]; then
    results+=("$s PASS")
    pass=$((pass + 1))
  else
    results+=("$s FAIL")
    fail=$((fail + 1))
  fi
done

echo ""
echo "===================== RELEASE SMOKE SUMMARY ====================="
echo "version-under-test: $GC_VERSION"
for r in "${results[@]}"; do
  echo "RESULT $r"
done
echo "SUMMARY {\"version\":\"$GC_VERSION\",\"passed\":$pass,\"failed\":$fail,\"total\":$((pass + fail))}"

[ "$fail" = "0" ]
