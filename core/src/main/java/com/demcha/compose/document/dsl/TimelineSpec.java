package com.demcha.compose.document.dsl;

import java.util.List;

/**
 * A whole timeline, normalized: everything the layout needs and nothing about how it was
 * authored.
 *
 * <p>{@link TimelineBuilder} resolves its defaults, its per-entry style overrides and its
 * content callbacks into one of these, and the layout reads only this. That separation is
 * the point — a second authoring API can produce the same spec, and the layout will not be
 * able to tell which one it came from.</p>
 *
 * @param rail                the connector rail
 * @param gutter              space between the rail and the entry's content
 * @param markerGap           horizontal gap between the marker column and the content
 *                            beside it
 * @param markerColumnWeight  the marker column's weight against a content weight of 1.0
 * @param entrySpacing        vertical space between entries; the rail spans it
 * @param keepTogether        whether the timeline relocates whole rather than splitting
 * @param keepEntriesTogether whether each entry relocates whole rather than splitting
 * @param entries             the entries, in document order
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
record TimelineSpec(TimelineRailSpec rail,
                    double gutter,
                    double markerGap,
                    double markerColumnWeight,
                    double entrySpacing,
                    boolean keepTogether,
                    boolean keepEntriesTogether,
                    List<TimelineEntrySpec> entries) {
}
