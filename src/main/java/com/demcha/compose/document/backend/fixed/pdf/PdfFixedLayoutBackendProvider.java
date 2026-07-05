package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.backend.fixed.FixedLayoutBackendProvider;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderer;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.output.DocumentDebugOptions;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentOutputOptions;

/**
 * PDFBox-backed {@link FixedLayoutBackendProvider}, registered through
 * {@code META-INF/services} so the document API can render, rasterize, and
 * concatenate documents without importing the PDF backend.
 *
 * <p>{@link #create} translates the backend-neutral chrome into PDF option types
 * and assembles a configured {@link PdfFixedLayoutBackend} — the translation that
 * previously lived in the session's chrome holder.</p>
 *
 * @since 2.0.0
 */
public final class PdfFixedLayoutBackendProvider implements FixedLayoutBackendProvider {

    /**
     * Creates the provider. Invoked reflectively by {@link java.util.ServiceLoader}.
     */
    public PdfFixedLayoutBackendProvider() {
    }

    @Override
    public String format() {
        return "pdf";
    }

    @Override
    public FixedLayoutRenderer create(DocumentOutputOptions chrome, DocumentDebugOptions debug) {
        if (!debug.enabled() && !chrome.hasAny()) {
            return new PdfFixedLayoutBackend();
        }
        PdfFixedLayoutBackend.Builder builder = PdfFixedLayoutBackend.builder()
                .debug(debug)
                .metadata(PdfOutputOptionsTranslator.toPdf(chrome.metadata()))
                .watermark(PdfOutputOptionsTranslator.toPdf(chrome.watermark()))
                .protect(PdfOutputOptionsTranslator.toPdf(chrome.protection()))
                .viewerPreferences(PdfOutputOptionsTranslator.toPdf(chrome.viewerPreferences()));
        for (DocumentHeaderFooter entry : chrome.headersAndFooters()) {
            PdfHeaderFooterOptions translated = PdfOutputOptionsTranslator.toPdf(entry);
            if (entry.getZone() == DocumentHeaderFooterZone.FOOTER) {
                builder.footer(translated);
            } else {
                builder.header(translated);
            }
        }
        return builder.build();
    }
}
