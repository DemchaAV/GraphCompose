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

class TwoColumnSidebarTest {

    private static DocumentNode spacer(String name) {
        return new SpacerNode(name, 0, 1, DocumentInsets.zero(), DocumentInsets.zero());
    }

    @Test
    void slotNamesAreMainThenSidebar() {
        assertThat(TwoColumnSidebar.layout().slotNames())
                .containsExactly(TwoColumnSidebar.MAIN, TwoColumnSidebar.SIDEBAR);
    }

    @Test
    void composeProducesSectionWithHeaderThenWeightedRow() {
        DocumentNode header = spacer("header");
        SlotMap slots = new SlotMap()
                .add(TwoColumnSidebar.MAIN, spacer("m1"))
                .add(TwoColumnSidebar.MAIN, spacer("m2"))
                .add(TwoColumnSidebar.SIDEBAR, spacer("s1"));

        DocumentNode composed = TwoColumnSidebar.layout().compose(header, slots);
        assertThat(composed).isInstanceOf(ContainerNode.class);
        ContainerNode root = (ContainerNode) composed;
        assertThat(root.children()).hasSize(2);
        assertThat(((SpacerNode) root.children().get(0)).name()).isEqualTo("header");
        assertThat(root.children().get(1)).isInstanceOf(RowNode.class);

        RowNode row = (RowNode) root.children().get(1);
        assertThat(row.children()).hasSize(2);
        assertThat(row.weights()).containsExactly(0.65, 0.35);

        ContainerNode mainCol = (ContainerNode) row.children().get(0);
        ContainerNode sideCol = (ContainerNode) row.children().get(1);
        assertThat(mainCol.children()).hasSize(2);
        assertThat(sideCol.children()).hasSize(1);
    }

    @Test
    void weightsAndGapsArePropagated() {
        TwoColumnSidebar layout = TwoColumnSidebar.layout()
                .mainWeight(0.7)
                .sidebarWeight(0.3)
                .columnGap(15.0)
                .moduleGap(8.0);
        ContainerNode root = (ContainerNode) layout.compose(spacer("h"), new SlotMap());
        RowNode row = (RowNode) root.children().get(1);
        assertThat(row.weights()).containsExactly(0.7, 0.3);
        assertThat(row.gap()).isEqualTo(15.0);
        ContainerNode mainCol = (ContainerNode) row.children().get(0);
        assertThat(mainCol.spacing()).isEqualTo(8.0);
    }

    @Test
    void weightsRejectNonPositive() {
        TwoColumnSidebar layout = TwoColumnSidebar.layout();
        assertThatThrownBy(() -> layout.mainWeight(0.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> layout.sidebarWeight(-1.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void gapsRejectNegativeOrInfinite() {
        TwoColumnSidebar layout = TwoColumnSidebar.layout();
        assertThatThrownBy(() -> layout.columnGap(-1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> layout.moduleGap(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void composeRejectsNullArguments() {
        TwoColumnSidebar layout = TwoColumnSidebar.layout();
        assertThatThrownBy(() -> layout.compose(null, new SlotMap()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> layout.compose(spacer("h"), null))
                .isInstanceOf(NullPointerException.class);
    }
}
