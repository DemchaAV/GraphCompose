package com.demcha.compose.document.node;

import com.demcha.compose.document.chart.ChartSpec;
import com.demcha.compose.document.chart.ChartStyle;
import com.demcha.compose.document.style.DocumentInsets;

import java.util.Objects;

/**
 * Semantic chart node. Carries only the orthogonal layers — what to plot
 * ({@link ChartSpec}, which embeds the {@link com.demcha.compose.document.chart.ChartData}),
 * and how it looks ({@link ChartStyle}, nullable = fully themed). It knows
 * nothing about rendering: the layout pass hands it to {@code ChartDefinition},
 * which runs the geometry resolver and rewrites the chart into existing
 * primitive nodes. No fixed-layout render handler ever sees a chart — that is
 * what keeps charts deterministic and snapshot-testable for free, and what
 * gives every fixed-layout backend (PDF today, others later) vector charts
 * with no chart-specific code. Semantic exports have no layout pass, so they
 * fall back to the chart's data: the DOCX backend writes a
 * categories-by-series table.
 *
 * <p>A chart is {@link com.demcha.compose.document.layout.PaginationPolicy#ATOMIC}:
 * it places whole on a page or moves to the next, exactly like an inline image
 * or a shape container.</p>
 *
 * @param name optional semantic name for snapshots / diagnostics
 * @param spec structural chart description (kind, axes, legend, data)
 * @param style visual styling, or {@code null} to inherit the theme entirely
 * @param margin outer spacing
 * @param padding inner spacing inside the chart box (insets the plot area)
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public record ChartNode(
        String name,
        ChartSpec spec,
        ChartStyle style,
        DocumentInsets margin,
        DocumentInsets padding
) implements DocumentNode {

    /** Normalizes nullable spacing and name. */
    public ChartNode {
        name = name == null ? "" : name;
        Objects.requireNonNull(spec, "spec");
        margin = margin == null ? DocumentInsets.zero() : margin;
        padding = padding == null ? DocumentInsets.zero() : padding;
    }

    /**
     * Themed chart, no explicit style, no spacing.
     *
     * @param spec structural chart description
     */
    public ChartNode(ChartSpec spec) {
        this("", spec, null, DocumentInsets.zero(), DocumentInsets.zero());
    }

    @Override
    public String nodeKind() {
        return "Chart";
    }
}
