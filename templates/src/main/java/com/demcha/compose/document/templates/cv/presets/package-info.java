/**
 * <h2>Layer 4 — preset composition</h2>
 *
 * <p>A <strong>preset</strong> in v2 is just three things glued
 * together:</p>
 *
 * <ol>
 *   <li>A {@link com.demcha.compose.document.templates.cv.data.CvDocument}
 *       supplied by the caller (the data).</li>
 *   <li>A {@link com.demcha.compose.document.templates.core.theme.BrandTheme}
 *       picked at construction (the cosmetics).</li>
 *   <li>A loop over the document's sections that calls renderers
 *       from {@code cv/components}.</li>
 * </ol>
 *
 * <p>That's it. No parsing, no palette literals, no per-section
 * branching outside the dispatcher. A new visual flavour is a new
 * theme factory in {@code cv/theme}; a new structural section is
 * a new sealed subtype in {@code cv/data} plus a renderer in
 * {@code cv/components}; a new preset is this file copied and
 * fed a different theme.</p>
 */
package com.demcha.compose.document.templates.cv.presets;
