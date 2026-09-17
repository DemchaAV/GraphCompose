package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.engine.components.content.barcode.BarcodeData;
import com.demcha.compose.engine.components.content.barcode.BarcodeType;
import com.google.zxing.common.BitMatrix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.Color;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class BarcodeRunsTest {

    static Stream<Arguments> symbols() {
        return Stream.of(
                Arguments.of(BarcodeType.QR_CODE, "https://github.com/DemchaAV/GraphCompose", 108, 108, 0),
                Arguments.of(BarcodeType.QR_CODE, "GC-2026-001", 160, 80, 4),
                Arguments.of(BarcodeType.DATA_MATRIX, "DM-parity-001", 80, 80, 0),
                Arguments.of(BarcodeType.PDF_417, "PDF417 payload for parity", 260, 90, 1),
                Arguments.of(BarcodeType.CODE_128, "GC-BENCH-2026-04-13", 280, 68, 4),
                Arguments.of(BarcodeType.CODE_39, "CODE39TEST", 240, 60, 0),
                Arguments.of(BarcodeType.EAN_13, "5901234123457", 190, 80, 9),
                Arguments.of(BarcodeType.EAN_8, "96385074", 140, 70, 9),
                Arguments.of(BarcodeType.UPC_A, "036000291452", 190, 80, 9));
    }

    @ParameterizedTest(name = "{0} {2}x{3} margin {4}")
    @MethodSource("symbols")
    void everyDarkCellIsCoveredByExactlyOneRectangle(BarcodeType type,
                                                     String content,
                                                     int boxWidth,
                                                     int boxHeight,
                                                     int margin) throws Exception {
        BitMatrix matrix = BarcodeMatrices.encode(
                BarcodeData.of(content, type, Color.BLACK, Color.WHITE, margin), boxWidth, boxHeight);

        assertCoversExactly(matrix, BarcodeRuns.of(matrix));
    }

    static Stream<Arguments> linearSymbols() {
        return symbols().filter(arguments -> switch ((BarcodeType) arguments.get()[0]) {
            case QR_CODE, DATA_MATRIX, PDF_417 -> false;
            default -> true;
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("linearSymbols")
    void aLinearSymbologyIsOneRectanglePerBar(BarcodeType type,
                                              String content,
                                              int boxWidth,
                                              int boxHeight,
                                              int margin) throws Exception {
        BitMatrix matrix = BarcodeMatrices.encode(
                BarcodeData.of(content, type, Color.BLACK, Color.WHITE, margin), boxWidth, boxHeight);

        BarcodeRuns runs = BarcodeRuns.of(matrix);

        assertThat(runs.count()).isEqualTo(barsInRow(matrix, 0));
        for (int i = 0; i < runs.count(); i++) {
            assertThat(runs.height(i)).as("bar %d spans the full height", i).isEqualTo(matrix.getHeight());
        }
    }

    @Test
    void emptyMatrixHasNoRectangles() {
        assertThat(BarcodeRuns.of(new BitMatrix(5, 3)).count()).isZero();
    }

    @Test
    void fullMatrixIsOneRectangle() {
        BitMatrix matrix = new BitMatrix(6, 4);
        matrix.setRegion(0, 0, 6, 4);

        BarcodeRuns runs = BarcodeRuns.of(matrix);

        assertThat(runs.count()).isEqualTo(1);
        assertThat(new int[]{runs.x(0), runs.y(0), runs.width(0), runs.height(0)}).containsExactly(0, 0, 6, 4);
    }

    @Test
    void checkerboardCellsStaySeparate() {
        BitMatrix matrix = new BitMatrix(4, 4);
        for (int y = 0; y < 4; y++) {
            for (int x = (y % 2); x < 4; x += 2) {
                matrix.set(x, y);
            }
        }

        BarcodeRuns runs = BarcodeRuns.of(matrix);

        assertThat(runs.count()).isEqualTo(8);
        assertCoversExactly(matrix, runs);
    }

    @Test
    void aRunExtendsOnlyWhenItCoversExactlyTheSameColumns() {
        // ##..    one rectangle two rows tall,
        // ##..    then a wider run below starts a new one,
        // ###.    and a shifted run starts another.
        // .##.
        BitMatrix matrix = new BitMatrix(4, 4);
        matrix.setRegion(0, 0, 2, 2);
        matrix.setRegion(0, 2, 3, 1);
        matrix.setRegion(1, 3, 2, 1);

        BarcodeRuns runs = BarcodeRuns.of(matrix);

        assertThat(runs.count()).isEqualTo(3);
        assertThat(new int[]{runs.x(0), runs.y(0), runs.width(0), runs.height(0)}).containsExactly(0, 0, 2, 2);
        assertThat(new int[]{runs.x(1), runs.y(1), runs.width(1), runs.height(1)}).containsExactly(0, 2, 3, 1);
        assertThat(new int[]{runs.x(2), runs.y(2), runs.width(2), runs.height(2)}).containsExactly(1, 3, 2, 1);
    }

    @Test
    void aRectangleStopsExtendingOnceARowSkipsIt() {
        // #.   the bottom cell is a new rectangle: the gap row closes the first one.
        // ..
        // #.
        BitMatrix matrix = new BitMatrix(2, 3);
        matrix.set(0, 0);
        matrix.set(0, 2);

        BarcodeRuns runs = BarcodeRuns.of(matrix);

        assertThat(runs.count()).isEqualTo(2);
        assertCoversExactly(matrix, runs);
    }

    private static void assertCoversExactly(BitMatrix matrix, BarcodeRuns runs) {
        int[][] coverage = new int[matrix.getHeight()][matrix.getWidth()];
        for (int i = 0; i < runs.count(); i++) {
            assertThat(runs.width(i)).isPositive();
            assertThat(runs.height(i)).isPositive();
            for (int y = runs.y(i); y < runs.y(i) + runs.height(i); y++) {
                for (int x = runs.x(i); x < runs.x(i) + runs.width(i); x++) {
                    coverage[y][x]++;
                }
            }
        }
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                assertThat(coverage[y][x])
                        .as("cell (%d, %d)", x, y)
                        .isEqualTo(matrix.get(x, y) ? 1 : 0);
            }
        }
    }

    private static int barsInRow(BitMatrix matrix, int y) {
        int bars = 0;
        boolean inBar = false;
        for (int x = 0; x < matrix.getWidth(); x++) {
            boolean dark = matrix.get(x, y);
            if (dark && !inBar) {
                bars++;
            }
            inBar = dark;
        }
        return bars;
    }
}
