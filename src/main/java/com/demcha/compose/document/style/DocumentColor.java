package com.demcha.compose.document.style;

import java.awt.Color;
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
     * @param red red channel from 0 to 255
     * @param green green channel from 0 to 255
     * @param blue blue channel from 0 to 255
     * @return immutable document color
     */
    public static DocumentColor rgb(int red, int green, int blue) {
        return new DocumentColor(new Color(red, green, blue));
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
