package com.demcha.compose.document.templates.data.cv;

import com.demcha.compose.document.templates.data.common.Header;

import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.templates.TemplateTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CvDocumentSpecTest {

    @Test
    void shouldBuildCvDocumentWithDeveloperFriendlyModuleMethods() {
        CvDocumentSpec spec = CvDocumentSpec.builder()
                .header(new Header())
                .summary("Backend engineer focused on clean APIs.")
                .technicalSkills("Java 21", "Spring Boot")
                .education("BSc Computer Systems Engineering - 2022")
                .projects("GraphCompose - Declarative PDF engine")
                .experience("Backend Engineer - Product platform")
                .additional("Open to hybrid roles in the UK")
                .build();

        assertThat(spec.header()).isNotNull();
        assertThat(spec.modules()).extracting(CvModule::title).containsExactly(
                "Professional Summary",
                "Technical Skills",
                "Education & Certifications",
                "Projects",
                "Professional Experience",
                "Additional Information");
        assertThat(spec.modules().get(1).bodyBlocks()).singleElement().satisfies(block -> {
            assertThat(block.kind()).isEqualTo(CvModule.BodyKind.LIST);
            assertThat(block.marker()).isEqualTo(ListMarker.bullet());
            assertThat(block.items()).containsExactly("Java 21", "Spring Boot");
        });
        assertThat(spec.modules().get(3).bodyBlocks()).singleElement().satisfies(block -> {
            assertThat(block.marker()).isEqualTo(ListMarker.none());
            assertThat(block.continuationIndent()).isEqualTo("  ");
        });
    }

    @Test
    void canonicalFixtureShouldAlreadyUseOrderedSemanticModules() {
        CvDocumentSpec spec = TemplateTestSupport.canonicalCv();

        assertThat(spec.header()).isNotNull();
        assertThat(spec.modules()).extracting(CvModule::name)
                .containsExactly(
                        "Summary",
                        "TechnicalSkills",
                        "Education",
                        "Projects",
                        "Experience",
                        "Additional");

        CvModule summary = spec.modules().get(0);
        CvModule technicalSkills = spec.modules().get(1);
        CvModule education = spec.modules().get(2);

        assertThat(summary.title()).isEqualTo("Professional Summary");
        assertThat(summary.bodyBlocks()).singleElement().satisfies(block -> {
            assertThat(block.kind()).isEqualTo(CvModule.BodyKind.PARAGRAPH);
            assertThat(block.text()).contains("Backend Java Developer");
        });

        assertThat(technicalSkills.title()).isEqualTo("Technical Skills");
        assertThat(technicalSkills.bodyBlocks()).singleElement().satisfies(block -> {
            assertThat(block.kind()).isEqualTo(CvModule.BodyKind.LIST);
            assertThat(block.marker()).isEqualTo(ListMarker.bullet());
            assertThat(block.items()).hasSize(7);
        });

        assertThat(education.title()).isEqualTo("Education & Certifications");
        assertThat(education.bodyBlocks()).singleElement().satisfies(block -> {
            assertThat(block.kind()).isEqualTo(CvModule.BodyKind.LIST);
            assertThat(block.marker()).isEqualTo(ListMarker.none());
            assertThat(block.continuationIndent()).isEqualTo("  ");
        });
    }
}
