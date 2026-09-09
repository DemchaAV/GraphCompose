package com.demcha.compose.document.dsl;

/**
 * How wide the axis column — the one the markers sit in — is.
 *
 * <p>Two strategies, and deliberately not one number. A weight is a share of what the row
 * has left; a fixed width is points. Converting one into the other would need the row's
 * width, which the builder does not have, and doing it anyway is how a timeline that has
 * rendered the same way for versions quietly moves: {@code markerColumnWeight(0.10)} means
 * "a tenth of the remainder" on a 260pt page and on a 500pt one, and no single point value
 * is both.</p>
 *
 * <p>So both survive to the layout, which asks the row to resolve whichever it was given.
 * A closed set of two rather than a reused {@code DocumentRowColumn} because the third
 * strategy that type offers — auto — cannot align a column across rows, which is the one
 * thing this column has to do.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
sealed interface TimelineAxisSize {

    /**
     * An axis of exactly this many points, the same on any page width.
     *
     * @param points width in points
     */
    record Fixed(double points) implements TimelineAxisSize {
    }

    /**
     * An axis taking this share of the row's remaining space, against a content weight
     * of 1.0.
     *
     * @param weight relative share
     */
    record Weight(double weight) implements TimelineAxisSize {
    }
}
