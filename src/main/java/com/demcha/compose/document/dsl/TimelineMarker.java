package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.*;
import com.demcha.compose.font.FontName;

import java.util.function.Consumer;

/**
 * The marker drawn in a timeline rail beside an entry's content — a dot, an
 * outlined circle, a numbered disc, or a square. Built with the static
 * factories and passed to
 * {@link TimelineBuilder#entry(TimelineMarker, java.util.function.Consumer)}.
 *
 * <p>A marker carries its own {@link #size()} (used to lay out the rail column)
 * and a recipe that draws it into that column, so new marker shapes are one
 * factory method without touching the timeline layout.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.7.0
 */
public final class TimelineMarker {

    private final double size;
    private final Consumer<SectionBuilder> recipe;

    private TimelineMarker(double size, Consumer<SectionBuilder> recipe) {
        this.size = size;
        this.recipe = recipe;
    }

    /**
     * A solid filled dot.
     *
     * @param size  diameter in points
     * @param color fill colour
     * @return the marker
     */
    public static TimelineMarker dot(double size, DocumentColor color) {
        return new TimelineMarker(size, column -> column.addCircle(size, color));
    }

    /**
     * An outlined circle (filled and/or stroked).
     *
     * @param size   diameter in points
     * @param fill   fill colour, or {@code null} for an unfilled ring
     * @param stroke outline stroke, or {@code null} for no outline
     * @return the marker
     */
    public static TimelineMarker circle(double size, DocumentColor fill, DocumentStroke stroke) {
        return new TimelineMarker(size, column -> column.addCircle(size, ellipse -> {
            if (fill != null) {
                ellipse.fillColor(fill);
            }
            if (stroke != null) {
                ellipse.stroke(stroke);
            }
        }));
    }

    /**
     * A numbered disc — a filled circle with a centred number.
     *
     * @param number    the step number to centre in the disc
     * @param size      diameter in points
     * @param fill      disc fill colour
     * @param textColor number colour; {@code null} falls back to white
     * @return the marker
     */
    public static TimelineMarker numbered(int number, double size,
                                          DocumentColor fill, DocumentColor textColor) {
        DocumentTextStyle label = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .decoration(DocumentTextDecoration.BOLD)
                .size(Math.max(6.0, size * 0.5))
                .color(textColor == null ? DocumentColor.WHITE : textColor)
                .build();
        String text = Integer.toString(number);
        return new TimelineMarker(size, column -> column.addCircle(size, fill, disc -> disc
                .center(new ParagraphBuilder()
                        .text(text)
                        .textStyle(label)
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero())
                        .build())));
    }

    /**
     * A filled square marker.
     *
     * @param size side length in points
     * @param fill fill colour
     * @return the marker
     */
    public static TimelineMarker square(double size, DocumentColor fill) {
        return new TimelineMarker(size, column -> column.addShape(shape -> shape
                .name("TimelineMarkerSquare")
                .size(size, size)
                .fillColor(fill)
                .margin(DocumentInsets.zero())));
    }

    double size() {
        return size;
    }

    void renderInto(SectionBuilder column) {
        recipe.accept(column);
    }
}
