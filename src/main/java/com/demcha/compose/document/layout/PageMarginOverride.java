package com.demcha.compose.document.layout;

import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.engine.components.style.Margin;

import java.util.Objects;

/**
 * Engine-side per-page-range margin override: the margin to apply to a contiguous
 * run of pages, addressed by 0-based page index. The canonical
 * {@code PageMarginRule} (1-based, public API) is translated into this internal
 * form via {@link #fromInsets}, which converts the canonical insets to an engine
 * margin at this layout boundary so {@code DocumentSession} never references an
 * engine type.
 *
 * @param fromPageIndex        first page index the override covers (0-based, inclusive)
 * @param toPageIndexExclusive one past the last page index covered (exclusive)
 * @param margin               the margin to apply across the covered pages
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public record PageMarginOverride(int fromPageIndex, int toPageIndexExclusive, Margin margin) {

    /**
     * Validates the index range and margin.
     */
    public PageMarginOverride {
        Objects.requireNonNull(margin, "margin");
        if (fromPageIndex < 0) {
            throw new IllegalArgumentException("fromPageIndex must be >= 0 but was " + fromPageIndex);
        }
        if (toPageIndexExclusive <= fromPageIndex) {
            throw new IllegalArgumentException(
                    "toPageIndexExclusive (" + toPageIndexExclusive + ") must be greater than fromPageIndex ("
                    + fromPageIndex + ")");
        }
    }

    /**
     * Builds an override from canonical page insets, converting them to an engine
     * {@link Margin} at this layout boundary.
     *
     * @param fromPageIndex        first page index the override covers (0-based, inclusive)
     * @param toPageIndexExclusive one past the last page index covered (exclusive)
     * @param insets               canonical page insets to apply across the covered pages
     * @return the engine-side override
     * @since 2.0.0
     */
    public static PageMarginOverride fromInsets(int fromPageIndex, int toPageIndexExclusive, DocumentInsets insets) {
        return new PageMarginOverride(fromPageIndex, toPageIndexExclusive, LayoutInsets.toMargin(insets));
    }

    /**
     * Whether this override covers the given page.
     *
     * @param pageIndex a 0-based page index
     * @return {@code true} when {@code fromPageIndex <= pageIndex < toPageIndexExclusive}
     */
    public boolean coversPage(int pageIndex) {
        return pageIndex >= fromPageIndex && pageIndex < toPageIndexExclusive;
    }
}
