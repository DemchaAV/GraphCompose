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

    static void gradientApproximated() {
        warnOnce("gradient",
                "gradient fills render as the gradient's primary color until DrawingML gradient "
                + "support lands");
    }

    private static void warnOnce(String key, String message) {
        if (WARNED.add(key)) {
            LOG.warn("render.pptx.capability {}", message);
        }
    }
}
