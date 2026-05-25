package com.demcha.compose.document.templates.cv.v2.theme;

import java.util.Objects;

/**
 * Glyph / separator tokens for a {@link CvTheme} — the small "what
 * character renders here" decisions that vary per visual flavour but
 * never depend on the layout or the data.
 *
 * <p>This is the answer to "I want to swap the bullet from {@code •}
 * to {@code ▶}", "I want to use {@code  · } as the contact-line
 * separator instead of pipes", or "I want stacked-body lines to align
 * with a 3-space indent instead of 2". None of these need a custom
 * renderer — pass a different {@code CvDecoration} into your theme.</p>
 *
 * <p>Decorations live in the {@code theme} layer because they are
 * cosmetic. They are <strong>not</strong> renderer constants:
 * components like {@code RowRenderer} and {@code ContactRenderer}
 * read these strings from the active theme on every call.</p>
 *
 * @param bulletGlyph      glyph + trailing space used by row styles
 *                         that draw a bullet
 *                         (e.g. {@code "• "}, {@code "▶ "})
 * @param stackedIndent    string prefix used for the second line of a
 *                         {@code BULLETED_STACKED} row — must visually
 *                         occupy the same width as
 *                         {@link #bulletGlyph} so wrapped body text
 *                         aligns with the bold name above
 * @param contactSeparator string used between phone / email /
 *                         address / links on the contact row
 *                         (e.g. {@code "   |   "}, {@code "  ·  "})
 */
public record CvDecoration(String bulletGlyph,
                           String stackedIndent,
                           String contactSeparator) {

    public CvDecoration {
        Objects.requireNonNull(bulletGlyph, "bulletGlyph");
        Objects.requireNonNull(stackedIndent, "stackedIndent");
        Objects.requireNonNull(contactSeparator, "contactSeparator");
    }

    /**
     * The classic decoration: round bullet, two-space stacked indent,
     * pipe contact separator. Used by {@link CvTheme#boxedClassic()}.
     */
    public static CvDecoration classic() {
        return new CvDecoration("• ", "  ", "   |   ");
    }

    /**
     * Blue Banner keeps classic bullets but uses the tighter contact
     * separator spacing from the legacy preset.
     */
    public static CvDecoration blueBanner() {
        return new CvDecoration("• ", "  ", "  |  ");
    }

    /**
     * Compact Mono uses slash-separated contact metadata in its dark
     * command-bar header. Row bullets keep the classic glyph for
     * callers that reuse shared row renderers with this theme.
     */
    public static CvDecoration compactMono() {
        return new CvDecoration("• ", "  ", "  /  ");
    }
}
