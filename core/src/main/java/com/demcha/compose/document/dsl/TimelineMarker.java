package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.CanvasChild;
import com.demcha.compose.document.node.CanvasLayerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.*;
import com.demcha.compose.font.FontName;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The marker drawn in a timeline rail beside an entry's content — a dot, an
 * outlined circle, a numbered disc, or a square. Built with the static
 * factories and passed to
 * {@link TimelineBuilder#entry(TimelineMarker, java.util.function.Consumer)}.
 *
 * <p>A marker is a declared box and a recipe that draws into it, so a new marker
 * shape is one factory method and the timeline layout never learns what shape it
 * is. {@link #custom(double, double, Consumer)} holds that door open for callers:
 * anything that can be drawn into a column can be a marker, and nothing in
 * {@link TimelineBuilder} needs to know about it.</p>
 *
 * <p>The box is <em>declared</em>, not measured. A marker drawn as three stacked
 * shapes has one box, exactly as one drawn as a single ellipse does — whatever
 * anchors on a marker must not be able to tell how the marker was built.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.7.0
 */
public final class TimelineMarker {

    private final TimelineMarkerBounds bounds;
    // Internal, and a Consumer: the timeline calls it and nothing else does, so its
    // shape can grow later — a marker that wants to know it is the first or the last,
    // say — without any of that reaching the public factories.
    private final Consumer<SectionBuilder> recipe;

    private TimelineMarker(TimelineMarkerBounds bounds, Consumer<SectionBuilder> recipe) {
        this.bounds = bounds;
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
        return new TimelineMarker(TimelineMarkerBounds.square(size), column -> column.addCircle(size, color));
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
        return new TimelineMarker(TimelineMarkerBounds.square(size), column -> column.addCircle(size, ellipse -> {
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
                .fontName(FontName.HELVETICA)
                .decoration(DocumentTextDecoration.BOLD)
                .size(Math.max(6.0, size * 0.5))
                .color(textColor == null ? DocumentColor.WHITE : textColor)
                .build();
        String text = Integer.toString(number);
        return new TimelineMarker(TimelineMarkerBounds.square(size), column -> column.addCircle(size, fill, disc -> disc
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
        return new TimelineMarker(TimelineMarkerBounds.square(size), column -> column.addShape(shape -> shape
                .name("TimelineMarkerSquare")
                .size(size, size)
                .fillColor(fill)
                .margin(DocumentInsets.zero())));
    }

    /**
     * A marker of your own: any content, drawn into the marker column.
     *
     * <p>The box is yours to declare and the timeline takes it at its word — it never
     * measures what you drew. That is what makes a marker of several shapes behave like a
     * marker of one:</p>
     * <pre>{@code
     * TimelineMarker.custom(16, 16, column -> column.addLayerStack(stack -> stack
     *         .back(ring).center(disc).center(pip)));
     * }</pre>
     *
     * @param width  the marker's declared width in points
     * @param height the marker's declared height in points
     * @param recipe draws the marker into its column
     * @return the marker
     * @throws NullPointerException     if {@code recipe} is null
     * @throws IllegalArgumentException if a dimension is not positive and finite
     * @since 2.4.0
     */
    public static TimelineMarker custom(double width, double height, Consumer<SectionBuilder> recipe) {
        Objects.requireNonNull(recipe, "recipe");
        return new TimelineMarker(new TimelineMarkerBounds(width, height), recipe);
    }

    /**
     * The box this marker declares for itself.
     *
     * @return the declared bounds
     */
    TimelineMarkerBounds bounds() {
        return bounds;
    }

    /**
     * This marker as one node: the box it declared, with its recipe drawn inside.
     *
     * <p>The declared box is what the marker <em>is</em>, whatever the recipe measures to.
     * The recipe is handed a canvas of exactly {@code width × height} and draws from its
     * origin: a recipe smaller than the box leaves the rest of it empty, and one larger
     * overflows visibly rather than growing the box. Either way the timeline reserves the
     * declared box, the anchor reports the declared box, and the rail is derived from the
     * declared box — so how a marker is drawn stays invisible to everything around it,
     * which is the reason a marker declares a box instead of being measured.</p>
     *
     * <p>It outranks the axis column too: a box wider than the column it is placed in keeps
     * its width and overflows, rather than being squeezed into the column. The rail is
     * derived from this box, so a clamped box would put the line somewhere neither the
     * marker nor the axis asked for.</p>
     *
     * @return the marker's node, sized to its declared box
     */
    DocumentNode node() {
        SectionBuilder drawn = new SectionBuilder();
        drawn.spacing(0);
        recipe.accept(drawn);
        return new CanvasLayerNode("marker", bounds.width(), bounds.height(),
                List.of(new CanvasChild(drawn.build(), 0, 0)),
                ClipPolicy.OVERFLOW_VISIBLE, DocumentInsets.zero(), DocumentInsets.zero());
    }
}
