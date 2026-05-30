package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.data.CvSkill;
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
 * Smoke test for the v2 Mint Editorial preset. Covers stable identity,
 * the two-page atomic-row composition against the full canonical sample
 * (including level-driven skill bars and the Awards / References grids),
 * and graceful degradation when the optional sidebar/main sections are
 * absent.
 */
class MintEditorialSmokeTest {

    @Test
    void exposes_stable_identity() {
        DocumentTemplate<CvDocument> template = MintEditorial.create();
        assertThat(template.id()).isEqualTo("mint-editorial");
        assertThat(template.displayName()).isEqualTo("Mint Editorial");
    }

    @Test
    void default_factory_renders_two_pages() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(48, 48, 48, 48)
                .create()) {
            MintEditorial.create().compose(session, fullDocument());
            assertThat(session.roots()).isNotEmpty();
            LayoutGraph layout = session.layoutGraph();
            // The preset emits two atomic page rows. The dense fullDocument
            // fills page 1, so the second (atomic) row flows whole onto
            // page 2 — a clean two-page document with neither row overflowing.
            assertThat(layout.totalPages()).isEqualTo(2);
        }
    }

    @Test
    void custom_theme_factory_renders() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(48, 48, 48, 48)
                .create()) {
            MintEditorial.create(CvTheme.mintEditorial())
                    .compose(session, fullDocument());
            assertThat(session.roots()).isNotEmpty();
        }
    }

    @Test
    void renders_with_awards_and_references_grids() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(48, 48, 48, 48)
                .create()) {
            MintEditorial.create().compose(session, documentWithAwardsAndReferences());
            assertThat(session.roots()).isNotEmpty();
        }
    }

    @Test
    void degrades_when_optional_sections_absent() throws Exception {
        // Only identity + profile + a single experience entry — no skills,
        // education, interests, awards, references or social.
        CvDocument minimal = CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jane", "Doe")
                        .jobTitle("Designer")
                        .contact("+44 0", "j@d.com", "London")
                        .build())
                .sections(
                        new ParagraphSection("Profile", "Builds **clean** layouts."),
                        EntriesSection.builder("Professional Experience")
                                .entry("Designer", "Acme", "2021-2024", "Did design.")
                                .build())
                .build();
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(48, 48, 48, 48)
                .create()) {
            MintEditorial.create().compose(session, minimal);
            assertThat(session.roots()).isNotEmpty();
        }
    }

    private static CvDocument fullDocument() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jordan", "Rivera")
                        .jobTitle("Platform Engineer")
                        .contact("+44 20 5555 1000", "jordan.rivera@example.com",
                                "London, UK")
                        .link("LinkedIn", "https://linkedin.com/in/jordan-rivera-demo")
                        .link("GitHub", "https://github.com/jrivera-demo")
                        .build())
                .sections(
                        new ParagraphSection("Professional Summary",
                                "Platform engineer building **resilient** document "
                                        + "pipelines and developer-facing template systems."),
                        SkillsSection.builder("Technical Skills")
                                .leveledGroup("Languages", List.of(
                                        CvSkill.of("Java 21", 0.95),
                                        CvSkill.of("Kotlin", 0.85),
                                        CvSkill.of("SQL", 0.8)))
                                .leveledGroup("Testing", List.of(
                                        CvSkill.of("JUnit 5", 0.9),
                                        CvSkill.of("AssertJ", 0.85)))
                                .build(),
                        EntriesSection.builder("Education & Certifications")
                                .entry("MSc Computer Science",
                                        "University of Manchester", "2019-2021",
                                        "Distinction.")
                                .entry("BSc Software Engineering",
                                        "Imperial College London", "2015-2019",
                                        "First-class honours.")
                                .build(),
                        EntriesSection.builder("Professional Experience")
                                .entry("Senior Platform Engineer", "Northwind Systems",
                                        "2024-Present", "Led the document platform.")
                                .entry("Software Engineer", "BrightLeaf Labs",
                                        "2021-2024", "Built rendering pipelines.")
                                .entry("Backend Engineer", "Helix Print Co",
                                        "2019-2021", "Maintained invoice printing.")
                                .build())
                .build();
    }

    private static CvDocument documentWithAwardsAndReferences() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jordan", "Rivera")
                        .jobTitle("Designer")
                        .contact("+44 0", "j@d.com", "London")
                        .link("LinkedIn", "https://linkedin.com/in/jordan")
                        .build())
                .sections(
                        new ParagraphSection("Profile", "Designs editorial layouts."),
                        EntriesSection.builder("Professional Experience")
                                .entry("Designer", "Acme", "2021-2024", "Did design.")
                                .build(),
                        RowsSection.builder("Awards", RowStyle.PLAIN)
                                .row("Best Layout", "Design Guild | 2023")
                                .row("Editorial Prize", "Type Society | 2022")
                                .build(),
                        RowsSection.builder("References", RowStyle.PLAIN)
                                .row("Alex Stone", "Acme | alex@acme.com")
                                .row("Sam Reed", "BrightLeaf | sam@bl.com")
                                .build())
                .build();
    }
}
