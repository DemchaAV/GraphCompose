package com.demcha.compose.document.templates.cv.components;

import com.demcha.compose.document.templates.cv.data.CvSection;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ModuleSection;
import com.demcha.compose.document.templates.cv.data.SectionRole;
import com.demcha.compose.document.templates.cv.data.CvItem;
import com.demcha.compose.document.templates.cv.data.CvKind;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.RowStyle;
import com.demcha.compose.document.templates.cv.data.RowsSection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SectionAllocation} hands each section out once and keeps the rest.
 *
 * <p>The behaviour worth pinning is what a preset built on
 * {@link SectionLookup#firstMatching} could not do: notice that a section was
 * never asked for, and tell two sections apart when both answer to the same
 * keywords.</p>
 */
class SectionAllocationTest {

    private static final ParagraphSection SUMMARY =
            new ParagraphSection("Professional Summary", "Builds pipelines.");
    private static final ParagraphSection PROFILE =
            new ParagraphSection("Profile", "A second prose block.");
    private static final RowsSection AWARDS =
            RowsSection.builder("Awards", RowStyle.PLAIN)
                    .row("Rising Star", "2019")
                    .build();

    @Test
    void claimReturnsTheFirstMatchInDocumentOrder() {
        SectionAllocation allocation = SectionAllocation.of(
                List.of(SUMMARY, PROFILE, AWARDS));

        assertThat(allocation.claim(List.of("summary", "profile")))
                .isSameAs(SUMMARY);
    }

    @Test
    void aClaimedSectionIsNotHandedOutTwice() {
        SectionAllocation allocation = SectionAllocation.of(
                List.of(SUMMARY, PROFILE));

        CvSection first = allocation.claim(List.of("summary", "profile"));
        CvSection second = allocation.claim(List.of("summary", "profile"));

        assertThat(first).isSameAs(SUMMARY);
        assertThat(second)
                .describedAs("the second module must see the second section, "
                        + "not the one already spoken for")
                .isSameAs(PROFILE);
    }

    @Test
    void remainingKeepsWhatNoModuleAskedForInDocumentOrder() {
        SectionAllocation allocation = SectionAllocation.of(
                List.of(SUMMARY, AWARDS, PROFILE));
        allocation.claim(List.of("summary"));

        assertThat(allocation.remaining())
                .describedAs("an unclaimed section is the one a keyword-only "
                        + "preset loses without trace")
                .containsExactly(AWARDS, PROFILE);
    }

    @Test
    void remainingIsEmptyOnceEverySectionIsSpokenFor() {
        SectionAllocation allocation = SectionAllocation.of(List.of(SUMMARY));
        allocation.claim(List.of("summary"));

        assertThat(allocation.remaining()).isEmpty();
    }

    @Test
    void anEmptySectionIsNotOfferedAsLeftoverWork() {
        CvSection empty = EntriesSection.builder("Publications").build();
        SectionAllocation allocation = SectionAllocation.of(List.of(SUMMARY, empty));

        assertThat(allocation.remaining())
                .describedAs("rendering a heading with nothing under it is worse "
                        + "than skipping the section")
                .doesNotContain(empty);
    }

    @Test
    void claimReturnsNullWhenNothingMatches() {
        SectionAllocation allocation = SectionAllocation.of(List.of(SUMMARY));

        assertThat(allocation.claim(List.of("references"))).isNull();
        assertThat(allocation.remaining()).containsExactly(SUMMARY);
    }

    @Test
    void nullInputsAreToleratedTheWayPresetCallSitesExpect() {
        SectionAllocation allocation = SectionAllocation.of(null);

        assertThat(allocation.remaining()).isEmpty();
        assertThat(allocation.claim(List.of("summary"))).isNull();
        assertThat(SectionAllocation.of(List.of(SUMMARY)).claim(null)).isNull();
    }

    @Test
    void theSectionsOwnTitleWinsOverThePresetsLabel() {
        assertThat(SectionAllocation.titleOr(SUMMARY, "Profile"))
                .isEqualTo("Professional Summary");
    }

    @Test
    void thePresetsLabelIsUsedOnlyWhenNoSectionMatched() {
        assertThat(SectionAllocation.titleOr(null, "Languages"))
                .isEqualTo("Languages");
    }

    @Test
    void aBlankTitleCannotReachTheLabelBecauseTheDataRejectsItFirst() {
        assertThatThrownBy(() -> new ParagraphSection("   ", "body"))
                .describedAs("titleOr has no blank branch precisely because "
                        + "this constructor makes one unreachable")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void aMissingFallbackLabelIsARejectedArgument() {
        assertThatThrownBy(() -> SectionAllocation.titleOr(SUMMARY, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("fallback");
    }

    @Test
    void aRoleClaimTakesTheModuleThatNamedTheRole() {
        ModuleSection experience = ModuleSection.builder("Опыт работы",
                        SectionRole.EXPERIENCE, CvKind.ENTRIES_DATED)
                .item(CvItem.of("Ведущий инженер").period("2021"))
                .build();
        SectionAllocation allocation = SectionAllocation.of(List.of(SUMMARY, experience));

        assertThat(allocation.claim(SectionRole.EXPERIENCE, List.of("experience")))
                .as("the heading matches no English keyword; the role is the answer")
                .isSameAs(experience);
        assertThat(allocation.remaining())
                .as("a role-claimed section is claimed, so it is not also a leftover")
                .doesNotContain(experience);
    }

    @Test
    void aRoleClaimFallsBackToTheHeadingForSectionsWithoutARole() {
        SectionAllocation allocation = SectionAllocation.of(List.of(SUMMARY));

        assertThat(allocation.claim(SectionRole.SUMMARY, List.of("summary")))
                .as("hand-written sections carry no role and still route by heading")
                .isSameAs(SUMMARY);
    }

    @Test
    void aDeclaredRoleIsNotClaimableByAnotherSlotsKeywords() {
        // Otherwise the experience slot takes it by role and the projects slot
        // takes it by heading, and the same module renders twice.
        ModuleSection module = ModuleSection.builder("Projects", SectionRole.EXPERIENCE,
                        CvKind.ENTRIES_DATED)
                .item(CvItem.of("Senior Engineer").period("2021"))
                .build();
        SectionAllocation allocation = SectionAllocation.of(List.of(module));

        assertThat(allocation.claim(SectionRole.PROJECTS, List.of("projects"))).isNull();
        assertThat(allocation.claim(SectionRole.EXPERIENCE, List.of("experience")))
                .isSameAs(module);
    }

    @Test
    void aRoleClaimsAtMostOneSectionSoASecondSlotSeesTheNextOne() {
        ModuleSection first = ModuleSection.builder("Erfahrung", SectionRole.EXPERIENCE,
                        CvKind.ENTRIES_DATED).item(CvItem.of("First").period("2021")).build();
        ModuleSection second = ModuleSection.builder("Weitere Erfahrung",
                        SectionRole.EXPERIENCE, CvKind.ENTRIES_DATED)
                .item(CvItem.of("Second").period("2019")).build();
        SectionAllocation allocation = SectionAllocation.of(List.of(first, second));

        assertThat(allocation.claim(SectionRole.EXPERIENCE, List.of("experience"))).isSameAs(first);
        assertThat(allocation.claim(SectionRole.EXPERIENCE, List.of("experience")))
                .as("claiming hands each section out once")
                .isSameAs(second);
    }
}
