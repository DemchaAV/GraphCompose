package com.demcha.compose.document.backend.fixed.pptx.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One-time capability warnings for PPTX features that render with a documented
 * deviation, so a large document logs each deviation once instead of once per
 * fragment — the same convention the DOCX semantic backend uses for its
 * clip/transform fallbacks.
 */
final class PptxCapabilityNotes {

    private static final Logger LOG = LoggerFactory.getLogger("com.demcha.compose.engine.render");
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private PptxCapabilityNotes() {
    }

    static void numericDashApproximated() {
        warnOnce("dash",
                "numeric dash patterns map to the generic dashed preset — DrawingML dashes are "
                + "percent-of-line-width presets, not point arrays");
    }

    static void perCornerRadiiApproximated() {
        warnOnce("corner-radii",
                "distinct per-corner radii render with the top-left radius on all four corners — "
                + "the PPTX roundRect preset carries a single adjust value");
    }

    static void clipSkipped() {
        warnOnce("clip",
                "clip regions render unclipped — DrawingML has no graphics-state clipping; "
                + "use the PDF backend or raster-slide mode for exact clipping");
    }

    static void gradientGeometryApproximated() {
        warnOnce("gradient-geometry",
                "radial gradient centres and explicit linear axes approximate — DrawingML "
                + "expresses gradients relative to the shape box, not exact endpoints");
    }

    static void gradientApproximated() {
        warnOnce("gradient",
                "inline SVG gradient layers render rasterized or as their primary color - "
                + "per-layer gradient geometry has no native DrawingML mapping inside a glyph box");
    }

    static void inlineSvgRasterized() {
        warnOnce("inline-svg-raster",
                "inline SVG layers that require exact clipping or stroke styles render through "
                + "a transparent PNG fallback; simple layers remain native DrawingML paths");
    }

    private static void warnOnce(String key, String message) {
        if (WARNED.add(key)) {
            LOG.warn("render.pptx.capability {}", message);
        }
    }
}
