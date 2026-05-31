# Which template system should I use?

**Short answer.** For any **new** code on GraphCompose 1.6.x and later, use
the [**layered**](v2-layered/README.md) template surface
(`com.demcha.compose.document.templates.cv.v2.*`, paired with
`*Letter` cover-letter presets in `…coverletter.v2.*`). The older
[**classic**](v1-classic/README.md) surface still ships, still works,
and stays supported through the 1.x line, but **the layered surface is
the path forward** and is the only template family that will be in
GraphCompose 2.0.

This page is the decision guide. It exists because the project ships
**two parallel canonical template surfaces** today, with confusingly
similar names, and a new contributor needs to know which one to read
and which one to write against.

---

## 0. The naming, once

The two surfaces have collided naming because the *codebase* and the
*docs* labelled them at different points in time:

| What you'll see | Where it lives | What it actually is |
|---|---|---|
| **"Templates v2" (in commit messages, ADR 0011, package names like `cv.v2`)** | `com.demcha.compose.document.templates.cv.v2.*` | The **layered** architecture — *data / theme / components / widgets / presets*, paired with `CvDocument` builder. Recommended. |
| **"Templates v1.6" / "templates rebuild"** | `com.demcha.compose.document.templates.cv.presets.*` | The 1.6 rebuilt canonical surface — `CvSpec` + `CvBuilder` + presets + `BusinessTheme`. Still supported. |
| **Folder `docs/templates/v1-classic/`** | docs only | Documents the **non-layered** surface (`cv.presets.*`). The doc folder name is *not* the same axis as the package's `v2` suffix. |
| **Folder `docs/templates/v2-layered/`** | docs only | Documents the layered surface (`cv.v2.*`). |

From here, this page uses one name per surface, consistently:

- **`classic`** → `cv.presets.*`, documented in `docs/templates/v1-classic/` — pre-layered, still supported.
- **`layered`** → `cv.v2.*` + `coverletter.v2.*`, documented in `docs/templates/v2-layered/` — recommended.

ADR references: [ADR-0011 templates-v2-architecture](../adr/0011-templates-v2-architecture.md) introduces the `classic` surface;
[ADR-0015 layered-template-architecture](../adr/0015-layered-template-architecture.md) introduces the `layered` one and supersedes 0011 for new template families.

---

## 1. Status matrix

| Surface | Status | Stability | Use for new code? | Will exist in 2.0? | First read |
|---|---|---|---|---|---|
| **Layered** (`cv.v2.*`, `coverletter.v2.*`) | **Recommended** | Stable (additive changes only) | ✅ Yes | ✅ Yes (canonical) | [v2-layered/quickstart](v2-layered/quickstart.md) |
| **Classic** (`cv.presets.*`, paired classic letter presets) | **Supported** | Stable, no new features | 🟡 Only if migrating an existing v1.5-era caller | ❌ Removed (full migration to layered) | [v1-classic/README](v1-classic/README.md) |
| **Built-in `*TemplateV2`** (Invoice / Proposal) | **Recommended** | Stable | ✅ Yes | ✅ Yes | [v1-classic/README](v1-classic/README.md) — InvoiceTemplateV2 / ProposalTemplateV2 sections (documented historically inside the v1-classic folder; pending a dedicated `built-ins/` section) |
| **Built-in `*TemplateV1`** (Invoice / Proposal / WeeklySchedule) | **Legacy** | Stable, no new features | ❌ No (use V2 versions; WeeklySchedule has no V2 yet) | ❌ Removed | (no top-level doc — see source Javadoc) |
| **Canonical DSL** (`GraphCompose.document()` + `DocumentSession` + `DocumentDsl`) | **Recommended** | Stable | ✅ Yes — required substrate for both template surfaces and direct authoring | ✅ Yes | [Main README — Hello world](../../README.md) |
| **Legacy PDF API** (`GraphCompose.pdf(...)`, `PdfComposer`, `com.demcha.compose.v2.*`, `com.demcha.templates.*`) | **Legacy** | Frozen — bug fixes only | ❌ No | ❌ Removed | `graphcompose-legacy-architect` skill (internal Claude / Codex artifact, not a repo page) |
| **Engine surface** (`com.demcha.compose.document.layout.*`, `com.demcha.compose.engine.*`, render handlers) | **Internal** | Stable in practice, not part of the public contract; can change in any minor release without a CHANGELOG entry | ❌ No | 🟡 Mostly — package boundaries will tighten further behind `@Internal` | `graphcompose-shared-engine-architect` skill (internal Claude / Codex artifact); [ADR-0015](../adr/0015-layered-template-architecture.md) for the canonical/engine seam |

> **Status definitions.** *Recommended* = the path GraphCompose 2.0 will
> ship; new code targets it. *Supported* = bug fixes + behaviour-preserving
> refactors only, no new features; exists through 1.x. *Internal* = engine
> surface, not part of the public contract; can change in any minor
> release without a CHANGELOG entry. *Legacy* = bug fixes only, removed
> in 2.0; do not start new code here.

---

## 2. Decision tree

```
I want to render a business document on GraphCompose.

├─ Is it a CV or cover letter?
│   ├─ Yes → use a `layered` preset (cv.v2 / coverletter.v2).
│   │       See [v2-layered/using-templates.md](v2-layered/using-templates.md).
│   │       Existing `classic` (cv.presets.*) caller? Read § 3 below; the
│   │       layered preset with the same name is a drop-in replacement
│   │       for almost every case.
│   │
│   └─ No → continue.
│
├─ Is it an invoice or proposal?
│   └─ Use `InvoiceTemplateV2` or `ProposalTemplateV2` (the `V2` variants).
│       The `V1` variants exist for backward compatibility; do not start
│       new code on them.
│
├─ Is it a weekly schedule?
│   └─ Use `WeeklyScheduleTemplateV1`. There is no V2 yet — the V1 API
│       will be re-shaped before 2.0 (no concrete plan yet); track in
│       the public release roadmap before adopting it long-term.
│
└─ It's something else (custom shape, brochure, report)?
    └─ Author directly on the canonical DSL: `GraphCompose.document()`
       + `pageFlow(...)` or `dsl().pageFlow(...)`. Reuse `BusinessTheme`
       and layered widgets (`SectionDispatcher`, `EntryCompactRenderer`,
       `RichParagraphRenderer`, `CardWidget`) wherever they fit; lift
       new reusable widgets into the `cv.v2/components` and
       `cv.v2/widgets` packages so the next template family can share
       them.
```

---

## 3. Migration table — `classic` → `layered`

Every CV preset that shipped in `cv.presets.*` has a same-named drop-in
replacement in `cv.v2.presets.*`. Migrating a caller is the import swap
plus a theme + data-record swap (introduced below):

```diff
-import com.demcha.compose.document.templates.cv.presets.NordicClean;
+import com.demcha.compose.document.templates.cv.v2.presets.NordicClean;

-// before: CvSpec + BusinessTheme
-NordicClean.create(BusinessTheme.nordicClean()).render(session, cvSpec);
+// after:  CvDocument + CvTheme — see the two shape changes below
+NordicClean.create(cvTheme).render(session, cvDocument);
```

Two real shape changes accompany the swap:

1. **Theme.** `BusinessTheme.X()` → `CvTheme.X()`. The CV-specific
   tokens (palette / typography / spacing) are split out so `cv.v2`
   themes don't carry invoice / proposal vocabulary they don't use.
2. **Data record.** `CvSpec` → `CvDocument`. The data shape becomes
   strongly-typed sections (`SectionGroup`, `EntryGroup`, `SkillGroup`,
   …) instead of a flat `Block` permit list. The
   [contributor-guide](v2-layered/contributor-guide.md) walks through
   the section types end-to-end.

### CV presets — one-to-one mapping

| `classic` (`cv.presets.*`) | `layered` (`cv.v2.presets.*`) | Cover letter (`coverletter.v2.presets.*`) | Notes |
|---|---|---|---|
| `BlueBanner` | `BlueBanner` | `BlueBannerLetter` | |
| `BoxedSections` | `BoxedSections` | `BoxedSectionsLetter` | Reference preset for the layered architecture. |
| `CenteredHeadline` | `CenteredHeadline` | `CenteredHeadlineLetter` | Layered-only in 1.6.5+; classic version is the rebuilt one. |
| `ClassicSerif` | `ClassicSerif` | `ClassicSerifLetter` | |
| `CompactMono` | `CompactMono` | `CompactMonoLetter` | |
| `EditorialBlue` | `EditorialBlue` | `EditorialBlueLetter` | |
| `EngineeringResume` | `EngineeringResume` | `EngineeringResumeLetter` | |
| `Executive` | `Executive` | `ExecutiveLetter` | |
| `ModernProfessional` | `ModernProfessional` | `ModernProfessionalLetter` | |
| `MonogramSidebar` | `MonogramSidebar` | `MonogramSidebarLetter` | Layered version uses `pageBackgrounds(...)` for sidebar chrome (multi-page-safe). |
| `NordicClean` | `NordicClean` | `NordicCleanLetter` | Layered version ships public `NordicClean.Options`. |
| `Panel` | `Panel` | `PanelLetter` | |
| `SidebarPortrait` | `SidebarPortrait` | `SidebarPortraitLetter` | Layered version uses `pageBackgrounds(...)`. |
| `TimelineMinimal` | `TimelineMinimal` | `TimelineMinimalLetter` | |
| _(no classic)_ | `MinimalUnderlined` | _(no letter yet)_ | Layered-only. |
| _(no classic)_ | `MintEditorial` | `MintEditorialLetter` | Layered-only (shipped in v1.6.5). |

If your caller uses a name on the left, the row tells you what to import
on the right and which cover-letter preset pairs with it.

### Built-ins (Invoice / Proposal / WeeklySchedule)

| V1 | V2 | Migration |
|---|---|---|
| `InvoiceTemplateV1` | `InvoiceTemplateV2` | Same data record (`InvoiceSpec`), same `BusinessTheme`. Renderer rewrite uses layered widgets; output is visually equivalent and snapshot-tested. |
| `ProposalTemplateV1` | `ProposalTemplateV2` | Same data record (`ProposalSpec`), same `BusinessTheme`. Same migration pattern as invoice. |
| `WeeklyScheduleTemplateV1` | _(no V2 yet)_ | Stay on V1 until a V2 ships. |

### Things that are **not** in either canonical surface (still legacy)

These live under `com.demcha.compose.v2.*`, `com.demcha.templates.*`, or
the `GraphCompose.pdf(...)` factory. They are the original (pre-1.6)
PDF-direct authoring path. They will be removed in 2.0:

- `GraphCompose.pdf(outputFile)` and `PdfComposer`.
- `com.demcha.templates.MainPageCV`, `MainPageCvDTO`, `ModuleYml`,
  `TemplateBuilder` and friends.
- `com.demcha.compose.v2.*` (engine-direct builders predating the
  canonical DSL).

If a caller still imports any of these, the migration target is
**directly the canonical DSL** (`GraphCompose.document()`) — there is no
1:1 equivalent in the template surfaces, because these classes were
never canonical templates, they were a PDF authoring shortcut. The
[migration-v1-5-to-v1-6 roadmap](../roadmaps/migration-v1-5-to-v1-6.md)
walks through the swap for the common shapes.

---

## 4. Deprecation inventory — 1.x → 2.0

Read this when planning long-lived code or when auditing dependencies
that may already use one of the slated-for-removal types. None of these
are deleted in 1.x; the work below is anti-roadmap and lives in the
private taskboard.

### Removed in 2.0

| Item | Reason | Replacement |
|---|---|---|
| `GraphCompose.pdf(...)` factory + `PdfComposer` | PDF-direct path predates the canonical DSL; bypasses layout and rendering invariants. | `GraphCompose.document(...)` + `DocumentSession.buildPdf()`. |
| `com.demcha.compose.v2.*` (the entire package) | Engine-direct builders predating canonical DSL. Mixes layout vocabulary with rendering vocabulary. | Canonical DSL surface (`document.api`, `document.dsl`, `document.node`, `document.style`). |
| `com.demcha.templates.MainPageCV`, `MainPageCvDTO`, `ModuleYml`, `TemplateBuilder` | The original CV template pre-rebuild. Replaced by `cv.presets.*` (classic) in 1.6, now slated to be replaced again by `cv.v2.*` (layered). | A `cv.v2.presets.*` preset, or direct canonical DSL authoring. |
| Entire `cv.presets.*` package (the `classic` surface) | Superseded by `cv.v2.presets.*` (layered). Every preset on the left has a same-named layered replacement (see § 3). | `cv.v2.presets.*`. |
| `DocumentSession.builder()` (deprecated alias) | Pre-rebuild builder entry point. | `GraphCompose.document()`. |
| `DocumentDsl.text(...)` (deprecated alias) | Pre-rebuild text shortcut. | `paragraph(...)` builders inside `pageFlow`. |
| `DocumentPalette.of(...)` (deprecated alias) | Pre-rebuild palette factory. | `DocumentPalette.from(...)` (or theme-specific factories). |
| PDF-specific chrome overloads on `BusinessTheme` | Coupled CV-specific tokens with PDF-specific decisions. | Layered `CvTheme` + render-time `pageBackgrounds(...)`. |

### Open questions for 2.0 (no decision yet)

- `WeeklyScheduleTemplateV1` — whether to ship a V2 along the layered
  architecture or to deprecate weekly schedule entirely.
- `InlineRow` / `SplittableRow` / `GridNode` decisions deferred from
  1.7 — may unblock magazine-style multi-column flow without needing a
  `GridNode` primitive. Tracked in the maintainers' internal release-
  readiness notes; will surface here once a public 1.8 engine-refactor
  roadmap is published.

### Maven coordinates do **not** change in 2.0

The library `pom.xml` artifact id stays `graphcompose`; JitPack
coordinates (`com.github.DemchaAV:GraphCompose:v<X>`) and the Maven
Central coordinates being introduced in 1.6.6 (`io.github.demchaav:graphcompose:<X>`)
both carry through to 2.0.

---

## 5. Cross-links

- **First-class architecture references**
  - [ADR-0011 Templates v2 architecture](../adr/0011-templates-v2-architecture.md) (the `classic` surface)
  - [ADR-0015 Layered template architecture](../adr/0015-layered-template-architecture.md) (the `layered` surface; supersedes 0011 for new families)
- **Layered (recommended) docs**
  - [README](v2-layered/README.md) · [Quickstart](v2-layered/quickstart.md) · [Using templates](v2-layered/using-templates.md) · [Authoring presets](v2-layered/authoring-presets.md) · [Contributor guide](v2-layered/contributor-guide.md)
- **Classic (supported) docs**
  - [README](v1-classic/README.md) · [Authoring](v1-classic/authoring.md)
- **Migration roadmaps**
  - [v1.4 → v1.5](../roadmaps/migration-v1-4-to-v1-5.md)
  - [v1.5 → v1.6](../roadmaps/migration-v1-5-to-v1-6.md)

---

*This page is maintained alongside the templates surfaces. When a new
preset, built-in, or deprecation lands, update §1 (status matrix) and
§3 (migration table) in the same commit. The `CanonicalSurfaceGuardTest`
documentation-coverage check enforces that no legacy API token leaks
into this doc.*
