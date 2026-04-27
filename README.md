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

What you get out of the box:

- **Author intent, not coordinates.** `GraphCompose.document(...) -> DocumentSession -> DocumentDsl` is a fluent builder for sections, modules, paragraphs, lists, tables, images, dividers, page-breaks, and (since v1.4) layer stacks.
- **Deterministic layout.** Two passes &mdash; layout resolves geometry, render consumes resolved fragments. Snapshots are stable across runs and machines, so you can regression-test layout before any PDF byte is written.
- **Atomic pagination, no manual paging.** Tables split row-by-row, rows are atomic, layer stacks are atomic, and the paginator keeps owner placement and page spans coherent.
- **Designer-grade output.** Page backgrounds, section bands, soft panels, accent strips, column spans, layered hero blocks, fluent rich text, and a tokenised `BusinessTheme` are all first-class &mdash; not workarounds.
- **PDFBox rendering, isolated.** PDF backend lives behind a single backend interface. The DOCX semantic backend (Apache POI) is ready for callers who need an editable file.
- **Tested at every layer.** 525 green tests on `main`, including 41 cinematic-feature tests, public-API leak guards, semantic-vs-engine isolation guards, and a brand-new `PdfVisualRegression` harness for screenshot-level checks.

The current release is **v1.4.0** &mdash; the "cinematic" release. v1.3 stabilised the core (rows, per-side borders, auto-size text, DOCX export); v1.4 lands the visual-design layer that turns "tidy PDF" into "designed document".

## Visual preview

<p align="center">
  <img src="./assets/readme/repository_showcase_render.png" alt="GraphCompose repository showcase render" width="850"/>
</p>

<p align="center">
  <img src="./assets/readme/compose-first-invoice-template.png" alt="GraphCompose compose-first invoice template" width="850"/>
</p>

The proposal screenshot above is built by [`CinematicProposalFileExample`](./examples/src/main/java/com/demcha/examples/CinematicProposalFileExample.java) in the runnable `examples/` module &mdash; a single Java file, no XML, no template engine.

## Installation

GraphCompose is currently distributed through JitPack.

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.DemchaAV</groupId>
    <artifactId>GraphCompose</artifactId>
    <version>v1.4.0</version>
</dependency>
```

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.demchaav:GraphCompose:v1.4.0")
}
```

The project POM coordinates are `io.github.demchaav:graphcompose:1.4.0`. JitPack keeps the GitHub repository coordinate with a lowercase owner (`com.github.demchaav:GraphCompose:v1.4.0`) and the `v1.4.0` tag. The DOCX backend depends on `org.apache.poi:poi-ooxml`, declared as `optional` &mdash; add it explicitly when you call `session.export(new DocxSemanticBackend())`.

## Quick start

```java
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;

import java.nio.file.Path;

public class QuickStart {
    public static void main(String[] args) throws Exception {
        try (DocumentSession document = GraphCompose.document(Path.of("output.pdf"))
                .pageSize(DocumentPageSize.A4)
                .margin(24, 24, 24, 24)
                .create()) {

            document.pageFlow(page -> page
                    .module("Summary", module -> module.paragraph("Hello GraphCompose")));

            document.buildPdf();
        }
    }
}
```

For HTTP responses, S3 uploads, or in-memory generation:

```java
try (DocumentSession document = GraphCompose.document()
        .pageSize(DocumentPageSize.A4)
        .margin(24, 24, 24, 24)
        .create()) {

    document.pageFlow(page -> page
            .module("Summary", module -> module.paragraph("In-memory PDF")));

    document.writePdf(responseOutputStream);
    byte[] pdfBytes = document.toPdfBytes();
}
```

### Built-in templates (compose-first)

```java
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.InvoiceTemplate;
import com.demcha.compose.document.templates.builtins.InvoiceTemplateV1;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;

import java.nio.file.Path;

InvoiceDocumentSpec invoice = InvoiceDocumentSpec.builder()
        .invoiceNumber("GC-2026-041")
        .issueDate("02 Apr 2026")
        .dueDate("16 Apr 2026")
        .fromParty(party -> party.name("GraphCompose Studio"))
        .billToParty(party -> party.name("Northwind Systems"))
        .lineItem("Template architecture", "Reusable invoice flow", "2", "GBP 980", "GBP 1,960")
        .totalRow("Total", "GBP 1,960")
        .build();

InvoiceTemplate template = new InvoiceTemplateV1();

try (DocumentSession document = GraphCompose.document(Path.of("invoice.pdf"))
        .pageSize(DocumentPageSize.A4)
        .margin(22, 22, 22, 22)
        .create()) {

    template.compose(document, invoice);
    document.buildPdf();
}
```

The runnable `examples/` module includes CV, cover letter, invoice, proposal, cinematic proposal, weekly schedule, and module-first documents:

```bash
./mvnw -f examples/pom.xml clean package
./mvnw -f examples/pom.xml exec:java -Dexec.mainClass=com.demcha.examples.GenerateAllExamples
```

Each example writes a PDF to `examples/build/`.

## Core concepts

### 1. Documents are semantic first

Application code describes modules, paragraphs, lists, rows, tables, images, dividers, page-breaks, and (v1.4) layer stacks. The engine turns those semantic nodes into measured, paginated render fragments.

### 2. Layout and rendering are separate passes

`DocumentSession.layoutGraph()` resolves geometry first. Rendering consumes already resolved pages and fragments. This is what makes snapshots, pagination, and future backends practical.

### 3. Layout traversal is deterministic

GraphCompose builds stable tree order, parent links, page spans, and coordinates so tests can compare layout snapshots before any PDF bytes are written.

### 4. Containers express structure

Use `document.pageFlow()` for the root flow, `module()` for full-width document blocks, `section()` for nested grouping, `addRow(...)` for horizontal columns, and `LayerStackBuilder` (v1.4) for stacked layers. Absolute coordinates stay inside the engine.

### 5. The template layer is optional

Use built-in templates when they fit, or compose your own document directly with `DocumentSession` and the DSL. Templates are themselves authored against the same semantic API.

---

## What's new in v1.4 &mdash; "cinematic"

Six designer-grade features lift GraphCompose from a tidy PDF layouter to a cinematic document engine.

### Column spans

A single `DocumentTableCell.colSpan(n)` lets one cell occupy multiple columns &mdash; clean totals rows, header groups, mid-table section dividers.

```java
document.pageFlow().addTable(table -> table
        .columns(
            DocumentTableColumn.fixed(150),
            DocumentTableColumn.fixed(80),
            DocumentTableColumn.fixed(80),
            DocumentTableColumn.fixed(100))
        .header("Item", "Qty", "Unit", "Amount")
        .row("Coffee beans", "12", "$15.00", "$180.00")
        .row("Filters",      "4",  "$5.00",  "$20.00")
        .rowCells(
            DocumentTableCell.text("Total").colSpan(3)
                    .withStyle(DocumentTableStyle.builder()
                            .fillColor(DocumentColor.LIGHT_GRAY)
                            .build()),
            DocumentTableCell.text("$200.00")));
```

`TableLayoutSupport` validates that `sum(colSpan) == columnCount` per row, distributes any extra width to `auto` columns inside the span, and keeps border ownership consistent. Spanned cells emit a single `TableResolvedCell` &mdash; no renderer change needed.

### Layer stacks (overlay primitive)

`LayerStackNode` composes children inside the same bounding box, in source order &mdash; first child behind, last in front. Each layer carries one of nine `LayerAlign` values. Pagination is atomic.

```java
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.node.LayerAlign;

document.add(new LayerStackBuilder()
        .name("Hero")
        .back(heroBackgroundShape)                // rendered behind, top-left
        .center(heroContent)                      // centered on top
        .layer(badge, LayerAlign.TOP_RIGHT)       // anchored to upper-right corner
        .build());
```

Use cases the engine could not express cleanly before:

- **background panels** under a section
- **watermark blocks** in front of body content
- **hero banners** combining a colored shape and centered headline
- **decorative lines** under text
- **status badges** anchored to the top-right of an invoice

### Page and section backgrounds

Document-wide page tint &mdash; one fluent setter, no template magic:

```java
GraphCompose.document(Path.of("proposal.pdf"))
        .pageSize(DocumentPageSize.A4)
        .pageBackground(new Color(252, 248, 240))  // cream paper for every page
        .create();
```

Internally, `DocumentSession.layoutGraph()` injects a full-canvas `ShapeFragmentPayload` at the start of every page after layout compile &mdash; the existing PDF backend draws it as a normal shape, no backend changes.

For section-scoped designs, `AbstractFlowBuilder` now ships ergonomic preset shortcuts:

```java
section
    .band(navy)                                   // full-width colored band
    .softPanel(palePink)                          // fill + 8pt corner radius + 12pt padding
    .softPanel(slate, 16, 20)                     // custom radius + padding
    .accentLeft(navy, 4)                          // left edge accent strip
    .accentBottom(navy, 2);                       // bottom rule under a header
```

Each shortcut is a thin alias over `fillColor`, `cornerRadius`, `padding`, or `DocumentBorders` &mdash; nothing magic, just better-named.

### Rich-text DSL

Mixed-style runs in a single chained expression &mdash; no need to drop into table cells or split paragraphs to highlight a status keyword.

```java
import com.demcha.compose.document.dsl.RichText;

section.addRich(t -> t
    .plain("Status: ")
    .bold("Pending")
    .plain(" — last review on ")
    .accent("Mar 14", brandBlue));

// Or build a reusable run sequence:
RichText footer = RichText.text("Generated by ").italic("GraphCompose");
pageFlow.addRich(footer);

// Inline links are first-class:
section.addRich(t -> t
    .plain("See ")
    .link("the docs", "https://demcha.io/docs")
    .plain(" for details."));
```

Available run methods: `plain / bold / italic / boldItalic / underline / strikethrough / color / accent / size / style / link / append`.

### Business themes

A single `BusinessTheme` bundles a `DocumentPalette`, `SpacingScale`, `TextScale`, `TablePreset`, and an optional page background, so invoice / proposal / report templates rendered through the same theme look like one product instead of three independently styled documents.

```java
import com.demcha.compose.document.theme.BusinessTheme;

BusinessTheme theme = BusinessTheme.modern();   // cream paper + teal/gold

GraphCompose.document(Path.of("proposal.pdf"))
        .pageBackground(theme.pageBackground())
        .create()
        .pageFlow(page -> page
                .addText("PROJECT PROPOSAL", theme.text().h1())
                .addSection("Overview", s -> s
                        .softPanel(theme.palette().surfaceMuted(),
                                   theme.spacing().sm(),
                                   theme.spacing().md())
                        .addText("Concise delivery plan…", theme.text().body()))
                .addSection("Plan", s -> s
                        .addTable(t -> t
                                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto(), DocumentTableColumn.auto())
                                .defaultCellStyle(theme.table().defaultCellStyle())
                                .headerStyle(theme.table().headerStyle())
                                .header("Phase", "Duration", "Deliverable")
                                .row("Discovery", "2w", "Scope")
                                .row("Design",    "3w", "Mockups"))));
```

Three built-in presets:

| Preset       | Surface                  | Primary           | Accent            | Heading font |
|--------------|--------------------------|-------------------|-------------------|--------------|
| `classic()`  | white                    | navy              | bright blue       | Helvetica-Bold |
| `modern()`   | cream paper (page tint)  | deep teal         | warm gold         | Helvetica-Bold |
| `executive()`| near-white               | slate             | muted gold        | Times-Roman  |

Tokens are exposed as the canonical document types (`DocumentColor`, `DocumentInsets`, `DocumentTextStyle`, `DocumentTableStyle`) &mdash; you can pull individual ones into existing builder calls without buying into the theme wholesale.

### Visual regression for README assets

A new `PdfVisualRegression` harness renders PDF bytes to one PNG per page via PDFBox `PDFRenderer`, compares each page to a baseline under `src/test/resources/visual-baselines/`, and fails with a side-by-side `actual.png` + `diff.png` when the render drifts.

```java
import com.demcha.testing.visual.PdfVisualRegression;

PdfVisualRegression visual = PdfVisualRegression.standard()
        .perPixelTolerance(6)
        .mismatchedPixelBudget(0);

byte[] pdf = session.toPdfBytes();
visual.assertMatchesBaseline("invoice-overview", pdf);
```

To bless a fresh baseline, set `-Dgraphcompose.visual.approve=true` (or `GRAPHCOMPOSE_VISUAL_APPROVE=true`) on the test command. This is the missing layer above layout-snapshot tests &mdash; it catches "the diff is structurally fine but it just looks ugly".

---

## Extending GraphCompose

GraphCompose is built around explicit seams &mdash; you do not have to fork the library to add a new node, a new backend, or a new template family.

- **Add a new semantic node.** Implement `DocumentNode`, register a `NodeDefinition<MyNode>` with the `NodeRegistry`, and the layout compiler picks it up. The definition controls measurement, pagination policy, splitting, and fragment emission. See [`com.demcha.compose.document.layout.NodeDefinition`](./src/main/java/com/demcha/compose/document/layout/NodeDefinition.java) and the built-ins in `BuiltInNodeDefinitions` for the established pattern.
- **Add a fragment payload.** Reuse `BuiltInNodeDefinitions.ShapeFragmentPayload` / `ParagraphFragmentPayload` / `LineFragmentPayload` / `BarcodeFragmentPayload` / `ImageFragmentPayload` &mdash; or define your own and register a matching `PdfFragmentRenderHandler`. The PDF backend dispatches by payload type.
- **Add a fluent builder.** Extend `AbstractFlowBuilder<T, N>` to inherit `addParagraph / addTable / addRow / addSection / addRich / softPanel / accent*` etc. for free.
- **Add a backend.** Implement `FixedLayoutBackend<R>` (PDF-style) or `SemanticBackend` (DOCX/PPTX-style) and consume the resolved `LayoutGraph`. Page background, layer stacks, spans, and theme tokens are all expressed in canonical fragment types &mdash; no engine internals needed.
- **Test for layout regressions.** Use `LayoutSnapshotAssertions` (graph-level) and `PdfVisualRegression` (pixel-level). Both ship in test scope, both gate at the snapshot/baseline level so you can refactor with confidence.

The architecture diagram:

```mermaid
graph TD
    UserCode["Application code<br/>GraphCompose.document + DocumentDsl"]
    Theme["BusinessTheme tokens"]
    Semantic["Semantic document nodes<br/>+ LayerStack, RichText, colSpan"]
    Layout["Deterministic layout + pagination<br/>+ page background injection"]
    Snapshot["Layout snapshots"]
    Visual["Visual regression (PNG diff)"]
    PdfBackend["PDFBox backend"]
    DocxBackend["DOCX (Apache POI) backend"]
    Future["Future backends"]

    UserCode --> Semantic
    Theme --> UserCode
    Semantic --> Layout
    Layout --> Snapshot
    Layout --> Visual
    Layout --> PdfBackend
    Layout --> DocxBackend
    Layout -.-> Future
```

Public authoring lives in `com.demcha.compose`, `document.api`, `document.dsl`, `document.node`, `document.style`, `document.table`, `document.theme` (v1.4), and `font`. Engine internals live under `com.demcha.compose.engine.*` and are not the recommended application API; they are guarded by `PublicApiNoEngineLeakTest`.

## Performance (smoke profile, v1.4)

Numbers below come from `CurrentSpeedBenchmark` (`-Dgraphcompose.benchmark.profile=smoke`, 30 warmup + 100 measurement, with `System.gc()` between warmup and measurement). They were captured on a developer laptop; CI machines are typically 1.5&ndash;2&times; slower.

| Scenario          | Avg ms | p50 ms | p95 ms | Docs/sec |
|-------------------|-------:|-------:|-------:|---------:|
| engine-simple     |   2.01 |   1.93 |   2.82 |   497.47 |
| invoice-template  |  14.29 |  14.04 |  17.18 |    69.98 |
| cv-template       |   7.08 |   6.92 |   8.86 |   141.17 |
| proposal-template |  14.99 |  15.26 |  17.42 |    66.70 |
| feature-rich      |  39.75 |  38.25 |  45.16 |    25.16 |

Per-stage breakdown (median ms per stage):

| Scenario          | Compose | Layout | Render | Total |
|-------------------|--------:|-------:|-------:|------:|
| invoice-template  |   0.31  |  3.11  |  7.13  | 10.70 |
| cv-template       |   0.23  |  2.94  |  2.00  |  5.16 |
| proposal-template |   0.29  |  6.42  |  6.54  | 13.26 |

Render time is dominated by PDFBox serialization (35&ndash;68 % of total), so engine-side optimisations look smaller in the end-to-end avg than they do in the layout column. Page-background injection is a constant 1 fragment per page; column spans, layer stacks, and themes do not change the number of fragments emitted. See [docs/benchmarks.md](./docs/benchmarks.md) for the full methodology and the local `scripts/run-benchmarks.ps1` workflow.

## Documentation

- [Getting Started](./docs/getting-started.md)
- [Recipes](./docs/recipes.md)
- [Architecture](./docs/architecture.md)
- [Package Map](./docs/package-map.md)
- [Lifecycle](./docs/lifecycle.md)
- [Production Rendering](./docs/production-rendering.md)
- [Layout Snapshot Testing](./docs/layout-snapshot-testing.md)
- [Benchmarks](./docs/benchmarks.md)
- [Canonical / Legacy Parity](./docs/canonical-legacy-parity.md)
- [Migration v1.1 to v1.2](./docs/migration-v1-1-to-v1-2.md)
- [v1.2 / v1.3 Roadmap](./docs/v1.2-roadmap.md)
- [Release Process](./docs/release-process.md)
- [Changelog](./CHANGELOG.md)

## Roadmap

- [x] Java semantic DSL
- [x] PDFBox rendering
- [x] automatic pagination
- [x] deterministic layout snapshots
- [x] built-in templates
- [x] public API boundary guards
- [x] horizontal rows + per-side borders + auto-size text (v1.3)
- [x] backend-neutral output options + functional DOCX export (v1.3)
- [x] **table column spans** (v1.4)
- [x] **layer/overlay primitive (`LayerStackNode`)** (v1.4)
- [x] **page background + section presets** (v1.4)
- [x] **rich-text DSL** (v1.4)
- [x] **`BusinessTheme` design tokens** (v1.4)
- [x] **`PdfVisualRegression` harness** (v1.4)
- [ ] table row spans
- [ ] header repeat on page break + zebra rows + total row presets in `TablePreset`
- [ ] anchored overlay positions (e.g. `position(x, y)` inside a layer)
- [ ] Maven Central release
- [ ] real PPTX export (v1.3 ships a manifest skeleton)

## License

MIT. See [LICENSE](./LICENSE).
