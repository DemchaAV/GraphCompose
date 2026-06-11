package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.templates.cv.v2.data.ParagraphSection;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the {@link SectionLookup#hasContent} null contract: six v2 presets
 * call it directly on {@code SectionLookup.firstMatching(...)} results
 * (which are {@code null} when no section title matches) without their own
 * null guards, so a {@code null} section must keep reporting {@code false}
 * — never throw — even if the method is later rewritten as an exhaustive
 * switch over the sealed {@code CvSection} hierarchy.
 */
class SectionLookupTest {

    @Test
    void hasContentToleratesNullSections() {
        assertThat(SectionLookup.hasContent(null)).isFalse();
    }

    @Test
    void hasContentSeesParagraphBodies() {
        assertThat(SectionLookup.hasContent(new ParagraphSection("Profile", "Seasoned engineer.")))
                .isTrue();
        assertThat(SectionLookup.hasContent(new ParagraphSection("Profile", "   ")))
                .isFalse();
    }
}
