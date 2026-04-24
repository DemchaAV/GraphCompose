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
    DEFAULT,
    BOLD,
    ITALIC,
    BOLD_ITALIC,
    UNDERLINE,
    STRIKETHROUGH
}
