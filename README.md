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
> 🟢 **Latest stable**: [v1.9.1](https://github.com/DemchaAV/GraphCompose/releases/tag/v1.9.1) &mdash; patch on the **"navigable"** line ([v1.9.0](https://github.com/DemchaAV/GraphCompose/releases/tag/v1.9.0)): long inline-code chips (Maven coordinates, FQCNs, URLs) now wrap inside their table column instead of spilling over the neighbour. **[What's new in v1.9 &darr;](#whats-new-in-v19)**

> &nbsp;·&nbsp; 🟡 **In progress (2.0)**: modular split into per-concern artifacts, with `graph-compose` kept as a drop-in wrapper (see [CHANGELOG](./CHANGELOG.md) and the [2.0 modules migration guide](./docs/migration/v2.0.0-modules.md))
> &nbsp;·&nbsp; See [API stability policy](./docs/api-stability.md) for tier definitions.

<p align="center">
  <a href="https://demchaav.github.io/GraphCompose/"><b>Live Showcase</b></a>
  &nbsp;·&nbsp;
  <a href="./examples/README.md"><b>Examples Gallery</b></a>
  &nbsp;·&nbsp;
  <a href="./docs/README.md"><b>Docs</b></a>
  &nbsp;·&nbsp;
  <a href="./CHANGELOG.md"><b>Changelog</b></a>
</p>

<p align="center">
  <img src="./assets/readme/repository_showcase_render.png" alt="GraphCompose render preview" width="780"/>
</p>

<p align="center">
  <sub>☝ This banner is itself a GraphCompose document — <a href="./assets/readme/examples/engine-deck.pdf"><b>view the full capability deck (PDF)</b></a>, rendered by <a href="./examples/src/main/java/com/demcha/examples/flagships/EngineDeckExample.java"><code>EngineDeckExample</code></a>: native vector charts and real comparative benchmarks, all drawn by the engine. It renders its own marketing.</sub>
</p>

## Why GraphCompose

- **Author intent, not coordinates.** Fluent DSL for sections, paragraphs, tables, lists, layer stacks, themes &mdash; the engine handles measurement, pagination, and rendering.
- **Deterministic by design.** Two-pass layout. Snapshots are stable across machines, so layout regressions are catchable in tests before any byte ships.
- **Cinematic by default.** Soft panels, accent strips, transforms, native vector charts, and gradients are first-class primitives, not workarounds.
- **PDFBox isolated, DOCX optional.** Single backend interface. Apache POI&ndash;backed DOCX export is available for compatible semantic content &mdash; see [support matrix](#output-support) for limitations.

Sits between **iText** (low-level page primitives) and **JasperReports** (XML-template-driven layout): a Java DSL describes the document semantically, the engine renders.

## What's new in v1.9

The **"navigable"** release &mdash; a rendered PDF becomes a document you can move through.

- **In-document navigation** &mdash; named `anchor(...)` targets and internal `link(...)` jumps emitted as native PDF `GoTo` actions: clickable cross-references, `[text](#heading)`-style links, and bidirectional footnotes (`DocumentLinkTarget` unifies internal and external links).
- **Native table of contents &amp; page references** &mdash; `addTableOfContents(toc -> toc.entry(label, anchor))` builds a clickable TOC with dot leaders and auto-resolved page numbers; `addPageReference(anchor)` prints a native "see page N" cross-reference; `DocumentSession.pageIndex()` resolves any anchor to its page.
- **Bookmarks &amp; viewer preferences** &mdash; `section.bookmark(...)` makes any section or container a PDF outline (bookmark-panel) target, and `chrome().viewerPreferences(...)` opens the reader on the outline panel, a chosen page layout, or the doc title in the window.
- **Multi-section documents** &mdash; `GraphCompose.documents()` concatenates independently authored sections &mdash; each with its own page size, margins, and footer numbering &mdash; into one PDF, with anchors, links, and the outline resolving across section boundaries.
- **Richer row &amp; page layout** &mdash; `row.columns(auto(), weight(1), fixed(80))`, main-axis `flexSpacer()` / `arrangement(...)`, cross-axis `verticalAlign(...)`, per-page `pageMargins(...)`, and full-bleed `bleed(...)`.
- **Inline chips, SVG icons &amp; colour emoji** &mdash; text on a rounded highlight chip, a parsed `SvgIcon` on the text baseline, and `RichText.emoji(":star:", size)` colour emoji via the new independently-versioned `graph-compose-emoji` module.
- **Render straight to images** &mdash; `DocumentSession` renders directly to `BufferedImage`s with no PDF round-trip; plus page-number offset / restart / style and round / dotted line caps.

Core document APIs stay source- and binary-compatible with v1.8 &mdash; v1.9 is purely additive (two cover-letter / CV shim types are newly `@Deprecated` for 2.0). Full notes in [`CHANGELOG.md`](./CHANGELOG.md).

## Installation

**Requires Java 17+** (enforced by the build).

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose</artifactId>
    <version>2.0.0</version>
</dependency>
```

```kotlin
dependencies { implementation("io.github.demchaav:graph-compose:2.0.0") }
```

> **Which artifact? (2.0 module split).** `graph-compose` above is the drop-in default —
> it renders PDF out of the box because it aggregates the lean `graph-compose-core` engine
> plus the `graph-compose-render-pdf` backend, so existing 1.x callers upgrade with **no
> code change**. Reach for a different coordinate only to take less or more:
>
> | Goal | Depend on |
> |---|---|
> | **PDF — the 1.x default** | `graph-compose` |
> | **Batteries-included** (PDF + templates + fonts + emoji) | `graph-compose-bundle` |
> | **Lean core, bring your own backend** | `graph-compose-core` |
> | **Built-in CV / cover-letter / invoice / proposal templates** | add `graph-compose-templates` |
> | **DOCX / PPTX export** | add `graph-compose-render-docx` / `graph-compose-render-pptx` |
>
> Every 2.0 coordinate shares the `graph-compose` version (the fonts and emoji companions
> keep their own lines). A bare `graph-compose-core` renders nothing until a backend is on
> the classpath — asking it to build a PDF throws `MissingBackendException`, which names the
> artifact to add (`graph-compose-render-pdf`, already included in `graph-compose`).

> **Bundled fonts (from v1.8.0).** The curated Google fonts no longer ship
> inside the engine jar &mdash; they live in an independently-versioned
> companion artifact so an engine upgrade never re-downloads ~18&nbsp;MB of
> fonts. Pure-text and standard-14 documents need nothing extra; to use the
> bundled families, add:
>
> ```xml
> <dependency>
>     <groupId>io.github.demchaav</groupId>
>     <artifactId>graph-compose-fonts</artifactId>
>     <version>1.0.0</version>
> </dependency>
> ```
>
> Prefer a single "batteries-included" coordinate? Depend on
> `io.github.demchaav:graph-compose-bundle` (same version as `graph-compose`
> above) to pull the default PDF engine, the built-in templates, the fonts, and
> the colour emoji together. Full details and upgrade steps: the
> [v1.8.0 fonts migration note](./docs/migration/v1.8.0-fonts.md).

> **Colour emoji (from v1.9.0).** `RichText.emoji(":star:", size)` resolves
> GitHub-style shortcodes to inline vector glyphs from an independently-versioned
> companion artifact (the same split model as the fonts above). Text without
> emoji needs nothing extra; to render colour emoji, add:
>
> ```xml
> <dependency>
>     <groupId>io.github.demchaav</groupId>
>     <artifactId>graph-compose-emoji</artifactId>
>     <version>1.0.0</version>
> </dependency>
> ```
>
> An unknown shortcode falls back to its literal text, so a document that uses no
> emoji &mdash; or runs without the artifact &mdash; renders unchanged. Depending
> on `graph-compose` directly keeps emoji opt-in; the batteries-included
> `graph-compose-bundle` includes it.

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
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import java.nio.file.Path;

class Hello {
    public static void main(String[] args) throws Exception {
        // A small inline palette — swap these for your own brand colours.
        DocumentColor cream = DocumentColor.rgb(252, 248, 240);
        DocumentColor panel = DocumentColor.rgb(244, 238, 228);
        DocumentColor accent = DocumentColor.rgb(196, 153, 76);
        DocumentTextStyle h1 = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD).size(28)
                .decoration(DocumentTextDecoration.BOLD)
                .color(DocumentColor.rgb(20, 60, 75)).build();
        DocumentTextStyle body = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA).size(11)
                .color(DocumentColor.rgb(34, 38, 50)).build();

        try (DocumentSession document = GraphCompose.document(Path.of("hello.pdf"))
                .pageSize(DocumentPageSize.A4)
                .pageBackground(cream)
                .margin(28, 28, 28, 28)
                .create()) {

            document.pageFlow(page -> page
                    .addSection("Hero", section -> section
                            .softPanel(panel, 10, 14)
                            .accentLeft(accent, 4)
                            .addParagraph(p -> p.text("GraphCompose").textStyle(h1))
                            .addParagraph(p -> p.text("A cinematic hero, no manual coordinates.")
                                    .textStyle(body))));

            document.buildPdf();
        }
    }
}
```

For a Spring Boot `@RestController` streaming the PDF straight to the response, see [`HttpStreamingExample`](./examples/src/main/java/com/demcha/examples/features/streaming/HttpStreamingExample.java).

## Scope and comparison

### Output support

| Format | Status | Notes |
|---|---|---|
| PDF | Production | Fixed-layout backend on PDFBox 3.0. Full DSL coverage. |
| DOCX | Partial | Semantic export via Apache POI. Unsupported nodes (`shape`, `line`, `ellipse`, `barcode`) are dropped silently &mdash; layout fidelity is best-effort for paragraph / list / table content. |
| PPTX | Skeleton | Validates supported node types and emits a manifest. **Not a real PowerPoint export yet** &mdash; planned only if there is demand. |

### When to use GraphCompose

- **Server-side PDF generation in Java** &mdash; invoices, CVs, reports, proposals, statements, schedules.
- **Templated documents from data** &mdash; themed presets (`ModernProfessional`, `ModernInvoice`, &hellip;) you parameterise instead of re-styling every time.
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
| **iText 7** | Object/layout API + low-level canvas | Automatic layout with direct-positioning options | AGPL / commercial | When AGPL is acceptable or you have a commercial licence |
| **OpenPDF** | iText 4 fork | Manual + helpers | LGPL / MPL | Legacy iText 4 codebases |
| **JasperReports** | XML templates compiled to `.jasper` | Template-driven | LGPL | Tabular reports with datasource bindings |

GraphCompose uses PDFBox under the hood as the rendering backend &mdash; the comparison is about authoring surface, not the renderer.

### Which API should I use?

| You want to&hellip; | Surface | Entry point |
|---|---|---|
| Generate a one-off PDF programmatically | DSL | `GraphCompose.document(...).pageFlow(...)` &mdash; see [Hello world](#hello-world) above |
| Generate a CV / cover letter from data | Layered templates | `ModernProfessional.create().compose(session, cvDocument)` &mdash; see [layered templates](./docs/templates/v2-layered/README.md) |
| Add a custom visual primitive | Engine extension | `NodeDefinition` + `PdfFragmentRenderHandler` &mdash; see [extension guide](./docs/contributing/extension-guide.md) |
| Regression-test generated layouts | Layout snapshots | `DocumentSession#layoutSnapshot()` &mdash; quickstart at [Testing your document](./docs/operations/test-your-document.md); full reference at [snapshot testing](./docs/operations/layout-snapshot-testing.md) |
| Pixel-test the rendered PDF (fonts, colours, anti-aliasing) | Visual regression | `PdfVisualRegression.standard()&hellip;assertMatchesBaseline(...)` &mdash; see [visual regression testing](./docs/operations/visual-regression-testing.md) |
| See the live gallery | Static showcase site | [Showcase](https://DemchaAV.github.io/GraphCompose/) &mdash; source under [`web/`](./web), deployed to GitHub Pages via the [Pages workflow](./.github/workflows/deploy-web.yml) |

> **Choosing a template surface** &mdash; layered (`cv.v2`), classic (`cv.presets`), or the built-in `*TemplateV2` family? See **[Which template system should I use?](./docs/templates/which-template-system.md)** for the status matrix, decision tree, and `classic → layered` migration map.

## v1.8 primitives in 30 lines

Three snippets from the new vector surfaces. Full runnable versions live in the [examples gallery](./examples/README.md).

**Native chart** &mdash; categories + series in, native vector bars out (no rasterization).

```java
ChartData revenue = ChartData.builder()
    .categories("Q1", "Q2", "Q3", "Q4")
    .series("2024", 12.4, 15.1, 9.8, 14.2)
    .series("2025", 14.0, 18.2, 11.3, 16.9)
    .build();
section.chart(ChartSpec.bar().data(revenue)
    .legend(LegendPosition.BOTTOM)
    .size(ChartSize.aspectRatio(16, 7))
    .build());
```

**Overshoot-free line** &mdash; a smooth curve constrained to never overshoot the data range.

```java
section.chart(ChartSpec.line().data(series)
    .interpolation(LineInterpolation.MONOTONE)
    .build());
```

**SVG import + alignment** &mdash; parse SVG to native geometry, seat any fixed node across the width.

```java
SvgIcon globe = SvgIcon.parse(svgMarkup);
flow.addSvgIcon(globe, 48, HorizontalAlign.CENTER);
flow.addAligned(HorizontalAlign.RIGHT, anyFixedNode);
```

## Architecture

GraphCompose splits into a **public canonical surface** you author against (`com.demcha.compose.document.*`) and an **internal shared engine foundation** (`com.demcha.compose.engine.*`, marked `@Internal`) that resolves geometry, pagination, and rendering behind it. You author intent; the engine resolves the rest.

```mermaid
flowchart LR
    A["GraphCompose.document(...)<br/>DocumentSession · DocumentDsl"] --> B["DocumentNode tree<br/>document.node"]
    B --> C["LayoutCompiler<br/>document.layout"]
    C --> D["Engine foundation @Internal<br/>measure → paginate → place"]
    D --> E{Backend}
    E -->|PDF| F["PdfFixedLayoutBackend"]
    E -->|DOCX| G["DocxSemanticBackend · POI"]
    D -.->|layoutSnapshot| H["Deterministic snapshot<br/>(regression tests)"]
```

Full detail: [architecture overview](./docs/architecture/overview.md) &middot; [package map](./docs/architecture/package-map.md) &middot; [lifecycle](./docs/architecture/lifecycle.md).

## Documentation

📚 **[Full docs index](./docs/README.md)** &mdash; categorised map of every doc, ADR, and recipe. Start there to navigate the documentation.

### Templates
- [**Templates — layered architecture**](./docs/templates/v2-layered/README.md) &mdash; the template surface: CV, cover-letter, invoice, and proposal preset families on `BrandTheme`. Personas: [quickstart](./docs/templates/v2-layered/quickstart.md) · [using templates](./docs/templates/v2-layered/using-templates.md) · [authoring presets](./docs/templates/v2-layered/authoring-presets.md) · [contributing a new family](./docs/templates/v2-layered/contributor-guide.md).
- [Which template system?](./docs/templates/which-template-system.md) &mdash; the template naming history and the migration map for callers arriving from a pre-2.0 surface (classic presets, built-in `*Template` classes, the legacy PDF API). The retired classic docs are archived at [v1-classic](./docs/templates/v1-classic/README.md).

### Architecture & operations
- [Architecture overview](./docs/architecture/overview.md) · [Lifecycle](./docs/architecture/lifecycle.md) · [Production rendering](./docs/operations/production-rendering.md) · [Layout snapshot testing](./docs/operations/layout-snapshot-testing.md) · [Troubleshooting](./docs/troubleshooting.md)

### Recipes & examples
- [Recipes index](./docs/recipes.md) &mdash; [shape-as-container](./docs/recipes/shape-as-container.md) · [shapes](./docs/recipes/shapes.md) · [transforms](./docs/recipes/transforms.md) · [page-backgrounds](./docs/recipes/page-backgrounds.md) · [layered-page-design](./docs/recipes/layered-page-design.md) · [absolute-placement](./docs/recipes/absolute-placement.md) · [tables](./docs/recipes/tables.md) · [themes](./docs/recipes/themes.md) · [streaming](./docs/recipes/streaming.md) · [extending](./docs/recipes/extending.md) · [font-coverage](./docs/font-coverage.md)
- [Examples gallery](./examples/README.md) &mdash; every runnable example with PDF preview

### Contributing & releases
- [Contributing](./CONTRIBUTING.md) · [Code of conduct](./CODE_OF_CONDUCT.md) · [Security policy](./SECURITY.md) · [Release process](./docs/contributing/release-process.md)
- [API stability policy](./docs/api-stability.md) · [Which template system?](./docs/templates/which-template-system.md) · [Migration v1.6 → v1.7](./docs/roadmaps/migration-v1-6-to-v1-7.md) · [Migration v1.5 → v1.6](./docs/roadmaps/migration-v1-5-to-v1-6.md)

## Companion projects

- [**graphcompose-ai-flow**](https://github.com/DemchaAV/graphcompose-ai-flow) &mdash; experimental sister project exploring an AI-assisted authoring flow on top of GraphCompose. Independent codebase, separate lifecycle &mdash; nothing in this repo depends on it. Track it if you are interested in agentic document composition driven by the same semantic node model.

## License

MIT &mdash; see [`LICENSE`](./LICENSE).

## Star History

<a href="https://www.star-history.com/?repos=DemchaAV%2FGraphCompose&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=DemchaAV/GraphCompose&type=date&theme=dark&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=DemchaAV/GraphCompose&type=date&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=DemchaAV/GraphCompose&type=date&legend=top-left" />
 </picture>
</a>
