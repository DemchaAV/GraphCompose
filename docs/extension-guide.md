# Extension Guide

How to extend the canonical document API without reaching into the
engine ECS. The guide walks through four common extension paths,
each with the v1.5 `ShapeContainerNode` work as a worked example so
the moving pieces are easy to find in the source tree.

If you only need to compose existing primitives, the
[recipes](recipes.md) and the [getting-started](getting-started.md)
quick-start are enough. Reach for this guide when:

- You want a brand-new semantic shape that the DSL can produce.
- You want to add a fluent setter to an existing builder.
- You want a new render backend (PDF variant, image, slide deck) that
  consumes the layout graph.
- You want to validate a custom node's layout via the snapshot
  framework.

## Mental model

GraphCompose layers separation of concerns from authoring all the way
to bytes:

```
Author code
   ↓
DocumentDsl + Builders        →  semantic nodes (DocumentNode subtypes)
   ↓
NodeRegistry + LayoutCompiler →  PreparedNode tree → PlacedNode + LayoutFragment list
   ↓
Backend (FixedLayoutBackend or SemanticBackend) → bytes
```

**Adding a new shape** touches three of these layers: a `DocumentNode`
record (semantic), a `NodeDefinition` (layout), and one or more
backend render handlers. **Adding a fluent setter** only touches the
builder layer. **Adding a backend** consumes `LayoutGraph` without
changing anything above it.

## 1. Add a semantic node

A semantic node is an immutable record that describes WHAT the author
wants on the page. It carries no layout state and no rendering bits.

The minimum:

1. New `record FooNode(...) implements DocumentNode`.
2. New `FooDefinition implements NodeDefinition<FooNode>` that
   prepares, paginates, and emits fragments for the node.
3. New `FooFragmentPayload` (often a record on
   `BuiltInNodeDefinitions` for built-ins) that carries the data the
   render handler needs.
4. Register the definition in `BuiltInNodeDefinitions.registerDefaults(...)`
   (or in your own registry).
5. New render handler(s) per backend that recognise the payload type.

### Worked example — `ShapeContainerNode`

The Phase B `ShapeContainerNode` is a clean example of all five steps:

| Step | Source file |
| --- | --- |
| Record | [`ShapeContainerNode.java`](../src/main/java/com/demcha/compose/document/node/ShapeContainerNode.java) |
| NodeDefinition | `ShapeContainerDefinition` (inner class of [`BuiltInNodeDefinitions.java`](../src/main/java/com/demcha/compose/document/layout/BuiltInNodeDefinitions.java)) |
| Payloads | `ShapeClipBeginPayload`, `ShapeClipEndPayload` (also inner classes of `BuiltInNodeDefinitions`) |
| Registration | `BuiltInNodeDefinitions.registerDefaults(...)` line that calls `.register(new ShapeContainerDefinition())` |
| Render handlers | [`PdfShapeClipBeginRenderHandler.java`](../src/main/java/com/demcha/compose/document/backend/fixed/pdf/handlers/PdfShapeClipBeginRenderHandler.java) and `PdfShapeClipEndRenderHandler.java` |

The `NodeDefinition` interface has four required methods plus three
defaults:

```java
public interface NodeDefinition<E extends DocumentNode> {
    Class<E> nodeType();
    PreparedNode<E> prepare(E node, PrepareContext ctx, BoxConstraints constraints);
    PaginationPolicy paginationPolicy(E node);
    List<LayoutFragment> emitFragments(PreparedNode<E> prepared,
                                       FragmentContext ctx,
                                       FragmentPlacement placement);

    // optional defaults
    default PreparedSplitResult<E> split(...) { ... }
    default List<DocumentNode> children(E node) { return node.children(); }
    default List<LayoutFragment> emitOverlayFragments(...) { return List.of(); }
}
```

`emitOverlayFragments` is the v1.5 hook for paired begin/end markers
that wrap children — `ShapeContainerNode` uses it to pair the clip-begin
fragment (emitted from `emitFragments`, sits BEHIND children) with the
matching clip-end fragment (emitted from `emitOverlayFragments`, sits
AFTER children).

### Architecture-guard tests

The repo has two architecture-guard tests that pin two invariants for
new nodes:

- [`PublicApiNoEngineLeakTest`](../src/test/java/com/demcha/documentation/PublicApiNoEngineLeakTest.java)
  — `com.demcha.compose.document.*` must not import
  `com.demcha.compose.engine.*` types.
- [`CanonicalSurfaceGuardTest`](../src/test/java/com/demcha/documentation/CanonicalSurfaceGuardTest.java)
  — public markdown docs and runnable examples must not reference the
  retired legacy surface (the v1.0–v1.3 PDF composer entry point and
  the old templates package). The forbidden token list lives at the
  top of that test file.

If your new record needs a TYPE from the engine package, route it
through a public adapter on the canonical surface; do not import
`engine.*` directly into `document.*`.

## 2. Add a fluent setter to an existing builder

When you only need to expose an existing knob on an existing node,
extend the builder. Common shapes:

- A new `addFoo(double, double, DocumentColor)` shortcut on
  `AbstractFlowBuilder` for a frequently-used composition (see
  `addCircle(double, DocumentColor)` for the v1.5.0-alpha pattern).
- A new mutator on a leaf builder (see
  `ShapeContainerBuilder.clipPolicy(ClipPolicy)`).
- A new mixin interface implemented by multiple builders
  (`Transformable<T>` is the v1.5 example — opt-in by implementing
  two abstract methods, gain `rotate`, `scale`, `transform` for free).

Test pattern:

- Unit test the builder asserts the resulting node has the expected
  fields (see `ShapeContainerBuilderTest`).
- Add a `*DemoTest` under `src/test/java/com/demcha/testing/visual/`
  that writes a PDF artefact under `target/visual-tests/...` so the
  reviewer can check the rendered output by eye.

## 3. Add a render handler for an existing backend

Render handlers consume one fragment payload type and write to the
backend's rendering surface. To add a handler for the canonical PDF
backend:

1. Implement `PdfFragmentRenderHandler<YourPayloadType>`. Two
   methods: `payloadType()` and `render(fragment, payload, env)`.
2. Register the handler in
   [`PdfFixedLayoutBackend.defaultHandlers()`](../src/main/java/com/demcha/compose/document/backend/fixed/pdf/PdfFixedLayoutBackend.java)
   (or pass a custom handler list to the package-private constructor).
3. Wrap any graphics-state changes in `saveGraphicsState()` /
   `restoreGraphicsState()` so the next handler sees a clean slate.
   `PdfShapeClipBeginRenderHandler` and `PdfTransformBeginRenderHandler`
   are the two handlers that intentionally leak graphics state across
   fragments — they're paired with end handlers that restore it. The
   `ShapeContainerInvariantsTest` enforces the pair balance.

Test pattern:

- A unit test that asserts the layout graph contains the expected
  fragment payload (use `LayoutGraph.fragments()` and pattern match
  on payload type).
- A `*DemoTest` that renders a real PDF and asserts the magic header
  is intact (`%PDF-`) — a graphics-state leak corrupts the byte
  stream and the magic header check catches it.

## 4. Add a new backend

A backend consumes a `LayoutGraph` and produces bytes. Two interfaces
to choose from:

- `FixedLayoutBackend<T>` — produces a fixed-layout artifact (PDF,
  image, slide). Receives the resolved `LayoutGraph` with absolute
  coordinates and pages. Example: `PdfFixedLayoutBackend`.
- `SemanticBackend<T>` — produces a semantic artifact (DOCX, HTML,
  Markdown). Receives the un-paginated semantic node tree via
  `DocumentGraph`. Example: `DocxSemanticBackend`.

Both interfaces are small (`name()` plus an `export(graph, context)`
method). The minimum pattern:

```java
public final class FooBackend implements SemanticBackend<byte[]> {
    @Override public String name() { return "foo-semantic"; }

    @Override
    public byte[] export(DocumentGraph graph, SemanticExportContext context) throws Exception {
        // walk graph.roots() recursively and translate to your format
    }
}
```

The semantic backend pattern is straightforward: pattern-match on
`DocumentNode` subtype, recurse for composites, ignore (or fall back
on) unsupported node types. `DocxSemanticBackend.writeShapeContainer`
is a good reference — it logs a one-time capability warning and
recurses into the children when the backend cannot express the
container's semantics.

## 5. Layout-snapshot tests for your own nodes

Layout snapshots are the regression net for layout changes. They
freeze the resolved `LayoutGraph` (positions, sizes, paths) into a
deterministic JSON file, and compare it against the next run. A
visual change is then expressed as a diff in source control instead
of a hidden behaviour drift.

Set-up per node:

1. Build a small fixture document under
   `src/test/resources/layout-snapshots/foo/` (or generate it from a
   helper class).
2. Write a test that opens a `DocumentSession`, composes the
   fixture, captures the layout graph, and runs
   `LayoutSnapshotAssertions.assertMatches(...)`.
3. The first run writes the baseline (after `-Dgraphcompose.snapshot.approve=true`);
   subsequent runs compare against it.

The
[`LayoutSnapshotAssertions`](../src/main/java/com/demcha/compose/testing/layout/LayoutSnapshotAssertions.java)
helper class wraps the diff machinery; existing snapshot tests under
`src/test/java/com/demcha/compose/document/templates/builtins/`
(e.g. `BuiltInTemplateLayoutSnapshotTest`) show the call site shape.

## Reading the source by responsibility

When something looks wrong, the package map is the fastest way to
locate the responsible file:

| Concern | Top-level package |
| --- | --- |
| Public author surface | `com.demcha.compose.document.{api,dsl,node,style,table,image}` |
| Layout pipeline | `com.demcha.compose.document.layout` (`LayoutCompiler`, `BuiltInNodeDefinitions`, `TableLayoutSupport`) |
| PDF backend | `com.demcha.compose.document.backend.fixed.pdf` |
| DOCX backend | `com.demcha.compose.document.backend.semantic` |
| Engine ECS internals | `com.demcha.compose.engine.*` (don't reach in from canonical code) |

Detailed ownership lives in
[`docs/package-map.md`](package-map.md).

## See also

- [`docs/architecture.md`](architecture.md) — high-level architecture
  and the canonical-vs-engine boundary.
- [`docs/implementation-guide.md`](implementation-guide.md) —
  engine-side ECS extension patterns (component records, system
  registration, low-level harness builders).
- [`docs/lifecycle.md`](lifecycle.md) — the session, layout, and
  render flow end-to-end.
- [ADR 0001 — Shape-as-container](adr/0001-shape-as-container.md) —
  the design rationale that shaped the v1.5 `ShapeContainerNode`,
  with the alternative considered ("flag on `LayerStackNode`") and
  why it was rejected.
