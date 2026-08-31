package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.exceptions.AtomicNodeTooLargeException;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
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
 * Smoke test for {@link CharcoalGold} — proves the preset renders a
 * {@link CvDocument} end-to-end with its packaged marks, reaches its
 * channels and titles as PDF links, and pins the contracts the class
 * documents: the credential marks are this preset's own vocabulary, an
 * entry without one is drawn plain, a skill without a level draws no
 * rating, and the sheet holds one page.
 */
class CharcoalGoldSmokeTest {

    private static byte[] render(CvDocument doc) throws Exception {
        try (DocumentSession session = open()) {
            CharcoalGold.create().compose(session, doc);
            assertThat(session.roots()).isNotEmpty();
            byte[] pdfBytes = session.toPdfBytes();
            assertThat(pdfBytes).isNotEmpty();
            return pdfBytes;
        }
    }

    /** Both columns run to the paper edge, so the page carries no margin. */
    private static DocumentSession open() {
        return GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(0f, 0f, 0f, 0f)
                .create();
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
        DocumentTemplate<CvDocument> template = CharcoalGold.create();
        assertThat(template.id()).isEqualTo(CharcoalGold.ID);
        assertThat(template.displayName()).isEqualTo(CharcoalGold.DISPLAY_NAME);
    }

    @Test
    void canonicalRenderCarriesTheText() throws Exception {
        String text = textOf(render(CharcoalGoldFixtures.canonicalCv()));
        assertThat(text)
                .contains("hello@anastasiasmith.com")
                .contains("London, United Kingdom")
                .contains("Results-driven Project Manager")
                .contains("Project Management")
                .contains("B2 – Upper Intermediate")
                .contains("MSc Project Management")
                .contains("Senior Project Manager")
                .contains("DIGITAL SOLUTIONS LTD   ·   LONDON, UK")
                .contains("Lead end-to-end delivery of enterprise software projects")
                .contains("PMP® Certification")
                .contains("Excellence in Delivery Award")
                .contains("Confluence");
    }

    @Test
    void rendersOneCanonicalPage() throws Exception {
        try (PDDocument document =
                     Loader.loadPDF(render(CharcoalGoldFixtures.canonicalCv()))) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void channelsReachThePdfAsLinks() throws Exception {
        // The dial and mail targets are built from the values. They are
        // annotations, so they move no pixel and no layout node — which is
        // why the parity gates cannot see them either.
        assertThat(linkUris(render(CharcoalGoldFixtures.canonicalCv())))
                .contains("tel:+447700900123",
                        "mailto:hello@anastasiasmith.com",
                        "https://anastasiasmith.com",
                        "https://www.linkedin.com/in/anastasiasmith");
    }

    @Test
    void aTitleWithALinkReachesThePdfAsOne() throws Exception {
        byte[] pdf = render(CvDocument.builder()
                .identity(identity())
                .section(EntriesSection.builder("Experience")
                        .entry(CvEntry.builder("Engineer")
                                .subtitle("Acme")
                                .date("2024")
                                .body("Shipped a thing.")
                                .link("https://acme.example.com")
                                .build())
                        .build())
                .section(EntriesSection.builder("Certifications")
                        .entry(CvEntry.builder("Certified Something")
                                .subtitle("An Institute")
                                .date("2023")
                                .icon("certificate")
                                .link("https://example.com/cert")
                                .build())
                        .build())
                .build());
        assertThat(linkUris(pdf))
                .contains("https://acme.example.com", "https://example.com/cert");
    }

    @Test
    void rejectsAnUnknownCredentialMarkByName() {
        CvDocument doc = CvDocument.builder()
                .identity(identity())
                .section(EntriesSection.builder("Achievements")
                        .entry(CvEntry.builder("Award").icon("no-such-mark").build())
                        .build())
                .build();
        try (DocumentSession session = open()) {
            assertThatThrownBy(() -> CharcoalGold.create().compose(session, doc))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no-such-mark")
                    .hasMessageContaining("trophy");
        }
    }

    @Test
    void drawsACredentialWithoutAMark() throws Exception {
        String text = textOf(render(CvDocument.builder()
                .identity(identity())
                .section(EntriesSection.builder("Certifications")
                        .entry(CvEntry.builder("Certified Something")
                                .subtitle("An Institute")
                                .date("2023")
                                .build())
                        .build())
                .build()));
        assertThat(text).contains("Certified Something").contains("An Institute");
    }

    @Test
    void drawsAnUnlevelledSkillWithoutARating() throws Exception {
        // A rating needs a level; without one the row is the name and its
        // bullet, not five empty dots that read as a score of zero.
        String text = textOf(render(CvDocument.builder()
                .identity(identity())
                .section(SkillsSection.of("Skills", SkillGroup.of("Core", "Java", "Kotlin")))
                .build()));
        assertThat(text).contains("Java").contains("Kotlin");
    }

    @Test
    void rendersWithoutAPortrait() throws Exception {
        // No photograph: the sidebar starts at the contact block rather than
        // leaving a hole above it.
        String text = textOf(render(CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Ada", "Lovelace")
                        .jobTitle("Analyst")
                        .contact(new Contact("+44 20 7946 0000", "ada@example.com",
                                "London, UK"))
                        .build())
                .build()));
        assertThat(text).contains("ada@example.com");
    }

    @Test
    void writesTheLanguageLevelTheDocumentCarries() throws Exception {
        // Languages are rows rather than levelled skills precisely so the
        // words survive; the row style is not read.
        String text = textOf(render(CvDocument.builder()
                .identity(identity())
                .section(RowsSection.builder("Languages", RowStyle.BULLETED_STACKED)
                        .row("Portuguese", "B1 – Intermediate")
                        .build())
                .build()));
        assertThat(text).contains("Portuguese").contains("B1 – Intermediate");
    }

    @Test
    void refusesACvLongerThanTheSheet() {
        // The two columns are a single row, and a row is atomic. A CV past
        // the sheet raises rather than flowing.
        EntriesSection.Builder experience = EntriesSection.builder("Experience");
        for (int i = 0; i < 16; i++) {
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
        try (DocumentSession session = open()) {
            CharcoalGold.create().compose(session, doc);
            assertThatThrownBy(session::toPdfBytes)
                    .isInstanceOf(AtomicNodeTooLargeException.class)
                    .hasMessageContaining("Body");
        }
    }

    private static CvIdentity identity() {
        return CvIdentity.builder()
                .name("Ada", "Lovelace")
                .jobTitle("Analyst")
                .contact(new Contact("+44 20 7946 0000", "ada@example.com", "London, UK"))
                .link(new Link("linkedin.com/in/ada", "https://linkedin.com/in/ada"))
                .portrait(CvFixturePortrait.silhouette())
                .build();
    }
}
