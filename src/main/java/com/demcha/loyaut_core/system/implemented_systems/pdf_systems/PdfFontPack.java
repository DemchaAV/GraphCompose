package com.demcha.loyaut_core.system.implemented_systems.pdf_systems;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public class PdfFontPack {
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
}
