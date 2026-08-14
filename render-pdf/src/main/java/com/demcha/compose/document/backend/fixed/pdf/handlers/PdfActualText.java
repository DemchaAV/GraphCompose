package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.engine.text.bidi.ArabicShaper;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;

/**
 * Lets a reordered run state, in the file, what it says.
 *
 * <p>A page is painted left to right, so a right-to-left run reaches the content stream
 * backwards — that is what putting it on the page means. Nothing in the file said so, which
 * left every reader to work the letters back out for itself. Most do, by running the
 * bidirectional algorithm over what they extracted; a reader that does not gets each word
 * reversed.</p>
 *
 * <p>{@code ActualText} is the mechanism the format provides for exactly this: content whose
 * meaning differs from the marks that were drawn. Each reversed run is wrapped in a marked
 * content section carrying its text as it was written — for Arabic, as the letters rather
 * than the joined forms they were drawn as. In the stream that is {@code /Span /PropN BDC},
 * with the property list holding the text living in the page's resources; PDFBox registers
 * one entry per section, which costs an Arabic page roughly a tenth of its size in
 * bookkeeping. Writing the dictionary inline would reclaim that, and needs an operator the
 * content-stream API does not expose.</p>
 *
 * <p>The section is one <em>run</em>, deliberately not the line. A reader that honours
 * {@code ActualText} takes it instead of the glyphs it covers, so the section's width is
 * exactly the width of what it replaces: wrap the line and a left-to-right word inside it is
 * swallowed too, its per-glyph text gone and its order at the mercy of how the reader
 * reassembles the substitute. Wrapped run by run, every left-to-right neighbour stays
 * ordinary glyphs, and what the section replaces is precisely the sequence that was drawn
 * backwards. The order of runs across the line stays a bidirectional question either way —
 * that is the nature of a painted page — but the letters inside each run now read the way
 * they were written, for any reader, algorithm or none.</p>
 */
final class PdfActualText {

    /** The generic inline-content tag; a reordered run is not a structural element. */
    private static final COSName SPAN = COSName.getPDFName("Span");

    private PdfActualText() {
    }

    /**
     * What a reversed run says: its text as written.
     *
     * <p>The span's text is logical already — reversal happens at the drawing seam — but for
     * Arabic it carries the joined forms the engine shaped it into, and those are a fact
     * about drawing. The section is the half of the file that is about meaning, so the base
     * letters go in. Mirrored punctuation likewise never enters: the span's own text still
     * holds the parenthesis the author typed, and the swap happens only in what is drawn.</p>
     *
     * <p>Deliberately the author's text, not the sanitised string the page draws. A glyph
     * the font cannot encode is drawn as {@code '?'}, and the section still states the
     * character the author wrote — replacement text is what {@code ActualText} is for, and
     * a search for the real character should find the run whose drawing degraded. The same
     * goes for the bidi controls the drawing strips: they are part of the written text, and
     * a copy that carries them re-pastes with its directions intact.</p>
     *
     * @param span a span whose glyphs go out in display order
     * @return the text as written, or {@code null} when there is nothing to state
     */
    static String writtenTextOf(ParagraphTextSpan span) {
        return writtenTextOf(span.text());
    }

    /**
     * As {@link #writtenTextOf(ParagraphTextSpan)}, for a line that is not a span.
     *
     * <p>A table cell is drawn line by line rather than span by span, and it owes a reader
     * the same thing a paragraph does.</p>
     *
     * @param logical the line as written
     * @return the text as written, or {@code null} when there is nothing to state
     */
    static String writtenTextOf(String logical) {
        if (logical == null) {
            return null;
        }
        String written = ArabicShaper.toBaseLetters(logical);
        return written.isEmpty() ? null : written;
    }

    /**
     * The marked-content properties that carry {@code text}.
     *
     * @param text the run's text as written
     * @return a property list holding it as this section's {@code ActualText}
     */
    static PDPropertyList properties(String text) {
        COSDictionary dictionary = new COSDictionary();
        // COSString picks its own encoding, and takes UTF-16BE for anything PDFDocEncoding
        // cannot spell — which is every script this exists for.
        dictionary.setItem(COSName.ACTUAL_TEXT, new COSString(text));
        return PDPropertyList.create(dictionary);
    }

    /** The tag a wrapped run carries. */
    static COSName tag() {
        return SPAN;
    }
}
