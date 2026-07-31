# Contributing

Thanks for helping improve GraphCompose.

## Before you start

Read these files first:

- [README.md](./README.md)
- [docs/architecture/overview.md](./docs/architecture/overview.md)
- [docs/architecture/package-map.md](./docs/architecture/package-map.md) — what lives in which package, and which of them are internal
- [docs/operations/benchmarks.md](./docs/operations/benchmarks.md) when you touch benchmark tooling, render hot paths, layout hot paths, or performance-facing docs

[docs/contributing/implementation-guide.md](./docs/contributing/implementation-guide.md)
goes deeper on the engine, but parts of it still describe the execution layer that
2.0 removed — read `overview.md` and `package-map.md` first and treat the guide as
background until it is rewritten.

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
- Run the engine-resident guard suite with `./mvnw -B -ntp "-Dtest=EnginePdfBoundaryTest,DocumentationCoverageTest,CanonicalSurfaceGuardTest,PackageMapGuardTest,VersionConsistencyGuardTest,CiGuardListGuardTest" test -pl :graph-compose-core` — the same list CI runs. Every name must live in `graph-compose-core`: Surefire drops a name that matches nothing as long as a sibling matches, so a guard that lives elsewhere would silently not run (`CiGuardListGuardTest` fails the build if one creeps in).
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
3. **Commit small, focused changes.** Each commit message should describe the *why*, not just the *what*. Recent commits on `develop` (`Prepare v1.5.0 release`, `Align public docs with the canonical surface`) are reasonable length and structure templates.
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
   - `Binary Compatibility` &mdash; PR-only japicmp diff of the `graph-compose-core` surface
   - `Performance Smoke Check` &mdash; PR-only coarse benchmark to catch performance regressions
   - `CI Gate` &mdash; single aggregate status check that is green when every job that ran passed

   **Selective on pull requests:** a `dorny/paths-filter` step skips the heavy jobs when a PR touches nothing that affects the build. Markdown counts as a build input, so a **docs-only PR still runs the reactor** &mdash; on the baseline JDK alone, and without example generation &mdash; because that is where the guards compiling the published snippets live. `Binary Compatibility` runs only when the core module changed, and the `Performance Smoke Check` only when core / render-pdf / templates changed. Pushes to `develop` / `main` (and manual dispatch) always run the full gate. Point branch protection at **`CI Gate`** + **`Architecture and Documentation Guards`** rather than the individual matrix legs, so a docs-only PR is not left waiting on a skipped check.

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
3. **Maven Central** picks up the new tag automatically via [`.github/workflows/publish.yml`](./.github/workflows/publish.yml) &mdash; the workflow re-runs `mvnw verify` at the tagged commit, signs the four artefacts (main / sources / javadoc / pom) with the repo's GPG key, and uploads via the `central-publishing-maven-plugin`. Hyphenated tags (`-rc`, `-alpha`, `-beta`) are skipped on Central; they ship only to the GitHub Release pre-release surface. Javadocs auto-publish to [javadoc.io/doc/io.github.demchaav/graph-compose](https://javadoc.io/doc/io.github.demchaav/graph-compose) shortly after each Central release.
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
  Built-in templates (CV, cover letter, invoice, proposal, weekly schedule), DTOs, themes, registries, and scene composition helpers
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
- Built-in templates in `...document.templates.builtins` stay thin
  public facades over reusable scene composers in
  `...document.templates.support`. Keep PDF-only setup in the document
  session/backend layer rather than inside template composers, and do
  not import `PDDocument`, `PDPage`, `PDRectangle`, or low-level PDF
  composer types into scene composer classes.
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
Five sub-packages (`data/` / `theme/` / `components/` / `widgets/`
/ `presets/`), each with a clear contract, over the shared
`templates.core` layer.

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
- [docs/contributing/implementation-guide.md](./docs/contributing/implementation-guide.md)

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
- If you add a new extension point or contribution pattern, update [README.md](./README.md), [docs/architecture/overview.md](./docs/architecture/overview.md), and [docs/contributing/implementation-guide.md](./docs/contributing/implementation-guide.md) as part of the same change.
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
- `com.demcha.compose.document.backend.fixed.pdf` — PDF fixed-layout backend
- `com.demcha.compose.document.backend.fixed.pptx` — PPTX fixed-layout backend (`@Beta`)
- `com.demcha.compose.document.backend.semantic` — semantic export SPI, the DOCX exporter, and the legacy PPTX manifest
- `com.demcha.compose.document.templates` — built-in templates and data
- `com.demcha.compose.engine` — internal shared engine foundation under the canonical surface; not part of the recommended public API
- `com.demcha.compose.font` — public font registry

Please treat these names as the current source of truth in code, tests, examples, and docs. Do not introduce aliases or partial fallback imports.
