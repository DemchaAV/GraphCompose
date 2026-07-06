# ADR 0016 — Multi-module packaging (lean core + render backends + templates)

- **Status:** Accepted
- **Date:** 2026-07-06
- **Authors:** Artem Demchyshyn

## Context

Through the 1.x line GraphCompose shipped as a single published jar,
`io.github.demchaav:graph-compose`. That one artifact bundled the engine, the
PDFBox render backend (plus PDFBox and zxing), the Apache POI semantic backend,
and the built-in template families. A consumer who only wanted the engine — or
who intended to bring their own render backend — still pulled the entire set,
and an engine patch republished everything.

The [API-stability ledger](../api-stability.md#3-deprecation-window) tracked this
as a **Stable** packaging commitment we intended to change in the next major: the
single-jar shape could not be split in a minor without breaking every existing
coordinate.

Two forces shaped the split:

- **Consumer-side leanness is the benefit, not release-side flexibility.** The
  goal is a small dependency tree for engine-only / bring-your-own-backend
  consumers. Versioning stays **lockstep** — a patch republishes the whole train
  — so the split never promises independent per-module releases.
- **PDF back-compatibility is non-negotiable.** Every existing caller depends on
  `graph-compose` and calls `…buildPdf()`. Upgrading to 2.0 must not make a bare
  `graph-compose` stop rendering PDF.

## Decision

Split the single jar into per-concern Maven modules, versioned in lockstep, with
render backends discovered at runtime through a `ServiceLoader` SPI.

**Coordinates:**

- **`graph-compose-core`** — the lean engine: the `DocumentSession` authoring API,
  the canonical DSL / nodes / style / layout, chart / svg / markdown / barcode, and
  the `FixedLayoutBackendProvider` / `FontMetricsProvider` SPI seams. Depends only on
  `slf4j-api` + `flexmark`. Rendering nothing until a backend is on the classpath —
  it throws `MissingBackendException` (naming the artifact to add) if asked to build a
  PDF without one.
- **`graph-compose-render-pdf`** — the entire PDFBox backend (`document.backend.fixed.pdf.**`
  and the `engine.render.pdf.**` tree), PDFBox, and zxing. Registers the PDF
  `FixedLayoutBackendProvider` / `FontMetricsProvider` via `META-INF/services`.
- **`graph-compose`** — kept as a coordinate, but now an **empty jar-wrapper** over
  `graph-compose-core` + `graph-compose-render-pdf`. So a 1.x caller who upgrades keeps
  rendering PDF with no code and no dependency change. Packaging stays `jar` (not `pom`)
  so existing `<dependency>` declarations need no `<type>`.
- **`graph-compose-render-docx`** / **`graph-compose-render-pptx`** — the POI semantic
  backends, split apart so a DOCX consumer does not pull the PPTX skeleton (and neither
  pulls into the engine).
- **`graph-compose-templates`** — the built-in CV / cover-letter / invoice / proposal
  presets. Pure authoring over the canonical DSL, so it depends only on
  `graph-compose-core`. **Opt-in:** the `graph-compose` wrapper does not bundle it.
- **`graph-compose-testing`** — the consumer testing support (`LayoutSnapshotAssertions`,
  `PdfVisualRegression`).
- **`graph-compose-bundle`** — the batteries-included aggregate: the default PDF stack
  (via the wrapper) + templates + the independently-versioned `graph-compose-fonts` and
  `graph-compose-emoji` companions. Office backends are not bundled.

The engine sources stay at the repository root and only the **root coordinate** is
renamed `graph-compose` → `graph-compose-core`; the new wrapper is a small module. The
default coordinate `graph-compose` therefore continues to mean "PDF out of the box".

No JPMS: modules carry an `Automatic-Module-Name` only, so a split package across
test-scope jars stays legal on the classpath.

## Consequences

- **Engine-only / bring-your-own-backend consumers get a lean tree** — depend on
  `graph-compose-core` and pull neither PDFBox, POI, zxing, nor the templates.
- **Existing PDF callers are unaffected** — `graph-compose` still renders PDF out of the
  box; no code change on upgrade.
- **Templates-via-`graph-compose` consumers break** — a caller that reached a preset
  through the 1.x single jar must now add `graph-compose-templates` (or
  `graph-compose-bundle`). This is the one accepted source break; PDF compatibility took
  priority. See the [modules migration guide](../migration/v2.0.0-modules.md).
- **Lockstep versioning, not independent releases** — every 2.0 coordinate shares the
  engine version; a patch republishes the train. `graph-compose-fonts` /
  `graph-compose-emoji` keep their own lines (they change rarely).
- **A `MissingBackendException` is now the "why won't it render" signal** for a lean
  `graph-compose-core`, replacing the pre-split assumption that the backend is always present.
- The legacy `Entity`-Component-System render pipeline moves into `graph-compose-render-pdf`
  rather than being deleted; it is dead on the canonical path but still exercised by
  test scaffolding, so its removal is deferred beyond 2.0.
