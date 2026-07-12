package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Engine-level contract for the row weights / children-count guard
 * shared by {@link LayoutCompiler} and {@link NodeDefinitionSupport}.
 *
 * <p>The {@code RowNode} canonical constructor already rejects the
 * mismatched state via {@link RowBuilder}, so under normal authoring the
 * helper is unreachable. The helper exists for defence-in-depth paths
 * that bypass the constructor (reflection-based deserialization, etc.);
 * these tests pin the contract so a future refactor cannot silently
 * delete the guard at either call site.</p>
 */
class RowSlotsTest {

    @Test
    void matchingSizesPassWithoutThrowing() {
        assertThatCode(() -> RowSlots.validateWeightsMatchChildren(List.of(1.0, 2.0, 3.0), 3))
                .doesNotThrowAnyException();
    }

    @Test
    void moreWeightsThanChildrenIsRejectedWithBothSizesNamed() {
        assertThatThrownBy(() -> RowSlots.validateWeightsMatchChildren(List.of(1.0, 2.0, 3.0), 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weights")
                .hasMessageContaining("children")
                .hasMessageContaining("(3)")
                .hasMessageContaining("(2)");
    }

    @Test
    void fewerWeightsThanChildrenIsRejectedWithBothSizesNamed() {
        assertThatThrownBy(() -> RowSlots.validateWeightsMatchChildren(List.of(1.0, 2.0), 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("(2)")
                .hasMessageContaining("(5)");
    }

    @Test
    void errorMessageHintsAtTheFix() {
        // The senior-review bar for engine exception messages: name the
        // values AND tell the caller how to fix it. Asserting the verb
        // is enough — the exact wording is implementation detail.
        assertThatThrownBy(() -> RowSlots.validateWeightsMatchChildren(List.of(1.0), 4))
                .isInstanceOf(IllegalArgumentException.class)
                .extracting(Throwable::getMessage)
                .satisfies(msg -> {
                    String s = (String) msg;
                    assertThat(s).containsAnyOf("Pass", "Provide", "Use");
                });
    }

    @Test
    void distributesEvenlyWhenNoWeights() {
        assertThat(RowSlots.distributeRowSlotWidths(children(3), List.of(), 0.0, 30.0))
                .containsExactly(10.0, 10.0, 10.0);
    }

    @Test
    void distributesProportionallyToWeights() {
        assertThat(RowSlots.distributeRowSlotWidths(children(2), List.of(1.0, 3.0), 0.0, 40.0))
                .containsExactly(10.0, 30.0);
    }

    @Test
    void subtractsInterChildGapsBeforeDistributing() {
        // 30 wide, two 5-wide gaps -> 20 available, split three ways.
        assertThat(RowSlots.distributeRowSlotWidths(children(3), List.of(), 5.0, 30.0))
                .containsExactly(20.0 / 3, 20.0 / 3, 20.0 / 3);
    }

    @Test
    void fallsBackToEvenSplitWhenWeightsSumToZero() {
        assertThat(RowSlots.distributeRowSlotWidths(children(2), List.of(0.0, 0.0), 0.0, 20.0))
                .containsExactly(10.0, 10.0);
    }

    /** A list of {@code n} placeholder children — only the count is read. */
    private static List<DocumentNode> children(int n) {
        DocumentNode spacer = new SpacerNode("s", 0.0, 0.0, DocumentInsets.zero(), DocumentInsets.zero());
        return Collections.nCopies(n, spacer);
    }
}
