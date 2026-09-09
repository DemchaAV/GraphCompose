package com.demcha.compose.document.dsl;

import com.demcha.compose.document.style.DocumentStroke;

/**
 * The rail, described rather than drawn.
 *
 * <p>One stroke, whichever way the caller asked for it: {@code connector(colour, width)}
 * and {@code rail(r -> r.stroke(...))} both normalize here, so there is one rail
 * configuration for the layout to read rather than an old shape and a new one.</p>
 *
 * <p>Today that rail is still a left border on every entry section, which is why a stroke
 * is all it takes to describe. Naming it separately is what lets that change without every
 * caller of the timeline layout learning about it.</p>
 *
 * @param stroke the rail's colour and width
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
record TimelineRailSpec(DocumentStroke stroke) {
}
