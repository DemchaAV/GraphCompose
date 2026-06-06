# Absolute placement (canvas)

When you need pixel-precise control — a certificate, a badge, a diploma, a
poster — reach for a **canvas layer**. `addCanvas(width, height, canvas -> ...)`
reserves a fixed-size rectangle in the surrounding flow and lets you drop
children at explicit `(x, y)` coordinates inside it.

`(0, 0)` is the canvas's **top-left** corner; positive `x` extends right and
positive `y` extends down (the screen convention). The canvas occupies its
declared `width x height` in the flow no matter where its children land.

## A positioned certificate block

```java
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;

document.pageFlow()
        .addCanvas(523, 300, canvas -> canvas
                .name("Certificate")
                .clipPolicy(ClipPolicy.CLIP_BOUNDS)
                .position(new ParagraphBuilder()
                        .text("CERTIFICATE OF ACHIEVEMENT")
                        .textStyle(eyebrow)
                        .align(TextAlign.CENTER)
                        .build(), 0, 40)
                .position(new ParagraphBuilder()
                        .text("Jordan Rivera")
                        .textStyle(name)
                        .align(TextAlign.CENTER)
                        .build(), 0, 90))
        .build();
```

A child placed at `x = 0` is prepared with the **full canvas width**, so
`TextAlign.CENTER` centres it across the whole canvas. A child placed at
`x > 0` inherits that same prepared width — give such a paragraph a horizontal
margin if it must not overflow the right edge.

## Clipping

`clipPolicy(ClipPolicy.CLIP_BOUNDS)` hides anything that overflows the canvas
rectangle. The default leaves overflow visible — useful for a decorative element
that intentionally bleeds past the box.

## Canvas vs. the flow

The canvas does **not** flow or wrap its children — they sit exactly where you
place them. Everything outside the canvas (the surrounding `pageFlow()`) still
flows and paginates normally; the canvas is one fixed block within it. Because
the box is fixed, a canvas does not split across a page break — keep its height
within one page.

## See also

- Runnable example: [`CanvasLayerExample`](../../examples/src/main/java/com/demcha/examples/features/canvas/CanvasLayerExample.java) — a full certificate.
- [`CanvasLayerNode`](../../src/main/java/com/demcha/compose/document/node/CanvasLayerNode.java) — the node behind `addCanvas`.
- [Layered page design](layered-page-design.md) — canvas vs. layer stack vs. row vs. page background.
