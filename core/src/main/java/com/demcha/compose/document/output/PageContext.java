package com.demcha.compose.document.output;

import com.demcha.compose.document.api.Beta;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.PageFieldKind;
import com.demcha.compose.document.node.PageFieldNode;
import com.demcha.compose.document.style.DocumentTextStyle;


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
 * survives export. One node type serves both lanes: a paginated render resolves
 * it to the number, a semantic export maps it onto a live {@code PAGE} field.</p>
 *
 * <p>What a semantic export can tell apart is the kind of page: it builds the
 * zone for each kind the format has a header or footer for, so
 * {@link #isFirst()} still chooses a cover's line over the running one there.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.3.0
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
     * @return a context that hands out page fields rather than page numbers, the
     *         first page and the last
     */
    public static PageContext unpaginated() {
        return unpaginated(true, true);
    }

    /**
     * A context for a semantic export that writes a zone once for each kind of page
     * the consuming format tells apart: Word, for one, has a header and footer for
     * a section's first page and another for the pages after it.
     *
     * <p>The numbers stay unknown, as with {@link #unpaginated()};
     * {@link #isFirst()} and {@link #isLast()} answer for the kind of page the
     * zone is built for.</p>
     *
     * <p><b>Experimental</b> ({@code @Beta}) — see {@code docs/api-stability.md}.</p>
     *
     * @param first whether the zone is built for the first page
     * @param last  whether it is built for the last page
     * @return a context that hands out page fields rather than page numbers
     * @since 2.5.0
     */
    @Beta
    public static PageContext unpaginated(boolean first, boolean last) {
        // Never read on a semantic export, the numbers only answer isFirst and isLast.
        int number = first ? 1 : 2;
        return new PageContext(number, last ? number : number + 1, false);
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
     * @return {@code true} on page 1; on a semantic export, whether the zone is
     * built for the kind of page the first one is ({@link #unpaginated(boolean, boolean)})
     */
    public boolean isFirst() {
        return number == 1;
    }

    /**
     * Whether this is the last page.
     *
     * @return {@code true} on the final page; on a semantic export, whether the
     * zone is built for the last page ({@link #unpaginated(boolean, boolean)})
     */
    public boolean isLast() {
        return number == total;
    }

    /**
     * The page number as a node, which each lane resolves for itself: a paginated
     * render draws the number, a semantic export maps it onto the format's own
     * field.
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
        return new PageFieldNode(PageFieldKind.NUMBER, textStyle);
    }

    /**
     * The page count as a node, which each lane resolves for itself: a paginated
     * render draws the count, a semantic export maps it onto the format's own
     * field.
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
        return new PageFieldNode(PageFieldKind.TOTAL, textStyle);
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

}
