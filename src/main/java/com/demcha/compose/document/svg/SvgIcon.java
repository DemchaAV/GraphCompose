package com.demcha.compose.document.svg;

import com.demcha.compose.document.api.Beta;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.PathNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentDashPattern;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentLineJoin;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A multi-layer vector icon read from the practical subset of an SVG file:
 * the {@code viewBox}, every {@code <path>} (plus {@code rect}, {@code
 * circle}, {@code ellipse}, {@code line}, {@code polyline} and {@code
 * polygon}, lowered to path data), {@code <g>} nesting with accumulated
 * {@code transform} attributes ({@code translate} / {@code scale} /
 * {@code rotate} / {@code matrix} — affine maps are exact on Bézier control
 * points), per-element {@code fill} / {@code stroke} / {@code stroke-width}
 * styling with SVG's inheritance and defaults (missing {@code fill} paints
 * black, {@code fill="none"} skips the fill), and {@code linearGradient} /
 * {@code radialGradient} paints referenced via {@code url(#id)} — on fills
 * and strokes alike, rendered as native PDF shadings.
 *
 * <p>Each layer is one {@link SvgPath} with its resolved paint, in document
 * order — render them back-to-front. {@link #node(double)} packages the
 * layers as one ready-to-place node, and the DSL sugar
 * {@code flow.addSvgIcon(icon, 48)} drops that node into the page at the
 * requested width with the icon's own aspect ratio.</p>
 *
 * <p>Out of scope (deliberately, this is an icon reader, not a browser):
 * CSS stylesheets and classes, text, masks, clip paths, filters,
 * {@code <use>} references, nested {@code <svg>} viewBoxes (inner frames
 * recurse but their coordinates stay in the outer space), animations, and
 * the gradient corners that have no PDF analogue (focal points,
 * {@code spreadMethod} other than pad, stop opacity). The XML reader
 * refuses DOCTYPEs, so external-entity tricks cannot reach the file
 * system.</p>
 *
 * <pre>{@code
 * SvgIcon logo = SvgIcon.read(Path.of("assets/logo.svg"));
 * flow.addSvgIcon(logo, 48);          // flow sugar
 * card.center(logo.node(48));         // node form for layer anchors
 * }</pre>
 *
 * <p><b>Beta:</b> the SVG surface is new in 1.8.0 and marked {@link Beta}
 * while it hardens against real-world exporter output.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
@Beta
public final class SvgIcon {

    private final List<Layer> layers;
    private final double sourceWidth;
    private final double sourceHeight;

    SvgIcon(List<Layer> layers, double sourceWidth, double sourceHeight) {
        this.layers = List.copyOf(layers);
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
    }

    /**
     * Reads and parses an SVG file.
     *
     * @param file path to the SVG file
     * @return parsed icon
     * @throws IOException              if the file cannot be read
     * @throws IllegalArgumentException if the document is not parseable SVG,
     *                                  has no viewBox or usable size, or
     *                                  contains no drawable geometry
     */
    public static SvgIcon read(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        return parse(Files.readString(file, StandardCharsets.UTF_8));
    }

    /**
     * Parses SVG markup.
     *
     * @param svgXml the SVG document text
     * @return parsed icon
     * @throws IllegalArgumentException if the document is not parseable SVG,
     *                                  has no viewBox or usable size, or
     *                                  contains no drawable geometry
     */
    public static SvgIcon parse(String svgXml) {
        return SvgIconReader.read(svgXml);
    }

    /**
     * Returns the icon's layers in document order (paint back-to-front).
     *
     * @return immutable layer list; never empty
     */
    public List<Layer> layers() {
        return layers;
    }

    /**
     * Returns the icon frame width in SVG user units.
     *
     * @return viewBox (or width attribute) width
     */
    public double sourceWidth() {
        return sourceWidth;
    }

    /**
     * Returns the icon frame height in SVG user units.
     *
     * @return viewBox (or height attribute) height
     */
    public double sourceHeight() {
        return sourceHeight;
    }

    /**
     * Returns the frame's width-to-height ratio for proportional sizing.
     *
     * @return {@code sourceWidth() / sourceHeight()}
     */
    public double aspectRatio() {
        return sourceWidth / sourceHeight;
    }

    /**
     * Packages the icon as one ready-to-place node: a layer stack of path
     * nodes at the given width, height following the icon's aspect ratio.
     * The stack's box is exactly the icon box, so it anchors true inside
     * {@code ShapeContainer} / {@code LayerStack} nine-point grids — the
     * node-form sibling of the {@code addSvgIcon(icon, width)} flow sugar.
     *
     * @param width target width in points; must be positive
     * @return layer stack rendering this icon at {@code width} points
     * @throws IllegalArgumentException if {@code width} is not positive
     * @since 1.8.0
     */
    public LayerStackNode node(double width) {
        if (!(width > 0) || Double.isInfinite(width)) {
            throw new IllegalArgumentException("icon width must be finite and positive: " + width);
        }
        double height = width / aspectRatio();
        // Stroke widths and dash lengths live in SVG user units on the
        // layers; they scale with the icon like every other dimension.
        double scale = width / sourceWidth;
        List<LayerStackNode.Layer> stack = new ArrayList<>(layers.size());
        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            DocumentStroke stroke = layer.stroke() == null ? null
                    : DocumentStroke.of(layer.stroke().color(), layer.stroke().width() * scale);
            DocumentDashPattern dash = null;
            if (!layer.dashArray().isEmpty()) {
                double[] scaled = new double[layer.dashArray().size()];
                for (int s = 0; s < scaled.length; s++) {
                    scaled[s] = layer.dashArray().get(s) * scale;
                }
                dash = DocumentDashPattern.of(scaled);
            }
            stack.add(new LayerStackNode.Layer(new PathNode(
                    "SvgLayer" + i,
                    width,
                    height,
                    layer.geometry().segments(),
                    layer.fill(),
                    layer.fillPaint(),
                    stroke,
                    layer.strokePaint(),
                    null,
                    null,
                    dash,
                    layer.lineCap(),
                    layer.lineJoin())));
        }
        return new LayerStackNode("SvgIcon", stack, null, null);
    }

    /**
     * One drawable layer: normalized geometry plus its resolved paint.
     * Gradient paints, when present, win over the flat colours; the flat
     * colours stay populated as the degradation target for backends that
     * cannot render gradients. Stroke width (inside {@code stroke}) and the
     * dash lengths are in <em>SVG user units</em> — {@link #node(double)}
     * scales them to points together with the geometry.
     *
     * @param geometry    normalized path geometry (shared icon frame)
     * @param fill        fill colour, or {@code null} for no fill
     * @param fillPaint   gradient fill, or {@code null} for flat / no fill
     * @param stroke      outline stroke (width in user units), or {@code null}
     * @param strokePaint gradient stroke paint, or {@code null} for flat
     * @param lineCap     stroke end-cap style; never {@code null} (BUTT default)
     * @param lineJoin    stroke corner style; never {@code null} (MITER default)
     * @param dashArray   stroke dash lengths in user units; empty for solid
     * @since 1.8.0
     */
    public record Layer(SvgPath geometry,
                        DocumentColor fill,
                        DocumentPaint fillPaint,
                        DocumentStroke stroke,
                        DocumentPaint strokePaint,
                        DocumentLineCap lineCap,
                        DocumentLineJoin lineJoin,
                        List<Double> dashArray) {
        /**
         * Validates the geometry reference and normalizes style defaults.
         */
        public Layer {
            Objects.requireNonNull(geometry, "geometry");
            lineCap = lineCap == null ? DocumentLineCap.BUTT : lineCap;
            lineJoin = lineJoin == null ? DocumentLineJoin.MITER : lineJoin;
            dashArray = dashArray == null ? List.of() : List.copyOf(dashArray);
        }

        /**
         * Compatibility constructor for flat-colour layers.
         *
         * @param geometry normalized path geometry
         * @param fill     fill colour, or {@code null}
         * @param stroke   outline stroke, or {@code null}
         */
        public Layer(SvgPath geometry, DocumentColor fill, DocumentStroke stroke) {
            this(geometry, fill, null, stroke, null, null, null, null);
        }

        /**
         * Compatibility constructor with paints but default stroke styling.
         *
         * @param geometry    normalized path geometry
         * @param fill        fill colour, or {@code null}
         * @param fillPaint   gradient fill, or {@code null}
         * @param stroke      outline stroke, or {@code null}
         * @param strokePaint gradient stroke paint, or {@code null}
         */
        public Layer(SvgPath geometry, DocumentColor fill, DocumentPaint fillPaint,
                     DocumentStroke stroke, DocumentPaint strokePaint) {
            this(geometry, fill, fillPaint, stroke, strokePaint, null, null, null);
        }
    }
}
