package com.demcha.compose;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic regression gate for the v1.8 vector-paint render branches,
 * driving {@link VectorRenderOperatorProbe#countOperators}.
 *
 * <p>A flat fill takes the fast path (no shading / alpha / clip); a linear
 * gradient clips to the shape and paints a shading (one {@code W} clip + one
 * {@code sh} per shape); a translucent fill sets an ExtGState alpha (one
 * {@code gs} per shape). Pinning these operator counts makes a regression — a
 * flat path accidentally taking the heavier gradient branch, or the gradient
 * clip/shading being dropped — a build failure rather than a silent CI pass.</p>
 */
class VectorRenderOperatorGateTest {

    @Test
    void flatFillTakesTheFastPathWithNoShadingAlphaOrClip() throws Exception {
        VectorRenderOperatorProbe.OperatorCounts flat =
                VectorRenderOperatorProbe.countOperators(VectorRenderOperatorProbe.PaintMode.FLAT);

        assertThat(flat.curves()).as("flat paths still emit curve operators").isGreaterThan(0);
        assertThat(flat.shadings()).as("flat fill must not paint a shading").isZero();
        assertThat(flat.extGStates()).as("flat fill must not set an ExtGState alpha").isZero();
        assertThat(flat.clips()).as("flat fill must not clip").isZero();
    }

    @Test
    void gradientFillClipsAndShadesOncePerShape() throws Exception {
        VectorRenderOperatorProbe.OperatorCounts gradient =
                VectorRenderOperatorProbe.countOperators(VectorRenderOperatorProbe.PaintMode.GRADIENT);

        assertThat(gradient.shadings())
                .as("a linear gradient paints one shading per shape")
                .isEqualTo(VectorRenderOperatorProbe.PATHS);
        assertThat(gradient.clips())
                .as("a gradient clips to each shape before shading")
                .isEqualTo(VectorRenderOperatorProbe.PATHS);
    }

    @Test
    void translucentFillSetsOneExtGStatePerShape() throws Exception {
        VectorRenderOperatorProbe.OperatorCounts alpha =
                VectorRenderOperatorProbe.countOperators(VectorRenderOperatorProbe.PaintMode.ALPHA);

        assertThat(alpha.extGStates())
                .as("a translucent fill sets one ExtGState alpha per shape")
                .isEqualTo(VectorRenderOperatorProbe.PATHS);
        assertThat(alpha.shadings())
                .as("a translucent solid fill must not paint a shading")
                .isZero();
    }
}
