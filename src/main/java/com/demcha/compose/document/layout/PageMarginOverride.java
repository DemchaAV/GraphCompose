package com.demcha.compose.document.layout;

import com.demcha.compose.engine.components.style.Margin;

import java.util.Objects;

/**
 * Engine-side per-page-range margin override: the margin to apply to a contiguous
 * run of pages, addressed by 0-based page index. The canonical
 * {@code PageMarginRule} (1-based, public API) is translated into this internal
 * form by {@code DocumentSession} before it reaches the compiler.
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
     * Whether this override covers the given page.
     *
     * @param pageIndex a 0-based page index
     * @return {@code true} when {@code fromPageIndex <= pageIndex < toPageIndexExclusive}
     */
    public boolean coversPage(int pageIndex) {
        return pageIndex >= fromPageIndex && pageIndex < toPageIndexExclusive;
    }
}
