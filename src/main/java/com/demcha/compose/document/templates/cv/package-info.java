/**
 * Templates v2 CV domain — layouts, presets, builder, and spec data types.
 *
 * <p>This package is the home of all CV (résumé) templates in the v2
 * architecture. Sub-packages partition the domain by concern:</p>
 *
 * <ul>
 *   <li>{@code cv.layouts} — slot caркасы (single-column, two-column-sidebar,
 *       three-column-magazine, hero-and-two-column).</li>
 *   <li>{@code cv.presets} — flat copy-and-tweak preset classes
 *       (ModernProfessional, NordicClean, ClassicSerif, CompactMono,
 *       Executive, EngineeringResume, Panel, Sidebar, MonogramSidebar,
 *       TimelineMinimal, BoxedSections, CenteredHeadline, BlueBanner,
 *       EditorialBlue).</li>
 *   <li>{@code cv.builder} — {@code CvBuilder} for users composing
 *       their own preset.</li>
 *   <li>{@code cv.spec} — data records ({@code CvSpec}, {@code CvHeader},
 *       {@code CvModule}) describing the user's CV content.</li>
 * </ul>
 *
 * <p>Sub-packages will be populated during Phases B–E of the Templates v2
 * migration. Top-level marker file lives here to register the package
 * with the build.</p>
 *
 * @since 1.6.0
 */
package com.demcha.compose.document.templates.cv;
