# Contributing

Thanks for helping improve GraphCompose.

## Before you start

Read these files first:

- [README.md](./README.md)
- [docs/architecture.md](./docs/architecture.md)
- [docs/implementation-guide.md](./docs/implementation-guide.md)

They explain the current public surface, the engine/template split, and the recommended extension points.

## Build and test

- Build the library with `mvn package`.
- Run the full test suite with `mvn test`.
- Run a focused documentation sanity check with `mvn -Dtest=DocumentationExamplesTest test`.

## Repository map

- `src/main/java/com/demcha/compose/*`
  Core engine: entities, builders, layout, pagination, render systems
- `src/main/java/com/demcha/templates/*`
  Higher-level template layer for CV and cover-letter composition
- `src/test/java/com/demcha/documentation/*`
  Examples used to keep README/documentation snippets honest
- `src/test/java/com/demcha/integration/*`
  End-to-end behavior checks for layout, pagination, rendering, and containers
- `src/test/java/com/demcha/templates/*`
  Template rendering tests
- `assets/readme/*`
  Screenshots used by the README

## Recommended workflow

1. Start with the smallest change that solves one problem.
2. Keep structural cleanup separate from behavior changes whenever possible.
3. If you touch public examples or screenshots, update the related docs in the same change.
4. Run the smallest relevant tests while iterating, then run `mvn test` before opening a pull request.

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
- a renderable component that the renderer understands

Marker rule of thumb:

- add `Expendable` only to parent-like boxes that should grow because of child content
- add `Breakable` only to entities whose own content may continue across pages
- do not treat `Expendable` as a pagination flag

The layout pass is driven by components and entity relationships, not by builder classes directly. See:

- [LayoutSystem.java](./src/main/java/com/demcha/compose/layout_core/system/LayoutSystem.java)
- [PdfRenderingSystemECS.java](./src/main/java/com/demcha/compose/layout_core/system/implemented_systems/pdf_systems/PdfRenderingSystemECS.java)

If the object should be available from `composer.componentBuilder()`, add a factory method to [ComponentBuilder.java](./src/main/java/com/demcha/compose/layout_core/components/components_builders/ComponentBuilder.java).

### Useful examples to copy

- Leaf builder with measured content:
  [TextBuilder.java](./src/main/java/com/demcha/compose/layout_core/components/components_builders/TextBuilder.java)
- Shape-style builder:
  [RectangleBuilder.java](./src/main/java/com/demcha/compose/layout_core/components/components_builders/RectangleBuilder.java)
- Container builder:
  [ModuleBuilder.java](./src/main/java/com/demcha/compose/layout_core/components/components_builders/ModuleBuilder.java)
- Template-level composition helper:
  [TemplateBuilder.java](./src/main/java/com/demcha/templates/TemplateBuilder.java)

## Testing expectations

Choose the smallest tests that match the change:

- For README or docs examples:
  [DocumentationExamplesTest.java](./src/test/java/com/demcha/documentation/DocumentationExamplesTest.java)
- For public builder registration or factory changes:
  [ComponentBuilderTest.java](./src/test/java/com/demcha/compose/layout_core/components/ComponentBuilderTest.java)
- For layout/positioning behavior:
  [ComputedPositionTest.java](./src/test/java/com/demcha/components/layout/ComputedPositionTest.java)
- For pagination and multi-page behavior:
  [PageBreakerIntegrationTest.java](./src/test/java/com/demcha/integration/PageBreakerIntegrationTest.java)
- For concrete templates:
  [TemplateCV1RenderTest.java](./src/test/java/com/demcha/templates/cv_templates/TemplateCV1RenderTest.java)
  [CoverLetterTemplateV1Test.java](./src/test/java/com/demcha/templates/cover_letter/CoverLetterTemplateV1Test.java)

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
- If you add a new extension point or contribution pattern, update [docs/implementation-guide.md](./docs/implementation-guide.md) as part of the same change.
- Visual PDF artifacts are grouped under `target/visual-tests/clean/*` and `target/visual-tests/guides/*` so guide-line renders are easy to find separately from clean outputs.

## Package naming

The repository now uses the normalized package roots `layout_core`, `word_systems`, and `com.demcha.templates`.

Please treat these names as the current source of truth in code, tests, examples, and docs. Do not introduce aliases or partial fallback imports.
