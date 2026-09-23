package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.layout.payloads.TableRowFragmentPayload;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.engine.components.content.table.TableResolvedCell;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A column sized to its content gets a point more in Word than on the page.
 *
 * <p>An auto column is exactly as wide as its widest unwrapped cell: the engine gives it no
 * slack, because on the page it needs none. An editor sets the text in its own substitute
 * for the face and keeps a border's width clear inside the cell, and either is enough to push
 * the widest cell onto a second line — measured on the probe corpus through LibreOffice, a
 * billing table's widest item wrapped and its row doubled once its cells were written at their
 * true size. A point per auto column keeps the page's line breaks.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxEditorSlackTest {

    private static final long SLACK_TWIPS = Math.round(DocxSemanticBackend.EDITOR_COLUMN_SLACK_POINTS * 20);

    @Test
    void anAutoColumnGetsAPointMoreThanTheLayoutGaveIt() throws Exception {
        Exported exported = export(page -> page.addTable(t -> t
                .name("Billing")
                .autoColumns(2)
                .row("Platform subscription", "1 440.00")));

        for (int column = 0; column < 2; column++) {
            assertThat(exported.gridTwips(column))
                    .as("column %d: the layout's width and the editor's point", column)
                    .isEqualTo(exported.layoutTwips(column) + SLACK_TWIPS);
        }
    }

    @Test
    void aFixedColumnKeepsTheWidthTheDocumentStated() throws Exception {
        Exported exported = export(page -> page.addTable(t -> t
                .name("Fixed")
                .columns(DocumentTableColumn.fixed(120), DocumentTableColumn.auto())
                .row("Label", "Value")));

        assertThat(exported.gridTwips(0)).isEqualTo(Math.round(120 * 20.0));
        assertThat(exported.gridTwips(1)).isEqualTo(exported.layoutTwips(1) + SLACK_TWIPS);
    }

    @Test
    void aTableWithAStatedWidthKeepsItExactly() throws Exception {
        Exported exported = export(page -> page.addTable(t -> t
                .name("Stated")
                .width(200)
                .autoColumns(2)
                .row("A", "B")));

        assertThat(exported.gridTwips(0) + exported.gridTwips(1)).isEqualTo(Math.round(200 * 20.0));
    }

    /** A table exported through a session, with the layout the session sized it with. */
    private record Exported(XWPFTable table, List<TableResolvedCell> firstRow) {

        long gridTwips(int column) {
            CTTblGrid grid = table.getCTTbl().getTblGrid();
            return DocxTwips.of(grid.getGridColArray(column).getW());
        }

        long layoutTwips(int column) {
            return Math.round(firstRow.get(column).width() * 20.0);
        }
    }

    private static Exported export(Consumer<PageFlowBuilder> content) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 600)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(content::accept);
            List<TableResolvedCell> firstRow = session.layoutGraph().fragments().stream()
                    .filter(fragment -> fragment.payload() instanceof TableRowFragmentPayload)
                    .map(fragment -> ((TableRowFragmentPayload) fragment.payload()).cells())
                    .findFirst()
                    .orElseThrow();
            byte[] docx = session.export(new DocxSemanticBackend());
            XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx));
            return new Exported(document.getTables().get(0), firstRow);
        }
    }
}
