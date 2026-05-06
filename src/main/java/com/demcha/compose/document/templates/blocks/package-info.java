/**
 * Templates v2 body block kinds — paragraph, list, key-value, table, custom.
 *
 * <p>Block kinds describe what content appears inside a module body. A
 * preset declares a block kind once (e.g. {@code Module.bulletList(items)})
 * and the renderer expands it according to the active theme and spacing
 * tokens.</p>
 *
 * <p>This package will be populated during Phase B of the Templates v2
 * migration with at least:</p>
 *
 * <ul>
 *   <li>{@code ParagraphBlock} — single paragraph of body text.</li>
 *   <li>{@code BulletListBlock} — bullet-pointed items.</li>
 *   <li>{@code NumberedListBlock} — numbered items.</li>
 *   <li>{@code IndentedBlock} — items with indented continuation
 *       (typical for education / projects entries with a bold key
 *       and indented body).</li>
 *   <li>{@code KeyValueBlock} — bold key + value on the same line
 *       (typical for "Languages: ..." rows).</li>
 *   <li>{@code MultiParagraphBlock} — several paragraphs with
 *       paragraph-spacing between.</li>
 * </ul>
 *
 * @since 1.6.0
 */
package com.demcha.compose.document.templates.blocks;
