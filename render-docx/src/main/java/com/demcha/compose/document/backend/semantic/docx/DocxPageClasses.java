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
 * <p>The sample runs to the document's own page count and to at least {@value #SAMPLE}
 * pages, so every kind is asked more than once where it can be: a predicate that tells the
 * third page from the fifth shows up as one that does not follow the kinds.</p>
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
     *         alike for every page of a kind, or refuses one of the sample pages
     */
    static Set<PageClass> of(DocumentPageZone zone, int pageCount) {
        if (zone.getAppliesTo() == null) {
            return EnumSet.allOf(PageClass.class);
        }
        int total = Math.max(pageCount, SAMPLE);
        Map<PageClass, Boolean> answers = new EnumMap<>(PageClass.class);
        for (int number = 1; number <= total; number++) {
            PageClass kind = classOf(number);
            boolean applies;
            try {
                applies = zone.appliesTo(PageContext.paginated(number, total));
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
        Set<PageClass> drawnOn = EnumSet.noneOf(PageClass.class);
        answers.forEach((kind, applies) -> {
            if (applies) {
                drawnOn.add(kind);
            }
        });
        return drawnOn;
    }

    /** The kind Word files a 1-based page number under. */
    static PageClass classOf(int number) {
        if (number == 1) {
            return PageClass.FIRST;
        }
        return number % 2 == 0 ? PageClass.EVEN : PageClass.LATER_ODD;
    }
}
