# Templates — Layered Architecture

> ⚠️ **Naming clarification.** This is the **layered** template
> architecture (data / theme / components / widgets / presets), the
> going-forward canonical pattern. Package:
> `com.demcha.compose.document.templates.cv.v2`.
>
> **Not to be confused with** the older v1.6 "Templates v2" surface
> (`CvSpec`, `CvBuilder`, presets with `BusinessTheme`) — that lives
> in [templates/v1-classic/](../v1-classic/README.md) and still ships
> alongside this one.

The **canonical going-forward pattern** for building business documents
on GraphCompose. CV is the reference implementation today
(`com.demcha.compose.document.templates.cv.v2`); invoice, cover-letter,
proposal, and any new template family will follow the same shape as
they're migrated.

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

You'll learn the `CvDocument` builder API, the three section types
(paragraph / rows / entries), how slots place sections into columns,
and how to swap a theme (colours / fonts / glyphs) without forking a
preset.

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
You're a GraphCompose maintainer or contributor. You want to bring
invoice-v2, cover-letter-v2, or a new document type onto the same
5-layer pattern that CV uses.

→ **[contributor-guide.md](contributor-guide.md)**

You'll get the package convention (`<name>/v2/data` /
`theme` / `components` / `widgets` / `presets`), naming rules, test
expectations, doc expectations, and a worked checklist for a new
template family from empty folder to merged PR.

---

## The 5-layer pattern at a glance

```
presets/      composition: data + theme + widgets → DocumentTemplate
   │ compose from
   ▼
widgets/      LEGO bricks: Headline, ContactLine, SectionHeader, …
   │ delegate to                                  read tokens from
   ▼                                              ▼
components/   internal renderers + primitives    theme/    palette
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

- ❌ **Not a migration mandate.** Existing v1 templates
  (`cv/spec`, `cv/builder`, `cv/presets`) continue to work and
  ship. The layered pattern is for **new** templates and major
  rewrites.
- ❌ **Not a framework with magic.** Every file is plain
  Java records + static helpers. No reflection, no annotations,
  no codegen.
- ❌ **Not coupled to CV.** The pattern is domain-agnostic; CV is
  just the first family migrated. Invoice or cover-letter would
  use the same five folders with their own data shapes inside.
- ❌ **Not a UI framework.** No state, no events, no lifecycle.
  Templates render static PDFs from immutable data.

---

## See also

- **Per-package JavaDocs**:
  [`cv/v2/package-info.java`](../../../src/main/java/com/demcha/compose/document/templates/cv/v2/package-info.java)
  has the ASCII diagram and 4-step author walkthrough.
- **AUTHORS.md**:
  [`cv/v2/AUTHORS.md`](../../../src/main/java/com/demcha/compose/document/templates/cv/v2/AUTHORS.md)
  is the recipe cookbook — 7 hands-on recipes from "change a bullet
  glyph" to "add a new section subtype".
- **Examples**:
  [`examples/cv/v2/`](../../examples/src/main/java/com/demcha/examples/templates/cv/v2)
  has three runnable rendering examples — one per shipped preset.
- **Legacy v1 surface**:
  [`docs/templates/v1-classic/README.md`](../v1-classic/README.md) describes the older
  spec / preset / theme split used by the v1 templates. Still valid
  for the v1 packages; superseded by this guide for v2 work.
