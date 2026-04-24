package com.demcha.compose.engine.render.pdf;

import com.demcha.compose.font.FontName;

public interface PdfFontGetter {
    PdfFont getPdfFont(FontName fontName);
}
