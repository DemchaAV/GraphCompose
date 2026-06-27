/**
 * <h2>Core — neutral text rendering (text)</h2>
 *
 * <p>Family-agnostic text primitives shared by every template family. They turn
 * text plus a {@link com.demcha.compose.document.style.DocumentTextStyle} (or a
 * theme-derived style) into DSL nodes, and depend on no document family's data
 * model — so invoice, proposal, CV, and cover-letter all build on the same text
 * layer.</p>
 *
 * <ul>
 *   <li>{@link com.demcha.compose.document.templates.core.text.MarkdownText}
 *       — parse inline {@code **bold**} / {@code *italic*} markdown into inline runs.</li>
 *   <li>{@link com.demcha.compose.document.templates.core.text.MarkdownInline}
 *       — append parsed inline markdown into a {@code RichText} run.</li>
 *   <li>{@link com.demcha.compose.document.templates.core.text.RichParagraphRenderer}
 *       — render a markdown paragraph into a section.</li>
 *   <li>{@link com.demcha.compose.document.templates.core.text.TextStyles}
 *       — {@code DocumentTextStyle} factory helpers.</li>
 *   <li>{@link com.demcha.compose.document.templates.core.text.TextOrnaments}
 *       — spaced-caps, pipe-joins, and other small string helpers.</li>
 * </ul>
 */
package com.demcha.compose.document.templates.core.text;
