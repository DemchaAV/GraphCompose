# Recipes

GraphCompose recipes are split into focused pages so each page covers
one topic end-to-end. All recipes use only the canonical session-first
authoring API; public application code should not import
`com.demcha.compose.engine.*`.

## Topic-focused recipe pages

| Page | Covers |
| --- | --- |
| [Charts](recipes/charts.md) | Native vector bar / line / area / pie-donut charts: data–spec–style layers, axis & grid toggles, point markers, value-label halos, legend placement, translucent area fills |
| [Keep-together pagination](recipes/keep-together.md) | `keepTogether()` / `keepEntriesTogether()` — blocks that relocate whole instead of orphaning a heading at a page break |
| [Themes](recipes/themes.md) | `BrandTheme` token bundle (palette / typography / spacing / decoration), theme factories per family, page background, direct DSL styling |
| [Shapes and visual primitives](recipes/shapes.md) | Filled cards, dividers, spacers, lines, ellipses, image fit modes, soft panels |
| [Shape-as-container](recipes/shape-as-container.md) | `addCircle` / `addEllipse` / `addContainer` with `ClipPolicy` (clipped layered children) |
| [Transforms and z-index](recipes/transforms.md) | `rotate` / `scale` mixin, per-layer `zIndex` for overlays |
| [Page backgrounds](recipes/page-backgrounds.md) | `pageBackground` / `pageBackgrounds`, `PageBackgroundFill` columns, bands, point-based fills, layering |
| [Multi-column flow](recipes/multi-column-flow.md) | `addColumnFlow(...)` — columns side by side, each continuing on the next page (a row is one atomic band) |
| [Layered page design](recipes/layered-page-design.md) | Page background vs. row vs. layer stack vs. canvas — choosing the layer |
| [Absolute placement](recipes/absolute-placement.md) | `addCanvas` + `position(x, y)` for pixel-precise certificates and badges |
| [Tables](recipes/tables.md) | Row span, zebra rows, totals row, repeated header on page break |
| [Text direction](recipes/text-direction.md) | `TextDirection` — right-to-left paragraphs, `AUTO` resolved from the text, mixed lines, and the bundled Hebrew / Arabic families |
| [Rich text](recipes/rich-text.md) | `RichText` mixed-style runs in one paragraph: bold/accent/styled segments, inline links, inline images, inline SVG icons, emoji shortcodes, inline shapes and checkboxes |
| [Lists](recipes/lists.md) | `addList`: quick bulleted lists, marker customisation, nested lists with per-depth markers, spacing and styled items |
| [Timelines](recipes/timelines.md) | `addTimeline`: markers (dot / circle / numbered / square) on a connector rail, geometry and text-style controls, pagination opt-ins |
| [Barcodes](recipes/barcodes.md) | QR / Code 128 / Code 39 / EAN / UPC / PDF417 / DataMatrix, tinting, quiet zone, card centring |
| [Images](recipes/images.md) | Sources (bytes/path), sizing precedence, STRETCH/CONTAIN/COVER fit modes, images in rows and cards |
| [PDF chrome](recipes/pdf-chrome.md) | Metadata, watermarks, running header/footer with `{page}/{pages}/{date}`, protection, links and outline bookmarks |
| [In-PDF navigation](recipes/in-pdf-navigation.md) | Named `anchor(...)` destinations + internal `linkTo(...)` links: clickable tables of contents, `#heading`-style jumps, bidirectional footnotes, inline-graphic links — native PDF GoTo actions |
| [Translucency](recipes/translucency.md) | `DocumentColor.rgba` / `withOpacity`: which primitives honour alpha, byte-identity for opaque colours, layered tints |
| [DOCX export](recipes/docx-export.md) | Semantic DOCX export: 1:1 node mapping, chart/shape-container fallbacks, skipped kinds |
| [Snapshot testing](recipes/snapshot-testing.md) | Layout-snapshot regression testing in consumer projects, baseline update flow |
| [Streaming and output](recipes/streaming.md) | `buildPdf` / `writePdf` / `toPdfBytes`, DOCX export, layout snapshots, header / footer chrome, guide lines |
| [Extending GraphCompose](recipes/extending.md) | New semantic node, fluent setter, render backend, snapshot-based regression tests |

For longer-form material:

- [Extension guide](contributing/extension-guide.md) — walkthrough of the four
  extension paths, with `ShapeContainerNode` as the worked example.
- [Font coverage and glyph fallback](font-coverage.md) — WinAnsi limits,
  `●` vs `•`, and the inline-shape / bundled-font alternatives.
- [`ADR 0001 — Shape as container`](adr/0001-shape-as-container.md)
  and [`ADR 0002 — Theme unification`](adr/0002-theme-unification.md)
  for the design reasoning behind shape containers and the unified theme model.

## Common DSL primitives — quick snippets

The following snippets cover the three smallest "I just want to put
text on a page" patterns. Use them as starting points before reaching
for a focused recipe page.

### Paragraph module

```java
document.pageFlow(page -> page
        .module("Professional Summary", module -> module.paragraph(
                "Backend engineer focused on secure Java systems and reliable document generation.")));
```

### Bullet list

```java
document.pageFlow(page -> page
        .module("Technical Skills", module -> module.bullets(
                "Java 21",
                "Spring Boot",
                "PostgreSQL",
                "Docker")));
```

### Markerless rows

```java
document.pageFlow(page -> page
        .module("Projects", module -> module.rows(
                "GraphCompose - Declarative PDF/document layout engine.",
                "CVRewriter - Profile-aware CV tailoring platform.")));
```

### Snapshot regression in a test

```java
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;

try (DocumentSession document = GraphCompose.document().create()) {
    document.pageFlow(page -> page
            .module("Snapshot Example", module -> module.paragraph("Hello GraphCompose")));

    LayoutSnapshotAssertions.assertMatches(document, "my-feature/hello");
}
```

See [recipes/extending.md § 4](recipes/extending.md#4-validate-a-custom-nodes-layout-via-snapshots)
for the full snapshot workflow including baseline approval.
