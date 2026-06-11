package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.output.DocumentDebugOptions;
import com.demcha.compose.engine.render.pdf.GlyphFallbackLogger;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.*;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Internal node-label overlay for the canonical semantic PDF backend.
 *
 * <p>Runs as a single post-pass after all content fragments have rendered,
 * so badges always paint on top of the page — a container's children or a
 * higher layer can never overdraw the label that annotates them. For every
 * owner path and page it prints one corner badge anchored at the top-right
 * of the owner's union bounds (the same bounds the guide renderer outlines),
 * straddling the top edge so at most an ascender half-strip of the first
 * content line is covered.</p>
 *
 * <p>The badge font is the built-in Helvetica base-14 face, constructed per
 * render pass (PDFBox font objects carry lazily-populated encode caches that
 * must not be shared across concurrent renders — the same reason the
 * watermark and header/footer renderers construct theirs per call). Text
 * degrades through {@link GlyphFallbackLogger#sanitize}, so WinAnsi-encodable
 * characters such as {@code é} survive and anything else becomes {@code ?}
 * with a one-time {@code glyph.missing} log.</p>
 */
final class PdfNodeLabelRenderer {
    private static final float FONT_SIZE = 5f;
    private static final float PADDING = 1f;
    private static final Color HALO_COLOR = new Color(255, 250, 205);
    private static final Color TEXT_COLOR = new Color(150, 20, 150);

    private PdfNodeLabelRenderer() {
    }

    /**
     * Draws one semantic label per owner path and page on top of the fully
     * rendered content.
     *
     * @param ownerBoundsByPath path → page → union bounds map shared with the
     *                          guide renderer; one badge is drawn per entry
     * @param environment       per-render PDF environment
     * @param labelText         which text variant to print
     * @throws IOException if writing to a page content stream fails
     */
    static void drawAll(Map<String, Map<Integer, PdfGuideLinesRenderer.Bounds>> ownerBoundsByPath,
                        PdfRenderEnvironment environment,
                        DocumentDebugOptions.LabelText labelText) throws IOException {
        if (ownerBoundsByPath.isEmpty()) {
            return;
        }
        // Regroup page-first and sort both levels so the emitted content
        // stream is deterministic regardless of the hash order of the
        // incoming maps (snapshot and byte-stability invariant).
        TreeMap<Integer, TreeMap<String, PdfGuideLinesRenderer.Bounds>> byPage = new TreeMap<>();
        for (Map.Entry<String, Map<Integer, PdfGuideLinesRenderer.Bounds>> byPath : ownerBoundsByPath.entrySet()) {
            for (Map.Entry<Integer, PdfGuideLinesRenderer.Bounds> pageBounds : byPath.getValue().entrySet()) {
                byPage.computeIfAbsent(pageBounds.getKey(), key -> new TreeMap<>())
                        .put(byPath.getKey(), pageBounds.getValue());
            }
        }

        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        for (Map.Entry<Integer, TreeMap<String, PdfGuideLinesRenderer.Bounds>> page : byPage.entrySet()) {
            int pageIndex = page.getKey();
            PDRectangle mediaBox = environment.document().getPage(pageIndex).getMediaBox();
            PDPageContentStream stream = environment.pageSurface(pageIndex);
            for (Map.Entry<String, PdfGuideLinesRenderer.Bounds> label : page.getValue().entrySet()) {
                draw(stream, font, mediaBox, label.getKey(), label.getValue(), labelText);
            }
        }
    }

    private static void draw(PDPageContentStream stream,
                             PDType1Font font,
                             PDRectangle mediaBox,
                             String path,
                             PdfGuideLinesRenderer.Bounds bounds,
                             DocumentDebugOptions.LabelText labelText) throws IOException {
        String text = GlyphFallbackLogger.sanitize(font,
                labelText == DocumentDebugOptions.LabelText.NAME
                        ? path.substring(path.lastIndexOf('/') + 1)
                        : path);
        if (text.isEmpty()) {
            return;
        }

        float textWidth = font.getStringWidth(text) / 1000f * FONT_SIZE;
        float boxHeight = FONT_SIZE + 2 * PADDING;
        float boxWidth = textWidth + 2 * PADDING;
        // Corner badge: anchored at the top-RIGHT of the owner bounds
        // (content usually starts flush left) and straddling the top edge so
        // only an ascender half-strip of the first line can be covered.
        // Clamped onto the page when the owner is narrower than the label or
        // touches a page edge.
        float boxX = (float) Math.max(bounds.x(), bounds.x() + bounds.width() - boxWidth);
        boxX = Math.min(boxX, mediaBox.getUpperRightX() - boxWidth);
        boxX = Math.max(boxX, mediaBox.getLowerLeftX());
        float boxTop = (float) Math.min(mediaBox.getUpperRightY(),
                bounds.y() + bounds.height() + boxHeight / 2.0);

        stream.saveGraphicsState();
        try {
            stream.setNonStrokingColor(HALO_COLOR);
            stream.addRect(boxX, boxTop - boxHeight, boxWidth, boxHeight);
            stream.fill();

            stream.setNonStrokingColor(TEXT_COLOR);
            stream.beginText();
            stream.setFont(font, FONT_SIZE);
            // Baseline sits one padding plus the approximate ascent below the
            // box top; Helvetica's ascent is ~80% of the point size.
            stream.newLineAtOffset(boxX + PADDING, boxTop - PADDING - FONT_SIZE * 0.8f);
            stream.showText(text);
            stream.endText();
        } finally {
            stream.restoreGraphicsState();
        }
    }
}
