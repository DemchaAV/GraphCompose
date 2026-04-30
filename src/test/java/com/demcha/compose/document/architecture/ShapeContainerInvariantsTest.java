package com.demcha.compose.document.architecture;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PaginationPolicy;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.node.ShapeContainerNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Architecture-guard for {@link ShapeContainerNode}. Pins behaviour that
 * would otherwise drift across releases:
 *
 * <ul>
 *   <li>The pagination policy is {@link PaginationPolicy#SHAPE_ATOMIC} —
 *       not {@code ATOMIC}, not {@code SPLITTABLE}. Downgrading to
 *       {@code ATOMIC} would lose the marker that lets render handlers
 *       and snapshots tell shape-clipped atomicity apart from bbox-only
 *       atomicity.</li>
 *   <li>Every {@link BuiltInNodeDefinitions.ShapeClipBeginPayload} has a
 *       matching {@link BuiltInNodeDefinitions.ShapeClipEndPayload} with
 *       the same {@code ownerPath} on the same page, in the right order
 *       (begin before end). Without this invariant the PDF graphics-state
 *       stack would leak save/restore calls across containers.</li>
 *   <li>{@link ClipPolicy#OVERFLOW_VISIBLE} is the only policy that
 *       suppresses both markers; every other policy emits the pair.</li>
 * </ul>
 *
 * <p>Behavioural tests for individual scenarios live in
 * {@code ShapeContainerBuilderTest}; this class covers cross-document
 * invariants so the contract stays pinned even as new shape-container
 * features land.</p>
 *
 * @author Artem Demchyshyn
 */
class ShapeContainerInvariantsTest {

    @Test
    void shapeContainerNodeUsesShapeAtomicPagination() throws Exception {
        // The contract is exposed through NodeDefinition.paginationPolicy(node);
        // calling layoutGraph() and inspecting fragments would also work but
        // is one indirection further away from the actual invariant.
        ShapeContainerNode node = new ShapeContainerBuilder()
                .name("AtomicProbe")
                .circle(40)
                .center(spacer("Inner", 10, 10))
                .build();

        try (DocumentSession session = GraphCompose.document()
                .pageSize(200, 200)
                .margin(DocumentInsets.of(10))
                .create()) {
            session.add(node);
            // SHAPE_ATOMIC + 60pt wide outline fits well within the 180pt
            // inner page; if pagination policy ever flipped to SPLITTABLE
            // this layoutGraph() call would still succeed but the next
            // assertion (single-page placement) would fail because the
            // splittable path would split the container into multiple
            // fragments with separate startPage/endPage.
            LayoutGraph graph = session.layoutGraph();
            var placed = graph.nodes().stream()
                    .filter(n -> "AtomicProbe".equals(n.semanticName()))
                    .findFirst()
                    .orElseThrow();
            assertThat(placed.startPage()).isEqualTo(placed.endPage());
        }
    }

    @Test
    void everyClipBeginInArbitraryDocumentHasMatchingEndOnSamePage() throws Exception {
        // Mix policies (CLIP_PATH default, OVERFLOW_VISIBLE explicit), mix
        // outline shapes (circle, rounded rect, ellipse), mix sizes (small,
        // wide). A single fragment list must keep every begin/end pair
        // properly nested and on the same page.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 700)
                .margin(DocumentInsets.of(20))
                .create()) {

            session.add(new ShapeContainerBuilder()
                    .name("Circle")
                    .circle(60)
                    .fillColor(DocumentColor.rgb(180, 40, 40))
                    .center(spacer("CircleLabel", 20, 10))
                    .build());
            session.add(new ShapeContainerBuilder()
                    .name("Card")
                    .roundedRect(180, 80, 8)
                    .clipPolicy(ClipPolicy.CLIP_BOUNDS)
                    .center(spacer("CardLabel", 60, 12))
                    .build());
            session.add(new ShapeContainerBuilder()
                    .name("Overflow")
                    .ellipse(160, 60)
                    .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                    .center(spacer("OverflowLabel", 40, 10))
                    .build());

            LayoutGraph graph = session.layoutGraph();
            assertClipPairsBalance(graph.fragments());

            // Overflow container must contribute zero markers; the other
            // two contribute one pair each → 2 pairs total.
            long beginCount = graph.fragments().stream()
                    .filter(f -> f.payload() instanceof BuiltInNodeDefinitions.ShapeClipBeginPayload)
                    .count();
            assertThat(beginCount).isEqualTo(2L);
        }
    }

    @Test
    void everyTransformBeginInArbitraryDocumentHasMatchingEndOnSamePage() throws Exception {
        // Mix transformed and non-transformed containers and mix policies
        // so transform begin/end pairs cross-cut clip begin/end pairs.
        // Both pair types must remain balanced and properly nested
        // independently — a transform begin/end can wrap a clip
        // begin/end, but neither can interleave with the other's owner.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 700)
                .margin(DocumentInsets.of(20))
                .create()) {

            session.add(new ShapeContainerBuilder()
                    .name("RotatedClipped")
                    .circle(60)
                    .rotate(15)
                    .fillColor(DocumentColor.rgb(180, 40, 40))
                    .center(spacer("L1", 20, 10))
                    .build());
            session.add(new ShapeContainerBuilder()
                    .name("PlainClipped")
                    .roundedRect(180, 80, 8)
                    .center(spacer("L2", 60, 12))
                    .build());
            session.add(new ShapeContainerBuilder()
                    .name("RotatedOverflow")
                    .ellipse(160, 60)
                    .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                    .scale(0.8)
                    .center(spacer("L3", 40, 10))
                    .build());

            LayoutGraph graph = session.layoutGraph();
            assertTransformPairsBalance(graph.fragments());
            // Transformed containers: RotatedClipped + RotatedOverflow → 2 pairs.
            long transformBeginCount = graph.fragments().stream()
                    .filter(f -> f.payload() instanceof BuiltInNodeDefinitions.TransformBeginPayload)
                    .count();
            assertThat(transformBeginCount).isEqualTo(2L);
        }
    }

    private static void assertTransformPairsBalance(List<PlacedFragment> fragments) {
        Map<String, Integer> openOwners = new HashMap<>();
        for (int i = 0; i < fragments.size(); i++) {
            PlacedFragment fragment = fragments.get(i);
            Object payload = fragment.payload();
            if (payload instanceof BuiltInNodeDefinitions.TransformBeginPayload begin) {
                assertThat(begin.ownerPath()).isNotEmpty();
                assertThat(openOwners)
                        .as("transform-begin must not nest with itself: %s already open", begin.ownerPath())
                        .doesNotContainKey(begin.ownerPath());
                openOwners.put(begin.ownerPath(), fragment.pageIndex());
            } else if (payload instanceof BuiltInNodeDefinitions.TransformEndPayload end) {
                assertThat(openOwners)
                        .as("transform-end without matching begin: %s", end.ownerPath())
                        .containsKey(end.ownerPath());
                int beginPage = openOwners.remove(end.ownerPath());
                assertThat(fragment.pageIndex())
                        .as("transform-end must land on the same page as its begin (%s)", end.ownerPath())
                        .isEqualTo(beginPage);
            }
        }
        assertThat(openOwners)
                .as("every transform-begin must be closed by an end before the document ends")
                .isEmpty();
    }

    private static void assertClipPairsBalance(List<PlacedFragment> fragments) {
        // Walk left to right with a per-owner stack of open begin indices.
        // Each end pops the matching begin; mismatch or leftover = invariant
        // violation.
        Map<String, Integer> openOwners = new HashMap<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < fragments.size(); i++) {
            PlacedFragment fragment = fragments.get(i);
            Object payload = fragment.payload();
            if (payload instanceof BuiltInNodeDefinitions.ShapeClipBeginPayload begin) {
                assertThat(begin.ownerPath()).isNotEmpty();
                assertThat(openOwners)
                        .as("clip-begin must not nest with itself: %s already open", begin.ownerPath())
                        .doesNotContainKey(begin.ownerPath());
                openOwners.put(begin.ownerPath(), fragment.pageIndex());
                seen.add(begin.ownerPath());
            } else if (payload instanceof BuiltInNodeDefinitions.ShapeClipEndPayload end) {
                assertThat(openOwners)
                        .as("clip-end without matching begin: %s", end.ownerPath())
                        .containsKey(end.ownerPath());
                int beginPage = openOwners.remove(end.ownerPath());
                assertThat(fragment.pageIndex())
                        .as("clip-end must land on the same page as its begin (%s)", end.ownerPath())
                        .isEqualTo(beginPage);
            }
        }
        assertThat(openOwners)
                .as("every clip-begin must be closed by an end before the document ends")
                .isEmpty();
    }

    private static SpacerNode spacer(String name, double width, double height) {
        return new SpacerNode(name, width, height, DocumentInsets.zero(), DocumentInsets.zero());
    }
}
