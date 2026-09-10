# Your First Document

A five-minute path from an empty project to a real PDF. Add the dependency,
copy one complete file, and run it. You do not need to learn the layout engine
or choose a template first.

> **Prerequisites:** Java 17+ and the `io.github.demchaav:graph-compose`
> dependency — see the [README install snippet](../README.md#installation).

## The smallest document

Open a document for a file path, add content from top to bottom, and render.
The engine handles placement and pagination.

<!-- doc-example: id=first-document-smallest mode=method -->
```java
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;

import java.nio.file.Path;

try (DocumentSession document = GraphCompose.document(Path.of("hello.pdf"))
        .margin(48, 48, 48, 48)
        .create()) {

    document.pageFlow(page -> page
            .addParagraph("Hello GraphCompose")
            .addParagraph("This PDF was created without manual coordinates."));

    document.buildPdf();
}
```

Those are statements, not a file: they go inside a method — the
[README's Hello world](../README.md#hello-world) shows the same program as a complete
`Hello.java`. Every snippet on this page is written the same way, so the shape you are
reading is the GraphCompose part and nothing else.

There are only four ideas here:

1. `GraphCompose.document(path)` chooses the output file.
2. `create()` opens a `DocumentSession`.
3. `pageFlow(...)` receives content in top-to-bottom reading order.
4. `buildPdf()` writes the file.

Use try-with-resources so the session is released even if rendering fails. The
page flow can contain paragraphs directly; introduce modules, sections, tables,
and styling only when the document actually needs them.

## A real custom document

The same flow model scales to a multi-section document. There are still no
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
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.invoice.presets.ModernInvoice;

DocumentTemplate<InvoiceDocumentSpec> template = ModernInvoice.create();

try (DocumentSession document = GraphCompose.document(Path.of("invoice.pdf")).create()) {
    template.compose(document, invoice);   // invoice = your InvoiceDocumentSpec
    document.buildPdf();
}
```

Templates and hand-written flow compose into the *same* `DocumentSession`, so you
can mix them. Continue with [Using built-in templates](templates/v2-layered/using-templates.md).

## Rendering on a server

When the caller already owns the output stream — an HTTP response, a cloud
upload — create the session *without* a default path and stream the PDF with
`writePdf(OutputStream)` instead of `buildPdf()`. GraphCompose writes the
stream but does not close it. For the full server snippet, see
[Getting started — Streaming output](getting-started.md#streaming-output).

Create one `DocumentSession` per render request; it is mutable and not
thread-safe. Use `toPdfBytes()` only when the caller truly needs a byte array.

## Where to go next

Choose the one line that matches your next task:

| Next task | Continue with |
| --- | --- |
| Add content such as a table, timeline, chart, image, icon, emoji, or barcode | [Content and data recipes](recipes.md#content-and-data) |
| Build cards, columns, clipping, layers, backgrounds, or a canvas | [Layout and visual recipes](recipes.md#layout-and-visual-composition) |
| Add headers, footers, navigation, previews, or debug overlays | [Page behaviour and development](recipes.md#page-behaviour-output-and-development) |
| Render an invoice, proposal, CV, or cover letter | [Using built-in templates](templates/v2-layered/using-templates.md) |
| Protect this document from layout drift | [Testing your document](operations/test-your-document.md) |
| Stream it from a backend | [Production rendering](operations/production-rendering.md) |
| Learn rows, layers, backgrounds, and canvases | [Layered page design](recipes/layered-page-design.md) |
