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

Use [EmptyBox.java](./../src/main/java/com/demcha/compose/loyaut_core/components/containers/abstract_builders/EmptyBox.java) when the new object is a leaf entity or a small custom object that does not manage children itself.

Examples in the codebase:

- [TextBuilder.java](./../src/main/java/com/demcha/compose/loyaut_core/components/components_builders/TextBuilder.java)
- [ImageBuilder.java](./../src/main/java/com/demcha/compose/loyaut_core/components/components_builders/ImageBuilder.java)
- [LinkBuilder.java](./../src/main/java/com/demcha/compose/loyaut_core/components/components_builders/LinkBuilder.java)
- [ElementBuilder.java](./../src/main/java/com/demcha/compose/loyaut_core/components/components_builders/ElementBuilder.java)

This is the right choice for the exact case you asked about: an object that does not expand into a child-owning container and just needs base entity functionality plus layout/render participation.

What `EmptyBox<T>` gives you:

- entity creation
- auto-generated `EntityName`
- fluent `addComponent(...)`
- parent/child helpers
- access to `EntityManager`
- default `build()` behavior through the builder hierarchy

### Extend `ShapeBuilderBase<T>` when

Use [ShapeBuilderBase.java](./../src/main/java/com/demcha/compose/loyaut_core/components/containers/abstract_builders/ShapeBuilderBase.java) when the object is still a leaf, but you want common shape helpers such as:

- fill color
- stroke
- corner radius

Examples:

- [RectangleBuilder.java](./../src/main/java/com/demcha/compose/loyaut_core/components/components_builders/RectangleBuilder.java)
- [ButtonBuilder.java](./../src/main/java/com/demcha/compose/loyaut_core/components/components_builders/ButtonBuilder.java)

### Extend `ContainerBuilder<T>` when

Use [ContainerBuilder.java](./../src/main/java/com/demcha/compose/loyaut_core/components/containers/abstract_builders/ContainerBuilder.java) when the new object owns child entities and participates in parent/child layout.

Examples:

- [HContainerBuilder.java](./../src/main/java/com/demcha/compose/loyaut_core/components/components_builders/HContainerBuilder.java)
- [VContainerBuilder.java](./../src/main/java/com/demcha/compose/loyaut_core/components/components_builders/VContainerBuilder.java)
- [ModuleBuilder.java](./../src/main/java/com/demcha/compose/loyaut_core/components/components_builders/ModuleBuilder.java)

Use this path when the object should call `addChild(...)` and arrange nested entities.

## Minimum components a new object usually needs

### Render marker

If the object should render something visible, the entity needs a renderable marker component.

Examples:

- [TextComponent.java](./../src/main/java/com/demcha/compose/loyaut_core/components/renderable/TextComponent.java)
- [Rectangle.java](./../src/main/java/com/demcha/compose/loyaut_core/components/renderable/Rectangle.java)
- [ImageComponent.java](./../src/main/java/com/demcha/compose/loyaut_core/components/renderable/ImageComponent.java)

Those renderable components implement the renderer contract used by the current PDF renderer.

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

- [ContentSize.java](./../src/main/java/com/demcha/compose/loyaut_core/components/geometry/ContentSize.java)

For simple fixed-size objects, set `ContentSize` directly in the builder.

For measured objects, compute size in `build()` before the entity is registered.

Example:

- [TextBuilder.java](./../src/main/java/com/demcha/compose/loyaut_core/components/components_builders/TextBuilder.java) calls `TextComponent.autoMeasureText(...)` when auto-size is enabled.

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
6. add a factory method to [ComponentBuilder.java](./../src/main/java/com/demcha/compose/loyaut_core/components/components_builders/ComponentBuilder.java) if you want `composer.componentBuilder().yourObject()`

### Case 2: add a new container-like object

Use this when the object owns child entities and arranges them.

Steps:

1. extend `ContainerBuilder<T>`
2. add the render/container marker in `initialize()`
3. ensure the container has the alignment / axis semantics it needs
4. use `addChild(...)` to wire child entities
5. provide `ContentSize` or logic that lets layout compute it correctly

### Case 3: add a purely logical helper object

If the object is not directly rendered and mostly groups behavior, it may not need a new render component at all. In that case you may only need:

- a builder/helper method
- a composition pattern over existing entities
- or a template-layer helper in `Templatese`

## Where rendering hooks in

The current PDF path works through renderable components and the PDF renderer system.

Important files:

- [Render.java](./../src/main/java/com/demcha/compose/loyaut_core/system/interfaces/Render.java)
- [PdfRenderingSystemECS.java](./../src/main/java/com/demcha/compose/loyaut_core/system/implemented_systems/pdf_systems/PdfRenderingSystemECS.java)

The practical rule is:

- the builder creates the entity and attaches the right renderable component
- during rendering, GraphCompose checks `entity.hasRender()`
- if the render component supports the active renderer, the renderer executes its drawing logic

So if your new object needs custom drawing, it is not enough to add a builder. You also need a renderable component with the correct renderer implementation.

## Where layout hooks in

The layout side uses entity components, not builder classes directly.

Important files:

- [LayoutSystem.java](./../src/main/java/com/demcha/compose/loyaut_core/system/LayoutSystem.java)
- [ComputedPosition.java](./../src/main/java/com/demcha/compose/loyaut_core/components/layout/coordinator/ComputedPosition.java)
- [PageBreaker.java](./../src/main/java/com/demcha/compose/loyaut_core/system/utils/page_breaker/PageBreaker.java)

In practice:

- `Anchor`, `Margin`, `Padding`, `ContentSize`, and parent/child links are what matter to layout
- the builder is just the place where you attach those components

If those components are missing or inconsistent, the renderer cannot save you later.

## When to add a method to `ComponentBuilder`

Add a method to [ComponentBuilder.java](./../src/main/java/com/demcha/compose/loyaut_core/components/components_builders/ComponentBuilder.java) when:

- the new object is part of the public builder API
- it should be reachable from `composer.componentBuilder()`
- you want it tracked alongside the other pending builders before `build()` / `toBytes()`

Do not add a method there if the new object is only an internal helper for templates.

## Practical checklist for a new object

- choose the correct builder base class
- add the render marker in `initialize()` if the object is drawable
- attach content/style components through fluent methods
- provide `ContentSize` directly or calculate it in `build()`
- attach `Anchor` / `Margin` / `Padding` as needed
- use `addChild(...)` only for true container objects
- add a `ComponentBuilder` factory method if this should be public API
- add tests for layout and rendering behavior

## Good examples to copy

- leaf text with measured size:
  [TextBuilder.java](./../src/main/java/com/demcha/compose/loyaut_core/components/components_builders/TextBuilder.java)
- shape-like object:
  [RectangleBuilder.java](./../src/main/java/com/demcha/compose/loyaut_core/components/components_builders/RectangleBuilder.java)
- container:
  [ModuleBuilder.java](./../src/main/java/com/demcha/compose/loyaut_core/components/components_builders/ModuleBuilder.java)
- template-level composition helper:
  [TemplateBuilder.java](./../src/main/java/com/demcha/Templatese/TemplateBuilder.java)

## Current caveats

- legacy package names such as `loyaut_core` and `Templatese` are still in use
- the PDF path is the supported renderer today
- Word-related classes exist in source, but they should be treated as experimental
