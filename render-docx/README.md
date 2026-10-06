# GraphCompose Render — DOCX

`io.github.demchaav:graph-compose-render-docx`

The semantic DOCX export backend for GraphCompose, backed by Apache POI. It carries
`DocxSemanticBackend` and brings POI transitively, so a PDF-only consumer never pays for it.

## When to depend on it

Add it (at compile scope) only when you export `.docx`. It is **not** included by
`graph-compose`, `graph-compose-core`, or `graph-compose-bundle` — DOCX is opt-in.

**From 2.5.0 it is sufficient on its own.** Opening a `DocumentSession` resolves a
`FontMetricsProvider` so text can be measured, and `graph-compose-render-pdf` is the only
artifact that publishes one — so this module brings it, at compile scope, as the PPTX
module does: a barcode is written as a picture of the same matrix the PDF backend draws,
through that module's encoder. A classpath of `graph-compose-core` +
`graph-compose-render-docx` opens a session and exports `.docx`.

**On 2.4.x and earlier it was not.** Those versions declared the PDF backend at test scope
only, and core + render-docx failed at `create()` with `MissingBackendException` before any
export happened. Add the PDF backend alongside it there — or depend on `graph-compose`,
which is core + render-pdf already:

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose-render-pdf</artifactId>
    <version>2.4.1</version>
    <scope>runtime</scope>
</dependency>
```

**Logging.** Apache POI logs through the Log4j API. Without a Log4j provider on the
classpath it prints one line at the first export —
`ERROR Log4j API could not find a logging provider.` — which is a notice, not a failure.
To send POI's messages where the rest of your logging goes, add the bridge at the same
version as the `log4j-api` POI brings (see `mvn dependency:tree`):

```xml
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-to-slf4j</artifactId>
    <version>2.24.3</version>
    <scope>runtime</scope>
</dependency>
```

## Usage

<!-- doc-example: id=readme-render-docx-export mode=method imports=com.demcha.compose.GraphCompose,com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend,java.nio.file.Path -->
```java
Path docxFile = Path.of("hello.docx");
try (var doc = GraphCompose.document().create()) {
    doc.pageFlow().addParagraph("Hello, DOCX").build();
    doc.export(new DocxSemanticBackend(), docxFile);
}
```

## What it maps, and what it does not

DOCX walks the **semantic node graph** in the order the document was written and produces
editable Word content — real paragraphs, runs, lists and tables — so Word owns the flow
once a reader edits it. What Word cannot work out for itself it takes from the session's
resolved layout: each line's height, table and row column widths, and how far a header or
footer sits from its page edge
([measured geometry](../docs/recipes/docx-export.md#measured-geometry)).

What maps:

- **Text.** Paragraphs keep their alignment and their runs; a run carries its font family,
  size, colour, bold, italic, underline and strikethrough. A block's margin and padding
  become paragraph spacing. The fonts the document is set in are embedded, where there is a
  file behind them.
- **Lists** are real Word lists — a numbering definition, one level per nesting depth, the
  authored marker as the level's text.
- **Tables.** `colSpan` and `rowSpan` map to `w:gridSpan` and `w:vMerge`; fill, borders
  and text style take the most specific value in the table / column / row / cell cascade;
  padding becomes the cell's margins and `textAnchor` its alignment; header rows repeat on
  each page and every row is kept whole. A composed cell is written by the same writers as
  anywhere else, so it can hold an image, a list or a nested table. A fill is opaque in
  Word.
- **Rows** are a one-row table whose columns are where the layout placed each child.
- **Panels.** A container with a fill or a border is a one-cell table carrying them; rounded
  corners come out square, and the report says so.
- **Images.** A block image keeps its size and fit mode. Pictures, SVG icons, emoji and
  shapes — dots, arrows, chevrons, checkboxes — in a line are inline pictures, placed where
  the page's alignment puts them; an icon's text is the picture's description. Code and badge chips keep their fill as run shading, without
  the shape.
- **Links and navigation.** A link is a `w:hyperlink`, to an address or to one of the
  document's anchors; an `anchor(...)` is a bookmark; a `bookmark(...)` outline level is
  Word's `HeadingN` style, so the paragraph is in the Navigation Pane, listed by the text of its
  Word paragraph.
- **Horizontal rules.** A horizontal line or an `addDivider` bar is a paragraph border.
- **Barcodes and QR codes** are pictures of the same matrix the PDF draws, so they scan.
- **Charts** are a table of their data.
- **Pages.** Page size, margins and orientation; page zones (`session.chrome().zone(...)`)
  and text headers and footers (`session.header(...)` / `footer(...)`) as real Word headers
  and footers with live page-number fields; document metadata (title, author, subject,
  keywords).
- **Byte-identical output** with `DocxSemanticBackend.builder().deterministic(true)`.

What is not written — each one is named in the export report
([finding out what the export could not carry](../docs/recipes/docx-export.md#finding-out-what-the-export-could-not-carry)):

- **Other drawing**: vertical and slanted lines, ellipses, polygons, paths, filled shapes
  other than a thin bar, and a line laid over something else in a layer stack or canvas.
- **Positioning and effects.** A layer stack, a canvas or a clipped container writes its
  children in order, without their positions and without the clip; a rotation or scale is
  not carried. A layer stack whose layers are side-by-side columns is the exception: it is
  written as one table row, a cell per column.
- **Watermarks, protection and viewer preferences**, each named in the export report.
- **A row's own fill, outline and side borders**, named in the export report as `row paint`.
- **In a page zone, anything but paragraphs, page fields and spacers** — a logo, a barcode,
  a rule — named in the export report as `page zone content`.
- **A list's own geometry where Word cannot hold it**, named in the export report on the list:
  - a centred or right-aligned list's alignment;
  - its `lineSpacing` where the layout's items are not its own;
  - its `continuationIndent`;
  - the marker column and `markerGap` of an item at a stated column — a list that nests, or a gap
    too narrow for its marker — while a flat hanging-indent list keeps the page's column;
  - a row the page draws as a marker alone, for a blank item.
- **What a paragraph's own fields set where Word cannot hold it**, named in the export report on
  the paragraph outside a header or footer (a page zone's paragraphs are not named yet):
  - the size an auto-sized paragraph's text is fitted to, where Word, to its half point, holds it
    apart from its style's;
  - the letters of a `bulletOffset` prefix; and, where it moves a line, the room a prefix sets
    lines in by in a paragraph written over the flow, as a side of an overlay's left-and-right
    pair or as a badge's initials;
  - an outline entry's title where it is not the text Word lists the heading by — for a side of
    an overlay's pair, the whole line — a level past Word's ninth that shares it with another, and
    the right side's entry of a pair whose left side holds the line's level.

Multi-section documents export through `MultiSectionDocument.toDocxBytes()`,
`writeDocx(...)` and `buildDocx(...)` (Experimental): each section becomes a Word section
with its own page size, orientation, margins, and the header and footer its page zones and
text header and footer describe — see the
[DOCX recipe](../docs/recipes/docx-export.md#several-sections-in-one-document).

Per-capability detail, with the implementing class for every supported cell:
[backend capability matrix](../docs/architecture/backend-capability-matrix.md).

## Install

Same version as the rest of the GraphCompose train (lockstep):

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose-render-docx</artifactId>
    <version>2.4.1</version>
</dependency>
```

```kotlin
dependencies { implementation("io.github.demchaav:graph-compose-render-docx:2.4.1") }
```

The full "which artifact?" table: [root README → Output formats and optional modules](../README.md#which-artifact).
Upgrading from 1.x: [modules migration guide](../docs/migration/v2.0.0-modules.md).
