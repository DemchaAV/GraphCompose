package com.demcha.compose.document.backend.fixed.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a produced document says its own text is, to a reader that is not this engine.
 *
 * <p>Two transformations stand between what an author wrote and what reaches the page.
 * Arabic is shaped into presentation forms, because {@code showText} walks the font's cmap
 * and never runs the substitutions that would join the letters; and every right-to-left run
 * is reversed, because the page is painted left to right. Both are about drawing. Neither is
 * something a reader searching the document, or a screen reader, or an indexer should ever
 * see — they want the word as it was typed.</p>
 *
 * <p>What carries that intent across is the font's {@code ToUnicode} map: for each glyph in
 * the file, what character it stands for. This holds the engine to both halves of that —
 * what extraction returns, and what the map itself says, which are not the same question and
 * currently do not have the same answer.</p>
 */
class PdfTextExtractionTest {

    /** Five letters that all join, so every one of them is drawn as a contextual form. */
    private static final String ARABIC = "مرحبا";

    /** Four letters that never join, so only the reversal stands between writing and page. */
    private static final String HEBREW = "שלום";

    @Test
    void anArabicWordComesBackOutAsTheWordThatWasWritten() throws Exception {
        // What a reader gets is the contract: the letters as typed, in the order typed,
        // not the joined forms the page is painted from and not back to front.
        assertThat(extractedText(ARABIC, FontName.AMIRI)).isEqualTo(ARABIC);
    }

    @Test
    void aHebrewWordComesBackOutAsTheWordThatWasWritten() throws Exception {
        assertThat(extractedText(HEBREW, FontName.DAVID_LIBRE)).isEqualTo(HEBREW);
    }

    @Test
    void theHebrewGlyphMapNamesTheLettersThemselves() throws Exception {
        // Hebrew is only reversed, never shaped, so each glyph in the file stands for the
        // letter it is. This is the shape the Arabic map should have and does not, which is
        // what identifies the difference below as being about shaping rather than about
        // right-to-left.
        assertThat(glyphMeanings(HEBREW, FontName.DAVID_LIBRE))
                .isNotEmpty()
                .allSatisfy(codePoint -> assertThat(codePoint)
                        .describedAs("U+%04X should be a Hebrew letter", codePoint)
                        .isBetween(0x0590, 0x05FF));
    }

    /**
     * Pins a known limit rather than a wanted behaviour.
     *
     * <p>The map is built by the font subsetter from the characters actually shown, and
     * what is shown is the shaped form — so the file says a glyph means U+FE8E, the final
     * form of alef, where the author wrote U+0627, alef. A reader that normalises recovers
     * the letter, which is why the extraction above passes: PDFBox's own extractor applies
     * a compatibility normalisation and re-derives the reading order with the bidirectional
     * algorithm. A reader that does not, does not — and the search that finds nothing gives
     * no sign of why.</p>
     *
     * <p>This is asserted so the day it changes is a deliberate day. The engine already
     * knows how to undo the shaping — {@code ArabicShaper.toBaseLetters} — and nothing on
     * the write path calls it.</p>
     */
    @Test
    void theArabicGlyphMapStillNamesTheShapedForms() throws Exception {
        assertThat(glyphMeanings(ARABIC, FontName.AMIRI))
                .isNotEmpty()
                .allSatisfy(codePoint -> assertThat(codePoint)
                        .describedAs("U+%04X is a presentation form; a reader that does not "
                                + "normalise sees this rather than the letter", codePoint)
                        .isBetween(0xFE70, 0xFEFC));
    }

    /** The text a reader gets back, which is what the two transformations must not reach. */
    private static String extractedText(String text, FontName font) throws IOException {
        try (PDDocument document = Loader.loadPDF(render(text, font))) {
            return new PDFTextStripper().getText(document).trim();
        }
    }

    /**
     * Every character the page's fonts claim their glyphs stand for.
     *
     * <p>Read from the {@code ToUnicode} stream rather than through PDFBox's own lookup, so
     * what is measured is what the file says and not what a reader can reconstruct.</p>
     */
    private static List<Integer> glyphMeanings(String text, FontName font) throws IOException {
        List<Integer> meanings = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(render(text, font))) {
            PDResources resources = document.getPage(0).getResources();
            for (COSName name : resources.getFontNames()) {
                PDFont embedded = resources.getFont(name);
                COSBase raw = embedded.getCOSObject().getDictionaryObject(COSName.TO_UNICODE);
                assertThat(raw)
                        .describedAs("a font with no ToUnicode map leaves its text "
                                + "unextractable altogether")
                        .isInstanceOf(COSStream.class);
                try (InputStream in = ((COSStream) raw).createInputStream()) {
                    meanings.addAll(destinations(
                            new String(in.readAllBytes(), StandardCharsets.ISO_8859_1)));
                }
            }
        }
        return meanings;
    }

    /**
     * The destination code points of a CMap's {@code bfchar} and {@code bfrange} entries.
     *
     * <p>Both spell a destination as the last {@code <hhhh>} on the line, so one pattern
     * reads either: {@code <src> <dst>} and {@code <lo> <hi> <dst>}. Only the mapping
     * blocks are read — {@code codespacerange} declares which byte sequences are valid
     * codes rather than what any of them mean, and its {@code <0000> <FFFF>} would
     * otherwise read as a glyph standing for U+FFFF.</p>
     */
    private static List<Integer> destinations(String cmap) {
        Pattern block = Pattern.compile("begin(bfchar|bfrange)(.*?)end\\1", Pattern.DOTALL);
        Pattern entry = Pattern.compile("^(?:<[0-9A-Fa-f]{4}>\\s+){1,2}<([0-9A-Fa-f]{4,})>\\s*$",
                Pattern.MULTILINE);

        List<Integer> destinations = new ArrayList<>();
        Matcher blocks = block.matcher(cmap);
        while (blocks.find()) {
            Matcher entries = entry.matcher(blocks.group(2));
            while (entries.find()) {
                // A destination may be several code units — a ligature standing for the
                // letters it was made from. The first places it in a block.
                destinations.add(Integer.parseInt(entries.group(1).substring(0, 4), 16));
            }
        }
        return destinations;
    }

    private static byte[] render(String text, FontName font) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(300, 100)
                .margin(DocumentInsets.of(20))
                .create()) {

            document.pageFlow(page -> page.addParagraph(p -> p.text(text)
                    .direction(TextDirection.RTL)
                    .textStyle(DocumentTextStyle.builder().fontName(font).size(20).build())));

            return document.toPdfBytes();
        }
    }
}
