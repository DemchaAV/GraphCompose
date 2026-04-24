# Getting Started

GraphCompose V2 is canonical-only. Application code starts with `GraphCompose.document(...)`, creates one `DocumentSession`, describes content with `DocumentDsl`, and finishes with `buildPdf()` or `toPdfBytes()`.

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

## In-Memory Output

```java
byte[] pdfBytes;

try (DocumentSession document = GraphCompose.document().create()) {
    document.pageFlow(page -> page
            .module("Summary", module -> module.paragraph("Generated for an HTTP response.")));

    pdfBytes = document.toPdfBytes();
}
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
- Use [Package Map](./package-map.md) before adding new public APIs or engine internals.
