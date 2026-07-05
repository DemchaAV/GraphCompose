package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.output.DocumentDebugOptions;
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
        String text = extractText(render(DocumentDebugOptions.nodeLabels(), "PriceSummary"));

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
                DocumentDebugOptions.nodeLabels().withLabelText(DocumentDebugOptions.LabelText.FULL_PATH),
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
        String text = extractText(render(DocumentDebugOptions.guides(), "PriceSummary"));

        assertThat(text).doesNotContain("ParagraphNode[1]");
        assertThat(text).doesNotContain("PriceSummaryTitle[0]");
    }

    @Test
    void guideLinesToggleAfterDebugKeepsLabelSettings() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(340, 260)
                .margin(DocumentInsets.of(18))
                .create()) {
            document.debug(DocumentDebugOptions.nodeLabels());
            document.guideLines(true);
            document.pageFlow(page -> page.module("PriceSummary",
                    module -> module.paragraph("Body copy")));

            String text = extractText(document.toPdfBytes());
            assertThat(text).contains("ParagraphNode[1]");
        }
    }

    @Test
    void winAnsiEncodableAccentsSurviveInLabels() throws Exception {
        String text = extractText(render(DocumentDebugOptions.nodeLabels(), "Résumé"));

        // é is WinAnsi-encodable, so the shared GlyphFallbackLogger
        // degradation keeps it intact instead of mangling it to '?'.
        assertThat(text).contains("RésuméTitle[0]");
        assertThat(text).doesNotContain("R?sum?Title[0]");
    }

    @Test
    void builderDebugAfterGuideLinesReplacesTheWholeConfig() throws Exception {
        // Last-write-wins on the GraphCompose builder, same as the session:
        // debug(none()) after guideLines(true) disables everything, so the
        // bytes match a render that never enabled debug at all.
        byte[] disabled;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(340, 260)
                .margin(DocumentInsets.of(18))
                .guideLines(true)
                .debug(DocumentDebugOptions.none())
                .create()) {
            document.pageFlow(page -> page.module("PriceSummary",
                    module -> module.paragraph("Body copy for the overlay test")));
            disabled = document.toPdfBytes();
        }
        byte[] plain = render(null, "PriceSummary");

        assertThat(disabled).hasSameSizeAs(plain);
    }

    @Test
    void builderGuideLinesAfterDebugPreservesLabelSettings() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(340, 260)
                .margin(DocumentInsets.of(18))
                .debug(DocumentDebugOptions.nodeLabels())
                .guideLines(true)
                .create()) {
            document.pageFlow(page -> page.module("PriceSummary",
                    module -> module.paragraph("Body copy")));

            String text = extractText(document.toPdfBytes());
            assertThat(text).contains("ParagraphNode[1]");
        }
    }

    @Test
    void splitOwnersAreLabelledOnEveryPageTheyTouch() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(220, 120)
                .margin(DocumentInsets.of(16))
                .debug(DocumentDebugOptions.nodeLabels())
                .create()) {
            document.pageFlow(page -> page.module("LongStory",
                    module -> module.paragraph("flow ".repeat(160))));

            byte[] pdf = document.toPdfBytes();
            try (PDDocument loaded = Loader.loadPDF(pdf)) {
                assertThat(loaded.getNumberOfPages()).isGreaterThan(1);
                PDFTextStripper firstPage = new PDFTextStripper();
                firstPage.setStartPage(1);
                firstPage.setEndPage(1);
                PDFTextStripper secondPage = new PDFTextStripper();
                secondPage.setStartPage(2);
                secondPage.setEndPage(2);
                // The split paragraph's owner gets one badge on each page it
                // touches, not just on the page of its first fragment.
                assertThat(firstPage.getText(loaded)).contains("ParagraphNode[1]");
                assertThat(secondPage.getText(loaded)).contains("ParagraphNode[1]");
            }
        }
    }

    @Test
    void nonWinAnsiNamesDegradeToPlaceholders() throws Exception {
        String text = extractText(render(
                DocumentDebugOptions.nodeLabels().withLabelText(DocumentDebugOptions.LabelText.FULL_PATH),
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
        byte[] explicitNone = render(DocumentDebugOptions.none(), "PriceSummary");

        assertThat(new String(plain, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(new String(explicitNone, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        // PDFBox stamps a fresh /ID on every save, so byte-for-byte equality
        // is impossible; identical length proves the overlay emitted nothing.
        assertThat(explicitNone).hasSameSizeAs(plain);
    }

    private static byte[] render(DocumentDebugOptions debug, String moduleName) throws Exception {
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
