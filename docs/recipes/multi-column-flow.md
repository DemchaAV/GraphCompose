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
