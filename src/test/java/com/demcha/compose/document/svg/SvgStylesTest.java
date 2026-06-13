package com.demcha.compose.document.svg;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentLineJoin;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit coverage for the SVG presentation-attribute grammar: the colour
 * subset (hex incl. alpha, {@code rgb()}/{@code rgba()}, CSS names), absolute
 * length units, and the stroke style trio.
 */
class SvgStylesTest {

    private static final DocumentColor INK = DocumentColor.rgb(0, 0, 0);

    @Test
    void hexColoursCoverThreeFourSixAndEightDigits() {
        assertThat(SvgStyles.color("#abc", INK).color()).isEqualTo(new Color(170, 187, 204));
        assertThat(SvgStyles.color("#A78BFA", INK).color()).isEqualTo(new Color(167, 139, 250));
        // 4- and 8-digit forms carry alpha in the low byte.
        assertThat(SvgStyles.color("#0000", INK).color().getAlpha()).isZero();
        Color half = SvgStyles.color("#11223380", INK).color();
        assertThat(half.getRed()).isEqualTo(0x11);
        assertThat(half.getAlpha()).isEqualTo(0x80);
    }

    @Test
    void rgbAndRgbaAcceptNumbersAndPercentages() {
        assertThat(SvgStyles.color("rgb(196, 30, 58)", INK).color()).isEqualTo(new Color(196, 30, 58));
        assertThat(SvgStyles.color("rgb(100%, 0%, 50%)", INK).color()).isEqualTo(new Color(255, 0, 128));
        Color translucent = SvgStyles.color("rgba(20, 80, 95, 0.5)", INK).color();
        assertThat(translucent.getAlpha()).isEqualTo(128);
    }

    @Test
    void namedColoursResolveCaseInsensitively() {
        assertThat(SvgStyles.color("RebeccaPurple", INK).color()).isEqualTo(new Color(102, 51, 153));
        assertThat(SvgStyles.color("tomato", INK).color()).isEqualTo(new Color(255, 99, 71));
        assertThat(SvgStyles.color("none", INK)).isNull();
        assertThat(SvgStyles.color("currentColor", DocumentColor.rgb(1, 2, 3)).color())
                .isEqualTo(new Color(1, 2, 3));
    }

    @Test
    void unknownColourFailsWithGuidance() {
        assertThatThrownBy(() -> SvgStyles.color("burlywoodish", INK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CSS colour name");
    }

    @Test
    void absoluteLengthUnitsConvertToUserUnits() {
        assertThat(SvgStyles.length("7", "x")).isCloseTo(7.0, within(1e-9));
        assertThat(SvgStyles.length("7px", "x")).isCloseTo(7.0, within(1e-9));
        assertThat(SvgStyles.length("72pt", "x")).isCloseTo(96.0, within(1e-9));
        assertThat(SvgStyles.length("1in", "x")).isCloseTo(96.0, within(1e-9));
        assertThat(SvgStyles.length("25.4mm", "x")).isCloseTo(96.0, within(1e-6));
    }

    @Test
    void relativeUnitsAreRefused() {
        assertThatThrownBy(() -> SvgStyles.length("2em", "stroke-width"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported unit");
    }

    @Test
    void strokeStyleEnumsParse() {
        assertThat(SvgStyles.lineCap("round")).isEqualTo(DocumentLineCap.ROUND);
        assertThat(SvgStyles.lineCap("square")).isEqualTo(DocumentLineCap.SQUARE);
        assertThat(SvgStyles.lineJoin("bevel")).isEqualTo(DocumentLineJoin.BEVEL);
        // SVG2 miter variants degrade to plain MITER.
        assertThat(SvgStyles.lineJoin("miter-clip")).isEqualTo(DocumentLineJoin.MITER);
        assertThatThrownBy(() -> SvgStyles.lineCap("pointy"))
                .hasMessageContaining("stroke-linecap");
    }

    @Test
    void dashArrayDoublesOddLengthListsAndTreatsZeroAsSolid() {
        assertThat(SvgStyles.dashArray("4 2")).containsExactly(4.0, 2.0);
        // Odd count repeats per the SVG rule.
        assertThat(SvgStyles.dashArray("5")).containsExactly(5.0, 5.0);
        assertThat(SvgStyles.dashArray("none")).isEmpty();
        assertThat(SvgStyles.dashArray("0 0")).isEmpty();
        // Units inside the list resolve too.
        assertThat(SvgStyles.dashArray("3pt")).hasSize(2);
        assertThatThrownBy(() -> SvgStyles.dashArray("-3 2"))
                .hasMessageContaining("non-negative");
    }

    @Test
    void dashArrayReturnsImmutableList() {
        List<Double> dashes = SvgStyles.dashArray("4 2");
        assertThatThrownBy(() -> dashes.add(1.0))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
