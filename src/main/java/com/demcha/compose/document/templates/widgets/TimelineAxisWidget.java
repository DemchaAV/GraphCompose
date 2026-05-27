package com.demcha.compose.document.templates.widgets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;

import java.util.Objects;

/**
 * Shared timeline-axis widget for template presets.
 *
 * <p>Draws a vertical line broken by a configurable number of markers
 * (circles, squares, or none). Used by the CV Timeline Minimal preset
 * to separate the sidebar from the main column, but the visual is
 * generic enough to live in the shared widget layer — proposals,
 * cover letters, or process / step documents can reuse the same
 * widget by tweaking marker shape, spacing, and stroke colour.</p>
 *
 * <h2>Geometry</h2>
 *
 * <p>The widget renders {@code segmentCount} vertical line segments
 * separated by {@code segmentCount - 1} markers. Total axis height is:
 * </p>
 *
 * <pre>
 *   total = segmentCount * segmentLength + (segmentCount - 1) * markerSize
 * </pre>
 *
 * <p>Use the {@link #render(SectionBuilder, Style, double)} overload
 * if you want to fix the total height and let the widget compute the
 * segment length automatically — handy when the axis must match a
 * sibling column's height.</p>
 *
 * <h2>Cross-page behaviour</h2>
 *
 * <p>The widget itself is a deterministic sequence of lines and
 * markers; it does not coordinate with the layout engine on page
 * boundaries. If a host section is split across pages by the engine,
 * the line / marker sequence is split with it, and a marker that
 * straddles a page break may be clipped. Callers that need an axis
 * that visually restarts on each page should compose the widget
 * inside a flow that controls page breaks explicitly (for example
 * one {@code render(...)} call per logical page).</p>
 */
public final class TimelineAxisWidget {

    /** Marker shape drawn between line segments. */
    public enum Marker {
        /** A circle with the configured stroke + fill. */
        CIRCLE,
        /** A square with the configured stroke + fill. */
        SQUARE,
        /** No marker — line segments join directly. */
        NONE
    }

    private TimelineAxisWidget() {
    }

    /**
     * Renders the timeline axis using the supplied {@link Style}. The
     * total height is implied by {@code segmentCount * segmentLength
     * + (segmentCount - 1) * markerSize}.
     */
    public static void render(SectionBuilder host, Style style) {
        Objects.requireNonNull(host, "host");
        Style safeStyle = style == null ? Style.builder().build() : style;
        drawAxis(host, safeStyle);
    }

    /**
     * Renders the timeline axis with an explicit overall height. The
     * widget keeps the supplied {@code marker}, {@code markerSize}
     * and {@code segmentCount} and adjusts {@code segmentLength} so
     * the rendered axis is exactly {@code totalHeight} tall (subject
     * to non-negative segment lengths — short axes with many markers
     * fall back to zero-length segments).
     *
     * @param host        host section receiving the axis
     * @param style       configured style; only {@code segmentLength}
     *                    is recomputed
     * @param totalHeight target total height of the axis
     */
    public static void render(SectionBuilder host, Style style,
                              double totalHeight) {
        Objects.requireNonNull(host, "host");
        Style safeStyle = style == null ? Style.builder().build() : style;
        int markers = Math.max(0, safeStyle.segmentCount() - 1);
        double markerOverhead = markers * safeStyle.markerSize();
        double segmentLength = Math.max(0.0,
                (totalHeight - markerOverhead) / safeStyle.segmentCount());
        Style adjusted = safeStyle.toBuilder()
                .segmentLength(segmentLength)
                .build();
        drawAxis(host, adjusted);
    }

    private static void drawAxis(SectionBuilder host, Style style) {
        host.spacing(0).padding(style.padding());
        int segments = style.segmentCount();
        double lineLeftOffset = Math.max(0.0,
                (style.markerSize() - style.lineThickness()) / 2.0);
        for (int i = 0; i < segments; i++) {
            host.addLine(line -> line
                    .vertical(style.segmentLength())
                    .color(style.lineColor())
                    .thickness(style.lineThickness())
                    .margin(new DocumentInsets(0, 0, 0, lineLeftOffset)));
            if (i < segments - 1) {
                renderMarker(host, style);
            }
        }
    }

    private static void renderMarker(SectionBuilder host, Style style) {
        DocumentStroke stroke = style.markerStroke() != null
                ? style.markerStroke()
                : (style.lineColor() != null
                        ? DocumentStroke.of(style.lineColor(), 0.8)
                        : null);
        DocumentColor fill = style.markerFillColor() != null
                ? style.markerFillColor()
                : DocumentColor.WHITE;
        switch (style.marker()) {
            case CIRCLE -> host.addCircle(style.markerSize(), circle -> {
                if (stroke != null) {
                    circle.stroke(stroke);
                }
                circle.fillColor(fill);
            });
            case SQUARE -> host.addShape(shape -> {
                shape.name("TimelineAxisMarkerSquare")
                        .size(style.markerSize(), style.markerSize())
                        .fillColor(fill)
                        .margin(DocumentInsets.zero());
                if (stroke != null) {
                    shape.stroke(stroke);
                }
            });
            case NONE -> {
                // No marker — the next line segment starts immediately.
            }
        }
    }

    /**
     * Visual configuration for {@link TimelineAxisWidget}.
     *
     * @param marker            shape drawn between segments
     * @param markerSize        diameter (CIRCLE) or side length (SQUARE)
     * @param markerFillColor   fill colour of the marker; {@code null}
     *                          falls back to {@link DocumentColor#WHITE}
     * @param markerStroke      stroke around the marker; {@code null}
     *                          falls back to {@code lineColor} at 0.8pt
     * @param segmentLength     length of each vertical line segment
     * @param segmentCount      number of segments (at least 1);
     *                          markers between = {@code segmentCount - 1}
     * @param lineColor         colour of every line segment
     * @param lineThickness     thickness of every line segment
     * @param padding           inset applied to the host section
     *                          before any drawing
     */
    public record Style(Marker marker,
                        double markerSize,
                        DocumentColor markerFillColor,
                        DocumentStroke markerStroke,
                        double segmentLength,
                        int segmentCount,
                        DocumentColor lineColor,
                        double lineThickness,
                        DocumentInsets padding) {

        public Style {
            marker = marker == null ? Marker.CIRCLE : marker;
            markerSize = Math.max(0.0, markerSize);
            segmentLength = Math.max(0.0, segmentLength);
            segmentCount = Math.max(1, segmentCount);
            lineThickness = lineThickness <= 0.0 ? 0.75 : lineThickness;
            padding = padding == null ? DocumentInsets.zero() : padding;
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder toBuilder() {
            return new Builder()
                    .marker(marker)
                    .markerSize(markerSize)
                    .markerFillColor(markerFillColor)
                    .markerStroke(markerStroke)
                    .segmentLength(segmentLength)
                    .segmentCount(segmentCount)
                    .lineColor(lineColor)
                    .lineThickness(lineThickness)
                    .padding(padding);
        }

        public static final class Builder {
            private Marker marker = Marker.CIRCLE;
            private double markerSize = 7.0;
            private DocumentColor markerFillColor = DocumentColor.WHITE;
            private DocumentStroke markerStroke;
            private double segmentLength = 150.0;
            private int segmentCount = 4;
            private DocumentColor lineColor;
            private double lineThickness = 0.75;
            private DocumentInsets padding = DocumentInsets.zero();

            private Builder() {
            }

            public Builder marker(Marker value) {
                this.marker = value;
                return this;
            }

            public Builder markerSize(double value) {
                this.markerSize = value;
                return this;
            }

            public Builder markerFillColor(DocumentColor value) {
                this.markerFillColor = value;
                return this;
            }

            public Builder markerStroke(DocumentStroke value) {
                this.markerStroke = value;
                return this;
            }

            public Builder segmentLength(double value) {
                this.segmentLength = value;
                return this;
            }

            public Builder segmentCount(int value) {
                this.segmentCount = value;
                return this;
            }

            public Builder lineColor(DocumentColor value) {
                this.lineColor = value;
                return this;
            }

            public Builder lineThickness(double value) {
                this.lineThickness = value;
                return this;
            }

            public Builder padding(DocumentInsets value) {
                this.padding = value;
                return this;
            }

            public Style build() {
                return new Style(marker, markerSize, markerFillColor,
                        markerStroke, segmentLength, segmentCount,
                        lineColor, lineThickness, padding);
            }
        }
    }
}
