package com.demcha.compose.document.templates.data.schedule;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyScheduleDocumentSpecTest {

    @Test
    void builderShouldCreateDocumentLevelWeeklyScheduleSpec() {
        WeeklyScheduleDocumentSpec spec = WeeklyScheduleDocumentSpec.builder()
                .title("Engineering Roster")
                .weekLabel("Week Of 20 Apr - 26 Apr 2026")
                .day("mon", "Monday", "Release prep", "delivery")
                .category("delivery", "DELIVERY", new Color(0, 173, 76), new Color(0, 110, 49))
                .headerMetric("CAPACITY", "4 engineers")
                .person("artem", "ARTEM", 10)
                .assignment("artem", "mon", "delivery", ScheduleSlot.of("09:00", "17:00"))
                .footerNote("Keep assignments data-driven.")
                .build();

        assertThat(spec.schedule().title()).isEqualTo("Engineering Roster");
        assertThat(spec.schedule().weekLabel()).isEqualTo("Week Of 20 Apr - 26 Apr 2026");
        assertThat(spec.schedule().days()).singleElement()
                .extracting(ScheduleDay::id)
                .isEqualTo("mon");
        assertThat(spec.schedule().categories()).singleElement()
                .extracting(ScheduleCategory::id)
                .isEqualTo("delivery");
        assertThat(spec.schedule().headerMetrics()).singleElement()
                .extracting(ScheduleMetricRow::label)
                .isEqualTo("CAPACITY");
        assertThat(spec.schedule().people()).singleElement()
                .extracting(SchedulePerson::id)
                .isEqualTo("artem");
        assertThat(spec.schedule().assignments()).singleElement()
                .satisfies(assignment -> assertThat(assignment.slots()).singleElement()
                        .extracting(ScheduleSlot::displayText)
                        .isEqualTo("09:00 17:00"));
        assertThat(spec.schedule().footerNotes()).containsExactly("Keep assignments data-driven.");
    }

    @Test
    void fromShouldWrapExistingWeeklyScheduleDataWithoutChangingIt() {
        WeeklyScheduleData schedule = WeeklyScheduleData.builder()
                .title("Roster")
                .build();

        assertThat(WeeklyScheduleDocumentSpec.from(schedule).schedule()).isSameAs(schedule);
    }
}
