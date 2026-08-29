package com.demcha.compose.document.output;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.PageFieldKind;
import com.demcha.compose.document.node.PageFieldNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.List;

/**
 * What a page zone knows about the page it is being drawn on.
 *
 * <p>Handed to a {@link DocumentPageZone}'s content function once per page. In a
 * combined document the numbers are section-local: each section's zone counts
 * from that section's first page, matching how the text chrome resolves its
 * tokens.</p>
 *
 * <h2>Two lanes, two ways to ask for the page number</h2>
 *
 * <p>A fixed-layout export (PDF, PPTX) paginates before the zone is built, so
 * {@link #number()} and {@link #total()} are real values and
 * {@code "Page " + page.number() + " of " + page.total()} is just string
 * concatenation — which is why a node zone needs no placeholder tokens, and why
 * roman numerals or a different line on the last page are ordinary Java in the
 * same lambda.</p>
 *
 * <p>A semantic export (DOCX) is different: it hands the node tree to Word and
 * <em>Word</em> paginates, so there is no page count to read. A number written
 * as text there would be right on one page and wrong on every other. So in that
 * lane {@link #number()} and {@link #total()} refuse rather than return a
 * plausible lie, and {@link #pageNumber()} / {@link #pageTotal()} — which give
 * back a node rather than an {@code int} — are the way to place a number that
 * survives export. They work in both lanes: resolved text in one, a live
 * {@code PAGE} field in the other.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.2.3
 */
public final class PageContext {

    private final int number;
    private final int total;
    private final boolean paginated;

    private PageContext(int number, int total, boolean paginated) {
        this.number = number;
        this.total = total;
        this.paginated = paginated;
    }

    /**
     * A context for a paginated (fixed-layout) render, where the numbers are known.
     *
     * @param number 1-based page number within the document or section
     * @param total  page count of the document or section
     * @return a context whose numbers can be read
     * @throws IllegalArgumentException if the numbers cannot describe a page
     */
    public static PageContext paginated(int number, int total) {
        if (number < 1) {
            throw new IllegalArgumentException("Page number must be 1-based, got " + number + ".");
        }
        if (total < number) {
            throw new IllegalArgumentException(
                    "Page total (" + total + ") cannot be smaller than the page number (" + number + ").");
        }
        return new PageContext(number, total, true);
    }

    /**
     * A context for a semantic export, where the consuming format paginates and
     * the numbers are therefore unknown here.
     *
     * @return a context that hands out page fields rather than page numbers
     */
    public static PageContext unpaginated() {
        return new PageContext(1, 1, false);
    }

    /**
     * Whether the numbers are known — {@code true} for a fixed-layout export.
     *
     * @return {@code true} when {@link #number()} and {@link #total()} may be read
     */
    public boolean isPaginated() {
        return paginated;
    }

    /**
     * The 1-based page number.
     *
     * @return the page number
     * @throws UnsupportedOperationException on a semantic export, where the
     *                                       consuming format paginates
     */
    public int number() {
        requirePaginated("number()");
        return number;
    }

    /**
     * The page count.
     *
     * @return the total number of pages
     * @throws UnsupportedOperationException on a semantic export, where the
     *                                       consuming format paginates
     */
    public int total() {
        requirePaginated("total()");
        return total;
    }

    /**
     * Whether this is the first page.
     *
     * @return {@code true} on page 1; always {@code true} on a semantic export,
     * where one zone definition serves every page
     */
    public boolean isFirst() {
        return number == 1;
    }

    /**
     * Whether this is the last page.
     *
     * @return {@code true} on the final page; always {@code true} on a semantic
     * export, where one zone definition serves every page
     */
    public boolean isLast() {
        return number == total;
    }

    /**
     * The page number as a node — resolved text on a fixed-layout export, a live
     * field on a semantic one.
     *
     * @return a node that renders this page's number
     */
    public DocumentNode pageNumber() {
        return pageNumber(DocumentTextStyle.DEFAULT);
    }

    /**
     * The page number as a node, in the given style.
     *
     * @param textStyle text style for the number
     * @return a node that renders this page's number
     */
    public DocumentNode pageNumber(DocumentTextStyle textStyle) {
        return paginated
                ? text(String.valueOf(number), textStyle)
                : new PageFieldNode(PageFieldKind.NUMBER, textStyle);
    }

    /**
     * The page count as a node — resolved text on a fixed-layout export, a live
     * field on a semantic one.
     *
     * @return a node that renders the page count
     */
    public DocumentNode pageTotal() {
        return pageTotal(DocumentTextStyle.DEFAULT);
    }

    /**
     * The page count as a node, in the given style.
     *
     * @param textStyle text style for the number
     * @return a node that renders the page count
     */
    public DocumentNode pageTotal(DocumentTextStyle textStyle) {
        return paginated
                ? text(String.valueOf(total), textStyle)
                : new PageFieldNode(PageFieldKind.TOTAL, textStyle);
    }

    private void requirePaginated(String accessor) {
        if (!paginated) {
            throw new UnsupportedOperationException(
                    "PageContext." + accessor + " is not available on a semantic export: the format"
                            + " paginates the document, so no page number exists here yet. Place"
                            + " pageNumber() / pageTotal() in the zone's content instead — they render"
                            + " as text on a fixed-layout export and as a live field in the exported"
                            + " document.");
        }
    }

    private static ParagraphNode text(String value, DocumentTextStyle textStyle) {
        return new ParagraphNode(
                "", value, List.of(),
                textStyle == null ? DocumentTextStyle.DEFAULT : textStyle,
                TextAlign.LEFT, 1.0, null, null, (com.demcha.compose.document.node.DocumentLinkOptions) null,
                null, DocumentInsets.zero(), DocumentInsets.zero(), null);
    }
}
