package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.font.FontFamilyDefinition;

/**
 * A family's four faces, as the source set names them.
 *
 * <p>Shared by {@link PptxDeckFonts}, which records the faces a render's runs drew, and
 * {@link PptxFontEmbedder}, which reads the program each of them names.</p>
 */
enum PptxFontFace {
    REGULAR, BOLD, ITALIC, BOLD_ITALIC;

    static PptxFontFace of(boolean bold, boolean italic) {
        if (bold && italic) {
            return BOLD_ITALIC;
        }
        return bold ? BOLD : italic ? ITALIC : REGULAR;
    }

    FontFamilyDefinition.FontBinarySource in(FontFamilyDefinition.FontSourceSet sources) {
        return switch (this) {
            case BOLD -> sources.bold();
            case ITALIC -> sources.italic();
            case BOLD_ITALIC -> sources.boldItalic();
            case REGULAR -> sources.regular();
        };
    }
}
