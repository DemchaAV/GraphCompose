package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.exceptions.AtomicNodeTooLargeException;
import com.demcha.compose.document.snapshot.LayoutNodeSnapshot;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvSkill;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.SkillsSection;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Smoke test for {@link SerifHeadline} — proves the preset renders a
 * {@link CvDocument} end-to-end with its packaged marks, reaches its
 * channels as PDF links, and pins the contracts the class documents: the
 * entry marks are this preset's own vocabulary, an entry without one is
 * drawn plain, the sheet holds one page, and the two blocks whose titles
 * overlap reach the right berths.
 */
class SerifHeadlineSmokeTest {

    /** Everything right of this belongs to the aside, not the main rail. */
    private static final double MAIN_RAIL_RIGHT_EDGE = 380.0;

    private static byte[] render(CvDocument doc) throws Exception {
        // The preset owns its page geometry, so the session starts unconfigured.
        try (DocumentSession session = GraphCompose.document().create()) {
            SerifHeadline.create().compose(session, doc);
            assertThat(session.roots()).isNotEmpty();
            byte[] pdfBytes = session.toPdfBytes();
            assertThat(pdfBytes).isNotEmpty();
            return pdfBytes;
        }
    }

    private static String textOf(byte[] pdfBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static List<String> linkUris(byte[] pdfBytes) throws Exception {
        List<String> uris = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            for (PDPage page : document.getPages()) {
                for (PDAnnotation annotation : page.getAnnotations()) {
                    if (annotation instanceof PDAnnotationLink link
                            && link.getAction() instanceof PDActionURI action) {
                        uris.add(action.getURI());
                    }
                }
            }
        }
        return uris;
    }

    @Test
    void exposesStableIdentity() {
        DocumentTemplate<CvDocument> template = SerifHeadline.create();
        assertThat(template.id()).isEqualTo(SerifHeadline.ID);
        assertThat(template.displayName()).isEqualTo(SerifHeadline.DISPLAY_NAME);
    }

    @Test
    void canonicalRenderCarriesTheUntrackedText() throws Exception {
        // The headings, the name and the role are set letter by letter to
        // reach their tracking, so they arrive on the text layer spaced out.
        // What is asserted here is the text drawn as written.
        String text = textOf(render(SerifHeadlineFixtures.canonicalCv()));
        assertThat(text)
                .contains("alex.morgan@email.com")
                .contains("github.com/alexmorgan")
                .contains("Software Engineer with 5+ years")
                .contains("TechSolutions Inc.")
                .contains("Led a team of 4 engineers and mentored junior developers.")
                .contains("E-Commerce Platform")
                .contains("Java, Spring Boot, React, PostgreSQL, AWS")
                .contains("New York University")
                .contains("Kubernetes")
                .contains("Problem Solving   |   Communication")
                // The certification titles wrap inside their narrow column,
                // which is the design: assert either side of the break.
                .contains("Docker Certified")
                .contains("(DCA)")
                .contains("Improved system efficiency by 40%");
    }

    @Test
    void rendersOneCanonicalPage() throws Exception {
        try (PDDocument document =
                     Loader.loadPDF(render(SerifHeadlineFixtures.canonicalCv()))) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void everyHighlightLineStartsOnTheSameTextColumn() throws Exception {
        // Neither parity gate can see this. A list's items are not layout nodes,
        // so the snapshot records the list's box and nothing inside it, and two
        // continuation lines moving 1.5pt sits far inside the pixel budget. The
        // contract is that the dot has a column and the text has a column, and
        // that a highlight which wraps resumes on the text one — so it is read
        // off the glyph positions, which is where it actually lives.
        List<HighlightLine> lines = highlightLines(
                render(SerifHeadlineFixtures.canonicalCv()));

        assertThat(lines).filteredOn(line -> !line.wrapped())
                .as("every authored highlight is here")
                .hasSize(11);
        assertThat(lines).filteredOn(HighlightLine::wrapped)
                .as("and at least one of them wraps, with nothing of its own in the marker"
                    + " column — a continuation padded back to that column with spaces would"
                    + " land here too, and is what this is watching for")
                .isNotEmpty();

        double textColumn = lines.get(0).textX();
        double markerColumn = lines.stream().filter(line -> !line.wrapped())
                .mapToDouble(HighlightLine::markerX).findFirst().orElse(Double.NaN);
        assertThat(lines).allSatisfy(line -> assertThat(line.textX())
                .as("the %s line %s starts its text on the column",
                        line.wrapped() ? "wrapped" : "first", line.text())
                .isCloseTo(textColumn, within(0.01)));
        assertThat(lines).filteredOn(HighlightLine::wrapped)
                .as("a wrapped line resumes on the text, never in the marker column")
                .allSatisfy(line -> assertThat(line.markerX()).isNaN());

        // 6.750pt between the two columns: 4.683 of dot advance and the 2.067
        // the design authors as the gap. Pinned because it is the number the
        // migration had to solve for, and nothing else on the sheet would move
        // if it drifted.
        assertThat(textColumn - markerColumn)
                .as("the dot's advance plus the authored gap")
                .isCloseTo(6.750, within(0.01));
    }

    /** One rendered highlight line: where its dot is, if any, and where its text starts. */
    private record HighlightLine(double markerX, double textX, String text) {

        boolean wrapped() {
            return Double.isNaN(markerX);
        }
    }

    /**
     * The highlight lines of the canonical sheet, read off the page.
     *
     * <p>Scoped by the y-bands of the {@code Highlights_*} list boxes and to the
     * main rail's half of the sheet, so the aside's own text cannot wander in.</p>
     *
     * @param pdfBytes the rendered sheet
     * @return one entry per rendered line
     */
    private static List<HighlightLine> highlightLines(byte[] pdfBytes) throws Exception {
        List<double[]> bands = new ArrayList<>();
        try (DocumentSession session = GraphCompose.document().create()) {
            SerifHeadline.create().compose(session, SerifHeadlineFixtures.canonicalCv());
            for (LayoutNodeSnapshot node : session.layoutSnapshot().nodes()) {
                if (node.entityName() != null && node.entityName().startsWith("Highlights_")) {
                    bands.add(new double[] {
                            node.placementY(), node.placementY() + node.placementHeight()});
                }
            }
        }
        assertThat(bands).as("three highlight lists on the canonical sheet").hasSize(3);

        List<Glyph> glyphs = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            double pageHeight = document.getPage(0).getMediaBox().getHeight();
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) {
                    for (TextPosition position : positions) {
                        // Every glyph that belongs to a highlight, with the
                        // baseline it sits on. The call boundaries are no use
                        // here: a marker is its own text run, so a first line
                        // arrives as two calls, and a call can reach across both
                        // columns of the sheet. Grouping by baseline afterwards
                        // is what puts a dot back with its own text.
                        double y = pageHeight - position.getYDirAdj();
                        boolean inBand = bands.stream()
                                .anyMatch(band -> y > band[0] - 1.5 && y < band[1] + 1.5);
                        if (!inBand || position.getXDirAdj() > MAIN_RAIL_RIGHT_EDGE) {
                            continue;
                        }
                        glyphs.add(new Glyph(position.getXDirAdj(), y, position.getUnicode()));
                    }
                }
            };
            stripper.setSortByPosition(true);
            stripper.getText(document);
        }

        // The dot does not survive the text layer as "•" — its font carries no
        // ToUnicode for it, so it extracts as a replacement character. The
        // marker column is therefore the leftmost x any highlight glyph sits at,
        // read off the page rather than assumed, and a baseline with a glyph
        // there is one that shows a dot. That is what makes the interesting
        // number — where the TEXT starts — comparable between a first line and a
        // wrapped one.
        double markerColumn = glyphs.stream().mapToDouble(Glyph::x).min().orElse(Double.NaN);
        Map<Long, List<Glyph>> byBaseline = new TreeMap<>(Comparator.reverseOrder());
        for (Glyph glyph : glyphs) {
            byBaseline.computeIfAbsent(Math.round(glyph.y() * 4.0), key -> new ArrayList<>())
                    .add(glyph);
        }

        List<HighlightLine> lines = new ArrayList<>(byBaseline.size());
        for (List<Glyph> baseline : byBaseline.values()) {
            boolean hasMarker = baseline.stream()
                    .anyMatch(glyph -> Math.abs(glyph.x() - markerColumn) < 0.01);
            StringBuilder body = new StringBuilder();
            double textX = Double.NaN;
            for (Glyph glyph : baseline.stream().sorted(Comparator.comparingDouble(Glyph::x))
                    .toList()) {
                if (hasMarker && Math.abs(glyph.x() - markerColumn) < 0.01) {
                    continue;
                }
                if (Double.isNaN(textX) && !glyph.unicode().isBlank()) {
                    textX = glyph.x();
                }
                if (!Double.isNaN(textX)) {
                    body.append(glyph.unicode());
                }
            }
            if (!Double.isNaN(textX)) {
                lines.add(new HighlightLine(hasMarker ? markerColumn : Double.NaN, textX,
                        body.length() > 18 ? body.substring(0, 18) : body.toString()));
            }
        }
        return lines;
    }

    /** One extracted glyph: where it sits and what it is. */
    private record Glyph(double x, double y, String unicode) {
    }

    @Test
    void channelsReachThePdfAsLinks() throws Exception {
        // The dial and mail targets are built from the values. They are
        // annotations, so they move no pixel and no layout node — which is
        // why the parity gates cannot see them either.
        assertThat(linkUris(render(SerifHeadlineFixtures.canonicalCv())))
                .contains("tel:+15551234567",
                        "mailto:alex.morgan@email.com",
                        "https://www.linkedin.com/in/alexmorgan",
                        "https://github.com/alexmorgan");
    }

    @Test
    void rejectsAnUnknownEntryMarkByName() {
        // An icon token is data, so a wrong one is a data error — and the
        // message names the set a document may choose from.
        CvDocument doc = CvDocument.builder()
                .identity(identity())
                .section(EntriesSection.builder("Projects")
                        .entry(CvEntry.builder("Ledgerkit").icon("no-such-mark").build())
                        .build())
                .build();
        try (DocumentSession session = GraphCompose.document().create()) {
            assertThatThrownBy(() -> SerifHeadline.create().compose(session, doc))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no-such-mark")
                    .hasMessageContaining("rocket");
        }
    }

    @Test
    void anEntryWithALinkReachesThePdfAsOne() throws Exception {
        // Every title this preset draws is a link when its entry carries
        // one. It is an annotation, so it moves no pixel and no layout node
        // — which is why neither parity gate can see it.
        byte[] pdf = render(CvDocument.builder()
                .identity(identity())
                .section(EntriesSection.builder("Projects")
                        .entry(CvEntry.builder("Ledgerkit")
                                .subtitle("Java")
                                .body("A ledger.")
                                .icon("api")
                                .link("https://example.com/ledgerkit")
                                .build())
                        .build())
                .section(EntriesSection.builder("Experience")
                        .entry(CvEntry.builder("Engineer")
                                .subtitle("Acme")
                                .date("2024")
                                .body("Shipped a thing.")
                                .link("https://acme.example.com")
                                .build())
                        .build())
                .section(EntriesSection.builder("Achievements")
                        .entry(CvEntry.builder("Engineer of the Year")
                                .body("For the platform work.")
                                .icon("trophy")
                                .link("https://example.com/award")
                                .build())
                        .build())
                .build());
        assertThat(linkUris(pdf))
                .contains("https://example.com/ledgerkit",
                        "https://acme.example.com",
                        "https://example.com/award");
    }

    @Test
    void drawsAnEntryWithoutAMark() throws Exception {
        // No token, no plate — the card is drawn, just unmarked.
        String text = textOf(render(CvDocument.builder()
                .identity(identity())
                .section(EntriesSection.builder("Projects")
                        .entry(CvEntry.builder("Ledgerkit")
                                .subtitle("Java")
                                .body("A ledger.")
                                .build())
                        .build())
                .build()));
        assertThat(text).contains("Ledgerkit").contains("A ledger.");
    }

    @Test
    void softSkillsTakeTheirOwnBerthRatherThanTheSkillsOne() throws Exception {
        // "Soft Skills" names the skills berth too. The block with a shape of
        // its own wins it, so both are drawn rather than one swallowing the
        // other.
        String text = textOf(render(CvDocument.builder()
                .identity(identity())
                .section(SkillsSection.builder("Skills")
                        .leveledGroup("Languages", List.of(CvSkill.of("Java", 0.8)))
                        .build())
                .section(new ParagraphSection("Soft Skills", "Teamwork   |   Curiosity"))
                .build()));
        assertThat(text).contains("Java").contains("Teamwork   |   Curiosity");
    }

    @Test
    void drawsTheEmployerWithoutAPlace() throws Exception {
        // The pipe belongs to the location; an entry with no place gets the
        // employer alone rather than a line ending in a separator.
        String text = textOf(render(CvDocument.builder()
                .identity(identity())
                .section(EntriesSection.builder("Experience")
                        .entry(CvEntry.builder("Engineer")
                                .subtitle("Acme")
                                .date("2024")
                                .body("Shipped a thing.")
                                .build())
                        .build())
                .build()));
        assertThat(text).contains("Acme").doesNotContain("Acme   |");
    }

    @Test
    void refusesACvLongerThanTheSheet() {
        // The sheet holds one page: the body is a single row, and a row is
        // atomic. A CV past that raises rather than flowing.
        EntriesSection.Builder experience = EntriesSection.builder("Experience");
        for (int i = 0; i < 14; i++) {
            experience.entry(CvEntry.builder("Role " + i)
                    .subtitle("Employer " + i)
                    .place("City")
                    .date("2010 - 2011")
                    .body(List.of(
                            "Did a substantial thing that took a full line of text here.",
                            "Did a second substantial thing, also a full line of text.",
                            "And a third, so each role occupies real vertical space."))
                    .build());
        }
        CvDocument doc = CvDocument.builder()
                .identity(identity())
                .section(experience.build())
                .build();
        try (DocumentSession session = GraphCompose.document().create()) {
            SerifHeadline.create().compose(session, doc);
            assertThatThrownBy(session::toPdfBytes)
                    .isInstanceOf(AtomicNodeTooLargeException.class)
                    .hasMessageContaining("BodyColumns");
        }
    }

    private static CvIdentity identity() {
        return CvIdentity.builder()
                .name("Ada", "Lovelace")
                .jobTitle("Engineer")
                .contact(new Contact("+44 20 7946 0000", "ada@example.com", "London, UK"))
                .link(new Link("github.com/ada", "https://github.com/ada"))
                .build();
    }
}
