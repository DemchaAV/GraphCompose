package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.DocumentLinkTarget;
import com.demcha.compose.document.node.InlineImageAlignment;

import java.util.List;

/**
 * Measured inline SVG-icon span inside a paragraph line — a stack of resolved
 * {@link ResolvedSvgLayer}s drawn back-to-front and scaled into the span's
 * bounding box, so a vector glyph (e.g. a colour emoji) places on the text
 * baseline as one unit.
 *
 * @param layers         resolved paint layers, back-to-front
 * @param width          bounding width in points
 * @param height         bounding height in points
 * @param alignment      vertical alignment relative to the surrounding text
 * @param baselineOffset extra vertical offset in points; positive moves up
 * @param linkTarget     optional link metadata
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public record ParagraphSvgSpan(
        List<ResolvedSvgLayer> layers,
        double width,
        double height,
        InlineImageAlignment alignment,
        double baselineOffset,
        DocumentLinkTarget linkTarget
) implements ParagraphSpan {
    /**
     * Copies the layer stack defensively and normalizes alignment defaults.
     */
    public ParagraphSvgSpan {
        layers = List.copyOf(layers);
        alignment = alignment == null ? InlineImageAlignment.CENTER : alignment;
    }
}
