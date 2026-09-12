package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.exceptions.AtomicNodeTooLargeException;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvName;
import com.demcha.compose.document.templates.cv.data.CvSection;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Smoke test for {@link MidnightNavy} — proves the preset renders a
 * {@link CvDocument} end-to-end with its packaged marks, builds the monogram
 * and the tracked role line from the identity it is given, rounds a language's
 * rating to the nearest of five dots, reports an unknown mark as a data error,
 * drops a berth nobody filled, and carries the channels and the entry titles as
 * link annotations, which move no pixel and no layout node so neither gate
 * would notice them going missing.
 */
class MidnightNavySmokeTest {

    private static final String NEWLINE = String.valueOf((char) 10);

    private static byte[] render(CvDocument doc) throws Exception {
        // The preset owns its page geometry, so the session starts unconfigured.
        try (DocumentSession session = GraphCompose.document().create()) {
            MidnightNavy.create().compose(session, doc);
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

    /** The text with its spaces removed — the role line is tracked with real spaces. */
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
    private static CvDocument withSection(Slot slot, CvSection replacement,
                                          String replacedTitle) {
        List<CvDocument.Placement> placements = new ArrayList<>();
        for (CvDocument.Placement placement : MidnightNavyFixtures.canonicalCv().placements()) {
            if (placement.section().title().equals(replacedTitle)) {
                placements.add(new CvDocument.Placement(slot, replacement));
            } else {
                placements.add(placement);
            }
        }
        return new CvDocument(MidnightNavyFixtures.identity(), placements);
    }

    /** The canonical document with one section left out entirely. */
    private static CvDocument withoutSection(String droppedTitle) {
        List<CvDocument.Placement> placements = new ArrayList<>();
        for (CvDocument.Placement placement : MidnightNavyFixtures.canonicalCv().placements()) {
            if (!placement.section().title().equals(droppedTitle)) {
                placements.add(placement);
            }
        }
        return new CvDocument(MidnightNavyFixtures.identity(), placements);
    }

    @Test
    void exposesStableIdentity() {
        DocumentTemplate<CvDocument> template = MidnightNavy.create();
        assertThat(template.id()).isEqualTo(MidnightNavy.ID);
        assertThat(template.displayName()).isEqualTo(MidnightNavy.DISPLAY_NAME);
    }

    @Test
    void rendersCanonicalCvWithPackagedMarks() throws Exception {
        render(MidnightNavyFixtures.canonicalCv());
    }

    @Test
    void canonicalRenderCarriesEveryBlocksText() throws Exception {
        String text = textOf(render(MidnightNavyFixtures.canonicalCv()));
        assertThat(text)
                .contains("alex.morgan@email.com")
                .contains("New York, NY, USA")
                .contains("University of Chicago Booth School of")
                .contains("Strategic Planning")
                .contains("English")
                .contains("Results-driven Marketing Manager")
                .contains("Starwave Solutions")
                .contains("Increased brand")
                .contains("Google Analytics Certified");
        assertThat(compact(text)).contains("CONTACT");
    }

    @Test
    void theMonogramIsTheTwoInitialsOfTheName() throws Exception {
        // The circle carries one initial per line, taken from the name rather
        // than from a field a document would have to fill in twice.
        String text = textOf(render(MidnightNavyFixtures.canonicalCv()));
        List<String> lines = text.lines().map(String::strip)
                .filter(line -> !line.isEmpty()).toList();
        assertThat(lines).contains("A", "M");
    }

    @Test
    void theNameAndRoleAreSetInCapitals() throws Exception {
        // The design sets both in capitals, and the role is tracked with real
        // spaces — so it is the spaceless text that carries the wording.
        String compact = compact(textOf(render(MidnightNavyFixtures.canonicalCv())));
        assertThat(compact).contains("ALEXMORGAN").contains("MARKETINGMANAGER");
    }

    @Test
    void contactChannelsAndLinksAreClickable() throws Exception {
        List<String> targets = linkTargets(render(MidnightNavyFixtures.canonicalCv()));
        assertThat(targets)
                .contains("mailto:alex.morgan@email.com")
                .contains("tel:+15551234567")
                .contains("https://www.linkedin.com/in/alexmorgan")
                .contains("https://alexmorgan.com");
    }

    @Test
    void aLinkShowsItsLabelAndHidesItsAddress() throws Exception {
        // The plate draws the label, so its width is the same whatever the
        // profile behind it is called; the address is reachable, not written.
        byte[] pdfBytes = render(MidnightNavyFixtures.canonicalCv());
        assertThat(textOf(pdfBytes))
                .contains("LinkedIn")
                .contains("Portfolio")
                .doesNotContain("linkedin.com/in/alexmorgan");
        assertThat(linkTargets(pdfBytes))
                .contains("https://www.linkedin.com/in/alexmorgan");
    }

    @Test
    void aTrunkPrefixIsNotDialled() throws Exception {
        CvIdentity printed = new CvIdentity(CvName.of("Ada", "Lovelace"), "Analyst",
                new Contact("+44 (0)20 7946 0832", "ada@example.test", "London, UK"),
                List.of(), Optional.empty());

        byte[] pdfBytes = render(new CvDocument(printed, List.of()));
        assertThat(textOf(pdfBytes)).contains("+44 (0)20 7946 0832");
        assertThat(Set.copyOf(linkTargets(pdfBytes)))
                .containsExactlyInAnyOrder("mailto:ada@example.test", "tel:+442079460832");
    }

    @Test
    void aDegreeAJobAndACertificationBecomeLinksWhenTheirEntryCarriesOne() throws Exception {
        List<CvDocument.Placement> placements = new ArrayList<>();
        for (CvDocument.Placement placement : MidnightNavyFixtures.canonicalCv().placements()) {
            placements.add(switch (placement.section().title()) {
                case "EDUCATION" -> new CvDocument.Placement(Slot.SIDEBAR,
                        new EntriesSection("EDUCATION", List.of(
                                CvEntry.builder("MBA").subtitle("Booth").date("2018")
                                        .link("https://example.test/booth").build())));
                case "EXPERIENCE" -> new CvDocument.Placement(Slot.MAIN,
                        new EntriesSection("EXPERIENCE", List.of(
                                CvEntry.builder("MARKETING MANAGER").subtitle("Starwave")
                                        .place("New York, NY").date("2021")
                                        .body("Ran the funnel.")
                                        .link("https://example.test/starwave").build())));
                case "CERTIFICATIONS" -> new CvDocument.Placement(Slot.MAIN,
                        new EntriesSection("CERTIFICATIONS", List.of(
                                CvEntry.builder("Analytics Certified").subtitle("Issuer")
                                        .date("2020")
                                        .link("https://example.test/cert").build())));
                default -> placement;
            });
        }

        assertThat(linkTargets(render(new CvDocument(MidnightNavyFixtures.identity(), placements))))
                .contains("https://example.test/booth")
                .contains("https://example.test/starwave")
                .contains("https://example.test/cert");
    }

    @Test
    void anUnknownMarkIsReportedAsADataError() {
        EntriesSection wrong = new EntriesSection("ACHIEVEMENTS", List.of(
                CvEntry.builder("Read minds").icon("telescope").build()));

        assertThatThrownBy(() -> render(withSection(Slot.MAIN, wrong, "ACHIEVEMENTS")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("telescope")
                .hasMessageContaining("trophy");
    }

    @Test
    void aLanguageRatingRoundsToTheNearestDot() throws Exception {
        // The model carries a fraction and the design shows fifths, so a rating
        // between two dots has to land on one of them rather than throw.
        SkillsSection odd = new SkillsSection("LANGUAGES", List.of(
                new SkillGroup("LANGUAGES", List.of(
                        CvSkill.of("Dutch", 0.7), CvSkill.of("Greek", 0.0),
                        CvSkill.of("Latin", 1.0)))));

        String text = textOf(render(withSection(Slot.SIDEBAR, odd, "LANGUAGES")));
        assertThat(text).contains("Dutch").contains("Greek").contains("Latin");
    }

    @Test
    void aSkillWithoutALevelDrawsAnEmptyMeter() throws Exception {
        SkillsSection unrated = new SkillsSection("SKILLS", List.of(
                new SkillGroup("SKILLS", List.of(CvSkill.of("Copywriting")))));

        assertThat(textOf(render(withSection(Slot.SIDEBAR, unrated, "SKILLS"))))
                .contains("Copywriting");
    }

    @Test
    void anEmptyBerthTakesItsHeadingWithIt() throws Exception {
        byte[] shorter = render(withoutSection("CERTIFICATIONS"));
        assertThat(textOf(shorter)).doesNotContain("Google Analytics Certified");
        assertThat(compact(textOf(shorter))).doesNotContain("CERTIFICATIONS");
    }

    @Test
    void aDocumentWithNothingButAnIdentityStillRenders() throws Exception {
        // Every berth empty: the plate, the monogram, the name and the contact
        // block, and nothing else on the sheet.
        render(new CvDocument(MidnightNavyFixtures.identity(), List.of()));
    }

    @Test
    void aCvLongerThanTheSheetIsRefusedRatherThanCut() throws Exception {
        // The body is one row and a row is atomic, so this design does not
        // paginate: content past the sheet is reported with the node that could
        // not fit rather than silently clipped. TimelineMinimal is the preset in
        // this package built to flow.
        List<CvEntry> many = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            many.add(CvEntry.builder("MARKETING MANAGER " + index)
                    .subtitle("Starwave Solutions")
                    .place("New York, NY")
                    .date("2021 - Present")
                    .body(String.join(NEWLINE,
                            "Develop and implement marketing strategies.",
                            "Lead a team across campaigns and channels."))
                    .build());
        }
        CvDocument longer = withSection(Slot.MAIN,
                new EntriesSection("EXPERIENCE", many), "EXPERIENCE");

        assertThatThrownBy(() -> render(longer))
                .isInstanceOf(AtomicNodeTooLargeException.class)
                .hasMessageContaining("PageGrid");
    }
}
