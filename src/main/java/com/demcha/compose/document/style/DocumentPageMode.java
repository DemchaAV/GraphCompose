package com.demcha.compose.document.style;

/**
 * How a PDF reader should present the document when it first opens — the page
 * mode written to the document catalog.
 *
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public enum DocumentPageMode {
    /** Neither the outline nor thumbnails panel is shown (the default). */
    USE_NONE,
    /** Open with the bookmark (outline) panel visible. */
    USE_OUTLINES,
    /** Open with the page-thumbnail panel visible. */
    USE_THUMBNAILS,
    /** Open in full-screen mode, with no menu bar, window controls, or panels. */
    FULL_SCREEN,
    /** Open with the attachments panel visible. */
    USE_ATTACHMENTS
}
