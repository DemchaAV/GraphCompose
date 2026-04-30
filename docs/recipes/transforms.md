# Transforms and z-index

Phase C of the v1.5 release adds two render-time concerns to the
canonical surface: the `DocumentTransform` value type plus the
`Transformable<T>` mixin (rotation around the placement centre and
non-uniform scaling), and explicit per-layer `zIndex` for
`LayerStackNode` / `ShapeContainerNode`.

Both are **render-time** — the canonical layout layer still measures
and places nodes against their natural bounding box, so layout
snapshots stay deterministic regardless of rotation, scale, or
z-index.

## Rotated badge

`ShapeContainerBuilder` implements `Transformable<ShapeContainerBuilder>`,
so a circle with a label can rotate as one unit. Positive degrees =
clockwise.

```java
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.style.DocumentColor;

DocumentColor brand = DocumentColor.rgb(20, 60, 75);

document.pageFlow(page -> page
        .addCircle(110, brand, circle -> circle
                .name("RotatedSeal")
                .padding(10)
                .rotate(15)                       // ← clockwise
                .center(new ParagraphBuilder()
                        .text("M&A")
                        .build())));
```

The outline, the optional clip path, and every layer rotate together
around the outline's geometric centre. Rotating in the opposite
direction is `rotate(-15)`.

## Reduced watermark in the corner

Use `scale(...)` to shrink (or grow) a container around its centre.
Pair with positioning to nudge it into a corner.

```java
document.pageFlow(page -> page
        .addContainer(card -> card
                .roundedRect(180, 100, 12)
                .fillColor(DocumentColor.WHITE)
                .scale(0.7)                       // ← uniform shrink
                .center(label("scale(0.7)"))));
```

Non-uniform scale is `scale(sx, sy)` — useful for a mild horizontal
stretch:

```java
.scale(1.1, 0.85)
```

Negative scale flips the axis (mirror effect):

```java
.scale(-1.0, 1.0)
```

`rotate(...)` and `scale(...)` chain naturally — each call preserves
the unmodified axis:

```java
.rotate(45).scale(2.0, 0.5)        // both effects on the same transform
```

The transform is stored on the node and applied at render time. The
PDF backend honours it via a graphics-state `cm` push/pop centred on
the outline; backends that cannot express affine CTM (DOCX) ignore
the transform and render the un-transformed geometry.

## z-index for overlays above other layers

Layers are stable-sorted by ascending `zIndex` before render. Higher
`zIndex` renders on top. The default is `0`, so layouts written before
Phase C continue to render in source order.

```java
import com.demcha.compose.document.node.LayerAlign;

document.pageFlow(page -> page
        .addContainer(stage -> stage
                .roundedRect(420, 160, 12)
                .fillColor(DocumentColor.WHITE)
                .padding(12)
                // RED declared first BUT with zIndex=10, so it renders on top.
                .position(redSquare(), -30, 0, LayerAlign.CENTER, 10)
                .position(tealSquare(), 30, 0, LayerAlign.CENTER)));
```

Without `zIndex`, source order controls the back-to-front stack — so
re-ordering source code is enough for most cases. Reach for `zIndex`
when:

* You compose a card top-down (background → content) but want a
  badge to land on top of both, and reordering source would split a
  cohesive section.
* You build layers programmatically (for example, from a list of
  data items) and need to pin a specific item to the top regardless
  of its source position.

z-index ordering is independent of `LayerAlign`, `position(...)`
offsets, and the transform — every layer keeps its anchor and offset
exactly as configured; only the render iteration shifts.

## Layout invariants you can rely on

The transform and z-index features pin three test invariants:

1. **`transformDoesNotShiftPlacementCoordinates`** — a 45°-rotated
   circle has identical `placementX` / `placementY` /
   `placementWidth` / `placementHeight` to the same circle without
   rotation. Layout snapshots therefore stay deterministic.
2. **`everyTransformBeginInArbitraryDocumentHasMatchingEndOnSamePage`**
   — every transform begin marker has a matching end marker with the
   same owner path on the same page; no nesting of the same owner.
   This is the architecture-guard for the PDF graphics-state stack.
3. **`equalZIndexLayersPreserveSourceOrder`** — the z-index sort is
   stable. When two layers share the same `zIndex`, source order
   wins.

## See also

- [`DocumentTransform`](../../src/main/java/com/demcha/compose/document/style/DocumentTransform.java) — value type with `none()` / `rotate()` / `scale()` factories.
- [`Transformable<T>`](../../src/main/java/com/demcha/compose/document/dsl/Transformable.java) — mixin interface, default rotate/scale shortcuts.
- [`LayerStackNode.Layer`](../../src/main/java/com/demcha/compose/document/node/LayerStackNode.java) — `zIndex` field plus back-compat constructors.
- [Shape-as-container recipe](shape-as-container.md) — base shape composition before adding transforms.
- Runnable example: `examples/src/main/java/com/demcha/examples/TransformsExample.java`.
