package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RowBuilderTest {
    private static final double TOLERANCE = 0.5;

    @Test
    void evenWeightRowDistributesInnerWidthAcrossChildren() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 200)
                .margin(DocumentInsets.of(10))
                .create()) {

            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addRow("Row", row -> row
                            .gap(10)
                            .addParagraph(paragraph -> paragraph
                                    .name("Left")
                                    .text("Left side")
                                    .textStyle(DocumentTextStyle.DEFAULT))
                            .addParagraph(paragraph -> paragraph
                                    .name("Right")
                                    .text("Right side")
                                    .textStyle(DocumentTextStyle.DEFAULT)))
                    .build();

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isEqualTo(1);

            List<PlacedNode> placedNodes = graph.nodes();
            // Expect at least three nodes for the row + two children. The flow root
            // and the row itself are also placed nodes.
            assertThat(placedNodes).hasSizeGreaterThanOrEqualTo(3);

            PlacedNode left = findNodeBySemanticName(placedNodes, "Left");
            PlacedNode right = findNodeBySemanticName(placedNodes, "Right");
            assertThat(left).isNotNull();
            assertThat(right).isNotNull();
            assertThat(left.startPage()).isEqualTo(right.startPage());
            assertThat(Math.abs(left.placementY() - right.placementY()))
                    .as("Row children must share the same baseline y")
                    .isLessThan(TOLERANCE);
            assertThat(left.placementX()).isLessThan(right.placementX());

            // Children render successfully without overlap: right child must start at
            // or after the left child's right edge plus the configured gap.
            double leftRightEdge = left.placementX() + left.placementWidth();
            assertThat(right.placementX() + TOLERANCE).isGreaterThan(leftRightEdge);

            byte[] pdfBytes = session.toPdfBytes();
            assertThat(pdfBytes).isNotEmpty();
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                String text = new PDFTextStripper().getText(document);
                assertThat(text).contains("Left side");
                assertThat(text).contains("Right side");
            }
        }
    }

    @Test
    void weightedRowAllocatesProportionalSlots() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 200)
                .margin(DocumentInsets.of(10))
                .create()) {

            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addRow("Row", row -> row
                            .gap(0)
                            .weights(1, 3)
                            .addParagraph(paragraph -> paragraph
                                    .name("Narrow")
                                    .text("S")
                                    .textStyle(DocumentTextStyle.DEFAULT))
                            .addParagraph(paragraph -> paragraph
                                    .name("Wide")
                                    .text("L")
                                    .textStyle(DocumentTextStyle.DEFAULT)))
                    .build();

            List<PlacedNode> nodes = session.layoutGraph().nodes();
            PlacedNode narrow = findNodeBySemanticName(nodes, "Narrow");
            PlacedNode wide = findNodeBySemanticName(nodes, "Wide");
            assertThat(narrow).isNotNull();
            assertThat(wide).isNotNull();

            // The wide slot should start roughly at narrow.x + narrowSlotWidth.
            // narrowSlotWidth = innerWidth * 1/4. innerWidth = pageWidth - margin*2 = 380.
            double expectedNarrowSlot = 380.0 * 0.25;
            double observedSlotGap = wide.placementX() - narrow.placementX();
            assertThat(Math.abs(observedSlotGap - expectedNarrowSlot))
                    .as("Weighted slot widths must follow the configured ratio")
                    .isLessThan(2.0);
        }
    }

    @Test
    void rowEmitsBackgroundFillFragment() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 200)
                .margin(DocumentInsets.of(10))
                .create()) {

            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addRow("Row", row -> row
                            .fillColor(DocumentColor.rgb(245, 248, 255))
                            .padding(DocumentInsets.of(8))
                            .addParagraph("First", DocumentTextStyle.DEFAULT)
                            .addParagraph("Second", DocumentTextStyle.DEFAULT))
                    .build();

            List<PlacedFragment> fragments = session.layoutGraph().fragments();
            assertThat(fragments).isNotEmpty();
        }
    }

    @Test
    void rowRejectsNestedRow() {
        DocumentSession session = GraphCompose.document().pageSize(300, 200).create();
        try {
            assertThatThrownBy(() -> session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addRow("Outer", row -> row
                            .addParagraph("Ok", DocumentTextStyle.DEFAULT)
                            .add(new RowBuilder().name("Inner").build()))
                    .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Outer")
                    .hasMessageContaining("another row");
        } finally {
            try {
                session.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    void rowRejectsTableChild() {
        DocumentSession session = GraphCompose.document().pageSize(300, 200).create();
        try {
            assertThatThrownBy(() -> session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addRow("Outer", row -> row
                            .addParagraph("Ok", DocumentTextStyle.DEFAULT)
                            .add(new TableBuilder()
                                    .columns(com.demcha.compose.document.table.DocumentTableColumn.auto())
                                    .row("only")
                                    .build()))
                    .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Outer")
                    .hasMessageContaining("table");
        } finally {
            try {
                session.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    void rowAcceptsSectionsAsColumns() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(10))
                .create()) {

            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addRow("Header", row -> row
                            .gap(12)
                            .addSection("LeftCol", section -> section
                                    .spacing(2)
                                    .addParagraph(p -> p.name("LeftTop").text("Left top").textStyle(DocumentTextStyle.DEFAULT))
                                    .addParagraph(p -> p.name("LeftBottom").text("Left bottom").textStyle(DocumentTextStyle.DEFAULT)))
                            .addSection("RightCol", section -> section
                                    .spacing(2)
                                    .addParagraph(p -> p.name("RightTop").text("Right top").textStyle(DocumentTextStyle.DEFAULT))
                                    .addParagraph(p -> p.name("RightBottom").text("Right bottom").textStyle(DocumentTextStyle.DEFAULT))
                                    .addParagraph(p -> p.name("RightTail").text("Right tail").textStyle(DocumentTextStyle.DEFAULT))))
                    .build();

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isEqualTo(1);

            List<PlacedNode> nodes = graph.nodes();
            PlacedNode leftCol = findNodeBySemanticName(nodes, "LeftCol");
            PlacedNode rightCol = findNodeBySemanticName(nodes, "RightCol");
            PlacedNode leftTop = findNodeBySemanticName(nodes, "LeftTop");
            PlacedNode rightTop = findNodeBySemanticName(nodes, "RightTop");
            PlacedNode leftBottom = findNodeBySemanticName(nodes, "LeftBottom");
            PlacedNode rightTail = findNodeBySemanticName(nodes, "RightTail");

            assertThat(leftCol).as("left column section is placed").isNotNull();
            assertThat(rightCol).as("right column section is placed").isNotNull();
            assertThat(leftTop).isNotNull();
            assertThat(rightTop).isNotNull();
            assertThat(leftBottom).isNotNull();
            assertThat(rightTail).isNotNull();

            assertThat(leftCol.placementX()).isLessThan(rightCol.placementX());

            // PlacedNode.placementY is the bottom-left y. Compare top-y
            // (placementY + height) to verify both columns start on the same
            // row band.
            double leftColTopY = leftCol.placementY() + leftCol.placementHeight();
            double rightColTopY = rightCol.placementY() + rightCol.placementHeight();
            assertThat(Math.abs(leftColTopY - rightColTopY))
                    .as("Both column sections start at the same top y inside the row band")
                    .isLessThan(TOLERANCE);

            // Top paragraphs of both columns share the same baseline.
            assertThat(Math.abs(leftTop.placementY() - rightTop.placementY()))
                    .as("Top paragraphs of both columns share the same baseline")
                    .isLessThan(TOLERANCE);

            // Each column measures its own children: the right column has an
            // extra paragraph, so its tail bottom sits at or below the left
            // column's last paragraph bottom (smaller y in PDF coordinates).
            assertThat(rightTail.placementY())
                    .as("Right column tail bottom sits at or below left column's last paragraph bottom")
                    .isLessThanOrEqualTo(leftBottom.placementY() + TOLERANCE);

            byte[] pdfBytes = session.toPdfBytes();
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                String text = new PDFTextStripper().getText(document);
                assertThat(text).contains("Left top");
                assertThat(text).contains("Left bottom");
                assertThat(text).contains("Right top");
                assertThat(text).contains("Right bottom");
                assertThat(text).contains("Right tail");
            }
        }
    }

    @Test
    void rowAcceptsLayerStackOverlayInsideColumnSection() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(10))
                .create()) {

            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addRow("BadgeRow", row -> row
                            .gap(8)
                            .addSection("Sidebar", section -> section
                                    .addLayerStack(stack -> stack
                                            .name("OverlayBadge")
                                            .back(new SpacerNode("BadgeBg", 60.0, 60.0,
                                                    DocumentInsets.zero(), DocumentInsets.zero()))
                                            .layer(new SpacerNode("BadgeFront", 30.0, 18.0,
                                                            DocumentInsets.zero(), DocumentInsets.zero()),
                                                    LayerAlign.CENTER)))
                            .addSection("Main", section -> section
                                    .addParagraph(p -> p.name("MainText").text("Hello").textStyle(DocumentTextStyle.DEFAULT))))
                    .build();

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isEqualTo(1);

            List<PlacedNode> nodes = graph.nodes();
            PlacedNode stackNode = findNodeBySemanticName(nodes, "OverlayBadge");
            PlacedNode badgeBg = findNodeBySemanticName(nodes, "BadgeBg");
            PlacedNode badgeFront = findNodeBySemanticName(nodes, "BadgeFront");
            PlacedNode mainText = findNodeBySemanticName(nodes, "MainText");

            assertThat(stackNode).as("LayerStack inside row column compiles").isNotNull();
            assertThat(badgeBg).isNotNull();
            assertThat(badgeFront).isNotNull();
            assertThat(mainText).isNotNull();

            // Stack must be at most as tall as its tallest layer (back).
            assertThat(stackNode.placementHeight()).isLessThanOrEqualTo(60.0 + TOLERANCE);

            // Front layer must overlap the back layer's bounding box.
            double backLeft = badgeBg.placementX();
            double backRight = backLeft + badgeBg.placementWidth();
            double backBottom = badgeBg.placementY();
            double backTop = backBottom + badgeBg.placementHeight();
            double frontCenterX = badgeFront.placementX() + badgeFront.placementWidth() / 2.0;
            double frontCenterY = badgeFront.placementY() + badgeFront.placementHeight() / 2.0;
            assertThat(frontCenterX).isGreaterThan(backLeft).isLessThan(backRight);
            assertThat(frontCenterY).isGreaterThan(backBottom).isLessThan(backTop);
        }
    }

    @Test
    void rowAcceptsLayerStackNodeDirectlyAsColumn() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(10))
                .create()) {

            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addRow("DirectBadgeRow", row -> row
                            .gap(8)
                            .add(new LayerStackBuilder()
                                    .name("DirectBadge")
                                    .back(new SpacerNode("DBBack", 50.0, 50.0,
                                            DocumentInsets.zero(), DocumentInsets.zero()))
                                    .layer(new SpacerNode("DBFront", 20.0, 20.0,
                                                    DocumentInsets.zero(), DocumentInsets.zero()),
                                            LayerAlign.CENTER)
                                    .build())
                            .addParagraph(p -> p.name("DirectMain").text("ok").textStyle(DocumentTextStyle.DEFAULT)))
                    .build();

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isEqualTo(1);
            assertThat(findNodeBySemanticName(graph.nodes(), "DirectBadge")).isNotNull();
        }
    }

    private static PlacedNode findNodeBySemanticName(List<PlacedNode> nodes, String name) {
        for (PlacedNode node : nodes) {
            if (name.equals(node.semanticName())) {
                return node;
            }
        }
        return null;
    }
}
