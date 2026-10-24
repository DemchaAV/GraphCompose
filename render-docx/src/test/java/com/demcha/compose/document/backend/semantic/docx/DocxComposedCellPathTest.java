package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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

        assertThat(report.notes()).filteredOn(note -> note.subject().equals("corner radius"))
                .singleElement().satisfies(note ->
                        assertThat(note.path()).isEqualTo("ContainerNode[0]/TableNode[0]/row[1]/cell[1]/Morning[0]"));
    }

    @Test
    void twoCellsComposedAlikeAreToldApart() throws Exception {
        DocxExportReport report = DocxExports.report(400, 600, 20, page -> page.addTable(table -> table
                .columns(DocumentTableColumn.fixed(120), DocumentTableColumn.fixed(120))
                .rowCells(DocumentTableCell.node(chip("Chip")), DocumentTableCell.node(chip("Chip")))));

        assertThat(report.notes()).filteredOn(note -> note.subject().equals("corner radius"))
                .extracting(DocxExportReport.Note::path)
                .containsExactly("ContainerNode[0]/TableNode[0]/row[0]/cell[0]/Chip[0]",
                        "ContainerNode[0]/TableNode[0]/row[0]/cell[1]/Chip[0]");
    }

    @Test
    void aTableComposedInACellNamesItsOwnCellsUnderItsCell() {
        DocumentNode chip = chip("Chip");
        DocumentNode inner = new TableBuilder().columns(DocumentTableColumn.fixed(100))
                .row("Top").rowCells(DocumentTableCell.node(chip)).build();
        DocumentNode outer = new TableBuilder().name("Outer").columns(DocumentTableColumn.fixed(200))
                .rowCells(DocumentTableCell.node(inner)).build();

        Map<DocumentNode, String> paths = DocxLayoutMetrics.pathsWithin(outer);

        assertThat(paths.get(inner)).isEqualTo("Outer[0]/row[0]/cell[0]/TableNode[0]");
        assertThat(paths.get(chip)).isEqualTo("Outer[0]/row[0]/cell[0]/TableNode[0]/row[1]/cell[0]/Chip[0]");
        assertThat(paths.values().stream().toList()).as("what the chip holds, under it").contains(
                "Outer[0]/row[0]/cell[0]/TableNode[0]/row[1]/cell[0]/Chip[0]/ParagraphNode[0]");
    }

    @Test
    void aNodeThatStandsInTheFlowTooKeepsTheFlowsPath() {
        // The layout knows the flow's path: a node found by the cell's would find no placement.
        DocumentNode shared = new ParagraphBuilder().name("Shared").text("Both").build();
        DocumentNode table = new TableBuilder().columns(DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.node(shared)).build();

        Map<DocumentNode, String> before = DocxLayoutMetrics.pathsWithin(new SectionBuilder()
                .add(shared).add(table).build());
        Map<DocumentNode, String> after = DocxLayoutMetrics.pathsWithin(new SectionBuilder()
                .add(table).add(shared).build());

        assertThat(before.get(shared)).isEqualTo("SectionNode[0]/Shared[0]");
        assertThat(after.get(shared)).isEqualTo("SectionNode[0]/Shared[1]");
        assertThat(List.of(before.get(table), after.get(table)))
                .containsExactly("SectionNode[0]/TableNode[1]", "SectionNode[0]/TableNode[0]");
    }

    @Test
    void aComposedNodeStillFindsNoPlacementOrFragmentsOfItsOwn() throws Exception {
        // Its path is the report's alone: found by it, the export would read the layout differently.
        DocumentNode chip = chip("Chip");
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(com.demcha.compose.document.style.DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addTable(table -> table.columns(DocumentTableColumn.fixed(120))
                    .rowCells(DocumentTableCell.node(chip))));
            DocxLayoutMetrics layout = DocxLayoutMetrics.of(session.documentGraph(), session.layoutGraph());

            assertThat(layout.pathOf(chip)).isEqualTo("ContainerNode[0]/TableNode[0]/row[0]/cell[0]/Chip[0]");
            assertThat(layout.placement(chip)).isNull();
            assertThat(layout.ownFragments(chip)).isEmpty();
        }
    }

    @Test
    void aNodeInTheFlowAndInACellKeepsTheFlowsPlacement() throws Exception {
        DocumentNode shared = new ParagraphBuilder().name("Shared").text("Both").build();
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(com.demcha.compose.document.style.DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.add(shared).addTable(table -> table.columns(DocumentTableColumn.fixed(120))
                    .rowCells(DocumentTableCell.node(shared))));
            DocxLayoutMetrics layout = DocxLayoutMetrics.of(session.documentGraph(), session.layoutGraph());

            assertThat(layout.pathOf(shared)).isEqualTo("ContainerNode[0]/Shared[0]");
            assertThat(layout.placement(shared)).isNotNull();
        }
    }

    @Test
    void aCellAfterOneSpanningColumnsIsNamedByItsPlaceAmongTheRowsCells() {
        DocumentNode chip = chip("Chip");
        DocumentNode table = new TableBuilder().name("Grid")
                .columns(DocumentTableColumn.fixed(50), DocumentTableColumn.fixed(50), DocumentTableColumn.fixed(50))
                .rowCells(DocumentTableCell.text("Wide").colSpan(2), DocumentTableCell.node(chip)).build();

        assertThat(DocxLayoutMetrics.pathsWithin(table).get(chip)).as("the second cell, in the third column")
                .isEqualTo("Grid[0]/row[0]/cell[1]/Chip[0]");
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

        assertThat(DocxLayoutMetrics.pathsWithin(twice).get(chip)).isEqualTo("Twice[0]/row[0]/cell[1]/Chip[0]");
        Map<DocumentNode, String> paths = DocxLayoutMetrics.pathsWithin(new SectionBuilder()
                .add(reused).add(reused).build());
        assertThat(paths.get(reused)).isEqualTo("SectionNode[0]/TableNode[1]");
        assertThat(paths.get(inner)).as("under the table's own path").isEqualTo(
                "SectionNode[0]/TableNode[1]/row[0]/cell[0]/Inner[0]");
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
