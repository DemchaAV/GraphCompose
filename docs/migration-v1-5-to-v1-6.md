# Migration: v1.5 → v1.6

v1.6 is **source-compatible with v1.5 for every documented public API**.
Every authoring call (`DocumentSession`, `DocumentDsl`, builders,
templates, `BusinessTheme`, the `FixedLayoutBackend` / `SemanticBackend`
SPIs) compiles and behaves the same way. The release sharpens the
public-vs-internal contract, opens previously-hidden extension points,
and replaces a small set of redundant or brittle API shapes with cleaner
alternatives.

If your application targets v1.5, you can bump the dependency version
to `1.6.x` and rebuild without touching authoring code. You may see
**deprecation warnings** for two specific surfaces — they document
where the API is heading; the existing methods continue to work until
their removal is announced (planned for v2.0).

## TL;DR

| Area | v1.6 change | Action |
| --- | --- | --- |
| `@Internal` annotation | New marker for implementation-detail types | Audit any imports from `com.demcha.compose.document.layout.*` |
| `DocumentRenderingException` | Rendering convenience methods no longer declare `throws Exception` | Optional cleanup: drop `throws Exception` from your `try { … } catch` blocks |
| Custom PDF render handlers | `PdfFixedLayoutBackend.Builder.addHandler(...)` is now a public extension point | Use it instead of forking the backend |
| DSL aliases | `DocumentDsl.text()` and `DocumentSession.builder()` deprecated | Rename to `paragraph()` and `dsl()` |
| PDF-typed chrome overloads | `metadata(PdfMetadataOptions)` and friends deprecated | Use the canonical `metadata(DocumentMetadata)` overloads |
| `DocumentPalette.of(...)` | Positional 7-arg factory deprecated | Use `DocumentPalette.builder()` |
| `Pdf_FontLoader` | Duplicate font loader deleted | Internal; no public callers existed |

Each section below has the details.

## Things that did NOT break

- Every entry point on `GraphCompose` (`document()`,
  `availableFonts()`, `renderAvailableFontsPreview(...)`).
- The full `DocumentSession` authoring lifecycle (compose, pageFlow,
  add, render, export, close, layoutGraph, layoutSnapshot).
- `DocumentDsl`, `BusinessTheme`, `DocumentPalette`, the template
  interfaces (`InvoiceTemplate`, `ProposalTemplate`, `CvTemplate`, …).
- `FixedLayoutBackend<R>` and `SemanticBackend<R>` two-method
  contracts.
- All existing v1.5 features (shape-as-container, transforms, z-index,
  table rowSpan/zebra/totals/repeatHeader, fluent rich text).
- Visual output, snapshot baselines, and PDF byte sequences for every
  template — the v1.5 → v1.6 perf wins (see "Performance changes")
  do not alter what gets drawn.

## Layout types are now `@Internal`

The `com.demcha.compose.document.layout` package — including
`LayoutGraph`, `PlacedFragment`, `PlacedNode`, `BoxConstraints`,
`MeasureResult`, `NodeDefinition`, `PreparedNode`, and ~15 other
records — is now annotated
[`@Internal`](../src/main/java/com/demcha/compose/document/api/Internal.java)
at the package level.

The annotation is a documentation signal, not a visibility change. Code
that imports these types continues to compile and run; IDE quick-doc
will show the `@Internal` marker. The contract is:

> Types annotated `@Internal` may change in any release without notice.
> Library users should not depend on annotated elements.

Why: the v1.6 plan reorganises the layout package internally (split
`BuiltInNodeDefinitions`, extract a `PlacementContext` interface, move
payload records to a dedicated subpackage) and we want those refactors
to ship without semver implications. See ADR
[0003](adr/0003-api-stability-and-internal-marker.md).

If you depend on a layout type today, please open an issue describing
the use case so we can design a stable replacement.

## Rendering exceptions: no more `throws Exception`

The convenience render methods on `DocumentSession` previously declared
`throws Exception`. v1.6 narrows them to `throws DocumentRenderingException`
(an unchecked exception), which means callers no longer need to declare
or catch a checked `Exception`:

```java
// v1.5
public void buildReport() throws Exception {
    try (DocumentSession session = GraphCompose.document(out).create()) {
        template.compose(session, spec);
        session.buildPdf();
    }
}

// v1.6 — same behaviour, cleaner caller signature
public void buildReport() {
    try (DocumentSession session = GraphCompose.document(out).create()) {
        template.compose(session, spec);
        session.buildPdf();
    }
}
```

The change applies to `toPdfBytes()`, `writePdf(OutputStream)`,
`buildPdf()`, `buildPdf(Path)`, and the `AutoCloseable.close()`
override.

Any underlying `IOException` from PDFBox is preserved as the
`Throwable.getCause()` on the wrapping `DocumentRenderingException`. If
your existing code does `catch (Exception e)`, it keeps working
unchanged. If your existing code does `catch (IOException e)`, the
compiler will tell you to either widen to `catch (Exception e)` or
unwrap via `e.getCause()`.

The lower-level SPI methods (`session.render(backend)`,
`session.export(backend)`) continue to declare `throws Exception` —
backend implementations may legitimately surface checked exceptions and
those signatures are part of the SPI contract.

## Custom PDF render handlers

`PdfFragmentRenderHandler` was nominally `public` in v1.5 but
documented as "package-private" with no registration path. v1.6 fixes
the contradiction:

```java
// Override how shapes are painted, e.g. to add a watermark overlay.
PdfFixedLayoutBackend backend = PdfFixedLayoutBackend.builder()
        .addHandler(new MyShapePainter())
        .metadata(canonicalMetadata)
        .build();

session.render(backend);
```

If your custom handler reports the same `payloadType()` as a built-in
default, your handler replaces the default for the resulting backend
instance and the replacement is logged at debug level. Adding two
custom handlers for the same payload type on one builder rejects the
second with `IllegalArgumentException`.

`PlacedFragment` and `PdfRenderEnvironment` remain `@Internal` — see
ADR [0004](adr/0004-pdf-handler-spi-extension.md) for the trade-off.

## Deprecations

All of the following continue to work in v1.6 and emit
`@Deprecated(since="1.6.0", forRemoval=true)` warnings. Removal is
planned for v2.0.

### `DocumentDsl.text()` and `DocumentSession.builder()`

These were name-aliases for `paragraph()` and `dsl()` respectively.
Two names for one operation pay maintenance cost without clarity.

```java
// v1.5
flow.builder().paragraph().addText("hi");
session.builder().compose(...);

// v1.6
flow.dsl().paragraph().addText("hi");
session.dsl().compose(...);
```

### PDF-typed chrome overloads on `DocumentSession`

The session exposes a backend-neutral chrome surface
(`metadata(DocumentMetadata)`, `watermark(DocumentWatermark)`,
`protect(DocumentProtection)`, `header(DocumentHeaderFooter)`,
`footer(DocumentHeaderFooter)`) and used to also accept the
PDF-specific `PdfMetadataOptions`/`PdfWatermarkOptions`/
`PdfProtectionOptions`/`PdfHeaderFooterOptions`. The PDF-typed
overloads are deprecated; the canonical overloads remain unchanged.

```java
// v1.5
session.metadata(PdfMetadataOptions.builder().title("Q1").build());
session.watermark(PdfWatermarkOptions.builder().text("DRAFT").build());

// v1.6
session.metadata(DocumentMetadata.builder().title("Q1").build());
session.watermark(DocumentWatermark.builder().text("DRAFT").build());
```

If you legitimately need the PDF-specific options because you're
talking to `PdfFixedLayoutBackend` directly, configure them on the
backend's builder
(`PdfFixedLayoutBackend.builder().metadata(PdfMetadataOptions.builder()…)`).

### `DocumentPalette.of(Color × 7)`

The 7-argument positional factory is brittle: adding or reordering
palette tokens silently shifts every downstream value, and reviewers
cannot tell argument-order mistakes from intent.

```java
// v1.5
DocumentPalette palette = DocumentPalette.of(
    new Color(28, 38, 60),     // primary navy
    new Color(40, 90, 200),    // accent blue
    Color.WHITE,               // surface
    new Color(245, 247, 250),  // muted surface
    new Color(28, 38, 60),     // text primary
    new Color(110, 120, 140),  // text muted
    new Color(210, 218, 230)); // rule

// v1.6 — same colours, names guard against reordering
DocumentPalette palette = DocumentPalette.builder()
    .primary(new Color(28, 38, 60))
    .accent(new Color(40, 90, 200))
    .surface(Color.WHITE)
    .surfaceMuted(new Color(245, 247, 250))
    .textPrimary(new Color(28, 38, 60))
    .textMuted(new Color(110, 120, 140))
    .rule(new Color(210, 218, 230))
    .build();
```

Each setter accepts either a `DocumentColor` or an AWT `Color`. If you
forget a token, `build()` throws `IllegalStateException` listing every
missing field in one message so a single fix-up resolves all gaps.

## Internal cleanup that may surface elsewhere

These changes are not part of the public surface but may show up if
your project grep'd for them:

- `com.demcha.compose.font.Pdf_FontLoader` (note the underscore) was a
  near-duplicate of `PdfFontLoader` with no callers. Deleted.
- The per-thread TrueType cache in `PdfFontLoader` is now bounded
  (LRU cap of 32 entries per thread). Long-lived servlet/worker threads
  that previously accumulated parsed TTFs forever now evict the
  least-recently-used after the cap.

## Performance changes

v1.6 ships five small render-path improvements
([commit `df8e9c4`](https://github.com/DemchaAV/GraphCompose/commit/df8e9c4)).
None of them alters output bytes; they trim allocations on hot paths:

- `LayoutCompiler.compositeDecorationFragments` /
  `compositeOverlayFragments` no longer wrap their result in
  `List.copyOf`. Two list copies per composite per page touched are
  gone.
- `LayoutCompiler.stableZIndexOrder` short-circuits when every layer
  in a `LayerStackNode` reports the same `zIndex` (the common case
  with default 0). Skips the boxed-array allocation and stable sort.
- `PdfRenderSession` keeps page surfaces in a
  `PDPageContentStream[]` indexed by page rather than a
  `LinkedHashMap<Integer, PDPageContentStream>`. Removes
  `Integer.valueOf` autoboxing on every `pageSurface(int)` call —
  visible at the multi-thousand-fragment scale (~100-page reports).
- `PdfFontLoader.THREAD_LOCAL_TTF_CACHE` is bounded (see "Internal
  cleanup" above).
- One duplicate file deleted (see same).

You should not see any output difference; benchmark numbers should
move in the same direction or be flat.

## What's coming next

v1.6 lays the groundwork for two larger refactors planned for v1.7
and v2.0:

- **v1.7**: split `BuiltInNodeDefinitions` per node type, slim
  `DocumentSession`, extract `PlacementContext` (each is internal —
  the public surface stays stable thanks to the `@Internal` marker).
- **v2.0**: migrate `engine.components.style.{Margin,Padding,…}` and
  `engine.components.content.text.{TextStyle,…}` to canonical
  `document.style.*` types; eventually delete the `engine.*` tree and
  publish a JPMS `module-info.java`.

If you have feedback on this migration, please open an issue at
<https://github.com/DemchaAV/GraphCompose/issues>.
