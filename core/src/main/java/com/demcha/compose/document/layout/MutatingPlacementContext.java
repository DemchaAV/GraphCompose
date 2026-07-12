package com.demcha.compose.document.layout;

import java.util.List;
import java.util.Objects;

/**
 * {@link PlacementContext} backed by a live {@link CompilerState}. Reads
 * {@link #pageIndex()} from the state every call so callers always see the
 * latest cursor, and forwards {@link #advancePage()} / {@link #touchPage()}
 * to the same state. Used wherever {@link LayoutCompiler} is following the
 * top-down page flow and needs to be free to break to a new page on overflow.
 *
 * @author Artem Demchyshyn
 */
public final class MutatingPlacementContext implements PlacementContext {

    private final CompilerState state;
    private final PrepareContext prepareContext;
    private final FragmentContext fragmentContext;
    private final List<PlacedNode> nodes;
    private final List<PlacedFragment> fragments;

    MutatingPlacementContext(CompilerState state,
                             PrepareContext prepareContext,
                             FragmentContext fragmentContext,
                             List<PlacedNode> nodes,
                             List<PlacedFragment> fragments) {
        this.state = Objects.requireNonNull(state, "state");
        this.prepareContext = Objects.requireNonNull(prepareContext, "prepareContext");
        this.fragmentContext = Objects.requireNonNull(fragmentContext, "fragmentContext");
        this.nodes = Objects.requireNonNull(nodes, "nodes");
        this.fragments = Objects.requireNonNull(fragments, "fragments");
    }

    @Override
    public int pageIndex() {
        return state.pageIndex;
    }

    @Override
    public LayoutCanvas canvas() {
        return state.canvas;
    }

    @Override
    public PrepareContext prepareContext() {
        return prepareContext;
    }

    @Override
    public FragmentContext fragmentContext() {
        return fragmentContext;
    }

    @Override
    public List<PlacedNode> nodes() {
        return nodes;
    }

    @Override
    public List<PlacedFragment> fragments() {
        return fragments;
    }

    @Override
    public boolean canAdvancePage() {
        return true;
    }

    @Override
    public void advancePage() {
        state.newPage();
    }

    @Override
    public void touchPage() {
        state.touchPage();
    }

    /**
     * Exposes the underlying mutable state for the page-flow paths in
     * {@link LayoutCompiler} that still need to read {@code usedHeight} or
     * {@code remainingHeight()} directly. Package-private to keep the
     * surface narrow.
     */
    CompilerState state() {
        return state;
    }
}
