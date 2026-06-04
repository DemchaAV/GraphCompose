package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.engine.components.content.shape.Stroke;

import java.awt.Color;

/**
 * One resolved paint layer of a {@link ParagraphShapeSpan}: an outline figure
 * whose fill colour and stroke are already resolved to AWT / engine primitives,
 * ready for the PDF backend.
 *
 * @param outline figure geometry
 * @param fillColor optional resolved fill color
 * @param stroke optional resolved outline stroke
 */
public record ResolvedShapeLayer(ShapeOutline outline, Color fillColor, Stroke stroke) {
}
