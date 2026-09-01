# ADR 0017 — Page chrome: the text header/footer and the node zone coexist

- **Status:** Accepted
- **Date:** 2026-08-29
- **Authors:** Artem Demchyshyn

## Context

A running header or footer was `DocumentHeaderFooter`: three text slots, three
placeholder tokens (`{page}`, `{pages}`, `{date}`), drawn by
`PdfHeaderFooterRenderer` after the page was finished, and again by
`PptxChromeRenderer` for a deck. Three consequences followed from the band being
drawn outside the engine rather than by it:

- The font was hardcoded to standard-14 Helvetica in both renderers, so a band
  could not be typeset in the family the document uses.
- The declared `height` reserved nothing from the content area — it positioned
  the text and nothing else, so whether the body ran under the footer was decided
  by whichever page margin the author happened to pick.
- Nothing but text could go in the band. No badge, no link, no image, no layout.
- DOCX had no header or footer at all, because three slots and three tokens
  describe a band somebody paints rather than content Word can own.

`DocumentPageZone` addresses all four by making the band content: a function
from `PageContext` to a `DocumentNode`, laid out against a canvas the size of the
band and spliced into the compiled layout graph, the way
`DocumentPageBackgrounds` splices a fill. Every fixed-layout backend draws it
with no code of its own, and the semantic lane maps it onto a real `w:ftr` part.

That left the obvious follow-up: reimplement `DocumentHeaderFooter` on top of the
zone — its three slots are a row with a spacer — and delete the two chrome
renderers. One rendering path instead of two.

## Decision

**The two coexist. The text header/footer is not rewritten onto the zone, and
the standalone chrome renderers stay.**

`DocumentHeaderFooter` keeps its own positioning arithmetic and its own
renderers. `DocumentPageZone` is the path for anything new. An author picks one;
neither changes the other.

The rewrite was cancelled rather than deferred. It is not scheduled for the next
major release: should a future version unify the paths, that is a fresh decision
made on the evidence available then, not a commitment recorded here.

## Rationale

The rewrite's benefit is entirely internal — one code path instead of two — and
its cost is entirely external. The three slots are positioned today by arithmetic
inside `PdfHeaderFooterRenderer` (centre at
`marginLeft + (usableWidth - textWidth) / 2`, baseline at `height - fontSize`).
Positioned instead through `RowBuilder`, the same slots land a fraction of a
point elsewhere, and long text behaves differently: today the slots simply
overlap, a row compresses or wraps.

That is not an API break — `com.demcha.compose.engine.*` is the Internal tier
(see [ADR 0003](0003-api-stability-and-internal-marker.md) and
[api-stability.md](../api-stability.md)), so the renderers may be deleted in any
release without a deprecation window. It is an **output** change, and this
project ships `PdfVisualRegression` and `LayoutSnapshotAssertions` as public API:
pinning rendered output is a supported thing for a consumer to do. Moving every
existing footer to buy internal uniformity spends the user's baselines on our
tidiness.

Worth recording honestly: the duplication is not free. Adding font selection had
to be written twice, once in each renderer. That cost is bounded only while the
text band stays feature-frozen.

## Consequences

- **An existing header or footer renders as it did.** Its geometry is pinned by
  `ChromeGeometryGuardTest`, including the absolute baseline offsets, so a
  change to the older path has to say so out loud.
- **New capability goes to the zone, not to the text band.** This is what keeps
  the duplication cost at zero: nothing new needs writing twice. A feature
  request against `DocumentHeaderFooter` should be answered with the zone.
- **Two chrome renderers remain** in `render-pdf` and `render-pptx`. They are
  Internal and cost nothing to a consumer.
- **Revisiting is justified by evidence, not tidiness.** The conditions that
  would change the answer: the two paths drifting in behaviour (a fix landing in
  one and not the other), the text band acquiring features again, or a major
  release that is already moving rendered output for other reasons.
- **`DocumentHeaderFooter` documents the zone** so that staying on the older type
  is a choice rather than an accident.
