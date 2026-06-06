# Page backgrounds

A page background is a rectangular fill painted **behind every other fragment**
(z = 0) and **repeated automatically on every page**. Backgrounds are defined as
ratios of the page size, so one definition scales to any page format. They are
the canonical way to paint a sidebar column, a header band, or a full-page tint
without adding a node to the flow.

Two entry points on `DocumentSession`:

- `pageBackground(DocumentColor)` — a single full-page tint (the common case).
- `pageBackgrounds(List<PageBackgroundFill>)` — one or more partial fills
  (columns, bands, layered tints).

## Full-page tint

```java
try (DocumentSession document = GraphCompose.document()
        .pageSize(DocumentPageSize.A4)
        .create()) {
    document.pageBackground(DocumentColor.rgb(252, 250, 246));
    document.pageFlow()
            .addParagraph("On warm paper, on every page.")
            .build();
}
```

## Sidebar column

`PageBackgroundFill.leftColumn(widthRatio, color)` paints a full-height column
flush with the left edge. Because the fill repeats on every page, a two-column
CV keeps its sidebar tint across page breaks for free.

```java
import com.demcha.compose.document.api.PageBackgroundFill;

document.pageBackgrounds(List.of(
        PageBackgroundFill.leftColumn(0.34, DocumentColor.rgb(28, 42, 56))));
```

`rightColumn(widthRatio, color)` and `column(xRatio, widthRatio, color)` cover
the right edge and arbitrary horizontal offsets.

## Header / footer bands

```java
document.pageBackgrounds(List.of(
        PageBackgroundFill.topBand(0.12, brand),        // top 12% of the page
        PageBackgroundFill.bottomBand(0.06, muted)));   // bottom 6%
```

`band(yRatioFromTop, heightRatio, color)` places a full-width band at any
vertical offset. When you would rather think in points than ratios, the
`*Points` factories convert for you against a reference page height:

```java
double pageHeight = document.canvas().height();
document.pageBackgrounds(List.of(
        PageBackgroundFill.topBandPoints(72, pageHeight, brand),          // 72pt band
        PageBackgroundFill.bandPoints(120, 4, pageHeight, accentRule)));  // 4pt rule at y = 120pt
```

## Layering

Fills paint at z = 0 in **list order**, so later entries paint on top of earlier
ones. The natural way to layer a narrow accent column over a full-page tint is
to list the tint first:

```java
document.pageBackgrounds(List.of(
        PageBackgroundFill.fullPage(DocumentColor.rgb(252, 250, 246)),         // base tint
        PageBackgroundFill.leftColumn(0.34, DocumentColor.rgb(28, 42, 56))));  // sidebar on top
```

Every fill stays behind the page-flow content — backgrounds never participate in
layout or pagination, so adding or changing them never shifts a single line of
text.

## When *not* to use a page background

- Content that must flow, wrap, or paginate → use `pageFlow()` nodes
  (`addSection`, `softPanel(...)`, `band(...)`), not a page background.
- A fill bound to one block (a card, a callout) → `softPanel(...)` or
  `fillColor(...)` on that flow node.
- Pixel-precise, non-repeating placement → an
  [absolute-placement canvas](absolute-placement.md).

## See also

- [`PageBackgroundFill`](../../src/main/java/com/demcha/compose/document/api/PageBackgroundFill.java) — every factory, with ratio semantics.
- `DocumentSession.pageBackground(...)` / `pageBackgrounds(...)`.
- [Layered page design](layered-page-design.md) — choosing between page backgrounds, rows, layer stacks, and canvases.
