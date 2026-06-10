package com.demcha.compose.document.chart;

import java.util.List;

/**
 * The pure geometry engine: {@code (spec, style, theme, w, h, metrics) →
 * List<ChartPrimitive>}. Deterministic by contract — no randomness, no system
 * fonts read outside {@code metrics}, no I/O, no hidden state — so identical
 * inputs yield identical positioned primitives and therefore byte-identical
 * output, exactly like the rest of the engine.
 *
 * <p>This facade dispatches each sealed {@link ChartSpec} kind to its layout
 * class ({@link BarChartLayout}, {@link LineChartLayout}, {@link PieChartLayout});
 * shared frame/legend/label geometry lives in {@link ChartLayoutSupport}.
 * Everything emitted is an existing primitive node wrapped in a
 * {@link ChartPrimitive}; the bars, lines, sectors, markers, and labels then
 * flow through the normal layout + render path via {@code ChartDefinition}.</p>
 *
 * <p>All coordinates are <b>bottom-up</b> in the chart's inner content box:
 * {@code (0,0)} is bottom-left, {@code y} grows up. See {@link ChartPrimitive}.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public final class ChartLayoutResolver {

    private ChartLayoutResolver() {
    }

    /**
     * Resolves a chart spec into positioned primitive children.
     *
     * @param spec what to plot
     * @param style fully-coalesced style (theme already merged in)
     * @param theme active chart theme (supplies the fallback palette)
     * @param width chart inner box width in points
     * @param height chart inner box height in points
     * @param metrics text measurement seam for label sizing
     * @return positioned primitive nodes filling the chart box; never null
     */
    public static List<ChartPrimitive> resolve(ChartSpec spec,
                                               ChartStyle style,
                                               ChartTheme theme,
                                               double width,
                                               double height,
                                               ChartTextMetrics metrics) {
        // Dispatch on the sealed ChartSpec. A new kind adds a branch here; the
        // final throw guards against an unhandled permitted subtype.
        if (spec instanceof ChartSpec.Bar bar) {
            return BarChartLayout.resolve(bar, style, theme, width, height, metrics);
        }
        if (spec instanceof ChartSpec.Line line) {
            return LineChartLayout.resolve(line, style, theme, width, height, metrics);
        }
        if (spec instanceof ChartSpec.Pie pie) {
            return PieChartLayout.resolve(pie, style, theme, width, height, metrics);
        }
        throw new IllegalStateException("unsupported chart spec: " + spec.getClass().getName());
    }
}
