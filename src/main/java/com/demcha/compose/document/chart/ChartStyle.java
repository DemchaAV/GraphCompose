package com.demcha.compose.document.chart;

import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Visual styling for one chart, expressed as the middle tier of a three-level
 * cascade:
 *
 * <pre>
 *   theme chart tokens   →   this ChartStyle   →   per-series override
 * </pre>
 *
 * <p>Every field is nullable / empty-by-default and means "inherit". The
 * resolver coalesces document {@code ChartStyle} over the active
 * {@link ChartTheme} tokens via {@link #mergedUnder(ChartStyle)}, then applies
 * any per-series override on top — the same merge discipline used elsewhere in
 * the engine. A chart with a {@code null} style therefore renders fully themed
 * with zero authoring.</p>
 *
 * <p>Note how few new types this needs: {@link DocumentStroke},
 * {@link DocumentCornerRadius}, and {@link DocumentTextStyle} already exist; the
 * chart-specific additions are the {@link DocumentPaint} palette (shared with
 * the gradient work), the {@link PointMarker} ellipse marker, and the small
 * {@link GridStyle} record below.</p>
 *
 * @param palette              ordered series paints
 * @param seriesPaintOverrides explicit per-series paint by zero-based index
 * @param lineWidth            stroke width for line series in points, or {@code null} for
 *                             the default; series colour always comes from the paint cascade
 * @param barCornerRadius      per-bar corner radius, or {@code null}
 * @param barWidthRatio        fraction (0,1] of the category slot a bar group fills
 * @param grid                 grid-line configuration
 * @param pointMarker          ellipse marker drawn at every line-chart data point, or
 *                             {@code null} for none; see {@link PointMarker}
 * @param valueLabelOffset     gap in points between a bar top / point marker and its
 *                             value label, or {@code null} for the default
 * @param axisTextStyle        tick / category label style
 * @param legendTextStyle      legend label style
 * @param valueLabelTextStyle  value-label style
 * @param valueLabelHalo       halo chip painted behind line-chart value labels so the
 *                             digits stay legible where line strokes cross them; match
 *                             it to the chart's surface colour on non-white backgrounds
 * @param areaOpacity          opacity of the area fill under {@code ChartSpec.line().area(true)}
 *                             series, in (0..1], or {@code null} for the default (0.35)
 * @param sliceStroke          pie/donut slice separator stroke, or {@code null} for the
 *                             themed default (white 1pt)
 * @param sliceGapDegrees      angular gap between pie/donut slices (pad angle), or
 *                             {@code null} for none
 * @param donutCenterTextStyle style for the donut-centre KPI text, or
 *                             {@code null} for the themed default
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public record ChartStyle(
        List<DocumentPaint> palette,
        Map<Integer, DocumentPaint> seriesPaintOverrides,
        Double lineWidth,
        DocumentCornerRadius barCornerRadius,
        Double barWidthRatio,
        GridStyle grid,
        PointMarker pointMarker,
        Double valueLabelOffset,
        DocumentTextStyle axisTextStyle,
        DocumentTextStyle legendTextStyle,
        DocumentTextStyle valueLabelTextStyle,
        DocumentPaint valueLabelHalo,
        Double areaOpacity,
        DocumentStroke sliceStroke,
        Double sliceGapDegrees,
        DocumentTextStyle donutCenterTextStyle
) {
    /**
     * Copy-protects collections and validates ratio bounds.
     */
    public ChartStyle {
        palette = palette == null ? List.of() : List.copyOf(palette);
        seriesPaintOverrides = seriesPaintOverrides == null
                ? Map.of() : Map.copyOf(seriesPaintOverrides);
        if (lineWidth != null && (lineWidth <= 0 || lineWidth.isNaN() || lineWidth.isInfinite())) {
            throw new IllegalArgumentException("lineWidth must be finite and positive: " + lineWidth);
        }
        if (barWidthRatio != null && (barWidthRatio <= 0 || barWidthRatio > 1)) {
            throw new IllegalArgumentException("barWidthRatio must be in (0,1]: " + barWidthRatio);
        }
        if (valueLabelOffset != null && (valueLabelOffset < 0
                                         || valueLabelOffset.isNaN() || valueLabelOffset.isInfinite())) {
            throw new IllegalArgumentException(
                    "valueLabelOffset must be finite and non-negative: " + valueLabelOffset);
        }
        if (areaOpacity != null && (areaOpacity <= 0 || areaOpacity > 1 || areaOpacity.isNaN())) {
            throw new IllegalArgumentException("areaOpacity must be in (0,1]: " + areaOpacity);
        }
        if (sliceGapDegrees != null && (sliceGapDegrees < 0 || sliceGapDegrees > 30
                                        || sliceGapDegrees.isNaN())) {
            throw new IllegalArgumentException(
                    "sliceGapDegrees must be in [0, 30]: " + sliceGapDegrees);
        }
    }

    /**
     * Empty style — inherit everything from the theme.
     *
     * @return an all-inherit style
     */
    public static ChartStyle inherit() {
        return new ChartStyle(List.of(), Map.of(), null, null, null,
                null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Starts a mutable builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Coalesces {@code top} over this style: every non-empty / non-null field of
     * {@code top} wins, otherwise this style's value is kept. Used by the
     * resolver to layer a document-level {@code ChartStyle} over the theme base
     * (so {@code themeBase.mergedUnder(authorStyle)} yields author-wins-over-theme).
     *
     * @param top the higher-priority style, or {@code null} to keep this
     * @return the merged style
     */
    public ChartStyle mergedUnder(ChartStyle top) {
        if (top == null) {
            return this;
        }
        List<DocumentPaint> mergedPalette = top.palette.isEmpty() ? this.palette : top.palette;
        Map<Integer, DocumentPaint> mergedOverrides = new HashMap<>(this.seriesPaintOverrides);
        mergedOverrides.putAll(top.seriesPaintOverrides);
        return new ChartStyle(
                mergedPalette,
                mergedOverrides,
                top.lineWidth != null ? top.lineWidth : this.lineWidth,
                top.barCornerRadius != null ? top.barCornerRadius : this.barCornerRadius,
                top.barWidthRatio != null ? top.barWidthRatio : this.barWidthRatio,
                top.grid != null ? top.grid : this.grid,
                top.pointMarker != null ? top.pointMarker : this.pointMarker,
                top.valueLabelOffset != null ? top.valueLabelOffset : this.valueLabelOffset,
                top.axisTextStyle != null ? top.axisTextStyle : this.axisTextStyle,
                top.legendTextStyle != null ? top.legendTextStyle : this.legendTextStyle,
                top.valueLabelTextStyle != null ? top.valueLabelTextStyle : this.valueLabelTextStyle,
                top.valueLabelHalo != null ? top.valueLabelHalo : this.valueLabelHalo,
                top.areaOpacity != null ? top.areaOpacity : this.areaOpacity,
                top.sliceStroke != null ? top.sliceStroke : this.sliceStroke,
                top.sliceGapDegrees != null ? top.sliceGapDegrees : this.sliceGapDegrees,
                top.donutCenterTextStyle != null ? top.donutCenterTextStyle : this.donutCenterTextStyle);
    }

    /**
     * Resolves the paint for series {@code index}: explicit override first, then
     * the style palette, then the supplied fallback palette, cycling by modulo so
     * a chart never runs out of colours.
     *
     * @param index           zero-based series index
     * @param fallbackPalette palette to use when neither override nor style palette applies
     * @return the paint to fill / stroke this series with
     */
    public DocumentPaint paintForSeries(int index, List<DocumentPaint> fallbackPalette) {
        DocumentPaint override = seriesPaintOverrides.get(index);
        if (override != null) {
            return override;
        }
        List<DocumentPaint> active = palette.isEmpty() ? fallbackPalette : palette;
        if (active == null || active.isEmpty()) {
            throw new IllegalStateException("no chart palette available (style + fallback both empty)");
        }
        return active.get(index % active.size());
    }

    /**
     * Grid line configuration. Either axis may be {@code null} to suppress that
     * grid family entirely.
     *
     * @param horizontal stroke for horizontal grid lines (value gridlines)
     * @param vertical   stroke for vertical grid lines (category separators)
     */
    public record GridStyle(DocumentStroke horizontal, DocumentStroke vertical) {
        /**
         * Horizontal grid lines only.
         *
         * @param stroke horizontal grid stroke
         * @return grid style
         */
        public static GridStyle horizontal(DocumentStroke stroke) {
            return new GridStyle(stroke, null);
        }

        /**
         * No grid lines.
         *
         * @return grid style
         */
        public static GridStyle none() {
            return new GridStyle(null, null);
        }
    }

    /**
     * Fluent builder; all setters optional.
     */
    public static final class Builder {
        private final List<DocumentPaint> palette = new ArrayList<>();
        private final Map<Integer, DocumentPaint> overrides = new HashMap<>();
        private Double lineWidth;
        private DocumentCornerRadius barCornerRadius;
        private Double barWidthRatio;
        private GridStyle grid;
        private PointMarker pointMarker;
        private Double valueLabelOffset;
        private DocumentTextStyle axisTextStyle;
        private DocumentTextStyle legendTextStyle;
        private DocumentTextStyle valueLabelTextStyle;
        private DocumentPaint valueLabelHalo;
        private Double areaOpacity;
        private DocumentStroke sliceStroke;
        private Double sliceGapDegrees;
        private DocumentTextStyle donutCenterTextStyle;

        /**
         * Sets the series palette.
         *
         * @param paints ordered paints
         * @return this builder
         */
        public Builder palette(DocumentPaint... paints) {
            this.palette.addAll(List.of(paints));
            return this;
        }

        /**
         * Overrides one series' paint.
         *
         * @param index zero-based series index
         * @param paint paint
         * @return this builder
         */
        public Builder seriesPaint(int index, DocumentPaint paint) {
            this.overrides.put(index, paint);
            return this;
        }

        /**
         * Sets the line-series stroke width in points. Series colour always
         * comes from the paint cascade ({@link #seriesPaint(int, DocumentPaint)}
         * / palette).
         *
         * @param width stroke width in points
         * @return this builder
         */
        public Builder lineWidth(double width) {
            this.lineWidth = width;
            return this;
        }

        /**
         * Sets the bar corner radius.
         *
         * @param r corner radius
         * @return this builder
         */
        public Builder barCornerRadius(DocumentCornerRadius r) {
            this.barCornerRadius = r;
            return this;
        }

        /**
         * Sets the bar group width ratio.
         *
         * @param ratio fraction (0,1] of the slot a bar group fills
         * @return this builder
         */
        public Builder barWidthRatio(double ratio) {
            this.barWidthRatio = ratio;
            return this;
        }

        /**
         * Sets the grid style.
         *
         * @param grid grid style
         * @return this builder
         */
        public Builder grid(GridStyle grid) {
            this.grid = grid;
            return this;
        }

        /**
         * Sets the ellipse marker drawn at every line-chart data point.
         *
         * @param marker marker configuration; see {@link PointMarker}
         * @return this builder
         */
        public Builder pointMarker(PointMarker marker) {
            this.pointMarker = marker;
            return this;
        }

        /**
         * Sets the gap between a bar top / point marker and its value label.
         *
         * @param offset gap in points
         * @return this builder
         */
        public Builder valueLabelOffset(double offset) {
            this.valueLabelOffset = offset;
            return this;
        }

        /**
         * Sets the axis text style.
         *
         * @param s text style
         * @return this builder
         */
        public Builder axisTextStyle(DocumentTextStyle s) {
            this.axisTextStyle = s;
            return this;
        }

        /**
         * Sets the legend text style.
         *
         * @param s text style
         * @return this builder
         */
        public Builder legendTextStyle(DocumentTextStyle s) {
            this.legendTextStyle = s;
            return this;
        }

        /**
         * Sets the value-label text style.
         *
         * @param s text style
         * @return this builder
         */
        public Builder valueLabelTextStyle(DocumentTextStyle s) {
            this.valueLabelTextStyle = s;
            return this;
        }

        /**
         * Sets the halo chip painted behind line-chart value labels. Match it to
         * the chart's surface colour on non-white backgrounds.
         *
         * @param halo halo paint
         * @return this builder
         */
        public Builder valueLabelHalo(DocumentPaint halo) {
            this.valueLabelHalo = halo;
            return this;
        }

        /**
         * Sets the opacity of area fills under {@code line().area(true)} series.
         *
         * @param opacity opacity in (0..1]
         * @return this builder
         */
        public Builder areaOpacity(double opacity) {
            this.areaOpacity = opacity;
            return this;
        }

        /**
         * Sets the pie/donut slice separator stroke.
         *
         * @param stroke separator stroke
         * @return this builder
         */
        public Builder sliceStroke(DocumentStroke stroke) {
            this.sliceStroke = stroke;
            return this;
        }

        /**
         * Sets the angular gap between pie/donut slices.
         *
         * @param degrees pad angle in degrees
         * @return this builder
         */
        public Builder sliceGapDegrees(double degrees) {
            this.sliceGapDegrees = degrees;
            return this;
        }

        /**
         * Sets the donut-centre KPI text style.
         *
         * @param s text style
         * @return this builder
         */
        public Builder donutCenterTextStyle(DocumentTextStyle s) {
            this.donutCenterTextStyle = s;
            return this;
        }

        /**
         * Builds the immutable style.
         *
         * @return chart style
         */
        public ChartStyle build() {
            return new ChartStyle(palette, overrides, lineWidth, barCornerRadius,
                    barWidthRatio, grid, pointMarker, valueLabelOffset,
                    axisTextStyle, legendTextStyle, valueLabelTextStyle, valueLabelHalo,
                    areaOpacity, sliceStroke, sliceGapDegrees, donutCenterTextStyle);
        }
    }
}
