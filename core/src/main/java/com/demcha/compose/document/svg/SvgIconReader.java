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
        Map<String, Element> ids = collectIds(root);

        List<SvgIcon.Layer> layers = new ArrayList<>();
        SkipTally skipped = new SkipTally();
        walk(root, identity(),
                new Paint(new PaintValue(DocumentColor.rgb(0, 0, 0), null), PaintValue.NONE, 1.0,
                        DocumentLineCap.BUTT, DocumentLineJoin.MITER, List.of()),
                null, box, gradients, ids, skipped, layers);
        if (layers.isEmpty()) {
            throw new IllegalArgumentException(
                    "SVG document contains no drawable geometry" + skipped.reason());
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
            double minX = parseNumber(parts[0], "viewBox min-x");
            double minY = parseNumber(parts[1], "viewBox min-y");
            double width = parseNumber(parts[2], "viewBox width");
            double height = parseNumber(parts[3], "viewBox height");
            requirePositive(width, height, viewBox);
            return new double[]{minX, minY, width, height};
        }
        String w = svg.getAttribute("width").replace("px", "").trim();
        String h = svg.getAttribute("height").replace("px", "").trim();
        if (w.isEmpty() || h.isEmpty()) {
            throw new IllegalArgumentException("SVG carries neither a viewBox nor width/height attributes");
        }
        double width = parseNumber(w, "width");
        double height = parseNumber(h, "height");
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

    private static void walk(Element element, double[] transform, Paint inherited, SvgPath clip,
                             double[] box, Map<String, Element> gradients, Map<String, Element> ids,
                             SkipTally skipped, List<SvgIcon.Layer> out) {
        String name = localName(element);
        // Hidden subtrees (display:none) carry no visible ink — e.g. an Illustrator
        // guide/template layer of registration hatching. Skip the element and its
        // descendants wholesale.
        if (isDisplayNone(element)) {
            return;
        }
        // Process THIS element's own geometry with element context, so any
        // unsupported colour / transform / gradient / unit names the offending
        // element. Recursion stays outside the try — a child's error is already
        // contextualized by its own walk, so it never double-wraps.
        Paint paint;
        double[] matrix;
        SvgPath activeClip = clip;
        try {
            paint = stylize(element, inherited, gradients);
            matrix = compose(transform, element.getAttribute("transform"));
            // A clip-path on this element (or group) clips it and its descendants
            // to the referenced shape, resolved in icon space with the same
            // matrix/box as the geometry it bounds. Nested clips are not
            // intersected — the innermost wins; this is exact for the Noto set
            // (no glyph nests a different clip inside another) and any residual
            // overflow is still bounded by the inline viewBox clip at render.
            SvgPath ownClip = resolveClip(element, matrix, box, ids);
            if (ownClip != null) {
                activeClip = ownClip;
            }
            String d = switch (name) {
                case "path" -> element.getAttribute("d");
                case "rect" -> SvgShapeLowering.rect(num(element, "x"), num(element, "y"),
                        num(element, "width"), num(element, "height"),
                        num(element, "rx"), num(element, "ry"));
                case "circle" -> SvgShapeLowering.ellipse(num(element, "cx"), num(element, "cy"),
                        num(element, "r"), num(element, "r"));
                case "ellipse" -> SvgShapeLowering.ellipse(num(element, "cx"), num(element, "cy"),
                        num(element, "rx"), num(element, "ry"));
                case "line" -> "M" + num(element, "x1") + " " + num(element, "y1")
                               + " L" + num(element, "x2") + " " + num(element, "y2");
                case "polyline" -> SvgShapeLowering.points(element.getAttribute("points"), false);
                case "polygon" -> SvgShapeLowering.points(element.getAttribute("points"), true);
                default -> null;
            };

            if (d != null && !d.isBlank()) {
                emitLayer(element, d, paint, matrix, activeClip, box, gradients, out);
            } else if (DROPS_CONTENT.contains(name)) {
                // A shape kind we deliberately don't render — count it so the icon
                // surfaces one warning per kind instead of silently losing pixels.
                skipped.note(name);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "in " + describe(element) + ": " + e.getMessage(), e);
        }

        // Containers (svg, g, unknown wrappers) recurse; defs and metadata
        // subtrees carry no direct geometry and are skipped wholesale.
        if (name.equals("svg") || name.equals("g")) {
            NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child instanceof Element childElement) {
                    walk(childElement, matrix, paint, activeClip, box, gradients, ids, skipped, out);
                }
            }
        }
    }

    /** Builds and appends the layer for a drawable element (curve geometry + paint). */
    private static void emitLayer(Element element, String d, Paint paint,
                                  double[] matrix, SvgPath clip, double[] box, Map<String, Element> gradients,
                                  List<SvgIcon.Layer> out) {
        boolean strokeVisible = paint.stroke().visible() && paint.strokeWidth() > 0;
        if (!paint.fill().visible() && !strokeVisible) {
            return;
        }
        SvgPath geometry = SvgPath.parseTransformed(d, matrix, box[0], box[1], box[2], box[3]);
        if (!geometry.hasDrawingSegment()) {
            // A moveto-only or moveto+close element (e.g. d="M12 12", or a
            // zero-length arc that lowers to a lone moveto) draws no ink. Drop
            // it so one degenerate element a real-world exporter emitted does
            // not fail the whole icon at SvgIcon#node(...).
            return;
        }

        // Gradients resolve here, where the shape's geometry (the
        // objectBoundingBox reference) and accumulated affine exist. The flat
        // colour keeps the gradient's first stop so backends without shadings
        // degrade per the DocumentPaint contract.
        DocumentColor fillColor = paint.fill().color();
        DocumentPaint fillPaint = null;
        if (paint.fill().gradient() != null) {
            if (!strokeVisible && SvgGradients.isAlphaOnlyOverlay(paint.fill().gradient(), gradients)) {
                // A same-colour translucent gradient is a pure alpha overlay (a
                // soft shadow or edge highlight, e.g. the hair-edge darkening on
                // the vampire glyphs). With no shading-alpha in the backend,
                // painting it opaque would cover the art beneath it — drop the
                // layer rather than blot out a face.
                return;
            }
            fillPaint = SvgGradients.paint(paint.fill().gradient(), gradients, matrix, box, geometry);
            fillColor = fillPaint.primaryColor();
        }
        DocumentStroke stroke = null;
        DocumentPaint strokePaint = null;
        if (strokeVisible) {
            if (paint.stroke().gradient() != null) {
                strokePaint = SvgGradients.paint(paint.stroke().gradient(), gradients, matrix, box, geometry);
                stroke = DocumentStroke.of(strokePaint.primaryColor(), paint.strokeWidth());
            } else {
                stroke = DocumentStroke.of(paint.stroke().color(), paint.strokeWidth());
            }
        }
        out.add(new SvgIcon.Layer(geometry, fillColor, fillPaint, stroke, strokePaint,
                paint.lineCap(), paint.lineJoin(), paint.dashArray(),
                clip != null && clip.hasDrawingSegment() ? clip : null));
    }

    // ------------------------------------------------------------------
    // Clipping (clip-path → a single clip shape in icon space)
    // ------------------------------------------------------------------

    /**
     * Resolves an element's {@code clip-path:url(#id)} to a clip shape in the
     * icon's normalized space, or {@code null} when there is none or it cannot be
     * resolved. Handles the Adobe-Illustrator idiom where the clipPath wraps a
     * {@code <use href="#shape">} pointing at a {@code <defs>} path.
     */
    private static SvgPath resolveClip(Element element, double[] matrix, double[] box,
                                       Map<String, Element> ids) {
        String value = attrOrStyle(element, "clip-path");
        if (value == null || value.isBlank() || value.trim().equals("none")
            || !value.trim().startsWith("url(")) {
            return null;
        }
        try {
            String id = SvgGradients.urlId(value.trim());
            Element clipPath = id == null ? null : ids.get(id);
            if (clipPath == null) {
                return null;
            }
            String d = clipShapeData(clipPath, ids);
            if (d == null || d.isBlank()) {
                return null;
            }
            return SvgPath.parseTransformed(d, matrix, box[0], box[1], box[2], box[3]);
        } catch (RuntimeException e) {
            // A clip we cannot model is better dropped (paint unclipped) than fatal.
            return null;
        }
    }

    /** Path data of a clipPath's first usable child (direct shape, or a <use href="#shape">). */
    private static String clipShapeData(Element clipPath, Map<String, Element> ids) {
        NodeList children = clipPath.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element child)) {
                continue;
            }
            if ("use".equals(localName(child))) {
                String href = child.getAttribute("xlink:href");
                if (href.isEmpty()) {
                    href = child.getAttribute("href");
                }
                if (href.startsWith("#")) {
                    Element target = ids.get(href.substring(1));
                    String d = target == null ? null : shapeData(target);
                    if (d != null && !d.isBlank()) {
                        return d;
                    }
                }
            } else {
                String d = shapeData(child);
                if (d != null && !d.isBlank()) {
                    return d;
                }
            }
        }
        return null;
    }

    /** Lowers a shape element to path data (the clip-path subset of the walk switch). */
    private static String shapeData(Element element) {
        return switch (localName(element)) {
            case "path" -> element.getAttribute("d");
            case "rect" -> SvgShapeLowering.rect(num(element, "x"), num(element, "y"),
                    num(element, "width"), num(element, "height"), num(element, "rx"), num(element, "ry"));
            case "circle" -> SvgShapeLowering.ellipse(num(element, "cx"), num(element, "cy"),
                    num(element, "r"), num(element, "r"));
            case "ellipse" -> SvgShapeLowering.ellipse(num(element, "cx"), num(element, "cy"),
                    num(element, "rx"), num(element, "ry"));
            case "polygon" -> SvgShapeLowering.points(element.getAttribute("points"), true);
            default -> null;
        };
    }

    private static boolean isDisplayNone(Element element) {
        String display = attrOrStyle(element, "display");
        return display != null && display.trim().equalsIgnoreCase("none");
    }

    private static Map<String, Element> collectIds(Element root) {
        Map<String, Element> ids = new java.util.HashMap<>();
        collectIds(root, ids);
        return ids;
    }

    private static void collectIds(Element element, Map<String, Element> ids) {
        String id = element.getAttribute("id");
        if (!id.isEmpty()) {
            ids.putIfAbsent(id, element);
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child) {
                collectIds(child, ids);
            }
        }
    }

    /**
     * A compact, log-safe descriptor of an element for error context:
     * {@code <path fill="magenta-ish" d="M0 0 H1 …">}. Attributes are listed
     * in document order; long values (notably {@code d}) are truncated.
     */
    private static String describe(Element element) {
        StringBuilder sb = new StringBuilder("<").append(element.getNodeName());
        org.w3c.dom.NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength() && sb.length() < 160; i++) {
            Node attr = attrs.item(i);
            String value = attr.getNodeValue();
            if (value != null && value.length() > 40) {
                value = value.substring(0, 40) + "…";
            }
            sb.append(' ').append(attr.getNodeName()).append("=\"").append(value).append('"');
        }
        return sb.append('>').toString();
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

    // ------------------------------------------------------------------
    // Shape lowering (synthesized path data through the tested parser) lives
    // in SvgShapeLowering; the reader only extracts the numbers.
    // ------------------------------------------------------------------

    private static double num(Element element, String attribute) {
        String value = element.getAttribute(attribute).trim();
        return value.isEmpty() ? 0.0 : parseNumber(value, attribute);
    }

    /**
     * Parses a numeric SVG value, naming the field and the offending input on
     * failure instead of leaking the raw {@link NumberFormatException} message
     * ("For input string: …"). The cause is chained so the JDK detail survives
     * for anyone who needs it.
     */
    private static double parseNumber(String value, String what) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    what + " must be a number, got '" + value + "'", e);
        }
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
            v[i] = parseNumber(args[i], "transform '" + source + "' argument");
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

        /**
         * An error-message suffix naming what was skipped, so a blank icon
         * explains itself ("…no drawable geometry — skipped: text, use; this
         * reader renders vector shapes only"). Empty when nothing was skipped.
         */
        String reason() {
            if (kinds.isEmpty()) {
                return "";
            }
            return " — skipped " + String.join(", ", kinds)
                   + "; this reader renders vector shapes only (no text, images, <use>, "
                   + "masks, clips or filters)";
        }
    }
}
