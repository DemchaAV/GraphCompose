package com.demcha.compose.document.templates.cv.layouts;

import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.SlotMap;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SingleColumnTest {

    private static DocumentNode spacer(String name) {
        return new SpacerNode(name, 0, 1, DocumentInsets.zero(), DocumentInsets.zero());
    }

    @Test
    void slotNamesIsJustMain() {
        assertThat(SingleColumn.layout().slotNames())
                .containsExactly(SingleColumn.MAIN);
    }

    @Test
    void composeStacksHeaderThenMainChildren() {
        DocumentNode header = spacer("header");
        SlotMap slots = new SlotMap()
                .add(SingleColumn.MAIN, spacer("m1"))
                .add(SingleColumn.MAIN, spacer("m2"));

        DocumentNode composed = SingleColumn.layout().compose(header, slots);

        assertThat(composed).isInstanceOf(ContainerNode.class);
        ContainerNode container = (ContainerNode) composed;
        assertThat(container.children()).hasSize(3);
        assertThat(((SpacerNode) container.children().get(0)).name()).isEqualTo("header");
        assertThat(((SpacerNode) container.children().get(1)).name()).isEqualTo("m1");
        assertThat(((SpacerNode) container.children().get(2)).name()).isEqualTo("m2");
    }

    @Test
    void emptyMainSlotJustEmitsHeader() {
        DocumentNode header = spacer("header");
        ContainerNode container = (ContainerNode) SingleColumn.layout()
                .compose(header, new SlotMap());
        assertThat(container.children()).hasSize(1);
    }

    @Test
    void moduleGapIsPropagatedToContainerSpacing() {
        SingleColumn layout = SingleColumn.layout().moduleGap(7.0);
        ContainerNode container = (ContainerNode) layout.compose(spacer("h"), new SlotMap());
        assertThat(container.spacing()).isEqualTo(7.0);
    }

    @Test
    void unknownSlotsAreSilentlyIgnored() {
        DocumentNode header = spacer("header");
        SlotMap slots = new SlotMap().add("unknown", spacer("rogue"));
        ContainerNode container = (ContainerNode) SingleColumn.layout().compose(header, slots);
        assertThat(container.children()).hasSize(1); // just the header
    }

    @Test
    void moduleGapRejectsInvalid() {
        assertThatThrownBy(() -> SingleColumn.layout().moduleGap(-1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SingleColumn.layout().moduleGap(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void composeRejectsNullArguments() {
        SingleColumn layout = SingleColumn.layout();
        assertThatThrownBy(() -> layout.compose(null, new SlotMap()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> layout.compose(spacer("h"), null))
                .isInstanceOf(NullPointerException.class);
    }
}
