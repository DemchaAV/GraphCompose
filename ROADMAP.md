# Roadmap

GraphCompose is solo-maintained. This roadmap is a direction, not a contract. Dates are intentionally omitted. Concrete work is tracked in [issues](https://github.com/DemchaAV/GraphCompose/issues) and shipped work is recorded in [CHANGELOG.md](CHANGELOG.md).

## Now — after 2.5

The live threads are tracked in [issues](https://github.com/DemchaAV/GraphCompose/issues): graduating the Java API of the Word and PowerPoint outputs from `@Beta` to Stable, moving the office backends to Apache POI 6, growing the built-in layered template line-up, and keeping the knowledge pack current as the API moves. Vector clipping for the PPTX backend remains open.

## Upcoming — 2.5

**2.5.0** is the current release. From this line **PDF, PowerPoint and Word are three full
outputs** of one document. It leads with a **DOCX export that writes the page the engine
laid out**: the line heights, column widths and space round each block the page has, real
Word lists, drawings anchored where the text that carries them moves them, rules, icons,
page zones, headers, footers and backgrounds, bookmarks, links and an outline, and the faces
the document is set in. Word still paginates the document and reflows it after an edit. A
chart is a Word chart with its numbers in an embedded workbook, and `DocxExportReport` names
every node the export dropped or wrote as the nearest thing Word has. The export is held to
a corpus of 104 documents measured line by line in Word and LibreOffice. The Java API of the
Word and PowerPoint outputs stays Experimental (`@Beta`); the formats are full outputs.

The release train reaches Maven Central as **one deployment** of its eight components, and
a line copied out of a PDF keeps its emoji.

Full detail in [CHANGELOG.md](CHANGELOG.md) under `v2.5.0`.

## Current stable — 2.4

**2.4.1** is the current release. It leads with a **much larger template line-up**. Two
families join invoice, proposal, CV and cover letter: **receipt**, for payment
confirmations (`ModernReceipt`), and **rota**, a staff shift schedule (`CobaltRota`) on a
new `templates.data.rota` model that replaces the `data.schedule` records nothing rendered.
The invoice, proposal and CV families gain presets, and the structured invoice and proposal
models grow to carry what those designs print. Every CV preset is now either
**ATS-friendly** or **design-first**, a classification made by reading its showcase sample
with three resume parsers; the showcase marks the ATS-friendly ones.

The engine work under it is typographic. `DocumentTextStyle` carries **real letter
spacing** — PDF `Tc`, DrawingML `spc`, Word `w:spacing` — so spaced caps copy and search as
the word they are instead of letters padded with spaces, and the built-in CV and
cover-letter presets use it. A timeline's rail is one line resolved from where its markers
landed, and the timeline can put a column before its markers, size the marker column in
points and set the gap before a marker on its own. A list can hang its wrapped lines under
its text, style an item in pieces, and draw its markers in a colour of their own.
`graph-compose-templates` joins `graph-compose-core` under the binary-compatibility gate.

Full detail in [CHANGELOG.md](CHANGELOG.md) under `v2.4.0`.

## Previously — 2.3

**2.3.0** is the current release. It leads with **page chrome built from nodes**: a
`DocumentPageZone` takes a content function and lays a real node subtree into the band —
a badge, a link annotation, an image, right-to-left text, any font the document has —
through the same layout and render path as the body, on every fixed-layout backend. A
`PageContext` hands each page its numbers as plain Java values; `pageNumber()` returns a
node, so the same zone exports to Word as a live `PAGE` field — the first DOCX
header/footer support — and a page reference inside a zone resolves against the body's
anchors. The text header/footer picks up the two things it was missing: a font family
(`fontName`) and real height reservation (`reserveSpace`).

The release also ships the **knowledge pack**: a tracked, generated description of the
public API under `knowledge/`, gated in CI against the compiled classes, with
documentation claims resolved to proofs and a queryable offline bundle attached to the
GitHub Release.

Full detail in [CHANGELOG.md](CHANGELOG.md) under `v2.3.0`.

## Previously — 2.2

**2.2.2** brings **right-to-left text**: a paragraph says which way it runs, and Hebrew and
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

- **PPTX API to Stable.** Graduate the fixed-layout PPTX backend's Java API from `@Beta` to Stable and close the remaining fidelity gaps &mdash; true vector clipping instead of the raster fallback ([#413](https://github.com/DemchaAV/GraphCompose/issues/413)), exact numeric dash arrays, and distinct per-corner radii.
- **A strict mode for DOCX.** Since 2.5, shapes, lines and ellipses are written where the layout places them &mdash; as drawings, or as Word's own rule for a plain horizontal line in the flow &mdash; and dropped only where they cannot be drawn: a fill that is only a gradient, or a document the engine could not lay out, among them. Every node the export drops or writes as the nearest thing Word has is named, with its path, in a `DocxExportReport` handed to the sink of the backend passed to `export(...)`. A caller exporting to bytes can read the report before keeping them, but an export given an output file has written it by the time the report arrives. A built-in option to refuse &mdash; a flag that fails the export instead of writing a document with content gone &mdash; is a direction, not a decision: today an export writes what it can and reports the rest.
- **Block-level alignment for fixed-size flow children.** Paths, images, layer stacks, shape containers and barcodes currently left-align in a flow; centring one means wrapping it in a full-width `ShapeContainer` just to use its CENTER anchor. Add a per-node horizontal align (left / centre / right &mdash; the `margin: auto` / `align(center)` analogue) so a fixed box can place itself in the flow directly. Surfaced by the v1.8 SVG icon-gallery and feature-catalog work.
- **Backend-neutral layout measurement.** Decouple measurement from PDFBox-specific resources so non-PDF backends do not pull PDFBox into the dependency graph.
- **DOCX after 2.5.** 2.5 expanded DOCX coverage toward the PDF's page rather than moving DOCX behind an experimental flag: the export writes from the compiled layout and is held to a corpus in Word and LibreOffice. Still open: graduating to stable what it added as `@Beta`, among them the export report, the backend builder, `buildDocx` and its siblings and the section export; writing a chart that shares a table cell with other drawings, or sits in a table composed in a cell, as a Word chart rather than its data table; the same deterministic bytes on every platform rather than on one; and LibreOffice setting the exported document as close to the engine's page as Word does: on the 2.5 corpus Word on Windows passes 95 of 104 documents, LibreOffice 24.2 on CI's Linux 60.
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
