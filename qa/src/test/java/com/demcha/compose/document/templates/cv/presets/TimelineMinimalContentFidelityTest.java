package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.snapshot.LayoutSnapshot;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A CV rendered through Timeline Minimal keeps every entry it was given.
 *
 * <p>A preset that drops content is worse than one that fails: the PDF looks
 * finished, the page is not obviously full, and the reader has no way to know
 * a job, a degree, or a whole section was never drawn. The three ways this
 * preset used to lose data are each pinned here, because each is silent.</p>
 *
 * <p>The fixture is deliberately denser than a one-page CV. Content that does
 * not fit belongs on page two — the engine paginates — not in the bin.</p>
 */
class TimelineMinimalContentFidelityTest {

    @Test
    void everyEntryOfADenseCvSurvivesIntoThePdf() throws Exception {
        String text = renderText(TimelineMinimal.create(), denseDocument());

        // Tail entries of each module: the ones a per-module cap dropped first.
        assertThat(text)
                .describedAs("every education entry must reach the page; the 4th "
                        + "was the first casualty of the 5-line sidebar cap")
                .contains("MSc Computer Science")
                .contains("University of Leeds")
                .contains("Cloud Architect")
                .contains("BSc Hydraulic Engineering");

        assertThat(text)
                .describedAs("every employer must reach the page")
                .contains("Acme Rendering")
                .contains("Northwind Data")
                .contains("Nikoplast");

        assertThat(text)
                .describedAs("every project must reach the page")
                .contains("GraphCompose")
                .contains("Ledger Sync")
                .contains("Spring Cart API");

        assertThat(text)
                .describedAs("every skill group must reach the page")
                .contains("Languages")
                .contains("Frameworks")
                .contains("AssertJ")
                .contains("Build")
                .contains("Datastores")
                .contains("Messaging")
                .contains("Observability")
                .contains("Kubernetes");
    }

    @Test
    void aSectionMatchingNoKnownCategoryIsStillRenderedUnderItsOwnTitle() throws Exception {
        String text = renderText(TimelineMinimal.create(), denseDocument());

        // Headings render in caps, so compare without case.
        assertThat(text)
                .describedAs("an arbitrary section title matched none of the preset's "
                        + "keyword lists and was discarded without a trace")
                .containsIgnoringCase("Awards")
                .contains("Rising Star");
    }

    @Test
    void aSecondSectionOfAKnownCategoryIsNotSwallowedByTheFirst() throws Exception {
        String text = renderText(TimelineMinimal.create(), denseDocument());

        assertThat(text)
                .describedAs("firstMatching consumed only the first title matching "
                        + "the summary keys, so the second prose section vanished")
                .containsIgnoringCase("Open Source")
                .contains("Maintains a document engine");
    }

    @Test
    void aLeftoverParagraphIsStyledAsProseRatherThanAsABullet() throws Exception {
        // Two prose sections carrying the same body. One title matches the
        // summary keys and takes the prose path; the other matches nothing and
        // arrives through the leftover path. Identical text through both paths
        // has to occupy identical space — bullet styling is a smaller face on
        // tighter leading, so the block shrinks the moment a paragraph is
        // treated as a list.
        String body = "Maintains a document engine used in production, "
                      + "and the tooling that keeps its output reproducible.";
        CvDocument doc = CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jane", "Doe")
                        .jobTitle("Backend Engineer")
                        .contact("+44 0", "j@d.com", "London")
                        .build())
                .sections(new ParagraphSection("Professional Summary", body),
                        new ParagraphSection("Open Source", body))
                .build();

        LayoutSnapshot snapshot = layoutOf(doc);

        assertThat(mainSectionHeight(snapshot, "opensource"))
                .describedAs("the leftover path bulleted every section it "
                        + "handled, so a paragraph arriving that way was set "
                        + "as a list instead of as prose")
                .isEqualTo(mainSectionHeight(snapshot, "professionalsummary"),
                        within(0.01));
    }

    /** Placement height of one main-column module block, by normalised title. */
    private static double mainSectionHeight(LayoutSnapshot snapshot, String key) {
        String name = "CvV2TimelineMinimalMain" + key;
        return snapshot.nodes().stream()
                .filter(node -> name.equals(node.entityName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "no main-column block named " + name))
                .placementHeight();
    }

    private static LayoutSnapshot layoutOf(CvDocument doc) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(TimelineMinimal.RECOMMENDED_MARGIN))
                .create()) {
            TimelineMinimal.create().compose(session, doc);
            return session.layoutSnapshot();
        }
    }

    @Test
    void proseIsNotCutWithAnEllipsisWhileThePageStillHasRoom() throws Exception {
        String text = renderText(TimelineMinimal.create(), denseDocument());

        assertThat(text)
                .describedAs("the summary was clipped mid-sentence by a character "
                        + "budget rather than by what actually fits")
                .contains("closing sentence of the profile");
    }

    @Test
    void aCvWithATallContactStackStillRendersEveryEntry() throws Exception {
        // The masthead grows a line per contact and per link, and the body
        // budget is what is left of the page after it. Thirteen contact rows
        // is far past anything the other fixtures exercise; the pagination has
        // to absorb that without dropping content at either end.
        CvIdentity.Builder identity = CvIdentity.builder()
                .name("Jane", "Doe")
                .jobTitle("Backend Engineer")
                .contact("+44 0", "j@d.com", "London");
        for (int i = 1; i <= 10; i++) {
            identity.link("Profile " + i, "https://example.com/" + i);
        }

        CvDocument doc = CvDocument.builder()
                .identity(identity.build())
                .sections(denseDocument().sections().toArray(new CvSection[0]))
                .build();

        String text = renderText(TimelineMinimal.create(), doc);

        assertThat(text)
                .describedAs("the body has to give way to the taller masthead "
                        + "without losing what it was holding")
                .contains("BSc Hydraulic Engineering")
                .contains("Nikoplast")
                .containsIgnoringCase("Awards");
    }

    private static String renderText(DocumentTemplate<CvDocument> template,
                                     CvDocument doc) throws Exception {
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(TimelineMinimal.RECOMMENDED_MARGIN))
                .create()) {
            template.compose(session, doc);
            pdf = session.toPdfBytes();
        }
        try (PDDocument document = Loader.loadPDF(pdf)) {
            // Collapse the layout's own line breaks: this asks whether a
            // phrase reached the page at all, and where the engine chose to
            // wrap it is the visual regression tests' business, not this
            // test's. Ligatures are a separate matter — PDFBox drops the
            // glyph for "ft"/"ti"/"fk", so "Software" extracts as "So ware";
            // the assertions below stay clear of those letter pairs.
            String text = new PDFTextStripper().getText(document);
            return text.replaceAll("\\s+", " ");
        }
    }

    /**
     * Four education entries, three employers, three projects, eight skill
     * groups, a second prose section, and a section whose title matches none
     * of the preset's keyword lists.
     */
    private static CvDocument denseDocument() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jane", "Doe")
                        .jobTitle("Backend Engineer")
                        .contact("+44 0", "j@d.com", "London")
                        .link("GitHub", "https://github.com/jane")
                        .build())
                .sections(
                        // Comfortably past the 245-character budget the preset
                        // used to clip prose at, so the closing words are only
                        // present if nothing clipped them.
                        new ParagraphSection("Professional Summary",
                                "Builds reliable document pipelines across the JVM, "
                                + "with a decade spent on rendering, layout and the "
                                + "unglamorous parts of typesetting that decide "
                                + "whether a page is readable rather than merely "
                                + "correct, from hyphenation and widow control to "
                                + "the way a table behaves when it meets the foot "
                                + "of a page. This is the "
                                + "closing sentence of the profile."),
                        new ParagraphSection("Open Source",
                                "Maintains a document engine used in production."),
                        SkillsSection.builder("Technical Skills")
                                .group("Languages", "Java 21", "Kotlin")
                                .group("Frameworks", "Spring Boot", "Quarkus")
                                .group("Testing", "JUnit 5", "AssertJ")
                                .group("Build", "Maven", "Gradle")
                                .group("Datastores", "PostgreSQL", "Redis")
                                .group("Messaging", "Kafka", "RabbitMQ")
                                .group("Observability", "Micrometer", "Grafana")
                                .group("Cloud", "AWS", "Kubernetes")
                                .build(),
                        EntriesSection.builder("Education & Certifications")
                                .entry("MSc Computer Science",
                                        "University of Manchester", "2019-2021", "")
                                .entry("BEng Software Engineering",
                                        "University of Leeds", "2015-2019", "")
                                .entry("Cloud Architect Certification",
                                        "AWS", "2022", "")
                                .entry("BSc Hydraulic Engineering",
                                        "Kyiv Polytechnic", "2011-2015", "")
                                .build(),
                        RowsSection.builder("Projects", RowStyle.BULLETED_STACKED)
                                .row("GraphCompose", "Declarative PDF layout engine.")
                                .row("Ledger Sync", "Double-entry reconciliation service.")
                                .row("Spring Cart API", "Storefront checkout backend.")
                                .build(),
                        EntriesSection.builder("Professional Experience")
                                .entry("Senior Engineer", "Acme Rendering",
                                        "2021-2024", "Built rendering services.")
                                .entry("Engineer", "Northwind Data",
                                        "2018-2021", "Owned the ingestion pipeline.")
                                .entry("Junior Engineer", "Nikoplast",
                                        "2015-2018", "Maintained the order system.")
                                .build(),
                        RowsSection.builder("Awards", RowStyle.PLAIN)
                                .row("Rising Star", "Engineering award, 2019")
                                .build(),
                        RowsSection.builder("Additional Information", RowStyle.PLAIN)
                                .row("Languages", "English, German")
                                .build())
                .build();
    }
}
