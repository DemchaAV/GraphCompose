# Contributing

Thanks for helping improve GraphCompose.

## Before you start

Read these files first:

- [README.md](./README.md)
- [docs/architecture/overview.md](./docs/architecture/overview.md)
- [docs/contributing/implementation-guide.md](./docs/contributing/implementation-guide.md)
- [docs/operations/benchmarks.md](./docs/operations/benchmarks.md) when you touch benchmark tooling, render hot paths, layout hot paths, or performance-facing docs

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

- The blocking validation gate for repository work is `./mvnw -B -ntp clean verify`.
- Run the guard-focused suite with `./mvnw -B -ntp "-Dtest=EnginePdfBoundaryTest,CanonicalTemplateComposerPdfBoundaryTest,PdfRenderInterfaceGuardTest,PdfRenderingSystemECSDispatchTest,DocumentationCoverageTest,DocumentationExamplesTest,CanonicalSurfaceGuardTest,TemplateComposeApiTest" test`.
- Run a focused documentation sanity check with `./mvnw -B -ntp "-Dtest=DocumentationExamplesTest" test`.
- Run the local benchmark wrapper with `powershell -ExecutionPolicy Bypass -File .\scripts\run-benchmarks.ps1` when you change performance-sensitive code or benchmark tooling.

## How to propose changes

GraphCompose follows a fork &rarr; feature branch &rarr; pull request flow. External contributions land on `develop` (the working branch); `main` is the public stable line and only accepts release merges from `develop`.

### Contribution flow

1. **Fork** the repository on GitHub and clone your fork locally.
2. **Create a feature branch** from `develop`:
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
5. **Push** your feature branch to your fork and open a pull request against `develop` on `DemchaAV/GraphCompose`. Reference any related issue and describe the user-visible change in the PR body.
6. **CI runs automatically.** Active jobs:
   - `Architecture and Documentation Guards` &mdash; fast canonical / engine-boundary guard tests, fail-first gate
   - `Build and run tests (JDK 17)`, `(JDK 21)`, `(JDK 25)` &mdash; full `mvnw verify` in parallel matrix across the supported JVMs
   - `Examples Generation Smoke Test` &mdash; regenerates all 26 runnable examples and uploads the PDFs as a CI artifact
   - `Performance Smoke Check` &mdash; PR-only coarse benchmark to catch performance regressions

   The PR cannot merge into a protected branch until all required checks are green.
7. **Address review comments**, then squash any fixup commits before merge. The maintainer merges through GitHub once review is complete.

### Branch protection

`main` is protected:

- pull request required (no direct pushes)
- both CI status checks must pass before merge
- linear history is enforced (squash or rebase, no merge commits)
- force pushes and branch deletion are disabled

`develop` accepts feature-branch PRs from contributors. The maintainer may push directly to `develop` for solo-driven release prep work; external contributions still flow through PRs.

### Release flow

1. **Release prep** lands on `develop` &mdash; version bumps propagate via `aggregator/pom.xml` to all modules in one pass; fresh CHANGELOG entry; migration guide for minor releases. **README install snippet stays pinned to the previously published version** until Maven Central confirms the new artifact, otherwise consumers copying the snippet during the publish window hit a 404.
2. **`scripts/cut-release.ps1 -Version <X.Y.Z>`** automates the bump + CHANGELOG date + commit + tag + push from `develop`. The maintainer fast-forwards `main` from `develop` after the tag lands (`git push origin develop:main`).
3. **Maven Central** picks up the new tag automatically via [`.github/workflows/publish.yml`](./.github/workflows/publish.yml) &mdash; the workflow re-runs `mvnw verify` at the tagged commit, signs the four artefacts (main / sources / javadoc / pom) with the repo's GPG key, and uploads via the `central-publishing-maven-plugin`. Hyphenated tags (`-rc`, `-alpha`, `-beta`) are skipped on Central; they ship only to the GitHub Release pre-release surface. Javadocs auto-publish to [javadoc.io/doc/io.github.demchaav/graph-compose](https://javadoc.io/doc/io.github.demchaav/graph-compose) shortly after each Central release.
4. **GitHub Release** is created with notes from the matching `CHANGELOG.md` section.

See [docs/contributing/release-process.md](./docs/contributing/release-process.md) for the full checklist (audit gates, hotfix protocol, lessons learned).

## Repository map

- `src/main/java/com/demcha/compose/document/api`, `document.dsl`, `document.node`, `document.style`, `document.table`, `document.image`, `document.output`, `document.exceptions`, `document.snapshot`
  Public canonical authoring surface — `DocumentSession`, the DSL builders, semantic node records, public style values, table types, image types, backend-neutral output options (metadata / watermark / protection / header-footer), and snapshot DTOs
- `src/main/java/com/demcha/compose/document/layout`
  Canonical functional layout pipeline: `LayoutCompiler`, `BuiltInNodeDefinitions`, `TableLayoutSupport`, `PreparedNode`, `PlacedFragment`
- `src/main/java/com/demcha/compose/document/backend/fixed/pdf`
  PDF backend: `PdfFixedLayoutBackend`, fragment handlers, and the option translators that bridge canonical types to PDFBox
- `src/main/java/com/demcha/compose/document/backend/semantic`
  Semantic exporters: `DocxSemanticBackend` (Apache POI based), `PptxSemanticBackend` (manifest skeleton)
- `src/main/java/com/demcha/compose/document/templates/*`
  Built-in templates (CV, cover letter, invoice, proposal, weekly schedule), DTOs, themes, registries, and scene composition helpers
- `src/main/java/com/demcha/compose/engine/*`
  Internal shared engine foundation under the canonical surface (measure, paginate, place, render). Not part of the recommended public API
- `src/main/java/com/demcha/compose/font`
  Public font registry, `FontName`, default fonts, `FontShowcase`
- `src/test/java/com/demcha/documentation/*`
  Examples used to keep README/documentation snippets honest
- `src/test/java/com/demcha/compose/engine/integration/*`
  End-to-end behaviour checks for the engine foundation's layout, pagination, and rendering paths
- `src/test/java/com/demcha/compose/document/*`
  Canonical API, DSL, layout, backend, and template tests
- `assets/readme/*`
  Screenshots used by the README

## Recommended workflow

1. Start with the smallest change that solves one problem.
2. Keep structural cleanup separate from behavior changes whenever possible.
3. If you touch public examples or screenshots, update the related docs in the same change.
4. Run the smallest relevant tests while iterating, then run `./mvnw -B -ntp clean verify` before opening a pull request.
5. For quick visual iteration on a template, run [GraphComposeDevTool.java](./src/test/java/com/demcha/compose/devtool/GraphComposeDevTool.java) in test scope &mdash; it hot-reloads the rendered PDF as you edit your template source.

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
- PDF rendering logic lives in
  `src/main/java/com/demcha/compose/engine/render/pdf/handlers/`.
  Backend-only helper objects live in
  `com.demcha.compose.engine.render.pdf.helpers`, not in
  `components/renderable`.
- Builders and layout code get text width and line metrics from
  `TextMeasurementSystem`, not from
  `LayoutSystem -> RenderingSystem`, `PdfFont`, or PDFBox objects.
- Keep `src/main/java/com/demcha/compose/engine/components/*` free of
  `org.apache.pdfbox` and `com.demcha.compose.engine.render.pdf`
  imports.
- When you add a new render marker, register its handler in
  `PdfRenderingSystemECS` and add or update dispatch coverage.

Keep the entity core thin:

- `Entity` stays an identity-plus-components object with compatibility
  delegates. It is not a home for new layout math or pagination
  mutation rules.
- Geometry reads live in `EntityBounds`. Parent-container size and
  page-shift propagation live in `ParentContainerUpdater`.
- `Entity.bounding*` and `Entity.updateParentContainer*` are
  deprecated compatibility wrappers; do not copy them into new code.
- Render-order optimizations live in rendering helpers such as
  `EntityRenderOrder`, not in `Entity`.

### Guard rails

The rules above are enforced by tests:

- canonical surface guards:
  [CanonicalSurfaceGuardTest.java](./src/test/java/com/demcha/compose/document/architecture/CanonicalSurfaceGuardTest.java),
  [TemplateComposeApiTest.java](./src/test/java/com/demcha/compose/document/templates/architecture/TemplateComposeApiTest.java),
  [PublicApiNoEngineLeakTest.java](./src/test/java/com/demcha/compose/document/architecture/PublicApiNoEngineLeakTest.java),
  [SemanticLayerNoPdfBoxDependencyTest.java](./src/test/java/com/demcha/compose/document/architecture/SemanticLayerNoPdfBoxDependencyTest.java),
  [DocumentationExamplesTest.java](./src/test/java/com/demcha/documentation/DocumentationExamplesTest.java),
  [DocumentationCoverageTest.java](./src/test/java/com/demcha/documentation/DocumentationCoverageTest.java)
- engine internals guards:
  [EnginePdfBoundaryTest.java](./src/test/java/com/demcha/compose/engine/architecture/EnginePdfBoundaryTest.java),
  [CanonicalTemplateComposerPdfBoundaryTest.java](./src/test/java/com/demcha/compose/document/templates/architecture/CanonicalTemplateComposerPdfBoundaryTest.java),
  [PdfRenderInterfaceGuardTest.java](./src/test/java/com/demcha/compose/engine/render/pdf/PdfRenderInterfaceGuardTest.java),
  [PdfRenderingSystemECSDispatchTest.java](./src/test/java/com/demcha/compose/engine/render/pdf/PdfRenderingSystemECSDispatchTest.java)

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

GraphCompose supports two template authoring patterns. Pick based
on whether you're extending an existing template family or building
a new one.

**For a NEW template family from scratch** (invoice-v2,
cover-letter-v2, report-v2, anything not yet in `cv/v2/`) — follow
the canonical layered architecture documented in
[**docs/templates/v2-layered/contributor-guide.md**](./docs/templates/v2-layered/contributor-guide.md).
Five sub-packages (`data/` / `theme/` / `components/` / `widgets/`
/ `presets/`), each with a clear contract. CV v2
(`com.demcha.compose.document.templates.cv.v2`) is the reference
implementation; read it before starting yours.

**For a new preset inside an existing v1-classic family** (a new CV
variant alongside `ModernProfessional`, a new invoice preset
alongside `InvoiceTemplateV2`):

- Constructor takes a `BusinessTheme` (or `CvTheme` for CV
  templates). Provide a no-arg overload that picks a default theme.
- Compose against `DocumentDsl` — no PDF-specific imports.
- Route every visible token through `theme.palette()` /
  `theme.text()` / `theme.spacing()` / `theme.table()`.
- Reference: `InvoiceTemplateV2`, `ProposalTemplateV2`. Read
  [docs/templates/v1-classic/authoring.md](./docs/templates/v1-classic/authoring.md) before
  starting.

> 📚 **Map of template docs**:
> [docs/README.md](./docs/README.md#templates) lists every template
> guide with a one-line description so you can pick the right one
> fast.

### New engine internal primitive

If you are extending the engine foundation itself (a new render
marker, a new layout system, a new render-pass session):

- Decide first whether the feature belongs on the public surface as
  a `DocumentNode` instead. If yes, see "New public node" above and
  treat the engine work as plumbing, not as new public ECS surface.
- For genuine engine primitives, add the engine content / style /
  layout component plus a backend-neutral renderable marker plus a
  backend-owned render handler.
- Marker rule of thumb:
  - add `Expendable` only to parent-like boxes that should grow
    because of child content
  - add `Breakable` only to entities whose own content may continue
    across pages
  - do not treat `Expendable` as a pagination flag

For text-heavy primitives, also read:

- [TextMeasurementSystem.java](./src/main/java/com/demcha/compose/engine/measurement/TextMeasurementSystem.java)
- [docs/architecture/overview.md](./docs/architecture/overview.md)
- [docs/contributing/implementation-guide.md](./docs/contributing/implementation-guide.md)

If the primitive should be available to application developers,
expose it through `DocumentDsl` and a public `DocumentNode`, not a
low-level test harness.

## Testing expectations

Choose the smallest tests that match the change:

- For README or docs examples:
  [DocumentationExamplesTest.java](./src/test/java/com/demcha/documentation/DocumentationExamplesTest.java)
- For engine/backend boundary changes:
  [EnginePdfBoundaryTest.java](./src/test/java/com/demcha/compose/engine/architecture/EnginePdfBoundaryTest.java)
  [PdfRenderInterfaceGuardTest.java](./src/test/java/com/demcha/compose/engine/render/pdf/PdfRenderInterfaceGuardTest.java)
- For low-level test harness changes:
  [ComponentBuilderTest.java](./src/test/java/com/demcha/compose/engine/components/ComponentBuilderTest.java)
- For render-marker dispatch changes:
  [PdfRenderingSystemECSDispatchTest.java](./src/test/java/com/demcha/compose/engine/render/pdf/PdfRenderingSystemECSDispatchTest.java)
- For layout/positioning behavior:
  [ComputedPositionTest.java](./src/test/java/com/demcha/compose/engine/components/layout/ComputedPositionTest.java)
- For pagination and multi-page behavior:
  [PageBreakerIntegrationTest.java](./src/test/java/com/demcha/compose/engine/integration/PageBreakerIntegrationTest.java)
- For Templates v2 CV / cover-letter presets:
  [PresetVisualParityTest.java (CV)](./src/test/java/com/demcha/compose/document/templates/cv/presets/PresetVisualParityTest.java)
  [PresetVisualParityTest.java (cover letter)](./src/test/java/com/demcha/compose/document/templates/coverletter/presets/PresetVisualParityTest.java)
  [PresetLayoutSnapshotTest.java](./src/test/java/com/demcha/compose/document/templates/cv/presets/PresetLayoutSnapshotTest.java)

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
- `com.demcha.compose.document.backend.semantic` — DOCX / PPTX semantic backends
- `com.demcha.compose.document.templates` — built-in templates and data
- `com.demcha.compose.engine` — internal shared engine foundation under the canonical surface; not part of the recommended public API
- `com.demcha.compose.font` — public font registry

Please treat these names as the current source of truth in code, tests, examples, and docs. Do not introduce aliases or partial fallback imports.
