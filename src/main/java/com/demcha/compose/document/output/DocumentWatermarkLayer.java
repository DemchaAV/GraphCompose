package com.demcha.compose.document.output;

/**
 * Backend-neutral watermark layer selector.
 *
 * @author Artem Demchyshyn
 */
public enum DocumentWatermarkLayer {
    /** Watermark renders behind page content. */
    BEHIND_CONTENT,
    /** Watermark renders on top of page content. */
    ABOVE_CONTENT
}
