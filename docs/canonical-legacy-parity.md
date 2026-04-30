# Canonical Legacy-Parity Matrix

This matrix tracks practical authoring parity for the canonical session-first API.
It is a living planning document (last refreshed for v1.5), not a request to
expose the old low-level authoring model.

Public application code should start with:

`GraphCompose.document(...) -> DocumentSession -> DocumentDsl -> layout graph -> PdfFixedLayoutBackend`

`EntityManager` and raw engine builders remain internal, test-support, or
compatibility concerns. New authoring features should be added through
`com.demcha.compose.document.*`, not through low-level entity assembly.

## Status Legend

| Status | Meaning |
| --- | --- |
| Done | Available through canonical public API and covered by tests. |
| Partial | Available for a common case, but not full authoring parity. |
| Planned | Accepted for canonical API design, not implemented yet. |
| Internal only | Exists below the public surface and should not be documented as app authoring API. |
| Rejected | Not planned for public canonical authoring. |

## Layout

| Capability | Canonical status | Decision |
| --- | --- | --- |
| Vertical document flow | Done | `pageFlow`, `module`, and `section` remain the default document model. |
| Nested vertical grouping | Done | Use `addSection(...)` or module body blocks. |
| Horizontal rows | Done | Use `addRow(...)` with optional `weights(...)` and `spacing(...)` (or the deprecated `gap(...)` alias); row children must be atomic primitives. |
| Spacers | Done | Use `addSpacer(...)` or `spacer(width, height)` for fixed flow gaps. |
| Child horizontal alignment | Done via `LayerStack` / `ShapeContainer` | Use `LayerStackBuilder.topLeft(...)` … `bottomRight(...)` (nine alignment shortcuts) or `ShapeContainerBuilder` with the same vocabulary; v1.5 also exposes `position(node, offsetX, offsetY, anchor)` for screen-space nudges from an anchor. |
| Child vertical alignment | Done via `LayerStack` / `ShapeContainer` | Same nine alignment anchors cover top, centre, bottom edges. |
| Absolute placement | Rejected for normal authoring | Keep coordinates inside layout and backend internals. Use `LayerStack.position(...)` if you need anchor-plus-offset placement. |

## Visual Primitives

| Capability | Canonical status | Decision |
| --- | --- | --- |
| Filled/stroked card sections | Done | Sections and flows emit background decoration fragments. |
| Rounded cards and shapes | Done | `cornerRadius(...)` is available on flows, sections, modules, and shapes. |
| Per-side borders | Done | Use `borders(DocumentBorders...)` on flows, sections, modules, and rows; rounded corners and per-side mixed strokes are not combined yet. |
| Horizontal divider | Partial | `DividerBuilder` remains a convenience over a thin shape. |
| Arbitrary line path | Done | Use `addLine(...)` for horizontal, vertical, diagonal, and custom lines. |
| Circle/ellipse | Done | Use `addEllipse(...)` or `addCircle(...)` with fill and stroke. |
| Shape-as-container (clipped) | Partial | Use `addCircle(diameter, fill, inside)` / `addEllipse(w, h, fill, inside)` / `addContainer(...)` (canonical `ShapeContainerNode`). PDF backend honours `ClipPolicy.CLIP_PATH` / `CLIP_BOUNDS` / `OVERFLOW_VISIBLE`; DOCX backend cannot express graphics-state path clipping (Apache POI limitation), so it renders the layers inline without the outline frame and logs a one-time `docx.export.shape-container-fallback` capability warning per export pass. Authors who need the outline must export to PDF. |
| Transform (rotate / scale) | Partial | `ShapeContainerBuilder` implements `Transformable<T>` via the `rotate(degrees)` / `scale(uniform)` / `scale(sx, sy)` mixin. PDF backend applies the transform via a graphics-state `cm` matrix that pivots around the outline's geometric centre; DOCX backend ignores the transform and renders the un-transformed geometry. Other shape builders opt in to `Transformable<T>` in a follow-up release. |
| Per-layer z-index | Done via `LayerStack` / `ShapeContainer` | `LayerStackBuilder.layer(node, align, zIndex)` and `ShapeContainerBuilder.layer(node, align, zIndex)` (plus matching `position(...)` overloads with `zIndex`) re-stack layers without re-ordering source. Default `zIndex` is `0` so existing layouts paginate identically. |

## Text

| Capability | Canonical status | Decision |
| --- | --- | --- |
| Paragraph wrapping | Done | Keep wrapping in paragraph layout preparation. |
| Inline text/link runs | Done | Keep as paragraph-level semantic content. |
| Lists | Done | Existing marker and indentation APIs remain canonical. |
| Nested list ergonomics | Planned | Evaluate a nested list builder instead of forcing sections. |
| Auto-size text | Done | Use `ParagraphBuilder.autoSize(maxSize, minSize)` to fit single-line headlines into the resolved inner width. |

## Images

| Capability | Canonical status | Decision |
| --- | --- | --- |
| Fixed width/height image | Done | Existing image builder supports explicit sizing. |
| Natural-size image | Done | Layout can resolve image metadata from source data. |
| Scale helper | Done | Use `scale(...)` when explicit width and height are omitted. |
| Fit modes | Done | Use `fitToBounds(...)` with `DocumentImageFitMode.CONTAIN`, `COVER`, or `STRETCH`. |

## Tables

| Capability | Canonical status | Decision |
| --- | --- | --- |
| Header and rows | Done | Use `TableBuilder.header(...)`, `headerRow(...)` (alias), `row(...)`, and `rows(...)`. |
| Column sizing | Done | Fixed and auto columns are public table values. |
| Cell style | Done | Keep table styling backend-neutral. |
| Column span | Done | `DocumentTableCell.text(...).colSpan(int)` merges cells horizontally. |
| Row span | Done | `DocumentTableCell.text(...).rowSpan(int)` merges cells vertically. The layout layer skips occupied grid positions so authors only specify the cells that aren't covered by a prior spanning cell. Composes with `colSpan`. |
| Zebra rows | Done | `TableBuilder.zebra(odd, even)` (style or colour overload) alternates row fills. Applied lazily at `build()`; explicit `rowStyle(idx, ...)` always wins. |
| Totals row | Done | `TableBuilder.totalRow(values)` appends a row with a default bold + subtle-fill style. `totalRow(style, values)` for a custom look. |
| Repeated header on page break | Done | `TableBuilder.repeatHeader()` repeats the first row at the top of every continuation page; `repeatHeader(int n)` for multi-row headers. |
| Complex cell composition | Planned | Consider only after row/column layout primitives land. |

## PDF Output

| Capability | Canonical status | Decision |
| --- | --- | --- |
| File output | Done | Use `GraphCompose.document(Path).create()` and `buildPdf()`. |
| Stream output | Done | Use `writePdf(OutputStream)`. |
| Byte output | Done | Use `toPdfBytes()`. |
| Guide lines | Done | `guideLines(true)` is available on document builder and session. |
| Metadata/protection/watermark/header/footer | Done | Configure on `DocumentSession` (e.g. `metadata(...)`, `watermark(...)`, `protect(...)`, `header(...)`, `footer(...)`); convenience PDF entrypoints (`buildPdf`, `writePdf`, `toPdfBytes`) honour these options without an explicit backend builder. `PdfFixedLayoutBackend.builder()` remains for advanced cases. |
| DOCX semantic export | Done | Use `session.export(new DocxSemanticBackend())` for paragraph/table/image-aware Word output. Requires `org.apache.poi:poi-ooxml` on the consumer classpath. |
| PPTX semantic export | Planned | Skeleton manifest backend remains; richer slide layout work is scheduled for v1.6+. |
| PDFBox types in session API | Rejected | Keep PDFBox behind the fixed PDF backend. |

## Diagnostics

| Capability | Canonical status | Decision |
| --- | --- | --- |
| Layout graph | Done | Use `DocumentSession.layoutGraph()`. |
| Layout snapshot | Done | Use `DocumentSession.layoutSnapshot()`. |
| Visual test artifacts | Done | Tests write PDFs under `target/visual-tests`. |
| Raw engine snapshot adapters | Internal only | Keep for compatibility and low-level tests only. |
