package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.payloads.ParagraphLine;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.impl.values.XmlValueDisconnectedException;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Keeps Word and LibreOffice from breaking a line inside the word the page starts a line with,
 * at a hyphen in it.
 *
 * <p>The page breaks a line only at a space; a hyphen inside a word is a place to break only for
 * a word wider than the whole line. Word and LibreOffice also break after a hyphen between two
 * letters or digits. So where the page carries a hyphenated word over to its next line whole
 * — {@code developer-facing}, {@code 2023-2024} — an editor sets its first part at the end of the
 * line above, and every line of the paragraph after it breaks at other words (measured in Word 16
 * and LibreOffice 24.2: every line five CVs and a cover letter lost was of this, 34 of the 58 lines
 * Word lost across the fidelity corpus).</p>
 *
 * <p>Each hyphen between two letters or digits in the first word of every line of the page's
 * but the paragraph's first is written as Word's non-breaking hyphen ({@code w:noBreakHyphen}),
 * which both editors draw as the hyphen and do not break at, and which Word's and LibreOffice's
 * PDFs carry as an ordinary hyphen. No other hyphen is touched: a reader of the file's text sees
 * a non-breaking hyphen only in a word that starts one of the page's lines (POI's text extractor
 * reads it as U+2011, Word's own text as 0x1E). A word the page breaks
 * itself, at a hyphen or anywhere, because it is wider than the line, is not held anywhere: not on
 * the line it starts, which it runs off the end of, nor on the line it runs on into.</p>
 *
 * <p>The hyphens are held once a section is written and before any of its tables is cut at its
 * pages, so a paragraph a cut copies carries them; a paragraph taken out of the document after it
 * was written holds none, and neither does a paragraph written into a text box over the flow,
 * whose box takes a copy of it as soon as it is written. Two paragraphs written into one Word paragraph — a line pair's halves —
 * are each read against the whole of it, and the second is left as written where the first's held
 * hyphens leave its letters standing other than once.</p>
 *
 * <p>Where in the written text a line starts is read from the letters: the page's lines and the
 * paragraph's runs hold the same letters in the same order, white space apart, give or take a
 * list's marker — written as characters before the item's text, or drawn by Word's numbering and
 * held by the page's lines alone. A paragraph whose runs do not hold its lines' letters exactly
 * once, nor its lines the runs' — text the file writes otherwise than the page sets it — is left
 * as written, as is one whose page lines are set right to left.</p>
 */
final class DocxLineStartHyphens {

    private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final QName TEXT = new QName(W, "t");
    private static final QName BREAK = new QName(W, "br");
    private static final QName CARRIAGE_RETURN = new QName(W, "cr");
    private static final QName TAB = new QName(W, "tab");
    private static final QName NO_BREAK_HYPHEN = new QName(W, "noBreakHyphen");
    private static final QName XML_SPACE = new QName("http://www.w3.org/XML/1998/namespace", "space");

    private final List<Written> written = new ArrayList<>();

    /** A paragraph written, with the lines the page set its text in. */
    private record Written(CTP paragraph, List<ParagraphLine> lines) {
    }

    /** One letter of a paragraph's text as written: the text element and the place in it. */
    private record Letter(CTText text, int at, char value, boolean spaceBefore) {
    }

    /**
     * Notes a paragraph and the lines the page set its text in, held when {@link #hold} runs. A
     * paragraph of one line, or of none the layout laid out, has no line to hold.
     */
    void record(CTP paragraph, List<ParagraphLine> lines) {
        if (paragraph != null && lines != null && lines.size() > 1) {
            written.add(new Written(paragraph, List.copyOf(lines)));
        }
    }

    /** Forgets every paragraph noted, for the next document. */
    void clear() {
        written.clear();
    }

    /**
     * Writes the hyphens to hold in every paragraph noted since the last call as non-breaking
     * ones, and forgets them. A paragraph taken out of the document since it was noted is passed
     * over.
     *
     * @return how many hyphens were held
     */
    int hold() {
        int held = 0;
        for (Written paragraph : written) {
            try {
                held += hold(paragraph.paragraph(), paragraph.lines());
            } catch (XmlValueDisconnectedException removed) {
                // Taken out of the document after it was written: there is nothing left to hold.
            }
        }
        written.clear();
        return held;
    }

    /**
     * Holds the hyphens of one paragraph's line-starting words.
     *
     * @return how many hyphens were held
     */
    static int hold(CTP paragraph, List<ParagraphLine> lines) {
        if (lines.stream().anyMatch(line -> !line.visualOrder().isEmpty())) {
            return 0;
        }
        List<Letter> letters = lettersOf(paragraph);
        StringBuilder writtenLetters = new StringBuilder(letters.size());
        letters.forEach(letter -> writtenLetters.append(letter.value()));
        StringBuilder pageLetters = new StringBuilder();
        List<Integer> lineStarts = new ArrayList<>();
        for (ParagraphLine line : lines) {
            lineStarts.add(pageLetters.length());
            appendLetters(pageLetters, line.text());
        }
        String page = pageLetters.toString().toLowerCase(Locale.ROOT);
        String text = writtenLetters.toString().toLowerCase(Locale.ROOT);
        if (page.isEmpty() || page.length() != pageLetters.length() || text.length() != writtenLetters.length()) {
            return 0;
        }
        // Where the page's letters stand in the written ones: the runs can hold more — a list's
        // marker written as characters — or fewer — a marker Word's numbering draws, which the
        // page's lines hold. Either way the letters shared stand once, or nothing is held.
        int offset = once(text, page);
        if (offset < 0) {
            int within = text.isEmpty() ? -1 : once(page, text);
            if (within < 0) {
                return 0;
            }
            offset = -within;
        }
        List<Letter> toHold = new ArrayList<>();
        for (int line = 1; line < lineStarts.size(); line++) {
            int start = offset + lineStarts.get(line);
            int end = Math.min(letters.size(),
                    offset + (line + 1 < lineStarts.size() ? lineStarts.get(line + 1) : page.length()));
            // A line that starts inside a word is the page breaking a word too wide for it: an
            // editor breaks that word its own way, and none of it is held.
            if (start < 0 || start >= end || !letters.get(start).spaceBefore()) {
                continue;
            }
            int wordEnd = start + 1;
            while (wordEnd < end && !letters.get(wordEnd).spaceBefore()) {
                wordEnd++;
            }
            // A first word that runs on into the next line is one too wide for this one, which the
            // page breaks itself.
            if (wordEnd == end && end < letters.size() && !letters.get(end).spaceBefore()) {
                continue;
            }
            for (int index = start + 1; index + 1 < wordEnd; index++) {
                if (letters.get(index).value() == '-'
                    && Character.isLetterOrDigit(letters.get(index - 1).value())
                    && Character.isLetterOrDigit(letters.get(index + 1).value())) {
                    toHold.add(letters.get(index));
                }
            }
        }
        // Right to left, so a text element split at a later hyphen still holds the earlier ones
        // where they were.
        for (int index = toHold.size() - 1; index >= 0; index--) {
            Letter hyphen = toHold.get(index);
            splitAtTheHyphen(hyphen.text(), hyphen.at());
        }
        return toHold.size();
    }

    /** Where {@code part} stands in {@code whole} if it stands there exactly once, else -1. */
    private static int once(String whole, String part) {
        int at = whole.indexOf(part);
        return at >= 0 && whole.indexOf(part, at + 1) < 0 ? at : -1;
    }

    private static void appendLetters(StringBuilder into, String text) {
        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            if (!Character.isWhitespace(value) && !Character.isSpaceChar(value)) {
                into.append(value);
            }
        }
    }

    /**
     * The paragraph's letters in the order it writes them, its runs' and its links' runs', each
     * with whether white space — a space, a tab, a line break — stands before it. A run's edge is
     * no space: a word can run on from one run into the next.
     */
    private static List<Letter> lettersOf(CTP paragraph) {
        List<Letter> letters = new ArrayList<>();
        boolean space = true;
        try (XmlCursor runs = paragraph.newCursor()) {
            runs.selectPath("declare namespace w='" + W + "' ./w:r | ./w:hyperlink/w:r");
            while (runs.toNextSelection()) {
                if (!(runs.getObject() instanceof CTR run)) {
                    continue;
                }
                try (XmlCursor child = run.newCursor()) {
                    if (!child.toFirstChild()) {
                        continue;
                    }
                    do {
                        QName name = child.getName();
                        XmlObject object = child.getObject();
                        if (TEXT.equals(name) && object instanceof CTText text) {
                            String value = text.getStringValue();
                            for (int index = 0; index < value.length(); index++) {
                                char letter = value.charAt(index);
                                if (Character.isWhitespace(letter) || Character.isSpaceChar(letter)) {
                                    space = true;
                                } else {
                                    letters.add(new Letter(text, index, letter, space));
                                    space = false;
                                }
                            }
                        } else if (BREAK.equals(name) || CARRIAGE_RETURN.equals(name) || TAB.equals(name)) {
                            space = true;
                        }
                    } while (child.toNextSibling());
                }
            }
        }
        return letters;
    }

    /**
     * Splits a text element at the hyphen at {@code at}: the text before it stays, the hyphen
     * becomes a non-breaking one, and the text after it follows in a text element of its own.
     */
    private static void splitAtTheHyphen(CTText text, int at) {
        String value = text.getStringValue();
        String after = value.substring(at + 1);
        text.setStringValue(value.substring(0, at));
        preserveSpace(text);
        try (XmlCursor cursor = text.newCursor()) {
            cursor.toEndToken();
            cursor.toNextToken();
            cursor.insertElement(NO_BREAK_HYPHEN);
            if (!after.isEmpty()) {
                cursor.beginElement(TEXT);
                cursor.insertAttributeWithValue(XML_SPACE, "preserve");
                cursor.insertChars(after);
            }
        }
    }

    private static void preserveSpace(CTText text) {
        try (XmlCursor cursor = text.newCursor()) {
            cursor.setAttributeText(XML_SPACE, "preserve");
        }
    }
}
