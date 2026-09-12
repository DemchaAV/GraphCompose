package com.demcha.compose.document.layout;

import com.demcha.compose.engine.components.style.Margin;

import java.util.IdentityHashMap;
import java.util.List;

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

    /**
     * Column bands published by rows during this compilation, by owner identity.
     *
     * <p>Local to one compile — a fresh state is built for every pass — so nothing survives
     * into another document, and identity is the key because two features that describe
     * their columns alike are still two features.</p>
     */
    private final IdentityHashMap<Object, List<ResolvedHorizontalBand>> bands = new IdentityHashMap<>();

    /** The key the next row to be compiled publishes under, set by the wrapper around it. */
    private Object expectedBandKey;

    CompilerState(LayoutCanvas canvas) {
        this(canvas, null);
    }

    CompilerState(LayoutCanvas canvas, PageGeometry geometry) {
        this.canvas = canvas;
        this.geometry = geometry;
    }

    /**
     * Announces that the row about to be compiled publishes its columns under this key.
     *
     * @param key the owner identity, from the wrapper
     */
    void expectBands(Object key) {
        this.expectedBandKey = key;
    }

    /**
     * The key set by a wrapper, cleared as it is read.
     *
     * @return the key, or null when this row is not published
     */
    Object takeExpectedBandKey() {
        Object key = expectedBandKey;
        expectedBandKey = null;
        return key;
    }

    /**
     * Records the columns a row resolved.
     *
     * @param key   the owner identity
     * @param slots the resolved columns, in order
     * @throws IllegalStateException if this key already published
     */
    void publishBands(Object key, List<ResolvedHorizontalBand> slots) {
        if (bands.containsKey(key)) {
            throw new IllegalStateException(
                    "Two rows publish their columns under one identity. A band identity names one row; "
                    + "give the second row an identity of its own.");
        }
        bands.put(key, List.copyOf(slots));
    }

    /**
     * The band a consumer asks for, or a refusal saying which part of the arrangement is wrong.
     *
     * @param key  the owner identity the consumer names
     * @param slot the column index
     * @param path the consumer's layout path, for the message
     * @return the resolved band
     * @throws IllegalStateException if nothing published, the slot does not exist, or it is empty
     */
    ResolvedHorizontalBand band(Object key, int slot, String path) {
        List<ResolvedHorizontalBand> published = bands.get(key);
        if (published == null) {
            throw new IllegalStateException("Node '" + path + "' lays out inside a column that was never "
                                            + "published. The row it names has to be laid out before it — an "
                                            + "earlier sibling, not a later one and not a parent.");
        }
        if (slot >= published.size()) {
            throw new IllegalStateException("Node '" + path + "' asks for column " + slot
                                            + " of a row that resolved " + published.size()
                                            + ". Columns are counted from zero.");
        }
        ResolvedHorizontalBand band = published.get(slot);
        if (band.width() <= EPS) {
            throw new IllegalStateException("Node '" + path + "' lays out inside column " + slot
                                            + ", which resolved to " + band.width()
                                            + "pt. There is nothing to lay out in — widen the column or "
                                            + "reduce what the row has to fit.");
        }
        return band;
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
