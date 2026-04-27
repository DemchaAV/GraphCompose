package com.demcha.compose.document.output;

import java.util.List;
import java.util.Objects;

/**
 * Aggregate of backend-neutral document output options.
 *
 * <p>Bundled into {@code SemanticExportContext} so semantic backends can apply
 * shared metadata, watermark, header/footer, and protection settings without
 * duplicating PDF-specific state. PDF and DOCX backends translate these values
 * into their native representations; backends that cannot honour a particular
 * option simply ignore it.</p>
 *
 * @param metadata document information (title, author, ...)
 * @param watermark optional document-wide watermark
 * @param protection optional access protection
 * @param headersAndFooters repeating header/footer entries
 *
 * @author Artem Demchyshyn
 */
public record DocumentOutputOptions(
        DocumentMetadata metadata,
        DocumentWatermark watermark,
        DocumentProtection protection,
        List<DocumentHeaderFooter> headersAndFooters
) {
    /** All-empty defaults. */
    public static final DocumentOutputOptions EMPTY = new DocumentOutputOptions(null, null, null, List.of());

    /**
     * Normalizes the headers-and-footers collection to an immutable snapshot.
     */
    public DocumentOutputOptions {
        headersAndFooters = headersAndFooters == null ? List.of() : List.copyOf(headersAndFooters);
        for (DocumentHeaderFooter entry : headersAndFooters) {
            Objects.requireNonNull(entry, "headersAndFooters entry");
        }
    }

    /**
     * Returns {@code true} when at least one option is configured.
     *
     * @return {@code true} for non-empty option bundles
     */
    public boolean hasAny() {
        return metadata != null || watermark != null || protection != null || !headersAndFooters.isEmpty();
    }
}
