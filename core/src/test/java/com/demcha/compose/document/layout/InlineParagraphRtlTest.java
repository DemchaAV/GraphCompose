package com.demcha.compose.document.layout;

import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.InlineShapeRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.engine.components.content.text.TextIndentStrategy;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import com.demcha.compose.engine.text.bidi.BidiParagraphResolver.BaseDirection;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers direction on the inline-run path — the one templates author through.
 *
 * <p>Inline runs already arrive as one span per word, so unlike a plain paragraph
 * nothing has to be split here; each span only has to be told which way it goes, and
 * the line has to know the order they are drawn in. Getting the first part without the
 * second reverses each word in place while leaving the words in the wrong order, which
 * looks almost right and is not.</p>
 */
class InlineParagraphRtlTest {

    private static final String HEBREW = "שלום";
    private static final String HEBREW_WORLD = "עולם";

    private static final TextStyle STYLE = TextStyle.builder().size(10).build();
    private static final TextMeasurementSystem MEASUREMENT = new FixedWidthMeasurement(1.0);
    private static final TextMeasurementSystem.LineMetrics METRICS = MEASUREMENT.lineMetrics(STYLE);

    @Test
    void aLatinInlineParagraphIsUntouched() {
        ParagraphLine line = only(wrap(List.of(run("Hello world")), BaseDirection.LEFT_TO_RIGHT));

        assertThat(line.visualOrder())
                .describedAs("the path every rich-text document already takes")
                .isEmpty();
        assertThat(textSpans(line)).allSatisfy(span ->
                assertThat(span.rightToLeft()).isFalse());
    }

    @Test
    void hebrewWordsAreMarkedAndReordered() {
        ParagraphLine line = only(wrap(List.of(run(HEBREW + " " + HEBREW_WORLD)),
                BaseDirection.RIGHT_TO_LEFT));

        assertThat(textSpans(line))
                .filteredOn(span -> !span.text().isBlank())
                .allSatisfy(span -> assertThat(span.rightToLeft())
                        .describedAs("each word has to know its own direction, or it draws "
                                + "letter by letter in reading order")
                        .isTrue());

        List<String> drawn = line.spansInVisualOrder().stream()
                .map(span -> ((ParagraphTextSpan) span).text())
                .filter(text -> !text.isBlank())
                .toList();
        assertThat(drawn)
                .describedAs("and the words themselves have to swap, or only the letters "
                        + "come out right")
                .containsExactly(HEBREW_WORLD, HEBREW);
    }

    @Test
    void anEmbeddedLatinRunKeepsItsOwnDirection() {
        ParagraphLine line = only(wrap(
                List.of(run(HEBREW + " "), run("GraphCompose"), run(" " + HEBREW_WORLD)),
                BaseDirection.RIGHT_TO_LEFT));

        ParagraphTextSpan latin = textSpans(line).stream()
                .filter(span -> span.text().contains("GraphCompose"))
                .findFirst()
                .orElseThrow();
        assertThat(latin.rightToLeft())
                .describedAs("a Latin word inside Hebrew still reads forwards")
                .isFalse();
    }

    @Test
    void anInlineGraphicSitsInTheOrderWithoutBeingText() {
        List<InlineRun> runs = List.of(
                run(HEBREW + " "),
                InlineShapeRun.checkbox(6.0, false, DocumentColor.rgb(0, 0, 0), DocumentColor.rgb(0, 0, 0)),
                run(" " + HEBREW_WORLD));

        ParagraphLine line = only(wrap(runs, BaseDirection.RIGHT_TO_LEFT));

        assertThat(line.spansInVisualOrder())
                .describedAs("the graphic is a neutral object in the line, so it takes part in "
                        + "the ordering rather than breaking it")
                .hasSameSizeAs(line.spans());
        assertThat(line.visualOrder()).isNotEmpty();
    }

    private static InlineTextRun run(String text) {
        return new InlineTextRun(text, (com.demcha.compose.document.style.DocumentTextStyle) null,
                (com.demcha.compose.document.node.DocumentLinkTarget) null);
    }

    private static List<ParagraphLine> wrap(List<InlineRun> runs, BaseDirection direction) {
        return ParagraphWrapping.wrapInlineParagraph(runs, STYLE, METRICS, 1000.0, "",
                TextIndentStrategy.NONE, MEASUREMENT, direction);
    }

    private static ParagraphLine only(List<ParagraphLine> lines) {
        assertThat(lines).hasSize(1);
        return lines.get(0);
    }

    private static List<ParagraphTextSpan> textSpans(ParagraphLine line) {
        return line.spans().stream()
                .filter(ParagraphTextSpan.class::isInstance)
                .map(ParagraphTextSpan.class::cast)
                .toList();
    }

    @Test
    void markdownEmphasisCarriesDirectionToo() {
        // Markdown splits a line into styled bodies, which is a different splitter
        // producing the same shape of spans — so it needs the same pass, and a path
        // that quietly kept logical order would be invisible next to a working one.
        List<ParagraphLine> lines = ParagraphWrapping.wrapMarkdownParagraph(
                List.of("**" + HEBREW + "** " + HEBREW_WORLD),
                STYLE, METRICS, 1000.0, "", TextIndentStrategy.NONE, MEASUREMENT,
                BaseDirection.RIGHT_TO_LEFT);

        ParagraphLine line = only(lines);
        assertThat(textSpans(line))
                .filteredOn(span -> !span.text().isBlank())
                .allSatisfy(span -> assertThat(span.rightToLeft()).isTrue());
        assertThat(line.visualOrder()).isNotEmpty();
    }

    @Test
    void markdownWithoutRightToLeftTextIsUntouched() {
        ParagraphLine line = only(ParagraphWrapping.wrapMarkdownParagraph(
                List.of("**Bold** and plain"),
                STYLE, METRICS, 1000.0, "", TextIndentStrategy.NONE, MEASUREMENT,
                BaseDirection.LEFT_TO_RIGHT));

        assertThat(line.visualOrder()).isEmpty();
        assertThat(textSpans(line)).allSatisfy(span ->
                assertThat(span.rightToLeft()).isFalse());
    }
}
