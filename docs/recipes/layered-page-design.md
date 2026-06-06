# Layered page design

GraphCompose has four ways to put something "on top of" or "beside" something
else. Picking the right one is the difference between a layout that paginates
cleanly and one that fights the engine. This page is the decision guide.

## The four tools

| Tool | Reach for it when | API |
| --- | --- | --- |
| **Page background** | A fill must sit behind everything and repeat on every page — a sidebar tint, a header band, a watermark wash. Ratio-based; never participates in layout. | `pageBackgrounds(List.of(PageBackgroundFill...))` |
| **Row** | Content sits **side by side** and should flow / wrap / paginate — a sidebar + main column, a label + value. Columns are weighted; the row is atomic. | `addRow(row -> row.weights(...).addSection(...))` |
| **Layer stack / shape container** | Layers **overlap** and align to each other (centre, edges, offsets) with optional `zIndex` and a clip — a badge over a card, a label inside a pill. Sizes to its content. | `addContainer(...)` with `.center(...)` / `.position(child, dx, dy, LayerAlign)` |
| **Canvas** | You want **pixel-precise `(x, y)`** placement in a fixed box, no flow at all — a certificate, a diploma, a poster. | `addCanvas(w, h, canvas -> canvas.position(child, x, y))` |

## Worked distinctions

### Sidebar: page background vs. row

A **tinted** sidebar that must repeat on every page is a page background — it
costs nothing at layout time and never shifts content:

```java
document.pageBackgrounds(List.of(
        PageBackgroundFill.leftColumn(0.34, sidebarTint)));
```

A sidebar that holds **content** (skills, contacts, dates) is a row column — it
flows and paginates with the main column:

```java
document.pageFlow()
        .addRow(row -> row
                .weights(0.34, 0.66)
                .addSection(sidebar -> sidebar.addParagraph("Skills").addParagraph("..."))
                .addSection(main -> main.addParagraph("Experience").addParagraph("...")))
        .build();
```

Often you want **both**: the tint as a page background and the content as a row
column over it.

### Overlap: layer stack vs. canvas

A badge centred on a card, sizing to the card, is a **layer stack** — use
alignment, not coordinates:

```java
document.pageFlow()
        .addContainer(card -> card
                .roundedRect(320, 120, 12)
                .fillColor(DocumentColor.WHITE)
                .center(badge()));
```

A badge at an exact spot in a fixed certificate is a **canvas** — use
coordinates:

```java
document.pageFlow()
        .addCanvas(523, 300, canvas -> canvas.position(badge(), 430, 40))
        .build();
```

Rule of thumb: **alignment relationships → layer stack; absolute coordinates →
canvas; flow relationships → row; page-spanning fills → page background.**

## See also

- [Page backgrounds](page-backgrounds.md) · [Absolute placement](absolute-placement.md) · [Shape-as-container](shape-as-container.md) · [Transforms and z-index](transforms.md)
