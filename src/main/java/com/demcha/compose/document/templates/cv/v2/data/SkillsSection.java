package com.demcha.compose.document.templates.cv.v2.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Section whose body is an ordered list of skill categories.
 *
 * <p>This is the semantic skill model: a preset receives
 * {@code Languages -> [Java 21, Kotlin]} rather than having to parse
 * comma-separated strings from a generic {@link RowsSection}. Legacy
 * or import layers may still split older module text into this shape
 * before it reaches the v2 renderer.</p>
 *
 * @param title  non-blank section heading
 * @param groups ordered skill groups; empty groups are ignored
 */
public record SkillsSection(String title, List<SkillGroup> groups)
        implements CvSection {

    /**
     * Validates that {@code title} and {@code groups} are non-null,
     * rejects a blank title, and drops any null or empty groups before
     * copying the list.
     */
    public SkillsSection {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(groups, "groups");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        List<SkillGroup> cleaned = new ArrayList<>(groups.size());
        for (SkillGroup group : groups) {
            if (group != null && !group.skills().isEmpty()) {
                cleaned.add(group);
            }
        }
        groups = List.copyOf(cleaned);
    }

    /**
     * Fluent builder for callers that assemble skills one by one.
     *
     * @param title non-blank section heading
     * @return new builder
     */
    public static Builder builder(String title) {
        return new Builder(title);
    }

    /**
     * Section assembled from a fixed set of skill groups.
     *
     * @param title  non-blank section heading
     * @param groups skill groups, in source order; null becomes empty
     * @return a {@code SkillsSection} carrying the supplied groups
     */
    public static SkillsSection of(String title, SkillGroup... groups) {
        if (groups == null) {
            return new SkillsSection(title, List.of());
        }
        return new SkillsSection(title, List.of(groups));
    }

    /**
     * Mutable builder.
     */
    public static final class Builder {
        private final String title;
        private final List<SkillGroup> groups = new ArrayList<>();

        private Builder(String title) {
            this.title = title;
        }

        /**
         * Appends one pre-built skill group.
         *
         * @param group the group to append (non-null)
         * @return this builder for chaining
         */
        public Builder group(SkillGroup group) {
            this.groups.add(Objects.requireNonNull(group, "group"));
            return this;
        }

        /**
         * Appends a group from a category label and a list of plain
         * skill labels.
         *
         * @param category group category label, non-blank
         * @param skills   plain skill labels; null or blank labels are ignored
         * @return this builder for chaining
         */
        public Builder group(String category, List<String> skills) {
            this.groups.add(SkillGroup.ofNames(category, skills));
            return this;
        }

        /**
         * Appends a group from a category label and plain skill labels.
         *
         * @param category group category label, non-blank
         * @param skills   plain skill labels; null or blank labels are ignored
         * @return this builder for chaining
         */
        public Builder group(String category, String... skills) {
            this.groups.add(SkillGroup.of(category, skills));
            return this;
        }

        /**
         * Add a group whose entries carry optional proficiency levels.
         * Use this for presets that render data-driven skill bars/meters;
         * name-only presets render the same entries as plain labels.
         *
         * @param category group category label, non-blank
         * @param entries  ordered leveled skill entries
         * @return this builder
         */
        public Builder leveledGroup(String category, List<CvSkill> entries) {
            this.groups.add(new SkillGroup(category, entries));
            return this;
        }

        /**
         * Builds the immutable {@link SkillsSection}.
         *
         * @return the assembled section
         */
        public SkillsSection build() {
            return new SkillsSection(title, groups);
        }
    }
}
