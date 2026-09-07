# Layered page design

GraphCompose has four ways to put something "on top of" or "beside" something
else. Picking the right one is the difference between a layout that paginates
cleanly and one that fights the engine. This page is the decision guide.

## The four tools

<!-- claim: capability=layout.choose-the-layer -->
<!-- claim: symbol=AbstractFlowBuilder.addLayerStack -->
<!-- claim: symbol=AbstractFlowBuilder.addCanvas -->
<!-- claim: symbol=CanvasLayerBuilder.position -->

| Tool | Reach for it when | API |
| --- | --- | --- |
| **Page background** | A fill must sit behind everything and repeat on every page — a sidebar tint, a header band, a watermark wash. Ratio-based; never participates in layout. | `pageBackgrounds(List.of(PageBackgroundFill...))` |
| **Row** | Content sits **side by side** and should flow / wrap / paginate — a sidebar + main column, a label + value. Columns are weighted; the row is atomic. | `addRow(row -> row.weights(...).addSection(...))` |
| **Layer stack / shape container** | Layers **overlap** and align to each other (centre, edges, offsets) with optional `zIndex` and a clip — a badge over a card, a label inside a pill. Sizes to its content. | `addContainer(...)` with `.center(...)` / `.position(child, dx, dy, LayerAlign)` |
| **Canvas** | You want **pixel-precise `(x, y)`** placement in a fixed box, no flow at all — a certificate, a diploma, a poster. | `addCanvas(w, h, canvas -> canvas.position(child, x, y))` |

## Worked distinctions

### Sidebar: page background vs. row

<!-- claim: capability=layout.two-columns -->
<!-- claim: symbol=AbstractFlowBuilder.addRow -->
<!-- claim: symbol=RowBuilder.weights -->
<!-- claim: symbol=DocumentSession.pageBackgrounds -->
<!-- claim: symbol=PageBackgroundFill.leftColumn -->
<!-- claim: behavior=row.rejects-a-nested-row proof=test:RowBuilderTest -->
<!-- claim: behavior=row.auto-column-rejects-right-aligned-text proof=test:AutoColumnRightAlignContractTest -->

A **tinted** sidebar that must repeat on every page is a page background — it
costs nothing at layout time and never shifts content:

<!-- doc-example-ignore: shown as two lines to contrast with the row below; wrapping it in a session would hide the comparison this section exists to make -->
```java
document.pageBackgrounds(List.of(
        PageBackgroundFill.leftColumn(0.34, sidebarTint)));
```

A sidebar that holds **content** (skills, contacts, dates) is a row column — it
flows and paginates with the main column:

<!-- doc-example-ignore: the row form of the same sidebar, paired with the background above for contrast -->
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

### Icon beside text

<!-- claim: capability=layout.icon-beside-text -->
<!-- claim: symbol=RowBuilder.columns -->
<!-- claim: symbol=DocumentRowColumn.auto -->
<!-- claim: symbol=DocumentRowColumn.weight -->
<!-- claim: symbol=RowBuilder.flexSpacer -->
<!-- claim: symbol=ParagraphBuilder.inlineSvgIcon -->
<!-- claim: behavior=row.no-column-spec-splits-the-width-evenly proof=test:RowWidthDistributionContractTest -->
<!-- claim: behavior=row.a-flex-row-sizes-every-other-child-to-its-content proof=test:RowWidthDistributionContractTest -->

A row with no `columns(...)`, no `weights(...)`, no grow spacer and the default
`START` arrangement splits its inner width into **equal shares — one per child**.
Content plays no part. Two children means half each, whether the child is a
paragraph or a 13pt icon:

<!-- doc-example: id=row-icon-even-split mode=method imports=com.demcha.compose.GraphCompose,com.demcha.compose.document.api.DocumentSession,com.demcha.compose.document.svg.SvgIcon,java.nio.file.Path -->
```java
SvgIcon icon = SvgIcon.read(Path.of("check.svg"));

try (DocumentSession document = GraphCompose.document(Path.of("cv.pdf")).create()) {
    document.pageFlow(page -> page
            .addRow(row -> row
                    .spacing(8)
                    .add(icon.node(13))
                    .addParagraph(p -> p.text("Delivered 120+ events annually."))));
}
```

In a 135.7pt sidebar column that is a 63.85pt slot each. The icon still *draws*
at 13pt, so nothing looks broken — it simply occupies nearly five times the width
it needs, and the paragraph pays for it in line count.

Size the icon column instead: `auto()` takes the icon's own width, `weight(1)`
takes everything left.

<!-- doc-example: id=row-icon-auto-column mode=method imports=com.demcha.compose.GraphCompose,com.demcha.compose.document.api.DocumentSession,com.demcha.compose.document.style.DocumentRowColumn,com.demcha.compose.document.svg.SvgIcon,java.nio.file.Path -->
```java
SvgIcon icon = SvgIcon.read(Path.of("check.svg"));

try (DocumentSession document = GraphCompose.document(Path.of("cv.pdf")).create()) {
    document.pageFlow(page -> page
            .addRow(row -> row
                    .spacing(8)
                    .columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1))
                    .add(icon.node(13))
                    .addParagraph(p -> p.text("Delivered 120+ events annually."))));
}
```

Use `fixed(13)` in place of `auto()` when a column of icons must line up down a
list whatever each one contains. Reach for `weights(...)` only when both children
are content and you want a *proportion* — a weight is a share of the row, so a
ratio picked for one column width is wrong at another, and it can hand a
fixed-size icon a slot narrower than the icon.

A trailing `flexSpacer()` reaches the same widths by a different route, and this
is worth knowing because it makes two nearly identical rows disagree: a grow
spacer switches the row to intrinsic sizing, where **every** non-grow child takes
its natural width and the spacer absorbs the rest. Add one to push a value to the
right edge and the icon stops taking half the row as a side effect — but no child
in that row can be given a proportional share any more.

**A non-`START` arrangement does the same thing.** `arrangement(CENTER)`,
`END`, `SPACE_BETWEEN`, `SPACE_AROUND` and `SPACE_EVENLY` each put the row on the
identical intrinsic-sizing path with no spacer anywhere, so "no grow spacer" is
only half the condition for the even split — the other half is that the
arrangement is `START`. The two triggers are interchangeable, and `columns(...)`
or `weights(...)` cannot be combined with either: the row rejects that outright
rather than picking one.

When the icon belongs *in* the sentence rather than beside the block, there is no
row to distribute: `inlineSvgIcon` puts it in the text run, where it wraps with
the line and cannot hold a left rail.

### A rule that reaches the column edge

<!-- claim: capability=layout.rule-beside-a-heading -->
<!-- claim: symbol=RowBuilder.addLine -->
<!-- claim: symbol=LineBuilder.horizontal -->
<!-- claim: symbol=LineBuilder.fill -->
<!-- claim: behavior=line.horizontal-is-points-and-is-not-clipped proof=test:LineWidthUnitsContractTest -->
<!-- claim: behavior=line.fill-spans-its-slot proof=test:LineWidthUnitsContractTest -->
<!-- claim: behavior=line.fill-overflows-a-flex-row proof=test:LineWidthUnitsContractTest -->
<!-- claim: behavior=row.an-aligned-paragraph-in-an-auto-column-takes-the-whole-row proof=test:LineWidthUnitsContractTest -->

`horizontal(width)` takes **points**. Not a percentage — even though the numbers
next to it on this page are ratios (`weights(0.34, 0.66)`,
`leftColumn(0.34, ...)`), and a `double` named `width` looks like one:

<!-- doc-example: id=row-rule-fixed-width mode=method imports=com.demcha.compose.GraphCompose,com.demcha.compose.document.api.DocumentSession,com.demcha.compose.document.style.DocumentColor,java.nio.file.Path -->
```java
DocumentColor coral = DocumentColor.rgb(0xE2, 0x6D, 0x5A);

try (DocumentSession document = GraphCompose.document(Path.of("cv.pdf")).create()) {
    document.pageFlow(page -> page
            .addRow(row -> row
                    .addParagraph(p -> p.text("CORE COMPETENCIES"))
                    .addLine(line -> line.horizontal(100).color(coral))));
}
```

Nothing clips a line to the space it was given. In a 135.7pt column the even
split above hands the rule a 67.85pt slot and it is drawn at 100pt anyway —
32.15pt past the column, across whatever is beside it, with no warning and a
clean render.

A rule that must *meet* the column edge is `fill()`, which stretches a horizontal
line to the slot it is placed in. Pair it with a weight column so the slot is the
leftover width rather than half the row:

<!-- doc-example: id=row-rule-fill mode=method imports=com.demcha.compose.GraphCompose,com.demcha.compose.document.api.DocumentSession,com.demcha.compose.document.style.DocumentColor,com.demcha.compose.document.style.DocumentRowColumn,java.nio.file.Path -->
```java
DocumentColor coral = DocumentColor.rgb(0xE2, 0x6D, 0x5A);

try (DocumentSession document = GraphCompose.document(Path.of("cv.pdf")).create()) {
    document.pageFlow(page -> page
            .addRow(row -> row
                    .spacing(6)
                    .columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1))
                    .addParagraph(p -> p.text("CORE COMPETENCIES"))
                    .addLine(line -> line.fill().color(coral))));
}
```

**Leave that heading unaligned.** A right- or centre-aligned paragraph claims the
full row width; the `auto()` column grants it, because a fixed-and-auto pair that
fits is not an error; and the weight column is left with nothing. The rule then
has width zero — it does not overflow and it does not throw, it simply is not
drawn. If the heading must be aligned, give it a `weight(...)` column instead of
`auto()`, so its share is decided by the row rather than by the text.

Keep `horizontal(n)` for what it is good at: a fixed-length accent under a title,
where the length is the design and not a measurement of the column.

Two repairs that look right and are not. A `flexSpacer()` before the rule *moves*
it without sizing it — in the 135.7pt column above, a 100pt rule still runs past
the edge, and it stops doing so only when whatever sits beside it is narrow
enough to leave the spacer some slack, which is a property of that row and not a
fix. And `fill()` in a **flex row** — one with a grow spacer *or* a non-`START`
arrangement — overflows further than the fixed rule it replaced: the flex path
asks every non-grow child for its natural width, a fill line answers with the
row's whole available width, and it is then placed after the other children.

What `fill()` needs is a weight column, not a spacer, and the row will not let
you have both: combining `columns(...)` or `weights(...)` with a grow spacer or a
non-`START` arrangement is rejected outright, with a message naming the two
strategies. So this is a choice between them rather than something to stack.

### Overlap: layer stack vs. canvas

A badge centred on a card, sizing to the card, is a **layer stack** — use
alignment, not coordinates:

<!-- doc-example-ignore: calls badge(), a stand-in for whatever the reader is overlaying -->
```java
document.pageFlow()
        .addContainer(card -> card
                .roundedRect(320, 120, 12)
                .fillColor(DocumentColor.WHITE)
                .center(badge()));
```

A badge at an exact spot in a fixed certificate is a **canvas** — use
coordinates:

<!-- doc-example-ignore: the canvas counterpart of the layer stack above; badge() is the same stand-in -->
```java
document.pageFlow()
        .addCanvas(523, 300, canvas -> canvas.position(badge(), 430, 40))
        .build();
```

Rule of thumb: **alignment relationships → layer stack; absolute coordinates →
canvas; flow relationships → row; page-spanning fills → page background.**

## See also

- [Page backgrounds](page-backgrounds.md) · [Absolute placement](absolute-placement.md) · [Shape-as-container](shape-as-container.md) · [Transforms and z-index](transforms.md)
