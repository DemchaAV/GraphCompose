package com.demcha.compose.document.table;

/**
 * Public table-cell text placement anchor.
 *
 * <p>Most documents can ignore this value and use the default left-aligned
 * cell text. Templates and advanced tables use it when a column needs centered
 * or right-aligned content.</p>
 *
 * @author Artem Demchyshyn
 */
public enum DocumentTableTextAnchor {
    /**
     * Use the renderer default.
     */
    DEFAULT,

    /**
     * Place text at the left side of the vertical middle.
     */
    CENTER_LEFT,

    /**
     * Place text in the visual center of the cell.
     */
    CENTER,

    /**
     * Place text at the right side of the vertical middle.
     */
    CENTER_RIGHT,

    /**
     * Place text at the top-left corner.
     */
    TOP_LEFT,

    /**
     * Place text at the top-right corner.
     */
    TOP_RIGHT,

    /**
     * Place text at the bottom-left corner.
     */
    BOTTOM_LEFT,

    /**
     * Place text at the bottom-right corner.
     */
    BOTTOM_RIGHT
}
