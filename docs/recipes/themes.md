# Themes

GraphCompose has two theming levels:

- **Direct DSL styling** — `DocumentColor`, `DocumentTextStyle`, and the
  page background, for documents you author yourself on the canonical
  DSL.
- **`BrandTheme`** — the template token bundle (a `Palette`, a
  `Typography`, a `Spacing`, and a `Decoration`) that every layered
  template family reads, so an invoice, a proposal, and a CV rendered
  through the same theme look like one product instead of three
  independently styled documents.

## Theme a template with `BrandTheme`

Each family ships default themes as static factories — the CV presets
each have a matching factory (`BrandTheme.boxedClassic()`,
`BrandTheme.nordicClean()`, `BrandTheme.executive()`, …), and the
invoice / proposal families ship `BrandTheme.invoiceModern()` and
`BrandTheme.proposalModern()`:

```java
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.invoice.presets.ModernInvoice;

BrandTheme theme = BrandTheme.invoiceModern();
DocumentTemplate<InvoiceDocumentSpec> template = ModernInvoice.create(theme);
```

The same data spec renders in any theme just by passing a different
`BrandTheme` to `create(...)` — no need to refactor call sites.

## Pick the right palette colour by role

`theme.palette()` exposes named colour slots. Reach for them instead of
hard-coding hex values — the slot names stay stable across theme tweaks:

| Slot | Typical use |
| --- | --- |
| `palette().ink()` | Body copy, headings |
| `palette().muted()` | Captions, metadata, secondary text |
| `palette().rule()` | Dividers, table strokes |
| `palette().banner()` | Section-header bands, accent fills |
| `palette().mainFill()` | The page background (`pageBackground(theme.palette().mainFill())`) |

## Customize one token bundle, keep the rest

`BrandTheme` is a record of four token records — swap any one and keep
the others:

```java
import com.demcha.compose.document.templates.core.theme.*;

BrandTheme base = BrandTheme.boxedClassic();
BrandTheme custom = new BrandTheme(
        base.palette(),
        base.typography(),
        base.spacing(),
        new Decoration("▶ ", "  ", "  ·  "));   // ▶ bullets, mid-dot separators
```

- `Palette` — the five colour slots above.
- `Typography` — headline / body fonts plus the point-size scale
  (`sizeHeadline`, `sizeBody`, `sizeEntryTitle`, …).
- `Spacing` — the vertical rhythm between sections and entries.
- `Decoration` — bullet glyph, stacked indent, contact separator.

## CV and cover-letter themes

CV presets pair with their theme factory by name; render one against a
`CvDocument` with the preset's `create()` factory:

```java
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.presets.ModernProfessional;

DocumentTemplate<CvDocument> cv = ModernProfessional.create();
cv.compose(session, cvDocument);
```

To restyle, pass a custom `BrandTheme` to the preset's `create(...)`
overload, or use its `Options` builder where one is provided (for
example `MintEditorial.Options.builder().headerBandColor(...).build()`
→ `MintEditorial.create(options)`). See
[authoring presets](../templates/v2-layered/authoring-presets.md) for
the per-preset customisation knobs and
[which template system?](../templates/which-template-system.md) for the
full preset list.

## Direct DSL styling

Authoring on the canonical DSL directly, define your colours and text
styles inline (or in a small constants class) and apply the page
background on the document builder so the entire page paints with your
paper colour rather than pure white:

```java
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

DocumentColor cream = DocumentColor.rgb(252, 248, 240);
DocumentColor panel = DocumentColor.rgb(244, 238, 228);
DocumentTextStyle h1 = DocumentTextStyle.builder()
        // The name picks the family, the decoration picks the face within it.
        .fontName(FontName.HELVETICA).size(28)
        .decoration(DocumentTextDecoration.BOLD)
        .color(DocumentColor.rgb(20, 60, 75)).build();

try (DocumentSession document = GraphCompose.document(Path.of("output.pdf"))
        .pageBackground(cream)
        .margin(28, 28, 28, 28)
        .create()) {
    document.pageFlow(page -> page
            .addSection("Hero", section -> section
                    .softPanel(panel, 10, 14)
                    .addParagraph(p -> p.text("GraphCompose").textStyle(h1))));
    document.buildPdf();
}
```

## See also

- [Shape-as-container](shape-as-container.md) — themed circles + cards.
- [Tables](tables.md) — the table style overrides used by
  `headerStyle(...)`, `totalRow(...)`, and `zebra(...)` accept full
  `DocumentTableStyle` values, so they pick up theme palettes.
- [Business templates](../templates/business-templates.md) — the
  invoice / proposal presets that consume `BrandTheme`.
