# Recipes

Task-oriented guides: each page shows how to use one slice of the canonical
API, with copy-pasteable snippets verified against the current release.

## Available recipes

| Recipe | Covers |
|---|---|
| [charts.md](charts.md) | Native vector bar / line / area / pie-donut charts: data–spec–style layers, axis & grid toggles, point markers, value-label halos, legend placement, translucent area fills |
| [keep-together.md](keep-together.md) | `keepTogether()` / `keepEntriesTogether()` — blocks that relocate whole instead of orphaning a heading at a page break |
| [shapes.md](shapes.md) | Filled cards, dividers, accent bars, lines, spacers, ellipses, images |
| [shape-as-container.md](shape-as-container.md) | Circles/ellipses/rounded cards holding clipped children, `ClipPolicy` |
| [absolute-placement.md](absolute-placement.md) | `CanvasLayerNode` — children at explicit (x, y) |
| [layered-page-design.md](layered-page-design.md) | Page background vs. row vs. layer stack vs. canvas — choosing the layer |
| [page-backgrounds.md](page-backgrounds.md) | Per-page fills: sidebars, bands, layered tints |
| [transforms.md](transforms.md) | Rotation, scaling, skewing |
| [tables.md](tables.md) | Tabular layouts: columns, headers, zebra rows, composed cells |
| [themes.md](themes.md) | `BusinessTheme` presets and custom palettes |
| [streaming.md](streaming.md) | Streaming PDFs to HTTP responses |
| [extending.md](extending.md) | Extension patterns: custom nodes via the `NodeDefinition` SPI |

## Not yet covered (planned)

Functionality that ships in the library but does not have a dedicated recipe
yet — until then, the runnable [examples](../../examples/README.md) are the
reference for these:

- Rich text and inline runs (mixed styles, inline images/shapes, checkboxes)
- Lists and nested lists
- Timelines (`addTimeline`, markers, connector rail)
- Barcodes and QR codes
- Images: sources, sizing, fit modes
- Links, bookmarks, and PDF chrome (metadata, watermark, header/footer, protection)
- Translucent colours (`DocumentColor.rgba` / `withOpacity`) beyond their chart usage
- Semantic DOCX export and its fallbacks
- Layout-snapshot regression testing in consumer projects
