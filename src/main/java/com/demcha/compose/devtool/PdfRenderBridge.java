package com.demcha.compose.devtool;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

/**
 * Lightweight bridge that renders a PDF page directly from an in-memory
 * {@link PDDocument} into a {@link BufferedImage}.
 */
public final class PdfRenderBridge {

    private PdfRenderBridge() {
        // Utility class
    }

    public static BufferedImage renderToImage(PDDocument document, int pageIndex, float scale) throws IOException {
        Objects.requireNonNull(document, "document");

        if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
            throw new IndexOutOfBoundsException(
                    "pageIndex must be between 0 and %d but was %d"
                            .formatted(Math.max(0, document.getNumberOfPages() - 1), pageIndex));
        }

        if (scale <= 0.0f) {
            throw new IllegalArgumentException("scale must be greater than 0");
        }

        var renderer = new PDFRenderer(document);
        return renderer.renderImage(pageIndex, scale, ImageType.RGB);
    }
}
