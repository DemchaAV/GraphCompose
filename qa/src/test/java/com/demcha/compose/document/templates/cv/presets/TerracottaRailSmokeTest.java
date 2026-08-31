package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvName;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.Slot;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Smoke test for {@link TerracottaRail} — proves the preset renders a
 * {@link CvDocument} end-to-end with its packaged marks, draws the monogram
 * from the name, reports an unknown icon token as a data error, refuses a CV
 * longer than its sheet rather than losing an entry, and carries the contact
 * channels and a project title as link annotations, which move no pixel and
 * no layout node so neither gate would notice them going missing.
 */
class TerracottaRailSmokeTest {

    private static byte[] render(CvDocument doc) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(0f, 0f, 0f, 0f)
                .create()) {
            TerracottaRail.create().compose(session, doc);
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

    /**
     * The text with its spaces removed. The sheet's headings and its masthead
     * are letter-spaced with real space characters, so what is being asserted
     * is the wording, not the tracking.
     */
    private static String compact(String text) {
        return text.replaceAll("(?U)\\s", "");
    }

    private static List<String> linkTargets(byte[] pdfBytes) throws Exception {
        List<String> targets = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            for (PDPage page : document.getPages()) {
                for (PDAnnotation annotation : page.getAnnotations()) {
                    if (annotation instanceof PDAnnotationLink link
                            && link.getAction() instanceof PDActionURI uri) {
                        targets.add(uri.getURI());
                    }
                }
            }
        }
        return targets;
    }

    /** The canonical document with one section replaced. */
    private static CvDocument withSection(Slot slot, EntriesSection replacement,
                                          String replacedTitle) {
        List<CvDocument.Placement> placements = new ArrayList<>();
        for (CvDocument.Placement placement : TerracottaRailFixtures.canonicalCv().placements()) {
            if (placement.section().title().equals(replacedTitle)) {
                placements.add(new CvDocument.Placement(slot, replacement));
            } else {
                placements.add(placement);
            }
        }
        return new CvDocument(TerracottaRailFixtures.identity(), placements);
    }

    @Test
    void exposesStableIdentity() {
        DocumentTemplate<CvDocument> template = TerracottaRail.create();
        assertThat(template.id()).isEqualTo(TerracottaRail.ID);
        assertThat(template.displayName()).isEqualTo(TerracottaRail.DISPLAY_NAME);
    }

    @Test
    void rendersCanonicalCvWithPackagedMarks() throws Exception {
        render(TerracottaRailFixtures.canonicalCv());
    }

    @Test
    void canonicalRenderCarriesEveryColumnsText() throws Exception {
        String text = textOf(render(TerracottaRailFixtures.canonicalCv()));
        assertThat(text)
                .contains("oliver.bennett@email.com")
                .contains("Architectural Design")
                .contains("Microsoft Office")
                .contains("ARB Registered Architect")
                .contains("Northline Studio, Bristol, UK")
                .contains("Harbour Point")
                .contains("University of Sheffield");
        assertThat(compact(text))
                .contains("OLIVERBENNETT")
                .contains("SENIORARCHITECT")
                .contains("PROFESSIONALEXPERIENCE");
    }

    @Test
    void theMonogramIsDrawnFromTheName() throws Exception {
        // Two capitals on their own line, and no field of their own in the
        // model: the initials are the whole source.
        String text = textOf(render(TerracottaRailFixtures.canonicalCv()));
        assertThat(text.lines().map(String::strip).toList()).contains("OB");
    }

    @Test
    void contactChannelsAndLinksAreClickable() throws Exception {
        List<String> targets = linkTargets(render(TerracottaRailFixtures.canonicalCv()));
        assertThat(targets)
                .contains("mailto:oliver.bennett@email.com")
                .contains("tel:+447700900123")
                .contains("https://linkedin.com/in/oliverbennett-architect");
    }

    @Test
    void aLinkShowsItsLabelAndHidesItsAddress() throws Exception {
        // The row draws the label, so its width is the same whatever the
        // profile behind it is called; the address is reachable, not written.
        byte[] pdfBytes = render(TerracottaRailFixtures.canonicalCv());
        assertThat(textOf(pdfBytes))
                .contains("LinkedIn")
                .doesNotContain("linkedin.com/in/oliverbennett-architect");
        assertThat(linkTargets(pdfBytes))
                .contains("https://linkedin.com/in/oliverbennett-architect");
    }

    @Test
    void everyContactRowStartsOnTheSameAxis() throws Exception {
        // Four rows, one mark width and one gap: a link that was set smaller
        // or nudged in would put its text on an axis of its own.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(0f, 0f, 0f, 0f)
                .create()) {
            TerracottaRail.create().compose(session, TerracottaRailFixtures.canonicalCv());
            List<Double> heights = session.layoutSnapshot().nodes().stream()
                    .filter(node -> node.entityName().startsWith("Contact_"))
                    .map(node -> node.placementHeight())
                    .toList();
            assertThat(heights).hasSize(4);
            assertThat(heights).allMatch(height -> Math.abs(height - heights.get(0)) < 0.01,
                    "every contact row is as tall as the first");
        }
    }

    @Test
    void everyTitleBecomesALinkWhenItsEntryCarriesOne() throws Exception {
        // A role, a degree and a credential, each pointing somewhere. None of
        // it moves a pixel or a layout node, so this is the only thing that
        // would notice the annotations going missing.
        List<CvDocument.Placement> placements = new ArrayList<>();
        for (CvDocument.Placement placement : TerracottaRailFixtures.canonicalCv().placements()) {
            placements.add(switch (placement.section().title()) {
                case "PROFESSIONAL EXPERIENCE" -> new CvDocument.Placement(Slot.MAIN,
                        new EntriesSection("PROFESSIONAL EXPERIENCE", List.of(
                                CvEntry.builder("Senior Architect")
                                        .subtitle("Northline Studio, Bristol, UK")
                                        .date("2021")
                                        .link("https://example.test/northline")
                                        .body("Lead design packages.")
                                        .build())));
                case "EDUCATION" -> new CvDocument.Placement(Slot.MAIN,
                        new EntriesSection("EDUCATION", List.of(
                                CvEntry.builder("MArch Architecture")
                                        .subtitle("University of Sheffield")
                                        .date("2014")
                                        .link("https://example.test/sheffield")
                                        .build())));
                case "CERTIFICATIONS" -> new CvDocument.Placement(Slot.SIDEBAR,
                        new EntriesSection("CERTIFICATIONS", List.of(
                                CvEntry.builder("ARB Registered Architect")
                                        .link("https://example.test/arb")
                                        .build())));
                default -> placement;
            });
        }

        List<String> targets = linkTargets(
                render(new CvDocument(TerracottaRailFixtures.identity(), placements)));
        assertThat(targets)
                .contains("https://example.test/northline")
                .contains("https://example.test/sheffield")
                .contains("https://example.test/arb");
    }

    @Test
    void aProjectTitleBecomesALinkWhenItsEntryCarriesOne() throws Exception {
        EntriesSection linked = new EntriesSection("SELECTED PROJECTS", List.of(
                CvEntry.builder("Harbour Point")
                        .subtitle("Mixed-Use Regeneration")
                        .place("Bristol")
                        .icon("building")
                        .link("https://example.test/harbour-point")
                        .body("Mixed-use development delivering 142 homes.")
                        .build()));

        List<String> targets = linkTargets(
                render(withSection(Slot.MAIN, linked, "SELECTED PROJECTS")));
        assertThat(targets).contains("https://example.test/harbour-point");
    }

    @Test
    void anEntryWithoutAMarkIsDrawnWithoutOne() throws Exception {
        EntriesSection unmarked = new EntriesSection("ADDITIONAL INFORMATION", List.of(
                CvEntry.builder("Right to Work:").body("United Kingdom").build()));

        String text = textOf(render(withSection(Slot.SIDEBAR, unmarked,
                "ADDITIONAL INFORMATION")));
        assertThat(text).contains("Right to Work:").contains("United Kingdom");
    }

    @Test
    void anUnknownMarkIsReportedAsADataError() {
        EntriesSection wrong = new EntriesSection("ADDITIONAL INFORMATION", List.of(
                CvEntry.builder("Languages:").icon("telescope").body("English").build()));

        assertThatThrownBy(() -> render(withSection(Slot.SIDEBAR, wrong,
                "ADDITIONAL INFORMATION")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("telescope")
                .hasMessageContaining("badge");
    }

    @Test
    void aDocumentWithNothingButAnIdentityStillRenders() throws Exception {
        // Every berth empty: no heading, no divider, and nothing drawn but
        // the monogram, its rule and the masthead.
        render(new CvDocument(TerracottaRailFixtures.identity(), List.of()));
    }

    @Test
    void anIdentityWithoutLinksDrawsTheThreeChannelsAndNothingElse() throws Exception {
        // The contact triple is non-blank by construction, so the three
        // channels are always there; the links are what a document may omit.
        CvIdentity noLinks = new CvIdentity(CvName.of("Ada", "Lovelace"), "Analyst",
                new Contact("+44 20 7946 0000", "ada@example.test", "London, UK"),
                List.of(), Optional.empty());

        // A run that wraps or breaks at a space carries one annotation per
        // piece, so it is the set of targets that is asserted, not the count.
        byte[] pdfBytes = render(new CvDocument(noLinks, List.of()));
        assertThat(Set.copyOf(linkTargets(pdfBytes)))
                .containsExactlyInAnyOrder("mailto:ada@example.test", "tel:+442079460000");
        assertThat(textOf(pdfBytes).lines().map(String::strip).toList()).contains("AL");
    }

    @Test
    void refusesACvLongerThanTheSheet() {
        // The two columns are one row, and a row is atomic: a CV that outgrows
        // the sheet raises rather than losing an entry to a silent cap.
        List<CvEntry> many = new ArrayList<>();
        for (int index = 0; index < 14; index++) {
            many.add(CvEntry.builder("Senior Architect " + index)
                    .subtitle("Northline Studio, Bristol, UK")
                    .date("2021")
                    .body("Lead design packages from concept through to detailed design.")
                    .build());
        }
        CvDocument tooLong = withSection(Slot.MAIN,
                new EntriesSection("PROFESSIONAL EXPERIENCE", many),
                "PROFESSIONAL EXPERIENCE");

        assertThatThrownBy(() -> render(tooLong))
                .hasMessageContaining("Body");
    }
}
