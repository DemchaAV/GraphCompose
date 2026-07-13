#!/usr/bin/env bash
#
# Release smoke harness — runs every external consumer project against the
# published GraphCompose coordinates on Maven Central. See README.md.
#
# Isolation model: a dedicated local repository under target/ that never receives
# an `mvn install` of GraphCompose. Before each scenario the GraphCompose
# artifacts (io/github/demchaav/**) are EVICTED, so every scenario must re-resolve
# graph-compose-* from Maven Central — proving the release resolves cleanly with
# no local reactor build behind it. Maven's own plugins and third-party libraries
# (PDFBox, JUnit, ...) are kept cached: re-downloading Maven's core plugins on an
# empty repo is heavy and flaky and tests Maven, not this release.
#
# Usage:
#   ./scripts/release-smoke/run.sh          # evict GraphCompose per scenario (Central-only, isolated)
#   ./scripts/release-smoke/run.sh --warm   # keep everything cached (fast dev iteration)
#
set -u

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$HERE/../.." && pwd)"
MVNW="$REPO_ROOT/mvnw"

SCENARIOS=(s1-graph-compose s2-core-only s3-core-render-pdf s4-templates s5-testing s6-bundle)
REPO="$REPO_ROOT/target/release-smoke-m2/repo"

WARM=0
[ "${1:-}" = "--warm" ] && WARM=1
mkdir -p "$REPO"

pass=0
fail=0
declare -a results

for s in "${SCENARIOS[@]}"; do
  if [ "$WARM" = "0" ]; then
    # Evict only the GraphCompose coordinates so they must come from Central.
    rm -rf "$REPO/io/github/demchaav"
  fi
  echo ""
  echo "=================================================================="
  echo "=== SMOKE $s   (maven.repo.local=$REPO, GraphCompose evicted=$([ "$WARM" = "0" ] && echo yes || echo no))"
  echo "=================================================================="
  "$MVNW" -B -ntp -f "$HERE/$s/pom.xml" -Dmaven.repo.local="$REPO" clean verify
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
for r in "${results[@]}"; do
  echo "RESULT $r"
done
echo "SUMMARY {\"passed\":$pass,\"failed\":$fail,\"total\":$((pass + fail))}"

[ "$fail" = "0" ]
