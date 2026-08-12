package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.testing.VisualTestOutputs;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders Arabic and Hebrew through the bundled families added in {@code graph-compose-fonts}
 * 1.1.0 and proves the glyphs actually reached the page.
 *
 * <p>The failure this guards against is quiet. A code point the font cannot encode is
 * replaced with {@code '?'} and logged at WARN, so a document with the wrong family still
 * renders, still passes a size-and-header check, and still opens — as rows of question
 * marks. Extraction order for right-to-left text is heuristic and deliberately not asserted
 * here; the presence of a substitution marker is not, which is what makes it a usable
 * assertion for this.</p>
 *
 * <p>Scope note: at this stage text is laid out in logical order — this test says the
 * glyphs exist and render, not that they are ordered or joined.</p>
 */
class ArabicHebrewGlyphsDemoTest {

    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);

    private static final String ARABIC = "مرحبا بالعالم";
    private static final String HEBREW = "שלום עולם";

    @Test
    void arabicRendersThroughTheBundledArabicFamily() throws Exception {
        Path output = VisualTestOutputs.preparePdf("arabic-amiri", "rtl-fonts");

        render(output, FontName.AMIRI, ARABIC + " — Amiri 2026");

        assertValidPdf(output);
        assertNoGlyphSubstitution(output);
    }

    @Test
    void hebrewRendersThroughTheBundledHebrewFamily() throws Exception {
        Path output = VisualTestOutputs.preparePdf("hebrew-davidlibre", "rtl-fonts");

        render(output, FontName.DAVID_LIBRE, HEBREW + " — David Libre 2026");

        assertValidPdf(output);
        assertNoGlyphSubstitution(output);
    }

    @Test
    void aRightToLeftParagraphReadsInItsOwnDirection() throws Exception {
        Path output = VisualTestOutputs.preparePdf("hebrew-rtl-paragraph", "rtl-fonts");

        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {

            DocumentTextStyle style = DocumentTextStyle.builder()
                    .fontName(FontName.DAVID_LIBRE)
                    .size(20)
                    .color(INK)
                    .build();

            document.pageFlow(page -> page
                    .addParagraph(p -> p.text(HEBREW).textStyle(style))
                    .addParagraph(p -> p
                            .text(HEBREW)
                            .direction(TextDirection.RTL)
                            .textStyle(style))
                    .addParagraph(p -> p
                            .text(HEBREW + " GraphCompose 2026 " + HEBREW)
                            .direction(TextDirection.RTL)
                            .textStyle(style)));

            Files.write(output, document.toPdfBytes());
        }

        assertValidPdf(output);
        assertNoGlyphSubstitution(output);
    }

    private static void render(Path output, FontName fontName, String text) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {

            document.pageFlow(page -> page
                    .addParagraph(p -> p
                            .text(text)
                            .textStyle(DocumentTextStyle.builder()
                                    .fontName(fontName)
                                    .size(18)
                                    .color(INK)
                                    .build())));

            Files.write(output, document.toPdfBytes());
        }
    }

    private static void assertNoGlyphSubstitution(Path output) throws Exception {
        try (PDDocument pdf = Loader.loadPDF(output.toFile())) {
            String extracted = new PDFTextStripper().getText(pdf);
            assertThat(extracted)
                    .describedAs("a '?' in the extracted text means the family could not encode "
                            + "the script and every glyph was substituted")
                    .doesNotContain("?");
        }
    }

    private static void assertValidPdf(Path output) throws Exception {
        byte[] bytes = Files.readAllBytes(output);
        assertThat(bytes).hasSizeGreaterThan(500);
        assertThat(new String(bytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }
}
