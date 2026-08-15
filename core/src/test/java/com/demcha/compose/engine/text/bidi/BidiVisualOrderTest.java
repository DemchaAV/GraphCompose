package com.demcha.compose.engine.text.bidi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the level-aware transform for the one span the wrapper cannot split.
 *
 * <p>The failure it guards inverted meaning, not just shape: a chip reading
 * {@code (a > b)} after a Hebrew word was reversed whole, and the page said
 * {@code (b < a)} — operands swapped and the comparison flipped, in the only copy of
 * the text the file has. The interior of that chip sits at the left-to-right level,
 * where UAX #9 reorders nothing and mirrors nothing; only the brackets enclosing it
 * belong to the right-to-left level that moves.</p>
 */
class BidiVisualOrderTest {

    private static final String HEBREW = "שלום";
    private static final String HEBREW_REVERSED = new StringBuilder(HEBREW).reverse().toString();

    @Test
    void aBracketWrappedLeftToRightInteriorKeepsItsMeaning() {
        // Brackets are right-to-left level and swap positions and forms; the interior
        // is left-to-right level and must come through exactly as written. The two
        // effects cancel on the brackets, so the visual string equals the logical one —
        // which is precisely what reversing the whole span destroys.
        assertThat(BidiVisualOrder.visualize("(a > b)", true)).isEqualTo("(a > b)");
    }

    @Test
    void aSingleLevelRunIsReversedAndMirroredWhole() {
        // For homogeneous text the transform must agree with the treatment plain spans
        // always had: reverse by cluster, mirror the pairs.
        assertThat(BidiVisualOrder.visualize("(" + HEBREW + ")", true))
                .isEqualTo("(" + HEBREW_REVERSED + ")");
    }

    @Test
    void digitsInsideARightToLeftRunStayForward() {
        // Digits resolve to the left-to-right level even in Hebrew text: the year in
        // "ב-2026" reads forwards. Reversed whole, it would not.
        assertThat(BidiVisualOrder.visualize(HEBREW + " 123", true))
                .isEqualTo("123 " + HEBREW_REVERSED);
    }

    @Test
    void levelKeyedMirroringSwapsOnlyWhatTheAlgorithmWould() {
        // For a viewer with its own bidi engine the text must stay logical — strong
        // right-to-left characters are reordered by what they are, not by what the
        // paragraph declares, so a pre-reordered string would come back re-reversed.
        // Only the mirroring is done for it, and only on the levels L4 touches.
        assertThat(BidiVisualOrder.mirrorRightToLeftLevels("(a > b)", true))
                .describedAs("a run that mixes levels goes over as typed: the viewer was "
                        + "measured to draw this correctly from the author's text, and to "
                        + "draw \")a > b(\" from a pre-swapped one")
                .isEqualTo("(a > b)");
        assertThat(BidiVisualOrder.mirrorRightToLeftLevels("(2026)", true))
                .describedAs("digits resolve to the left-to-right level, so this mixes too")
                .isEqualTo("(2026)");
        assertThat(BidiVisualOrder.mirrorRightToLeftLevels("(" + HEBREW + ")", true))
                .describedAs("uniformly right-to-left is the case that does need the swap — "
                        + "handed over as typed it was drawn with the brackets reversed")
                .isEqualTo(BidiMirroring.mirror("(" + HEBREW + ")"));
        assertThat(BidiVisualOrder.mirrorRightToLeftLevels(HEBREW + " 2026", true))
                .describedAs("nothing mirrors, nothing reorders — the letters stay "
                        + "logical for the viewer's own engine")
                .isEqualTo(HEBREW + " 2026");
        assertThat(BidiVisualOrder.mirrorRightToLeftLevels("plain latin", true))
                .isEqualTo("plain latin");
        assertThat(BidiVisualOrder.mirrorRightToLeftLevels("", true)).isEmpty();
        assertThat(BidiVisualOrder.mirrorRightToLeftLevels(null, true)).isEmpty();
    }

    @Test
    void aLeftToRightBaseKeepsItsHebrewOnTheSideThatBaseGivesIt() {
        // The base is the run's own, not the paragraph's: a chip opening on Latin is a
        // left-to-right run that may still hold Hebrew. Resolved against the wrong base
        // — or not resolved at all, which is what a direction-flag gate did — the word
        // is handed over in logical order and drawn backwards.
        assertThat(BidiVisualOrder.visualize("a " + HEBREW, false))
                .describedAs("Latin first, then the Hebrew reversed for drawing")
                .isEqualTo("a " + HEBREW_REVERSED);
        assertThat(BidiVisualOrder.visualize(HEBREW + " a", false))
                .describedAs("the run still reads left to right overall")
                .isEqualTo(HEBREW_REVERSED + " a");
    }

    @Test
    void textWithoutARightToLeftLevelPassesThrough() {
        // The flag the caller holds comes from paragraph context; if the run's own
        // analysis finds nothing right-to-left, drawing it as written is already
        // correct and the transform must not invent a reversal.
        assertThat(BidiVisualOrder.visualize("plain latin", true)).isEqualTo("plain latin");
        assertThat(BidiVisualOrder.visualize("", true)).isEmpty();
        assertThat(BidiVisualOrder.visualize(null, true)).isEmpty();
    }
}
