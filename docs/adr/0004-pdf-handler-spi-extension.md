# ADR 0004 — PDF fragment render handler SPI is public

- **Status:** Accepted
- **Date:** 2026-05-04
- **Authors:** Artem Demchyshyn

## Context

The canonical PDF backend dispatches each resolved fragment to a
payload-specific painter via `PdfFragmentRenderHandler<T>`. Through v1.5
this contract carried a contradiction: the type modifier was
`public interface PdfFragmentRenderHandler<T>`, but its Javadoc opened
with the line *"Package-private PDF fragment painter contract"* and the
backend's handler registration path
(`PdfFixedLayoutBackend.defaultHandlers()`) was `private static`.

The post-v1.5 audit flagged this as the project's biggest extensibility
cliff:

- `DocumentSession.registerNodeDefinition(...)` lets a user register a
  new layout `NodeDefinition`, which is good for adding new layout
  primitives.
- But if that `NodeDefinition` emits a fragment payload the backend
  has no built-in handler for, the canonical PDF backend cannot render
  it — and the user has no public method to install a handler.
- Adding a handler required either forking `PdfFixedLayoutBackend` or
  putting a custom handler class inside the backend's package by
  abusing Java's package-private rules.

The Javadoc and the modifier contradicted each other; the registration
path was inaccessible. The empirical evidence: when v1.5 introduced
`ShapeContainerNode`, the author had to put
`PdfShapeClipBeginRenderHandler` and `PdfShapeClipEndRenderHandler`
inside the same package as the default handlers because there was no
external way to install them.

## Decision

Promote `PdfFragmentRenderHandler` to a real, fully public extension
point and add a registration path on the backend's existing builder.

Concretely:

1. The `PdfFragmentRenderHandler` interface stays `public`, but its
   Javadoc is rewritten to declare it a public extension point with
   stateless invariant. The "package-private" wording is gone.
2. A new method
   [`PdfFixedLayoutBackend.Builder#addHandler(PdfFragmentRenderHandler<?>)`](../../src/main/java/com/demcha/compose/document/backend/fixed/pdf/PdfFixedLayoutBackend.java)
   accepts a custom handler. It is additive: omitting it reproduces the
   existing default-handler set. Calling it with a payload type already
   covered by a default replaces the default for the resulting backend
   instance and emits a debug-level log entry.
3. Calling `addHandler(...)` twice with two custom handlers for the
   same payload type rejects the second registration with an
   `IllegalArgumentException`. There is exactly one handler per
   payload type per backend instance — the merging happens at
   `Builder.build()` so the underlying constructor's "no duplicates"
   contract still holds.
4. A new private static `mergeHandlers(defaults, additions)` helper
   performs the merge. It is intentionally not a public utility —
   users always go through the builder.

## Coverage

- [`PdfFragmentRenderHandler`](../../src/main/java/com/demcha/compose/document/backend/fixed/pdf/PdfFragmentRenderHandler.java)
  Javadoc names `Builder.addHandler(...)` as the registration path and
  states the stateless invariant explicitly.
- [`PdfFixedLayoutBackend.Builder#addHandler`](../../src/main/java/com/demcha/compose/document/backend/fixed/pdf/PdfFixedLayoutBackend.java)
  is the new entry point.
- [`PdfBackendExtensibilityTest`](../../src/test/java/com/demcha/compose/document/backend/fixed/pdf/PdfBackendExtensibilityTest.java)
  pins three behaviours:
  - A custom handler whose `payloadType()` matches a built-in default
    replaces the default for that backend (verified via call counter
    on a doc with a page background that emits `ShapeFragmentPayload`).
  - Adding a second custom handler for the same payload type on one
    builder rejects with a named-payload-type message.
  - Building without `addHandler(...)` still produces a functional
    backend equivalent to the previous default behaviour.
- The `document/backend/fixed/pdf/package-info.java` Javadoc carries
  an "Extension Points" section that documents this as the canonical
  way to add or override fragment painters.

## Consequences

- **For users:** custom rendering scenarios — overriding how shapes
  draw, adding a watermark overlay, supporting a new payload emitted
  by a custom `NodeDefinition` — are now expressible without forking
  the backend or using package-private hacks.
- **`PlacedFragment` and the layout types remain `@Internal`** (see
  ADR 0003). Custom handlers receive `PlacedFragment` and
  `PdfRenderEnvironment` references, both of which are documented
  internal. Users who write custom handlers accept the same
  may-change-without-notice contract that any internal-API consumer
  does. This trade-off is accepted: the alternative — promoting all
  layout types to stable public — would freeze too much of the engine
  too early.
- **Performance:** `mergeHandlers` runs at most once per
  `Builder.build()`, allocates one `LinkedHashMap` plus one `List.copyOf`
  result, and is bypassed entirely when no custom handlers are
  registered (`additions.isEmpty()` short-circuit). Existing rendering
  behaviour is byte-identical when nothing is customised.
- **Future evolution:** if the layout types ever stabilise (Phase E.4
  introduces `PlacementContext` and may pave the way), the handler SPI
  surface naturally grows with them. Users do not need to migrate.

## Alternatives considered

1. **Provide an end-to-end "Custom PDF backend" SPI** that accepts the
   `LayoutGraph` and produces bytes. This already exists
   (`FixedLayoutBackend<R>`); the audit confirmed it works, but it
   forces backend authors to reimplement page lifecycle, font cache,
   image cache, and chrome post-processing. Most users want to add or
   override one handler, not build a backend from scratch.
2. **Make handlers register themselves through `ServiceLoader`**.
   Implicit registration is hard to debug — the user does not see
   *which* handler is responsible for a fragment without studying the
   classpath. Explicit `Builder.addHandler(...)` keeps the registration
   ownership obvious and reviewable.
3. **Expose the internal handler map directly**. Reading or mutating a
   `Map<Class<?>, PdfFragmentRenderHandler<?>>` after the backend is
   built would compromise the immutability guarantee documented on
   `PdfFixedLayoutBackend`. The builder boundary is the right seam.

## Related

- ADR 0003 — API stability boundary and the `@Internal` marker
  (companion decision: this ADR opens an extension point while ADR
  0003 keeps adjacent types tagged internal).
- v1.6 execution plan (private), Phase A.5.
- Audit findings C1 (PDF handler SPI cliff).
