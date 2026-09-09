package com.demcha.compose.document.layout;

import com.demcha.compose.document.layout.payloads.LayoutAnchorPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Every anchor the finished layout resolved, collected once and shared by every pass.
 *
 * <p>Collected <em>before</em> the first pass runs, from the compiled fragment list. That
 * ordering is the guarantee, not a convention: a pass's own additions go into a separate
 * list this collector never reads again, so nothing a pass contributes can grow new
 * anchors within the same compile. A pass therefore cannot influence what a later pass
 * sees, and running the same passes twice over the same graph gives the same answer.</p>
 *
 * <p>Anchors keep the order their fragments had, which is the order the compiler placed
 * them — reading order down the document. A consumer that wants a stronger guarantee
 * should sort by {@link LayoutAnchorId#index()}, which the declaring feature controls.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public final class ResolvedLayoutMetadata {

    private static final ResolvedLayoutMetadata EMPTY = new ResolvedLayoutMetadata(List.of());

    private final List<ResolvedLayoutAnchor> anchors;

    private ResolvedLayoutMetadata(List<ResolvedLayoutAnchor> anchors) {
        this.anchors = anchors;
    }

    /**
     * Collects the anchors a compiled graph carries.
     *
     * @param graph resolved layout graph
     * @return the metadata; empty when the document declares no anchors
     * @throws NullPointerException if {@code graph} is null
     */
    public static ResolvedLayoutMetadata from(LayoutGraph graph) {
        Objects.requireNonNull(graph, "graph");
        List<ResolvedLayoutAnchor> found = new ArrayList<>();
        for (PlacedFragment fragment : graph.fragments()) {
            if (fragment.payload() instanceof LayoutAnchorPayload anchor) {
                // Position from the fragment, size from the payload. The fragment box is
                // the anchor node's own — it measures to its child — but taking the size
                // from the payload keeps that true even if a future container hands the
                // anchor a wider placement.
                found.add(new ResolvedLayoutAnchor(anchor.id(), fragment.pageIndex(),
                        fragment.x(), fragment.y(), anchor.width(), anchor.height()));
            }
        }
        return found.isEmpty() ? EMPTY : new ResolvedLayoutMetadata(List.copyOf(found));
    }

    /**
     * Every resolved anchor, in placement order.
     *
     * @return immutable list, possibly empty
     */
    public List<ResolvedLayoutAnchor> anchors() {
        return anchors;
    }

    /**
     * The resolved anchors belonging to one logical owner and kind.
     *
     * @param groupKey the owner, compared by reference
     * @param kind     the anchor kind, compared by reference
     * @return immutable list in placement order, possibly empty
     * @throws NullPointerException if either argument is null
     */
    public List<ResolvedLayoutAnchor> anchors(Object groupKey, Object kind) {
        Objects.requireNonNull(groupKey, "groupKey");
        Objects.requireNonNull(kind, "kind");
        List<ResolvedLayoutAnchor> matching = new ArrayList<>();
        for (ResolvedLayoutAnchor anchor : anchors) {
            if (anchor.id().groupKey() == groupKey && anchor.id().kind() == kind) {
                matching.add(anchor);
            }
        }
        return List.copyOf(matching);
    }

    /**
     * Whether the document declared no anchors at all.
     *
     * @return true when there is nothing for a pass to anchor to
     */
    public boolean isEmpty() {
        return anchors.isEmpty();
    }
}
