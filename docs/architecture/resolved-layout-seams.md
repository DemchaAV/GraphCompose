# Resolved-layout seams

Two capabilities let a built-in feature use geometry the layout has already worked out.
They look alike and are not interchangeable, and the difference is *when* they run:

```
ResolvedLayoutPass       geometry is already laid out  ->  add drawing
ResolvedHorizontalBand   resolved earlier in the same compile  ->  influences later measurement
```

Both are `@Internal` engine plumbing. Neither names a feature; the timeline is the first
consumer of each and appears below only as an example.

---

## A. Resolved layout pass — drawing from settled geometry

A node wrapped in a `LayoutAnchorNode` leaves one non-visual fragment per page it occupies,
carrying that page's slice of its border box. After the document is compiled, a
`ResolvedLayoutPass` reads those anchors and contributes fragments of its own.

- It runs **after** layout. It can draw; it cannot move anything, change a width, or add a
  page — a fragment for a page the document does not have is refused.
- Passes are discovered from the anchors themselves: an anchor's owner that is also a pass
  is a feature saying it has something to draw. Nothing is registered.
- Ownership is object identity. Two features that anchor alike are still two features.

*Example consumer:* the timeline rail. Marker anchors give the line its x, entry anchors give
it its two ends on each page, and the pass contributes one line fragment per occupied page.

## B. Resolved horizontal band — measuring inside a settled column

A row resolves where each of its columns starts and how wide it is — from points, from shares
of what is left, or from a mixture — and normally forgets the arithmetic. Wrapped in a
`HorizontalBandsNode`, it records each slot as `ResolvedHorizontalBand{x, width}` under an
identity; a later `HorizontalBandContentNode` naming the same identity is laid out inside one
of them.

- It runs **during** the same compile, and the band is applied **before** the consumer is
  measured. That order is the whole point: the width decides the wrapping, the wrapping
  decides the height, and the height decides the pagination. A post-layout pass is too late
  to change any of them.
- The consumer stays an ordinary vertical block, so it splits across pages normally and keeps
  one x and one width on every page it reaches.
- Nothing becomes splittable that was not: a row is still laid out on one page.
- Fixed and weighted columns behave identically, because the band is read after the row
  resolved it rather than recomputed from the sizing.
- Storage is per compile — `CompilerState` is rebuilt for every pass — and keyed by object
  identity. There is no registry and nothing survives a document.
- Everything else fails closed and says which part of the arrangement is wrong: a column
  nobody published, content that comes before its row, two rows under one identity, a column
  the row does not have, a column that resolved to nothing, content in a fixed slot.

*Example consumer:* a timeline entry's body under `markerOnRail()`. The entry's header row
publishes its columns; the body is laid out in the content one, so the rail — which now runs
through the axis column — has only markers left to pass through.

### Why the band is not a second pass

A block whose horizontal origin comes from a column resolved earlier in the same compile is
safe only because preparation is lazy and in document order: when the body is measured, the
row above it has already been laid out. Nothing is compiled twice — relocation is decided
before a block is compiled, by comparing its measured height against what is left of the page,
so there is no abandoned attempt to leave stale state behind.

---

## Paint order

The engine has no z-index. Draw order is list order, and the driver splices contributions into
one list:

```
page background
  <  under-body additions        (immediately before the contributing feature's own content)
  <  body: containers, fills, text, markers
  <  over-body additions         (after the whole body)
  <  zone chrome (header / footer)
```

`UNDER_BODY` means **under the contributing feature's own content**, not at the front of the
page's fragment list. The distinction is not academic: a fragment placed at the front also
sits beneath the fill of whatever the feature is inside, so a rail inside a filled panel was
painted first and covered a moment later — present in the geometry, absent from the page, and
invisible to every assertion that reads coordinates. The splice point is now the first
fragment the feature's own anchors produced on that page; a pass that anchored nothing on a
page keeps the front of the list, which is what a page-wide backdrop wants.
