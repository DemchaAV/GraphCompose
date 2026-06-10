# Layout snapshot testing: catch re-flows before they ship

GraphCompose's layout engine is deterministic: the same document produces
the same resolved geometry, every run, on every machine. That makes layout
itself testable — `DocumentSession.layoutSnapshot()` captures the page
count, canvas, and the depth-first list of every node's resolved bounds
and metadata as stable JSON, deliberately leaving out renderer-specific
bytes (font embedding, PDFBox object IDs, timestamps). When a change
re-flows something it shouldn't have, the JSON diff shows it instantly —
no PDF diffing, no golden images.

## A snapshot test in three lines

```java
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;

@Test
void invoiceLayoutIsStable() throws Exception {
    try (DocumentSession document = GraphCompose.document()
            .pageSize(DocumentPageSize.A4)
            .margin(DocumentInsets.of(28))
            .create()) {
        new InvoiceTemplateV2(theme).compose(document, sampleInvoice());

        LayoutSnapshotAssertions.assertMatches(document, "templates/invoice/invoice_baseline");
    }
}
```

The slash-delimited key is a logical path: this example compares against
`src/test/resources/layout-snapshots/templates/invoice/invoice_baseline.json`.
`LayoutSnapshotAssertions` ships in the main GraphCompose artifact, so
consumer projects can use it without any extra test dependency.

## First run and updates

On the **first run** the baseline does not exist: the assertion fails,
writes the actual snapshot, and tells you how to accept it. Accepting —
and updating after any *deliberate* layout change — is one flag:

```
./mvnw test -Dtest=YourSnapshotTest -Dgraphcompose.updateSnapshots=true
```

This overwrites the committed baseline with the current layout. Review
the JSON diff before committing: that diff *is* the layout change.

## On mismatch

A failed comparison writes the offending snapshot to
`target/visual-tests/layout-snapshots/<path>.actual.json` and the
assertion message names both files:

```
Layout snapshot mismatch for invoice_baseline.
Expected: src/test/resources/layout-snapshots/templates/invoice/invoice_baseline.json
Actual:   target/visual-tests/layout-snapshots/templates/invoice/invoice_baseline.actual.json
Re-run with -Dgraphcompose.updateSnapshots=true to update the baseline.
```

Diff the two to see exactly which node moved, grew, or paginated
differently. A passing run cleans up any stale `.actual.json`.

## Using it in consumer projects

This is not just an internal tool — if you build templates *on*
GraphCompose, snapshot tests are the cheapest regression net for them:
compose the template with fixed sample data, assert the snapshot, and a
GraphCompose upgrade (or your own refactor) that shifts the layout fails
loudly instead of silently re-flowing a customer document.

Baselines default to `src/test/resources/layout-snapshots`; overloads
take explicit roots when your project keeps them elsewhere:

```java
LayoutSnapshotAssertions.assertMatches(document,
        Path.of("src", "test", "resources", "my-baselines"),
        Path.of("target", "snapshot-failures"),
        "quotes/quote_standard");
```

For non-JUnit flows, the underlying pieces are public too:
`document.layoutSnapshot()` returns the snapshot and
`LayoutSnapshotJson.toJson(snapshot)` serialises it, so you can wire the
same check into any harness.

Pair the snapshot with a rendered PDF from the same session
(`document.toPdfBytes()`) when you want human-reviewable output next to
the machine check.

Runnable walkthrough of the full workflow:
[`LayoutSnapshotRegressionExample`](../../examples/src/main/java/com/demcha/examples/features/snapshots/LayoutSnapshotRegressionExample.java).
A real in-tree test using the production pattern:
[`ShapeContainerLayoutSnapshotTest`](../../src/test/java/com/demcha/compose/document/dsl/ShapeContainerLayoutSnapshotTest.java).
