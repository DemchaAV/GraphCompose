package com.demcha.compose.document.templates.cv.v2.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * One semantic skill category and its atomic skill entries.
 *
 * <p>Examples: {@code Languages -> [Java 21, Kotlin, SQL]} or
 * {@code Testing -> [JUnit 5, AssertJ, visual regression]}. Presets
 * can render the same group as a table cell, a sidebar list, chips,
 * proficiency bars, or a compact inline row without reparsing free
 * text.</p>
 *
 * <p>Each entry is a {@link CvSkill} that optionally carries a
 * proficiency level in {@code [0, 1]}. The level is read only by
 * data-driven visuals (skill bars/meters); name-only renderers use
 * {@link #skills()} and are unaffected by whether levels are present.
 * Groups built through the string APIs ({@link #of(String, String...)},
 * {@link #ofNames(String, List)}) carry no levels.</p>
 *
 * @param category category label, non-blank
 * @param entries  ordered skill entries; blank-named entries are ignored
 */
public record SkillGroup(String category, List<CvSkill> entries) {

    public SkillGroup {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(entries, "entries");
        category = category.trim();
        if (category.isBlank()) {
            throw new IllegalArgumentException("category must not be blank");
        }
        List<CvSkill> cleaned = new ArrayList<>(entries.size());
        for (CvSkill entry : entries) {
            if (entry != null && !entry.name().isBlank()) {
                cleaned.add(entry);
            }
        }
        entries = List.copyOf(cleaned);
    }

    /** Skill group from plain labels (no proficiency levels). */
    public static SkillGroup of(String category, String... skills) {
        return ofNames(category,
                skills == null ? List.of() : Arrays.asList(skills));
    }

    /** Skill group from a list of plain labels (no proficiency levels). */
    public static SkillGroup ofNames(String category, List<String> skills) {
        List<CvSkill> out = new ArrayList<>(skills == null ? 0 : skills.size());
        if (skills != null) {
            for (String skill : skills) {
                String value = skill == null ? "" : skill.trim();
                if (!value.isBlank()) {
                    out.add(CvSkill.of(value));
                }
            }
        }
        return new SkillGroup(category, out);
    }

    /**
     * Ordered skill labels with the proficiency levels dropped.
     *
     * <p>This is the shape every name-only renderer consumes; it is kept
     * stable so adding levels never changes name-based output.</p>
     *
     * @return ordered skill labels
     */
    public List<String> skills() {
        List<String> names = new ArrayList<>(entries.size());
        for (CvSkill entry : entries) {
            names.add(entry.name());
        }
        return List.copyOf(names);
    }

    /**
     * @return comma-separated skill labels, useful for compact renderers
     */
    public String skillsInline() {
        return String.join(", ", skills());
    }
}
