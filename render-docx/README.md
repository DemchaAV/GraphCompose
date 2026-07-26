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
    <version>2.0.0</version>
    <scope>runtime</scope>
</dependency>
```

## Usage

```java
try (var doc = GraphCompose.document().create()) {
    doc.pageFlow().addParagraph("Hello, DOCX").build();
    doc.export(new DocxSemanticBackend(), docxFile);
}
```

## What it maps, and what it does not

DOCX walks the **semantic node graph**, not the fixed PDF layout — Word owns the flow, so
the page geometry the PDF backend resolves is deliberately ignored. Shapes, gradients,
clips, transforms and alpha have no DOCX counterpart by design; a document that draws with
them exports its text and tables, not its drawing.

What maps: paragraphs (runs, alignment), block images (`STRETCH` / `CONTAIN` / `COVER`),
table rows including row and column spans, and document metadata. Run styling carries font
family, size, colour, bold, italic and underline.

Beyond geometry, these are **not implemented** even though Word itself can express them —
check the list before you promise a `.docx` to a reader:

- **Hyperlinks are dropped**, external and internal alike; the link text survives as plain text.
- **No bookmarks and no navigation outline.**
- **No repeating headers or footers**, and no watermark layer.
- **`STRIKETHROUGH` is dropped** — the other decorations map.
- **No barcodes or QR codes**, and no inline images or code/badge chips inside a paragraph.
- **Per-run styling in a mixed-style paragraph is flattened.** Every run in a `RichText`
  paragraph is written with the paragraph's style, so a bold or accent-coloured segment
  loses its own styling; the text itself is kept.
- **Multi-section documents export through the fixed-layout backends only** —
  `renderSections` is not part of the semantic path.
- **Output is not byte-deterministic**: rendering twice does not produce identical files.

Per-capability detail, with the implementing class for every supported cell:
[backend capability matrix](../docs/architecture/backend-capability-matrix.md).

## Install

Same version as the rest of the GraphCompose train (lockstep):

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose-render-docx</artifactId>
    <version>2.0.0</version>
</dependency>
```

```kotlin
dependencies { implementation("io.github.demchaav:graph-compose-render-docx:2.0.0") }
```

The full "which artifact?" table: [root README → Installation](../README.md#installation).
Upgrading from 1.x: [modules migration guide](../docs/migration/v2.0.0-modules.md).
