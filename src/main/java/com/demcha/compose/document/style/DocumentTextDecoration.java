package com.demcha.compose.document.style;

/**
 * Text decoration modes exposed by the canonical document API.
 *
 * <p>The values intentionally mirror the renderer-supported styles while
 * keeping application imports under {@code com.demcha.compose.document.*}.</p>
 *
 * @author Artem Demchyshyn
 */
public enum DocumentTextDecoration {
    /**
     * Regular text without additional decoration.
     */
    DEFAULT,
    /**
     * Bold text.
     */
    BOLD,
    /**
     * Italic text.
     */
    ITALIC,
    /**
     * Bold italic text.
     */
    BOLD_ITALIC,
    /**
     * Underlined text.
     */
    UNDERLINE,
    /**
     * Strikethrough text.
     */
    STRIKETHROUGH
}
