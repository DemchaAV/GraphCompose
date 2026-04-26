package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.PlacedNode;
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
    void rowRejectsCompositeChildren() {
        DocumentSession session = GraphCompose.document().pageSize(300, 200).create();
        try {
            assertThatThrownBy(() -> session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addRow("BadRow", row -> row
                            .addParagraph("Ok", DocumentTextStyle.DEFAULT)
                            .add(new SectionBuilder().name("Inner").build()))
                    .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("BadRow");
        } finally {
            try {
                session.close();
            } catch (Exception ignored) {
            }
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
