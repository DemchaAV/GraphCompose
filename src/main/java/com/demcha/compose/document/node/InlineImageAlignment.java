package com.demcha.compose.document.node;

/**
 * Vertical alignment of an inline image relative to the surrounding text run.
 *
 * <p>Alignment is resolved per visual line. Engine line height is computed as
 * {@code max(textLineHeight, imageHeight)}, then each image is positioned
 * using the resolved alignment plus an optional offset.</p>
 *
 * @author Artem Demchyshyn
 */
public enum InlineImageAlignment {
    /**
     * Aligns the bottom of the image with the baseline of the surrounding
     * text. The image rises above the baseline.
     */
    BASELINE,

    /**
     * Centers the image vertically with the {@code x-height} band of the
     * surrounding text. This is the most common choice for icon-with-label
     * patterns (LinkedIn, GitHub, e-mail) used by CV templates.
     */
    CENTER,

    /**
     * Aligns the top of the image with the top of the line box (ascender
     * line of the surrounding text).
     */
    TEXT_TOP,

    /**
     * Aligns the bottom of the image with the descender line of the
     * surrounding text — useful for badges that should hug the bottom of
     * the line.
     */
    TEXT_BOTTOM
}
