package com.demcha.compose.document.layout;

import java.util.List;

/**
 * Contributes fragments derived from geometry the layout has already resolved.
 *
 * <p>Some things cannot be drawn during layout because they depend on where other things
 * ended up: a rail down a timeline runs between its markers, a bracket spans two sections,
 * a leader line joins a callout to its subject. A node definition cannot see any of that —
 * it is handed its own box and nothing else. A pass runs afterwards, over the finished
 * graph, when every position is known.</p>
 *
 * <h2>What a pass may do</h2>
 *
 * <p><strong>Add fragments. Nothing else.</strong> A pass cannot remove, reorder or modify
 * what the compiler produced, and cannot add pages or change the canvas. This is what
 * makes a document with no pass registered byte-identical to one compiled without the
 * mechanism at all, and it stops one feature's pass from corrupting another's output.</p>
 *
 * <p>A pass does not re-run layout. It reads a compiled {@link LayoutGraph} and the
 * {@link ResolvedLayoutMetadata} collected from it, and returns additions. The metadata is
 * gathered once, before any pass runs, so a pass never sees another pass's contributions
 * and cannot make the result depend on execution order in a way the caller did not
 * declare.</p>
 *
 * <h2>Ordering</h2>
 *
 * <p>Passes run in registration order, and each pass's additions keep the order it
 * returned them in. Within a page, everything at {@link LayoutDepth#UNDER_BODY} draws
 * behind the document body and everything at {@link LayoutDepth#OVER_BODY} in front of it;
 * there is no z-index in this engine, so depth is expressed by where the driver splices
 * the fragments into the list the backends walk.</p>
 *
 * <p>Internal on purpose. Whether authors should be able to register their own passes is a
 * larger question than the built-in features that need one — it would have to settle
 * failure handling, re-entrancy, thread safety and what a pass may see of another — and
 * answering it by accident is worse than leaving it open.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public interface ResolvedLayoutPass {

    /**
     * A short name for diagnostics. Never used to identify anything.
     *
     * @return non-null identifier
     */
    String id();

    /**
     * Produces the fragments this pass wants added.
     *
     * @param graph    the compiled layout, already resolved
     * @param metadata anchors collected from that graph before any pass ran
     * @return additions in draw order within their depth; empty when there is nothing to add
     */
    List<ResolvedLayoutAddition> contribute(LayoutGraph graph, ResolvedLayoutMetadata metadata);
}
