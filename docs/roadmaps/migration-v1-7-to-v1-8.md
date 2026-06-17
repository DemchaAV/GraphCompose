# Migration: v1.7 → v1.8

v1.8 — codenamed **"illustrative"** — is **additive only**. Every public
type, method, and behaviour from v1.7 is unchanged: bump the dependency
to `1.8.0` and rebuild with **no source changes required**. Adding public
API is what turns the open cycle into a minor release.

The theme of the release is **data and artwork as first-class authoring
primitives** — charts you used to rasterise through Graphics2D, and icons
you used to embed as images, are now drawn directly from semantic data
and native vector geometry, deterministic and snapshot-testable like the
rest of the document.

If your application targets v1.7 today, there is nothing to do but
upgrade. The rest of this guide is a tour of what you can now reach for.

## TL;DR

Everything below is `@since 1.8.0` and purely additive — no v1.7 API is
replaced, deprecated, or removed.

| Area | v1.8 addition | Reach for it when |
| --- | --- | --- |
| **Native vector charts** | `ChartData` + sealed `ChartSpec` (`bar()` / `line()` / `pie()`), `ChartStyle`, `section.chart(spec)` / `chart(spec, style)` | bar / line / pie-donut charts compiled into engine primitives — deterministic, themable, no rasterised PNG |
| **Line interpolation modes** | `LineInterpolation` (`LINEAR` / `SMOOTH` / `MONOTONE`) via `ChartSpec.line().interpolation(…)` | a pretty curve (`SMOOTH`), or a smooth curve that never overshoots the data (`MONOTONE`), or exact straight segments (`LINEAR`) |
| **Inline sparklines** | `RichText` / `ParagraphBuilder` `sparkline(…)` / `sparklineLine(…)` | mini trend charts on the text baseline, inside running prose |
| **Vector path primitive** | `PathNode` + `addPath(p -> p.moveTo(…).curveTo(…)…)` on every flow builder; `dashed(…)` | open, curve-capable design shapes rendered with native PDF curve operators (no tessellation) |
| **Free-form clip outline** | `ShapeOutline.Path` + `ShapeContainerBuilder.path(w, h, segments)` / `path(w, h, svgPath)` | clip a container's children to — and fill / stroke along — an arbitrary native-curve silhouette |
| **SVG path & icon import** | `SvgPath.parse(d)`, `PathBuilder.svg(…)`, `SvgIcon.read(file)` / `parse(xml)`, `addSvgIcon(icon, width)` (**beta**) | drop an icon or logo's `d` string / whole SVG file in as native curves, with native linear / radial gradients on fills and strokes |
| **Block-level horizontal alignment** | `AlignNode` + `HorizontalAlign` (`LEFT` / `CENTER` / `RIGHT`), `flow.addAligned(align, node)`, `flow.addSvgIcon(icon, width, align)` | centre or right-align a fixed-size flow child (path, image, icon, barcode, shape) without a full-width wrapper |
| **Declarative paints** | `DocumentPaint` (solid, `LinearAxis`, `RadialCircle`); `PathNode` / `PathBuilder` `fill(paint)` / `strokePaint(paint)` | endpoint-exact gradient fills and strokes on charts and paths |

Runnable code for each primitive lives in the
[examples gallery](../../examples/README.md); the exact public-API list
is in [`CHANGELOG.md`](../../CHANGELOG.md) under **v1.8.0**.

## One behaviour to know about

- **`ChartData.Series` rejects non-finite values.** A `NaN` / ±∞ entry
  now fails fast at construction — naming the series and the offending
  index — instead of poisoning axis derivation and surfacing later as a
  misleading "height must be finite" failure in the layout pass. `null`
  entries are still allowed as gaps. This only affects code that builds a
  chart series from a value that can be non-finite; guard or filter such
  values before constructing the series.

## Things that did NOT break

- Every entry point on `GraphCompose`, the full `DocumentSession`
  authoring lifecycle (compose, `pageFlow`, add, `buildPdf`, export,
  close, `layoutGraph`, `layoutSnapshot`).
- `DocumentDsl`, `BusinessTheme`, `DocumentPalette`, and the invoice /
  proposal / CV / cover-letter / weekly-schedule template entry points
  (V1 and V2).
- The fixed-layout and semantic backend SPIs and every public render
  handler.
- Layout snapshots and visual-regression baselines — the additive
  primitives change geometry only where you opt into them.

> **Binary compatibility note.** `ShapeOutline` gained a `Path` permit
> and `DocumentPaint` gained `LinearAxis` / `RadialCircle` forms. These
> are additive — the published authoring surface stays binary-compatible
> with v1.7 (the `japicmp` gate is green); only consumer code that
> exhaustively `switch`es over the sealed `ShapeOutline` would need a new
> branch, and the canonical authoring surface exposes no such switch.

## Upgrading

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose</artifactId>
    <version>1.8.0</version>
</dependency>
```

That is the entire migration. Pull in any of the primitives above as you
need them.
