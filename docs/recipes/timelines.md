# Timelines: markers on a connector rail

`addTimeline` builds a vertical timeline: a sequence of entries, each a `TimelineMarker`
paired with its content, threaded on one continuous rail. Pairing the marker with its entry —
instead of hand-placing a bullet plus a left margin per row — is the semantic win.

## The model

```
Timeline
├── Rail                 one continuous line, resolved from where the markers and entries landed
└── Entry ×N
    ├── Leading          optional, e.g. a date column
    ├── Marker           dot / circle / numbered / square / your own
    └── Content          title, meta, body — or a column of your own
```

Horizontally an entry is three columns:

```
LEADING | AXIS | CONTENT
```

- The **rail belongs to the axis**. Leading content sits to its left and never moves it.
- **Marker size does not move the axis.** A 6pt dot and a 24pt square share one rail.
- The axis is sized either as a **share** of the row or in **points** — both fully supported.
- The rail is **one logical line**, computed after layout from the resolved marker and entry
  positions and contributed as one fragment per page it crosses. It is not a border repeated
  on each entry.

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

Each entry slot is optional — a marker with only a title, or only a body, renders fine.
`e.add(content -> ...)` appends arbitrary extra blocks below the body (chips, nested rows,
lists), configured against the entry's content section.

This is the shape every timeline written before the advanced API uses, and it keeps its
placement: markers packed to the left of the axis, rail one gutter further left, body spanning
the entry.

## Entries: two ways to fill one

The convenience mode above resolves `title` / `meta` / `body` into styled paragraphs. The
advanced form hands you the content column instead:

```java
section.addTimeline(timeline -> timeline
        .entry(e -> e
                .marker(TimelineMarker.numbered(1, 16, accent, DocumentColor.WHITE))
                .content(column -> column
                        .addParagraph("Anything you like")
                        .addTable(t -> t.headerRow("Stage", "Owner")))));
```

- A marker is required, either way.
- The two modes are **mutually exclusive**: an entry that calls `content(...)` cannot also set
  `title` / `meta` / `body`, and declaring the marker twice throws.
- Both spellings normalize into the same internal entry, so nothing downstream can tell which
  one an author used.

## Leading column: DATE | AXIS | CONTENT

```java
import com.demcha.compose.document.style.DocumentRowColumn;

section.addTimeline(timeline -> timeline
        .leadingColumn(DocumentRowColumn.fixed(64))
        .entry(e -> e.marker(TimelineMarker.dot(8, accent))
                .leading(date -> date.addParagraph("2023"))
                .title("Senior Engineer")
                .body("Led the layout engine rewrite."))
        .entry(e -> e.marker(TimelineMarker.dot(8, accent))
                .title("No date on this one")));
```

- `leadingColumn(...)` belongs to the **timeline**, not to an entry: every entry reserves the
  same column, so an entry that puts nothing in it still starts its marker where the others do.
- `fixed(...)` and `weight(...)` are supported. `auto()` is not: an auto column sized per entry
  would be a different width on each row, and the axis — and with it the rail — would move.
- A leading column never pushes the rail out to the entry's boundary. The rail stays with the
  axis, so the dates sit to the left of the line.

## Axis width

```java
section.addTimeline(timeline -> timeline.markerColumnWeight(0.12) …);  // a share of the row
section.addTimeline(timeline -> timeline.axisWidth(28) …);             // points
```

Two sizing strategies for the same column. A weight is a share of what the row has left, so it
grows with the page; `axisWidth` is points, so it does not. Declare one or the other —
configuring both throws — and neither is converted into the other.

## Markers

```java
import com.demcha.compose.document.style.DocumentStroke;

TimelineMarker.dot(8, accent);                                    // solid filled dot
TimelineMarker.circle(10, null, DocumentStroke.of(accent, 1.2));  // outlined ring
TimelineMarker.numbered(3, 16, accent, DocumentColor.WHITE);      // numbered disc
TimelineMarker.square(8, accent);                                 // filled square
TimelineMarker.custom(18, 18, column -> column                    // anything you can draw
        .addLayerStack(stack -> stack.back(ring).center(disc).center(pip)));
```

`circle(size, fill, stroke)` takes an optional fill and/or outline — pass a fill for a two-tone
disc, or only a stroke for an empty ring. `numbered(n, size, fill, textColor)` centres the step
number in the disc; the label scales with the disc size.

**A marker is the box it declares.** `custom(width, height, recipe)` reserves exactly that box:
the timeline lays out around it, and the marker's anchor — what the rail is derived from — is
that box.

- The recipe may draw smaller than the box; the rest of the box is simply empty.
- It may draw larger; the drawing overflows visibly and the box does not grow.
- The box need not be square.
- How thick an outline is does not change it — ink spreads about a shape's edge, the declared
  bounds do not follow it.

An outlined circle filled with the page's own colour is worth knowing: with the markers on the
rail, the line passes behind each ring and the fill covers it, so the rail reads as broken at
every stop without any change to its geometry.

```java
TimelineMarker.circle(14, DocumentColor.WHITE, DocumentStroke.of(accent, 1.2));
```

## markerOnRail(): markers on the line, not beside it

```java
section.addTimeline(timeline -> timeline
        .markerOnRail()
        .axisWidth(28)
        .entry(TimelineMarker.dot(6, accent), e -> e.title("Small").body("…"))
        .entry(TimelineMarker.numbered(2, 14, accent, DocumentColor.WHITE), e -> e.title("Medium"))
        .entry(TimelineMarker.square(24, accent), e -> e.title("Large")));
```

Opting in aligns each marker's declared anchor with the axis. Today's on-rail anchor is the
marker's centre, so markers of different declared sizes share **one** rail x — each is placed
inside the resolved axis column rather than packed to its left.

An entry's **body moves with it**, into the content column: with the rail inside the axis, a
body spanning the entry would be drawn through, so the body starts where the title starts and
the rail is left with only markers to cross. The body stays a normal vertical block — a long
one still splits across pages, keeping the same x and width on each — and it uses the column
the entry's own header row resolved, so a fixed axis and a weighted one behave alike. A leading
column works unchanged.

A timeline that does not call this keeps the placement it has always had.

## Rail configuration

```java
section.addTimeline(timeline -> timeline
        .connector(DocumentColor.rgb(150, 158, 172), 1.5)   // shorthand: colour + width
        .gutter(8)                                          // rail → marker gap
        .markerGap(8)                                       // marker → content gap
        .spacing(14)                                        // vertical gap between entries
        …);
```

```java
import com.demcha.compose.document.dsl.TimelineRailExtent;

section.addTimeline(timeline -> timeline
        .rail(r -> r
                .stroke(DocumentStroke.of(accent, 2.0))
                .extent(TimelineRailExtent.MARKER_TO_MARKER))
        …);
```

`connector(colour, width)` is the shorthand for `rail(r -> r.stroke(...))` and produces the
same rail. Use one spelling or the other: a timeline that configures the rail **both ways**
throws, naming both calls. Repeating the same spelling is ordinary setter accumulation and
stays legal — `connector(colour, 0)` then `connector(null, width)` has always been a way to set
the two halves separately. A call that changes nothing (a null colour and a non-positive width)
is not a use and does not count.

## Rail extent

| Extent | What the line covers |
|---|---|
| `ENTRY_BOUNDS` *(default)* | Every entry, on each page. The spacing between entries belongs to the entry above it, so the line is continuous through the gaps — and there is no tail after the last entry. |
| `MARKER_TO_MARKER` | The same per-page bands, trimmed: the first page starts at the first marker's anchor, the last ends at the last marker's, and a page carrying no marker runs its whole band. One entry means no rail at all rather than a line of no length. |
| `TIMELINE_BOUNDS` | **Not implemented.** Declared and rejected with a message: on one page it is the same line as `ENTRY_BOUNDS`, and across pages there is nothing to measure it against. |

Neither supported extent moves the rail sideways — where the line runs is the marker anchor's
business, how far it runs is the extent's.

## Pagination

A timeline paginates between entries by default, and a tall entry splits within itself — the
rail continues across the break. The one logical rail becomes one fragment per page it
occupies; nothing is repeated because content continued, and a marker is not redrawn on a
continuation page. Under `markerOnRail()` the body keeps the same content-column x and width on
every page it reaches.

```java
section.addTimeline(timeline -> timeline
        .keepTogether()          // relocate the whole timeline to a fresh page
        .keepEntriesTogether()   // never split one entry across pages
        .entry(TimelineMarker.dot(8, accent), e -> e.title("Atomic entry")));
```

`keepTogether()` moves the whole timeline to the next page when it does not fit in the
remaining space but would fit on a fresh one — timelines taller than a page still flow.
`keepEntriesTogether()` keeps each entry whole while still allowing breaks between entries.
Same semantics as the section-level controls in the [keep-together recipe](keep-together.md).

Only an entry's marker-plus-title row is atomic, so `AtomicNodeTooLargeException` is reachable
only in the degenerate case of a single marker row taller than a whole page.

## Backends

| | Rail | Content |
|---|---|---|
| **PDF** | ✅ drawn, one fragment per page, beneath the markers | ✅ |
| **PPTX** | ✅ same payload, same per-page fragments | ✅ |
| **DOCX** | ⚠️ omitted | ✅ entries, titles, meta and bodies all export |

DOCX is a semantic export: it walks the document tree and never consumes the resolved layout
geometry the rail is made of, so the line is absent by construction rather than by defect. The
export does not throw and the timeline's content comes through in full. See the
[backend capability matrix](../architecture/backend-capability-matrix.md).

## Text styles

Timeline-wide defaults and per-entry overrides:

```java
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

section.addTimeline(timeline -> timeline
        .titleStyle(DocumentTextStyle.builder()
                // The name picks the family, the decoration picks the face
                // within it — the built-in title default sets BOLD, so a style
                // that overrides it must set it too or the title turns regular
                // while only the size was meant to change.
                .fontName(FontName.HELVETICA)
                .decoration(DocumentTextDecoration.BOLD)
                .size(11)
                .build())
        .metaStyle(DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(8.5)
                .build())
        .entry(TimelineMarker.dot(8, accent), e -> e
                .title("Launch", DocumentTextStyle.builder()  // per-entry override
                        .fontName(FontName.HELVETICA)
                        .decoration(DocumentTextDecoration.BOLD)
                        .size(13)
                        .build())
                .meta("June 2026")
                .body("General availability.")));
```

`titleStyle` / `metaStyle` / `bodyStyle` on the timeline set the defaults for every entry; the
two-argument `title(text, style)`, `meta(text, style)`, and `body(text, style)` (or the matching
`*Style(...)` setters on the entry) override one entry.

---

Runnable demo:
[TimelineDemoTest](../../qa/src/test/java/com/demcha/testing/visual/TimelineDemoTest.java)
renders a marker/rail sheet to `target/visual-tests/timeline/timeline.pdf`.
