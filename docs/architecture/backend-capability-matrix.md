# Backend capability matrix

What each render backend can do, at a glance: one row per engine
capability, one column per backend. Each capability links to its details —
the class behind every cell and each deviation in full — in
[backend-capability-details.md](backend-capability-details.md).

Maintenance rule: any PR that adds, degrades, or removes a backend
capability updates this table and its details in the same change. A
capability without a row here does not exist.

Legend: ✅ supported · ⚠️ supported with a deviation its details name ·
🚧 in development · ❌ not supported · n/a the concept does not apply to
that backend's output.

PDF, PPTX and DOCX are all full outputs. PDF and PPTX draw the resolved
layout, PPTX geometry-identical to PDF; DOCX writes editable Word content
that Word paginates and re-flows after an edit, and its export report
names what it cannot carry. The PPTX and DOCX Java APIs are Experimental
(`@Beta`) — see [../api-stability.md](../api-stability.md).

## Fragment payloads (fixed-layout drawing surface)

| Capability | PDF | PPTX | DOCX |
|---|---|---|---|
| [Paragraph](backend-capability-details.md#paragraph) | ✅ | ✅ one fixed frame per measured line | ⚠️ editable Word paragraphs at the page's line heights |
| [List hanging indent](backend-capability-details.md#list-hanging-indent) | ✅ | ✅ same geometry as PDF | ⚠️ flat lists only; a nested list uses a fixed indent |
| [Inline code/badge chips](backend-capability-details.md#inline-codebadge-chips) | ✅ | ✅ | ⚠️ run shading; corner radius and vertical padding lost |
| [Inline images](backend-capability-details.md#inline-images) | ✅ | ✅ | ⚠️ inline picture; lowered ones sit higher in LibreOffice |
| [Inline vector shapes](backend-capability-details.md#inline-vector-shapes) | ✅ | ⚠️ unequal corner radii use the top-left one | ⚠️ picture, not an editable shape |
| [Inline SVG](backend-capability-details.md#inline-svg) | ✅ | ⚠️ complex art falls back to PNG; gradients flat | ⚠️ always a PNG picture; gradients flat |
| [Text an inline icon stands for](backend-capability-details.md#text-an-inline-icon-stands-for) | ✅ invisible text layer for copy and search | ❌ icon drawn, its text not written | ⚠️ picture description only; not copied or searched |
| [Rectangle shape](backend-capability-details.md#rectangle-shape) | ✅ | ⚠️ unequal corner radii use the top-left one | ⚠️ panels as tables, square corners; shapes float |
| [Ellipse](backend-capability-details.md#ellipse) | ✅ | ✅ | ⚠️ floating ellipse shape; transform not carried |
| [Line](backend-capability-details.md#line) | ✅ | ⚠️ dash arrays become the generic dashed preset | ⚠️ Word rule or floating line; rule dashes approximated |
| [Polygon](backend-capability-details.md#polygon) | ✅ | ✅ | ⚠️ floating custom shape; LibreOffice bevels sharp corners |
| [Free path](backend-capability-details.md#free-path) | ✅ | ⚠️ dash arrays become the dashed preset | ⚠️ floating custom shape; gradient not carried |
| [Linear gradient fill](backend-capability-details.md#linear-gradient-fill) | ✅ | ✅ native; explicit axis approximated to an angle | ❌ not written; report names each |
| [Radial gradient fill](backend-capability-details.md#radial-gradient-fill) | ✅ | ⚠️ circle shade; radius not exact | ❌ not written, same as linear; report names each |
| [Gradient strokes](backend-capability-details.md#gradient-strokes) | ✅ | ✅ native | ❌ drawn in the stroke's solid colour; report names it |
| [Image](backend-capability-details.md#image) | ✅ | ✅ cover fit by cropping the picture | ⚠️ fits kept; left margin and padding not written |
| [Barcode / QR](backend-capability-details.md#barcode--qr) | ✅ vector | ✅ native vector shapes | ⚠️ PNG picture; scans, data not editable |
| [Chart](backend-capability-details.md#chart) | ✅ drawn from ordinary shapes | ✅ same shapes as PDF | ⚠️ native Word chart; Word lays out labels and legend |
| [Table rows](backend-capability-details.md#table-rows) | ✅ | ✅ positioned shapes, not native tables | ⚠️ real Word table; Word repaginates, rows kept whole |
| [Clip region open/close](backend-capability-details.md#clip-region-openclose) | ✅ native clip | ✅ native; a clip that hides content becomes a picture | ⚠️ mostly not clipped; a clip that cuts is reported |
| [Timeline rail](backend-capability-details.md#timeline-rail) | ✅ one fragment per page | ✅ same per-page fragments | ⚠️ one table, rail as cell border; else a fixed bar |
| [Transform open/close](backend-capability-details.md#transform-openclose) | ✅ | ✅ as a group shape | ⚠️ not rotated or scaled; report names it |
| [Anchor markers](backend-capability-details.md#anchor-markers) | ✅ | ✅ slide jumps; forward references work | ⚠️ Word bookmarks; shared anchors reported |
| [Bookmark markers](backend-capability-details.md#bookmark-markers) | ✅ outline | ⚠️ first bookmark names the slide; rest dropped | ⚠️ Word heading levels; the look is kept |
| [Alpha / opacity](backend-capability-details.md#alpha--opacity) | ✅ but text header/footer and its line drawn opaque | ✅ native on every surface | ⚠️ text and drawings keep it; shading flattened |
| [Text decorations](backend-capability-details.md#text-decorations) | ✅ | ✅ PowerPoint draws its own marks | ✅ native underline and strike |
| [Writing direction](backend-capability-details.md#writing-direction) | ✅ reordered by the engine | ⚠️ a text box per piece, not one editable paragraph | ✅ native; Word orders and joins the text |
| [Arabic contextual shaping](backend-capability-details.md#arabic-contextual-shaping) | ✅ shaped before drawing | ✅ base letters; PowerPoint shapes them | ✅ Word shapes it |
| [Bidi mirroring](backend-capability-details.md#bidi-mirroring) | ✅ paired punctuation mirrored | ⚠️ brackets flipped in advance; right in PowerPoint only | n/a — Word mirrors it |
| [Text extraction fidelity of reordered/shaped text](backend-capability-details.md#text-extraction-fidelity-of-reorderedshaped-text) | ✅ copies out as written | ⚠️ copied text has swapped brackets | n/a — runs stored as written |
| [Font portability](backend-capability-details.md#font-portability) | ✅ subset into the file | ✅ fonts carried in the deck, whole | ⚠️ whole faces embedded; one face set per name; restricted and name-sharing ones named |
| [Writing direction inside a **table cell**](backend-capability-details.md#writing-direction-inside-a-table-cell) | ✅ | ✅ PowerPoint orders the cell text | ⚠️ cell is right-to-left; columns not mirrored |

## Navigation and interactivity

| Capability | PDF | PPTX | DOCX |
|---|---|---|---|
| [External hyperlinks](backend-capability-details.md#external-hyperlinks) | ✅ | ✅ as clickable areas | ⚠️ text and in-line pictures; images, shapes, tables none |
| [Internal links](backend-capability-details.md#internal-links) | ✅ forward links work | ✅ slide jumps, forward references too | ⚠️ bookmark links; floating drawings can't be targets |
| [Page references](backend-capability-details.md#page-references) | ✅ | ✅ | ⚠️ live page field; the editor recomputes it |
| [Document outline / bookmarks tree](backend-capability-details.md#document-outline--bookmarks-tree) | ✅ | ⚠️ slide names; extra bookmarks dropped | ⚠️ built by Word from heading styles |

## Document chrome and output options

| Capability | PDF | PPTX | DOCX |
|---|---|---|---|
| [Metadata](backend-capability-details.md#metadata) | ✅ | ⚠️ no producer field | ⚠️ no producer field; report names it |
| [Page backgrounds](backend-capability-details.md#page-backgrounds) | ✅ | ✅ drawn on every slide | ⚠️ page shapes in the header; LibreOffice sets text lower |
| [Watermark](backend-capability-details.md#watermark) | ✅ | ✅ per-slide shape, as on the page | ❌ not written; report names it |
| [Repeating headers / footers](backend-capability-details.md#repeating-headers--footers) | ✅ in the zone's named font | ✅ text boxes with page tokens | ⚠️ one Word header or footer line; live page fields |
| [Page zones](backend-capability-details.md#page-zones) | ✅ drawn by the ordinary handlers | ✅ same | ⚠️ one Word header or footer line; some kinds dropped |
| [Protection / encryption](backend-capability-details.md#protection--encryption) | ✅ | ❌ ignored with a warning | ❌ opens unprotected; report names it |
| [Viewer preferences](backend-capability-details.md#viewer-preferences) | ✅ | ❌ ignored with a warning; PDF-viewer concept | ❌ not written; report names it |
| [Debug guide lines / node labels](backend-capability-details.md#debug-guide-lines--node-labels) | ✅ | ❌ ignored with a warning; render via PDF | n/a |
| [Keep a block on one page](backend-capability-details.md#keep-a-block-on-one-page) | ✅ resolved before any backend runs | ✅ same | ⚠️ Word keep flags; Word re-paginates |
| [Layer stack](backend-capability-details.md#layer-stack) | ✅ | ✅ | ⚠️ layers written one after another, not overlaid |

## Output surface and lifecycle

| Capability | PDF | PPTX | DOCX |
|---|---|---|---|
| [Render to bytes / stream / file](backend-capability-details.md#render-to-bytes--stream--file) | ✅ | ✅ | ✅ |
| [Render to images](backend-capability-details.md#render-to-images) | ✅ | ❌ throws; use the PDF backend | n/a (no raster output) |
| [Raster-slide mode](backend-capability-details.md#raster-slide-mode) | n/a (the PDF raster is the source) | ✅ | n/a (a fixed-layout builder option) |
| [Multi-section documents](backend-capability-details.md#multi-section-documents) | ✅ | ⚠️ all sections must share one page size | ⚠️ a Word section each; Word may add a blank page |
| [Per-page margins](backend-capability-details.md#per-page-margins) | ✅ | ✅ | ⚠️ top and bottom via Word sections; sides shared |
| [Deterministic output](backend-capability-details.md#deterministic-output) | ✅ | ✅ | ✅ same bytes on one platform, not across |
| [ServiceLoader discovery](backend-capability-details.md#serviceloader-discovery) | ✅ | ✅ | ✅ beta |
| [`DocumentSession` convenience methods](backend-capability-details.md#documentsession-convenience-methods) | ✅ | ✅ | ✅ default backend; no report, dates differ each run |
| [A node kind of the caller's own](backend-capability-details.md#a-node-kind-of-the-callers-own) | ✅ via a registered handler | ✅ same | ⚠️ children written in order, or not written |

## More detail

- [DOCX export report subjects](backend-capability-details.md#docx-export-report-subjects) — every
  note a Word export's report can carry, its severity, and the capability
  it belongs to.
- [Fidelity notes (PPTX)](backend-capability-details.md#fidelity-notes-pptx) — how PowerPoint
  draws text with the fonts on the viewing machine.
