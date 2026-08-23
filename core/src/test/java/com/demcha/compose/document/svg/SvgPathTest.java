package com.demcha.compose.document.svg;

import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.document.style.DocumentPathSegment.Close;
import com.demcha.compose.document.style.DocumentPathSegment.CubicTo;
import com.demcha.compose.document.style.DocumentPathSegment.LineTo;
import com.demcha.compose.document.style.DocumentPathSegment.MoveTo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Grammar and geometry coverage for {@link SvgPath}: every command family,
 * relative forms, implicit repetition, exact quad→cubic elevation, arc→cubic
 * conversion, viewBox vs tight-box normalization with the y-flip, and the
 * syntax-error contract.
 */
class SvgPathTest {

    @Test
    void absoluteTriangleNormalizesAndFlipsY() {
        SvgPath path = SvgPath.parse("M0 0 L10 0 L5 8 Z", 0, 0, 10, 8);

        List<DocumentPathSegment> s = path.segments();
        assertThat(s).hasSize(4);
        MoveTo move = (MoveTo) s.get(0);
        // SVG (0,0) is the top-left of the viewBox → flipped to y=1.
        assertThat(move.x()).isCloseTo(0.0, within(1e-9));
        assertThat(move.y()).isCloseTo(1.0, within(1e-9));
        LineTo apex = (LineTo) s.get(2);
        assertThat(apex.x()).isCloseTo(0.5, within(1e-9));
        assertThat(apex.y()).isCloseTo(0.0, within(1e-9));
        assertThat(s.get(3)).isInstanceOf(Close.class);
    }

    @Test
    void relativeCommandsAccumulateAndTightBoxNormalizes() {
        SvgPath path = SvgPath.parse("m1 1 l2 0 l0 2 z");

        List<DocumentPathSegment> s = path.segments();
        assertThat(s).hasSize(4);
        // User points (1,1) → (3,1) → (3,3); tight box is [1..3]×[1..3].
        MoveTo move = (MoveTo) s.get(0);
        assertThat(move.x()).isCloseTo(0.0, within(1e-9));
        assertThat(move.y()).isCloseTo(1.0, within(1e-9));
        LineTo last = (LineTo) s.get(2);
        assertThat(last.x()).isCloseTo(1.0, within(1e-9));
        assertThat(last.y()).isCloseTo(0.0, within(1e-9));
        assertThat(path.aspectRatio()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void horizontalAndVerticalShorthandsDraw() {
        SvgPath path = SvgPath.parse("M0 0 H10 V5 h-4 v-2", 0, 0, 10, 5);

        assertThat(path.segments()).hasSize(5);
        assertThat(path.segments().subList(1, 5)).allMatch(LineTo.class::isInstance);
        LineTo h = (LineTo) path.segments().get(1);
        assertThat(h.x()).isCloseTo(1.0, within(1e-9));
        assertThat(h.y()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void implicitLinetoContinuesAMovetoChain() {
        SvgPath path = SvgPath.parse("M0 0 10 0 10 10", 0, 0, 10, 10);

        assertThat(path.segments()).hasSize(3);
        assertThat(path.segments().get(0)).isInstanceOf(MoveTo.class);
        assertThat(path.segments().get(1)).isInstanceOf(LineTo.class);
        assertThat(path.segments().get(2)).isInstanceOf(LineTo.class);
    }

    @Test
    void quadraticElevatesToCubicExactly() {
        SvgPath path = SvgPath.parse("M0 0 Q5 10 10 0", 0, 0, 10, 10);

        CubicTo cubic = (CubicTo) path.segments().get(1);
        // c1 = q0 + 2/3(q − q0) = (10/3, 20/3) in user space; y flips.
        assertThat(cubic.control1X()).isCloseTo(1.0 / 3.0, within(1e-9));
        assertThat(cubic.control1Y()).isCloseTo(1.0 / 3.0, within(1e-9));
        assertThat(cubic.control2X()).isCloseTo(2.0 / 3.0, within(1e-9));
        assertThat(cubic.control2Y()).isCloseTo(1.0 / 3.0, within(1e-9));
        assertThat(cubic.y()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void smoothCubicReflectsThePreviousControl() {
        SvgPath path = SvgPath.parse("M0 5 C0 0 5 0 5 5 S10 10 10 5", 0, 0, 10, 10);

        CubicTo smooth = (CubicTo) path.segments().get(2);
        // Reflection of (5,0) about (5,5) is (5,10) in user space → y=0 flipped.
        assertThat(smooth.control1X()).isCloseTo(0.5, within(1e-9));
        assertThat(smooth.control1Y()).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void smoothQuadReflectsThePreviousControl() {
        SvgPath path = SvgPath.parse("M0 5 Q2.5 0 5 5 T10 5", 0, 0, 10, 10);

        assertThat(path.segments()).hasSize(3);
        CubicTo second = (CubicTo) path.segments().get(2);
        // Reflected quad control is (7.5, 10) user → c1 = cur + 2/3 (q − cur)
        // = (5 + 5/3, 5 + 10/3); flipped y = 1 − 25/30.
        assertThat(second.control1X()).isCloseTo((5 + 5.0 / 3.0) / 10.0, within(1e-9));
        assertThat(second.control1Y()).isCloseTo(1.0 - (5 + 10.0 / 3.0) / 10.0, within(1e-9));
    }

    @Test
    void quarterArcApproximatesTheCircleConstant() {
        // Quarter circle radius 10 from (10,0) to (0,10), centre at origin.
        SvgPath path = SvgPath.parse("M10 0 A10 10 0 0 1 0 10", 0, 0, 10, 10);

        assertThat(path.segments()).hasSize(2);
        CubicTo arc = (CubicTo) path.segments().get(1);
        // First control ≈ (10, κ·10) in user space, κ = 0.5523.
        assertThat(arc.control1X()).isCloseTo(1.0, within(1e-3));
        assertThat(arc.control1Y()).isCloseTo(1.0 - 0.5523, within(1e-3));
        assertThat(arc.x()).isCloseTo(0.0, within(1e-9));
        assertThat(arc.y()).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void largeArcSplitsIntoNinetyDegreeSlices() {
        // Three-quarter circle: 270° → three cubic slices.
        SvgPath path = SvgPath.parse("M10 5 A5 5 0 1 1 5 0", -1, -1, 12, 12);

        long cubics = path.segments().stream().filter(CubicTo.class::isInstance).count();
        assertThat(cubics).isEqualTo(3);
    }

    @Test
    void zeroRadiusArcDegradesToALine() {
        SvgPath path = SvgPath.parse("M0 0 A0 5 0 0 1 10 10", 0, 0, 10, 10);

        assertThat(path.segments().get(1)).isInstanceOf(LineTo.class);
    }

    @Test
    void compactArcFlagsParse() {
        // Flags are single characters: "011 1" carries large-arc=0, sweep=1,
        // then the endpoint 1,1 — a form vector editors actually emit.
        SvgPath path = SvgPath.parse("M0 0 a1 1 0 011 1", 0, 0, 2, 2);

        assertThat(path.segments().stream().anyMatch(CubicTo.class::isInstance)).isTrue();
    }

    @Test
    void materialHeartParsesEndToEnd() {
        String d = "M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3"
                   + "c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5"
                   + "c0 3.78-3.4 6.86-8.55 11.54L12 21.35z";
        SvgPath heart = SvgPath.parse(d, 0, 0, 24, 24);

        assertThat(heart.segments().size()).isGreaterThan(8);
        assertThat(heart.segments().get(0)).isInstanceOf(MoveTo.class);
        assertThat(heart.aspectRatio()).isCloseTo(1.0, within(1e-9));
        // Every coordinate landed inside the viewBox-normalized unit range.
        for (DocumentPathSegment segment : heart.segments()) {
            if (segment instanceof LineTo line) {
                assertThat(line.x()).isBetween(0.0, 1.0);
                assertThat(line.y()).isBetween(0.0, 1.0);
            }
        }
    }

    @Test
    void flatPathsKeepFiniteCoordinates() {
        SvgPath path = SvgPath.parse("M0 5 H10");

        LineTo line = (LineTo) path.segments().get(1);
        assertThat(line.y()).isCloseTo(0.5, within(1e-9));
        assertThat(path.sourceHeight()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void syntaxErrorsCarryThePosition() {
        assertThatThrownBy(() -> SvgPath.parse("M0 0 X5 5"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("position");
        assertThatThrownBy(() -> SvgPath.parse("L1 1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must start with a moveto");
        assertThatThrownBy(() -> SvgPath.parse(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
        assertThatThrownBy(() -> SvgPath.parse("M0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("number");
        assertThatThrownBy(() -> SvgPath.parse("M0 0 A1 1 0 2 0 5 5"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("arc flag");
        assertThatThrownBy(() -> SvgPath.parse("M0 0 L1 1", 0, 0, 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("viewBox");
    }

    @Test
    void strayTokenAfterCloseIsRejectedAndDoesNotHang() {
        // Regression: an operand-less Z/z followed by a non-command token used
        // to spin forever (the close op consumes no characters, so the scanner
        // never advanced), appending a close op per iteration until OOM. A
        // single malformed/hostile 'd' string would DoS the @Beta reader.
        // Each call must fail fast; the assertTimeout pins that it cannot hang.
        Assertions.assertTimeoutPreemptively(
                Duration.ofSeconds(2), () -> {
                    assertThatThrownBy(() -> SvgPath.parse("M0 0 Z5"))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining("position");
                    assertThatThrownBy(() -> SvgPath.parse("M0 0 z9 9"))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining("position");
                });
    }

    @Test
    void scientificNotationNumbersParse() {
        // Optimizing exporters (SVGO) emit exponents: 1e1 = 10, 2E1 = 20,
        // 5e-1 = 0.5. Same square as "M0 0 L10 0 L10 10 L0 10 Z".
        SvgPath path = SvgPath.parse("M0 0 L1e1 0 L1E1 1e+1 L0 10 Z", 0, 0, 10, 10);

        List<DocumentPathSegment> s = path.segments();
        assertThat(s).hasSize(5);
        LineTo corner = (LineTo) s.get(2);
        assertThat(corner.x()).isCloseTo(1.0, within(1e-9));
        // SVG y=10 is the viewBox bottom → flipped to 0.
        assertThat(corner.y()).isCloseTo(0.0, within(1e-9));
        LineTo half = (LineTo) SvgPath.parse("M0 0 L5e-1 0 L0.5 1 Z", 0, 0, 1, 1)
                .segments().get(1);
        assertThat(half.x()).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void rotatedArcLandsOnItsEndpoint() {
        // x-axis-rotation only shapes the interior of the arc; the endpoint
        // contract holds regardless. Pin it for a non-zero rotation, which no
        // other test exercises.
        SvgPath path = SvgPath.parse("M0 5 A5 2.5 45 0 1 10 5", 0, 0, 10, 10);

        List<DocumentPathSegment> s = path.segments();
        assertThat(s.get(0)).isInstanceOf(MoveTo.class);
        assertThat(s.size()).isGreaterThanOrEqualTo(2);
        assertThat(s.subList(1, s.size())).allMatch(segment -> segment instanceof CubicTo);
        CubicTo last = (CubicTo) s.get(s.size() - 1);
        assertThat(last.x()).isCloseTo(1.0, within(1e-9));
        assertThat(last.y()).isCloseTo(0.5, within(1e-9));
    }
}
