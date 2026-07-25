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
  ignores fixed-layout geometry; Word owns the flow. Geometry rows are
  n/a for it by design.

The PPTX *semantic* skeleton (`PptxSemanticBackend`, slide-safe node
validation + manifest, writes no file) is out of scope for this matrix.

## Fragment payloads (fixed-layout drawing surface)

Payload records live in `core` under
`com.demcha.compose.document.layout.payloads`.

| Capability (payload) | PDF (fixed) | PPTX (fixed) | DOCX (semantic) |
|---|---|---|---|
| Paragraph — pre-wrapped lines, runs, alignment (`ParagraphFragmentPayload`) | ✅ `PdfParagraphFragmentRenderHandler` | ✅ `PptxParagraphFragmentRenderHandler` (one absolute, wrap-disabled frame per measured line) | ✅ semantic paragraphs (`DocxSemanticBackend`) |
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
| Image — STRETCH / CONTAIN / COVER fit (`ImageFragmentPayload`) | ✅ `PdfImageFragmentRenderHandler` | ✅ `PptxImageFragmentRenderHandler` (COVER via the picture source crop) | ✅ semantic images (`DocxSemanticBackend`) |
| Barcode / QR (`BarcodeFragmentPayload`) | ✅ `PdfBarcodeFragmentRenderHandler` (ZXing raster) | ✅ `PptxBarcodeFragmentRenderHandler` (identical ZXing raster) | ❌ |
| Table rows — resolved cells, row/col spans, two-pass fill/border paint (`TableRowFragmentPayload`) | ✅ `PdfTableRowFragmentRenderHandler` + row grouping in `PdfFixedLayoutBackend` | ✅ `PptxTableRowFragmentRenderHandler` + row grouping in `PptxFixedLayoutBackend` (positioned rectangles, edge lines, and text frames — never native PPTX tables, which re-lay-out content) | ✅ semantic tables (`DocxSemanticBackend`) |
| Clip region open/close (`ShapeClipBegin/EndPayload`) | ✅ `PdfShapeClipBegin/EndRenderHandler` (CLIP_BOUNDS + CLIP_PATH) | ✅ `PptxClipSafety` + raster fallback in `PptxFixedLayoutBackend` — a provably no-op clip (padded content that cannot be cut) skips the fallback entirely and stays native, editable shapes; a clip that can cut ink renders through the PDF backend into one transparent picture on the clip bounds (pixel-exact, not editable as shapes; run-level link hotspots are not emitted and custom fragment handlers do not apply inside the picture; `Builder.clipRasterFallback(false)` restores unclipped vectors + warning; the raster targets a 2048px long edge, clamped to between native size and 4x, so a region larger than that is rendered at native resolution rather than downscaled — which also means its transient memory grows with the clip instead of stopping at the target (a 3370pt A0-landscape region costs ~45MB while rendering, against ~17MB for anything up to 2048pt); a true vector clip is tracked in [#413](https://github.com/DemchaAV/GraphCompose/issues/413)) | ⚠️ inline fallback + one-time capability warning |
| Transform open/close — rotate/scale about fragment centre (`TransformBegin/EndPayload`) | ✅ `PdfTransformBegin/EndRenderHandler` | ✅ `PptxTransformBegin/EndRenderHandler` (group shape; rotation and centre-pivot scaling via the exterior/interior frame ratio) | ⚠️ inline fallback + one-time capability warning |
| Anchor markers (`AnchorMarkerPayload`) | ✅ `PdfAnchorMarkerRenderHandler` + `PdfInternalLinkWriter` | ✅ `PptxAnchorMarkerRenderHandler` + `PptxNavigationWriter` (slide-jump hyperlinks resolved after all fragments, so forward references work) | ❌ |
| Bookmark markers (`BookmarkMarkerPayload`) | ✅ `PdfBookmarkMarkerRenderHandler` + `PdfBookmarkOutlineWriter` | ⚠️ `PptxBookmarkMarkerRenderHandler` + `PptxNavigationWriter` (PPTX has no outline tree — the first bookmark on a page names its slide, further bookmarks on the same page are dropped with a debug note) | ❌ |
| Alpha / opacity | ✅ `PdfAlphaSupport` (`PDExtendedGraphicsState` on every surface — shape fills/strokes, text runs, lines, side borders, table paint) | ✅ native `<a:alpha>` via POI on every surface — fills, strokes, text runs, table paint | ❌ |
| Text decorations — underline / strikethrough (`DocumentTextDecoration`) | ✅ `PdfTextDecorations` (em-proportional marks: underline −0.10 em, strikethrough +0.28 em, thickness 0.05 em) | ✅ `PptxTextFrames.applyStyle` (PowerPoint draws its own marks — sub-point placement differences vs the PDF's constants) | ❌ |

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
| Metadata (title, author, …) | ✅ `PdfDocumentPostProcessor` | ⚠️ `applyMetadata` in `PptxFixedLayoutBackend` (OPC core properties + extended `Application`; OPC has no producer field, so that value is not representable) | ✅ (`DocxSemanticBackend` via `SemanticExportContext`) |
| Watermark (front/back layers) | ✅ `PdfWatermarkRenderer` | ✅ `PptxChromeRenderer` (per-slide shape at the PDF placement math; behind-content applies before fragments, so no z-order surgery) | ❌ |
| Repeating headers / footers | ✅ `PdfHeaderFooterRenderer` | ✅ `PptxChromeRenderer` (positioned per-slide text boxes; `{page}` / `{pages}` / `{date}` tokens with the numbering window rules) | ❌ |
| Protection / encryption | ✅ `PdfDocumentPostProcessor` | ❌ (ignored with a one-time warning — no OOXML encryption support planned) | ❌ |
| Viewer preferences | ✅ `applyViewerPreferences` in `PdfFixedLayoutBackend` | ❌ (ignored with a one-time warning — PDF-viewer concept) | n/a |
| Debug guide lines / node labels | ✅ `PdfGuideLinesRenderer`, `PdfNodeLabelRenderer` | ❌ (ignored with a one-time warning — render through the PDF backend to see overlays) | n/a |

## Output surface and lifecycle

| Capability | PDF (fixed) | PPTX (fixed) | DOCX (semantic) |
|---|---|---|---|
| Render to bytes / stream / file (`FixedLayoutRenderer`) | ✅ `PdfFixedLayoutBackend` | ✅ `PptxFixedLayoutBackend` | ✅ `DocxSemanticBackend` (`SemanticBackend<byte[]>`) |
| Render to images (`renderToImages`) | ✅ PDFBox `PDFRenderer` | ❌ (throws with a pointer to the PDF backend — POI's slide rasterizer cannot honour embedded fonts, and the PDF raster of the same graph is the canonical image output) | ❌ |
| Raster-slide mode — every page as one full-slide picture, pixel-exact to the PDF/PNG output (`Builder.rasterSlides(dpi)`) | n/a (the PDF raster is the source) | ✅ `PptxFixedLayoutBackend` | ❌ |
| Multi-section documents (`renderSections`, per-section chrome, cross-section links) | ✅ `buildSectionsDocument` in `PdfFixedLayoutBackend` | ⚠️ `renderSections` in `PptxFixedLayoutBackend` (a deck carries one slide size, so every section must share the same page canvas — differing sizes throw) | ❌ |
| Deterministic output (render twice → identical bytes) | ✅ `PdfDeterminismWriter` | ✅ `PptxDeterminismWriter` (pinned OPC created/modified + zip entry-time normalization) | ❌ |
| ServiceLoader discovery (`FixedLayoutBackendProvider`) | ✅ `PdfFixedLayoutBackendProvider` (`format() == "pdf"`) | ✅ `PptxFixedLayoutBackendProvider` (`format() == "pptx"`) | n/a (semantic SPI: `SemanticBackend`) |
| `DocumentSession` convenience methods | ✅ `buildPdf` / `writePdf` / `toPdfBytes` / `toImages` | ✅ `buildPptx` / `writePptx` / `toPptxBytes` (resolved via `BackendProviders.fixedLayout("pptx")`; session chrome applies; fails with `MissingBackendException` naming `graph-compose-render-pptx` when the backend is absent) | via `session.export(new DocxSemanticBackend(...))` |

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
