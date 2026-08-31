package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.exceptions.AtomicNodeTooLargeException;
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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Smoke test for {@link SerifHeadline} — proves the preset renders a
 * {@link CvDocument} end-to-end with its packaged marks, reaches its
 * channels as PDF links, and pins the contracts the class documents: the
 * entry marks are this preset's own vocabulary, an entry without one is
 * drawn plain, the sheet holds one page, and the two blocks whose titles
 * overlap reach the right berths.
 */
class SerifHeadlineSmokeTest {

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
