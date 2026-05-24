# ADR 0012 — Nested list ergonomics: extend `ListNode` vs introduce `NestedListNode`

- **Status:** Accepted
- **Date:** 2026-05-07
- **Authors:** Artem Demchyshyn

## Context

`ListBuilder` and `ListNode` in v1.5 only model **one level** of
items. Authors compose sub-bullets through nested
`addSection(...)` or sibling paragraph calls — both lose marker
semantics (no per-depth bullet glyph), break pagination on
splitting, and force the caller to re-implement a depth indent
that any reasonable list primitive should provide.

The v1.6 roadmap (`docs/roadmaps/v1.6-roadmap.md` Phase A) lands real
nested-list authoring with the public API:

- `ListBuilder.addItem(String label, Consumer<ListBuilder> body)`
  — appends a list item with a label and a builder callback that
  scopes children.
- A new `ListItem` value type carrying `(label, marker, children)`.
- Per-depth marker resolution via
  `ListBuilder.markerFor(int depth, ListMarker)` overrides plus a
  built-in cascade default (`•` → `◦` → `▪` → `·`).

The architectural question for v1.6: should the nested
representation live in **`ListNode` itself** (record extension
with a new component) or in a **new sibling node**
(`NestedListNode`)?

## Decision

**Extend `ListNode`** with one additional record component
(`List<ListItem> nestedItems`) and keep the existing
`List<String> items` field for the flat path. Add an explicit
back-compat constructor matching the v1.4 / v1.5 11-component
shape so v1.5 callers and the internal `new ListNode(...)` sites
recompile unchanged.

```java
public record ListNode(
        String name,
        List<String> items,            // flat path (back-compat)
        List<ListItem> nestedItems,    // nested path (new)
        ListMarker marker,             // top-level marker (flat path)
        DocumentTextStyle textStyle,
        TextAlign align,
        double lineSpacing,
        double itemSpacing,
        String continuationIndent,
        boolean normalizeMarkers,
        DocumentInsets padding,
        DocumentInsets margin) implements DocumentNode {

    public ListNode { /* canonical: validate + copy-protect */ }

    /** Back-compat constructor matching the v1.5 11-component shape. */
    public ListNode(String name,
                    List<String> items,
                    ListMarker marker,
                    DocumentTextStyle textStyle,
                    TextAlign align,
                    double lineSpacing,
                    double itemSpacing,
                    String continuationIndent,
                    boolean normalizeMarkers,
                    DocumentInsets padding,
                    DocumentInsets margin) {
        this(name, items, List.of(), marker, /* … */);
    }
}
```

When `nestedItems` is empty (the flat case), the layout pipeline
runs unchanged. When `nestedItems` is non-empty, `prepareList`
flattens the tree depth-first into indent-prefixed paragraph
fragments and the existing flat-list rendering paginates and
emits them.

The flatten step uses **non-breaking spaces** (` `) per
nesting depth so the paragraph wrap pipeline preserves them —
Java's `Character.isWhitespace` intentionally excludes NBSP from
leading-trim (the paragraph wrapper strips leading whitespace
from the first token of each line, which would otherwise erase
depth indentation). The marker character at each depth is taken
from `item.marker()` if set, otherwise the per-depth override
from `markerFor(int, ListMarker)`, otherwise the built-in
cascade.

`ListBuilder` was restructured to keep a single
`List<ListItem> items` storage. `addItem(String)` now appends
a leaf `ListItem`; `addItem(String, Consumer)` flips a
`usedNestedAuthoring` flag and appends a nested `ListItem`.
`build()` branches on the flag — flat-only callers receive the
v1.5-shaped `ListNode` (`nestedItems = []`); any nested usage
produces the unified-tree shape with source order preserved
across mixed flat / nested entries.

## Consequences

### Positive

- **One node type for every list shape.** Any code that handles
  `ListNode` (templates, snapshots, custom renderers) continues
  to work without checking a second node type. Authors don't
  pick between `ListNode` and `NestedListNode`; they reach for
  one builder.
- **Source compatibility for v1.5 callers.** Both the public
  builder API and internal `new ListNode(...)` constructor
  shape stay valid via the explicit back-compat constructor.
- **Shared rendering.** The flatten-on-prepare strategy reuses
  the entire flat-list pipeline (pagination, wrapping, padding,
  fragment emission). Maintenance cost stays at one code path.
- **Source-order preservation.** Mixing
  `addItem(String)` and `addItem(String, Consumer)` in any
  order produces a tree where depth-0 entries appear in the
  exact order they were added.
- **Markdown-friendly cascade.** The default depth cascade
  (`•` → `◦` → `▪` → `·`) matches GitHub-flavoured Markdown
  rendering of a 3-level bullet list, so authors get the
  expected glyphs without any setter calls.

### Negative

- **`ListNode` grew one component.** The record now has 12
  components (was 11). Reflective tooling that hardcoded
  `ListNode.class.getRecordComponents().length == 11` breaks.
  No such tooling exists in-tree; the back-compat constructor
  shields source callers.
- **NBSP leakage to extracted text.** PDF text extraction
  through `PDFTextStripper` reports NBSP characters in nested
  list output rather than regular spaces. This is a deliberate
  trade-off: regular spaces would be stripped by the paragraph
  wrap pipeline and lose the depth indentation. Tools that
  post-process extracted PDF text need to normalise
  ` ` → ` ` if they want canonical whitespace.
- **`normalizeListItem` semantics widened.** When
  `normalizeMarkers = false`, the helper now returns the raw
  string (preserving leading whitespace) instead of the
  trimmed string. Only one in-tree caller (`CvModuleApiTest`)
  uses `normalizeMarkers(false)` and that test only validates
  the model property, not rendering, so the change is safe.
  The previous behaviour ("trim regardless of normalize flag")
  was unintentional and not documented.

### Alternatives considered

- **New `NestedListNode` type.** Cleaner separation of
  concerns at the node level but doubles the surface every
  `ListNode`-aware site has to handle (templates, layout,
  snapshots, render handlers). The shared-rendering benefit
  of extending `ListNode` outweighs the cosmetic upside.
- **Render nested items as a sequence of standalone
  `ParagraphNode`s with explicit `padding.left` per item.**
  Avoids the NBSP indent leak and would give wrap-aware
  hanging indent for free, but moves nested authoring out of
  the list pipeline — pagination would then split on
  paragraph boundaries rather than list-item boundaries,
  breaking the "list is splittable on item boundary"
  invariant. Deferred to v1.7 alongside the broader hanging-
  indent work for nested continuation lines.
- **Add `markerOverrides` as a 13th `ListNode` component.**
  Rejected because per-depth marker overrides are an
  authoring concern, not a render-time one. Baking the
  resolved marker into each `ListItem` at `build()` time
  keeps `ListNode` focused on the rendered shape.

## References

- v1.6 roadmap, Phase A:
  [`docs/roadmaps/v1.6-roadmap.md`](../roadmaps/v1.6-roadmap.md).
- ADR 0003 — API stability and `@Internal` marker:
  [`0003-api-stability-and-internal-marker.md`](0003-api-stability-and-internal-marker.md)
  (gate that allows new components on existing records).
- Tests:
  [`src/test/java/com/demcha/compose/document/dsl/ListBuilderNestedTest.java`](../../src/test/java/com/demcha/compose/document/dsl/ListBuilderNestedTest.java).
- Snapshot baseline:
  `src/test/resources/layout-snapshots/document/nested_list_three_levels.json`.
