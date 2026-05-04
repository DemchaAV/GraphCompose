# ADR 0003 — API stability boundary and the `@Internal` marker

- **Status:** Accepted
- **Date:** 2026-05-04
- **Authors:** Artem Demchyshyn

## Context

Through v1.5 the canonical surface declared a clean separation between
"public" and "internal" packages, but every type in the
`com.demcha.compose.document.layout.*` package was technically `public`
with no visibility marker beyond the Java keyword. Library users could
reach `LayoutGraph`, `PlacedFragment`, `PreparedNode`, `MeasureResult`,
`NodeDefinition`, `BoxConstraints`, and ~15 other layout records simply
by calling `session.layoutGraph()`.

The post-v1.5 architecture audit flagged this as a high-severity
maintainability risk:

- Users who write regression tests against snapshot output occasionally
  bind to `LayoutGraph` or `PlacedFragment` directly. Renaming or
  refactoring those types — which the v1.6 plan does in phases E.1 / E.2
  / E.4 — would silently become a breaking API change.
- `BuiltInNodeDefinitions` exposes ~10 inner payload records as `public`
  (`ShapeFragmentPayload`, `TableRowFragmentPayload`,
  `ParagraphSpan` and friends, `PreparedStackLayout`, `SideBorders`,
  `PdfSemanticFragmentPayload`). These are implicit public commitments
  with no signal that their schema can change.
- Java does not provide `internal` visibility outside JPMS, and we are
  not yet ready to ship a `module-info.java` (deferred to v2.0).

The audit recommended adding an explicit `@Internal` annotation marker
plus runtime-visible coverage so guard tests can keep us honest as the
public surface grows.

## Decision

Introduce
[`com.demcha.compose.document.api.Internal`](../../src/main/java/com/demcha/compose/document/api/Internal.java) —
a `@Documented`, `@Retention(RUNTIME)` annotation with targets
`{TYPE, METHOD, FIELD, CONSTRUCTOR, PACKAGE}`.

Apply it transitively at the package level for the layout package and
individually on the most-touched payload records inside
`BuiltInNodeDefinitions`. The annotation does not change visibility — it
documents the contract:

> Types annotated `@Internal` may change in any release without notice.
> Library users should not depend on annotated elements.

Runtime retention is intentional: architecture-guard tests use
reflection (`Class.getPackage().getAnnotation(Internal.class)`) to
enforce coverage without scanning source files.

The annotation lives under `document.api` rather than under a separate
`api.support` package because the public surface and the contract that
modulates it belong together — IDE autocomplete surfaces `Internal`
right next to `DocumentSession`, so it is discoverable.

## Coverage

- [`document.layout.package-info.java`](../../src/main/java/com/demcha/compose/document/layout/package-info.java)
  carries `@Internal` at the package level. Every type in the package
  inherits the marker through `Class.getPackage()`.
- 10 `public` payload records inside `BuiltInNodeDefinitions` (the
  outer-class payloads visible from rendering and snapshot tests)
  carry an explicit `@Internal` annotation in addition to the package
  marker, so the contract survives if those records later move to
  `document.layout.payloads.*` (planned in Phase E.2).
- [`InternalAnnotationCoverageTest`](../../src/test/java/com/demcha/documentation/InternalAnnotationCoverageTest.java)
  asserts the package marker is in place and propagates to a
  representative cross-section of layout types
  (`BoxConstraints`, `MeasureResult`, `NodeDefinition`,
  `PlacedFragment`, `LayoutGraph`).
- [`InternalAnnotationDocumentationTest`](../../src/test/java/com/demcha/compose/document/api/InternalAnnotationDocumentationTest.java)
  pins the annotation's retention, target set, `@Documented`-ness,
  and the source-level Javadoc contract (the phrase
  *"may change in any release without notice"* and the link to the
  issue tracker).

## Consequences

- **For users:** any code that imports a layout type now produces an
  IDE-visible warning (the `@Documented` `@Internal` marker shows up
  in quick-doc and Javadoc). Existing call sites continue to compile
  and run; the annotation is a documentation signal, not a hard break.
  Migration guidance is published in
  [`docs/migration-v1-5-to-v1-6.md`](../migration-v1-5-to-v1-6.md).
- **For us:** Phase E.1 / E.2 (split `BuiltInNodeDefinitions`,
  relocate payload records) and Phase E.4 (extract `PlacementContext`)
  can now ship as internal refactors without semver implications. If a
  user files an issue requesting access to a layout type, we have a
  forcing function — promote the type out of `@Internal`, design a
  stable wrapper, or close the request explicitly.
- **For architecture guards:** `InternalAnnotationCoverageTest` adds
  a runtime check that the layout package stays marked. Removing the
  package-level annotation accidentally during a refactor will fail
  the build.
- **Trade-off accepted:** users running snapshot diff tests against
  `session.layoutGraph()` may need to migrate to higher-level
  `session.compose(...)` assertions or wait for a stable
  layout-introspection API in a later release.

## Alternatives considered

1. **Move types to a `*.internal` package**: Java does not provide
   visibility scoped to a package suffix, and any rename would force a
   public-API break right now. Deferred to v2.0 alongside JPMS.
2. **Use a third-party annotation** (e.g. JetBrains' `@ApiStatus.Internal`
   or Google's `@Beta`): adds a runtime dependency on an annotations
   jar for a single purpose. The local annotation has zero dependency
   cost and the same semantics for our guard tests.
3. **Mark every layout type `final` and seal them**: helps with
   subclassing-by-accident, not with the import-by-accident problem.
   Sealing is orthogonal and may still happen in Phase E.

## Related

- ADR 0004 — PDF fragment render handler SPI is public (companion
  decision: opens an extension point that previously read as internal).
- v1.6 execution plan (private), Phase A.1 / A.2 / A.3.
- Audit findings H4 (no stability annotations), C3 (god class +
  payload records exposed).
