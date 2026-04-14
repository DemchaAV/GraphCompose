# Architecture

GraphCompose is split into two practical layers.

## Pipeline overview

The engine flow is:

1. application code uses builders to create entities
2. builders attach components that describe content, style, size, and parent/child relationships
3. `EntityManager` stores the entity graph
4. `LayoutSystem` resolves geometry, placement, and pagination
5. the active rendering system turns resolved entities into output bytes

In short, the runtime pipeline is:

`builder -> entity/components -> layout -> render`

That separation is the core project concept. Builders describe intent, components hold the data, layout resolves geometry, and renderers only draw already-resolved output.

The same separation also enables layout snapshot regression tests. Test code can inspect the resolved document after layout and pagination, before rendering, through `PdfComposer.layoutSnapshot()`.

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

## Engine layer: `com.demcha.compose.*`

- `layout_core` contains the document model, geometry, layout resolution, pagination, and rendering systems.
- `font_library` contains font registration, lookup, and PDF font helpers.
- `markdown` contains markdown-to-text-token parsing helpers used by the engine.

This layer is the reusable document engine. It is responsible for turning entities and styles into positioned render output.

### Semantic modules in the engine

`ModuleBuilder` now represents a semantic full-width section rather than a plain vertical container alias.

- modules resolve their width from the parent inner box
- modules keep that width stable even if one child is wider
- modules primarily grow in height as content is added
- page roots should therefore be regular `vContainer(...)` flows that stack modules

### TableBuilder v1 in the engine

The current table implementation lives in the engine layer, not in templates.

Its shape is intentionally hybrid:

- `ComponentBuilder.table()` creates a public engine-level builder
- the table root materializes as a breakable vertical container
- rows materialize as atomic leaf entities with precomputed cell payload
- row rendering is page-aware, which lets the engine draw both fragment edges at page breaks without double-drawing separators inside a page

This keeps table pagination consistent with the rest of the engine while avoiding a separate ad-hoc table layout subsystem.

### Measurement and renderer ownership

- Engine builders and layout helpers should consume an engine-level `TextMeasurementSystem` instead of reaching through `LayoutSystem` into the active renderer.
- Render marker components should primarily identify what needs to be rendered.
- Backend-specific drawing logic should live in renderer-owned handler packages such as `...pdf_systems.handlers` and backend helper packages such as `...pdf_systems.helpers`.
- `RenderStream` should act as a session factory, not as a per-entity content-stream opener.
- `RenderPassSession` is the shared seam for page lifetime and page-surface reuse; it must stay free of PDFBox and backend package imports.
- The PDF entity path dispatches through registered render handlers only; backend-specific render interfaces are not part of the preferred engine extension seam.
- Renderer-specific draw ordering should be backend-neutral at the policy level and backend-owned at the integration level. In practice the engine exposes resolved layout coordinates, while each backend chooses how to consume the shared deterministic render order for its output format.

### Migration rule for new renderables

- New engine entity renderables must implement backend-neutral `Render`, not backend-specific render interfaces.
- New PDF drawing code must live in renderer-owned handlers under `...pdf_systems.handlers`.
- PDF-only helper objects that are not entity render markers should live under renderer-owned helper packages such as `...pdf_systems.helpers`.
- Engine-side text sizing and line metrics must come from `TextMeasurementSystem`, not from `LayoutSystem -> RenderingSystem`.
- The PDF entity path no longer supports a legacy backend-specific render fallback.

Fixed leaf primitives such as `Rectangle`, `Circle`, `Image`, and `Line` follow the same general engine contract:

- they materialize as regular entities with render/content/layout components
- they rely on normal `ContentSize`, `Padding`, `Margin`, and `Placement`
- they do not introduce a separate layout subsystem or pagination model

## Template layer: `com.demcha.templates.*`

- `templates` contains higher-level CV and cover-letter builders, DTOs, themes, and template registries.
- These classes sit on top of the engine and package common document structures into reusable templates.
- `templates.api` contains template-facing contracts and registry/helper types.
- `templates.builtins` contains concrete template implementations.
- template contracts are now compose-first: `compose(DocumentComposer, ...)` is the primary seam, while PDF-returning `render(...)` overloads remain deprecated compatibility adapters.
- built-in templates should be split into a thin backend adapter plus a backend-neutral scene/composition builder.
- `TemplateBuilder.pageFlow(...)` is the canonical template root that stacks semantic modules in document order.

## Current package roots

- `com.demcha.compose.layout_core.*` contains the engine internals and public builder-facing layout layer.
- `com.demcha.compose.layout_core.system.implemented_systems.word_systems.*` contains the experimental Word-specific rendering path.
- `com.demcha.templates.*` contains the higher-level template layer.

## Experimental areas

- The PDF backend is the main supported rendering path.
- The Word backend under `...implemented_systems.word_systems` is experimental and should be treated as less stable than the PDF path.
- Future backends should add their own rendering system, render-pass session, text measurement system, and handler set without changing engine builders such as tables or template data models.
- The shared abstraction intentionally stops at render-pass lifetime. PDF text mode, PDF annotations, and `PDPageContentStream` state management stay inside `...pdf_systems`.

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
