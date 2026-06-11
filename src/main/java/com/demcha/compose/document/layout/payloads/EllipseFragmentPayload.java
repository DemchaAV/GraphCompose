package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.engine.components.content.shape.Stroke;

import java.awt.*;

/**
 * PDF payload for a resolved ellipse or circle fragment.
 *
 * @param fillColor       optional fill color
 * @param stroke          optional stroke
 * @param linkOptions     optional fragment-level link metadata
 * @param bookmarkOptions optional fragment-level bookmark metadata
 */
public record EllipseFragmentPayload(
        Color fillColor,
        Stroke stroke,
        DocumentLinkOptions linkOptions,
        DocumentBookmarkOptions bookmarkOptions
) implements PdfSemanticFragmentPayload {
}
