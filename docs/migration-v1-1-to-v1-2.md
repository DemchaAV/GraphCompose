# Migration: GraphCompose v1.1 to v1.2

This guide explains how to move from earlier v1.1 usage patterns to the v1.2 canonical session-first API. v1.2 is intentionally not framed as a major release, but it does stabilize the preferred public path around `GraphCompose.document(...)`, `DocumentSession`, and `DocumentDsl`.

## At a glance

| Concern | v1.1 style | v1.2 canonical style |
| --- | --- | --- |
| Maven coordinates | `com.github.demchaav:GraphCompose:v1.1.0` | `com.github.demchaav:GraphCompose:v1.2.0` |
| Authoring entry point | template helpers or lower-level composer paths | `GraphCompose.document(path).create()` returning a `DocumentSession` |
| Lifecycle | ad-hoc close paths in examples | `try-with-resources` `DocumentSession` with idempotent `close()` |
| Empty document | easy to miss until render time | throws `IllegalStateException` with a domain-specific message |
| Closed session | undefined behavior in old paths | every authoring/rendering call fails fast with `IllegalStateException` |

## Step 1 - Update the dependency

### Maven

```xml
<dependency>
    <groupId>com.github.DemchaAV</groupId>
    <artifactId>GraphCompose</artifactId>
    <version>v1.2.0</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.github.demchaav:GraphCompose:v1.2.0")
}
```

JitPack resolves releases by git tag, so the `v` prefix stays in the dependency version. The project POM version is `1.2.0` without the prefix.

## Step 2 - Prefer `DocumentSession`

### Before

```java
GraphCompose.compose(session, template -> template
        .module("Summary", module -> module.paragraph("Hello GraphCompose")));

session.buildPdf();
```

### After

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

- the session is owned by your code and should usually be closed with try-with-resources
- authoring happens through `pageFlow(...)`, `compose(...)`, or `dsl()` directly on the session
- `buildPdf()` requires a default output file; use `writePdf(stream)` or `toPdfBytes()` for server-side output

## Step 3 - Adopt the lifecycle contract

`DocumentSession` is `AutoCloseable` with explicit lifecycle rules. After the session is closed, every public authoring or rendering method throws `IllegalStateException("DocumentSession is already closed.")`. Calling `close()` twice is safe.

```java
DocumentSession document = GraphCompose.document().pageSize(DocumentPageSize.A4).create();
try {
    document.pageFlow(page -> page.module("Summary", m -> m.paragraph("...")));
    byte[] pdf = document.toPdfBytes();
} finally {
    document.close();
}
```

Create one session per render request. Avoid caching and reusing a closed `DocumentSession`.

## Step 4 - Handle empty documents explicitly

v1.2 treats empty renders as a caller error:

```text
IllegalStateException: Cannot render an empty document. Add at least one root before calling writePdf/toPdfBytes/buildPdf.
```

Add at least one root through `pageFlow`, `compose`, `dsl()`, or `add` before rendering.

## Step 5 - Audit public imports

Application code should prefer the public document authoring types:

- `com.demcha.compose.document.style.DocumentInsets`
- `com.demcha.compose.document.style.DocumentColor`
- `com.demcha.compose.document.style.DocumentStroke`
- `com.demcha.compose.document.style.DocumentTextStyle`
- `com.demcha.compose.document.table.DocumentTable*`

Avoid importing `com.demcha.compose.engine.*` from application authoring code. Engine packages remain available for internal and advanced extension work, but they are not the recommended public DSL surface.

## Step 6 - Run verification

After switching, run:

```powershell
.\mvnw.cmd -B -ntp clean verify
.\mvnw.cmd -B -ntp -f examples\pom.xml clean package
```

## Where to go next

- [Getting Started](./getting-started.md)
- [Recipes](./recipes.md)
- [Lifecycle](./lifecycle.md)
- [v1.2 Roadmap](./v1.2-roadmap.md)
