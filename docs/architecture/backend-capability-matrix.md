# Backend capability matrix

One row per engine capability, one column per render backend, and — for
every supported cell — the class that implements it. This file is the
source of truth for "which backend can do what, and where is that code".

Maintenance rule: any PR that adds, degrades, or removes a backend
capability updates this file in the same change. A capability without a
row here does not exist; a ✅ cell without a class name is a doc bug.

Legend:

- ✅ supported — cell names the implementing class
- ⚠️ degraded — supported with a documented deviation (see the note)
- 🚧 in development — implementation in progress, not usable yet
- ❌ not supported — no implementation, no current plan
- n/a — concept does not apply to this backend's output model

Backends:

- **PDF (fixed-layout)** — `graph-compose-render-pdf`,
  `com.demcha.compose.document.backend.fixed.pdf`. Consumes the resolved
  `LayoutGraph`; the reference implementation.
- **PPTX (fixed-layout)** — `graph-compose-render-pptx`,
  `com.demcha.compose.document.backend.fixed.pptx`. Consumes the same
  resolved `LayoutGraph`; geometry-identical to PDF by construction, since
  layout is compiled in core before any backend runs. Ships as `@Beta`
  (Experimental) in its first release — usable, with the per-capability
  status in the tables below; the API shape may still change in a minor
  while feedback lands (see [../api-stability.md](../api-stability.md)).
- **DOCX (semantic)** — `graph-compose-render-docx`,
  `DocxSemanticBackend`. Walks the semantic node tree, deliberately
  ignores fixed-layout geometry; Word owns the flow.

Reading the DOCX column: a row is `n/a` only when the capability is
declared on the fixed-layout SPI and can never reach a semantic backend —
`SemanticBackend` carries just `name()` and `export(DocumentGraph,
SemanticExportContext)`, so `renderSections`, `renderToImages` and the
raster-slide builder option are not questions it can be asked. Drawing
nodes are a different case: `ShapeNode`, `LineNode`, `EllipseNode`,
`PolygonNode`, `PathNode` and `BarcodeNode` all reach
`DocxSemanticBackend.writeNode` and are dropped there with a logged
warning, and Word can express floating shapes, pictures and gradient
fills. Those rows are ❌ — no implementation, no current plan — not
`n/a`.

The PPTX *semantic* skeleton (`PptxSemanticBackend`, slide-safe node
validation + manifest, writes no file) is out of scope for this matrix.

## Fragment payloads (fixed-layout drawing surface)

Payload records live in `core` under
`com.demcha.compose.document.layout.payloads`.

| Capability (payload) | PDF (fixed) | PPTX (fixed) | DOCX (semantic) |
|---|---|---|---|
| Paragraph — pre-wrapped lines, runs, alignment (`ParagraphFragmentPayload`) | ✅ `PdfParagraphFragmentRenderHandler` | ✅ `PptxParagraphFragmentRenderHandler` (one absolute, wrap-disabled frame per measured line) | ⚠️ semantic paragraphs (`DocxSemanticBackend`) — each run keeps its own style, falling back to the paragraph's when it has none; `linkTarget` is still dropped |
| Inline code/badge chips (`InlineBackground` on text spans) | ✅ `PdfParagraphFragmentRenderHandler` | ✅ `PptxParagraphFragmentRenderHandler` | ❌ |
| Inline images (`ParagraphImageSpan`) | ✅ `PdfParagraphFragmentRenderHandler` | ✅ `PptxParagraphFragmentRenderHandler` | ❌ |
| Inline vector shapes (`ParagraphShapeSpan`) | ✅ `PdfParagraphFragmentRenderHandler` | ⚠️ `PptxParagraphFragmentRenderHandler` + `PptxInlineGeometry` (distinct per-corner radii render with the top-left radius — single-adjust preset) | ❌ |
| Inline SVG (`ParagraphSvgSpan`) | ✅ `PdfParagraphFragmentRenderHandler` + `PdfPathPainter` | ⚠️ `PptxParagraphFragmentRenderHandler` + `PptxInlineGeometry` + `PptxInlineSvgRasterizer` (simple layers stay native; arbitrary clips, exact dash/cap/join styles, and off-viewBox art use a transparent PNG fallback; gradient paints use their primary colour) | ❌ |
| Rectangle shape — fill, stroke, per-corner radii, side borders (`ShapeFragmentPayload`) | ✅ `PdfShapeFragmentRenderHandler` | ⚠️ `PptxShapeFragmentRenderHandler` (distinct per-corner radii render with the top-left radius on all corners — single-adjust `roundRect` preset — until custom geometry lands; uniform radii and side borders exact) | ❌ |
| Ellipse (`EllipseFragmentPayload`) | ✅ `PdfEllipseFragmentRenderHandler` | ✅ `PptxEllipseFragmentRenderHandler` | ❌ |
| Line — dash pattern, line cap (`LineFragmentPayload`) | ✅ `PdfLineFragmentRenderHandler` | ⚠️ `PptxLineFragmentRenderHandler` (numeric dash arrays map to the generic dashed preset; solid lines and caps exact) | ❌ |
| Polygon (`PolygonFragmentPayload`) | ✅ `PdfPolygonFragmentRenderHandler` | ✅ `PptxPolygonFragmentRenderHandler` + `PptxInlineGeometry` | ❌ |
| Free path — segments, dash, cap, join (`PathFragmentPayload`) | ✅ `PdfPathFragmentRenderHandler` + `PdfPathPainter` | ⚠️ `PptxPathFragmentRenderHandler` + `PptxInlineGeometry` (numeric dash arrays map to the dashed preset) | ❌ |
| Linear gradient fill (`DocumentPaint`) | ✅ `PdfShadingSupport` | ✅ `PptxGradientFill` (native `gradFill`; explicit-axis endpoints approximate to the angle) | ❌ |
| Radial gradient fill (`DocumentPaint`) | ✅ `PdfShadingSupport` | ⚠️ `PptxGradientFill` (`circle` path shade — DrawingML cannot express radius-to-farthest-corner exactly) | ❌ |
| Gradient strokes | ✅ `PdfPathPainter` (pattern stroking colour) | ✅ `PptxGradientFill` (native `ln`/`gradFill`) | ❌ |
| Image — STRETCH / CONTAIN / COVER fit (`ImageFragmentPayload`) | ✅ `PdfImageFragmentRenderHandler` | ✅ `PptxImageFragmentRenderHandler` (COVER via the picture source crop) | ✅ `DocxSemanticBackend.writeImage` (the box comes from `NodeDefinitionSupport.resolveImageDimensions`, the same rule layout applies to `width` / `height` / `scale` and the content-width clamp; CONTAIN is embedded at its fitted size, COVER via the picture source crop as in PPTX, and the picture type is read from the bytes) |
| Barcode / QR (`BarcodeFragmentPayload`) | ✅ `PdfBarcodeFragmentRenderHandler` (ZXing raster) | ✅ `PptxBarcodeFragmentRenderHandler` (identical ZXing raster) | ❌ |
| Table rows — resolved cells, row/col spans, two-pass fill/border paint (`TableRowFragmentPayload`) | ✅ `PdfTableRowFragmentRenderHandler` + row grouping in `PdfFixedLayoutBackend` | ✅ `PptxTableRowFragmentRenderHandler` + row grouping in `PptxFixedLayoutBackend` (positioned rectangles, edge lines, and text frames — never native PPTX tables, which re-lay-out content) | ⚠️ `DocxSemanticBackend.writeTable` (a real Word table on the grid `TableGrid` resolves: `colSpan` maps to `w:gridSpan`, `rowSpan` to `w:vMerge`, and the cascaded `DocumentTableStyle` text style reaches the cell's runs; the cell's fill maps to `w:shd` and its stroke to `w:tcBorders`; a composed cell writes paragraphs and their wrappers only — one built from an image or a list lands empty, and a fill's opacity is dropped since `w:shd` is opaque) |
| Clip region open/close (`ShapeClipBegin/EndPayload`) | ✅ `PdfShapeClipBegin/EndRenderHandler` (CLIP_BOUNDS + CLIP_PATH) | ✅ `PptxClipSafety` + raster fallback in `PptxFixedLayoutBackend` — a provably no-op clip (padded content that cannot be cut) skips the fallback entirely and stays native, editable shapes; a clip that can cut ink renders through the PDF backend into one transparent picture on the clip bounds (pixel-exact, not editable as shapes; run-level link hotspots are not emitted and custom fragment handlers do not apply inside the picture; `Builder.clipRasterFallback(false)` restores unclipped vectors + warning; the raster targets a 2048px long edge, clamped to between native size and 4x, so a region larger than that is rendered at native resolution rather than downscaled — which also means its transient memory grows with the clip instead of stopping at the target (a 3370pt A0-landscape region costs ~45MB while rendering, against ~17MB for anything up to 2048pt); a true vector clip is tracked in [#413](https://github.com/DemchaAV/GraphCompose/issues/413)) | ⚠️ inline fallback + one-time capability warning |
| Transform open/close — rotate/scale about fragment centre (`TransformBegin/EndPayload`) | ✅ `PdfTransformBegin/EndRenderHandler` | ✅ `PptxTransformBegin/EndRenderHandler` (group shape; rotation and centre-pivot scaling via the exterior/interior frame ratio) | ⚠️ inline fallback + one-time capability warning |
| Anchor markers (`AnchorMarkerPayload`) | ✅ `PdfAnchorMarkerRenderHandler` + `PdfInternalLinkWriter` | ✅ `PptxAnchorMarkerRenderHandler` + `PptxNavigationWriter` (slide-jump hyperlinks resolved after all fragments, so forward references work) | ❌ |
| Bookmark markers (`BookmarkMarkerPayload`) | ✅ `PdfBookmarkMarkerRenderHandler` + `PdfBookmarkOutlineWriter` | ⚠️ `PptxBookmarkMarkerRenderHandler` + `PptxNavigationWriter` (PPTX has no outline tree — the first bookmark on a page names its slide, further bookmarks on the same page are dropped with a debug note) | ❌ |
| Alpha / opacity | ✅ `PdfAlphaSupport` (`PDExtendedGraphicsState` on every surface — shape fills/strokes, text runs, lines, side borders, table paint) | ✅ native `<a:alpha>` via POI on every surface — fills, strokes, text runs, table paint | ❌ |
| Text decorations — underline / strikethrough (`DocumentTextDecoration`) | ✅ `PdfTextDecorations` (em-proportional marks: underline −0.10 em, strikethrough +0.28 em, thickness 0.05 em) | ✅ `PptxTextFrames.applyStyle` (PowerPoint draws its own marks — sub-point placement differences vs the PDF's constants) | ✅ `DocxSemanticBackend.applyStyle` (underline maps to Word's single underline, strikethrough to `w:strike`) |
| Writing direction — right-to-left paragraphs (`ParagraphBuilder.direction`, `TextDirection`) | ✅ `ParagraphWrapping` resolves the line with the Unicode Bidirectional Algorithm and `PdfParagraphFragmentRenderHandler` draws it reordered — the page is painted, so the engine owns the order | ⚠️ `PptxParagraphFragmentRenderHandler` — a right-to-left line goes through **per-span absolute frames** rather than one flowing frame, each pinned where the layout put it, because a shared frame lets PowerPoint re-flow the runs and undo the resolved order. Each such frame declares its direction (`rtl`), which is what puts a neutral on the correct side. The deviation is that the line is not one editable paragraph, and that the text a reader copies out carries mirrored punctuation (see the mirroring row) | ⚠️ `DocxSemanticBackend.applyDirection` writes `w:bidi` (resolving `AUTO` through the same `ParagraphDirection` the page used) and hands Word logical text for its own bidi engine, which orders and joins it correctly. Two properties are missing ([#546](https://github.com/DemchaAV/GraphCompose/issues/546)): `w:jc` is written physically, but Word reads it as start/end **relative to the paragraph direction**, so a flush-right right-to-left paragraph is written as `right` and drawn flush **left**; and no `w:szCs` / `w:bCs` / `w:iCs` is written, so Word takes size and weight for the complex-script characters from its own defaults rather than the requested style |
| Arabic contextual shaping — joined letter forms (`ArabicShaper`) | ✅ shaped into Presentation Forms-B before measurement, because `showText` walks the font `cmap` and never runs `GSUB`; a font carrying the letters but not the forms degrades to unjoined base letters rather than `?` | ✅ base letters restored (`PptxParagraphFragmentRenderHandler` → `ArabicShaper.toBaseLetters`, joining controls kept) — PowerPoint shapes Arabic itself, and frozen forms would land in a file users search and copy from | ✅ never shaped — Word receives the letters and shapes them itself |
| Bidi mirroring — paired punctuation in a reordered line (UAX #9 L4) | ✅ `BidiMirroring` swaps at the PDF seam, so a parenthesis faces what it encloses; the mirrored set is document punctuation rather than the whole Unicode table | ⚠️ `PptxParagraphFragmentRenderHandler` + `BidiMirroring` — PowerPoint places a neutral from the declared direction but does not mirror it, so the swap is made before the text is handed over. Visually correct; the stored run therefore holds the mirrored character, including `<` and `>` | n/a — Word mirrors from the logical text it is given |
| Text extraction fidelity of reordered/shaped text | ✅ the font's `ToUnicode` map names the letters an author wrote rather than the shaped forms (`PdfShapedGlyphUnicode`, applied after subsetting and before encryption), and each reversed run carries its written text as `ActualText` (`PdfActualText`) so the reading order is in the file rather than re-derived; per-glyph text inside a section is reachable through the font's map rather than plain extraction | ⚠️ the runs are stored as written except for mirrored punctuation, so copying a right-to-left line out of a slide yields swapped brackets | n/a — the runs are stored as written |
| Writing direction inside a **table cell** | ❌ a cell's text carries no direction — Hebrew draws reversed and Arabic unjoined; compose such content as a paragraph (the Hebrew invoice example builds its line items from rows for this reason) | ❌ same limit | ❌ same limit |

## Navigation and interactivity

| Capability | PDF (fixed) | PPTX (fixed) | DOCX (semantic) |
|---|---|---|---|
| External hyperlinks (fragment- and run-level) | ✅ `PdfLinkAnnotationWriter` + link rects in `PdfFixedLayoutBackend` | ✅ `PptxNavigationWriter` (transparent hotspots for measured span, line, and fragment rectangles, emitted above all content after the fragment pass) | ❌ |
| Internal links (anchor jump, forward references) | ✅ `PdfInternalLinkWriter` (two-pass) | ✅ `PptxNavigationWriter` (deferred slide-jump hyperlinks, resolved after all fragments — including across sections) | ❌ |
| Document outline / bookmarks tree | ✅ `PdfBookmarkOutlineWriter` | ⚠️ `PptxNavigationWriter` (no PPTX outline concept — slide names where 1:1, extra bookmarks dropped with a note) | ❌ |

## Document chrome and output options

Neutral options come from `DocumentOutputOptions`; a backend that cannot
honour an option ignores it (documented contract).

| Capability | PDF (fixed) | PPTX (fixed) | DOCX (semantic) |
|---|---|---|---|
| Metadata (title, author, …) | ✅ `PdfDocumentPostProcessor` | ⚠️ `applyMetadata` in `PptxFixedLayoutBackend` (OPC core properties + extended `Application`; OPC has no producer field, so that value is not representable) | ⚠️ `applyOutputOptions` (OPC core properties — title, author as creator, subject, keywords; OPC has no producer field here either, so that value is not representable) |
| Watermark (front/back layers) | ✅ `PdfWatermarkRenderer` | ✅ `PptxChromeRenderer` (per-slide shape at the PDF placement math; behind-content applies before fragments, so no z-order surgery) | ❌ |
| Repeating headers / footers | ✅ `PdfHeaderFooterRenderer` | ✅ `PptxChromeRenderer` (positioned per-slide text boxes; `{page}` / `{pages}` / `{date}` tokens with the numbering window rules) | ❌ |
| Protection / encryption | ✅ `PdfDocumentPostProcessor` | ❌ (ignored with a one-time warning — no OOXML encryption support planned) | ❌ |
| Viewer preferences | ✅ `applyViewerPreferences` in `PdfFixedLayoutBackend` | ❌ (ignored with a one-time warning — PDF-viewer concept) | n/a |
| Debug guide lines / node labels | ✅ `PdfGuideLinesRenderer`, `PdfNodeLabelRenderer` | ❌ (ignored with a one-time warning — render through the PDF backend to see overlays) | n/a |

## Output surface and lifecycle

| Capability | PDF (fixed) | PPTX (fixed) | DOCX (semantic) |
|---|---|---|---|
| Render to bytes / stream / file (`FixedLayoutRenderer`) | ✅ `PdfFixedLayoutBackend` | ✅ `PptxFixedLayoutBackend` | ✅ `DocxSemanticBackend` (`SemanticBackend<byte[]>`) |
| Render to images (`renderToImages`) | ✅ PDFBox `PDFRenderer` | ❌ (throws with a pointer to the PDF backend — POI's slide rasterizer cannot honour embedded fonts, and the PDF raster of the same graph is the canonical image output) | n/a (a `FixedLayoutRenderer` surface; the semantic SPI has no raster output) |
| Raster-slide mode — every page as one full-slide picture, pixel-exact to the PDF/PNG output (`Builder.rasterSlides(dpi)`) | n/a (the PDF raster is the source) | ✅ `PptxFixedLayoutBackend` | n/a (a fixed-layout backend builder option) |
| Multi-section documents (`renderSections`, per-section chrome, cross-section links) | ✅ `buildSectionsDocument` in `PdfFixedLayoutBackend` | ⚠️ `renderSections` in `PptxFixedLayoutBackend` (a deck carries one slide size, so every section must share the same page canvas — differing sizes throw) | n/a (`renderSections` is declared on `FixedLayoutRenderer`; `MultiSectionDocument` drives fixed-layout backends only) |
| Deterministic output (render twice → identical bytes) | ✅ `PdfDeterminismWriter` | ✅ `PptxDeterminismWriter` (pinned OPC created/modified + zip entry-time normalization) | ❌ |
| ServiceLoader discovery (`FixedLayoutBackendProvider`) | ✅ `PdfFixedLayoutBackendProvider` (`format() == "pdf"`) | ✅ `PptxFixedLayoutBackendProvider` (`format() == "pptx"`) | n/a (semantic SPI: `SemanticBackend`) |
| `DocumentSession` convenience methods | ✅ `buildPdf` / `writePdf` / `toPdfBytes` / `toImages` | ✅ `buildPptx` / `writePptx` / `toPptxBytes` (resolved via `BackendProviders.fixedLayout("pptx")`; session chrome applies; fails with `MissingBackendException` naming `graph-compose-render-pptx` when the backend is absent) | ✅ `session.export(new DocxSemanticBackend(...), target)` — the semantic backend is named directly rather than discovered |

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
font travels as a standard-14 replacement or a name-only reference
instead of an embedded program; embedded families render silently.
