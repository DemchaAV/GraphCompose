# Barcodes and QR codes

A barcode is a first-class canonical node: `addBarcode` lives on
`AbstractFlowBuilder`, so codes drop into `pageFlow`, `module`, and
`section` containers — and combine with rows, cards, layer stacks, and
tables — like any other block. The builder is `BarcodeBuilder`; the
result is a `BarcodeNode` carrying `DocumentBarcodeOptions`.

## Quick start: a QR code

```java
document.pageFlow()
        .addBarcode(barcode -> barcode
                .name("RepoQr")
                .qrCode()
                .data("https://github.com/DemchaAV/GraphCompose")
                .size(150, 150))
        .build();
```

`data(...)` sets the encoded content; `size(width, height)` (or the
separate `width(...)` / `height(...)` setters) fixes the drawn box in
points.

## Symbologies

Five formats have fluent shortcuts — each is a single call:

```java
barcode.qrCode();      // QR code (2D)
barcode.code128();     // Code 128 — invoice numbers, logistics ids
barcode.code39();      // Code 39 — alphanumeric asset tags
barcode.ean13();       // EAN-13 — 13-digit retail
barcode.ean8();        // EAN-8 — 8-digit compact retail
```

The full `DocumentBarcodeType` enum also covers `UPC_A`, `PDF_417`,
and `DATA_MATRIX` — select those through the generic setter:

```java
import com.demcha.compose.document.node.DocumentBarcodeType;

barcode.type(DocumentBarcodeType.DATA_MATRIX)
        .data("LOT-2026-04-001");
```

## Brand tinting and quiet zone

Foreground and background take any `DocumentColor`, so a QR code can
match the document palette instead of shipping in black-on-white.
`quietZone(n)` adds the symbology's blank margin, measured in barcode
modules:

```java
import com.demcha.compose.document.style.DocumentColor;

section.addBarcode(barcode -> barcode
        .qrCode()
        .data("https://demcha.io/graphcompose")
        .foreground(DocumentColor.rgb(20, 80, 95))      // brand teal
        .background(DocumentColor.rgb(232, 244, 245))   // soft tint
        .quietZone(2)
        .size(150, 150));
```

Keep the foreground much darker than the background — scanners need
the contrast.

## Clickable codes

A barcode accepts the same link and bookmark metadata as paragraphs
and images, so the printed QR can double as a clickable area in the
PDF viewer:

```java
import com.demcha.compose.document.node.DocumentLinkOptions;

barcode.qrCode()
        .data("https://github.com/DemchaAV/GraphCompose")
        .link(new DocumentLinkOptions("https://github.com/DemchaAV/GraphCompose"))
        .size(150, 150);
```

## Centring a barcode in a card

Barcodes have fixed sizes, so inside a card they sit at the leading
edge by default. Wrap the built node in a shape container sized to the
card's content width with `CENTER` alignment — `OVERFLOW_VISIBLE`
skips clipping (see the
[shape-as-container recipe](shape-as-container.md)):

```java
import com.demcha.compose.document.dsl.BarcodeBuilder;
import com.demcha.compose.document.node.BarcodeNode;
import com.demcha.compose.document.style.ClipPolicy;

BarcodeNode code = new BarcodeBuilder()
        .ean13()
        .data("5901234123457")
        .size(190, 90)
        .build();

section
        .softPanel(DocumentColor.WHITE, 8, 14)
        .addContainer(card -> card
                .name("Ean13_Center")
                .rectangle(228, code.height())   // card content width
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .center(code));
```

The container's rectangle spans the available slot; the barcode lands
at the visual midline regardless of its natural aspect ratio.

## See also

- [Shape-as-container](shape-as-container.md) — the centring pattern
  above, plus clipped circles and rounded cards.
- [PDF chrome](pdf-chrome.md) — links and bookmarks on any node,
  including barcodes.

Runnable showcase:
[`BarcodeShowcaseExample`](../../examples/src/main/java/com/demcha/examples/features/barcodes/BarcodeShowcaseExample.java)
([rendered PDF](../../assets/readme/examples/barcode-showcase.pdf)) —
all five fluent symbologies plus a brand-tinted QR, each centred in a
two-column card grid.
