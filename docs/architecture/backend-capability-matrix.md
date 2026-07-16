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
  `com.demcha.compose.document.backend.fixed.pptx` (in development).
  Consumes the same resolved `LayoutGraph`; geometry-identical to PDF by
  construction, since layout is compiled in core before any backend runs.
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
| Paragraph — pre-wrapped lines, runs, alignment (`ParagraphFragmentPayload`) | ✅ `PdfParagraphFragmentRenderHandler` | 🚧 | ✅ semantic paragraphs (`DocxSemanticBackend`) |
| Inline code/badge chips (`InlineBackground` on text spans) | ✅ `PdfParagraphFragmentRenderHandler` | 🚧 | ❌ |
| Inline images (`ParagraphImageSpan`) | ✅ `PdfParagraphFragmentRenderHandler` | 🚧 | ❌ |
| Inline vector shapes (`ParagraphShapeSpan`) | ✅ `PdfParagraphFragmentRenderHandler` | 🚧 | ❌ |
| Inline SVG (`ParagraphSvgSpan`) | ✅ `PdfParagraphFragmentRenderHandler` + `PdfPathPainter` | 🚧 | ❌ |
| Rectangle shape — fill, stroke, per-corner radii, side borders (`ShapeFragmentPayload`) | ✅ `PdfShapeFragmentRenderHandler` | 🚧 | ❌ |
| Ellipse (`EllipseFragmentPayload`) | ✅ `PdfEllipseFragmentRenderHandler` | 🚧 | ❌ |
| Line — dash pattern, line cap (`LineFragmentPayload`) | ✅ `PdfLineFragmentRenderHandler` | 🚧 | ❌ |
| Polygon (`PolygonFragmentPayload`) | ✅ `PdfPolygonFragmentRenderHandler` | 🚧 | ❌ |
| Free path — segments, dash, cap, join (`PathFragmentPayload`) | ✅ `PdfPathFragmentRenderHandler` + `PdfPathPainter` | 🚧 | ❌ |
| Linear gradient fill (`DocumentPaint`) | ✅ `PdfShadingSupport` | 🚧 | ❌ |
| Radial gradient fill (`DocumentPaint`) | ✅ `PdfShadingSupport` | 🚧 (approximation expected — DrawingML cannot express radius-to-farthest-corner exactly) | ❌ |
| Gradient strokes | ✅ `PdfPathPainter` (pattern stroking colour) | 🚧 | ❌ |
| Image — STRETCH / CONTAIN / COVER fit (`ImageFragmentPayload`) | ✅ `PdfImageFragmentRenderHandler` | 🚧 | ✅ semantic images (`DocxSemanticBackend`) |
| Barcode / QR (`BarcodeFragmentPayload`) | ✅ `PdfBarcodeFragmentRenderHandler` (ZXing raster) | 🚧 | ❌ |
| Table rows — resolved cells, row/col spans, two-pass fill/border paint (`TableRowFragmentPayload`) | ✅ `PdfTableRowFragmentRenderHandler` + row grouping in `PdfFixedLayoutBackend` | 🚧 | ✅ semantic tables (`DocxSemanticBackend`) |
| Clip region open/close (`ShapeClipBegin/EndPayload`) | ✅ `PdfShapeClipBegin/EndRenderHandler` (CLIP_BOUNDS + CLIP_PATH) | 🚧 (rect crops for pictures; other content degrades with a one-time warning — PPTX has no graphics-state clipping) | ⚠️ inline fallback + one-time capability warning |
| Transform open/close — rotate/scale about fragment centre (`TransformBegin/EndPayload`) | ✅ `PdfTransformBegin/EndRenderHandler` | 🚧 (group shape with `xfrm`) | ⚠️ inline fallback + one-time capability warning |
| Anchor markers (`AnchorMarkerPayload`) | ✅ `PdfAnchorMarkerRenderHandler` + `PdfInternalLinkWriter` | 🚧 | ❌ |
| Bookmark markers (`BookmarkMarkerPayload`) | ✅ `PdfBookmarkMarkerRenderHandler` + `PdfBookmarkOutlineWriter` | 🚧 (PPTX has no document outline; mapped to slide names where 1:1) | ❌ |
| Alpha / opacity | ✅ `PdfAlphaSupport` (`PDExtendedGraphicsState`) | 🚧 (native fill alpha) | ❌ |

## Navigation and interactivity

| Capability | PDF (fixed) | PPTX (fixed) | DOCX (semantic) |
|---|---|---|---|
| External hyperlinks (fragment- and run-level) | ✅ `PdfLinkAnnotationWriter` + link rects in `PdfFixedLayoutBackend` | 🚧 (`XSLFHyperlink`) | ❌ |
| Internal links (anchor jump, forward references) | ✅ `PdfInternalLinkWriter` (two-pass) | 🚧 (slide-jump hyperlinks) | ❌ |
| Document outline / bookmarks tree | ✅ `PdfBookmarkOutlineWriter` | 🚧 (no PPTX outline concept — slide names where 1:1, otherwise dropped with a note) | ❌ |

## Document chrome and output options

Neutral options come from `DocumentOutputOptions`; a backend that cannot
honour an option ignores it (documented contract).

| Capability | PDF (fixed) | PPTX (fixed) | DOCX (semantic) |
|---|---|---|---|
| Metadata (title, author, …) | ✅ `PdfDocumentPostProcessor` | 🚧 (OPC core properties) | ✅ (`DocxSemanticBackend` via `SemanticExportContext`) |
| Watermark (front/back layers) | ✅ `PdfWatermarkRenderer` | 🚧 (per-slide shape, send-to-back/front) | ❌ |
| Repeating headers / footers | ✅ `PdfHeaderFooterRenderer` | 🚧 (positioned per-slide text boxes) | ❌ |
| Protection / encryption | ✅ `PdfDocumentPostProcessor` | ❌ (ignored — no OOXML encryption support planned) | ❌ |
| Viewer preferences | ✅ `applyViewerPreferences` in `PdfFixedLayoutBackend` | ❌ (ignored — PDF-viewer concept) | n/a |
| Debug guide lines / node labels | ✅ `PdfGuideLinesRenderer`, `PdfNodeLabelRenderer` | 🚧 | n/a |

## Output surface and lifecycle

| Capability | PDF (fixed) | PPTX (fixed) | DOCX (semantic) |
|---|---|---|---|
| Render to bytes / stream / file (`FixedLayoutRenderer`) | ✅ `PdfFixedLayoutBackend` | 🚧 `PptxFixedLayoutBackend` | ✅ `DocxSemanticBackend` (`SemanticBackend<byte[]>`) |
| Render to images (`renderToImages`) | ✅ PDFBox `PDFRenderer` (in-memory, no round-trip) | 🚧 (decision pending: POI `XSLFSlide.draw` quality) | ❌ |
| Multi-section documents (`renderSections`, per-section chrome, cross-section links) | ✅ `buildSectionsDocument` in `PdfFixedLayoutBackend` | 🚧 | ❌ |
| Deterministic output (render twice → identical bytes) | ✅ `PdfDeterminismWriter` | 🚧 (pinned OPC timestamps + zip normalization) | ❌ |
| ServiceLoader discovery (`FixedLayoutBackendProvider`) | ✅ `PdfFixedLayoutBackendProvider` (`format() == "pdf"`) | 🚧 (`format() == "pptx"`) | n/a (semantic SPI: `SemanticBackend`) |
| `DocumentSession` convenience methods | ✅ `buildPdf` / `writePdf` / `toPdfBytes` / `toImages` | 🚧 (`buildPptx` / `writePptx` / `toPptxBytes` planned) | via `session.export(new DocxSemanticBackend(...))` |

## Fidelity notes (PPTX)

Geometry identity with PDF is guaranteed by construction at the
shape-frame level: both backends consume the same resolved `LayoutGraph`,
and text is emitted as one absolutely-positioned, wrap-disabled text box
per already-wrapped line, so PowerPoint can never reflow content. What
PPTX cannot guarantee is glyph-level rasterization: PowerPoint draws text
with the fonts installed on the viewing machine. Standard-14 font names are mapped to
metric-compatible system fonts (Helvetica → Arial, Times → Times New
Roman, Courier → Courier New); binary font families keep their real
family names and should be installed on the viewer for exact glyphs.
