package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.layout.PlacedFragment;

import java.awt.geom.Rectangle2D;

/**
 * Single source of truth for mapping resolved layout coordinates into the
 * PPTX drawing space.
 *
 * <p>{@link PlacedFragment} coordinates arrive in the engine's PDF-native page
 * space: origin at the bottom-left of the page, y growing upwards, values in
 * points, with {@code (x, y)} naming the fragment's bottom-left corner. PPTX
 * anchors shapes from the top-left of the slide with y growing downwards, so
 * every vertical coordinate flips against the canvas height. Anchors are
 * expressed in points — POI converts to EMU (1 pt = 12 700 EMU) internally,
 * so no precision is lost in this class.</p>
 *
 * <p>Public because custom {@link PptxFragmentRenderHandler} implementations
 * need the same mapping; the canvas height comes from
 * {@link PptxRenderEnvironment#canvasHeight()}.</p>
 *
 * @since 2.1.0
 */
public final class PptxCoordinates {

    private PptxCoordinates() {
    }

    /**
     * Converts a bottom-left-anchored y coordinate of a box into the slide's
     * top-down space.
     *
     * @param canvasHeight page height in points
     * @param y            box bottom edge in PDF-native space
     * @param height       box height in points
     * @return top edge of the box in slide space
     */
    public static double topY(double canvasHeight, double y, double height) {
        return canvasHeight - y - height;
    }

    /**
     * Flips a single point (not a box edge) into the slide's top-down space.
     *
     * @param canvasHeight page height in points
     * @param y            point y in PDF-native space
     * @return the same point's y in slide space
     */
    public static double flipPoint(double canvasHeight, double y) {
        return canvasHeight - y;
    }

    /**
     * Returns the fragment's box as a top-left-anchored shape anchor in points.
     *
     * @param canvasHeight page height in points
     * @param fragment     placed fragment in PDF-native space
     * @return anchor rectangle for {@code XSLFShape.setAnchor}
     */
    public static Rectangle2D.Double anchorOf(double canvasHeight, PlacedFragment fragment) {
        return new Rectangle2D.Double(
                fragment.x(),
                topY(canvasHeight, fragment.y(), fragment.height()),
                fragment.width(),
                fragment.height());
    }
}
