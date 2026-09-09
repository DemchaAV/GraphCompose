package com.demcha.compose.document.dsl;

import com.demcha.compose.document.style.DocumentStroke;

/**
 * The rail, described rather than drawn.
 *
 * <p>One stroke, whichever way the caller asked for it: {@code connector(colour, width)}
 * and {@code rail(r -> r.stroke(...))} both normalize here, so there is one rail
 * configuration for the layout to read rather than an old shape and a new one.</p>
 *
 * <p>A stroke is all it takes to describe, because the rail's geometry is not here: how far
 * it runs is a {@link TimelineRailExtent} and where it runs comes from the marker anchor,
 * both resolved after layout. This is only what it is drawn with.</p>
 *
 * @param stroke the rail's colour and width
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
record TimelineRailSpec(DocumentStroke stroke) {
}
