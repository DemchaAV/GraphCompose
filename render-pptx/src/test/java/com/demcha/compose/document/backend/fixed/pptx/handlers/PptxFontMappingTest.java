package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.font.FontName;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

class PptxFontMappingTest {

    @Test
    void mapsStandardFamiliesToMetricCompatibleViewerFonts() {
        assertThat(PptxFontMapping.familyFor(FontName.HELVETICA_BOLD)).isEqualTo("Arial");
        assertThat(PptxFontMapping.familyFor(FontName.TIMES_ITALIC)).isEqualTo("Times New Roman");
        assertThat(PptxFontMapping.familyFor(FontName.COURIER)).isEqualTo("Courier New");
        assertThat(PptxFontMapping.familyFor(FontName.of("Acme-Sans"))).isEqualTo("Acme-Sans");
        assertThat(PptxFontMapping.familyFor(FontName.of("Acme-Sans-Bold"))).isEqualTo("Acme-Sans");
    }

    @Test
    void combinesDecorationWithFaceSuffixes() {
        TextStyle boldFace = new TextStyle(FontName.HELVETICA_BOLD, 12,
                TextDecoration.DEFAULT, Color.BLACK);
        TextStyle decorated = new TextStyle(FontName.HELVETICA, 12,
                TextDecoration.BOLD_ITALIC, Color.BLACK);

        assertThat(PptxFontMapping.isBold(boldFace)).isTrue();
        assertThat(PptxFontMapping.isItalic(boldFace)).isFalse();
        assertThat(PptxFontMapping.isBold(decorated)).isTrue();
        assertThat(PptxFontMapping.isItalic(decorated)).isTrue();
    }
}
