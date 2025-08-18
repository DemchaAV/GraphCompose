package com.demcha.legacy.components.data.text;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;

public record TextStyle(PDFont font, int size, TextDecoration decoration, Color color) {

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

    // Adjust font size automatically based on the width of the text and available space
    public TextStyle adjustFontSizeToFit(String text, float availableWidth) {
        float textWidth = getTextWidth(text);

        // If textWidth exceeds availableWidth, reduce the font size
        int newSize = size;
        while (textWidth > availableWidth && newSize > 1) {
            newSize--;  // Reduce size
            textWidth = getTextWidth(text);  // Recalculate text width
        }
        return new TextStyle(font, size, decoration, color);
    }

    // Get the width of the text for the given font and size
    public float getTextWidth(String text) {
        try {
            return font.getStringWidth(text) / 1000 * size;  // PDFBox returns width in thousandths of a unit
        } catch (Exception e) {
            e.printStackTrace();
            return 0;  // Return 0 if something goes wrong
        }
    }



    private static PDType1Font pickStandard14(String family, TextDecoration deco) {
        String f = family.trim().toUpperCase(); // "HELVETICA", "TIMES", "COURIER"
        boolean bold   = deco == TextDecoration.BOLD || deco == TextDecoration.BOLD_ITALIC;
        boolean italic = deco == TextDecoration.ITALIC || deco == TextDecoration.BOLD_ITALIC;

        if (f.equals("HELVETICA")) {
            if (bold && italic) return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
            if (bold)          return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            if (italic)        return new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
            return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        }
        if (f.equals("TIMES") || f.equals("TIMES_ROMAN")) {
            if (bold && italic) return new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD_ITALIC);
            if (bold)          return new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
            if (italic)        return new PDType1Font(Standard14Fonts.FontName.TIMES_ITALIC);
            return new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
        }
        if (f.equals("COURIER")) {
            if (bold && italic) return new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE);
            if (bold)          return new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);
            if (italic)        return new PDType1Font(Standard14Fonts.FontName.COURIER_OBLIQUE);
            return new PDType1Font(Standard14Fonts.FontName.COURIER);
        }
        // дефолт — Helvetica
        if (bold && italic) return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
        if (bold)          return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        if (italic)        return new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
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
class testtr{
    public static void main(String[] args) {
        TextStyle style = new TextStyle(new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN), 35, TextDecoration.DEFAULT, Color.BLACK);
        TextData email = new TextData("E",style);
        System.out.println("The width is: " + email.style().getTextWidth(email.value()) );
    }

}
