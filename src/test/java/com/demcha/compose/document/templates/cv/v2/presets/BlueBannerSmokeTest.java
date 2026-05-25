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
 * Smoke test for the v2 BlueBanner preset. Covers the preset-local
 * body dispatcher, especially bulletless project rows and custom
 * uppercase timeline entries.
 */
class BlueBannerSmokeTest {

    @Test
    void exposes_stable_identity() {
        DocumentTemplate<CvDocument> template = BlueBanner.create();
        assertThat(template.id()).isEqualTo("blue-banner");
        assertThat(template.displayName()).isEqualTo("Blue Banner");
    }

    @Test
    void default_factory_renders_full_document() throws Exception {
        DocumentTemplate<CvDocument> template = BlueBanner.create();
        renderAndAssertNonEmpty(template, fullDocument());
    }

    @Test
    void custom_theme_factory_renders() throws Exception {
        DocumentTemplate<CvDocument> template =
                BlueBanner.create(CvTheme.blueBanner());
        renderAndAssertNonEmpty(template, fullDocument());
    }

    private static void renderAndAssertNonEmpty(
            DocumentTemplate<CvDocument> template, CvDocument doc) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 595)
                .margin(DocumentInsets.of(24))
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
                        .link("GitHub", "https://github.com/jane-doe")
                        .build())
                .sections(
                        new ParagraphSection("Professional Summary", "body"),
                        RowsSection.builder("Technical Skills", RowStyle.BULLETED)
                                .row("Languages", "Java").build(),
                        EntriesSection.builder("Professional Experience")
                                .entry("Engineer", "Acme", "2020-2024", "did stuff")
                                .build(),
                        RowsSection.builder("Projects", RowStyle.BULLETED_STACKED)
                                .row("X", "desc").build(),
                        RowsSection.builder("Additional Information", RowStyle.PLAIN)
                                .row("Languages", "English").build())
                .build();
    }
}
