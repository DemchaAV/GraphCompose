package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Text stands on the page's baseline in Word's exact lines.
 *
 * <p>Word stands the baseline of an exact line four fifths of the way down it, whatever the
 * face; the page sets it the face's ascent below the line's top. {@code NorthlineProposal}'s
 * 46pt Spectral title, whose ascent is under seven tenths of its line, stood 7pt low in Word.</p>
 */
class DocxBaselineSeatTest {

    /** Spectral's ascent, in ems: 1059 of 1000 units. */
    private static final double SPECTRAL_ASCENT = 1.059;
    /** Spectral's own line, in ems: its ascent and 463 units of descent. */
    private static final double SPECTRAL_LINE = 1.522;

    private static DocumentTextStyle spectral(double size) {
        return DocumentTextStyle.builder().fontName(FontName.SPECTRAL).size(size).build();
    }

    @Test
    void aDeepFaceIsRaisedFromWordsBaselineToThePages() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Title").textStyle(spectral(30))))) {
            XWPFParagraph title = paragraph(document, "Title");
            double wordBelowThePage = 0.8 * line(title) - SPECTRAL_ASCENT * 30;

            assertThat(wordBelowThePage).as("the premise: Word's baseline is the lower").isGreaterThan(4);
            assertThat(position(title)).isCloseTo((int) Math.round(wordBelowThePage * 2), within(1));
        }
    }

    @Test
    void aFaceWhoseBaselineWordAlreadyNearlyMatchesIsLeftAlone() throws Exception {
        // Lato's ascent is 0.82 of its line: a quarter point from Word's four fifths at 10pt.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Body").textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.LATO).size(10).build())))) {
            assertThat(position(paragraph(document, "Body"))).isZero();
        }
    }

    @Test
    void aPictureOnTheBaselineMovesWithItsText() throws Exception {
        // An 8pt picture standing on the baseline of a Spectral line: Word stands both on its own
        // baseline, so the picture takes the text's move and nothing else.
        byte[] png = png();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.inlineText("Title ", spectral(30))
                        .inlineImage(com.demcha.compose.document.image.DocumentImageData.fromBytes(png), 8, 8,
                                com.demcha.compose.document.node.InlineImageAlignment.BASELINE, 0, null)))) {
            XWPFParagraph title = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().startsWith("Title")).findFirst().orElseThrow();
            int text = positionOf(title.getRuns().get(0).getCTR());

            assertThat(text).as("the premise: the line moved").isPositive();
            assertThat(positionOf(title.getRuns().get(1).getCTR())).isEqualTo(text);
        }
    }

    private static byte[] png() {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(8, 8,
                    java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    @Test
    void theHalfOfALinePairSetLowerIsSeatedFromTheLinesTop() throws Exception {
        // A 12pt label centred beside a 30pt value: the line is the value's, and the label's own
        // line starts half the difference below its top.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .add(new ShapeContainerBuilder().name("Row").rectangle(360, 60)
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .position(new ParagraphBuilder().name("Label").text("Label").textStyle(spectral(12)).build(),
                                0, 0, LayerAlign.CENTER_LEFT)
                        .position(new ParagraphBuilder().name("Value").text("Value").textStyle(spectral(30)).build(),
                                0, 0, LayerAlign.CENTER_RIGHT)
                        .build()))) {
            XWPFParagraph pair = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().contains("Value")).findFirst().orElseThrow();
            double line = line(pair);
            double labelLine = line * 12 / 30;
            double labelBelow = (line - labelLine) / 2;
            CTR label = pair.getRuns().stream().filter(run -> run.text().contains("Label")).findFirst()
                    .orElseThrow().getCTR();

            assertThat(pair.getText()).as("one line").contains("Label");
            assertThat(positionOf(label))
                    .isCloseTo((int) Math.round((0.8 * line - labelBelow - SPECTRAL_ASCENT * 12) * 2), within(1));
        }
    }

    @Test
    void linesThatTookTheirGapFromTheSpaceAboveAreSeatedFromTheHigherTop() throws Exception {
        // Two lines 10pt apart under 20pt of space: Word's lines take their gaps from above and
        // start 10pt above the page's first line.
        try (XWPFDocument document = DocxExports.withLayout(240, 600, 20, page -> page
                .addParagraph(p -> p.text("First line of it and second line of it").textStyle(spectral(20))
                        .lineSpacing(10).margin(new DocumentInsets(20, 0, 0, 0))))) {
            XWPFParagraph text = paragraph(document, "First line of it and second line of it");
            double line = line(text);

            assertThat(line).as("the premise: two lines, each its gap taller").isGreaterThan(40);
            assertThat(position(text)).isCloseTo((int) Math.round((0.8 * line - 10 - SPECTRAL_ASCENT * 20) * 2),
                    within(1));
        }
    }

    @Test
    void linesWhoseGapIsSharedOutAreSeatedAtTheMiddleOne() throws Exception {
        // Two lines 10pt apart with no space above to give: Word's lines are each 5pt taller
        // than the face, stepping 5pt closer than the page's. Seated at the first line, the
        // second would stand 5pt high; at the middle, each is 2.5pt off.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("First\nSecond").textStyle(spectral(20)).lineSpacing(10)))) {
            XWPFParagraph text = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().contains("First")).findFirst().orElseThrow();
            double face = SPECTRAL_LINE * 20;
            double line = line(text);
            double middle = 0.5;

            assertThat(line).as("the premise: the one gap shared by two lines").isCloseTo(face + 5, within(0.05));
            assertThat(position(text)).isCloseTo((int) Math.round(
                    (middle * line + 0.8 * line - middle * (face + 10) - SPECTRAL_ASCENT * 20) * 2), within(1));
        }
    }

    private static XWPFParagraph paragraph(XWPFDocument document, String text) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getText().equals(text)).findFirst().orElseThrow();
    }

    private static double line(XWPFParagraph paragraph) {
        return DocxTwips.of(paragraph.getCTP().getPPr().getSpacing().getLine()) / 20.0;
    }

    private static int position(XWPFParagraph paragraph) {
        return positionOf(paragraph.getRuns().get(0).getCTR());
    }

    private static int positionOf(CTR run) {
        var properties = run.getRPr();
        return properties == null || properties.sizeOfPositionArray() == 0 ? 0
                : ((Number) properties.getPositionArray(0).getVal()).intValue();
    }
}
