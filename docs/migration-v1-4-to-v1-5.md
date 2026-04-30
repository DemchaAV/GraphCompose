# Migration: v1.4 → v1.5

v1.5 is **fully source-compatible with v1.4**. Every public class,
method, and constructor that v1.4 callers used continues to compile
and behave the same way without code changes. The release adds
opt-in features rather than reshaping existing surfaces.

If your application targets v1.4, you can bump the dependency version
to `1.5.x` and rebuild without touching authoring code. The sections
below summarise what's new, in case you want to adopt the new APIs.

## TL;DR

| Area | v1.5 addition | Opt-in |
| --- | --- | --- |
| Layer overlays | Nine alignment shortcuts (`topLeft` … `bottomRight`) plus `position(node, dx, dy, anchor)` | Yes — additions to `LayerStackBuilder` |
| Shape composition | `addCircle(diameter, fill, inside)` / `addEllipse(w, h, fill, inside)` / `addContainer(...)` build a clipped `ShapeContainerNode` | Yes — new builder + factory methods |
| Transforms | `rotate(degrees)` / `scale(uniform)` / `scale(sx, sy)` on `ShapeContainerBuilder` (Transformable<T> mixin) | Yes — new methods |
| z-index | `layer(node, align, zIndex)` and `position(node, dx, dy, align, zIndex)` on layer stacks and shape containers | Yes — new overloads; default `zIndex = 0` keeps source order |
| Tables | `colSpan(int)` (already in v1.4) joined by `rowSpan(int)`, `zebra(odd, even)`, `totalRow(...)`, `repeatHeader()` | Yes — new methods |
| DSL fluent rich text | `DocumentDsl.richText(Consumer<RichText>)` callback entry | Yes — new entry point |

## Things that did NOT break

* The `LayerStackNode.Layer` record gained a fifth field
  (`int zIndex`) but the original 1-, 2-, and 4-arg constructors
  remain and default `zIndex` to `0`.
* `DocumentTableCell` gained a fifth field (`int rowSpan`); the
  original 2-arg and 3-arg constructors remain and default `rowSpan`
  to `1`.
* `TableNode` gained a 12th field (`int repeatedHeaderRowCount`);
  the original 7-arg, 9-arg, and 11-arg constructors remain and
  default the new field to `0`.
* `TableResolvedCell` (engine type used by render handlers) gained a
  fifth field (`double yOffset`); the original 8-arg constructor
  remains and defaults `yOffset` to `0`.
* `BuiltInNodeDefinitions.PreparedStackLayout` gained a fourth list
  (`zIndices`) but the previous 3-arg and 1-arg constructors remain
  and default the list to all-zero.

So third-party tests that constructed any of the above with positional
arguments still compile, and behaviour for those callers is byte-for-
byte identical to v1.4.

## RowBuilder.gap → spacing

`RowBuilder.gap(double)` was renamed to `spacing(double)` to match the
rest of the canonical DSL. The old name still compiles via a
`@Deprecated` alias that delegates to `spacing(...)`. Replace at your
convenience:

```java
// Before
.addRow(row -> row.gap(8).weights(1, 1).add(...))

// After
.addRow(row -> row.spacing(8).weights(1, 1).add(...))
```

## RowBuilder.add — eager validation

`RowBuilder.add(node)` now validates the child type **eagerly** and
raises `IllegalArgumentException` from the offending call site instead
of deferring to `build()` and raising `IllegalStateException` later.
If you have tests that asserted the deferred exception, switch them
to `IllegalArgumentException`.

## New recipe pages

The recipe catalogue split into focused pages:

- [`docs/recipes/shape-as-container.md`](recipes/shape-as-container.md)
- [`docs/recipes/transforms.md`](recipes/transforms.md)
- [`docs/recipes/tables.md`](recipes/tables.md) — covers row span,
  zebra, totals, repeated header

## Worth checking

If you depend on the engine internals (the `com.demcha.compose.engine`
packages — typically only test code or low-level integrations), note:

* `NodeDefinition` gained a default `emitOverlayFragments(...)` method
  returning an empty list. Existing implementations inherit the
  default and need no changes.
* `LayoutCompiler.compileStackedLayer` and the STACK branch of
  `compileNodeInFixedSlot` now stable-sort layer iteration by
  `zIndex`. With default `zIndex = 0` this is a no-op
  (identity permutation) but the iteration order semantics changed
  from "always source order" to "by ascending zIndex, source order on
  ties".

If you have engine-side fixtures that hand-construct layer lists with
custom `PreparedStackLayout` payloads, prefer the new four-arg
constructor and pass an explicit `zIndices` list.
