# Charts: native vector bar, line, area, and pie/donut

GraphCompose charts are **not rasterised images**. A `ChartNode` is compiled
at layout time into the same primitives everything else uses (shapes, lines,
polygons, paragraphs), so charts are deterministic, snapshot-testable,
theme-stylable, and render as crisp vectors in every fixed-layout backend
with zero chart-specific render code.

The API is split into independent layers so nothing is baked in:

| Layer | Type | Answers |
|---|---|---|
| Data | `ChartData` | *what numbers* — categories + series, knows nothing about type or colour |
| Spec | `ChartSpec` (sealed: `bar()` / `line()` / `pie()`) | *what to show* — orientation, axes, legend, labels, sizing |
| Style | `ChartStyle` | *how it looks* — nullable fields merged CSS-style over the built-in `ChartTheme` |
| Geometry | `ChartLayoutResolver` | *where the shapes come from* — a pure `(spec, style, theme, size, metrics) → primitives` function |

All chart types live in `com.demcha.compose.document.chart`.

## Data: one dataset, any chart kind

```java
import com.demcha.compose.document.chart.ChartData;

ChartData revenue = ChartData.builder()
        .categories("Q1", "Q2", "Q3", "Q4")
        .series("2024", 12.4, 15.1, 9.8, 14.2)
        .series("2025", 14.0, 18.2, 11.3, 16.9)
        .build();
```

Every series must align with the categories (a ragged dataset fails at
construction). A `null` value means a *missing point*: a gap in a line, a
skipped bar — distinct from `0`.

## Bar charts

```java
import com.demcha.compose.document.chart.*;

section.chart(ChartSpec.bar()
        .data(revenue)
        .grouping(BarGrouping.GROUPED)         // or STACKED
        .valueAxis(AxisSpec.builder()
                .baselineAtZero(true)
                .format(NumberFormatSpec.pattern("#,##0.0").withSuffix("k"))
                .build())
        .legend(LegendPosition.BOTTOM)         // NONE / BOTTOM / TOP / RIGHT
        .valueLabels(ValueLabelMode.OUTSIDE)   // numbers above each bar
        .size(ChartSize.aspectRatio(16, 9))    // width from container
        .build());
```

- `grouping(STACKED)` stacks the series; `valueLabels(OUTSIDE)` then labels
  each category **total**.
- `horizontal(true)` transposes the chart: categories run down the Y axis in
  reading order, values grow right, labels sit at the bar ends.
- `AxisSpec.min(...)` / `max(...)` pin the axis to explicit bounds; ticks
  still land on nice 1/2/5 values.
- `ChartStyle.barWidthRatio(...)` sets how much of a category slot the bar
  group fills (default `0.72`). Lower it for airy, editorial bars; raise it
  toward `1.0` to close the gaps.

## Line, smooth, and area charts

```java
section.chart(ChartSpec.line()
        .data(revenue)
        .smooth(true)                          // Catmull-Rom curves
        .area(true)                            // translucent fill to baseline
        .legend(LegendPosition.TOP)
        .build());
```

`area(true)` fills each series down to the baseline with the series colour at
`ChartStyle.areaOpacity` (default 0.35) — overlapping series stay legible
because the fills are genuinely translucent (graphics-state alpha, not
pre-mixed tints).
`smooth(true)` subdivides each span at a fixed step, so geometry stays
deterministic; like any interpolating spline it may slightly overshoot sharp
local extremes.

### Point markers and value labels

```java
section.chart(lineSpec, ChartStyle.builder()
        .lineWidth(1.8)
        .pointMarker(PointMarker.circle(5.5)
                .withStroke(DocumentStroke.of(DocumentColor.WHITE, 1.2)))
        .valueLabelOffset(3)
        .build());
```

Markers are ellipses (`PointMarker.circle(d)` / `ellipse(w, h)`) drawn
**above every stroke**, so joints where lines meet stay readable; the white
ring is the classic separator. Per-point value labels draw above their marker,
each behind a halo chip — see [the value-label halo](#the-value-label-halo).
When two series' labels would collide at the same category, the lower one
automatically flips below its point.

`valueLabelOffset` (default `2`) is the gap between a label and the thing it
labels — the marker here, the bar end on a bar chart, the rim on a pie or donut.

## Pie and donut

```java
section.chart(ChartSpec.pie()
        .data(regions)                          // exactly ONE series
        .donutRatio(0.58)                       // 0 = solid pie
        .sliceLabels(SliceLabelMode.PERCENT)    // VALUE / CATEGORY / CATEGORY_PERCENT
        .centerText("58.4k")                    // KPI in the donut hole
        .legend(LegendPosition.BOTTOM)          // lists category names
        .build(),
    ChartStyle.builder()
        .sliceGapDegrees(2.0)                   // pad angle between slices
        .build());
```

Slices are arc-tessellated vector polygons. `sliceStroke` (white 1pt by
default) separates adjacent slices; `startAngleDegrees` / `clockwise(false)`
control layout. Negative values and multi-series data are rejected loudly.

`centerText` is the KPI in the hole, and `ChartStyle.donutCenterTextStyle(...)`
is what sizes and colours it — 13pt bold dark grey unless you say otherwise.
It is a plain `DocumentTextStyle`, so a bigger figure in the brand colour is
one call:

<!-- doc-example: id=charts-donut-center-style mode=members -->

```java
import com.demcha.compose.document.chart.ChartStyle;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

ChartStyle kpiDonut = ChartStyle.builder()
        .donutCenterTextStyle(DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .decoration(DocumentTextDecoration.BOLD)
                .size(22)
                .color(DocumentColor.rgb(20, 80, 95))
                .build())
        .build();
```

Slice labels use `valueLabelTextStyle` and the same halo as every other value
label.

## Hiding chrome: down to "just the bars"

Axis numbers, grid lines, and category labels are independent toggles, and
hidden chrome collapses its gutter:

```java
ChartSpec.bar().data(revenue)
        .valueAxis(AxisSpec.builder()
                .showGridLines(false)
                .showTickLabels(false)
                .build())
        .showCategoryLabels(false)
        .valueLabels(ValueLabelMode.OUTSIDE)    // only bars + numbers remain
        .build();
```

## Styling: the cascade

Every `ChartStyle` field is nullable, and null means *inherit*. The style you
pass to `chart(spec, style)` is merged CSS-style over `ChartDefaults.DEFAULT_THEME`,
so you set the handful of things you care about and the rest stays consistent:

```java
ChartStyle.builder()
        .seriesPaint(0, DocumentPaint.solid(DocumentColor.rgb(20, 80, 95)))
        .seriesPaint(1, DocumentPaint.solid(DocumentColor.rgb(196, 153, 76)))
        .barCornerRadius(DocumentCornerRadius.top(2))
        .grid(ChartStyle.GridStyle.horizontal(
                DocumentStroke.of(DocumentColor.rgb(224, 224, 224), 0.5)))
        .build();
```

The palette cycles by modulo, so a chart never runs out of colours.

`ChartTheme` is that base set of tokens, and the authoring API does not currently
expose a way to swap it: a chart resolves its geometry during the layout pass,
after the document's theme is out of reach, so every chart placed through the DSL
starts from `ChartDefaults.DEFAULT_THEME` and `ChartStyle` is the author-facing
override. Give charts that must match a brand a shared `ChartStyle` constant and
pass it to each one.

`ChartLayoutResolver.resolve(...)` does take an explicit `ChartTheme`, but it
returns raw primitives rather than placing a chart in a document — that is the
geometry seam, useful for tooling and tests, not a second way to author.

### Typography

Three text styles cover the chrome, all plain `DocumentTextStyle` (the fourth,
[`donutCenterTextStyle`](#pie-and-donut), belongs to the donut hole):

<!-- doc-example: id=charts-typography mode=members -->

```java
import com.demcha.compose.document.chart.ChartStyle;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

ChartStyle onDark = ChartStyle.builder()
        .axisTextStyle(label(7.5, DocumentColor.rgb(150, 160, 175)))    // ticks + categories
        .legendTextStyle(label(8, DocumentColor.rgb(150, 160, 175)))    // series names
        .valueLabelTextStyle(label(9, DocumentColor.WHITE))             // numbers on the data
        .build();

static DocumentTextStyle label(double size, DocumentColor color) {
    return DocumentTextStyle.builder()
            .fontName(FontName.HELVETICA)
            .size(size)
            .color(color)
            .build();
}
```

`axisTextStyle` covers both the numeric ticks and the category labels — they
are the same chrome and read best when they match. Defaults are 8pt/9pt/8pt
grey, tuned for a white page: on a dark card you will want to set all three,
and the halo below along with them.

### The value-label halo

`valueLabelHalo` is the chip drawn *behind* a value or slice label so the digits
stay legible where the chart's own graphics run under them — a grid line, a
series stroke, a slice edge. It is a `DocumentPaint`, white by default, which is
right on a white page and wrong everywhere else: on a tinted card an unset halo
paints white rectangles across your background.

Set it to the surface the chart sits on:

<!-- doc-example: id=charts-halo mode=members -->

```java
import com.demcha.compose.document.chart.ChartStyle;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentPaint;

static final DocumentColor CARD = DocumentColor.rgb(18, 24, 38);

ChartStyle onCard = ChartStyle.builder()
        .valueLabelHalo(DocumentPaint.solid(CARD))                  // match the card
        .build();

ChartStyle softened = ChartStyle.builder()
        .valueLabelHalo(DocumentPaint.solid(CARD.withOpacity(0.6)))  // let the grid show
        .build();
```

`withOpacity(...)` makes the chip genuinely translucent — real graphics-state
alpha, so the grid stays faintly visible through it instead of being punched
out. Useful when a solid chip reads as a sticker. (`DocumentColor.rgba(r, g, b, a)`
does the same with an integer 0–255 alpha.)

### Every `ChartStyle` field

| Setting | Default | Applies to |
|---|---|---|
| `palette(...)` / `seriesPaint(i, ...)` | 8-colour Tableau-inspired palette † | all |
| `lineWidth(double)` | `1.5` | line |
| `pointMarker(PointMarker)` | none | line |
| `areaOpacity(double)` | `0.35` | line with `area(true)` |
| `barCornerRadius(DocumentCornerRadius)` | square corners | bar |
| `barWidthRatio(double)` | `0.72` | bar |
| `grid(GridStyle)` | horizontal, 0.5pt `#E0E0E0` † | bar, line |
| `axisTextStyle(DocumentTextStyle)` | 8pt `#5A5A5A` † | tick + category labels |
| `legendTextStyle(DocumentTextStyle)` | 9pt `#3C3C3C` † | legend |
| `valueLabelTextStyle(DocumentTextStyle)` | 8pt `#3C3C3C` † | value + slice labels |
| `valueLabelHalo(DocumentPaint)` | white † | chip behind those labels |
| `valueLabelOffset(double)` | `2` | gap from a line marker, bar end or pie edge |
| `sliceStroke(DocumentStroke)` | white 1pt | pie, donut |
| `sliceGapDegrees(double)` | `0` | pie, donut |
| `donutCenterTextStyle(DocumentTextStyle)` | 13pt bold `#2D2D2D` | donut centre |

† inherited from `ChartDefaults.DEFAULT_THEME`; unmarked rows are fixed engine
defaults, some of them constants in `ChartDefaults` and some — `pointMarker`,
square corners — simply the absence of an override.

## Inline sparklines

Mini-charts that sit on the text baseline like any other inline shape — a
skill trend in a CV line, a KPI direction next to a number:

```java
section.addRich(r -> r
        .plain("Revenue trend ")
        .sparkline(42, 9, accent, 65.2, 69.8, 74.1, 81.3, 88.2)      // filled area
        .plain("   profit ")
        .sparklineLine(42, 9, 1.6, gold, 28.1, 30.7, 32.9, 36.4, 39.5)); // line band
```

The run's minimum maps to the bottom of the box and its maximum to the top;
`sparklineLine` keeps a constant thickness even at the peaks. Combine with
`DocumentColor.withOpacity(...)` for a softer area fill.

## Backends

Any fixed-layout backend (PDF today) renders charts for free — they are
ordinary primitives by the time rendering starts. The semantic DOCX export
has no layout pass, so it writes the chart's **data table**
(categories × series) with a one-time capability warning.

Unsupported combinations fail fast with `UnsupportedOperationException`
rather than rendering silently wrong; in the current release that is only
`ValueLabelMode.INSIDE`.

Runnable showcase: `examples/.../features/charts/ChartShowcaseExample.java`
([rendered PDF](../../assets/readme/examples/chart-showcase.pdf)). A
real-document integration lives in the flagship
`BusinessReportExample` (navy/gold restyled chart inside a dashboard page).
