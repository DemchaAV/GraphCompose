package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.layout.PreparedNodeLayout;
import com.demcha.compose.document.node.LayerAlign;

import java.util.Arrays;
import java.util.List;

/**
 * Prepared layout payload attached to {@code LayerStackNode} prepared nodes.
 *
 * <p>Carries per-layer alignment plus an on-screen offset (positive
 * {@code offsetX} = right, positive {@code offsetY} = down) and an
 * explicit per-layer {@code zIndex}. The layout compiler stable-sorts
 * layers by ascending {@code zIndex} before emitting fragments, so a
 * later-declared layer with {@code zIndex = 10} renders on top of an
 * earlier-declared layer with {@code zIndex = 5}.</p>
 *
 * @param alignments per-layer alignments resolved from the source order
 * @param offsetsX   per-layer horizontal offsets from the alignment anchor
 * @param offsetsY   per-layer vertical offsets from the alignment anchor
 * @param zIndices   per-layer render-order keys (defaults are {@code 0})
 */
public record PreparedStackLayout(
        List<LayerAlign> alignments,
        List<Double> offsetsX,
        List<Double> offsetsY,
        List<Integer> zIndices) implements PreparedNodeLayout {
    /**
     * Creates a prepared stack layout payload with frozen alignment, offset,
     * and z-index metadata.
     */
    public PreparedStackLayout {
        alignments = List.copyOf(alignments);
        offsetsX = List.copyOf(offsetsX);
        offsetsY = List.copyOf(offsetsY);
        zIndices = List.copyOf(zIndices);
        if (offsetsX.size() != alignments.size()
            || offsetsY.size() != alignments.size()
            || zIndices.size() != alignments.size()) {
            throw new IllegalArgumentException(
                    "PreparedStackLayout: alignments/offsets/zIndices size mismatch ("
                    + alignments.size() + "/" + offsetsX.size() + "/"
                    + offsetsY.size() + "/" + zIndices.size() + ")");
        }
    }

    /**
     * Backward-compatible 3-arg constructor — defaults zIndices to all
     * zeros so layers render in source order.
     *
     * @param alignments per-layer alignments
     * @param offsetsX   per-layer horizontal offsets
     * @param offsetsY   per-layer vertical offsets
     */
    public PreparedStackLayout(List<LayerAlign> alignments,
                               List<Double> offsetsX,
                               List<Double> offsetsY) {
        this(alignments, offsetsX, offsetsY, zeroInts(alignments.size()));
    }

    /**
     * Backward-compatible factory for callers that only carry alignments;
     * fills both offset lists with zeros and zIndices with zeros.
     *
     * @param alignments per-layer alignments
     */
    public PreparedStackLayout(List<LayerAlign> alignments) {
        this(alignments, zeros(alignments.size()), zeros(alignments.size()), zeroInts(alignments.size()));
    }

    private static List<Double> zeros(int size) {
        Double[] out = new Double[size];
        Arrays.fill(out, 0.0);
        return List.of(out);
    }

    private static List<Integer> zeroInts(int size) {
        Integer[] out = new Integer[size];
        Arrays.fill(out, 0);
        return List.of(out);
    }
}
