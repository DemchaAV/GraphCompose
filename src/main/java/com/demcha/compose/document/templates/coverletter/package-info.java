/**
 * Templates v2 cover-letter domain — layouts, presets, builder, and spec data types.
 *
 * <p>This package is the home of all cover-letter templates in the v2
 * architecture. The user requirement is one cover-letter preset paired
 * with each CV preset (same Header / Typography / Palette), so writers
 * can ship a CV and a matching cover letter with consistent visual
 * identity.</p>
 *
 * <p>Sub-packages partition the domain by concern:</p>
 *
 * <ul>
 *   <li>{@code coverletter.layouts} — slot каркасы (LetterFormat — a
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
 * <p>Sub-packages will be populated during Phase E of the Templates v2
 * migration.</p>
 *
 * <p><strong>Naming note:</strong> the user-facing concept is
 * "cover-letter" with a hyphen, but Java packages cannot contain hyphens.
 * The package name {@code coverletter} drops the hyphen for compatibility;
 * file id strings and example file names retain the hyphenated form
 * (e.g. {@code cover-letter-modern-professional.pdf}).</p>
 *
 * @since 1.6.0
 */
package com.demcha.compose.document.templates.coverletter;
