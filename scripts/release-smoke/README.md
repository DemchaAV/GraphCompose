# Release smoke — external consumer projects

Standalone Maven projects that consume the **published** GraphCompose
coordinates from Maven Central, proving the modular 2.0 release resolves and
behaves as documented *outside* this repository — no reactor, no local
`mvn install` of GraphCompose beforehand.

These projects are **not** part of the root reactor (the root `pom.xml`
`<modules>` does not list them and each pom has no `<parent>`), so a normal
build never touches them. Run them explicitly with the harness below.

## Scenarios

| Dir | Coordinate(s) under test | Proves |
|---|---|---|
| `s1-graph-compose` | `graph-compose` | the drop-in wrapper renders a PDF out of the box |
| `s2-core-only` | `graph-compose-core` | a lean core throws `MissingBackendException` naming `graph-compose-render-pdf`, and its dependency tree pulls no PDFBox / POI / ZXing / templates / fonts / emoji (enforced by `maven-enforcer` bannedDependencies) |
| `s3-core-render-pdf` | `graph-compose-core` + `graph-compose-render-pdf` | the explicit lean + backend combination renders a PDF |
| `s4-templates` | `graph-compose-templates` | a built-in template composes and renders through the PDF stack |
| `s5-testing` | `graph-compose` + `graph-compose-testing` | the consumer testing helper (`LayoutSnapshotAssertions`) resolves and round-trips a layout snapshot |
| `s6-bundle` | `graph-compose-bundle` | the batteries-included aggregate renders a templated document, exposes the bundled fonts (`DefaultFonts.bundledFontNames()`), and makes the colour-emoji set resolvable (`GraphComposeEmoji.isAvailable()`) |

The "must pull core + render-pdf" (wrapper) and "must pull the documented
aggregate" (bundle) assertions are proven positively: `s1` / `s6` can only
render because the backend and companions were pulled transitively.

## Running

```bash
# Evict the GraphCompose artifacts before each scenario (Central-only, isolated):
./scripts/release-smoke/run.sh

# Fast dev iteration — keep everything cached:
./scripts/release-smoke/run.sh --warm
```

```powershell
pwsh ./scripts/release-smoke/run.ps1          # isolated
pwsh ./scripts/release-smoke/run.ps1 -Warm    # warm
```

The harness uses one dedicated local repository under `target/` that never
receives an `mvn install` of GraphCompose. In the default (isolated) mode it
**evicts the GraphCompose artifacts** (`io/github/demchaav/**`) before each
scenario, so every scenario must re-resolve `graph-compose-*` from Maven Central
— proving the release resolves with no local reactor build behind it. Maven's
own plugins and third-party libraries (PDFBox, JUnit, …) stay cached, because
re-downloading Maven's core plugins onto an empty repository is heavy, flaky,
and tests Maven rather than this release. Classpath isolation between scenarios
(e.g. the lean core never seeing the PDF backend) comes from each project's
declared dependencies and the `s2` enforcer rule, not from the repository state.

The harness prints one `RESULT <scenario> PASS|FAIL` line per project and ends
with a machine-readable `SUMMARY {"passed":N,"failed":M,"total":T}` line. Exit
code is non-zero if any scenario fails.

The version under test is the `gc.version` property in each pom (currently
`2.0.0`). Bump it to smoke-test a later release.
