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

### Reserving the zone's height

A zone's `height` positions its text; by default it takes nothing away from the
page's content area, so whether the body runs under the footer is decided by the
page margin. Ask for the space explicitly:

```java
document.footer(DocumentHeaderFooter.builder()
        .centerText("Page {page} of {pages}")
        .height(48f)
        .reserveSpace(true)
        .build());
```

The content area is inset to the **larger** of the page margin and the zone's
height — not their sum — so a zone that already fits inside the margin reserves
nothing and moves nothing. `DocumentSession.availableHeight()` reports the
reduced area, which is what a composition sizing itself against the page should
read.

Off by default, because turning it on can add pages to a document that was
relying on the overlap.

### Choosing the zone's font

A zone is typeset in `FontName.HELVETICA` unless it says otherwise. That
is standard-14 Helvetica, whose WinAnsi encoding has no Cyrillic, Greek,
Hebrew or Arabic — so a footer written in one of those scripts renders as
a row of `?`. Name the family the text needs:

```java
import com.demcha.compose.font.FontName;

document.footer(DocumentHeaderFooter.builder()
        .centerText("Стр. {page} из {pages}")
        .fontName(FontName.PT_SANS)
        .build());
```

There is no automatic fallback — not in a zone, and not in body text
either. The engine draws what the family you named can encode and
substitutes `?` for the rest, so the family has to cover the script.
Custom families registered on the session are resolved here too.

## A zone: nodes in the band, not text slots

`DocumentHeaderFooter` gives three text slots and three tokens. When the band
needs a badge, a link, a logo or a layout, register a **zone** instead: its
content is a node subtree, built per page from that page's `PageContext`.

```java
import com.demcha.compose.document.output.DocumentPageZone;

document.chrome().zone(DocumentPageZone.footer(36, page -> new RowBuilder()
        .gap(10)
        .addParagraph(p -> p.text("Confidential"))
        .flexSpacer()
        .addParagraph(p -> p.inlineChip("v2.4", ink, fill))
        .addParagraph(p -> p.text(page.number() + " / " + page.total()))
        .build()));
```

The subtree goes through the same layout and render path as the body, so the
fonts, the bidi reordering, the inline chips and the link annotations are the
ones the body already gets — the backends need no zone-specific code, and the
PPTX deck carries the band for the same reason the PDF does.

There are no placeholder tokens here and none are needed: `page.number()` and
`page.total()` are the values, and roman numerals, an offset or a different line
on the last page are ordinary Java in the same lambda.

- **`appliesTo(page -> !page.isFirst())`** decides which pages carry the zone.
  It is a separate question from numbering, so "no number on the cover, keep the
  logo" is one zone with a predicate rather than two zones.
- **A zone reserves its height by default** — the opposite of
  `DocumentHeaderFooter`, which cannot, because it has to keep rendering
  documents written before the flag existed.
- **A zone does not paginate.** Content that needs more than the declared height
  raises `AtomicNodeTooLargeException` naming the zone, rather than silently
  losing half of what it was given.

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
