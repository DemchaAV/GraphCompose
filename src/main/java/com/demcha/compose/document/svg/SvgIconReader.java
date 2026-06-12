package com.demcha.compose.document.svg;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentStroke;
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
import java.util.Locale;

/**
 * Internal DOM walker behind {@link SvgIcon#parse(String)}: secure XML setup
 * (DOCTYPE refused, so XXE cannot reach the file system), viewBox
 * resolution, recursive {@code <g>} traversal with affine accumulation and
 * paint inheritance, shape-to-path lowering (every basic shape becomes
 * synthesized path data fed through the one tested parser), and the icon
 * colour subset ({@code #rgb}, {@code #rrggbb}, {@code rgb(r,g,b)},
 * {@code none}, {@code currentColor} → default ink).
 */
final class SvgIconReader {

    private SvgIconReader() {
    }

    static SvgIcon read(String svgXml) {
        Element root = parseXml(svgXml);
        if (!"svg".equals(localName(root))) {
            throw new IllegalArgumentException("not an SVG document: root element is <" + root.getNodeName() + ">");
        }
        double[] box = viewBox(root);

        List<SvgIcon.Layer> layers = new ArrayList<>();
        walk(root, identity(), new Paint(DocumentColor.rgb(0, 0, 0), null, 1.0), box, layers);
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("SVG document contains no drawable geometry");
        }
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

    /** Inherited paint state: SVG fills default to black, strokes to none. */
    private record Paint(DocumentColor fill, DocumentColor strokeColor, double strokeWidth) {
    }

    private static void walk(Element element, double[] transform, Paint inherited,
                             double[] box, List<SvgIcon.Layer> out) {
        Paint paint = stylize(element, inherited);
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
            DocumentStroke stroke = paint.strokeColor() == null || paint.strokeWidth() <= 0
                    ? null
                    : DocumentStroke.of(paint.strokeColor(), paint.strokeWidth());
            if (paint.fill() != null || stroke != null) {
                SvgPath geometry = SvgPath.parseTransformed(d, matrix, box[0], box[1], box[2], box[3]);
                out.add(new SvgIcon.Layer(geometry, paint.fill(), stroke));
            }
        }

        // Containers (svg, g, unknown wrappers) recurse; defs and metadata
        // subtrees carry no direct geometry and are skipped wholesale.
        if (name.equals("svg") || name.equals("g")) {
            NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child instanceof Element childElement) {
                    walk(childElement, matrix, paint, box, out);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Styling
    // ------------------------------------------------------------------

    private static Paint stylize(Element element, Paint inherited) {
        DocumentColor fill = inherited.fill();
        DocumentColor strokeColor = inherited.strokeColor();
        double strokeWidth = inherited.strokeWidth();

        String fillAttr = attrOrStyle(element, "fill");
        if (fillAttr != null) {
            fill = color(fillAttr, inherited.fill());
        }
        String strokeAttr = attrOrStyle(element, "stroke");
        if (strokeAttr != null) {
            strokeColor = color(strokeAttr, inherited.strokeColor());
        }
        String widthAttr = attrOrStyle(element, "stroke-width");
        if (widthAttr != null) {
            strokeWidth = Double.parseDouble(widthAttr.replace("px", "").trim());
        }
        return new Paint(fill, strokeColor, strokeWidth);
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

    private static DocumentColor color(String value, DocumentColor current) {
        String v = value.trim().toLowerCase(Locale.ROOT);
        if (v.equals("none")) {
            return null;
        }
        if (v.equals("currentcolor") || v.equals("inherit")) {
            return current;
        }
        if (v.startsWith("#")) {
            String hex = v.substring(1);
            if (hex.length() == 3) {
                hex = "" + hex.charAt(0) + hex.charAt(0)
                      + hex.charAt(1) + hex.charAt(1)
                      + hex.charAt(2) + hex.charAt(2);
            }
            if (hex.length() == 6) {
                return DocumentColor.rgb(
                        Integer.parseInt(hex.substring(0, 2), 16),
                        Integer.parseInt(hex.substring(2, 4), 16),
                        Integer.parseInt(hex.substring(4, 6), 16));
            }
        }
        if (v.startsWith("rgb(") && v.endsWith(")")) {
            String[] parts = v.substring(4, v.length() - 1).split(",");
            if (parts.length == 3) {
                return DocumentColor.rgb(
                        Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim()));
            }
        }
        if (v.equals("black")) {
            return DocumentColor.rgb(0, 0, 0);
        }
        if (v.equals("white")) {
            return DocumentColor.rgb(255, 255, 255);
        }
        throw new IllegalArgumentException(
                "unsupported SVG colour '" + value + "' — use #hex, rgb(r,g,b), none, or currentColor");
    }

    // ------------------------------------------------------------------
    // Shape lowering (synthesized path data through the tested parser)
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Transforms
    // ------------------------------------------------------------------

    private static double[] identity() {
        return new double[]{1, 0, 0, 1, 0, 0};
    }

    /** Composes {@code transform="…"} ops onto the parent matrix, left to right. */
    private static double[] compose(double[] parent, String transformAttribute) {
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

    /** SVG matrix composition: result = a × b (b applies first). */
    private static double[] multiply(double[] a, double[] b) {
        return new double[]{
                a[0] * b[0] + a[2] * b[1],
                a[1] * b[0] + a[3] * b[1],
                a[0] * b[2] + a[2] * b[3],
                a[1] * b[2] + a[3] * b[3],
                a[0] * b[4] + a[2] * b[5] + a[4],
                a[1] * b[4] + a[3] * b[5] + a[5]};
    }
}
