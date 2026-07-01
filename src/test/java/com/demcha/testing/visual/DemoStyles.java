package com.demcha.testing.visual;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import java.awt.Color;

/**
 * Small "modern business" styling constants for the visual demo tests,
 * replacing the retired {@code BusinessTheme} these demos used incidentally.
 * Values are illustrative — the demo tests assert a valid render, not a
 * pixel baseline.
 */
final class DemoStyles {

    static final DocumentColor PAGE_BACKGROUND = DocumentColor.of(new Color(252, 248, 240));
    static final DocumentColor SURFACE_MUTED = DocumentColor.of(new Color(244, 238, 228));
    static final DocumentColor ACCENT = DocumentColor.of(new Color(196, 153, 76));

    static final DocumentTextStyle H1 =
            text(FontName.HELVETICA_BOLD, 28, DocumentTextDecoration.BOLD);
    static final DocumentTextStyle H3 =
            text(FontName.HELVETICA_BOLD, 13, DocumentTextDecoration.BOLD);
    static final DocumentTextStyle BODY =
            text(FontName.HELVETICA, 11, DocumentTextDecoration.DEFAULT);

    private DemoStyles() {
    }

    private static DocumentTextStyle text(FontName font, double size, DocumentTextDecoration decoration) {
        return DocumentTextStyle.builder()
                .fontName(font)
                .size(size)
                .decoration(decoration)
                .color(DocumentColor.of(new Color(34, 38, 50)))
                .build();
    }
}
