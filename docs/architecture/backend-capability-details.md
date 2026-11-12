# Backend capability details

The full text behind the [backend capability matrix](backend-capability-matrix.md):
one heading per engine capability, in the matrix's order, and — for every
supported backend — the class that implements it. The matrix is the
one-glance view; this file is the source of truth for "which backend can do
what, and where is that code".

Maintenance rule: any PR that adds, degrades, or removes a backend
capability updates this file and the matrix in the same change. A
capability without a row in the matrix and a heading here does not exist;
a ✅ entry here without a class name is a doc bug.

Legend:

- ✅ supported — the entry names the implementing class
- ⚠️ degraded — supported with a documented deviation (see the note)
- 🚧 in development — implementation in progress, not usable yet
- ❌ not supported — no implementation, no current plan
- n/a — concept does not apply to this backend's output model

Backends — PDF, PPTX and DOCX are all full outputs. The matrix says,
per capability, what each one carries;
the headings below say it in full. The PPTX Java API, and the DOCX API
added in 2.5, are Experimental (`@Beta`), so their shape may still change in a
minor (see
[../api-stability.md](../api-stability.md)).

- **PDF (fixed-layout)** — `graph-compose-render-pdf`,
  `com.demcha.compose.document.backend.fixed.pdf`. Consumes the resolved
  `LayoutGraph`; the reference the other two are measured against.
- **PPTX (fixed-layout)** — `graph-compose-render-pptx`,
  `com.demcha.compose.document.backend.fixed.pptx`. Consumes the same
  resolved `LayoutGraph`; geometry-identical to PDF by construction, since
  layout is compiled in core before any backend runs.
- **DOCX** — `graph-compose-render-docx`,
  `DocxSemanticBackend`. Walks the semantic node tree and writes editable
  Word content, which Word paginates and re-flows after an edit. It reads
  the resolved layout, when it has one, for what Word cannot work out
  itself: how tall a line is, how wide a column came out, where a drawing
  stands (see [Measured geometry](../recipes/docx-export.md#measured-geometry));
  a document it writes without one, its report names (`measured geometry`).
  What it cannot carry it names in a `DocxExportReport` — every subject
  is listed under [DOCX export report subjects](#docx-export-report-subjects).

Reading the DOCX entries: a row is `n/a` only when the capability is
declared on the fixed-layout SPI and can never reach a semantic backend —
`SemanticBackend` carries `name()`, `requiresResolvedLayout()`,
`export(DocumentGraph, SemanticExportContext)` and
`exportSections(List<SemanticSection>)`, so `renderToImages`, the
raster-slide builder option and the debug overlays, which never reach a
`SemanticExportContext`, are not questions it can be asked. An output
option that does reach it but that it does not write — a watermark, a
protection, viewer preferences — is ❌, and the export report names it.
Drawing
nodes are a different case: `ShapeNode`, `LineNode`, `EllipseNode`,
`PolygonNode` and `PathNode` all reach
`DocxSemanticBackend.writeNode`. A horizontal `LineNode`, or a `ShapeNode` built as a
divider in the flow that is only a fill — no stroke or gradient, square corners, no
transform — and no taller than Word's thickest border, is written as a rule, and a
`ShapeNode` written so is named in the
report (a box in the tree is a line in the file) while a `LineNode` is not; any other rectangle, rounded rectangle,
ellipse, line, polygon or path is drawn — or, where it cannot be, dropped with a
note — from the fragments the layout placed it as, a floating DrawingML shape behind the text, anchored in a paragraph the nearest block holding it writes on its page — the one whose line it stands level with or below that places it at the smallest offset, the paragraph nearest it where that block writes none, the paragraph of the cell whose box holds it, by that same order, for what a composed cell draws — or else to the page (`DocxDrawings`, `DocxDrawingAnchors`,
polygons and paths as custom geometry). Three writers offer a paragraph to be
anchored in: a paragraph node's own, an item of a list the layout's items are
matched to, and the one line the parts of a band share. A table's own cell text
offers none — its cell states no width for a shape to be placed from — so a
shape whose block's text is written only so keeps to the text nearest it. A
shared line is offered from its text's own start, which its indent is written
from, and reaches across to the end of the box its other half is set in; one
the layout starts left of its cell's text column offers none, its indent being
held at nought there.
Inside a filled panel a shape stands in
front of the text unless it frames text; anchored in a paragraph of the panel's
cell, it is placed from the cell's text column, as Word lays a shape anchored in a
table cell out inside the cell. A filled or outlined
rectangle, rounded rectangle or ellipse whose one centred layer is a line of at
most four characters in one style, left to right and unlinked, is one shape
holding that text. A shape or a path painted only with a gradient, with no outline, shows nothing the export carries: it is dropped and named in the report (`DROPPED`) — in a table cell as anywhere — with one logged warning per node kind; one with an outline that a table cell composes is its table's drawing, and the table's `cell drawing` note lists a gradient among what it does not carry.
A `BarcodeNode` reaches it too and is written as a picture.

The PPTX *semantic* skeleton (`PptxSemanticBackend`, slide-safe node
validation + manifest, writes no file) is out of scope for the matrix and this file.

## Fragment payloads (fixed-layout drawing surface)

Payload records live in `core` under
`com.demcha.compose.document.layout.payloads`.

### Paragraph

Paragraph — pre-wrapped lines, runs, alignment (`ParagraphFragmentPayload`)

**PDF.** ✅ `PdfParagraphFragmentRenderHandler`

**PPTX.** ✅ `PptxParagraphFragmentRenderHandler` (one absolute, wrap-disabled frame per measured line)

**DOCX.** ⚠️ semantic paragraphs (`DocxSemanticBackend`) — lines written at the height the page measured, as a least height Word grows with the text where they are at least as tall as their faces' own line and Word then stands the text where the page does (`DocxGrownLines`: a least line's baseline its runs' descent above its foot), exact elsewhere, which the report names once for the document (`line height`); each run keeps its own style, falling back to the paragraph's when it has none; a hyphen between two letters or digits in the first word of each of the page's lines after a paragraph's or a list item's first is written as Word's non-breaking hyphen (`DocxLineStartHyphens`), because Word and LibreOffice break after a hyphen and the page does not, so both carry the word whole to the next line as the page does (a paragraph whose runs do not hold its lines' letters, a list item not matched to its laid-out lines and a paragraph in a text box over the flow are written as before, and a break after an en dash is not held); a character the page draws nothing for (Unicode category C but the direction marks and joiners) is not written; a margin below zero sets a paragraph's, a list's or a block's text out past that side in the flow as the page does, in a table cell no further out than the cell's text, what is held back reported; a top edge below zero comes out of the space owed above the paragraph, the foot of the one line above it and the top of its own line, and what none gives is not written and is reported (`space above`), the paragraph and what follows that much lower in the flow, and in a table cell up to that much, its row as much taller where the cell sets the row's height — not yet on the first block of a band's layer or of a row of columns' later layer, on a block opening a page in the flow (a section's first, one moved to a new page, one after a page break), or on a side of an overlay's line pair, which takes its pull out of the space above alone; a centred or right-aligned left-to-right line of its own, of text alone and untracked, that Word sets a point or more wider or narrower at its half-point size has its letters spaced by the difference (`w:spacing`) and its room reckoned from the page's width; a `linkTarget` becomes a `w:hyperlink`, with a relationship for an address or `w:anchor` for one of the document's own anchors, and a run's own link wins over the paragraph's; a paragraph seated off its baseline (`TextVerticalAlign`) has its runs raised or lowered in the line (`w:position`) by the PDF backend's own correction (`ParagraphSeating`), one shift for the paragraph where the page seats each line by its own, and a badge's initials written as its shape's text stand off the shape's middle by as much, in its insets: in Word, and in LibreOffice where the line and twice the seat fit the preset's text rectangle (elsewhere LibreOffice, which reads an inset below zero as none, moves them only by half the inset it reads, short of the seat); Word and LibreOffice stand an exact line's baseline four fifths of the way down it whatever the face, where the page sets it the face's ascent down, matched at a paragraph's middle line: a line of its own Word sets a tenth of a point or more low is moved up into the space written above it and that space owed below it — a line pair's shared line by the lesser of its halves' calls where neither is moved by its position — in the flow, in cells, in a stack of one layer, which is the flow, and in a band, which takes back the raise of its last line before its next layer and below itself, not in another overlay nor a band's first paragraph or item where a band in the flow opens its page, a paragraph the layout moves to a new page or breaks over one, nor the space a panel's top border takes above its first line; a line opening a cell, under half a point low, takes what that space cannot give from the cell's top padding — a panel's, its row held as much taller, and a table's composed cell's in a row whose margins even — and a row of columns' first row is lifted out of the space above its table by the most its cells' first lines call for, what follows kept in place (not a row the layout moves to a new page, held to a height, or whose column opens with a panel or a table); a paragraph in a composed table cell of a placed row is raised as anywhere, its row kept on one page; a line of its own Word sets a tenth of a point or more, and under half a point, high is lowered by what is left of the space between it and what follows once the block under it has taken its own of it — its line gap, its raise, its pictures' room, a row's lift out of the space above its table — above that block, or written below the line before a table or at a cell's end, nothing under it moved (not where the next block is on a later page or in an overlay, nor at a page break, a block moved to the next page or a column's later layer, and where the section's end leaves a cell's space below out, the lowering it gave is taken back with it; not a line cut to fit, opening its page or a panel, not on one page, or in an overlay other than a stack of one layer, nor a list's item, lowered by its own list); what that space cannot give, and a line Word sets half a point or more high, is moved in the same position where the two are half a point or more apart — an exact Poppins line's; never a least line's, which holds no position — which LibreOffice moves further by the face's height over its em (a list's item seated as a paragraph is — not raised into the space above where the layout opens a page with it or sets its list over two pages in a cell, an item whose marker Word draws not moved by its position, since LibreOffice does not move that marker by its mark's, but lowered, where Word sets it high, by the space above it, as far as the items under it and what follows the list give that back, and taken back where the hang below the list is let go, and one the layout's lines are not matched to not seated (a list composed in a table cell is matched to the run of its table's lines holding its items as the page lays them out, opening at a line no node before it took, and takes their height, lineSpacing, `hangingIndent` marker column and seat as one in the flow does, an item in a row Word keeps whole rising into the gap above it — a Word list whose marker stands in its text keeping its stated column, having no place of its own; one holding an item of runs, an item whose markdown marks the page drops, a flagged list's blank item or an item run onto the next page is not matched); a table text cell's by its row's margins, see Table rows; a picture among it moves with it in Word and stays on its own baseline in LibreOffice); lines a container stacks over one another tighter than their face each end halfway between their letters and the next line's (Word draws an exact line's text on screen only inside the line; its PDF export does not cut it), and the last layer of a shape container on one page, where its line runs past the foot, ends at the foot or below its letters; letters two lines share are split halfway so the page does not move, and a stack that holds a picture keeps its lines' own heights; a `bulletOffset` of spaces becomes the paragraph's indent (`w:ind` left, hanging or first line, by `indentStrategy`) in the flow and in cells, not yet over the flow, in an overlay's left-and-right pair, as a badge's initials or in a header or footer; one with letters in it is not written, its wrapped lines still set after the spaces that cover it; an auto-sized paragraph's text that takes its style is written at the size the page fits it to, read off the laid-out lines, a run with a style of its own keeping it, on every path that writes a paragraph — at its style's size where the lines do not tell the fitted size (no lines read, lines in sizes that do not say which is the paragraph's, or one paragraph added at more than one place); a paragraph a session reads as markdown — the default, unless `markdown(false)` — is written as the page sets it, read through the page's own parser, one run a piece in the face, family, colour, tracking and size the page's laid-out lines hold (an auto-sized one's read at the size the page fits it to, a heading at its multiple of it), its marks dropped, wherever the lines hold the pieces' letters so; where they are not read, or hold other letters or none (text the parser reads into nothing, which the page sets as nothing), as authored, its marks as letters; a `bookmark(...)` is Word's `HeadingN`, which Word's outline lists by the text of its Word paragraph — an overlay's pair's whole line, one level for both sides — at no level past the ninth. Outside a header or footer, the paragraph's report note (`ParagraphNode`) names each of these where it moves or renames something: the prefix's letters, and the room a path that writes no prefix leaves out where it moves a line; the size an auto-sized paragraph's text is written at, where the size the page fits it to is not measured; the marks of a paragraph the page read as markdown and the file holds as letters, where its laid-out lines hold fewer of them than its text, not measured where its lines are not read; a markdown heading written taller than the line the page sets it in, which Word cuts on screen; the lines of a paragraph seated off its baseline that take another seat than its first, from half a point; a picture on a later line, set by the first line's measure, from half a point; pictures beside text standing on the baseline where the lines are not read; an outline title that is not the text Word lists, a level past the ninth that shares it with another, and the right side's entry where the left holds the line's level

### List hanging indent

List hanging indent — a marker column and a content column (`ListBuilder.hangingIndent(true)`, `markerGap(...)`)

**PDF.** ✅ marker and content emitted as separate `ParagraphFragmentPayload` fragments at the resolved `markerX` / `contentX`

**PPTX.** ✅ the same fragments — the fixed-layout pipeline resolves the geometry before either backend sees it

**DOCX.** ⚠️ the top level only. `DocxSemanticBackend` exports a list as a real Word list — `numbering.xml`, `w:numPr` per item, the level carrying the marker — or, with rich items, a drawn marker or a marker holding a percent sign before a digit, which Word reads in a level's text as a level's number, as paragraphs; content and nesting are unaffected. With the flag, the top level's marker column is the layout's — the marker's width and `markerGap`, the text and its wrapped lines where the page sets them — where the gap covers what Word may set the marker wider: a picture at its written size, its edges included, or text in the page's face (embedded, or a standard one Word sets in the same widths) grown to its half-point size, half a point clear. A Word list's level then indents and hangs by that column; a list of paragraphs writes the marker, a tab to a stop there, and hangs the item there. Word places content at absolute indents and has no relative-advance primitive, so without the layout's measure the gap could not be honoured; a Word list without the flag that the layout placed and that does not nest takes the page's column too, the spaces the page sets its wrapped lines after, its marker followed by a space (`w:suff`) and an item that wraps measured at Word's half-point size; a list that nests items, a list built as a tree of items (laid out flattened), and a marker the gap does not clear keep the stated column (180 twips, plus 120 per nesting level) — except, in a list of paragraphs, a nested rich item with no marker, which stands where the layout set its text, its measure weighed at Word's half-point sizes, where the layout's items are matched to the list's; a list that nests only such items sets its top level at the page's column too. The report counts, on the list, the items that stand at a stated column, a space past their marker or two spaces a level in, and names a centred or right-aligned list written flush left, a lineSpacing not written where the layout's items are not the list's own and one wraps (in a list composed in a table cell that its table's lines are not matched to, its wrapping not measured), a continuationIndent not written where an item of a markerless list or a tree of items without the flag wraps or its wrapping is not measured, the rows the page draws as a marker alone for blank items of a flagged list, which are not written, and what items the page reads as markdown lose. An item of plain text the page reads as markdown — the default, unless `markdown(false)` — is written as the page sets it, matched to the lines the page laid it out in and read as the page lays it out (a flat item after the marker the page sets before it; a flagged item as its text alone; an item of a list built as a tree of items without the flag after the indent and marker the page reads with it), one run a piece in the face, family, colour, tracking and size those lines hold, Word or the file drawing the marker where it did (Word draws a tree's bullets regular, as the parser sets the marker it reads), the font table shipping the faces the page sets the pieces in; it is written as authored, its marks as letters, where its list's items are not matched one by one to the layout's (no layout, composed in a table cell and not matched to its table's lines, an item run onto the next page, a flagged list with a blank item), where its lines hold other letters, where the parser reads a tree's marker as markdown with the item (`*a*`), and where the parser reads it into nothing, and named where the page drops a mark from it or sets none of its text — not where it changes only its face or letters no mark is made of (`1.`); a markdown heading written taller than its item's line is named

### Inline code/badge chips

Inline code/badge chips (`InlineBackground` on text spans)

**PDF.** ✅ `PdfParagraphFragmentRenderHandler`

**PPTX.** ✅ `PptxParagraphFragmentRenderHandler`

**DOCX.** ⚠️ `DocxSemanticBackend` — the fill becomes the run's own `w:shd`, in a paragraph and in a list item alike, so a badge still reads as a badge. What Word has no way to say is the shape: shading covers the glyph box, so the corner radius, the padding above and below the letters and a padding below zero on any side are not in the file, and the export records them. The padding beside the letters is written as the room it takes (`spaceAfterTheLastLetter`): character spacing after the chip's last letter, shaded with it, and after the letter before the chip — unshaded, or in the fill of a chip right before it. A chip opening its paragraph or following a picture has no letter before it, so its left padding is not in the file; the letter before a chip is read from the paragraph's runs, not its lines, so a chip the page wraps to the start of a line has its left padding after the last letter of the line above. No space is written after right-to-left letters or after a symbol or emoji. The export records, chip by chip, how each side was written. LibreOffice sets no spacing after a line's last letter, so it does not apply the right padding of a chip that ends a line. Word draws nothing for the spaces closing a paragraph, their shading included, and does not count them in a centred or right-aligned line, so a chip's spaces there are written as no-break spaces, which Word fills and counts in its line (`DocxClosingSpaces`); LibreOffice 24.2 draws neither kind, its fill ending at the letters and such a line standing right of the page's, and the chip's note says so. A `w:shd` fill is opaque, so a translucent chip is flattened first against what Word paints underneath it — the paragraph's shading, the cell's, or else the colour the page paints under the paragraph, a page background included — so the chip agrees with the file it is in and shows the colour the PDF shows. It stops being translucent, and that is recorded with the rest

### Inline images

Inline images (`ParagraphImageSpan`)

**PDF.** ✅ `PdfParagraphFragmentRenderHandler`

**PPTX.** ✅ `PptxParagraphFragmentRenderHandler`

**DOCX.** ⚠️ `DocxSemanticBackend.writeInlinePicture` (a picture in its own run where it sits among the words, at its size, inside the run's or the paragraph's link; raised or lowered by `w:position` to where the page's alignment and `baselineOffset` put it, from the layout's measure of the paragraph's first line — in a list, the list's text on a line as tall as the item's own tallest picture; a picture on a later line of another height is set by that measure too, and where the paragraph's lines are not read, nothing moves a picture beside text off the baseline: a paragraph's note names either, not a list item's; LibreOffice ignores `w:position` on a picture and stands it on the baseline, so a picture the export draws itself (icon, emoji, shape) that the page raises carries the rise as transparent rows and needs no `w:position`, while one the page lowers stands in LibreOffice higher than on the page by as much as the page lowers it — up to the text's descent for a centred icon as tall as its line; the editor clips a picture to an exact line height, so a paragraph holding a picture that leaves its text — past the ascent or the descent, in Word's placement or on the baseline — has its lines written at least the height the picture reaches, grown by the editor rather than clipped, every line of the paragraph since Word has one line height for it, and each as tall as the editor's font makes it — for 14pt text about 2.5pt taller than the page's in LibreOffice; a picture inside its text in both editors keeps the exact height; a paragraph of one line of text in a Word paragraph of its own, with room above for its pictures' reach, keeps an exact line at the page's height of it, the pictures set in it where the page puts them in Word and what their ink reaches past it taken from the gaps around it, and in LibreOffice a lowered picture there stands higher and loses what passes the line's top; its description is the text it stands for or empty)

### Inline vector shapes

Inline vector shapes (`ParagraphShapeSpan`)

**PDF.** ✅ `PdfParagraphFragmentRenderHandler`

**PPTX.** ⚠️ `PptxParagraphFragmentRenderHandler` + `PptxInlineGeometry` (distinct per-corner radii render with the top-left radius — single-adjust preset)

**DOCX.** ⚠️ `DocxSemanticBackend.writeInlinePicture` + `DocxShapePictures` (a transparent PNG drawn by the shared `InlineSvgRasters` from the outline, fill and stroke — every outline kind, each layer centred in the run's box — placed as an inline picture is; the picture takes as far as the stroked ink reaches past the outline — half the stroke on an edge, more at a sharp corner's miter — and a pixel on each side, measured side by side, and is lowered by what it takes below, so no edge is cut and a shape takes that much more room in the line, which the text beside it gives back in whole tenths of a point a letter where it has letters on one line (`giveBackWidth`) — where none does, the shape's note says, from half a point, up to how far that moves it and what stands beside it in its line, as Word sets the line from the side its alignment names, the paragraph taken as one line; what the tenths miss, up to a twentieth of a point a letter and all of it in a run too long for a tenth a letter, is not named, a quarter point at most on the corpus; a list marker that draws a disc is its picture; at the top level of a `hangingIndent(true)` list that does not nest it is followed by a tab to where the layout starts the item's text, the item's lines hanging there, when the picture clears that stop, else by a space; Word could hold a shape in the line, `wp:inline` round a `wps:wsp`, which the export does not write, so each shape among the words is reported `APPROXIMATED` as a picture)

### Inline SVG

Inline SVG (`ParagraphSvgSpan`)

**PDF.** ✅ `PdfParagraphFragmentRenderHandler` + `PdfPathPainter`

**PPTX.** ⚠️ `PptxParagraphFragmentRenderHandler` + `PptxInlineGeometry` + `PptxInlineSvgRasterizer` (simple layers stay native; arbitrary clips, exact dash/cap/join styles, and off-viewBox art use a transparent PNG fallback — drawn by the shared `InlineSvgRasters`; gradient paints use their primary colour)

**DOCX.** ⚠️ `DocxSemanticBackend.writeInlinePicture` (always the transparent PNG: the same layers the layout resolves, through `InlineSvgLayers`, drawn by the raster the PPTX fallback uses — `InlineSvgRasters`, four pixels a point — and placed as an inline picture is; emoji included, so an emoji is a picture rather than a character; every icon is reported `APPROXIMATED` as a picture, an icon's text as its description, and a gradient, which the raster draws flat in its first colour)

### Text an inline icon stands for

Text an inline icon stands for — copy, search, extraction (`ParagraphSvgSpan.text`, set by `SvgIcon.withText` and on every `EmojiLibrary` emoji)

**PDF.** ✅ `PdfTextLayer` via `PdfRenderEnvironment.writeTextLayer`: one invisible glyph over the icon on the line's baseline, from a Type 3 font of empty glyphs whose `ToUnicode` states each text, a whole ZWJ sequence included; rendering mode 3, so nothing is painted. One font per document, a new one after 255 distinct texts; a text over 256 UTF-16 units is not written. A block icon (`addSvgIcon`, `SvgIcon.node`) writes no text. `ActualText` around the paths was measured to reach none of PDFBox, poppler, pdf.js and MuPDF — it replaces glyphs, and a drawing has none. In a right-to-left line the glyph sits between the words it was written between and states its whole text; reading such a line back, PDFBox reverses the emoji one UTF-16 unit at a time and poppler reverses the code points of a ZWJ sequence or a U+FE0F pair, while pdf.js and MuPDF keep it whole (measured; a reader-side reversal of the glyph's text)

**PPTX.** ❌ the icon is drawn and its text is not written

**DOCX.** ⚠️ the icon is a picture whose description (`docPr/@descr`) is its text — read by a screen reader, but not a character a reader copies or searches

### Rectangle shape

Rectangle shape — fill, stroke, per-corner radii, side borders (`ShapeFragmentPayload`)

**PDF.** ✅ `PdfShapeFragmentRenderHandler`

**PPTX.** ⚠️ `PptxShapeFragmentRenderHandler` (distinct per-corner radii render with the top-left radius on all corners — single-adjust `roundRect` preset — until custom geometry lands; uniform radii and side borders exact)

**DOCX.** ⚠️ `DocxSemanticBackend.writeContainerChildren` — a `SectionNode` or `ContainerNode` carrying a fill, per-side borders or a uniform stroke is written as a one-cell table, and so is a `RowNode` in the flow that paints its box (`writePaintedRow`, through the same `writePanelPiece`), its columns a table nested in the cell without its padding — not in a band of layers or a stack's measured column, with a padding or margin it cannot hold, composed in a table cell or in a page zone, where it is its columns alone and its paint is reported: the cell's shading is the fill behind everything inside, its borders are the container's at its full height, and its margins are the padding on all four sides, less half of each border, with the table half a border wider on each side, so the text and the border land where the page draws them — in a cell, a panel set in from the cell's text by half its left border or more keeps its whole left padding as its left margin, and one set in less what it is set in by of that half, as both editors set a nested table's text its left border's centre and its margin in — in the body, the left margin is the whole padding, the table no wider on that side, and its indent places its text, as Word 16 and LibreOffice on Windows centre a body table's left border on its edge (an older LibreOffice, the Linux CI's, reads the indent as the table's edge and sets the text about a padding right). Word sets a cell's content its whole top border and its margin inside the row, and ends the row its margin and its whole bottom border past the content, so half of each border — and as much of its inner half as the padding falls short of it — reaches past the panel: the space round it takes that first, then its padding, the border then drawn that much inside the panel's edge, then, above, the room over its first line; what none of them takes is reported (`space above` on the block below, the panel's kind for its top border). What its content leaves below itself past where the page ends it — a last line held to an icon, a card's border ending it, which that card's padding takes first — comes out of its padding below first, the same way; a card's border ending a row's cell reaches past the row only by what the row's room under that cell does not hold, and one ending a table's composed cell is reported on the card, its row up to that much taller. A negative bottom margin pulls what follows up out of the space between them, and what that space cannot give is reported, as is the tenth of a point of the paragraph between any two tables where the space between them does not take it. A shape container's borders stay inside its outline. A panel the layout measures holds the page's height less its padding as its row's height, which Word and LibreOffice on Windows read as its content's, its margins and borders round it; a shape container holds its outline's, less its margins and its borders as wide as Word draws them, two and a half twips an eighth of a point rounded down. A panel's borders are written from two to ninety-six eighths of a point, as a table cell's rules and a rule are, and a width that, rounded to the eighth, is thinner or wider is named (`border width`). The table takes the width the layout placed the container at. The point of slack an editor's face is given past it comes out of the panel's right margin, so its fill and borders end where the page ends them; where the margin cannot hold it, or a line Word sets at its size to the half point runs further into the margin than the rest holds, the table keeps that much of the slack and the report names it (`right end`). In a cell, a panel whose left border hangs half its width left of the cell's text — or which a margin below zero sets further out — stands up to that much right of the page's, as LibreOffice and Word start a nested table at the cell's text, which the report names (`left edge`); it gives up what its left side had given its text, the half border the table is wider by and what its left margin gave back of the other half, as both editors start it half that border in, so its right end stands where the page's does, and a nested table running past its cell, which widens the cell, fixed layout or not, runs past no further — what more it gives up held in at the cell's edge than at its text coming off its right margin, what holding its right border in has already taken counting towards it, and what of it the cell does not take going back to a line set past its text further than its margin holds, reported (`right end`); one whose right border reaches the cell's edge — the cell's own right margin holds what passes its text, where the cell draws no right rule of its own that may cover it — ends that border and half a point short of it, the points taken from its right margin and no more than it holds, since Word draws a nested table's right border outside the table and, on screen, cuts off what passes the cell's edge, and the report names how much narrower than the page draws the panel its table is written, and about how far what the margin cannot take in still passes that edge (`right border`). A `keepTogether()` panel the layout held on one page is a row that may not split. Panels, rows and tables inside it are tables in its cell; a table cell no style fills is written white, as the page draws it. The corner radius is dropped (a cell is rectangular), named on each panel in the report (`corner radius`), with one logged warning per export. A translucent fill is flattened against what Word paints under the panel: the panel or cell around it as written, or on the page the colour the layout paints there (`DocxLayoutMetrics.colourUnder`: the fills drawn before it, at its centre, a page background included and a row's own fill left out, what stands on it being written in the row's panel, on its shading). A translucent border is flattened against the panel's fill where it has one, since a cell's shading and borders are opaque; each is named in the report as `translucency`. An unpainted container is not a table: its paragraphs are indented by every enclosing margin and padding, and its rows and tables get `w:tblInd`. A standalone `ShapeNode` built as a divider (`ShapeNode.divider()`, which the DSL's divider builder and the templates module's `Divider` widget set), in the flow — outside every layer stack, a stack of one layer included, canvas and shape container —, that is otherwise only a fill, with no stroke, radius, gradient or transform, and no taller than Word's thickest border (12pt), is a rule, written as a line's is; a box of a colour that did not ask to be a divider goes to the drawing path however thin it is — drawn where the page puts it, or dropped with a note where the export cannot draw it — and so does any other `ShapeNode`, standalone or laid over others, by `DocxDrawings` as a floating DrawingML shape behind the text, where the layout placed it — `rect`, or `roundRect` with the largest corner radius, in its fill and outline colours with their alpha, the outline mitred at its corners as the page strokes it, each side border a filled bar the stroke's width across, centred on its edge, as a stroke with flat ends covers it; a gradient paint and a transform are not carried, nor are a link, an outline entry or an anchor's bookmark — each named in the shape's report note, as unequal corners are —, and a shape with neither a fill colour nor an outline is dropped and reported. It is anchored in a paragraph the nearest block holding it writes on its page — the one whose line it stands level with or below that places it at the smallest offset, usually the last of them, which where that block writes two columns may be one of the other column's, placing the shape out of that column; in a table cell its own cell of that paragraph's row where one holds it across, else the paragraph's cell placing it out of its column (one holding text, a text box or a badge's letters, only where it holds it across), none in a cell set to the middle or the bottom or sitting in one, whose text LibreOffice 24.2 then sets from the top — placed down from its top however far off, so it moves with that block when the text above is edited; where no such block writes one, in the paragraph nearest it on its page; what a composed cell draws, in the paragraph able to carry it of the cell whose box the layout placed it in — that cell standing for the block holding it — by that same order, the carrying cell written to be read from its top where it holds no text (a cell set to the middle or the bottom takes one only where the page fills its box, which is then excused its own alignment whatever it holds while a cell inside it must hold nothing an editor would set lower; a repeated header row and a cell composed of a node placed more than once take none, and what they refuse keeps the nearest-text rule), and in the paragraph whose text is nearest it within reach where no cell's box holds it; with none of these, in a body paragraph on its page (a table cell's only on a page with no other, reported), where it stays — except a drawing that is all a table cell holds, an icon beside its label (in a row of the flow, a layer stack or shape container that only draws, with no margins, on one page and painting nothing outside its box; in a composed cell, a layer stack of shapes with no margin or padding, the only drawing the cell holds, matched to the table's fragments inside the cell by kind and size), which is anchored in that cell's paragraph, held at its height and placed from its top and the cell's text column, so it moves with its row, and down with the paragraph's line where space is added above that line once it is placed, as a lifted row's cell starts lower; a drawing holding a line is not held at its height, and goes by its block —, and is drawn in front of the text inside a filled panel unless it frames text or a picture there; reported `APPROXIMATED`

### Ellipse

Ellipse (`EllipseFragmentPayload`)

**PDF.** ✅ `PdfEllipseFragmentRenderHandler`

**PPTX.** ✅ `PptxEllipseFragmentRenderHandler`

**DOCX.** ⚠️ `DocxDrawings` — an `ellipse` shape anchored as a rectangle is; a shape container's elliptical outline is drawn the same way, and a picture that fills the container it clips takes the ellipse as its geometry; a transform is not carried, nor a link, an outline entry or an anchor's bookmark (each named in the ellipse's report note), and a shape in a filled panel is drawn in front of the text, over the cell's shading, unless it frames text or a picture; a shape is anchored as a rectangle is — except, as for a rectangle, a drawing that is all a table cell holds — in a row of the flow, a badge alone beside its text — which is anchored in that cell and moves with its row

### Line

Line — dash pattern, line cap (`LineFragmentPayload`)

**PDF.** ✅ `PdfLineFragmentRenderHandler`

**PPTX.** ⚠️ `PptxLineFragmentRenderHandler` (numeric dash arrays map to the generic dashed preset; solid lines and caps exact)

**DOCX.** ⚠️ `DocxSemanticBackend.writeRule` — a horizontal line with no transform, stroked no thicker than Word's 12pt border, is Word's own rule: an empty paragraph whose bottom border is the stroke (colour, thickness in eighths of a point, a hairline thinner than Word's thinnest drawn at it and reported, `border width`), its ends as the paragraph's indents, each taken in by what Word draws a paragraph's border past it — 1.42pt at the left and 1.51pt at the right, measured 1.36 to 1.59; a rule running past the width it is written in held at that width's edge, less only what Word draws further than the page ends it — so LibreOffice, which draws it between them, sets each end that much further in, and a rule shorter than both keeps a tenth of a point of its paragraph, and the space above and below the stroke in its box as the paragraph's height and the space owed below it, and what Word's paragraph stands taller than the box — the tenth of a point of line a stroke with no room above it is held to, and the border counted in whole twips of its eighths — taken out of the space owed below and, past that, hung below the rule for the space above what follows to take, as a line hanging below a block is; a dash pattern becomes Word's dashed or dotted border, reported `APPROXIMATED`; a translucent stroke is flattened against the colour the page paints under it, a page background included, reported as `translucency`; the line cap and a link are not carried (both reported, a link as `rule link`; an outline entry on it is reported too); composed in a table's cell, it is written once, where the page draws it, and its table does not draw it again — unless it stands in a panel or a row inside the cell, or another drawing of its kind and size there cannot be told from it, where both stay and the report says it stands twice. A thicker line (holding its room in the flow and among the text of a stack of one layer, none in a table's cell, reported), a vertical or slanted line, and a line laid over others in a layer stack, canvas or shape container — a line among the text of a layer stack of one layer excepted, which is a rule —, is drawn by `DocxDrawings` as a `line` shape anchored as a rectangle is, ending in the page's cap and dashed as the page dashes it (`a:custDash`, running from the end the page starts it at, a round or square dash written as long as it shows with its caps; LibreOffice draws a square dash an outline's width longer and its gap as much shorter, and keeps at most two dash lengths and one gap length of a pattern) — a transform is not carried, nor a link, an outline entry or an anchor's bookmark (each named in the line's report note); a line in a page zone is dropped and reported

### Polygon

Polygon (`PolygonFragmentPayload`)

**PDF.** ✅ `PdfPolygonFragmentRenderHandler`

**PPTX.** ✅ `PptxPolygonFragmentRenderHandler` + `PptxInlineGeometry`

**DOCX.** ⚠️ `DocxDrawings` + `DocxCustomGeometry` — `a:custGeom`, the vertex ring closed, its corners mitred as the page strokes them (LibreOffice bevels one sharper than 30 degrees), anchored as a rectangle is

### Free path

Free path — segments, dash, cap, join (`PathFragmentPayload`)

**PDF.** ✅ `PdfPathFragmentRenderHandler` + `PdfPathPainter`

**PPTX.** ⚠️ `PptxPathFragmentRenderHandler` + `PptxInlineGeometry` (numeric dash arrays map to the dashed preset)

**DOCX.** ⚠️ `DocxDrawings` + `DocxCustomGeometry` — `a:custGeom` through the same move/line/cubic/close segments, an unfilled path left open; the fill and stroke colours are carried, and the outline's cap and join as the page's — butt, round or square; mitred up to ten times the stroke's width, round or bevelled — which Word draws as the page does, and LibreOffice too but for a mitred corner sharper than 30 degrees, which it bevels — neither reads the miter limit, Word's own being the page's (written with neither cap nor join, both end an outline flat and round its corners); the dash pattern as a drawn line's, each part of the path from its own first point; a gradient paint is not carried, and a path's report note names it, composed in a table cell the table's note naming it; the SVG icons of a block (`addSvgIcon`) are drawn this way, one shape per layer, unclipped

### Linear gradient fill

Linear gradient fill (`DocumentPaint`)

**PDF.** ✅ `PdfShadingSupport`

**PPTX.** ✅ `PptxGradientFill` (native `gradFill`; explicit-axis endpoints approximate to the angle)

**DOCX.** ❌ no gradient is written, and each is named in the report: a shape or a path painted only with one, with no outline, is dropped (`DROPPED`, at its node's kind) — in a table cell as anywhere; one with an outline keeps the outline, and composed in a table cell its table's `cell drawing` note lists a gradient among what it does not carry, and elsewhere its own note names the fill it loses; an icon's gradient is drawn flat in its first colour in the icon's picture (`inline icon`), and a chart series' is written in its first colour (`chart`)

### Radial gradient fill

Radial gradient fill (`DocumentPaint`)

**PDF.** ✅ `PdfShadingSupport`

**PPTX.** ⚠️ `PptxGradientFill` (`circle` path shade — DrawingML cannot express radius-to-farthest-corner exactly)

**DOCX.** ❌ as the linear gradient

### Gradient strokes

Gradient strokes

**PDF.** ✅ `PdfPathPainter` (pattern stroking colour)

**PPTX.** ✅ `PptxGradientFill` (native `ln`/`gradFill`)

**DOCX.** ❌ a path's outline is drawn in its stroke's own solid colour, and its report note names the gradient paint; an icon's is drawn flat in its first colour (`inline icon`)

### Image

Image — STRETCH / CONTAIN / COVER fit (`ImageFragmentPayload`)

**PDF.** ✅ `PdfImageFragmentRenderHandler`

**PPTX.** ✅ `PptxImageFragmentRenderHandler` (COVER via the picture source crop)

**DOCX.** ⚠️ `DocxSemanticBackend.writeImage` (the box comes from `NodeDefinitionSupport.resolveImageDimensions`, the same rule layout applies to `width` / `height` / `scale` and the content-width clamp; CONTAIN is embedded at its fitted size, the room it leaves in its box held round it as the page centres it — half above and below as its paragraph's space, half at its side as its indent — COVER via the picture source crop as in PPTX, and the picture type is read from the bytes; a picture in the flow, a stack of one layer's included, that the layout moves to a new page keeps the space above it there, in a line kept with it, as a paragraph's is; its left margin and padding are not written, and a picture drawn beside its text is fitted to its box with its padding in it — each named in the report)

### Barcode / QR

Barcode / QR (`BarcodeFragmentPayload`)

**PDF.** ✅ `PdfBarcodeFragmentRenderHandler` (vector: the ZXing bit matrix filled as merged rectangles)

**PPTX.** ✅ `PptxBarcodeFragmentRenderHandler` (native freeforms: the same ZXing bit matrix as merged rectangles)

**DOCX.** ⚠️ `DocxSemanticBackend.writeBarcode` (a PNG picture of the same ZXing bit matrix through `BarcodeMatrices`, one pixel a cell, in the symbol's two colours with their alpha and at the node's size, its data as the picture's description; it scans, but its data is part of the picture rather than editable, reported `APPROXIMATED`, which also names a link, a transform or its left margin and padding as not carried; an `anchor` is a bookmark on its paragraph; in a page zone it is skipped)

### Chart

Chart — bar, line, pie and donut (`ChartNode`, drawn as `ShapeFragmentPayload`, `PolygonFragmentPayload`, `LineFragmentPayload`, `PathFragmentPayload`, `EllipseFragmentPayload` and `ParagraphFragmentPayload`)

**PDF.** ✅ compiled into ordinary primitives by `ChartDefinition`, drawn by their handlers

**PPTX.** ✅ same primitives, same handlers

**DOCX.** ⚠️ `DocxSemanticBackend.writeChart` + `DocxNativeCharts` (a Word chart part, `c:chartSpace`, inline at the size the page draws the chart, its numbers in an embedded workbook a reader edits through *Edit Data*: bars vertical/horizontal, grouped/stacked; lines straight/smoothed with markers, labels and area fill; pie and donut with start angle, hole, slice labels; series colours, the value axis's bounds, step and number format as the page computes them, legend side, text styles; the plot pinned where the page draws it — a pie's disc, the rectangle a bar or line chart's value grid spans — a donut's slice labels and the legend laid out where the page sets them, and its centre text a text box of the chart's own. Word lays the rest of the chart out itself, reported `APPROXIMATED`, which also names the room Word takes from the plot for the axes' labels, Word's legend keys and spacing (a legend row ends short of the page's, its last entry up to 47pt left), a legend at the top set lower, a pie's legend at the foot LibreOffice sets higher, a legend beside a stack of columns listed top down, LibreOffice's donut labels nearer the hole, a monotone curve written straight, a smoothed line over an area filled straight, an area's missing value LibreOffice fills, stack totals, rounded bars, bars past Word's widest gap, gradients, slice gaps, a counterclockwise pie, fixed decimals where whole and fractional numbers mix, and a number format Excel cannot hold. A chart beside other drawings in its table cell, or in a table composed in a cell, is its data table, drawn over as well)

### Table rows

Table rows — resolved cells, row/col spans, two-pass fill/border paint (`TableRowFragmentPayload`)

**PDF.** ✅ `PdfTableRowFragmentRenderHandler` + row grouping in `PdfFixedLayoutBackend`

**PPTX.** ✅ `PptxTableRowFragmentRenderHandler` + row grouping in `PptxFixedLayoutBackend` (positioned rectangles, edge lines, and text frames — never native PPTX tables, which re-lay-out content)

**DOCX.** ⚠️ `DocxSemanticBackend.writeTable` (a real Word table on the grid `TableGrid` resolves, its columns the layout's widths — where one is `auto` and the layout does not show them, composed in a table cell or with a column edge no row shows, the editor's, and reported (`TableNode`): `colSpan` maps to `w:gridSpan`, `rowSpan` to `w:vMerge`, and the cascaded `DocumentTableStyle` text style reaches the cell's runs; the cell's fill maps to `w:shd` and its stroke to `w:tcBorders` — the engine's default 1pt black rule where the table states none, not Word's thinner grid — its padding to `w:tcMar`, less above and below the room Word makes for the horizontal rules (half of a rule between two rows, the lower row's, to each; the rules above and below the table whole to their row) — a row where a cell of text its padding then leaves short of that room needs more than the page's height stands taller in Word, reported (`cell rule`) — each as wide as Word draws it, in eighths of a point (one outside Word's range reported, `border width`), and counted as both editors count it, two and a half twips an eighth rounded down (a rule between two rows shared in whole twips, the row above it taking half rounded down; a cell's top and bottom margins rounded to the twip together), a row held at the page's height less the most any of its cells takes, its margins and the rules it holds — between two others, the rest of its own above and half that of the cell under it; LibreOffice makes room at the edge between two rows for the heavier of their rules, where Word makes it for the lower row's, and under a row ruled heavier than it a row stands the difference lower there; a text cell whose lines are at least as tall as its face's own written at a least height, which stands its text where the page does and grows with it, its row's margins its padding and an exact cell beside it left to its position and its lines' heights; a row of exact text cells moved by its top and bottom margins together, from Word's seat four fifths of the way down an exact line onto the page's baselines, by the least its cells' lines call for and no further than the margins hold — not a row holding a composed cell, one the layout did not measure, one in a vertical merge or whose cells call for opposite ways, nor a least call under a tenth of a point — and what the margins leave of a cell's call, half a point and more, moved by its text's position, and less, from a tenth of a point, by the height of its lines, each written shorter and the space after it taller, or the other way as far as that space holds, which keeps the cell's content as tall — where every paragraph of the cell is one line, in a face whose letters the export reads, and its letters stay clear of the line's top and foot; a row's cells at the row's smallest top and bottom margins, since both editors give every cell the row's largest, the rest of each cell's padding as space above its first paragraph and below its last, down to the largest margin a cell opening with a table or in a vertical merge keeps; the cascaded `textAnchor` maps to `w:vAlign` on every cell — none written for the top, Word's own default — and to `w:jc` on a text cell's paragraph, with the engine's default — the vertical middle, on the left, or on the right for a right-to-left cell — and `DEFAULT` at the bottom left, as the renderer draws it; a composed cell is written by the same writers that write its node anywhere, so one built from an image, a list or a table carries it — a nested table is a real `w:tbl` taking the width of the column it sits in, less its own margins and padding, which is the column's rather than the one the page gives it, since the layout reports a composed cell's content under the owner's path; the paragraph Word requires after a nested table is hidden where it ends its cell holding nothing and no space; a translucent fill is flattened against the colour the page paints under the cell, and a translucent rule against the cell's fill where it has one, since `w:shd` and `w:tcBorders` are opaque, and the table's report note names it (`translucency`); Word re-paginates, so the export states where the layout breaks: every row the layout placed is `w:cantSplit`, `repeatHeader(n)` rows are `w:tblHeader` and keep with the row under them, and a row of blocks is kept whole the same way; the paragraph Word requires after a document's closing table is an ordinary one where the last page has room for two lines below it, so a reader can type below the table, and otherwise a point tall with its mark hidden, so it opens no blank page, reported `APPROXIMATED` since text typed at the end then goes into the table's last cell; a column sized to its content gives its text the editor's point out of its text cells' side margins, on the side their text does not lean on, so the column keeps the page's width, and what its cells do not hold — a composed cell's, padding too thin — widens it, reported `APPROXIMATED` (`right end`))

### Clip region open/close

Clip region open/close (`ShapeClipBegin/EndPayload`)

**PDF.** ✅ `PdfShapeClipBegin/EndRenderHandler` (CLIP_BOUNDS + CLIP_PATH)

**PPTX.** ✅ `PptxClipSafety` + raster fallback in `PptxFixedLayoutBackend` — a provably no-op clip (padded content that cannot be cut) skips the fallback entirely and stays native, editable shapes; a clip that can cut ink renders through the PDF backend into one transparent picture on the clip bounds (pixel-exact, not editable as shapes; run-level link hotspots are not emitted and custom fragment handlers do not apply inside the picture; `Builder.clipRasterFallback(false)` restores unclipped vectors + warning; the raster targets a 2048px long edge, clamped to between native size and 4x, so a region larger than that is rendered at native resolution rather than downscaled — which also means its transient memory grows with the clip instead of stopping at the target (a 3370pt A0-landscape region costs ~45MB while rendering, against ~17MB for anything up to 2048pt); a true vector clip is tracked in [#413](https://github.com/DemchaAV/GraphCompose/issues/413))

**DOCX.** ⚠️ inline fallback + one-time capability warning; the report names a clip that cuts what its layers paint, measured from the layout's fragments by `DocxClipInk` — a clip that cuts nothing (an icon inside its box, a disc's initials, a photo filling its circle) is not named; a picture that fills a container clipped to an ellipse takes the ellipse as its geometry, which both editors crop it to; a badge's glyph — a smaller picture in a painted container that clips it to its outline (`CLIP_PATH`) and holds nothing else but drawing — is drawn by `DocxDrawings` as a floating picture over the outline, where the layout places it, reported `APPROXIMATED` — inside a filled panel the badge and its glyph are drawn in front of the shading; an icon picture beside its text in an unpainted container or a layer stack is drawn the same way; a filled or outlined rectangle or rounded rectangle holding text, composed in a table cell, which has no place in the layout to be drawn at, or placed in the body, outside any cell, laid over nothing and given room in the flow, where its layers written in the flow ran past its outline, is written as a panel — a one-cell table in its fill and outline, its corners squared and reported, its row held at least the outline's height less its borders and margins, which keep inside it, so it grows with text an edit adds; placed in the body, its layers held in across where the layout places them in the outline, one set from its top — or taller than the outline, from its top — set at the cell's top and taking its top border out of the space above its first line, a clip that cuts what its layers paint past the outline's foot holding the row at exactly the outline's height less one border, which Word and LibreOffice cut its content at, square, and layers running past the foot unclipped growing it, each reported; composed in a cell, a one-line label the shape centres top to bottom on a line taller than the room Word leaves its content cut alike on both sides to that room, no closer to its letters than three quarters of a point, and seated where the page sets it, from the shape's top where the line is taller than its outline and Word sets it on the shape's middle, and a label it holds whole written at the line height that stands its text on the page's baseline, as far as its letters and the room allow, by a tenth of a point or more and not in a standard face the export does not embed — its row held and its label cut in whole twips rounded down, as the row the cell is in holds the page's height rounded once, and what its letters keep the label past that room taken out of the margins of the cell it is in, where it is all that cell holds and the cell all its row holds; the rest of what a composed cell draws (an icon, a tile, a disc) is the table's own drawing and is drawn by `drawCellDrawing` where the layout puts it, anchored as a rectangle is

### Timeline rail

Timeline rail — one logical connector line resolved from marker and entry anchors after layout (`ShapeFragmentPayload` per page)

**PDF.** ✅ `PdfShapeFragmentRenderHandler` — one fragment per page, spliced beneath the markers

**PPTX.** ✅ `PptxShapeFragmentRenderHandler` — same payload, same per-page fragments

**DOCX.** ⚠️ `DocxTimelineRails` — a timeline whose entries are each a row alone (every timeline the templates write) is written as one table, an entry a row and each gap a row of its exact height, its rail the left border of the cells it runs along, cut at a marker where the rail ends there: the rail stretches with the entries when one is edited, and nothing is reported. A timeline that cannot be written so — among others one any entry of which has a body below its row, whose rail stands beside the markers of a timeline set in a cell, is see-through, or would cut a numbered marker's figure; the DOCX recipe lists every case — keeps `DocxDrawings`' rail, read from the resolved layout's pass fragments and drawn per page as a filled bar the rail's width, anchored in the entry it starts in on that page, or in a block holding that entry (in the paragraph nearest it where none of them writes one on that page, or no entry holds it): it keeps the length the layout gives it on each page, and the report says so (`timeline rail`). The markers are the shapes they are, anchored as a rectangle is: beside an entry's text they move with it when the text above is edited

### Transform open/close

Transform open/close — rotate/scale about fragment centre (`TransformBegin/EndPayload`)

**PDF.** ✅ `PdfTransformBegin/EndRenderHandler`

**PPTX.** ✅ `PptxTransformBegin/EndRenderHandler` (group shape; rotation and centre-pivot scaling via the exterior/interior frame ratio)

**DOCX.** ⚠️ no rotation or scale is written: a turned shape, ellipse, line, picture, barcode or shape container stands upright at its size, and its report note names the transform — a shape container's in its drawn outline's note, in its panel's (`shape container`), or on its own where the outline draws nothing; a shape a table's cell composes, in its table's `cell drawing` note, which says a transform is not carried; a page zone's picture, in its `page zone` note. A shape container not written as a panel has its layers written inline, which one warning per export logs

### Anchor markers

Anchor markers (`AnchorMarkerPayload`)

**PDF.** ✅ `PdfAnchorMarkerRenderHandler` + `PdfInternalLinkWriter`

**PPTX.** ✅ `PptxAnchorMarkerRenderHandler` + `PptxNavigationWriter` (slide-jump hyperlinks resolved after all fragments, so forward references work)

**DOCX.** ⚠️ `DocxSemanticBackend` — an anchor becomes a `w:bookmarkStart` / `w:bookmarkEnd` pair wrapping the paragraph's text, named as Word requires (letters, digits and underscores, starting with a letter, 40 characters); two anchors that clean to one name, or to a name a suffix gave another, stay two bookmarks; an anchor two nodes declare — two cells of one table too — is marked on the last node the file writes a bookmark for, and reported; one in a table's header row the page repeats, which the page sends a link to on its last repeat, is marked on the one row the file holds, where the table starts, and reported (both `anchor`). An anchor the export writes no bookmark for is named on its node — see Internal links

### Bookmark markers

Bookmark markers (`BookmarkMarkerPayload`)

**PDF.** ✅ `PdfBookmarkMarkerRenderHandler` + `PdfBookmarkOutlineWriter`

**PPTX.** ⚠️ `PptxBookmarkMarkerRenderHandler` + `PptxNavigationWriter` (PPTX has no outline tree — the first bookmark on a page names its slide, further bookmarks on the same page are dropped with a debug note)

**DOCX.** ⚠️ `DocxSemanticBackend` — the stated outline level becomes Word's own `HeadingN` style, so the Navigation Pane, the outline view and a generated table of contents all see the document's structure. The style carries the outline level and no formatting, so the paragraph keeps the look its author gave it; only the levels the document uses are defined, and one past Word's nine is clamped to the ninth. The role is never inferred from type size. Word lists an entry by the text of its paragraph, so the node's report note names an entry whose title is not that text, an entry not written because its node is not written as a body paragraph — a drawing, a picture, a table, a page zone's paragraph —, and a level past the ninth where it shares the ninth with another

### Alpha / opacity

Alpha / opacity

**PDF.** ✅ `PdfAlphaSupport` (`PDExtendedGraphicsState` on every surface — shape fills/strokes, text runs, lines, side borders, table paint) but a `DocumentHeaderFooter` zone's text slots and separator, drawn opaque with a warning in the log (`render.pdf.header-footer.opaque`)

**PPTX.** ✅ native `<a:alpha>` via POI on every surface — fills, strokes, text runs, table paint

**DOCX.** ⚠️ `DocxTranslucency` — text keeps its alpha as Word's text fill (`w14:textFill` with a transparency, the last of the run's properties, its namespace marked `mc:Ignorable` on the part; `w:color` keeps the colour as authored, which LibreOffice reads with the fill's transparency; an opaque run or list marker under a translucent Normal style writes an opaque fill of its own), and a drawing's fill and outline, a page background and a picture keep theirs; a cell's shading, a border, a rule, a run's shading (a chip) and a text header's separator hold an opaque colour only, so a translucent one is flattened against what Word paints under it — inside a panel or cell the export shaded, that shading as written; on the page, the colour the layout paints there, a page background included and a row's own fill left out, what stands on it being written in the row's panel, or white where a picture, barcode, gradient or transformed fill is under it; a panel's border or a cell's rule against the block's own fill; white under a separator — and named in the report as `translucency` (a chip's on its `inline chip` note). Word saves translucent text into a PDF as drawing, with no text layer

### Text decorations

Text decorations — underline / strikethrough (`DocumentTextDecoration`)

**PDF.** ✅ `PdfTextDecorations` (em-proportional marks: underline −0.10 em, strikethrough +0.28 em, thickness 0.05 em)

**PPTX.** ✅ `PptxTextFrames.applyStyle` (PowerPoint draws its own marks — sub-point placement differences vs the PDF's constants)

**DOCX.** ✅ `DocxSemanticBackend.applyStyle` (underline maps to Word's single underline, strikethrough to `w:strike`)

### Writing direction

Writing direction — right-to-left paragraphs (`ParagraphBuilder.direction`, `TextDirection`)

**PDF.** ✅ `ParagraphWrapping` resolves the line with the Unicode Bidirectional Algorithm and `PdfParagraphFragmentRenderHandler` draws it reordered — the page is painted, so the engine owns the order

**PPTX.** ⚠️ `PptxParagraphFragmentRenderHandler` — a right-to-left line goes through **per-span absolute frames** rather than one flowing frame, each pinned where the layout put it, because a shared frame lets PowerPoint re-flow the runs and undo the resolved order. Every frame this handler emits — plain span and chip text alike — declares its direction (`rtl`), which is what puts a neutral on the correct side. A table cell declares it too, through the overload of `PptxTextFrames.singleRunBox` that takes a direction. A header/footer and a watermark still take the overload that declares nothing, so right-to-left text there shows the original defect. The deviation is that the line is not one editable paragraph, and that the text a reader copies out carries mirrored punctuation (see the mirroring row)

**DOCX.** ✅ `DocxSemanticBackend.applyParagraphProperties` writes `w:bidi` (resolving `AUTO` through the same `ParagraphDirection` the page used) and hands Word logical text for its own bidi engine, which orders and joins it. Every run of that paragraph also carries `w:rtl`: `w:bidi` settles which edge the line starts from, `w:rtl` settles how Word resolves the characters inside a run, and a run without it is handled as Latin — measured in Word, `(2026)` closing an Arabic line was drawn as `)2026(` with `w:bidi` alone. Hebrew was unaffected, so the defect needed Arabic, where digits after a letter resolve as an Arabic number. Alignment is mapped through the direction, because Word reads `w:jc`'s left/right as start/end **relative to the paragraph** — written physically, a flush-right right-to-left paragraph came out flush left. The indents that carry a container's margin and padding, and a rail timeline body's column, are mapped the same way (`applyDirection` swaps `w:ind` `left` and `right`): Word and LibreOffice both read them as start/end in a `w:bidi` paragraph, and `w:start`/`w:end` read the same — measured, a Hebrew paragraph in a section padded on the left ended short of the right margin by the padding in both. Size and weight are written to the complex-script twins (`w:szCs`, `w:bCs`, `w:iCs`) as well as the Latin ones, since Word takes Hebrew and Arabic from those. Column order in a right-to-left table is not mirrored: `w:tblPr/w:bidiVisual` is unwritten

### Arabic contextual shaping

Arabic contextual shaping — joined letter forms (`ArabicShaper`)

**PDF.** ✅ shaped into Presentation Forms-B before measurement, because `showText` walks the font `cmap` and never runs `GSUB`; a font carrying the letters but not the forms degrades to unjoined base letters rather than `?`

**PPTX.** ✅ base letters restored (`PptxParagraphFragmentRenderHandler` → `ArabicShaper.toBaseLetters`, joining controls kept) — PowerPoint shapes Arabic itself, and frozen forms would land in a file users search and copy from

**DOCX.** ✅ never shaped — Word receives the letters and shapes them itself

### Bidi mirroring

Bidi mirroring — paired punctuation in a reordered line (UAX #9 L4)

**PDF.** ✅ `BidiMirroring` swaps at the PDF seam, so a parenthesis faces what it encloses; the mirrored set is document punctuation rather than the whole Unicode table. An atomic chip that mixes directions is resolved level by level (`BidiVisualOrder`) rather than reversed whole, so its left-to-right interior keeps reading the way it was typed

**PPTX.** ⚠️ `PptxParagraphFragmentRenderHandler` + `BidiMirroring` — measured on PowerPoint, it places a neutral from the declared direction but does not go on to mirror it, so the swap is made before the text is handed over. **That measurement is the load-bearing assumption of this row**, not a property of the format: a consumer that does apply L4 (any ICU-backed viewer) mirrors the character a second time and draws issue [#545](https://github.com/DemchaAV/GraphCompose/issues/545) again, with the stored text wrong as well. Only in PowerPoint is the result correct. The swap is keyed to the run, and only a run that is **uniformly** right-to-left gets it (`BidiVisualOrder.mirrorRightToLeftLevels`): that is the one PowerPoint reverses for itself, so the glyph has to be swapped for it, while a run mixing levels comes out right from the text exactly as typed and pre-swapping it drew `)a > b(` on a slide. A chip is the span that can hold both, since one rounded fill means the wrapper cannot split it — so `renderChip` splits its **text** instead, one frame per directional run placed in visual order (`BidiVisualOrder.visualRuns`), leaving each frame single-level and PowerPoint nothing to re-resolve. Handed over whole it was re-resolved as a fragment without the line around it, and the deck disagreed with the PDF about where a chip's Latin sat. The runs are measured on the joined forms, the width the fill was sized with; measured unshaped, an Arabic chip's last run ended past its own background. The stored run holds the mirrored character where the swap applies, including `<` and `>`

**DOCX.** n/a — Word mirrors from the logical text it is given

### Text extraction fidelity of reordered/shaped text

Text extraction fidelity of reordered/shaped text

**PDF.** ✅ the font's `ToUnicode` map names the letters an author wrote rather than the shaped forms (`PdfShapedGlyphUnicode`, applied after subsetting and before encryption), and each reversed run carries its written text as `ActualText` (`PdfActualText`) so the reading order is in the file rather than re-derived; per-glyph text inside a section is reachable through the font's map rather than plain extraction

**PPTX.** ⚠️ the runs are stored as written except for mirrored punctuation, so copying a right-to-left line out of a slide yields swapped brackets; a chip's letters stay in logical order, its right-to-left-level punctuation mirrored (see the mirroring row)

**DOCX.** n/a — the runs are stored as written

### Font portability

Font portability — does a file carry the family it drew with

**PDF.** ✅ every embedded family is subset into the file (`PdfFontLoader`), so a reader needs nothing installed

**PPTX.** ✅ `PptxRenderEnvironment.embedBundledFontsUsed` + `PptxEmbeddedFont` — a registered family and a bundled one the deck drew with are both carried. Whole-font, no subsetting — the **bundled** path narrows that to the faces a run actually drew, so a five-script deck grows from 27 KB to about 1.4 MB rather than carrying weights no slide contains; a **registered** family is still taken at family granularity, all four faces. `PptxFixedLayoutBackend.Builder.embedBundledFonts(false)` declines the bundled half (a registered family travels regardless), and a declined or failed family is warned about by name and carried whole or not at all — a family one of whose faces cannot be read, or whose licence bits forbid embedding, leaves nothing of itself in the deck rather than half of it. A deck declares an embedded face under one of four style elements (`p:regular`, `p:bold`, `p:italic`, `p:boldItalic`) and holds one face in each, and which element a face lands under is not the slot the family registered it in: `XSLFFontInfo.addFacet` reads the weight and the italic flag out of the container and picks from those, so a face a caller put in the bold slot whose own `usWeightClass` is 400 or less reads back as the regular one. Two faces of a family that read back as the same element cannot both travel — a second face seated in a taken element overwrites the part the first wrote and keeps the first's declaration — so such a family is refused as an unreadable one is, with both faces and the element named. Before that it was silent and worse than a loss: the deck came out with one program that was the later face's bytes under the earlier face's declaration, PowerPoint 16.0 reported the family as embedded, and every run of it — the regular ones included — was drawn from the one program that survived, measured in an exported PDF whose subset carried the `glyf`, `hmtx`, `cmap` and `loca` tables of the face from the bold slot byte for byte. Refused rather than reseated, because the element is the presentation's answer and not the backend's to pick: there is no fifth one, and two faces that collide agree on the only two fields it is read from. The sources dedupe by description before the elements are compared, and the builder fills an unset slot from a source already given, so neither a family of one file nor a partially named one can collide with itself, and none of the bundled families collides (each one's distinct faces read back onto distinct elements; four of them ship fewer than four faces and so reach the dedupe rather than the comparison). This answers for one family definition: the element's capacity belongs to a declared typeface, so two definitions registered under the same `wordFamily` share one font-list entry and the second still writes over the first's part, measured and unlogged — which of two families a refusal should cost is not decided here. A registered family is read by two parsers that do not accept the same files: the embedder parses table headers with fontbox, which skips a table whose directory entry runs past the end of the file, and the viewer metrics read the program whole through `java.awt.Font.createFont`, which rejects the font for such an entry. Both reads happen before the presentation is handed anything, so a font only one of them accepts — a truncated one — degrades the family instead of costing the deck. Carrying it without metrics is the alternative not taken: what seats such a family is then the PDF descriptor's ascent, which for 126 of the 130 bundled faces is the ascent the viewer uses and for the four Alegreya Sans faces is a full point out at 10pt text, and which of the two a refused font is cannot be established, since the reader that would say is the one that refused. A face's header is measured where the container is built, against the tighter of the two thousand-byte windows the presentation reads it through — each name on its own when it takes the face, refusing a longer one unchecked from inside the hand-over, and the whole header (96 bytes plus four names) when it reads the written part back. The metrics reader in front of it reads the family and full-name records but never the subfamily, so a long subfamily reached the writer and cost the deck: Lato-Regular rewritten with a 496-character subfamily makes a 1,002-byte full name. A header moves in fours, since the family is written twice, so 998 bytes is the largest any face can express and it is carried; the heaviest header among the 130 bundled faces is 250. The thousand is POI's default, which a host can lower with `IOUtils.setByteArrayMaxOverride`, so the check is a floor rather than a guarantee. A deck that carries a program declares it embedded and asks the reader to keep every character of it rather than only those its slides use, since nothing here subsets a face; a deck that carries none leaves embedding off, where the subset flag POI's blank template already sets decides nothing. Standard-14 names carry no binary and map to metric-compatible viewer replacements (Helvetica to Arial, and so on) rather than being embedded

**DOCX.** ⚠️ `DocxFontTable` + `DocxFontEmbedding` — a bundled or registered family the body, a page zone or a header band sets text in is carried in Word's font table (`w:embedRegular`, `w:embedBold`, …), only the faces its styles name, each obfuscated against a key derived from the font as Word stores it. Whole faces, no subsetting, so a character typed later on a machine without the family is still set in it (measured in Word 16 and LibreOffice 26.8). A document that ships a face asks for embedded fonts in its settings (`w:embedTrueTypeFonts`, never `w:saveSubsetFonts`), so the faces survive a save in LibreOffice 24.2 and 26.8 as well as in Word 16. A face whose `OS/2` `fsType` forbids embedding, or allows it for reading and printing only, is named but not shipped, and so is one that cannot be read; the report names each (`embedded font`). Standard-14 names carry no binary and fall to Word's own substitution

### Writing direction inside a **table cell**

Writing direction inside a **table cell** (`DocumentTableStyle.direction`) — the row grades the plain-**string** cell (`table.row("…")`). A cell holding a composed **paragraph** is a paragraph everywhere and takes the row above.

**PDF.** ✅ `TableLayoutSupport` answers the declared direction against the cell's own text — `AUTO` per cell, never per line — and `PdfTableRowFragmentRenderHandler` shapes the letters, draws them in display order, and marks the line with its written text as `ActualText`. Auto columns are measured on the joined forms, so the column fits what is drawn

**PPTX.** ✅ `PptxTableRowFragmentRenderHandler` declares the direction on the cell's frame and hands PowerPoint the text as written, mirroring nothing and keeping every Unicode formatting control (`PdfFont.sanitizeForLogicalTextExport`) — the joining controls because PowerPoint shapes, the direction marks because it also resolves the order, which is more than the paragraph path leaves it. This is the opposite of the paragraph rows above and for a stated reason: a paragraph arrives as one frame per span, so no frame holds a bracket pair for PowerPoint to resolve and the swap has to be made first, whereas a cell is one frame holding a whole line — the input PowerPoint's own implementation is complete for. Measured on a slide: pre-mirroring a cell drew `(2026)` as `)2026(`. A copy out of a cell therefore carries the brackets as typed, which the paragraph path cannot promise

**DOCX.** ⚠️ `DocxSemanticBackend.writeCellContent` writes `w:bidi` on the cell's paragraph and hands Word the text untouched, for its own bidi engine to order and join. Cell **alignment** is written as on any cell (see Table rows): the cascaded `textAnchor` as the cell's `w:vAlign` and its paragraph's `w:jc`, read through the direction, so a right-to-left cell with none stated sits at the vertical middle on the right, as the page draws it. Column order is not mirrored (`w:tblPr/w:bidiVisual` is unwritten)

## Navigation and interactivity

### External hyperlinks

External hyperlinks (fragment- and run-level)

**PDF.** ✅ `PdfLinkAnnotationWriter` + link rects in `PdfFixedLayoutBackend`

**PPTX.** ✅ `PptxNavigationWriter` (transparent hotspots for measured span, line, and fragment rectangles, emitted above all content after the fragment pass)

**DOCX.** ⚠️ `DocxSemanticBackend.newRun` — a paragraph's, a run's, a picture-in-a-line's and a page zone picture's link is a `w:hyperlink` with an external relationship, a run's own link winning over its paragraph's. A block written as a picture, a drawing or a rule — an image, a barcode, a shape, an ellipse, a line — and a table carry none, and the report names the link

### Internal links

Internal links (anchor jump, forward references)

**PDF.** ✅ `PdfInternalLinkWriter` (two-pass)

**PPTX.** ✅ `PptxNavigationWriter` (deferred slide-jump hyperlinks, resolved after all fragments — including across sections)

**DOCX.** ⚠️ `DocxSemanticBackend` — an internal `linkTarget` is a `w:hyperlink` with `w:anchor`, on what an external one is written on, and every anchor the export writes is a bookmark: a paragraph's around its text, a section's, container's, table's, image's or barcode's around everything the block wrote, a stack's layer written as a column around what it writes, a rule's around its paragraph (`bookmarkAround`); a block that writes no paragraph — a section, a container or a column of drawings alone — at the start of the paragraph its first drawing is anchored in, which Word keeps the drawing on the page of. A drawing floated over the text — a shape, an ellipse, a line not written as a rule — a page zone's paragraph or picture, and a block that draws nothing, whose first drawing stands on a page nothing is written on, or that is composed in a table's cell hold no bookmark: the report names the anchor, a link to it points at no bookmark, and a page reference to it is written as the number the layout found

### Page references

Page references — a table of contents' numbers, `addPageReference(...)` (`PageReferenceNode`)

**PDF.** ✅ `PageReferenceDefinition` lays out the resolved page as text, drawn by `PdfParagraphFragmentRenderHandler`

**PPTX.** ✅ the same laid-out text through `PptxParagraphFragmentRenderHandler`

**DOCX.** ⚠️ `DocxSemanticBackend.writePageReference` — a `PAGEREF` field to the anchor's bookmark, as a hyperlink (`\h`), storing the page the layout resolved; the editor recomputes it (LibreOffice on layout, Word on a field update or when it saves the file as PDF). A reference to an anchor the export writes no bookmark for is the text the page prints, since Word turns a `PAGEREF` to a missing bookmark into an error, and the report names it, as it does the margins and padding on the sides its alignment sets it from, which are not written. Every field names a bookmark the file holds. A reference in a table's composed cell stores no number: Word shows it once its fields are updated, and one whose anchor has no bookmark is empty; LibreOffice 24.2 reads a reference as empty where its bookmark is named like one of its numbering sequences (`Table`, `Figure`, `Illustration`, `Drawing`, `Text`), and drops the bookmark of an anchored block that opens with a table right after another table, opened on the paragraph between the two tables, reading its reference as an error. A reference from one section into another stores no number, as the page prints none there; Word, once it updates the field, and LibreOffice show the page as the anchor's section numbers it. Neither `w:updateFields` nor `w:dirty` is set: either makes Word ask, when it opens the file, whether to update its fields, and a yes sets every page reference to 1 (measured in Word 16)

### Document outline / bookmarks tree

Document outline / bookmarks tree

**PDF.** ✅ `PdfBookmarkOutlineWriter`

**PPTX.** ⚠️ `PptxNavigationWriter` (no PPTX outline concept — slide names where 1:1, extra bookmarks dropped with a note)

**DOCX.** ⚠️ `DocxSemanticBackend` — a Word file holds no outline tree of its own: Word builds its Navigation Pane and a table of contents from the `HeadingN` styles the bookmarks' levels become, listing each entry by its paragraph's text (see Bookmark markers, for what the report names)

## Document chrome and output options

Neutral options come from `DocumentOutputOptions`; a backend that cannot
honour an option ignores it (documented contract).

### Metadata

Metadata (title, author, …)

**PDF.** ✅ `PdfDocumentPostProcessor`

**PPTX.** ⚠️ `applyMetadata` in `PptxFixedLayoutBackend` (OPC core properties + extended `Application`; OPC has no producer field, so that value is not representable)

**DOCX.** ⚠️ `applyMetadata` (OPC core properties — title, author as creator, subject, keywords — and the creating application as the extended `Application`; OPC has no producer field here either, so that value is not representable, and the report names one the caller set other than the application, `DROPPED`, `producer`)

### Page backgrounds

Page backgrounds (`DocumentSession.pageBackgrounds`, `PageBackgroundFill` — full page, columns, bands)

**PDF.** ✅ `DocumentPageBackgrounds` adds each fill as a shape fragment under every page's content, drawn by the ordinary shape handler

**PPTX.** ✅ the same fragments, drawn as shapes on every slide

**DOCX.** ⚠️ `DocxPageBackgrounds` — each fill is a rectangle anchored to the page, behind the text. A section with fills carries them in every header part it has (default, first page, even pages), so they are drawn on every page; a section without a header gets an empty one against the page edge to carry them, and a later section without fills gets an empty header of its own rather than inheriting them. On a page with no top margin LibreOffice still sets the first line about 3pt lower under that header. A section of one page is given that header too, so a page an editor's text runs onto is painted like the first; the price is the LibreOffice shift, which the seven sidebar CVs of the fidelity corpus pay — 2.77 to 3.08pt low by the median in LibreOffice 24.2, 3.99pt at the furthest line, where Word moves no line of them by more than 0.12pt, one step of its own grid. A fill's alpha is carried as the shape's. A two-column layout still flows its columns one after the other, so a column fill can stand beside text that is not its column's

### Watermark

Watermark (front/back layers)

**PDF.** ✅ `PdfWatermarkRenderer`

**PPTX.** ✅ `PptxChromeRenderer` (per-slide shape at the PDF placement math; behind-content applies before fragments, so no z-order surgery)

**DOCX.** ❌ (not written; reported `DROPPED`, `watermark`)

### Repeating headers / footers

Repeating headers / footers

**PDF.** ✅ `PdfHeaderFooterRenderer` — the zone's `fontName` is resolved through the document's own `FontLibrary`, so a zone draws in the family the author named; unnamed means standard-14 Helvetica, and a code point that family cannot encode is substituted with `?` exactly as body text is. A family the library does not hold is drawn in Helvetica, and a translucent text or separator colour opaque, each with a warning in the log (`render.pdf.header-footer.font`, `render.pdf.header-footer.opaque`)

**PPTX.** ✅ `PptxChromeRenderer` (positioned per-slide text boxes; `{page}` / `{pages}` / `{date}` tokens with the numbering window rules). The named family reaches the slide run through `PptxFontMapping.familyFor`, and the same family measures the slots — a run measured against one face and typeset in another lands off-centre

**DOCX.** ⚠️ `DocxSemanticBackend.writeBand` (`DocxTextBands`) — one line of a Word header or footer part (what this cell calls reported is a note on the band, `page header` or `page footer`, and on its separator, `translucency` or `border width`): the left slot, the centre slot at a centre tab and the right slot at a right tab against the margins; `{page}` / `{pages}` as `PAGE` / `NUMPAGES` (`SECTIONPAGES` per section) fields with the roman or alphabetic switch; `{date}` as the date of the export; the separator as the paragraph's border, a translucent one flattened against white and reported (`translucency`), one wider or thinner than Word draws held to its range, one of no width drawn at Word's thinnest and one below zero not written, each reported (`border width`); the header or footer distance from the band's geometry, baseline within 0.1pt in LibreOffice; a band sharing its kind with another band or a page zone stands in a frame (`w:framePr`) at its own height; `showOnFirstPage(false)` or counting from page 2 → an empty first-page part. A band starting after page 2, numbers not counting from 1 on page 1, and a band alone of its kind reaching past the page margin (written as a negative margin, so that Word holds the body at it as the page does; LibreOffice moves the body clear of it) are reported; so are a band too shallow for its line, whose text and separator Word stands further in from the edge (9pt in a header 11pt tall: 2.14pt lower), page numbers in letters past 26, which Word letters by repeating one letter (page 28 `bb`, the page `ab`), slots that overlap where the page sets them, which the page draws over one another and both editors set apart, and a slot wider than the line, which both editors wrap

### Page zones

Page zones (node subtree in the band)

**PDF.** ✅ Spliced into the layout graph by `DocumentPageZones`, so the ordinary fragment handlers draw it — no zone-specific code in the backend

**PPTX.** ✅ Same splice, same reason: `PptxFixedLayoutBackend.renderGraph` draws every fragment of the graph

**DOCX.** ⚠️ `DocxSemanticBackend` (`DocxZoneParts`, `DocxPageClasses`) — written into a real `w:ftr` / `w:hdr` part. The band's children become runs on one Word line: a paragraph contributes its runs, an `ImageNode` an inline picture at the size the page draws it — contained or cropped to cover its box as the page draws it, its link its run's — a row the page sets on one line keeps its columns, each part at a tab stop where the page sets it (by its alignment in its column — left, centre or right stop; a picture or page field by its start; after a grow spacer by its end), unless a part takes more lines than one or is not laid out, a part starts right where one held at a right or centre stop ends, or the row is sized to its content (arrangement, grow spacer or a page field in an `auto()` column) and a page field would move another part on a wider page; otherwise a spacer becomes the right tab stop, and `PageContext.pageNumber()` / `pageTotal()` become live `PAGE` / `NUMPAGES` fields. Other node kinds are skipped, logged once a kind and reported once each (`DROPPED`, `page zone content`); a zone row's own fill, outline and side borders are reported as `row paint`. Word sets a row held at stops each part at its stop (a part it cannot set there, the parts before it reaching it, is counted, and where those after it stand is not measured), any other line's parts one after another from the left margin and, after the first spacer, against the right margin, on one baseline, its tallest part's, in a zone of one line a one-line part the page sets on a baseline of its own raised or lowered to it by `w:position`, in whole half points — lowered only as far as the line holds below its baseline, a fifth of it or as far as its tallest part reaches; LibreOffice raises a run about a seventh further than Word, does not raise a page field and stands every picture on the baseline —; the page sets each by the zone's padding, a row's columns and gap, a paragraph's alignment and a part's own sides (a page field's alignment moves its number up to a point, within a box a point wider than it). The `page zone` note counts the parts the page sets elsewhere — read from the layout's zone fragments: a part stands where the page sets it when its line starts, or against the right margin or at a right stop ends, within a point and a half of Word's, on the baseline Word sets it on, in one line and with no prefix before it on the left side; past a part whose width Word does not keep, or a zone whose nodes the page names or nests otherwise, where a part stands is said to be not measured — and names a zone paragraph's right-to-left direction, prefix letters, fitted size where it is not measured, markdown marks written as letters, a markdown heading taller than its line, outline entry, anchor (no bookmark) and a picture set off the baseline, which the line does not carry, and a picture part's transform, outline entry and anchor. The line is an exact line as tall as the tallest part's line on the page — taller for a picture in it, a part written at a larger size than the page's or a part raised above it — standing as far from its edge as puts that part's baseline — a picture's foot — where the page has it (a lone or tallest part within 0.1pt in Word and LibreOffice, measured on 8pt and 18pt Lato headers and 8pt Lato footers), a lone part's padding and margin above and below included; a tallest part of more lines than one is as many exact lines, a footer's standing as many lines further from its edge; lines reaching past the page margin by more than half a point are held there with a negative margin and named; a zone the layout measures that shares its kind with another page zone stands in a frame (`w:framePr`) at its own height, at least its lines tall; a zone the page draws otherwise on the page it is measured on than it is written — other text, face, size or pictures — is not measured, its line Word's, and a zone whose content is none for every page Word writes it on is not written and named, as is one building none for the first page or the pages between, where the page draws it. Because Word paginates, `PageContext.number()` refuses here rather than baking a number that would be wrong on every page but one. The content is built for the first page and for the pages after it (`PageContext.unpaginated(first, last)`): built otherwise for the first, it becomes the title page's part (`w:titlePg`) and the ordinary one, a measured one framed where the two cannot share one distance; built otherwise for the last page, it is written there as built for the pages between and reported. A zone's `appliesTo` predicate is asked over sample pages (`DocxPageClasses`) — a layout of fewer than six pages also over its own, each as one of as many as it ran to, which decide its kinds of page — and, when it follows Word's first / even / other pages, becomes the matching part — `w:titlePg` for the first page, `w:evenAndOddHeaders` for even pages — with an empty part on the pages it skips; a predicate that picks pages within a kind (the last page) is written on every page and reported

### Protection / encryption

Protection / encryption

**PDF.** ✅ `PdfDocumentPostProcessor`

**PPTX.** ❌ (ignored with a one-time warning — no OOXML encryption support planned)

**DOCX.** ❌ (not written, so the file opens unprotected; reported `DROPPED`, `protection`)

### Viewer preferences

Viewer preferences

**PDF.** ✅ `applyViewerPreferences` in `PdfFixedLayoutBackend`

**PPTX.** ❌ (ignored with a one-time warning — PDF-viewer concept)

**DOCX.** ❌ (not written; reported `DROPPED`, `viewer preferences`)

### Debug guide lines / node labels

Debug guide lines / node labels

**PDF.** ✅ `PdfGuideLinesRenderer`, `PdfNodeLabelRenderer`

**PPTX.** ❌ (ignored with a one-time warning — render through the PDF backend to see overlays)

**DOCX.** n/a

### Keep a block on one page

Keep a block on one page (`keepTogether()`, `keepWithNext()`)

**PDF.** ✅ resolved by `LayoutCompiler` before any backend runs

**PPTX.** ✅ same — the slides are the laid-out pages

**DOCX.** ⚠️ `DocxSemanticBackend.keepOnOnePage` — Word re-paginates, so a block the layout placed on one page is told to stay there: `w:keepLines` on each of its paragraphs and `w:keepNext` on every one but the last (on the last too for `keepWithNext`), a table inside it chained row by row, and every row the layout placed held whole by the table writer's own `w:cantSplit` — but the row of a one-row body table of paragraphs all kept with the next, taken back off it by `letTheOneRowTablesKeptWithTheNextBreak` below; five writers decide which rows are given it, so no one rule says which rows carry it — `breakRowsWhereTheLayoutDoes` gives it to each row of a table where the layout placed that row, `writeRow` to a row written as a table where the layout placed that row, `writePanelPiece` to a block written as a panel on its own row where the layout put the block on one page and it either asks to be kept together or is a row or a shape container, and `letTheOneRowTablesKeptWithTheNextBreak` then takes it off a one-row body table of paragraphs all kept with the next, whichever of the three put it there; `DocxTimelineRails`, after that pass, gives it back to each entry's row of a timeline written as one table — which stands among other rows there — unless the timeline is one entry left whole, and to each gap row. Where the export splits a table (`DocxTableSplit`) — a panel cut at its pages, or a body table cut for a page's own margins — the second part is copied with its rows' flags, so a copied row carries what the original was given rather than anything decided for it; both cuts run before that pass, which sees each copy, leaves a panel's row alone where its cell holds a table, never reaches the copied rows of the inner table at all, and judges a body table's copy like any other. What it adds over the chain is a row Word cannot end a page inside — a cell of more than one paragraph, which a plain cell is where its style sets a line spacing and it holds more than one line, or a cell of a table — the chain reaching only a cell's own paragraphs. A table composed in a cell gets neither and is held by the row it stands in, where the layout placed that row. Measured at the foot of a page, in two positions each — the block fitting there, and the layout moving it to the next page whole — ten shapes over twenty documents, held in the fidelity corpus — a panel kept together holding rows of an icon beside a label among them — stood whole in Word 16.0.20527 and LibreOffice 24.2, with no group and no row broken, and all but one on the layout's page in both editors: the band whose second layer the page sets on the last line of its first, which the export writes as a paragraph of its own under the band and its report names (`overlapping layer`), so the band is a line taller and moves to the next page whole where it only just fits at the foot. A first measurement had LibreOffice move six more and took taller table rows for the cause; its documents were set in a face the export does not embed, which that LibreOffice set wider, breaking the cells' text onto two lines. In an embedded face it holds all six where the layout does. Of the 1116 blocks the export held on one page in the 71 documents its corpus had when they were counted, 20 hold a table and none holds a table composed in a cell (two of the documents at the foot of a page, added since, hold one each); across those 71 no row a keep chain reaches, in any of the corpus's 68 tables of more than one row, lacks `w:cantSplit`, while 56 one-row tables carry keeps without it. In a table cell, `w:keepLines` alone, as Word reads `w:keepNext` on the paragraph a cell opens with as keeping the row with what follows its table. A layer stack, a shape container, a canvas or an alignment wrapper in the flow is kept so too, as the layout moves it to a new page whole, a band of layers included; the first block of a band the layout moves to a new page — a paragraph, a picture, a list, a title and its dates set as one line or a spacer, a backdrop drawn before it or not — holds the space above it there, as one in the flow does: in a line kept with it, or in a spacer's own line; a table, a row or a panel opening it holds the band's top edge so with its own, the gap before the band left at the foot of the page above. A list, such a line or a rule the layout moves to a new page in the flow holds its space above there as a paragraph does, and a table, a row or a panel on that one page holds the gap the layout carries there with its own top edge; below a table, where no paragraph on the page above carries the gap the layout leaves there, the line holding the edge carries it as its space above, which Word and LibreOffice drop where the line opens a page they break to and keep where they break the page elsewhere — not where a page-margin section opens with the line, whose first space above both keep. A block that ran over a page break in the layout is taller than a page and is left to flow, as the layout left it. A block the layout moves to the next page whole, or with the block after it, inside a panel that runs on there is named on its node: Word breaks the panel's row where its lines run out and holds no keeps across that break. A body table of one row kept with what follows, its cells holding paragraphs alone, is written with a row that may break (`letTheOneRowTablesKeptWithTheNextBreak`): LibreOffice reads a one-row table whose row may not break as one that may not split, and keeps such a table with nothing. A panel in the body the layout breaks over pages inside a table it holds is a table a page, cut at the row each page starts with (`cutThePanelsAtTheirPages`): Word repeats no header row of a table nested in a cell, so each part after a cut opens with copies of the table's header rows, where it has them, and the page breaks between the parts are fixed; where the page after a cut has other top and bottom margins, and the page above has room for it, the paragraph between the parts ends a Word section with them (per-page margins, below)

### Layer stack

Layer stack — layers drawn over one another (`addLayerStack`)

**PDF.** ✅ each layer's fragments at the place the layout gave it

**PPTX.** ✅ same

**DOCX.** ⚠️ `DocxSemanticBackend` — Word has no layers, so a stack's children are written one after the other, without their positions. Which of these a stack or a shape container becomes — side-by-side columns, a band of its layers, one line its single-line paragraphs share (two or more, each after the first at a tab where the page sets it), drawing alone with its room owed as space, one drawn shape holding a badge's letters, or, laid over the flow, text boxes where the page sets them — is named in the report at the node's own path, whether or not the node loses anything else with it. The exception is a stack whose layers are side-by-side columns (`DocxLayerColumns`): every layer a plain container at the stack's top-left corner, the bands their padding leaves either the same or apart. That is written as one table row, a cell per band, the way a two-column CV lays its columns out as layers to draw the name first. Layers sharing a band follow one another in its cell. A spacer the layout placed level with content of another layer of the same band only keeps that content's place, and is not written. The space above a later layer's first block is the gap the page shows below the content before it, to the block's content — from and to a panel's box where a panel on its page ends that content or holds the block, since the panel writes its padding itself: what the block holds above that content past the gap — its margin, its padding, a contained picture's room — comes out of its own edge (`DocxLayerColumns.resume`) where it is a paragraph, a picture, a list or a table in its layer's own flow with its edge above at zero or more, and a list keeps its own edges after the gap, as a table does. Each layer is held to its box across, past the stack's edge where the page sets it past it, by an indent below zero; in a table cell no further left than the indents round it give, the rest named. The one layer of a stack of one layer is held the same way, its offset across kept and its measure with it; in a table cell, composed in a table's cell with no place of its own, and for an offset down or up, which moves it without making room for it, it is written where the stack starts and named — not where a band or a column's layer sets it at the page's distance below what is above it, which holds the offset. A shape container written as a panel in a table cell keeps its padding round its outline, as the page does — the table's indent and the space above and below it; where its table ends its cell, Word sets the cell's content at its middle or foot without the space below, which a centred cell takes off the space above, the rest named — sets what its cell holds at the top, middle or foot as its first written layer is set, holds a paragraph across to its box, and names a written layer after the first (written under the ones before it), an offset down or up, a box past the shape's side, and a layer set across by a box the layout does not give (`layer offset`). A layer whose content the page starts above where those before it end — a caption over its photo, a label in a card's corner, not a block giving back its edge whose padding or contained picture's room alone reaches there, and a panel the layer opens with read from its box — is written under them, in a column's cell as in a band, and the report names it; a stack composed in a table cell, or exported with no layout, has no place of its own, and one writing more than one of its layers is named with them not measured. What the page draws inside another layer's fill comes after that fill. Drawings stack as the page paints them, by their z-index, each given its place (`wp:anchor/@relativeHeight`) from where the page paints its fragment, whatever order the layers are written in — among the drawings behind the text, and among those in front of it, as Word draws every one behind under every one in front. A painted layer, though, is a shaded cell rather than a drawing, and its fill belongs to the text layer: it is painted over every drawing behind the text, a drawing on a layer *above* it included. So a shape a node paints itself, on a layer the page paints after a painted layer of a stack, a container or a canvas it stands in, is drawn in front of the text (`laidOverAPaintedNeighbour`): an icon laid over a card's corner, which drew none of its ink in Word 16.0.20527 or LibreOffice 24.2 against 4343 pixels of it at 300 dpi in the engine's PDF, now draws 4339 in Word where the page puts it and all of them in LibreOffice, 13.9pt to the right (LibreOffice sets a shape anchored in a cell that cell's left padding further across). Such a shape stays behind where its box holds a line of the flow or a picture, where a line of text painted after it lies over part of its box, where anything is drawn after it in any overlay it stands in or among what it holds (a picture, a shape, a table, a barcode, a chart), where a kind of the caller's own holds it at any depth, and where its node stands at more than one place; there the report names it and one panel that covers it, taking the panels where it writes their fill, so that a panel bearing borders alone — which writes no shading — is not named and a row written as a panel is. Not named: a drawing under a panel the layout breaks across pages or composed in an authored table cell, one under a table cell's own shading, a cell's paint the table draws itself, and the second panel over a drawing lying over two (`DocxNeighbourFillGuardTest` holds the corpus to a ledger that is now empty, and writes those edges out). Measured in LibreOffice: `CharcoalGold`, `SidebarPortrait` and `SlateOrange` fit on one page, as on the page; `NavySidebar` runs one line onto a second page. A stack or shape container the page gives no room — its margins taking back its whole height, as `LumaStudioInvoice`'s sidebar over the page's top margin — is laid over the flow (`laidOverTheFlow`): its drawings where the page draws them, each paragraph in a text box in front of the text where the page sets it — a badge's letters in the shape drawn for them instead — nothing in the flow. Not in a table cell or a panel, and not when it holds anything but drawing and plain paragraphs — a link, an anchor, a picture, a list or a table keeps it in the flow as above. A canvas is written as its contents — what it writes one block after another, its drawings where it places them — and holds its height: the room under what it writes is the space below it, and one that only draws holds its whole box where nothing round it — a band before its layer's first block, drawings held whole, an overlay it is a layer of — measures that room already. The report names the places and the width it loses, as it does a column's fixed width narrower than its band, and what it writes running past its height, a drawing in what it writes that takes no room in Word, or a height not measured. In practice a canvas whose text runs top to bottom in reading order — a name over a title, as `MintEditorial`'s masthead draws — reads in Word as on the page; text a canvas places side by side or out of reading order does not, so a poster- or slide-like canvas page belongs in PDF or PPTX

## Output surface and lifecycle

### Render to bytes / stream / file

Render to bytes / stream / file (`FixedLayoutRenderer`)

**PDF.** ✅ `PdfFixedLayoutBackend`

**PPTX.** ✅ `PptxFixedLayoutBackend`

**DOCX.** ✅ `DocxSemanticBackend` (`SemanticBackend<byte[]>`)

### Render to images

Render to images (`renderToImages`)

**PDF.** ✅ PDFBox `PDFRenderer`

**PPTX.** ❌ (throws with a pointer to the PDF backend — POI's slide rasterizer cannot honour embedded fonts, and the PDF raster of the same graph is the canonical image output)

**DOCX.** n/a (a `FixedLayoutRenderer` surface; the semantic SPI has no raster output)

### Raster-slide mode

Raster-slide mode — every page as one full-slide picture, pixel-exact to the PDF/PNG output (`Builder.rasterSlides(dpi)`)

**PDF.** n/a (the PDF raster is the source)

**PPTX.** ✅ `PptxFixedLayoutBackend`

**DOCX.** n/a (a fixed-layout backend builder option)

### Multi-section documents

Multi-section documents (`renderSections`, per-section chrome, cross-section links)

**PDF.** ✅ `buildSectionsDocument` in `PdfFixedLayoutBackend`

**PPTX.** ⚠️ `renderSections` in `PptxFixedLayoutBackend` (a deck carries one slide size, so every section must share the same page canvas — differing sizes throw)

**DOCX.** ⚠️ `DocxSemanticBackend.exportSections`, reached through `MultiSectionDocument.export` and `toDocxBytes` / `writeDocx` / `buildDocx` — a Word section per section, each with its own page size, orientation, margins and page-zone header and footer (the text header and footer slots are written per section, their page total as `SECTIONPAGES`). Page numbers restart at 1 in every section and a zone's total is `SECTIONPAGES`, as the PDF counts; a section with no zone of its own gets an empty header or footer rather than Word's inherited one; styles, fonts and bookmark names are shared, so links cross sections. With different even and odd pages stated, Word starts every section on an odd page and adds a blank page before a section whose section before it runs to an odd number of pages, which the report names (`page zone`; LibreOffice adds none)

### Per-page margins

Per-page margins (`DocumentSession.pageMargins`)

**PDF.** ✅ laid out page by page (`PageGeometry`)

**PPTX.** ✅ drawn from the layout

**DOCX.** ⚠️ `DocxPageMarginSections` — the first page's top and bottom margins are the section's; a page set against other top and bottom margins than Word sets it with starts a Word section with them where it starts between blocks of the body, an explicit page break there becoming the section's break and a hairline between two tables carrying its end, the title-page part kept to the first section, as is the page count from 1 in a document of several sections — measured in Word and LibreOffice on Windows, on pages of paragraphs, after a page break, after a table, between two tables and under a card's held margin, every page where the layout sets it, line for line, where one set of margins ran a page with a larger bottom margin on past its foot. Each section is named (`page margin`): its page break is fixed when the text above it is edited. Side margins stay the document's, which the body is written against and the page sets its header and footer from; a page whose side margins differ is named. A page that starts among a body table's rows splits the table at that row, the second part opening with copies of the header rows the layout repeats, the hairline between the parts ending the section — measured in both editors, every page holds the lines the layout sets on it; the split is named on the table. A page that starts where a panel is cut at the row of its table the page starts with (`DocxTableSplit.splitInItsPanel`) ends the section in the paragraph between the two parts, which then breaks no page before itself — measured in both editors on a card over three pages with other margins from page 2, every line on its page, lower by the cut's top border only; where the page above has room for the cut and not for that paragraph under the part, the page is named. In a section whose margins change, the cuts are decided once the section is written, before page references are settled and bookmarks kept: a row holding a page reference leaves the panel uncut at its page and every page before it, as any field does, and so does a header row holding a bookmark the export later takes out. LibreOffice 24.2 reads no section's margins after a table it broke over a page itself (a page the panel runs onto not cut, a later one cut); Word does. A page that starts inside another block (a paragraph's lines, a panel not cut there), at a table's row a cell spans rows across or only the header rows stand above, under a header row holding a bookmark, a drawing or a numbered list item, where a header row or a row that would move holds a page reference, after a page whose content reaches its foot under a table or in its rows, or in a document of sections whose header or footer counts its section's pages starts none and is named, Word setting it with the margins in force

### Deterministic output

Deterministic output (render twice → identical bytes)

**PDF.** ✅ `PdfDeterminismWriter`

**PPTX.** ✅ `PptxDeterminismWriter` (pinned OPC created/modified + zip entry-time normalization)

**DOCX.** ✅ `DocxDeterminism` (`DocxSemanticBackend.builder().deterministic(true)` or `deterministic(Instant)`: pinned OPC created/modified + zip entry-time normalization; an embedded font's obfuscation key is derived from the font, so it needs no pinning; a chart's embedded workbook is pinned with or without it, its entries stored and its declarations' line breaks plain, so it is the same bytes on any machine; `buildDocx` / `writeDocx` / `toDocxBytes` take a backend without the option and so write live dates — pass a deterministic backend to `export(...)` for the same bytes; the same bytes on one platform — across platforms the document's own parts differ where Apache POI ends an XML declaration with the platform's line separator)

### ServiceLoader discovery

ServiceLoader discovery (`FixedLayoutBackendProvider`; `SemanticBackendProvider` for a semantic backend)

**PDF.** ✅ `PdfFixedLayoutBackendProvider` (`format() == "pdf"`)

**PPTX.** ✅ `PptxFixedLayoutBackendProvider` (`format() == "pptx"`)

**DOCX.** ✅ `DocxBackendProvider` (`SemanticBackendProvider`, `format() == "docx"`, registered in `META-INF/services`), resolved by `SemanticBackendProviders.forFormat`, which refuses two providers for one format; a new backend each export. `SemanticBackendProvider`, `SemanticBackendProviders` and `DocxBackendProvider` are `@Beta`

### `DocumentSession` convenience methods

`DocumentSession` convenience methods

**PDF.** ✅ `buildPdf` / `writePdf` / `toPdfBytes` / `toImages`

**PPTX.** ✅ `buildPptx` / `writePptx` / `toPptxBytes` (resolved via `BackendProviders.fixedLayout("pptx")`; session chrome applies; fails with `MissingBackendException` naming `graph-compose-render-pptx` when the backend is absent)

**DOCX.** ✅ `buildDocx` / `writeDocx` / `toDocxBytes`, on `DocumentSession` and on `MultiSectionDocument` (`@Beta`; resolved via `SemanticBackendProviders.forFormat("docx")`; fails with `MissingBackendException` naming `graph-compose-render-docx` when the backend is absent) — they take a default backend, so no report sink and no deterministic dates; `session.export(DocxSemanticBackend.builder()…build(), target)` for a configured one

### A node kind of the caller's own

A node kind of the caller's own — a `DocumentNode` laid out by its own `NodeDefinition` (see [the extension guide](../contributing/extension-guide.md))

**PDF.** ✅ its fragments drawn by the handler for each payload type, a custom one registered through `PdfFixedLayoutBackend.Builder.addHandler`; a payload no handler takes throws `UnsupportedNodeCapabilityException`

**PPTX.** ✅ the same, through `PptxFixedLayoutBackend.Builder.addHandler`

**DOCX.** ⚠️ `DocxSemanticBackend` has no handler registry: the shapes such a node paints itself in built-in payloads are drawn by `DocxDrawings` where the page puts them. Where its definition lays out the children it lists as a composite, so the layout places a node at the place of each — its layout path, the name or kind and the index — and none beside them, they are written in its place as an unpainted container's are — one after another, inside its margin and padding (a fixed width it states is not written, and not named), with the space the layout sets between the first two that each stand whole on one page, kept on one page in the body where the page sets them side by side or over one another on one page and, written one after another, they fit a page — and the report names, at the node's kind, the nodes written in its place, and that they are written one after another where the page sets them side by side or over one another (`APPROXIMATED`; measured in Word 16 and LibreOffice 24.2 on two Lato paragraphs at the root, in a section and in a row's cell: every line within 0.06pt of the page in Word, and where a built-in section of the same spacing sets it in both). A definition that lays out other nodes of the same kinds and names at those places has the listed ones written there. One that lists no children, one whose definition lays out a node at another place than those it lists — one that paints what it holds itself lays out none —, one composed in a table's cell (`DocumentTableCell.node`), where the layout gives what a cell holds no place of its own, and one exported with no layout are not written: the report names their text, their pictures and the nodes they hold as not written (`APPROXIMATED` where one paints a shape, otherwise `DROPPED`, and the node is not in the file)

## DOCX export report subjects

Every note `DocxExportReport` carries names its subject. This table lists every subject the
export writes, what it says, and the capability above it belongs to. A subject
written as `<kind>` is the node's own kind — `ParagraphNode`, `TableNode`, `ImageNode`,
`ShapeNode`, `EllipseNode`, `LineNode`, `Path`, `Polygon`, `LayerStackNode`,
`ShapeContainerNode`, `SectionNode` and so on — at the node's path.

`DocxCapabilityMatrixGuardTest` in the `qa` module reads the export's sources and fails when this
table lists a subject or severity the export does not write, or leaves out one it does, when the
Row column names a capability this file has no heading for, and when a DOCX entry names a `Docx…`
class or method no source declares.

| Subject | Severity | What the file holds instead | Row |
|---|---|---|---|
| `<kind>` | `APPROXIMATED` | what the node is written as where that is not the kind of thing it is (a divider as a paragraph border, a layer stack as table columns or a band, a box as drawing alone, a badge as one shape holding its letters, a node laid over the flow, a node kind of the caller's own as the nodes it holds), and what it is written without — a link, an outline entry, a transform, a margin or padding, a keep across a panel's break; a drawing a panel painted beside it hides; an anchor with no bookmark; a page reference written as the number the layout found | the node's own row; Internal links; Page references; A node kind of the caller's own |
| `<kind>` | `DROPPED` | nothing: a drawing that shows nothing the export carries (painted only with a gradient, or with neither fill nor outline), a drawing exported with no layout to say where the page draws it, a page field placed in the body rather than a page zone, a node kind the export does not know that paints no shape and whose children the export does not write (see its row), a geometry with no Word analogue, or what a node draws on a page no paragraph is written on — with, where it has one, its anchor's missing bookmark | the node's own row; Linear gradient fill; Page zones; A node kind of the caller's own |
| `anchor` | `APPROXIMATED` | one bookmark for an anchor two nodes declare, on the last; one bookmark for an anchor in a header row the page repeats, where the table starts | Anchor markers |
| `barcode` | `APPROXIMATED` | a picture of the symbol, its data the description | Barcode / QR |
| `border width` | `APPROXIMATED`, `DROPPED` | a border, rule or separator held to Word's widths; a separator set at no width a border has, not written | Rectangle shape, Line, Table rows, Repeating headers / footers |
| `cell drawing` | `APPROXIMATED`, `DROPPED` | what a table's cells draw, as shapes where the layout puts them, without a transform or gradient; or, on a page no paragraph is written on, not in the file | Table rows, Clip region open/close |
| `cell padding` | `APPROXIMATED` | a cell opening with a table, or spanning rows, keeping its padding as its margins, which Word gives every cell of its row: the row's other cells' text lower, the row as much taller where one of them is its tallest, and taller by a bottom padding kept | Table rows |
| `cell rule` | `APPROXIMATED` | a table's rows taller than the page, where a rule is wider than the padding that takes it | Table rows |
| `chart` | `APPROXIMATED` | a Word chart Word lays out itself, or the chart's data table | Chart |
| `clipped cell content` | `APPROXIMATED` | what a clip composed in a table's cells cuts, written whole | Clip region open/close |
| `clipped <kind>` | `APPROXIMATED` | a clip of a node kind of the caller's own, its content written whole | Clip region open/close; A node kind of the caller's own |
| `clipped layer stack` | `APPROXIMATED` | its layers written whole, what the clip cuts away included | Clip region open/close, Layer stack |
| `clipped shape container` | `APPROXIMATED` | its layers written whole, what the clip cuts away included; or, written as a panel, the panel held at the outline's height, which Word cuts its content at, square | Clip region open/close |
| `closing paragraph` | `APPROXIMATED` | the paragraph after a closing table, a point tall, where text typed at the end goes into the table | Table rows |
| `corner radius` | `APPROXIMATED` | a panel with square corners | Rectangle shape |
| `dash pattern` | `APPROXIMATED` | a rule in Word's own dash for a border | Line |
| `drawing` | `APPROXIMATED`, `DROPPED` | shapes anchored in a table cell, the page having no other paragraph; shapes no node owns on a page no paragraph is written on, not in the file | Rectangle shape, Ellipse, Line, Polygon, Free path |
| `embedded font` | `DROPPED` | a face not shipped: one that cannot be read, or whose `fsType` forbids embedding or allows it for reading and printing only | Font portability |
| `inline <run kind>` | `DROPPED` | the paragraph's text without an inline run of a kind with no Word analogue — not reached by any of the five built-in run kinds, which `InlineRun` is sealed to | Paragraph |
| `inline chip` | `APPROXIMATED` | the chip's fill as run shading, how each side's padding is written or lost, and that LibreOffice does not draw its spaces closing a paragraph | Inline code/badge chips |
| `inline icon` | `APPROXIMATED` | the icon as a picture, its text the picture's description, a gradient flat | Inline SVG, Text an inline icon stands for |
| `inline image` | `DROPPED` | nothing, where the picture's data is empty | Inline images |
| `inline shape` | `APPROXIMATED` | the shape as a picture, and how far a picture wider than its box moves the line | Inline vector shapes |
| `layer offset` | `APPROXIMATED` | a layer written where its stack starts, without its offset or the place across the page sets it; in a table cell, no further left than the cell's text; a shape container's layer written as a panel's content | Layer stack |
| `left edge` | `APPROXIMATED` | a panel in a table cell started at the cell's text, right of the page's | Rectangle shape |
| `line height` | `APPROXIMATED` | lines written at an exact height, which cut text made larger | Paragraph |
| `list picture` | `APPROXIMATED` | a list item's pictures, its marker's and its own, placed by the list's first line, or on the text's baseline where no line the layout set carries the list, rather than by the line of the item that holds each | Inline images, List hanging indent |
| `measured geometry` | `APPROXIMATED` | a document or section written without its resolved layout, and, where the layout failed, why | the DOCX backend, at the top of this file |
| `overlapping layer` | `APPROXIMATED` | a layer the page starts over the ones before it, written under them; or, with no place of its own in the layout, layers written one after another, whether they overlap not measured | Layer stack |
| `page header`, `page footer` | `APPROXIMATED` | a text band set otherwise than the page sets it: on more pages, numbered from 1, past the margin, with its baseline moved in, lettered otherwise past 26, slots set apart or wrapped | Repeating headers / footers |
| `page margin` | `APPROXIMATED` | a Word section where a page's top and bottom margins change, its break fixed; or the margins in force, where no section can start | Per-page margins |
| `page zone` | `APPROXIMATED`, `DROPPED` | a zone written on every page, or as built for the pages between, or past the margin, or what its line cannot carry; a zone empty for the pages Word writes it on, not written; a blank page Word adds before a section | Page zones, Multi-section documents |
| `page zone content` | `DROPPED` | nothing, for a node kind a zone's line cannot hold | Page zones |
| `panel width` | `APPROXIMATED` | a panel composed in a table cell narrower than it, held to the cell, where the page draws it past the cell's edge | Rectangle shape |
| `producer` | `DROPPED` | the creating application, not the producer | Metadata |
| `protection` | `DROPPED` | no protection | Protection / encryption |
| `right border` | `APPROXIMATED` | a panel in a cell ended short of the cell's edge, narrower than the page draws it | Rectangle shape |
| `right end` | `APPROXIMATED` | a panel's table, or a row's or table's columns sized to their content, wider than the page draws them, by the room an editor setting the text in its own face may need; or a line beside a row's spacer that grows, set wider by Word at its size to the half point than the spacer's column has room to give, which Word cuts off at its cell's edge | Rectangle shape, Table rows |
| `row paint` | `APPROXIMATED`, `DROPPED` | a row's fill and borders drawn as a shape, or not written — in a page zone, or where the row is its columns alone | Rectangle shape, Page zones |
| `rule link` | `APPROXIMATED` | a rule without its link | Line |
| `shape container` | `APPROXIMATED` | a panel that grows past its outline, stands upright, or a container's layers written inline | Clip region open/close, Transform open/close, Layer stack |
| `shape container outline` | `DROPPED` | nothing, for an outline no shape this export draws can show | Clip region open/close |
| `space above` | `APPROXIMATED` | a block standing lower than the page sets it, where a top edge below zero pulls further than the space above gives | Paragraph, Rectangle shape |
| `timeline rail` | `APPROXIMATED`, `DROPPED` | the rail drawn per page as a filled bar, which keeps the length the layout gives it, or hidden by a panel painted beside it; or, on a page no paragraph is written on, not in the file | Timeline rail |
| `translucency` | `APPROXIMATED` | an opaque colour flattened against what is painted under it | Alpha / opacity |
| `viewer preferences` | `DROPPED` | none; Word opens the file its own way | Viewer preferences |
| `watermark` | `DROPPED` | no watermark | Watermark |

## Fidelity notes (PPTX)

Geometry identity with PDF is guaranteed by construction at the
shape-frame level: both backends consume the same resolved `LayoutGraph`,
and text is emitted as one absolutely-positioned, wrap-disabled text box
per already-wrapped line, so PowerPoint can never reflow content. What
PPTX cannot guarantee is glyph-level rasterization: PowerPoint draws text
with the fonts installed on the viewing machine. Standard-14 font names are mapped to
metric-compatible system fonts (Helvetica → Arial, Times → Times New
Roman, Courier → Courier New); document-local fonts use their registered
viewer-facing `wordFamily` and should be installed on the viewer for exact glyphs.
Every substitution is explicit: the backend logs
`render.pptx.font.substitution` once per family per render whenever a
font cannot travel as an embedded program — because it has no program,
because one could not be carried, because no family of either kind covers
the name, because the render was asked not to carry a bundled family it
drew with, or because it is a standard-14 name mapped to a
metric-compatible replacement. Three of those lines say what the
deck will not carry and leave what it does reference to the text that was
drawn — in the family, or in the face that did not travel; embedded
families render silently.
