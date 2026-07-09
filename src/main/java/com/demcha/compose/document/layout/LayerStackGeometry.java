package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.LayerAlign;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Pure geometry for {@code LayerStackNode} placement, pulled out of
 * {@link LayoutCompiler}: the stable z-index render order and the align-based
 * offset of a layer within its stack. All inputs are passed in and the results
 * depend only on them — no compiler pagination state is read — so the compiler
 * calls these to position layers while owning the placement itself.
 *
 * @author Artem Demchyshyn
 */
final class LayerStackGeometry {

    private LayerStackGeometry() {
    }

    /**
     * Returns an iteration order over {@code zIndices} that is stable on
     * ties. Layers with equal {@code zIndex} keep their source order, so
     * the default of all-zero zIndices yields the identity permutation
     * {@code [0, 1, ..., n-1]} and existing snapshots stay deterministic.
     *
     * @param zIndices per-layer render-order keys (in source order)
     * @return source indices sorted by ascending {@code zIndex}, stable
     * on ties
     */
    static int[] zOrder(List<Integer> zIndices) {
        int n = zIndices.size();
        if (n <= 1) {
            return identityOrder(n);
        }
        // Common case: every layer uses the same zIndex (typically 0). A
        // stable sort would preserve source order anyway, so skip the boxed
        // array allocation and the full sort.
        int firstZ = zIndices.get(0);
        boolean allEqual = true;
        for (int i = 1; i < n; i++) {
            if (zIndices.get(i) != firstZ) {
                allEqual = false;
                break;
            }
        }
        if (allEqual) {
            return identityOrder(n);
        }
        Integer[] boxed = new Integer[n];
        for (int i = 0; i < n; i++) {
            boxed[i] = i;
        }
        // Comparator.comparingInt + java.util.Arrays.sort on boxed array is
        // documented stable; primitive int[] sort is not.
        Arrays.sort(boxed, Comparator.comparingInt(zIndices::get));
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = boxed[i];
        }
        return order;
    }

    private static int[] identityOrder(int n) {
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        return order;
    }

    static double horizontalOffset(LayerAlign align, double innerWidth, double childOuterWidth) {
        return switch (align) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> 0.0;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> Math.max(0.0, (innerWidth - childOuterWidth) / 2.0);
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> Math.max(0.0, innerWidth - childOuterWidth);
        };
    }

    static double verticalOffset(LayerAlign align, double innerHeight, double childOuterHeight) {
        return switch (align) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 0.0;
            case CENTER_LEFT, CENTER, CENTER_RIGHT -> Math.max(0.0, (innerHeight - childOuterHeight) / 2.0);
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> Math.max(0.0, innerHeight - childOuterHeight);
        };
    }
}
