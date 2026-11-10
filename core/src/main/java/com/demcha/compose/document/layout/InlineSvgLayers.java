package com.demcha.compose.document.layout;

import com.demcha.compose.document.layout.payloads.ResolvedSvgLayer;
import com.demcha.compose.document.svg.SvgIcon;

import java.util.ArrayList;
import java.util.List;

/**
 * An inline SVG icon's layers as the layout resolves them for a line of text.
 *
 * <p>The layout lowers an icon drawn in a line to {@link ResolvedSvgLayer}s — solid paints
 * flattened, stroke and dash lengths scaled from the icon's own units to points, the clip
 * carried — and hands them to the fixed-layout backends in a paragraph's fragment. A backend
 * that writes from the document itself, as the Word export does, has the icon and not the
 * fragment; this gives it the same layers from the same code, so the two cannot resolve one
 * icon differently.</p>
 */
public final class InlineSvgLayers {

    private InlineSvgLayers() {
    }

    /**
     * Resolves an icon drawn at {@code width} points wide.
     *
     * @param icon  the icon
     * @param width the width it is drawn at, in points
     * @return its layers, geometry normalised to the unit box and strokes in points
     */
    public static List<ResolvedSvgLayer> of(SvgIcon icon, double width) {
        // Geometry (and the clip region) stay normalised to the unit box and scale at render;
        // the stroke width and dash lengths are in SVG user units, so they are scaled to
        // points here (scale = target width / source frame width) — the same arithmetic
        // SvgIcon.node(double) does, but carrying the clip through.
        double scale = width / icon.sourceWidth();
        List<ResolvedSvgLayer> resolved = new ArrayList<>(icon.layers().size());
        for (SvgIcon.Layer layer : icon.layers()) {
            resolved.add(InlineSvgToken.toResolvedSvgLayer(layer, scale));
        }
        return List.copyOf(resolved);
    }
}
