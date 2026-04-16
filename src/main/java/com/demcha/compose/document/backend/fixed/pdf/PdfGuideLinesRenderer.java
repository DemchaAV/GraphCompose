package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.PlacedFragment;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.awt.Color;
import java.io.IOException;

/**
 * Internal guide renderer for the canonical semantic PDF backend.
 */
final class PdfGuideLinesRenderer {
    private static final Color OUTER_COLOR = new Color(214, 39, 40);
    private static final Color INNER_COLOR = new Color(31, 119, 180);

    private PdfGuideLinesRenderer() {
    }

    static void draw(PlacedFragment fragment, Object payload, PdfRenderEnvironment environment) throws IOException {
        PDPageContentStream stream = environment.pageSurface(fragment.pageIndex());
        stream.saveGraphicsState();
        try {
            stream.setStrokingColor(OUTER_COLOR);
            stream.setLineWidth(0.6f);
            stream.addRect((float) fragment.x(), (float) fragment.y(), (float) fragment.width(), (float) fragment.height());
            stream.stroke();

            if (payload instanceof BuiltInNodeDefinitions.ParagraphFragmentPayload paragraphPayload
                    && (paragraphPayload.padding().horizontal() > 0 || paragraphPayload.padding().vertical() > 0)) {
                double innerX = fragment.x() + paragraphPayload.padding().left();
                double innerY = fragment.y() + paragraphPayload.padding().bottom();
                double innerWidth = Math.max(0.0, fragment.width() - paragraphPayload.padding().horizontal());
                double innerHeight = Math.max(0.0, fragment.height() - paragraphPayload.padding().vertical());
                if (innerWidth > 0.0 && innerHeight > 0.0) {
                    stream.setStrokingColor(INNER_COLOR);
                    stream.setLineWidth(0.4f);
                    stream.addRect((float) innerX, (float) innerY, (float) innerWidth, (float) innerHeight);
                    stream.stroke();
                }
            }
        } finally {
            stream.restoreGraphicsState();
        }
    }
}
