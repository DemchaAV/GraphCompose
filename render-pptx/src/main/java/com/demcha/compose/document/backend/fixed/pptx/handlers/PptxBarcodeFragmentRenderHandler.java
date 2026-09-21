package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.backend.fixed.pdf.handlers.BarcodeMatrices;
import com.demcha.compose.document.backend.fixed.pdf.handlers.BarcodeRuns;
import com.demcha.compose.document.backend.fixed.pptx.PptxCoordinates;
import com.demcha.compose.document.backend.fixed.pptx.PptxFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.PptxRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.BarcodeFragmentPayload;
import com.demcha.compose.engine.components.content.barcode.BarcodeData;
import com.google.zxing.common.BitMatrix;
import org.apache.poi.xslf.usermodel.XSLFShapeContainer;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.io.IOException;

/**
 * Renders semantic barcode fragments as native freeform shapes.
 *
 * <p>The bit matrix is the one the PDF handler draws (see {@link BarcodeMatrices}),
 * stretched over the fragment box the same way, so both formats place the symbol
 * alike. The background is one freeform over the box and the dark cells, merged into
 * rectangles, are a second one, so the barcode stays sharp at any zoom and editable
 * as two shapes rather than a picture. As in a bitmap, each cell is either foreground
 * or background: when the foreground is not opaque, the dark cells are cut out of the
 * background, so the foreground composites with whatever lies under the barcode.</p>
 *
 * @since 2.1.0
 */
public final class PptxBarcodeFragmentRenderHandler
        implements PptxFragmentRenderHandler<BarcodeFragmentPayload> {

    /**
     * Creates the barcode fragment renderer.
     */
    public PptxBarcodeFragmentRenderHandler() {
    }

    @Override
    public Class<BarcodeFragmentPayload> payloadType() {
        return BarcodeFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       BarcodeFragmentPayload payload,
                       PptxRenderEnvironment environment) throws IOException {
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
        Cells cells = new Cells(
                fragment.x(),
                PptxCoordinates.topY(environment.canvasHeight(), fragment.y(), fragment.height()),
                fragment.width() / matrix.getWidth(),
                fragment.height() / matrix.getHeight());
        XSLFShapeContainer surface = environment.surface(fragment.pageIndex());

        if (background.getAlpha() > 0) {
            Path2D.Double path = new Path2D.Double();
            cells.addRect(path, 0, 0, matrix.getWidth(), matrix.getHeight(), true);
            if (foreground.getAlpha() < 255) {
                // Each cell is either foreground or background, as in a bitmap of the
                // matrix: a foreground that is not opaque composites with whatever lies
                // under the barcode, so the dark cells are holes in the background. The
                // holes wind against the outline, so they stay open under either fill rule.
                addRuns(path, cells, runs, false);
            }
            PptxInlineGeometry.drawPath(surface, path, background, null);
        }
        if (foreground.getAlpha() > 0 && runs.count() > 0) {
            Path2D.Double path = new Path2D.Double();
            addRuns(path, cells, runs, true);
            PptxInlineGeometry.drawPath(surface, path, foreground, null);
        }
    }

    private static void addRuns(Path2D.Double path, Cells cells, BarcodeRuns runs, boolean clockwise) {
        for (int i = 0; i < runs.count(); i++) {
            cells.addRect(path, runs.x(i), runs.y(i), runs.width(i), runs.height(i), clockwise);
        }
    }

    /** Maps matrix cells, first row at the top, onto the slide box, whose y grows downwards. */
    private record Cells(double left, double top, double cellWidth, double cellHeight) {

        void addRect(Path2D.Double path, int x, int y, int width, int height, boolean clockwise) {
            double x0 = left + x * cellWidth;
            double y0 = top + y * cellHeight;
            double x1 = left + (x + width) * cellWidth;
            double y1 = top + (y + height) * cellHeight;
            path.moveTo(x0, y0);
            if (clockwise) {
                path.lineTo(x1, y0);
                path.lineTo(x1, y1);
                path.lineTo(x0, y1);
            } else {
                path.lineTo(x0, y1);
                path.lineTo(x1, y1);
                path.lineTo(x1, y0);
            }
            path.closePath();
        }
    }
}
