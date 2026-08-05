package com.demcha.compose.document.templates.cv.components;

import com.demcha.compose.document.templates.cv.data.CvSection;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
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
}
