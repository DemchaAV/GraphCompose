package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.CanvasChild;
import com.demcha.compose.document.node.CanvasLayerNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Behavioural test suite for the v1.6 Phase C
 * {@link CanvasLayerNode} — explicit (x, y) placement of children
 * inside a fixed-size bounding box.
 */
class CanvasLayerBuilderTest {

    private static final double EPS = 1e-3;

    @Test
    void zeroOrNegativeDimensionsAreRejected() {
        assertThatThrownBy(() -> new CanvasLayerBuilder(0, 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CanvasLayerBuilder(100, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CanvasLayerBuilder(100, 100).size(0, 50))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void canvasReservesItsExplicitBoundingBoxRegardlessOfChildren() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(440, 320)
                .margin(DocumentInsets.of(20))
                .create()) {

            CanvasLayerNode canvas = new CanvasLayerBuilder(360, 200)
                    .name("FixedCanvas")
                    .position(new SpacerNode("Tiny", 10.0, 10.0,
                            DocumentInsets.zero(), DocumentInsets.zero()), 5, 5)
                    .build();

            session.add(canvas);
            LayoutGraph graph = session.layoutGraph();

            PlacedNode canvasNode = nodeWithSemanticName(graph, "FixedCanvas");
            assertThat(canvasNode.placementWidth()).isEqualTo(360.0, within(EPS));
            assertThat(canvasNode.placementHeight()).isEqualTo(200.0, within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void positionPlacesChildAtExplicitTopLeftRelativeCoordinates() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(440, 320)
                .margin(DocumentInsets.of(20))
                .create()) {

            SpacerNode badge = new SpacerNode("Badge", 40.0, 30.0,
                    DocumentInsets.zero(), DocumentInsets.zero());
            CanvasLayerNode canvas = new CanvasLayerBuilder(360, 200)
                    .name("BadgeCanvas")
                    .position(badge, 50, 30)
                    .build();

            session.add(canvas);
            LayoutGraph graph = session.layoutGraph();

            PlacedNode canvasNode = nodeWithSemanticName(graph, "BadgeCanvas");
            PlacedNode badgeNode = nodeWithSemanticName(graph, "Badge");

            // Canvas (0, 0) is top-left. Badge at (50, 30) →
            // x = canvas.x + 50, y = canvas.y + (height - 30 - badgeHeight)
            // because PDF y grows up; we anchor TOP_LEFT and offsetY=30
            // means 30pt down from top.
            assertThat(badgeNode.placementX())
                    .isEqualTo(canvasNode.placementX() + 50.0, within(EPS));
            assertThat(badgeNode.placementY())
                    .isEqualTo(canvasNode.placementY() + canvasNode.placementHeight()
                            - 30.0 - badgeNode.placementHeight(), within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void multipleChildrenLandAtIndependentCoordinates() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(500, 360)
                .margin(DocumentInsets.of(20))
                .create()) {

            CanvasLayerNode canvas = new CanvasLayerBuilder(400, 240)
                    .name("MultiCanvas")
                    .position(new SpacerNode("Topbar", 360.0, 24.0,
                            DocumentInsets.zero(), DocumentInsets.zero()), 0, 0)
                    .position(new SpacerNode("Center", 60.0, 40.0,
                            DocumentInsets.zero(), DocumentInsets.zero()), 170, 100)
                    .position(new SpacerNode("Corner", 80.0, 30.0,
                            DocumentInsets.zero(), DocumentInsets.zero()), 300, 200)
                    .build();

            session.add(canvas);
            LayoutGraph graph = session.layoutGraph();

            PlacedNode canvasNode = nodeWithSemanticName(graph, "MultiCanvas");
            PlacedNode topbar = nodeWithSemanticName(graph, "Topbar");
            PlacedNode center = nodeWithSemanticName(graph, "Center");
            PlacedNode corner = nodeWithSemanticName(graph, "Corner");

            // Topbar at (0, 0) — top-left of canvas.
            assertThat(topbar.placementX()).isEqualTo(canvasNode.placementX(), within(EPS));
            assertThat(topbar.placementY()).isEqualTo(
                    canvasNode.placementY() + canvasNode.placementHeight() - topbar.placementHeight(),
                    within(EPS));

            // Center at (170, 100).
            assertThat(center.placementX()).isEqualTo(canvasNode.placementX() + 170, within(EPS));
            assertThat(center.placementY()).isEqualTo(
                    canvasNode.placementY() + canvasNode.placementHeight() - 100 - center.placementHeight(),
                    within(EPS));

            // Corner at (300, 200).
            assertThat(corner.placementX()).isEqualTo(canvasNode.placementX() + 300, within(EPS));
            assertThat(corner.placementY()).isEqualTo(
                    canvasNode.placementY() + canvasNode.placementHeight() - 200 - corner.placementHeight(),
                    within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void clipPolicyDefaultsToClipBoundsAndIsConfigurable() {
        CanvasLayerNode defaultCanvas = new CanvasLayerBuilder(200, 100)
                .name("Default")
                .position(new SpacerNode("Inside", 10, 10,
                        DocumentInsets.zero(), DocumentInsets.zero()), 5, 5)
                .build();
        assertThat(defaultCanvas.clipPolicy()).isEqualTo(ClipPolicy.CLIP_BOUNDS);

        CanvasLayerNode visibleCanvas = new CanvasLayerBuilder(200, 100)
                .name("Visible")
                .position(new SpacerNode("Overflow", 100, 100,
                        DocumentInsets.zero(), DocumentInsets.zero()), 150, 90)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .build();
        assertThat(visibleCanvas.clipPolicy()).isEqualTo(ClipPolicy.OVERFLOW_VISIBLE);
    }

    @Test
    void positionRecordValidatesNullsAndNonFiniteCoordinates() {
        SpacerNode child = new SpacerNode("Child", 10, 10,
                DocumentInsets.zero(), DocumentInsets.zero());
        assertThatThrownBy(() -> new CanvasChild(null, 0, 0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CanvasChild(child, Double.NaN, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CanvasChild(child, 0, Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void canvasNodeChildrenReturnsPlacedNodesInSourceOrder() {
        SpacerNode a = new SpacerNode("A", 10, 10,
                DocumentInsets.zero(), DocumentInsets.zero());
        SpacerNode b = new SpacerNode("B", 10, 10,
                DocumentInsets.zero(), DocumentInsets.zero());
        SpacerNode c = new SpacerNode("C", 10, 10,
                DocumentInsets.zero(), DocumentInsets.zero());

        CanvasLayerNode canvas = new CanvasLayerBuilder(100, 100)
                .position(a, 0, 0)
                .position(b, 20, 20)
                .position(c, 40, 40)
                .build();

        assertThat(canvas.children()).containsExactly(a, b, c);
        assertThat(canvas.placements()).extracting(CanvasChild::node)
                .containsExactly(a, b, c);
    }

    @Test
    void addCanvasShortcutPlacesChildrenViaPageFlowDsl() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(440, 320)
                .margin(DocumentInsets.of(20))
                .create()) {

            session.pageFlow()
                    .name("FlowRoot")
                    .addCanvas(360, 200, canvas -> canvas
                            .name("FlowCanvas")
                            .position(new SpacerNode("FlowBadge", 40, 30,
                                    DocumentInsets.zero(), DocumentInsets.zero()), 100, 50))
                    .build();

            LayoutGraph graph = session.layoutGraph();
            PlacedNode canvasNode = nodeWithSemanticName(graph, "FlowCanvas");
            PlacedNode badge = nodeWithSemanticName(graph, "FlowBadge");

            assertThat(canvasNode.placementWidth()).isEqualTo(360.0, within(EPS));
            assertThat(badge.placementX())
                    .isEqualTo(canvasNode.placementX() + 100.0, within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void canvasLayoutMatchesSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(440, 320)
                .margin(DocumentInsets.of(20))
                .create()) {

            session.pageFlow()
                    .name("CanvasSnapshotRoot")
                    .addCanvas(360, 200, canvas -> canvas
                            .name("SnapshotCanvas")
                            .position(new SpacerNode("Topbar", 360, 24,
                                    DocumentInsets.zero(), DocumentInsets.zero()), 0, 0)
                            .position(new SpacerNode("Center", 60, 40,
                                    DocumentInsets.zero(), DocumentInsets.zero()), 150, 80))
                    .build();

            LayoutSnapshotAssertions.assertMatches(session, "document/canvas_layer_basic");
        }
    }

    private static PlacedNode nodeWithSemanticName(LayoutGraph graph, String name) {
        return graph.nodes().stream()
                .filter(n -> name.equals(n.semanticName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No PlacedNode with semantic name: " + name));
    }
}
