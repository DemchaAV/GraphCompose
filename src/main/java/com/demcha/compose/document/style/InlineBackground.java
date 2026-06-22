package com.demcha.compose.document.style;

import java.util.Objects;

/**
 * Background "chip" behind an inline run: a rounded, padded fill drawn beneath
 * the glyphs on the text baseline — e.g. a GitHub-style inline {@code code}
 * highlight. Backend-neutral: the PDF backend paints it as a filled rounded
 * rectangle; a future text backend (DOCX) keeps the text and drops the fill.
 *
 * <p>Horizontal padding widens the run's advance (it reserves space and counts
 * toward line wrapping). Vertical padding expands the chip <em>outside</em> the
 * line box (the chip overflows like a browser highlight) so it never perturbs
 * line metrics or pagination.</p>
 *
 * @param fill         background fill colour; must not be {@code null}
 * @param cornerRadius corner radius in points, clamped to half the chip height
 *                     at paint; must be finite and {@code >= 0}
 * @param padding      inset between the glyph box and the chip edges
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public record InlineBackground(DocumentColor fill, double cornerRadius, DocumentInsets padding) {
    /**
     * Validates the fill/radius and normalizes a null padding to
     * {@link DocumentInsets#zero()}.
     */
    public InlineBackground {
        Objects.requireNonNull(fill, "fill");
        padding = padding == null ? DocumentInsets.zero() : padding;
        if (cornerRadius < 0 || !Double.isFinite(cornerRadius)) {
            throw new IllegalArgumentException("cornerRadius must be finite and >= 0: " + cornerRadius);
        }
    }
}
