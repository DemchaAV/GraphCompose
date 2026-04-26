package com.demcha.compose.document.style;

/**
 * Public auto-size hint for paragraph text rendering.
 *
 * <p>When attached to a paragraph, the layout engine searches for the largest
 * font size in the inclusive range {@code [minSize, maxSize]} that fits the
 * paragraph's text on a single line within the resolved inner width. If even
 * the minimum size does not fit, the paragraph wraps as usual at the minimum
 * size.</p>
 *
 * @param maxSize upper bound for the resolved font size in points
 * @param minSize lower bound for the resolved font size in points
 * @param step search step in points (rounded down to a positive value)
 *
 * @author Artem Demchyshyn
 */
public record DocumentTextAutoSize(double maxSize, double minSize, double step) {
    public static final double DEFAULT_STEP = 0.5;
    public static final double DEFAULT_MIN_SIZE = 6.0;

    /**
     * Creates a normalized auto-size hint.
     */
    public DocumentTextAutoSize {
        if (maxSize <= 0 || Double.isNaN(maxSize) || Double.isInfinite(maxSize)) {
            throw new IllegalArgumentException("maxSize must be positive and finite: " + maxSize);
        }
        if (minSize <= 0 || Double.isNaN(minSize) || Double.isInfinite(minSize)) {
            throw new IllegalArgumentException("minSize must be positive and finite: " + minSize);
        }
        if (minSize > maxSize) {
            throw new IllegalArgumentException("minSize " + minSize + " must be <= maxSize " + maxSize);
        }
        if (step <= 0 || Double.isNaN(step) || Double.isInfinite(step)) {
            step = DEFAULT_STEP;
        }
    }

    /**
     * Convenience factory using {@link #DEFAULT_STEP}.
     *
     * @param maxSize upper bound for the resolved font size in points
     * @param minSize lower bound for the resolved font size in points
     * @return auto-size hint with the default step
     */
    public static DocumentTextAutoSize between(double maxSize, double minSize) {
        return new DocumentTextAutoSize(maxSize, minSize, DEFAULT_STEP);
    }

    /**
     * Convenience factory using {@link #DEFAULT_MIN_SIZE} as the lower bound and
     * {@link #DEFAULT_STEP} as the search step.
     *
     * @param maxSize upper bound for the resolved font size in points
     * @return auto-size hint with default minimum size and step
     */
    public static DocumentTextAutoSize upTo(double maxSize) {
        return new DocumentTextAutoSize(maxSize, DEFAULT_MIN_SIZE, DEFAULT_STEP);
    }
}
