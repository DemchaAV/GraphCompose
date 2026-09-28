package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentNode;
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
        assertThat(cell.getTables().get(0).getRow(0).getHeight()).as("as tall as its line, not held to its outline")
                .isZero();
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
    void aTileHoldingOnlyDrawingIsNotAPanel() throws Exception {
        XWPFTableCell cell = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Mon"), DocumentTableCell.node(
                        new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                                .name("Swatch").roundedRect(40, 12, 4)
                                .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148))
                                .center(new com.demcha.compose.document.dsl.EllipseBuilder().circle(4)
                                        .fillColor(com.demcha.compose.document.style.DocumentColor.WHITE).build())
                                .build()))));

        assertThat(cell.getTables()).as("nothing a cell can hold: no empty panel").isEmpty();
    }

    @Test
    void thePillsSquaredCornersAreReportedAndNothingIsDropped() throws Exception {
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        export(report, DocumentTableCell.node(pill()));

        assertThat(report.get().notes()).anyMatch(note -> note.severity() == DocxExportReport.Severity.APPROXIMATED
                                                          && note.subject().equals("corner radius"));
        assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
    }

    @Test
    void aCircleInACellIsStillDroppedAndTheReportSaysWhy() throws Exception {
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        export(report, DocumentTableCell.node(new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                .name("Dot").circle(14)
                .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148))
                .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("A").build())
                .build()));

        assertThat(report.get().notes()).anyMatch(note -> note.severity() == DocxExportReport.Severity.DROPPED
                                                          && note.detail().contains("composed inside a table cell"));
    }

    private static void export(java.util.concurrent.atomic.AtomicReference<DocxExportReport> report,
                               DocumentTableCell composed) throws Exception {
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(com.demcha.compose.document.style.DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addTable(t -> t
                    .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                    .rowCells(DocumentTableCell.text("Mon"), composed)));
            session.export(new DocxSemanticBackend(report::set));
        }
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
