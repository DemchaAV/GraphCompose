# GraphCompose Testing

`io.github.demchaav:graph-compose-testing`

Consumer testing support: deterministic **layout-snapshot assertions** and **PDF visual
regression** (pixel-diff of rendered pages). Add it at **test scope** to lock the output of
documents you generate.

## When to depend on it

Add it (test scope) when you want to catch layout/visual regressions in your own documents.
Not needed at runtime; not bundled by any published artifact.

**Not sufficient on its own.** Opening a `DocumentSession` resolves a
`FontMetricsProvider`, and `graph-compose-render-pdf` is the only artifact that publishes
one — so a classpath without it fails at `create()` with `MissingBackendException`, PDFBox
on the classpath notwithstanding. Add `graph-compose-render-pdf` at test scope too, or
depend on `graph-compose`.

## Smallest complete example

One session, both gates — the layout assertion needs no render, the visual one takes the
rendered bytes:

```java
try (DocumentSession doc = GraphCompose.document().create()) {
    doc.pageFlow().addParagraph("Hello").build();

    // Layout snapshot — asserts the renderer-neutral layout graph is unchanged.
    LayoutSnapshotAssertions.assertMatches(doc.layoutSnapshot(), "invoice-basic");

    // Visual regression — pixel-diffs the rendered pages against a committed baseline.
    PdfVisualRegression.standard().assertMatchesBaseline("invoice-basic", doc.toPdfBytes());
}
```

Update baselines deliberately with the documented system properties
(`LayoutSnapshotAssertions.UPDATE_PROPERTY` / `PdfVisualRegression.APPROVE_PROPERTY`).

## Install

Same version as the rest of the GraphCompose train (lockstep):

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose-testing</artifactId>
    <version>2.1.0</version>
    <scope>test</scope>
</dependency>
```

```kotlin
dependencies { testImplementation("io.github.demchaav:graph-compose-testing:2.1.0") }
```

The full "which artifact?" table: [root README → Installation](../README.md#installation).
Upgrading from 1.x: [modules migration guide](../docs/migration/v2.0.0-modules.md).
