# Visual Regression Testing

Pixel-level visual regression is the outermost geometry-fidelity layer in GraphCompose. It renders a PDF to one PNG per page and diffs each page against a committed baseline.

It complements — does not replace — [layout snapshot testing](./layout-snapshot-testing.md):

1. unit tests validate isolated layout math
2. **layout snapshots** validate the resolved document tree (coordinates, page spans, layer/order) — structural geometry
3. **visual regression** validates the rendered pixels — font shape, colour, anti-aliasing, glyph fallback
4. human inspection of the PDF remains the final eye

Reach for visual regression when the failure you care about is *pixel-level* rather than *geometry-level*: the layout snapshot still matches but the PDF looks wrong (wrong font, wrong colour, missing glyph, anti-aliasing drift).

## Pixel vs semantic — which layer?

| You want to catch&hellip; | Use |
|---|---|
| A node moved / page break shifted / sibling order changed | layout snapshot (semantic) |
| The PDF looks identical pixel-for-pixel — fonts, colours, glyphs | **visual regression (pixel)** |
| A specific layout-math rule | a focused unit test |

The semantic layer is cheap, deterministic, and cross-platform stable. The pixel layer is precise but sensitive to platform font rendering (see [Cross-platform tolerance](#cross-platform-tolerance) below). A flagship template or a preset you publish to others deserves both.

## Public API

The harness is `com.demcha.compose.testing.visual.PdfVisualRegression` (`@since 1.6.9`), a sibling to the semantic `com.demcha.compose.testing.layout.*` helpers. It ships in the main artifact, so library consumers use the exact helpers GraphCompose uses in its own tests.

### Assert a committed baseline

```java
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import org.junit.jupiter.api.Test;

class InvoiceVisualParityTest {

    @Test
    void invoiceRendersPixelIdentical() throws Exception {
        byte[] pdfBytes;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(22, 22, 22, 22)
                .create()) {
            template.compose(document, spec);
            pdfBytes = document.toPdfBytes();
        }

        PdfVisualRegression.standard()
                .assertMatchesBaseline("invoice_standard", pdfBytes);
    }
}
```

`assertMatchesBaseline(name, pdfBytes)` renders every page, compares against `<name>-page-N.png` under the baseline root, and throws `AssertionError` if any page exceeds the configured budget. On failure it writes `<name>-page-N.actual.png` and `<name>-page-N.diff.png` next to the baseline for inspection.

### Configure the harness

`PdfVisualRegression` is immutable; every setter returns a copy.

| Setter | Default | Meaning |
|---|---|---|
| `baselineRoot(Path)` | `src/test/resources/visual-baselines` | where baselines and diff sidecars live |
| `renderScale(float)` | `1.0` | render scale multiplier (`2.0` = retina); must be `> 0` |
| `perPixelTolerance(int)` | `6` | allowed per-channel delta (`0..255`) before a pixel counts as mismatched |
| `mismatchedPixelBudget(long)` | `0` | mismatched pixels tolerated per page before the assertion fails |

### Diff images directly

For ad-hoc comparison, render pages and call `ImageDiff` yourself:

```java
List<BufferedImage> pages = PdfVisualRegression.standard().renderPages(pdfBytes);
ImageDiff.Result diff = ImageDiff.compare(expectedPng, pages.get(0), 6);
assertThat(diff.withinBudget(0)).isTrue();
```

## Approve mode (blessing baselines)

There is no baseline the first time. Run with the approve flag to write the current renders as the baseline:

```bash
./mvnw test -Dtest=InvoiceVisualParityTest -Dgraphcompose.visual.approve=true
```

The system-property name is exposed as `PdfVisualRegression.APPROVE_PROPERTY`; the environment variable `GRAPHCOMPOSE_VISUAL_APPROVE=true` works as a fallback. In approve mode the harness writes baselines and skips the diff assertion — so **never enable it in CI verification**, only when you have reviewed the new render and intend to re-bless.

## Where files live

- committed baselines: `<baselineRoot>/<name>-page-N.png`
- mismatch artifacts (normal runs): `<baselineRoot>/<name>-page-N.actual.png` and `<name>-page-N.diff.png` (mismatched pixels red, matching pixels greyscale)

Use a flat `name`, or pre-create nested baseline directories — the harness creates the baseline root but not intermediate folders.

## Cross-platform tolerance

PDFBox font rasterization drifts slightly across platforms (different system fonts, different rasterizer). A baseline recorded on Windows will not match Linux CI pixel-for-pixel.

The `standard()` defaults are strict (tolerance `6`, budget `0`) — good for same-platform, deterministic renders. For baselines that must survive a Windows-author → Linux-CI round trip, loosen both. GraphCompose's own CV / cover-letter parity tests calibrate to:

```java
PdfVisualRegression.standard()
        .perPixelTolerance(8)            // absorb sub-pixel anti-aliasing drift
        .mismatchedPixelBudget(50_000)   // ~glyph edges across a full A4 page
        .assertMatchesBaseline(slug, pdfBytes);
```

Tune these to your fonts and page density: too tight and CI flakes on anti-aliasing noise; too loose and a real regression slips through. Start from the values above and tighten until CI is stable.

## Using visual regression in downstream projects

Library consumers use the same published helpers:

```java
import com.demcha.compose.testing.visual.PdfVisualRegression;

PdfVisualRegression.standard()
        .baselineRoot(Path.of("src", "test", "resources", "pdf-baselines"))
        .perPixelTolerance(8)
        .mismatchedPixelBudget(50_000)
        .assertMatchesBaseline("reports/monthly_invoice", pdfBytes);
```

`PublicVisualApiDogfoodTest` in this repository drives exactly this consumer workflow end-to-end and proves the published surface is sufficient without any package-private access.

## When not to use pixel regression

- when structural geometry is what you care about → use a [layout snapshot](./layout-snapshot-testing.md) (cheaper, cross-platform stable)
- when a small unit test proves the same rule more directly
- as the *only* gate on a CI that runs on a different OS than where baselines were recorded — pair it with semantic snapshots and a sensible tolerance budget

## Examples in this repository

- `CvV2VisualParityTest`, `CoverLetterV2VisualParityTest` — preset parity with Windows-baseline / Linux-CI calibration
- `ShapeContainerVisualRegressionTest` — engine primitive fidelity
- `TableRowSpanDemoTest` — table rendering
- `PdfVisualRegressionTest` — the harness's own unit tests
- `PublicVisualApiDogfoodTest` — consumer-surface dogfood
