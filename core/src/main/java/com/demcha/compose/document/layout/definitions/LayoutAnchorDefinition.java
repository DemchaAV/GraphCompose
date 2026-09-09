package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.BoxConstraints;
import com.demcha.compose.document.layout.CompositeLayoutSpec;
import com.demcha.compose.document.layout.FragmentContext;
import com.demcha.compose.document.layout.FragmentPlacement;
import com.demcha.compose.document.layout.LayoutAnchorNode;
import com.demcha.compose.document.layout.LayoutFragment;
import com.demcha.compose.document.layout.MeasureResult;
import com.demcha.compose.document.layout.NodeDefinition;
import com.demcha.compose.document.layout.PaginationPolicy;
import com.demcha.compose.document.layout.PrepareContext;
import com.demcha.compose.document.layout.PreparedNode;
import com.demcha.compose.document.layout.payloads.LayoutAnchorPayload;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentInsets;

import java.util.List;

/**
 * Layout definition for {@link LayoutAnchorNode}: lays the child out unchanged and emits
 * one non-visual fragment saying where it ended up.
 *
 * <p>The wrapper measures to the child's own size rather than to the available width. A
 * wrapper that filled the width would report its container's box, which is the error this
 * whole seam exists to avoid — an 8×8 marker in a 16pt table cell must anchor at 8×8, not
 * at the cell.</p>
 *
 * <p>What it reports is the child's <b>border box</b>: the box the child was laid out
 * into, its own margin excluded and its padding included. Two boxes are in play and only
 * one of them is useful to a consumer — see {@link #emitFragments}.</p>
 *
 * <p>For a child that spans pages it reports <b>one slice per page</b>, from the placement
 * the compiler already computed for that page, so the anchor of a tall section is usable on
 * every page it crosses rather than a single box that fits on none of them.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public final class LayoutAnchorDefinition implements NodeDefinition<LayoutAnchorNode> {

    /**
     * Creates the anchor layout definition.
     */
    public LayoutAnchorDefinition() {
    }

    @Override
    public Class<LayoutAnchorNode> nodeType() {
        return LayoutAnchorNode.class;
    }

    @Override
    public PreparedNode<LayoutAnchorNode> prepare(LayoutAnchorNode node, PrepareContext ctx,
                                                  BoxConstraints constraints) {
        DocumentNode child = node.child();
        double childInner = Math.max(0.0, constraints.availableWidth() - child.margin().horizontal());
        PreparedNode<DocumentNode> childPrepared = ctx.prepare(child, BoxConstraints.natural(childInner));
        // Shrink to the child, both ways: the anchor must not report a box the child does
        // not occupy, and it must not add height the author did not ask for.
        double width = childPrepared.measureResult().width() + child.margin().horizontal();
        double height = childPrepared.measureResult().height() + child.margin().vertical();
        return PreparedNode.composite(node, new MeasureResult(width, height),
                new CompositeLayoutSpec(0.0, CompositeLayoutSpec.Axis.VERTICAL));
    }

    @Override
    public PaginationPolicy paginationPolicy(LayoutAnchorNode node) {
        return PaginationPolicy.ATOMIC;
    }

    @Override
    public List<DocumentNode> children(LayoutAnchorNode node) {
        return node.children();
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<LayoutAnchorNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        // The slice of the child's border box that lands on *this* page.
        //
        // The placement already is that slice. A composite's fragments are emitted once
        // per page it occupies, and the band each segment clamps to is computed by
        // CompositeDecoration — the same geometry that puts a spanning section's border on
        // every page it crosses, per-page margins included. Reading the placement is what
        // keeps this one formula instead of a second one drifting beside it. Taking the
        // measured height instead reported the whole subtree on every page: a section
        // across five pages said 453.25pt five times, with tops far outside the page.
        //
        // The margin is a separate matter and comes off per edge. It is part of the flow
        // extent the wrapper has to occupy but no part of the box being reported, and a
        // slice only meets the edges it actually contains — the top margin is inside the
        // first page's slice, the bottom margin inside the last page's, and a middle page
        // holds neither. A node that fits on one page is both, so it loses both, exactly
        // as before. Horizontally every slice spans the whole box, so both sides always go.
        DocumentInsets margin = prepared.node().child().margin();
        double topInset = placement.pageIndex() == placement.startPage() ? margin.top() : 0.0;
        double bottomInset = placement.pageIndex() == placement.endPage() ? margin.bottom() : 0.0;

        double width = Math.max(0.0, placement.width() - margin.horizontal());
        double height = Math.max(0.0, placement.height() - topInset - bottomInset);
        return List.of(new LayoutFragment(
                placement.path(),
                0,
                margin.left(),
                bottomInset,
                width,
                height,
                new LayoutAnchorPayload(prepared.node().id(), width, height)));
    }
}
