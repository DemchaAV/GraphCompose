package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.exceptions.AtomicNodeTooLargeException;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvSkill;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.RowStyle;
import com.demcha.compose.document.templates.cv.data.RowsSection;
import com.demcha.compose.document.templates.cv.data.SkillGroup;
import com.demcha.compose.document.templates.cv.data.SkillsSection;
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
 * Smoke test for {@link NavySidebar} — proves the preset renders a
 * {@link CvDocument} end-to-end with its packaged marks and the portrait
 * from the identity, composes without one, and pins the limits the class
 * documents: the sheet holds one page, and the fields this design has no
 * place for are not drawn.
 */
class NavySidebarSmokeTest {

    private static byte[] render(CvDocument doc) throws Exception {
        // The preset owns its page geometry, so the session starts unconfigured.
        try (DocumentSession session = GraphCompose.document().create()) {
            NavySidebar.create().compose(session, doc);
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
        DocumentTemplate<CvDocument> template = NavySidebar.create();
        assertThat(template.id()).isEqualTo(NavySidebar.ID);
        assertThat(template.displayName()).isEqualTo(NavySidebar.DISPLAY_NAME);
    }

    @Test
    void canonicalRenderCarriesTheUntrackedText() throws Exception {
        // The headings, the name and the role are set letter by letter to
        // reach their tracking, so they arrive on the text layer spaced out.
        // What is asserted here is the text drawn as written.
        String text = textOf(render(NavySidebarFixtures.canonicalCv()));
        assertThat(text)
                .contains("your.email@gmail.com")
                .contains("New York, NY, USA")
                .contains("linkedin.com/in/yourname")
                .contains("Results-driven marketing professional")
                .contains("New York University")
                .contains("Digital Marketing")
                .contains("Native")
                .contains("BrightWave Solutions, New York, NY")
                .contains("Increased website traffic by 60%")
                .contains("HubSpot Content Marketing Certification");
    }

    @Test
    void channelsReachThePdfAsLinks() throws Exception {
        // The dial and mail targets are built from the values, so nothing in
        // the document carries them and nothing but the PDF can show them.
        // They are annotations, so they move no pixel and no layout node —
        // which is why the parity gates cannot see them either.
        assertThat(linkUris(render(NavySidebarFixtures.canonicalCv())))
                .contains("tel:+15551234567",
                        "mailto:your.email@gmail.com",
                        "https://linkedin.com/in/yourname");
    }

    @Test
    void rendersOneCanonicalPage() throws Exception {
        try (PDDocument document =
                     Loader.loadPDF(render(NavySidebarFixtures.canonicalCv()))) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void rendersWithoutAPortraitOrAJobTitle() throws Exception {
        // No photograph: the ring is drawn around an empty navy disc rather
        // than left as a hole in the column. And no role: that line is
        // written letter by letter to reach its tracking, so a blank one
        // reaches the engine as a paragraph holding no runs at all.
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
    void degreesAndTitlesAreDrawnInCapitals() throws Exception {
        // This design upper-cases for the author — the name, the role, each
        // degree and every heading — so a document written in title case
        // still reads as the sheet it is.
        String text = textOf(render(CvDocument.builder()
                .identity(identity())
                .section(EntriesSection.builder("Education")
                        .entry("Master of Science", "Some University", "2019", "Berlin")
                        .build())
                .build()));
        assertThat(text).contains("MASTER OF SCIENCE").doesNotContain("Master of Science");
    }

    @Test
    void drawsNothingTheDesignHasNoPlaceFor() throws Exception {
        // A berth is filled once, and this sheet has nowhere to put a skill
        // level, a group category or the summary's own title.
        String text = textOf(render(CvDocument.builder()
                .identity(identity())
                .section(new ParagraphSection("Summary title here", "The opening paragraph."))
                .section(SkillsSection.of("Skills",
                        new SkillGroup("Category name here", List.of(CvSkill.of("Java", 0.8)))))
                .section(new ParagraphSection("Achievements", "Shipped a thing."))
                .section(new ParagraphSection("More achievements", "Second block here."))
                .build()));
        assertThat(text)
                .contains("The opening paragraph.")
                .contains("Java")
                .contains("Shipped a thing.")
                .doesNotContain("Summary title here")
                .doesNotContain("Category name here")
                .doesNotContain("Second block here.");
    }

    @Test
    void writesTheProficiencyTheDocumentCarries() throws Exception {
        // Languages are rows rather than levelled skills precisely so the
        // words survive; the row style is not read.
        String text = textOf(render(CvDocument.builder()
                .identity(identity())
                .section(RowsSection.builder("Languages", RowStyle.BULLETED_STACKED)
                        .row("Portuguese", "Conversational")
                        .build())
                .build()));
        assertThat(text).contains("Portuguese").contains("Conversational");
    }

    @Test
    void refusesACvLongerThanTheSheet() {
        // The sheet holds one page: the two columns are a single row, and a
        // row is atomic. A CV past that raises rather than flowing.
        EntriesSection.Builder experience = EntriesSection.builder("Experience");
        for (int i = 0; i < 14; i++) {
            experience.entry("Role " + i, "Employer " + i + ", City", "2010 - 2011",
                    String.join("\n",
                            "Did a substantial thing that took a full line of text here.",
                            "Did a second substantial thing, also a full line of text.",
                            "And a third, so each role occupies real vertical space."));
        }
        CvDocument doc = CvDocument.builder()
                .identity(identity())
                .section(experience.build())
                .build();
        try (DocumentSession session = GraphCompose.document().create()) {
            NavySidebar.create().compose(session, doc);
            assertThatThrownBy(session::toPdfBytes)
                    .isInstanceOf(AtomicNodeTooLargeException.class)
                    .hasMessageContaining("PageGrid");
        }
    }

    private static CvIdentity identity() {
        return CvIdentity.builder()
                .name("Ada", "Lovelace")
                .jobTitle("Analyst")
                .contact(new Contact("+44 20 7946 0000", "empty@example.com", "London, UK"))
                .link(new Link("linkedin.com/in/ada", "https://linkedin.com/in/ada"))
                .portrait(NavySidebarFixtures.portrait())
                .build();
    }
}
