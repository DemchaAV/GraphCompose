package com.demcha.compose.document.templates.cv.v2.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillsSectionTest {

    @Test
    void skillGroup_trims_category_and_skips_blank_skills() {
        SkillGroup group = new SkillGroup(" Languages ",
                List.of(" Java 21 ", "", "Kotlin"));

        assertThat(group.category()).isEqualTo("Languages");
        assertThat(group.skills()).containsExactly("Java 21", "Kotlin");
        assertThat(group.skillsInline()).isEqualTo("Java 21, Kotlin");
    }

    @Test
    void skillsSection_keeps_non_empty_groups_only() {
        SkillsSection section = SkillsSection.builder("Technical Skills")
                .group("Languages", "Java 21", "Kotlin")
                .group(new SkillGroup("Empty", List.of()))
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
