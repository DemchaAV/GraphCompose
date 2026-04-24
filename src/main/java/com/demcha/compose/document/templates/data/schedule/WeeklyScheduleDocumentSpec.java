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

        /**
         * Sets the schedule title.
         *
         * @param title schedule title
         * @return this builder
         */
        public Builder title(String title) {
            schedule.title(title);
            return this;
        }

        /**
         * Sets the visible week label.
         *
         * @param weekLabel week label
         * @return this builder
         */
        public Builder weekLabel(String weekLabel) {
            schedule.weekLabel(weekLabel);
            return this;
        }

        /**
         * Replaces all day headers.
         *
         * @param days days in display order
         * @return this builder
         */
        public Builder days(List<ScheduleDay> days) {
            schedule.days(days);
            return this;
        }

        /**
         * Appends one day header.
         *
         * @param day schedule day
         * @return this builder
         */
        public Builder addDay(ScheduleDay day) {
            schedule.addDay(day);
            return this;
        }

        /**
         * Builds and appends one day header.
         *
         * @param spec day builder callback
         * @return this builder
         */
        public Builder day(Consumer<ScheduleDay.Builder> spec) {
            schedule.day(spec);
            return this;
        }

        /**
         * Appends a day header from display values.
         *
         * @param id stable day identifier
         * @param label display day label
         * @param headerNote optional header note
         * @param headerCategoryId optional category identifier
         * @return this builder
         */
        public Builder day(String id, String label, String headerNote, String headerCategoryId) {
            schedule.day(id, label, headerNote, headerCategoryId);
            return this;
        }

        /**
         * Replaces all categories.
         *
         * @param categories category catalog
         * @return this builder
         */
        public Builder categories(List<ScheduleCategory> categories) {
            schedule.categories(categories);
            return this;
        }

        /**
         * Appends one category.
         *
         * @param category schedule category
         * @return this builder
         */
        public Builder addCategory(ScheduleCategory category) {
            schedule.addCategory(category);
            return this;
        }

        /**
         * Builds and appends one category.
         *
         * @param spec category builder callback
         * @return this builder
         */
        public Builder category(Consumer<ScheduleCategory.Builder> spec) {
            schedule.category(spec);
            return this;
        }

        /**
         * Appends a category using black text.
         *
         * @param id stable category identifier
         * @param label display label
         * @param fillColor fill color
         * @param borderColor border color
         * @return this builder
         */
        public Builder category(String id, String label, Color fillColor, Color borderColor) {
            schedule.category(id, label, fillColor, borderColor);
            return this;
        }

        /**
         * Appends a category with explicit text color.
         *
         * @param id stable category identifier
         * @param label display label
         * @param fillColor fill color
         * @param textColor text color
         * @param borderColor border color
         * @return this builder
         */
        public Builder category(String id, String label, Color fillColor, Color textColor, Color borderColor) {
            schedule.category(id, label, fillColor, textColor, borderColor);
            return this;
        }

        /**
         * Replaces all header metric rows.
         *
         * @param headerMetrics header metric rows
         * @return this builder
         */
        public Builder headerMetrics(List<ScheduleMetricRow> headerMetrics) {
            schedule.headerMetrics(headerMetrics);
            return this;
        }

        /**
         * Appends a header metric row.
         *
         * @param headerMetric header metric row
         * @return this builder
         */
        public Builder addHeaderMetric(ScheduleMetricRow headerMetric) {
            schedule.addHeaderMetric(headerMetric);
            return this;
        }

        /**
         * Builds and appends a header metric row.
         *
         * @param spec metric row builder callback
         * @return this builder
         */
        public Builder headerMetric(Consumer<ScheduleMetricRow.Builder> spec) {
            schedule.headerMetric(spec);
            return this;
        }

        /**
         * Appends a header metric row from label and values.
         *
         * @param label metric label
         * @param dayValues day values
         * @return this builder
         */
        public Builder headerMetric(String label, List<String> dayValues) {
            schedule.headerMetric(label, dayValues);
            return this;
        }

        /**
         * Appends a header metric row from label and values.
         *
         * @param label metric label
         * @param dayValues day values
         * @return this builder
         */
        public Builder headerMetric(String label, String... dayValues) {
            schedule.headerMetric(label, dayValues);
            return this;
        }

        /**
         * Replaces all people rows.
         *
         * @param people people rows
         * @return this builder
         */
        public Builder people(List<SchedulePerson> people) {
            schedule.people(people);
            return this;
        }

        /**
         * Appends a person row.
         *
         * @param person schedule person
         * @return this builder
         */
        public Builder addPerson(SchedulePerson person) {
            schedule.addPerson(person);
            return this;
        }

        /**
         * Builds and appends one person row.
         *
         * @param spec person builder callback
         * @return this builder
         */
        public Builder person(Consumer<SchedulePerson.Builder> spec) {
            schedule.person(spec);
            return this;
        }

        /**
         * Appends a person row from display values.
         *
         * @param id person identifier
         * @param displayName display name
         * @param sortOrder sort order
         * @return this builder
         */
        public Builder person(String id, String displayName, int sortOrder) {
            schedule.person(id, displayName, sortOrder);
            return this;
        }

        /**
         * Replaces all assignments.
         *
         * @param assignments schedule assignments
         * @return this builder
         */
        public Builder assignments(List<ScheduleAssignment> assignments) {
            schedule.assignments(assignments);
            return this;
        }

        /**
         * Appends one assignment.
         *
         * @param assignment schedule assignment
         * @return this builder
         */
        public Builder addAssignment(ScheduleAssignment assignment) {
            schedule.addAssignment(assignment);
            return this;
        }

        /**
         * Builds and appends one assignment.
         *
         * @param spec assignment builder callback
         * @return this builder
         */
        public Builder assignment(Consumer<ScheduleAssignment.Builder> spec) {
            schedule.assignment(spec);
            return this;
        }

        /**
         * Appends an assignment from ids and slot rows.
         *
         * @param personId person identifier
         * @param dayId day identifier
         * @param categoryId category identifier
         * @param slots assignment slots
         * @return this builder
         */
        public Builder assignment(String personId, String dayId, String categoryId, ScheduleSlot... slots) {
            schedule.assignment(personId, dayId, categoryId, slots);
            return this;
        }

        /**
         * Appends an assignment from ids, slot rows, and a note.
         *
         * @param personId person identifier
         * @param dayId day identifier
         * @param categoryId category identifier
         * @param slots assignment slots
         * @param note optional note
         * @return this builder
         */
        public Builder assignment(String personId,
                                  String dayId,
                                  String categoryId,
                                  List<ScheduleSlot> slots,
                                  String note) {
            schedule.assignment(personId, dayId, categoryId, slots, note);
            return this;
        }

        /**
         * Replaces all footer notes.
         *
         * @param footerNotes footer note rows
         * @return this builder
         */
        public Builder footerNotes(List<String> footerNotes) {
            schedule.footerNotes(footerNotes);
            return this;
        }

        /**
         * Appends a footer note.
         *
         * @param footerNote footer note text
         * @return this builder
         */
        public Builder footerNote(String footerNote) {
            schedule.footerNote(footerNote);
            return this;
        }

        /**
         * Builds the weekly schedule document spec.
         *
         * @return weekly schedule document spec
         */
        public WeeklyScheduleDocumentSpec build() {
            return new WeeklyScheduleDocumentSpec(schedule.build());
        }
    }
}
