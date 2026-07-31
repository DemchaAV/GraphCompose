---
name: Bug report
about: Report a defect in GraphCompose
title: "[BUG] "
labels: bug
---

## What I expected

<!-- Describe the rendered output, layout behaviour, or API contract you expected. -->

## What actually happened

<!-- Stack trace, layout snapshot diff, mismatched pixel count, or a description of the wrong render. -->

## How to reproduce

```java
// Minimal, runnable Java that triggers the issue. Close the session and produce
// the output — a repro that never renders shows nothing.
try (DocumentSession document = GraphCompose.document(Path.of("repro.pdf"))
        .pageSize(DocumentPageSize.A4)
        .margin(24, 24, 24, 24)
        .create()) {

    document.pageFlow(page -> page
            .module("Repro", module -> module.paragraph("...")));

    document.buildPdf();   // or buildPptx(Path) / toPdfBytes()
}
```

## Generated artifact

<!-- Attach the produced PDF / PPTX / DOCX, or paste document.layoutSnapshot().
     The snapshot is renderer-neutral and usually pins a layout bug faster
     than the file does. -->

## Environment

- GraphCompose version: <!-- e.g. 2.1.0 -->
- Output backend: <!-- PDF (render-pdf) / PPTX (render-pptx) / DOCX (render-docx) -->
- Modules on the classpath: <!-- e.g. graph-compose; or core + render-pdf + templates. Note graph-compose-fonts / -emoji if present -->
- Java: <!-- e.g. Temurin 17.0.10 -->
- OS: <!-- e.g. Windows 11 / macOS 14 / Ubuntu 24.04 -->
- Font source: <!-- bundled graph-compose-fonts / registerFontFamily(...) / the PDF standard 14 -->

## Additional context

<!-- Optional. Related issues, prior art, screenshots, layout snapshot JSON, etc. -->
