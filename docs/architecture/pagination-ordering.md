# Pagination Ordering

This note explains one of the most important pagination rules in GraphCompose:
descendants must be paginated before their parent containers.

## Why ordering matters

During page breaking, a fixed leaf object may be moved to the next page instead of being split.
When that happens, the leaf can increase the effective size of its parent container.

That parent size change is applied through the normal entity update flow:

1. the leaf detects that it does not fit on the current page
2. the leaf is shifted to the next page
3. the shift updates the parent container's `ContentSize`
4. the parent container must then compute its final `Placement` using the updated size

If the parent container is processed before the child shift happens, the parent receives a stale
`Placement`. The `ContentSize` may later be correct, but the already-written `Placement.height`
will still be based on the old value. In practice, this looks like a container guide or span box
that does not extend to cover the final child position.

## The Circle vs Image confusion

This issue can be easy to misdiagnose because it does not belong to the leaf renderable itself.

`Circle` and `ImageComponent` are both fixed leaf renderables:

- they render a single box
- they are not `Breakable`
- when they do not fit, they are moved as a whole

So if a circle container fails to expand while an image container appears correct, the first thing
to check is not the render implementation. The first thing to check is pagination order.

In one scenario, the image child may happen to be processed before its parent container, so the
container placement is computed with the final child-driven size and the result looks correct.
In another scenario, a circle child may be processed after its parent container, which exposes the
bug immediately.

The difference is therefore often accidental ordering, not a semantic difference between image and
circle rendering.

## Correct rule

For pagination, GraphCompose should follow this ordering contract:

- any descendant must be processed before its ancestors
- unrelated entities can then be ordered by visual position
- if visual positions are equal, deeper entities should still win over shallower ones

This rule guarantees that parent containers see all child-induced page shifts before their own
`Placement` is finalized.

## Current implementation shape

The current engine implementation does not enforce this rule with a pairwise ancestor comparator.
Instead it builds one deterministic traversal context for the whole pass through `LayoutTraversalContext`:

- `ParentComponent` provides the authoritative parent relation
- `Entity.children` provides canonical sibling order
- the page breaker runs a priority-based topological walk where only nodes with no unresolved children can enter the ready queue
- within that ready queue, unrelated nodes are ordered by `ComputedPosition.y`, then depth, then UUID

This keeps the algorithm fast on larger trees because it does not repeatedly walk parent chains during sorting.
It also keeps the rule renderer-agnostic: pagination still reasons over engine layout data, not PDF-specific output state.

## Practical symptom

If you see one of these behaviors, check pagination ordering first:

- a container guide box stops too early after a child is moved to the next page
- `ContentSize` looks updated in logs, but `Placement.height` still has the old value
- the same structural bug appears only for some leaf types or only in some align/margin setups

## Recommended debugging approach

When debugging a pagination tree:

1. log the `PageBreaker` processing order
2. compare parent and child `ComputedPosition.y`
3. check whether a child shift updates parent `ContentSize`
4. verify that the parent `Placement` is created after that update, not before it

If step 4 is false, the bug is usually ordering, not rendering.
