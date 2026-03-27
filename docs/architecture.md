# Architecture

GraphCompose is split into two practical layers.

## Engine layer: `com.demcha.compose.*`

- `loyaut_core` contains the document model, geometry, layout resolution, pagination, and rendering systems.
- `font_library` contains font registration, lookup, and PDF font helpers.
- `markdown` contains markdown-to-text-token parsing helpers used by the engine.

This layer is the reusable document engine. It is responsible for turning entities and styles into positioned render output.

## Template layer: `com.demcha.Templatese.*`

- `Templatese` contains higher-level CV and cover-letter builders, DTOs, themes, and template registries.
- These classes sit on top of the engine and package common document structures into reusable templates.

## Legacy package names

- `loyaut_core` is the current engine package name and remains in place for compatibility.
- `word_sustems` is also a legacy name and is preserved as-is for now.
- `Templatese` is the current template package name and is likewise preserved for compatibility.

These names are known typos, but package renames are deferred to a future migration so public paths do not change unexpectedly.

## Experimental areas

- The PDF backend is the main supported rendering path.
- The Word backend under `...implemented_systems.word_sustems` is experimental and should be treated as less stable than the PDF path.
