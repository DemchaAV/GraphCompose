package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.payloads.TableRowFragmentPayload;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.engine.components.content.table.TableCellLayoutStyle;
import com.demcha.compose.engine.components.content.table.TableResolvedCell;
import com.demcha.compose.engine.components.style.Padding;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A table cell carries the height of one line of its text, as the layout measured it.
 *
 * <p>The layout measured it to size the row and then threw it away: the PDF and PPTX
 * renderers measured it again when drawing, and a semantic export — which has no font
 * runtime — could not measure it at all, so Word set a cell at its own spacing for the font.
 * Measured on the probe corpus, a totals row whose style states its face came out 3pt taller
 * in Word than the page draws it.</p>
 *
 * @author Artem Demchyshyn
 */
class TableCellLineHeightTest {

    @Test
    void everyTextCellCarriesTheLineHeightItsRowWasSizedWith() {
        List<TableResolvedCell> cells = resolvedCells(new TableBuilder()
                .name("Billing")
                .autoColumns(2)
                .row("Item", "Amount")
                .totalRow("Total", "2 726.00")
                .build());

        assertThat(cells).isNotEmpty().allSatisfy(cell -> {
            assertThat(cell.hasMeasuredLineHeight()).as(cell.name()).isTrue();
            assertThat(cell.lineHeight()).as(cell.name()).isPositive();
        });
    }

    @Test
    void aLargerFaceMeasuresATallerLine() {
        // Measured per cell from the cell's own resolved text style, not once per table.
        List<TableResolvedCell> cells = resolvedCells(new TableBuilder()
                .name("Sized")
                .autoColumns(1)
                .row("Body")
                .row("Large")
                .rowStyle(1, com.demcha.compose.document.table.DocumentTableStyle.builder()
                        .textStyle(com.demcha.compose.document.style.DocumentTextStyle.builder()
                                .size(20)
                                .build())
                        .build())
                .build());

        double body = cellNamed(cells, "Sized__row_0__cell_0").lineHeight();
        double large = cellNamed(cells, "Sized__row_1__cell_0").lineHeight();
        assertThat(large).isGreaterThan(body);
    }

    @Test
    void aCellBuiltWithoutALineHeightSaysSoRatherThanClaimingZero() {
        // The constructor the record had before stays linkable, and a renderer reading such
        // a cell measures the line itself — what every renderer did before.
        TableResolvedCell cell = new TableResolvedCell("legacy", 0, 10, 10, 0,
                List.of("x"), TableCellLayoutStyle.DEFAULT, Padding.zero(), Set.of());

        assertThat(cell.hasMeasuredLineHeight()).isFalse();
        assertThat(cell.lineHeight()).isNaN();
    }

    @Test
    void aLineHeightThatIsNotAMeasurementIsRefused() {
        assertThatThrownBy(() -> new TableResolvedCell("bad", 0, 10, 10, 0, List.of("x"),
                TableCellLayoutStyle.DEFAULT, Padding.zero(), Set.of(), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lineHeight");
        assertThatThrownBy(() -> new TableResolvedCell("bad", 0, 10, 10, 0, List.of("x"),
                TableCellLayoutStyle.DEFAULT, Padding.zero(), Set.of(), Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lineHeight");
    }

    private static TableResolvedCell cellNamed(List<TableResolvedCell> cells, String name) {
        return cells.stream()
                .filter(cell -> name.equals(cell.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no cell named " + name + " among "
                        + cells.stream().map(TableResolvedCell::name).toList()));
    }

    private static List<TableResolvedCell> resolvedCells(com.demcha.compose.document.node.TableNode table) {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 360)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(table);
            LayoutGraph graph = session.layoutGraph();
            return graph.fragments().stream()
                    .filter(fragment -> fragment.payload() instanceof TableRowFragmentPayload)
                    .flatMap(fragment -> ((TableRowFragmentPayload) fragment.payload()).cells().stream())
                    .toList();
        }
    }
}
