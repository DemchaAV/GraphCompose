package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.document.svg.SvgIcon;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a picture among a paragraph's words loses of its place, its width or its paint is named.
 *
 * <p>Word stands an inline picture on its line's baseline and moves it by {@code w:position} to
 * where the page's alignment sets it, from the layout's measure of the line. Where the lines are
 * not read nothing moves it; all of them are moved by the first line's measure; one Word paragraph
 * takes one seat by its cap band; a chip's shading covers its letters' box whatever its padding; a
 * shape's picture is wider than its box where no text beside it gives that back; an icon's
 * gradient is drawn flat. None of these is on the corpus's 62 documents; each is named from half a
 * point.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxInlinePictureLossTest {

    private static final SvgIcon ICON = SvgIcon.parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'>"
            + "<rect width='24' height='24' fill='#1A5694'/></svg>");

    private static final SvgIcon GRADIENT = SvgIcon.parse("""
            <svg viewBox="0 0 10 10">
              <linearGradient id="g">
                <stop offset="0" stop-color="#ff0000"/>
                <stop offset="1" stop-color="#0000ff"/>
              </linearGradient>
              <rect width="10" height="10" fill="url(#g)"/>
            </svg>""");

    private static final DocumentTextStyle SMALL = DocumentTextStyle.builder().size(8).build();
    private static final DocumentTextStyle LARGE = DocumentTextStyle.builder().size(24).build();

    /** A stroke that reaches 1.5pt past a dot's box on each side. */
    private static final DocumentStroke STROKE = DocumentStroke.of(DocumentColor.BLACK, 3);

    @Test
    void aPictureBesideTextOnALineNotReadIsNamed() throws Exception {
        DocxExportReport report = DocxExports.reportWithoutLayout(400, 300, 20,
                page -> page.addParagraph(p -> p.inlineText("Call ").inlineSvgIcon(ICON, 12)));

        assertThat(details(report, "ParagraphNode")).anySatisfy(detail -> assertThat(detail)
                .contains("its pictures stand on the line's baseline, where the page sets them by their alignment: "
                          + "its lines are not read here"));
    }

    @Test
    void aLineOfPicturesAloneIsNotNamed() throws Exception {
        // Set at their size, in Word as on the page: there is no text for them to stand off.
        DocxExportReport report = DocxExports.reportWithoutLayout(400, 300, 20,
                page -> page.addParagraph(p -> p.inlineSvgIcon(ICON, 12)));

        assertThat(details(report, "ParagraphNode")).noneMatch(detail -> detail.contains("baseline"));
    }

    @Test
    void aPictureOnTheBaselineIsNotNamed() throws Exception {
        DocxExportReport report = DocxExports.reportWithoutLayout(400, 300, 20,
                page -> page.addParagraph(p -> p.inlineText("Sign ")
                        .inlineImage(picture(), 12, 12, InlineImageAlignment.BASELINE)));

        assertThat(details(report, "ParagraphNode")).noneMatch(detail -> detail.contains("baseline"));
    }

    @Test
    void aPictureOffTheBaselineByLessThanHalfAPointIsNotNamed() throws Exception {
        DocxExportReport report = DocxExports.reportWithoutLayout(400, 300, 20,
                page -> page.addParagraph(p -> p.inlineText("Sign ")
                        .inlineImage(picture(), 12, 12, InlineImageAlignment.BASELINE, 0.2, null)));

        assertThat(details(report, "ParagraphNode")).noneMatch(detail -> detail.contains("baseline"));
    }

    @Test
    void aShapeOnTheBaselineReachingBelowItsBoxIsNamed() throws Exception {
        // The page stands its box on the baseline, its stroke 1pt and a pixel below it; Word stands
        // its picture's foot there, the whole shape that much higher.
        DocxExportReport report = DocxExports.reportWithoutLayout(400, 300, 20,
                page -> page.addParagraph(p -> p.inlineText("Sign ").shape(ShapeOutline.circle(10),
                        DocumentColor.BLACK, DocumentStroke.of(DocumentColor.BLACK, 2), InlineImageAlignment.BASELINE,
                        0, null)));

        assertThat(details(report, "ParagraphNode")).anySatisfy(detail -> assertThat(detail)
                .contains("its pictures stand on the line's baseline, up to 1.25pt from where the page sets them"));
    }

    @Test
    void aCentredIconBesideTextWhoseLinesAreReadIsNotNamedForTheBaseline() throws Exception {
        DocxExportReport report = report(page -> page.addParagraph(p -> p.inlineText("Call ")
                .inlineSvgIcon(ICON, 12, InlineImageAlignment.CENTER)));

        assertThat(details(report, "ParagraphNode")).noneMatch(detail -> detail.contains("baseline"));
    }

    @Test
    void aPictureOnALaterLineOfAnotherHeightIsNamed() throws Exception {
        // Its line is as tall as the picture; the first, of text alone, is not: centred by the first
        // line's measure, it stands apart from where the page centres it on its own.
        DocxExportReport report = report(page -> page.addParagraph(p -> p
                .inlineText("Words that run on past the width of this narrow page and onto the next line ")
                .inlineImage(picture(), 20, 20, InlineImageAlignment.CENTER)));

        assertThat(details(report, "ParagraphNode")).anySatisfy(detail -> assertThat(detail)
                .contains("a picture on a line after its first is set by the first line's measure, up to"));
    }

    @Test
    void aPictureOnTheFirstLineIsNotNamed() throws Exception {
        DocxExportReport report = report(page -> page.addParagraph(p -> p
                .inlineImage(picture(), 20, 20, InlineImageAlignment.CENTER)
                .inlineText(" Words that run on past the width of this narrow page and onto the next line")));

        assertThat(details(report, "ParagraphNode")).noneMatch(detail -> detail.contains("after its first"));
    }

    @Test
    void linesInTwoSizesSeatedByTheirCapBandAreNamed() throws Exception {
        // The first line opens with the large word, the second with the small text it runs on in.
        DocxExportReport report = report(page -> page.addParagraph(p -> p.verticalAlign(TextVerticalAlign.CENTER)
                .inlineText("LARGE ", LARGE)
                .inlineText("small words that run on past the width of this narrow page and onto the next", SMALL)));

        assertThat(details(report, "ParagraphNode")).anySatisfy(detail -> assertThat(detail)
                .contains("its lines are seated as its first is, a line in another size or face up to"));
    }

    @Test
    void linesInOneSizeSeatedByTheirCapBandAreNotNamed() throws Exception {
        DocxExportReport report = report(page -> page.addParagraph(p -> p.verticalAlign(TextVerticalAlign.CENTER)
                .inlineText("words that run on past the width of this narrow page and onto the next", SMALL)));

        assertThat(details(report, "ParagraphNode")).noneMatch(detail -> detail.contains("seated as its first"));
    }

    @Test
    void aLineOfAPictureAloneDoesNotTakeTheSeat() throws Exception {
        // The paragraph is seated by its first line of text, and those are in one size.
        DocxExportReport report = report(page -> page.addParagraph(p -> p.verticalAlign(TextVerticalAlign.CENTER)
                .inlineImage(picture(), 150, 30, InlineImageAlignment.BASELINE)
                .inlineText(" words that run on past the width of this narrow page and onto the next", SMALL)));

        assertThat(details(report, "ParagraphNode")).noneMatch(detail -> detail.contains("seated as its first"));
    }

    @Test
    void aChipPaddedBelowZeroIsNamed() throws Exception {
        DocxExportReport report = report(page -> page.addParagraph(p -> p.inlineText("Status ")
                .inlineHighlight("Paid", SMALL, DocumentColor.LIGHT_GRAY, 0, new DocumentInsets(3, -2, -3, -2))
                .inlineText(" today")));

        assertThat(details(report, "inline chip")).singleElement().asString()
                .contains("its left padding, below zero, is not in the file")
                .contains("its right padding, below zero, is not in the file")
                .contains("its padding above and below its letters is not in the file");
    }

    @Test
    void aShapeOpeningItsLineNamesTheWidthNoTextGivesBack() throws Exception {
        // Its stroke reaches 1.5pt past its box on each side, and its picture a pixel further: the
        // room on its left moves it and its text; the text after it gives back its right.
        DocxExportReport report = report(page -> page.addParagraph(p -> p.dot(10, DocumentColor.BLACK, STROKE)
                .inlineText(" Item")));

        assertThat(details(report, "inline shape")).singleElement().asString()
                .contains("wider than its box where no text beside it gives that back, and moves it and what stands "
                          + "beside it in its line up to 1.75pt from where the page sets them");
    }

    @Test
    void aShapeBetweenWordsIsNotNamedForItsWidth() throws Exception {
        // The text just before it gives back its left, the text after it its right.
        DocxExportReport report = report(page -> page.addParagraph(p -> p.inlineText("One ")
                .dot(10, DocumentColor.BLACK, STROKE).inlineText(" two")));

        assertThat(details(report, "inline shape")).singleElement().asString().doesNotContain("wider than its box");
    }

    @Test
    void aShapeEndingItsParagraphIsNotNamedForItsWidth() throws Exception {
        // Nothing stands right of it for the room on its right to move.
        DocxExportReport report = report(page -> page.addParagraph(p -> p.inlineText("One ")
                .dot(10, DocumentColor.BLACK, STROKE)));

        assertThat(details(report, "inline shape")).singleElement().asString().doesNotContain("wider than its box");
    }

    @Test
    void aShapeBeforePicturesAloneIsNamedForItsRight() throws Exception {
        DocxExportReport report = report(page -> page.addParagraph(p -> p.inlineText("One ")
                .dot(10, DocumentColor.BLACK, STROKE).inlineSvgIcon(ICON, 12)));

        assertThat(details(report, "inline shape")).singleElement().asString().contains("up to 1.75pt");
    }

    @Test
    void aShapeAfterALineBreakIsNamedForItsLeft() throws Exception {
        // Word sets no run holding a line break closer: the shape opens Word's line.
        DocxExportReport report = report(page -> page.addParagraph(p -> p.inlineText("Java\n")
                .dot(10, DocumentColor.BLACK, STROKE).inlineText(" Python")));

        assertThat(details(report, "inline shape")).singleElement().asString().contains("up to 1.75pt");
    }

    @Test
    void aShapeBeforeARunHoldingALineBreakIsNamedForItsRight() throws Exception {
        DocxExportReport report = report(page -> page.addParagraph(p -> p.inlineText("One ")
                .dot(10, DocumentColor.BLACK, STROKE).inlineText(" two\nthree")));

        assertThat(details(report, "inline shape")).singleElement().asString().contains("up to 1.75pt");
    }

    @Test
    void aShapeOpeningARightAlignedLineIsNotNamedForItsWidth() throws Exception {
        // Set from the right, the room on its left moves nothing; its text gives back its right.
        DocxExportReport report = report(page -> page.addParagraph(p -> p.align(TextAlign.RIGHT)
                .dot(10, DocumentColor.BLACK, STROKE).inlineText(" Online")));

        assertThat(details(report, "inline shape")).singleElement().asString().doesNotContain("wider than its box");
    }

    @Test
    void aShapeEndingARightAlignedLineIsNamedForItsRight() throws Exception {
        // Set from the right, the room on its right moves it and the text before it.
        DocxExportReport report = report(page -> page.addParagraph(p -> p.align(TextAlign.RIGHT)
                .inlineText("Online ").dot(10, DocumentColor.BLACK, STROKE)));

        assertThat(details(report, "inline shape")).singleElement().asString().contains("up to 1.75pt");
    }

    @Test
    void aShapeAloneInACentredLineIsNotNamedForItsWidth() throws Exception {
        // Its room on either side is alike, and centred, it stands where the page centres it.
        DocxExportReport report = report(page -> page.addParagraph(p -> p.align(TextAlign.CENTER)
                .dot(10, DocumentColor.BLACK, STROKE)));

        assertThat(details(report, "inline shape")).singleElement().asString().doesNotContain("wider than its box");
    }

    @Test
    void aShapeInALineSetRightToLeftIsNamedForItsWidth() throws Exception {
        // Text set right to left gives back nothing, and the line is set from the right: the room
        // on the shape's right moves it, and both sides what stands left of it, the text after it.
        DocxExportReport report = report(page -> page.addParagraph(p -> p.direction(TextDirection.RTL)
                .inlineText("שלום ").dot(10, DocumentColor.BLACK, STROKE).inlineText(" עולם")));

        assertThat(details(report, "inline shape")).singleElement().asString().contains("up to 3.5pt");
    }

    @Test
    void aShapeEndingALineSetRightToLeftIsNamedForItsRightAlone() throws Exception {
        // Last in reading order, it stands at the line's left, and the line is set from the right:
        // the room on its right moves it, and nothing stands left of it for the rest to move.
        DocxExportReport report = report(page -> page.addParagraph(p -> p.direction(TextDirection.RTL)
                .inlineText("שלום ").dot(10, DocumentColor.BLACK, STROKE)));

        assertThat(details(report, "inline shape")).singleElement().asString().contains("up to 1.75pt");
    }

    @Test
    void aShapeInAListItemIsNamedForItsWidth() throws Exception {
        // An item's text gives back none of it, before the shape or after it.
        DocxExportReport report = report(page -> page.addList(list -> list
                .addItem(item -> item.plain("One ").dot(10, DocumentColor.BLACK, STROKE).plain(" two"))));

        assertThat(details(report, "inline shape")).singleElement().asString().contains("up to 3.5pt");
    }

    @Test
    void anIconsGradientIsNamed() throws Exception {
        DocxExportReport report = report(page -> page.addParagraph(p -> p.inlineText("Rate ").inlineSvgIcon(GRADIENT, 12)));

        assertThat(details(report, "inline icon")).singleElement().asString()
                .endsWith("; its gradients are drawn flat, in their first colour");
    }

    @Test
    void anIconsGradientStrokeIsNamed() throws Exception {
        SvgIcon stroked = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <linearGradient id="g">
                    <stop offset="0" stop-color="#ff0000"/>
                    <stop offset="1" stop-color="#0000ff"/>
                  </linearGradient>
                  <rect x="1" y="1" width="8" height="8" fill="none" stroke="url(#g)" stroke-width="2"/>
                </svg>""");
        DocxExportReport report = report(page -> page.addParagraph(p -> p.inlineText("Rate ").inlineSvgIcon(stroked, 12)));

        assertThat(details(report, "inline icon")).singleElement().asString()
                .endsWith("; its gradients are drawn flat, in their first colour");
    }

    @Test
    void anIconInOneColourIsNotNamedForAGradient() throws Exception {
        DocxExportReport report = report(page -> page.addParagraph(p -> p.inlineText("Rate ").inlineSvgIcon(ICON, 12)));

        assertThat(details(report, "inline icon")).singleElement().asString().doesNotContain("gradient");
    }

    @Test
    void aMarkerWithAPercentSignBeforeADigitIsWrittenAsText() throws Exception {
        // Read in a level's text as the level's number, "%1 " showed nothing in Word (measured).
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 20,
                page -> page.addList(list -> list.marker("%1 ").items("Alpha", "Beta")))) {
            XWPFParagraph alpha = document.getParagraphs().stream()
                    .filter(p -> p.getText().contains("Alpha")).findFirst().orElseThrow();

            assertThat(alpha.getCTP().getPPr() != null && alpha.getCTP().getPPr().isSetNumPr())
                    .as("no Word numbering").isFalse();
            assertThat(alpha.getText()).as("its marker as characters").startsWith("%1");
        }
    }

    @Test
    void aNestedLevelsMarkerWithAPercentSignBeforeADigitIsWrittenAsText() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 20,
                page -> page.addList(list -> list.marker("- ")
                        .markerFor(1, ListMarker.custom("%2 "))
                        .addItem("Alpha", nested -> nested.items("Beta"))))) {
            assertThat(document.getParagraphs())
                    .filteredOn(p -> p.getText().contains("Alpha") || p.getText().contains("Beta"))
                    .hasSize(2)
                    .allSatisfy(p -> assertThat(p.getCTP().getPPr() != null && p.getCTP().getPPr().isSetNumPr())
                            .as("no Word numbering").isFalse());
        }
    }

    @Test
    void aMarkerWithAPercentSignAloneStaysAWordList() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 20,
                page -> page.addList(list -> list.marker("50% ").items("Alpha", "Beta")))) {
            XWPFParagraph alpha = document.getParagraphs().stream()
                    .filter(p -> p.getText().contains("Alpha")).findFirst().orElseThrow();

            assertThat(alpha.getCTP().getPPr().isSetNumPr()).as("Word numbering").isTrue();
        }
    }

    private static DocxExportReport report(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.report(200, 300, 20, content);
    }

    private static List<String> details(DocxExportReport report, String subject) {
        return report.notes().stream().filter(note -> note.subject().equals(subject))
                .map(DocxExportReport.Note::detail).toList();
    }

    private static DocumentImageData picture() {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try {
            javax.imageio.ImageIO.write(image, "png", png);
        } catch (java.io.IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
        return DocumentImageData.fromBytes(png.toByteArray());
    }
}
