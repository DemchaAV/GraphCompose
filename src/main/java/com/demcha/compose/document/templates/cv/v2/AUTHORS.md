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

## Recipe 6 — conditional sections (data-driven)

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
