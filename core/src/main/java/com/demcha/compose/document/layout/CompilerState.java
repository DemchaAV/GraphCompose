package com.demcha.compose.document.layout;

import com.demcha.compose.engine.components.style.Margin;

import static com.demcha.compose.document.layout.NodeDefinitionSupport.EPS;

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

    /**
     * A second cursor over the same canvas, starting exactly where this one
     * is. Used by a multi-column flow so each column advances independently:
     * they all begin at the flow's entry position and are rejoined afterwards
     * via {@link #rejoinAt(int, double, int)}.
     *
     * <p>The geometry is shared, not copied — per-page margins must resolve
     * identically in every column, or the layout fixed point would not
     * converge.</p>
     */
    CompilerState forkAtCurrentPosition() {
        CompilerState fork = new CompilerState(canvas, geometry);
        fork.pageIndex = pageIndex;
        fork.usedHeight = usedHeight;
        fork.maxTouchedPage = maxTouchedPage;
        return fork;
    }

    /**
     * Moves this cursor to where the longest of several forked cursors ended.
     *
     * <p>{@code usedHeight} is meaningful only together with the page it was
     * measured on, which is why the caller passes both: a column that finished
     * two pages earlier says nothing about how much of the final page is
     * occupied.</p>
     *
     * @param page        the page the flow ends on
     * @param used        the height consumed on that page
     * @param touchedPage the highest page any fork reached
     */
    void rejoinAt(int page, double used, int touchedPage) {
        pageIndex = page;
        usedHeight = Math.min(activeInnerHeight(), Math.max(0.0, used));
        maxTouchedPage = Math.max(maxTouchedPage, Math.max(touchedPage, page));
    }

    void newPage() {
        pageIndex++;
        usedHeight = 0.0;
        touchPage();
    }

    void touchPage() {
        maxTouchedPage = Math.max(maxTouchedPage, pageIndex);
    }

    /**
     * Advances the flow by {@code amount}, spilling to a fresh page first when the
     * amount does not fit in the remaining height (and the page has already been
     * used). A non-positive amount is dropped; the used height never exceeds the
     * active page's content height.
     */
    void advanceSpace(double amount) {
        if (amount <= EPS) {
            return;
        }
        if (amount > remainingHeight() + EPS && usedHeight > EPS) {
            newPage();
        }
        touchPage();
        usedHeight = Math.min(activeInnerHeight(), usedHeight + amount);
    }

    /**
     * Closes out a composite's bottom edge. A positive bottom inset advances the
     * flow as usual; a NEGATIVE one (a negative bottom margin) pulls the following
     * sibling up — symmetric with a negative top margin, which already offsets via
     * {@code placementTopY}. The plain {@link #advanceSpace} drops a non-positive
     * amount, so the closing edge needs this dedicated path. The top-of-node
     * reservation deliberately stays on {@link #advanceSpace} so a negative top
     * margin keeps its existing flow behaviour; only the closing edge gains the
     * pull-up. The cursor never drops below the page top.
     */
    void closeBottomSpace(double amount) {
        if (amount >= EPS) {
            advanceSpace(amount);
        } else if (amount <= -EPS) {
            touchPage();
            usedHeight = Math.max(0.0, usedHeight + amount);
        }
    }
}
