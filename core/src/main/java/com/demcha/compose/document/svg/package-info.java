/**
 * SVG interoperability for the canonical document model.
 *
 * <p>The entry point is {@link com.demcha.compose.document.svg.SvgPath},
 * which parses SVG path data ({@code <path d="…">}) into the canonical
 * {@link com.demcha.compose.document.style.DocumentPathSegment} set —
 * normalized, y-flipped, and ready for
 * {@code PathBuilder.svg(...)} / {@code PathNode}. Curves render as native
 * PDF operators; nothing is tessellated.</p>
 *
 * @since 1.8.0
 */
package com.demcha.compose.document.svg;
