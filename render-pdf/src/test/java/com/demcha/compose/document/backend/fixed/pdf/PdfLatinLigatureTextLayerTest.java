package com.demcha.compose.document.backend.fixed.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.font.DefaultFonts;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;

import org.apache.fontbox.ttf.model.GsubData;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * What a bundled Latin family says its own text is, to a reader that is not this engine.
 *
 * <p>PDFBox runs a font's {@code GSUB} substitutions on the engine's behalf, and most of
 * the bundled families define ligatures over the commonest letter pairs in English —
 * {@code ti}, {@code tf}, {@code ft}. Each pair drawn that way becomes one glyph, and the
 * {@code ToUnicode} map that says what a glyph means is built by reading the font's
 * character map backwards, where a ligature is reachable from no character at all. The
 * entry is therefore absent and both letters are lost on extraction: {@code Platform}
 * comes back as {@code Pla orm}. Nothing about the rendered page shows it.</p>
 *
 * <p>Which makes extraction the only place it can be caught, and the reason this asks for
 * the whole sentence back rather than for the presence of a word: a test that looked for
 * {@code Pla} would pass on a broken file. The probe is built from the pairs that break —
 * a family that stops substituting for some other reason still has to return them.</p>
 */
class PdfLatinLigatureTextLayerTest {

    /**
     * A sentence of ordinary English words, chosen so that every one of them carries a
     * pair the bundled families ligate. Kept to one line at the size rendered, because a
     * wrap would put a line break into the extracted text and say nothing about glyphs.
     */
    private static final String PROBE =
            "Platform certification retired after fifteen notification drafts";

    /** The letter pairs the bundled families draw as one glyph. */
    private static final List<String> LIGATED_PAIRS = List.of("ti", "tf", "ft", "fi");

    /** The binary families — the standard-14 are Type 1 and substitute nothing. */
    private static final List<FontName> FAMILIES = DefaultFonts.googleFamilies().stream()
            .map(FontFamilyDefinition::name)
            .toList();

    @Test
    void theProbeCarriesEveryPairThatGoesMissing() {
        // The rest of this class is only as strong as the sentence it asks about, and a
        // sentence quietly edited into one without ligature pairs would leave every
        // assertion below passing against a file that still loses text.
        assertThat(LIGATED_PAIRS)
                .allSatisfy(pair -> assertThat(PROBE)
                        .describedAs("the probe has to contain %s to say anything about it", pair)
                        .contains(pair));
    }

    @Test
    void everyBundledFamilyDrawsTextThatComesBackOutAsWritten() throws Exception {
        List<String> mangled = new ArrayList<>();
        for (FontName family : FAMILIES) {
            String extracted = extractedText(PROBE, family);
            if (!PROBE.equals(extracted)) {
                mangled.add(family + ": \"" + extracted + "\"");
            }
        }

        assertThat(mangled)
                .describedAs("a page drawn with these families lost letters on extraction, "
                        + "which is what a search box, a copy-and-paste and an applicant "
                        + "tracking system all read")
                .isEmpty();
    }

    @Test
    void aLatinFaceIsHandedToPdfboxWithNothingToSubstitute() {
        // The mechanism, not its symptom: PDFBox substitutes whenever the face it is given
        // reports substitutions for the Latin script, so the fix is that the face reports
        // none. Held separately because a family could stop losing letters for an
        // unrelated reason — a ligature that happened to gain a code point of its own —
        // and the page would still be drawn as shapes the engine never measured.
        assertThat(substitutionsOf(FontName.LATO))
                .describedAs("a Latin face still carrying substitutions will be drawn "
                        + "as ligatures again the moment it is made current")
                .isSameAs(GsubData.NO_DATA_FOUND);
    }

    @Test
    void aFaceThatNeedsItsSubstitutionsToRenderKeepsThem() {
        // Poppins carries Devanagari, and PDFBox shapes that script through the same
        // mechanism. There the substitutions are how the script renders rather than a
        // flourish on top of it, so silencing Latin must not reach them.
        assertThat(substitutionsOf(FontName.POPPINS))
                .describedAs("a face whose script is shaped by substitution cannot lose it")
                .isNotSameAs(GsubData.NO_DATA_FOUND);
    }

    /** What the face a family resolves to reports it would substitute. */
    private static GsubData substitutionsOf(FontName family) {
        PDFont face = FontCoverageProbe.face(family, TextDecoration.DEFAULT);
        assertThat(face).isInstanceOf(PDType0Font.class);
        return ((PDType0Font) face).getGsubData();
    }

    /** The text a reader gets back from a page drawn in one family. */
    private static String extractedText(String text, FontName family) throws IOException {
        try (PDDocument document = Loader.loadPDF(render(text, family))) {
            return new PDFTextStripper().getText(document).trim();
        }
    }

    private static byte[] render(String text, FontName family) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(600, 100)
                .margin(DocumentInsets.of(20))
                .create()) {

            document.pageFlow(page -> page.addParagraph(p -> p.text(text)
                    .textStyle(DocumentTextStyle.builder().fontName(family).size(11).build())));

            return document.toPdfBytes();
        }
    }
}
