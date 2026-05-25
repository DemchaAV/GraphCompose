package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.DocumentNode;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the v2 BoxedSections preset. Builds a document with
 * <strong>every</strong> sealed section subtype and verifies compose
 * runs cleanly — catches dispatcher omissions and renderer
 * regressions before they hit the example PDFs.
 */
class BoxedSectionsSmokeTest {

    @Test
    void exposes_stable_identity() {
        DocumentTemplate<CvDocument> template = BoxedSections.create();
        assertThat(template.id()).isEqualTo("boxed-sections");
        assertThat(template.displayName()).isEqualTo("Boxed Sections");
    }

    @Test
    void default_factory_uses_classic_theme_and_renders() throws Exception {
        DocumentTemplate<CvDocument> template = BoxedSections.create();
        renderAndAssertNonEmpty(template, fullDocument());
    }

    @Test
    void custom_theme_factory_renders() throws Exception {
        DocumentTemplate<CvDocument> template =
                BoxedSections.create(CvTheme.boxedClassic());
        renderAndAssertNonEmpty(template, fullDocument());
    }

    @Test
    void every_section_subtype_dispatches_cleanly() throws Exception {
        // Documents that exercise each subtype in isolation — catches
        // any dispatcher branch that throws on a specific row style
        // or empty-list shape.
        DocumentTemplate<CvDocument> template = BoxedSections.create();

        renderAndAssertNonEmpty(template, documentWith(
                new ParagraphSection("Summary", "body text")));
        renderAndAssertNonEmpty(template, documentWith(
                SkillsSection.builder("Skills")
                        .group("Languages", "Java", "Kotlin")
                        .build()));
        renderAndAssertNonEmpty(template, documentWith(
                RowsSection.builder("Info", RowStyle.PLAIN)
                        .row("Languages", "English")
                        .build()));
        renderAndAssertNonEmpty(template, documentWith(
                RowsSection.builder("Projects", RowStyle.BULLETED_STACKED)
                        .row("Project X", "what it does")
                        .build()));
        renderAndAssertNonEmpty(template, documentWith(
                EntriesSection.builder("Experience")
                        .entry("Engineer", "Acme", "2020-2024", "did stuff")
                        .build()));
    }

    private static void renderAndAssertNonEmpty(
            DocumentTemplate<CvDocument> template, CvDocument doc) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 595)
                .margin(DocumentInsets.of(24))
                .create()) {

            template.compose(session, doc);

            List<DocumentNode> roots = session.roots();
            assertThat(roots).isNotEmpty();
        }
    }

    private static CvDocument documentWith(
            com.demcha.compose.document.templates.cv.v2.data.CvSection section) {
        return CvDocument.builder()
                .identity(identity())
                .section(section)
                .build();
    }

    private static CvDocument fullDocument() {
        return CvDocument.builder()
                .identity(identity())
                .sections(
                        new ParagraphSection("Summary", "body"),
                        SkillsSection.builder("Skills")
                                .group("Languages", "Java").build(),
                        EntriesSection.builder("Experience")
                                .entry("Engineer", "Acme", "2020", "did stuff").build(),
                        RowsSection.builder("Projects", RowStyle.BULLETED_STACKED)
                                .row("X", "desc").build(),
                        RowsSection.builder("Info", RowStyle.PLAIN)
                                .row("Languages", "English").build())
                .build();
    }

    private static CvIdentity identity() {
        return CvIdentity.builder()
                .name("Jane", "Doe")
                .contact("+44 0", "j@d.com", "London")
                .link("GitHub", "https://github.com/jane-doe")
                .build();
    }
}
