package com.demcha.compose.engine.integration;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.font.DefaultFonts;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.testsupport.EngineComposerHarness;
import com.demcha.compose.engine.render.pdf.PdfFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SingleLineTextIntegrationTest {

    @Test
    void shouldRenderAutosizedSingleLineTextAtPaddingAdjustedBaseline() throws Exception {
        TextStyle style = TextStyle.DEFAULT_STYLE;
        Padding padding = Padding.of(5);
        Margin margin = Margin.of(5);
        Entity textEntity;

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf()
                .pageSize(PDRectangle.A4)
                .create()) {

            textEntity = composer.componentBuilder()
                    .text()
                    .textWithAutoSize("In-memory PDF")
                    .padding(padding)
                    .margin(margin)
                    .textStyle(style)
                    .anchor(Anchor.topLeft())
                    .build();

            PDDocument document = composer.toPDDocument();
            TextPosition firstGlyph = firstVisibleGlyph(document);
            Placement placement = textEntity.getComponent(Placement.class).orElseThrow();
            PdfFont.VerticalMetrics metrics;
            try (PDDocument fontDocument = new PDDocument()) {
                PdfFont pdfFont = (PdfFont) DefaultFonts.library(fontDocument)
                        .getFont(style.fontName(), PdfFont.class)
                        .orElseThrow();
                metrics = pdfFont.verticalMetrics(style);
            }

            assertThat(firstGlyph).isNotNull();
            assertThat(firstGlyph.getUnicode()).startsWith("I");
            assertThat((double) firstGlyph.getTextMatrix().getTranslateX())
                    .isCloseTo(placement.x() + padding.left(), within(0.5));
            assertThat((double) firstGlyph.getTextMatrix().getTranslateY())
                    .isCloseTo(placement.y() + padding.bottom() + metrics.baselineOffsetFromBottom(), within(0.5));
        }
    }

    private TextPosition firstVisibleGlyph(PDDocument document) throws IOException {
        final TextPosition[] first = new TextPosition[1];

        PDFTextStripper stripper = new PDFTextStripper() {
            @Override
            protected void processTextPosition(TextPosition text) {
                if (first[0] == null && text.getUnicode() != null && !text.getUnicode().isBlank()) {
                    first[0] = text;
                }
                super.processTextPosition(text);
            }
        };

        stripper.getText(document);

        assertThat(first[0]).as("first rendered glyph").isNotNull();
        return first[0];
    }
}
