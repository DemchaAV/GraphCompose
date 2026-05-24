package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.data.EntriesSection;
import com.demcha.compose.document.templates.cv.v2.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.v2.data.RowStyle;
import com.demcha.compose.document.templates.cv.v2.data.RowsSection;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the v2 ModernProfessional preset — proves the
 * compose-don't-subclass pattern works for a second visually-distinct
 * preset on the same data + same body renderers.
 */
class ModernProfessionalSmokeTest {

    @Test
    void exposes_stable_identity() {
        DocumentTemplate<CvDocument> template = ModernProfessional.create();
        assertThat(template.id()).isEqualTo("modern-professional");
        assertThat(template.displayName()).isEqualTo("Modern Professional");
    }

    @Test
    void default_factory_uses_modernProfessional_theme_and_renders() throws Exception {
        DocumentTemplate<CvDocument> template = ModernProfessional.create();
        renderAndAssertNonEmpty(template, fullDocument());
    }

    @Test
    void custom_theme_factory_renders() throws Exception {
        DocumentTemplate<CvDocument> template =
                ModernProfessional.create(CvTheme.modernProfessional());
        renderAndAssertNonEmpty(template, fullDocument());
    }

    @Test
    void renders_with_classic_theme_too() throws Exception {
        // Preset should not assume any specific theme — handing it the
        // boxedClassic theme must not throw.
        DocumentTemplate<CvDocument> template =
                ModernProfessional.create(CvTheme.boxedClassic());
        renderAndAssertNonEmpty(template, fullDocument());
    }

    private static void renderAndAssertNonEmpty(
            DocumentTemplate<CvDocument> template, CvDocument doc) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 595)
                .margin(DocumentInsets.of(18))
                .create()) {

            template.compose(session, doc);
            assertThat(session.roots()).isNotEmpty();
        }
    }

    private static CvDocument fullDocument() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jane", "Doe")
                        .contact("+44 0", "j@d.com", "London")
                        .link("LinkedIn", "https://linkedin.com/in/jane-doe")
                        .build())
                .sections(
                        new ParagraphSection("Summary", "body"),
                        RowsSection.builder("Skills", RowStyle.BULLETED)
                                .row("Languages", "Java").build(),
                        EntriesSection.builder("Experience")
                                .entry("Engineer", "Acme", "2020", "did stuff").build())
                .build();
    }
}
