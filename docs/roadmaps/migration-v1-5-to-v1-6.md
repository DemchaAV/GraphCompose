# Migration: v1.5 → v1.6

v1.6 is **source-compatible with v1.5 for the engine** and the
backend-neutral authoring surface — `DocumentSession`, `DocumentDsl`,
`BusinessTheme`, `DocumentPalette`, the `FixedLayoutBackend` /
`SemanticBackend` SPIs, and every entry point on `GraphCompose`. The
release sharpens the public-vs-internal contract, opens previously-
hidden extension points, and replaces a small set of redundant or
brittle API shapes with cleaner alternatives.

The **canonical CV and cover-letter templates layer is rebuilt
from scratch** in v1.6 under a four-layer architecture
(Theme → Layout → Components → Spec). The 14 V1 CV templates and
the V1 cover-letter template are deleted, replaced by 14 v2 CV
preset classes under `templates/cv/presets/` and 14 paired letter
presets under `templates/coverletter/presets/`. v1.x SemVer "API
stability" covers the engine, not the templates layer — the
templates carve-out is documented in
[ADR 0011](adr/0011-templates-v2-architecture.md). The breaking
migration is in
[Templates v2 — CV / cover-letter API rebuilt](#templates-v2--cv--cover-letter-api-rebuilt)
below.

If your application targets v1.5 and does **not** use the V1 CV or
cover-letter templates, you can bump the dependency version to
`1.6.x` and rebuild without touching authoring code. You may see
**deprecation warnings** for two specific surfaces — they document
where the API is heading; the existing methods continue to work
until their removal is announced (planned for v2.0).

## TL;DR

| Area | v1.6 change | Action |
| --- | --- | --- |
| **Templates v2 — CV / cover letter** | **Breaking.** 14 V1 CV templates + V1 cover letter deleted; new `templates/cv/presets/*` + `templates/coverletter/presets/*` | Switch each `new XxxCvTemplate()` to `Xxx.create(BusinessTheme.modern())` — see [section](#templates-v2--cv--cover-letter-api-rebuilt) |
| `@Internal` annotation | New marker for implementation-detail types | Audit any imports from `com.demcha.compose.document.layout.*` |
| `DocumentRenderingException` | Rendering convenience methods no longer declare `throws Exception` | Optional cleanup: drop `throws Exception` from your `try { … } catch` blocks |
| Custom PDF render handlers | `PdfFixedLayoutBackend.Builder.addHandler(...)` is now a public extension point | Use it instead of forking the backend |
| DSL aliases | `DocumentDsl.text()` and `DocumentSession.builder()` deprecated | Rename to `paragraph()` and `dsl()` |
| PDF-typed chrome overloads | `metadata(PdfMetadataOptions)` and friends deprecated | Use the canonical `metadata(DocumentMetadata)` overloads |
| `DocumentPalette.of(...)` | Positional 7-arg factory deprecated | Use `DocumentPalette.builder()` |
| `Pdf_FontLoader` | Duplicate font loader deleted | Internal; no public callers existed |

Each section below has the details.

## Things that did NOT break

- Every entry point on `GraphCompose` (`document()`,
  `availableFonts()`, `renderAvailableFontsPreview(...)`).
- The full `DocumentSession` authoring lifecycle (compose, pageFlow,
  add, render, export, close, layoutGraph, layoutSnapshot).
- `DocumentDsl`, `BusinessTheme`, `DocumentPalette`, and the
  invoice / proposal / weekly-schedule template entry points
  (`InvoiceTemplateV1`, `InvoiceTemplateV2`, `ProposalTemplateV1`,
  `ProposalTemplateV2`, `WeeklyScheduleTemplateV1`).
- `FixedLayoutBackend<R>` and `SemanticBackend<R>` two-method
  contracts.
- All existing v1.5 features (shape-as-container, transforms, z-index,
  table rowSpan/zebra/totals/repeatHeader, fluent rich text).
- Visual output, snapshot baselines, and PDF byte sequences for the
  invoice / proposal / schedule templates — the v1.5 → v1.6 perf
  wins (see "Performance changes") do not alter what gets drawn.

The CV / cover-letter template surface (`CvTemplate`,
`CvTemplateV1`, `NordicCleanCvTemplate`, `CoverLetterTemplateV1`,
the 12 sibling V1 CV templates, `CvTheme`, `CvDocumentSpec`) is
**not** in this list — see
[Templates v2 — CV / cover-letter API rebuilt](#templates-v2--cv--cover-letter-api-rebuilt)
for the full migration path.

## Layout types are now `@Internal`

The `com.demcha.compose.document.layout` package — including
`LayoutGraph`, `PlacedFragment`, `PlacedNode`, `BoxConstraints`,
`MeasureResult`, `NodeDefinition`, `PreparedNode`, and ~15 other
records — is now annotated
[`@Internal`](../src/main/java/com/demcha/compose/document/api/Internal.java)
at the package level.

The annotation is a documentation signal, not a visibility change. Code
that imports these types continues to compile and run; IDE quick-doc
will show the `@Internal` marker. The contract is:

> Types annotated `@Internal` may change in any release without notice.
> Library users should not depend on annotated elements.

Why: the v1.6 plan reorganises the layout package internally (split
`BuiltInNodeDefinitions`, extract a `PlacementContext` interface, move
payload records to a dedicated subpackage) and we want those refactors
to ship without semver implications. See ADR
[0003](adr/0003-api-stability-and-internal-marker.md).

If you depend on a layout type today, please open an issue describing
the use case so we can design a stable replacement.

## Rendering exceptions: no more `throws Exception`

The convenience render methods on `DocumentSession` previously declared
`throws Exception`. v1.6 narrows them to `throws DocumentRenderingException`
(an unchecked exception), which means callers no longer need to declare
or catch a checked `Exception`:

```java
// v1.5
public void buildReport() throws Exception {
    try (DocumentSession session = GraphCompose.document(out).create()) {
        template.compose(session, spec);
        session.buildPdf();
    }
}

// v1.6 — same behaviour, cleaner caller signature
public void buildReport() {
    try (DocumentSession session = GraphCompose.document(out).create()) {
        template.compose(session, spec);
        session.buildPdf();
    }
}
```

The change applies to `toPdfBytes()`, `writePdf(OutputStream)`,
`buildPdf()`, `buildPdf(Path)`, and the `AutoCloseable.close()`
override.

Any underlying `IOException` from PDFBox is preserved as the
`Throwable.getCause()` on the wrapping `DocumentRenderingException`. If
your existing code does `catch (Exception e)`, it keeps working
unchanged. If your existing code does `catch (IOException e)`, the
compiler will tell you to either widen to `catch (Exception e)` or
unwrap via `e.getCause()`.

The lower-level SPI methods (`session.render(backend)`,
`session.export(backend)`) continue to declare `throws Exception` —
backend implementations may legitimately surface checked exceptions and
those signatures are part of the SPI contract.

## Custom PDF render handlers

`PdfFragmentRenderHandler` was nominally `public` in v1.5 but
documented as "package-private" with no registration path. v1.6 fixes
the contradiction:

```java
// Override how shapes are painted, e.g. to add a watermark overlay.
PdfFixedLayoutBackend backend = PdfFixedLayoutBackend.builder()
        .addHandler(new MyShapePainter())
        .metadata(canonicalMetadata)
        .build();

session.render(backend);
```

If your custom handler reports the same `payloadType()` as a built-in
default, your handler replaces the default for the resulting backend
instance and the replacement is logged at debug level. Adding two
custom handlers for the same payload type on one builder rejects the
second with `IllegalArgumentException`.

`PlacedFragment` and `PdfRenderEnvironment` remain `@Internal` — see
ADR [0004](adr/0004-pdf-handler-spi-extension.md) for the trade-off.

## Templates v2 — CV / cover-letter API rebuilt

The canonical CV and cover-letter templates were rebuilt from
scratch in v1.6 under a four-layer architecture: **Theme tokens →
Layout slots → Components + Blocks → Spec data**. v1.x SemVer "API
stability" covers the engine, not the templates layer; the
carve-out is documented in
[ADR 0011](adr/0011-templates-v2-architecture.md).

The change deletes:

- **14 V1 CV template classes** under `templates/builtins/`
  (`CvTemplateV1`, `NordicCleanCvTemplate`, `BlueBannerCvTemplate`,
  …) — replaced by 14 v2 preset classes under `templates/cv/presets/`.
- **15 hand-coded composer classes** under `templates/support/cv/` —
  replaced by per-domain builders + reusable components.
- **`CvTemplate` interface** under `templates/api/` — replaced by
  the generic `DocumentTemplate<S>`.
- **`CvTemplateRegistry`** under `templates/api/` — no replacement;
  call the preset factory directly.
- **`CvTheme`** under `templates/theme/` — replaced by
  `BusinessTheme` + `Spacing` tokens.
- **`CvDocumentSpec` + module data classes** (`MainPageCV`,
  `MainPageCvDTO`, `ModuleSummary`, `ModuleYml`, `SimpleModule`)
  under `templates/data/cv/` — replaced by `CvSpec` / `CvHeader` /
  `CvModule` under `templates/cv/spec/`.
- **`CoverLetterTemplateV1`** — replaced by 14 paired letter
  presets, one per CV preset.

`InvoiceTemplateV1` / `InvoiceTemplateV2`, `ProposalTemplateV1` /
`ProposalTemplateV2`, and `WeeklyScheduleTemplateV1` remain in
`templates/builtins/` and are unaffected. The new `ModernInvoice` /
`ModernProposal` v2 presets ship as a minimal canonical surface;
cinematic feature parity for those domains lands in a follow-up
release.

### CV preset migration table

Each V1 CV template maps to exactly one v2 preset. Rename the
class, swap the constructor for the preset's
`create(BusinessTheme)` factory, and switch from `CvTemplate` to
`DocumentTemplate<CvSpec>`:

| v1.5 V1 template | v1.6 v2 preset |
|---|---|
| `new CvTemplateV1()` | `ModernProfessional.create(BusinessTheme.modern())` |
| `new NordicCleanCvTemplate()` | `NordicClean.create(BusinessTheme.modern())` |
| `new ClassicSerifCvTemplate()` | `ClassicSerif.create(BusinessTheme.classic())` |
| `new CompactMonoCvTemplate()` | `CompactMono.create(BusinessTheme.modern())` |
| `new ExecutiveSlateCvTemplate()` | `Executive.create(BusinessTheme.executive())` |
| `new TechLeadCvTemplate()` | `EngineeringResume.create(BusinessTheme.modern())` |
| `new TimelineMinimalCvTemplate()` | `TimelineMinimal.create(BusinessTheme.modern())` |
| `new BoxedSectionsCvTemplate()` | `BoxedSections.create(BusinessTheme.modern())` |
| `new CenteredHeadlineCvTemplate()` | `CenteredHeadline.create(BusinessTheme.modern())` |
| `new BlueBannerCvTemplate()` | `BlueBanner.create(BusinessTheme.modern())` |
| `new EditorialBlueCvTemplate()` | `EditorialBlue.create(BusinessTheme.modern())` |
| `new ProductLeaderCvTemplate()` | `Panel.create(BusinessTheme.modern())` |
| `new SidebarPortraitCvTemplate()` | `SidebarPortrait.create(BusinessTheme.modern())` |
| `new MonogramSidebarCvTemplate()` | `MonogramSidebar.create(BusinessTheme.modern())` |

Two presets were renamed: `TechLeadCvTemplate` →
`EngineeringResume` and `ProductLeaderCvTemplate` → `Panel`. The
V1 names were industry-specific where the visual treatment is
general — switching to a name that describes the look-and-feel
makes the catalogue easier to scan.

Each preset exposes a public `RECOMMENDED_MARGIN` constant so the
caller can wire the right page margin without magic numbers:

```java
float m = (float) NordicClean.RECOMMENDED_MARGIN;
try (DocumentSession session = GraphCompose.document(out)
        .pageSize(DocumentPageSize.A4)
        .margin(m, m, m, m)
        .create()) {
    template.compose(session, spec);
    session.buildPdf();
}
```

### Cover-letter preset migration

`CoverLetterTemplateV1` is replaced by 14 paired letter presets
that share each CV preset's palette and typography. Pick the
letter that pairs with the CV preset you used; if you only used
the default cover-letter, switch to `ModernProfessionalLetter`:

| v1.5 | v1.6 |
|---|---|
| `new CoverLetterTemplateV1()` (default / Modern Professional) | `ModernProfessionalLetter.create(BusinessTheme.modern())` |
| `new CoverLetterTemplateV1()` paired with `NordicClean` CV | `NordicCleanLetter.create(BusinessTheme.modern())` |
| `new CoverLetterTemplateV1()` paired with `EditorialBlue` CV | `EditorialBlueLetter.create(BusinessTheme.modern())` |
| `new CoverLetterTemplateV1()` paired with `BlueBanner` CV | `BlueBannerLetter.create(BusinessTheme.modern())` |

The full set under `templates/coverletter/presets/`:
`ModernProfessionalLetter`, `NordicCleanLetter`, `ClassicSerifLetter`,
`CompactMonoLetter`, `ExecutiveLetter`, `EngineeringResumeLetter`,
`TimelineMinimalLetter`, `BoxedSectionsLetter`,
`CenteredHeadlineLetter`, `BlueBannerLetter`, `EditorialBlueLetter`,
`PanelLetter`, `SidebarPortraitLetter`, `MonogramSidebarLetter`.

### Interface, theme, and spec migration

| v1.5 type | v1.6 replacement |
|---|---|
| `CvTemplate` | `DocumentTemplate<CvSpec>` (generic) |
| `CvTemplateRegistry` | (removed — call the preset factory directly) |
| `CvTheme.defaultTheme()` | `BusinessTheme.modern()` (palette) + `Spacing.compact()` (padding tokens) |
| `CvTheme.classicTheme()` | `BusinessTheme.classic()` + `Spacing.comfortable()` |
| `CvDocumentSpec` | `CvSpec` |
| `MainPageCV`, `MainPageCvDTO`, `ModuleSummary`, `ModuleYml`, `SimpleModule` | `CvModule` (single record) |
| `CvHeader` (V1 inner type) | `CvHeader` under `templates/cv/spec/` (extended with `jobTitle`) |

The deprecated `CvTheme` engine-typed style accessors
(`nameTextStyle`, `sectionHeaderTextStyle`, `bodyTextStyle`,
`smallBodyTextStyle`, `linkTextStyle`, `moduleMargin`) — flagged
for removal in v2.0 in earlier v1.6 milestones — are gone
together with `CvTheme.java` itself. Clients that read styles
from `CvTheme` directly (rather than via the deleted V1
templates) need to source the palette from
`BusinessTheme.modern().palette()`, the typography scale from
`BusinessTheme.modern().text()`, and per-domain padding from
`Spacing.compact()` / `.comfortable()` / `.airy()` (under
`com.demcha.compose.document.templates.themes`).

### Before / after

```java
// v1.5
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.builtins.NordicCleanCvTemplate;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;

CvTemplate template = new NordicCleanCvTemplate(CvTheme.defaultTheme());
CvDocumentSpec spec = /* … */;

try (DocumentSession session = GraphCompose.document(out).create()) {
    template.compose(session, spec);
    session.buildPdf();
}
```

```java
// v1.6
import com.demcha.compose.document.api.DocumentTemplate;
import com.demcha.compose.document.templates.blocks.ParagraphBlock;
import com.demcha.compose.document.templates.cv.presets.NordicClean;
import com.demcha.compose.document.templates.cv.spec.CvHeader;
import com.demcha.compose.document.templates.cv.spec.CvModule;
import com.demcha.compose.document.templates.cv.spec.CvSpec;
import com.demcha.compose.document.theme.BusinessTheme;

DocumentTemplate<CvSpec> template = NordicClean.create(BusinessTheme.modern());
CvSpec spec = CvSpec.builder()
    .header(CvHeader.builder()
        .name("Jane Doe")
        .jobTitle("Backend Java Developer")
        .email("jane@example.com")
        .phone("+1 555 0100")
        .build())
    .module(CvModule.of("Professional Profile",
        new ParagraphBlock(
            "Engineer with **10+ years** of backend experience.")))
    .build();

try (DocumentSession session = GraphCompose.document(out).create()) {
    template.compose(session, spec);
    session.buildPdf();
}
```

The full builder fluent surface and module / block taxonomy are
documented in [`docs/templates/v1-classic/authoring.md`](../templates/v1-classic/authoring.md).
The full gallery of CV / cover-letter renders lives under
[`assets/readme/examples/`](../../assets/readme/examples/) and is
regenerable via
[`CvTemplateGalleryFileExample`](../../examples/src/main/java/com/demcha/examples/templates/cv/CvTemplateGalleryFileExample.java)
and
[`CoverLetterTemplateGalleryFileExample`](../../examples/src/main/java/com/demcha/examples/templates/coverletter/CoverLetterTemplateGalleryFileExample.java).

### What v2 gives you for free

Beyond the rename, the v2 surface picks up:

- **Inline markdown in body strings** — every paragraph, bullet,
  numbered, key-value, indented, and multi-paragraph block parses
  `**bold**` / `*italic*` / `_italic_` markers through
  `MarkdownText.parse(text, baseStyle)`. An LLM-emitted bullet
  like `**Java 21**, SQL, Kotlin` renders Java 21 in bold without
  the caller constructing inline runs by hand.
- **`CvHeader.jobTitle`** — first-class subtitle field rendered
  under the name by presets that surface it (`EditorialBlue`,
  `Panel`, `SidebarPortrait`, `MonogramSidebar`).
- **Active hyperlinks** — header email becomes a `mailto:` link
  and LinkedIn / GitHub labels become clickable `https://` links
  via `DocumentLinkOptions` on the per-run inline runs.
- **Per-preset `RECOMMENDED_MARGIN`** — public constant so
  callers wire the right page margin without magic numbers.
- **Slot-based layouts** — `Panel`, `SidebarPortrait`, and
  `MonogramSidebar` declare named slots (`MAIN`, `SIDEBAR`); a
  custom CV authored through `CvBuilder` can rearrange which
  modules go into which slot via
  `.place(slot, "Module Name", …)`.
- **Single source of truth for spacing** — `Spacing.compact()` /
  `.comfortable()` / `.airy()` replace the three scattered
  `CvTheme.spacing` / `TemplateLayoutPolicy.rootSpacing` /
  `MINIMUM_TOP_LEVEL_MODULE_SPACING` knobs from v1.5.

### Tech debt acknowledged for v1.7

13 of the 14 v2 CV presets are currently implemented as hand-
coded `DocumentTemplate` subclasses (≈ 400-700 LOC each) driving
the canonical PageFlow DSL directly, rather than thin recipes
through the slot-based `CvBuilder`. This was the explicit
trade-off during the Phase E.1 visual-parity reopen — restoring
V1 visual fidelity required components the v2 library hadn't
grown yet (`Panel.softTinted`, `TwoColumnSidebar.tinted`,
`SectionStyle.uppercaseRule`, `WorkEntryRenderer`).

The refactor that turns each hand-coded preset back into a thin
builder recipe is scheduled for v1.7 as Phase E.4. **Public API
shape stays the same** — only the preset internals change, so
callers do not need to migrate again. See
[ADR 0011](adr/0011-templates-v2-architecture.md) for the full
reasoning.

## Deprecations

All of the following continue to work in v1.6 and emit
`@Deprecated(since="1.6.0", forRemoval=true)` warnings. Removal is
planned for v2.0.

### `DocumentDsl.text()` and `DocumentSession.builder()`

These were name-aliases for `paragraph()` and `dsl()` respectively.
Two names for one operation pay maintenance cost without clarity.

```java
// v1.5
flow.builder().paragraph().addText("hi");
session.builder().compose(...);

// v1.6
flow.dsl().paragraph().addText("hi");
session.dsl().compose(...);
```

### PDF-typed chrome overloads on `DocumentSession`

The session exposes a backend-neutral chrome surface
(`metadata(DocumentMetadata)`, `watermark(DocumentWatermark)`,
`protect(DocumentProtection)`, `header(DocumentHeaderFooter)`,
`footer(DocumentHeaderFooter)`) and used to also accept the
PDF-specific `PdfMetadataOptions`/`PdfWatermarkOptions`/
`PdfProtectionOptions`/`PdfHeaderFooterOptions`. The PDF-typed
overloads are deprecated; the canonical overloads remain unchanged.

```java
// v1.5
session.metadata(PdfMetadataOptions.builder().title("Q1").build());
session.watermark(PdfWatermarkOptions.builder().text("DRAFT").build());

// v1.6
session.metadata(DocumentMetadata.builder().title("Q1").build());
session.watermark(DocumentWatermark.builder().text("DRAFT").build());
```

If you legitimately need the PDF-specific options because you're
talking to `PdfFixedLayoutBackend` directly, configure them on the
backend's builder
(`PdfFixedLayoutBackend.builder().metadata(PdfMetadataOptions.builder()…)`).

### `DocumentPalette.of(Color × 7)`

The 7-argument positional factory is brittle: adding or reordering
palette tokens silently shifts every downstream value, and reviewers
cannot tell argument-order mistakes from intent.

```java
// v1.5
DocumentPalette palette = DocumentPalette.of(
    new Color(28, 38, 60),     // primary navy
    new Color(40, 90, 200),    // accent blue
    Color.WHITE,               // surface
    new Color(245, 247, 250),  // muted surface
    new Color(28, 38, 60),     // text primary
    new Color(110, 120, 140),  // text muted
    new Color(210, 218, 230)); // rule

// v1.6 — same colours, names guard against reordering
DocumentPalette palette = DocumentPalette.builder()
    .primary(new Color(28, 38, 60))
    .accent(new Color(40, 90, 200))
    .surface(Color.WHITE)
    .surfaceMuted(new Color(245, 247, 250))
    .textPrimary(new Color(28, 38, 60))
    .textMuted(new Color(110, 120, 140))
    .rule(new Color(210, 218, 230))
    .build();
```

Each setter accepts either a `DocumentColor` or an AWT `Color`. If you
forget a token, `build()` throws `IllegalStateException` listing every
missing field in one message so a single fix-up resolves all gaps.

### `CvTheme` (deleted, see Templates v2 above)

The earlier v1.6 milestones flagged `CvTheme.{nameTextStyle,
sectionHeaderTextStyle, bodyTextStyle, smallBodyTextStyle,
linkTextStyle, moduleMargin}` as deprecated and shipped
canonical-typed companion accessors (`nameStyle()`,
`sectionHeaderStyle()`, …, `moduleInsets()`) returning
`DocumentTextStyle` / `DocumentInsets`. `CvTheme.java` itself was
**deleted** in v1.6 final as part of the Templates v2 restructure,
together with both the engine-typed and the canonical-typed
companion accessors. See
[Templates v2 — CV / cover-letter API rebuilt](#templates-v2--cv--cover-letter-api-rebuilt)
above for the replacement: read the palette from
`BusinessTheme.modern().palette()`, the typography scale from
`BusinessTheme.modern().text()`, and the per-domain padding
tokens from `Spacing.compact()` (under
`com.demcha.compose.document.templates.themes`).

`WeeklyScheduleTheme` is unaffected — `WeeklyScheduleTemplateV1`
remains in `templates/builtins/` and uses the same
v1.5-compatible theme.

## Internal cleanup that may surface elsewhere

These changes are not part of the public surface but may show up if
your project grep'd for them:

- `com.demcha.compose.font.Pdf_FontLoader` (note the underscore) was a
  near-duplicate of `PdfFontLoader` with no callers. Deleted.
- The per-thread TrueType cache in `PdfFontLoader` is now bounded
  (LRU cap of 32 entries per thread). Long-lived servlet/worker threads
  that previously accumulated parsed TTFs forever now evict the
  least-recently-used after the cap.

## Performance changes

v1.6 ships five small render-path improvements
([commit `df8e9c4`](https://github.com/DemchaAV/GraphCompose/commit/df8e9c4)).
None of them alters output bytes; they trim allocations on hot paths:

- `LayoutCompiler.compositeDecorationFragments` /
  `compositeOverlayFragments` no longer wrap their result in
  `List.copyOf`. Two list copies per composite per page touched are
  gone.
- `LayoutCompiler.stableZIndexOrder` short-circuits when every layer
  in a `LayerStackNode` reports the same `zIndex` (the common case
  with default 0). Skips the boxed-array allocation and stable sort.
- `PdfRenderSession` keeps page surfaces in a
  `PDPageContentStream[]` indexed by page rather than a
  `LinkedHashMap<Integer, PDPageContentStream>`. Removes
  `Integer.valueOf` autoboxing on every `pageSurface(int)` call —
  visible at the multi-thousand-fragment scale (~100-page reports).
- `PdfFontLoader.THREAD_LOCAL_TTF_CACHE` is bounded (see "Internal
  cleanup" above).
- One duplicate file deleted (see same).

You should not see any output difference; benchmark numbers should
move in the same direction or be flat.

## What's coming next

v1.6 lays the groundwork for further refactors planned for v1.7
and v2.0:

- **v1.7**: split `BuiltInNodeDefinitions` per node type
  (already landed in v1.6), slim `DocumentSession`, extract
  `PlacementContext` (each is internal — the public surface stays
  stable thanks to the `@Internal` marker). **Templates v2 Phase
  E.4**: refactor the 13 hand-coded CV presets back into thin
  recipes through `CvBuilder` once the v2 component library
  grows the missing primitives (`Panel.softTinted`,
  `TwoColumnSidebar.tinted`, `SectionStyle.uppercaseRule`,
  `WorkEntryRenderer`). Public preset API stays the same.
- **v2.0**: migrate `engine.components.style.{Margin,Padding,…}`
  and `engine.components.content.text.{TextStyle,…}` to canonical
  `document.style.*` types; eventually delete the `engine.*` tree
  and publish a JPMS `module-info.java`.

If you have feedback on this migration, please open an issue at
<https://github.com/DemchaAV/GraphCompose/issues>.
