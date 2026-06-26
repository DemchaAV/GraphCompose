package com.demcha.compose.document.api;

import com.demcha.compose.document.style.DocumentInsets;

import java.util.Objects;

/**
 * A per-page-range margin override: the page margin to use for a contiguous run
 * of pages, replacing the document-wide {@link DocumentSession#margin(DocumentInsets)}
 * for the pages it covers. Supplied to {@link DocumentSession#pageMargins(java.util.List)}.
 *
 * <p>Pages are 1-based (page&nbsp;1 is the first page). The range is half-open —
 * {@code [fromPage, toPageExclusive)} — so {@code of(1, 2, …)} covers page&nbsp;1
 * only. Use the factory methods for the common cases:</p>
 * <ul>
 *   <li>{@link #page(int, DocumentInsets)} — a single page.</li>
 *   <li>{@link #range(int, int, DocumentInsets)} — an inclusive page range.</li>
 *   <li>{@link #from(int, DocumentInsets)} — a page to the end of the document.</li>
 * </ul>
 *
 * <p>Rules are applied in list order and the <em>last</em> rule that covers a page
 * wins, mirroring {@link DocumentSession#pageBackgrounds(java.util.List)}. A page no
 * rule covers keeps the document-wide margin.</p>
 *
 * <p>Each block of content is laid out at the width of the page it begins on, so a
 * margin that changes the content width takes effect where content breaks onto a
 * page the rule covers — the intended use is different margins for different page
 * ranges (e.g. a full-bleed cover with {@code zero()} insets followed by book
 * margins on the body). To extend a single node past the page edge instead, use
 * {@code bleed(...)} (see {@link com.demcha.compose.document.style.DocumentBleed}).</p>
 *
 * @param fromPage         first page the rule covers, 1-based and inclusive
 * @param toPageExclusive  one past the last page the rule covers (exclusive);
 *                         {@link Integer#MAX_VALUE} runs to the end of the document
 * @param insets           the page margin to use for the covered pages
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public record PageMarginRule(int fromPage, int toPageExclusive, DocumentInsets insets) {

    /**
     * Validates the page range and that the insets are non-negative.
     */
    public PageMarginRule {
        Objects.requireNonNull(insets, "insets");
        if (fromPage < 1) {
            throw new IllegalArgumentException("fromPage must be >= 1 (pages are 1-based) but was " + fromPage);
        }
        if (toPageExclusive <= fromPage) {
            throw new IllegalArgumentException(
                    "toPageExclusive (" + toPageExclusive + ") must be greater than fromPage (" + fromPage + ")");
        }
        if (insets.top() < 0 || insets.right() < 0 || insets.bottom() < 0 || insets.left() < 0) {
            throw new IllegalArgumentException(
                    "page margin must be non-negative; use bleed() to extend a node past the page edge "
                    + "(see DocumentBleed). Insets were " + insets);
        }
    }

    /**
     * A rule covering a single page.
     *
     * @param page   the 1-based page number
     * @param insets the page margin to use on that page
     * @return a rule covering exactly {@code page}
     */
    public static PageMarginRule page(int page, DocumentInsets insets) {
        return new PageMarginRule(page, page + 1, insets);
    }

    /**
     * A rule covering an inclusive page range.
     *
     * @param fromPage first page (1-based, inclusive)
     * @param toPage   last page (1-based, inclusive)
     * @param insets   the page margin to use across the range
     * @return a rule covering {@code fromPage..toPage}
     */
    public static PageMarginRule range(int fromPage, int toPage, DocumentInsets insets) {
        if (toPage < fromPage) {
            throw new IllegalArgumentException("toPage (" + toPage + ") must be >= fromPage (" + fromPage + ")");
        }
        return new PageMarginRule(fromPage, toPage + 1, insets);
    }

    /**
     * A rule covering a page through to the end of the document.
     *
     * @param fromPage first page (1-based, inclusive)
     * @param insets   the page margin to use from {@code fromPage} onward
     * @return a rule covering {@code fromPage} to the last page
     */
    public static PageMarginRule from(int fromPage, DocumentInsets insets) {
        return new PageMarginRule(fromPage, Integer.MAX_VALUE, insets);
    }

    /**
     * Whether this rule covers the given page.
     *
     * @param pageNumber a 1-based page number
     * @return {@code true} when {@code fromPage <= pageNumber < toPageExclusive}
     */
    public boolean coversPage(int pageNumber) {
        return pageNumber >= fromPage && pageNumber < toPageExclusive;
    }
}
