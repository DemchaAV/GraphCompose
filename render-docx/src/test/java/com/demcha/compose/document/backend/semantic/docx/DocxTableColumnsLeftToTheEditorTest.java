package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.ChartSpec;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.ChartNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A table whose columns the layout does not show the widths of — composed in a table cell, or
 * one with a column edge no row of it shows — is written with its columns left to the editor,
 * and the report says so where the page sizes an auto column to its content.
 */
class DocxTableColumnsLeftToTheEditorTest {

    /** The report addresses a table by its node kind, as it does a row whose columns are Word's. */
    private static final String SUBJECT = "TableNode";
    private static final String LEFT ="its columns are the editor's to size, where the page sizes an auto column "
                                       + "to its content: ";
    private static final String COMPOSED = LEFT + "composed in a table cell, it is laid out among its table's own "
                                           + "rows, which do not tell its columns apart";
    private static final String HIDDEN = LEFT + "the layout's rows of it do not show every one of its column edges";
    private static final String FIXED = "; its fixed columns' widths are not written either";

    @Test
    void aTableOfAutoColumnsComposedInACellIsNamed() throws Exception {
        List<DocxExportReport.Note> notes = tableNotes(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.fixed(250))
                .rowCells(DocumentTableCell.text("Breakdown"),
                        DocumentTableCell.node(inner(DocumentTableColumn.auto(), DocumentTableColumn.auto())))));
        assertThat(notes).singleElement().satisfies(note -> {
            assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.APPROXIMATED);
            assertThat(note.detail()).isEqualTo(COMPOSED);
            assertThat(note.path()).as("composed content is named under its table, by its cell")
                    .endsWith("TableNode[0]/row[0]/cell[1]/TableNode[0]");
        });
    }

    @Test
    void aFixedColumnBesideAnAutoOneComposedInACellIsNamedWithIt() throws Exception {
        assertThat(tableNotes(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.fixed(300))
                .rowCells(DocumentTableCell.text("Breakdown"),
                        DocumentTableCell.node(inner(DocumentTableColumn.fixed(120), DocumentTableColumn.auto()))))))
                .extracting(DocxExportReport.Note::detail).containsExactly(COMPOSED + FIXED);
    }

    @Test
    void aFixedColumnBesideAPositionPastTheDeclaredColumnsIsNamedWithIt() throws Exception {
        // The row's second cell stands in a column the table does not declare, which is sized as
        // an auto one; a named table composed in a cell is still composed in it.
        assertThat(tableNotes(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.fixed(300))
                .rowCells(DocumentTableCell.text("Breakdown"), DocumentTableCell.node(new TableBuilder()
                        .name("Hours").columns(DocumentTableColumn.fixed(120)).row("Hours", "12").build())))))
                .extracting(DocxExportReport.Note::detail).containsExactly(COMPOSED + FIXED);
    }

    @Test
    void aTableOfFixedColumnsComposedInACellIsWrittenAtThemAndNotNamed() throws Exception {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument document = DocxExports.withLayout(500, 400, 30, page -> page
                .addTable(t -> t.columns(DocumentTableColumn.auto(), DocumentTableColumn.fixed(300))
                        .rowCells(DocumentTableCell.text("Breakdown"), DocumentTableCell.node(
                                inner(DocumentTableColumn.fixed(120), DocumentTableColumn.fixed(110))))))) {
            var grid = document.getTables().get(0).getRow(0).getCell(1).getTables().get(0).getCTTbl().getTblGrid();
            assertThat(grid.getGridColList()).extracting(column -> DocxTwips.of(column.getW()))
                    .containsExactly(2400L, 2200L);
        }
        assertThat(tableNotes(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.fixed(300))
                .rowCells(DocumentTableCell.text("Breakdown"),
                        DocumentTableCell.node(inner(DocumentTableColumn.fixed(120), DocumentTableColumn.fixed(110)))))))
                .isEmpty();
    }

    @Test
    void aTableWithAColumnEdgeNoRowShowsIsNamedWithItsFixedColumns() throws Exception {
        assertThat(tableNotes(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(120), DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("A wide heading here").colSpan(2), DocumentTableCell.text("c"))
                .rowCells(DocumentTableCell.text("Another one").colSpan(2), DocumentTableCell.text("dd")))))
                .extracting(DocxExportReport.Note::detail).containsExactly(HIDDEN + FIXED);
    }

    @Test
    void aTableWithAColumnEdgeNoRowShowsIsNamed() throws Exception {
        List<DocxExportReport.Note> notes = tableNotes(page -> page.addTable(t -> t.name("Spans")
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("A wide heading here").colSpan(2), DocumentTableCell.text("c"))
                .rowCells(DocumentTableCell.text("Another one").colSpan(2), DocumentTableCell.text("dd"))));
        assertThat(notes).singleElement().satisfies(note -> {
            assertThat(note.detail()).isEqualTo(HIDDEN);
            assertThat(note.path()).as("a table in the flow is found by its path").contains("Spans");
        });
    }

    @Test
    void aTableTheLayoutMeasuresIsNotNamed() throws Exception {
        assertThat(tableNotes(page -> page
                .addTable(t -> t.columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                        .row("Hours", "12").row("Rate", "95.00 per hour"))
                .addTable(t -> t.columns(DocumentTableColumn.auto(), DocumentTableColumn.auto(), DocumentTableColumn.auto())
                        .rowCells(DocumentTableCell.text("A wide heading here").colSpan(2), DocumentTableCell.text("c"))
                        .rowCells(DocumentTableCell.text("a"), DocumentTableCell.text("b"), DocumentTableCell.text("d")))
                .addTable(t -> t.columns(DocumentTableColumn.auto(), DocumentTableColumn.auto(), DocumentTableColumn.auto())
                        .rowCells(DocumentTableCell.text("A wide heading here").colSpan(2), DocumentTableCell.text("c"))
                        .rowCells(DocumentTableCell.text("a"), DocumentTableCell.text("Another one").colSpan(2)))))
                .as("a row that shows every edge measures the table, and so do rows that show them between them")
                .isEmpty();
    }

    @Test
    void aChartsDataTableIsTheChartsNoteAndNotNamed() throws Exception {
        ChartSpec spec = ChartSpec.bar()
                .data(ChartData.builder().categories("Q1", "Q2").series("Sales", 3.0, 5.0).build()).build();
        DocxExportReport report = DocxExports.report(400, 400, 20, page -> page.add(new ChartNode(spec)));
        assertThat(report.bySubject()).containsKey("chart").doesNotContainKey(SUBJECT)
                .as("laid out").doesNotContainKey("measured geometry");
        DocxExportReport composed = DocxExports.report(400, 400, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(300)).rowCells(DocumentTableCell.node(new ChartNode(spec)))));
        assertThat(composed.bySubject()).as("composed in a cell").doesNotContainKey(SUBJECT)
                .as("laid out").doesNotContainKey("measured geometry");
        assertThat(tableNotes(page -> page.add(new ChartNode(spec)).addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.fixed(250))
                .rowCells(DocumentTableCell.text("Breakdown"),
                        DocumentTableCell.node(inner(DocumentTableColumn.auto(), DocumentTableColumn.auto()))))))
                .as("a table after a chart's").extracting(DocxExportReport.Note::detail).containsExactly(COMPOSED);
    }

    @Test
    void withNoLayoutTheSectionsNoteSaysItAndATableIsNotNamed() throws Exception {
        DocxExportReport report = DocxExports.reportWithoutLayout(400, 400, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.fixed(250))
                .rowCells(DocumentTableCell.text("Breakdown"),
                        DocumentTableCell.node(inner(DocumentTableColumn.auto(), DocumentTableColumn.auto())))));
        assertThat(report.bySubject()).doesNotContainKey(SUBJECT).containsKey("measured geometry");
    }

    private static TableNode inner(DocumentTableColumn first, DocumentTableColumn second) {
        return new TableBuilder().columns(first, second).row("Hours", "12").row("Rate", "95.00 per hour").build();
    }

    /** The table notes of an export the layout stands behind: a document it refuses is another case. */
    private static List<DocxExportReport.Note> tableNotes(Consumer<PageFlowBuilder> content) throws Exception {
        DocxExportReport report = DocxExports.report(500, 400, 30, content);
        assertThat(report.bySubject()).as("laid out").doesNotContainKey("measured geometry");
        return report.bySubject().getOrDefault(SUBJECT, List.of());
    }
}
