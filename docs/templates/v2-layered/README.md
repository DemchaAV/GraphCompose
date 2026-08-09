# Templates — Layered Architecture

> ⚠️ **Naming clarification.** This is the **layered** template
> architecture (data / theme / components / widgets / presets) — the
> template surface. Reference package:
> `com.demcha.compose.document.templates.cv`.
>
> **Naming note:** through 1.x an older surface also called
> "Templates v2" (the pre-2.0 spec/builder surface, since removed)
> shipped alongside this one; it was removed in 2.0 and its docs are
> archived at [templates/v1-classic/](../v1-classic/README.md).

The **template surface** for building business documents on
GraphCompose. All five families ship on it — CV (the reference
implementation, `com.demcha.compose.document.templates.cv`),
cover-letter, invoice, proposal, and receipt — and any new template
family follows the same shape.

This is the entry point. Pick the doc that matches your goal.

---

## Pick your path

### 🆕 You're new to GraphCompose and templates
Start with **[quickstart.md](quickstart.md)** — 5 minutes of orientation:
what GraphCompose templates are, why they're layered, and a copy-paste
example that renders a CV to a PDF.

### 👤 You want to render a document with your own data
You like an existing preset (Boxed Sections, Minimal Underlined,
Modern Professional). Now you just want to feed it your name, your
experience, your skills.

→ **[using-templates.md](using-templates.md)**

You'll learn the `CvDocument` builder API, the built-in section types
(paragraph / grouped skills / rows / entries), how slots place
sections into columns, and how to swap a theme (colours / fonts /
glyphs) without forking a preset.

### 🎨 You want a custom visual style on top of v2
Existing presets aren't quite your design. You want a new look —
different section title style, different alignment, different colour
palette — but still using the v2 building blocks.

→ **[authoring-presets.md](authoring-presets.md)**

You'll learn the **widget cookbook** (`Headline`, `ContactLine`,
`SectionHeader`), the 12-line `compose()` pattern, when to drop down to
inline DSL, and how to ship a new preset as ~150 lines that anyone can
read end-to-end.

### 🛠 You're adding a new template family to the library
You're a GraphCompose maintainer or contributor. You want to bring a
new document type onto the same layering the shipped families use.

→ **[contributor-guide.md](contributor-guide.md)**

You'll get the package convention (`<family>/data` / `components` /
`widgets` / `presets` over the shared `templates.core.theme`), naming
rules, test expectations, doc expectations, and a worked checklist for a
new template family from empty folder to merged PR.

---

## The layering at a glance

```
presets/      composition: data + theme + widgets → DocumentTemplate
   │ compose from
   ▼
widgets/      LEGO bricks: Headline, Subheadline, ContactLine, SectionHeader, …
   │ delegate to                                  read tokens from
   ▼                                              ▼
components/   internal renderers + primitives    templates.core.theme
                                                   BrandTheme: palette
                                                              typography
                                                              spacing
                                                              decoration
   │ render
   ▼
data/         records describing what to render (no styling)
```

Every layer has **one job**. Layers below don't know about layers
above. Adding a new theme variant, a new widget, a new section
subtype, or a new preset each touches one layer and leaves the
others alone.

The detailed contract for each layer is in
[contributor-guide.md](contributor-guide.md).

---

## What this pattern is *not*

- ❌ **Not one shape for every family.** `presets/` is the only
  package a family always has. CV carries all four; cover letter has no
  `widgets/`; invoice and proposal are presets alone; receipt carries
  `components/` + `widgets/` + `presets/`. All of them read their data
  records from the shared `templates.data.<family>`. Add a layer when
  the family needs one.
- ❌ **Not a framework with magic.** Every file is plain
  Java records + static helpers. No reflection, no annotations,
  no codegen.
- ❌ **Not coupled to CV.** The pattern is domain-agnostic; CV is
  just the family that uses every layer, which is why it is the
  reference implementation.
- ❌ **Not a UI framework.** No state, no events, no lifecycle.
  Templates render static PDFs from immutable data.

---

## See also

- **Per-package JavaDocs**:
  [`cv/package-info.java`](../../../templates/src/main/java/com/demcha/compose/document/templates/cv/package-info.java)
  has the ASCII diagram and 4-step author walkthrough.
- **AUTHORS.md**:
  [`cv/AUTHORS.md`](../../../templates/src/main/java/com/demcha/compose/document/templates/cv/AUTHORS.md)
  is the recipe cookbook — 7 hands-on recipes from "change a bullet
  glyph" to "add a new section subtype".
- **Examples**:
  [`examples/cv/v2/`](../../../examples/src/main/java/com/demcha/examples/templates/cv/v2)
  has runnable rendering examples for the shipped presets.
- **Receipt family**: the newest family, and the smallest complete one
  to read end-to-end —
  [`receipt/package-info.java`](../../../templates/src/main/java/com/demcha/compose/document/templates/receipt/package-info.java)
  for the layer map,
  [`receipt/AUTHORS.md`](../../../templates/src/main/java/com/demcha/compose/document/templates/receipt/AUTHORS.md)
  for its recipes, and
  [`ModernReceiptExample`](../../../examples/src/main/java/com/demcha/examples/templates/receipt/ModernReceiptExample.java)
  for a rendered transfer confirmation.
- **Archived classic surface**:
  [`docs/templates/v1-classic/README.md`](../v1-classic/README.md) describes the older
  spec / preset / theme split that was removed in 2.0; kept for
  pre-2.0 callers.
