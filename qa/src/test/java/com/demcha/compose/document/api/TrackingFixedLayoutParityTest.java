package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.backend.fixed.pdf.PdfFixedLayoutBackend;
import com.demcha.compose.document.backend.fixed.pptx.PptxFixedLayoutBackend;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLetterSpacing;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

/**
 * One tracking value, one fixed-layout number.
 *
 * <p>A fixed-layout document reserves space, wraps, aligns and sizes its frames
 * from the width the engine measured. PPTX can only declare spacing in
 * hundredths of a point, so if the engine measured a finer value than that, the
 * width it reserved is a width the deck will never draw. The engine therefore
 * measures on the grid the file can express, and this holds the three numbers
 * together: what the engine measured, what the PDF's {@code Tc} says, and what
 * the deck's {@code spc} says.</p>
 *
 * <p>This is <em>not</em> a claim that a PDF and a deck rasterise identically.
 * Measured by exporting both through PowerPoint, an <em>untracked</em>
 * forty-glyph line already lands 0.77pt apart, because PowerPoint has its own
 * font handling and rounds its own output. That difference is not ours to
 * remove. The one in this test is: it is arithmetic we perform, it is knowable,
 * and it accumulates with the length of the string.</p>
 */
class TrackingFixedLayoutParityTest {

    /** Long enough that a per-code-point residue would be unmistakable. */
    private static final String LONG = "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMN";
    private static final Pattern SPC = Pattern.compile("spc=\"(-?[0-9]+)\"");
    private static final Pattern TC = Pattern.compile("(-?[0-9]*\\.?[0-9]+)\\s+Tc");
    private static final Pattern W_SPACING = Pattern.compile("spacing[^/>]*val=\"(-?[0-9]+)\"");

    private static DocumentTextStyle style(DocumentLetterSpacing spacing) {
        return DocumentTextStyle.builder()
                .fontName(FontName.LATO).size(20).letterSpacing(spacing).build();
    }

    // --- the invariant ---------------------------------------------------

    @ParameterizedTest(name = "[{index}] {0} pt")
    @ValueSource(doubles = {1.0 / 3.0, 0.005, -1.0 / 3.0, 0.125, 2.0 / 7.0})
    void whatThePdfDeclaresIsWhatTheDeckDeclares(double points) throws Exception {
        DocumentTextStyle style = style(DocumentLetterSpacing.points(points));

        int spc = spcOf(render(style, s -> s.render(new PptxFixedLayoutBackend())));
        double tc = tcOf(render(style, s -> s.render(new PdfFixedLayoutBackend())));

        // Both files state the same distance. Unquantised, the PDF said
        // 0.3333333333333333 where the deck said 0.33.
        assertThat(spc / 100.0).as("PPTX spc=%d against PDF Tc=%s", spc, tc).isEqualTo(tc);
    }

    @ParameterizedTest(name = "[{index}] {0} pt")
    @ValueSource(doubles = {1.0 / 3.0, 0.005, -1.0 / 3.0, 0.125, 2.0 / 7.0})
    void whatTheEngineMeasuredIsWhatThoseFilesDeclare(double points) throws Exception {
        DocumentTextStyle style = style(DocumentLetterSpacing.points(points));

        int spc = spcOf(render(style, s -> s.render(new PptxFixedLayoutBackend())));

        // Read back off the LayoutGraph's measured widths, so this is the number
        // wrapping and alignment actually used. Derived as a difference of two
        // summed line widths, so it carries ordinary float residue — the gap
        // being ruled out is four orders of magnitude larger.
        assertThat(engineTracking(style))
                .as("engine measurement against declared spc=%d", spc)
                .isCloseTo(spc / 100.0, within(1e-9));
    }

    @ParameterizedTest(name = "[{index}] {0} pt")
    @ValueSource(doubles = {1.0 / 3.0, 0.005, -1.0 / 3.0, 2.0 / 7.0})
    void aLongStringCannotAccumulateDriftBetweenPdfAndPptx(double points) throws Exception {
        DocumentTextStyle style = style(DocumentLetterSpacing.points(points));

        double engine = engineTracking(style);
        int spc = spcOf(render(style, s -> s.render(new PptxFixedLayoutBackend())));
        int codePoints = LONG.codePointCount(0, LONG.length());

        // Per code point they agree, so over any length they still do.
        // Unquantised, a third of a point was 0.0033 out per code point and
        // 0.133pt out over this line — which is what this tolerance excludes.
        assertThat((spc / 100.0) * codePoints)
                .as("accumulated over %d code points", codePoints)
                .isCloseTo(engine * codePoints, within(1e-6));
    }

    @Test
    void aTrackingBelowHalfTheGridQuantisesAwayToNone() throws Exception {
        // Half-up rounding, which is Java's Math.round and what the PPTX
        // conversion already used: +0.005 reaches the first hundredth while
        // -0.005 does not. Stated here because it is the one asymmetry the grid
        // has, and it costs a hundredth of a point at the knife edge.
        assertThat(engineTracking(style(DocumentLetterSpacing.points(0.005))))
                .isCloseTo(0.01, within(1e-9));

        DocumentTextStyle justUnder = style(DocumentLetterSpacing.points(-0.005));
        assertThat(engineTracking(justUnder)).isCloseTo(0.0, within(1e-9));
        // No tracking left to declare, so nothing is written.
        assertThat(pptxRunXml(render(justUnder, s -> s.render(new PptxFixedLayoutBackend()))))
                .noneMatch(xml -> SPC.matcher(xml).find());
    }

    @Test
    void aValueAlreadyOnTheGridIsUntouched() throws Exception {
        DocumentTextStyle style = style(DocumentLetterSpacing.points(0.25));

        assertThat(engineTracking(style)).isCloseTo(0.25, within(1e-9));
        assertThat(spcOf(render(style, s -> s.render(new PptxFixedLayoutBackend())))).isEqualTo(25);
    }

    @Test
    void zeroStaysTheExactLegacyPath() throws Exception {
        DocumentTextStyle style = style(DocumentLetterSpacing.NONE);

        assertThat(engineTracking(style)).isCloseTo(0.0, within(1e-12));
        // Still no attribute and no operator at all.
        assertThat(pptxRunXml(render(style, s -> s.render(new PptxFixedLayoutBackend()))))
                .noneMatch(xml -> SPC.matcher(xml).find());
        assertThat(contentStream(render(style, s -> s.render(new PdfFixedLayoutBackend()))))
                .doesNotContain(" Tc");
    }

    @Test
    void anUntrackedDocumentIsTheSameBytesItWouldHaveBeenWithoutTheFeature() throws Exception {
        // The CHANGELOG says every existing document is byte-for-byte what it
        // was. That is a claim about output, so it is asserted against output:
        // a style that never mentions tracking and one that explicitly asks for
        // NONE must produce the identical file.
        DocumentTextStyle silent = DocumentTextStyle.builder()
                .fontName(FontName.LATO).size(20).build();
        DocumentTextStyle explicitNone = style(DocumentLetterSpacing.NONE);

        // PDF embeds a creation date and a document ID, so whole-file equality
        // is only a meaningful question in deterministic mode. It is the right
        // question, though — it covers the content stream and every dictionary.
        assertThat(render(explicitNone, s -> s.render(
                PdfFixedLayoutBackend.builder().deterministic(true).build())))
                .isEqualTo(render(silent, s -> s.render(
                        PdfFixedLayoutBackend.builder().deterministic(true).build())));

        // The OOXML pair carry timestamps of their own, so they are compared on
        // the run properties — which is where a stray zero would have shown up.
        assertThat(pptxRunXml(render(explicitNone, s -> s.render(new PptxFixedLayoutBackend()))))
                .isEqualTo(pptxRunXml(render(silent, s -> s.render(new PptxFixedLayoutBackend()))));
        assertThat(docxRunXml(render(explicitNone, s -> s.export(new DocxSemanticBackend()))))
                .isEqualTo(docxRunXml(render(silent, s -> s.export(new DocxSemanticBackend()))));
    }

    @Test
    void theAuthoredValueItselfIsNeverRewritten() {
        // Quantisation is a property of fixed layout, not of the value. What the
        // author wrote is what the style still says.
        DocumentLetterSpacing authored = DocumentLetterSpacing.points(1.0 / 3.0);
        DocumentTextStyle style = style(authored);

        assertThat(style.letterSpacing()).isSameAs(authored);
        assertThat(style.letterSpacing().value()).isEqualTo(1.0 / 3.0);
        assertThat(style.letterSpacing().resolve(20)).isEqualTo(1.0 / 3.0);
        assertThat(engineTracking(style)).isCloseTo(0.33, within(1e-9));
    }

    // --- DOCX keeps its own, coarser grid --------------------------------

    @Test
    void docxRoundsToItsOwnTwentiethsFromTheAuthoredValue() throws Exception {
        // Word's grid is 0.05pt, and a semantic export owes the fixed backends
        // no coordinate — so it rounds the authored value itself rather than
        // inheriting the hundredth the fixed path settled on.
        DocumentTextStyle style = style(DocumentLetterSpacing.points(1.0 / 3.0));

        // 1/3 pt -> 6.667 twentieths -> 7 twips = 0.35pt, which is neither the
        // authored third nor the fixed path's 0.33.
        assertThat(wSpacingOf(render(style, s -> s.export(new DocxSemanticBackend()))))
                .isEqualTo(7);
        assertThat(engineTracking(style)).isCloseTo(0.33, within(1e-9));
    }

    // --- range ------------------------------------------------------------

    @Test
    void aTrackingTooLargeForFixedLayoutIsRefusedRatherThanWrapped() {
        // 2.2e7 points would be 2_200_000_000 hundredths, which lands at
        // -2094967296 on the int cast: wide tracking silently becoming tight.
        DocumentTextStyle style = style(DocumentLetterSpacing.points(2.2e7));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> render(style, s -> s.render(new PdfFixedLayoutBackend())))
                .withMessageContaining("fixed-layout");
    }

    @Test
    void theFixedLayoutBoundIsTheOneTheSchemaActuallyEnforces() throws Exception {
        // ST_TextPoint validates 400000 and rejects 400001, so 4000pt is in and
        // anything past it is out.
        DocumentTextStyle inRange = style(DocumentLetterSpacing.points(4000.0));
        assertThat(spcOf(render(inRange, s -> s.render(new PptxFixedLayoutBackend()))))
                .isEqualTo(400000);

        DocumentTextStyle outOfRange = style(DocumentLetterSpacing.points(4000.01));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> render(outOfRange, s -> s.render(new PptxFixedLayoutBackend())));
    }

    @Test
    void aTrackingTooLargeForWordIsRefusedRatherThanWrapped() {
        // 1e9 points would be 20_000_000_000 twentieths, landing at -1474836480.
        DocumentTextStyle style = style(DocumentLetterSpacing.points(1e9));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> render(style, s -> s.export(new DocxSemanticBackend())))
                .withMessageContaining("Word run");
    }

    @Test
    void theValueTypeItselfStillAcceptsAnyFiniteNumber() {
        // The limits belong to the formats, not to the authored value: it is
        // only refused where it cannot be written.
        assertThat(DocumentLetterSpacing.points(1e9).resolve(20)).isEqualTo(1e9);
        assertThat(DocumentLetterSpacing.ofFontSize(1e9).resolve(20)).isEqualTo(2e10);
    }

    // --- helpers ---------------------------------------------------------

    /**
     * The tracking the engine actually measured with, per code point.
     *
     * <p>Read off the {@code LayoutGraph} — the measured span widths the whole
     * fixed-layout pipeline reserves space from — rather than off any internal
     * helper, so what is compared is the number that really drives wrapping,
     * alignment and frame sizing. Derived as the difference the tracking made
     * to the line, divided by the code points that were tracked.</p>
     */
    private static double engineTracking(DocumentTextStyle style) {
        double tracked = measuredLineWidth(style);
        double plain = measuredLineWidth(style.withLetterSpacing(DocumentLetterSpacing.NONE));
        return (tracked - plain) / LONG.codePointCount(0, LONG.length());
    }

    private static double measuredLineWidth(DocumentTextStyle style) {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(900, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(page -> page.addParagraph(p -> p.text(LONG).textStyle(style)));
            return session.layoutGraph().fragments().stream()
                    .map(fragment -> fragment.payload())
                    .filter(payload -> payload instanceof ParagraphFragmentPayload)
                    .map(payload -> (ParagraphFragmentPayload) payload)
                    .flatMap(payload -> payload.lines().stream())
                    .flatMap(line -> line.spans().stream())
                    .filter(span -> span instanceof ParagraphTextSpan)
                    .mapToDouble(span -> ((ParagraphTextSpan) span).width())
                    .sum();
        }
    }

    @FunctionalInterface
    private interface Export {
        byte[] from(DocumentSession session) throws Exception;
    }

    private static byte[] render(DocumentTextStyle style, Export export) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(900, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(page -> page.addParagraph(p -> p.text(LONG).textStyle(style)));
            return export.from(session);
        }
    }

    private static int spcOf(byte[] pptx) throws Exception {
        for (String xml : pptxRunXml(pptx)) {
            Matcher matcher = SPC.matcher(xml);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        throw new AssertionError("no spc written");
    }

    /** Every run's properties as the document spells them, in order. */
    private static List<String> docxRunXml(byte[] docx) throws Exception {
        List<String> xml = new ArrayList<>();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                for (XWPFRun run : paragraph.getRuns()) {
                    xml.add(run.getCTR().xmlText());
                }
            }
        }
        return xml;
    }

    private static int wSpacingOf(byte[] docx) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                for (XWPFRun run : paragraph.getRuns()) {
                    Matcher matcher = W_SPACING.matcher(run.getCTR().xmlText());
                    if (matcher.find()) {
                        return Integer.parseInt(matcher.group(1));
                    }
                }
            }
        }
        throw new AssertionError("no w:spacing written");
    }

    private static double tcOf(byte[] pdf) throws Exception {
        Matcher matcher = TC.matcher(contentStream(pdf));
        if (!matcher.find()) {
            throw new AssertionError("no Tc written");
        }
        return Double.parseDouble(matcher.group(1));
    }

    private static List<String> pptxRunXml(byte[] pptx) throws Exception {
        List<String> xml = new ArrayList<>();
        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
            for (XSLFShape shape : show.getSlides().get(0).getShapes()) {
                if (shape instanceof XSLFTextShape textShape) {
                    for (XSLFTextParagraph paragraph : textShape.getTextParagraphs()) {
                        for (XSLFTextRun run : paragraph.getTextRuns()) {
                            xml.add(run.getXmlObject().xmlText());
                        }
                    }
                }
            }
        }
        return xml;
    }

    private static String contentStream(byte[] pdf) throws Exception {
        try (org.apache.pdfbox.pdmodel.PDDocument document =
                     org.apache.pdfbox.Loader.loadPDF(pdf)) {
            return new String(document.getPage(0).getContents().readAllBytes(),
                    java.nio.charset.StandardCharsets.ISO_8859_1);
        }
    }
}
