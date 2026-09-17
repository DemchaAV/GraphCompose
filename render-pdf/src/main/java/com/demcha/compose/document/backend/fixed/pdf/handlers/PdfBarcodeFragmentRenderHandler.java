package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.BarcodeFragmentPayload;
import com.demcha.compose.engine.components.content.barcode.BarcodeData;
import com.google.zxing.common.BitMatrix;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.util.Matrix;

import java.awt.Color;
import java.io.IOException;

/**
 * Renders semantic barcode fragments as vector paths.
 *
 * <p>ZXing encodes the content into a bit matrix (see {@link BarcodeMatrices}); the
 * handler fills the background over the fragment box and then fills the dark cells,
 * merged into rectangles, as one path. The matrix is stretched over the box exactly
 * as a bitmap of it would be, so each symbology keeps the placement ZXing gives it,
 * while the edges stay sharp at any zoom and no image is encoded. A translucent
 * foreground is painted over the background; a fully transparent one cuts the dark
 * cells out of it.</p>
 */
public final class PdfBarcodeFragmentRenderHandler
        implements PdfFragmentRenderHandler<BarcodeFragmentPayload> {

    /**
     * Creates the barcode fragment renderer.
     */
    public PdfBarcodeFragmentRenderHandler() {
    }

    @Override
    public Class<BarcodeFragmentPayload> payloadType() {
        return BarcodeFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       BarcodeFragmentPayload payload,
                       PdfRenderEnvironment environment) throws IOException {
        if (fragment.width() <= 0 || fragment.height() <= 0) {
            return;
        }

        BarcodeData data = payload.barcodeData();
        Color background = data.getBackground();
        Color foreground = data.getForeground();
        if (background.getAlpha() == 0 && foreground.getAlpha() == 0) {
            return;
        }
        BitMatrix matrix = BarcodeMatrices.encode(data, (int) fragment.width(), (int) fragment.height());
        BarcodeRuns runs = BarcodeRuns.of(matrix);
        PDPageContentStream stream = environment.pageSurface(fragment.pageIndex());
        stream.saveGraphicsState();
        try {
            // One transform to matrix cells, so every rectangle is written as whole numbers.
            stream.transform(matrixToBox(matrix, fragment.x(), fragment.y(), fragment.width(), fragment.height()));
            fillBackground(stream, environment, matrix, runs, background, foreground.getAlpha() == 0);
            fillCells(stream, environment, runs, foreground);
        } finally {
            stream.restoreGraphicsState();
        }
    }

    private static void fillBackground(PDPageContentStream stream,
                                       PdfRenderEnvironment environment,
                                       BitMatrix matrix,
                                       BarcodeRuns runs,
                                       Color background,
                                       boolean cellsTransparent) throws IOException {
        if (background.getAlpha() == 0) {
            return;
        }
        // Its own graphics state, so a translucent background's alpha does not
        // carry over to the cells drawn after it.
        stream.saveGraphicsState();
        try {
            PdfAlphaSupport.applyFillAlpha(environment, stream, background);
            stream.setNonStrokingColor(background);
            stream.addRect(0, 0, matrix.getWidth(), matrix.getHeight());
            if (cellsTransparent) {
                // A transparent foreground leaves the dark cells empty: the background
                // is cut away there and whatever lies under the barcode shows through.
                addRuns(stream, runs);
                stream.fillEvenOdd();
            } else {
                stream.fill();
            }
        } finally {
            stream.restoreGraphicsState();
        }
    }

    private static void fillCells(PDPageContentStream stream,
                                  PdfRenderEnvironment environment,
                                  BarcodeRuns runs,
                                  Color foreground) throws IOException {
        if (foreground.getAlpha() == 0 || runs.count() == 0) {
            return;
        }
        PdfAlphaSupport.applyFillAlpha(environment, stream, foreground);
        stream.setNonStrokingColor(foreground);
        // One path and one fill, so no seam shows where rectangles meet.
        addRuns(stream, runs);
        stream.fill();
    }

    private static void addRuns(PDPageContentStream stream, BarcodeRuns runs) throws IOException {
        for (int i = 0; i < runs.count(); i++) {
            stream.addRect(runs.x(i), runs.y(i), runs.width(i), runs.height(i));
        }
    }

    /**
     * Maps matrix cells onto the fragment box: the matrix spans the box on both axes,
     * and its first row is the top of the box, where PDF user space grows upwards.
     *
     * @param matrix barcode matrix
     * @param x      box left edge in points
     * @param y      box bottom edge in points
     * @param width  box width in points
     * @param height box height in points
     * @return the transform from cell coordinates to page coordinates
     */
    static Matrix matrixToBox(BitMatrix matrix, double x, double y, double width, double height) {
        return new Matrix(
                (float) (width / matrix.getWidth()), 0,
                0, (float) (-height / matrix.getHeight()),
                (float) x, (float) (y + height));
    }
}
