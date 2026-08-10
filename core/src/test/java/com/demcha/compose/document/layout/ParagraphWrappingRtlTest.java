package com.demcha.compose.document.layout;

import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.layout.payloads.ParagraphSpan;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import com.demcha.compose.engine.text.bidi.BidiParagraphResolver.BaseDirection;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers how a laid-out line carries direction.
 *
 * <p>The load-bearing detail is that a span is drawn with one show-text operation, so a
 * line whose direction changes partway cannot be reordered by moving spans unless each
 * directional run is a span of its own. A single span holding
 * {@code "שלום Hello"} would draw in logical order however the renderer walked it.</p>
 *
 * <p>Measured with a fixed-width system so the assertions are about structure and order,
 * not about a font.</p>
 */
class ParagraphWrappingRtlTest {

    private static final String HEBREW = "שלום";
    private static final String ARABIC = "مرحبا";
    private static final String RLM = "‏";

    private static final TextStyle STYLE = TextStyle.builder().size(10).build();
    private static final TextMeasurementSystem MEASUREMENT = new FixedWidthMeasurement(1.0);
    private static final TextMeasurementSystem.LineMetrics METRICS =
            MEASUREMENT.lineMetrics(STYLE);

    @Test
    void aLeftToRightLineIsStillOneSpanInSourceOrder() {
        ParagraphLine line = only(lines(List.of("Plain ASCII text"), BaseDirection.LEFT_TO_RIGHT));

        assertThat(line.spans())
                .describedAs("the shape every existing document has must not change")
                .hasSize(1);
        assertThat(line.visualOrder())
                .describedAs("an empty order tells the renderer to walk the spans as before")
                .isEmpty();
        assertThat(line.text()).isEqualTo("Plain ASCII text");
        assertThat(line.spansInVisualOrder()).isSameAs(line.spans());
    }

    @Test
    void aMixedLineBecomesOneSpanPerDirectionalRun() {
        ParagraphLine line = only(lines(List.of(HEBREW + " Hello " + ARABIC),
                BaseDirection.LEFT_TO_RIGHT));

        assertThat(line.spans())
                .describedAs("one span per run is what makes reordering possible at all")
                .hasSize(3);
        assertThat(textOf(line.spans()))
                .describedAs("the spans stay in logical order")
                .containsExactly(HEBREW, " Hello ", ARABIC);
    }

    @Test
    void theVisualOrderDrawsARightToLeftLineFromItsLastRun() {
        ParagraphLine line = only(lines(List.of(HEBREW + " Hello " + ARABIC),
                BaseDirection.RIGHT_TO_LEFT));

        // The spaces around "Hello" belong to the Arabic and Hebrew runs here: in a
        // right-to-left paragraph a neutral character between a right-to-left run and
        // an embedded left-to-right one resolves to the paragraph's own direction.
        assertThat(textOf(line.spansInVisualOrder()))
                .describedAs("in a right-to-left paragraph the Arabic sits leftmost and the "
                        + "Hebrew rightmost, while the embedded Latin keeps running forwards")
                .containsExactly(" " + ARABIC, "Hello", HEBREW + " ");
    }

    @Test
    void aRightToLeftOnlyLineIsMarkedEvenThoughItIsOneRun() {
        ParagraphLine line = only(lines(List.of(HEBREW), BaseDirection.LEFT_TO_RIGHT));

        assertThat(line.spans()).hasSize(1);
        assertThat(line.text())
                .describedAs("a single right-to-left run still goes through the directional "
                        + "path, so the renderer is told which way it runs")
                .isEqualTo(HEBREW);
    }

    @Test
    void theLineTextAndWidthStayConsistentWithItsSpans() {
        ParagraphLine line = only(lines(List.of(HEBREW + " Hello"), BaseDirection.RIGHT_TO_LEFT));

        double spanWidths = line.spans().stream().mapToDouble(ParagraphSpan::width).sum();
        assertThat(line.width()).isEqualTo(spanWidths);
        assertThat(line.text())
                .describedAs("the line text is the concatenation of its spans, in logical order")
                .isEqualTo(textOf(line.spans()).stream().reduce("", String::concat));
    }

    @Test
    void aDirectionMarkSteersTheLayoutAndThenLeavesTheText() {
        ParagraphLine line = only(lines(List.of("abc " + RLM + HEBREW), BaseDirection.LEFT_TO_RIGHT));

        assertThat(line.text())
                .describedAs("the mark is the author's instruction to the algorithm, not "
                        + "content — it must not reach a backend or a width")
                .doesNotContain(RLM)
                .isEqualTo("abc " + HEBREW);
        assertThat(line.spans()).hasSize(2);
    }

    @Test
    void everyVisualOrderIsAPermutationOfTheSpans() {
        for (BaseDirection direction : BaseDirection.values()) {
            ParagraphLine line = only(lines(List.of(HEBREW + " 12 Hello " + ARABIC), direction));

            if (!line.visualOrder().isEmpty()) {
                assertThat(line.visualOrder())
                        .describedAs("direction %s", direction)
                        .hasSize(line.spans().size())
                        .doesNotHaveDuplicates()
                        .allSatisfy(index -> assertThat(index).isBetween(0, line.spans().size() - 1));
            }
            assertThat(line.spansInVisualOrder()).hasSize(line.spans().size());
        }
    }

    private static List<ParagraphLine> lines(List<String> wrapped, BaseDirection direction) {
        return ParagraphWrapping.toParagraphLines(wrapped, STYLE, METRICS, MEASUREMENT, direction);
    }

    private static ParagraphLine only(List<ParagraphLine> lines) {
        assertThat(lines).hasSize(1);
        return lines.get(0);
    }

    private static List<String> textOf(List<ParagraphSpan> spans) {
        return spans.stream()
                .map(span -> ((ParagraphTextSpan) span).text())
                .toList();
    }
}
