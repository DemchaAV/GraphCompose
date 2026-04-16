package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

class BuiltInTemplateLayoutSnapshotTest {

    @Test
    void shouldMatchCoverLetterLayoutSnapshot() throws Exception {
        try (DocumentSession document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 15, 10, 15, 15)) {
            new CoverLetterTemplateV1().compose(
                    document,
                    TemplateTestSupport.canonicalHeader(),
                    TemplateTestSupport.coverLetter("Visual Test Company"),
                    TemplateTestSupport.jobDetails("Visual Test Company"));
            TemplateTestSupport.assertCanonicalSnapshot(document, "cover-letter/cover_letter_standard");
        }
    }

    @Test
    void shouldMatchEditorialBlueLayoutSnapshot() throws Exception {
        var original = TemplateTestSupport.canonicalCv();
        var rewritten = TemplateTestSupport.rewrite(original);

        try (DocumentSession document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 18, 18, 18, 18)) {
            new EditorialBlueCvTemplate().compose(document, original, rewritten);
            TemplateTestSupport.assertCanonicalSnapshot(document, "editorial_blue_standard", "cv");
        }
    }

    @Test
    void shouldMatchInvoiceLayoutSnapshotAndRenderPdf() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("invoice_layout_snapshot", "clean", "templates", "invoice");

        try (DocumentSession document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 22, 22, 22, 22, false)) {
            new InvoiceTemplateV1().compose(document, TemplateTestSupport.canonicalInvoiceData());
            TemplateTestSupport.assertCanonicalSnapshot(document, "invoice/invoice_standard_layout");
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
    }

    @Test
    void shouldMatchLongProposalLayoutSnapshot() throws Exception {
        try (DocumentSession document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 22, 22, 22, 22)) {
            new ProposalTemplateV1().compose(document, TemplateTestSupport.canonicalProposalData());
            TemplateTestSupport.assertCanonicalSnapshot(document, "proposal/proposal_long_layout");
        }
    }

    @Test
    void shouldMatchStandardWeeklyScheduleLayoutSnapshot() throws Exception {
        assertWeeklyScheduleMatches("weekly_schedule_standard", TemplateTestSupport.canonicalWeeklyScheduleData());
    }

    @Test
    void shouldMatchWeeklyScheduleWithoutMetricsOrFooterLayoutSnapshot() throws Exception {
        assertWeeklyScheduleMatches(
                "weekly_schedule_without_metrics_footer",
                TemplateTestSupport.canonicalWeeklyScheduleWithoutMetricsOrFooter());
    }

    @Test
    void shouldMatchWeeklyScheduleWithAdditionalPersonLayoutSnapshot() throws Exception {
        assertWeeklyScheduleMatches(
                "weekly_schedule_with_additional_person",
                TemplateTestSupport.canonicalWeeklyScheduleWithAdditionalPerson());
    }

    @Test
    void shouldMatchWeeklyScheduleChangedCategoryCatalogLayoutSnapshot() throws Exception {
        assertWeeklyScheduleMatches(
                "weekly_schedule_changed_category_catalog",
                TemplateTestSupport.canonicalWeeklyScheduleWithChangedCategoryCatalog());
    }

    private void assertWeeklyScheduleMatches(String snapshotName,
                                             com.demcha.compose.document.templates.data.WeeklyScheduleData data) throws Exception {
        PDRectangle landscapeA4 = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());

        try (DocumentSession document = TemplateTestSupport.openInMemoryDocument(landscapeA4, 18, 18, 18, 18)) {
            new WeeklyScheduleTemplateV1().compose(document, data);
            TemplateTestSupport.assertCanonicalSnapshot(document, snapshotName, "weekly-schedule");
        }
    }
}
