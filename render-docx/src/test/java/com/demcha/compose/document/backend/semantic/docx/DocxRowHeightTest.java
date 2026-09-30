package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A row keeps the page's height in Word where what makes it that tall is not written in it.
 *
 * <p>{@code WorkspaceInvoice}'s bill-to heading stands beside a 20pt badge drawn where the page
 * puts it: its cell holds nothing in Word, and the row was only as tall as its 8pt label — the
 * address under it stood 10pt high. Its masthead title is set 4pt above its row by the cell's
 * padding, and a Word paragraph starts no higher than its cell: the title and the page under it
 * stood 4pt low.</p>
 */
class DocxRowHeightTest {

    private static final DocumentTextStyle LABEL = DocumentTextStyle.builder().fontName(FontName.LATO).size(8).build();

    @Test
    void aRowADrawingMakesTallIsHeldToThePagesHeight() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow("Head", row -> row
                        .columns(DocumentRowColumn.fixed(20), DocumentRowColumn.weight(1))
                        .addSection("Disc", disc -> disc.add(badge(20)))
                        .addParagraph(p -> p.text("BILL TO").textStyle(LABEL))))) {
            XWPFTableRow row = document.getTables().get(0).getRow(0);

            assertThat(row.getHeight()).as("the badge's 20pt, not the label's line").isBetween(380, 400);
        }
    }

    @Test
    void aRowWithMarginsIsHeldToItsOwnBoxWhoseMarginsAreWrittenAroundIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow("Head", row -> row
                        .margin(new DocumentInsets(10, 0, 10, 0))
                        .columns(DocumentRowColumn.fixed(20), DocumentRowColumn.weight(1))
                        .addSection("Disc", disc -> disc.add(badge(20)))
                        .addParagraph(p -> p.text("BILL TO").textStyle(LABEL))))) {
            XWPFTableRow row = document.getTables().get(0).getRow(0);

            assertThat(row.getHeight()).as("the badge's 20pt, its margins not taken off").isBetween(380, 400);
        }
    }

    @Test
    void aDrawingNoTallerThanItsNeighboursTextLeavesTheRowToWord() throws Exception {
        // A timeline's rail beside its entry: the entry's text makes the row, and Word sets it.
        DocumentTextStyle entry = DocumentTextStyle.builder().fontName(FontName.LATO).size(16).build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow("Entry", row -> row
                        .columns(DocumentRowColumn.fixed(20), DocumentRowColumn.weight(1))
                        .addSection("Rail", rail -> rail.add(badge(8)))
                        .addParagraph(p -> p.text("Senior Engineer").textStyle(entry))))) {
            XWPFTableRow row = document.getTables().get(0).getRow(0);

            assertThat(row.getHeight()).as("nothing held").isZero();
        }
    }

    private static com.demcha.compose.document.node.DocumentNode badge(double diameter) {
        return new ShapeContainerBuilder().name("Badge")
                .circle(diameter).fillColor(DocumentColor.rgb(80, 40, 160))
                .center(new com.demcha.compose.document.dsl.ImageBuilder().name("Glyph")
                        .source(png()).size(diameter / 2, diameter / 2).build())
                .build();
    }

    private static byte[] png() {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(10, 10,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB), "png", out);
            return out.toByteArray();
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    @Test
    void aRowItsTextMakesTallIsLeftToWord() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow("Head", row -> row
                        .columns(DocumentRowColumn.fixed(40), DocumentRowColumn.weight(1))
                        .addParagraph(p -> p.text("01").textStyle(LABEL))
                        .addParagraph(p -> p.text("BILL TO").textStyle(LABEL))))) {
            XWPFTableRow row = document.getTables().get(0).getRow(0);

            assertThat(row.getHeight()).as("nothing held").isZero();
        }
    }

    @Test
    void aTitlePulledAboveItsRowRisesInsideItsOwnLine() throws Exception {
        // The page's line for 36pt Lato is 43.2pt; pulled 4pt above its cell, it is written 4pt
        // shorter, starting where the cell does.
        DocumentTextStyle title = DocumentTextStyle.builder().fontName(FontName.LATO).size(36).build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow("Masthead", row -> row
                        .columns(DocumentRowColumn.weight(1), DocumentRowColumn.weight(1))
                        .addParagraph(p -> p.text("Logo").textStyle(LABEL))
                        .addSection("Title", cell -> cell
                                .padding(new DocumentInsets(-4, 0, 0, 0))
                                .addParagraph(p -> p.text("INVOICE").textStyle(title)))))) {
            XWPFParagraph invoice = document.getTables().get(0).getRow(0).getCell(1).getParagraphs().get(0);
            var spacing = invoice.getCTP().getPPr().getSpacing();
            double line = DocxTwips.of(spacing.getLine()) / 20.0;
            int position = invoice.getRuns().stream()
                    .map(run -> run.getCTR().getRPr())
                    .filter(properties -> properties != null && properties.sizeOfPositionArray() > 0)
                    .map(properties -> ((Number) properties.getPositionArray(0).getVal()).intValue())
                    .findFirst().orElse(0);

            assertThat(line).isCloseTo(43.2 - 4, within(0.1));
            assertThat(spacing.isSetBefore() ? DocxTwips.of(spacing.getBefore()) : 0L).as("no space above it").isZero();
            // Word's baseline four fifths down 39.2pt is 31.4pt; the page's is 4pt above the cell,
            // its ascent 35.5pt down: 31.5pt. Within a half point, as it is.
            assertThat(position).as("seated in half points").isBetween(-1, 1);
        }
    }

    @Test
    void aTitlePulledUpFurtherThanItsLettersAllowRisesOnlyAsFarAsTheyDo() throws Exception {
        // Pulled 10pt up, "RÉSUMÉ"'s accents stand under 2pt below its line's top: Word, drawing
        // the line's text only inside it, would cut them. The line gives up no more than that room.
        DocumentTextStyle title = DocumentTextStyle.builder().fontName(FontName.LATO).size(36).build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow("Masthead", row -> row
                        .columns(DocumentRowColumn.weight(1), DocumentRowColumn.weight(1))
                        .addParagraph(p -> p.text("Logo").textStyle(LABEL))
                        .addSection("Title", cell -> cell
                                .padding(new DocumentInsets(-10, 0, 0, 0))
                                .addParagraph(p -> p.text("RÉSUMÉ").textStyle(title)))))) {
            XWPFParagraph resume = document.getTables().get(0).getRow(0).getCell(1).getParagraphs().get(0);
            double line = DocxTwips.of(resume.getCTP().getPPr().getSpacing().getLine()) / 20.0;

            assertThat(line).as("shortened by less than the 10pt it is pulled").isGreaterThan(43.2 - 4);
            assertThat(line).as("but shortened").isLessThan(43.2);
        }
    }

    @Test
    void aDrawingsMarginCountsTowardsTheRowItMakes() throws Exception {
        // A 12pt badge 8pt down its cell makes the row 20pt, taller than the 16pt text's 19.2pt
        // line beside it: the row is sized by each child's margin box.
        DocumentTextStyle entry = DocumentTextStyle.builder().fontName(FontName.LATO).size(16).build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow("Head", row -> row
                        .columns(DocumentRowColumn.fixed(20), DocumentRowColumn.weight(1))
                        .addSection("Disc", disc -> disc.margin(new DocumentInsets(8, 0, 0, 0)).add(badge(12)))
                        .addParagraph(p -> p.text("Heading").textStyle(entry))))) {
            XWPFTableRow row = document.getTables().get(0).getRow(0);

            assertThat(row.getHeight()).as("held to the badge's 20pt box").isBetween(380, 400);
        }
    }

    @Test
    void aPulledTitlesOwnMarginIsWrittenAboveItAndNotTakenFromItsLine() throws Exception {
        // The section pulls up 4pt, the paragraph sets itself 2pt down: its own 2pt is written as
        // space above it, as for any paragraph, and the line gives up the section's 4pt.
        DocumentTextStyle title = DocumentTextStyle.builder().fontName(FontName.LATO).size(36).build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow("Masthead", row -> row
                        .columns(DocumentRowColumn.weight(1), DocumentRowColumn.weight(1))
                        .addParagraph(p -> p.text("Logo").textStyle(LABEL))
                        .addSection("Title", cell -> cell
                                .padding(new DocumentInsets(-4, 0, 0, 0))
                                .addParagraph(p -> p.text("INVOICE").textStyle(title)
                                        .margin(new DocumentInsets(2, 0, 0, 0))))))) {
            XWPFParagraph invoice = document.getTables().get(0).getRow(0).getCell(1).getParagraphs().get(0);
            var spacing = invoice.getCTP().getPPr().getSpacing();

            assertThat(DocxTwips.of(spacing.getLine()) / 20.0).isCloseTo(43.2 - 4, within(0.1));
            assertThat(DocxTwips.of(spacing.getBefore()) / 20.0).as("its own margin above it").isCloseTo(2, within(0.1));
        }
    }

    @Test
    void aSectionAfterABlockInItsCellWritesTheSpaceOwedBeforeItRises() throws Exception {
        // 6pt owed below the label, the section pulls up 4pt: the title stands 2pt below the
        // label's line, written as 2pt of space, its line whole.
        DocumentTextStyle title = DocumentTextStyle.builder().fontName(FontName.LATO).size(36).build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow("Masthead", row -> row
                        .columns(DocumentRowColumn.weight(1), DocumentRowColumn.weight(1))
                        .addParagraph(p -> p.text("Logo").textStyle(LABEL))
                        .addSection("Title", cell -> cell
                                .addParagraph(p -> p.text("ISSUED").textStyle(LABEL)
                                        .margin(new DocumentInsets(0, 0, 6, 0)))
                                .addSection("Pulled", pulled -> pulled
                                        .padding(new DocumentInsets(-4, 0, 0, 0))
                                        .addParagraph(p -> p.text("INVOICE").textStyle(title))))))) {
            java.util.List<XWPFParagraph> paragraphs = document.getTables().get(0).getRow(0).getCell(1).getParagraphs();
            XWPFParagraph invoice = paragraphs.stream()
                    .filter(paragraph -> paragraph.getText().contains("INVOICE")).findFirst().orElseThrow();
            var spacing = invoice.getCTP().getPPr().getSpacing();

            assertThat(DocxTwips.of(spacing.getLine()) / 20.0).as("nothing to rise into").isCloseTo(43.2, within(0.1));
            assertThat(DocxTwips.of(spacing.getBefore()) / 20.0).as("6pt owed less the 4pt pulled").isCloseTo(2, within(0.1));
        }
    }

    @Test
    void aPulledLineHoldingAPictureIsLeftAsItWas() throws Exception {
        // Its letters cannot be read past the picture, so how far it may rise is not known.
        DocumentTextStyle title = DocumentTextStyle.builder().fontName(FontName.LATO).size(36).build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow("Masthead", row -> row
                        .columns(DocumentRowColumn.weight(1), DocumentRowColumn.weight(1))
                        .addParagraph(p -> p.text("Logo").textStyle(LABEL))
                        .addSection("Title", cell -> cell
                                .padding(new DocumentInsets(-4, 0, 0, 0))
                                .addParagraph(p -> p.inlineText("INVOICE ", title)
                                        .inlineImage(com.demcha.compose.document.image.DocumentImageData.fromBytes(png()),
                                                10, 10)))))) {
            XWPFParagraph invoice = document.getTables().get(0).getRow(0).getCell(1).getParagraphs().get(0);
            double line = DocxTwips.of(invoice.getCTP().getPPr().getSpacing().getLine()) / 20.0;

            assertThat(line).as("not cut short").isGreaterThan(43.2 - 1);
        }
    }
}
