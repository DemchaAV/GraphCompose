package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfDebugOptions;
import com.demcha.compose.document.layout.PlacedFragment;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.*;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Internal node-label overlay for the canonical semantic PDF backend.
 *
 * <p>Prints the stable semantic path of a fragment's owning node once per
 * owner and page, anchored at the top-left corner of the owner's union
 * bounds (the same bounds the guide renderer uses for margin/padding
 * rectangles). Uses the built-in Helvetica base-14 font so the overlay
 * never touches the session's font system.</p>
 */
final class PdfNodeLabelRenderer {
    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final float FONT_SIZE = 5f;
    private static final float PADDING = 1f;
    private static final Color HALO_COLOR = new Color(255, 250, 205);
    private static final Color TEXT_COLOR = new Color(150, 20, 150);

    private PdfNodeLabelRenderer() {
    }

    /**
     * Draws the semantic label for the fragment's owner if it has not been
     * drawn on this page yet.
     *
     * @param fragment           fragment whose owner may need a label
     * @param environment        per-render PDF environment
     * @param ownerBoundsByPath  path → page → union bounds map shared with the
     *                           guide renderer
     * @param drawnKeys          mutable set of {@code path#page} keys already
     *                           labelled during this render pass
     * @param labelText          which text variant to print
     * @throws IOException if writing to the page content stream fails
     */
    static void draw(PlacedFragment fragment,
                     PdfRenderEnvironment environment,
                     Map<String, Map<Integer, PdfGuideLinesRenderer.Bounds>> ownerBoundsByPath,
                     Set<String> drawnKeys,
                     PdfDebugOptions.LabelText labelText) throws IOException {
        String path = fragment.path();
        if (path == null || !drawnKeys.add(path + '#' + fragment.pageIndex())) {
            return;
        }

        PdfGuideLinesRenderer.Bounds bounds = lookupOwnerBounds(fragment, ownerBoundsByPath);
        String text = sanitize(labelText == PdfDebugOptions.LabelText.NAME
                ? path.substring(path.lastIndexOf('/') + 1)
                : path);
        if (text.isEmpty()) {
            return;
        }

        float textWidth = FONT.getStringWidth(text) / 1000f * FONT_SIZE;
        float boxHeight = FONT_SIZE + 2 * PADDING;
        // Corner-badge placement: anchored at the top-RIGHT of the owner
        // bounds (content usually starts flush left) and straddling the top
        // edge, so half the badge sits in the inter-block gap above and only
        // a half-strip of the first line can be covered. Clamped back onto
        // the page when the owner touches the page top or is narrower than
        // the label.
        float boxWidth = textWidth + 2 * PADDING;
        float boxX = (float) Math.max(bounds.x(), bounds.x() + bounds.width() - boxWidth);
        double pageTop = environment.document().getPage(fragment.pageIndex()).getMediaBox().getUpperRightY();
        float boxTop = (float) Math.min(pageTop, bounds.y() + bounds.height() + boxHeight / 2.0);

        PDPageContentStream stream = environment.pageSurface(fragment.pageIndex());
        stream.saveGraphicsState();
        try {
            stream.setNonStrokingColor(HALO_COLOR);
            stream.addRect(boxX, boxTop - boxHeight, boxWidth, boxHeight);
            stream.fill();

            stream.setNonStrokingColor(TEXT_COLOR);
            stream.beginText();
            stream.setFont(FONT, FONT_SIZE);
            // Baseline sits one padding plus the approximate ascent below the
            // box top; Helvetica's ascent is ~80% of the point size.
            stream.newLineAtOffset(boxX + PADDING, boxTop - PADDING - FONT_SIZE * 0.8f);
            stream.showText(text);
            stream.endText();
        } finally {
            stream.restoreGraphicsState();
        }
    }

    private static PdfGuideLinesRenderer.Bounds lookupOwnerBounds(
            PlacedFragment fragment,
            Map<String, Map<Integer, PdfGuideLinesRenderer.Bounds>> ownerBoundsByPath) {
        Map<Integer, PdfGuideLinesRenderer.Bounds> byPage =
                ownerBoundsByPath == null ? null : ownerBoundsByPath.get(fragment.path());
        PdfGuideLinesRenderer.Bounds bounds = byPage == null ? null : byPage.get(fragment.pageIndex());
        return bounds == null ? PdfGuideLinesRenderer.Bounds.from(fragment) : bounds;
    }

    /**
     * Replaces every character outside printable ASCII with {@code ?} so the
     * base-14 Helvetica encoder never rejects a label. Semantic names accept
     * any Unicode letter (the DSL normalizer keeps letters and digits), while
     * WinAnsi covers only a Latin subset — a Cyrillic or CJK node name must
     * degrade gracefully instead of failing the debug render.
     */
    private static String sanitize(String text) {
        StringBuilder safe = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            safe.append(current >= 0x20 && current <= 0x7E ? current : '?');
        }
        return safe.toString();
    }
}
