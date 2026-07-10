# Post-2.0 engineering roadmap

The 2.0 line focused on **packaging and internal hygiene**: splitting the
monolithic jar into per-concern modules, removing the dead legacy
Entity-Component-System code, retiring the deprecated API surface, and
tightening CI. This document tracks the **engineering** work deliberately left
for after 2.0 — internal refactors, scale work, and tooling. None of it changes
the public authoring API (`GraphCompose.document(...)` → `DocumentSession` →
`DocumentDsl`); it is about the health of the engine and the build.

## Status legend

| Status | Meaning |
| --- | --- |
| Planned | Shape agreed; not started. |
| Investigating | Being scoped; the approach is not yet fixed. |
| Deferred | Intentionally postponed; captured here so it is not lost. |

## Engine internals

### Decompose the layout hot files

`LayoutCompiler` (~1690 LOC) and `TextFlowSupport` (~1900 LOC) are the two
largest files in `com.demcha.compose.document.layout`, and each carries several
distinct responsibilities. The plan is to split them along their natural seams —
pagination, row distribution, stack / overlay placement, and decoration — into
focused, individually-tested collaborators (the package-private `RowSlots`
extraction is the pattern to follow), with layout output unchanged and covered
by the existing snapshot suite. **Status: Planned.**

### Extract the session's layout-resolution loop

`DocumentSession` is a delegating facade — caching, rendering, and document
chrome already live in dedicated package-private collaborators
(`DocumentLayoutCache`, `DocumentRenderingFacade`, `DocumentChromeOptions`).
The one substantive algorithm still inside the class is the coupled fixed
point in `computeLayout()`: page-reference numbers and per-page margins feed
layout results back into content and converge over up to five compile passes.
Extracting that loop into a package-private collaborator (following the
`DocumentRenderingFacade.Context` pattern) would make the convergence loop
unit-testable without opening measurement resources; the public surface stays
unchanged — the session remains the single mutable entry point owning
lifecycle, authoring state, and the revision-based layout cache. A stricter
phase-pipeline restructuring (builder → immutable document → renderer) is
explicitly rejected: cross-references and tables of contents require layout
results to feed back into content, which a one-way pipeline cannot express.
**Status: Planned.**

### Retire the internal `Entity` model

The engine still resolves layout on a legacy `Entity` / `EntityManager` object
model — the live layout coordinate, geometry (`EntityBounds`), and guide helpers
are built on it. The dead ECS *execution* layer around it has been removed; what
remains is genuinely live infrastructure. Fully retiring `Entity` /
`EntityManager` means rebuilding the coordinate / geometry / guide helpers on a
non-`Entity` representation and removing the legacy `engine.debug` snapshot
overloads in `graph-compose-testing` that still reference it. This is a real
engine refactor, sequenced after the module line stabilises. **Status: Deferred.**

### Fail loudly on non-converged layout

Some layout passes iterate toward a fixed point. By default a pass that does not
converge silently uses its last iteration. Setting the
`graphcompose.failOnUnconvergedLayout` system property makes the resolver throw
instead, surfacing the rare non-converging document in a test or build run rather
than shipping a subtly-wrong render. Off by default, so production output is
unchanged. **Status: Done.**

## Scale & memory

### Streaming / bounded-memory rendering

Rendering currently holds the full layout graph and all page fragments in
memory. Very large documents (thousands of pages) would benefit from a streaming
path that paginates and flushes finalised pages incrementally, bounding peak
memory. This needs a backend seam that can emit pages as they are completed
rather than at the end of the pass. **Status: Investigating.**

## Tooling & guardrails

### ArchUnit module-boundary guards

The canonical / engine / render layering and the module split are guarded today
by targeted tests plus path-based greps that can pass vacuously after a move.
ArchUnit rules would enforce the boundaries structurally at test time (for
example: no `com.demcha.compose.engine.*` import inside the canonical
`document.*` public API, and no cross-module back-edges), replacing the brittle
checks with ones that cannot silently rot. **Status: Planned.**

### Cross-module coverage aggregation

The canonical core packages (`document.layout` / `document.dsl` /
`document.backend.fixed`) are exercised mostly by the cross-module `qa` suites,
which depend on the engine at **test** scope — so a single-module coverage
report undercounts, and JaCoCo's `report-aggregate` does not traverse test-scope
dependencies. An accurate report needs a small, dedicated, non-published
aggregation module that compile-depends on the tested modules. The first step is
report-only; thresholds follow after a baseline read. **Status: Planned.**

### Per-module binary-compatibility baselines

`japicmp` runs report-only on the 2.0 line — the major intentionally breaks
binary compatibility. Once the 2.0 GA artifacts are published, the gate should
switch to per-module baselines pinned at the GA release and break-on-incompatible
mode, so each published module's public surface is protected from that point on.
**Status: Deferred (post-GA).**
