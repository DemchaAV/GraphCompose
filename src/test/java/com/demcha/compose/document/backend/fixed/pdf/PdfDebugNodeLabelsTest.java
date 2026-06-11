package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfDebugOptions;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration coverage for the node-label debug overlay: labels print the
 * owning node's semantic path, respect the configured text mode, stay off by
 * default, and degrade non-WinAnsi names to placeholders instead of failing
 * the render.
 */
class PdfDebugNodeLabelsTest {

    @Test
    void nodeLabelsPrintLeafSegmentByDefault() throws Exception {
        String text = extractText(render(PdfDebugOptions.nodeLabels(), "PriceSummary"));

        // The module title is an auto-named paragraph at child index 0; the
        // body paragraph follows at index 1.
        assertThat(text).contains("PriceSummaryTitle[0]");
        assertThat(text).contains("ParagraphNode[1]");
        // NAME mode prints only the owner's own segment, never the ancestry.
        assertThat(text).doesNotContain("PriceSummary[0]/");
    }

    @Test
    void fullPathLabelsIncludeAncestry() throws Exception {
        String text = extractText(render(
                PdfDebugOptions.nodeLabels().withLabelText(PdfDebugOptions.LabelText.FULL_PATH),
                "PriceSummary"));

        assertThat(text).contains("PriceSummary[0]");
        assertThat(text).contains("/ParagraphNode[1]");
    }

    @Test
    void labelsStayOffByDefault() throws Exception {
        String text = extractText(render(null, "PriceSummary"));

        assertThat(text).doesNotContain("ParagraphNode[1]");
        assertThat(text).doesNotContain("PriceSummaryTitle[0]");
    }

    @Test
    void guideOverlayAloneDrawsNoLabels() throws Exception {
        String text = extractText(render(PdfDebugOptions.guides(), "PriceSummary"));

        assertThat(text).doesNotContain("ParagraphNode[1]");
        assertThat(text).doesNotContain("PriceSummaryTitle[0]");
    }

    @Test
    void guideLinesToggleAfterDebugKeepsLabelSettings() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(340, 260)
                .margin(DocumentInsets.of(18))
                .create()) {
            document.debug(PdfDebugOptions.nodeLabels());
            document.guideLines(true);
            document.pageFlow(page -> page.module("PriceSummary",
                    module -> module.paragraph("Body copy")));

            String text = extractText(document.toPdfBytes());
            assertThat(text).contains("ParagraphNode[1]");
        }
    }

    @Test
    void nonWinAnsiNamesDegradeToPlaceholders() throws Exception {
        String text = extractText(render(
                PdfDebugOptions.nodeLabels().withLabelText(PdfDebugOptions.LabelText.FULL_PATH),
                "Шапка"));

        // The five Cyrillic letters survive name normalization but exceed the
        // base-14 Helvetica WinAnsi range, so the label degrades to '?'
        // placeholders instead of throwing inside the debug render.
        assertThat(text).contains("?????[0]");
        assertThat(text).doesNotContain("Шапка[0]");
    }

    @Test
    void disabledDebugOptionsMatchTheDefaultRender() throws Exception {
        byte[] plain = render(null, "PriceSummary");
        byte[] explicitNone = render(PdfDebugOptions.none(), "PriceSummary");

        assertThat(new String(plain, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(new String(explicitNone, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        // PDFBox stamps a fresh /ID on every save, so byte-for-byte equality
        // is impossible; identical length proves the overlay emitted nothing.
        assertThat(explicitNone).hasSameSizeAs(plain);
    }

    private static byte[] render(PdfDebugOptions debug, String moduleName) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(340, 260)
                .margin(DocumentInsets.of(18))
                .debug(debug)
                .create()) {
            document.pageFlow(page -> page.module(moduleName,
                    module -> module.paragraph("Body copy for the overlay test")));
            return document.toPdfBytes();
        }
    }

    private static String extractText(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }
}
