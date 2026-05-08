# GraphCompose

<p align="center">
  <img src="./assets/GraphComposeLogo.png" alt="GraphCompose logo" width="300"/>
</p>

<p align="center">
  <b>Java-first declarative document layout engine for cinematic PDFs.</b><br/>
  Describe what the document <i>says</i>; the engine resolves layout, pagination, and PDFBox rendering.
</p>

<p align="center">
  <a href="https://github.com/DemchaAV/GraphCompose/actions/workflows/ci.yml?query=branch%3Amain"><img src="https://img.shields.io/github/actions/workflow/status/DemchaAV/GraphCompose/ci.yml?branch=main&style=for-the-badge&label=CI" alt="CI"/></a>
  <a href="https://github.com/DemchaAV/GraphCompose/releases/latest"><img src="https://img.shields.io/github/v/release/DemchaAV/GraphCompose?style=for-the-badge&label=Release" alt="Latest release"/></a>
  <a href="https://jitpack.io/#DemchaAV/GraphCompose"><img src="https://img.shields.io/jitpack/v/github/DemchaAV/GraphCompose?style=for-the-badge&label=JitPack" alt="JitPack"/></a>
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" alt="Java 21"/>
  <img src="https://img.shields.io/badge/PDFBox-3.0-red?style=for-the-badge" alt="PDFBox 3.0"/>
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="MIT License"/>
</p>

<p align="center">
  <a href="https://demchaav.github.io/GraphCompose/"><b>Live Showcase</b></a>
  &nbsp;·&nbsp;
  <a href="./examples/README.md"><b>Examples Gallery</b></a>
  &nbsp;·&nbsp;
  <a href="./CHANGELOG.md"><b>Changelog</b></a>
</p>

<p align="center">
  <img src="./assets/readme/repository_showcase_render.png" alt="GraphCompose render preview" width="780"/>
</p>

## Why GraphCompose

- **Author intent, not coordinates.** Fluent DSL for sections, paragraphs, tables, lists, layer stacks, themes &mdash; the engine handles measurement, pagination, and rendering.
- **Deterministic by design.** Two-pass layout. Snapshots are stable across machines, so layout regressions are catchable in tests before any byte ships.
- **Cinematic-by-default.** `BusinessTheme` + soft panels + accent strips + transforms + advanced tables are first-class primitives, not workarounds.
- **PDFBox isolated, DOCX optional.** Single backend interface. Apache POI&ndash;backed DOCX export is one method call away.

Sits between **iText** (low-level page primitives) and **JasperReports** (XML-template-driven layout): a Java DSL describes the document semantically, the engine renders.

## Installation

```xml
<repositories>
    <repository><id>jitpack.io</id><url>https://jitpack.io</url></repository>
</repositories>

<dependency>
    <groupId>com.github.DemchaAV</groupId>
    <artifactId>GraphCompose</artifactId>
    <version>v1.6.0</version>
</dependency>
```

```kotlin
repositories { maven("https://jitpack.io") }
dependencies { implementation("com.github.demchaav:GraphCompose:v1.6.0") }
```

> **Distribution status** &mdash; currently **JitPack**. Maven Central is planned for v1.7 ([tracking issue](https://github.com/DemchaAV/GraphCompose/issues/7)).

> **Upgrading from v1.5?** Core document authoring stays source-compatible &mdash; engine, DSL, themes, and backend-neutral records carry v1.5 callers unchanged. **Templates v2** replaces the legacy CV / cover-letter template classes; legacy classes were **deleted**, not deprecated. Read the [migration guide](./docs/migration-v1-5-to-v1-6.md) before upgrading template-heavy code.

## Hello world

```java
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.theme.BusinessTheme;

import java.nio.file.Path;

class Hello {
    public static void main(String[] args) throws Exception {
        BusinessTheme theme = BusinessTheme.modern();

        try (DocumentSession document = GraphCompose.document(Path.of("hello.pdf"))
                .pageSize(DocumentPageSize.A4)
                .pageBackground(theme.pageBackground())
                .margin(28, 28, 28, 28)
                .create()) {

            document.pageFlow(page -> page
                    .addSection("Hero", section -> section
                            .softPanel(theme.palette().surfaceMuted(), 10, 14)
                            .accentLeft(theme.palette().accent(), 4)
                            .addParagraph(p -> p.text("GraphCompose").textStyle(theme.text().h1()))
                            .addParagraph(p -> p.text("A theme-driven hero, no manual coordinates.")
                                    .textStyle(theme.text().body()))));

            document.buildPdf();
        }
    }
}
```

For a Spring Boot `@RestController` streaming the PDF straight to the response, see [`HttpStreamingExample`](./examples/src/main/java/com/demcha/examples/features/streaming/HttpStreamingExample.java).

## What's in v1.6 &mdash; "expressive"

- **Templates v2** &mdash; 14 CV and 14 paired cover-letter presets, theme-driven via `BusinessTheme`, one-liner `create(theme)` factories. Inline markdown, slot-based multi-column layouts. See [`docs/templates-v2.md`](./docs/templates-v2.md).
- **Composed primitives** &mdash; `ListBuilder.addItem(label, Consumer)` (nested lists), `DocumentTableCell.node(...)` (any node inside a cell), `CanvasLayerNode` (pixel-precise free-canvas placement).
- **Architecture hardening** &mdash; `@Internal` API stability marker, public `PdfFragmentRenderHandler` SPI, `DocumentRenderingException` on the convenience render path, documented thread-safety contract.

Full notes in [`CHANGELOG.md`](./CHANGELOG.md). Upgrade guide: [`docs/migration-v1-5-to-v1-6.md`](./docs/migration-v1-5-to-v1-6.md).

## v1.6 primitives in 30 lines

Three snippets, one per new primitive. Full runnable versions live in the [examples gallery](./examples/README.md).

**Nested list** &mdash; builder-callback child scopes with a per-depth marker cascade.

```java
document.pageFlow().addList(list -> list
    .addItem("Backend platform", row -> row
        .addItem("Java 21, Spring Boot, PostgreSQL")
        .addItem("REST APIs and event-driven services"))
    .addItem("Document generation", row -> row
        .addItem("PDF rendering pipeline")
        .addItem("Layout snapshot tests")));
```

**Composed table cell** &mdash; any composable node inside a cell, two-pass row measurement.

```java
DocumentTableCell richSummary = DocumentTableCell.node(
        new ParagraphNode("Summary",
                "**Q3 results** were *strong* — revenue grew 18% YoY.",
                bodyStyle, TextAlign.LEFT, 1.0,
                DocumentInsets.zero(), DocumentInsets.zero()));
```

**Canvas layer** &mdash; pixel-precise `(x, y)` placement inside a fixed bounding box.

```java
document.pageFlow().addCanvas(523, 360, canvas -> canvas
        .clipPolicy(ClipPolicy.CLIP_BOUNDS)
        .position(headline, 0, 60)
        .position(rule(503, 1.4, accent), 10, 32));
```

## Documentation

- [Template authoring cheatsheet](./docs/template-authoring.md) &mdash; read this once before writing your own template
- [Templates v2 landing](./docs/templates-v2.md) &mdash; CV / cover-letter / invoice / proposal preset library
- [Examples gallery](./examples/README.md) &mdash; every runnable example with PDF preview
- [Architecture](./docs/architecture.md) · [Lifecycle](./docs/lifecycle.md) · [Production rendering](./docs/production-rendering.md)
- Recipes: [shape-as-container](./docs/recipes/shape-as-container.md) · [transforms](./docs/recipes/transforms.md) · [tables](./docs/recipes/tables.md) · [themes](./docs/recipes/themes.md) · [streaming](./docs/recipes/streaming.md) · [extending](./docs/recipes/extending.md)
- [Migration v1.5 → v1.6](./docs/migration-v1-5-to-v1-6.md) · [Release process](./docs/release-process.md) · [Contributing](./CONTRIBUTING.md)

## License

MIT &mdash; see [`LICENSE`](./LICENSE).
