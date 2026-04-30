# Extending GraphCompose

Short cheatsheets for the four most common extension paths. Each
section gives you a 30-second mental model plus a pointer to the deep
walkthrough in [`docs/extension-guide.md`](../extension-guide.md),
which uses the v1.5 `ShapeContainerNode` work as a worked example.

| You want to... | Touch | Read |
| --- | --- | --- |
| Add a new semantic node | `DocumentNode` record + `NodeDefinition` + render handler | [Extension guide § 1](../extension-guide.md#1-add-a-semantic-node) |
| Add a fluent setter | One `*Builder` only | [Extension guide § 2](../extension-guide.md#2-add-a-fluent-setter-to-a-builder) |
| Add a render backend | Implement `FixedLayoutBackend` or `SemanticBackend` | [Extension guide § 3](../extension-guide.md#3-add-a-render-backend) |
| Pin layout in a snapshot test | Use `LayoutSnapshotAssertions.assertMatches` | [Extension guide § 4](../extension-guide.md#4-validate-a-custom-nodes-layout-via-snapshots) |

## 1. Add a semantic node — five-step skeleton

```text
1. record FooNode(...) implements DocumentNode
2. FooDefinition implements NodeDefinition<FooNode>
3. FooFragmentPayload (carries the data the render handler needs)
4. Register the definition in BuiltInNodeDefinitions.registerDefaults(...)
5. PdfFooRenderHandler implements FixedRenderHandler<FooFragmentPayload>
```

The Phase B `ShapeContainerNode`, `ShapeContainerDefinition`,
`ShapeFragmentPayload`, and `PdfShape*RenderHandler` files are the
canonical worked example. Walk
[`docs/extension-guide.md`](../extension-guide.md) for the full
narrative.

## 2. Add a fluent setter — single-builder change

Most "I want my own DSL verb" requests stop at the builder layer.
Pick the existing builder (e.g. `ParagraphBuilder` or
`SectionBuilder`), add a setter that mutates the builder's state, and
return `this` for chaining:

```java
public ParagraphBuilder dropCap(boolean enabled) {
    this.dropCap = enabled;
    return this;
}
```

The setter feeds the builder's `build()` method, which constructs the
`DocumentNode` record. No layout or render changes needed if the
behaviour can be composed from existing fragments.

## 3. Add a render backend

A render backend consumes the **layout graph** (a deterministic list
of `PlacedFragment`s with typed payloads) and produces bytes. Two
shapes:

- **Fixed-layout backend** (PDF-like): implement
  `FixedLayoutBackend.render(LayoutGraph, OutputStream)` and dispatch
  on payload type.
- **Semantic backend** (DOCX, slide formats, manifest exporters):
  implement `SemanticBackend<R>.export(DocumentSession, Path)` and
  walk the semantic tree directly.

The DOCX backend (`DocxSemanticBackend`) is a good reference for the
semantic shape; the PDF backend (`PdfFixedLayoutBackend`) shows the
fixed-layout shape including the v1.5 graphics-state markers used by
shape-container clipping and CTM transforms.

## 4. Validate a custom node's layout via snapshots

Snapshots are deterministic, renderer-agnostic JSON pinned to disk.
The helper hides path resolution, baseline approval, and diff
artifacts:

```java
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;

@Test
void myNodeLaysOutAsExpected() throws Exception {
    try (DocumentSession document = GraphCompose.document()
            .pageSize(260, 180)
            .margin(DocumentInsets.of(18))
            .create()) {
        composeMyDocument(document);
        LayoutSnapshotAssertions.assertMatches(document, "my-feature/scenario_a");
    }
}
```

On first run the assertion writes
`src/test/resources/layout-snapshots/my-feature/scenario_a.json`. To
accept a baseline change after a deliberate refactor, re-run with
`-Dgraphcompose.updateSnapshots=true`. See
[`LayoutSnapshotRegressionExample`](../../examples/src/main/java/com/demcha/examples/LayoutSnapshotRegressionExample.java)
for a runnable end-to-end demonstration.

## See also

- [Extension guide](../extension-guide.md) — long-form walkthrough
  with the `ShapeContainerNode` worked example.
- [Streaming and output](streaming.md) — how custom backends slot
  into the existing `writePdf` / `toPdfBytes` / `export(...)` paths.
- [`ADR 0001`](../adr/0001-shape-as-container.md) and
  [`ADR 0002`](../adr/0002-theme-unification.md) for the design
  reasoning behind two recent extension points.
