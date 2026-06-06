# Font coverage and glyph fallback

Not every character can be drawn by every font. This page explains what the
built-in PDF fonts can encode, why an unexpected `?` sometimes appears, and the
three ways to render the symbol you actually wanted.

## WinAnsi and the base-14 fonts

The built-in fonts — `HELVETICA`, `TIMES`, `COURIER` and their bold / italic
variants — use **WinAnsi** encoding. WinAnsi covers Latin-1 plus a handful of
typographic extras (curly quotes, en / em dashes, the bullet `•`, the euro sign,
trademark, …) — roughly 220 characters. Anything outside that set has **no
glyph** in these fonts.

A frequent surprise: the **bullet `•` (U+2022) is in WinAnsi**, but the larger
**black circle `●` (U+25CF) is not**. They look similar, so a skill-rating row
written with `●●●●○` silently loses its dots, while one written with `•` keeps
them.

| Symbol | Code point | In WinAnsi? |
| --- | --- | --- |
| `•` bullet | U+2022 | yes |
| `–` en dash / `—` em dash | U+2013 / U+2014 | yes |
| `“ ” ‘ ’` curly quotes | U+2018–U+201D | yes |
| `●` black circle | U+25CF | **no** |
| `→ ▶ ✓ ★` arrows / ticks / stars | U+2190+ | **no** |
| emoji | U+1F300+ | **no** |

## What GraphCompose does with an unencodable character

It does **not** crash. At render time `PdfFont.sanitizeForRender` substitutes any
code point the resolved font cannot encode with `?`, keeping the width
measurement and the bytes emitted in lockstep (so wrapping never drifts). A
stray `?` in your output is the signal that a character fell outside the font's
coverage.

## Three ways to get the symbol you wanted

### 1. Draw it as geometry (recommended for shapes)

Dots, arrows, chevrons, diamonds, stars, checkmarks and checkboxes are
**shapes, not text** — render them from geometry with inline-shape runs and the
font's coverage stops mattering:

```java
document.pageFlow()
        .addParagraph(p -> p.rich(rich -> rich
                .style("Java ", bodyStyle)
                .dot(7, accent).dot(7, accent).dot(7, accent)         // ● ● ● — always render
                .dot(7, DocumentColor.WHITE, DocumentStroke.of(accent, 1.0))))  // ○ outlined ring
        .build();
```

See the [Inline shapes example](examples/src/main/java/com/demcha/examples/features/text/InlineShapesExample.java)
for the full set (`dot`, `arrow`, `chevron`, `diamond`, `star`, `checkmark`,
`checkbox`).

### 2. Use a font that covers the range

`FontName.JETBRAINS_MONO` is bundled for monospaced code and data. For broader
Unicode coverage, register a custom font family that includes the glyphs you
need and select it through `DocumentTextStyle.fontName(...)`.

### 3. Stay inside WinAnsi

For simple lists, prefer the characters the base fonts already have — `•`
instead of `●`, `-` or `–` instead of an arrow.

## See also

- [`FontName`](../src/main/java/com/demcha/compose/font/FontName.java) — the built-in catalogue.
- Inline shapes: [`InlineShapesExample`](../examples/src/main/java/com/demcha/examples/features/text/InlineShapesExample.java).
- [Shapes recipe](recipes/shapes.md) · [Themes recipe](recipes/themes.md).
