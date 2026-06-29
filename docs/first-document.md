# Your First Document

A five-minute path from an empty project to a real PDF. GraphCompose is
session-first: you open a `DocumentSession`, describe content in reading order
with a page flow, and render. No coordinates, no manual page breaks.

> **Prerequisites:** Java 17+ and the `io.github.demchaav:graph-compose`
> dependency — see the [README install snippet](../README.md#installation).

## The smallest document

Open a session for a file path, add one page flow, render. The engine handles
placement and pagination.

<!-- doc-example: id=first-document-smallest mode=method -->
```java
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;

import java.nio.file.Path;

try (DocumentSession document = GraphCompose.document(Path.of("hello.pdf"))
        .pageSize(DocumentPageSize.A4)
        .margin(24, 24, 24, 24)
        .create()) {

    document.pageFlow(page -> page
            .module("Summary", module -> module.paragraph("Hello GraphCompose")));

    document.buildPdf();
}
```

`GraphCompose.document(path)` configures the output; `create()` returns the
`DocumentSession`. Use try-with-resources so the session is always released, even
if rendering fails. Inside the session, `pageFlow(...)` is the document body:
modules, sections, paragraphs, lists, tables, and rows are added top to bottom.

## A real custom document

The same Flow model scales to a multi-section document. There are still no
coordinates and no manual page breaks — just structure in reading order.

<!-- doc-example: id=first-document-custom mode=method -->
```java
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;

import java.nio.file.Path;

try (DocumentSession document = GraphCompose.document(Path.of("profile.pdf"))
        .pageSize(DocumentPageSize.A4)
        .margin(24, 24, 24, 24)
        .create()) {

    document.pageFlow()
            .name("CandidateProfile")
            .spacing(12)
            .module("Professional Summary", module -> module.paragraph(
                    "Backend engineer focused on clean Java APIs, stable document "
                            + "output, and reusable template architecture."))
            .module("Technical Skills", module -> module.bullets(
                    "Java 21 and Spring Boot",
                    "PDF document generation with GraphCompose",
                    "Layout snapshot testing and render regression checks"))
            .module("Projects", module -> module.rows(
                    "GraphCompose - declarative document layout engine.",
                    "CVRewriter - profile-aware CV tailoring platform."))
            .build();

    document.buildPdf();
}
```

The callback form (`pageFlow(page -> ...)`) builds and attaches the root for you.
The builder form (`pageFlow().…build()`) gives you the fluent chain but you must
call `.build()` yourself.

## Already a known document? Use a template

If your document is a known family — invoice, proposal, CV, cover letter — do not
hand-build it. A maintained template maps a typed data object into the same
session, then you render as usual:

```java
import com.demcha.compose.document.templates.builtins.InvoiceTemplateV2;
import com.demcha.compose.document.theme.BusinessTheme;

InvoiceTemplateV2 template = new InvoiceTemplateV2(BusinessTheme.modern());

try (DocumentSession document = GraphCompose.document(Path.of("invoice.pdf")).create()) {
    template.compose(document, invoice);   // invoice = your InvoiceDocumentSpec
    document.buildPdf();
}
```

Templates and hand-written Flow compose into the *same* `DocumentSession`, so you
can mix them. To choose a template surface, see
[Which template system should I use?](templates/which-template-system.md).

## Rendering on a server

When the caller already owns the output stream — an HTTP response, a cloud
upload — create the session *without* a default path and stream the PDF with
`writePdf(OutputStream)` instead of `buildPdf()`. GraphCompose writes the
stream but does not close it. For the full server snippet, see
[Getting started — Streaming output](getting-started.md#streaming-output).

Create one `DocumentSession` per render request; it is mutable and not
thread-safe. Use `toPdfBytes()` only when the caller truly needs a byte array.

## Where to go next

- [Getting Started](getting-started.md) — themes, hero blocks, layer stacks,
  shape-as-container, and built-in templates.
- [Recipes](recipes.md) — themes, shapes, transforms, tables, and layout
  snapshots.
- [Which template system should I use?](templates/which-template-system.md) —
  the decision tree for CV / invoice / proposal surfaces.
- [Production Rendering](operations/production-rendering.md) — server-side
  lifecycle, streaming, and load guidance.
