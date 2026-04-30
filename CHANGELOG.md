# Changelog

## v1.5.0-beta.13 (in progress) - Phase E.4 (slice 2): HTTP streaming example

E.4 continues. This slice ships the canonical "render PDF straight to
an output stream" example — the pattern most production adopters reach
for first.

### Examples

- New runnable
  `examples/.../HttpStreamingExample.java` isolates the streaming code
  path into a single
  `streamInvoiceTo(InvoiceDocumentSpec, OutputStream)` method so
  Servlet / S3 / GCS adopters can copy-paste the body. The class
  javadoc shows the matching Spring Boot `@RestController` snippet.
- The example's `main` runs the same code path against
  `ByteArrayOutputStream`, then writes the captured bytes to disk —
  output: `examples/target/generated-pdfs/invoice-http-stream.pdf`.
  Hooked into `GenerateAllExamples`.

### Tests

- New `HttpStreamingDemoTest` (2 cases) pins the streaming contract:
  - `writePdf(OutputStream)` produces a valid PDF and **does not**
    close the caller's stream (mandatory for the Servlet response
    pattern). Verified by a `TrackingOutputStream` that records
    whether `close()` was called.
  - `writePdf(OutputStream)` and `toPdfBytes()` produce equally-sized
    documents with identical PDF version headers for the same input.
    Byte-for-byte equality is not achievable (PDFBox stamps every
    render with a fresh `/ID` UUID), but the length-and-header check
    proves both code paths route through the same renderer.
- Stream output PDF lives at
  `target/visual-tests/http-streaming/invoice-http-stream.pdf` for
  reviewer inspection.

---

## v1.5.0-beta.12 (in progress) - Phase E.4 (slice 1): custom BusinessTheme example

E.4 lifts the runnable-examples module to the v1.4 bar. This slice adds
the first of three remaining showpieces — a hand-built `BusinessTheme`
that proves every visible token on `InvoiceTemplateV2` is theme-driven.

### Examples

- New runnable
  `examples/.../CustomBusinessThemeExample.java` constructs a
  "Studio Emerald" theme from raw `DocumentPalette`, `SpacingScale`,
  `TextScale`, and `TablePreset` records (no factory shortcut), then
  pipes `ExampleDataFactory.sampleInvoice()` through
  `InvoiceTemplateV2`. Output: `examples/target/generated-pdfs/
  invoice-custom-theme.pdf`. Hooked into `GenerateAllExamples`.
- The example doubles as a copy-and-paste starter for projects that
  want to brand GraphCompose output for their own product.

### Tests

- New `CustomBusinessThemeDemoTest` (3 cases): renders the same
  invoice once with `BusinessTheme.modern()` and once with the
  hand-built Studio Emerald theme, validates both files are well-
  formed PDFs, and asserts the custom theme is accepted by
  `InvoiceTemplateV2`. Output PDFs land under
  `target/visual-tests/custom-business-theme/` for side-by-side
  review.

---

## v1.5.0-beta.11 (in progress) - Phase E.3: CvTheme ↔ BusinessTheme bridge

E.3 takes a phased approach to theme unification, captured in the new
[ADR 0002](docs/adr/0002-theme-unification.md). Rather than collapse
the two theme types under a common interface (rejected — it loses the
CV-specific vocabulary that ten templates rely on), we add a thin
adapter so projects that already chose a `BusinessTheme` for their
invoices and proposals can derive a visually-matching `CvTheme`
without re-stating the visual tokens.

### Public API

- New `CvTheme.fromBusinessTheme(BusinessTheme)` static factory
  derives a CV theme from a business theme. The bridge maps the
  business palette / text-scale slots into the CV-specific tokens:
  - `primaryColor`   ← `palette().primary()`
  - `secondaryColor` ← `palette().accent()`
  - `bodyColor`      ← `palette().textPrimary()`
  - `accentColor`    ← `palette().accent()`
  - `headerFont`     ← `text().h1().fontName()`
  - `bodyFont`       ← `text().body().fontName()`
  - `nameFontSize`   ← `text().h1().size()`
  - `headerFontSize` ← `text().h2().size()`
  - `bodyFontSize`   ← `text().body().size()`
  - CV-specific layout tokens (`spacing`, `modulMargin`,
    `spacingModuleName`) keep the existing CV defaults.
- Existing `CvTheme.defaultTheme()` / `timesRoman()` / `courier()`
  factories are unchanged. The ten existing CV templates and the
  `CvTemplateV1` composer continue to work without code changes.

### Documentation

- New `docs/adr/0002-theme-unification.md` records the analysis,
  the rejected alternative ("Option B: common Theme interface"),
  and the migration plan.
- `docs/recipes/themes.md` gets a new "Sharing themes with CV
  templates" section that shows the bridge in use, and an
  "Layered themes for invoices and proposals" section refreshed for
  the V2 templates that landed in E.1 / E.2.

### Tests

- New `CvThemeBusinessThemeAdapterTest` pins the bridge mapping:
  colours follow the business palette slots, fonts and sizes follow
  the business text scale, three different business themes produce
  three distinct CV themes, and the CV-specific layout tokens
  (`spacing`, `moduleMargin`, `spacingModuleName`) match the CV
  defaults.

### Out of scope for v1.5

- Mass migration of the ten CV templates to a `BusinessTheme`-driven
  composer. Templates can opt in incrementally when they're touched
  for unrelated reasons.

---

## v1.5.0-beta.10 (in progress) - Phase E.2: cinematic proposal template

E.2 lands `ProposalTemplateV2`, the canonical-DSL proposal counterpart
to `InvoiceTemplateV2`. Same `ProposalDocumentSpec` data renders in
any of the three built-in `BusinessTheme` themes by passing the theme
to the constructor.

### Public API

- New `com.demcha.compose.document.templates.builtins.ProposalTemplateV2`
  implementing the existing `ProposalTemplate` interface. The no-arg
  constructor picks `BusinessTheme.modern()`; the one-arg
  `ProposalTemplateV2(BusinessTheme)` accepts any theme.
- `ProposalTemplateV1` is unchanged — both ship side by side.

### Visual composition

- Hero soft panel rounded only on the right (via Phase E.1.1
  `DocumentCornerRadius.right(...)`), with the title, project title,
  and proposal-number / prepared / valid-until row read out as inline
  rich text.
- Themed executive-summary panel with line-spaced body text.
- Sender / recipient parties row using `theme.text().label()` and
  line-spaced `body()` styles for the address block.
- Sections rendered as `theme.text().h2()` headings + body paragraphs.
- Timeline table (Phase / Duration / Details) and pricing table
  (Item / Description / Amount) both use `headerStyle` on row 0,
  `repeatHeader()` for pagination, and zebra rows on the pricing
  table. The emphasized total-pricing row is anchored at the bottom
  via `TableBuilder.totalRow(style, values)`.
- Acceptance terms list with `accentLeft` strip; footer note with
  top margin so it breathes off the body content.

### Tests + examples

- New `ProposalTemplateV2Test` pins five invariants — default
  constructor uses modern theme; sample proposal produces a valid
  PDF byte stream; modern vs classic produces distinct bytes (theme
  switch observable); the layout graph contains both
  `ProposalTimeline` and `ProposalPricing` table nodes.
- New `ProposalTemplateV2DemoTest` renders the same sample proposal
  with each of the three built-in themes
  (`modern` / `classic` / `executive`) to PDF artefacts under
  `target/visual-tests/proposal-template-v2/`.
- New `examples/.../ProposalCinematicFileExample.java` (hooked into
  `GenerateAllExamples`) renders the standard sample proposal with
  the modern theme to
  `examples/target/generated-pdfs/proposal-cinematic.pdf`.

---

## v1.5.0-beta.9 (in progress) - Phase E.1: cinematic invoice template

E.1 lands `InvoiceTemplateV2`, the first canonical template that
composes against the canonical DSL using a `BusinessTheme` for every
visual choice. Same `InvoiceDocumentSpec` data renders in any of the
three built-in themes (or a custom one) by passing it to the
constructor.

### Public API

- New `com.demcha.compose.document.templates.builtins.InvoiceTemplateV2`
  implementing the existing `InvoiceTemplate` interface. Two
  constructors: the no-arg form picks `BusinessTheme.modern()`; the
  one-arg `InvoiceTemplateV2(BusinessTheme)` accepts any theme.
- `InvoiceTemplateV1` is unchanged — both templates ship side by
  side. Authors who want the cinematic look opt in by switching the
  type; nothing else has to change.

### Visual composition

The template stacks the v1.4 / v1.5 cinematic primitives:

- `softPanel` hero block carrying the invoice number, dates, and the
  status read out as inline rich text via the new
  `DocumentDsl.richText` callback.
- A two-column row with `From` / `Bill to` parties, each rendered as
  a small section using the theme's `text().label()` and
  `text().body()` styles.
- A line-items table with header style on the first row, zebra
  alternation on the body, the totals row anchored at the bottom via
  `TableBuilder.totalRow(style, values)`, and `repeatHeader()` so the
  totals header re-emits on every continuation page when the invoice
  paginates.
- A footer row with `accentLeft` accent strips on the notes / payment
  terms columns.

Every visual style is derived from `BusinessTheme` — palette,
text scale, stroke colour, fill colours.

### Tests + examples

- New `InvoiceTemplateV2Test` covers four invariants:
  - Default constructor uses `BusinessTheme.modern()` and the
    template id is stable.
  - `compose(...)` produces a valid PDF byte stream for the standard
    sample invoice.
  - The same invoice rendered with `BusinessTheme.modern()` and
    `BusinessTheme.classic()` produces DIFFERENT byte streams (the
    theme colours get embedded in the PDF), so a theme switch is
    observable downstream.
  - The resulting layout graph contains a node whose semantic name
    contains `InvoiceLineItems` — anchor invariant that proves the
    template ran the table composition, not just the hero block.
- New `InvoiceTemplateV2DemoTest` renders the same sample invoice
  with each of the three built-in themes
  (`modern` / `classic` / `executive`) to PDF artefacts under
  `target/visual-tests/invoice-template-v2/` so a reviewer can flip
  through the three side-by-side.
- New `examples/.../InvoiceCinematicFileExample.java` (hooked into
  `GenerateAllExamples`) renders the standard sample invoice with
  the modern theme to
  `examples/target/generated-pdfs/invoice-cinematic.pdf`.

### Known limitations

- The `InvoiceLineItem.details` field is omitted from the rendered
  table cell. Including it would force the auto-sized description
  column to measure against the much longer "details" sentence and
  overflow the inner page width on typical A4 invoices. Templates
  that need the details alongside should compose them in a separate
  notes column or section.

---

## v1.5.0-beta.8 (in progress) - Phase D wrap-up: tables recipe + runnable example

D.4 closes Phase D with author-facing documentation and a runnable
demo for the four advanced-table features that landed in D.1 / D.2 /
D.3 (row span, zebra rows, totals row, repeating header).

### Documentation

- New recipe `docs/recipes/tables.md` covers row span (with note on
  composition with `colSpan`), zebra row alternation (single and
  two-arg overloads, precedence rule), totals row (default + custom
  styles), and repeated header on page break (single-row + multi-row
  variants). Includes a "Layout invariants you can rely on" section
  pinning the five test invariants from `TableBuilderRowSpanTest`,
  `TableBuilderZebraAndTotalsTest`, and
  `TableBuilderRepeatHeaderTest`.

### Examples

- New `examples/.../TableAdvancedExample.java` (hooked into
  `GenerateAllExamples`) renders three sections to one PDF:
  hero callout, a small row-span demo (Q1/Q2/Q3 with a 3-row spanning
  side note), and a 36-row invoice with bold-on-teal repeating header,
  zebra body rows, and a gold totals row at the bottom. Output lands
  at `examples/target/generated-pdfs/table-advanced.pdf`.

---

## v1.5.0-beta.7 (in progress) - Phase D continues: repeated header on page break

D.3 lands repeated-header pagination for tables. When a table is split
across pages the layer compiler now re-emits the configured leading
rows at the top of every continuation page, so long invoices and
reports don't lose their column titles partway through.

### Public API

- `TableBuilder.repeatHeader()` — repeats the first row at the top of
  every continuation page (equivalent to `repeatHeader(1)`).
- `TableBuilder.repeatHeader(int rowCount)` — repeats the first
  `rowCount` rows. `0` disables the feature; the builder defaults to
  `0` so existing tables paginate exactly as before.
- `TableNode` gains a 12th field `int repeatedHeaderRowCount` (default
  `0`). Three back-compat constructors (the original 7-arg, 9-arg, and
  pre-D.3 11-arg signatures) default the new field to `0` so every
  v1.4 / v1.5.0-alpha caller compiles unchanged.

### Architecture

- `TableLayoutSupport.sliceTablePreparedNode` gains a third overload
  with a `prependHeaderRowCount` parameter. When non-zero, it prepends
  rows `[0, prependHeaderRowCount)` from the source table to the body
  slice, producing a tail prepared node whose first
  `repeatedHeaderRowCount` rows are the original header. The new
  measure result accounts for the prepended rows so the page-breaker
  reserves enough room.
- `TableDefinition.split` honours `repeatedHeaderRowCount`:
  - It still fits as many rows as possible into the remaining height
    (header + body counted together), but rejects splits that would
    fit ONLY the header rows (no body progress would loop forever).
  - The tail slice is built with `prependHeaderRowCount = headerCount`,
    so it carries the header at the top. Each subsequent split on
    that tail again preserves the prefix invariant.
- The change is isolated to the table-specific code path; the generic
  splittable-leaf compiler in `LayoutCompiler` is unchanged.

### Tests

- New `TableBuilderRepeatHeaderTest` covers three scenarios:
  - 60-row long table with `repeatHeader()` — every page's first
    table-row fragment must have "Item" in its left cell.
  - `repeatHeader(2)` repeats both a title row and the column-header
    row on every continuation page.
  - `repeatHeader` defaults to `0`, so a table without the call still
    paginates exactly as before — the second page starts with a data
    row, not the header.
- New `TableRepeatHeaderDemoTest` renders a 50-row invoice with
  zebra rows + repeating "Item / Qty / Amount" header to
  `target/visual-tests/table-repeat-header/long-invoice.pdf`.

---

## v1.5.0-beta.6 (in progress) - Phase D continues: zebra rows, totals row, header alias

D.2 lands three table-builder shortcuts that cover the most common
"rendered-report" styling patterns: alternating row fills (`zebra`),
a totals row appended at the bottom (`totalRow`), and a naming alias
for the existing `header(...)` method that reads consistently with
`totalRow(...)` (`headerRow`).

### Public API

- `TableBuilder.zebra(DocumentTableStyle odd, DocumentTableStyle even)`
  configures alternating row styles. Odd-indexed rows (0, 2, 4 — first,
  third, fifth visually) take the {@code odd} style; even-indexed rows
  (1, 3, 5) take the {@code even} style. Either argument may be
  {@code null} to skip painting that parity.
- `TableBuilder.zebra(DocumentColor odd, DocumentColor even)` is a
  convenience overload that wraps the colours into fill-only
  {@code DocumentTableStyle} values.
- `TableBuilder.totalRow(String... values)` adds a totals row as the
  last logical row and assigns a default totals style: bold text plus
  a subtle gray-blue fill (RGB 240, 240, 245).
- `TableBuilder.totalRow(DocumentTableStyle style, String... values)`
  is the customisable overload — pass any fill, text style, padding,
  or stroke as the totals row look.
- `TableBuilder.headerRow(String... values)` is a naming alias for
  `header(String...)` so callers writing
  `headerRow(...).row(...).totalRow(...)` keep a parallel vocabulary.

### Architecture

- Zebra is applied lazily at `build()` time — the builder remembers
  the odd / even style and walks the row list only after every other
  row has been added. Existing entries in the {@code rowStyles} map
  (set via `headerStyle(...)`, `rowStyle(idx, ...)`, or
  `totalRow(...)` itself) are NEVER overwritten, so explicit per-row
  styling always wins over zebra alternation.
- `totalRow(...)` adds the row first, then registers the totals style
  at the row's index. The registration happens before zebra is
  applied at `build()` time, so the totals style takes precedence on
  the totals row regardless of zebra parity.

### Tests

- New `TableBuilderZebraAndTotalsTest` covers seven invariants:
  zebra parity assignment, headerStyle wins over zebra, null-style
  parity is skipped, default totals style, totals row + zebra
  precedence, custom totals style, and `headerRow` alias.
- New `TableZebraAndTotalsDemoTest` renders an invoice-style table
  to `target/visual-tests/table-zebra-totals/zebra-invoice.pdf`:
  white-on-teal header, alternating white / pale-blue data rows,
  bold gold totals row.

---

## v1.5.0-beta.5 (in progress) - Phase D kickoff: table row span

D.1 lands canonical row-span support for tables. A cell can declare
`rowSpan(n)` to merge vertically across the next `n - 1` rows; the
layout layer skips occupied grid positions when interpreting subsequent
source rows so authors only specify the cells that are not yet covered
by a prior spanning cell. Existing `colSpan` continues to work and the
two compose freely (a single cell can be both `colSpan(2).rowSpan(3)`).

### Public API

- `DocumentTableCell` gains a fifth field `int rowSpan` (default `1`).
  Four-arg canonical constructor + back-compat 2/3-arg constructors so
  every v1.4 / v1.5.0-alpha caller compiles unchanged.
- New mutator `DocumentTableCell.rowSpan(int)` mirrors the existing
  `colSpan(int)`. Authors compose by chaining:
  `DocumentTableCell.text("Tall").colSpan(2).rowSpan(3)`.

### Architecture

- `TableLayoutSupport` replaces the per-row colSpan-sum check with a
  unified cell-grid pre-pass driven by an occupancy mask. The new
  `buildLogicalRows(node, columnCount)` walks columns left-to-right
  per source row, skips positions already covered by a prior row's
  spanning cell, and consumes the next source cell otherwise.
  Misalignments raise precise diagnostics: missing cell, extra source
  cell, overlapping span, span exceeding remaining rows / columns.
- `LogicalCell` now carries `(startRow, startColumn, colSpan,
  rowSpan, content)` so downstream resolution knows the cell's full
  extent.
- Row-height resolution is two-pass: first pass derives each row's
  height from single-row cells (`rowSpan = 1`); second pass walks
  spanning cells and distributes any deficit (`naturalHeight - sum
  of spanned row heights`) equally across the spanned rows. Single-row
  layouts therefore stay deterministic; spanning layouts grow the
  shortest contributing rows just enough to satisfy the span.
- `TableResolvedCell.height` for a cell with `rowSpan > 1` equals the
  sum of its covered row heights, so the cell visually merges across
  rows when the renderer paints it.
- `TableResolvedCell` gains a fifth field `double yOffset` (default
  `0` via a back-compat 8-arg constructor). Spanning cells use a
  NEGATIVE offset equal to the cumulative height of the rows below
  the starting row so the cell's rectangle extends downward through
  the rows it merges instead of upward beyond the starting row. Both
  PDF row-render handlers (canonical `PdfTableRowFragmentRenderHandler`
  and engine `PdfTableRowRenderHandler`) honour the offset by
  computing `cellY = rowFragment.y() + yOffset`. Without this offset
  spanning cells extended above the table area in PDF coordinates,
  which produced the symptoms reported on the first round of
  visual demos: missing top border on the merged cell and missing
  left borders on the cells right of it (the merged cell's right
  border was drawn at the wrong y range, leaving the visible boundary
  unstroked).
- `buildStylesGrid` propagates the spanning cell's resolved style to
  every `(row, column)` position it occupies, so neighbour-style
  lookups for borders and fill insets correctly detect a single shared
  cell — a spanning cell does not draw an internal horizontal border
  between the rows it merges.
- `resolveColumnCount` now derives the column count from the FIRST
  source row's colSpan sum (which by definition is not affected by
  prior rowSpan). Subsequent rows may have fewer source cells when
  prior rowSpan covers some columns, so they cannot be used to derive
  the count.

### Tests

- New `TableBuilderRowSpanTest` covers four invariants:
  - 2x2 merged top-left cell — width, height, and right-column x
    coordinates line up correctly.
  - Middle-column rowSpan — first row has 3 cells, subsequent rows
    have 2 each (middle is occupied by the spanning cell), and the
    spanning cell's height equals the sum of all three row heights.
  - rowSpan exceeding remaining rows is rejected with a precise
    diagnostic (`rowSpan 3` / `only 2 rows remain`).
  - Overlapping rowSpan is rejected with a precise diagnostic
    (`Row 1` / `extra source cell`).
- `TableBuilderColSpanTest.rowWithMismatchedColSpanSumIsRejectedDuringLayout`
  was updated to assert the new error message wording (the dedicated
  "colSpan sum" check is now subsumed by the cell-grid pre-pass that
  reports the offending row index and surplus cell count).
- New `TableRowSpanDemoTest` renders two scenarios to PDF artefacts
  under `target/visual-tests/table-rowspan/`: a 2x2 merged cell with
  a teal fill, and a tall middle column spanning three rows. Each
  scenario asserts the PDF magic header is intact.

---

## v1.5.0-beta.4 (in progress) - Phase C wrap-up: recipe + runnable example

C.4 closes Phase C with author-facing documentation and a runnable
demo for the transform mixin and per-layer z-index features that
landed in C.1 / C.2 / C.3.

### Documentation

- New recipe `docs/recipes/transforms.md` covers rotation around the
  placement centre, uniform and non-uniform scale (including the
  mirror case via negative scale), the `rotate(...).scale(...)`
  composition rule (each call preserves the unmodified axis), and
  per-layer z-index for overlays. Includes a "Layout invariants you
  can rely on" section that names the three pinned tests so authors
  can trust the determinism guarantees.

### Examples

- New `examples/.../TransformsExample.java` (hooked into
  `GenerateAllExamples`) renders three sections to one PDF:
  three-circle rotate row (15° / -15° / no tilt), three-card scale
  row (`scale(0.7)`, `scale(1.1, 0.85)`, identity), and a z-swap
  stage where a RED square declared first with `zIndex = 10` draws
  on top of a TEAL square declared second with default zIndex.
  Output lands at `examples/target/generated-pdfs/transforms.pdf`.

---

## v1.5.0-beta.3 (in progress) - Phase C continues: per-layer z-index

Phase C.3: introduce explicit per-layer z-index inside
`LayerStackNode` / `ShapeContainerNode`. Layers are stable-sorted by
ascending `zIndex` before render, so a later-declared layer with
`zIndex = 10` draws on top of an earlier-declared layer with
`zIndex = 5`. Default is `0`, so existing layouts and snapshots stay
deterministic without any code changes.

### Public API

- `LayerStackNode.Layer` gains a fifth field, `int zIndex` (default
  `0`). The five-arg canonical constructor is the new shape; the
  existing 1-, 2-, and 4-arg constructors plus the static factories
  default `zIndex = 0`, so all v1.4 / v1.5.0-alpha callers compile
  unchanged.
- `LayerStackBuilder` adds two zIndex overloads:
  `layer(node, align, zIndex)` and
  `position(node, offsetX, offsetY, align, zIndex)`.
- `ShapeContainerBuilder` adds the same pair of overloads.
- The semantic placement / path of each layer stays in source order —
  only the render iteration shifts. Snapshots and architecture-guard
  tests therefore continue to assert the same fragment paths; only
  the order of renderable fragments inside a stack/container changes
  with z-index.

### Architecture

- `BuiltInNodeDefinitions.PreparedStackLayout` gains a fourth list
  `zIndices: List<Integer>`. The new four-arg canonical constructor is
  the source of truth; the previous 3-arg and 1-arg constructors keep
  compiling and default zIndices to all-zero (source order). Both
  `ShapeContainerDefinition` and `LayerStackDefinition` populate the
  new list from each layer.
- `LayoutCompiler.compileStackedLayer` and the STACK branch of
  `compileNodeInFixedSlot` now compute a stable `iterationOrder`
  permutation via a private `stableZIndexOrder(...)` helper before
  iterating the layer list. Stable on ties → equal `zIndex` keeps
  source order.

### Tests

- `ShapeContainerBuilderTest` grows two cases:
  - `higherZIndexLayerRendersOnTopRegardlessOfSourceOrder` — BACK and
    FRONT squares declared in source order BACK then FRONT, but BACK
    carries `zIndex = 10`. The test asserts FRONT's fragment lands
    BEFORE BACK's in the placed-fragment list (rendered first → behind),
    so BACK draws on top.
  - `equalZIndexLayersPreserveSourceOrder` — three equal-zIndex
    layers stay in source order (stable sort).
- New `ShapeContainerZIndexDemoTest` renders two demo scenarios to
  PDF artefacts under `target/visual-tests/shape-container-zindex/`:
  intersecting RED + TEAL squares with z-swap, and a feature card
  with backdrop + gold badge background + white "NEW" label stacked
  via z-index. Both assert the PDF magic header is intact as a smoke
  check.

### Deferred

- Other builders (`ShapeBuilder`, `EllipseBuilder`, `ImageBuilder`,
  `LineBuilder`, `BarcodeBuilder`) opt in to `Transformable<T>`
  in follow-up commits — same pattern as ShapeContainer.

---

## v1.5.0-beta.2 (in progress) - Phase C kickoff: Transform mixin + render

Phase C lands the canonical-surface transform primitive (rotation around
the placement centre and/or scaling). C.1 introduced the public value
type, mixin, and `ShapeContainerBuilder` opt-in; C.2 wires the PDF
backend so the transform actually rotates and scales the rendered
output. Other-builder opt-ins (`ShapeBuilder`, `EllipseBuilder`, etc.)
land in follow-up commits.

### Render pipeline (C.2)

- New marker payload pair on `BuiltInNodeDefinitions`:
  `TransformBeginPayload(transform, ownerPath)` and
  `TransformEndPayload(ownerPath)`. Same architectural pattern as the
  existing `ShapeClipBeginPayload` / `ShapeClipEndPayload` pair —
  emitted from `emitFragments` and `emitOverlayFragments` respectively
  so a single layout pass produces a flat sequence
  `[transform-begin → outline → clip-begin → … layers … → clip-end → transform-end]`.
- New PDF render handlers
  `PdfTransformBeginRenderHandler` and `PdfTransformEndRenderHandler`,
  registered in `PdfFixedLayoutBackend.defaultHandlers()`. The begin
  handler issues `saveGraphicsState() + cm(matrix)` where the matrix is
  derived from `T(cx,cy) · R(θ) · S(sx,sy) · T(-cx,-cy)` so rotation
  and scaling pivot around the outline's geometric centre. The end
  handler issues `restoreGraphicsState()`.
- Convention: `DocumentTransform.rotationDegrees()` is interpreted as
  *clockwise* (matches the engine convention). PDF native rotation is
  counter-clockwise, so the begin handler negates the angle when
  building the `cm` matrix.
- `ShapeContainerDefinition` now emits the transform pair only when
  `transform.isIdentity()` is `false`. The clip pair is independent —
  a container can rotate without clipping (`OVERFLOW_VISIBLE`), clip
  without rotating (the previous default), or both.

### Tests (C.2)

- `ShapeContainerBuilderTest` grows three fragment-ordering cases:
  rotated container brackets everything else with transform begin/end,
  identity transform skips emitting the markers, and
  `OVERFLOW_VISIBLE` + rotation emits transform markers without clip
  markers.
- `ShapeContainerInvariantsTest` adds an architecture-guard
  `everyTransformBeginInArbitraryDocumentHasMatchingEndOnSamePage`
  that mixes transformed and non-transformed containers across clip
  policies and verifies every transform begin/end pair stays balanced
  on the same page (no nesting of the same owner).
- New `ShapeContainerTransformDemoTest` renders three demo scenarios
  (rotated circle, scaled card, rotated+scaled ellipse) to PDF
  artefacts under `target/visual-tests/shape-container-transform/`.
  The PDFs are not pixel-asserted yet — they exist so a reviewer can
  open them and verify the rotation/scaling behaviour visually.
  A graphics-state leak from an unbalanced transform begin/end would
  corrupt the PDF byte stream, so each scenario also asserts the
  `%PDF-` magic header is intact as a smoke check.

### Public API (C.1, recap)

- New value type `com.demcha.compose.document.style.DocumentTransform`
  carries `rotationDegrees`, `scaleX`, `scaleY` plus an
  `isIdentity()` helper. Static factories: `none()` (alias for
  `NONE`), `rotate(deg)`, `scale(uniformFactor)`, `scale(sx, sy)`.
  `withRotation(deg)` / `withScale(sx, sy)` produce updated copies that
  preserve the unchanged axis. Validates that rotation is finite and
  scale factors are finite and non-zero (zero would collapse the
  geometry to a point).
- New mixin interface `com.demcha.compose.document.dsl.Transformable<T>`
  exposes `transform(DocumentTransform)`, `rotate(degrees)`,
  `scale(uniformFactor)`, `scale(sx, sy)` as default methods. Builders
  opt in by implementing two abstract methods: the transform setter and
  `currentTransform()`. The defaults preserve the unmodified axis when
  callers chain `rotate(...).scale(...)`.
- `ShapeContainerBuilder` now implements `Transformable<ShapeContainerBuilder>`,
  so authors can write
  `addCircle(60, brand, c -> c.rotate(15).scale(0.9).center(label))`.
  Default transform is `DocumentTransform.NONE`.
- `ShapeContainerNode` gains a `transform: DocumentTransform` field
  (ninth canonical-constructor parameter, defaulted to
  `DocumentTransform.NONE` by the existing eight-arg compatibility
  constructor). The transform is a render-time concern: the canonical
  layout layer still measures and places the node against its natural
  bounding box, so layout snapshots stay deterministic regardless of
  rotation/scale.

### Public API

- New value type `com.demcha.compose.document.style.DocumentTransform`
  carries `rotationDegrees`, `scaleX`, `scaleY` plus an
  {@code isIdentity()} helper. Static factories: `none()` (alias for
  `NONE`), `rotate(deg)`, `scale(uniformFactor)`, `scale(sx, sy)`.
  `withRotation(deg)` / `withScale(sx, sy)` produce updated copies that
  preserve the unchanged axis. Validates that rotation is finite and
  scale factors are finite and non-zero (zero would collapse the
  geometry to a point).
- New mixin interface `com.demcha.compose.document.dsl.Transformable<T>`
  exposes `transform(DocumentTransform)`, `rotate(degrees)`,
  `scale(uniformFactor)`, `scale(sx, sy)` as default methods. Builders
  opt in by implementing two abstract methods: the transform setter and
  `currentTransform()`. The defaults preserve the unmodified axis when
  callers chain `rotate(...).scale(...)`.
- `ShapeContainerBuilder` now implements `Transformable<ShapeContainerBuilder>`,
  so authors can write
  `addCircle(60, brand, c -> c.rotate(15).scale(0.9).center(label))`.
  Default transform is `DocumentTransform.NONE`.
- `ShapeContainerNode` gains a `transform: DocumentTransform` field
  (ninth canonical-constructor parameter, defaulted to
  `DocumentTransform.NONE` by the existing eight-arg compatibility
  constructor). The transform is a render-time concern: the canonical
  layout layer still measures and places the node against its natural
  bounding box, so layout snapshots stay deterministic regardless of
  rotation/scale.

### Tests

- New `DocumentTransformTest` (9 cases) pins value-type contracts —
  identity, factories, axis-preserving updates, zero-scale rejection,
  non-finite rejection, mirror via negative scale.
- `ShapeContainerBuilderTest` grows five C.1 cases: default identity
  transform, `rotate(...)` shortcut, `scale(uniform)` shortcut,
  rotate+scale composition, and the layout invariant that a transform
  does not shift placement coordinates (placement of a 45°-rotated
  circle equals placement of the same circle without rotation).

### Deferred

- Other builders opt in to `Transformable<T>` later — `ShapeBuilder`,
  `EllipseBuilder`, `ImageBuilder`, `LineBuilder`, `BarcodeBuilder` are
  natural candidates now that C.2 ships and rotate/scale produces
  visible output.
- C.3 — per-entity z-index (`zIndex(int)` + `Layer` ECS-component
  honoured by `EntityRenderOrder`).
- C.4 — recipe + extending the runnable example with rotated/scaled
  scenarios.

---

## v1.5.0-beta.1 (in progress) - Phase B "Shape-as-container" (full pipeline)

Phase B follow-up to alpha.2: lands the actual shape-clipped render path.
The PDF backend now honours `ClipPolicy.CLIP_PATH` (default) and
`CLIP_BOUNDS`, so a circle with a child label clips the label to the
circle's outline. `OVERFLOW_VISIBLE` skips clipping entirely. The DOCX
backend renders a graceful fallback (layers without outline + capability
warning) since Apache POI cannot express graphics-state path clipping.

### Public API

- `ShapeContainerNode` and `ShapeContainerBuilder` now default to
  `ClipPolicy.CLIP_PATH` per ADR §Decision — the natural reading of "add
  a circle with a label inside" is that the label is clipped by the
  circle's outline. Callers who explicitly want axis-aligned bbox
  clipping or no clipping at all set the policy through
  `clipPolicy(...)`.

### Architecture

- `NodeDefinition` gains a default `emitOverlayFragments(...)` hook
  alongside the existing `emitFragments(...)`. Most node types do not
  need it — opening decorations (backgrounds, borders, outlines) already
  render before children. The overlay hook exists for paired begin/end
  markers such as the graphics-state save/restore pair used by
  `ShapeContainerNode`: the clip-begin fragment is emitted via
  `emitFragments` (so it sits behind the children), and the matching
  clip-end fragment via `emitOverlayFragments` (so it sits after the
  children, restoring graphics state on the same page).
- New engine-side payload pair on `BuiltInNodeDefinitions`:
  `ShapeClipBeginPayload` (carries outline geometry + chosen policy +
  owner path) and `ShapeClipEndPayload` (carries owner path). They are
  marker payloads — the begin emits `saveGraphicsState() + add path +
  clip()`, the end emits `restoreGraphicsState()`. Owner path lets
  invariant tests verify that every begin pairs with an end on the
  same page.
- `LayoutCompiler.compileStackedLayer` and
  `LayoutCompiler.compileNodeInFixedSlot` (STACK branch) now invoke
  `definition.emitOverlayFragments(...)` after children are placed and
  append the result to the fragment list. A new
  `compositeOverlayFragments(...)` helper mirrors the existing
  `compositeDecorationFragments(...)` shape so multi-page composites can
  emit per-page overlays if they ever need to. Today only
  `ShapeContainerDefinition` opts in.
- New PDF render handlers in
  `com.demcha.compose.document.backend.fixed.pdf.handlers`:
  `PdfShapeClipBeginRenderHandler` and `PdfShapeClipEndRenderHandler`,
  registered alongside the existing default handlers in
  `PdfFixedLayoutBackend`. The begin handler builds an ellipse,
  rounded-rectangle, or rectangle path (matching the outline kind) and
  applies it as a graphics-state clip; the end handler issues the
  matching `restoreGraphicsState()`.

### DOCX backend

- `DocxSemanticBackend` now recognises `ShapeContainerNode`. Apache POI
  cannot express path clipping, so the backend renders the container's
  layers inline without the outline frame and logs a one-time
  `docx.export.shape-container-fallback` capability warning per export
  pass. The fallback rule is recorded in
  `docs/canonical-legacy-parity.md` under "Surfaces and structure".
  Authors who need the outline must export to PDF.

### Tests

- `ShapeContainerBuilderTest` extended with three fragment-ordering
  tests: default `CLIP_PATH` emits outline → clip-begin → layer →
  clip-end in the right order with matching owner paths;
  `OVERFLOW_VISIBLE` emits no clip markers; multi-container documents
  keep every begin/end pair balanced. Plus a PDF render smoke test that
  ensures the new handlers dispatch cleanly through
  `PdfFixedLayoutBackend.toPdfBytes()`.
- New `DocxSemanticBackendTest.shapeContainerExportsLayersInlineWithoutOutline`
  pins the DOCX fallback contract: layer paragraph text survives,
  outline does not.
- New `ShapeContainerInvariantsTest` (architecture-guard) pins the two
  cross-document invariants: `ShapeContainerNode` placement is
  single-page (`SHAPE_ATOMIC`), and every `ShapeClipBeginPayload` has a
  matching `ShapeClipEndPayload` with the same owner path on the same
  page across an arbitrary mix of policies and outline kinds.

### Documentation

- New recipe `docs/recipes/shape-as-container.md` covers hello-circle,
  layered ellipse with a badge + label, rounded card with `RichText`
  body, the three clip policies, and edge cases (oversized container,
  DOCX fallback). Plus a "How the rendering pipeline emits a clipped
  container" section that documents the fragment sequence so readers
  who hit visual surprises can trace them through the layout layer.
- `docs/canonical-legacy-parity.md` gains a "Shape-as-container
  (clipped)" row explaining that the PDF backend honours all three clip
  policies while DOCX renders layers inline without the outline.

### Deferred

- B.7 (snapshot extension exposing the clip-path geometry directly on
  `PlacedFragment` plus an `assertHasClipPath(...)` helper) is partly
  redundant with the existing `ShapeClipBeginPayload` already carried on
  the placed fragment — the dedicated helper lands later.
- Visual baselines (`circle-with-text`, `ellipse-with-overlay`,
  `rounded-rect-card`) come with later Phase B / Phase F polish.

---

## v1.5.0-alpha.2 (in progress) - Phase B "Shape-as-container"

This release starts Phase B: shape-as-container support on the canonical
surface. The opening slice (B.1, B.2, B.4) lands the public DSL, the new
`ShapeContainerNode` record, and the `SHAPE_ATOMIC` pagination policy. The
PDF clip-path render path (B.5) and snapshot infrastructure (B.7) are still
in flight — the layout compiler currently treats `SHAPE_ATOMIC` like
`ATOMIC` for placement purposes, so children render *atop* the outline but
are not yet clipped to its path.

### Public API

- New semantic node `com.demcha.compose.document.node.ShapeContainerNode`:
  a composite whose bounding box is dictated by a `ShapeOutline` (rectangle,
  rounded rectangle, ellipse, or circle) and that hosts one or more child
  `LayerStackNode.Layer`s. Unlike `LayerStackNode` (where the bbox is
  `max(child outer size)`), the outline drives the size — children can be
  smaller and the container still occupies the full outline.
- New sealed value type `com.demcha.compose.document.style.ShapeOutline`
  with `Rectangle`, `RoundedRectangle`, and `Ellipse` cases plus a
  `circle(diameter)` factory. Validates that dimensions are finite and
  positive at construction time.
- New enum `com.demcha.compose.document.style.ClipPolicy` with
  `CLIP_BOUNDS`, `CLIP_PATH`, `OVERFLOW_VISIBLE` cases. The PDF backend
  will honour `CLIP_PATH` once B.5 lands; today every policy renders the
  outline plus layers without clipping.
- New builder `com.demcha.compose.document.dsl.ShapeContainerBuilder`
  exposing the same nine-point alignment vocabulary as `LayerStackBuilder`
  (`topLeft`/`topCenter`/.../`bottomRight` plus
  `position(node, offsetX, offsetY, anchor)`), plus outline configuration
  (`rectangle(w, h)`, `roundedRect(w, h, radius)`, `ellipse(w, h)`,
  `circle(diameter)`) and `clipPolicy(...)`, `fillColor(...)`,
  `stroke(...)`, `padding(...)`, `margin(...)` setters.
- `AbstractFlowBuilder` gains three new convenience overloads:
  `addContainer(Consumer<ShapeContainerBuilder>)`,
  `addCircle(double diameter, DocumentColor fill, Consumer<ShapeContainerBuilder> inside)`,
  `addEllipse(double w, double h, DocumentColor fill, Consumer<ShapeContainerBuilder> inside)`.
- New pagination policy `PaginationPolicy.SHAPE_ATOMIC`. From the
  page-breaker's perspective it is identical to `ATOMIC` — the outline
  plus every layer move to the next page as one unit — but it lets render
  handlers and snapshots tell "shape-clipped atomicity" apart from
  "bbox-only atomicity". Oversized containers raise the existing
  `AtomicNodeTooLargeException` with the offending semantic name.

### Architecture

- New ADR `docs/adr/0001-shape-as-container.md` records the decision to
  introduce `ShapeContainerNode` as a separate semantic type rather than
  overload `LayerStackNode` with a `clipOutline` flag. The ADR captures
  the alternative considered (clip flag on the existing record), the
  reasons it was rejected (mixed semantics, harder pagination policy,
  public-record signature change for an already-shipped v1.4 type), and
  the implementation order through B.2 → B.10.
- `BuiltInNodeDefinitions` registers the new `ShapeContainerDefinition`.
  Its `prepare(...)` derives the bounding box from `outline.size() +
  padding` (children do not influence the container size). Its
  `emitFragments(...)` materialises the outline as an existing
  `EllipseFragmentPayload` or `ShapeFragmentPayload`
  (RoundedRectangle reuses the rectangle payload with a corner radius), so
  the renderer keeps a single source of truth for shape geometry.

### Tests

- New `ShapeContainerBuilderTest` covers builder validation (missing
  outline, empty layers), bbox derivation from outline (circle and
  rounded-rectangle cases), screen-space `position(...)` offsets, the
  flow-shortcut form (`section.addCircle(diameter, fill, inside)`), and
  pagination invariants under `SHAPE_ATOMIC` (atomic page-break and
  oversized-container rejection).
- `FlowShortcutOverloadsTest` was migrated off the no-arg
  `PageFlowBuilder` constructor (which now requires a `DocumentSession`)
  to `SectionBuilder`, since the test exercises shortcut overloads on the
  shared `AbstractFlowBuilder` parent.

### Deferred

- B.3 (layout shape geometry pre-pass + `ShapeClipPath` engine
  component), B.5 (PDF clip-path render via graphics state), B.6 (DOCX
  fallback), B.7 (snapshot extension for `clipPath`), B.8
  (architecture-guard tests), B.9 (recipe + runnable example) and B.10
  (wrap-up) remain in flight.

---

## v1.5.0-alpha.1 (in progress) - Phase A "Quick UX wins"

This is the first slice of the v1.5 "Intuitive" release. Phase A only adds
fluent shortcuts and renames; no public record signatures change for builders
that survive — the one record extension lands in `LayerStackNode.Layer`, but
its old constructors stay backward-compatible.

### Public API

- `LayerStackBuilder` now exposes nine alignment shortcuts (`topLeft`,
  `topCenter`, `topRight`, `centerLeft`, `center`, `centerRight`, `bottomLeft`,
  `bottomCenter`, `bottomRight`) on top of the existing `back`/`center` helpers.
  Discoverable from autocomplete instead of forcing callers to remember the
  full `LayerAlign` enum.
- `LayerStackBuilder.position(node, offsetX, offsetY, anchor)` nudges a layer
  from its anchor by an on-screen offset (positive `offsetX` = right, positive
  `offsetY` = down). `LayerStackNode.Layer` gains `offsetX` / `offsetY`
  components; the two existing constructors (`Layer(node)`, `Layer(node, align)`)
  default both to `0.0` so existing callers compile unchanged.
- `AbstractFlowBuilder` gains five convenience overloads for the most common
  cases: `addShape(w, h, fill)`, `addEllipse(diameter, fill)`,
  `addEllipse(w, h, fill)`, `addCircle(diameter, fill)`,
  `addImage(data, w, h)`. Sugar over the existing builder-callback signatures.
- `RowBuilder.spacing(double)` is now the canonical name for horizontal child
  spacing. `RowBuilder.gap(double)` remains as a `@Deprecated(since = "1.5.0")`
  alias that delegates to `spacing(...)`. CV templates and runnable examples
  were migrated to the new name.
- `RowBuilder.add(node)` now validates the child type **eagerly** and throws
  `IllegalArgumentException` from the offending call site — instead of waiting
  until `build()` and reporting `IllegalStateException` later. Existing tests
  that asserted the deferred `IllegalStateException` were updated.
- `DocumentDsl.richText(Consumer<RichText>)` is a new callback entry point
  that builds a `RichText` run sequence in one fluent call alongside the rest
  of the DSL builders.

### Architecture

- `BuiltInNodeDefinitions.PreparedStackLayout` now carries per-layer
  `offsetsX` / `offsetsY` lists in addition to the existing `alignments`. A
  backward-compatible single-arg constructor fills both with zeros.
- `LayoutCompiler.compileStackedLayer` honours layer offsets after applying
  alignment, so positioned layers shift in screen-space units.

### Deferred to Phase B

- `expandWidth()` / `expandHeight()` shortcuts and the matching
  `addLine(thickness, color)` overload need new `expandWidth` / `expandHeight`
  flags on the canonical record types. They are folded into Phase B together
  with `ShapeContainerNode` and `Transform`, since all three are public-record
  extensions and benefit from being released together.
- `ListBuilder.addItem(label, Consumer<ListBuilder>)` for nested lists requires
  a new `ListItem` value type and a `ListNode` record signature change. Also
  moved into Phase B for the same reason.

---

## v1.4.1 - 2026-04-27

### Documentation

- README rewrite for v1.4.0 dropped three structural sections (`## Table component`, `## Line primitive`, `## Architecture at a glance`) that the `DocumentationCoverageTest` guards baseline. CI flagged the regression on the `main` branch; v1.4.1 restores the sections (the table snippet now also points readers to the new column-span feature), keeps the canonical-DSL anti-patterns out of the snippets, and moves the architecture mermaid diagram back into its dedicated section.

### Tooling

- `examples/src/main/java/com/demcha/examples/GenerateAllExamples.java` now wires `CinematicProposalFileExample.generate()` into the orchestrator, so the runnable examples module produces all seven fixtures (including `project-proposal-cinematic.pdf`) used by the README visual previews.

This is a documentation-only patch release. There are no public API changes; v1.4.0 consumers can upgrade with no code changes.

---

## v1.4.0 - 2026-04-27

### Headline — "cinematic document engine"

v1.4 closes the visual-design gap that the previous releases left open. Tables can now span columns, layers can stack on top of each other, sections and pages carry semantic backgrounds, paragraphs accept fluent rich text, and the whole look-and-feel can be parametrised through a single `BusinessTheme`. The release also lands the visual-regression scaffolding required to keep README screenshots stable across refactors.

### Public API — semantic primitives

- `DocumentTableCell` is now a 3-field record (`lines`, `style`, `colSpan`). The new `colSpan(int)` factory plus `withColSpan(...)` on `TableCellContent` let one cell occupy several columns; sum-of-spans-per-row is validated by `TableLayoutSupport`. Border ownership and natural-width distribution understand spans (extra width is shared across `auto` columns inside the span; an all-fixed span throws when it cannot fit). Renderer code is unchanged — spanned cells emit a single `TableResolvedCell` with the merged width.
- new `LayerStackNode` + `LayerAlign` primitive composes children inside the same bounding box, in source order (first behind, last in front). Each layer carries one of nine alignments (`TOP_LEFT … BOTTOM_RIGHT`). Pagination is atomic. Backed by a new `Axis.STACK` in `CompositeLayoutSpec` and a `compileStackedLayer` branch in `LayoutCompiler`. DSL surface: `LayerStackBuilder` with `back(...)`, `center(...)`, `layer(node, align)`.
- `DocumentSession.pageBackground(DocumentColor | Color)` (and the matching `GraphCompose.DocumentBuilder` setter) injects a full-canvas `ShapeFragmentPayload` at the start of every page. Combine with `LayerStackNode` for cinematic hero pages without any backend changes.
- `AbstractFlowBuilder` gains semantic shortcuts on every flow / section / module: `band(color)`, `softPanel(color)` / `softPanel(color, radius, padding)`, and `accentLeft / accentRight / accentTop / accentBottom(color, width)`. They reuse the existing `fillColor`, `cornerRadius`, `padding`, and `DocumentBorders` plumbing — the new methods are sugar for designer-style flows.
- `RichText` fluent builder (`document.dsl.RichText`) plus `ParagraphBuilder.rich(...)` / `AbstractFlowBuilder.addRich(...)` cover the `Status: Pending` label/value pattern in one expression: `RichText.text("Status: ").bold("Pending").color("…", red).accent("…", brand)`. Includes `plain / bold / italic / boldItalic / underline / strikethrough / color / accent / size / style / link / append`.

### Public API — design tokens

- new `com.demcha.compose.document.theme` package — entirely on top of public document-level types, no engine leaks.
  - `DocumentPalette` — primary / accent / surface / surfaceMuted / textPrimary / textMuted / rule
  - `SpacingScale` — five-step `xs / sm / md / lg / xl` with monotonicity validation and `insetsXs() … insetsXl()` helpers
  - `TextScale` — `h1 / h2 / h3 / body / caption / label / accent` resolved styles
  - `TablePreset` — `defaultCellStyle / headerStyle / totalRowStyle / zebraStyle`
  - `BusinessTheme` — composes the four scales plus an optional page background, with three built-in presets (`classic()`, `modern()` cream paper + teal, `executive()` slate panels with Times-Roman headings) and immutable `withName / withPageBackground` forks

### Testing infrastructure

- `com.demcha.testing.visual.ImageDiff` — pixel-by-pixel comparison with per-channel tolerance and a red/grey diff image.
- `com.demcha.testing.visual.PdfVisualRegression` — renders PDF bytes to one PNG per page via `PdfRenderBridge` and compares against baselines under `src/test/resources/visual-baselines`. Approve mode (`-Dgraphcompose.visual.approve=true` or `GRAPHCOMPOSE_VISUAL_APPROVE=true`) writes new baselines; comparison failures drop `actual.png` and `diff.png` next to the baseline for inspection.
- 41 new tests across the cinematic surfaces (`TableColSpanIntegrationTest`, `TableBuilderColSpanTest`, `LayerStackBuilderTest`, `PageBackgroundTest`, `SectionPresetTest`, `RichTextTest`, `BusinessThemeTest`, `PdfVisualRegressionTest`). Total green count: **525**.

### Architecture

- `CompositeLayoutSpec.Axis.STACK` joins `VERTICAL` and `HORIZONTAL`. The compiler dispatches `STACK` to `compileStackedLayer`, which positions each child inside the stack box via per-layer alignment offsets and shares the same `compileNodeInFixedSlot` plumbing rows already use.
- table layout (`TableLayoutSupport`, test-side `TableBuilder`) was rewritten around a "logical cell" model: each authored cell is one `LogicalCell(startColumn, colSpan, content)` resolved against a `stylesGrid[row][col]` — the grid keeps existing border-ownership logic intact while letting render code keep emitting one `TableResolvedCell` per logical cell.
- `DocumentSession.layoutGraph()` now wraps `compiler.compile(...)` with `withPageBackgrounds(...)` so backends never need to know about the page-background option — they just iterate fragments as usual.

### Performance

- Cinematic features have negligible overhead: page-background injection is a single fragment per page; column spans, layer stacks, and themes do not change the number of emitted fragments. End-to-end template latency stays in the same envelope as v1.3 once JIT is warm.
- Full benchmark surface is now published in the README: `current-speed` (full profile) latency + per-stage breakdown, parallel throughput on the invoice template (1&rarr;8 threads), `scalability` suite (1&rarr;16 threads, 13.8&times; speedup at 16), 50-thread `stress` test (5,000 docs, 0 errors), and the `comparative` table against iText 5 and JasperReports.

### Documentation

- README rewritten around the cinematic v1.4 narrative: new sections for column spans, layer stacks, page background + section presets, rich text DSL, business themes, the visual-regression workflow, "Extending GraphCompose" guidance, and a refreshed Performance section sourced from `scripts/run-benchmarks.ps1`.

---

## v1.3.0 - 2026-04-27

### Public API

- `DocumentSession` now exposes ergonomic mutators for document-level PDF chrome: `metadata(...)`, `watermark(...)`, `protect(...)`, `header(...)`, `footer(...)`, and `clearHeadersAndFooters()`. Convenience entrypoints (`buildPdf`, `writePdf`, `toPdfBytes`) honour these options without having to build a `PdfFixedLayoutBackend` manually
- new horizontal layout primitive: `addRow(...)` on flows, sections, and modules creates a `RowNode` that arranges atomic children left-to-right with optional `weights(...)` and `gap(...)`. Rows are atomic blocks from the paginator's perspective
- `DocumentBorders` value type plus `borders(...)` on flows, sections, modules, and rows let you describe per-side strokes (top / right / bottom / left). Per-side borders override the uniform `stroke(...)` setting
- `ParagraphBuilder.autoSize(maxSize, minSize)` / `autoSize(DocumentTextAutoSize)` searches for the largest font size that fits the paragraph on a single line within the resolved inner width
- new `addLink(text, uri)` and `addLink(text, DocumentLinkOptions)` shortcuts on `AbstractFlowBuilder` for the common single-link case (was previously only available as a paragraph inline run)
- backend-neutral output options under `com.demcha.compose.document.output` (`DocumentMetadata`, `DocumentWatermark`, `DocumentProtection`, `DocumentHeaderFooter`, aggregated by `DocumentOutputOptions`). PDF and DOCX backends translate them; session-level metadata propagates to DOCX core properties as well as the PDF backend
- `DocxSemanticBackend` is now a functional Apache POI based backend that returns DOCX bytes (was a manifest-only skeleton); supports paragraphs, tables, images, spacers, page breaks, and document-level page geometry. Apache POI is declared optional, so consumers that only render PDFs do not pay the dependency cost

### Performance

- `PageBreaker.paginationPriority` pre-computes `(y, depth)` keys and uses `UUID.compareTo` for tie-breaks (no per-compare string allocation). Old comparator allocated a 36-character UUID string for every priority queue compare
- `Entity.getComponent` and `Entity.require` no longer issue per-call debug logging (even guarded `isDebugEnabled` calls cost a volatile read on Logback)
- table layout helpers (`resolveTableLayout`, `sliceTablePreparedNode`, ~350 lines) extracted into `TableLayoutSupport` for clarity
- end-to-end template rendering is 19–30 % faster than v1.2.0 on the canonical benchmark suite (`invoice-template`: 25.77 → 10.77 ms avg; `cv-template`: 17.89 → 6.50 ms avg; `proposal-template`: 24.77 → 13.44 ms avg)

### Benchmark methodology

- `CurrentSpeedBenchmark` smoke profile bumped from 2 / 5 → 30 / 100 warmup / measurement iterations so the JIT reaches a steady state and percentiles are statistically meaningful
- percentile calculation now uses linear interpolation between order statistics (`rank = (n-1) * p`) so p95 no longer collapses to max at small sample counts
- `System.gc()` plus a 50 ms sleep separates warmup from measurement, dropping run-to-run variance from 10–25 % to 2–5 %
- `peakHeapMb` reports the heap delta over the post-warmup baseline rather than absolute used heap
- a per-stage breakdown table (`compose / layout / render / total`) prints alongside the latency table so consumers can attribute regressions to engine layout vs PDFBox serialization
- smoke gate thresholds tightened from 800–2600 ms (effectively a no-op) to 8–100 ms (~3× the observed avg) — still safe against CI machine variance, now catches ≥50 % regressions
- the `ComparativeBenchmark` console table no longer wraps when library names exceed 20 characters

### Architecture

- `CompositeLayoutSpec` carries an explicit `Axis` (vertical / horizontal) and optional per-child weights; the layout compiler dispatches to a dedicated horizontal-row code path for `RowNode`
- `ShapeFragmentPayload` carries an optional `SideBorders` payload; `PdfShapeFragmentRenderHandler` draws each configured side stroke independently of the uniform rectangle stroke
- `SemanticExportContext` carries `DocumentOutputOptions` so semantic backends (DOCX, future PPTX) can apply metadata / chrome configured at the session level
- the unused engine `Button` renderable and the `ButtonBuilder` test-support factory entry were removed
- guide-line overlays now compute owner bounds across sub-fragments (e.g. table rows) and paint margin / padding once around the entire owning node instead of stacking dashed rectangles inside every row

### Documentation

- `docs/canonical-legacy-parity.md` is updated to reflect the v1.3 capabilities (rows, per-side borders, auto-size text, DOCX export)
- `docs/benchmarks.md` documents the new smoke profile defaults, the GC stabilization point, the linear-interpolation percentile rule, and the stage-breakdown table
- `CONTRIBUTING.md` repository map and package list now describe the canonical functional layout (`document.layout`, `document.backend`, `document.output`) alongside the legacy ECS engine

---

## v1.2.0 - 2026-04-25

### Release identity

- the current canonical API cleanup is being released as **v1.2.0** to match the project's early maturity while still making `GraphCompose.document(...) -> DocumentSession -> DocumentDsl` the preferred authoring path
- Maven coordinates are `io.github.demchaav:graphcompose:1.2.0`; JitPack consumers continue to use `com.github.demchaav:GraphCompose:v1.2.0`
- consumers on `v1.1.x` should follow the [migration guide](./docs/migration-v1-1-to-v1-2.md) before adopting the canonical API path

### Public API

- `DocumentSession` is now an `AutoCloseable` lifecycle owner: `close()` is idempotent, and authoring/rendering methods on a closed session fail fast with `IllegalStateException` instead of returning broken state
- empty document rendering (`writePdf` / `toPdfBytes` / `buildPdf`) now throws a domain-specific `IllegalStateException` instead of producing a zero-byte / zero-page PDF; add at least one root before rendering
- `DocumentPageSize` is the public page-size value; `GraphCompose.document(...).pageSize(PDRectangle)` was removed from the canonical API
- `DocumentSession#margin(Margin)` and `GraphCompose.DocumentBuilder#margin(Margin)` were removed from the canonical API; use `DocumentInsets` or `margin(top, right, bottom, left)` to keep authoring renderer-neutral
- PDF-specific metadata, protection, watermark, and header/footer options moved behind `PdfFixedLayoutBackend.builder()` instead of the canonical `GraphCompose` / `DocumentSession` surface
- `GraphCompose.document(...).guideLines(true)` and `DocumentSession.guideLines(true)` now enable debug guide-line overlays for `buildPdf`, `writePdf`, and `toPdfBytes` convenience output
- `DocumentSession.layoutSnapshot()` now returns public renderer-neutral `com.demcha.compose.document.snapshot.*` DTOs instead of engine debug types
- `BoxConstraints.natural(width)` is now the canonical natural-measurement factory; `unboundedHeight(width)` remains as a compatibility alias
- the public font registry no longer exposes the unadvertised `getPdfFont(...)` bridge; backend code resolves typed fonts through `getFont(..., PdfFont.class)`

### Architecture guards

- `PublicApiNoEngineLeakTest` baselines the inventory of `com.demcha.compose.engine.*` imports allowed in the public API surface — any new leak fails the build
- `SemanticLayerNoPdfBoxDependencyTest` keeps `document.node.*` free of direct PDFBox imports and pins the remaining `backend.fixed.pdf.options.*` references for Phase 3 cleanup
- `PdfBackendIsolationGuardTest` keeps PDFBox out of canonical API, DSL, semantic nodes, layout, snapshots, and non-PDF backend contracts

### Layout

- `PaginationEdgeCaseTest` adds focused regressions for exact-fit content, near-boundary float handling, leading / trailing page breaks, oversized atomic images, too-tall table rows, module splits with PDF chrome, and nested sections that paginate while preserving margin and padding

### Documentation

- new `docs/migration-v1-1-to-v1-2.md` outlines the move from older v1.1 usage patterns to the canonical session-first API
- new `docs/v1.2-roadmap.md` tracks the remaining stabilization work for the v1.2 release polish window
- `docs/release-process.md` now describes the current JitPack-first 1.x release flow and runnable examples verification
- user-facing docs now describe debug guide-line overlays through `GraphCompose.document(...).guideLines(true)` / `DocumentSession.guideLines(true)` and call out JitPack tag-cache handling during release verification

---

## v1.1.0 - 2026-04-13

### Highlights

- shifted the public built-in template narrative to `compose(DocumentSession, ...)`
- added document-level PDF features for richer real-world output
- moved the engine further away from PDF-centric internals through backend-neutral composition and render-handler seams
- strengthened architecture guard rails for template scene builders
- expanded visual testing and benchmark tooling for day-to-day development

### Added

- canonical `DocumentSession` contract as the primary composition seam
- layout snapshot extraction and JSON-based regression coverage for resolved document geometry
- runnable `examples/` module for CV, cover letter, invoice, proposal, and weekly schedule generation
- new built-in business templates and data models for invoice, proposal, and weekly schedule documents
- barcode support with QR, Code 128, and EAN-13 builders
- watermark support
- configurable headers and footers with page numbers and separators
- PDF bookmarks / outline generation
- document metadata support
- PDF protection hooks
- explicit page-break and divider builders
- visual showcase render tests for barcodes, QR codes, pagination, and document chrome
- current-speed benchmark suite
- benchmark JSON/CSV export and diff tooling
- one-command benchmark runner: [scripts/run-benchmarks.ps1](./scripts/run-benchmarks.ps1)

### Changed

- bumped the library release to `v1.1.0`
- updated README installation snippets to the new release version
- documented built-in templates as compose-first by default
- refreshed README visuals to show barcode/QR and compose-first template output
- added release-facing notes for the experimental live preview dev tool in test scope
- refreshed release documentation to point contributors at visual tests and benchmark workflows

### Architecture and CI

- engine-side text measurement and rendering dispatch are now more explicitly decoupled from PDFBox-specific implementation details
- added template boundary guard coverage so `*SceneBuilder` classes stay free of backend-specific PDFBox types
- split architecture/documentation guards into a dedicated CI job that can be required independently in branch protection

### Compatibility notes

- older tagged JitPack releases remain usable as long as consumers pin a specific version such as `v1.0.3`
- deprecated `render(...)` template adapters remain available for compatibility, but new docs and examples now prefer `compose(...)`
