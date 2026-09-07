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

## DOCX export is missing shapes, lines, ellipses, or barcodes

**Cause.** The DOCX backend (`DocxSemanticBackend`, Apache POI) is a
**semantic** exporter. POI cannot express fixed-layout graphics, so
`shape`, `line`, `ellipse`, and `barcode` nodes are **dropped silently**,
and `ShapeContainerNode` clipping / `DocumentTransform` rotation + scale
fall back to inline content with a one-time capability warning.

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
  <version>2.3.0</version>
</dependency>
```

The exception message names the exact artifact to add. Plain `graph-compose` and
`graph-compose-bundle` both include the PDF backend out of the box — only a deliberately
lean `graph-compose-core` needs this.

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
  <version>2.3.0</version>
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
