# CV Templates v2 — author guide

This document is for developers who want to **build their own CV template**
on top of the v2 surface. It complements the JavaDoc in
`package-info.java` with longer, copy-pasteable recipes.

If you have never used this package before, read
[the package overview](./package-info.java) first. The four layers
(`data/` · `theme/` · `components/` · `presets/`) and what each is for
are explained there.

This guide answers the **"how do I…"** questions.

---

## The scaffold is persona-neutral

Nothing in the v2 API assumes a software-developer audience. The
sample data shipped with the package happens to be a developer CV
(Jordan Rivera, "Platform Engineer", GitHub, Projects, Tech Skills),
but that's only because we needed *some* fixture for visual
regression. The same builders fit any persona — skip the link / section
types you don't need.

```java
// A primary school teacher's CV — no GitHub, no Projects, no Tech Skills
CvDocument.builder()
    .identity(CvIdentity.builder()
        .name("Maria", "Lopez")
        .contact("+34 600 000 000", "maria@example.com", "Madrid, Spain")
        // no .link(...) calls — she has no public profiles, and that is fine
        .build())
    .section(new ParagraphSection("About Me",
        "Primary school teacher with 12 years' experience in literacy " +
        "and inclusive education."))
    .section(EntriesSection.builder("Teaching Experience")
        .entry("Lead Teacher Y3", "Colegio Santa Ana", "2018-Present",
               "Year-3 lead teacher; designed the school's reading-rota " +
               "and mentored two newly-qualified teachers.")
        .entry("Year Teacher", "Escuela Primaria Goya", "2013-2018",
               "Y1-Y2 generalist; led the SEN reading-club after hours.")
        .build())
    .section(RowsSection.builder("Languages", RowStyle.PLAIN)
        .row("Spanish",   "Native")
        .row("English",   "Fluent (CEFR C1)")
        .row("Catalan",   "Conversational")
        .build())
    .section(RowsSection.builder("Certifications", RowStyle.PLAIN)
        .row("Inclusive Education",     "Universidad Complutense, 2020")
        .row("Children's First Aid",    "Cruz Roja, 2022")
        .build())
    .build();
```

Notice what's absent:

- **No `link(...)` calls.** Optional, simply omitted.
- **No Projects, no Skills.** The three section types
  (`ParagraphSection`, `RowsSection`, `EntriesSection`) work for any
  content shape — you choose what to put in them and what to call
  them.
- **No required IT vocabulary anywhere.** Section titles are free
  strings (`"About Me"`, `"Teaching Experience"`, `"Certifications"`).

The rest of this guide leans on dev-style examples for continuity,
but every recipe below works the same way for any persona.

---

## Recipe 1 — change a bullet glyph

You want `▶` instead of `•`, or numbered bullets, or em-dashes.

This is a **theme** change, not a renderer change. Build a custom
`CvDecoration` and hand it to a fresh `CvTheme`:

```java
CvTheme theme = new CvTheme(
        CvPalette.classic(),
        CvTypography.classic(),
        CvSpacing.classic(),
        new CvDecoration(
                "▶ ",      // bullet glyph
                "  ",      // stacked-row second-line indent (same visual width as bullet)
                "  ·  "    // contact-line separator
        ));

DocumentTemplate<CvDocument> template = BoxedSections.create(theme);
```

That's it. No renderer code changes. `RowRenderer` and
`ContactRenderer` read these strings from `theme.decoration()` on every
call.

**Why the second-line indent is its own token:** when a stacked row
(Projects style) wraps to a second line, the body text must align under
the **bold name**, not under the bullet. The indent string must have
the same visual width as the bullet glyph + trailing space, so if you
pick a wider bullet you'll likely want a wider stacked-indent too.

---

## Recipe 2 — change colours only

You want the same Boxed Sections look but in navy instead of grey.

```java
CvPalette navy = new CvPalette(
        DocumentColor.rgb(15, 34, 80),     // ink — primary text
        DocumentColor.rgb(90, 110, 150),   // muted — italic subtitles
        DocumentColor.rgb(120, 140, 180),  // rule — separator lines
        DocumentColor.rgb(220, 230, 240)); // banner — pale fill behind titles

CvTheme navyTheme = new CvTheme(
        navy,
        CvTypography.classic(),
        CvSpacing.classic(),
        CvDecoration.classic());

DocumentTemplate<CvDocument> template = BoxedSections.create(navyTheme);
```

Same shape — sub-record swap, keep the rest of the theme.

---

## Recipe 3 — change fonts and sizes

You want a sans-serif body or a tighter scale.

```java
CvTypography compact = new CvTypography(
        FontName.INTER, FontName.INTER,
        18.0,    // headline (was 21.5)
        7.8,     // contact
        8.6,     // banner
        8.4,     // entry title
        8.0,     // entry date
        7.6,     // entry subtitle
        7.8,     // body
        1.3);    // line spacing (was 1.4)

CvTheme compactTheme = new CvTheme(
        CvPalette.classic(),
        compact,
        CvSpacing.classic(),
        CvDecoration.classic());
```

---

## Recipe 4 — write a new preset (reuse existing renderers)

You want a different page layout — no banner panels, section titles
underlined instead. Same data, same components, different composition.

See `presets/MinimalUnderlined.java` for a worked example. The pattern:

```java
public final class MyPreset {

    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.boxedClassic());
    }

    public static DocumentTemplate<CvDocument> create(CvTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new Template(theme);
    }

    private static final class Template implements DocumentTemplate<CvDocument> {
        private final CvTheme theme;
        Template(CvTheme theme) { this.theme = theme; }

        @Override public String id() { return "my-preset"; }
        @Override public String displayName() { return "My Preset"; }

        @Override
        public void compose(DocumentSession document, CvDocument doc) {
            PageFlowBuilder pageFlow = document.dsl().pageFlow()
                    .name("MyRoot")
                    .spacing(theme.spacing().pageFlowSpacing())
                    .addSection("Headline", s ->
                            HeadlineRenderer.render(s, doc.identity().name(), theme))
                    .addSection("Contact", s ->
                            ContactRenderer.render(s, doc.identity(), theme));

            for (int i = 0; i < doc.sections().size(); i++) {
                final CvSection sec = doc.sections().get(i);
                final int idx = i;

                // Replace BannerRenderer with whatever title style you want.
                pageFlow.addSection("Title_" + idx, host -> {
                    /* custom title rendering */
                });
                pageFlow.addSection("Body_" + idx, host ->
                        SectionDispatcher.renderBody(host, sec, theme));
            }

            pageFlow.build();
        }
    }
}
```

The renderers in `components/` are all `static` — your preset just
calls them. No inheritance, no instance state to manage.

---

## Recipe 5 — add a brand-new section subtype

You need something the existing three section types can't express —
say, a skill-bar chart, a quote block, or a contact-references list.

Three places to touch (compile-checked path):

**a)** Add a record to `data/`:

```java
public record QuoteSection(String title, String quote, String attribution)
        implements CvSection {
    public QuoteSection {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(quote, "quote");
        Objects.requireNonNull(attribution, "attribution");
        if (title.isBlank()) throw new IllegalArgumentException("title must not be blank");
    }
}
```

**b)** Add it to the sealed permits in `CvSection.java`:

```java
public sealed interface CvSection
        permits ParagraphSection, RowsSection, EntriesSection, QuoteSection {
    String title();
}
```

**c)** Write a renderer in `components/`:

```java
public final class QuoteRenderer {
    private QuoteRenderer() {}
    public static void render(SectionBuilder section, QuoteSection q, CvTheme theme) {
        // …compose the visual using ParagraphPrimitive + theme tokens…
    }
}
```

**d)** Add a branch in `SectionDispatcher.renderBody`:

```java
} else if (section instanceof QuoteSection q) {
    QuoteRenderer.render(host, q, theme);
}
```

The final `else` of the dispatcher throws — if you forget (d) the
runtime will say so loudly the first time someone uses your new
subtype. (On Java 21 this would be a compile-time exhaustiveness
check via pattern-match switch; we target Java 17 so it's a runtime
guard.)

---

## Recipe 6 — place sections in slots (sidebar / footer)

A `CvDocument` is not just a flat list of sections — every section is
**placed** into a `Slot` (one of `MAIN`, `SIDEBAR`, `FOOTER`).
Single-column presets like `BoxedSections` read only `Slot.MAIN`
sections; multi-column presets read whichever slots they support.

**Placing a section into a slot** (builder API):

```java
CvDocument doc = CvDocument.builder()
    .identity(identity)
    .section(summary)                              // defaults to MAIN
    .section(Slot.MAIN, technicalSkills)           // explicit, same as above
    .section(Slot.SIDEBAR, languagesSpoken)        // sidebar column
    .section(Slot.SIDEBAR, certifications)         // sidebar column
    .sections(Slot.MAIN, experience, projects)     // varargs in a slot
    .build();
```

**Reading by slot** (in a preset's `compose()`):

```java
// Single-column preset — render only MAIN, drop the rest
List<CvSection> mainSections = doc.sectionsIn(Slot.MAIN);

// Two-column preset
List<CvSection> mainSections    = doc.sectionsIn(Slot.MAIN);
List<CvSection> sidebarSections = doc.sectionsIn(Slot.SIDEBAR);
```

**Why this matters:** when the user feeds your document to a
single-column preset, sidebar sections are silently dropped (they
have no place to render). When the same document goes through a
two-column preset, sidebar sections show up in the sidebar column.
The data model is the same — only the preset's interpretation
differs.

**Tip:** `doc.sections()` returns *every* section in source order
regardless of slot. Use it for debug printing or unfiltered
iteration — not in a preset's render loop unless you really want
sidebar content to flow inline with main.

---

## Widget cookbook — the LEGO bricks

When you build a preset, you compose your `compose()` method from
**widgets** that live in
`com.demcha.compose.document.templates.cv.v2.widgets`. Each widget
captures one visual idea, with named variants per visual style.

This means your preset reads as a sequence of visual decisions, not
as DSL plumbing. Below is the current catalog.

### `Headline` — top-of-document name

| Variant | Visual | Used in |
|---|---|---|
| `Headline.spacedCentered(host, name, theme)` | centred letter-spaced uppercase (`J A N E   D O E`) | BoxedSections, MinimalUnderlined, CenteredHeadline, BlueBanner |
| `Headline.rightAligned(host, name, theme)` | right-aligned plain bold (`Jane Doe`) | ModernProfessional |
| `Headline.render(host, name, theme, align, spacedCaps)` | low-level: pick any alignment + transform | — |

### `Subheadline` — secondary tagline under the name

| Variant | Visual | Used in |
|---|---|---|
| `Subheadline.centeredSpacedCaps(host, text, style)` | centred letter-spaced uppercase tagline | CenteredHeadline |

### `ContactLine` — phone / email / address / links row

| Variant | Visual | Used in |
|---|---|---|
| `ContactLine.centered(host, identity, theme)` | centred, phone → email → address → links | BoxedSections, MinimalUnderlined, CenteredHeadline, BlueBanner |
| `ContactLine.rightAligned(host, identity, theme)` | right-aligned, address → phone → email → links | ModernProfessional |
| `ContactLine.twoRowRightAligned(host, identity, theme, bodyStyle, linkStyle, separatorStyle)` | right-aligned address/phone row plus email/link row | ModernProfessional |
| `ContactLine.render(host, identity, theme, align, order)` | low-level: pick alignment + field order | — |

The separator glyph comes from
`theme.decoration().contactSeparator()` — swap `CvDecoration` to
change `   |   ` to `  ·  ` or anything else.

### `SectionHeader` — title above a section body

| Variant | Visual | Used in |
|---|---|---|
| `SectionHeader.banner(host, title, theme)` | pale-grey panel with centred spaced-caps inside | BoxedSections |
| `SectionHeader.fullWidthBanner(host, title, theme[, style])` | full-width fill banner with centred spaced-caps inside; rules around it stay in the preset page flow | BlueBanner |
| `SectionHeader.underlined(host, title, theme)` | small spaced-caps left-aligned, thin rule below | MinimalUnderlined |
| `SectionHeader.flat(host, title, color, theme)` | large bold title in a given colour, no panel | ModernProfessional |
| `SectionHeader.flatSpacedCaps(host, title, color, theme, titleStyle)` | small spaced-caps title in a soft colour, no panel | CenteredHeadline |

Note that `flat` and `flatSpacedCaps` take a `DocumentColor`
argument — the section title colour is the preset's signature
accent, and the widget deliberately surfaces it as a parameter
rather than burying it in the theme.

### Composing a preset from widgets

A typical preset's `compose()` becomes a sequence of widget calls:

```java
@Override
public void compose(DocumentSession document, CvDocument doc) {
    PageFlowBuilder pageFlow = document.dsl().pageFlow()
            .name("MyPresetRoot")
            .spacing(theme.spacing().pageFlowSpacing())
            .addSection("Headline", s ->
                    Headline.spacedCentered(s, doc.identity().name(), theme))
            .addSection("Contact", s ->
                    ContactLine.centered(s, doc.identity(), theme));

    for (CvSection sec : doc.sectionsIn(Slot.MAIN)) {
        pageFlow
            .addSection("Title", s -> SectionHeader.banner(s, sec.title(), theme))
            .addSection("Body",  s -> SectionDispatcher.renderBody(s, sec, theme));
    }
    pageFlow.build();
}
```

That's **the whole preset.** Twelve lines. No paragraph DSL, no
custom rendering helpers.

### When the widget doesn't fit

Widgets are optional, not mandatory. When a preset needs something
no widget covers (e.g. unusual link colours, special separator,
custom alignment combo), **inline it**:

```java
.addSection("Contact", section -> {
    DocumentTextStyle myStyle = DocumentTextStyle.builder()
        .fontName(FontName.HELVETICA)
        .color(DocumentColor.rgb(65, 105, 225))
        .build();

    section.addParagraph(p -> p
        .text(doc.identity().contact().email())
        .textStyle(myStyle)
        .align(TextAlign.RIGHT));
})
```

Inline is fine for one-off needs. If the same inline pattern shows
up in **2+ presets**, that's the signal to extract a new widget
variant or add a parameter to an existing one. Don't pre-extract.

### Adding a new widget — the test of when

| Pattern repetition | Action |
|---|---|
| 1 preset only | Inline. Leave it alone. |
| 2 presets | Add a new factory method to an existing widget, OR add a parameter. |
| 3+ presets | It's its own widget. New class in `cv/v2/widgets/`. |

### Examples of widgets we could add (not done yet)

These don't exist today but illustrate where the catalog could
grow. **Don't write them until a real preset needs them** —
premature widgets are noise.

- `Badge.pill(host, label, fillColor, textColor)` — labelled pill
  for tags, awards, certifications.
- `IconLabel.render(host, icon, text, theme)` — small icon next
  to a text label, useful for contact rows that show icons.
- `Divider.thin(host, theme)` / `.thick(...)` — horizontal rule
  with configurable weight + colour.
- `TwoColumnRow.render(host, leftBuilder, rightBuilder, weights, theme)` —
  generic split with weights, useful for entry headers across
  presets.

---

## Recipe 7 — conditional sections (data-driven)

Real CVs often hide a section when there's nothing to put in it:

```java
CvDocument.Builder builder = CvDocument.builder()
        .identity(identity)
        .section(summary);

if (!skillCategories.isEmpty()) {
    builder.section(buildSkillsSection(skillCategories));
}
if (latestCertification != null) {
    builder.section(buildEducationSection());
}

CvDocument doc = builder.build();
```

The plain `if` is fine. There's no built-in `.sectionIf(...)` yet — if
this pattern becomes pervasive in your code, file an issue.

---

## Style guide for authors

When writing your own classes in this package, follow these conventions
so future readers can navigate the same way:

- **Records over classes** for data. Records carry no behavior.
- **`final` classes with private constructors** for static renderers
  and helpers. Mark them `package-private` if they aren't meant to be
  called by user code (like `ParagraphPrimitive`).
- **Theme as parameter**, not as a static or instance field. Renderers
  must work for any theme passed to them.
- **No magic numbers** in renderer code. Every literal that affects
  visuals goes into `CvSpacing`, `CvTypography`, or `CvDecoration`.
- **No instanceof on the data** outside `SectionDispatcher`. That class
  is the single dispatch point.
- **JavaDoc the public surface.** Sub-records and section types get a
  paragraph describing what they model and where they're rendered.

---

## What <em>not</em> to do

- ❌ Subclass renderers. They're `final`. If you need different
     behavior, write a new preset that composes them differently, or
     add a new theme token if it's cosmetic.
- ❌ Read raw `DocumentColor.rgb(...)` literals in renderer code. Add
     them to `CvPalette` so a theme can swap them.
- ❌ Use `instanceof` on `CvSection` outside `SectionDispatcher`.
     The dispatcher is the only place that knows about variants.
- ❌ Add behavior to data records. Records are inert.
- ❌ Break the public v2 API. If you must change a signature, add the
     new one and mark the old `@Deprecated` — see `CvTheme`'s 3-arg
     constructor for the pattern.

---

## When to write a new preset vs. a new theme vs. a new section type

| You want to change… | Add a new… |
|---|---|
| Colour / font / size | `CvPalette` / `CvTypography` (theme) |
| Bullet / separator glyph | `CvDecoration` (theme) |
| Layout / page-flow / which renderers run | **preset** |
| The data shape itself (new section type) | `CvSection` permits + renderer + dispatch branch |

When in doubt: try a theme change first. If that doesn't fit, write a
preset. Adding new data shapes is the last resort and the most
invasive — but the compiler will guide you (sealed + dispatch).
