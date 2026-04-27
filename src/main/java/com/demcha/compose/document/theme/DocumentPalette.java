package com.demcha.compose.document.theme;

import com.demcha.compose.document.style.DocumentColor;

import java.awt.Color;
import java.util.Objects;

/**
 * Semantic color palette for business documents (invoice, proposal, report).
 *
 * <p>Tokens are referenced by role rather than by value so a single template can
 * be re-skinned by swapping {@link BusinessTheme}s.</p>
 *
 * @param primary headline color (titles, primary buttons, brand accents)
 * @param accent secondary accent (status keywords, hyperlinks, highlights)
 * @param surface dominant background tone (page, default panel)
 * @param surfaceMuted subdued background (soft panels, table zebra)
 * @param textPrimary primary body text color
 * @param textMuted secondary/caption text color
 * @param rule rule/divider/border color used by tables and accent strips
 *
 * @author Artem Demchyshyn
 */
public record DocumentPalette(
        DocumentColor primary,
        DocumentColor accent,
        DocumentColor surface,
        DocumentColor surfaceMuted,
        DocumentColor textPrimary,
        DocumentColor textMuted,
        DocumentColor rule
) {
    /**
     * Validates required references and freezes the palette tokens.
     */
    public DocumentPalette {
        Objects.requireNonNull(primary, "primary");
        Objects.requireNonNull(accent, "accent");
        Objects.requireNonNull(surface, "surface");
        Objects.requireNonNull(surfaceMuted, "surfaceMuted");
        Objects.requireNonNull(textPrimary, "textPrimary");
        Objects.requireNonNull(textMuted, "textMuted");
        Objects.requireNonNull(rule, "rule");
    }

    /**
     * Convenience factory accepting AWT colors.
     *
     * @param primary primary color
     * @param accent accent color
     * @param surface surface color
     * @param surfaceMuted muted surface color
     * @param textPrimary primary text color
     * @param textMuted muted text color
     * @param rule rule/border color
     * @return palette built from raw AWT colors
     */
    public static DocumentPalette of(Color primary,
                                     Color accent,
                                     Color surface,
                                     Color surfaceMuted,
                                     Color textPrimary,
                                     Color textMuted,
                                     Color rule) {
        return new DocumentPalette(
                DocumentColor.of(primary),
                DocumentColor.of(accent),
                DocumentColor.of(surface),
                DocumentColor.of(surfaceMuted),
                DocumentColor.of(textPrimary),
                DocumentColor.of(textMuted),
                DocumentColor.of(rule));
    }
}
