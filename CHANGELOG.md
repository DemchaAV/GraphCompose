# Changelog

All notable changes to GraphCompose are documented here. Versions
follow semantic versioning; release dates are ISO 8601.

## v1.6.0 — Planned

The "expressive" release. Closes the remaining canonical-vs-legacy
parity gaps for advanced authoring without architectural rollback.
Every new primitive ships through `DocumentNode + NodeDefinition +
render handler`. See [`docs/v1.6-roadmap.md`](docs/v1.6-roadmap.md)
for the phased plan, verification gates, and ADRs.

### Architecture hardening (committed in v1.6 line, develop)

The architecture lane closes the highest-severity findings from the
post-1.5 audit. None of these change author-facing behaviour for
unmodified v1.5 code; they sharpen the public-vs-internal boundary,
open extension points, and split the load-bearing files. See
[`docs/migration-v1-5-to-v1-6.md`](docs/migration-v1-5-to-v1-6.md)
for the user-facing summary.

- **`@Internal` API stability marker.** New
  `com.demcha.compose.document.api.Internal` annotation
  (runtime-retained) marks `document.layout.*` and the
  `BuiltInNodeDefinitions` payload records as implementation detail.
  `InternalAnnotationCoverageTest` enforces propagation. ADR 0003
  records the boundary decision.
- **`DocumentRenderingException`** wraps the convenience render path:
  `buildPdf`, `writePdf`, `toPdfBytes`, and the AutoCloseable `close()`
  override no longer declare `throws Exception`. Lower-level backend
  SPIs continue to declare `throws Exception` on purpose.
- **Public PDF render handler SPI.** The
  `PdfFragmentRenderHandler` Javadoc is rewritten as an extension
  point and `PdfFixedLayoutBackend.Builder.addHandler(...)` is the
  new registration path. Custom handlers replace built-in defaults
  by `payloadType()`. ADR 0004 records the SPI shape.
- **Thread-safety contract** documented on
  `document.api/package-info.java` and
  `document.backend.fixed.pdf/package-info.java`.
- **DSL polish.** `DocumentDsl.text()` and `DocumentSession.builder()`
  aliases are `@Deprecated(forRemoval=true, since="1.6.0")`; prefer
  `paragraph()` and `dsl()` respectively.
- **PDF-typed chrome overloads on `DocumentSession`** — `metadata`,
  `watermark`, `protect`, `header`, `footer` accepting
  `Pdf*Options` — are `@Deprecated(forRemoval=true, since="1.6.0")`;
  the canonical backend-neutral overloads are unchanged.
- **`DocumentPalette.builder()`** replaces the positional
  `DocumentPalette.of(Color × 7)` factory; the old factory is
  `@Deprecated(forRemoval=true)`. `BusinessTheme.classic()/modern()/executive()`
  now use the builder. `IllegalStateException` from `build()` names
  every missing token in one message.
- **Targeted layout perf wins** (none alter output bytes):
  `LayoutCompiler.compositeDecorationFragments` /
  `compositeOverlayFragments` no longer wrap with `List.copyOf`,
  `stableZIndexOrder` short-circuits when every layer reports the
  same `zIndex`, `PdfRenderSession` keeps page surfaces in a
  `PDPageContentStream[]` (no `Integer.valueOf` autoboxing),
  `PdfFontLoader.THREAD_LOCAL_TTF_CACHE` is a bounded LRU
  (max 32 entries per thread). Duplicate
  `com.demcha.compose.font.Pdf_FontLoader` deleted.
- **Layout invariant tests.** `LayoutCompilerInvariantsTest` pins
  four scenarios that previously had only transitive snapshot
  coverage: page-advance on overflow, layer source-order under
  uniform `zIndex`, explicit `zIndex` ordering, equal-weight row
  slot distribution.
- **`BuiltInNodeDefinitions` split (Phase E.1).** All 15 built-in
  `NodeDefinition` implementations now live in
  `document.layout.definitions.*` (one file per node type):
  PageBreak, Spacer, Shape, Line, Ellipse, Image, Barcode,
  Container, Section, Row, LayerStack, ShapeContainer, Table,
  Paragraph, List. Shared inline helpers (`EPS`, transform
  wrapping, decoration / table / measurement adapters) live in
  `NodeDefinitionSupport`; the paragraph / list text-flow cluster
  (wrapping, markdown tokenisation, inline-run layout, split
  slicing) lives in the new `TextFlowSupport` helper.
  `BuiltInNodeDefinitions` drops from 3,037 to ~60 lines and now
  only exposes `registerDefaults(NodeRegistry)` as the single
  registration entry point.
- **`PlacementContext` strategy interface (Phase E.4).** A new
  `PlacementContext` sealed interface unifies the placement
  bookkeeping that `LayoutCompiler` helpers need (current page
  index, canvas, prepare/fragment contexts, target lists for placed
  nodes/fragments, and a `canAdvancePage()` / `advancePage()` /
  `touchPage()` strategy). `FixedSlotPlacementContext` pins the page
  for row slots, stacked layers, and atomic leaf placement;
  `MutatingPlacementContext` wraps the live `CompilerState` for
  callers that drive top-down page flow. The previously private
  inner `CompilerState` is lifted to a sibling package-private
  class. `placeStackLayer`, `placeAtomicLeafFragments`, and
  `compileNodeInFixedSlot` now take `PlacementContext` instead of
  six explicit parameters each. Pure refactor — no public API
  change, no behaviour change.
- **`DocumentSession` slim (Phase E.3).** New `SessionFontApi`
  facade (`session.fonts()`) groups
  `registerFontFamily(FontFamilyDefinition)` and
  `registerNodeDefinition(NodeDefinition)` alongside the existing
  `chrome()` and `layout()` facades. Page-background composition
  moves from a private inner method to a new
  `DocumentPageBackgrounds` utility. The four convenience PDF
  methods (`toPdfBytes`, `writePdf`, `buildPdf`, `buildPdf(Path)`)
  share a single `wrapPdfRendering` exception-mapping helper instead
  of repeating the same try/catch four times. Javadoc on the
  deprecated PDF-typed chrome overloads is compacted to a single
  `@deprecated` tag. `DocumentSession` drops from 1,024 to ~937
  lines without changing any public method signatures.

### Templates v2 restructure (committed in v1.6 line, develop)

The **biggest change in v1.6** — the canonical template surface
(CV, cover letter, invoice, proposal) was rewritten from the ground
up. Old positional / cinematic-monolith composers (`CvTemplateV1`,
`NordicCleanCvTemplate`, `InvoiceTemplateV2`, etc.) replaced with a
four-layer architecture: **Theme tokens → Layout slots → Components
+ Blocks → Spec data**, glued together by per-domain builders that
preset classes wrap into one-liner factories. The result is a
copy-and-tweak preset surface where adjusting one visual decision
takes one method change rather than a fork of a 600-line composer.

**New template package layout** (replaces legacy `templates/builtins`,
`templates/support/cv`, `templates/data/cv`, `templates/theme/CvTheme`):

```
templates/
  api/         DocumentTemplate<S>, SlotMap
  themes/      Spacing, Typography (token records)
  components/  Header, Module, MarkdownText
  blocks/      sealed Block hierarchy:
               ParagraphBlock, BulletListBlock, NumberedListBlock,
               IndentedBlock, KeyValueBlock, MultiParagraphBlock
  decorations/ Spacer, Divider, AccentStrip
  cv/
    layouts/   SingleColumn, TwoColumnSidebar, ThreeColumnMagazine
    presets/   14 flat copy-and-tweak preset classes
    builder/   CvBuilder
    spec/      CvSpec, CvHeader, CvModule
  coverletter/
    layouts/   LetterFormat
    presets/   14 paired letter presets (one per CV preset)
    builder/   CoverLetterBuilder
    spec/      CoverLetterSpec, CoverLetterHeader
  invoice/
    presets/   ModernInvoice (minimal v2 surface)
    builder/   InvoiceBuilder
    spec/      InvoiceSpec
  proposal/
    presets/   ModernProposal (minimal v2 surface)
    builder/   ProposalBuilder
    spec/      ProposalSpec
```

**14 CV presets**: `ModernProfessional`, `NordicClean`,
`ClassicSerif`, `CompactMono`, `Executive`, `EngineeringResume` (was
`TechLeadCvTemplate`), `TimelineMinimal`, `BoxedSections`,
`CenteredHeadline`, `BlueBanner`, `EditorialBlue`, `Panel` (was
`ProductLeaderCvTemplate`), `SidebarPortrait`, `MonogramSidebar`.
Each is one final class with one `create(BusinessTheme)` factory:

```java
import com.demcha.compose.document.templates.cv.presets.ModernProfessional;
import com.demcha.compose.document.templates.cv.spec.CvSpec;
import com.demcha.compose.document.theme.BusinessTheme;

DocumentTemplate<CvSpec> template = ModernProfessional.create(BusinessTheme.modern());
template.compose(session, mySpec);
```

**Inline markdown rich text** — body strings carrying
`**bold**` and `*italic*` markers render with proper
{@code DocumentTextDecoration} via the new
`templates.components.MarkdownText` parser. Lets an LLM emit a
resume bullet like `**Java 21**, SQL, Kotlin` and the preset
renders Java 21 in bold without separate inline-run construction.

**Active hyperlinks** — header email + LinkedIn / GitHub labels
become clickable mailto: / https: hyperlinks via
`DocumentLinkOptions` on per-run inline runs.

**Slot-based layouts** — multi-column CV presets
(`Panel`, `SidebarPortrait`, `MonogramSidebar`) declare named
slots (`MAIN`, `SIDEBAR`); a custom preset can rearrange which
modules go into which slot via `.place(slot, "Module Name", ...)`.

**Layout snapshot tests** lock the rendered tree of every preset
(28 baselines under
`src/test/resources/layout-snapshots/canonical-templates/cv-v2/`
and `.../coverletter-v2/`).

**Examples** — `CvTemplateGalleryFileExample` renders all 14 v2
CV presets to `examples/target/generated-pdfs/cv-<id>.pdf`; new
`CoverLetterTemplateGalleryFileExample` renders all 14 paired
letter presets to `cover-letter-<id>.pdf`.

**Migration**: legacy classes have been **deleted**, not
deprecated. Anyone on
`new CvTemplateV1()` / `new NordicCleanCvTemplate()` / etc. must
switch to the new factory:

| Old | New |
|---|---|
| `new CvTemplateV1()` | `ModernProfessional.create(BusinessTheme.modern())` |
| `new NordicCleanCvTemplate()` | `NordicClean.create(BusinessTheme.modern())` |
| `CvTheme.defaultTheme()` | `BusinessTheme.modern() + Spacing.compact()` |
| `CvTemplate` interface | `DocumentTemplate<CvSpec>` |

`InvoiceTemplateV2` and `ProposalTemplateV2` (cinematic) remain
in `templates/builtins/` as the recommended path for fully-styled
output; the new `ModernInvoice` / `ModernProposal` v2 presets
provide the canonical builder seam, with cinematic feature parity
landing in a follow-up.

#### Phase E.1 reopen — visual parity recovery (May 2026)

The first Phase E.1 pass shipped visually-broken renders.
Every CV preset rendered as a teal-tinted single-column
ModernProfessional clone — `NordicClean` lost its sidebar and
soft-tinted PROFILE panel, `BlueBanner` lost its full-width
section banners, `MonogramSidebar` lost its monogram badge,
`SidebarPortrait` lost its portrait sidebar, `ClassicSerif`
lost its two-page editorial structure, and so on. The
`ModernProfessionalVisualParityTest` was a smoke test
(`assertThat(output).exists()`) and `PresetLayoutSnapshotTest`
recorded baselines from the new (broken) v2 renders without
comparing against V1, so the regressions sailed through CI.

**Phase E.1 was reopened.** All 14 CV preset renders + 14
cover-letter pair renders were rebuilt against the V1 visual
references (committed `assets/readme/examples/cv-*.pdf` for
the 7 presets that had a baseline; V1 source code under
`docs/private/v1-reference/` — gitignored — for the rest).
The author-facing API stays stable; only the rendered output
changed.

- **Adaptive sidebar fill** — `SidebarPortrait` and
  `MonogramSidebar` size the trailing spacer dynamically
  from `canvas().innerHeight()` so the SIDEBAR_BG fill
  reaches the bottom of the page on A4 / Letter / smaller
  test fixtures without overflowing the row's page capacity.
- **`Header` API gained three fluent overrides** —
  `withNameStyle(DocumentTextStyle)`,
  `withContactStyle(DocumentTextStyle)`,
  `withLinkStyle(DocumentTextStyle)`. Required for V1-parity
  palette (e.g. slate-blue name + royal-blue underlined
  links for ModernProfessional) — the unstyled
  `Header.rightAligned` rendered names with the active
  `BusinessTheme`'s `h1()` colour instead.
- **`CvHeader.jobTitle` field added** for the subtitle
  rendered under the name by presets that surface it
  (EditorialBlue, Panel, SidebarPortrait, MonogramSidebar).
  Falls back to a placeholder string when the spec leaves it
  empty.
- **Markdown rendering routed through `MarkdownText.parse`
  in every CV / cover-letter preset paragraph body** so
  spec-author bold / italic markers (`**bold**` / `*italic*`)
  carry through to the rendered runs — previously the
  paragraphs stripped markdown.
- **Sample data factory** updated so `Education` / `Projects`
  use `MultiParagraphBlock` with markdown bold prefixes
  (`**MSc Computer Science** - University of Manchester | 2021`)
  rather than `IndentedBlock`'s multi-line shape, and
  `Additional Information` carries `KeyValueBlock` entries
  (Languages / Work Eligibility) for bold-key + plain-value
  rendering.
- **Snapshot baselines regenerated** — 28 `*.json` files
  under
  `src/test/resources/layout-snapshots/canonical-templates/cv-v2/`
  and `coverletter-v2/` updated to lock the V1-parity render
  in place.
- **Pixel-diff visual parity gate landed.**
  `PresetVisualParityTest` (one for CV, one for cover letters)
  rasterises each preset's PDF page 0 (and `classic_serif`'s
  page 1) via PDFBox `PDFRenderer` and asserts per-pixel diff
  against a checked-in baseline PNG with budget 2500 mismatched
  pixels at per-channel tolerance 8 (per
  `templates-restructure-plan.md` sec 6.2). 29 baselines under
  `src/test/resources/visual-baselines/{cv-v2,coverletter-v2}/`.
  Re-bless with `-Dgraphcompose.visual.approve=true`. The
  `PdfVisualRegression` harness was already built; the reopen
  plugged the 28 presets into it. The placeholder
  `ModernProfessionalVisualParityTest` smoke test is deleted.
  `mvnw verify` → 792 / 0 / 0 / 0.

**Tech debt** (deferred to v1.7 as Phase E.4): 13 of the 14
v2 CV presets are implemented as hand-coded
`DocumentTemplate` subclasses driving the canonical
PageFlow DSL directly (≈ 400-700 LOC each) rather than thin
recipes through the slot-based `CvBuilder`. This was an
explicit trade-off during the reopen — restoring V1 visual
fidelity required components the v2 library hadn't grown
yet (`Panel.softTinted`, `TwoColumnSidebar.tinted`,
`SectionStyle.uppercaseRule`, `WorkEntryRenderer`). The
component library extension + preset refactor is tracked in
`docs/private/templates-restructure-plan.md` Phase E.4 and
`docs/private/templates-v2-audit-remediation.md`.

### Feature scope

- **Nested list ergonomics (Phase A — landed).**
  `ListBuilder.addItem(String label, Consumer<ListBuilder> body)`
  appends a nested item with a builder-callback child scope. New
  `ListItem` record carries `(label, marker, children)`. `ListNode`
  gains a `nestedItems` component (record now has 12) and a
  back-compat 11-component constructor matching the v1.5 shape.
  Per-depth marker resolution: item-level marker wins, then
  `ListBuilder.markerFor(int depth, ListMarker)` overrides, then
  the built-in cascade (`•` → `◦` → `▪` → `·`). The internal
  `usedNestedAuthoring` flag preserves source order across mixed
  flat / nested entries — flat-only callers still get the v1.5
  flat `ListNode`. Layout flattens the tree depth-first into
  indent-prefixed paragraph fragments using non-breaking spaces
  (`U+00A0`) for the per-depth indent so the paragraph wrap
  pipeline preserves them. ADR
  [0012](docs/adr/0012-nested-list-evolution.md) records the
  `ListNode`-extension-vs-new-`NestedListNode` decision.
  Snapshot baseline:
  `src/test/resources/layout-snapshots/document/nested_list_three_levels.json`.
  `mvnw verify` → 804 / 0 / 0 / 0.
- **Composed table cell content (Phase B — landed).**
  `DocumentTableCell.node(DocumentNode)` factory accepts any
  composable canonical node as cell content; `DocumentTableCell`
  gains a 5th component `DocumentNode content` with explicit
  4-arg / 3-arg / 2-arg back-compat constructors so v1.5
  plain-text callers compile unchanged. `TableLayoutSupport`
  threads `PrepareContext` through `resolveTableLayout` and
  prepares each composed cell's child against the cell's
  resolved inner width before row-height resolution; the
  prepared height feeds the existing two-pass row-height pass.
  `FragmentContext` gains a default
  `emitChildFragments(PreparedNode, FragmentPlacement)` method
  that `DocumentLayoutPassContext` overrides to dispatch through
  the registered `NodeDefinition` — so any node type works
  inside a cell automatically (paragraph, list, layer-stack,
  sub-table). Pagination preserves row-by-row behaviour: a
  composed cell stays atomic on its row, and
  `sliceTablePreparedNode` subsets the prepared-content map to
  the slice's row range while keeping repeat-header keys
  intact. The PDF table render handler is unchanged: it still
  iterates `cell.lines()` (empty for composed cells) and the
  child fragments render through their own already-registered
  handlers. ADR
  [0013](docs/adr/0013-composed-table-cell.md) records the
  extend-vs-new-hierarchy and recursion-vs-special-case
  decisions. Snapshot baseline:
  `src/test/resources/layout-snapshots/document/table_cell_with_paragraph.json`.
  `mvnw verify` → 810 / 0 / 0 / 0.

### Stretch goals (v1.6 if time allows; otherwise v1.7)

- **Controlled free-canvas (`CanvasLayerNode`).** New atomic
  semantic node accepting children at explicit `(x, y)` coordinates.
  Separate from `LayerStackNode` / `ShapeContainerNode` so absolute
  placement stays an opt-in primitive. ADR 0007 will record why
  `CanvasLayer` is its own node and why absolute placement is rejected
  on `RowBuilder` / `SectionBuilder`.
- **Phase D — Real PPTX semantic export.** Build out
  `PptxSemanticBackend` from the existing manifest skeleton to a
  working POI-based exporter (paragraphs → text boxes, tables →
  PowerPoint tables, sections → slides).
- **Phase E — Maven Central distribution.** Sonatype OSSRH + GPG
  signing + automated deployment on tag push. Primary install
  coordinates switch to `io.github.demchaav:graphcompose:1.6.0`;
  JitPack stays documented as a fallback.
- **Phase F — Benchmark infrastructure modernisation.** Replace the
  custom warmup / measurement harness with `org.openjdk.jmh` for
  JIT-aware measurement, dead-code elimination protection, and proper
  statistical output. Move the benchmark suite (currently in test
  scope: `CurrentSpeedBenchmark`, `ComparativeBenchmark`,
  `ScalabilityBenchmark`, `FullCvBenchmark`, `GraphComposeBenchmark`)
  into a separate `benchmarks/` Maven module mirroring the
  `examples/` pattern, with a self-executing JMH jar built via
  `maven-shade-plugin`. Add a standalone `layoutGraph()`-only
  scenario so the README can publish a true Layout-vs-Render table
  backed by independently measured values rather than stage
  breakdown subtractions. CI Performance Smoke Check switches to the
  new JMH jar; `scripts/run-benchmarks.ps1` becomes a thin wrapper so
  the documented workflow keeps working.

### Non-goals

- No revival of `GraphCompose.pdf(...)` or public `EntityManager`.
- No nested rows or nested tables inside `RowBuilder` (preserves
  pagination contract).
- No DOCX path-clipping or transform support (Apache POI limit).
- No deprecation of v1.4 / v1.5 public records — back-compat
  constructors stay.

---

## v1.5.1 — 2026-05-05

### Dependencies

- **PDFBox 3.0.7.** Bumped from 3.0.5 to 3.0.7 (Apache PDFBox patch
  release with upstream rendering and security fixes). No
  public-API impact for GraphCompose consumers.

### Tooling

- `ShapeContainerVisualRegressionTest` tolerates the cross-platform
  PDF font-rendering drift that surfaces between Windows-rendered
  baselines and the Linux CI runner (~1-2% pixel diff), via a
  calibrated `mismatchedPixelBudget` instead of bit-exact comparison.
- `DocumentationCoverageTest` no longer pins to the structural
  section anchors that the v1.5.0 README slim removed; the guard now
  scans the whole README for canonical-DSL coverage and
  legacy-API leakage in one whole-file pass.

This is a maintenance patch release. There are no public API
changes; v1.5.0 consumers can upgrade with no code changes.

---

## v1.5.0 — 2026-05-04

### Headline — "intuitive"

v1.5 keeps every v1.4 cinematic primitive and turns the canonical
authoring surface into a polished, theme-driven experience. Three new
visual feature pillars — **shape-as-container with clip path**,
**transforms (rotate / scale) + per-layer z-index**, and **advanced
tables** — combine with **two new cinematic templates**
(`InvoiceTemplateV2`, `ProposalTemplateV2`), a **`CvTheme` ↔
`BusinessTheme` bridge** (ADR 0002), six **modernised CV templates**,
and a documentation pass that covers every new primitive with a recipe
and a runnable example. Test count grew from 525 (v1.4.1) to 675 — an
extra +150 tests across the cinematic, transform, table, theme-bridge,
streaming, snapshot, CV-render, and Transformable-leaf-builder surfaces.

v1.5 is **fully source-compatible with v1.4**. Every public record
that grew a new field ships back-compat constructors that default the
new value, so v1.4 callers compile and behave unchanged. See
[`docs/migration-v1-4-to-v1-5.md`](docs/migration-v1-4-to-v1-5.md).

### Public API — visual primitives

- **Shape-as-container.** New `addCircle(diameter, fill, inside)`,
  `addEllipse(w, h, fill, inside)`, and `addContainer(...)` shortcuts
  on `AbstractFlowBuilder` build a `ShapeContainerNode` whose bounding
  box is dictated by a `ShapeOutline` (`Rectangle`,
  `RoundedRectangle`, `Ellipse`, plus a `circle(diameter)` factory).
  Children are clipped via the new `ClipPolicy` enum
  (`CLIP_PATH` — default — / `CLIP_BOUNDS` / `OVERFLOW_VISIBLE`). The
  PDF backend honours every clip policy via graphics-state
  `saveGraphicsState() + clip(path)` markers; the DOCX backend renders
  layers inline without the outline frame and logs a one-time
  `docx.export.shape-container-fallback` capability warning.
  `ShapeContainerBuilder` exposes the same nine-point alignment
  vocabulary as `LayerStackBuilder` plus `position(node, dx, dy,
  anchor)` for screen-space nudges.
- **Transforms (rotate / scale).** New
  `com.demcha.compose.document.style.DocumentTransform` value type
  with `rotate(deg)`, `scale(uniform)`, `scale(sx, sy)` factories
  plus `withRotation(...)` / `withScale(...)` axis-preserving copies
  and an `isIdentity()` helper. New
  `com.demcha.compose.document.dsl.Transformable<T>` mixin exposes
  `transform(...)`, `rotate(...)`, `scale(...)` as default methods.
  Every shape-shaped builder opts in: `ShapeContainerBuilder`,
  `ShapeBuilder`, `LineBuilder`, `EllipseBuilder`, `ImageBuilder`,
  `BarcodeBuilder`. `rotate(...).scale(...)` chain naturally and pivot
  around the placement centre. The PDF backend issues
  `saveGraphicsState() + cm(matrix)` around each transformed leaf
  (rotation is negated on the way out so the engine's clockwise
  convention matches PDF native counter-clockwise). Identity
  transforms short-circuit and emit no markers, so layout snapshots
  for default-configured nodes are byte-identical to v1.4.
- **Per-layer z-index.** `LayerStackNode.Layer` and shape-container
  layers gain `int zIndex` (default `0`).
  `LayerStackBuilder.layer(node, align, zIndex)` /
  `position(node, dx, dy, align, zIndex)` and the matching
  `ShapeContainerBuilder` overloads let a layer declared earlier draw
  on top of layers declared later. The layout compiler stable-sorts
  layers before render; equal `zIndex` keeps source order.

### Public API — advanced tables

- `DocumentTableCell.rowSpan(int)` mirrors the existing
  `colSpan(int)`. Cells compose freely:
  `DocumentTableCell.text("Tall").colSpan(2).rowSpan(3)`. The layout
  layer skips occupied grid positions when interpreting subsequent
  source rows; misalignments (missing cell, extra source cell,
  overlapping span, span exceeding remaining rows) raise precise
  diagnostics.
- `TableBuilder.zebra(odd, even)` paints alternating row fills.
  Available as `(DocumentTableStyle, DocumentTableStyle)` and as a
  `(DocumentColor, DocumentColor)` overload. Either argument may be
  `null` to skip painting that parity. Existing entries in the
  `rowStyles` map (`headerStyle(...)`, `rowStyle(idx, ...)`,
  `totalRow(...)`) always win over zebra alternation.
- `TableBuilder.totalRow(values)` adds a totals row with a default
  bold-on-grey-blue style; `totalRow(style, values)` is the
  customisable form.
- `TableBuilder.repeatHeader()` / `repeatHeader(rowCount)` re-emits
  the configured leading rows at the top of every continuation page
  when a table paginates. Default is `0` so existing tables paginate
  exactly as before.
- `TableBuilder.headerRow(values)` is a naming alias for
  `header(...)` so authors writing
  `headerRow(...).row(...).totalRow(...)` keep a parallel vocabulary.

### Public API — templates and themes

- **`InvoiceTemplateV2`** is the cinematic invoice counterpart to
  `InvoiceTemplateV1`. Two constructors: the no-arg form picks
  `BusinessTheme.modern()`, the one-arg
  `InvoiceTemplateV2(BusinessTheme)` accepts any theme. Hero
  `softPanel` carrying invoice number / dates / inline rich-text
  status, a two-column row with `From` / `Bill to` parties, themed
  line-items table with `headerStyle` / zebra / totals /
  `repeatHeader()`, and a footer row with `accentLeft` strips on the
  notes / payment-terms columns.
- **`ProposalTemplateV2`** is the proposal counterpart, sharing the
  same `BusinessTheme`-driven composition: hero panel rounded only on
  the right (via the new `DocumentCornerRadius.right(...)` form),
  themed executive-summary panel, sender / recipient parties row,
  sections rendered through `theme.text().h2()` headings, a timeline
  table (Phase / Duration / Details), and a pricing table (Item /
  Description / Amount) with `repeatHeader()`, zebra rows, and a
  total-pricing row anchored at the bottom via `totalRow(...)`.
- **`CvTheme.fromBusinessTheme(BusinessTheme)`** static factory
  derives a CV theme from a business theme (ADR 0002). The bridge
  maps palette / text-scale slots into `primaryColor` /
  `secondaryColor` / `bodyColor` / `accentColor` / `headerFont` /
  `bodyFont` / font sizes; CV-specific layout tokens (`spacing`,
  `moduleMargin`, `spacingModuleName`) keep the existing CV defaults.
  The ten existing CV templates and `CvTemplateV1` continue to work
  unchanged.
- **Six CV templates modernised** to v1.5 idioms:
  `BlueBannerCvTemplate`, `BoxedSectionsCvTemplate`,
  `CenteredHeadlineCvTemplate`, `MonogramSidebarCvTemplate`,
  `SidebarPortraitCvTemplate`, `TimelineMinimalCvTemplate`. Each
  gains a `(CvTheme)` constructor and keeps a no-arg one whose
  default theme matches the legacy palette/font choices, so default-
  constructed instances render identical-page-count PDFs to v1.4.
  `accentTop` / `accentBottom` replace the old
  `addLine(horizontal=innerWidth)` separators around section banners,
  and `softPanel(...)` collapses the
  `padding(asymmetric) + fillColor(...)` cascade.
- `InvoiceTemplateV1` and `ProposalTemplateV1` continue to ship
  side-by-side. Authors who want the cinematic look opt in by
  switching the type.

### Public API — DSL ergonomics (Phase A)

- `LayerStackBuilder` exposes nine alignment shortcuts (`topLeft`,
  `topCenter`, `topRight`, `centerLeft`, `center`, `centerRight`,
  `bottomLeft`, `bottomCenter`, `bottomRight`) on top of `back` /
  `center` so authors do not need to remember the full `LayerAlign`
  enum.
- `LayerStackBuilder.position(node, offsetX, offsetY, anchor)` nudges
  a layer from its anchor by an on-screen offset (positive `offsetX`
  = right, positive `offsetY` = down).
- `AbstractFlowBuilder` gains five convenience overloads on top of
  the v1.4 surface: `addShape(w, h, fill)`,
  `addEllipse(diameter, fill)`, `addEllipse(w, h, fill)`,
  `addCircle(diameter, fill)`, `addImage(data, w, h)`.
- `RowBuilder.spacing(double)` is the canonical name for horizontal
  child spacing; `RowBuilder.gap(double)` becomes a deprecated alias
  (`@Deprecated(since = "1.5.0")`) that delegates to `spacing(...)`.
- `RowBuilder.add(node)` validates the child type **eagerly** and
  raises `IllegalArgumentException` from the offending call site
  instead of deferring to `build()` and raising
  `IllegalStateException` later.
- `DocumentDsl.richText(Consumer<RichText>)` is a new callback entry
  point that builds a `RichText` run sequence in one fluent call.

### Architecture

- New `NodeDefinition.emitOverlayFragments(...)` hook complements the
  existing `emitFragments(...)`. It exists for paired begin/end
  marker pairs (clip-begin/end, transform-begin/end) so the layout
  compiler can emit a single flat fragment sequence
  `[transform-begin → outline → clip-begin → … layers … → clip-end →
  transform-end]` in one pass. Most node types inherit the empty
  default and need no changes.
- New marker payloads on `BuiltInNodeDefinitions`:
  `ShapeClipBeginPayload` / `ShapeClipEndPayload` (carry outline +
  policy + owner path), `TransformBeginPayload` /
  `TransformEndPayload`. PDF render handlers ship alongside:
  `PdfShapeClipBeginRenderHandler`, `PdfShapeClipEndRenderHandler`,
  `PdfTransformBeginRenderHandler`, `PdfTransformEndRenderHandler`,
  registered in `PdfFixedLayoutBackend.defaultHandlers()`.
- New `PaginationPolicy.SHAPE_ATOMIC` distinguishes shape-clipped
  atomicity from bbox-only `ATOMIC` for snapshots and render
  handlers. Oversized containers raise the existing
  `AtomicNodeTooLargeException` with the offending semantic name.
- `TableLayoutSupport` replaces the per-row `colSpan`-sum check with
  a unified cell-grid pre-pass driven by an occupancy mask. The new
  `buildLogicalRows(node, columnCount)` walks columns left-to-right,
  skipping positions covered by a prior row's spanning cell.
  `LogicalCell` carries the cell's full
  `(startRow, startColumn, colSpan, rowSpan, content)` extent.
  Row-height resolution is two-pass: single-row first, then spanning
  cells distribute deficit equally across covered rows.
- `TableResolvedCell` gains `double yOffset` (eighth field). Spanning
  cells use a NEGATIVE offset equal to the cumulative height of the
  rows below the starting row, so the cell's rectangle extends
  downward through the rows it merges instead of upward beyond the
  starting row. Both PDF row-render handlers honour the offset.
- `TableNode` gains a 12th field `int repeatedHeaderRowCount`
  (default `0`). `TableDefinition.split` honours the field: the tail
  slice is built with `prependHeaderRowCount = headerCount` so each
  continuation carries the header at the top.
- `LayoutCompiler.compileStackedLayer` and the STACK branch of
  `compileNodeInFixedSlot` compute a stable `iterationOrder`
  permutation via `stableZIndexOrder(...)` before iterating the
  layer list. Stable on ties → equal `zIndex` keeps source order.
- `BuiltInNodeDefinitions.PreparedStackLayout` gains a fourth list
  `zIndices: List<Integer>` populated by both
  `ShapeContainerDefinition` and `LayerStackDefinition`.
- New ADR `docs/adr/0001-shape-as-container.md` records the
  "separate semantic type" decision (rejected: a clip flag on the
  existing `LayerStackNode` record).
- New ADR `docs/adr/0002-theme-unification.md` records the phased
  approach to `CvTheme` ↔ `BusinessTheme` (rejected: a common
  `Theme` interface that loses CV-specific vocabulary).

### Examples

The runnable `examples/` module gains six new showcases hooked into
`GenerateAllExamples`:

- `ShapeContainerExample` — circles, ellipses, rounded cards with
  clipped layers (`ClipPolicy.CLIP_PATH`).
- `TransformsExample` — three-circle rotate row (15° / -15° / no
  tilt), three-card scale row (`scale(0.7)`, `scale(1.1, 0.85)`,
  identity), and a z-swap stage where a RED square declared first
  with `zIndex = 10` draws on top of a TEAL square declared second.
- `TableAdvancedExample` — hero callout, a 3-row spanning side note,
  and a 36-row invoice with bold-on-teal repeating header, zebra
  body rows, and a gold totals row.
- `CustomBusinessThemeExample` — a hand-built "Studio Emerald"
  `BusinessTheme` constructed from raw `DocumentPalette` /
  `SpacingScale` / `TextScale` / `TablePreset` records (no factory
  shortcut), feeding `InvoiceTemplateV2`.
- `HttpStreamingExample` — `writePdf(OutputStream)` for Servlet /
  S3 / GCS adopters. Includes a Spring Boot `@RestController`
  snippet in the Javadoc and a `TrackingOutputStream` test that
  proves the caller's stream is **not** closed.
- `LayoutSnapshotRegressionExample` — full
  compose → `layoutSnapshot()` → `LayoutSnapshotJson.toJson(...)`
  workflow with a copy-and-paste baseline / drift-report pattern,
  plus a pointer to the production
  `LayoutSnapshotAssertions.assertMatches(document, "...")` helper
  for in-test usage.
- `WeeklyScheduleFileExample` rewritten to delegate to a new reusable
  `examples/support/WeeklyScheduleRenderer`. The renderer's typed
  surface — `JobTitle` enum, `StaffMember` / `DayPlan` / `Shift`
  records, sealed-interface `Half` and `DayShift` types with factory
  methods (`DayShift.OFF`, `.acrossDay(start, end, ShiftType.STOCK)`,
  `.shifts(lunchStart, lunchEnd, dinnerStart, dinnerEnd)`,
  `.lunchOnly(...)`, `.dinnerOnly(...)`,
  `.halves(Half.shift(...), Half.STANDBY)`) — replaces the cryptic
  string tokens used previously. `Theme` (with `aurora()` default and
  a per-`ShiftStatus` colour map) and `Layout` (page size + margin +
  column widths) records keep every colour and dimension out of the
  renderer's static state, so re-skinning the schedule is a
  swap-one-record call. Auto-fills the seven day labels from a
  `LocalDate weekStart`, sorts staff by `JobTitle.ordinal()`, and
  emits a separator row at every job-title boundary so adding or
  removing a `StaffMember` never requires updating positional indices.
  The example file shrinks from ~700 lines of literal data to ~180
  lines of typed declarations.

### Documentation

- README quick-start refreshed to open with a
  `BusinessTheme.modern()`-driven hero (`softPanel` + `accentLeft` +
  `theme.text().h1()`); the plain-text DSL stays underneath for
  callers who do not want a theme.
- New "v1.5 sample renders (PDF)" section links six committed PDFs
  under `assets/readme/v1.5/` so the README works without running
  anything.
- New [`examples/README.md`](examples/README.md) examples gallery —
  every example listed with description, key code snippet, committed
  PDF preview, and source link, grouped by category (built-in
  templates / cinematic templates / v1.5 feature showcases / public-
  API surface / production patterns / operational documents).
  Committed PDF previews of all 22 examples live under
  [`assets/readme/examples/`](assets/readme/examples/) (whitelisted in
  `.gitignore`) so users can browse renders straight from GitHub
  without running anything.
- New [`docs/template-authoring.md`](docs/template-authoring.md) (~620
  lines) — the canonical cheatsheet covering builder hierarchy, a
  per-builder one-liner cheatsheet, a style-types reference, the
  theme system in 60 seconds, six golden patterns, ten anti-patterns,
  a 40-line `StatusReportTemplateV1` skeleton, and a "where to look
  next" map.
- New recipes:
  - [`docs/recipes/shape-as-container.md`](docs/recipes/shape-as-container.md)
  - [`docs/recipes/transforms.md`](docs/recipes/transforms.md)
  - [`docs/recipes/tables.md`](docs/recipes/tables.md) (row span /
    zebra / totals / repeating header)
  - [`docs/recipes/shapes.md`](docs/recipes/shapes.md) (filled cards,
    dividers, spacers, lines, ellipses, image fit, soft panels)
  - [`docs/recipes/extending.md`](docs/recipes/extending.md)
- [`docs/recipes.md`](docs/recipes.md) is now a pure index linking
  every topic-focused recipe page plus four 5-line "common DSL
  primitives" starter snippets.
- [`docs/canonical-legacy-parity.md`](docs/canonical-legacy-parity.md)
  gains a "Shape-as-container (clipped)" row recording the DOCX
  fallback rule.
- New [`docs/migration-v1-4-to-v1-5.md`](docs/migration-v1-4-to-v1-5.md)
  — fresh migration guide for v1.4 consumers.

### Performance — v1.5 baseline

`CurrentSpeedBenchmark` smoke profile (single-thread, 30 warmup +
100 measurement iterations per scenario) recorded on Java 21,
Windows 11. All five scenarios are well within healthy production
ranges.

| Scenario | Avg ms | p50 ms | p95 ms | Docs/sec | Peak MB |
|---|---:|---:|---:|---:|---:|
| `engine-simple`        |  2.25 |  1.96 |  4.20 | 444.60 |  22 |
| `invoice-template` (V1) | 13.39 | 13.12 | 17.55 |  74.67 | 182 |
| `cv-template` (V1)     |  6.94 |  6.58 | 10.18 | 144.02 |  78 |
| `proposal-template` (V1) | 15.77 | 15.50 | 18.31 |  63.43 | 182 |
| `feature-rich`         | 36.80 | 32.06 | 35.51 |  27.18 |  94 |

Stage breakdown (median ms per stage):

| Scenario | Compose | Layout | Render | Total |
|---|---:|---:|---:|---:|
| invoice-template       | 0.249 | 2.774 | 6.042 |  9.312 |
| cv-template            | 0.173 | 2.343 | 1.544 |  4.087 |
| proposal-template      | 0.256 | 8.715 | 5.345 | 14.563 |

The smoke profile is single-thread by design; throughput numbers
reflect "one document at a time" latency, not concurrent throughput.
The formal "no >5% regression" gate first activates between this
baseline and the next snapshot.

### Tests

- 675/675 green (was 525 on v1.4.1) — +150 new tests across:
  - shape-clip-path fragment ordering and pagination invariants
    (`ShapeContainerBuilderTest`, `ShapeContainerInvariantsTest`)
  - transform mixin contract and CTM checks
    (`DocumentTransformTest`, the
    `everyTransformBeginInArbitraryDocumentHasMatchingEndOnSamePage`
    architecture-guard test)
  - per-layer z-index ordering and stable-tie behaviour
    (`ShapeContainerZIndexDemoTest` plus the two zIndex cases on
    `ShapeContainerBuilderTest`)
  - table row-span / zebra / totals / repeating-header invariants
    (`TableBuilderRowSpanTest`, `TableBuilderZebraAndTotalsTest`,
    `TableBuilderRepeatHeaderTest`)
  - `InvoiceTemplateV2` / `ProposalTemplateV2` invariants and three-
    theme demo renders
    (`InvoiceTemplateV2Test`, `InvoiceTemplateV2DemoTest`,
    `ProposalTemplateV2Test`, `ProposalTemplateV2DemoTest`)
  - custom `BusinessTheme` end-to-end
    (`CustomBusinessThemeDemoTest`)
  - HTTP streaming contract (`HttpStreamingDemoTest` —
    no-close-on-caller invariant)
  - layout-snapshot determinism
    (`LayoutSnapshotRegressionDemoTest`)
  - `CvTheme.fromBusinessTheme` mapping
    (`CvThemeBusinessThemeAdapterTest`)
  - six modernised CV templates rendered to file at expected page
    counts (`CvTemplateRenderTest`)
  - `Transformable<T>` contract pinned for every leaf builder that
    opted in (`TransformableLeafBuildersTest`): default identity
    transform, `rotate(...)` / `scale(...)` propagation, identity
    short-circuit emits no markers, non-identity wraps the leaf
    payload with matching transform-begin / transform-end carrying the
    same owner path

### Migration from v1.4.x

- `RowBuilder.gap(double)` is deprecated in favour of
  `spacing(double)`. The deprecated alias still compiles; CV
  templates and runnable examples were migrated.
- `RowBuilder.add(node)` now throws `IllegalArgumentException`
  eagerly. Tests that asserted the deferred `IllegalStateException`
  in `build()` must switch their expectation.
- All other v1.4 record signatures stay backward-compatible:
  `LayerStackNode.Layer`, `ShapeContainerNode`, `TableNode`,
  `DocumentTableCell`, `TableResolvedCell`, and
  `BuiltInNodeDefinitions.PreparedStackLayout` ship new canonical
  constructors *and* preserve every existing constructor as a back-
  compat shim that defaults the new fields. `InvoiceTemplateV1` and
  `ProposalTemplateV1` ship side-by-side with the V2 templates;
  callers who want the cinematic look opt in by switching the type.

See [`docs/migration-v1-4-to-v1-5.md`](docs/migration-v1-4-to-v1-5.md)
for the full guide.

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
