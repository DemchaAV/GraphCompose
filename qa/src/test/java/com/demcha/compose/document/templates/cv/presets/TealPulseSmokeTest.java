package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
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
 * Smoke test for {@link TealPulse} — proves the preset renders a
 * {@link CvDocument} end-to-end with its packaged marks, sets its own page
 * over whatever the caller configured, draws only the tagline's body, and
 * carries the channels and the titles as link annotations, which move no pixel
 * and no layout node so neither gate would notice them going missing.
 *
 * <p>It also pins what happens past one page, which is not what the other
 * ported CV presets do: the bands flow, so a longer CV runs onto a second
 * page, while the body row itself cannot be split.</p>
 */
class TealPulseSmokeTest {

    private static byte[] render(CvDocument doc) throws Exception {
        // The preset owns its page geometry, so the session starts unconfigured.
        try (DocumentSession session = GraphCompose.document().create()) {
            TealPulse.create().compose(session, doc);
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
     * The text with its spaces removed. The sheet's headings and its name are
     * letter-spaced with real space characters, so what is being asserted is
     * the wording, not the tracking.
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
        for (CvDocument.Placement placement : TealPulseFixtures.canonicalCv().placements()) {
            if (placement.section().title().equals(replacedTitle)) {
                placements.add(new CvDocument.Placement(slot, replacement));
            } else {
                placements.add(placement);
            }
        }
        return new CvDocument(TealPulseFixtures.identity(), placements);
    }

    @Test
    void exposesStableIdentity() {
        DocumentTemplate<CvDocument> template = TealPulse.create();
        assertThat(template.id()).isEqualTo(TealPulse.ID);
        assertThat(template.displayName()).isEqualTo(TealPulse.DISPLAY_NAME);
    }

    @Test
    void rendersCanonicalCvWithPackagedMarks() throws Exception {
        render(TealPulseFixtures.canonicalCv());
    }

    @Test
    void canonicalRenderCarriesEveryBandsText() throws Exception {
        String text = textOf(render(TealPulseFixtures.canonicalCv()));
        assertThat(text)
                .contains("isabella.moore@email.com")
                .contains("Patient Assessment")
                .contains("Compassionate Registered Nurse")
                .contains("Manchester Royal Infirmary")
                .contains("BSc (Hons) Adult Nursing")
                .contains("NMC Registered Nurse")
                .contains("Right to Work:");
        assertThat(compact(text))
                .contains("ISABELLAMOORE")
                .contains("REGISTEREDNURSE")
                .contains("PROFESSIONALEXPERIENCE")
                .contains("COMPASSIONATECARE.CLINICALEXCELLENCE.BETTEROUTCOMES.");
    }

    @Test
    void theTaglineBerthDrawsItsBodyAndNotItsTitle() throws Exception {
        // The title is how a document names the berth; only the line reaches
        // the sheet, so a document is free to call the berth what it likes.
        String text = compact(textOf(render(TealPulseFixtures.canonicalCv())));
        assertThat(text)
                .contains("COMPASSIONATECARE.")
                .doesNotContain("TAGLINE");
    }

    @Test
    void thePresetSetsItsOwnPageOverTheCallersOwn() throws Exception {
        // The sheet is a fixed composition on the design's own page, so a
        // caller who configures A4 still gets the design's proportion.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(40f, 40f, 40f, 40f)
                .create()) {
            TealPulse.create().compose(session, TealPulseFixtures.canonicalCv());
            byte[] pdfBytes = session.toPdfBytes();
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                float height = document.getPage(0).getMediaBox().getHeight();
                assertThat(height).isCloseTo((float) TealPulseStyles.PAGE_HEIGHT,
                        org.assertj.core.data.Offset.offset(0.5f));
                assertThat(height).isNotCloseTo((float) DocumentPageSize.A4.height(),
                        org.assertj.core.data.Offset.offset(1.0f));
            }
        }
    }

    @Test
    void contactChannelsAndLinksAreClickable() throws Exception {
        List<String> targets = linkTargets(render(TealPulseFixtures.canonicalCv()));
        assertThat(targets)
                .contains("mailto:isabella.moore@email.com")
                .contains("tel:+447700900123")
                .contains("https://www.linkedin.com/in/isabellamoore-rn");
    }

    @Test
    void aLinkShowsItsLabelAndHidesItsAddress() throws Exception {
        // The strip draws the label, so its gaps are the same whatever the
        // profile behind it is called; the address is reachable, not written.
        byte[] pdfBytes = render(TealPulseFixtures.canonicalCv());
        assertThat(textOf(pdfBytes))
                .contains("LinkedIn")
                .doesNotContain("linkedin.com/in/isabellamoore-rn");
        assertThat(linkTargets(pdfBytes))
                .contains("https://www.linkedin.com/in/isabellamoore-rn");
    }

    @Test
    void aRoleAndADegreeBecomeLinksWhenTheirEntryCarriesOne() throws Exception {
        CvDocument linked = withSection(Slot.MAIN,
                new EntriesSection("PROFESSIONAL EXPERIENCE", List.of(
                        CvEntry.builder("Senior Staff Nurse")
                                .subtitle("Manchester Royal Infirmary, Manchester, UK")
                                .date("2021")
                                .link("https://example.test/mri")
                                .body("Deliver direct nursing care.")
                                .build())),
                "PROFESSIONAL EXPERIENCE");
        List<CvDocument.Placement> placements = new ArrayList<>();
        for (CvDocument.Placement placement : linked.placements()) {
            if (placement.section().title().equals("EDUCATION")) {
                placements.add(new CvDocument.Placement(Slot.FOOTER,
                        new EntriesSection("EDUCATION", List.of(
                                CvEntry.builder("BSc (Hons) Adult Nursing")
                                        .subtitle("University of Manchester")
                                        .date("2013")
                                        .link("https://example.test/manchester")
                                        .build()))));
            } else {
                placements.add(placement);
            }
        }

        List<String> targets = linkTargets(
                render(new CvDocument(TealPulseFixtures.identity(), placements)));
        assertThat(targets)
                .contains("https://example.test/mri")
                .contains("https://example.test/manchester");
    }

    @Test
    void aDocumentWithNothingButAnIdentityStillRenders() throws Exception {
        // Every berth empty: no badge, no heading, and nothing drawn but the
        // mark, the name and the rules that frame them.
        render(new CvDocument(TealPulseFixtures.identity(), List.of()));
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
    void aLongerCvCarriesTheClosingBandOntoASecondPage() throws Exception {
        // The bands are stacked in the page flow, so a CV with more roles than
        // the design holds runs over rather than losing anything.
        try (DocumentSession session = GraphCompose.document().create()) {
            TealPulse.create().compose(session, withRoles(6));
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(2);
        }
    }

    @Test
    void refusesABodyTallerThanOnePage() {
        // What cannot flow is the body itself: its two columns are one row,
        // and a row is atomic, so it raises rather than being cut in half.
        assertThatThrownBy(() -> render(withRoles(14)))
                .hasMessageContaining("BodyGrid");
    }

    /** The canonical CV with its experience replaced by {@code count} roles. */
    private static CvDocument withRoles(int count) {
        List<CvEntry> many = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            many.add(CvEntry.builder("Senior Staff Nurse " + index)
                    .subtitle("Manchester Royal Infirmary, Manchester, UK")
                    .date("2021")
                    .body("Deliver direct nursing care for adult patients on a busy ward.")
                    .build());
        }
        return withSection(Slot.MAIN,
                new EntriesSection("PROFESSIONAL EXPERIENCE", many),
                "PROFESSIONAL EXPERIENCE");
    }
}
