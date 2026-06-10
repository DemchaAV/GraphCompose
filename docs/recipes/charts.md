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
| Style | `ChartStyle` over `ChartTheme` tokens | *how it looks* — cascading nullable fields, CSS-style merge |
| Geometry | `ChartLayoutResolver` | internal pure function `(data, spec, style) → primitives` |

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
ring is the classic separator. Per-point value labels draw above markers
behind a halo chip (`ChartStyle.valueLabelHalo`, themed white — match it to
your card colour on tinted surfaces, including translucent paints via
`DocumentColor.rgba(...)`). When two series' labels would collide at the same
category, the lower one automatically flips below its point.

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

Slices are arc-tessellated vector polygons. `sliceStroke` (themed white 1pt)
separates adjacent slices; `startAngleDegrees` / `clockwise(false)` control
layout. Negative values and multi-series data are rejected loudly.

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

`ChartTheme` tokens → document `ChartStyle` → per-series override, merged like
CSS (every `ChartStyle` field is nullable = inherit):

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
