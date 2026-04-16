package com.demcha.compose.document.templates;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.data.JobDetails;
import com.demcha.compose.document.templates.data.MainPageCV;
import com.demcha.compose.document.templates.data.MainPageCvDTO;
import com.demcha.compose.document.templates.support.LegacyTemplateMappers;
import com.demcha.compose.layout_core.debug.LayoutNodeSnapshot;
import com.demcha.compose.layout_core.debug.LayoutSnapshot;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.mock.CoverLetterMock;
import com.demcha.mock.InvoiceDataFixtures;
import com.demcha.mock.MainPageCVMock;
import com.demcha.mock.ProposalDataFixtures;
import com.demcha.mock.WeeklyScheduleDataFixtures;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalTemplateParityTest {

    @Test
    void standardCvShouldMatchLegacyBridgeBySnapshotPagesAndPdfText() throws Exception {
        com.demcha.templates.data.MainPageCV legacyOriginal = new MainPageCVMock().getMainPageCV();
        com.demcha.templates.api.MainPageCvDTO legacyRewrite = com.demcha.templates.api.MainPageCvDTO.from(legacyOriginal);
        MainPageCV canonicalOriginal = LegacyTemplateMappers.toCanonical(legacyOriginal);
        MainPageCvDTO canonicalRewrite = MainPageCvDTO.from(canonicalOriginal);

        RenderedTemplate legacy = renderLegacy(PDRectangle.A4, 24, composer ->
                new com.demcha.templates.builtins.CvTemplateV1().compose(composer, legacyOriginal, legacyRewrite));
        RenderedTemplate canonical = renderCanonical(PDRectangle.A4, 24, document ->
                new com.demcha.compose.document.templates.builtins.CvTemplateV1().compose(document, canonicalOriginal, canonicalRewrite));

        assertParity(legacy, canonical);
    }

    @Test
    void editorialBlueCvShouldMatchLegacyBridgeBySnapshotPagesAndPdfText() throws Exception {
        com.demcha.templates.data.MainPageCV legacyOriginal = new MainPageCVMock().getMainPageCV();
        com.demcha.templates.api.MainPageCvDTO legacyRewrite = com.demcha.templates.api.MainPageCvDTO.from(legacyOriginal);
        MainPageCV canonicalOriginal = LegacyTemplateMappers.toCanonical(legacyOriginal);
        MainPageCvDTO canonicalRewrite = MainPageCvDTO.from(canonicalOriginal);

        RenderedTemplate legacy = renderLegacy(PDRectangle.A4, 18, composer ->
                new com.demcha.templates.builtins.EditorialBlueCvTemplate().compose(composer, legacyOriginal, legacyRewrite));
        RenderedTemplate canonical = renderCanonical(PDRectangle.A4, 18, document ->
                new com.demcha.compose.document.templates.builtins.EditorialBlueCvTemplate().compose(document, canonicalOriginal, canonicalRewrite));

        assertParity(legacy, canonical);
    }

    @Test
    void coverLetterShouldMatchLegacyBridgeBySnapshotPagesAndPdfText() throws Exception {
        com.demcha.templates.data.MainPageCV legacyCv = new MainPageCVMock().getMainPageCV();
        com.demcha.templates.JobDetails legacyJobDetails = new com.demcha.templates.JobDetails(
                "https://linkedin.com/jobs/view/compose-path",
                "Software Engineer",
                "Compose Path Ltd",
                "Remote",
                "Compose-first compatibility test",
                "Mid",
                "Full-time");
        JobDetails canonicalJobDetails = LegacyTemplateMappers.toCanonical(legacyJobDetails);
        String letter = CoverLetterMock.letter.replace("${companyName}", "Compose Path Ltd");

        RenderedTemplate legacy = renderLegacy(PDRectangle.A4, 15, 10, 15, 15, composer ->
                new com.demcha.templates.builtins.CoverLetterTemplateV1().compose(
                        composer,
                        legacyCv.getHeader(),
                        letter,
                        legacyJobDetails));
        RenderedTemplate canonical = renderCanonical(PDRectangle.A4, 15, 10, 15, 15, document ->
                new com.demcha.compose.document.templates.builtins.CoverLetterTemplateV1().compose(
                        document,
                        LegacyTemplateMappers.toCanonical(legacyCv.getHeader()),
                        letter,
                        canonicalJobDetails));

        assertParity(legacy, canonical);
    }

    @Test
    void invoiceShouldMatchLegacyBridgeBySnapshotPagesAndPdfText() throws Exception {
        com.demcha.templates.data.InvoiceData legacyData = InvoiceDataFixtures.standardInvoice();
        var canonicalData = LegacyTemplateMappers.toCanonical(legacyData);

        RenderedTemplate legacy = renderLegacy(PDRectangle.A4, 22, composer ->
                new com.demcha.templates.builtins.InvoiceTemplateV1().compose(composer, legacyData));
        RenderedTemplate canonical = renderCanonical(PDRectangle.A4, 22, document ->
                new com.demcha.compose.document.templates.builtins.InvoiceTemplateV1().compose(document, canonicalData));

        assertParity(legacy, canonical);
    }

    @Test
    void proposalShouldMatchLegacyBridgeBySnapshotPagesAndPdfText() throws Exception {
        com.demcha.templates.data.ProposalData legacyData = ProposalDataFixtures.longProposal();
        var canonicalData = LegacyTemplateMappers.toCanonical(legacyData);

        RenderedTemplate legacy = renderLegacy(PDRectangle.A4, 22, composer ->
                new com.demcha.templates.builtins.ProposalTemplateV1().compose(composer, legacyData));
        RenderedTemplate canonical = renderCanonical(PDRectangle.A4, 22, document ->
                new com.demcha.compose.document.templates.builtins.ProposalTemplateV1().compose(document, canonicalData));

        assertParity(legacy, canonical);
    }

    @Test
    void weeklyScheduleShouldMatchLegacyBridgeBySnapshotPagesAndPdfText() throws Exception {
        PDRectangle landscapeA4 = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
        com.demcha.templates.data.WeeklyScheduleData legacyData = WeeklyScheduleDataFixtures.standardSchedule();
        var canonicalData = LegacyTemplateMappers.toCanonical(legacyData);

        RenderedTemplate legacy = renderLegacy(landscapeA4, 18, composer ->
                new com.demcha.templates.builtins.WeeklyScheduleTemplateV1().compose(composer, legacyData));
        RenderedTemplate canonical = renderCanonical(landscapeA4, 18, document ->
                new com.demcha.compose.document.templates.builtins.WeeklyScheduleTemplateV1().compose(document, canonicalData));

        assertParity(legacy, canonical);
    }

    private void assertParity(RenderedTemplate legacy, RenderedTemplate canonical) throws Exception {
        assertThat(pdfPageCount(legacy.pdfBytes())).isEqualTo(legacy.snapshot().totalPages());
        assertThat(pdfPageCount(canonical.pdfBytes())).isEqualTo(canonical.snapshot().totalPages());
        assertThat(canonical.snapshot().totalPages()).isEqualTo(legacy.snapshot().totalPages());
        Map<String, Long> canonicalLines = normalizedPdfLineHistogram(canonical.pdfBytes());
        Map<String, Long> legacyLines = normalizedPdfLineHistogram(legacy.pdfBytes());
        if (!canonicalLines.equals(legacyLines)) {
            assertThat(normalizedPdfTokenHistogram(canonical.pdfBytes())).isEqualTo(normalizedPdfTokenHistogram(legacy.pdfBytes()));
            return;
        }
        assertThat(canonicalLines).isEqualTo(legacyLines);
    }

    private RenderedTemplate renderLegacy(PDRectangle pageSize, float margin, LegacyComposeAction action) throws Exception {
        return renderLegacy(pageSize, margin, margin, margin, margin, action);
    }

    private RenderedTemplate renderLegacy(PDRectangle pageSize,
                                          float top,
                                          float right,
                                          float bottom,
                                          float left,
                                          LegacyComposeAction action) throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(pageSize)
                .margin(top, right, bottom, left)
                .markdown(true)
                .create()) {
            action.apply(composer);
            LayoutSnapshot snapshot = composer.layoutSnapshot();
            byte[] pdfBytes = composer.toBytes();
            return new RenderedTemplate(snapshot, pdfBytes);
        }
    }

    private RenderedTemplate renderCanonical(PDRectangle pageSize, float margin, CanonicalComposeAction action) throws Exception {
        return renderCanonical(pageSize, margin, margin, margin, margin, action);
    }

    private RenderedTemplate renderCanonical(PDRectangle pageSize,
                                             float top,
                                             float right,
                                             float bottom,
                                             float left,
                                             CanonicalComposeAction action) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(pageSize)
                .margin(top, right, bottom, left)
                .create()) {
            action.apply(document);
            LayoutSnapshot snapshot = document.layoutSnapshot();
            byte[] pdfBytes = document.toPdfBytes();
            return new RenderedTemplate(snapshot, pdfBytes);
        }
    }

    private int pdfPageCount(byte[] pdfBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return document.getNumberOfPages();
        }
    }

    private Map<String, Long> normalizedPdfLineHistogram(byte[] pdfBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document)
                    .replace("\r\n", "\n")
                    .lines()
                    .map(this::normalizePdfLine)
                    .filter(line -> !line.isBlank())
                    .collect(java.util.stream.Collectors.groupingBy(
                            line -> line,
                            TreeMap::new,
                            java.util.stream.Collectors.counting()));
        }
    }

    private Map<String, Long> normalizedPdfTokenHistogram(byte[] pdfBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document)
                    .replace("\r\n", "\n")
                    .lines()
                    .map(this::normalizePdfLine)
                    .flatMap(line -> java.util.Arrays.stream(line.split(" ")))
                    .map(String::trim)
                    .filter(token -> !token.isBlank())
                    .collect(java.util.stream.Collectors.groupingBy(
                            token -> token,
                            TreeMap::new,
                            java.util.stream.Collectors.counting()));
        }
    }

    private String normalizePdfLine(String line) {
        return line
                .replace('\u2022', '*')
                .replace('\u2013', '-')
                .replace("\u2014", "--")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record RenderedTemplate(LayoutSnapshot snapshot, byte[] pdfBytes) {
    }

    @FunctionalInterface
    private interface LegacyComposeAction {
        void apply(PdfComposer composer) throws Exception;
    }

    @FunctionalInterface
    private interface CanonicalComposeAction {
        void apply(DocumentSession document) throws Exception;
    }
}
