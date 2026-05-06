package com.demcha.compose.document.templates.api;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlotMapTest {

    private static DocumentNode spacer(String name) {
        return new SpacerNode(name, 0, 1, DocumentInsets.zero(), DocumentInsets.zero());
    }

    @Test
    void newMapIsEmpty() {
        SlotMap map = SlotMap.empty();
        assertThat(map.isEmpty()).isTrue();
        assertThat(map.populatedSlots()).isEmpty();
        assertThat(map.get("anything")).isEmpty();
    }

    @Test
    void addAppendsToTheNamedSlotInOrder() {
        SlotMap map = new SlotMap()
                .add("main", spacer("a"))
                .add("main", spacer("b"))
                .add("main", spacer("c"));

        assertThat(map.get("main")).hasSize(3);
        assertThat(map.get("main")).extracting(node -> ((SpacerNode) node).name())
                .containsExactly("a", "b", "c");
    }

    @Test
    void addAllAppendsAllInOrder() {
        SlotMap map = new SlotMap().addAll("sidebar",
                List.of(spacer("x"), spacer("y")));
        assertThat(map.get("sidebar")).hasSize(2);
    }

    @Test
    void getReturnsImmutableList() {
        SlotMap map = new SlotMap().add("main", spacer("a"));
        List<DocumentNode> children = map.get("main");
        assertThatThrownBy(() -> children.add(spacer("b")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getOnUnknownSlotReturnsEmptyList() {
        SlotMap map = new SlotMap().add("main", spacer("a"));
        assertThat(map.get("unknown")).isEmpty();
    }

    @Test
    void populatedSlotsPreservesInsertionOrder() {
        SlotMap map = new SlotMap()
                .add("main", spacer("a"))
                .add("sidebar", spacer("b"));
        assertThat(map.populatedSlots()).containsExactly("main", "sidebar");
    }

    @Test
    void addRejectsNullSlotOrNode() {
        SlotMap map = new SlotMap();
        assertThatThrownBy(() -> map.add(null, spacer("a")))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> map.add("main", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void addAllRejectsNullArgumentsOrAnyNullElement() {
        SlotMap map = new SlotMap();
        assertThatThrownBy(() -> map.addAll(null, List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> map.addAll("main", null))
                .isInstanceOf(NullPointerException.class);
        java.util.ArrayList<DocumentNode> withNull = new java.util.ArrayList<>();
        withNull.add(spacer("a"));
        withNull.add(null);
        assertThatThrownBy(() -> map.addAll("main", withNull))
                .isInstanceOf(NullPointerException.class);
    }
}
