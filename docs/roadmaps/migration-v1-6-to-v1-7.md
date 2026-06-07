# Migration: v1.6 → v1.7

v1.7 — codenamed **"geometric"** — is **additive only**. Every public
type, method, and behaviour from v1.6 is unchanged: bump the dependency
to `1.7.0` and rebuild with **no source changes required**. Adding public
API is what turns the open cycle into a minor release.

The theme of the release is **geometry as a first-class authoring
primitive** — shapes you used to fake with font glyphs or raster images
are now drawn directly from vertex geometry, inline on the paragraph
baseline or block-level in a shape container.

If your application targets v1.6 today, there is nothing to do but
upgrade. The rest of this guide is a tour of what you can now reach for.

## TL;DR

Everything below is `@since 1.7.0` and purely additive — no v1.6 API is
replaced, deprecated, or removed.

| Area | v1.7 addition | Reach for it when |
| --- | --- | --- |
| **Inline shape runs** | `RichText` / `ParagraphBuilder` `dot`, `ellipse`, `diamond`, `triangle`, `star`, `shape(ShapeOutline, …)` | skill-rating dots (`Java ●●●●○`), custom bullets, inline status markers — without depending on a font shipping the glyph |
| **Polygon geometry** | `ShapeOutline` `polygon`, `diamond`, `triangle`, `star`, `arrow` / `arrowRight` / `arrowLeft` (`Direction`), `chevron`, `checkmark`, `plus`, `regularPolygon(sides)` | directional bullets ("Step 1 → Step 2", "Home › Docs"), block-level or inline figures from normalized `ShapePoint` vertices |
| **Inline checkboxes** | `RichText` / `ParagraphBuilder` `checkbox(size, checked, color)` / `checkbox(size, checked, boxColor, checkColor)` | todo / checklist markers inline in running text |
| **Composite inline figures** | `ShapeLayer` stack on `InlineShapeRun` | multi-layer marks (frame + tick) measured and placed as one unit on the baseline |
| **Swappable tick / arrow designs** | `ShapeOutline.CheckmarkStyle` (`CLASSIC` / `HEAVY`), `ArrowStyle` (`BLOCK` / `TRIANGLE`) + matching `checkmark(…)` / `arrow(…)` overloads | choosing the look of a checkmark or arrow; default look is unchanged |
| **Per-corner radius** | `ShapeContainerBuilder.roundedRect(width, height, DocumentCornerRadius)` + `DocumentCornerRadius.left/right/top/bottom(…)` | a card rounded on some corners, square on others — no CLIP_PATH-parent workaround |
| **`softPanel(…)` stroke overloads** | `softPanel(color, radius, padding, stroke)` | a rounded, padded background **and** an outline on one flow node |
| **Vertical text seating** | shape-container label vertical alignment | seating a label top / middle / bottom inside its line box |
| **Filled title band** | `headingBar(…)` | a section title on a filled accent band |
| **Dashed / dotted lines** | `LineBuilder.dashed(…)` + `DocumentDashPattern` | separators, cut lines, framing |
| **Semantic timelines** | `addTimeline(…)` + `TimelineBuilder` | chronologies, roadmaps, process steps |
| **Remaining-height accessor** | `canvas().innerHeight()` / `availableHeight()` alias | sizing a block to the usable page-content height |
| **JetBrains Mono** | bundled in the default font library | monospaced code / data without shipping your own font |

Runnable code for each primitive lives in the
[examples gallery](../../examples/README.md); the exact public-API list
is in [`CHANGELOG.md`](../../CHANGELOG.md) under **v1.7.0**.

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

> **Binary compatibility note.** `ParagraphFragmentPayload` (an internal
> layout→render payload in
> `com.demcha.compose.document.layout.payloads`) gained a `verticalAlign`
> component for vertical text seating. It is internal plumbing — not part
> of the canonical authoring API and not constructed by callers — so the
> package is excluded from the binary-compatibility gate. The published
> authoring surface stays binary-compatible with v1.6.

## Upgrading

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose</artifactId>
    <version>1.7.0</version>
</dependency>
```

That is the entire migration. Pull in any of the primitives above as you
need them.
