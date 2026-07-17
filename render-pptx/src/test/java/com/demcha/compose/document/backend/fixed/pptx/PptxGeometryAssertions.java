package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.*;
import com.demcha.compose.document.node.ExternalLinkTarget;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.document.backend.fixed.pdf.PdfMeasurementResources;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.FontFamilyDefinition;
import org.apache.poi.util.Units;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFFreeformShape;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Path2D;
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

    static void assertTextGeometryMatches(LayoutGraph graph, byte[] pptxBytes) throws Exception {
        assertTextGeometryMatches(graph, pptxBytes, List.of());
    }

    static void assertTextGeometryMatches(LayoutGraph graph,
                                          byte[] pptxBytes,
                                          List<FontFamilyDefinition> customFonts) throws Exception {
        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptxBytes));
             PdfMeasurementResources measurement = PdfMeasurementResources.open(customFonts)) {
            List<ExpectedTextLine> expected = new ArrayList<>();
            List<ExpectedLinkHotspot> expectedLinks = new ArrayList<>();
            for (PlacedFragment fragment : graph.fragments()) {
                if (!(fragment.payload() instanceof ParagraphFragmentPayload payload)) {
                    continue;
                }
                double innerX = fragment.x() + payload.padding().left();
                double innerWidth = fragment.width() - payload.padding().horizontal();
                double cursorTop = fragment.y() + fragment.height() - payload.padding().top();
                for (ParagraphLine line : payload.lines()) {
                    double baselineY = cursorTop - line.lineHeight() + line.baselineOffsetFromBottom();
                    baselineY += verticalSeatShift(line, measurement, payload.verticalAlign());
                    double lineX = switch (payload.align()) {
                        case RIGHT -> innerX + innerWidth - line.width();
                        case CENTER -> innerX + (innerWidth - line.width()) / 2.0;
                        case LEFT -> innerX;
                    };
                    double linkCursorX = lineX;
                    for (ParagraphSpan span : line.spans()) {
                        if (spanLinkTarget(span) instanceof ExternalLinkTarget external
                                && span.width() > 0) {
                            expectedLinks.add(new ExpectedLinkHotspot(fragment.pageIndex(),
                                    new Rectangle2D.Double(linkCursorX,
                                            graph.canvas().height() - cursorTop,
                                            span.width(), line.lineHeight()),
                                    external.options().uri()));
                        }
                        linkCursorX += span.width();
                    }
                    if (payload.linkTarget() instanceof ExternalLinkTarget paragraphLink
                            && line.width() > 0 && !line.spans().isEmpty()) {
                        expectedLinks.add(new ExpectedLinkHotspot(fragment.pageIndex(),
                                new Rectangle2D.Double(lineX,
                                        graph.canvas().height() - cursorTop,
                                        line.width(), line.lineHeight()),
                                paragraphLink.options().uri()));
                    }
                    boolean usesAbsoluteSpans = mixesFontSizes(line)
                            || line.spans().stream().anyMatch(span ->
                            !(span instanceof ParagraphTextSpan textSpan) || textSpan.background() != null);
                    if (!usesAbsoluteSpans && !line.spans().isEmpty()) {
                        StringBuilder text = new StringBuilder();
                        for (var span : line.spans()) {
                            if (span instanceof ParagraphTextSpan textSpan) {
                                PdfFont font = measurement.fontLibrary()
                                        .getFont(textSpan.textStyle().fontName(), PdfFont.class).orElseThrow();
                                text.append(font.sanitizeForRender(textSpan.textStyle(), textSpan.text()));
                            }
                        }
                        expected.add(new ExpectedTextLine(fragment.pageIndex(),
                                new Rectangle2D.Double(lineX,
                                        graph.canvas().height() - baselineY - line.textAscent(),
                                        line.width() + 0.1, line.textLineHeight()),
                                "GraphCompose Text Line", text.toString()));
                    }

                    if (usesAbsoluteSpans) {
                        double cursorX = lineX;
                        for (var span : line.spans()) {
                            if (span instanceof ParagraphTextSpan textSpan) {
                                PdfFont font = measurement.fontLibrary()
                                        .getFont(textSpan.textStyle().fontName(), PdfFont.class).orElseThrow();
                                String sanitized = font.sanitizeForRender(
                                        textSpan.textStyle(), textSpan.text());
                                PdfFont.VerticalMetrics metrics = font.verticalMetrics(textSpan.textStyle());
                                double top = graph.canvas().height() - baselineY - metrics.ascent();
                                if (textSpan.background() == null) {
                                    expected.add(new ExpectedTextLine(fragment.pageIndex(),
                                            new Rectangle2D.Double(cursorX, top,
                                                    Math.max(0.1, textSpan.width() + 0.1),
                                                    Math.max(0.1, metrics.lineHeight())),
                                            "GraphCompose Inline Text Span", sanitized));
                                } else if (!sanitized.isEmpty()) {
                                    DocumentInsets padding = textSpan.background().padding();
                                    expected.add(new ExpectedTextLine(fragment.pageIndex(),
                                            new Rectangle2D.Double(cursorX + padding.left(), top,
                                                    Math.max(0.1,
                                                            textSpan.width() - padding.horizontal()),
                                                    Math.max(0.1, metrics.lineHeight())),
                                            "GraphCompose Inline Chip Text", sanitized));
                                }
                            }
                            cursorX += span.width();
                        }
                    }
                    cursorTop -= line.lineHeight() + payload.lineGap();
                }
            }

            List<ActualTextLine> actual = new ArrayList<>();
            for (int page = 0; page < show.getSlides().size(); page++) {
                for (XSLFShape shape : show.getSlides().get(page).getShapes()) {
                    if (shape instanceof XSLFTextBox textBox
                            && isMeasuredTextShape(textBox.getShapeName())) {
                        actual.add(new ActualTextLine(page, textBox.getAnchor(),
                                textBox.getShapeName(), textBox.getText()));
                    }
                }
            }
            assertThat(actual).hasSize(expected.size());
            for (int i = 0; i < expected.size(); i++) {
                ExpectedTextLine want = expected.get(i);
                ActualTextLine got = actual.get(i);
                assertThat(got.pageIndex).isEqualTo(want.pageIndex);
                assertThat(got.shapeName).as("text shape %d name", i).isEqualTo(want.shapeName);
                assertRectangle(got.anchor, want.anchor, "text line " + i);
                assertThat(got.text).as("text line %d sanitized content", i).isEqualTo(want.text);
            }

            List<ActualLinkHotspot> actualLinks = new ArrayList<>();
            for (int page = 0; page < show.getSlides().size(); page++) {
                for (XSLFShape shape : show.getSlides().get(page).getShapes()) {
                    if (shape instanceof XSLFAutoShape autoShape
                            && "GraphCompose Text Link Hotspot".equals(shape.getShapeName())) {
                        assertThat(autoShape.getHyperlink())
                                .as("external link hotspot on slide %d", page)
                                .isNotNull();
                        assertThat(autoShape.getFillColor()).isNotNull();
                        actualLinks.add(new ActualLinkHotspot(page, autoShape.getAnchor(),
                                autoShape.getHyperlink().getAddress(),
                                autoShape.getFillColor().getAlpha()));
                    }
                }
            }
            assertThat(actualLinks).hasSize(expectedLinks.size());
            for (int i = 0; i < expectedLinks.size(); i++) {
                ExpectedLinkHotspot want = expectedLinks.get(i);
                ActualLinkHotspot got = actualLinks.get(i);
                assertThat(got.pageIndex).isEqualTo(want.pageIndex);
                assertRectangle(got.anchor, want.anchor, "external link hotspot " + i);
                assertThat(got.address).isEqualTo(want.address);
                assertThat(got.alpha).as("external link hotspot %d transparency", i).isZero();
            }
        }
    }

    /**
     * Checks chip, image, and inline vector anchors for paragraph-only
     * fixtures. It deliberately ignores text frames, then compares the
     * remaining slide shapes to span geometry re-derived from the graph.
     */
    static void assertInlineSpanAnchorsMatch(LayoutGraph graph, byte[] pptxBytes) throws Exception {
        assertInlineSpanAnchorsMatch(graph, pptxBytes, List.of());
    }

    static void assertInlineSpanAnchorsMatch(LayoutGraph graph,
                                             byte[] pptxBytes,
                                             List<FontFamilyDefinition> customFonts) throws Exception {
        List<ExpectedInlineShape> expected = new ArrayList<>();
        try (PdfMeasurementResources measurement = PdfMeasurementResources.open(customFonts)) {
            for (PlacedFragment fragment : graph.fragments()) {
                if (!(fragment.payload() instanceof ParagraphFragmentPayload payload)) {
                    continue;
                }
                double innerX = fragment.x() + payload.padding().left();
                double innerWidth = fragment.width() - payload.padding().horizontal();
                double cursorTop = fragment.y() + fragment.height() - payload.padding().top();
                for (ParagraphLine line : payload.lines()) {
                    double baselineY = cursorTop - line.lineHeight() + line.baselineOffsetFromBottom();
                    baselineY += verticalSeatShift(line, measurement, payload.verticalAlign());
                    double lineX = switch (payload.align()) {
                        case RIGHT -> innerX + innerWidth - line.width();
                        case CENTER -> innerX + (innerWidth - line.width()) / 2.0;
                        case LEFT -> innerX;
                    };
                    double cursorX = lineX;
                    for (ParagraphSpan span : line.spans()) {
                        if (span instanceof ParagraphTextSpan text && text.background() != null) {
                            DocumentInsets padding = text.background().padding();
                            double bottom = baselineY - line.baselineOffsetFromBottom() - padding.bottom();
                            double height = line.textLineHeight() + padding.vertical();
                            expected.add(new ExpectedInlineShape(fragment.pageIndex(), XSLFAutoShape.class,
                                    new Rectangle2D.Double(cursorX,
                                            graph.canvas().height() - bottom - height,
                                            text.width(), height)));
                        } else if (span instanceof ParagraphImageSpan image) {
                            double bottom = inlineBottom(image.height(), image.alignment(),
                                    image.baselineOffset(), baselineY, line);
                            expected.add(new ExpectedInlineShape(fragment.pageIndex(), XSLFPictureShape.class,
                                    new Rectangle2D.Double(cursorX,
                                            graph.canvas().height() - bottom - image.height(),
                                            image.width(), image.height())));
                        } else if (span instanceof ParagraphShapeSpan shape) {
                            double bottom = inlineBottom(shape.height(), shape.alignment(),
                                    shape.baselineOffset(), baselineY, line);
                            for (ResolvedShapeLayer layer : shape.layers()) {
                                ShapeOutline outline = layer.outline();
                                Class<? extends XSLFShape> type = outline instanceof ShapeOutline.Polygon
                                        || outline instanceof ShapeOutline.Path
                                        ? XSLFFreeformShape.class : XSLFAutoShape.class;
                                Rectangle2D.Double box = new Rectangle2D.Double(
                                        cursorX + (shape.width() - outline.width()) / 2.0,
                                        graph.canvas().height() - bottom
                                                - (shape.height() + outline.height()) / 2.0,
                                        outline.width(), outline.height());
                                expected.add(new ExpectedInlineShape(fragment.pageIndex(), type,
                                        type == XSLFFreeformShape.class
                                                ? freeformBounds(outline, box) : box));
                            }
                        } else if (span instanceof ParagraphSvgSpan svg) {
                            double bottom = inlineBottom(svg.height(), svg.alignment(),
                                    svg.baselineOffset(), baselineY, line);
                            Rectangle2D anchor = new Rectangle2D.Double(cursorX,
                                    graph.canvas().height() - bottom - svg.height(),
                                    svg.width(), svg.height());
                            // This helper's fixtures use full-box simple SVG paths. Complex SVG
                            // fallback is covered independently by PptxInlineSvgRasterizerTest.
                            for (ResolvedSvgLayer layer : svg.layers()) {
                                expected.add(new ExpectedInlineShape(fragment.pageIndex(),
                                        XSLFFreeformShape.class,
                                        pathBounds(layer.segments(), anchor)));
                            }
                        }
                        cursorX += span.width();
                    }
                    cursorTop -= line.lineHeight() + payload.lineGap();
                }
            }
        }

        List<ActualInlineShape> actual = new ArrayList<>();
        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptxBytes))) {
            for (int page = 0; page < show.getSlides().size(); page++) {
                for (XSLFShape shape : show.getSlides().get(page).getShapes()) {
                    if (!(shape instanceof XSLFTextBox)) {
                        actual.add(new ActualInlineShape(page, shape.getClass(), shape.getAnchor()));
                    }
                }
            }
        }
        assertThat(actual).hasSize(expected.size());
        for (int i = 0; i < expected.size(); i++) {
            ExpectedInlineShape want = expected.get(i);
            ActualInlineShape got = actual.get(i);
            assertThat(got.pageIndex).isEqualTo(want.pageIndex);
            assertThat(want.type).as("inline shape %d type", i).isAssignableFrom(got.type);
            assertRectangle(got.anchor, want.anchor, "inline shape " + i);
        }
    }

    private static Rectangle2D freeformBounds(ShapeOutline outline, Rectangle2D box) {
        if (outline instanceof ShapeOutline.Path path) {
            return pathBounds(path.segments(), box);
        }
        if (outline instanceof ShapeOutline.Polygon polygon) {
            Path2D.Double path = new Path2D.Double();
            for (int i = 0; i < polygon.points().size(); i++) {
                var point = polygon.points().get(i);
                double x = box.getX() + point.x() * box.getWidth();
                double y = box.getY() + (1.0 - point.y()) * box.getHeight();
                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            path.closePath();
            return path.getBounds2D();
        }
        return box;
    }

    private static Rectangle2D pathBounds(List<com.demcha.compose.document.style.DocumentPathSegment> segments,
                                          Rectangle2D box) {
        Path2D.Double path = new Path2D.Double();
        for (var segment : segments) {
            if (segment instanceof com.demcha.compose.document.style.DocumentPathSegment.MoveTo move) {
                path.moveTo(pathX(box, move.x()), pathY(box, move.y()));
            } else if (segment instanceof com.demcha.compose.document.style.DocumentPathSegment.LineTo line) {
                path.lineTo(pathX(box, line.x()), pathY(box, line.y()));
            } else if (segment instanceof com.demcha.compose.document.style.DocumentPathSegment.CubicTo curve) {
                path.curveTo(pathX(box, curve.control1X()), pathY(box, curve.control1Y()),
                        pathX(box, curve.control2X()), pathY(box, curve.control2Y()),
                        pathX(box, curve.x()), pathY(box, curve.y()));
            } else if (segment instanceof com.demcha.compose.document.style.DocumentPathSegment.Close) {
                path.closePath();
            }
        }
        return path.getBounds2D();
    }

    private static double pathX(Rectangle2D box, double normalized) {
        return box.getX() + normalized * box.getWidth();
    }

    private static double pathY(Rectangle2D box, double normalized) {
        return box.getY() + (1.0 - normalized) * box.getHeight();
    }

    /** Mirrors the handler's shared-frame gate: mixed plain-run sizes force per-span frames. */
    private static boolean mixesFontSizes(ParagraphLine line) {
        double size = Double.NaN;
        for (ParagraphSpan span : line.spans()) {
            if (span instanceof ParagraphTextSpan textSpan && textSpan.background() == null) {
                double spanSize = textSpan.textStyle().size();
                if (Double.isNaN(size)) {
                    size = spanSize;
                } else if (spanSize != size) {
                    return true;
                }
            }
        }
        return false;
    }

    private static com.demcha.compose.document.node.DocumentLinkTarget spanLinkTarget(ParagraphSpan span) {
        if (span instanceof ParagraphTextSpan textSpan) {
            return textSpan.linkTarget();
        }
        if (span instanceof ParagraphImageSpan imageSpan) {
            return imageSpan.linkTarget();
        }
        if (span instanceof ParagraphShapeSpan shapeSpan) {
            return shapeSpan.linkTarget();
        }
        if (span instanceof ParagraphSvgSpan svgSpan) {
            return svgSpan.linkTarget();
        }
        return null;
    }

    private static boolean isMeasuredTextShape(String name) {
        return "GraphCompose Text Line".equals(name)
                || "GraphCompose Inline Text Span".equals(name)
                || "GraphCompose Inline Chip Text".equals(name);
    }

    private static double verticalSeatShift(ParagraphLine line,
                                            PdfMeasurementResources measurement,
                                            TextVerticalAlign align) {
        if (align == TextVerticalAlign.DEFAULT) {
            return 0;
        }
        for (var span : line.spans()) {
            if (span instanceof ParagraphTextSpan textSpan) {
                PdfFont font = measurement.fontLibrary()
                        .getFont(textSpan.textStyle().fontName(), PdfFont.class).orElseThrow();
                double capHeight = font.getCapHeight(textSpan.textStyle());
                double descent = line.baselineOffsetFromBottom();
                double leading = Math.max(0,
                        line.textLineHeight() - line.textAscent() - descent);
                double capTopToBoxTop = line.textAscent() + leading - capHeight;
                return switch (align) {
                    case TOP -> capTopToBoxTop;
                    case CENTER -> (capTopToBoxTop - descent) / 2.0;
                    case BOTTOM -> -descent;
                    case DEFAULT -> 0;
                };
            }
        }
        return 0;
    }

    private static double inlineBottom(double height,
                                       InlineImageAlignment alignment,
                                       double baselineOffset,
                                       double baselineY,
                                       ParagraphLine line) {
        double lineBottom = baselineY - line.baselineOffsetFromBottom();
        double base = switch (alignment == null ? InlineImageAlignment.CENTER : alignment) {
            case BASELINE -> baselineY;
            case CENTER -> lineBottom + (line.lineHeight() - height) / 2.0;
            case TEXT_TOP -> baselineY + line.textAscent() - height;
            case TEXT_BOTTOM -> lineBottom;
        };
        return base + baselineOffset;
    }

    private static void assertRectangle(Rectangle2D actual, Rectangle2D expected, String label) {
        assertThat(actual.getX()).as(label + " x").isCloseTo(expected.getX(),
                org.assertj.core.data.Offset.offset(EPSILON_PT));
        assertThat(actual.getY()).as(label + " y").isCloseTo(expected.getY(),
                org.assertj.core.data.Offset.offset(EPSILON_PT));
        assertThat(actual.getWidth()).as(label + " width").isCloseTo(expected.getWidth(),
                org.assertj.core.data.Offset.offset(EPSILON_PT));
        assertThat(actual.getHeight()).as(label + " height").isCloseTo(expected.getHeight(),
                org.assertj.core.data.Offset.offset(EPSILON_PT));
    }

    private record ExpectedTextLine(int pageIndex,
                                    Rectangle2D anchor,
                                    String shapeName,
                                    String text) {
    }

    private record ActualTextLine(int pageIndex,
                                  Rectangle2D anchor,
                                  String shapeName,
                                  String text) {
    }

    private record ExpectedLinkHotspot(int pageIndex,
                                       Rectangle2D anchor,
                                       String address) {
    }

    private record ActualLinkHotspot(int pageIndex,
                                     Rectangle2D anchor,
                                     String address,
                                     int alpha) {
    }

    private record ExpectedInlineShape(int pageIndex,
                                       Class<? extends XSLFShape> type,
                                       Rectangle2D anchor) {
    }

    private record ActualInlineShape(int pageIndex,
                                     Class<?> type,
                                     Rectangle2D anchor) {
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
