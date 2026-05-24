# ADR 0013 — Composed table cell content

- **Status:** Accepted
- **Date:** 2026-05-07
- **Authors:** Artem Demchyshyn

## Context

`DocumentTableCell` in v1.4 / v1.5 only carried plain text:
`(List<String> lines, DocumentTableStyle style, int colSpan,
int rowSpan)`. Real reports want richer content inside a cell — a
paragraph with a styled status keyword, a small inline icon row,
or even a sub-table.

Authors who needed that today either:

1. Pre-rendered the paragraph to flat string lines and lost
   inline styles, links, and markdown.
2. Composed the table fragment manually with the engine API
   (which v1.5 retired from the public surface).

The v1.6 roadmap (`docs/roadmaps/v1.6-roadmap.md` Phase B) opens the cell
to any composable `DocumentNode`, with two-pass cell measurement
and pagination preserving row-by-row behaviour.

The architectural questions for v1.6 are:

1. **Cell shape** — extend `DocumentTableCell` with an optional
   `content` field, or introduce a new sealed `TableCellContent`
   hierarchy with `Lines` and `NodeContent` variants?
2. **Layout integration** — hand the table layout helper a
   `PrepareContext` so it can prepare child sub-trees, or stand
   up a parallel "composed-table" definition?
3. **Render dispatch** — special-case `ParagraphNode` in the PDF
   table render handler, or recurse through the standard
   `NodeDefinition` pipeline?

## Decision

### Cell shape — extend `DocumentTableCell`

Add a new optional 5th component `DocumentNode content` to
`DocumentTableCell`. Plain-text callers get back-compat through
explicit 4-arg, 3-arg, and 2-arg constructors that delegate to
the canonical 5-arg form with `content = null`. A new
`DocumentTableCell.node(DocumentNode)` factory mirrors the
existing `text(...)` and `lines(...)` factories.

```java
DocumentTableCell rich = DocumentTableCell.node(
    new ParagraphNode("Notes", "**Important** *italic* notes",
        DocumentTextStyle.DEFAULT, TextAlign.LEFT, 0.0,
        DocumentInsets.zero(), DocumentInsets.zero()));
```

When `content` is non-null the cell renders the child node
inside its bounds; the cell's own `lines` field is unused. When
`content` is null the cell behaves exactly like the v1.5
plain-text shape.

A sealed `TableCellContent` hierarchy was rejected because every
`DocumentTableCell`-aware site (templates, snapshots, custom
renderers) would have to switch on the variant — doubling the
surface area for a purely additive feature.

### Layout integration — thread `PrepareContext` through `TableLayoutSupport`

`TableLayoutSupport.resolveTableLayout` gains a `PrepareContext`
parameter (nullable when a caller knows the table is plain-text
only). Before row-height resolution, the helper walks every
logical cell and, for each `DocumentTableCell.hasComposedContent()
== true` entry, calls
`prepareContext.prepare(cell.content(), BoxConstraints.unboundedHeight(cellInnerWidth))`
to obtain a `PreparedNode<?>`. The prepared height feeds the
two-pass row-height pass through a new
`naturalCellHeight(LogicalCell, style, measurement, preparedContents)`
helper that branches on `hasComposedContent()` at the cell
level.

The prepared children are returned alongside the resolved
layout in a new `ResolvedTableLayoutWithContents` record, then
attached to `PreparedTableLayout` as a third record component
(`Map<CellKey, PreparedNode<?>> preparedContents`).
`PreparedTableLayout` keeps a 2-arg back-compat constructor that
defaults the map to empty.

`sliceTablePreparedNode` (used by pagination) takes the prepared
map, subsets it to the slice's row range, and remaps row keys
to the fragment's local indices so prepended header rows keep
their original positions while body rows shift by
`(prependHeaderRowCount - fromInclusive)`.

### Render dispatch — recursion via `FragmentContext.emitChildFragments`

`FragmentContext` gains a default
`emitChildFragments(PreparedNode<E>, FragmentPlacement)` method
that throws `UnsupportedOperationException`.
`DocumentLayoutPassContext` overrides it to look up the child's
`NodeDefinition` in the registry it already holds and dispatch
to `definition.emitFragments(child, this, placement)`.

`emitTableFragments` now takes `FragmentContext ctx` (was just
`FragmentPlacement placement`). For each composed cell the helper
constructs a `FragmentPlacement` at the cell's content area
(inside cell padding), calls
`ctx.emitChildFragments(preparedChild, childPlacement)`, then
**translates** the returned fragments into the table's local
coordinate system: `fragment.localX += cellLocalX +
padding.left()`. The translation is necessary because the
`LayoutCompiler` converts a fragment's local coordinates to
absolute by adding the `emitTableFragments` caller's placement
— the table's, not the child's. Translating once at emit time
keeps the framework's placement model intact while honouring
the cell's offset.

The PDF table render handler is **unchanged**: it iterates each
cell's `lines` field as before. Composed cells have an empty
`lines` list (the `node(...)` factory sets it to `List.of()`),
so the existing line-iteration loop emits nothing for the cell's
text body. The cell's borders and fill are still painted from
the `TableResolvedCell.style()` data. The composed child's
fragments render via their own already-registered handler
(`PdfParagraphFragmentRenderHandler` for paragraph children,
etc.).

## Consequences

### Positive

- **Any registered `NodeDefinition` works inside a cell.** A
  paragraph, a list, a layer-stack, even a sub-table renders
  correctly because emit dispatches through the registry. New
  custom node types light up automatically.
- **Two-pass measurement honours real child geometry.** The cell's
  natural height equals the prepared child's measured height
  plus cell padding — no flat-text approximation.
- **Pagination unchanged.** The row-atomic split contract holds:
  composed cells keep their child sub-tree on a single row, and
  the row's row-span / repeat-header semantics are preserved.
- **Plain-text cells fully back-compat.** Existing tables that
  use `text(...)` / `lines(...)` go through the same code path
  they did in v1.5 — `cellNaturalHeight` falls through to the
  line-based formula when `content == null`.
- **PDF render handler stays text-focused.** No special-case
  branch for composed cells; the renderer doesn't know about
  child sub-trees, the framework's recursion handles it.

### Negative

- **`DocumentTableCell` grew one component.** Reflective tooling
  that hardcodes the record arity needs an update; in-tree
  callers go through the back-compat constructors.
- **`emitTableFragments` is one parameter wider.** External
  callers of `emitTableFragments(prepared, placement)` need to
  switch to `emitTableFragments(prepared, ctx, placement)`. No
  external callers exist outside the `TableDefinition` wrapper,
  but this would be a source-incompatible change for anyone who
  forked the helper.
- **Coordinate-translation step in
  `emitComposedCellFragments`.** Because the
  `LayoutCompiler` resolves fragment coordinates relative to the
  emitting node's placement (the table), composed cell fragments
  are translated by the cell offset before being returned. This
  is brittle — if the framework's placement model evolves, the
  translation might drift. Documented inline.
- **Composed cell content cannot split across pages.** A row
  with a tall composed child stays atomic; if the child is
  taller than a page, it overflows. The same constraint applies
  to text-only cells today (a row with N lines of plain text is
  also atomic on the row), so this is consistent.

### Alternatives considered

- **Sealed `TableCellContent` hierarchy.** Rejected — additive
  shape change is purely surface noise; the field gate
  (`hasComposedContent()`) gives the same compile-time
  ergonomics with less ceremony.
- **Special-case `ParagraphNode` in the PDF render handler.**
  Rejected — would only deliver paragraph-in-cell, missing
  layer-stack / list / sub-table support that the roadmap
  explicitly listed. The recursion path delivers all
  `NodeDefinition`-backed types in one shot.
- **Build composed-table as a parallel composite definition (à
  la `LayerStackDefinition`).** Rejected — would require
  rebuilding `TableDefinition` from scratch and breaking the
  existing row-fragment payload that the PDF handler depends
  on. The recursion path keeps `TableDefinition` shape intact
  and extends it additively.

## References

- v1.6 roadmap, Phase B:
  [`docs/roadmaps/v1.6-roadmap.md`](../roadmaps/v1.6-roadmap.md).
- ADR 0011 — Templates v2 architecture (consumer of composed
  cells in v1.7+):
  [`0011-templates-v2-architecture.md`](0011-templates-v2-architecture.md).
- ADR 0012 — Nested list ergonomics (sibling Phase A feature
  that lands the same recursion-friendly shape on lists):
  [`0012-nested-list-evolution.md`](0012-nested-list-evolution.md).
- Tests:
  [`src/test/java/com/demcha/compose/document/table/TableCellComposedContentTest.java`](../../src/test/java/com/demcha/compose/document/table/TableCellComposedContentTest.java).
- Snapshot baseline:
  `src/test/resources/layout-snapshots/document/table_cell_with_paragraph.json`.
