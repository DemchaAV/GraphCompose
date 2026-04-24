package com.demcha.compose.document.templates.theme;

import com.demcha.compose.font.FontName;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.style.ComponentColor;
import com.demcha.compose.engine.components.style.Margin;

import java.awt.Color;

/**
 * Reusable visual theme for CV and cover-letter style templates.
 *
 * @param primaryColor primary heading color
 * @param secondaryColor section heading color
 * @param bodyColor body text color
 * @param accentColor accent/link color
 * @param headerFont font used for display headings
 * @param bodyFont font used for body text
 * @param nameFontSize display name font size
 * @param headerFontSize section heading font size
 * @param bodyFontSize body font size
 * @param spacing default wrapped-line/list spacing
 * @param modulMargin historical module margin token
 * @param spacingModuleName spacing around module titles
 */
public record CvTheme(
        Color primaryColor,
        Color secondaryColor,
        Color bodyColor,
        Color accentColor,
        FontName headerFont,
        FontName bodyFont,
        double nameFontSize,
        double headerFontSize,
        double bodyFontSize,
        double spacing,
        Margin modulMargin,
        double spacingModuleName
) {

    /**
     * Returns the shared module margin token.
     *
     * @return module margin
     */
    public Margin moduleMargin() {
        return modulMargin;
    }

    /**
     * Style for the main display name.
     *
     * @return semantic text style
     */
    public TextStyle nameTextStyle() {
        return TextStyle.builder()
                .size(nameFontSize)
                .color(primaryColor)
                .decoration(TextDecoration.BOLD)
                .fontName(headerFont)
                .build();
    }

    /**
     * Style for section headings.
     *
     * @return semantic text style
     */
    public TextStyle sectionHeaderTextStyle() {
        return TextStyle.builder()
                .size(headerFontSize)
                .color(secondaryColor)
                .decoration(TextDecoration.BOLD)
                .fontName(headerFont)
                .build();
    }

    /**
     * Style for default body text.
     *
     * @return semantic text style
     */
    public TextStyle bodyTextStyle() {
        return TextStyle.builder()
                .size(bodyFontSize)
                .color(bodyColor)
                .fontName(bodyFont)
                .build();
    }

    /**
     * Style for compact metadata text.
     *
     * @return semantic text style
     */
    public TextStyle smallBodyTextStyle() {
        return TextStyle.builder()
                .size(bodyFontSize - 1)
                .color(bodyColor)
                .fontName(bodyFont)
                .build();
    }

    /**
     * Style for link-like values.
     *
     * @return semantic text style
     */
    public TextStyle linkTextStyle() {
        return TextStyle.builder()
                .size(bodyFontSize)
                .color(accentColor)
                .fontName(bodyFont)
                .decoration(TextDecoration.UNDERLINE)
                .build();
    }

    /**
     * Default bundled theme used by the standard CV template.
     *
     * @return default CV theme
     */
    public static CvTheme defaultTheme() {
        return new CvTheme(
                new Color(44, 62, 80),
                new Color(41, 128, 185),
                ComponentColor.MODULE_LINE_TEXT,
                ComponentColor.ROYAL_BLUE,
                FontName.HELVETICA,
                FontName.HELVETICA,
                28,
                17.4,
                10,
                5,
                Margin.top(5),
                0);
    }

    /**
     * Returns a Times-Roman based theme.
     *
     * @return Times-Roman theme
     */
    public static CvTheme timesRoman() {
        return new CvTheme(
                new Color(44, 62, 80),
                new Color(41, 128, 185),
                ComponentColor.MODULE_LINE_TEXT,
                ComponentColor.BLACK,
                FontName.TIMES_ROMAN,
                FontName.TIMES_ROMAN,
                28,
                17.4,
                10,
                5,
                Margin.top(5),
                5);
    }

    /**
     * Returns a Courier-based theme.
     *
     * @return Courier theme
     */
    public static CvTheme courier() {
        return new CvTheme(
                new Color(44, 62, 80),
                new Color(41, 128, 185),
                ComponentColor.MODULE_LINE_TEXT,
                ComponentColor.BLACK,
                FontName.COURIER,
                FontName.COURIER,
                28,
                17.4,
                10,
                5,
                Margin.top(5),
                5);
    }
}
