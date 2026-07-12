package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase D.2 — zebra rows, totals row, header row alias on
 * {@link TableBuilder}.
 *
 * @author Artem Demchyshyn
 */
class TableBuilderZebraAndTotalsTest {

    private static final DocumentColor ZEBRA_ODD = DocumentColor.rgb(244, 247, 252);
    private static final DocumentColor ZEBRA_EVEN = DocumentColor.WHITE;

    @Test
    void zebraColorsAreAppliedToRowsByParity() {
        TableNode table = new TableBuilder()
                .name("Zebra")
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .row("R0", "x")
                .row("R1", "x")
                .row("R2", "x")
                .row("R3", "x")
                .zebra(ZEBRA_ODD, ZEBRA_EVEN)
                .build();

        Map<Integer, DocumentTableStyle> rowStyles = table.rowStyles();
        assertThat(rowStyles).containsOnlyKeys(0, 1, 2, 3);
        assertThat(rowStyles.get(0).fillColor()).isEqualTo(ZEBRA_ODD);
        assertThat(rowStyles.get(1).fillColor()).isEqualTo(ZEBRA_EVEN);
        assertThat(rowStyles.get(2).fillColor()).isEqualTo(ZEBRA_ODD);
        assertThat(rowStyles.get(3).fillColor()).isEqualTo(ZEBRA_EVEN);
    }

    @Test
    void zebraDoesNotOverrideExplicitRowStyle() {
        DocumentTableStyle headerStyle = DocumentTableStyle.builder()
                .fillColor(DocumentColor.rgb(20, 60, 75))
                .build();

        TableNode table = new TableBuilder()
                .name("ZebraWithHeader")
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .header("Col A", "Col B")
                .row("R1", "x")
                .row("R2", "x")
                .headerStyle(headerStyle)
                .zebra(ZEBRA_ODD, ZEBRA_EVEN)
                .build();

        Map<Integer, DocumentTableStyle> rowStyles = table.rowStyles();
        // Header style at index 0 wins over zebra-odd.
        assertThat(rowStyles.get(0)).isSameAs(headerStyle);
        // Data rows below pick up zebra parity from their own indices.
        assertThat(rowStyles.get(1).fillColor()).isEqualTo(ZEBRA_EVEN);
        assertThat(rowStyles.get(2).fillColor()).isEqualTo(ZEBRA_ODD);
    }

    @Test
    void zebraSkipsParityWhenStyleIsNull() {
        // Only paint odd rows; even rows stay un-styled.
        TableNode table = new TableBuilder()
                .name("StripeOddOnly")
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .row("R0", "x")
                .row("R1", "x")
                .row("R2", "x")
                .zebra(ZEBRA_ODD, null)
                .build();

        Map<Integer, DocumentTableStyle> rowStyles = table.rowStyles();
        assertThat(rowStyles).containsOnlyKeys(0, 2);
        assertThat(rowStyles.get(0).fillColor()).isEqualTo(ZEBRA_ODD);
        assertThat(rowStyles.get(2).fillColor()).isEqualTo(ZEBRA_ODD);
    }

    @Test
    void totalRowAddsRowAndAssignsBoldFilledStyleByDefault() {
        TableNode table = new TableBuilder()
                .name("WithTotals")
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .header("Item", "Amount")
                .row("Apples", "5")
                .row("Pears", "7")
                .totalRow("Total", "12")
                .build();

        // Header + 2 data + totals = 4 rows.
        assertThat(table.rows()).hasSize(4);
        assertThat(table.rows().get(3).get(0).lines()).containsExactly("Total");
        assertThat(table.rows().get(3).get(1).lines()).containsExactly("12");

        DocumentTableStyle totalStyle = table.rowStyles().get(3);
        assertThat(totalStyle).isNotNull();
        assertThat(totalStyle.fillColor().color()).isEqualTo(DocumentColor.rgb(240, 240, 245).color());
        assertThat(totalStyle.textStyle()).isNotNull();
        assertThat(totalStyle.textStyle().decoration()).isEqualTo(DocumentTextDecoration.BOLD);
    }

    @Test
    void totalRowOverridesZebraOnTheLastRowIndex() {
        TableNode table = new TableBuilder()
                .name("ZebraTotalConflict")
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .row("R0", "x")
                .row("R1", "x")
                .totalRow("Total", "12")  // index 2 → would otherwise be zebra-odd
                .zebra(ZEBRA_ODD, ZEBRA_EVEN)
                .build();

        DocumentTableStyle row2Style = table.rowStyles().get(2);
        // The totals style (gray-blue fill, bold text) wins over the
        // zebra-odd style (light blue fill, no decoration).
        assertThat(row2Style.fillColor().color()).isEqualTo(DocumentColor.rgb(240, 240, 245).color());
        assertThat(row2Style.textStyle().decoration()).isEqualTo(DocumentTextDecoration.BOLD);
        // Other rows still get their zebra parity (compare AWT colors —
        // DocumentColor uses reference equality).
        assertThat(table.rowStyles().get(0).fillColor()).isSameAs(ZEBRA_ODD);
        assertThat(table.rowStyles().get(1).fillColor()).isSameAs(ZEBRA_EVEN);
    }

    @Test
    void totalRowTwoArgOverloadAcceptsCustomStyle() {
        DocumentTableStyle customTotal = DocumentTableStyle.builder()
                .fillColor(DocumentColor.rgb(255, 240, 220))
                .textStyle(DocumentTextStyle.builder()
                        .decoration(DocumentTextDecoration.BOLD)
                        .build())
                .build();

        TableNode table = new TableBuilder()
                .name("CustomTotals")
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .row("A", "1")
                .row("B", "2")
                .totalRow(customTotal, "Total", "3")
                .build();

        assertThat(table.rowStyles().get(2)).isSameAs(customTotal);
    }

    @Test
    void headerRowIsAnAliasForHeader() {
        TableNode aliased = new TableBuilder()
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .headerRow("Col A", "Col B")
                .row("v1", "v2")
                .build();

        TableNode reference = new TableBuilder()
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .header("Col A", "Col B")
                .row("v1", "v2")
                .build();

        assertThat(aliased.rows()).isEqualTo(reference.rows());
    }
}
