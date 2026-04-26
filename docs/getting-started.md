# Getting Started

GraphCompose v1.2 uses the canonical session-first API. Application code starts with `GraphCompose.document(...)`, creates one `DocumentSession`, describes content with `DocumentDsl`, and finishes with `writePdf(...)`, `buildPdf()`, or `toPdfBytes()`.

## Minimal File Output

```java
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;

import java.nio.file.Path;

try (DocumentSession document = GraphCompose.document(Path.of("output.pdf")).create()) {
    document.pageFlow(page -> page
            .module("Summary", module -> module.paragraph("Hello GraphCompose")));

    document.buildPdf();
}
```

## Streaming Output

Use `writePdf(OutputStream)` for web APIs, cloud storage uploads, and other
server paths where the caller already owns an output stream. GraphCompose writes
the PDF but does not close the stream.

```java
void writeResponse(OutputStream responseOutputStream) throws Exception {
    try (DocumentSession document = GraphCompose.document().create()) {
        document.pageFlow(page -> page
                .module("Summary", module -> module.paragraph("Generated for an HTTP response.")));

        document.writePdf(responseOutputStream);
    }
}
```

## In-Memory Output

```java
byte[] pdfBytes;

try (DocumentSession document = GraphCompose.document().create()) {
    document.pageFlow(page -> page
            .module("Summary", module -> module.paragraph("Generated for an HTTP response.")));

    pdfBytes = document.toPdfBytes();
}
```

`toPdfBytes()` is a convenience wrapper around the streaming path. Prefer
`writePdf(...)` when the next step is already a stream.

## Debug Guide Lines

Guide lines are a render-only diagnostic overlay for checking page margins,
padding, and resolved boxes. They do not change layout geometry or layout
snapshots.

```java
try (DocumentSession document = GraphCompose.document(Path.of("debug.pdf"))
        .guideLines(true)
        .create()) {
    document.pageFlow(page -> page
            .module("Summary", module -> module.paragraph("Guide-line preview")));

    document.buildPdf();
}
```

You can also toggle the same option on an open session before convenience PDF
output:

```java
document.guideLines(true);
byte[] debugPdf = document.toPdfBytes();
```

## Module-First Authoring

Use modules when you are building a normal document section. A module is a titled full-width block with a body made from semantic content.

```java
document.pageFlow(page -> page
        .module("Professional Summary", module -> module.paragraph(summary))
        .module("Technical Skills", module -> module.bullets(skills))
        .module("Projects", module -> module.rows(projectRows)));
```

The common body calls are `paragraph`, `bullets`, `dashList`, `rows`, `table`, `image`, `divider`, and `pageBreak`.

## Built-In Templates

Built-ins compose into the same `DocumentSession`. Template data lives under `com.demcha.compose.document.templates.data.*`, and templates live under `com.demcha.compose.document.templates.builtins`.

```java
InvoiceTemplate template = new InvoiceTemplateV1();

try (DocumentSession document = GraphCompose.document(Path.of("invoice.pdf")).create()) {
    template.compose(document, invoice);
    document.buildPdf();
}
```

## Where To Go Next

- Use [Recipes](./recipes.md) for copy-paste examples.
- Use [Lifecycle](./lifecycle.md) to understand the session, layout, and render flow.
- Use [Production Rendering](./production-rendering.md) for server-side lifecycle, privacy, and load guidance.
- Use [Package Map](./package-map.md) before adding new public APIs or engine internals.
