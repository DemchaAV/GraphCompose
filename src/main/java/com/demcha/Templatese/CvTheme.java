package com.demcha.Templatese;

import com.demcha.components.content.text.TextDecoration;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.style.ComponentColor;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;

/**
 * Visual theme for CV: colours + fonts + sizes + ready TextStyles.
 */
public record CvTheme(
        Color primaryColor,      // e.g. name colour
        Color secondaryColor,    // e.g. module titles
        Color bodyColor,         // normal text
        Color accentColor,       // links / accents
        PDFont headerFont,
        PDFont bodyFont,
        double nameFontSize,
        double headerFontSize,
        double bodyFontSize,
        double spacing
) {

    /* --------- READY TextStyle FACTORIES (semantic) --------- */

    /** Big name at the top. */
    public TextStyle nameTextStyle() {
        return TextStyle.builder()
                .size(nameFontSize)
                .color(primaryColor)
                .font(headerFont)
                .build();
    }

    /** Section/module headers (e.g. "Professional Experience"). */
    public TextStyle sectionHeaderTextStyle() {
        return TextStyle.builder()
                .size(headerFontSize)
                .color(secondaryColor)
                .font(headerFont)
                .build();
    }

    /** Normal body text. */
    public TextStyle bodyTextStyle() {
        return TextStyle.builder()
                .size(bodyFontSize)
                .color(bodyColor)
                .font(bodyFont)
                .build();
    }

    /** Small muted body text (e.g. address, phone, meta). */
    public TextStyle smallBodyTextStyle() {
        return TextStyle.builder()
                .size(bodyFontSize - 1) // tweak if you want
                .color(bodyColor)
                .font(bodyFont)
                .build();
    }

    /** Link text (email, LinkedIn, GitHub). */
    public TextStyle linkTextStyle() {
        return TextStyle.builder()
                .size(bodyFontSize)
                .color(accentColor)
                .font(bodyFont)
                .decoration(TextDecoration.UNDERLINE)
                .build();
    }

    /** Bullet / list item title (e.g. skill name). */
    public TextStyle bulletTitleTextStyle() {
        return TextStyle.builder()
                .size(bodyFontSize)
                .color(bodyColor)
                .font(bodyFont)
                .build();
    }

    /** Optional: emphasised text inside body. */
    public TextStyle emphasisTextStyle() {
        return TextStyle.builder()
                .size(bodyFontSize)
                .color(primaryColor)
                .font(bodyFont)
                .build();
    }

    /* --------- DEFAULT THEME --------- */

    public static CvTheme defaultTheme() {
        return new CvTheme(
                new Color(44, 62, 80),                      // primary (name)
                new Color(41, 128, 185),                    // secondary (headers)
                ComponentColor.MODULE_LINE_TEXT,            // body text
                ComponentColor.ROYAL_BLUE,                  // accent / links
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                28,        // name
                17.4,      // header
                11,// body
                6.5 // spacing
        );
    }
}
