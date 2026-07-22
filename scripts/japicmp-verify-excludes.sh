#!/usr/bin/env bash
#
# Controlled verification that the japicmp binary-compatibility gate enforces the
# Stable public surface and excludes the Internal surface exactly as
# docs/api-stability.md defines it (§ 1 tiers, § 4 package map, "Binary-
# compatibility enforcement"). Run it after changing the japicmp <excludes> in
# core/pom.xml, or after moving a type between the Stable and Internal tiers.
#
# It applies one controlled break at a time, runs the gate, checks the outcome,
# and ALWAYS reverts the source (even on interrupt) via a trap. Requires a clean
# working tree for the two touched files. The engine version must be off the
# japicmp baseline (a -SNAPSHOT dev version) or the gate short-circuits.
#
# Expected: a Stable break FAILS the gate; an Internal break does NOT.
#
set -u

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$HERE/.." && pwd)"
cd "$REPO_ROOT"
MVNW="$REPO_ROOT/mvnw"

# A Stable public method (document.style is a Stable package) and an Internal
# public method (document.layout is @Internal). Both are compile-safe to reduce to
# package-private: rgba has no main callers, compile() has no cross-package caller.
STABLE_FILE="core/src/main/java/com/demcha/compose/document/style/DocumentColor.java"
INTERNAL_FILE="core/src/main/java/com/demcha/compose/document/layout/LayoutCompiler.java"

LOGDIR="$REPO_ROOT/target/japicmp-verify"
mkdir -p "$LOGDIR"
MVN_ARGS=(-B -ntp clean -P japicmp -Dmaven.test.skip=true -Djacoco.skip=true verify -pl :graph-compose-core)

revert() { git checkout -- "$STABLE_FILE" "$INTERNAL_FILE" 2>/dev/null || true; }
trap revert EXIT

fail=0

echo "== [1/2] Stable break must FAIL the gate =="
perl -0pi -e 's/    public static DocumentColor rgba/    static DocumentColor rgba/' "$STABLE_FILE"
if "$MVNW" "${MVN_ARGS[@]}" > "$LOGDIR/stable.log" 2>&1; then
  echo "  FAIL: the gate PASSED a Stable-surface break (DocumentColor.rgba)"; fail=1
elif grep -q "DocumentColor.rgba.*METHOD_LESS_ACCESSIBLE" "$LOGDIR/stable.log"; then
  echo "  OK: the gate failed the build (DocumentColor.rgba METHOD_LESS_ACCESSIBLE)"
else
  echo "  FAIL: the gate failed, but not on the rgba break — see $LOGDIR/stable.log"; fail=1
fi
git checkout -- "$STABLE_FILE"

echo "== [2/2] Internal break must NOT fail the gate =="
perl -0pi -e 's/    public LayoutGraph compile\(/    LayoutGraph compile(/' "$INTERNAL_FILE"
if "$MVNW" "${MVN_ARGS[@]}" > "$LOGDIR/internal.log" 2>&1; then
  echo "  OK: the gate passed (LayoutCompiler.compile is excluded via document.layout.**)"
else
  echo "  FAIL: the gate FAILED an Internal-surface break — see $LOGDIR/internal.log"; fail=1
fi
git checkout -- "$INTERNAL_FILE"

echo ""
if [ "$fail" = "0" ]; then
  echo "VERIFY PASS — Stable enforced, Internal excluded."
else
  echo "VERIFY FAIL — see the logs above."
fi
exit "$fail"
