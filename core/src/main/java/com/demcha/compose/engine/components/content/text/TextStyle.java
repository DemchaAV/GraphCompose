package com.demcha.compose.engine.components.content.text;

import com.demcha.compose.font.FontName;
import lombok.Builder;

import java.awt.*;

/**
 * Engine-side text style.
 *
 * <p>{@code letterSpacing} is the tracking already <em>resolved to points</em>:
 * the public {@code DocumentLetterSpacing} keeps its unit (points or a share of
 * the font size) and the single conversion seam resolves it, so nothing below
 * this type has to remember to multiply by the font size.</p>
 *
 * @param fontName      font family name
 * @param size          font size in points
 * @param decoration    text decoration
 * @param color         text color
 * @param letterSpacing tracking in points, already resolved; {@code 0} for none
 */
@Builder
public record TextStyle(FontName fontName,
                        double size,
                        TextDecoration decoration,
                        Color color,
                        double letterSpacing) {
public static  TextStyle DEFAULT_STYLE = new TextStyle(FontName.HELVETICA, 14, TextDecoration.DEFAULT, Color.BLACK);

    /**
     * Creates a style without tracking &mdash; the shape this record had before
     * {@code letterSpacing} was added, kept so the existing engine call sites
     * that build a style from four values stay as they are.
     *
     * @param fontName   font family name
     * @param size       font size in points
     * @param decoration text decoration
     * @param color      text color
     */
    public TextStyle(FontName fontName, double size, TextDecoration decoration, Color color) {
        this(fontName, size, decoration, color, 0.0);
    }
}

