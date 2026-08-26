# GraphCompose Render — PDF

`io.github.demchaav:graph-compose-render-pdf`

The PDFBox-backed PDF render backend for GraphCompose. It carries the whole
`com.demcha.compose.document.backend.fixed.pdf` implementation plus the
`engine.render.pdf` render tree, and brings PDFBox and ZXing (barcodes) transitively.

## When to depend on it

- You depend on **`graph-compose-core`** directly and want it to render PDF.
- You are **not** already using **`graph-compose`** (the wrapper) or **`graph-compose-bundle`** —
  both of those include this backend transitively, so add it only for a lean, explicit setup.

A bare `graph-compose-core` renders nothing until this artifact is on the classpath;
opening a session (`create()`) throws `MissingBackendException`, whose message names this
coordinate.

## Smallest complete example

You don't call the backend directly. It registers a `FixedLayoutBackendProvider` and a
`FontMetricsProvider` via `META-INF/services`, so the core discovers it as the session opens
and the normal path just works:

<!-- doc-example: id=readme-render-pdf-hello mode=method imports=com.demcha.compose.GraphCompose,com.demcha.compose.document.api.DocumentSession,java.nio.file.Path -->
```java
Path out = Path.of("hello.pdf");
try (DocumentSession doc = GraphCompose.document(out).create()) {
    doc.pageFlow().addParagraph("Hello, PDF").build();
    doc.buildPdf();                 // -> hello.pdf
}
```

`toPdfBytes()` and `toImages(int dpi)` are the in-memory counterparts.

`PdfFixedLayoutBackend` is available if you need the backend explicitly (custom fragment
handlers, options).

## Install

Same version as the rest of the GraphCompose train (lockstep):

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose-render-pdf</artifactId>
    <version>2.2.2</version>
</dependency>
```

```kotlin
dependencies { implementation("io.github.demchaav:graph-compose-render-pdf:2.2.2") }
```

The full "which artifact?" table: [root README → Installation](../README.md#installation).
Upgrading from 1.x: [modules migration guide](../docs/migration/v2.0.0-modules.md).
