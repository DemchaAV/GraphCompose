package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentWatermark;
import com.demcha.compose.document.output.DocumentWatermarkLayer;
import com.demcha.compose.document.output.DocumentWatermarkPosition;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;
import com.demcha.testing.VisualTestOutputs;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end regression for R1 glyph sanitizer. Renders documents that
 * intentionally contain code points Helvetica's WinAnsi encoding cannot
 * cover — arrows ({@code U+2192}), bullets ({@code U+25CF}), emoji
 * ({@code U+1F389}), white circles ({@code U+25E6}), copyright marks —
 * through every PDF text render seam wired by R1.b and R1.c and
 * asserts no {@link IllegalArgumentException} escapes from PDFBox.
 *
 * <p>Pre-R1 every test in this class would crash with
 * {@code "U+2192 ('arrowright') is not available in the font Helvetica,
 * encoding: WinAnsiEncoding"} or an equivalent message. After R1 the
 * substituted glyphs render as {@code '?'} and the document completes
 * successfully — verified here by writing real PDFs to
 * {@code target/visual-tests/glyph-fallback/} and asserting on file
 * size and the {@code %PDF-} header.</p>
 *
 * @author Artem Demchyshyn
 */
class UnicodeFallbackDemoTest {

    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(112, 116, 128);

    @Test
    void paragraphWithUnsupportedGlyphsRendersWithoutCrash() throws Exception {
        Path output = VisualTestOutputs.preparePdf("paragraph", "glyph-fallback");

        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .pageBackground(THEME.pageBackground())
                .margin(DocumentInsets.of(36))
                .create()) {

            document.pageFlow(page -> page
                    .addSection("Unicode", section -> section
                            .addParagraph(p -> p
                                    .text("R1 sanitiser: arrows → bullets ● white-circles ◦ "
                                            + "small-squares ▪ middle-dots · emoji 🎉 © ™ — none crash.")
                                    .textStyle(body()))));

            Files.write(output, document.toPdfBytes());
        }

        assertValidPdf(output);
    }

    @Test
    void tableWithUnsupportedGlyphsRendersWithoutCrash() throws Exception {
        Path output = VisualTestOutputs.preparePdf("table", "glyph-fallback");

        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .pageBackground(THEME.pageBackground())
                .margin(DocumentInsets.of(36))
                .create()) {

            document.pageFlow(page -> page
                    .addTable(table -> table
                            .columns(
                                    DocumentTableColumn.fixed(120),
                                    DocumentTableColumn.fixed(120),
                                    DocumentTableColumn.fixed(120))
                            .headerRow("Symbol →", "Code", "Notes ●")
                            .row("● bullet", "U+25CF", "Sanitised → ?")
                            .row("🎉 party", "U+1F389", "Emoji → ?")));

            Files.write(output, document.toPdfBytes());
        }

        assertValidPdf(output);
    }

    @Test
    void watermarkWithUnsupportedGlyphRendersWithoutCrash() throws Exception {
        Path output = VisualTestOutputs.preparePdf("watermark", "glyph-fallback");

        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .pageBackground(THEME.pageBackground())
                .margin(DocumentInsets.of(36))
                .create()) {

            document.watermark(DocumentWatermark.builder()
                    .text("DRAFT → © 2026 ●")
                    .fontSize(72f)
                    .rotation(45f)
                    .color(DocumentColor.rgb(180, 60, 60))
                    .opacity(0.15f)
                    .layer(DocumentWatermarkLayer.BEHIND_CONTENT)
                    .position(DocumentWatermarkPosition.CENTER)
                    .build());

            document.pageFlow(page -> page
                    .addParagraph(p -> p
                            .text("Page body underneath a watermark with unsupported glyphs.")
                            .textStyle(body())));

            Files.write(output, document.toPdfBytes());
        }

        assertValidPdf(output);
    }

    @Test
    void headerFooterWithUnsupportedGlyphRendersWithoutCrash() throws Exception {
        Path output = VisualTestOutputs.preparePdf("header-footer", "glyph-fallback");

        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .pageBackground(THEME.pageBackground())
                .margin(48, 34, 48, 34)
                .create()) {

            document.header(DocumentHeaderFooter.builder()
                    .zone(DocumentHeaderFooterZone.HEADER)
                    .leftText("GraphCompose ● Demo")
                    .rightText("→ Page {page}")
                    .fontSize(9f)
                    .textColor(MUTED)
                    .showSeparator(true)
                    .build());

            document.footer(DocumentHeaderFooter.builder()
                    .zone(DocumentHeaderFooterZone.FOOTER)
                    .centerText("© 2026 ◦ Confidential 🎉")
                    .fontSize(9f)
                    .textColor(MUTED)
                    .showSeparator(true)
                    .build());

            document.pageFlow(page -> page
                    .addParagraph(p -> p
                            .text("Body content. Header and footer above/below contain unsupported glyphs.")
                            .textStyle(body())));

            Files.write(output, document.toPdfBytes());
        }

        assertValidPdf(output);
    }

    private static DocumentTextStyle body() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(11)
                .color(INK)
                .build();
    }

    private static void assertValidPdf(Path output) throws Exception {
        byte[] bytes = Files.readAllBytes(output);
        // 500 bytes catches a truncated / empty render without
        // over-specifying real PDF body size (PDFBox can emit a valid
        // single-paragraph PDF in under 1 KB).
        assertThat(bytes)
                .describedAs("PDF should be a non-empty, reasonably-sized file")
                .hasSizeGreaterThan(500);
        assertThat(new String(bytes, 0, 5, StandardCharsets.US_ASCII))
                .describedAs("PDF must start with the %PDF- magic header")
                .isEqualTo("%PDF-");
    }
}
