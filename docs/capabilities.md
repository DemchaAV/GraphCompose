# Capabilities

A one-glance map of what GraphCompose can do, the main API for each,
and its stability tier. "Stability" rows use the tiers defined in the
[API stability policy](api-stability.md); "Guide" links to the page that
shows it in context.

This is a feature catalogue, not the contract — the
[API stability policy](api-stability.md) is authoritative for what each
tier promises, and [canonical ⇄ legacy parity](architecture/canonical-legacy-parity.md)
tracks what is `Partial` or `Planned`.

---

## Authoring

| Capability | Main API | Stability | Guide |
|---|---|---|---|
| Open a document session | `GraphCompose.document(...)` → `DocumentSession` | Stable | [Your first document](first-document.md) |
| Describe content in reading order | `pageFlow(...)`, `module(...)`, `addSection(...)` | Stable | [Getting started](getting-started.md) |
| Maintained document templates | `ModernInvoice`, `ModernProposal`, the `templates.cv.*` / `templates.coverletter.*` preset galleries | Stable | [Templates](templates/which-template-system.md) |
| Reusable building blocks (helpers) | helper methods / widgets over the DSL | Stable | [Diagrams](diagrams.md#choose-your-authoring-path) |
| Custom node / backend | `NodeDefinition`, render-handler SPI, `FixedLayoutBackend` | Extension SPI (`@Beta`) | [Extending](recipes/extending.md) |

## Content

| Capability | Main API | Stability | Guide |
|---|---|---|---|
| Paragraphs & rich inline text | `addParagraph(...)`, `addRich(...)`, `RichText` | Stable | [Recipes — rich text](recipes.md) |
| Lists (flat & nested) | `addList(...)`, `ListBuilder` | Stable | [Recipes](recipes.md) |
| Tables (spans, zebra, totals, repeat header) | `addTable(...)`, `DocumentTableCell` | Stable | [Advanced tables](recipes/tables.md) |
| Raster images | `addImage(...)`, fit modes | Stable | [Shapes & images](recipes/shapes.md) |
| Vector shapes, dividers, lines | `addShape(...)`, `addLine(...)`, `addEllipse(...)` | Stable | [Shapes](recipes/shapes.md) |
| Charts (bar / line / pie) | `chart(ChartSpec...)`, `ChartData` | Stable | [Charts](recipes/charts.md) |
| Barcodes & QR | `addBarcode(...)` | Stable | [Recipes](recipes.md) |

## Layout

| Capability | Main API | Stability | Guide |
|---|---|---|---|
| Columns that still flow | `addRow(row -> row.weights(...))` | Stable | [Layered page design](recipes/layered-page-design.md) |
| Page-wide fills / bands | `pageBackground(...)`, `pageBackgrounds(...)` | Stable | [Page backgrounds](recipes/page-backgrounds.md) |
| Overlap & alignment | `addLayerStack(...)` | Stable | [Layered page design](recipes/layered-page-design.md) |
| Shape-as-container | `addContainer(...)`, `addCircle(...)`, `addEllipse(...)` | Stable | [Shape as container](recipes/shape-as-container.md) |
| Fixed (x, y) placement | `addCanvas(w, h, canvas -> canvas.position(...))` | Stable | [Absolute placement](recipes/absolute-placement.md) |
| Bleed to page edge | `bleedToEdge(...)` | Stable | [Page backgrounds](recipes/page-backgrounds.md) |
| Transforms (rotate / scale) | `DocumentTransform` | Stable | [Transforms](recipes/transforms.md) |

## Output & testing

| Capability | Main API | Stability | Guide |
|---|---|---|---|
| Write a PDF file | `buildPdf()`, `buildPdf(Path)` | Stable | [Getting started](getting-started.md) |
| Stream to a caller-owned stream | `writePdf(OutputStream)` | Stable | [Streaming](recipes/streaming.md) |
| In-memory bytes | `toPdfBytes()` | Stable | [Getting started](getting-started.md) |
| Geometry-identical PowerPoint deck | `buildPptx()`, `buildPptx(Path)`, `writePptx(OutputStream)`, `toPptxBytes()` — needs `graph-compose-render-pptx` on the classpath | Experimental (`@Beta`, first shipped in 2.1.0) | [Backend capability matrix](architecture/backend-capability-matrix.md) |
| Editable Word (semantic) | `export(new DocxSemanticBackend())` | Stable (semantic, not PDF parity) | [Troubleshooting](troubleshooting.md) |
| PDF chrome (metadata / watermark / header / footer / protection) | `metadata(...)`, `watermark(...)`, `header(...)`, `footer(...)`, `protect(...)` | Stable | [Getting started](getting-started.md) |
| Layout snapshot regression | `LayoutSnapshotAssertions.assertMatches(...)` | Stable | [Layout snapshot testing](operations/layout-snapshot-testing.md) |
| Visual (pixel) regression | `PdfVisualRegression` | Stable | [Layout snapshot testing](operations/layout-snapshot-testing.md) |
| Render-only debug overlays | `guideLines(...)`, `debug(...)` | Stable | [Getting started](getting-started.md#debug-guide-lines) |

## Navigation

| Capability | Main API | Stability | Guide |
|---|---|---|---|
| External links | `addLink(...)`, `inlineLink(...)` | Stable | [Getting started](getting-started.md) |
| Internal jumps | `anchor("x")` + `linkTo("x")` | Stable | [Getting started](getting-started.md) |
| PDF outline bookmarks | `bookmark(new DocumentBookmarkOptions(...))` | Stable | [Getting started](getting-started.md) |

---

## New in 2.1.0

These ship from 2.1.0 onward — confirm your dependency version before relying on them:

| Capability | Main API |
|---|---|
| Editable PowerPoint deck (`@Beta`) | `buildPptx(Path)`, `writePptx(OutputStream)`, `toPptxBytes()` — add `graph-compose-render-pptx` |
| Slide page-size presets | `DocumentPageSize.SLIDE_16_9`, `DocumentPageSize.SLIDE_4_3` |
| Select a render backend by format | `BackendProviders.fixedLayout("pptx")` |
| Keep a heading with its content | `SectionBuilder.keepWithNext()`, `LineBuilder.keepWithNext()` |

## New in 1.9.0

These ship from 1.9.0 onward:

| Capability | Main API |
|---|---|
| Printed page references | `addPageReference("anchor")` |
| Generated Table of Contents | `addTableOfContents(toc -> toc.entry(...))` |
| Page preview images | `toImage(pageIndex, dpi)`, `toImages(dpi)` |

---

## See also

- [API stability policy](api-stability.md) — what each tier promises.
- [Decision diagrams](diagrams.md) — visual "which API do I use?".
- [Recipes](recipes.md) — the full cookbook.
- [Which template system should I use?](templates/which-template-system.md) — template-surface decision.
