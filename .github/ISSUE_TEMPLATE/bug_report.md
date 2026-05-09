---
name: Bug report
about: Report a defect in GraphCompose
title: "[BUG] "
labels: bug
---

## What I expected

<!-- Describe the rendered output, layout behaviour, or API contract you expected. -->

## What actually happened

<!-- Stack trace, rendered output, layout snapshot diff, mismatched pixel count, etc. Attach the produced PDF if relevant. -->

## How to reproduce

```java
// Minimal, runnable Java code that triggers the issue.
// Prefer the canonical `GraphCompose.document(...)` API.
DocumentSession document = GraphCompose.document(Path.of("repro.pdf"))
        .pageSize(DocumentPageSize.A4)
        .create();
// ...
```

## Environment

- GraphCompose version: <!-- e.g. v1.6.1 -->
- Java: <!-- e.g. Temurin 17.0.10 -->
- OS: <!-- e.g. Windows 11 / macOS 14 / Ubuntu 24.04 -->
- PDFBox: <!-- 3.0.7 unless overridden -->

## Additional context

<!-- Optional. Related issues, prior art, screenshots, layout snapshot JSON, etc. -->
