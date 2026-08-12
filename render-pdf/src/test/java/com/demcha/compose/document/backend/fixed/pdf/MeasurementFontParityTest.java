package com.demcha.compose.document.backend.fixed.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.DefaultFonts;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.font.FontName;

import java.awt.Color;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

/**
 * Guards Finding 4 (measurement no longer embeds binary fonts into a throwaway
 * document).
 *
 * <p>Measurement resolves binary families to a per-thread cached, document-free
 * {@code PDType0Font}; the render path embeds a fresh subset into the saved
 * document. Both must report <b>byte-identical</b> glyph widths — they read the
 * same parsed {@code TrueTypeFont}, so any drift here would silently move layout
 * geometry. This is the permanent CI counterpart to the manual
 * {@code FontEmbedProbe} width-parity check in the benchmarks module.</p>
 */
class MeasurementFontParityTest {

    /**
     * Every bundled binary (Google) family — the ones that actually embed. Read off the
     * catalog rather than listed here, so a family added to it is covered on the day it
     * lands instead of on the day someone remembers this file.
     */
    private static final List<FontName> BINARY_FAMILIES = DefaultFonts.googleFamilies().stream()
            .map(FontFamilyDefinition::name)
            .toList();

    private static final List<TextDecoration> FACES = List.of(
            TextDecoration.DEFAULT, TextDecoration.BOLD, TextDecoration.ITALIC, TextDecoration.BOLD_ITALIC);

    private static final List<String> STRINGS = List.of(
            "The quick brown fox jumps over the lazy dog WAVE AVA To.",
            "Em dash — “smart quotes”  nbsp",          // standard sanitize cleanup
            "Arrows → bullet ● emoji 😀 fallback");    // unencodable code points -> '?'

    @Test
    void measurementWidthsMatchRenderWidthsForEveryBinaryFamily() throws Exception {
        // The list is derived, and the only assertion below sits inside the loop over it:
        // an empty derivation would turn the whole guard into a green no-op.
        assertThat(BINARY_FAMILIES).hasSizeGreaterThanOrEqualTo(32);

        try (PDDocument renderDocument = new PDDocument();
             PdfMeasurementResources measurement = PdfMeasurementResources.open(List.of())) {
            // Exactly what PdfFixedLayoutBackend builds: a render library that embeds
            // a fresh subset into the (saved) render document.
            FontLibrary renderLibrary = PdfFontLibraryFactory.library(renderDocument, List.of());

            for (FontName family : BINARY_FAMILIES) {
                PdfFont renderFont = renderLibrary.getFont(family, PdfFont.class)
                        .orElseThrow(() -> new AssertionError("render font missing for " + family));
                for (TextDecoration face : FACES) {
                    for (String text : STRINGS) {
                        TextStyle style = new TextStyle(family, 11.0, face, Color.BLACK);
                        double renderWidth = renderFont.getTextWidth(style, text);
                        double measurementWidth = measurement.textMeasurementSystem().textWidth(style, text);

                        assertThat(measurementWidth)
                                .describedAs("measurement vs render width parity: %s / %s / \"%s\"", family, face, text)
                                .isEqualTo(renderWidth);
                    }
                }
            }
        }
    }

    @Test
    void measurementLibraryResolvesBinaryFamiliesWithoutOwningDocument() {
        // F4 contract: a measurement library embeds nothing into a document and so
        // needs none to resolve a binary family.
        FontLibrary measurementLibrary = PdfFontLibraryFactory.measurementLibrary(List.of());

        assertThat(measurementLibrary.getFont(FontName.LATO, PdfFont.class))
                .describedAs("binary family resolves through the document-free measurement library")
                .isPresent();
    }
}
