# Layout Snapshot Testing

Layout snapshot tests are the primary geometry-regression layer in GraphCompose.

They sit between unit-level layout math tests and final PDF render tests:

1. unit tests validate isolated geometry rules
2. layout snapshot tests validate the resolved document tree after layout and pagination
3. PDF render tests remain the outer smoke and visual inspection layer

This keeps regressions cheap to detect:

- if coordinates drift, the JSON snapshot fails immediately
- if layering or pagination changes unexpectedly, the diff shows it directly
- if the snapshot still matches but the final PDF looks wrong, the issue is likely in rendering rather than layout

## What is captured

`PdfComposer.layoutSnapshot()` resolves the document through `LayoutSystem` and `PageBreaker`, then extracts a deterministic JSON snapshot of the resolved entity tree.

The snapshot intentionally contains stable layout data only:

- canvas and page metadata
- total page count
- deterministic node paths
- parent/child ordering
- depth and layer
- computed coordinates
- placement box coordinates, size, and page span
- content size
- margin and padding

It intentionally excludes unstable or noisy values such as:

- UUIDs
- raw text payload
- colors
- PDF resource ids

## Where files live

Committed baselines:

- `src/test/resources/layout-snapshots/...`

Mismatch artifacts generated during normal test runs:

- `target/visual-tests/layout-snapshots/.../*.actual.json`

## Recommended test pattern

Use a layout snapshot assertion on the same `PdfComposer` instance that you later render.

```java
@Test
void shouldMatchInvoiceLayoutSnapshotAndRenderPdf() throws Exception {
    Path outputFile = VisualTestOutputs.preparePdf("invoice_render_file", "clean", "templates", "invoice");

    try (PdfComposer composer = GraphCompose.pdf(outputFile)
            .pageSize(PDRectangle.A4)
            .margin(22, 22, 22, 22)
            .markdown(true)
            .create()) {

        template.compose(composer, data);
        LayoutSnapshotAssertions.assertMatches(composer, "templates/invoice/invoice_standard_layout");
        composer.build();
    }
}
```

That gives one test two kinds of feedback:

- machine-precise layout regression coverage
- a PDF artifact for visual inspection

## Snapshot naming

Prefer semantic names that describe the document state:

- `templates/invoice/invoice_standard_layout`
- `templates/proposal/proposal_long_layout`
- `integration/table_pagination_test`
- `templates/cv/font-themes/template_cv_1_poppins`

The last path segment becomes the JSON file name. The preceding segments become folders.

## Update flow

Normal mode compares against committed baselines:

```powershell
./mvnw test
```

To accept an intentional layout change locally:

```powershell
./mvnw "-Dgraphcompose.updateSnapshots=true" test
```

Or update a focused test only:

```powershell
./mvnw "-Dgraphcompose.updateSnapshots=true" "-Dtest=InvoiceTemplateV1LayoutSnapshotTest" test
```

CI should run without `graphcompose.updateSnapshots`.

## Adding snapshot coverage to an existing visual test

1. If the test already creates a `PdfComposer`, add `LayoutSnapshotAssertions.assertMatches(...)` before `build()`.
2. If a template hides the composer inside `render(...)`, add a package-private `compose(...)` method inside the built-in template and let `render(...)` delegate to it.
3. Keep the existing PDF render assertion. Do not replace it.
4. Generate the baseline once with `-Dgraphcompose.updateSnapshots=true`.

## What to snapshot first

Prioritize documents that are most sensitive to layout regressions:

- multi-page templates
- tables with pagination
- nested container compositions
- documents where sibling order or `Layer(depth)` matters
- theme or font variants that affect text measurement

## Examples in this repository

- `RepositoryShowcaseRenderTest`
- `SmartPaginationTest`
- `TablePaginationIntegrationTest`
- `CvTemplateV1LayoutSnapshotTest`
- `InvoiceTemplateV1LayoutSnapshotTest`
- `ProposalTemplateV1LayoutSnapshotTest`
- `WeeklyScheduleTemplateV1LayoutSnapshotTest`

## Troubleshooting

If a snapshot fails:

1. open the `.actual.json` file under `target/visual-tests/layout-snapshots`
2. compare path, coordinates, pages, and layer/order changes
3. decide whether the change is expected
4. if expected, re-run with `-Dgraphcompose.updateSnapshots=true`
5. if not expected, investigate the layout math before trusting the rendered PDF
