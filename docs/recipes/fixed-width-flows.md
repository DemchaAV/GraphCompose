# Fixed-width flows: a narrow box in a wide column

A vertical flow — a section, a module, the root page flow — is measured
against the width its parent offers. That is right for body content, and
wrong for a callout card, a sidebar aside, or a signature block that should
stay narrow while the page around it stays wide.

`fixedWidth(points)` pins the horizontal axis and leaves the vertical one
alone:

```java
document.pageFlow()
        .addSection("Callout", section -> section
                .fixedWidth(240)                       // the box is 240pt wide
                .softPanel(DocumentColor.WHITE, 8, 12) // fill, radius, padding
                .addParagraph("Wraps inside 216pt of inner width."))
        .build();
```

The box is measured, decorated and placed at exactly 240pt. Its children wrap
inside `240 − padding.horizontal()`, so text re-flows into the narrower
column instead of overflowing it.

## Width is fixed, height is not

This is a fixed **width**, not a fixed box. The height stays the natural,
content-driven measurement it always was:

| Change | Width | Height |
|---|---|---|
| Add a paragraph | unchanged | grows |
| Narrow the fixed width | as requested | grows (more wrapped lines) |
| Content shorter than the box | still the full fixed width | shrinks |

There is no `fixedHeight` counterpart, and pagination is untouched: a
fixed-width section flows and relocates exactly as it did before.

## Padding is inside the fixed width

The requested width is the **outer** width. Padding eats into it rather than
being added on top, so a `fixedWidth(240).padding(20)` card occupies 240pt of
the column and gives its children 200pt.

## A request wider than the parent is clamped

Asking for more width than the surrounding region can give resolves to the
region's width rather than overflowing it or failing layout:

```java
section.fixedWidth(900)   // in a 360pt column → placed at 360pt
```

Clamping changes the reported width only, never the line breaking underneath
it: the content wraps at the same width it would have without the call.

Nesting clamps against the *parent box*, not the page — a `fixedWidth(320)`
section inside a `fixedWidth(200).padding(10)` parent is placed at 180pt.

Under per-page margins the cap is per page: a box that spans onto a page with
a narrower content column is capped to that column rather than keeping the
width of the page it started on.

## Where it applies

| Surface | Call |
|---|---|
| Section | `addSection(s -> s.fixedWidth(240)…)` |
| Module | `module(m -> m.fixedWidth(240)…)` |
| Root page flow | `pageFlow(page -> page.fixedWidth(200)…)` |

Two boundaries to know:

- **Default off.** A flow that never calls `fixedWidth` keeps the
  shrink-to-fit measurement it always had — an unpadded section still reports
  the width of its widest child, capped at the column.
- **Bleed still wins.** On an edge declared through `bleed(...)` the
  background reaches the trimmed page edge, because that is the point of
  asking for it.
- **One case is currently off.** A flow used as a *row column* that also
  carries a horizontal `margin` is placed narrower than it asked for, because
  the row subtracts that margin twice before the width is resolved. The
  underlying row defect predates this feature (it misplaces unconstrained
  boxes too) and is being fixed separately; a fixed-width column with no
  horizontal margin is unaffected.

`fixedWidth` rejects NaN, infinity, zero and negative values outright, so a
computed width that went wrong fails at the call rather than at layout time.
