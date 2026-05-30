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
     */
    public static Builder builder(String title) {
        return new Builder(title);
    }

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

        public Builder group(SkillGroup group) {
            this.groups.add(Objects.requireNonNull(group, "group"));
            return this;
        }

        public Builder group(String category, List<String> skills) {
            this.groups.add(SkillGroup.ofNames(category, skills));
            return this;
        }

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

        public SkillsSection build() {
            return new SkillsSection(title, groups);
        }
    }
}
