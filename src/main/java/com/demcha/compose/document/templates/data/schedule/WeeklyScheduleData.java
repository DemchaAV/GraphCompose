package com.demcha.compose.document.templates.data.schedule;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Display-oriented weekly schedule document input.
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

    public static Builder builder() {
        return new Builder();
    }

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

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder weekLabel(String weekLabel) {
            this.weekLabel = weekLabel;
            return this;
        }

        public Builder days(List<ScheduleDay> days) {
            this.days.clear();
            if (days != null) {
                this.days.addAll(days);
            }
            return this;
        }

        public Builder addDay(ScheduleDay day) {
            this.days.add(day);
            return this;
        }

        public Builder day(Consumer<ScheduleDay.Builder> spec) {
            ScheduleDay.Builder builder = ScheduleDay.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return addDay(builder.build());
        }

        public Builder day(String id, String label, String headerNote, String headerCategoryId) {
            return addDay(new ScheduleDay(id, label, headerNote, headerCategoryId));
        }

        public Builder categories(List<ScheduleCategory> categories) {
            this.categories.clear();
            if (categories != null) {
                this.categories.addAll(categories);
            }
            return this;
        }

        public Builder addCategory(ScheduleCategory category) {
            this.categories.add(category);
            return this;
        }

        public Builder category(Consumer<ScheduleCategory.Builder> spec) {
            ScheduleCategory.Builder builder = ScheduleCategory.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return addCategory(builder.build());
        }

        public Builder category(String id, String label, Color fillColor, Color borderColor) {
            return addCategory(new ScheduleCategory(id, label, fillColor, Color.BLACK, borderColor));
        }

        public Builder category(String id, String label, Color fillColor, Color textColor, Color borderColor) {
            return addCategory(new ScheduleCategory(id, label, fillColor, textColor, borderColor));
        }

        public Builder headerMetrics(List<ScheduleMetricRow> headerMetrics) {
            this.headerMetrics.clear();
            if (headerMetrics != null) {
                this.headerMetrics.addAll(headerMetrics);
            }
            return this;
        }

        public Builder addHeaderMetric(ScheduleMetricRow headerMetric) {
            this.headerMetrics.add(headerMetric);
            return this;
        }

        public Builder headerMetric(Consumer<ScheduleMetricRow.Builder> spec) {
            ScheduleMetricRow.Builder builder = ScheduleMetricRow.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return addHeaderMetric(builder.build());
        }

        public Builder headerMetric(String label, List<String> dayValues) {
            return addHeaderMetric(new ScheduleMetricRow(label, dayValues));
        }

        public Builder headerMetric(String label, String... dayValues) {
            return addHeaderMetric(ScheduleMetricRow.builder()
                    .label(label)
                    .dayValues(dayValues)
                    .build());
        }

        public Builder people(List<SchedulePerson> people) {
            this.people.clear();
            if (people != null) {
                this.people.addAll(people);
            }
            return this;
        }

        public Builder addPerson(SchedulePerson person) {
            this.people.add(person);
            return this;
        }

        public Builder person(Consumer<SchedulePerson.Builder> spec) {
            SchedulePerson.Builder builder = SchedulePerson.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return addPerson(builder.build());
        }

        public Builder person(String id, String displayName, int sortOrder) {
            return addPerson(new SchedulePerson(id, displayName, sortOrder));
        }

        public Builder assignments(List<ScheduleAssignment> assignments) {
            this.assignments.clear();
            if (assignments != null) {
                this.assignments.addAll(assignments);
            }
            return this;
        }

        public Builder addAssignment(ScheduleAssignment assignment) {
            this.assignments.add(assignment);
            return this;
        }

        public Builder assignment(Consumer<ScheduleAssignment.Builder> spec) {
            ScheduleAssignment.Builder builder = ScheduleAssignment.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return addAssignment(builder.build());
        }

        public Builder assignment(String personId, String dayId, String categoryId, ScheduleSlot... slots) {
            List<ScheduleSlot> normalizedSlots = new ArrayList<>();
            if (slots != null) {
                for (ScheduleSlot slot : slots) {
                    normalizedSlots.add(slot);
                }
            }
            return assignment(personId, dayId, categoryId, normalizedSlots, "");
        }

        public Builder assignment(String personId,
                                  String dayId,
                                  String categoryId,
                                  List<ScheduleSlot> slots,
                                  String note) {
            return addAssignment(new ScheduleAssignment(personId, dayId, categoryId, slots, note));
        }

        public Builder footerNotes(List<String> footerNotes) {
            this.footerNotes.clear();
            if (footerNotes != null) {
                this.footerNotes.addAll(footerNotes);
            }
            return this;
        }

        public Builder footerNote(String footerNote) {
            this.footerNotes.add(footerNote);
            return this;
        }

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
