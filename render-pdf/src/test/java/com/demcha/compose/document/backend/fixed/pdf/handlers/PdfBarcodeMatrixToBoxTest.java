package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.google.zxing.common.BitMatrix;
import org.apache.pdfbox.util.Matrix;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PdfBarcodeMatrixToBoxTest {

    @Test
    void theMatrixCornersLandOnTheBoxCornersWithTheFirstRowOnTop() {
        // A 400 x 200 matrix in a 200 x 60 box at (10, 20): the axes scale differently.
        Matrix transform = PdfBarcodeFragmentRenderHandler.matrixToBox(new BitMatrix(400, 200), 10, 20, 200, 60);

        assertPoint(transform.transformPoint(0, 0), 10, 80);
        assertPoint(transform.transformPoint(400, 0), 210, 80);
        assertPoint(transform.transformPoint(0, 200), 10, 20);
        assertPoint(transform.transformPoint(400, 200), 210, 20);
        assertPoint(transform.transformPoint(100, 50), 60, 65);
    }

    private static void assertPoint(Point2D.Float point, double x, double y) {
        assertThat(point.getX()).isCloseTo(x, within(1e-4));
        assertThat(point.getY()).isCloseTo(y, within(1e-4));
    }
}
