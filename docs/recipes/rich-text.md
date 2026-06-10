# Rich text: mixed styles in one paragraph

`RichText` is the fluent builder for paragraphs that need more than one
styled segment — label/value pairs, accented status keywords, inline
links, even small geometric shapes drawn on the text baseline. Each
chained call appends one inline run; the runs wrap together as a single
paragraph. Author it through `addRich` on any flow container
(`pageFlow`, `module`, `section`).

## Mixed styles in one paragraph

```java
import com.demcha.compose.document.style.DocumentColor;

section.addRich(rich -> rich
        .plain("Status: ")
        .bold("Pending")
        .plain(" — last review on ")
        .accent("Mar 14", DocumentColor.rgb(40, 90, 180))
        .plain(", supersedes ")
        .strikethrough("Mar 02"));
```

The full set of decorated runs: `plain`, `bold`, `italic`, `boldItalic`,
`underline`, `strikethrough`, `color(text, color)`, `size(text, points)`,
and `accent(text, color)` — bold-and-coloured in one call, the typical
pattern for status keywords ("Paid", "Overdue"). For anything beyond
those, `style(text, DocumentTextStyle)` takes a fully explicit style:

```java
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

section.addRich(rich -> rich
        .plain("Code: ")
        .style("RT-2026-04", DocumentTextStyle.builder()
                .fontName(FontName.COURIER_BOLD)
                .size(10)
                .build()));
```

## Reusable fragments

`RichText.text(...)` starts a standalone builder; `append(other)` splices
its runs into another paragraph, so recurring fragments (a badge, a
branded product name) live in one place:

```java
import com.demcha.compose.document.dsl.RichText;

RichText badge = RichText.text("GraphCompose ").bold("v1.8");

section.addRich(rich -> rich
        .plain("Built with ")
        .append(badge)
        .plain(" — see the changelog."));
```

`addRich(RichText)` also accepts a pre-built instance directly.

## Inline links

```java
section.addRich(rich -> rich
        .plain("Read the ")
        .link("authoring cheatsheet", "https://example.com/cheatsheet")
        .plain(" before writing a template."));

// Shortcut when the whole paragraph is one link sentence:
section.addLink("Open the project page", "https://example.com");
```

`link(text, uri)` renders with default link styling and a clickable
annotation on supporting backends; `with(text, style, linkOptions)`
combines an explicit style with link metadata. On `ParagraphBuilder`,
`inlineLink(text, options)` is the equivalent low-level call.

## Inline images

```java
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.InlineImageAlignment;

section.addRich(rich -> rich
        .plain("Powered by ")
        .image(DocumentImageData.fromPath("assets/logo.png"), 24, 12,
                InlineImageAlignment.CENTER)
        .plain(" since 2024."));
```

The image flows with the text and wraps like a word. Alignment is
relative to the surrounding line (`CENTER` by default); the full
overload adds a `baselineOffset` and `DocumentLinkOptions` for a
clickable inline image.

## Inline shapes and checkboxes

Geometric figures drawn from geometry — not font glyphs — so they render
identically regardless of font coverage. Rating dots, directional
arrows, breadcrumb chevrons, and todo checkboxes are the headline uses:

```java
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.ShapeOutline;

section.addRich(rich -> rich
        .plain("Java ")
        .dot(5, brand).dot(5, brand).dot(5, brand)
        .dot(5, null, DocumentStroke.of(brand, 0.6))   // outlined = empty slot
        .plain("    Draft ")
        .arrow(8, ShapeOutline.Direction.RIGHT, accent)
        .plain(" Review ")
        .chevron(6, ShapeOutline.Direction.RIGHT, muted)
        .plain(" Done ")
        .diamond(7, accent).star(8, accent));

section.addRich(rich -> rich
        .checkbox(10, true, green)
        .plain("  Checked todo marker"));
section.addRich(rich -> rich
        .checkbox(10, false, muted)
        .plain("  Unchecked todo marker"));
```

Beyond the named shortcuts (`dot`, `ellipse`, `diamond`, `triangle`,
`star`, `arrow`, `chevron`), `shape(ShapeOutline, fill)` accepts any
`ShapeOutline` — e.g. `ShapeOutline.checkmark(8, 8)` or
`ShapeOutline.regularPolygon(8, 8, 6)` for a hexagon. The checkbox has
"pick your tick" overloads: pass a `ShapeOutline.CheckmarkStyle`, or any
sized `ShapeOutline` as the checked-state mark. Arrows likewise take a
`ShapeOutline.ArrowStyle` to swap the design.

## Paragraph-level controls

`addRich` is a shorthand for `addParagraph(p -> p.rich(...))`. Drop down
to the paragraph builder when the rich content needs alignment, line
spacing, or margins:

```java
import com.demcha.compose.document.node.TextAlign;

section.addParagraph(p -> p
        .rich(rich -> rich
                .plain("Centred line with an ")
                .accent("accented", brand)
                .plain(" keyword."))
        .align(TextAlign.CENTER)
        .lineSpacing(2));
```

Runnable showcases:
[RichTextShowcaseExample](../../examples/src/main/java/com/demcha/examples/features/text/RichTextShowcaseExample.java)
(every fluent run) and
[InlineShapesExample](../../examples/src/main/java/com/demcha/examples/features/text/InlineShapesExample.java)
(shapes, checkboxes, and design variants).
