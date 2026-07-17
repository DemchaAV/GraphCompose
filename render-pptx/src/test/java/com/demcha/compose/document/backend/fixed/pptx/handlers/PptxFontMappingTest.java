package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.font.FontName;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

class PptxFontMappingTest {

    /**
     * Anti-tautology pin: production and the geometry-test mirror share one
     * ascent resolver, so this hand-computed constant is what actually proves
     * the seat. Arial's hhea ascent is 1854/2048 em — at 22pt the viewer
     * ascent must be 22 × 1854 / 2048 = 19.916015625 pt exactly.
     */
    @Test
    void helveticaViewerAscentRatioIsArialsHheaAscent() {
        assertThat(PptxFontMapping.viewerAscentRatio(FontName.HELVETICA, 0.5) * 22.0)
                .isEqualTo(19.916015625);
        assertThat(PptxFontMapping.viewerAscentRatio(FontName.of("SomeCustom"), 0.5))
                .as("unknown families fall back to the supplied ratio")
                .isEqualTo(0.5);
    }

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
