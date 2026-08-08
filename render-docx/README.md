# GraphCompose Render — DOCX

`io.github.demchaav:graph-compose-render-docx`

The semantic DOCX export backend for GraphCompose, backed by Apache POI. It carries
`DocxSemanticBackend` and brings POI transitively, so a PDF-only consumer never pays for it.

## When to depend on it

Add it (at compile scope) only when you export `.docx`. It is **not** included by
`graph-compose`, `graph-compose-core`, or `graph-compose-bundle` — DOCX is opt-in.

**It is not sufficient on its own.** Opening a `DocumentSession` resolves a
`FontMetricsProvider` so text can be measured, and `graph-compose-render-pdf` is the
only artifact that publishes one. A classpath of `graph-compose-core` +
`graph-compose-render-docx` fails at `create()` with `MissingBackendException` before
any export happens. Add the PDF backend alongside it — or depend on `graph-compose`,
which is core + render-pdf already:

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose-render-pdf</artifactId>
    <version>2.1.1</version>
    <scope>runtime</scope>
</dependency>
```

## Usage

<!-- doc-example: id=readme-render-docx-export mode=method imports=com.demcha.compose.GraphCompose,com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend,java.nio.file.Path -->
```java
Path docxFile = Path.of("hello.docx");
try (var doc = GraphCompose.document().create()) {
    doc.pageFlow().addParagraph("Hello, DOCX").build();
    doc.export(new DocxSemanticBackend(), docxFile);
}
```

## What it maps, and what it does not

DOCX walks the **semantic node graph**, not the fixed PDF layout — Word owns the flow, so
the page geometry the PDF backend resolves is deliberately ignored. Drawing nodes — shape,
line, ellipse, polygon, path, barcode — do reach the backend, and are dropped there with one
logged warning per kind; a clip or transform container renders its children inline, without
the boundary and without the transform. A document that draws exports its text and tables,
not its drawing.

What maps: paragraphs, lists, block images, tables, and document metadata (title, author,
subject, keywords). Run styling carries font family, size, colour, bold, italic,
underline and strikethrough, per run rather than per paragraph.

What maps only in part:

- **Table cells keep their text, not their structure.** `colSpan` and `rowSpan` are not
  applied, so a table with merged cells exports with its columns misaligned. Per-cell
  style and fill/border paint are dropped, and a `table` cell's text carries no styling
  at all — it is written from the cell's lines rather than from runs. (A `row` cell is
  a paragraph and does keep per-run styling.)
- **Image fit is ignored.** The picture is embedded at the node's width and height;
  `CONTAIN` and `COVER` therefore behave as `STRETCH`, and an image sized only by `scale`
  falls back to 100 × 100 pt.

These are **not implemented** even though Word itself can express them — check the list
before you promise a `.docx` to a reader:

- **Hyperlinks are dropped**, external and internal alike; the link text survives as plain text.
- **No bookmarks and no navigation outline.**
- **No repeating headers or footers**, and no watermark layer.
- **No barcodes or QR codes**, and no inline images or code/badge chips inside a paragraph
  — a chip's text is exported with its own styling, but not its background.
- **Output is not byte-deterministic**: rendering twice does not produce identical files.

Multi-section documents are a separate case: `renderSections` is declared on the
fixed-layout SPI, and `SemanticBackend` carries only `name()` and `export(...)`, so a
multi-section export runs through the PDF or PPTX backend rather than this one.

Per-capability detail, with the implementing class for every supported cell:
[backend capability matrix](../docs/architecture/backend-capability-matrix.md).

## Install

Same version as the rest of the GraphCompose train (lockstep):

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose-render-docx</artifactId>
    <version>2.1.1</version>
</dependency>
```

```kotlin
dependencies { implementation("io.github.demchaav:graph-compose-render-docx:2.1.1") }
```

The full "which artifact?" table: [root README → Installation](../README.md#installation).
Upgrading from 1.x: [modules migration guide](../docs/migration/v2.0.0-modules.md).
