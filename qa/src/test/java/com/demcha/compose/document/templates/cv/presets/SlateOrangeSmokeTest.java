package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvName;
import com.demcha.compose.document.templates.cv.data.CvSkill;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.SkillGroup;
import com.demcha.compose.document.templates.cv.data.SkillsSection;
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
import java.util.OptionalDouble;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Smoke test for {@link SlateOrange} — proves the preset renders a
 * {@link CvDocument} end-to-end with its packaged marks, draws the monogram
 * from the name and the specialism strip from its berth's body, reports an
 * unknown mark as a data error, draws both channels of a rated language,
 * refuses a CV taller than its sheet rather than losing a role, and carries
 * the channels and the titles as link annotations — which move no pixel and
 * no layout node, so neither gate would notice them going missing.
 */
class SlateOrangeSmokeTest {

    private static byte[] render(CvDocument doc) throws Exception {
        // The preset owns its page geometry, so the session starts unconfigured.
        try (DocumentSession session = GraphCompose.document().create()) {
            SlateOrange.create().compose(session, doc);
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
        for (CvDocument.Placement placement : SlateOrangeFixtures.canonicalCv().placements()) {
            if (placement.section().title().equals(replacedTitle)) {
                placements.add(new CvDocument.Placement(slot, replacement));
            } else {
                placements.add(placement);
            }
        }
        return new CvDocument(SlateOrangeFixtures.identity(), placements);
    }

    @Test
    void exposesStableIdentity() {
        DocumentTemplate<CvDocument> template = SlateOrange.create();
        assertThat(template.id()).isEqualTo(SlateOrange.ID);
        assertThat(template.displayName()).isEqualTo(SlateOrange.DISPLAY_NAME);
    }

    @Test
    void rendersCanonicalCvWithPackagedMarks() throws Exception {
        render(SlateOrangeFixtures.canonicalCv());
    }

    @Test
    void canonicalRenderCarriesEveryBlocksText() throws Exception {
        String text = textOf(render(SlateOrangeFixtures.canonicalCv()));
        assertThat(text)
                .contains("DANIEL HARPER")
                .contains("VERSATILE PROFESSIONAL")
                .contains("Customer Service Excellence")
                .contains("Process Improvement")
                .contains("Professional Working")
                .contains("Adaptable and resourceful professional")
                .contains("BrightStart Logistics, Austin, TX")
                .contains("Bachelor of Arts in Communications")
                .contains("ServSafe Food Handler Certification");
    }

    @Test
    void theMonogramAndTheSpecialismStripAreDrawn() throws Exception {
        // The monogram comes from the name's own initials, and the strip is
        // the specialism berth's body joined by the design's own separator.
        String text = textOf(render(SlateOrangeFixtures.canonicalCv()));
        assertThat(text.lines().map(String::strip).toList())
                .anyMatch(line -> line.startsWith("DH"));
        assertThat(text.replaceAll("(?U)\\s", ""))
                .contains("OPERATIONS")
                .contains("CUSTOMERSUCCESS")
                .contains("COORDINATION")
                .doesNotContain("SPECIALISMS");
    }

    @Test
    void aLanguageDrawsBothItsRatingAndItsWording() throws Exception {
        // The discs come from the level and the words beside them from the
        // note; this is the block that wants both channels of a rated skill.
        SkillsSection languages = new SkillsSection("LANGUAGES",
                List.of(new SkillGroup("LANGUAGES", List.of(
                        CvSkill.of("Dutch", 0.6, "Conversational")))));
        List<CvDocument.Placement> placements = new ArrayList<>();
        for (CvDocument.Placement placement : SlateOrangeFixtures.canonicalCv().placements()) {
            placements.add(placement.section().title().equals("LANGUAGES")
                    ? new CvDocument.Placement(Slot.SIDEBAR, languages)
                    : placement);
        }

        String text = textOf(render(new CvDocument(SlateOrangeFixtures.identity(), placements)));
        assertThat(text).contains("Dutch").contains("Conversational");
    }

    @Test
    void aSkillWithoutANoteLeavesThatColumnEmpty() throws Exception {
        SkillsSection languages = new SkillsSection("LANGUAGES",
                List.of(new SkillGroup("LANGUAGES", List.of(
                        new CvSkill("Dutch", OptionalDouble.of(0.6))))));
        List<CvDocument.Placement> placements = new ArrayList<>();
        for (CvDocument.Placement placement : SlateOrangeFixtures.canonicalCv().placements()) {
            placements.add(placement.section().title().equals("LANGUAGES")
                    ? new CvDocument.Placement(Slot.SIDEBAR, languages)
                    : placement);
        }

        String text = textOf(render(new CvDocument(SlateOrangeFixtures.identity(), placements)));
        assertThat(text).contains("Dutch").doesNotContain("Conversational");
    }

    @Test
    void contactChannelsAndLinksAreClickable() throws Exception {
        // The dial target is derived from the number as the document writes
        // it: this one carries no country code, so neither does the target.
        List<String> targets = linkTargets(render(SlateOrangeFixtures.canonicalCv()));
        assertThat(targets)
                .contains("tel:5551234567")
                .contains("mailto:daniel.harper@email.com")
                .contains("https://www.linkedin.com/in/danielharper");
    }

    @Test
    void aLinkShowsItsLabelAndHidesItsAddress() throws Exception {
        byte[] pdfBytes = render(SlateOrangeFixtures.canonicalCv());
        assertThat(textOf(pdfBytes))
                .contains("LinkedIn")
                .doesNotContain("linkedin.com/in/danielharper");
        assertThat(linkTargets(pdfBytes)).contains("https://www.linkedin.com/in/danielharper");
    }

    @Test
    void aRoleAndADegreeBecomeLinksWhenTheirEntryCarriesOne() throws Exception {
        List<CvDocument.Placement> placements = new ArrayList<>();
        for (CvDocument.Placement placement : SlateOrangeFixtures.canonicalCv().placements()) {
            placements.add(switch (placement.section().title()) {
                case "PROFESSIONAL EXPERIENCE" -> new CvDocument.Placement(Slot.MAIN,
                        new EntriesSection("PROFESSIONAL EXPERIENCE", List.of(
                                CvEntry.builder("Operations Coordinator")
                                        .subtitle("BrightStart Logistics, Austin, TX")
                                        .date("Jan 2022")
                                        .link("https://example.test/brightstart")
                                        .body("Coordinate daily operations.")
                                        .build())));
                case "EDUCATION" -> new CvDocument.Placement(Slot.FOOTER,
                        new EntriesSection("EDUCATION", List.of(
                                CvEntry.builder("Bachelor of Arts in Communications")
                                        .subtitle("University of Texas at Austin")
                                        .link("https://example.test/utexas")
                                        .build())));
                default -> placement;
            });
        }

        List<String> targets = linkTargets(
                render(new CvDocument(SlateOrangeFixtures.identity(), placements)));
        assertThat(targets)
                .contains("https://example.test/brightstart")
                .contains("https://example.test/utexas");
    }

    @Test
    void anUnknownMarkIsReportedAsADataError() {
        EntriesSection wrong = new EntriesSection("CORE COMPETENCIES", List.of(
                CvEntry.builder("Telepathy").icon("telescope").build()));

        assertThatThrownBy(() -> render(withSection(Slot.SIDEBAR, wrong, "CORE COMPETENCIES")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("telescope")
                .hasMessageContaining("time-management");
    }

    @Test
    void anAchievementWithoutAMarkTakesTheTrophy() throws Exception {
        // Every achievement carries the same mark in this design, so an entry
        // that names none is drawn with it rather than without one.
        EntriesSection unmarked = new EntriesSection("SELECTED ACHIEVEMENTS", List.of(
                CvEntry.builder("Process Improvement").body("Reduced reporting time.").build()));

        String text = textOf(render(withSection(Slot.SIDEBAR, unmarked,
                "SELECTED ACHIEVEMENTS")));
        assertThat(text).contains("Process Improvement").contains("Reduced reporting time.");
    }

    @Test
    void aDocumentWithNothingButAnIdentityStillRenders() throws Exception {
        // Every berth empty: the band, the monogram and the name, and nothing
        // in either column.
        render(new CvDocument(SlateOrangeFixtures.identity(), List.of()));
    }

    @Test
    void anIdentityWithoutLinksDrawsTheThreeChannels() throws Exception {
        CvIdentity noLinks = new CvIdentity(CvName.of("Ada", "Lovelace"), "Analyst",
                new Contact("+44 20 7946 0000", "ada@example.test", "London, UK"),
                List.of(), Optional.empty());

        // A run that breaks at a space carries one annotation per piece, so it
        // is the set of targets that is asserted, not the count.
        byte[] pdfBytes = render(new CvDocument(noLinks, List.of()));
        assertThat(Set.copyOf(linkTargets(pdfBytes)))
                .containsExactlyInAnyOrder("mailto:ada@example.test", "tel:+442079460000");
    }

    @Test
    void refusesACvTallerThanTheSheet() {
        // The body's two columns are one row, and a row is atomic: a CV that
        // outgrows the sheet raises rather than losing a role to a silent cap.
        List<CvEntry> many = new ArrayList<>();
        for (int index = 0; index < 14; index++) {
            many.add(CvEntry.builder("Operations Coordinator " + index)
                    .subtitle("BrightStart Logistics, Austin, TX")
                    .date("Jan 2022")
                    .body("Coordinate daily operations and scheduling for a team of 12.")
                    .build());
        }

        assertThatThrownBy(() -> render(withSection(Slot.MAIN,
                new EntriesSection("PROFESSIONAL EXPERIENCE", many),
                "PROFESSIONAL EXPERIENCE")))
                .hasMessageContaining("Body");
    }
}
