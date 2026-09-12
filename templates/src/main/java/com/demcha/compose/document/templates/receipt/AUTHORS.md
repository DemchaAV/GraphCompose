# Receipt family — recipe cookbook

Everything here builds on `ModernReceipt`, the reference preset. Each
recipe is a change you can make without forking a widget.

Read [`../../../../../../../../../docs/templates/v2-layered/authoring-presets.md`](../../../../../../../../../docs/templates/v2-layered/authoring-presets.md)
first if you have not — it explains why the layers look the way they do.

---

## 0. The shortest complete receipt

```java
ReceiptDocumentSpec spec = ReceiptDocumentSpec.of(receipt -> receipt
        .documentTitle("Transfer confirmation")
        .issuerName("Northwind Pay")
        .generatedOn("09 August 2026")
        .amount("Amount sent", "£66.62")
        .status(ReceiptStatus.settled("Completed"))
        .payer("Paid from", p -> p.name("Alex Sample").field("Account", "•••• 4396"))
        .beneficiary("Paid to", p -> p.name("Harbour Finance Ltd")));

BrandTheme theme = BrandTheme.receiptModern();
float m = (float) ModernReceipt.RECOMMENDED_MARGIN;
try (DocumentSession document = GraphCompose.document(out)
        .pageSize(DocumentPageSize.A4)
        .pageBackground(theme.palette().mainFill())
        .margin(m, m, m, m)
        .create()) {
    ModernReceipt.create(theme).compose(document, spec);
    document.buildPdf();
}
```

Every block you did not fill — detail groups, timeline, notes, QR code —
drops out rather than rendering an empty heading.

---

## 1. Brand it for a different issuer

The theme carries **no** brand colour. An issuer arrives through
`Options`:

```java
ModernReceipt.Options options = ModernReceipt.Options
        .branded(SvgGlyph.fromResource("/brand/northwind-pay-mark.svg"),
                 DocumentColor.rgb(23, 92, 211))
        .withLogoWidth(14)          // a symbol mark, sized to the name beside it
        .withLogoColor(DocumentColor.rgb(23, 92, 211));

ModernReceipt.create(BrandTheme.receiptModern(), options);
```

The accent reaches the hero's left strip, the direction arrow between the
parties, the reached step of the status trail, and an in-progress status
chip. Nothing else is tinted — which is what keeps the numbers readable.

**Wordmark instead of a symbol?** Leave `issuerName` blank in the data and
size the mark to its real width:

```java
ModernReceipt.Options.branded(SvgGlyph.fromFile(Path.of("brand/acme.svg")), accent)
        .withLogoWidth(78)
        .withLogoColor(DocumentColor.rgb(13, 15, 20));
```

`SvgGlyph` flattens the file's filled layers into one recolourable
silhouette, so a single logo file serves a dark mark on white and a light
one on a dark page.

---

## 2. Add a block the template has never heard of

A detail group is just a title and rows:

```java
receipt.detailGroup("Fees and charges", group -> group
        .field("Correspondent fee", "£3.50")
        .field("FX margin", "0.35%")
        .emphasized("Total charged", "£70.47"));
```

`emphasized` sets the value in the bold weight — use it for the one row per
group a reader looks for first. Groups render in the order you add them,
each keeping together across a page break.

---

## 3. Say what your payment system says

`ReceiptStatus` carries the issuer's own word plus the tone a template may
colour on:

```java
receipt.status("Being processed", ReceiptStatusTone.IN_PROGRESS);
receipt.status("Returned by the beneficiary bank", ReceiptStatusTone.ATTENTION);
receipt.status("Rejected", ReceiptStatusTone.FAILED);
```

Settled is green, attention amber, failed red, in-progress the issuer's
accent — in every theme. That is deliberate: a reader checking whether
money arrived should not have to learn a colour scheme per bank.

---

## 4. Show how the payment got there

```java
receipt.event("Instructed", "07 Jul 2026, 08:12 BST", "Submitted to the scheme.")
       .event("In clearing", "08 Jul 2026, 09:00 BST", "Three-working-day Bacs cycle.")
       .event("Settled", "09 Jul 2026, 06:30 BST", "Confirmed by the receiving bank.");
```

Steps render oldest first; the last one takes the accent dot, so the eye
lands on where the payment got to without reading a timestamp. Rename the
heading with `Options.withTimelineTitle("Where it got to")`, or pass no
events at all and the block disappears.

---

## 5. Make it verifiable

```java
receipt.verification("https://pay.example.com/v/NWP-4821-0067",
                     "Scan to check this confirmation against our records.")
       .supportLine("Support  +44 20 0000 0000")
       .supportLine("help@example.com")
       .legalNote("…registered in England and Wales…");
```

These three make up the footer, which the preset seats on the bottom
margin when the receipt owns the document. The QR code encodes the URL
verbatim — it is what makes a printed copy resolvable again.

---

## 6. Change the look without touching a widget

Every size, colour, and gap comes from the `BrandTheme`. To make a denser
receipt, build a theme from the shipped parts and swap the one you want:

```java
BrandTheme dense = new BrandTheme(
        Palette.receiptModern(),
        Typography.receiptModern(),
        Spacing.invoiceModern(),        // tighter block rhythm
        Decoration.classic());
ModernReceipt.create(dense, options);
```

The slots carry receipt-specific readings worth knowing:

| Token | On a receipt it is |
|---|---|
| `typography.sizeHeadline` | the hero **amount** — the largest thing on the page |
| `typography.sizeEntryTitle` | the document title and the party names |
| `typography.sizeEntryDate` | field values in the right-hand column |
| `typography.sizeBanner` | spaced-caps group headings and eyebrow labels |
| `spacing.accentRuleWidth` | the hairline rules, not a wide accent strip |
| `spacing.bannerCornerRadius` | the corner radius of the hero and party panels |

---

## 7. Put a receipt inside a bigger document

`compose` seats the footer on the bottom margin by re-composing once — and
that clears the session, so it only happens when the session was empty
when you called it. Compose a cover page first and the receipt still
renders correctly; its footer simply flows after the body instead of
pinning. The page-number chrome is installed either way, and a caller who
wants different chrome sets it after composing.
