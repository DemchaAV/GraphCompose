package com.demcha.compose.document.debug.snapshot;

import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.AnchorMarkerPayload;
import com.demcha.compose.document.snapshot.PageIndex;
import com.demcha.compose.document.snapshot.PageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds a {@link PageIndex} from a resolved layout graph by reading the
 * {@link AnchorMarkerPayload} marker fragments every anchored node emits at its
 * top-left. Backend-neutral — it walks the same {@code graph.fragments()} list,
 * in the same order, that the PDF backend resolves to go-to destinations, so a
 * resolved page matches where a {@code linkTo(anchor)} jumps.
 *
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public final class PageIndexExtractor {

    private PageIndexExtractor() {
    }

    /**
     * Resolves every declared anchor to its page from the layout graph.
     *
     * @param graph resolved layout graph
     * @return the page index; a duplicate anchor keeps its last registration
     */
    public static PageIndex from(LayoutGraph graph) {
        Objects.requireNonNull(graph, "graph");
        List<PageReference> references = new ArrayList<>();
        for (PlacedFragment fragment : graph.fragments()) {
            if (fragment.payload() instanceof AnchorMarkerPayload marker && !marker.anchor().isEmpty()) {
                references.add(new PageReference(marker.anchor(), fragment.pageIndex()));
            }
        }
        return new PageIndex(references, graph.totalPages());
    }
}
