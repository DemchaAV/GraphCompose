package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A note on what a table's cell is composed of names it under the table, by its cell: the layout
 * lays it out under the table and gives it no path of its own, and a note with none told neither
 * the node nor its cell.
 */
class DocxComposedCellPathTest {

    @Test
    void aNoteOnWhatACellIsComposedOfNamesItUnderItsTableByItsCell() throws Exception {
        DocxExportReport report = DocxExports.report(400, 600, 20, page -> page.addTable(table -> table
                .columns(DocumentTableColumn.fixed(120), DocumentTableColumn.fixed(120))
                .rowCells(DocumentTableCell.text("Shift"), DocumentTableCell.text("Hours"))
                .rowCells(DocumentTableCell.text("Monday"), DocumentTableCell.node(chip("Morning")))));

        assertThat(cornerPaths(report)).containsExactly("ContainerNode[0]/TableNode[0]/row[1]/cell[1]/Morning[0]");
    }

    @Test
    void twoCellsComposedAlikeAreToldApart() throws Exception {
        DocxExportReport report = DocxExports.report(400, 600, 20, page -> page.addTable(table -> table
                .columns(DocumentTableColumn.fixed(120), DocumentTableColumn.fixed(120))
                .rowCells(DocumentTableCell.node(chip("Chip")), DocumentTableCell.node(chip("Chip")))));

        assertThat(cornerPaths(report)).containsExactly("ContainerNode[0]/TableNode[0]/row[0]/cell[0]/Chip[0]",
                "ContainerNode[0]/TableNode[0]/row[0]/cell[1]/Chip[0]");
    }

    @Test
    void aNoteOnWhatATableComposedInACellHoldsNamesBothCells() throws Exception {
        DocxExportReport report = DocxExports.report(400, 600, 20, page -> page.addTable(table -> table
                .columns(DocumentTableColumn.fixed(240))
                .rowCells(DocumentTableCell.node(new TableBuilder().name("Shifts").columns(DocumentTableColumn.fixed(200))
                        .row("Monday").rowCells(DocumentTableCell.node(chip("Morning"))).build()))));

        assertThat(cornerPaths(report)).containsExactly(
                "ContainerNode[0]/TableNode[0]/row[0]/cell[0]/Shifts[0]/row[1]/cell[0]/Morning[0]");
    }

    @Test
    void aComposedNodeStillFindsNoPlacementOrFragmentsOfItsOwn() throws Exception {
        // Its path is the report's alone: found by it, the export would read the layout differently.
        DocumentNode chip = chip("Chip");
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(page -> page.addTable(table -> table.columns(DocumentTableColumn.fixed(120))
                    .rowCells(DocumentTableCell.node(chip))));
            DocxLayoutMetrics layout = DocxLayoutMetrics.of(session.documentGraph(), session.layoutGraph());

            assertThat(layout.pathOf(chip)).isEqualTo("ContainerNode[0]/TableNode[0]/row[0]/cell[0]/Chip[0]");
            assertThat(layout.placement(chip)).isNull();
            assertThat(layout.ownFragments(chip)).isEmpty();
        }
    }

    @Test
    void aNodeInTheFlowAndInACellKeepsTheFlowsPathAndPlacement() throws Exception {
        DocumentNode shared = new ParagraphBuilder().name("Shared").text("Both").build();
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(page -> page.add(shared).addTable(table -> table.columns(DocumentTableColumn.fixed(120))
                    .rowCells(DocumentTableCell.node(shared))));
            DocxLayoutMetrics layout = DocxLayoutMetrics.of(session.documentGraph(), session.layoutGraph());

            assertThat(layout.pathOf(shared)).isEqualTo("ContainerNode[0]/Shared[0]");
            assertThat(layout.placement(shared)).isNotNull();
        }
        DocumentNode table = new TableBuilder().columns(DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.node(shared)).build();
        assertThat(pathOf(new SectionBuilder().add(table).add(shared).build(), shared))
                .as("the flow's, coming after the table").isEqualTo("SectionNode[0]/Shared[1]");
    }

    @Test
    void aTableInTheFlowAndInAnotherTablesCellNamesWhatItHoldsUnderItsFlowPath() {
        DocumentNode chip = chip("Chip");
        DocumentNode inner = new TableBuilder().name("Inner").columns(DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.node(chip)).build();
        DocumentNode outer = new TableBuilder().name("Outer").columns(DocumentTableColumn.fixed(200))
                .rowCells(DocumentTableCell.node(inner)).build();
        DocumentNode root = new SectionBuilder().add(inner).add(outer).build();

        assertThat(pathOf(root, inner)).isEqualTo("SectionNode[0]/Inner[0]");
        assertThat(pathOf(root, chip)).isEqualTo("SectionNode[0]/Inner[0]/row[0]/cell[0]/Chip[0]");
    }

    @Test
    void aCellIsNamedByItsPlaceAmongTheCellsAuthoredForItsRow() {
        // A cell spanning columns before it, or rows into its row from above, puts it in a later column.
        DocumentNode behindASpan = chip("BehindASpan");
        DocumentNode underASpan = chip("UnderASpan");
        DocumentNode root = new TableBuilder().name("Grid")
                .columns(DocumentTableColumn.fixed(50), DocumentTableColumn.fixed(50), DocumentTableColumn.fixed(50))
                .rowCells(DocumentTableCell.text("Wide").colSpan(2), DocumentTableCell.node(behindASpan))
                .rowCells(DocumentTableCell.text("Tall").rowSpan(2), DocumentTableCell.text("B"),
                        DocumentTableCell.text("C"))
                .rowCells(DocumentTableCell.node(underASpan), DocumentTableCell.text("F"))
                .build();

        assertThat(pathOf(root, behindASpan)).as("the second cell, in the third column")
                .isEqualTo("Grid[0]/row[0]/cell[1]/BehindASpan[0]");
        assertThat(pathOf(root, underASpan)).as("the first cell authored, in the second column")
                .isEqualTo("Grid[0]/row[2]/cell[0]/UnderASpan[0]");
    }

    @Test
    void aNodeAtMoreThanOnePlaceIsNamedByTheLastAsInTheFlow() {
        DocumentNode chip = chip("Chip");
        DocumentNode twice = new TableBuilder().name("Twice").columns(DocumentTableColumn.fixed(50),
                DocumentTableColumn.fixed(50)).rowCells(DocumentTableCell.node(chip), DocumentTableCell.node(chip))
                .build();
        DocumentNode inner = chip("Inner");
        DocumentNode reused = new TableBuilder().columns(DocumentTableColumn.fixed(50))
                .rowCells(DocumentTableCell.node(inner)).build();
        DocumentNode root = new SectionBuilder().add(reused).add(reused).build();

        assertThat(pathOf(twice, chip)).isEqualTo("Twice[0]/row[0]/cell[1]/Chip[0]");
        assertThat(pathOf(root, reused)).isEqualTo("SectionNode[0]/TableNode[1]");
        assertThat(pathOf(root, inner)).as("under the table's own path")
                .isEqualTo("SectionNode[0]/TableNode[1]/row[0]/cell[0]/Inner[0]");
    }

    /** A node's path in a document of one root, with no layout behind it. */
    private static String pathOf(DocumentNode root, DocumentNode node) {
        return DocxLayoutMetrics.of(new DocumentGraph(List.of(root)), null).pathOf(node);
    }

    private static List<String> cornerPaths(DocxExportReport report) {
        return report.notes().stream().filter(note -> note.subject().equals("corner radius"))
                .map(DocxExportReport.Note::path).toList();
    }

    private static DocumentNode chip(String name) {
        return new ShapeContainerBuilder()
                .name(name)
                .roundedRect(60, 14, 7)
                .fillColor(DocumentColor.rgb(26, 86, 148))
                .center(new ParagraphBuilder().text("09:00").build())
                .build();
    }
}
