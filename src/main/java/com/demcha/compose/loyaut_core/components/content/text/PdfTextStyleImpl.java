package com.demcha.compose.loyaut_core.components.content.text;

import com.demcha.compose.loyaut_core.components.renderable.TextComponent;
import com.demcha.compose.loyaut_core.components.style.ComponentColor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
//TODO нужно будте передел стиль текста на уже абстрактные спомощью интерфейсов
@Slf4j
@Builder
public record PdfTextStyleImpl(PDFont font, double size, TextDecoration decoration, Color color)  {

    // 1) Fonts first
    public static final PDFont TIMES_ROMAN = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
    public static final PDFont TIMES_BOLD = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
    public static final PDFont TIMES_ITALIC = new PDType1Font(Standard14Fonts.FontName.TIMES_ITALIC);
    public static final PDFont TIMES_BOLD_ITALIC = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD_ITALIC);
    public static final PDFont HELVETICA = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    public static final PDFont HELVETICA_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    public static final PDFont HELVETICA_OBLIQUE = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
    public static final PDFont HELVETICA_BOLD_OBLIQUE = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
    public static final PDFont COURIER = new PDType1Font(Standard14Fonts.FontName.COURIER);
    public static final PDFont COURIER_BOLD = new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);
    public static final PDFont COURIER_OBLIQUE = new PDType1Font(Standard14Fonts.FontName.COURIER_OBLIQUE);
    public static final PDFont COURIER_BOLD_OBLIQUE = new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE);
    public static final PDFont SYMBOL = new PDType1Font(Standard14Fonts.FontName.SYMBOL);
    public static final PDFont ZAPF_DINGBATS = new PDType1Font(Standard14Fonts.FontName.ZAPF_DINGBATS);

    // 2) Then DEFAULT_STYLE
    public static final PdfTextStyleImpl DEFAULT_STYLE =
            new PdfTextStyleImpl(HELVETICA, 14, TextDecoration.DEFAULT, ComponentColor.TITLE);

    public PdfTextStyleImpl(PDFont font, int size, TextDecoration decoration) {
        this(font, size, decoration, Color.BLACK);
    }

    public static PdfTextStyleImpl defaultStyle() {
        log.debug("Getting default style");
        return new PdfTextStyleImpl(DEFAULT_STYLE.font, DEFAULT_STYLE.size, TextDecoration.DEFAULT, DEFAULT_STYLE.color);

    }


    // Factory methods for standard fonts
    public static PdfTextStyleImpl standard14(String family, int size, TextDecoration deco) {
        return new PdfTextStyleImpl(pickStandard14(family, deco), size, deco, Color.BLACK);
    }

    public static PdfTextStyleImpl standard14(String family, int size, TextDecoration deco, Color color) {
        return new PdfTextStyleImpl(pickStandard14(family, deco), size, deco, color);
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
    public PdfTextStyleImpl adjustFontSizeToFit(String text, float availableWidth) {
        double textWidth = getTextWidth(text);

        // If textWidth exceeds availableWidth, reduce the font size
        double newSize = size;
        while (textWidth > availableWidth && newSize > 1) {
            newSize--;  // Reduce size
            textWidth = getTextWidth(text);  // Recalculate text width
        }
        return new PdfTextStyleImpl(font, size, decoration, color);
    }

    // Get the width of the text for the given font and size
    public double getTextWidth(String text) {
        String sanitized = text.replace("\r", " ").replace("\n", " ")
                .replaceAll("[\\p{Cntrl}&&[^\\t]]", " ")
                .replace('\u00A0',' ').replaceAll(" +", " ");
        try {
            float width =  font.getStringWidth(sanitized) / 1000 * (float) size;
            log.debug("Getting text width: " + width);
            return width;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error while getting text width {}", e.getMessage(), e);
            return 0;  // Return 0 if something goes wrong
        }
    }

    public double getTextHeight() {
        try {
            float v = font.getBoundingBox().getHeight() / 1000 * (float)size;
            log.debug("getTextHeight: " );
            return v;
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Error while getting text height {}", e.getMessage(), e);
            return 0;  // Return 0 if something goes wrong
        }
    }
    /**
     * Line height based on font metrics (recommended for baseline-to-baseline step).
     * Uses ascent, descent (usually negative), and optional leading if present.
     */
    public double getLineHeight() {
        PDFontDescriptor fd = font.getFontDescriptor();
        BoundingBox boundingBox = null;
        try {
            boundingBox = font.getBoundingBox();
        } catch (IOException e) {
            log.error("Error while getting text height {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        float ascent  = (fd != null ? fd.getAscent()  : boundingBox.getUpperRightY());
        float descent = (fd != null ? fd.getDescent() : boundingBox.getLowerLeftY()); // often negative
        float leading = (fd != null ? fd.getLeading() : 0);
        return (ascent - descent + leading) * scale();
    }

    /**
     * Visual height of capitals (useful for tight boxes behind text like buttons/titles).
     */
    public double getCapHeight() throws IOException {
        PDFontDescriptor fd = font.getFontDescriptor();
        float cap = (fd != null ? fd.getCapHeight() : 0);
        if (cap == 0) { // fallback: approximate via bbox
            return font.getBoundingBox().getHeight() * 0.7f * scale();
        }
        return cap * scale();
    }

    // Scale once: PDF units are per 1000 EM
    private float scale() { return (float)size / 1000f; }

    /**
     * Tight per-string bounds (slow but exact for hit-areas/links).
     * Computes the glyph path bounds for THIS string.
     */
    public Rectangle2D getTightBounds(String text) {
        if (text == null || text.isEmpty()) return new Rectangle2D.Float(0,0,0,0);
        GeneralPath path = font.getFontDescriptor().getFontBoundingBox().toGeneralPath();
        AffineTransform at = AffineTransform.getScaleInstance(scale(), scale());
        Shape s = at.createTransformedShape(path);
        return s.getBounds2D(); // width/height in user units; y is relative to baseline
    }

    public double getTextHeight(TextComponent textComponent) {
        try {
            float v = font.getBoundingBox().getHeight() / 1000 *(float) size;
            log.debug("getTextHeight: " + textComponent);
            return v;
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Error while getting textComponent height {}", e.getMessage(), e);
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
