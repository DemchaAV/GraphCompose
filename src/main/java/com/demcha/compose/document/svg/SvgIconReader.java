package com.demcha.compose.document.svg;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentLineJoin;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Internal DOM walker behind {@link SvgIcon#parse(String)}: secure XML setup
 * (DOCTYPE refused, so XXE cannot reach the file system), viewBox
 * resolution, recursive {@code <g>} traversal with affine accumulation and
 * paint inheritance, shape-to-path lowering (every basic shape becomes
 * synthesized path data fed through the one tested parser), the icon
 * colour subset ({@code #rgb}, {@code #rrggbb}, {@code rgb(r,g,b)},
 * {@code none}, {@code currentColor} → default ink), and {@code url(#id)}
 * gradient references resolved through {@link SvgGradients}.
 */
final class SvgIconReader {

    private static final Logger LOG = LoggerFactory.getLogger(SvgIconReader.class);

    /**
     * Shape elements that carry visible content this reader does not render —
     * worth one warning per kind rather than a silent drop. Containers
     * ({@code defs}, {@code g}, {@code symbol}, {@code metadata}…) are not
     * here: they hold no direct geometry, so skipping them loses nothing.
     */
    private static final Set<String> DROPS_CONTENT = Set.of(
            "text", "tspan", "textPath", "image", "use", "foreignObject");

    private SvgIconReader() {
    }

    static SvgIcon read(String svgXml) {
        Element root = parseXml(svgXml);
        if (!"svg".equals(localName(root))) {
            throw new IllegalArgumentException("not an SVG document: root element is <" + root.getNodeName() + ">");
        }
        double[] box = viewBox(root);
        Map<String, Element> gradients = SvgGradients.collect(root);

        List<SvgIcon.Layer> layers = new ArrayList<>();
        SkipTally skipped = new SkipTally();
        walk(root, identity(),
                new Paint(new PaintValue(DocumentColor.rgb(0, 0, 0), null), PaintValue.NONE, 1.0,
                        DocumentLineCap.BUTT, DocumentLineJoin.MITER, List.of()),
                box, gradients, skipped, layers);
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("SVG document contains no drawable geometry");
        }
        skipped.flush();
        return new SvgIcon(layers, box[2], box[3]);
    }

    // ------------------------------------------------------------------
    // XML
    // ------------------------------------------------------------------

    private static Element parseXml(String svgXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(svgXml == null ? "" : svgXml)));
            return document.getDocumentElement();
        } catch (Exception e) {
            throw new IllegalArgumentException("not parseable SVG: " + e.getMessage(), e);
        }
    }

    private static String localName(Element element) {
        String name = element.getNodeName();
        int colon = name.indexOf(':');
        return colon < 0 ? name : name.substring(colon + 1);
    }

    private static double[] viewBox(Element svg) {
        String viewBox = svg.getAttribute("viewBox").trim();
        if (!viewBox.isEmpty()) {
            String[] parts = viewBox.split("[\\s,]+");
            if (parts.length != 4) {
                throw new IllegalArgumentException("viewBox must carry four numbers: '" + viewBox + "'");
            }
            double minX = Double.parseDouble(parts[0]);
            double minY = Double.parseDouble(parts[1]);
            double width = Double.parseDouble(parts[2]);
            double height = Double.parseDouble(parts[3]);
            requirePositive(width, height, viewBox);
            return new double[]{minX, minY, width, height};
        }
        String w = svg.getAttribute("width").replace("px", "").trim();
        String h = svg.getAttribute("height").replace("px", "").trim();
        if (w.isEmpty() || h.isEmpty()) {
            throw new IllegalArgumentException("SVG carries neither a viewBox nor width/height attributes");
        }
        double width = Double.parseDouble(w);
        double height = Double.parseDouble(h);
        requirePositive(width, height, w + " x " + h);
        return new double[]{0, 0, width, height};
    }

    private static void requirePositive(double width, double height, String source) {
        if (!(width > 0) || !(height > 0)) {
            throw new IllegalArgumentException("SVG frame must be positive: " + source);
        }
    }

    // ------------------------------------------------------------------
    // Tree walk
    // ------------------------------------------------------------------

    private static void walk(Element element, double[] transform, Paint inherited,
                             double[] box, Map<String, Element> gradients,
                             SkipTally skipped, List<SvgIcon.Layer> out) {
        Paint paint = stylize(element, inherited, gradients);
        double[] matrix = compose(transform, element.getAttribute("transform"));

        String name = localName(element);
        String d = switch (name) {
            case "path" -> element.getAttribute("d");
            case "rect" -> rectToPath(element);
            case "circle" -> ellipseToPath(num(element, "cx"), num(element, "cy"),
                    num(element, "r"), num(element, "r"));
            case "ellipse" -> ellipseToPath(num(element, "cx"), num(element, "cy"),
                    num(element, "rx"), num(element, "ry"));
            case "line" -> "M" + num(element, "x1") + " " + num(element, "y1")
                           + " L" + num(element, "x2") + " " + num(element, "y2");
            case "polyline" -> pointsToPath(element.getAttribute("points"), false);
            case "polygon" -> pointsToPath(element.getAttribute("points"), true);
            default -> null;
        };

        if (d != null && !d.isBlank()) {
            boolean strokeVisible = paint.stroke().visible() && paint.strokeWidth() > 0;
            if (paint.fill().visible() || strokeVisible) {
                SvgPath geometry = SvgPath.parseTransformed(d, matrix, box[0], box[1], box[2], box[3]);

                // Gradients resolve here, where the shape's geometry (the
                // objectBoundingBox reference) and accumulated affine exist.
                // The flat colour keeps the gradient's first stop so backends
                // without shadings degrade per the DocumentPaint contract.
                DocumentColor fillColor = paint.fill().color();
                DocumentPaint fillPaint = null;
                if (paint.fill().gradient() != null) {
                    fillPaint = SvgGradients.paint(paint.fill().gradient(), gradients,
                            matrix, box, geometry);
                    fillColor = fillPaint.primaryColor();
                }
                DocumentStroke stroke = null;
                DocumentPaint strokePaint = null;
                if (strokeVisible) {
                    if (paint.stroke().gradient() != null) {
                        strokePaint = SvgGradients.paint(paint.stroke().gradient(), gradients,
                                matrix, box, geometry);
                        stroke = DocumentStroke.of(strokePaint.primaryColor(), paint.strokeWidth());
                    } else {
                        stroke = DocumentStroke.of(paint.stroke().color(), paint.strokeWidth());
                    }
                }
                out.add(new SvgIcon.Layer(geometry, fillColor, fillPaint, stroke, strokePaint,
                        paint.lineCap(), paint.lineJoin(), paint.dashArray()));
            }
        } else if (DROPS_CONTENT.contains(name)) {
            // A shape kind we deliberately don't render — count it so the icon
            // surfaces one warning per kind instead of silently losing pixels.
            skipped.note(name);
        }

        // Containers (svg, g, unknown wrappers) recurse; defs and metadata
        // subtrees carry no direct geometry and are skipped wholesale.
        if (name.equals("svg") || name.equals("g")) {
            NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child instanceof Element childElement) {
                    walk(childElement, matrix, paint, box, gradients, skipped, out);
                }
            }
        }
    }

    private static Paint stylize(Element element, Paint inherited, Map<String, Element> gradients) {
        PaintValue fill = inherited.fill();
        PaintValue stroke = inherited.stroke();
        double strokeWidth = inherited.strokeWidth();
        DocumentLineCap lineCap = inherited.lineCap();
        DocumentLineJoin lineJoin = inherited.lineJoin();
        List<Double> dashArray = inherited.dashArray();

        String fillAttr = attrOrStyle(element, "fill");
        if (fillAttr != null) {
            fill = paintValue(fillAttr, inherited.fill(), gradients);
        }
        String strokeAttr = attrOrStyle(element, "stroke");
        if (strokeAttr != null) {
            stroke = paintValue(strokeAttr, inherited.stroke(), gradients);
        }
        String widthAttr = attrOrStyle(element, "stroke-width");
        if (widthAttr != null) {
            strokeWidth = SvgStyles.length(widthAttr, "stroke-width");
        }
        String capAttr = attrOrStyle(element, "stroke-linecap");
        if (capAttr != null) {
            DocumentLineCap parsed = SvgStyles.lineCap(capAttr);
            lineCap = parsed == null ? inherited.lineCap() : parsed;
        }
        String joinAttr = attrOrStyle(element, "stroke-linejoin");
        if (joinAttr != null) {
            DocumentLineJoin parsed = SvgStyles.lineJoin(joinAttr);
            lineJoin = parsed == null ? inherited.lineJoin() : parsed;
        }
        String dashAttr = attrOrStyle(element, "stroke-dasharray");
        if (dashAttr != null) {
            dashArray = dashAttr.equalsIgnoreCase("inherit")
                    ? inherited.dashArray()
                    : SvgStyles.dashArray(dashAttr);
        }
        return new Paint(fill, stroke, strokeWidth, lineCap, lineJoin, dashArray);
    }

    /**
     * Resolves one paint attribute: url(#id) gradient, flat colour, or none.
     */
    private static PaintValue paintValue(String value, PaintValue current,
                                         Map<String, Element> gradients) {
        String id = SvgGradients.urlId(value);
        if (id != null) {
            Element gradient = gradients.get(id);
            if (gradient == null) {
                throw new IllegalArgumentException("paint '" + value.trim()
                                                   + "' references no <linearGradient>/<radialGradient> with id '"
                                                   + id + "'");
            }
            return new PaintValue(null, gradient);
        }
        DocumentColor color = color(value, current.color());
        return color == null ? PaintValue.NONE : new PaintValue(color, null);
    }

    // ------------------------------------------------------------------
    // Styling
    // ------------------------------------------------------------------

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

    /**
     * Resolves an SVG paint colour through the shared {@link SvgStyles}
     * grammar (hex incl. alpha, {@code rgb()}/{@code rgba()}, CSS names,
     * {@code none}, {@code currentColor}). Stays here as the package entry
     * point {@link SvgGradients} also calls.
     */
    static DocumentColor color(String value, DocumentColor current) {
        return SvgStyles.color(value, current);
    }

    private static String rectToPath(Element rect) {
        double x = num(rect, "x");
        double y = num(rect, "y");
        double w = num(rect, "width");
        double h = num(rect, "height");
        double rx = num(rect, "rx");
        double ry = num(rect, "ry");
        if (rx <= 0 && ry <= 0) {
            return "M" + x + " " + y + " h" + w + " v" + h + " h" + (-w) + " Z";
        }
        if (rx <= 0) {
            rx = ry;
        }
        if (ry <= 0) {
            ry = rx;
        }
        rx = Math.min(rx, w / 2);
        ry = Math.min(ry, h / 2);
        return "M" + (x + rx) + " " + y
               + " h" + (w - 2 * rx)
               + " a" + rx + " " + ry + " 0 0 1 " + rx + " " + ry
               + " v" + (h - 2 * ry)
               + " a" + rx + " " + ry + " 0 0 1 " + (-rx) + " " + ry
               + " h" + (2 * rx - w)
               + " a" + rx + " " + ry + " 0 0 1 " + (-rx) + " " + (-ry)
               + " v" + (2 * ry - h)
               + " a" + rx + " " + ry + " 0 0 1 " + rx + " " + (-ry)
               + " Z";
    }

    private static String ellipseToPath(double cx, double cy, double rx, double ry) {
        if (rx <= 0 || ry <= 0) {
            return null;
        }
        return "M" + (cx - rx) + " " + cy
               + " a" + rx + " " + ry + " 0 1 0 " + (2 * rx) + " 0"
               + " a" + rx + " " + ry + " 0 1 0 " + (-2 * rx) + " 0"
               + " Z";
    }

    // ------------------------------------------------------------------
    // Shape lowering (synthesized path data through the tested parser)
    // ------------------------------------------------------------------

    private static String pointsToPath(String points, boolean close) {
        String trimmed = points == null ? "" : points.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return "M" + trimmed + (close ? " Z" : "");
    }

    private static double num(Element element, String attribute) {
        String value = element.getAttribute(attribute).trim();
        return value.isEmpty() ? 0.0 : Double.parseDouble(value);
    }

    static double[] identity() {
        return new double[]{1, 0, 0, 1, 0, 0};
    }

    /**
     * Composes {@code transform="…"} ops onto the parent matrix, left to right.
     */
    static double[] compose(double[] parent, String transformAttribute) {
        String attr = transformAttribute == null ? "" : transformAttribute.trim();
        if (attr.isEmpty()) {
            return parent;
        }
        double[] m = parent;
        int index = 0;
        while (index < attr.length()) {
            int open = attr.indexOf('(', index);
            if (open < 0) {
                break;
            }
            int closeParen = attr.indexOf(')', open);
            if (closeParen < 0) {
                throw new IllegalArgumentException("unterminated transform: '" + attr + "'");
            }
            String op = attr.substring(index, open).replace(",", " ").trim();
            String[] args = attr.substring(open + 1, closeParen).trim().split("[\\s,]+");
            m = multiply(m, transformOp(op, args, attr));
            index = closeParen + 1;
            while (index < attr.length()
                   && (attr.charAt(index) == ' ' || attr.charAt(index) == ',')) {
                index++;
            }
        }
        return m;
    }

    // ------------------------------------------------------------------
    // Transforms
    // ------------------------------------------------------------------

    private static double[] transformOp(String op, String[] args, String source) {
        double[] v = new double[args.length];
        for (int i = 0; i < args.length; i++) {
            v[i] = Double.parseDouble(args[i]);
        }
        return switch (op) {
            case "translate" -> new double[]{1, 0, 0, 1, v[0], v.length > 1 ? v[1] : 0};
            case "scale" -> new double[]{v[0], 0, 0, v.length > 1 ? v[1] : v[0], 0, 0};
            case "matrix" -> new double[]{v[0], v[1], v[2], v[3], v[4], v[5]};
            case "rotate" -> {
                double radians = Math.toRadians(v[0]);
                double cos = Math.cos(radians);
                double sin = Math.sin(radians);
                double[] rotation = {cos, sin, -sin, cos, 0, 0};
                if (v.length == 3) {
                    double[] toOrigin = {1, 0, 0, 1, -v[1], -v[2]};
                    double[] back = {1, 0, 0, 1, v[1], v[2]};
                    yield multiply(multiply(back, rotation), toOrigin);
                }
                yield rotation;
            }
            default -> throw new IllegalArgumentException(
                    "unsupported transform '" + op + "' in '" + source + "'");
        };
    }

    /**
     * SVG matrix composition: result = a × b (b applies first).
     */
    private static double[] multiply(double[] a, double[] b) {
        return new double[]{
                a[0] * b[0] + a[2] * b[1],
                a[1] * b[0] + a[3] * b[1],
                a[0] * b[2] + a[2] * b[3],
                a[1] * b[2] + a[3] * b[3],
                a[0] * b[4] + a[2] * b[5] + a[4],
                a[1] * b[4] + a[3] * b[5] + a[5]};
    }

    /**
     * One inheritable paint slot: a flat colour, a gradient element awaiting
     * geometry context, or nothing.
     */
    private record PaintValue(DocumentColor color, Element gradient) {
        static final PaintValue NONE = new PaintValue(null, null);

        boolean visible() {
            return color != null || gradient != null;
        }
    }

    /**
     * Inherited paint state: SVG fills default to black, strokes to none.
     * Stroke style (cap / join / dash) is inheritable too, so it rides here.
     */
    private record Paint(PaintValue fill, PaintValue stroke, double strokeWidth,
                         DocumentLineCap lineCap, DocumentLineJoin lineJoin,
                         List<Double> dashArray) {
    }

    /**
     * One-warning-per-kind tally for shape elements we deliberately drop
     * (text, images, embedded references). Emitted once after the walk so a
     * busy icon doesn't flood the log.
     */
    private static final class SkipTally {
        private final Set<String> kinds = new java.util.LinkedHashSet<>();

        void note(String kind) {
            kinds.add(kind);
        }

        void flush() {
            if (!kinds.isEmpty()) {
                LOG.warn("SvgIcon: skipped unsupported element(s) {} — this icon reader renders "
                         + "vector geometry only (no text, images, <use>, masks, clips or filters)",
                        kinds);
            }
        }
    }
}
