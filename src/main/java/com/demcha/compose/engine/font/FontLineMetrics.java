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
 * <p>Carries only the three primitive components; the measurement system reads
 * them via {@code ascent()}/{@code descent()}/{@code leading()} and converts to
 * {@code TextMeasurementSystem.LineMetrics} (which owns the derived
 * {@code lineHeight()} / baseline helpers) for layout consumers.</p>
 *
 * @param ascent  distance from the baseline to the glyph top
 * @param descent distance from the baseline to the glyph bottom (non-negative)
 * @param leading extra line leading applied by the backend font metrics
 */
public record FontLineMetrics(double ascent, double descent, double leading) {
}
