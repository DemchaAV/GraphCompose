package com.demcha.compose.document.layout;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.Chars;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.constraints.StringLength;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based coverage of pure engine layout invariants: over many generated
 * inputs (not a handful of hand-picked examples) these functions must hold their
 * algebraic contracts. The example-based unit tests ({@code TokenBreakingTest},
 * {@code LayerStackGeometryTest}, {@code RowSlotsTest}) pin specific cases; these
 * assert the laws across the whole input space, with jqwik shrinking any
 * counter-example to its minimal form.
 */
class LayoutInvariantsPropertiesTest {

    // ── TokenBreaking.softBreakSegments ──────────────────────────────────────

    /** Splitting a token and re-joining the pieces must reproduce it exactly. */
    @Property
    void softBreakSegmentsReconstructTheInput(
            @ForAll @StringLength(max = 40) @Chars({'a', 'b', 'c', '1', '.', ':', '/', '-'}) String token) {
        List<String> segments = TokenBreaking.softBreakSegments(token);
        assertThat(String.join("", segments)).isEqualTo(token);
    }

    /** No emitted segment is empty for a non-empty token. */
    @Property
    void softBreakSegmentsAreNeverEmptyForNonEmptyInput(
            @ForAll @StringLength(min = 1, max = 40) @Chars({'a', 'b', '.', ':', '/', '-'}) String token) {
        assertThat(TokenBreaking.softBreakSegments(token))
                .allSatisfy(segment -> assertThat(segment).isNotEmpty());
    }

    // ── LayerStackGeometry.zOrder ────────────────────────────────────────────

    /** The z-order is a stable permutation of the source indices, ascending by zIndex. */
    @Property
    void zOrderIsAStablePermutationSortedByZIndex(
            @ForAll @Size(max = 20) List<@IntRange(min = -5, max = 5) Integer> zIndices) {
        int n = zIndices.size();
        int[] order = LayerStackGeometry.zOrder(zIndices);

        assertThat(order).hasSize(n);
        boolean[] seen = new boolean[n];
        for (int index : order) {
            assertThat(index).isBetween(0, n - 1);
            assertThat(seen[index]).as("index %d must appear exactly once", index).isFalse();
            seen[index] = true;
        }
        for (int i = 1; i < n; i++) {
            int prev = order[i - 1];
            int cur = order[i];
            int zPrev = zIndices.get(prev);
            int zCur = zIndices.get(cur);
            assertThat(zPrev).as("z-order must be ascending by zIndex").isLessThanOrEqualTo(zCur);
            if (zPrev == zCur) {
                assertThat(prev).as("equal zIndex must keep source order").isLessThan(cur);
            }
        }
    }

    // ── RowSlots.validateWeightsMatchChildren ────────────────────────────────

    /** The guard throws exactly when the weight count differs from the child count. */
    @Property
    void validateWeightsThrowsExactlyWhenSizesDiffer(
            @ForAll @Size(max = 10) List<@DoubleRange(min = 0.0, max = 5.0) Double> weights,
            @ForAll @IntRange(min = 0, max = 10) int childCount) {
        if (weights.size() == childCount) {
            assertThatCode(() -> RowSlots.validateWeightsMatchChildren(weights, childCount))
                    .doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> RowSlots.validateWeightsMatchChildren(weights, childCount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("(" + weights.size() + ")")
                    .hasMessageContaining("(" + childCount + ")");
        }
    }
}
