package com.demcha.compose.engine.text.bidi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the paired-punctuation swap for right-to-left runs.
 *
 * <p>The failure it guards is quiet and looks almost right: without the swap every
 * parenthesis in Hebrew text faces away from what it encloses — the text is all
 * there, ordered correctly, and subtly wrong in a way a non-reader will not spot.</p>
 */
class BidiMirroringTest {

    @Test
    void pairedPunctuationSwaps() {
        assertThat(BidiMirroring.mirror("(שלום) [עולם] {x} <y> «z» ‹w›"))
                .isEqualTo(")שלום( ]עולם[ }x{ >y< »z« ›w‹");
    }

    @Test
    void mirroringIsItsOwnInverse() {
        String text = "(a[b{c«d";

        assertThat(BidiMirroring.mirror(BidiMirroring.mirror(text))).isEqualTo(text);
    }

    @Test
    void textWithoutPairsIsReturnedAsTheSameInstance() {
        String plain = "שלום עולם 123";

        assertThat(BidiMirroring.mirror(plain)).isSameAs(plain);
        assertThat(BidiMirroring.mirror("")).isEmpty();
        assertThat(BidiMirroring.mirror(null)).isEmpty();
    }
}
