package com.demcha.compose.document.node;

import com.demcha.compose.document.image.DocumentImageData;

import java.util.Objects;

/**
 * One inline image run inside a {@link ParagraphNode}.
 *
 * <p>Inline images are measured as part of paragraph wrapping: their width
 * and height contribute to span placement, line breaking and per-line
 * height. They participate in the same baseline as adjacent text runs and
 * can carry their own link metadata so the resulting PDF annotation covers
 * the icon (or icon + label, when callers split the link across runs).</p>
 *
 * @param imageData semantic image payload; never {@code null}
 * @param width target width in points; must be finite and positive
 * @param height target height in points; must be finite and positive
 * @param alignment vertical alignment relative to the surrounding text;
 *                  defaults to {@link InlineImageAlignment#CENTER}
 * @param baselineOffset extra vertical offset in points applied after
 *                       {@code alignment} resolution; positive values move
 *                       the image up
 * @param linkOptions optional per-run link metadata
 *
 * @author Artem Demchyshyn
 */
public record InlineImageRun(
        DocumentImageData imageData,
        double width,
        double height,
        InlineImageAlignment alignment,
        double baselineOffset,
        DocumentLinkOptions linkOptions
) implements InlineRun {
    /**
     * Validates dimensions and normalizes alignment defaults.
     */
    public InlineImageRun {
        Objects.requireNonNull(imageData, "imageData");
        if (width <= 0 || Double.isNaN(width) || Double.isInfinite(width)) {
            throw new IllegalArgumentException("inline image width must be finite and positive: " + width);
        }
        if (height <= 0 || Double.isNaN(height) || Double.isInfinite(height)) {
            throw new IllegalArgumentException("inline image height must be finite and positive: " + height);
        }
        if (Double.isNaN(baselineOffset) || Double.isInfinite(baselineOffset)) {
            throw new IllegalArgumentException("inline image baselineOffset must be finite: " + baselineOffset);
        }
        alignment = alignment == null ? InlineImageAlignment.CENTER : alignment;
    }

    /**
     * Convenience constructor with default {@link InlineImageAlignment#CENTER}
     * alignment and zero offset.
     *
     * @param imageData image payload
     * @param width target width in points
     * @param height target height in points
     */
    public InlineImageRun(DocumentImageData imageData, double width, double height) {
        this(imageData, width, height, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Convenience constructor with explicit alignment, zero offset and no
     * link metadata.
     *
     * @param imageData image payload
     * @param width target width in points
     * @param height target height in points
     * @param alignment vertical alignment
     */
    public InlineImageRun(DocumentImageData imageData,
                          double width,
                          double height,
                          InlineImageAlignment alignment) {
        this(imageData, width, height, alignment, 0.0, null);
    }
}
