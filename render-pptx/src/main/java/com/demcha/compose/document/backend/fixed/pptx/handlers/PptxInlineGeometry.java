package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.document.style.ShapePoint;
import com.demcha.compose.engine.components.content.shape.Stroke;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFFreeformShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/** Shared native-DrawingML geometry used by inline shape and SVG spans. */
final class PptxInlineGeometry {

    private PptxInlineGeometry() {
    }

    static void drawOutline(XSLFSlide slide,
                            ShapeOutline outline,
                            Rectangle2D box,
                            Color fill,
                            Stroke stroke) {
        if (outline instanceof ShapeOutline.Rectangle) {
            drawPreset(slide, ShapeType.RECT, box, fill, stroke);
        } else if (outline instanceof ShapeOutline.RoundedRectangle rounded) {
            XSLFAutoShape shape = drawPreset(slide, ShapeType.ROUND_RECT, box, fill, stroke);
            PptxShapeStyle.applyRoundRectAdjust(shape, rounded.cornerRadius(),
                    box.getWidth(), box.getHeight());
        } else if (outline instanceof ShapeOutline.RoundedRectanglePerCorner rounded) {
            PptxCapabilityNotes.perCornerRadiiApproximated();
            XSLFAutoShape shape = drawPreset(slide, ShapeType.ROUND_RECT, box, fill, stroke);
            PptxShapeStyle.applyRoundRectAdjust(shape, rounded.corners().topLeft(),
                    box.getWidth(), box.getHeight());
        } else if (outline instanceof ShapeOutline.Ellipse) {
            drawPreset(slide, ShapeType.ELLIPSE, box, fill, stroke);
        } else if (outline instanceof ShapeOutline.Polygon polygon) {
            drawPath(slide, polygonPath(polygon.points(), box), fill, stroke);
        } else if (outline instanceof ShapeOutline.Path path) {
            drawPath(slide, path(path.segments(), box), fill, stroke);
        }
    }

    static XSLFFreeformShape drawPath(XSLFSlide slide, Path2D path, Color fill, Stroke stroke) {
        XSLFFreeformShape shape = slide.createFreeform();
        shape.setPath(path);
        PptxShapeStyle.applyFill(shape, fill);
        PptxShapeStyle.applyStroke(shape, stroke);
        return shape;
    }

    static Path2D path(List<DocumentPathSegment> segments, Rectangle2D box) {
        Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        for (DocumentPathSegment segment : segments) {
            if (segment instanceof DocumentPathSegment.MoveTo move) {
                path.moveTo(x(box, move.x()), y(box, move.y()));
            } else if (segment instanceof DocumentPathSegment.LineTo line) {
                path.lineTo(x(box, line.x()), y(box, line.y()));
            } else if (segment instanceof DocumentPathSegment.CubicTo curve) {
                path.curveTo(
                        x(box, curve.control1X()), y(box, curve.control1Y()),
                        x(box, curve.control2X()), y(box, curve.control2Y()),
                        x(box, curve.x()), y(box, curve.y()));
            } else if (segment instanceof DocumentPathSegment.Close) {
                path.closePath();
            }
        }
        return path;
    }

    private static Path2D polygonPath(List<ShapePoint> points, Rectangle2D box) {
        Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        for (int i = 0; i < points.size(); i++) {
            ShapePoint point = points.get(i);
            if (i == 0) {
                path.moveTo(x(box, point.x()), y(box, point.y()));
            } else {
                path.lineTo(x(box, point.x()), y(box, point.y()));
            }
        }
        path.closePath();
        return path;
    }

    private static XSLFAutoShape drawPreset(XSLFSlide slide,
                                            ShapeType type,
                                            Rectangle2D box,
                                            Color fill,
                                            Stroke stroke) {
        XSLFAutoShape shape = slide.createAutoShape();
        shape.setShapeType(type);
        shape.setAnchor(box);
        PptxShapeStyle.applyFill(shape, fill);
        PptxShapeStyle.applyStroke(shape, stroke);
        return shape;
    }

    private static double x(Rectangle2D box, double normalized) {
        return box.getX() + normalized * box.getWidth();
    }

    private static double y(Rectangle2D box, double normalized) {
        return box.getY() + (1.0 - normalized) * box.getHeight();
    }
}
