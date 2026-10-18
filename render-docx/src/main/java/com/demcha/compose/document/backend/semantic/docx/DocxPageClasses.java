package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.output.PageContext;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Which of Word's kinds of page a page zone is drawn on.
 *
 * <p>A zone chooses its pages with a predicate over the page, which the fixed-layout backends
 * ask once per page they draw. Word does not ask: it paginates on its own and gives a section
 * three kinds of header and footer — one for the first page, one for even pages, and one for
 * every other page. So the predicate is asked instead, over sample pages, and the answer is
 * sorted into those three kinds. A predicate that answers alike for every page of a kind is
 * one Word can state exactly: the first page only, every page but the first, even pages, odd
 * ones. One that does not — the last page, the third — has no Word equivalent, and is
 * reported as such rather than guessed.</p>
 *
 * <p>The sample is a document of the layout's own page count and of at least {@value #SAMPLE}
 * pages, so every kind is asked more than once where it can be: a predicate that tells the
 * third page from the fifth shows up as one that does not follow the kinds. A layout of fewer
 * pages is asked as well, each of its pages as the page asks it — one of as many as the layout
 * ran to — and its answers decide the kinds of page it has; the sample's, the rest. A predicate
 * that reads the total — a footer on one-page documents only — then answers for the layout's
 * pages as the page does, where the sample alone would ask them as pages of another
 * document.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxPageClasses {

    /** The kinds of page Word gives a header or footer of its own. */
    enum PageClass {
        /** The first page of a section, when the section states a title page. */
        FIRST,
        /** Even pages, when the document states different even and odd pages. */
        EVEN,
        /** Odd pages after the first — and every page Word has no other kind for. */
        LATER_ODD
    }

    /** The fewest pages a predicate is asked about. */
    static final int SAMPLE = 6;

    private DocxPageClasses() {
    }

    /**
     * The kinds of page a zone is drawn on.
     *
     * @param zone      the page zone
     * @param pageCount how many pages the layout ran to, or 0 when it is unknown
     * @return the kinds it is drawn on — every kind for a zone with no predicate, none for a
     *         zone drawn on no page — or {@code null} when its predicate does not answer
     *         alike for every page of a kind, of the sample or of the layout, or refuses one of
     *         the pages it is asked about
     */
    static Set<PageClass> of(DocumentPageZone zone, int pageCount) {
        if (zone.getAppliesTo() == null) {
            return EnumSet.allOf(PageClass.class);
        }
        Map<PageClass, Boolean> answers = answers(zone, Math.max(pageCount, SAMPLE));
        if (answers == null) {
            return null;
        }
        if (pageCount > 0 && pageCount < SAMPLE) {
            // The layout's own pages, asked as the page asks them, decide the kinds they are of.
            Map<PageClass, Boolean> laidOut = answers(zone, pageCount);
            if (laidOut == null) {
                return null;
            }
            answers.putAll(laidOut);
        }
        Set<PageClass> drawnOn = EnumSet.noneOf(PageClass.class);
        answers.forEach((kind, applies) -> {
            if (applies) {
                drawnOn.add(kind);
            }
        });
        return drawnOn;
    }

    /**
     * What a zone's predicate answers for each kind of page of a document so many pages long,
     * each page asked as one of them; {@code null} where it answers otherwise for two pages of a
     * kind, or refuses a page.
     */
    private static Map<PageClass, Boolean> answers(DocumentPageZone zone, int pages) {
        Map<PageClass, Boolean> answers = new EnumMap<>(PageClass.class);
        for (int number = 1; number <= pages; number++) {
            PageClass kind = classOf(number);
            boolean applies;
            try {
                applies = zone.appliesTo(PageContext.paginated(number, pages));
            } catch (RuntimeException pageItWasNeverAskedAbout) {
                // The sample can run past the document's own pages, and a predicate written
                // for those pages — one looking something up by page number — may refuse a
                // page that does not exist. That says nothing Word can use either way.
                return null;
            }
            Boolean earlier = answers.putIfAbsent(kind, applies);
            if (earlier != null && earlier != applies) {
                return null;
            }
        }
        return answers;
    }

    /** The kind Word files a 1-based page number under. */
    static PageClass classOf(int number) {
        if (number == 1) {
            return PageClass.FIRST;
        }
        return number % 2 == 0 ? PageClass.EVEN : PageClass.LATER_ODD;
    }
}
