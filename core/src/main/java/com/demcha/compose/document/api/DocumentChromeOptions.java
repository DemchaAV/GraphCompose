package com.demcha.compose.document.api;

import com.demcha.compose.document.backend.fixed.BackendProviders;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderer;
import com.demcha.compose.document.output.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Mutable holder for document-level chrome options (metadata, watermark,
 * protection, repeating headers and footers) attached to a
 * {@link DocumentSession}.
 *
 * <p>The class is package-private and serves as a focused collaborator that
 * keeps the session's public facade free of chrome-specific assembly logic.
 * It owns two responsibilities:</p>
 *
 * <ol>
 *     <li>persisting the canonical, backend-neutral chrome values</li>
 *     <li>snapshotting them into an immutable {@link DocumentOutputOptions}
 *         passed to the output backends</li>
 * </ol>
 *
 * <p>Instances are not thread-safe; the owning {@link DocumentSession}
 * already documents that contract for the public API.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocumentChromeOptions {
    private final List<DocumentHeaderFooter> headersAndFooters = new ArrayList<>();
    private DocumentMetadata metadata;
    private DocumentWatermark watermark;
    private DocumentProtection protection;
    private DocumentViewerPreferences viewerPreferences;

    DocumentChromeOptions() {
    }

    void setMetadata(DocumentMetadata metadata) {
        this.metadata = metadata;
    }

    void setViewerPreferences(DocumentViewerPreferences viewerPreferences) {
        this.viewerPreferences = viewerPreferences;
    }

    void setWatermark(DocumentWatermark watermark) {
        this.watermark = watermark;
    }

    void setProtection(DocumentProtection protection) {
        this.protection = protection;
    }

    void addHeader(DocumentHeaderFooter header) {
        Objects.requireNonNull(header, "header");
        this.headersAndFooters.add(header.withZone(DocumentHeaderFooterZone.HEADER));
    }

    void addFooter(DocumentHeaderFooter footer) {
        Objects.requireNonNull(footer, "footer");
        this.headersAndFooters.add(footer.withZone(DocumentHeaderFooterZone.FOOTER));
    }

    void clearHeadersAndFooters() {
        this.headersAndFooters.clear();
    }

    /**
     * The height the given zone claims from the page's content area — the tallest
     * of the space-reserving zones registered for it, or zero when none reserves.
     *
     * <p>The tallest rather than the sum: zones in the same band are drawn on top
     * of one another, not stacked, so the band is as deep as its deepest member.</p>
     *
     * @param zone the band to measure
     * @return reserved height in points, never negative
     */
    double reservedHeight(DocumentHeaderFooterZone zone) {
        double reserved = 0.0;
        for (DocumentHeaderFooter entry : headersAndFooters) {
            if (entry.getZone() == zone && entry.isReserveSpace()) {
                reserved = Math.max(reserved, entry.getHeight());
            }
        }
        return reserved;
    }

    /**
     * Indicates whether at least one chrome option is configured.
     *
     * @return {@code true} when the session has metadata / watermark /
     * protection / repeating chrome attached
     */
    boolean isEmpty() {
        return metadata == null
               && watermark == null
               && protection == null
               && viewerPreferences == null
               && headersAndFooters.isEmpty();
    }

    /**
     * Snapshots the current state into an immutable bundle for semantic
     * backends.
     *
     * @return immutable output-option bundle
     */
    DocumentOutputOptions snapshot() {
        if (isEmpty()) {
            return DocumentOutputOptions.EMPTY;
        }
        return new DocumentOutputOptions(metadata, watermark, protection, viewerPreferences,
                List.copyOf(headersAndFooters));
    }

    /**
     * Resolves and configures the fixed-layout backend of the requested format
     * for the session's convenience output methods, translating the attached
     * chrome through the registered
     * {@link com.demcha.compose.document.backend.fixed.FixedLayoutBackendProvider}.
     *
     * @param format backend format key, e.g. {@code "pdf"} or {@code "pptx"}
     * @param debug  debug overlay options; never {@code null}
     * @return a configured renderer
     * @throws com.demcha.compose.document.exceptions.MissingBackendException
     *         if no provider for the format is on the classpath
     */
    FixedLayoutRenderer toConvenienceBackend(String format, DocumentDebugOptions debug) {
        return BackendProviders.fixedLayout(format).create(snapshot(), debug);
    }
}
