package com.demcha.compose.layout_core.system.measurement;

import com.demcha.compose.font_library.FontLibrary;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.system.interfaces.Font;
import com.demcha.compose.layout_core.system.interfaces.TextMeasurementSystem;

import java.util.Objects;

/**
 * Default measurement system backed by a document font library and a concrete
 * font implementation class supplied by the backend runtime.
 */
public final class FontLibraryTextMeasurementSystem implements TextMeasurementSystem {
    private final FontLibrary fonts;
    private final Class<? extends Font<?>> fontClass;

    public FontLibraryTextMeasurementSystem(FontLibrary fonts, Class<? extends Font<?>> fontClass) {
        this.fonts = Objects.requireNonNull(fonts, "fonts");
        this.fontClass = Objects.requireNonNull(fontClass, "fontClass");
    }

    @Override
    public ContentSize measure(TextStyle style, String text) {
        Font<?> font = resolveFont(style);
        return new ContentSize(
                font.getTextWidth(style, text == null ? "" : text),
                font.getLineHeight(style)
        );
    }

    @Override
    public double lineHeight(TextStyle style) {
        return resolveFont(style).getLineHeight(style);
    }

    private Font<?> resolveFont(TextStyle style) {
        TextStyle safeStyle = style == null ? TextStyle.DEFAULT_STYLE : style;
        return fonts.getFont(safeStyle.fontName(), fontClass)
                .orElseThrow(() -> new IllegalStateException("Font not found for style: " + safeStyle.fontName()));
    }
}
