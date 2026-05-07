# ADR 0011 — Templates v2 architecture

- **Status:** Accepted (with Phase E.1 reopen caveat — see Consequences)
- **Date:** 2026-05-06
- **Authors:** Artem Demchyshyn

## Context

The v1.5 templates layer evolved into a known mess that blocked
contributors and forked easily on every visual change. The pain
points (recorded in `docs/private/templates-restructure-plan.md`
sec 1.1):

- **CV mixed with invoice / proposal in one folder.** All 14 CV
  presets, V1/V2 invoice and proposal templates, and the weekly
  schedule template all sat in `templates/builtins/`.
- **Meaningless class names.** `CvTemplateV1`, `BlueBannerCvTemplate`,
  `EditorialBlueCvTemplate`, `MonogramSidebarCvTemplate` — names that
  conveyed neither the visual style nor the domain.
- **One composer per template.** ~15 hand-coded composer classes
  under `templates/support/cv/` (300-700 LOC each), each
  reimplementing its own `addHeader()`, `addModule()`, `addSidebar()`
  helpers. A new visual variant required forking a 600-line file.
- **Header rebuilt per composer.** Every composer constructed its own
  contact line + link row in private methods. ~15 duplicate
  implementations of the same idea.
- **Spacing tokens scattered across three places.** `CvTheme.spacing`,
  `TemplateLayoutPolicy.rootSpacing`,
  `MINIMUM_TOP_LEVEL_MODULE_SPACING` hardcoded. No single source of
  truth.
- **Templates baked their own table.** `InvoiceTemplateV2` couldn't
  be themed because its line-items table was hardcoded in the
  composer.
- **No public builder.** Authors couldn't compose a custom CV from
  the existing components — the only path was forking a builtin
  composer.
- **`CvTemplate` interface privatised to CV data.** No
  `DocumentTemplate<S>` generic — every domain (CV / invoice /
  proposal / cover letter) needed its own marker interface.
- **Decorations baked into composers.** Dividers, panels, accent
  strips were inline in each composer rather than a shared library.
- **Layout coupled to style.** Each composer hardcoded its own
  column count and slot positions — no way to swap the layout
  without rewriting the composer.
- **14 presets with duplicates / near-copies.** `BlueBanner` ≈
  `Editorial`, `BoxedSections` ≈ `CenteredHeadline`. Hard to tell
  what's intentionally different.

The user's directive (May 2026): the templates layer is breakable
in v1.6 minor — v1.x SemVer "API stability" covers the engine, not
the templates. Few external users; rebuild from scratch under a
clean architecture.

## Decision

Rebuild the templates layer under a four-layer mental model
(Theme → Layout → Components → Spec) with per-domain folders and
flat copyable preset recipes. The new package layout:

```
templates/
  api/         DocumentTemplate<S>, SlotMap
  themes/      Spacing, Typography (token records)
  components/  Header, Module, MarkdownText
  blocks/      sealed Block hierarchy:
               ParagraphBlock, BulletListBlock,
               NumberedListBlock, IndentedBlock,
               KeyValueBlock, MultiParagraphBlock
  decorations/ Spacer, Divider, AccentStrip
  cv/
    layouts/   SingleColumn, TwoColumnSidebar, ThreeColumnMagazine
    presets/   14 flat copy-and-tweak preset classes
    builder/   CvBuilder
    spec/      CvSpec, CvHeader, CvModule
  coverletter/
    layouts/   LetterFormat
    presets/   14 paired letter presets
    builder/   CoverLetterBuilder
    spec/      CoverLetterSpec, CoverLetterHeader
  invoice/
    presets/   ModernInvoice (minimal v2 surface)
    builder/   InvoiceBuilder
    spec/      InvoiceSpec
  proposal/
    presets/   ModernProposal (minimal v2 surface)
    builder/   ProposalBuilder
    spec/      ProposalSpec
```

Key design contracts:

1. **`DocumentTemplate<S>` generic interface.**
   `compose(DocumentSession, S spec)` — replaces the old
   `CvTemplate` and the various per-domain marker interfaces.
2. **Flat preset recipes.** Each preset is one final class with one
   `static DocumentTemplate<S> create(BusinessTheme)` factory. No
   inheritance, no subclassing — copy the body of `create(...)`,
   tweak the builder calls, done.
3. **Reusable components.** `Header` (with right-aligned variant +
   fluent `withNameStyle / withContactStyle / withLinkStyle`
   overrides), `Module` (with per-instance `Style` record), `Block`
   sealed hierarchy (paragraph, bullet, numbered, indented, key-value,
   multi-paragraph) — all live in `templates/components/` and
   `templates/blocks/`.
4. **Shared `Spacing` tokens.** Single `Spacing.compact()` /
   `Spacing.comfortable()` / `Spacing.airy()` factories — replaces
   the three scattered `CvTheme.spacing` /
   `TemplateLayoutPolicy.rootSpacing` /
   `MINIMUM_TOP_LEVEL_MODULE_SPACING` knobs.
5. **Slot-based layouts.** `SingleColumn` / `TwoColumnSidebar` /
   `ThreeColumnMagazine` — the slot-based `CvBuilder` lets
   multi-column presets rearrange modules across slots through
   `.place(slot, "Module Name", ...)` calls.
6. **Markdown-aware bodies.** `MarkdownText.parse(text, baseStyle)`
   helper renders inline `**bold**` / `*italic*` / `_italic_`
   markers as `InlineRun` lists with the matching
   `DocumentTextDecoration`. Used by every CV / cover-letter preset
   paragraph body so spec authors carry inline emphasis without
   preprocessing.
7. **Per-preset `RECOMMENDED_MARGIN` constant** so the gallery
   example wires the right page margin without magic numbers at the
   call site.
8. **API break is intentional.** `templates/builtins` (14 V1 CV
   templates), `templates/support/cv` (15 composers),
   `templates/theme/CvTheme`, the `CvTemplate` interface — all
   removed. Migration table in `CHANGELOG.md` and
   `docs/migration-v1-5-to-v1-6.md`. Cinematic V2 templates
   (`InvoiceTemplateV2` / `ProposalTemplateV2`) are kept until the
   minimal v2 surface (`ModernInvoice` / `ModernProposal`) closes
   feature parity in a follow-up release.

## Consequences

### Positive

- **Per-domain folders** — CV / invoice / proposal / cover letter
  no longer share a flat `builtins/` namespace.
- **Meaningful preset names** — `ModernProfessional` / `NordicClean`
  / `BlueBanner` / `EditorialBlue` / `Executive` / `EngineeringResume`
  (was `TechLead`) / `Panel` (was `ProductLeader`) / `SidebarPortrait`
  / `MonogramSidebar` / `TimelineMinimal` / `BoxedSections` /
  `CenteredHeadline` / `ClassicSerif` / `CompactMono`.
- **`Spacing` single source of truth** — every preset reads spacing
  from one record.
- **Generic `DocumentTemplate<S>`** — same interface for CV, cover
  letter, invoice, proposal, future domains.
- **Markdown-aware bodies** — spec authors can emit `**bold**` /
  `*italic*` inline.
- **Examples gallery completeness** — `cv-<id>.pdf` and
  `cover-letter-<id>.pdf` per preset, regenerable via
  `examples/CvTemplateGalleryFileExample` /
  `CoverLetterTemplateGalleryFileExample`.
- **`CvHeader.jobTitle`** — first-class subtitle field for the
  presets that surface it (EditorialBlue / Panel / SidebarPortrait
  / MonogramSidebar).

### Negative — Phase E.1 reopen caveat

The first Phase E.1 pass shipped visually-broken renders. Every
CV preset rendered as a teal-tinted single-column
`ModernProfessional` clone — `NordicClean` lost its sidebar,
`BlueBanner` lost its banners, `MonogramSidebar` lost its
monogram badge. The visual parity gate
(`templates-restructure-plan.md` sec 6.2) was specified as a
PNG-rasterize pixel-diff with budget 2500 but **was never built**:
`ModernProfessionalVisualParityTest` was a smoke test (file-exists
check) and `PresetLayoutSnapshotTest` recorded baselines from the
new (broken) v2 renders without comparing against V1.

Phase E.1 was reopened in May 2026 and all 14 CV preset renders
+ 14 cover-letter pair renders were rebuilt against V1 visual
references. The reopen made an explicit trade-off:

- **13 of 14 CV presets are implemented as hand-coded
  `DocumentTemplate` subclasses** driving the canonical PageFlow
  DSL directly (≈ 400-700 LOC each) rather than thin recipes
  through the slot-based `CvBuilder`. Restoring V1 visual fidelity
  required components the v2 library hadn't grown yet
  (`Panel.softTinted`, `TwoColumnSidebar.tinted`,
  `SectionStyle.uppercaseRule`, `WorkEntryRenderer`).
- **Tech debt**: ≈ 5500 LOC of inlined parsing helpers
  (`parseWorkEntry`, `parseProjectEntry`, `contactParts`) and
  layout recipes. Architecturally identical to the V1 composers
  the restructure was supposed to retire.
- **Phase E.4** (deferred to v1.7) tracks the component library
  extension + preset refactor that closes this gap. See
  `docs/private/templates-restructure-plan.md` sec 12 and
  `docs/private/templates-v2-audit-remediation.md`.

### Other consequences

- **Visual parity gate is still missing.** The reopen replaced it
  with manual glance review through the project owner. A real
  pixel-diff test is on the v1.7 backlog.
- **Migration is breaking.** Anyone on
  `new CvTemplateV1()` / `new NordicCleanCvTemplate()` etc. must
  switch to the new factory (see migration table in `CHANGELOG.md`
  and `docs/migration-v1-5-to-v1-6.md`).
- **Cinematic V2 templates remain.** `InvoiceTemplateV2` /
  `ProposalTemplateV2` / `WeeklyScheduleTemplateV1` /
  `BuiltInCvTemplateSupport` stay in `templates/builtins/`. The
  builtins folder is not yet empty; final cleanup happens once
  `ModernInvoice` / `ModernProposal` close cinematic feature parity.
- **`templates/data/` is partially deferred.**
  `data/cv/CvDocumentSpec.java` and `data/coverletter/CoverLetterDocumentSpec.java`
  are dead code referenced only by tests; `data/invoice/*` /
  `data/proposal/*` / `data/schedule/*` are still consumed by V1/V2
  composers and stay until those are retired.

## Status

- Implemented through Phase A → Phase G in May 2026.
- Phase E.1 reopened May 2026 to recover V1 visual fidelity. Tech
  debt acknowledged; refactor scheduled for v1.7 (Phase E.4).
- Visual parity gate remains a deferred item. Manual glance review
  is the current process.
