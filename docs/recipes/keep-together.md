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
