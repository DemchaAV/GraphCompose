# Keep-together pagination: blocks that never split

By default a vertical section flows across page boundaries: its children fill
the remaining space and continue on the next page. That is right for long
prose — and wrong for a card whose heading would be orphaned from the chart
or table below it.

`keepTogether()` makes a block paginate as a unit: when it does not fit in
the remaining page space **but fits on a fresh page**, the whole block
relocates instead of splitting.

```java
document.pageFlow()
        .addSection("ChartCard", section -> section
                .keepTogether()
                .softPanel(DocumentColor.WHITE, 8, 16)
                .addParagraph(p -> p.text("Quarterly revenue"))
                .chart(spec))                  // heading + chart move together
        .build();
```

Available on:

| Surface | Call | Keeps together |
|---|---|---|
| Section | `addSection(s -> s.keepTogether()…)` | any group of blocks/rows |
| Module | `module(m -> m.keepTogether()…)` | title + body |
| Timeline | `addTimeline(t -> t.keepTogether()…)` | the whole timeline |
| Timeline entries | `addTimeline(t -> t.keepEntriesTogether()…)` | each entry (marker + title + body); the timeline may still break *between* entries |

`keepEntriesTogether()` is the usual choice for CV experience sections: an
entry never splits mid-body, while a long history still spans pages.

Two boundaries to know:

- **Best-effort, not absolute.** A block taller than a full page still flows —
  nothing can keep what physically cannot fit on one page.
- **Default off.** Existing layouts (including the CV presets, which rely on
  sections flowing) are unchanged unless a block opts in.

`Row`, `LayerStackNode`, `ShapeContainerNode`, and `CanvasLayerNode` are
already atomic by design and never split — `keepTogether()` exists for the
*composites* (sections, modules, timelines) that flow by default.

## Keep a heading with its content: `keepWithNext()`

`keepTogether()` keeps a *whole* block on one page — wrong for a section
heading above a long, page-spanning body: the body never fits, so the request
is ignored and the heading can still strand at a page bottom, apart from what
it introduces (a boxed title torn from its background is the visible symptom).

`keepWithNext()` is the tool for that case. A section marked keep-with-next is
never left as the last block on a page when a sibling follows it: if the
section **plus the first line** of the following block would overflow the
remaining space (but fit on a fresh page), the section relocates to the next
page so the heading stays glued to its body.

```java
document.pageFlow()
        .addSection("ExperienceTitle", s -> s
                .keepWithNext()                       // title follows its body down
                .softPanel(DocumentColor.rgb(238, 240, 242), 4, 10)
                .addParagraph(p -> p.text("PROFESSIONAL EXPERIENCE")))
        .addSection("ExperienceBody", s -> s          // a long, page-spanning list
                .addParagraph(/* … many entries … */))
        .build();
```

The difference from `keepTogether()`: keep-with-next binds the heading to only
the **first following line**, not the whole body — so it works even when the
body spans several pages.

"First line" means the first line of text when the following block starts with
prose or list entries (the usual heading-over-body case). When it instead starts
with an indivisible unit (an image, a row) or a table/list, the heading is kept
with that whole first block. Consecutive keep-with-next sections relocate as one
run, so a multi-part heading (rule + banner + rule) moves together.

Two boundaries, mirroring `keepTogether()`:

- **Inert when nothing follows.** A trailing heading (no following sibling, or
  nothing that places a line on the page) is never relocated — there is no
  orphan to avoid.
- **Best-effort.** If the heading plus one following line cannot share a page
  at all, the heading flows in place rather than jumping to a page it still
  cannot share with its body.

Default off — sections without the opt-in flow exactly as before.
