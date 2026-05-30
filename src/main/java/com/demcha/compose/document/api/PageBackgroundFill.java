package com.demcha.compose.document.api;

import com.demcha.compose.document.style.DocumentColor;

import java.util.Objects;

/**
 * Per-page rectangular background fill, defined as ratios of the
 * canvas page size so the same fill scales correctly to any page
 * format. Used by {@link DocumentSession#pageBackgrounds(java.util.List)}
 * to paint multi-column or partial-page backgrounds that repeat on
 * every page automatically.
 *
 * <p>Use the factory methods for the common cases:</p>
 * <ul>
 *   <li>{@link #fullPage(DocumentColor)} — entire page (same effect as
 *       the legacy single-color {@link DocumentSession#pageBackground}).</li>
 *   <li>{@link #leftColumn(double, DocumentColor)} — full-height column
 *       aligned to the left edge.</li>
 *   <li>{@link #rightColumn(double, DocumentColor)} — full-height
 *       column aligned to the right edge.</li>
 *   <li>{@link #column(double, double, DocumentColor)} — arbitrary
 *       horizontal slice spanning the full page height.</li>
 *   <li>{@link #topBand(double, DocumentColor)} /
 *       {@link #bottomBand(double, DocumentColor)} /
 *       {@link #band(double, double, DocumentColor)} — full-width
 *       horizontal bands at the top, bottom, or an arbitrary vertical
 *       offset (also available in absolute points via
 *       {@link #topBandPoints(double, double, DocumentColor)} and
 *       {@link #bandPoints(double, double, double, DocumentColor)}).</li>
 * </ul>
 *
 * <p>Fills supplied to a session are painted at z=0 (below every other
 * fragment) in list order, so later entries paint on top of earlier
 * entries when they overlap. This is the natural way to layer a
 * narrow accent column over a full-page tint.</p>
 *
 * @param xRatio      0.0 = left edge, 1.0 = right edge
 * @param yRatio      top edge of the fill: 0.0 = page top, 1.0 = page
 *                    bottom. The fill extends downward from here by
 *                    {@code heightRatio}.
 * @param widthRatio  width as a fraction of the canvas width (0..1]
 * @param heightRatio height as a fraction of the canvas height (0..1].
 *                    Keep {@code yRatio + heightRatio <= 1.0} so the fill
 *                    stays within the page.
 * @param color       fill color (required)
 */
public record PageBackgroundFill(double xRatio,
                                 double yRatio,
                                 double widthRatio,
                                 double heightRatio,
                                 DocumentColor color) {

    public PageBackgroundFill {
        Objects.requireNonNull(color, "color");
        if (xRatio < 0.0 || xRatio > 1.0) {
            throw new IllegalArgumentException(
                    "xRatio must be in [0,1] but was " + xRatio);
        }
        if (yRatio < 0.0 || yRatio > 1.0) {
            throw new IllegalArgumentException(
                    "yRatio must be in [0,1] but was " + yRatio);
        }
        if (widthRatio <= 0.0 || widthRatio > 1.0) {
            throw new IllegalArgumentException(
                    "widthRatio must be in (0,1] but was " + widthRatio);
        }
        if (heightRatio <= 0.0 || heightRatio > 1.0) {
            throw new IllegalArgumentException(
                    "heightRatio must be in (0,1] but was " + heightRatio);
        }
    }

    /** Full-page fill, equivalent to the legacy single-color page background. */
    public static PageBackgroundFill fullPage(DocumentColor color) {
        return new PageBackgroundFill(0.0, 0.0, 1.0, 1.0, color);
    }

    /** Full-height column at the left page edge, with width = ratio of page width. */
    public static PageBackgroundFill leftColumn(double widthRatio,
                                                DocumentColor color) {
        return new PageBackgroundFill(0.0, 0.0, widthRatio, 1.0, color);
    }

    /** Full-height column at the right page edge, with width = ratio of page width. */
    public static PageBackgroundFill rightColumn(double widthRatio,
                                                 DocumentColor color) {
        return new PageBackgroundFill(1.0 - widthRatio, 0.0,
                widthRatio, 1.0, color);
    }

    /** Full-height column at an arbitrary horizontal offset. */
    public static PageBackgroundFill column(double xRatio,
                                            double widthRatio,
                                            DocumentColor color) {
        return new PageBackgroundFill(xRatio, 0.0, widthRatio, 1.0, color);
    }

    /** Full-width band flush with the top of the page (height = ratio of page height). */
    public static PageBackgroundFill topBand(double heightRatio,
                                             DocumentColor color) {
        return new PageBackgroundFill(0.0, 0.0, 1.0, heightRatio, color);
    }

    /** Full-width band flush with the bottom of the page (height = ratio of page height). */
    public static PageBackgroundFill bottomBand(double heightRatio,
                                                DocumentColor color) {
        return new PageBackgroundFill(0.0, 1.0 - heightRatio, 1.0,
                heightRatio, color);
    }

    /** Full-width band whose top edge sits {@code yRatioFromTop} down the page (0.0 = page top). */
    public static PageBackgroundFill band(double yRatioFromTop,
                                          double heightRatio,
                                          DocumentColor color) {
        return new PageBackgroundFill(0.0, yRatioFromTop, 1.0,
                heightRatio, color);
    }

    /** Top-aligned band sized in absolute points, converted against {@code pageHeight}. */
    public static PageBackgroundFill topBandPoints(double heightPoints,
                                                   double pageHeight,
                                                   DocumentColor color) {
        return topBand(heightPoints / pageHeight, color);
    }

    /** Band positioned and sized in absolute points from the page top, converted against {@code pageHeight}. */
    public static PageBackgroundFill bandPoints(double yFromTopPoints,
                                                double heightPoints,
                                                double pageHeight,
                                                DocumentColor color) {
        return band(yFromTopPoints / pageHeight, heightPoints / pageHeight,
                color);
    }
}
