package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.engine.components.content.table.TableResolvedCell;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Phase D.1 — row-span behaviour for the canonical table.
 *
 * @author Artem Demchyshyn
 */
class TableBuilderRowSpanTest {

    private static final double EPS = 1e-3;

    @Test
    void twoByTwoMergedTopLeftSpansBothRowsAndBothColumns() {
        // Row 0: [merged 2x2, "B"]    (the merged cell starts here and
        //                              covers (0,0), (0,1), (1,0), (1,1))
        // Row 1: ["D"]                (only "D" is needed because the
        //                              merged cell already occupies cols 0-1)
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 360)
                .margin(DocumentInsets.of(20))
                .create()) {

            TableNode table = new TableBuilder()
                    .name("MergedTwoByTwo")
                    .columns(
                            DocumentTableColumn.auto(),
                            DocumentTableColumn.auto(),
                            DocumentTableColumn.auto())
                    .rowCells(
                            DocumentTableCell.text("Merged").colSpan(2).rowSpan(2),
                            DocumentTableCell.text("B"))
                    .rowCells(
                            DocumentTableCell.text("D"))
                    .build();
            session.add(table);

            LayoutGraph graph = session.layoutGraph();
            List<List<TableResolvedCell>> rows = collectRowFragments(graph);

            assertThat(rows).hasSize(2);
            assertThat(rows.get(0)).hasSize(2);
            assertThat(rows.get(1)).hasSize(1);

            TableResolvedCell merged = rows.get(0).get(0);
            TableResolvedCell topRight = rows.get(0).get(1);
            TableResolvedCell bottomRight = rows.get(1).get(0);

            // The merged cell starts at x=0 (leftmost) and is wider than
            // the single-column "B" cell because it covers two source
            // columns. Exact widths depend on natural-width distribution
            // across auto columns — we assert the structural ordering
            // rather than a precise arithmetic equality.
            assertThat(merged.x()).isEqualTo(0.0, within(EPS));
            assertThat(merged.width()).isGreaterThan(topRight.width());
            // The merged cell height covers two source rows — its height
            // equals row0.height + row1.height. The top-right and
            // bottom-right cells each occupy a single row.
            assertThat(merged.height())
                    .as("merged cell height = sum of spanned row heights")
                    .isEqualTo(topRight.height() + bottomRight.height(), within(EPS));
            // The right-edge column starts AFTER the merged cell ends —
            // top-right and bottom-right share the same x coordinate.
            assertThat(bottomRight.x()).isEqualTo(topRight.x(), within(EPS));
            assertThat(topRight.x())
                    .as("the right column starts where the merged cell ends")
                    .isEqualTo(merged.x() + merged.width(), within(EPS));

            // yOffset shifts the spanning cell's bottom edge DOWNWARD in
            // PDF coords (negative offset). For a 2-row span the offset
            // equals -row1.height — that's exactly enough to put the
            // cell's bottom at row 1's bottom instead of row 0's bottom.
            // Single-row cells stay at yOffset = 0 so they remain flush
            // with the row fragment's bottom.
            assertThat(merged.yOffset())
                    .as("merged cell bottom shifts down by row 1's height")
                    .isEqualTo(-bottomRight.height(), within(EPS));
            assertThat(topRight.yOffset()).isZero();
            assertThat(bottomRight.yOffset()).isZero();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void rowSpanColumnIsSkippedInSubsequentSourceRows() {
        // Row 0: ["A", merged rowSpan=3, "C"]   (left column "A", middle
        //                                        spans rows 0..2, right "C")
        // Row 1: ["A1", "C1"]                   (only 2 cells — middle col
        //                                        is occupied by the spanning cell)
        // Row 2: ["A2", "C2"]
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 360)
                .margin(DocumentInsets.of(20))
                .create()) {

            TableNode table = new TableBuilder()
                    .name("MiddleColumnSpan")
                    .columns(
                            DocumentTableColumn.auto(),
                            DocumentTableColumn.auto(),
                            DocumentTableColumn.auto())
                    .rowCells(
                            DocumentTableCell.text("A"),
                            DocumentTableCell.text("Tall").rowSpan(3),
                            DocumentTableCell.text("C"))
                    .rowCells(
                            DocumentTableCell.text("A1"),
                            DocumentTableCell.text("C1"))
                    .rowCells(
                            DocumentTableCell.text("A2"),
                            DocumentTableCell.text("C2"))
                    .build();
            session.add(table);

            LayoutGraph graph = session.layoutGraph();
            List<List<TableResolvedCell>> rows = collectRowFragments(graph);

            assertThat(rows).hasSize(3);
            // Row 0 has all 3 cells (A, Tall, C); rows 1 and 2 have only
            // the left and right cells (middle is covered by the span).
            assertThat(rows.get(0)).hasSize(3);
            assertThat(rows.get(1)).hasSize(2);
            assertThat(rows.get(2)).hasSize(2);

            TableResolvedCell tall = rows.get(0).get(1);
            // The tall cell's height is the sum of all three row heights.
            double row0H = rows.get(0).get(0).height();
            double row1H = rows.get(1).get(0).height();
            double row2H = rows.get(2).get(0).height();
            assertThat(tall.height()).isEqualTo(row0H + row1H + row2H, within(EPS));
            // yOffset shifts the tall cell's bottom edge DOWNWARD by
            // (row1 + row2) heights so it lands at row 2's bottom in
            // PDF coordinates instead of overflowing above row 0.
            assertThat(tall.yOffset())
                    .as("3-row span shifts bottom down by row1 + row2 heights")
                    .isEqualTo(-(row1H + row2H), within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void rowSpanThatExceedsRemainingRowsIsRejectedAtPrepare() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 200)
                .margin(DocumentInsets.of(20))
                .create()) {

            // Two rows total; a rowSpan of 3 starting at row 0 has only
            // 2 rows of room. The cell-grid pre-pass must reject this
            // with a precise diagnostic.
            TableNode table = new TableBuilder()
                    .name("OverflowingRowSpan")
                    .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                    .rowCells(
                            DocumentTableCell.text("Bad").rowSpan(3),
                            DocumentTableCell.text("B"))
                    .rowCells(
                            DocumentTableCell.text("D"))
                    .build();
            session.add(table);

            assertThatThrownBy(session::layoutGraph)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("rowSpan 3")
                    .hasMessageContaining("only 2 rows remain");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void overlappingRowSpansAreRejectedAtPrepare() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 360)
                .margin(DocumentInsets.of(20))
                .create()) {

            // The first row's "Tall" spans col 0 across 2 rows. The
            // second row author then puts an extra cell in col 0 — that
            // overlaps the span. The pre-pass surfaces a precise error
            // pointing at the overlap rather than silently producing a
            // corrupted grid.
            TableNode table = new TableBuilder()
                    .name("OverlapRowSpan")
                    .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                    .rowCells(
                            DocumentTableCell.text("Tall").rowSpan(2),
                            DocumentTableCell.text("B"))
                    .rowCells(
                            DocumentTableCell.text("Overlap"),
                            DocumentTableCell.text("D"))
                    .build();
            session.add(table);

            assertThatThrownBy(session::layoutGraph)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Row 1")
                    .hasMessageContaining("extra source cell");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<List<TableResolvedCell>> collectRowFragments(LayoutGraph graph) {
        java.util.List<List<TableResolvedCell>> rows = new java.util.ArrayList<>();
        for (PlacedFragment fragment : graph.fragments()) {
            if (fragment.payload() instanceof BuiltInNodeDefinitions.TableRowFragmentPayload payload) {
                rows.add(payload.cells());
            }
        }
        return rows;
    }
}
