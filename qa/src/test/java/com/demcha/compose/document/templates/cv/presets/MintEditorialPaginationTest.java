package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.snapshot.LayoutNodeSnapshot;
import com.demcha.compose.document.snapshot.LayoutSnapshot;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvSection;
import com.demcha.compose.document.templates.cv.data.CvSkill;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.SkillsSection;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A CV rendered through Mint Editorial keeps every entry it was given, and takes
 * the pages that needs.
 *
 * <p>This preset did not cap its body the way its siblings did — it <em>dealt</em>
 * it. Each page was a separate atomic row filled by hand: the first two jobs and
 * the profile on page 1, the rest of the jobs plus awards and references on page 2,
 * with the sidebar split the same way. A CV with a third page's worth of career had
 * nowhere to put it, and two blocks were capped outright: six expertise labels and
 * six skill bars, with everything after them dropped in silence.</p>
 *
 * <p>The body is one column flow now. Each column runs the whole document and
 * breaks where it runs out of page, so what lands on page 2 is what did not fit
 * rather than what the preset decided in advance belonged there.</p>
 */
class MintEditorialPaginationTest {

    @Test
    void everySkillAndExpertiseLabelSurvivesIntoThePdf() throws Exception {
        String text = renderText(denseDocument());

        assertThat(text)
                .describedAs("every expertise category, not the first six")
                .contains("LANGUAGES", "DOCUMENT&PRINT", "LAYOUTENGINES",
                        "BUILD&INFRASTRUCTURE", "TESTING", "DISTRIBUTION",
                        "OPERATIONS");

        assertThat(text)
                .describedAs("every skill bar, not the first six")
                .contains("JAVA21", "KOTLIN", "GROOVY", "PYTHON", "SQL", "PDFBOX")
                .contains("POSTSCRIPT", "FONTMETRICS", "PAGINATION", "ASSERTJ",
                        "GPGSIGNING", "MAVENCENTRAL", "DOCKER");
    }

    @Test
    void everyJobReachesThePageRatherThanTheSliceItWasDealt() throws Exception {
        String text = renderText(denseDocument());

        // Employers, not job titles: the titles are letter-spaced and two of them
        // are substrings of other things this preset draws — "BACKENDENGINEER" of
        // "SENIORBACKENDENGINEER", and "SOFTWAREENGINEER" of the education entry
        // "BSc Software Engineering" — so a cap that dropped those jobs would still
        // find the string. Each employer appears exactly once in the document.
        assertThat(text)
                .describedAs("the whole run of jobs reaches the page, however many "
                        + "pages that takes")
                .contains("AcmeRendering")
                .contains("NorthwindData")
                .contains("HeliosPrint")
                .contains("MeridianLabs")
                .contains("Nikoplast")
                .contains("HarbourSystems");
    }

    @Test
    void bothColumnsFlowFromOneDocumentRatherThanPerPageLists() throws Exception {
        LayoutSnapshot snapshot = layoutOf(denseDocument());

        assertThat(snapshot.totalPages())
                .describedAs("the fixture outgrows one page")
                .isGreaterThan(1);
        // One body, not one row per page: the old shape produced a separate
        // CvV2MintEditorialPageOne / PageTwo node, each pinned to its page.
        assertThat(snapshot.nodes())
                .describedAs("no per-page row survives")
                .noneMatch(node -> node.entityName() != null
                        && node.entityName().contains("PageOne"))
                .noneMatch(node -> node.entityName() != null
                        && node.entityName().contains("PageTwo"));
        assertThat(lastPageOf(snapshot, "CvV2MintEditorialSidebar"))
                .describedAs("the sidebar continues past the first page")
                .isGreaterThan(0);
    }

    @Test
    void everyBlockHeadingIsBoundToTheBlockItIntroduces() {
        // A column breaks between its children, so a heading can close a page with
        // its block overleaf. Asserted on the tree: a fixture that happens to break
        // at a heading is a coincidence, and one that stops breaking there would
        // take the guard with it.
        try (DocumentSession session = newSession()) {
            MintEditorial.create().compose(session, denseDocument());

            List<DocumentNode> headings = new ArrayList<>();
            collectHeadings(session.roots(), headings);

            assertThat(headings)
                    .describedAs("the fixture draws blocks in both columns")
                    .hasSizeGreaterThanOrEqualTo(4);
            assertThat(headings)
                    .allSatisfy(heading -> assertThat(heading.keepWithNext())
                            .describedAs("heading '%s' is kept with its block", heading.name())
                            .isTrue());
        }
    }

    // -- helpers -----------------------------------------------------------

    private static void collectHeadings(List<DocumentNode> nodes, List<DocumentNode> sink) {
        for (DocumentNode node : nodes) {
            if (node.name() != null && node.name().endsWith("BlockHeading")) {
                sink.add(node);
            }
            collectHeadings(node.children(), sink);
        }
    }

    private static LayoutNodeSnapshot column(LayoutSnapshot snapshot, String name) {
        return snapshot.nodes().stream()
                .filter(node -> name.equals(node.entityName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no column named " + name));
    }

    private static int lastPageOf(LayoutSnapshot snapshot, String columnName) {
        return column(snapshot, columnName).endPage();
    }

    private static String renderText(CvDocument doc) throws Exception {
        byte[] pdf;
        try (DocumentSession session = newSession()) {
            MintEditorial.create().compose(session, doc);
            pdf = session.toPdfBytes();
        }
        try (PDDocument document = Loader.loadPDF(pdf)) {
            // Every heading and entry title in this preset is letter-spaced, so
            // the text layer carries them one character at a time. Squeezing all
            // whitespace out is the only comparison that survives that, which is
            // why the expected strings above carry no spaces either.
            return new PDFTextStripper().getText(document).replaceAll("\\s+", "");
        }
    }

    private static LayoutSnapshot layoutOf(CvDocument doc) throws Exception {
        try (DocumentSession session = newSession()) {
            MintEditorial.create().compose(session, doc);
            return session.layoutSnapshot();
        }
    }

    private static DocumentSession newSession() {
        return GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(MintEditorial.RECOMMENDED_MARGIN))
                .create();
    }

    /** Six jobs and seven expertise groups — past every block this preset bounded. */
    private static CvDocument denseDocument() {
        List<CvSection> sections = new ArrayList<>();
        sections.add(new ParagraphSection("Professional Summary",
                "Platform engineer building document-generation pipelines."));

        sections.add(SkillsSection.builder("Technical Skills")
                .leveledGroup("Languages", List.of(
                        CvSkill.of("Java 21", 0.95), CvSkill.of("Kotlin", 0.85),
                        CvSkill.of("Groovy", 0.7)))
                .leveledGroup("Document & Print", List.of(
                        CvSkill.of("PDFBox", 0.9), CvSkill.of("PostScript", 0.6),
                        CvSkill.of("font metrics", 0.7)))
                .leveledGroup("Layout engines", List.of(
                        CvSkill.of("pagination", 0.85), CvSkill.of("Python", 0.75)))
                .leveledGroup("Build & infrastructure", List.of(
                        CvSkill.of("SQL", 0.8), CvSkill.of("GPG signing", 0.7)))
                .leveledGroup("Testing", List.of(CvSkill.of("AssertJ", 0.85)))
                .leveledGroup("Distribution", List.of(CvSkill.of("Maven Central", 0.8)))
                .leveledGroup("Operations", List.of(CvSkill.of("Docker", 0.7)))
                .build());

        sections.add(EntriesSection.builder("Education & Certifications")
                .entry("MSc Computer Science", "University of Manchester",
                        "2019-2021", "Distinction.")
                .entry("BSc Software Engineering", "Imperial College London",
                        "2015-2019", "First-class honours.")
                .build());

        EntriesSection.Builder experience =
                EntriesSection.builder("Professional Experience");
        experience.entry("Principal Platform Engineer", "Acme Rendering",
                "2022-present", "Owns the rendering pipeline and its release train.");
        experience.entry("Senior Backend Engineer", "Northwind Data",
                "2020-2022", "Built the typed reporting layer.");
        experience.entry("Backend Engineer", "Helios Print",
                "2018-2020", "Migrated the print queue off cron scripts.");
        experience.entry("Software Engineer", "Meridian Labs",
                "2016-2018", "Instrumented the batch exporters.");
        experience.entry("Junior Developer", "Nikoplast",
                "2015-2016", "Maintained the intranet document tooling.");
        experience.entry("Placement Engineer", "Harbour Systems",
                "2014-2015", "Wrote the berth-allocation reports.");
        sections.add(experience.build());

        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jordan", "Rivera")
                        .jobTitle("Platform Engineer")
                        .contact("+44 20 5555 1000", "jordan@example.com", "London, UK")
                        .link("LinkedIn", "https://linkedin.com/in/jordan-rivera-demo")
                        .link("GitHub", "https://github.com/jrivera-demo")
                        .build())
                .sections(sections.toArray(new CvSection[0]))
                .build();
    }
}
