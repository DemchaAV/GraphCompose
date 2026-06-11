package com.demcha.compose.document.layout.payloads;

import java.util.Objects;

/**
 * Prepared layout payload for one item inside a {@code ListNode}.
 * Wraps the item's text alongside the prepared paragraph layout that
 * the list definition uses to split items across pages.
 *
 * @param text            raw item text
 * @param paragraphLayout prepared paragraph layout for this item
 */
public record PreparedListItemLayout(
        String text,
        PreparedParagraphLayout paragraphLayout
) {
    /**
     * Normalizes the item text and validates the paragraph layout is
     * present.
     */
    public PreparedListItemLayout {
        text = text == null ? "" : text;
        paragraphLayout = Objects.requireNonNull(paragraphLayout, "paragraphLayout");
    }
}
