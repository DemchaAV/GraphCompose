package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.exceptions.AtomicNodeTooLargeException;
import com.demcha.compose.document.snapshot.LayoutNodeSnapshot;
import com.demcha.compose.document.snapshot.LayoutSnapshot;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Smoke test for {@link ProfessionalSidebar} — proves the preset renders a
 * {@link CvDocument} end-to-end with its packaged marks, reaches its
 * channels as PDF links, degrades through its guards on a document that
 * fills none of the berths, and draws a name without a level rather than an
 * empty meter.
 *
 * <p>It also pins what the class documents about length and about fields,
 * because each is something a caller finds out the hard way otherwise: a CV
 * longer than the sheet carries on to more pages, each row on a page of its
 * own; a role taller than the sheet is refused by name; and three fields this
 * design has no place for are not drawn.</p>
 */
class ProfessionalSidebarSmokeTest {

    private static byte[] render(CvDocument doc) throws Exception {
        // The preset owns its page geometry, so the session starts unconfigured.
        try (DocumentSession session = GraphCompose.document().create()) {
            ProfessionalSidebar.create().compose(session, doc);
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
        DocumentTemplate<CvDocument> template = ProfessionalSidebar.create();
        assertThat(template.id()).isEqualTo(ProfessionalSidebar.ID);
        assertThat(template.displayName()).isEqualTo(ProfessionalSidebar.DISPLAY_NAME);
    }

    @Test
    void canonicalRenderCarriesTheUntrackedText() throws Exception {
        // The headings and the name are set letter by letter to reach their
        // tracking, so they arrive on the text layer spaced out. What is
        // asserted here is the text drawn as written.
        String text = textOf(render(ProfessionalSidebarFixtures.canonicalCv()));
        assertThat(text)
                .contains("your.email@example.com")
                .contains("City, State, Country")
                .contains("Java backend engineer")
                .contains("DEGREE NAME")
                .contains("University Name")
                .contains("Docker")
                .contains("English")
                .contains("Product Studio")
                .contains("Built secure Spring Boot REST APIs for customer workflows.")
                .contains("Available upon request.");
    }

    @Test
    void channelsReachThePdfAsLinks() throws Exception {
        // The dial and mail targets are built from the values, so nothing in
        // the document carries them and nothing but the PDF can show them.
        assertThat(linkUris(render(ProfessionalSidebarFixtures.canonicalCv())))
                .contains("tel:+12345678900",
                        "mailto:your.email@example.com",
                        "https://linkedin.com/in/username",
                        "https://www.yourwebsite.com");
    }

    @Test
    void rendersOneCanonicalPage() throws Exception {
        try (PDDocument document =
                     Loader.loadPDF(render(ProfessionalSidebarFixtures.canonicalCv()))) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void rendersIdentityOnly() throws Exception {
        // Every berth empty: the plate, the initials and the three channels
        // the identity always carries, and no hairline left hanging under a
        // section that was never drawn.
        String text = textOf(render(CvDocument.builder()
                .identity(identity())
                .build()));
        assertThat(text).contains("empty@example.com");
    }

    @Test
    void rendersWithoutAJobTitle() throws Exception {
        // The role line is written letter by letter to reach its tracking, so
        // an identity with no job title reaches the engine as a paragraph
        // holding no runs at all.
        String text = textOf(render(CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Ada", "Lovelace")
                        .contact(new Contact("+44 20 7946 0000", "empty@example.com",
                                "London, UK"))
                        .build())
                .build()));
        assertThat(text).contains("empty@example.com");
    }

    @Test
    void drawsAnUnlevelledSkillAsItsNameAlone() throws Exception {
        // A meter needs a level; without one the row is the name, not an
        // empty track that reads as a zero score.
        String text = textOf(render(CvDocument.builder()
                .identity(identity())
                .section(Slot.SIDEBAR, SkillsSection.of("SKILLS",
                        SkillGroup.of("Core", "Java", "Kotlin")))
                .section(Slot.SIDEBAR, SkillsSection.of("LANGUAGES",
                        new SkillGroup("Spoken", List.of(CvSkill.of("English")))))
                .build()));
        assertThat(text).contains("Java").contains("Kotlin").contains("English");
    }

    @Test
    void placesSectionsByTitleWhateverSlotTheDocumentNames() throws Exception {
        // The columns are fixed, so a document that leaves everything in the
        // main slot still gets its education in the sidebar.
        String text = textOf(render(CvDocument.builder()
                .identity(identity())
                .section(Slot.MAIN, EntriesSection.builder("EDUCATION")
                        .entry("BSc Computing", "University Name", "2016", "")
                        .build())
                .build()));
        assertThat(text).contains("BSc Computing").contains("University Name");
    }

    @Test
    void carriesACvLongerThanTheSheetOntoMorePages() throws Exception {
        // A CV that fits the sheet is the one row the design is. A longer one
        // continues onto more pages a whole role at a time, rather than
        // raising — or dropping what does not fit, as the capped siblings do.
        EntriesSection.Builder experience = EntriesSection.builder("EXPERIENCE");
        for (int i = 0; i < 12; i++) {
            experience.entry("ROLE " + (char) ('A' + i), "Employer " + i + "   |   City",
                    "2010 - 2011",
                    String.join("\n",
                            "Did a substantial thing that took a full line of text here.",
                            "Did a second substantial thing, also a full line of text.",
                            "And a third, so each role occupies real vertical space."));
        }
        byte[] pdfBytes = render(CvDocument.builder()
                .identity(identity())
                .section(experience.build())
                .build());

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
        }
        String text = textOf(pdfBytes);
        for (int i = 0; i < 12; i++) {
            assertThat(text).contains("ROLE " + (char) ('A' + i));
        }
    }

    @Test
    void startsTheLastRoleOnAPageOfItsOwnWhenOnlyTheHairlineAboveItOverflows() {
        // The canonical profile and first role, then four contract roles: the last one
        // and the hairline above it overrun the sheet by a few points. The role opens the
        // next page without the hairline, and its row is then short enough to fit the
        // room the first row leaves — the break before it is what keeps it off that page.
        CvDocument doc = CvDocument.builder()
                .identity(ProfessionalSidebarFixtures.canonicalCv().identity())
                .section(Slot.MAIN, canonicalSection("PROFILE"))
                .section(Slot.MAIN, new EntriesSection("EXPERIENCE", firstRoleAndFourContracts()))
                .build();

        try (DocumentSession session = GraphCompose.document().create()) {
            ProfessionalSidebar.create().compose(session, doc);
            LayoutSnapshot snapshot = session.layoutSnapshot();

            assertThat(snapshot.totalPages()).isEqualTo(2);
            assertThat(snapshot.nodes())
                    .filteredOn(node -> "PageGrid_1".equals(node.entityName()))
                    .extracting(LayoutNodeSnapshot::startPage)
                    .containsExactly(1);
        }
    }

    @Test
    void refusesARoleTallerThanTheSheetAndNamesIt() {
        // A role moves to another page whole, so one with more highlights than a sheet
        // holds has nowhere to go: composing it says which block that is.
        List<String> highlights = new ArrayList<>();
        for (int i = 1; i <= 80; i++) {
            highlights.add("Highlight " + i + " takes a line of its own.");
        }
        CvDocument doc = CvDocument.builder()
                .identity(identity())
                .section(EntriesSection.builder("EXPERIENCE")
                        .entry("ROLE", "Employer   |   City", "2010 - 2011",
                                String.join("\n", highlights))
                        .build())
                .build();

        try (DocumentSession session = GraphCompose.document().create()) {
            assertThatThrownBy(() -> ProfessionalSidebar.create().compose(session, doc))
                    .isInstanceOf(AtomicNodeTooLargeException.class)
                    .hasMessageContaining("'Experience_0'");
        }
    }

    /** The canonical fixture's section of this title. */
    private static CvSection canonicalSection(String title) {
        for (CvDocument.Placement placement
                : ProfessionalSidebarFixtures.canonicalCv().placements()) {
            if (placement.section().title().equals(title)) {
                return placement.section();
            }
        }
        throw new AssertionError("The canonical fixture has no " + title);
    }

    /** The canonical first role, then the long fixture's first four contract roles. */
    private static List<CvEntry> firstRoleAndFourContracts() {
        for (CvDocument.Placement placement : ProfessionalSidebarFixtures.longCv().placements()) {
            if (placement.section() instanceof EntriesSection entries
                    && entries.title().equals("EXPERIENCE")) {
                List<CvEntry> roles = new ArrayList<>();
                roles.add(entries.entries().get(0));
                for (CvEntry entry : entries.entries()) {
                    if (entry.title().matches("CONTRACT DEVELOPER [1-4]")) {
                        roles.add(entry);
                    }
                }
                return roles;
            }
        }
        throw new AssertionError("The long fixture has no EXPERIENCE");
    }

    @Test
    void drawsNothingTheDesignHasNoPlaceFor() throws Exception {
        // A berth is filled once, and three fields have nowhere to go on this
        // sheet. A caller who puts content there gets none of it drawn, which
        // is why the class says so.
        String text = textOf(render(CvDocument.builder()
                .identity(identity())
                .section(EntriesSection.builder("PROJECTS")
                        .entry("LEDGERKIT", "Client name here", "2024", "A ledger.")
                        .build())
                .section(EntriesSection.builder("EDUCATION")
                        .entry("BSc", "University Name", "2016", "Thesis title here")
                        .build())
                .section(SkillsSection.of("SKILLS",
                        SkillGroup.of("Category name here", "Java")))
                .section(EntriesSection.builder("EXPERIENCE")
                        .entry("FIRST ROLE", "Employer", "2020", "Did a thing.")
                        .build())
                .section(EntriesSection.builder("MORE EXPERIENCE")
                        .entry("SECOND ROLE", "Employer", "2021", "Did another thing.")
                        .build())
                .build()));
        assertThat(text)
                .contains("LEDGERKIT")
                .contains("University Name")
                .contains("Java")
                .contains("FIRST ROLE")
                .doesNotContain("Client name here")
                .doesNotContain("Thesis title here")
                .doesNotContain("Category name here")
                .doesNotContain("SECOND ROLE");
    }

    private static CvIdentity identity() {
        return CvIdentity.builder()
                .name("Ada", "Lovelace")
                .jobTitle("ANALYST")
                .contact(new Contact("+44 20 7946 0000", "empty@example.com", "London, UK"))
                .link(new Link("linkedin.com/in/ada", "https://linkedin.com/in/ada"))
                .build();
    }
}
