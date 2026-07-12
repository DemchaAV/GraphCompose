# Implementation Guide

This guide explains how to add new objects and engine extensions in GraphCompose without fighting the current architecture.

## Mental model

GraphCompose does not render directly from application calls.

The usual flow is:

1. application code describes the document through canonical DSL/modules/nodes
2. the canonical layout compiler prepares deterministic layout fragments
3. internal engine components carry content, style, size, and parent/child relationships
4. layout and pagination systems calculate size, placement, and page fragments
5. rendering systems inspect resolved fragments/components and draw them

That means a new object usually needs the right answer in four areas:

- what canonical node or engine model it should introduce
- which components must be attached to the entity
- whether it participates in parent/child layout
- how it gets rendered

## Keep `Entity` thin

`Entity` is the ECS core object, not the preferred home for new layout helpers.

Use these ownership rules when adding or refactoring engine code:

- put geometry reads in `EntityBounds`
- put parent container size propagation and page-shift updates in `ParentContainerUpdater`
- keep render-order policy in the render layer (the `engine.render` contracts and the PDF fragment handlers)
- treat `Entity.bounding*` and `Entity.updateParentContainer*` as deprecated compatibility wrappers

Rule of thumb:

- if the logic needs `Placement`, `ContentSize`, `Margin`, or parent traversal semantics, it probably belongs in a helper or system utility
- if the logic only needs identity, component access, or canonical child order, it may belong on `Entity`

## Minimum components a new object usually needs

### Render marker

If the object should render something visible, the entity needs a renderable marker component.

Examples:

- [TextComponent.java](../../core/src/main/java/com/demcha/compose/engine/components/renderable/TextComponent.java)
- [BlockText.java](../../core/src/main/java/com/demcha/compose/engine/components/renderable/BlockText.java)

Those renderable components are render markers. Prefer keeping them backend-neutral and let renderer-owned handlers perform format-specific drawing.

### Engine markers with different jobs

There are three different ideas in the engine that are easy to mix up:

- render marker: tells the active renderer how to draw the entity
- container-growth marker: tells the container expansion phase that the parent box may grow to fit children
- `Breakable`: tells the page breaker that the entity's own content may continue across pages

Use them for different reasons:

- add a render marker when the object is visible
- add the container-growth marker only when the entity is a true parent-like box that should resize because of child content
- add `Breakable` only when the entity itself can be split or continued during pagination

Examples:

- a container marker is both parent-growable and `Breakable` because it owns children and may span pages
- `BlockText` is `Breakable` because its content can flow across pages
- `TextComponent` is a fixed leaf renderable; it renders a single resolved box and stays non-breakable

Important:

- the container-growth marker is not a pagination flag
- `Breakable` is not a child-sizing flag
- if a long leaf object is not `Breakable`, the engine treats it as a single block and moves it to the next page when needed

Leaf parity rule:

- if two objects are conceptually fixed leaf renderables, they should use the same layout contract
- that usually means the same kind of `ContentSize`, the same padding-aware inner draw area, and the same non-breakable pagination behavior
- if one of them behaves differently in containers or multi-page flow, first check the render/layout contract before changing pagination rules

### Content / style components

Attach the components that describe what the object is and how it should look.

Examples:

- `Text`, `TextStyle`
- `Stroke`
- `ImageData`

The renderer reads those components later during the render pass.

### Size component

If the engine needs to place the object, it usually needs a size signal.

The most common component is:

- [ContentSize.java](../../core/src/main/java/com/demcha/compose/engine/components/geometry/ContentSize.java)

For simple fixed-size objects, set `ContentSize` directly in the builder.

For measured objects, compute size in `build()` before the entity is registered.

### Layout components

The layout engine expects the usual layout metadata to be present when needed:

- `Anchor`
- `Margin`
- `Padding`
- `ParentComponent` for child entities

You normally do not add `Placement` yourself. The layout system calculates placement later.

## Typical implementation recipes

### Case 1: add a new leaf object

Use this when the object is visible, does not manage children, and should behave like text/image/rectangle.

Steps:

1. create a canonical node and node definition, or an internal engine model/assembler if the feature is not public authoring
2. in `initialize()`, attach the render marker component
3. add canonical DSL methods that attach the data/style intent
4. set or calculate `ContentSize`
5. register the entity through `build()`
6. add test-support builder helpers only when low-level engine tests need direct entity setup

Leaf rule of thumb:

- most leaf renderables should not grow to fit children
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

- containers that must resize around children usually need the container-growth marker
- containers whose content may continue on another page usually need `Breakable`
- some containers need both markers
- if the container models semantic section behavior, decide whether width should be inherited from the parent before letting child layout run

### Case 2.5: build a hybrid object with a breakable root and atomic leaf rows

Some engine objects look like containers from the outside, but still need their own leaf rendering contract inside.

The engine's table layout uses exactly this contract:

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

- [TableResolvedCell.java](../../core/src/main/java/com/demcha/compose/engine/components/content/table/TableResolvedCell.java)

Rule of thumb:

- make the root breakable only if the object as a whole can continue across pages
- keep logical row-like units as fixed leaves when the user would perceive splitting them as a bug
- if separators depend on where page fragments start or end, compute that in the render phase from resolved `Placement`, not only from builder-time metadata

### Case 3: add a purely logical helper object

If the object is not directly rendered and mostly groups behavior, it may not need a new render component at all. In that case you may only need:

- a builder/helper method
- a composition pattern over existing entities
- or a template-layer helper in `com.demcha.compose.document.templates.*`

### Case 4: add or refactor a built-in template

Built-in templates now use a canonical compose-first contract on top of
`DocumentSession`.

Use this split:

1. a public template interface in `com.demcha.compose.document.templates.api` exposes `compose(DocumentSession, ...)`
2. a preset class under `com.demcha.compose.document.templates.<family>.presets` exposes a stable `create(...)` factory and theme defaults
3. the preset delegates the reusable drawing work to its family's `components` / `widgets` layers and the shared `templates.core` widgets, keeping the preset itself a thin orchestrator
4. focused canonical tests and examples keep `compose(DocumentSession, ...)` stable while the component layer owns the reusable document structure

Practical rules:

- keep current `render(...): PDDocument` and `render(..., Path)` overloads only as deprecated compatibility adapters
- keep `GraphCompose.document(...)`, page size selection, margins, `document.buildPdf()`, and `document.toPdfBytes()` in canonical examples and integrations
- let deprecated bridge adapters call into the canonical `DocumentSession` PDF path rather than owning production layout/render logic
- put the actual document structure, sections, tables, and paragraph assembly in the family's component/widget layer
- keep components backend-neutral: no `PDDocument`, `PDPage`, `PDRectangle`, or low-level PDF imports
- keep public examples and integration docs compose-first: show `compose(DocumentSession, ...)` before any deprecated convenience path
- pass the `BrandTheme` (or style collaborators) into renderers instead of hard-wiring backend assumptions into composition code
- when a family already has a good component split, extend that pattern instead of reintroducing backend-specific logic into the composition layer

Current guard rails:

- `PublicApiNoEngineLeakTest` keeps the public authoring surface free of engine imports; template components stay backend-neutral by construction
- the per-family smoke and visual-parity tests keep `compose(DocumentSession, ...)` aligned with the presets

Rule of thumb:

- presets should feel like reusable public templates
- the family component and widget layers (plus `templates.core`) should feel like the reusable document composition core

## Where rendering hooks in

The current PDF path works through renderable components and the PDF renderer system.

Preferred extension pattern for new backends:

1. keep engine components as format-neutral render markers
2. register a backend-specific `TextMeasurementSystem`
3. register renderer-side handlers for marker types
4. keep backend-only helper drawing in renderer-owned helper packages when the code is not an entity render marker
5. keep renderer ordering policy in the rendering layer rather than in pagination utilities

> **Canonical PDF renderer.** Canonical PDF output is produced by
> `com.demcha.compose.document.backend.fixed.pdf`: `PdfFixedLayoutBackend`
> dispatches each layout fragment to a `PdfFragmentRenderHandler` implementation
> under `document.backend.fixed.pdf.handlers`. The backend-neutral `engine.render`
> *contracts* (`Render`, `RenderPassSession`, `RenderStream`)
> remain the shared render seam — extend PDF drawing by adding or updating a
> fragment handler, not by touching those contracts.

Important files:

- [Render.java](../../core/src/main/java/com/demcha/compose/engine/render/Render.java)
- [RenderPassSession.java](../../core/src/main/java/com/demcha/compose/engine/render/RenderPassSession.java)
- [RenderStream.java](../../core/src/main/java/com/demcha/compose/engine/render/RenderStream.java)
- [PdfFixedLayoutBackend.java](../../render-pdf/src/main/java/com/demcha/compose/document/backend/fixed/pdf/PdfFixedLayoutBackend.java)
- [PdfFragmentRenderHandler.java](../../render-pdf/src/main/java/com/demcha/compose/document/backend/fixed/pdf/PdfFragmentRenderHandler.java)

Migration rule for new engine components:

- implement backend-neutral `Render`, not backend-specific render interfaces
- move PDF drawing into a `PdfFragmentRenderHandler` under `...document.backend.fixed.pdf.handlers`
- use `TextMeasurementSystem` for text width and line metrics instead of reaching through the active renderer
- place PDF-only helper objects alongside the backend in `...document.backend.fixed.pdf`
- keep page-surface lifetime in a backend-specific `RenderPassSession`, not in engine builders or render markers
- keep resolved draw ordering in the render layer (the PDF fragment handlers), not in pagination utilities
- register a render handler for every engine render marker because the PDF entity path no longer supports a backend-specific render fallback

### Render-pass session rules

The current render seam is deliberately narrower than a full backend abstraction. Use these rules when extending it:

- `RenderStream<T>` should create one render-pass session, not one stream per entity
- renderer orchestrators such as `PdfFixedLayoutBackend` should open one session for the whole pass
- single-page handlers should use the session-managed page surface directly
- multi-page handlers should request page surfaces explicitly per fragment or page
- page creation or annotation-only work should use the session's page-availability helper instead of opening a dummy drawing surface
- handlers must restore graphics state and text state before returning
- handlers must never close a session-owned surface
- backend-specific text lifecycle helpers belong in backend handlers or backend sessions, not in shared engine interfaces

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

See [layout-snapshot-testing.md](../operations/layout-snapshot-testing.md) for the baseline locations, update flow, and concrete examples.

So if your new object needs custom drawing, it is not enough to add a builder. You also need a renderable component with the correct renderer implementation.

## Where layout hooks in

The layout side uses entity components, not builder classes directly.

Important files:

- [LayoutTraversalContext.java](../../core/src/main/java/com/demcha/compose/engine/core/LayoutTraversalContext.java)
- [ComputedPosition.java](../../core/src/main/java/com/demcha/compose/engine/components/layout/coordinator/ComputedPosition.java)
- [EntityBounds.java](../../core/src/main/java/com/demcha/compose/engine/components/geometry/EntityBounds.java)
- [ParentContainerUpdater.java](../../core/src/main/java/com/demcha/compose/engine/pagination/ParentContainerUpdater.java)

In practice:

- `Anchor`, `Margin`, `Padding`, `ContentSize`, and parent/child links are what matter to layout
- the builder is just the place where you attach those components
- `LayoutTraversalContext` should build one deterministic hierarchy snapshot per pass instead of letting each subsystem rediscover roots and children independently
- `ParentComponent` is the authoritative parent relation, while `Entity.children` is the canonical sibling order
- if those two sources disagree, traversal code should warn loudly and use a deterministic fallback rather than silently hiding the inconsistency
- during pagination, descendants should be resolved before parent containers so parent size updates caused by child page shifts are reflected before parent placement is finalized

Use the helpers directly when that intent is what you need:

- read bounds and edges through `EntityBounds` instead of adding more bound helpers to `Entity`
- update parent container size or shifted positions through `ParentContainerUpdater` instead of growing the `Entity` API further

See [pagination-ordering.md](../architecture/pagination-ordering.md) for a focused explanation of this rule, including why one leaf type can fail while another appears to work.

If those components are missing or inconsistent, the renderer cannot save you later.

## Practical checklist for a new object

- choose the correct builder base class
- add the render marker in `initialize()` if the object is drawable
- add the container-growth marker only for parent-like boxes that should grow because of children
- add `Breakable` only for entities whose own content can span pages
- attach content/style components through fluent methods
- provide `ContentSize` directly or calculate it in `build()`
- attach `Anchor` / `Margin` / `Padding` as needed
- use `addChild(...)` only for true container objects
- add a `DocumentDsl` method if this should be public API
- add tests for layout and rendering behavior

## Good examples to copy

- template-level composition helper:
  [SectionDispatcher.java](../../templates/src/main/java/com/demcha/compose/document/templates/cv/components/SectionDispatcher.java)

## Overlay primitive: `LayerStackNode`

`LayerStackNode` is the canonical atomic overlay composite. Its layers
share the same bounding box and are painted in source order — first
layer behind, last layer in front. Use it whenever two or more nodes
need to sit at the same coordinates with explicit alignment instead of
stacking vertically.

### Where it can live

- inside `pageFlow` directly (root flow);
- inside any `SectionNode` body;
- inside a row column slot — the layout compiler treats stacks as
  atomic overlays, distinct from the still-forbidden nested horizontal
  rows or splittable tables.

### Layout contract

- measurement: stack outer size = `max(child outer size)` plus stack
  padding and margin. Smaller layers are aligned inside the resolved
  box;
- pagination: stacks are atomic — they always move whole to the next
  page when they do not fit on the current page;
- alignment: each layer carries its own `LayerAlign` (`TOP_LEFT`,
  `CENTER`, `BOTTOM_RIGHT`, etc.) that resolves inside the inner box
  obtained by subtracting stack padding from the outer box.

### DSL surface

Use `addLayerStack(Consumer<LayerStackBuilder>)` on any flow builder
(page flow, section, container) to drop a stack inline. Place the
background as the first layer, then add foreground layers with the
desired alignment:

```java
section.addLayerStack(stack -> stack
        .name("MonogramBadge")
        .back(new EllipseBuilder()
                .name("MonogramRing")
                .size(78, 78)
                .stroke(DocumentStroke.of(MONOGRAM_RING, 1.25))
                .build())
        .layer(new ParagraphBuilder()
                .name("MonogramInitials")
                .text("M | H")
                .textStyle(monogramStyle())
                .align(TextAlign.CENTER)
                .build(),
                LayerAlign.CENTER));
```

### Typical use cases

- monogram and initial badges,
- watermark stamps on certificates and invoices,
- image-with-caption hero blocks,
- status labels overlaid on cards,
- decorative seals, signatures, and embossed marks.

`RowBuilder.add(...)` accepts `LayerStackNode` directly when the stack
is constructed manually. Nested rows and tables remain forbidden inside
row slots — only stacks pass the atomic-overlay capability check.

## Current caveats

- the PDF path is the supported renderer today
- Word-related classes exist in source, but they should be treated as experimental
