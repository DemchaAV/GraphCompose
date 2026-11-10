# GraphCompose Render — DOCX

`io.github.demchaav:graph-compose-render-docx`

The DOCX export backend for GraphCompose, backed by Apache POI. It carries
`DocxSemanticBackend` and brings POI transitively, so a PDF-only consumer never pays for it.

Word is a full output from 2.5, beside PDF and PowerPoint: the export writes the page the
engine laid out as a document an editor can change, and Word paginates it. The Java API added
in 2.5 is Experimental (`@Beta`); `session.export(new DocxSemanticBackend())` is Stable.
Upgrading a 2.4 export: [2.5 Word export migration note](../docs/migration/v2.5.0-docx.md).

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

One call, as `buildPdf(path)` is for the PDF:

<!-- doc-example: id=readme-render-docx-export mode=method imports=com.demcha.compose.GraphCompose,java.nio.file.Path -->
```java
try (var doc = GraphCompose.document().create()) {
    doc.pageFlow().addParagraph("Hello, DOCX").build();
    doc.buildDocx(Path.of("hello.docx"));
}
```

`buildDocx(Path)`, `writeDocx(OutputStream)` and `toDocxBytes()` (Experimental, as are the
backend's provider, its builder and its report) find this module on the classpath and build
a default backend for each export. So they hand back no report, and they
stamp the export with the date it was made. For either, configure the backend and export
through the session — which writes the file directly, not through the replace-on-success
`buildDocx` uses, and throws a plain `Exception` rather than `DocumentRenderingException`:

<!-- doc-example: id=readme-render-docx-report mode=method imports=com.demcha.compose.GraphCompose,com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend,java.nio.file.Path -->
```java
try (var doc = GraphCompose.document().create()) {
    doc.pageFlow().addParagraph("Hello, DOCX").build();
    doc.export(DocxSemanticBackend.builder()
            .deterministic(true)                  // the same bytes on every run
            .reportSink(report -> report.notes().forEach(System.out::println))
            .build(), Path.of("hello.docx"));
}
```

**The report** (`DocxExportReport`) lists what Word could not carry as authored, and is empty
when everything was. Each note is `DROPPED` — the page draws it and the file does not have it —
or `APPROXIMATED` — it is in the file as the nearest thing Word has, such as a panel with square
corners where the page rounds them. A note also carries a subject (`chart`, `corner radius`),
where it was, and what it means for the document. Where it was is the authored node's path as
the layout graph addresses it, or `page N` — `section N` and `section N, page N` in a document
of several sections — or none, for the whole document or a page zone. `bySubject()` and
`count(severity)` summarise it. The sink is called once, after the bytes are complete; an
export that cannot proceed throws instead. See
[finding out what the export could not carry](../docs/recipes/docx-export.md#finding-out-what-the-export-could-not-carry).

**Deterministic output** pins the package's created and modified dates and every zip entry's
time, so the same document exports to the same bytes on every run on one platform. Across
platforms the bytes still differ where Apache POI ends an XML declaration with the platform's
line separator. `deterministic(Instant)` pins the dates to one of your own. A `{date}` token in a header or footer
still changes with the day unless `-Dgraphcompose.renderDate` pins it, as for the PDF.

[`WordExportExample`](../examples/src/main/java/com/demcha/examples/features/docx/WordExportExample.java)
exports one document as a PDF and as a Word file this way, and prints its report.

## What it maps, and what it does not

DOCX walks the **semantic node graph** in the order the document was written and produces
editable Word content — real paragraphs, runs, lists and tables — so Word owns the flow
once a reader edits it. What Word cannot work out for itself it takes from the session's
resolved layout: each line's height, table and row column widths, and how far a header or
footer sits from its page edge
([measured geometry](../docs/recipes/docx-export.md#measured-geometry)).

What maps:

- **Text.** Paragraphs keep their alignment and their runs; a run carries its font family,
  size, colour — a translucent one with its transparency, as Word's text fill — bold, italic,
  underline and strikethrough. A block's margin and padding
  become paragraph spacing. The fonts the document is set in are embedded, where there is a
  file behind them.
- **Lists** are real Word lists — a numbering definition, one level per nesting depth, the
  authored marker as the level's text.
- **Tables.** `colSpan` and `rowSpan` map to `w:gridSpan` and `w:vMerge`; fill, borders
  and text style take the most specific value in the table / column / row / cell cascade;
  padding becomes the cell's margins and `textAnchor` its alignment; header rows repeat on
  each page and every row is kept whole. A composed cell is written by the same writers as
  anywhere else, so it can hold an image, a list or a nested table.
- **Rows** are a one-row table whose columns are where the layout placed each child.
- **Panels.** A container with a fill or a border is a one-cell table carrying them, as is a row
  that paints its box, holding its columns; rounded corners come out square, and the report says
  so.
- **Images.** A block image keeps its size and fit mode. Pictures, SVG icons, emoji and
  shapes — dots, arrows, chevrons, checkboxes — in a line are inline pictures, placed where
  the page's alignment puts them; an icon's text is the picture's description. Code and badge
  chips keep their fill as run shading, without the shape.
- **Links and navigation.** A link is a `w:hyperlink`, to an address or to one of the
  document's anchors; an `anchor(...)` is a bookmark; a `bookmark(...)` outline level is
  Word's `HeadingN` style, so the paragraph is in the Navigation Pane, listed by the text of its
  Word paragraph.
- **Horizontal rules.** A level line with no transform, no thicker than 12pt, or an
  `addDivider` bar of plain fill no taller than 12pt, in the flow is Word's own rule: a
  paragraph whose bottom border is the stroke. Any other line, and one laid over others in a
  layer stack, is drawn as a shape.
- **Drawing.** Boxes, rounded boxes, ellipses, lines, polygons and paths are floating shapes
  placed where the page draws them, anchored in a paragraph of the block they belong to so they
  move with it. One whose block writes no paragraph on its page is carried by another paragraph
  there and placed from the page's edges. What
  a drawing loses — a transform, a gradient, a link or an outline entry — is named in a report
  note, on its table for one composed in a table cell. A line or a path is dashed as the page
  dashes it.
- **Barcodes and QR codes** are pictures of the same matrix the PDF draws, so they scan.
- **Charts** are Word charts — bar, line, pie and donut — with their numbers in an embedded
  workbook a reader edits through *Edit Data*; one drawn beside other drawings in a table cell,
  or in a table composed in a cell, is a table of its data.
- **Pages.** Page size, margins and orientation; page zones (`session.chrome().zone(...)`)
  and text headers and footers (`session.header(...)` / `footer(...)`) as real Word headers
  and footers with live page-number fields, a zone's logo an inline picture where the page
  draws it, and, in a zone of one line, each one-line part Word sets as the page does raised
  or lowered to the baseline the page sets it on, as far as the line holds it; document
  metadata (title, author, subject, keywords).
- **Byte-identical output** with `DocxSemanticBackend.builder().deterministic(true)` (see
  [Usage](#usage)).

What is not written — each one is named in the export report
([finding out what the export could not carry](../docs/recipes/docx-export.md#finding-out-what-the-export-could-not-carry)):

- **A drawing with nothing to place or show it by**: a shape or a path painted only with a
  gradient and a drawing exported without a layout, each named under its node's kind; one on a
  page no paragraph is written on, named as `drawing`; and any drawing in a page zone, named as
  `page zone content`.
- **Positioning and effects.** Word has no layers, so a layer stack or a canvas writes its text
  one block after another rather than where the page places it; its drawings still stand where
  the page draws them. Some stacks take a form Word has instead, and the report names the form
  on the stack: side-by-side columns are one table row, a cell per column; single-line paragraphs
  the page sets on one line share one Word line at tab stops; a stack the page gives no room,
  outside a table cell and holding only drawings and plain paragraphs on one page, is laid over
  the flow, its paragraphs in text boxes where the page sets them. A clip is not
  written, and the report names it where it cuts what its layers paint; a picture that fills a
  container clipped to an ellipse takes the ellipse. A rotation or scale is not carried.
- **Watermarks, protection and viewer preferences** (`watermark`, `protection`,
  `viewer preferences`).
- **The fill, outline and side borders of a row** composed in a table cell (drawn as a shape where
  they frame no text), in a page zone's line, in a band of overlapping layers or where a stack's
  column measures the space round it to its text, with a padding below zero or a column
  hanging into it, or with a margin below zero above or below it or at a side in a cell, named
  in the export report as `row paint`. Elsewhere a row that paints is written as a panel holding
  its columns, what of its borders above and below Word draws past its box taken by the space
  round it and its padding, and named where neither takes it.
- **What a block above leaves over the next** where the space between them does not take it —
  a panel's border below, a line hanging below its block, a negative bottom margin, the tenth of
  a point Word keeps between two tables — standing the next block that much lower, named in the
  export report as `space above`.
- **Translucency where Word holds an opaque colour only** — a cell's shading, a border, a rule,
  a chip's run shading: the colour is flattened against what Word paints under it — the panel or
  cell the export shaded, or what the page paints there — and a text header's separator against
  white; each is named in the export report (`translucency`, a chip's on its `inline chip` note).
  Text, drawings and pictures keep their alpha.
- **In a page zone, anything but paragraphs, page fields, spacers and pictures** — a barcode,
  a rule, a panel — named in the export report as `page zone content`.
- **Where a page zone's parts stand on Word's line, and what a paragraph or a picture in it
  loses of its own**, named in the export report as `page zone`.
- **A list's own geometry where Word cannot hold it**, named in the export report on the list:
  - a centred or right-aligned list's alignment;
  - its `lineSpacing` where the layout's items are not its own;
  - its `continuationIndent`;
  - the marker column and `markerGap` of an item at a stated column — a list that nests, or a gap
    too narrow for its marker — while a flat hanging-indent list keeps the page's column;
  - a row the page draws as a marker alone, for a blank item;
  - the marks of items the page reads as markdown, where they are written as letters and the page
    drops one: an item is written as the page sets it — its marks dropped, each piece in the
    page's face and size — wherever its lines show how, and as authored where its list's items are
    not matched one by one to the layout's (no layout, a cell, an item run onto the next page, a
    hanging-indent list with a blank item), its lines hold other letters, the parser reads a
    tree's marker as markdown with it (`*a*`), or the page sets none of its text, which is named
    as such; and a markdown heading written taller than its line. An item written as authored
    whose marks the page keeps is not named where the page changes only its face or letters no
    mark is made of (`1.`).
- **What a paragraph's own fields set where Word cannot hold it**, named in the export report on
  the paragraph outside a header or footer (a page zone's are named on the zone):
  - the size an auto-sized paragraph's text is fitted to, where the layout does not tell it (no
    lines read, lines in sizes that do not say which is the paragraph's, or one paragraph added
    at more than one place); elsewhere the text is written at it;
  - the marks of a paragraph the session reads as markdown (the default; `markdown(false)` turns
    it off), where they are written as letters: the paragraph is written as the page sets it —
    its marks dropped, each piece in the page's face and size — wherever the page's lines show
    how, and as authored where they do not (no layout, a cell whose text no line carries, a
    page zone's part the layout shows none of), hold other letters (Arabic, which the page shapes
    first) or hold none (text the parser reads into nothing, which the page sets as nothing); and
    a markdown heading written taller than its line, which Word cuts on screen;
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
