package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.engine.components.content.table.TableResolvedCell;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase D.3 — repeated header on page break. The header rows are
 * re-emitted at the top of every continuation page when the table is
 * split across pages.
 *
 * @author Artem Demchyshyn
 */
class TableBuilderRepeatHeaderTest {

    @Test
    void longTableWithRepeatHeaderEmitsHeaderOnEveryPage() throws Exception {
        // 60 data rows + 1 header on a small page guarantees a
        // multi-page split. Each page must start with a row whose
        // first-cell text is "Item" (the header).
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 200)
                .margin(DocumentInsets.of(20))
                .create()) {

            DocumentTableStyle bordered = DocumentTableStyle.builder()
                    .stroke(DocumentStroke.of(DocumentColor.rgb(180, 188, 200), 0.6))
                    .padding(DocumentInsets.of(6))
                    .build();
            DocumentTableStyle headerStyle = DocumentTableStyle.builder()
                    .fillColor(DocumentColor.rgb(20, 60, 75))
                    .stroke(DocumentStroke.of(DocumentColor.rgb(180, 188, 200), 0.6))
                    .padding(DocumentInsets.of(7))
                    .textStyle(DocumentTextStyle.builder()
                            .decoration(DocumentTextDecoration.BOLD)
                            .color(DocumentColor.WHITE)
                            .build())
                    .build();

            session.dsl().pageFlow()
                    .name("LongTableShowcase")
                    .addTable(table -> {
                        TableBuilder configured = table
                                .name("LongTable")
                                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                                .defaultCellStyle(bordered)
                                .headerRow("Item", "Amount")
                                .headerStyle(headerStyle)
                                .repeatHeader();
                        for (int i = 1; i <= 60; i++) {
                            configured.row("Row " + i, String.format("$%d.00", i));
                        }
                    })
                    .build();

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isGreaterThan(1);

            // Group fragments by page, then assert the FIRST table-row
            // fragment on every page has "Item" in its left cell.
            Map<Integer, List<PlacedFragment>> rowsByPage = new HashMap<>();
            for (PlacedFragment fragment : graph.fragments()) {
                if (fragment.payload() instanceof BuiltInNodeDefinitions.TableRowFragmentPayload) {
                    rowsByPage
                            .computeIfAbsent(fragment.pageIndex(), k -> new ArrayList<>())
                            .add(fragment);
                }
            }
            assertThat(rowsByPage.keySet()).hasSizeGreaterThan(1);

            for (Map.Entry<Integer, List<PlacedFragment>> entry : rowsByPage.entrySet()) {
                List<PlacedFragment> pageRows = entry.getValue();
                BuiltInNodeDefinitions.TableRowFragmentPayload firstRow =
                        (BuiltInNodeDefinitions.TableRowFragmentPayload) pageRows.get(0).payload();
                TableResolvedCell firstCell = firstRow.cells().get(0);
                assertThat(firstCell.lines())
                        .as("page %d should start the table with the repeating header", entry.getKey())
                        .containsExactly("Item");
            }
        }
    }

    @Test
    void multiRowRepeatHeaderRepeatsAllConfiguredLeadingRows() throws Exception {
        // repeatHeader(2) repeats both the title row AND the column
        // header row on every continuation page.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 220)
                .margin(DocumentInsets.of(20))
                .create()) {

            DocumentTableStyle bordered = DocumentTableStyle.builder()
                    .stroke(DocumentStroke.of(DocumentColor.rgb(180, 188, 200), 0.6))
                    .padding(DocumentInsets.of(5))
                    .build();

            session.dsl().pageFlow()
                    .addTable(table -> {
                        TableBuilder configured = table
                                .name("DoubleHeaderTable")
                                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                                .defaultCellStyle(bordered)
                                .row("Quarterly Sales", "")          // index 0 — title row
                                .row("Item", "Amount")               // index 1 — column header
                                .repeatHeader(2);
                        for (int i = 1; i <= 40; i++) {
                            configured.row("Row " + i, String.format("$%d.00", i));
                        }
                    })
                    .build();

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isGreaterThan(1);

            Map<Integer, List<PlacedFragment>> rowsByPage = new HashMap<>();
            for (PlacedFragment fragment : graph.fragments()) {
                if (fragment.payload() instanceof BuiltInNodeDefinitions.TableRowFragmentPayload) {
                    rowsByPage
                            .computeIfAbsent(fragment.pageIndex(), k -> new ArrayList<>())
                            .add(fragment);
                }
            }

            for (Map.Entry<Integer, List<PlacedFragment>> entry : rowsByPage.entrySet()) {
                List<PlacedFragment> pageRows = entry.getValue();
                assertThat(pageRows.size())
                        .as("page %d must have at least the two header rows plus one data row",
                                entry.getKey())
                        .isGreaterThanOrEqualTo(3);
                BuiltInNodeDefinitions.TableRowFragmentPayload row0 =
                        (BuiltInNodeDefinitions.TableRowFragmentPayload) pageRows.get(0).payload();
                BuiltInNodeDefinitions.TableRowFragmentPayload row1 =
                        (BuiltInNodeDefinitions.TableRowFragmentPayload) pageRows.get(1).payload();
                assertThat(row0.cells().get(0).lines())
                        .as("page %d row 0 = title", entry.getKey())
                        .containsExactly("Quarterly Sales");
                assertThat(row1.cells().get(0).lines())
                        .as("page %d row 1 = column header", entry.getKey())
                        .containsExactly("Item");
            }
        }
    }

    @Test
    void repeatHeaderZeroBehavesLikeBeforePhaseD3() throws Exception {
        // No repetition: only the first page shows the header. The
        // second page starts directly with whatever data row the split
        // landed on.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 200)
                .margin(DocumentInsets.of(20))
                .create()) {

            DocumentTableStyle bordered = DocumentTableStyle.builder()
                    .stroke(DocumentStroke.of(DocumentColor.rgb(180, 188, 200), 0.6))
                    .padding(DocumentInsets.of(6))
                    .build();

            session.dsl().pageFlow()
                    .addTable(table -> {
                        TableBuilder configured = table
                                .name("NoRepeatTable")
                                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                                .defaultCellStyle(bordered)
                                .headerRow("Item", "Amount");
                        for (int i = 1; i <= 60; i++) {
                            configured.row("Row " + i, String.format("$%d.00", i));
                        }
                    })
                    .build();

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isGreaterThan(1);

            // Find the FIRST row fragment on page 1 (zero-indexed). It
            // should NOT be "Item" (the header) — it's a data row.
            PlacedFragment firstOnPage1 = graph.fragments().stream()
                    .filter(f -> f.pageIndex() == 1
                            && f.payload() instanceof BuiltInNodeDefinitions.TableRowFragmentPayload)
                    .findFirst()
                    .orElseThrow();
            BuiltInNodeDefinitions.TableRowFragmentPayload payload =
                    (BuiltInNodeDefinitions.TableRowFragmentPayload) firstOnPage1.payload();
            assertThat(payload.cells().get(0).lines())
                    .as("without repeatHeader, page 2 should NOT start with the header")
                    .doesNotContain("Item");
        }
    }
}
