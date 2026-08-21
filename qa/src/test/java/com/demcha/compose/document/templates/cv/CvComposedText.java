package com.demcha.compose.document.templates.cv;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;

import java.util.List;
import java.util.Locale;

/**
 * Every string a CV template's composed layout carries, joined.
 *
 * <p>Read from the layout rather than the PDF text layer because the CV
 * themes draw with the standard-14 Helvetica, whose encoding has no Cyrillic
 * — and a heading in the author's own alphabet is the case these tests care
 * about most. What a template owes is that the section reached the page
 * carrying its own words; which glyphs a font can draw is the caller's font
 * choice.</p>
 */
public final class CvComposedText {

    private CvComposedText() {
    }

    /**
     * Composes the document and returns its text.
     *
     * @param template the preset to compose with
     * @param doc      the CV to compose
     * @return every string the composed layout carries, space-separated
     */
    public static String of(DocumentTemplate<CvDocument> template, CvDocument doc) {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(24, 24, 24, 24)
                .create()) {
            template.compose(session, doc);
            StringBuilder text = new StringBuilder();
            collect(session.roots(), text);
            return text.toString();
        }
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
        // Doubled backslashes on purpose: "\s" in a Java string literal is the
        // escape for a single space, not the regex class, and "\u00A0" would be
        // folded to the character itself before the regex ever saw it \u2014 leaving
        // a pattern that reads like this one but matches neither tabs nor
        // newlines.
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

    private static void collect(List<DocumentNode> nodes, StringBuilder out) {
        for (DocumentNode node : nodes) {
            if (node instanceof ParagraphNode paragraph) {
                // text() carries the plain string AND the concatenation of any
                // rich runs, so it sees both a header written with .text(...)
                // and a body assembled from markdown runs.
                out.append(paragraph.text()).append(' ');
            }
            collect(node.children(), out);
        }
    }
}
