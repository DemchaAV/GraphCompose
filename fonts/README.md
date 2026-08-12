# GraphCompose Fonts

`io.github.demchaav:graph-compose-fonts`

Bundled Google font binaries for GraphCompose, packaged as an **independently-versioned**
companion artifact (its own `fonts-v*` tag line — it changes rarely, so it does not track the
engine version). Split out of the engine jar in v1.8.0 so an engine upgrade never re-downloads
the curated font set.

## When to depend on it

- Pure-text and **standard-14** documents (`FontName.HELVETICA`, `FontName.COURIER`, `TIMES`)
  need **nothing extra** — the base-14 are the standard PDF fonts the render backend already
  provides.
- Add this artifact only to render in the **bundled Google families**.

It is a resource-only jar (no public Java API); its families become available to the font
catalog when it is on the classpath. `graph-compose-bundle` includes it.

**1.1.0** adds the first families for right-to-left scripts: **Amiri** for Arabic and
**David Libre** for Hebrew. Amiri also carries the Arabic presentation forms, which is what
contextual letter joining needs — a PDF draws text through the font's `cmap` without
executing OpenType `GSUB`, so a family that shapes only through `GSUB` can never join its
letters here. Neither family covers the other's script, so a run mixing both needs a font
of your own registered through `FontFamilyDefinition`. Select them with `FontName.AMIRI`
and `FontName.DAVID_LIBRE`.

## Install

Independent version line (`fonts-v*`) — pinned, not the engine version:

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose-fonts</artifactId>
    <version>1.1.0</version>
</dependency>
```

```kotlin
dependencies { implementation("io.github.demchaav:graph-compose-fonts:1.1.0") }
```

The full "which artifact?" table: [root README → Installation](../README.md#installation);
background in the [v1.8.0 fonts migration note](../docs/migration/v1.8.0-fonts.md).
