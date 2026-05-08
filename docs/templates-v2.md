# Templates v2 &mdash; CV, cover letter, invoice, proposal

GraphCompose v1.6 ships a rebuilt canonical template surface for the four
business document shapes: **CV, cover letter, invoice, proposal**. Every
preset is one final class with one `create(BusinessTheme)` factory, and
the spec / preset / theme split is the same across all four domains so a
caller learns it once and reuses it everywhere.

This page is a landing reference. For a full conceptual walk-through
(four-layer architecture, when to extend a preset vs. write your own,
how rendering decisions cascade through theme tokens), read
[`docs/template-authoring.md`](./template-authoring.md). For the v1 → v2
upgrade table (every old class → its v2 replacement, with before/after
code), read [`docs/migration-v1-5-to-v1-6.md`](./migration-v1-5-to-v1-6.md).

---

## Architecture in four layers

Every Templates v2 preset is composed in four layers, top-to-bottom:

1. **Theme tokens** &mdash; `BusinessTheme` + `Spacing` + `Typography`. Colours, type scale, gutters, accent rules.
2. **Layout slots** &mdash; `SingleColumn`, `TwoColumnSidebar`, `ThreeColumnMagazine`, `LetterFormat`. The page geometry strategy.
3. **Components + blocks** &mdash; `Header`, `Module`, `MarkdownText`; sealed `Block` hierarchy (`ParagraphBlock`, `BulletListBlock`, `KeyValueBlock`, `IndentedBlock`, `MultiParagraphBlock`).
4. **Spec data** &mdash; `CvSpec`, `CoverLetterSpec`, `InvoiceSpec`, `ProposalSpec`. The plain-Java record carrying the document's content.

Presets glue 1+2+3 together; a caller hands them 4. Adjusting one visual
decision is one method on one preset, never a fork of a 600-line composer.

---

## Quick start

```java
import com.demcha.compose.document.templates.cv.presets.ModernProfessional;
import com.demcha.compose.document.templates.cv.spec.CvSpec;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.theme.BusinessTheme;

DocumentTemplate<CvSpec> template = ModernProfessional.create(BusinessTheme.modern());
template.compose(session, mySpec);
```

Same shape for cover letters, invoices, and proposals &mdash; swap the
package, the spec type, and the preset class.

---

## CV preset gallery (14)

Each preset is a one-class final type with `RECOMMENDED_MARGIN` and
`create(BusinessTheme)`. Visual baselines are pixel-diff verified against
v1 reference renders.

| Preset | Layout | Notes |
|---|---|---|
| `ModernProfessional` | Single column | Slate-blue name + royal-blue underlined links; canonical default |
| `NordicClean` | Two-column sidebar | Soft-tinted profile panel on the left |
| `ClassicSerif` | Two-page editorial | Serif-only typography, restrained accents |
| `CompactMono` | Single column | Tight grid for technical CVs |
| `Executive` | Two-column sidebar | Larger accent rule + uppercase section heads |
| `EngineeringResume` | Two-column sidebar | Dense skill stack, code-friendly mono |
| `TimelineMinimal` | Single column | Vertical rule + dot timeline along the left margin |
| `BoxedSections` | Single column | Section headers in tinted boxes with full-width rule |
| `CenteredHeadline` | Single column | Centered name + bullet & key-value sections |
| `BlueBanner` | Single column | Full-width section banners in theme accent |
| `EditorialBlue` | Single column | Slab subtitle + jobTitle line + uppercase rule |
| `Panel` | Two-column sidebar | Soft-tinted panel for sidebar; project-leader shape |
| `SidebarPortrait` | Two-column sidebar | Portrait + sidebar fill to page bottom |
| `MonogramSidebar` | Two-column sidebar | Monogram badge + sidebar fill |

## Cover-letter preset gallery (14, paired)

Each cover-letter preset pairs visually with its same-named CV preset
(palette, header rhythm, accent rule). Mix-and-match is supported but the
default pairing produces the cleanest visual.

`ModernProfessional` · `NordicClean` · `ClassicSerif` · `CompactMono`
· `Executive` · `EngineeringResume` · `TimelineMinimal` · `BoxedSections`
· `CenteredHeadline` · `BlueBanner` · `EditorialBlue` · `Panel`
· `SidebarPortrait` · `MonogramSidebar`

Render any preset against a sample spec to inspect the output:

```bash
./mvnw -f examples/pom.xml exec:java \
    -Dexec.mainClass=com.demcha.examples.templates.cv.CvTemplateGalleryFileExample
```

The runnable gallery writes one PDF per preset under
`examples/target/generated-pdfs/templates/cv/`. The cover-letter gallery
mirrors the layout under `templates/coverletter/`.

---

## Invoice + proposal (minimal v2 surface)

`ModernInvoice` and `ModernProposal` ship as the canonical builder seam
for invoices and proposals respectively. Cinematic feature parity
(rich hero blocks, accent-driven section bands) lives in
`InvoiceTemplateV2` / `ProposalTemplateV2` under
`templates/builtins/` &mdash; those remain the recommended path when full
visual styling is required, while the v2 builders provide a smaller,
copy-and-tweak entry point for callers extending their own branding.

---

## Authoring features built into every preset

- **Inline markdown** &mdash; body strings carrying `**bold**` and `*italic*` markers render with proper `DocumentTextDecoration` via `templates.components.MarkdownText`.
- **Active hyperlinks** &mdash; header email + LinkedIn / GitHub labels become clickable `mailto:` / `https:` runs via `DocumentLinkOptions`.
- **Slot-based layouts** &mdash; multi-column presets (`Panel`, `SidebarPortrait`, `MonogramSidebar`) declare named slots (`MAIN`, `SIDEBAR`); custom presets rearrange modules via `.place(slot, "Module Name", ...)`.
- **Adaptive sidebar fill** &mdash; sidebar layouts size the trailing spacer dynamically from `canvas().innerHeight()` so background panels reach the page bottom on A4 / Letter / smaller fixtures without overflow.
- **`CvHeader.jobTitle` subtitle** &mdash; presets that surface a subtitle (`EditorialBlue`, `Panel`, `SidebarPortrait`, `MonogramSidebar`) read it from the spec; an empty value falls back to a placeholder.

---

## Visual parity & regression coverage

- **28 layout-snapshot baselines** under `src/test/resources/layout-snapshots/canonical-templates/{cv-v2,coverletter-v2}/` lock the rendered tree of every preset.
- **29 pixel-diff baselines** under `src/test/resources/visual-baselines/{cv-v2,coverletter-v2}/` enforce per-channel rendering parity against the v1 reference renders. The gate runs on every CI build with a calibrated `mismatchedPixelBudget` for cross-platform PDFBox font drift.
- Re-bless after a deliberate visual change with `-Dgraphcompose.visual.approve=true`.

---

## Migration from v1 templates

Legacy CV / cover-letter classes (`CvTemplateV1`, `NordicCleanCvTemplate`,
`MonogramSidebarCvTemplate`, ...) are **deleted**, not deprecated, in
v1.6. Any code constructing those classes must switch to the matching v2
preset's `create(BusinessTheme)` factory. The full mapping (every old
class → its v2 replacement, with side-by-side code) is documented in
[`docs/migration-v1-5-to-v1-6.md`](./migration-v1-5-to-v1-6.md).
