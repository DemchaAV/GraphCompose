package com.demcha.compose.document.templates.core.identity;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.document.svg.SvgIcon;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A bundled, recolorable single-colour vector glyph for CV contact / social
 * rows.
 *
 * <h2>What it is</h2>
 *
 * <p>CV templates draw small monochrome glyphs (phone / email / location /
 * LinkedIn …). Since v1.8.0 these ship as classpath SVGs instead of raster
 * PNGs, which shrinks the published artifact dramatically. The engine renders
 * the colours baked into an SVG file as-is (there is no render-time tint), so
 * this helper flattens an icon's <em>filled</em> layers into one
 * {@link ShapeOutline} silhouette whose colour is then chosen by the caller via
 * {@code rich.shape(outline, colour)}. That is what lets the same glyph render
 * in each template's own accent colour without per-template copies.</p>
 *
 * <h2>How it works</h2>
 *
 * <p>{@link #fromResource(String)} parses the SVG with {@link SvgIcon} (which
 * normalizes every path into one shared unit frame), concatenates the segments
 * of the layers that carry a fill, and caches the result. Stroke-only
 * decorative sub-paths ({@code fill="none"}) are dropped; if an icon has no
 * filled layer at all its full geometry is used as a fallback so it still
 * renders. {@link #outline(double)} scales that silhouette to a target width,
 * preserving the icon's aspect ratio. The outline fills under the non-zero
 * winding rule, so authored holes (handset cut-outs, letter counters) stay
 * open.</p>
 *
 * <h2>Reuse</h2>
 *
 * <p>Lives in the core identity layer as the shared glyph primitive behind
 * the header widgets and per-preset contact rows. Reuse it instead of
 * loading icon bytes by hand.</p>
 */
public final class SvgGlyph {

    private static final Map<String, SvgGlyph> CACHE = new ConcurrentHashMap<>();

    private final List<DocumentPathSegment> segments;
    private final double aspectRatio;

    private SvgGlyph(List<DocumentPathSegment> segments, double aspectRatio) {
        this.segments = segments;
        this.aspectRatio = aspectRatio;
    }

    /**
     * Loads and caches the flattened glyph for a classpath SVG resource.
     *
     * @param resourcePath absolute classpath path, e.g.
     *                     {@code "/templates/cv/timeline-minimal/icons/email.svg"}
     * @return the cached, recolorable glyph
     * @throws IllegalStateException if the resource is missing
     * @throws UncheckedIOException  if the resource cannot be read
     */
    public static SvgGlyph fromResource(String resourcePath) {
        return CACHE.computeIfAbsent(resourcePath, SvgGlyph::load);
    }

    /**
     * Loads the flattened glyph for an SVG file on disk.
     *
     * <p>The classpath variant covers glyphs a template ships with. This one
     * covers the glyph a template is <em>given</em> — a receipt's issuer mark,
     * a report's client logo — which arrives as a file next to the running
     * application rather than repackaged into its jar.</p>
     *
     * <p><strong>Not cached</strong>, unlike {@link #fromResource(String)}. A
     * classpath entry cannot change while the JVM runs and the key space is the
     * jar's; a file can be replaced, and the key space is whatever paths a
     * caller passes — a service rendering for a hundred issuers would hold a
     * hundred glyphs for its lifetime and keep serving the logo each one had
     * when it started. Callers that load the same mark per document should hold
     * the returned glyph themselves; it is immutable and safe to share.</p>
     *
     * @param file path to the SVG file
     * @return the recolorable glyph
     * @throws IllegalStateException if the file does not exist
     * @throws UncheckedIOException  if the file cannot be read
     * @since 2.1.2
     */
    public static SvgGlyph fromFile(Path file) {
        Objects.requireNonNull(file, "file");
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("Missing glyph file: " + file);
        }
        try {
            return flatten(SvgIcon.parse(Files.readString(file, StandardCharsets.UTF_8)),
                    file.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read glyph file: " + file, e);
        }
    }

    private static SvgGlyph load(String resourcePath) {
        try (InputStream input = SvgGlyph.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing CV glyph resource: " + resourcePath);
            }
            return flatten(SvgIcon.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8)),
                    resourcePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read CV glyph: " + resourcePath, e);
        }
    }

    private static SvgGlyph flatten(SvgIcon icon, String source) {
        List<DocumentPathSegment> filled = new ArrayList<>();
        for (SvgIcon.Layer layer : icon.layers()) {
            if (isInkFill(layer.fill()) || layer.fillPaint() != null) {
                filled.addAll(layer.geometry().segments());
            }
        }
        if (filled.isEmpty()) {
            // Stroke-only / unfilled icon: keep every path so the glyph
            // still renders as a filled silhouette rather than vanishing.
            for (SvgIcon.Layer layer : icon.layers()) {
                filled.addAll(layer.geometry().segments());
            }
        }
        if (filled.isEmpty()) {
            throw new IllegalStateException("Glyph has no drawable geometry: " + source);
        }
        return new SvgGlyph(List.copyOf(filled), icon.aspectRatio());
    }

    /**
     * Reports whether a fill is "ink" (part of the glyph) rather than a
     * near-white background. Many svgrepo icons wrap the figure in a full-box
     * {@code <rect fill="white"/>} backing plate; when every layer is recolored
     * to one ink colour, that white paper would flood the whole box and swallow
     * the figure (it reads as an inverted, solid tile). Treating near-white
     * fills as background keeps only the real silhouette.
     */
    private static boolean isInkFill(DocumentColor fill) {
        if (fill == null) {
            return false;
        }
        java.awt.Color awt = fill.color();
        return awt.getRed() < 245 || awt.getGreen() < 245 || awt.getBlue() < 245;
    }

    /**
     * Returns the glyph as a path outline at {@code width} points; the height
     * follows the icon's aspect ratio. Fill it with any colour via
     * {@code rich.shape(glyph.outline(w), colour)}.
     *
     * @param width target width in points; must be positive
     * @return a path {@link ShapeOutline} of this glyph
     */
    public ShapeOutline outline(double width) {
        double height = width / aspectRatio;
        return ShapeOutline.path(width, height, segments);
    }

    /**
     * Returns the glyph's width-to-height ratio for proportional sizing.
     *
     * @return aspect ratio (width / height)
     */
    public double aspectRatio() {
        return aspectRatio;
    }
}
