package com.demcha.compose.layout_core.system.implemented_systems.word_systems;

import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.system.interfaces.FontBase;

/**
 * Implementation of FontBase for Word documents.
 * <p>
 * In Word (Apache POI), fonts are identified by their family name (e.g., "Arial").
 * Therefore, the generic type T is String.
 */
public class WordFont extends FontBase<String> {

    public WordFont(String defaultFont, String bold, String italic, String boldItalic, String underline, String strikethrough) {
        super(defaultFont, bold, italic, boldItalic, underline, strikethrough);
    }

    public WordFont(String defaultFont, String bold, String italic, String boldItalic) {
        super(defaultFont, bold, italic, boldItalic);
    }

    // Optional: A helper constructor for standard usage where the font family stays the same
    // and styles (bold/italic) are applied via flags later, but we need to satisfy the parent structure.
    public WordFont(String fontFamily) {
        super(fontFamily, fontFamily, fontFamily, fontFamily);
    }

    /**
     * @param style
     * @param text
     * @return
     */
    @Override
    public double getTextWidth(TextStyle style, String text) {
        return 0;
    }

    /**
     * @param style
     * @param text
     * @return
     */
    @Override
    public double getTextWidthNoSanitize(TextStyle style, String text) {
        return 0;
    }

    /**
     * @param style
     * @return
     */
    @Override
    public double getLineHeight(TextStyle style) {
        return 0;
    }

    /**
     * @param style
     * @return
     */
    @Override
    public double getTextHeight(TextStyle style) {
        return 0;
    }

    /**
     * @param style
     * @return
     */
    @Override
    public double getCapHeight(TextStyle style) {
        return 0;
    }

    /**
     * @param size
     * @return
     */
    @Override
    public double scale(double size) {
        return 0;
    }

    /**
     * @param text
     * @param style
     * @param availableWidth
     * @return
     */
    @Override
    public TextStyle adjustFontSizeToFit(String text, TextStyle style, double availableWidth) {
        return null;
    }

    /**
     * @param text
     * @param style
     * @return
     */
    @Override
    public ContentSize getTightBounds(String text, TextStyle style) {
        return null;
    }
}
