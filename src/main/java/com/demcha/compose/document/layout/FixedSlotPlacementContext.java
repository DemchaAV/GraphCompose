package com.demcha.compose.document.layout;

import java.util.List;

/**
 * {@link PlacementContext} that pins a single page index and forbids page
 * advancement. Used by row slots, stacked layers, and any branch that must
 * place into a fixed page band — the parent flow has already decided which
 * page is being filled, so children must not move on by themselves.
 *
 * @param pageIndex       pinned page index that placements land on
 * @param canvas          canvas the placement is happening on
 * @param prepareContext  prepare-phase context used to (re)measure children
 * @param fragmentContext fragment-emission context forwarded to {@code emitFragments}
 * @param nodes           mutable list of placed semantic nodes; helpers append to this
 * @param fragments       mutable list of placed render fragments; helpers append to this
 * @author Artem Demchyshyn
 */
public record FixedSlotPlacementContext(
        int pageIndex,
        LayoutCanvas canvas,
        PrepareContext prepareContext,
        FragmentContext fragmentContext,
        List<PlacedNode> nodes,
        List<PlacedFragment> fragments
) implements PlacementContext {

    @Override
    public boolean canAdvancePage() {
        return false;
    }

    @Override
    public void advancePage() {
        throw new IllegalStateException(
                "FixedSlotPlacementContext is locked to page " + pageIndex
                + " and cannot advance pages.");
    }

    @Override
    public void touchPage() {
        // no-op: the surrounding mutating context already touched this page band.
    }
}
