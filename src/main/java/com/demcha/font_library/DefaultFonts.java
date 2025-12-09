package com.demcha.font_library;


import com.demcha.loyaut_core.system.implemented_systems.pdf_systems.PdfFont;
import lombok.Data;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.util.List;

@Data
public class DefaultFonts {

    public static FontLibrary library() {
        FontLibrary fontLibrary = new FontLibrary();

        List<FontSet> fonts = List.of(new FontSet(FontName.HELVETICA, new PdfFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE),
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE),
                new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                new PDType1Font(Standard14Fonts.FontName.HELVETICA))));
        fonts.forEach(fontLibrary::addFont);
        return fontLibrary;
    }


}
