package com.demcha.compose.document.backend.fixed.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Internal helper that resolves deferred in-document links against the collected
 * anchor destinations and emits {@code GoTo} annotations.
 *
 * <p>This is pass B of the two-pass anchor resolution: it runs after every
 * fragment has been placed and rendered, so a link may target an anchor that
 * appears later in the document (a forward reference). An unresolved anchor
 * emits no annotation — the source run already rendered as ordinary styled text
 * — and logs a warning. Resolution never throws.</p>
 */
final class PdfInternalLinkWriter {
    private static final Logger LOG = LoggerFactory.getLogger("com.demcha.compose.engine.render");

    private PdfInternalLinkWriter() {
    }

    static void apply(PDDocument document,
                      Map<String, PdfRenderEnvironment.AnchorDestination> anchors,
                      List<PdfRenderEnvironment.DeferredInternalLink> links) throws IOException {
        if (links.isEmpty()) {
            return;
        }
        int pageCount = document.getNumberOfPages();
        for (PdfRenderEnvironment.DeferredInternalLink link : links) {
            PdfRenderEnvironment.AnchorDestination destination = anchors.get(link.anchor());
            if (destination == null) {
                LOG.warn("render.pdf.internal-link.unresolved anchor={} — rendered as text, no annotation",
                        link.anchor());
                continue;
            }
            if (!validPage(link.pageIndex(), pageCount) || !validPage(destination.pageIndex(), pageCount)) {
                continue;
            }
            PDPage sourcePage = document.getPage(link.pageIndex());
            PDPage targetPage = document.getPage(destination.pageIndex());
            PdfLinkAnnotationWriter.addInternalLink(
                    sourcePage,
                    link.rectangle(),
                    targetPage,
                    destination.left(),
                    destination.top());
        }
    }

    private static boolean validPage(int pageIndex, int pageCount) {
        return pageIndex >= 0 && pageIndex < pageCount;
    }
}
