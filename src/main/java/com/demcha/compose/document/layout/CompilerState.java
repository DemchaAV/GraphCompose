package com.demcha.compose.document.layout;

import com.demcha.compose.engine.components.style.Margin;

/**
 * Mutable bookkeeping for the page-flow path of {@link LayoutCompiler}: the
 * canvas the document is being placed on, the active page index, the height
 * already consumed on that page, and the highest page touched so far.
 *
 * <p>Lifted to a sibling class (Phase E.4) so {@link MutatingPlacementContext}
 * can hold and mutate the same state object that {@code LayoutCompiler} reads
 * from. Stays package-private — callers outside {@code document.layout} should
 * use {@link PlacementContext} for placement-strategy access.</p>
 *
 * <p>An optional {@link PageGeometry} lets the active margin / inner width /
 * inner height vary by page index. When it is {@code null} every accessor falls
 * back to the single canvas geometry, so a document with no per-page margins is
 * laid out exactly as before.</p>
 */
final class CompilerState {
    final LayoutCanvas canvas;
    private final PageGeometry geometry;
    int pageIndex;
    double usedHeight;
    int maxTouchedPage = -1;

    CompilerState(LayoutCanvas canvas) {
        this(canvas, null);
    }

    CompilerState(LayoutCanvas canvas, PageGeometry geometry) {
        this.canvas = canvas;
        this.geometry = geometry;
    }

    /** Whether per-page geometry is active (a document with per-page margins). */
    boolean hasPageGeometry() {
        return geometry != null;
    }

    /** The margin of the page currently being placed on. */
    Margin activeMargin() {
        return geometry == null ? canvas.margin() : geometry.marginForPage(pageIndex);
    }

    /** The content height of the page currently being placed on. */
    double activeInnerHeight() {
        return geometry == null ? canvas.innerHeight() : geometry.innerHeightForPage(pageIndex);
    }

    /** The content height of a specific page (for fresh-page capacity checks). */
    double innerHeightForPage(int page) {
        return geometry == null ? canvas.innerHeight() : geometry.innerHeightForPage(page);
    }

    /** The content width of a specific page (for the per-page region width). */
    double innerWidthForPage(int page) {
        return geometry == null ? canvas.innerWidth() : geometry.innerWidthForPage(page);
    }

    /** The left margin of a specific page (for the per-page region x-origin). */
    double marginLeftForPage(int page) {
        return geometry == null ? canvas.margin().left() : geometry.marginForPage(page).left();
    }

    double remainingHeight() {
        return Math.max(0.0, activeInnerHeight() - usedHeight);
    }

    double pageTop() {
        return canvas.height() - activeMargin().top();
    }

    void newPage() {
        pageIndex++;
        usedHeight = 0.0;
        touchPage();
    }

    void touchPage() {
        maxTouchedPage = Math.max(maxTouchedPage, pageIndex);
    }
}
