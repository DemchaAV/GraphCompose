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

For pagination-sensitive trees, GraphCompose relies on a child-first page-breaking order. Fixed leaf objects are resolved before their parent containers so parent `ContentSize` can reflect child shifts before container placement is finalized.

See [pagination-ordering.md](./pagination-ordering.md) for the detailed explanation of why this rule exists and how ordering bugs can look like render bugs.

## Engine layer: `com.demcha.compose.*`

- `layout_core` contains the document model, geometry, layout resolution, pagination, and rendering systems.
- `font_library` contains font registration, lookup, and PDF font helpers.
- `markdown` contains markdown-to-text-token parsing helpers used by the engine.

This layer is the reusable document engine. It is responsible for turning entities and styles into positioned render output.

## Template layer: `com.demcha.templates.*`

- `templates` contains higher-level CV and cover-letter builders, DTOs, themes, and template registries.
- These classes sit on top of the engine and package common document structures into reusable templates.
- `templates.api` contains template-facing contracts and registry/helper types.
- `templates.builtins` contains concrete template implementations.

## Current package roots

- `com.demcha.compose.layout_core.*` contains the engine internals and public builder-facing layout layer.
- `com.demcha.compose.layout_core.system.implemented_systems.word_systems.*` contains the experimental Word-specific rendering path.
- `com.demcha.templates.*` contains the higher-level template layer.

## Experimental areas

- The PDF backend is the main supported rendering path.
- The Word backend under `...implemented_systems.word_systems` is experimental and should be treated as less stable than the PDF path.

## Language status

- Java is the primary implementation language.
- The build currently includes Kotlin runtime/plugin support, but the repository does not currently ship production `.kt` sources.
- Public docs should therefore treat GraphCompose as a Java-first library with Kotlin compatibility in the build setup, not as a full dual-language codebase.

## Developer tools

- `dev-tools/` contains local developer helpers and maintenance scripts.
- Files in `dev-tools/` are not part of the runtime library API or the published Maven artifact.
