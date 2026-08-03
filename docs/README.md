# GraphCompose Documentation

Comprehensive docs for the engine, the templates, and the
contribution workflow. Below is the map — pick your path.

If you're new to GraphCompose entirely, start at the
[**root README**](../README.md) for the project overview, then come
back here.

---

## 🧭 By persona — start here

| You are… | Read |
|---|---|
| **New to GraphCompose** — what is it, how do I render my first PDF | [Your first document](first-document.md) → [Getting started](getting-started.md) |
| **Author rendering an invoice or proposal** | [Templates v2 (layered) — using templates](templates/v2-layered/using-templates.md) |
| **Author rendering a CV** with your own data | [Templates v2 (layered) — quickstart](templates/v2-layered/quickstart.md) |
| **Designer / author** wanting a custom visual style for CVs | [Templates v2 (layered) — authoring presets](templates/v2-layered/authoring-presets.md) |
| **Maintainer of a pre-2.0 caller** (classic `*Spec` + builder templates, removed in 2.0) | [Which template system? — migration map](templates/which-template-system.md) |
| **Contributor adding a new template family** to the library | [Templates v2 (layered) — contributor guide](templates/v2-layered/contributor-guide.md) |
| **Contributor extending the engine** (new node type, new backend handler) | [Extension guide](contributing/extension-guide.md) → [Package map](architecture/package-map.md) |
| **Operator** running GraphCompose in production | [Production rendering](operations/production-rendering.md) → [Performance](operations/performance.md) → [Logging](operations/logging.md) |

---

## 📁 By category

### Getting started
- **[first-document.md](first-document.md)** — the five-minute path from an empty project to a rendered PDF.
- **[getting-started.md](getting-started.md)** — DSL vs templates, first-render walk-through, decision tree.
- **[capabilities.md](capabilities.md)** — one-glance map of every feature with its stability tier and guide link.
- **[diagrams.md](diagrams.md)** — visual decision diagrams (authoring path, layout, output, lifecycle).
- **[troubleshooting.md](troubleshooting.md)** — symptom-first fixes for common gotchas: stray `?` glyphs, silent DOCX drops, optional-dependency `NoClassDefFoundError`, running the bundled examples.

### Templates
- **[templates/business-templates.md](templates/business-templates.md)** — invoice & proposal templates: the compose-first contract, end to end, on the layered `ModernInvoice` / `ModernProposal` surface.
- **[templates/v2-layered/](templates/v2-layered/)** — the template surface (CV is the reference implementation): `data` / `components` / `widgets` / `presets` per family, over the shared `templates.core.theme`.
- **[templates/v1-classic/](templates/v1-classic/)** — 🗄️ archived: the classic spec/builder/presets surface removed in 2.0; kept for pre-2.0 callers.

### Output backends
- **[architecture/backend-capability-matrix.md](architecture/backend-capability-matrix.md)** — what each render backend supports, per capability. The source of truth for PDF vs PPTX fidelity.
- **[../render-pptx/README.md](../render-pptx/README.md)** — `graph-compose-render-pptx`: editable PowerPoint decks from the same session that prints the PDF (`@Beta`, first shipped in 2.1.0).
- **[api-stability.md](api-stability.md)** — stability tier per package, and what a tier promises.

### Architecture
- **[architecture/overview.md](architecture/overview.md)** — high-level system architecture (engine + DSL + templates + backends).
- **[architecture/lifecycle.md](architecture/lifecycle.md)** — the document lifecycle from `GraphCompose.document(...)` through `buildPdf()`.
- **[architecture/pagination-ordering.md](architecture/pagination-ordering.md)** — how nodes are paginated and ordered.
- **[architecture/package-map.md](architecture/package-map.md)** — what's in which package.
- **[architecture/canonical-legacy-parity.md](architecture/canonical-legacy-parity.md)** — per-feature authoring coverage of the canonical API, refreshed for the 2.1 line. The recipes, the capabilities catalogue and the troubleshooting guide all link into it.

### Operations
- **[operations/production-rendering.md](operations/production-rendering.md)** — server-side rendering, streaming, thread safety.
- **[operations/performance.md](operations/performance.md)** — perf characteristics + tuning.
- **[operations/benchmarks.md](operations/benchmarks.md)** — how to run benchmarks; reference numbers.
- **[operations/logging.md](operations/logging.md)** — logger configuration, what each logger emits.
- **[operations/layout-snapshot-testing.md](operations/layout-snapshot-testing.md)** — snapshot-based layout regression testing.

### Contributing
- **[contributing/extension-guide.md](contributing/extension-guide.md)** — add a new node type, backend handler, or theme primitive.
- **[architecture/pagination-ordering.md](architecture/pagination-ordering.md)** — how nodes are paginated and ordered, for contributors working on layout / measurement.
- **[contributing/release-process.md](contributing/release-process.md)** — versioning, tag procedure, Maven Central publication.

### Migrations & roadmap
- **[migration/v2.0.0-modules.md](migration/v2.0.0-modules.md)** — the current upgrade guide: the 1.x → 2.x module split, the one dependency-level break, and every removed API with its replacement.
- **[migration/v1.8.0-fonts.md](migration/v1.8.0-fonts.md)** — the fonts artifact split, still relevant to anyone adding `graph-compose-fonts`.
- **[../ROADMAP.md](../ROADMAP.md)** — direction for the line after 2.1.
- **[roadmaps/post-2.0-engineering.md](roadmaps/post-2.0-engineering.md)** — internal refactors, scale, and tooling deferred past the 2.0 line.

<details>
<summary><b>Historical documentation</b> — shipped roadmaps and superseded minor-to-minor upgrade guides</summary>

Kept for anyone stepping through the 1.x line one minor at a time. Nothing here describes the current API.

- [roadmaps/v1.6-roadmap.md](roadmaps/v1.6-roadmap.md) — the v1.6 "expressive" roadmap (shipped).
- [roadmaps/migration-v1-8-to-v1-9.md](roadmaps/migration-v1-8-to-v1-9.md) · [v1-7-to-v1-8](roadmaps/migration-v1-7-to-v1-8.md) · [v1-6-to-v1-7](roadmaps/migration-v1-6-to-v1-7.md) · [v1-5-to-v1-6](roadmaps/migration-v1-5-to-v1-6.md) · [v1-4-to-v1-5](roadmaps/migration-v1-4-to-v1-5.md)

</details>

### Recipes (cookbook-style howtos)
- **[recipes.md](recipes.md)** — index of all recipes (coverage map in [recipes/README.md](recipes/README.md)).
- **[recipes/charts.md](recipes/charts.md)** — native vector bar/line/area/pie charts.
- **[recipes/keep-together.md](recipes/keep-together.md)** — blocks that never split at a page break.
- **[recipes/shapes.md](recipes/shapes.md)** — cards, dividers, lines, ellipses, images.
- **[recipes/shape-as-container.md](recipes/shape-as-container.md)** — shapes that hold child content.
- **[recipes/transforms.md](recipes/transforms.md)** — rotation, scaling, skewing.
- **[recipes/tables.md](recipes/tables.md)** — tabular layouts.
- **[recipes/themes.md](recipes/themes.md)** — custom themes.
- **[recipes/streaming.md](recipes/streaming.md)** — streaming PDFs to HTTP responses.
- **[recipes/extending.md](recipes/extending.md)** — extension patterns by example.
- **[recipes/page-backgrounds.md](recipes/page-backgrounds.md)** — per-page fills: sidebars, bands, layered tints.
- **[recipes/layered-page-design.md](recipes/layered-page-design.md)** — page background vs. row vs. layer stack vs. canvas.
- **[recipes/absolute-placement.md](recipes/absolute-placement.md)** — pixel-precise canvas placement.
- **[font-coverage.md](font-coverage.md)** — WinAnsi limits, `●` vs `•`, and glyph fallback.

### Architecture Decision Records (ADRs)
Numbered, dated decisions about non-trivial design choices. Read these
when you need to understand *why* a piece of the system looks the way
it does.

- **[adr/0001-shape-as-container.md](adr/0001-shape-as-container.md)** — shape nodes as content containers.
- **[adr/0002-theme-unification.md](adr/0002-theme-unification.md)** — single canonical theme model.
- **[adr/0003-api-stability-and-internal-marker.md](adr/0003-api-stability-and-internal-marker.md)** — public-API guarantees + `@Internal` marker.
- **[adr/0004-pdf-handler-spi-extension.md](adr/0004-pdf-handler-spi-extension.md)** — PDF render handler SPI.
- **[adr/0011-templates-v2-architecture.md](adr/0011-templates-v2-architecture.md)** — the v1.6 templates restructure (spec/builder/presets/themes); **superseded** for CV + cover letter by [0015](adr/0015-layered-template-architecture.md).
- **[adr/0012-nested-list-evolution.md](adr/0012-nested-list-evolution.md)** — nested list rendering evolution.
- **[adr/0013-composed-table-cell.md](adr/0013-composed-table-cell.md)** — composed table cell model.
- **[adr/0014-controlled-absolute-placement.md](adr/0014-controlled-absolute-placement.md)** — controlled absolute placement strategy.
- **[adr/0015-layered-template-architecture.md](adr/0015-layered-template-architecture.md)** — the layered `templates.cv` / `templates.coverletter` authoring model (current standard); supersedes the preset/builder portion of 0011.

> **ADR numbering gap (0005–0010)** is intentional — those numbers
> were reserved during a v1.5 restructure that landed under ADR 0011
> instead of multiple smaller records. No deleted ADRs.

### Showcase website (separate from docs)
- The public showcase website is **not** documentation — it lives in
  [`web/`](../web/) (static GitHub Pages site) and is documented by its own
  [`web/README.md`](../web/README.md). Kept out of `docs/` on purpose so the two
  don't tangle.

### Archive
- **[archive/](archive/)** — old migration guides and roadmaps kept
  for historical reference. Not part of the live doc set.

---

## 🔗 Quick links

- [Project root README](../README.md)
- [CONTRIBUTING.md](../CONTRIBUTING.md)
- [CHANGELOG.md](../CHANGELOG.md)
- [Examples gallery](../examples/README.md)
- [Live showcase site](https://demchaav.github.io/GraphCompose/)
