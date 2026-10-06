package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A table with padding on its sides is written as the page draws it: its rows inside the
 * padding, on the grid the layout resolved, at the heights it gave them.
 *
 * <p>The page draws a table's rows inside its padding, so they are narrower than the table's
 * placement by that padding. The export matched a table's rows to the layout by the
 * placement's width, and a table with side padding matched none: it was written without the
 * layout's grid, with no row heights and rows Word could break, and stood its padding left of
 * the page's.</p>
 */
class DocxPaddedTableTest {

    @Test
    void aTableWithSidePaddingIsWrittenAsOneWithThoseMarginsIs() throws Exception {
        // The page draws both tables' rows at the same place and width.
        String padded = bodyOf(page -> page.addTable(table -> rows(table.columns(
                DocumentTableColumn.fixed(120), DocumentTableColumn.fixed(140)), 2)
                .padding(new DocumentInsets(0, 10, 0, 30))));
        String margined = bodyOf(page -> page.addTable(table -> rows(table.columns(
                DocumentTableColumn.fixed(120), DocumentTableColumn.fixed(140)), 2)
                .margin(new DocumentInsets(0, 10, 0, 30))));

        assertThat(padded).isEqualTo(margined);
        assertThat(padded).as("the layout's grid").contains("<w:tblLayout w:type=\"fixed\"/>")
                .as("rows Word may not break").contains("<w:cantSplit")
                .as("the row heights it gave them").contains("w:hRule=\"atLeast\"")
                .as("its padding as its indent").contains("<w:tblInd");
    }

    @Test
    void aTableOfAutoColumnsWithSidePaddingKeepsTheGridItsContentResolved() throws Exception {
        // An auto column is its content's width, a point of editor slack on it from the room
        // the padding leaves.
        String padded = bodyOf(page -> page.addTable(table -> rows(table.columns(
                DocumentTableColumn.auto(), DocumentTableColumn.auto()), 2)
                .padding(new DocumentInsets(0, 12, 0, 18))));
        String margined = bodyOf(page -> page.addTable(table -> rows(table.columns(
                DocumentTableColumn.auto(), DocumentTableColumn.auto()), 2)
                .margin(new DocumentInsets(0, 12, 0, 18))));

        assertThat(padded).isEqualTo(margined).contains("<w:tblLayout w:type=\"fixed\"/>");
    }

    @Test
    void anAutoColumnGetsTheSlackTheRoomInsideThePaddingLeaves() throws Exception {
        // The fixed column and the auto one leave less room than the padding: counted once, the
        // auto column takes its point of editor slack; counted twice, there is none to give.
        String padded = bodyOf(page -> page.addTable(table -> rows(table.columns(
                DocumentTableColumn.fixed(300), DocumentTableColumn.auto()), 2)
                .padding(new DocumentInsets(0, 12, 0, 18))));
        String margined = bodyOf(page -> page.addTable(table -> rows(table.columns(
                DocumentTableColumn.fixed(300), DocumentTableColumn.auto()), 2)
                .margin(new DocumentInsets(0, 12, 0, 18))));

        assertThat(padded).isEqualTo(margined);
    }

    @Test
    void aTableComposedInACellIsHeldInByItsPadding() throws Exception {
        String body = bodyOf(page -> page.addTable(outer -> outer.columns(DocumentTableColumn.fixed(240))
                .rowCells(com.demcha.compose.document.table.DocumentTableCell.node(new TableBuilder()
                        .columns(DocumentTableColumn.fixed(120)).row("Inner")
                        .padding(new DocumentInsets(0, 0, 0, 15)).build()))));

        assertThat(body).as("its indent in the cell, the padding").containsPattern("<w:tblInd[^>]*w:w=\"300\"");
    }

    @Test
    void aTableWithSidePaddingLosesNothingTheReportNames() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = session()) {
            session.pageFlow(page -> page.addTable(table -> rows(table.columns(DocumentTableColumn.fixed(200)), 1)
                    .padding(new DocumentInsets(4, 10, 4, 30))));
            session.export(new DocxSemanticBackend(report::set));
        }

        assertThat(report.get().bySubject()).doesNotContainKey("TableNode");
    }

    /** Two rows of a table of one column or two. */
    private static TableBuilder rows(TableBuilder table, int columns) {
        return columns == 1 ? table.row("First").row("Second") : table.row("First", "A").row("Second", "B");
    }

    private static String bodyOf(Consumer<PageFlowBuilder> content) throws Exception {
        try (DocumentSession session = session()) {
            session.pageFlow(content::accept);
            try (XWPFDocument document = new XWPFDocument(
                    new ByteArrayInputStream(session.export(new DocxSemanticBackend())))) {
                return document.getDocument().getBody().xmlText();
            }
        }
    }

    private static DocumentSession session() {
        return GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create();
    }
}
