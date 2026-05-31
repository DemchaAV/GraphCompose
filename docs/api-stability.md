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
| **Stable** | _(default — no annotation)_ | The canonical authoring surface that user code is meant to call: `GraphCompose.document(...)`, `DocumentSession`, `DocumentDsl`, `RowBuilder` / `SectionBuilder` / `ParagraphBuilder` and friends, `DocumentInsets` / `DocumentColor` / `DocumentTextStyle`, the `BusinessTheme` and `CvTheme` factories, the recommended template presets in `cv.v2.*` and `coverletter.v2.*`. | **Major releases only.** |
| **Supported** | _(no annotation; called out in the page's Javadoc)_ | A canonical surface that ships through 1.x but won't be in 2.0 — its replacement is already the Stable path. The `cv.presets.*` "classic" CV preset surface is the only Supported tier in 1.x today (replaced by `cv.v2.*` per [`which-template-system.md`](templates/which-template-system.md)). Bug fixes + behaviour-preserving refactors only. | **Minor releases for behaviour-preserving refactors; removed wholesale in 2.0.** |
| **Extension SPI** | [`@Beta`](../src/main/java/com/demcha/compose/document/api/Beta.java) | Public extension points that authors are expected to **implement**, not only call: render-handler interfaces, [`NodeDefinition`](../src/main/java/com/demcha/compose/document/layout/NodeDefinition.java), custom `Theme` subtype contracts, fragment payload interfaces designed for extension. | Minor releases, with a one-minor deprecation window where possible. |
| **Experimental** | [`@Beta`](../src/main/java/com/demcha/compose/document/api/Beta.java) _(same annotation as Extension SPI; the distinction lives in the docstring on the annotated element)_ | A brand-new public type shipping in its first minor release before its contract has stabilised. The contract is in active flux. | Any minor release, including removal. No deprecation window. |
| **Internal** | [`@Internal`](../src/main/java/com/demcha/compose/document/api/Internal.java) (per-element or per-package) | Engine surface: everything in `com.demcha.compose.document.layout.*`, `com.demcha.compose.engine.*`, render-pipeline payload records, `LayoutCompiler`, `NodeDefinitionSupport`, the placement / measure / split contracts. Technically `public` for cross-package collaboration; not part of the contract. Canonical list lives in [ADR-0003](adr/0003-api-stability-and-internal-marker.md) § *Coverage*. | **Any release.** No deprecation window, no CHANGELOG entry required. |
| **Legacy** | _(no annotation today; flagged in [`which-template-system.md`](templates/which-template-system.md) § 4 and in CHANGELOG `### Deprecations`)_ | Pre-rebuild surface kept only so downstream callers from before the v1.6 rebuild keep compiling: `com.demcha.templates.*` (the original `MainPageCV` / `MainPageCvDTO` / `ModuleYml` / `TemplateBuilder` family), `com.demcha.compose.v2.*` (the original engine-direct builders). Frozen — bug fixes only. | **Removed in 2.0**; no patch / minor changes other than security fixes. |

> Both marker annotations
> ([`@Internal`](../src/main/java/com/demcha/compose/document/api/Internal.java)
> and [`@Beta`](../src/main/java/com/demcha/compose/document/api/Beta.java))
> live in the public `document.api` package and are pinned by
> [`InternalAnnotationCoverageTest`](../src/test/java/com/demcha/documentation/InternalAnnotationCoverageTest.java),
> `InternalAnnotationDocumentationTest`, and `BetaAnnotationDocumentationTest`.
> The Extension SPI seam currently carrying `@Beta` is
> [`NodeDefinition`](../src/main/java/com/demcha/compose/document/layout/NodeDefinition.java);
> additional Extension SPI surfaces (render-handler interfaces,
> fragment-payload interfaces designed for extension) will gain the
> marker incrementally as their contract solidifies.

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

- [`Block`](../src/main/java/com/demcha/compose/document/templates/blocks/Block.java) (Stable)
- [`CvSection`](../src/main/java/com/demcha/compose/document/templates/cv/v2/data/CvSection.java) (Stable)
- [`InlineRun`](../src/main/java/com/demcha/compose/document/node/InlineRun.java) (Stable)
- [`ShapeOutline`](../src/main/java/com/demcha/compose/document/style/ShapeOutline.java) (Stable)
- [`TemplateModuleBlock`](../src/main/java/com/demcha/compose/document/templates/support/common/TemplateModuleBlock.java) (Extension SPI)

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
   on `Block` and hits a `NewlyAddedBlock` it didn't expect, the
   default rendering must visually degrade gracefully — typically by
   delegating to the closest stable variant (often `ParagraphBlock`)
   rather than throwing.
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

### Currently slated for removal in 2.0

See [`docs/templates/which-template-system.md` § 4](templates/which-template-system.md#4-deprecation-inventory--1x--20)
for the full deprecation inventory.

---

## 4. Tier mapping per package

A quick lookup so callers can classify an import without reading
Javadoc per element.

| Package | Tier | Notes |
|---|---|---|
| `com.demcha.compose` (the `GraphCompose` factory class) | **Stable** | The single entry point. |
| `com.demcha.compose.document.api` | **Stable** | `DocumentSession`, `DocumentBuilder`, `PageBackgroundFill`, and the `@Internal` marker itself live here. |
| `com.demcha.compose.document.dsl` | **Stable** | All builder types (`RowBuilder`, `SectionBuilder`, `ParagraphBuilder`, etc.). |
| `com.demcha.compose.document.node` | **Stable** | Node records (`RowNode`, `SectionNode`, `ParagraphNode`, ...). Sealed where relevant — see § 2. |
| `com.demcha.compose.document.style` | **Stable** | `DocumentColor`, `DocumentInsets`, `DocumentTextStyle`, `DocumentTransform`, ... |
| `com.demcha.compose.document.templates.cv.v2.*` | **Stable** | Layered CV presets, `CvDocument`, `CvTheme`. Recommended template surface. |
| `com.demcha.compose.document.templates.coverletter.v2.*` | **Stable** | Layered cover-letter presets. |
| `com.demcha.compose.document.templates.builtins` | **Stable** | `InvoiceTemplateV2`, `ProposalTemplateV2`, `BusinessTheme`. |
| `com.demcha.compose.document.templates.cv.presets.*` | **Stable but Supported** | The "classic" v1.6 rebuild surface. See [`which-template-system.md`](templates/which-template-system.md). Supported through 1.x; removed in 2.0. |
| `com.demcha.compose.document.templates.support.common` | **Extension SPI** | Helpers template authors build new presets against. `@Beta` arrives in Track H2. |
| `com.demcha.compose.document.layout.*` | **Internal** | Marked `@Internal` at the package level. Engine surface. |
| `com.demcha.compose.engine.*` | **Internal** | Engine surface; not part of the public contract regardless of `public` keyword. |
| `com.demcha.templates.*` | **Legacy** | Pre-rebuild surface; removed in 2.0. See [`which-template-system.md`](templates/which-template-system.md). |
| `com.demcha.compose.v2.*` | **Legacy** | Pre-rebuild engine-direct surface; removed in 2.0. |

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
  Stable hierarchies.** See § 2. Switching on a sealed `Block` without
  a `default` branch *will* fail to compile cleanly on the next minor
  release that adds a new permit — by design.

---

## 6. References

- [ADR-0003 — API stability boundary and the `@Internal` marker](adr/0003-api-stability-and-internal-marker.md)
  — the mechanism side (how `@Internal` is wired up and the architecture
  guards that enforce it).
- [ADR-0004 — PDF fragment render handler SPI is public](adr/0004-pdf-handler-spi-extension.md)
  — a worked example of opening an Extension SPI seam.
- [ADR-0011 — Templates v2 architecture](adr/0011-templates-v2-architecture.md)
  and [ADR-0015 — Layered template architecture](adr/0015-layered-template-architecture.md)
  — the architectural justification for the `classic` / `layered`
  template tiers in § 4.
- [`docs/templates/which-template-system.md`](templates/which-template-system.md)
  — the recommended-vs-legacy decision guide for template surfaces;
  this stability policy lives one level up and covers all packages,
  not just templates.
- [`InternalAnnotationCoverageTest`](../src/test/java/com/demcha/documentation/InternalAnnotationCoverageTest.java)
  and [`InternalAnnotationDocumentationTest`](../src/test/java/com/demcha/compose/document/api/InternalAnnotationDocumentationTest.java)
  — the architecture guards that fail the build if the package-level
  `@Internal` marker disappears from `document.layout` or the
  annotation's contract drifts from this policy.

---

*This page is maintained in lockstep with the public surface. When a
new public package lands, a sealed hierarchy gains a permit, or a
deprecation crosses its window, update §1 (tier matrix), §2 (sealed
policy if relevant), §3 (deprecation table), and §4 (package tier
lookup) in the same commit.*
