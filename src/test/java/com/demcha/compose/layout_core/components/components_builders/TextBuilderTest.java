package com.demcha.compose.layout_core.components.components_builders;

import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.renderable.TextComponent;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.LayoutSystem;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfCanvas;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfFont;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TextBuilderTest {

    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        entityManager = new EntityManager();
        PDDocument doc = new PDDocument();
        PdfCanvas canvas = new PdfCanvas(PDRectangle.A4, 0.0f);
        PdfRenderingSystemECS renderingSystemECS = new PdfRenderingSystemECS(doc, canvas);
        entityManager.getSystems().addSystem(new LayoutSystem(canvas, renderingSystemECS));
        entityManager.getSystems().addSystem(renderingSystemECS);
    }

    @Test
    void shouldUseLineHeightForAutosizedSingleLineHeight() {
        TextStyle style = TextStyle.DEFAULT_STYLE;
        Padding padding = Padding.of(5);
        String text = "In-memory PDF";

        Entity entity = new TextBuilder(entityManager)
                .textWithAutoSize(text)
                .textStyle(style)
                .padding(padding)
                .build();

        ContentSize contentSize = entity.getComponent(ContentSize.class).orElseThrow();
        PdfFont font = (PdfFont) entityManager.getFonts().getFont(style.fontName(), PdfFont.class).orElseThrow();

        assertThat(contentSize.width())
                .isCloseTo(font.getTextWidth(style, text) + padding.horizontal(), within(0.001));
        assertThat(contentSize.height())
                .isCloseTo(font.getLineHeight(style) + padding.vertical(), within(0.001));
    }

    @Test
    void shouldMatchAutosizedHeightWithSingleLineMeasurementMetrics() throws Exception {
        TextStyle style = TextStyle.DEFAULT_STYLE;
        Padding padding = Padding.of(5);

        Entity entity = new TextBuilder(entityManager)
                .textWithAutoSize("In-memory PDF")
                .textStyle(style)
                .padding(padding)
                .build();

        ContentSize contentSize = entity.getComponent(ContentSize.class).orElseThrow();
        ContentSize measuredText = TextComponent.autoMeasureText(entity, entityManager);
        PdfFont font = (PdfFont) entityManager.getFonts().getFont(style.fontName(), PdfFont.class).orElseThrow();
        PdfFont.VerticalMetrics metrics = font.verticalMetrics(style);

        assertThat(measuredText.height()).isCloseTo(metrics.lineHeight(), within(0.001));
        assertThat(contentSize.height() - padding.vertical()).isCloseTo(measuredText.height(), within(0.001));
    }
}
