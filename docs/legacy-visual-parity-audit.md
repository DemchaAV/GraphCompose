# Legacy Visual Parity Audit

This note captures the visual and behavioral gaps still present after the
canonical template/test migration on `graphcompose-v2-engine`.

The goal is not to restore the deprecated public legacy API. The goal is to
restore the user-visible rendering contract that still exists on `dev`, then
lock that contract into the canonical suite.

## Scope

- Compared current canonical template rendering on `graphcompose-v2-engine`
  against legacy rendering on `dev`.
- Focused on the regressions reported during manual inspection of visual tests:
  guide lines, margins/box decoration, content placement, and markdown styling.
- Used the built-in CV template as the first audit target because it exercises
  header layout, paragraph wrapping, bullets, markdown emphasis, and guide-line
  overlays in one document.

## Confirmed Gaps

### 1. Canonical template layout no longer matches the legacy document structure

The canonical CV template currently emits a flat paragraph flow:

- `MainVBoxContainer -> ModuleHeaderName`
- `MainVBoxContainer -> ModuleHeaderInfo`
- `MainVBoxContainer -> ModuleHeaderLinks`

The legacy template on `dev` emitted a structured header module:

- `MainVBoxContainer -> ModuleHeader`
- `ModuleHeader -> TextComponent`
- `ModuleHeader -> HContainer`
- `HContainer -> TextComponent / Rectangle / TextComponent`

This is not a small geometry drift. It is a different layout model.

Concrete evidence from the committed snapshot baselines:

- Current canonical baseline:
  `src/test/resources/layout-snapshots/canonical-templates/cv/template_cv_1_standard.json`
  places `ModuleHeaderName` at `placementX = 15.0`.
- Legacy baseline on `dev`:
  `src/test/resources/layout-snapshots/templates/cv/template_cv_1_standard.json`
  places the header name inside `ModuleHeader/TextComponent` at
  `placementX = 425.896`.

The current suite is therefore green against a new canonical baseline, not
against the legacy visual contract.

### 2. Guide-line rendering semantics are not legacy-equivalent

Legacy guide rendering used the full guide system:

- box guides
- margin guides
- padding guides
- guide markers
- multi-page guide fragmentation through the legacy guide renderer

The current canonical renderer
`src/main/java/com/demcha/compose/document/backend/fixed/pdf/PdfGuideLinesRenderer.java`
only draws:

- one outer rectangle for the fragment
- one inner rectangle for padding when padding exists

It does not reproduce the legacy margin/padding/box overlay language or marker
style from:

- `dev:src/main/java/com/demcha/compose/layout_core/system/implemented_systems/pdf_systems/PdfGuidesRenderer.java`
- `dev:src/main/java/com/demcha/compose/layout_core/system/interfaces/guides/**`

Practical result: the current "guides" PDFs are useful for coarse fragment
inspection, but they are not the same debugging artifact users relied on in the
old engine.

### 3. Markdown support is functionally missing on the canonical template path

Legacy template composition used `BlockTextBuilder`, which supports markdown
tokenization through `MarkDownParser` when the composer has markdown enabled.

Current canonical templates do not render markdown. They strip a small subset
of markdown syntax to plain text:

- `src/main/java/com/demcha/compose/document/templates/support/TemplateSceneSupport.java`
- `src/main/java/com/demcha/compose/document/templates/support/CvTemplateComposer.java`
- `src/main/java/com/demcha/compose/document/templates/support/CoverLetterTemplateComposer.java`
- `src/main/java/com/demcha/compose/document/templates/support/EditorialBlueCvTemplateComposer.java`

The canonical document surface also has no markdown toggle analogous to
`GraphCompose.pdf().markdown(true)`.

Observed effect in the generated CV visual artifact:

- Legacy `dev` render preserved bold/italic spans inside summary and skill rows.
- Current canonical render outputs the same text as plain `Helvetica` spans.

This is a real feature regression, not just a styling preference.

### 4. Current visual coverage does not protect legacy parity

The current render tests under:

- `src/test/java/com/demcha/compose/document/templates/builtins/BuiltInTemplateRenderTest.java`
- `src/test/java/com/demcha/compose/document/templates/cv/CvTemplateRenderTest.java`

mostly assert:

- file exists
- file is non-empty
- minimum page count
- expected font family appears somewhere in the PDF

They do not assert:

- guide-line semantics
- legacy-equivalent placement
- markdown span styling
- raster-level parity against `dev`

The current layout snapshot suite also compares against canonical baselines
stored under `src/test/resources/layout-snapshots/canonical-templates`, so it
cannot detect divergence from the `dev` branch by itself.

## Evidence Collected

### Raster comparison

Generated artifacts:

- current:
  `target/visual-tests/guides/templates/cv/template_cv_1_render_file_with_guide_lines.pdf`
- `dev` worktree:
  `../GraphCompose-dev-audit/target/visual-tests/guides/templates/cv/template_cv_1_render_file_with_guide_lines.pdf`

Rendering page 1 from both PDFs and comparing raw pixel samples produced:

- `mean-abs-diff ~= 33.15`

That indicates a substantive visual difference, not a small anti-aliasing drift.

### Snapshot structure comparison

Comparing the first nodes of the current and legacy CV snapshot baselines shows:

- canonical root kind: `ContainerNode`
- legacy root kind: `VContainer`
- canonical header: flattened paragraphs
- legacy header: module plus nested horizontal containers and separators

### Markdown span comparison

On `dev`, extracted PDF text spans include mixed fonts for markdown emphasis:

- `Helvetica-Bold`
- `Helvetica-Oblique`
- regular `Helvetica`

On the current canonical branch, the same content is emitted as plain regular
`Helvetica` paragraph text.

## Root Cause Summary

The parity gap is not one bug. It is a bundle of three separate migration gaps:

1. The canonical template composers were simplified into a flatter semantic
   paragraph flow instead of reproducing the old scene composition model.
2. The canonical PDF backend implemented a new guide renderer instead of
   porting the old guide overlay contract.
3. The canonical paragraph path never received span-aware markdown handling, so
   template composers fell back to stripping markdown characters.

## Recommended Plan

### Stage 1. Lock the old contract into the canonical regression suite

- Reintroduce `dev` visual expectations as canonical parity fixtures instead of
  treating the new canonical outputs as the source of truth.
- Add explicit parity coverage for:
  - CV clean render
  - CV guide-line render
  - cover letter render
  - markdown-heavy text blocks
- Keep the old committed legacy snapshots as historical baselines until the
  canonical path reproduces them.
- Add at least one automated artifact comparison beyond "file exists", ideally:
  - structured snapshot comparison against the legacy baseline
  - span/style assertions for markdown
  - raster or image-hash comparison for guide overlays

### Stage 2. Restore missing canonical primitives before rewriting more templates

- Add markdown support to the canonical paragraph path instead of stripping
  markdown tokens.
- Extend canonical guide rendering to cover the legacy box/margin/padding
  debugging semantics.
- Introduce the missing composition primitives needed for template parity:
  - horizontal grouping / info panel composition
  - separator elements inside those groups
  - module-level layout behavior close to the legacy template structure

Without this stage, more template ports will keep baking in visual regressions.

### Stage 3. Rebuild built-in templates against the old visual contract

Suggested order:

1. CV template V1
2. Cover letter template V1
3. Editorial Blue CV
4. Invoice
5. Proposal
6. Weekly schedule

The first two should go first because they already demonstrate all reported
regression classes: header placement, markdown, and guide/debug readability.

### Stage 4. Tighten CI and migration policy

- Make canonical visual tests fail on parity regressions, not only on broken
  files.
- Keep layout snapshot updates opt-in and documented.
- Do not replace legacy-equivalent baselines with new canonical baselines unless
  the visual change is intentional and explicitly approved.

## Suggested Next Implementation Slice

The highest-value next slice is:

1. restore CV visual parity first
2. add markdown span parity to canonical paragraphs
3. port the legacy guide overlay contract into the canonical PDF backend

That single slice gives the fastest path to making the manual visual tests look
like the `dev` branch again, while also fixing the test suite so the regression
does not reappear later.
