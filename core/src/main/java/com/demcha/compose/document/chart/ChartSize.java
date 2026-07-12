package com.demcha.compose.document.chart;

/**
 * Resolves chart height once the layout pass supplies the available width.
 * Width is always taken from the enclosing container (responsive); only the
 * height policy varies.
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public sealed interface ChartSize permits ChartSize.Fixed, ChartSize.AspectRatio {

    /**
     * Height fixed in points regardless of width.
     *
     * @param points fixed height
     * @return size policy
     */
    static ChartSize fixedHeight(double points) {
        return new Fixed(points);
    }

    /**
     * Height derived from width by the given aspect ratio (w:h).
     *
     * @param w ratio width term
     * @param h ratio height term
     * @return size policy
     */
    static ChartSize aspectRatio(double w, double h) {
        return new AspectRatio(w, h);
    }

    /**
     * Resolves the chart height in points.
     *
     * @param availableWidth width handed down by the layout pass
     * @return chart height in points
     */
    double resolveHeight(double availableWidth);

    /**
     * Fixed-height policy.
     *
     * @param points height in points
     */
    record Fixed(double points) implements ChartSize {
        /**
         * Validates the fixed height.
         */
        public Fixed {
            if (points <= 0 || Double.isNaN(points) || Double.isInfinite(points)) {
                throw new IllegalArgumentException("fixed height must be finite and positive: " + points);
            }
        }

        @Override
        public double resolveHeight(double availableWidth) {
            return points;
        }
    }

    /**
     * Aspect-ratio height policy.
     *
     * @param w ratio width term
     * @param h ratio height term
     */
    record AspectRatio(double w, double h) implements ChartSize {
        /**
         * Validates the ratio terms.
         */
        public AspectRatio {
            if (w <= 0 || h <= 0 || Double.isNaN(w) || Double.isNaN(h)) {
                throw new IllegalArgumentException("aspect ratio sides must be positive: " + w + ":" + h);
            }
        }

        @Override
        public double resolveHeight(double availableWidth) {
            return availableWidth * (h / w);
        }
    }
}
