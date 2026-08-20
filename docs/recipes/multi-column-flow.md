# Multi-column flow — columns that continue on the next page

A **row** places its children side by side in one band, and the band is
atomic: it must fit on the page it starts on. That is right for a row of
cells, and wrong for a document body. A two-column layout built from a row
can hold exactly one page of content, and the moment it holds more the
compiler throws `AtomicNodeTooLargeException` — there is nowhere for the
overflow to go.

A **column flow** places the same columns and lets each one break where it
runs out of page:

```
  page 1                    page 2
  ┌────────┬─────────┐      ┌────────┬─────────┐
  │ side   │ main    │      │ side   │ main    │
  │ …      │ …       │  →   │ …cont. │ …cont.  │
  └────────┴─────────┘      └────────┴─────────┘
```

```java
flow.addColumnFlow("Body", body -> body
        .gap(18)
        .weights(0.72, 1.28)
        .addColumn("Sidebar", side -> side
                .spacing(6)
                .addParagraph(p -> p.text("Skills, education, languages…")))
        .addColumn("Main", main -> main
                .spacing(8)
                .addParagraph(p -> p.text("Profile, experience, projects…"))));
```

## Which one do I want?

| | `addRow` | `addColumnFlow` |
|---|---|---|
| Shape | one band, children side by side | columns side by side |
| Pagination | atomic — the whole band moves, or throws | each column breaks and continues |
| Use for | a header bar, a label/value pair, a card strip | a document body, a sidebar layout |
| Children | paragraphs, images, sections… | columns only (a section or container) |

If the content is bounded and belongs together on one page, a row is the
simpler node and stays the right answer.

## How it paginates

Each column is an ordinary vertical flow. That is the whole mechanism: a
section already spans pages, because the compiler places its children one at
a time and any child may start a new page. A column flow gives each column
its own page cursor, starting where the flow starts, and rejoins them at the
end — so everything inside a column (paragraphs, tables, nested sections)
breaks exactly as it does anywhere else.

The flow ends on the last page **any** column reached, and whatever follows
continues below the longest column. A column that finished two pages earlier
does not pull the next block up.

## Columns are the children

A column is a vertical container — a section or a container — because that
is what paginates. Passing anything else is rejected at build time rather
than laid out oddly:

```java
body.addColumn(someParagraph);   // IllegalArgumentException — wrap it in a column
```

## Decoration and chrome

The flow itself has no fill or border. A column that wants a panel is a
section with a fill, and the engine already repeats a section's fill on
every page that section spans:

```java
body.addColumn("Sidebar", side -> side
        .fillColor(DocumentColor.rgb(244, 246, 248))
        .padding(DocumentInsets.of(12)));
```

Chrome that must reach the page edge — a full-height sidebar band, a rule
down the gutter — belongs in a [page background](page-backgrounds.md), which
paints on every page by definition and does not care where the content
broke.

## Safe areas on continuation pages

A column's padding is an edge of the **column**, not of each page. It is
reserved once, at the top of the page the column opens on and the bottom of
the page it closes on. Every page in between — and the top of every page the
column continues onto — gets nothing from it.

The page margin is the inset that applies once per page, so in an ordinary
document this never comes up: the margin is already holding content away from
the paper edge on every page, and the padding is decoration on top of it. It
shows up in a **full-bleed** layout. Setting the margin to zero so a page
background can reach the paper edge gives up the safe area on all four sides,
when only the two horizontal ones had to go — and the first line of page 2
lands inside the band most printers cannot reach:

```
  page 1                    page 2
  ┌────────┬─────────┐      ┌────────┬─────────┐
  │        │         │      │ side…  │ main…   │ ← first line on the trim
  │ side   │ main    │      │ …cont. │ …cont.  │
  └────────┴─────────┘      └────────┴─────────┘
    ↑ the column padding held this page down, and only this page
```

Restore the top of the margin on the continuation pages only — a page-margin
rule is the one inset the engine applies per page:

<!-- doc-example: id=column-flow-continuation-safe-area mode=method imports=com.demcha.compose.GraphCompose,com.demcha.compose.document.api.DocumentPageSize,com.demcha.compose.document.api.DocumentSession,com.demcha.compose.document.api.PageMarginRule,com.demcha.compose.document.style.DocumentInsets,java.util.List -->
```java
try (DocumentSession session = GraphCompose.document()
        .pageSize(DocumentPageSize.A4)
        .margin(DocumentInsets.zero())        // full bleed on all four sides…
        .create()) {
    // …but half an inch of safe area at the top of page 2 onward
    session.pageMargins(List.of(
            PageMarginRule.from(2, new DocumentInsets(36, 0, 0, 0))));
}
```

Keep the other three edges of the margin you chose, so the layout stays
full-bleed horizontally and the page backgrounds — which are ratios of the
*page*, not of the content box — keep bleeding on every page. Page 1 is
deliberately not covered: a body's first page owns its own top edge, and a rule
that covered it would push that whole page down.

Which page and which edge is a design decision rather than an engine one, so
the ready-made version lives in `templates`:
`ContinuationSafeArea.applyTo(session, 2, ContinuationSafeArea.PRINTER_SAFE_TOP)`
derives the rule from `session.margin()`, names the first page to inset (`2`
for a body that opens the document, `3` when a cover comes first), and does
nothing at all when the margin already provides the safe area — so a template
can call it unconditionally without turning an ordinary document into a
per-page one, or deleting rules its caller set. `pageMargins(...)` replaces
rather than merges, so a template that wants rules of its own writes them
together rather than calling both.

It guards the **top** edge only. Content still flows to the bottom of every
page it fills, which in a full-bleed layout is the trimmed bottom edge. A
bottom safe area is a bottom inset on `margin(...)` itself rather than a rule
like this one, because unlike the top it has to apply to page 1 as well — and
giving page 1 less height changes where it breaks.

A rule that moves only the top or bottom margin costs nothing extra to lay out:
per-page margins are resolved through a fixed point only when a page's *width*
can differ, and a vertical-only rule leaves one width for the whole document.

Keeping the horizontal edges is not only about the bleed. Column widths are
resolved once, at the flow's entry, so a rule that narrowed the pages the flow
continues onto would inset the *page* without narrowing the columns drawn on
it — see [Widths](#widths).

## Widths

Widths come from `weights` (or split evenly when you pass none) and are
resolved **once**, at the flow's entry. Every page uses the same ones: a
column that changed width halfway down would not read as one column, and the
layout's fixed point requires the geometry to be a pure function of where
the flow started. In a document with per-page margins that means the flow
keeps its entry page's widths on the pages it continues onto, as any nested
block does.

## What it does not do

- **Balance columns.** Each column flows independently; the engine does not
  even out their lengths.
- **Keep content together across columns.** `keepTogether` and
  `keepWithNext` work inside a column, which is where they mean something.
  On the flow itself `keepTogether` is ignored — a node that spans pages by
  construction cannot relocate whole. A `keepWithNext` heading placed
  *above* the flow works normally: it is kept with the flow's first line,
  not with the whole flow.
- **Break every column at once.** A page break inside a column breaks that
  column; its neighbours keep flowing.
- **Live inside a row slot or a stack layer.** A column flow needs to
  advance pages, and both of those layers are pinned to one page. Putting
  one there is rejected rather than laid out over the top of what follows.
