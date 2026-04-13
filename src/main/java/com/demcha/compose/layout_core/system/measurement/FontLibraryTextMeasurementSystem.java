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
        TextStyle safeStyle = style == null ? TextStyle.DEFAULT_STYLE : style;
        Font<?> font = resolveFont(safeStyle);
        return new ContentSize(
                font.getTextWidth(safeStyle, text == null ? "" : text),
                lineMetrics(safeStyle).lineHeight()
        );
    }

    @Override
    public double textWidth(TextStyle style, String text) {
        TextStyle safeStyle = style == null ? TextStyle.DEFAULT_STYLE : style;
        return resolveFont(safeStyle).getTextWidth(safeStyle, text == null ? "" : text);
    }

    @Override
    public LineMetrics lineMetrics(TextStyle style) {
        TextStyle safeStyle = style == null ? TextStyle.DEFAULT_STYLE : style;
        Font<?> font = resolveFont(safeStyle);

        if (font instanceof com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfFont pdfFont) {
            var metrics = pdfFont.verticalMetrics(safeStyle);
            return new LineMetrics(metrics.ascent(), metrics.descent(), metrics.leading());
        }

        double lineHeight = Math.max(0.0, font.getLineHeight(safeStyle));
        return new LineMetrics(lineHeight, 0.0, 0.0);
    }

    private Font<?> resolveFont(TextStyle style) {
        TextStyle safeStyle = style == null ? TextStyle.DEFAULT_STYLE : style;
        return fonts.getFont(safeStyle.fontName(), fontClass)
                .orElseThrow(() -> new IllegalStateException("Font not found for style: " + safeStyle.fontName()));
    }
}
