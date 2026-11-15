# Font coverage and glyph fallback

Not every character can be drawn by every font. This page explains what the
built-in PDF fonts can encode, why an unexpected `?` sometimes appears, the
three ways to render the symbol you actually wanted, and what names a font file
states for itself.

## The name picks the family, the decoration picks the face

A style names a family and a decoration, and those are two different choices:

```java
DocumentTextStyle.builder()
        .fontName(FontName.HELVETICA)                    // family
        .decoration(DocumentTextDecoration.BOLD)         // face within it
        .size(28)
        .build();
```

The standard-14 face constants — `HELVETICA_BOLD`, `TIMES_ITALIC`,
`COURIER_BOLD_OBLIQUE` and the rest — are **aliases of their family**, not faces.
`FontLibrary` rewrites each one to its base family before any lookup, so naming a
face contributes nothing and the decoration alone decides the glyph program. A
style that names `HELVETICA_BOLD` and sets no decoration renders regular
Helvetica, measured with regular metrics; the text lays out and draws, so nothing
announces it. The library logs one warning per such name to make it visible.

Name the family, set the decoration.

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

See the [Inline shapes example](../examples/src/main/java/com/demcha/examples/features/text/InlineShapesExample.java)
for the full set (`dot`, `arrow`, `chevron`, `diamond`, `star`, `checkmark`,
`checkbox`).

### 2. Use a font that covers the range

The curated Google families — `FontName.JETBRAINS_MONO` for monospaced code and
data, plus the serif / sans / Thai families with broader Unicode coverage — ship
in the separate `io.github.demchaav:graph-compose-fonts` artifact since v1.8.0.
Add it (or the `graph-compose-bundle` aggregate) to use them; without it on the
classpath the engine renders with the standard-14 fonts only, and asking for a
bundled family fails fast with a message naming the dependency. See the
[v1.8.0 fonts migration note](migration/v1.8.0-fonts.md).

Five scripts gained a covering family in fonts **1.1.0**: `FontName.AMIRI`
for Arabic, `FontName.DAVID_LIBRE` for Hebrew, `FontName.NOTO_SANS_GEORGIAN`,
`FontName.NOTO_SANS_ARMENIAN`, and `FontName.GOTHIC_A1` for Korean. Each carries
its own script plus Latin and none of the other four, and a paragraph is drawn in
a single family, so a run mixing two of them needs a font of your own. Chinese and
Japanese have no bundled family. The full table is in the
[fonts artifact README](../fonts/README.md#which-script-needs-which-family).

For glyphs no bundled family covers, register a custom font family that includes
them and select it through `DocumentTextStyle.fontName(...)`.

### 3. Stay inside WinAnsi

For simple lists, prefer the characters the base fonts already have — `•`
instead of `●`, `-` or `–` instead of an arrow.

## What a bundled face calls itself

A font file states its own names, and a system that lists fonts reads them. Two
of those names matter: the **family** (`name` id 1), which the four-style model
groups by, and the **typographic family** (id 16), which a face states when that
model cannot hold it — when it is one of six weights, say, or a width. Where a
face states id 16, that is the family it really belongs to and id 1 carries
whatever made the pair fit the four styles.

One bundled face states an id 1 that reads oddly: `AsapCondensed-Italic` gives
`Asap Condensed Condensed Regular` there, where its three siblings give
`Asap Condensed`. Everything else about it is right — its full name is
`Asap Condensed Italic`, its PostScript name `AsapCondensed-Italic`, and its
id 16 `Asap Condensed`, which is also why a reader following the format arrives
at the right family. A system that reads id 1 alone will show that string
alongside `Asap Condensed` in its font list; fontconfig, which LibreOffice uses
to build its list on Linux, lists both and puts `Asap Condensed` first.

The file is the one Google Fonts publishes, byte for byte, and no corrected
version is published upstream, so GraphCompose ships it as it is rather than
carrying a rebuilt binary of its own.

Where that record does and does not travel:

- **A PDF** names each face by its PostScript name, so this one appears as
  `AsapCondensed-Italic`.
- **A DOCX** names the family its caller registered, never a name out of the
  file. It does *read* these records — id 16 first, then id 1 — to tell whether
  a family's faces are of one typeface, which is why a family of Asap Condensed
  faces draws no note about them.
- **A PPTX** carries an embedded face in a container that names it by id 1 and
  id 2, so a deck setting text in this face carries
  `Asap Condensed Condensed Regular` in that container's family field while the
  deck itself declares `Asap Condensed`. Nothing in GraphCompose reads those
  container names back, and PowerPoint 16 was measured drawing such a deck the
  same whichever name the container carried.

## See also

- [`FontName`](../core/src/main/java/com/demcha/compose/font/FontName.java) — the built-in catalogue.
- Inline shapes: [`InlineShapesExample`](../examples/src/main/java/com/demcha/examples/features/text/InlineShapesExample.java).
- [Shapes recipe](recipes/shapes.md) · [Themes recipe](recipes/themes.md).
