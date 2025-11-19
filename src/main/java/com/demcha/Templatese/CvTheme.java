package com.demcha.Templatese;

import com.demcha.components.content.text.TextStyle;
import com.demcha.components.style.ComponentColor;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import java.awt.Color;

public record CvTheme(
        Color primaryColor,         // e.g., Dark Blue for Names
        Color secondaryColor,       // e.g., Lighter Blue for Module Titles
        Color bodyColor,           // e.g., Dark Grey for text
        Color accentColor,         // e.g., Royal Blue for links
        PDFont headerFont,
        PDFont bodyFont,
        double nameFontSize,
        double headerFontSize,
        double bodyFontSize
) {
    // A static method to provide a default theme (The one you had hardcoded)
    public static CvTheme defaultTheme() {
        return new CvTheme(
                new Color(44, 62, 80),              // Primary
                new Color(41, 128, 185),            // Secondary
                ComponentColor.MODULE_LINE_TEXT,    // Body
                ComponentColor.ROYAL_BLUE,          // Accent
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                30,
                18.4,
                12
        );
    }
}