package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A cell built from a node carries that node, whatever it is.
 *
 * <p>{@code DocumentTableCell.node(...)} lets a cell hold anything the document can hold,
 * and the export wrote paragraphs out of it and nothing else: a cell built from an image or
 * a list came out empty — not wrong, <em>empty</em>, with the content the page draws simply
 * missing from the file and one line in a log to say so. Silent loss inside a table is the
 * worst place for it, because a table is where a document keeps the things a reader counts.</p>
 *
 * <p>The fix is not a second writer that knows about cells. The cell became a destination,
 * so the writers that handle a node anywhere handle it here too — which is why a nested
 * table works without anything in this class knowing how a table is written.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxComposedCellTest {

    @Test
    void aCellBuiltFromAnImageCarriesThePicture() throws Exception {
        XWPFTableCell cell = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Logo"),
                        DocumentTableCell.node(image()))));

        assertThat(cell.getParagraphs())
                .as("the picture is in the cell, not dropped with a warning")
                .anyMatch(p -> !p.getRuns().isEmpty() && !p.getRuns().get(0).getEmbeddedPictures().isEmpty());
    }

    @Test
    void aCellBuiltFromAListCarriesEveryItem() throws Exception {
        XWPFTableCell cell = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Included"),
                        DocumentTableCell.node(list("Setup", "Training", "Support")))));

        assertThat(cell.getParagraphs())
                .extracting(p -> p.getText())
                .contains("Setup", "Training", "Support");
        assertThat(cell.getParagraphs())
                .as("and they are list items, so Enter continues the list in the cell too")
                .anyMatch(p -> p.getCTP().getPPr() != null && p.getCTP().getPPr().isSetNumPr());
    }

    @Test
    void aCellBuiltFromATableCarriesARealNestedTable() throws Exception {
        XWPFTableCell cell = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Breakdown"),
                        DocumentTableCell.node(innerTable()))));

        assertThat(cell.getTables()).hasSize(1);
        assertThat(cell.getTables().get(0).getRow(0).getCell(0).getText()).isEqualTo("Hours");
        assertThat(cell.getTables().get(0).getRow(0).getCell(1).getText()).isEqualTo("12");
    }

    @Test
    void aNestedTableIsFollowedByAParagraph() throws Exception {
        // Word requires a cell to end with a paragraph. A cell ending in a table is
        // malformed, and Word refuses the file rather than showing the table — so the
        // guard is on the structure, not on the render.
        XWPFTableCell cell = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Breakdown"),
                        DocumentTableCell.node(innerTable()))));

        String xml = cell.getCTTc().xmlText();
        assertThat(xml.lastIndexOf("<w:p>"))
                .as("the last block in the cell is a paragraph, not the table")
                .isGreaterThan(xml.lastIndexOf("<w:tbl>"));
    }

    @Test
    void aNestedTableIsGivenTheWidthOfTheColumnItSitsIn() throws Exception {
        // Not the width the page gives it — the layout reports a composed cell's content
        // under the owner's path, so which measured row belongs to which nested table
        // cannot be told apart there. But a nested table with no width at all is squeezed
        // by Word to about one character a line, which is not a document anybody can read.
        XWPFTableCell cell = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Breakdown"),
                        DocumentTableCell.node(innerTable()))));

        long column = DocxTwips.of(cell.getTableRow().getTable()
                .getCTTbl().getTblGrid().getGridColArray(1).getW());
        long nested = DocxTwips.of(cell.getTables().get(0)
                .getCTTbl().getTblPr().getTblW().getW());

        assertThat(cell.getTables().get(0).getCTTbl().getTblPr().getTblW().getType().toString())
                .as("stated, not left to Word")
                .isEqualTo("dxa");
        // The column less the margins the cell itself states: the engine's own default
        // cell padding, 4pt a side, which this export writes as w:tcMar.
        assertThat(nested).isEqualTo(column - 2 * 80);
    }

    @Test
    void aWrapperInsideTheCellIsStillTransparent() throws Exception {
        // A section around the content is not content: its children are written where it
        // stood, here as much as anywhere else.
        XWPFTableCell cell = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Notes"),
                        DocumentTableCell.node(new com.demcha.compose.document.dsl.SectionBuilder()
                                .name("Wrapper")
                                .addParagraph(p -> p.text("First"))
                                .addParagraph(p -> p.text("Second"))
                                .build()))));

        assertThat(cell.getParagraphs())
                .extracting(p -> p.getText())
                .contains("First", "Second");
    }

    @Test
    void anImageInACellDoesNotEscapeIntoTheBody() throws Exception {
        // The destination is restored after the cell, not cleared: the paragraph after the
        // table has to land in the body.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addTable(t -> t
                        .columns(DocumentTableColumn.auto())
                        .rowCells(DocumentTableCell.node(image())))
                .addParagraph(p -> p.text("After the table")))) {

            assertThat(document.getParagraphs())
                    .extracting(p -> p.getText())
                    .as("written to the body, not appended to the cell")
                    .contains("After the table");
        }
    }

    @Test
    void aFilledPillInACellIsAPanelWithItsTextInside() throws Exception {
        // Composed in a cell, the pill has no place in the layout, and its outline was dropped:
        // a rota's shift chips came out as bare hours.
        XWPFTableCell cell = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Mon"), DocumentTableCell.node(pill()))));

        assertThat(cell.getTables()).as("the pill, as a table of one cell").hasSize(1);
        XWPFTableCell chip = cell.getTables().get(0).getRow(0).getCell(0);
        assertThat(chip.getColor()).isEqualToIgnoringCase("1A5694");
        assertThat(chip.getText()).contains("09:00-17:00");
        assertThat(((Number) chip.getCTTc().getTcPr().getTcW().getW()).longValue())
                .as("as wide as its outline, and a point for the editor's face")
                .isEqualTo(61L * 20);
        assertThat(chip.getVerticalAlignment()).isEqualTo(XWPFTableCell.XWPFVertAlign.CENTER);
        // Held to its outline's 14pt, as the page draws it: left to its line, CobaltRota's 17.5pt
        // chips closed to 13pt round their hours.
        assertThat(cell.getTables().get(0).getRow(0).getHeight()).as("held to its outline")
                .isEqualTo(14 * 20);
        assertThat(cell.getTables().get(0).getRow(0).getCtRow().getTrPr().getTrHeightArray(0).getHRule())
                .as("at least: a longer label still grows it")
                .isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STHeightRule.AT_LEAST);
    }

    @Test
    void anOutlinedPillInACellIsAPanelWithItsBorders() throws Exception {
        XWPFTableCell cell = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Mon"), DocumentTableCell.node(
                        new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                                .name("Soft").roundedRect(60, 14, 7)
                                .stroke(com.demcha.compose.document.style.DocumentStroke.of(
                                        com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148), 1))
                                .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("12:00").build())
                                .build()))));

        assertThat(cell.getTables()).hasSize(1);
        XWPFTableCell chip = cell.getTables().get(0).getRow(0).getCell(0);
        assertThat(chip.getCTTc().getTcPr().getTcBorders().getTop().xmlText()).containsIgnoringCase("1A5694");
        assertThat(chip.getText()).contains("12:00");
    }

    @Test
    void aChipsLabelTallerThanItsOutlineIsCutToIt() throws Exception {
        // CobaltRota's stacked chips: a 9.2pt outline round 8.2pt text on a 10pt line. Grown to
        // the line in Word, each stood 0.8pt taller than the page draws it.
        XWPFTableCell chip = chipCell(null);
        double line = lineOf(chip);

        assertThat(line).as("the outline's height, not the text's line").isCloseTo(9.2, org.assertj.core.api.Assertions.within(0.06));
    }

    @Test
    void anOutlinedChipsLabelIsCutAlikeAsFarAsItsLettersAllow() throws Exception {
        // Word keeps the 1.1pt borders outside the content, leaving 7pt: less than the letters
        // and their margin need. Lato's 8.2pt line is 9.84pt, its baseline 1.75pt above the foot,
        // and the digits reach 0.07pt below it: 0.75pt from them, the foot can give 0.93pt, and
        // the cell centring the line, the top gives no more — 9.84 less twice 0.93.
        XWPFTableCell chip = chipCell(com.demcha.compose.document.style.DocumentStroke.of(
                com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148), 1.1));

        assertThat(lineOf(chip)).isCloseTo(7.98, org.assertj.core.api.Assertions.within(0.06));
    }

    @Test
    void aPaddedOutlinedChipStaysItsOutlinesHeightInWord() throws Exception {
        // The cell's margins are its padding less half each border, the page drawing that half
        // inside the box; Word keeps the borders outside the content. Margins, borders and the
        // line come to the 17.5pt outline, not 1pt short of it.
        com.demcha.compose.document.dsl.ShapeContainerBuilder chip = new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                .name("Padded").roundedRect(90, 17.5, 4).padding(DocumentInsets.of(2))
                .stroke(com.demcha.compose.document.style.DocumentStroke.of(
                        com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148), 1.1))
                .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("09:00-16:00")
                        .textStyle(com.demcha.compose.document.style.DocumentTextStyle.builder()
                                .fontName(com.demcha.compose.font.FontName.LATO).size(12).build()).build());
        DocumentNode node = chip.build();
        XWPFTableCell cell = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.text("Mon"), DocumentTableCell.node(node))));
        XWPFTableCell padded = cell.getTables().get(0).getRow(0).getCell(0);
        var margins = padded.getCTTc().getTcPr().getTcMar();
        double around = (DocxTwips.of(margins.getTop().getW()) + DocxTwips.of(margins.getBottom().getW())) / 20.0 + 2 * 1.1;

        assertThat(around + lineOf(padded)).isCloseTo(17.5, org.assertj.core.api.Assertions.within(0.1));
    }

    @Test
    void aLabelSetFromTheChipsTopIsLeftAsItWas() throws Exception {
        // It passes the outline below only: cut on both sides, its text would stand high.
        XWPFTableCell chip = chipCell(null, com.demcha.compose.document.node.LayerAlign.TOP_LEFT, 8.2);

        assertThat(lineOf(chip)).as("the text's own line").isGreaterThan(9.8);
    }

    private static XWPFTableCell chipCell(com.demcha.compose.document.style.DocumentStroke stroke) throws Exception {
        return chipCell(stroke, com.demcha.compose.document.node.LayerAlign.CENTER, 8.2);
    }

    private static XWPFTableCell chipCell(com.demcha.compose.document.style.DocumentStroke stroke,
                                          com.demcha.compose.document.node.LayerAlign align, double size) throws Exception {
        com.demcha.compose.document.dsl.ShapeContainerBuilder chip = new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                .name("Stacked").roundedRect(90, 9.2, 4).padding(DocumentInsets.zero())
                .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(20, 160, 70))
                .layer(new com.demcha.compose.document.dsl.ParagraphBuilder().text("09:00-16:00")
                        .textStyle(com.demcha.compose.document.style.DocumentTextStyle.builder()
                                .fontName(com.demcha.compose.font.FontName.LATO).size(size).build()).build(), align);
        if (stroke != null) {
            chip.stroke(stroke);
        }
        DocumentNode node = chip.build();
        XWPFTableCell cell = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.text("Mon"), DocumentTableCell.node(node))));
        return cell.getTables().get(0).getRow(0).getCell(0);
    }

    private static double lineOf(XWPFTableCell chip) {
        var spacing = chip.getParagraphs().get(0).getCTP().getPPr().getSpacing();
        return DocxTwips.of(spacing.getLine()) / 20.0;
    }

    @Test
    void aTileHoldingOnlyDrawingIsDrawnWhereThePageDrawsItNotAPanel() throws Exception {
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        String body = export(report, DocumentTableCell.node(new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                .name("Swatch").roundedRect(40, 12, 4)
                .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148))
                .center(new com.demcha.compose.document.dsl.EllipseBuilder().circle(4)
                        .fillColor(com.demcha.compose.document.style.DocumentColor.WHITE).build())
                .build()));

        assertThat(body.split("<w:tbl>", -1)).as("the table alone, no empty panel nested in it").hasSize(2);
        assertThat(anchors(body)).as("the tile and the dot on it, anchored where the page draws them")
                .anyMatch(anchor -> anchor.contains("prst=\"roundRect\""))
                .anyMatch(anchor -> anchor.contains("prst=\"ellipse\""));
        assertThat(anchors(body)).as("a composed tile is not taken into its cell: it stays on the page")
                .allMatch(anchor -> anchor.contains("<wp:positionV relativeFrom=\"page\">"));
        assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
    }

    @Test
    void thePillsSquaredCornersAreReportedAndItIsNotDrawnAgainAsAShape() throws Exception {
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        String body = export(report, DocumentTableCell.node(pill()));

        assertThat(report.get().notes()).anyMatch(note -> note.severity() == DocxExportReport.Severity.APPROXIMATED
                                                          && note.subject().equals("corner radius"));
        assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
        assertThat(anchors(body)).as("a panel frames its text; its box is not drawn over it as well").isEmpty();
    }

    @Test
    void aDiscUnderTextInACellIsDrawnBehindIt() throws Exception {
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        String body = export(report, DocumentTableCell.node(new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                .name("Dot").circle(14)
                .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148))
                .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("A").build())
                .build()));

        assertThat(anchors(body)).singleElement().asString()
                .contains("prst=\"ellipse\"").contains("behindDoc=\"1\"");
        assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
    }

    @Test
    void anIconInAPaintedCellStandsInFrontAndADiscUnderItsNumberStaysBehind() throws Exception {
        // Both editors paint a cell's shading over a drawing behind the text.
        com.demcha.compose.document.table.DocumentTableStyle navy = com.demcha.compose.document.table.DocumentTableStyle
                .builder().fillColor(com.demcha.compose.document.style.DocumentColor.rgb(16, 32, 80)).build();
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        String body = export(report, List.of(
                DocumentTableCell.node(new com.demcha.compose.document.dsl.EllipseBuilder().name("Icon").circle(8)
                        .fillColor(com.demcha.compose.document.style.DocumentColor.WHITE).build()).withStyle(navy),
                DocumentTableCell.node(new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                        .name("Disc").circle(14)
                        .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148))
                        .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("7").build())
                        .build()).withStyle(navy)));

        assertThat(anchors(body)).as("the icon, alone in its navy cell").anyMatch(anchor ->
                anchor.contains("prst=\"ellipse\"") && anchor.contains("behindDoc=\"0\"") && anchor.contains("FFFFFF"));
        assertThat(anchors(body)).as("the disc, under its number").anyMatch(anchor ->
                anchor.contains("prst=\"ellipse\"") && anchor.contains("behindDoc=\"1\"") && anchor.contains("1A5694"));
    }

    @Test
    void anIconInAWhiteCellStandsInFrontOfItsShading() throws Exception {
        // A cell whose style names white is shaded white in Word, and the shading hid the icon.
        com.demcha.compose.document.table.DocumentTableStyle white = com.demcha.compose.document.table.DocumentTableStyle
                .builder().fillColor(com.demcha.compose.document.style.DocumentColor.WHITE).build();
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        String body = export(report, List.of(DocumentTableCell.node(
                new com.demcha.compose.document.dsl.EllipseBuilder().name("Icon").circle(8)
                        .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148)).build())
                .withStyle(white)));

        assertThat(body).contains("w:fill=\"FFFFFF\"");
        assertThat(anchors(body)).singleElement().asString().contains("behindDoc=\"0\"");
    }

    @Test
    void aBoxLostInACellIsStillReportedWhenItsTableDrawsSomethingElse() throws Exception {
        // A rectangle framing text inside a layer stack is no panel, and its table leaves it to one.
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        export(report, List.of(
                DocumentTableCell.node(new com.demcha.compose.document.dsl.EllipseBuilder().name("Icon").circle(8)
                        .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148)).build()),
                DocumentTableCell.node(new com.demcha.compose.document.dsl.LayerStackBuilder().name("Pill")
                        .layer(new com.demcha.compose.document.dsl.ShapeBuilder().name("PillBox").size(60, 14)
                                .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148)).build())
                        .layer(new com.demcha.compose.document.dsl.ParagraphBuilder().text("Open").build())
                        .build())));

        assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).as("the box, not lost in silence")
                .isPositive();
    }

    @Test
    void aFlatPathInACellIsDrawnAndAGradientOneIsStillReported() throws Exception {
        // An SVG icon carries its colour as fillColor, not as a paint; only a gradient goes undrawn.
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        String body = export(report, List.of(DocumentTableCell.node(triangle()
                .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148)).name("Icon").build())));

        assertThat(anchors(body)).singleElement().asString().contains("1A5694");
        assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();

        export(report, List.of(
                DocumentTableCell.node(new com.demcha.compose.document.dsl.EllipseBuilder().name("Dot").circle(8)
                        .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148)).build()),
                DocumentTableCell.node(triangle().name("Glow").fill(com.demcha.compose.document.style.DocumentPaint.linear(
                        com.demcha.compose.document.style.DocumentColor.WHITE,
                        com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148))).build())));

        assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).as("the gradient path").isPositive();
    }

    @Test
    void aRowHoldingPaddedContentKeepsTheHeightThePageGaveIt() throws Exception {
        // The padding is the composed section's, not the cell's: Word, given only the text,
        // closed the row round it.
        try (XWPFDocument document = exportDocument(List.of(DocumentTableCell.node(
                new com.demcha.compose.document.dsl.SectionBuilder()
                        .name("Padded").padding(com.demcha.compose.document.style.DocumentInsets.of(12))
                        .addParagraph(p -> p.text("Coastline Advanced")).build())))) {
            assertThat(heightOf(document.getTables().get(0))).as("the text and 24pt of padding")
                    .isGreaterThan(24 * 20 + 8 * 20);
        }
    }

    @Test
    void aRowsHeightIsWrittenLessTheMarginsItsCellsHold() throws Exception {
        // Two rows the page makes equally tall: padded inside the composed content, and padded as
        // the cell. LibreOffice adds a cell's margins to the written height, so the second row's
        // is written less them.
        DocumentInsets twelve = new DocumentInsets(12, 0, 12, 0);
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page
                    .addTable(t -> t.columns(DocumentTableColumn.fixed(200))
                            .rowCells(DocumentTableCell.node(new com.demcha.compose.document.dsl.SectionBuilder()
                                    .name("Padded").padding(twelve)
                                    .addParagraph(p -> p.text("Row")).build())
                                    .withStyle(com.demcha.compose.document.table.DocumentTableStyle.builder()
                                            .padding(DocumentInsets.zero()).build())))
                    .addTable(t -> t.columns(DocumentTableColumn.fixed(200))
                            .rowCells(DocumentTableCell.node(new com.demcha.compose.document.dsl.ParagraphBuilder()
                                    .text("Row").build())
                                    .withStyle(com.demcha.compose.document.table.DocumentTableStyle.builder()
                                            .padding(twelve).build()))));
            byte[] docx = session.export(new DocxSemanticBackend());
            try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(docx))) {
                int inside = heightOf(document.getTables().get(0));
                int asTheCell = heightOf(document.getTables().get(1));

                // 12pt above and below, less the default 1pt rule on each side that Word gives
                // room of its own.
                assertThat(inside - asTheCell).as("the second cell's 22pt of margins").isEqualTo(22 * 20);
            }
        }
    }

    @Test
    void aParagraphHoldingAPictureKeepsItsOwnLineFromALaterOneOfTheSameText() throws Exception {
        // Its laid-out text is its text runs'. Left unpaired, it left its 8pt line for the 16pt
        // "Paid" after it, whose text was then clipped to that line.
        try (XWPFDocument document = exportDocument(List.of(
                DocumentTableCell.node(new com.demcha.compose.document.dsl.ParagraphBuilder()
                        .inlineImage(DocumentImageData.fromBytes(pngBytes()), 6, 6)
                        .inlineText("Paid", com.demcha.compose.document.style.DocumentTextStyle.DEFAULT.withSize(8))
                        .build()),
                DocumentTableCell.node(new com.demcha.compose.document.dsl.ParagraphBuilder().text("Paid")
                        .textStyle(com.demcha.compose.document.style.DocumentTextStyle.DEFAULT.withSize(16)).build())))) {
            List<org.apache.poi.xwpf.usermodel.XWPFParagraph> paid = cellParagraphs(document, "Paid");

            assertThat(paid).hasSize(2);
            assertThat(lineOf(paid.get(1))).as("a 16pt line, not the 8pt one").isGreaterThan(14 * 20);
        }
    }

    /** A table's first row's written height, which the export writes "at least". */
    private static int heightOf(XWPFTable table) {
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTrPr properties = table.getRow(0).getCtRow().getTrPr();
        assertThat(properties).isNotNull();
        assertThat(properties.sizeOfTrHeightArray()).isEqualTo(1);
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHeight height = properties.getTrHeightArray(0);
        assertThat(height.getHRule()).isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STHeightRule.AT_LEAST);
        return ((Number) height.getVal()).intValue();
    }

    /** A paragraph's written line height, in twips. */
    private static int lineOf(org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph) {
        return ((Number) paragraph.getCTP().getPPr().getSpacing().getLine()).intValue();
    }

    /** The paragraphs of the body's tables reading a text, in document order. */
    private static List<org.apache.poi.xwpf.usermodel.XWPFParagraph> cellParagraphs(XWPFDocument document, String text) {
        List<org.apache.poi.xwpf.usermodel.XWPFParagraph> found = new java.util.ArrayList<>();
        for (XWPFTable table : document.getTables()) {
            for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph : cell.getParagraphs()) {
                        if (paragraph.getText().equals(text)) {
                            found.add(paragraph);
                        }
                    }
                }
            }
        }
        return found;
    }

    @Test
    void theSameTextComposedTwiceTakesEachItsOwnLine() throws Exception {
        // Paragraphs are paired with their table's fragments in the order both were laid out.
        try (XWPFDocument document = exportDocument(List.of(
                DocumentTableCell.node(new com.demcha.compose.document.dsl.ParagraphBuilder().text("0.00")
                        .textStyle(com.demcha.compose.document.style.DocumentTextStyle.DEFAULT.withSize(8)).build()),
                DocumentTableCell.node(new com.demcha.compose.document.dsl.ParagraphBuilder().text("0.00")
                        .textStyle(com.demcha.compose.document.style.DocumentTextStyle.DEFAULT.withSize(16)).build())))) {
            List<org.apache.poi.xwpf.usermodel.XWPFParagraph> amounts = cellParagraphs(document, "0.00");

            assertThat(amounts).hasSize(2).allSatisfy(amount -> assertThat(
                    amount.getCTP().getPPr().getSpacing().getLineRule()).hasToString("exact"));
            assertThat(lineOf(amounts.get(1))).isEqualTo(2 * lineOf(amounts.get(0)));
        }
    }

    /** The export of a table of one row: a text cell, then the composed ones. */
    private static XWPFDocument exportDocument(List<DocumentTableCell> composed) throws Exception {
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            List<DocumentTableCell> cells = new java.util.ArrayList<>();
            cells.add(DocumentTableCell.text("Mon"));
            cells.addAll(composed);
            List<DocumentTableColumn> columns = new java.util.ArrayList<>();
            for (int i = 0; i < cells.size(); i++) {
                columns.add(DocumentTableColumn.auto());
            }
            session.pageFlow(page -> page.addTable(t -> t
                    .columns(columns.toArray(DocumentTableColumn[]::new))
                    .rowCells(cells)));
            return new XWPFDocument(new java.io.ByteArrayInputStream(session.export(new DocxSemanticBackend())));
        }
    }

    private static com.demcha.compose.document.dsl.PathBuilder triangle() {
        return new com.demcha.compose.document.dsl.PathBuilder().size(10, 10)
                .moveTo(0, 0).lineTo(10, 0).lineTo(5, 10).closePath();
    }

    private static String export(java.util.concurrent.atomic.AtomicReference<DocxExportReport> report,
                                 List<DocumentTableCell> composed) throws Exception {
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(com.demcha.compose.document.style.DocumentInsets.of(20)).create()) {
            List<DocumentTableCell> cells = new java.util.ArrayList<>();
            cells.add(DocumentTableCell.text("Mon"));
            cells.addAll(composed);
            List<DocumentTableColumn> columns = new java.util.ArrayList<>();
            for (int i = 0; i < cells.size(); i++) {
                columns.add(DocumentTableColumn.auto());
            }
            session.pageFlow(page -> page.addTable(t -> t
                    .columns(columns.toArray(DocumentTableColumn[]::new))
                    .rowCells(cells)));
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(docx))) {
                return document.getDocument().xmlText();
            }
        }
    }

    private static String export(java.util.concurrent.atomic.AtomicReference<DocxExportReport> report,
                                 DocumentTableCell composed) throws Exception {
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(com.demcha.compose.document.style.DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addTable(t -> t
                    .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                    .rowCells(DocumentTableCell.text("Mon"), composed)));
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(docx))) {
                return document.getDocument().xmlText();
            }
        }
    }

    private static List<String> anchors(String xml) {
        List<String> anchors = new java.util.ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("<wp:anchor .*?</wp:anchor>", java.util.regex.Pattern.DOTALL).matcher(xml);
        while (matcher.find()) {
            anchors.add(matcher.group());
        }
        return anchors;
    }

    private static DocumentNode pill() {
        return new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                .name("Chip")
                .roundedRect(60, 14, 7)
                .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148))
                .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("09:00-17:00").build())
                .build();
    }

    private static DocumentNode image() {
        return new com.demcha.compose.document.dsl.ImageBuilder()
                .name("Logo")
                .source(DocumentImageData.fromBytes(pngBytes()))
                .width(40)
                .height(20)
                .build();
    }

    private static DocumentNode list(String... items) {
        return new com.demcha.compose.document.dsl.ListBuilder()
                .name("Items")
                .bullet()
                .items(items)
                .build();
    }

    private static DocumentNode innerTable() {
        return new com.demcha.compose.document.dsl.TableBuilder()
                .name("Inner")
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .row("Hours", "12")
                .build();
    }

    private static byte[] pngBytes() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    /** The second cell of the only table — the one these cases compose. */
    private static XWPFTableCell onlyTableCell(Consumer<PageFlowBuilder> content) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, content)) {
            assertThat(document.getTables()).hasSize(1);
            XWPFTable table = document.getTables().get(0);
            return table.getRow(0).getCell(table.getRow(0).getTableCells().size() - 1);
        }
    }
}
