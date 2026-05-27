package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
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
 * Smoke test for the v2 Monogram Sidebar preset. Covers the badge
 * initials extraction, contact stack icon resolution, and the
 * Profile + Experience main-column composition.
 */
class MonogramSidebarSmokeTest {

    @Test
    void exposes_stable_identity() {
        DocumentTemplate<CvDocument> template = MonogramSidebar.create();
        assertThat(template.id()).isEqualTo("monogram-sidebar");
        assertThat(template.displayName()).isEqualTo("Monogram Sidebar");
    }

    @Test
    void default_factory_renders_full_document() throws Exception {
        renderAndAssertNonEmpty(MonogramSidebar.create(), fullDocument());
    }

    @Test
    void custom_theme_factory_renders() throws Exception {
        renderAndAssertNonEmpty(MonogramSidebar.create(CvTheme.monogramSidebar()),
                fullDocument());
    }

    /**
     * Asserts the two-column page chrome is emitted via
     * {@code pageBackgrounds(List<PageBackgroundFill>)} — protects the
     * engine contract that both the sidebar fill and main fill are
     * splice-painted on every page, so multi-page content
     * automatically inherits the same visual structure without any
     * preset-side filler logic.
     */
    @Test
    void emits_two_column_page_chrome_on_every_page() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.zero())
                .create()) {
            MonogramSidebar.create().compose(session, fullDocument());
            LayoutGraph layout = session.layoutGraph();
            assertThat(layout.totalPages()).isGreaterThanOrEqualTo(1);
            for (int page = 0; page < layout.totalPages(); page++) {
                int pageBgFragments = 0;
                for (PlacedFragment frag : layout.fragments()) {
                    if (frag.pageIndex() == page
                            && frag.path().startsWith("@page-background[")) {
                        pageBgFragments++;
                    }
                }
                assertThat(pageBgFragments)
                        .as("page %d must paint both sidebar + main fills",
                                page)
                        .isEqualTo(2);
            }
        }
    }

    private static void renderAndAssertNonEmpty(
            DocumentTemplate<CvDocument> template,
            CvDocument doc) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 595)
                .margin(DocumentInsets.zero())
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
