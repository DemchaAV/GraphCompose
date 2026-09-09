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
        // The child's border box, which is not the wrapper's. prepare() measured the
        // child's *margin* box, because that is the space the wrapper has to occupy for
        // the surrounding flow to be right; reporting it would be wrong for the one thing
        // this seam is for. A marker with margin(top 2, right 4, bottom 6, left 8) would
        // have its anchor centre land 2pt off its own ink in both directions, and a rail
        // drawn through that centre would visibly miss it. So the margin comes back off
        // here, in the offset and in the size. Measured rather than assumed: the compiler
        // seats the child at the anchor's bottom-left plus (left, bottom) — y grows up.
        DocumentInsets margin = prepared.node().child().margin();
        MeasureResult measured = prepared.measureResult();
        double width = Math.max(0.0, measured.width() - margin.horizontal());
        double height = Math.max(0.0, measured.height() - margin.vertical());
        return List.of(new LayoutFragment(
                placement.path(),
                0,
                margin.left(),
                margin.bottom(),
                width,
                height,
                new LayoutAnchorPayload(prepared.node().id(), width, height)));
    }
}
