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
        assertThat(BidiVisualOrder.visualize("(a > b)")).isEqualTo("(a > b)");
    }

    @Test
    void aSingleLevelRunIsReversedAndMirroredWhole() {
        // For homogeneous text the transform must agree with the treatment plain spans
        // always had: reverse by cluster, mirror the pairs.
        assertThat(BidiVisualOrder.visualize("(" + HEBREW + ")"))
                .isEqualTo("(" + HEBREW_REVERSED + ")");
    }

    @Test
    void digitsInsideARightToLeftRunStayForward() {
        // Digits resolve to the left-to-right level even in Hebrew text: the year in
        // "ב-2026" reads forwards. Reversed whole, it would not.
        assertThat(BidiVisualOrder.visualize(HEBREW + " 123"))
                .isEqualTo("123 " + HEBREW_REVERSED);
    }

    @Test
    void mixedAndHomogeneousRunsAreToldApart() {
        assertThat(BidiVisualOrder.mixesDirections("(a > b)")).isTrue();
        assertThat(BidiVisualOrder.mixesDirections(HEBREW + " 123")).isTrue();
        assertThat(BidiVisualOrder.mixesDirections("(" + HEBREW + ")")).isFalse();
        assertThat(BidiVisualOrder.mixesDirections(HEBREW)).isFalse();
        assertThat(BidiVisualOrder.mixesDirections("plain latin")).isFalse();
        assertThat(BidiVisualOrder.mixesDirections("")).isFalse();
    }

    @Test
    void textWithoutARightToLeftLevelPassesThrough() {
        // The flag the caller holds comes from paragraph context; if the run's own
        // analysis finds nothing right-to-left, drawing it as written is already
        // correct and the transform must not invent a reversal.
        assertThat(BidiVisualOrder.visualize("plain latin")).isEqualTo("plain latin");
        assertThat(BidiVisualOrder.visualize("")).isEmpty();
        assertThat(BidiVisualOrder.visualize(null)).isEmpty();
    }
}
