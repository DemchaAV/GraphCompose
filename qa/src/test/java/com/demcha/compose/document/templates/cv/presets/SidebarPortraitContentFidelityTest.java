package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
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
import com.demcha.compose.document.templates.cv.data.SkillsSection;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A CV rendered through Sidebar Portrait keeps every entry it was given.
 *
 * <p>The preset used to hold its body in one row. A row is atomic — it must
 * fit the page it starts on — so the preset capped each block (two jobs, two
 * degrees, five skills, three languages, two projects) and dropped the rest
 * without a word: the PDF looked finished and a reader had no way to know a
 * job was never drawn. The body is a column flow now, so the content that
 * does not fit belongs on page two rather than in the bin.</p>
 *
 * <p>Every assertion below names an item the old caps dropped first, in both
 * columns, so a cap reintroduced anywhere fails here rather than shipping.</p>
 */
class SidebarPortraitContentFidelityTest {

    /**
     * The floor a continuation page has to clear regardless of what the preset
     * chooses to reserve — roughly 8.5mm, past the widest non-printable band in
     * common consumer printers. Stated here as a literal so the guard measures the
     * requirement rather than the preset's own constant.
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
                .describedAs("sidebar: every skill, not the first five")
                .contains("Java 21", "Kotlin", "Groovy", "Python", "SQL")
                .contains("PDFBox", "PostScript", "ICC colour profiles", "font metrics")
                .contains("JUnit 5", "AssertJ", "Pitest");

        assertThat(text)
                .describedAs("sidebar: every language, not the first three")
                .contains("ENGLISH", "GERMAN", "FRENCH", "UKRAINIAN", "SPANISH");

        assertThat(text)
                .describedAs("main column: every employer, not the first two")
                .contains("Acme Rendering")
                .contains("Northwind Data")
                .contains("Helios Print")
                .contains("Meridian Labs")
                .contains("Nikoplast");

        assertThat(text)
                .describedAs("main column: every project, not the first two")
                .contains("GraphCompose")
                .contains("Ledger Sync")
                .contains("Field Atlas");
    }

    @Test
    void aDenseCvPaginatesInsteadOfOverflowing() throws Exception {
        LayoutSnapshot snapshot = layoutOf(denseDocument());

        assertThat(snapshot.totalPages())
                .describedAs("the fixture outgrows one page; a row would have "
                        + "thrown here rather than opened a second one")
                .isGreaterThan(1);

        // Both columns have to reach the later pages. A flow that carried only
        // the main column over would leave the sidebar's tail behind — the same
        // silent loss the caps used to cause, one layer down. The column node's
        // own endPage says it directly, without depending on where a child
        // happens to start.
        assertThat(lastPageOf(snapshot, "CvV2SidebarPortraitSidebar"))
                .describedAs("the sidebar continues past the first page")
                .isGreaterThan(0);
        assertThat(lastPageOf(snapshot, "CvV2SidebarPortraitMain"))
                .describedAs("the main column continues past the first page")
                .isGreaterThan(0);
    }

    @Test
    void theColumnsStaySideBySide() throws Exception {
        // Stacking the columns instead of placing them beside each other would
        // satisfy every assertion above; only geometry rules it out.
        LayoutSnapshot snapshot = layoutOf(denseDocument());
        LayoutNodeSnapshot sidebar = column(snapshot, "CvV2SidebarPortraitSidebar");
        LayoutNodeSnapshot main = column(snapshot, "CvV2SidebarPortraitMain");

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
        // the columns' padding is an edge of the columns rather than of each page:
        // it holds page one's content down and says nothing about page two's. The
        // preset asks for a page-margin rule to cover the difference; this is the
        // measurement that says it arrived.
        try (DocumentSession session = newSession()) {
            SidebarPortrait.create().compose(session, denseDocument());
            LayoutGraph graph = session.layoutGraph();
            double pageHeight = session.canvas().height();

            assertThat(graph.totalPages())
                    .describedAs("the fixture has to reach a second page for this to mean anything")
                    .isGreaterThan(1);
            for (int page = 1; page < graph.totalPages(); page++) {
                double gap = firstLineTopGap(graph, pageHeight, page);
                // Two floors, deliberately. The literal is the requirement — it
                // does not move when the preset's constant does, so lowering
                // CONTINUATION_TOP_SAFE_AREA to zero fails here rather than
                // quietly turning this assertion into `>= 0`. The constant is the
                // preset's advertised promise, checked separately so a preset that
                // reserves less than it documents is named as that.
                assertThat(gap)
                        .describedAs("page %d clears the widest common non-printable band", page + 1)
                        .isGreaterThanOrEqualTo(PRINTER_SAFE_MINIMUM);
                assertThat(gap)
                        .describedAs("page %d gets the inset the preset advertises", page + 1)
                        .isGreaterThanOrEqualTo(SidebarPortrait.CONTINUATION_TOP_SAFE_AREA);
            }
        }

        // The same document with the preset's rule taken back off, so the number
        // above is attributed to the rule and not to whatever else is on the page.
        // A preset that stopped asking for the safe area fails the loop above; a
        // preset whose padding silently started covering it fails here.
        //
        // Measured as the difference between the two, not as a bound on either:
        // what starts a continuation page — a body line, a heading carrying a
        // top margin — moves whenever the fixture's content does, and a literal
        // read off one rendering has to be loosened every time it does. The
        // difference is the rule's own contribution and nothing else's.
        try (DocumentSession session = newSession()) {
            SidebarPortrait.create().compose(session, denseDocument());
            double withRule = firstLineTopGap(
                    session.layoutGraph(), session.canvas().height(), 1);

            session.pageMargins(List.of());
            double withoutRule = firstLineTopGap(
                    session.layoutGraph(), session.canvas().height(), 1);

            assertThat(withRule - withoutRule)
                    .describedAs("the inset on page two is the page-margin rule's, "
                            + "not something else on the page")
                    .isCloseTo(SidebarPortrait.CONTINUATION_TOP_SAFE_AREA,
                            org.assertj.core.data.Offset.offset(0.01));
            assertThat(withoutRule)
                    .describedAs("without the rule the first line lands inside the "
                            + "band a printer will not print")
                    .isLessThan(PRINTER_SAFE_MINIMUM);
        }
    }

    @Test
    void theFirstPageKeepsTheTopEdgeItsOwnDesignGaveIt() throws Exception {
        // A safe area applied to every page rather than to the continuations would
        // push the portrait, the hero strip and the whole first page down with it.
        try (DocumentSession session = newSession()) {
            SidebarPortrait.create().compose(session, denseDocument());
            double withRule = firstLineTopGap(
                    session.layoutGraph(), session.canvas().height(), 0);

            session.pageMargins(List.of());
            double withoutRule = firstLineTopGap(
                    session.layoutGraph(), session.canvas().height(), 0);

            assertThat(withRule)
                    .describedAs("page one is laid out identically either way")
                    .isCloseTo(withoutRule, org.assertj.core.data.Offset.offset(0.01));
            // Pinned as a number too: comparing the two runs alone would pass just
            // as well if the preset had stopped asking for a safe area at all. The
            // hero strip's 59pt top margin plus its 19pt padding is what puts the
            // first glyph here, and no page rule may move it.
            assertThat(withRule)
                    .describedAs("and it is the hero strip's own top edge, 59 + 19")
                    .isCloseTo(78.0, org.assertj.core.data.Offset.offset(0.5));
        }
    }

    // -- helpers -----------------------------------------------------------

    /**
     * Distance from the trimmed top edge to the top of the highest line of text on
     * a page. Measured on text fragments rather than placed nodes: a paragraph that
     * broke across the page boundary belongs to a node that started on the previous
     * page, and it is exactly that line the reader sees at the top of this one.
     */
    private static double firstLineTopGap(LayoutGraph graph, double pageHeight, int pageIndex) {
        return graph.fragments().stream()
                .filter(fragment -> fragment.pageIndex() == pageIndex)
                .filter(fragment -> fragment.payload() instanceof ParagraphFragmentPayload)
                .mapToDouble(fragment -> pageHeight - (fragment.y() + fragment.height()))
                .min()
                .orElseThrow(() -> new AssertionError("no text on page " + (pageIndex + 1)));
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
            SidebarPortrait.create().compose(session, doc);
            pdf = session.toPdfBytes();
        }
        try (PDDocument document = Loader.loadPDF(pdf)) {
            // Collapse wrapping the way the sibling fidelity test does: an item
            // that reached the page but broke across two lines is present, and
            // an assertion that failed on the line break would be reporting the
            // wrong thing. (PDFBox's Latin GSUB also eats "ti" in the text
            // layer, which is why no assertion here spans that pair.)
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }

    private static LayoutSnapshot layoutOf(CvDocument doc) throws Exception {
        try (DocumentSession session = newSession()) {
            SidebarPortrait.create().compose(session, doc);
            return session.layoutSnapshot();
        }
    }

    private static DocumentSession newSession() {
        return GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(SidebarPortrait.RECOMMENDED_MARGIN))
                .create();
    }

    /**
     * A career denser than any one page: five jobs, four degrees, a dozen
     * skills, five languages, three projects. Every block reaches past the
     * cap that used to truncate it.
     */
    private static CvDocument denseDocument() {
        List<CvSection> sections = new ArrayList<>();
        sections.add(new ParagraphSection("Professional Summary",
                "Platform engineer building document-generation pipelines, "
                        + "layout engines, and developer-facing template systems."));

        sections.add(SkillsSection.builder("Technical Skills")
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

        sections.add(RowsSection.builder("Languages", RowStyle.PLAIN)
                .row("Languages", "English (native), German (B2), French (B1), "
                        + "Ukrainian (C1), Spanish (A2)")
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
