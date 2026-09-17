package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;

import java.util.Arrays;

/**
 * The dark cells of a barcode bit matrix, merged into as few rectangles as a
 * row-by-row scan finds.
 *
 * <p>Each row is split into runs of set bits. A run that covers exactly the same
 * columns as a rectangle touching the row above extends that rectangle one row
 * down, and otherwise starts a new one. A barcode matrix is scaled up from its
 * modules, so a module's rows repeat and collapse into one rectangle; a linear
 * symbology, which repeats one row over its whole height, becomes one rectangle
 * per bar. Every set bit is covered by exactly one rectangle.</p>
 *
 * <p>Coordinates are matrix cells with the origin at the top-left, as ZXing
 * indexes them.</p>
 */
final class BarcodeRuns {

    private final int[] rects;
    private final int count;

    private BarcodeRuns(int[] rects, int count) {
        this.rects = rects;
        this.count = count;
    }

    /**
     * Merges the set bits of {@code matrix} into rectangles.
     *
     * @param matrix barcode matrix; a set bit is a dark cell
     * @return the rectangles covering every set bit once
     */
    static BarcodeRuns of(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int[] rects = new int[64];
        int count = 0;
        // Rectangles that reached the previous row, left to right.
        int[] open = new int[16];
        int openCount = 0;
        int[] reached = new int[16];
        BitArray row = new BitArray(width);

        for (int y = 0; y < height; y++) {
            row = matrix.getRow(y, row);
            int reachedCount = 0;
            int candidate = 0;
            int start = row.getNextSet(0);
            while (start < width) {
                int end = row.getNextUnset(start);
                while (candidate < openCount && rects[open[candidate] * 4] < start) {
                    candidate++;
                }
                int index;
                if (candidate < openCount
                        && rects[open[candidate] * 4] == start
                        && rects[open[candidate] * 4 + 2] == end - start) {
                    index = open[candidate++];
                    rects[index * 4 + 3]++;
                } else {
                    if ((count + 1) * 4 > rects.length) {
                        rects = Arrays.copyOf(rects, rects.length * 2);
                    }
                    index = count++;
                    rects[index * 4] = start;
                    rects[index * 4 + 1] = y;
                    rects[index * 4 + 2] = end - start;
                    rects[index * 4 + 3] = 1;
                }
                if (reachedCount == reached.length) {
                    reached = Arrays.copyOf(reached, reached.length * 2);
                }
                reached[reachedCount++] = index;
                start = end < width ? row.getNextSet(end) : width;
            }
            int[] swap = open;
            open = reached;
            reached = swap;
            openCount = reachedCount;
        }
        return new BarcodeRuns(rects, count);
    }

    /** @return the number of rectangles */
    int count() {
        return count;
    }

    /** @return left column of rectangle {@code index} */
    int x(int index) {
        return rects[index * 4];
    }

    /** @return top row of rectangle {@code index} */
    int y(int index) {
        return rects[index * 4 + 1];
    }

    /** @return width in cells of rectangle {@code index} */
    int width(int index) {
        return rects[index * 4 + 2];
    }

    /** @return height in cells of rectangle {@code index} */
    int height(int index) {
        return rects[index * 4 + 3];
    }
}
