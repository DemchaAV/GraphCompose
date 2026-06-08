package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.data.EntriesSection;
import com.demcha.compose.document.templates.cv.v2.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.v2.data.RowStyle;
import com.demcha.compose.document.templates.cv.v2.data.RowsSection;
import com.demcha.compose.document.templates.cv.v2.data.SkillsSection;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.theme.CvTypography;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Smoke test for the v2 Engineering Resume preset. Covers the navy
 * command header with subtitle + contact stack, plus the 2-column
 * rail / main-card composition fed through
 * {@link com.demcha.compose.document.templates.cv.v2.components.SectionLookup}.
 */
class EngineeringResumeSmokeTest {

    // this flag exists to allow us to emit the PDF if we want to exmine it visually.
    private static final boolean SAVE_MULTILINGUAL_PDF = true;
    private static final FontName MULTISCRIPT_FONT = FontName.of("Test Multiscript");
    // Hungarian sample: classic accented-Latin unicode test phrase,
    // often glossed as "floodproof mirror-drilling machine".
    private static final String HUNGARIAN_TEXT = "Árvíztűrő tükörfúrógép";
    // Hebrew sample: "shalom olam" / "hello world".
    private static final String HEBREW_TEXT = "שלום עולם";
    // Arabic sample: "marhaban bil-alam" / "hello world".
    private static final String ARABIC_TEXT = "مرحبا بالعالم";

    @Test
    void exposes_stable_identity() {
        DocumentTemplate<CvDocument> template = EngineeringResume.create();
        assertThat(template.id()).isEqualTo("engineering-resume");
        assertThat(template.displayName()).isEqualTo("Engineering Resume");
    }

    @Test
    void default_factory_renders_full_document() throws Exception {
        renderAndAssertNonEmpty(EngineeringResume.create(), fullDocument());
    }

    @Test
    void custom_theme_factory_renders() throws Exception {
        renderAndAssertNonEmpty(EngineeringResume.create(CvTheme.engineeringResume()),
                fullDocument());
    }

    @Test
    void custom_multiscript_theme_renders_hungarian_hebrew_and_arabic_to_pdf() throws Exception {
        Path fontPath = resolveMultiscriptFont();
        assumeTrue(fontPath != null, "requires a local font with Hebrew + Arabic + Latin coverage");

        DocumentTemplate<CvDocument> template = EngineeringResume.create(multiscriptTheme());

        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 595)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.registerFontFamily(FontFamilyDefinition.files(MULTISCRIPT_FONT, fontPath).build());
            template.compose(session, multilingualDocument());

            byte[] pdfBytes = session.toPdfBytes();
            maybeWriteMultilingualPdf(pdfBytes);
            assertThat(pdfBytes).hasSizeGreaterThan(500);
            assertThat(new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                assertThat(document.getNumberOfPages()).isGreaterThan(0);

                String extracted = new PDFTextStripper().getText(document);
                // Hungarian is the LTR Unicode control in this test: it proves
                // the preset can carry non-English text through a real resume
                // render without introducing RTL ordering/shaping ambiguity.
                assertThat(extracted).contains(HUNGARIAN_TEXT);

                // We intentionally do not assert exact PDFTextStripper output
                // for Hebrew or Arabic. PDF text extraction is heuristic and
                // may reorder or normalize RTL runs independently of whether
                // the renderer successfully painted the correct glyphs.
                //
                // The thing being testeded here is resume-template PDF generation
                // with a font that genuinely covers Hebrew + Arabic + Latin,
                // not PDFBox's exact bidi extraction behavior.
            }
        }
    }

    private static void renderAndAssertNonEmpty(
            DocumentTemplate<CvDocument> template,
            CvDocument doc) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 595)
                .margin(DocumentInsets.of(20))
                .create()) {
            template.compose(session, doc);
            assertThat(session.roots()).isNotEmpty();
        }
    }

    private static CvDocument fullDocument() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jane", "Doe")
                        .jobTitle("Senior Platform Engineer")
                        .contact("+44 0", "j@d.com", "London")
                        .link("LinkedIn", "https://linkedin.com/in/jane-doe")
                        .link("GitHub", "https://github.com/jane")
                        .build())
                .sections(
                        new ParagraphSection("Professional Summary",
                                "Builds **reliable** document pipelines."),
                        SkillsSection.builder("Technical Skills")
                                .group("Languages", "Java 21", "Kotlin")
                                .group("Testing", "JUnit 5", "AssertJ")
                                .build(),
                        EntriesSection.builder("Education & Certifications")
                                .entry("MSc Computer Science",
                                        "University of Manchester",
                                        "2019-2021",
                                        "Distinction.")
                                .build(),
                        RowsSection.builder("Projects", RowStyle.BULLETED_STACKED)
                                .row("GraphCompose (Java, PDFBox)",
                                        "Declarative PDF layout engine.")
                                .build(),
                        EntriesSection.builder("Professional Experience")
                                .entry("Senior Platform Engineer", "Acme",
                                        "2021-2024",
                                        "Built rendering services.")
                                .build(),
                        RowsSection.builder("Additional Information", RowStyle.PLAIN)
                                .row("Languages", "English, German")
                                .build())
                .build();
    }

    private static CvDocument multilingualDocument() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("János", "Farkas")
                        .jobTitle("Senior Platform Engineer")
                        .contact("+36 30 555 0101", "janos@example.dev", "Budapest")
                        .link("LinkedIn", "https://linkedin.com/in/janos-farkas")
                        .link("GitHub", "https://github.com/janos")
                        .build())
                .sections(
                        new ParagraphSection("Professional Summary",
                                "Builds reliable multilingual document pipelines. Hungarian sample: "
                                        + HUNGARIAN_TEXT),
                        SkillsSection.builder("Technical Skills")
                                .group("Languages", "Java 21", "Kotlin", "SQL")
                                .group("Internationalisation", HEBREW_TEXT, ARABIC_TEXT)
                                .build(),
                        EntriesSection.builder("Education & Certifications")
                                .entry("MSc Computer Science",
                                        "Budapest University",
                                        "2019-2021",
                                        "Focused on document systems.")
                                .build(),
                        RowsSection.builder("Projects", RowStyle.BULLETED_STACKED)
                                .row("GraphCompose",
                                        "Resume PDF generation with multilingual content.")
                                .build(),
                        EntriesSection.builder("Professional Experience")
                                .entry("Senior Platform Engineer", "Acme",
                                        "2021-2024",
                                        "Shipped CV rendering and localization workflows.")
                                .build(),
                        RowsSection.builder("Additional Information", RowStyle.PLAIN)
                                .row("Hebrew sample", HEBREW_TEXT)
                                .row("Arabic sample", ARABIC_TEXT)
                                .row("Hungarian sample", HUNGARIAN_TEXT)
                                .build())
                .build();
    }

    private static CvTheme multiscriptTheme() {
        CvTheme base = CvTheme.engineeringResume();
        CvTypography typography = new CvTypography(
                MULTISCRIPT_FONT,
                MULTISCRIPT_FONT,
                base.typography().sizeHeadline(),
                base.typography().sizeContact(),
                base.typography().sizeBanner(),
                base.typography().sizeEntryTitle(),
                base.typography().sizeEntryDate(),
                base.typography().sizeEntrySubtitle(),
                base.typography().sizeBody(),
                base.typography().bodyLineSpacing());
        return new CvTheme(base.palette(), typography, base.spacing(), base.decoration());
    }

    private static Path resolveMultiscriptFont() {
        List<Path> candidates = List.of(
                Path.of("/Library/Fonts/Arial Unicode.ttf"),
                Path.of("/System/Library/Fonts/Supplemental/Arial Unicode.ttf"),
                Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
                Path.of("/usr/share/fonts/truetype/freefont/FreeSans.ttf"));

        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static void maybeWriteMultilingualPdf(byte[] pdfBytes) throws Exception {
        if (!SAVE_MULTILINGUAL_PDF) {
            return;
        }
        Path output = Path.of("target", "visual-tests", "cv-v2", "engineering-resume-multilingual.pdf");
        Files.createDirectories(output.getParent());
        Files.write(output, pdfBytes);
    }
}
