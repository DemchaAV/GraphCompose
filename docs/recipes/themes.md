# Themes

`BusinessTheme` bundles a `DocumentPalette`, `SpacingScale`,
`TextScale`, `TablePreset`, and an optional page background, so
invoice / proposal / report templates rendered through the same
theme look like one product instead of three independently styled
documents.

## Pick a built-in theme

Three built-ins ship with the canonical surface:

```java
import com.demcha.compose.document.theme.BusinessTheme;

BusinessTheme classic   = BusinessTheme.classic();    // crisp blue + white
BusinessTheme modern    = BusinessTheme.modern();     // cream paper + teal/gold
BusinessTheme executive = BusinessTheme.executive();  // graphite + warm accent
```

Each theme exposes a palette, a text scale, a table preset, a
spacing scale, and an optional page background:

```java
document.pageFlow(page -> page
        .addSection("Hero", section -> section
                .softPanel(theme.palette().surfaceMuted(), 10, 14)
                .accentLeft(theme.palette().accent(), 4)
                .addParagraph(p -> p
                        .text("GraphCompose")
                        .textStyle(theme.text().h1()))
                .addParagraph(p -> p
                        .text("A theme-driven hero section.")
                        .textStyle(theme.text().body()))));
```

## Apply the page background

`BusinessTheme.modern()` ships with a cream `pageBackground()`. Apply
it on the document builder so the entire page paints with the
theme's paper colour rather than pure white:

```java
try (DocumentSession document = GraphCompose.document(Path.of("output.pdf"))
        .pageBackground(theme.pageBackground())
        .margin(28, 28, 28, 28)
        .create()) {
    // ...
    document.buildPdf();
}
```

## Pick the right palette colour by role

Reach for the named slots on `theme.palette()` instead of hard-coding
hex values — this keeps the document consistent with the rest of the
theme:

| Slot | Typical use |
| --- | --- |
| `palette().accent()` | Brand accent on borders, badges, total rows |
| `palette().surfaceMuted()` | Soft section backgrounds (`softPanel(...)`) |
| `palette().textPrimary()` | Body copy |
| `palette().textMuted()` | Captions, metadata |

The exact RGB values are an implementation detail of each theme — the
slot names stay stable across future tweaks.

## Reusable text styles via the text scale

`theme.text()` returns a `TextScale` with named slots (`h1`, `h2`,
`bodyBold`, `body`, `caption`). Use them instead of hand-rolling
`DocumentTextStyle.builder()...` calls so your headings stay
consistent.

```java
.addParagraph(p -> p.text("Quarterly summary").textStyle(theme.text().h2()))
.addParagraph(p -> p.text("Generated " + date).textStyle(theme.text().caption()))
```

## Layered themes for invoices and proposals

The built-in `InvoiceTemplateV1` is hard-coded to a default theme via
`BusinessDocumentSceneStyles`. To render the same business data with
a different theme, write a small custom composer that takes a
`BusinessTheme` parameter and uses the theme's palette / text scale
instead of the static styles. A future
`InvoiceTemplateV2(BusinessTheme)` will lift this opt-in to the
canonical templates.

## See also

- [Shape-as-container](shape-as-container.md) — themed circles + cards.
- [Tables](tables.md) — the table style overrides used by
  `headerStyle(...)`, `totalRow(...)`, and `zebra(...)` accept full
  `DocumentTableStyle` values, so they pick up theme palettes.
- [`docs/canonical-legacy-parity.md`](../canonical-legacy-parity.md) —
  the parity matrix lists every theme-aware style override that
  v1.5 supports.
