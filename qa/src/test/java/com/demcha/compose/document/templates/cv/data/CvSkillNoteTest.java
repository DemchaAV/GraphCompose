package com.demcha.compose.document.templates.cv.data;

import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the written-out level on {@link CvSkill}: what it normalises, and that
 * every way of building a skill that predates it still compiles and still
 * produces a skill without one.
 */
class CvSkillNoteTest {

    @Test
    void aSkillCarriesTheLevelAsTheDocumentWordsIt() {
        CvSkill skill = new CvSkill("Spanish", OptionalDouble.of(0.6),
                "Professional Working");
        assertThat(skill.note()).isEqualTo("Professional Working");
        assertThat(skill.level()).hasValue(0.6);
    }

    @Test
    void theFactoryTakesBothChannels() {
        CvSkill skill = CvSkill.of("French", 0.4, "Basic");
        assertThat(skill.note()).isEqualTo("Basic");
        assertThat(skill.level()).hasValue(0.4);
    }

    @Test
    void anAbsentNoteIsBlankRatherThanNull() {
        assertThat(new CvSkill("Java", OptionalDouble.empty(), null).note()).isEmpty();
        assertThat(new CvSkill("Java", OptionalDouble.empty(), "   ").note()).isEmpty();
    }

    @Test
    void aNoteIsTrimmed() {
        assertThat(new CvSkill("Java", OptionalDouble.empty(), "  Native  ").note())
                .isEqualTo("Native");
    }

    @Test
    void theTwoArgumentConstructorStillBuildsASkillWithoutANote() {
        CvSkill skill = new CvSkill("Java", OptionalDouble.of(0.9));
        assertThat(skill.note()).isEmpty();
        assertThat(skill.level()).hasValue(0.9);
    }

    @Test
    void theNameOnlyAndLevelFactoriesStillBuildSkillsWithoutANote() {
        assertThat(CvSkill.of("Java").note()).isEmpty();
        assertThat(CvSkill.of("Java").level()).isEmpty();
        assertThat(CvSkill.of("Java", 0.5).note()).isEmpty();
        assertThat(CvSkill.of("Java", 0.5).level()).hasValue(0.5);
    }

    @Test
    void aSkillGroupBuiltFromNamesCarriesNoNotes() {
        SkillGroup group = SkillGroup.of("Languages", "Java", "Kotlin");
        assertThat(group.entries()).allSatisfy(skill -> assertThat(skill.note()).isEmpty());
    }
}
