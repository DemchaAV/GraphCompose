package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders one line of a script through one bundled family, and reads back whether its
 * glyphs actually reached the page.
 *
 * <p>The failure this exists for is quiet. A code point the resolved font cannot encode is
 * replaced with {@code '?'} and logged at WARN, so a document set in the wrong family still
 * renders, still passes a size-and-header check, and still opens — as rows of question
 * marks.</p>
 */
final class ScriptGlyphDemo {

    static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);

    private ScriptGlyphDemo() {
    }

    /** Renders {@code text} in {@code fontName} to a single-paragraph page at {@code output}. */
    static void render(Path output, FontName fontName, String text) throws Exception {
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

    /**
     * Asserts the script actually reached the page, by counting the extracted code points
     * that fall in its Unicode block.
     *
     * <p>{@link #assertNoGlyphSubstitution} on its own is not enough: it fails only when
     * something <em>was</em> drawn and drawn wrongly. A render that dropped the paragraph,
     * emitted an empty text object, or wrote no text at all contains no {@code '?'} either,
     * and would sail past a size-and-header check on a page that is simply blank. Keying on
     * the presence of the script is what makes these demos fail closed.</p>
     *
     * <p>Extraction reads the content stream, so a right-to-left line comes back reversed;
     * a count is indifferent to that. Arabic comes back as base letters rather than the
     * presentation forms actually drawn, because the backend writes a {@code ToUnicode}
     * map — so the block to pass here is always the one the source text is written in.</p>
     */
    static void assertScriptOnPage(Path output, int first, int last, int minimumCodePoints)
            throws Exception {
        try (PDDocument pdf = Loader.loadPDF(output.toFile())) {
            String extracted = new PDFTextStripper().getText(pdf);
            long found = extracted.codePoints()
                    .filter(codePoint -> codePoint >= first && codePoint <= last)
                    .count();
            assertThat(found)
                    .describedAs("the page must carry at least %d code points in "
                            + "U+%04X..U+%04X; extracted text was \"%s\"",
                            minimumCodePoints, first, last, extracted.trim())
                    .isGreaterThanOrEqualTo(minimumCodePoints);
        }
    }

    static void assertNoGlyphSubstitution(Path output) throws Exception {
        try (PDDocument pdf = Loader.loadPDF(output.toFile())) {
            String extracted = new PDFTextStripper().getText(pdf);
            assertThat(extracted)
                    .describedAs("a '?' in the extracted text means the family could not encode "
                            + "the script and every glyph was substituted")
                    .doesNotContain("?");
        }
    }

    static void assertValidPdf(Path output) throws Exception {
        byte[] bytes = Files.readAllBytes(output);
        // 500 bytes catches a truncated / empty render without over-specifying real PDF
        // body size (PDFBox can emit a valid single-paragraph PDF in under 1 KB).
        assertThat(bytes)
                .describedAs("PDF should be a non-empty, reasonably-sized file")
                .hasSizeGreaterThan(500);
        assertThat(new String(bytes, 0, 5, StandardCharsets.US_ASCII))
                .describedAs("PDF must start with the %PDF- magic header")
                .isEqualTo("%PDF-");
    }
}
