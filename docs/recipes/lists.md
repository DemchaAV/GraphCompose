# Lists: bullets, markers, and nesting

`addList` builds a semantic list node — items plus a marker policy —
instead of hand-rolling indented paragraphs. It lives on every flow
container (`pageFlow`, `module`, `section`), with a varargs shortcut for
the common case and a full `ListBuilder` lambda for everything else.

## Quick lists

```java
section.addList("Crisp vector output", "Deterministic layout", "Snapshot tests");
```

`addList(String...)` and `addList(List<String>)` render bulleted items
with the default `•` marker. Leading raw markers in the input
(`"- item"`, `"• item"`) are stripped by default so data from mixed
sources normalises cleanly; switch that off with
`normalizeMarkers(false)` when the prefix is intentional.

## Configured lists

```java
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

section.addList(list -> list
        .name("Highlights")
        .textStyle(DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(10.5)
                .build())
        .itemSpacing(3)          // gap between items, in points
        .lineSpacing(1.5)        // gap between wrapped lines of one item
        .items("First highlight", "Second highlight", "Third highlight"));
```

`align(TextAlign...)`, `padding(...)`, and `margin(...)` round out the
block-level controls — the same `DocumentInsets` conventions as every
other builder.

## Markers

```java
import com.demcha.compose.document.node.ListMarker;

section.addList(list -> list.dash().items("Dash-marked item"));
section.addList(list -> list.noMarker().items("Markerless row"));
section.addList(list -> list.marker(">").items("Custom prefix"));
section.addList(list -> list
        .marker(ListMarker.custom("✓"))   // explicit ListMarker value
        .items("Tick-marked item"));
```

`bullet()`, `dash()`, and `noMarker()` are shortcuts over
`marker(ListMarker)`. A custom marker can be any string; a trailing
space is added automatically. For markerless lists,
`continuationIndent("  ")` sets the prefix used only on wrapped
continuation lines, keeping hanging indents readable.

## Hanging indent

By default the marker is part of the item's text, and a wrapped line is
indented with a run of spaces wide enough to clear it. A whole number of
spaces rarely equals a bullet, so those lines land a little past the
first line's own text — about 2.9pt at the default style, which reads as
ragged in a CV or a report.

`hangingIndent(true)` replaces that with a marker column and a content
column, so every visual line of an item starts at the same place — the
first, the ones it wraps onto, and the ones that continue on the next
page:

```java
section.addList(list -> list
        .bullet()
        .hangingIndent(true)
        .markerGap(6)              // points between marker and text
        .items("A long item whose wrapped lines all begin directly "
               + "beneath the first word of its own first line."));
```

```
•  A long item whose wrapped
   lines all begin directly
   beneath the first word…
```

The marker is measured, so a bullet, a dash and `=>` each get the column
they actually need; `markerGap` defaults to 4pt. A markerless item takes
no marker width and no gap — it starts flush, rather than at an inset
with nothing in it. Nested lists indent as an outline: a child's marker
starts where its parent's *text* starts. `CENTER` and `RIGHT` align text
inside the content column and leave the marker where it is.

It is off by default and stays off; existing lists are untouched.

> **Fixed-layout only.** PDF and PPTX honour this. The semantic DOCX
> export writes one Word paragraph per item and lets Word lay it out, so
> it keeps the marker in the text and exports the same either way — same
> paragraphs, same text, same nesting. See
> [DOCX export](docx-export.md).

## Nested lists

`addItem(label, body)` opens a child scope: every `addItem` inside the
callback adds a child of that item, recursively. Unset depths fall back
to a built-in marker cascade (`•` → `◦` → `▪` → `·`), and
`markerFor(depth, marker)` overrides the marker for one depth:

```java
import com.demcha.compose.document.node.ListMarker;

section.addList(list -> list
        .name("Roadmap")
        .markerFor(1, ListMarker.dash())
        .markerFor(2, ListMarker.custom("*"))
        .addItem("Engineering", eng -> eng
                .addItem("Document engine", engine -> engine
                        .addItem("Nested lists")
                        .addItem("Composed table cells"))
                .addItem("Backend SPI", spi -> spi
                        .addItem("DOCX semantic backend")))
        .addItem("Documentation", docs -> docs
                .addItem("Migration guide")
                .addItem("Published ADRs")));
```

Precedence per depth: an explicit per-item marker wins over
`markerFor(depth, ...)`, which wins over the cascade. Mixing flat
`addItem(String)` and nested `addItem(label, body)` on the same builder
is supported — flat items become depth-0 leaves and source order is
preserved.

## Styled items inside cards

A list is an ordinary block node, so it composes with the usual section
chrome — soft panels, accents, spacing:

```java
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;

section.addSection("Checklist", card -> card
        .fillColor(DocumentColor.rgb(248, 244, 234))
        .cornerRadius(8)
        .padding(DocumentInsets.of(10))
        .addList(list -> list
                .marker("✓")
                .itemSpacing(2)
                .items("Tests green", "Docs updated", "Changelog entry")));
```

For bullets that are *drawn shapes* rather than text glyphs (diamonds,
stars, rating dots), use inline shape runs instead — see the
[rich-text recipe](rich-text.md).

Runnable showcase:
[NestedListExample](../../examples/src/main/java/com/demcha/examples/features/lists/NestedListExample.java)
(depth cascade, per-depth overrides, mixed flat/nested authoring).
