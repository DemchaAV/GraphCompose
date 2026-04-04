package com.demcha.templates.weekly_schedule;

import com.demcha.mock.WeeklyScheduleDataFixtures;
import com.demcha.templates.builtins.WeeklyScheduleTemplateV1;
import com.demcha.templates.data.WeeklyScheduleData;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyScheduleTemplateV1RenderTest {

    private final WeeklyScheduleTemplateV1 template = new WeeklyScheduleTemplateV1();

    @Test
    void shouldExposeTemplateMetadata() {
        assertThat(template.getTemplateId()).isEqualTo("weekly-schedule-v1");
        assertThat(template.getTemplateName()).isEqualTo("Weekly Schedule V1");
        assertThat(template.getDescription()).isNotBlank();
    }

    @Test
    void shouldRenderStandardWeeklyScheduleAsDocument() throws Exception {
        WeeklyScheduleData data = WeeklyScheduleDataFixtures.standardSchedule();
        Path outputFile = VisualTestOutputs.preparePdf("weekly_schedule_standard_document", "clean", "templates", "weekly-schedule");

        try (PDDocument document = template.render(data)) {
            document.save(outputFile.toFile());
        }

        assertPdfLooksValid(outputFile, 1);
    }

    @Test
    void shouldRenderScheduleWithoutMetricsOrFooterNotes() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("weekly_schedule_without_metrics_footer", "clean", "templates", "weekly-schedule");

        template.render(WeeklyScheduleDataFixtures.withoutMetricsOrFooter(), outputFile);

        assertPdfLooksValid(outputFile, 1);
    }

    @Test
    void shouldRenderScheduleWithAdditionalPerson() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("weekly_schedule_with_additional_person", "clean", "templates", "weekly-schedule");

        template.render(WeeklyScheduleDataFixtures.withAdditionalPerson(), outputFile);

        assertPdfLooksValid(outputFile, 1);
    }

    @Test
    void shouldRenderScheduleWhenCategoryCatalogChanges() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("weekly_schedule_changed_category_catalog", "clean", "templates", "weekly-schedule");

        template.render(WeeklyScheduleDataFixtures.withAddedAndRemovedCategory(), outputFile);

        assertPdfLooksValid(outputFile, 1);
    }

    private void assertPdfLooksValid(Path outputFile, int minPages) throws Exception {
        assertThat(outputFile).exists();
        assertThat(outputFile).isRegularFile();
        assertThat(outputFile).isNotEmptyFile();

        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            assertThat(saved.getNumberOfPages()).isGreaterThanOrEqualTo(minPages);
        }
    }
}
