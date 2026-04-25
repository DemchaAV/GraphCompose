package com.demcha.compose.document.layout;

/**
 * Available layout space for one node measurement/layout step.
 *
 * @param availableWidth available content width
 * @param availableHeight available content height
 */
public record BoxConstraints(double availableWidth, double availableHeight) {

    /**
     * Sentinel value used to represent an effectively unbounded vertical axis.
     *
     * <p>The constraint contract rejects {@link Double#POSITIVE_INFINITY}, so the
     * engine uses a large finite ceiling instead. Use the {@link #natural(double)}
     * factory rather than this constant directly.</p>
     */
    public static final double UNBOUNDED_HEIGHT = 1_000_000.0;

    /**
     * Creates validated non-negative layout constraints.
     */
    public BoxConstraints {
        if (availableWidth < 0 || Double.isNaN(availableWidth) || Double.isInfinite(availableWidth)) {
            throw new IllegalArgumentException("availableWidth must be finite and non-negative: " + availableWidth);
        }
        if (availableHeight < 0 || Double.isNaN(availableHeight) || Double.isInfinite(availableHeight)) {
            throw new IllegalArgumentException("availableHeight must be finite and non-negative: " + availableHeight);
        }
    }

    /**
     * Constraints with a fixed width and an effectively unbounded height, used when
     * the engine wants a node to report its natural / preferred height.
     *
     * @param availableWidth available content width
     * @return constraints fixing the width and using {@link #UNBOUNDED_HEIGHT} for the height
     */
    public static BoxConstraints natural(double availableWidth) {
        return new BoxConstraints(availableWidth, UNBOUNDED_HEIGHT);
    }

    /**
     * Compatibility alias for {@link #natural(double)}.
     *
     * @param availableWidth available content width
     * @return natural measurement constraints
     */
    public static BoxConstraints unboundedHeight(double availableWidth) {
        return natural(availableWidth);
    }
}
