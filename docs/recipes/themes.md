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

`InvoiceTemplateV2` and `ProposalTemplateV2` (Phase E.1 / E.2) take a
`BusinessTheme` in their constructor:

```java
import com.demcha.compose.document.templates.builtins.InvoiceTemplateV2;
import com.demcha.compose.document.theme.BusinessTheme;

InvoiceTemplateV2 invoice = new InvoiceTemplateV2(BusinessTheme.modern());
ProposalTemplateV2 proposal = new ProposalTemplateV2(BusinessTheme.modern());
```

The same business data (`InvoiceDocumentSpec`,
`ProposalDocumentSpec`) renders in any of the three built-in themes
just by passing the theme to the constructor — no need to refactor
the call sites.

The earlier `InvoiceTemplateV1` is hard-coded to a default theme via
the static `BusinessDocumentSceneStyles`. Both V1 and V2 ship side by
side; V2 is the cinematic theme-driven path.

## Sharing themes with CV templates

`CvTheme` is the legacy theme type used by the CV gallery
(`CvTemplateV1` and the ten visual variants). It carries the same
visual concerns as `BusinessTheme` but with CV-specific accessor
names (`nameTextStyle`, `sectionHeaderTextStyle`, `bodyTextStyle`).

To keep a CV and a business document visually consistent without
re-stating colours and fonts, derive the `CvTheme` from your chosen
`BusinessTheme`:

```java
import com.demcha.compose.document.templates.theme.CvTheme;

BusinessTheme theme = BusinessTheme.modern();
CvTheme cvTheme = CvTheme.fromBusinessTheme(theme);

// invoice + proposal both use `theme`
// CV uses `cvTheme`
```

The bridge maps the business palette / text-scale slots into the
CV-specific tokens (`primaryColor`, `accentColor`, `nameFontSize`,
etc.). See [ADR 0002 — Theme unification](../adr/0002-theme-unification.md)
for the mapping table and the rationale.

## See also

- [Shape-as-container](shape-as-container.md) — themed circles + cards.
- [Tables](tables.md) — the table style overrides used by
  `headerStyle(...)`, `totalRow(...)`, and `zebra(...)` accept full
  `DocumentTableStyle` values, so they pick up theme palettes.
- [`docs/canonical-legacy-parity.md`](../canonical-legacy-parity.md) —
  the parity matrix lists every theme-aware style override that
  v1.5 supports.
