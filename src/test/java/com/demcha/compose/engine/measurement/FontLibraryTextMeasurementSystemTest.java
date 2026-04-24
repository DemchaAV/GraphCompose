package com.demcha.compose.engine.measurement;

import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.DefaultFonts;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FontLibraryTextMeasurementSystemTest {

    @Test
    void shouldNotKeepUserTextInStaticWidthCache() {
        List<String> unsafeStaticCaches = Arrays.stream(FontLibraryTextMeasurementSystem.class.getDeclaredFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .filter(field -> Map.class.isAssignableFrom(field.getType()))
                .map(field -> field.getName().toUpperCase())
                .filter(name -> name.contains("TEXT_WIDTH"))
                .toList();

        assertThat(unsafeStaticCaches)
                .describedAs("User text width measurements must stay request/session-local, not static.")
                .isEmpty();
    }

    @Test
    void clearCachesShouldDiscardSessionTextWidthCache() {
        FontLibraryTextMeasurementSystem measurement = new FontLibraryTextMeasurementSystem(
                DefaultFonts.standardLibrary(),
                PdfFont.class);

        assertThat(measurement.sessionTextWidthCacheSize()).isZero();

        double width = measurement.textWidth(TextStyle.DEFAULT_STYLE, "unique user cv text 4db771");

        assertThat(width).isGreaterThan(0.0);
        assertThat(measurement.sessionTextWidthCacheSize()).isEqualTo(1);

        measurement.clearCaches();

        assertThat(measurement.sessionTextWidthCacheSize()).isZero();
    }
}
