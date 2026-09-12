package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.ListNode;

/**
 * How one {@link ListNode}'s items are laid out — the single internal strategy
 * that the public {@code hangingIndent} flag normalizes into.
 *
 * <p>The point of naming the strategy is that the decision is made <b>once</b>,
 * in {@link TextFlowSupport#prepareList}, instead of being re-read as a boolean
 * at every step of measure, split and emit. Preparation then branches on the
 * strategy and each branch owns its own geometry end to end.</p>
 *
 * <p>This is a layout concept and stays inside the {@code @Internal}
 * {@code document.layout} package: the public authoring surface is
 * {@code ListBuilder.hangingIndent(boolean)} and
 * {@code ListBuilder.markerGap(double)}, and nothing outside the compiler needs
 * to name the strategy.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public enum ListItemLayout {

    /**
     * The v1.4-through-2.3 behaviour, unchanged. The marker is a text prefix on
     * the item's first visual line, wrapped lines are indented with a run of
     * ASCII spaces wide enough to clear it, and a nested list is flattened into
     * a flat one with the depth indent and the resolved marker baked into each
     * label. There is no marker geometry: the marker is content.
     *
     * <p>{@code markerGap} is not observed in this mode — the gap is whatever
     * the marker's own trailing separator measures.</p>
     */
    LEGACY_PREFIX,

    /**
     * Opt-in marker/content geometry. Depth, marker and content stay separate
     * all the way through preparation instead of being concatenated into one
     * string, so the marker can be measured on its own and every visual line of
     * an item can share one content origin.
     *
     * <p>{@code markerGap} is real geometry in this mode, in points.</p>
     */
    MARKER_CONTENT;

    /**
     * Resolves the strategy for a list. This is the one place the public flag
     * turns into an internal decision.
     *
     * @param node list node carrying the authored intent
     * @return the strategy its items are prepared with
     */
    public static ListItemLayout of(ListNode node) {
        return node != null && node.hangingIndent() ? MARKER_CONTENT : LEGACY_PREFIX;
    }
}
