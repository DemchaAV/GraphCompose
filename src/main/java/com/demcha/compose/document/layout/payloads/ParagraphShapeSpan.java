package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineImageAlignment;

import java.util.List;

/**
 * Measured inline shape span inside a paragraph line — a stack of resolved
 * {@link ResolvedShapeLayer}s drawn overlaid and centred within the span's
 * bounding box, so composite figures (e.g. a checkbox: box + checkmark) place
 * on the text baseline as one unit.
 *
 * @param layers resolved paint layers, back-to-front
 * @param width bounding width in points
 * @param height bounding height in points
 * @param alignment vertical alignment relative to the surrounding text
 * @param baselineOffset extra vertical offset in points; positive moves up
 * @param linkOptions optional link metadata
 */
public record ParagraphShapeSpan(
        List<ResolvedShapeLayer> layers,
        double width,
        double height,
        InlineImageAlignment alignment,
        double baselineOffset,
        DocumentLinkOptions linkOptions
) implements ParagraphSpan {
    /**
     * Copies the layer stack defensively and normalizes alignment defaults.
     */
    public ParagraphShapeSpan {
        layers = List.copyOf(layers);
        alignment = alignment == null ? InlineImageAlignment.CENTER : alignment;
    }
}
