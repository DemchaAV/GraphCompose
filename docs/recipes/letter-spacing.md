# Letter spacing: spaced caps without wrecking the text

Wide-set capitals are a typographic effect — a name across the top of a
CV, a section banner, an eyebrow label. The obvious way to draw them is
to put a space between every pair of letters, and it is the wrong way:
the picture is right and the file is wrong. `"JANE DOE"` written as
`"J A N E   D O E"` is what search, copy/paste, a screen reader and an
applicant-tracking parser then read.

`DocumentLetterSpacing` moves the pen instead of the text.

```java
import com.demcha.compose.document.style.DocumentLetterSpacing;
import com.demcha.compose.document.style.DocumentTextStyle;

DocumentTextStyle headline = DocumentTextStyle.builder()
        .fontName(FontName.LATO)
        .size(22)
        .letterSpacing(DocumentLetterSpacing.ofFontSize(0.18))
        .build();

section.addParagraph(p -> p.text("JANE DOE").textStyle(headline));
```

The page shows spaced caps; the file still says `JANE DOE`.

## Two units, and why the value names its own

```java
DocumentLetterSpacing.ofFontSize(0.18)   // 18% of the font size
DocumentLetterSpacing.points(1.2)        // 1.2 points, whatever the size
```

Prefer `ofFontSize`. Expressed as a share, the tracking scales with the
type, so one style value reads the same on a 24pt name as on an 8pt
label — and keeps its proportions under auto-size. `points` is there for
designs specified in absolute measure.

The unit lives in the value rather than in a bare `double` because
`0.18` and `1.2` are both plausible-looking numbers and a call site
passing one has no way to say which it meant.

Negative values tighten. `DocumentLetterSpacing.NONE` is the default and
resolves to zero at every size, so a style that never mentions tracking
renders exactly as it did.

## What each format can express

Tracking is declared natively, never faked, in all three outputs — PDF's
`Tc`, DrawingML's `spc`, Word's `w:spacing`. They do not all measure the
same, though:

| | granularity | why |
|---|---|---|
| PDF and PPTX | **0.01pt** | DrawingML states spacing in hundredths of a point, so that is the finest distinction a PDF and a deck can both make. The engine measures on that grid, which keeps the width it reserves equal to the width the file draws. |
| DOCX | **0.05pt** | Word states spacing in twentieths and owns its own layout, so the export rounds to that grid independently. |

The value you wrote is never rewritten: `resolve(fontSize)` returns what
you asked for. Ask for a third of a point and the fixed-layout formats
both use `0.33`; that is the grid, not a loss of your value.

Tracking larger than a format can state is refused when the document is
rendered rather than silently wrapped — fixed layout tops out at
±4000pt, which is DrawingML's own bound.

## Where the spacing lands

One unit goes after **every** code point, the last one included, and an
ordinary space is spaced like any other character. That is what the PDF
`Tc` operator does, and PowerPoint and Word were measured doing the same.

The practical consequence: a centred or right-aligned tracked line is
aligned on a width that includes that trailing unit, so it sits half a
unit left of where an untracked line of the same glyphs would. This is
also how CSS `letter-spacing` behaves.

## The built-in presets

The bundled CV and cover-letter presets use `ofFontSize(0.18)` for their
spaced-caps blocks, exposed as
`TextOrnaments.SPACED_CAPS`. Reach for the same constant if you are
writing a preset that should match them.

### Coming from `TextOrnaments.spacedUpper`

`spacedUpper(...)` drew spaced caps by rewriting the string with a space
between every pair of letters. It still exists and still behaves exactly
as it always did, but it is deprecated as of 2.4.0 and no built-in preset
calls it any longer. Put the text through `TextOrnaments.upper(...)` and
carry the spacing on the style instead — `SPACED_CAPS`, or any
`DocumentLetterSpacing` you prefer:

```java
// before — the spacing is in the string
String text = TextOrnaments.spacedUpper(name);

// after — the spacing is in the style
String text = TextOrnaments.upper(name);
DocumentTextStyle resolved = style.withLetterSpacing(TextOrnaments.SPACED_CAPS);
```

The reason to move is the text layer, not the look: padding the string is
what stored a name in the file as `J A N E   D O E`, so that is what
search, copy/paste, screen readers and applicant-tracking parsers saw.

The two do not render at identical widths. A whole space glyph per gap is
wider than editorial tracking, which is why `0.18` was chosen to match the
old *total* width rather than the old per-gap width.

Runnable showcase:
[LetterSpacingExample](../../examples/src/main/java/com/demcha/examples/features/text/LetterSpacingExample.java)
— renders the same name at four trackings and prints what each of the
three formats says its text is.
