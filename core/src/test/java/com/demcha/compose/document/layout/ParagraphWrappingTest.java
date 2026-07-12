package com.demcha.compose.document.layout;

import com.demcha.compose.engine.components.content.text.TextIndentStrategy;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Direct unit coverage for the greedy plain-text wrap and the markdown probe in
 * {@link ParagraphWrapping}, driven by a deterministic fixed-width measurement so
 * the assertions don't depend on real font metrics. The inline-run and markdown
 * wrap paths, and the full rendered output, are guarded by the qa parity suites.
 */
class ParagraphWrappingTest {

    private static final TextStyle STYLE = TextStyle.DEFAULT_STYLE;
    /** Each character is exactly 1.0 wide, so a token's width equals its length. */
    private static final TextMeasurementSystem UNIT = new FixedWidthMeasurement(1.0);

    private static List<String> wrap(String line, double maxWidth) {
        return ParagraphWrapping.wrapParagraph(List.of(line), STYLE, maxWidth, "", TextIndentStrategy.NONE, UNIT);
    }

    @Test
    void greedilyPacksWordsUntilTheNextOneOverflows() {
        // "aa bb"=5 fits in 6; adding " cc" (→8) overflows, so it wraps.
        assertThat(wrap("aa bb cc dd", 6)).containsExactly("aa bb", "cc dd");
    }

    @Test
    void keepsAWholeLineWhenItFits() {
        assertThat(wrap("aa bb", 100)).containsExactly("aa bb");
    }

    @Test
    void placesEachWordOnItsOwnLineWhenNoTwoFitTogether() {
        assertThat(wrap("aaa bbb ccc", 3)).containsExactly("aaa", "bbb", "ccc");
    }

    @Test
    void containsMarkdownSyntaxDetectsEmphasisAndCodeMarkers() {
        assertThat(ParagraphWrapping.containsMarkdownSyntax("**bold**")).isTrue();
        assertThat(ParagraphWrapping.containsMarkdownSyntax("snake_case")).isTrue();
        assertThat(ParagraphWrapping.containsMarkdownSyntax("`code`")).isTrue();
    }

    @Test
    void containsMarkdownSyntaxIsFalseForPlainNullAndEmpty() {
        assertThat(ParagraphWrapping.containsMarkdownSyntax("plain text")).isFalse();
        assertThat(ParagraphWrapping.containsMarkdownSyntax("")).isFalse();
        assertThat(ParagraphWrapping.containsMarkdownSyntax(null)).isFalse();
    }

    /** Each character is exactly {@code unit} wide, so a token's width equals its length. */
    private record FixedWidthMeasurement(double unit) implements TextMeasurementSystem {
        @Override
        public ContentSize measure(TextStyle style, String text) {
            return new ContentSize(text.length() * unit, unit);
        }

        @Override
        public LineMetrics lineMetrics(TextStyle style) {
            return new LineMetrics(unit, 0.0, 0.0);
        }
    }
}
