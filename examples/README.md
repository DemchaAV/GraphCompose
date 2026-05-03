# GraphCompose Examples

Runnable file-render examples for every public document scenario plus
the v1.5 cinematic showcases.

## Example catalogue

### Document scenarios

- `CvFileExample` — single CV using `CvTemplateV1`
- `CvTemplateGalleryFileExample` — every built-in CV template in one PDF
- `CoverLetterFileExample` — single-page cover letter
- `InvoiceFileExample` — invoice via `InvoiceTemplateV1`
- `ProposalFileExample` — proposal via `ProposalTemplateV1`
- `WeeklyScheduleFileExample` — weekly schedule template
- `ModuleFirstFileExample` — module-first authoring against `DocumentSession`

### Cinematic templates (v1.5)

- `InvoiceCinematicFileExample` — `InvoiceTemplateV2 + BusinessTheme.modern()`
- `ProposalCinematicFileExample` — `ProposalTemplateV2 + BusinessTheme.modern()`
- `CinematicProposalFileExample` — handcrafted v1.4-style cinematic proposal

### v1.5 feature showcases

- `ShapeContainerExample` — circles, ellipses, rounded cards with
  clipped layers (`ClipPolicy.CLIP_PATH`)
- `TransformsExample` — rotate, scale, and per-layer z-index swap
- `TableAdvancedExample` — row span, zebra rows, totals row,
  repeating header on page break
- `CustomBusinessThemeExample` — hand-built `BusinessTheme` driving
  `InvoiceTemplateV2`
- `HttpStreamingExample` — `writePdf(OutputStream)` for Servlet / S3
  / GCS adopters
- `LayoutSnapshotRegressionExample` — deterministic
  `layoutSnapshot()` workflow with baseline / drift report

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
./mvnw -f examples/pom.xml exec:java -Dexec.mainClass=com.demcha.examples.ShapeContainerExample
```

The same `mvnw.cmd` form works on Windows PowerShell with backslash
paths.
