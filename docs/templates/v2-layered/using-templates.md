# Using Templates — author your own document

You have a preset you like. You want to render **your** content
(your name, your experience, your skills, your design tweaks). This
doc walks the `CvDocument` builder, the section types, and the
theme variants.

If you haven't read [quickstart.md](quickstart.md), do that first —
it sets up the conceptual model in 5 minutes.

---

## Table of contents

1. [The three pieces you assemble](#the-three-pieces-you-assemble)
2. [Identity — name, contact, optional links](#identity)
3. [Section types — three shapes cover every case](#section-types)
4. [Slots — main vs sidebar](#slots)
5. [Picking a preset](#picking-a-preset)
6. [Customising a theme](#customising-a-theme)
7. [Rendering — pageSize, margins, output](#rendering)
8. [Common patterns](#common-patterns)

---

## The three pieces you assemble

```java
CvDocument  doc      = …;                  // your content
CvTheme     theme    = CvTheme.boxedClassic();   // optional override
DocumentTemplate<CvDocument> tpl = BoxedSections.create(theme);

try (DocumentSession s = GraphCompose.document(path).create()) {
    tpl.compose(s, doc);
    s.buildPdf();
}
```

Three lines of "what":
- **`CvDocument`** — your content. Built via builder.
- **`CvTheme`** — visual style. Use a shipped factory or build your own.
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
## Section types — three shapes cover every case

The `CvSection` sealed hierarchy has exactly **three** concrete
shapes. Each captures a structurally different content pattern, not
a visual flavour.

### 1. `ParagraphSection` — one block of prose

For Professional Summary, Profile, Objective, About Me.

```java
new ParagraphSection("Professional Summary",
    "Backend engineer with **5 years** of experience...");
```

Inline markdown (`**bold**`, `*italic*`, `_italic_`) is honoured.

### 2. `RowsSection` — list of label/body rows with a decoration style

For Technical Skills, Languages, Awards, Additional Information,
Projects, anything with "label: value" entries.

```java
RowsSection.builder("Technical Skills", RowStyle.BULLETED)
    .row("Languages", "Java 21, Kotlin, SQL")
    .row("Tools",     "Maven, Docker, GitHub Actions")
    .build();
```

**Three decoration styles** — pick by what you want visually:

```java
RowStyle.PLAIN              // <b>Label:</b> body  (no bullet, single line)
RowStyle.BULLETED           // • <b>Label:</b> body
RowStyle.BULLETED_STACKED   // • <b>Label</b>
                            //   body (on second line, indented)
```

The same `RowsSection` type covers Technical Skills, Additional
Information, Projects — pick the style that matches the visual
density you want.

### 3. `EntriesSection` — timeline entries (title / subtitle / date / body)

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
`ModernProfessional`, `CenteredHeadline`) render only `Slot.MAIN`.
Sidebar content is silently dropped — switch to a multi-column preset
to render it.

If you don't use slots at all, your sections go to `MAIN` and every
preset renders them. The slot model is opt-in.

---

<a id="picking-a-preset"></a>
## Picking a preset

Four shipped today:

| Preset | Visual signature |
|---|---|
| `BoxedSections.create()` | Centred letter-spaced name, pale-grey panel section banners, two-page friendly |
| `MinimalUnderlined.create()` | Centred name with thin rule, small spaced-caps section titles with accent rule, single page |
| `ModernProfessional.create()` | Right-aligned big slate-blue name, flat bright-blue bold section titles, dense single page |
| `CenteredHeadline.create()` | Centred spaced-caps name, small subheadline, full-width rules around contact and modules |

Each factory has a no-arg form (uses a sensible default theme) and
a `create(CvTheme)` form (custom theme).

```java
BoxedSections.create()                          // default theme
BoxedSections.create(CvTheme.boxedClassic())    // explicit
BoxedSections.create(myCustomTheme)             // your own
```

---

<a id="customising-a-theme"></a>
## Customising a theme

Themes are records made of four sub-records:

| Sub-record | What it controls |
|---|---|
| `CvPalette` | Colours (`ink`, `muted`, `rule`, `banner`) |
| `CvTypography` | Fonts + size scale (8 sizes + line spacing) |
| `CvSpacing` | Margins, padding, weights, gaps |
| `CvDecoration` | Bullet glyph, stacked indent, contact separator |

**Swap one piece, keep the rest:**

```java
// Navy palette, classic everything else
CvPalette navy = new CvPalette(
    DocumentColor.rgb(15, 34, 80),     // ink — primary text
    DocumentColor.rgb(90, 110, 150),   // muted — italic subtitles
    DocumentColor.rgb(120, 140, 180),  // rule — separator lines
    DocumentColor.rgb(220, 230, 240)); // banner — pale fill

CvTheme navyTheme = new CvTheme(
    navy,
    CvTypography.classic(),
    CvSpacing.classic(),
    CvDecoration.classic());

BoxedSections.create(navyTheme);
```

**Change a glyph** (bullet, separator):

```java
CvDecoration arrowDecoration = new CvDecoration(
    "▶ ",       // bullet glyph
    "  ",       // stacked-row second-line indent
    "  ·  ");   // contact-line separator

CvTheme theme = new CvTheme(
    CvPalette.classic(),
    CvTypography.classic(),
    CvSpacing.classic(),
    arrowDecoration);
```

**Change a font** (`Helvetica` instead of `PT Serif`):

```java
CvTypography sans = new CvTypography(
    FontName.HELVETICA_BOLD, FontName.HELVETICA,
    21.5, 8.5, 9.6, 9.2, 8.8, 8.4, 8.6, 1.4);  // sizes per role

CvTheme theme = new CvTheme(
    CvPalette.classic(), sans, CvSpacing.classic(), CvDecoration.classic());
```

For more recipes (compact spacing, alternative typography scales,
etc.) see [`cv/v2/AUTHORS.md`](../../../src/main/java/com/demcha/compose/document/templates/cv/v2/AUTHORS.md).

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
[`cv/v2/AUTHORS.md`](../../../src/main/java/com/demcha/compose/document/templates/cv/v2/AUTHORS.md)
