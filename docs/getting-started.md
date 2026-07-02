# Getting Started

GraphCompose uses a canonical session-first API. Application
code starts with `GraphCompose.document(...)`, creates one
`DocumentSession`, describes content with `DocumentDsl`, and finishes
with `writePdf(...)`, `buildPdf()`, or `toPdfBytes()`.

> **Prerequisites:** Java 17+ and the `io.github.demchaav:graph-compose` dependency — see the [README install snippet](../README.md#installation).

> **New to GraphCompose?** Start with [Your First Document](first-document.md) — a five-minute, copy-paste path from an empty project to a rendered PDF, then come back here for themes, layer stacks, and built-in templates.

## Templates vs DSL — pick the right starting point

GraphCompose has two layers a caller can target. Use this decision
tree to choose the right one for the document you're rendering.

| Question | Answer | Pick this layer |
| --- | --- | --- |
| Is your document one of the template families (CV, cover letter, invoice, proposal)? | Yes | **Layered template preset.** Skip ahead to "Templates". |
| Do you need pixel-level control over a one-off PDF? | Yes | **Raw DSL** (`DocumentSession.pageFlow(...)`). |
| Do you need a re-usable scene for a *new* business document type? | Yes | **Custom template that wraps the DSL.** Implement `DocumentTemplate<S>` and take a `BrandTheme` for visual coherence. |

The DSL and the templates compose against the SAME `DocumentSession`
— a template can also live alongside hand-written DSL inside one
session, so you don't have to commit to one layer per document.

Arriving from a pre-2.0 surface (classic presets or the built-in
`*Template` classes)? See
[Which template system should I use?](templates/which-template-system.md)
for the naming history and the migration map.

## Quick start

The shortest path to a real PDF: open a session, drop a soft-panel
hero on the page, render. A couple of inline colour and text-style
constants keep the look consistent — swap them for your own brand
values.

```java
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import java.nio.file.Path;

DocumentColor cream = DocumentColor.rgb(252, 248, 240);   // paper
DocumentColor panel = DocumentColor.rgb(244, 238, 228);   // soft panel
DocumentColor accent = DocumentColor.rgb(196, 153, 76);   // gold accent
DocumentTextStyle h1 = DocumentTextStyle.builder()
        .fontName(FontName.HELVETICA_BOLD).size(28)
        .color(DocumentColor.rgb(20, 60, 75)).build();

try (DocumentSession document = GraphCompose.document(Path.of("output.pdf"))
        .pageBackground(cream)
        .margin(28, 28, 28, 28)
        .create()) {
    document.pageFlow(page -> page
            .addSection("Hero", section -> section
                    .softPanel(panel, 10, 14)
                    .accentLeft(accent, 4)
                    .addParagraph(p -> p
                            .text("GraphCompose")
                            .textStyle(h1))
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

Need to know *which* block is which? `DocumentDebugOptions` adds semantic
node labels on top of the guides — every rendered node gets a small corner
badge with the same stable path `layoutSnapshot()` reports:

```java
document.debug(DocumentDebugOptions.guidesAndNodeLabels());
byte[] labelledPdf = document.toPdfBytes();
```

Spot the misplaced block on the sheet, read its badge, then search that
name in your builder code.

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

## Templates

Templates compose into the same `DocumentSession`. Data specs live
under `com.demcha.compose.document.templates.data.*`, and each family
ships presets under `com.demcha.compose.document.templates.<family>.presets`
— `ModernInvoice`, `ModernProposal`, and the CV / cover-letter preset
galleries (see the
[Templates v2 (layered) quickstart](templates/v2-layered/quickstart.md)).

```java
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.invoice.presets.ModernInvoice;

DocumentTemplate<InvoiceDocumentSpec> template = ModernInvoice.create();

try (DocumentSession document = GraphCompose.document(Path.of("invoice.pdf")).create()) {
    template.compose(document, invoice);   // invoice = your InvoiceDocumentSpec
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
- [Canonical Legacy-Parity Matrix](./architecture/canonical-legacy-parity.md) — what
  works today, what is `Partial`, what is `Planned`.
- [Lifecycle](./architecture/lifecycle.md) — the session, layout, and render flow.
- [Production Rendering](./operations/production-rendering.md) — server-side
  lifecycle, privacy, and load guidance.
- [Package Map](./architecture/package-map.md) — read this before adding new public
  APIs or engine internals.
