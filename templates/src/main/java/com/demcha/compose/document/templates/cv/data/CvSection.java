package com.demcha.compose.document.templates.cv.data;

/**
 * Sealed top of the v2 CV section hierarchy. Each concrete subtype
 * captures a <strong>structurally distinct</strong> body shape — not
 * a visual flavour. Visual flavours like "bulleted vs plain" are
 * style toggles inside the section, not separate types.
 *
 * <p>The built-in subtypes cover the canonical CV layout shapes:</p>
 *
 * <ul>
 *   <li>{@link ParagraphSection} — one block of prose. One field.</li>
 *   <li>{@link RowsSection} — list of two-field {@link CvRow} items
 *       plus a {@link RowStyle} decoration enum (plain / bulleted /
 *       bulleted-stacked).</li>
 *   <li>{@link EntriesSection} — list of timeline {@link CvEntry}
 *       items with four fields (title, subtitle, date, body).</li>
 *   <li>{@link SkillsSection} — grouped skill categories where each
 *       category owns an ordered list of skill labels.</li>
 *   <li>{@link ModuleSection} — a section whose shape is a value
 *       rather than a type: one {@link CvItem} record plus a
 *       {@link CvKind} chosen at runtime. The four above stay the
 *       natural choice for a CV written in Java, where the author
 *       picks the record and the compiler checks it; this one is for a
 *       CV assembled from data, where the shape is not known until it
 *       arrives. It renders through the same components, so both
 *       routes lay out the same content identically.</li>
 * </ul>
 *
 * <p>Every implementation carries a {@code title} — the banner text
 * the renderer wraps in a styled panel above the section body.</p>
 */
public sealed interface CvSection
        permits ParagraphSection, RowsSection, EntriesSection, SkillsSection, ModuleSection {

    /**
     * Banner heading shown above this section's body.
     *
     * @return banner heading shown above the body (non-blank by
     * construction)
     */
    String title();
}
