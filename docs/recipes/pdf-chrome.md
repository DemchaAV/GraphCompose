# PDF chrome: metadata, watermarks, headers/footers, protection

Everything *around* the body content — document properties, page-wide
watermarks, running headers and footers, encryption, clickable links,
and the outline panel — is configured through backend-neutral value
types in `com.demcha.compose.document.output` and applied with
`DocumentSession` mutators: `metadata`, `watermark`, `header`,
`footer`, `protect`. Backends that cannot honour a surface ignore it
(DOCX honours metadata, skips watermark/header/footer/bookmarks).

## Document metadata

```java
import com.demcha.compose.document.output.DocumentMetadata;

document.metadata(DocumentMetadata.builder()
        .title("Q2 business report")
        .author("Jordan Rivera")
        .subject("Quarterly performance summary")
        .keywords("graphcompose, report, q2")
        .creator("GraphCompose Examples")   // defaults to "GraphCompose"
        .producer("GraphCompose")
        .build());
```

The values land in the PDF information dictionary — visible under
*Document Properties* in any viewer.

## Watermark

A watermark paints on every page, text- or image-based:

```java
import com.demcha.compose.document.output.DocumentWatermark;
import com.demcha.compose.document.output.DocumentWatermarkLayer;
import com.demcha.compose.document.output.DocumentWatermarkPosition;

document.watermark(DocumentWatermark.builder()
        .text("DRAFT")
        .fontSize(96f)
        .rotation(45f)
        .color(DocumentColor.rgb(196, 153, 76))
        .opacity(0.12f)
        .layer(DocumentWatermarkLayer.BEHIND_CONTENT)   // or ABOVE_CONTENT
        .position(DocumentWatermarkPosition.CENTER)
        .build());
```

- **Layer** — `BEHIND_CONTENT` sits under the body; `ABOVE_CONTENT`
  paints on top of it.
- **Position** — `CENTER`, the four corners (`TOP_LEFT` …
  `BOTTOM_RIGHT`), or `TILE` for a repeated pattern across the page.
- **Image mode** — set `imagePath(...)` or `imageBytes(...)` instead
  of `text(...)` to stamp a logo.

## Running header and footer

Header and footer share the `DocumentHeaderFooter` type, targeted by
`zone`. Each zone has independent left / center / right text slots,
and the text supports the placeholder tokens `{page}`, `{pages}`, and
`{date}`:

```java
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;

document.header(DocumentHeaderFooter.builder()
        .zone(DocumentHeaderFooterZone.HEADER)
        .leftText("GraphCompose · Chrome showcase")
        .rightText("{date}")
        .fontSize(9f)
        .textColor(DocumentColor.rgb(112, 116, 128))
        .showSeparator(true)                 // rule between chrome and body
        .separatorColor(DocumentColor.rgb(224, 224, 224))
        .separatorThickness(0.5f)
        .build());

document.footer(DocumentHeaderFooter.builder()
        .zone(DocumentHeaderFooterZone.FOOTER)
        .centerText("Page {page} of {pages}")
        .fontSize(9f)
        .showSeparator(true)
        .build());
```

The flagship `BusinessReportExample` uses exactly this footer in a
real document — `"Confidential and proprietary"` on the left,
`"Page {page} of {pages}"` on the right, with a 0.5pt separator rule.

## Protection (passwords and permissions)

```java
import com.demcha.compose.document.output.DocumentProtection;

document.protect(DocumentProtection.builder()
        .userPassword("preview")        // required to open
        .ownerPassword("change-me")     // required to change permissions
        .canPrint(true)
        .canCopyContent(false)
        .canModify(false)
        .canFillForms(true)
        .keyLength(128)
        .build());
```

Further toggles: `canExtractForAccessibility`, `canAssemble`,
`canPrintHighQuality`. The PDF backend maps these to PDFBox
encryption settings.

## Clickable links

`DocumentLinkOptions` is a one-field record holding a validated
absolute URI. Three levels of granularity:

```java
import com.demcha.compose.document.node.DocumentLinkOptions;

// 1. Whole paragraph as a link — the addLink shortcut:
section.addLink("Project repository", "https://github.com/DemchaAV/GraphCompose");

// 2. A link run inside a longer paragraph:
section.addParagraph(p -> p
        .inlineText("Full details in the ")
        .inlineLink("online docs", new DocumentLinkOptions("https://demcha.io/graphcompose"))
        .inlineText("."));

// 3. Node-level: images and barcodes take .link(...) on their builders.
```

## Outline bookmarks

`ParagraphBuilder.bookmark(...)` materialises a PDF outline entry —
the navigable side panel in most viewers. `DocumentBookmarkOptions`
takes a title and a nesting level (`0` is a root entry; the one-arg
constructor defaults to root):

```java
import com.demcha.compose.document.node.DocumentBookmarkOptions;

section.addParagraph(p -> p
        .text("1. Executive summary")
        .bookmark(new DocumentBookmarkOptions("Executive summary", 0)))
       .addParagraph(p -> p
        .text("1.1 Highlights")
        .bookmark(new DocumentBookmarkOptions("Highlights", 1)));
```

Image and barcode builders accept the same `bookmark(...)` metadata.

Runnable showcase:
[`PdfChromeExample`](../../examples/src/main/java/com/demcha/examples/features/chrome/PdfChromeExample.java)
([rendered PDF](../../assets/readme/examples/pdf-chrome.pdf)) —
metadata, a diagonal DRAFT watermark, bordered header/footer with
page tokens, and a three-level outline, all in one A4 page.
