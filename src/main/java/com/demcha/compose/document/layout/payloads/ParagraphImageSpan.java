package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.engine.components.content.ImageData;

import java.util.Objects;

/**
 * Measured inline image span inside a paragraph line.
 *
 * @param imageData      engine image payload, ready for the PDF backend
 * @param width          target width in points
 * @param height         target height in points
 * @param alignment      vertical alignment relative to the surrounding text
 * @param baselineOffset extra vertical offset in points; positive moves up
 * @param linkOptions    optional link metadata
 */
public record ParagraphImageSpan(
        ImageData imageData,
        double width,
        double height,
        InlineImageAlignment alignment,
        double baselineOffset,
        DocumentLinkOptions linkOptions
) implements ParagraphSpan {
    /**
     * Validates and normalizes inline image span fields.
     */
    public ParagraphImageSpan {
        Objects.requireNonNull(imageData, "imageData");
        alignment = alignment == null ? InlineImageAlignment.CENTER : alignment;
    }
}
