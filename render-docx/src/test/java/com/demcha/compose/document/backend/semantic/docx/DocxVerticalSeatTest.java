package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Text the page seats off its baseline sits there in Word, and text running past the foot of
 * the band holding it hangs below it.
 *
 * <p>{@code LumaStudioInvoice}'s title sets "INVOICE" against the top of a line far taller than
 * its capitals ({@link TextVerticalAlign#TOP}), in a title block 13pt shorter than that line. In
 * Word the word stood on the line's foot, 20pt low, with its rule through the letters, and the
 * line's overhang pushed the invoice's details and everything under them lower, onto a second
 * page.</p>
 */
class DocxVerticalSeatTest {

    /** A face whose capitals stand below its ascent, as {@code LumaStudioInvoice}'s title's do. */
    private static final DocumentTextStyle DISPLAY = DocumentTextStyle.builder()
            .fontName(com.demcha.compose.font.FontName.SPECTRAL).size(30).color(DocumentColor.BLACK).build();

    @Test
    void textSeatedOffItsBaselineIsMovedInItsLine() throws Exception {
        int top = position(TextVerticalAlign.TOP);
        int bottom = position(TextVerticalAlign.BOTTOM);

        assertThat(top).as("raised to the line's top").isPositive();
        assertThat(bottom).as("lowered to the line's foot").isNegative();
        assertThat(position(TextVerticalAlign.CENTER)).as("halfway between the two, the page's own midpoint")
                .isCloseTo((top + bottom) / 2, org.assertj.core.data.Offset.offset(1));
        assertThat(position(TextVerticalAlign.DEFAULT)).as("on its baseline, as written before").isZero();
    }

    @Test
    void aBandInsideABandHangsOnlyOnce() throws Exception {
        // A title block whose line hangs below it, laid in a taller stack: the outer stack measures
        // its space below from that same line, so what the inner one left hanging is not taken
        // from the gap a second time — the gap is what it is with the title block tall enough.
        assertThat(beforeTheStack(20)).as("the inner block too short for its line")
                .isCloseTo(beforeTheStack(60), org.assertj.core.data.Offset.offset(2L));
    }

    @Test
    void aLineRunningPastItsBandsFootHangsBelowIt() throws Exception {
        // A title set at the top of its block, a rule drawn at the block's foot: the gap under
        // the title is what the block leaves below its line, and its margin. A block shorter
        // than the line leaves less than nothing, which the margin gives up.
        long hanging = beforeTheDetails(20, 40);
        long clear = beforeTheDetails(60, 40);

        assertThat(hanging).as("a block 40pt shorter, a gap 40pt shorter — the line's overhang taken from it")
                .isCloseTo(clear - 40L * 20, org.assertj.core.data.Offset.offset(2L));
        assertThat(hanging).as("and less than the margin alone").isLessThan(40L * 20);
    }

    @Test
    void anOverhangDeeperThanTheBandsMarginLeavesNoGapAtAll() throws Exception {
        // The line hangs further below the block than its 5pt margin reaches: the paragraph after
        // it starts at once, the rest of the overhang taken from what follows.
        assertThat(beforeTheDetails(20, 5)).isZero();
    }

    @Test
    void onlyTheSeatedHalfOfALineMovesInIt() throws Exception {
        // A label and its value set as one line: the value is seated at its line's top, the
        // label on its baseline, and the label stays there.
        DocumentNode row = new ShapeContainerBuilder().name("Row")
                .rectangle(300, 40).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(new ParagraphBuilder().name("Label").text("Label").build(), 0, 0, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder().name("Value").text("Value").textStyle(DISPLAY)
                        .verticalAlign(TextVerticalAlign.TOP).build(), 0, 0, LayerAlign.CENTER_RIGHT)
                .build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 40, page -> page.add(row))) {
            XWPFParagraph line = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().contains("Value")).findFirst().orElseThrow();

            assertThat(line.getText()).as("one line").contains("Label");
            var label = line.getRuns().stream().filter(run -> run.text().contains("Label")).findFirst().orElseThrow();
            var value = line.getRuns().stream().filter(run -> run.text().contains("Value")).findFirst().orElseThrow();
            assertThat(positionOf(label.getCTR())).as("the label on its baseline").isZero();
            assertThat(positionOf(value.getCTR())).as("the value raised").isPositive();
        }
    }

    /** The space written above the paragraph after a stack holding a title block of the given height. */
    private static long beforeTheStack(double titleBlockHeight) throws Exception {
        DocumentNode titleBlock = new ShapeContainerBuilder().name("TitleBlock")
                .rectangle(200, titleBlockHeight).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(new ParagraphBuilder().name("Title").text("INVOICE").textStyle(DISPLAY).build(),
                        0, 0, LayerAlign.TOP_LEFT)
                .position(new LineBuilder().name("TitleRule").horizontal(40).thickness(1)
                        .color(DocumentColor.BLACK).build(), 0, 0, LayerAlign.BOTTOM_LEFT)
                .build();
        DocumentNode stack = new com.demcha.compose.document.dsl.LayerStackBuilder().name("Header")
                .layer(new com.demcha.compose.document.dsl.ShapeBuilder().name("Backdrop").size(300, 90)
                        .fillColor(DocumentColor.rgb(230, 230, 230)).build(), LayerAlign.TOP_LEFT)
                .layer(titleBlock, LayerAlign.TOP_LEFT)
                .margin(new DocumentInsets(0, 0, 8, 0))
                .build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 40, page -> page
                .add(stack).addParagraph("INVOICE NO."))) {
            XWPFParagraph details = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().equals("INVOICE NO.")).findFirst().orElseThrow();
            var properties = details.getCTP().getPPr();
            return properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetBefore()
                    ? 0 : DocxTwips.of(properties.getSpacing().getBefore());
        }
    }

    @Test
    void aSeatedLinksTextMovesWithIt() throws Exception {
        // An internal link's runs are not among a paragraph's runs in POI; they are its text too.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 40, page -> page
                .addParagraph(p -> p.text("Target").anchor("target"))
                .addParagraph(p -> p.text("INVOICE").textStyle(DISPLAY).verticalAlign(TextVerticalAlign.TOP)
                        .linkTo("target")))) {
            var link = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getCTP().sizeOfHyperlinkArray() > 0).findFirst().orElseThrow()
                    .getCTP().getHyperlinkArray(0);

            assertThat(link.sizeOfRArray()).isPositive();
            assertThat(positionOf(link.getRArray(0))).as("the linked text raised").isPositive();
        }
    }

    private static int positionOf(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR run) {
        var properties = run.getRPr();
        return properties == null || properties.sizeOfPositionArray() == 0 ? 0
                : ((Number) properties.getPositionArray(0).getVal()).intValue();
    }

    /** The run position a 30pt word seated as given is written with, in half-points. */
    private static int position(TextVerticalAlign seat) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 40, page -> page
                .addParagraph(p -> p.text("INVOICE").textStyle(DISPLAY).lineSpacing(20).verticalAlign(seat)))) {
            XWPFParagraph title = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().equals("INVOICE")).findFirst().orElseThrow();
            var properties = title.getRuns().get(0).getCTR().getRPr();
            return properties == null || properties.sizeOfPositionArray() == 0 ? 0
                    : ((Number) properties.getPositionArray(0).getVal()).intValue();
        }
    }

    /** The space written above the paragraph after a title block of the given height and margin. */
    private static long beforeTheDetails(double blockHeight, double margin) throws Exception {
        DocumentNode block = new ShapeContainerBuilder().name("TitleBlock")
                .rectangle(200, blockHeight).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(new ParagraphBuilder().name("Title").text("INVOICE").textStyle(DISPLAY).build(),
                        0, 0, LayerAlign.TOP_LEFT)
                .position(new LineBuilder().name("TitleRule").horizontal(40).thickness(1)
                        .color(DocumentColor.BLACK).build(), 0, 0, LayerAlign.BOTTOM_LEFT)
                .margin(new DocumentInsets(0, 0, margin, 0))
                .build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 40, page -> page
                .add(block).addParagraph("INVOICE NO."))) {
            XWPFParagraph details = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().equals("INVOICE NO.")).findFirst().orElseThrow();
            var properties = details.getCTP().getPPr();
            return properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetBefore()
                    ? 0 : DocxTwips.of(properties.getSpacing().getBefore());
        }
    }
}
