# graph-compose-emoji — NOTICE

## What ships here

A small **original starter set** of colour-emoji SVG glyphs, authored for
GraphCompose and licensed under the project's **MIT** license (same as this
module's metadata). The set is intentionally minimal — enough to exercise the
inline colour-emoji pipeline end to end. Glyphs live under
`src/main/resources/emoji/svg/<codepoint>.svg`, indexed by
`src/main/resources/emoji/emoji-index.properties` (`shortcode=codepoint`).

The engine resolves these via `com.demcha.compose.document.emoji.EmojiLibrary`
and `RichText.emoji(":star:", size)` — no engine change is needed to add more.

## Shipping the full Twemoji set (drop-in, optional)

To ship real, comprehensive colour emoji, use the maintained
[**jdecked/twemoji**](https://github.com/jdecked/twemoji) SVG set
(**CC-BY 4.0**):

1. Copy `assets/svg/*.svg` into `src/main/resources/emoji/svg/`
   (filenames are already lowercase hex codepoints, e.g. `1f680.svg`).
2. Extend `src/main/resources/emoji/emoji-index.properties` with the
   `shortcode=codepoint` pairs you want resolvable (the GitHub/gemoji shortcode
   list is the usual source).
3. Add the required **CC-BY 4.0 attribution** to this NOTICE (Twemoji is
   © Twitter, Inc. and other contributors) — attribution is mandatory.

No code changes are required: `EmojiLibrary` is fully data-driven from the
classpath layout above.

> The Google **Noto Color Emoji** font (`NotoColorEmoji-Regular.ttf`) is **not**
> usable here — PDFBox renders its CBDT colour tables blank. This module ships
> **vector SVG** glyphs precisely to avoid that limitation.
