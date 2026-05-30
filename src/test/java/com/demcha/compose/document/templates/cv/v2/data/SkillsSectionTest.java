package com.demcha.compose.document.templates.cv.v2.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillsSectionTest {

    @Test
    void skillGroup_trims_category_and_skips_blank_skills() {
        SkillGroup group = SkillGroup.ofNames(" Languages ",
                List.of(" Java 21 ", "", "Kotlin"));

        assertThat(group.category()).isEqualTo("Languages");
        assertThat(group.skills()).containsExactly("Java 21", "Kotlin");
        assertThat(group.skillsInline()).isEqualTo("Java 21, Kotlin");
    }

    @Test
    void skillGroup_carries_optional_levels_without_affecting_names() {
        SkillGroup group = new SkillGroup("Design", List.of(
                CvSkill.of("Illustration", 0.9),
                CvSkill.of("Typography")));

        // Name-only view is unchanged whether or not levels are present.
        assertThat(group.skills()).containsExactly("Illustration", "Typography");
        // Levels are readable by data-driven renderers.
        assertThat(group.entries()).hasSize(2);
        assertThat(group.entries().get(0).level().getAsDouble()).isEqualTo(0.9);
        assertThat(group.entries().get(1).level()).isEmpty();
    }

    @Test
    void cvSkill_clamps_level_via_factory_and_rejects_blank_name() {
        assertThat(CvSkill.of("X", 1.5).level().getAsDouble()).isEqualTo(1.0);
        assertThat(CvSkill.of("X", -0.2).level().getAsDouble()).isEqualTo(0.0);
        assertThatThrownBy(() -> CvSkill.of(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void skillsSection_keeps_non_empty_groups_only() {
        SkillsSection section = SkillsSection.builder("Technical Skills")
                .group("Languages", "Java 21", "Kotlin")
                .group(SkillGroup.ofNames("Empty", List.of()))
                .group("Testing", "JUnit 5")
                .build();

        assertThat(section.groups())
                .extracting(SkillGroup::category)
                .containsExactly("Languages", "Testing");
    }

    @Test
    void rejects_blank_category() {
        assertThatThrownBy(() -> SkillGroup.of(" ", "Java"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("category");
    }
}
