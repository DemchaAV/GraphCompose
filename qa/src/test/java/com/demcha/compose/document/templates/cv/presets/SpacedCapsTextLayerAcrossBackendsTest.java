package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.pptx.PptxFixedLayoutBackend;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.SkillsSection;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A migrated preset, read back out of all three formats.
 *
 * <p>The name in a CV is the field the document is searched by, and while the
 * spaced-caps look was made by rewriting the string it was the one field not in
 * the file: {@code "J A N E   D O E"} is what an applicant-tracking parser got.
 * This holds the migration to its purpose in each format — the picture is
 * spaced, the text is not.</p>
 *
 * <p>The fixture carries the cases the old transform handled by three separate
 * rules: several words, digits, and punctuation. It inserted one space between
 * adjacent letters <em>or digits</em>, widened an authored space into three,
 * and did neither around punctuation, so those are where it did the most
 * damage.</p>
 */
class SpacedCapsTextLayerAcrossBackendsTest {

    private static final String FIRST = "Jane";
    private static final String LAST = "O'Doe-Smith 3rd";
    private static final String FULL_UPPER = "JANE O'DOE-SMITH 3RD";
    private static final String TITLE_UPPER = "BACKEND ENGINEER";

    /** Four or more single letters or digits in a row, each stranded by spaces. */
    private static final Pattern SPELLED_OUT =
            Pattern.compile("\\b(?:[A-Za-z0-9] ){3,}[A-Za-z0-9]\\b");

    private static final Supplier<DocumentTemplate<CvDocument>> PRESET = MintEditorial::create;

    @Test
    void thePdfTextLayerHoldsTheNameWithItsDigitsAndPunctuation() throws Exception {
        String text = pdfText();

        assertThat(text).contains(FULL_UPPER);
        assertThat(text).contains(TITLE_UPPER);
        assertThat(text).doesNotContain("J A N E");
        assertThat(SPELLED_OUT.matcher(text).find())
                .describedAs("something is spelled out letter by letter in: %s", text)
                .isFalse();
    }

    @Test
    void thePptxRunsHoldTheNameAndDeclareTheTrackingNatively() throws Exception {
        byte[] pptx = render(session -> session.render(new PptxFixedLayoutBackend()));

        StringBuilder text = new StringBuilder();
        boolean sawTracking = false;
        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
            for (XSLFShape shape : show.getSlides().get(0).getShapes()) {
                if (!(shape instanceof XSLFTextShape textShape)) {
                    continue;
                }
                for (XSLFTextParagraph paragraph : textShape.getTextParagraphs()) {
                    for (XSLFTextRun run : paragraph.getTextRuns()) {
                        text.append(run.getRawText()).append(' ');
                        sawTracking |= run.getXmlObject().xmlText().contains("spc=");
                    }
                }
            }
        }

        assertThat(text.toString()).contains(FULL_UPPER);
        assertThat(sawTracking)
                .describedAs("no run declared spc, so the deck is not actually tracked")
                .isTrue();
        assertThat(SPELLED_OUT.matcher(text.toString()).find()).isFalse();
    }

    @Test
    void theDocxRunsHoldTheNameAndDeclareTheTrackingNatively() throws Exception {
        byte[] docx = render(session -> session.export(new DocxSemanticBackend()));

        StringBuilder text = new StringBuilder();
        boolean sawTracking = false;
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                for (XWPFRun run : paragraph.getRuns()) {
                    text.append(run.text()).append(' ');
                    sawTracking |= run.getCTR().xmlText().contains("spacing");
                }
            }
        }

        assertThat(text.toString()).contains(FULL_UPPER);
        assertThat(sawTracking)
                .describedAs("no run declared w:spacing, so the document is not tracked")
                .isTrue();
        assertThat(SPELLED_OUT.matcher(text.toString()).find()).isFalse();
    }

    // --- helpers ---------------------------------------------------------

    private static String pdfText() throws Exception {
        byte[] pdf = render(DocumentSession::toPdfBytes);
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }

    @FunctionalInterface
    private interface Export {
        byte[] from(DocumentSession session) throws Exception;
    }

    private static byte[] render(Export export) throws Exception {
        float m = (float) MintEditorial.RECOMMENDED_MARGIN;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(m, m, m, m)
                .create()) {
            PRESET.get().compose(session, document());
            return export.from(session);
        }
    }

    private static CvDocument document() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name(FIRST, LAST)
                        .jobTitle("Backend Engineer")
                        .contact("+44 0", "j@d.com", "London")
                        .build())
                .sections(
                        new ParagraphSection("Professional Summary", "Platform work."),
                        SkillsSection.builder("Technical Skills")
                                .group("Languages", "Java", "Kotlin")
                                .build(),
                        EntriesSection.builder("Professional Experience")
                                .entry("Senior Engineer", "Acme Rendering",
                                        "2021-2024", "Built rendering services.")
                                .build())
                .build();
    }
}
