package com.demcha.compose.document.templates.cv.v2.data;

/**
 * Logical placement region inside a {@link CvDocument}. Presets read
 * the slot of each section to decide where on the page it should
 * appear — main column, sidebar, or footer.
 *
 * <p>Single-column presets like
 * {@code com.demcha.compose.document.templates.cv.v2.presets.BoxedSections}
 * iterate only {@link #MAIN} sections; sections placed in
 * {@link #SIDEBAR} or {@link #FOOTER} are silently dropped by such
 * presets. Multi-column presets (two-column sidebar, magazine
 * layouts) iterate {@link #MAIN} and {@link #SIDEBAR} separately to
 * fill their columns.</p>
 *
 * <p>Sections that don't specify a slot at build time default to
 * {@link #MAIN} — so single-column callers keep working without
 * changes after the slot model was introduced.</p>
 */
public enum Slot {

    /**
     * Primary content column. Default when no slot is specified.
     */
    MAIN,

    /**
     * Sidebar / secondary column — typically narrower than MAIN.
     */
    SIDEBAR,

    /**
     * Footer area below the main flow — useful for references,
     * disclaimers, or page-bottom notes. Currently only rendered by
     * presets that explicitly opt in.
     */
    FOOTER
}
