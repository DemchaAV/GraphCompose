package com.demcha.compose.document.templates.core.page;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageMarginRule;
import com.demcha.compose.document.style.DocumentInsets;

import java.util.List;
import java.util.Objects;

/**
 * Keeps a full-bleed template's content off the trimmed edge on the pages its body
 * continues onto.
 *
 * <p>A container's padding is an edge of the container: it is reserved once, at the
 * top of the page the container opens on and the bottom of the page it closes on.
 * The page margin is the inset applied once per <em>page</em>. In an ordinary
 * document the distinction never surfaces, because the margin is already holding
 * content off the paper edge everywhere. It surfaces in a full-bleed template, which
 * sets the page margin to zero so a page background can reach the paper edge — and
 * thereby gives up the safe area on all four sides when only the two horizontal ones
 * had to go. A body that runs past its first page then resumes wherever the break
 * left it, which can be a couple of points from the edge.</p>
 *
 * <p>{@link #applyTo(DocumentSession, int, double)} raises the top margin from a
 * given page onward and keeps the other three edges of whatever margin the caller
 * chose, so the layout stays full-bleed horizontally and page backgrounds — which
 * are ratios of the page, not of the content box — keep bleeding on every page.</p>
 *
 * <p>This lives in {@code templates} rather than in the engine because the rule it
 * writes is a design decision — which page, which edge, how much — over a per-page
 * margin mechanism the engine already exposes. A template that wants a different
 * shape writes its own {@link PageMarginRule}.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.3.0
 */
public final class ContinuationSafeArea {

    /**
     * Half an inch — the safe area every common consumer printer's non-printable
     * band fits inside. A template is free to ask for more or less; this is the
     * number to reach for when there is no reason to pick another.
     */
    public static final double PRINTER_SAFE_TOP = 36.0;

    private ContinuationSafeArea() {
    }

    /**
     * Reserves {@code topSafeArea} at the top of every page from
     * {@code firstContinuationPage} onward, leaving the pages before it alone.
     *
     * <p>Does nothing at all when the document margin already provides the safe
     * area, so a template can call this unconditionally: a document that does not
     * need per-page geometry is not given any, and any rules the caller set
     * themselves survive untouched. When it does apply, it <strong>replaces</strong>
     * the session's page-margin rules — {@link DocumentSession#pageMargins(List)} has
     * no additive form — so a template that also wants rules of its own has to write
     * them together rather than call this as well.</p>
     *
     * @param document             the session to configure
     * @param firstContinuationPage 1-based number of the first page to inset — the
     *                             page after the one the body starts on
     * @param topSafeArea          top inset in points; see {@link #PRINTER_SAFE_TOP}
     * @throws IllegalArgumentException if {@code firstContinuationPage} is below 2,
     *                                  or {@code topSafeArea} is negative or not finite
     */
    public static void applyTo(DocumentSession document,
                               int firstContinuationPage,
                               double topSafeArea) {
        Objects.requireNonNull(document, "document");
        if (firstContinuationPage < 2) {
            throw new IllegalArgumentException(
                    "firstContinuationPage must be 2 or greater — page 1 is where a body starts, "
                            + "and its own design owns its top edge. Was " + firstContinuationPage);
        }
        if (!Double.isFinite(topSafeArea) || topSafeArea < 0.0) {
            throw new IllegalArgumentException(
                    "topSafeArea must be a finite, non-negative value but was " + topSafeArea);
        }
        DocumentInsets margin = document.margin();
        if (margin.top() >= topSafeArea) {
            return;
        }
        document.pageMargins(List.of(PageMarginRule.from(firstContinuationPage,
                new DocumentInsets(topSafeArea, margin.right(), margin.bottom(), margin.left()))));
    }
}
