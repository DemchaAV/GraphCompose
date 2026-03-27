package com.demcha.compose.layout_core.components.renderable;

import com.demcha.compose.layout_core.components.components_builders.TableCellStyle;
import com.demcha.compose.layout_core.components.content.shape.Side;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.content.table.TableResolvedCell;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.ParentComponent;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.EntityManager;
import org.junit.jupiter.api.Test;

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
                "value",
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
                "value",
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
}
