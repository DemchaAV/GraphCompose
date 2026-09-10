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
    private TimelineRailExtent extent;

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
     * Sets how far the rail runs.
     *
     * <p>Independent of where it runs: an extent is the line's two ends, and its x comes
     * from the marker anchor. A timeline that sets neither keeps
     * {@link TimelineRailExtent#ENTRY_BOUNDS}, which is what it already draws.</p>
     *
     * @param extent the rail's extent
     * @return this builder
     * @throws NullPointerException if {@code extent} is null
     * @since 2.4.0
     */
    public TimelineRailBuilder extent(TimelineRailExtent extent) {
        this.extent = Objects.requireNonNull(extent, "extent");
        return this;
    }

    /** The stroke this rail was given, or null when the caller set none. */
    DocumentStroke stroke() {
        return stroke;
    }

    /** The extent this rail was given, or null when the caller set none. */
    TimelineRailExtent extent() {
        return extent;
    }
}
