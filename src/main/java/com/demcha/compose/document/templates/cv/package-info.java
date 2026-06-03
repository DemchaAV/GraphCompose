/**
 * Superseded Gen-2 CV domain — slot-based layouts, presets, builder, and
 * spec data types.
 *
 * <p><strong>Deprecated surface.</strong> This package is the older Gen-2
 * CV (résumé) template stack. It is <em>not</em> the current standard. The
 * current standard is the layered surface
 * {@code com.demcha.compose.document.templates.cv.v2} (data / theme /
 * components / widgets / presets). This package is kept only for backward
 * compatibility and is scheduled for removal in a future major.</p>
 *
 * <p>Sub-packages partition the (deprecated) domain by concern:</p>
 *
 * <ul>
 *   <li>{@code cv.layouts} — slot frames (single-column, two-column-sidebar,
 *       three-column-magazine).</li>
 *   <li>{@code cv.presets} — flat copy-and-tweak preset classes
 *       (ModernProfessional, NordicClean, ClassicSerif, CompactMono,
 *       Executive, EngineeringResume, Panel, SidebarPortrait, MonogramSidebar,
 *       TimelineMinimal, BoxedSections, CenteredHeadline, BlueBanner,
 *       EditorialBlue).</li>
 *   <li>{@code cv.builder} — {@code CvBuilder} for users composing
 *       their own preset.</li>
 *   <li>{@code cv.spec} — data records ({@code CvSpec}, {@code CvHeader},
 *       {@code CvModule}) describing the user's CV content.</li>
 * </ul>
 *
 * <p>New code should target the layered {@code cv.v2} surface instead. See
 * {@code docs/templates/v2-layered/}.</p>
 *
 * @since 1.6.0
 */
@Deprecated(since = "1.7.0", forRemoval = true)
package com.demcha.compose.document.templates.cv;
