package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.exceptions.AtomicNodeTooLargeException;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.ShapeContainerNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.ShapeOutline;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ShapeContainerBuilderTest {

    private static final double EPS = 1e-3;
    private static final DocumentColor BRAND = DocumentColor.rgb(180, 40, 40);

    @Test
    void containerWithoutOutlineFailsAtBuild() {
        assertThatThrownBy(() -> new ShapeContainerBuilder()
                .name("Bad")
                .layer(spacer("X", 10, 10))
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Bad")
                .hasMessageContaining("outline");
    }

    @Test
    void containerRequiresAtLeastOneLayer() {
        assertThatThrownBy(() -> new ShapeContainerBuilder()
                .name("Empty")
                .circle(40)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one layer");
    }

    @Test
    void circleContainerSizeIsDictatedByOutlineNotChildren() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {

            // Child intentionally smaller than outline; the container's bbox
            // should still match the circle, not max(child).
            SpacerNode label = spacer("CircleLabel", 30.0, 14.0);

            ShapeContainerNode container = new ShapeContainerBuilder()
                    .name("BrandCircle")
                    .circle(120.0)
                    .fillColor(BRAND)
                    .center(label)
                    .build();

            session.add(container);
            LayoutGraph graph = session.layoutGraph();

            PlacedNode containerNode = nodeWithSemanticName(graph, "BrandCircle");
            assertThat(containerNode.placementWidth()).isEqualTo(120.0, within(EPS));
            assertThat(containerNode.placementHeight()).isEqualTo(120.0, within(EPS));

            PlacedNode labelNode = nodeWithSemanticName(graph, "CircleLabel");
            assertThat(labelNode.placementX())
                    .as("label centred horizontally inside the circle")
                    .isEqualTo(containerNode.placementX() + (120.0 - 30.0) / 2.0, within(EPS));
            assertThat(labelNode.placementY())
                    .as("label centred vertically inside the circle (PDF y grows up)")
                    .isEqualTo(containerNode.placementY() + (120.0 - 14.0) / 2.0, within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void roundedRectangleContainerHonoursOutlineDimensions() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {

            SpacerNode badge = spacer("Badge", 24.0, 12.0);
            SpacerNode label = spacer("Label", 80.0, 16.0);

            ShapeContainerNode container = new ShapeContainerBuilder()
                    .name("Card")
                    .roundedRect(180.0, 90.0, 12.0)
                    .fillColor(DocumentColor.LIGHT_GRAY)
                    .clipPolicy(ClipPolicy.CLIP_BOUNDS)
                    .center(label)
                    .topRight(badge)
                    .build();

            session.add(container);
            LayoutGraph graph = session.layoutGraph();

            PlacedNode card = nodeWithSemanticName(graph, "Card");
            assertThat(card.placementWidth()).isEqualTo(180.0, within(EPS));
            assertThat(card.placementHeight()).isEqualTo(90.0, within(EPS));

            PlacedNode badgeNode = nodeWithSemanticName(graph, "Badge");
            double badgeRightEdge = badgeNode.placementX() + badgeNode.placementWidth();
            double badgeTopEdge = badgeNode.placementY() + badgeNode.placementHeight();
            double cardRightEdge = card.placementX() + card.placementWidth();
            double cardTopEdge = card.placementY() + card.placementHeight();
            assertThat(badgeRightEdge).isEqualTo(cardRightEdge, within(EPS));
            assertThat(badgeTopEdge).isEqualTo(cardTopEdge, within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void positionOffsetMovesLayerInScreenSpaceUnits() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {

            SpacerNode dot = spacer("CornerDot", 8.0, 8.0);

            ShapeContainerNode container = new ShapeContainerBuilder()
                    .name("OffsetDemo")
                    .roundedRect(120.0, 60.0, 6.0)
                    .position(dot, -6.0, -4.0, LayerAlign.BOTTOM_RIGHT)
                    .build();

            session.add(container);
            LayoutGraph graph = session.layoutGraph();

            PlacedNode shell = nodeWithSemanticName(graph, "OffsetDemo");
            PlacedNode dotPlaced = nodeWithSemanticName(graph, "CornerDot");

            double shellRight = shell.placementX() + shell.placementWidth();
            double dotRight = dotPlaced.placementX() + dotPlaced.placementWidth();
            // -6 right offset → 6pt to the left of the right edge.
            assertThat(dotRight).isEqualTo(shellRight - 6.0, within(EPS));
            // -4 down offset (positive Y in screen space goes down) →
            // dot bottom sits 4pt above the container bottom edge, which in
            // PDF coordinates means a higher Y value.
            assertThat(dotPlaced.placementY()).isEqualTo(shell.placementY() + 4.0, within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void addContainerFlowShortcutBuildsTheSameNodeTopology() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {

            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addCircle(60.0, BRAND, c -> c
                            .name("FlowCircle")
                            .center(spacer("FlowLabel", 30.0, 12.0)))
                    .build();

            LayoutGraph graph = session.layoutGraph();

            PlacedNode circle = nodeWithSemanticName(graph, "FlowCircle");
            assertThat(circle.placementWidth()).isEqualTo(60.0, within(EPS));
            assertThat(circle.placementHeight()).isEqualTo(60.0, within(EPS));
            assertThat(nodeWithSemanticName(graph, "FlowLabel")).isNotNull();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shapeContainerSplitsAtomicallyToNextPage() {
        // Page inner area is 180x180 (200x200 minus 10pt margins). A 90pt
        // tall warm-up shape fills half the inner height; a 120pt circle
        // ShapeContainer would overflow the remaining 90pt slice, so under
        // SHAPE_ATOMIC pagination it must move to page 2 as a single unit.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(200, 200)
                .margin(DocumentInsets.of(10))
                .create()) {

            session.add(new ShapeNode(
                    "WarmUp",
                    180,
                    100,
                    Color.LIGHT_GRAY,
                    DocumentStroke.of(DocumentColor.BLACK, 1),
                    DocumentInsets.zero(),
                    DocumentInsets.zero()));

            ShapeContainerNode atomicCircle = new ShapeContainerBuilder()
                    .name("AtomicCircle")
                    .circle(120.0)
                    .fillColor(BRAND)
                    .center(spacer("CircleLabel", 30.0, 14.0))
                    .build();
            session.add(atomicCircle);

            LayoutGraph graph = session.layoutGraph();

            assertThat(graph.totalPages()).isEqualTo(2);

            PlacedNode warmUp = nodeWithSemanticName(graph, "WarmUp");
            assertThat(warmUp.startPage()).isEqualTo(0);
            assertThat(warmUp.endPage()).isEqualTo(0);

            PlacedNode container = nodeWithSemanticName(graph, "AtomicCircle");
            PlacedNode label = nodeWithSemanticName(graph, "CircleLabel");
            assertThat(container.startPage()).isEqualTo(1);
            assertThat(container.endPage()).isEqualTo(1);
            assertThat(label.startPage())
                    .as("label travels with the outline — same page")
                    .isEqualTo(container.startPage());
            assertThat(label.endPage()).isEqualTo(container.endPage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void oversizedShapeContainerThrowsAtomicNodeTooLarge() {
        // Inner page is 180x180; the rounded rectangle is only 100pt wide
        // (so the width pre-check passes) but its 300pt height exceeds the
        // page capacity. Under SHAPE_ATOMIC the page-breaker must surface
        // AtomicNodeTooLargeException with the offending semantic name so
        // the caller can either shrink the outline or pick
        // ClipPolicy.OVERFLOW_VISIBLE.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(200, 200)
                .margin(DocumentInsets.of(10))
                .create()) {

            ShapeContainerNode oversized = new ShapeContainerBuilder()
                    .name("OversizedCard")
                    .roundedRect(100.0, 300.0, 6.0)
                    .fillColor(BRAND)
                    .center(spacer("L", 10.0, 10.0))
                    .build();
            session.add(oversized);

            assertThatThrownBy(session::layoutGraph)
                    .isInstanceOf(AtomicNodeTooLargeException.class)
                    .hasMessageContaining("OversizedCard");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static SpacerNode spacer(String name, double width, double height) {
        return new SpacerNode(name, width, height, DocumentInsets.zero(), DocumentInsets.zero());
    }

    private static PlacedNode nodeWithSemanticName(LayoutGraph graph, String name) {
        return graph.nodes().stream()
                .filter(n -> name.equals(n.semanticName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No PlacedNode with semantic name: " + name));
    }
}
