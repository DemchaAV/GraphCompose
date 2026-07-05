package com.demcha.compose.document.layout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.DocumentInsets;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * Targeted invariant tests for {@link LayoutCompiler}. The compiler is the
 * load-bearing 1,400-line piece of the engine and historically had only
 * transitive coverage through template snapshots. Each test pins one specific
 * branch the audit flagged as previously uncovered.
 */
class LayoutCompilerInvariantsTest {

    private static final double EPS = 1e-3;

    /**
     * Splittable / atomic page-advance path: when content placed near the
     * page bottom no longer fits, the second piece must move to the next
     * page rather than overflow or be dropped.
     */
    @Test
    void contentExceedingRemainingHeightAdvancesToNextPage() {
        // Page 200x150 with margin 10 -> inner area 180x130.
        // First spacer 100 high consumes most of page 1; second spacer 100 high
        // does not fit in the remaining ~30 and must move to page 2.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(200, 150)
                .margin(DocumentInsets.of(10))
                .create()) {

            session.add(new SpacerNode("Block A", 180.0, 100.0, DocumentInsets.zero(), DocumentInsets.zero()));
            session.add(new SpacerNode("Block B", 180.0, 100.0, DocumentInsets.zero(), DocumentInsets.zero()));

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages())
                    .describedAs("two 100-high spacers cannot fit on a single 130-high page")
                    .isGreaterThanOrEqualTo(2);

            Set<Integer> pageIndices = graph.nodes().stream()
                    .filter(n -> List.of("Block A", "Block B").contains(n.semanticName()))
                    .map(PlacedNode::startPage)
                    .collect(Collectors.toSet());
            assertThat(pageIndices)
                    .describedAs("the two spacers should each occupy a distinct page")
                    .contains(0, 1);
        }
    }

    /**
     * Stack tie-breaking: when every layer in a {@link LayerStackNode} reports
     * the same {@code zIndex} (the default of 0), the compiler must preserve
     * source order. After the C.2 short-circuit added a fast path for the
     * all-equal case, this guards against a regression where the short-circuit
     * returns a different order than the stable sort would have.
     */
    @Test
    void layerStackPreservesSourceOrderWhenAllZIndicesEqual() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {

            LayerStackNode stack = new LayerStackBuilder()
                    .name("ZeroZIndexStack")
                    .layer(new SpacerNode("Layer A", 100.0, 80.0, DocumentInsets.zero(), DocumentInsets.zero()))
                    .layer(new SpacerNode("Layer B", 100.0, 80.0, DocumentInsets.zero(), DocumentInsets.zero()))
                    .layer(new SpacerNode("Layer C", 100.0, 80.0, DocumentInsets.zero(), DocumentInsets.zero()))
                    .build();

            session.add(stack);
            LayoutGraph graph = session.layoutGraph();

            // Layers identified by name should appear in declaration order
            // among the placed nodes — the placed-fragment list is the
            // implicit z-order PdfFixedLayoutBackend renders by, so source
            // order must survive when zIndex provides no tie-breaker.
            List<String> orderedLayerNames = graph.nodes().stream()
                    .map(PlacedNode::semanticName)
                    .filter(name -> List.of("Layer A", "Layer B", "Layer C").contains(name))
                    .toList();

            assertThat(orderedLayerNames)
                    .describedAs("layers with all-equal zIndex must keep source order")
                    .containsExactly("Layer A", "Layer B", "Layer C");
        }
    }

    /**
     * Stack with explicit zIndex order: ensures the sort branch (not the
     * all-equal short-circuit) still produces the correct ordering. Higher
     * {@code zIndex} renders on top, which means later in the placed-fragment
     * list.
     */
    @Test
    void layerStackHonoursExplicitZIndexAcrossLayers() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {

            LayerStackNode stack = new LayerStackBuilder()
                    .name("ExplicitZIndexStack")
                    .layer(new SpacerNode("Bottom", 100.0, 80.0, DocumentInsets.zero(), DocumentInsets.zero()),
                            LayerAlign.TOP_LEFT, 5)
                    .layer(new SpacerNode("Middle", 100.0, 80.0, DocumentInsets.zero(), DocumentInsets.zero()),
                            LayerAlign.TOP_LEFT, 1)
                    .layer(new SpacerNode("Top", 100.0, 80.0, DocumentInsets.zero(), DocumentInsets.zero()),
                            LayerAlign.TOP_LEFT, 10)
                    .build();

            session.add(stack);
            LayoutGraph graph = session.layoutGraph();

            // After sorting by zIndex we should see Middle (1), then Bottom (5),
            // then Top (10) in the placed nodes' source iteration.
            List<String> sortedNames = graph.nodes().stream()
                    .map(PlacedNode::semanticName)
                    .filter(name -> List.of("Bottom", "Middle", "Top").contains(name))
                    .toList();

            assertThat(sortedNames)
                    .describedAs("explicit zIndex must order layers by ascending zIndex regardless of source order")
                    .containsExactly("Middle", "Bottom", "Top");
        }
    }

    /**
     * Row weighted slot distribution: a row with two slots whose section
     * children use the full slot width should place those slots side-by-side
     * such that their leftmost x-coordinates differ by approximately the
     * left slot's width.
     */
    @Test
    void rowWithEqualWeightSlotsPlacesSlotsSideBySide() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 200)
                .margin(DocumentInsets.of(10))
                .create()) {

            session.compose(dsl -> dsl.pageFlow(flow -> flow
                    .name("WeightedRow")
                    .addRow(row -> row
                            .name("EqualSlots")
                            .weights(1.0, 1.0)
                            .addSection(left -> left
                                    .name("Left")
                                    .addText("Left side"))
                            .addSection(right -> right
                                    .name("Right")
                                    .addText("Right side")))));

            LayoutGraph graph = session.layoutGraph();

            PlacedNode left = graph.nodes().stream()
                    .filter(n -> "Left".equals(n.semanticName()))
                    .findFirst()
                    .orElseThrow();
            PlacedNode right = graph.nodes().stream()
                    .filter(n -> "Right".equals(n.semanticName()))
                    .findFirst()
                    .orElseThrow();

            // Both slots must share the same y baseline (placed in one row).
            assertThat(left.placementY())
                    .describedAs("row slots must share a y baseline")
                    .isEqualTo(right.placementY(), within(EPS));

            // Right slot must start to the right of the left slot.
            assertThat(right.placementX())
                    .describedAs("right slot must be positioned to the right of the left slot")
                    .isGreaterThan(left.placementX());

            // The horizontal gap between slot origins is approximately half
            // of the row's inner width (380pt) when weights are equal.
            double slotOriginGap = right.placementX() - left.placementX();
            assertThat(slotOriginGap)
                    .describedAs("with equal weights and inner width 380, slot-origin gap should be roughly 190pt")
                    .isGreaterThan(150.0)
                    .isLessThan(230.0);
        }
    }
}
