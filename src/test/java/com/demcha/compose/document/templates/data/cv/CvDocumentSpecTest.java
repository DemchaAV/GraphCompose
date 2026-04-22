package com.demcha.compose.document.templates.data.cv;

import com.demcha.compose.document.templates.data.common.Header;

import com.demcha.compose.document.model.node.ListMarker;
import com.demcha.compose.document.templates.TemplateTestSupport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CvDocumentSpecTest {

    @Test
    void shouldAdaptLegacyStandardCvIntoOrderedSemanticModules() {
        MainPageCV original = TemplateTestSupport.canonicalCv();
        MainPageCvDTO rewritten = TemplateTestSupport.rewrite(original);

        CvDocumentSpec spec = CvDocumentSpec.from(original, rewritten);

        assertThat(spec.header()).isNotNull();
        assertThat(spec.modules()).extracting(CvModule::name)
                .containsExactly("Summary", "TechnicalSkills", "Education", "Projects", "Experience", "Additional");

        CvModule summary = spec.modules().get(0);
        CvModule technicalSkills = spec.modules().get(1);
        CvModule education = spec.modules().get(2);

        assertThat(summary.title()).isEqualTo(original.getModuleSummary().getModuleName());
        assertThat(summary.bodyBlocks()).singleElement().satisfies(block -> {
            assertThat(block.kind()).isEqualTo(CvModule.BodyKind.PARAGRAPH);
            assertThat(block.text()).isEqualTo(original.getModuleSummary().getBlockSummary());
        });

        assertThat(technicalSkills.title()).isEqualTo(original.getTechnicalSkills().getName());
        assertThat(technicalSkills.bodyBlocks()).singleElement().satisfies(block -> {
            assertThat(block.kind()).isEqualTo(CvModule.BodyKind.LIST);
            assertThat(block.marker()).isEqualTo(ListMarker.bullet());
            assertThat(block.items()).hasSize(
                    (int) original.getTechnicalSkills().getModulePoints().stream()
                            .map(value -> value == null ? "" : value.trim())
                            .filter(value -> !value.isBlank())
                            .count());
        });

        assertThat(education.title()).isEqualTo(original.getEducationCertifications().getName());
        assertThat(education.bodyBlocks()).singleElement().satisfies(block -> {
            assertThat(block.kind()).isEqualTo(CvModule.BodyKind.LIST);
            assertThat(block.marker()).isEqualTo(ListMarker.none());
            assertThat(block.continuationIndent()).isEqualTo("  ");
        });
    }

    @Test
    void shouldNormalizeLegacyTechnicalSkillMarkersWhileKeepingOtherModulesUntouched() {
        MainPageCV original = TemplateTestSupport.canonicalCv();
        original.getTechnicalSkills().setModulePoints(List.of(
                "• Languages: Java (21), SQL",
                "- Backend: Spring Boot, Spring Data JPA",
                "* Tools: Maven, Docker"));

        CvDocumentSpec spec = CvDocumentSpec.from(original, null);
        CvModule technicalSkills = spec.modules().get(1);

        assertThat(technicalSkills.bodyBlocks()).singleElement().satisfies(block -> {
            assertThat(block.marker()).isEqualTo(ListMarker.bullet());
            assertThat(block.items()).containsExactly(
                    "Languages: Java (21), SQL",
                    "Backend: Spring Boot, Spring Data JPA",
                    "Tools: Maven, Docker");
        });
    }
}
