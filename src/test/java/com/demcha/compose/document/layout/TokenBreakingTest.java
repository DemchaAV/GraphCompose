package com.demcha.compose.document.layout;

import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Direct unit coverage for {@link TokenBreaking} — soft-seam segmentation, greedy
 * packing, and the character-level fallback — driven by a deterministic
 * fixed-width measurement so the assertions do not depend on real font metrics.
 */
class TokenBreakingTest {

    private static final TextStyle STYLE = TextStyle.DEFAULT_STYLE;
    /** Each character is exactly 1.0 wide, so a token's width equals its length. */
    private static final TextMeasurementSystem UNIT = new FixedWidthMeasurement(1.0);

    // ── softBreakSegments (pure, no measurement) ────────────────────────────

    @Test
    void softBreakSegmentsSplitsAfterEachSeamKeepingTheDelimiter() {
        assertThat(TokenBreaking.softBreakSegments("org.junit.jupiter:junit-jupiter:5.10.2"))
                .containsExactly("org.", "junit.", "jupiter:", "junit-", "jupiter:", "5.", "10.", "2");
    }

    @Test
    void softBreakSegmentsWithNoSeamReturnsTheWholeToken() {
        assertThat(TokenBreaking.softBreakSegments("abcdefgh")).containsExactly("abcdefgh");
    }

    @Test
    void softBreakSegmentsHandlesLeadingAndTrailingDelimiters() {
        assertThat(TokenBreaking.softBreakSegments("/a/b")).containsExactly("/", "a/", "b");
        assertThat(TokenBreaking.softBreakSegments("...")).containsExactly(".", ".", ".");
        // The concatenation of the segments always reconstructs the input.
        assertThat(String.join("", TokenBreaking.softBreakSegments("a.b:c/d-e"))).isEqualTo("a.b:c/d-e");
    }

    @Test
    void softBreakSegmentsOnNullOrEmptyIsEmpty() {
        assertThat(TokenBreaking.softBreakSegments(null)).isEmpty();
        assertThat(TokenBreaking.softBreakSegments("")).isEmpty();
    }

    // ── fitCharacters ───────────────────────────────────────────────────────

    @Test
    void fitCharactersReturnsTheLongestFittingPrefix() {
        assertThat(TokenBreaking.fitCharacters("abcdefgh", STYLE, 3.0, UNIT)).isEqualTo(3);
    }

    @Test
    void fitCharactersReturnsAtLeastOneCharBelowGlyphWidth() {
        assertThat(TokenBreaking.fitCharacters("abc", STYLE, 0.4, UNIT)).isEqualTo(1);
    }

    // ── splitLongToken (char-level) ─────────────────────────────────────────

    @Test
    void splitLongTokenBreaksIntoFittingChunks() {
        assertThat(TokenBreaking.splitLongToken("abcdefgh", STYLE, 3.0, UNIT))
                .containsExactly("abc", "def", "gh");
    }

    // ── breakLongToken (seam-aware, char-split fallback) ────────────────────

    @Test
    void breakLongTokenWithoutSeamsMatchesSplitLongToken() {
        List<String> viaBreak = TokenBreaking.breakLongToken("abcdefgh", STYLE, 3.0, UNIT);
        List<String> viaSplit = TokenBreaking.splitLongToken("abcdefgh", STYLE, 3.0, UNIT);
        assertThat(viaBreak).isEqualTo(viaSplit).containsExactly("abc", "def", "gh");
    }

    @Test
    void breakLongTokenReturnsTheWholeTokenWhenItAlreadyFits() {
        // A no-seam token that fits the budget is returned as a single piece.
        assertThat(TokenBreaking.breakLongToken("abc", STYLE, 10.0, UNIT)).containsExactly("abc");
    }

    @Test
    void breakLongTokenPacksSegmentsAtSeams() {
        // "org." + "junit." fill width 10 exactly; "jupiter" starts a new piece.
        assertThat(TokenBreaking.breakLongToken("org.junit.jupiter", STYLE, 10.0, UNIT))
                .containsExactly("org.junit.", "jupiter");
    }

    @Test
    void breakLongTokenCharSplitsAnOverWideSegment() {
        // "verylongsegment." (16) exceeds width 5, so it char-splits; the tail seeds
        // the piece that the following short "x" segment then joins.
        assertThat(TokenBreaking.breakLongToken("verylongsegment.x", STYLE, 5.0, UNIT))
                .containsExactly("veryl", "ongse", "gment", ".x");
    }

    @Test
    void breakLongTokenEveryPieceFitsAndReconstructsTheInput() {
        for (String token : List.of(
                "org.junit.jupiter:junit-jupiter:5.10.2",
                "https://repo1.maven.org/maven2/",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")) {
            for (double width : new double[] {1.0, 4.0, 7.0, 12.0}) {
                List<String> pieces = TokenBreaking.breakLongToken(token, STYLE, width, UNIT);
                assertThat(String.join("", pieces))
                        .as("pieces reconstruct the token at width %s", width).isEqualTo(token);
                assertThat(pieces).allSatisfy(piece ->
                        assertThat((double) piece.length())
                                .as("each piece fits width %s", width)
                                .isLessThanOrEqualTo(width + 1e-6));
            }
        }
    }

    @Test
    void breakLongTokenEmptyOrNullReturnsEmpty() {
        assertThat(TokenBreaking.breakLongToken("", STYLE, 5.0, UNIT)).isEmpty();
        assertThat(TokenBreaking.breakLongToken(null, STYLE, 5.0, UNIT)).isEmpty();
    }

    /** Deterministic measurement: width == character count * unit; metrics are inert. */
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
