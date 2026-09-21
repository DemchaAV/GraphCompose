# DOCX export: the semantic backend

PDF is GraphCompose's fixed-layout output — every fragment lands at exact
coordinates. DOCX is different on purpose: it is a **semantic export** that
walks the document graph and writes editable Word content, skipping the
layout pass entirely (no per-page pagination, no PDF chrome). Use it when
the recipient needs to *edit* the document; use PDF when pixels must match.

## Exporting a session

```java
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;

try (DocumentSession document = GraphCompose.document()
        .pageSize(595, 842)
        .margin(DocumentInsets.of(36))
        .create()) {
    document.pageFlow().name("Flow")
            .addParagraph(p -> p.text("Hello Word"))
            .addTable(t -> t
                    .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                    .row("R1C1", "R1C2"))
            .build();

    byte[] docx = document.export(new DocxSemanticBackend());
    // or write straight to disk:
    document.export(new DocxSemanticBackend(), Path.of("out/report.docx"));
}
```

`export(backend)` returns the DOCX bytes and also writes the session's
default output file when one was given to `GraphCompose.document(path)`;
the two-argument overload targets an explicit path.

**Dependency note:** the DOCX backend ships in the
`io.github.demchaav:graph-compose-render-docx` artifact, which brings Apache POI
transitively. Add that one dependency to export DOCX — consumers who only render
PDF never pull POI.

## What maps 1:1

| Document node | DOCX output |
|---|---|
| Paragraphs | Word paragraphs with alignment, font, size, colour, bold/italic/underline; inline runs preserved |
| Lists | Marker-prefixed paragraphs in the list's text style; nested items indent per depth and keep their own markers |
| Tables | Word tables, one cell per cell |
| Images | Embedded pictures at the node's declared size |
| Rows | A one-row table, so editors keep the side-by-side layout (cell content limited to atomic children) |
| Sections / containers | Children written in order; a fill, per-side borders or a uniform stroke travel to each paragraph inside as `w:shd` and `w:pBdr`, so a card keeps its panel — see "What a panel keeps and loses" below |
| Spacers | Empty paragraphs carrying the vertical gap as spacing-after |
| Page breaks | Explicit Word page breaks |

Page geometry (size and margins) and session metadata (title, author,
subject, keywords) carry into the Word document as well.

## Named styles, so the document can be restyled

The export writes a styles part whose `Normal` carries the document's own body text —
the style the most characters are set in, not the one the most nodes use. Runs that only
restate it stay silent, so changing `Normal` in Word changes the body the way a reader
expects. A run whose font, size or colour differs keeps saying so, so headings, chips and
accents are unaffected.

There is one `Normal` and no generated heading styles yet: a heading still carries its
own direct formatting rather than a named `Heading 1`. Restyling the body works; restyling
"all headings" in one go does not.

## What a panel keeps and loses

Word has no element that wraps a run of paragraphs, but it shades and borders each one,
and consecutive paragraphs sharing a fill render as a single band. So a container's paint
travels with the paragraphs inside it:

```java
page.addSection("Notice", card -> card
        .softPanel(surface, 8, 14)     // fill lands; radius and padding do not
        .accentLeft(accent, 3)         // lands as a left w:pBdr
        .addParagraph(p -> p.text("The band grows with this text when it is edited.")));
```

Kept: the fill, per-side borders, and a uniform stroke standing in for all four sides.
Nested containers resolve innermost-first, and the paint stops where the container does.
The band is a property of the paragraphs, so it grows and reflows as the text is edited —
which is the point of exporting DOCX rather than PDF.

Not representable, and left undone rather than approximated:

- **The corner radius.** Word paragraph shading is rectangular. The panel renders with
  square corners and the export logs one warning per document.
- **The container's padding.** A paragraph's shading hugs its own text, so the band does
  not inset its content the way the PDF does. Add spacing inside the container if the
  breathing room matters in Word.
- **A table inside a painted container.** The table keeps its own cell fills and borders
  rather than inheriting the band.

## What falls back

- **Charts → data table.** The semantic export has no layout pass, so a
  chart's compiled vector geometry does not exist here. Its *semantic*
  content is its data, so the backend writes a categories-by-series table
  (values formatted with the chart's own axis format) and logs **one
  capability warning per export**. See [charts.md](charts.md).
- **Shape containers → inline layers.** DOCX has no portable equivalent
  of a graphics-state path clip, so the container's layers are written
  inline, in source order, without the outline frame and without clipping
  — again with one warning per export.
- **`hangingIndent(true)` → the ordinary list form.** A list that opts
  into marker/content geometry exports exactly as one that did not: one
  paragraph per item, the marker in the item's text, two spaces per
  nesting depth. Nothing is lost — same paragraphs, same text, same
  nesting — but wrapped lines align the way Word aligns them rather than
  the way the PDF does, and `markerGap` has no effect here.

  This is a decision rather than an omission. Word places content at
  absolute indents and has no way to be told "start the text one marker
  width plus a gap from here", so every mechanism that looks like it
  would — a hanging indent, a hanging indent with a tab stop, real Word
  numbering — leaves a distance beside the marker equal to the column
  minus the marker's own width, a number only Word knows. Honouring the
  gap would mean measuring the marker, and this backend has no font
  runtime to measure with: its dependencies are the core model and POI,
  and keeping them that way is the point of a semantic backend. The
  approximations were built and rendered through Word before being
  rejected — a reserved column renders a gap that is not the one
  configured, and a marker wider than the column misaligns outright.

## What is skipped

Lines, ellipses, standalone shapes, and barcodes are **silently skipped**
— they are pure fixed-layout geometry with no semantic equivalent.
Headers/footers, watermarks, and protection options are also ignored by
the current exporter.

The rule of thumb: if the document leans on geometry — shapes, layered
designs, precise placement — export PDF for the reader and DOCX only as
an editable companion.

Round-trip coverage (paragraphs, tables, metadata, chart fallback) lives in
[`DocxSemanticBackendTest`](../../render-docx/src/test/java/com/demcha/compose/document/backend/semantic/docx/DocxSemanticBackendTest.java).
