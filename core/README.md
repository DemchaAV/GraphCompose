# GraphCompose Core

`io.github.demchaav:graph-compose-core`

The lean document engine: the canonical authoring surface (`com.demcha.compose.document.*` —
DSL, semantic nodes, themes, layout snapshots) and the deterministic two-pass layout behind
it, with **no PDFBox, POI, or template code** on its dependency tree.

## When to depend on it

- You want the smallest engine and will **bring your own render backend** — add
  `graph-compose-render-pdf`, or implement the `FixedLayoutBackendProvider` SPI.
- You are building a library on top of GraphCompose and don't want to impose a backend
  on your consumers.

Most applications should depend on `graph-compose` (core + the PDF backend, the 1.x
drop-in) or `graph-compose-bundle` (batteries included) instead.

## Usage

Author against the canonical surface; a render backend on the classpath is discovered
through `ServiceLoader` at render time:

```java
try (var doc = GraphCompose.document(out).create()) {
    doc.pageFlow().addParagraph("Hello").build();
} // with graph-compose-render-pdf present, buildPdf() / toPdfBytes() / toImages() work
```

A core-only classpath asked to render throws `MissingBackendException`, whose message
names the artifact to add.

## Install

Same version as the rest of the GraphCompose train (lockstep):

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose-core</artifactId>
    <version>2.0.0</version>
</dependency>
```

```kotlin
dependencies { implementation("io.github.demchaav:graph-compose-core:2.0.0") }
```

The full "which artifact?" table: [root README → Installation](../README.md#installation).
Upgrading from 1.x: [modules migration guide](../docs/migration/v2.0.0-modules.md).
