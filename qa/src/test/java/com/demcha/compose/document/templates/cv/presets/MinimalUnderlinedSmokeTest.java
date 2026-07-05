package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.SkillsSection;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the v2 MinimalUnderlined preset. Demonstrates that
 * the same {@link CvDocument} renders cleanly through a different
 * composition of the existing components.
 */
class MinimalUnderlinedSmokeTest {

    @Test
    void exposes_stable_identity() {
        DocumentTemplate<CvDocument> template = MinimalUnderlined.create();
        assertThat(template.id()).isEqualTo("minimal-underlined");
        assertThat(template.displayName()).isEqualTo("Minimal Underlined");
    }

    @Test
    void default_factory_renders_full_document() throws Exception {
        DocumentTemplate<CvDocument> template = MinimalUnderlined.create();
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 595)
                .margin(DocumentInsets.of(24))
                .create()) {
            template.compose(session, fullDocument());
            assertThat(session.roots()).isNotEmpty();
        }
    }

    @Test
    void custom_theme_factory_renders() throws Exception {
        DocumentTemplate<CvDocument> template =
                MinimalUnderlined.create(BrandTheme.boxedClassic());
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 595)
                .margin(DocumentInsets.of(24))
                .create()) {
            template.compose(session, fullDocument());
            assertThat(session.roots()).isNotEmpty();
        }
    }

    private static CvDocument fullDocument() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jane", "Doe")
                        .contact("+44 0", "j@d.com", "London")
                        .build())
                .sections(
                        new ParagraphSection("Summary", "body"),
                        SkillsSection.builder("Skills")
                                .group("Languages", "Java").build(),
                        EntriesSection.builder("Experience")
                                .entry("Engineer", "Acme", "2020", "did stuff").build())
                .build();
    }
}
