package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.style.ShapePoint;
import com.demcha.compose.engine.components.content.shape.Stroke;

import java.awt.*;
import java.util.List;
import java.util.Objects;

/**
 * PDF payload for a resolved polygon fragment (diamond, triangle, star or any
 * vertex ring). The normalized vertices are scaled to the placed fragment's
 * size by the render handler.
 *
 * @param points          normalized vertex ring (at least three), in draw order
 * @param fillColor       optional fill color
 * @param stroke          optional stroke
 * @param linkOptions     optional fragment-level link metadata
 * @param bookmarkOptions optional fragment-level bookmark metadata
 */
public record PolygonFragmentPayload(
        List<ShapePoint> points,
        Color fillColor,
        Stroke stroke,
        DocumentLinkOptions linkOptions,
        DocumentBookmarkOptions bookmarkOptions
) implements PdfSemanticFragmentPayload {
    /**
     * Copies the vertex ring defensively.
     */
    public PolygonFragmentPayload {
        Objects.requireNonNull(points, "points");
        points = List.copyOf(points);
    }
}
