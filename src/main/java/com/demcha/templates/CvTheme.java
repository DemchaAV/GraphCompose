package com.demcha.templates;

import com.demcha.compose.font_library.FontName;
import com.demcha.compose.layout_core.components.content.text.TextDecoration;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Margin;

import java.awt.Color;

/**
 * Reusable visual theme for the template layer.
 * <p>
 * {@code CvTheme} groups the font families, colors, font sizes, spacing values,
 * and semantic text styles used by {@link TemplateBuilder}. It keeps styling
 * decisions separate from layout structure so templates can stay readable and
 * easy to swap between themes.
 * </p>
 *
 * @deprecated Use {@link com.demcha.compose.document.templates.theme.CvTheme}
 *             instead.
 */
@Deprecated(forRemoval = false)
public record CvTheme(
        Color primaryColor,      // e.g. name colour
        Color secondaryColor,    // e.g. module titles
        Color bodyColor,         // normal text
        Color accentColor,       // links / accents
        FontName headerFont,
        FontName bodyFont,
        double nameFontSize,
        double headerFontSize,
        double bodyFontSize,
        double spacing,
        Margin modulMargin,
        double spacingModuleName

) {

    public Margin moduleMargin() {
        return modulMargin;
    }

    /* --------- READY TextStyle FACTORIES (semantic) --------- */

    /**
     * Style for the main display name at the top of a document.
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
     * Style for module and section headings.
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
     * Default body text style.
     */
    public TextStyle bodyTextStyle() {
        return TextStyle.builder()
                .size(bodyFontSize)
                .color(bodyColor)
                .fontName(bodyFont)
                .build();
    }

    /**
     * Secondary body text style for metadata such as phone, address, or labels.
     */
    public TextStyle smallBodyTextStyle() {
        return TextStyle.builder()
                .size(bodyFontSize - 1) // tweak if you want
                .color(bodyColor)
                .fontName(bodyFont)
                .build();
    }

    /**
     * Style for link-like values such as email or profile URLs.
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
     * Style for bullet-like labels or short list item titles.
     */
    public TextStyle bulletTitleTextStyle() {
        return TextStyle.builder()
                .size(bodyFontSize)
                .color(bodyColor)
                .fontName(bodyFont)
                .build();
    }

    /**
     * Optional emphasis style for highlighted text inside body content.
     */
    public TextStyle emphasisTextStyle() {
        return TextStyle.builder()
                .size(bodyFontSize)
                .color(primaryColor)
                .fontName(bodyFont)
                .build();
    }

    /* --------- DEFAULT THEME --------- */

    /**
     * Returns the default bundled theme used by documentation examples.
     */
    public static CvTheme defaultTheme() {
        return new CvTheme(
                new Color(44, 62, 80),                      // primary (name)
                new Color(41, 128, 185),                    // secondary (headers)
                ComponentColor.MODULE_LINE_TEXT,            // body text
                ComponentColor.ROYAL_BLUE,                  // accent / links
                FontName.HELVETICA,
                FontName.HELVETICA,
                28,        // name
                17.4,      // header
                10,// body
                5 // spacing
                ,Margin.top(5),
                0

        );
    }

    /**
     * Legacy compatibility alias for {@link #timesRoman()}.
     */
    @Deprecated(forRemoval = false)
    public static CvTheme timeRoman() {
        return timesRoman();
    }

    /**
     * Returns a Times-Roman based theme.
     */
    public static CvTheme timesRoman() {
        return new CvTheme(
                new Color(44, 62, 80),                      // primary (name)
                new Color(41, 128, 185),                    // secondary (headers)
                ComponentColor.MODULE_LINE_TEXT,            // body text
                ComponentColor.BLACK,                  // accent / links
                FontName.TIMES_ROMAN,
                FontName.TIMES_ROMAN,
                28,        // name
                17.4,      // header
                10,// body
                5 // spacing
                ,Margin.top(5),
                5

        );
    }
    /**
     * Returns a Courier-based theme.
     */
    public static CvTheme courier() {
        return new CvTheme(
                new Color(44, 62, 80),                      // primary (name)
                new Color(41, 128, 185),                    // secondary (headers)
                ComponentColor.MODULE_LINE_TEXT,            // body text
                ComponentColor.BLACK,                  // accent / links
                FontName.COURIER,
                FontName.COURIER,
                28,        // name
                17.4,      // header
                10,// body
                5 // spacing
                ,Margin.top(5),
                5

        );
    }
}
