# Recipes

GraphCompose recipes are split into focused pages so each page covers
one topic end-to-end. All recipes use only the canonical session-first
authoring API; public application code should not import
`com.demcha.compose.engine.*`.

## Topic-focused recipe pages

| Page | Covers |
| --- | --- |
| [Themes](recipes/themes.md) | `BusinessTheme.classic / modern / executive`, page background, palette slots, text scale, the `CvTheme` ↔ `BusinessTheme` bridge |
| [Shapes and visual primitives](recipes/shapes.md) | Filled cards, dividers, spacers, lines, ellipses, image fit modes, soft panels |
| [Shape-as-container](recipes/shape-as-container.md) | `addCircle` / `addEllipse` / `addContainer` with `ClipPolicy` (clipped layered children) |
| [Transforms and z-index](recipes/transforms.md) | `rotate` / `scale` mixin, per-layer `zIndex` for overlays |
| [Tables](recipes/tables.md) | Row span, zebra rows, totals row, repeated header on page break |
| [Streaming and output](recipes/streaming.md) | `buildPdf` / `writePdf` / `toPdfBytes`, DOCX export, layout snapshots, header / footer chrome, guide lines |
| [Extending GraphCompose](recipes/extending.md) | New semantic node, fluent setter, render backend, snapshot-based regression tests |

For longer-form material:

- [Extension guide](extension-guide.md) — walkthrough of the four
  extension paths with the v1.5 `ShapeContainerNode` work as a
  worked example.
- [Migration v1.4 → v1.5](migration-v1-4-to-v1-5.md) — every public
  API change in v1.5 plus suggested migration order.
- [`ADR 0001 — Shape as container`](adr/0001-shape-as-container.md)
  and [`ADR 0002 — Theme unification`](adr/0002-theme-unification.md)
  for the design reasoning behind the two largest v1.5 additions.

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
