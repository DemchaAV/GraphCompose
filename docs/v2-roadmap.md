# GraphCompose v2.0 Roadmap

This document tracks the v2.0 stabilisation plan. The goal is *not* to add new features but to land the engine, public API, tests, documentation, and release packaging in a state worthy of a major release.

The roadmap is split into 10 phases. Phases marked **Done** have already been merged on the `graphcompose-v2-engine` branch; phases marked **In progress** or **Planned** describe outstanding work.

> **Working principle.** Stabilise the public API first, harden semantics second, refactor internals third. Do not introduce features that have not already shipped on the v1.x line.

## Phase 0 — Freeze release identity — **Done**

- [x] The next release ships as **v2.0.0** because the public composition surface (`GraphCompose.document() → DocumentSession → DocumentDsl`) is a deliberate replacement of the v1.x template-first API
- [x] Maven coordinates moved to `io.github.demchaav:graphcompose:2.0.0`; JitPack tag remains `v2.0.0`
- [x] CHANGELOG entry committed for v2.0.0
- [x] Migration guide stub published at `docs/migration-v1-to-v2.md`

## Phase 1 — Public API stabilisation — **In progress**

Goal: keep the public surface clean, predictable, and safe.

### 1.1 Public package boundaries — **Partial**

Public:

- `com.demcha.compose`
- `com.demcha.compose.document.api`
- `com.demcha.compose.document.dsl`
- `com.demcha.compose.document.node`
- `com.demcha.compose.document.style`
- `com.demcha.compose.document.table`
- `com.demcha.compose.font`

Internal / advanced:

- `com.demcha.compose.engine.*`
- `com.demcha.compose.document.layout.*`
- `com.demcha.compose.document.backend.*`

Acceptance criteria:

- [x] `PublicApiNoEngineLeakTest` baselines every accepted engine import in the public API and fails on new leaks
- [x] `PdfBackendIsolationGuardTest` keeps PDFBox out of canonical API, DSL, semantic nodes, layout, snapshots, and non-PDF backend contracts
- [x] README states the public entry point is `GraphCompose.document(...)`
- [x] `engine.*` is documented as advanced/internal
- [x] `package-map.md` clearly separates the two zones
- [x] runnable examples import only the public document API for page sizing and authoring

### 1.2 Remove engine types from the public DSL — **Partial**

The public DSL still accepts internal-flavoured types in some places. Targeted replacements:

- `engine.components.style.Margin` / `Padding` → `document.style.DocumentInsets`
- `engine.components.content.text.TextStyle` → `document.style.DocumentTextStyle`
- `engine.components.content.shape.Stroke` → `document.style.DocumentStroke`
- `engine.components.style.ComponentColor` → `document.style.DocumentColor`

Acceptance criteria:

- [x] `GraphCompose.DocumentBuilder#margin(Margin)` and `DocumentSession#margin(Margin)` overloads were removed from the canonical API
- [x] page sizing uses backend-neutral `DocumentPageSize` or point dimensions instead of `PDRectangle`
- [x] the public DSL compiles without importing from `com.demcha.compose.engine.*`
- [x] internal conversions live in `dsl/internal/StyleAdapters.java` and related adapter helpers
- [x] README and example code import only public document style classes

### 1.3 `DocumentSession` lifecycle safety — **Done**

- [x] Idempotent `close()`
- [x] `ensureOpen()` guard on every public authoring / rendering method
- [x] `DocumentSessionLifecycleTest` covering the closed-state contract

## Phase 2 — Split `DocumentDsl` — **Done**

`DocumentDsl.java` is now a small facade. Focused builders live under `com.demcha.compose.document.dsl`:

```
DocumentDsl.java          (facade only)
PageFlowBuilder.java
SectionBuilder.java
ModuleBuilder.java
ParagraphBuilder.java
ListBuilder.java
TableBuilder.java
ImageBuilder.java
ShapeBuilder.java
BarcodeBuilder.java
DividerBuilder.java
PageBreakBuilder.java
internal/
    StyleAdapters.java
    TableAdapters.java
    TextAdapters.java
    SemanticNameNormalizer.java
```

Rules:

- DSL classes hold only authoring intent — no engine vocabulary
- adapter classes are package-private under `dsl/internal`
- public methods read like document language, not engine language

Acceptance criteria:

- [x] `DocumentDsl.java` is now a thin facade
- [x] each builder has a single responsibility and no circular dependencies
- [x] existing snapshot/render tests still pass

## Phase 3 — Semantic layer purity — *Done*

Semantic nodes describe document intent, never PDF implementation. Link, bookmark, and barcode metadata now use renderer-neutral `DocumentLinkOptions`, `DocumentBookmarkOptions`, and `DocumentBarcodeOptions`.

Implemented:

- introduced `document.node.DocumentLinkOptions`, `DocumentBookmarkOptions`, `DocumentBarcodeOptions`, and `DocumentBarcodeType`
- moved PDF translation into the fixed PDF backend/layout adapter layer
- kept PDF-specific options under the PDF backend layer only

Acceptance criteria:

- `document.node.*` has zero PDFBox imports and zero `document.backend.fixed.pdf.options.*` imports
- semantic nodes are renderer-neutral
- PDF links / bookmarks still render correctly
- architecture guard tests enforce the boundary

## Phase 4 — Layout engine hardening — **Partial**

### 4.1 Replace magic constants — **Done**

- [x] `BoxConstraints.UNBOUNDED_HEIGHT` constant centralises the previous `1_000_000.0` literal
- [x] `BoxConstraints.unboundedHeight(width)` factory replaces every internal `new BoxConstraints(_, NATURAL_HEIGHT)` call site

```java
BoxConstraints.unboundedHeight(width);
```

### 4.2 Empty-document behaviour — **Done**

- [x] Empty render now throws `IllegalStateException` with a domain-specific message
- [x] `DocumentSessionLifecycleTest#emptyDocumentRenderShouldThrow` covers this

### 4.3 Pagination edge cases — **Mostly done**

`PaginationEdgeCaseTest` covers the main edge cases:

- [x] single paragraph larger than one page
- [x] page break at start (no-op)
- [x] page break at end (records trailing blank page)
- [x] page break between content
- [x] nested section with padding/margin paginating across pages
- [x] exact-fit shape at page bottom
- [x] near-boundary float (epsilon under inner height)
- [x] tall atomic shape moves to next page when current page is partially used
- [x] atomic image larger than page fails fast with a domain-specific message
- [x] table row too tall fails fast with a domain-specific message
- [x] module split across pages survives PDF header/footer chrome

## Phase 5 — Render backend polishing — **In progress**

Make PDF rendering reliable, fast, and isolated.

### 5.1 PDF backend ownership

Allowed: `document.backend.fixed.pdf.*`, `engine.render.pdf.*`.
Forbidden: PDFBox imports inside `GraphCompose`, `document.api.*`, `document.node.*`, `document.dsl.*`, `document.layout.*`, `document.snapshot.*`, and non-PDF backend contracts.

- [x] `GraphCompose`, `DocumentSession`, and `LayoutCanvas` are free of direct PDFBox imports
- [x] `DocumentPageSize` replaces public `PDRectangle` page-size usage
- [x] PDF metadata/protection/watermark/header/footer options are configured through `PdfFixedLayoutBackend.builder()`
- [x] `PdfBackendIsolationGuardTest` enforces the canonical/non-PDF boundary
- [ ] the public font package still contains PDF font materialisation bridges; move those behind the PDF backend in a later cleanup

### 5.2 Render-pass resource safety

- one `PDPageContentStream` per page per render pass
- handlers never close session-owned streams
- graphics state restored after handler drawing
- text-mode state isolated
- exceptions close PDF resources correctly

## Phase 6 — Testing pyramid — **In progress**

Layers:

- unit tests (math, constraints, style conversion, node preparation)
- layout snapshot tests (resolved coordinates, pages, fragment order)
- PDF render smoke tests (artifacts + visual diffs)
- architecture guard tests (package boundaries, no PDFBox in semantic layer, no engine types in public DSL)
- benchmark tests (PR smoke, scheduled full run)

Important new tests:

- `DocumentSessionLifecycleTest` — **Done**
- `DocumentSessionClosedStateTest` — covered by `DocumentSessionLifecycleTest`
- `PublicApiNoEngineLeakTest` — **Done**
- `SemanticLayerNoPdfBoxDependencyTest` — **Done**
- `EmptyDocumentRenderTest` — covered by `DocumentSessionLifecycleTest`
- `LargeAtomicNodeErrorTest` — covered by `DocumentSessionTest#atomicNodeTooLargeShouldFailDeterministically`
- `PaginationExactFitTest` — covered by `PaginationEdgeCaseTest`
- `TablePaginationRegressionTest` — covered by `PaginationEdgeCaseTest` and existing table pagination tests
- `DslPublicApiCompileTest` — covered by `DocumentationExamplesTest` plus runnable examples compile
- `MigrationExampleCompileTest` — covered by `DocumentationExamplesTest` and migration snippets

## Phase 7 — Performance and benchmarks — **Done**

Rules:

- avoid universal performance claims
- frame benchmark numbers as scenario-specific regression signals
- keep PR benchmark smoke small; full run is weekly / manual
- store JSON / CSV reports as CI artifacts
- document machine variability in `docs/benchmarks.md`

Implemented:

- [x] PR smoke benchmark profile
- [x] scheduled/manual full benchmark workflow
- [x] JSON/CSV artifact output
- [x] median repeated-run mode for local comparison
- [x] conservative benchmark documentation

## Phase 8 — Documentation polish — *Planned*

README target structure (kept short):

```
GraphCompose
Why GraphCompose?
Visual Preview
Installation
Quick Start
Core Concepts
Examples
Documentation
Roadmap
License
```

Detail moves to:

- `docs/architecture.md`
- `docs/getting-started.md`
- `docs/recipes.md`
- `docs/lifecycle.md`
- `docs/package-map.md`
- `docs/layout-snapshot-testing.md`
- `docs/benchmarks.md`
- `docs/migration-v1-to-v2.md`
- `docs/release-process.md`

## Phase 9 — Release packaging — *Planned*

- groupId `io.github.demchaav` ✅
- artifactId `graphcompose` ✅
- version `2.0.0` ✅
- git tag `v2.0.0`
- GitHub release notes
- GitHub repository topics: `java`, `pdf`, `pdf-generation`, `document-generation`, `layout-engine`, `pdfbox`, `declarative-ui`, `templates`, `pagination`, `open-source`
- release checklist in `docs/release-process.md`
- compatibility statement

## Phase 10 — Portfolio / recruiter polish — *Planned*

Positioning:

> GraphCompose is a Java-first declarative document layout engine built on PDFBox, with semantic authoring, automatic pagination, deterministic layout snapshots, reusable templates, and a backend-oriented architecture designed for production document generation.

Pinned-repo description:

> Declarative Java document layout engine with semantic authoring, automatic pagination, PDFBox rendering, layout snapshots, and reusable templates.

## Execution order

| Week | Track |
| --- | --- |
| 1 | Identity + API boundaries (✅ Phase 0, 1.1 baseline, 1.2 deprecation, 1.3, 4.1, 4.2, 4.3 baseline, 6 baseline landed) |
| 2 | DocumentDsl split |
| 3 | Public DSL cleanup (engine type removal — finish DSL methods, shrink allowlists) |
| 4 | Semantic / backend separation (Phase 3 — semantic node PDF options moved out) |
| 5 | Layout / pagination edge cases (Phase 4.3 remainder — table row too tall, module + chrome) |
| 6 | Render + benchmark hardening |
| 7 | Documentation polish |
| 8 | Release packaging + portfolio polish (✅ release-process.md scaffolded) |
