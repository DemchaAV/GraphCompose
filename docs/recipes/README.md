# Recipes

Task-oriented guides: each page shows how to use one slice of the canonical
API, with copy-pasteable snippets verified against the current release.

## Available recipes

| Recipe | Covers |
|---|---|
| [charts.md](charts.md) | Native vector bar / line / area / pie-donut charts: data–spec–style layers, axis & grid toggles, point markers, value-label halos, legend placement, translucent area fills |
| [rich-text.md](rich-text.md) | `RichText` mixed-style runs in one paragraph: bold/accent/styled segments, inline links, inline images, inline shapes and checkboxes |
| [lists.md](lists.md) | `addList`: quick bulleted lists, marker customisation, nested lists with per-depth markers, spacing and styled items |
| [timelines.md](timelines.md) | `addTimeline`: markers (dot / circle / numbered / square) on a connector rail, geometry and text-style controls, pagination opt-ins |
| [keep-together.md](keep-together.md) | `keepTogether()` / `keepEntriesTogether()` — blocks that relocate whole instead of orphaning a heading at a page break |
| [shapes.md](shapes.md) | Filled cards, dividers, accent bars, lines, spacers, ellipses, images |
| [shape-as-container.md](shape-as-container.md) | Circles/ellipses/rounded cards holding clipped children, `ClipPolicy` |
| [absolute-placement.md](absolute-placement.md) | `CanvasLayerNode` — children at explicit (x, y) |
| [layered-page-design.md](layered-page-design.md) | Page background vs. row vs. layer stack vs. canvas — choosing the layer |
| [page-backgrounds.md](page-backgrounds.md) | Per-page fills: sidebars, bands, layered tints |
| [transforms.md](transforms.md) | Rotation, scaling, skewing |
| [tables.md](tables.md) | Tabular layouts: columns, headers, zebra rows, composed cells |
| [themes.md](themes.md) | `BusinessTheme` presets and custom palettes |
| [barcodes.md](barcodes.md) | QR / Code 128 / Code 39 / EAN / UPC / PDF417 / DataMatrix, tinting, quiet zone, card centring |
| [images.md](images.md) | Sources (bytes/path), sizing precedence, STRETCH/CONTAIN/COVER fit modes, images in rows and cards |
| [pdf-chrome.md](pdf-chrome.md) | Metadata, watermarks, running header/footer with `{page}/{pages}/{date}`, protection, links and outline bookmarks |
| [translucency.md](translucency.md) | `DocumentColor.rgba` / `withOpacity`: which primitives honour alpha, byte-identity for opaque colours, layered tints |
| [docx-export.md](docx-export.md) | Semantic DOCX export: 1:1 node mapping, chart/shape-container fallbacks, skipped kinds |
| [snapshot-testing.md](snapshot-testing.md) | Layout-snapshot regression testing in consumer projects, baseline update flow |
| [streaming.md](streaming.md) | Streaming PDFs to HTTP responses |
| [extending.md](extending.md) | Extension patterns: custom nodes via the `NodeDefinition` SPI |

Every shipped feature now has a recipe; the runnable
[examples](../../examples/README.md) remain the end-to-end references.
