package com.demcha.compose.document.style;

/**
 * Horizontal size constraint for a vertical flow box &mdash; a
 * {@link com.demcha.compose.document.node.SectionNode} or a
 * {@link com.demcha.compose.document.node.ContainerNode}.
 *
 * <p>A flow box is normally measured shrink-to-fit inside the width its parent
 * offers. A <em>fixed</em> flow width pins that horizontal size instead: the box
 * is measured, decorated and placed at exactly {@link #points()}, and its
 * children are laid out inside {@code points - padding.horizontal()}. The
 * vertical axis is untouched &mdash; height stays the natural, content-driven
 * measurement, so a fixed-width box still grows as content is added to it and
 * still paginates the way it did before.</p>
 *
 * <p>The requested width is clamped to the width the parent actually offers
 * (see {@link #resolve(double)}), so asking for more than the page can give
 * degrades to the available width rather than overflowing the region or failing
 * the compiler's width guard.</p>
 *
 * <p>{@link #natural()} is the neutral value carried by every node that has not
 * opted in, and it resolves to the parent's available width &mdash; i.e. exactly
 * the pre-existing behaviour. Instances are immutable and thread-safe.</p>
 *
 * @param points fixed outer width in points, or {@code 0} for the natural
 *               (unconstrained) width
 * @author Artem Demchyshyn
 * @see com.demcha.compose.document.dsl.AbstractFlowBuilder#fixedWidth(double)
 * @since 2.4.0
 */
public record DocumentFlowWidth(double points) {

    private static final DocumentFlowWidth NATURAL = new DocumentFlowWidth(0.0);

    /**
     * Validates the width &mdash; finite and non-negative, where {@code 0} is the
     * natural (unconstrained) width.
     *
     * @param points fixed outer width in points, or {@code 0} for natural
     */
    public DocumentFlowWidth {
        if (Double.isNaN(points) || Double.isInfinite(points) || points < 0.0) {
            throw new IllegalArgumentException("flow width must be finite and non-negative: " + points);
        }
    }

    /**
     * Returns the natural, content-driven width &mdash; the flow box takes the
     * width its parent offers, exactly as it did before this constraint existed.
     *
     * @return the unconstrained flow width
     */
    public static DocumentFlowWidth natural() {
        return NATURAL;
    }

    /**
     * Returns a fixed flow width.
     *
     * @param points fixed outer width in points; must be finite and greater than zero
     * @return a fixed flow width
     * @throws IllegalArgumentException if {@code points} is NaN, infinite, zero or negative
     */
    public static DocumentFlowWidth of(double points) {
        if (Double.isNaN(points) || Double.isInfinite(points) || points <= 0.0) {
            throw new IllegalArgumentException(
                    "fixed flow width must be finite and greater than zero: " + points);
        }
        return new DocumentFlowWidth(points);
    }

    /**
     * Whether this value pins the horizontal size.
     *
     * @return true when a fixed width was requested, false for {@link #natural()}
     */
    public boolean isFixed() {
        return points > 0.0;
    }

    /**
     * Resolves the effective width to measure and place a flow box at, clamping a
     * fixed request to the width the parent offers.
     *
     * @param availableWidth width the parent offers, in points
     * @return the requested width capped at {@code availableWidth}, or
     *         {@code availableWidth} itself for {@link #natural()}
     */
    public double resolve(double availableWidth) {
        return isFixed() ? Math.min(points, availableWidth) : availableWidth;
    }
}
