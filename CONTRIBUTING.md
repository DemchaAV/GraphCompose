# Contributing

Thanks for helping improve GraphCompose.

## Before you start

Read these files first:

- [README.md](./README.md)
- [docs/architecture.md](./docs/architecture.md)
- [docs/implementation-guide.md](./docs/implementation-guide.md)
- [docs/benchmarks.md](./docs/benchmarks.md) when you touch benchmark tooling, render hot paths, layout hot paths, or performance-facing docs

They explain the current public surface, the engine/template split, and the recommended extension points.

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
   Use `feature/...` for new functionality, `fix/...` for bug fixes, and `docs/...` for documentation-only changes.
3. **Commit small, focused changes.** Each commit message should describe the *why*, not just the *what*. Recent commits on `develop` (`Prepare v1.5.0 release`, `Align public docs with the canonical surface`) are reasonable length and structure templates.
4. **Run the validation gate locally** before opening a PR:
   ```bash
   ./mvnw -B -ntp clean verify
   ```
   This runs the architecture-and-documentation guards plus the full test suite. The same gate runs in CI on every PR.
5. **Push** your feature branch to your fork and open a pull request against `develop` on `DemchaAV/GraphCompose`. Reference any related issue and describe the user-visible change in the PR body.
6. **CI runs automatically.** The required status checks are `Architecture and Documentation Guards` and `Build and run tests`. The PR cannot merge into a protected branch until both are green.
7. **Address review comments**, then squash any fixup commits before merge. The maintainer merges through GitHub once review is complete.

### Branch protection

`main` is protected:

- pull request required (no direct pushes)
- both CI status checks must pass before merge
- linear history is enforced (squash or rebase, no merge commits)
- force pushes and branch deletion are disabled

`develop` accepts feature-branch PRs from contributors. The maintainer may push directly to `develop` for solo-driven release prep work; external contributions still flow through PRs.

### Release flow

1. Release prep lands on `develop` &mdash; version bump in `pom.xml` + `examples/pom.xml`, fresh CHANGELOG entry, README install snippets refreshed, migration guide updated when needed.
2. The maintainer merges `develop` into `main` via pull request &mdash; this is the only path a commit takes to reach `main`.
3. The maintainer tags the release on `main` (`vX.Y.Z`) and creates the GitHub release with notes copied from the matching `CHANGELOG.md` section.
4. JitPack picks up the new tag automatically; the `Installation` block in the README is the consumer-facing source of truth.

See [docs/release-process.md](./docs/release-process.md) for the full checklist.

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
  Internal ECS engine kept for the legacy template path. Not part of the recommended public API
- `src/main/java/com/demcha/compose/font`
  Public font registry, `FontName`, default fonts, `FontShowcase`
- `src/test/java/com/demcha/documentation/*`
  Examples used to keep README/documentation snippets honest
- `src/test/java/com/demcha/compose/engine/integration/*`
  End-to-end behaviour checks for the legacy ECS layout, pagination, and rendering paths
- `src/test/java/com/demcha/compose/document/*`
  Canonical API, DSL, layout, backend, and template tests
- `assets/readme/*`
  Screenshots used by the README

## Recommended workflow

1. Start with the smallest change that solves one problem.
2. Keep structural cleanup separate from behavior changes whenever possible.
3. If you touch public examples or screenshots, update the related docs in the same change.
4. Run the smallest relevant tests while iterating, then run `./mvnw -B -ntp clean verify` before opening a pull request.
5. For quick visual iteration on a template, you can use the experimental live preview tool in test scope by running [GraphComposeDevTool.java](./src/test/java/com/demcha/compose/devtool/GraphComposeDevTool.java) and editing [LivePreviewProvider.java](./src/test/java/com/demcha/preview/LivePreviewProvider.java).

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

- Constructor takes a `BusinessTheme` (or `CvTheme` for CV
  templates). Provide a no-arg overload that picks a default theme.
- Compose against `DocumentDsl` — no PDF-specific imports.
- Route every visible token through `theme.palette()` /
  `theme.text()` / `theme.spacing()` / `theme.table()`.
- Reference: `InvoiceTemplateV2`, `ProposalTemplateV2`. Read
  [docs/template-authoring.md](./docs/template-authoring.md) before
  starting.

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
- [docs/architecture.md](./docs/architecture.md)
- [docs/implementation-guide.md](./docs/implementation-guide.md)

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
- For concrete templates:
  [BuiltInTemplateRenderTest.java](./src/test/java/com/demcha/compose/document/templates/builtins/BuiltInTemplateRenderTest.java)
  [CvTemplateV1LayoutSnapshotTest.java](./src/test/java/com/demcha/compose/document/templates/builtins/CvTemplateV1LayoutSnapshotTest.java)

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
- If you add a new extension point or contribution pattern, update [README.md](./README.md), [docs/architecture.md](./docs/architecture.md), and [docs/implementation-guide.md](./docs/implementation-guide.md) as part of the same change.
- If you change benchmark flow, benchmark artifact layout, or diff selection rules, update [README.md](./README.md) and [docs/benchmarks.md](./docs/benchmarks.md) in the same change.
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
- `com.demcha.compose.engine` — internal ECS engine; kept for the legacy template path, not part of the recommended public API
- `com.demcha.compose.font` — public font registry

Please treat these names as the current source of truth in code, tests, examples, and docs. Do not introduce aliases or partial fallback imports.
