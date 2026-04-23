package com.demcha.compose.document.templates.data.schedule;

import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;

/**
 * Public compose-first weekly schedule input.
 *
 * <p><b>Authoring role:</b> gives callers one document-level object to pass to
 * schedule templates while keeping a domain-friendly builder vocabulary:
 * days, categories, metrics, people, assignments, and footer notes.</p>
 *
 * @param schedule normalized weekly schedule content rendered by templates
 * @author Artem Demchyshyn
 */
public record WeeklyScheduleDocumentSpec(WeeklyScheduleData schedule) {

    /**
     * Creates a normalized weekly schedule document spec.
     */
    public WeeklyScheduleDocumentSpec {
        schedule = schedule == null ? WeeklyScheduleData.builder().build() : schedule;
    }

    /**
     * Wraps existing schedule data in the document-level spec expected by
     * canonical templates.
     *
     * @param schedule weekly schedule data
     * @return document spec
     */
    public static WeeklyScheduleDocumentSpec from(WeeklyScheduleData schedule) {
        return new WeeklyScheduleDocumentSpec(schedule);
    }

    /**
     * Starts a fluent weekly schedule document builder.
     *
     * @return document builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for weekly schedule document specs.
     */
    public static final class Builder {
        private final WeeklyScheduleData.Builder schedule = WeeklyScheduleData.builder();

        private Builder() {
        }

        public Builder title(String title) {
            schedule.title(title);
            return this;
        }

        public Builder weekLabel(String weekLabel) {
            schedule.weekLabel(weekLabel);
            return this;
        }

        public Builder days(List<ScheduleDay> days) {
            schedule.days(days);
            return this;
        }

        public Builder addDay(ScheduleDay day) {
            schedule.addDay(day);
            return this;
        }

        public Builder day(Consumer<ScheduleDay.Builder> spec) {
            schedule.day(spec);
            return this;
        }

        public Builder day(String id, String label, String headerNote, String headerCategoryId) {
            schedule.day(id, label, headerNote, headerCategoryId);
            return this;
        }

        public Builder categories(List<ScheduleCategory> categories) {
            schedule.categories(categories);
            return this;
        }

        public Builder addCategory(ScheduleCategory category) {
            schedule.addCategory(category);
            return this;
        }

        public Builder category(Consumer<ScheduleCategory.Builder> spec) {
            schedule.category(spec);
            return this;
        }

        public Builder category(String id, String label, Color fillColor, Color borderColor) {
            schedule.category(id, label, fillColor, borderColor);
            return this;
        }

        public Builder category(String id, String label, Color fillColor, Color textColor, Color borderColor) {
            schedule.category(id, label, fillColor, textColor, borderColor);
            return this;
        }

        public Builder headerMetrics(List<ScheduleMetricRow> headerMetrics) {
            schedule.headerMetrics(headerMetrics);
            return this;
        }

        public Builder addHeaderMetric(ScheduleMetricRow headerMetric) {
            schedule.addHeaderMetric(headerMetric);
            return this;
        }

        public Builder headerMetric(Consumer<ScheduleMetricRow.Builder> spec) {
            schedule.headerMetric(spec);
            return this;
        }

        public Builder headerMetric(String label, List<String> dayValues) {
            schedule.headerMetric(label, dayValues);
            return this;
        }

        public Builder headerMetric(String label, String... dayValues) {
            schedule.headerMetric(label, dayValues);
            return this;
        }

        public Builder people(List<SchedulePerson> people) {
            schedule.people(people);
            return this;
        }

        public Builder addPerson(SchedulePerson person) {
            schedule.addPerson(person);
            return this;
        }

        public Builder person(Consumer<SchedulePerson.Builder> spec) {
            schedule.person(spec);
            return this;
        }

        public Builder person(String id, String displayName, int sortOrder) {
            schedule.person(id, displayName, sortOrder);
            return this;
        }

        public Builder assignments(List<ScheduleAssignment> assignments) {
            schedule.assignments(assignments);
            return this;
        }

        public Builder addAssignment(ScheduleAssignment assignment) {
            schedule.addAssignment(assignment);
            return this;
        }

        public Builder assignment(Consumer<ScheduleAssignment.Builder> spec) {
            schedule.assignment(spec);
            return this;
        }

        public Builder assignment(String personId, String dayId, String categoryId, ScheduleSlot... slots) {
            schedule.assignment(personId, dayId, categoryId, slots);
            return this;
        }

        public Builder assignment(String personId,
                                  String dayId,
                                  String categoryId,
                                  List<ScheduleSlot> slots,
                                  String note) {
            schedule.assignment(personId, dayId, categoryId, slots, note);
            return this;
        }

        public Builder footerNotes(List<String> footerNotes) {
            schedule.footerNotes(footerNotes);
            return this;
        }

        public Builder footerNote(String footerNote) {
            schedule.footerNote(footerNote);
            return this;
        }

        public WeeklyScheduleDocumentSpec build() {
            return new WeeklyScheduleDocumentSpec(schedule.build());
        }
    }
}
