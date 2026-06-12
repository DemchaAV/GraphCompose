package com.demcha.compose.document.svg;

import com.demcha.compose.document.api.Beta;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentStroke;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * A multi-layer vector icon read from the practical subset of an SVG file:
 * the {@code viewBox}, every {@code <path>} (plus {@code rect}, {@code
 * circle}, {@code ellipse}, {@code line}, {@code polyline} and {@code
 * polygon}, lowered to path data), {@code <g>} nesting with accumulated
 * {@code transform} attributes ({@code translate} / {@code scale} /
 * {@code rotate} / {@code matrix} — affine maps are exact on Bézier control
 * points), and per-element {@code fill} / {@code stroke} /
 * {@code stroke-width} styling with SVG's inheritance and defaults
 * (missing {@code fill} paints black, {@code fill="none"} skips the fill).
 *
 * <p>Each layer is one {@link SvgPath} with its resolved paint, in document
 * order — render them back-to-front. The DSL does exactly that:
 * {@code flow.addSvgIcon(icon, 48)} stacks the layers into the page at the
 * requested width with the icon's own aspect ratio.</p>
 *
 * <p>Out of scope (deliberately, this is an icon reader, not a browser):
 * gradients, CSS stylesheets and classes, text, masks, filters,
 * {@code <use>} references, and animations. The XML reader refuses
 * DOCTYPEs, so external-entity tricks cannot reach the file system.</p>
 *
 * <pre>{@code
 * SvgIcon logo = SvgIcon.read(Path.of("assets/logo.svg"));
 * flow.addSvgIcon(logo, 48);
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
     * One drawable layer: normalized geometry plus its resolved paint.
     *
     * @param geometry normalized path geometry (shared icon frame)
     * @param fill     fill colour, or {@code null} for no fill
     * @param stroke   outline stroke, or {@code null} for no stroke
     * @since 1.8.0
     */
    public record Layer(SvgPath geometry, DocumentColor fill, DocumentStroke stroke) {
        /**
         * Validates the geometry reference.
         */
        public Layer {
            Objects.requireNonNull(geometry, "geometry");
        }
    }
}
