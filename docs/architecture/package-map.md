# Package Map

This document is the source of truth for production package ownership in the canonical engine branch.

**Stability markers.** Public packages default to the **Stable** tier of the
[API stability policy](../api-stability.md). Engine packages (`com.demcha.compose.engine.*`,
`com.demcha.compose.document.layout`, render-pipeline internals) carry the
[`@Internal`](../../core/src/main/java/com/demcha/compose/document/api/Internal.java)
marker at the package level; individual types deliberately exposed as
Extension SPI inside an `@Internal` package carry
[`@Beta`](../../core/src/main/java/com/demcha/compose/document/api/Beta.java)
on the type itself (`NodeDefinition` is the current example). The
"Extension rule" column below names the extension seam where one is
intended.

## Module Layout

The 2.0 module split (see [ADR 0016](../adr/0016-multi-module-packaging.md)) maps these
packages onto Maven coordinates. The package names are unchanged — only which artifact
ships them differs.

| Module | Owns |
| --- | --- |
| `graph-compose-core` | The lean engine: `com.demcha.compose`, the canonical `document.*` authoring surface (`api` / `dsl` / `node` / `style` / `table` / `snapshot`), `document.showcase` (`FontShowcase`), the `document.backend.fixed` SPI seam, the public `document.backend.fixed.pdf.options` records, `document.layout`, `font.*`, and the internal `engine.*` foundation. |
| `graph-compose-render-pdf` | The PDFBox backend: `document.backend.fixed.pdf.**` (the `PdfFixedLayoutBackend` impl + handlers) and the `engine.render.pdf.**` render tree. Registers the PDF `FixedLayoutBackendProvider` / `FontMetricsProvider`. |
| `graph-compose-render-docx` / `graph-compose-render-pptx` | The POI semantic exporters — `document.backend.semantic.docx` / `.pptx`. |
| `graph-compose-templates` | The built-in preset families — `document.templates.**`. |
| `graph-compose-testing` | Consumer test support — `com.demcha.compose.testing.**`. |
| `graph-compose` | Back-compat wrapper: an empty jar over `graph-compose-core` + `graph-compose-render-pdf`. |
| `graph-compose-bundle` | Batteries-included aggregate: the wrapper + templates + `graph-compose-fonts` + `graph-compose-emoji`. |

The per-package tables below describe responsibility and extension rules; the `Module`
column in [api-stability.md § 4](../api-stability.md#4-tier-mapping-per-package) gives the
per-package artifact.

## Public Authoring Surface

| Package | Responsibility | Extension rule |
| --- | --- | --- |
| `com.demcha.compose` | Top-level entrypoint, currently `GraphCompose.document(...)`. | Keep this package tiny; new authoring concepts should live under `document.*`. |
| `com.demcha.compose.document.api` | `DocumentSession` lifecycle and public session operations. | Add only session-level behavior that belongs to every document. |
| `com.demcha.compose.document.dsl` | Developer-friendly DSL facade and focused builders: `pageFlow`, `module`, `paragraph`, `list`, `rows`, `table`, `image`, `divider`. | Prefer simple domain words over low-level geometry or renderer terms. |
| `com.demcha.compose.document.node` | Canonical semantic nodes such as paragraphs, lists, tables, images, sections, and page breaks. | Nodes describe intent; layout/render mechanics belong in `document.layout` or `engine.*`. |
| `com.demcha.compose.document.snapshot` | Public renderer-neutral layout snapshot DTOs for deterministic diagnostics and regression tests. | Keep values immutable and backend-neutral. |
| `com.demcha.compose.document.style` | Public style values such as document colors, spacing, and text styles. | Keep application-facing style values here; convert to engine values inside DSL/backends. |
| `com.demcha.compose.document.table` | Public table authoring values such as columns, cells, and table style overrides. | Keep table authoring ergonomic; row layout and PDF drawing stay internal. |
| `com.demcha.compose.document.backend.fixed.pdf.options` | Advanced PDF backend options: metadata, protection, watermark, headers, and footers. | Configure through `PdfFixedLayoutBackend.builder()`; keep PDFBox implementation behind backend internals. |
| `com.demcha.compose.font` | Public font names, built-in families, registration, and showcase helpers. | Add explicit font definitions; do not add render handlers here. |

## Canonical Layout And Backend

| Package | Responsibility | Extension rule |
| --- | --- | --- |
| `com.demcha.compose.document.layout` (`@Internal` at package level) | Semantic layout compiler, node definitions, fragments, split/measure contracts, and layout graph. | `NodeDefinition` is `@Beta` — Extension SPI for custom node types. New node behavior must be deterministic and covered by layout or render tests. |
| `com.demcha.compose.document.backend.fixed` | Backend-neutral fixed-layout rendering contract. | Keep it independent from PDFBox and semantic template data. |
| `com.demcha.compose.document.backend.fixed.pdf` | Canonical fixed-layout PDF backend, fragment handlers, PDF-specific options, and PDF-backed measurement resources. | Keep PDFBox lifecycle internal; normal callers should use `DocumentSession` and default PDF convenience methods. |
| `com.demcha.compose.document.dsl.internal` | Internal helpers for public DSL builders such as semantic name normalization and builder callback application. | Do not expose these helpers in examples; move reusable authoring concepts to public builder classes instead. |
| `com.demcha.compose.document.backend.semantic` | Experimental semantic export contracts for non-fixed outputs. | Keep exporters separate from PDF fixed-layout rendering. |
| `com.demcha.compose.document.debug` | Snapshot/debug adapters for canonical layout graph inspection. | Debug output should be deterministic and safe for tests. |

## Shared Engine Foundation

| Package | Responsibility | Extension rule |
| --- | --- | --- |
| `com.demcha.compose.engine.components.*` | Low-level ECS components, content payloads, style values, geometry, layout, and render markers. | Use only for engine primitives; public document authoring should go through `DocumentDsl` and semantic nodes. |
| `com.demcha.compose.engine.core` | Entity manager, canvas, traversal context, and base ECS system contracts. | Keep core thin; put stage-specific logic in layout, pagination, measurement, or render packages. |
| `com.demcha.compose.engine.pagination` | Pagination markers and helpers (`Breakable`, `ParentContainerUpdater`, `Offset`). | Maintain child-first ordering and page-shift propagation rules. |
| `com.demcha.compose.engine.measurement` | Text measurement contracts and font-backed implementations. | Builders/layout helpers depend on this seam instead of reaching into renderers. |
| `com.demcha.compose.engine.render` | Backend-neutral render contracts, handler registry, and render-pass session lifetime. | Add backend-neutral contracts here, backend-specific drawing elsewhere. |
| `com.demcha.compose.engine.render.pdf` | Shared PDFBox primitives for the canonical fixed-layout backend: `PdfFont`, `GlyphFallbackLogger`, and the header/footer + watermark renderers under `helpers`. | Add canonical-shared PDF support here; per-fragment PDF drawing lives in the `PdfFragmentRenderHandler` implementations under `document.backend.fixed.pdf.handlers`. |
| `com.demcha.compose.engine.render.word` | Experimental Word backend internals. | Treat as research until a supported public surface is designed. |
| `com.demcha.compose.engine.text` | Internal text hot-path utilities shared by layout and render stages. | Keep helpers backend-neutral and avoid public authoring concepts here. |
| `com.demcha.compose.engine.text.markdown` | Markdown token parsing used by text preparation. | Keep output backend-neutral. |

## Template Layer

| Package | Responsibility | Extension rule |
| --- | --- | --- |
| `com.demcha.compose.document.templates.api` | The `DocumentTemplate<S>` compose-first contract. | Templates compose into an open `DocumentSession`; do not add output decisions here. |
| `com.demcha.compose.document.templates.data.*` | Public template specs and domain data models. | Specs should read like domain data, not layout scripts. |
| `com.demcha.compose.document.templates.core.theme` | Family-neutral theme tokens — `BrandTheme` + `Palette` / `Typography` / `Spacing` / `Decoration`. | The single token source every family's presets read. |
| `com.demcha.compose.document.templates.core.text` | Family-neutral text rendering — `MarkdownText`, `MarkdownInline`, `RichParagraphRenderer`, `TextStyles`, `TextOrnaments`. | Markdown → DSL nodes; no family data model. |
| `com.demcha.compose.document.templates.core.identity` | Family-neutral identity — `PartyIdentity` contract + header widgets (`Headline`, `ContactLine`, `Masthead`, `Subheadline`, `SvgGlyph`) + `Contact` / `Link`. | A masthead is the same shape in every family. |
| `com.demcha.compose.document.templates.core.widgets` | Family-neutral shared widgets + decoration primitives — `CardWidget`, `TableWidget`, `TimelineAxisWidget`, `Divider`, `AccentStrip`, `Spacer`. | Keep generic (no CV-only assumptions) so any family can reuse them. |

> **Preset families.** Concrete document families live under `…templates.<family>` — `cv`, `coverletter`, `invoice`, `proposal` — each a layered stack (family data / components / widgets / presets). These per-family packages are documented by the template guides rather than enumerated here — see [which-template-system.md](../templates/which-template-system.md) for the status matrix and [templates/v2-layered/](../templates/v2-layered/README.md) for the layered architecture.


## Policy

- Public authoring starts at `GraphCompose.document(...)`, `DocumentSession`, and `DocumentDsl`.
- `engine.*` is internal foundation; it can be used by tests and low-level tooling, but it is not the recommended app authoring API.
- Fluent low-level entity builders are test-support harnesses only; production code should use semantic nodes and engine model/assembly types instead.
- Do not introduce compatibility aliases for removed package roots.
- Every production package must have `package-info.java` explaining responsibility, ownership, and extension rules.
