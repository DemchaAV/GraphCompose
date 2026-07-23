# Roadmap

GraphCompose is solo-maintained. This roadmap is a direction, not a contract. Dates are intentionally omitted. Concrete work is tracked in [issues](https://github.com/DemchaAV/GraphCompose/issues) and shipped work is recorded in [CHANGELOG.md](CHANGELOG.md). For v1.6 phase-level detail, see [docs/roadmaps/v1.6-roadmap.md](docs/roadmaps/v1.6-roadmap.md).

## Now — 2.1 line

In development on `develop`, building toward the **2.1.0** minor. The headline is the **fixed-layout PPTX render backend**: the same `DocumentSession` that prints a PDF now also emits an editable PowerPoint deck — one page per slide, identical geometry by construction, native shapes. It ships as `@Beta` (Experimental) while its API shape settles; the geometry identity with the PDF backend is a design invariant, not subject to change. See the [API stability policy](docs/api-stability.md) and the [backend capability matrix](docs/architecture/backend-capability-matrix.md).

Alongside it, 2.1 hardens pagination: `keepWithNext()` for headings, orphaned-heading fixes ahead of paragraphs, tables and lists, and the layered CV presets wired to the keep-with-next policy.

Full detail lands in [CHANGELOG.md](CHANGELOG.md) under `v2.1.0`.

## Current stable — 2.0

The **2.0.0** GA is the current stable line. 2.0 was about **packaging and internal hygiene**, not new authoring API — binary-breaking by design.

- **Modular split** &mdash; the single jar is split into `graph-compose-core` plus `graph-compose-render-pdf` / `graph-compose-render-docx` / `graph-compose-render-pptx` / `graph-compose-templates` / `graph-compose-testing`, with render backends discovered through a `ServiceLoader` SPI. `graph-compose` stays a drop-in wrapper (core + render-pdf) so existing PDF callers upgrade unchanged. See the [2.0 modules migration guide](docs/migration/v2.0.0-modules.md) and [ADR 0016](docs/adr/0016-multi-module-packaging.md).
- **Legacy removal** &mdash; the dead Entity-Component-System execution layer and the deprecated (`forRemoval`) public API are gone; the classic template presets are replaced by the layered `templates.*` stack on `BrandTheme`.
- **Release &amp; publishing pipeline** &mdash; multi-module Maven Central publishing, the `core/`-layout reactor (`./mvnw clean verify` at the root builds everything), and the cut / tag / GA runbook.
- **Compatibility tests** &mdash; the guard, snapshot, and visual-regression suites that prove the split left rendered output unchanged.

The **v1.9.x** line receives critical fixes only.

## Next — post-2.0 engineering

Committed internal direction for the post-2.0 line: refactors, scale work, and tooling that do **not** change the public authoring API. Tracked in [docs/roadmaps/post-2.0-engineering.md](docs/roadmaps/post-2.0-engineering.md).

- **Decompose the layout hot files** &mdash; split `LayoutCompiler` and `TextFlowSupport` along their natural seams into individually-tested collaborators, with layout output unchanged.
- **Per-module binary-compatibility baselines** &mdash; now that the 2.0 GA artifacts are published, switch `japicmp` from the single-artifact baseline to per-module baselines in break-on-incompatible mode.

## Later (directional)

Not committed. Reflects current thinking; priorities may shift based on user feedback and adoption signals.

- **PPTX beyond beta.** Graduate the fixed-layout PPTX backend from `@Beta` to stable and close the remaining fidelity gaps &mdash; true vector clipping instead of the raster fallback ([#413](https://github.com/DemchaAV/GraphCompose/issues/413)), exact numeric dash arrays, and distinct per-corner radii.
- **DOCX visibility for unsupported nodes.** Make currently-silent skips (`shape`, `line`, `ellipse`, `barcode`) loud &mdash; minimum a warn log, ideally a strict-mode flag that fails instead of dropping content silently.
- **Block-level alignment for fixed-size flow children.** Paths, images, layer stacks, shape containers and barcodes currently left-align in a flow; centring one means wrapping it in a full-width `ShapeContainer` just to use its CENTER anchor. Add a per-node horizontal align (left / centre / right &mdash; the `margin: auto` / `align(center)` analogue) so a fixed box can place itself in the flow directly. Surfaced by the v1.8 SVG icon-gallery and feature-catalog work.
- **Backend-neutral layout measurement.** Decouple measurement from PDFBox-specific resources so non-PDF backends do not pull PDFBox into the dependency graph.
- **DOCX maturity.** Either expand DOCX coverage toward PDF parity, or move DOCX behind an explicitly experimental flag.
- **Property-based testing.** Expand the `@Property` layout-invariant seed to random table spans, pagination edge cases, and deeply nested layouts.
- **Public Javadoc site.** Generated and hosted, kept in sync with releases.

## Not on the roadmap

- Hosted PDF rendering service.
- WYSIWYG editor.
- HTML / CSS input.
- Browser-side rendering.

See [README &mdash; What GraphCompose is not](README.md#what-graphcompose-is-not).

## Feedback

Have a use case that should be on this list, or strong feelings about priority? Open a [discussion issue](https://github.com/DemchaAV/GraphCompose/issues/new?labels=question&title=Roadmap%3A+) or comment on the relevant tracked issue.
