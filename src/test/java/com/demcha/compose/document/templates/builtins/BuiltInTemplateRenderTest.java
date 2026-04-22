package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.templates.TemplateTestSupport;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BuiltInTemplateRenderTest {

    @Test
    void shouldExposeCoverLetterTemplateMetadata() {
        CoverLetterTemplateV1 template = new CoverLetterTemplateV1();

        assertThat(template.getTemplateId()).isEqualTo("cover-letter-v1");
        assertThat(template.getTemplateName()).isEqualTo("Cover Letter V1");
        assertThat(template.getDescription()).isNotBlank();
    }

    @Test
    void shouldRenderCoverLetterAsDocument() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("cover_letter_render_document", "clean", "templates", "cover-letter");
        byte[] pdfBytes;

        try (var document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 15, 10, 15, 15)) {
            new CoverLetterTemplateV1().compose(
                    document,
                    TemplateTestSupport.canonicalHeader(),
                    TemplateTestSupport.coverLetter("Visual Test Company"),
                    TemplateTestSupport.jobDetails("Visual Test Company"));
            pdfBytes = document.toPdfBytes();
        }

        TemplateTestSupport.writePdf(outputFile, pdfBytes);
        TemplateTestSupport.assertPdfBytesLookValid(pdfBytes, 1);
        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
    }

    @Test
    void shouldRenderCoverLetterDirectlyToFile() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("cover_letter_render_file", "clean", "templates", "cover-letter");

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 15, 10, 15, 15, false)) {
            new CoverLetterTemplateV1().compose(
                    document,
                    TemplateTestSupport.canonicalHeader(),
                    TemplateTestSupport.coverLetter("Visual Test Company"),
                    TemplateTestSupport.jobDetails("Visual Test Company"));
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
    }

    @Test
    void shouldRenderCoverLetterDirectlyToFileWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("cover_letter_render_file_with_guide_lines", "guides", "templates", "cover-letter");

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 15, 10, 15, 15, true)) {
            new CoverLetterTemplateV1().compose(
                    document,
                    TemplateTestSupport.canonicalHeader(),
                    TemplateTestSupport.coverLetter("Visual Test Company"),
                    TemplateTestSupport.jobDetails("Visual Test Company"));
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
    }

    @Test
    void shouldMakeCoverLetterHeaderContactLinksClickable() throws Exception {
        var header = TemplateTestSupport.canonicalHeader();
        byte[] pdfBytes;

        try (var document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 15, 10, 15, 15)) {
            new CoverLetterTemplateV1().compose(
                    document,
                    header,
                    TemplateTestSupport.coverLetter("Visual Test Company"),
                    TemplateTestSupport.jobDetails("Visual Test Company"));
            pdfBytes = document.toPdfBytes();
        }

        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            List<String> uris = linkAnnotationUris(pdf);

            assertThat(uris).anySatisfy(uri -> assertThat(uri).startsWith("mailto:" + header.getEmail().getTo()));
            assertThat(uris).contains(header.getLinkedIn().getLinkUrl().getUrl());
            assertThat(uris).contains(header.getGitHub().getLinkUrl().getUrl());
        }
    }

    @Test
    void shouldExposeInvoiceTemplateMetadata() {
        InvoiceTemplateV1 template = new InvoiceTemplateV1();

        assertThat(template.getTemplateId()).isEqualTo("invoice-v1");
        assertThat(template.getTemplateName()).isEqualTo("Invoice V1");
        assertThat(template.getDescription()).isNotBlank();
    }

    @Test
    void shouldRenderInvoiceAsDocument() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("invoice_render_document", "clean", "templates", "invoice");
        byte[] pdfBytes;

        try (var document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 22, 22, 22, 22)) {
            new InvoiceTemplateV1().compose(document, TemplateTestSupport.canonicalInvoiceData());
            pdfBytes = document.toPdfBytes();
        }

        TemplateTestSupport.writePdf(outputFile, pdfBytes);
        TemplateTestSupport.assertPdfBytesLookValid(pdfBytes, 1);
        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
    }

    @Test
    void shouldRenderInvoiceDirectlyToFile() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("invoice_render_file", "clean", "templates", "invoice");

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 22, 22, 22, 22, false)) {
            new InvoiceTemplateV1().compose(document, TemplateTestSupport.canonicalInvoiceData());
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
    }

    @Test
    void shouldRenderInvoiceDirectlyToFileWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("invoice_render_file_with_guide_lines", "guides", "templates", "invoice");

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 22, 22, 22, 22, true)) {
            new InvoiceTemplateV1().compose(document, TemplateTestSupport.canonicalInvoiceData());
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
    }

    @Test
    void shouldExposeProposalTemplateMetadata() {
        ProposalTemplateV1 template = new ProposalTemplateV1();

        assertThat(template.getTemplateId()).isEqualTo("proposal-v1");
        assertThat(template.getTemplateName()).isEqualTo("Proposal V1");
        assertThat(template.getDescription()).isNotBlank();
    }

    @Test
    void shouldRenderProposalAsDocument() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("proposal_render_document", "clean", "templates", "proposal");
        byte[] pdfBytes;

        try (var document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 22, 22, 22, 22)) {
            new ProposalTemplateV1().compose(document, TemplateTestSupport.canonicalProposalData());
            pdfBytes = document.toPdfBytes();
        }

        TemplateTestSupport.writePdf(outputFile, pdfBytes);
        TemplateTestSupport.assertPdfBytesLookValid(pdfBytes, 2);
        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 2);
    }

    @Test
    void shouldRenderProposalDirectlyToFile() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("proposal_render_file", "clean", "templates", "proposal");

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 22, 22, 22, 22, false)) {
            new ProposalTemplateV1().compose(document, TemplateTestSupport.canonicalProposalData());
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 2);
    }

    @Test
    void shouldRenderProposalDirectlyToFileWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("proposal_render_file_with_guide_lines", "guides", "templates", "proposal");

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 22, 22, 22, 22, true)) {
            new ProposalTemplateV1().compose(document, TemplateTestSupport.canonicalProposalData());
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 2);
    }

    @Test
    void shouldExposeWeeklyScheduleTemplateMetadata() {
        WeeklyScheduleTemplateV1 template = new WeeklyScheduleTemplateV1();

        assertThat(template.getTemplateId()).isEqualTo("weekly-schedule-v1");
        assertThat(template.getTemplateName()).isEqualTo("Weekly Schedule V1");
        assertThat(template.getDescription()).isNotBlank();
    }

    @Test
    void shouldRenderStandardWeeklyScheduleAsDocument() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("weekly_schedule_standard_document", "clean", "templates", "weekly-schedule");
        byte[] pdfBytes;
        PDRectangle landscapeA4 = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());

        try (var document = TemplateTestSupport.openInMemoryDocument(landscapeA4, 18, 18, 18, 18)) {
            new WeeklyScheduleTemplateV1().compose(document, TemplateTestSupport.canonicalWeeklyScheduleData());
            pdfBytes = document.toPdfBytes();
        }

        TemplateTestSupport.writePdf(outputFile, pdfBytes);
        TemplateTestSupport.assertPdfBytesLookValid(pdfBytes, 1);
        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
    }

    @Test
    void shouldRenderScheduleWithoutMetricsOrFooterNotes() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("weekly_schedule_without_metrics_footer", "clean", "templates", "weekly-schedule");
        renderWeeklySchedule(outputFile, TemplateTestSupport.canonicalWeeklyScheduleWithoutMetricsOrFooter());
        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
    }

    @Test
    void shouldRenderScheduleWithAdditionalPerson() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("weekly_schedule_with_additional_person", "clean", "templates", "weekly-schedule");
        renderWeeklySchedule(outputFile, TemplateTestSupport.canonicalWeeklyScheduleWithAdditionalPerson());
        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
    }

    @Test
    void shouldRenderScheduleWhenCategoryCatalogChanges() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("weekly_schedule_changed_category_catalog", "clean", "templates", "weekly-schedule");
        renderWeeklySchedule(outputFile, TemplateTestSupport.canonicalWeeklyScheduleWithChangedCategoryCatalog());
        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
    }

    private void renderWeeklySchedule(Path outputFile,
                                      com.demcha.compose.document.templates.data.schedule.WeeklyScheduleData data) throws Exception {
        PDRectangle landscapeA4 = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());

        try (var document = TemplateTestSupport.openFileDocument(outputFile, landscapeA4, 18, 18, 18, 18, false)) {
            new WeeklyScheduleTemplateV1().compose(document, data);
            document.buildPdf();
        }
    }

    private List<String> linkAnnotationUris(PDDocument document) throws Exception {
        List<String> uris = new ArrayList<>();
        for (var page : document.getPages()) {
            for (var annotation : page.getAnnotations()) {
                if (annotation instanceof PDAnnotationLink link
                        && link.getAction() instanceof PDActionURI action) {
                    uris.add(action.getURI());
                }
            }
        }
        return List.copyOf(uris);
    }
}
