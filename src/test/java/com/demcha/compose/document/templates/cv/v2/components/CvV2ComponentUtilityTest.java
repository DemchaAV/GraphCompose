package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.templates.cv.v2.data.CvSection;
import com.demcha.compose.document.templates.cv.v2.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.v2.data.RowStyle;
import com.demcha.compose.document.templates.cv.v2.data.RowsSection;
import com.demcha.compose.document.templates.cv.v2.data.SkillsSection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CvV2ComponentUtilityTest {

    @Test
    void plainTextRemovesInlineMarkdownMarkers() {
        assertThat(MarkdownInline.plainText("**Graph** _Compose_ `PDF`"))
                .isEqualTo("Graph Compose PDF");
    }

    @Test
    void labelValueRendererNormalizesTrailingColons() {
        assertThat(LabelValueRenderer.normalizedLabel("Languages:"))
                .isEqualTo("Languages");
        assertThat(LabelValueRenderer.normalizedLabel("**Languages:**"))
                .isEqualTo("Languages");
    }

    @Test
    void projectLabelSplitsTrailingStack() {
        ProjectLabel label = ProjectLabel.parse(
                "**GraphCompose** (Java 21, PDFBox, Maven)");

        assertThat(label.title()).isEqualTo("GraphCompose");
        assertThat(label.stack()).isEqualTo("Java 21, PDFBox, Maven");
    }

    @Test
    void projectLabelLeavesPlainTitlesUntouched() {
        ProjectLabel label = ProjectLabel.parse("LayoutLint");

        assertThat(label.title()).isEqualTo("LayoutLint");
        assertThat(label.stack()).isEmpty();
    }

    @Test
    void sectionLookupFindsByNormalizedTitle() {
        CvSection profile = new ParagraphSection("Professional Profile", "Body");
        CvSection projects = RowsSection.builder("Selected Projects",
                        RowStyle.PLAIN)
                .row("GraphCompose", "PDF layout engine")
                .build();

        CvSection found = SectionLookup.firstMatching(
                List.of(profile, projects),
                List.of("projects", "selected projects"));

        assertThat(found).isSameAs(projects);
    }

    @Test
    void sectionLookupDetectsEmptyAndNonEmptySections() {
        assertThat(SectionLookup.hasContent(
                new ParagraphSection("Summary", ""))).isFalse();
        assertThat(SectionLookup.hasContent(
                new ParagraphSection("Summary", "Body"))).isTrue();
        assertThat(SectionLookup.hasContent(
                RowsSection.builder("Projects", RowStyle.PLAIN).build()))
                .isFalse();
        assertThat(SectionLookup.hasContent(
                SkillsSection.builder("Technical Skills").build()))
                .isFalse();
    }
}
