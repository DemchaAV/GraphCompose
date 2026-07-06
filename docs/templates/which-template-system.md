# Which template system should I use?

**Short answer.** There is one: the [**layered**](v2-layered/README.md)
template surface — `com.demcha.compose.document.templates.{cv, coverletter,
invoice, proposal}`, every preset a final class with a
`create(BrandTheme)` factory returning a `DocumentTemplate<…>`. GraphCompose
2.0 ships no other template system.

> **Dependency.** These presets ship in the opt-in `graph-compose-templates` artifact — not
> bundled in `graph-compose`. Add it, or depend on `graph-compose-bundle`. See the
> [README install matrix](../../README.md#installation).

Through the 1.x line this page was a decision guide between two parallel
surfaces. On the 2.0 line the decision is gone; what remains here is the
naming history (so old commit messages and ADRs still make sense) and the
migration map for callers arriving from a pre-2.0 surface.

---

## 1. The naming, once

Two template generations coexisted through 1.x, with confusingly similar
names. Both older labels refer to surfaces that **no longer exist in 2.0**:

| Historical label | What it was | Fate |
|---|---|---|
| **"classic"** / "Templates v1.6" / "templates rebuild" (`cv.presets.*`, `CvSpec` + `CvBuilder` + `BusinessTheme`) | The 1.6 rebuilt canonical surface, documented in [`v1-classic/`](v1-classic/README.md) (now archived). | Removed in 2.0. Same-named layered presets replace it (§ 3). |
| **"Templates v2"** / "layered" (package suffix `cv.v2` etc.) | The layered architecture — *data / theme / components / widgets / presets* on `BrandTheme`, introduced by [ADR-0015](../adr/0015-layered-template-architecture.md). | **This is the 2.0 surface.** The `.v2` package suffix was dropped in 2.0 (`templates.cv.v2.*` → `templates.cv.*`); the docs folder keeps its historical [`v2-layered/`](v2-layered/README.md) name. |
| **Built-in `*TemplateV1` / `*TemplateV2`** (Invoice / Proposal / WeeklySchedule) | Standalone one-class templates on `BusinessTheme`, predating the layered families. | Removed in 2.0. Invoice / proposal are layered families now (§ 3); weekly schedule has no 2.0 template yet. |
| **Legacy PDF API** (`GraphCompose.pdf(...)`, `PdfComposer`, `com.demcha.compose.v2.*`, `com.demcha.templates.*`) | The original pre-1.6, PDF-direct authoring path. | Removed in 2.0. Migration target is the canonical DSL directly (§ 3). |

ADR references: [ADR-0011](../adr/0011-templates-v2-architecture.md)
introduced the classic surface; [ADR-0015](../adr/0015-layered-template-architecture.md)
introduced the layered one and superseded 0011.

---

## 2. Status matrix

| Surface | Status | Use for new code? | First read |
|---|---|---|---|
| **Layered templates** (`templates.cv.*`, `templates.coverletter.*`, `templates.invoice.*`, `templates.proposal.*` on `BrandTheme`) | **Stable** — the template surface | ✅ Yes | [v2-layered/quickstart](v2-layered/quickstart.md) |
| **Canonical DSL** (`GraphCompose.document()` + `DocumentSession` + builders) | **Stable** — the authoring substrate under the templates, and the direct path for custom shapes | ✅ Yes | [Main README — Hello world](../../README.md) |
| **Engine surface** (`com.demcha.compose.document.layout.*`, `com.demcha.compose.engine.*`, render handlers) | **Internal** — not part of the public contract; can change in any release | ❌ No | [ADR-0015](../adr/0015-layered-template-architecture.md) for the canonical/engine seam |

Choosing is simple: rendering a CV, cover letter, invoice, or proposal →
pick a layered preset. Anything else (report, brochure, custom shape) →
author directly on the canonical DSL, reusing `templates.core` widgets
where they fit.

---

## 3. Migrating a pre-2.0 caller

### Classic CV / cover-letter presets → layered

Every classic CV preset has a **same-named** layered replacement; the
migration is the theme + data-record swap:

1. **Theme.** `BusinessTheme.X()` → `BrandTheme.X()` (per-family factories,
   e.g. `BrandTheme.boxedClassic()`, `BrandTheme.invoiceModern()`).
2. **Data record.** `CvSpec` → `CvDocument` — strongly-typed sections
   (`ParagraphSection`, `SkillsSection`, `EntriesSection`, …) instead of a
   flat block list. The [contributor guide](v2-layered/contributor-guide.md)
   walks the section types end-to-end.

| Classic preset (removed) | Layered CV preset | Paired cover letter |
|---|---|---|
| `BlueBanner` | `BlueBanner` | `BlueBannerLetter` |
| `BoxedSections` | `BoxedSections` | `BoxedSectionsLetter` |
| `CenteredHeadline` | `CenteredHeadline` | `CenteredHeadlineLetter` |
| `ClassicSerif` | `ClassicSerif` | `ClassicSerifLetter` |
| `CompactMono` | `CompactMono` | `CompactMonoLetter` |
| `EditorialBlue` | `EditorialBlue` | `EditorialBlueLetter` |
| `EngineeringResume` | `EngineeringResume` | `EngineeringResumeLetter` |
| `Executive` | `Executive` | `ExecutiveLetter` |
| `ModernProfessional` | `ModernProfessional` | `ModernProfessionalLetter` |
| `MonogramSidebar` | `MonogramSidebar` | `MonogramSidebarLetter` |
| `NordicClean` | `NordicClean` | `NordicCleanLetter` |
| `Panel` | `Panel` | `PanelLetter` |
| `SidebarPortrait` | `SidebarPortrait` | `SidebarPortraitLetter` |
| `TimelineMinimal` | `TimelineMinimal` | `TimelineMinimalLetter` |
| _(layered-only)_ | `MinimalUnderlined`, `MintEditorial` | `MintEditorialLetter` |

### Built-in templates → layered families

| Removed built-in | 2.0 replacement |
|---|---|
| `InvoiceTemplateV1` / `InvoiceTemplateV2` | `templates.invoice.presets.ModernInvoice` — `create()` or `create(BrandTheme)`, data record `InvoiceDocumentSpec`. |
| `ProposalTemplateV1` / `ProposalTemplateV2` | `templates.proposal.presets.ModernProposal` — same shape, data record `ProposalDocumentSpec`. |
| `WeeklyScheduleTemplateV1` | No 2.0 template yet. The `templates.data.schedule` records still ship; author the rendering on the canonical DSL. |

### Legacy PDF API → canonical DSL

`GraphCompose.pdf(...)`, `PdfComposer`, `com.demcha.templates.*`, and
`com.demcha.compose.v2.*` have no 1:1 template equivalent — they were a
PDF-authoring shortcut, not templates. The migration target is the
canonical DSL directly: `GraphCompose.document(...)` + `pageFlow(...)` +
`DocumentSession.buildPdf()`. The
[v1.5 → v1.6 migration roadmap](../roadmaps/migration-v1-5-to-v1-6.md)
walks through the swap for the common shapes.

### Maven coordinates do **not** change in 2.0

The artifact stays `io.github.demchaav:graph-compose:<X>` on Maven
Central. The legacy JitPack URL (`com.github.DemchaAV:GraphCompose:v<X>`)
remains resolvable for callers pinned to v1.6.5 and earlier but is not
the documented install channel.

---

## 4. Cross-links

- **Layered (current) docs** —
  [README](v2-layered/README.md) · [Quickstart](v2-layered/quickstart.md) ·
  [Using templates](v2-layered/using-templates.md) ·
  [Authoring presets](v2-layered/authoring-presets.md) ·
  [Contributor guide](v2-layered/contributor-guide.md)
- **Architecture** —
  [ADR-0015 Layered template architecture](../adr/0015-layered-template-architecture.md) ·
  [API stability policy](../api-stability.md)
- **Historical (archived)** —
  [v1-classic README](v1-classic/README.md) · [v1-classic authoring](v1-classic/authoring.md) ·
  [ADR-0011 Templates v2 architecture](../adr/0011-templates-v2-architecture.md)
- **Migration roadmaps** —
  [v1.4 → v1.5](../roadmaps/migration-v1-4-to-v1-5.md) ·
  [v1.5 → v1.6](../roadmaps/migration-v1-5-to-v1-6.md)

---

*When a new preset or family lands, update § 2 (status matrix) and § 3
(migration table) in the same commit. This page is allowlisted in
`CanonicalSurfaceGuardTest` so it may name retired API tokens — keep
such mentions scoped to the migration guidance above.*
