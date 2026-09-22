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

        long column = Long.parseLong(String.valueOf(cell.getTableRow().getTable()
                .getCTTbl().getTblGrid().getGridColArray(1).getW()));
        long nested = Long.parseLong(String.valueOf(cell.getTables().get(0)
                .getCTTbl().getTblPr().getTblW().getW()));

        assertThat(cell.getTables().get(0).getCTTbl().getTblPr().getTblW().getType().toString())
                .as("stated, not left to Word")
                .isEqualTo("dxa");
        // The column less the margins Word keeps inside every cell edge: 5.4pt a side.
        assertThat(nested).isEqualTo(column - 2 * 108);
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
