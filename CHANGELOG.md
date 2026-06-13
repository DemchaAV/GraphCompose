# Changelog

All notable changes to GraphCompose are documented here. Versions
follow semantic versioning; release dates are ISO 8601.

## v1.8.0 — Planned

Open cycle — the chart subsystem and the keep-together pagination control.
Entries land here as they merge.

### Public API

- **Native vector charts** (`@since 1.8.0`). New `com.demcha.compose.document.chart`
  package with a layered, serialization-friendly API: `ChartData` (categories +
  series, type/colour-agnostic), sealed `ChartSpec` (`bar()` / `line()` with
  axis, legend, value-label, and sizing knobs), `ChartStyle` (nullable-field
  cascade merged over `ChartTheme` tokens, per-series paint overrides), and
  `DocumentPaint` (solid, linear, and radial — see the gradient entry below).
  Charts compile at layout time into existing primitives
  (shapes, lines, paragraphs) via `ChartDefinition` — no new render handlers,
  deterministic geometry, covered by the standard snapshot machinery; any
  fixed-layout backend renders charts with no chart-specific code, while the
  semantic DOCX export (which has no layout pass) falls back to the chart's
  categories-by-series data table with a one-time capability warning. DSL:
  `section.chart(spec)` / `chart(spec, style)`. Declarative `NumberFormatSpec`
  keeps specs JSON-serializable. The one unsupported combination
  (`ValueLabelMode.INSIDE`) fails fast with `UnsupportedOperationException`
  instead of rendering silently wrong.
- **Horizontal bars, smooth lines, area fills, stacked totals, legend
  placement.** `ChartSpec.bar().horizontal(true)` transposes the chart
  (categories on Y in reading order, value axis on X, labels at bar ends);
  stacked bars label the category total. `ChartSpec.line().smooth(true)`
  draws deterministic Catmull-Rom curves as **native cubic Béziers** through
  the vector path primitive — one `PathNode` per run, perfectly smooth at
  any zoom level, zero tessellation; `.area(true)` fills each series down to
  the baseline with a translucent series colour (`ChartStyle.areaOpacity`,
  default 0.35) — alpha-blended fills layer legibly, and in smooth mode the
  fill closes the exact stroke curve so fill and stroke edges coincide. `LegendPosition.TOP` and `RIGHT` now lay out as a top
  strip / right column for every chart kind, including pie. The chart
  resolver is split per kind (`BarChartLayout` / `LineChartLayout` /
  `PieChartLayout` over a shared `ChartLayoutSupport`).
- **Axis / grid / label visibility toggles.** `AxisSpec.showTickLabels(false)`
  hides the numeric axis and collapses its gutter; `showGridLines(false)` and
  `ChartStyle.GridStyle` control horizontal/vertical grid lines;
  `ChartSpec.bar()/line().showCategoryLabels(false)` hides the category axis —
  down to a minimal "bars + value numbers only" chart.
- **Pie / donut charts** (`@since 1.8.0`). `ChartSpec.pie()` — one slice per
  category from a single series (multi-series data is rejected loudly).
  Configurable: `donutRatio` (hole size), `startAngleDegrees`, `clockwise`,
  `SliceLabelMode` (VALUE / PERCENT / CATEGORY / CATEGORY_PERCENT) with
  independent value/percent formats, donut-centre KPI text, and a
  category-listing legend. Style cascade adds `sliceStroke` (separator),
  `sliceGapDegrees` (pad angle), and `donutCenterTextStyle`. Sectors compile
  into the new general-purpose `PolygonNode` (arc-tessellated ring polygons at
  a fixed 3° step — deterministic vertices, no new render handlers), which also
  lays the groundwork for SVG icon-path import.
- **Vector path primitive** (`@since 1.8.0`). New `PathNode` — the open-path,
  curve-capable sibling of `PolygonNode`: normalized `DocumentPathSegment`s
  (`moveTo` / `lineTo` / cubic `cubicTo` / `close`; Bézier control points are
  free to overshoot the unit box) are scaled to the node's box and rendered
  with native PDF curve operators, so curves stay perfectly smooth at any
  zoom level instead of being tessellated into straight pieces. Atomic
  pagination, deterministic layout snapshots, fill (non-zero winding rule)
  and/or stroke. This is the leaf vehicle for smooth chart lines, decorative
  design shapes, and future SVG path import. DSL:
  `addPath(p -> p.moveTo(...).curveTo(...).closePath().fillColor(...))` on
  every flow builder authors design shapes directly, and
  `dashed(on, off, ...)` makes the stroke dashed with the same
  `DocumentDashPattern` contract as lines — the pattern follows the curve.
- **Path-outline clipper** (`@since 1.8.0`). `ShapeOutline.Path` joins the
  sealed outline family as the curve-capable sibling of `Polygon`, so a
  shape container can clip its children to — and fill / stroke along — an
  arbitrary native-curve silhouette. `ShapeContainerBuilder.path(w, h,
  segments)` takes raw `DocumentPathSegment`s; `path(w, h, svgPath)` (beta)
  clips to an imported SVG path, turning any icon or logo into a content
  mask under `ClipPolicy.CLIP_PATH`. The outline rides the existing
  vector-path fragment pipeline (one source of truth for native curves) and
  the clip handler emits the same `addPathSegments` geometry, so fill, clip,
  and `addPath(...)` all agree.
- **SVG path import** (`@since 1.8.0`, **beta** — annotated `@Beta` while
  the surface hardens against real-world exporter output). `SvgPath.parse(d)` /
  `parse(d, viewBox...)` in the new `document.svg` package lowers the full
  SVG 1.1 path grammar — absolute/relative `M L H V C S Q T A Z`, implicit
  repetition, quadratics (exact cubic elevation), smooth shorthands, and
  elliptical arcs (deterministic W3C endpoint-to-center conversion, ≤90°
  cubic slices) — into normalized, y-flipped `DocumentPathSegment`s.
  `PathBuilder.svg(svgPath)` drops the result straight into `addPath(...)`:
  any icon's `d` string renders as native PDF curves, no tessellation.
  Syntax errors report the character position; fills keep SVG's default
  non-zero winding rule. On top of it, `SvgIcon.read(file)` / `parse(xml)`
  reads the practical subset of a whole SVG file — every `<path>` plus
  `rect` / `circle` / `ellipse` / `line` / `polyline` / `polygon` lowered to
  path data, `<g>` nesting with `translate` / `scale` / `rotate` / `matrix`
  transforms (affine maps are exact on Bézier control points), and
  `fill` / `stroke` / `stroke-width` styling with SVG inheritance and
  defaults — into ordered layers, and `addSvgIcon(icon, width)` stacks them
  back-to-front on the page. `SvgIcon#node(width)` packages the same layers
  as one ready-to-place node whose box is exactly the icon box, so it
  anchors true inside `ShapeContainer` / `LayerStack` nine-point grids (and
  rows now accept `ShapeContainerNode` children directly — it is the same
  atomic overlay composite as the already-allowed `LayerStackNode`).
  **Gradients render natively**: `linearGradient` / `radialGradient`
  referenced via `url(#id)` — on fills *and strokes* — map to PDF axial /
  radial shadings with exact endpoints (`userSpaceOnUse` and
  `objectBoundingBox` units, `gradientTransform`, percentage offsets,
  multi-stop stitching, one `href` hop for split definitions); gradient
  strokes ride a shading-pattern stroking colour. Underneath,
  `DocumentPaint` gains endpoint-exact `LinearAxis` / `RadialCircle` forms
  and `PathNode` / `PathBuilder` grow `fill(paint)` / `strokePaint(paint)`
  with solid paints normalising to the flat-colour path (byte-identical
  output for non-gradient documents). **Stroke fidelity**: the reader honours
  `stroke-linecap` / `stroke-linejoin` (rendered as native PDF `J` / `j`
  operators via new `DocumentLineCap` / `DocumentLineJoin`, also on
  `PathBuilder.lineCap()` / `lineJoin()`) and `stroke-dasharray`, the full
  CSS named-colour table (147 keywords), `rgb()` / `rgba()` with numbers or
  percentages, `#rgb` / `#rgba` / `#rrggbb` / `#rrggbbaa` hex, and absolute
  length units (`px` / `pt` / `pc` / `in` / `mm` / `cm`) on stroke widths;
  relative units and unknown colours fail with the supported alternatives
  listed. `SvgIcon#node(width)` now scales stroke widths and dash lengths
  with the geometry (they live in user units), so an icon drawn smaller than
  its source no longer renders an over-thick outline. Content the reader
  can't render (`text`, `image`, `use`, masks, clips, filters) is dropped
  with a single deduplicated warn-log per kind instead of silently, and the
  DOCX backend warns once per geometry-only node kind (`path`, `polygon`,
  `shape`, …) it drops. The XML reader refuses DOCTYPEs (no XXE); CSS
  stylesheets, text, filters, focal radials, non-pad `spreadMethod` and
  translucent gradient stops stay deliberately out of scope — the reader
  fails loudly rather than rendering them wrong.
- **Inline sparklines** (`@since 1.8.0`). `RichText.sparkline(w, h, color,
  values...)` draws a filled mini-area silhouette on the text baseline, and
  `sparklineLine(w, h, thickness, color, values...)` a constant-thickness line
  band (full thickness preserved at the peaks). Both runs are smoothed with
  the same Catmull-Rom curve the chart engine uses (densified to 12
  sub-segments per span — facets stay under half a point at sparkline
  sizes), and both compile into the existing inline-shape polygon run — a KPI trend next to a number, a skill trajectory
  inside a CV line.
- **Configurable line-chart point markers.** `PointMarker` draws an ellipse at
  every data point — independent width/height axes, explicit fill (or the
  series paint), and an optional outline ring (`PointMarker.circle(5)
  .withStroke(...)`) that keeps joints legible where lines meet; markers always
  render above all line strokes. Per-point value labels sit at a configurable
  `ChartStyle.valueLabelOffset(...)` from the marker (or bar top) in the
  cascading `valueLabelTextStyle`, draw above strokes and markers behind a
  configurable halo chip (`ChartStyle.valueLabelHalo(...)`, themed white) so
  digits stay legible where lines cross them, and deterministically flip below
  their point when two series' labels would collide at the same category.
- **Gradient fills** (`@since 1.8.0`). `DocumentPaint` graduates to
  `com.demcha.compose.document.style` as the shared paint vocabulary, and
  gradients now actually render: `ShapeNode` gains an optional `fillPaint`
  (`ShapeBuilder.fill(paint)`) that wins over `fillColor`. The PDF backend
  paints `DocumentPaint.linear` as a native axial shading (0° = left→right,
  90° = bottom→top; two stops exponential, more stops stitched) and
  `DocumentPaint.radial` as a radial shading reaching the farthest corner,
  clipped to the shape path — rounded corners included. Chart bars now carry
  their full series paint, so a gradient palette renders as gradients instead
  of degrading to the first stop. Solid paints normalise to the plain
  fill-colour path, keeping existing documents byte-identical; backends
  without shading support fall back to `primaryColor()` by contract. The
  flagship `BusinessReportExample` hero is now fully vector — gradient-sky
  shape plus polygon mountain ranges replace the last Graphics2D raster.
- **Translucent shape colours** (`@since 1.8.0`). `DocumentColor.rgba(r, g, b, a)`
  and `withOpacity(0..1)`: the PDF backend honours the alpha channel on shape
  fills and strokes (rectangles/panels/bars, chart value-label halos, ellipse
  point markers, polygons, inline shapes) via a graphics-state alpha constant —
  e.g. a semi-transparent chart halo lets crossing lines show through faintly.
  Fully opaque colours emit no graphics-state entry, so existing documents stay
  byte-identical. Text/lines and the DOCX backend still render opaquely.
- **`keepTogether()` pagination control** (`@since 1.8.0`). Opt-in flag on
  `SectionBuilder`, `ModuleBuilder`, and `TimelineBuilder` (plus
  `keepEntriesTogether()` for per-entry timeline integrity): a block that does
  not fit in the remaining page space relocates whole to the next page instead
  of orphaning its heading from the content below. Blocks taller than a page
  still flow. Default off — existing layouts are byte-identical.
- **Removed: `ConfigLoader`** (breaking). The `com.demcha.compose.ConfigLoader`
  YAML/JSON config-file helper was an application-bootstrap utility with no
  connection to document rendering — nothing in the library, tests, or
  examples referenced it. Gone with it: the `<optional>`
  `jackson-dataformat-yaml` dependency (ConfigLoader was its only consumer)
  and the YAML entry in the `NoClassDefFoundError` troubleshooting section.
  Consumers who relied on the helper can copy the former ~100-line class into
  their own codebase or load configs directly with Jackson
  (`new ObjectMapper(new YAMLFactory()).readValue(...)`).
- **Debug node labels** (`@since 1.8.0`). The debug overlay grew a second
  layer: backend-neutral `DocumentDebugOptions` (guides + node labels +
  label-text mode, in `document.output` next to the other neutral output
  options) configures fixed-layout rendering via
  `GraphCompose.document(...).debug(...)`, `DocumentSession.debug(...)`, or
  `PdfFixedLayoutBackend.builder().debug(...)`. With `nodeLabels()` enabled,
  every rendered node prints its stable semantic path — the same path
  `layoutSnapshot()` reports — once per node and page, as a small corner
  badge straddling the top edge of the node's bounds (right-aligned 5pt
  Helvetica on a pale halo), so a misplaced block on the sheet reads straight
  back to the builder call that authored it. Labels paint as a single
  deterministic post-pass after all content, so badges always sit on top —
  a container's children or a higher layer can never overdraw the label that
  annotates them. `LabelText.NAME` (default) prints the compact own segment
  (`PriceSummaryTitle[0]`); `FULL_PATH` prints the whole ancestry. Label text
  degrades through the shared WinAnsi fallback (accents like `é` survive,
  anything outside WinAnsi becomes `?` with a `glyph.missing` log). The
  overlay draws strictly on top of content and never touches measurement or
  pagination. `guideLines(boolean)` everywhere became sugar over the options
  object with uniform last-write-wins semantics on all three surfaces —
  node-label settings survive the toggle, `debug(none())` reliably disables
  everything — and disabled debug output stays byte-identical.

### Bug fixes

- **`BEHIND_CONTENT` watermarks no longer wash out the page.** The PDF
  watermark renderer set its low-opacity graphics state in a *prepended*
  content stream without a save/restore pair; PDFBox's `resetContext` only
  isolates appended streams, so the watermark alpha leaked into the entire
  page and every element rendered nearly invisible. The watermark now wraps
  its drawing in `q`/`Q`, keeping page content at full strength. This
  affected every document using the default `DocumentWatermark` layer.
- **DOCX export no longer drops lists.** `DocxSemanticBackend` had no branch
  for `ListNode`, so `addList(...)` content silently vanished from Word
  exports. Lists now map to marker-prefixed paragraphs in the list's text
  style, with nested items indented per depth and keeping their own markers.
  (Found by the recipe fact-check: the docx-export recipe's "what is skipped"
  list could not honestly be written without it.)
- **DOCX list items no longer double-space after the marker.** The new list
  branch concatenated `ListMarker.value()` — which already carries its
  trailing space — with another literal space, so every exported item read
  `"•  text"`, and markerless lists gained a stray leading space. The export
  now uses `ListMarker.prefix()`, matching the fixed-layout text pipeline.
- **DOCX list export fully matches the PDF list pipeline.** The semantic Word
  backend resolved nested-item marker fallbacks against the flat-list marker
  and skipped flat-item normalization, so the two outputs of one session
  disagreed: a nested item without an explicit marker exported as the list
  bullet where the PDF renders the depth cascade (`•` → `◦` → `▪` → `·`),
  an author-typed `"- item"` doubled up as `"• - item"`, and blank items
  produced marker-only paragraphs. Both rules now live in one shared place —
  `ListMarker.defaultForDepth(int)` and
  `ListMarker.normalizeItemText(String, boolean)` (`@since 1.8.0`) — and the
  fixed-layout pipeline and the DOCX export both call them.

### Documentation

- **Browsable feature-catalog PDF.** New flagship `FeatureCatalogExample`
  renders every shipped capability as a self-documenting block: the heading
  lands in the PDF outline (the bookmarks panel works as a clickable index),
  a code panel shows the exact API call, and the live result renders right
  under it — rich text, sparklines, nested lists, timelines, tables, every
  chart kind, images (COVER vs CONTAIN fit), gradients, translucency,
  polygons, vector paths (solid and dashed native Béziers), SVG path import
  and a beta `SvgIcon` tile row, shape basics (dividers, ellipses, soft
  cards), clipped containers, canvas, transforms, barcodes, the
  debug-overlay switch, and the document's own chrome — 23 blocks across
  7 pages. Blocks use `keepTogether()`, so a snippet is never orphaned
  from its result.
- **Recipe coverage is complete.** Nine new cookbook pages close every gap the
  recipe index tracked: rich text, lists, timelines, barcodes, images,
  PDF chrome (metadata / watermark / running header-footer / protection /
  links / bookmarks), translucency, semantic DOCX export, and layout-snapshot
  regression testing. Every snippet is verified against the current API;
  the folder index (`docs/recipes/README.md`) no longer carries a
  "not yet covered" list.
- **Word-export example.** New `WordExportExample`
  (`examples/features/docx`) renders the same `DocumentSession` as a
  fixed-layout PDF *and* an editable Word file via `DocxSemanticBackend`,
  one section per capability-table row: inline runs, nested lists with
  custom markers, tables, side-by-side rows, an embedded image, a page
  break, the chart→data-table fallback, and the geometry that stays
  PDF-only. Committed previews live under `assets/readme/examples/`
  (`word-export-companion.pdf` / `.docx`); the examples module adds the
  optional `poi-ooxml` dependency exactly like a consuming project would.
- **`BusinessReportExample` chart is now a native vector chart.** The flagship
  report's five-quarter Revenue/Profit block previously rasterised a bar chart
  through Graphics2D into an embedded PNG; it now uses `ChartSpec.bar()` with a
  `ChartStyle` palette override (navy/gold) and an explicit 0–100 axis —
  ~90 lines of hand-drawn AWT geometry replaced by a declarative spec.

### Internal

- **Sweep follow-up note for future bisectors.** The v1.8.0 import/Javadoc
  sweep (`f04a7dce`, part of #162) also carried mechanical code rewrites in
  roughly 40 files beyond its stated scope: ~30 private preset `Template`
  classes converted to records, constructor copy-loops replaced with
  `Collections.addAll`, explicit imports collapsed to wildcards, and five
  presets' explicit `section == null` guards folded into
  `SectionLookup.hasContent`'s null tolerance (now documented on the method
  and pinned by `SectionLookupTest`). All rewrites were verified
  behavior-preserving by the full gate at merge time; recorded here so a
  future bisect does not skip that commit on the strength of its message.

### Tests

- Chart geometry pinned without rendering: `NiceScaleTest` golden tables and
  `ChartLayoutResolverTest` exact-position assertions on a font-independent
  text-metrics fake; `ChartLayoutSnapshotTest` layout snapshots + a
  fragment-lowering assertion; `SectionKeepTogetherTest` covers section,
  module, and timeline relocation plus the unchanged default.
- Audit-driven edge-case coverage. DOCX semantic export: nested lists indent
  two spaces per depth, per-depth custom markers survive, lists inside
  sections export, empty lists are a no-op. Pagination: a keep-together
  section taller than a full page still flows instead of relocating. Charts:
  negative bar values extend the axis below zero and measure from the nice
  floor, stacked bars skip non-positive segments, a one-point smooth/area
  line keeps its marker and label, long category labels stay slot-sized,
  tight-width legends keep every entry, all-negative `NiceScale` ranges.

## v1.7.1 — 2026-06-09

Open cycle — bug-fix / housekeeping. Entries land here as they merge.

### Performance

- **Text wrapping stops re-measuring the growing line prefix.** The greedy line
  wrapper in `TextFlowSupport` now keeps a running line width and measures each
  token once, instead of re-measuring the whole accumulated line on every token.
  This removes O(line-length × tokens) measured-character work — and the
  per-glyph sanitize/encode it triggered — from paragraph layout. **Output is
  byte-identical: all layout and visual-regression snapshots pass unchanged.**
  The effect is workload-dependent and concentrated in long-text documents;
  measured locally (same-session A/B, full profile) a long multi-page proposal
  rendered markedly faster, and a measurement-count probe showed ~9× fewer
  measured characters on a long paragraph. No public API or behaviour change.

- **Long-token line breaking is no longer quadratic.** `TextFlowSupport.fitCharacters`
  now binary-searches the break point instead of re-measuring every growing prefix
  one character at a time. For an unbreakable run (long URL/ID, no-space CJK, or a
  very narrow column) this cuts measurement calls and measured characters by
  ~80–85% (probe: 652 → 97 width calls, 36k → 7k measured chars on a 600-char
  token). **Output is byte-identical** — the fit predicate is monotonic, so the
  search returns the same break index. No public API or behaviour change.

- **Text measurement no longer embeds binary fonts into a throwaway document.**
  The layout measurement pipeline used to subset-embed every Google/custom font
  family into a private `PDDocument` that was immediately discarded — repeated on
  every new `DocumentSession`, because each render in a server opens a fresh
  session. Measurement now resolves binary families to a **per-thread cached**
  font (mirroring the existing parsed-TrueType cache) bound to a reusable,
  never-saved document, so a family embeds once per worker thread instead of once
  per session, and opening measurement resources owns no PDF document at all.
  **Output is byte-identical** — both paths read glyph widths and metrics from the
  same parsed `TrueTypeFont`; proven by a 960-case render-vs-measurement
  width-parity check (max |Δ| = 0.0), a new `MeasurementFontParityTest`, and the
  full visual-regression / snapshot suite passing unchanged. Only Google/custom-font
  documents are affected (the standard-14 path never embedded); a measurement probe
  showed the per-session embed waste drop ~94–97% (≈1.5–3 MB and ≈2–4.5 ms of font
  subsetting removed per session after the first on a thread). Standard-14-only
  documents are unaffected. No public API or behaviour change.

- **Glyph-coverage probing is memoized instead of repeated per glyph.** The render
  sanitizer (`GlyphFallbackLogger.sanitize` — shared by paragraph spans, table
  cells, watermark and header/footer chrome, and by width measurement) used to
  call `PDFont.encode` for *every code point of every string* — allocating a
  `String` per glyph and, for any glyph the font cannot encode, **throwing and
  catching an exception** — at measurement and again at render. Coverage is now
  memoized per `(font, code point)`: `encode` runs once per distinct glyph, then
  it is a map lookup, and kept glyphs append by code point with no per-glyph
  `String`. **Output is byte-identical** — the substitution decision is the same
  `encode`, only cached; the glyph-fallback warning cadence is unchanged (pinned
  by `PdfFontSanitizerTest`, and width parity by `MeasurementFontParityTest`).
  This removes real per-glyph work from the render hot path: a long document
  re-probed tens of thousands of glyph occurrences that now collapse to roughly
  the number of distinct characters it uses. No public API or behaviour change.

- **Paragraph render writes font and colour operators only when they change.** The
  paragraph render handler emitted a `setFont` (`Tf`) and `setNonStrokingColor`
  (`rg`) operator for *every* text span, even across the spans of a single-style
  paragraph. It now tracks the last-written `(font, size)` and colour across the
  paragraph's graphics-state block and re-emits only on a real change (invalidating
  after inline images/shapes), so a multi-span single-style paragraph carries one
  `Tf` + one `rg` instead of one pair per span — fewer operators for PDFBox to
  serialize. **Rendered output is unchanged** (the skipped operators were
  redundant); pinned by the visual-regression suite plus a content-stream test
  asserting one `Tf` across many drawn spans. No public API or behaviour change.

- **Table cell text is sanitized once per cell instead of three times.** Resolving
  a table ran each cell's lines through `sanitizeCellLines` separately in the
  natural-width, natural-height and resolve passes, rebuilding the list and its
  per-line control-character cleanup up to three times per cell. The sanitized
  lines are now computed once when the logical grid is built and reused by all
  three passes. **Output is byte-identical** (sanitization is deterministic); on a
  large table this removes the dominant per-cell layout allocation. No public API
  or behaviour change.

- **Process-wide line-metrics cache stops inserting instead of flushing when full.**
  The static line-metrics cache `clear()`-ed every entry once it passed 50,000
  distinct styles — a full flush whose non-atomic check-then-clear is a
  thundering-herd recompute under concurrent rendering. It now stops inserting at
  the cap and keeps the existing entries (distinct styles are few in real use, so
  this is only a pathological-explosion guard; it runs on a cache miss, never on
  the per-measurement path). **Measured line metrics are unchanged.** No public API
  or behaviour change.

- **Auto-size font fitting binary-searches the size grid.** A paragraph with
  `autoSize(...)` resolved its font size by scanning every step from max down to
  min, re-measuring the line at each candidate (up to ~50 measurements). Line width
  is linear in font size, so the fit is monotonic — the search now binary-searches
  the grid for the same boundary in ~log2(n) measurements instead of n. **Output is
  byte-identical** — it returns the same grid size the linear scan did (covered by
  the existing auto-size integration and snapshot tests). No public API or behaviour
  change.

- **Table pagination stops re-copying the tail on every page split.** A table that
  spans many pages is split page-by-page, and each split re-sliced the shrinking
  tail by `List.copyOf`-ing its row and row-height lists — even though the source
  layout already holds those lists immutably, so the copy made continuation
  O(rows × pages). The body-only slice now reuses the immutable sub-list views
  directly. **Output is byte-identical** — same rows in the same order (all table
  layout, pagination, and visual-regression tests pass unchanged); a deterministic
  allocation probe on a 2,500-row / 68-page table shows warm compile allocation
  drop 11,155 KB → 9,851 KB (−11.7%). No public API or behaviour change.

### Deprecations

- **`Font.adjustFontSizeToFit(...)` is deprecated.** The engine-internal
  `Font#adjustFontSizeToFit` (and its `PdfFont` / `WordFont` implementations) is
  unused and incorrect — the only real implementation re-measured with the
  unchanged style, so it always returned the minimum size. Canonical auto-size is
  resolved by the layout compiler. The method is kept for binary compatibility and
  scheduled for removal in the next major.

- **The legacy ECS engine packages are deprecated.** `com.demcha.compose.engine.core`,
  `engine.layout` (and `engine.layout.container`), and `engine.pagination` are the
  original `Entity`-based layout/pagination engine — a parallel second engine
  whose execution path the canonical pipeline
  (`GraphCompose.document() → DocumentSession → LayoutCompiler`) never runs; it
  imports nothing from them directly, and the former `GraphCompose.pdf(...)`
  entry point has already been removed. The ECS execution engine runs only under
  the legacy engine regression tests. The packages are now `@Deprecated` (package
  level, so no deprecation-warning cascade)
  with corrected package docs, to stop misdirecting contributors into optimizing a
  dead engine. The genuinely shared engine packages (`engine.components`,
  `engine.measurement`, `engine.font`, `engine.render`) are **not** deprecated.
  No public API or behaviour change.

- **`TextMeasurementSystem` decoupled from `engine.core.SystemECS`.** The shared
  text-measurement contract (`engine.measurement.TextMeasurementSystem`) dropped
  its vestigial `extends SystemECS` and the no-op `process(EntityManager)` default
  it carried — it was never consumed as an ECS system. The legacy ECS engine now
  obtains the measurement service via `SystemRegistry.registerTextMeasurement(...)`
  / `textMeasurement()` instead of enrolling it as a `process()`-driven system,
  completing the isolation of the deprecated `engine.core` from live and shared
  code (only the legacy engine regression tests still reference it). Dropping the
  super-interface is binary-incompatible on paper, so
  `engine.measurement.TextMeasurementSystem` is excluded from the japicmp gate
  until the baseline advances past this release. No canonical API or behaviour
  change.

- **The legacy ECS PDF render pipeline is deprecated.** Follow-up to the ECS
  engine deprecation above. The `Entity`-based PDFBox renderer
  (`PdfRenderingSystemECS` and its collaborators — `PdfRenderSession`, `PdfCanvas`,
  `PdfStream`, `PdfImageCache`, `PdfFileManagerSystem`, `PdfGuidesRenderer`, the
  render-marker handlers, and the `TableCellBox` / `PdfBookmarkBuilder` helpers) is
  the renderer for the removed `GraphCompose.pdf(...)` surface and now runs only
  under the legacy engine regression tests; canonical PDF output goes through
  `com.demcha.compose.document.backend.fixed.pdf`. Because `engine.render.pdf` is a
  *mixed* package — it also holds the canonical-shared `PdfFont`,
  `GlyphFallbackLogger`, and the header/footer + watermark post-processors — the
  legacy classes were physically moved into a new `engine.render.pdf.ecs`
  (with `.handlers` / `.helpers` sub-packages), which is then `@Deprecated` at
  package level (so no deprecation-warning cascade, same pattern as the ECS engine
  packages). The four genuinely shared `engine.render.pdf` types are **not**
  deprecated and stay put. No behaviour change. The relocated renderer has no
  public entry point and carries no binary-compatibility promise, so the move is
  excluded from the japicmp gate rather than treated as a breaking removal.

### Internal

- **Text-measurement line metrics resolve through the `Font` contract instead of a
  PDF-specific fast path.** `FontLibraryTextMeasurementSystem` previously
  special-cased `instanceof PdfFont` to obtain real ascent/descent/leading — every
  other backend font fell back to a degraded `lineHeight`-only metric — which
  coupled the shared measurement system to `engine.render.pdf.PdfFont` and meant a
  new backend could get first-class metrics only by editing shared code. Vertical
  metrics and the process-wide cache key now live on the backend-neutral `Font<T>`
  seam (`Font.lineMetrics(...)` + `Font.measurementCacheKey(...)`, both `default`
  methods; new `FontLineMetrics` record), so a backend supplies first-class metrics
  by overriding the contract and the shared measurement system no longer imports
  `PdfFont`. Binary-compatible (default methods only; japicmp green) and
  behaviour-neutral — PDF and Word produce identical metrics, covered by the
  existing suite plus new polymorphism tests.

### Tests / tooling

- **Benchmark regression gate and measurement probe (benchmarks module, not part
  of the published library).** `BenchmarkVerdictTool` compares a current-speed run
  to the committed baseline (`baselines/current-speed-full.json`) and reports
  improved / neutral / regressed. The hard gate fails only on an **average-latency**
  regression beyond the noise band; peak heap is **advisory** (the `peakHeapMb`
  used-heap delta is GC-timing noisy — use the probe's per-compile allocation
  bytes for deterministic heap). A single run is advisory; the hard gate needs a
  median (`-Repeat` >= 2).
  `MeasurementCountBenchmark` + `CountingTextMeasurementSystem` capture
  deterministic measurement-call counts and per-compile allocation bytes for
  proving algorithmic / allocation changes (the probe warms up the JVM before its
  allocation window, so `Alloc KB` reflects steady state, not one-time
  class-load / JIT cold-start). `scripts/run-benchmarks.ps1` gains the
  `11-verdict-current-speed` step (skippable via `-SkipVerdict`).

- **Cross-platform A/B benchmark harness.** `scripts/ab-bench.sh` (Linux / macOS /
  Windows Git Bash) joins the PowerShell `scripts/ab-bench.ps1` to compare engine
  speed between two branches — interleaved runs, median, per-scenario diff via the
  existing `BenchmarkMedianTool` / `BenchmarkDiffTool`. A path-filtered
  `ab-bench-smoke` CI job runs it on Linux; `.gitattributes` pins `*.sh` and `mvnw`
  to LF so the wrappers stay runnable cross-platform. Benchmark tooling only — not
  part of the published library.

## v1.7.0 — 2026-06-07

Canonical DSL primitives — additive only, zero breaking changes. Adding public
API turns the open cycle into a minor.

### Public API

- **Inline shape runs — geometry-based dots, diamonds, stars and bullets.** New
  `com.demcha.compose.document.node.InlineShapeRun` (`@since 1.7.0`) joins the
  sealed `InlineRun` hierarchy alongside text and image runs. It draws any
  `ShapeOutline` figure on the paragraph baseline directly from geometry — no
  raster payload, no font glyph — so skill rating dots (`Java ●●●●○`), custom
  bullets and inline status markers no longer depend on a font shipping
  `U+25CF` and friends. Authored through `ParagraphBuilder` / `RichText`
  `dot(...)`, `ellipse(...)`, `diamond(...)`, `triangle(...)`, `star(...)` and
  the generic `shape(ShapeOutline, ...)`; measured into line width and height
  like inline images. A `null` fill paints an outlined figure, a `null` stroke
  a filled one; at least one must be present.
- **New polygon shape geometry, usable block-level and inline.** `ShapeOutline`
  (`com.demcha.compose.document.style`) gains a `Polygon` kind plus a family of
  factories built from normalized `ShapePoint` vertices (`@since 1.7.0`):
  `diamond`, `triangle`, `star`, `polygon`, `arrow` / `arrowRight` / `arrowLeft`
  (4-way `Direction`), `chevron`, `checkmark`, `plus` and `regularPolygon(sides)`.
  Arrows and chevrons read as directional list bullets or inline markers
  between text ("Step 1 → Step 2", "Home › Docs"). `ParagraphBuilder` /
  `RichText` add `arrow(size, Direction, fill)` and `chevron(...)` shortcuts
  (every other kind is reachable through `shape(ShapeOutline, ...)`);
  `ShapeContainerBuilder` exposes matching block outlines. Rectangle,
  rounded-rectangle and ellipse shape containers are unchanged.
- **Inline checkboxes + composite (multi-layer) inline figures.** An inline
  shape run is now a stack of paint layers
  (`com.demcha.compose.document.node.ShapeLayer`, `@since 1.7.0`) drawn overlaid
  and centred, so a figure can compose several outlines — each with its own
  fill/stroke — and still measure and place as one unit on the baseline.
  `ParagraphBuilder` / `RichText` gain `checkbox(size, checked, color)` /
  `checkbox(size, checked, boxColor, checkColor)` (`@since 1.7.0`): a rounded
  frame plus, in the checked state, a centred tick — the todo / checklist marker
  for "some items done, some not". The single-outline `InlineShapeRun`
  convenience constructors are unchanged; every other kind still renders as one
  layer.
- **Swappable tick and arrow designs (the "pick your figure" seam).**
  `ShapeOutline` adds `CheckmarkStyle` (`CLASSIC`, `HEAVY`) and `ArrowStyle`
  (`BLOCK`, `TRIANGLE`) enums plus the overloads
  `checkmark(w, h, CheckmarkStyle)` and `arrow(w, h, Direction, ArrowStyle)`
  (`@since 1.7.0`); the no-style factories delegate to `CLASSIC` / `BLOCK`, so
  the default look is unchanged. `checkbox(...)`, `RichText.arrow(...)` and
  `ParagraphBuilder.arrow(...)` gain matching style overloads, and `checkbox`
  also accepts a raw `ShapeOutline` mark for fully custom ticks. Adding a new
  design is one enum constant plus its vertex ring — the foundation for letting
  a caller choose which tick or arrow to render.
- **`softPanel(...)` gains stroke overloads — rounded fill + outline in one node.**
  `AbstractFlowBuilder.softPanel(color, radius, padding, stroke)` and
  `softPanel(color, cornerRadius, padding, stroke)` (`@since 1.7.0`) apply a
  border stroke alongside the panel fill on the same flow node (section, module,
  page flow), so a rounded, padded background with a thin outline no longer needs
  a separate wrapping node. Equivalent to the always-available
  `softPanel(...).stroke(...)` chain — these overloads just make the one-node
  form discoverable.
- **Per-corner radius on shape containers.** `ShapeContainerBuilder.roundedRect(width,
  height, DocumentCornerRadius)` plus the new `ShapeOutline.RoundedRectanglePerCorner`
  (`@since 1.7.0`) round a container's four corners independently — a card "rounded on
  the left, square on the right" no longer needs a CLIP_PATH-parent workaround, since
  both the outline fill/stroke and the child clip now follow the per-corner geometry.
  `DocumentCornerRadius.left/right/top/bottom(...)` give the common asymmetric presets.
  The single-radius `roundedRect(w, h, double)` overload is unchanged, and uniform
  rounded rectangles render byte-for-byte identically (the clip and fill now share one
  per-corner path implementation, called with four equal radii).
- **Vertical text alignment for shape-container labels.**
  `ParagraphBuilder.verticalAlign(TextVerticalAlign)` (new enum
  `com.demcha.compose.document.node.TextVerticalAlign`, `@since 1.7.0`) seats a
  single line by its cap band within its line box — `TOP` (cap top to the box top),
  `CENTER` (cap band centred) or `BOTTOM` (baseline to the box bottom). Combined
  with a vertically-centred layer placement (`.center(...)` / `.centerLeft(...)`), a
  label dropped into a taller `ShapeContainer` / `LayerStack` "pill" sits where you
  ask instead of always on the font baseline — no compensating offset hacks.
  `TextVerticalAlign.DEFAULT` keeps the pre-1.7.0 baseline seating;
  the correction is derived from font metrics (ascent, descent, leading, cap
  height), not a magic number. Render-only and opt-in — existing layouts are
  byte-for-byte unchanged.
- **Bundled font: JetBrains Mono.** New `FontName.JETBRAINS_MONO` (`@since 1.7.0`)
  joins the built-in `DefaultFonts` catalog with its Regular / Bold / Italic /
  Bold-Italic faces (bundled from the OFL-1.1 release), so monospaced code and
  data blocks render without a system-font install. Usable through any
  `DocumentTextStyle.fontName(...)` and listed in the font showcase.
- **Dashed and dotted lines.** `LineBuilder.dashed(double... pattern)` /
  `dashed()` / `dashed(DocumentDashPattern)` plus the new value type
  `com.demcha.compose.document.style.DocumentDashPattern` (`@since 1.7.0`) make a
  `line(...)` paint an on/off dash instead of a solid stroke — section and résumé
  dividers, timeline connectors, cut-here rules. The pattern alternates paint-on
  and paint-off lengths in points (`dashed(8, 5)`; `dashed()` is a balanced 3pt-on
  / 2pt-off; `dashed(1, 4)` reads as dotted). Carried on the line independently of
  `DocumentStroke`, so the stroke value stays a stable two-component record; lines
  are solid by default and the PDF backend honours the dash (other backends fall
  back to a solid stroke).
- **Semantic timelines.** `addTimeline(timeline -> ...)` on every flow / section /
  module, plus `TimelineBuilder`, `TimelineMarker` and `TimelineEntryBuilder`
  (`com.demcha.compose.document.dsl`, `@since 1.7.0`), lay out a vertical timeline
  where each `entry(marker, e -> e.title(...).meta(...).body(...))` pairs a marker
  with its content along a continuous connector rail — work history, project
  milestones, numbered process steps. Markers are `TimelineMarker.dot`, `circle`,
  `numbered` or `square`; the rail colour/width, gutter, entry spacing and default
  title / meta / body styles are all tunable. Declaring the marker-to-content
  relationship replaces the hand-placed bullet-plus-margin pattern; the timeline
  paginates between entries and a tall entry splits within itself, the rail
  continuing across the page break.
- **`DocumentSession.availableHeight()`.** A one-call alias for
  `canvas().innerHeight()` (`@since 1.7.0`) — the usable page content height
  (page height minus top and bottom margins), the value a composition reads to
  decide how much vertical room a section, sidebar or spacer may fill.
  Previously reachable only through the layout-inspection facade.
- **`headingBar(text, bar -> ...)` — one-call filled title band.** Every flow,
  section and module gains `headingBar(String)` and `headingBar(String,
  Consumer<HeadingBarStyle>)` (`@since 1.7.0`) on `AbstractFlowBuilder`: a
  filled, rounded heading band with a single label, added as a child above the
  body. `HeadingBarStyle` (`com.demcha.compose.document.dsl`) tunes fill, corner
  radius, padding, margin, label text style, alignment and an optional outline
  stroke, each with a sensible default (a light-grey band with a centred bold
  label), so `bar -> bar.fill(brand).textStyle(white)` is enough. Sugar over the
  `softPanel(...).addParagraph(...)` recipe — no new node type, just discoverable.

### Fixed

- **`position(node, dx, dy, align)` offsets are now honored for stacks nested
  inside a fixed slot.** A `LayerStack` / `ShapeContainer` placed inside a row
  column or another layer compiled through the fixed-slot stack path, which
  silently dropped the per-layer offsets (anchoring on alignment only) — so a
  positioned badge or cap could not be nudged from its anchor once nested, even
  though the same call worked at the document root. The nested path now feeds
  the same `PreparedStackLayout` offsets as the root path. Layout for documents
  that did not use `position(...)` inside a nested stack is unchanged.

### Documentation

- **New recipe pages for page composition and font coverage** (closing the
  `docs/recipes/` discoverability gaps G2 / G6):
  [`page-backgrounds.md`](docs/recipes/page-backgrounds.md) (`PageBackgroundFill`
  columns / bands / point-based fills / layering),
  [`layered-page-design.md`](docs/recipes/layered-page-design.md) (choosing
  between page backgrounds, rows, layer stacks and canvases),
  [`absolute-placement.md`](docs/recipes/absolute-placement.md) (`addCanvas` +
  `position(x, y)`), and [`font-coverage.md`](docs/font-coverage.md) (WinAnsi
  limits, `●` vs `•`, and the inline-shape / bundled-font alternatives). Linked
  from the README recipes index, `docs/README.md`, and `docs/recipes.md`.

### Build

- **Showcase website separated from documentation (`docs/` → `web/`), now deployed
  via GitHub Actions.** The static GitHub Pages site (`index.html`, `styles.css`,
  `examples.js`, the generated `examples.json` + `showcase/` gallery assets,
  `robots.txt`, `sitemap.xml`, logo) moved out of `docs/` — which previously had to
  host it because branch-based Pages can only serve repo-root or `/docs` — into a new
  top-level [`web/`](web/) folder, so `docs/` now holds **only documentation**. A new
  [`deploy-web.yml`](.github/workflows/deploy-web.yml) publishes `web/` to Pages from
  the **"GitHub Actions"** source; the old branch-`/docs` `deploy-site.yml` was
  removed. `ShowcaseSync` now writes `web/showcase` + `web/examples.json`,
  `VersionConsistencyGuardTest` reads `web/index.html`, and `cut-release.ps1`
  bumps / commits `web/`. The unused Next.js rebuild under `site/` (added in v1.6.8
  but never deployed) was removed. Also renamed `docs/SHOWCASE.md` → `web/README.md`.
  **⚠️ Action required before the next release reaches `main`:** set
  **Settings → Pages → Source = "GitHub Actions"** — once the move lands on `main`,
  `/docs` no longer holds `index.html`, so a branch-`/docs` Pages source would 404.
  The live site is unaffected until then.

## v1.6.9 — 2026-06-03

Housekeeping cycle plus the public pixel-level visual-regression API (Track N).

### Public API

- **Promoted the pixel-level visual-regression harness to public API.**
  `com.demcha.compose.testing.visual.PdfVisualRegression` and
  `com.demcha.compose.testing.visual.ImageDiff` (`@since 1.6.9`) move from the
  test source set into `src/main/java`, alongside the existing
  `com.demcha.compose.testing.layout.*` semantic snapshot helpers. Library
  consumers can now run the same render-PDF → diff-PNG baseline gate against
  their own presets and templates instead of copying the harness. Behaviour is
  unchanged; the PDF→image step is inlined on PDFBox's `PDFRenderer`.
- Exposed `PdfVisualRegression.APPROVE_PROPERTY` (`@since 1.6.9`) — the
  `graphcompose.visual.approve` system-property name — so consumers can toggle
  baseline-approve mode without hard-coding the string (mirrors
  `LayoutSnapshotAssertions.UPDATE_PROPERTY`).

### Documentation

- Added [`docs/operations/visual-regression-testing.md`](docs/operations/visual-regression-testing.md):
  pixel-vs-semantic guidance, the `PdfVisualRegression` API, approve mode,
  baseline layout, and cross-platform tolerance calibration.
- README "Which API should I use?" gains a pixel-level visual-regression row.
- **Made the entire `com.demcha.compose.document.*` public API Javadoc
  doclint-clean.** Added the missing `@param` / `@return` / `@throws` tags and
  element descriptions across 142 files so `mvn javadoc:javadoc`
  (`doclint=all`) runs warning-free. Java's default `-Xmaxwarns=100` cap had
  masked ~90% of the gaps (true count: 929 warnings, not the ~100 first
  visible). Additive Javadoc only — no behaviour change; the only code
  additions are 16 behaviour-neutral no-arg constructors in
  `layout/definitions/*` (documenting the otherwise-synthesised public default
  constructor) and removal of the `@deprecated` block-tags `doclint` forbids in
  `package-info.java` (the `@Deprecated` annotation + prose body already carry
  the notice).

### Build

- CI Javadoc validation (`maven-javadoc-plugin`, `doclint=all`) now covers the
  public `com.demcha.compose.testing.*` helpers (`testing.layout` + `testing.visual`)
  in addition to the canonical `document` API, so Javadoc regressions on the
  testing surface fail fast in CI. No artifact or behaviour change.
- Bumped `central-publishing-maven-plugin` 0.9.0 → 0.10.0 (the Maven Central
  publishing plugin) and removed the Dependabot block on 0.10.0; the
  release-profile build is verified locally and the Central upload is exercised
  at the next publish.

## v1.6.8 — 2026-06-01

**CV v2 migration completion + design-token expansion.** v1.6.8
finishes the CV v2 migration with hyperlink-aware project / entry
titles: a row authored as `"[GraphCompose](https://github.com/x/y)
(Java, PDFBox)"` now renders the title as a clickable link in the
final PDF, with the technology stack remaining a plain
` (Java, PDFBox)` tail. The mechanism is a small extension to
the inline-Markdown parser used by every CV / cover-letter body
row — the `[label](url)` syntax produces a `RichText.link(...)`
run; bare brackets stay literal; everything else (`**bold**`,
`*italic*`, `_italic_`) keeps working as before. The release also
ships four contemporary `BusinessTheme` factory presets
(`nordic()`, `editorial()`, `cinematic()`, `monochrome()`)
alongside the classic / modern / executive trio, expanding the
built-in design-token range to seven presets. Senior-review
follow-ups from v1.6.7 round out the release: the two registry
mutation entry points on `DocumentSession` are now fully
interchangeable (both refuse to mutate a closed session and both
invalidate the layout cache), `target-branch: develop` is pinned
in Dependabot config so future bumps land on the integration
branch, and `logback-classic` rolls forward to 1.5.34 which
fixes [CVE-2026-9828](https://www.cve.org/cverecord?id=CVE-2026-9828)
(deserialisation whitelist bypass).

**Zero breaking public API changes.** The `japicmp` gate against
the v1.6.7 baseline reports `semver PATCH, compatible bug fix`
across every PR in the cycle. New `BusinessTheme` factories are
pure additions; `MarkdownInline.append` and `plainText` extend
their behaviour without changing their signatures; `ProjectLabel.
parse` keeps its two-field record shape (the `title()` field now
preserves Markdown rather than returning a pre-flattened
projection, but the type contract is unchanged and the visible
text projection is one call away via `MarkdownInline.plainText(
title)`). 1058 tests pass at the release-prep tip.

**Migration from v1.6.7.** No code changes required for typical
usage. If you build a custom renderer on top of
`ProjectLabel.parse`:

- Old `title()` was already the visible plain text (emphasis +
  link syntax stripped). New `title()` preserves the original
  inline-Markdown. Wrap with `MarkdownInline.plainText(...)` to
  recover the old behaviour, or route through
  `MarkdownInline.append(rich, title, style)` to get
  emphasis / link rendering for free (the same path
  `ProjectRenderer` now uses).
- `MarkdownInline.append` consumers automatically pick up link
  rendering for `[label](url)` syntax. If any CV / cover-letter
  fixture in your codebase contained a literal `[...](...) `
  string that previously rendered as text, it will now render
  as a hyperlink. Escape with HTML entities or restructure the
  string if you need to keep it literal.

The next release is **v1.7.0** — the additive canonical-DSL
feature minor (LineBuilder.dashed, inline shapes, TimelineBuilder,
dx shortcuts, recipes docs). See [ROADMAP.md](ROADMAP.md).

### Fixes

- The two `DocumentSession` registration entry points are now
  **fully** interchangeable, not just cache-equivalent.
  `session.registry().register(...)` now calls `ensureOpen()`
  before mutating, matching the behaviour of
  `session.registerNodeDefinition(...)`. Previously
  `registry().register(...)` on a closed session silently mutated
  the registry and invalidated a closed-session cache (harmless
  but semantically odd). After this change both paths throw
  `IllegalStateException` on a closed session. (Track J2 — carry-
  over polish from the v1.6.7 senior review.)

### Internal

- `NodeRegistry` Javadoc updated to call out the v1.6.7 non-final
  relaxation explicitly (Track J4). The class became non-final
  in v1.6.7 (Track I3) so `DocumentSession` could install the
  auto-invalidating subclass; the change was already binary-
  compatible (japicmp classified it as `semver PATCH`). The
  Javadoc just makes the rationale discoverable without reading
  the CHANGELOG.

### Public API

- `MarkdownInline.append(...)` (the inline-markdown adapter used by
  every CV / cover-letter body / row / entry renderer) now
  recognises standard Markdown link syntax `[label](url)` and emits
  a clickable hyperlink run via `RichText.link(label, url)`. Pure
  parser extension — no `CvRow` data-shape change required.
  `MarkdownInline.plainText(...)` is updated in lockstep to strip
  link syntax cleanly so callers that pull a plain-text projection
  (e.g. `ProjectLabel.parse`) keep getting just the visible label.
- `ProjectRenderer.inline(...)` and `ProjectRenderer.titleThenBody(...)`
  now route the project-row title segment through
  `MarkdownInline.append(...)` instead of emitting it as a flat
  `RichText.style(...)` run. End-to-end consequence: a CV row with
  `label = "[GraphCompose](https://gc) (Java, PDFBox)"` renders the
  title as a clickable hyperlink and the stack as plain
  `" (Java, PDFBox)"`. Labels without inline Markdown render
  identically to before. `ProjectRenderer.plainInline(...)` (the
  one-line listing variant) intentionally continues to drop link
  syntax via `MarkdownInline.plainText(...)` because a clickable
  link would not survive the compact formatting context.
- `ProjectLabel.parse(...)` now preserves inline Markdown syntax
  inside the returned `title` (the legacy implementation eagerly
  flattened `**emphasis**` and `[links](url)` via `plainText` and
  then split on the last `(`). The split heuristic now targets a
  trailing `\s+\([^()]*\)\s*$` pattern so a leading
  `[name](https://...)` URL's `(...)` segment is not mistaken for
  the technology-stack delimiter. Callers that only need the
  visible-text projection should pass `title()` back through
  `MarkdownInline.plainText(...)`.
- Four new `BusinessTheme` factory presets `@since 1.6.8`:
  `BusinessTheme.nordic()` (Scandinavian minimal — cool whites +
  slate-blue accent + generous whitespace, for design-studio
  reports and product launch decks),
  `BusinessTheme.editorial()` (warm cream surface + deep ink +
  brick-red accent on a serif body, for long-form proposals and
  annual reports),
  `BusinessTheme.cinematic()` (inverted dark navy surface with
  light text + bright copper accent, for investor pitch decks and
  product launch one-pagers), and
  `BusinessTheme.monochrome()` (pure black-on-white with a single
  bold yellow accent, for brutalist editorial layouts where
  typographic contrast carries the identity). Pure additions —
  no change to the existing `classic()` / `modern()` /
  `executive()` presets. japicmp gate against v1.6.7 reports
  `semver PATCH` (compatible additions only).

### Build

- Bumped `jackson-bom` 2.21.3 &rarr; 2.21.4 (broken 2.22.0 skipped via
  the `.github/dependabot.yml` ignore entry added in v1.6.7),
  `logback-classic` 1.5.32 &rarr; 1.5.34 (fixes
  [CVE-2026-9828](https://www.cve.org/cverecord?id=CVE-2026-9828) —
  deserialization whitelist bypass in `HardenedModelInputStream`),
  `central-publishing-maven-plugin` 0.7.0 &rarr; 0.9.0 (0.10.0
  blocked by the existing ignore entry; revisit after a focused
  release-profile evaluation), `japicmp-maven-plugin` 0.23.1 &rarr;
  0.26.1, and a handful of `maven-*-plugin` minor/patch bumps
  (clean / site / resources / enforcer 3.5.0 &rarr; 3.6.3 / surefire
  3.5.5 &rarr; 3.5.6 / source 3.3.1 &rarr; 3.4.0 / gpg 3.2.7 &rarr;
  3.2.8) ([#115](https://github.com/DemchaAV/GraphCompose/pull/115),
  cherry-picked from `main` to align `develop`).

### CI

- `.github/dependabot.yml` now pins both ecosystems
  (`maven`, `github-actions`) to `target-branch: develop` so future
  grouped PRs land on the integration branch instead of `main`.
  Closes the divergence root cause behind the v1.6.7-era #111 /
  #115 episodes where every Dependabot PR force-split history
  between branches and required a cherry-pick to align.

### Documentation

- New quickstart guide
  [Testing your document](docs/operations/test-your-document.md) —
  end-to-end recipe (author the document &rarr; add a layout
  snapshot test &rarr; bless the baseline &rarr; CI guards the
  shape on every PR), with a "when to use which layer" table for
  the three protection tiers (smoke / layout snapshot / pixel-level
  visual). Complements the existing
  [layout-snapshot-testing.md](docs/operations/layout-snapshot-testing.md)
  reference: that one is reference-style, the new one is
  tutorial-style. README's "What can I do with this?" table row
  now links to both.

### Web

- **New Next.js showcase site** under `site/` is now the official
  GitHub Pages deploy target for v1.6.8 onwards. Fully static
  one-page marketing / playground built with Next.js 14 App
  Router + TypeScript + Tailwind. `next build` emits `./out` (4
  static pages, 99.7 kB first-load JS) and the new
  `.github/workflows/deploy-site.yml` (removed in v1.7.0)
  uploads it to Pages on every push to `main` that touches
  `site/**`. **Repo Settings → Pages source must be flipped to
  "GitHub Actions"** for the workflow to take over from the
  legacy branch-based deploy of `docs/index.html`; both files
  coexist in the tree for one more cycle as a rollback.
- Live code snippets in the Hero / Playground sections mirror
  the canonical README hello-world, `examples/.../InvoiceFileExample`,
  and `ModernProfessional.create()` paths, so a visitor copying
  any snippet into a fresh Maven project pulled at
  `io.github.demchaav:graph-compose:1.6.8` gets compiling code.
  Gallery enumerates the full **16-preset cv/v2 lineup** (15
  paired cover letters; `MinimalUnderlined` ships without a
  paired letter by design).
- `scripts/cut-release.ps1` learns a new `Update-SiteDepsVersion`
  step so the Maven / Gradle install snippets in
  `site/lib/deps.ts` flip in lockstep with the README + pom
  versions at cut time — no more silent drift between the site
  and the real released coordinates. The same release commit
  now also stages `site/lib/deps.ts`.

## v1.6.7 — 2026-06-01

**Transitive dependency cleanup.** v1.6.7 narrows the runtime
classpath GraphCompose imposes on consumers. The Kotlin standard
library is gone (the codebase is Java-first; no production
`.kt` sources exist), the `flexmark-all` aggregator is replaced
with the three modules `MarkDownParser` actually references,
`jackson-dataformat-yaml` is marked `<optional>true</optional>`
(mirroring the existing `poi-ooxml` pattern — only consumers that
load YAML configs through `ConfigLoader` need to pull it in),
`jackson-module-jsonSchema` and the explicit `snakeyaml`
declaration are dropped as unused, and `jcl-over-slf4j` is added
explicitly so PDFBox's `commons-logging` call sites keep routing
through SLF4J after the flexmark narrowing (the bridge was
previously provided transitively via `flexmark-all`). The cycle
also fixes a latent layout-cache staleness bug on
`DocumentSession.registry().register(...)` (Track I3): the
registry returned by `registry()` is now a session-owned wrapper
that invalidates the layout cache on every mutation, matching the
semantics of `DocumentSession.registerNodeDefinition(...)`.

**Zero breaking public API changes.** The `japicmp` gate against
the v1.6.6 baseline reports `semver PATCH, compatible bug fix` —
the one surface delta is `NodeRegistry` becoming non-`final` so
`DocumentSession` can install the auto-invalidating subclass
described above. All existing call sites compile and run
unchanged. The transitive cleanup is a runtime-classpath change,
not a compile-surface change.

**Migration from v1.6.6.** Consumers that relied on dependencies
flowing transitively through GraphCompose must now declare them
explicitly:

| If you transitively depended on… | Add to your build |
|---|---|
| Kotlin stdlib via GraphCompose | `org.jetbrains.kotlin:kotlin-stdlib-jdk8` |
| Flexmark extensions (tables, footnotes, gfm-strikethrough, …) | the relevant `com.vladsch.flexmark:flexmark-ext-*` modules |
| YAML config loading through `ConfigLoader` | `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml` |
| `jackson-module-jsonSchema` | `com.fasterxml.jackson.module:jackson-module-jsonSchema` |
| The `commons-logging` API beyond SLF4J routing | declare `commons-logging:commons-logging` explicitly (GraphCompose intentionally excludes it from PDFBox and bridges via `jcl-over-slf4j`) |

No code changes are required for typical usage — pure-PDF
consumers and JSON-only `ConfigLoader` callers carry on as before.
The next minor with new canonical DSL primitives is **v1.7.0**
(see [ROADMAP.md](ROADMAP.md)).

### Build

- Dropped the `kotlin-stdlib-jdk8` compile dependency, the
  `kotlin-test` test dependency, and the `kotlin-maven-plugin`
  build extension. GraphCompose is Java-first; no production
  Kotlin sources exist, and the runtime now no longer carries
  the Kotlin standard library transitively. Consumers that
  relied on `kotlin-stdlib` flowing through GraphCompose must
  declare it explicitly.
- Replaced the `flexmark-all` aggregator dependency with the three
  modules actually referenced by `MarkDownParser`: `flexmark`
  (core parser + AST), `flexmark-util-ast` (Node / NodeVisitor /
  VisitHandler), and `flexmark-util-data` (MutableDataSet). No
  extension modules (tables, footnotes, gfm-strikethrough, etc.)
  are used by GraphCompose. Consumers that relied on extensions
  flowing through GraphCompose must depend on the relevant
  `flexmark-ext-*` modules explicitly.
- Added `jcl-over-slf4j` as an explicit compile dependency. PDFBox
  3.0.7's `PDDocument.<clinit>` calls `org.apache.commons.logging.
  LogFactory` directly; we exclude PDFBox's own `commons-logging`
  artifact to keep one logging facade, and the bridge routes those
  calls through SLF4J. Previously the bridge was provided
  transitively via `flexmark-all`; making it explicit keeps the
  classpath reproducible after the flexmark narrowing above.
- Marked `jackson-dataformat-yaml` as `<optional>true</optional>`,
  mirroring the existing `poi-ooxml` pattern. The only consumer is
  `ConfigLoader.loadConfigWithEnv(...)` when the caller passes a
  `.yaml` / `.yml` resource; library consumers that load JSON
  configs (or skip `ConfigLoader` altogether) no longer pull in the
  ~1.7 MB SnakeYAML transitive footprint. Applications that load
  YAML configs through this helper must now declare
  `jackson-dataformat-yaml` in their own build.
- Removed the unused `jackson-module-jsonSchema` dependency — no
  code path references it.
- Removed the explicit `snakeyaml` dependency declaration and the
  `snakeyaml.version` property. SnakeYAML is now resolved
  transitively (and `optional`) through `jackson-dataformat-yaml`,
  which version-aligns it with Jackson's BOM.
- Bumped `net.sf.jasperreports:jasperreports` 6.21.3 &rarr; 7.0.7
  in the benchmarks module. Benchmarks are a sibling Maven module
  consumed only by the manual performance harness — no impact on
  library consumers ([#111](https://github.com/DemchaAV/GraphCompose/pull/111)).

### Documentation

- `ConfigLoader.loadConfigWithEnv` Javadoc now states the YAML
  path requires `jackson-dataformat-yaml` on the classpath and
  throws `NoClassDefFoundError` when the optional dep is absent.
- `DocumentSession.registry()` Javadoc now explains that the
  returned registry is a session-owned wrapper whose
  `register(...)` mutates the registry *and* invalidates the
  layout cache, making the two registration entry points
  (`session.registry().register(...)` and
  `session.registerNodeDefinition(...)`) interchangeable.

### Fixes

- `DocumentSession.registry().register(...)` now invalidates the
  layout cache the same way
  `DocumentSession.registerNodeDefinition(...)` does. Previously,
  registering a node definition through `registry()` mutated the
  registry in place but left the cached `LayoutGraph` pinned to
  the previous compile, so a follow-up call to `render(...)` or
  `layoutGraph()` silently returned the stale graph routed through
  the old definition. Implemented by wrapping the session's
  `NodeRegistry` in a private session-owned subclass that funnels
  every `register(...)` call through `invalidate()`. (Track I3.)

### Internal

- `NodeRegistry` is no longer `final` so `DocumentSession` can
  install a session-owned subclass that auto-invalidates the
  layout cache on mutation (see Fixes above). Standalone
  `NodeRegistry` instances retain their previous behaviour.
- Replaced eight residual `org.jetbrains.annotations.NotNull` /
  `@Nullable` usages with `lombok.NonNull` (where the surrounding
  file already used Lombok) or removed them entirely (private
  methods and test fixtures). `org.jetbrains:annotations` is no
  longer on the runtime classpath after the Kotlin removal.

## v1.6.6 — 2026-05-31

**First Maven Central release.** GraphCompose now ships under
`io.github.demchaav:graph-compose:1.6.6` — note the **hyphenated**
artifactId, chosen for readability ahead of the Central debut. The
release adds publishable sources/javadoc jars, GPG-signed artefacts,
a binary-compatibility gate against v1.6.5, the metadata Maven
Central requires, and a substantial documentation polish for the
maturity / stability / migration story.

**Zero breaking changes from v1.6.5.** Existing JitPack callers continue
to resolve through the same coordinates (`com.github.DemchaAV:GraphCompose:v1.6.5`);
existing API surface compiles and runs unchanged (validated by the new
`japicmp` gate against the v1.6.5 baseline). New: the `@Beta`
annotation marker, the `@since 1.0.0` class-level Javadoc on
entry-point packages, and a curated docs pass (decision guide for
the two template surfaces, examples maturity index, explicit API
stability policy).

**Migration from v1.6.5:** no code changes required. Swap the
JitPack `<dependency>` for the Maven Central form
(`io.github.demchaav:graph-compose:1.6.6`). The legacy JitPack URL
keeps resolving for callers pinned to v1.6.5 and earlier.

### Build

- **Binary-compatibility gate against v1.6.5** (`japicmp` profile,
  Track E1). The new `binary-compat` CI job builds the artifact on every
  pull request and diffs it against `com.github.DemchaAV:GraphCompose:v1.6.5`
  pulled from JitPack. Binary-incompatible modifications to the public
  surface fail the build; source-incompatible changes are reported only
  (phased policy, will tighten after the 1.6.6 cut). Run locally with
  `./mvnw -DskipTests -P japicmp verify -pl .`; HTML/MD/XML reports
  land in `target/japicmp/`. JitPack repository is scoped to the
  `japicmp` profile, so downstream consumers do not inherit it.
- **Maven Central publish workflow** (Track D4). New
  [`.github/workflows/publish.yml`](.github/workflows/publish.yml) fires
  on the same `v*` tag push that triggers the existing
  `release.yml`. It re-runs `mvnw verify` at the tagged commit, imports
  the GPG key (Track D2) into the runner keyring, writes the
  `<server id="central">` credentials block into `~/.m2/settings.xml`
  via `actions/setup-java@v5`, then invokes
  `./mvnw -P release -Dgpg.skip=false deploy` — the
  `central-publishing-maven-plugin` (Track D3) uploads to Central and
  blocks until Sonatype's validator responds. Hyphenated tags
  (`-rc`, `-alpha`, `-beta`, `-snapshot`) are explicitly skipped — those
  ship only to JitPack and the GitHub Release pre-release surface.
  A `workflow_dispatch` input lets the maintainer re-publish an
  existing tag without re-cutting it if Central had a transient
  validator hiccup. The workflow is dormant until four GitHub repo
  secrets are wired: `MAVEN_GPG_PRIVATE_KEY`, `MAVEN_GPG_PASSPHRASE`,
  `CENTRAL_USERNAME`, `CENTRAL_TOKEN`.
- **`docs/contributing/release-process.md` updated** with the
  end-to-end Maven Central runbook (Track D4 docs). New § 2.C
  "One-time Maven Central setup (maintainer)" walks through GPG key
  generation, keyserver upload, Sonatype account / namespace
  verification, Central user-token generation, the four GitHub
  secrets, and the release-candidate dry-run strategy. § 2.B
  post-release checklist gains a new step 9 for the Central publish
  alongside the existing JitPack step.
- **Hosted Javadocs via `javadoc.io`** (Track H3). README's
  distribution-status note now points callers at
  [javadoc.io/doc/io.github.demchaav/graph-compose](https://javadoc.io/doc/io.github.demchaav/graph-compose),
  which auto-mirrors any artefact published to Maven Central within
  minutes — no separate hosting infrastructure required. The note
  also pins Maven Central as the going-forward primary distribution
  starting v1.6.6 (JitPack stays available alongside for existing
  callers). The full Central install snippet ("Central as primary,
  JitPack as fallback") lands in the v1.6.6 release-prep PR after the
  first Central publish proves the pipeline end-to-end.
- **`central-publishing-maven-plugin` in the `release` profile**
  (Track D3). Adds Sonatype's `central-publishing-maven-plugin` 0.7.0
  to the existing `release` profile as a packaging extension. Replaces
  the legacy `nexus-staging-maven-plugin` + manual staging-repository
  workflow with a single `deploy` call. Configuration:
  `publishingServerId=central` (matches the `<server id="central">`
  entry the publish workflow writes from `CENTRAL_USERNAME` /
  `CENTRAL_TOKEN` secrets), `autoPublish=false` (validation gate before
  the artefact goes live — flips to `true` once we're confident
  post-D4), `waitUntil=validated` (the build waits for Sonatype's
  validator so any rejection surfaces in the workflow run, not a
  silent stuck upload). Requires the `io.github.demchaav` namespace to
  be verified on `central.sonatype.com` (one-time human step via
  GitHub auth or DNS TXT record). The plugin loads inert until D4's
  workflow provides the credentials.
- **GPG signing in the `release` profile** (Track D2). Adds
  `maven-gpg-plugin` 3.2.7 to the existing `release` profile, binding
  to the `verify` phase to sign main / sources / javadoc / pom
  artefacts — Maven Central rejects unsigned uploads. **Off by
  default**: a new property `<gpg.skip>true</gpg.skip>` keeps local
  `mvn -P release package` runs working without a configured GPG key.
  The publish workflow (Track D4) flips it explicitly with
  `-Dgpg.skip=false` once the `MAVEN_GPG_PRIVATE_KEY` and
  `MAVEN_GPG_PASSPHRASE` secrets are wired. `gpgArguments` declares
  `--pinentry-mode loopback` so non-interactive CI runs accept the
  passphrase from `-Dgpg.passphrase` / `MAVEN_GPG_PASSPHRASE` without
  needing a TTY for `gpg-agent`.
- **`release` Maven profile with sources + javadoc jars** (Track D1).
  Activated with `-P release`, attaches `*-sources.jar` and
  `*-javadoc.jar` to the `package` phase via the standard
  `maven-source-plugin` (3.3.1) and `maven-javadoc-plugin` (3.12.0)
  configurations Maven Central requires. The Javadoc plugin runs with
  `doclint=none` and `failOnError=false` so Lombok-generated members
  and `@Internal` engine surface don't block a publish; warnings are
  surfaced quietly. Default `mvnw verify` still does not pay the
  ~30 s of extra packaging — the profile is off by default and turned
  on by `cut-release.ps1` (once Track D3's central-publishing plugin
  lands) and the publish workflow (Track D4).
- **SCM block canonicalised** in `pom.xml` (Track D1 polish). The
  Central metadata validator is strict about the `<scm>` block:
  `<connection>` now uses `scm:git:https://…` (HTTPS, not the legacy
  `git://` transport) and `<developerConnection>` now uses
  `scm:git:ssh://git@github.com/…` (the canonical SSH URL with the
  `git@` user, not the older `ssh://github.com:…` form). Matches the
  shape every Central artefact's POM carries.
- **New `benchmarks/README.md`** (Track B1). Honest framing for the
  manual benchmark layer ahead of the Maven Central debut: explicitly
  positions the harness as a smoke / diff / endurance tool — not a
  JMH-grade benchmark — and tells callers when *not* to use it
  (publishable performance claims, architectural decisions,
  cross-library comparisons that read too much into a single number).
  Documents the file-by-file role of each runner / report tool, the
  exact CI smoke invocation, and a "How to read a report" cheat sheet.
  Cross-links the planned JMH chain (Track C, B3 → B6 in 1.7.0) so a
  reader knows what's coming and how to identify "rigorous"
  measurements when they arrive.
- **Class-level `@since 1.0.0` Javadoc on the public entry-point
  surface** (Track H1). 26 public types in the canonical user-reached
  packages (`com.demcha.compose.GraphCompose`, `com.demcha.compose.document.api.{DocumentSession, DocumentPageSize, PageBackgroundFill}`,
  `com.demcha.compose.document.dsl.{DocumentDsl, RichText, Transformable}` plus all 19 DSL builders)
  now carry class-level `@since 1.0.0` Javadoc tags so callers can see
  the introduction version at IDE quick-doc / generated Javadoc time
  without trawling CHANGELOG history. New guard test
  `PublicApiSinceTagCoverageTest` source-scans the three entry-point
  roots and fails the build if a new public top-level type lands
  without a class-level `@since` tag; `internal/` sub-packages are
  excluded by convention (`InternalAnnotationCoverageTest` covers those).
  Method-level `@since` backfill for the ~380 public methods in these
  packages is intentionally out of scope here and tracked separately.
- **`maven-enforcer-plugin` gate** (Track E2). Binds three rules to the
  `validate` phase so the build refuses to start when a precondition is
  broken: `requireJavaVersion` (≥ 17 — the declared baseline, catches
  accidental JDK 11 / 15 attempts), `requireMavenVersion` (≥ 3.8.0 —
  the oldest version the planned central-publishing pipeline supports),
  and `requirePluginVersions` (every plugin must declare an explicit
  non-`LATEST` / non-`RELEASE` / non-`SNAPSHOT` version — the
  generalisation of the PR-7.1 exec-plugin drift lesson).
  Default-lifecycle plugins (`clean` / `install` / `site` / `resources` /
  `deploy`) are now pinned in a new `<pluginManagement>` block so
  `requirePluginVersions` has nothing to flag. Minimums and versions
  live in `<properties>` (`enforcer.requireMavenVersion`,
  `enforcer.requireJavaVersion`, `maven.enforcer.plugin.version`).
- **Parallel-session stress test** (Track I2). New
  `DocumentSessionParallelStressTest` drives 32 independent
  `DocumentSession` instances on a fixed-size thread pool through 4
  iterations and asserts (a) all parallel renders produce a layout-graph
  signature byte-equal to the sequential baseline — exercising the
  shared font registry, glyph cache, built-in node definitions, and
  shape-outline cache for race conditions; (b) every PDF output starts
  with the `%PDF` magic, is at least 256 bytes, and has size variance
  under 256 bytes across threads (catching corruption or rare
  non-determinism without locking exact byte counts that timestamps
  could drift). 128 + 128 = 256 renders complete in ~1.6 s locally, so
  the test does not bloat CI. The contract is that each
  `DocumentSession` is single-threaded but the process-wide machinery
  handles concurrent _independent_ sessions safely; this test pins that.
- **`no-poi` Maven profile + CI job** (Track I1). The `poi-ooxml`
  dependency is declared `<optional>true</optional>` so callers that
  render only PDFs don't pay the ~10 MB POI footprint; this PR adds a
  regression gate that proves it. Running `./mvnw -P no-poi test -pl .`
  excludes `poi-ooxml` (and its `poi` / `poi-ooxml-lite` transitives)
  from the surefire test classpath and sets the system property
  `no.poi=true`. DOCX-specific tests (`DocxSemanticBackendTest` and the
  one DOCX export in `DocumentSessionTest`) now carry
  `@DisabledIfSystemProperty(named = "no.poi", matches = "true")` and
  skip cleanly. The rest of the canonical suite (1029 tests, 4 skipped
  under `-P no-poi`) runs green without POI on the classpath. A new
  `no-poi-suite` CI job exercises the profile on every pull request.

### Public API

- **New `@Beta` annotation** (Track H2). Companion to the existing
  [`@Internal`](src/main/java/com/demcha/compose/document/api/Internal.java)
  marker:
  [`com.demcha.compose.document.api.Beta`](src/main/java/com/demcha/compose/document/api/Beta.java)
  signals an **Extension SPI** or **Experimental** surface — a
  deliberately-exposed seam library users can implement or call, but
  whose shape may still evolve between minor releases per the
  [API stability policy](docs/api-stability.md) § 1. First application:
  [`com.demcha.compose.document.layout.NodeDefinition`](src/main/java/com/demcha/compose/document/layout/NodeDefinition.java)
  — the canonical custom-node-type seam, carved out of the otherwise
  `@Internal` `document.layout` package. New
  `BetaAnnotationDocumentationTest` pins the annotation's retention /
  target / `@Documented`-ness / source-Javadoc contract in the same
  shape `InternalAnnotationDocumentationTest` already pins for
  `@Internal`. Additional Extension SPI surfaces (render-handler
  interfaces, fragment-payload interfaces) will gain the marker
  incrementally as their contract solidifies.

### Documentation

- **New flagship example: `EngineShowcase`** + **regenerated
  `assets/readme/repository_showcase_render.png` hero image** ahead of
  the Maven Central debut. A presentation audit before v1.6.6 flagged
  that the existing hero PDF was a dated single-page render and the
  GitHub Pages showcase had 20 broken asset paths (CV v2 migration
  added `-v2` suffixes that `docs/index.html` never picked up). Fixed
  in three commits: (a) `docs/index.html` path repair so every CV /
  cover-letter preview resolves; (b) new flagship
  `examples/.../flagships/EngineShowcase.java` renders a single-page
  cinematic brand promo — a navy + electric-orange composition with a
  rounded clip-frame hero (semantic-graph → polished-PDFs visual
  metaphor), a magazine-headline lockup ("Documents as code. /
  Cinematic by default."), three KPI cards (Templates v2 · 1,033
  tests · v1.6.6 Maven Central), a three-column capability grid
  (Semantic DSL · Deterministic Layout · Cinematic Themes), and a
  footer brand stripe — exercising `ShapeContainerNode` +
  `ClipPolicy.CLIP_PATH` for the hero frame, classpath-loaded image
  embedding (`examples/src/main/resources/engine-hero.png`),
  `softPanel(...)` + `accentLeft(...)` decorators on V2 sections, and
  mixed serif/sans typography; (c) page 1 rasterised to
  `assets/readme/repository_showcase_render.png` via the new persistent
  helper `com.demcha.examples.support.PdfPageRasterizer` (PDFBox-based,
  no external Ghostscript / ImageMagick dependency). The hero now
  reads as the engine's brand register rather than a Lorem-ipsum
  template render.
- **`docs/architecture/package-map.md` updated** alongside H2. A new
  intro paragraph documents the stability-marker convention (Stable
  default; engine packages are package-level `@Internal`; individual
  Extension SPI seams carved out of `@Internal` packages carry
  `@Beta`), and the `document.layout` row calls out `NodeDefinition`
  as the current `@Beta` seam.
- **`docs/api-stability.md` revised** alongside H2 — `@Beta` annotation
  reference cells in §1 are no longer hedged as "pending"; the
  associated quote block lists both annotations side-by-side with the
  guard tests that pin them.

### Engine internals (no behaviour change)

- **`RowSlots` helper extracted** from `LayoutCompiler` and
  `NodeDefinitionSupport`. The defence-in-depth `IllegalArgumentException`
  guard added in v1.6.5 (PR-7.3) for the row weights / children size
  mismatch lived as duplicated inline code at both engine call sites
  with no direct test — a future refactor could have silently deleted
  either copy. The validation now lives in
  `com.demcha.compose.document.layout.RowSlots#validateWeightsMatchChildren`
  (package-private), with `RowSlotsTest` driving it directly. Error
  message is unchanged. `GraphCompose.DocumentBuilder#pageBackgrounds(...)`
  Javadoc now spells out the empty-list-clears semantics in prose, not
  only in the `@param` line.

### Documentation

- **New decision guide: [`docs/templates/which-template-system.md`](docs/templates/which-template-system.md)**
  (Track G1). The repo ships two parallel canonical template surfaces —
  `cv.presets.*` (the "classic" v1.6 rebuild) and `cv.v2.presets.*` (the
  layered architecture, recommended) — under confusingly similar names.
  The new page pins the terminology once, gives a status matrix
  (Recommended / Supported / Legacy / Internal) for every template
  surface and the canonical DSL, walks a decision tree for new code, and
  provides a preset-by-preset migration table from `classic` to
  `layered` plus a 1.x → 2.0 deprecation inventory naming every type
  scheduled for removal. `CanonicalSurfaceGuardTest` allowlist updated
  so the deprecation-inventory section's literal mentions of
  `GraphCompose.pdf(...)`, `PdfComposer`, etc. don't trip the
  legacy-token scan (same allowlist class as the v1.5 → v1.6 migration
  log already in there).
- **`examples/README.md` reorganised by maturity** (Track G2). The
  gallery section was grouped by the GraphCompose release that
  introduced each example (Built-in templates / Cinematic v1.5 /
  v1.5 feature showcases / v1.6 feature showcases / Public-API
  surface / Production patterns / Operational documents) — useful
  history for maintainers, less useful for someone landing on the
  examples folder for the first time. The gallery now categorises
  by maturity / intent: **🚀 Start here**, **🧱 Core DSL**,
  **📋 Templates recommended**, **🔧 Advanced SPI**, **🗄️ Legacy**.
  All 26 examples retained their anchor IDs, so existing deep links
  continue to resolve; only the gallery index is restructured. A
  maturity legend introduces the five tiers and links to
  `docs/templates/which-template-system.md` for the V1 → V2 path that
  the **Legacy** tier points at.
- **New API stability policy: [`docs/api-stability.md`](docs/api-stability.md)**
  (Track G3). User-facing companion to
  [ADR-0003](docs/adr/0003-api-stability-and-internal-marker.md): pins
  the four stability tiers (**Stable**, **Extension SPI**, **Internal**,
  **Experimental**) with what each one promises in patch / minor /
  major releases, the sealed-hierarchy permit-list policy (additive
  variants must degrade gracefully without `default`-branch failures),
  the deprecation window (≥ 1 minor release with `@Deprecated`, removed
  in next major), a per-package tier-lookup table for the canonical
  surface plus the legacy packages headed for 2.0 removal, and an
  "anti-policy" section (no pixel-stable PDFs, no bit-stable artefact
  bytes, no sealed-permit exhaustiveness across minor releases for
  Stable hierarchies). `CanonicalSurfaceGuardTest` allowlist extended
  so the page can name `com.demcha.templates.*` / `com.demcha.compose.v2.*`
  and the legacy `pdf(Path)` factory in the package-tier and
  deprecation-example sections.

## v1.6.5 — 2026-05-30

### Templates v2

- Added the `CenteredHeadline` CV preset to the `cv/v2` layered
  template surface, including its isolated theme tokens, visual
  regression baselines, and reusable `Subheadline` /
  `SectionHeader.flatSpacedCaps` widget support.
- Added the **Mint Editorial** template set: a two-page, two-column
  editorial CV preset `MintEditorial` (centred spaced-caps masthead with
  a full-width mint accent rule; sidebar contact / interests / education /
  expertise / skill-bars / social beside a profile / experience / awards /
  references main column) and its paired `MintEditorialLetter`, both on
  `CvTheme.mintEditorial()` and with visual regression baselines.
- Added two reusable `cv/v2/widgets`: `SkillBar` (data-driven proficiency
  bar — spaced-caps label above a track with a level-positioned marker;
  no bar when the level is absent) and `IconTextRow` (inline icon + text
  row, optionally a single click target), with `WidgetSmokeTest` coverage.
- Added optional proficiency levels to `SkillGroup` via the new
  `CvSkill` record and `SkillsSection.Builder.leveledGroup(...)`. Fully
  backward-compatible: name-only skills carry no level and every existing
  name-based renderer is unaffected.
- Added `MintEditorial.Options` (and a matching `MintEditorialLetter.Options`)
  — an additive masthead colour API (accent, rule, name, and an optional
  full-width page-1 header band) whose defaults reproduce the stock render
  exactly, so the committed look and the parity baselines are unchanged.

### Public API

- **`PageBackgroundFill` band helpers.** Added `topBand`, `bottomBand`,
  `band`, `topBandPoints`, and `bandPoints` factory methods for full-width
  horizontal background bands (top, bottom, or arbitrary vertical offset;
  ratio- or point-based), complementing the existing column helpers and
  building on the v1.6.5 y-coordinate fix below.

### Bug fixes

- **`PageBackgroundFill` y-coordinate.** A partial-height page-background
  fill (`heightRatio < 1.0`) was painted from the page **bottom** upward
  instead of from the `yRatio` top edge the API documents, so a band with
  `yRatio = 0` rendered at the bottom of the page. Fills now convert the
  top-down ratios to the PDF bottom-up origin correctly
  (`y = (1 - yRatio - heightRatio) * pageHeight`); full-page and
  full-height column fills are unchanged. Adds top-/bottom-/mid-band
  regression tests.
- **`GraphCompose.document().pageBackgrounds(emptyList())` now actually
  clears.** The builder's Javadoc promised that an explicit empty list
  overrides any earlier `pageBackground(color)` on the same builder, but
  the implementation skipped empty lists, so `pageBackground(LIGHT_GRAY)`
  followed by `pageBackgrounds(List.of())` still emitted the grey
  background. The guard is removed; the empty list is now the documented
  clear. Adds a regression test.
- **`distributeRowSlotWidths` weights / children mismatch.** When a row
  was constructed with a `weights` list whose size did not match the
  number of children (only reachable by bypassing `RowBuilder` and
  building a `RowNode` directly), the engine's row distribution code
  walked off the end of the `weights` list with a raw
  `IndexOutOfBoundsException`. Both row-distribution call sites
  (`LayoutCompiler#distributeRowSlotWidths`, `NodeDefinitionSupport#measureRow`)
  now reject the mismatch with an `IllegalArgumentException` whose
  message names both sizes and the expected fix. `RowNode`'s canonical
  constructor already validated this at construction time; the new
  engine guards are defence-in-depth for any path that bypasses it
  (e.g. reflection-based deserialization). Adds regression tests for
  the canonical-constructor IAE and the `RowBuilder.build()` ISE.

### Build

- **`byte-buddy` is now `<scope>test</scope>`.** Mockito already excludes
  its transitive `byte-buddy` and the project pins a single version in a
  standalone dependency; that dependency was missing a scope, so the
  published POM advertised `byte-buddy` as a compile dependency even
  though no production code references it. Setting `<scope>test</scope>`
  keeps the version pin but keeps `byte-buddy` out of consumers' runtime
  classpath (`mvn dependency:tree` shows it only as `:test`).
- **CI `exec-maven-plugin` version drift removed.** The CI workflow's
  three benchmark steps invoked
  `org.codehaus.mojo:exec-maven-plugin:3.5.0:java` directly, while
  `benchmarks/pom.xml` already declared `exec-maven-plugin` at `3.6.3`
  for local runs — a silent version split between CI and local invocations
  that grew the surface area to keep aligned. CI now calls the configured
  plugin via `exec:java`, picking up the pinned `3.6.3` from
  `benchmarks/pom.xml`. No behaviour change; one fewer hardcoded version
  to bump.

## v1.6.4 — 2026-05-22

Bug fix + structured-block patch. Adds two new public Block types —
`WorkHistoryBlock` and `EducationBlock` — that let template authors
declare work-history and education entries with explicit (title,
organisation, date, description) / (degree, institution, year,
details) fields instead of relying on the legacy
`MultiParagraphBlock` pipe-separated string parser. Also closes a
Boxed Sections layout defect that bundled the date and description
into the right-aligned date column for any author-supplied line that
used an em-dash (`" — "`), en-dash (`" – "`), or contained
prose-shaped content the parser misread as a date. **No public API
break** — the sealed `Block` permit list grows from six to eight,
existing `MultiParagraphBlock` work-history strings continue to
parse, and the deprecated parser path stays in place for backward
compatibility.

### Templates — new structured blocks

- **`WorkHistoryBlock`.** New public record block carrying a list of
  `Item(title, organisation, date, description)` entries. The
  `BoxedSections` preset renders each item as a structured row:
  title bold on the left, date right-aligned on the same row,
  organisation italic on the next line under the title, and
  description as a full-width paragraph beneath. Other presets fall
  back to a single concatenated paragraph per item. Authors who use
  `WorkHistoryBlock` bypass the legacy
  `BoxedSections#parseWorkEntry` heuristic parser entirely.
- **`EducationBlock`.** New public record block carrying a list of
  `Item(degree, institution, year, details)` entries. Renders with
  the same structured layout as `WorkHistoryBlock` (degree bold
  left, year right, institution italic, details paragraph) so
  Education & Certifications sections visually match Professional
  Experience.
- **Sample data migrated.** `ExampleDataFactory.sampleCvSpecV2` now
  uses `WorkHistoryBlock` for Professional Experience and
  `EducationBlock` for Education & Certifications. The legacy
  `MultiParagraphBlock` pattern remains supported and is exercised
  by `PresetLayoutSnapshotTest` / `PresetVisualParityTest` to lock
  the backward-compat path.

### Templates — parser robustness (legacy path)

- **`parseWorkEntry` accepts em-dash and en-dash.** Used to split
  the post-pipe segment on ASCII `" - "` only; now tries `" — "`,
  `" – "`, and `" - "` in order, mirroring `splitHeading`. Authors
  who typed `"*2024-Present* — Led reusable document flows."` saw
  the whole tail collapse into the date column — this no longer
  happens.
- **`parseWorkEntry` rejects prose dressed up as a date.** The
  loose `looksLikeDate` check accepted any string containing a
  year and a hyphen anywhere, which caused education lines like
  `"... | 2019. First-class honours. Specialisation ..."` to
  parse as work entries (the hyphen inside `"First-class"` was
  enough to satisfy the heuristic). Parser now rejects post-pipe
  segments that contain sentence-ending punctuation (`.`, `:`,
  `;`) when no explicit date / description separator was found,
  letting these lines fall back to plain paragraph rendering.
  Marked `@Deprecated` with a `@deprecated` Javadoc pointing
  callers to `WorkHistoryBlock` / `EducationBlock`.
- **`parseProjectItem`** picks up the same em-dash / en-dash /
  ASCII separator set so future Project items typed with em-dash
  don't regress into "title only" rendering.

### Tests

- `BlockTest.blockSealingPermitsAllEightVariants` updated for the
  two new permitted block types.
- `PresetVisualGalleryTest.sampleSpec` migrated to
  `WorkHistoryBlock` so the visible "primary example" exercises the
  new structured shape.
- `PresetLayoutSnapshotTest` intentionally retained on
  `MultiParagraphBlock` to lock the legacy parser's behaviour.

## v1.6.3 — 2026-05-22

Bug fix patch. Closes two independent hyperlink clickable-area
defects that surfaced on CV gallery presets and made the LinkedIn /
GitHub contact rows hijack each other's clicks (paragraph-level
link path) or drift past their visible text (span-level link path
through multi-space separators). **No public API change** — engine,
DSL, themes, templates, and backend records all stay
source-compatible with v1.6.2.

### Engine

- **Paragraph-level link annotations now hug rendered text.**
  `PdfFixedLayoutBackend` used to emit a paragraph's `linkOptions`
  as a single rectangle covering the entire fragment box
  (`fragment.x()` + `fragment.width()`), ignoring `TextAlign.RIGHT`
  / `TextAlign.CENTER`. Stacked right-aligned contact paragraphs
  (e.g. one per LinkedIn / GitHub icon row in Timeline Minimal /
  Sidebar Portrait / Monogram Sidebar) therefore produced
  full-column-wide rects that overlapped the empty alignment gap of
  neighbouring rows — hovering over GitHub clicked the LinkedIn row.
  The backend now emits one per-line rect tight to `line.width()`
  positioned at the alignment-aware `lineX`, matching how
  inline-span links already worked. Span-level link emission, table
  / shape / barcode payload links, and bookmark anchoring are
  unchanged.
- **Glyph sanitizer preserves all author whitespace.**
  `PdfFont.sanitizeForRender` used to collapse any run of consecutive
  spaces into a single space, both for whitespace-only tokens (the
  `"   "` halves of a `"   |   "` separator) and for inter-word gaps
  in spaced-caps strings (`spacedUpper("ARTEM DEMCHYSHYN")` produces
  `"A R T E M   D E M C H Y S H Y N"` with deliberate triple-spaces
  between words). The collapse shrank the rendered glyph stream
  under measurement, drifting inline-link rectangles ~8pt per
  `"   |   "` separator past their visible labels and visually
  merging spaced-caps titles back into a single run (`"A R T E M D E
  M C H Y S H Y N"` — no word boundary). The sanitizer no longer
  collapses adjacent spaces; newlines / NBSP / non-tab control
  characters still resolve to a single space each, but author
  whitespace is now preserved verbatim so wrap geometry,
  link-rectangle emission, and `showText(...)` all see the same
  string. Layout snapshot baselines for five CV presets and one
  nested-list document widened to reflect the recovered whitespace —
  the deliberate visual change is the bug fix.

### Templates

- **Boxed Sections projects render as title + indented description.**
  The "Projects" module now renders each bullet-list or
  `IndentedBlock` item as two stacked paragraphs — bullet plus bold
  project name (with an optional tech-stack chunk in parentheses) on
  the first line, then a hanging-indented description below aligned
  to the project name (not the bullet). The previous single-line
  rendering ran the project name and description together. Bullet
  marker, hanging-indent, and surrounding modules are unchanged.
  Example data in `ExampleDataFactory.sampleCvSpecV2` and
  `PresetVisualGalleryTest` now ships tech-stack chunks (`"Java 21,
  PDFBox, Maven, JMH"`) so the gallery PDFs reflect the new layout.

### Tests

- New regression in `PdfFixedLayoutBackendFeaturesTest` —
  `shouldTightlyHugRightAlignedParagraphLinkRectangles` — stacks
  three right-aligned link paragraphs and asserts each clickable
  rect hugs its rendered label width (≤ 150pt), sits flush against
  the inner right margin, and does not overlap the Y-band of
  neighbouring rows.
- New regression in `PdfFixedLayoutBackendFeaturesTest` —
  `shouldKeepCenteredInlineLinkRectanglesAlignedAcrossMultiSpaceSeparators`
  — renders a centered contact line built with `"   |   "` separators
  and asserts the three resulting link rectangles preserve
  left-to-right order with non-overlapping X ranges and a sane
  per-separator gap (5..40pt), pinning the bug where collapsed
  whitespace pushed later rects past the line.
- New regression in `PdfFontSanitizerTest` —
  `sanitizeForRender_preservesWhitespaceOnlyTokensVerbatim` — pins
  the whitespace-only short-circuit so render width stays in
  lockstep with `getTextWidth` for tokenised contact-line
  separators.

## v1.6.2 — 2026-05-20

Robustness patch. Closes four engine defects surfaced while building
the Noir corporate CV example: any Unicode glyph that the active PDF
font cannot encode used to crash the whole render, rounded human
input for page sizes hit a 1e-6 capacity check, a `Row` inside a
`LayerStack` content layer was rejected by the validator, and the
existing exceptions did not point at a fix. **No public API change**
— engine, DSL, themes, templates, and backend records all stay
source-compatible with v1.6.1.

### Engine

- **Glyph sanitizer on every PDF text render path.** Any code point
  the resolved font cannot encode (arrows `U+2192`, bullets `U+25CF`,
  emoji, custom unicode) is now substituted with `?` instead of
  throwing `IllegalArgumentException` deep inside PDFBox `showText`.
  New `PdfFont.sanitizeForRender(TextStyle, String)` is the single
  entry point the paragraph / watermark / header-footer / table /
  block-text handlers route through; width measurement
  (`PdfFont.getTextWidth`) uses the same string so wrap geometry
  stays in lockstep with the bytes drawn. First substitution per
  unique `(font, codePoint)` emits a one-shot WARN through the new
  `GlyphFallbackLogger` (category
  `com.demcha.compose.engine.render.pdf.glyph-fallback`); subsequent
  substitutions are silent.
- **Page capacity rounding tolerance.** The full-page check in
  `LayoutCompiler` now uses a dedicated `CAPACITY_TOLERANCE = 0.5pt`
  (≤ 0.18 mm — visually indistinguishable) instead of the
  floating-point `EPS = 1e-6`. Authors who size content at the
  rounded `842.0pt` against the true A4 inner height `841.88977pt`
  no longer hit `AtomicNodeTooLargeException`; overflows of more
  than 1pt still throw. `EPS` stays as is for split / remaining-
  height decisions inside the splittable-leaf path.
- **`Row` allowed inside `LayerStack` content layer.** New private
  `FixedSlotKind { ROW_SLOT, STACK_LAYER_SLOT }` is threaded through
  `compileNodeInFixedSlot` and propagated down recursive calls.
  Validator at the row-in-fixed-slot guard now rejects nested
  horizontal rows only when the parent slot is a real row band; a
  `Row` directly inside a `LayerStack` layer rectangle (or any
  vertical descendant thereof) is now a normal column-row and
  compiles cleanly. Row-in-row still throws — the relaxation does
  not leak into the `ROW_SLOT` path.
- **Exception messages now include action verbs.** The five
  engine-thrown exception messages
  (`AtomicNodeTooLargeException` plus four `IllegalStateException`s
  in `LayoutCompiler`) now say what to try, not just which rule
  fired: "Reduce the node height, split content into multiple atomic
  blocks, or increase the page size"; "Wrap the inner row in a
  LayerStack layer (allowed since v1.6.2), or stack horizontal
  content as sections inside a vertical column"; etc. Existing
  substring-based test assertions stay green.

### Tests

- New `PdfFontSanitizerTest` pins the sanitizer contract: substitution
  policy for unsupported glyphs, ASCII pass-through, single + collapsed
  spaces, empty / null input, width consistency between bullet input
  and `?` substitute, and the direct `sanitizeByFont` escape hatch
  used by raw-`PDFont` helpers.
- New `LayerStackRowCompositionTest` pins both ends of the R3
  contract: three positive cases (row directly inside a layer, the
  Noir-CV shape with a dark band + sidebar/main row, row deep inside
  a layer through vertical sections) and one negative guard
  (row-inside-row still throws).
- `PaginationEdgeCaseTest` gains
  `atomicNodeHalfPointOverCapacityShouldFitWithinTolerance` and
  `atomicNodeClearlyOverCapacityShouldStillThrow` boundary cases for
  the new capacity tolerance.
- Three new visual regression demos write real PDFs under
  `target/visual-tests/` for manual review:
  `glyph-fallback/UnicodeFallback*.pdf` (paragraph + table +
  watermark + header/footer all with unsupported glyphs),
  `page-capacity/PageCapacityToleranceDemo.pdf` (842pt shape on A4),
  `layer-stack/LayerStackRowDemo.pdf` (full Noir-CV shape).
- New `DevelopTest` scratch test class under
  `src/test/java/com/demcha/testing/visual/` renders a minimal
  document for manual API experimentation; output lives at
  `target/visual-tests/develop/Develop.pdf`.
- One layout snapshot baseline
  (`document/nested_list_three_levels.json`) updated as a deliberate
  consequence of the new font-aware width measurement: `ListBuilder`
  default markers for deep nesting (`◦ U+25E6`, `▪ U+25AA`) are
  outside Helvetica's WinAnsi coverage, so they now substitute to
  `?` and widen the list rectangle. Follow-up tracked: ship safer
  ASCII / font-aware list marker defaults in v1.7.

### Documentation

- `README.md` gains a **Companion projects** section linking the
  experimental [`graphcompose-ai-flow`](https://github.com/DemchaAV/graphcompose-ai-flow)
  sister repo (independent codebase, separate lifecycle, no
  dependency from this repo).
- Maintainer email in `pom.xml` and `CODE_OF_CONDUCT.md` corrected
  from the non-existent `demchyshyn.artem@gmail.com` to the real
  inbox `demchishynartem@gmail.com`, so JitPack artifact metadata
  and CoC enforcement contact resolve.

## v1.6.1 — 2026-05-09

Maintenance + compatibility patch. Drops the Java 21 source/target
baseline to **Java 17+** so the library can ship into older
enterprise stacks without a fork, and refreshes test/build
dependencies. **No public API change** — engine, DSL, themes,
templates, and backend records all stay source-compatible with
v1.6.0; existing v1.6.0 callers compile and behave unchanged.

Co-developed with external contributor
[@jottinger](https://github.com/jottinger)
([#8](https://github.com/DemchaAV/GraphCompose/issues/8),
[#10](https://github.com/DemchaAV/GraphCompose/issues/10)).

### Toolchain

- **Java 17 baseline.** `<maven.compiler.release>` flips from `21`
  to `17` across `pom.xml`, `examples/pom.xml`, and `benchmarks/pom.xml`.
  Engine source loses the Java 21–only constructs
  (switch-with-type-patterns, switch-with-deconstruction,
  `List.getFirst()`, `Thread.threadId()`) in favour of Java 17
  –compatible forms. CI runs against Temurin JDK 17.
- **Dependency refresh + CVE pass.** Bumps Jackson `2.20.1 → 2.21.3`,
  Logback `1.5.18 → 1.5.32`, Lombok `1.18.38 → 1.18.46`, POI
  `5.4.0 → 5.5.1`, SnakeYAML `2.4 → 2.6`, AssertJ `3.27.3 → 3.27.6`,
  JUnit `5.12.2 → 5.14.4`, Mockito `5.20.0 → 5.23.0`. Adds explicit
  ByteBuddy `1.18.7` so Mockito works on the Java 25+ access rules.
  Maven plugin bumps: `maven-compiler-plugin 3.13 → 3.15`,
  `maven-surefire-plugin 3.2.5 → 3.5.5`, `exec-maven-plugin 3.5 → 3.6.2`.

### Looking ahead

Maven Central distribution
([#7](https://github.com/DemchaAV/GraphCompose/issues/7)) remains
on the **v1.7.0** roadmap alongside the JMH benchmark migration;
v1.6.1 stays on JitPack as a maintenance release.

---

## v1.6.0 — 2026-05-07

The "expressive" release. Closes the remaining canonical-vs-legacy
parity gaps for advanced authoring without architectural rollback.
Every new primitive ships through `DocumentNode + NodeDefinition +
render handler`. See [`docs/roadmaps/v1.6-roadmap.md`](docs/roadmaps/v1.6-roadmap.md)
for the phased plan, verification gates, and ADRs.

### Headline — "expressive"

- **Nested list ergonomics (Phase A — landed).**
  `ListBuilder.addItem(label, Consumer)` for builder-callback
  child scopes; per-depth marker cascade; mixed flat / nested
  authoring preserves source order. ADR 0012.
- **Composed table cell content (Phase B — landed).**
  `DocumentTableCell.node(DocumentNode)` accepts any composable
  canonical node as cell content (paragraphs, lists,
  layer-stacks, sub-tables) with two-pass measurement. ADR 0013.
- **Controlled free-canvas placement (Phase C — landed).**
  `CanvasLayerNode` — pixel-precise `(x, y)` placement of
  children inside a fixed-size bounding box, with `ClipPolicy`
  clipping and atomic pagination. ADR 0014.
- **Templates v2 preset library (committed).** Canonical CV /
  cover-letter / invoice / proposal surface rebuilt around four
  layers (theme tokens → layout slots → components + blocks →
  spec data); 14 CV presets and 14 paired cover-letter presets
  with one-liner `create(BusinessTheme)` factories, inline
  markdown, hyperlinks, and slot-based multi-column layouts.
  ADR 0011.
- **Architecture hardening (committed).** `@Internal` API
  stability marker, public `PdfFragmentRenderHandler` SPI,
  `DocumentRenderingException` on the convenience render path,
  thread-safety contract documented. ADRs 0003 + 0004.
- **Verify gate**: 819 / 0 / 0 / 0 (`mvnw verify`). 26 runnable
  examples regenerate cleanly through `GenerateAllExamples`.

### Architecture hardening (committed in v1.6 line, develop)

The architecture lane closes the highest-severity findings from the
post-1.5 audit. None of these change author-facing behaviour for
unmodified v1.5 code; they sharpen the public-vs-internal boundary,
open extension points, and split the load-bearing files. See
[`docs/roadmaps/migration-v1-5-to-v1-6.md`](docs/roadmaps/migration-v1-5-to-v1-6.md)
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

### Feature scope (continued)

- **Controlled free-canvas (Phase C — landed).** New
  `CanvasLayerNode` atomic composite accepts children at
  explicit `(x, y)` pixel coordinates inside a fixed-size
  bounding box. Coordinates use the screen convention:
  `(0, 0)` is the canvas's top-left, positive `x` extends
  right, positive `y` extends downward. New `CanvasChild`
  record carries `(node, x, y)`. `CanvasLayerBuilder`
  exposes `position(child, x, y)`, `size(width, height)`,
  `clipPolicy(...)` and is plumbed through
  `AbstractFlowBuilder.addCanvas(width, height, Consumer)`.
  `CanvasLayerDefinition` reuses the existing
  `LayerStackNode` placement plumbing — every child anchors
  at `LayerAlign.TOP_LEFT` and the canvas's `(x, y)` maps
  one-to-one onto the stack layout's `(offsetX, offsetY)`.
  Pagination is atomic; clip policy defaults to
  `ClipPolicy.CLIP_BOUNDS` and reuses the
  `ShapeContainerNode` clipping pipeline. The canvas's
  measured size is explicit (independent of children) so
  the surrounding flow reserves a deterministic rectangle.
  ADR [0014](docs/adr/0014-controlled-absolute-placement.md)
  records why `CanvasLayerNode` is a separate node and why
  absolute placement is rejected as a global policy on
  `RowBuilder` / `SectionBuilder`. Snapshot baseline:
  `src/test/resources/layout-snapshots/document/canvas_layer_basic.json`.
  Showcase: `examples/.../CanvasLayerExample.java`.
  `mvnw verify` → 819 / 0 / 0 / 0.

### Deferred to v1.7

These were on the v1.6 stretch list and did not land in time;
they carry over to v1.7.

- **Phase D — Real PPTX semantic export.** Build out
  `PptxSemanticBackend` from the existing manifest skeleton to a
  working POI-based exporter (paragraphs → text boxes, tables →
  PowerPoint tables, sections → slides).
- **Phase E — Maven Central distribution.** Sonatype OSSRH + GPG
  signing + automated deployment on tag push. Primary install
  coordinates switch to `io.github.demchaav:graphcompose:1.7.0`;
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
[`docs/roadmaps/migration-v1-4-to-v1-5.md`](docs/roadmaps/migration-v1-4-to-v1-5.md).

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
- New [`docs/templates/v1-classic/authoring.md`](docs/templates/v1-classic/authoring.md) (~620
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
- [`docs/architecture/canonical-legacy-parity.md`](docs/architecture/canonical-legacy-parity.md)
  gains a "Shape-as-container (clipped)" row recording the DOCX
  fallback rule.
- New [`docs/roadmaps/migration-v1-4-to-v1-5.md`](docs/roadmaps/migration-v1-4-to-v1-5.md)
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

See [`docs/roadmaps/migration-v1-4-to-v1-5.md`](docs/roadmaps/migration-v1-4-to-v1-5.md)
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

- `docs/architecture/canonical-legacy-parity.md` is updated to reflect the v1.3 capabilities (rows, per-side borders, auto-size text, DOCX export)
- `docs/operations/benchmarks.md` documents the new smoke profile defaults, the GC stabilization point, the linear-interpolation percentile rule, and the stage-breakdown table
- `CONTRIBUTING.md` repository map and package list now describe the canonical functional layout (`document.layout`, `document.backend`, `document.output`) alongside the legacy ECS engine

---

## v1.2.0 - 2026-04-25

### Release identity

- the current canonical API cleanup is being released as **v1.2.0** to match the project's early maturity while still making `GraphCompose.document(...) -> DocumentSession -> DocumentDsl` the preferred authoring path
- Maven coordinates are `io.github.demchaav:graphcompose:1.2.0`; JitPack consumers continue to use `com.github.demchaav:GraphCompose:v1.2.0`
- consumers on `v1.1.x` should adopt the canonical `GraphCompose.document(...)` session-first path; the planned `docs/migration-v1-1-to-v1-2.md` was never written and the canonical surface has stabilised since

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
- `docs/contributing/release-process.md` now describes the current JitPack-first 1.x release flow and runnable examples verification
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
