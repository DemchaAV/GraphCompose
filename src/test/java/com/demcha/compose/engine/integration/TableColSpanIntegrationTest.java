package com.demcha.compose.engine.integration;

import com.demcha.compose.engine.components.content.shape.Side;
import com.demcha.compose.engine.components.content.table.TableCellContent;
import com.demcha.compose.engine.components.content.table.TableCellLayoutStyle;
import com.demcha.compose.engine.components.content.table.TableColumnLayout;
import com.demcha.compose.engine.components.content.table.TableLayoutData;
import com.demcha.compose.engine.components.content.table.TableResolvedCell;
import com.demcha.compose.engine.components.content.table.TableRowData;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.style.ComponentColor;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.testsupport.EngineComposerHarness;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class TableColSpanIntegrationTest {

    private static final double EPS = 1e-6;

    @Test
    void shouldCollapseTotalsRowIntoSpannedLabelAndValue() throws Exception {
        Path output = VisualTestOutputs.preparePdf("table_colspan_totals", "clean", "integration");

        try (EngineComposerHarness composer = EngineComposerHarness.pdf(output)
                .pageSize(PDRectangle.A4)
                .margin(24, 24, 24, 24)
                .guideLines(false)
                .create()) {

            Entity table = composer.componentBuilder()
                    .table()
                    .entityName("ColSpanTotals")
                    .anchor(Anchor.topCenter())
                    .margin(Margin.of(12))
                    .columns(
                            TableColumnLayout.fixed(150),
                            TableColumnLayout.fixed(80),
                            TableColumnLayout.fixed(80),
                            TableColumnLayout.fixed(100))
                    .row("Item", "Qty", "Unit", "Amount")
                    .row("Coffee beans", "12", "$15.00", "$180.00")
                    .row("Filters", "4", "$5.00", "$20.00")
                    .row(
                            TableCellContent.text("Total").withColSpan(3).withStyle(TableCellLayoutStyle.builder()
                                    .fillColor(ComponentColor.LIGHT_GRAY)
                                    .textAnchor(Anchor.centerRight())
                                    .build()),
                            TableCellContent.text("$200.00").withStyle(TableCellLayoutStyle.builder()
                                    .fillColor(ComponentColor.LIGHT_GRAY)
                                    .textAnchor(Anchor.centerRight())
                                    .build()))
                    .build();

            TableLayoutData layoutData = table.getComponent(TableLayoutData.class).orElseThrow();
            assertThat(layoutData.columnCount()).isEqualTo(4);
            assertThat(layoutData.rowCount()).isEqualTo(4);

            TableRowData totalsRow = layoutData.rowEntities().get(3).getComponent(TableRowData.class).orElseThrow();
            assertThat(totalsRow.cells()).hasSize(2);

            TableResolvedCell label = totalsRow.cells().get(0);
            assertThat(label.x()).isEqualTo(0.0, within(EPS));
            assertThat(label.width()).isEqualTo(150.0 + 80.0 + 80.0, within(EPS));

            TableResolvedCell amount = totalsRow.cells().get(1);
            assertThat(amount.x()).isEqualTo(150.0 + 80.0 + 80.0, within(EPS));
            assertThat(amount.width()).isEqualTo(100.0, within(EPS));

            composer.build();
        }

        assertThat(output).exists().isNotEmptyFile();
    }

    @Test
    void shouldRenderHeaderGroupWithSpannedMiddleCell() throws Exception {
        Path output = VisualTestOutputs.preparePdf("table_colspan_header_group", "clean", "integration");

        try (EngineComposerHarness composer = EngineComposerHarness.pdf(output)
                .pageSize(PDRectangle.A4)
                .margin(24, 24, 24, 24)
                .guideLines(false)
                .create()) {

            Entity table = composer.componentBuilder()
                    .table()
                    .entityName("ColSpanHeader")
                    .anchor(Anchor.topCenter())
                    .margin(Margin.of(12))
                    .columns(
                            TableColumnLayout.fixed(120),
                            TableColumnLayout.fixed(90),
                            TableColumnLayout.fixed(90),
                            TableColumnLayout.fixed(120))
                    .row(
                            TableCellContent.text("Region"),
                            TableCellContent.text("Sales").withColSpan(2),
                            TableCellContent.text("Owner"))
                    .row("North", "Q1: $10k", "Q2: $14k", "Mark")
                    .row("South", "Q1: $8k", "Q2: $11k", "Anna")
                    .build();

            TableLayoutData layoutData = table.getComponent(TableLayoutData.class).orElseThrow();
            assertThat(layoutData.columnCount()).isEqualTo(4);

            TableRowData header = layoutData.rowEntities().get(0).getComponent(TableRowData.class).orElseThrow();
            assertThat(header.cells()).hasSize(3);

            TableResolvedCell region = header.cells().get(0);
            assertThat(region.x()).isEqualTo(0.0, within(EPS));
            assertThat(region.width()).isEqualTo(120.0, within(EPS));

            TableResolvedCell sales = header.cells().get(1);
            assertThat(sales.x()).isEqualTo(120.0, within(EPS));
            assertThat(sales.width()).isEqualTo(90.0 + 90.0, within(EPS));

            TableResolvedCell owner = header.cells().get(2);
            assertThat(owner.x()).isEqualTo(120.0 + 90.0 + 90.0, within(EPS));
            assertThat(owner.width()).isEqualTo(120.0, within(EPS));

            TableRowData dataRow = layoutData.rowEntities().get(1).getComponent(TableRowData.class).orElseThrow();
            assertThat(dataRow.cells()).hasSize(4);

            composer.build();
        }

        assertThat(output).exists().isNotEmptyFile();
    }

    @Test
    void shouldRenderFullWidthSectionDividerRow() throws Exception {
        Path output = VisualTestOutputs.preparePdf("table_colspan_section", "clean", "integration");

        try (EngineComposerHarness composer = EngineComposerHarness.pdf(output)
                .pageSize(PDRectangle.A4)
                .margin(24, 24, 24, 24)
                .guideLines(false)
                .create()) {

            Entity table = composer.componentBuilder()
                    .table()
                    .entityName("ColSpanSection")
                    .anchor(Anchor.topCenter())
                    .margin(Margin.of(12))
                    .columns(
                            TableColumnLayout.fixed(110),
                            TableColumnLayout.fixed(110),
                            TableColumnLayout.fixed(110))
                    .row("Phase", "Owner", "Status")
                    .row("Discovery", "Mark", "Done")
                    .row(
                            TableCellContent.text("— PHASE 2 —").withColSpan(3).withStyle(TableCellLayoutStyle.builder()
                                    .fillColor(ComponentColor.LIGHT_GRAY)
                                    .textAnchor(Anchor.center())
                                    .build()))
                    .row("Implementation", "Anna", "In progress")
                    .build();

            TableLayoutData layoutData = table.getComponent(TableLayoutData.class).orElseThrow();
            assertThat(layoutData.rowCount()).isEqualTo(4);

            TableRowData divider = layoutData.rowEntities().get(2).getComponent(TableRowData.class).orElseThrow();
            assertThat(divider.cells()).hasSize(1);

            TableResolvedCell only = divider.cells().get(0);
            assertThat(only.x()).isEqualTo(0.0, within(EPS));
            assertThat(only.width()).isEqualTo(110.0 * 3, within(EPS));
            assertThat(only.borderSides()).contains(Side.LEFT, Side.RIGHT);

            composer.build();
        }

        assertThat(output).exists().isNotEmptyFile();
    }

    @Test
    void shouldFailWhenColSpanSumDoesNotMatchColumnCount() throws Exception {
        try (EngineComposerHarness composer = EngineComposerHarness.pdf().create()) {
            assertThatThrownBy(() -> composer.componentBuilder()
                    .table()
                    .entityName("BrokenSpan")
                    .columns(TableColumnLayout.auto(), TableColumnLayout.auto(), TableColumnLayout.auto())
                    .row("A", "B", "C")
                    .row(
                            TableCellContent.text("Wrong").withColSpan(2),
                            TableCellContent.text("X"),
                            TableCellContent.text("Y"))
                    .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("colSpan sum");
        }
    }

    @Test
    void shouldFailWhenColSpanIsZeroOrNegative() throws Exception {
        assertThatThrownBy(() -> TableCellContent.text("X").withColSpan(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("colSpan");

        assertThatThrownBy(() -> TableCellContent.text("X").withColSpan(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("colSpan");
    }

    @Test
    void shouldDistributeDeficitToAutoColumnsWhenSpannedCellNeedsMoreWidth() throws Exception {
        try (EngineComposerHarness composer = EngineComposerHarness.pdf().create()) {
            Entity table = composer.componentBuilder()
                    .table()
                    .entityName("ColSpanDeficit")
                    .columns(TableColumnLayout.fixed(40), TableColumnLayout.auto(), TableColumnLayout.auto())
                    .row("ID", "First", "Second")
                    .row(
                            TableCellContent.text("X"),
                            TableCellContent.text("This is a very long spanned label that needs more room").withColSpan(2))
                    .build();

            TableLayoutData layoutData = table.getComponent(TableLayoutData.class).orElseThrow();
            assertThat(layoutData.columnWidths().get(0)).isEqualTo(40.0, within(EPS));

            double col1Width = layoutData.columnWidths().get(1);
            double col2Width = layoutData.columnWidths().get(2);
            assertThat(col1Width + col2Width).isGreaterThan(50.0);
        }
    }

    @Test
    void shouldFailWhenSpannedCellNeedsExtraWidthButAllSpannedColumnsAreFixed() throws Exception {
        try (EngineComposerHarness composer = EngineComposerHarness.pdf().create()) {
            assertThatThrownBy(() -> composer.componentBuilder()
                    .table()
                    .entityName("ColSpanFixedDeficit")
                    .columns(TableColumnLayout.fixed(20), TableColumnLayout.fixed(20))
                    .row("a", "b")
                    .row(TableCellContent.text("Way too long for forty points combined").withColSpan(2))
                    .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("fixed");
        }
    }
}
