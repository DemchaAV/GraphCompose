package com.demcha.compose.document.api;

import com.demcha.compose.document.backend.fixed.pdf.PdfFixedLayoutBackend;
import com.demcha.compose.document.backend.fixed.pdf.PdfOutputOptionsTranslator;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterZone;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfMetadataOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfProtectionOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkOptions;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.output.DocumentOutputOptions;
import com.demcha.compose.document.output.DocumentProtection;
import com.demcha.compose.document.output.DocumentWatermark;

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
 * It owns three responsibilities:</p>
 *
 * <ol>
 *     <li>persisting the canonical, backend-neutral chrome values</li>
 *     <li>snapshotting them into an immutable {@link DocumentOutputOptions}
 *         passed to semantic export backends</li>
 *     <li>translating them into the PDF backend's option types when the
 *         convenience PDF entrypoints assemble a {@link PdfFixedLayoutBackend}</li>
 * </ol>
 *
 * <p>Instances are not thread-safe; the owning {@link DocumentSession}
 * already documents that contract for the public API.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocumentChromeOptions {
    private DocumentMetadata metadata;
    private DocumentWatermark watermark;
    private DocumentProtection protection;
    private final List<DocumentHeaderFooter> headersAndFooters = new ArrayList<>();

    DocumentChromeOptions() {
    }

    void setMetadata(DocumentMetadata metadata) {
        this.metadata = metadata;
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
     * Indicates whether at least one chrome option is configured.
     *
     * @return {@code true} when the session has metadata / watermark /
     *         protection / repeating chrome attached
     */
    boolean isEmpty() {
        return metadata == null
                && watermark == null
                && protection == null
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
        return new DocumentOutputOptions(metadata, watermark, protection, List.copyOf(headersAndFooters));
    }

    /**
     * Builds a configured {@link PdfFixedLayoutBackend} for the session's
     * convenience PDF methods. When {@code guideLines} is {@code false} and
     * no chrome is attached, returns the bare default backend so callers do
     * not pay for empty option arrays.
     *
     * @param guideLines whether the convenience PDF backend should draw
     *                   guide-line overlays
     * @return ready-to-use PDF backend
     */
    PdfFixedLayoutBackend toConveniencePdfBackend(boolean guideLines) {
        if (!guideLines && isEmpty()) {
            return new PdfFixedLayoutBackend();
        }
        PdfFixedLayoutBackend.Builder builder = PdfFixedLayoutBackend.builder()
                .guideLines(guideLines)
                .metadata(PdfOutputOptionsTranslator.toPdf(metadata))
                .watermark(PdfOutputOptionsTranslator.toPdf(watermark))
                .protect(PdfOutputOptionsTranslator.toPdf(protection));
        for (DocumentHeaderFooter entry : headersAndFooters) {
            PdfHeaderFooterOptions translated = PdfOutputOptionsTranslator.toPdf(entry);
            if (entry.getZone() == DocumentHeaderFooterZone.FOOTER) {
                builder.footer(translated);
            } else {
                builder.header(translated);
            }
        }
        return builder.build();
    }

    // PDF-flavoured compatibility setters -----------------------------------

    void setMetadata(PdfMetadataOptions options) {
        setMetadata(options == null ? null : DocumentMetadata.builder()
                .title(options.getTitle())
                .author(options.getAuthor())
                .subject(options.getSubject())
                .keywords(options.getKeywords())
                .creator(options.getCreator())
                .producer(options.getProducer())
                .build());
    }

    void setWatermark(PdfWatermarkOptions options) {
        setWatermark(options == null ? null : PdfOutputOptionsToCanonical.toCanonical(options));
    }

    void setProtection(PdfProtectionOptions options) {
        setProtection(options == null ? null : PdfOutputOptionsToCanonical.toCanonical(options));
    }

    void addHeader(PdfHeaderFooterOptions options) {
        Objects.requireNonNull(options, "options");
        addHeader(PdfOutputOptionsToCanonical.toCanonical(options.withZone(PdfHeaderFooterZone.HEADER)));
    }

    void addFooter(PdfHeaderFooterOptions options) {
        Objects.requireNonNull(options, "options");
        addFooter(PdfOutputOptionsToCanonical.toCanonical(options.withZone(PdfHeaderFooterZone.FOOTER)));
    }
}
