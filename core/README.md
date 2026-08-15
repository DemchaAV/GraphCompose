# GraphCompose Core

`io.github.demchaav:graph-compose-core`

The lean document engine: the canonical authoring surface (`com.demcha.compose.document.*` —
DSL, semantic nodes, themes, layout snapshots) and the deterministic two-pass layout behind
it, with **no PDFBox, POI, or template code** on its dependency tree.

## When to depend on it

- You want the smallest engine and will **bring your own render backend** — add
  `graph-compose-render-pdf`, or implement the `FixedLayoutBackendProvider` SPI. A custom
  backend needs **both** halves of the SPI: opening a session measures text through a
  `FontMetricsProvider`, so publish one alongside your backend. `graph-compose-render-pdf`
  is the only shipped artifact that registers one.
- You are building a library on top of GraphCompose and don't want to impose a backend
  on your consumers.

Most applications should depend on `graph-compose` (core + the PDF backend, the 1.x
drop-in) or `graph-compose-bundle` (batteries included) instead.

## Smallest complete example

Author against the canonical surface. Core cannot open a session on its own: `create()`
resolves a `FontMetricsProvider` through `ServiceLoader`, so a core-only classpath throws
`MissingBackendException` there — before any render call — and the message names the
artifact to add. With `graph-compose-render-pdf` present the whole path works:

<!-- doc-example: id=readme-core-hello mode=method imports=com.demcha.compose.GraphCompose,com.demcha.compose.document.api.DocumentSession,java.nio.file.Path -->
```java
Path out = Path.of("hello.pdf");
try (DocumentSession doc = GraphCompose.document(out).create()) {
    doc.pageFlow().addParagraph("Hello").build();
    doc.buildPdf();                 // writes hello.pdf
}
```

`toPdfBytes()` and `toImages(int dpi)` are the in-memory counterparts.

## Install

Same version as the rest of the GraphCompose train (lockstep):

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose-core</artifactId>
    <version>2.2.0</version>
</dependency>
```

```kotlin
dependencies { implementation("io.github.demchaav:graph-compose-core:2.2.0") }
```

The full "which artifact?" table: [root README → Installation](../README.md#installation).
Upgrading from 1.x: [modules migration guide](../docs/migration/v2.0.0-modules.md).
