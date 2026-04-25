# Changelog

## v2.0.0 - Unreleased

### Release identity

- the `graphcompose-v2-engine` rewrite is being shipped as **v2.0.0** because the public composition model has changed: `GraphCompose.document(...) → DocumentSession → DocumentDsl` is now the canonical authoring path
- Maven coordinates moved to `io.github.demchaav:GraphCompose:2.0.0` (the `v` prefix is now reserved for git tags only); JitPack consumers continue to use `com.github.DemchaAV:GraphCompose:v2.0.0`
- consumers of `v1.x` should follow the [migration guide](./docs/migration-v1-to-v2.md) before upgrading

### Public API

- `DocumentSession` is now an `AutoCloseable` lifecycle owner: `close()` is idempotent, and authoring/rendering methods on a closed session fail fast with `IllegalStateException` instead of returning broken state
- empty document rendering (`writePdf` / `toPdfBytes` / `buildPdf`) now throws a domain-specific `IllegalStateException` instead of producing a zero-byte / zero-page PDF; add at least one root before rendering
- `DocumentSession#margin(Margin)` and `GraphCompose.DocumentBuilder#margin(Margin)` are deprecated for removal; use the `DocumentInsets` overload (or `margin(top, right, bottom, left)`) which keeps the public API renderer-neutral
- `BoxConstraints.unboundedHeight(width)` factory replaces the previous magic `1_000_000.0` height constant in internal layout call sites

### Architecture guards

- `PublicApiNoEngineLeakTest` baselines the inventory of `com.demcha.compose.engine.*` imports allowed in the public API surface — any new leak fails the build
- `SemanticLayerNoPdfBoxDependencyTest` keeps `document.node.*` free of direct PDFBox imports and pins the remaining `backend.fixed.pdf.options.*` references for Phase 3 cleanup

### Layout

- `PaginationEdgeCaseTest` adds focused regressions for exact-fit content, near-boundary float handling, leading / trailing page breaks, and nested sections that paginate while preserving margin and padding

### Documentation

- new `docs/migration-v1-to-v2.md` outlines the move from the legacy template-first API to the canonical session-first API
- new `docs/v2-roadmap.md` tracks the remaining stabilisation work scheduled for the v2 release window (DSL split, semantic-layer purity, layout edge cases, release packaging)
- new `docs/release-process.md` defines the v2 release checklist (Maven coordinates, JitPack tag, GitHub release notes, post-release smoke checks)

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
