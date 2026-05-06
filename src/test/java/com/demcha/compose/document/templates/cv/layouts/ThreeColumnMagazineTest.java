package com.demcha.compose.document.templates.cv.layouts;

import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.SlotMap;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThreeColumnMagazineTest {

    private static DocumentNode spacer(String name) {
        return new SpacerNode(name, 0, 1, DocumentInsets.zero(), DocumentInsets.zero());
    }

    @Test
    void slotNamesAreThreeColumns() {
        assertThat(ThreeColumnMagazine.layout().slotNames())
                .containsExactly(ThreeColumnMagazine.COL_1,
                        ThreeColumnMagazine.COL_2,
                        ThreeColumnMagazine.COL_3);
    }

    @Test
    void composeProducesRowOfThreeEqualColumnsByDefault() {
        DocumentNode header = spacer("header");
        SlotMap slots = new SlotMap()
                .add(ThreeColumnMagazine.COL_1, spacer("a"))
                .add(ThreeColumnMagazine.COL_2, spacer("b"))
                .add(ThreeColumnMagazine.COL_3, spacer("c"));

        ContainerNode root = (ContainerNode) ThreeColumnMagazine.layout().compose(header, slots);
        assertThat(root.children()).hasSize(2);
        RowNode row = (RowNode) root.children().get(1);
        assertThat(row.children()).hasSize(3);
        assertThat(row.weights()).containsExactly(1.0, 1.0, 1.0);
    }

    @Test
    void weightsCanBeCustomised() {
        ThreeColumnMagazine layout = ThreeColumnMagazine.layout().weights(2.0, 3.0, 1.0);
        ContainerNode root = (ContainerNode) layout.compose(spacer("h"), new SlotMap());
        RowNode row = (RowNode) root.children().get(1);
        assertThat(row.weights()).containsExactly(2.0, 3.0, 1.0);
    }

    @Test
    void columnGapAndModuleGapPropagate() {
        ThreeColumnMagazine layout = ThreeColumnMagazine.layout()
                .columnGap(11.0)
                .moduleGap(5.0);
        ContainerNode root = (ContainerNode) layout.compose(spacer("h"), new SlotMap()
                .add(ThreeColumnMagazine.COL_1, spacer("a")));
        RowNode row = (RowNode) root.children().get(1);
        assertThat(row.gap()).isEqualTo(11.0);
        ContainerNode col1 = (ContainerNode) row.children().get(0);
        assertThat(col1.spacing()).isEqualTo(5.0);
    }

    @Test
    void weightsRejectNonPositive() {
        ThreeColumnMagazine layout = ThreeColumnMagazine.layout();
        assertThatThrownBy(() -> layout.weights(0.0, 1.0, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> layout.weights(1.0, -1.0, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void gapsRejectInvalid() {
        ThreeColumnMagazine layout = ThreeColumnMagazine.layout();
        assertThatThrownBy(() -> layout.columnGap(-1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> layout.moduleGap(Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
