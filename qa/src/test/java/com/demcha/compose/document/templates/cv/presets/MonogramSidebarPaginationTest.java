package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.snapshot.LayoutNodeSnapshot;
import com.demcha.compose.document.snapshot.LayoutSnapshot;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvSection;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.RowStyle;
import com.demcha.compose.document.templates.cv.data.RowsSection;
import com.demcha.compose.document.templates.core.page.ContinuationSafeArea;
import com.demcha.compose.document.templates.cv.data.SkillsSection;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A CV denser than one page keeps every entry Monogram Sidebar was given, and the
 * pages it needs.
 *
 * <p>The preset used to hold its body in one row. A row is atomic — it must fit
 * the page it starts on — so the preset capped each block (two jobs, two degrees,
 * seven skills, three projects, three additional rows) and dropped the rest
 * without a word: the PDF looked finished and a reader had no way to know a job
 * was never drawn. The body is a column flow now, so content that does not fit
 * belongs on page two rather than in the bin.</p>
 *
 * <p>The entry subtitle — the employer — is not asserted here because this preset
 * draws position, date and description but never the subtitle. That omission and
 * its fix live in their own change; this one is about what the caps and the atomic
 * row cost.</p>
 */
class MonogramSidebarPaginationTest {

    /**
     * The floor a continuation page has to clear regardless of what the preset
     * reserves — past the widest non-printable band in common consumer printers.
     */
    private static final double PRINTER_SAFE_MINIMUM = 24.0;

    @Test
    void everyEntryOfADenseCvSurvivesIntoThePdf() throws Exception {
        String text = renderText(denseDocument());

        assertThat(text)
                .describedAs("sidebar: every degree, not the first two")
                .contains("University of Manchester")
                .contains("Imperial College London")
                .contains("Open University")
                .contains("University of Leeds");

        assertThat(text)
                .describedAs("sidebar: every skill, not the first seven")
                .contains("Groovy", "Python", "SQL")
                .contains("PDFBox", "PostScript", "ICC colour profiles", "font metrics")
                .contains("JUnit 5", "AssertJ", "Pitest");

        assertThat(text)
                .describedAs("main column: every position, not the first two")
                .contains("PRINCIPAL PLATFORM ENGINEER")
                .contains("SENIOR BACKEND ENGINEER")
                .contains("BACKEND ENGINEER")
                .contains("SOFTWARE ENGINEER")
                .contains("JUNIOR DEVELOPER");

        assertThat(text)
                .describedAs("main column: every project, not the first three")
                .contains("GraphCompose")
                .contains("Ledger Sync")
                .contains("Field Atlas")
                .contains("Harbour Watch");

        assertThat(text)
                .describedAs("main column: every additional row, not the first three")
                .contains("Languages")
                .contains("Work Eligibility")
                .contains("Open Source")
                .contains("Speaking");
    }

    @Test
    void aDenseCvPaginatesInsteadOfOverflowing() throws Exception {
        LayoutSnapshot snapshot = layoutOf(denseDocument());

        assertThat(snapshot.totalPages())
                .describedAs("the fixture outgrows one page; a row would have thrown "
                        + "here rather than opened a second one")
                .isGreaterThan(1);
        // Only the sidebar outgrows page one on this fixture — the main column
        // finishes on it — which is the point of a column flow: each column takes
        // the pages it needs, and neither waits for the other.
        assertThat(lastPageOf(snapshot, "CvV2MonogramSidebarSidebar"))
                .describedAs("the sidebar continues past the first page")
                .isGreaterThan(0);
        assertThat(lastPageOf(snapshot, "CvV2MonogramSidebarMain"))
                .describedAs("while the main column finishes on the first — every "
                        + "column of a flow opens on the same page, so only where it "
                        + "ends says anything")
                .isZero();
    }

    @Test
    void theColumnsStaySideBySide() throws Exception {
        LayoutSnapshot snapshot = layoutOf(denseDocument());
        LayoutNodeSnapshot sidebar = column(snapshot, "CvV2MonogramSidebarSidebar");
        LayoutNodeSnapshot main = column(snapshot, "CvV2MonogramSidebarMain");

        assertThat(sidebar.placementX())
                .describedAs("the sidebar is the left column")
                .isLessThan(main.placementX());
        assertThat(sidebar.startPage())
                .describedAs("and both columns open on the same page")
                .isEqualTo(main.startPage());
    }

    @Test
    void aContinuationPageKeepsItsFirstLineOffTheTrimmedEdge() throws Exception {
        // RECOMMENDED_MARGIN is 0 so the sidebar fill reaches the paper edge, and
        // the columns' padding is an edge of the columns rather than of each page.
        try (DocumentSession session = newSession()) {
            MonogramSidebar.create().compose(session, denseDocument());
            LayoutGraph graph = session.layoutGraph();
            double pageHeight = session.canvas().height();

            assertThat(graph.totalPages()).isGreaterThan(1);
            for (int page = 1; page < graph.totalPages(); page++) {
                double gap = firstLineTopGap(graph, pageHeight, page);
                // Two floors. The literal is the requirement and does not move when
                // the preset changes its mind; the constant is what the preset
                // actually asks for, so reserving less than it advertises is named
                // as that rather than passing silently.
                assertThat(gap)
                        .describedAs("page %d clears the widest common non-printable band",
                                page + 1)
                        .isGreaterThanOrEqualTo(PRINTER_SAFE_MINIMUM);
                assertThat(gap)
                        .describedAs("page %d gets the inset the preset asks for", page + 1)
                        .isGreaterThanOrEqualTo(ContinuationSafeArea.PRINTER_SAFE_TOP);
            }
        }

        // The same document with the preset's rule taken back off, so the number
        // above is attributable to the rule rather than to whatever else happened to
        // sit at the top of the page.
        try (DocumentSession session = newSession()) {
            MonogramSidebar.create().compose(session, denseDocument());
            session.pageMargins(List.of());
            LayoutGraph graph = session.layoutGraph();

            assertThat(firstLineTopGap(graph, session.canvas().height(), 1))
                    .describedAs("without the rule the first line lands on the trimmed edge")
                    .isLessThan(PRINTER_SAFE_MINIMUM);
        }
    }

    @Test
    void theFirstPageKeepsTheTopEdgeItsOwnDesignGaveIt() throws Exception {
        // A safe area applied to every page rather than to the continuations would
        // push the monogram badge and the whole first page down with it.
        try (DocumentSession session = newSession()) {
            MonogramSidebar.create().compose(session, denseDocument());
            double withRule = firstLineTopGap(
                    session.layoutGraph(), session.canvas().height(), 0);

            session.pageMargins(List.of());
            double withoutRule = firstLineTopGap(
                    session.layoutGraph(), session.canvas().height(), 0);

            assertThat(withRule)
                    .describedAs("page one is laid out identically either way")
                    .isCloseTo(withoutRule, within(0.01));
        }
    }

    @Test
    void everySectionHeadingIsBoundToTheBlockItIntroduces() {
        // Once a column breaks at all, a heading can close a page with its list
        // overleaf, or be parted from its own rule — neither of which an atomic row
        // could do. Asserted on the tree rather than on a rendered page because a
        // fixture that happens to break at a heading is a coincidence, and one that
        // stops breaking there would take the guard with it. The pagination
        // behaviour of the flag itself is the engine's own SectionKeepWithNextTest.
        try (DocumentSession session = newSession()) {
            MonogramSidebar.create().compose(session, denseDocument());

            List<DocumentNode> headings = new ArrayList<>();
            collectHeadings(session.roots(), headings);

            assertThat(headings)
                    .describedAs("both columns draw headings, and each is its own group")
                    .hasSizeGreaterThanOrEqualTo(2);
            assertThat(headings)
                    .allSatisfy(heading -> assertThat(heading.keepWithNext())
                            .describedAs("heading '%s' is kept with its block",
                                    heading.name())
                            .isTrue());
        }
    }

    // -- helpers -----------------------------------------------------------

    private static void collectHeadings(List<DocumentNode> nodes, List<DocumentNode> sink) {
        for (DocumentNode node : nodes) {
            if (node.name() != null && node.name().endsWith("Heading")) {
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

    private static double firstLineTopGap(LayoutGraph graph, double pageHeight, int pageIndex) {
        return graph.fragments().stream()
                .filter(fragment -> fragment.pageIndex() == pageIndex)
                .filter(fragment -> fragment.payload() instanceof ParagraphFragmentPayload)
                .mapToDouble(fragment -> pageHeight - (fragment.y() + fragment.height()))
                .min()
                .orElseThrow(() -> new AssertionError("no text on page " + (pageIndex + 1)));
    }

    private static String renderText(CvDocument doc) throws Exception {
        byte[] pdf;
        try (DocumentSession session = newSession()) {
            MonogramSidebar.create().compose(session, doc);
            pdf = session.toPdfBytes();
        }
        try (PDDocument document = Loader.loadPDF(pdf)) {
            // Collapsed, so an item that reached the page but wrapped still counts.
            // (PDFBox's Latin GSUB also eats "ti" in the text layer, which is why no
            // assertion here spans that pair.)
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }

    private static LayoutSnapshot layoutOf(CvDocument doc) throws Exception {
        try (DocumentSession session = newSession()) {
            MonogramSidebar.create().compose(session, doc);
            return session.layoutSnapshot();
        }
    }

    private static DocumentSession newSession() {
        return GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(MonogramSidebar.RECOMMENDED_MARGIN))
                .create();
    }

    /** A career denser than one page: five jobs, four degrees, a dozen skills. */
    private static CvDocument denseDocument() {
        List<CvSection> sections = new ArrayList<>();
        sections.add(new ParagraphSection("Professional Summary",
                "Platform engineer building document-generation pipelines, layout "
                        + "engines, and developer-facing template systems."));

        sections.add(SkillsSection.builder("Expertise")
                .group("Languages", "Java 21", "Kotlin", "Groovy", "Python", "SQL")
                .group("Document & Print", "PDFBox", "PostScript",
                        "ICC colour profiles", "font metrics")
                .group("Testing", "JUnit 5", "AssertJ", "Pitest")
                .build());

        sections.add(EntriesSection.builder("Education & Certifications")
                .entry("MSc Computer Science", "University of Manchester",
                        "2019-2021", "Distinction.")
                .entry("BSc Software Engineering", "Imperial College London",
                        "2015-2019", "First-class honours.")
                .entry("Oracle Java Certification", "Open University",
                        "2023-2024", "Java 17 platform deep-dive.")
                .entry("BSc Hydraulic Engineering", "University of Leeds",
                        "2011-2015", "Fluid dynamics and structural analysis.")
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
        sections.add(experience.build());

        sections.add(RowsSection.builder("Projects", RowStyle.BULLETED_STACKED)
                .row("GraphCompose (Java 21, PDFBox)",
                        "Declarative Java PDF layout engine.")
                .row("Ledger Sync (Kotlin, Postgres)",
                        "Reconciles billing ledgers across three regions.")
                .row("Field Atlas (Java, MapLibre)",
                        "Offline survey maps for field crews.")
                .row("Harbour Watch (Java, Kafka)",
                        "Streams berth telemetry into an operations dashboard.")
                .build());

        sections.add(RowsSection.builder("Additional Information", RowStyle.PLAIN)
                .row("Languages", "English (Fluent), German (Intermediate)")
                .row("Work Eligibility", "Eligible to work in the UK and the EU")
                .row("Open Source", "Maintainer of GraphCompose")
                .row("Speaking", "JVM Summit 2024, Devoxx UK 2025")
                .build());

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
