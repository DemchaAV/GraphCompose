package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Table structure in the DOCX semantic backend.
 *
 * <p>An authored row is not a row of columns. A {@code rowSpan} covers positions in the rows
 * below and those rows do not repeat the covered cells; a {@code colSpan} makes the number of
 * authored records differ from the number of columns. The backend used to size the grid from
 * the first row's record count and stop filling at the last column that existed, so a table
 * with any span came out narrow and the cells past the end were dropped without a word.</p>
 *
 * <p>These pin the grid the cells actually occupy, the merge markup Word uses to express it,
 * and the two cell shapes that carried nothing before: a composed cell, whose {@code lines()}
 * is empty by definition, and a multi-line cell, whose lines were joined with a character
 * Word does not read as a break.</p>
 */
class DocxTableStructureTest {

    @Test
    void aColSpanWidensItsCellInsteadOfNarrowingTheTable() throws Exception {
        XWPFTable table = firstTable(new TableBuilder()
                .name("Spans")
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Header spans two"). colSpan(2),
                        DocumentTableCell.text("Third"))
                .row("a", "b", "c")
                .build());

        // Three columns, from the first row's colSpan sum — not its two records.
        assertThat(table.getRow(0).getTableCells()).hasSize(2);
        assertThat(gridSpan(table.getRow(0).getCell(0))).isEqualTo(2);

        // The row below keeps all three cells. The third used to fall outside the grid.
        assertThat(table.getRow(1).getTableCells()).hasSize(3);
        assertThat(table.getRow(1).getCell(0).getText()).isEqualTo("a");
        assertThat(table.getRow(1).getCell(1).getText()).isEqualTo("b");
        assertThat(table.getRow(1).getCell(2).getText()).isEqualTo("c");
    }

    @Test
    void aRowSpanMergesVerticallyAndTheRowBelowKeepsItsOwnCells() throws Exception {
        XWPFTable table = firstTable(new TableBuilder()
                .name("Merged")
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Tall").rowSpan(2), DocumentTableCell.text("top"))
                .rowCells(DocumentTableCell.text("bottom"))
                .build());

        assertThat(vMerge(table.getRow(0).getCell(0))).isEqualTo(STMerge.RESTART);
        assertThat(table.getRow(0).getCell(0).getText()).isEqualTo("Tall");

        // The covered position is a cell carrying the continuation marker, so the authored
        // cell beside it stays in its own column instead of sliding left.
        assertThat(table.getRow(1).getTableCells()).hasSize(2);
        assertThat(vMerge(table.getRow(1).getCell(0))).isEqualTo(STMerge.CONTINUE);
        assertThat(table.getRow(1).getCell(1).getText()).isEqualTo("bottom");
    }

    @Test
    void aCellSpanningBothWaysCarriesTheMergeOnEveryRowItCovers() throws Exception {
        // The hardest position for the cover matrix: one cell owning a 2x2 rectangle of a
        // 3x3 grid. The row below authors one cell, not three, and the continuation needs
        // the width as well as the merge marker — a w:vMerge without w:gridSpan would leave
        // Word a row two grid columns short of the others.
        XWPFTable table = firstTable(new TableBuilder()
                .name("Both")
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("A").colSpan(2).rowSpan(2), DocumentTableCell.text("B"))
                .rowCells(DocumentTableCell.text("C"))
                .row("D", "E", "F")
                .build());

        assertThat(table.getRow(0).getTableCells()).hasSize(2);
        assertThat(gridSpan(table.getRow(0).getCell(0))).isEqualTo(2);
        assertThat(vMerge(table.getRow(0).getCell(0))).isEqualTo(STMerge.RESTART);
        assertThat(table.getRow(0).getCell(0).getText()).isEqualTo("A");
        assertThat(table.getRow(0).getCell(1).getText()).isEqualTo("B");

        assertThat(table.getRow(1).getTableCells()).hasSize(2);
        assertThat(gridSpan(table.getRow(1).getCell(0))).isEqualTo(2);
        assertThat(vMerge(table.getRow(1).getCell(0))).isEqualTo(STMerge.CONTINUE);
        assertThat(table.getRow(1).getCell(1).getText()).isEqualTo("C");

        // Every row still accounts for three grid columns.
        assertThat(table.getRow(2).getTableCells()).hasSize(3);
        assertThat(table.getRow(2).getCell(2).getText()).isEqualTo("F");
    }

    @Test
    void aComposedCellExportsItsContentInsteadOfNothing() throws Exception {
        XWPFTable table = firstTable(new TableBuilder()
                .name("Composed")
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(
                        DocumentTableCell.node(new ParagraphNode("CellParagraph", "composed text",
                                DocumentTextStyle.DEFAULT, TextAlign.LEFT, 0.0,
                                DocumentInsets.zero(), DocumentInsets.zero())),
                        DocumentTableCell.text("plain"))
                .build());

        // lines() is empty for a composed cell, which is what the backend used to write.
        assertThat(table.getRow(0).getCell(0).getText()).contains("composed text");
        assertThat(table.getRow(0).getCell(1).getText()).isEqualTo("plain");
    }

    @Test
    void aCellTakesTheMostSpecificStyleInTheCascade() throws Exception {
        DocumentTableStyle bold = DocumentTableStyle.builder()
                .textStyle(DocumentTextStyle.builder().size(11)
                        .decoration(DocumentTextDecoration.BOLD).build())
                .build();
        DocumentTableStyle plain = DocumentTableStyle.builder()
                .textStyle(DocumentTextStyle.builder().size(11).build())
                .build();

        XWPFTable table = firstTable(new TableBuilder()
                .name("Styled")
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .defaultCellStyle(plain)
                .rowCells(DocumentTableCell.text("bold").withStyle(bold),
                        DocumentTableCell.text("default"))
                .build());

        assertThat(table.getRow(0).getCell(0).getParagraphs().get(0).getRuns().get(0).isBold()).isTrue();
        assertThat(table.getRow(0).getCell(1).getParagraphs().get(0).getRuns().get(0).isBold()).isFalse();
    }

    @Test
    void aMultiLineCellBreaksItsLinesRatherThanJoiningThem() throws Exception {
        XWPFTable table = firstTable(new TableBuilder()
                .name("Lines")
                .columns(DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.lines("first", "second"))
                .build());

        XWPFTableCell cell = table.getRow(0).getCell(0);
        var run = cell.getParagraphs().get(0).getRuns().get(0).getCTR();
        // The lines used to be joined into one w:t with a newline inside, which Word reads
        // as a space. They are two texts around a real break now. Asserting on the markup
        // rather than on getText(), which renders a break back as "\n" either way.
        assertThat(run.getBrList()).hasSize(1);
        assertThat(run.getTList()).hasSize(2);
        assertThat(run.getTList().get(0).getStringValue()).isEqualTo("first");
        assertThat(run.getTList().get(1).getStringValue()).isEqualTo("second");
    }

    @Test
    void aTableThatClaimsNoColumnAtAllStillExports() throws Exception {
        // No declared column and a row with no cells: the grid has no positions. Sizing it
        // to one anyway leaves a slot nothing covers, and reading that slot aborts the whole
        // export for a document the PDF backend renders.
        XWPFTable table = firstTable(new TableBuilder().name("Empty").row().build());

        assertThat(table.getRows()).hasSize(1);
        assertThat(table.getRow(0).getTableCells()).hasSize(1);
    }

    @Test
    void aCellWhoseContentWritesNothingKeepsAParagraph() throws Exception {
        // A wrapper that ended up with no children contributes nothing, and the cell's own
        // paragraph was removed before writing. A w:tc with no block-level child is not a
        // shape Word should be handed.
        XWPFTable table = firstTable(new TableBuilder()
                .name("EmptyComposed")
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(
                        DocumentTableCell.node(new ContainerNode("Wrapper", List.of(), 0.0,
                                DocumentInsets.zero(), DocumentInsets.zero(), null, null)),
                        DocumentTableCell.text("beside"))
                .build());

        assertThat(table.getRow(0).getCell(0).getParagraphs()).isNotEmpty();
        assertThat(table.getRow(0).getCell(1).getText()).isEqualTo("beside");
    }

    private static int gridSpan(XWPFTableCell cell) {
        return cell.getCTTc().getTcPr().getGridSpan().getVal().intValue();
    }

    private static STMerge.Enum vMerge(XWPFTableCell cell) {
        return cell.getCTTc().getTcPr().getVMerge().getVal();
    }

    private static XWPFTable firstTable(TableNode node) throws Exception {
        byte[] docx;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {
            session.add(node);
            docx = session.export(new DocxSemanticBackend());
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            List<XWPFTable> tables = document.getTables();
            assertThat(tables).hasSize(1);
            return tables.get(0);
        }
    }
}
