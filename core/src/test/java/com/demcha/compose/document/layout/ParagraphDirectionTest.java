package com.demcha.compose.document.layout;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.node.TextAlign;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the one place that answers which way a paragraph runs.
 *
 * <p>The point of the class is that there is only one such place, so most of these
 * assert agreement between the two callers rather than a value in isolation: the
 * alignment a paragraph is built with, and the direction its layout is compiled with,
 * have to come from the same reading of the same text.</p>
 */
class ParagraphDirectionTest {

    private static final String HEBREW = "שלום";

    /**
     * The isolate initiators and terminator, built from their code points rather than
     * written as literals: they are zero-width, so a literal is invisible in review — in
     * a file whose every interesting case turns on exactly these characters.
     */
    private static final String LRI = String.valueOf((char) 0x2066);
    private static final String RLI = String.valueOf((char) 0x2067);
    private static final String PDI = String.valueOf((char) 0x2069);

    @Test
    void anExplicitDirectionIsReturnedAsGiven() {
        assertThat(ParagraphDirection.resolve(HEBREW, TextDirection.LTR))
                .describedAs("a caller who said LTR gets it even over Hebrew")
                .isEqualTo(TextDirection.LTR);
        assertThat(ParagraphDirection.resolve("hello", TextDirection.RTL))
                .isEqualTo(TextDirection.RTL);
    }

    @Test
    void automaticReadsTheFirstStrongCharacter() {
        assertThat(ParagraphDirection.resolve(HEBREW, TextDirection.AUTO)).isEqualTo(TextDirection.RTL);
        assertThat(ParagraphDirection.resolve("hello", TextDirection.AUTO)).isEqualTo(TextDirection.LTR);
        assertThat(ParagraphDirection.resolve("2026 " + HEBREW, TextDirection.AUTO))
                .describedAs("digits are neutral, so the search runs past them")
                .isEqualTo(TextDirection.RTL);
        assertThat(ParagraphDirection.resolve("", TextDirection.AUTO)).isEqualTo(TextDirection.LTR);
    }

    @Test
    void automaticSkipsAnIsolatedRunTheWayTheAlgorithmSaysTo() {
        // UAX #9 P2: when looking for the first strong character, skip everything between
        // an isolate initiator and its matching PDI. This is the case a plain scan for the
        // first strongly-directional character gets wrong — it reads straight into the
        // isolate and answers with the direction the author isolated precisely to contain.
        assertThat(ParagraphDirection.resolve(LRI + HEBREW + PDI + " hello", TextDirection.AUTO))
                .describedAs("the Hebrew is isolated, so the paragraph is the Latin around it")
                .isEqualTo(TextDirection.LTR);
        assertThat(ParagraphDirection.resolve(RLI + "hello" + PDI + " " + HEBREW, TextDirection.AUTO))
                .describedAs("and the mirror image")
                .isEqualTo(TextDirection.RTL);
    }

    @Test
    void theAlignmentAParagraphIsBuiltWithMatchesTheDirectionItIsLaidOutWith() {
        // The two used to be answered separately, and parted company on exactly the input
        // above: the alignment said right-to-left, the layout said left-to-right, and the
        // paragraph sat at one edge while running from the other.
        for (String text : new String[]{
                HEBREW,
                "hello",
                "2026 " + HEBREW,
                LRI + HEBREW + PDI + " hello",
                RLI + "hello" + PDI + " " + HEBREW}) {

            ParagraphNode node = new ParagraphBuilder().text(text).direction(TextDirection.AUTO).build();
            TextDirection resolved = ParagraphDirection.resolve(node);

            assertThat(node.align() == TextAlign.RIGHT)
                    .describedAs("a paragraph resolving to %s must be built aligned to the edge "
                            + "it runs from; text was %s", resolved, describe(text))
                    .isEqualTo(resolved == TextDirection.RTL);
        }
    }

    @Test
    void anAlignmentTheCallerChoseSurvivesTheDefault() {
        ParagraphNode node = new ParagraphBuilder()
                .text(HEBREW)
                .direction(TextDirection.AUTO)
                .align(TextAlign.LEFT)
                .build();

        assertThat(node.align())
                .describedAs("the right-edge default is a default, not an override")
                .isEqualTo(TextAlign.LEFT);
    }

    @Test
    void aParagraphBuiltFromInlineRunsResolvesFromTheirText() {
        ParagraphNode node = new ParagraphBuilder()
                .rich(rich -> rich.plain(HEBREW))
                .direction(TextDirection.AUTO)
                .build();

        assertThat(ParagraphDirection.resolve(node)).isEqualTo(TextDirection.RTL);
        assertThat(node.align()).isEqualTo(TextAlign.RIGHT);
    }

    /** Renders the zero-width characters visibly, so a failure message can be read. */
    private static String describe(String text) {
        return text.replace(LRI, "<LRI>").replace(RLI, "<RLI>").replace(PDI, "<PDI>");
    }
}
