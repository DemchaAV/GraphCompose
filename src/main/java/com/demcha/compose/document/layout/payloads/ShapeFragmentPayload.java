package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.engine.components.content.shape.Stroke;

import java.awt.Color;

/**
 * PDF payload for a resolved shape fragment.
 *
 * <p>The {@code cornerRadius} carries the four per-corner radii so
 * authors can round, e.g., only the right side of a card while
 * leaving the left edge square. Single-radius callers continue to
 * work via the legacy double-precision constructor.</p>
 *
 * @param fillColor optional shape fill color
 * @param stroke optional shape stroke
 * @param cornerRadius per-corner radii in points
 * @param linkOptions optional fragment-level link metadata
 * @param bookmarkOptions optional fragment-level bookmark metadata
 * @param sideBorders optional per-side border strokes
 */
public record ShapeFragmentPayload(
        Color fillColor,
        Stroke stroke,
        DocumentCornerRadius cornerRadius,
        DocumentLinkOptions linkOptions,
        DocumentBookmarkOptions bookmarkOptions,
        SideBorders sideBorders
) implements PdfSemanticFragmentPayload {
    /**
     * Normalizes the render-only corner radius.
     */
    public ShapeFragmentPayload {
        if (cornerRadius == null) {
            cornerRadius = DocumentCornerRadius.ZERO;
        }
    }

    /**
     * Backwards-compatible constructor that accepts a single uniform
     * radius (pre-Phase E.1.1 wiring) and applies it to every corner.
     *
     * @param fillColor optional shape fill color
     * @param stroke optional shape stroke
     * @param cornerRadius uniform corner radius in points
     * @param linkOptions optional fragment-level link metadata
     * @param bookmarkOptions optional fragment-level bookmark metadata
     * @param sideBorders optional per-side border strokes
     */
    public ShapeFragmentPayload(Color fillColor,
                                Stroke stroke,
                                double cornerRadius,
                                DocumentLinkOptions linkOptions,
                                DocumentBookmarkOptions bookmarkOptions,
                                SideBorders sideBorders) {
        this(fillColor, stroke,
                cornerRadius < 0 || Double.isNaN(cornerRadius) || Double.isInfinite(cornerRadius)
                        ? DocumentCornerRadius.ZERO
                        : DocumentCornerRadius.of(cornerRadius),
                linkOptions, bookmarkOptions, sideBorders);
    }

    /**
     * Backwards-compatible constructor without per-side borders.
     *
     * @param fillColor optional shape fill color
     * @param stroke optional shape stroke
     * @param cornerRadius uniform corner radius in points
     * @param linkOptions optional fragment-level link metadata
     * @param bookmarkOptions optional fragment-level bookmark metadata
     */
    public ShapeFragmentPayload(Color fillColor,
                                Stroke stroke,
                                double cornerRadius,
                                DocumentLinkOptions linkOptions,
                                DocumentBookmarkOptions bookmarkOptions) {
        this(fillColor, stroke, cornerRadius, linkOptions, bookmarkOptions, null);
    }
}
