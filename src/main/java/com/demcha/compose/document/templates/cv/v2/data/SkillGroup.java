package com.demcha.compose.document.templates.cv.v2.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * One semantic skill category and its atomic skill labels.
 *
 * <p>Examples: {@code Languages -> [Java 21, Kotlin, SQL]} or
 * {@code Testing -> [JUnit 5, AssertJ, visual regression]}. Presets
 * can render the same group as a table cell, a sidebar list, chips,
 * or a compact inline row without reparsing free text.</p>
 *
 * @param category category label, non-blank
 * @param skills   ordered skill labels; blank values are ignored
 */
public record SkillGroup(String category, List<String> skills) {

    public SkillGroup {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(skills, "skills");
        category = category.trim();
        if (category.isBlank()) {
            throw new IllegalArgumentException("category must not be blank");
        }
        List<String> cleaned = new ArrayList<>(skills.size());
        for (String skill : skills) {
            String value = skill == null ? "" : skill.trim();
            if (!value.isBlank()) {
                cleaned.add(value);
            }
        }
        skills = List.copyOf(cleaned);
    }

    public static SkillGroup of(String category, String... skills) {
        return new SkillGroup(category,
                skills == null ? List.of() : Arrays.asList(skills));
    }

    /**
     * @return comma-separated skills, useful for compact renderers
     */
    public String skillsInline() {
        return String.join(", ", skills);
    }
}
