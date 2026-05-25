/**
 * <h2>Layer 3 — cosmetic tokens (theme)</h2>
 *
 * <p>The <strong>shift-able</strong> layer. Every colour, font, size,
 * padding, corner radius, accent width — everything purely visual —
 * lives in {@link com.demcha.compose.document.templates.cv.v2.theme.CvTheme}
 * and its four sub-records:</p>
 *
 * <ul>
 *   <li>{@link com.demcha.compose.document.templates.cv.v2.theme.CvPalette}
 *       — colours.</li>
 *   <li>{@link com.demcha.compose.document.templates.cv.v2.theme.CvTypography}
 *       — fonts, sizes, line spacing.</li>
 *   <li>{@link com.demcha.compose.document.templates.cv.v2.theme.CvSpacing}
 *       — paddings, margins, banner radius, accent widths, row
 *       weights.</li>
 *   <li>{@link com.demcha.compose.document.templates.cv.v2.theme.CvDecoration}
 *       — bullets, contact separators, and other small glyph
 *       choices.</li>
 * </ul>
 *
 * <p>Renderers in {@code cv/v2/components} accept a {@code CvTheme}
 * argument and never read constants directly — so a new visual flavour
 * is just a new {@code CvTheme} factory, no renderer changes
 * required.</p>
 *
 * <p>Why split into sub-records: it lets you mix-and-match — a
 * preset can build {@code new CvTheme(palette, defaultTypography,
 * tighterSpacing)} for a compact variant without redeclaring every
 * field.</p>
 */
package com.demcha.compose.document.templates.cv.v2.theme;
