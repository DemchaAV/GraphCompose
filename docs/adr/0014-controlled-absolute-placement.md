# ADR 0014 — Controlled absolute placement: `CanvasLayerNode`

- **Status:** Accepted
- **Date:** 2026-05-07
- **Authors:** Artem Demchyshyn

## Context

GraphCompose has three placement primitives in v1.5:

1. **Flow** — `addParagraph`, `addList`, `addRow` lay out children
   top-to-bottom (with row-internal horizontal flow).
2. **Stack with alignment** — `LayerStackNode` anchors children at
   one of nine `LayerAlign` corners/edges plus an optional
   `(offsetX, offsetY)` nudge.
3. **Shape-as-container** — `ShapeContainerNode` clips children to
   a shape outline.

What's missing is a fourth primitive: **explicit pixel-level
placement** of children at author-specified `(x, y)` coordinates
inside a fixed-size bounding box. Authors who genuinely need
this — diploma seals, pixel-perfect cover-page badges, custom
diagrams, marketing blocks where specific dots must land at
specific coordinates — currently reach for either:

- A `LayerStackNode` with `LayerAlign.TOP_LEFT` plus heavy
  `offsetX/offsetY` arithmetic (offsets are computed from the
  alignment anchor, not from the bounding box, so authors have
  to mentally translate twice), or
- The retired low-level `EntityManager` engine API that v1.5
  removed from the public surface.

The v1.6 roadmap (`docs/roadmaps/v1.6-roadmap.md` Phase C) lands an
explicit "I want absolute placement" opt-in that is still a
canonical node — not an engine bypass.

## Decision

**Add `CanvasLayerNode`** as a new public composite node.
Children sit at explicit `(x, y)` pixel coordinates inside a
fixed-size bounding box; the canvas reserves a stable
`(width × height)` rectangle in the surrounding flow regardless
of where children land.

```java
public record CanvasLayerNode(
        String name,
        double width,
        double height,
        List<CanvasChild> placements,
        ClipPolicy clipPolicy,
        DocumentInsets padding,
        DocumentInsets margin) implements DocumentNode { ... }

public record CanvasChild(DocumentNode node, double x, double y) { ... }
```

DSL shortcut on `AbstractFlowBuilder`:

```java
section.addCanvas(523, 360, canvas -> canvas
    .position(new ParagraphNode("title", "OFFICIAL", ...), 100, 60)
    .position(new ShapeNode("seal", 80, 50, ...), 410, 250)
    .clipPolicy(ClipPolicy.CLIP_BOUNDS));
```

### Coordinate system

The canvas uses the **screen convention**: `(0, 0)` is the
canvas's **top-left** corner, positive `x` extends right,
positive `y` extends downward. Authors think in the same model
they use for HTML/SVG/Figma — no PDF-y-up arithmetic in the
public API.

### Layout integration

`CanvasLayerDefinition` reuses the existing `LayerStackNode`
placement plumbing:

- `prepare(node, ctx, constraints)` builds a
  `PreparedStackLayout` where every child's alignment is
  `LayerAlign.TOP_LEFT` and `offsetX = canvasChild.x()`,
  `offsetY = canvasChild.y()`.
- `LayerStack`'s `offsetY` follows the same screen convention
  (positive `y` = down), so the canvas's `(x, y)` maps directly
  to the stack layout's `(offsetX, offsetY)` — no conversion
  arithmetic.
- `paginationPolicy = ATOMIC`.
- `children(node)` returns the canvas's child nodes; the
  framework prepares + places them through the existing STACK
  axis dispatch in `LayoutCompiler`.

The canvas's measured size is **explicit**:
`(width + padding.horizontal(), height + padding.vertical())`.
Unlike `LayerStackNode`, the canvas does **not** derive its
size from children — its dimensions are author-controlled so
the surrounding flow can reserve a deterministic rectangle.

### Why a new node and not a flag on `LayerStackNode`

`LayerStackNode` semantics are alignment-anchored: every layer
chooses one of nine corners/edges and optionally nudges. Adding
a "use absolute coordinates instead" boolean would either
double the meaning of `LayerStackNode.offsetX/offsetY` or
require a parallel `pixelOffsetX/pixelOffsetY`. Both options
muddy a node that authors already understand. A separate node
keeps each primitive single-purpose: `LayerStackNode` is
"alignment-based stack", `CanvasLayerNode` is
"explicit-coordinate stack".

### Why explicit `width`/`height` (not "wrap the largest child")

A canvas exists precisely because the author wants control over
the placement rectangle. Auto-sizing from children would defeat
that intent — moving a child two points right would silently
expand the surrounding flow. The fixed-size contract also makes
pagination predictable: the canvas either fits on the current
page or doesn't.

### Rejected: absolute placement on `RowBuilder` / `SectionBuilder`

A global "place this child at `(x, y)`" policy on every flow
builder would invite authors to layer absolute placement on top
of normal flow — which is exactly the unpredictable mess that
the retired `EntityManager` API created. Absolute placement is
explicitly **opt-in inside a `CanvasLayerNode`**; the rest of
the flow stays declarative.

### DOCX backend behaviour

The Apache POI Word model has no canvas abstraction. The DOCX
semantic backend (planned for v1.6 stretch / v1.7) emits a
one-time `docx.export.canvas-layer-fallback` capability
warning and renders the canvas's children inline (losing the
explicit placement). The PDF backend honours the explicit
coordinates via the existing `PlacedFragment` pipeline.

### `ClipPolicy` field

The canvas carries a `ClipPolicy` field (default
`CLIP_BOUNDS`). The PDF backend's clipping pipeline (introduced
for `ShapeContainerNode` in v1.5) already understands the three
modes — `CLIP_BOUNDS` (rectangle), `CLIP_PATH` (shape outline),
`OVERFLOW_VISIBLE` — and the canvas plugs into the existing
overlay-fragment path. Authors who place children whose
bounding boxes overflow the canvas keep them clipped by default;
diagram authors who want overflow can opt into
`OVERFLOW_VISIBLE`.

## Consequences

### Positive

- **All four placement primitives are now first-class.** Flow,
  stack-aligned, shape-clipped, and explicit-coordinate are
  each one canonical node with one coherent semantic. No
  engine bypass needed for pixel-precise authoring.
- **Reuses the LayerStack pipeline.** No new framework code
  (no new `CompositeLayoutSpec.Axis`, no new placement
  dispatch). The definition is ~80 LOC of orchestration.
- **Coordinate model matches author intuition.** Top-left
  origin + positive y down is the model every modern UI tool
  uses; authors reach for it without translation.
- **Fixed-size contract makes pagination predictable.** A
  canvas either fits on the page or moves to the next page
  whole — same row-atomic story as every other v1.6 ATOMIC
  composite.

### Negative

- **No automatic anti-overlap.** Two children placed at the
  same `(x, y)` overlap silently — that's the author's
  responsibility (and arguably a feature for badge-on-card
  use cases).
- **`zIndex` is hardcoded to 0.** Source order is render
  order. Authors who need explicit z-stacking inside a canvas
  wrap the canvas in a `LayerStackNode` with explicit
  `zIndex` per layer. A future v1.7 follow-up could expose
  `position(child, x, y, z)` if the demand is real.
- **DOCX fidelity is lost.** A canvas in a DOCX export becomes
  inline children. This is consistent with how DOCX handles
  every PDF-only primitive (transforms, clip paths) and the
  capability-warning channel surfaces it cleanly.
- **Author can place children beyond the bounding box.**
  `CLIP_BOUNDS` (the default) hides the overflow at render
  time; the layout graph still records the original
  coordinates for snapshots and tooling.

### Alternatives considered

- **Add `Axis.CANVAS` + new `PreparedCanvasLayout`.**
  Rejected — would require a new dispatch arm in
  `LayoutCompiler` for no behavioural difference from STACK
  with TOP_LEFT anchors. The reuse path is identical and
  cheaper.
- **Make `CanvasLayerNode` auto-size from children.**
  Rejected — defeats the "reserve a stable rectangle" intent
  and breaks the predictable-pagination story.
- **Place children at PDF-y-up coordinates.** Rejected —
  authors think in screen-y-down and every modern UI tool
  uses that convention. Forcing PDF coordinates would create
  a constant translation tax at the call site.

## References

- v1.6 roadmap, Phase C:
  [`docs/roadmaps/v1.6-roadmap.md`](../roadmaps/v1.6-roadmap.md).
- ADR 0001 — Shape-as-container (consumer of the same
  `ClipPolicy`):
  [`0001-shape-as-container.md`](0001-shape-as-container.md).
- ADR 0012 — Nested list ergonomics (sibling Phase A
  feature):
  [`0012-nested-list-evolution.md`](0012-nested-list-evolution.md).
- ADR 0013 — Composed table cell content (sibling Phase B
  feature):
  [`0013-composed-table-cell.md`](0013-composed-table-cell.md).
- Tests:
  [`src/test/java/com/demcha/compose/document/dsl/CanvasLayerBuilderTest.java`](../../src/test/java/com/demcha/compose/document/dsl/CanvasLayerBuilderTest.java).
- Snapshot baseline:
  `src/test/resources/layout-snapshots/document/canvas_layer_basic.json`.
- Showcase:
  [`examples/src/main/java/com/demcha/examples/CanvasLayerExample.java`](../../examples/src/main/java/com/demcha/examples/CanvasLayerExample.java).
