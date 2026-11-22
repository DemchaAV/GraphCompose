# Troubleshooting

Symptom-first answers to the most common GraphCompose surprises. Each
entry links to the canonical doc where there is more depth.

## A stray `?` appears in the PDF — or rating dots / arrows / checkmarks vanish

**Cause.** The built-in base-14 fonts (`HELVETICA`, `TIMES`, `COURIER`)
use **WinAnsi** encoding — roughly 220 characters (Latin-1 plus a few
typographic extras). A code point the resolved font cannot encode is
replaced with `?` at render time (`PdfFont.sanitizeForRender`) so width
measurement and emitted bytes stay in lockstep. The classic trap: the
bullet `•` (U+2022) **is** in WinAnsi, but the larger black circle `●`
(U+25CF), `→ ▶ ✓ ★`, and emoji are **not**.

**Fix (pick one).**

- Draw symbols as **geometry**, not text — inline-shape runs (`dot`,
  `arrow`, `chevron`, `diamond`, `star`, `checkmark`, `checkbox`) render
  from vectors and ignore font coverage. Recommended for rating dots and
  status markers.
- Register a font family that covers the glyphs you need and select it
  via `DocumentTextStyle.fontName(...)`.
- Stay inside WinAnsi (`•` not `●`, `-` / `–` not arrows).

Full detail and the coverage table: [font coverage and glyph fallback](font-coverage.md).

## PowerPoint draws Arial where the deck embeds a font

A presentation can match an embedded face to the family a deck declares by the
family the **program** states for itself — `name` id 1 — rather than by the name
the deck declares or anything in the font container. Measured in PowerPoint 16,
a family carrying a regular face that states the declared name and an italic one
that states another had that italic paragraph drawn in Arial Italic, the carried
program used by nothing.

The export says so: look for `render.pptx.font.name.mismatch` in the render log,
which names the family the file states and the one the deck declares. The line
names the condition and not an outcome, because the same condition was measured
costing nothing twice: a bold-slot face stating another family, and an italic
face of a family whose faces all state the *same* other name, were each drawn as
the page laid them out.

Declare the family under the name its files state. The declared name is
`wordFamily()`, which defaults to the name the family was registered under, so
`FontFamilyDefinition.files("Corporate", path)` on a file that states
`Acme Sans` meets this. See
[Font coverage](font-coverage.md#what-a-bundled-face-calls-itself).

If the family you declare is one a font catalogue carries — `Oswald` and `Lato`
are — read the next section first: that family may be drawn from the viewing
machine's own copy whether or not this line fires, and then the cause is not the
name.

## PowerPoint draws the right family but the wrong weight

The machine viewing the deck may have the declared family itself, and then it can
draw the text from its own copy and leave the carried faces unused — Office keeps
a cache of families it downloads on demand, so a name like `Oswald` or `Lato` is
local on a machine whose font folder holds no such file. A pair whose bold slot
carries a SemiBold is where it shows: measured in PowerPoint 16, a deck declaring
`Oswald` and carrying `Oswald-SemiBold` as its bold face had that paragraph drawn
from the local family's **Bold** at 480.10pt, 10.40pt wider than the carried
program draws it.

No field measured changes this, and nothing in the render log can report it:
the export cannot know what the viewing machine has. If the text must be drawn
from the face you ship, declare the family under a name no font catalogue
carries. See
[What a deck cannot carry its way out of](font-coverage.md#what-a-deck-cannot-carry-its-way-out-of).

## DOCX export is missing paths, polygons, or other drawing

**Cause.** The DOCX backend (`DocxSemanticBackend`, Apache POI) is a
**semantic** exporter: it writes text, lists and tables as Word's own, and draws
rectangles, rounded bars, ellipses, lines, polygons and paths as shapes anchored to the
page where the layout puts them, behind the text. A shape filled only with a gradient
paint is **dropped** — logged once per kind and named in the export report — and
`ShapeContainerNode` clipping /
`DocumentTransform` rotation + scale fall back to inline content with a
one-time capability warning. A `barcode` is not dropped: it is written as a
picture of the symbol, which scans but whose data is not editable in Word. Nor
is a horizontal line, or a box built by the DSL's divider builder (`addDivider`,
`ModuleBuilder.divider`, `DocumentDsl.divider`) or the templates module's
`Divider` widget, in the flow, only a fill with no stroke and no taller than
Word's thickest border: it is written as Word's own rule, a paragraph's bottom
border.
Any other filled box, however thin, goes the way every drawing goes — drawn
where the page puts it, or dropped with a note where the export cannot draw it.

**Fix.** Export those documents to **PDF** (the full-fidelity backend).
Use DOCX only for paragraph / list / table / image / section content.
Per-feature mapping: [canonical ↔ legacy parity matrix](architecture/canonical-legacy-parity.md).

<a id="missingbackendexception-when-rendering"></a>

## `MissingBackendException` when opening a session

**Cause.** You depend on `graph-compose-core` (the lean engine artifact) but no render
backend is on the classpath. The core carries the `DocumentSession` authoring API and a
`ServiceLoader` seam; the renderer ships separately.

The throw comes **earlier than the name suggests**: `create()` resolves the font-metrics
provider immediately, because laying text out needs measurement before anything is drawn.
So **opening the session** fails — you never reach `buildPdf()` / `toPdfBytes()` /
`toImages()`, and the stack trace points at your `create()` call, not at a render call.

The one case that does surface at the output call is asking for a format whose backend is
missing while another is present: `buildPptx()` without `graph-compose-render-pptx` on a
classpath that has the PDF backend. The message names that artifact instead.

**Fix.** Add the PDF backend, or depend on `graph-compose` (which already bundles it):

```xml
<dependency>
  <groupId>io.github.demchaav</groupId>
  <artifactId>graph-compose-render-pdf</artifactId>
  <version>2.5.1</version>
</dependency>
```

The exception message names the exact artifact to add. Plain `graph-compose` and
`graph-compose-bundle` both include the PDF backend out of the box — only a deliberately
lean `graph-compose-core` needs this. So does `graph-compose-core` +
`graph-compose-render-docx` on 2.4.x and earlier; from 2.5.0 the DOCX module brings the
PDF backend itself.

## `NoClassDefFoundError` at runtime

GraphCompose keeps the heavier office backends in **separate** artifacts so
PDF-only consumers don't pay for them. If you use DOCX export, add the
dependency to **your** project.

**DOCX export** — `document.export(new DocxSemanticBackend())` ships in the
`graph-compose-render-docx` artifact, which brings Apache POI transitively:

```xml
<dependency>
  <groupId>io.github.demchaav</groupId>
  <artifactId>graph-compose-render-docx</artifactId>
  <version>2.5.1</version>
</dependency>
```

## The bundled examples won't resolve `graph-compose`

**Cause.** `examples/` is a **separate Maven module** that depends on the
published library artifact, not on the source tree next to it.

**Fix.** Install the library to your local repository once from the repo
root, then run an example:

```bash
./mvnw -DskipTests install
./mvnw -f examples/pom.xml exec:java -Dexec.mainClass=com.demcha.examples.GenerateAllExamples
```

More: [examples/README](../examples/README.md).

## Building from source

**Build fails copying fonts — `The cloud file provider is not running`.**
The checkout lives in a cloud-synced folder (OneDrive / Dropbox / iCloud)
and the font resources under `fonts/src/main/resources/fonts/` (the
`graph-compose-fonts` module, since v1.8.0) are dehydrated placeholders. Make sure the sync client is running and the files are
downloaded locally ("Always keep on this device"), or move the checkout
outside the synced folder.

**Windows git error — `cannot update the ref 'HEAD' … Invalid argument`.**
Same cloud-folder cause, this time affecting `.git`. Apply git's own
suggestion:

```bash
git config windows.appendAtomically false
```

See [CONTRIBUTING.md](../CONTRIBUTING.md) for the full build and test gate.

## See also

- [Getting started](getting-started.md) · [Production rendering](operations/production-rendering.md) · [Logging](operations/logging.md)
- [Which template system should I use?](templates/which-template-system.md)
- [Full docs index](README.md)
