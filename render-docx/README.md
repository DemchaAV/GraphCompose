# GraphCompose Render — DOCX

`io.github.demchaav:graph-compose-render-docx`

The semantic DOCX export backend for GraphCompose, backed by Apache POI. It carries
`DocxSemanticBackend` and brings POI transitively, so a PDF-only consumer never pays for it.

## When to depend on it

Add it (at compile scope) only when you export `.docx`. It is **not** included by
`graph-compose`, `graph-compose-core`, or `graph-compose-bundle` — DOCX is opt-in.

## Usage

```java
try (var doc = GraphCompose.document().create()) {
    doc.pageFlow().addParagraph("Hello, DOCX").build();
    doc.export(new DocxSemanticBackend(), docxFile);
}
```

DOCX maps the semantic node graph, not the fixed PDF layout — coverage is narrower than the
PDF backend.

## Install

Same version as the rest of the GraphCompose train (lockstep). Copy-paste snippet and the
full "which artifact?" table: [root README → Installation](../README.md#installation).
Upgrading from 1.x: [modules migration guide](../docs/migration/v2.0.0-modules.md).
