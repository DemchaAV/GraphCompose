package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentBleed;
import com.demcha.compose.document.style.DocumentInsets;

import java.util.List;

/**
 * Semantic authoring node in the GraphCompose document graph.
 *
 * <p>Implementations are immutable semantic values that describe author intent
 * independently from layout or backend concerns. Measurement, splitting, and
 * fragment emission are delegated to registered layout definitions rather than
 * encoded in the node itself.</p>
 */
public interface DocumentNode {

    /**
     * Optional semantic name used for snapshots and diagnostics.
     *
     * @return human-readable semantic name, or an empty string when unnamed
     */
    String name();

    /**
     * Outer spacing around the node.
     *
     * @return outer margin contribution
     */
    default DocumentInsets margin() {
        return DocumentInsets.zero();
    }

    /**
     * Inner spacing inside the node box.
     *
     * @return inner padding contribution
     */
    default DocumentInsets padding() {
        return DocumentInsets.zero();
    }

    /**
     * Child semantic nodes. Leaf nodes return an empty list.
     *
     * @return immutable child node list
     */
    default List<DocumentNode> children() {
        return List.of();
    }

    /**
     * Stable logical kind for diagnostics and snapshots.
     *
     * @return logical node kind
     */
    default String nodeKind() {
        return getClass().getSimpleName();
    }

    /**
     * Whether this node must paginate as a single unit — when it does not fit in
     * the remaining page space but would fit on a fresh page, the compiler
     * relocates it whole to the next page instead of flowing its children across
     * the boundary. Default {@code false} (normal flow). Nodes taller than a full
     * page always flow regardless of this flag.
     *
     * @return true to keep the node together on one page when possible
     * @since 1.8.0
     */
    default boolean keepTogether() {
        return false;
    }

    /**
     * Whether this node must stay with the block that follows it — it may not be
     * left as the last placed block on its page when a subsequent sibling with
     * content exists. When the node plus the first line of the following content
     * would not fit in the remaining page space (but do fit on a fresh page), the
     * compiler relocates the node to the next page so it stays glued to what it
     * introduces (CSS {@code break-after: avoid} semantics). This is the
     * orphaned-heading fix: a boxed section title never strands at a page bottom
     * apart from its body.
     *
     * <p>Default {@code false} (normal flow). The rule is inert when nothing
     * follows the node on the page (a trailing heading is not relocated), and
     * best-effort: if the node plus one following line cannot fit even on a fresh
     * page, the node flows in place. Unlike {@link #keepTogether()}, which keeps
     * the <em>whole</em> block together, this keeps the node with only the first
     * line of the next block — the right tool for a heading above a long,
     * page-spanning body.</p>
     *
     * <p>"First line" applies to a following block whose first flow unit is a line
     * of text (the common case — a heading above prose or list entries). When the
     * following block's first unit is indivisible (an image, a shape, a row) or a
     * non-text splittable (a table or list), the node is instead kept with that
     * whole first block, so a heading above a body taller than a page that starts
     * with such a unit is left in place. Runs of consecutive keep-with-next
     * siblings relocate together, with the break hoisted before the run.</p>
     *
     * @return true to keep this node with the first line of the following block
     * @since 2.1.0
     */
    default boolean keepWithNext() {
        return false;
    }

    /**
     * Edges on which this node bleeds past the page content margin to the
     * trimmed physical page edge. Default {@link DocumentBleed#none()} (normal
     * in-margin placement), so nodes that do not opt in are placed exactly as
     * before.
     *
     * @return the edges to bleed; never {@code null}
     * @since 1.9.0
     */
    default DocumentBleed bleed() {
        return DocumentBleed.none();
    }
}

