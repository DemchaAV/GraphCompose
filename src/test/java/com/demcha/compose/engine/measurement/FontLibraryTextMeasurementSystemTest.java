package com.demcha.compose.engine.measurement;

import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.font.FontBase;
import com.demcha.compose.engine.font.FontLineMetrics;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.DefaultFonts;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.font.FontName;
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

    @Test
    void resolvesBackendLineMetricsPolymorphicallyWithoutPdfSpecialCase() {
        // A backend font that is NOT a PdfFont but supplies first-class metrics by
        // overriding Font#lineMetrics. The shared measurement system must honour
        // them via the contract, with no instanceof PdfFont fast-path.
        FontLibrary library = DefaultFonts.standardLibrary();
        library.addFont(FontName.HELVETICA, FirstClassMetricsFont.class, new FirstClassMetricsFont());
        FontLibraryTextMeasurementSystem measurement =
                new FontLibraryTextMeasurementSystem(library, FirstClassMetricsFont.class);
        TextStyle style = helveticaStyle();

        TextMeasurementSystem.LineMetrics metrics = measurement.lineMetrics(style);

        assertThat(metrics.ascent()).isEqualTo(10.0);
        assertThat(metrics.descent())
                .describedAs("a non-PDF backend must get its real descent, not the degraded descent=0 fallback")
                .isEqualTo(3.0);
        assertThat(metrics.leading()).isEqualTo(1.0);
        assertThat(metrics.lineHeight()).isEqualTo(14.0);
    }

    @Test
    void defaultLineMetricsDeriveFromLineHeightWithZeroDescentAndLeading() {
        // A backend font that does NOT override Font#lineMetrics falls back to the
        // contract default: ascent = line height, descent = leading = 0.
        FontLibrary library = DefaultFonts.standardLibrary();
        library.addFont(FontName.HELVETICA, DefaultMetricsFont.class, new DefaultMetricsFont(20.0));
        FontLibraryTextMeasurementSystem measurement =
                new FontLibraryTextMeasurementSystem(library, DefaultMetricsFont.class);
        TextStyle style = helveticaStyle();

        TextMeasurementSystem.LineMetrics metrics = measurement.lineMetrics(style);

        assertThat(metrics.ascent()).isEqualTo(20.0);
        assertThat(metrics.descent()).isZero();
        assertThat(metrics.leading()).isZero();
        assertThat(metrics.lineHeight()).isEqualTo(20.0);
    }

    @Test
    void globalMetricsCacheIsNamespacedByBackendFontType() {
        // Two different backend font types that return the SAME measurementCacheKey
        // must not collide in the process-wide cache — the multi-backend invariant.
        FontLibrary library = DefaultFonts.standardLibrary();
        library.addFont(FontName.HELVETICA, BackendAFont.class, new BackendAFont());
        library.addFont(FontName.HELVETICA, BackendBFont.class, new BackendBFont());
        FontLibraryTextMeasurementSystem a = new FontLibraryTextMeasurementSystem(library, BackendAFont.class);
        FontLibraryTextMeasurementSystem b = new FontLibraryTextMeasurementSystem(library, BackendBFont.class);
        TextStyle style = helveticaStyle();

        // Resolve A first so it populates the shared static cache under the colliding key.
        assertThat(a.lineMetrics(style).ascent()).isEqualTo(10.0);
        assertThat(b.lineMetrics(style).ascent())
                .describedAs("backend B must get its own metrics, not backend A's value cached under the shared key")
                .isEqualTo(20.0);
    }

    private static TextStyle helveticaStyle() {
        return new TextStyle(FontName.HELVETICA, 12.0,
                TextStyle.DEFAULT_STYLE.decoration(), TextStyle.DEFAULT_STYLE.color());
    }

    /** Minimal non-PDF backend font that relies on the {@link com.demcha.compose.engine.font.Font} metric defaults. */
    private static class DefaultMetricsFont extends FontBase<Object> {
        private final double lineHeight;

        DefaultMetricsFont(double lineHeight) {
            super(new Object(), new Object(), new Object(), new Object());
            this.lineHeight = lineHeight;
        }

        @Override public double getTextWidth(TextStyle style, String text) { return text == null ? 0.0 : text.length(); }
        @Override public double getTextWidthNoSanitize(TextStyle style, String text) { return getTextWidth(style, text); }
        @Override public double getLineHeight(TextStyle style) { return lineHeight; }
        @Override public double getTextHeight(TextStyle style) { return lineHeight; }
        @Override public double getCapHeight(TextStyle style) { return 0.0; }
        @Override public double scale(double size) { return size; }
        @Override public TextStyle adjustFontSizeToFit(String text, TextStyle style, double availableWidth) { return style; }
        @Override public ContentSize getTightBounds(String text, TextStyle style) { return new ContentSize(0.0, 0.0); }
    }

    /** Non-PDF backend font that overrides the metric seam with first-class ascent/descent/leading. */
    private static final class FirstClassMetricsFont extends DefaultMetricsFont {
        FirstClassMetricsFont() {
            super(99.0); // deliberately distinct from the overridden metrics below
        }

        @Override
        public FontLineMetrics lineMetrics(TextStyle style) {
            return new FontLineMetrics(10.0, 3.0, 1.0);
        }

        @Override
        public String measurementCacheKey(TextStyle style) {
            return "first-class|" + style.size();
        }
    }

    /** Two backends below deliberately share a measurementCacheKey to exercise cross-backend cache isolation. */
    private static final class BackendAFont extends DefaultMetricsFont {
        BackendAFont() { super(0.0); }
        @Override public FontLineMetrics lineMetrics(TextStyle style) { return new FontLineMetrics(10.0, 0.0, 0.0); }
        @Override public String measurementCacheKey(TextStyle style) { return "shared-collision-key"; }
    }

    private static final class BackendBFont extends DefaultMetricsFont {
        BackendBFont() { super(0.0); }
        @Override public FontLineMetrics lineMetrics(TextStyle style) { return new FontLineMetrics(20.0, 0.0, 0.0); }
        @Override public String measurementCacheKey(TextStyle style) { return "shared-collision-key"; }
    }
}
