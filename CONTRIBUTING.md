# Contributing

Thanks for helping improve GraphCompose.

## Before you start

Read these files first:

- [README.md](./README.md)
- [docs/architecture/overview.md](./docs/architecture/overview.md)
- [docs/architecture/package-map.md](./docs/architecture/package-map.md) — what lives in which package, and which of them are internal
- [docs/operations/benchmarks.md](./docs/operations/benchmarks.md) when you touch benchmark tooling, render hot paths, layout hot paths, or performance-facing docs

[docs/contributing/extension-guide.md](./docs/contributing/extension-guide.md)
walks the extension points end to end — a new semantic node, a fluent setter, a
render handler, a whole backend — and is the guide to follow when adding one.

They explain the current public surface, the engine/template split, and the recommended extension points.

## Java 17 baseline

GraphCompose targets **Java 17+** as of v1.6.1. CI runs the full test suite against Temurin JDK 17 / 21 / 25 in parallel matrix, so JDK-incompatibility regressions fail the PR immediately.

When writing new code, avoid Java 21+ APIs and language constructs that don't exist in 17:

- `List.getFirst()` / `List.getLast()` &rarr; `list.get(0)` / `list.get(list.size() - 1)`
- `Thread.threadId()` &rarr; `Thread.getId()`
- `switch` with type patterns (`case Foo f -> …`) &rarr; `instanceof` if-else chains
- `switch` with deconstruction patterns (`case Foo(Bar b) -> …`) &rarr; `instanceof Foo f` + `f.bar()`
- `case null, default ->` &rarr; explicit `if (x == null) return …;` early return
- `List.reversed()` &rarr; `Collections.reverse(new ArrayList<>(list))`

## Build and test

- The blocking validation gate for repository work is `./mvnw -B -ntp clean verify` at the repository root — the root pom is the reactor aggregator, so this builds and verifies **every module**. For a fast inner loop while iterating on the engine, scope it to the core module: `./mvnw -B -ntp verify -pl :graph-compose-core`.
- Run the engine-resident guard suite with `./mvnw -B -ntp "-Dtest=EnginePdfBoundaryTest,DocumentationCoverageTest,CanonicalSurfaceGuardTest,PackageMapGuardTest,VersionConsistencyGuardTest,ShowcaseSiteGuardTest,CiGuardListGuardTest,CiGateCoverageGuardTest,BinaryCompatibilityGateGuardTest,CodeQlScopeGuardTest,AgentsGuideGuardTest,BenchmarkDependencyInstallGuardTest" test -pl :graph-compose-core` — the same list CI runs. Every name must live in `graph-compose-core`: Surefire drops a name that matches nothing as long as a sibling matches, so a guard that lives elsewhere would silently not run (`CiGuardListGuardTest` fails the build if one creeps in).
- The cross-module documentation guards — `DocumentationExamplesTest` and `DocumentationSnippetCompileTest`, which compiles the literal java fences published in `docs/` — live in `graph-compose-qa`: `./mvnw -B -ntp "-Dtest=DocumentationExamplesTest,DocumentationSnippetCompileTest" test -f qa/pom.xml`. A standalone `-f qa/pom.xml` run resolves its `graph-compose-*` dependencies from `~/.m2`, not from the reactor, so run `./mvnw -B -ntp -DskipTests install` once first — otherwise it quietly tests the artifacts you last installed instead of your working tree.
- Run the local benchmark wrapper when you change performance-sensitive code or benchmark tooling: `powershell -ExecutionPolicy Bypass -File .\scripts\run-benchmarks.ps1` (Windows). To compare two branches fairly, use `scripts/ab-bench.ps1` (Windows) or the cross-platform `scripts/ab-bench.sh` (Linux/macOS/Git Bash). See [docs/operations/benchmarks.md](./docs/operations/benchmarks.md).

## How to propose changes

GraphCompose follows a fork &rarr; feature branch &rarr; pull request flow. **Target the branch that matches your change** &mdash; pick the base branch from this table before you fork:

| Change type | Base branch |
|---|---|
| **Feature / fix** (almost all work) | `develop` |
| **Critical 1.9.x fix** (bug / security backport) | `1.x` |
| Stable releases (tags) | `main` |

Almost all work targets **`develop`**, the ongoing 2.x line. The `1.x` branch takes critical fixes and security backports only &mdash; no features. `main` is the public stable surface and accepts release merges only. See [Version lines](#version-lines-and-the-1x-maintenance-branch) below.

### Contribution flow

1. **Fork** the repository on GitHub and clone your fork locally.
2. **Create a feature branch** from your target base (`develop` for feature work &mdash; substitute `1.x` for a critical 1.9.x backport):
   ```bash
   git checkout develop
   git pull --ff-only origin develop
   git checkout -b feature/short-description
   ```
   Use `feature/...` for new functionality, `fix/...` for bug fixes, and `docs/...` for documentation-only changes. Issue-prefixed names (`42/fix/short-description`) are also welcome &mdash; convenient when the branch closes a specific issue.
3. **Commit small, focused changes.** Subjects follow Conventional Commits — `type(scope): the concrete effect` — the same shape the [pull request template](.github/pull_request_template.md) asks of a PR title. Say the *why*, not only the *what*. `git log --oneline -20` on `develop` is the reference; `fix(font): the donut-centre KPI renders the weight it declares` and `ci(guards): run every guard the job names, and compile docs-only pull requests` are the shape.
4. **Run the validation gate locally** before opening a PR:
   ```bash
   ./mvnw -B -ntp clean verify
   ```
   This runs the architecture-and-documentation guards plus the full test suite. The same gate runs in CI on every PR.
5. **Push** your feature branch to your fork and open a pull request against the base branch you started from (`develop` for feature work) on `DemchaAV/GraphCompose`. Reference any related issue and describe the user-visible change in the PR body.
6. **CI runs automatically.** Active jobs:
   - `Architecture and Documentation Guards` &mdash; fast canonical / engine-boundary guard tests, fail-first gate (always runs)
   - `Build and run tests (JDK 17)`, `(JDK 21)`, `(JDK 25)` &mdash; full `mvnw verify` in parallel matrix across the supported JVMs
   - `Examples Generation Smoke Test` &mdash; regenerates every runnable example and uploads the PDFs as a CI artifact
   - `Binary Compatibility` &mdash; PR-only japicmp diff of the `graph-compose-core` and `graph-compose-templates` surfaces
   - `Performance Smoke Check` &mdash; PR-only coarse benchmark to catch performance regressions
   - `CI Gate` &mdash; single aggregate status check that is green when every job that ran passed

   **Selective on pull requests:** a `dorny/paths-filter` step skips the heavy jobs when a PR touches nothing that affects the build. Markdown counts as a build input, so a **docs-only PR still runs the reactor** &mdash; on the baseline JDK alone, and without example generation &mdash; because that is where the guards compiling the published snippets live. `Binary Compatibility` runs only when the core or templates module changed, and the `Performance Smoke Check` only when core / render-pdf / templates changed. Pushes to `develop` / `main` (and manual dispatch) always run the full gate. Point branch protection at **`CI Gate`** + **`Architecture and Documentation Guards`** rather than the individual matrix legs, so a docs-only PR is not left waiting on a skipped check.

   The PR cannot merge into a protected branch until all required checks are green.
7. **Address review comments**, then squash any fixup commits before merge. The maintainer merges through GitHub once review is complete.

### Branch protection

`main` is protected:

- pull request required (no direct pushes)
- both CI status checks must pass before merge
- linear history is enforced (squash or rebase, no merge commits)
- force pushes and branch deletion are disabled

`develop` (and, for 1.9.x backports, `1.x`) accepts feature-branch PRs from contributors. The maintainer may push directly for solo-driven release-prep work; external contributions still flow through PRs.

### Release flow

1. **Release prep** lands on `develop` &mdash; version bumps propagate via the root reactor `pom.xml` to all modules in one pass; fresh CHANGELOG entry; migration guide for minor releases. **README install snippet stays pinned to the previously published version** until Maven Central confirms the new artifact, otherwise consumers copying the snippet during the publish window hit a 404.
2. **`scripts/cut-release.ps1 -Version <X.Y.Z>`** automates the bump + CHANGELOG date + commit + tag + push from `develop`. The maintainer fast-forwards `main` from `develop` after the tag lands (`git push origin develop:main`).
3. **Maven Central** picks up the new tag automatically via [`.github/workflows/publish.yml`](./.github/workflows/publish.yml) &mdash; the workflow re-runs `mvnw verify` at the tagged commit, signs each module's artefacts (main / sources / javadoc / pom &mdash; the `graph-compose` wrapper has no sources of its own, so it publishes no sources jar) with the repo's GPG key, and uploads via the `central-publishing-maven-plugin`. Hyphenated tags (`-rc`, `-alpha`, `-beta`) are skipped on Central; they ship only to the GitHub Release pre-release surface. Javadocs for the engine API publish to [javadoc.io/doc/io.github.demchaav/graph-compose](https://javadoc.io/doc/io.github.demchaav/graph-compose) shortly after each Central release, from v2.1.1 onwards — the `graph-compose` coordinate carries no sources of its own, so its javadoc jar is built from the engine's (`wrapper/pom.xml`, `includeDependencySources`). Before that it shipped no javadoc artifact at all and the hosted page stayed on 1.9.1.
4. **GitHub Release** is created with notes from the matching `CHANGELOG.md` section.

See [docs/contributing/release-process.md](./docs/contributing/release-process.md) for the full checklist (audit gates, hotfix protocol, lessons learned).

### Version lines and the 1.x maintenance branch

The 2.0 GA shipped, so the branches now hold their long-term roles:

- **`develop`** is the ongoing 2.x working branch &mdash; all feature branches target it.
- **`main`** is the stable 2.x line, tagged at each release (latest `v2.x`).
- **`1.x`** is the maintenance branch cut from the final 1.9.x commit. It receives **critical fixes and security / CVE backports only &mdash; no features** &mdash; released as `1.9.x` patches from `1.x` via the same `cut-release.ps1` + tag flow. New feature work always targets the 2.x line.

## Repository map

- `core/src/main/java/com/demcha/compose/document/api`, `document.dsl`, `document.node`, `document.style`, `document.table`, `document.image`, `document.output`, `document.exceptions`, `document.snapshot`
  Public canonical authoring surface — `DocumentSession`, the DSL builders, semantic node records, public style values, table types, image types, backend-neutral output options (metadata / watermark / protection / header-footer), and snapshot DTOs
- `core/src/main/java/com/demcha/compose/document/layout`
  Canonical functional layout pipeline: `LayoutCompiler`, `BuiltInNodeDefinitions`, `TableLayoutSupport`, `PreparedNode`, `PlacedFragment`
- `render-pdf/src/main/java/com/demcha/compose/document/backend/fixed/pdf` — module **graph-compose-render-pdf**
  PDF backend: `PdfFixedLayoutBackend`, fragment handlers, and the option translators that bridge canonical types to PDFBox
- `render-pptx/src/main/java/com/demcha/compose/document/backend/fixed/pptx` — module **graph-compose-render-pptx**
  PPTX fixed-layout backend: `PptxFixedLayoutBackend` and its handlers, consuming the same resolved `LayoutGraph` as the PDF backend. The module also carries the older `PptxSemanticBackend` manifest skeleton under `com.demcha.compose.document.backend.semantic.pptx`
- `render-docx/` — module **graph-compose-render-docx**
  Semantic exporter `DocxSemanticBackend` (Apache POI based), under `com.demcha.compose.document.backend.semantic.docx`
- `templates/src/main/java/com/demcha/compose/document/templates/*` — module **graph-compose-templates**
  Template families (CV, cover letter, invoice, proposal), their data records, the shared `BrandTheme`, and the widgets they compose from. Schedule data records live here too; the schedule document is composed by an example rather than by a template.
- `core/src/main/java/com/demcha/compose/document/showcase`
  `FontShowcase` (bundled-font preview renderer) — stays in the core engine
- `core/src/main/java/com/demcha/compose/engine/*`
  Internal shared engine foundation under the canonical surface (measure, paginate, place, render). Not part of the recommended public API
- `core/src/main/java/com/demcha/compose/font`
  Public font registry, `FontName`, default fonts
- `core/src/test/java/com/demcha/documentation/*`
  Examples used to keep README/documentation snippets honest
- `core/src/test/java/com/demcha/compose/engine/integration/*`
  End-to-end behaviour checks for the engine foundation's layout, pagination, and rendering paths
- `core/src/test/java/com/demcha/compose/document/*`
  Canonical API, DSL, layout, backend, and template tests
- `assets/readme/*`
  Screenshots used by the README
- `pom.xml`
  The repository-root Maven reactor (aggregator POM); release tooling propagates the version bump across all modules through it in one pass. The engine itself lives in `core/pom.xml` (`graph-compose-core`)
- `baselines/`
  Committed performance-benchmark baselines: `current-speed-full.json` is the median reference the `11-verdict-current-speed` gate judges runs against; `BASELINE_SUMMARY.md` / `COMPARISON.md` are historical pre-optimization snapshots

## Recommended workflow

1. Start with the smallest change that solves one problem.
2. Keep structural cleanup separate from behavior changes whenever possible.
3. If you touch public examples or screenshots, update the related docs in the same change.
4. Run the smallest relevant tests while iterating, then run `./mvnw -B -ntp clean verify` before opening a pull request.
5. For quick visual iteration on a template, run [GraphComposeDevTool.java](qa/src/test/java/com/demcha/compose/devtool/GraphComposeDevTool.java) in test scope &mdash; it hot-reloads the rendered PDF as you edit your template source.

## Contributor architecture rules

GraphCompose is split into a public canonical authoring surface
(`com.demcha.compose.document.*`) and an internal engine foundation
(`com.demcha.compose.engine.*`). New features land on the canonical
surface; the engine foundation stays an internal detail. The rules
below reflect that split.

### Canonical surface — primary contributor lane

Most contributions add a new public node, builder, style value, or
template feature. The rules:

- DSL builders live in `com.demcha.compose.document.dsl`.
  Implementation helpers belong in `document.dsl.internal` and must
  not leak into the public surface.
- Semantic node records live in `com.demcha.compose.document.node`.
  When a new field is added later, ship a back-compat constructor that
  defaults the new field — see `ShapeContainerNode`, `TableNode`,
  `LayerStackNode.Layer`, the v1.5 `*Node` records that gained
  `transform`, etc.
- Public style / table / image / theme / output value types live under
  `document.style`, `document.table`, `document.image`,
  `document.theme`, and `document.output`. They stay renderer-neutral —
  no `org.apache.pdfbox` imports.
- Layout integration for a new node is a `NodeDefinition<MyNode>`
  registered with `NodeRegistry`. See `BuiltInNodeDefinitions` for
  the established pattern.
- A template family lives under `...document.templates.<family>` with
  `data` / `components` / `widgets` / `presets`; cosmetic tokens are the
  shared `BrandTheme` in `...document.templates.core.theme`, never a
  package of the family's own. Keep PDF-only setup in the document
  session/backend layer rather than inside a preset, and do not import
  `PDDocument`, `PDPage`, `PDRectangle` or other low-level PDF types into
  template code.
- Public template contracts are compose-first: prefer
  `compose(DocumentSession, ...)`. New README snippets, runnable
  examples, and integration docs must show `compose(...)` rather than
  the removed low-level PDF entry points.

### Engine internals — only for engine and backend contributors

These rules apply when you touch measurement, pagination, the
render-pass session, or PDF render dispatch. Application code should
not need any of them.

- Engine render markers implement backend-neutral `Render`. Do not
  add backend-specific render interfaces back into
  `engine/components`.
- PDF rendering logic lives in the canonical fixed-layout backend under
  `render-pdf/src/main/java/com/demcha/compose/document/backend/fixed/pdf/handlers/`,
  where each `PdfFragmentRenderHandler` draws one fragment kind. Backend-only
  helper objects live alongside the backend in
  `com.demcha.compose.document.backend.fixed.pdf`, not in
  `components/renderable`.
- Builders and layout code get text width and line metrics from
  `TextMeasurementSystem`, not from the active renderer, `PdfFont`,
  or PDFBox objects.
- Keep `core/src/main/java/com/demcha/compose/engine/components/*` free of
  `org.apache.pdfbox` and `com.demcha.compose.engine.render.pdf`
  imports.
- When you add a new fragment kind, register its handler with **both**
  fixed-layout backends — `PdfFixedLayoutBackend` and `PptxFixedLayoutBackend` —
  and add or update dispatch coverage. A kind registered with only one renders
  in one output and silently vanishes from the other.

### Guard rails

The rules above are enforced by tests:

- canonical surface guards:
  [CanonicalSurfaceGuardTest.java](./core/src/test/java/com/demcha/documentation/CanonicalSurfaceGuardTest.java),
  [PublicApiNoEngineLeakTest.java](./core/src/test/java/com/demcha/documentation/PublicApiNoEngineLeakTest.java),
  [SemanticLayerNoPdfBoxDependencyTest.java](./core/src/test/java/com/demcha/documentation/SemanticLayerNoPdfBoxDependencyTest.java),
  [DocumentationExamplesTest.java](qa/src/test/java/com/demcha/documentation/DocumentationExamplesTest.java),
  [DocumentationCoverageTest.java](./core/src/test/java/com/demcha/documentation/DocumentationCoverageTest.java)
- engine internals guards:
  [EnginePdfBoundaryTest.java](./core/src/test/java/com/demcha/compose/engine/architecture/EnginePdfBoundaryTest.java),
  [PdfRenderInterfaceGuardTest.java](render-pdf/src/test/java/com/demcha/compose/engine/render/pdf/PdfRenderInterfaceGuardTest.java)

## Adding a new feature

### New public node + builder (most common path)

If application code should be able to add a new visible thing to a
document:

1. Define a public record under `com.demcha.compose.document.node`
   with a compact constructor that normalizes optional fields.
   Validate non-finite or negative dimensions where relevant.
2. Add a `NodeDefinition<MyNode>` in `BuiltInNodeDefinitions`.
   Implement `prepare(...)` (measurement),
   `paginationPolicy(...)`, and `emitFragments(...)`. If your node
   should support `DocumentTransform`, follow the
   `wrapAtomicWithTransform` pattern used by `ShapeNode`,
   `EllipseNode`, `LineNode`, `ImageNode`, and `BarcodeNode`.
3. Add a public builder under `com.demcha.compose.document.dsl`.
   Inherit `AbstractFlowBuilder<T, N>` for the common
   `addParagraph` / `addTable` / `addRow` / `softPanel` / `accent*`
   surface; implement `Transformable<T>` if rotation / scale should
   apply.
4. Add convenience overloads on `AbstractFlowBuilder` for the
   "common case" if the new node has one worth a one-line shortcut
   (e.g. `addCircle(diameter, fill)`).
5. Add a `*Test` covering the builder contract plus a layout snapshot
   test (`LayoutSnapshotAssertions`) and a PDF render smoke test
   (`PdfVisualRegression`) when the visual output matters.
6. Update [docs/recipes/](./docs/recipes) if the feature has a
   copy-pasteable usage pattern, and add a runnable example under
   `examples/src/main/java/com/demcha/examples/` with a
   `GenerateAllExamples` hook if it deserves a PDF preview.

Reference templates to copy:

- `ShapeContainerNode` + `ShapeContainerBuilder` +
  `ShapeContainerBuilderTest` (composite + clip + transform)
- `TableNode` + `TableBuilder` + `TableBuilderRowSpanTest` /
  `TableBuilderZebraAndTotalsTest` /
  `TableBuilderRepeatHeaderTest` (multi-feature node)
- `EllipseBuilder` + `EllipseNode` + `TransformableLeafBuildersTest`
  (atomic leaf with transform)

### New built-in template

There is one template authoring pattern, whether you are adding a
new family or a new preset inside an existing one: the layered
architecture documented in
[**docs/templates/v2-layered/contributor-guide.md**](./docs/templates/v2-layered/contributor-guide.md).
A family has `presets/` and adds `data/`, `components/` and `widgets/`
as it needs them, each with a clear contract, over the shared
`templates.core` layer that owns the theme.

- Every preset is a `public final class` — no inheritance — with a
  `create(BrandTheme)` factory returning `DocumentTemplate<S>`, plus a
  no-arg overload that picks a default theme.
- Compose against `DocumentDsl` — no PDF-specific imports.
- Route every visible token through `theme.palette()` /
  `theme.text()` / `theme.spacing()` / `theme.table()`.
- Reference implementations:
  `templates.cv.presets.ModernProfessional`,
  `templates.invoice.presets.ModernInvoice`,
  `templates.proposal.presets.ModernProposal`. Read one before
  starting yours.

> 📚 **Map of template docs**:
> [docs/README.md](./docs/README.md#templates) lists every template
> guide with a one-line description so you can pick the right one
> fast.

### New engine internal primitive

If you are extending the engine foundation itself (a new render
marker, a new layout system, a new render-pass session):

- Decide first whether the feature belongs on the public surface as
  a `DocumentNode` instead. If yes, see "New public node" above and
  treat the engine work as plumbing, not as new public surface.
- For genuine engine primitives, add the `NodeDefinition` that
  prepares and measures the node, plus a backend-owned render handler
  per fixed-layout backend.
- Pagination is declared by the definition, not by a marker: state the
  split behaviour on the `NodeDefinition` itself, and compile a node
  that can continue across pages through `SplittableLeafCompiler` so
  the continuation indices stay monotonic.
- A container grows from the sizes its children report during
  `prepare`; do not add growth as a separate signal, and do not read
  it as a pagination flag.

For text-heavy primitives, also read:

- [TextMeasurementSystem.java](./core/src/main/java/com/demcha/compose/engine/measurement/TextMeasurementSystem.java)
- [docs/architecture/overview.md](./docs/architecture/overview.md)
- [docs/contributing/extension-guide.md](./docs/contributing/extension-guide.md)

If the primitive should be available to application developers,
expose it through `DocumentDsl` and a public `DocumentNode`, not a
low-level test harness.

## Testing expectations

Choose the smallest tests that match the change:

- For README or docs examples:
  [DocumentationExamplesTest.java](qa/src/test/java/com/demcha/documentation/DocumentationExamplesTest.java)
- For engine/backend boundary changes:
  [EnginePdfBoundaryTest.java](./core/src/test/java/com/demcha/compose/engine/architecture/EnginePdfBoundaryTest.java)
  [PdfRenderInterfaceGuardTest.java](render-pdf/src/test/java/com/demcha/compose/engine/render/pdf/PdfRenderInterfaceGuardTest.java)
- For PDF fragment-handler dispatch changes:
  [PdfRenderInterfaceGuardTest.java](render-pdf/src/test/java/com/demcha/compose/engine/render/pdf/PdfRenderInterfaceGuardTest.java)
- For layout/positioning behavior:
  [LayoutInsetsTest.java](./core/src/test/java/com/demcha/compose/document/layout/LayoutInsetsTest.java)
- For pagination and multi-page behavior:
  [PaginationEdgeCaseTest.java](qa/src/test/java/com/demcha/compose/document/api/PaginationEdgeCaseTest.java)
- For Templates v2 CV / cover-letter presets:
  [CvV2VisualParityTest.java (CV)](qa/src/test/java/com/demcha/compose/document/templates/cv/presets/CvV2VisualParityTest.java)
  [CoverLetterV2VisualParityTest.java (cover letter)](qa/src/test/java/com/demcha/compose/document/templates/coverletter/presets/CoverLetterV2VisualParityTest.java)
  and the [per-preset smoke tests](qa/src/test/java/com/demcha/compose/document/templates/cv/presets)
- For DOCX export (`render-docx`) changes:
  [DocxFidelityCorpusTest.java](qa/src/test/java/com/demcha/compose/document/templates/fidelity/DocxFidelityCorpusTest.java).
  It exports every template preset to DOCX, with documents built for shapes no preset has on
  its own fixture, has LibreOffice set each one, and holds every
  line of every document to the drift its baseline records, down and across: a line that
  moved more than 0.5pt from where the baseline holds it, nearer the page or further, a line
  set at other words, a line found that the baseline does not hold, another number of pages
  from the editor, or another number of pages or lines from the engine fails it. It needs
  LibreOffice and runs only when asked for — install first, as for any standalone `qa` run:
  `./mvnw -B -ntp test -f qa/pom.xml -Dtest=DocxFidelityCorpusTest -Dgraphcompose.docxFidelity=libreoffice`.
  A change that moves documents, nearer the page included, rewrites the baseline with
  `-Dgraphcompose.docxFidelity.update=true`, and commits it with the change: its diff shows
  which documents moved, and which way. A new preset joins the corpus in its family's
  `*DocxCorpus` class, and so does a preset on a longer fixture; a shape no preset has
  — a page break inside a
  panel, a break in a row's column, a shape container narrower than the block it stands in
  or clipped short of its text, an icon a layer stack lays over a card's corner — goes in
  `SyntheticDocxCorpus`. Its baseline holds it as the editors set it, a defect of the export
  included, and the change fixing that rewrites its rows nearer the page.
  CI runs it on Linux (the `DOCX Fidelity` job) against `libreoffice-linux.tsv`, and uploads
  what it measured: LibreOffice sets text a little differently per platform, so a change that
  moves documents takes that artifact's files as the Linux baseline, and its own run's as the
  Windows one. Microsoft Word, the editor the export answers to, is not in CI: on
  Windows with Word, `scripts/docx-visual/word-fidelity.ps1` (`-Update` to rewrite) exports the
  corpus, has a private Word instance convert it, and holds it to `word-windows.tsv` — run it
  from PowerShell (pwsh), after the install above, before a DOCX export change is opened.
  Rewrite the baseline in the change that moves the documents, and the gate will remind you —
  as long as you run it, which for Word means running the script above: it fails a line that has
  moved more than 0.5pt from where the baseline holds it, nearer the page or further, a document
  the editor sets on another number of pages, one the engine draws on another number of pages or
  lines, and one whose lines the editor finds a different set of. Until the gate was made
  two-sided it only looked for lines set further, which let a change move documents nearer, or
  less than 0.5pt further, with the baseline left as it was: between #899 and #974 that left
  1609 of the Word baseline's 5165 rows stale over 74 commits, 226 of them standing more than
  half a point nearer the page across, until three documents stood 0.48pt further and the next
  change to touch them would have failed the gate for drift it did not cause. The slack is read
  afresh against each rewrite, so it bounds no total: what two-sidedness buys is that every move
  past rounding is seen and written down, not that drift cannot accumulate over many changes.
  A failure naming lines that moved nearer the page is not a defect to fix — it is the export
  doing better, and the baseline is how it gets recorded. A rewrite keeps the `#` notes the file
  it replaces carried, by either path, and leaves it to you to prune the ones it has made stale;
  a note is a block under the header, not an annotation of the rows below it, and comes back
  above them. Each baseline's header records the build it was shot on, and a
  differing build is rarely the cause: Word 16.0.20430 and 16.0.20527 measure the corpus as
  #899 exported it identically, row for row.
  `libreoffice-windows.tsv` is known stale, and the local LibreOffice run fails on it: 2
  documents fail even one-sidedly — 8 lines no longer found, 2 page counts and 65 lines more
  than half a point further, 8 of them `proposal-indigo`'s, 10.25pt lower than it holds them
  on the LibreOffice build the baseline names. It has not been shot whole since #898, where
  `libreoffice-linux.tsv` now has. Do not rewrite it as part of an unrelated change: a move
  further from the page wants the commit that made it found first, as the Word baseline's drift
  was attributed before #975 rewrote it, and the file rewritten with that reckoning. Until then,
  read a local LibreOffice run against the documents your change touches rather than against a
  green run.
  Each run also writes `measured-<editor>-acceptance.md`: which documents the editor sets as the
  page does — the same pages, 95% of the lines found, a median of 1pt or less, no more than 10%
  past 2pt — and what keeps the rest short of it. CI shows LibreOffice's in the job summary.
  The corpus also answers for how a document edits in Word.
  `scripts/docx-visual/edit-protocol-corpus.ps1` runs over the DOCX that
  `-Dgraphcompose.docxFidelity=export` leaves in `qa/target/docx-fidelity/engine`. For each
  document it makes ten edits, each on a copy that it saves and reopens:
  - lengthen the longest paragraph, and shorten it;
  - press Enter after it and type;
  - restyle Normal;
  - add a table row, and delete one;
  - continue a list;
  - type at the end until a new page opens;
  - lengthen the first paragraph of body text, watching the drawings;
  - save with no edit.

  It records what it can decide alone in `edit-protocol-corpus.json`:
  - text kept, and a paragraph grown or shrunk and still above the block after it;
  - the body size following Normal, with room for it wherever the restyle reaches — the main
    text, the headers and footers, the text drawings hold: no paragraph whose letters grew
    stands on exact lines shorter than 1.1 of them, none sits in a row or frame of exact
    height, and no drawing's text runs past it;
  - typed text standing below everything else, outside any table, visible, in the size of the
    text it continues (or, after an empty paragraph, on lines that fit it);
  - each drawing beside text the lengthening moved moving with it, by as much and onto the same
    page, and each drawing beside text the lengthening left standing staying where it was.
    "By as much" is read within a point and a half and half a twip. A drawing's place comes from
    the page geometry in whole points and a paragraph's line from a range, which gives Word's own
    layout in twips, so two readings of a drawing that moved with its text exactly can be a point
    apart between them and the limit has to clear that; and it is held half a twip off the twip
    grid rather than on it, because a limit of a whole number of twips is a difference the
    readings can produce exactly and the verdict would then turn on the last digit of a float
    instead of on where Word drew anything. At a point and a half it did: of the 279 drawings in
    the verdict of the documents that hold every finding, 15 passed with the difference at exactly
    1.50 and three failed by between 0.00002 and 0.05 of a point, and two runs of the script on
    bytes that had not changed came to report five fewer followers over one more drawing in the
    set. Neither bound of the limit moves with the half twip, drift landing on that same grid
    — a drawing that drifted half a point still passes always, one that drifted two and a half can
    still pass, and 0.55 and 2.55, the next drifts there are, still cannot. Which of the two sets
    a drawing is counted in — beside text the edit moved, or beside text it left standing — is read
    the same way: the text counts as moved past half a point and half a twip, the half point being
    the noise its reading leaves and the half twip keeping a move of exactly ten twips off the
    comparison. Both thresholds are held from either side, and off that grid, by `-SelfCheck`. Only
    a drawing a paragraph carries is asked that. One nailed to the sheet — `wp:positionV
    relativeFrom="page"` — is placed from the top of the page it is drawn on and by nothing else,
    so the text beside it has no claim on where it stands: measured on the self-check's two alike
    probe documents, with the paragraph above lengthened by two lines, such a drawing moved 0.00pt
    in Word 16 and 0.00pt in LibreOffice 24.2 while its own text moved 32.04 and 52.50pt, where the
    same drawing carried by that text's paragraph in the other document moved 31.96 and 52.50pt
    with it. Which anchor the export chose is held by `DocxDrawingAnchorGuardTest`, which names
    every drawing of the corpus still placed from the page and fails closed when one is added to
    that list or taken off it. So the nailed ones are measured here as the carried ones are, named
    in the detail with the counts for their set and by name for each one that left its text or lost
    its place, and left out of the verdict; a drawing that ought to follow stays in it whether or
    not nailed drawings stand beside it, since what is read is each drawing's own anchor and not
    the document it stands in. Nothing may leave the verdict by carrying an answer the scenario has
    no question for: a drawing placed down from anything else — neither its paragraph nor the page
    — is named and fails, and so is a name Word gives to more than one drawing, which is read as no
    drawing at all and is reported on a line of its own rather than among the places the page
    geometry does not give. A drawing belongs to the paragraph nearest its top-left corner — how
    far that corner stands from
    where the paragraph's text begins, plus how far it stands outside the width that text runs to —
    and a drawing standing in the space above a paragraph counts as standing no distance down the
    page from it, the width still having its say; a page's backgrounds are left out. The corner, the
    page, and the space above a paragraph the geometry draws a box for are read from Word's own page
    geometry, which draws a rectangle wherever it draws a shape and lines wherever it draws text, in
    points from the corner of the page it draws them on; the first line and the width come from a
    range. A cell's width counts the padding it holds its text away from on both sides, so the
    width its text runs to is read from the cell's own edges inward: read instead as the text's
    left plus the cell's width, it reached past the end of that text by both paddings — 35.1pt in
    `cv-terracotta_rail`, whose sidebar holds its text 25.4pt from its left edge and 9.7pt from
    its right, and whose text Word reads as starting at 25.25pt and so running to 172.4, where
    the reach was read as 207.5 (the two readings differ by three twips, and the reach carries
    that: it is narrow by 0.15pt, which is as near as Word's own two readings come to each
    other). The sidebar's text was then taken to run on past the next column's text at 183.25pt
    and over the gutter at 202pt where four of that column's rail dots stand, and those four were
    read as standing beside the sidebar's list — a list the edit leaves where it is — rather than
    beside the entries that carry them. The space matters because a drawing placed from its
    paragraph stands a stored offset
    below the top of that paragraph's box, the space above the paragraph included, so a paragraph
    read from its first letter alone stands that whole space away from its own drawing and loses it
    to the text above wherever the space is the wider. The geometry draws a box per block and not
    per paragraph — a whole table is one, and its lines run one to a row — so a paragraph of a row
    has no space of its own to read and keeps that limit, and a paragraph of a row Word draws on
    more than one page takes the page a range reports, since none of the row's lines answers that
    one paragraph at a time. A drawing Word's page geometry does not name is reported with the
    reason and not measured, and the scenario fails where a paragraph carries that drawing: a
    document the instrument cannot read is not a document that passed. A nailed one is named with
    its reason too and fails nothing, there being nothing the scenario would have asked of it.
    A paragraph the reading cannot place before the edit is named in the
    detail instead of failing the scenario, since a drawing beside it is held to another paragraph
    and the naming is what makes that visible; one it cannot place after the edit is counted against
    the drawing held to it, and fails where a paragraph carries that drawing. A drawing the reading
    no longer finds after the save and the reopen — Word does not name it, or names it and another
    alike so that none of them can be told from another — is counted apart from that and fails for
    either set: a drawing gone from the document, or one that can no longer be told apart, is not a
    question of what it was placed from, and no other scenario counts the shapes; text that cannot
    be placed again fails for the carried set alone. A document where no drawing was both read and
    found text to stand beside while some paragraph could not be placed is the instrument failing,
    and that one is raised with what the reading did find of its drawings. A document whose
    paragraphs it can place none of is raised too, as Word naming no rectangle for any drawing
    is. `-SelfCheck` runs the protocol over probe documents it writes itself: it holds where it
    reads a place to where Word's own PDF draws the same drawing, holds the attribution with
    documents one wrong owner fails, and holds what is asked of whom with a document carrying a
    nailed drawing beside one that ought to follow and fails to. Two of its checks hold the two
    thresholds the verdict is read by — each from either side, and each off the grid its readings
    land on — and need no document at all, so they run before Word is asked for: a machine without
    Word holds both thresholds, and exits 1 where either of them is wrong or the grid under them
    is. Run it after changing how a
    place is read, how an owner is chosen, what the verdict is taken over, or either threshold it
    is read by. What no probe reaches is
    broken on purpose and recorded as caught by nothing rather than claimed: a drawing the reading
    no longer finds no longer failing, on either side, since no document can make Word drop a shape
    or give two shapes one name over an edit, and the two exclusions the nailed set carries of its
    own, a drawing whose own text cannot be placed again and one whose place the geometry stops
    giving after the edit. The order the names print in is unprobed as well, and changes no
    verdict. One path out of the verdict is older than any of this and still open: a carried drawing
    whose page holds no paragraph the reading could place is counted as standing beside no text, and
    a document can pass with it.

  A scenario a document has nothing to edit for — no table, no list, no drawing beside text —
  is recorded `N/A` with what the document lacks, rather than left out of the JSON.

  The protocol reads the letters' size and visibility from the text, without the paragraph
  mark. Beside the lengthened copy and the new-page copy it writes PDFs, from which a person
  judges whether a page still looks right. It takes about a minute a document; `-Scenario`
  runs only the scenarios named, by the names the JSON records.

If a change affects public docs, examples, or screenshots, update those assets in the same PR so the repository stays internally consistent.

If a change affects resolved geometry, pagination, or ordering, prefer adding or updating a layout snapshot test as well. Snapshot coverage is debug-only and test-oriented: it should validate layout state without being wired into the normal production PDF pipeline.

## Contribution guidelines

- Preserve existing public Java class names and package paths unless a planned migration explicitly says otherwise.
- Avoid mixing cleanup, refactors, and behavior changes in one PR.
- When touching docs or examples, keep them aligned with the current public API and file layout.
- If a change affects resources, tests, or generated outputs, update the related references in the same PR.
- Prefer additive or backward-compatible changes when extending canonical DSL APIs or template contracts.
- If a rename or move could break imports, resource paths, or examples, either update every affected reference in the same change or leave it as a documented follow-up.

## Documentation and screenshots

- Keep [README.md](./README.md) aligned with the tested examples.
- Keep benchmark values clearly dated when they are refreshed.
- Keep `assets/readme/*` screenshots consistent with the current render outputs.
- If you add a new extension point or contribution pattern, update [README.md](./README.md), [docs/architecture/overview.md](./docs/architecture/overview.md), and [docs/contributing/extension-guide.md](./docs/contributing/extension-guide.md) as part of the same change.
- If you change benchmark flow, benchmark artifact layout, or diff selection rules, update [README.md](./README.md) and [docs/operations/benchmarks.md](./docs/operations/benchmarks.md) in the same change.
- Visual PDF artifacts are grouped under `target/visual-tests/clean/*` and `target/visual-tests/guides/*` so guide-line renders are easy to find separately from clean outputs.

## Package naming

The repository uses these normalized package roots:

- `com.demcha.compose` — `GraphCompose` factory and shared entrypoint
- `com.demcha.compose.document.api` — `DocumentSession`, `DocumentPageSize`
- `com.demcha.compose.document.dsl` — public DSL builders
- `com.demcha.compose.document.node` — semantic node records
- `com.demcha.compose.document.style`, `document.table`, `document.image`, `document.output` — public value types
- `com.demcha.compose.document.layout` — canonical functional layout pipeline
- `com.demcha.compose.document.backend.fixed` — the backend-neutral fixed-layout SPI (`FixedLayoutBackend`, `FixedLayoutBackendProvider`); implement it to add an output format
- `com.demcha.compose.document.backend.fixed.pdf` — PDF fixed-layout backend
- `com.demcha.compose.document.backend.fixed.pptx` — PPTX fixed-layout backend (`@Beta`)
- `com.demcha.compose.document.backend.semantic` — semantic export SPI, the DOCX exporter, and the legacy PPTX manifest
- `com.demcha.compose.document.templates` — built-in templates and data
- `com.demcha.compose.engine` — internal shared engine foundation under the canonical surface; not part of the recommended public API
- `com.demcha.compose.font` — public font registry

Please treat these names as the current source of truth in code, tests, examples, and docs. Do not introduce aliases or partial fallback imports.
