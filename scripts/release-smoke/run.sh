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
#   ./scripts/release-smoke/run.sh                 # isolated, tests gc.version=2.4.1
#   ./scripts/release-smoke/run.sh --version 2.0.1 # test a different published version
#   ./scripts/release-smoke/run.sh --warm          # keep everything cached (fast dev iteration)
#   ./scripts/release-smoke/run.sh --staged-repo <dir>
#       # before upload: resolve the GraphCompose coordinates from <dir>, a Maven
#       # repository-layout directory such as the unzipped central-bundle.zip, and
#       # everything else from Central. The version defaults to the single version
#       # staged there. After each scenario every GraphCompose artifact of that
#       # version must have come from <dir> — one resolved from anywhere else (a stale
#       # cache, Central) fails the scenario, so a pass proves the staged bytes.
#
set -u

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$HERE/../.." && pwd)"
MVNW="$REPO_ROOT/mvnw"
SETTINGS="$HERE/settings.xml"

SCENARIOS=(s1-graph-compose s2-core-only s3-core-render-pdf s4-templates s5-testing s6-bundle s7-core-render-pptx s8-core-render-docx s9-cv-templates)
REPO="$REPO_ROOT/target/release-smoke-m2/repo"

# Default version under test: the currently published release. Release smoke must
# test PUBLISHED artifacts — never a -SNAPSHOT.
GC_VERSION="2.4.1"
WARM=0
STAGED=""
STAGED_SET=0
VERSION_SET=0
while [ $# -gt 0 ]; do
  case "$1" in
    --warm) WARM=1; shift ;;
    --version) GC_VERSION="${2:?--version needs a value}"; VERSION_SET=1; shift 2 ;;
    --version=*) GC_VERSION="${1#*=}"; VERSION_SET=1; shift
                 [ -n "$GC_VERSION" ] || { echo "--version needs a value" >&2; exit 2; } ;;
    --staged-repo) STAGED="${2-}"; STAGED_SET=1; shift; [ $# -gt 0 ] && shift ;;
    --staged-repo=*) STAGED="${1#*=}"; STAGED_SET=1; shift ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done
mkdir -p "$REPO"

# An empty value (an unset shell variable) must not quietly turn staged mode off and
# smoke the published default from Central instead.
if [ "$STAGED_SET" = "1" ] && [ -z "$STAGED" ]; then
  echo "--staged-repo needs a directory (got an empty value)" >&2
  exit 2
fi

if [ -n "$STAGED" ]; then
  if [ "$WARM" = "1" ]; then
    # A warm cache can satisfy a coordinate without consulting the staged repo,
    # which is the one thing staged mode exists to rule out.
    echo "--staged-repo cannot be combined with --warm" >&2
    exit 2
  fi
  if [ ! -d "$STAGED/io/github/demchaav" ]; then
    echo "FATAL: $STAGED is not a Maven repository layout holding io/github/demchaav" >&2
    exit 2
  fi
  # Java on Windows needs C:/... — cygpath gives it under Git Bash and Cygwin;
  # elsewhere (Linux, WSL, macOS) the plain absolute path is right.
  STAGED_ABS="$(cd "$STAGED" && pwd)"
  if command -v cygpath >/dev/null 2>&1; then STAGED_ABS="$(cygpath -m "$STAGED_ABS")"; fi
  # The path goes into XML; escape the characters that would break it.
  STAGED_URL_PATH="$(printf '%s' "${STAGED_ABS#/}" | sed -e 's/&/\&amp;/g' -e 's/</\&lt;/g' -e 's/>/\&gt;/g')"
  if [ "$VERSION_SET" = "0" ]; then
    # Version directories only — a maven-metadata.xml beside them is not a version.
    staged_versions="$(cd "$STAGED/io/github/demchaav/graph-compose-core" 2>/dev/null && ls -d -- */ 2>/dev/null | tr -d /)"
    if [ "$(printf '%s\n' "$staged_versions" | grep -c .)" != "1" ]; then
      echo "FATAL: expected exactly one staged graph-compose-core version, found: $staged_versions" >&2
      exit 2
    fi
    GC_VERSION="$staged_versions"
  fi
  # The isolated settings, with one exception carved out of the Central-only mirror:
  # the staged repository, active for every scenario.
  SETTINGS="$REPO_ROOT/target/release-smoke-m2/settings-staged.xml"
  cat > "$SETTINGS" <<EOF
<settings>
  <mirrors>
    <mirror>
      <id>central-only</id>
      <url>https://repo.maven.apache.org/maven2</url>
      <mirrorOf>*,!staged</mirrorOf>
    </mirror>
  </mirrors>
  <profiles>
    <profile>
      <id>staged</id>
      <repositories>
        <repository>
          <id>staged</id>
          <url>file:///${STAGED_URL_PATH}</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>staged</activeProfile>
  </activeProfiles>
</settings>
EOF
fi

# Staged mode only. Every file of every train artifact the scenario resolved must
# record the staged repository as its source, and every train artifact must be at
# the version under test — a train module resolved at another version is lockstep
# drift in a staged POM, and it would have come from Central unchecked. fonts and
# emoji are exempt: they version independently and always come from Central. Fails
# closed — a scenario that resolved no train artifact proves nothing.
staged_provenance_ok() {
  local seen=0 bad=0 dir artifact version line
  for dir in "$REPO"/io/github/demchaav/*/*/; do
    [ -d "$dir" ] || continue
    dir="${dir%/}"
    version="${dir##*/}"
    artifact="$(basename "$(dirname "$dir")")"
    case "$artifact" in graph-compose-fonts|graph-compose-emoji) continue ;; esac
    if [ "$version" != "$GC_VERSION" ]; then
      echo "PROVENANCE: $artifact resolved at $version, not the staged $GC_VERSION" >&2
      bad=$((bad + 1))
      continue
    fi
    seen=$((seen + 1))
    if [ ! -f "$dir/_remote.repositories" ]; then
      echo "PROVENANCE: $dir records no source repository" >&2
      bad=$((bad + 1))
      continue
    fi
    while IFS= read -r line || [ -n "$line" ]; do
      line="${line%$'\r'}"
      case "$line" in ''|'#'*) continue ;; esac
      case "$line" in
        *'>staged=') ;;
        *) echo "PROVENANCE: $artifact $version: '$line' was not resolved from the staged repository" >&2
           bad=$((bad + 1)) ;;
      esac
    done < "$dir/_remote.repositories"
  done
  if [ "$seen" = "0" ]; then
    echo "PROVENANCE: no GraphCompose $GC_VERSION artifact was resolved at all" >&2
    return 1
  fi
  [ "$bad" = "0" ]
}

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
  echo "=== SMOKE $s   (version=$GC_VERSION, repo=$REPO, evicted=$([ "$WARM" = "0" ] && echo yes || echo no)${STAGED:+, staged=$STAGED})"
  echo "=================================================================="
  "$MVNW" -B -ntp -s "$SETTINGS" -Dgc.version="$GC_VERSION" \
    -f "$HERE/$s/pom.xml" -Dmaven.repo.local="$REPO" clean verify
  status=$?
  if [ "$status" -eq 0 ] && [ -n "$STAGED" ] && ! staged_provenance_ok; then
    status=1
  fi
  if [ "$status" -eq 0 ]; then
    results+=("$s PASS")
    pass=$((pass + 1))
  else
    results+=("$s FAIL")
    fail=$((fail + 1))
  fi
done

echo ""
echo "===================== RELEASE SMOKE SUMMARY ====================="
echo "version-under-test: $GC_VERSION${STAGED:+ (staged: $STAGED)}"
for r in "${results[@]}"; do
  echo "RESULT $r"
done
echo "SUMMARY {\"version\":\"$GC_VERSION\",\"passed\":$pass,\"failed\":$fail,\"total\":$((pass + fail))}"

[ "$fail" = "0" ]
