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

## Why GraphCompose?

Most Java PDF libraries hand you low-level drawing commands. GraphCompose gives Java applications a **semantic authoring model** &mdash; you describe modules, paragraphs, tables, rows, layers, and themes; the engine measures, paginates, and renders.

- **Author intent, not coordinates.** Fluent builder for sections, modules, paragraphs, lists, tables, images, dividers, page-breaks, and layer stacks.
- **Deterministic layout.** Two passes &mdash; layout resolves geometry, render consumes resolved fragments. Snapshots are stable across runs and machines, so you can regression-test layout before any PDF byte is written.
- **Atomic pagination, no manual paging.** Tables split row-by-row, rows are atomic, layer stacks are atomic.
- **Designer-grade output.** Page backgrounds, section bands, soft panels, accent strips, column spans, layered hero blocks, fluent rich text, and a tokenised `BusinessTheme` are all first-class &mdash; not workarounds.
- **PDFBox rendering, isolated.** PDF backend lives behind a single backend interface. The DOCX backend (Apache POI) is ready for callers who need an editable file.
- **Tested at every layer.** 675 green tests on `develop` (525 → 675 across v1.5), including cinematic-feature tests, shape-as-container clip-path invariants, transform CTM checks, table row-span / zebra / repeated-header tests, public-API leak guards, and a `PdfVisualRegression` harness.

The current release is **v1.5.0** &mdash; the "intuitive" release. v1.5 turns the surface intuitive: shape-as-container with clip path, rotate / scale + per-layer z-index, advanced tables (row span, zebra, totals, repeating header), and two new theme-driven cinematic templates (`InvoiceTemplateV2`, `ProposalTemplateV2`). v1.5 is fully source-compatible with v1.4 &mdash; every public record gained back-compat constructors that default the new fields. See [`docs/migration-v1-4-to-v1-5.md`](docs/migration-v1-4-to-v1-5.md).

## Who is GraphCompose for?

GraphCompose is built for **server-side Java services that need to generate structured business PDFs** &mdash; the kind of documents your application has to produce on demand from real data, not the kind a human types into Word.

- **Invoices and quotes** generated per request from order data (`InvoiceTemplateV2 + BusinessTheme.modern()` is the canonical entry point).
- **Proposals, statements of work, and reports** with consistent branding across teams (`ProposalTemplateV2`, custom `BusinessTheme`).
- **CVs and cover letters** for ATS exports, hiring tools, and recruiter dashboards (seven modernised CV templates plus the `CvTheme.fromBusinessTheme(...)` bridge).
- **Schedules, dispatch sheets, and operational reports** with deterministic pagination on long data (advanced tables: row span, zebra, totals, repeating header on page break).
- **Internal tooling and admin PDFs** that ship through Spring Boot, Quarkus, Micronaut, Ktor, or any plain Java HTTP service &mdash; `writePdf(OutputStream)` streams straight into a Servlet response without buffering in memory.

Reaching for **iText** for low-level page primitives or **JasperReports** for XML-template-driven layout? GraphCompose sits between them: a Java DSL describes the document semantically, the engine resolves layout and pagination deterministically, and the PDF backend renders the result with PDFBox.

## Visual preview

<p align="center">
  <img src="./assets/readme/repository_showcase_render.png" alt="GraphCompose repository showcase render" width="850"/>
</p>

The proposal screenshot above is built by [`CinematicProposalFileExample`](./examples/src/main/java/com/demcha/examples/CinematicProposalFileExample.java) in the runnable `examples/` module &mdash; a single Java file, no XML, no template engine.

> 📚 **[Browse the full examples gallery →](./examples/README.md)** — every one of the 22 examples with description, key DSL snippet, committed PDF preview, and source link.

## Installation

Distributed through JitPack.

```xml
<repositories>
    <repository><id>jitpack.io</id><url>https://jitpack.io</url></repository>
</repositories>

<dependency>
    <groupId>com.github.DemchaAV</groupId>
    <artifactId>GraphCompose</artifactId>
    <version>v1.5.0</version>
</dependency>
```

```kotlin
repositories { maven("https://jitpack.io") }
dependencies { implementation("com.github.demchaav:GraphCompose:v1.5.0") }
```

The DOCX backend depends on `org.apache.poi:poi-ooxml`, declared as `optional` &mdash; add it explicitly when you call `session.export(new DocxSemanticBackend())`.

## Quick start

The fastest path to a designed PDF is the v1.5 cinematic stack: pick a `BusinessTheme`, drop a `softPanel + accentLeft` hero block on the page, and let the theme drive every colour, font, and table style.

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

For an HTTP response, S3 upload, or in-memory generation, use the `writePdf(OutputStream)` overload &mdash; it streams directly and does **not** close the caller's stream. See [`HttpStreamingExample`](./examples/src/main/java/com/demcha/examples/HttpStreamingExample.java) for the Spring Boot `@RestController` pattern.

For built-in templates (`InvoiceTemplateV2`, `ProposalTemplateV2`) and the canonical authoring patterns (builder hierarchy, theme tokens, golden patterns, anti-patterns, 40-line new-template skeleton), read the **[Template authoring cheatsheet](./docs/template-authoring.md)** once before writing your own.

## What's new in v1.5

Five highlights &mdash; full notes in [`CHANGELOG.md`](./CHANGELOG.md).

- **Shape-as-container with clip path.** `addCircle / addEllipse / addContainer` build a `ShapeContainerNode` whose children are clipped via `ClipPolicy.CLIP_PATH`, `CLIP_BOUNDS`, or `OVERFLOW_VISIBLE`. → [recipe](./docs/recipes/shape-as-container.md)
- **Transforms + per-layer z-index.** `rotate / scale` chain naturally on every shape-shaped builder; `LayerStackNode.Layer.zIndex` lets layers declared earlier draw on top of layers declared later. → [recipe](./docs/recipes/transforms.md)
- **Advanced tables.** `DocumentTableCell.rowSpan(int)`, `zebra(odd, even)`, `totalRow(...)`, and `repeatHeader()` cover the four features most rendered reports need. → [recipe](./docs/recipes/tables.md)
- **Two cinematic templates.** `InvoiceTemplateV2(BusinessTheme)` and `ProposalTemplateV2(BusinessTheme)` &mdash; the same `(theme, spec)` shape, drop-in replacements for V1 when you want the cinematic look.
- **`CvTheme.fromBusinessTheme(BusinessTheme)`** bridges the business theme tokens into CV-specific layout slots (ADR 0002). The seven CV templates each gain a `(CvTheme)` constructor while keeping a no-arg one for legacy palettes.

## Architecture in three lines

- **Public authoring** lives in `com.demcha.compose`, `document.api`, `document.dsl`, `document.node`, `document.style`, `document.table`, `document.theme`, `font`. Engine internals (`com.demcha.compose.engine.*`) are guarded by `PublicApiNoEngineLeakTest` &mdash; never reach into them from application code.
- **Two passes**: `DocumentSession.layoutGraph()` resolves geometry; rendering consumes the resolved fragments. This is what makes layout snapshots, pagination, and future backends practical.
- **Extension seams**: implement `DocumentNode` + register a `NodeDefinition`; emit a `FragmentPayload` + register a `PdfFragmentRenderHandler`; add a `FixedLayoutBackend<R>` or `SemanticBackend` to target a new format. See [`docs/architecture.md`](./docs/architecture.md) for the full map.

## Documentation

- [**Template authoring cheatsheet**](./docs/template-authoring.md) — read this once before writing your own template
- [Examples gallery](./examples/README.md) — 22 runnable examples with PDF previews
- [Architecture](./docs/architecture.md) · [Lifecycle](./docs/lifecycle.md) · [Production rendering](./docs/production-rendering.md)
- Recipes: [shape-as-container](./docs/recipes/shape-as-container.md) · [transforms](./docs/recipes/transforms.md) · [tables](./docs/recipes/tables.md) · [shapes](./docs/recipes/shapes.md) · [themes](./docs/recipes/themes.md) · [streaming](./docs/recipes/streaming.md) · [extending](./docs/recipes/extending.md)
- [Layout snapshot testing](./docs/layout-snapshot-testing.md) · [Performance numbers](./docs/performance.md) · [Benchmark methodology](./docs/benchmarks.md)
- [Migration v1.4 → v1.5](./docs/migration-v1-4-to-v1-5.md) · [Canonical / legacy parity](./docs/canonical-legacy-parity.md)
- [Contributing](./CONTRIBUTING.md) · [Release process](./docs/release-process.md) · [v1.6 roadmap](./docs/v1.6-roadmap.md)

## Roadmap

v1.6 (the "expressive" release) is in the planning phase &mdash; nested lists, composed table cells, `CanvasLayer` for free-form drawing, a full DOCX backend pass, and the start of Maven Central publishing. Full plan in [`docs/v1.6-roadmap.md`](./docs/v1.6-roadmap.md). Feature requests and bug reports welcome via GitHub Issues.

## License

GraphCompose is released under the MIT License &mdash; see [`LICENSE`](./LICENSE).
