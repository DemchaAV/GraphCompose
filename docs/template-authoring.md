# Template authoring — canonical cheatsheet

This page is the single reference for writing new templates and DSL
code without boilerplate. Read it once before starting a template;
keep it open while you're writing one.

The companion long-form docs are
[`getting-started.md`](getting-started.md) (concepts) and
[`extension-guide.md`](extension-guide.md) (engine internals). When
the cheatsheet says "see X recipe", the file lives under
[`recipes/`](recipes/).

---

## 1. Entry & output pipeline

```java
try (DocumentSession document = GraphCompose.document(Path.of("output.pdf"))
        .pageSize(DocumentPageSize.A4)
        .pageBackground(theme.pageBackground())   // optional
        .margin(28, 28, 28, 28)
        .create()) {

    document.pageFlow(page -> page
        .addSection("Hero", section -> section
            .softPanel(theme.palette().surfaceMuted(), 10, 14)
            .addParagraph(p -> p.text("Hello").textStyle(theme.text().h1()))));

    document.buildPdf();           // file path supplied above
}
```

| You want                       | Method                                      |
| ------------------------------ | ------------------------------------------- |
| Write a file (path was set)    | `document.buildPdf()`                       |
| Write a file (no path was set) | `document.buildPdf(Path)`                   |
| Stream to HTTP / S3            | `document.writePdf(OutputStream)`           |
| Buffer in memory               | `document.toPdfBytes()` → `byte[]`          |
| Non-PDF backend (DOCX, …)      | `document.export(SemanticBackend, Path)`    |
| Snapshot for regression tests  | `document.layoutSnapshot()` → `LayoutSnapshot` |

`writePdf` does **not** close the supplied stream. The session
itself must be closed (try-with-resources).

---

## 2. Builder hierarchy

```
DocumentSession
 └─ pageFlow() → PageFlowBuilder            (extends AbstractFlowBuilder)
     ├─ addSection(name, spec)  → SectionBuilder    (extends AbstractFlowBuilder)
     ├─ module(title, spec)     → ModuleBuilder     (extends AbstractFlowBuilder, prepends a title paragraph)
     ├─ addRow(spec)            → RowBuilder        (horizontal, atomic; rejects nested rows + tables)
     │   └─ addSection / addParagraph / addImage / addLayerStack / …
     ├─ addParagraph(spec)      → ParagraphBuilder    (text or RichText, mutually exclusive)
     ├─ addList(spec)           → ListBuilder        (bullets / dashes / numbers / custom marker)
     ├─ addTable(spec)          → TableBuilder       (splittable; never inside a row)
     ├─ addImage / addShape / addEllipse / addLine / addDivider / addSpacer / addBarcode / addPageBreak
     ├─ addContainer(spec)      → ShapeContainerBuilder  (clipped composite, Transformable)
     └─ addLayerStack(spec)     → LayerStackBuilder      (atomic overlay, allowed inside rows)
```

`document.dsl()` exposes the same primitives as detached factories
when you need a `DocumentNode` outside a flow (e.g. composing a
`LayerStack` ahead of time).

---

## 3. Per-builder one-liners

| Builder | Produces | Top fluent methods | Watch out for |
|---|---|---|---|
| `PageFlowBuilder` | root `ContainerNode`, *auto-attaches on `build()`* | `name`, `spacing`, `softPanel`, `addSection`, `module` | Forgetting trailing `.build()` — root is never attached |
| `SectionBuilder` / `ModuleBuilder` | `SectionNode` | `spacing`, `padding`, `margin`, `softPanel(...)`, `accentLeft/Right/Top/Bottom(color, w)`, `cornerRadius(DocumentCornerRadius)` | `ModuleBuilder` adds `title()`, `bullets()`, `dashList()`, `table()` |
| `RowBuilder` | `RowNode` | `spacing`, `weights(double...)`, `evenWeights()`, `addSection(spec)` (column) | `gap()` is **deprecated** since 1.5 — use `spacing`. Rejects nested `RowNode` and any `TableNode` at `add()` time |
| `ParagraphBuilder` | `ParagraphNode` | `text`, `textStyle`, `align`, `lineSpacing(double)`, `rich(Consumer<RichText>)`, `inlineLink/inlineImage` | `text()` and `rich()` are mutually exclusive |
| `ListBuilder` | `ListNode` | `items(...)`, `bullet() / dash() / noMarker() / marker(String)`, `textStyle`, `itemSpacing`, `continuationIndent` | `normalizeMarkers(true)` strips leading `- ` / `* ` from raw input |
| `TableBuilder` | `TableNode` | `columns(...)` / `autoColumns(int)`, `headerRow(...)` + `headerStyle(style)`, `row(...)`, `totalRow(style, ...)`, `zebra(odd, even)`, `repeatHeader()` | Style precedence at `build()`: `rowStyle`/`headerStyle`/`totalRow` always beat `zebra` |
| `ImageBuilder` | `ImageNode` | `source(Path|byte[]|String|DocumentImageData)`, `size(w,h)` / `fitToBounds(w,h)`, `fitMode(DocumentImageFitMode)` | Default is `STRETCH`; `fitToBounds` switches to `CONTAIN` |
| `ShapeBuilder` | `ShapeNode` | `size`, `fillColor`, `stroke`, `cornerRadius(double|DocumentCornerRadius)` | — |
| `ShapeContainerBuilder` | `ShapeContainerNode` (Transformable) | `rectangle/roundedRect/ellipse/circle(...)`, `clipPolicy(ClipPolicy)`, 9-point alignment shortcuts, `position(node, dx, dy, align[, z])`, `rotate/scale` | `build()` throws if outline missing. Default `ClipPolicy.CLIP_PATH` |
| `LayerStackBuilder` | `LayerStackNode` (atomic) | `layer(node[, align[, z]])`, 9-point shortcuts, `back(node)`, `position(node, dx, dy, align[, z])` | Allowed inside a `RowBuilder`. Source order = paint order unless `zIndex` is set |
| `DividerBuilder` | `ShapeNode` | `width`, `thickness`, `color` | Subclass of `ShapeBuilder`; default 1pt `LIGHT_GRAY` |
| `SpacerBuilder` | `SpacerNode` | `size(w, h)` / `width()` / `height()` | — |
| `LineBuilder` | `LineNode` | `horizontal(w)` / `vertical(h)` / `diagonal`, `color`, `thickness` | — |
| `EllipseBuilder` | `EllipseNode` | `circle(d)` / `size(w, h)`, `fillColor`, `stroke` | `addCircle(d, fill)` is the convenience overload on the parent flow |
| `BarcodeBuilder` | `BarcodeNode` | `data`, `qrCode/code128/code39/ean13/ean8`, `size`, `quietZone(int)` | Default type is `QR_CODE` |
| `PageBreakBuilder` | `PageBreakNode` | `name`, `margin` | That's the entire surface |
| `RichText` | `List<InlineRun>` | `text(String)` (static seed), `plain/bold/italic/underline/strikethrough`, `color/accent/size`, `link(text, uri)`, `image(...)` | Pass either a built `RichText` or a `Consumer<RichText>` to `ParagraphBuilder.rich(...)` |
| `Transformable<T>` | mixin (currently only `ShapeContainerBuilder`) | `rotate(deg)`, `scale(uniform)`, `scale(sx, sy)` | Render-only; layout snapshot stays deterministic |

---

## 4. Style types

| Type | Factories | Gotcha |
|---|---|---|
| `DocumentColor` | `BLACK / WHITE / GRAY / DARK_GRAY / LIGHT_GRAY / ROYAL_BLUE / ORANGE`, `of(Color)`, `rgb(r, g, b)` | In a themed template, **always** route through `theme.palette().*` instead of constants |
| `DocumentStroke` | `of(color)` (1pt), `of(color, width)` | Width must be finite & non-negative |
| `DocumentInsets` | `zero()`, `of(value)`, `symmetric(v, h)`, `top(v)`, `bottom(v)`, `left(v)`, `right(v)` | Constructor order is **TRBL**, not CSS shorthand |
| `DocumentTextStyle` | `DEFAULT`, `builder().fontName().size().decoration().color().build()`, `withSize(...)`, `withColor(...)` | Default font is `FontName.HELVETICA`, default size is **14pt** if 0/negative |
| `DocumentCornerRadius` | `ZERO`, `of(r)`, `of(tl,tr,br,bl)`, `right(r)`, `left(r)`, `top(r)`, `bottom(r)`, `.isUniform()`, `.isZero()` | Order is **TL, TR, BR, BL**. Render-only; clamped to half of smaller side |
| `DocumentTextDecoration` | `DEFAULT`, `BOLD`, `ITALIC`, `BOLD_ITALIC`, `UNDERLINE`, `STRIKETHROUGH` | — |
| `ClipPolicy` | `CLIP_BOUNDS`, `CLIP_PATH`, `OVERFLOW_VISIBLE` | DOCX backend ignores `CLIP_PATH` and emits a one-time capability warning |
| `ShapeOutline` | `Rectangle(w,h)`, `RoundedRectangle(w,h,r)`, `Ellipse(w,h)`, `circle(d)` | All dimensions must be positive |
| `DocumentTransform` | `NONE`, `rotate(deg)`, `scale(u)`, `scale(sx,sy)`, `withRotation`, `withScale`, `.isIdentity()` | Scale must be non-zero. Rotation is around placement centre |

---

## 5. Theme system

### Palette slots → role

| Slot | Where it goes |
|---|---|
| `palette().primary()` | Document title (`h1` colour), brand accents on tables/headers |
| `palette().accent()` | Status keywords (RichText), border strips (`accentLeft/Right/...`), badges, total-row borders |
| `palette().surface()` | Page background fallback, default table cell fill |
| `palette().surfaceMuted()` | Hero `softPanel` fill, table header / total / zebra fill |
| `palette().textPrimary()` | Body text colour, `h3` heading |
| `palette().textMuted()` | Captions, metadata |
| `palette().rule()` | Table cell borders, divider lines, accent strip background |

### Text scale → reach for these (don't rebuild styles)

| Slot | Use for |
|---|---|
| `text().h1()` | Document title (one per page-flow) |
| `text().h2()` | Section heading |
| `text().h3()` | Sub-heading inside a section |
| `text().body()` | Default paragraph |
| `text().caption()` | Captions, metadata, footer notes |
| `text().label()` | Form labels, table header text |
| `text().accent()` | Inline accent (status keyword via RichText) |

### Table preset slots

| Preset slot | Apply via |
|---|---|
| `table().defaultCellStyle()` | `TableBuilder.defaultCellStyle(style)` |
| `table().headerStyle()` | `TableBuilder.headerStyle(style)` |
| `table().totalRowStyle()` | `TableBuilder.totalRow(style, ...)` |
| `table().zebraStyle()` | `TableBuilder.zebra(odd, even)` (use `surface()` as the "odd" if you want a clean alternate) |

### Built-in themes

- `BusinessTheme.classic()` — crisp blue + white, no page background
- `BusinessTheme.modern()` — cream paper + teal/gold, `pageBackground = palette.surface()`
- `BusinessTheme.executive()` — graphite + warm accent, no page background

`BusinessTheme.withPageBackground(color)` and `withName(name)` give
you cheap forks. To brand for your project, hand-build a
`BusinessTheme(...)` once — see
[`CustomBusinessThemeExample`](../examples/src/main/java/com/demcha/examples/CustomBusinessThemeExample.java).

### Bridging to the legacy `CvTheme`

```java
CvTheme cvTheme = CvTheme.fromBusinessTheme(theme);   // ADR 0002
```

Routes the business palette + text scale into the older CV record so a
single theme drives both invoice / proposal templates and the legacy
CV gallery without redeclaring colours.

---

## 6. Golden patterns (copy these)

### 6.1 Theme-driven hero with right-rounded soft panel + left accent stripe

```java
.addSection("Hero", hero -> hero
    .softPanel(theme.palette().surfaceMuted(), DocumentCornerRadius.right(10), 14)
    .accentLeft(theme.palette().accent(), 4)
    .spacing(6)
    .addParagraph(p -> p.text(spec.title()).textStyle(theme.text().h1()))
    .addParagraph(p -> p.text(spec.subtitle()).textStyle(theme.text().caption())))
```

The soft panel sits flush against the accent stripe — round only the
**right** corners so the join is clean.

### 6.2 Address blocks with `lineSpacing(1.3)`

```java
.addParagraph(p -> p.text(joinAddress(party))
    .textStyle(theme.text().body())
    .lineSpacing(1.3)               // default 1.0 squashes \n-joined lines
    .margin(DocumentInsets.zero()))
```

### 6.3 Two-column block via `addRow` with weights

```java
.addRow("Parties", row -> row
    .spacing(18)
    .weights(1, 1)
    .addSection("From",   col -> col.addParagraph(...))
    .addSection("BillTo", col -> col.addParagraph(...)))
```

`weights` distributes the row's width; `addSection` is the canonical
"column". Don't try to put a `TableNode` here — `RowBuilder` rejects
it at `add()` time.

### 6.4 Themed table with header + zebra + repeating header

```java
.addTable(t -> {
    TableBuilder configured = t
        .columns(DocumentTableColumn.auto(), DocumentTableColumn.fixed(54))
        .defaultCellStyle(borderedCell)
        .headerRow("Description", "Qty")
        .headerStyle(headerStyle)
        .repeatHeader()                                // re-emit on each continuation page
        .zebra(theme.palette().surfaceMuted(), theme.palette().surface());

    spec.lineItems().forEach(item -> configured.row(item.description(), item.qty()));
    configured.totalRow(totalStyle, "Total", spec.total());
})
```

### 6.5 Inline accent via `RichText`

```java
.addParagraph(p -> p.rich(rich -> rich
    .plain("Status ")
    .accent(spec.status(), theme.palette().accent())))
```

Highlights one keyword inline — no extra `ParagraphBuilder` and no
manual style juggling.

### 6.6 Per-side accent strips around a header

```java
section
    .accentLeft(theme.palette().accent(), 4)     // left edge stripe
    .accentBottom(theme.palette().rule(), 1);    // thin rule under the header
```

---

## 7. Anti-patterns (don't do these)

1. **No raw `new Color(...)` or `DocumentColor.rgb(...)` in a themed template.** Route through `theme.palette().*` so a theme switch actually re-skins.
2. **No re-built `DocumentTextStyle.builder()` for `h1` / `h2` / `body`.** Use `theme.text().h1()` etc. — that's literally what the text scale is for.
3. **No `RowBuilder.gap()`** — it's `@Deprecated(since="1.5.0")`. Use `spacing(...)` so vertical and horizontal flows share a verb.
4. **Don't put `RowNode` or `TableNode` inside a `RowBuilder`.** Throws `IllegalArgumentException` at `add()` time. Use `addSection(...)` for "column-with-vertical-stack-of-stuff", and put tables in the surrounding flow.
5. **Don't call no-arg `buildPdf()` if the session was created via `GraphCompose.document()` (no path).** Throws `IllegalStateException`. Use `buildPdf(Path)`, `writePdf(OutputStream)`, or `toPdfBytes()`.
6. **Don't import `com.demcha.compose.engine.*` from a template.** Enforced by `PublicApiNoEngineLeakTest`. Everything you need lives under `com.demcha.compose.document.*`.
7. **Don't construct a `ShapeContainerBuilder` without an outline.** `build()` throws. Always call `rectangle/roundedRect/ellipse/circle(...)` first.
8. **Don't forget the trailing `.build()` on `pageFlow()`.** Without it the root never attaches to the session.
9. **Don't mix `pageFlow(Consumer)` and `pageFlow().build()` for the same root.** Pick one ergonomics style and stick with it within a template.
10. **Don't compose a hero around a `LayerStack` without giving it a fixed size.** Layer stacks are atomic — they need either an explicit `padding`/`margin` plus a sized `back(...)` layer, or a `ShapeContainerBuilder` outline driving the bounds.

---

## 8. New-template skeleton

```java
package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.theme.BusinessTheme;

import java.util.Objects;

/**
 * Starter template — copy and rename.
 *
 * Pattern: theme injected via constructor, every visible token comes
 * from {@code theme.palette()}, {@code theme.text()}, or
 * {@code theme.table()}. The same template renders in any
 * {@code BusinessTheme} without code changes.
 */
public final class StatusReportTemplateV1 implements StatusReportTemplate {

    private final BusinessTheme theme;

    public StatusReportTemplateV1() {
        this(BusinessTheme.modern());
    }

    public StatusReportTemplateV1(BusinessTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    @Override public String getTemplateId()   { return "status-report-v1"; }
    @Override public String getTemplateName() { return "Status Report V1"; }

    @Override
    public void compose(DocumentSession document, StatusReportSpec spec) {
        DocumentColor accent       = theme.palette().accent();
        DocumentColor surfaceMuted = theme.palette().surfaceMuted();
        DocumentColor rule         = theme.palette().rule();

        DocumentTableStyle borderedCell = DocumentTableStyle.builder()
                .stroke(DocumentStroke.of(rule, 0.6))
                .padding(DocumentInsets.of(7))
                .build();
        DocumentTableStyle headerStyle = DocumentTableStyle.builder()
                .fillColor(theme.palette().primary())
                .stroke(DocumentStroke.of(rule, 0.6))
                .padding(DocumentInsets.of(8))
                .textStyle(theme.text().label().withColor(theme.palette().surface()))
                .build();
        DocumentTableStyle totalStyle = DocumentTableStyle.builder()
                .fillColor(surfaceMuted)
                .stroke(DocumentStroke.of(rule, 0.6))
                .textStyle(theme.text().label())
                .build();

        document.pageFlow()
                .name("StatusReportRoot")
                .spacing(14)

                // Hero — right-rounded soft panel + left accent stripe
                .addSection("Hero", hero -> hero
                        .softPanel(surfaceMuted, DocumentCornerRadius.right(10), 14)
                        .accentLeft(accent, 4)
                        .spacing(6)
                        .addParagraph(p -> p.text(spec.title()).textStyle(theme.text().h1()))
                        .addParagraph(p -> p.text(spec.subtitle()).textStyle(theme.text().caption())))

                // Summary section
                .addSection("Summary", section -> section
                        .spacing(4)
                        .addParagraph(p -> p.text("Summary").textStyle(theme.text().h2()))
                        .addParagraph(p -> p.text(spec.summary())
                                .textStyle(theme.text().body())
                                .lineSpacing(1.3)))

                // Metrics table — themed header + zebra + total row + repeating header
                .addTable(table -> {
                    TableBuilder configured = table
                            .name("Metrics")
                            .columns(DocumentTableColumn.auto(), DocumentTableColumn.fixed(96))
                            .defaultCellStyle(borderedCell)
                            .headerRow("Metric", "Value")
                            .headerStyle(headerStyle)
                            .repeatHeader()
                            .zebra(surfaceMuted, theme.palette().surface());
                    spec.metrics().forEach(m -> configured.row(m.name(), m.value()));
                    configured.totalRow(totalStyle, "Total", spec.total());
                })
                .build();
    }
}
```

Render the template via:

```java
BusinessTheme theme = BusinessTheme.modern();
StatusReportTemplate template = new StatusReportTemplateV1(theme);

try (DocumentSession document = GraphCompose.document(Path.of("status.pdf"))
        .pageSize(DocumentPageSize.A4)
        .pageBackground(theme.pageBackground())
        .margin(28, 28, 28, 28)
        .create()) {
    template.compose(document, spec);
    document.buildPdf();
}
```

---

## 9. Template testing pattern

A new template gets two test layers:

```java
class StatusReportTemplateV1Test {

    // 1. Layout snapshot — deterministic, machine-stable JSON.
    @Test
    void pinsLayoutForModernTheme() throws Exception {
        try (DocumentSession document = newSession(BusinessTheme.modern())) {
            new StatusReportTemplateV1(BusinessTheme.modern()).compose(document, sample());
            LayoutSnapshotAssertions.assertMatches(document,
                    "templates/status-report/v1_modern");
        }
    }

    // 2. Visual PDF — the file a reviewer opens.
    @Test
    void rendersValidPdfForModernTheme() throws Exception {
        Path output = VisualTestOutputs.preparePdf("status-report-modern", "status-report-v1");
        try (DocumentSession document = newSession(BusinessTheme.modern())) {
            new StatusReportTemplateV1(BusinessTheme.modern()).compose(document, sample());
            Files.write(output, document.toPdfBytes());
        }
        byte[] bytes = Files.readAllBytes(output);
        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 5, US_ASCII)).isEqualTo("%PDF-");
    }
}
```

Pin one snapshot per supported theme. The first run writes the JSON
under `src/test/resources/layout-snapshots/...`; re-run with
`-Dgraphcompose.updateSnapshots=true` to accept a deliberate change.

PDFs land under `target/visual-tests/<test-folder>/<stem>.pdf`. They
are renderer-bytes checks — useful for catching crashes and quick
visual review, not as the regression contract (use snapshots for
that).

For the runnable end-to-end pattern see
[`InvoiceTemplateV2DemoTest`](../src/test/java/com/demcha/testing/visual/InvoiceTemplateV2DemoTest.java)
and
[`LayoutSnapshotRegressionExample`](../examples/src/main/java/com/demcha/examples/LayoutSnapshotRegressionExample.java).

---

## 10. Where to look next

| Need | File |
|---|---|
| First-time orientation | [`getting-started.md`](getting-started.md) |
| Theme deep-dive (palette, scales, bridge) | [`recipes/themes.md`](recipes/themes.md) |
| Tables (row span, zebra, totals, repeat header) | [`recipes/tables.md`](recipes/tables.md) |
| Shapes-as-containers, transforms, z-index | [`recipes/shape-as-container.md`](recipes/shape-as-container.md) + [`recipes/transforms.md`](recipes/transforms.md) |
| Adding a new node / builder / backend | [`extension-guide.md`](extension-guide.md) + [`recipes/extending.md`](recipes/extending.md) |
| Streaming output to HTTP / S3 | [`recipes/streaming.md`](recipes/streaming.md) |
| Snapshot-based regression workflow | [`recipes/extending.md` § 4](recipes/extending.md#4-validate-a-custom-nodes-layout-via-snapshots) |
| Migrating from v1.4 to v1.5 | [`migration-v1-4-to-v1-5.md`](migration-v1-4-to-v1-5.md) |
| Architecture decision records | [`adr/`](adr/) |
| Reference V2 templates | [`InvoiceTemplateV2`](../src/main/java/com/demcha/compose/document/templates/builtins/InvoiceTemplateV2.java), [`ProposalTemplateV2`](../src/main/java/com/demcha/compose/document/templates/builtins/ProposalTemplateV2.java) |
| Hand-built theme example | [`CustomBusinessThemeExample`](../examples/src/main/java/com/demcha/examples/CustomBusinessThemeExample.java) |
