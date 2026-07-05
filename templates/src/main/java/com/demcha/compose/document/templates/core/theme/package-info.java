/**
 * <h2>Layer 3 — cosmetic tokens (theme)</h2>
 *
 * <p>The <strong>shift-able</strong> layer. Every colour, font, size,
 * padding, corner radius, accent width — everything purely visual —
 * lives in {@link com.demcha.compose.document.templates.core.theme.BrandTheme}
 * and its four sub-records:</p>
 *
 * <ul>
 *   <li>{@link com.demcha.compose.document.templates.core.theme.Palette}
 *       — colours.</li>
 *   <li>{@link com.demcha.compose.document.templates.core.theme.Typography}
 *       — fonts, sizes, line spacing.</li>
 *   <li>{@link com.demcha.compose.document.templates.core.theme.Spacing}
 *       — paddings, margins, banner radius, accent widths, row
 *       weights.</li>
 *   <li>{@link com.demcha.compose.document.templates.core.theme.Decoration}
 *       — bullets, contact separators, and other small glyph
 *       choices.</li>
 * </ul>
 *
 * <p>Template renderers and widgets accept a {@code BrandTheme}
 * argument and never read constants directly — so a new visual flavour
 * is just a new {@code BrandTheme} factory, no renderer changes
 * required.</p>
 *
 * <p>Why split into sub-records: it lets you mix-and-match — a
 * preset can build {@code new BrandTheme(palette, defaultTypography,
 * tighterSpacing)} for a compact variant without redeclaring every
 * field.</p>
 */
package com.demcha.compose.document.templates.core.theme;
