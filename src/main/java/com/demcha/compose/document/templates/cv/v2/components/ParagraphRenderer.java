package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

/**
 * Draws one prose paragraph (markdown-aware) in body style. Used by
 * {@code ParagraphSection} bodies.
 *
 * <p>Implementation is a one-liner delegation to
 * {@link ParagraphPrimitive} — the actual addParagraph DSL plumbing
 * lives there so every body / row / entry renderer agrees on
 * alignment, margin, line spacing, and markdown handling.</p>
 */
public final class ParagraphRenderer {

    private ParagraphRenderer() {
    }

    /**
     * Renders one body-style prose paragraph into the host section.
     *
     * @param section host
     * @param text    paragraph text; blank inputs are silently skipped
     * @param theme   active theme
     */
    public static void render(SectionBuilder section, String text, CvTheme theme) {
        if (text == null || text.isBlank()) {
            return;
        }
        ParagraphPrimitive.writeBody(section, text.trim(), theme.bodyStyle(), theme);
    }
}
