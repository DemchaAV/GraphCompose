package com.demcha.compose.document.layout;

import java.util.List;

/**
 * Strategy interface that abstracts the page bookkeeping a placement helper
 * needs: where it lands ({@link #pageIndex()}), what global lists it appends
 * to, the prepare/fragment contexts to forward to definitions, and whether it
 * is allowed to advance to a new page.
 *
 * <p>Two implementations exist:</p>
 * <ul>
 *   <li>{@link MutatingPlacementContext} — wraps the live
 *       {@code CompilerState} used by the page-flow path; reads
 *       {@code pageIndex()} from {@link CompilerState#pageIndex} and lets
 *       callers advance the page cursor when a node overflows.</li>
 *   <li>{@link FixedSlotPlacementContext} — pins the page index and
 *       forbids advancement; used by row slots and stacked layers that lay
 *       out into a single fixed page band.</li>
 * </ul>
 *
 * <p>Helpers that only need to know "which page do I land on?" can take this
 * type and stay agnostic of the surrounding pagination strategy.</p>
 *
 * @author Artem Demchyshyn
 */
public sealed interface PlacementContext
        permits MutatingPlacementContext, FixedSlotPlacementContext {

    /**
     * Active page index for placements scheduled through this context.
     *
     * @return the active page index
     */
    int pageIndex();

    /**
     * Canvas the placement is happening on.
     *
     * @return the layout canvas
     */
    LayoutCanvas canvas();

    /**
     * Prepare-phase context used to (re)measure children.
     *
     * @return the prepare context
     */
    PrepareContext prepareContext();

    /**
     * Fragment-emission context forwarded to {@code emitFragments}.
     *
     * @return the fragment context
     */
    FragmentContext fragmentContext();

    /**
     * Mutable list of placed semantic nodes; helpers append to this.
     *
     * @return the mutable list of placed semantic nodes
     */
    List<PlacedNode> nodes();

    /**
     * Mutable list of placed render fragments; helpers append to this.
     *
     * @return the mutable list of placed render fragments
     */
    List<PlacedFragment> fragments();

    /**
     * Whether this context can move on to a fresh page.
     *
     * @return {@code true} if this context can advance to a fresh page
     */
    boolean canAdvancePage();

    /**
     * Advances to a new page if the strategy allows it.
     *
     * @throws IllegalStateException for fixed-slot contexts where page
     *         advancement is meaningless
     */
    void advancePage();

    /**
     * Marks the current page as visited so the final
     * {@code totalPages} calculation is correct. No-op for fixed-slot
     * contexts; the parent flow already accounted for the page.
     */
    void touchPage();
}
