package com.demcha.compose.document.svg;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentPaint;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gradient side of the icon reader: collects {@code <linearGradient>} /
 * {@code <radialGradient>} definitions and resolves a {@code url(#id)}
 * reference into a {@link DocumentPaint} in the icon's normalized space.
 *
 * <p>Coordinate mapping follows the SVG gradient units: {@code
 * objectBoundingBox} (the default) maps endpoints through the referencing
 * shape's normalized bounding box, {@code userSpaceOnUse} pushes them
 * through the element's accumulated affine and the icon frame — the same
 * pipeline the geometry itself rides, so a gradient lands exactly where the
 * browser puts it. {@code gradientTransform} (the affine subset:
 * translate / scale / rotate / matrix) applies to the endpoints first.
 * One level of {@code href} / {@code xlink:href} indirection supplies
 * stops for the split-definition style Inkscape and Figma emit.</p>
 *
 * <p>Out of PDF reach and loudly refused: focal radials ({@code fx} /
 * {@code fy}), {@code spreadMethod} other than pad, and translucent stops
 * ({@code stop-opacity}).</p>
 */
final class SvgGradients {

    private SvgGradients() {
    }

    /**
     * Collects every gradient definition in the document by id, wherever it
     * sits (the {@code <defs>} subtree is not walked for geometry, but
     * gradients may also appear inline).
     */
    static Map<String, Element> collect(Element root) {
        Map<String, Element> byId = new HashMap<>();
        NodeList all = root.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            if (all.item(i) instanceof Element element) {
                String name = localName(element);
                if (("linearGradient".equals(name) || "radialGradient".equals(name))
                    && !element.getAttribute("id").isEmpty()) {
                    byId.put(element.getAttribute("id"), element);
                }
            }
        }
        return byId;
    }

    /**
     * Extracts the gradient id from a {@code url(#id)} paint value, or
     * returns {@code null} if the value is not a url reference.
     */
    static String urlId(String paintValue) {
        String v = paintValue.trim();
        if (!v.startsWith("url(") || !v.endsWith(")")) {
            return null;
        }
        String ref = v.substring(4, v.length() - 1).trim();
        if (ref.startsWith("'") || ref.startsWith("\"")) {
            ref = ref.substring(1, ref.length() - 1).trim();
        }
        if (!ref.startsWith("#")) {
            throw new IllegalArgumentException(
                    "unsupported paint reference '" + paintValue + "' — only url(#id) gradients resolve");
        }
        return ref.substring(1);
    }

    /**
     * Resolves a gradient element into a paint in normalized icon space.
     *
     * @param gradient      the {@code <linearGradient>} / {@code <radialGradient>}
     * @param all           every gradient by id (for one-level href stops)
     * @param elementMatrix accumulated affine of the referencing element
     * @param box           icon frame {@code [minX, minY, width, height]}
     * @param geometry      referencing shape's normalized geometry (bounding
     *                      box source for objectBoundingBox units)
     * @return resolved paint; solid when the axis degenerates per SVG rules
     */
    static DocumentPaint paint(Element gradient, Map<String, Element> all,
                               double[] elementMatrix, double[] box, SvgPath geometry) {
        String spread = gradient.getAttribute("spreadMethod").trim();
        if (!spread.isEmpty() && !spread.equals("pad")) {
            throw new IllegalArgumentException(
                    "unsupported spreadMethod '" + spread + "' — only pad maps to PDF shadings");
        }
        List<DocumentPaint.Stop> stops = stops(gradient, all);
        boolean userSpace = "userSpaceOnUse".equals(gradient.getAttribute("gradientUnits").trim());
        double[] gt = SvgIconReader.compose(SvgIconReader.identity(),
                gradient.getAttribute("gradientTransform"));

        if ("linearGradient".equals(localName(gradient))) {
            double[] p0 = point(gradient, "x1", "y1", 0.0, 0.0, userSpace, box);
            double[] p1 = point(gradient, "x2", "y2", 1.0, 0.0, userSpace, box);
            p0 = apply(gt, p0);
            p1 = apply(gt, p1);
            double[] n0 = normalize(p0, userSpace, elementMatrix, box, geometry);
            double[] n1 = normalize(p1, userSpace, elementMatrix, box, geometry);
            if (n0[0] == n1[0] && n0[1] == n1[1]) {
                // SVG: a degenerate axis paints the last stop as a flat fill.
                return DocumentPaint.solid(stops.get(stops.size() - 1).color());
            }
            return new DocumentPaint.LinearAxis(stops, n0[0], n0[1], n1[0], n1[1]);
        }

        if (!gradient.getAttribute("fx").isEmpty() || !gradient.getAttribute("fy").isEmpty()) {
            throw new IllegalArgumentException(
                    "focal radial gradients (fx/fy) have no PDF analogue and are not supported");
        }
        double[] centre = point(gradient, "cx", "cy", 0.5, 0.5, userSpace, box);
        double r = length(gradient, "r", 0.5, userSpace, box);
        centre = apply(gt, centre);
        r = r * scaleOf(gt);
        double[] n = normalize(centre, userSpace, elementMatrix, box, geometry);
        double radius = normalizeRadius(r, userSpace, elementMatrix, box, geometry);
        return new DocumentPaint.RadialCircle(stops, n[0], n[1], radius);
    }

    // ------------------------------------------------------------------
    // Stops
    // ------------------------------------------------------------------

    private static List<DocumentPaint.Stop> stops(Element gradient, Map<String, Element> all) {
        List<DocumentPaint.Stop> stops = readOwnStops(gradient);
        if (stops.isEmpty()) {
            Element target = href(gradient, all);
            if (target != null) {
                stops = readOwnStops(target);
            }
        }
        if (stops.isEmpty()) {
            throw new IllegalArgumentException("gradient '" + gradient.getAttribute("id")
                                               + "' carries no <stop> elements (one href hop searched)");
        }
        if (stops.size() == 1) {
            // Single stop = flat colour per SVG; duplicate so the paint
            // contract (two stops minimum) holds.
            stops = List.of(stops.get(0),
                    new DocumentPaint.Stop(1.0, stops.get(0).color()));
        }
        return stops;
    }

    private static List<DocumentPaint.Stop> readOwnStops(Element gradient) {
        List<DocumentPaint.Stop> stops = new ArrayList<>();
        NodeList children = gradient.getChildNodes();
        double previous = 0.0;
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (!(child instanceof Element stop) || !"stop".equals(localName(stop))) {
                continue;
            }
            double offset = fraction(attrOrStyle(stop, "offset"), 0.0);
            // SVG clamps offsets into [0,1] and forces them non-decreasing.
            offset = Math.max(previous, Math.min(1.0, Math.max(0.0, offset)));
            previous = offset;
            String opacity = attrOrStyle(stop, "stop-opacity");
            if (opacity != null && fraction(opacity, 1.0) < 1.0) {
                throw new IllegalArgumentException(
                        "gradient stop-opacity is not supported — flatten transparency before import");
            }
            String colorValue = attrOrStyle(stop, "stop-color");
            DocumentColor color = colorValue == null
                    ? DocumentColor.rgb(0, 0, 0)
                    : SvgIconReader.color(colorValue, DocumentColor.rgb(0, 0, 0));
            if (color == null) {
                throw new IllegalArgumentException(
                        "stop-color 'none' is not a paintable gradient stop");
            }
            stops.add(new DocumentPaint.Stop(offset, color));
        }
        return stops;
    }

    private static Element href(Element gradient, Map<String, Element> all) {
        String ref = gradient.getAttribute("href").trim();
        if (ref.isEmpty()) {
            ref = gradient.getAttribute("xlink:href").trim();
        }
        if (ref.startsWith("#")) {
            return all.get(ref.substring(1));
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Coordinates
    // ------------------------------------------------------------------

    private static double[] point(Element gradient, String xAttr, String yAttr,
                                  double defaultX, double defaultY,
                                  boolean userSpace, double[] box) {
        return new double[]{
                coordinate(gradient.getAttribute(xAttr), defaultX, userSpace, box[2]),
                coordinate(gradient.getAttribute(yAttr), defaultY, userSpace, box[3])};
    }

    /**
     * One gradient coordinate: bare numbers are user units (userSpaceOnUse)
     * or bounding-box fractions; percentages are fractions in both unit
     * systems (of the viewport dimension in user space).
     */
    private static double coordinate(String value, double defaultFraction,
                                     boolean userSpace, double viewportSize) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) {
            return userSpace ? defaultFraction * viewportSize : defaultFraction;
        }
        if (v.endsWith("%")) {
            double fraction = Double.parseDouble(v.substring(0, v.length() - 1)) / 100.0;
            return userSpace ? fraction * viewportSize : fraction;
        }
        return Double.parseDouble(v);
    }

    private static double length(Element gradient, String attr, double defaultFraction,
                                 boolean userSpace, double[] box) {
        // Per SVG, percentage lengths resolve against the normalized diagonal.
        double diagonal = Math.sqrt((box[2] * box[2] + box[3] * box[3]) / 2.0);
        String v = gradient.getAttribute(attr).trim();
        if (v.isEmpty()) {
            return userSpace ? defaultFraction * diagonal : defaultFraction;
        }
        if (v.endsWith("%")) {
            double fraction = Double.parseDouble(v.substring(0, v.length() - 1)) / 100.0;
            return userSpace ? fraction * diagonal : fraction;
        }
        return Double.parseDouble(v);
    }

    /**
     * Maps a gradient point into normalized icon space: user-space points
     * ride the element affine and the icon frame (y flipped); bounding-box
     * points interpolate the shape's normalized bbox (SVG bbox y runs down).
     */
    private static double[] normalize(double[] p, boolean userSpace,
                                      double[] elementMatrix, double[] box, SvgPath geometry) {
        if (userSpace) {
            double[] u = apply(elementMatrix, p);
            return new double[]{
                    (u[0] - box[0]) / box[2],
                    (box[1] + box[3] - u[1]) / box[3]};
        }
        double[] bounds = bounds(geometry);
        double top = bounds[1] + bounds[3];
        return new double[]{
                bounds[0] + p[0] * bounds[2],
                top - p[1] * bounds[3]};
    }

    private static double normalizeRadius(double r, boolean userSpace,
                                          double[] elementMatrix, double[] box, SvgPath geometry) {
        if (userSpace) {
            return r * scaleOf(elementMatrix) / box[2];
        }
        // objectBoundingBox: r is a fraction of the bbox "diagonal mean"
        // sqrt((w² + h²) / 2); convert through user units into a fraction of
        // the frame width (the RadialCircle contract).
        double[] bounds = bounds(geometry);
        double bw = bounds[2];
        double bh = bounds[3] * (box[3] / box[2]);
        return r * Math.sqrt((bw * bw + bh * bh) / 2.0);
    }

    /** Normalized bounding box of the geometry: {@code [x, y, w, h]}, y up. */
    private static double[] bounds(SvgPath geometry) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (com.demcha.compose.document.style.DocumentPathSegment segment : geometry.segments()) {
            for (double[] p : points(segment)) {
                minX = Math.min(minX, p[0]);
                minY = Math.min(minY, p[1]);
                maxX = Math.max(maxX, p[0]);
                maxY = Math.max(maxY, p[1]);
            }
        }
        return new double[]{minX, minY, Math.max(1e-9, maxX - minX), Math.max(1e-9, maxY - minY)};
    }

    private static double[][] points(com.demcha.compose.document.style.DocumentPathSegment segment) {
        if (segment instanceof com.demcha.compose.document.style.DocumentPathSegment.MoveTo m) {
            return new double[][]{{m.x(), m.y()}};
        }
        if (segment instanceof com.demcha.compose.document.style.DocumentPathSegment.LineTo l) {
            return new double[][]{{l.x(), l.y()}};
        }
        if (segment instanceof com.demcha.compose.document.style.DocumentPathSegment.CubicTo c) {
            return new double[][]{
                    {c.control1X(), c.control1Y()},
                    {c.control2X(), c.control2Y()},
                    {c.x(), c.y()}};
        }
        return new double[0][];
    }

    /** Uniform scale factor of an affine: sqrt(|det|), exact for similarity maps. */
    private static double scaleOf(double[] m) {
        return Math.sqrt(Math.abs(m[0] * m[3] - m[1] * m[2]));
    }

    private static double[] apply(double[] m, double[] p) {
        return new double[]{
                m[0] * p[0] + m[2] * p[1] + m[4],
                m[1] * p[0] + m[3] * p[1] + m[5]};
    }

    private static double fraction(String value, double defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String v = value.trim();
        if (v.endsWith("%")) {
            return Double.parseDouble(v.substring(0, v.length() - 1)) / 100.0;
        }
        return Double.parseDouble(v);
    }

    private static String attrOrStyle(Element element, String property) {
        String attr = element.getAttribute(property).trim();
        if (!attr.isEmpty()) {
            return attr;
        }
        String style = element.getAttribute("style");
        for (String declaration : style.split(";")) {
            int colon = declaration.indexOf(':');
            if (colon > 0 && declaration.substring(0, colon).trim().equals(property)) {
                return declaration.substring(colon + 1).trim();
            }
        }
        return null;
    }

    private static String localName(Element element) {
        String name = element.getNodeName();
        int colon = name.indexOf(':');
        return colon < 0 ? name : name.substring(colon + 1);
    }
}
