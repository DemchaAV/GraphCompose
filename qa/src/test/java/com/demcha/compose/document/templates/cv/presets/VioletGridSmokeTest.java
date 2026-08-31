package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.identity.Contact;
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
 * Smoke test for {@link VioletGrid} — proves the preset renders a
 * {@link CvDocument} end-to-end with its packaged marks, sets the two-tone
 * name from the structured one, draws only the quotation's body, reports an
 * unknown mark as a data error, and carries the channels and the titles as
 * link annotations, which move no pixel and no layout node so neither gate
 * would notice them going missing.
 */
class VioletGridSmokeTest {

    private static byte[] render(CvDocument doc) throws Exception {
        // The preset owns its page geometry, so the session starts unconfigured.
        try (DocumentSession session = GraphCompose.document().create()) {
            VioletGrid.create().compose(session, doc);
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
     * The text with its spaces removed. The name and every heading are
     * letter-spaced with real space runs, so what is being asserted is the
     * wording, not the tracking.
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
        for (CvDocument.Placement placement : VioletGridFixtures.canonicalCv().placements()) {
            if (placement.section().title().equals(replacedTitle)) {
                placements.add(new CvDocument.Placement(slot, replacement));
            } else {
                placements.add(placement);
            }
        }
        return new CvDocument(VioletGridFixtures.identity(), placements);
    }

    @Test
    void exposesStableIdentity() {
        DocumentTemplate<CvDocument> template = VioletGrid.create();
        assertThat(template.id()).isEqualTo(VioletGrid.ID);
        assertThat(template.displayName()).isEqualTo(VioletGrid.DISPLAY_NAME);
    }

    @Test
    void rendersCanonicalCvWithPackagedMarks() throws Exception {
        render(VioletGridFixtures.canonicalCv());
    }

    @Test
    void canonicalRenderCarriesEveryBandsText() throws Exception {
        String text = textOf(render(VioletGridFixtures.canonicalCv()));
        assertThat(text)
                .contains("sofia.martinez.design@gmail.com")
                .contains("User-centered designer")
                .contains("Figma")
                .contains("NovaFin (FinTech Startup)")
                .contains("NovaFin Mobile App")
                .contains("Bachelor of Fine Arts in Graphic Design")
                .contains("Conversational")
                .contains("I design with empathy");
        assertThat(compact(text))
                .contains("SOFIAMARTINEZ")
                .contains("UX/UIDESIGNER")
                .contains("DESIGNSKILLS")
                .doesNotContain("QUOTE");
    }

    @Test
    void theNameIsSetFromTheStructuredNameInTwoTones() throws Exception {
        // Both names are runs of one paragraph, so the gap between them is a
        // real word space; the two tones are what the design asks for.
        String text = textOf(render(VioletGridFixtures.canonicalCv()));
        assertThat(text.lines().map(String::strip).toList())
                .anyMatch(line -> line.contains("SOFIA") && line.contains("MARTINEZ"));
    }

    @Test
    void contactChannelsAndLinksAreClickable() throws Exception {
        List<String> targets = linkTargets(render(VioletGridFixtures.canonicalCv()));
        assertThat(targets)
                .contains("mailto:sofia.martinez.design@gmail.com")
                .contains("tel:+14155557842")
                .contains("https://sofiamartinez.design")
                .contains("https://www.linkedin.com/in/sofia-martinez-ux");
    }

    @Test
    void aLinkShowsItsLabelAndHidesItsAddress() throws Exception {
        // The column draws the label, so its width is the same whatever the
        // profile behind it is called; the address is reachable, not written.
        byte[] pdfBytes = render(VioletGridFixtures.canonicalCv());
        assertThat(textOf(pdfBytes))
                .contains("LinkedIn")
                .doesNotContain("linkedin.com/in/sofia-martinez-ux");
        assertThat(linkTargets(pdfBytes))
                .contains("https://www.linkedin.com/in/sofia-martinez-ux");
    }

    @Test
    void aRoleAProjectAndADegreeBecomeLinksWhenTheirEntryCarriesOne() throws Exception {
        List<CvDocument.Placement> placements = new ArrayList<>();
        for (CvDocument.Placement placement : VioletGridFixtures.canonicalCv().placements()) {
            placements.add(switch (placement.section().title()) {
                case "EXPERIENCE" -> new CvDocument.Placement(Slot.MAIN,
                        new EntriesSection("EXPERIENCE", List.of(
                                CvEntry.builder("Senior UX/UI Designer")
                                        .subtitle("NovaFin")
                                        .place("San Francisco, CA")
                                        .date("2022")
                                        .link("https://example.test/novafin")
                                        .body("Led end-to-end design.")
                                        .build())));
                case "SELECTED PROJECTS" -> new CvDocument.Placement(Slot.MAIN,
                        new EntriesSection("SELECTED PROJECTS", List.of(
                                CvEntry.builder("NovaFin Mobile App")
                                        .subtitle("Personal Finance")
                                        .date("2023")
                                        .icon("project-wallet")
                                        .link("https://example.test/app")
                                        .body("Led UX/UI design.")
                                        .build())));
                case "EDUCATION" -> new CvDocument.Placement(Slot.FOOTER,
                        new EntriesSection("EDUCATION", List.of(
                                CvEntry.builder("Bachelor of Fine Arts")
                                        .subtitle("California College of the Arts")
                                        .place("San Francisco, CA")
                                        .date("2014 - 2018")
                                        .icon("graduation")
                                        .link("https://example.test/cca")
                                        .build())));
                default -> placement;
            });
        }

        List<String> targets = linkTargets(
                render(new CvDocument(VioletGridFixtures.identity(), placements)));
        assertThat(targets)
                .contains("https://example.test/novafin")
                .contains("https://example.test/app")
                .contains("https://example.test/cca");
    }

    @Test
    void anUnknownMarkIsReportedAsADataError() {
        EntriesSection wrong = new EntriesSection("DESIGN SKILLS", List.of(
                CvEntry.builder("Telepathy").icon("telescope").body("Reading minds.").build()));

        assertThatThrownBy(() -> render(withSection(Slot.MAIN, wrong, "DESIGN SKILLS")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("telescope")
                .hasMessageContaining("wireframing");
    }

    @Test
    void aSkillWithoutAMarkIsDrawnWithoutOne() throws Exception {
        EntriesSection unmarked = new EntriesSection("DESIGN SKILLS", List.of(
                CvEntry.builder("UX RESEARCH").body("User interviews and surveys.").build()));

        String text = textOf(render(withSection(Slot.MAIN, unmarked, "DESIGN SKILLS")));
        // The description wraps inside its narrow column, so a fragment
        // of one line is what is asserted.
        assertThat(text).contains("User interviews and");
    }

    @Test
    void aDocumentWithNothingButAnIdentityStillRenders() throws Exception {
        // Every berth empty: the name, the discipline line and its rule, and
        // nothing else on the sheet.
        render(new CvDocument(VioletGridFixtures.identity(), List.of()));
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
    void aLongerCvRunsOntoASecondPageRatherThanLosingARole() throws Exception {
        // The page is a stack of bands, so more content than the design holds
        // runs over; each entry is held together, so a role is never cut in two.
        List<CvEntry> many = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            many.add(CvEntry.builder("Senior UX/UI Designer " + index)
                    .subtitle("NovaFin")
                    .place("San Francisco, CA")
                    .date("2022")
                    .body("Led end-to-end design for a personal finance platform.")
                    .build());
        }

        try (DocumentSession session = GraphCompose.document().create()) {
            VioletGrid.create().compose(session, withSection(Slot.MAIN,
                    new EntriesSection("EXPERIENCE", many), "EXPERIENCE"));
            assertThat(session.layoutSnapshot().totalPages()).isGreaterThan(1);
        }
        String text = textOf(render(withSection(Slot.MAIN,
                new EntriesSection("EXPERIENCE", many), "EXPERIENCE")));
        assertThat(text).contains("Senior UX/UI Designer 7");
    }
}
