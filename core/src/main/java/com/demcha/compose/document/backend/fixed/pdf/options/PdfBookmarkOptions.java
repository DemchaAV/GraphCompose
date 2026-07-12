package com.demcha.compose.document.backend.fixed.pdf.options;

import java.util.Objects;

/**
 * Canonical PDF outline entry metadata attached to semantic content.
 *
 * <p>Bookmark options are content-scoped and are carried through the resolved
 * semantic graph into the canonical PDF backend.</p>
 *
 * @param title display label shown in the PDF outline panel
 * @param level nesting level where {@code 0} is a root bookmark
 */
public record PdfBookmarkOptions(String title, int level) {
    /**
     * Creates a root-level bookmark entry.
     *
     * @param title display label shown in the PDF outline panel
     */
    public PdfBookmarkOptions(String title) {
        this(title, 0);
    }

    /**
     * Canonical compact constructor.
     *
     * @param title display label shown in the PDF outline panel
     * @param level nesting level where {@code 0} is a root bookmark
     */
    public PdfBookmarkOptions {
        title = Objects.requireNonNullElse(title, "").trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Bookmark title must not be blank.");
        }
        level = Math.max(0, level);
    }
}
