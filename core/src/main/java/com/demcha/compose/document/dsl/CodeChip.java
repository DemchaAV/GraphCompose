package com.demcha.compose.document.dsl;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.style.InlineBackground;
import com.demcha.compose.font.FontName;

/**
 * Default styling for the {@code code(...)} inline-chip sugar, single-sourced so
 * {@link RichText} and {@link ParagraphBuilder} stay in lockstep. GitHub-ish:
 * a monospace glyph in a muted ink on a light, translucent rounded fill.
 */
final class CodeChip {

    /** Muted code ink (GitHub light {@code #24292f}). */
    static final DocumentColor TEXT_COLOR = DocumentColor.rgb(36, 41, 47);

    /** Light translucent chip fill (GitHub {@code rgba(175,184,193,.2)}). */
    static final InlineBackground BACKGROUND = new InlineBackground(
            DocumentColor.rgb(175, 184, 193).withOpacity(0.20), 3.0, DocumentInsets.symmetric(1.0, 4.0));

    /** Monospace code glyph style. */
    static final DocumentTextStyle STYLE = DocumentTextStyle.builder()
            .fontName(FontName.COURIER)
            .color(TEXT_COLOR)
            .build();

    private CodeChip() {
    }
}
