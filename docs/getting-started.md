# Getting Started

GraphCompose v1.5 uses the canonical session-first API. Application
code starts with `GraphCompose.document(...)`, creates one
`DocumentSession`, describes content with `DocumentDsl`, and finishes
with `writePdf(...)`, `buildPdf()`, or `toPdfBytes()`.

## Templates vs DSL — pick the right starting point

GraphCompose has two layers a caller can target. Use this decision
tree to choose the right one for the document you're rendering.

| Question | Answer | Pick this layer |
| --- | --- | --- |
| Is your document one of the built-in shapes (CV, invoice, proposal, weekly schedule, cover letter)? | Yes | **Built-in template.** Skip ahead to "Built-in templates". |
| Do you need pixel-level control over a one-off PDF? | Yes | **Raw DSL** (`DocumentSession.pageFlow(...)`). |
| Do you need a re-usable scene for a *new* business document type? | Yes | **Custom template that wraps the DSL.** Implement the `*Template` interface and re-use `BusinessTheme` for visual coherence. |

The DSL and the templates compose against the SAME `DocumentSession`
— a template can also live alongside hand-written DSL inside one
session, so you don't have to commit to one layer per document.

## Quick start

The shortest path to a real PDF: open a session, drop a soft-panel
hero on the page, render. The `BusinessTheme` keeps the look
consistent with the rest of your branded documents.

```java
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.theme.BusinessTheme;

import java.nio.file.Path;

BusinessTheme theme = BusinessTheme.modern();   // cream paper + teal/gold

try (DocumentSession document = GraphCompose.document(Path.of("output.pdf"))
        .pageBackground(theme.pageBackground())
        .margin(28, 28, 28, 28)
        .create()) {
    document.pageFlow(page -> page
            .addSection("Hero", section -> section
                    .softPanel(theme.palette().surfaceMuted(), 10, 14)
                    .accentLeft(theme.palette().accent(), 4)
                    .addParagraph(p -> p
                            .text("GraphCompose")
                            .textStyle(theme.text().h1()))
                    .addParagraph("Quick-start hero block."))
            .module("Summary", module -> module.paragraph(
                    "GraphCompose composes a document graph and renders it twice — "
                            + "once as a deterministic layout snapshot, once as the final PDF.")));

    document.buildPdf();
}
```

## Streaming Output

Use `writePdf(OutputStream)` for web APIs, cloud storage uploads, and
other server paths where the caller already owns an output stream.
GraphCompose writes the PDF but does not close the stream.

```java
void writeResponse(OutputStream responseOutputStream) throws Exception {
    try (DocumentSession document = GraphCompose.document().create()) {
        document.pageFlow(page -> page
                .module("Summary", module -> module.paragraph("Generated for an HTTP response.")));

        document.writePdf(responseOutputStream);
    }
}
```

## In-Memory Output

```java
byte[] pdfBytes;

try (DocumentSession document = GraphCompose.document().create()) {
    document.pageFlow(page -> page
            .module("Summary", module -> module.paragraph("Generated for an HTTP response.")));

    pdfBytes = document.toPdfBytes();
}
```

`toPdfBytes()` is a convenience wrapper around the streaming path.
Prefer `writePdf(...)` when the next step is already a stream.

## Debug Guide Lines

Guide lines are a render-only diagnostic overlay for checking page
margins, padding, and resolved boxes. They do not change layout
geometry or layout snapshots.

```java
try (DocumentSession document = GraphCompose.document(Path.of("debug.pdf"))
        .guideLines(true)
        .create()) {
    document.pageFlow(page -> page
            .module("Summary", module -> module.paragraph("Guide-line preview")));

    document.buildPdf();
}
```

You can also toggle the same option on an open session before
convenience PDF output:

```java
document.guideLines(true);
byte[] debugPdf = document.toPdfBytes();
```

## Module-First Authoring

Use modules when you are building a normal document section. A module
is a titled full-width block with a body made from semantic content.

```java
document.pageFlow(page -> page
        .module("Professional Summary", module -> module.paragraph(summary))
        .module("Technical Skills", module -> module.bullets(skills))
        .module("Projects", module -> module.rows(projectRows)));
```

The common body calls are `paragraph`, `bullets`, `dashList`, `rows`,
`table`, `image`, `divider`, and `pageBreak`.

## Layer stacks (overlay primitive)

`addLayerStack(...)` composes children inside the same bounding box,
in source order — first child behind, last in front. Each layer
carries one of nine `LayerAlign` values. The builder also exposes
nine alignment-named shortcuts (`topLeft`, `topCenter`, …,
`bottomRight`) plus `position(node, offsetX, offsetY, anchor)` for
screen-space nudges. Pagination is atomic: the entire stack moves to
the next page if it does not fit.

```java
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.node.ShapeNode;

document.pageFlow(page -> page
        .addLayerStack(stack -> stack
                .name("HeroBadge")
                .back(new ShapeNode(...))                          // background
                .center(new ParagraphBuilder().text("M&A").build())  // foreground
                .topRight(badge)                                   // overlay anchor
                .position(stamp, -8, 4, LayerAlign.BOTTOM_LEFT)));   // anchor + offset
```

## Shape-as-container with clip path

`addCircle(diameter, fill, inside)` /
`addEllipse(w, h, fill, inside)` / `addContainer(...)` build a
`ShapeContainerNode` whose bounding box is dictated by the outline
(rectangle, rounded rectangle, ellipse, or circle). Children are
clipped to the outline path (`ClipPolicy.CLIP_PATH` is the default),
to the bounding box (`CLIP_BOUNDS`), or render unclipped
(`OVERFLOW_VISIBLE`).

```java
document.pageFlow(page -> page
        .addCircle(80, brand, circle -> circle
                .name("BrandSeal")
                .center(new ParagraphBuilder().text("M&A").build())));
```

The PDF backend honours every clip policy via graphics-state
`saveGraphicsState()` / `clip(path)` / `restoreGraphicsState()` markers
emitted by the layout layer; the DOCX backend renders layers inline
without the outline frame and logs a one-time capability warning. See
[`docs/recipes/shape-as-container.md`](recipes/shape-as-container.md)
for the full recipe.

## Built-In Templates

Built-ins compose into the same `DocumentSession`. Template data
lives under `com.demcha.compose.document.templates.data.*`, and
templates live under
`com.demcha.compose.document.templates.builtins`.

```java
InvoiceTemplate template = new InvoiceTemplateV1();

try (DocumentSession document = GraphCompose.document(Path.of("invoice.pdf")).create()) {
    template.compose(document, invoice);
    document.buildPdf();
}
```

## Where To Go Next

- [Recipes — themes, shapes, transforms, tables, layout snapshots](./recipes.md)
  splits the long-form copy-paste catalogue into focused pages.
- [Shape-as-container](./recipes/shape-as-container.md) — circles,
  ellipses, rounded cards with clipped layers.
- [Transforms and z-index](./recipes/transforms.md) — rotate, scale,
  per-layer z-index for overlays.
- [Advanced tables](./recipes/tables.md) — row span, zebra rows,
  totals row, repeating header on page break.
- [Canonical Legacy-Parity Matrix](./canonical-legacy-parity.md) — what
  works today, what is `Partial`, what is `Planned`.
- [Lifecycle](./lifecycle.md) — the session, layout, and render flow.
- [Production Rendering](./production-rendering.md) — server-side
  lifecycle, privacy, and load guidance.
- [Package Map](./package-map.md) — read this before adding new public
  APIs or engine internals.
