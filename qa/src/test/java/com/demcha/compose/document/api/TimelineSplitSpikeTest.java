package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableCell;
import org.junit.jupiter.api.Test;

/**
 * PHASE 0b SPIKE — throwaway. Delete or promote before the PR.
 *
 * <p>The plan blocks on one question: is there a {@code leading | axis | content}
 * structure whose content can continue onto the next page? {@code RowNode} cannot —
 * {@code RowDefinition} is ATOMIC and rows refuse to nest. {@code TableDefinition} is
 * the only SPLITTABLE horizontal container, and {@code RowBuilder} rejects tables
 * precisely because "tables are splittable and would conflict with the row's atomic
 * pagination".</p>
 *
 * <p>So: build the three columns as a table, force a page break through it, and answer
 * two things — does it actually split, and does a marker sitting in a cell still appear
 * as a {@code PlacedNode} the rail pass could anchor to?</p>
 */
class TimelineSplitSpikeTest {

    private static final DocumentColor INK = DocumentColor.rgb(20, 60, 160);

    @Test
    void doesAThreeColumnTableSplitAndKeepMarkersVisible() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow()
                    .addTable(t -> {
                        t.columns(
                                DocumentTableColumn.fixed(80),
                                DocumentTableColumn.fixed(24),
                                DocumentTableColumn.fixed(120));
                        for (int i = 1; i <= 14; i++) {
                            t.rowCells(
                                    DocumentTableCell.text("2024-0" + i),
                                    new DocumentTableCell(java.util.List.of(), null, 1, 1,
                                            new EllipseNode("marker" + i, 8, 8, INK, null, null, null, null, null)),
                                    DocumentTableCell.lines("Entry " + i, "second line"));
                        }
                    })
                    .build();

            LayoutGraph graph = session.layoutGraph();
            System.out.println("SPIKE0B pages=" + graph.totalPages());

            for (PlacedNode n : graph.nodes()) {
                if ("Ellipse".equals(n.nodeKind()) || n.nodeKind().contains("Ellipse")
                        || "Table".equals(n.nodeKind()) || n.nodeKind().contains("Table")) {
                    System.out.printf("SPIKE0B node %-14s name=%-10s box=(%7.2f,%7.2f %6.2fx%6.2f) p%d..%d%n",
                            n.nodeKind(), n.semanticName(),
                            n.placementX(), n.placementY(), n.placementWidth(), n.placementHeight(),
                            n.startPage(), n.endPage());
                }
            }

            long ellipseFragments = 0;
            for (PlacedFragment f : graph.fragments()) {
                String p = f.payload() == null ? "" : f.payload().getClass().getSimpleName();
                if (p.contains("Ellipse")) {
                    ellipseFragments++;
                    System.out.printf("SPIKE0B ellipseFragment page=%d box=(%7.2f,%7.2f %6.2fx%6.2f)%n",
                            f.pageIndex(), f.x(), f.y(), f.width(), f.height());
                }
            }
            System.out.println("SPIKE0B ellipseFragments=" + ellipseFragments);
        }
    }
}
