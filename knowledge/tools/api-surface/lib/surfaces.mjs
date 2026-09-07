/**
 * knowledge/tools/api-surface/lib/surfaces.mjs — which surface a public type
 * belongs to, and how stable it is.
 *
 * Two independent questions, answered in two stages, because fusing them is a
 * bug that hides: `@Beta` says how mature a type is, never whether it is API at
 * all. `NodeDefinition` is `@Beta` and public and sits inside package-`@Internal`
 * `com.demcha.compose.document.layout`; it is Extension SPI because it is
 * *named* as such below, not because it is `@Beta`. A `@Beta` type inside an
 * `@Internal` package with no rule behind it stays excluded.
 *
 *   Stage A — admission   (this file's ADMISSION ladder)
 *   Stage B — stability   (type/package `@Beta`)
 *   Stage C — members     (member `@Internal` / `@Beta`, else inherit)
 *
 * Every public type must come out of Stage A with exactly one outcome. A type
 * that matches nothing is a hard error, not a silent drop: a silent drop is how
 * the 2.0 module split removed three modules from the allow-list without anyone
 * noticing.
 */

export const INTERNAL = "com.demcha.compose.document.api.Internal";
export const BETA = "com.demcha.compose.document.api.Beta";

/**
 * Extension SPI — the A2 admission list.
 *
 * `docs/api-stability.md:33` defines the category as the public extension
 * points "authors are expected to **implement**, not only call: render-handler
 * interfaces, NodeDefinition, custom Theme subtype contracts, fragment payload
 * interfaces designed for extension."
 *
 * This list has to be authored rather than derived, and that is not a
 * convenience. `api-stability.md:34` gives Extension SPI and Experimental the
 * *same* `@Beta` annotation and says outright that the distinction "lives in the
 * docstring on the annotated element". There is no byte-code signal that
 * separates them, so no classifier can route between the two surfaces on its
 * own.
 *
 * `PptxFragmentRenderHandler` is here rather than in `backends` even though its
 * package is Experimental: it is a render-handler interface, which is the
 * line-33 definition, and `PdfFragmentRenderHandler` is the identical seam at a
 * different maturity. Routing one to `extension-spi` and its twin to `backends`
 * would sort two identical things by how new they are — which is what the
 * surface/stability split exists to stop. Maturity is carried by
 * `stability: beta` instead.
 */
export const EXTENSION_SPI = new Set([
  // Named explicitly in api-stability.md:33.
  "com.demcha.compose.document.layout.NodeDefinition",
  // Backend seams: implement one to teach the engine a new output format.
  "com.demcha.compose.document.backend.fixed.FixedLayoutBackend",
  "com.demcha.compose.document.backend.fixed.FixedLayoutBackendProvider",
  "com.demcha.compose.document.backend.fixed.FixedLayoutRenderer",
  "com.demcha.compose.document.backend.fixed.FontMetricsProvider",
  "com.demcha.compose.document.backend.fixed.MeasurementResources",
  "com.demcha.compose.document.backend.semantic.SemanticBackend",
  // Render-handler interfaces: implement one to draw a fragment kind yourself.
  "com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler",
  "com.demcha.compose.document.backend.fixed.pptx.PptxFragmentRenderHandler",
]);

/**
 * A4 — excluded by name, with a reason.
 *
 * This branch exists for what the annotations cannot say.
 * `com.demcha.compose.document.dsl.internal` is not annotated `@Internal` at
 * package level, yet `BuilderSupport` and `SemanticNameNormalizer` are public
 * and are plainly not authoring API. Without A4 they reach A6 and fail the
 * build — a correct signal with no way to act on it, because adding the missing
 * annotation is a public-API change this tooling is not allowed to make.
 *
 * Every entry here is a standing admission that the source lacks an annotation
 * it should have. Each should carry a follow-up issue rather than sit here
 * indefinitely; the reason string is what makes that visible in
 * `knowledge/api/excluded.json`.
 */
export const EXPLICIT_EXCLUSIONS = [
  {
    match: (binaryName) => binaryName.startsWith("com.demcha.compose.document.dsl.internal."),
    reason:
      "package `document.dsl.internal` is implementation detail but carries no " +
      "package-level @Internal; excluded by name until the annotation is added",
  },
];

/**
 * A5 — the surface rules, in order. First match wins.
 *
 * Keyed on the package a type is declared in. Ordering matters only where one
 * prefix contains another: `document.backend.fixed.pdf` must be tested before
 * `document.backend`, so the more specific entries come first.
 */
export const SURFACE_RULES = [
  // --- testing -------------------------------------------------------------
  { surface: "testing", prefix: "com.demcha.compose.testing" },

  // --- backends ------------------------------------------------------------
  // Everything public under a render backend that is not an SPI seam (A2) and
  // not @Internal: the concrete backends, their options, their built-in
  // handlers. Experimental ones land here too — beta is a stability, not a
  // surface.
  { surface: "backends", prefix: "com.demcha.compose.document.backend" },

  // --- templates -----------------------------------------------------------
  { surface: "templates", prefix: "com.demcha.compose.document.templates" },

  // --- authoring -----------------------------------------------------------
  // What a person composing a document calls.
  { surface: "authoring", prefix: "com.demcha.compose.document.api" },
  { surface: "authoring", prefix: "com.demcha.compose.document.dsl" },
  { surface: "authoring", prefix: "com.demcha.compose.document.style" },
  { surface: "authoring", prefix: "com.demcha.compose.document.table" },
  { surface: "authoring", prefix: "com.demcha.compose.document.chart" },
  { surface: "authoring", prefix: "com.demcha.compose.document.node" },
  { surface: "authoring", prefix: "com.demcha.compose.document.image" },
  { surface: "authoring", prefix: "com.demcha.compose.document.svg" },
  { surface: "authoring", prefix: "com.demcha.compose.document.output" },
  // The layout read model. It reads like engine internals, but
  // `session.layoutSnapshot()` returns these records, so leaving them out left
  // an agent able to obtain a snapshot and — under the closed-set rule — not
  // allowed to read a single field of it.
  { surface: "authoring", prefix: "com.demcha.compose.document.snapshot" },
  { surface: "authoring", prefix: "com.demcha.compose.document.showcase" },
  { surface: "authoring", prefix: "com.demcha.compose.document.exceptions" },
  { surface: "authoring", prefix: "com.demcha.compose.font" },
  // `GraphCompose` and its nested `DocumentBuilder` — the entry point. Matched
  // on the type rather than the package, because `com.demcha.compose` is the
  // root of everything and admitting the package would admit the whole library.
  { surface: "authoring", exactType: "com.demcha.compose.GraphCompose" },
];

export const SURFACES = ["authoring", "templates", "backends", "testing", "extension-spi"];

/**
 * Packages whose public types are implementation unless the surface reaches
 * them.
 *
 * This is not a fifth exclusion rule — it is a statement about where the hard
 * error applies. A public type these roots contain, that no admitted API
 * mentions, is excluded with a reason rather than failing the run; a public type
 * *outside* them that matches nothing still fails the run, because that is a
 * package nobody has ruled on and silence there is how the 2.0 split lost three
 * modules.
 *
 * It has to be consulted AFTER the reachability pass, never as an A4 rule.
 * `com.demcha.compose.engine.components` is inside this root and holds
 * `TextStyle`, `DocumentMetadata`, `ImageData`, `Padding` and `Anchor` — types
 * the public API hands you and you must construct. Excluding the root up front
 * would drop every one of them; letting reachability speak first keeps them and
 * still drops their engine-side twins (`HeaderFooterConfig` beside the public
 * `DocumentHeaderFooter`, `WatermarkConfig` beside `DocumentWatermark`), which
 * nothing public mentions.
 */
export const IMPLEMENTATION_ROOTS = [
  "com.demcha.compose.engine",
  "com.demcha.compose.document.debug",
  "com.demcha.compose.document.emoji",
];

export const UNREFERENCED_REASON =
  "implementation package, and no admitted API mentions it";

export const inImplementationRoot = (packageName) =>
  IMPLEMENTATION_ROOTS.some((root) => packageName === root || packageName.startsWith(`${root}.`));

const inPackage = (binaryName, prefix) =>
  binaryName === prefix || binaryName.startsWith(`${prefix}.`);

/**
 * Stage A. Returns `{admitted: true, surface}` or
 * `{admitted: false, reason}`; `reason === null` means nothing matched, which
 * the caller must treat as a hard error rather than an exclusion.
 */
export function admit(type) {
  const { binaryName, packageName, annotations, packageAnnotations } = type;

  // A1 — the type says so itself.
  if (annotations.includes(INTERNAL)) {
    return { admitted: false, reason: "type @Internal" };
  }

  // A2 — an authored SPI seam, admitted even out of an @Internal package.
  if (EXTENSION_SPI.has(binaryName)) {
    return { admitted: true, surface: "extension-spi" };
  }

  // A3 — the package says so. Weaker than A2 by design: a package annotation
  // was written before the type inside it was deliberately opened.
  if (packageAnnotations.includes(INTERNAL)) {
    return { admitted: false, reason: `package @Internal (${packageName})` };
  }

  // A4 — excluded by name, because the source has no annotation to lean on.
  for (const rule of EXPLICIT_EXCLUSIONS) {
    if (rule.match(binaryName)) return { admitted: false, reason: rule.reason };
  }

  // A5 — the surface rules. `exactType` matches the type and its nested types,
  // so `GraphCompose` brings `GraphCompose$DocumentBuilder` with it.
  for (const rule of SURFACE_RULES) {
    const hit = rule.exactType
      ? binaryName === rule.exactType || binaryName.startsWith(`${rule.exactType}$`)
      : inPackage(packageName, rule.prefix);
    if (hit) return { admitted: true, surface: rule.surface };
  }

  // A6 — nothing matched. The caller fails the run.
  return { admitted: false, reason: null };
}

/**
 * Stage B. Resolved per type — including each nested type separately, never per
 * file: `DocumentPaint` is stable while the records nested inside it,
 * `DocumentPaint.LinearAxis` and `.RadialCircle`, are both `@Beta`.
 */
export function stability(type) {
  if (type.annotations.includes(BETA)) return "beta";
  if (type.packageAnnotations.includes(BETA)) return "beta";
  return "stable";
}

/**
 * Stage C. `null` means the member is excluded outright; otherwise the
 * stability to record on it.
 */
export function memberStability(memberAnnotations, typeStability) {
  if (memberAnnotations.includes(INTERNAL)) return null;
  if (memberAnnotations.includes(BETA)) return "beta";
  return typeStability;
}
