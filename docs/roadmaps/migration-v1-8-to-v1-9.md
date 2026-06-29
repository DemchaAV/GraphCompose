# Migration: v1.8 → v1.9

v1.9 — codenamed **"navigable"** — is **additive only**. Every public
type, method, and behaviour from v1.8 is unchanged: bump the dependency
to `1.9.0` and rebuild with **no source changes required**. Adding public
API is what turns the open cycle into a minor release.

The theme of the release is **in-document navigation** — a rendered PDF
stops being a flat sequence of pages and becomes a document you can move
through: named anchors, internal links, a native clickable table of
contents, "see page N" cross-references, and a bookmark outline, all
emitted as native PDF `GoTo` actions and resolved in a single authoring
pass.

If your application targets v1.8 today, there is nothing to do but
upgrade. The rest of this guide is a tour of what you can now reach for.

## TL;DR

Everything below is `@since 1.9.0` and purely additive — no v1.8 API is
replaced or removed.

| Area | v1.9 addition | Reach for it when |
| --- | --- | --- |
| **In-PDF navigation** | `anchor(...)` targets + internal `link(...)`; sealed `DocumentLinkTarget` (`InternalLinkTarget` / `ExternalLinkTarget`) | clickable cross-references and `[text](#heading)`-style jumps emitted as native PDF `GoTo` actions |
| **Table of contents** | `addTableOfContents(toc -> toc.entry(label, anchor))` + `TocBuilder` / `DocumentLeader` | a native clickable TOC with dot leaders and auto-resolved page numbers |
| **Page references** | `addPageReference(anchor)` + `PageReferenceNode`; `DocumentSession.pageIndex()` → `PageIndex` / `PageReference` | a "see page N" cross-reference, or resolving any anchor to its page from code |
| **Bookmarks & viewer prefs** | container `bookmark(DocumentBookmarkOptions)`; `chrome().viewerPreferences(...)` + `DocumentViewerPreferences` / `DocumentPageMode` / `DocumentPageLayout` | a PDF outline (bookmark panel), and controlling how a reader opens the document |
| **Multi-section documents** | `GraphCompose.documents()` + `MultiSectionDocumentBuilder` / `MultiSectionDocument` | concatenating independently authored sections (own page size / margins / numbering) into one PDF, with links resolving across sections |
| **Per-page margins & bleed** | `pageMargins(List<PageMarginRule>)`; `bleed(...)` + `DocumentBleed` / `DocumentEdge` | mixing a full-bleed cover with book-margin body pages, or running content to the trimmed page edge |
| **Row layout** | `row.columns(auto() / weight() / fixed())` + `DocumentRowColumn`; `flexSpacer()` / `pushRight()` / `arrangement(...)` + `RowArrangement`; `verticalAlign(...)` + `RowVerticalAlign` | column tracks, main-axis justify (push a badge flush right), and cross-axis seating within a row |
| **Inline chips / SVG / emoji** | inline highlight chips, inline `SvgIcon` runs, `RichText.emoji(":star:", size)` via the new `graph-compose-emoji` module | a code/badge chip, an icon on the text baseline, or colour emoji by shortcode |
| **Page numbering** | `DocumentPageNumbering` / `DocumentPageNumberStyle` | page-number offset / restart / numeral style in headers and footers |
| **Lines** | `LineBuilder.fill()`; `LineBuilder.lineCap(DocumentLineCap)` | a line that stretches to its slot (dot leaders), and round / square caps or dotted strokes |
| **Render to images** | render a `DocumentSession` straight to `BufferedImage` | a raster preview / thumbnail with no PDF round-trip |

Runnable code for each lives in the
[examples gallery](../../examples/README.md); the exact public-API list
is in [`CHANGELOG.md`](../../CHANGELOG.md) under **v1.9.0**.

## One behaviour to know about

- **A negative page margin is now rejected; a negative bottom *content*
  margin is honoured.** A negative `margin(...)` on the page fails fast at
  construction instead of silently mis-laying-out the page, while an
  intentional negative bottom margin on a block now pulls the following
  content up as written. This only affects code that passed a negative
  *page* margin — pass a non-negative one.

## Things that did NOT break

- Every entry point on `GraphCompose`, the full `DocumentSession`
  authoring lifecycle (`compose`, `pageFlow`, add, `buildPdf`, export,
  close, `layoutGraph`, `layoutSnapshot`).
- `DocumentDsl`, `BusinessTheme`, `DocumentPalette`, and the invoice /
  proposal / CV / cover-letter / weekly-schedule template entry points
  (V1 and V2).
- The fixed-layout and semantic backend SPIs and every public render
  handler.
- Layout snapshots and visual-regression baselines — navigation markers
  are non-visual, so a document that adds no anchors, links, or bookmarks
  renders byte-for-byte as before.

> **Deprecations (informational, not breaking).**
> `templates.api.CoverLetterTemplate` and the `cv.v2.components`
> `HeadlineRenderer` / `ContactRenderer` / `BannerRenderer` shims are now
> `@Deprecated(forRemoval = true, since = "1.9.0")` — superseded by the
> generic `DocumentTemplate` seam and the `cv.v2.widgets` widgets
> respectively. They still compile and behave exactly as before in 1.x;
> they are slated for removal in 2.0. See
> [`docs/api-stability.md`](../api-stability.md) §3 for the ledger.

## Upgrading

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose</artifactId>
    <version>1.9.0</version>
</dependency>
```

That is the entire migration. Pull in any of the primitives above as you
need them.
