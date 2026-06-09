package com.demcha.compose.engine.font;

/**
 * Backend-neutral vertical text metrics for one resolved text style: the
 * baseline-relative {@code ascent} and {@code descent} plus inter-line
 * {@code leading}, all in document units.
 *
 * <p>This is the font-layer counterpart of
 * {@code engine.measurement.TextMeasurementSystem.LineMetrics}. It lives in
 * {@code engine.font} so the {@link Font} contract can expose vertical metrics
 * without {@code engine.font} depending on {@code engine.measurement} (which
 * already depends on {@code engine.font}); the measurement system converts this
 * record into its own {@code LineMetrics} for layout consumers.</p>
 *
 * @param ascent  distance from the baseline to the glyph top
 * @param descent distance from the baseline to the glyph bottom (non-negative)
 * @param leading extra line leading applied by the backend font metrics
 */
public record FontLineMetrics(double ascent, double descent, double leading) {

    /**
     * Returns the total baseline-to-baseline line height.
     *
     * @return {@code ascent + descent + leading}
     */
    public double lineHeight() {
        return ascent + descent + leading;
    }

    /**
     * Returns the distance from the line bottom to the text baseline.
     *
     * @return the baseline offset, equal to {@code descent}
     */
    public double baselineOffsetFromBottom() {
        return descent;
    }
}
