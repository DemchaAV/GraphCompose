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

The same release adds **Noto Sans Georgian** and **Noto Sans Armenian**
(`FontName.NOTO_SANS_GEORGIAN`, `FontName.NOTO_SANS_ARMENIAN`), covering both cases of
each script — including Georgian Mtavruli, the capitals headings are set in, which Unicode
puts in a block of its own. Upstream ships them as variable fonts and there are no static
weights to take, so the artifact carries the regular instance and bold or italic styles
resolve to it: they render, unemboldened and upright.

Korean arrives with **Gothic A1** (`FontName.GOTHIC_A1`), in a drawn regular and bold.
It carries all 11 172 precomposed Hangul syllables, both jamo forms, and — unlike the
better-known Korean families — Latin-1, Latin Extended-A, the whole Cyrillic block and
the modern Greek alphabet. That last
part matters more than it sounds: one paragraph is drawn in one family, so a Korean
sentence holding a European name is drawn entirely in the Korean font, and a family with
ASCII but no accents turns *Müller* into *M?ller*. Hanja are not covered.

## Which script needs which family

Counts below are out of the 35 bundled Google families, and count a family only when its
`cmap` covers every letter of the named range — which is why Cyrillic appears twice.
Eighteen families carry the Russian and Ukrainian letters most Cyrillic text is written
in; four carry the whole block, including the extended letters the Central Asian and
Caucasian languages need. Greek is the modern alphabet rather than its Unicode block,
whose archaic and Coptic letters no text face carries. Everything here is measured against
the shipped binaries rather than against what a family is known for. (The standard-14 are
WinAnsi and carry none of these scripts.) `BundledScriptCoverageTest` holds this table to the binaries: the single-family rows to
those exact families, and the wider rows to their counts — so changing the bundled set
fails a build and names the row to re-measure, rather than leaving the table to go quietly
stale.

| Script | Families that cover it | How many |
| --- | --- | --- |
| Basic Latin (ASCII) | all of them | 35 |
| Vietnamese | most — **not** `VOLKHOV`, `AMIRI`, `PT_SANS`, `PT_SERIF`, `POPPINS`, `ZILLA_SLAB`, `UBUNTU`, `NOTO_SANS_GEORGIAN`, `NOTO_SANS_ARMENIAN` | 26 |
| Cyrillic — Russian and Ukrainian letters | `PT_SANS`, `PT_SERIF`, `FIRA_SANS`, `ARSENAL`, `LATO`, `CARLITO`, `GOTHIC_A1`, … | 18 |
| Cyrillic — the whole block | `LATO`, `TINOS`, `COUSINE`, `GOTHIC_A1` | 4 |
| Latin Extended-A | `LATO`, `CARLITO`, `FIRA_SANS`, `COUSINE`, `TINOS`, `GOTHIC_A1`, `AMIRI`, … | 11 |
| Greek — the modern alphabet | `GENTIUM_PLUS`, `TINOS`, `COUSINE`, `LATO`, `FIRA_SANS`, `GOTHIC_A1`, … | 11 |
| Thai | `SARABUN`, `KANIT`, `PROMPT`, `TAVIRAJ`, `TRIRONG`, `BAI_JAMJUREE` | 6 |
| Hebrew | `DAVID_LIBRE`, `TINOS`, `COUSINE` | 3 |
| Arabic | `AMIRI` | 1 |
| Georgian | `NOTO_SANS_GEORGIAN` | 1 |
| Armenian | `NOTO_SANS_ARMENIAN` | 1 |
| Korean (Hangul) | `GOTHIC_A1` | 1 |
| Chinese, Japanese | — | **0** |

One paragraph is drawn in one family — the engine does not fall back across families — so
a run mixing two scripts needs a single family that carries both. For Arabic, Georgian,
Armenian and Hangul there is exactly one bundled family each and it carries none of the
others, so mixing any two of those means registering a font of your own through
`FontFamilyDefinition`.

Chinese and Japanese have no bundled family and cannot get one from the usual sources:
the official static Noto CJK faces use CFF outlines, which the PDF backend cannot embed,
and the variable ones carry a `wght` axis defaulting to 100 — Thin is the weight a PDF
would draw, since it applies no variable-font instancing. Register a CJK font of your own
with `FontFamilyDefinition`: a TrueType-outline file already at the weight you want.

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
