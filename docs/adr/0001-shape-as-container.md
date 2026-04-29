# ADR 0001 — Shape-as-container

- **Status:** Accepted
- **Date:** 2026-04-29
- **Authors:** Artem Demchyshyn

## Context

Today an author can stack arbitrary nodes inside a shared bounding box with
`LayerStackNode` (added in v1.4) and apply nine alignment anchors plus a
screen-space offset (added in v1.5 / Phase A). What still cannot be expressed
on the canonical surface is a use case the user keeps asking for:

> "I want to drop a circle on the page, put a label inside it, and have the
> label clip to the circle's outline and move with it as one unit."

That is structurally different from `LayerStackNode`:

| Concept | LayerStackNode | The "shape-as-container" we want |
|---|---|---|
| Bbox derives from | `max(child outer size)` | The outline's intrinsic size (e.g. circle diameter) |
| Children clipping | None — children may escape the bbox | Optional, by the outline path (`CLIP_PATH`) or its bbox (`CLIP_BOUNDS`) |
| Visible "frame" / fill | None directly — needs a separate `ShapeNode` as a back layer | Yes — the outline is a first-class part of the node and renders fill / stroke / corner radius |
| Pagination | `ATOMIC` | `SHAPE_ATOMIC` — outline plus all children stay on the same page |

We want a clean public API that reads as: *"Container is a circle / rounded
rect / ellipse, and inside it I'm composing other content."*

## Decision

We introduce a **new semantic node** `com.demcha.compose.document.node.ShapeContainerNode`
rather than overloading `LayerStackNode` with a `clipOutline` flag.

`ShapeContainerNode` carries:

```text
ShapeContainerNode {
    String          name
    ShapeOutline    outline       // sealed: Rectangle | RoundedRectangle | Ellipse
    List<Layer>     layers        // shares the same Layer record as LayerStackNode (offsets, anchor)
    ClipPolicy      clipPolicy    // CLIP_BOUNDS | CLIP_PATH | OVERFLOW_VISIBLE
    DocumentColor   fillColor     // optional outline fill
    DocumentStroke  stroke        // optional outline stroke
    DocumentInsets  padding
    DocumentInsets  margin
}
```

DSL surface:

```java
section.addCircle(60, brand, c -> c.center(label));   // shortcut

section.addShape(s -> s.roundedRect(180, 90, 12)
        .fillColor(slate)
        .clipPolicy(ClipPolicy.CLIP_PATH)
        .addInside(inner -> inner.padding(12).addText("Featured", h2)));

section.addEllipse(e -> e.size(160, 100)
        .fillColor(brand)
        .topRight(badge)         // delegates to layer(badge, TOP_RIGHT) under the hood
        .center(label));
```

Layout policy:

- A new `PaginationPolicy.SHAPE_ATOMIC` is added. From the page-breaker's
  perspective it behaves like `ATOMIC` (the container plus all its layers
  stays together), but the layout compiler also reserves `outline` geometry
  *before* it lays out the children, so child placement is deterministic
  relative to the outline.

Render policy:

- The PDF backend adds one new render path:
  `outline path → optional clip → draw layers → restore graphics state`. It
  reuses the existing `PdfShapeFragmentRenderHandler` rectangle / ellipse path
  builder so we do not introduce a second source of truth for shape geometry.
- The DOCX backend renders the outline as a borderless shape with fill / stroke
  and emits the layers without clipping (POI does not support arbitrary path
  clipping). A `BackendCapabilityWarning` is logged once per session.

## Why a new node, not a flag on `LayerStackNode`

We considered two alternatives.

### (A) New `ShapeContainerNode` *(chosen)*

- **+** The semantic intent is read at the type level: a container that
  *is a* shape, not a stack of layers that happens to also draw a shape.
- **+** Different pagination policy (`SHAPE_ATOMIC`) is a property of the
  type, not a flag callers can flip on `LayerStackNode` and accidentally
  break atomic layer overlays that rely on free-form bbox.
- **+** Discoverable through autocomplete: `addCircle(diameter, color, …)`,
  `addEllipse(...)`, `addShape(...).addInside(...)`. The `LayerStackBuilder`
  surface stays small and explicitly about z-order overlays.
- **+** Public-record extension is additive. Existing consumers of
  `LayerStackNode` are not affected.
- **−** Two builders look superficially similar to learn. Mitigated by the
  recipe page (see B.9) and by `LayerStackBuilder` Javadoc that points to
  `ShapeBuilder.addInside(...)` when the user actually wants clipping.

### (B) Add `clipOutline: ShapeOutline?` and `clipPolicy` to `LayerStackNode`

- **+** Single node type, fewer concepts.
- **−** Public record signature change for an already-shipped v1.4 type.
- **−** Two pagination policies on the same record, switched by a flag —
  hard to keep correct as the engine evolves.
- **−** Mixes semantic intents: "stack layers" vs "the outline *is* the
  container". Code reading `new LayerStackNode(..., clipOutline = circle)`
  is harder to scan than `new ShapeContainerNode(circle, ...)`.

## Consequences

- New public package leaf: `com.demcha.compose.document.style.ShapeOutline`
  (sealed value type). It is consumed by `ShapeContainerNode` and reused by
  whatever future node needs to describe an axis-aligned outline.
- `Layer` record stays in `LayerStackNode` for now; if a third node ever needs
  it, we extract it into `com.demcha.compose.document.node.LayerSpec`. Until
  then: `ShapeContainerNode` accepts `LayerStackNode.Layer` directly to avoid
  premature abstraction.
- Public API surface guards (`PublicApiNoEngineLeakTest`, `CanonicalSurfaceGuardTest`)
  must continue to show empty allowlists. The pre-pass that resolves the
  outline geometry lives in `LayoutCompiler`, not in the public surface.
- The DOCX backend gains documented limitations around `CLIP_PATH`. They are
  recorded in `docs/canonical-legacy-parity.md` as part of B.6.

## Implementation order (executed in B.2 → B.10)

1. B.2 — Public API: `ShapeOutline`, `ClipPolicy`, `ShapeContainerNode`,
   builder surface (`ShapeBuilder.addInside`, `EllipseBuilder.addInside`,
   `addCircle(diameter, color, Consumer)`).
2. B.3 — Layout pre-pass that materializes outline geometry, reuses the layer
   compiler with the outline-derived bbox.
3. B.4 — `PaginationPolicy.SHAPE_ATOMIC`.
4. B.5 — PDF render: outline draw + optional clip.
5. B.6 — DOCX fallback.
6. B.7 — Snapshot extension for `clipPath`.
7. B.8 — Architecture-guard tests for the new node.
8. B.9 — Recipe + runnable example.

## Out of scope for v1.5

- Rotation of the outline (covered by Phase C `Transform`).
- Per-layer rotation inside a shape container (Phase C).
- Polygon outlines (Phase v1.6).
- `expandWidth` / `expandHeight` flags on the outline (folded in from
  Phase A.6 — same record-extension batch).
- Nested `ListItem` value type (folded in from Phase A.7 — separate record
  change, scheduled in Phase B alongside this work but tracked under its own
  ADR if required).
