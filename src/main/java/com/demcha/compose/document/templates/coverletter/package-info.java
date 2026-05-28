/**
 * Superseded Gen-2 cover-letter domain — layout, presets, builder, and spec
 * data types.
 *
 * <p><strong>Deprecated surface.</strong> This package is the older Gen-2
 * cover-letter template stack. It is <em>not</em> the current standard. The
 * current standard is the layered surface
 * {@code com.demcha.compose.document.templates.coverletter.v2} (data / theme /
 * components / widgets / presets). This package is kept only for backward
 * compatibility and is scheduled for removal in a future major.</p>
 *
 * <p>Sub-packages partition the (deprecated) domain by concern:</p>
 *
 * <ul>
 *   <li>{@code coverletter.layouts} — slot frames (LetterFormat — a
 *       single-column layout with generous side margins for letter body
 *       text).</li>
 *   <li>{@code coverletter.presets} — flat copy-and-tweak preset classes,
 *       one per CV preset (ModernProfessionalLetter, NordicCleanLetter,
 *       ClassicSerifLetter, CompactMonoLetter, ExecutiveLetter,
 *       EngineeringResumeLetter, PanelLetter, SidebarPortraitLetter,
 *       MonogramSidebarLetter, TimelineMinimalLetter, BoxedSectionsLetter,
 *       CenteredHeadlineLetter, BlueBannerLetter, EditorialBlueLetter).</li>
 *   <li>{@code coverletter.builder} — {@code CoverLetterBuilder} for
 *       users composing their own preset.</li>
 *   <li>{@code coverletter.spec} — data records ({@code CoverLetterSpec}
 *       with header, greeting, body paragraphs, closing).</li>
 * </ul>
 *
 * <p>New code should target the layered {@code coverletter.v2} surface
 * instead. See {@code docs/templates/v2-layered/}.</p>
 *
 * <p><strong>Naming note:</strong> the user-facing concept is
 * "cover-letter" with a hyphen, but Java packages cannot contain hyphens.
 * The package name {@code coverletter} drops the hyphen for compatibility;
 * file id strings and example file names retain the hyphenated form
 * (e.g. {@code cover-letter-modern-professional.pdf}).</p>
 *
 * @since 1.6.0
 * @deprecated Superseded by the layered
 *             {@code com.demcha.compose.document.templates.coverletter.v2}
 *             surface (the current standard). This Gen-2 package is kept for
 *             backward compatibility and will be removed in a future major.
 *             See {@code docs/templates/v2-layered/}.
 */
@Deprecated(since = "1.7.0", forRemoval = true)
package com.demcha.compose.document.templates.coverletter;
