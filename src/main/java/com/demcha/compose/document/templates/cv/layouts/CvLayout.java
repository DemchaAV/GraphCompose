package com.demcha.compose.document.templates.cv.layouts;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.templates.api.SlotMap;

import java.util.List;

/**
 * Templates v2 CV layout — assembles a header plus slot-filled module
 * content into the final document tree.
 *
 * <p>Implementations expose a fixed set of named slots
 * ({@link #slotNames()}) and a single composition seam
 * ({@link #compose(DocumentNode, SlotMap)}). A preset:</p>
 *
 * <ol>
 *   <li>Picks a layout (e.g. {@link SingleColumn}, {@link TwoColumnSidebar}).</li>
 *   <li>Reads the layout's slot names so it knows which buckets to
 *       fill.</li>
 *   <li>Builds module nodes from {@code CvSpec} data via the
 *       components / blocks pipeline.</li>
 *   <li>Places each module node into the appropriate slot.</li>
 *   <li>Calls {@code layout.compose(headerNode, slots)} to obtain the
 *       final {@link DocumentNode} for the session.</li>
 * </ol>
 *
 * <p>Layouts are pure structural composers: they do not own any
 * styling decisions other than gap / weight tokens supplied at
 * construction time. Theming and spacing live with the components and
 * theme tokens; layouts only decide where blocks of pre-rendered
 * content sit on the page.</p>
 */
public interface CvLayout {

    /**
     * Returns the stable slot names exposed by this layout, in the
     * order the layout renders them.
     *
     * <p>Slot names are part of the public contract: presets reference
     * them through layout constants (e.g.
     * {@link TwoColumnSidebar#MAIN}) and end users may reference them
     * by string when overriding placement.</p>
     *
     * @return non-null list of slot names; never empty
     */
    List<String> slotNames();

    /**
     * Assembles the final {@link DocumentNode} for the layout, given
     * the document header and the populated slot map.
     *
     * <p>Unknown slot entries (children added to a slot name not in
     * {@link #slotNames()}) are silently dropped by the implementation
     * — the layout is the source of truth for which slots are
     * rendered. Empty slots are also valid and emit either an empty
     * column (in multi-column layouts) or skip cleanly (single-column).</p>
     *
     * @param header non-null pre-rendered header node
     * @param slots  non-null slot map carrying the per-slot module
     *               nodes
     * @return composed root node ready to add to the active session
     * @throws NullPointerException if either argument is null
     */
    DocumentNode compose(DocumentNode header, SlotMap slots);
}
