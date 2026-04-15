package com.demcha.compose.document.templates.support;

import com.demcha.compose.font_library.FontName;
import com.demcha.compose.layout_core.components.content.text.TextDecoration;
import com.demcha.compose.layout_core.components.content.text.TextStyle;

import java.awt.Color;

final class BusinessDocumentStyles {
    static final Color TITLE_COLOR = new Color(21, 46, 86);
    static final Color ACCENT_COLOR = new Color(44, 107, 184);
    static final Color BODY_COLOR = new Color(44, 52, 64);
    static final Color MUTED_COLOR = new Color(108, 121, 142);
    static final Color BORDER_COLOR = new Color(128, 148, 178);
    static final Color SOFT_FILL = new Color(243, 247, 252);
    static final Color STRONG_FILL = new Color(224, 234, 246);

    private BusinessDocumentStyles() {
    }

    static TextStyle titleStyle(double size) {
        return TextStyle.builder()
                .fontName(FontName.POPPINS)
                .size(size)
                .decoration(TextDecoration.BOLD)
                .color(TITLE_COLOR)
                .build();
    }

    static TextStyle headingStyle(double size) {
        return TextStyle.builder()
                .fontName(FontName.POPPINS)
                .size(size)
                .decoration(TextDecoration.BOLD)
                .color(TITLE_COLOR)
                .build();
    }

    static TextStyle labelStyle(double size) {
        return TextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(size)
                .decoration(TextDecoration.BOLD)
                .color(ACCENT_COLOR)
                .build();
    }

    static TextStyle bodyStyle(double size) {
        return TextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(size)
                .decoration(TextDecoration.DEFAULT)
                .color(BODY_COLOR)
                .build();
    }

    static TextStyle bodyBoldStyle(double size) {
        return TextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(size)
                .decoration(TextDecoration.BOLD)
                .color(TITLE_COLOR)
                .build();
    }

    static TextStyle metaStyle(double size) {
        return TextStyle.builder()
                .fontName(FontName.IBM_PLEX_SERIF)
                .size(size)
                .decoration(TextDecoration.DEFAULT)
                .color(MUTED_COLOR)
                .build();
    }
}
