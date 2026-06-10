# Timelines: markers on a connector rail

`addTimeline` builds a vertical timeline: a sequence of entries, each a
`TimelineMarker` sitting in a continuous connector rail, paired with its
content (title, meta, body). Pairing the marker with its entry — instead
of hand-placing a bullet plus a left margin per row — is the semantic
win. The rail auto-stretches to each entry's height, so it spans
variable-length content and reads as one continuous line.

## A basic timeline

```java
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.style.DocumentColor;

DocumentColor accent = DocumentColor.rgb(40, 90, 120);

section.addTimeline(timeline -> timeline
        .entry(TimelineMarker.dot(8, accent), e -> e
                .title("Senior Engineer")
                .meta("2021 - present")
                .body("Led the rendering pipeline rewrite and mentored three engineers."))
        .entry(TimelineMarker.dot(8, accent), e -> e
                .title("Engineer")
                .meta("2019 - 2021")
                .body("Shipped the layout engine and the table system.")));
```

Each entry slot is optional — a marker with only a title, or only a
body, renders fine. `e.add(content -> ...)` appends arbitrary extra
blocks below the body (chips, nested rows, lists), configured against
the entry's content section.

## Marker kinds

```java
import com.demcha.compose.document.style.DocumentStroke;

TimelineMarker.dot(8, accent);                          // solid filled dot
TimelineMarker.circle(10, null,                         // outlined ring
        DocumentStroke.of(accent, 1.2));
TimelineMarker.numbered(3, 16, accent,                  // numbered disc
        DocumentColor.WHITE);
TimelineMarker.square(8, accent);                       // filled square
```

`circle(size, fill, stroke)` takes an optional fill and/or outline —
pass a fill for a two-tone disc, or only a stroke for an empty ring.
`numbered(n, size, fill, textColor)` centres the step number in the
disc; the label scales with the disc size. A marker carries its own size
into the rail column, so mixed marker sizes in one timeline lay out
correctly.

## Rail and geometry

```java
section.addTimeline(timeline -> timeline
        .connector(DocumentColor.rgb(150, 158, 172), 1.5) // rail colour + width
        .gutter(8)              // rail → marker/content gap
        .markerGap(8)           // marker → title gap
        .markerColumnWeight(0.12) // widen for large numbered discs
        .spacing(14)            // vertical gap between entries
        .entry(TimelineMarker.dot(8, accent), e -> e.title("Kick-off")));
```

The rail is a left accent border on each entry that spans the entry
spacing too, so the line never breaks between entries. Increase
`markerColumnWeight` (relative to a content weight of 1.0) when large
numbered discs crowd a narrow timeline.

## Text styles

Timeline-wide defaults and per-entry overrides:

```java
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

section.addTimeline(timeline -> timeline
        .titleStyle(DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(11)
                .build())
        .metaStyle(DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(8.5)
                .build())
        .entry(TimelineMarker.dot(8, accent), e -> e
                .title("Launch", DocumentTextStyle.builder()  // per-entry override
                        .fontName(FontName.HELVETICA_BOLD)
                        .size(13)
                        .build())
                .meta("June 2026")
                .body("General availability.")));
```

`titleStyle` / `metaStyle` / `bodyStyle` on the timeline set the
defaults for every entry; the two-argument `title(text, style)`,
`meta(text, style)`, and `body(text, style)` (or the matching
`*Style(...)` setters on the entry) override one entry.

## Pagination

A timeline paginates between entries by default, and a tall entry splits
within itself — the rail continues across the page break. Two opt-in
controls tighten that (both `@since 1.8.0`):

```java
section.addTimeline(timeline -> timeline
        .keepTogether()          // relocate the whole timeline to a fresh page
        .keepEntriesTogether()   // never split one entry across pages
        .entry(TimelineMarker.dot(8, accent), e -> e.title("Atomic entry")));
```

`keepTogether()` moves the whole timeline to the next page when it does
not fit in the remaining space but would fit on a fresh page — timelines
taller than a page still flow. `keepEntriesTogether()` keeps each entry
whole while still allowing breaks between entries. Same semantics as
the section-level controls in the
[keep-together recipe](keep-together.md).

Runnable demo:
[TimelineDemoTest](../../src/test/java/com/demcha/testing/visual/TimelineDemoTest.java)
renders a marker/rail sheet to `target/visual-tests/timeline/timeline.pdf`.
