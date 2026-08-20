# Using Templates — author your own document

You have a preset you like. You want to render **your** content
(your name, your experience, your skills, your design tweaks). This
doc walks the `CvDocument` builder, the section types, and the
theme variants.

If you haven't read [quickstart.md](quickstart.md), do that first —
it sets up the conceptual model in 5 minutes.

> **Dependency.** These presets ship in the opt-in `graph-compose-templates` artifact (not
> bundled in `graph-compose`); add it, or use `graph-compose-bundle`. See the
> [README install matrix](../../../README.md#installation).

---

## Table of contents

1. [The pieces you assemble](#the-pieces-you-assemble)
2. [Identity — name, contact, optional links](#identity)
3. [Section types](#section-types)
4. [Building sections at runtime — `ModuleSection`](#runtime-modules)
5. [Slots — main vs sidebar](#slots)
6. [Picking a preset](#picking-a-preset)
7. [Customising a theme](#customising-a-theme)
8. [Rendering — pageSize, margins, output](#rendering)
9. [Common patterns](#common-patterns)

---

## The Pieces You Assemble

```java
CvDocument  doc      = …;                  // your content
BrandTheme     theme    = BrandTheme.boxedClassic();   // optional override
DocumentTemplate<CvDocument> tpl = BoxedSections.create(theme);

try (DocumentSession s = GraphCompose.document(path).create()) {
    tpl.compose(s, doc);
    s.buildPdf();
}
```

Three lines of "what":
- **`CvDocument`** — your content. Built via builder.
- **`BrandTheme`** — visual style. Use a shipped factory or build your own.
- **A preset** — orchestrates them into a page flow.

---

<a id="identity"></a>
## Identity — name, contact, optional links

Every CV starts with a `CvIdentity`. Two required pieces, links are
optional.

```java
CvIdentity identity = CvIdentity.builder()
    // Required: first + last (middle is optional)
    .name("Jane", "Doe")
    // .name("Jane", "Q.", "Doe")              // ← with middle name

    // Required: phone + email + address (none can be blank)
    .contact("+44 20 7946 0958",
             "jane.doe@example.com",
             "London, UK")

    // Optional: any number of labelled links
    .link("LinkedIn", "https://linkedin.com/in/jane-doe")
    .link("GitHub",   "https://github.com/jane-doe")
    .link("Portfolio","https://jane.dev")
    .build();
```

**No `.link(...)` calls?** Header just renders the phone / email /
address. Optional means truly optional.

**Label is a free string.** `"Behance"`, `"Substack"`, `"Etsy"`, anything.
The widget renders the label; the URL is the click target.

---

<a id="section-types"></a>
## Section Types

The `CvSection` sealed hierarchy has a small set of concrete
shapes. Each captures a structurally different content pattern, not
a visual flavour.

### 1. `ParagraphSection` — one block of prose

For Professional Summary, Profile, Objective, About Me.

```java
new ParagraphSection("Professional Summary",
    "Backend engineer with **5 years** of experience...");
```

Inline markdown (`**bold**`, `*italic*`, `_italic_`) is honoured.

### 2. `SkillsSection` — grouped skills

For Technical Skills and similar capability groups where the content
is naturally `category -> skills[]`.

```java
SkillsSection.builder("Technical Skills")
    .group("Languages", "Java 21", "Kotlin", "SQL")
    .group("Tools", "Maven", "Docker", "GitHub Actions")
    .build();
```

Keeping skills grouped means the same CV data can render as bullets,
a grid/table, sidebar chips, or compact inline rows depending on the
preset.

### 3. `RowsSection` — list of label/body rows with a decoration style

For Languages, Awards, Additional Information, Projects, anything
with "label: value" entries that is not a skill taxonomy.

```java
RowsSection.builder("Additional Information", RowStyle.PLAIN)
    .row("Languages", "English (Fluent), German (Intermediate)")
    .row("Work Eligibility", "Eligible to work in the UK and the EU")
    .build();
```

**Three decoration styles** — pick by what you want visually:

```java
RowStyle.PLAIN              // <b>Label:</b> body  (no bullet, single line)
RowStyle.BULLETED           // • <b>Label:</b> body
RowStyle.BULLETED_STACKED   // • <b>Label</b>
                            //   body (on second line, indented)
```

The same `RowsSection` type covers Additional Information and
Projects — pick the style that matches the visual density you want.

### 4. `EntriesSection` — timeline entries (title / subtitle / date / body)

For Education, Professional Experience — anything where you have a
list of items each with a title, subtitle, date, and description.

```java
EntriesSection.builder("Experience")
    .entry("Senior Engineer",      // title (bold)
           "Acme Payments",          // subtitle (italic, muted)
           "2022-Present",           // date (right-aligned)
           "Owned the settlement service...")    // body (paragraph)
    .entry("Engineer",
           "Bright Bank",
           "2019-2022",
           "Built the fraud-detection rule engine.")
    .build();
```

Blank fields collapse — a blank `date` removes the right column, a
blank `subtitle` drops the italic line, a blank `body` drops the
paragraph beneath.

---

<a id="runtime-modules"></a>
## Building sections at runtime — `ModuleSection`

The four types above are the right choice when you write a CV in Java:
you pick the record and the compiler checks it. They are the wrong one
when the CV is assembled from data — a form, a JSON payload, an LLM —
because the shape is not known until it arrives, and a user who picks
"dated entries" from a menu cannot instantiate a different record per
choice.

`ModuleSection` moves that choice into a value. One item record carries
every optional field, and a `CvKind` decides which of them are read:

```java
ModuleSection.builder("Volunteering", SectionRole.OTHER, CvKind.ENTRIES_DATED)
    .item(CvItem.of("Mentor, Rails Girls")
                .at("Rails Girls Berlin")       // subtitle
                .in("Berlin, DE")               // location
                .period("2019 - 2021")          // read by dated kinds only
                .bullets("Ran three weekend workshops"))
    .build();
```

| `CvKind` | Shape | Reads |
|---|---|---|
| `PARAGRAPH` | prose under the section heading | `body` |
| `BULLETS` | a bullet per item, description inline | `title`, `body` (not `link`) |
| `BULLETS_STACKED` | a bullet per item, description underneath | `title`, `link`, `body` |
| `INLINE_LIST` | `Languages: Java 21, Kotlin` | `title`, `body` (not `link`) |
| `ENTRIES` | timeline, no date column | everything but `period` |
| `ENTRIES_DATED` | timeline with dates | everything |

Only `title` is required on an item. Whatever a kind does not read is
ignored, so the same item renders with or without its dates depending
on the kind alone — which is what lets a "Volunteering" module be
shaped exactly like Education without a new type.

`SectionRole` says what a section *means*, separately from how it
draws — and it is the first thing a preset routes on. A preset with a designed
layout places sections into fixed slots, and it used to choose what
went where by matching the heading against a list of English words:
a CV headed `Ausbildung` or `Навыки` matched nothing, so the section
was dropped and the slot that wanted it rendered empty. Give the module
a role and it lands in the right slot whatever language the CV is
written in, and whatever kind you chose to draw it with — for the roles
that preset has a slot for. `SectionRole.OTHER` names no slot, so a
module carrying it routes by heading like any other section.

A heading that matches a keyword still routes a section that has no
role — every hand-written section, and any module you left as
`SectionRole.OTHER`. What a heading may not do is overrule a role: a
module declared `EXPERIENCE` and headed "Projects" goes where you put
it, and the projects slot does not also claim it.

Modules and the four fixed types mix freely in one document, and both
render through the same components — a module drawn as `ENTRIES_DATED`
lays out exactly like the `EntriesSection` carrying the same content.

### Which preset can you hand a runtime module to?

Not every preset. Several compose a fixed set of modules and find each by
matching headings, so a section they do not recognise never reaches a
renderer; the CV still comes out, minus a section, looking finished. The
ones that render whatever they are handed say so in the type system:

```java
List<ModularCvTemplate> safe = CvTemplates.modular();   // offer these

CvTemplates.byId("modern-professional")                 // or look one up
           .orElseThrow()
           .compose(session, doc);
```

`CvTemplates` also answers `all()`, `ids()`, and `recommendedMargin(id)` —
the margin a preset was designed at, which you need while building the
session, before you have a template.

Declaring `ModularCvTemplate` is not free: a fidelity suite renders a
document carrying every kind, an invented heading, a heading in a script no
keyword list contains, and a heading that *does* match one, through each
template that declares it, and asserts every item reached the page under the
author's own words.

The promise covers `Slot.MAIN`, which is where sections go unless you say
otherwise. Every shipped preset composes a single main column, so a section
placed in `Slot.SIDEBAR` is dropped — by these templates as by every other.

The presets outside that list are not broken, they are *designed*: each
composes a fixed set of slots, so it renders the roles it has a place for
and drops a section it has no slot for. Give such a preset a CV whose
sections map onto roles and it renders them all; give it an extra
"Volunteering" module and that one is lost. For most of them the reason is
structural — the whole body is one atomic block that cannot break across
pages, so there is nowhere to put an extra section, and lifting that needs
pagination work rather than routing.

`SidebarPortrait`, `MonogramSidebar` and `MintEditorial` are past that point:
their bodies are column flows, so both columns continue on the next page and
each renders as much of a career as it is handed. None of them caps its blocks
any more —
`SidebarPortrait` used to draw two jobs, two degrees, five skills, three
languages and two projects, `MonogramSidebar` two jobs, two degrees, seven
skills, three projects and three additional rows, and `MintEditorial` six
expertise labels and six skill bars — and all of them silently dropped the rest.
`MintEditorial` also stopped dealing its content two pages at a time. What they
still do not have is a place for a section they do not recognise, which is why
none of them is on the modular list yet.

A template also says *how* it draws through `CvRenderKit`. The shared
lowering turns a module into paragraphs, rows, and entries; the kit draws
them, so a preset with its own entry style renders your runtime module in
that style rather than the canonical one. Presets whose bodies already use
the shared components return `CvRenderKit.defaults()`.

---

<a id="slots"></a>
## Slots — main vs sidebar

By default, every section is placed in `Slot.MAIN` — the main
column. Multi-column presets read `Slot.SIDEBAR` separately.

```java
CvDocument doc = CvDocument.builder()
    .identity(identity)
    .section(summary)                              // → MAIN (default)
    .section(Slot.MAIN, skills)                    // → MAIN (explicit)
    .section(Slot.SIDEBAR, languagesSpoken)        // → SIDEBAR
    .sections(Slot.MAIN, experience, education)    // varargs → MAIN
    .build();
```

**Single-column presets** (`BoxedSections`, `MinimalUnderlined`,
`ModernProfessional`, `CenteredHeadline`, `BlueBanner`,
`EditorialBlue`, `ClassicSerif`) render only `Slot.MAIN`. `NordicClean`
and `CompactMono` also read `Slot.MAIN`, but lay it out as their own
two-column rail/body compositions. Sidebar content is silently dropped
— switch to a multi-column preset to render it.

If you don't use slots at all, your sections go to `MAIN` and every
preset renders them. The slot model is opt-in.

---

<a id="picking-a-preset"></a>
## Picking a preset

Nine shipped today:

| Preset | Visual signature |
|---|---|
| `BoxedSections.create()` | Centred letter-spaced name, pale-grey panel section banners, two-page friendly |
| `MinimalUnderlined.create()` | Centred name with thin rule, small spaced-caps section titles with accent rule, single page |
| `ModernProfessional.create()` | Right-aligned big slate-blue name, flat bright-blue bold section titles, dense single page |
| `CenteredHeadline.create()` | Centred spaced-caps name, small subheadline, full-width rules around contact and modules |
| `BlueBanner.create()` | Centred PT-Serif name, compact Lato body, blue full-width section banners between thin rules |
| `EditorialBlue.create()` | Centred uppercase masthead, optional job-title subtitle, blue editorial rules, compact skills table |
| `ClassicSerif.create()` | PT-Serif cover/detail layout, cream profile band, tan rules |
| `NordicClean.create()` | Barlow uppercase identity, teal profile band, tinted sidebar rail, compact main column |
| `CompactMono.create()` | Dark command-bar header, pale left rail, same-width right-column cards |

Each factory has a no-arg form (uses a sensible default theme) and
a `create(BrandTheme)` form (custom theme).

```java
BoxedSections.create()                          // default theme
BoxedSections.create(BrandTheme.boxedClassic())    // explicit
BoxedSections.create(myCustomTheme)             // your own
```

`NordicClean` also exposes preset-specific options because its
signature has a structural rail and three editable colour surfaces:
the accent rules/links/name underline, the rail fill, and the profile
band fill.

```java
NordicClean.create(
    BrandTheme.nordicClean(),
    NordicClean.Options.builder()
        .railSide(NordicClean.RailSide.RIGHT)   // skills rail on the right
        .accentColor(DocumentColor.rgb(40, 110, 120))
        .railFillColor(DocumentColor.rgb(244, 249, 249))
        .profileFillColor(DocumentColor.rgb(226, 244, 245))
        .build());
```

---

<a id="customising-a-theme"></a>
## Customising a theme

Themes are records made of four sub-records:

| Sub-record | What it controls |
|---|---|
| `Palette` | Colours (`ink`, `muted`, `rule`, `banner`) |
| `Typography` | Fonts + size scale (8 sizes + line spacing) |
| `Spacing` | Margins, padding, weights, gaps |
| `Decoration` | Bullet glyph, stacked indent, contact separator |

**Swap one piece, keep the rest:**

```java
// Navy palette, classic everything else
Palette navy = new Palette(
    DocumentColor.rgb(15, 34, 80),     // ink — primary text
    DocumentColor.rgb(90, 110, 150),   // muted — italic subtitles
    DocumentColor.rgb(120, 140, 180),  // rule — separator lines
    DocumentColor.rgb(220, 230, 240)); // banner — pale fill

BrandTheme navyTheme = new BrandTheme(
    navy,
    Typography.classic(),
    Spacing.classic(),
    Decoration.classic());

BoxedSections.create(navyTheme);
```

**Change a glyph** (bullet, separator):

```java
Decoration arrowDecoration = new Decoration(
    "▶ ",       // bullet glyph
    "  ",       // stacked-row second-line indent
    "  ·  ");   // contact-line separator

BrandTheme theme = new BrandTheme(
    Palette.classic(),
    Typography.classic(),
    Spacing.classic(),
    arrowDecoration);
```

**Change a font** (`Helvetica` instead of `PT Serif`):

```java
Typography sans = new Typography(
    FontName.HELVETICA_BOLD, FontName.HELVETICA,
    21.5, 8.5, 9.6, 9.2, 8.8, 8.4, 8.6, 1.4);  // sizes per role

BrandTheme theme = new BrandTheme(
    Palette.classic(), sans, Spacing.classic(), Decoration.classic());
```

For more recipes (compact spacing, alternative typography scales,
etc.) see [`cv/AUTHORS.md`](../../../templates/src/main/java/com/demcha/compose/document/templates/cv/AUTHORS.md).

---

<a id="rendering"></a>
## Rendering — pageSize, margins, output

Standard session-first API. The preset has a `RECOMMENDED_MARGIN`
constant that pairs visually with its design.

```java
float m = (float) BoxedSections.RECOMMENDED_MARGIN;  // 28pt for Boxed

try (DocumentSession session = GraphCompose.document(Path.of("cv.pdf"))
        .pageSize(DocumentPageSize.A4)
        .margin(m, m, m, m)
        .create()) {

    template.compose(session, doc);
    session.buildPdf();          // writes the file
}
```

Other output forms:

```java
session.toPdfBytes();        // byte[]
session.buildPdf(output);    // OutputStream
```

---

<a id="common-patterns"></a>
## Common patterns

### Conditional section (omit if data is empty)

```java
CvDocument.Builder b = CvDocument.builder().identity(identity);
b.section(summary);
if (!certificates.isEmpty()) {
    b.section(buildCertificationsSection(certificates));
}
CvDocument doc = b.build();
```

### Sidebar content

```java
CvDocument.builder()
    .identity(identity)
    .section(summary)                              // main
    .sections(Slot.SIDEBAR, skills, languages)     // sidebar
    .build();
```

Then render with a multi-column preset (when one ships) — sidebar
content is dropped by single-column presets today.

### Skip section title

The section's `title` is rendered by the preset. To suppress a
section title visually you'd need a preset that doesn't render it
(or write your own — see [authoring-presets.md](authoring-presets.md)).

### Persona-neutral content

Nothing in the API assumes a developer audience. A teacher's CV
looks the same — different strings, same builders:

```java
CvDocument.builder()
    .identity(CvIdentity.builder()
        .name("Maria", "Lopez")
        .contact("+34 600 000 000", "maria@example.com", "Madrid, Spain")
        // no .link() — Maria has no public profiles
        .build())
    .section(new ParagraphSection("About Me",
        "Primary school teacher with 12 years' experience."))
    .section(EntriesSection.builder("Teaching Experience")
        .entry("Lead Teacher Y3", "Colegio Santa Ana", "2018-Present",
               "Year-3 lead, mentored two NQTs.")
        .build())
    .section(RowsSection.builder("Languages", RowStyle.PLAIN)
        .row("Spanish",   "Native")
        .row("English",   "Fluent (CEFR C1)")
        .build())
    .build();
```

No GitHub, no Projects, no Tech Skills — and the API doesn't notice.

---

## Next step

→ Want a custom visual style on top of the v2 building blocks?
[**authoring-presets.md**](authoring-presets.md)

→ Reference for every recipe (change bullet, swap colours, …)
[`cv/AUTHORS.md`](../../../templates/src/main/java/com/demcha/compose/document/templates/cv/AUTHORS.md)
