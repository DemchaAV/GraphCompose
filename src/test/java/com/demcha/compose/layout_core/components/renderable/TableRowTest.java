package com.demcha.compose.layout_core.components.renderable;

import com.demcha.compose.font_library.DefaultFonts;
import com.demcha.compose.layout_core.components.components_builders.TableCellStyle;
import com.demcha.compose.layout_core.components.content.shape.Side;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.content.table.TableResolvedCell;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.layout.ParentComponent;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TableRowTest {

    private final TableRow tableRow = new TableRow();

    @Test
    void shouldTreatFirstRowOnNewPageAsNewFragment() {
        EntityManager manager = new EntityManager();
        Entity parent = new Entity();
        Entity previousRow = new Entity();
        Entity currentRow = new Entity();

        parent.getChildren().add(previousRow.getUuid());
        parent.getChildren().add(currentRow.getUuid());

        previousRow.addComponent(new ParentComponent(parent));
        currentRow.addComponent(new ParentComponent(parent));

        previousRow.addComponent(new Placement(0, 0, 200, 20, 0, 0));
        currentRow.addComponent(new Placement(0, 0, 200, 20, 1, 1));

        manager.putEntity(parent);
        manager.putEntity(previousRow);
        manager.putEntity(currentRow);

        TableResolvedCell cell = new TableResolvedCell(
                "cell",
                0,
                100,
                20,
                List.of("value"),
                TableCellStyle.builder()
                        .stroke(new Stroke(ComponentColor.BLACK, 2.0))
                        .build(),
                new Padding(3.0, 0.5, 0.5, 0.5),
                Set.of(Side.RIGHT, Side.BOTTOM)
        );

        assertThat(tableRow.startsPageFragment(manager, currentRow)).isTrue();
        assertThat(tableRow.effectiveBorderSides(cell, true)).containsExactlyInAnyOrder(Side.TOP, Side.RIGHT, Side.BOTTOM);
        assertThat(tableRow.effectiveFillInsets(cell, true))
                .isEqualTo(new Padding(1.0, 0.5, 0.5, 0.5));
    }

    @Test
    void shouldKeepSingleSeparatorWhenRowsStayOnSamePage() {
        EntityManager manager = new EntityManager();
        Entity parent = new Entity();
        Entity previousRow = new Entity();
        Entity currentRow = new Entity();

        parent.getChildren().add(previousRow.getUuid());
        parent.getChildren().add(currentRow.getUuid());

        previousRow.addComponent(new ParentComponent(parent));
        currentRow.addComponent(new ParentComponent(parent));

        previousRow.addComponent(new Placement(0, 40, 200, 20, 0, 0));
        currentRow.addComponent(new Placement(0, 20, 200, 20, 0, 0));

        manager.putEntity(parent);
        manager.putEntity(previousRow);
        manager.putEntity(currentRow);

        TableResolvedCell cell = new TableResolvedCell(
                "cell",
                0,
                100,
                20,
                List.of("value"),
                TableCellStyle.builder()
                        .stroke(new Stroke(ComponentColor.BLACK, 2.0))
                        .build(),
                new Padding(3.0, 0.5, 0.5, 0.5),
                Set.of(Side.RIGHT, Side.BOTTOM)
        );

        assertThat(tableRow.startsPageFragment(manager, currentRow)).isFalse();
        assertThat(tableRow.effectiveBorderSides(cell, false)).containsExactlyInAnyOrder(Side.RIGHT, Side.BOTTOM);
        assertThat(tableRow.effectiveFillInsets(cell, false))
                .isEqualTo(new Padding(3.0, 0.5, 0.5, 0.5));
    }

    @Test
    void shouldResolveTopAndMiddleAnchorsForMultilineContent() {
        var font = DefaultFonts.standardLibrary().getPdfFont(TextStyle.DEFAULT_STYLE.fontName());

        TableResolvedCell topCell = new TableResolvedCell(
                "top",
                0,
                120,
                60,
                List.of("09:00 17:00", "Stock take"),
                TableCellStyle.merge(TableCellStyle.DEFAULT, TableCellStyle.builder()
                        .textAnchor(Anchor.topLeft())
                        .build()),
                Padding.zero(),
                Set.of(Side.TOP, Side.LEFT, Side.RIGHT, Side.BOTTOM)
        );

        TableResolvedCell middleCell = new TableResolvedCell(
                "middle",
                0,
                120,
                60,
                List.of("09:00 17:00", "Stock take"),
                TableCellStyle.merge(TableCellStyle.DEFAULT, TableCellStyle.builder()
                        .textAnchor(Anchor.centerLeft())
                        .build()),
                Padding.zero(),
                Set.of(Side.TOP, Side.LEFT, Side.RIGHT, Side.BOTTOM)
        );

        var topLines = tableRow.resolveTextLines(font, topCell, 0, 0);
        var middleLines = tableRow.resolveTextLines(font, middleCell, 0, 0);
        double lineHeight = font.getLineHeight(topCell.style().textStyle());

        assertThat(topLines).hasSize(2);
        assertThat(middleLines).hasSize(2);
        assertThat(topLines.getFirst().baselineY()).isGreaterThan(middleLines.getFirst().baselineY());
        assertThat(topLines.getFirst().baselineY() - topLines.get(1).baselineY()).isEqualTo(lineHeight);
        assertThat(middleLines.getFirst().baselineY() - middleLines.get(1).baselineY()).isEqualTo(lineHeight);
    }
}
