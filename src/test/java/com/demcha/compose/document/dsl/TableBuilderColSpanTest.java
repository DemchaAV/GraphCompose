package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions.TableRowFragmentPayload;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.engine.components.content.table.TableResolvedCell;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class TableBuilderColSpanTest {

    private static final double EPS = 1e-6;

    @Test
    void colSpanMethodReturnsCellWithRequestedSpan() {
        DocumentTableCell base = DocumentTableCell.text("Total");
        assertThat(base.colSpan()).isEqualTo(1);

        DocumentTableCell spanned = base.colSpan(3);
        assertThat(spanned.colSpan()).isEqualTo(3);
        assertThat(spanned.lines()).isEqualTo(base.lines());
        assertThat(spanned.style()).isEqualTo(base.style());
    }

    @Test
    void cellRecordRejectsZeroOrNegativeColSpan() {
        assertThatThrownBy(() -> DocumentTableCell.text("X").colSpan(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("colSpan");

        assertThatThrownBy(() -> DocumentTableCell.text("X").colSpan(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("colSpan");
    }

    @Test
    void totalsRowCollapsesIntoSpannedLabelAndAmountCellViaDsl() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 360)
                .margin(DocumentInsets.of(20))
                .create()) {

            TableNode table = new TableBuilder()
                    .name("Totals")
                    .columns(
                            DocumentTableColumn.fixed(120),
                            DocumentTableColumn.fixed(80),
                            DocumentTableColumn.fixed(80),
                            DocumentTableColumn.fixed(100))
                    .row("Item", "Qty", "Unit", "Amount")
                    .row("Coffee beans", "12", "$15.00", "$180.00")
                    .row("Filters", "4", "$5.00", "$20.00")
                    .rowCells(
                            DocumentTableCell.text("Total")
                                    .colSpan(3)
                                    .withStyle(DocumentTableStyle.builder().build()),
                            DocumentTableCell.text("$200.00"))
                    .build();

            session.add(table);
            LayoutGraph graph = session.layoutGraph();

            List<PlacedFragment> rowFragments = graph.fragments().stream()
                    .filter(fragment -> fragment.payload() instanceof TableRowFragmentPayload)
                    .toList();

            assertThat(rowFragments).hasSize(4);

            TableRowFragmentPayload totals = (TableRowFragmentPayload) rowFragments.get(3).payload();
            assertThat(totals.cells()).hasSize(2);

            TableResolvedCell label = totals.cells().get(0);
            assertThat(label.x()).isEqualTo(0.0, within(EPS));
            assertThat(label.width()).isEqualTo(120.0 + 80.0 + 80.0, within(EPS));

            TableResolvedCell amount = totals.cells().get(1);
            assertThat(amount.x()).isEqualTo(120.0 + 80.0 + 80.0, within(EPS));
            assertThat(amount.width()).isEqualTo(100.0, within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void headerGroupingProducesThreeCellsAcrossFourColumns() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(540, 320)
                .margin(DocumentInsets.of(20))
                .create()) {

            TableNode table = new TableBuilder()
                    .name("HeaderGroup")
                    .columns(
                            DocumentTableColumn.fixed(120),
                            DocumentTableColumn.fixed(90),
                            DocumentTableColumn.fixed(90),
                            DocumentTableColumn.fixed(120))
                    .rowCells(
                            DocumentTableCell.text("Region"),
                            DocumentTableCell.text("Sales").colSpan(2),
                            DocumentTableCell.text("Owner"))
                    .row("North", "Q1: $10k", "Q2: $14k", "Mark")
                    .row("South", "Q1: $8k", "Q2: $11k", "Anna")
                    .build();

            session.add(table);
            LayoutGraph graph = session.layoutGraph();

            List<PlacedFragment> rowFragments = graph.fragments().stream()
                    .filter(fragment -> fragment.payload() instanceof TableRowFragmentPayload)
                    .toList();

            assertThat(rowFragments).hasSize(3);

            TableRowFragmentPayload header = (TableRowFragmentPayload) rowFragments.get(0).payload();
            assertThat(header.cells()).hasSize(3);
            assertThat(header.cells().get(0).width()).isEqualTo(120.0, within(EPS));
            assertThat(header.cells().get(1).x()).isEqualTo(120.0, within(EPS));
            assertThat(header.cells().get(1).width()).isEqualTo(90.0 + 90.0, within(EPS));
            assertThat(header.cells().get(2).x()).isEqualTo(120.0 + 90.0 + 90.0, within(EPS));
            assertThat(header.cells().get(2).width()).isEqualTo(120.0, within(EPS));

            TableRowFragmentPayload dataRow = (TableRowFragmentPayload) rowFragments.get(1).payload();
            assertThat(dataRow.cells()).hasSize(4);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void rowWithMismatchedColSpanSumIsRejectedDuringLayout() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 360)
                .margin(DocumentInsets.of(20))
                .create()) {

            TableNode table = new TableBuilder()
                    .name("BrokenSpan")
                    .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto(), DocumentTableColumn.auto())
                    .row("A", "B", "C")
                    .rowCells(
                            DocumentTableCell.text("Wrong").colSpan(2),
                            DocumentTableCell.text("X"),
                            DocumentTableCell.text("Y"))
                    .build();

            session.add(table);

            // The cell-grid pre-pass (D.1) replaced the dedicated
            // "colSpan sum" check with a more precise diagnostic that
            // names the offending row index and surplus cell count.
            // Either reading is fine — both messages identify the same
            // root cause.
            assertThatThrownBy(session::layoutGraph)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Row 1")
                    .hasMessageContaining("extra source cell");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
