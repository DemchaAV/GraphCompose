# GraphCompose

<p align="center">
  <img src="./assets/GraphComposeLogo.png" alt="GraphCompose logo" width="300"/>
</p>

<p align="center">
  <b>Java-first declarative document layout engine for programmatic PDF generation.</b><br/>
  Describe semantic document structure; GraphCompose handles layout, pagination, snapshots, and PDFBox rendering.
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

Most Java PDF libraries expose low-level drawing commands. GraphCompose gives Java applications a semantic authoring model:

- `GraphCompose.document(...)` is the canonical public entry point
- `DocumentSession` owns lifecycle, layout, snapshots, and rendering
- `DocumentDsl` builds modules, paragraphs, lists, tables, images, dividers, and page breaks
- automatic pagination and deterministic layout snapshots are built into the engine
- PDF rendering is isolated behind a PDFBox backend

The current release is **v1.3.0**, expanding the canonical authoring path with horizontal rows, per-side borders, auto-size text, session-level PDF chrome (metadata / watermark / protection / header / footer), and a functional Apache POI based DOCX semantic backend. The release also lands engine-side perf work (templates render 19–30 % faster than v1.2.0 on the canonical benchmark suite).

## Visual Preview

<p align="center">
  <img src="./assets/readme/repository_showcase_render.png" alt="GraphCompose repository showcase render" width="850"/>
</p>

<p align="center">
  <img src="./assets/readme/compose-first-invoice-template.png" alt="GraphCompose compose-first invoice template" width="850"/>
</p>

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
    <version>v1.3.0</version>
</dependency>
```

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.demchaav:GraphCompose:v1.3.0")
}
```

The project POM coordinates are `io.github.demchaav:graphcompose:1.3.0`. JitPack keeps the GitHub repository coordinate with a lowercase owner (`com.github.demchaav:GraphCompose:v1.3.0`) and the `v1.3.0` tag. The DOCX backend depends on `org.apache.poi:poi-ooxml`, declared as `optional` — add it explicitly when you call `session.export(new DocxSemanticBackend())`.

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

The runnable `examples/` module includes CV, cover letter, invoice, proposal, weekly schedule, and module-first documents.

## Core Concepts

### 1. Documents are semantic first

Application code describes modules, paragraphs, lists, rows, tables, images, and dividers. The engine turns those semantic nodes into measured, paginated render fragments.

### 2. Layout and rendering are separate passes

The layout pass resolves geometry first. Rendering consumes already resolved pages and fragments. This is what makes snapshots, pagination, and future backends practical.

### 3. Layout traversal is deterministic

GraphCompose builds stable tree order, parent links, page spans, and coordinates so tests can compare layout snapshots before any PDF bytes are written.

### 4. Containers express structure

Use `document.pageFlow()` for the root flow, `module()` for full-width document blocks, and `section()` for nested grouping. Absolute coordinates stay inside the engine.

### 5. The template layer is optional

Use built-in templates when they fit, or compose your own document directly with `DocumentSession` and the DSL.

## Table component

```java
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;

document.pageFlow()
        .name("StatusSection")
        .spacing(12)
        .addTable(table -> table
                .name("StatusTable")
                .columns(
                        DocumentTableColumn.fixed(90),
                        DocumentTableColumn.auto(),
                        DocumentTableColumn.auto())
                .width(520)
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.of(6))
                        .build())
                .headerStyle(DocumentTableStyle.builder()
                        .fillColor(DocumentColor.LIGHT_GRAY)
                        .padding(DocumentInsets.of(6))
                        .build())
                .header("Role", "Owner", "Status")
                .rows(
                        new String[]{"Engine", "GraphCompose", "Stable"},
                        new String[]{"Feature", "Table Builder", "Canonical"}))
        .build();
```

## Line primitive

```java
import com.demcha.compose.document.style.DocumentColor;

document.pageFlow()
        .name("LinePrimitives")
        .spacing(12)
        .addDivider(divider -> divider
                .name("HorizontalRule")
                .width(220)
                .thickness(3)
                .color(DocumentColor.ROYAL_BLUE))
        .addShape(shape -> shape
                .name("VerticalAccent")
                .size(3, 90)
                .fillColor(DocumentColor.ORANGE))
        .build();
```

## Architecture at a glance

```mermaid
graph TD
    UserCode["Application code<br/>GraphCompose.document + DocumentDsl"]
    Semantic["Semantic document nodes"]
    Layout["Deterministic layout + pagination"]
    Snapshot["Layout snapshots"]
    PdfBackend["PDFBox backend"]
    Future["Future backends"]

    UserCode --> Semantic
    Semantic --> Layout
    Layout --> Snapshot
    Layout --> PdfBackend
    Layout -.-> Future
```

Public authoring lives in `com.demcha.compose`, `document.api`, `document.dsl`, `document.node`, `document.style`, `document.table`, and `font`. Engine internals live under `com.demcha.compose.engine.*` and are not the recommended application API.

## What's new in v1.3

- **Horizontal rows**: `addRow(...)` on flows, sections, and modules with optional `weights(...)` and `gap(...)`. Children render side-by-side; the row is one atomic block from the paginator's perspective.
- **Per-side borders**: `DocumentBorders` value type and `borders(...)` on flows, sections, modules, and rows. Each side carries an optional stroke; per-side borders override the uniform `stroke(...)` setting.
- **Auto-size paragraph text**: `ParagraphBuilder.autoSize(maxSize, minSize)` searches for the largest font that fits the paragraph on a single line within the resolved inner width.
- **Session-level PDF chrome**: `DocumentSession.metadata(...)`, `watermark(...)`, `protect(...)`, `header(...)`, and `footer(...)` apply through the convenience entrypoints (`buildPdf`, `writePdf`, `toPdfBytes`) without an explicit backend builder.
- **Backend-neutral output options**: types in `com.demcha.compose.document.output` (`DocumentMetadata`, `DocumentWatermark`, `DocumentProtection`, `DocumentHeaderFooter`) — both PDF and DOCX backends translate them. Session-level metadata propagates to DOCX core properties.
- **Functional DOCX export**: `session.export(new DocxSemanticBackend())` emits real DOCX bytes (paragraphs, tables, images, spacers, page breaks, page geometry) via Apache POI. POI is declared `optional`, so PDF-only consumers do not pay the dependency cost.
- **Engine performance**: pagination priority queue uses `UUID.compareTo` (no per-compare string allocation), `Entity` hot-path component lookups dropped per-call debug logging, and table layout helpers were extracted into `TableLayoutSupport` for clarity. Templates render 19–30 % faster than v1.2.0 on the canonical benchmark suite.

## Performance (smoke profile, post-v1.3)

Numbers below come from `CurrentSpeedBenchmark` (`-Dgraphcompose.benchmark.profile=smoke`, 30 warmup + 100 measurement, with `System.gc()` between warmup and measurement). They were captured on a developer laptop; CI machines are typically 1.5–2× slower.

| Scenario          | Avg ms | p50 ms | p95 ms | Docs/sec |
|-------------------|-------:|-------:|-------:|---------:|
| engine-simple     |   1.83 |   1.67 |   2.78 |   546.84 |
| invoice-template  |  10.77 |  10.76 |  13.46 |    92.89 |
| cv-template       |   6.50 |   5.63 |  10.27 |   153.81 |
| proposal-template |  13.44 |  13.36 |  15.42 |    74.43 |
| feature-rich      |  34.28 |  29.52 |  34.87 |    29.18 |

The same harness reports a per-stage breakdown so consumers can attribute regressions to compose / layout / render independently:

| Scenario          | Compose | Layout | Render | Total |
|-------------------|--------:|-------:|-------:|------:|
| invoice-template  |    0.18 |   2.10 |   5.17 |  7.60 |
| cv-template       |    0.16 |   2.20 |   1.35 |  3.75 |
| proposal-template |    0.22 |   5.82 |   5.93 | 12.40 |

Render time is dominated by PDFBox serialization (35–68 % of total), so engine-side optimisations look smaller in the end-to-end avg than they do in the layout column. See [docs/benchmarks.md](./docs/benchmarks.md) for the full methodology.

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
- [ ] Maven Central release
- [ ] real PPTX export (v1.3 ships a manifest skeleton)
- [ ] child horizontal/vertical alignment, nested list builder, complex table cell composition

## License

MIT. See [LICENSE](./LICENSE).
