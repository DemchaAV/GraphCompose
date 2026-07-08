# Roadmap

GraphCompose is solo-maintained. This roadmap is a direction, not a contract. Dates are intentionally omitted. Concrete work is tracked in [issues](https://github.com/DemchaAV/GraphCompose/issues) and shipped work is recorded in [CHANGELOG.md](CHANGELOG.md). For v1.6 phase-level detail, see [docs/roadmaps/v1.6-roadmap.md](docs/roadmaps/v1.6-roadmap.md).

## Now — 2.0 line

In flight on `2.0-dev`, preparing the 2.0.0 GA. The 2.0 line is about **packaging and internal hygiene**, not new authoring API — binary-breaking by design, with `japicmp` report-only for the cycle.

- **Modular split** &mdash; the single jar is split into `graph-compose-core` plus `graph-compose-render-pdf` / `graph-compose-render-docx` / `graph-compose-render-pptx` / `graph-compose-templates` / `graph-compose-testing`, with render backends discovered through a `ServiceLoader` SPI. `graph-compose` stays a drop-in wrapper (core + render-pdf) so existing PDF callers upgrade unchanged. See the [2.0 modules migration guide](docs/migration/v2.0.0-modules.md) and [ADR 0016](docs/adr/0016-multi-module-packaging.md).
- **Legacy removal** &mdash; the dead Entity-Component-System execution layer and the deprecated (`forRemoval`) public API are gone; the classic template presets are replaced by the layered `templates.*` stack on `BrandTheme`.
- **Maintenance** &mdash; the **v1.9.x** line (current stable) receives critical fixes only.

Full detail in [CHANGELOG.md](CHANGELOG.md) under `v2.0.0 — Planned`.

## Next — post-2.0 engineering

Committed direction for after the 2.0 GA: internal refactors, scale work, and tooling that do **not** change the public authoring API. Tracked in [docs/roadmaps/post-2.0-engineering.md](docs/roadmaps/post-2.0-engineering.md).

- **Decompose the layout hot files** &mdash; split `LayoutCompiler` and `TextFlowSupport` along their natural seams into individually-tested collaborators, with layout output unchanged.
- **ArchUnit module-boundary guards** &mdash; enforce the canonical / engine / render layering structurally, replacing the path-based greps that can pass vacuously after a move.
- **Cross-module coverage aggregation** &mdash; a dedicated non-published module that compile-depends on the tested modules so JaCoCo sees the `qa` suites; report-only first, thresholds after a baseline read.
- **Per-module binary-compatibility baselines** &mdash; once the 2.0 GA artifacts are published, switch `japicmp` to per-module baselines in break-on-incompatible mode.

## Later (directional)

Not committed. Reflects current thinking; priorities may shift based on user feedback and adoption signals.

- **DOCX visibility for unsupported nodes.** Make currently-silent skips (`shape`, `line`, `ellipse`, `barcode`) loud &mdash; minimum a warn log, ideally a strict-mode flag that fails instead of dropping content silently.
- **Block-level alignment for fixed-size flow children.** Paths, images, layer stacks, shape containers and barcodes currently left-align in a flow; centring one means wrapping it in a full-width `ShapeContainer` just to use its CENTER anchor. Add a per-node horizontal align (left / centre / right &mdash; the `margin: auto` / `align(center)` analogue) so a fixed box can place itself in the flow directly. Surfaced by the v1.8 SVG icon-gallery and feature-catalog work.
- **Backend-neutral layout measurement.** Decouple measurement from PDFBox-specific resources so non-PDF backends do not pull PDFBox into the dependency graph.
- **DOCX maturity.** Either expand DOCX coverage toward PDF parity, or move DOCX behind an explicitly experimental flag.
- **Property-based testing.** Random table spans, pagination edge cases, deeply nested layouts.
- **Real PPTX export.** Current state is a manifest skeleton. Will only be built out if there is concrete user demand.
- **Public Javadoc site.** Generated and hosted, kept in sync with releases.

## Not on the roadmap

- Hosted PDF rendering service.
- WYSIWYG editor.
- HTML / CSS input.
- Browser-side rendering.

See [README &mdash; What GraphCompose is not](README.md#what-graphcompose-is-not).

## Feedback

Have a use case that should be on this list, or strong feelings about priority? Open a [discussion issue](https://github.com/DemchaAV/GraphCompose/issues/new?labels=question&title=Roadmap%3A+) or comment on the relevant tracked issue.
