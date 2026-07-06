# GraphCompose Emoji

`io.github.demchaav:graph-compose-emoji`

Colour-emoji SVG glyphs (Noto Emoji) plus a GitHub-shortcode index, packaged as an
**independently-versioned** companion artifact (its own `emoji-v*` tag line — it changes only
when the glyph set or shortcode index does, so it does not track the engine version).

## When to depend on it

Add it only to render colour emoji. Text without emoji needs nothing, and an unknown shortcode
falls back to its literal text — so a document renders unchanged with or without this artifact.
`graph-compose-bundle` includes it.

## Usage

```java
RichText.text("").emoji(":star:", size)   // resolves the shortcode to an inline vector glyph
```

It is a resource-only jar (glyphs + `emoji-index.properties`); the shortcode resolver in the
engine reads it from the classpath.

## Install

Independent version line (`emoji-v*`), pinned to a version compatible with your engine train.
See [root README → Installation](../README.md#installation) for the pinned coordinate.
