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
import com.demcha.compose.document.templates.cv.v2.data.SkillsSection;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the v2 Engineering Resume preset. Covers the navy
 * command header with subtitle + contact stack, plus the 2-column
 * rail / main-card composition fed through
 * {@link com.demcha.compose.document.templates.cv.v2.components.SectionLookup}.
 */
class EngineeringResumeSmokeTest {

    @Test
    void exposes_stable_identity() {
        DocumentTemplate<CvDocument> template = EngineeringResume.create();
        assertThat(template.id()).isEqualTo("engineering-resume");
        assertThat(template.displayName()).isEqualTo("Engineering Resume");
    }

    @Test
    void default_factory_renders_full_document() throws Exception {
        renderAndAssertNonEmpty(EngineeringResume.create(), fullDocument());
    }

    @Test
    void custom_theme_factory_renders() throws Exception {
        renderAndAssertNonEmpty(EngineeringResume.create(CvTheme.engineeringResume()),
                fullDocument());
    }

    private static void renderAndAssertNonEmpty(
            DocumentTemplate<CvDocument> template,
            CvDocument doc) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 595)
                .margin(DocumentInsets.of(20))
                .create()) {
            template.compose(session, doc);
            assertThat(session.roots()).isNotEmpty();
        }
    }

    private static CvDocument fullDocument() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jane", "Doe")
                        .jobTitle("Senior Platform Engineer")
                        .contact("+44 0", "j@d.com", "London")
                        .link("LinkedIn", "https://linkedin.com/in/jane-doe")
                        .link("GitHub", "https://github.com/jane")
                        .build())
                .sections(
                        new ParagraphSection("Professional Summary",
                                "Builds **reliable** document pipelines."),
                        SkillsSection.builder("Technical Skills")
                                .group("Languages", "Java 21", "Kotlin")
                                .group("Testing", "JUnit 5", "AssertJ")
                                .build(),
                        EntriesSection.builder("Education & Certifications")
                                .entry("MSc Computer Science",
                                        "University of Manchester",
                                        "2019-2021",
                                        "Distinction.")
                                .build(),
                        RowsSection.builder("Projects", RowStyle.BULLETED_STACKED)
                                .row("GraphCompose (Java, PDFBox)",
                                        "Declarative PDF layout engine.")
                                .build(),
                        EntriesSection.builder("Professional Experience")
                                .entry("Senior Platform Engineer", "Acme",
                                        "2021-2024",
                                        "Built rendering services.")
                                .build(),
                        RowsSection.builder("Additional Information", RowStyle.PLAIN)
                                .row("Languages", "English, German")
                                .build())
                .build();
    }
}
