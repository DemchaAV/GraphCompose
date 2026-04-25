# Architecture

GraphCompose is split into two practical layers.

## Pipeline overview

The supported V2 flow is:

1. application code describes a document through `GraphCompose.document(...)`, `DocumentSession`, and `DocumentDsl`
2. canonical nodes describe semantic intent: modules, paragraphs, lists, rows, tables, images, dividers, and page breaks
3. `document.layout` prepares those nodes into deterministic layout fragments
4. the shared engine foundation resolves measurement, pagination, placement, and render ordering
5. the active backend turns resolved fragments/entities into output bytes

In short, the runtime pipeline is:

`document -> semantic nodes -> layout fragments -> pagination -> render`

That separation is the core project concept. Public code describes document intent, layout resolves geometry, and renderers only draw already-resolved output.

The same separation also enables layout snapshot regression tests. Test code can inspect the resolved document after layout and pagination, before rendering, through `DocumentSession.layoutSnapshot()`. Older low-level snapshot adapters remain internal compatibility paths and are not part of the supported public workflow.

Semantic nodes are renderer-neutral. Link, bookmark, and barcode metadata live in `document.node.DocumentLinkOptions`, `DocumentBookmarkOptions`, and `DocumentBarcodeOptions`; PDF-specific translation happens inside `document.backend.fixed.pdf`.

`DocumentDsl` is intentionally a small facade. The public builder classes live beside it in `com.demcha.compose.document.dsl`, while implementation helpers such as semantic name normalization stay in `document.dsl.internal`.

The rendering layer now also has an explicit render-pass seam:

- the engine opens one backend-neutral render session for one document render pass
- the session owns page availability and page-local drawing surfaces
- handlers may change graphics or text state while drawing, but they must restore that state before returning
- handlers must never close session-owned surfaces directly

For the PDF backend this seam is implemented as a page-scoped session that reuses one `PDPageContentStream` per page for the lifetime of the pass. That keeps PDFBox lifecycle concerns inside the PDF renderer while still letting the engine stay format-neutral for future DOCX/PPTX-style backends.

For pagination-sensitive trees, GraphCompose relies on a child-first page-breaking order. Fixed leaf objects are resolved before their parent containers so parent `ContentSize` can reflect child shifts before container placement is finalized.

The engine now materializes one deterministic hierarchy snapshot per layout pass:

- parent links come from `ParentComponent`
- sibling order comes from `Entity.children`
- roots, layers, and depth metadata are rebuilt for every pass instead of being reused across runs

That keeps layout, pagination, snapshot extraction, and render backends aligned on the same tree semantics.

See [pagination-ordering.md](./pagination-ordering.md) for the detailed explanation of why this rule exists and how ordering bugs can look like render bugs.

## Entity responsibilities and helper boundaries

`Entity` is intentionally being pushed back toward a thinner ECS-style role.

Today `Entity` owns:

- stable identity
- the component map
- canonical child order through `Entity.children`
- a cached render marker reference for fast `hasRender()` checks

Layout-specific math and pagination mutation now live in dedicated helpers:

- `EntityBounds` owns bounding-line and outer-edge calculations derived from `Placement`, `ContentSize`, and optional `Margin`
- `ParentContainerUpdater` owns parent-container size and page-shift propagation rules used during pagination-sensitive updates

Deprecated helper methods still exist on `Entity` as compatibility delegates, but new code should treat those methods as migration shims rather than extension points.

## Engine layer: `com.demcha.compose.engine.*`

- `com.demcha.compose.engine.*` contains the ECS engine internals, geometry, layout resolution, pagination, measurement, and rendering systems.
- `com.demcha.compose.font.*` contains public font registration, lookup, and PDF font helpers.
- `com.demcha.compose.engine.text.*` contains internal text utilities used by layout/render hot paths.
- `com.demcha.compose.engine.text.markdown.*` contains markdown-to-text-token parsing helpers used by semantic text preparation.

This layer is the reusable document engine foundation. It is responsible for turning canonical layout state, ECS components, and styles into positioned render output. It is not a supported application authoring API.

### Semantic modules

Canonical modules represent full-width document sections rather than plain vertical container aliases.

- modules resolve their width from the parent inner box
- modules keep that width stable even if one child is wider
- modules primarily grow in height as content is added
- page roots should therefore be canonical `pageFlow(...)` flows that stack modules

### Table layout in the engine

The current table implementation lives in the canonical layout plus shared engine layer, not in legacy templates.

Its shape is intentionally hybrid:

- `DocumentDsl.table(...)` and template table specs create semantic table nodes
- the table layout materializes breakable rows and deterministic cell payloads internally
- rows materialize as atomic leaf entities with precomputed cell payload
- row rendering is page-aware, which lets the engine draw both fragment edges at page breaks without double-drawing separators inside a page

This keeps table pagination consistent with the rest of the engine while avoiding a separate ad-hoc table layout subsystem.

### Measurement and renderer ownership

- Engine builders and layout helpers should consume an engine-level `TextMeasurementSystem` instead of reaching through `LayoutSystem` into the active renderer.
- Render marker components should primarily identify what needs to be rendered.
- Backend-specific drawing logic should live in renderer-owned handler packages such as `...render.pdf.handlers` and backend helper packages such as `...render.pdf.helpers`.
- `RenderStream` should act as a session factory, not as a per-entity content-stream opener.
- `RenderPassSession` is the shared seam for page lifetime and page-surface reuse; it must stay free of PDFBox and backend package imports.
- The PDF entity path dispatches through registered render handlers only; backend-specific render interfaces are not part of the preferred engine extension seam.
- Renderer-specific draw ordering should be backend-neutral at the policy level and backend-owned at the integration level. In practice the engine exposes resolved layout coordinates, while each backend chooses how to consume the shared deterministic render order for its output format.
- `EntityRenderOrder` is the shared render-order helper for resolved entities. It now precomputes lightweight sort entries per layer before sorting so render ordering stays deterministic without repeated component lookups inside the comparator hot path.

### Migration rule for new renderables

- New engine entity renderables must implement backend-neutral `Render`, not backend-specific render interfaces.
- New PDF drawing code must live in renderer-owned handlers under `...render.pdf.handlers`.
- PDF-only helper objects that are not entity render markers should live under renderer-owned helper packages such as `...render.pdf.helpers`.
- Engine-side text sizing and line metrics must come from `TextMeasurementSystem`, not from `LayoutSystem -> RenderingSystem`.
- The PDF entity path does not support backend-specific render fallback paths.

Fixed leaf primitives such as `Rectangle`, `Circle`, `Image`, and `Line` follow the same general engine contract:

- they materialize as regular entities with render/content/layout components
- they rely on normal `ContentSize`, `Padding`, `Margin`, and `Placement`
- they do not introduce a separate layout subsystem or pagination model

## Template layer: `com.demcha.compose.document.templates.*`

- `com.demcha.compose.document.templates.*` contains the canonical higher-level template contracts, built-ins, DTOs, themes, registries, and scene helpers.
- These classes sit on top of the semantic `document.*` API and package common document structures into reusable templates.
- `...templates.api` contains template-facing contracts and registries.
- `...templates.builtins` contains concrete canonical template implementations.
- `...templates.support.common` contains backend-neutral composition primitives, while domain packages such as `...templates.support.cv`, `...templates.support.business`, and `...templates.support.schedule` contain concrete scene composers.
- canonical template contracts are compose-first: `compose(DocumentSession, ...)` is the primary seam.
- canonical built-ins should start one semantic page-flow root through `DocumentSession.pageFlow(...)`, `DocumentSession.dsl().pageFlow()`, or the internal `TemplateComposeTarget` seam that feeds that same path.

## Current package roots

- `com.demcha.compose.document.*` contains the canonical semantic document API, layout graph, backends, exceptions, and snapshot/debug helpers.
- `com.demcha.compose.document.templates.*` contains the canonical higher-level template layer.
- `com.demcha.compose.engine.*` contains the engine internals, measurement, layout, pagination, and PDF backend foundation.
- `com.demcha.compose.engine.render.word.*` contains the experimental Word-specific rendering path.

## Experimental areas

- The PDF backend is the main supported rendering path.
- The Word backend under `com.demcha.compose.engine.render.word.*` is experimental and should be treated as less stable than the PDF path.
- Future backends should add their own rendering system, render-pass session, text measurement system, and handler set without changing engine builders such as tables or template data models.
- The shared abstraction intentionally stops at render-pass lifetime. PDF text mode, PDF annotations, and `PDPageContentStream` state management stay inside `...engine.render.pdf`.

## Language status

- Java is the primary implementation language.
- The build currently includes Kotlin runtime/plugin support, but the repository does not currently ship production `.kt` sources.
- Public docs should therefore treat GraphCompose as a Java-first library with Kotlin compatibility in the build setup, not as a full dual-language codebase.

## Developer tools

- `dev-tools/` contains local developer helpers and maintenance scripts.
- Files in `dev-tools/` are not part of the runtime library API or the published Maven artifact.

## Regression testing pyramid

GraphCompose now uses a practical three-layer regression strategy:

1. layout math unit tests for isolated calculations
2. layout snapshot tests for deterministic full-document geometry checks
3. PDF render tests for visual smoke coverage and artifact inspection

See [layout-snapshot-testing.md](./layout-snapshot-testing.md) for the snapshot workflow and developer conventions.

## Maintenance references

- [package-map.md](./package-map.md) is the source of truth for package ownership and extension rules.
- [lifecycle.md](./lifecycle.md) describes the document session, layout, pagination, and render lifecycle.
- [logging.md](./logging.md) documents the quiet-by-default lifecycle logger categories.
