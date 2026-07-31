# GraphCompose Render — PPTX

`io.github.demchaav:graph-compose-render-pptx`

The fixed-layout PowerPoint render backend for GraphCompose. The same
`DocumentSession` that prints a PDF also emits an editable `.pptx` deck — one
resolved page becomes one identically-sized slide, and every fragment lands at
the same coordinates, because both backends consume the same `LayoutGraph`
compiled in core before either of them runs.

## Status

**Experimental (`@Beta`), first shipped in 2.1.0.** The backend is complete
enough for production decks: text, tables, shapes, images, gradients, hyperlinks
and slide jumps, headers/footers/watermarks, and multi-section documents all
arrive as native PowerPoint objects rather than pictures.

Across the 38 capabilities the matrix tracks, 24 map to a native equivalent and 4
are not supported at all. The remaining 10 are partial, and not all in the same
way. Some render natively with an approximated styling detail — distinct
per-corner radii collapse to one value, numeric dash arrays map to the nearest
preset, a radial gradient uses the closest DrawingML shade. Others lose something
the format cannot carry: bookmarks beyond the first on a slide, parts of an inline
SVG, the metadata producer field. Both kinds are listed under limitations below.

The `@Beta` marker covers the *API shape* (`PptxFixedLayoutBackend`, its builder,
and the `PptxFragmentRenderHandler` seam), which may still change in a minor
release while feedback lands. Geometry identity with the PDF backend is a design
invariant and is not subject to change.

The per-capability breakdown lives in one place:
[backend capability matrix](../docs/architecture/backend-capability-matrix.md).
The stability policy is [docs/api-stability.md](../docs/api-stability.md).

## First deck

Put `graph-compose-core` and this artifact on the classpath; the backend
registers itself through `ServiceLoader`, so no wiring is needed.

<!-- doc-example-ignore: compose(...) and the font family are the reader's own code, named here as placeholders -->
```java
try (DocumentSession document = GraphCompose.document()
        .pageSize(DocumentPageSize.SLIDE_16_9)
        .create()) {
    compose(document);
    document.buildPptx(Path.of("deck.pptx"));
}
```

`writePptx(OutputStream)` and `toPptxBytes()` are the streaming and in-memory
counterparts. Rendering the *same* session to both formats gives a PDF and a
deck with matching page/slide geometry.

`DocumentPageSize.SLIDE_16_9` (960 × 540 pt) and `DocumentPageSize.SLIDE_4_3`
(720 × 540 pt) match the PowerPoint defaults. Any other page size works too —
the slide simply takes that size.

## Limitations to know before you ship

- **Clipping falls back to a raster island.** A clipped composite is re-rendered
  through the PDF backend and placed as a picture, so that region loses text
  editability and any run-level links inside it. It is pixel-accurate — the raster
  targets a 2048-pixel long edge, clamped between native size and 4×, so a region
  larger than that is rendered at native resolution rather than downscaled, at a
  transient memory cost proportional to its size. Switch it off with
  `PptxFixedLayoutBackend.builder().clipRasterFallback(false)`.
  True vector clipping is tracked in
  [#413](https://github.com/DemchaAV/GraphCompose/issues/413).
- **Glyphs are drawn by the viewer, not by GraphCompose.** Shape frames, line
  boxes and positions are fixed by the layout graph, but the actual glyph
  rasterisation depends on the fonts available to the viewing machine. See
  [Getting glyph-identical decks](#getting-glyph-identical-decks) for what that
  costs and how to avoid it.
- **Byte-for-byte reproducibility is opt-in.** The default path streams the deck
  with live timestamps. Pass
  `PptxFixedLayoutBackend.builder().deterministic(instant)` to pin the core
  properties and normalise the zip entries.
- **PDF-only options do not apply**: document protection, viewer preferences and
  the debug guide-line overlays are PDF concepts and are ignored here, each with
  a one-time warning.
- **`renderToImages` is not implemented** and throws with a pointer to the PDF
  backend — POI's slide rasteriser cannot honour embedded fonts. The session-level
  `toImage(...)` / `toImages(...)` are unaffected: they resolve the PDF backend
  explicitly.
- **Bookmarks do not survive as a tree.** PPTX has no outline concept, so the
  first bookmark on a page names its slide and further bookmarks on that page are
  dropped with a debug note. A bookmark-heavy document loses most of its
  navigation structure; hyperlinks and internal slide jumps are unaffected.
- **Inline SVG is native only for simple layers.** Arbitrary clips, exact
  dash/cap/join styles and off-viewBox art fall back to a transparent PNG, and
  gradient paints use their primary colour.
- **Document metadata loses its producer value** — OPC core properties have no
  equivalent field.
- **Multi-section documents** render into one deck; the sections must share a
  slide size.

## Getting glyph-identical decks

Geometry is identical to the PDF by construction — both backends consume the
same resolved layout, and every line is pre-wrapped and absolutely placed, so
PowerPoint can never reflow content. **Glyphs are the one thing PPTX cannot
guarantee on its own**, because a deck names its fonts and the viewer draws
them. Which of the three cases you land in depends on how the font reached the
document, and the backend warns once per family for the two that are not exact:

| How the font reached the document | In the deck | Glyphs |
|---|---|---|
| A standard-14 name (`FontName.HELVETICA`, `TIMES`, `COURIER`) | referenced as its metric-compatible viewer font — Arial, Times New Roman, Courier New | **widths identical, letterforms differ** |
| A binary family whose licensing bits allow embedding | embedded in the deck | **identical everywhere** |
| A family registered without binary sources, or one whose bits refuse embedding | referenced by name only | identical **if** the viewer has that font installed, otherwise the viewer substitutes |

So for a deck that matches the PDF glyph for glyph, use a real font program
rather than a PDF built-in — either a bundled family from `graph-compose-fonts`,
or your own file registered on the session:

<!-- doc-example-ignore: compose(...) and the font family are the reader's own code, named here as placeholders -->
```java
try (DocumentSession document = GraphCompose.document()
        .pageSize(DocumentPageSize.SLIDE_16_9)
        .create()) {
    document.registerFontFamily(myFamily);   // embedded when licensing allows
    compose(document);
    document.buildPptx(Path.of("deck.pptx"));
}
```

Every substitution is announced at `WARN` under the key
`render.pptx.font.substitution`, once per family per render, naming the font the
document asked for and what the deck will reference instead. Embedded families
log nothing — silence means the deck carries its own glyphs.

## When to depend on it

Opt-in, at compile scope, when you want PowerPoint output. It is not pulled in
by `graph-compose`, `graph-compose-core`, or `graph-compose-bundle`.

Note that this artifact depends on `graph-compose-render-pdf` at compile scope:
the PDF backend supplies the shared font-measurement library (so glyph widths
stay aligned with the widths that produced the layout graph) and performs the
raster pass behind the clipping fallback. A PPTX-only consumer therefore also
resolves the PDF stack. Extracting a backend-neutral measurement artifact is
tracked on the roadmap.

## Install

Same version as the rest of the GraphCompose train (lockstep):

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose-render-pptx</artifactId>
    <version>2.1.0</version>
</dependency>
```

```kotlin
dependencies { implementation("io.github.demchaav:graph-compose-render-pptx:2.1.0") }
```

The full "which artifact?" table: [root README → Installation](../README.md#installation).
Upgrading from 1.x: [modules migration guide](../docs/migration/v2.0.0-modules.md).

---

<sub>The module also carries `PptxSemanticBackend`, a slide-safe semantic
manifest that validates a node graph against what a slide surface can represent.
It predates the fixed-layout backend, is outside the scope of the capability
matrix, and is not what `buildPptx(...)` uses.</sub>
