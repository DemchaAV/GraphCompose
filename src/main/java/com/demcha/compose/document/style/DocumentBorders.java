package com.demcha.compose.document.style;

/**
 * Public per-side border value for sections, flows, rows, and shapes.
 *
 * <p>Each side carries an optional {@link DocumentStroke}. A {@code null}
 * stroke means no border on that side. The canonical authoring API uses this
 * value so application code can describe asymmetric outlines (for example, a
 * bottom-only divider under a row) without importing internal engine shape
 * components.</p>
 *
 * @param top stroke applied to the top side, or {@code null} for none
 * @param right stroke applied to the right side, or {@code null} for none
 * @param bottom stroke applied to the bottom side, or {@code null} for none
 * @param left stroke applied to the left side, or {@code null} for none
 *
 * @author Artem Demchyshyn
 */
public record DocumentBorders(DocumentStroke top, DocumentStroke right, DocumentStroke bottom, DocumentStroke left) {
    /** All-sides empty borders. */
    public static final DocumentBorders NONE = new DocumentBorders(null, null, null, null);

    /**
     * Returns borders that draw the same stroke on every side.
     *
     * @param stroke stroke applied to every side
     * @return uniform borders
     */
    public static DocumentBorders all(DocumentStroke stroke) {
        return new DocumentBorders(stroke, stroke, stroke, stroke);
    }

    /**
     * Returns borders that only draw the supplied stroke on the bottom side.
     *
     * @param stroke bottom-side stroke
     * @return borders with the bottom side filled in
     */
    public static DocumentBorders bottom(DocumentStroke stroke) {
        return new DocumentBorders(null, null, stroke, null);
    }

    /**
     * Returns borders that only draw the supplied stroke on the top side.
     *
     * @param stroke top-side stroke
     * @return borders with the top side filled in
     */
    public static DocumentBorders top(DocumentStroke stroke) {
        return new DocumentBorders(stroke, null, null, null);
    }

    /**
     * Returns borders that only draw the supplied stroke on the left side.
     *
     * @param stroke left-side stroke
     * @return borders with the left side filled in
     */
    public static DocumentBorders left(DocumentStroke stroke) {
        return new DocumentBorders(null, null, null, stroke);
    }

    /**
     * Returns borders that only draw the supplied stroke on the right side.
     *
     * @param stroke right-side stroke
     * @return borders with the right side filled in
     */
    public static DocumentBorders right(DocumentStroke stroke) {
        return new DocumentBorders(null, stroke, null, null);
    }

    /**
     * Returns horizontal borders (top + bottom) with the same stroke and no
     * vertical borders.
     *
     * @param stroke stroke applied to top and bottom
     * @return borders with horizontal sides filled in
     */
    public static DocumentBorders horizontal(DocumentStroke stroke) {
        return new DocumentBorders(stroke, null, stroke, null);
    }

    /**
     * Returns vertical borders (left + right) with the same stroke and no
     * horizontal borders.
     *
     * @param stroke stroke applied to left and right
     * @return borders with vertical sides filled in
     */
    public static DocumentBorders vertical(DocumentStroke stroke) {
        return new DocumentBorders(null, stroke, null, stroke);
    }

    /**
     * Returns {@code true} when at least one side is configured.
     *
     * @return {@code true} when any side carries a stroke
     */
    public boolean hasAny() {
        return top != null || right != null || bottom != null || left != null;
    }
}
