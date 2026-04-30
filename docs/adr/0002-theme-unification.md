# ADR 0002 — Theme unification

- **Status:** Accepted (with phased migration)
- **Date:** 2026-04-30
- **Authors:** Artem Demchyshyn

## Context

GraphCompose ships two theme types in v1.5:

| Type | Package | Used by |
| --- | --- | --- |
| `BusinessTheme` | `com.demcha.compose.document.theme` | `InvoiceTemplateV2`, `ProposalTemplateV2`, `getting-started.md` quick-start, `addSection` recipe code, runnable examples |
| `CvTheme` | `com.demcha.compose.document.templates.theme` | `CvTemplateV1`, the CV template gallery (`PanelCvTemplateComposer`, ten `Cv*` template variants), `CvFileExample` |

The two types describe overlapping but not identical concerns:

- **`BusinessTheme`** is a canonical record exposing `DocumentPalette`,
  `SpacingScale`, `TextScale` (with `h1`/`h2`/`h3`/`body`/`caption`/
  `label`/`accent` slots), `TablePreset`, and an optional
  `pageBackground` colour. It uses **canonical types only** so authors
  who never touch the engine API can reach for it.
- **`CvTheme`** is a CV-specific record carrying eight numeric and
  colour fields (`primaryColor`, `secondaryColor`, `bodyColor`,
  `accentColor`, header / body fonts, name / header / body font sizes,
  spacing tokens). Its accessor methods return engine
  `com.demcha.compose.engine.components.content.text.TextStyle`
  values, which is the type the legacy CV composers consume.

Rendering the same data through both layers is awkward: the
business-themed templates and the CV templates can drift visually even
when consumers expect "one product look" because the two theme types
are independent.

## Decision

We adopt a **two-step unification** rather than a single big-bang
refactor:

1. **`CvTheme` becomes a thin adapter that can be derived from a
   `BusinessTheme`.** A new static factory
   `CvTheme.fromBusinessTheme(BusinessTheme)` materialises a
   `CvTheme` whose colours, fonts, and sizes are derived from the
   business theme's `palette()` + `text()` slots. Existing
   `CvTheme.defaultTheme()` / `timesRoman()` / `courier()` factories
   remain so every legacy caller compiles unchanged.

2. **CV templates and composers migrate to the bridge incrementally.**
   New CV-style templates that need theme parity with the
   business templates can take a `BusinessTheme` parameter and
   internally build their `CvTheme` via the new factory. Existing
   composers that hand-thread a `CvTheme` keep working until they're
   touched for unrelated reasons.

The alternative we considered ("Option B" — extract a common
`Theme` interface that both records implement) was rejected because:

- Both are records with different field counts and accessor names; a
  common interface would either be a thin marker (no real polymorphism
  benefit) or require a mid-level "ThemeView" with duplicated
  accessors — extra surface area without a corresponding consumer.
- The CV composers and the BusinessTheme composers have different
  semantic anchors (CV speaks in "name / section header / body /
  small body" while Business speaks in "h1 / h2 / h3 / body / caption /
  label / accent"). Forcing them under one interface loses the
  CV-specific vocabulary that 10 templates already rely on.

## Consequences

**Positive:**

- Authors who already use `BusinessTheme.modern()` for their invoice
  and proposal templates can instantly derive a matching `CvTheme`
  for their CV templates: `CvTheme.fromBusinessTheme(myTheme)`. The
  output looks visually consistent across all three document types.
- Migration is incremental — no template needs to change all at once.
  The bridge factory works whether the consumer eventually moves to a
  `BusinessTheme`-only path or sticks with `CvTheme`.
- The `BusinessTheme` API surface stays narrow. We did not need a
  new interface or wrapper.

**Negative / accepted trade-offs:**

- The two types still coexist. Code that wants to know "the project
  has a single theme" has to know about both. The bridge keeps them
  in sync but doesn't collapse them.
- The bridge's mapping decisions (which `palette()` slot drives which
  CV colour, which `text()` slot drives which CV font size) are
  judgement calls. They are documented in `CvTheme.fromBusinessTheme`
  and surface as values that callers can override after construction
  via `CvTheme`'s legacy factory pattern (build manually from
  primitives).

**Out of scope for v1.5:**

- Mass migration of the 10 CV templates to a `BusinessTheme`-driven
  composer. Existing templates keep working; new templates can use
  the bridge.
- A unified `theme.text().h1()` style for both BusinessTheme-driven
  and CvTheme-driven flows. The CV composers continue to call into
  CvTheme's CV-specific accessors (`nameTextStyle`,
  `sectionHeaderTextStyle`, etc.).

## Implementation order

1. Add `CvTheme.fromBusinessTheme(BusinessTheme)` factory.
2. Pin behaviour with `CvThemeBusinessThemeAdapterTest` —
   the derived theme uses the business palette / text fonts.
3. Document the bridge in `docs/recipes/themes.md` so readers know
   how to keep CV and business themes in sync.
4. Defer further migration to a follow-up release (per-template
   refactor when each template is otherwise touched).
