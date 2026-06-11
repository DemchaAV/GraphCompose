package com.demcha.compose.document.style;

import java.awt.*;
import java.util.Objects;

/**
 * Public document color value used by the canonical DSL.
 *
 * <p>This class keeps examples and application code on the document authoring
 * surface instead of requiring imports from the internal engine style package.
 * Instances are immutable and thread-safe.</p>
 *
 * @author Artem Demchyshyn
 */
public final class DocumentColor {
    /**
     * Black color token.
     */
    public static final DocumentColor BLACK = rgb(0, 0, 0);
    /**
     * Dark gray color token.
     */
    public static final DocumentColor DARK_GRAY = rgb(64, 64, 64);
    /**
     * Gray color token.
     */
    public static final DocumentColor GRAY = rgb(128, 128, 128);
    /**
     * Light gray color token.
     */
    public static final DocumentColor LIGHT_GRAY = rgb(192, 192, 192);
    /**
     * White color token.
     */
    public static final DocumentColor WHITE = rgb(255, 255, 255);
    /**
     * Royal blue color token.
     */
    public static final DocumentColor ROYAL_BLUE = rgb(65, 105, 225);
    /**
     * Orange color token.
     */
    public static final DocumentColor ORANGE = rgb(255, 165, 0);

    private final Color color;

    private DocumentColor(Color color) {
        this.color = Objects.requireNonNull(color, "color");
    }

    /**
     * Creates a document color from an AWT color.
     *
     * @param color source color
     * @return immutable document color
     */
    public static DocumentColor of(Color color) {
        return new DocumentColor(color);
    }

    /**
     * Creates a document color from RGB components.
     *
     * @param red   red channel from 0 to 255
     * @param green green channel from 0 to 255
     * @param blue  blue channel from 0 to 255
     * @return immutable document color
     */
    public static DocumentColor rgb(int red, int green, int blue) {
        return new DocumentColor(new Color(red, green, blue));
    }

    /**
     * Creates a translucent document color from RGBA components.
     *
     * <p>The PDF backend honours the alpha channel on shape fills and strokes —
     * rectangles/panels/bars, chart value-label halos, ellipses (chart point
     * markers), polygons, and inline shapes — via a graphics-state alpha
     * constant. Text, lines, and the DOCX backend currently render the colour
     * fully opaque.</p>
     *
     * @param red   red channel from 0 to 255
     * @param green green channel from 0 to 255
     * @param blue  blue channel from 0 to 255
     * @param alpha alpha channel from 0 (transparent) to 255 (opaque)
     * @return immutable document color
     * @since 1.8.0
     */
    public static DocumentColor rgba(int red, int green, int blue, int alpha) {
        return new DocumentColor(new Color(red, green, blue, alpha));
    }

    /**
     * Returns a copy of this color with the supplied opacity.
     *
     * @param opacity opacity from 0.0 (transparent) to 1.0 (opaque)
     * @return translucent copy
     * @since 1.8.0
     */
    public DocumentColor withOpacity(double opacity) {
        if (opacity < 0.0 || opacity > 1.0 || Double.isNaN(opacity)) {
            throw new IllegalArgumentException("opacity must be in [0,1]: " + opacity);
        }
        return new DocumentColor(new Color(
                color.getRed(), color.getGreen(), color.getBlue(),
                (int) Math.round(opacity * 255.0)));
    }

    /**
     * Returns the Java color used by the renderer adapter layer.
     *
     * @return AWT color value
     */
    public Color color() {
        return color;
    }
}
