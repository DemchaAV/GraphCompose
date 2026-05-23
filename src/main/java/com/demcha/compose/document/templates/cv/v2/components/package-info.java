/**
 * <h2>Layer 2 — reusable renderers</h2>
 *
 * <p>Every class here is a static helper that takes a host
 * {@link com.demcha.compose.document.dsl.SectionBuilder}, a
 * {@code cv/v2/data} record, and a
 * {@link com.demcha.compose.document.templates.cv.v2.theme.CvTheme},
 * and draws the data into the host using the theme's tokens.</p>
 *
 * <p>Components <strong>never</strong>:</p>
 * <ul>
 *   <li>store theme as a static field — theme is always an argument,
 *       so the same component renders any theme without reflection
 *       or singletons;</li>
 *   <li>parse data — that is the spec's job;</li>
 *   <li>read magic numbers — every value reads from the theme.</li>
 * </ul>
 *
 * <p>The dispatch hub is
 * {@link com.demcha.compose.document.templates.cv.v2.components.SectionDispatcher}
 * — it pattern-matches on the sealed
 * {@link com.demcha.compose.document.templates.cv.v2.data.CvSection}
 * subtype and delegates to one of the row / entry / paragraph
 * renderers.</p>
 */
package com.demcha.compose.document.templates.cv.v2.components;
