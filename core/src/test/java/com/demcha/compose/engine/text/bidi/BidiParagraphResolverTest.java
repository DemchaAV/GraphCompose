package com.demcha.compose.engine.text.bidi;

import com.demcha.compose.engine.text.bidi.BidiParagraphResolver.BaseDirection;
import com.demcha.compose.engine.text.bidi.BidiParagraphResolver.DirectionalRun;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the bidirectional resolution the layout pipeline is built on.
 *
 * <p>The assertions are written against text, not against levels alone, because the
 * levels are only interesting for what they make the page do: which stretch reverses,
 * which keeps running forwards, and whether a line that has always been left to right
 * still takes the untouched path.</p>
 */
class BidiParagraphResolverTest {

    private static final String HEBREW = "שלום";           // shalom
    private static final String ARABIC = "مرحبا";     // marhaba
    private static final String RLM = "‏";
    private static final String LRM = "‎";

    @Test
    void aLineWithoutRightToLeftTextResolvesToItself() {
        List<DirectionalRun> runs =
                BidiParagraphResolver.resolve("Plain ASCII, 123.", BaseDirection.LEFT_TO_RIGHT);

        assertThat(runs).singleElement()
                .describedAs("the overwhelmingly common line must not be split or reordered")
                .isEqualTo(new DirectionalRun("Plain ASCII, 123.", 0));
    }

    @Test
    void accentedLatinIsStillNotBidirectional() {
        assertThat(BidiParagraphResolver.resolve("café naïve — dash",
                BaseDirection.LEFT_TO_RIGHT))
                .describedAs("non-ASCII alone must not drag a document onto the slow path")
                .hasSize(1);
    }

    @Test
    void aMixedLineSplitsIntoRunsAtEveryDirectionChange() {
        List<DirectionalRun> runs =
                BidiParagraphResolver.resolve(HEBREW + " Hello " + ARABIC, BaseDirection.LEFT_TO_RIGHT);

        assertThat(runs).hasSize(3);
        assertThat(runs.get(0).text()).isEqualTo(HEBREW);
        assertThat(runs.get(0).isRightToLeft()).isTrue();
        assertThat(runs.get(1).text()).isEqualTo(" Hello ");
        assertThat(runs.get(1).isRightToLeft()).isFalse();
        assertThat(runs.get(2).text()).isEqualTo(ARABIC);
        assertThat(runs.get(2).isRightToLeft()).isTrue();
    }

    @Test
    void concatenatingTheRunsReturnsTheOriginalLine() {
        String line = HEBREW + " Hello 123 " + ARABIC + "!";

        String rejoined = BidiParagraphResolver.resolve(line, BaseDirection.RIGHT_TO_LEFT).stream()
                .map(DirectionalRun::text)
                .reduce("", String::concat);

        assertThat(rejoined)
                .describedAs("splitting must partition the line — a lost or duplicated character "
                        + "would silently change the text before it is ever measured")
                .isEqualTo(line);
    }

    @Test
    void digitsInsideRightToLeftTextRunForwardsInTheirOwnRun() {
        List<DirectionalRun> runs =
                BidiParagraphResolver.resolve(ARABIC + " 2026 " + ARABIC, BaseDirection.RIGHT_TO_LEFT);

        assertThat(runs).hasSize(3);
        assertThat(runs.get(1).text().trim())
                .describedAs("a year inside an Arabic sentence reads left to right")
                .isEqualTo("2026");
        assertThat(runs.get(1).isRightToLeft()).isFalse();
        assertThat(runs.get(0).isRightToLeft()).isTrue();
        assertThat(runs.get(2).isRightToLeft()).isTrue();
    }

    @Test
    void anExplicitDirectionMarkStillSteersResolution() {
        String withoutMark = "abc " + HEBREW + " def";
        String withMark = "abc " + RLM + HEBREW + " def";

        List<DirectionalRun> plain =
                BidiParagraphResolver.resolve(withoutMark, BaseDirection.LEFT_TO_RIGHT);
        List<DirectionalRun> marked =
                BidiParagraphResolver.resolve(withMark, BaseDirection.LEFT_TO_RIGHT);

        assertThat(plain.get(1).text()).isEqualTo(HEBREW);
        assertThat(marked.get(1).text())
                .describedAs("the mark is category Cf and the sanitizer deletes it, so resolving "
                        + "after sanitizing would silently ignore what the author wrote")
                .isEqualTo(RLM + HEBREW);
        assertThat(marked.get(1).isRightToLeft()).isTrue();
    }

    @Test
    void aLeftToRightMarkAloneDoesNotMakeALineBidirectional() {
        assertThat(BidiParagraphResolver.resolve("plain " + LRM + "text", BaseDirection.LEFT_TO_RIGHT))
                .describedAs("a mark with no right-to-left text to steer has nothing to do")
                .hasSize(1);
    }

    @Test
    void firstStrongCharacterDecidesTheBaseDirection() {
        assertThat(BidiParagraphResolver.baseLevel("Hello " + HEBREW, BaseDirection.FIRST_STRONG_CHARACTER))
                .isEqualTo(0);
        assertThat(BidiParagraphResolver.baseLevel(HEBREW + " Hello", BaseDirection.FIRST_STRONG_CHARACTER))
                .isEqualTo(1);
        assertThat(BidiParagraphResolver.baseLevel("123 " + HEBREW, BaseDirection.FIRST_STRONG_CHARACTER))
                .describedAs("digits are not strong, so the Hebrew decides")
                .isEqualTo(1);
        assertThat(BidiParagraphResolver.baseLevel("(" + HEBREW + ")", BaseDirection.FIRST_STRONG_CHARACTER))
                .describedAs("a bracket is neutral, so the Hebrew decides")
                .isEqualTo(1);
    }

    @Test
    void anEmptyOrNeutralLineFallsBackToLeftToRight() {
        assertThat(BidiParagraphResolver.resolve("", BaseDirection.FIRST_STRONG_CHARACTER)).isEmpty();
        assertThat(BidiParagraphResolver.resolve(null, BaseDirection.RIGHT_TO_LEFT)).isEmpty();
        assertThat(BidiParagraphResolver.baseLevel("", BaseDirection.FIRST_STRONG_CHARACTER)).isEqualTo(0);
        assertThat(BidiParagraphResolver.baseLevel("   ", BaseDirection.FIRST_STRONG_CHARACTER)).isEqualTo(0);
    }

    @Test
    void anExplicitRightToLeftParagraphIsRightToLeftEvenWithoutRightToLeftText() {
        assertThat(BidiParagraphResolver.baseLevel("Latin only", BaseDirection.RIGHT_TO_LEFT))
                .describedAs("the author asked for it; content does not override the request")
                .isEqualTo(1);
    }

    @Test
    void visualOrderIsTheIdentityWhenEverythingRunsLeftToRight() {
        assertThat(BidiParagraphResolver.visualOrder(new int[]{0, 0, 0, 0}))
                .describedAs("an empty order is the signal to keep drawing exactly as before")
                .isEmpty();
        assertThat(BidiParagraphResolver.visualOrder(new int[]{1})).isEmpty();
        assertThat(BidiParagraphResolver.visualOrder(new int[0])).isEmpty();
        assertThat(BidiParagraphResolver.visualOrder(null)).isEmpty();
    }

    @Test
    void visualOrderReversesRightToLeftItemsAndKeepsEmbeddedOnesForwards() {
        // "שלום Hello 123 مرحبا" tokenised by word in a right-to-left paragraph.
        int[] levels = {1, 2, 2, 1};

        assertThat(BidiParagraphResolver.visualOrder(levels))
                .describedAs("the last word is drawn leftmost, while the embedded Latin and the "
                        + "number keep their own left-to-right order between them")
                .containsExactly(3, 1, 2, 0);
    }

    @Test
    void visualOrderIsAlwaysAPermutationOfTheLogicalIndices() {
        int[] order = BidiParagraphResolver.visualOrder(new int[]{1, 2, 1, 2, 1});

        assertThat(order).hasSize(5).containsExactlyInAnyOrder(0, 1, 2, 3, 4);
    }
}
