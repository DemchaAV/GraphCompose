/**
 * <h2>Layer 4 — preset composition</h2>
 *
 * <p>A <strong>preset</strong> in v2 is just three things glued
 * together:</p>
 *
 * <ol>
 *   <li>A {@link com.demcha.compose.document.templates.cv.v2.data.CvDocument}
 *       supplied by the caller (the data).</li>
 *   <li>A {@link com.demcha.compose.document.templates.cv.v2.theme.CvTheme}
 *       picked at construction (the cosmetics).</li>
 *   <li>A loop over the document's sections that calls renderers
 *       from {@code cv/v2/components}.</li>
 * </ol>
 *
 * <p>That's it. No parsing, no palette literals, no per-section
 * branching outside the dispatcher. A new visual flavour is a new
 * theme factory in {@code cv/v2/theme}; a new structural section is
 * a new sealed subtype in {@code cv/v2/data} plus a renderer in
 * {@code cv/v2/components}; a new preset is this file copied and
 * fed a different theme.</p>
 */
package com.demcha.compose.document.templates.cv.v2.presets;
