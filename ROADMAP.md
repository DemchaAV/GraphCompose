# Roadmap

GraphCompose is solo-maintained. This roadmap is a direction, not a contract. Dates are intentionally omitted. Concrete work is tracked in [issues](https://github.com/DemchaAV/GraphCompose/issues) and shipped work is recorded in [CHANGELOG.md](CHANGELOG.md).

## Now — after 2.1

Consolidation ahead of the next patch: the documentation and contributor surfaces are being brought onto the 2.x vocabulary, the CI guards onto what they actually run, and the committed example assets onto a regeneration path. Open engineering threads are tracked in [issues](https://github.com/DemchaAV/GraphCompose/issues) — vector clipping for the PPTX backend, backend-neutral font measurement, and font-face selection are the live ones.

## Current stable — 2.2

**2.2.0** brings **right-to-left text**: a paragraph says which way it runs, and Hebrew and
Arabic lay out, shape, join and mirror correctly through all three backends. The paragraph
is the unit that carries it, including inside a table cell composed as one; a cell written
as a plain string still has no direction. The PDF is painted, so the engine resolves the line itself with the
Unicode Bidirectional Algorithm, shapes Arabic into its joined forms, and writes the file
so a reader copies out the letters an author typed rather than the shapes they were drawn
as. Word and PowerPoint have bidirectional engines of their own, so each is handed logical
text and told what it needs — `w:bidi` and the complex-script twins for Word, a declared
direction per frame for PowerPoint — with the differences recorded in the
[backend capability matrix](docs/architecture/backend-capability-matrix.md).

The scripts come with it: Amiri, David Libre, the Noto faces for Georgian and Armenian and
Gothic A1 for Korean join the bundled catalogue, and a PowerPoint deck now carries the
bundled family it drew with instead of naming a font the reader may not have.

Full detail in [CHANGELOG.md](CHANGELOG.md) under `v2.2.0`.

## Previously — 2.1

**2.1.1** is the current release. Its headline is the one 2.1.0 opened the line with: the **fixed-layout PPTX render backend**, where the same `DocumentSession` that prints a PDF also emits an editable PowerPoint deck — one page per slide, identical geometry by construction, native shapes. It ships as `@Beta` (Experimental) while its API shape settles; the geometry identity with the PDF backend is a design invariant, not subject to change. See the [API stability policy](docs/api-stability.md) and the [backend capability matrix](docs/architecture/backend-capability-matrix.md).

2.1 also hardened pagination: `keepWithNext()` for headings, orphaned-heading fixes ahead of paragraphs, tables and lists, and the layered CV presets wired to the keep-with-next policy. 2.1.1 followed with release-tooling and Javadoc-gate fixes only — no authoring API moved.

Full detail in [CHANGELOG.md](CHANGELOG.md) under `v2.1.1` and `v2.1.0`.

## Previously — 2.0

The **2.0.0** GA opened the 2.x line. 2.0 was about **packaging and internal hygiene**, not new authoring API — binary-breaking by design.

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
- **A strict mode for DOCX.** Dropping a node the format cannot carry (`shape`, `line`, `ellipse`, `barcode`) now warns, so the loss is at least visible in the log. What is missing is the option to refuse: a flag that fails the export instead of silently producing a document with content gone.
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
