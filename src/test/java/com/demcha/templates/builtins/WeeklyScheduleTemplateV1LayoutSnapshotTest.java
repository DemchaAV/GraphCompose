package com.demcha.templates.builtins;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.mock.WeeklyScheduleDataFixtures;
import com.demcha.templates.data.WeeklyScheduleData;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

class WeeklyScheduleTemplateV1LayoutSnapshotTest {

    private final WeeklyScheduleTemplateV1 template = new WeeklyScheduleTemplateV1();

    @Test
    void shouldMatchStandardWeeklyScheduleLayoutSnapshot() throws Exception {
        assertMatches("weekly_schedule_standard", WeeklyScheduleDataFixtures.standardSchedule());
    }

    @Test
    void shouldMatchWeeklyScheduleWithoutMetricsOrFooterLayoutSnapshot() throws Exception {
        assertMatches("weekly_schedule_without_metrics_footer", WeeklyScheduleDataFixtures.withoutMetricsOrFooter());
    }

    @Test
    void shouldMatchWeeklyScheduleWithAdditionalPersonLayoutSnapshot() throws Exception {
        assertMatches("weekly_schedule_with_additional_person", WeeklyScheduleDataFixtures.withAdditionalPerson());
    }

    @Test
    void shouldMatchWeeklyScheduleChangedCategoryCatalogLayoutSnapshot() throws Exception {
        assertMatches("weekly_schedule_changed_category_catalog", WeeklyScheduleDataFixtures.withAddedAndRemovedCategory());
    }

    private void assertMatches(String snapshotName, WeeklyScheduleData data) throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()))
                .margin(18, 18, 18, 18)
                .markdown(true)
                .create()) {
            template.compose(composer, data);
            LayoutSnapshotAssertions.assertMatches(composer, snapshotName, "templates", "weekly-schedule");
        }
    }
}
