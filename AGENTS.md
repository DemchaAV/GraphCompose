# AGENTS.md

## Purpose

This file defines the operating rules for coding agents working on GraphCompose.

GraphCompose is a modular Java document layout engine.
Changes must preserve its architecture, module boundaries, public API stability,
deterministic layout behaviour, and backend independence.

Do not treat this repository as a generic Java application.

## Read Before Editing

Before making non-trivial changes, read the relevant repository documentation:

- `CONTRIBUTING.md`
- `docs/architecture/overview.md`
- `docs/architecture/package-map.md`
- `docs/contributing/extension-guide.md`

Also read when relevant:

- `docs/architecture/backend-capability-matrix.md`
  for PDF, PPTX, or DOCX capability changes
- `docs/api-stability.md`
  for public API changes
- `docs/adr/`
  for architectural decisions
- `docs/operations/benchmarks.md`
  for layout/render/performance changes
- `docs/contributing/release-process.md`
  for release-related work

Do not duplicate or replace these documents in code comments.
Treat them as the repository source of truth.

## Development Branches

Normal feature and bug-fix work targets `develop`.

- `develop` — active 2.x development
- `main` — stable release line
- `1.x` — critical 1.9.x fixes and security backports only

Do not implement normal feature work directly against `main`.

Do not run release scripts, create release tags, change release versions,
or modify release metadata unless the task explicitly requires release work.

## Java Baseline

Production code must remain compatible with Java 17.

`maven.compiler.release` in `core/pom.xml` enforces this, so a Java 21+ API
fails the build rather than reaching a user. The list below is what to watch for
while writing and reviewing, not the only line of defence:

- `List.getFirst()`
- `List.getLast()`
- `List.reversed()`
- `Thread.threadId()`
- record deconstruction patterns
- Java 21+ pattern-switch constructs

The build may run on newer JDKs, but Java 17 is the compatibility baseline.

## Architecture

The supported authoring pipeline is:

`GraphCompose.document(...)`
→ `DocumentSession`
→ `DocumentDsl`
→ semantic `DocumentNode` tree
→ `LayoutCompiler`
→ deterministic layout/pagination
→ backend rendering

Keep these responsibilities separated.

### Canonical public surface

Application-facing features belong primarily under:

- `com.demcha.compose.document.api`
- `com.demcha.compose.document.dsl`
- `com.demcha.compose.document.node`
- `com.demcha.compose.document.style`
- `com.demcha.compose.document.table`
- `com.demcha.compose.document.image`
- `com.demcha.compose.document.output`
- `com.demcha.compose.document.snapshot`

New public authoring features should normally enter through the canonical
`document.*` surface.

### Internal engine

`com.demcha.compose.engine.*` is internal infrastructure.

Do not expose engine internals as the normal application API.

Before adding a new engine primitive, determine whether it should instead be:

1. a semantic `DocumentNode`,
2. a DSL builder operation,
3. a `NodeDefinition`,
4. backend-specific rendering logic.

Do not create a second authoring model beside the canonical document surface.

## Module Boundaries

GraphCompose is intentionally multi-module.

Important modules:

- `core` — renderer-neutral engine and canonical authoring API
- `render-pdf` — PDFBox backend
- `render-pptx` — PowerPoint backend
- `render-docx` — semantic DOCX exporter
- `templates` — built-in document templates
- `testing` — reusable consumer testing support
- `wrapper` — compatibility `graph-compose` coordinate
- `bundle` — batteries-included aggregate
- `qa` — cross-module validation
- `benchmarks` — performance benchmarks

Do not move backend implementation concerns into `core`.

### Core must remain backend-neutral

Production code in the canonical authoring surface must not depend on:

- PDFBox
- Apache POI
- PPTX/XSLF types
- DOCX/XWPF types
- other backend-specific implementation classes

Backend-specific translation belongs in the appropriate renderer module.

### PDF

PDFBox lifecycle, content streams, PDF-specific rendering, annotations,
and PDF-specific conversions belong in `render-pdf`.

### PPTX

PPTX rendering belongs in `render-pptx`.

The fixed-layout PPTX backend consumes the resolved layout graph.
Do not create a separate layout algorithm merely for PPTX.

When changing PPTX capabilities, update the backend capability matrix.

### DOCX

DOCX is a semantic exporter and does not consume the fixed-layout graph.

Do not attempt to force fixed PDF geometry into the DOCX path unless an
explicit architectural change has been approved.

## Reuse Before Adding

Before introducing a new:

- class
- interface
- utility
- builder
- DTO
- abstraction
- render handler
- layout primitive
- template component

search the repository for an existing equivalent or extension point.

Prefer extending an established abstraction over creating a parallel one.

In particular, inspect:

- similar `DocumentNode` implementations
- existing DSL builders
- `NodeDefinition`
- `BuiltInNodeDefinitions`
- backend fragment handlers
- shared template widgets/components
- existing public value types

Do not duplicate functionality merely because a local implementation is easier.

## Public API Changes

Treat public APIs as compatibility-sensitive.

Prefer additive and backward-compatible changes.

When adding fields to public records or changing constructors,
preserve compatibility where the established pattern requires it.

Do not:

- rename public packages casually
- move public classes casually
- introduce compatibility aliases without architectural justification
- leak internal implementation types into public method signatures

Check `docs/api-stability.md` before modifying public API contracts.

## DSL and Node Rules

DSL builders belong in:

`com.demcha.compose.document.dsl`

DSL implementation helpers belong in:

`com.demcha.compose.document.dsl.internal`

Semantic nodes belong in:

`com.demcha.compose.document.node`

Nodes describe intent, not renderer mechanics.

Layout behaviour for a node should normally be implemented through
`NodeDefinition` and registered through the established registry mechanism.

## Templates

Templates must compose through the canonical `DocumentDsl`.

Do not import PDFBox or POI implementation classes into template code.

Reuse the shared template architecture:

- `templates.core`
- shared `BrandTheme`
- shared widgets
- shared identity/text components

Visible design values should come from theme tokens where the template system
already provides them.

Do not create family-specific copies of generic reusable widgets.

## Determinism

Layout output must remain deterministic.

Pay special attention when modifying:

- measurement
- pagination
- text shaping
- RTL/BiDi handling
- table layout
- fragment ordering
- transforms
- clipping
- fonts
- backend dispatch

Do not depend on unordered iteration where it can affect output geometry
or rendering order.

## Rendering Rules

Layout determines geometry.
Renderers draw resolved geometry.

Do not move layout decisions into renderer handlers merely to fix one backend.

An unsupported backend capability should fail or use the documented fallback
mechanism rather than silently producing incorrect output.

Renderer handlers must respect backend-owned resource and graphics-state
lifecycles.

## Testing

During implementation, run the smallest relevant tests first.

Core iteration:

`./mvnw -B -ntp verify -pl :graph-compose-core`

Before considering repository work complete, run:

`./mvnw -B -ntp clean verify`

For architecture-sensitive core changes, also run the repository guard suite
documented in `CONTRIBUTING.md`.

For documentation code snippets, run the documentation guards from the
`qa` module as described in `CONTRIBUTING.md`.

For changes affecting:

- geometry
- pagination
- ordering

prefer layout snapshot tests.

For changes affecting visual PDF output, add or update the appropriate
visual regression/render smoke coverage.

For performance-sensitive changes, follow
`docs/operations/benchmarks.md`.

Do not weaken, delete, or bypass an existing regression test merely to make a
change pass unless the expected behaviour itself intentionally changed.

### Running the examples after a render change

The full reactor builds `examples` from source, but a standalone invocation does
not: `cd examples && ../mvnw compile exec:java -Dexec.mainClass=...` resolves its
`graph-compose-*` dependencies from `~/.m2`, the same way a standalone
`-f qa/pom.xml` run does. So after changing a render backend, install first:

`./mvnw -B -ntp -DskipTests install`

Without it the examples regenerate through the artifacts you last installed, and
the output looks unchanged because the change was never in it.

### Example output is not byte-comparable

Regenerating an example twice in a row produces different bytes — the writers
embed timestamps. Comparing a fresh render against the committed preview under
`assets/readme/` therefore reports a difference for every example, including
ones your change cannot reach, so a byte comparison answers no question worth
asking.

To decide whether a render change owes regenerated previews, establish the
changed code path's trigger condition and search the examples for documents that
meet it. The visual-regression baselines in the `qa` module are the independent
check.

## Working Tree Hygiene

Stage explicit paths — `git add <path> …`. Never `git add .` or `git add -A`.

The working tree accumulates zero-byte files with names like `$env`, `b)` or
`'Current`, left behind by shell quoting accidents rather than by anyone's
intent. They are invisible in a summary and permanent once committed. `git add`
by path cannot pick them up.

Before committing, read `git status --porcelain --untracked-files=all` and
remove such files by name. Do not run `git clean -f`: it also deletes new source
files that have not been staged yet.

## Documentation

When changing public behaviour, update affected documentation in the same change.

Keep these synchronized when relevant:

- README examples
- architecture documentation
- package map
- extension guide
- capability matrix
- runnable examples
- screenshots
- migration documentation

Examples must use the current canonical public API.

Do not document an API that does not compile.

Prose that describes how the engine works is checked by guards in
`core/src/test/java/com/demcha/documentation/`. A description that has stopped
being true fails the build there, so correct the sentence rather than the guard.

## Code Quality

Follow existing Java naming and formatting conventions.

Prefer:

- small focused changes
- explicit names
- immutable values where appropriate
- existing abstractions
- single-purpose classes
- clear module ownership

Avoid:

- unrelated refactoring
- speculative abstractions
- duplicated helpers
- hidden coupling
- backend leakage
- unnecessary dependencies
- clever code that makes layout behaviour harder to reason about

Keep refactoring separate from behavioural changes when practical.

## Dependencies

Do not add a dependency until checking whether the repository already provides
the required functionality.

New runtime dependencies require particular care because `graph-compose-core`
is intentionally lean.

Do not add PDFBox, POI, or other backend libraries to `core`.

## POM and Versioning Rules

The repository root `pom.xml` is a reactor aggregator, not the published engine.

`core/pom.xml` owns `graph-compose-core`.

Do not convert the root aggregator into a shared Maven parent without an
explicit architectural decision.

Engine modules follow the repository's lockstep release model.
Fonts and emoji have their own version lines.

Do not manually change a subset of lockstep module versions.

## Before Finishing

Before reporting completion:

1. inspect the final diff;
2. verify no unrelated files changed;
3. confirm module boundaries were preserved;
4. confirm public API compatibility was considered;
5. run the relevant focused tests;
6. run `./mvnw -B -ntp clean verify` when feasible;
7. report any test or validation command that could not be run.

Never claim a test passed unless it was actually executed successfully.
