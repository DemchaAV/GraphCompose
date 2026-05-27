package com.demcha.compose.document.api;

import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Splices session-wide page background fills into a compiled
 * {@link LayoutGraph}. Supports any number of partial-page rectangular
 * fills per page (multi-column backgrounds, accent stripes, etc.).
 *
 * <p>Background fragments are placed at the very bottom of the page
 * z-order (z=0), so every other fragment paints on top of them. Within
 * a single page, fills are emitted in list order — later entries paint
 * over earlier entries where they overlap. If the layout has no pages
 * or no fills were configured, the original graph is returned
 * unchanged.</p>
 */
final class DocumentPageBackgrounds {
    private DocumentPageBackgrounds() {
    }

    /**
     * Multi-rect form. Emits one fragment per fill per page, with
     * coordinates computed from the fill's ratios and the canvas size.
     *
     * @param base  freshly compiled layout graph
     * @param fills ordered list of page-background fills (each painted
     *              on every page); {@code null}/empty leaves {@code base}
     *              unchanged
     * @return a layout graph with background fragments, or {@code base}
     */
    static LayoutGraph apply(LayoutGraph base, List<PageBackgroundFill> fills) {
        if (fills == null || fills.isEmpty() || base.totalPages() == 0) {
            return base;
        }
        double pageWidth = base.canvas().width();
        double pageHeight = base.canvas().height();
        int extra = base.totalPages() * fills.size();
        List<PlacedFragment> combined =
                new ArrayList<>(base.fragments().size() + extra);
        for (int page = 0; page < base.totalPages(); page++) {
            for (int i = 0; i < fills.size(); i++) {
                PageBackgroundFill fill = fills.get(i);
                combined.add(new PlacedFragment(
                        "@page-background[" + page + "][" + i + "]",
                        0,
                        page,
                        fill.xRatio() * pageWidth,
                        fill.yRatio() * pageHeight,
                        fill.widthRatio() * pageWidth,
                        fill.heightRatio() * pageHeight,
                        com.demcha.compose.engine.components.style.Margin.zero(),
                        com.demcha.compose.engine.components.style.Padding.zero(),
                        new ShapeFragmentPayload(fill.color().color(),
                                null, 0.0, null, null, null)));
            }
        }
        combined.addAll(base.fragments());
        return new LayoutGraph(base.canvas(), base.totalPages(),
                base.nodes(), combined);
    }
}
