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
underline and strikethrough, per run rather than per paragraph. A block image is sized by
the rule layout uses — `width` / `height` / `scale`, the aspect ratio filling in whichever
is missing, and a clamp to the page's content width — and honours its fit mode: `CONTAIN`
is embedded at its fitted size, `COVER` fills the box and the overflow is cropped out of
the picture source rather than clipped, which Word has no way to express for an inline
picture.

What maps only in part:

- **Table cells keep their structure and their paint.** `colSpan` and `rowSpan` map to
  Word's own `w:gridSpan` and `w:vMerge`; a cell's fill maps to `w:shd` and its stroke to
  `w:tcBorders`; and text, fill and stroke each take the most specific value in the
  table / column / row / cell cascade, resolved per field, so a table-wide rule survives a
  row that only overrides the fill. A stroke of no width is read as "no border" and says so
  in the file, so a deliberately borderless design does not inherit the grid Word puts on a
  table; a table that says nothing about borders keeps that grid, Word owning the look it
  was not given. What a fill loses is its opacity — `w:shd` is opaque,
  and blending it would need a background Word owns rather than this backend. A composed
  cell writes the shapes a cell can hold — paragraphs, and the wrappers around them — so
  one built from an image or a list still lands empty.

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
