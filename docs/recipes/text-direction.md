# Text direction: Hebrew, Arabic, and mixed lines

Text is stored in the order it is read and drawn in the order it appears on
the page. For Latin the two coincide, so nothing has to say which is which.
For Hebrew and Arabic they do not, and a paragraph that never says so comes
out reversed.

`direction(...)` is that statement.

```java
import com.demcha.compose.document.node.TextDirection;

section.addParagraph(p -> p
        .text("שלום עולם")
        .direction(TextDirection.RTL)
        .textStyle(hebrew));
```

## Direction is not alignment

They are separate choices that meet in one place. Alignment says where a line
sits in the available width; direction says which way it runs, and so decides
the edge a line starts from. A right-to-left paragraph therefore aligns right
by default:

```java
p.text(hebrew).direction(TextDirection.RTL);                    // right-aligned
p.text(hebrew).direction(TextDirection.RTL).align(TextAlign.LEFT); // left-aligned
```

Calling `align(...)` always wins. The default only applies when you did not
choose one.

## Letting the text decide

`TextDirection.AUTO` reads the direction off the paragraph's first strong
character, which is what the Unicode algorithm does. Useful when the script is
not known at authoring time — a user-supplied name, an address, a row from a
database:

```java
p.text(anythingTheUserTyped).direction(TextDirection.AUTO);
```

Digits and punctuation are not strong, so `"2026 שלום"` resolves right to left.
A paragraph with no strong character at all stays left to right.

## Mixed lines look after themselves

Within a paragraph, each stretch of text keeps its own direction. A Latin word
or a number embedded in Hebrew still reads forwards; the paragraph direction
only decides what it is embedded in.

```java
p.text("שלום GraphCompose 2026 עולם").direction(TextDirection.RTL);
```

You do not need to split such a line into runs — that is the algorithm's job,
not yours.

## Steering a neutral stretch

Punctuation and spaces between two scripts belong to neither, and the
algorithm resolves them by context. When that context gives the wrong answer,
the Unicode direction marks — `U+200E` (left-to-right mark) and `U+200F`
(right-to-left mark) — say which side a neutral run belongs to. Put them in
the text and they are honoured; they draw nothing and never reach the page.

## Fonts

A run is drawn in one font, and the engine does not fall back across families,
so the font you pick has to cover the script:

| Script | Bundled family | Since |
| --- | --- | --- |
| Hebrew | `FontName.DAVID_LIBRE` | `graph-compose-fonts` 1.1.0 |
| Arabic | `FontName.AMIRI` | `graph-compose-fonts` 1.1.0 |

No bundled family covers both, so a paragraph mixing Hebrew and Arabic needs a
font of your own registered through `FontFamilyDefinition`. Without a covering
font every glyph is replaced with `?` — see [font coverage](../font-coverage.md).

## Arabic joining

Arabic letters change shape by position, and the engine shapes them itself —
a PDF never runs the font's own OpenType shaping, so the contextual forms are
reached through the Arabic presentation forms the font carries. `FontName.AMIRI`
carries them all. A font that covers the Arabic letters but not the forms
renders unjoined base letters rather than `?`: the joining is lost, the text
is not. Shaping covers the base Arabic block (U+0621–U+064A) — the Persian and
Urdu extensions render unjoined for now. Vowel points and direction marks sit between letters without breaking
the join.

## Tables

A table cell says it the same way, through its style:

<!-- doc-example: id=text-direction-table-cell mode=method imports=com.demcha.compose.document.node.TextDirection -->
```java
import com.demcha.compose.document.table.DocumentTableStyle;

DocumentTableStyle.builder()
        .direction(TextDirection.AUTO)
        .build();
```

The builder's `direction(...)` takes the same `LTR`, `RTL` and `AUTO`, and
follows the same cascade as the rest of a cell style — the table's
`defaultCellStyle`, then `columnStyle(i, …)`, then `rowStyle(i, …)`, then the
cell's own `withStyle(…)` — so one call on the table's default sets it for every
cell. Give those cells a `textStyle` whose font covers the script, as a
paragraph needs (see [Fonts](#fonts)). A cell whose content is a composed
paragraph (`DocumentTableCell.node(...)`) is laid out as that paragraph, and
takes its direction from the paragraph instead.

The cell is the unit `AUTO` reads. Two cells side by side under one `AUTO`
answer it separately, and a cell's second line does not run the other way from
its first because it happens to open on Latin.

A right-to-left cell sits at its right edge unless a `textAnchor` set anywhere
in its cascade says where to put it — the rule alignment already follows in a
paragraph. An anchor sets the horizontal side along with the vertical one, so a
table-wide `TOP_LEFT` chosen only to top-align the cells also pins right-to-left
ones to the left; give them `TOP_RIGHT`. Word output is the exception: the DOCX
backend writes no alignment for a cell written as plain text, so Word places it
by its own default — the right edge, for right-to-left text — and an explicit
`textAnchor` does not reach it. An auto-width column is measured on the joined
Arabic forms, so it is sized to the text that is drawn.

A cell that declares nothing still draws its Hebrew and Arabic the right way
round: a declaration settles which direction a line is *embedded* in, while a
script runs the way it runs inside that.

## Where direction stops

Only paragraphs and table cells declare a direction. A list carries none of its
own: each item is laid out as a left-to-right paragraph, so
`align(TextAlign.RIGHT)` sets the items against the right margin, but each
bullet stays at the left end of its item.

Column order is not mirrored. The first column of a right-to-left table is still
the leftmost one — write the columns in the order they should appear.

## See also

- [`TextDirectionExample`](../../examples/src/main/java/com/demcha/examples/features/text/TextDirectionExample.java)
  — every case above, rendered.
- [Rich text](rich-text.md) · [Font coverage](../font-coverage.md)
