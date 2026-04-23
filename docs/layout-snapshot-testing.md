# Layout Snapshot Testing

Layout snapshot tests are the primary geometry-regression layer in GraphCompose.

They sit between unit-level layout math tests and final PDF render tests:

1. unit tests validate isolated geometry rules
2. layout snapshot tests validate the resolved document tree after layout and pagination
3. PDF render tests remain the outer smoke and human inspection layer

This ordering makes regressions cheap to diagnose:

- if coordinates drift, the JSON snapshot fails immediately
- if layering or pagination changes unexpectedly, the diff shows it directly
- if the snapshot still matches but the final PDF looks wrong, the issue is likely in rendering rather than layout

## Purpose

Visual PDF tests are still useful, but they are expensive to inspect and harder to diff precisely.

Layout snapshots solve a different problem: they let the library compare resolved geometry directly, before rendered pixels become the source of truth.

Use them when you want to know that:

- a node moved to a different coordinate
- a page break started or ended on a different page
- sibling ordering changed
- `Layer(depth)` resolution changed
- a template still resolves to the same layout after internal engine changes

## Pipeline position

`DocumentSession.layoutSnapshot()` captures the document after layout and pagination, but before PDF rendering.

That means the coordinates in the snapshot are:

- after `LayoutSystem`
- after page-breaking decisions have been applied
- before any PDFBox drawing happens

In other words, the snapshot represents the layout engine's resolved truth, not the renderer's output.

## Debug-only API contract

`layoutSnapshot()` is a debug and test API.

It does not render the PDF by itself.

If you later call:

- `buildPdf()`
- `toPdfBytes()`
- `render(...)`

on the same `DocumentSession`, GraphCompose reuses the already resolved layout so the
debug snapshot and final PDF stay in sync.

This matters for two reasons:

1. the runtime PDF path stays clean and predictable for normal library users
2. snapshot-first regression tests can still render the exact same resolved layout for inspection

If application code never calls `layoutSnapshot()`, this feature does not change the normal output pipeline.

## Public API

### Capture a raw layout snapshot

```java
try (DocumentSession document = GraphCompose.document()
        .pageSize(PDRectangle.A4)
        .margin(24, 24, 24, 24)
        .create()) {

    document.pageFlow()
            .name("SnapshotExample")
            .addParagraph("Hello GraphCompose", TextStyle.DEFAULT_STYLE)
            .build();

    LayoutSnapshot snapshot = document.layoutSnapshot();
}
```

### Assert a committed JSON baseline

Use the test harness for normal snapshot regression coverage:

```java
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;

@Test
void shouldMatchInvoiceLayoutSnapshotAndRenderPdf() throws Exception {
    Path outputFile = VisualTestOutputs.preparePdf("invoice_render_file", "clean", "templates", "invoice");

    try (DocumentSession document = GraphCompose.document(outputFile)
            .pageSize(PDRectangle.A4)
            .margin(22, 22, 22, 22)
            .create()) {

        template.compose(document, spec);
        LayoutSnapshotAssertions.assertMatches(document, "canonical-templates/invoice/invoice_standard_layout");
        document.buildPdf();
    }
}
```

This gives one test two kinds of feedback:

- machine-precise layout regression coverage
- a PDF artifact for visual inspection

## Quick recipe for adding a snapshot test

If you are adding a new feature, template, or pagination case, the fastest way to add snapshot coverage is:

1. create a JUnit test that instantiates a canonical `DocumentSession`
2. compose the document into that session
3. call `LayoutSnapshotAssertions.assertMatches(...)`
4. optionally call `buildPdf()` if you also want a PDF artifact for visual inspection

Minimal pattern:

```java
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.builtins.InvoiceTemplateV1;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

class MyFeatureLayoutSnapshotTest {

    @Test
    void shouldKeepMyFeatureLayoutStable() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(PDRectangle.A4)
                .margin(22, 22, 22, 22)
                .create()) {

            feature.compose(document, fixtureData());

            LayoutSnapshotAssertions.assertMatches(
                    document,
                    "features/my_feature_layout");
        }
    }
}
```

First run for a brand-new snapshot:

```powershell
./mvnw "-Dgraphcompose.updateSnapshots=true" "-Dtest=MyFeatureLayoutSnapshotTest" test
```

That creates the committed baseline under:

- `src/test/resources/layout-snapshots/features/my_feature_layout.json`

Normal verification run after that:

```powershell
./mvnw "-Dtest=MyFeatureLayoutSnapshotTest" test
```

If the test fails, compare:

- expected baseline in `src/test/resources/layout-snapshots/...`
- generated actual file in `target/visual-tests/layout-snapshots/.../*.actual.json`

Use snapshot tests when the thing you care about is layout stability. If you need visual confirmation too, keep `document.buildPdf()` in the same test or pair the snapshot test with a render test.

## Using snapshots in downstream projects

Library consumers can use the same public helpers that GraphCompose uses in its own tests:

```java
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

class InvoiceTemplateSnapshotTest {

    @Test
    void shouldKeepInvoiceLayoutStable() throws Exception {
        InvoiceTemplateV1 template = new InvoiceTemplateV1();
        InvoiceDocumentSpec spec = invoiceFixture();

        try (DocumentSession document = GraphCompose.document()
                .pageSize(PDRectangle.A4)
                .margin(22, 22, 22, 22)
                .create()) {

            template.compose(document, spec);
            LayoutSnapshotAssertions.assertMatches(document, "canonical-templates/invoice/invoice_standard_layout");
        }
    }
}
```

Repository snapshot coverage should be authored against the canonical `DocumentSession` path because `layoutSnapshot()` now lives there directly.

If you want different baseline folders in your own project, use the public overloads with custom roots:

```java
LayoutSnapshotAssertions.assertMatches(
        document,
        Path.of("src", "test", "resources", "layout-snapshots"),
        Path.of("target", "visual-tests", "layout-snapshots"),
        "consumer/invoice_layout");
```

## Snapshot contents

`DocumentSession.layoutSnapshot()` extracts a deterministic JSON snapshot of the resolved entity tree.

The snapshot intentionally contains stable layout data only:

- format version
- canvas and page metadata
- total page count
- deterministic node paths
- parent path and child index
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

## Identity and determinism

Each node is identified by stable tree order plus semantic naming.

The extractor uses the following strategy:

- prefer `EntityName` when it exists
- otherwise fall back to `<entityKind>[childIndex]`
- build the final identity from the full parent path

This keeps sibling collisions deterministic and makes diffs readable even when many nodes share the same render kind.

The extractor also normalizes numeric values before serialization. The current default is rounding doubles to 3 decimal places so snapshots stay stable across tiny floating-point differences while still catching real layout regressions.

## Where files live

Committed baselines:

- `src/test/resources/layout-snapshots/...`

Mismatch artifacts generated during normal test runs:

- `target/visual-tests/layout-snapshots/.../*.actual.json`

## Snapshot naming

Prefer semantic names that describe the document state:

- `canonical-templates/invoice/invoice_standard_layout`
- `canonical-templates/proposal/proposal_long_layout`
- `integration/table_pagination_test`
- `templates/cv/font-themes/template_cv_1_poppins`

The last path segment becomes the JSON file name. The preceding segments become folders.

## Local workflow

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

The same property works for downstream projects that use `LayoutSnapshotAssertions` from the published GraphCompose artifact.

In normal mode:

- expected JSON stays committed in `src/test/resources/layout-snapshots`
- mismatches write an `.actual.json` artifact under `target/visual-tests/layout-snapshots`
- the assertion failure points to both expected and actual paths

In update mode:

- the baseline JSON is overwritten intentionally
- the `.actual.json` mismatch artifact is removed if it exists

## CI expectations

CI should never enable `graphcompose.updateSnapshots`.

The expected behavior in CI is strict comparison only:

- match the committed baseline
- write `.actual.json` when there is a mismatch
- fail fast so the diff can be reviewed locally

This keeps baseline updates explicit and prevents accidental golden-file drift in automated pipelines.

## Recommended adoption pattern

When adding snapshot coverage to an existing visual test:

1. if the test already creates a `DocumentSession`, add `LayoutSnapshotAssertions.assertMatches(...)` before `buildPdf()`
2. keep the composition path explicit so the same canonical document can be snapshotted and rendered in one test
3. keep the existing PDF render assertion and artifact generation
4. generate the baseline once with `-Dgraphcompose.updateSnapshots=true`

The recommended developer flow is:

1. unit tests for local layout math
2. layout snapshot tests for full-document geometry regressions
3. PDF render tests for final visual confidence

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
- `FontShowcaseLayoutSnapshotTest`
- `CvTemplateV1LayoutSnapshotTest`
- `BuiltInTemplateLayoutSnapshotTest`
- `LayoutSnapshotPublicApiDogfoodTest`

## Interpreting a mismatch

If a snapshot fails:

1. open the `.actual.json` file under `target/visual-tests/layout-snapshots`
2. compare path, coordinates, page span, and layer/order changes
3. decide whether the change is expected
4. if expected, re-run with `-Dgraphcompose.updateSnapshots=true`
5. if not expected, investigate the layout math before trusting the rendered PDF

Useful signals to check first:

- `startPage` and `endPage`
- `computedX` and `computedY`
- `placementX`, `placementY`, `width`, and `height`
- node `path`
- `layer`

## When not to use snapshots

Layout snapshots are not a replacement for every test.

Do not use them as the only safety net when:

- you are testing renderer-specific drawing behavior
- the failure you care about is pixel-level rather than geometry-level
- a small unit test can prove the same rule more directly

They work best as the middle layer in the test pyramid, not as the only layer.
