package com.demcha.components.content.text;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.*;
import java.io.IOException;

@Slf4j
@Builder
public record TextStyle(PDFont font, int size, TextDecoration decoration, Color color) {

    public static PDFont TIMES_ROMAN = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
    public static PDFont TIMES_BOLD = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
    public static PDFont TIMES_ITALIC = new PDType1Font(Standard14Fonts.FontName.TIMES_ITALIC);
    public static PDFont TIMES_BOLD_ITALIC = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD_ITALIC);
    public static PDFont HELVETICA = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    public static PDFont HELVETICA_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    public static PDFont HELVETICA_OBLIQUE = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
    public static PDFont HELVETICA_BOLD_OBLIQUE = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
    public static PDFont COURIER = new PDType1Font(Standard14Fonts.FontName.COURIER);
    public static PDFont COURIER_BOLD = new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);
    public static PDFont COURIER_OBLIQUE = new PDType1Font(Standard14Fonts.FontName.COURIER_OBLIQUE);
    public static PDFont COURIER_BOLD_OBLIQUE = new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE);
    public static PDFont SYMBOL = new PDType1Font(Standard14Fonts.FontName.SYMBOL);
    public static PDFont ZAPF_DINGBATS = new PDType1Font(Standard14Fonts.FontName.ZAPF_DINGBATS);

    public TextStyle(PDFont font, int size, TextDecoration decoration) {
        this(font, size, decoration, Color.BLACK);
    }

    // Factory methods for standard fonts
    public static TextStyle standard14(String family, int size, TextDecoration deco) {
        return new TextStyle(pickStandard14(family, deco), size, deco, Color.BLACK);
    }

    public static TextStyle standard14(String family, int size, TextDecoration deco, Color color) {
        return new TextStyle(pickStandard14(family, deco), size, deco, color);
    }

    private static PDType1Font pickStandard14(String family, TextDecoration deco) {
        String f = family.trim().toUpperCase(); // "HELVETICA", "TIMES", "COURIER"
        boolean bold = deco == TextDecoration.BOLD || deco == TextDecoration.BOLD_ITALIC;
        boolean italic = deco == TextDecoration.ITALIC || deco == TextDecoration.BOLD_ITALIC;

        if (f.equals("HELVETICA")) {
            if (bold && italic) return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
            if (bold) return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            if (italic) return new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
            return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        }
        if (f.equals("TIMES") || f.equals("TIMES_ROMAN")) {
            if (bold && italic) return new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD_ITALIC);
            if (bold) return new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
            if (italic) return new PDType1Font(Standard14Fonts.FontName.TIMES_ITALIC);
            return new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
        }
        if (f.equals("COURIER")) {
            if (bold && italic) return new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE);
            if (bold) return new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);
            if (italic) return new PDType1Font(Standard14Fonts.FontName.COURIER_OBLIQUE);
            return new PDType1Font(Standard14Fonts.FontName.COURIER);
        }
        // дефолт — Helvetica
        if (bold && italic) return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
        if (bold) return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        if (italic) return new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    // Adjust font size automatically based on the width of the text and available space
    public TextStyle adjustFontSizeToFit(String text, float availableWidth) {
        double textWidth = getTextWidth(text);

        // If textWidth exceeds availableWidth, reduce the font size
        int newSize = size;
        while (textWidth > availableWidth && newSize > 1) {
            newSize--;  // Reduce size
            textWidth = getTextWidth(text);  // Recalculate text width
        }
        return new TextStyle(font, size, decoration, color);
    }

    // Get the width of the text for the given font and size
    public double getTextWidth(String text) {
        try {
            float width = font.getStringWidth(text) / 1000 * size;
            log.debug("Getting text width: " + width);
            return width;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error while getting text width {}", e.getMessage(), e);
            return 0;  // Return 0 if something goes wrong
        }
    }

    public double getTextHeight(String text) {
        try {
            float v = font.getBoundingBox().getHeight() / 1000 * size;
            log.debug("getTextHeight: " + text);
            return v;
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Error while getting text height {}", e.getMessage(), e);
            return 0;  // Return 0 if something goes wrong
        }
    }
    public double getTextHeight(Text text) {
        try {
            float v = font.getBoundingBox().getHeight() / 1000 * size;
            log.debug("getTextHeight: " + text);
            return v;
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Error while getting text height {}", e.getMessage(), e);
            return 0;  // Return 0 if something goes wrong
        }
    }

    // Удобные флаги
    public boolean isBold() {
        return decoration == TextDecoration.BOLD || decoration == TextDecoration.BOLD_ITALIC;
    }

    public boolean isItalic() {
        return decoration == TextDecoration.ITALIC || decoration == TextDecoration.BOLD_ITALIC;
    }

    public boolean isUnderline() {
        return decoration == TextDecoration.UNDERLINE;
    }
}


