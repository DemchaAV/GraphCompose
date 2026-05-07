# GraphCompose Examples

A runnable, single-file Java example for every public surface in
GraphCompose — built-in templates, v1.5 cinematic features, public-API
showcases, and a kitchen-sink master demo. Each example writes a PDF
to `examples/target/generated-pdfs/`; the same PDFs are committed to
[`assets/readme/examples/`](../assets/readme/examples/) so you can
preview every render straight from GitHub without running anything.

## Run examples

Install the library artifact once from the repository root:

```bash
./mvnw -DskipTests install
```

Then run all 26 examples in one shot:

```bash
./mvnw -f examples/pom.xml exec:java \
    -Dexec.mainClass=com.demcha.examples.GenerateAllExamples
```

…or run a single example by passing its main class:

```bash
./mvnw -f examples/pom.xml exec:java \
    -Dexec.mainClass=com.demcha.examples.MasterShowcaseExample
```

Generated PDFs land in `examples/target/generated-pdfs/`. The same
`mvnw.cmd` form works on Windows PowerShell with backslash paths.

## Gallery

### Built-in document templates

| Example | What it shows | Preview · Source |
|---|---|---|
| [Cover Letter](#cover-letter) | One-page `BusinessTheme.modern()` cover letter with section presets | [PDF](../assets/readme/examples/cover-letter.pdf) · [Source](src/main/java/com/demcha/examples/templates/coverletter/CoverLetterFileExample.java) |
| [Invoice (V1)](#invoice-v1) | `InvoiceTemplateV1` driven from `InvoiceDocumentSpec` | [PDF](../assets/readme/examples/invoice.pdf) · [Source](src/main/java/com/demcha/examples/templates/invoice/InvoiceFileExample.java) |
| [Proposal (V1)](#proposal-v1) | `ProposalTemplateV1` driven from `ProposalDocumentSpec` | [PDF](../assets/readme/examples/proposal.pdf) · [Source](src/main/java/com/demcha/examples/templates/proposal/ProposalFileExample.java) |
| [Module-first Profile](#module-first-profile) | Authoring directly against `DocumentSession.module(...).paragraph(...)` | [PDF](../assets/readme/examples/module-first-profile.pdf) · [Source](src/main/java/com/demcha/examples/flagships/ModuleFirstFileExample.java) |
| [CV — single template](#cv-single-template) | One CV via `ModernProfessional.create(BusinessTheme.modern())` (Templates v2) | [PDF](../assets/readme/examples/cv-modern-professional.pdf) · [Source](src/main/java/com/demcha/examples/templates/cv/CvFileExample.java) |
| [CV — template gallery](#cv-template-gallery) | All 14 v2 CV presets in one orchestrated run | [Source](src/main/java/com/demcha/examples/templates/cv/CvTemplateGalleryFileExample.java) |
| [Cover letter — template gallery](#cover-letter-template-gallery) | All 14 paired v2 cover-letter presets in one orchestrated run | [Source](src/main/java/com/demcha/examples/templates/coverletter/CoverLetterTemplateGalleryFileExample.java) |

### Cinematic templates (v1.5)

| Example | What it shows | Preview · Source |
|---|---|---|
| [Invoice — cinematic V2](#invoice-cinematic-v2) | `InvoiceTemplateV2 + BusinessTheme.modern()` | [PDF](../assets/readme/examples/invoice-cinematic.pdf) · [Source](src/main/java/com/demcha/examples/templates/invoice/InvoiceCinematicFileExample.java) |
| [Proposal — cinematic V2](#proposal-cinematic-v2) | `ProposalTemplateV2 + BusinessTheme.modern()` | [PDF](../assets/readme/examples/proposal-cinematic.pdf) · [Source](src/main/java/com/demcha/examples/templates/proposal/ProposalCinematicFileExample.java) |
| [Handcrafted Proposal](#handcrafted-proposal) | v1.4-style cinematic proposal composed by hand | [PDF](../assets/readme/examples/project-proposal-cinematic.pdf) · [Source](src/main/java/com/demcha/examples/templates/proposal/CinematicProposalFileExample.java) |

### v1.5 feature showcases

| Example | What it shows | Preview · Source |
|---|---|---|
| [Shape containers](#shape-containers) | Circles, ellipses, rounded cards with `ClipPolicy.CLIP_PATH` | [PDF](../assets/readme/examples/shape-container.pdf) · [Source](src/main/java/com/demcha/examples/features/shapes/ShapeContainerExample.java) |
| [Transforms](#transforms) | `rotate`, `scale`, and per-layer `zIndex` swap | [PDF](../assets/readme/examples/transforms.pdf) · [Source](src/main/java/com/demcha/examples/features/transforms/TransformsExample.java) |
| [Advanced tables](#advanced-tables) | Row span, zebra rows, totals, repeating header on page break | [PDF](../assets/readme/examples/table-advanced.pdf) · [Source](src/main/java/com/demcha/examples/features/tables/TableAdvancedExample.java) |
| [Custom Business Theme](#custom-business-theme) | Hand-built `BusinessTheme` driving `InvoiceTemplateV2` | [PDF](../assets/readme/examples/invoice-custom-theme.pdf) · [Source](src/main/java/com/demcha/examples/features/themes/CustomBusinessThemeExample.java) |

### v1.6 feature showcases

| Example | What it shows | Preview · Source |
|---|---|---|
| [Nested lists](#nested-lists-v16) | `ListBuilder.addItem(label, Consumer)` — depth cascade, per-depth markers, mixed flat / nested authoring | [PDF](../assets/readme/examples/nested-list-showcase.pdf) · [Source](src/main/java/com/demcha/examples/features/lists/NestedListExample.java) |
| [Composed table cells](#composed-table-cells-v16) | `DocumentTableCell.node(DocumentNode)` — paragraphs, lists, sub-tables inside cells with two-pass measurement | [PDF](../assets/readme/examples/composed-table-cell-showcase.pdf) · [Source](src/main/java/com/demcha/examples/features/tables/ComposedTableCellExample.java) |
| [Canvas layer (free placement)](#canvas-layer-v16) | `CanvasLayerNode` — pixel-precise `(x, y)` placement of children inside a fixed bounding box, with `ClipPolicy` clipping | [PDF](../assets/readme/examples/canvas-layer-showcase.pdf) · [Source](src/main/java/com/demcha/examples/features/canvas/CanvasLayerExample.java) |

### Public-API surface

| Example | What it shows | Preview · Source |
|---|---|---|
| [Rich text](#rich-text) | Every `RichText` method (bold / italic / underline / link / colour / accent / size / link / append) | [PDF](../assets/readme/examples/rich-text-showcase.pdf) · [Source](src/main/java/com/demcha/examples/features/text/RichTextShowcaseExample.java) |
| [Section presets](#section-presets) | `pageBackground`, `band`, `softPanel`, `accentLeft / Right / Top / Bottom`, per-corner `DocumentCornerRadius` | [PDF](../assets/readme/examples/section-presets.pdf) · [Source](src/main/java/com/demcha/examples/features/text/SectionPresetsExample.java) |
| [Barcodes](#barcodes) | QR, Code 128, Code 39, EAN-13, EAN-8, branded QR with theme colours | [PDF](../assets/readme/examples/barcode-showcase.pdf) · [Source](src/main/java/com/demcha/examples/features/barcodes/BarcodeShowcaseExample.java) |
| [PDF chrome](#pdf-chrome) | `DocumentMetadata`, `DocumentWatermark`, `DocumentHeaderFooter`, `DocumentBookmarkOptions` | [PDF](../assets/readme/examples/pdf-chrome.pdf) · [Source](src/main/java/com/demcha/examples/features/chrome/PdfChromeExample.java) |

### Production patterns

| Example | What it shows | Preview · Source |
|---|---|---|
| [HTTP streaming](#http-streaming) | `writePdf(OutputStream)` for Servlet / S3 / GCS — caller's stream is not closed | [PDF](../assets/readme/examples/invoice-http-stream.pdf) · [Source](src/main/java/com/demcha/examples/features/streaming/HttpStreamingExample.java) |
| [Layout snapshot regression](#layout-snapshot-regression) | Deterministic `layoutSnapshot()` workflow with baseline + drift report | [PDF](../assets/readme/examples/invoice-snapshot-regression.pdf) · [Source](src/main/java/com/demcha/examples/features/snapshots/LayoutSnapshotRegressionExample.java) |

### Operational documents

| Example | What it shows | Preview · Source |
|---|---|---|
| [Weekly schedule](#weekly-schedule) | Bar / restaurant shift schedule via reusable `WeeklyScheduleRenderer` with typed `DayShift` API | [PDF](../assets/readme/examples/weekly-schedule.pdf) · [Source](src/main/java/com/demcha/examples/templates/schedule/WeeklyScheduleFileExample.java) |
| [Business report cover](#business-report-cover) | Single-page Q1 investor brief — hero image, KPI cards, bar chart, metrics table | [PDF](../assets/readme/examples/business-report.pdf) · [Source](src/main/java/com/demcha/examples/flagships/BusinessReportExample.java) |
| [Master showcase](#master-showcase) | Kitchen-sink "Q2 sample report" combining the canonical surface end-to-end | [PDF](../assets/readme/examples/master-showcase.pdf) · [Source](src/main/java/com/demcha/examples/flagships/MasterShowcaseExample.java) |

---

## Document templates

### Cover letter

A one-page modern cover letter — `BusinessTheme.modern()` drives every
colour and font choice; section presets (`softPanel`, `accentLeft`,
`accentTop`) carry the visual hierarchy; opening rich-text strip
highlights the candidate's headline.

```java
try (DocumentSession document = GraphCompose.document(outputFile)
        .pageSize(DocumentPageSize.A4)
        .pageBackground(THEME.pageBackground())
        .margin(56, 48, 56, 48)
        .create()) {

    document.pageFlow()
            .name("CoverLetter")
            .spacing(18)
            .addRow("Header", row -> row
                    .weights(3, 1)
                    .addSection("Identity", section -> section
                            .addParagraph(p -> p.text("Mariia Demchyshyn")
                                    .textStyle(THEME.text().h1())))
                    .addSection("Date", section -> section
                            .addParagraph(p -> p.text("15 May 2026"))))
            .addSection("Headline", section -> section
                    .softPanel(THEME.palette().surfaceMuted(), 10, 18)
                    .accentLeft(ACCENT, 4)
                    .addRich(rich -> rich
                            .plain("I help teams ship ")
                            .style("designed PDFs as code", BOLD_BRAND)
                            .plain(" — semantic Java DSL, deterministic layout.")))
            // … recipient block + body paragraphs + highlights row + closing …
            .build();

    document.buildPdf();
}
```

[📄 View PDF](../assets/readme/examples/cover-letter.pdf) ·
[📜 Full source](src/main/java/com/demcha/examples/templates/coverletter/CoverLetterFileExample.java)

### Invoice (V1)

`InvoiceTemplateV1.compose(document, spec)` handles the full layout —
header band, parties row, line-items table, totals row, payment-terms
footer — driven from a `InvoiceDocumentSpec`. Use this when you want
the legacy hard-coded theme; for V2 cinematic, see below.

```java
InvoiceDocumentSpec spec = InvoiceDocumentSpec.builder()
        .invoiceNumber("GC-2026-041")
        .issueDate("02 Apr 2026")
        .dueDate("16 Apr 2026")
        .fromParty(p -> p.name("GraphCompose Studio"))
        .billToParty(p -> p.name("Northwind Systems"))
        .lineItem("Template architecture", "Reusable invoice flow", "2", "GBP 980", "GBP 1,960")
        .totalRow("Total", "GBP 1,960")
        .build();

try (DocumentSession document = GraphCompose.document(outputFile)
        .pageSize(DocumentPageSize.A4)
        .margin(28, 28, 28, 28)
        .create()) {
    new InvoiceTemplateV1().compose(document, spec);
    document.buildPdf();
}
```

[📄 View PDF](../assets/readme/examples/invoice.pdf) ·
[📜 Full source](src/main/java/com/demcha/examples/templates/invoice/InvoiceFileExample.java)

### Proposal (V1)

`ProposalTemplateV1` rendered against a `ProposalDocumentSpec` —
sections, scope items, deliverables, sign-off. Pairs naturally with
`InvoiceTemplateV1` for consistent "spec → PDF" pipelines.

[📄 View PDF](../assets/readme/examples/proposal.pdf) ·
[📜 Full source](src/main/java/com/demcha/examples/templates/proposal/ProposalFileExample.java)

### Module-first profile

Authoring against `DocumentSession.pageFlow().module(...)` — no
template, no theme, just the canonical DSL. Smallest possible footprint
for "I just need a one-page PDF from data".

```java
document.pageFlow()
        .module("Profile", module -> module
                .heading("Mariia Demchyshyn")
                .paragraph("Senior Backend Engineer")
                .paragraph("mariia@example.com  ·  +44 20 7946 0234"));
document.buildPdf();
```

[📄 View PDF](../assets/readme/examples/module-first-profile.pdf) ·
[📜 Full source](src/main/java/com/demcha/examples/flagships/ModuleFirstFileExample.java)

### CV — single template

One CV rendered through the Templates v2 surface:
`ModernProfessional.create(BusinessTheme.modern())` paired with a
`CvSpec` data shape. The preset is one final class with one
`create(BusinessTheme)` factory — copy-and-tweak rather than
fork-a-monolith.

[📄 View PDF](../assets/readme/examples/cv-modern-professional.pdf) ·
[📜 Full source](src/main/java/com/demcha/examples/templates/cv/CvFileExample.java)

### CV — template gallery

Generates every v2 CV preset in one orchestrated run — 14 presets
covering single-column, two-column-sidebar, and three-column-magazine
layouts. Use this as the side-by-side catalogue when picking a base
preset for your own CV product. Each preset is a one-liner factory
(`ModernProfessional.create(theme)`, `NordicClean.create(theme)`,
…); see `templates/cv/presets/` for the full list.

| Variant | PDF |
|---|---|
| Modern professional | [PDF](../assets/readme/examples/cv-modern-professional.pdf) |
| Nordic clean | [PDF](../assets/readme/examples/cv-nordic-clean.pdf) |
| Classic serif | [PDF](../assets/readme/examples/cv-classic-serif.pdf) |
| Compact mono | [PDF](../assets/readme/examples/cv-compact-mono.pdf) |
| Timeline minimal | [PDF](../assets/readme/examples/cv-timeline-minimal.pdf) |
| Engineering resume (was "Tech lead") | [PDF](../assets/readme/examples/cv-tech-lead.pdf) |
| Panel (was "Product leader") | [PDF](../assets/readme/examples/cv-product-leader.pdf) |
| Executive · BoxedSections · CenteredHeadline · BlueBanner · EditorialBlue · SidebarPortrait · MonogramSidebar | run the gallery to render |

[📜 Full source](src/main/java/com/demcha/examples/templates/cv/CvTemplateGalleryFileExample.java)

### Cover letter — template gallery

Generates all 14 paired v2 cover-letter presets in one run — one
letter style per CV preset so a candidate's CV and cover letter
share the same visual language end-to-end. Each preset is a
one-liner factory (`ModernProfessionalLetter.create(theme)`,
`NordicCleanLetter.create(theme)`, …) under
`templates/coverletter/presets/`.

[📜 Full source](src/main/java/com/demcha/examples/templates/coverletter/CoverLetterTemplateGalleryFileExample.java)

---

## Cinematic templates (v1.5)

### Invoice — cinematic V2

`InvoiceTemplateV2(BusinessTheme.modern())` — the cinematic invoice.
Hero `softPanel` with invoice number / dates / inline rich-text status,
two-column parties row, themed line-items table with `headerStyle` +
zebra + totals + `repeatHeader()`, footer row with `accentLeft` strips.

```java
BusinessTheme theme = BusinessTheme.modern();
InvoiceTemplateV2 template = new InvoiceTemplateV2(theme);

try (DocumentSession document = GraphCompose.document(outputFile)
        .pageSize(DocumentPageSize.A4)
        .pageBackground(theme.pageBackground())
        .margin(28, 28, 28, 28)
        .create()) {
    template.compose(document, invoice);
    document.buildPdf();
}
```

[📄 View PDF](../assets/readme/examples/invoice-cinematic.pdf) ·
[📜 Full source](src/main/java/com/demcha/examples/templates/invoice/InvoiceCinematicFileExample.java)

### Proposal — cinematic V2

`ProposalTemplateV2` — same `BusinessTheme`-driven pattern as the
invoice. Hero rounded only on the right (`DocumentCornerRadius.right(...)`),
themed executive-summary panel, sender / recipient parties row,
`theme.text().h2()` headings, timeline + pricing tables with
`repeatHeader()`, zebra rows, and a `totalRow(...)`.

[📄 View PDF](../assets/readme/examples/proposal-cinematic.pdf) ·
[📜 Full source](src/main/java/com/demcha/examples/templates/proposal/ProposalCinematicFileExample.java)

### Handcrafted proposal

A v1.4-style cinematic proposal composed by hand — no template — to
show how the same primitives compose without a `XxxTemplateV2`
wrapper. Useful starting point when your domain doesn't fit any
built-in template.

[📄 View PDF](../assets/readme/examples/project-proposal-cinematic.pdf) ·
[📜 Full source](src/main/java/com/demcha/examples/templates/proposal/CinematicProposalFileExample.java)

---

## v1.5 feature showcases

### Shape containers

`addContainer(...)`, `addCircle(...)`, `addEllipse(...)` build a
`ShapeContainerNode` whose bounding box is dictated by a
`ShapeOutline`. Children are clipped via `ClipPolicy.CLIP_PATH`
(default), `CLIP_BOUNDS`, or `OVERFLOW_VISIBLE`. The
`ShapeContainerBuilder` exposes the same nine-point alignment
vocabulary as `LayerStackBuilder` plus `position(node, dx, dy, anchor)`
for screen-space nudges.

```java
.addContainer(
        ShapeOutline.RoundedRectangle.of(220, 140, 14),
        ClipPolicy.CLIP_PATH,
        DocumentColor.rgb(28, 31, 38),                       // background
        DocumentStroke.of(DocumentColor.rgb(196, 153, 76), 1.0),
        container -> container
                .center(centeredImage)                       // 9-point alignment
                .position(badge, 120, 18, Anchor.TOP_LEFT))  // pixel-precise
```

[📄 View PDF](../assets/readme/examples/shape-container.pdf) ·
[📜 Full source](src/main/java/com/demcha/examples/features/shapes/ShapeContainerExample.java)

### Transforms

`DocumentTransform.rotate(deg)` and `.scale(uniform | sx, sy)` —
attached to any `Transformable<T>` builder
(`ShapeContainerBuilder`, `ShapeBuilder`, `LineBuilder`,
`EllipseBuilder`, `ImageBuilder`, `BarcodeBuilder`). Identity
transforms emit no markers, so layout snapshots stay byte-identical to
v1.4. Per-layer `zIndex` lets a layer declared earlier draw on top of
layers declared later — `LayerStackNode.Layer` and shape-container
layers both gain `int zIndex` (default `0`).

```java
.addCircle(60, ROYAL_BLUE, container -> container
        .rotate(15)
        .center(label))

.layer(redSquare,  Anchor.CENTER, /* zIndex */ 10)   // declared first, drawn on top
.layer(tealSquare, Anchor.CENTER)                    // declared second, drawn beneath
```

[📄 View PDF](../assets/readme/examples/transforms.pdf) ·
[📜 Full source](src/main/java/com/demcha/examples/features/transforms/TransformsExample.java)

### Advanced tables

`DocumentTableCell.rowSpan(int)` mirrors `colSpan(int)`.
`TableBuilder.zebra(odd, even)` paints alternating rows.
`totalRow(...)` adds a bold-on-grey-blue totals row.
`repeatHeader()` re-emits the leading rows on every continuation page
when the table paginates.

```java
table.columns(...)
        .headerRow("Item", "Description", "Qty", "Unit", "Amount")
        .repeatHeader()                                   // pinned on every page
        .zebra(zebraOdd, zebraEven)
        .row("Tall", "Spans 3 rows", "—", "—", "—") // colSpan(2).rowSpan(3) cell
        .row("…", …)
        .totalRow("", "", "", "Total", "GBP 1,960");
```

[📄 View PDF](../assets/readme/examples/table-advanced.pdf) ·
[📜 Full source](src/main/java/com/demcha/examples/features/tables/TableAdvancedExample.java)

### Custom Business Theme

Build a `BusinessTheme` from raw `DocumentPalette` / `SpacingScale` /
`TextScale` / `TablePreset` records — no factory shortcut. Plug it
straight into `InvoiceTemplateV2` to retheme the whole template
without touching any code that uses it.

```java
BusinessTheme studioEmerald = new BusinessTheme(
        new DocumentPalette(/* page, surface, surfaceMuted, ink, accent, … */),
        SpacingScale.cinematic(),
        new TextScale(/* h1, h2, body, caption fonts … */),
        TablePreset.cinematic());

new InvoiceTemplateV2(studioEmerald).compose(document, invoice);
```

[📄 View PDF](../assets/readme/examples/invoice-custom-theme.pdf) ·
[📜 Full source](src/main/java/com/demcha/examples/features/themes/CustomBusinessThemeExample.java)

---

## Public-API surface

### Rich text

Every `RichText` method laid out as labelled rows on a single A4 page:
`plain`, `bold`, `italic`, `boldItalic`, `underline`, `strikethrough`,
`color`, `accent`, `size`, `style`, `link`, `append`. Use this as the
visual reference when picking which call to make for inline text.

```java
.addRich(rich -> rich
        .plain("Customer ")
        .bold("Northwind Systems")
        .plain(" placed order ")
        .accent("#GC-2026-041", BRAND_GOLD)
        .plain(" — see ")
        .link("invoice", "https://example.com/invoice/41")
        .plain("."))
```

[📄 View PDF](../assets/readme/examples/rich-text-showcase.pdf) ·
[📜 Full source](src/main/java/com/demcha/examples/features/text/RichTextShowcaseExample.java)

### Section presets

`pageBackground`, `band`, `softPanel`, the four
`accentLeft / accentRight / accentTop / accentBottom` strips, and
per-corner `DocumentCornerRadius` (`top`, `bottom`, `left`, `right`,
`only(...)`) rendered side-by-side as recipe cards.

```java
.addSection("Hero", section -> section
        .softPanel(theme.palette().surfaceMuted(), 10, 18)
        .accentLeft(theme.palette().accent(), 4)
        .cornerRadius(DocumentCornerRadius.right(12))
        .addParagraph(p -> p.text("Hero block").textStyle(theme.text().h1())))
```

[📄 View PDF](../assets/readme/examples/section-presets.pdf) ·
[📜 Full source](src/main/java/com/demcha/examples/features/text/SectionPresetsExample.java)

### Barcodes

`BarcodeBuilder` with five symbology types — `QR`, `Code 128`,
`Code 39`, `EAN-13`, `EAN-8` — plus a branded QR using the active
theme's foreground / background colours. ZXing is the encoder; the
PDF backend rasterises and embeds.

```java
.addBarcode(b -> b
        .symbology(BarcodeSymbology.QR_CODE)
        .data("https://github.com/DemchaAV/GraphCompose")
        .size(140, 140)
        .foreground(theme.palette().ink())
        .background(theme.palette().surface()))
```

![Barcode showcase preview](../assets/readme/barcode-showcase.png)

[📄 View PDF](../assets/readme/examples/barcode-showcase.pdf) ·
[📜 Full source](src/main/java/com/demcha/examples/features/barcodes/BarcodeShowcaseExample.java)

### PDF chrome

Backend-neutral `DocumentMetadata`, `DocumentWatermark`,
`DocumentHeaderFooter` (header + footer with `{page} / {pages} /
{date}` tokens), and paragraph-level `DocumentBookmarkOptions`
materialising as PDF outline entries.

```java
GraphCompose.document(outputFile)
        .metadata(DocumentMetadata.builder()
                .title("Q1 2026 Investor Brief")
                .author("Mariia Demchyshyn")
                .build())
        .watermark(DocumentWatermark.draftStamp(theme.palette().muted()))
        .headerFooter(DocumentHeaderFooter.tokens("Q1 Brief", "Page {page} of {pages}"))
        .create();
```

[📄 View PDF](../assets/readme/examples/pdf-chrome.pdf) ·
[📜 Full source](src/main/java/com/demcha/examples/features/chrome/PdfChromeExample.java)

---

## Production patterns

### HTTP streaming

`writePdf(OutputStream)` for Servlet / S3 / GCS adopters. The caller's
stream is **not** closed by GraphCompose — pinned by
`HttpStreamingDemoTest`. A Spring Boot `@RestController` snippet in the
example javadoc shows the canonical wiring.

```java
@GetMapping(value = "/invoice/{id}", produces = MediaType.APPLICATION_PDF_VALUE)
public ResponseEntity<StreamingResponseBody> invoice(@PathVariable Long id) {
    InvoiceDocumentSpec spec = invoiceService.loadInvoice(id);

    StreamingResponseBody body = response -> {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .pageBackground(BusinessTheme.modern().pageBackground())
                .margin(28, 28, 28, 28)
                .create()) {
            new InvoiceTemplateV2(BusinessTheme.modern()).compose(document, spec);
            document.writePdf(response);   // streams directly, no in-memory PDF
        }
    };

    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice.pdf")
            .body(body);
}
```

[📄 View PDF](../assets/readme/examples/invoice-http-stream.pdf) ·
[📜 Full source](src/main/java/com/demcha/examples/features/streaming/HttpStreamingExample.java)

### Layout snapshot regression

The full `compose → layoutSnapshot() → LayoutSnapshotJson.toJson(...)`
workflow with a copy-and-paste baseline / drift report pattern, plus a
pointer to the production
`LayoutSnapshotAssertions.assertMatches(document, "...")` helper for
in-test usage.

```java
DocumentSession document = GraphCompose.document(outputFile)…create();
new InvoiceTemplateV2(theme).compose(document, spec);

String snapshot = LayoutSnapshotJson.toJson(document.layoutSnapshot());
String baseline = Files.readString(baselinePath);
if (!baseline.equals(snapshot)) {
    Files.writeString(driftPath, snapshot);
    throw new AssertionError("Layout drift detected — diff " + driftPath);
}
document.buildPdf();
```

[📄 View PDF](../assets/readme/examples/invoice-snapshot-regression.pdf) ·
[📜 Full source](src/main/java/com/demcha/examples/features/snapshots/LayoutSnapshotRegressionExample.java)

---

## Operational documents

### Weekly schedule

Bar / restaurant weekly shift schedule rendered through the canonical
DSL via a reusable `WeeklyScheduleRenderer`. The renderer's typed API
lets you express any combination of full-day status fills, half-day
shifts (lunch / dinner), and cross-meal shifts without parsing strings:

```java
import com.demcha.examples.support.WeeklyScheduleRenderer;
import com.demcha.examples.support.WeeklyScheduleRenderer.*;

private static final List<StaffMember> STAFF = List.of(
        new StaffMember("AARON PARK",  JobTitle.MANAGER),
        new StaffMember("DIANA COLE",  JobTitle.BARTENDER),
        new StaffMember("JASPER LIN",  JobTitle.BAR_BACK)
);

private static final Map<String, DayShift[]> SHIFTS = Map.ofEntries(
        Map.entry("AARON PARK", new DayShift[] {
                DayShift.acrossDay("09:00", "18:00", ShiftType.STOCK),  // Mon — stock recon all day
                DayShift.OFF,                                            // Tue
                DayShift.OFF,                                            // Wed
                DayShift.dinnerOnly("16:00", "00:00"),                   // Thu — dinner only
                DayShift.OFF,                                            // Fri
                DayShift.shifts("11:00", "16:00", "16:00", "22:00"),     // Sat — both halves
                DayShift.shifts("08:00", "13:00", "13:00", "18:00")      // Sun
        }),
        Map.entry("DIANA COLE", new DayShift[] {
                DayShift.halves(Half.shift("12:00", "20:00"), Half.STANDBY),
                // …
        })
);

WeeklyScheduleRenderer.renderTo(outputFile, "AURORA",
        LocalDate.of(2026, 5, 4), STAFF, WEEK, SHIFTS);
```

The renderer auto-fills the seven date labels from `weekStart`
(`Monday 4th` / `Tuesday 5th` / …), sorts staff by `JobTitle.ordinal()`
so groups appear in declared order, and emits a separator row at every
job-title boundary so adding or removing a `StaffMember` never
requires updating positional indices. The colour palette and column
widths live behind `Theme.aurora()` and `Layout.landscape()` records,
so reskinning is one parameter swap.

[📄 View PDF](../assets/readme/examples/weekly-schedule.pdf) ·
[📜 Example source](src/main/java/com/demcha/examples/templates/schedule/WeeklyScheduleFileExample.java) ·
[📜 Renderer source](src/main/java/com/demcha/examples/support/WeeklyScheduleRenderer.java)

### Business report cover

Single-page Q1 investor brief — top band identifier, serif headline,
procedurally rendered hero image (Java `Graphics2D` PNG embedded via
`DocumentImageData.fromBytes(...)`), three gold-ringed KPI cards,
strategic-highlights bullet list paired with a five-quarter Revenue /
Profit bar chart, YoY metrics table, and a confidential / page-number
footer. Use this as the visual reference for landing-page hero shots.

[📄 View PDF](../assets/readme/examples/business-report.pdf) ·
[📜 Full source](src/main/java/com/demcha/examples/flagships/BusinessReportExample.java)

### Master showcase

Fictional "Q2 sample report" combining the canonical surface
end-to-end: `BusinessTheme` + page background + hero with rotated
shape container + branded QR + executive summary + zebra-striped
totals table + accent-bordered highlight cards + Code 128 footer
barcode. Reference it when composing your own multi-page documents.

[📄 View PDF](../assets/readme/examples/master-showcase.pdf) ·
[📜 Full source](src/main/java/com/demcha/examples/flagships/MasterShowcaseExample.java)

---

## Anatomy of an example

Every example file follows the same shape:

```java
public final class FooExample {

    private static final BusinessTheme THEME = BusinessTheme.modern();
    // … more constants if useful …

    private FooExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("foo.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(THEME.pageBackground())
                .margin(28, 28, 28, 28)
                .create()) {

            document.pageFlow()
                    .name("Foo")
                    .spacing(12)
                    // … your composition …
                    .build();

            document.buildPdf();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
```

This makes every example **runnable on its own** (`main`), **callable
from `GenerateAllExamples`** (`generate()` returns the output path),
and **deterministic** — the same data + same code always produces the
same bytes (verified by `LayoutSnapshotRegressionExample`).

For the canonical authoring patterns — builder hierarchy, theme
tokens, table presets, golden / anti-patterns, and a 40-line skeleton
for new templates — read
[**`docs/template-authoring.md`**](../docs/template-authoring.md)
once before writing your own.

## Where things live

| Path | What's there |
|---|---|
| `examples/src/main/java/com/demcha/examples/` | One file per example, runnable via `main` |
| `examples/src/main/java/com/demcha/examples/support/` | Reusable helpers (`ExampleOutputPaths`, `WeeklyScheduleRenderer`) |
| `examples/target/generated-pdfs/` | Output of running the examples (gitignored) |
| `assets/readme/examples/` | Committed PDF previews linked from this gallery |
| `docs/template-authoring.md` | Template authoring cheatsheet |
| `CHANGELOG.md` | Per-version surface changes (every example link is current to v1.6) |
