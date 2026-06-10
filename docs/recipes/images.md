# Images: sources, sizing, and fit modes

Images go through `addImage` on `AbstractFlowBuilder` (so they work in
`pageFlow`, `module`, `section`, and row cells) and through
`ImageBuilder` when you build the node yourself. The source descriptor
is `DocumentImageData` — a small value type that keeps authoring code
independent from the image cache and the PDF backend.

## Sources: path or bytes

```java
import com.demcha.compose.document.image.DocumentImageData;

import java.nio.file.Path;

image.source(Path.of("assets/logo.png"));          // filesystem Path
image.source("assets/logo.png");                   // path string
image.source(readBytesFromAnywhere());             // in-memory byte[]
image.source(DocumentImageData.fromPath(path));    // explicit descriptor
```

`fromPath` normalizes to an absolute path so adapters resolve it
deterministically; `fromBytes` defensively copies. There is no URL
source — download the bytes yourself and pass them through
`source(byte[])`.

## Sizing: explicit, scaled, or natural

Dimension resolution follows a strict precedence:

1. `size(w, h)` — exact box; `fitMode` decides how the image fills it.
2. `width(w)` *or* `height(h)` alone — the other side comes from the
   image's intrinsic aspect ratio.
3. `scale(s)` — natural pixel size multiplied uniformly (only applies
   when width and height are both omitted).
4. Nothing — the image draws at its natural pixel size in points.

In every case the result is clamped to the available content width,
preserving aspect ratio, so an oversized photo never overflows its
column.

```java
page.addImage(image -> image.source(logo).size(96, 48))   // exact box
    .addImage(image -> image.source(logo).width(120))     // height from ratio
    .addImage(image -> image.source(photo).scale(0.5));   // half natural size
```

## Fit modes

`DocumentImageFitMode` controls drawing inside an explicit box:
`STRETCH` (default) fills it exactly, `CONTAIN` letterboxes the whole
image inside, `COVER` fills and crops the overflow.
`fitToBounds(w, h)` is the common shortcut — it sets the box *and*
switches to `CONTAIN`:

```java
import com.demcha.compose.document.image.DocumentImageFitMode;

page.addImage(image -> image
                .name("Logo")
                .source(Path.of("assets/logo.png"))
                .fitToBounds(96, 48))                      // CONTAIN
    .addImage(image -> image
                .name("Avatar")
                .source(Path.of("assets/avatar.png"))
                .size(48, 48)
                .fitMode(DocumentImageFitMode.COVER));     // fill + crop
```

## Images inside rows and cards

An image is an ordinary block node, so the classic "photo card" is a
filled section in a weighted row:

```java
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentStroke;

page.addRow("Gallery", row -> row
        .spacing(14)
        .weights(1, 1)
        .addSection("PhotoCard", card -> card
                .fillColor(DocumentColor.rgb(230, 235, 255))
                .stroke(DocumentStroke.of(DocumentColor.rgb(180, 193, 226), 0.7))
                .cornerRadius(7)
                .addImage(image -> image
                        .source(photoFile)
                        .fitToBounds(250, 134)
                        .fitMode(DocumentImageFitMode.COVER)))
        .addSection("Caption", caption -> caption
                .addParagraph(p -> p.text("Riverside site, week 14."))));
```

## Inline images in paragraphs

For an icon or logo flowing *with* text, use
`ParagraphBuilder.inlineImage(data, width, height)` — the run is
measured on the same baseline as the surrounding text and defaults to
`InlineImageAlignment.CENTER`; overloads add explicit alignment, a
baseline offset, and link metadata:

```java
section.addParagraph(p -> p
        .inlineText("Built with ")
        .inlineImage(DocumentImageData.fromPath("assets/logo.png"), 12, 12)
        .inlineText(" GraphCompose."));
```

## Links and bookmarks

`ImageBuilder.link(DocumentLinkOptions)` makes the whole image a
clickable area; `bookmark(DocumentBookmarkOptions)` adds it to the PDF
outline. Both are covered end-to-end in the
[PDF chrome recipe](pdf-chrome.md).

## See also

- [Shapes](shapes.md) — `addImage` alongside the other visual
  primitives, plus the CONTAIN vs COVER comparison.
- [Shape-as-container](shape-as-container.md) — clip an image to a
  circle or rounded card.

Runnable usage in a real document:
[`CinematicProposalFileExample`](../../examples/src/main/java/com/demcha/examples/templates/proposal/CinematicProposalFileExample.java)
([rendered PDF](../../assets/readme/examples/project-proposal-cinematic.pdf))
— a `COVER`-fitted photo inside a rounded, stroked card.
