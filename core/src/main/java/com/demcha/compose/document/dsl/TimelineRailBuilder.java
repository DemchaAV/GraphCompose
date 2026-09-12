package com.demcha.compose.document.dsl;

import com.demcha.compose.document.style.DocumentStroke;

import java.util.Objects;

/**
 * Configures a timeline's connector rail.
 *
 * <p>Reached through {@link TimelineBuilder#rail(java.util.function.Consumer)}:</p>
 * <pre>{@code
 * timeline.rail(rail -> rail.stroke(DocumentStroke.of(accent, 1.5)));
 * }</pre>
 *
 * <p>{@link TimelineBuilder#connector(com.demcha.compose.document.style.DocumentColor, double)}
 * is the shorthand for exactly this and produces the same rail — there is one rail
 * configuration, not an old one and a new one. Setting the rail both ways throws rather
 * than letting one of them win.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public final class TimelineRailBuilder {

    private DocumentStroke stroke;
    private TimelineRailEnd start;
    private TimelineRailEnd end;

    TimelineRailBuilder() {
    }

    /**
     * Sets the rail's colour and width.
     *
     * @param stroke the rail stroke
     * @return this builder
     * @throws NullPointerException if {@code stroke} is null
     */
    public TimelineRailBuilder stroke(DocumentStroke stroke) {
        this.stroke = Objects.requireNonNull(stroke, "stroke");
        return this;
    }

    /**
     * Sets where the rail begins.
     *
     * <p>Independent of where it ends, and of where it runs: the two ends are chosen one at
     * a time and the x comes from the marker anchor. A rail that is asked for neither end
     * begins at {@link TimelineRailEnd#ENTRY_BOUND}, which is what a timeline already
     * draws.</p>
     *
     * @param start where the rail's first end sits
     * @return this builder
     * @throws NullPointerException if {@code start} is null
     * @since 2.4.0
     */
    public TimelineRailBuilder from(TimelineRailEnd start) {
        this.start = Objects.requireNonNull(start, "start");
        return this;
    }

    /**
     * Sets where the rail ends.
     *
     * <p>The companion of {@link #from(TimelineRailEnd)} and just as independent: a rail
     * may begin on its first marker and still run to the foot of its last entry. Unasked,
     * it ends at {@link TimelineRailEnd#ENTRY_BOUND}.</p>
     *
     * @param end where the rail's last end sits
     * @return this builder
     * @throws NullPointerException if {@code end} is null
     * @since 2.4.0
     */
    public TimelineRailBuilder to(TimelineRailEnd end) {
        this.end = Objects.requireNonNull(end, "end");
        return this;
    }

    /** The stroke this rail was given, or null when the caller set none. */
    DocumentStroke stroke() {
        return stroke;
    }

    /** The start this rail was given, or null when the caller asked for none. */
    TimelineRailEnd start() {
        return start;
    }

    /** The end this rail was given, or null when the caller asked for none. */
    TimelineRailEnd end() {
        return end;
    }
}
