# Package Map

This document is the source of truth for production package ownership in the canonical engine branch.

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
| `com.demcha.compose.document.layout` | Semantic layout compiler, node definitions, fragments, split/measure contracts, and layout graph. | New node behavior must be deterministic and covered by layout or render tests. |
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
| `com.demcha.compose.engine.layout` | Low-level entity layout systems. | Preserve deterministic traversal and container sizing semantics. |
| `com.demcha.compose.engine.layout.container` | Container alignment, expansion, and module width helpers. | Helpers should operate on existing ECS state and avoid renderer dependencies. |
| `com.demcha.compose.engine.pagination` | Page-breaking helpers and pagination fallback systems. | Maintain child-first ordering and page-shift propagation rules. |
| `com.demcha.compose.engine.measurement` | Text measurement contracts and font-backed implementations. | Builders/layout helpers depend on this seam instead of reaching into renderers. |
| `com.demcha.compose.engine.render` | Backend-neutral render contracts, handler registry, render ordering, and render-pass session lifetime. | Add backend-neutral contracts here, backend-specific drawing elsewhere. |
| `com.demcha.compose.engine.render.pdf` | Low-level PDF backend internals for ECS rendering. | PDFBox state stays here and in child handler/helper packages. |
| `com.demcha.compose.engine.render.word` | Experimental Word backend internals. | Treat as research until a supported public surface is designed. |
| `com.demcha.compose.engine.text` | Internal text hot-path utilities shared by layout and render stages. | Keep helpers backend-neutral and avoid public authoring concepts here. |
| `com.demcha.compose.engine.text.markdown` | Markdown token parsing used by text preparation. | Keep output backend-neutral. |

## Template Layer

| Package | Responsibility | Extension rule |
| --- | --- | --- |
| `com.demcha.compose.document.templates.api` | Public template interfaces and registries. | Interfaces compose into `DocumentSession`; do not add legacy composer overloads. |
| `com.demcha.compose.document.templates.builtins` | Thin built-in template facades. | Facades choose the composer/theme and emit lifecycle logs; composition logic belongs in support packages. |
| `com.demcha.compose.document.templates.data.*` | Public template specs and domain data models. | Specs should read like domain data, not layout scripts. |
| `com.demcha.compose.document.templates.support.common` | Shared template composition primitives, module specs, link helpers, layout policy, and lifecycle logging. | Keep reusable and backend-neutral. |
| `com.demcha.compose.document.templates.support.cv` | CV-specific scene composers. | CV layout rules belong here, not in generic business helpers. |
| `com.demcha.compose.document.templates.support.business` | Invoice, proposal, and cover-letter scene composers and policies. | Use shared module/render paths and template layout policy. |
| `com.demcha.compose.document.templates.support.schedule` | Schedule-specific scene composer. | Keep schedule-specific table/rhythm decisions isolated here. |
| `com.demcha.compose.document.templates.theme` | Theme value objects for built-ins. | Themes should carry styling decisions, not document content. |

## Policy

- Public authoring starts at `GraphCompose.document(...)`, `DocumentSession`, and `DocumentDsl`.
- `engine.*` is internal foundation; it can be used by tests and low-level tooling, but it is not the recommended app authoring API.
- Fluent low-level entity builders are test-support harnesses only; production code should use semantic nodes and engine model/assembly types instead.
- Do not introduce compatibility aliases for removed package roots.
- Every production package must have `package-info.java` explaining responsibility, ownership, and extension rules.
