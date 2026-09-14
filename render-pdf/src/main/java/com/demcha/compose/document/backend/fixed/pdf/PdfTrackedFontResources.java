package com.demcha.compose.document.backend.fixed.pdf;

import org.apache.fontbox.ttf.CmapLookup;
import org.apache.fontbox.ttf.gsub.GsubWorker;
import org.apache.fontbox.ttf.gsub.GsubWorkerFactory;
import org.apache.fontbox.ttf.model.GsubData;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Font resources that carry letter spacing in their glyph widths instead of in {@code Tc}.
 *
 * <p>A letter-spaced run drawn with {@code Tc} puts every glyph exactly where it belongs, but
 * the space it adds sits <em>between</em> the glyph boxes a reader derives from the font's
 * widths. Readers that ignore {@code ActualText} find word breaks by those gaps: pdf.js inserts
 * a space once a gap passes about a tenth of the font size, and pdfplumber once it passes its
 * default tolerance of three points. At 0.18em pdf.js therefore read every tracked heading as
 * {@code P R O F E S S I O N A L}, and pdfplumber read the runs large enough to cross three
 * points, such as a 21.5pt name, as {@code A R T E M}; a CV parser built on either could not find
 * its sections or its candidate's name.</p>
 *
 * <p>So an eligible run is drawn with a second font resource over the <em>same</em> embedded
 * font program: the base font's FontFile2, ToUnicode, CIDToGIDMap and FontDescriptor, shared by
 * reference, with every {@code /W} entry and {@code /DW} raised by the tracking in thousandths of
 * an em. Glyph origins do not move, but each glyph's box now reaches the next glyph, so there is
 * no gap left to read as a word break. No font program is duplicated or modified.</p>
 *
 * <p>The widths are whole numbers. PDFium, the renderer in Chrome, reads CID widths as integers:
 * a fractional width moved glyphs there and nowhere else. The part of the tracking a whole
 * thousandth of an em cannot state stays in {@code Tc} &mdash; at most half a thousandth of the
 * font size, far below any gap a reader would split on.</p>
 *
 * <p>The raised widths disagree with the advances in the font program on purpose. ISO 32000 only
 * recommends that the two agree, so the file is valid; PDF/A and PDF/UA require it, so these
 * resources must not be used for output that claims either. The backend offers no such output
 * today.</p>
 *
 * <p>Eligible: positive tracking of at least half a thousandth of an em, drawn with a horizontal
 * {@link PDType0Font} that is embedded as a subset, showing text its GSUB substitutions (if the
 * face keeps any) leave unchanged. Everything else keeps {@code Tc}: Standard 14 faces (no
 * embedded program to share), vertical text, negative tracking (tightening never opens a gap),
 * and a run whose glyphs the substitutions would rewrite &mdash; the content stream shapes such a
 * run through PDFBox's GSUB worker for the base font, which a second resource would bypass.</p>
 *
 * <p>One resource per base font and whole per-mille delta, per document, whatever size or page it
 * is drawn at. Its dictionaries can only be completed after PDFBox has built the base font's
 * subset, which happens inside {@code save()}; {@link PdfSubsetAwareSave} does that.</p>
 */
final class PdfTrackedFontResources {

    private static final Logger LOG = LoggerFactory.getLogger("com.demcha.compose.engine.render");

    /** Beyond a hundred ems of tracking a run keeps {@code Tc}; no layout asks for it. */
    private static final long MAX_EXTRA_PER_MILLE = 100_000;

    /** Remainders below a millionth of a point are written as no {@code Tc} at all. */
    private static final double NO_REMAINDER = 1.0e-6;

    private final PDDocument document;
    private final Map<PDFont, Boolean> eligibility = new IdentityHashMap<>();
    private final Map<PDType0Font, GsubWorker> gsubWorkers = new IdentityHashMap<>();
    private final Map<PDType0Font, Map<Integer, PdfTrackedFontView>> views = new IdentityHashMap<>();

    PdfTrackedFontResources(PDDocument document) {
        this.document = document;
    }

    /**
     * Resolves the face and character spacing for letter-spaced text.
     *
     * @param font          the face the text would otherwise be drawn with; a face this registry
     *                      returned stands for its base font
     * @param fontSize      font size in points
     * @param letterSpacing the letter spacing in points
     * @param text          the text the returned face would draw
     * @return the face to draw with and the remaining character spacing, or {@code null} when
     *         the text keeps drawing with {@code font} and {@code Tc}
     */
    PdfRenderEnvironment.LetterSpacedFont resolve(PDFont font, double fontSize, double letterSpacing, String text) {
        if (!(letterSpacing > 0.0) || !(fontSize > 0.0) || text == null || text.isEmpty()) {
            return null;
        }
        // Asking again with a face handed out earlier must not stack its widths on top of the
        // spacing it already carries.
        PDFont face = font instanceof PdfTrackedFontView view ? view.base() : font;
        long extraPerMille = Math.round(letterSpacing / fontSize * 1000.0);
        if (extraPerMille < 1 || extraPerMille > MAX_EXTRA_PER_MILLE || !eligible(face)) {
            return null;
        }
        PDType0Font base = (PDType0Font) face;
        if (!substitutionsLeaveGlyphsAlone(base, text)) {
            return null;
        }
        PdfTrackedFontView view = view(base, (int) extraPerMille);
        if (view == null) {
            return null;
        }
        double remainder = letterSpacing - extraPerMille * fontSize / 1000.0;
        return new PdfRenderEnvironment.LetterSpacedFont(view,
                Math.abs(remainder) < NO_REMAINDER ? 0f : (float) remainder);
    }

    /**
     * Whether any run was drawn with a letter-spaced resource, which the save then has to complete.
     *
     * @return {@code true} once a resource exists
     */
    boolean hasResources() {
        return !views.isEmpty();
    }

    /**
     * Fills in every resource from its base font. Must run after the base fonts are subset and
     * after any correction that replaces a base font's ToUnicode stream, because the resources
     * share those objects by reference.
     */
    void completeAfterSubsetting() {
        for (Map<Integer, PdfTrackedFontView> byDelta : views.values()) {
            for (PdfTrackedFontView view : byDelta.values()) {
                view.complete();
            }
        }
    }

    private boolean eligible(PDFont font) {
        Boolean known = eligibility.get(font);
        if (known != null) {
            return known;
        }
        boolean eligible = font instanceof PDType0Font type0
                && type0.willBeSubset()
                && !type0.isVertical();
        eligibility.put(font, eligible);
        return eligible;
    }

    /**
     * Whether the base font's GSUB substitutions leave the glyphs of {@code text} as its character
     * map gives them. A letter-spaced resource encodes through the character map alone, while
     * PDFBox shapes every word through the base font's GSUB worker when the base font is shown
     * directly; only text the substitutions leave alone draws the same glyphs both ways.
     *
     * <p>A face without GSUB data always qualifies, and so does every Latin face, whose GSUB
     * {@code PdfFontLoader} switches off. What this decides is the face FontBox keeps
     * substitutions for on behalf of another script: Poppins, whose GSUB serves Devanagari, draws
     * a Latin heading unchanged and qualifies for it, and draws a Devanagari conjunct differently
     * and does not.</p>
     */
    private boolean substitutionsLeaveGlyphsAlone(PDType0Font font, String text) {
        GsubData substitutions = font.getGsubData();
        if (substitutions == GsubData.NO_DATA_FOUND) {
            return true;
        }
        GsubWorker worker = gsubWorkers.computeIfAbsent(font,
                face -> new GsubWorkerFactory().getGsubWorker(face.getCmapLookup(), substitutions));
        int wordStart = 0;
        for (int index = 0; index <= text.length(); index++) {
            if (index == text.length() || separatesWords(text.charAt(index))) {
                if (!leftAlone(font, worker, text.substring(wordStart, index))) {
                    return false;
                }
                wordStart = index + 1;
            }
        }
        return true;
    }

    /**
     * Mirrors how PDFBox shows one word with a face that keeps substitutions: a lone whitespace
     * character is encoded directly, anything else is mapped to glyphs and shaped.
     */
    private static boolean leftAlone(PDType0Font font, GsubWorker worker, String word) {
        if (word.isEmpty() || (word.length() == 1 && Character.isWhitespace(word.charAt(0)))) {
            return true;
        }
        CmapLookup characterMap = font.getCmapLookup();
        List<Integer> glyphs = new ArrayList<>(word.length());
        for (int codePoint : word.codePoints().toArray()) {
            int glyph = characterMap.getGlyphId(codePoint);
            if (glyph <= 0) {
                return false;
            }
            glyphs.add(glyph);
        }
        return worker.applyTransforms(new ArrayList<>(glyphs)).equals(glyphs);
    }

    /** The characters PDFBox splits shown text on before shaping each word: what {@code \s} matches. */
    private static boolean separatesWords(char character) {
        return character == ' ' || character == '\t' || character == '\n'
                || character == 0x0B || character == '\f' || character == '\r';
    }

    private PdfTrackedFontView view(PDType0Font base, int extraPerMille) {
        Map<Integer, PdfTrackedFontView> byDelta = views.get(base);
        if (byDelta == null) {
            if (!registerForSubsetting(base)) {
                eligibility.put(base, false);
                return null;
            }
            byDelta = new HashMap<>();
            views.put(base, byDelta);
        }
        return byDelta.computeIfAbsent(extraPerMille, delta -> new PdfTrackedFontView(base, delta));
    }

    /**
     * PDFBox subsets only the fonts that were set on a content stream of the document, and a base
     * drawn exclusively through its letter-spaced resources never is. Setting it once on a
     * throwaway stream queues it for subsetting without writing anything to a page.
     */
    private boolean registerForSubsetting(PDType0Font base) {
        PDAppearanceStream scratch = new PDAppearanceStream(document);
        scratch.setResources(new PDResources());
        try (PDPageContentStream stream =
                     new PDPageContentStream(document, scratch, OutputStream.nullOutputStream())) {
            stream.setFont(base, 1f);
            return true;
        } catch (IOException e) {
            LOG.warn("render.pdf.letterSpacing.fallback font={} reason=subsetRegistration", base.getName(), e);
            return false;
        }
    }
}
