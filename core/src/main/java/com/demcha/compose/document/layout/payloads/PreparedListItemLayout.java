package com.demcha.compose.document.layout.payloads;

import java.util.Objects;

/**
 * Prepared layout payload for one item inside a {@code ListNode}.
 * Wraps the item's text alongside the prepared paragraph layout that
 * the list definition uses to split items across pages.
 *
 * @param text            raw item text
 * @param paragraphLayout prepared paragraph layout for this item
 * @param geometry        resolved marker/content geometry under
 *                        {@link com.demcha.compose.document.layout.ListItemLayout#MARKER_CONTENT},
 *                        or {@code null} under the legacy prefix layout, where
 *                        the marker is already inside {@code text}
 * @param startsItem      whether this layout is the <em>beginning</em> of the
 *                        item the author wrote, rather than the remainder of one
 *                        that ran onto another page. It is what decides whether
 *                        a marker is drawn, and it cannot be inferred from a
 *                        fragment index: pagination restarts fragment indices on
 *                        every page, so the continuation of a split item is also
 *                        index 0 and would draw a second marker
 */
public record PreparedListItemLayout(
        String text,
        PreparedParagraphLayout paragraphLayout,
        MarkerContentItem geometry,
        boolean startsItem
) {
    /**
     * Normalizes the item text and validates the paragraph layout is
     * present.
     */
    public PreparedListItemLayout {
        text = text == null ? "" : text;
        paragraphLayout = Objects.requireNonNull(paragraphLayout, "paragraphLayout");
    }

    /**
     * Creates a legacy-layout item: no resolved geometry, and a whole item
     * rather than the tail of a split one.
     *
     * @param text            raw item text
     * @param paragraphLayout prepared paragraph layout for this item
     */
    public PreparedListItemLayout(String text, PreparedParagraphLayout paragraphLayout) {
        this(text, paragraphLayout, null, true);
    }

    /**
     * Returns whether this item draws a marker: it has resolved geometry with a
     * visible marker, and it is the start of the authored item rather than a
     * continuation of one.
     *
     * @return whether a marker belongs on this slice
     */
    public boolean drawsMarker() {
        return geometry != null && startsItem && geometry.spec().hasMarker();
    }

    /**
     * Returns this item as the remainder of a split, which never repeats the
     * marker its head already drew.
     *
     * @param slicedLayout the paragraph layout of the remaining lines
     * @param slicedText   the remaining text
     * @return a continuation item
     */
    public PreparedListItemLayout continuedAs(String slicedText, PreparedParagraphLayout slicedLayout) {
        return new PreparedListItemLayout(slicedText, slicedLayout, geometry, false);
    }

    /**
     * Returns the head of a split, which starts the item exactly when this one
     * did.
     *
     * @param slicedLayout the paragraph layout of the leading lines
     * @param slicedText   the leading text
     * @return a head item
     */
    public PreparedListItemLayout startingAs(String slicedText, PreparedParagraphLayout slicedLayout) {
        return new PreparedListItemLayout(slicedText, slicedLayout, geometry, startsItem);
    }
}
