# ADR 0015 — Layered template architecture (cv.v2 / coverletter.v2)

- **Status:** Accepted
- **Date:** 2026-05-28
- **Authors:** Artem Demchyshyn

## Context

ADR 0011 reorganised templates into a per-domain "Templates v2" surface
(`templates/cv/{presets,builder,spec,layouts}`, `templates/coverletter/…`)
built on `CvSpec` / `CoverLetterSpec` data records, `BusinessTheme`, and
flat copy-and-tweak preset classes. That removed the v1.5 mess (one
600–700-line composer per template), but two limits remained:

- **Preset classes still re-implemented header / contact / section
  rendering.** Each preset carried its own `addHeader`, contact row, and
  body dispatch — the duplication ADR 0011 set out to kill kept creeping
  back at the preset layer.
- **Style, layout, and data were not cleanly separated.** A visual
  re-skin still meant editing preset code; there was no single token
  source a preset read from.

A refined **layered** architecture was prototyped under `cv/v2`, proved
out across all 14 CV presets (with a pixel-parity migration from the
Gen-2 presets), then extended to all 14 cover letters under
`coverletter/v2` (which reuses the CV theme + components so a CV and its
paired letter render as a matched set).

## Decision

Adopt the **layered** template architecture as the canonical
template-authoring pattern. A template family is built in five layers:

1. **data** — typed input records (`CvDocument`, `CoverLetterDocument`,
   reusing `CvIdentity`); no rendering logic.
2. **theme** — `CvTheme` = palette + typography + spacing + decoration
   tokens; the only place colour / font / spacing literals live.
3. **components** — shared stateless renderers (`SectionDispatcher`,
   `RichParagraphRenderer`, `MarkdownInline`, `CvTextStyles`,
   `LetterBody`, …) reused across presets and families.
4. **widgets** — composable visual blocks (`Headline`, `ContactLine`,
   `Masthead`, `CardWidget`, `SectionHeader`, …).
5. **presets** — thin orchestrators: a `create()` / `create(CvTheme)`
   factory plus a `compose()` that lays out the page-flow and delegates
   to components / widgets. No re-implemented parsing, no duplicated
   headers.

Presets are exposed through the generic `DocumentTemplate<T>` contract.
`cv.v2` is the reference implementation; `coverletter.v2` is the paired
family.

The earlier Gen-2 surface
(`templates/cv/{presets,builder,spec,layouts}` and the equivalent
`templates/coverletter/{presets,builder,spec,layouts}`) is **deprecated**
(`@Deprecated(since = "1.7.0", forRemoval = true)`) and scheduled for
removal in a future major. It keeps compiling and working until then —
existing callers are not broken.

## Consequences

- **One documented authoring path.** New template families follow
  `docs/templates/v2-layered/` (quickstart · using-templates ·
  authoring-presets · contributor-guide). A new preset is a thin
  orchestrator, not a forked composer.
- **Re-skin without code edits.** A new visual flavour is a new
  `CvTheme.<brand>()` factory; the preset is unchanged.
- **Naming overlap resolved.** Gen-2 package-info prose previously also
  called itself "Templates v2", colliding with the `cv.v2` folder name.
  Deprecating Gen-2 and correcting its package-info removes the
  ambiguity; "the v2 / layered surface" now unambiguously means
  `cv.v2` / `coverletter.v2`.
- **Showcase + examples render the layered surface.** The CV and
  cover-letter gallery examples and the committed README previews are
  generated from `cv.v2` / `coverletter.v2`.
- **Not yet ported:** invoice, proposal, and schedule remain on the
  Gen-2 / builtins surface (`BusinessTheme`-based) and are **not**
  deprecated. Porting them to the layered architecture is future work.
- **Supersedes the preset / builder / spec portion of ADR 0011** while
  keeping its domain-folder split (cv / coverletter / invoice /
  proposal / schedule).
