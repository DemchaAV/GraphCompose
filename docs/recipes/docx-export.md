# DOCX export: the semantic backend

PDF is GraphCompose's fixed-layout output — every fragment lands at exact
coordinates. DOCX is different on purpose: it is a **semantic export** that
walks the document graph and writes editable Word content, skipping the
layout pass entirely (no per-page pagination, no PDF chrome). Use it when
the recipient needs to *edit* the document; use PDF when pixels must match.

## Exporting a session

```java
import com.demcha.compose.document.backend.semantic.DocxSemanticBackend;

try (DocumentSession document = GraphCompose.document()
        .pageSize(595, 842)
        .margin(DocumentInsets.of(36))
        .create()) {
    document.pageFlow().name("Flow")
            .addParagraph(p -> p.text("Hello Word"))
            .addTable(t -> t
                    .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                    .row("R1C1", "R1C2"))
            .build();

    byte[] docx = document.export(new DocxSemanticBackend());
    // or write straight to disk:
    document.export(new DocxSemanticBackend(), Path.of("out/report.docx"));
}
```

`export(backend)` returns the DOCX bytes and also writes the session's
default output file when one was given to `GraphCompose.document(path)`;
the two-argument overload targets an explicit path.

**Dependency note:** the backend requires `org.apache.poi:poi-ooxml` on
the classpath. GraphCompose declares it **optional** in its POM, so
consumers who export DOCX must add it explicitly — consumers who only
render PDF carry no POI footprint.

## What maps 1:1

| Document node | DOCX output |
|---|---|
| Paragraphs | Word paragraphs with alignment, font, size, colour, bold/italic/underline; inline runs preserved |
| Tables | Word tables, one cell per cell |
| Images | Embedded pictures at the node's declared size |
| Rows | A one-row table, so editors keep the side-by-side layout (cell content limited to atomic children) |
| Sections / containers | Children written in order |
| Spacers | Empty paragraphs carrying the vertical gap as spacing-after |
| Page breaks | Explicit Word page breaks |

Page geometry (size and margins) and session metadata (title, author,
subject, keywords) carry into the Word document as well.

## What falls back

- **Charts → data table.** The semantic export has no layout pass, so a
  chart's compiled vector geometry does not exist here. Its *semantic*
  content is its data, so the backend writes a categories-by-series table
  (values formatted with the chart's own axis format) and logs **one
  capability warning per export**. See [charts.md](charts.md).
- **Shape containers → inline layers.** DOCX has no portable equivalent
  of a graphics-state path clip, so the container's layers are written
  inline, in source order, without the outline frame and without clipping
  — again with one warning per export.

## What is skipped

Lines, ellipses, standalone shapes, and barcodes are **silently skipped**
— they are pure fixed-layout geometry with no semantic equivalent.
Headers/footers, watermarks, and protection options are also ignored by
the current exporter.

The rule of thumb: if the document leans on geometry — shapes, layered
designs, precise placement — export PDF for the reader and DOCX only as
an editable companion.

Round-trip coverage (paragraphs, tables, metadata, chart fallback) lives in
[`DocxSemanticBackendTest`](../../src/test/java/com/demcha/compose/document/backend/semantic/DocxSemanticBackendTest.java).
