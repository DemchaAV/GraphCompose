package com.demcha.compose.document.templates.cv.v2.data;

import java.util.Objects;
import java.util.OptionalDouble;

/**
 * One atomic skill label with an optional proficiency level in
 * {@code [0, 1]}.
 *
 * <p>The level is what data-driven skill visuals — proficiency bars,
 * dots, meters — read. Skills created without a level (the common case,
 * and every skill built through the string-based
 * {@link SkillGroup#of(String, String...)} /
 * {@link SkillGroup#ofNames(String, java.util.List)} APIs) carry an
 * empty level, so name-only renderers are completely unaffected by the
 * level channel.</p>
 *
 * @param name  non-blank skill label
 * @param level optional proficiency in {@code [0, 1]}; empty when
 *              unspecified
 */
public record CvSkill(String name, OptionalDouble level) {

    /**
     * Validates that both fields are non-null, trims {@code name} and
     * rejects a blank result, and bounds any present {@code level} to
     * {@code [0, 1]}.
     */
    public CvSkill {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(level, "level");
        name = name.trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (level.isPresent()) {
            double value = level.getAsDouble();
            if (value < 0.0 || value > 1.0) {
                throw new IllegalArgumentException(
                        "level must be in [0,1] but was " + value);
            }
        }
    }

    /**
     * Skill with no proficiency level.
     *
     * @param name non-blank skill label
     * @return a {@code CvSkill} carrying an empty level
     */
    public static CvSkill of(String name) {
        return new CvSkill(name, OptionalDouble.empty());
    }

    /**
     * Skill with a proficiency level, clamped to {@code [0, 1]}.
     *
     * @param name  non-blank skill label
     * @param level proficiency, clamped into {@code [0, 1]}
     * @return a {@code CvSkill} carrying the clamped level
     */
    public static CvSkill of(String name, double level) {
        return new CvSkill(name,
                OptionalDouble.of(Math.max(0.0, Math.min(1.0, level))));
    }
}
