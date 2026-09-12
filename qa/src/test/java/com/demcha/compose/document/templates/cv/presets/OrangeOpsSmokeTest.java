package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvName;
import com.demcha.compose.document.templates.cv.data.CvSection;
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
 * Smoke test for {@link OrangeOps} — proves the preset renders a
 * {@link CvDocument} end-to-end with its packaged marks, uppercases the name it
 * is given, sets a heading's parenthetical apart from the heading, reports an
 * unknown mark as a data error, drops a berth nobody filled together with its
 * join rule, and carries the channels and the entry titles as link annotations,
 * which move no pixel and no layout node so neither gate would notice them
 * going missing.
 */
class OrangeOpsSmokeTest {

    /** The break a body stacks its lines on. */
    private static final String NEWLINE = String.valueOf((char) 10);

    private static byte[] render(CvDocument doc) throws Exception {
        // The preset owns its page geometry, so the session starts unconfigured
        // apart from the display family the preset names but does not carry.
        try (DocumentSession session = GraphCompose.document().create()) {
            OrangeOpsTestFont.register(session);
            OrangeOps.create().compose(session, doc);
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

    /** The text as one line, so an assertion is about wording, not wrapping. */
    private static String unwrapped(String text) {
        return text.replaceAll("(?U)\\s+", " ");
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
        for (CvDocument.Placement placement : OrangeOpsFixtures.canonicalCv().placements()) {
            if (placement.section().title().equals(replacedTitle)) {
                placements.add(new CvDocument.Placement(slot, replacement));
            } else {
                placements.add(placement);
            }
        }
        return new CvDocument(OrangeOpsFixtures.identity(), placements);
    }

    /** The canonical document with one section left out entirely. */
    private static CvDocument withoutSection(String droppedTitle) {
        List<CvDocument.Placement> placements = new ArrayList<>();
        for (CvDocument.Placement placement : OrangeOpsFixtures.canonicalCv().placements()) {
            if (!placement.section().title().equals(droppedTitle)) {
                placements.add(placement);
            }
        }
        return new CvDocument(OrangeOpsFixtures.identity(), placements);
    }

    @Test
    void exposesStableIdentity() {
        DocumentTemplate<CvDocument> template = OrangeOps.create();
        assertThat(template.id()).isEqualTo(OrangeOps.ID);
        assertThat(template.displayName()).isEqualTo(OrangeOps.DISPLAY_NAME);
    }

    @Test
    void namesTheDisplayFamilyItDoesNotCarry() {
        // A caller has to register something, and this is the name to register
        // it under; the constant is the contract, so it is pinned.
        assertThat(OrangeOps.DISPLAY_FONT.name()).isEqualTo("Oswald");
    }

    @Test
    void rendersCanonicalCvWithPackagedMarks() throws Exception {
        render(OrangeOpsFixtures.canonicalCv());
    }

    @Test
    void canonicalRenderCarriesEveryBlocksText() throws Exception {
        String text = textOf(render(OrangeOpsFixtures.canonicalCv()));
        assertThat(text)
                .contains("marcus.bennett@email.com")
                .contains("Birmingham, West Midlands, UK")
                .contains("Warehouse Operations Management")
                .contains("ZERO LOST TIME INCIDENTS")
                .contains("De Montfort University")
                .contains("IOSH Managing Safely")
                .contains("Results-driven Warehouse Operations Supervisor")
                .contains("ExpressLink Distribution Ltd")
                .contains("Productivity")
                .contains("SAP EWM, Manhattan WMS");
    }

    @Test
    void theNameIsSetInTwoTonesAndUppercased() throws Exception {
        // The fixture writes the name as a person would; the design sets it in
        // capitals, so the preset is what uppercases it. Both names are runs of
        // one paragraph, so the gap between them is a real word space.
        String text = textOf(render(OrangeOpsFixtures.canonicalCv()));
        assertThat(text.lines().map(String::strip).toList())
                .anyMatch(line -> line.contains("MARCUS") && line.contains("BENNETT"));
        assertThat(text).doesNotContain("Marcus Bennett");
    }

    @Test
    void theRoleBarIsSetInCapitals() throws Exception {
        String text = textOf(render(OrangeOpsFixtures.canonicalCv()));
        assertThat(text).contains("WAREHOUSE OPERATIONS SUPERVISOR");
    }

    @Test
    void aHeadingsParentheticalIsDrawnBesideIt() throws Exception {
        // The berth's title carries both; the preset splits at the bracket and
        // sets what follows smaller, so both halves reach the page.
        String text = unwrapped(textOf(render(OrangeOpsFixtures.canonicalCv())));
        assertThat(text).contains("KEY KPI SNAPSHOT (Recent 12 Months)");
    }

    @Test
    void contactChannelsAndLinksAreClickable() throws Exception {
        List<String> targets = linkTargets(render(OrangeOpsFixtures.canonicalCv()));
        assertThat(targets)
                .contains("mailto:marcus.bennett@email.com")
                .contains("tel:+447700900123")
                .contains("https://www.linkedin.com/in/marcusbennett");
    }

    @Test
    void aLinkShowsItsLabelAndHidesItsAddress() throws Exception {
        // The strip draws the label, so its width is the same whatever the
        // profile behind it is called; the address is reachable, not written.
        byte[] pdfBytes = render(OrangeOpsFixtures.canonicalCv());
        assertThat(textOf(pdfBytes))
                .contains("LinkedIn")
                .doesNotContain("linkedin.com/in/marcusbennett");
        assertThat(linkTargets(pdfBytes))
                .contains("https://www.linkedin.com/in/marcusbennett");
    }

    @Test
    void aLinkToSomethingOtherThanANetworkTakesTheGlobe() throws Exception {
        // The globe is a packaged mark like any other, so a missing file would
        // fail the render rather than quietly dropping the item.
        CvIdentity site = new CvIdentity(CvName.of("Ada", "Lovelace"), "Analyst",
                new Contact("+44 20 7946 0000", "ada@example.test", "London, UK"),
                List.of(new Link("Portfolio", "https://ada.example.test")), Optional.empty());

        byte[] pdfBytes = render(new CvDocument(site, List.of()));
        assertThat(textOf(pdfBytes)).contains("Portfolio").doesNotContain("ada.example.test");
        assertThat(linkTargets(pdfBytes)).contains("https://ada.example.test");
    }

    @Test
    void metricsWithUnevenCaptionsStillShareOneGrid() throws Exception {
        // Every metric gets a row per caption line the longest one has, so the
        // numbers stay on one baseline and a shorter caption leaves its lower
        // rows empty rather than lifting its neighbours.
        EntriesSection uneven = new EntriesSection("KEY KPI SNAPSHOT", List.of(
                CvEntry.builder("15%").icon("kpi-productivity")
                        .body("Productivity" + NEWLINE + "Increase").build(),
                CvEntry.builder("0").icon("kpi-safety").body("Incidents").build()));

        String text = textOf(render(withSection(Slot.MAIN, uneven, "KEY KPI SNAPSHOT "
                + "(Recent 12 Months)")));
        assertThat(text).contains("Productivity").contains("Incidents");
    }

    @Test
    void aTrunkPrefixIsNotDialled() throws Exception {
        // "(0)" is for a domestic dialler; left in the digits it would make a
        // number that reaches nobody.
        CvIdentity printed = new CvIdentity(CvName.of("Ada", "Lovelace"), "Analyst",
                new Contact("+44 (0)20 7946 0832", "ada@example.test", "London, UK"),
                List.of(), Optional.empty());

        byte[] pdfBytes = render(new CvDocument(printed, List.of()));
        assertThat(textOf(pdfBytes)).contains("+44 (0)20 7946 0832");
        assertThat(Set.copyOf(linkTargets(pdfBytes)))
                .containsExactlyInAnyOrder("mailto:ada@example.test", "tel:+442079460832");
    }

    @Test
    void anAchievementAJobAndTheDegreeBecomeLinksWhenTheirEntryCarriesOne() throws Exception {
        List<CvDocument.Placement> placements = new ArrayList<>();
        for (CvDocument.Placement placement : OrangeOpsFixtures.canonicalCv().placements()) {
            CvSection section = placement.section();
            placements.add(switch (section.title()) {
                case "ACHIEVEMENTS" -> new CvDocument.Placement(Slot.SIDEBAR,
                        new EntriesSection("ACHIEVEMENTS", List.of(
                                CvEntry.builder("15% PRODUCTIVITY INCREASE")
                                        .icon("achievement-productivity")
                                        .body("Workflow redesign.")
                                        .link("https://example.test/award")
                                        .build())));
                case "EDUCATION" -> new CvDocument.Placement(Slot.SIDEBAR,
                        new EntriesSection("EDUCATION", List.of(
                                CvEntry.builder("BSc Logistics")
                                        .icon("graduation")
                                        .body("De Montfort University")
                                        .link("https://example.test/dmu")
                                        .build())));
                case "CERTIFICATIONS" -> new CvDocument.Placement(Slot.SIDEBAR,
                        new EntriesSection("CERTIFICATIONS", List.of(
                                CvEntry.builder("IOSH Managing Safely")
                                        .subtitle("IOSH")
                                        .link("https://example.test/iosh")
                                        .build())));
                case "WORK EXPERIENCE" -> new CvDocument.Placement(Slot.MAIN,
                        new EntriesSection("WORK EXPERIENCE", List.of(
                                CvEntry.builder("Warehouse Operations Supervisor")
                                        .subtitle("ExpressLink")
                                        .date("2021")
                                        .body("Ran the floor.")
                                        .link("https://example.test/expresslink")
                                        .build())));
                default -> placement;
            });
        }

        List<String> targets = linkTargets(
                render(new CvDocument(OrangeOpsFixtures.identity(), placements)));
        assertThat(targets)
                .contains("https://example.test/award")
                .contains("https://example.test/dmu")
                .contains("https://example.test/iosh")
                .contains("https://example.test/expresslink");
    }

    @Test
    void aRolesPlaceIsSetBesideItsEmployer() throws Exception {
        EntriesSection located = new EntriesSection("WORK EXPERIENCE", List.of(
                CvEntry.builder("Shift Lead")
                        .subtitle("Midland Gate")
                        .place("Coventry, UK")
                        .date("2018")
                        .body("Ran the floor.")
                        .build()));

        String text = textOf(render(withSection(Slot.MAIN, located, "WORK EXPERIENCE")));
        assertThat(text).contains("Midland Gate | Coventry, UK");
    }

    @Test
    void anUnknownMarkIsReportedAsADataError() {
        EntriesSection wrong = new EntriesSection("ACHIEVEMENTS", List.of(
                CvEntry.builder("Telepathy").icon("telescope").body("Reading minds.").build()));

        assertThatThrownBy(() -> render(withSection(Slot.SIDEBAR, wrong, "ACHIEVEMENTS")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("telescope")
                .hasMessageContaining("graduation");
    }

    @Test
    void anEntryWithoutAMarkIsDrawnWithoutOne() throws Exception {
        EntriesSection unmarked = new EntriesSection("ADDITIONAL INFORMATION", List.of(
                CvEntry.builder("Availability:").body("Immediate").build()));

        String text = textOf(render(withSection(Slot.MAIN, unmarked, "ADDITIONAL INFORMATION")));
        assertThat(unwrapped(text)).contains("Availability: Immediate");
    }

    @Test
    void anEmptyBerthTakesItsHeadingAndItsJoinRuleWithIt() throws Exception {
        // The rule between two blocks belongs to the pair; drop one block and
        // the sheet must not keep a hairline over the gap where it was.
        byte[] full = render(OrangeOpsFixtures.canonicalCv());
        byte[] shorter = render(withoutSection("CERTIFICATIONS"));

        assertThat(textOf(full)).contains("CERTIFICATIONS");
        assertThat(textOf(shorter)).doesNotContain("CERTIFICATIONS");

        try (DocumentSession session = GraphCompose.document().create()) {
            OrangeOpsTestFont.register(session);
            OrangeOps.create().compose(session, withoutSection("CERTIFICATIONS"));
            assertThat(session.layoutSnapshot().nodes())
                    .noneMatch(node -> "AfterEducationRule".equals(node.entityName()));
        }
        try (DocumentSession session = GraphCompose.document().create()) {
            OrangeOpsTestFont.register(session);
            OrangeOps.create().compose(session, OrangeOpsFixtures.canonicalCv());
            assertThat(session.layoutSnapshot().nodes())
                    .anyMatch(node -> "AfterEducationRule".equals(node.entityName()));
        }
    }

    @Test
    void aDocumentWithNothingButAnIdentityStillRenders() throws Exception {
        // Every berth empty: the name, the role bar and the contact strip, and
        // nothing else on the sheet.
        render(new CvDocument(OrangeOpsFixtures.identity(), List.of()));
    }

    @Test
    void aLongerCvRunsOntoASecondPageRatherThanLosingARole() throws Exception {
        // Each column is a stack of blocks, so more content than the design
        // holds runs over; each entry is held together, so a role is never cut
        // in two.
        List<CvEntry> many = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            many.add(CvEntry.builder("Warehouse Operations Supervisor " + index)
                    .subtitle("ExpressLink Distribution Ltd")
                    .date("Mar 2021")
                    .body("Oversee daily warehouse operations across a large facility.")
                    .build());
        }
        CvDocument longer = withSection(Slot.MAIN,
                new EntriesSection("WORK EXPERIENCE", many), "WORK EXPERIENCE");

        try (DocumentSession session = GraphCompose.document().create()) {
            OrangeOpsTestFont.register(session);
            OrangeOps.create().compose(session, longer);
            assertThat(session.layoutSnapshot().totalPages()).isGreaterThan(1);
        }
        assertThat(textOf(render(longer))).contains("Warehouse Operations Supervisor 7");
    }
}
