# Pagination Ordering

This note explains one of the most important pagination rules in GraphCompose:
descendants must be paginated before their parent containers.

## Why ordering matters

During page breaking, a fixed leaf object may be moved to the next page instead of being split.
When that happens, the leaf can increase the effective size of its parent container.

That parent size change is applied through the normal compile flow:

1. the leaf detects that it does not fit on the current page
2. the leaf is shifted to the next page
3. the shift updates the parent container's resolved content size
4. the parent container must then compute its final `FragmentPlacement` using the updated size

If the parent container is processed before the child shift happens, the parent receives a stale
placement. The content size may later be correct, but the already-written placement height
will still be based on the old value. In practice, this looks like a container guide or span box
that does not extend to cover the final child position.

## The leaf-type ordering confusion

This issue can be easy to misdiagnose because it does not belong to the leaf renderable itself.

Atomic leaf nodes — an image, a shape, a barcode, a single-line paragraph — are all the
same shape here:

- they render a single box
- their `NodeDefinition` declares an atomic pagination policy, so `split` is never called
- when they do not fit the remaining page height, they relocate whole

So if one leaf container fails to expand while another appears correct, the first thing
to check is not the render implementation. The first thing to check is pagination order.

In one scenario, one child may happen to be processed before its parent container, so the
container placement is computed with the final child-driven size and the result looks correct.
In another scenario, a child may be processed after its parent container, which exposes the
bug immediately.

The difference is therefore often accidental ordering, not a semantic difference between the
leaf types.

## Correct rule

For pagination, GraphCompose should follow this ordering contract:

- any descendant must be processed before its ancestors
- unrelated entities can then be ordered by visual position
- if visual positions are equal, deeper entities should still win over shallower ones

This rule guarantees that parent containers see all child-induced page shifts before their own
`FragmentPlacement` is finalized.

## Current implementation shape

The rule is satisfied structurally rather than by a comparator. `LayoutCompiler.compile`
walks the semantic roots of the `DocumentGraph` in document order, threading a single
`CompilerState` that owns the page cursor:

- each node is prepared through its `NodeDefinition` into a `PreparedNode`, which
  carries its measured size
- a composite recurses into its children *before* its own placement is finalized, so a
  parent's box already reflects every child-induced page shift — the descendants-first
  requirement above is a property of the recursion, not of a sort
- a splittable node that does not fit the remaining page height is divided through
  `NodeDefinition.split`; an atomic one relocates whole
- placement produces `PlacedNode` and `PlacedFragment` records appended to one ordered
  list per pass

Because there is a single sequential emission pass, **emission order is render order** —
no backend re-sorts fragments to reconstruct z-order. Explicit stacking is resolved
locally where it is declared: `LayerStackGeometry` orders a layer stack's children by
`zIndex` with a stable sort, so equal values keep source order.

Both properties matter beyond PDF: the PPTX backend consumes the same fragment stream in
the same order, which is why shape stacking matches between the two outputs without any
backend-specific ordering rule.

## Practical symptom

If you see one of these behaviors, check pagination ordering first:

- a container guide box stops too early after a child is moved to the next page
- the resolved content size looks updated in logs, but the emitted fragment height still has the old value
- the same structural bug appears only for some leaf types or only in some align/margin setups

## Recommended debugging approach

When debugging a pagination tree:

1. turn on the `com.demcha.compose.engine.pagination` logger at `DEBUG` —
   `LayoutCompiler` emits `pagination.compile.start` (root count, inner height) and
   `pagination.compile.end` (pages, nodes, fragments, duration), which bracket the pass
   and tell you whether the page count itself is wrong
2. take a `DocumentSession.layoutSnapshot()` — it resolves the layout without rendering a
   byte, so you can inspect placement in a plain unit test instead of reading a PDF
3. compare a parent's resolved box against its children's: a parent that does not
   include a child's page-shift means the child was placed after the parent was finalized
4. check whether the node in question is atomic or splittable — a block that "jumps a
   page too early" is usually an atomic leaf that cannot divide, not an ordering bug

If step 3 shows the parent finalized first, the bug is ordering. If step 4 shows an
atomic leaf taller than the usable page height, it is a sizing problem and surfaces as
`AtomicNodeTooLargeException` rather than a silent misplacement.
