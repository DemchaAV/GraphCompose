# DOCX export: the semantic backend

PDF is GraphCompose's fixed-layout output — every fragment lands at exact
coordinates. DOCX is different on purpose: it is a **semantic export** that
walks the document graph and writes editable Word content — Word paginates
it, and re-flows it after an edit. It reads the resolved layout only for
what Word cannot work out itself (see "Measured geometry"). Use it when
the recipient needs to *edit* the document; use PDF when pixels must match.

## Exporting a session

```java
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;

try (DocumentSession document = GraphCompose.document()
        .pageSize(595, 842)
        .margin(DocumentInsets.of(36))
        .create()) {
    document.pageFlow().name("Flow")
            .addParagraph(p -> p.text("Hello Word"))
            .addTable(t -> t
                    .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                    .row("R1C1", "R1C2"))
            .build();

    byte[] docx = document.export(new DocxSemanticBackend());
    // or write straight to disk:
    document.export(new DocxSemanticBackend(), Path.of("out/report.docx"));
}
```

`export(backend)` returns the DOCX bytes and also writes the session's
default output file when one was given to `GraphCompose.document(path)`;
the two-argument overload targets an explicit path.

**Dependency note:** the DOCX backend ships in the
`io.github.demchaav:graph-compose-render-docx` artifact, which brings Apache POI
transitively. Add that one dependency to export DOCX — consumers who only render
PDF never pull POI.

### Reusing a backend, and threads

A `DocxSemanticBackend` starts every export from nothing, so one instance can export any
number of documents one after another — including after an export that threw. It holds the
running export's state in its own fields, so it is not for two threads at once: create one
per thread. `session.buildDocx(...)`, `writeDocx(...)` and `toDocxBytes()` take a new backend
for every export and need nothing.

### Byte-identical output

For reproducible builds and byte-level tests, pin the package's clocks:

```java
byte[] docx = session.export(DocxSemanticBackend.builder()
        .deterministic(true)                  // or deterministic(Instant) for your own date
        .reportSink(report -> System.out.println(report.notes()))
        .build());
```

The package's created / modified dates and every zip entry's time are pinned, so the same
document exports to the same bytes on every run and machine — the contract the PDF and PPTX
backends keep, with the same default instant. Embedded fonts are deterministic either way:
their obfuscation keys are derived from the font. Off by default, because a document's
creation date is real metadata.

## What maps 1:1

| Document node | DOCX output |
|---|---|
| Paragraphs | Word paragraphs with alignment, font, size, colour, bold/italic/underline; inline runs preserved |
| Lists | Real Word lists: a `numbering.xml` definition per list, `w:numPr` on each item, and the authored marker as the level's text. Nesting is a list level, so Enter continues the list and Tab demotes an item. See "What a list becomes" below for the kinds that stay plain paragraphs |
| Tables | Word tables, one cell per cell. Each cell states its own padding as `w:tcMar`, on all four sides, so a row is as tall as the page draws it. Its `textAnchor` becomes `w:vAlign` and the paragraph's `w:jc`, with the engine's default — the vertical middle, on the left — where Word's is the top, so a line beside a taller neighbour sits where the page puts it and an amount column stays right-aligned. A cell with no style of its own is set in the engine's default cell face rather than the document's Normal. A column sized to its content gets a point more than the page gives it, so the editor's font substitute cannot wrap its widest cell. The width is written when the document states one or every column is fixed; otherwise Word sizes the table — see "What falls back". A table breaks across pages where the layout breaks it: every row the layout placed is kept whole (`w:cantSplit`), `repeatHeader(n)` rows repeat on each page (`w:tblHeader`) and stay with the row under them. Two tables in a row — rows included, since a row is carried as a table — are kept apart by a paragraph a tenth of a point tall, holding the rest of the gap between them: an editor joins two tables with nothing between them into one |
| Composed cells (`DocumentTableCell.node(...)`) | Written by the same writers that write that node anywhere else, so a cell built from an image, a list or a table carries it. A nested table is a real `w:tbl` followed by the paragraph Word requires a cell to end with — a hairline, which the paragraph written next in the cell takes over, so no empty line opens under the table — and takes the width of the column it sits in — the column's, not the one the page gives it, because the layout reports a composed cell's content under the owner's path |
| Inline chips (`inlineCode(...)`, `inlineChip(...)`, `highlight(...)`) | The chip's fill becomes the run's own `w:shd`, in a paragraph and in a list item alike. Its shape does not travel — see "What a chip keeps and loses" below |
| Images | Embedded pictures at the node's declared size |
| Links and anchors | A `linkTarget` becomes a `w:hyperlink` — a relationship for an address, `w:anchor` for one of the document's own anchors — and a run's own link wins over the paragraph's — in a list item as much as in a paragraph. An `anchor(...)` becomes a bookmark wrapping that paragraph's text, named as Word requires; on a section, container, table or image it wraps everything the block wrote, from the start of its first paragraph to the end of its last, so a link to a block lands on its first line. A `bookmark(...)` outline level becomes Word's own `HeadingN` style, which is what puts the paragraph in the Navigation Pane, the outline view and a generated table of contents. The style states the outline level and nothing else, so the paragraph keeps its own formatting. The role comes from what the document declared, never from how big the text is |
| Rows | A one-row table spanning the content width, so editors keep the side-by-side layout. The row's slots become the column grid when they are weights, an even split or fixed columns; the gap and the row's padding ride in the neighbouring column and come back out as that cell's margin; a cell holds whatever its child is, written as it is anywhere else. The row's `verticalAlign` is every cell's `w:vAlign`, so a child shorter than the row sits at its middle or bottom as on the page — a table of contents' leader on its entry's baseline. The row is kept whole across a page break, as the layout keeps it |
| Sections / containers | Children written in order. A container with a fill, per-side borders or a uniform stroke is a one-cell table carrying them, its padding as the cell's margins, so a card keeps its panel — see "What a panel keeps and loses" below. A `keepTogether()` or `keepWithNext()` block the layout placed on one page stays on one page in Word too (`w:keepLines` + `w:keepNext`, and a row that may not split for a panel) |
| Spacers | Empty paragraphs carrying the vertical gap as spacing-after |
| Page breaks | Explicit Word page breaks |

Page geometry (size, margins and orientation — a page wider than it is tall is stated as
landscape) and session metadata (title, author, subject, keywords) carry into the Word
document as well.

## Fields, and when they update

Everything that states a page number is a Word field rather than text, so it stays right
when the reader edits the document:

| Where | Field | Updated by |
|---|---|---|
| a page zone's `pageNumber()` | `PAGE` | the editor, every time it lays the pages out |
| a page zone's `pageTotal()` | `NUMPAGES`, or `SECTIONPAGES` in a multi-section document | the editor, every time it lays the pages out — except LibreOffice, which does not update `SECTIONPAGES` |
| a table of contents' page numbers, `addPageReference(...)` | `PAGEREF` to the anchor's bookmark, as a hyperlink | LibreOffice on every layout (measured: a field whose stored number was replaced by 99 showed the real page); Word when fields are updated — F9, or printing with field updates on |

Each field also stores a result, which is what a reader sees before an editor updates it
and what a text extractor finds: the page the layout resolved, and for a page total the
number of pages it laid out. A file therefore opens reading the same numbers as the PDF.

The export does not set `w:updateFields`. It would make Word ask, on every open, whether to
update fields — to recompute numbers that already read correctly.

A page reference to an anchor the document does not bookmark is written as its text, the
placeholder the page prints: Word turns a `PAGEREF` to a missing bookmark into "Error!
Bookmark not defined." the first time it updates.

## Several sections in one document

A `MultiSectionDocument` — a cover in one page size, a body in another — exports to Word
the way it renders to PDF, one section of the file per session:

```java
try (MultiSectionDocument document = GraphCompose.documents()
        .section(cover)
        .section(body)
        .create()) {
    document.buildDocx(Path.of("out/report.docx"));
}
```

Each section keeps its own page size, orientation, margins, and the header and footer its
page zones (`session.chrome().zone(...)`) describe — the text header and footer slots are
not written, in a section or in a single document (see below). Where Word
would behave differently left to itself, the export tells it what the PDF does:

- page numbers start again at 1 in every section, and a zone's `pageTotal()` is the
  section's page count (`SECTIONPAGES`), not the document's;
- a section with no header or footer of its own gets an empty one, since Word would
  otherwise repeat the previous section's;
- the metadata is the first section's that states any.

Styles, fonts and bookmark names are shared across the document, so a link in the cover
reaches an anchor in the body. `export(backend)` takes any semantic backend; one that
cannot combine sections refuses more than one.

## Finding out what the export could not carry

The export says what it drops — but it says it to the log, which a service generating
documents for other people cannot read. Pass a sink and the same information arrives as a
value:

```java
var notes = new ArrayList<DocxExportReport.Note>();
session.export(new DocxSemanticBackend(report -> notes.addAll(report.notes())));
```

Each note carries a severity, what it was about, the authored node's path, and what it
means for the document. `DROPPED` means the page draws it and the document does not carry
it; `APPROXIMATED` means it is in the document as the nearest thing Word owns — a panel
that keeps its fill and loses its rounded corners. Neither is an error: an export that
cannot proceed throws, and the report is not how you find that out.

The sink is called once, after the bytes are complete. The convenience methods
(`buildDocx`, `writeDocx`, `toDocxBytes`) build their own backend and so have no sink —
use `session.export(...)` when you need the report.

## Measured geometry

The export asks the session for the resolved layout and writes three things from it that
it cannot work out for itself:

| What | Where it lands |
|---|---|
| Line height | `w:spacing w:lineRule="exact"` on every paragraph, cells and list items included — the height the engine measured, not a multiple Word would measure again against a substituted font |
| Table columns | the resolved cell widths as `w:gridCol`, with `w:tblLayout` fixed so Word does not re-fit them |
| Row columns | where the layout placed each child, with the row's gap and padding folded into the neighbouring column and taken back out as that cell's margin. A column sized to its content (`DocumentRowColumn.auto()`) gets a point more, taken from the row's weight columns so the row keeps its width, for the reason a table's does: the editor's substitute font would wrap it — a table of contents' labels broke mid-word ("Intr" / "o") in LibreOffice without it. A row with no auto column, no weight column, or no stated columns (weights, an even split) is written as placed |

The space a block holds above and below itself needs no measuring and is written from the
document: a paragraph's `margin` and `padding` become `w:spacing`, and a container hands
its top edge to the first paragraph inside it and its bottom edge to the last, since a
container is not a Word object. Everything meeting at one gap adds up, the way the page
sums it.

An image and a list hold their own space the same way a paragraph does — a picture's
paragraph is the picture's block, and a list's edges go to the paragraphs around it, with
`itemSpacing` as the gap above each item after the first.

A table holds its own space the same way. Word has no space above a table and none below
one, so a table's or a row's `margin` and `padding` travel to the paragraphs around it —
the space above a table is the space below the paragraph before it. A table with no
paragraph above it loses that edge, which is the one gap Word has nowhere to put.

A gap is written **once, above**. The space a block holds below itself waits for the next
paragraph and is written there as `w:before`, together with whatever that paragraph asks
for itself — rather than as `w:after` on one paragraph and `w:before` on the next. Editors
disagree about two adjacent gaps: measured on a card holding 20pt below itself followed by
a heading asking for 16pt above, LibreOffice rendered 20pt where the page shows 36, taking
the larger instead of the sum. One number on one side reads the same either way. The gap
goes back to the paragraph above only where nothing below can hold it — before a table,
which has no space above it in Word, before a page break, at the end of a cell, and at the
end of the document.

The horizontal half is carried as an indent: outside any panel, every paragraph by each enclosing container's
margin and padding, a row or a table by the same amount as `w:tblInd` — see "What a panel
keeps and loses".

Asking for the layout costs a measurement and pagination pass over the document, the same
work a PDF render does, and it reads each image a second time.

## Fonts travel with the document

The package carries the faces the document is set in, so a reader without them installed
sees the document rather than a substitution. What is shipped is narrow on purpose:

- **Only families with a file behind them** — the bundled ones and whatever the session
  registered. The standard PDF faces are names rather than files: nothing bundles
  Helvetica, and a reader gets the editor's substitution for it, the same one a PDF viewer
  applies.
- **Only the faces the document uses.** One family's four faces are about 2.5 MB, so the
  face is chosen from each style's decoration. A reader who later bolds a word gets
  whatever their machine does for a missing bold face.
- **Only what the face permits.** An OpenType face states its terms in `OS/2`, and the
  format distinguishes embedding for reading and printing from embedding in a document
  someone will edit. A face that allows only the first is named but not shipped, with one
  warning naming the family.

Each face is stored the way Word stores one: the font with its first 32 bytes scrambled
against a key the font table states beside it.

A run names the **family**, not the face. `FontName.HELVETICA_BOLD` is a face, and Word
resolves families and takes the weight from `w:b`; asked for a family by that name it finds
none and substitutes. The face is resolved to its family exactly as the layout resolves it,
through `FontLibrary.resolveFamily`, and the name written is that family's `wordFamily()`.
The weight is not read from the face name, because the engine does not read it either — a
style naming `HELVETICA_BOLD` and setting no `decoration` lays out regular, so writing
`w:b` would make Word bolder than the page it is matching. Set `decoration(BOLD)` to get
bold in both.

## Named styles, so the document can be restyled

The export writes a styles part whose `Normal` carries the document's own body text —
the style the most characters are set in, not the one the most nodes use. Runs that only
restate it stay silent, so changing `Normal` in Word changes the body the way a reader
expects. A run whose font, size or colour differs keeps saying so, so headings, chips and
accents are unaffected.

A paragraph that declared an outline level — `bookmark(new DocumentBookmarkOptions(name,
level))` — also carries Word's own `HeadingN` style, which is what fills the Navigation
Pane, the outline view and a generated table of contents. The style states the outline
level and no formatting, so the paragraph keeps the look its author gave it and "restyle
all headings" in Word still reaches it. Only the levels the document uses are defined, and
a level past Word's nine is clamped. A heading is never inferred from type size: a large
first line claims nothing about structure.

## What a list becomes

A list exports as a list Word owns: a `numbering.xml` definition, `w:numPr` on each item,
and the authored marker as the level's text. That is what makes Enter continue the list
and Tab demote an item, instead of producing a plain paragraph beside a bullet character
that only looked like one.

Nesting is a level rather than padding, so no indent characters reach the text, and the
`ListMarker.defaultForDepth` cascade the PDF path uses becomes the levels' markers —
`markerFor(depth, ...)` still chooses a level's own.

Four kinds of list stay plain paragraphs, because Word could not express them without
changing what was asked for:

- **A markerless list.** Numbering always draws something and indents; a list that asked
  for neither would gain both.
- **A drawn marker** — one made of runs, an icon or a disc. It has no Word list analogue,
  so the item keeps the run path it already used.
- **A list whose siblings at one depth carry different markers.** A Word list definition
  names one marker per level, and silently replacing one of them with the other would be
  worse than writing both as text.
- **Rich items**, whose runs the numbered path does not write.

The marker column is a stated constant — 180 twips, plus 120 for each nesting level —
chosen near the single space the old text form left. It is a convention, not a
measurement: measuring the marker needs a font runtime this backend does not have, which
is the same reason `markerGap` is unrepresentable here.

## What a panel keeps and loses

A container that paints — a fill, per-side borders, a uniform stroke — exports as a table of
one cell, which is how a panel is built in Word by hand. Word has no element that wraps a
run of paragraphs, and a cell holds everything a card needs: its shading is the fill behind
whatever is inside, its borders are the card's edges at the card's full height, and its
margins are the padding on all four sides.

```java
page.addSection("Notice", card -> card
        .softPanel(surface, 8, 14)     // fill and padding land; the radius does not
        .accentLeft(accent, 3)         // the cell's left border
        .addParagraph(p -> p.text("The panel grows with this text when it is edited.")));
```

What is inside is written by the same writers as anywhere else, so it stays paragraphs,
lists, rows, tables and pictures a reader edits as usual, and the panel grows as they do —
which is the point of exporting DOCX rather than PDF. A container with no paint is not a
table: its children are written where it stood, indented by its margin and padding.

How it lands:

- **Width.** The table is as wide as the layout placed the container, which is as wide as
  its content when the content is short, plus a point of slack so an editor setting the
  text in its own face keeps the page's line breaks.
- **Borders.** The page centres a border on the panel's edge; Word keeps a cell's border
  inside the cell. Half of each border comes off that side's margin and the table widens
  by the other half, so the text and the border land where the page draws them. Measured
  in LibreOffice against the engine's render at 96 dpi, the band, a 3pt accent bar and the
  text of a card land within a pixel of the page's.
- **Nesting.** A panel inside a panel is a table inside its cell. So is a row, with no fill
  of its own, so the panel shows through it. A table keeps its own cell fills, and a cell
  no style fills is written white, as the page draws it on the card.
- **Keeping together.** A `keepTogether()` panel the layout placed on one page is a row
  Word may not split. Anchors and keeps on the blocks inside a panel carry as they do
  anywhere else.
- **Page breaks.** Word breaks no page inside a table cell, so a page break among a
  panel's children closes the panel there and opens it again after the break.
- **Measured in LibreOffice.** Word has not been measured yet; where it places a nested
  table differently, a panel inside a panel may sit a few points off.

Not representable, and left undone rather than approximated:

- **The corner radius.** A cell is rectangular. The panel renders with square corners and
  the export logs one warning per document.

## Pictures and icons in a line

A picture, an SVG icon or an emoji in a line of text is a picture in Word, in its own run
between the words around it, at its size:

```java
page.addParagraph(p -> p
        .inlineSvgIcon(phone, 12, InlineImageAlignment.CENTER)
        .inlineText(" +44 20 7946 0000 ")
        .inlineEmoji(":rocket:", 14));
```

- **Where it sits.** Word stands a picture on the line's baseline; the page centres it on
  the line, or sets it on the baseline or at the text's top or bottom. The picture is
  raised or lowered by `w:position` to where the page's alignment and `baselineOffset`
  put it, from the layout's measure of the paragraph's first line — in a list, the list's
  text on a line as tall as the item's own tallest picture. Word honours that on a picture;
  LibreOffice does not — measured, a picture written at 0, −2, −10 and +10pt stood in the
  same place — so there a picture always stands on the baseline, higher than on the page by
  as much as the page lowers it: up to the text's descent for a centred icon as tall as its
  line.
- **Line height.** Lines are written at an exact height, and the editor clips a picture
  to it — where in that height it puts the baseline is its own, so no fixed room is
  enough: measured in LibreOffice, a 14pt icon centred over 9pt text lost its top up to
  a 17.6pt line. A paragraph holding a picture that rises above its text's ascent or hangs
  below its descent — where Word puts it, or on the baseline where LibreOffice does — is
  written with its lines *at least* the height the picture reaches instead, so the editor
  grows the line to the picture rather than clip it. Word has one line height for a
  paragraph, so every line of it is then at least that reach and otherwise as tall as the
  editor's own font makes it — for 14pt text, about 2.5pt taller than the page's in
  LibreOffice. A picture that stays inside the text in both editors keeps the exact
  height; a 12pt icon on a line of 14pt text does not, since on the baseline it rises past
  the ascent.
- **What an icon is.** An SVG icon — an emoji among them — is drawn into a transparent
  picture from the same layers the page draws, by the raster the PPTX export falls back
  to, so it looks as it does on the page. The text it stands for is the picture's
  description, which a screen reader reads; it is not a character a reader copies or
  searches, and the report says so.
- **Links.** A picture carrying a link, or in a linked paragraph, is inside the link.

An inline shape — a `dot(...)`, an arrow, a chevron — is not written yet, and the report
names each one.

## What a chip keeps and loses

A chip is a fill behind a phrase, and Word has one: `w:shd` on the run, taking any RGB.
So a status badge still reads as a badge and an inline `code()` span still reads as code,
in a paragraph and inside a list item alike.

```java
page.addParagraph(p -> p
        .inlineText("Invoice ")
        .inlineChip("overdue", DocumentColor.WHITE, accent)   // fill lands
        .inlineText(" — settle by Friday."));
```

What Word has no way to say is the chip's *shape*. Shading covers the glyph box, so:

- **The corner radius** is square in Word.
- **The padding** that widens the run on the page is not in the file, so the fill hugs
  the glyphs and the line is fractionally shorter than the PDF's.

Both are recorded as `APPROXIMATED` in the export report, per chip, so a caller can see
which phrase lost what.

A `w:shd` fill is opaque, so a translucent chip — `inlineCode(...)` is a fifth-opacity
grey — is flattened first against what the export wrote underneath it: the paragraph's own
shading, the cell's, or the page. Written at full strength the default code chip would be
a solid slab where the page has a tint; flattened, it is the colour the PDF shows. The
chip agrees with the file it is in rather than with the page the PDF drew — a translucent
*container* fill lands opaque too, and a chip on it composites over that. And the chip
stops being translucent: shade that paragraph another colour in Word and it keeps the
tint it was flattened to. Recorded, like the other two.

## What falls back

- **A document the engine cannot lay out → the same export, without measured geometry.**
  The export asks the session for the resolved layout (see "Measured geometry" above). A
  document that the fixed-layout pipeline refuses — a list item made of inline runs
  without marker geometry, for instance — still exports: the failure is logged once and
  the writer falls back to what the document itself states. In that fallback a table's
  width is written only when the author stated one or every column is fixed, a row's
  columns only when they are weights, an even split or fixed, and no line height is
  written at all. An `auto` column and the flex path are measurements, and a guess in
  their place would be right for a table whose text fills the line and wrong for one
  holding three short values.

- **Charts → data table.** A chart compiles to vector geometry, which this
  export does not draw. Its *semantic*
  content is its data, so the backend writes a categories-by-series table
  (values formatted with the chart's own axis format) and logs **one
  capability warning per export**. See [charts.md](charts.md).
- **Shape containers → inline layers.** DOCX has no portable equivalent
  of a graphics-state path clip, so the container's layers are written
  inline, in source order, without the outline frame and without clipping
  — again with one warning per export.
- **`hangingIndent(true)` → the ordinary list form.** A list that opts
  into marker/content geometry exports exactly as one that did not: the
  same Word list, the same levels, the same markers. Nothing is lost —
  same items, same text, same nesting — but the marker column is the
  level's own and `markerGap` has no effect here.

  This is a decision rather than an omission. Word places content at
  absolute indents and has no way to be told "start the text one marker
  width plus a gap from here", so every mechanism that looks like it
  would — a hanging indent, a hanging indent with a tab stop, real Word
  numbering — leaves a distance beside the marker equal to the column
  minus the marker's own width, a number only Word knows. Honouring the
  gap would mean knowing the marker's width as the reader's editor sets
  it, in whatever font it substitutes — which the export cannot know. The
  approximations were built and rendered through Word before being
  rejected — a reserved column renders a gap that is not the one
  configured, and a marker wider than the column misaligns outright.

## What is skipped

A horizontal rule is not skipped. A horizontal line, or a filled bar no taller
than 12pt — what `addDivider` draws — is Word's own rule: an empty paragraph
whose bottom border is the stroke, in its colour and thickness, from where the
line starts to where it ends, with the space above and below the stroke kept.
It flows with the text, and a reader moves or deletes it as a line of the
document. A dashed line keeps a dash, in Word's own lengths; a translucent one is
flattened against what lies under it, since a border is opaque. Three limits:

- A line laid over something else — a layer in a layer stack or a canvas, such as
  a skill meter's track and the fill over it — is not a rule in the flow, and is
  dropped and reported like other drawing.
- A rule in a page zone is not written, as a zone takes paragraphs, fields and
  spacers.
- Word draws one border for consecutive paragraphs whose borders and indents are
  the same, whatever the space between them, so two identical rules with no other
  paragraph between them show as one.

The placement was measured in LibreOffice; Word has not been measured yet.

Vertical and slanted lines, ellipses and other standalone shapes are
**skipped**, and the report names each one — they are pure fixed-layout
geometry with no semantic equivalent.

A barcode in the body is not skipped: it exports as a picture of the symbol at
its size, the same matrix the PDF draws, so it scans, with its data as the
picture's description. Its data is part of the picture — changing it means
exporting again — and the report says so, and names a link or a transform on it
as not carried. In a page zone a barcode is still skipped.
The text header and footer slots, watermarks, and protection options are
also ignored by the current exporter.

A page zone (`session.chrome().zone(...)`) is not: it exports as a real
Word header or footer part, with the page number as a live field, and it
sits as far from its page edge as the page puts it — the distance is read
from where the zone's content landed in the resolved layout and written as
`w:pgMar/@w:header` or `@w:footer`, rather than left to Word's 36pt.

A zone drawn on some pages only (`appliesTo(...)`) lands on the same pages when Word can
say so. Word has a header and footer for the first page, for even pages and for the rest,
so the predicate is asked over sample pages and sorted into those kinds:

| Predicate | In Word |
|---|---|
| `PageContext::isFirst` | the section's first-page header, with a title page stated |
| `page -> !page.isFirst()` | the ordinary header, and an empty one on the first page |
| even or odd page numbers | the even-page header or the ordinary one, with different even and odd pages stated for the whole document |

A predicate that picks pages within a kind — the last page, the third — has no Word part.
Such a zone is written on every page and the export report says so.

The rule of thumb: if the document leans on geometry — shapes, layered
designs, precise placement — export PDF for the reader and DOCX only as
an editable companion.

Round-trip coverage (paragraphs, tables, metadata, chart fallback) lives in
[`DocxSemanticBackendTest`](../../render-docx/src/test/java/com/demcha/compose/document/backend/semantic/docx/DocxSemanticBackendTest.java).
