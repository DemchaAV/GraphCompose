package com.demcha.compose.engine.components.components_builders;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.engine.measurement.FontLibraryTextMeasurementSystem;
import com.demcha.compose.engine.components.content.table.TableLayoutData;
import com.demcha.compose.engine.components.content.table.TableResolvedCell;
import com.demcha.compose.engine.components.content.table.TableRowData;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.core.EntityName;
import com.demcha.compose.engine.components.content.shape.Side;
import com.demcha.compose.engine.components.style.ComponentColor;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.testsupport.EngineComposerHarness;
import com.demcha.compose.engine.render.pdf.PdfFont;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TableBuilderTest {

    @Test
    void shouldComputeFixedAndAutoColumnWidths() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            Entity table = composer.componentBuilder()
                    .table()
                    .entityName("MetricsTable")
                    .columns(TableColumnSpec.fixed(120), TableColumnSpec.auto())
                    .row("ID", "Longest visible metric label")
                    .row("Code", "Label")
                    .build();

            TableLayoutData layoutData = table.getComponent(TableLayoutData.class).orElseThrow();
            Entity firstRow = child(table, 0);
            TableRowData rowData = firstRow.getComponent(TableRowData.class).orElseThrow();

            assertThat(layoutData.columnWidths()).hasSize(2);
            assertThat(layoutData.columnWidths().getFirst()).isEqualTo(120.0);
            assertThat(rowData.cells().getFirst().width()).isEqualTo(120.0);
            assertThat(rowData.cells().get(1).width()).isEqualTo(layoutData.columnWidths().get(1));
            assertThat(layoutData.finalWidth()).isEqualTo(layoutData.columnWidths().stream().mapToDouble(Double::doubleValue).sum());
        }
    }

    @Test
    void shouldUseNaturalWidthWhenExplicitWidthIsNotProvided() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            Entity table = composer.componentBuilder()
                    .table()
                    .entityName("NaturalWidthTable")
                    .columns(TableColumnSpec.auto(), TableColumnSpec.auto())
                    .row("First", "Second")
                    .row("Much wider first column", "Value")
                    .build();

            TableLayoutData layoutData = table.getComponent(TableLayoutData.class).orElseThrow();

            assertThat(layoutData.finalWidth()).isEqualTo(layoutData.naturalWidth());
            assertThat(layoutData.finalWidth()).isEqualTo(layoutData.columnWidths().stream().mapToDouble(Double::doubleValue).sum());
        }
    }

    @Test
    void shouldFailWhenRequestedWidthIsSmallerThanNaturalWidth() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            assertThatThrownBy(() -> composer.componentBuilder()
                    .table()
                    .columns(TableColumnSpec.auto(), TableColumnSpec.auto())
                    .width(60)
                    .row("Very long text", "Another long text")
                    .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Requested table width");
        }
    }

    @Test
    void shouldApplyStylePrecedenceDefaultThenColumnThenRow() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            TableCellStyle tableDefault = TableCellStyle.builder()
                    .fillColor(ComponentColor.LIGHT_GRAY)
                    .padding(Padding.of(2))
                    .lineSpacing(1.0)
                    .build();

            TableCellStyle columnOverride = TableCellStyle.builder()
                    .fillColor(ComponentColor.BLUE)
                    .lineSpacing(2.0)
                    .textStyle(TextStyle.builder()
                            .fontName(TextStyle.DEFAULT_STYLE.fontName())
                            .size(18)
                            .decoration(TextStyle.DEFAULT_STYLE.decoration())
                            .color(TextStyle.DEFAULT_STYLE.color())
                            .build())
                    .build();

            TableCellStyle rowOverride = TableCellStyle.builder()
                    .fillColor(ComponentColor.RED)
                    .padding(Padding.of(8))
                    .build();

            Entity table = composer.componentBuilder()
                    .table()
                    .entityName("StyledTable")
                    .defaultCellStyle(tableDefault)
                    .columnStyle(0, columnOverride)
                    .rowStyle(0, rowOverride)
                    .row("A", "B")
                    .row("C", "D")
                    .build();

            TableResolvedCell firstRowFirstCell = child(table, 0)
                    .getComponent(TableRowData.class)
                    .orElseThrow()
                    .cells()
                    .getFirst();

            TableResolvedCell secondRowFirstCell = child(table, 1)
                    .getComponent(TableRowData.class)
                    .orElseThrow()
                    .cells()
                    .getFirst();

            assertThat(firstRowFirstCell.style().fillColor()).isEqualTo(ComponentColor.RED);
            assertThat(firstRowFirstCell.style().padding()).isEqualTo(Padding.of(8));
            assertThat(firstRowFirstCell.style().textStyle().size()).isEqualTo(18);
            assertThat(firstRowFirstCell.style().lineSpacing()).isEqualTo(2.0);

            assertThat(secondRowFirstCell.style().fillColor()).isEqualTo(ComponentColor.BLUE);
            assertThat(secondRowFirstCell.style().padding()).isEqualTo(Padding.of(2));
            assertThat(secondRowFirstCell.style().textStyle().size()).isEqualTo(18);
            assertThat(secondRowFirstCell.style().lineSpacing()).isEqualTo(2.0);
        }
    }

    @Test
    void shouldApplyCellOverrideAfterDefaultColumnAndRowStyles() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            TableCellStyle tableDefault = TableCellStyle.builder()
                    .fillColor(ComponentColor.LIGHT_GRAY)
                    .padding(Padding.of(2))
                    .build();

            TableCellStyle columnOverride = TableCellStyle.builder()
                    .textStyle(TextStyle.builder()
                            .fontName(TextStyle.DEFAULT_STYLE.fontName())
                            .size(18)
                            .decoration(TextStyle.DEFAULT_STYLE.decoration())
                            .color(TextStyle.DEFAULT_STYLE.color())
                            .build())
                    .build();

            TableCellStyle rowOverride = TableCellStyle.builder()
                    .fillColor(ComponentColor.RED)
                    .padding(Padding.of(8))
                    .build();

            TableCellStyle cellOverride = TableCellStyle.builder()
                    .fillColor(ComponentColor.GREEN)
                    .textAnchor(com.demcha.compose.engine.components.layout.Anchor.centerRight())
                    .build();

            Entity table = composer.componentBuilder()
                    .table()
                    .entityName("StyledTableWithCellOverride")
                    .defaultCellStyle(tableDefault)
                    .columnStyle(0, columnOverride)
                    .rowStyle(0, rowOverride)
                    .row(
                            TableCellSpec.text("A").withStyle(cellOverride),
                            TableCellSpec.text("B"))
                    .build();

            TableResolvedCell firstCell = child(table, 0)
                    .getComponent(TableRowData.class)
                    .orElseThrow()
                    .cells()
                    .getFirst();

            assertThat(firstCell.style().fillColor()).isEqualTo(ComponentColor.GREEN);
            assertThat(firstCell.style().padding()).isEqualTo(Padding.of(8));
            assertThat(firstCell.style().textStyle().size()).isEqualTo(18);
            assertThat(firstCell.style().textAnchor()).isEqualTo(com.demcha.compose.engine.components.layout.Anchor.centerRight());
        }
    }

    @Test
    void shouldPreserveStringRowApiAsSingleLineCellSpec() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            Entity table = composer.componentBuilder()
                    .table()
                    .entityName("StringApiTable")
                    .row("Alpha", "Beta")
                    .build();

            TableResolvedCell firstCell = child(table, 0)
                    .getComponent(TableRowData.class)
                    .orElseThrow()
                    .cells()
                    .getFirst();

            assertThat(firstCell.lines()).containsExactly("Alpha");
        }
    }

    @Test
    void shouldMeasureMultilineCellsUsingLongestLineAndLineCount() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            Entity multilineTable = composer.componentBuilder()
                    .table()
                    .entityName("MultilineTable")
                    .row(TableCellSpec.lines("Short", "Longest visible metric label"))
                    .build();

            Entity longestLineTable = composer.componentBuilder()
                    .table()
                    .entityName("LongestLineTable")
                    .row("Longest visible metric label")
                    .build();

            TableResolvedCell multilineCell = child(multilineTable, 0)
                    .getComponent(TableRowData.class)
                    .orElseThrow()
                    .cells()
                    .getFirst();

            TableResolvedCell singleLineCell = child(longestLineTable, 0)
                    .getComponent(TableRowData.class)
                    .orElseThrow()
                    .cells()
                    .getFirst();

            double paddingVertical = TableCellStyle.DEFAULT.padding().vertical();
            double expectedMultilineHeight = (2 * (singleLineCell.height() - paddingVertical)) + paddingVertical;

            assertThat(multilineCell.width()).isEqualTo(singleLineCell.width());
            assertThat(multilineCell.height()).isEqualTo(expectedMultilineHeight);
            assertThat(multilineCell.lines()).containsExactly("Short", "Longest visible metric label");
        }
    }

    @Test
    void shouldIncludeCellLineSpacingInMultilineCellHeight() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            TableCellStyle style = TableCellStyle.builder()
                    .padding(Padding.zero())
                    .lineSpacing(2.5)
                    .build();

            Entity multilineTable = composer.componentBuilder()
                    .table()
                    .entityName("MultilineLineSpacingTable")
                    .defaultCellStyle(style)
                    .row(TableCellSpec.lines("Short", "Longer line"))
                    .build();

            Entity singleLineTable = composer.componentBuilder()
                    .table()
                    .entityName("SingleLineSpacingTable")
                    .defaultCellStyle(style)
                    .row("Short")
                    .build();

            TableResolvedCell multilineCell = child(multilineTable, 0)
                    .getComponent(TableRowData.class)
                    .orElseThrow()
                    .cells()
                    .getFirst();
            TableResolvedCell singleLineCell = child(singleLineTable, 0)
                    .getComponent(TableRowData.class)
                    .orElseThrow()
                    .cells()
                    .getFirst();

            assertThat(multilineCell.height()).isEqualTo((2 * singleLineCell.height()) + 2.5);
        }
    }

    @Test
    void shouldBuildWithoutLayoutSystemWhenTextMeasurementSystemIsRegistered() {
        EntityManager entityManager = new EntityManager();
        entityManager.getSystems().addSystem(new FontLibraryTextMeasurementSystem(entityManager.getFonts(), PdfFont.class));

        Entity table = new TableBuilder(entityManager)
                .entityName("MeasurementOnlyTable")
                .row("Alpha", "Beta")
                .build();

        TableLayoutData layoutData = table.getComponent(TableLayoutData.class).orElseThrow();
        assertThat(layoutData.columnWidths()).hasSize(2);
        assertThat(layoutData.finalWidth()).isGreaterThan(0.0);
    }

    @Test
    void shouldRejectEmptyTable() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            assertThatThrownBy(() -> composer.componentBuilder()
                    .table()
                    .entityName("EmptyTable")
                    .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("at least one row");
        }
    }

    @Test
    void shouldRejectRowsThatDoNotMatchResolvedColumnCount() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            assertThatThrownBy(() -> composer.componentBuilder()
                    .table()
                    .columns(TableColumnSpec.fixed(120), TableColumnSpec.auto())
                    .row("Only one cell")
                    .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("requires 2 columns");
        }
    }

    @Test
    void shouldCreateStableRowAndCellNamesFromTableName() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            Entity table = composer.componentBuilder()
                    .table()
                    .entityName("Orders")
                    .row("A", "B")
                    .build();

            Entity row = child(table, 0);
            TableResolvedCell cell = row.getComponent(TableRowData.class).orElseThrow().cells().getFirst();

            assertThat(row.getComponent(EntityName.class)).hasValue(new EntityName("Orders__row_0"));
            assertThat(cell.name()).isEqualTo("Orders__row_0__cell_0");
        }
    }

    @Test
    void shouldAssignBordersToCurrentCellsSoTrailingPageLinesStayVisible() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            Entity table = composer.componentBuilder()
                    .table()
                    .entityName("BorderOwnership")
                    .row("A", "B")
                    .row("C", "D")
                    .build();

            TableResolvedCell firstRowFirstCell = child(table, 0)
                    .getComponent(TableRowData.class)
                    .orElseThrow()
                    .cells()
                    .getFirst();

            TableResolvedCell firstRowSecondCell = child(table, 0)
                    .getComponent(TableRowData.class)
                    .orElseThrow()
                    .cells()
                    .get(1);

            TableResolvedCell secondRowSecondCell = child(table, 1)
                    .getComponent(TableRowData.class)
                    .orElseThrow()
                    .cells()
                    .get(1);

            assertThat(firstRowFirstCell.borderSides()).containsExactlyInAnyOrder(Side.TOP, Side.LEFT, Side.RIGHT, Side.BOTTOM);
            assertThat(firstRowSecondCell.borderSides()).containsExactlyInAnyOrder(Side.TOP, Side.RIGHT, Side.BOTTOM);
            assertThat(secondRowSecondCell.borderSides()).containsExactlyInAnyOrder(Side.RIGHT, Side.BOTTOM);
        }
    }

    @Test
    void shouldLetCellOwnLeftBoundaryWhenInvisibleSpacerPrecedesIt() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            Entity table = composer.componentBuilder()
                    .table()
                    .entityName("InvisibleSpacerBoundary")
                    .defaultCellStyle(TableCellStyle.builder()
                            .stroke(new Stroke(ComponentColor.BLACK, 2.0))
                            .build())
                    .columnStyle(1, TableCellStyle.builder()
                            .stroke(new Stroke(ComponentColor.WHITE, 0.0))
                            .build())
                    .row("Notes", "", "Total")
                    .build();

            TableResolvedCell summaryCell = child(table, 0)
                    .getComponent(TableRowData.class)
                    .orElseThrow()
                    .cells()
                    .get(2);

            assertThat(summaryCell.borderSides()).containsExactlyInAnyOrder(Side.TOP, Side.LEFT, Side.RIGHT, Side.BOTTOM);
            assertThat(summaryCell.fillInsets().left()).isEqualTo(1.0);
        }
    }

    @Test
    void shouldLetCellOwnTopBoundaryWhenPreviousRowStrokeIsInvisible() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            Entity table = composer.componentBuilder()
                    .table()
                    .entityName("InvisiblePreviousRowBoundary")
                    .defaultCellStyle(TableCellStyle.builder()
                            .stroke(new Stroke(ComponentColor.BLACK, 2.0))
                            .build())
                    .rowStyle(0, TableCellStyle.builder()
                            .stroke(new Stroke(ComponentColor.WHITE, 0.0))
                            .build())
                    .row("Notes", "Total")
                    .row("Terms", "Paid")
                    .build();

            TableResolvedCell secondRowCell = child(table, 1)
                    .getComponent(TableRowData.class)
                    .orElseThrow()
                    .cells()
                    .getFirst();

            assertThat(secondRowCell.borderSides()).containsExactlyInAnyOrder(Side.TOP, Side.LEFT, Side.RIGHT, Side.BOTTOM);
            assertThat(secondRowCell.fillInsets().top()).isEqualTo(1.0);
        }
    }

    @Test
    void shouldInsetFillUsingOwningNeighborStrokeWidths() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            Entity table = composer.componentBuilder()
                    .table()
                    .entityName("FillInsets")
                    .defaultCellStyle(TableCellStyle.builder()
                            .stroke(new Stroke(ComponentColor.BLACK, 1.0))
                            .build())
                    .rowStyle(0, TableCellStyle.builder()
                            .stroke(new Stroke(ComponentColor.BLACK, 2.0))
                            .build())
                    .columnStyle(0, TableCellStyle.builder()
                            .stroke(new Stroke(ComponentColor.BLACK, 4.0))
                            .build())
                    .row("A", "B")
                    .row("C", "D")
                    .build();

            TableResolvedCell secondRowSecondCell = child(table, 1)
                    .getComponent(TableRowData.class)
                    .orElseThrow()
                    .cells()
                    .get(1);

            assertThat(secondRowSecondCell.fillInsets().top()).isEqualTo(1.0);
            assertThat(secondRowSecondCell.fillInsets().left()).isEqualTo(2.0);
            assertThat(secondRowSecondCell.fillInsets().right()).isEqualTo(0.5);
            assertThat(secondRowSecondCell.fillInsets().bottom()).isEqualTo(0.5);
        }
    }

    private Entity child(Entity parent, int index) {
        TableLayoutData layoutData = parent.getComponent(TableLayoutData.class).orElseThrow();
        return layoutData.rowEntities().get(index);
    }
}
