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

## Repository map

- `src/main/java/com/demcha/compose/*`
  Core engine: entities, builders, layout, pagination, render systems
- `src/main/java/com/demcha/compose/document/templates/*`
  Canonical template layer for built-ins, DTOs, themes, registries, and scene composition helpers
- `src/test/java/com/demcha/documentation/*`
  Examples used to keep README/documentation snippets honest
- `src/test/java/com/demcha/integration/*`
  End-to-end behavior checks for layout, pagination, rendering, and containers
- `src/test/java/com/demcha/compose/document/templates/*`
  Canonical template API, render, layout, and boundary tests
- `assets/readme/*`
  Screenshots used by the README

## Recommended workflow

1. Start with the smallest change that solves one problem.
2. Keep structural cleanup separate from behavior changes whenever possible.
3. If you touch public examples or screenshots, update the related docs in the same change.
4. Run the smallest relevant tests while iterating, then run `./mvnw -B -ntp clean verify` before opening a pull request.
5. For quick visual iteration on a template, you can use the experimental live preview tool in test scope by running [GraphComposeDevTool.java](./src/test/java/com/demcha/compose/devtool/GraphComposeDevTool.java) and editing [LivePreviewProvider.java](./src/test/java/com/demcha/preview/LivePreviewProvider.java).

## Contributor architecture rules

These rules reflect the current engine design and should be treated as project policy, not just suggestions.

- Engine render markers must implement backend-neutral `Render`. Do not add backend-specific render interfaces back into `layout_core/components`.
- Rendering logic for PDF belongs in `src/main/java/com/demcha/compose/layout_core/system/implemented_systems/pdf_systems/handlers/`.
- Backend-only helper objects belong in renderer-owned helper packages such as `...pdf_systems/helpers/`, not in `components/renderable`.
- Builders and layout code must get text width and line metrics from `TextMeasurementSystem`, not from `LayoutSystem -> RenderingSystem`, `PdfFont`, or PDFBox objects.
- Keep `src/main/java/com/demcha/compose/layout_core/components/*` free of `org.apache.pdfbox` and `...implemented_systems.pdf_systems` imports.
- When you add a new render marker, register its handler in `PdfRenderingSystemECS` and add/update dispatch coverage.

For built-in templates, use the template-layer split as project policy as well:

- public template contracts are compose-first: prefer `compose(DocumentSession, ...)`
- built-in template classes in `src/main/java/com/demcha/compose/document/templates/builtins/` should stay thin public facades over reusable scene composers
- document structure and scene assembly belong in dedicated backend-neutral scene composer classes under `...document.templates.support`
- keep PDF-only setup in the document session/backend layer rather than inside template composers
- do not import `PDDocument`, `PDPage`, `PDRectangle`, or low-level PDF composer types into scene composer classes
- new README snippets, runnable examples, and integration docs should show `compose(DocumentSession, ...)`
- do not reintroduce the removed low-level PDF entry point, low-level PDF composer types, or the removed template namespace into public-facing usage docs or runnable examples

The current guard rails for these rules live in:

- [EnginePdfBoundaryTest.java](./src/test/java/com/demcha/compose/layout_core/architecture/EnginePdfBoundaryTest.java)
- [CanonicalTemplateComposerPdfBoundaryTest.java](./src/test/java/com/demcha/compose/document/templates/architecture/CanonicalTemplateComposerPdfBoundaryTest.java)
- [PdfRenderInterfaceGuardTest.java](./src/test/java/com/demcha/compose/layout_core/system/implemented_systems/pdf_systems/PdfRenderInterfaceGuardTest.java)
- [PdfRenderingSystemECSDispatchTest.java](./src/test/java/com/demcha/compose/layout_core/system/implemented_systems/pdf_systems/PdfRenderingSystemECSDispatchTest.java)
- [DocumentationExamplesTest.java](./src/test/java/com/demcha/documentation/DocumentationExamplesTest.java)

Keep the entity core thin as project policy as well:

- `Entity` should stay an identity-plus-components object with compatibility delegates, not a home for new layout math or pagination mutation rules
- geometry reads belong in `EntityBounds`
- parent container size and page-shift propagation belong in `ParentContainerUpdater`
- existing `Entity.bounding*` and `Entity.updateParentContainer*` methods remain deprecated compatibility wrappers and should not be copied into new code
- render-order optimizations belong in rendering helpers such as `EntityRenderOrder`, not in `Entity`

## Adding or changing engine objects

If you add a new engine object, decide first what kind of object it is.

### Use the right builder base class

- Extend [EmptyBox.java](./src/main/java/com/demcha/compose/layout_core/components/containers/abstract_builders/EmptyBox.java) for a leaf object that does not manage children.
- Extend [ShapeBuilderBase.java](./src/main/java/com/demcha/compose/layout_core/components/containers/abstract_builders/ShapeBuilderBase.java) for a leaf object that needs common shape behavior such as fill, stroke, or corner radius.
- Extend [ContainerBuilder.java](./src/main/java/com/demcha/compose/layout_core/components/containers/abstract_builders/ContainerBuilder.java) for a parent object that owns child entities and participates in container layout.

### Make the object participate in the engine

For a visible object, the implementation usually needs all of the following:

- a builder that creates and configures the entity
- the components that describe content and style
- a size signal such as `ContentSize`
- layout metadata such as `Anchor`, `Margin`, `Padding`, and parent/child links when needed
- a backend-neutral renderable marker plus a backend-owned render handler

Marker rule of thumb:

- add `Expendable` only to parent-like boxes that should grow because of child content
- add `Breakable` only to entities whose own content may continue across pages
- do not treat `Expendable` as a pagination flag

The layout pass is driven by components and entity relationships, not by builder classes directly. See:

- [LayoutSystem.java](./src/main/java/com/demcha/compose/layout_core/system/LayoutSystem.java)
- [PdfRenderingSystemECS.java](./src/main/java/com/demcha/compose/layout_core/system/implemented_systems/pdf_systems/PdfRenderingSystemECS.java)

For text-heavy objects, also read:

- [TextMeasurementSystem.java](./src/main/java/com/demcha/compose/layout_core/system/interfaces/TextMeasurementSystem.java)
- [docs/architecture.md](./docs/architecture.md)
- [docs/implementation-guide.md](./docs/implementation-guide.md)

If the object should be available from `composer.componentBuilder()`, add a factory method to [ComponentBuilder.java](./src/main/java/com/demcha/compose/layout_core/components/components_builders/ComponentBuilder.java).

### Useful examples to copy

- Leaf builder with measured content:
  [TextBuilder.java](./src/main/java/com/demcha/compose/layout_core/components/components_builders/TextBuilder.java)
- Shape-style builder:
  [RectangleBuilder.java](./src/main/java/com/demcha/compose/layout_core/components/components_builders/RectangleBuilder.java)
- Container builder:
  [ModuleBuilder.java](./src/main/java/com/demcha/compose/layout_core/components/components_builders/ModuleBuilder.java)
- Template-level composition helper:
  [CvTemplateComposer.java](./src/main/java/com/demcha/compose/document/templates/support/CvTemplateComposer.java)

## Testing expectations

Choose the smallest tests that match the change:

- For README or docs examples:
  [DocumentationExamplesTest.java](./src/test/java/com/demcha/documentation/DocumentationExamplesTest.java)
- For engine/backend boundary changes:
  [EnginePdfBoundaryTest.java](./src/test/java/com/demcha/compose/layout_core/architecture/EnginePdfBoundaryTest.java)
  [PdfRenderInterfaceGuardTest.java](./src/test/java/com/demcha/compose/layout_core/system/implemented_systems/pdf_systems/PdfRenderInterfaceGuardTest.java)
- For public builder registration or factory changes:
  [ComponentBuilderTest.java](./src/test/java/com/demcha/compose/layout_core/components/ComponentBuilderTest.java)
- For render-marker dispatch changes:
  [PdfRenderingSystemECSDispatchTest.java](./src/test/java/com/demcha/compose/layout_core/system/implemented_systems/pdf_systems/PdfRenderingSystemECSDispatchTest.java)
- For layout/positioning behavior:
  [ComputedPositionTest.java](./src/test/java/com/demcha/components/layout/ComputedPositionTest.java)
- For pagination and multi-page behavior:
  [PageBreakerIntegrationTest.java](./src/test/java/com/demcha/integration/PageBreakerIntegrationTest.java)
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
- Prefer additive or backward-compatible changes when extending builder APIs or template contracts.
- If a rename or move could break imports, resource paths, or examples, either update every affected reference in the same change or leave it as a documented follow-up.

## Documentation and screenshots

- Keep [README.md](./README.md) aligned with the tested examples.
- Keep benchmark values clearly dated when they are refreshed.
- Keep `assets/readme/*` screenshots consistent with the current render outputs.
- If you add a new extension point or contribution pattern, update [README.md](./README.md), [docs/architecture.md](./docs/architecture.md), and [docs/implementation-guide.md](./docs/implementation-guide.md) as part of the same change.
- If you change benchmark flow, benchmark artifact layout, or diff selection rules, update [README.md](./README.md) and [docs/benchmarks.md](./docs/benchmarks.md) in the same change.
- Visual PDF artifacts are grouped under `target/visual-tests/clean/*` and `target/visual-tests/guides/*` so guide-line renders are easy to find separately from clean outputs.

## Package naming

The repository now uses the normalized package roots `layout_core`, `word_systems`, and `com.demcha.compose.document.templates`.

Please treat these names as the current source of truth in code, tests, examples, and docs. Do not introduce aliases or partial fallback imports.
