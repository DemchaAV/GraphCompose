# Streaming and output

GraphCompose has three convenience output paths plus an explicit
backend-driven path. Pick the one that matches where the bytes
are going next.

## File output

The simplest case: render straight to a file path.

```java
try (DocumentSession document = GraphCompose.document(Path.of("output.pdf")).create()) {
    document.pageFlow(page -> page.module("Summary",
            module -> module.paragraph("Hello GraphCompose")));
    document.buildPdf();
}
```

`buildPdf()` writes to the path supplied to
`GraphCompose.document(Path)`. If you constructed the session with
`GraphCompose.document()` (no path), call
`buildPdf(Path target)` instead.

## Stream output for HTTP responses

Use `writePdf(OutputStream)` for web APIs, cloud storage uploads, and
other server paths where the caller already owns an output stream.
GraphCompose writes the PDF but **does not close the stream** — the
caller stays in control of lifecycle and backpressure.

```java
import jakarta.servlet.http.HttpServletResponse;

void exportInvoice(HttpServletResponse response, InvoiceDocumentSpec invoice) throws Exception {
    response.setContentType("application/pdf");
    response.setHeader("Content-Disposition", "attachment; filename=invoice.pdf");

    try (DocumentSession document = GraphCompose.document().create()) {
        new InvoiceTemplateV1().compose(document, invoice);
        document.writePdf(response.getOutputStream());
    }
}
```

The Servlet response stream stays open after `writePdf` returns; the
container closes it as part of its own lifecycle.

### Why prefer `writePdf` over `toPdfBytes`

- **Memory:** `writePdf` streams page bytes into the supplied stream
  without buffering the full PDF in heap.
- **Time-to-first-byte:** the HTTP client starts receiving bytes as
  the document writes; `toPdfBytes` makes them wait for the whole
  document.
- **Backpressure:** the stream lifecycle stays with the container,
  not GraphCompose.

## In-memory bytes

When the next step is "send these bytes to S3 / a queue / a unit
test" and you don't already have a stream, `toPdfBytes()` is the
convenience wrapper.

```java
byte[] pdfBytes;
try (DocumentSession document = GraphCompose.document().create()) {
    document.pageFlow(page -> page.module("Summary",
            module -> module.paragraph("In-memory render")));
    pdfBytes = document.toPdfBytes();
}
s3.putObject(bucket, key, RequestBody.fromBytes(pdfBytes));
```

`toPdfBytes()` is implemented as a streaming write into a
`ByteArrayOutputStream`. For large documents prefer `writePdf(...)`
with an explicit stream — the in-memory path holds the entire PDF
before returning.

## DOCX semantic export

`DocxSemanticBackend` produces an editable Word document. Apache POI
must be on the consumer classpath (it's an optional dependency in the
GraphCompose POM).

```java
try (DocumentSession document = GraphCompose.document().create()) {
    document.pageFlow(page -> page
            .module("Summary", module -> module.paragraph("Editable in Word")));

    byte[] docxBytes = document.export(new DocxSemanticBackend());
    Files.write(Path.of("output.docx"), docxBytes);
}
```

The DOCX path is semantic — it preserves paragraphs, tables, images,
and module structure but ignores fixed-layout concerns (page
backgrounds, exact placement, graphics-state clip paths). See the
[parity matrix](../canonical-legacy-parity.md) for the per-feature
mapping.

## Header / footer chrome

`PdfHeaderFooterOptions` (configured on the document builder) adds
a header zone and / or a footer zone that renders on every page
without affecting layout snapshots. The chrome ignores the layout
graph entirely — it's painted by the PDF backend after the body
fragments.

```java
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterZone;

try (DocumentSession document = GraphCompose.document(Path.of("report.pdf"))
        .header(PdfHeaderFooterOptions.builder()
                .zone(PdfHeaderFooterZone.LEFT, "Quarterly Report")
                .zone(PdfHeaderFooterZone.RIGHT, "Page {page} / {totalPages}")
                .build())
        .create()) {
    // ... pageFlow content ...
    document.buildPdf();
}
```

## Guide-line debug overlay

`guideLines(true)` paints page margins, padding, and resolved boxes
as thin coloured strokes. Use it during template development to see
where the layout layer thinks every box is. The flag is render-only
and does not affect layout snapshots.

```java
try (DocumentSession document = GraphCompose.document(Path.of("debug.pdf"))
        .guideLines(true)
        .create()) {
    document.pageFlow(page -> page.module("Summary",
            module -> module.paragraph("Guide-line preview")));
    document.buildPdf();
}
```

## See also

- [`docs/getting-started.md`](../getting-started.md) — covers the
  three convenience output flows in the quick-start section.
- [`docs/production-rendering.md`](../production-rendering.md) —
  server-side lifecycle, privacy, and load guidance for production
  HTTP / queue paths.
- [`docs/canonical-legacy-parity.md`](../canonical-legacy-parity.md) —
  PDF Output section lists every chrome / metadata / protection /
  watermark / header / footer option.
