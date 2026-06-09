package com.demcha.compose.document.chart;

import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.List;
import java.util.Objects;

/**
 * Chart-specific design tokens, the TOP tier of the style cascade
 * ({@code theme → ChartStyle → per-series}). A built-in default lives in
 * {@link ChartDefaults#DEFAULT_THEME}; a future {@code BusinessTheme} integration
 * can supply a document-matched instance at authoring time so a chart authored
 * with no {@link ChartStyle} still matches the document's palette and typography
 * automatically.
 *
 * @param palette ordered series paints; cycled by the resolver
 * @param gridStroke default grid line stroke
 * @param axisTextStyle default tick / category label style
 * @param legendTextStyle default legend label style
 * @param valueLabelTextStyle default value-label style
 * @param valueLabelHalo default halo chip painted behind line-chart value labels
 *                       so digits stay legible where line strokes cross them
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public record ChartTheme(
        List<DocumentPaint> palette,
        DocumentStroke gridStroke,
        DocumentTextStyle axisTextStyle,
        DocumentTextStyle legendTextStyle,
        DocumentTextStyle valueLabelTextStyle,
        DocumentPaint valueLabelHalo
) {
    /** Copy-protects and validates the palette. */
    public ChartTheme {
        Objects.requireNonNull(palette, "palette");
        palette = List.copyOf(palette);
        if (palette.isEmpty()) {
            throw new IllegalArgumentException("chart theme palette must not be empty");
        }
        Objects.requireNonNull(gridStroke, "gridStroke");
        Objects.requireNonNull(axisTextStyle, "axisTextStyle");
        Objects.requireNonNull(legendTextStyle, "legendTextStyle");
        Objects.requireNonNull(valueLabelTextStyle, "valueLabelTextStyle");
        Objects.requireNonNull(valueLabelHalo, "valueLabelHalo");
    }

    /**
     * Projects these tokens into a fully-populated {@link ChartStyle} that the
     * resolver treats as the cascade base. Document-level
     * {@link ChartStyle#mergedUnder(ChartStyle)} then layers author overrides on
     * top.
     *
     * @return a style carrying every theme default
     */
    public ChartStyle toChartStyle() {
        return ChartStyle.builder()
                .palette(palette.toArray(new DocumentPaint[0]))
                .grid(ChartStyle.GridStyle.horizontal(gridStroke))
                .axisTextStyle(axisTextStyle)
                .legendTextStyle(legendTextStyle)
                .valueLabelTextStyle(valueLabelTextStyle)
                .valueLabelHalo(valueLabelHalo)
                .build();
    }
}
