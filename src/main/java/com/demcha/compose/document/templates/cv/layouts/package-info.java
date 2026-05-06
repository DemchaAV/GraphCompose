/**
 * Templates v2 CV layouts — slot caркасы that arrange a header plus
 * named slot content into a final document tree.
 *
 * <p>Layouts are pure structural composers. They expose a fixed set of
 * named slots ({@code "main"}, {@code "sidebar"}, {@code "col-1"} etc.)
 * and a single composition seam that takes a pre-rendered header node
 * plus a populated {@link com.demcha.compose.document.templates.api.SlotMap}
 * and returns the composed root {@code DocumentNode}.</p>
 *
 * <p>Concrete layouts in this package:</p>
 *
 * <ul>
 *   <li>{@link com.demcha.compose.document.templates.cv.layouts.SingleColumn}
 *       — one main slot, header on top.</li>
 *   <li>{@link com.demcha.compose.document.templates.cv.layouts.TwoColumnSidebar}
 *       — main + sidebar slots, weighted row beneath the header.</li>
 *   <li>{@link com.demcha.compose.document.templates.cv.layouts.ThreeColumnMagazine}
 *       — col-1 / col-2 / col-3 slots, weighted row beneath the header.</li>
 * </ul>
 *
 * <p>Additional layouts ({@code HeroAndTwoColumn}, etc.) will land
 * alongside the presets that need them in Phase E of the Templates v2
 * migration.</p>
 *
 * @since 1.6.0
 */
package com.demcha.compose.document.templates.cv.layouts;
