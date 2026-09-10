# GraphCompose Documentation

Use these docs as a path while learning and as a catalogue afterwards. You do not
need to understand the engine, module layout, or ADRs to generate documents.

If you have not rendered anything yet, begin with the [root README](../README.md).

## The learning path

Follow the path only as far as your current task requires:

1. **Render one PDF:** [Your first document](first-document.md).
2. **Add the blocks you need:** [Recipes](recipes.md) explains where text, tables,
   charts, icons, images, cards, rows, backgrounds, and canvases fit.
3. **Reuse a business-document design:** [Use a built-in template](templates/v2-layered/using-templates.md)
   for an invoice, proposal, CV, or cover letter.
4. **Protect the result:** [Testing your document](operations/test-your-document.md)
   adds deterministic layout snapshots and pixel-level visual diffs.
5. **Run it in a backend:** [Production rendering](operations/production-rendering.md)
   covers streams, concurrency, failure handling, and operations.

Stop there if you are a library user. Continue to
[Contributing](../CONTRIBUTING.md), the [architecture overview](architecture/overview.md),
and [extension guide](contributing/extension-guide.md) only when you are changing
GraphCompose itself.

## Go directly to a task

| I need to… | Read |
|---|---|
| Add text, a list, table, chart, timeline, image, icon, emoji, or barcode | [Content and data recipes](recipes.md#content-and-data) |
| Build columns, cards, clipping, overlapping layers, backgrounds, or a canvas | [Layout and visual recipes](recipes.md#layout-and-visual-composition) |
| Add a header, footer, page number, watermark, link, bookmark, or contents page | [Page behaviour recipes](recipes.md#page-behaviour-output-and-development) |
| Inspect layout boxes or create a page preview while developing | [Developer tools and output](recipes.md#page-behaviour-output-and-development) |
| Protect a document with snapshots and visual diffs | [Testing your document](operations/test-your-document.md) |
| Render an invoice or proposal from data | [Using templates](templates/v2-layered/using-templates.md) |
| Render a CV with my own data | [CV template quickstart](templates/v2-layered/quickstart.md) |
| Design a custom CV style | [Authoring presets](templates/v2-layered/authoring-presets.md) |
| Upgrade a pre-2.0 caller | [2.0 migration guide](migration/v2.0.0-modules.md) |
| Add a new template family | [Template contributor guide](templates/v2-layered/contributor-guide.md) |
| Add a node or backend handler | [Extension guide](contributing/extension-guide.md) → [Package map](architecture/package-map.md) |
| Operate GraphCompose in production | [Production rendering](operations/production-rendering.md) → [Performance](operations/performance.md) → [Logging](operations/logging.md) |

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
- **[recipes.md](recipes.md)** — the catalogue: every recipe page and what it covers.
- **[recipes/rich-text.md](recipes/rich-text.md)** — styled runs, links, inline images, SVG icons, emoji, shapes, and checkboxes.
- **[recipes/lists.md](recipes/lists.md)** — flat and nested lists with custom markers and spacing.
- **[recipes/charts.md](recipes/charts.md)** — native vector bar/line/area/pie charts.
- **[recipes/tables.md](recipes/tables.md)** — columns, structured cells, spans, zebra rows, totals, and repeated headers.
- **[recipes/timelines.md](recipes/timelines.md)** — timelines, marker/rail geometry, dated entries, and pagination controls.
- **[recipes/images.md](recipes/images.md)** — image sources, sizing, fit modes, rows, and cards.
- **[recipes/barcodes.md](recipes/barcodes.md)** — QR and common barcode formats, tinting, quiet zones, and placement.
- **[recipes/keep-together.md](recipes/keep-together.md)** — blocks that never split at a page break.
- **[recipes/shapes.md](recipes/shapes.md)** — cards, dividers, lines, ellipses, images.
- **[recipes/shape-as-container.md](recipes/shape-as-container.md)** — shapes that hold child content.
- **[recipes/transforms.md](recipes/transforms.md)** — rotation, scaling, skewing.
- **[recipes/themes.md](recipes/themes.md)** — custom themes.
- **[recipes/pdf-chrome.md](recipes/pdf-chrome.md)** — metadata, watermarks, headers, footers, page zones, and protection.
- **[recipes/in-pdf-navigation.md](recipes/in-pdf-navigation.md)** — anchors, links, page references, bookmarks, and tables of contents.
- **[recipes/streaming.md](recipes/streaming.md)** — streaming PDFs to HTTP responses.
- **[recipes/snapshot-testing.md](recipes/snapshot-testing.md)** — layout regression baselines in consumer projects.
- **[recipes/docx-export.md](recipes/docx-export.md)** — semantic DOCX output and capability fallbacks.
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
- **[adr/0017-page-chrome-two-paths.md](adr/0017-page-chrome-two-paths.md)** — why the text header/footer and the node page zone coexist instead of one replacing the other.

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
