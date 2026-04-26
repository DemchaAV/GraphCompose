package com.demcha.compose.document.image;

/**
 * Public image fit policy used when an image is drawn inside explicit bounds.
 *
 * @author Artem Demchyshyn
 */
public enum DocumentImageFitMode {
    /**
     * Stretch the image to exactly fill the resolved image box.
     */
    STRETCH,

    /**
     * Preserve aspect ratio and fit the full image inside the resolved box.
     */
    CONTAIN,

    /**
     * Preserve aspect ratio and cover the resolved box, clipping overflow.
     */
    COVER
}
