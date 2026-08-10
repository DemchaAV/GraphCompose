package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextDirection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins how direction and alignment interact on the authoring surface.
 *
 * <p>They are separate choices that meet in one place: a right-to-left paragraph starts
 * at the right edge, so it aligns there by default. The rule has to distinguish "the
 * caller did not choose an alignment" from "the caller chose LEFT", which a field
 * defaulted to {@code LEFT} cannot express on its own — get that wrong and either the
 * default never applies, or it silently overrides an explicit request.</p>
 */
class TextDirectionApiTest {

    @Test
    void aParagraphIsLeftToRightUnlessItSaysOtherwise() {
        ParagraphNode node = new ParagraphBuilder().text("Plain").build();

        assertThat(node.direction()).isEqualTo(TextDirection.LTR);
        assertThat(node.align()).isEqualTo(TextAlign.LEFT);
    }

    @Test
    void aRightToLeftParagraphAlignsRightWithoutBeingAsked() {
        ParagraphNode node = new ParagraphBuilder()
                .text("שלום")
                .direction(TextDirection.RTL)
                .build();

        assertThat(node.direction()).isEqualTo(TextDirection.RTL);
        assertThat(node.align())
                .describedAs("right is the edge a right-to-left line starts from")
                .isEqualTo(TextAlign.RIGHT);
    }

    @Test
    void anExplicitAlignmentSurvivesTheDirectionDefault() {
        ParagraphNode node = new ParagraphBuilder()
                .text("שלום")
                .direction(TextDirection.RTL)
                .align(TextAlign.LEFT)
                .build();

        assertThat(node.align())
                .describedAs("asking for LEFT must not be mistaken for not having asked")
                .isEqualTo(TextAlign.LEFT);
    }

    @Test
    void theOrderOfTheTwoCallsDoesNotMatter() {
        ParagraphNode alignFirst = new ParagraphBuilder()
                .align(TextAlign.CENTER)
                .direction(TextDirection.RTL)
                .build();
        ParagraphNode directionFirst = new ParagraphBuilder()
                .direction(TextDirection.RTL)
                .align(TextAlign.CENTER)
                .build();

        assertThat(alignFirst.align()).isEqualTo(TextAlign.CENTER);
        assertThat(directionFirst.align()).isEqualTo(TextAlign.CENTER);
    }

    @Test
    void autoAlignsRightWhenTheTextItselfRunsRightToLeft() {
        ParagraphNode node = new ParagraphBuilder()
                .text("שלום עולם")
                .direction(TextDirection.AUTO)
                .build();

        assertThat(node.align()).isEqualTo(TextAlign.RIGHT);
    }

    @Test
    void autoLeavesLeftToRightTextAlone() {
        assertThat(new ParagraphBuilder().text("Hello").direction(TextDirection.AUTO).build().align())
                .isEqualTo(TextAlign.LEFT);
        assertThat(new ParagraphBuilder().text("").direction(TextDirection.AUTO).build().align())
                .describedAs("nothing strong to read means the left-to-right fallback")
                .isEqualTo(TextAlign.LEFT);
    }

    @Test
    void autoSkipsCharactersThatCarryNoDirectionOfTheirOwn() {
        assertThat(new ParagraphBuilder().text("2026 — שלום").direction(TextDirection.AUTO)
                .build().align())
                .describedAs("digits and punctuation are not strong, so the Hebrew decides — "
                        + "the same rule the algorithm applies to the text itself")
                .isEqualTo(TextAlign.RIGHT);
        assertThat(new ParagraphBuilder().text("2026 — Hello").direction(TextDirection.AUTO)
                .build().align())
                .isEqualTo(TextAlign.LEFT);
    }

    @Test
    void autoReadsTheTextOfARichParagraphToo() {
        ParagraphNode node = new ParagraphBuilder()
                .rich(RichText.text("שלום"))
                .direction(TextDirection.AUTO)
                .build();

        assertThat(node.align())
                .describedAs("a rich paragraph holds its text in inline runs, and AUTO has to "
                        + "read those as well or it silently never fires for them")
                .isEqualTo(TextAlign.RIGHT);
    }

    @Test
    void anExplicitAlignmentSurvivesAutoToo() {
        ParagraphNode node = new ParagraphBuilder()
                .text("שלום")
                .direction(TextDirection.AUTO)
                .align(TextAlign.CENTER)
                .build();

        assertThat(node.align()).isEqualTo(TextAlign.CENTER);
    }

    @Test
    void aNullDirectionRestoresTheDefault() {
        ParagraphNode node = new ParagraphBuilder()
                .direction(TextDirection.RTL)
                .direction(null)
                .build();

        assertThat(node.direction()).isEqualTo(TextDirection.LTR);
        assertThat(node.align()).isEqualTo(TextAlign.LEFT);
    }

    @Test
    void theConstructorWithoutADirectionStillCompilesAndDefaults() {
        ParagraphNode node = new ParagraphNode(
                "p", "text", null, null, TextAlign.CENTER, 0.0, null, null,
                null, null, null, null, null, null, null);

        assertThat(node.direction())
                .describedAs("the previous canonical signature stays available so existing "
                        + "callers keep compiling and linking")
                .isEqualTo(TextDirection.LTR);
        assertThat(node.align()).isEqualTo(TextAlign.CENTER);
    }
}
