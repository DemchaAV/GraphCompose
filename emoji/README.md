# GraphCompose Emoji

`io.github.demchaav:graph-compose-emoji`

Colour-emoji SVG glyphs (Noto Emoji) plus a GitHub-shortcode index, packaged as an
**independently-versioned** companion artifact (its own `emoji-v*` tag line — it changes only
when the glyph set or shortcode index does, so it does not track the engine version).

## When to depend on it

Add it only to render colour emoji. Text without emoji needs nothing, and an unknown shortcode
falls back to its literal text — so a document renders unchanged with or without this artifact.
`graph-compose-bundle` includes it.

## Smallest complete example

A shortcode resolves to an inline vector glyph inside a rich run, which the flow places like
any other content. Rendering still needs a backend — `graph-compose-render-pdf`, or the
`graph-compose` wrapper that brings it:

```java
Path out = Path.of("rated.pdf");
try (DocumentSession doc = GraphCompose.document(out).create()) {
    doc.pageFlow()
       .addRich(RichText.text("Rated ").emoji(":star:", 12))
       .build();
    doc.buildPdf();                 // -> rated.pdf
}
```

It is a resource-only jar (glyphs + `emoji-index.properties`); the shortcode resolver in the
engine reads it from the classpath.

## Install

Independent version line (`emoji-v*`) — pinned, not the engine version:

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose-emoji</artifactId>
    <version>1.0.0</version>
</dependency>
```

```kotlin
dependencies { implementation("io.github.demchaav:graph-compose-emoji:1.0.0") }
```

The full "which artifact?" table: [root README → Installation](../README.md#installation).
