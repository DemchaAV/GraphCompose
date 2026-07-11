package com.demcha.compose.document.layout;

import com.demcha.compose.document.exceptions.AtomicNodeTooLargeException;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.engine.components.style.Margin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Engine-level contract for {@link SplittableLeafCompiler}, extracted from
 * {@link LayoutCompiler}. Whole-document pagination is guarded end-to-end by the
 * qa parity / snapshot suites; these pin the split loop's decisions — a piece
 * that fits whole, the no-progress guard, and the too-large throw — in isolation
 * with a stub {@link NodeDefinition} so the branches are exercised directly.
 */
class SplittableLeafCompilerTest {

    // 200×100 canvas, 10pt margins → ~80pt of inner content height per page.
    private final CompilerState state = new CompilerState(LayoutCanvas.from(200, 100, Margin.of(10)));

    @SuppressWarnings("unchecked")
    private final NodeDefinition<DocumentNode> definition = mock(NodeDefinition.class);
    private final PrepareContext prepareContext = mock(PrepareContext.class);
    private final FragmentContext fragmentContext = mock(FragmentContext.class);
    private final List<PlacedNode> nodes = new ArrayList<>();
    private final List<PlacedFragment> fragments = new ArrayList<>();

    private static PreparedNode<DocumentNode> leaf(double height) {
        DocumentNode node = new SpacerNode("leaf", 100, height, DocumentInsets.zero(), DocumentInsets.zero());
        return PreparedNode.leaf(node, new MeasureResult(100, height));
    }

    private void compile(PreparedNode<DocumentNode> prepared) {
        SplittableLeafCompiler.compile(prepared, definition, "leaf", "leaf", null, 0, 1,
                10, 180, state, prepareContext, fragmentContext, nodes, fragments);
    }

    @Test
    void aPieceThatFitsWholeIsPlacedOnASinglePage() {
        when(definition.emitFragments(any(), any(), any()))
                .thenReturn(List.of(new LayoutFragment("leaf", 0, 0.0, 0.0, 100, 10, "band")));

        compile(leaf(10)); // 10pt fits in the ~80pt page.

        assertThat(nodes).singleElement().satisfies(n -> {
            assertThat(n.startPage()).isZero();
            assertThat(n.endPage()).isZero();
        });
        assertThat(fragments).hasSize(1);
        assertThat(state.usedHeight).isGreaterThan(0.0);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS) // a regressed no-progress guard loops forever; fail fast instead.
    void aSplitThatReturnsTheOriginalAsItsTailIsRejectedAsNoProgress() {
        // The piece cannot fit whole, so it is split; the definition returns the
        // input as the tail — an infinite split loop the compiler must reject.
        PreparedNode<DocumentNode> tooTall = leaf(10_000);
        when(definition.split(any(), any()))
                .thenReturn(new PreparedSplitResult<>(leaf(20), tooTall));

        assertThatThrownBy(() -> compile(tooTall))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("did not make progress");
    }

    @Test
    void aPieceThatCannotBeSplitOnAnEmptyPageThrowsTooLarge() {
        // Doesn't fit whole and the definition can't split it (head == null) on a
        // fresh page — it can never be placed, so the too-large error is raised.
        when(definition.split(any(), any()))
                .thenReturn(new PreparedSplitResult<>(null, null));

        assertThatThrownBy(() -> compile(leaf(10_000)))
                .isInstanceOf(AtomicNodeTooLargeException.class)
                .hasMessageContaining("page capacity");
    }
}
