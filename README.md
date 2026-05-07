# GraphCompose

<p align="center">
  <img src="./assets/GraphComposeLogo.png" alt="GraphCompose logo" width="300"/>
</p>

<p align="center">
  <b>Java-first declarative document layout engine for cinematic PDFs.</b><br/>
  Describe semantic document structure; GraphCompose handles layout, pagination, snapshots, and PDFBox rendering &mdash; with a designer-grade visual layer on top.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" alt="Java 21"/>
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="MIT License"/>
  <img src="https://img.shields.io/badge/PDFBox-3.0-red?style=for-the-badge" alt="PDFBox 3.0"/>
  <a href="https://jitpack.io/#DemchaAV/GraphCompose">
    <img src="https://img.shields.io/jitpack/v/github/DemchaAV/GraphCompose?style=for-the-badge&label=JitPack" alt="JitPack"/>
  </a>
</p>

<p align="center">
  <a href="https://demchaav.github.io/GraphCompose/"><b>View Live Showcase</b></a>
</p>

## Why GraphCompose?

Most Java PDF libraries hand you low-level drawing commands. GraphCompose gives Java applications a **semantic authoring model** &mdash; you describe modules, paragraphs, tables, rows, layers, and themes; the engine measures, paginates, and renders.

- **Author intent, not coordinates.** Fluent builder for sections, modules, paragraphs, lists, tables, images, dividers, page-breaks, and layer stacks.
- **Deterministic layout.** Two passes &mdash; layout resolves geometry, render consumes resolved fragments. Snapshots are stable across runs and machines, so you can regression-test layout before any PDF byte is written.
- **Atomic pagination, no manual paging.** Tables split row-by-row, rows are atomic, layer stacks are atomic.
- **Designer-grade output.** Page backgrounds, section bands, soft panels, accent strips, column spans, layered hero blocks, fluent rich text, and a tokenised `BusinessTheme` are all first-class &mdash; not workarounds.
- **PDFBox rendering, isolated.** PDF backend lives behind a single backend interface. The DOCX backend (Apache POI) is ready for callers who need an editable file.
- **Tested at every layer.** 819 green tests on `develop` (525 → 672 across v1.5; +147 across the v1.6 line for Templates v2, nested lists, composed table cells, free-canvas placement, and architecture hardening), including cinematic-feature tests, shape-as-container clip-path invariants, transform CTM checks, table row-span / zebra / repeated-header tests, public-API leak guards, layout-snapshot baselines for every CV / cover-letter preset, and a `PdfVisualRegression` harness.

**v1.6 &mdash; the "expressive" release** is in flight on `develop`. v1.6 closes the canonical-vs-legacy parity gaps for advanced authoring without architectural rollback: nested-list ergonomics (`ListBuilder.addItem(label, Consumer)`), composed table cells (`DocumentTableCell.node(DocumentNode)` &mdash; paragraphs, lists, sub-tables inside cells), pixel-precise free-canvas placement (`CanvasLayerNode`), the **Templates v2** preset library (14 CV + 14 paired cover-letter presets, theme-driven via `BusinessTheme`), and architecture hardening (`@Internal` API stability marker, public `PdfFragmentRenderHandler` SPI, `DocumentRenderingException`, thread-safety contract). Source-compatible with v1.5 on the engine surface; the Templates v1 → v2 carve-out is migration-noted in [`docs/migration-v1-5-to-v1-6.md`](docs/migration-v1-5-to-v1-6.md).

The latest **published tag** is **v1.5.1** &mdash; the "intuitive" release (shape-as-container with clip path, transforms + per-layer z-index, advanced tables, `InvoiceTemplateV2` / `ProposalTemplateV2`). The install snippets below stay pinned to `v1.5.1` until v1.6.0 ships on JitPack; track v1.6 progress on `develop` and in [`CHANGELOG.md`](./CHANGELOG.md).

## Who is GraphCompose for?

GraphCompose is built for **server-side Java services that need to generate structured business PDFs** &mdash; the kind of documents your application has to produce on demand from real data, not the kind a human types into Word.

- **Invoices and quotes** generated per request from order data (`InvoiceTemplateV2 + BusinessTheme.modern()` is the canonical entry point).
- **Proposals, statements of work, and reports** with consistent branding across teams (`ProposalTemplateV2`, custom `BusinessTheme`).
- **CVs and cover letters** for ATS exports, hiring tools, and recruiter dashboards (modernised CV presets and paired cover-letter presets, all themed via `BusinessTheme`).
- **Schedules, dispatch sheets, and operational reports** with deterministic pagination on long data (advanced tables: row span, zebra, totals, repeating header on page break).
- **Internal tooling and admin PDFs** that ship through Spring Boot, Quarkus, Micronaut, Ktor, or any plain Java HTTP service &mdash; `writePdf(OutputStream)` streams straight into a Servlet response without buffering in memory.

Reaching for **iText** for low-level page primitives or **JasperReports** for XML-template-driven layout? GraphCompose sits between them: a Java DSL describes the document semantically, the engine resolves layout and pagination deterministically, and the PDF backend renders the result with PDFBox.

## Visual preview

<p align="center">
  <img src="./assets/readme/repository_showcase_render.png" alt="GraphCompose repository showcase render" width="850"/>
</p>

The proposal screenshot above is built by [`CinematicProposalFileExample`](./examples/src/main/java/com/demcha/examples/templates/proposal/CinematicProposalFileExample.java) in the runnable `examples/` module &mdash; a single Java file, no XML, no template engine.

> 📚 **[Browse the full examples gallery →](./examples/README.md)** — every one of the 26 examples with description, key DSL snippet, committed PDF preview, and source link.

## Installation

Distributed through JitPack.

```xml
<repositories>
    <repository><id>jitpack.io</id><url>https://jitpack.io</url></repository>
</repositories>

<dependency>
    <groupId>com.github.DemchaAV</groupId>
    <artifactId>GraphCompose</artifactId>
    <version>v1.5.1</version>
</dependency>
```

```kotlin
repositories { maven("https://jitpack.io") }
dependencies { implementation("com.github.demchaav:GraphCompose:v1.5.1") }
```

The DOCX backend depends on `org.apache.poi:poi-ooxml`, declared as `optional` &mdash; add it explicitly when you call `session.export(new DocxSemanticBackend())`.

## Quick start

The fastest path to a designed PDF is the cinematic stack: pick a `BusinessTheme`, drop a `softPanel + accentLeft` hero block on the page, and let the theme drive every colour, font, and table style. v1.6 layers nested lists, composed table cells, and `CanvasLayerNode` (free-canvas placement) on top of the same surface.

```java
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.theme.BusinessTheme;

import java.nio.file.Path;

public class QuickStart {
    public static void main(String[] args) throws Exception {
        BusinessTheme theme = BusinessTheme.modern();   // cream paper + teal/gold

        try (DocumentSession document = GraphCompose.document(Path.of("output.pdf"))
                .pageSize(DocumentPageSize.A4)
                .pageBackground(theme.pageBackground())
                .margin(28, 28, 28, 28)
                .create()) {

            document.pageFlow(page -> page
                    .addSection("Hero", section -> section
                            .softPanel(theme.palette().surfaceMuted(), 10, 14)
                            .accentLeft(theme.palette().accent(), 4)
                            .addParagraph(p -> p.text("GraphCompose").textStyle(theme.text().h1()))
                            .addParagraph(p -> p.text("A theme-driven hero, one page, no manual coordinates.")
                                    .textStyle(theme.text().body()))));

            document.buildPdf();
        }
    }
}
```

For an HTTP response, S3 upload, or in-memory generation, use the `writePdf(OutputStream)` overload &mdash; it streams directly and does **not** close the caller's stream. See [`HttpStreamingExample`](./examples/src/main/java/com/demcha/examples/features/streaming/HttpStreamingExample.java) for the Spring Boot `@RestController` pattern.

For built-in templates (`InvoiceTemplateV2`, `ProposalTemplateV2`) and the canonical authoring patterns (builder hierarchy, theme tokens, golden patterns, anti-patterns, 40-line new-template skeleton), read the **[Template authoring cheatsheet](./docs/template-authoring.md)** once before writing your own.

## What's new in v1.6

Five highlights &mdash; full notes in [`CHANGELOG.md`](./CHANGELOG.md).

- **Nested list ergonomics (Phase A).** `ListBuilder.addItem(String label, Consumer<ListBuilder> body)` appends a nested item with a builder-callback child scope. New `ListItem` record carries `(label, marker, children)`; per-depth marker cascade (`•` → `◦` → `▪` → `·`) with item-level + builder-level overrides. Mixed flat / nested authoring preserves source order &mdash; flat-only callers still get the v1.5 flat `ListNode`. ADR [0012](./docs/adr/0012-nested-list-evolution.md).
- **Composed table cells (Phase B).** `DocumentTableCell.node(DocumentNode)` accepts any composable canonical node as cell content &mdash; paragraphs, lists, layer-stacks, sub-tables. `FragmentContext.emitChildFragments(...)` dispatches recursively through the registered `NodeDefinition`, so any node type works inside a cell automatically. Two-pass measurement preserves the existing row-by-row pagination contract. ADR [0013](./docs/adr/0013-composed-table-cell.md).
- **Free-canvas placement (Phase C).** `CanvasLayerNode` &mdash; pixel-precise `(x, y)` placement of children inside a fixed-size bounding box, with `ClipPolicy.CLIP_BOUNDS` clipping and atomic pagination. Reuses the `LayerStackNode` placement plumbing; the surrounding flow reserves a deterministic rectangle. ADR [0014](./docs/adr/0014-controlled-absolute-placement.md).
- **Templates v2 preset library.** Canonical CV / cover-letter / invoice / proposal surface rebuilt around four layers (theme tokens → layout slots → components + blocks → spec data). 14 CV presets and 14 paired cover-letter presets, each one final class with a one-liner `create(BusinessTheme)` factory. Inline markdown, hyperlinks, slot-based multi-column layouts, and `CvHeader.jobTitle` subtitle built in. ADR [0011](./docs/adr/0011-templates-v2-architecture.md).
- **Architecture hardening.** `@Internal` API stability marker on `document.layout.*` and `BuiltInNodeDefinitions` payload records (ADR [0003](./docs/adr/0003-api-stability-and-internal-marker.md)). Public `PdfFragmentRenderHandler` SPI as the new render-handler registration path (ADR [0004](./docs/adr/0004-pdf-handler-spi-extension.md)). `DocumentRenderingException` wraps the convenience render path so `buildPdf` / `writePdf` / `toPdfBytes` no longer declare `throws Exception`. Thread-safety contract documented on `document.api/package-info.java`.

### What was new in v1.5

Shape-as-container with clip path ([recipe](./docs/recipes/shape-as-container.md)), transforms + per-layer z-index ([recipe](./docs/recipes/transforms.md)), advanced tables (row span, zebra, totals, repeating header &mdash; [recipe](./docs/recipes/tables.md)), `InvoiceTemplateV2` / `ProposalTemplateV2`, and `CvTheme.fromBusinessTheme(...)` (ADR 0002). v1.5 is fully source-compatible with v1.4. See [`docs/migration-v1-4-to-v1-5.md`](./docs/migration-v1-4-to-v1-5.md) and the v1.5 sections of [`CHANGELOG.md`](./CHANGELOG.md).

## Architecture in three lines

- **Public authoring** lives in `com.demcha.compose`, `document.api`, `document.dsl`, `document.node`, `document.style`, `document.table`, `document.theme`, `font`. Engine internals (`com.demcha.compose.engine.*`) are guarded by `PublicApiNoEngineLeakTest` &mdash; never reach into them from application code.
- **Two passes**: `DocumentSession.layoutGraph()` resolves geometry; rendering consumes the resolved fragments. This is what makes layout snapshots, pagination, and future backends practical.
- **Extension seams**: implement `DocumentNode` + register a `NodeDefinition`; emit a `FragmentPayload` + register a `PdfFragmentRenderHandler`; add a `FixedLayoutBackend<R>` or `SemanticBackend` to target a new format. See [`docs/architecture.md`](./docs/architecture.md) for the full map.

## Documentation

- [**Template authoring cheatsheet**](./docs/template-authoring.md) — read this once before writing your own template
- [Examples gallery](./examples/README.md) — 26 runnable examples with PDF previews
- [Architecture](./docs/architecture.md) · [Lifecycle](./docs/lifecycle.md) · [Production rendering](./docs/production-rendering.md)
- Recipes: [shape-as-container](./docs/recipes/shape-as-container.md) · [transforms](./docs/recipes/transforms.md) · [tables](./docs/recipes/tables.md) · [shapes](./docs/recipes/shapes.md) · [themes](./docs/recipes/themes.md) · [streaming](./docs/recipes/streaming.md) · [extending](./docs/recipes/extending.md)
- [Layout snapshot testing](./docs/layout-snapshot-testing.md) · [Performance numbers](./docs/performance.md) · [Benchmark methodology](./docs/benchmarks.md)
- [Migration v1.4 → v1.5](./docs/migration-v1-4-to-v1-5.md) · [Migration v1.5 → v1.6](./docs/migration-v1-5-to-v1-6.md) · [Canonical / legacy parity](./docs/canonical-legacy-parity.md)
- [Contributing](./CONTRIBUTING.md) · [Release process](./docs/release-process.md) · [v1.6 roadmap](./docs/v1.6-roadmap.md)

## Templates v2 &mdash; the v1.6 preset library

v1.6 ships **Templates v2** &mdash; the canonical CV / cover letter / invoice / proposal surface rebuilt from the ground up around four layers (theme tokens → layout slots → components + blocks → spec data). Every preset is one final class with a one-liner `create(BusinessTheme)` factory:

```java
import com.demcha.compose.document.templates.cv.presets.ModernProfessional;
import com.demcha.compose.document.templates.cv.spec.CvSpec;
import com.demcha.compose.document.theme.BusinessTheme;

DocumentTemplate<CvSpec> template = ModernProfessional.create(BusinessTheme.modern());
template.compose(session, mySpec);
```

14 CV presets (`ModernProfessional`, `NordicClean`, `ClassicSerif`, `CompactMono`, `Executive`, `EngineeringResume`, `TimelineMinimal`, `BoxedSections`, `CenteredHeadline`, `BlueBanner`, `EditorialBlue`, `Panel`, `SidebarPortrait`, `MonogramSidebar`), 14 paired cover-letter presets, plus minimal v2 `ModernInvoice` / `ModernProposal` builders. Inline markdown rich text (`**bold**`, `*italic*`), active hyperlinks, slot-based multi-column layouts, and a first-class `CvHeader.jobTitle` subtitle field ship out of the box.

**Breaking** &mdash; legacy CV / cover-letter classes are deleted, not deprecated. v1.x SemVer "API stability" covers the engine, not the templates layer; the carve-out is documented in [ADR 0011](./docs/adr/0011-templates-v2-architecture.md), and the full migration table (every V1 class → its v2 preset, plus before/after code) is in [`docs/migration-v1-5-to-v1-6.md`](./docs/migration-v1-5-to-v1-6.md). The 14 presets render with verified V1 visual parity (pixel-diff baselines under `src/test/resources/visual-baselines/cv-v2/`); tech debt acknowledged for v1.7 &mdash; refactor the hand-coded presets back into thin builder recipes once the v2 component library grows the missing primitives.

## Roadmap

**Next: v1.7.** Carrying over from the v1.6 stretch goals &mdash; real PPTX semantic export (`PptxSemanticBackend` built out from the manifest skeleton), Maven Central distribution (Sonatype OSSRH + GPG signing on tag push, primary coordinates `io.github.demchaav:graphcompose:1.7.0`, JitPack stays as a fallback), JMH-based benchmark infrastructure (`benchmarks/` Maven module mirroring `examples/`, self-executing JMH jar), and the Templates v2 component-library extension that lets the 13 hand-coded presets refactor back into thin builder recipes. Full plan in [`docs/v1.6-roadmap.md`](./docs/v1.6-roadmap.md). Feature requests and bug reports welcome via GitHub Issues.

## License

GraphCompose is released under the MIT License &mdash; see [`LICENSE`](./LICENSE).
