package com.demcha.compose.document.node;

import com.demcha.compose.document.svg.SvgIcon;

import java.util.Objects;

/**
 * One inline SVG-icon run inside a {@link ParagraphNode} — a parsed
 * {@link SvgIcon} drawn as crisp vector layers on the surrounding text
 * baseline.
 *
 * <p>Inline SVG icons are measured as part of paragraph wrapping exactly like
 * {@link InlineImageRun}: the run's {@link #width()} / {@link #height()}
 * contribute to span placement, line breaking and per-line height, and the
 * glyph shares the text baseline. Unlike a raster image the icon stays sharp at
 * any zoom, and unlike a font glyph it carries its own colours — so it renders
 * regardless of the active font's coverage. This is the inline path for vector
 * colour emoji (Twemoji SVG) and other small vector marks.</p>
 *
 * @param icon           parsed vector icon; never {@code null}
 * @param width          target width in points; must be finite and positive
 * @param height         target height in points; must be finite and positive
 * @param alignment      vertical alignment relative to the surrounding text;
 *                       defaults to {@link InlineImageAlignment#CENTER}
 * @param baselineOffset extra vertical offset in points applied after
 *                       {@code alignment} resolution; positive values move the
 *                       icon up
 * @param linkTarget     optional per-run link target (external URI or internal anchor)
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public record InlineSvgRun(
        SvgIcon icon,
        double width,
        double height,
        InlineImageAlignment alignment,
        double baselineOffset,
        DocumentLinkTarget linkTarget
) implements InlineRun {
    /**
     * Validates dimensions and normalizes alignment defaults.
     */
    public InlineSvgRun {
        Objects.requireNonNull(icon, "icon");
        if (width <= 0 || Double.isNaN(width) || Double.isInfinite(width)) {
            throw new IllegalArgumentException("inline svg width must be finite and positive: " + width);
        }
        if (height <= 0 || Double.isNaN(height) || Double.isInfinite(height)) {
            throw new IllegalArgumentException("inline svg height must be finite and positive: " + height);
        }
        if (Double.isNaN(baselineOffset) || Double.isInfinite(baselineOffset)) {
            throw new IllegalArgumentException("inline svg baselineOffset must be finite: " + baselineOffset);
        }
        alignment = alignment == null ? InlineImageAlignment.CENTER : alignment;
    }

    /**
     * Convenience constructor with default {@link InlineImageAlignment#CENTER}
     * alignment and zero offset.
     *
     * @param icon   parsed vector icon
     * @param width  target width in points
     * @param height target height in points
     */
    public InlineSvgRun(SvgIcon icon, double width, double height) {
        this(icon, width, height, InlineImageAlignment.CENTER, 0.0, (DocumentLinkTarget) null);
    }

    /**
     * Creates an inline SVG run with external link metadata.
     *
     * @param icon           parsed vector icon
     * @param width          target width in points
     * @param height         target height in points
     * @param alignment      vertical alignment relative to surrounding text
     * @param baselineOffset extra vertical shift in points; positive moves up
     * @param linkOptions    external link metadata, wrapped into an
     *                       {@link ExternalLinkTarget}
     */
    public InlineSvgRun(SvgIcon icon,
                        double width,
                        double height,
                        InlineImageAlignment alignment,
                        double baselineOffset,
                        DocumentLinkOptions linkOptions) {
        this(icon, width, height, alignment, baselineOffset,
                linkOptions == null ? null : new ExternalLinkTarget(linkOptions));
    }
}
