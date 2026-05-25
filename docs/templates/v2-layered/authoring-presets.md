# Authoring Presets — write your own visual style

You like the layered architecture, but the shipped presets
(`BoxedSections`, `MinimalUnderlined`, `ModernProfessional`,
`CenteredHeadline`, `BlueBanner`, `EditorialBlue`) don't match the
design you want. This doc walks you through writing a new preset from
scratch — **without subclassing, without duplicating rendering code**.

If you haven't read [quickstart.md](quickstart.md) and
[using-templates.md](using-templates.md), do those first.

---

## Table of contents

1. [The core idea — compose, don't subclass](#the-core-idea)
2. [The widget catalog](#the-widget-catalog)
3. [Anatomy of a preset](#anatomy-of-a-preset)
4. [Full worked example — `CardStyle` preset](#full-worked-example)
5. [When the widget doesn't fit — go inline](#when-the-widget-doesnt-fit)
6. [Three layers of widget customisation](#three-layers-of-widget-customisation)
7. [Adding a new widget — the test of when](#adding-a-new-widget)
8. [Tests + render parity](#tests--render-parity)

---

<a id="the-core-idea"></a>
## The core idea — compose, don't subclass

A preset is one `public final class` (no inheritance) with a
`create()` factory that returns a `DocumentTemplate<CvDocument>`.
Inside, `compose()` is the orchestration method: it sequences
**widgets** in a page flow.

```java
@Override
public void compose(DocumentSession document, CvDocument doc) {
    document.dsl().pageFlow()
        .name("MyPresetRoot")
        .spacing(theme.spacing().pageFlowSpacing())
        .addSection("Headline", s -> Headline.spacedCentered(s, doc.identity().name(), theme))
        .addSection("Contact",  s -> ContactLine.centered(s, doc.identity(), theme));

    for (CvSection sec : doc.sectionsIn(Slot.MAIN)) {
        pageFlow
            .addSection("Title", s -> SectionHeader.banner(s, sec.title(), theme))
            .addSection("Body",  s -> SectionDispatcher.renderBody(s, sec, theme));
    }
    pageFlow.build();
}
```

That's the **entire** rendering decision tree. ~12 lines. No DSL
plumbing. No private `renderXxx` methods. Each line is a single
visual decision you can read like a recipe.

---

<a id="the-widget-catalog"></a>
## The widget catalog

Today, four widget classes live in
`com.demcha.compose.document.templates.cv.v2.widgets`. Each has a
small set of named variants.

### `Headline` — top-of-document name

| Variant | Visual |
|---|---|
| `Headline.spacedCentered(host, name, theme)` | Centred letter-spaced uppercase (`J A N E   D O E`) |
| `Headline.uppercaseCentered(host, name, theme)` | Centred uppercase without extra spacing (`JANE DOE`) |
| `Headline.rightAligned(host, name, theme)` | Right-aligned plain bold (`Jane Doe`) |
| `Headline.render(host, name, theme, align, spacedCaps)` | Low-level — any (alignment, transform) combo |

### `Subheadline` — secondary tagline under the name

| Variant | Visual |
|---|---|
| `Subheadline.centeredSpacedCaps(host, text, style)` | Centred letter-spaced uppercase tagline (`P R O F E S S I O N A L   T I T L E`) |

### `ContactLine` — phone / email / address / links row

| Variant | Visual |
|---|---|
| `ContactLine.centered(host, identity, theme)` | Centred, phone → email → address → links |
| `ContactLine.rightAligned(host, identity, theme)` | Right-aligned, address → phone → email → links |
| `ContactLine.twoRowRightAligned(host, identity, theme, bodyStyle, linkStyle, separatorStyle)` | Right-aligned address/phone row plus email/link row |
| `ContactLine.render(host, identity, theme, align, order)` | Low-level — any alignment + field-order combo |

### `SectionHeader` — title above each section body

| Variant | Visual |
|---|---|
| `SectionHeader.banner(host, title, theme)` | Pale-grey panel + centred spaced-caps inside |
| `SectionHeader.fullWidthBanner(host, title, theme[, style])` | Full-width fill banner + centred spaced-caps inside; surrounding rules stay in preset page flow |
| `SectionHeader.underlined(host, title, theme)` | Small left spaced-caps + thin rule below |
| `SectionHeader.flat(host, title, color, theme)` | Large bold title in a given colour, no panel |
| `SectionHeader.flatSpacedCaps(host, title, color, theme, titleStyle)` | Small left spaced-caps title in a soft colour, no panel |

The separator glyph used by `ContactLine`, the bullet glyph used by
`RowRenderer`, and other character-level choices come from
`theme.decoration()` — swap a `CvDecoration` to change them
globally.

---

<a id="anatomy-of-a-preset"></a>
## Anatomy of a preset

Every preset is the same skeleton:

```java
public final class MyPreset {

    public static final String ID            = "my-preset";
    public static final String DISPLAY_NAME  = "My Preset";
    public static final double RECOMMENDED_MARGIN = 28.0;

    private MyPreset() { }

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

        @Override public String id()          { return ID; }
        @Override public String displayName() { return DISPLAY_NAME; }

        @Override
        public void compose(DocumentSession document, CvDocument doc) {
            // ← the only place that varies between presets
        }
    }
}
```

Two factories (`create()` and `create(CvTheme)`), three constants
(`ID`, `DISPLAY_NAME`, `RECOMMENDED_MARGIN`), one inner `Template`
class implementing `DocumentTemplate<CvDocument>`. Stable.

---

<a id="full-worked-example"></a>
## Full worked example — `CardStyle` preset

Suppose you want a preset where each section is wrapped in a soft
card with a coloured left accent stripe. Here's the full preset.

```java
public final class CardStyle {

    public static final String ID            = "card-style";
    public static final String DISPLAY_NAME  = "Card Style";
    public static final double RECOMMENDED_MARGIN = 24.0;

    private static final DocumentColor ACCENT = DocumentColor.rgb(33, 150, 243);

    private CardStyle() { }

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

        @Override public String id()          { return ID; }
        @Override public String displayName() { return DISPLAY_NAME; }

        @Override
        public void compose(DocumentSession document, CvDocument doc) {
            PageFlowBuilder pageFlow = document.dsl().pageFlow()
                .name("CardStyleRoot")
                .spacing(8)
                .addSection("Headline", s ->
                    Headline.rightAligned(s, doc.identity().name(), theme))
                .addSection("Contact", s ->
                    ContactLine.rightAligned(s, doc.identity(), theme));

            for (CvSection sec : doc.sectionsIn(Slot.MAIN)) {
                pageFlow.addSection("Card", host -> {
                    host.accentLeft(ACCENT, 3.0)             // ← the "card" stripe
                        .padding(new DocumentInsets(8, 12, 8, 12));
                    SectionHeader.flat(host, sec.title(), ACCENT, theme);
                    SectionDispatcher.renderBody(host, sec, theme);
                });
            }
            pageFlow.build();
        }
    }
}
```

**Forty-five lines** including the boilerplate. Everything that
makes it visually distinct is in `compose()`:

- right-aligned headline (existing widget)
- right-aligned contact (existing widget)
- a custom "card" wrapper around each section (inline — uses DSL
  `accentLeft` + `padding` directly)
- flat coloured section title (existing widget, given `ACCENT`)
- body rendered via the dispatcher (no custom rendering)

You used **three widgets** (`Headline`, `ContactLine`,
`SectionHeader`) plus **two inline DSL calls** (`accentLeft` and
`padding`) to build the card shape. No private `renderXxx`. No
duplicated rendering.

---

<a id="when-the-widget-doesnt-fit"></a>
## When the widget doesn't fit — go inline

Widgets are **optional helpers**, not required wrappers. If your
preset needs something the catalog doesn't cover, **inline it**.

Example: `ModernProfessional` uses preset-specific colours
(slate-blue name, royal-blue link underlines) that no widget
default knows about. Its `renderHeader` and `renderContact` stay
inline — only `renderSectionTitle` uses a widget
(`SectionHeader.flat(..., SECTION_TITLE_COLOR, theme)` because that
widget takes a colour parameter).

```java
private void renderHeader(SectionBuilder section, CvIdentity identity) {
    DocumentTextStyle nameStyle = DocumentTextStyle.builder()
        .fontName(FontName.HELVETICA_BOLD)
        .size(theme.typography().sizeHeadline())
        .color(NAME_COLOR)
        .build();

    section.addParagraph(p -> p
        .text(identity.name().full())
        .textStyle(nameStyle)
        .align(TextAlign.RIGHT)
        .margin(DocumentInsets.zero()));
}
```

This is fine. **Widgets coexist with inline DSL** in the same
`compose()`. If you see the same inline rendering repeating across
**2+ presets**, *then* extract a widget — not before.

---

<a id="three-layers-of-widget-customisation"></a>
## Three layers of widget customisation

Every widget exposes three layers, escalating from convenience to
control:

### Layer 1 — convenience factory (covers ~80% of cases)

```java
Headline.spacedCentered(host, name, theme);
```

One line. No params beyond `(host, content, theme)`.

### Layer 2 — `.render(...)` with parameters (covers ~15%)

```java
Headline.render(host, name, theme, TextAlign.LEFT, /* spacedCaps */ false);
```

Same widget, fully parameterised. Use when the convenience method
doesn't match your need but the widget shape is right.

### Layer 3 — inline DSL (covers ~5%)

```java
section.addParagraph(p -> p
    .text(name.full())
    .textStyle(myCustomStyle)
    .align(TextAlign.RIGHT));
```

Bypass the widget entirely. Use when no widget shape fits.

**Don't fight the widget API.** If Layer 1 fits, use Layer 1. If
not, try Layer 2. If still not, go inline. That's the design.

---

<a id="adding-a-new-widget"></a>
## Adding a new widget — the test of when

| Pattern repetition across presets | Action |
|---|---|
| 1 preset only | Inline. Don't extract. |
| 2 presets | Add a new factory method to an existing widget, OR add a parameter to `.render(...)`. |
| 3+ presets | It's its own widget — new class in `cv/v2/widgets/`. |

Don't predict — extract. Premature widgets are noise; they add API
surface that nobody calls.

When you do add a new widget:

1. **One file per widget** in `cv/v2/widgets/`.
2. **`public final class`** with a private constructor.
3. **1-3 named factories** + a lower-level `.render(...)` when useful.
4. **First parameter is always `SectionBuilder host`**.
5. **Pass `CvTheme theme` when the widget reads shared tokens**;
   pass an explicit style only when the preset owns that unique style.
6. **No instance state** — all static, all stateless.
7. **JavaDoc the visual** — what does this look like? Who uses it?
8. **Add to `WidgetSmokeTest`** with a basic "renders without
   throwing" check.

---

<a id="tests--render-parity"></a>
## Tests + render parity

A new preset needs at least:

1. **Smoke test** in `src/test/.../cv/v2/presets/MyPresetSmokeTest.java`:
   - `exposes_stable_identity` — checks `id()` and `displayName()`
   - `default_factory_renders` — calls `create().compose(...)` with
     a full sample document, asserts `session.roots()` is non-empty
   - `custom_theme_renders` — same but with `create(theme)`
   - `renders_with_classic_theme_too` — proves the preset doesn't
     depend on theme-specific tokens

2. **Example runner** in
   `examples/src/main/java/com/demcha/examples/templates/cv/v2/CvMyPresetExample.java`:
   - Renders to `examples/target/generated-pdfs/templates/cv/cv-my-preset.pdf`
   - Uses `ExampleDataFactory.sampleCvDocumentV2()` for content

3. **Eyeball the rendered PDF** — does it match your design
   intent? Are sections in the right slots? Is page break sensible?

A future Phase will add PDF/PNG snapshot diffing so visual
regressions break the build. Until then, render parity is by-hand.

---

## Next step

→ Want to add a brand-new template family (invoice-v2,
cover-letter-v2) following the same layered shape?
[**contributor-guide.md**](contributor-guide.md)

→ The full recipe cookbook (with code for every customisation
combo):
[`cv/v2/AUTHORS.md`](../../../src/main/java/com/demcha/compose/document/templates/cv/v2/AUTHORS.md)
