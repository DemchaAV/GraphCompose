package com.demcha.compose.document.svg;

import com.demcha.compose.document.style.DocumentPathSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed SVG path data (<code>&lt;path d="…"&gt;</code>), lowered to the
 * canonical {@link DocumentPathSegment} set and normalized into the unit
 * box with the y-axis flipped to the PDF orientation.
 *
 * <p>The full SVG 1.1 path grammar is supported: absolute and relative
 * {@code M L H V C S Q T A Z}, implicit command repetition (including the
 * lineto chain after a moveto), quadratic curves (converted exactly to
 * cubics), smooth shorthands ({@code S}/{@code T} control-point
 * reflection), and elliptical arcs (converted deterministically to cubic
 * spans of at most 90° each via the W3C endpoint-to-center algorithm).
 * Everything a vector editor exports as a {@code d} string lands here as
 * lines, cubics and closes — ready for native PDF curve rendering.</p>
 *
 * <p>Normalization uses the supplied viewBox when one is given (the usual
 * icon workflow, keeping the icon's designed padding) or the tight bounding
 * box of the parsed geometry otherwise. SVG's y-down user space is flipped
 * to the y-up convention of {@code DocumentPathSegment}; fills keep SVG's
 * default non-zero winding semantics.</p>
 *
 * <pre>{@code
 * SvgPath heart = SvgPath.parse(MATERIAL_HEART_D, 0, 0, 24, 24);
 * flow.addPath(path -> path
 *         .size(64, 64 / heart.aspectRatio())
 *         .svg(heart)
 *         .fillColor(crimson));
 * }</pre>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public final class SvgPath {

    private static final double EPS = 1e-9;

    private final List<DocumentPathSegment> segments;
    private final double sourceWidth;
    private final double sourceHeight;

    private SvgPath(List<DocumentPathSegment> segments, double sourceWidth, double sourceHeight) {
        this.segments = List.copyOf(segments);
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
    }

    /**
     * Parses SVG path data and normalizes it against the tight bounding box
     * of the parsed geometry (anchor and control points).
     *
     * @param d SVG path data, e.g. {@code "M0 0 C20 40 40 40 60 0 Z"}
     * @return parsed, normalized path
     * @throws IllegalArgumentException if the data is empty, does not start
     *                                  with a moveto, or contains a syntax
     *                                  error (the message carries the
     *                                  character position)
     */
    public static SvgPath parse(String d) {
        List<double[]> ops = new SvgPathParser(d).parse();
        double[] box = tightBox(ops);
        return normalize(ops, box[0], box[1], box[2], box[3]);
    }

    /**
     * Parses SVG path data and normalizes it against the given viewBox —
     * the usual workflow for icons, where the viewBox preserves the icon's
     * designed padding inside its frame.
     *
     * @param d      SVG path data
     * @param minX   viewBox min-x
     * @param minY   viewBox min-y
     * @param width  viewBox width; must be finite and positive
     * @param height viewBox height; must be finite and positive
     * @return parsed, normalized path
     * @throws IllegalArgumentException on syntax errors or a non-positive box
     */
    public static SvgPath parse(String d, double minX, double minY, double width, double height) {
        if (!(width > 0) || Double.isInfinite(width) || !(height > 0) || Double.isInfinite(height)) {
            throw new IllegalArgumentException(
                    "viewBox must have finite positive dimensions: " + width + " x " + height);
        }
        return normalize(new SvgPathParser(d).parse(), minX, minY, width, height);
    }

    /**
     * Returns the normalized segments (unit box, y-up), ready for
     * {@code PathBuilder.svg(...)} or a {@code PathNode}.
     *
     * @return immutable normalized segment list
     */
    public List<DocumentPathSegment> segments() {
        return segments;
    }

    /**
     * Returns the source box width in SVG user units (viewBox width or tight
     * bounding-box width).
     *
     * @return source width
     */
    public double sourceWidth() {
        return sourceWidth;
    }

    /**
     * Returns the source box height in SVG user units.
     *
     * @return source height
     */
    public double sourceHeight() {
        return sourceHeight;
    }

    /**
     * Returns the width-to-height ratio of the source box, for sizing the
     * target {@code PathNode} proportionally.
     *
     * @return {@code sourceWidth() / sourceHeight()}
     */
    public double aspectRatio() {
        return sourceWidth / sourceHeight;
    }

    // ------------------------------------------------------------------
    // Normalization (y-flip into the unit box)
    // ------------------------------------------------------------------

    /** Op encoding: [0]=kind (0 move, 1 line, 2 cubic, 3 close), then coords. */
    private static SvgPath normalize(List<double[]> ops,
                                     double minX, double minY, double width, double height) {
        // Degenerate extents (a purely horizontal or vertical path with a
        // tight box) keep coordinates finite by centring on the flat axis.
        boolean flatX = width < EPS;
        boolean flatY = height < EPS;
        double w = flatX ? 1.0 : width;
        double h = flatY ? 1.0 : height;
        double topY = minY + height;

        List<DocumentPathSegment> out = new ArrayList<>(ops.size());
        for (double[] op : ops) {
            switch ((int) op[0]) {
                case 0 -> out.add(DocumentPathSegment.moveTo(nx(op[1], minX, w, flatX), ny(op[2], topY, h, flatY)));
                case 1 -> out.add(DocumentPathSegment.lineTo(nx(op[1], minX, w, flatX), ny(op[2], topY, h, flatY)));
                case 2 -> out.add(DocumentPathSegment.cubicTo(
                        nx(op[1], minX, w, flatX), ny(op[2], topY, h, flatY),
                        nx(op[3], minX, w, flatX), ny(op[4], topY, h, flatY),
                        nx(op[5], minX, w, flatX), ny(op[6], topY, h, flatY)));
                default -> out.add(DocumentPathSegment.close());
            }
        }
        return new SvgPath(out, width < EPS ? 1.0 : width, height < EPS ? 1.0 : height);
    }

    private static double nx(double x, double minX, double w, boolean flat) {
        return flat ? 0.5 : (x - minX) / w;
    }

    private static double ny(double y, double topY, double h, boolean flat) {
        return flat ? 0.5 : (topY - y) / h;
    }

    private static double[] tightBox(List<double[]> ops) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (double[] op : ops) {
            for (int i = 1; i + 1 < op.length; i += 2) {
                minX = Math.min(minX, op[i]);
                maxX = Math.max(maxX, op[i]);
                minY = Math.min(minY, op[i + 1]);
                maxY = Math.max(maxY, op[i + 1]);
            }
        }
        if (minX > maxX) {
            throw new IllegalArgumentException("SVG path data contains no drawable geometry");
        }
        return new double[]{minX, minY, maxX - minX, maxY - minY};
    }
}
