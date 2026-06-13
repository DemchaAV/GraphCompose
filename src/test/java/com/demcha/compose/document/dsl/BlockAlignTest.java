package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Block-level horizontal alignment: a fixed-size node wrapped in
 * {@link com.demcha.compose.document.node.AlignNode} seats LEFT / CENTER /
 * RIGHT within the full content width, where the bare flow would always
 * left-align it.
 */
class BlockAlignTest {

    private static final double EPS = 0.5;
    private static final double PAGE = 300;
    private static final double MARGIN = 20;
    private static final double CONTENT = PAGE - 2 * MARGIN;   // 260
    private static final double ICON = 40;

    /** Places one 40pt square path under the given alignment, returns its x. */
    private double placedX(HorizontalAlign align) {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(PAGE, PAGE)
                .margin(DocumentInsets.of(MARGIN))
                .create()) {
            session.dsl().pageFlow().name("Flow")
                    .addAligned(align, square())
                    .build();
            LayoutGraph graph = session.layoutGraph();
            PlacedNode square = graph.nodes().stream()
                    .filter(n -> n.path().contains("Square"))
                    .findFirst().orElseThrow();
            return square.placementX();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void leftKeepsTheNodeAtTheContentLeftEdge() {
        assertThat(placedX(HorizontalAlign.LEFT)).isCloseTo(MARGIN, within(EPS));
    }

    @Test
    void centerPlacesTheNodeInTheMiddleOfTheContentWidth() {
        double expected = MARGIN + (CONTENT - ICON) / 2.0;   // 20 + 110 = 130
        assertThat(placedX(HorizontalAlign.CENTER)).isCloseTo(expected, within(EPS));
    }

    @Test
    void rightPushesTheNodeToTheContentRightEdge() {
        double expected = MARGIN + (CONTENT - ICON);          // 20 + 220 = 240
        assertThat(placedX(HorizontalAlign.RIGHT)).isCloseTo(expected, within(EPS));
    }

    @Test
    void alignedDocumentRendersToPdf() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(PAGE, PAGE)
                .margin(DocumentInsets.of(MARGIN))
                .create()) {
            session.dsl().pageFlow().name("Flow")
                    .addAligned(HorizontalAlign.CENTER, square())
                    .build();
            byte[] pdf = session.toPdfBytes();
            assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        }
    }

    @Test
    void svgIconAlignOverloadCentresTheIcon() throws Exception {
        var icon = com.demcha.compose.document.svg.SvgIcon.parse(
                "<svg viewBox=\"0 0 24 24\"><path d=\"M0 0 H24 V24 Z\" fill=\"#123456\"/></svg>");
        try (DocumentSession session = GraphCompose.document()
                .pageSize(PAGE, PAGE)
                .margin(DocumentInsets.of(MARGIN))
                .create()) {
            session.dsl().pageFlow(page -> page.addSvgIcon(icon, ICON, HorizontalAlign.CENTER));
            LayoutGraph graph = session.layoutGraph();
            PlacedNode iconNode = graph.nodes().stream()
                    .filter(n -> n.path().contains("SvgIcon"))
                    .findFirst().orElseThrow();
            assertThat(iconNode.placementX())
                    .isCloseTo(MARGIN + (CONTENT - ICON) / 2.0, within(EPS));
        }
    }

    private static com.demcha.compose.document.node.DocumentNode square() {
        return new PathBuilder()
                .name("Square")
                .size(ICON, ICON)
                .moveTo(0, 0).lineTo(1, 0).lineTo(1, 1).lineTo(0, 1).closePath()
                .fillColor(DocumentColor.rgb(20, 80, 95))
                .stroke(DocumentStroke.of(DocumentColor.rgb(0, 0, 0), 1))
                .build();
    }
}
