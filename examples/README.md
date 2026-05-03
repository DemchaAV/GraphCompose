# GraphCompose Examples

Runnable file-render examples for every public surface — built-in
templates, v1.5 cinematic features, public-API showcases, and a
kitchen-sink master demo.

## Example catalogue

### Document scenarios

- `ModuleFirstFileExample` — module-first authoring against `DocumentSession`
- `CvFileExample` — single CV using `CvTemplateV1`
- `CvTemplateGalleryFileExample` — every built-in CV variant in one orchestrated run
- `CoverLetterFileExample` — single-page cover letter
- `InvoiceFileExample` — invoice via `InvoiceTemplateV1`
- `ProposalFileExample` — proposal via `ProposalTemplateV1`
- `WeeklyScheduleFileExample` — weekly schedule template

### Cinematic templates (v1.5)

- `InvoiceCinematicFileExample` — `InvoiceTemplateV2 + BusinessTheme.modern()`
- `ProposalCinematicFileExample` — `ProposalTemplateV2 + BusinessTheme.modern()`
- `CinematicProposalFileExample` — handcrafted v1.4-style cinematic proposal

### v1.5 feature showcases

- `ShapeContainerExample` — circles, ellipses, rounded cards with clipped layers (`ClipPolicy.CLIP_PATH`)
- `TransformsExample` — rotate, scale, and per-layer z-index swap
- `TableAdvancedExample` — row span, zebra rows, totals row, repeating header on page break
- `CustomBusinessThemeExample` — hand-built `BusinessTheme` driving `InvoiceTemplateV2`
- `HttpStreamingExample` — `writePdf(OutputStream)` for Servlet / S3 / GCS adopters
- `LayoutSnapshotRegressionExample` — deterministic `layoutSnapshot()` workflow with baseline / drift report

### Public-API surface showcases

- `RichTextShowcaseExample` — every `RichText` method
  (`plain / bold / italic / boldItalic / underline / strikethrough /
  color / accent / size / style / link / append`) laid out as labelled
  rows on one A4 page
- `SectionPresetsExample` — `pageBackground`, `band`, `softPanel`,
  `accentLeft / accentRight / accentTop / accentBottom`, and per-corner
  `DocumentCornerRadius` side-by-side
- `BarcodeShowcaseExample` — QR, Code 128, Code 39, EAN-13, EAN-8 plus
  a branded QR with theme foreground / background
- `PdfChromeExample` — backend-neutral `DocumentMetadata`,
  `DocumentWatermark`, `DocumentHeaderFooter` (header + footer with
  `{page} / {pages} / {date}` tokens), and paragraph-level
  `DocumentBookmarkOptions` materialising as PDF outline entries

### Kitchen-sink master demo

- `MasterShowcaseExample` — fictional "Q2 sample report" that combines
  the canonical surface end-to-end: `BusinessTheme` + page background +
  hero with rotated shape container + branded QR + executive summary +
  zebra-striped totals table + accent-bordered highlight cards + Code
  128 footer barcode. Use as a reference when composing your own
  multi-page documents.

## Workflow

1. Install the root library artifact from the repository root:

```bash
./mvnw -DskipTests install
```

2. Run the whole gallery:

```bash
./mvnw -f examples/pom.xml exec:java -Dexec.mainClass=com.demcha.examples.GenerateAllExamples
```

3. Generated PDFs are written to:

```text
examples/target/generated-pdfs/
```

You can also run a single example by passing its main class to
`exec.mainClass`, for example:

```bash
./mvnw -f examples/pom.xml exec:java -Dexec.mainClass=com.demcha.examples.MasterShowcaseExample
```

The same `mvnw.cmd` form works on Windows PowerShell with backslash
paths.
