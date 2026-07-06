# GraphCompose Testing

`io.github.demchaav:graph-compose-testing`

Consumer testing support: deterministic **layout-snapshot assertions** and **PDF visual
regression** (pixel-diff of rendered pages). Add it at **test scope** to lock the output of
documents you generate.

## When to depend on it

Add it (test scope) when you want to catch layout/visual regressions in your own documents.
Not needed at runtime; not bundled by any published artifact.

## Usage

```java
// Layout snapshot — asserts the renderer-neutral layout graph is unchanged.
LayoutSnapshotAssertions.assertMatches(session.layoutSnapshot(), "invoice-basic");

// Visual regression — pixel-diffs the rendered pages against a committed baseline.
PdfVisualRegression.standard().assertMatchesBaseline("invoice-basic", pdfBytes);
```

Update baselines deliberately with the documented system properties
(`LayoutSnapshotAssertions.UPDATE_PROPERTY` / `PdfVisualRegression.APPROVE_PROPERTY`).

## Install

Same version as the rest of the GraphCompose train (lockstep). Copy-paste snippet and the
full "which artifact?" table: [root README → Installation](../README.md#installation).
Upgrading from 1.x: [modules migration guide](../docs/migration/v2.0.0-modules.md).
