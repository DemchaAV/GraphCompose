package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.backend.fixed.pdf.PdfFixedLayoutBackend;
import com.demcha.compose.document.backend.fixed.pptx.PptxFixedLayoutBackend;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLetterSpacing;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * One authored style, all three backends.
 *
 * <p>The point of {@code DocumentLetterSpacing} keeping its unit is that one
 * declaration means the same thing everywhere. This takes a single
 * {@code ofFontSize(0.12)} at 20pt — 2.4 points of tracking — and checks that
 * each backend received <em>that</em> amount expressed in its own native unit,
 * and that none of them touched the author's text to get it.</p>
 *
 * <p>The three units are genuinely different numbers for the same distance:
 * 2.4pt is {@code spc="240"} in DrawingML's hundredths, {@code w:val="48"} in
 * Word's twentieths, and a {@code Tc} of 2.4 in the PDF's points. A test that
 * asserted one number across all three would be asserting a bug.</p>
 */
class LetterSpacingAcrossBackendsTest {

    private static final String NAME = "JANE DOE";
    /** 12% of 20pt. */
    private static final double EXPECTED_POINTS = 2.4;

    private static final Pattern SPC = Pattern.compile("spc=\"(-?[0-9]+)\"");
    private static final Pattern W_SPACING = Pattern.compile("spacing[^/>]*val=\"(-?[0-9]+)\"");

    private static final DocumentTextStyle TRACKED = DocumentTextStyle.builder()
            .fontName(FontName.LATO)
            .size(20)
            .letterSpacing(DocumentLetterSpacing.ofFontSize(0.12))
            .build();

    @Test
    void theStyleResolvesToTheSameDistanceBeforeAnyBackendSeesIt() {
        assertThat(TRACKED.letterSpacing().resolve(TRACKED.size())).isEqualTo(EXPECTED_POINTS);
    }

    @Test
    void pptxReceivesItInHundredthsOfAPoint() throws Exception {
        byte[] pptx = render(session -> session.render(new PptxFixedLayoutBackend()));

        assertThat(values(pptxRunXml(pptx), SPC))
                .isNotEmpty()
                .allMatch(value -> value == (int) Math.round(EXPECTED_POINTS * 100));
        assertThat(pptxText(pptx)).isEqualTo(NAME);
    }

    @Test
    void docxReceivesItInTwentiethsOfAPoint() throws Exception {
        byte[] docx = render(session -> session.export(new DocxSemanticBackend()));

        assertThat(values(docxRunXml(docx), W_SPACING))
                .isNotEmpty()
                .allMatch(value -> value == (int) Math.round(EXPECTED_POINTS * 20));
        assertThat(docxText(docx)).isEqualTo(NAME);
    }

    @Test
    void pdfReceivesItAsPointsOfCharacterSpacing() throws Exception {
        byte[] pdf = render(session -> session.render(new PdfFixedLayoutBackend()));

        // Tc is written in points, so the operator carries the resolved value
        // itself rather than a converted one.
        assertThat(contentStream(pdf)).containsPattern("2\\.4\\d*\\s+Tc");
        assertThat(pdfText(pdf)).isEqualTo(NAME);
    }

    @Test
    void noBackendPadsTheTextToFakeIt() throws Exception {
        // The whole reason the feature exists: the picture is spaced out and the
        // text is not.
        assertThat(pptxText(render(s -> s.render(new PptxFixedLayoutBackend())))).isEqualTo(NAME);
        assertThat(docxText(render(s -> s.export(new DocxSemanticBackend())))).isEqualTo(NAME);
        assertThat(pdfText(render(s -> s.render(new PdfFixedLayoutBackend())))).isEqualTo(NAME);
    }

    // --- helpers ---------------------------------------------------------

    /** An export that is allowed to fail, which every backend's is. */
    @FunctionalInterface
    private interface Export {
        byte[] from(DocumentSession session) throws Exception;
    }

    private static byte[] render(Export export) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(500, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(page -> page.addParagraph(p -> p.text(NAME).textStyle(TRACKED)));
            return export.from(session);
        }
    }

    private static List<Integer> values(List<String> xml, Pattern pattern) {
        List<Integer> values = new ArrayList<>();
        for (String fragment : xml) {
            Matcher matcher = pattern.matcher(fragment);
            if (matcher.find()) {
                values.add(Integer.valueOf(matcher.group(1)));
            }
        }
        return values;
    }

    private static List<String> pptxRunXml(byte[] pptx) throws Exception {
        List<String> xml = new ArrayList<>();
        for (XSLFTextRun run : pptxRuns(pptx)) {
            xml.add(run.getXmlObject().xmlText());
        }
        return xml;
    }

    private static List<XSLFTextRun> pptxRuns(byte[] pptx) throws Exception {
        List<XSLFTextRun> runs = new ArrayList<>();
        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
            for (XSLFShape shape : show.getSlides().get(0).getShapes()) {
                if (shape instanceof XSLFTextShape textShape) {
                    for (XSLFTextParagraph paragraph : textShape.getTextParagraphs()) {
                        runs.addAll(paragraph.getTextRuns());
                    }
                }
            }
        }
        return runs;
    }

    private static String pptxText(byte[] pptx) throws Exception {
        StringBuilder text = new StringBuilder();
        for (XSLFTextRun run : pptxRuns(pptx)) {
            text.append(run.getRawText());
        }
        return text.toString();
    }

    private static List<String> docxRunXml(byte[] docx) throws Exception {
        List<String> xml = new ArrayList<>();
        for (XWPFRun run : docxRuns(docx)) {
            xml.add(run.getCTR().xmlText());
        }
        return xml;
    }

    private static List<XWPFRun> docxRuns(byte[] docx) throws Exception {
        List<XWPFRun> runs = new ArrayList<>();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                runs.addAll(paragraph.getRuns());
            }
        }
        return runs;
    }

    private static String docxText(byte[] docx) throws Exception {
        StringBuilder text = new StringBuilder();
        for (XWPFRun run : docxRuns(docx)) {
            text.append(run.text());
        }
        return text.toString();
    }

    private static String pdfText(byte[] pdf) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).trim();
        }
    }

    private static String contentStream(byte[] pdf) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new String(document.getPage(0).getContents().readAllBytes(),
                    java.nio.charset.StandardCharsets.ISO_8859_1);
        }
    }
}
