package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.engine.components.content.shape.Stroke;

import java.awt.Color;
import java.util.Objects;

/**
 * Measured inline shape span inside a paragraph line.
 *
 * <p>The semantic {@code InlineShapeRun} is resolved into this payload during
 * wrapping: the DSL fill color becomes an AWT {@link Color} and the DSL stroke
 * becomes an engine {@link Stroke}, while the {@link ShapeOutline} carries the
 * figure geometry (and its intrinsic {@link #width()} / {@link #height()}). The
 * PDF backend dispatches on the outline kind to paint the figure.</p>
 *
 * @param outline figure geometry; supplies the span width and height
 * @param fillColor optional resolved fill color
 * @param stroke optional resolved outline stroke
 * @param alignment vertical alignment relative to the surrounding text
 * @param baselineOffset extra vertical offset in points; positive moves up
 * @param linkOptions optional link metadata
 */
public record ParagraphShapeSpan(
        ShapeOutline outline,
        Color fillColor,
        Stroke stroke,
        InlineImageAlignment alignment,
        double baselineOffset,
        DocumentLinkOptions linkOptions
) implements ParagraphSpan {
    /**
     * Validates the outline and normalizes alignment defaults.
     */
    public ParagraphShapeSpan {
        Objects.requireNonNull(outline, "outline");
        alignment = alignment == null ? InlineImageAlignment.CENTER : alignment;
    }

    @Override
    public double width() {
        return outline.width();
    }

    @Override
    public double height() {
        return outline.height();
    }
}
