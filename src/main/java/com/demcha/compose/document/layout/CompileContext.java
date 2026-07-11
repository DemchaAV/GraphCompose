package com.demcha.compose.document.layout;

import java.util.List;

/**
 * The cursor + emission context threaded through the compile pass: the page
 * cursor, the node-preparation and fragment-emission contexts, and the output
 * accumulators. Bundled so the per-node compile collaborators take one context
 * argument instead of repeating the same five parameters.
 *
 * <p>Package-private — engine surface, not public API. The accumulators are the
 * caller's live lists (appended to, not copied); the {@code state} cursor is
 * mutated as pages advance.</p>
 *
 * @param state           the compiler page cursor (mutated as pages advance)
 * @param prepareContext  the node-preparation context
 * @param fragmentContext the fragment-emission context
 * @param nodes           accumulator for placed nodes (appended to)
 * @param fragments       accumulator for placed fragments (appended to)
 * @author Artem Demchyshyn
 */
record CompileContext(CompilerState state,
                      PrepareContext prepareContext,
                      FragmentContext fragmentContext,
                      List<PlacedNode> nodes,
                      List<PlacedFragment> fragments) {
}
