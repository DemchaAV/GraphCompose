# Migration: GraphCompose v1.x → v2.0

This guide explains how to move from the v1.x template-first API to the v2.0 canonical session-first API. v2.0 is a deliberate breaking release; the upgrade is straightforward but not source-compatible.

## At a glance

| Concern | v1.x | v2.0 |
| --- | --- | --- |
| Maven coordinates | `com.demcha:GraphCompose:v1.1.0` | `io.github.demchaav:graphcompose:2.0.0` |
| JitPack coordinates | `com.github.DemchaAV:GraphCompose:v1.1.0` | `com.github.DemchaAV:GraphCompose:v2.0.0` |
| Authoring entry point | static `compose(DocumentSession, ...)` template helpers | `GraphCompose.document(path).create()` returning a `DocumentSession` |
| Lifecycle | session-per-render, ad-hoc close paths | `try-with-resources` `DocumentSession` with idempotent `close()` |
| Empty document | wrote a zero-page PDF silently | throws `IllegalStateException` with a domain-specific message |
| Closed session | undefined behaviour, occasional NPEs | every authoring/rendering call fails fast with `IllegalStateException` |

## Step 1 — Update the dependency

### Maven

```xml
<dependency>
    <groupId>com.github.DemchaAV</groupId>
    <artifactId>GraphCompose</artifactId>
    <version>v2.0.0</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.github.DemchaAV:GraphCompose:v2.0.0")
}
```

JitPack continues to resolve releases by git tag, so the `v` prefix stays in the version coordinate. Maven Central / direct POM lookup uses `2.0.0` without the prefix.

## Step 2 — Move from template helpers to `DocumentSession`

### Before (v1.x)

```java
GraphCompose.compose(session, template -> template
        .module("Summary", module -> module.paragraph("Hello GraphCompose")));

session.buildPdf();
```

### After (v2.0)

```java
try (DocumentSession document = GraphCompose.document(Path.of("output.pdf"))
        .pageSize(DocumentPageSize.A4)
        .margin(24, 24, 24, 24)
        .create()) {

    document.pageFlow(page -> page
            .module("Summary", module -> module.paragraph("Hello GraphCompose")));

    document.buildPdf();
}
```

Key differences:

- the session is now owned by your code and must be closed (typically via try-with-resources)
- authoring happens through `pageFlow(...)`, `compose(...)`, or `dsl()` directly on the session
- `buildPdf()` requires a default output file to be configured on the builder; otherwise call `writePdf(stream)` or `toPdfBytes()`

## Step 3 — Adopt the lifecycle contract

`DocumentSession` is now `AutoCloseable` with explicit lifecycle rules. After the session is closed, every public authoring or rendering method throws `IllegalStateException("DocumentSession is already closed.")`. Calling `close()` twice is safe.

```java
DocumentSession document = GraphCompose.document().pageSize(DocumentPageSize.A4).create();
try {
    document.pageFlow(page -> page.module("Summary", m -> m.paragraph("...")));
    byte[] pdf = document.toPdfBytes();
} finally {
    document.close(); // idempotent
}

// document.toPdfBytes(); // throws IllegalStateException
```

If you previously cached a `DocumentSession` reference and reused it across requests, that pattern is now an explicit error: create one session per render.

## Step 4 — Handle empty documents explicitly

v1.x silently produced empty PDFs when no roots were attached. v2.0 treats this as a bug and throws:

```
IllegalStateException: Cannot render an empty document. Add at least one root before calling writePdf/toPdfBytes/buildPdf.
```

Action: ensure the session has at least one root (`add`, `pageFlow`, `compose`, or `dsl()`) before rendering. If you intentionally want a blank page, add an empty `pageFlow`:

```java
document.pageFlow(page -> { /* intentionally empty */ });
document.buildPdf();
```

## Step 5 — Audit your imports

Some v1.x example code reached into `com.demcha.compose.engine.*` for spacing primitives. Public DSL code should now prefer:

- `com.demcha.compose.document.style.DocumentInsets` instead of `com.demcha.compose.engine.components.style.Padding`
- `com.demcha.compose.document.style.DocumentColor` / `DocumentStroke` / `DocumentTextStyle`
- `com.demcha.compose.document.table.DocumentTable*`

The legacy engine types still compile in v2.0 for backwards compatibility, but they are scheduled to leave the public DSL surface during the v2.x line; new code should target the document.* style classes.

## Step 6 — Run the test suite

After switching, run:

```
./mvnw -pl . -DskipITs=false clean verify
```

The new lifecycle and empty-document tests will catch any places where your code relied on the old silent behaviours.

## What stays the same

- the layout engine, pagination policy, and layout snapshot APIs are unchanged
- node types (`ParagraphNode`, `TableNode`, `ImageNode`, etc.) keep their structural identity
- PDF rendering output is byte-for-byte equivalent for the same input graph
- `DocumentSession.layoutGraph()` and `layoutSnapshot()` continue to work for regression tests

## Where to go next

- [Getting Started](./getting-started.md)
- [Recipes](./recipes.md)
- [Lifecycle](./lifecycle.md)
- [v2 Roadmap](./v2-roadmap.md) for the remaining stabilisation tracks
