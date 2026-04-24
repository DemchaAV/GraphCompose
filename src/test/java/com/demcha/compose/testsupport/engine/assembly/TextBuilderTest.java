package com.demcha.compose.testsupport.engine.assembly;

import com.demcha.compose.engine.measurement.FontLibraryTextMeasurementSystem;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.components.renderable.TextComponent;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.render.pdf.PdfFont;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TextBuilderTest {

    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        entityManager = new EntityManager();
        entityManager.getSystems().addSystem(new FontLibraryTextMeasurementSystem(entityManager.getFonts(), PdfFont.class));
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

    @Test
    void shouldAutosizeWithoutLayoutSystemWhenMeasurementSystemIsRegistered() {
        Entity entity = new TextBuilder(entityManager)
                .textWithAutoSize("Layout-free measurement")
                .textStyle(TextStyle.DEFAULT_STYLE)
                .build();

        assertThat(entity.getComponent(ContentSize.class)).isPresent();
    }

    @Test
    void shouldExposeFullPdfLineMetricsThroughMeasurementSystem() {
        TextStyle style = TextStyle.DEFAULT_STYLE;
        var measurementSystem = entityManager.getSystems()
                .getSystem(com.demcha.compose.engine.measurement.TextMeasurementSystem.class)
                .orElseThrow();

        PdfFont font = (PdfFont) entityManager.getFonts().getFont(style.fontName(), PdfFont.class).orElseThrow();
        PdfFont.VerticalMetrics expected = font.verticalMetrics(style);
        var actual = measurementSystem.lineMetrics(style);

        assertThat(actual.ascent()).isCloseTo(expected.ascent(), within(0.001));
        assertThat(actual.descent()).isCloseTo(expected.descent(), within(0.001));
        assertThat(actual.leading()).isCloseTo(expected.leading(), within(0.001));
        assertThat(actual.lineHeight()).isCloseTo(expected.lineHeight(), within(0.001));
    }
}
