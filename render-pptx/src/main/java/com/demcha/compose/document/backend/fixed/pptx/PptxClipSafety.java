package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.AnchorMarkerPayload;
import com.demcha.compose.document.layout.payloads.BookmarkMarkerPayload;
import com.demcha.compose.document.layout.payloads.BarcodeFragmentPayload;
import com.demcha.compose.document.layout.payloads.EllipseFragmentPayload;
import com.demcha.compose.document.layout.payloads.ImageFragmentPayload;
import com.demcha.compose.document.layout.payloads.LineFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.layout.payloads.ParagraphSpan;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.document.layout.payloads.PathFragmentPayload;
import com.demcha.compose.document.layout.payloads.PolygonFragmentPayload;
import com.demcha.compose.document.layout.payloads.ShapeClipBeginPayload;
import com.demcha.compose.document.layout.payloads.ShapeClipEndPayload;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.layout.payloads.SideBorders;
import com.demcha.compose.document.layout.payloads.TransformBeginPayload;
import com.demcha.compose.document.layout.payloads.TransformEndPayload;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.engine.components.content.shape.Stroke;

import java.util.List;

/**
 * Proves that a clip region cannot remove any ink — every child fragment sits
 * safely inside the clip outline — so the raster fallback can be skipped and
 * the region's children stay native, editable PowerPoint shapes. Most real
 * documents clip defensively (a rounded card whose padded content never
 * reaches the corners); rasterizing those regions costs editability for
 * nothing.
 *
 * <p>Conservative by construction: any doubt keeps the pixel-exact raster
 * path. Provable ink is modelled exactly — smooth strokes bleed half their
 * width past the fragment box, side borders likewise, unstroked polygon
 * fills and in-unit-box path fills stay inside the box, rasters draw inside
 * their box, and text gets a font-size-aware guard for glyph overhang.
 * Everything else is unprovable and rasters: polygon/path outlines as clip
 * shapes, stroked polygons and free paths (miter joins can spike far beyond
 * half the stroke width), paths whose segments leave the unit box (the
 * segment contract does not clamp them), table rows (border bleed is not
 * modelled), transforms inside the region, and unknown payload types.</p>
 */
final class PptxClipSafety {

    /** Base inset for text payloads, whose ink can exceed the measured boxes. */
    private static final double TEXT_GUARD = 2.0;

    /** Glyph ink overhang (italics, swashes) as a fraction of the font size. */
    private static final double GLYPH_OVERHANG_FACTOR = 0.15;

    /** Ink extent that cannot be modelled — always keeps the raster path. */
    private static final double UNPROVABLE = Double.POSITIVE_INFINITY;

    /**
     * Float-placement tolerance: ink exactly on the boundary is not cut, so an
     * exact-fit child (an SVG path spanning its clip box) still proves safe.
     */
    private static final double PLACEMENT_TOLERANCE = 0.01;

    private PptxClipSafety() {
    }

    /**
     * Returns {@code true} when every renderable fragment between the clip
     * markers provably stays inside the clip outline, so clipping is a visual
     * no-op.
     *
     * @param clip         the clip-begin payload (outline + policy)
     * @param clipFragment the placed clip fragment (absolute outline box)
     * @param fragments    the page's fragment list
     * @param beginIndex   index of the clip-begin marker
     * @param endIndex     index of the matching clip-end marker
     */
    static boolean clipCannotRemoveInk(ShapeClipBeginPayload clip,
                                       PlacedFragment clipFragment,
                                       List<PlacedFragment> fragments,
                                       int beginIndex,
                                       int endIndex) {
        for (int index = beginIndex + 1; index < endIndex; index++) {
            PlacedFragment child = fragments.get(index);
            Object payload = child.payload();
            if (payload instanceof TransformBeginPayload) {
                // A rotated or scaled composite can carry ink far outside its
                // children's untransformed boxes — never provable.
                return false;
            }
            if (payload instanceof TransformEndPayload
                    || payload instanceof ShapeClipBeginPayload
                    || payload instanceof ShapeClipEndPayload
                    || payload instanceof AnchorMarkerPayload
                    || payload instanceof BookmarkMarkerPayload) {
                // Markers draw nothing themselves; a nested clip's children
                // are checked individually right here, and if the verdict is
                // "no-op" the main loop re-dispatches the nested region on
                // its own merits.
                continue;
            }
            if (!safelyInside(clip, clipFragment, child)) {
                return false;
            }
        }
        return true;
    }

    private static boolean safelyInside(ShapeClipBeginPayload clip,
                                        PlacedFragment clipFragment,
                                        PlacedFragment child) {
        double guard = requiredInset(child.payload()) - PLACEMENT_TOLERANCE;
        if (Double.isNaN(guard)) {
            // A NaN stroke width would make every comparison below false-pass.
            return false;
        }
        double left = child.x() - clipFragment.x();
        double bottom = child.y() - clipFragment.y();
        double right = left + child.width();
        double top = bottom + child.height();
        double width = clipFragment.width();
        double height = clipFragment.height();
        if (left < guard || bottom < guard || right > width - guard || top > height - guard) {
            return false;
        }
        if (clip.policy() != ClipPolicy.CLIP_PATH) {
            // Bounds clipping is the plain rectangle the guard just proved.
            return true;
        }
        ShapeOutline outline = clip.outline();
        if (outline instanceof ShapeOutline.Rectangle) {
            return true;
        }
        if (outline instanceof ShapeOutline.RoundedRectangle rounded) {
            double radius = rounded.cornerRadius();
            return avoidsCornerSquares(left, bottom, right, top, width, height,
                    radius, radius, radius, radius, guard);
        }
        if (outline instanceof ShapeOutline.RoundedRectanglePerCorner rounded) {
            DocumentCornerRadius corners = rounded.corners();
            return avoidsCornerSquares(left, bottom, right, top, width, height,
                    corners.bottomLeft(), corners.bottomRight(),
                    corners.topLeft(), corners.topRight(), guard);
        }
        if (outline instanceof ShapeOutline.Ellipse) {
            // Minkowski: growing the ink box by the guard and testing against
            // the true ellipse is exact; shrinking the ellipse's axes is not.
            return insideEllipse(left - guard, bottom - guard, right + guard, top + guard,
                    width, height);
        }
        // Polygon and free-path outlines are not cheaply provable.
        return false;
    }

    /**
     * A rounded rectangle only removes ink inside the four corner squares
     * (side = that corner's radius); a child avoiding all of them — with the
     * guard — cannot be cut, even when it spans the full middle band.
     */
    private static boolean avoidsCornerSquares(double left, double bottom, double right, double top,
                                               double width, double height,
                                               double bottomLeft, double bottomRight,
                                               double topLeft, double topRight,
                                               double guard) {
        return !intersects(left, bottom, right, top,
                        0, 0, bottomLeft + guard, bottomLeft + guard)
                && !intersects(left, bottom, right, top,
                        width - bottomRight - guard, 0, width, bottomRight + guard)
                && !intersects(left, bottom, right, top,
                        0, height - topLeft - guard, topLeft + guard, height)
                && !intersects(left, bottom, right, top,
                        width - topRight - guard, height - topRight - guard, width, height);
    }

    /** Convexity: a box is inside the ellipse iff all four corners are. */
    private static boolean insideEllipse(double left, double bottom, double right, double top,
                                         double width, double height) {
        double a = width / 2.0;
        double b = height / 2.0;
        if (a <= 0 || b <= 0 || left > right || bottom > top) {
            return false;
        }
        return pointInsideEllipse(left, bottom, a, b, a, b)
                && pointInsideEllipse(right, bottom, a, b, a, b)
                && pointInsideEllipse(left, top, a, b, a, b)
                && pointInsideEllipse(right, top, a, b, a, b);
    }

    private static boolean pointInsideEllipse(double x, double y,
                                              double centerX, double centerY,
                                              double a, double b) {
        double dx = (x - centerX) / a;
        double dy = (y - centerY) / b;
        return dx * dx + dy * dy <= 1.0;
    }

    private static boolean intersects(double left, double bottom, double right, double top,
                                      double squareLeft, double squareBottom,
                                      double squareRight, double squareTop) {
        return left < squareRight && right > squareLeft
                && bottom < squareTop && top > squareBottom;
    }

    /**
     * The distance the child's ink may extend beyond its fragment box, or
     * {@link #UNPROVABLE} when it cannot be modelled. Smooth strokes and side
     * borders are centred on their geometry and bleed half their width;
     * mitred joins on stroked polygons and free paths can spike far beyond
     * that, so only their unstroked fills are provable — and a path fill only
     * when its segments (a conservative Bézier control-point hull) stay in
     * the unit box, since the segment contract does not clamp them. Rasters
     * draw inside their box; text gets a font-size-aware overhang guard.
     */
    private static double requiredInset(Object payload) {
        if (payload instanceof ShapeFragmentPayload shape) {
            double inset = halfWidth(shape.stroke());
            SideBorders borders = shape.sideBorders();
            if (borders != null) {
                inset = Math.max(inset, halfWidth(borders.top()));
                inset = Math.max(inset, halfWidth(borders.right()));
                inset = Math.max(inset, halfWidth(borders.bottom()));
                inset = Math.max(inset, halfWidth(borders.left()));
            }
            return inset;
        }
        if (payload instanceof EllipseFragmentPayload ellipse) {
            return halfWidth(ellipse.stroke());
        }
        if (payload instanceof PolygonFragmentPayload polygon) {
            return polygon.stroke() == null ? 0 : UNPROVABLE;
        }
        if (payload instanceof PathFragmentPayload path) {
            return path.stroke() == null && segmentsInsideUnitBox(path.segments())
                    ? 0
                    : UNPROVABLE;
        }
        if (payload instanceof LineFragmentPayload line) {
            // A projecting (square) cap on a diagonal line extends its corner
            // up to halfWidth·√2 per axis beyond the geometry; butt and round
            // caps stay within halfWidth.
            double factor = line.lineCap() == DocumentLineCap.SQUARE ? Math.sqrt(2.0) : 1.0;
            return halfWidth(line.stroke()) * factor;
        }
        if (payload instanceof ImageFragmentPayload || payload instanceof BarcodeFragmentPayload) {
            return 0;
        }
        if (payload instanceof ParagraphFragmentPayload paragraph) {
            return textGuard(paragraph);
        }
        // Table rows (border bleed unmodelled) and unknown payload types.
        return UNPROVABLE;
    }

    /**
     * Glyph ink can exceed the measured boxes — italic overhang and swashes
     * scale with the font size — so the guard grows with the largest run.
     */
    private static double textGuard(ParagraphFragmentPayload paragraph) {
        double maxSize = 0;
        for (ParagraphLine line : paragraph.lines()) {
            for (ParagraphSpan span : line.spans()) {
                if (span instanceof ParagraphTextSpan text) {
                    maxSize = Math.max(maxSize, text.textStyle().size());
                }
            }
        }
        return Math.max(TEXT_GUARD, GLYPH_OVERHANG_FACTOR * maxSize);
    }

    /**
     * Conservative unit-box containment: every explicit coordinate, including
     * Bézier control points (which bound the curve's hull), must stay inside
     * the normalized box. Any unknown future segment kind fails the proof.
     */
    private static boolean segmentsInsideUnitBox(List<DocumentPathSegment> segments) {
        for (DocumentPathSegment segment : segments) {
            if (segment instanceof DocumentPathSegment.MoveTo move) {
                if (!insideUnitBox(move.x(), move.y())) {
                    return false;
                }
            } else if (segment instanceof DocumentPathSegment.LineTo lineTo) {
                if (!insideUnitBox(lineTo.x(), lineTo.y())) {
                    return false;
                }
            } else if (segment instanceof DocumentPathSegment.CubicTo cubic) {
                if (!insideUnitBox(cubic.control1X(), cubic.control1Y())
                        || !insideUnitBox(cubic.control2X(), cubic.control2Y())
                        || !insideUnitBox(cubic.x(), cubic.y())) {
                    return false;
                }
            } else if (!(segment instanceof DocumentPathSegment.Close)) {
                return false;
            }
        }
        return true;
    }

    private static boolean insideUnitBox(double x, double y) {
        return x >= 0 && x <= 1 && y >= 0 && y <= 1;
    }

    private static double halfWidth(Stroke stroke) {
        return stroke == null ? 0 : stroke.width() / 2.0;
    }
}
