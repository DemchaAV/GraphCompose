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
| In progress | Underway; the clean part landed, a remainder is scoped. |
| Investigating | Being scoped; the approach is not yet fixed. |
| Deferred | Intentionally postponed; captured here so it is not lost. |
| Done | Shipped. |

## Engine internals

### Decompose the layout hot files

`LayoutCompiler` and `TextFlowSupport` were the two largest files in
`com.demcha.compose.document.layout`, each carrying several distinct
responsibilities. They are being split along their natural seams into focused,
individually-tested collaborators (the package-private `RowSlots` extraction is
the pattern), with layout output unchanged and covered by the snapshot suite.
Extracted so far: `CompositeDecoration` (per-page fill / overlay bands) and
`LayerStackGeometry` (z-order + align offsets) out of `LayoutCompiler`;
`InlineLayoutToken` (the inline token model) and `ParagraphWrapping` (the three
wrap loops plus tokenize / trim / indent) out of `TextFlowSupport`, which dropped
from ~1877 to ~765 LOC. The clean, side-effect-free seams are out; the remaining
`LayoutCompiler` methods are entangled with the mutable `CompilerState` and need a
stateful refactor rather than a verbatim move. **Status: In progress.**

### Extract the session's layout-resolution loop

`DocumentSession` is a delegating facade — caching, rendering, and document
chrome already live in dedicated package-private collaborators
(`DocumentLayoutCache`, `DocumentRenderingFacade`, `DocumentChromeOptions`).
The one substantive algorithm still inside the class was the coupled fixed
point in `computeLayout()`: page-reference numbers and per-page margins feed
layout results back into content and converge over up to five compile passes.
That loop was extracted into the package-private `DocumentLayoutResolver`
(following the `DocumentRenderingFacade.Context` pattern), making the convergence
loop unit-testable without opening measurement resources; the public surface is
unchanged — the session remains the single mutable entry point owning
lifecycle, authoring state, and the revision-based layout cache. A stricter
phase-pipeline restructuring (builder → immutable document → renderer) was
explicitly rejected: cross-references and tables of contents require layout
results to feed back into content, which a one-way pipeline cannot express.
**Status: Done.**

### Retire the internal entity model

Layout once resolved on a legacy object model whose coordinate, geometry and guide
helpers all hung off it, and this entry planned the rebuild that would replace it.
The 2.0 line did the work instead of deferring it: the execution layer and the
object model went together, layout now resolves through `LayoutCompiler` on the
canonical node tree, and the `graph-compose-testing` snapshot overloads that
referenced the old types went with them. No type of that model survives in any
`src/main` tree. **Status: Done.**

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

The canonical / engine / render layering and the module split were guarded only
by targeted tests plus path-based greps that can pass vacuously after a move.
ArchUnit rules now enforce the boundaries structurally at test time — the
`ModuleBoundaryArchTest` (canonical surface, including `document.api`, must not
depend on `com.demcha.compose.engine.*`; the font catalog must not depend on
`document.*`) and the qa `CrossModuleBoundaryArchTest` (templates / render-pdf
back-edges) — bytecode-level checks that cannot silently rot. **Status: Done.**

### Cross-module coverage aggregation

The canonical core packages (`document.layout` / `document.dsl` /
`document.backend.fixed`) are exercised mostly by the cross-module `qa` suites,
which depend on the engine at **test** scope — so a single-module coverage
report undercounts, and JaCoCo's `report-aggregate` does not traverse test-scope
dependencies. A small, dedicated, non-published `graph-compose-coverage` module
now compile-depends on the tested modules and runs `report-aggregate` over core +
render-pdf + templates + qa, so the core's coverage counts the qa suites that
exercise it. Report-only; thresholds can follow after a baseline read.
**Status: Done.**

### Per-module binary-compatibility baselines

`japicmp` runs report-only on the 2.0 line — the major intentionally breaks
binary compatibility. Once the 2.0 GA artifacts are published, the gate should
switch to per-module baselines pinned at the GA release and break-on-incompatible
mode, so each published module's public surface is protected from that point on.
**Status: Deferred (post-GA).**
