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
| `s7-core-render-pptx` | `graph-compose-core` + `graph-compose-render-pptx` | the PPTX backend is discovered by format, and `toPptxBytes()` produces a real OPC package — one slide part, 16:9 slide dimensions in EMU, editable text runs, and **no** full-slide picture in vector mode. **Requires 2.1.0+**: the fixed-layout backend and `DocumentPageSize.SLIDE_16_9` do not exist in 2.0.0, so this scenario cannot pass against an earlier version |
| `s8-core-render-docx` | `graph-compose-core` + `graph-compose-render-docx` + `graph-compose-render-pdf` | the semantic Word exporter is on the consumer's compile classpath (it is named directly, not discovered through the ServiceLoader) and `export(new DocxSemanticBackend())` produces a `word/document.xml` carrying the text. The PDF backend is in the set because it is **required**: opening a session resolves a `FontMetricsProvider` and render-pdf is the only artifact that publishes one, while render-docx declares it at test scope only — so core + render-docx alone cannot construct a session |
| `s9-cv-templates` | `graph-compose-bundle` | the profile half of the same templates path: a CV preset (`BoxedSections`) composes a `CvDocument` and renders a PDF. `s4` proves the business half on `graph-compose` + `graph-compose-templates`; this adds the other data model **and** the reason that pair is not enough for it — a CV theme draws in PT Serif, and the bundled Google faces ship in a companion that is versioned independently of the release (`graph-compose-fonts` is at 1.1.0 with the engine at 2.4.0), so the pair compiles and then throws `Bundled font resource not found` at render. The aggregate is the one coordinate carrying them at the release's own version. The calls are the ones the showcase publishes, so an API they use that has not shipped yet fails here rather than in a reader's project |

The two OPC scenarios (`s7`, `s8`) inspect the emitted package with `java.util.zip` rather than
Apache POI. The point is to prove the *published* artifacts work for a consumer who
installed nothing else, so a scenario must not pull a parsing library of its own to make
its assertions pass.

The "must pull core + render-pdf" (wrapper) and "must pull the documented
aggregate" (bundle) assertions are proven positively: `s1` / `s6` can only
render because the backend and companions were pulled transitively.

## Running

```bash
# Evict the GraphCompose artifacts before each scenario (Central-only, isolated):
./scripts/release-smoke/run.sh

# Smoke-test a different published version:
./scripts/release-smoke/run.sh --version 2.0.1

# Fast dev iteration — keep everything cached:
./scripts/release-smoke/run.sh --warm

# Before upload — consume the staged deployment instead of Central:
./scripts/release-smoke/run.sh --staged-repo target/staged-bundle
```

```powershell
pwsh ./scripts/release-smoke/run.ps1                 # isolated
pwsh ./scripts/release-smoke/run.ps1 -Version 2.0.1  # a different published version
pwsh ./scripts/release-smoke/run.ps1 -Warm           # warm
pwsh ./scripts/release-smoke/run.ps1 -StagedRepo target/staged-bundle  # staged
```

### Staged mode (before the upload)

`--staged-repo <dir>` / `-StagedRepo <dir>` runs the same scenarios against a
deployment that has not reached Central yet. `<dir>` is a Maven repository-layout
directory — the unzipped `central-bundle.zip` the release reactor builds (see
[`docs/contributing/release-process.md`](../../docs/contributing/release-process.md),
*Dry-running the Central deployment*). The harness writes a settings file whose
Central-only mirror excludes one repository, `staged`, pointing at `<dir>`; the
GraphCompose coordinates resolve from there and everything else (third-party
libraries, the independently versioned fonts and emoji) from Central. The version
defaults to the single `graph-compose-core` version staged in `<dir>`.

A scenario passes only if, besides its own assertions, every file of every train
artifact it resolved records `staged` as its source in `_remote.repositories`, and
no train artifact was resolved at a version other than the one staged — so a stale
cache or a Central copy cannot stand in for the staged bytes, a staged POM that
pins a sibling at a drifted version fails, and a scenario that resolved no train
artifact fails. `graph-compose-fonts` and `graph-compose-emoji` are exempt: they
version independently and come from Central. Staged mode refuses `--warm` for the
same reason.

The bundle a tagged run uploaded is kept as the workflow artifact
`central-bundle-v<X.Y.Z>`. While the deployment waits at `VALIDATED`, download it,
unzip it and smoke it with `--staged-repo` before pressing Publish.

Or dispatch the **Release Smoke (consumer verification)** GitHub Actions workflow
(`.github/workflows/release-smoke.yml`) with a `version` input — handy after a
publish, once Central has indexed the release.

The harness passes an isolated `settings.xml` (`-s`) whose `mirrorOf=*` mirror
forces **every** artifact and plugin request through Maven Central, and uses one
dedicated local repository under `target/` that never receives an `mvn install`
of GraphCompose. In the default (isolated) mode it **evicts the GraphCompose
artifacts** (`io/github/demchaav/**`) before each scenario — hard-failing if the
eviction does not take — so every scenario must re-resolve `graph-compose-*` from
Central, proving the release resolves with no local reactor build behind it.
Maven's own plugins and third-party libraries (PDFBox, JUnit, …) stay cached,
because re-downloading Maven's core plugins onto an empty repository is heavy,
flaky, and tests Maven rather than this release. Classpath isolation between
scenarios (e.g. the lean core never seeing the PDF backend) comes from each
project's declared dependencies and the `s2` enforcer rule, not from the
repository state.

The harness prints one `RESULT <scenario> PASS|FAIL` line per project and ends
with a machine-readable `SUMMARY {"version":"…","passed":N,"failed":M,"total":T}`
line. Exit code is non-zero if any scenario fails.

The version under test defaults to the current published release (`2.4.1`) and is
overridable with `--version` / `-Version` (or the workflow's `version` input); it
is passed to Maven as `-Dgc.version`, overriding the `gc.version` property in each
pom. Release smoke always tests **published** artifacts — never a `-SNAPSHOT`.
