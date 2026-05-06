/**
 * Templates v2 reusable composition components — header, module, section, sidebar.
 *
 * <p>Components are the small reusable building blocks that Templates v2
 * presets stitch together to render a document. Each component has a small
 * set of style variants (factory methods like {@code Header.rightAligned()},
 * {@code Header.centered()}) plus configuration setters for typography,
 * palette, and spacing tokens.</p>
 *
 * <p>This package will be populated during Phase B of the Templates v2
 * migration with at least:</p>
 *
 * <ul>
 *   <li>{@code Header} — name, contact info, links — with style variants
 *       (right-aligned, centered, left-banner, monogram).</li>
 *   <li>{@code Module} — section title plus body — with style variants
 *       (heading-flat, heading-boxed, heading-underline).</li>
 *   <li>{@code Section} — group of modules sharing common styling.</li>
 *   <li>{@code Sidebar} — narrow column for two-column layouts.</li>
 * </ul>
 *
 * <p>Components are stateless after construction and produce
 * {@code DocumentNode} instances that the active layout assembles into a
 * final tree.</p>
 *
 * @since 1.6.0
 */
package com.demcha.compose.document.templates.components;
