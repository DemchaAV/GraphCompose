# Shapes and visual primitives

The canonical DSL ships ergonomic builders for the most common
non-text visual blocks: filled cards, dividers, accent bars, lines,
spacers, ellipses, and images. Each builder lives on
`AbstractFlowBuilder`, so it works inside `pageFlow`, `module`, and
`section` containers without any extra plumbing.

For clipped *containers* (a circle that holds layered children, an
ellipse with overlay, a rounded card whose contents are clipped to
the outline) reach for the dedicated
[shape-as-container recipe](shape-as-container.md) — that page covers
`addCircle`, `addEllipse`, `addContainer`, and the `ClipPolicy` modes
introduced in v1.5.

## Filled card with rounded corners

A filled `addSection` is the canonical "card" primitive. Combine
`fillColor`, `cornerRadius`, `padding`, and `stroke` to get the
classic info-card look:

```java
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;

document.pageFlow(page -> page
        .spacing(12)
        .addSection("InfoCard", card -> card
                .fillColor(DocumentColor.rgb(245, 248, 255))
                .stroke(DocumentStroke.of(DocumentColor.ROYAL_BLUE, 0.8))
                .cornerRadius(10)
                .padding(DocumentInsets.of(12))
                .margin(DocumentInsets.bottom(10))
                .addParagraph(paragraph -> paragraph
                        .text("Block text inside a filled rounded card.")
                        .textStyle(DocumentTextStyle.DEFAULT)
                        .lineSpacing(2))));
```

For per-corner radii (e.g. round only the right side of a hero strip),
use `DocumentCornerRadius.right(10)` / `top(10)` / etc. — the v1.5
extension covers four independent corner radii. See
[themes recipe](themes.md) for the matching `softPanel(color, radius,
padding)` shortcut.

## Divider and accent shape

A short rule plus a thin coloured bar is a common visual punctuation
inside a module:

```java
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;

document.pageFlow(page -> page
        .module("Visual Blocks", module -> module
                .divider(divider -> divider
                        .width(220)
                        .thickness(3)
                        .color(DocumentColor.ROYAL_BLUE)
                        .padding(DocumentInsets.of(6)))
                .addShape(shape -> shape
                        .name("Accent")
                        .size(3, 90)
                        .fillColor(DocumentColor.ORANGE)
                        .padding(DocumentInsets.of(6)))));
```

## Spacer, line, and circle

`addSpacer`, `addLine`, and `addEllipse` cover the small primitives
you reach for when a layout needs breathing room or a brand mark:

```java
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentStroke;

document.pageFlow(page -> page
        .name("VisualPrimitives")
        .spacing(8)
        .addSpacer(spacer -> spacer.name("Gap").height(12))
        .addLine(line -> line
                .name("Rule")
                .horizontal(180)
                .thickness(2)
                .color(DocumentColor.ROYAL_BLUE))
        .addEllipse(ellipse -> ellipse
                .name("Badge")
                .circle(24)
                .fillColor(DocumentColor.ORANGE)
                .stroke(DocumentStroke.of(DocumentColor.BLACK, 0.5))));
```

`addEllipse(diameter, fill)` and `addCircle(diameter, fill)` are the
v1.5 convenience overloads when you only need a single coloured
disc — perfect for status dots and icon backgrounds.

## Image fit modes

Images go through `addImage` with explicit bounds and a
`DocumentImageFitMode`. `CONTAIN` keeps the entire image inside the
bounds (letterboxed when the aspect ratio differs); `COVER` fills the
bounds and crops the overflow:

```java
import com.demcha.compose.document.image.DocumentImageFitMode;

import java.nio.file.Path;

document.pageFlow(page -> page
        .name("ImageFit")
        .addImage(image -> image
                .name("Logo")
                .source(Path.of("assets/logo.png"))
                .fitToBounds(96, 48)
                .fitMode(DocumentImageFitMode.CONTAIN))
        .addImage(image -> image
                .name("Avatar")
                .source(Path.of("assets/avatar.png"))
                .fitToBounds(48, 48)
                .fitMode(DocumentImageFitMode.COVER)));
```

## Soft panel from a theme

When you have a `BusinessTheme`, prefer `softPanel(color, radius,
padding)` over hand-rolled `fillColor` + `cornerRadius` + `padding`:
it keeps the visual identity routed through the theme's palette
slots. Full discussion in the [themes recipe](themes.md).

```java
section
    .softPanel(theme.palette().surfaceMuted(), 10, 14)
    .accentLeft(theme.palette().accent(), 4)
    // ... children
```

## See also

- [Shape-as-container](shape-as-container.md) — circles, ellipses, and
  rounded rectangles as **clipping containers** for layered children.
- [Transforms and z-index](transforms.md) — rotate / scale a shape
  container; restack layers with per-layer `zIndex`.
- [Themes](themes.md) — palette / spacing / text / table tokens that
  drive every shape's colour and size from one place.
