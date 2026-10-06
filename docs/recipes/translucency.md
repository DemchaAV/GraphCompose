# Translucency: alpha colours in PDF output

`DocumentColor` carries an optional alpha channel. The PDF backend renders
it as a **graphics-state alpha constant** — genuine translucency where the
content behind shows through, not a pre-mixed tint against an assumed
background. That matters on tinted pages and layered designs, where a
pre-mixed colour would only look right on white.

## Creating translucent colours

```java
import com.demcha.compose.document.style.DocumentColor;

DocumentColor glass  = DocumentColor.rgba(255, 255, 255, 178);  // alpha 0–255
DocumentColor tint   = DocumentColor.rgb(20, 80, 95).withOpacity(0.35);
```

`rgba(r, g, b, a)` takes an explicit alpha byte (0 = transparent, 255 =
opaque); `withOpacity(0.0–1.0)` derives a translucent copy of any existing
colour — handy for turning one brand colour into a family of tints.
Opacities outside `[0, 1]` are rejected at construction.

## What honours alpha (and what does not)

In the PDF backend, alpha applies on every surface:

- rectangles, panels (`softPanel(...)`), and chart bars
- ellipses, including chart point markers
- polygons (pie/donut slices, line-chart area fills)
- inline shapes
- chart value-label halo chips
- text runs, lines, side borders, table fills and rules, and barcodes

The semantic DOCX export keeps the alpha where Word can hold it — text, as
Word's text fill, and drawings and pictures — and flattens it where Word
holds an opaque colour only: a table cell's shading, a border, a rule. A
flattened colour is composited against what Word paints under it, so it looks
as the PDF does on first opening, and the export report names it. See
[Translucent colours](docx-export.md#translucent-colours) in the DOCX recipe.

## Opaque colours stay byte-identical

The alpha machinery only engages for translucent colours: a fully opaque
fill or stroke emits **no extended graphics state at all**, so documents
that never use alpha produce byte-identical PDFs before and after this
feature. Each translucent fill is also scoped to its own fragment — the
alpha never leaks into content drawn afterwards.

## Recipe: a translucent panel over a page background

A frosted-glass card that lets the page tint shimmer through
(see [page-backgrounds.md](page-backgrounds.md) for the background API):

```java
document.pageBackground(DocumentColor.rgb(28, 42, 56));     // deep navy page

document.pageFlow()
        .addSection("GlassCard", section -> section
                .softPanel(DocumentColor.rgba(255, 255, 255, 200), 8, 16)
                .addParagraph(p -> p.text("Readable on navy, without going solid white.")))
        .build();
```

## Recipe: chart value-label halos on tinted cards

Line-chart value labels draw behind a halo chip that is themed white. On a
tinted card a solid white chip looks like a sticker — a translucent halo
blends into the card instead (full chart API in [charts.md](charts.md)):

```java
section.chart(lineSpec, ChartStyle.builder()
        .valueLabelHalo(DocumentPaint.solid(DocumentColor.rgba(255, 255, 255, 190)))
        .build());
```

The same idea drives `area(true)` line charts: area fills use genuine
graphics-state alpha (`ChartStyle.areaOpacity`, default 0.35), so
overlapping series stay legible.

## Recipe: layered tints from one brand colour

Because the alpha mixes with whatever is behind it at render time, one
base colour plus `withOpacity` gives consistent tint steps on any surface:

```java
DocumentColor brand = DocumentColor.rgb(20, 80, 95);

section.addShape(120, 24, brand.withOpacity(0.15));   // faint band
section.addShape(120, 24, brand.withOpacity(0.45));   // mid band
section.addShape(120, 24, brand);                     // full strength
```

Verified end-to-end (including the opaque byte-identity guarantee) by
[`PdfShapeAlphaTest`](../../render-pdf/src/test/java/com/demcha/compose/document/backend/fixed/pdf/PdfShapeAlphaTest.java).
