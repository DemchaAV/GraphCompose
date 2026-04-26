# Canonical Legacy-Parity Matrix

This matrix tracks practical authoring parity for the canonical session-first API.
It is a v1.3 planning document, not a request to expose the old low-level
authoring model.

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
| Horizontal rows | Planned | Add explicit `row(...)` or `columns(...)` for v1.3. |
| Spacers | Done | Use `addSpacer(...)` or `spacer(width, height)` for fixed flow gaps. |
| Child horizontal alignment | Planned | Add container-level alignment without absolute coordinates. |
| Child vertical alignment | Planned | Add after row/columns semantics are stable. |
| Absolute placement | Rejected for normal authoring | Keep coordinates inside layout and backend internals. |

## Visual Primitives

| Capability | Canonical status | Decision |
| --- | --- | --- |
| Filled/stroked card sections | Done | Sections and flows emit background decoration fragments. |
| Rounded cards and shapes | Done | `cornerRadius(...)` is available on flows, sections, modules, and shapes. |
| Per-side borders | Planned | Add a public border-sides value only when recipes need it. |
| Horizontal divider | Partial | `DividerBuilder` remains a convenience over a thin shape. |
| Arbitrary line path | Done | Use `addLine(...)` for horizontal, vertical, diagonal, and custom lines. |
| Circle/ellipse | Done | Use `addEllipse(...)` or `addCircle(...)` with fill and stroke. |

## Text

| Capability | Canonical status | Decision |
| --- | --- | --- |
| Paragraph wrapping | Done | Keep wrapping in paragraph layout preparation. |
| Inline text/link runs | Done | Keep as paragraph-level semantic content. |
| Lists | Done | Existing marker and indentation APIs remain canonical. |
| Nested list ergonomics | Planned | Evaluate a nested list builder instead of forcing sections. |
| Auto-size text | Planned | Treat as a separate constrained feature after row/column layout. |

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
| Header and rows | Done | Use `TableBuilder.header(...)`, `row(...)`, and `rows(...)`. |
| Column sizing | Done | Fixed and auto columns are public table values. |
| Cell style | Done | Keep table styling backend-neutral. |
| Complex cell composition | Planned | Consider only after row/column layout primitives land. |

## PDF Output

| Capability | Canonical status | Decision |
| --- | --- | --- |
| File output | Done | Use `GraphCompose.document(Path).create()` and `buildPdf()`. |
| Stream output | Done | Use `writePdf(OutputStream)`. |
| Byte output | Done | Use `toPdfBytes()`. |
| Guide lines | Done | `guideLines(true)` is available on document builder and session. |
| Metadata/protection/watermark/header/footer | Done | Use `PdfFixedLayoutBackend.builder()` for advanced output. |
| PDFBox types in session API | Rejected | Keep PDFBox behind the fixed PDF backend. |

## Diagnostics

| Capability | Canonical status | Decision |
| --- | --- | --- |
| Layout graph | Done | Use `DocumentSession.layoutGraph()`. |
| Layout snapshot | Done | Use `DocumentSession.layoutSnapshot()`. |
| Visual test artifacts | Done | Tests write PDFs under `target/visual-tests`. |
| Raw engine snapshot adapters | Internal only | Keep for compatibility and low-level tests only. |
