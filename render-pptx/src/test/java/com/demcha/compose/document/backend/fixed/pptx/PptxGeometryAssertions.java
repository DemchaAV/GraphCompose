package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.EllipseFragmentPayload;
import com.demcha.compose.document.layout.payloads.LineFragmentPayload;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import org.apache.poi.util.Units;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Geometry-identity assertions for the fixed-layout PPTX backend: reopens the
 * emitted bytes with POI and checks every shape anchor against the resolved
 * {@link LayoutGraph} the backend consumed. Expected anchors are re-derived
 * here from payload semantics (independently of the handlers), so a handler
 * mapping bug cannot cancel itself out in the comparison.
 */
final class PptxGeometryAssertions {

    /** Anchor tolerance in points — well under half a point. */
    static final double EPSILON_PT = 0.5;

    private PptxGeometryAssertions() {
    }

    static void assertGeometryMatches(LayoutGraph graph, byte[] pptxBytes) throws IOException {
        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptxBytes))) {
            assertThat(show.getSlides()).hasSize(Math.max(graph.totalPages(), 1));

            long expectedCx = Units.toEMU(graph.canvas().width());
            long expectedCy = Units.toEMU(graph.canvas().height());
            assertThat(show.getCTPresentation().getSldSz().getCx()).isEqualTo((int) expectedCx);
            assertThat(show.getCTPresentation().getSldSz().getCy()).isEqualTo((int) expectedCy);

            Map<Integer, List<Rectangle2D.Double>> expectedByPage = expectedAnchors(graph);
            assertThat(expectedByPage)
                    .as("the graph must contain drawable fragments — an empty expectation set proves nothing")
                    .isNotEmpty();
            for (int pageIndex = 0; pageIndex < show.getSlides().size(); pageIndex++) {
                List<Rectangle2D.Double> expected =
                        expectedByPage.getOrDefault(pageIndex, List.of());
                XSLFSlide slide = show.getSlides().get(pageIndex);
                List<XSLFShape> shapes = slide.getShapes();
                assertThat(shapes)
                        .as("slide %d shape count", pageIndex)
                        .hasSize(expected.size());
                for (int i = 0; i < expected.size(); i++) {
                    Rectangle2D actual = shapes.get(i).getAnchor();
                    Rectangle2D.Double want = expected.get(i);
                    assertThat(actual.getX()).as("slide %d shape %d x", pageIndex, i)
                            .isCloseTo(want.x, org.assertj.core.data.Offset.offset(EPSILON_PT));
                    assertThat(actual.getY()).as("slide %d shape %d y", pageIndex, i)
                            .isCloseTo(want.y, org.assertj.core.data.Offset.offset(EPSILON_PT));
                    assertThat(actual.getWidth()).as("slide %d shape %d width", pageIndex, i)
                            .isCloseTo(want.width, org.assertj.core.data.Offset.offset(EPSILON_PT));
                    assertThat(actual.getHeight()).as("slide %d shape %d height", pageIndex, i)
                            .isCloseTo(want.height, org.assertj.core.data.Offset.offset(EPSILON_PT));
                }
            }
        }
    }

    /**
     * Re-derives the expected shape anchors per page, in fragment order:
     * shape/ellipse anchors are the fragment box flipped into top-down space;
     * a line's anchor is the bounding box of its two flipped endpoints.
     */
    private static Map<Integer, List<Rectangle2D.Double>> expectedAnchors(LayoutGraph graph) {
        double canvasHeight = graph.canvas().height();
        Map<Integer, List<Rectangle2D.Double>> byPage = new TreeMap<>();
        for (PlacedFragment fragment : graph.fragments()) {
            Object payload = fragment.payload();
            Rectangle2D.Double anchor = null;
            if (payload instanceof ShapeFragmentPayload || payload instanceof EllipseFragmentPayload) {
                anchor = boxAnchor(canvasHeight, fragment);
            } else if (payload instanceof LineFragmentPayload line) {
                anchor = lineAnchor(canvasHeight, fragment, line);
            }
            if (anchor != null) {
                byPage.computeIfAbsent(fragment.pageIndex(), ignored -> new ArrayList<>()).add(anchor);
            }
        }
        return byPage;
    }

    private static Rectangle2D.Double boxAnchor(double canvasHeight, PlacedFragment fragment) {
        return new Rectangle2D.Double(
                fragment.x(),
                canvasHeight - fragment.y() - fragment.height(),
                fragment.width(),
                fragment.height());
    }

    private static Rectangle2D.Double lineAnchor(double canvasHeight,
                                                 PlacedFragment fragment,
                                                 LineFragmentPayload line) {
        double x1 = fragment.x() + line.startX();
        double y1 = canvasHeight - (fragment.y() + line.startY());
        double x2 = fragment.x() + line.endX();
        double y2 = canvasHeight - (fragment.y() + line.endY());
        return new Rectangle2D.Double(
                Math.min(x1, x2),
                Math.min(y1, y2),
                Math.abs(x2 - x1),
                Math.abs(y2 - y1));
    }
}
