package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShapeContainerLayoutSnapshotTest {
    private static final DocumentColor TEAL = DocumentColor.rgb(20, 60, 75);
    private static final DocumentColor GOLD = DocumentColor.rgb(196, 153, 76);
    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);

    @Test
    void circleWithTextMatchesLayoutSnapshot() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(260, 180)
                .margin(DocumentInsets.of(18))
                .create()) {
            composeCircleWithText(document);

            LayoutSnapshotAssertions.assertMatches(document, "shape-container/circle_with_text");
        }
    }

    @Test
    void ellipseWithOverlayMatchesLayoutSnapshot() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(320, 210)
                .margin(DocumentInsets.of(20))
                .create()) {
            composeEllipseWithOverlay(document);

            LayoutSnapshotAssertions.assertMatches(document, "shape-container/ellipse_with_overlay");
        }
    }

    @Test
    void layoutGraphCarriesClipMarkersAroundShapeContainerLayers() {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(260, 180)
                .margin(DocumentInsets.of(18))
                .create()) {
            composeCircleWithText(document);

            LayoutGraph graph = document.layoutGraph();
            List<PlacedFragment> fragments = graph.fragments();
            int beginIndex = indexOfPayload(fragments, BuiltInNodeDefinitions.ShapeClipBeginPayload.class);
            int endIndex = indexOfPayload(fragments, BuiltInNodeDefinitions.ShapeClipEndPayload.class);

            assertThat(beginIndex).as("clip-begin marker").isGreaterThanOrEqualTo(0);
            assertThat(endIndex).as("clip-end marker").isGreaterThan(beginIndex);

            BuiltInNodeDefinitions.ShapeClipBeginPayload begin =
                    (BuiltInNodeDefinitions.ShapeClipBeginPayload) fragments.get(beginIndex).payload();
            BuiltInNodeDefinitions.ShapeClipEndPayload end =
                    (BuiltInNodeDefinitions.ShapeClipEndPayload) fragments.get(endIndex).payload();

            assertThat(begin.policy()).isEqualTo(ClipPolicy.CLIP_PATH);
            assertThat(end.ownerPath()).isEqualTo(begin.ownerPath());
            assertThat(fragments.get(endIndex).pageIndex()).isEqualTo(fragments.get(beginIndex).pageIndex());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void composeCircleWithText(DocumentSession document) {
        document.pageFlow()
                .name("ShapeContainerCircleFixture")
                .spacing(8)
                .addCircle(92, TEAL, circle -> circle
                        .name("InitialCircle")
                        .padding(10)
                        .stroke(DocumentStroke.of(GOLD, 1.2))
                        .center(label("GC", 22, DocumentColor.WHITE, true))
                        .position(label("CLIP", 7, GOLD, true), 0, -10, LayerAlign.BOTTOM_CENTER))
                .addParagraph(paragraph -> paragraph
                        .name("CircleCaption")
                        .text("Circle container keeps children clipped to the outline.")
                        .textStyle(style(8.5, INK, false))
                        .lineSpacing(1.2)
                        .margin(DocumentInsets.zero()))
                .build();
    }

    private static void composeEllipseWithOverlay(DocumentSession document) {
        document.pageFlow()
                .name("ShapeContainerEllipseFixture")
                .spacing(10)
                .addEllipse(170, 86, GOLD, ellipse -> ellipse
                        .name("OffsetEllipse")
                        .padding(9)
                        .stroke(DocumentStroke.of(TEAL, 1.0))
                        .center(label("Overlay", 15, INK, true))
                        .topRight(new ShapeBuilder()
                                .name("BadgePill")
                                .size(42, 18)
                                .fillColor(TEAL)
                                .cornerRadius(9)
                                .build())
                        .position(label("TOP", 7, DocumentColor.WHITE, true), -6, 5, LayerAlign.TOP_RIGHT))
                .addParagraph(paragraph -> paragraph
                        .name("EllipseCaption")
                        .text("Nine-point anchors and screen-space offsets are stable in snapshots.")
                        .textStyle(style(8.5, INK, false))
                        .lineSpacing(1.2)
                        .margin(DocumentInsets.zero()))
                .build();
    }

    private static com.demcha.compose.document.node.DocumentNode label(String text,
                                                                       double size,
                                                                       DocumentColor color,
                                                                       boolean bold) {
        return new ParagraphBuilder()
                .text(text)
                .textStyle(style(size, color, bold))
                .align(TextAlign.CENTER)
                .margin(DocumentInsets.zero())
                .build();
    }

    private static DocumentTextStyle style(double size, DocumentColor color, boolean bold) {
        return DocumentTextStyle.builder()
                .fontName(bold ? FontName.HELVETICA_BOLD : FontName.HELVETICA)
                .decoration(bold ? DocumentTextDecoration.BOLD : DocumentTextDecoration.DEFAULT)
                .size(size)
                .color(color)
                .build();
    }

    private static int indexOfPayload(List<PlacedFragment> fragments, Class<?> payloadType) {
        for (int i = 0; i < fragments.size(); i++) {
            if (payloadType.isInstance(fragments.get(i).payload())) {
                return i;
            }
        }
        return -1;
    }
}
