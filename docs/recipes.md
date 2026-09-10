# Recipes

GraphCompose recipes are split into focused pages so each page covers
one topic end-to-end. All recipes use only the canonical session-first
authoring API; public application code should not import
`com.demcha.compose.engine.*`.

## Where content goes

Most document features are blocks in the same `pageFlow`. A section, module, row,
or card is just a container that groups those blocks; it does not introduce a
different authoring API.

```text
DocumentSession
├── pageBackground(...) / pageBackgrounds(...)  behind every page
├── chrome() / header(...) / footer(...)         repeating page chrome
└── pageFlow(...)
    ├── addParagraph / addRich / addList         text
    ├── addImage / addSvgIcon / addBarcode       media
    ├── addTable / chart / addTimeline           data and stories
    ├── addSection / module                      grouped content
    ├── addRow                                   side-by-side content
    ├── addContainer / addLayerStack             clipped or overlapping content
    └── addCanvas                                absolute placement, only when needed
```

Calls such as `chart(...)`, `addTable(...)`, and `addImage(...)` are shared by
the flow containers. Put them directly on the page, or call the same method inside
a section/module when the element needs a title, panel, padding, or accent.

## Find the feature, then refine it

Start with the entry point in the middle column. The linked recipe answers the next
questions: where the element can live, which controls matter most, and how
pagination or backend limits behave when they apply.

### Content and data

| I want to add… | Smallest entry point | Recipe answers next |
| --- | --- | --- |
| [A paragraph with links, styles, icons, or emoji](recipes/rich-text.md) | `addRich(rich -> ...)` | runs, links, SVG icons, emoji, inline shapes |
| [A list](recipes/lists.md) | `addList(list -> ...)` | markers, nesting, spacing, styled items |
| [A table](recipes/tables.md) | `addTable(table -> ...)` | columns, cells, zebra rows, totals, repeated headers |
| [A chart](recipes/charts.md) | `chart(ChartSpec...)` | labels, value formats, colours, bar/line shape, background, legend |
| [A timeline](recipes/timelines.md) | `addTimeline(timeline -> ...)` | markers, rail geometry, dated entries, text styles, pagination |
| [An image](recipes/images.md) | `addImage(image -> ...)` | path/bytes, size, contain/cover, links |
| [A QR code or barcode](recipes/barcodes.md) | `addBarcode(barcode -> ...)` | format, colour, quiet zone, card alignment |

### Layout and visual composition

| I want to build… | Smallest entry point | Recipe answers next |
| --- | --- | --- |
| [A section, panel, divider, or visual shape](recipes/shapes.md) | `addSection(...)` / `softPanel(...)` | fill, border, radius, accent, spacing, primitive shapes |
| [Side-by-side columns](recipes/layered-page-design.md) | `addRow(row -> ...)` | column weights, flow layout, and when a row is the right primitive |
| [Overlapping content](recipes/layered-page-design.md) | `addLayerStack(stack -> ...)` | alignment, offsets, z-index, and when to use a container or canvas |
| [A clipped circle, ellipse, or custom container](recipes/shape-as-container.md) | `addContainer(...)` | outline, child alignment, `CLIP_PATH`, bounds, visible overflow |
| [A repeating page tint, sidebar, or band](recipes/page-backgrounds.md) | `pageBackgrounds(...)` | full fills, partial fills, bleed, and layering |
| [Rotation, scale, or layer ordering](recipes/transforms.md) | `rotate(...)` / `scale(...)` / `zIndex(...)` | transform origin, clipped transforms, deterministic overlap |
| [Pixel-precise placement](recipes/absolute-placement.md) | `addCanvas(...)` | fixed box size, `(x, y)` positions, clipping, appropriate use cases |
| [A block that should not split badly](recipes/keep-together.md) | `keepTogether()` / `keepWithNext()` | sections, lines, timeline entries, relocation at page breaks |
| [A theme shared across documents](recipes/themes.md) | `BrandTheme` | palette, typography, spacing, decoration, preset-level reuse |

### Page behaviour, output, and development

| I want to… | Smallest entry point | Recipe answers next |
| --- | --- | --- |
| [Add metadata, watermark, header, footer, or page numbers](recipes/pdf-chrome.md) | `metadata(...)` / `header(...)` / `footer(...)` | text chrome, node-based page zones, protection, viewer behaviour |
| [Add links, bookmarks, or a clickable table of contents](recipes/in-pdf-navigation.md) | `anchor(...)` / `linkTo(...)` | internal destinations, page references, TOC entries, PDF actions |
| [Preview, stream, or choose an output form](recipes/streaming.md) | `buildPdf()` / `writePdf(...)` / `toImage(...)` | files, streams, bytes, preview images, backend selection |
| [See layout boxes and node names while developing](getting-started.md#debug-guide-lines) | `guideLines(true)` / `debug(...)` | margin guides, resolved boxes, stable node labels |
| [Protect a document from regressions](operations/test-your-document.md) | `LayoutSnapshotAssertions` | smoke tests, geometry snapshots, pixel-level PDF diffs, CI flow |
| [Export semantic DOCX](recipes/docx-export.md) | `export(new DocxSemanticBackend())` | semantic mapping and fixed-layout feature fallbacks |
| [Add a new node or backend capability](recipes/extending.md) | `NodeDefinition` / render handler | extension path, fluent builder, rendering, snapshot coverage |

## Full recipe catalogue

| Page | Covers |
| --- | --- |
| [Charts](recipes/charts.md) | Native vector bar / line / area / pie-donut charts: data–spec–style layers, axis & grid toggles, point markers, value-label halos, legend placement, translucent area fills |
| [Keep-together pagination](recipes/keep-together.md) | `keepTogether()` / `keepEntriesTogether()` — blocks that relocate whole instead of orphaning a heading at a page break |
| [Fixed-width flows](recipes/fixed-width-flows.md) | `fixedWidth(points)` — a section, module or page flow pinned to a narrow width, with the height left content-driven |
| [Themes](recipes/themes.md) | `BrandTheme` token bundle (palette / typography / spacing / decoration), theme factories per family, page background, direct DSL styling |
| [Shapes and visual primitives](recipes/shapes.md) | Filled cards, dividers, spacers, lines, ellipses, image fit modes, soft panels |
| [Shape-as-container](recipes/shape-as-container.md) | `addCircle` / `addEllipse` / `addContainer` with `ClipPolicy` (clipped layered children) |
| [Transforms and z-index](recipes/transforms.md) | `rotate` / `scale` mixin, per-layer `zIndex` for overlays |
| [Page backgrounds](recipes/page-backgrounds.md) | `pageBackground` / `pageBackgrounds`, `PageBackgroundFill` columns, bands, point-based fills, layering |
| [Layered page design](recipes/layered-page-design.md) | Page background vs. row vs. layer stack vs. canvas — choosing the layer; how a row splits its width, icon beside text, a rule that reaches the column edge |
| [Absolute placement](recipes/absolute-placement.md) | `addCanvas` + `position(x, y)` for pixel-precise certificates and badges |
| [Tables](recipes/tables.md) | Row span, zebra rows, totals row, repeated header on page break |
| [Text direction](recipes/text-direction.md) | `TextDirection` — right-to-left paragraphs, `AUTO` resolved from the text, mixed lines, and the bundled Hebrew / Arabic families |
| [Rich text](recipes/rich-text.md) | `RichText` mixed-style runs in one paragraph: bold/accent/styled segments, inline links, inline images, inline SVG icons, emoji shortcodes, inline shapes and checkboxes |
| [Lists](recipes/lists.md) | `addList`: quick bulleted lists, marker customisation, nested lists with per-depth markers, spacing and styled items |
| [Timelines](recipes/timelines.md) | `addTimeline`: the leading / axis / content model, markers (dot / circle / numbered / square / custom), leading column, axis sizing, `markerOnRail()`, rail extent, pagination, backends |
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
