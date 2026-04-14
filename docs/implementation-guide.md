# Implementation Guide

This guide explains how to add new objects and engine extensions in GraphCompose without fighting the current architecture.

## Mental model

GraphCompose does not render directly from builder calls.

The usual flow is:

1. a builder creates an `Entity`
2. the builder attaches components to that entity
3. the entity is registered in `EntityManager`
4. layout systems calculate size and placement
5. rendering systems inspect the entity's render component and draw it

That means a new object usually needs the right answer in four areas:

- what builder base class it should extend
- which components must be attached to the entity
- whether it participates in parent/child layout
- how it gets rendered

## Choose the right base class

### Extend `EmptyBox<T>` when

Use [EmptyBox.java](./../src/main/java/com/demcha/compose/layout_core/components/containers/abstract_builders/EmptyBox.java) when the new object is a leaf entity or a small custom object that does not manage children itself.

Examples in the codebase:

- [TextBuilder.java](./../src/main/java/com/demcha/compose/layout_core/components/components_builders/TextBuilder.java)
- [ImageBuilder.java](./../src/main/java/com/demcha/compose/layout_core/components/components_builders/ImageBuilder.java)
- [CircleBuilder.java](./../src/main/java/com/demcha/compose/layout_core/components/components_builders/CircleBuilder.java)
- [LineBuilder.java](./../src/main/java/com/demcha/compose/layout_core/components/components_builders/LineBuilder.java)
- [LinkBuilder.java](./../src/main/java/com/demcha/compose/layout_core/components/components_builders/LinkBuilder.java)
- [ElementBuilder.java](./../src/main/java/com/demcha/compose/layout_core/components/components_builders/ElementBuilder.java)

This is the right choice for the exact case you asked about: an object that does not expand into a child-owning container and just needs base entity functionality plus layout/render participation.

What `EmptyBox<T>` gives you:

- entity creation
- auto-generated `EntityName`
- fluent `addComponent(...)`
- parent/child helpers
- access to `EntityManager`
- default `build()` behavior through the builder hierarchy

### Extend `ShapeBuilderBase<T>` when

Use [ShapeBuilderBase.java](./../src/main/java/com/demcha/compose/layout_core/components/containers/abstract_builders/ShapeBuilderBase.java) when the object is still a leaf, but you want common shape helpers such as:

- fill color
- stroke
- corner radius

Examples:

- [RectangleBuilder.java](./../src/main/java/com/demcha/compose/layout_core/components/components_builders/RectangleBuilder.java)
- [ButtonBuilder.java](./../src/main/java/com/demcha/compose/layout_core/components/components_builders/ButtonBuilder.java)

### Extend `ContainerBuilder<T>` when

Use [ContainerBuilder.java](./../src/main/java/com/demcha/compose/layout_core/components/containers/abstract_builders/ContainerBuilder.java) when the new object owns child entities and participates in parent/child layout.

Examples:

- [HContainerBuilder.java](./../src/main/java/com/demcha/compose/layout_core/components/components_builders/HContainerBuilder.java)
- [VContainerBuilder.java](./../src/main/java/com/demcha/compose/layout_core/components/components_builders/VContainerBuilder.java)
- [ModuleBuilder.java](./../src/main/java/com/demcha/compose/layout_core/components/components_builders/ModuleBuilder.java)

Use this path when the object should call `addChild(...)` and arrange nested entities.

Special note for modules:

- `ModuleBuilder` is the semantic section primitive
- it resolves to the full available width of its parent minus its own horizontal margin
- it should usually live under a normal root `vContainer(...)` or `TemplateBuilder.pageFlow(...)`
- nested horizontal/vertical composition should happen inside the module through regular containers

## Minimum components a new object usually needs

### Render marker

If the object should render something visible, the entity needs a renderable marker component.

Examples:

- [TextComponent.java](./../src/main/java/com/demcha/compose/layout_core/components/renderable/TextComponent.java)
- [Rectangle.java](./../src/main/java/com/demcha/compose/layout_core/components/renderable/Rectangle.java)
- [ImageComponent.java](./../src/main/java/com/demcha/compose/layout_core/components/renderable/ImageComponent.java)

Those renderable components are render markers. Prefer keeping them backend-neutral and let renderer-owned handlers perform format-specific drawing.

### Engine markers with different jobs

There are three different ideas in the engine that are easy to mix up:

- render marker: tells the active renderer how to draw the entity
- `Expendable`: tells the container expansion phase that the parent box may grow to fit children
- `Breakable`: tells the page breaker that the entity's own content may continue across pages

Use them for different reasons:

- add a render marker when the object is visible
- add `Expendable` only when the entity is a true parent-like box that should resize because of child content
- add `Breakable` only when the entity itself can be split or continued during pagination

Examples:

- `Container` is both `Expendable` and `Breakable` because it owns children and may span pages
- `BlockText` is `Breakable` because its content can flow across pages
- `ImageComponent` is neither `Expendable` nor `Breakable`; it is a fixed leaf renderable
- `Circle` is a fixed leaf renderable like `ImageComponent`; it renders, but it should not be marked `Expendable`
- `Line` is also a fixed leaf renderable; it draws inside its resolved box and should stay non-breakable unless the engine gets a true multi-page line contract later

Important:

- `Expendable` is not a pagination flag
- `Breakable` is not a child-sizing flag
- if a long leaf object is not `Breakable`, the engine treats it as a single block and moves it to the next page when needed

Leaf parity rule:

- if two objects are conceptually fixed leaf renderables, such as `ImageComponent`, `Circle`, and `Line`, they should use the same layout contract
- that usually means the same kind of `ContentSize`, the same padding-aware inner draw area, and the same non-breakable pagination behavior
- if one of them behaves differently in containers or multi-page flow, first check the render/layout contract before changing pagination rules

### Content / style components

Attach the components that describe what the object is and how it should look.

Examples:

- `Text`, `TextStyle`
- `FillColor`, `Stroke`, `CornerRadius`
- `ImageData`

The renderer reads those components later during the render pass.

### Size component

If the engine needs to place the object, it usually needs a size signal.

The most common component is:

- [ContentSize.java](./../src/main/java/com/demcha/compose/layout_core/components/geometry/ContentSize.java)

For simple fixed-size objects, set `ContentSize` directly in the builder.

For measured objects, compute size in `build()` before the entity is registered.

Example:

- [TextBuilder.java](./../src/main/java/com/demcha/compose/layout_core/components/components_builders/TextBuilder.java) calls `TextComponent.autoMeasureText(...)` when auto-size is enabled.

### Layout components

The layout engine expects the usual layout metadata to be present when needed:

- `Anchor`
- `Margin`
- `Padding`
- `Align` for containers
- `ParentComponent` for child entities

You normally do not add `Placement` yourself. The layout system calculates placement later.

## Typical implementation recipes

### Case 1: add a new leaf object

Use this when the object is visible, does not manage children, and should behave like text/image/rectangle.

Steps:

1. create a builder extending `EmptyBox<T>` or `ShapeBuilderBase<T>`
2. in `initialize()`, attach the render marker component
3. add fluent builder methods that attach the data/style components
4. set or calculate `ContentSize`
5. register the entity through `build()`
6. add a factory method to [ComponentBuilder.java](./../src/main/java/com/demcha/compose/layout_core/components/components_builders/ComponentBuilder.java) if you want `composer.componentBuilder().yourObject()`

Leaf rule of thumb:

- most leaf renderables should not implement `Expendable`
- only implement `Breakable` if the leaf's content can really continue across pages

### Case 2: add a new container-like object

Use this when the object owns child entities and arranges them.

Steps:

1. extend `ContainerBuilder<T>`
2. add the render/container marker in `initialize()`
3. ensure the container has the alignment / axis semantics it needs
4. use `addChild(...)` to wire child entities
5. provide `ContentSize` or logic that lets layout compute it correctly

Container rule of thumb:

- containers that must resize around children usually need `Expendable`
- containers whose content may continue on another page usually need `Breakable`
- some containers need both markers
- if the container is a semantic section like `ModuleBuilder`, decide whether width should be inherited from the parent before letting child layout run

### Case 2.5: build a hybrid object with a breakable root and atomic leaf rows

Some engine objects look like containers from the outside, but still need their own leaf rendering contract inside.

`TableBuilder v1` is the current example:

- the table root is a breakable vertical container
- each row is a non-breakable leaf entity with explicit `ContentSize`
- each row renders all of its cells through a dedicated row renderable instead of exposing each cell as a separate child entity

This pattern is useful when:

- the parent object should flow across pages
- one logical child block must stay atomic
- rendering needs sibling-aware behavior, such as page-break separators

Why the table uses this contract:

- rows must move to the next page as units
- column widths are negotiated once at the table level
- cell borders and page-break separators are easier to render consistently from a row-level payload

Relevant files:

- [TableBuilder.java](./../src/main/java/com/demcha/compose/layout_core/components/components_builders/TableBuilder.java)
- [TableRow.java](./../src/main/java/com/demcha/compose/layout_core/components/renderable/TableRow.java)
- [TableCellBox.java](./../src/main/java/com/demcha/compose/layout_core/system/implemented_systems/pdf_systems/helpers/TableCellBox.java)
- [TableResolvedCell.java](./../src/main/java/com/demcha/compose/layout_core/components/content/table/TableResolvedCell.java)

Rule of thumb:

- make the root breakable only if the object as a whole can continue across pages
- keep logical row-like units as fixed leaves when the user would perceive splitting them as a bug
- if separators depend on where page fragments start or end, compute that in the render phase from resolved `Placement`, not only from builder-time metadata

### Case 3: add a purely logical helper object

If the object is not directly rendered and mostly groups behavior, it may not need a new render component at all. In that case you may only need:

- a builder/helper method
- a composition pattern over existing entities
- or a template-layer helper in `templates`

### Case 4: add or refactor a built-in template

Built-in templates now use an additive compose-first contract.

Use this split:

1. a public template interface in `com.demcha.templates.api` exposes `compose(DocumentComposer, ...)`
2. a built-in template class acts as a thin backend adapter
3. a dedicated scene builder under `com.demcha.templates.builtins` owns document composition

Practical rules:

- keep current `render(...): PDDocument` and `render(..., Path)` overloads only as deprecated compatibility adapters
- put `GraphCompose.pdf(...)`, page size selection, margins, `composer.toPDDocument()`, and `composer.build()` in the adapter class
- put the actual document structure, sections, tables, and block text assembly in the scene builder
- keep scene builders backend-neutral: no `PDDocument`, `PDPage`, `PDRectangle`, or `PdfComposer` imports
- keep public examples and integration docs compose-first: show `compose(DocumentComposer, ...)` before any deprecated `render(...)` convenience path
- pass theme or style collaborators into the scene builder constructor instead of hard-wiring backend assumptions into composition code
- when a built-in template already has a good scene split, extend that pattern instead of reintroducing backend-specific logic into the composition layer

Current guard rails:

- `TemplateScenePdfBoundaryTest` keeps `*SceneBuilder` classes free of `PDDocument`, `PDPage`, `PDRectangle`, and `PdfComposer`
- `TemplateComposeApiTest` keeps the compose-first public contract and deprecated compatibility adapters aligned

Rule of thumb:

- `*TemplateV1` should feel like a bridge to a backend
- `*SceneBuilder` should feel like the reusable document composition core

## Where rendering hooks in

The current PDF path works through renderable components and the PDF renderer system.

Preferred extension pattern for new backends:

1. keep engine components as format-neutral render markers
2. register a backend-specific `TextMeasurementSystem`
3. register renderer-side handlers for marker types
4. keep backend-only helper drawing in renderer-owned helper packages when the code is not an entity render marker
5. keep renderer ordering policy in the rendering layer rather than in pagination utilities

Important files:

- [Render.java](./../src/main/java/com/demcha/compose/layout_core/system/interfaces/Render.java)
- [PdfRenderingSystemECS.java](./../src/main/java/com/demcha/compose/layout_core/system/implemented_systems/pdf_systems/PdfRenderingSystemECS.java)
- [EntityRenderOrder.java](./../src/main/java/com/demcha/compose/layout_core/system/rendering/EntityRenderOrder.java)

Migration rule for new engine components:

- implement backend-neutral `Render`, not backend-specific render interfaces
- move PDF drawing into `...pdf_systems.handlers`
- use `TextMeasurementSystem` for text width and line metrics instead of reaching through `LayoutSystem`
- place PDF-only helper objects in `...pdf_systems.helpers`
- keep resolved draw ordering in renderer-owned or renderer-neutral rendering helpers such as `EntityRenderOrder`
- register a render handler for every engine render marker because the PDF entity path no longer supports a backend-specific render fallback

The practical rule is:

- the builder creates the entity and attaches the right renderable component
- during rendering, GraphCompose checks `entity.hasRender()`
- if the render component supports the active renderer, the renderer executes its drawing logic

## Testing layout-sensitive changes

When a feature can affect resolved coordinates, layering, child order, or pagination, add or update a layout snapshot test in addition to any unit tests.

Recommended rule:

- unit tests prove the local math
- layout snapshot tests prove the composed document geometry
- PDF render tests prove the final backend output still looks sane

Prefer pairing a snapshot assertion with an existing render test when the document is complex or business-critical.

See [layout-snapshot-testing.md](./layout-snapshot-testing.md) for the baseline locations, update flow, and concrete examples.

So if your new object needs custom drawing, it is not enough to add a builder. You also need a renderable component with the correct renderer implementation.

## Where layout hooks in

The layout side uses entity components, not builder classes directly.

Important files:

- [LayoutTraversalContext.java](./../src/main/java/com/demcha/compose/layout_core/core/LayoutTraversalContext.java)
- [LayoutSystem.java](./../src/main/java/com/demcha/compose/layout_core/system/LayoutSystem.java)
- [ComputedPosition.java](./../src/main/java/com/demcha/compose/layout_core/components/layout/coordinator/ComputedPosition.java)
- [PageBreaker.java](./../src/main/java/com/demcha/compose/layout_core/system/utils/page_breaker/PageBreaker.java)

In practice:

- `Anchor`, `Margin`, `Padding`, `ContentSize`, and parent/child links are what matter to layout
- the builder is just the place where you attach those components
- `LayoutTraversalContext` should build one deterministic hierarchy snapshot per pass instead of letting each subsystem rediscover roots and children independently
- `ParentComponent` is the authoritative parent relation, while `Entity.children` is the canonical sibling order
- if those two sources disagree, traversal code should warn loudly and use a deterministic fallback rather than silently hiding the inconsistency
- during pagination, descendants should be resolved before parent containers so parent size updates caused by child page shifts are reflected before parent placement is finalized

See [pagination-ordering.md](./pagination-ordering.md) for a focused explanation of this rule, including why a `Circle` case can fail while an `Image` case appears to work.

If those components are missing or inconsistent, the renderer cannot save you later.

## When to add a method to `ComponentBuilder`

Add a method to [ComponentBuilder.java](./../src/main/java/com/demcha/compose/layout_core/components/components_builders/ComponentBuilder.java) when:

- the new object is part of the public builder API
- it should be reachable from `composer.componentBuilder()`
- you want it tracked alongside the other pending builders before `build()` / `toBytes()`

Do not add a method there if the new object is only an internal helper for templates.

## Practical checklist for a new object

- choose the correct builder base class
- add the render marker in `initialize()` if the object is drawable
- add `Expendable` only for parent-like boxes that should grow because of children
- add `Breakable` only for entities whose own content can span pages
- attach content/style components through fluent methods
- provide `ContentSize` directly or calculate it in `build()`
- attach `Anchor` / `Margin` / `Padding` as needed
- use `addChild(...)` only for true container objects
- add a `ComponentBuilder` factory method if this should be public API
- add tests for layout and rendering behavior

## Good examples to copy

- leaf text with measured size:
  [TextBuilder.java](./../src/main/java/com/demcha/compose/layout_core/components/components_builders/TextBuilder.java)
- shape-like object:
  [RectangleBuilder.java](./../src/main/java/com/demcha/compose/layout_core/components/components_builders/RectangleBuilder.java)
- fixed leaf line object:
  [LineBuilder.java](./../src/main/java/com/demcha/compose/layout_core/components/components_builders/LineBuilder.java)
- container:
  [ModuleBuilder.java](./../src/main/java/com/demcha/compose/layout_core/components/components_builders/ModuleBuilder.java)
- template-level composition helper:
  [TemplateBuilder.java](./../src/main/java/com/demcha/templates/TemplateBuilder.java)

## Current caveats

- the PDF path is the supported renderer today
- Word-related classes exist in source, but they should be treated as experimental
