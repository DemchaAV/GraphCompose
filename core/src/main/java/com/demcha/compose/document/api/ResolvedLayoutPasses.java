package com.demcha.compose.document.api;

import com.demcha.compose.document.layout.LayoutDepth;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.ResolvedLayoutAddition;
import com.demcha.compose.document.layout.ResolvedLayoutMetadata;
import com.demcha.compose.document.layout.ResolvedLayoutPass;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runs the resolved-layout passes over a compiled graph and splices what they contribute.
 *
 * <p>Package-private: this is the wiring behind a built-in feature, not a seam authors
 * reach. It sits beside {@code DocumentPageBackgrounds} and {@code DocumentPageZones}, the
 * two other post-compilation splices, and runs before both.</p>
 *
 * <p>The order matters and is not arbitrary. Backgrounds prepend their fragments and zones
 * append theirs, so running passes first yields, with no index arithmetic:</p>
 *
 * <pre>
 * background  &lt;  pass-under  &lt;  body  &lt;  pass-over  &lt;  zone chrome
 * </pre>
 *
 * <p>A pass running after backgrounds would have its under-body fragment prepended to
 * index 0 — beneath an opaque page background, and invisible.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
final class ResolvedLayoutPasses {

    private ResolvedLayoutPasses() {
    }

    /**
     * Applies the passes in registration order.
     *
     * @param base   freshly compiled layout graph
     * @param passes passes to run, in order; {@code null}/empty leaves {@code base} unchanged
     * @return a graph carrying the contributed fragments, or {@code base} itself
     * @throws NullPointerException  if {@code base} is null
     * @throws IllegalStateException if a pass returns null, or contributes a fragment for a
     *                               page the document does not have
     */
    static LayoutGraph apply(LayoutGraph base, List<ResolvedLayoutPass> passes) {
        Objects.requireNonNull(base, "base");
        if (passes == null || passes.isEmpty()) {
            return base;
        }

        // Collected once, from the compiled graph, and handed to every pass. Nothing a
        // pass contributes can seed a new anchor for a later pass, so the result does not
        // depend on how the passes happen to interleave.
        ResolvedLayoutMetadata metadata = ResolvedLayoutMetadata.from(base);

        List<PlacedFragment> under = new ArrayList<>();
        List<PlacedFragment> over = new ArrayList<>();
        for (ResolvedLayoutPass pass : passes) {
            List<ResolvedLayoutAddition> additions = pass.contribute(base, metadata);
            if (additions == null) {
                throw new IllegalStateException(
                        "Resolved-layout pass '" + pass.id() + "' returned null; return an empty list instead.");
            }
            for (ResolvedLayoutAddition addition : additions) {
                if (addition == null) {
                    throw new IllegalStateException(
                            "Resolved-layout pass '" + pass.id() + "' returned a null addition.");
                }
                PlacedFragment fragment = addition.fragment();
                if (fragment.pageIndex() < 0 || fragment.pageIndex() >= base.totalPages()) {
                    throw new IllegalStateException("Resolved-layout pass '" + pass.id()
                                                    + "' contributed a fragment on page " + fragment.pageIndex()
                                                    + ", but the document has " + base.totalPages()
                                                    + " page(s). A pass may not add pages.");
                }
                // A non-finite coordinate is not caught anywhere downstream: PlacedFragment
                // does not validate, and NaN reaches the content stream as a broken operator.
                requireFinite(pass, fragment.x(), "x");
                requireFinite(pass, fragment.y(), "y");
                requireFinite(pass, fragment.width(), "width");
                requireFinite(pass, fragment.height(), "height");
                (addition.depth() == LayoutDepth.UNDER_BODY ? under : over).add(fragment);
            }
        }

        if (under.isEmpty() && over.isEmpty()) {
            return base;
        }

        List<PlacedFragment> combined =
                new ArrayList<>(under.size() + base.fragments().size() + over.size());
        combined.addAll(under);
        combined.addAll(base.fragments());
        combined.addAll(over);
        // Nodes pass through untouched: a pass contributes drawing, never structure.
        return new LayoutGraph(base.canvas(), base.totalPages(), base.nodes(), combined);
    }

    private static void requireFinite(ResolvedLayoutPass pass, double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalStateException("Resolved-layout pass '" + pass.id()
                                            + "' contributed a fragment whose " + name + " is " + value
                                            + ". A non-finite coordinate reaches the content stream unchecked.");
        }
    }
}
