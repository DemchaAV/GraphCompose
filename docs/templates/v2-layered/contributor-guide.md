# Contributor Guide — add a new template family

You're a GraphCompose contributor and you want to bring a brand-new
document type onto the layered architecture — invoice-v2,
cover-letter-v2, report-v2, anything that isn't CV. This doc is
your checklist + convention reference.

It assumes you've read [quickstart.md](quickstart.md) and
[authoring-presets.md](authoring-presets.md) — those explain *why*
the layered pattern looks the way it does.

---

## Table of contents

1. [The 5-layer convention](#the-5-layer-convention)
2. [Package map for a new family](#package-map-for-a-new-family)
3. [Naming rules](#naming-rules)
4. [Worked walkthrough — `invoice/v2`](#worked-walkthrough)
5. [Required deliverables](#required-deliverables)
6. [Test checklist](#test-checklist)
7. [Doc checklist](#doc-checklist)
8. [What you must NOT do](#what-you-must-not-do)
9. [PR review expectations](#pr-review-expectations)

---

<a id="the-5-layer-convention"></a>
## The 5-layer convention

Every new template family lives under
`com.demcha.compose.document.templates.<family>.v2` with **exactly
five sub-packages**:

```
<family>/v2/
├── data/         records describing what's on the page
├── theme/        cosmetic tokens (palette, typography, spacing, decoration)
├── components/   internal renderers + low-level primitives
├── widgets/      reusable visual LEGO bricks
└── presets/      composition: data + theme + widgets → DocumentTemplate
```

Each layer's contract:

| Layer | Contract |
|---|---|
| `data/` | Pure records. Zero dependencies on rendering, theming, or DSL. Sealed hierarchy for section / block subtypes. |
| `theme/` | Records + factories. No rendering logic. Aggregate root is `<Family>Theme(palette, typography, spacing, decoration)`. |
| `components/` | Static helpers. Take `(host, data, theme)`. No statics holding state. No magic numbers — read tokens from theme. |
| `widgets/` | Static helpers. Named factory methods per visual variant. Compose internally from `components/`. |
| `presets/` | One `public final class` per visual style. Two factories: `create()` and `create(<Family>Theme)`. Inner `Template` implements `DocumentTemplate<<Family>Document>`. |

**The convention is the same regardless of domain.** A cv has
identity + sections; an invoice has parties + line items + totals; a
proposal has scope + pricing + acceptance terms. The *shape* of the
data differs; the *layering* is identical.

---

<a id="package-map-for-a-new-family"></a>
## Package map for a new family

Mirror the CV v2 layout. Concrete example for invoice:

```
src/main/java/com/demcha/compose/document/templates/invoice/v2/
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
├── theme/
│   ├── package-info.java
│   ├── InvoicePalette.java             ← ink / muted / rule / accent
│   ├── InvoiceTypography.java          ← scale
│   ├── InvoiceSpacing.java             ← margins, gaps
│   ├── InvoiceDecoration.java          ← row separators, totals divider
│   └── InvoiceTheme.java               ← aggregate + factories
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

The structure is **identical** to cv/v2 — only the records inside
differ.

---

<a id="naming-rules"></a>
## Naming rules

- **Family prefix** on top-level records to avoid name collisions.
  CV uses `CvName`, `CvContact`, `CvTheme`. Invoice should use
  `InvoiceParty`, `InvoiceLine`, `InvoiceTheme`. Cover letter:
  `CoverLetterRecipient`, `CoverLetterTheme`. Etc.
- **`<Family>Document`** for the root record. (`CvDocument`,
  `InvoiceDocument`, `CoverLetterDocument`.)
- **`<Family>Section` (sealed)** for the body content hierarchy.
- **`<Family>Theme(palette, typography, spacing, decoration)`** for
  the aggregate theme record.
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
## Worked walkthrough — `invoice/v2`

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

### 2. `theme/` second (no rendering, just tokens)

```java
public record InvoiceTheme(
    InvoicePalette palette,
    InvoiceTypography typography,
    InvoiceSpacing spacing,
    InvoiceDecoration decoration) {

    public static InvoiceTheme classic() {
        return new InvoiceTheme(
            InvoicePalette.classic(),
            InvoiceTypography.classic(),
            InvoiceSpacing.classic(),
            InvoiceDecoration.classic());
    }
}
```

### 3. `components/` third (low-level renderers consume data + theme)

```java
public final class LineRowRenderer {
    private LineRowRenderer() {}

    public static void render(SectionBuilder host, InvoiceLine line,
                              InvoiceTheme theme) {
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
                              List<InvoiceLine> lines, InvoiceTheme theme) {
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
        return create(InvoiceTheme.classic());
    }

    public static DocumentTemplate<InvoiceDocument> create(InvoiceTheme theme) {
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

- [ ] **5 packages** under `<family>/v2/` populated per convention
- [ ] **At least 1 reference preset** that renders a sample document
- [ ] **`AUTHORS.md`** in the family root (recipe cookbook — copy
      the cv/v2 one as starting structure)
- [ ] **`package-info.java`** at family root + each sub-package
- [ ] **Sample fixture** in `ExampleDataFactory.sample<Family>DocumentV2()`
- [ ] **Example runner** in `examples/.../templates/<family>/v2/`
- [ ] **Smoke tests** per the checklist below
- [ ] **No edits** to `engine/`, `dsl/`, or v1 `<family>/` surface

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

CI must be green before merge. Visual regression (PNG diff against
a reference render) is encouraged but optional today.

---

<a id="doc-checklist"></a>
## Doc checklist

- [ ] `<family>/v2/package-info.java` — ASCII diagram of the 5
      layers, plus a 4-step "how to author a document" walkthrough
      (copy the cv/v2 one's structure).
- [ ] `<family>/v2/AUTHORS.md` — recipe cookbook. At least:
      change a glyph, change colours, add a new section subtype,
      conditional sections.
- [ ] `<family>/v2/<sub-package>/package-info.java` — each
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
- ❌ **Don't edit v1 surface** for the same family. They coexist.
  Mark v1 `@Deprecated` only after the v2 surface is feature-complete
  and shipped — that's a follow-up PR.
- ❌ **Don't fork widgets across families.** If invoice needs a
  `Headline`, write `templates/invoice/v2/widgets/Letterhead.java`
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
5. **No engine / v1 edits** — additive only.

Expected size: ~1500-2500 lines of new code for a fresh family.
Compare to cv/v2 baseline (PR #45) which was 2082 lines including
35 files.

---

## See also

- The **CV v2** package
  (`com.demcha.compose.document.templates.cv.v2`) is the reference
  implementation. Read it end-to-end before starting a new family
  — every convention listed here is visible there.
- [authoring-presets.md](authoring-presets.md) — how preset authors
  use widgets. Same conventions apply when designing widgets for a
  new family.
- [using-templates.md](using-templates.md) — what end users see.
  Your new family's API should feel consistent with this.
