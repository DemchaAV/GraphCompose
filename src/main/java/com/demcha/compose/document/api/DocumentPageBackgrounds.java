package com.demcha.compose.document.api;

import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.style.DocumentColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Splices a session-wide page background fill into a compiled
 * {@link LayoutGraph}. Extracted from {@link DocumentSession} as part
 * of the Phase E.3 slim.
 *
 * <p>Pages get one extra {@link PlacedFragment} at the bottom of their
 * z-order so every other fragment paints on top of the background fill.
 * If the session has no page background or the layout has no pages,
 * the original graph is returned unchanged.</p>
 */
final class DocumentPageBackgrounds {
    private DocumentPageBackgrounds() {
    }

    /**
     * Returns a copy of {@code base} with a page-background fragment
     * spliced into every page, or {@code base} unchanged when there is
     * nothing to do.
     *
     * @param base   freshly compiled layout graph
     * @param color  session-wide background color, or {@code null}
     * @return a layout graph with background fragments, or {@code base}
     */
    static LayoutGraph apply(LayoutGraph base, DocumentColor color) {
        if (color == null || base.totalPages() == 0) {
            return base;
        }
        List<PlacedFragment> combined = new ArrayList<>(base.fragments().size() + base.totalPages());
        for (int page = 0; page < base.totalPages(); page++) {
            combined.add(new PlacedFragment(
                    "@page-background[" + page + "]",
                    0,
                    page,
                    0.0,
                    0.0,
                    base.canvas().width(),
                    base.canvas().height(),
                    com.demcha.compose.engine.components.style.Margin.zero(),
                    com.demcha.compose.engine.components.style.Padding.zero(),
                    new ShapeFragmentPayload(color.color(), null, 0.0, null, null, null)));
        }
        combined.addAll(base.fragments());
        return new LayoutGraph(base.canvas(), base.totalPages(), base.nodes(), combined);
    }
}
