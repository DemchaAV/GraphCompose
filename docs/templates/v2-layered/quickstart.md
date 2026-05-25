# Quickstart — Templates Layered Architecture

**5 minutes.** What it is, why it's structured this way, and a working
example that renders a CV PDF.

---

## What you get

GraphCompose's templates v2 (layered) gives you:

- **Records describing content** — `CvDocument`, `CvIdentity`,
  `CvSection`. No styling, no rendering, just structured data.
- **Themes describing visuals** — `CvTheme` (palette + typography +
  spacing + decoration). Swap a theme to change colours, fonts,
  bullet glyphs without touching renderers.
- **Widgets as visual LEGO bricks** — `Headline`, `Subheadline`,
  `ContactLine`, `SectionHeader`. Each one is a named visual decision
  you can drop into a preset.
- **Presets as compositions** — a preset orchestrates widgets in a
  page flow. `BoxedSections`, `MinimalUnderlined`,
  `ModernProfessional`, `CenteredHeadline`, `BlueBanner`,
  `EditorialBlue`, `ClassicSerif`, and `NordicClean` ship today;
  writing your own is ~150 lines.

You hand a `CvDocument` to a preset, you get a PDF. The preset
internally composes widgets that read theme tokens that ultimately
emit DSL calls to the engine.

---

## Render your first CV

```java
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.data.*;
import com.demcha.compose.document.templates.cv.v2.presets.BoxedSections;

import java.nio.file.Path;

CvDocument doc = CvDocument.builder()
    .identity(CvIdentity.builder()
        .name("Jane", "Doe")
        .contact("+44 20 7946 0958", "jane@example.com", "London, UK")
        .link("LinkedIn", "https://linkedin.com/in/jane-doe")
        .build())
    .section(new ParagraphSection("Professional Summary",
        "Backend engineer with **5 years** of experience building "
        + "high-throughput payment systems."))
    .section(SkillsSection.builder("Technical Skills")
        .group("Languages", "Java 21", "Kotlin", "SQL")
        .group("Frameworks", "Spring Boot", "Quarkus")
        .build())
    .section(EntriesSection.builder("Experience")
        .entry("Senior Engineer", "Acme Payments", "2022-Present",
            "Owned the settlement service handling **2M+ tx/day**.")
        .entry("Engineer", "Bright Bank", "2019-2022",
            "Built the fraud-detection rule engine.")
        .build())
    .build();

DocumentTemplate<CvDocument> template = BoxedSections.create();

try (DocumentSession session = GraphCompose.document(Path.of("cv.pdf"))
        .pageSize(DocumentPageSize.A4)
        .margin(28, 28, 28, 28)
        .create()) {
    template.compose(session, doc);
    session.buildPdf();
}
```

Run it. You get `cv.pdf` rendered in the Boxed Sections visual style.
Want it in the Modern Professional style instead? Change one line:

```java
DocumentTemplate<CvDocument> template = ModernProfessional.create();
```

Same data, different visual. That's the layering.

---

## The 5 layers

```
┌─────────────────────────────────────────────────────────────┐
│  presets/   BoxedSections, MinimalUnderlined,               │
│             ModernProfessional, CenteredHeadline,           │
│             BlueBanner, EditorialBlue, ClassicSerif,         │
│             NordicClean                                      │
│             — composition of widgets in a page flow         │
└─────────────────────────────────────────────────────────────┘
        │ compose from widgets
        ▼
┌─────────────────────────────────────────────────────────────┐
│  widgets/   Headline, Subheadline, ContactLine,             │
│             SectionHeader                                   │
│             — named visual LEGO bricks                      │
└─────────────────────────────────────────────────────────────┘
        │ delegate to ↓    │ read tokens from ↓
        ▼                  ▼
┌─────────────────────┐  ┌──────────────────────────────────┐
│  components/         │  │  theme/                          │
│    SectionDispatcher │  │    CvPalette  (colours)          │
│    EntryRenderer     │  │    CvTypography (fonts + sizes)  │
│    RowRenderer       │  │    CvSpacing  (margins + gaps)   │
│    ParagraphRenderer │  │    CvDecoration (bullet, sep)    │
│    + primitives      │  │    CvTheme (bundle + factories)  │
└─────────────────────┘  └──────────────────────────────────┘
        │ renders into DSL
        ▼
┌─────────────────────────────────────────────────────────────┐
│  data/      CvDocument, CvIdentity, CvSection (sealed),     │
│             ParagraphSection / SkillsSection / RowsSection  │
│             / EntriesSection,                               │
│             CvRow, CvEntry, Slot                            │
│             — pure records, zero rendering deps             │
└─────────────────────────────────────────────────────────────┘
```

**What each layer is for** (in plain English):

| Layer | "Answers the question…" |
|---|---|
| `data/` | "What goes on the page?" — content, no styling. |
| `theme/` | "How does it look?" — colours, fonts, glyphs. |
| `components/` | "How is one element drawn?" — paragraph, row, entry primitives. |
| `widgets/` | "Which visual building block do I want here?" — named LEGO bricks. |
| `presets/` | "In what order, with which widgets, on which page flow?" — composition. |

Lower layers don't know about higher ones. A theme change doesn't
touch a renderer. A new widget doesn't touch the data model.

---

## Where to go next

| You want to… | Read |
|---|---|
| Render your own CV with your data | [using-templates.md](using-templates.md) |
| Make a new visual style | [authoring-presets.md](authoring-presets.md) |
| Add a new template family to the library (invoice, cover-letter) | [contributor-guide.md](contributor-guide.md) |
| See hands-on recipes (change a bullet, swap colours, …) | [`cv/v2/AUTHORS.md`](../../../src/main/java/com/demcha/compose/document/templates/cv/v2/AUTHORS.md) |
| Run the shipped examples | [`examples/cv/v2/`](../../examples/src/main/java/com/demcha/examples/templates/cv/v2) |
