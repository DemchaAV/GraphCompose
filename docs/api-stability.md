# API Stability Policy

GraphCompose follows **Semantic Versioning** (major.minor.patch). This
page is the user-facing contract for which parts of the public surface
that promise covers, what breaking changes are allowed in each release
type, and how sealed hierarchies, deprecations, and unannounced
internal changes are handled.

The mechanism side of the same decision — how `@Internal` is wired up
and which guard tests enforce it — lives in
[ADR-0003](adr/0003-api-stability-and-internal-marker.md). This page is
the **policy** that ADR's mechanism enforces.

---

## 1. Stability tiers

Every public class, method, field, and annotation that lives under
`com.demcha.compose.*` falls into exactly one of six tiers. The tier
is signalled by the package it lives in (with one exception, `@Internal`,
which can appear on individual elements too) and by an explicit
annotation marker where one exists.

The **Supported** and **Legacy** tiers mirror the same labels used in
[`docs/templates/which-template-system.md` § 1](templates/which-template-system.md):
this page is the package-wide version of that template-only status
matrix.

| Tier | Marker | Used for | Breaking changes allowed in |
|---|---|---|---|
| **Stable** | _(default — no annotation)_ | The canonical authoring surface that user code is meant to call: `GraphCompose.document(...)`, `DocumentSession`, `DocumentDsl`, `RowBuilder` / `SectionBuilder` / `ParagraphBuilder` and friends, `DocumentInsets` / `DocumentColor` / `DocumentTextStyle`, the `BrandTheme` factories, and the layered template presets (`templates.cv.*`, `templates.coverletter.*`, `templates.invoice.*`, `templates.proposal.*`). | **Major releases only.** |
| **Supported** | _(no annotation; called out in the page's Javadoc)_ | A canonical surface that ships through a major line but won't be in the next one — its replacement is already the Stable path. Bug fixes + behaviour-preserving refactors only. *(No package holds this tier on the 2.0 line; the classic `cv.presets.*` CV surface held it through 1.x and was removed in 2.0 per [`which-template-system.md`](templates/which-template-system.md).)* | **Minor releases for behaviour-preserving refactors; removed wholesale in the next major.** |
| **Extension SPI** | [`@Beta`](../core/src/main/java/com/demcha/compose/document/api/Beta.java) | Public extension points that authors are expected to **implement**, not only call: render-handler interfaces, [`NodeDefinition`](../core/src/main/java/com/demcha/compose/document/layout/NodeDefinition.java), custom `Theme` subtype contracts, fragment payload interfaces designed for extension. | Minor releases, with a one-minor deprecation window where possible. |
| **Experimental** | [`@Beta`](../core/src/main/java/com/demcha/compose/document/api/Beta.java) _(same annotation as Extension SPI; the distinction lives in the docstring on the annotated element)_ | A brand-new public type shipping in its first minor release before its contract has stabilised. The contract is in active flux. | Any minor release, including removal. No deprecation window. |
| **Internal** | [`@Internal`](../core/src/main/java/com/demcha/compose/document/api/Internal.java) (per-element or per-package) | Engine surface: everything in `com.demcha.compose.document.layout.*`, `com.demcha.compose.engine.*`, render-pipeline payload records, `LayoutCompiler`, `NodeDefinitionSupport`, the placement / measure / split contracts. Technically `public` for cross-package collaboration; not part of the contract. Canonical list lives in [ADR-0003](adr/0003-api-stability-and-internal-marker.md) § *Coverage*. | **Any release.** No deprecation window, no CHANGELOG entry required. |
| **Legacy** | _(no annotation; flagged in [`which-template-system.md`](templates/which-template-system.md) and in CHANGELOG `### Deprecations`)_ | Pre-rebuild surface kept only so callers from before a major rebuild keep compiling. Frozen — bug fixes only. *(No package holds this tier on the 2.0 line; `com.demcha.templates.*` and `com.demcha.compose.v2.*` held it through 1.x and were removed in 2.0 — the migration target is the canonical DSL.)* | **Removed in the next major**; no patch / minor changes other than security fixes. |

> Both marker annotations
> ([`@Internal`](../core/src/main/java/com/demcha/compose/document/api/Internal.java)
> and [`@Beta`](../core/src/main/java/com/demcha/compose/document/api/Beta.java))
> live in the public `document.api` package and are pinned by
> [`InternalAnnotationCoverageTest`](../core/src/test/java/com/demcha/documentation/InternalAnnotationCoverageTest.java),
> `InternalAnnotationDocumentationTest`, and `BetaAnnotationDocumentationTest`.
> The Extension SPI seam currently carrying `@Beta` is
> [`NodeDefinition`](../core/src/main/java/com/demcha/compose/document/layout/NodeDefinition.java);
> additional Extension SPI surfaces (render-handler interfaces,
> fragment-payload interfaces designed for extension) will gain the
> marker incrementally as their contract solidifies.
>
> The Experimental surface currently carrying `@Beta` is the **fixed-layout
> PPTX backend**, shipping its first release in 2.1.0: the
> `document.backend.fixed.pptx` and `…pptx.handlers` packages (package-level
> marker plus explicit markers on `PptxFixedLayoutBackend`,
> `PptxFixedLayoutBackendProvider`, `PptxFragmentRenderHandler`,
> `PptxRenderEnvironment`, `PptxCoordinates`) and the PPTX convenience
> methods on `DocumentSession` (`toPptxBytes`, `writePptx`, `buildPptx`).
> Geometry identity with the PDF backend is a design invariant and will not
> change; the API shape around it may still move in a minor release.

### What each tier promises

- **Stable** — your code that imports a Stable type compiles and runs against the next 1.x.y release without code changes; behaviour is preserved across patch releases and additive in minor releases. A removal is a major-version event called out in the CHANGELOG migration section.
- **Extension SPI** — implementations you wrote against the SPI continue to load in any patch release. In a minor release the SPI **may** require small adaptations; the previous shape is `@Deprecated` for at least one minor release first, and the CHANGELOG entry calls out the migration explicitly.
- **Internal** — no promises. The shape can change in any release without notice; CHANGELOG entries are optional and usually omitted to keep the user-facing changelog focused on the public surface. If you imported an `@Internal` type, you opted out of the stability contract — please open an issue so a stable wrapper can be designed.
- **Experimental** — no promises within minor releases. We ship Experimental APIs to gather feedback before locking the shape. Once the contract stabilises (typically by the next minor release) the annotation is dropped and the type joins **Stable** or **Extension SPI**; the CHANGELOG transition is called out explicitly.

---

## 2. Sealed hierarchy policy

GraphCompose uses sealed interfaces in several places to keep visitor
code exhaustive. The public ones — the ones this policy actually covers —
are:

- [`ChartSize`](../core/src/main/java/com/demcha/compose/document/chart/ChartSize.java) (Stable)
- [`ChartSpec`](../core/src/main/java/com/demcha/compose/document/chart/ChartSpec.java) (Stable)
- [`CvSection`](../templates/src/main/java/com/demcha/compose/document/templates/cv/data/CvSection.java) (Stable)
- [`DocumentLinkTarget`](../core/src/main/java/com/demcha/compose/document/node/DocumentLinkTarget.java) (Stable)
- [`DocumentPaint`](../core/src/main/java/com/demcha/compose/document/style/DocumentPaint.java) (Stable)
- [`DocumentPathSegment`](../core/src/main/java/com/demcha/compose/document/style/DocumentPathSegment.java) (Stable)
- [`InlineRun`](../core/src/main/java/com/demcha/compose/document/node/InlineRun.java) (Stable)
- [`ShapeOutline`](../core/src/main/java/com/demcha/compose/document/style/ShapeOutline.java) (Stable)

Sealed types under `@Internal` packages — `ParagraphSpan` and
`PlacementContext` — are outside this policy by definition; their permit
list can change in any release without notice.

A `sealed interface X permits A, B, C` carries a stronger contract than
a regular interface: every implementation is known to the compiler, so
a `switch (block)` over the permits list can be exhaustive.
**Adding a new permit is therefore a breaking change** for any caller
that switches on the sealed type without a `default` branch — even
though it's purely additive at the source level.

### Policy

1. **Stable sealed hierarchies are additive in minor releases only when
   the new variant carries a sensible default rendering for callers
   that did not switch on it.** Concretely: if a caller pattern-matches
   on `CvSection` and hits a newly added section variant it didn't
   expect, the default rendering must visually degrade gracefully —
   typically by delegating to the closest stable variant (often
   `ParagraphSection`) rather than throwing.
2. **The CHANGELOG entry for the minor release names the new permit
   explicitly** under `### Public API` so callers know to audit their
   visitor code. Example wording, from the v1.6.4 cut:
   > Added two new public Block types — `WorkHistoryBlock` and
   > `EducationBlock` — that let template authors declare work-history
   > and education entries with explicit fields. The sealed `Block`
   > permit list grows from six to eight; existing `MultiParagraphBlock`
   > work-history strings continue to parse.
3. **Internal sealed hierarchies have no permit-list policy.** The
   compiler enforces exhaustiveness for engine code; the public contract
   doesn't surface them at all.
4. **Removing a permit is a major-version event** for any tier other
   than `@Internal`.

The same policy applies to sealed *classes* (records and class
hierarchies). The mechanism is identical.

---

## 3. Deprecation window

A Stable API element marked `@Deprecated` is removed only in a major
release, and only after the deprecation has been in effect for at least
one full minor release.

### Rules

| Tier | Minimum deprecation window | Removed in |
|---|---|---|
| **Stable** | ≥ 1 minor release with `@Deprecated`. | Major. |
| **Supported** | Already deprecated by category — entire tier is removed in the next major. | Major (entire tier). |
| **Extension SPI** | ≥ 1 minor release with `@Deprecated`. | Next minor that calls out the migration in CHANGELOG `### Public API`. |
| **Experimental** | None required. | Any minor. |
| **Internal** | None required. | Any. |
| **Legacy** | Already deprecated by category — frozen at current shape, removed in next major. | Major (entire tier). |

### What a deprecation must include

Every `@Deprecated` element ships with a Javadoc note pointing to its
replacement. The format — illustrated with placeholder type names; the
real shape uses the actual canonical replacement:

```java
/**
 * @deprecated since 1.X.0; removed in 2.0.
 *             Use {@link com.demcha.compose.canonical.ReplacementType#replacement(...)} instead.
 *             The migration is one of:
 *               - same shape, different package — swap the import;
 *               - same name, narrower contract — adjust call sites per ADR-NNN;
 *               - no replacement, the problem itself moved — see CHANGELOG migration note.
 *             Pick the bullet that applies.
 */
@Deprecated(forRemoval = true, since = "1.X.0")
public static LegacyReturn legacyMethod(LegacyArg arg) { ... }
```

If a migration target exists, link it with `{@link ...}`. If the
deprecation is *"this will simply go away in 2.0 and there is no
replacement because the problem itself moved,"* say so explicitly in
prose so the reader knows not to look for one.

### Currently slated for removal in the next major

Two inventories track what is deferred to the next major:

- **Template surfaces and pre-rebuild aliases** — the 1.x → 2.0 removals
  are done on the 2.0 line; the migration map lives in
  [`docs/templates/which-template-system.md` § 3](templates/which-template-system.md#3-migrating-a-pre-20-caller).
- **Every other public-API rework, simplification, or deprecation** — the ledger below.

#### 2.0 breaking-changes ledger

A row lands here during senior review (the `graphcompose-senior-review` skill,
Lane 1.5 "accept a compromise" step) whenever a **Stable** public API ships in a shape
we already intend to change but can't break in 1.x.

The cheaper path is to ship a new or unsure shape `@Beta` (Experimental) instead — then
there is *no debt and no row*, because an Experimental API may change in any minor
release (§ 1). A row exists only for compromises that had to land in the **Stable**
tier, which can change only in a major release. A `// TODO(v2)` code comment is **not**
a record — it rots and nothing tracks it; the record is a row here, plus an ADR
`## Consequences` note when the compromise reflects a real design decision.

When the 2.0 cycle opens, a `planned` row's API is marked
`@Deprecated(forRemoval = true, since = "1.x.0")` per the format above, the deprecation
window starts, and its `Status` flips to `deprecated 1.x`.

| Element | Tier now | Status | Why the 1.x shape is a compromise | 2.0 action | ADR | Issue |
|---|---|---|---|---|---|---|
| `DocumentSession.pageMargins(List<PageMarginRule>)` / `PageMarginRule` | Stable | planned | Per-page margins resolve a block's content width by the page it *begins* on (the engine measures each block once, before pagination). A margin that changes the content width therefore does not re-wrap a block mid-flow across a page boundary. | Revisit a page-aware per-line/per-fragment width model so a block can re-wrap when it crosses a margin boundary, if demand warrants. | — | — |
| `io.github.demchaav:graph-compose` single-jar packaging | Stable | **landed 2.0** | The one published jar bundled the engine, the PDFBox render backend, the POI semantic backend, zxing, and the template families, so an engine-only or bring-your-own-backend consumer still pulled all of them. | **Done in 2.0.** Split into per-concern lockstep modules, render backends discovered via a `ServiceLoader` SPI. The root coordinate is renamed `graph-compose-core` (the lean engine); `graph-compose` is kept as a back-compat wrapper over `graph-compose-core` + `graph-compose-render-pdf`, so it still renders PDF out of the box. Templates are opt-in (`graph-compose-templates`); DOCX / PPTX ship in `graph-compose-render-docx` / `-render-pptx`. Migration: [modules guide](migration/v2.0.0-modules.md). | [ADR 0016](adr/0016-multi-module-packaging.md) | — |

### Binary-compatibility enforcement

The Stable-tier promise (§ 1 — no binary breaks outside a major release) is enforced
mechanically by [japicmp](https://siom79.github.io/japicmp/), run in a `japicmp` Maven
profile on the engine module during `verify`.

- **Baseline:** the published `graph-compose-core` on Maven Central, pinned by the
  `japicmp.baseline` property in `core/pom.xml`. It is the current major's **floor** —
  `2.0.0` for the whole 2.x line — and advances only at the next major. Holding it at
  the floor (rather than the previous release) is what enforces the Stable promise:
  every 2.x build must stay binary-compatible with the `2.0.0` public surface, not
  merely with the last minor.
- **What fails the build:** any binary-incompatible change to the public surface
  against the baseline — a removed or less-accessible public method/field/type, a
  changed signature, and so on. `@Internal` packages (`com.demcha.compose.engine.*`,
  `com.demcha.compose.document.layout.*` and its render-handoff payload records) are
  excluded; they carry no compatibility promise (§ 1). Source-only incompatibilities
  (e.g. adding a default method to an interface) are reported but do not fail, pending
  a finalized 2.x source-compatibility policy.
- **Activity window:** the gate compares the working version against the baseline, so
  it is a no-op only when the two are equal — the `2.0.0` release commit itself — and
  active for every `-SNAPSHOT` development cycle across the 2.x line that follows.

During the 2.0 major transition the gate ran report-only (the major intentionally
broke 1.x binary compatibility); it enforces from the `2.0.0` baseline forward.

---

## 4. Tier mapping per package

A quick lookup so callers can classify an import without reading
Javadoc per element.

| Package | Tier | Module | Notes |
|---|---|---|---|
| `com.demcha.compose` (the `GraphCompose` factory class) | **Stable** | `graph-compose-core` | The single entry point. |
| `com.demcha.compose.document.api` | **Stable** | `graph-compose-core` | `DocumentSession`, `DocumentBuilder`, `PageBackgroundFill`, and the `@Internal` marker itself live here. |
| `com.demcha.compose.document.dsl` | **Stable** | `graph-compose-core` | All builder types (`RowBuilder`, `SectionBuilder`, `ParagraphBuilder`, etc.). |
| `com.demcha.compose.document.node` | **Stable** | `graph-compose-core` | Node records (`RowNode`, `SectionNode`, `ParagraphNode`, ...). Sealed where relevant — see § 2. |
| `com.demcha.compose.document.style` | **Stable** | `graph-compose-core` | `DocumentColor`, `DocumentInsets`, `DocumentTextStyle`, `DocumentTransform`, ... |
| `com.demcha.compose.document.showcase` | **Stable** | `graph-compose-core` | `FontShowcase` — bundled-font preview renderer. |
| `com.demcha.compose.document.templates.api` | **Stable** | `graph-compose-templates` | The `DocumentTemplate<S>` seam every preset factory returns. |
| `com.demcha.compose.document.templates.core.*` | **Stable** | `graph-compose-templates` | The shared, family-neutral template layer — `BrandTheme` tokens (`core.theme`), neutral header bricks (`core.identity`), text helpers (`core.text`), shared widgets (`core.widgets`). |
| `com.demcha.compose.document.templates.cv.*` | **Stable** | `graph-compose-templates` | Layered CV family — `CvDocument` data, components, widgets, presets. |
| `com.demcha.compose.document.templates.coverletter.*` | **Stable** | `graph-compose-templates` | Layered cover-letter family. |
| `com.demcha.compose.document.templates.invoice.*` | **Stable** | `graph-compose-templates` | Layered invoice family — `ModernInvoice` on `InvoiceDocumentSpec`. |
| `com.demcha.compose.document.templates.proposal.*` | **Stable** | `graph-compose-templates` | Layered proposal family — `ModernProposal` on `ProposalDocumentSpec`. |
| `com.demcha.compose.document.templates.data.*` | **Stable** | `graph-compose-templates` | Family-neutral document data records (invoice / proposal / schedule specs). |
| `com.demcha.compose.document.backend.fixed.pptx` | **Experimental** | `graph-compose-render-pptx` | Marked `@Beta` at the package level — `PptxFixedLayoutBackend`, its builder, and `PptxFixedLayoutBackendProvider`. First shipped in 2.1.0. |
| `com.demcha.compose.document.backend.fixed.pptx.handlers` | **Experimental** | `graph-compose-render-pptx` | Marked `@Beta` at the package level — the `PptxFragmentRenderHandler` seam and its built-in handlers. |
| `com.demcha.compose.document.layout.*` | **Internal** | `graph-compose-core` | Marked `@Internal` at the package level. Engine surface. |
| `com.demcha.compose.engine.*` | **Internal** | `graph-compose-core` | Engine surface; not part of the public contract regardless of `public` keyword. `engine.render.pdf.*` ships in `graph-compose-render-pdf`. |

---

## 5. What we don't promise (anti-policy)

- **Pixel-stable PDF output across patch releases.** The layout engine
  preserves *structural* invariants (page count, fragment ordering,
  cell-content order) under semver, but pixel-exact rendering can
  shift by a few sub-pixels when PDFBox bumps, font metrics change, or
  a kerning fix lands. Layout regression tests (see
  `LayoutSnapshotRegressionExample` in the examples README) capture
  structure, not pixels; visual regression tests (`*VisualRegressionTest`)
  ship with calibrated `mismatchedPixelBudget` values rather than zero.
- **Bit-stable artefact bytes.** PDFs include creation timestamps,
  resource ordering hashes, and other metadata that can vary even when
  output is visually identical. Compare semantically, not by file hash.
- **Internal package shape across releases.** See § 1, tier Internal.
- **Sealed hierarchy permits' exhaustiveness across minor releases for
  Stable hierarchies.** See § 2. Switching on a sealed `CvSection`
  without a `default` branch *will* fail to compile cleanly on the next
  minor release that adds a new permit — by design.

---

## 6. References

- [ADR-0003 — API stability boundary and the `@Internal` marker](adr/0003-api-stability-and-internal-marker.md)
  — the mechanism side (how `@Internal` is wired up and the architecture
  guards that enforce it).
- [ADR-0004 — PDF fragment render handler SPI is public](adr/0004-pdf-handler-spi-extension.md)
  — a worked example of opening an Extension SPI seam.
- [ADR-0015 — Layered template architecture](adr/0015-layered-template-architecture.md)
  — the architectural justification for the layered template packages in
  § 4 ([ADR-0011](adr/0011-templates-v2-architecture.md) documents the
  removed classic predecessor).
- [`docs/templates/which-template-system.md`](templates/which-template-system.md)
  — the template naming history and the migration map for pre-2.0
  callers; this stability policy lives one level up and covers all
  packages, not just templates.
- [`InternalAnnotationCoverageTest`](../core/src/test/java/com/demcha/documentation/InternalAnnotationCoverageTest.java)
  and [`InternalAnnotationDocumentationTest`](../core/src/test/java/com/demcha/compose/document/api/InternalAnnotationDocumentationTest.java)
  — the architecture guards that fail the build if the package-level
  `@Internal` marker disappears from `document.layout` or the
  annotation's contract drifts from this policy.

---

*This page is maintained in lockstep with the public surface. When a
new public package lands, a sealed hierarchy gains a permit, or a
deprecation crosses its window, update §1 (tier matrix), §2 (sealed
policy if relevant), §3 (deprecation table), and §4 (package tier
lookup) in the same commit.*
