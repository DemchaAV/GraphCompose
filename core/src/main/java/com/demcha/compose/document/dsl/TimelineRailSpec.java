package com.demcha.compose.document.dsl;

import com.demcha.compose.document.style.DocumentColor;

/**
 * The rail, described rather than drawn.
 *
 * <p>Today a timeline's rail is a left border on every entry section, which is why it is
 * described by nothing more than a colour and a width. Naming it separately is what lets
 * that change later without every caller of the timeline layout learning about it.</p>
 *
 * @param color rail colour
 * @param width rail width in points
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
record TimelineRailSpec(DocumentColor color, double width) {
}
