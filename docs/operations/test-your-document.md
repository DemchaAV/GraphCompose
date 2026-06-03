# Testing your document — from "I just authored it" to "CI guards it"

A short, end-to-end recipe for protecting a GraphCompose document
(template, preset, or one-off layout) with automated tests, so any
future change to the engine or your own code shows up as a red CI
run, not a silent visual regression.

If you want the deep reference, jump to
[Layout snapshot testing](./layout-snapshot-testing.md). This page
is the "Hello world" — start here, link there when you need detail.

---

## The three protection layers

GraphCompose offers three test layers, ordered cheap → expensive:

| Layer | Catches | Where the baseline lives | Test class pattern |
|---|---|---|---|
| **1. Smoke** | Does the document compile + render at all? | _no baseline, exit code only_ | `*SmokeTest` |
| **2. Layout snapshot** | Geometry — coordinates, sibling order, page breaks, layer/z-index | JSON file (deterministic, cross-machine stable) | `*LayoutSnapshotTest` |
| **3. Pixel-level visual** | Final render — fonts, colours, anti-aliasing | PNG file (per-pixel diff, tolerance budget) | `*VisualParityTest` / `*DemoTest` |

In day-to-day work **layout snapshots are the workhorse**: deterministic,
diff-able, fast. Pixel-level visual catches the "looks wrong in PDF
but the math is right" class, but it is slower to inspect and more
sensitive to font/renderer drift between OS — keep it for templates
and presets you ship to others.

---

## End-to-end recipe (Layout snapshot)

Five steps. First three are once-per-document; the rest is automatic.

### 1. Author your document

```java
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;

try (DocumentSession session = GraphCompose.document()
        .pageSize(DocumentPageSize.A4)
        .margin(22, 22, 22, 22)
        .create()) {

    session.pageFlow(page -> page
            .module("Hello", module -> module
                    .paragraph("First report — GraphCompose layout demo")));

    session.buildPdf();  // optional — for visual inspection
}
```

### 2. Add a layout snapshot test next to your document

```java
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.junit.jupiter.api.Test;

class MyReportLayoutSnapshotTest {

    @Test
    void shouldKeepReportLayoutStable() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(22, 22, 22, 22)
                .create()) {

            session.pageFlow(page -> page
                    .module("Hello", module -> module
                            .paragraph("First report — GraphCompose layout demo")));

            LayoutSnapshotAssertions.assertMatches(
                    session,
                    "my_reports/report_v1_layout");   // baseline path (no extension)
        }
    }
}
```

`LayoutSnapshotAssertions.assertMatches(session, name)` resolves the
baseline at:

```
src/test/resources/layout-snapshots/my_reports/report_v1_layout.json
```

The first run will fail because the baseline does not exist yet —
that's expected. Go to step 3.

### 3. Bless the first baseline

Once. Run the test in **update mode** so it writes the baseline JSON:

```bash
./mvnw test -Dgraphcompose.updateSnapshots=true \
            -Dtest=MyReportLayoutSnapshotTest -pl .
```

The baseline JSON appears under `src/test/resources/layout-snapshots/`.
Commit it alongside your test class — the baseline is part of the
test, not generated output.

### 4. Day-to-day: just run the suite

```bash
./mvnw test -pl .
```

The test now passes deterministically. Any change that drifts the
layout — a margin tweak, a new module insertion, a builder behaviour
change deep in the engine — fails this test immediately, with a
specific path / coordinate / page diff in the failure message and a
generated `*.actual.json` under `target/visual-tests/layout-snapshots/`
that you can diff against the committed baseline.

### 5. You changed something on purpose. Re-bless.

```bash
./mvnw test -Dgraphcompose.updateSnapshots=true \
            -Dtest=MyReportLayoutSnapshotTest -pl .
```

The baseline is overwritten with the new layout. **Commit the updated
JSON in the same change as the production code** — the baseline diff
in the PR is itself part of the review (a senior reviewer should look
at the JSON diff to confirm the layout change is what you intended).

---

## What a snapshot file looks like

```json
{
  "formatVersion": 1,
  "canvas": { "width": 595.276, "height": 841.89 },
  "totalPages": 1,
  "nodes": [
    {
      "path": "module[Hello]/paragraph[0]",
      "depth": 2,
      "layer": 0,
      "computedX": 22.0,
      "computedY": 22.0,
      "placementX": 22.0,
      "placementY": 22.0,
      "width": 551.276,
      "height": 14.4,
      "startPage": 0,
      "endPage": 0
    }
  ]
}
```

Stable fields only — coordinates, dimensions, structure, paging. No
UUIDs, no text payload, no colours. That is by design: small,
content-agnostic diffs that a human can review in a PR.

If you want to also assert text content or colour, drive those
checks separately with regular unit tests — snapshot is for geometry.

---

## When a snapshot fails — debugging recipe

1. The failure message points at the actual file:
   `target/visual-tests/layout-snapshots/<name>.actual.json`
2. Compare the actual against the committed baseline under
   `src/test/resources/layout-snapshots/<name>.json`. Most diff tools
   highlight a single field-level change.
3. Decide what you're looking at:
   - **`computedY` / `placementY` shifted by a few units** → a margin
     or padding change upstream, or a font swap that changed text
     height.
   - **`startPage` / `endPage` changed** → page-break shifted; check
     pagination tolerance and whether you added content before the
     break.
   - **A node appeared / disappeared** → semantic graph changed; check
     conditional `if (...)` branches in your document author code.
   - **Sibling order changed** → composition order in your DSL changed.
4. If the change is intentional: re-bless (step 5 above) and commit
   the baseline diff in the same PR.
5. If the change is *not* intentional: investigate the layout math
   before you trust the PDF output.

---

## Where every file lives

```
src/test/java/com/example/MyReportLayoutSnapshotTest.java   ← your test
src/test/resources/layout-snapshots/my_reports/
    report_v1_layout.json                                    ← committed baseline
target/visual-tests/layout-snapshots/my_reports/
    report_v1_layout.actual.json                             ← generated on mismatch
```

---

## CI behaviour

CI **never** sets `graphcompose.updateSnapshots=true`. Snapshot tests
in CI run in strict comparison mode — any drift fails the build and
writes the `.actual.json` artifact for download. This is the property
that prevents accidental baseline drift on a busy main branch.

---

## Pixel-level visual gate

When the math is right but the PDF looks wrong — wrong font shape,
wrong colour, anti-aliasing artefacts — the layout snapshot does not
catch it. GraphCompose uses a pixel-diff visual parity gate for each
shipped CV / cover-letter preset and for the engine showcase tests
(see `CvV2VisualParityTest`, `CoverLetterV2VisualParityTest`,
`TableRowSpanDemoTest` and friends).

The harness behind those tests is the public
`com.demcha.compose.testing.visual.PdfVisualRegression` +
`ImageDiff` API (`@since 1.6.9`), a sibling to the
`com.demcha.compose.testing.layout.*` snapshot helpers — library
consumers can adopt the same pixel-level gate against their own
presets. Start from `PdfVisualRegression.standard()`, point
`baselineRoot(...)` at your own baseline directory, and call
`assertMatchesBaseline(name, pdfBytes)`; run with
`-Dgraphcompose.visual.approve=true` to (re)bless baselines.

---

## When to use which layer

| You want to know that… | Use |
|---|---|
| The document compiles + renders at all | smoke (just call `buildPdf()` in a test) |
| The semantic graph and resolved coordinates are stable across engine refactors | **layout snapshot** |
| The PDF visually looks identical, fonts/colours and all | **pixel-level visual** (`PdfVisualRegression`) |
| A specific layout math rule holds | a focused unit test |

The advice scales: a flagship template or a preset you publish to
others deserves all three. A one-off internal report needs smoke +
layout snapshot — that catches 95% of the regressions you'd care
about, at near-zero cost per run.

---

## Deeper reference

- [Layout snapshot testing](./layout-snapshot-testing.md) —
  full reference: pipeline position, snapshot contents,
  determinism guarantees, downstream-project adoption, CI policy,
  what NOT to snapshot.
- [`LayoutSnapshotPublicApiDogfoodTest`](../../src/test/java/com/demcha/testing/layout/LayoutSnapshotPublicApiDogfoodTest.java)
  — a working integration test that drives the snapshot API
  entirely through the published surface. Copyable starting point.
- [`CvV2VisualParityTest`](../../src/test/java/com/demcha/compose/document/templates/cv/v2/presets/CvV2VisualParityTest.java)
  — example of the pixel-level pattern (currently test-only;
  becoming public via Track N).
