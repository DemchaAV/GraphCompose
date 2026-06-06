# GraphCompose

<p align="center">
  <img src="./assets/GraphComposeLogo.png" alt="GraphCompose logo" width="300"/>
</p>

<p align="center">
  <b>Declarative Java DSL for structured business PDFs.</b><br/>
  Describe what the document <i>says</i>; the engine resolves layout, pagination, themes, and PDFBox rendering. <b>Cinematic by default.</b>
</p>

<p align="center">
  <a href="https://github.com/DemchaAV/GraphCompose/actions/workflows/ci.yml?query=branch%3Amain"><img src="https://img.shields.io/github/actions/workflow/status/DemchaAV/GraphCompose/ci.yml?branch=main&style=for-the-badge&label=CI" alt="CI"/></a>
  <a href="https://github.com/DemchaAV/GraphCompose/releases/latest"><img src="https://img.shields.io/github/v/release/DemchaAV/GraphCompose?style=for-the-badge&label=Release" alt="Latest release"/></a>
  <a href="https://central.sonatype.com/artifact/io.github.demchaav/graph-compose"><img src="https://img.shields.io/maven-central/v/io.github.demchaav/graph-compose?style=for-the-badge&label=Maven%20Central" alt="Maven Central"/></a>
  <img src="https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge&logo=openjdk" alt="Java 17+"/>
  <img src="https://img.shields.io/badge/PDFBox-3.0-red?style=for-the-badge" alt="PDFBox 3.0"/>
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="MIT License"/>
</p>

> **Release status** &mdash;
> 🟢 **Latest stable**: [v1.6.9](https://github.com/DemchaAV/GraphCompose/releases/tag/v1.6.9) (public pixel-level visual-regression API + canonical Javadoc doclint-clean + central-publishing 0.10; zero breaking from v1.6.8)
> &nbsp;·&nbsp; 🟡 **In develop**: v1.7.0 (new canonical DSL primitives — inline shapes, checkboxes, per-corner radii, vertical text seating)
> &nbsp;·&nbsp; See [API stability policy](./docs/api-stability.md) for tier definitions.

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
- **PDFBox isolated, DOCX optional.** Single backend interface. Apache POI&ndash;backed DOCX export is available for compatible semantic content &mdash; see [support matrix](#output-support) for limitations.

Sits between **iText** (low-level page primitives) and **JasperReports** (XML-template-driven layout): a Java DSL describes the document semantically, the engine renders.

## Scope and comparison

### Output support

| Format | Status | Notes |
|---|---|---|
| PDF | Production | Fixed-layout backend on PDFBox 3.0. Full DSL coverage. |
| DOCX | Partial | Semantic export via Apache POI. Unsupported nodes (`shape`, `line`, `ellipse`, `barcode`) are dropped silently &mdash; layout fidelity is best-effort for paragraph / list / table content. |
| PPTX | Skeleton | Validates supported node types and emits a manifest. **Not a real PowerPoint export yet** &mdash; planned only if there is demand. |

### When to use GraphCompose

- **Server-side PDF generation in Java** &mdash; invoices, CVs, reports, proposals, statements, schedules.
- **Templated documents from data** &mdash; themed presets (`ModernProfessional`, `InvoiceTemplateV2`, &hellip;) you parameterise instead of re-styling every time.
- **Regression-tested layouts** &mdash; `DocumentSession#layoutSnapshot()` makes layout changes visible in PRs before any byte ships; `PdfVisualRegression` adds a pixel-level gate for font and colour fidelity.
- **Streaming PDFs from web backends** &mdash; Spring Boot `@RestController` writing straight to the response ([`HttpStreamingExample`](./examples/src/main/java/com/demcha/examples/features/streaming/HttpStreamingExample.java)).
- **Higher-level than PDFBox, lighter than JasperReports** &mdash; Java DSL describes semantics; no XML templates, no manual coordinates.

### What GraphCompose is not

- Not a hosted PDF rendering service &mdash; it is a library you embed.
- Not a WYSIWYG editor &mdash; the DSL is code, not drag-and-drop.
- Not a reporting engine like JasperReports &mdash; no datasource bindings, no XML templates, no compiled `.jasper` files.
- Not a browser / HTML-to-PDF renderer &mdash; the engine has its own layout pipeline; HTML/CSS input is not supported.

### Compared with similar Java libraries

| Library | API style | Layout | License | Best for |
|---|---|---|---|---|
| **GraphCompose** | Java DSL, semantic nodes | Two-pass, deterministic, snapshot-testable | MIT | Code-first business documents with layout regression tests |
| **PDFBox** | Low-level text / path primitives | Manual coordinates | Apache 2.0 | Direct PDF manipulation, parsing, extraction |
| **iText 7** | Low-level page primitives + high-level helpers | Manual + helpers | AGPL / commercial | When AGPL is acceptable or you have a commercial licence |
| **OpenPDF** | iText 4 fork | Manual + helpers | LGPL / MPL | Legacy iText 4 codebases |
| **JasperReports** | XML templates compiled to `.jasper` | Template-driven | LGPL | Tabular reports with datasource bindings |

GraphCompose uses PDFBox under the hood as the rendering backend &mdash; the comparison is about authoring surface, not the renderer.

### Which API should I use?

| You want to&hellip; | Surface | Entry point |
|---|---|---|
| Generate a one-off PDF programmatically | DSL | `GraphCompose.document(...).pageFlow(...)` &mdash; see [Hello world](#hello-world) below |
| Generate a CV / cover letter from data | Layered templates | `ModernProfessional.create().compose(session, cvDocument)` &mdash; see [layered templates](./docs/templates/v2-layered/README.md) |
| Add a custom visual primitive | Engine extension | `NodeDefinition` + `PdfFragmentRenderHandler` &mdash; see [extension guide](./docs/contributing/extension-guide.md) |
| Regression-test generated layouts | Layout snapshots | `DocumentSession#layoutSnapshot()` &mdash; quickstart at [Testing your document](./docs/operations/test-your-document.md); full reference at [snapshot testing](./docs/operations/layout-snapshot-testing.md) |
| Pixel-test the rendered PDF (fonts, colours, anti-aliasing) | Visual regression | `PdfVisualRegression.standard()&hellip;assertMatchesBaseline(...)` &mdash; see [visual regression testing](./docs/operations/visual-regression-testing.md) |
| See the live playground / gallery | Next.js showcase site | [Showcase](https://DemchaAV.github.io/GraphCompose/) &mdash; source under [`site/`](./site), built with `next build` and deployed via the [Pages workflow](./.github/workflows/deploy-site.yml) |

## Installation

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose</artifactId>
    <version>1.6.9</version>
</dependency>
```

```kotlin
dependencies { implementation("io.github.demchaav:graph-compose:1.6.9") }
```

> **Distribution** &mdash; Maven Central is the canonical channel from **v1.6.6** onwards
> (`io.github.demchaav:graph-compose:<version>`). Hosted Javadocs auto-publish to
> [javadoc.io/doc/io.github.demchaav/graph-compose](https://javadoc.io/doc/io.github.demchaav/graph-compose)
> shortly after each Central release. The legacy JitPack URL
> (`com.github.DemchaAV:GraphCompose:v<version>`) remains resolvable for callers
> pinned to v1.6.5 and earlier but is no longer the documented install option.

> **Upgrading from v1.5?** Core document authoring stays source-compatible &mdash; engine, DSL, themes, and backend-neutral records carry v1.5 callers unchanged. **Templates v2** replaces the legacy CV / cover-letter template classes; legacy classes were **deleted**, not deprecated. Read the [migration guide](./docs/roadmaps/migration-v1-5-to-v1-6.md) before upgrading template-heavy code.

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

- **Layered templates** &mdash; 14 CV and 14 paired cover-letter presets on the layered `cv.v2` / `coverletter.v2` architecture (data → theme → components → widgets → presets), one-liner `create()` factories over a typed `CvDocument` / `CoverLetterDocument`. Inline markdown, multi-column layouts. The going-forward standard for new template families. See [`docs/templates/v2-layered/README.md`](./docs/templates/v2-layered/README.md). (The earlier `BusinessTheme`-based preset surface is now deprecated.)
- **Composed primitives** &mdash; `ListBuilder.addItem(label, Consumer)` (nested lists), `DocumentTableCell.node(...)` (any node inside a cell), `CanvasLayerNode` (pixel-precise free-canvas placement).
- **Architecture hardening** &mdash; `@Internal` API stability marker, public `PdfFragmentRenderHandler` SPI, `DocumentRenderingException` on the convenience render path, documented thread-safety contract.

Full notes in [`CHANGELOG.md`](./CHANGELOG.md). Upgrade guide: [`docs/roadmaps/migration-v1-5-to-v1-6.md`](./docs/roadmaps/migration-v1-5-to-v1-6.md).

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

📚 **[Full docs index](./docs/README.md)** &mdash; categorised map of every doc, ADR, and recipe. Start there to navigate the documentation.

### Templates
- 🆕 [**Templates — v2 layered architecture**](./docs/templates/v2-layered/README.md) &mdash; the canonical going-forward pattern for new template families (CV v2 is the reference implementation). Personas: [quickstart](./docs/templates/v2-layered/quickstart.md) · [using templates](./docs/templates/v2-layered/using-templates.md) · [authoring presets](./docs/templates/v2-layered/authoring-presets.md) · [contributing a new family](./docs/templates/v2-layered/contributor-guide.md).
- [Templates v1-classic landing](./docs/templates/v1-classic/README.md) &mdash; the older `BusinessTheme` / `CvSpec` CV / cover-letter / invoice / proposal preset library (**deprecated** — CV + cover letter are superseded by [v2-layered](./docs/templates/v2-layered/README.md); invoice / proposal / schedule are not yet ported). Cheat sheet: [authoring](./docs/templates/v1-classic/authoring.md).

### Architecture & operations
- [Architecture overview](./docs/architecture/overview.md) · [Lifecycle](./docs/architecture/lifecycle.md) · [Production rendering](./docs/operations/production-rendering.md) · [Layout snapshot testing](./docs/operations/layout-snapshot-testing.md)

### Recipes & examples
- [Recipes index](./docs/recipes.md) &mdash; [shape-as-container](./docs/recipes/shape-as-container.md) · [shapes](./docs/recipes/shapes.md) · [transforms](./docs/recipes/transforms.md) · [page-backgrounds](./docs/recipes/page-backgrounds.md) · [layered-page-design](./docs/recipes/layered-page-design.md) · [absolute-placement](./docs/recipes/absolute-placement.md) · [tables](./docs/recipes/tables.md) · [themes](./docs/recipes/themes.md) · [streaming](./docs/recipes/streaming.md) · [extending](./docs/recipes/extending.md) · [font-coverage](./docs/font-coverage.md)
- [Examples gallery](./examples/README.md) &mdash; every runnable example with PDF preview

### Contributing & releases
- [Contributing](./CONTRIBUTING.md) · [Code of conduct](./CODE_OF_CONDUCT.md) · [Security policy](./SECURITY.md) · [Release process](./docs/contributing/release-process.md)
- [API stability policy](./docs/api-stability.md) · [Which template system?](./docs/templates/which-template-system.md) · [Migration v1.5 → v1.6](./docs/roadmaps/migration-v1-5-to-v1-6.md)

## Companion projects

- [**graphcompose-ai-flow**](https://github.com/DemchaAV/graphcompose-ai-flow) &mdash; experimental sister project exploring an AI-assisted authoring flow on top of GraphCompose. Independent codebase, separate lifecycle &mdash; nothing in this repo depends on it. Track it if you are interested in agentic document composition driven by the same semantic node model.

## License

MIT &mdash; see [`LICENSE`](./LICENSE).
