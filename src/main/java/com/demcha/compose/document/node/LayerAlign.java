package com.demcha.compose.document.node;

/**
 * Alignment of a single layer inside a {@link LayerStackNode}.
 *
 * <p>The stack's bounding box is sized to the largest layer (or the available
 * page width). Smaller layers are positioned inside the stack box according to
 * the alignment they are paired with.</p>
 *
 * @author Artem Demchyshyn
 */
public enum LayerAlign {
    /** Anchored to the top-left corner of the stack box. */
    TOP_LEFT,
    /** Centered horizontally and anchored to the top edge. */
    TOP_CENTER,
    /** Anchored to the top-right corner of the stack box. */
    TOP_RIGHT,
    /** Centered vertically and anchored to the left edge. */
    CENTER_LEFT,
    /** Centered both horizontally and vertically. */
    CENTER,
    /** Centered vertically and anchored to the right edge. */
    CENTER_RIGHT,
    /** Anchored to the bottom-left corner of the stack box. */
    BOTTOM_LEFT,
    /** Centered horizontally and anchored to the bottom edge. */
    BOTTOM_CENTER,
    /** Anchored to the bottom-right corner of the stack box. */
    BOTTOM_RIGHT
}
