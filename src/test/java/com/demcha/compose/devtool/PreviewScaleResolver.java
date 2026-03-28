package com.demcha.compose.devtool;

/**
 * Resolves the preview render scale from JVM properties while keeping the
 * supported range narrow enough for responsive live preview.
 */
final class PreviewScaleResolver {
    static final String PROPERTY_NAME = "graphcompose.preview.scale";
    static final float DEFAULT_SCALE = 1.0f;
    static final float MIN_SCALE = 0.5f;
    static final float MAX_SCALE = 2.0f;

    private PreviewScaleResolver() {
        // Utility class
    }

    static float fromSystemProperties() {
        return fromProperty(System.getProperty(PROPERTY_NAME));
    }

    static float fromProperty(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return DEFAULT_SCALE;
        }

        try {
            return clamp(Float.parseFloat(rawValue.trim()));
        } catch (NumberFormatException ignored) {
            return DEFAULT_SCALE;
        }
    }

    private static float clamp(float value) {
        if (!Float.isFinite(value)) {
            return DEFAULT_SCALE;
        }

        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
    }
}
