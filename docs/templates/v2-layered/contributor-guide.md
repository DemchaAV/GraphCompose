# Contributor Guide — add a new template family

You're a GraphCompose contributor and you want to bring a brand-new
document type onto the layered architecture — a report, a
statement, anything the shipped families do not already cover. This
doc is your checklist + convention reference.

It assumes you've read [quickstart.md](quickstart.md) and
[authoring-presets.md](authoring-presets.md) — those explain *why*
the layered pattern looks the way it does.

---

## Table of contents

1. [The layer convention](#the-5-layer-convention)
2. [Package map for a new family](#package-map-for-a-new-family)
3. [Naming rules](#naming-rules)
4. [Worked walkthrough — `invoice`](#worked-walkthrough)
5. [Required deliverables](#required-deliverables)
6. [Test checklist](#test-checklist)
7. [Doc checklist](#doc-checklist)
8. [What you must NOT do](#what-you-must-not-do)
9. [PR review expectations](#pr-review-expectations)

---

<a id="the-5-layer-convention"></a>
## The layer convention

Every new template family lives under
`com.demcha.compose.document.templates.<family>` with these
sub-packages:

```
<family>/
├── data/         records describing what's on the page
├── components/   internal renderers + low-level primitives
├── widgets/      reusable visual LEGO bricks — omit if the family has none
└── presets/      composition: data + theme + widgets → DocumentTemplate
```

Cosmetic tokens are **not** per-family. `templates.core.theme` holds
`BrandTheme` and its parts (`Palette`, `Typography`, `Spacing`,
`Decoration`), and every family reads from it — that is what keeps two
presets from two families looking like one product. A family that needs
a token nobody else has adds it there, not to a package of its own.

Each layer's contract:

| Layer | Contract |
|---|---|
| `data/` | Pure records. Zero dependencies on rendering, theming, or DSL. Sealed hierarchy for section / block subtypes. |
| `templates.core.theme` | Shared, not per-family. `BrandTheme(palette, typography, spacing, decoration)` plus its parts. Records + factories, no rendering logic. |
| `components/` | Static helpers. Take `(host, data, theme)`. No statics holding state. No magic numbers — read tokens from theme. |
| `widgets/` | Static helpers. Named factory methods per visual variant. Compose internally from `components/`. Optional — the cover-letter family has none. |
| `presets/` | One `public final class` per visual style. Two factories: `create()` and `create(BrandTheme)`. Inner `Template` implements `DocumentTemplate<<Family>Document>`. |

**The convention is the same regardless of domain.** A cv has
identity + sections; an invoice has parties + line items + totals; a
proposal has scope + pricing + acceptance terms. The *shape* of the
data differs; the *layering* is identical.

---

<a id="package-map-for-a-new-family"></a>
## Package map for a new family

Mirror the CV layout. Concrete example for invoice:

```
templates/src/main/java/com/demcha/compose/document/templates/invoice/
├── package-info.java                   ← ASCII diagram + 4-step walkthrough
├── AUTHORS.md                          ← recipe cookbook
├── data/
│   ├── package-info.java
│   ├── InvoiceDocument.java            ← root record
│   ├── InvoiceParty.java               ← from / bill-to
│   ├── InvoiceLine.java                ← line item record
│   ├── InvoiceTotals.java              ← subtotal / tax / total
│   ├── InvoiceSection.java             ← sealed interface
│   ├── HeaderSection.java              ← concrete subtype
│   ├── LineItemsSection.java           ← concrete subtype
│   ├── TotalsSection.java              ← concrete subtype
│   ├── NotesSection.java               ← concrete subtype
│   └── Slot.java                       ← if multi-column variants needed
├── components/
│   ├── package-info.java
│   ├── ParagraphPrimitive.java         ← internal — package-private
│   ├── LineRowRenderer.java            ← one line item row
│   ├── TotalsBlockRenderer.java        ← right-aligned totals stack
│   ├── PartyBlockRenderer.java         ← from / bill-to address block
│   ├── SectionDispatcher.java          ← branches on sealed subtype
│   └── … (other internal renderers)
├── widgets/
│   ├── package-info.java
│   ├── Letterhead.java                 ← top-of-doc invoice header
│   ├── PartyPair.java                  ← from + bill-to side-by-side
│   ├── LineTable.java                  ← line items table
│   └── TotalsCard.java                 ← totals summary
└── presets/
    ├── package-info.java
    ├── ClassicInvoice.java             ← reference preset
    └── (more as added)
```

The structure is **identical** to cv — only the records inside
differ.

---

<a id="naming-rules"></a>
## Naming rules

- **Family prefix** on top-level records to avoid name collisions.
  CV uses `CvName`, `CvIdentity`, `CvSection`. Invoice should use
  `InvoiceParty`, `InvoiceLine`. Cover letter:
  `CoverLetterRecipient`, `CoverLetterBody`. Etc.
- **`<Family>Document`** for the root record. (`CvDocument`,
  `InvoiceDocument`, `CoverLetterDocument`.)
- **`<Family>Section` (sealed)** for the body content hierarchy.
- **No `<Family>Theme`.** The aggregate theme record is the shared
  `BrandTheme`; a family names a factory on it, not a type of its own.
- **Widgets** are domain-specific verbs / nouns describing the
  visual: `Headline`, `ContactLine`, `SectionHeader` (CV);
  `Letterhead`, `LineTable`, `TotalsCard` (invoice). Don't try to
  share widget names across families — they capture different
  visual ideas.
- **Presets** are descriptive proper-noun names: `BoxedSections`,
  `ModernProfessional`, `ClassicInvoice`, `MinimalLetter`. Avoid
  generic names like `Default` or `Standard`.

---

<a id="worked-walkthrough"></a>
## Worked walkthrough — `invoice`

Order to write things in:

### 1. `data/` first (no rendering deps)

```java
public record InvoiceDocument(
    InvoiceParty issuer,
    InvoiceParty recipient,
    String invoiceNumber,
    String issueDate,
    String dueDate,
    List<InvoiceSection> sections) {
    // builder, validation, accessors
}

public sealed interface InvoiceSection
    permits HeaderSection, LineItemsSection, TotalsSection, NotesSection {
    String title();
}
```

Sealed `InvoiceSection` lists every body shape an invoice can have.
Concrete subtypes (`LineItemsSection`, `TotalsSection`, …) are
records carrying the section data.

### 2. the theme second (there is nothing to write)

Pick the `BrandTheme` factory the family should default to, or add
one beside the others in `templates.core.theme` if none fits. The
record is shared, so a token added for an invoice is available to a
CV — which is the point, and also the reason to think before adding
one.

```java
BrandTheme theme = BrandTheme.invoiceModern();
```

### 3. `components/` third (low-level renderers consume data + theme)

```java
public final class LineRowRenderer {
    private LineRowRenderer() {}

    public static void render(SectionBuilder host, InvoiceLine line,
                              BrandTheme theme) {
        // … DSL calls reading theme tokens …
    }
}
```

Also: extract the shared paragraph DSL into a package-private
`ParagraphPrimitive` (same idea as CV) so renderers don't duplicate
configuration.

### 4. `widgets/` fourth (named visual building blocks)

```java
public final class LineTable {
    private LineTable() {}

    public static void render(SectionBuilder host,
                              List<InvoiceLine> lines, BrandTheme theme) {
        for (InvoiceLine line : lines) {
            LineRowRenderer.render(host, line, theme);
        }
    }
}
```

Widgets wrap components into composable units that a preset can drop
into its page flow.

### 5. `presets/` last (orchestration)

```java
public final class ClassicInvoice {
    public static final String ID = "classic-invoice";
    public static final String DISPLAY_NAME = "Classic Invoice";
    public static final double RECOMMENDED_MARGIN = 28.0;

    private ClassicInvoice() {}

    public static DocumentTemplate<InvoiceDocument> create() {
        return create(BrandTheme.invoiceModern());
    }

    public static DocumentTemplate<InvoiceDocument> create(BrandTheme theme) {
        return new Template(theme);
    }

    private static final class Template
            implements DocumentTemplate<InvoiceDocument> {
        // … compose() picks widgets in order …
    }
}
```

Mirror the CV preset shape exactly.

---

<a id="required-deliverables"></a>
## Required deliverables

A new template family PR ships with:

- [ ] **The sub-packages** under `<family>/` populated per convention
- [ ] **At least 1 reference preset** that renders a sample document
- [ ] **`AUTHORS.md`** in the family root (recipe cookbook — copy
      the cv one as starting structure)
- [ ] **`package-info.java`** at family root + each sub-package
- [ ] **Sample fixture** in `ExampleDataFactory.sample<Family>DocumentV2()`
- [ ] **Example runner** under `examples/.../templates/<family>/`
- [ ] **Smoke tests** per the checklist below
- [ ] **No edits** to `engine/` or `dsl/`

---

<a id="test-checklist"></a>
## Test checklist

Minimum test coverage matching CV v2:

| Test class | What it asserts |
|---|---|
| `<Family>DocumentTest` | Builder rejects null / blank required fields; valid build succeeds |
| `<Family>ThemeTest` | All factories produce valid themes; deprecated constructors (if any) wrap correctly |
| `<Preset>SmokeTest` | `id()`, `displayName()`, default-factory render, custom-theme render |
| `SectionDispatcherTest` *(optional)* | Each sealed subtype routes correctly |
| `WidgetSmokeTest` | Each public widget variant renders without throwing |
| `<Family>V2VisualParityTest` | **Per-pixel diff** against a checked-in baseline PNG for each preset |

CI must be green before merge.

### Visual regression — pixel-diff parity gate

Each preset's visual signature is **frozen** in a checked-in
baseline PNG. A parameterised test renders the preset on A4 against
a canonical sample document, rasterises each page via PDFBox, and
asserts the per-pixel diff stays within a budget. Catches silent
visual breakage from theme / widget / renderer refactors.

**Workflow:**

```bash
# 1. After a deliberate visual change — refresh baselines:
./mvnw test -Dtest='<Family>V2VisualParityTest' -Dgraphcompose.visual.approve=true

# 2. Commit the updated PNGs in the same change:
git add src/test/resources/visual-baselines/<family>-v2-layered/*.png
git commit -m "test: refresh visual baselines after <reason>"

# 3. Normal run (defends against unintended drift):
./mvnw test -Dtest='<Family>V2VisualParityTest'
```

**Where baselines live:**
`core/src/test/resources/visual-baselines/<family>-v2-layered/<slug>-page-N.png`

One PNG per page per preset. Pages overflow naturally — a 2-page
preset gets `<slug>-page-0.png` and `<slug>-page-1.png`.

**Budget calibration** — mirror the CV v2 settings until you have
evidence your family needs different limits:

```java
private static final long PIXEL_DIFF_BUDGET = 50_000L;   // max mismatched pixels per page
private static final int  PER_PIXEL_TOLERANCE = 8;        // per-channel tolerance
```

These are calibrated for cross-platform PDFBox font + colour
rendering drift between Windows-recorded baselines and Linux CI.
**Helvetica-based presets** (e.g. ModernProfessional) hit ~40k
mismatched pixels on the Linux CI; **PT-Serif-based presets**
(BoxedSections, MinimalUnderlined) stay under 10k. The 50k budget
covers both with margin.

If CI flakes on a specific preset above 50k, widen the budget for
that preset specifically (e.g. via a per-test `Map<String, Long>`
of overrides) rather than relaxing the global setting.

**Failure mode:** when the diff exceeds budget, the harness writes
`<slug>-page-N.actual.png` and `<slug>-page-N.diff.png` next to the
baseline so a reviewer can see exactly what changed before deciding
to re-bless or fix.

**Reference**: see
`core/src/test/java/com/demcha/compose/document/templates/cv/presets/CvV2VisualParityTest.java`
— a 200-line drop-in template you can copy for a new family.

---

<a id="doc-checklist"></a>
## Doc checklist

- [ ] `<family>/package-info.java` — ASCII diagram of the layers,
      plus a 4-step "how to author a document" walkthrough (copy the
      cv one's structure).
- [ ] `<family>/AUTHORS.md` — recipe cookbook. At least:
      change a glyph, change colours, add a new section subtype,
      conditional sections.
- [ ] `<family>/<sub-package>/package-info.java` — each
      sub-package gets a paragraph explaining its role.
- [ ] **Update [`docs/templates/v2-layered/README.md`](README.md)** to
      list the new family in the "implementations" section.

---

<a id="what-you-must-not-do"></a>
## What you must NOT do

- ❌ **Don't edit the engine** (`document/api`, `document/dsl`,
  `document/engine`, `document/node`, `document/style`). If your
  family needs an engine feature that doesn't exist, that's a
  separate prerequisite PR.
- ❌ **Don't add a theme package to your family.** Tokens live in
  `templates.core.theme` so families stay visually consistent; a
  private palette is how that consistency is lost one family at a time.
- ❌ **Don't fork widgets across families.** If invoice needs a
  `Headline`, write `templates/invoice/widgets/Letterhead.java`
  — an invoice letterhead has different needs from a CV name
  headline. Don't try to share the same widget class across
  domains.
- ❌ **Don't use inheritance.** All records are sealed, all
  components / widgets / presets are `final` with private
  constructors. The intentional API shape.
- ❌ **Don't add `instanceof` on the sealed section type outside
  `SectionDispatcher`.** The dispatcher is the single dispatch
  point.
- ❌ **Don't hard-code colours / fonts / sizes in components or
  widgets.** Every value reads from the theme. Preset-specific
  accents (a single colour used only by one preset) may live as
  `private static final` in that preset — but only when no other
  preset needs them.

---

<a id="pr-review-expectations"></a>
## PR review expectations

A new template family PR is reviewed against:

1. **Layer discipline** — do data / theme / components / widgets /
   presets each obey their contract?
2. **Test coverage** — smoke tests for every public surface;
   builder validation tests; theme factory tests.
3. **Doc completeness** — `package-info.java` everywhere,
   `AUTHORS.md` with at least 4 recipes, root README updated.
4. **Visual signature** — render the reference preset, attach the
   PDF to the PR description, eyeball-validate it matches the
   intent.
5. **No engine edits** — additive only.

Expected size: ~1500-2500 lines of new code for a fresh family.
Compare to cv baseline (PR #45) which was 2082 lines including
35 files.

---

## See also

- The **CV v2** package
  (`com.demcha.compose.document.templates.cv`) is the reference
  implementation. Read it end-to-end before starting a new family
  — every convention listed here is visible there.
- [authoring-presets.md](authoring-presets.md) — how preset authors
  use widgets. Same conventions apply when designing widgets for a
  new family.
- [using-templates.md](using-templates.md) — what end users see.
  Your new family's API should feel consistent with this.
