package com.demcha.compose.document.templates.data.schedule;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyScheduleDataBuilderTest {

    @Test
    void builderShouldCreateScheduleWithDaysCategoriesMetricsPeopleAssignmentsAndFooterNotes() {
        WeeklyScheduleData schedule = WeeklyScheduleData.builder()
                .title("Engineering Roster")
                .weekLabel("Week Of 20 Apr - 26 Apr 2026")
                .day("mon", "Monday", "Release prep", "delivery")
                .day(day -> day
                        .id("tue")
                        .label("Tuesday")
                        .headerNote("QA pass")
                        .headerCategoryId("qa"))
                .category("delivery", "DELIVERY", new Color(0, 173, 76), new Color(0, 110, 49))
                .category(category -> category
                        .id("qa")
                        .label("QA")
                        .fillColor(new Color(245, 131, 24))
                        .textColor(Color.BLACK)
                        .borderColor(new Color(183, 82, 0)))
                .headerMetric("CAPACITY", "4 engineers", "3 engineers")
                .headerMetric(metric -> metric
                        .label("FOCUS")
                        .dayValues(List.of("API", "Regression")))
                .person("artem", "ARTEM", 10)
                .person(person -> person
                        .id("alex")
                        .displayName("ALEX")
                        .sortOrder(20))
                .assignment("artem", "mon", "delivery", ScheduleSlot.of("09:00", "17:00"))
                .assignment(assignment -> assignment
                        .personId("alex")
                        .dayId("tue")
                        .categoryId("qa")
                        .slot("10:00", "18:00")
                        .note("Regression owner"))
                .footerNote("Keep assignments data-driven.")
                .build();

        assertThat(schedule.title()).isEqualTo("Engineering Roster");
        assertThat(schedule.weekLabel()).isEqualTo("Week Of 20 Apr - 26 Apr 2026");
        assertThat(schedule.days()).extracting(ScheduleDay::id).containsExactly("mon", "tue");
        assertThat(schedule.categories()).extracting(ScheduleCategory::id).containsExactly("delivery", "qa");
        assertThat(schedule.headerMetrics()).extracting(ScheduleMetricRow::label).containsExactly("CAPACITY", "FOCUS");
        assertThat(schedule.people()).extracting(SchedulePerson::id).containsExactly("artem", "alex");
        assertThat(schedule.assignments()).hasSize(2);
        assertThat(schedule.assignments().get(0).slots().get(0).displayText()).isEqualTo("09:00 17:00");
        assertThat(schedule.assignments().get(1).note()).isEqualTo("Regression owner");
        assertThat(schedule.footerNotes()).containsExactly("Keep assignments data-driven.");
    }
}
