package com.demcha.compose.document.templates.data.schedule;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Display-oriented weekly schedule document input.
 *
 * @param title document title
 * @param weekLabel display week label
 * @param days days in display order
 * @param categories shared category catalog
 * @param headerMetrics metric rows rendered above the matrix
 * @param people people rows in the schedule matrix
 * @param assignments person/day assignments
 * @param footerNotes footer notes rendered after the matrix
 */
public record WeeklyScheduleData(
        String title,
        String weekLabel,
        List<ScheduleDay> days,
        List<ScheduleCategory> categories,
        List<ScheduleMetricRow> headerMetrics,
        List<SchedulePerson> people,
        List<ScheduleAssignment> assignments,
        List<String> footerNotes
) {
    /**
     * Normalizes optional schedule fields and freezes collection inputs.
     */
    public WeeklyScheduleData {
        title = Objects.requireNonNullElse(title, "Weekly Schedule");
        weekLabel = Objects.requireNonNullElse(weekLabel, "");
        days = List.copyOf(Objects.requireNonNullElse(days, List.of()));
        categories = List.copyOf(Objects.requireNonNullElse(categories, List.of()));
        headerMetrics = List.copyOf(Objects.requireNonNullElse(headerMetrics, List.of()));
        people = List.copyOf(Objects.requireNonNullElse(people, List.of()));
        assignments = List.copyOf(Objects.requireNonNullElse(assignments, List.of()));
        footerNotes = List.copyOf(Objects.requireNonNullElse(footerNotes, List.of()));
    }

    /**
     * Starts a fluent weekly schedule builder.
     *
     * @return weekly schedule builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for complete weekly schedule content.
     */
    public static final class Builder {
        private String title;
        private String weekLabel;
        private final List<ScheduleDay> days = new ArrayList<>();
        private final List<ScheduleCategory> categories = new ArrayList<>();
        private final List<ScheduleMetricRow> headerMetrics = new ArrayList<>();
        private final List<SchedulePerson> people = new ArrayList<>();
        private final List<ScheduleAssignment> assignments = new ArrayList<>();
        private final List<String> footerNotes = new ArrayList<>();

        private Builder() {
        }

        /**
         * Sets the schedule title.
         *
         * @param title schedule title
         * @return this builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the visible week label.
         *
         * @param weekLabel week label
         * @return this builder
         */
        public Builder weekLabel(String weekLabel) {
            this.weekLabel = weekLabel;
            return this;
        }

        /**
         * Replaces all day headers.
         *
         * @param days days in display order
         * @return this builder
         */
        public Builder days(List<ScheduleDay> days) {
            this.days.clear();
            if (days != null) {
                this.days.addAll(days);
            }
            return this;
        }

        /**
         * Appends one day header.
         *
         * @param day schedule day
         * @return this builder
         */
        public Builder addDay(ScheduleDay day) {
            this.days.add(day);
            return this;
        }

        /**
         * Builds and appends one day header.
         *
         * @param spec day builder callback
         * @return this builder
         */
        public Builder day(Consumer<ScheduleDay.Builder> spec) {
            ScheduleDay.Builder builder = ScheduleDay.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return addDay(builder.build());
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
            return addDay(new ScheduleDay(id, label, headerNote, headerCategoryId));
        }

        /**
         * Replaces all categories.
         *
         * @param categories category catalog
         * @return this builder
         */
        public Builder categories(List<ScheduleCategory> categories) {
            this.categories.clear();
            if (categories != null) {
                this.categories.addAll(categories);
            }
            return this;
        }

        /**
         * Appends one category.
         *
         * @param category schedule category
         * @return this builder
         */
        public Builder addCategory(ScheduleCategory category) {
            this.categories.add(category);
            return this;
        }

        /**
         * Builds and appends one category.
         *
         * @param spec category builder callback
         * @return this builder
         */
        public Builder category(Consumer<ScheduleCategory.Builder> spec) {
            ScheduleCategory.Builder builder = ScheduleCategory.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return addCategory(builder.build());
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
            return addCategory(new ScheduleCategory(id, label, fillColor, Color.BLACK, borderColor));
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
            return addCategory(new ScheduleCategory(id, label, fillColor, textColor, borderColor));
        }

        /**
         * Replaces all header metric rows.
         *
         * @param headerMetrics header metric rows
         * @return this builder
         */
        public Builder headerMetrics(List<ScheduleMetricRow> headerMetrics) {
            this.headerMetrics.clear();
            if (headerMetrics != null) {
                this.headerMetrics.addAll(headerMetrics);
            }
            return this;
        }

        /**
         * Appends a header metric row.
         *
         * @param headerMetric header metric row
         * @return this builder
         */
        public Builder addHeaderMetric(ScheduleMetricRow headerMetric) {
            this.headerMetrics.add(headerMetric);
            return this;
        }

        /**
         * Builds and appends a header metric row.
         *
         * @param spec metric row builder callback
         * @return this builder
         */
        public Builder headerMetric(Consumer<ScheduleMetricRow.Builder> spec) {
            ScheduleMetricRow.Builder builder = ScheduleMetricRow.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return addHeaderMetric(builder.build());
        }

        /**
         * Appends a header metric row from label and values.
         *
         * @param label metric label
         * @param dayValues day values
         * @return this builder
         */
        public Builder headerMetric(String label, List<String> dayValues) {
            return addHeaderMetric(new ScheduleMetricRow(label, dayValues));
        }

        /**
         * Appends a header metric row from label and values.
         *
         * @param label metric label
         * @param dayValues day values
         * @return this builder
         */
        public Builder headerMetric(String label, String... dayValues) {
            return addHeaderMetric(ScheduleMetricRow.builder()
                    .label(label)
                    .dayValues(dayValues)
                    .build());
        }

        /**
         * Replaces all people rows.
         *
         * @param people people rows
         * @return this builder
         */
        public Builder people(List<SchedulePerson> people) {
            this.people.clear();
            if (people != null) {
                this.people.addAll(people);
            }
            return this;
        }

        /**
         * Appends one person row.
         *
         * @param person schedule person
         * @return this builder
         */
        public Builder addPerson(SchedulePerson person) {
            this.people.add(person);
            return this;
        }

        /**
         * Builds and appends one person row.
         *
         * @param spec person builder callback
         * @return this builder
         */
        public Builder person(Consumer<SchedulePerson.Builder> spec) {
            SchedulePerson.Builder builder = SchedulePerson.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return addPerson(builder.build());
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
            return addPerson(new SchedulePerson(id, displayName, sortOrder));
        }

        /**
         * Replaces all assignments.
         *
         * @param assignments schedule assignments
         * @return this builder
         */
        public Builder assignments(List<ScheduleAssignment> assignments) {
            this.assignments.clear();
            if (assignments != null) {
                this.assignments.addAll(assignments);
            }
            return this;
        }

        /**
         * Appends one assignment.
         *
         * @param assignment schedule assignment
         * @return this builder
         */
        public Builder addAssignment(ScheduleAssignment assignment) {
            this.assignments.add(assignment);
            return this;
        }

        /**
         * Builds and appends one assignment.
         *
         * @param spec assignment builder callback
         * @return this builder
         */
        public Builder assignment(Consumer<ScheduleAssignment.Builder> spec) {
            ScheduleAssignment.Builder builder = ScheduleAssignment.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return addAssignment(builder.build());
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
            List<ScheduleSlot> normalizedSlots = new ArrayList<>();
            if (slots != null) {
                for (ScheduleSlot slot : slots) {
                    normalizedSlots.add(slot);
                }
            }
            return assignment(personId, dayId, categoryId, normalizedSlots, "");
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
            return addAssignment(new ScheduleAssignment(personId, dayId, categoryId, slots, note));
        }

        /**
         * Replaces all footer notes.
         *
         * @param footerNotes footer note rows
         * @return this builder
         */
        public Builder footerNotes(List<String> footerNotes) {
            this.footerNotes.clear();
            if (footerNotes != null) {
                this.footerNotes.addAll(footerNotes);
            }
            return this;
        }

        /**
         * Appends a footer note.
         *
         * @param footerNote footer note text
         * @return this builder
         */
        public Builder footerNote(String footerNote) {
            this.footerNotes.add(footerNote);
            return this;
        }

        /**
         * Builds immutable weekly schedule data.
         *
         * @return weekly schedule data
         */
        public WeeklyScheduleData build() {
            return new WeeklyScheduleData(
                    title,
                    weekLabel,
                    days,
                    categories,
                    headerMetrics,
                    people,
                    assignments,
                    footerNotes);
        }
    }
}
