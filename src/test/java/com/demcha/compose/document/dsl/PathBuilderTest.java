package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.PathNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.svg.SvgPath;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DSL coverage for {@link PathBuilder}: fluent assembly into a
 * {@link PathNode}, node validation flowing through {@code build()}, and
 * end-to-end placement through {@code addPath(...)} on a page flow.
 */
class PathBuilderTest {

    @Test
    void builderAssemblesTheNode() {
        PathNode node = new PathBuilder()
                .name("Swoosh")
                .size(200, 80)
                .moveTo(0.0, 0.5)
                .curveTo(0.3, 1.1, 0.7, -0.1, 1.0, 0.5)
                .lineTo(1.0, 0.0)
                .closePath()
                .fillColor(DocumentColor.rgb(230, 240, 250))
                .stroke(DocumentStroke.of(DocumentColor.rgb(20, 60, 120), 1.5))
                .padding(DocumentInsets.of(2))
                .margin(DocumentInsets.bottom(6))
                .build();

        assertThat(node.name()).isEqualTo("Swoosh");
        assertThat(node.width()).isEqualTo(200);
        assertThat(node.height()).isEqualTo(80);
        assertThat(node.segments()).hasSize(4);
        assertThat(node.segments().get(0)).isInstanceOf(DocumentPathSegment.MoveTo.class);
        assertThat(node.segments().get(1)).isInstanceOf(DocumentPathSegment.CubicTo.class);
        assertThat(node.segments().get(3)).isInstanceOf(DocumentPathSegment.Close.class);
        assertThat(node.fillColor()).isNotNull();
        assertThat(node.stroke()).isNotNull();
        assertThat(node.padding().top()).isEqualTo(2);
        assertThat(node.margin().bottom()).isEqualTo(6);
    }

    @Test
    void dashedFlowsThroughToTheNode() {
        PathNode node = new PathBuilder()
                .size(100, 40)
                .moveTo(0.0, 0.5)
                .lineTo(1.0, 0.5)
                .stroke(DocumentStroke.of(DocumentColor.rgb(20, 60, 120), 1.0))
                .dashed(4, 2)
                .build();

        assertThat(node.dashPattern().segments()).containsExactly(4.0, 2.0);
    }

    @Test
    void svgBridgeAppendsParsedSegments() {
        PathNode node = new PathBuilder()
                .size(24, 24)
                .svg(SvgPath.parse("M0 0 L10 0 L5 8 Z", 0, 0, 10, 8))
                .fillColor(DocumentColor.rgb(196, 30, 58))
                .build();

        assertThat(node.segments()).hasSize(4);
        assertThat(node.segments().get(0))
                .isInstanceOf(DocumentPathSegment.MoveTo.class);
        assertThat(node.segments().get(3))
                .isInstanceOf(DocumentPathSegment.Close.class);
    }

    @Test
    void nodeValidationFlowsThroughBuild() {
        PathBuilder missingMoveTo = new PathBuilder()
                .size(100, 40)
                .lineTo(0.5, 0.5)
                .lineTo(1.0, 1.0);

        assertThatThrownBy(missingMoveTo::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must start with a MoveTo");
    }

    @Test
    void gradientPaintsFlowThroughToTheNode() {
        DocumentPaint axis = new DocumentPaint.LinearAxis(java.util.List.of(
                new DocumentPaint.Stop(0.0, DocumentColor.rgb(167, 139, 250)),
                new DocumentPaint.Stop(1.0, DocumentColor.rgb(97, 40, 217))),
                0.0, 0.0, 1.0, 1.0);

        PathNode node = new PathBuilder()
                .size(100, 40)
                .moveTo(0.0, 0.5)
                .lineTo(1.0, 0.5)
                .fill(axis)
                .stroke(DocumentStroke.of(DocumentColor.rgb(20, 60, 120), 2.0))
                .strokePaint(axis)
                .build();

        assertThat(node.fillPaint()).isSameAs(axis);
        assertThat(node.strokePaint()).isSameAs(axis);
    }

    @Test
    void strokePaintWithoutAStrokeFailsAtBuild() {
        PathBuilder builder = new PathBuilder()
                .size(100, 40)
                .moveTo(0.0, 0.5)
                .lineTo(1.0, 0.5)
                .strokePaint(DocumentPaint.linear(
                        DocumentColor.rgb(167, 139, 250), DocumentColor.rgb(97, 40, 217)));

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strokePaint needs a stroke");
    }

    @Test
    void addPathPlacesTheNodeInTheFlow() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(240, 200)
                .margin(DocumentInsets.of(12))
                .create()) {
            document.pageFlow(page -> page.addPath(path -> path
                    .name("Wavy")
                    .size(200, 60)
                    .moveTo(0.0, 0.5)
                    .curveTo(0.25, 1.0, 0.75, 0.0, 1.0, 0.5)
                    .stroke(DocumentStroke.of(DocumentColor.rgb(20, 60, 120), 2.0))));

            LayoutGraph graph = document.layoutGraph();
            assertThat(graph.nodes()).extracting(PlacedNode::path)
                    .anyMatch(path -> path.endsWith("Wavy[0]"));

            byte[] pdf = document.toPdfBytes();
            assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        }
    }
}
