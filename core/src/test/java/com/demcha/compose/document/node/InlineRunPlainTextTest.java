package com.demcha.compose.document.node;

import com.demcha.compose.document.dsl.RichText;
import com.demcha.compose.document.style.DocumentColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The two reductions of a run sequence, and the fact that every surface needing
 * one shares them.
 *
 * <p>Which {@link InlineRun} variants carry text is knowledge that has to be
 * revisited whenever a variant is added, so it lives in one place. These tests
 * guard that: a second copy of the rule elsewhere would pass its own tests and
 * drift from this one silently.</p>
 */
class InlineRunPlainTextTest {

    private static final DocumentColor INK = DocumentColor.rgb(0x33, 0x66, 0x99);
    private static final DocumentColor PAPER = DocumentColor.rgb(0xEE, 0xEE, 0xEE);

    @Test
    void textAndChipsReadAsTheirTextAndPicturesReadAsNothing() {
        List<InlineRun> runs = RichText.empty()
                .bold("Status: ")
                .dot(6.0, INK)
                .chip("pending", INK, PAPER)
                .plain(" today")
                .runs();

        assertThat(InlineRun.plainText(runs))
                .as("the dot contributes nothing rather than a placeholder character")
                .isEqualTo("Status: pending today");
    }

    @Test
    void aNullOrEmptySequenceReadsAsNothing() {
        assertThat(InlineRun.plainText(null)).isEmpty();
        assertThat(InlineRun.plainText(List.of())).isEmpty();
        assertThat(InlineRun.textRuns(null)).isEmpty();
        assertThat(InlineRun.textRuns(List.of())).isEmpty();
    }

    @Test
    void textRunsKeepStylesDropPicturesAndFlattenAChipToOneLine() {
        List<InlineRun> runs = RichText.empty()
                .bold("Head")
                .dot(6.0, INK)
                .chip("two\nlines", INK, PAPER)
                .runs();

        List<InlineTextRun> textRuns = InlineRun.textRuns(runs);
        assertThat(textRuns).as("the dot is dropped, the chip degrades to text").hasSize(2);
        assertThat(textRuns.get(0).text()).isEqualTo("Head");
        assertThat(textRuns.get(0).textStyle())
                .as("a run keeps the style it was authored with")
                .isNotNull();
        assertThat(textRuns.get(1).text())
                .as("a chip stays one line wherever it is read, so its newline is a space")
                .isEqualTo("two lines");
    }

    @Test
    void aParagraphsTextIsThatSameReading() {
        List<InlineRun> runs = RichText.empty()
                .bold("Status: ")
                .chip("pending", INK, PAPER)
                .runs();
        ParagraphNode paragraph = new ParagraphNode("", "", runs, null, null, 0.0, "",
                null, null, null, null, null);

        assertThat(paragraph.text()).isEqualTo(InlineRun.plainText(runs));
        assertThat(paragraph.inlineTextRuns()).isEqualTo(InlineRun.textRuns(runs));
    }

    @Test
    void aRichListItemReadsAsThatSameReadingToo() {
        List<InlineRun> runs = RichText.empty()
                .bold("Status: ")
                .chip("pending", INK, PAPER)
                .runs();

        assertThat(ListItem.ofRuns(runs).label()).isEqualTo("Status: pending");
        assertThat(ListItem.ofRuns(runs).isRich()).isTrue();
        assertThat(ListItem.of("plain").isRich())
                .as("an item that is only a label is not rich, so it keeps the label path")
                .isFalse();
        assertThat(ListItem.ofRuns("Done", runs).label())
                .as("an explicit reading wins, for runs that draw something that is not text")
                .isEqualTo("Done");
    }
}
