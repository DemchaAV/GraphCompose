# Shape-as-container

`ShapeContainerNode` is a composite whose bounding box is dictated by a
geometric outline (rectangle, rounded rectangle, ellipse, or circle), and
which hosts one or more child *layers* anchored inside that outline. It
differs from `LayerStackNode` in two ways:

| | `LayerStackNode` | `ShapeContainerNode` |
| --- | --- | --- |
| Bbox | `max(child outer size)` | The outline's intrinsic size |
| Clipping | None — children may escape the bbox | `ClipPolicy.CLIP_PATH` (default), `CLIP_BOUNDS`, or `OVERFLOW_VISIBLE` |
| Frame / fill | Needs a separate back layer | First-class part of the node (fill, stroke, corner radius) |
| Pagination | `ATOMIC` | `SHAPE_ATOMIC` — outline plus every layer stays on one page |

See [ADR 0001](../adr/0001-shape-as-container.md) for the rationale.

## Hello-circle

The shortest path: drop a circle on the page and put a label inside it.
The default `ClipPolicy.CLIP_PATH` ensures the label is clipped to the
circle's outline if it ever overflows.

```java
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextStyle;

DocumentColor brand = DocumentColor.rgb(180, 40, 40);
DocumentTextStyle headline = DocumentTextStyle.builder()
        .size(18)
        .color(DocumentColor.WHITE)
        .build();

document.pageFlow(page -> page
        .addCircle(80, brand, c -> c
                .name("BrandCircle")
                .center(new ParagraphBuilder()
                        .text("M&A")
                        .textStyle(headline)
                        .build())));
```

## Layered ellipse — badge + center label

`ShapeContainerBuilder` exposes the same nine-point alignment vocabulary
as `LayerStackBuilder`, so a badge in `topRight` plus a label in `center`
reads naturally:

```java
document.pageFlow(page -> page
        .addEllipse(160, 100, brand, e -> e
                .name("EllipseBadge")
                .topRight(new ParagraphBuilder()
                        .text("NEW")
                        .textStyle(DocumentTextStyle.builder().size(10).color(DocumentColor.WHITE).build())
                        .build())
                .center(new ParagraphBuilder()
                        .text("Featured project")
                        .textStyle(DocumentTextStyle.builder().size(14).color(DocumentColor.WHITE).build())
                        .build())));
```

The badge sits at the ellipse's top-right corner and travels with it —
under `SHAPE_ATOMIC` the whole composite moves as one unit if it doesn't
fit on the current page.

## Rounded card with accent and rich-text body

A rounded rectangle outline reads as a card. Combine it with a fluent
`RichText` body for a lightweight callout:

```java
import com.demcha.compose.document.dsl.RichText;

document.pageFlow(page -> page
        .addContainer(card -> card
                .name("Highlight")
                .roundedRect(220, 90, 12)
                .fillColor(DocumentColor.rgb(20, 30, 60))
                .padding(12)
                .center(new ParagraphBuilder()
                        .rich(RichText.of()
                                .text("Status: ")
                                .bold("Pending"))
                        .textStyle(DocumentTextStyle.builder()
                                .size(13)
                                .color(DocumentColor.WHITE)
                                .build())
                        .build())));
```

## Choosing a clip policy

```java
import com.demcha.compose.document.style.ClipPolicy;
```

| Policy | When to use |
| --- | --- |
| `CLIP_PATH` *(default)* | The container *is* a shape and children should respect its outline. Choose this for anything that reads as "circle/ellipse/card with content inside". |
| `CLIP_BOUNDS` | Children should stay inside the axis-aligned bounding box but path clipping is unnecessary or undesirable (rectangle/rounded-rectangle outlines, no decorative overhangs). |
| `OVERFLOW_VISIBLE` | The outline is decorative and floating overlays should be allowed to stick out (e.g. a badge that overlaps the outline edge by design). The layout layer skips emitting clip markers entirely. |

```java
.addContainer(c -> c.circle(80).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE) ... )
```

## Edge cases

### Outline larger than the page

A `ShapeContainerNode` is `SHAPE_ATOMIC`: the outline plus every layer
stays on one page. A container too tall to fit on a single page raises
`AtomicNodeTooLargeException` with the offending semantic name so the
caller can either shrink the outline or pick `OVERFLOW_VISIBLE`.

```java
session.add(new ShapeContainerBuilder()
        .name("OversizedCard")
        .roundedRect(100, 600, 6)   // taller than the inner page
        .center(label)
        .build());

session.layoutGraph();              // → AtomicNodeTooLargeException
                                    //   message contains "OversizedCard"
```

### DOCX export

Apache POI cannot express a graphics-state path clip, so the
`DocxSemanticBackend` renders the container's *layers* inline without the
outline frame and without clipping, and logs a one-time
`docx.export.shape-container-fallback` capability warning per export
pass. Authors who need the outline must export to PDF.

This is documented in [canonical-legacy-parity.md](../canonical-legacy-parity.md)
under the "Surfaces and structure" section.

## How the rendering pipeline emits a clipped container

For `CLIP_PATH` / `CLIP_BOUNDS`, the canonical layout compiler emits this
fragment sequence on the shape's page:

1. **Outline fragment** — `EllipseFragmentPayload` (circle / ellipse) or
   `ShapeFragmentPayload` (rectangle / rounded-rectangle, with corner
   radius). Rendered first so it sits behind the layers.
2. **Clip-begin marker** — `ShapeClipBeginPayload` carrying the outline
   geometry and chosen policy. The PDF backend turns this into
   `saveGraphicsState() + add path + clip()`.
3. **Layer fragments** — each child layer is compiled normally; its
   fragments land on a page surface that is already restricted to the
   outline.
4. **Clip-end marker** — `ShapeClipEndPayload` with the same owner path.
   The PDF backend issues `restoreGraphicsState()` so subsequent
   fragments draw without the clip.

`ShapeContainerInvariantsTest` pins the begin/end pair invariant; you
can rely on it across releases.

## See also

- [ADR 0001 — Shape-as-container](../adr/0001-shape-as-container.md) — design rationale.
- [`ShapeContainerBuilder`](../../src/main/java/com/demcha/compose/document/dsl/ShapeContainerBuilder.java) — full builder surface.
- [`ShapeOutline`](../../src/main/java/com/demcha/compose/document/style/ShapeOutline.java) — sealed value type for the supported outline kinds.
