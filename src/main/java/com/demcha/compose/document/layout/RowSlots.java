package com.demcha.compose.document.layout;

import java.util.List;

/**
 * Shared validation helpers for row weight / children distribution code.
 *
 * <p>Centralises the {@link IllegalArgumentException} contract used by both
 * {@link LayoutCompiler#distributeRowSlotWidths(List, List, double, double) compile-phase}
 * and {@link NodeDefinitionSupport#measureRow measure-phase} row distribution.
 * The {@code RowNode} canonical constructor already rejects a mismatched
 * weights list at construction time; these helpers are defence-in-depth
 * for any path that bypasses the constructor (reflection-based
 * deserialization, framework proxies, etc.) and arrives at the engine
 * with an inconsistent {@code (weights, children)} pair.</p>
 *
 * <p>Package-private intentionally — engine surface, not public API.</p>
 *
 * @author Artem Demchyshyn
 */
final class RowSlots {

    private RowSlots() {
        // Utility class, no instantiation.
    }

    /**
     * Asserts that an explicit {@code weights} list matches the row's
     * children count. Callers must skip this check when {@code weights}
     * is null or empty — the even-split fallback applies there instead.
     *
     * @param weights    non-null, non-empty weights list
     * @param childCount number of row children
     * @throws IllegalArgumentException if {@code weights.size() != childCount}
     */
    static void validateWeightsMatchChildren(List<Double> weights, int childCount) {
        if (weights.size() != childCount) {
            throw new IllegalArgumentException(
                    "Row weights size (" + weights.size() + ") must match children size ("
                    + childCount + "). Pass exactly " + childCount
                    + " weight(s) or leave weights empty for an even split.");
        }
    }
}
