package com.demcha.compose.document.templates.cv;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Every string a CV template's composed layout carries.
 *
 * <p>Read from the layout rather than the PDF text layer because the CV
 * themes draw with the standard-14 Helvetica, whose encoding has no Cyrillic
 * — and a heading in the author's own alphabet is the case these tests care
 * about most. What a template owes is that the section reached the page
 * carrying its own words; which glyphs a font can draw is the caller's font
 * choice.</p>
 */
public final class CvComposedText {

    /**
     * Separates one paragraph from the next in {@link #squashedNodes}. A
     * control character, so no needle an assertion can build contains it.
     */
    private static final String BOUNDARY = String.valueOf((char) 1);

    private CvComposedText() {
    }

    /**
     * Composes the document and returns its text, paragraph by paragraph,
     * each {@link #squash squashed} and separated by a boundary.
     *
     * <p>Prefer this to squashing {@link #of}: that joins paragraphs with a
     * space, and squashing the result strips the one character that recorded
     * where a paragraph ended — so {@code contains("Mentor, Rails Girls")}
     * would be satisfied by a template that drew "Mentor," in one node and
     * "Rails Girls" in another. Here a match has to live inside a single
     * paragraph, while an assertion that spans them on purpose still reads
     * across the boundary as a subsequence.</p>
     *
     * @param template the preset to compose with
     * @param doc      the CV to compose
     * @return the squashed paragraphs, boundary-separated
     */
    public static String squashedNodes(DocumentTemplate<CvDocument> template,
                                       CvDocument doc) {
        StringBuilder out = new StringBuilder();
        for (String paragraph : paragraphs(template, doc)) {
            out.append(squash(paragraph)).append(BOUNDARY);
        }
        return out.toString();
    }

    /**
     * Composes the document and returns its text as one string.
     *
     * @param template the preset to compose with
     * @param doc      the CV to compose
     * @return every string the composed layout carries, space-separated
     */
    public static String of(DocumentTemplate<CvDocument> template, CvDocument doc) {
        return String.join(" ", paragraphs(template, doc));
    }

    /**
     * Drops spacing and case so an assertion can name the words a template
     * owes without owning its typography. These presets letter-space and
     * upper-case their headings — "Core Skills" reaches the page as
     * "C O R E   S K I L L S", and the gap inside a word is U+00A0, which no
     * whitespace class matches.
     *
     * @param value text to normalise
     * @return the value with all spacing removed, lower-cased
     */
    public static String squash(String value) {
        // Doubled backslashes on purpose. In a Java string literal "\s" is the
        // escape for a single space rather than the regex class, and a
        // single-backslash unicode escape is folded to the character itself
        // before the regex engine ever sees it — either way leaving a pattern
        // that reads like this one but matches neither tabs nor newlines.
        return value.replaceAll("[\\s\\u00A0]+", "").toLowerCase(Locale.ROOT);
    }

    /**
     * How many times {@code needle} occurs in {@code haystack}, without overlap.
     *
     * @param haystack text to search
     * @param needle   text to count
     * @return the number of non-overlapping occurrences
     */
    public static int occurrences(String haystack, String needle) {
        int count = 0;
        for (int from = haystack.indexOf(needle); from >= 0;
             from = haystack.indexOf(needle, from + needle.length())) {
            count++;
        }
        return count;
    }

    /** One entry per paragraph the composed layout carries, in layout order. */
    private static List<String> paragraphs(DocumentTemplate<CvDocument> template,
                                           CvDocument doc) {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(24, 24, 24, 24)
                .create()) {
            template.compose(session, doc);
            List<String> texts = new ArrayList<>();
            collect(session.roots(), texts);
            return texts;
        }
    }

    private static void collect(List<DocumentNode> nodes, List<String> out) {
        for (DocumentNode node : nodes) {
            if (node instanceof ParagraphNode paragraph) {
                // text() carries the plain string AND the concatenation of any
                // rich runs, so it sees both a header written with .text(...)
                // and a body assembled from markdown runs.
                out.add(paragraph.text());
            }
            collect(node.children(), out);
        }
    }
}
