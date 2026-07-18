package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.backend.fixed.FixedLayoutBackendProvider;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderer;
import com.demcha.compose.document.output.DocumentDebugOptions;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentOutputOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * POI-backed {@link FixedLayoutBackendProvider} for the {@code "pptx"} format,
 * registered through {@code META-INF/services} so the document API can resolve
 * the PPTX backend by format without importing it.
 *
 * <p>{@link #create} translates the session's backend-neutral chrome onto the
 * {@link PptxFixedLayoutBackend} builder: metadata, watermark, and repeating
 * headers/footers apply natively. Protection and PDF viewer preferences have
 * no .pptx counterpart and are ignored with a one-time warning, per the
 * {@link DocumentOutputOptions} contract; debug overlays are PDF-only.</p>
 *
 * @since 2.1.0
 */
public final class PptxFixedLayoutBackendProvider implements FixedLayoutBackendProvider {

    private static final Logger LOG = LoggerFactory.getLogger("com.demcha.compose.engine.render");
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    /**
     * Creates the provider. Invoked reflectively by {@link java.util.ServiceLoader}.
     */
    public PptxFixedLayoutBackendProvider() {
    }

    @Override
    public String format() {
        return "pptx";
    }

    @Override
    public FixedLayoutRenderer create(DocumentOutputOptions chrome, DocumentDebugOptions debug) {
        if (debug.enabled()) {
            warnOnce("debug", "debug overlays (guides, node labels) are not implemented for "
                    + "the PPTX backend and are skipped; render through the PDF backend to see them");
        }
        if (chrome.protection() != null) {
            warnOnce("protection", "document protection has no .pptx counterpart and is "
                    + "skipped — OOXML presentations carry no PDF-style permission model");
        }
        if (chrome.viewerPreferences() != null) {
            warnOnce("viewer-preferences", "PDF viewer preferences have no .pptx counterpart "
                    + "and are skipped");
        }
        if (!chrome.hasAny()) {
            return new PptxFixedLayoutBackend();
        }
        PptxFixedLayoutBackend.Builder builder = PptxFixedLayoutBackend.builder()
                .metadata(chrome.metadata())
                .watermark(chrome.watermark());
        for (DocumentHeaderFooter entry : chrome.headersAndFooters()) {
            if (entry.getZone() == DocumentHeaderFooterZone.FOOTER) {
                builder.footer(entry);
            } else {
                builder.header(entry);
            }
        }
        return builder.build();
    }

    private static void warnOnce(String key, String message) {
        if (WARNED.add(key)) {
            LOG.warn("render.pptx.capability {}", message);
        }
    }
}
