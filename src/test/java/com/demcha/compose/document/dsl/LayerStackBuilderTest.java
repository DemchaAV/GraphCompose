package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class LayerStackBuilderTest {

    private static final double EPS = 1e-3;

    @Test
    void emptyLayerListIsRejected() {
        assertThatThrownBy(() -> new LayerStackBuilder().name("Empty").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one layer");
    }

    @Test
    void singleLayerStackTakesItsLayerSize() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {

            LayerStackNode stack = new LayerStackBuilder()
                    .name("SingleLayer")
                    .layer(new SpacerNode("Filler", 200.0, 80.0, DocumentInsets.zero(), DocumentInsets.zero()))
                    .build();

            session.add(stack);
            LayoutGraph graph = session.layoutGraph();

            PlacedNode stackNode = nodeWithSemanticName(graph, "SingleLayer");
            assertThat(stackNode.placementWidth()).isEqualTo(200.0, within(EPS));
            assertThat(stackNode.placementHeight()).isEqualTo(80.0, within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void backgroundPanelAndContentShareTheSameBoundingBox() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 320)
                .margin(DocumentInsets.of(20))
                .create()) {

            SpacerNode background = new SpacerNode(
                    "Background",
                    300.0,
                    120.0,
                    DocumentInsets.zero(),
                    DocumentInsets.zero());

            SpacerNode content = new SpacerNode(
                    "Content",
                    160.0,
                    60.0,
                    DocumentInsets.zero(),
                    DocumentInsets.zero());

            LayerStackNode stack = new LayerStackBuilder()
                    .name("HeroBanner")
                    .back(background)
                    .center(content)
                    .build();

            session.add(stack);
            LayoutGraph graph = session.layoutGraph();

            PlacedNode stackNode = nodeWithSemanticName(graph, "HeroBanner");
            assertThat(stackNode.placementWidth()).isEqualTo(300.0, within(EPS));
            assertThat(stackNode.placementHeight()).isEqualTo(120.0, within(EPS));

            PlacedNode bgNode = nodeWithSemanticName(graph, "Background");
            PlacedNode contentNode = nodeWithSemanticName(graph, "Content");

            assertThat(bgNode.placementX()).isEqualTo(stackNode.placementX(), within(EPS));
            assertThat(bgNode.placementY()).isEqualTo(stackNode.placementY(), within(EPS));
            assertThat(bgNode.placementWidth()).isEqualTo(300.0, within(EPS));
            assertThat(bgNode.placementHeight()).isEqualTo(120.0, within(EPS));

            assertThat(contentNode.placementX())
                    .isEqualTo(stackNode.placementX() + (300.0 - 160.0) / 2.0, within(EPS));
            assertThat(contentNode.placementY())
                    .isEqualTo(stackNode.placementY() + (120.0 - 60.0) / 2.0, within(EPS));
            assertThat(contentNode.placementWidth()).isEqualTo(160.0, within(EPS));
            assertThat(contentNode.placementHeight()).isEqualTo(60.0, within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void topRightLayerSitsAtUpperRightCorner() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 320)
                .margin(DocumentInsets.of(20))
                .create()) {

            SpacerNode background = new SpacerNode("Bg", 300.0, 120.0,
                    DocumentInsets.zero(), DocumentInsets.zero());
            SpacerNode badge = new SpacerNode("Badge", 40.0, 24.0,
                    DocumentInsets.zero(), DocumentInsets.zero());

            LayerStackNode stack = new LayerStackBuilder()
                    .name("BadgeOverPanel")
                    .layer(background, LayerAlign.TOP_LEFT)
                    .layer(badge, LayerAlign.TOP_RIGHT)
                    .build();

            session.add(stack);
            LayoutGraph graph = session.layoutGraph();

            PlacedNode stackNode = nodeWithSemanticName(graph, "BadgeOverPanel");
            PlacedNode badgeNode = nodeWithSemanticName(graph, "Badge");

            double stackTopY = stackNode.placementY() + stackNode.placementHeight();
            double badgeTopY = badgeNode.placementY() + badgeNode.placementHeight();
            double stackRightX = stackNode.placementX() + stackNode.placementWidth();
            double badgeRightX = badgeNode.placementX() + badgeNode.placementWidth();

            assertThat(badgeTopY).isEqualTo(stackTopY, within(EPS));
            assertThat(badgeRightX).isEqualTo(stackRightX, within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void bottomLeftLayerSitsAtLowerLeftCorner() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 320)
                .margin(DocumentInsets.of(20))
                .create()) {

            SpacerNode background = new SpacerNode("Bg", 280.0, 100.0,
                    DocumentInsets.zero(), DocumentInsets.zero());
            SpacerNode signature = new SpacerNode("Sig", 60.0, 18.0,
                    DocumentInsets.zero(), DocumentInsets.zero());

            LayerStackNode stack = new LayerStackBuilder()
                    .name("SignedPanel")
                    .layer(background, LayerAlign.TOP_LEFT)
                    .layer(signature, LayerAlign.BOTTOM_LEFT)
                    .build();

            session.add(stack);
            LayoutGraph graph = session.layoutGraph();

            PlacedNode stackNode = nodeWithSemanticName(graph, "SignedPanel");
            PlacedNode sig = nodeWithSemanticName(graph, "Sig");

            assertThat(sig.placementX()).isEqualTo(stackNode.placementX(), within(EPS));
            assertThat(sig.placementY()).isEqualTo(stackNode.placementY(), within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void layersArePaintedInSourceOrder() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 320)
                .margin(DocumentInsets.of(20))
                .create()) {

            SpacerNode behind = new SpacerNode("Behind", 200.0, 60.0,
                    DocumentInsets.zero(), DocumentInsets.zero());
            SpacerNode middle = new SpacerNode("Middle", 200.0, 60.0,
                    DocumentInsets.zero(), DocumentInsets.zero());
            SpacerNode front = new SpacerNode("Front", 200.0, 60.0,
                    DocumentInsets.zero(), DocumentInsets.zero());

            LayerStackNode stack = new LayerStackBuilder()
                    .name("ZOrder")
                    .layer(behind)
                    .layer(middle)
                    .layer(front)
                    .build();

            session.add(stack);
            LayoutGraph graph = session.layoutGraph();

            PlacedNode stackNode = nodeWithSemanticName(graph, "ZOrder");
            List<PlacedNode> stackChildren = graph.nodes().stream()
                    .filter(n -> stackNode.path().equals(n.parentPath()))
                    .toList();

            assertThat(stackChildren).hasSize(3);
            assertThat(stackChildren).extracting(PlacedNode::semanticName)
                    .containsExactly("Behind", "Middle", "Front");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void alignmentShortcutsPlaceLayersAtTheRightCorner() {
        verifyShortcutPlacement("TopLeft", LayerStackBuilder::topLeft, 0.0, 0.0);
        verifyShortcutPlacement("TopCenter", LayerStackBuilder::topCenter, 0.5, 0.0);
        verifyShortcutPlacement("TopRight", LayerStackBuilder::topRight, 1.0, 0.0);
        verifyShortcutPlacement("CenterLeft", LayerStackBuilder::centerLeft, 0.0, 0.5);
        verifyShortcutPlacement("Center", LayerStackBuilder::center, 0.5, 0.5);
        verifyShortcutPlacement("CenterRight", LayerStackBuilder::centerRight, 1.0, 0.5);
        verifyShortcutPlacement("BottomLeft", LayerStackBuilder::bottomLeft, 0.0, 1.0);
        verifyShortcutPlacement("BottomCenter", LayerStackBuilder::bottomCenter, 0.5, 1.0);
        verifyShortcutPlacement("BottomRight", LayerStackBuilder::bottomRight, 1.0, 1.0);
    }

    private static void verifyShortcutPlacement(
            String label,
            BiConsumer<LayerStackBuilder, com.demcha.compose.document.node.DocumentNode> shortcut,
            double xFraction,
            double yFraction) {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 320)
                .margin(DocumentInsets.of(20))
                .create()) {

            SpacerNode background = new SpacerNode(label + "-Bg", 300.0, 120.0,
                    DocumentInsets.zero(), DocumentInsets.zero());
            SpacerNode child = new SpacerNode(label + "-Child", 60.0, 24.0,
                    DocumentInsets.zero(), DocumentInsets.zero());

            LayerStackBuilder stack = new LayerStackBuilder()
                    .name(label + "-Stack")
                    .layer(background, LayerAlign.TOP_LEFT);
            shortcut.accept(stack, child);

            session.add(stack.build());
            LayoutGraph graph = session.layoutGraph();

            PlacedNode stackNode = nodeWithSemanticName(graph, label + "-Stack");
            PlacedNode childNode = nodeWithSemanticName(graph, label + "-Child");

            double expectedX = stackNode.placementX()
                    + (stackNode.placementWidth() - childNode.placementWidth()) * xFraction;
            double expectedY = stackNode.placementY()
                    + (stackNode.placementHeight() - childNode.placementHeight()) * (1.0 - yFraction);

            assertThat(childNode.placementX())
                    .as("%s: x", label)
                    .isEqualTo(expectedX, within(EPS));
            assertThat(childNode.placementY())
                    .as("%s: y", label)
                    .isEqualTo(expectedY, within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void stackPaddingShrinksLayerBoundingBox() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 320)
                .margin(DocumentInsets.of(20))
                .create()) {

            SpacerNode background = new SpacerNode("Bg", 200.0, 100.0,
                    DocumentInsets.zero(), DocumentInsets.zero());
            SpacerNode badge = new SpacerNode("Badge", 30.0, 18.0,
                    DocumentInsets.zero(), DocumentInsets.zero());

            LayerStackNode stack = new LayerStackBuilder()
                    .name("PaddedStack")
                    .padding(8.0)
                    .layer(background, LayerAlign.TOP_LEFT)
                    .layer(badge, LayerAlign.TOP_RIGHT)
                    .build();

            session.add(stack);
            LayoutGraph graph = session.layoutGraph();

            PlacedNode stackNode = nodeWithSemanticName(graph, "PaddedStack");
            PlacedNode bg = nodeWithSemanticName(graph, "Bg");
            PlacedNode badgeNode = nodeWithSemanticName(graph, "Badge");

            assertThat(bg.placementX()).isEqualTo(stackNode.placementX() + 8.0, within(EPS));
            assertThat(badgeNode.placementX() + badgeNode.placementWidth())
                    .isEqualTo(stackNode.placementX() + stackNode.placementWidth() - 8.0, within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static PlacedNode nodeWithSemanticName(LayoutGraph graph, String name) {
        return graph.nodes().stream()
                .filter(n -> name.equals(n.semanticName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No PlacedNode with semantic name: " + name));
    }
}
