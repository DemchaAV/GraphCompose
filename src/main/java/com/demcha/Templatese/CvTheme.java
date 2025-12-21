package com.demcha.Templatese;

import com.demcha.font_library.FontName;
import com.demcha.loyaut_core.components.content.text.TextDecoration;
import com.demcha.loyaut_core.components.content.text.TextStyle;
import com.demcha.loyaut_core.components.style.ComponentColor;
import com.demcha.loyaut_core.components.style.Margin;

import java.awt.Color;

/**
 * Visual theme for CV: colours + fonts + sizes + ready TextStyles.
 */
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

    /* --------- READY TextStyle FACTORIES (semantic) --------- */

    /** Big name at the top. */
    public TextStyle nameTextStyle() {
        return TextStyle.builder()
                .size(nameFontSize)
                .color(primaryColor)
                .decoration(TextDecoration.BOLD)
                .fontName(headerFont)
                .build();
    }

    /** Section/module headers (e.g. "Professional Experience"). */
    public TextStyle sectionHeaderTextStyle() {
        return TextStyle.builder()
                .size(headerFontSize)
                .color(secondaryColor)
                .decoration(TextDecoration.BOLD)
                .fontName(headerFont)
                .build();
    }

    /** Normal body text. */
    public TextStyle bodyTextStyle() {
        return TextStyle.builder()
                .size(bodyFontSize)
                .color(bodyColor)
                .fontName(bodyFont)
                .build();
    }

    /** Small muted body text (e.g. address, phone, meta). */
    public TextStyle smallBodyTextStyle() {
        return TextStyle.builder()
                .size(bodyFontSize - 1) // tweak if you want
                .color(bodyColor)
                .fontName(bodyFont)
                .build();
    }

    /** Link text (email, LinkedIn, GitHub). */
    public TextStyle linkTextStyle() {
        return TextStyle.builder()
                .size(bodyFontSize)
                .color(accentColor)
                .fontName(bodyFont)
                .decoration(TextDecoration.UNDERLINE)
                .build();
    }

    /** Bullet / list item title (e.g. skill name). */
    public TextStyle bulletTitleTextStyle() {
        return TextStyle.builder()
                .size(bodyFontSize)
                .color(bodyColor)
                .fontName(bodyFont)
                .build();
    }

    /** Optional: emphasised text inside body. */
    public TextStyle emphasisTextStyle() {
        return TextStyle.builder()
                .size(bodyFontSize)
                .color(primaryColor)
                .fontName(bodyFont)
                .build();
    }

    /* --------- DEFAULT THEME --------- */

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
                5

        );
    }

    public static CvTheme timeRoman() {
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
}
