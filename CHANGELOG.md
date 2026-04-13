# Changelog

## v1.1.0 - 2026-04-13

### Highlights

- shifted the public built-in template narrative to `compose(DocumentComposer, ...)`
- added document-level PDF features for richer real-world output
- moved the engine further away from PDF-centric internals through backend-neutral composition and render-handler seams
- strengthened architecture guard rails for template scene builders
- expanded visual testing and benchmark tooling for day-to-day development

### Added

- backend-neutral `DocumentComposer` contract as the primary composition seam
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
- added template boundary guard coverage so `*SceneBuilder` classes stay free of `PDDocument`, `PDPage`, `PDRectangle`, and `PdfComposer`
- split architecture/documentation guards into a dedicated CI job that can be required independently in branch protection

### Compatibility notes

- older tagged JitPack releases remain usable as long as consumers pin a specific version such as `v1.0.3`
- deprecated `render(...)` template adapters remain available for compatibility, but new docs and examples now prefer `compose(...)`
