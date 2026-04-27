# Changelog

## v1.3.0 - 2026-04-27

### Public API

- `DocumentSession` now exposes ergonomic mutators for document-level PDF chrome: `metadata(...)`, `watermark(...)`, `protect(...)`, `header(...)`, `footer(...)`, and `clearHeadersAndFooters()`. Convenience entrypoints (`buildPdf`, `writePdf`, `toPdfBytes`) honour these options without having to build a `PdfFixedLayoutBackend` manually
- new horizontal layout primitive: `addRow(...)` on flows, sections, and modules creates a `RowNode` that arranges atomic children left-to-right with optional `weights(...)` and `gap(...)`. Rows are atomic blocks from the paginator's perspective
- `DocumentBorders` value type plus `borders(...)` on flows, sections, modules, and rows let you describe per-side strokes (top / right / bottom / left). Per-side borders override the uniform `stroke(...)` setting
- `ParagraphBuilder.autoSize(maxSize, minSize)` / `autoSize(DocumentTextAutoSize)` searches for the largest font size that fits the paragraph on a single line within the resolved inner width
- new `addLink(text, uri)` and `addLink(text, DocumentLinkOptions)` shortcuts on `AbstractFlowBuilder` for the common single-link case (was previously only available as a paragraph inline run)
- backend-neutral output options under `com.demcha.compose.document.output` (`DocumentMetadata`, `DocumentWatermark`, `DocumentProtection`, `DocumentHeaderFooter`, aggregated by `DocumentOutputOptions`). PDF and DOCX backends translate them; session-level metadata propagates to DOCX core properties as well as the PDF backend
- `DocxSemanticBackend` is now a functional Apache POI based backend that returns DOCX bytes (was a manifest-only skeleton); supports paragraphs, tables, images, spacers, page breaks, and document-level page geometry. Apache POI is declared optional, so consumers that only render PDFs do not pay the dependency cost

### Performance

- `PageBreaker.paginationPriority` pre-computes `(y, depth)` keys and uses `UUID.compareTo` for tie-breaks (no per-compare string allocation). Old comparator allocated a 36-character UUID string for every priority queue compare
- `Entity.getComponent` and `Entity.require` no longer issue per-call debug logging (even guarded `isDebugEnabled` calls cost a volatile read on Logback)
- table layout helpers (`resolveTableLayout`, `sliceTablePreparedNode`, ~350 lines) extracted into `TableLayoutSupport` for clarity
- end-to-end template rendering is 19–30 % faster than v1.2.0 on the canonical benchmark suite (`invoice-template`: 25.77 → 10.77 ms avg; `cv-template`: 17.89 → 6.50 ms avg; `proposal-template`: 24.77 → 13.44 ms avg)

### Benchmark methodology

- `CurrentSpeedBenchmark` smoke profile bumped from 2 / 5 → 30 / 100 warmup / measurement iterations so the JIT reaches a steady state and percentiles are statistically meaningful
- percentile calculation now uses linear interpolation between order statistics (`rank = (n-1) * p`) so p95 no longer collapses to max at small sample counts
- `System.gc()` plus a 50 ms sleep separates warmup from measurement, dropping run-to-run variance from 10–25 % to 2–5 %
- `peakHeapMb` reports the heap delta over the post-warmup baseline rather than absolute used heap
- a per-stage breakdown table (`compose / layout / render / total`) prints alongside the latency table so consumers can attribute regressions to engine layout vs PDFBox serialization
- smoke gate thresholds tightened from 800–2600 ms (effectively a no-op) to 8–100 ms (~3× the observed avg) — still safe against CI machine variance, now catches ≥50 % regressions
- the `ComparativeBenchmark` console table no longer wraps when library names exceed 20 characters

### Architecture

- `CompositeLayoutSpec` carries an explicit `Axis` (vertical / horizontal) and optional per-child weights; the layout compiler dispatches to a dedicated horizontal-row code path for `RowNode`
- `ShapeFragmentPayload` carries an optional `SideBorders` payload; `PdfShapeFragmentRenderHandler` draws each configured side stroke independently of the uniform rectangle stroke
- `SemanticExportContext` carries `DocumentOutputOptions` so semantic backends (DOCX, future PPTX) can apply metadata / chrome configured at the session level
- the unused engine `Button` renderable and the `ButtonBuilder` test-support factory entry were removed
- guide-line overlays now compute owner bounds across sub-fragments (e.g. table rows) and paint margin / padding once around the entire owning node instead of stacking dashed rectangles inside every row

### Documentation

- `docs/canonical-legacy-parity.md` is updated to reflect the v1.3 capabilities (rows, per-side borders, auto-size text, DOCX export)
- `docs/benchmarks.md` documents the new smoke profile defaults, the GC stabilization point, the linear-interpolation percentile rule, and the stage-breakdown table
- `CONTRIBUTING.md` repository map and package list now describe the canonical functional layout (`document.layout`, `document.backend`, `document.output`) alongside the legacy ECS engine

---

## v1.2.0 - 2026-04-25

### Release identity

- the current canonical API cleanup is being released as **v1.2.0** to match the project's early maturity while still making `GraphCompose.document(...) -> DocumentSession -> DocumentDsl` the preferred authoring path
- Maven coordinates are `io.github.demchaav:graphcompose:1.2.0`; JitPack consumers continue to use `com.github.demchaav:GraphCompose:v1.2.0`
- consumers on `v1.1.x` should follow the [migration guide](./docs/migration-v1-1-to-v1-2.md) before adopting the canonical API path

### Public API

- `DocumentSession` is now an `AutoCloseable` lifecycle owner: `close()` is idempotent, and authoring/rendering methods on a closed session fail fast with `IllegalStateException` instead of returning broken state
- empty document rendering (`writePdf` / `toPdfBytes` / `buildPdf`) now throws a domain-specific `IllegalStateException` instead of producing a zero-byte / zero-page PDF; add at least one root before rendering
- `DocumentPageSize` is the public page-size value; `GraphCompose.document(...).pageSize(PDRectangle)` was removed from the canonical API
- `DocumentSession#margin(Margin)` and `GraphCompose.DocumentBuilder#margin(Margin)` were removed from the canonical API; use `DocumentInsets` or `margin(top, right, bottom, left)` to keep authoring renderer-neutral
- PDF-specific metadata, protection, watermark, and header/footer options moved behind `PdfFixedLayoutBackend.builder()` instead of the canonical `GraphCompose` / `DocumentSession` surface
- `GraphCompose.document(...).guideLines(true)` and `DocumentSession.guideLines(true)` now enable debug guide-line overlays for `buildPdf`, `writePdf`, and `toPdfBytes` convenience output
- `DocumentSession.layoutSnapshot()` now returns public renderer-neutral `com.demcha.compose.document.snapshot.*` DTOs instead of engine debug types
- `BoxConstraints.natural(width)` is now the canonical natural-measurement factory; `unboundedHeight(width)` remains as a compatibility alias
- the public font registry no longer exposes the unadvertised `getPdfFont(...)` bridge; backend code resolves typed fonts through `getFont(..., PdfFont.class)`

### Architecture guards

- `PublicApiNoEngineLeakTest` baselines the inventory of `com.demcha.compose.engine.*` imports allowed in the public API surface — any new leak fails the build
- `SemanticLayerNoPdfBoxDependencyTest` keeps `document.node.*` free of direct PDFBox imports and pins the remaining `backend.fixed.pdf.options.*` references for Phase 3 cleanup
- `PdfBackendIsolationGuardTest` keeps PDFBox out of canonical API, DSL, semantic nodes, layout, snapshots, and non-PDF backend contracts

### Layout

- `PaginationEdgeCaseTest` adds focused regressions for exact-fit content, near-boundary float handling, leading / trailing page breaks, oversized atomic images, too-tall table rows, module splits with PDF chrome, and nested sections that paginate while preserving margin and padding

### Documentation

- new `docs/migration-v1-1-to-v1-2.md` outlines the move from older v1.1 usage patterns to the canonical session-first API
- new `docs/v1.2-roadmap.md` tracks the remaining stabilization work for the v1.2 release polish window
- `docs/release-process.md` now describes the current JitPack-first 1.x release flow and runnable examples verification
- user-facing docs now describe debug guide-line overlays through `GraphCompose.document(...).guideLines(true)` / `DocumentSession.guideLines(true)` and call out JitPack tag-cache handling during release verification

---

## v1.1.0 - 2026-04-13

### Highlights

- shifted the public built-in template narrative to `compose(DocumentSession, ...)`
- added document-level PDF features for richer real-world output
- moved the engine further away from PDF-centric internals through backend-neutral composition and render-handler seams
- strengthened architecture guard rails for template scene builders
- expanded visual testing and benchmark tooling for day-to-day development

### Added

- canonical `DocumentSession` contract as the primary composition seam
- layout snapshot extraction and JSON-based regression coverage for resolved document geometry
- runnable `examples/` module for CV, cover letter, invoice, proposal, and weekly schedule generation
- new built-in business templates and data models for invoice, proposal, and weekly schedule documents
- barcode support with QR, Code 128, and EAN-13 builders
- watermark support
- configurable headers and footers with page numbers and separators
- PDF bookmarks / outline generation
- document metadata support
- PDF protection hooks
- explicit page-break and divider builders
- visual showcase render tests for barcodes, QR codes, pagination, and document chrome
- current-speed benchmark suite
- benchmark JSON/CSV export and diff tooling
- one-command benchmark runner: [scripts/run-benchmarks.ps1](./scripts/run-benchmarks.ps1)

### Changed

- bumped the library release to `v1.1.0`
- updated README installation snippets to the new release version
- documented built-in templates as compose-first by default
- refreshed README visuals to show barcode/QR and compose-first template output
- added release-facing notes for the experimental live preview dev tool in test scope
- refreshed release documentation to point contributors at visual tests and benchmark workflows

### Architecture and CI

- engine-side text measurement and rendering dispatch are now more explicitly decoupled from PDFBox-specific implementation details
- added template boundary guard coverage so `*SceneBuilder` classes stay free of backend-specific PDFBox types
- split architecture/documentation guards into a dedicated CI job that can be required independently in branch protection

### Compatibility notes

- older tagged JitPack releases remain usable as long as consumers pin a specific version such as `v1.0.3`
- deprecated `render(...)` template adapters remain available for compatibility, but new docs and examples now prefer `compose(...)`
