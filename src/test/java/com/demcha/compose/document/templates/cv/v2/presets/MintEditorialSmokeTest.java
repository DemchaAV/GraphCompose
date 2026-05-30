package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
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
import com.demcha.compose.document.style.DocumentColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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
    void custom_colour_options_render_two_pages() throws Exception {
        // Dark header band + white name + a contrasting rule and accent —
        // exercises every Options knob at once. Still a clean two-page render.
        MintEditorial.Options options = MintEditorial.Options.builder()
                .headerBandColor(DocumentColor.rgb(24, 24, 24))
                .nameColor(DocumentColor.WHITE)
                .ruleColor(DocumentColor.rgb(220, 120, 90))
                .accentColor(DocumentColor.rgb(139, 207, 190))
                .build();
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(48, 48, 48, 48)
                .create()) {
            MintEditorial.create(options).compose(session, fullDocument());
            assertThat(session.roots()).isNotEmpty();
            assertThat(session.layoutGraph().totalPages()).isEqualTo(2);
        }
    }

    @Test
    void default_options_equal_no_options() {
        // The default Options factory must leave the stock surface identity
        // intact (and, by the parity gate, the stock render).
        DocumentTemplate<CvDocument> withDefaults =
                MintEditorial.create(MintEditorial.Options.defaults());
        assertThat(withDefaults.id()).isEqualTo("mint-editorial");
        assertThat(withDefaults.displayName()).isEqualTo("Mint Editorial");
    }

    @Test
    void band_constants_match_default_masthead() throws Exception {
        // Guard: the banded masthead reuses hand-measured MASTHEAD_* constants
        // to place the name/tagline/rule at the SAME positions the default
        // (bandless) flow produces. This test ties MASTHEAD_RULE_Y to the real
        // default rule y, so any future typography / margin / spacing change
        // that moves the masthead fails here and signals the constants must be
        // re-measured.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(48, 48, 48, 48)
                .create()) {
            MintEditorial.create().compose(session, fullDocument());
            LayoutGraph layout = session.layoutGraph();
            PlacedFragment rule = layout.fragments().stream()
                    .filter(f -> f.pageIndex() == 0)
                    .filter(f -> f.path().contains("CvV2MintEditorialHeaderRule"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "default masthead rule fragment not found"));
            // PlacedFragment.y is the PDF bottom-left origin (y grows up);
            // convert to the top-down page-edge coordinate the constant uses.
            double pageHeight = session.canvas().height();
            double ruleTop = pageHeight - (rule.y() + rule.height());
            assertThat(ruleTop)
                    .as("default rule top must equal MASTHEAD_RULE_Y (re-measure "
                            + "the MASTHEAD_* band constants if this drifts)")
                    .isCloseTo(MintEditorial.MASTHEAD_RULE_Y, within(0.5));
        }
    }

    @Test
    void banded_and_bandless_place_first_row_identically() throws Exception {
        // Complementary guard: the band must not shift the body. The first
        // page-1 content row must start at the same y with and without a band.
        double bandless = firstPageOneRowTop(MintEditorial.create());
        double banded = firstPageOneRowTop(MintEditorial.create(
                MintEditorial.Options.builder()
                        .headerBandColor(DocumentColor.rgb(228, 217, 198))
                        .build()));
        assertThat(banded)
                .as("banded masthead must place the first row at the same y as "
                        + "the bandless masthead")
                .isCloseTo(bandless, within(0.5));
    }

    private static double firstPageOneRowTop(DocumentTemplate<CvDocument> template)
            throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(48, 48, 48, 48)
                .create()) {
            template.compose(session, fullDocument());
            LayoutGraph layout = session.layoutGraph();
            double pageHeight = session.canvas().height();
            return layout.fragments().stream()
                    .filter(f -> f.pageIndex() == 0)
                    .filter(f -> f.path().contains("CvV2MintEditorialPageOne"))
                    .mapToDouble(f -> pageHeight - (f.y() + f.height()))
                    .min()
                    .orElseThrow(() -> new AssertionError(
                            "page-1 row fragments not found"));
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
