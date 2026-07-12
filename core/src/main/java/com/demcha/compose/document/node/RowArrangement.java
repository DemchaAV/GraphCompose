package com.demcha.compose.document.node;

/**
 * Main-axis (horizontal) distribution of a row's leftover width when its children
 * do not fill it — the {@code justify-content} analogue for a horizontal row.
 *
 * <p>Only meaningful when the children are content-sized (no weights, columns, or
 * grow spacer absorbs the slack). {@link #START} is the default and leaves the
 * children flush left, exactly as before.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public enum RowArrangement {
    /** Children packed at the start (the default). */
    START,
    /** Children packed and centred in the row. */
    CENTER,
    /** Children packed at the end. */
    END,
    /** Leftover split evenly between children, none at the edges. */
    SPACE_BETWEEN,
    /** Leftover split around each child (half-size space at the edges). */
    SPACE_AROUND,
    /** Leftover split into equal gaps, including at the edges. */
    SPACE_EVENLY
}
