package com.demcha.compose.document.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.TableRowFragmentPayload;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guards the table-pagination slice invariant behind Finding 10. When a table
 * spans many pages the engine re-slices the shrinking tail page-by-page — a
 * sub-list of a sub-list of … the resolved layout. The body-only slice reuses
 * those immutable sub-list views instead of re-copying them, so this test pins
 * the contract that survives the optimization: every body row still lands on
 * exactly one page, in order, no matter how many times the tail is re-sliced.
 */
class TableSlicePaginationTest {

    @Test
    void tableSpanningManyPagesPlacesEveryRowOnExactlyOnePage() {
        int rowCount = 120;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(24, 24, 24, 24)
                .create()) {
            session.pageFlow(flow -> flow.addTable(table -> {
                table.autoColumns(3);
                for (int i = 0; i < rowCount; i++) {
                    table.row("Row " + i, "Col B " + i, "Col C " + i);
                }
            }));

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages())
                    .as("120 rows at A4 should span several pages")
                    .isGreaterThanOrEqualTo(3);

            List<PlacedFragment> rowFragments = graph.fragments().stream()
                    .filter(fragment -> fragment.payload() instanceof TableRowFragmentPayload)
                    .toList();

            // Exactly one fragment per body row — the chained tail re-slices
            // neither drop nor duplicate a row.
            assertThat(rowFragments).hasSize(rowCount);

            // …and the rows are spread across every page, not collapsed onto one.
            assertThat(rowFragments.stream().map(PlacedFragment::pageIndex).distinct().count())
                    .isEqualTo(graph.totalPages());
        }
    }
}
