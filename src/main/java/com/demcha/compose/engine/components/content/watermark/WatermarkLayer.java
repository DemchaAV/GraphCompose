package com.demcha.compose.engine.components.content.watermark;

/**
 * Controls whether the watermark draws behind or above page content.
 *
 * @author Artem Demchyshyn
 */
public enum WatermarkLayer {

    /** Watermark is drawn before content — appears behind text and images. */
    BEHIND_CONTENT,

    /** Watermark is drawn after content — overlays text and images. */
    ABOVE_CONTENT
}
