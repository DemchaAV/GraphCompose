/**
 * Superseded Gen-2 CV layouts — slot frames that arrange a header plus
 * named slot content into a final document tree.
 *
 * <p><strong>Deprecated surface.</strong> These are the older Gen-2 CV
 * layouts. They are <em>not</em> the current standard. The current standard
 * is the layered surface
 * {@code com.demcha.compose.document.templates.cv.v2} (data / theme /
 * components / widgets / presets). This package is kept only for backward
 * compatibility and is scheduled for removal in a future major.</p>
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
 * <p>New code should target the layered {@code cv.v2} surface instead. See
 * {@code docs/templates/v2-layered/}.</p>
 *
 * @since 1.6.0
 */
@Deprecated(since = "1.7.0", forRemoval = true)
package com.demcha.compose.document.templates.cv.layouts;
