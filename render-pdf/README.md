# GraphCompose Render — PDF

`io.github.demchaav:graph-compose-render-pdf`

The PDFBox-backed PDF render backend for GraphCompose. It carries the whole
`com.demcha.compose.document.backend.fixed.pdf` implementation plus the
`engine.render.pdf` render tree, and brings PDFBox and ZXing (barcodes) transitively.

## When to depend on it

- You depend on **`graph-compose-core`** directly and want it to render PDF.
- You are **not** already using **`graph-compose`** (the wrapper) or **`graph-compose-bundle`** —
  both of those include this backend transitively, so add it only for a lean, explicit setup.

A bare `graph-compose-core` renders nothing until this artifact is on the classpath; asking
it to build a PDF throws `MissingBackendException`, whose message names this coordinate.

## Usage

You don't call the backend directly. It registers a `FixedLayoutBackendProvider` and a
`FontMetricsProvider` via `META-INF/services`, so the core discovers it at runtime and the
normal path just works:

```java
try (var doc = GraphCompose.document(out).create()) {
    doc.pageFlow().addParagraph("Hello, PDF").build();
} // buildPdf() / toPdfBytes() / toImages() now resolve the PDF backend
```

`PdfFixedLayoutBackend` is available if you need the backend explicitly (custom fragment
handlers, options).

## Install

Same version as the rest of the GraphCompose train (lockstep):

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose-render-pdf</artifactId>
    <version>2.1.0</version>
</dependency>
```

```kotlin
dependencies { implementation("io.github.demchaav:graph-compose-render-pdf:2.1.0") }
```

The full "which artifact?" table: [root README → Installation](../README.md#installation).
Upgrading from 1.x: [modules migration guide](../docs/migration/v2.0.0-modules.md).
