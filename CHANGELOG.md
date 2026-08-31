# Changelog

All notable changes to GraphCompose are documented here. Versions
follow semantic versioning; release dates are ISO 8601.

## v2.2.3 — Planned

### Public API

- **The structured invoice model carries what a second sheet needs.** It landed with
  one consumer, `ConsultingInvoice`, and a model shaped around one document is a model
  nobody has tested. Fitting a second published invoice to it found six things it could
  not say, each of them general rather than one design's whim: a brand lockup drawn as
  a two-line monogram instead of a logo (`InvoiceBrand.monogramTop` / `monogramBottom`);
  a second labelled registration, because a UK sender prints both a company number and
  a VAT number (`InvoiceContactBlock.taxRegistrationLabel` / `taxRegistrationNumber`);
  a delivery address beside the billing one (`StructuredInvoiceData.shipTo`); a tax
  rate printed per line and its column (`InvoiceServiceLines.Line.vatRate`,
  `Columns.vat`), written as the design shows it because the wording differs by
  jurisdiction; and who the money is paid to, with the closing line beside the due
  notice (`InvoicePaymentBlock.accountHolder` / `signOff`). Every addition is blank
  when absent, and every constructor that predates one is kept explicitly, so existing
  calls compile and link unchanged — `ConsultingInvoice` passes its snapshot and pixel
  gates untouched, which is the proof.

- **`CvEntry` carries a link.** An entry that points somewhere — a repository, a case
  study, a company — had no way to say so, and a preset had no way to make its title
  reachable. `CvEntry` now carries `link`, a plain string blank when absent like the
  fields beside it, set through `CvEntry.Builder.link(...)`. It costs the layout
  nothing: a link is an annotation rather than ink, so a linked title and a plain one
  are the same sheet, which is also why the parity gates cannot see it and a test
  asserts the targets directly. The six-argument constructor is kept explicitly, so
  existing calls compile and link unchanged.

- **`CvEntry` carries a location and a mark, and gains a builder.** The record held a
  title, a subtitle, a date and a body, which is enough for a dated block and not
  enough for the designs that set the city beside the employer in its own colour, or
  that open a project with an icon. Folding a location into the subtitle would have
  merged two things a design styles apart; deriving an icon from the text would have
  been guesswork. `CvEntry` now carries `place` and `icon` — both plain strings, blank
  when absent, matching how `subtitle` and `date` already behave — and
  `CvEntry.builder(title)` reaches them without counting six positions. The icon
  vocabulary is preset-scoped: a token means something only to the preset that packages
  it, and the presets that draw no marks ignore it. The four-argument constructor is
  kept explicitly, so existing calls compile and link unchanged.

- **`CvIdentity` carries an optional portrait.** A CV design with a photograph in it
  had nowhere to put one: the identity record held the name, the title, the contact
  triple and the links, and a preset that wanted a face had to ship a silhouette of its
  own. `CvIdentity` now carries `Optional<DocumentImageData> portrait` — the image
  itself, because a photograph is caller-supplied content rather than template chrome —
  reachable through `CvIdentity.Builder.portrait(...)`. The four- and three-argument
  constructors are kept explicitly, so existing calls compile and link unchanged; only a
  record deconstruction pattern over `CvIdentity` sees the extra component. A document
  carrying a portrait still renders through every preset in the family — the ones with
  nowhere to draw it ignore it.

- **A structured invoice document model.** The invoice family's data layer knew one
  shape — an invoice as pre-formatted display strings, with one address block per
  party, line items whose quantity and money are already rendered, and a flat list of
  summary rows. That cannot carry the structured business invoice: a brand lockup with
  the sender's own logo, labelled masthead metadata, a contact block with a business
  registration, priced service lines carrying `BigDecimal` figures and the unit they
  are counted in, a totals stack with its own total band, bank payment fields, and a
  footer line. `templates.data.invoice` now carries that second model —
  `StructuredInvoiceData` (+ its section records) wrapped by
  `StructuredInvoiceDocumentSpec` — alongside the display one; a preset consumes the
  model whose shape it renders. The brand logo arrives as `DocumentImageData`, because
  the logo is caller-supplied content rather than template chrome, and it stays
  optional so a wordmark-only lockup composes. Every component normalizes `null` to
  its empty form, money and quantities default to zero, and collections are frozen.

- **A structured proposal document model.** The proposal family's data layer knew one
  shape — a titled run of prose sections with a flat timeline and pricing list — which
  cannot carry the structured business proposal: brand marks, an authored multi-line
  title, an at-a-glance fact card, goal cells, a numbered scope list, authored
  deliverable columns, a phase grid with its own headers, priced rows with a
  `Role` (`NONE` / `SUBTOTAL` / `OPTIONAL`) and a total band, and a signing card.
  `templates.data.proposal` now carries that second model —
  `StructuredProposalData` (+ its section records) wrapped by
  `StructuredProposalDocumentSpec` — alongside the narrative one; a preset consumes
  the model whose shape it renders. Every component normalizes `null` to its empty
  form and freezes its collections, matching the family's existing records.

### Templates

- **An architect's two-column CV preset: `TerracottaRail`.** A one-page A4 sheet whose
  narrow column carries a serif monogram over a terracotta rule, the contact channels
  behind their marks, two bulleted lists and a block of closing facts, beside a wide
  column carrying a letter-spaced masthead, the summary, the roles held on a ringed
  rail, a projects grid and the degrees. Ships as `cv.presets.TerracottaRail` on the
  existing `CvDocument` model with **no model change at all**, porting the rendered
  layout of a published standalone template. Like `CharcoalGold` it leaves the page to
  the caller. Eight berths reach their sections by title; the two bulleted lists are
  skills without levels, because this design writes them as plain lines — one takes a
  terracotta square and no dash under its heading, the other a disc and a dash, which
  is how the sheet tells two lists of one-liners apart. Each fact and each project
  takes the mark its entry names in `CvEntry.icon()` from this preset's own vocabulary,
  a project title is a link when its entry carries one, and the monogram is drawn from
  the name's own initials rather than a field of its own — a document states its name
  once, and a monogram that could disagree with it would be a second place to keep
  true. **A link in the contact block is drawn as its own label with the address behind
  it**, which is where the preset departs from the sheet it ports: writing the URL out
  makes that row as wide as whatever the reader's profile happens to be called — long
  enough that the published design sets it smaller and nudged in, on an axis of its
  own. Stating `Link("LinkedIn", "https://…")` puts the four rows on one axis at one
  size for every document. The departure is exactly measured — two of 154 nodes narrow,
  1 743 of 2 173 720 pixels change, and nothing moves vertically — and both baselines
  were recorded with it. A link takes the mark of the network it points at, or a globe.
  Like its siblings it holds one page: the body is a single atomic row, so a longer CV raises
  `AtomicNodeTooLargeException` rather than flowing or dropping entries. Guarded by a
  smoke test (including the unknown-mark data error, an entry with no mark, an identity
  with no links, a document with nothing but an identity, the monogram, the link
  targets, the four contact rows sharing one axis and the one-page limit), an exact
  layout snapshot and a pixel-parity gate;
  the examples showcase gains `cv-terracotta-rail-v2`.

- **The first invoice preset that paginates what it ports: `LumaStudioInvoice`.** A
  studio invoice built around a cream sidebar — the two-line monogram and the wordmark
  on a terracotta block at its head, a tinted quarter-disc, an arch and a sprig running
  down the rest of it — beside a billing sheet that reads sender, title and metadata,
  the billed-to and shipped-to pair across a rule, the priced service lines with a VAT
  column, the totals stack closing on a filled total-due band, and the notes and bank
  details above a dark sign-off band. Ships as `invoice.presets.LumaStudioInvoice` on
  the structured invoice model, porting the rendered layout of the published standalone
  `luma-co-studio-invoice` template. Unlike the CV presets promoted before it this one
  flows: the line-items table repeats its dark header on the next page, the totals stack
  and each closing block stay whole, and the paper tint, the sidebar column and the dark
  foot band are page backgrounds, so every page carries the same frame and the folio
  always has a dark ground. **The sign-off carries its own strip, which is where the
  preset deliberately departs from the sheet it ports.** On the published template the
  words rely on that background band, which is pinned to the paper's edge — right on a
  one-page invoice, and white-on-cream in the middle of the last page of a longer one,
  where the flow ends well above the paper's foot. Drawing the strip with the words
  fixes it wherever they land; the cost is 7 440 of 2 173 720 pixels on the reference
  sheet, where the strip starts three points above the background band it sits on, and
  one extra layout node. It is flow content only because footer chrome carries text
  today — one size, one colour, no glyph, where this band needs two faces and a disc —
  so once a footer zone can hold a node the sign-off belongs in one and the strip goes
  away. Its geometry is not a set of round numbers: the design was drawn
  on a pixel grid, so the preset carries that grid as page ratios and states every
  vertical gap as the white the drawing shows, subtracting the blank a line box already
  carries — per family, because the three faces it sets fill different line boxes at the
  same size. Letter-spaced runs are written letter by letter with an invisible inline
  rectangle between the pairs, since a text style carries no tracking; the six spacings
  are frozen constants rather than a measurement, because measuring at compose time
  would tie the preset to the font artifact's resource layout. Amounts take their mark
  from `StructuredInvoiceData.currencyCode()` — the code is the authority, so the sheet
  cannot contradict itself — and the contact channels carry `tel:`, `mailto:` and
  `https:` targets derived from their values, with a parenthesised trunk prefix dropped
  from the dial target the way a caller dialling from abroad drops it. Guarded by a
  smoke test (including the empty document, an unknown and a blank currency code, a
  supplier with one registration and with none, the link targets, the repeated header
  and folio on a continuation page, and the sign-off landing on its own ground), an
  exact layout snapshot over both a one-page and a three-page invoice, and a
  pixel-parity gate; the examples showcase gains `invoice-luma-studio-v2`.

- **`SerifHeadline` links its titles and lets its bands breathe.** Every title the
  preset draws — a role, a project, a degree, an achievement — is now a link when its
  entry carries one. And a band column keeps a gutter at its right edge, so its text
  stops short of the hairline between columns instead of running into it: **the first
  place a promoted preset deliberately departs from the sheet it ports**, where a line
  that happens to fill its column touches the rule. The departure is exactly measured —
  six of 235 nodes narrow, 8 191 of 2 173 720 pixels change, and nothing else moves —
  and both baselines were re-recorded with it. The class documentation names the two
  colours the packaged marks come in, so a document chooses one deliberately rather
  than mixing navy and gold in a row by accident.

- **A photographic CV preset: `CharcoalGold`.** A one-page sheet in two columns: a
  charcoal sidebar carrying a ringed photograph, the contact channels behind their
  marks, rated skills, languages and degrees, beside a paper column carrying a two-tone
  name — the given name in ink, the family name larger and in gold — the summary, the
  roles held on a dated rail, a pair of credential columns divided by a hairline, and a
  closing strip of tools. Ships as `cv.presets.CharcoalGold` on the existing
  `CvDocument` model with **no model change at all**, porting the rendered layout of the
  published standalone `charcoal-gold-cv` template. Unlike its ported siblings it leaves
  the page size to the caller: the design is drawn on A4 and its geometry follows the
  page's own width, so a different page rescales rather than breaks. Seven berths reach
  their sections by title; credentials take the mark each entry names in
  `CvEntry.icon()` from this preset's own set, every title it draws is a link when the
  entry carries one, and a skill the document leaves unlevelled draws no rating rather
  than five empty dots. Guarded by a smoke test (including the unknown-mark data error,
  the missing portrait, the unlevelled skill, the link targets and the one-page limit),
  an exact layout snapshot and a pixel-parity gate; the examples showcase gains
  `cv-charcoal-gold-v2`.

- **A two-column editorial CV preset: `SerifHeadline`.** A one-page A4 sheet under a
  Volkhov masthead — the name in the display serif over its role and a short gold rule,
  the contact channels stacked opposite — then a two-column body: the roles held on a
  timeline rail and the projects as marked cards on the left, the degrees and grouped
  skill meters across a hairline divider on the right, closing with full-width bands of
  certifications and achievements. Ships as `cv.presets.SerifHeadline` on the existing
  `CvDocument` model, porting the rendered layout of the published standalone
  `serif-headline-cv` template. Its geometry is not a set of round numbers: the design
  was drawn on a 1024-pixel grid, so the preset carries that grid and scales it onto A4
  — heights by an extra factor — and states every vertical gap as the white the drawing
  shows, subtracting the blank a line box already carries above and below its own type.
  Eight berths reach their sections by title, the projects and achievements take the
  mark each entry names in `CvEntry.icon()` from this preset's own vocabulary, and the
  employer's city and the campus come from `CvEntry.place()`. Like its siblings it owns
  its page and holds one: the body is a single atomic row, so a longer CV raises
  `AtomicNodeTooLargeException` rather than flowing or dropping entries. Guarded by a
  smoke test (including the unknown-mark data error, an entry with no mark, an employer
  with no place, the overlapping soft-skills berth, the link targets and the one-page
  limit), an exact layout snapshot and a pixel-parity gate; the examples showcase gains
  `cv-serif-headline-v2`.

- **A portrait CV preset: `NavySidebar`.** A one-page A4 CV in two columns on Lato —
  a navy plate carrying a ringed portrait, the contact channels behind their marks, the
  degrees, the skills and the languages, beside a white column of the name, the summary,
  the roles held on a timeline rail with a filled marker at each one, and the
  achievements and certifications behind badged headings. Ships as
  `cv.presets.NavySidebar` on the existing `CvDocument` model, porting the rendered
  layout of the published standalone `navy-sidebar-cv` template. Like its sibling it
  owns its page and holds one: the two columns are a single atomic row, so a CV longer
  than the sheet raises `AtomicNodeTooLargeException` rather than flowing or dropping
  entries. Sections reach their berth by title rather than by `Slot`; languages are a
  `RowsSection` because this design writes the proficiency out — "Native", "Advanced" —
  which a levelled skill could not carry back. The photograph comes from the new
  `CvIdentity.portrait()`; an identity without one draws the ring around an empty navy
  disc; its education entries read the `place` field for the campus line. The phone,
  the email and each link are reachable from the PDF, with the
  `tel:` and `mailto:` targets built from the values — the published sheet drew its
  channels as plain text, and this is the one place the port deliberately improves on
  it, at no cost to the render: annotations move no pixel and no layout node, which
  both gates confirm without re-blessing. Guarded by a smoke test (including the link
  targets, the missing portrait, the capitals this design imposes, the one-page limit
  and the fields it has no place for), an exact layout snapshot and a pixel-parity
  gate; the examples showcase gains `cv-navy-sidebar-v2`.

- **The first CV preset that owns its page: `ProfessionalSidebar`.** A one-page CV
  in two columns on the Barlow&nbsp;Condensed / Lato pair — a navy monogram plate over a
  pale sidebar carrying the contact channels, meter-bar skills, an education rail with
  dot markers and five-dot language ratings, beside a white column of the tracked name,
  the profile, the roles held with bulleted highlights, the projects and the references
  note. Ships as `cv.presets.ProfessionalSidebar` on the existing `CvDocument` model —
  no model change was needed — porting the rendered layout of the published standalone
  `professional-sidebar-cv` template. The preset owns its page: a 491.6&nbsp;x&nbsp;737.28pt
  sheet with no margin, the page fill and the pale sidebar painted as page backgrounds. The
  sheet holds one page — its two columns are a single atomic row, so a CV longer than the
  sheet raises `AtomicNodeTooLargeException` rather than flowing onto a second page or
  silently dropping entries the way the capped sidebar presets do. The class documentation
  says so, points at `TimelineMinimal` for a preset that splits its own columns, and
  `docs/templates/v2-layered/using-templates.md` carries it beside the capped presets.
  Sections reach their berth by title rather than by `Slot`,
  because the columns are fixed; skills and languages are both `SkillsSection`s drawn
  differently, and a level the document omits draws the name alone. The contact channels
  come off `CvIdentity`, with `tel:` and `mailto:` targets built from the values and the
  packaged marks chosen per channel. Guarded by a smoke test (including the identity-only
  document, the unlevelled skill, the PDF link targets, the one-page limit and the fields
  this design has no place for), an exact layout snapshot and a pixel-parity gate; the
  examples showcase gains `cv-professional-sidebar-v2`.

- **A second structured proposal preset: `EditorialProposal`.** The same document the
  `NorthlineProposal` preset renders, in a different hand: an orange accent, section
  headings set in the display serif over short accent rules instead of in the body sans
  inside icon badges, a brand mark drawn from vector paths instead of a monogram letter,
  an untitled fact card, a scope ordinal set in plain accent text, and a hairline page
  foot. Moving a document between the two presets is a one-line change plus two data
  checks — the badge and goal icon tokens are preset-scoped (the four `fact-*` tokens
  are not), and headings are drawn as authored, so the sibling's tracked capitals stay
  capitals here. Ships as
  `proposal.presets.EditorialProposal` on the structured proposal model, porting the
  rendered layout of the published standalone `northline-proposal-orange` template,
  with its SVG icon set packaged in the artifact. Guarded by a smoke test (including
  the empty document, both data contracts, and the proof that one document renders
  through both proposal presets), an exact two-page layout snapshot, and a
  pixel-parity gate; the examples showcase gains `proposal-editorial-v2`.

- **A professional-services invoice preset: `ConsultingInvoice`.** A corporate masthead
  — brand lockup and contact channels beside the document title and its metadata —
  over priced service lines that carry a service period and a unit per line, closing
  with an emphasized total band and the bank details beside the notes and the due-by
  chip. Ships as `invoice.presets.ConsultingInvoice` consuming the new structured
  invoice model, with its contact marks, bank badge and calendar packaged in the
  templates artifact, porting the rendered layout of the published standalone
  `northpoint-consulting-invoice` template. Long invoices flow: the line-items table
  repeats its header and the totals stack stays whole. Guarded by a smoke test
  (including the empty document, the wordmark fallback, the rendered figures and the
  repeated header), exact layout snapshots for the single page and the overflow, and a
  pixel-parity gate; the examples showcase gains `invoice-consulting-v2`.

- **A second invoice preset: `ClassicInvoice`.** The letterhead-style invoice — a header
  band with the company name and a 28pt INVOICE title, a TOTAL DUE hero strip,
  BILL&nbsp;TO / FROM party columns, and a dedicated Summary table composed after the
  line items (subtotal / tax / TOTAL, the last row emphasized) — now ships as
  `invoice.presets.ClassicInvoice` on the layered stack, with the same
  `create()` / `create(BrandTheme)` contract as `ModernInvoice`, porting the rendered
  layout of the published standalone `invoice-classic` template. Guarded by a smoke
  test, exact layout snapshots (the canonical single page plus a forty-line-item
  overflow that freezes the two-page table continuation), and the invoice pixel-parity
  gate; the examples showcase gains `invoice-classic-v2`.

- **The first structured proposal preset: `NorthlineProposal`.** A two-page
  teal-and-navy business proposal on the Spectral/Lato pair — brand header with logo
  mark and wordmark, three stacked title lines on the reference's own pitch, the
  executive summary beside an at-a-glance fact card, icon goal cells, a numbered
  scope list, deliverable columns, the phase grid, an investment table with subtotal /
  optional / total row styling, and a signing card — with the navy footer band, the
  teal page-number block and the `01`/`02` page numbers as page chrome rather than
  flow content. Ships as `proposal.presets.NorthlineProposal` consuming the new
  structured proposal model, with its icon set packaged in the templates artifact,
  porting the rendered layout of the published standalone `northline-proposal`
  template. Guarded by a smoke test (including the empty document and the
  phase-grid header-count contract), an exact two-page layout snapshot, and a
  pixel-parity gate; the examples showcase gains `proposal-northline-v2`.

- **Monogram Sidebar draws the employer.** Its experience entries rendered the position,
  the date and the description, and never `CvEntry.subtitle()` — so every company name
  was missing from the rendered CV while the education block, which does render its
  subtitle, looked complete. The employer is now drawn between the position and the date,
  in the shared theme entry-subtitle style.

### Tests

- **The sidebar CV samples are held to the width of the column they are drawn in.**
  A contact channel in `ProfessionalSidebar` and `NavySidebar` is one paragraph — the
  mark and the value share a line — so a value wider than the sidebar's text column
  wraps and leaves the mark alone on the first line. The promotion gates could not see
  it: they measure the published template's own fixture, whose addresses fit, while the
  example sample data is written for the repository and can outgrow the column with
  every test still green. It shipped that way once, spotted in the rendered preview
  rather than by a build. `SidebarContactRowsFitTest` now measures the shape of the row
  instead of the length of the string — a single-line channel is as tall as its mark,
  a wrapped one close to twice that — and the two sample addresses were shortened to
  fit.

### Documentation

- **The presets that cap content say so.** `MonogramSidebar`, `SidebarPortrait` and
  `MintEditorial` compose a fixed amount of content: entries past a per-block cap are not
  rendered, do not move to a continuation page, and are reported nowhere. Each preset's
  class documentation now names its caps, explains that they are load-bearing — the
  columns are one atomic `addRow`, so an uncapped block raises
  `AtomicNodeTooLargeException` instead of spilling onto a second page — and points at
  `TimelineMinimal`, which splits its own columns. `docs/templates/v2-layered/using-templates.md`
  carries the same table under *Picking a preset*. The caps themselves are unchanged.


## v2.2.2 — 2026-08-27

### Public API

- **The layout snapshot can now say what the text became.** It could already say that a
  block moved; it could never say why the block is the size it is, because the thing that
  decides that — which font, at what size, broken into how many lines — was measured
  during layout and then discarded. A wrong font and a wrong padding produce the same
  symptom on the page, and telling them apart by eye is exactly the guessing a measured
  snapshot exists to end.

  **It is opt-in, and `LayoutSnapshot` did not change shape.** A diagnostic section that
  appeared on its own would turn every consumer's snapshot suite red on an upgrade that
  moved nothing in their document:

  ```java
  LayoutSnapshot plain = document.layoutSnapshot();               // exactly as before

  LayoutDiagnosticSnapshot rich = document.layoutSnapshot(
          LayoutSnapshotOptions.builder().typography(true).build());

  rich.layout().equals(plain);                                    // true
  ```

  The diagnostics live on a new `LayoutDiagnosticSnapshot` that *wraps* the layout
  snapshot rather than on `LayoutSnapshot` itself. That distinction is the guarantee:
  `LayoutSnapshot` still has exactly the four components it had in 2.0, so its JSON, its
  `toString()` and its `equals` are unchanged however you serialize it — through
  `LayoutSnapshotJson`, through an `ObjectMapper` of your own, or by hand. Every committed
  baseline in this repo is unchanged, and nothing added here can reach one of yours.

  `LayoutDiagnosticSnapshot.formatVersion` versions the envelope independently of the
  layout snapshot's `2.0`, so a section added later moves one number and not the other.
  `LayoutSnapshotOptions` is a builder rather than an overload so that next section costs
  a method rather than a new `layoutSnapshot(...)` signature.

  `LayoutDiagnosticSnapshot.typography()` is a list of `LayoutTypographySnapshot`, one
  entry per resolved paragraph fragment: the declared font, the resolved family, the
  decoration, the size, the line count, the bounds of the laid-out line boxes, and a
  `LayoutTextLineSnapshot` per line carrying its own bounds and baseline in absolute page
  coordinates.

  It hangs off fragments rather than nodes because that is what text is — a paragraph
  broken across a page boundary has one fragment per page, each with its own lines, and a
  per-node projection would have to keep one and discard the other. Join it to
  `layout().nodes()` on `path`, one-to-many. Entries are ordered by path, then page, then
  emission ordinal:
  a split paragraph restarts its ordinal at zero on each page, so page has to be in the
  key or the order falls back to whatever order pagination emitted fragments in.

  **`declaredFont`, `resolvedFamily` and `decoration` are three fields because the face
  needs all three.** A standard-14 face such as `HELVETICA_BOLD` is an alias of its family
  and contributes nothing on its own — the face comes from the decoration — so a style
  that names the bold face and sets no decoration renders regular, silently.
  `fontSubstituted` reports that, and *only* that: naming the bold face **and** asking for
  bold draws exactly what it named and is not flagged. Reporting the family alone could
  not tell those two apart, nor `Helvetica + DEFAULT` from `Helvetica + BOLD`. The family
  rule is reachable as `FontLibrary.resolveFamily(FontName)`, pure and silent so a
  diagnostic pass emits no warnings of its own. A font that is neither registered nor
  aliased never reaches the snapshot: measurement fails first, loudly.

  `resolvedFamily`, `decoration` and `fontSize` describe the text the engine actually
  measured — after an `autoSize` shrink, and after a span-level override — so the reported
  size always matches the line boxes beside it. `declaredFont` stays what the paragraph
  asked for.

  **The limits, stated rather than implied.** A paragraph using a non-default
  `TextVerticalAlign` has its glyphs shifted by a correction read from the backend font's
  cap height, which nothing renderer-neutral can compute; those lines carry
  `baselineExact = false`, and because the shift moves the glyphs and not the line box the
  whole entry is positional there rather than a bound on painted output. The bounds are
  laid-out line boxes, not tight glyph ink — a code chip's fill extends past them by its
  own padding. Text drawn outside the paragraph pipeline, such as a table cell written as
  a plain string, produces no entry, so an empty list means "no paragraph text" rather
  than "no text". Coordinates are the laid-out ones, so a transformed or clipped container
  is not reflected. A paragraph whose spans mix fonts is described by its first span.

  The line's own text is deliberately not included. A snapshot excludes raw text payload,
  the words are already in the document that produced it, and a line is identified by its
  index within the fragment.

  The vertical line walk moved into `ParagraphLineGeometry` (`contentTop`, `nextLineTop`,
  `baselineY`) and the PDF handler now draws through it, so the snapshot and the page
  cannot describe different lines. That helper already existed for the horizontal half,
  for exactly this reason.

  No rendered output changed, and no layout, pagination or render behaviour changed.


## v2.2.1 — 2026-08-25

### Public API

- **The SVG surface graduates from `@Beta` to Stable.** `SvgPath`, `SvgIcon`,
  `PathBuilder.svg(svgPath)`, both `addSvgIcon(...)` flow adders,
  `ShapeContainerBuilder.path(w, h, svgPath)`, and the gradient paints the
  reader emits (`DocumentPaint.LinearAxis` / `RadialCircle`) drop the
  annotation and join the Stable tier — additive-only changes in minors from
  here on. The surface shipped in 1.8.0 marked `@Beta` "while it hardens
  against real-world exporter output"; that hardening is done — the stroke,
  colour and unit work, per-element error context, the clip-path support
  added in 1.9.0, and the opacity-family + warning pass in this release —
  and the API shape itself has not moved since 1.9.0, four minors of real
  use. The annotation drop also closes an inconsistency: `inlineSvgIcon` /
  `RichText.svgIcon` and the emoji pipeline were built on `@Beta` `SvgIcon`
  without carrying the marker themselves; now no SVG entry point does. No
  binary or source change for callers — the remaining `@Beta` carriers are
  the `NodeDefinition` Extension SPI seam and the PPTX backend, exactly as
  [docs/api-stability.md](docs/api-stability.md) lists them.

### Fixed

- **A PDF now carries the words it draws.** Text set in a bundled TrueType family lost
  letters from its text layer: `Platform` extracted as `Pla orm`, `certification` as
  `cer fica on`. The page looked right, so nothing showed it — but the text layer is what
  a search box, a copy-and-paste, a screen reader and an applicant tracking system all
  read, so a CV rendered through one of these families quietly failed to contain the
  words printed on it.

  PDFBox applies a font's `GSUB` substitutions itself whenever a face carrying them is
  made current on a content stream, and most of the bundled families define ligatures
  over the commonest English letter pairs — `ti`, `tf`, `ft`. Each pair was drawn as a
  single glyph, and the map that says what a glyph stands for is built by reading the
  font's character map backwards, where a ligature is reachable from no character at all.
  The entry was therefore absent and both letters were lost. The families whose ligatures
  happen to have code points of their own (`fi`, `fl`) survived, which is why the damage
  looked arbitrary.

  A Latin face is now handed to PDFBox with nothing to substitute. That is also what the
  engine already assumed: layout measures a string ligature-blind, so a line drawn with
  ligatures was slightly narrower than the box measured for it, and the DOCX and PPTX
  backends never substituted. Non-Latin faces are untouched — PDFBox shapes Devanagari,
  Bengali and Gujarati through the same mechanism, and there the substitutions are how
  the script renders rather than a flourish on top of it.

  Visible consequence: text set in a bundled family no longer forms ligatures, so `fi`
  and `fl` are drawn as two letters. PDFBox applies `ccmp`, `liga` and `clig` together
  and offers no way to keep one without the others, but in the bundled families the
  Latin `ccmp` changes nothing — decomposed combining sequences and precomposed letters
  are drawn exactly as before. The committed visual baselines for the layered CV and
  cover-letter presets moved by the ligatures alone and were re-recorded.

- **A composite node inside a composed table cell renders its children.**
  `DocumentTableCell.node(...)` holding a `SectionNode`, `ContainerNode`,
  `RowNode` or `LayerStackNode` measured the child, reserved its full height,
  and then drew nothing inside it — a correctly-sized blank hole in the table.
  A composite leaves its children to the compiler and emits only its own
  decoration from `emitFragments`, so dispatching a composed cell straight at
  the child's `emitFragments` picked up the section background and dropped
  every paragraph under it. The cell now lays the child's whole sub-tree out
  inside the cell box, with the same column / row / stack layout the sub-tree
  gets anywhere else on the page. Leaf children (paragraph, list) and nested
  tables already worked and are unchanged. The row stays atomic: a composed
  cell still does not split across a page break.

- **A row nested in a fixed rectangle keeps its horizontal band.** A `RowNode`
  inside a `LayerStackNode` layer — allowed since 1.6.2 — stacked its children
  downwards instead of seating them side by side, and because the band was
  measured as one row tall, every child after the first spilled out of the
  layer. The fixed-rectangle walk had no horizontal branch at all: it is a
  vertical y-cursor, right for a section or a container and wrong for a row.
  It now resolves slot widths through the same `RowSlots` path the page-level
  row band uses, so a nested row honours weights, fixed columns, flex
  arrangement and vertical alignment identically. Vertical composites in a
  fixed rectangle are unchanged. Found while composing a row into a table
  cell, which is the second rectangle this walk fills.

  **Behaviour note:** a row nested *directly inside another row* in a fixed
  rectangle now raises the same `IllegalStateException` the page-level row
  band has always raised (`"cannot contain a nested horizontal row"`, which
  names the fix: wrap the inner row in its own layer). It previously
  produced a layout instead — but not a usable one: with a two-child inner
  row inside a two-child outer row in a layer, two of the three leaves
  landed on the same point, 17pt below the layer's own bottom edge. Wrapping
  the inner row in its own `LayerStackNode` layer lays it out correctly.

- **The SVG reader honours the opacity family.** `opacity`, `fill-opacity` and
  `stroke-opacity` — attribute or `style=""`, number or percentage, with SVG's
  inheritance for the paint slots and composition for group `opacity` — now
  multiply into each layer's flat paint alpha, on top of any alpha the colour
  already carries from an 8-digit hex or `rgba()`. They were read from nowhere
  before: a translucent logo landed on the page fully opaque, and the only fix
  was editing the SVG. A slot whose product reaches zero is treated like
  `fill="none"`, so an `opacity="0"` guide layer no longer paints at all. A
  *partial* opacity cannot reach a gradient slot — the `DocumentPaint` contract
  refuses translucent stops because shadings carry no alpha — so a gradient
  under `fill-opacity="0.5"` still paints opaque, now with a one-line warning
  naming the approximation. Group opacity is the per-layer approximation of SVG's
  offscreen compositing — overlapping siblings inside one translucent group
  darken where a browser would flatten them first — which is the same trade
  every lightweight icon reader makes.

- **What the SVG reader cannot honour now says so.** `fill-rule="evenodd"` was
  read from nowhere and filled with non-zero winding — a donut whose hole is cut
  by a same-direction subpath came out solid, silently; it still renders the
  same way, but the icon now logs one warning naming the approximation (a
  `fill-rule` value outside nonzero / evenodd / inherit, previously unread, is
  now refused like any other bad presentation value). A `mask="url(#id)"` or
  `filter="url(#id)"` attribute — whose definition sits in `<defs>`, where the
  walk never looks — paints unmasked / unfiltered as before, but the
  referencing attribute is now read and warned about, because that attribute is
  the only place the divergence is observable. Element kinds outside the
  reader's vocabulary (`mask`, `pattern`, `marker`, `<style>` CSS, `<a>`
  wrappers…) joined the skip tally that previously only counted
  text / image / `use` — one warning per kind, and a blank icon's "no drawable
  geometry" error names them. The DOCX export gained the inline mirror of its
  block-level drop warning: image / shape / SVG runs (emoji included) vanish
  from a paragraph by contract, and now say so once per export instead of only
  the block path warning.

- **SVG reader errors keep their house style at the edges.** A malformed hex
  colour (`#zzz`), a non-numeric `rgb()` channel or `rgba()` alpha, and a
  unit-only length (`stroke-width="px"`) leaked the raw
  `NumberFormatException` ("For input string: …") past the reader's
  name-the-field-and-the-element convention; each now reports what was being
  parsed and the offending input, with the JDK detail chained as the cause.

### Documentation

- **`DocumentTableCell.text("a\nb")` is one line, and now says so.** The
  advanced-tables recipe demonstrated a multi-line cell by putting `\n` inside
  `text(...)`, which renders as a single line — the newline is whitespace
  between two words there. The recipe and the `DocumentTableCell` Javadoc now
  name the three cell shapes explicitly: `text(...)` for one line,
  `lines(...)` for several, `node(...)` for any registered node (and
  `ParagraphNode` *does* honour `\n` as a hard break, inside a cell as
  anywhere else). The two examples that showed the misleading form were
  switched to `lines(...)`, and the composed-cell showcase gained a section
  and a row inside table cells.

- **The SVG Javadoc stopped describing a younger reader.** `SvgGradients` claimed
  focal radials and `stop-opacity` are "loudly refused" — both degrade
  deliberately (centred-radial approximation, opaque stops, alpha-only overlay
  layers dropped) and the class doc now says what actually happens. `SvgIcon`
  still listed clip paths as out of scope and the skip-tally warning said "no
  clips" — `clip-path:url(#id)` has rendered since 1.9.0, so the supported list
  gained it (with its innermost-wins nesting rule) and the out-of-scope list,
  the warning, and the empty-icon error text shrank to what is actually
  dropped.

### Build

- **The weekly benchmark run builds the modules it measures.** The JMH workflow
  installed the engine from source and then let Maven resolve the rest from Central,
  and one of them is not there to resolve: the benchmarks read their document fixtures
  out of the `tests`-classifier jar of `graph-compose-templates`, which the templates
  release profile unbinds precisely so it is never published. The job died at
  dependency resolution before a single benchmark ran: five of the six weekly runs
  since the 2.0 module split reached the default branch failed that way, on four
  different versions, each looking for the jar under the version the release before it
  had just published. It now installs `render-pdf` and `templates` from source the way
  the two benchmark jobs in the main pipeline already do.

  `BenchmarkDependencyInstallGuardTest` reads every workflow and fails when a job
  builds `benchmarks/pom.xml` without first installing each first-party dependency the
  benchmarks pom declares. It takes that list from the pom rather than a copy of it, so
  a new sibling dependency is guarded the day it is added, and it runs in the guard job
  on every pull request — which is what the workflow itself cannot do, running only
  from the default branch on a schedule.

## v2.2.0 — 2026-08-15

### Public API

- **A paragraph can say which way it runs.** `ParagraphBuilder.direction(...)` takes
  `TextDirection.LTR`, `RTL`, or `AUTO`, which reads the direction off the first strong
  character. Hebrew and Arabic were previously laid out and drawn in logical order — the
  order text is read in, not the order a page draws it — so every line came out reversed
  in a document that otherwise looked finished.

  Direction is a separate choice from `TextAlign`: alignment says where a line sits,
  direction says which way it runs. They meet in one place, so a right-to-left paragraph
  aligns right unless the caller chose an alignment of their own.

  Lines are resolved with the Unicode Bidirectional Algorithm, so a Latin word or a
  number embedded in Hebrew keeps running forwards, and the paragraph direction only
  decides what it is embedded in. A line with no right-to-left character resolves to
  itself without the algorithm running at all, so existing documents take the path they
  always took — held to that by the layout snapshots and visual baselines, none of which
  moved.

  A paragraph is the unit this applies to; the sibling entry below carries it into a
  table cell.

  All three wrap paths carry it: plain text, inline runs (what templates author
  through), and markdown. Each backend does what it must and no more — the PDF backend
  reverses a right-to-left run, because a PDF draws characters in the order it is given
  them; PowerPoint and Word have their own bidirectional engines, so the text reaches
  them in logical order rather than rewritten. Word is told the paragraph's base
  direction with `w:bidi`, which is the only way it can lay out a line that opens on a
  neutral character; PowerPoint is told the same thing per frame, because pinning a span
  where the page put it settles the order *across* the line but not which side a neutral
  falls on *inside* a frame.

  The bidirectional formatting characters (`U+200E`, `U+200F`, `U+061C` and the
  embeddings and isolates) now survive control-character sanitizing until the algorithm
  has read them. They are what an author uses to steer a neutral stretch of text, and
  removing them with the rest of Unicode category C deleted the instruction before
  anything could act on it. They draw nothing, so they are dropped again at the seam
  that measures and draws — where substituting them with `?` would have put a visible
  mark on the page and given a zero-width character a width.

  One shaping limit is worth knowing. Letters are given their contextual forms before
  the line is wrapped, because wrapping measures widths and the forms are what carry
  them. A word longer than the column is therefore broken with the forms it was given
  while whole: the letters either side of the break keep their connecting strokes, as if
  the word continued across the line boundary. Arabic does not break words, so this only
  arises where the word cannot fit at all — the case every script degrades in — and
  re-shaping the halves would change their widths, which is what the wrap already spent.

  One limit was worth knowing here, and this release closes it further down this page:
  the content stream carries the visual order, and every reordered run now states its
  text as written in an `ActualText` marked-content section — see the entry below. The
  DOCX export was never affected, since Word receives logical text.

- **A table cell can say which way it runs.** `DocumentTableStyle.direction(...)` takes the
  same `TextDirection.LTR`, `RTL` or `AUTO` a paragraph does, and inherits down the cascade
  a cell style already follows — the table's default, then the column's, then the row's,
  then the cell's own — so one call turns a whole table round.

  A cell written as a plain string reached the page through the table's own layout rather
  than the text pipeline, and so received neither of the two things that make Hebrew and
  Arabic correct: the same string drew reversed in a cell while drawing properly in a
  paragraph, and Arabic came out as isolated letters instead of joined. Both now happen on
  the cell path, and a column is measured on the joined forms, so an auto column is sized
  to the text the page actually draws rather than to a wider form that never appears.

  The cell is the unit `AUTO` reads. Two cells side by side under one declared direction
  answer it separately, and a cell's second line does not run the other way from its first
  because it happens to open on Latin. Direction decides the edge only when nobody asked
  for one — a right-to-left cell sits at its right edge unless it carries a `textAnchor` of
  its own — which is the rule a paragraph already follows for alignment.

  Each backend does what it must and no more. The PDF is painted, so the engine shapes and
  reorders the line itself and marks it with the text as written, which is what a reader
  copies out. Word is told the direction twice — `w:bidi` on the cell's paragraph and
  `w:rtl` on its runs — and receives the text untouched, because it reorders and joins on
  its own. PowerPoint is told the direction on the cell's frame and likewise receives the
  text as written.

  The Unicode formatting controls reach whoever still has to read them. A cell's line keeps
  its joining controls and direction marks through the backend's own sanitising, because
  below that sit the shaper and the algorithm — and in a cell handed to PowerPoint neither
  has run yet. They are dropped at the glyph seam, where a zero-width character has nothing
  to draw. Removed earlier, with a space in their place, a `ZWNJ` between two Arabic letters
  did not merely go missing: it became a word break, and the letters the author had
  separated joined up anyway.

  That last one is the opposite of what a paragraph does, and the difference is the size of
  what each hands over. A paragraph reaches PowerPoint as one frame per span, so no frame
  holds a bracket pair for PowerPoint to resolve and the mirroring has to be done first. A
  cell is one frame holding a whole line, which is the input PowerPoint's own algorithm is
  complete for — mirroring it first swaps the brackets a second time, and `(2026)` closing
  an Arabic cell was drawn as `)2026(` until this stopped. The upside is that a copy out of
  a cell carries the brackets as typed, which the paragraph path cannot promise.

  A left-to-right cell is untouched, so a table of Latin content keeps the geometry and the
  export it always had. A cell holding Hebrew or Arabic is not, and deliberately: what a
  declaration settles is the direction a line is *embedded* in, while a script runs the way
  it runs inside that whatever the base. So a cell that declares nothing is now shaped and
  ordered too, and its auto column measured on the joined forms. Such a table moves —
  because what it drew before was the word backwards.

- **Arabic joins.** Arabic letters change shape by position, and a font does that
  through OpenType `GSUB` — which a PDF never executes: `showText` walks the font's
  `cmap` and nothing else. The engine now shapes Arabic itself, mapping each letter to
  its contextual presentation form (and lam-alef to its ligature) before measurement,
  so what is measured is what is drawn. Vowel points and direction marks are
  transparent to the join, as Unicode's joining rules say. PowerPoint gets the base
  letters back — it shapes Arabic itself, and frozen forms would end up in a file
  users search and copy from — and Word was never given forms to begin with. The
  joining controls travel with them: they are the author's instruction about which
  letters may connect, and PowerPoint's shaper is the reader they were written for, so
  dropping them would have handed it a word it joins straight back up. A font
  that carries the letters but not the forms (the `GSUB`-only families) now degrades
  to unjoined base letters instead of `?`, which costs the joining rather than the
  text. An annotation mark between two letters no longer breaks their join: which
  characters are transparent to shaping is decided by Unicode's own rule — general
  category — rather than by a list of ranges that covered the vowel points and missed
  the rest. In right-to-left runs, paired punctuation is mirrored at the PDF seam
  (UAX #9 L4), so a parenthesis in Hebrew faces what it encloses. The mirrored set is
  the punctuation that occurs in documents — parentheses, brackets, braces, angle
  brackets, guillemets — rather than the whole Unicode mirroring table, so a relational
  or set operator such as `≤` or `⊂` passes through drawn as written. (The angle brackets
  in that list are `<` and `>`, which Unicode also classes as mathematical, so they do
  mirror.)

- **A PDF now says the letters an author wrote, not the shapes they were drawn as.** A
  font's `ToUnicode` map states what each glyph in the file means, and the subsetter
  builds it from the characters shown — which, for Arabic, are the joined forms. So the
  file stated that a glyph meant U+FE8E, the final form of alef, where the author had
  typed U+0627. A reader that applies a compatibility normalisation recovered the letter,
  which is why extraction usually looked right; one that handed the code point straight to
  a search box did not, and a search for the ordinary spelling of a word found nothing
  with no sign of why. The maps are now corrected once the subsetter has built them, so a
  copied word is the word, and the lam-alef ligature — one glyph standing for two letters
  — comes back out as both. Hebrew is untouched: it is reordered, never shaped, so its
  glyphs already named their own letters.

  Only a document that drew a reordered run pays anything. The map has to exist before it
  can be read and it is written during the save, so such a document is saved twice — the
  first time into a null sink, so both saves stream and nothing is buffered regardless of
  document size. Everything else takes exactly the path it took before, saving once.

  Password protection is applied *between* the two saves rather than before them, because
  encrypting is part of saving and writes ciphertext back into the streams it encrypted —
  a map built by a protected first save would be unreadable, and the correction would
  silently find nothing. The protected-document test reads the decrypted map itself, since
  opening and extracting were both true even while the correction was being skipped.

  And each reordered run now states its own text in the file. The glyphs of a
  right-to-left run go out backwards — that is what drawing one means — and every reader
  was left to work the letters back out for itself. The run is now wrapped in an
  `ActualText` marked-content section carrying its text as written (for Arabic, the
  letters rather than the joined forms), so a reader that honours the section gets the
  words with no algorithm at all, and one that ignores it loses nothing it ever had.

  The section is one run, deliberately not the line. A reader takes `ActualText`
  *instead of* the glyphs it covers, so a section spanning the line would swallow the
  left-to-right words inside it — measured before this shape was chosen: wrapping the
  line made PDFBox return the embedded Latin word reversed and dropped the highlight
  chip's glyph positions from extraction entirely. Wrapped run by run, mixed lines
  extract exactly as they did before the sections existed, which a regression test now
  pins. The order of runs across a line stays a bidirectional question either way; the
  letters inside each run no longer are. A document with no reordered run emits no
  marked content at all.

  Two costs, stated plainly. Each section registers a property-list entry in the page's
  resources — roughly a tenth of an Arabic page's size in bookkeeping; writing the
  dictionary inline would reclaim it and needs an operator PDFBox's content-stream API
  does not expose. And a reader that honours `ActualText` takes it instead of the glyphs
  it covers, so per-glyph text inside a reordered run is no longer available through
  plain extraction — an extractor reports the run's whole text against its first glyph.
  The character each glyph stands for is still in the file, in the font's own map, which
  is where anything asking where a particular word was drawn now has to look; the
  engine's own tests read it there.

- **A chip that opens on Latin still draws its Hebrew the way it reads.** A chip takes
  its direction from its first character, and that character settles only where the chip
  sits in the line — not what the chip holds. One opening on Latin is a left-to-right
  run that may still carry Hebrew or Arabic, and the PDF skipped the bidirectional
  resolution for it entirely, handing the content stream logical order: drawn left to
  right, the word came out backwards.

  The direction is now the *base* the resolution runs against rather than the question
  of whether to run it, so a chip is resolved whenever its text needs it. A chip that is
  wholly right-to-left is unchanged, and so is one holding no such script at all.

  The slide backend has the same gap for the same reason, and it shows in the
  punctuation rather than the letters: PowerPoint orders the Hebrew itself, but a
  neutral standing between two right-to-left words takes their level even under a
  left-to-right base, and PowerPoint does not mirror what it places. A chip reading
  `a בית > ספר` now reaches the slide as `a בית < ספר`, so the comparison faces the way
  the line reads; a bracket enclosing Hebrew swaps for the same reason, while everything
  the left-to-right base owns is left as typed.

- **A deck carries the bundled fonts it drew with.** A family a caller registers has
  always been embedded; a family this library *ships* — Amiri for Arabic, David Libre for
  Hebrew, the Noto faces for Georgian and Armenian, Gothic A1 for Hangul — was only warned
  about. The deck named it and embedded nothing, so a viewer without the font installed
  substituted, and for a script the substitute does not cover the slide showed boxes. The
  shipped families are exactly the ones a deck reaches for when its reader is least likely
  to have the font, which is what made the asymmetry sharp: register your own Arabic font
  and it travelled, use the one shipped for exactly that purpose and it did not.

  Only what was drawn travels, down to the face: the bundled set is dozens of families,
  and a face nobody drew is pure weight — embedding carries a font program whole, so the
  five-script catalogue was shipping Gothic A1 Bold at 2.2 MB for glyphs no slide
  contains. Measured on the shipped examples, the Arabic article goes from 29 KB to
  235 KB and the catalogue from 27 KB to 1.4 MB.
  `PptxFixedLayoutBackend.Builder.embedBundledFonts(false)` declines it for a deck whose
  readers are known to have the fonts.

  A family the deck carries is no longer reported as one the reader must install. The
  substitution warning fires while a run is drawn and the embedding happens after the last
  one, so a deck that carries Amiri was telling its author to register Amiri — the opposite
  of what shipped. A render told not to carry them still says so, because then the file
  really does only name the family.

  Decks that use no bundled binary family are byte-identical — all seven committed deck
  previews, measured. Layout is untouched either way: a registered family also contributes
  viewer metrics, which participate in placement, and taking those from a bundled family
  now would move text in decks that already render correctly. This changes what the file
  carries, not where anything sits.
- **Word draws a right-to-left paragraph on the side it starts from, at the size it was
  asked for.** Four properties — one on the paragraph, three on its runs — meant something
  different to Word than the way they were written, and all of them showed the moment a
  Hebrew or Arabic document was opened.

  `w:jc` takes `left` and `right` as the *start* and *end* of the text flow rather than as
  edges of the page. The alignment the page resolved — flush right for a right-to-left
  paragraph — was written literally, which told Word to align to the flow's end and drew
  the text flush left. Alignment is now mapped through the paragraph's direction, so the
  value written is the one that means what the page decided.

  Hebrew and Arabic are complex scripts, and Word takes their size and weight from
  `w:szCs`, `w:bCs` and `w:iCs`; the Latin twins do not reach them. None of the three was
  written, and the export ships no `styles.xml` to fall back on, so a 15pt Hebrew
  paragraph was drawn at Word's own default while Latin in the same run obeyed `w:sz`.
  Each is now written alongside its Latin twin.

  The same two properties now reach a paragraph **inside a table cell**, which the cell
  walk had been writing without them: it wrote the runs and skipped the alignment and
  direction the identical paragraph gets outside a table. That is where an invoice keeps
  its line items, so every right-to-left cell in one was left undeclared.

  A left-to-right document is drawn the way it always was: the alignment mapping is the
  identity there, and Latin always obeyed the properties that were being written, which is
  why none of this surfaced until a document had Hebrew in it. Its **bytes** do move, in
  two ways worth knowing before re-baselining a committed `.docx`. A paragraph in a table
  cell now carries `w:jc`, where before it carried none — so a right-aligned amount in a
  line item draws flush right instead of flush left, which is what it asked for and did
  not get. And every run now carries the complex-script twins beside the Latin ones; Word
  picks between them per character, so for Latin they are inert, but they are in the file.

  While the size path was being touched: a run's size is now written as the half-points
  `w:sz` and `w:szCs` actually count, rather than rounded to whole points first. A 9.5pt
  label — the timeline builder writes two — was reaching Word as 10pt.
- **A right-to-left slide's punctuation faces the way the line reads.** A line that gets
  reordered reaches PowerPoint as one frame per span, each pinned where the layout put it,
  so the order *across* the line is settled before PowerPoint sees it. What was not
  settled is what happens inside a frame: the text handed over is logical, the frame
  declared no base direction, and a frame holding a lone bracket had nothing to resolve
  against — so a parenthesis closing a right-to-left line was drawn facing the way it was
  typed rather than the way the line reads, while the same document as a PDF was correct.

  Each frame carrying right-to-left text now declares it, which settles *placement*: the
  em-dash of a mixed line moved to the side it belongs on the moment that was written.
  That is necessary and not sufficient. Measured on a slide, PowerPoint places a neutral
  from the declared direction but does not go on to mirror it, so the character is swapped
  before it is handed over, at the same seam the PDF backend swaps it.

  The cost is worth naming: a copy out of the slide carries the mirrored character rather
  than the typed one. The swapped set is document punctuation and includes `<` and `>`, so
  a comparison written between two Hebrew or Arabic words copies out reversed. A `>`
  surrounded by Latin does not, because the wrapper gives that stretch a left-to-right span
  of its own and the swap is keyed to the span's direction. Left-to-right frames are
  untouched.

  This rests on PowerPoint not applying UAX #9 L4 itself, which is measured rather than
  specified. A viewer that does apply it mirrors the character a second time and draws the
  original bug; the assumption is recorded in the backend capability matrix.

- **A chip that mixes directions is drawn the way each of its parts reads — in both
  backends.** A chip is one rounded fill, so the wrapper cannot split it at a level
  boundary the way it splits plain text; it reaches the renderer whole, carrying its
  first character's level. The PDF backend reversed and mirrored it whole, and that
  inverted meaning, not just shape: a chip reading `(a > b)` after a Hebrew word drew as
  `(b < a)` — operands swapped, comparison flipped — while the chip's interior is
  left-to-right text that UAX #9 neither reorders nor mirrors.

  The engine now resolves the chip's own embedding levels (`BidiVisualOrder`), and each
  backend takes from that resolution exactly what its viewer lacks. The PDF draws
  characters in the order it is given them, so it gets the full visual transform —
  runs reordered, right-to-left ones reversed and mirrored; for a single-level chip that
  is exactly the old reverse-and-mirror, so a wholly-Hebrew chip is unchanged, and a
  chip holding `שנה 2026` no longer draws its year backwards. The slide backend keeps
  the chip's text **logical** and its frame's direction declared, because PowerPoint
  reorders strong right-to-left characters by what they are, not by what the frame says
  — a pre-reordered string would come back with its Hebrew re-reversed. What PowerPoint
  was measured not to do is the mirroring, so pairs are swapped for it — but only on the
  levels UAX #9 mirrors, which is what keeps the interior's `>` a `>`.

### Fonts

- **Bundled families for Arabic and Hebrew.** `FontName.AMIRI` and
  `FontName.DAVID_LIBRE` join the bundled catalog, shipping in
  `graph-compose-fonts` **1.1.0** (the font artifact keeps its own version line).
  Both scripts previously rendered as `?` unless you registered a font of your own.
  Amiri was picked for a property that a later release needs: a PDF draws text through
  the font's `cmap` without executing OpenType `GSUB`, so contextual Arabic letter
  forms are only ever reachable when the font itself carries the Arabic presentation
  forms — and popular families that shape purely through `GSUB`, Scheherazade New and
  Rubik among them, carry none. `ArabicHebrewFontCoverageTest` holds both families to
  the ranges they were chosen for, so swapping in a `GSUB`-only family fails loudly.
  David Libre ships no italic upstream; italic styles resolve to its regular face and
  bold-italic to its bold one. No bundled family covers both scripts, so a run mixing
  Arabic and Hebrew still needs a font registered through `FontFamilyDefinition`.
  Text is still laid out in logical order — this release makes the glyphs available,
  not the bidirectional ordering or Arabic joining that use them.
- **Bundled families for Georgian and Armenian.** `FontName.NOTO_SANS_GEORGIAN` and
  `FontName.NOTO_SANS_ARMENIAN` ship in the same **1.1.0** font artifact. Both scripts
  rendered as `?` before, and both are covered in full: Armenian in both cases, Georgian
  in Mkhedruli *and* Mtavruli — the capitals headings are set in, which Unicode encodes in
  a block of its own, so a family carrying only the lowercase range sets body text and
  loses every title. Upstream publishes them as variable fonts with no static weights to
  take, so the artifact carries the regular instance and the other faces resolve to it:
  bold Georgian renders unemboldened rather than failing, which is asserted rather than
  left to be discovered.

  With Arabic, Hebrew and Korean that makes five scripts covered by a named family. A
  paragraph is drawn in a single family, so a run mixing two scripts needs one family that
  carries both — and for Arabic, Georgian, Armenian and Hangul no bundled family carries
  more than its own, so that means a font of your own registered through
  `FontFamilyDefinition`. (Hebrew is the exception: Tinos and Cousine carry it too.)
  [`fonts/README.md`](fonts/README.md#which-script-needs-which-family) has the table, and
  `BundledScriptCoverageTest` holds it to the binaries.
- **A bundled family for Korean.** `FontName.GOTHIC_A1` ships in the same **1.1.0**
  font artifact, in a drawn regular and bold. It carries all 11 172 precomposed Hangul
  syllables — asserted whole rather than sampled, since a gap in that range loses
  whichever words use it while the rest of the document renders — plus both jamo forms
  and Latin-1, Latin Extended-A, the whole Cyrillic block and the modern Greek alphabet.

  The Latin coverage is why this family rather than a better-known one. A paragraph is
  drawn in a single family, so a Korean sentence holding a European name is drawn
  entirely in the Korean font; the popular alternatives carry ASCII but almost no
  accented Latin, which turns *Müller* into *M?ller* with the Korean around it
  rendering perfectly. Hanja are not covered.

  Chinese and Japanese are still without a bundled family, and not by oversight: the
  official static Noto CJK faces use CFF outlines, which the PDF backend cannot embed
  at all, and the variable ones default to the Thin weight — the weight a renderer
  without variable-font support draws. Register a CJK font of your own through
  `FontFamilyDefinition`.
- **A stale font artifact is now distinguished from a missing one.** Asking for a
  family that a newer `graph-compose-fonts` introduced produced the same message as
  having no font artifact at all — an instruction to add a dependency that was already
  there. The two causes are now told apart, and the stale case names the version the
  consumer actually has.

  The artifact states that version itself: it ships a small descriptor written at build
  time, and the engine reads it. Presence is asked separately, of a face the artifact has
  carried since its first release — the descriptor only ships from 1.1.0, so for the
  releases published before it a missing descriptor means "too old to say", not "not
  here", and those two need opposite advice. The first shape of this carried a map from family folder
  to the release that introduced it, which meant the catalog had to remember the
  artifact's history and every new font needed an entry in a class that otherwise knows
  nothing about which fonts exist. Reading the version off the artifact answers the
  question that actually helps — *what do you have* rather than *what should you have* —
  and needs no maintenance as families are added.

### Templates

- **Timeline Minimal renders the whole CV.** The preset used to drop content three
  ways, none of them visible in the output. Per-module caps kept the first few lines
  of each block and discarded the rest, so a fourth degree or a third employer simply
  was not drawn — on a page that the fixed-height axis left looking only four-fifths
  used. Prose was cut at a character count, ending a summary mid-sentence with an
  ellipsis while the column still had room. And sections were matched to modules by
  title keyword, taking the first hit for each category: a second prose section was
  shadowed by the first, a section whose title matched nothing — a user's own
  "Awards" or "Publications" — was never looked at, and the ones that did match were
  relabelled, so "Projects" printed as EXPERTISE and "Additional Information"
  printed as LANGUAGES.

  Everything the document carries is now rendered. Headings come from the section's
  own title, with the preset's label left only for a module that matched nothing.
  An unmatched paragraph is still set as prose: reaching the main column that way
  no longer sets a summary in the tighter face the bulleted modules use.
  Content past one page continues on the next: the body is a row, and a row is
  atomic — the paginator cannot break inside one — so the preset estimates its
  columns' heights from the column width and font metrics, emits one row per page,
  and lets each finished row overflow naturally. The axis keeps its full height on
  the opening page and follows the content on a continuation page.

- **New `SectionAllocation` for CV presets.** Hands each section out once and
  returns what no module claimed, which is what `SectionLookup.firstMatching` alone
  cannot express. The remaining CV presets still slot by keyword and still discard
  what does not match; they are unchanged here.

### Build

- **CI builds the companion asset artifacts instead of downloading them.** The engine
  pulls `graph-compose-fonts` and `graph-compose-emoji` at test scope, and both carry
  their own version lines, so the tree can legitimately pin a version before it is
  published. Jobs scoped to a single module without `-am` resolved them from Maven
  Central and failed on dependency resolution — before running anything — whenever the
  pinned version was newer than the published one. The guards and binary-compatibility
  jobs now install both from source first, and the three benchmark jobs that already
  did so for fonts do it for emoji too. Whether a pinned version really exists on
  Central is still checked, by the release smoke harness, which resolves every
  coordinate through Central in an isolated repository.

- **The open changelog entry and the development version cannot name different
  releases.** The post-release step opens the next line by incrementing the patch, so a
  GA of `X.Y.Z` always leaves the poms on `X.Y.(Z+1)-SNAPSHOT` — right when the next
  release is a patch, wrong from the first commit when it is a minor. What that costs is
  not tidiness. While the poms and the changelog name different releases, an `@since`
  tag written in between has two answers available, and `@since` is a contract with the
  consumer that outlives the cycle: the last time the two disagreed, tags went out
  against both, and the ones that followed the previous release had to be corrected when
  the line was. `VersionConsistencyGuardTest` now holds the poms to the open entry, which
  is where the next version gets recorded first, so there is one answer to take.

  An entry counts as open because it carries no date, not because of the word after the
  version — the 2.1.0 line was opened as `— in progress`, and a check that recognised
  only one spelling would have watched that whole line go by. For the same reason a `##`
  heading that names no release is reported rather than skipped: leaving the check with
  nothing to compare must not look like agreement. Two open entries fail as well, being
  an ambiguous answer rather than a wrong one.

  The wording is then held to the exact `— Planned` the cut replaces, em dash included,
  since an ASCII hyphen is as invisible to that replacement as another word would be.
  Getting it wrong does not ship an undated entry — the cut stops on the missing date —
  but it stops the release rather than the commit that introduced it, and by then the
  cause is a step away. A version keeps its pre-release qualifier throughout, so a dated
  `-rc.N` entry reads as shipped instead of as a second open one.

  Having no open entry passes: the post-release bump writes none and runs this guard as
  its own gate, so demanding one would fail the commit that opens the window. The check
  begins with the cycle's first entry. It compares the two recorded answers against each
  other, so it catches one being corrected without the other — not a pair that was wrong
  together from the start.

### Documentation

- **Three examples that render right-to-left documents rather than describing them.** An
  Arabic article that runs onto a second page, a Hebrew invoice, and a catalogue of every
  bundled script. The article exists because the interesting failures only appear at
  length: a paragraph wrapping over many lines with each one reordered on its own, and a
  page break landing inside a paragraph that then has to start from the right edge again.
  The invoice exists because that is where right-to-left text meets numbers — nearly every
  line mixes a Hebrew description with a Latin product name, a quantity and a total.

  One thing the examples had to work around is worth knowing before writing one: a list
  carries no direction of its own, so an Arabic list needs `align(RIGHT)` or its bullets
  sit on the wrong side of the text. The invoice is built from rows rather than a table
  because it was written before a cell could declare a direction — a table would carry it
  now, and the layout is what keeps the rows.

  The invoice also shows what isolates are for. A Latin name inside a Hebrew line is
  handled by the algorithm, but the punctuation touching it is neutral — so
  "GraphCompose Ltd." printed its full stop at the far end of the line until the run was
  isolated.

- **The image at the top of README is page one of the Maven Central banner.** It was a page
  of the module-first deck, and the caption beside it pointed at a PDF the release does not
  advertise. Both now name the same document — `MavenBannerPptxExample`, which the release
  publishes as a PDF and as an editable PowerPoint deck, so the picture a reader sees is a
  page of a file they can open. What the release commits keeps its name and its place in
  the cut, so the version stamped into the image still arrives from the version bump.

  The banner gained a page for this release: Amiri, David Libre, the two Noto faces and
  Gothic A1 each set a word, and where that word sits in its card is the direction it
  declared — Hebrew and Arabic to the right, the rest to the left. Nothing on the page is an
  image.

### Tests

- **Right-to-left layout is held against coordinates and against pixels.** Five scenarios —
  Arabic wrapping, Hebrew wrapping, a page break inside a right-to-left flow, a line mixing
  scripts with Latin and digits, and every bundled script on one page — each asserted twice.

  The two catch different things, which was measured rather than assumed: switching the
  right-to-left reversal off leaves *every coordinate identical* (same spans, same widths,
  same positions) and changes only the order the glyphs are painted in, so all five layout
  snapshots still pass while four of the five pixel baselines fail. Coordinates cannot see
  a line drawn backwards. The fifth — every bundled script on one page — guards the font
  catalogue rather than the reordering: its right-to-left lines are too short to trip a
  pixel budget, and the wrapping and mixing scenarios are what guard the reordering.

  That same experiment found the pixel budget was too loose: copied from a page four times
  the area, it absorbed the regression on the shortest scenarios. It scales with the page
  now.

  The published examples are held the same two ways, with one honest difference: their A4
  pages are rasterised differently enough across platforms that a pixel budget tight
  enough to catch a reordering regression on the quietest page would fail an honest render
  on another machine — the two signals overlap, measured. So their pixel baselines catch
  gross breakage and show what a deliberate change looked like, while exactness is carried
  by the byte-level drift guard, which no rasteriser can blur.

- **What a document tells a reader its own text is, is now two questions.** What an
  extractor returns and what the font's glyph map says are not the same thing: the first
  can be right while the second is wrong, because a reader is free to normalise a shaped
  form back to its letter and PDFBox's own extractor does. Both are asserted, for Arabic
  and for Hebrew, so a correction that overreached — rewriting entries that were already
  right — fails on the Hebrew case rather than passing everywhere.

  Two existing assertions moved to a different channel as a result. They read the joined
  forms out of what the file said its glyphs meant, which only worked while that answer
  was the drawing rather than the text; joining is now read from the glyph codes
  themselves, which is where it always was.

- **A hyperlink, an underline, and a highlight chip are each held to the words they were
  written on**, in a line that gets reordered. All three are placed by arithmetic rather
  than by where the glyphs went, and in a left-to-right line the two orders agree, so
  nothing distinguishes a correct implementation from one that walks the logical order.
  Each is checked against that failure: placed by a logical walk, the mark or rectangle
  lands under the other words, and the left-to-right control keeps passing — so the tests
  tell the reordering apart from the arithmetic.

  A first-line indent is held to the edge its paragraph starts from, which is the right
  one: the indent is a text prefix, a left-to-right shape of thinking, and a reader of an
  earlier measurement could reasonably have concluded it was being dropped. It is not —
  the prefix spaces are drawn glyphs sitting at the margin, so asking where a line's first
  glyph is answers where the padding starts rather than the text.

- **Every rendered document is now held against a fresh render.** A third of the example
  catalogue — thirty-two documents, the cover-letter presets and most of the CV gallery
  among them — rendered on every run with nothing comparing the result, so a change to the
  engine moved those documents and no test said so. They are committed as previews now,
  which is what puts them under `CommittedAssetDriftTest`: a PDF is compared by its bytes,
  and a difference names the file that moved.

  The list of deliberately unpublished documents shrinks from thirty-five entries to three,
  each carrying its reason. The emoji gallery stays out on weight: its embedded glyph set
  renders to nearly 4 MB against 1.4 MB for the thirty-two together, so committing it would
  put another copy of that in history on every deliberate re-render. The other two hold
  pixels rasterised at render time, which a CI runner antialiases differently from a
  developer's machine — the measurement `AssetContent` already records — so committing them
  would fail the build for a document nobody changed. Those three renders stay unguarded,
  which is the price of the exceptions rather than an oversight.

### Fixed

- **An inline chip reads the same on a slide as it does on the page.** A chip is one
  rounded fill, so the wrapper cannot split it where its characters change direction — it
  reached PowerPoint as a single frame holding the whole thing. PowerPoint has a
  bidirectional engine of its own and re-resolves whatever string it is handed, and it
  re-resolved this one *without the line around it*: a fragment out of context comes back
  in the order that fragment deserves, so a chip reading `a בית (ספר)` put its Latin in one
  place on a slide and another in the PDF. The chip's **text** is now split into its
  directional runs, one frame each, placed in the order the page places them; the fill
  stays a single shape. A single-level run has nothing left to reorder, so the question
  does not arise rather than being answered.

  The mirroring that goes with it was keyed to the wrong thing. Paired punctuation is
  swapped for PowerPoint because it places a neutral without mirroring it — but only for a
  run that is **uniformly** right-to-left, the one it reverses for itself. A run that mixes
  levels comes out right from the text exactly as typed, and swapping it drew `(a > b)` as
  `)a > b(` inside a Hebrew line. Both halves were measured in PowerPoint, one category of
  content at a time, rather than derived.

- **Word mirrors the brackets in an Arabic line.** A right-to-left paragraph declared its
  direction with `w:bidi` and nothing else. That settles which edge the line starts from;
  it does not settle how Word resolves the characters *inside* a run, which comes from
  `w:rtl`. Without it a run is handled as Latin, and paired punctuation is not mirrored —
  `صدرت في (2026)` was drawn as `صدرت في )2026(` while the same document as a PDF was
  correct. Every right-to-left run now carries `w:rtl`, in a paragraph and in a table cell
  alike; it is the run-level half of the pair `w:szCs` belongs to.

  Hebrew came out right either way, which is how this shipped: the defect needs Arabic,
  where digits following a letter resolve as an Arabic number rather than a European one,
  and only there does Word part company with the algorithm. Measured in Word, one property
  at a time, against a matrix that also ruled out `w:cs` and `w:lang`.
- **A Word export resolves an image once, and says so when it cannot.** `writeImage`
  needed the node's data twice over — the bytes it writes, and the intrinsic size it
  measures the fit against — and fetched it twice, from two places. Resolving is not
  free: the source cache copies the byte array whole and hashes it, so a large image
  paid both costs on every export.

  The second fetch was also reading a different thing. The cache keys on the path alone,
  so once a render had warmed it, a file rewritten underneath gave the export fresh bytes
  from disk and the previous version's dimensions — a picture embedded at another image's
  size, with nothing reporting it, because both halves succeeded and simply described
  different files. The bytes now come from the resolution that sizes the frame.

  One behaviour changes with it: an image whose source cannot be read stops the export.
  It used to disappear from the document silently — not a decision, but the side effect
  of a read that swallowed its own exception — and a document that comes back one picture
  short is the worst of the available answers.

- **DOCX keeps the styling a mixed paragraph asks for.** A `RichText` paragraph exported
  with every run in the paragraph's base style, so a bold segment, an accent-coloured
  segment and plain text all came out identical — a valid `.docx`, no warning, and the
  emphasis simply absent. `InlineTextRun` documents its style as falling back to the
  paragraph's *when null*; the backend was applying that fallback unconditionally, with
  the run in hand. Each run now carries its own style, in a paragraph and in a `row`
  cell, which used to be written from its concatenated text in one style. (A `table`
  cell is written from lines rather than runs and still carries no styling.)

- **`STRIKETHROUGH` reaches Word.** It was the one `DocumentTextDecoration` with no
  branch in the DOCX style mapping and fell through to no decoration at all.

- **DOCX writes a table on the grid its cells occupy.** An authored row is not a row of
  columns: a `rowSpan` covers positions in the rows below and those rows do not repeat the
  covered cells, and a `colSpan` makes the record count differ from the column count. The
  backend read a row's records as its columns and sized the grid from the first row's
  record count, so a `rowSpan` shifted every row beneath it one column to the left, and a
  `colSpan` did that *and* left the grid too narrow, dropping the cells past its end
  without a word. `colSpan` and `rowSpan` now map to Word's `w:gridSpan` and `w:vMerge`, a
  cell takes the most specific text style in the table / column / row / cell cascade, a
  composed cell exports its node instead of the empty `lines()` it has by definition, and
  a multi-line cell is separated by a real break rather than a newline Word reads as a
  space.

  A table whose authored rows cannot form a rectangle now fails the export with the
  position at fault, where before it was drawn wrong. That is the rule the layout pipeline
  already applied, so a document the PDF backend refuses is no longer one DOCX accepts.

- **A DOCX table is painted the way it was styled.** `DocumentTableStyle` carries a fill and
  a stroke, and neither reached the file: a zebra body, a header band and a ruled grid all
  exported on Word's defaults. The fill maps to `w:shd` and the stroke to `w:tcBorders`, and
  the cascade that already resolved a cell's text style now resolves every field on its own,
  so a table-wide rule survives a row that only overrides the fill. A merged cell is painted
  on every position it covers, since a `w:vMerge` continuation draws its own shading and
  would otherwise stripe the region. A stroke of no width — how this codebase says "no
  border", and what a shipped CV preset uses — writes that instruction rather than omitting
  it, so a borderless design no longer inherits the grid Word puts on a table by default.
  What a fill loses is its opacity: `w:shd` is opaque, and blending it needs a background
  Word owns rather than the backend.

  The Word companion example styles its table, so the feature ships with a render behind it
  — and both of its committed previews move, the DOCX for the new markup and the PDF because
  the fixed-layout backend paints the same style it was never given before.

- **A DOCX image is the size it asked for, in the shape it asked for.** The drawn box came
  from the node's literal `width` and `height` and fell back to a hardcoded 100 × 100 pt
  when either was absent, so an image sized only by `scale` — or by one dimension with the
  other implied by its aspect ratio — came out at a size nothing had asked for. The box now
  comes from `NodeDefinitionSupport.resolveImageDimensions`, the rule layout already applies,
  clamp to the page's content width included.

  `fitMode` was not read at all, which left `CONTAIN` and `COVER` behaving as `STRETCH`.
  `CONTAIN` is embedded at its fitted size, which needs no clipping because it is inside the
  box already; `COVER` fills the box and the overflow is cropped out of the picture source,
  centred, since Word has no clip for an inline picture — the same geometry the PPTX backend
  expresses. And the picture type is read from the image's signature instead of every picture
  being declared PNG, which is what a JPEG was announced as.

  The grid itself is resolved by `TableGrid`, extracted from the layout pipeline so both it
  and the backend answer from one implementation. It is `@Internal`: a backend seam, not a
  public promise.

### Documentation

- **The `rich(...)` lambda examples seed the builder they are handed.** The Javadoc on
  `ParagraphBuilder.rich(Consumer<RichText>)` and `AbstractFlowBuilder.addRich(Consumer<RichText>)`
  opened the lambda with `t.text("Status: ")` — but `RichText.text(String)` is a static factory,
  and Java resolves a static call made through an instance reference. The documented line
  compiled, built a separate `RichText`, discarded it, and the paragraph rendered empty — no
  warning, no exception. Both examples now seed with `plain(...)`, the factory's Javadoc spells
  out the trap, and a regression test pins the documented lambda form to a non-empty paragraph.
  The shape-as-container recipe's `RichText.of()` — a factory that never existed — is now
  `RichText.text(...)`.

## v2.1.1 — 2026-08-05

### Build

- **The cut installs a module after the things it needs.** Step 4 installs each train
  sibling on its own, so everything it depends on has to be in the local repository at
  the version the bump just wrote — a version that exists in no reactor and not yet on
  Central. `render-pptx` was listed before `testing` while depending on it, and had been
  since PPTX gained its text-fidelity suite. That stayed invisible: the cut only fails on
  it when the local repository does not already hold `graph-compose-testing` at the new
  version, which is the normal state of a clean machine and not of one that has been
  building all week. The 2.1.1 cut hit it and stopped at Step 4 — after the version bump
  had rewritten thirty files, before any commit, tag or push. `testing` now installs
  second, and `ReleaseScriptInstallListGuardTest` derives the required order from the
  poms rather than restating it, so a new edge cannot be added without failing the build.

- **CI opens the Javadoc jar it is about to publish.** The existing step lints the
  engine's sources, which says nothing about whether the artefact Maven Central serves
  has anything in it — and that was the failure: `graph-compose` carries no sources of
  its own, the javadoc goal found nothing to archive, and every 2.x release shipped an
  artefact with no pages while javadoc.io went on rendering **1.9.1**, the newest version
  that carried a reference at all. Nothing was red for it. The configuration is guarded
  by `PublishedJavadocCoordinateGuardTest`; CI now builds the jar the release profile
  builds and looks inside, failing if the index, `GraphCompose` or `DocumentSession` is
  missing — the three pages a reader arrives at, standing in for the reference.
- **The Javadoc gate lints the class readers open first.** It ran with
  `subpackages` set to `com.demcha.compose.document`, so `GraphCompose` — the entry
  point every snippet in the README starts from — sat in the root package outside it,
  and had done since the module layout moved. It carried a real doclint error the
  whole time: two `<h3>` headings under an implicit `<h1>`, which is the sequence
  break `doclint` exists to catch. Nothing else caught it either, because the
  published Javadoc jar is built with `doclint=none` so a broken tag never blocks a
  release. The gate now covers the whole of `com.demcha.compose`, root package
  included, and the two headings are `<h2>`. Widening it also pulls in the
  `@Internal` engine package: `excludePackageNames` does not take effect alongside
  `subpackages`, and that costs warnings rather than failures, since `failOnError`
  fails on errors only.
- **A CI job the gate guard cannot name is a job it cannot report.** The guard that
  checks every pull-request job is aggregated by `CI Gate` found those jobs with a
  pattern admitting lower case and hyphens — everything today's names happen to use.
  A job added as `build_and_test` or `CodeQL` was not matched, and neither was
  `security_scan: # nightly` or a name with a space after the colon, because the
  pattern also required the line to end there. A job the guard never sees is one it
  cannot report missing: it sits outside the gate's `needs`, the test stays green, and
  the aggregate check branch protection requires is blind to it. Job names are now
  taken structurally, from the YAML's own indentation, rather than from a guess at
  GitHub's identifier grammar, and the parser takes the workflow as text so
  `CiGateCoverageGuardParsingTest` can drive it with the job spellings this
  repository's own workflow does not happen to contain.
- **A recipe nobody links is a recipe nobody reaches.** `docs/recipes.md` is the page
  the README and the documentation index both point at, and nothing held it to the
  folder it indexes: a new page under `docs/recipes/` left every gate green while
  having no inbound link from anywhere. `RecipeCatalogueGuardTest` now fails the build
  in both directions — a page the catalogue omits, and a catalogue row pointing at a
  file that was renamed or removed.
- **A release re-renders the previews it publishes.** The committed previews record
  the version they were rendered at, and until now nothing moved it: a cut bumped
  every pom, regenerated the showcase site at the new version, and left
  `assets/readme/**` on the release before — with the drift gate comparing both
  sides at the old version and staying green through it. The cut now bumps that
  property with the tag and re-renders the previews from the same catalogue the
  site is built from, before the verify step that checks them. `-SkipShowcase`
  skips the published site under `web/` and no longer skips these, since they ship
  in the repository; `-PostReleaseOnly` leaves them alone, because they belong to
  the tag rather than to the branch it opens.
- **A committed preview cannot fall behind the code that renders it.** README and
  the showcase site read files under `assets/readme/**` rather than rendering
  anything, and nothing held those files to the catalogue: a change to an example,
  a theme or the engine moved the render while the committed file stayed put, and
  the first anybody knew was a release publishing it. Twenty-three of the
  sixty-seven were behind and are re-rendered here; the twenty-two PDFs among
  them rasterise to the same pixels as before, so nothing visible had been
  carrying the drift, and the one DOCX now marks bold as `<w:b/>` rather than by
  asking for a font named `Helvetica-Bold`. Every preview is now
  compared against a fresh render on each build, exactly: the comparison drops
  only what a machine writes rather than an author (a PDF's clock-seeded `/ID`, an
  OOXML package's zip and creation stamps, the platform's line separator, and one
  named watermark whose antialiasing differs between machines), all of it measured
  by rendering the catalogue on both platforms rather than assumed. Editing a
  preview by hand now runs the job that checks it.
- **The CI guard job runs every guard it names.** It selected eight test classes
  while scoping the reactor to `graph-compose-core`: two had been deleted months
  earlier and two live in `graph-compose-qa` and `graph-compose-render-pdf`, so
  four never ran. Surefire aborts only when a selection is entirely empty, so the
  four that did match kept the job green. The list is now the five guards that
  live in the engine module, and `CiGuardListGuardTest` fails the job if a name in
  it stops resolving.
- **The aggregate status check notices when nothing was built.** `CI Gate` is one of
  the two checks `develop` and `main` require, and it watched the four heavy jobs
  without watching the path-detection job they all gate on. When that job's
  `git fetch` returned HTTP 503 the four resolved to `skipped` rather than `failure`,
  so the gate found nothing to report and went green over a run that compiled
  nothing — leaving both required checks green and, on a pull request, the branch
  protection satisfied by a build that never happened. The gate now aggregates the
  detection job too, and `CiGateCoverageGuardTest` reads the workflow and fails if
  any job that can run on a pull request is left out of it. Schedule-only jobs are
  recognised from their own `if:` condition rather than an exclusion list, so a new
  job either joins the gate or fails the guard.
- **`graph-compose` publishes an API reference again.** The coordinate the README
  sends readers to for Javadoc carries no sources of its own, so the javadoc goal
  found nothing to archive and attached no artifact — not an empty one, none. Every
  2.x release shipped without it, and javadoc.io, which serves the newest version
  that carries one, kept rendering the **1.9.1** API: complete, convincing, and two
  majors stale, beside prose promising documentation fresh after each release. The
  wrapper now builds that jar from the engine's sources, which is the surface a
  caller of this coordinate authors against — 903 pages where there were none. Doc
  lint stays off, as it is for the engine's own release jar, but a hard failure is
  no longer swallowed: the `failOnError=false` that hid the empty state is gone.
  `PublishedJavadocCoordinateGuardTest` fails if a coordinate the documentation
  advertises as an API reference cannot produce a javadoc jar, and the release
  checklist builds the wrapper's before a cut.
- **A documentation-only pull request is compiled.** Markdown was not a
  change-detection input, so a PR touching only `.md` skipped the reactor build
  and merged without `DocumentationSnippetCompileTest` ever compiling the java
  fences it publishes. Markdown now routes to the reactor slice that carries those
  guards, on the baseline JDK, and still skips example generation.
- **`graph-compose-render-pptx` declares PDFBox.** It compiles against
  `org.apache.pdfbox` types while declaring only `fontbox`, taking the rest
  transitively; the resolved version is unchanged.
- **The contributor guide stops teaching a removed architecture.** The engine
  implementation guide still walked a new object through attaching components to an
  entity, adding a render marker and a container-growth marker, and implementing
  `Breakable` — a model 2.0 removed, and one the current contributing guide
  contradicts. It is archived, with a banner naming what replaced it; the extension
  guide and the package map are now the route for anyone adding a node, a handler or
  a backend.
- **Documentation drift fails the build.** Three checks now cover what four
  releases of hand-fixing kept re-breaking: a relative link in the public docs
  must resolve, a contributor-facing document must not name a type 2.0 removed,
  and a published snippet that names a `*_BOLD` font constant must pair it with a
  decoration — the constant resolves to its base family, so without the decoration
  the text renders regular. The README release-status block must name a published
  version and link the tag it names. `SECURITY.md`, `SUPPORT.md`, `ROADMAP.md` and
  `.github/` are scanned for the first time; historical records are skipped by path,
  so a new archived page is covered the day it lands.
- **Code scanning reaches the render backends.** The scan compiled the engine module
  alone, and the Java extractor sees only what the build compiles — so the PDFBox and
  POI parsing paths, the SVG and image handling, font loading and the ZIP/OPC writers
  were not partially analysed, they were absent from the scan, with nothing in a green
  result to say so. Every deployed module that carries code is now named outright, and a
  guard holds that list against two inventories that fail differently: what CI compiles,
  and what a release deploys. The second is what catches an artifact added to the publish
  train and forgotten everywhere else — the first cannot, because a module missing from
  both lists leaves them in perfect agreement.
- **The package map is derived from the source tree.** A backend was findable only if
  someone remembered to list it, and the backend-neutral fixed-layout SPI was missing
  from the contributing guide — the one document a reader consults before adding an
  output format, where a contributor registers a fragment kind with the backend they
  can find, and a kind registered with only one fixed-layout backend renders in one
  output and vanishes from the other. Packages are now discovered by scanning every
  reactor module for `*Backend` types, so a backend arriving in a new module is covered
  the day it lands. Each must be named in the contributing guide and the package map in
  its own right: naming a parent covers no child, or adding the missing parent would
  have made every package beneath it uncheckable.
- **The READMEs are compiled.** The snippet guard read only `docs/`, leaving the pages
  a reader copies from first — the root one and each module's — free to name a method
  the library no longer has. Every Java fence in a README now either compiles or carries
  the reason it cannot, so an unmarked block no longer reads as a covered one: seven
  compile against the current API on every build and forty-five are exempt on the
  record. A module README opens with a three-line taste of the API, which the imports it
  needs would double in length, so a snippet can take them from the invisible marker
  instead; the compile verifies those too. `docs/private/` is out of the scan, and the
  two guards that read the published documentation resolve the same set of pages rather
  than each keeping its own list.
- **The showcase register is checked against the catalogue.** The register falls back to
  a filename-derived card, so an entry keyed on a document the runner never writes is
  never read: no card, no warning, no failure. Every entry must now match a generated
  document, and its source link must resolve to a file in the tree, so a renamed example
  fails the build instead of leaving a 404 behind the card. The example tree is emptied
  before it is rebuilt — the runner only writes, and a leftover from an earlier build
  would answer for an entry that has nothing left to describe.
- **The release publishes the showcase it just built.** `cut-release.ps1` never
  ran `GenerateAllExamples`, so the site was synced from whatever happened to be
  in `examples/target/generated-pdfs` — nothing at all on a clean checkout, which
  aborted the cut. Step 4 regenerates the catalogue first.
- **A removed example stops being published.** `ShowcaseSync` copied but never
  deleted, so an artifact whose example was renamed or removed stayed live on the
  site indefinitely, reachable by URL and absent from the manifest. Three such
  documents are gone.
- **Every generated document has a showcase card.** Eight artifacts had no
  metadata entry and published as filename-derived placeholders linking the
  examples root — among them both flagships the 2.1 line exists to show.
  `EngineDeckV2Example`, which renders the deck the README banner is cut from,
  was never wired into the example runner at all.
- **Five committed previews were re-rendered.** The largest, the PDF-chrome
  preview, was drawn at low alpha throughout with the watermark bleeding through
  every panel. Eleven committed binaries that nothing references, 9 MiB of them,
  are removed.
- **The release script owns the README release-status block.** It was a
  hand-edit the script only validated, and the validation demanded the *target*
  version — so between releases `develop` had to advertise an unpublished version
  as "latest stable", behind a release link that 404s. `cut-release.ps1` now
  promotes the in-development half to latest stable and opens the next patch line
  as part of the release commit, and verifies the result after the mutation.
- **An install snippet in the documentation moves with the release.** The version
  guard covered the README, the module READMEs and the showcase site, and stopped
  there. The troubleshooting page carries the two snippets a reader copies at the
  worst possible moment — when a session already refuses to start — and both had
  sat on the previous minor since 2.0, handing out a render backend one minor
  behind the engine that reader was running. The guard now walks `docs/`, skipping
  the trees that pin an old version on purpose — migration guides, archived pages,
  shipped roadmaps and the like — by path prefix rather than by a list someone has
  to remember to extend. A second check closes the same gap one step earlier: a page
  that carries such a snippet must be named in both of the release script's lists —
  the one that rewrites the version and the one that stages the file — so a new page
  cannot quietly keep the old version through every future cut. The bumper itself no
  longer relies on a file naming a single coordinate: it now skips
  `graph-compose-fonts` and `graph-compose-emoji` wherever they appear, since those
  ship on their own release lines.

### Fixed

- **A bar chart's value labels get the halo the style asks for.** The halo chip exists
  so a grid line stops running through the digits. `BarChartLayout` resolved the colour
  from the style and then passed `null` for every value label except a stacked total —
  a hardcoded argument forty lines from a sibling call that passed the real one. The
  result read as a feature that does nothing: the style accepted a halo, the layout
  computed it, and the label was drawn without it. Line charts and donuts had it all
  along, which is why the gap survived since 1.8.0 — where the release notes already
  promised the chip for a label sitting at a "marker **or bar top**". Two arguments, and
  the six committed previews that carry a bar chart with outside labels are re-rendered
  with their grid lines cleanly interrupted.
- **The example catalogue renders the weights it declares.** The same defect the donut
  KPI had, 138 times over in the examples: a font *face* constant named with no
  decoration, which the library rewrites to its base family before the lookup. The PPTX
  backend began reading the face from the decoration in the 2.1 line, so decks that had
  rendered bold went out regular — a live loss on `develop`, invisible because nothing
  re-rendered the committed previews to show it. Restoring the weights changed four
  decks back and gave a fifth the emphasis it had always asked for. Thirty-one committed
  previews and the README hero are re-rendered; the weekly schedule's columns are recomputed to fill the
  printable width, because bold day notes no longer fit a span over fixed sub-columns and
  a spanned cell cannot borrow width from them.
- **The donut-centre KPI renders the weight it declares.** A style names a font
  *family* and a *decoration*, and the decoration is what picks the face within the
  family — the standard-14 face constants (`HELVETICA_BOLD`, `TIMES_ITALIC`, …) are
  aliases of their family and carry no weight of their own. The chart default named
  the bold face and set no decoration, so it rendered regular, measured with regular
  metrics, in every donut chart that did not override the centre style. Nothing
  announced it: the text laid out and drew. `FontLibrary` now logs one warning per
  face constant it rewrites, the sixteen places in the library that named a face
  redundantly name their family instead, and the resolved glyph program is pinned by
  test — both the rule and the donut default itself, since the rule alone would not
  catch the site going back. Three rendered documents change — the engine
  deck, the feature catalogue and the chart showcase. ([#451](https://github.com/DemchaAV/GraphCompose/issues/451))


- **A heading no longer strands above a block that was asked to stay whole.**
  `keepWithNext()` decides by asking whether the heading plus the *first line* of
  the next block fits, but a `keepTogether()` block has no first line to break
  after — it relocates entire. The lookahead measured the first line anyway,
  concluded it fit, left the heading in place, and the compiler then moved the
  whole block to the next page. It affected any heading above a block that opts
  into `keepTogether()` — including a timeline built with
  `keepEntriesTogether()`, whose entries are kept whole one level down. A block
  taller than a page still anchors by its first line, because its keep-together
  request is ignored anyway.

### Documentation

- **Every relative link in the docs goes somewhere, and stays that way.** Nothing read a
  link: no test followed one, no CI step checked one. Renaming a heading silently broke
  every jump to it, moving a file broke every link into it, and both look harmless in a
  diff. `DocumentationLinkGuardTest` now resolves all 1051 relative links across the 99
  published pages — file targets and anchors, the latter computed with GitHub's own
  heading rule. It found eight dead anchors in the examples catalogue: five missed their
  heading by a single hyphen, because the em-dash in `CV — single template` collapses to
  two, and three pointed at sections that no longer exist. The five are corrected; the
  three keep their name and lose the link, since the row already carries working PDF and
  Source links.

- **The engine deck stopped calling a shipped backend planned.** Its first page listed
  PPTX as *Planned* beside a version badge reading v2.1.0 — the release that shipped it,
  and the release whose own copy of that deck is published as a `.pptx`. The page also
  described the library as generating "structured business PDF documents" and ended its
  pipeline at "PDFBox writes the bytes", and page two was headed "From one Java file to
  a designed PDF"; all three were written when PDF was the only output. PPTX is now the
  live `@Beta` backend it is, ordered ahead of the semantic-only DOCX export, and the
  prose covers both formats.
- **The Maven Central banner became a deck.** It was one 16:9 slide: wordmark,
  coordinate, capability tags and a code → layout → document diagram. It now carries
  three more pages in the same amber-on-navy language — the authoring pipeline and what
  each step guarantees, the measured comparison against iText 9 and JasperReports, and
  how all three behave as the report grows from 40 to 1000 rows. Every figure on the
  last two pages is read from the committed benchmark file at render time rather than
  typed into the layout. The `.pptx` gains the same three slides; the PNG preview stays
  the banner alone, since that is what it is for. `MavenBannerNativeShapeTest` now walks
  all four slides and holds the whole deck to one rasterised element — the SVG
  checkmark — so the table and both charts have to arrive as native shapes rather than
  as an embedded image, which is the claim the last page makes.
- **The PowerShell commands run as written.** Two pages handed Windows readers a
  command that fails. PowerShell splits a `-D` flag whose property name contains a dot,
  passing the native command `-Dexec` and `.mainClass=…` as separate arguments, and
  Maven then rejects the fragment with `Unknown lifecycle phase ".mainClass=…"`; a flag
  with no dot before the `=` survives, which is what makes it easy to miss. The
  examples README compounded it by claiming the bash form works unchanged on
  PowerShell — the block above it ends a line with `\`, a continuation PowerShell does
  not have — while pointing at backslash paths, which were never the problem. Both
  pages now carry the command that was run on PowerShell to check it.
- **The authoring cheatsheet stops describing `lineSpacing` as a multiple.** Its
  address-block recipe passed `1.3` and explained it as overriding a default of `1.0` —
  the reading a CSS `line-height` invites, and the one the number itself suggests.
  `lineSpacing` is extra space in **points**, defaulting to `0`: a paragraph grows by
  `(lines - 1) × spacing`, so `1.3` bought a little over one point of air rather than
  30% more leading, and there was no `1.0` default to override. The recipe now passes a
  value that does what the prose beside it promises, and the page states the unit.
- **The chart recipe documents the styling surface it has.** Five settings were named
  nowhere on the page — the three text styles, the donut centre style and the bar width
  ratio — four of them already load-bearing in the flagship examples that put a chart on
  a dark card, and discoverable only by reading the builder's source. The
  value-label halo was filed under line charts, the one place its default white chip is
  least likely to be wrong, rather than described as what it is: the backing behind
  value and slice labels alike, and the first thing that must move when a chart leaves a
  white page. And `ChartTheme` was presented as a layer an author styles through, though
  no authoring API accepts one — a chart resolves its geometry after the document's theme
  is out of reach, so `ChartStyle` is the author-facing whole of it. The low-level
  `ChartLayoutResolver.resolve(...)` does take a `ChartTheme`, and the page now says so
  rather than leaving it out. The page carries every setting
  with its default and what it affects, worked examples for typography and for the halo,
  and `ChartStyleDocumentationGuardTest` fails the build when a setter reaches the builder
  without reaching the page.
- **One recipe catalogue instead of two.** The cookbook page and the folder index each
  carried a hand-maintained table of all twenty-two recipe pages. They happened to agree
  on which pages exist and disagreed on ten of the descriptions — the folder index named
  PDF417 and DataMatrix among the barcode symbologies, the cookbook did not; the cookbook
  was fuller on themes, transforms and page backgrounds. Merged into the page both the
  README and the documentation index point at, taking the better description of each
  pair, so nothing is lost. The folder file keeps the one job GitHub gives it — being
  what you see when you open the directory — and now says where the catalogue is instead
  of holding a copy that drifts.
- **Documents nobody could reach are reachable.** The archive index named its three
  files in code spans rather than links, so the folder's own table of contents did not
  actually lead anywhere, and one of the three was not listed at all. The two benchmark
  notes sitting beside the committed baseline had no inbound link from anywhere; the
  benchmark guide names them now, and says what they are — an April 2026 capture and the
  before/after of the optimization round it belonged to, six weeks older than the
  baseline file and unrelated to it.
- **The starting-point table has an answer for the ordinary document.** Its three rows
  each asked a "yes" question — a known template family, pixel-level control, a re-usable
  new type — and a reader generating an ordinary report from data matched none of them
  and fell off the end. The DSL is the default, not the fallback, and the table says so.
- **The example catalogue stops calling current code legacy.** Two examples sat under
  a heading reading "🗄️ Legacy — pre-rebuild examples kept for downstream callers still
  on V1; do not start new code here", pointing at a V1 → V2 migration guide. There is no
  V1: that surface was removed in 2.0, both examples compile against the current DSL,
  and three hundred lines further down the same file called one of them a "useful
  starting point". They are what they always were — documents assembled from primitives
  with no template behind them — and the category now says that.
- **Reading one example no longer starts by rendering a hundred.** The run
  instructions led with `GenerateAllExamples`, which empties the output directory and
  regenerates the whole catalogue, and only afterwards mentioned running a single class.
  A newcomer following the page in order paid for the catalogue before seeing one
  document. Single-example first now, with the rule for deriving any other example's
  main class from its source path, and the batch entry point below it — described as
  what it actually is, the source both `cut-release.ps1` and `ShowcaseSync` read from
  rather than something that publishes anything itself.
- **The landing page lets you run it before it finishes selling it.** Ninety-one lines
  and twelve images stood between the title and `## Installation`, most of them a second
  showcase: thirty-five lines demonstrating the PowerPoint backend, with two renders and
  a screenshot, before the reader had seen one line of the authoring API. That section
  now sits after "Next steps", where a reader who has already written a document can
  appreciate that the same session emits a deck; install is reached in fifty-two lines
  and nine images, seven of which are badges. The "Upgrading from 1.x" note moves into
  Installation, where an upgrader is already headed.
- **The first snippet a reader copies is a file they can run.** "Hello world" opened
  with eight imports and eight public types — colours, text styles, a font, a page
  background, a soft panel, an accent strip — before the reader had rendered anything.
  It now opens with four types and a complete `Hello.java`: paste, run, get a PDF. The
  styled version keeps its place directly beneath as "Make it cinematic", which is what
  it was always demonstrating, and points at the committed preview of the example that
  renders the whole family. The first-document guide keeps its snippets as statements —
  three `main` wrappers would bury what it is teaching — and now says so, pointing at
  the README for the complete file rather than leaving the reader to discover that its
  code does not compile on its own. Also: the feature map, `docs/capabilities.md`, is
  now reachable from the landing page rather than only through the documentation index.
- **The last three documents that put `MissingBackendException` at the render call.**
  An earlier pass moved the module READMEs and the exception's own Javadoc onto
  `create()` and stopped at the repository root, so the troubleshooting entry, the 2.0
  migration guide and ADR 0016 kept telling a reader to look at `buildPdf()` — and the
  migration guide links straight into the troubleshooting entry, so the two reinforced
  each other. All three now name the call that actually fails, and the troubleshooting
  entry adds the one case that really does surface at the output call: `buildPptx()`
  when the PPTX backend is missing but the PDF one is not. The contract test carried
  the same confusion: it wrapped `create()`, `pageFlow(...)` and `toPdfBytes()` in one
  assertion, so only the first line ever ran while its name promised the third. It is
  split, and a second case pins the other side of the boundary — configuring a document
  needs no backend, opening the session does. The output-call half is covered where it
  is actually reachable: a test in `render-pdf`, whose classpath has one backend and not
  the other, calls `buildPptx(...)` through the public API rather than the resolver
  underneath it. The old heading keeps working as an anchor, so links already published
  against it still land on the entry.
- **The examples stop describing the releases they were written for.** Eight committed
  previews read as documents about 1.x: three framed a current feature as "v1.6 Phase
  A/B/C" — a plan for a release that shipped — one told the reader to tag v1.9.0 to
  publish a module that has been on Central since, two signed off "Composed with
  GraphCompose v1.5", one badged a canvas demo "v1.8", and the certificate on the
  free-canvas page was awarded for shipping v1.6. They describe what they demonstrate
  now, so nothing in them dates again. The hyperlink example was worse than dated: both
  URLs it rendered — a template-authoring page and a v1.6 roadmap — had been deleted, so
  the example that demonstrates links shipped two of them broken. They point at the
  preset cheatsheet and the extension guide.
- **A document that prints the date can be rendered twice and come out the same.** The
  `{date}` header token resolved from the clock with no way to pin it, so any document
  using it was a different file every morning — and the example demonstrating it could
  not be held to its committed preview at all. `-Dgraphcompose.renderDate=YYYY-MM-DD`
  fixes what the token resolves to, the way `SOURCE_DATE_EPOCH` does for archives; unset,
  it is the clock as before. The examples module pins it, so that preview is now compared
  like every other one instead of being trusted.
- **Two things the previews used to be read for are now checked.** A preview naming a
  release the project has moved past, and an example rendering a link to a repository
  path that no longer exists — both render perfectly, so only reading caught them, and
  both had been true for six releases.
- **A committed preview can be reproduced from a branch that has moved past it.** The
  documents that print a version took it from the reactor, which between releases sits on
  the next patch — so a render from `develop` named a version nobody could depend on yet,
  and stripping the `-SNAPSHOT` never helped because the number itself had moved. The
  version to display can now be passed in; `banner.properties` still sources
  `@project.version@`, so the reactor remains the answer when nobody says otherwise. Two
  headers stopped carrying the `{date}` token, which the engine resolves against the wall
  clock and which made the same document differ by the day it was rendered — the token
  stays in the example whose subject is the token. The number also left the places that
  merely decorated with it, a hero kicker and two footers among them, and stays where it
  informs: the version pill and the Maven coordinate card, which now reads the same input
  as everything else instead of a string literal.
- **The example catalogue is built one way, and it starts empty.** The runner only ever
  wrote, so a renamed or deleted example left its old document behind — published to the
  site, counted by the guards that read the tree, and indistinguishable from a current
  one. Generation now clears the output tree first, inside the runner itself, so the
  release script, CI and the tests all get the same guarantee instead of the one path
  that happened to remember. `ShowcaseSync` removes the directories as well as the files
  it replaces, which makes the published tree a function of the catalogue rather than a
  record of everything ever generated.
- **Nine committed previews are gone and two examples now run.** Seven were pre-2.0
  renders of CV presets whose current versions the runner already produced, so the
  gallery linked the older snapshot of each; those links follow the current render and
  the stale files are removed. Two more, an invoice and a proposal, were left by a rename
  and nothing linked them at all. The cover-letter example and a custom-theme CV were
  never wired into the runner, so one of them backed a link with a file no code produced
  — both are generated now, and both have a showcase entry.
- **The template-authoring guide describes the packages that exist.** It told a
  contributor to put a new family under `templates.<family>.v2` in exactly five
  sub-packages, one of them a per-family `theme/` — a layout 2.0 replaced. A family has
  `data` / `components` / `widgets` / `presets`, the cosmetic tokens are the shared
  `BrandTheme`, and the rule about not editing the v1 surface outlived that surface. The
  contributing guide had the same problem in one paragraph, routing new template code
  into `templates.builtins` and `templates.support`; both are gone, so the instruction
  produced code that does not compile. Package names join the retired-surface guard,
  which until now could only see types.
- **Three documents stop pointing at things that are not there.** The extension guide
  sent a reader to a snapshot-test directory that does not exist, the layout-snapshot
  page listed two tests among six that are not in the repository, and the docs index
  still carried a rework warning for a page reworked a major release ago. The roadmap
  asked for a warning on dropped DOCX content that the backend already logs; what it is
  really asking for — a mode that refuses instead of dropping — now says so.
- **The example catalogue stops offering a type that ships nowhere.** Its entry-point
  table introduced the cover letter as a `BusinessTheme.modern()` document, and
  `BusinessTheme` is a record local to the examples module — a reader adding the
  dependency and reaching for it finds nothing to import. The rows and prose now say
  what the examples demonstrate, the advanced section that quotes the helper says
  plainly that it is examples-local and names `BrandTheme` as the shipping equivalent,
  and a guard rejects the factory-call form on any README while leaving the quotation
  of an example's own source alone.
- **The last hardcoded counts are gone.** The example catalogue named a number of
  generated documents and a number of committed previews; both sat well below the real
  inventory, having been reconciled by hand once already and drifted again since. The
  banner caption stated a line count for the example it links, and named the module graph
  by a version the image itself no longer shows. All four are removed rather than
  corrected — a count in prose has no owner and nothing to keep it true, so correcting
  one only resets the clock.
- **The contributing guide's commit examples match the convention it asks for.** It
  pointed at two subjects from the 1.5 line as the shape to copy, while the repository
  has moved to Conventional Commits and the pull-request template requires that shape in
  a title. The examples are two real recent subjects instead.
- **Documented headings render bold, and say why.** Six snippets across the
  getting-started guide, the root README and the theme, timeline, rich-text and
  preset-authoring recipes set `fontName(FontName.HELVETICA_BOLD)` without a
  decoration. The name selects the family and the decoration selects the face
  within it, so each of those headings rendered regular. The timeline snippet was
  the sharpest: it presents itself as the default title style, while
  `TimelineBuilder` does set `BOLD` — a reader copying it to change only the size
  lost a working default. The snippets now name the family they mean
  (`FontName.HELVETICA`, `FontName.COURIER`) rather than a `*_BOLD` constant that
  `FontLibrary` resolves to that same family, so the pair reads as one choice
  each instead of `BOLD` twice.
- **Module usage examples reach an artifact.** The `core`, `render-pdf`,
  `templates`, `testing` and `emoji` READMEs opened a session, built a document
  graph and closed it without rendering anything. Each now shows the smallest
  complete path, and the `templates` and `testing` READMEs state that neither
  artifact can open a session without `graph-compose-render-pdf`.
- **`MissingBackendException` is located where it fires.** `core/README.md`,
  `render-pdf/README.md` and the root README said a missing backend surfaces when
  you ask for a PDF; it surfaces at `create()`, because opening a session resolves
  the font metrics. `core/README.md` said both things eight lines apart. The
  exception's own Javadoc described a single lookup at the output call; it now
  separates the two that exist — the measurement provider resolved when the
  session opens, and the format backend resolved at the output call — and records
  that the explicit `render` / `export` paths skip only the second.
- **`toImages()` takes a dpi.** Three places advertised a no-argument overload
  that does not exist; the signature is `toImages(int dpi)`.
- **The PowerPoint snippet names the artifact it needs.** The first code block in
  the root README calls `buildPptx` on the classpath the README's own install
  snippet produces, where it fails.
- **Every published module README is reachable** from the module list in the root
  README; eight of the ten had no inbound link from anywhere.
- **The release surface names the release that shipped.** `ROADMAP.md` described
  2.1 as in development and 2.0 as the current stable line; 2.1 is now current,
  2.0 is history, and `## Now` points at what follows. The README release-status
  block, the changelog, the roadmap and the showcase metadata now agree on which
  version is published and which is in development.
- **`SECURITY.md` covers every 2.x line.** The supported-versions table jumped
  from `2.1.x` to `1.9.x`, so a 2.0.x user could not tell whether their version
  was supported.
- **The documentation index carries the migration guide that applies.**
  `docs/README.md` listed five superseded 1.x minor-to-minor upgrade guides on the
  main path and omitted `migration/v2.0.0-modules.md` entirely. The current
  guides are listed; the historical ones moved behind a fold.
- **The PPTX backend is documented where it lives.** `CONTRIBUTING.md`,
  `SECURITY.md` and the architecture overview described the pptx module as a
  semantic exporter. Its primary backend is fixed-layout
  (`document.backend.fixed.pptx`), alongside the older semantic manifest; a new
  fragment kind must register a handler with both fixed-layout backends or it
  renders in one output and vanishes from the other.
- **Contributor instructions compile.** The "new built-in template" section
  routed authors to `BusinessTheme`, `InvoiceTemplateV2` and `ProposalTemplateV2`
  — none of which exist since 2.0 — and the engine-primitive section described
  the entity/marker execution layer that 2.0 removed. Both now describe the
  layered preset stack and the `NodeDefinition` pipeline. The bug-report template
  asked for a 1.6-era version and carried a reproduction that never rendered; the
  feature-request template pointed at a removed theme type; the pull-request
  template offered a lane that no longer exists.
- **Usage questions route to Discussions** through a new issue-template
  `config.yml`, instead of being filed as labelled issues.
- **Hand-maintained example counts are gone.** Four places stated four different
  totals, none matching the catalogue.
- **The release runbook checks things that exist.** One gate grepped the README
  for a test-count claim the README does not make, so it always passed; another
  compared the gallery row count against the example file count, two numbers that
  had long diverged; three references pointed at a migration path that moved.
- **A gallery entry for a deleted example is gone.** `examples/README.md`
  documented rethemeing through `InvoiceTemplateV2`, a type removed in 2.0, linked
  a source file that no longer exists, and carried a committed preview for it. The
  single-example run command also named the wrong class, so it could not be
  copy-pasted.

## v2.1.0 — 2026-07-26

### Highlights

- **The same document now also prints to PowerPoint.** `graph-compose-render-pptx`
  gains a fixed-layout backend that consumes the *same* resolved layout graph as the
  PDF backend, so one page becomes one identically-sized slide and every element keeps
  its position by construction — as native, editable shapes rather than a picture of a
  page. Ships as `@Beta` (Experimental) in its first release.
- **A failed render no longer destroys the document it was replacing.**
- **Headings stay with their content** across a page break, in the engine and in the
  CV and proposal presets.
- **CV entry, subtitle and project-card titles accept inline `[text](url)` links.**

### Public API

- **Editable PowerPoint output.** `buildPptx(Path)`, `writePptx(OutputStream)` and
  `toPptxBytes()` render the current session through the PPTX backend when
  `graph-compose-render-pptx` is on the classpath; a missing backend fails with
  `MissingBackendException` naming the artifact. `DocumentPageSize.SLIDE_16_9`
  (960 × 540 pt) and `SLIDE_4_3` (720 × 540 pt) match the PowerPoint defaults.
  The PPTX surface — the `document.backend.fixed.pptx` packages and these
  convenience methods — carries `@Beta`, so its shape may still change in a minor
  release; the geometry relationship with the PDF backend is a design invariant and
  is not subject to change. See [docs/api-stability.md](docs/api-stability.md).
- **A failed render no longer truncates the destination.** `buildPdf(Path)`,
  `buildPptx(Path)` and multi-section `buildPdf(Path)` render into a scratch file in
  the destination's own directory and move it onto the destination only after the
  render returns. Previously the destination was opened — and therefore emptied —
  before the backend produced a byte, so an oversized node, a missing backend or a
  full disk left an empty file where a published document used to be. The move is
  atomic where the filesystem supports it, so a concurrent reader never observes a
  half-written document; on POSIX the destination keeps the permissions it already
  had, or gets `rw-r--r--` when it is new. Replacing the destination entry replaces
  a symlink rather than writing through it.
- **Keep a heading with its content** — `SectionBuilder.keepWithNext()`. A section
  marked keep-with-next is never left stranded as the last block on a page apart from
  the content it introduces: when the section plus the first slice of the following
  block would overflow the remaining page space but fit on a fresh page, the section
  relocates. The first slice is a paragraph's first line, a table's repeated header
  rows plus first body row, or a list's first item, so the rule holds whether the
  following block is atomic or page-spanning. Distinct from `keepTogether()`, which
  relocates a *whole* block. Inert when nothing follows, best-effort when the heading
  plus the first slice cannot share a page, and off by default.
  `LineBuilder.keepWithNext()` is the line counterpart, so a full-width header rule
  joins its banner's run and the whole title block moves together.
- Keep-with-next also surfaces on the node model: `DocumentNode.keepWithNext()` is a
  new default method (existing implementations keep the `false` default), and
  `SectionNode` and `LineNode` each gain a trailing `keepWithNext` record component.
  Constructor calls are unaffected — both records keep an overload at the previous
  argument count — but the canonical component count changed (`SectionNode` 13 → 14,
  `LineNode` 17 → 18), so a **record deconstruction pattern** written against the
  2.0.0 component list must add the new binding to compile.
- **`DocumentSession.buildPptx()` (no-arg) is removed**; use `buildPptx(Path)`. The
  session has a single configured output path, shared with `buildPdf()`, so the no-arg
  form wrote deck bytes into whatever that path was — including a file named `.pdf`.
  Naming the destination is also what lets one session emit both formats. The PPTX
  surface is Experimental and was never published, so no released code can depend on
  the removed overload.
- **Two backends registered for one format now fail loudly.**
  `BackendProviders.fixedLayout(String format)` selects a fixed-layout backend by its
  `FixedLayoutBackendProvider.format()` key (case-insensitive), so several backends can
  coexist on one classpath; the no-arg default resolves the `"pdf"` provider when
  present, otherwise the lexicographically smallest format. Where both entry points
  previously took the first `ServiceLoader` match — letting classpath order decide the
  renderer — an ambiguous format now throws `IllegalStateException` naming the
  competing provider classes.
- **`MarkdownInline`** gains `appendTransformed`, `appendUpperCased` and
  `appendIfPresent`, and **`PdfRenderEnvironment`** gains `fillAlphaState` /
  `strokeAlphaState` for handlers that need a shared alpha graphics state.

### PPTX backend

The per-capability status — what is native, what is approximated, what is unsupported
— lives in
[docs/architecture/backend-capability-matrix.md](docs/architecture/backend-capability-matrix.md).
Of the 38 capabilities it tracks, 24 map to a native equivalent and 4 are unsupported.
The remaining 10 are partial, and not all in the same way: some render natively with an
approximated styling detail (distinct per-corner radii collapse to one value, numeric
dash arrays map to the nearest preset, a radial gradient uses the closest DrawingML
shade), while others lose something the format cannot carry — see Known limitations.

- **Content.** Paragraphs render as absolute, wrap-disabled text frames seated on the
  measured baselines with PDF-identical glyph sanitization, carrying rich runs, inline
  code chips, inline images, shapes and SVG. Tables render as row fills, border edge
  lines and per-cell text frames at graph coordinates, across page breaks with repeated
  headers and row spans. Shapes, ellipses, lines, polygons, free paths (with gradient
  fills and strokes), images, barcodes and transform groups all render natively.
  Unsupported payloads fail with `UnsupportedNodeCapabilityException`; custom handlers
  plug in through `PptxFragmentRenderHandler` and `Builder.addHandler`, which rejects a
  duplicate registration for one payload type.
- **Chrome and navigation.** Metadata maps onto OPC core properties, watermarks and
  repeating headers/footers render per slide with token resolution, and hyperlinks,
  internal slide jumps and bookmark slide names are emitted. Multi-section documents
  concatenate into one deck; every section must share the slide size.
- **Fonts.** Families are embedded where the licensing bits allow it and the backend
  warns once per family whenever a font is substituted, so a deck that will render
  differently on another machine says so at build time.
- **Clipping.** DrawingML cannot express graphics-state clipping, so a clip region that
  can actually cut ink renders through the PDF backend into one transparent picture on
  the clip bounds — pixel-exact, but not editable as shapes, and run-level link
  hotspots inside it are not emitted. A clip that provably cannot remove ink (a rounded
  card whose padded content never reaches the corners) skips the fallback and stays
  native. `Builder.clipRasterFallback(false)` restores unclipped vectors with a
  one-time warning. The raster targets a 2048-pixel long edge, clamped between native
  size and 4×.
- **Raster-slide mode.** `Builder.rasterSlides(int dpi)` renders each page through the
  PDF backend and places it as one full-slide picture — a pixel-exact copy for decks
  that must not be edited.
- **Reproducible output is opt-in.** `Builder.deterministic(true)` or
  `deterministic(Instant)` pins the OPC created/modified properties and normalizes
  every zip entry timestamp, so the same document renders to byte-identical bytes
  across runs. **The default path does not**: `buildPptx(Path)` and the other
  convenience methods stream the deck with live timestamps, matching the PDF backend's
  opt-in convention.

### Fixed

- **PPTX text no longer overruns the frame the engine measured for it.** A span that
  named a standard-14 style variant — `Helvetica-Bold`, `Times-Italic` and their
  siblings — travelled to PowerPoint as a bold or italic run flag. Those names are
  family aliases: the engine resolves each to its regular base and takes the real face
  from the span's decoration, so the layout had measured the regular metrics. The viewer
  then drew a face about 6% wider than its slot, which pushed a chip label past its card
  and closed the gap between two words of a rich-text heading until they read as one.
  Run flags now follow the decoration for those families, so a deck renders the same
  face the PDF does; a binary family still carries its face in its own name.
- **A large clipped region in a PPTX deck is no longer downscaled.** The raster scale
  was capped only from above, so a clip box wider than 2048 pt rasterized *below*
  native size — an A0-scale composite landed near 44 DPI and read as visibly blurry,
  since the picture is anchored at the full clip size regardless. Decks whose clip
  regions fit a normal page are unaffected; their scale already saturated at the upper
  cap. A very large region now costs transient memory proportional to its size.
- **The PDF backend draws `DocumentTextDecoration.UNDERLINE` and `STRIKETHROUGH`.**
  The flags previously resolved only to font faces, which alias to the regular program,
  so decorated text rendered as plain glyphs while the PPTX backend already drew real
  marks. Marks are em-proportional filled bands in the run's colour — underline 0.10 em
  below the baseline, strikethrough 0.28 em above, thickness 0.05 em, the Type 1
  convention — on paragraph runs, chips and table cell text alike.
- **The PDF backend honours the alpha channel of `DocumentColor.rgba` everywhere** —
  text runs, lines, side borders, and table fills, borders and cell text — matching the
  shape fills and strokes that already carried it. Fully opaque documents render
  byte-identically to before.
- **`toImages` / `toImage` no longer rasterize binary fonts with substitute glyphs.**
  PDFBox writes embedded font subsets only during `save()`, so rendering the unsaved
  in-memory document never saw the real glyph programs and produced visually garbled
  text with correct spacing. The backend now saves to an in-memory buffer and reloads
  before rasterizing; standard-14-only documents are unaffected apart from the marginal
  serialization cost.
- The PPTX backend logs, once per render at `DEBUG` on
  `com.demcha.compose.engine.render`, how many clip regions were rasterized and their
  total megapixels — each one costs a full sub-render through the PDF backend and loses
  text editability inside its bounds. Silent when nothing was rasterized.

### Templates

- **Single-column CV presets no longer orphan a section title.** Every preset whose
  sections flow down the page — BoxedSections, MinimalUnderlined, ModernProfessional,
  Executive, CenteredHeadline, BlueBanner, EditorialBlue and ClassicSerif — keeps a
  section heading with the first line of its body across a page break, including
  Panel's full-width Profile card whose header now stays with the summary. Multi-column
  presets place their sections in fixed columns that do not paginate.
- **The Modern Proposal template no longer orphans a section heading.** Its flowing
  section bodies, the Timeline and Investment tables, and the Acceptance terms each
  keep their title with the first line of the block it introduces.
- **CV entry titles and subtitles accept inline `[text](url)` links.** A `[label](url)`
  in an experience or education entry title or subtitle renders as a clickable
  hyperlink — the convention already used for project rows and body text — and
  upper-cased or letter-spaced preset titles keep the link with the visible label
  styled. Where a preset fuses a line that cannot carry a link (the single-line
  TimelineMinimal excerpt, and the combined subtitle+date meta line of MintEditorial
  and SidebarPortrait experience entries) the label text is shown instead. Titles
  without inline-Markdown syntax render exactly as before.
- **Project-card titles accept the same links in the `EngineeringResume` and
  `SidebarPortrait` presets**, whose hand-rolled cards previously emitted the title as
  flat styled text; the trailing `" (stack)"` run is preserved.

### Documentation and examples

- **The examples now show the engine's warnings.** Their logging config silenced
  `com.demcha.compose` entirely, so a reader running a deck example never saw which
  fonts the viewer would substitute or which capability had degraded — the warnings
  fired into a muted logger. The output stays short because every warning is
  deduplicated before it is logged: a font substitution once per family per render, a
  capability note once per kind for the process.
- **`render-pptx` documents how to get a glyph-identical deck.** Geometry matches the
  PDF by construction, but glyphs are drawn by the viewer; the page now states the
  three cases — a standard-14 name travels as a metric-compatible viewer font with
  identical widths and different letterforms, an embeddable binary family travels as
  itself, and a name-only family depends on the viewer having it installed.
- **`TwinOutputExample`** — a single 16:9 page written once and emitted twice from the
  same session. The README gains a dual-output section showing the PDF render beside
  PowerPoint's own export of the generated slide, plus the deck open in PowerPoint with
  the headline text frame selected; the committed artifacts land 69 native shapes with
  only the clip-masked logo as a picture.
- **`MavenBannerPptxExample`** — a single 16:9 "Available on Maven Central" brand slide
  composed as one full-bleed `CanvasLayerNode`, with a native-shape guard pinning it to
  a single picture (the badge checkmark).
- **PDF→PPTX twins** for the business report, financial report and master showcase, and
  the Engine Deck flagship also renders as a deck. The showcase site publishes the deck
  beside the PDF on the same card.
- **`docs/architecture/backend-capability-matrix.md`** — the per-capability matrix of
  what each backend supports and which class implements it, maintained as part of every
  capability-changing PR. The PPTX documentation, the architecture pages and the
  release runbook were rewritten to describe the shipped backend and the 2.x layout.

### Build and compatibility

- The consumer release-smoke harness gains scenarios for `graph-compose-render-pptx`
  and `graph-compose-render-docx`, so a broken or unpublished backend artifact can no
  longer pass the release gate unnoticed. Its default version under test now follows
  the release instead of staying pinned to the previous one.
- The examples CI job installs `graph-compose-render-pptx` before generating.
- The PDF backend reuses one `PDExtendedGraphicsState` per distinct (channel, alpha)
  pair for a render pass, so a page's resource dictionary stays bounded by the number
  of distinct alpha values instead of growing with every translucent draw. Fully opaque
  documents still carry no `/ExtGState` resources at all.
- `graph-compose-render-pptx` depends on `graph-compose-render-pdf` at compile scope:
  the backend reuses the PDF measurement font library so glyph widths stay aligned with
  the widths that produced the layout graph, and re-renders through it for the clip
  raster fallback. A PPTX-only consumer therefore also resolves the PDF stack.

### Known limitations

- Clipped regions that can cut ink become a picture: text inside them is not editable
  and run-level link hotspots are not emitted. True vector clipping is tracked in
  [#413](https://github.com/DemchaAV/GraphCompose/issues/413).
- Glyphs are rasterized by the viewing application, so text rendering depends on the
  fonts installed there. Frames and positions are fixed by the layout graph.
- Byte-identical PPTX output requires opting in through `Builder.deterministic(...)`.
- Document protection, viewer preferences and the debug guide-line overlays are PDF
  concepts and are ignored by the PPTX backend with a one-time warning;
  `renderToImages` is not implemented on it and throws.
- Distinct per-corner radii collapse to a single value and numeric dash arrays map to
  the nearest preset in PPTX.
- PPTX has no outline tree: the first bookmark on a page names its slide and **further
  bookmarks on that page are dropped**, so a bookmark-heavy document does not survive
  the round trip as navigation.
- Inline SVG stays native for simple layers, but arbitrary clips, exact dash/cap/join
  styles and off-viewBox art fall back to a transparent PNG, and gradient paints use
  their primary colour.
- OPC has no producer field, so document metadata's producer value is not representable
  in a deck.
- `graph-compose-render-docx` needs `graph-compose-render-pdf` on the classpath as
  well: opening a session resolves a font-metrics provider and the PDF backend is the
  only artifact that publishes one.

## v2.0.0 — 2026-07-13

The 2.0 development line. Binary-breaking by design — japicmp runs report-only
for this cycle.

### Removed

- The classic (pre-layered) CV and cover-letter template presets have been removed.
  The layered template stack — `templates.cv.*`, `templates.coverletter.*`,
  `templates.invoice.*`, and `templates.proposal.*`, all on `BrandTheme` — is now the
  single template surface.
- The standalone `BusinessTheme` design-token bundle (and its `DocumentPalette` /
  `SpacingScale` / `TextScale` / `TablePreset` companions) is no longer part of the
  library. It lives on only as a styling helper inside the examples module; author
  documents with explicit `DocumentColor` / `DocumentTextStyle` values or a template
  `BrandTheme`.
- The DSL name-aliases `DocumentSession.builder()` and `DocumentDsl.text()` have been
  removed. Use `DocumentSession.dsl()` and `DocumentDsl.paragraph()`.
- The PDF-typed document-chrome overloads on `DocumentSession` —
  `metadata(PdfMetadataOptions)`, `watermark(PdfWatermarkOptions)`,
  `protect(PdfProtectionOptions)`, `header(PdfHeaderFooterOptions)` and
  `footer(PdfHeaderFooterOptions)` — have been removed in favour of the canonical,
  backend-neutral overloads (`metadata(DocumentMetadata)`, `watermark(DocumentWatermark)`,
  `protect(DocumentProtection)`, `header(DocumentHeaderFooter)`,
  `footer(DocumentHeaderFooter)`). The PDF option types remain available on
  `PdfFixedLayoutBackend.builder()` for advanced backend-level configuration.
- The `linkOptions()` accessor on the document nodes and inline runs (paragraph, table,
  image, shape, ellipse, line, barcode, and the inline image / shape / text runs) has
  been removed. Use `linkTarget()` and read the external URI from
  `ExternalLinkTarget.options()`.
- The unused engine-internal `Font.adjustFontSizeToFit(...)` (and its `PdfFont` /
  `WordFont` implementations) has been removed; text auto-sizing is resolved by the
  layout compiler.
- The dormant Entity-Component-System engine internals have been removed: the
  `EntityManager` / `SystemECS` runtime, the `Entity` component model with its
  geometry / coordinator / renderable companions, the ECS render pipeline
  (`engine.render.*` and the guide renderers under `engine.render.guides`), and
  `LayoutSnapshotExtractor`. None were reachable from the live render path —
  `DocumentSession` → layout compiler → fixed-layout backend — so document layout,
  PDF output, and the public `guideLines(...)` overlay are unchanged.

### Public API

- **Reproducible PDF output** (`@Beta`). `PdfFixedLayoutBackend.builder().deterministic(true)`
  (or `.deterministic(Instant)` for an explicit timestamp) pins the document
  CreationDate / ModDate and derives the PDF `/ID` from the document metadata instead
  of PDFBox's time-seeded default, so the same document renders to byte-identical PDF
  bytes across runs — for reproducible builds and byte-level output tests. Off by
  default (output keeps the live timestamp and `/ID`). PDF encryption via `protect(...)`
  can reintroduce randomness (AES-256 uses random salts), so an encrypted document is
  not byte-reproducible even with this enabled. Multi-section documents opt in through
  the new `MultiSectionDocument.toPdfBytes(FixedLayoutRenderer)` /
  `writePdf(FixedLayoutRenderer, OutputStream)` overloads — the multi-section
  counterpart of `DocumentSession.render(backend)`.
- The layered template packages dropped their `.v2` suffix:
  `com.demcha.compose.document.templates.<family>.v2.*` →
  `com.demcha.compose.document.templates.<family>.*` for `cv`, `coverletter`,
  `invoice`, and `proposal`. Update imports accordingly — behaviour and rendering are
  unchanged; this is a package rename only.

### Packaging

- 2.0 splits the monolithic engine into modules. The engine now builds under a new
  **`graph-compose-core`** coordinate, and the original **`graph-compose` coordinate
  becomes a thin drop-in aggregator** that depends on `graph-compose-core` — so an
  existing `graph-compose` dependency keeps compiling and rendering PDF unchanged.
  Consumer-testing support (`graph-compose-testing`) and the semantic DOCX / PPTX
  backends (`graph-compose-render-docx` / `graph-compose-render-pptx`) are already
  separate artifacts; the **PDF render backend now lives in
  `graph-compose-render-pdf`**, which the `graph-compose` wrapper aggregates so a
  bare `graph-compose` still renders PDF, while `graph-compose-core` alone is lean
  (it throws `MissingBackendException` if asked to render without a backend). The
  **built-in templates now ship in `graph-compose-templates`** (opt-in; the
  `graph-compose` wrapper does not bundle them). The
  optional `graph-compose-fonts` / `graph-compose-emoji` artifacts are unchanged.
  Full install guidance ships with 2.0.0.
- `graph-compose-render-pdf` is a separate artifact: the entire PDFBox backend —
  `document.backend.fixed.pdf.**` and the `engine.render.pdf.**` render tree — plus
  PDFBox, zxing (barcodes), and the commons-logging→SLF4J bridge leave
  `graph-compose-core`. The core keeps the `DocumentSession` API and the
  `ServiceLoader` seam (`FixedLayoutBackendProvider` / `FontMetricsProvider`); the
  PDF provider ships in render-pdf and is discovered at runtime. Depend on
  `graph-compose` (or `graph-compose-render-pdf` directly) for PDF; a bare
  `graph-compose-core` renders nothing until a backend is on the classpath.
- `graph-compose-templates` is a separate artifact: the built-in CV, cover-letter,
  invoice, and proposal templates (`document.templates.**`) leave the engine. They
  are pure authoring code over the canonical DSL, so the module depends only on
  `graph-compose-core`. Add `graph-compose-templates` for the ready-made presets; a
  consumer that used them through `graph-compose` before must now add this artifact
  (the `com.demcha.compose.document.templates.**` packages are unchanged).
- The semantic office backends are separate artifacts and Apache POI leaves the
  engine: `DocxSemanticBackend` ships in `graph-compose-render-docx` (which brings
  POI transitively — add it to export DOCX), and `PptxSemanticBackend` ships in its
  own `graph-compose-render-pptx` (a POI-free skeleton for now — add it for the
  slide-safe semantic manifest). Their packages —
  `com.demcha.compose.document.backend.semantic.{docx,pptx}` — are unchanged. The
  `no-poi` build profile is retired.
- `graph-compose-testing` is now a separate artifact: the consumer testing
  support — `LayoutSnapshotAssertions` (deterministic layout snapshots) and
  `PdfVisualRegression` (pixel-diff of rendered pages) — leaves the
  `graph-compose` jar together with its Jackson and PDFBox dependencies. The
  `com.demcha.compose.testing.layout` / `com.demcha.compose.testing.visual`
  packages are unchanged, so imports stay the same; add `graph-compose-testing`
  at test scope to keep using them.
- `graph-compose-bundle` is the batteries-included aggregate for the split layout:
  one dependency pulls the default PDF stack (`graph-compose` = core + render-pdf),
  the built-in templates (`graph-compose-templates`), the bundled Google fonts, and
  the colour-emoji set at compatible pinned versions. The office backends
  (`graph-compose-render-docx` / `graph-compose-render-pptx`) stay opt-in and are not
  bundled.

### Internal

- The process-wide image caches (decoded source bytes and image metadata) are now
  bounded with LRU eviction instead of growing without limit. A long-lived JVM that
  renders many distinct images — a rendering service, a batch job — no longer
  accumulates image data indefinitely. Single documents are unaffected: the caps sit
  far above any realistic distinct-image count, so a render never evicts or re-decodes.

### Build

- Added report-only cross-module code coverage. A new non-published
  `graph-compose-coverage` module runs JaCoCo `report-aggregate` over the engine, the
  PDF backend, and the built-in templates plus the `graph-compose-qa` cross-module
  suites, so each module's coverage counts the tests that actually exercise it (a
  single-module report undercounts, because much of the production code is driven from
  qa at test scope). CI publishes the HTML/XML report as an artifact; no coverage
  threshold is enforced yet.

### Documentation

- Documented that text is laid out left-to-right only: bidirectional (RTL) reordering
  and complex-script shaping (Arabic joining, Indic reordering) are not performed. Added
  to the README support matrix.
- The 2.0 module migration guide now lists the API changes it previously delegated to the
  changelog — the `.v2` package rename, the `BusinessTheme` removal, the retired classic
  presets, and `Font.adjustFontSizeToFit` — each with its migration action.

## v1.9.1 — 2026-07-06

Table columns now contain long inline-code content instead of letting it spill
over the next column.

### Bug fixes

- **Long inline-code chips no longer overflow their column.** A long
  `inlineCode(...)` / `inlineHighlight(...)` token with no spaces — a package
  coordinate, fully-qualified class name or URL — now breaks *within* its
  paragraph or table cell instead of drawing over the neighbouring content. It
  breaks at `.` `:` `/` `-` seams and char-splits only when a segment is still
  too wide, keeping the rounded chip fill intact on every wrapped fragment.
  Applies to all three paragraph wrap paths (plain, inline, markdown); text that
  already fits is laid out exactly as before.

- **`auto()` table columns grow to fit composed cell content.** A composed cell
  (`DocumentTableCell.node(...)`) in an `auto()` column now contributes its
  intrinsic content width, so the column sizes to the content (e.g. an
  inline-code chip) instead of collapsing toward zero. `fixed(...)` columns are
  unchanged. As with plain-text auto columns, a table too narrow for the summed
  intrinsic width of its auto columns reports `exceeds available width`.

## v1.9.0 — 2026-06-29

In-document navigation. Rendered PDFs can now declare named **anchors** and
**internal links** that jump to them — clickable tables of contents,
`[text](#heading)`-style links, and bidirectional footnotes — emitted as native
PDF `GoTo` actions. External links are unchanged.

### Public API

- **`DocumentSession.pageMargins(List<PageMarginRule>)`** (`@since 1.9.0`). Overrides
  the page margin for ranges of pages, so one document can mix a full-bleed cover
  (`PageMarginRule.page(1, DocumentInsets.zero())`) with book margins on the body
  (`PageMarginRule.from(2, …)`) — both horizontally and vertically. Pages are 1-based;
  rules apply in list order, last-covering-rule wins. Each top-level block is laid out
  at the content width of the page it begins on. A document that sets no rules is laid
  out exactly as before.

- **`chrome().viewerPreferences(...)` + `DocumentViewerPreferences` /
  `DocumentPageMode` / `DocumentPageLayout`** (`@since 1.9.0`). Controls how a PDF
  reader presents the document on open — the page mode (`USE_OUTLINES` opens the
  bookmark panel, pairing with `bookmark(...)`), the page layout (single / one-column
  / two-column / two-page), and the window flags (`displayDocTitle`, `hideToolbar`,
  `hideMenubar`, `fitWindow`, `centerWindow`). Written to the PDF document catalog;
  `DocumentViewerPreferences.openOutline()` is a one-line preset. PDF-only — other
  backends ignore it. A document that sets none is unchanged.

- **Container `bookmark(...)`** (`@since 1.9.0`). `bookmark(DocumentBookmarkOptions)`
  on any container flow builder (`Section` / `Container` / page flow) — previously
  only the seven leaf builders carried a bookmark — adds a PDF outline entry pointing
  at that container's start page, making a structured document navigable through the
  reader's bookmark panel. Emitted via its own non-visual marker fragment, so it works
  even on an unstyled container, and a container without a bookmark is unaffected.

- **`RowBuilder.flexSpacer()` / `pushRight()` / `arrangement(...)` + `RowArrangement`
  + `SpacerBuilder.grow(...)`** (`@since 1.9.0`). Main-axis (`justify-content`) layout
  for a row. A `flexSpacer()` (or `pushRight()`) is an invisible spring that absorbs
  the row's leftover width — a title stays left while a badge sits flush right; a
  spacer's `grow(...)` factor sets its share. `arrangement(START / CENTER / END /
  SPACE_BETWEEN / SPACE_AROUND / SPACE_EVENLY)` justifies content-sized children
  instead. Flex is mutually exclusive with `weights` / `columns`. The default
  (`START`, no grow) is byte-for-byte unchanged, so existing rows are unaffected.

- **`RowBuilder.verticalAlign(...)` + `RowVerticalAlign`** (`@since 1.9.0`). Seats a
  row's children on the cross axis within the row band, whose height is that of the
  tallest child: `TOP` (the default), `CENTER`, or `BOTTOM` — the `align-items`
  analogue for a horizontal row, without manual coordinates. The measure phase is
  unchanged and `TOP` rows render byte-for-byte as before, so existing documents are
  unaffected.

- **`GraphCompose.documents()` + `MultiSectionDocumentBuilder` / `MultiSectionDocument`**
  (`@since 1.9.0`). Concatenates several independently authored `DocumentSession`
  sections — each with its own page size, margins, fonts, and footer numbering —
  into one PDF inside the engine, with no external PDF merge. Anchors, internal
  links, and the bookmark outline resolve across section boundaries against the
  combined document, and each section is numbered from its own first page, so a
  full-bleed cover of one page size can precede a margined, page-numbered body of
  another. Document-level metadata and protection are taken from the first section
  that declares them. Single-section output is unchanged. `MultiSectionDocument`
  is `AutoCloseable` and owns its sections.

- **`addTableOfContents(...)` + `TocBuilder` / `DocumentLeader`** (`@since 1.9.0`).
  A native, clickable table of contents: each `entry(label, anchor)` becomes a row
  whose label links to the chapter (`linkTo`), a dotted or dashed leader fills the
  gap, and the page number is resolved automatically from the laid-out document —
  no manual two-pass. Built entirely from the existing primitives (auto/weight
  columns, `line().fill()`, `addPageReference`) and added to the flow, so a long
  contents paginates across pages.

- **`addPageReference(anchor)` + `PageReferenceNode`** (`@since 1.9.0`). Prints the
  page a declared `anchor(...)` lands on — a native "see page N" cross-reference —
  in a single authoring pass. A document that contains a page reference is laid
  out twice: the first pass resolves every anchor's page, the second renders the
  references with the resolved numbers; the reference reserves only its content
  width in both passes, so its own footprint does not shift the pages it reports.
  Available on flows (`addPageReference(anchor)`) and inside rows (the number
  column of a table-of-contents row). Documents without a page reference are
  unaffected (single pass, byte-identical). `pageIndex()` remains for programmatic
  access.

- **`RowBuilder.columns(...)` + `DocumentRowColumn`** (`@since 1.9.0`). Size each row
  column explicitly: `DocumentRowColumn.fixed(pt)`, `auto()` (intrinsic content
  width), or `weight(w)` (a share of the space left after the fixed and intrinsic
  columns). Mix them freely — `columns(auto(), weight(1), auto())` with a
  `line().fill()` in the middle is a dot-leader table-of-contents row, with the
  label and page number sized to their content. `weights(...)` stays as sugar for
  the even / weighted split (and a weight-only column list resolves identically),
  so existing rows are byte-identical.

- **`LineBuilder.fill()`** (`@since 1.9.0`). A line stretches to the width
  available where it is placed — its column inside a row, or the content width at
  flow level — instead of its authored fixed width. Paired with a dotted stroke
  (`dashed(0.1, 4).lineCap(ROUND)`) it is the flex leader behind a
  table-of-contents row, drawn without measuring the gap by hand. A non-fill line
  is unchanged, so existing line output stays byte-identical.

- **Negative-margin handling** (`@since 1.9.0`). A negative **page** margin
  (`DocumentSession.margin(...)` or the builder's `margin(...)`) is now rejected
  with an `IllegalArgumentException` — it would make the content area larger than
  the sheet, silently overflowing it; use a node's `bleed(...)` to reach the page
  edge instead. Separately, a negative **node** bottom margin now pulls the
  following content up — symmetric with a negative top margin, which was already
  honoured (the vertical flow previously dropped it). Existing documents are
  unaffected, since neither shape was usable before.

- **`DocumentSession.pageIndex()` + `PageIndex` / `PageReference`** (`@since 1.9.0`).
  Resolves every declared `anchor(...)` to its final page in a single,
  backend-neutral pass over the laid-out document — `pageNumberOf("intro")` for a
  "see page N" cross-reference, `forAnchor(...)` for the full `PageReference`.
  Computed from the resolved layout graph (not from rendered PDF bytes) and cached
  per layout revision alongside `layoutSnapshot()`. The read-side foundation for
  clickable tables of contents and cross-references. A duplicate anchor resolves to
  its last registration — the same destination a `linkTo(anchor)` jumps to.

- **`DocumentPageNumbering` / `DocumentPageNumberStyle`** (`@since 1.9.0`). Header
  and footer `{page}` / `{pages}` tokens can now offset, restart, restyle, and
  suppress-on-first-page numbering per zone via
  `DocumentHeaderFooter.builder().numbering(...)`. `DocumentPageNumbering` carries
  `startAt` (printed value on the first counted page), `countFrom` (physical page
  where counting begins), `showOnFirstPage`, and a `DocumentPageNumberStyle`
  (`DECIMAL`, `LOWER_ROMAN`, `UPPER_ROMAN`, `LOWER_ALPHA`, `UPPER_ALPHA`) — e.g.
  lower-roman or alphabetic numbering, an uncounted cover, or an offset/restarted
  count (one style per zone; switching style mid-document — roman front matter then
  arabic body — is a per-section concern). Under
  an offset, `{pages}` expands to the counted total
  (`startAt + (totalPages - countFrom)`), not the physical page count. The default
  (`DocumentPageNumbering.DEFAULT`) is decimal, no offset, shown on every page, so
  existing header/footer output is byte-identical.

- **`LineBuilder.lineCap(DocumentLineCap)`** (`@since 1.9.0`). Lines gain the
  round / square end-caps `PathBuilder` already exposed. Pairing `ROUND` with a
  short dash draws a dotted line — `line.dashed(0.1, 4).lineCap(DocumentLineCap.ROUND)`
  renders round dots (the standard table-of-contents leader / separator style).
  The `BUTT` default emits no cap operator, so existing line output is
  byte-identical.

- **Content bleed: `DocumentBleed` / `DocumentEdge`** (`@since 1.9.0`). Flow
  builders gain `bleed(DocumentBleed)` and `bleedToEdge(DocumentEdge...)`, so a
  section's background fill extends to the trimmed physical page edge on the
  declared sides — a full-bleed masthead band or an edge-to-edge colour panel —
  while the section's children stay inside the content margin (text never runs
  off the page). It is the content-side twin of `PageBackgroundFill` and the
  intent-revealing replacement for the hand-computed negative-margin idiom,
  resolved against the active page margin at layout time. Nodes that do not bleed
  render byte-identically to before.

- **In-PDF navigation: anchors + internal links** (`@since 1.9.0`). Every flow
  and leaf builder gains `anchor(String)`, declaring a named destination at the
  element's top-left — `section.anchor("intro")`, `paragraph.anchor("fn-1")`, and
  the same on image / shape / ellipse / line / barcode / table builders. A link
  targets an anchor instead of a URI via `RichText.linkTo(text, anchor)` /
  `linkTo(text, style, anchor)`, `ParagraphBuilder.inlineLinkTo(text, anchor)` /
  `linkTo(anchor)`, and `linkTo(anchor)` on the leaf builders. Inline graphics
  inside a paragraph jump to anchors too via `RichText.imageLinkTo(...)` /
  `shapeLinkTo(...)` (and the matching `ParagraphBuilder.inlineImageLinkTo(...)` /
  `shapeLinkTo(...)`). Anchor resolution
  is deferred to the end of the render pass, so a link may target an anchor that
  appears later in the document (a forward reference). An unknown anchor renders
  as ordinary styled text (no annotation) and logs a warning; a link whose text
  wraps produces one annotation per line fragment; a duplicate anchor name keeps
  the last registration. Backends without in-document navigation (DOCX) render an
  internal link as plain text.
- **Unified `DocumentLinkTarget`** (`@since 1.9.0`). A new sealed
  `DocumentLinkTarget` — `ExternalLinkTarget` (wrapping `DocumentLinkOptions`)
  and `InternalLinkTarget` (an anchor name) — is now the link type carried
  through semantic nodes and resolved layout fragments. `DocumentLinkOptions` is
  unchanged and still accepted by every existing `link(DocumentLinkOptions)` and
  inline-link DSL method (wrapped into an `ExternalLinkTarget` automatically), so
  authoring code is source-compatible. The link accessor on the inline-run
  records (`InlineTextRun` / `InlineImageRun` / `InlineShapeRun`) is now
  `linkTarget()`; the former `linkOptions()` remains as a deprecated bridge that
  returns the external options (or `null` for an internal link).
- **Inline SVG-icon runs** (`@since 1.9.0`). A parsed `SvgIcon` can now sit on
  the text baseline inside a paragraph via `RichText.svgIcon(icon, size)` and
  `ParagraphBuilder.inlineSvgIcon(icon, size)` (with `alignment` / `baselineOffset` /
  link overloads, plus a clickable form). `size` is the glyph's height in points;
  the width follows the icon's aspect ratio. The icon is drawn as crisp vector
  layers carrying their own colours — gradients included — so it renders
  independently of the active font's glyph coverage. This is the engine path for
  vector colour emoji (e.g. a Twemoji SVG dropped inline) and small vector marks.
  A new sealed `InlineRun` variant (`InlineSvgRun`) joins text / image / shape;
  the inline render reuses the existing SVG paint pipeline (shared with the block
  path fragment), so flat-colour output stays byte-identical.
- **Inline highlight chips** (`@since 1.9.0`). An inline run can now sit on a
  rounded, padded background fill — the GitHub inline `code` look and inline
  status badges. `RichText.highlight(text, style, bg, radius, padding)` is the
  primitive; `code(text)` ships engine defaults (a monospace font, a muted ink
  and a light chip) and `chip(text, fg, bg)` colours a badge — with the matching
  `ParagraphBuilder.inlineHighlight` / `inlineCode` / `inlineChip`. A `highlight`
  overload takes `DocumentLinkOptions`, so a chip can also be a link. The fill is
  a new `InlineBackground(fill, cornerRadius, padding)` carried by a new sealed
  `InlineRun` variant, `InlineHighlightRun`; horizontal padding widens the run's
  advance, vertical padding overflows the line box without changing line metrics.
  A multi-word chip wraps with the surrounding line, painting one continuous
  rounded fill per visual-line fragment (its horizontal padding sits on the run's
  outer edges, so a wrapped fragment is open on the inner break). Text-only
  backends (DOCX) keep the text and drop the fill.
- **Colour emoji by shortcode** (`@since 1.9.0`). `RichText.emoji(":star:", size)`
  and `ParagraphBuilder.inlineEmoji(...)` resolve a GitHub-style shortcode to an inline
  vector colour glyph. Resolution is lenient — an unknown shortcode (or no emoji
  set on the classpath) is rendered as the literal text, the way GitHub treats an
  unrecognised `:code:`. The resolver is the new `EmojiLibrary`
  (`com.demcha.compose.document.emoji`): data-driven from the classpath layout
  `emoji/emoji-index.properties` (`shortcode=codepoint`) + `emoji/svg/<codepoint>.svg`,
  with `find(...)` (lenient `Optional`), `require(...)` (strict), `isAvailable()`
  and per-codepoint caching (a glyph using an SVG feature the parser rejects is
  treated as unresolved, so it falls back to text rather than failing the render).
  The glyphs ship in a new, independently-versioned **`graph-compose-emoji`**
  companion module (mirroring the `graph-compose-fonts` split): the engine carries
  no emoji art and has no Maven dependency on it. The module bundles the full
  **Noto Emoji** SVG set (~3.7k glyphs, SIL OFL 1.1) with a GitHub-style shortcode
  index (~1.6k shortcodes) generated from the gemoji database; both are rebuilt by
  `emoji/tools/build-emoji-set.py`.
- **SVG gradient import is now best-effort** (`@since 1.9.0`). `stop-opacity`
  (which has no opaque-PDF-shading analogue) is ignored — the gradient renders
  with opaque stops — and a focal radial (`fx` / `fy`) approximates as a plain
  radial about the centre, instead of failing the whole icon. This lets
  real-world artwork import (keeps gradient scenes like `:framed_picture:` /
  `:city_sunrise:` looking like scenes rather than flat blobs); fully-opaque
  gradients are unchanged, byte for byte.
- **SVG `clip-path` and `display:none` support** (`@since 1.9.0`). A
  `clip-path:url(#id)` (including the Adobe-Illustrator `<use>` + `clipPath`
  idiom, where the clipPath references a `<defs>` shape) is resolved to a clip
  region on each affected `SvgIcon.Layer` and honoured by the inline renderer, so
  glyphs that clip detail to a silhouette — hand gestures, body parts, the
  probing cane — render correctly instead of overflowing into halos. Hidden
  subtrees (`display:none`, e.g. an Illustrator guide layer of registration
  hatching) are skipped. Together these take the Noto Emoji set to essentially
  the whole bundled set rendering cleanly.
- **Same-colour translucent gradients are dropped, not painted opaque.** A
  gradient whose stops are all the same RGB with at least one translucent stop
  carries no colour — it is a pure alpha overlay (a soft shadow or edge
  highlight, e.g. the hair-edge darkening on the vampire glyphs). With no
  shading-alpha in the backend, painting it opaque covered the art beneath (the
  vampire's face rendered as a solid hair blob); such layers are now dropped.
  Multi-colour gradients (real scenes — `:framed_picture:`, `:sunrise:`,
  `:city_sunset:`) are structural and keep rendering as gradients.
- **Inline SVG icons are clipped to their viewBox.** Real-world icon art
  (notably Noto's working files) parks geometry outside the viewBox — a browser
  clips it to the viewBox, but the inline renderer was painting it, so an icon
  could smear copies of itself across adjacent glyphs (`:package:` rendered as
  several duplicated boxes overlapping its neighbours). The inline SVG render now
  clips each icon to its glyph box, matching SVG `viewBox` semantics.
- **Block SVG icons are clipped to their viewBox too.** The same off-canvas art
  bled past the box on the block path (`addSvgIcon(icon, w)` / `SvgIcon.node(w)`),
  which had no viewBox clip. A block icon's layer stack now clips its layers to
  the icon box: `LayerStackNode` gains an opt-in `clipToBounds` (`@since 1.9.0`,
  default off so existing stacks stay byte-identical) and `SvgIcon.node(...)`
  sets it. It reuses the `ShapeContainer` clip pipeline — one paired
  begin/end marker per icon — so it matches the inline fix above. The same
  flag is exposed to the DSL as `LayerStackBuilder.clipToBounds()` — the
  `overflow: hidden` of a stacking box for any layer stack.
- **Render a document straight to images** (`@since 1.9.0`). `DocumentSession`
  gains `toImages(int dpi)` → `List<BufferedImage>` (one per page) and
  `toImage(int pageIndex, int dpi)` → `BufferedImage`, plus `transparent`
  overloads (`toImages(dpi, transparent)` / `toImage(pageIndex, dpi, transparent)`)
  that return ARGB instead of opaque white. These rasterize the in-memory document
  directly, skipping the previous `toPdfBytes()` → reparse round-trip needed to get
  a preview or thumbnail. The return type is the JDK `java.awt.image.BufferedImage`,
  so the public surface stays renderer-agnostic; the PDFBox `PDFRenderer` call lives
  in the PDF backend. `PdfVisualRegression` also gains direct `renderPages(session)` /
  `assertMatchesBaseline(name, session)` overloads on the same path.

### Deprecations

- **`templates.api.CoverLetterTemplate`** marked `@Deprecated(forRemoval = true)`.
  Nothing implements it — the layered cover-letter presets implement the generic
  `DocumentTemplate<CoverLetterDocumentSpec>` seam instead. Removed in 2.0.
- **`cv.v2.components.HeadlineRenderer` / `ContactRenderer` / `BannerRenderer`**
  (already-deprecated pre-widgets shims) are now `forRemoval` — use the
  `cv.v2.widgets` `Headline` / `ContactLine` / `SectionHeader` widgets instead.
  Removed in 2.0.

### Documentation

- New runnable flagship example
  `examples/src/main/java/com/demcha/examples/features/title/BookTemplateExample.java`
  — a full novel front (full-bleed wave cover, clickable dotted-leader table of
  contents with live page numbers, chapters) assembled in one `DocumentSession`
  using the v1.9 book primitives, with no external PDF merge or two-pass probe.
- New runnable example
  `examples/src/main/java/com/demcha/examples/features/navigation/InPdfNavigationExample.java`
  — a clickable table of contents plus a bidirectional footnote.
- New runnable example
  `examples/src/main/java/com/demcha/examples/features/text/InlineSvgIconExample.java`
  — multi-colour vector glyphs (gold star, green check badge, violet gradient
  orb, info / warning marks) flowing inline with text, at several sizes.
- New `graph-compose-emoji` module bundling the Noto Emoji SVG set (OFL 1.1) with
  `emoji/OFL.txt`, `emoji/NOTICE.md` and the `emoji/tools/build-emoji-set.py`
  generator that rebuilds the glyphs + shortcode index from noto-emoji + gemoji.
- New runnable example
  `examples/src/main/java/com/demcha/examples/features/text/EmojiShortcodeExample.java`
  — `:shortcode:` colour emoji flowing inline with text, the starter-set legend,
  the unknown-shortcode text fallback, and several glyph sizes.
- New runnable example
  `examples/src/main/java/com/demcha/examples/features/text/EmojiSvgVsPngExample.java`
  — a `Shortcode | SVG (vector) | PNG (raster)` comparison table, drawing each
  starter glyph down both inline paths (`RichText.svgIcon` vs `RichText.image`).
- New runnable example
  `examples/src/main/java/com/demcha/examples/features/text/EmojiGalleryExample.java`
  — a paginated catalogue of the entire bundled emoji set (every indexed glyph,
  drawn inline).

### Build

- **The README hero banner is now version-stamped and re-rendered on release.**
  `EngineDeckExample` reads its version and codename from a filtered
  `banner.properties` (`@project.version@`) instead of hardcoded constants, and
  the new `ReadmeBannerRenderer` writes
  `assets/readme/repository_showcase_render.png` straight from the engine via
  `DocumentSession.toImage(...)` — no PDF-rasterize round-trip.
  `cut-release.ps1` re-renders and stages the hero on every tag, and
  `VersionConsistencyGuardTest` fails the build if the banner version is ever
  hardcoded again.

### Tests

- `InternalLinkAnchorTest` (PDFBox assertions): forward and backward references
  resolve to `GoTo`; an unknown anchor produces no annotation and no crash; the
  destination points at the correct page across a page break; a wrapped link
  emits an annotation per line fragment; external links still emit `URI`; a
  section anchor and a shape internal link are both navigable; a duplicate anchor
  keeps the last registration; plus a visual artifact write.
- `InlineSvgRunTest` (run validation: null icon, non-finite / non-positive
  dimensions, alignment default, external-link wrapping) and `InlineSvgRenderTest`
  (PDFBox end-to-end: text preserved with no glyph substitution, the icon's fill
  colour and an inline gradient both rasterize onto the page, a linked icon emits
  a clickable annotation, and `svgIcon` sizes by aspect ratio). `InlineSvgRenderTest`
  also rasterizes off-canvas geometry to prove the inline glyph-box clip, and the
  new `BlockSvgRenderTest` does the same for the block path — off-canvas art does
  not bleed, in-box art still paints, the layer stack emits a balanced
  `CLIP_BOUNDS` begin/end pair, and a plain (non-icon) stack emits none.
- `EmojiLibraryTest` (resolves shortcodes case-insensitively with/without colons,
  unknown → empty, `require` throws, an absent set reports unavailable and names
  the `graph-compose-emoji` artifact) and `EmojiRenderTest` (a known shortcode
  rasterizes a colour glyph, a gradient emoji paints its shading, an unknown
  shortcode falls back to literal text, and `RichText.emoji` yields an
  `InlineSvgRun` or a text run accordingly).
- `DocumentSessionImageTest` (direct render-to-image): `toImages(dpi)` returns one
  image per page sized to the page at that DPI; dimensions scale with DPI; rendered
  pages contain painted (non-background) pixels; `transparent` yields an ARGB image
  with a fully-transparent margin while the default is opaque RGB; `toImage(pageIndex,
  dpi)` returns the requested page and is pixel-identical to the matching `toImages`
  entry; a post-processed watermark also lands in the raster; the direct render is
  pixel-identical to the `toPdfBytes()` round-trip (`PdfVisualRegression` / `ImageDiff`,
  budget 0); and `dpi <= 0`, an out-of-range page, and an empty document are rejected.

## v1.8.0 — 2026-06-18

Codenamed **"illustrative"**. Native vector charts (bar / line / pie, inline
sparklines, monotone & smooth interpolation), SVG path & icon import with native
gradients, free-form `ShapeOutline.Path` clipping, the `keepTogether()`
pagination control, and a leaner publication — the bundled Google fonts split
into the independently-versioned `graph-compose-fonts` artifact. Core document
APIs stay source- and binary-compatible with v1.7; the two consumption changes
are the fonts split and the removal of `ConfigLoader` (both detailed below).

### Public API

- **Line-chart interpolation modes** (`@since 1.8.0`). New
  `LineInterpolation` enum selects how a line series connects its points:
  `LINEAR` (straight, exact), `SMOOTH` (the existing pretty Catmull-Rom
  curve, which may overshoot local extremes on sharp swings), and the new
  `MONOTONE` (Fritsch-Carlson) — a curve that looks just as smooth but is
  constrained to never overshoot, staying within the value range of the
  points it spans, for an accurate yet smooth reading of the data. Set it
  with `ChartSpec.line().interpolation(LineInterpolation.MONOTONE)` — the
  single, explicit knob for line shape. All three render through the same
  native PDF curve operators with zero tessellation, so geometry stays
  deterministic and the hot path is unchanged.
- **`ChartData.Series` rejects non-finite values.** A `NaN` / ±∞ entry now
  fails at construction — naming the series and the offending index —
  instead of poisoning axis derivation and surfacing as a misleading
  "height must be finite" failure deep in the layout pass. `null` entries
  are still allowed as gaps.
- **Block-level horizontal alignment** (`@since 1.8.0`). Fixed-size flow
  children (paths, images, SVG icons, barcodes, shape containers) left-align
  by default — there was no built-in way to centre or right-align one without
  wrapping it in a full-width container and hand-computing the content width.
  New `AlignNode` + `HorizontalAlign` (LEFT / CENTER / RIGHT) seat any node
  across the available width: `flow.addAligned(HorizontalAlign.CENTER, node)`
  and the icon sugar `flow.addSvgIcon(icon, width, HorizontalAlign.CENTER)`.
  The wrapper fills the width and reuses the stack placement engine (one
  anchor), so there is no new render handler and no hot-path change.
- **Native vector charts** (`@since 1.8.0`). New `com.demcha.compose.document.chart`
  package with a layered, serialization-friendly API: `ChartData` (categories +
  series, type/colour-agnostic), sealed `ChartSpec` (`bar()` / `line()` with
  axis, legend, value-label, and sizing knobs), `ChartStyle` (nullable-field
  cascade merged over `ChartTheme` tokens, per-series paint overrides), and
  `DocumentPaint` (solid, linear, and radial — see the gradient entry below).
  Charts compile at layout time into existing primitives
  (shapes, lines, paragraphs) via `ChartDefinition` — no new render handlers,
  deterministic geometry, covered by the standard snapshot machinery; any
  fixed-layout backend renders charts with no chart-specific code, while the
  semantic DOCX export (which has no layout pass) falls back to the chart's
  categories-by-series data table with a one-time capability warning. DSL:
  `section.chart(spec)` / `chart(spec, style)`. Declarative `NumberFormatSpec`
  keeps specs JSON-serializable. The one unsupported combination
  (`ValueLabelMode.INSIDE`) fails fast with `UnsupportedOperationException`
  instead of rendering silently wrong.
- **Horizontal bars, smooth lines, area fills, stacked totals, legend
  placement.** `ChartSpec.bar().horizontal(true)` transposes the chart
  (categories on Y in reading order, value axis on X, labels at bar ends);
  stacked bars label the category total. `ChartSpec.line().smooth(true)`
  draws deterministic Catmull-Rom curves as **native cubic Béziers** through
  the vector path primitive — one `PathNode` per run, perfectly smooth at
  any zoom level, zero tessellation; `.area(true)` fills each series down to
  the baseline with a translucent series colour (`ChartStyle.areaOpacity`,
  default 0.35) — alpha-blended fills layer legibly, and in smooth mode the
  fill closes the exact stroke curve so fill and stroke edges coincide. `LegendPosition.TOP` and `RIGHT` now lay out as a top
  strip / right column for every chart kind, including pie. The chart
  resolver is split per kind (`BarChartLayout` / `LineChartLayout` /
  `PieChartLayout` over a shared `ChartLayoutSupport`).
- **Axis / grid / label visibility toggles.** `AxisSpec.showTickLabels(false)`
  hides the numeric axis and collapses its gutter; `showGridLines(false)` and
  `ChartStyle.GridStyle` control horizontal/vertical grid lines;
  `ChartSpec.bar()/line().showCategoryLabels(false)` hides the category axis —
  down to a minimal "bars + value numbers only" chart.
- **Pie / donut charts** (`@since 1.8.0`). `ChartSpec.pie()` — one slice per
  category from a single series (multi-series data is rejected loudly).
  Configurable: `donutRatio` (hole size), `startAngleDegrees`, `clockwise`,
  `SliceLabelMode` (VALUE / PERCENT / CATEGORY / CATEGORY_PERCENT) with
  independent value/percent formats, donut-centre KPI text, and a
  category-listing legend. Style cascade adds `sliceStroke` (separator),
  `sliceGapDegrees` (pad angle), and `donutCenterTextStyle`. Sectors compile
  into the new general-purpose `PolygonNode` (arc-tessellated ring polygons at
  a fixed 3° step — deterministic vertices, no new render handlers), which also
  lays the groundwork for SVG icon-path import.
- **Vector path primitive** (`@since 1.8.0`). New `PathNode` — the open-path,
  curve-capable sibling of `PolygonNode`: normalized `DocumentPathSegment`s
  (`moveTo` / `lineTo` / cubic `cubicTo` / `close`; Bézier control points are
  free to overshoot the unit box) are scaled to the node's box and rendered
  with native PDF curve operators, so curves stay perfectly smooth at any
  zoom level instead of being tessellated into straight pieces. Atomic
  pagination, deterministic layout snapshots, fill (non-zero winding rule)
  and/or stroke. This is the leaf vehicle for smooth chart lines, decorative
  design shapes, and future SVG path import. DSL:
  `addPath(p -> p.moveTo(...).curveTo(...).closePath().fillColor(...))` on
  every flow builder authors design shapes directly, and
  `dashed(on, off, ...)` makes the stroke dashed with the same
  `DocumentDashPattern` contract as lines — the pattern follows the curve.
- **Path-outline clipper** (`@since 1.8.0`). `ShapeOutline.Path` joins the
  sealed outline family as the curve-capable sibling of `Polygon`, so a
  shape container can clip its children to — and fill / stroke along — an
  arbitrary native-curve silhouette. `ShapeContainerBuilder.path(w, h,
  segments)` takes raw `DocumentPathSegment`s; `path(w, h, svgPath)` (beta)
  clips to an imported SVG path, turning any icon or logo into a content
  mask under `ClipPolicy.CLIP_PATH`. The outline rides the existing
  vector-path fragment pipeline (one source of truth for native curves) and
  the clip handler emits the same `addPathSegments` geometry, so fill, clip,
  and `addPath(...)` all agree. The new `Path` permit is additive and keeps
  the artifact binary-compatible (the `japicmp` gate stays green); only
  consumer code that exhaustively `switch`es over `ShapeOutline` would need a
  new branch, and the canonical authoring surface exposes no such switch.
- **SVG path import** (`@since 1.8.0`, **beta** — annotated `@Beta` while
  the surface hardens against real-world exporter output). `SvgPath.parse(d)` /
  `parse(d, viewBox...)` in the new `document.svg` package lowers the full
  SVG 1.1 path grammar — absolute/relative `M L H V C S Q T A Z`, implicit
  repetition, quadratics (exact cubic elevation), smooth shorthands, and
  elliptical arcs (deterministic W3C endpoint-to-center conversion, ≤90°
  cubic slices) — into normalized, y-flipped `DocumentPathSegment`s.
  `PathBuilder.svg(svgPath)` drops the result straight into `addPath(...)`:
  any icon's `d` string renders as native PDF curves, no tessellation.
  Syntax errors report the character position; fills keep SVG's default
  non-zero winding rule. On top of it, `SvgIcon.read(file)` / `parse(xml)`
  reads the practical subset of a whole SVG file — every `<path>` plus
  `rect` / `circle` / `ellipse` / `line` / `polyline` / `polygon` lowered to
  path data, `<g>` nesting with `translate` / `scale` / `rotate` / `matrix`
  transforms (affine maps are exact on Bézier control points), and
  `fill` / `stroke` / `stroke-width` styling with SVG inheritance and
  defaults — into ordered layers, and `addSvgIcon(icon, width)` stacks them
  back-to-front on the page. `SvgIcon#node(width)` packages the same layers
  as one ready-to-place node whose box is exactly the icon box, so it
  anchors true inside `ShapeContainer` / `LayerStack` nine-point grids (and
  rows now accept `ShapeContainerNode` children directly — it is the same
  atomic overlay composite as the already-allowed `LayerStackNode`).
  **Gradients render natively**: `linearGradient` / `radialGradient`
  referenced via `url(#id)` — on fills *and strokes* — map to PDF axial /
  radial shadings with exact endpoints (`userSpaceOnUse` and
  `objectBoundingBox` units, `gradientTransform`, percentage offsets,
  multi-stop stitching, one `href` hop for split definitions); gradient
  strokes ride a shading-pattern stroking colour. Underneath,
  `DocumentPaint` gains endpoint-exact `LinearAxis` / `RadialCircle` forms
  and `PathNode` / `PathBuilder` grow `fill(paint)` / `strokePaint(paint)`
  with solid paints normalising to the flat-colour path (byte-identical
  output for non-gradient documents). **Stroke fidelity**: the reader honours
  `stroke-linecap` / `stroke-linejoin` (rendered as native PDF `J` / `j`
  operators via new `DocumentLineCap` / `DocumentLineJoin`, also on
  `PathBuilder.lineCap()` / `lineJoin()`) and `stroke-dasharray`, the full
  CSS named-colour table (147 keywords), `rgb()` / `rgba()` with numbers or
  percentages, `#rgb` / `#rgba` / `#rrggbb` / `#rrggbbaa` hex, and absolute
  length units (`px` / `pt` / `pc` / `in` / `mm` / `cm`) on stroke widths;
  relative units and unknown colours fail with the supported alternatives
  listed. `SvgIcon#node(width)` now scales stroke widths and dash lengths
  with the geometry (they live in user units), so an icon drawn smaller than
  its source no longer renders an over-thick outline. Content the reader
  can't render (`text`, `image`, `use`, masks, clips, filters) is dropped
  with a single deduplicated warn-log per kind instead of silently, and the
  DOCX backend warns once per geometry-only node kind (`path`, `polygon`,
  `shape`, …) it drops. The XML reader refuses DOCTYPEs (no XXE); CSS
  stylesheets, text, filters, focal radials, non-pad `spreadMethod` and
  translucent gradient stops stay deliberately out of scope — the reader
  fails loudly rather than rendering them wrong. **Every reader error names
  the offending element and why**: an unsupported colour / transform /
  gradient / unit is reported as `in <circle fill="…" …>: <reason — and the
  supported set>`, pinpointing the deepest failing element (not its wrapping
  `<g>`); a blank result explains itself (`no drawable geometry — skipped
  text; this reader renders vector shapes only`) instead of a bare "no
  geometry".
- **Inline sparklines** (`@since 1.8.0`). `RichText.sparkline(w, h, color,
  values...)` draws a filled mini-area silhouette on the text baseline, and
  `sparklineLine(w, h, thickness, color, values...)` a constant-thickness line
  band (full thickness preserved at the peaks). Both runs are smoothed with
  the same Catmull-Rom curve the chart engine uses (densified to 12
  sub-segments per span — facets stay under half a point at sparkline
  sizes), and both compile into the existing inline-shape polygon run — a KPI trend next to a number, a skill trajectory
  inside a CV line.
- **Configurable line-chart point markers.** `PointMarker` draws an ellipse at
  every data point — independent width/height axes, explicit fill (or the
  series paint), and an optional outline ring (`PointMarker.circle(5)
  .withStroke(...)`) that keeps joints legible where lines meet; markers always
  render above all line strokes. Per-point value labels sit at a configurable
  `ChartStyle.valueLabelOffset(...)` from the marker (or bar top) in the
  cascading `valueLabelTextStyle`, draw above strokes and markers behind a
  configurable halo chip (`ChartStyle.valueLabelHalo(...)`, themed white) so
  digits stay legible where lines cross them, and deterministically flip below
  their point when two series' labels would collide at the same category.
- **Gradient fills** (`@since 1.8.0`). `DocumentPaint` graduates to
  `com.demcha.compose.document.style` as the shared paint vocabulary, and
  gradients now actually render: `ShapeNode` gains an optional `fillPaint`
  (`ShapeBuilder.fill(paint)`) that wins over `fillColor`. The PDF backend
  paints `DocumentPaint.linear` as a native axial shading (0° = left→right,
  90° = bottom→top; two stops exponential, more stops stitched) and
  `DocumentPaint.radial` as a radial shading reaching the farthest corner,
  clipped to the shape path — rounded corners included. Chart bars now carry
  their full series paint, so a gradient palette renders as gradients instead
  of degrading to the first stop. Solid paints normalise to the plain
  fill-colour path, keeping existing documents byte-identical; backends
  without shading support fall back to `primaryColor()` by contract. The
  flagship `BusinessReportExample` hero is now fully vector — gradient-sky
  shape plus polygon mountain ranges replace the last Graphics2D raster.
- **Translucent shape colours** (`@since 1.8.0`). `DocumentColor.rgba(r, g, b, a)`
  and `withOpacity(0..1)`: the PDF backend honours the alpha channel on shape
  fills and strokes (rectangles/panels/bars, chart value-label halos, ellipse
  point markers, polygons, inline shapes) via a graphics-state alpha constant —
  e.g. a semi-transparent chart halo lets crossing lines show through faintly.
  Fully opaque colours emit no graphics-state entry, so existing documents stay
  byte-identical. Text/lines and the DOCX backend still render opaquely.
- **`keepTogether()` pagination control** (`@since 1.8.0`). Opt-in flag on
  `SectionBuilder`, `ModuleBuilder`, and `TimelineBuilder` (plus
  `keepEntriesTogether()` for per-entry timeline integrity): a block that does
  not fit in the remaining page space relocates whole to the next page instead
  of orphaning its heading from the content below. Blocks taller than a page
  still flow. Default off — existing layouts are byte-identical.
- **Removed: `ConfigLoader`** (breaking). The `com.demcha.compose.ConfigLoader`
  YAML/JSON config-file helper was an application-bootstrap utility with no
  connection to document rendering — nothing in the library, tests, or
  examples referenced it. Gone with it: the `<optional>`
  `jackson-dataformat-yaml` dependency (ConfigLoader was its only consumer)
  and the YAML entry in the `NoClassDefFoundError` troubleshooting section.
  Consumers who relied on the helper can copy the former ~100-line class into
  their own codebase or load configs directly with Jackson
  (`new ObjectMapper(new YAMLFactory()).readValue(...)`).
- **Debug node labels** (`@since 1.8.0`). The debug overlay grew a second
  layer: backend-neutral `DocumentDebugOptions` (guides + node labels +
  label-text mode, in `document.output` next to the other neutral output
  options) configures fixed-layout rendering via
  `GraphCompose.document(...).debug(...)`, `DocumentSession.debug(...)`, or
  `PdfFixedLayoutBackend.builder().debug(...)`. With `nodeLabels()` enabled,
  every rendered node prints its stable semantic path — the same path
  `layoutSnapshot()` reports — once per node and page, as a small corner
  badge straddling the top edge of the node's bounds (right-aligned 5pt
  Helvetica on a pale halo), so a misplaced block on the sheet reads straight
  back to the builder call that authored it. Labels paint as a single
  deterministic post-pass after all content, so badges always sit on top —
  a container's children or a higher layer can never overdraw the label that
  annotates them. `LabelText.NAME` (default) prints the compact own segment
  (`PriceSummaryTitle[0]`); `FULL_PATH` prints the whole ancestry. Label text
  degrades through the shared WinAnsi fallback (accents like `é` survive,
  anything outside WinAnsi becomes `?` with a `glyph.missing` log). The
  overlay draws strictly on top of content and never touches measurement or
  pagination. `guideLines(boolean)` everywhere became sugar over the options
  object with uniform last-write-wins semantics on all three surfaces —
  node-label settings survive the toggle, `debug(none())` reliably disables
  everything — and disabled debug output stays byte-identical.

### Build & distribution

- **Bundled Google fonts moved to a separate, independently-versioned
  artifact** (`io.github.demchaav:graph-compose-fonts`). **Breaking for
  consumers who use the bundled families.** The ~18 MB of curated Google fonts
  no longer ship inside the `graph-compose` jar, so an engine upgrade never
  re-downloads them and the engine artifact drops from ~40 MB to a few MB. The
  public `FontName` constants and the `DefaultFonts` catalog are unchanged
  (source- and binary-compatible), and the classpath layout `fonts/google/...`
  is preserved byte-for-byte. To keep the bundled fonts, add
  `io.github.demchaav:graph-compose-fonts` (its own version line, starting at
  `1.0.0`) to your build, or depend on the new "batteries-included"
  `io.github.demchaav:graph-compose-bundle` (engine + fonts at compatible
  versions). With neither on the classpath, standard-14 documents render
  unchanged and requesting a bundled family fails fast with a message that
  names the missing dependency. See
  [docs/migration/v1.8.0-fonts.md](docs/migration/v1.8.0-fonts.md).
- **Leaner Maven Central publication.** The release build no longer attaches or
  uploads the `-tests` classifier jar (it stays a local-only build aid for the
  benchmarks module), and with the fonts gone the `-sources.jar` no longer
  carries font binaries either. The published artifact set is now just the
  engine bytecode plus the small template assets.
- **`graph-compose-fonts` releases on its own `fonts-v*` tag** via a dedicated
  publish workflow, so the font set ships only when it actually changes,
  independent of the engine's `v*` release cadence.

### Bug fixes

- **A stray non-drawing element no longer breaks a whole SVG icon.** A
  visible-painted SVG element that lowers to a moveto-only or moveto+close
  path — `d="M12 12"`, a zero-length arc, the stray subpaths real exporters
  emit — drew no ink, yet a lone moveto threw at `SvgIcon#node(...)` (an empty
  `PathNode`) and a moveto+close rendered blank. `SvgIconReader` now drops a
  layer with no drawing segment, so one degenerate element no longer fails the
  icon; an icon of only such elements still fails loudly with "no drawable
  geometry".
- **Stacked bars anchor at zero even with an explicit positive axis minimum.**
  A stacked bar chart with `valueAxis().min(positive)` lifted the baseline
  while segment heights stayed measured from zero, so the stack overshot its
  total and ran past the plot top. The stacked floor is now pinned to zero
  (parts summing to a whole), independent of the requested minimum. Grouped
  bars still honour an explicit minimum.
- **Grouped bars emanate from the zero baseline.** A grouped (non-stacked) bar
  measured its height from the axis nice-floor, so on an axis that crossed zero
  a negative value rendered as a short upward column anchored at the floor —
  visually indistinguishable from a small positive value — and positive bars
  overshot below zero. Grouped bars now grow from the zero line (positive up,
  negative hanging below it), matching the standard bar-chart convention and
  the stacked-bar behaviour. When zero is off-scale — an explicit non-zero
  `valueAxis().min(...)` or `baselineAtZero(false)` over a range that excludes
  zero — the baseline clamps to the nearest visible bound, so a deliberately
  zoomed axis still anchors its bars at the plot floor. Charts with positive
  data on a zero-based axis are byte-identical.
- **`ChartStyle.paintForSeries` rejects a negative series index** with a
  value-naming `IllegalArgumentException` instead of leaking a bare
  `IndexOutOfBoundsException` from the palette modulo.
- **A translucent gradient stop is rejected instead of silently rendering
  opaque.** Gradients render through PDF axial / radial shadings, which carry
  no alpha channel, so `PdfShadingSupport` dropped a stop colour's alpha and a
  translucent stop rendered fully opaque with no diagnostic. `DocumentPaint.Stop`
  now rejects a colour with alpha below 255 at construction, naming the offending
  alpha — flatten the transparency into the stop colour, or apply opacity to the
  whole shape. This matches the SVG reader, which already refuses `stop-opacity`,
  and reaches the `DocumentPaint.linear(from, to)` sugar too. Opaque gradients are
  unaffected.
- **SVG path reader no longer hangs on malformed `d` data.** A `Z`/`z`
  close command (which consumes no operands) followed by a stray
  non-command token — e.g. `"M0 0 Z5"` — made the scanner loop forever,
  appending a close op every pass until the heap was exhausted. A single
  malformed or hostile path string could therefore DoS the `@Beta`
  `SvgPath.parse` / `SvgIcon` reader. The scanner now fails fast with the
  usual position-carrying `IllegalArgumentException` when an iteration
  consumes neither a command nor an operand.
- **`BEHIND_CONTENT` watermarks no longer wash out the page.** The PDF
  watermark renderer set its low-opacity graphics state in a *prepended*
  content stream without a save/restore pair; PDFBox's `resetContext` only
  isolates appended streams, so the watermark alpha leaked into the entire
  page and every element rendered nearly invisible. The watermark now wraps
  its drawing in `q`/`Q`, keeping page content at full strength. This
  affected every document using the default `DocumentWatermark` layer.
- **DOCX export no longer drops lists.** `DocxSemanticBackend` had no branch
  for `ListNode`, so `addList(...)` content silently vanished from Word
  exports. Lists now map to marker-prefixed paragraphs in the list's text
  style, with nested items indented per depth and keeping their own markers.
  (Found by the recipe fact-check: the docx-export recipe's "what is skipped"
  list could not honestly be written without it.)
- **DOCX list items no longer double-space after the marker.** The new list
  branch concatenated `ListMarker.value()` — which already carries its
  trailing space — with another literal space, so every exported item read
  `"•  text"`, and markerless lists gained a stray leading space. The export
  now uses `ListMarker.prefix()`, matching the fixed-layout text pipeline.
- **DOCX list export fully matches the PDF list pipeline.** The semantic Word
  backend resolved nested-item marker fallbacks against the flat-list marker
  and skipped flat-item normalization, so the two outputs of one session
  disagreed: a nested item without an explicit marker exported as the list
  bullet where the PDF renders the depth cascade (`•` → `◦` → `▪` → `·`),
  an author-typed `"- item"` doubled up as `"• - item"`, and blank items
  produced marker-only paragraphs. Both rules now live in one shared place —
  `ListMarker.defaultForDepth(int)` and
  `ListMarker.normalizeItemText(String, boolean)` (`@since 1.8.0`) — and the
  fixed-layout pipeline and the DOCX export both call them.
- **SVG gradient number errors read in the reader's house style.** A
  non-numeric gradient coordinate, radius, or stop value (e.g. `x1="abc"`,
  `r="x%"`, `offset="?"`) leaked the raw JDK `NumberFormatException`
  ("For input string: …") as the reason. `SvgGradients` now parses through one
  shared helper that throws `"<field> must be a number, got '…'"` with the
  cause chained — matching the rest of the beta SVG reader, where the
  per-element wrapper already names the referencing element.

### Documentation

- **Contract-drift Javadoc fixes on the new 1.8 surface.** `LegendPosition`
  no longer claims `RIGHT`/`TOP` are "reserved and rejected by validation" —
  all four placements are laid out for every chart kind, as the resolver and
  its tests already prove. `DocumentPaint` documents why the `Linear`/`Radial`
  (angle/corner-reaching) and `LinearAxis`/`RadialCircle` (exact endpoint/radius)
  forms coexist. `ShapeContainerBuilder`'s missing-outline error and class
  Javadoc now name the full set of outline setters (including `path`).
  `PathBuilder.dashed(double...)` documents the `IllegalArgumentException` it
  throws eagerly, and `SvgIcon` documents that a gradient `href` inherits stops
  only, not geometry attributes.
- **Browsable feature-catalog PDF.** New flagship `FeatureCatalogExample`
  renders every shipped capability as a self-documenting block: the heading
  lands in the PDF outline (the bookmarks panel works as a clickable index),
  a code panel shows the exact API call, and the live result renders right
  under it — rich text, sparklines, nested lists, timelines, tables, every
  chart kind, images (COVER vs CONTAIN fit), gradients, translucency,
  polygons, vector paths (solid and dashed native Béziers), SVG path import
  and a beta `SvgIcon` tile row, shape basics (dividers, ellipses, soft
  cards), clipped containers, canvas, transforms, barcodes, the
  debug-overlay switch, and the document's own chrome — 23 blocks across
  7 pages. Blocks use `keepTogether()`, so a snippet is never orphaned
  from its result.
- **Landscape capability deck on real benchmark data.** New flagship
  `EngineDeckExample` renders GraphCompose about itself: a full-page banner
  (DSL code → engine grid → output backends → real rendered-document
  thumbnails), an authoring-pipeline page, and two pages that load the
  repository's comparative benchmark result file and draw the table and charts
  (GraphCompose vs iText 9 vs JasperReports) straight from it. Content lives in
  an `EngineDeckData` data layer; an `EngineDeckLayoutSnapshotTest` locks the
  layout.
- **Recipe coverage is complete.** Nine new cookbook pages close every gap the
  recipe index tracked: rich text, lists, timelines, barcodes, images,
  PDF chrome (metadata / watermark / running header-footer / protection /
  links / bookmarks), translucency, semantic DOCX export, and layout-snapshot
  regression testing. Every snippet is verified against the current API;
  the folder index (`docs/recipes/README.md`) no longer carries a
  "not yet covered" list.
- **Word-export example.** New `WordExportExample`
  (`examples/features/docx`) renders the same `DocumentSession` as a
  fixed-layout PDF *and* an editable Word file via `DocxSemanticBackend`,
  one section per capability-table row: inline runs, nested lists with
  custom markers, tables, side-by-side rows, an embedded image, a page
  break, the chart→data-table fallback, and the geometry that stays
  PDF-only. Committed previews live under `assets/readme/examples/`
  (`word-export-companion.pdf` / `.docx`); the examples module adds the
  optional `poi-ooxml` dependency exactly like a consuming project would.
- **`BusinessReportExample` chart is now a native vector chart.** The flagship
  report's five-quarter Revenue/Profit block previously rasterised a bar chart
  through Graphics2D into an embedded PNG; it now uses `ChartSpec.bar()` with a
  `ChartStyle` palette override (navy/gold) and an explicit 0–100 axis —
  ~90 lines of hand-drawn AWT geometry replaced by a declarative spec.
- **Chart showcase contrasts SMOOTH vs MONOTONE.** `ChartShowcaseExample`
  gains a paired before/after on a volatile series — the pretty Catmull-Rom
  curve overshooting its peaks next to the monotone curve that stays within
  the data range — and the committed `assets/readme/chart-showcase.png` hero
  preview now shows that comparison.

### Internal

- **CV / cover-letter template icons moved from PNG to recolorable SVG.**
  The bundled contact / social glyphs (phone, email, location, website,
  LinkedIn, GitHub, …) and the sidebar-portrait avatar now ship as SVG
  instead of raster PNG. A new internal `SvgGlyph` helper flattens an icon's
  filled layers into one outline that the presets fill with each template's
  own accent colour via `rich.shape(...)` — so one bundled glyph recolours
  per template with no per-template copies, and the icons stay crisp at any
  zoom. The sidebar-portrait avatar is a swappable SVG placeholder. This
  shrinks the bundled `templates/cv` assets from ~717 KB to ~133 KB (the
  431 KB `portrait.png` alone becomes a ~4 KB SVG), trimming the published
  jar. No public API change; the CV / cover-letter presets render the same
  layout (visual baselines refreshed for the new glyphs; the sidebar-portrait
  layout snapshot updated for the vector avatar).
- **Benchmark suite cleanup (not shipped).** Removed three redundant
  benchmark mains: `FullCvBenchmark` (superseded by the JMH
  `TemplateCvJmhBenchmark`), `GraphComposeBenchmark` (early-engine relic
  duplicating `CurrentSpeedBenchmark`'s `engine-simple` scenario), and
  `ScalabilityBenchmark` (its thread-scaling sweep folded into
  `CurrentSpeedBenchmark`'s full-profile throughput run, now `1,2,4,8,16`).
  Dropped the matching `run-benchmarks.ps1` steps and doc entries.
- **Feature-object benchmarks for the v1.8 vector surface (not shipped).**
  The suite previously exercised only text/table primitives. Added JMH render
  benches and deterministic probes over the new vector features:
  `SvgJmhBenchmark` (path parse / whole-file icon read / icon→node) plus a
  `SvgParseAllocProbe`; `ChartJmhBenchmark` (bar + line + pie render) plus a
  `ChartAllocProbe` (layout-compile allocation); `VectorRenderOperatorProbe`
  (the same paths drawn flat vs. gradient vs. translucent, counted as PDF
  content-stream operators); `IconRampJmhBenchmark` (icon-placement scaling,
  `@Param` 8/32/128); and `MixedShowcaseJmhBenchmark` (one document combining
  prose, inline sparklines, bar + pie charts, SVG icons and a gradient path).
  Shared `SvgBenchmarkFixtures` / `ChartBenchmarkFixtures` hold the inputs so
  each bench and its probe measure identical data.
- **Current-speed report carries a stage breakdown and a run summary (not
  shipped).** `CurrentSpeedBenchmark` persists a per-scenario compose / layout /
  render split (`stages[]`, median ms) to the JSON and a `stages` CSV, and
  writes a readable `summary.md`. `BenchmarkDiffTool` consumes `stages[]`,
  prints a per-stage delta table, and reports the scenarios added/removed
  between two runs.
- **Every current-speed scenario is now covered by the smoke perf gate (not
  shipped).** The `long-token` scenario previously had no SMOKE threshold and
  silently escaped the gate; it now has one, and `CurrentSpeedScenarioGateTest`
  fails the build if any scenario lacks a threshold.
- **Benchmark coverage for the render hot paths (not shipped).** Added an image
  embed/scale gate (`ImageCacheOperatorProbe` + `ImageBenchmarkFixtures` +
  `ImageJmhBenchmark`, with `ImageCacheGateTest` pinning `PdfImageCache` reuse), a
  single-shot cold-start render bench (`ColdStartJmhBenchmark`), a report-scaling
  sweep in `ComparativeBenchmark` (equivalent content across GraphCompose /
  iText 9 / JasperReports at 40 / 200 / 1000 table rows — iText upgraded from the
  EOL 5.5.x to current 9.x — printing a per-size GraphCompose-advantage ratio plus
  a post-run sample-PDF dump per library/size), a
  production-scale `LargeTableJmhBenchmark`, an allocation-rate / GC-pressure probe
  (`AllocationRateProbe`), and an accented-Latin measurement scenario.
- **Deterministic benchmark gates run on every PR (not shipped).** The benchmarks
  module's tests never ran in CI; the `perf-smoke` job now runs them, so the
  image-cache, render-operator (F5 coalescing), vector-paint (flat / gradient /
  alpha / stroked / dashed operator structure), and scenario-coverage gates fail a
  PR on a structural regression. A `vector-rich` scenario (charts + SVG icons +
  gradient) joins the gated current-speed harness; `BenchmarkMedianTool` carries the
  stage breakdown into its aggregate; and the smoke gate's GC-noisy `peakHeapMb`
  check is now advisory (fails only on average latency). Chart-layout variants
  (horizontal / stacked / donut / value-axis-min), a sparkline ramp, and a
  per-paint-mode vector render bench round out the JMH suite.
- **Removed the `java.awt.*` / `java.util.*` co-wildcard in four files.**
  `InvoiceTemplateComposer`, `ProposalTemplateComposer`,
  `WeeklyScheduleTemplateComposer`, and the engine `PdfRenderingSystemECS`
  imported both wildcards, leaving `List` resolvable from either
  `java.awt.List` or `java.util.List` — sound today only because `java.awt.List`
  was never referenced. Each used only `java.awt.Color`, so the wildcard is now
  an explicit `import java.awt.Color;`. No behaviour change.
- **Sweep follow-up note for future bisectors.** The v1.8.0 import/Javadoc
  sweep (`f04a7dce`, part of #162) also carried mechanical code rewrites in
  roughly 40 files beyond its stated scope: ~30 private preset `Template`
  classes converted to records, constructor copy-loops replaced with
  `Collections.addAll`, explicit imports collapsed to wildcards, and five
  presets' explicit `section == null` guards folded into
  `SectionLookup.hasContent`'s null tolerance (now documented on the method
  and pinned by `SectionLookupTest`). All rewrites were verified
  behavior-preserving by the full gate at merge time; recorded here so a
  future bisect does not skip that commit on the strength of its message.

### Tests

- Pinned the fail-loud guards on the new value types so a future refactor
  cannot silently drop one: `PolygonNodeTest` (fewer than three points,
  non-positive / `NaN` / ∞ box, defensive vertex-ring copy), `DocumentColorTest`
  (`withOpacity` range + `NaN` rejection, boundary alpha rounding, `rgba`
  alpha), `ShapeOutline.Path` cases in `ShapeOutlineTest` (segment-count /
  `MoveTo`-first / null guards, defensive copy), and `PathBuilder` dashed-pattern
  rejection plus the documented `build()`-snapshot contract. Extended
  `PublicApiNoEngineLeakTest` to cover the new public `document.svg` package
  (it is engine-clean today; the guard now keeps it that way).
- Chart geometry pinned without rendering: `NiceScaleTest` golden tables and
  `ChartLayoutResolverTest` exact-position assertions on a font-independent
  text-metrics fake; `ChartLayoutSnapshotTest` layout snapshots + a
  fragment-lowering assertion; `SectionKeepTogetherTest` covers section,
  module, and timeline relocation plus the unchanged default.
- Audit-driven edge-case coverage. DOCX semantic export: nested lists indent
  two spaces per depth, per-depth custom markers survive, lists inside
  sections export, empty lists are a no-op. Pagination: a keep-together
  section taller than a full page still flows instead of relocating. Charts:
  negative grouped bars extend the axis below zero and hang from the zero
  baseline (positive and negative bars meet at zero, heights proportional to
  `|value|`), an explicit positive axis minimum anchors grouped bars at the
  visible floor, stacked bars skip non-positive segments, a one-point
  smooth/area line keeps its marker and label, long category labels stay
  slot-sized, tight-width legends keep every entry, all-negative `NiceScale`
  ranges.
- Monotone interpolation pinned in `ChartLayoutResolverTest`: the `MONOTONE`
  curve's bounding box stays within the `LINEAR` data range (ground truth)
  while `SMOOTH` overshoots it, plus a one-native-Bézier-run assertion and a
  `charts/line_monotone` layout snapshot.

## v1.7.1 — 2026-06-09

Open cycle — bug-fix / housekeeping. Entries land here as they merge.

### Performance

- **Text wrapping stops re-measuring the growing line prefix.** The greedy line
  wrapper in `TextFlowSupport` now keeps a running line width and measures each
  token once, instead of re-measuring the whole accumulated line on every token.
  This removes O(line-length × tokens) measured-character work — and the
  per-glyph sanitize/encode it triggered — from paragraph layout. **Output is
  byte-identical: all layout and visual-regression snapshots pass unchanged.**
  The effect is workload-dependent and concentrated in long-text documents;
  measured locally (same-session A/B, full profile) a long multi-page proposal
  rendered markedly faster, and a measurement-count probe showed ~9× fewer
  measured characters on a long paragraph. No public API or behaviour change.

- **Long-token line breaking is no longer quadratic.** `TextFlowSupport.fitCharacters`
  now binary-searches the break point instead of re-measuring every growing prefix
  one character at a time. For an unbreakable run (long URL/ID, no-space CJK, or a
  very narrow column) this cuts measurement calls and measured characters by
  ~80–85% (probe: 652 → 97 width calls, 36k → 7k measured chars on a 600-char
  token). **Output is byte-identical** — the fit predicate is monotonic, so the
  search returns the same break index. No public API or behaviour change.

- **Text measurement no longer embeds binary fonts into a throwaway document.**
  The layout measurement pipeline used to subset-embed every Google/custom font
  family into a private `PDDocument` that was immediately discarded — repeated on
  every new `DocumentSession`, because each render in a server opens a fresh
  session. Measurement now resolves binary families to a **per-thread cached**
  font (mirroring the existing parsed-TrueType cache) bound to a reusable,
  never-saved document, so a family embeds once per worker thread instead of once
  per session, and opening measurement resources owns no PDF document at all.
  **Output is byte-identical** — both paths read glyph widths and metrics from the
  same parsed `TrueTypeFont`; proven by a 960-case render-vs-measurement
  width-parity check (max |Δ| = 0.0), a new `MeasurementFontParityTest`, and the
  full visual-regression / snapshot suite passing unchanged. Only Google/custom-font
  documents are affected (the standard-14 path never embedded); a measurement probe
  showed the per-session embed waste drop ~94–97% (≈1.5–3 MB and ≈2–4.5 ms of font
  subsetting removed per session after the first on a thread). Standard-14-only
  documents are unaffected. No public API or behaviour change.

- **Glyph-coverage probing is memoized instead of repeated per glyph.** The render
  sanitizer (`GlyphFallbackLogger.sanitize` — shared by paragraph spans, table
  cells, watermark and header/footer chrome, and by width measurement) used to
  call `PDFont.encode` for *every code point of every string* — allocating a
  `String` per glyph and, for any glyph the font cannot encode, **throwing and
  catching an exception** — at measurement and again at render. Coverage is now
  memoized per `(font, code point)`: `encode` runs once per distinct glyph, then
  it is a map lookup, and kept glyphs append by code point with no per-glyph
  `String`. **Output is byte-identical** — the substitution decision is the same
  `encode`, only cached; the glyph-fallback warning cadence is unchanged (pinned
  by `PdfFontSanitizerTest`, and width parity by `MeasurementFontParityTest`).
  This removes real per-glyph work from the render hot path: a long document
  re-probed tens of thousands of glyph occurrences that now collapse to roughly
  the number of distinct characters it uses. No public API or behaviour change.

- **Paragraph render writes font and colour operators only when they change.** The
  paragraph render handler emitted a `setFont` (`Tf`) and `setNonStrokingColor`
  (`rg`) operator for *every* text span, even across the spans of a single-style
  paragraph. It now tracks the last-written `(font, size)` and colour across the
  paragraph's graphics-state block and re-emits only on a real change (invalidating
  after inline images/shapes), so a multi-span single-style paragraph carries one
  `Tf` + one `rg` instead of one pair per span — fewer operators for PDFBox to
  serialize. **Rendered output is unchanged** (the skipped operators were
  redundant); pinned by the visual-regression suite plus a content-stream test
  asserting one `Tf` across many drawn spans. No public API or behaviour change.

- **Table cell text is sanitized once per cell instead of three times.** Resolving
  a table ran each cell's lines through `sanitizeCellLines` separately in the
  natural-width, natural-height and resolve passes, rebuilding the list and its
  per-line control-character cleanup up to three times per cell. The sanitized
  lines are now computed once when the logical grid is built and reused by all
  three passes. **Output is byte-identical** (sanitization is deterministic); on a
  large table this removes the dominant per-cell layout allocation. No public API
  or behaviour change.

- **Process-wide line-metrics cache stops inserting instead of flushing when full.**
  The static line-metrics cache `clear()`-ed every entry once it passed 50,000
  distinct styles — a full flush whose non-atomic check-then-clear is a
  thundering-herd recompute under concurrent rendering. It now stops inserting at
  the cap and keeps the existing entries (distinct styles are few in real use, so
  this is only a pathological-explosion guard; it runs on a cache miss, never on
  the per-measurement path). **Measured line metrics are unchanged.** No public API
  or behaviour change.

- **Auto-size font fitting binary-searches the size grid.** A paragraph with
  `autoSize(...)` resolved its font size by scanning every step from max down to
  min, re-measuring the line at each candidate (up to ~50 measurements). Line width
  is linear in font size, so the fit is monotonic — the search now binary-searches
  the grid for the same boundary in ~log2(n) measurements instead of n. **Output is
  byte-identical** — it returns the same grid size the linear scan did (covered by
  the existing auto-size integration and snapshot tests). No public API or behaviour
  change.

- **Table pagination stops re-copying the tail on every page split.** A table that
  spans many pages is split page-by-page, and each split re-sliced the shrinking
  tail by `List.copyOf`-ing its row and row-height lists — even though the source
  layout already holds those lists immutably, so the copy made continuation
  O(rows × pages). The body-only slice now reuses the immutable sub-list views
  directly. **Output is byte-identical** — same rows in the same order (all table
  layout, pagination, and visual-regression tests pass unchanged); a deterministic
  allocation probe on a 2,500-row / 68-page table shows warm compile allocation
  drop 11,155 KB → 9,851 KB (−11.7%). No public API or behaviour change.

### Deprecations

- **`Font.adjustFontSizeToFit(...)` is deprecated.** The engine-internal
  `Font#adjustFontSizeToFit` (and its `PdfFont` / `WordFont` implementations) is
  unused and incorrect — the only real implementation re-measured with the
  unchanged style, so it always returned the minimum size. Canonical auto-size is
  resolved by the layout compiler. The method is kept for binary compatibility and
  scheduled for removal in the next major.

- **The legacy ECS engine packages are deprecated.** `com.demcha.compose.engine.core`,
  `engine.layout` (and `engine.layout.container`), and `engine.pagination` are the
  original `Entity`-based layout/pagination engine — a parallel second engine
  whose execution path the canonical pipeline
  (`GraphCompose.document() → DocumentSession → LayoutCompiler`) never runs; it
  imports nothing from them directly, and the former `GraphCompose.pdf(...)`
  entry point has already been removed. The ECS execution engine runs only under
  the legacy engine regression tests. The packages are now `@Deprecated` (package
  level, so no deprecation-warning cascade)
  with corrected package docs, to stop misdirecting contributors into optimizing a
  dead engine. The genuinely shared engine packages (`engine.components`,
  `engine.measurement`, `engine.font`, `engine.render`) are **not** deprecated.
  No public API or behaviour change.

- **`TextMeasurementSystem` decoupled from `engine.core.SystemECS`.** The shared
  text-measurement contract (`engine.measurement.TextMeasurementSystem`) dropped
  its vestigial `extends SystemECS` and the no-op `process(EntityManager)` default
  it carried — it was never consumed as an ECS system. The legacy ECS engine now
  obtains the measurement service via `SystemRegistry.registerTextMeasurement(...)`
  / `textMeasurement()` instead of enrolling it as a `process()`-driven system,
  completing the isolation of the deprecated `engine.core` from live and shared
  code (only the legacy engine regression tests still reference it). Dropping the
  super-interface is binary-incompatible on paper, so
  `engine.measurement.TextMeasurementSystem` is excluded from the japicmp gate
  until the baseline advances past this release. No canonical API or behaviour
  change.

- **The legacy ECS PDF render pipeline is deprecated.** Follow-up to the ECS
  engine deprecation above. The `Entity`-based PDFBox renderer
  (`PdfRenderingSystemECS` and its collaborators — `PdfRenderSession`, `PdfCanvas`,
  `PdfStream`, `PdfImageCache`, `PdfFileManagerSystem`, `PdfGuidesRenderer`, the
  render-marker handlers, and the `TableCellBox` / `PdfBookmarkBuilder` helpers) is
  the renderer for the removed `GraphCompose.pdf(...)` surface and now runs only
  under the legacy engine regression tests; canonical PDF output goes through
  `com.demcha.compose.document.backend.fixed.pdf`. Because `engine.render.pdf` is a
  *mixed* package — it also holds the canonical-shared `PdfFont`,
  `GlyphFallbackLogger`, and the header/footer + watermark post-processors — the
  legacy classes were physically moved into a new `engine.render.pdf.ecs`
  (with `.handlers` / `.helpers` sub-packages), which is then `@Deprecated` at
  package level (so no deprecation-warning cascade, same pattern as the ECS engine
  packages). The four genuinely shared `engine.render.pdf` types are **not**
  deprecated and stay put. No behaviour change. The relocated renderer has no
  public entry point and carries no binary-compatibility promise, so the move is
  excluded from the japicmp gate rather than treated as a breaking removal.

### Internal

- **Text-measurement line metrics resolve through the `Font` contract instead of a
  PDF-specific fast path.** `FontLibraryTextMeasurementSystem` previously
  special-cased `instanceof PdfFont` to obtain real ascent/descent/leading — every
  other backend font fell back to a degraded `lineHeight`-only metric — which
  coupled the shared measurement system to `engine.render.pdf.PdfFont` and meant a
  new backend could get first-class metrics only by editing shared code. Vertical
  metrics and the process-wide cache key now live on the backend-neutral `Font<T>`
  seam (`Font.lineMetrics(...)` + `Font.measurementCacheKey(...)`, both `default`
  methods; new `FontLineMetrics` record), so a backend supplies first-class metrics
  by overriding the contract and the shared measurement system no longer imports
  `PdfFont`. Binary-compatible (default methods only; japicmp green) and
  behaviour-neutral — PDF and Word produce identical metrics, covered by the
  existing suite plus new polymorphism tests.

### Tests / tooling

- **Benchmark regression gate and measurement probe (benchmarks module, not part
  of the published library).** `BenchmarkVerdictTool` compares a current-speed run
  to the committed baseline (`baselines/current-speed-full.json`) and reports
  improved / neutral / regressed. The hard gate fails only on an **average-latency**
  regression beyond the noise band; peak heap is **advisory** (the `peakHeapMb`
  used-heap delta is GC-timing noisy — use the probe's per-compile allocation
  bytes for deterministic heap). A single run is advisory; the hard gate needs a
  median (`-Repeat` >= 2).
  `MeasurementCountBenchmark` + `CountingTextMeasurementSystem` capture
  deterministic measurement-call counts and per-compile allocation bytes for
  proving algorithmic / allocation changes (the probe warms up the JVM before its
  allocation window, so `Alloc KB` reflects steady state, not one-time
  class-load / JIT cold-start). `scripts/run-benchmarks.ps1` gains the
  `11-verdict-current-speed` step (skippable via `-SkipVerdict`).

- **Cross-platform A/B benchmark harness.** `scripts/ab-bench.sh` (Linux / macOS /
  Windows Git Bash) joins the PowerShell `scripts/ab-bench.ps1` to compare engine
  speed between two branches — interleaved runs, median, per-scenario diff via the
  existing `BenchmarkMedianTool` / `BenchmarkDiffTool`. A path-filtered
  `ab-bench-smoke` CI job runs it on Linux; `.gitattributes` pins `*.sh` and `mvnw`
  to LF so the wrappers stay runnable cross-platform. Benchmark tooling only — not
  part of the published library.

## v1.7.0 — 2026-06-07

Canonical DSL primitives — additive only, zero breaking changes. Adding public
API turns the open cycle into a minor.

### Public API

- **Inline shape runs — geometry-based dots, diamonds, stars and bullets.** New
  `com.demcha.compose.document.node.InlineShapeRun` (`@since 1.7.0`) joins the
  sealed `InlineRun` hierarchy alongside text and image runs. It draws any
  `ShapeOutline` figure on the paragraph baseline directly from geometry — no
  raster payload, no font glyph — so skill rating dots (`Java ●●●●○`), custom
  bullets and inline status markers no longer depend on a font shipping
  `U+25CF` and friends. Authored through `ParagraphBuilder` / `RichText`
  `dot(...)`, `ellipse(...)`, `diamond(...)`, `triangle(...)`, `star(...)` and
  the generic `shape(ShapeOutline, ...)`; measured into line width and height
  like inline images. A `null` fill paints an outlined figure, a `null` stroke
  a filled one; at least one must be present.
- **New polygon shape geometry, usable block-level and inline.** `ShapeOutline`
  (`com.demcha.compose.document.style`) gains a `Polygon` kind plus a family of
  factories built from normalized `ShapePoint` vertices (`@since 1.7.0`):
  `diamond`, `triangle`, `star`, `polygon`, `arrow` / `arrowRight` / `arrowLeft`
  (4-way `Direction`), `chevron`, `checkmark`, `plus` and `regularPolygon(sides)`.
  Arrows and chevrons read as directional list bullets or inline markers
  between text ("Step 1 → Step 2", "Home › Docs"). `ParagraphBuilder` /
  `RichText` add `arrow(size, Direction, fill)` and `chevron(...)` shortcuts
  (every other kind is reachable through `shape(ShapeOutline, ...)`);
  `ShapeContainerBuilder` exposes matching block outlines. Rectangle,
  rounded-rectangle and ellipse shape containers are unchanged.
- **Inline checkboxes + composite (multi-layer) inline figures.** An inline
  shape run is now a stack of paint layers
  (`com.demcha.compose.document.node.ShapeLayer`, `@since 1.7.0`) drawn overlaid
  and centred, so a figure can compose several outlines — each with its own
  fill/stroke — and still measure and place as one unit on the baseline.
  `ParagraphBuilder` / `RichText` gain `checkbox(size, checked, color)` /
  `checkbox(size, checked, boxColor, checkColor)` (`@since 1.7.0`): a rounded
  frame plus, in the checked state, a centred tick — the todo / checklist marker
  for "some items done, some not". The single-outline `InlineShapeRun`
  convenience constructors are unchanged; every other kind still renders as one
  layer.
- **Swappable tick and arrow designs (the "pick your figure" seam).**
  `ShapeOutline` adds `CheckmarkStyle` (`CLASSIC`, `HEAVY`) and `ArrowStyle`
  (`BLOCK`, `TRIANGLE`) enums plus the overloads
  `checkmark(w, h, CheckmarkStyle)` and `arrow(w, h, Direction, ArrowStyle)`
  (`@since 1.7.0`); the no-style factories delegate to `CLASSIC` / `BLOCK`, so
  the default look is unchanged. `checkbox(...)`, `RichText.arrow(...)` and
  `ParagraphBuilder.arrow(...)` gain matching style overloads, and `checkbox`
  also accepts a raw `ShapeOutline` mark for fully custom ticks. Adding a new
  design is one enum constant plus its vertex ring — the foundation for letting
  a caller choose which tick or arrow to render.
- **`softPanel(...)` gains stroke overloads — rounded fill + outline in one node.**
  `AbstractFlowBuilder.softPanel(color, radius, padding, stroke)` and
  `softPanel(color, cornerRadius, padding, stroke)` (`@since 1.7.0`) apply a
  border stroke alongside the panel fill on the same flow node (section, module,
  page flow), so a rounded, padded background with a thin outline no longer needs
  a separate wrapping node. Equivalent to the always-available
  `softPanel(...).stroke(...)` chain — these overloads just make the one-node
  form discoverable.
- **Per-corner radius on shape containers.** `ShapeContainerBuilder.roundedRect(width,
  height, DocumentCornerRadius)` plus the new `ShapeOutline.RoundedRectanglePerCorner`
  (`@since 1.7.0`) round a container's four corners independently — a card "rounded on
  the left, square on the right" no longer needs a CLIP_PATH-parent workaround, since
  both the outline fill/stroke and the child clip now follow the per-corner geometry.
  `DocumentCornerRadius.left/right/top/bottom(...)` give the common asymmetric presets.
  The single-radius `roundedRect(w, h, double)` overload is unchanged, and uniform
  rounded rectangles render byte-for-byte identically (the clip and fill now share one
  per-corner path implementation, called with four equal radii).
- **Vertical text alignment for shape-container labels.**
  `ParagraphBuilder.verticalAlign(TextVerticalAlign)` (new enum
  `com.demcha.compose.document.node.TextVerticalAlign`, `@since 1.7.0`) seats a
  single line by its cap band within its line box — `TOP` (cap top to the box top),
  `CENTER` (cap band centred) or `BOTTOM` (baseline to the box bottom). Combined
  with a vertically-centred layer placement (`.center(...)` / `.centerLeft(...)`), a
  label dropped into a taller `ShapeContainer` / `LayerStack` "pill" sits where you
  ask instead of always on the font baseline — no compensating offset hacks.
  `TextVerticalAlign.DEFAULT` keeps the pre-1.7.0 baseline seating;
  the correction is derived from font metrics (ascent, descent, leading, cap
  height), not a magic number. Render-only and opt-in — existing layouts are
  byte-for-byte unchanged.
- **Bundled font: JetBrains Mono.** New `FontName.JETBRAINS_MONO` (`@since 1.7.0`)
  joins the built-in `DefaultFonts` catalog with its Regular / Bold / Italic /
  Bold-Italic faces (bundled from the OFL-1.1 release), so monospaced code and
  data blocks render without a system-font install. Usable through any
  `DocumentTextStyle.fontName(...)` and listed in the font showcase.
- **Dashed and dotted lines.** `LineBuilder.dashed(double... pattern)` /
  `dashed()` / `dashed(DocumentDashPattern)` plus the new value type
  `com.demcha.compose.document.style.DocumentDashPattern` (`@since 1.7.0`) make a
  `line(...)` paint an on/off dash instead of a solid stroke — section and résumé
  dividers, timeline connectors, cut-here rules. The pattern alternates paint-on
  and paint-off lengths in points (`dashed(8, 5)`; `dashed()` is a balanced 3pt-on
  / 2pt-off; `dashed(1, 4)` reads as dotted). Carried on the line independently of
  `DocumentStroke`, so the stroke value stays a stable two-component record; lines
  are solid by default and the PDF backend honours the dash (other backends fall
  back to a solid stroke).
- **Semantic timelines.** `addTimeline(timeline -> ...)` on every flow / section /
  module, plus `TimelineBuilder`, `TimelineMarker` and `TimelineEntryBuilder`
  (`com.demcha.compose.document.dsl`, `@since 1.7.0`), lay out a vertical timeline
  where each `entry(marker, e -> e.title(...).meta(...).body(...))` pairs a marker
  with its content along a continuous connector rail — work history, project
  milestones, numbered process steps. Markers are `TimelineMarker.dot`, `circle`,
  `numbered` or `square`; the rail colour/width, gutter, entry spacing and default
  title / meta / body styles are all tunable. Declaring the marker-to-content
  relationship replaces the hand-placed bullet-plus-margin pattern; the timeline
  paginates between entries and a tall entry splits within itself, the rail
  continuing across the page break.
- **`DocumentSession.availableHeight()`.** A one-call alias for
  `canvas().innerHeight()` (`@since 1.7.0`) — the usable page content height
  (page height minus top and bottom margins), the value a composition reads to
  decide how much vertical room a section, sidebar or spacer may fill.
  Previously reachable only through the layout-inspection facade.
- **`headingBar(text, bar -> ...)` — one-call filled title band.** Every flow,
  section and module gains `headingBar(String)` and `headingBar(String,
  Consumer<HeadingBarStyle>)` (`@since 1.7.0`) on `AbstractFlowBuilder`: a
  filled, rounded heading band with a single label, added as a child above the
  body. `HeadingBarStyle` (`com.demcha.compose.document.dsl`) tunes fill, corner
  radius, padding, margin, label text style, alignment and an optional outline
  stroke, each with a sensible default (a light-grey band with a centred bold
  label), so `bar -> bar.fill(brand).textStyle(white)` is enough. Sugar over the
  `softPanel(...).addParagraph(...)` recipe — no new node type, just discoverable.

### Fixed

- **`position(node, dx, dy, align)` offsets are now honored for stacks nested
  inside a fixed slot.** A `LayerStack` / `ShapeContainer` placed inside a row
  column or another layer compiled through the fixed-slot stack path, which
  silently dropped the per-layer offsets (anchoring on alignment only) — so a
  positioned badge or cap could not be nudged from its anchor once nested, even
  though the same call worked at the document root. The nested path now feeds
  the same `PreparedStackLayout` offsets as the root path. Layout for documents
  that did not use `position(...)` inside a nested stack is unchanged.

### Documentation

- **New recipe pages for page composition and font coverage** (closing the
  `docs/recipes/` discoverability gaps G2 / G6):
  [`page-backgrounds.md`](docs/recipes/page-backgrounds.md) (`PageBackgroundFill`
  columns / bands / point-based fills / layering),
  [`layered-page-design.md`](docs/recipes/layered-page-design.md) (choosing
  between page backgrounds, rows, layer stacks and canvases),
  [`absolute-placement.md`](docs/recipes/absolute-placement.md) (`addCanvas` +
  `position(x, y)`), and [`font-coverage.md`](docs/font-coverage.md) (WinAnsi
  limits, `●` vs `•`, and the inline-shape / bundled-font alternatives). Linked
  from the README recipes index, `docs/README.md`, and `docs/recipes.md`.

### Build

- **Showcase website separated from documentation (`docs/` → `web/`), now deployed
  via GitHub Actions.** The static GitHub Pages site (`index.html`, `styles.css`,
  `examples.js`, the generated `examples.json` + `showcase/` gallery assets,
  `robots.txt`, `sitemap.xml`, logo) moved out of `docs/` — which previously had to
  host it because branch-based Pages can only serve repo-root or `/docs` — into a new
  top-level [`web/`](web/) folder, so `docs/` now holds **only documentation**. A new
  [`deploy-web.yml`](.github/workflows/deploy-web.yml) publishes `web/` to Pages from
  the **"GitHub Actions"** source; the old branch-`/docs` `deploy-site.yml` was
  removed. `ShowcaseSync` now writes `web/showcase` + `web/examples.json`,
  `VersionConsistencyGuardTest` reads `web/index.html`, and `cut-release.ps1`
  bumps / commits `web/`. The unused Next.js rebuild under `site/` (added in v1.6.8
  but never deployed) was removed. Also renamed `docs/SHOWCASE.md` → `web/README.md`.
  **⚠️ Action required before the next release reaches `main`:** set
  **Settings → Pages → Source = "GitHub Actions"** — once the move lands on `main`,
  `/docs` no longer holds `index.html`, so a branch-`/docs` Pages source would 404.
  The live site is unaffected until then.

## v1.6.9 — 2026-06-03

Housekeeping cycle plus the public pixel-level visual-regression API (Track N).

### Public API

- **Promoted the pixel-level visual-regression harness to public API.**
  `com.demcha.compose.testing.visual.PdfVisualRegression` and
  `com.demcha.compose.testing.visual.ImageDiff` (`@since 1.6.9`) move from the
  test source set into `src/main/java`, alongside the existing
  `com.demcha.compose.testing.layout.*` semantic snapshot helpers. Library
  consumers can now run the same render-PDF → diff-PNG baseline gate against
  their own presets and templates instead of copying the harness. Behaviour is
  unchanged; the PDF→image step is inlined on PDFBox's `PDFRenderer`.
- Exposed `PdfVisualRegression.APPROVE_PROPERTY` (`@since 1.6.9`) — the
  `graphcompose.visual.approve` system-property name — so consumers can toggle
  baseline-approve mode without hard-coding the string (mirrors
  `LayoutSnapshotAssertions.UPDATE_PROPERTY`).

### Documentation

- Added [`docs/operations/visual-regression-testing.md`](docs/operations/visual-regression-testing.md):
  pixel-vs-semantic guidance, the `PdfVisualRegression` API, approve mode,
  baseline layout, and cross-platform tolerance calibration.
- README "Which API should I use?" gains a pixel-level visual-regression row.
- **Made the entire `com.demcha.compose.document.*` public API Javadoc
  doclint-clean.** Added the missing `@param` / `@return` / `@throws` tags and
  element descriptions across 142 files so `mvn javadoc:javadoc`
  (`doclint=all`) runs warning-free. Java's default `-Xmaxwarns=100` cap had
  masked ~90% of the gaps (true count: 929 warnings, not the ~100 first
  visible). Additive Javadoc only — no behaviour change; the only code
  additions are 16 behaviour-neutral no-arg constructors in
  `layout/definitions/*` (documenting the otherwise-synthesised public default
  constructor) and removal of the `@deprecated` block-tags `doclint` forbids in
  `package-info.java` (the `@Deprecated` annotation + prose body already carry
  the notice).

### Build

- CI Javadoc validation (`maven-javadoc-plugin`, `doclint=all`) now covers the
  public `com.demcha.compose.testing.*` helpers (`testing.layout` + `testing.visual`)
  in addition to the canonical `document` API, so Javadoc regressions on the
  testing surface fail fast in CI. No artifact or behaviour change.
- Bumped `central-publishing-maven-plugin` 0.9.0 → 0.10.0 (the Maven Central
  publishing plugin) and removed the Dependabot block on 0.10.0; the
  release-profile build is verified locally and the Central upload is exercised
  at the next publish.

## v1.6.8 — 2026-06-01

**CV v2 migration completion + design-token expansion.** v1.6.8
finishes the CV v2 migration with hyperlink-aware project / entry
titles: a row authored as `"[GraphCompose](https://github.com/x/y)
(Java, PDFBox)"` now renders the title as a clickable link in the
final PDF, with the technology stack remaining a plain
` (Java, PDFBox)` tail. The mechanism is a small extension to
the inline-Markdown parser used by every CV / cover-letter body
row — the `[label](url)` syntax produces a `RichText.link(...)`
run; bare brackets stay literal; everything else (`**bold**`,
`*italic*`, `_italic_`) keeps working as before. The release also
ships four contemporary `BusinessTheme` factory presets
(`nordic()`, `editorial()`, `cinematic()`, `monochrome()`)
alongside the classic / modern / executive trio, expanding the
built-in design-token range to seven presets. Senior-review
follow-ups from v1.6.7 round out the release: the two registry
mutation entry points on `DocumentSession` are now fully
interchangeable (both refuse to mutate a closed session and both
invalidate the layout cache), `target-branch: develop` is pinned
in Dependabot config so future bumps land on the integration
branch, and `logback-classic` rolls forward to 1.5.34 which
fixes [CVE-2026-9828](https://www.cve.org/cverecord?id=CVE-2026-9828)
(deserialisation whitelist bypass).

**Zero breaking public API changes.** The `japicmp` gate against
the v1.6.7 baseline reports `semver PATCH, compatible bug fix`
across every PR in the cycle. New `BusinessTheme` factories are
pure additions; `MarkdownInline.append` and `plainText` extend
their behaviour without changing their signatures; `ProjectLabel.
parse` keeps its two-field record shape (the `title()` field now
preserves Markdown rather than returning a pre-flattened
projection, but the type contract is unchanged and the visible
text projection is one call away via `MarkdownInline.plainText(
title)`). 1058 tests pass at the release-prep tip.

**Migration from v1.6.7.** No code changes required for typical
usage. If you build a custom renderer on top of
`ProjectLabel.parse`:

- Old `title()` was already the visible plain text (emphasis +
  link syntax stripped). New `title()` preserves the original
  inline-Markdown. Wrap with `MarkdownInline.plainText(...)` to
  recover the old behaviour, or route through
  `MarkdownInline.append(rich, title, style)` to get
  emphasis / link rendering for free (the same path
  `ProjectRenderer` now uses).
- `MarkdownInline.append` consumers automatically pick up link
  rendering for `[label](url)` syntax. If any CV / cover-letter
  fixture in your codebase contained a literal `[...](...) `
  string that previously rendered as text, it will now render
  as a hyperlink. Escape with HTML entities or restructure the
  string if you need to keep it literal.

The next release is **v1.7.0** — the additive canonical-DSL
feature minor (LineBuilder.dashed, inline shapes, TimelineBuilder,
dx shortcuts, recipes docs). See [ROADMAP.md](ROADMAP.md).

### Fixes

- The two `DocumentSession` registration entry points are now
  **fully** interchangeable, not just cache-equivalent.
  `session.registry().register(...)` now calls `ensureOpen()`
  before mutating, matching the behaviour of
  `session.registerNodeDefinition(...)`. Previously
  `registry().register(...)` on a closed session silently mutated
  the registry and invalidated a closed-session cache (harmless
  but semantically odd). After this change both paths throw
  `IllegalStateException` on a closed session. (Track J2 — carry-
  over polish from the v1.6.7 senior review.)

### Internal

- `NodeRegistry` Javadoc updated to call out the v1.6.7 non-final
  relaxation explicitly (Track J4). The class became non-final
  in v1.6.7 (Track I3) so `DocumentSession` could install the
  auto-invalidating subclass; the change was already binary-
  compatible (japicmp classified it as `semver PATCH`). The
  Javadoc just makes the rationale discoverable without reading
  the CHANGELOG.

### Public API

- `MarkdownInline.append(...)` (the inline-markdown adapter used by
  every CV / cover-letter body / row / entry renderer) now
  recognises standard Markdown link syntax `[label](url)` and emits
  a clickable hyperlink run via `RichText.link(label, url)`. Pure
  parser extension — no `CvRow` data-shape change required.
  `MarkdownInline.plainText(...)` is updated in lockstep to strip
  link syntax cleanly so callers that pull a plain-text projection
  (e.g. `ProjectLabel.parse`) keep getting just the visible label.
- `ProjectRenderer.inline(...)` and `ProjectRenderer.titleThenBody(...)`
  now route the project-row title segment through
  `MarkdownInline.append(...)` instead of emitting it as a flat
  `RichText.style(...)` run. End-to-end consequence: a CV row with
  `label = "[GraphCompose](https://gc) (Java, PDFBox)"` renders the
  title as a clickable hyperlink and the stack as plain
  `" (Java, PDFBox)"`. Labels without inline Markdown render
  identically to before. `ProjectRenderer.plainInline(...)` (the
  one-line listing variant) intentionally continues to drop link
  syntax via `MarkdownInline.plainText(...)` because a clickable
  link would not survive the compact formatting context.
- `ProjectLabel.parse(...)` now preserves inline Markdown syntax
  inside the returned `title` (the legacy implementation eagerly
  flattened `**emphasis**` and `[links](url)` via `plainText` and
  then split on the last `(`). The split heuristic now targets a
  trailing `\s+\([^()]*\)\s*$` pattern so a leading
  `[name](https://...)` URL's `(...)` segment is not mistaken for
  the technology-stack delimiter. Callers that only need the
  visible-text projection should pass `title()` back through
  `MarkdownInline.plainText(...)`.
- Four new `BusinessTheme` factory presets `@since 1.6.8`:
  `BusinessTheme.nordic()` (Scandinavian minimal — cool whites +
  slate-blue accent + generous whitespace, for design-studio
  reports and product launch decks),
  `BusinessTheme.editorial()` (warm cream surface + deep ink +
  brick-red accent on a serif body, for long-form proposals and
  annual reports),
  `BusinessTheme.cinematic()` (inverted dark navy surface with
  light text + bright copper accent, for investor pitch decks and
  product launch one-pagers), and
  `BusinessTheme.monochrome()` (pure black-on-white with a single
  bold yellow accent, for brutalist editorial layouts where
  typographic contrast carries the identity). Pure additions —
  no change to the existing `classic()` / `modern()` /
  `executive()` presets. japicmp gate against v1.6.7 reports
  `semver PATCH` (compatible additions only).

### Build

- Bumped `jackson-bom` 2.21.3 &rarr; 2.21.4 (broken 2.22.0 skipped via
  the `.github/dependabot.yml` ignore entry added in v1.6.7),
  `logback-classic` 1.5.32 &rarr; 1.5.34 (fixes
  [CVE-2026-9828](https://www.cve.org/cverecord?id=CVE-2026-9828) —
  deserialization whitelist bypass in `HardenedModelInputStream`),
  `central-publishing-maven-plugin` 0.7.0 &rarr; 0.9.0 (0.10.0
  blocked by the existing ignore entry; revisit after a focused
  release-profile evaluation), `japicmp-maven-plugin` 0.23.1 &rarr;
  0.26.1, and a handful of `maven-*-plugin` minor/patch bumps
  (clean / site / resources / enforcer 3.5.0 &rarr; 3.6.3 / surefire
  3.5.5 &rarr; 3.5.6 / source 3.3.1 &rarr; 3.4.0 / gpg 3.2.7 &rarr;
  3.2.8) ([#115](https://github.com/DemchaAV/GraphCompose/pull/115),
  cherry-picked from `main` to align `develop`).

### CI

- `.github/dependabot.yml` now pins both ecosystems
  (`maven`, `github-actions`) to `target-branch: develop` so future
  grouped PRs land on the integration branch instead of `main`.
  Closes the divergence root cause behind the v1.6.7-era #111 /
  #115 episodes where every Dependabot PR force-split history
  between branches and required a cherry-pick to align.

### Documentation

- New quickstart guide
  [Testing your document](docs/operations/test-your-document.md) —
  end-to-end recipe (author the document &rarr; add a layout
  snapshot test &rarr; bless the baseline &rarr; CI guards the
  shape on every PR), with a "when to use which layer" table for
  the three protection tiers (smoke / layout snapshot / pixel-level
  visual). Complements the existing
  [layout-snapshot-testing.md](docs/operations/layout-snapshot-testing.md)
  reference: that one is reference-style, the new one is
  tutorial-style. README's "What can I do with this?" table row
  now links to both.

### Web

- **New Next.js showcase site** under `site/` is now the official
  GitHub Pages deploy target for v1.6.8 onwards. Fully static
  one-page marketing / playground built with Next.js 14 App
  Router + TypeScript + Tailwind. `next build` emits `./out` (4
  static pages, 99.7 kB first-load JS) and the new
  `.github/workflows/deploy-site.yml` (removed in v1.7.0)
  uploads it to Pages on every push to `main` that touches
  `site/**`. **Repo Settings → Pages source must be flipped to
  "GitHub Actions"** for the workflow to take over from the
  legacy branch-based deploy of `docs/index.html`; both files
  coexist in the tree for one more cycle as a rollback.
- Live code snippets in the Hero / Playground sections mirror
  the canonical README hello-world, `examples/.../InvoiceFileExample`,
  and `ModernProfessional.create()` paths, so a visitor copying
  any snippet into a fresh Maven project pulled at
  `io.github.demchaav:graph-compose:1.6.8` gets compiling code.
  Gallery enumerates the full **16-preset cv/v2 lineup** (15
  paired cover letters; `MinimalUnderlined` ships without a
  paired letter by design).
- `scripts/cut-release.ps1` learns a new `Update-SiteDepsVersion`
  step so the Maven / Gradle install snippets in
  `site/lib/deps.ts` flip in lockstep with the README + pom
  versions at cut time — no more silent drift between the site
  and the real released coordinates. The same release commit
  now also stages `site/lib/deps.ts`.

## v1.6.7 — 2026-06-01

**Transitive dependency cleanup.** v1.6.7 narrows the runtime
classpath GraphCompose imposes on consumers. The Kotlin standard
library is gone (the codebase is Java-first; no production
`.kt` sources exist), the `flexmark-all` aggregator is replaced
with the three modules `MarkDownParser` actually references,
`jackson-dataformat-yaml` is marked `<optional>true</optional>`
(mirroring the existing `poi-ooxml` pattern — only consumers that
load YAML configs through `ConfigLoader` need to pull it in),
`jackson-module-jsonSchema` and the explicit `snakeyaml`
declaration are dropped as unused, and `jcl-over-slf4j` is added
explicitly so PDFBox's `commons-logging` call sites keep routing
through SLF4J after the flexmark narrowing (the bridge was
previously provided transitively via `flexmark-all`). The cycle
also fixes a latent layout-cache staleness bug on
`DocumentSession.registry().register(...)` (Track I3): the
registry returned by `registry()` is now a session-owned wrapper
that invalidates the layout cache on every mutation, matching the
semantics of `DocumentSession.registerNodeDefinition(...)`.

**Zero breaking public API changes.** The `japicmp` gate against
the v1.6.6 baseline reports `semver PATCH, compatible bug fix` —
the one surface delta is `NodeRegistry` becoming non-`final` so
`DocumentSession` can install the auto-invalidating subclass
described above. All existing call sites compile and run
unchanged. The transitive cleanup is a runtime-classpath change,
not a compile-surface change.

**Migration from v1.6.6.** Consumers that relied on dependencies
flowing transitively through GraphCompose must now declare them
explicitly:

| If you transitively depended on… | Add to your build |
|---|---|
| Kotlin stdlib via GraphCompose | `org.jetbrains.kotlin:kotlin-stdlib-jdk8` |
| Flexmark extensions (tables, footnotes, gfm-strikethrough, …) | the relevant `com.vladsch.flexmark:flexmark-ext-*` modules |
| YAML config loading through `ConfigLoader` | `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml` |
| `jackson-module-jsonSchema` | `com.fasterxml.jackson.module:jackson-module-jsonSchema` |
| The `commons-logging` API beyond SLF4J routing | declare `commons-logging:commons-logging` explicitly (GraphCompose intentionally excludes it from PDFBox and bridges via `jcl-over-slf4j`) |

No code changes are required for typical usage — pure-PDF
consumers and JSON-only `ConfigLoader` callers carry on as before.
The next minor with new canonical DSL primitives is **v1.7.0**
(see [ROADMAP.md](ROADMAP.md)).

### Build

- Dropped the `kotlin-stdlib-jdk8` compile dependency, the
  `kotlin-test` test dependency, and the `kotlin-maven-plugin`
  build extension. GraphCompose is Java-first; no production
  Kotlin sources exist, and the runtime now no longer carries
  the Kotlin standard library transitively. Consumers that
  relied on `kotlin-stdlib` flowing through GraphCompose must
  declare it explicitly.
- Replaced the `flexmark-all` aggregator dependency with the three
  modules actually referenced by `MarkDownParser`: `flexmark`
  (core parser + AST), `flexmark-util-ast` (Node / NodeVisitor /
  VisitHandler), and `flexmark-util-data` (MutableDataSet). No
  extension modules (tables, footnotes, gfm-strikethrough, etc.)
  are used by GraphCompose. Consumers that relied on extensions
  flowing through GraphCompose must depend on the relevant
  `flexmark-ext-*` modules explicitly.
- Added `jcl-over-slf4j` as an explicit compile dependency. PDFBox
  3.0.7's `PDDocument.<clinit>` calls `org.apache.commons.logging.
  LogFactory` directly; we exclude PDFBox's own `commons-logging`
  artifact to keep one logging facade, and the bridge routes those
  calls through SLF4J. Previously the bridge was provided
  transitively via `flexmark-all`; making it explicit keeps the
  classpath reproducible after the flexmark narrowing above.
- Marked `jackson-dataformat-yaml` as `<optional>true</optional>`,
  mirroring the existing `poi-ooxml` pattern. The only consumer is
  `ConfigLoader.loadConfigWithEnv(...)` when the caller passes a
  `.yaml` / `.yml` resource; library consumers that load JSON
  configs (or skip `ConfigLoader` altogether) no longer pull in the
  ~1.7 MB SnakeYAML transitive footprint. Applications that load
  YAML configs through this helper must now declare
  `jackson-dataformat-yaml` in their own build.
- Removed the unused `jackson-module-jsonSchema` dependency — no
  code path references it.
- Removed the explicit `snakeyaml` dependency declaration and the
  `snakeyaml.version` property. SnakeYAML is now resolved
  transitively (and `optional`) through `jackson-dataformat-yaml`,
  which version-aligns it with Jackson's BOM.
- Bumped `net.sf.jasperreports:jasperreports` 6.21.3 &rarr; 7.0.7
  in the benchmarks module. Benchmarks are a sibling Maven module
  consumed only by the manual performance harness — no impact on
  library consumers ([#111](https://github.com/DemchaAV/GraphCompose/pull/111)).

### Documentation

- `ConfigLoader.loadConfigWithEnv` Javadoc now states the YAML
  path requires `jackson-dataformat-yaml` on the classpath and
  throws `NoClassDefFoundError` when the optional dep is absent.
- `DocumentSession.registry()` Javadoc now explains that the
  returned registry is a session-owned wrapper whose
  `register(...)` mutates the registry *and* invalidates the
  layout cache, making the two registration entry points
  (`session.registry().register(...)` and
  `session.registerNodeDefinition(...)`) interchangeable.

### Fixes

- `DocumentSession.registry().register(...)` now invalidates the
  layout cache the same way
  `DocumentSession.registerNodeDefinition(...)` does. Previously,
  registering a node definition through `registry()` mutated the
  registry in place but left the cached `LayoutGraph` pinned to
  the previous compile, so a follow-up call to `render(...)` or
  `layoutGraph()` silently returned the stale graph routed through
  the old definition. Implemented by wrapping the session's
  `NodeRegistry` in a private session-owned subclass that funnels
  every `register(...)` call through `invalidate()`. (Track I3.)

### Internal

- `NodeRegistry` is no longer `final` so `DocumentSession` can
  install a session-owned subclass that auto-invalidates the
  layout cache on mutation (see Fixes above). Standalone
  `NodeRegistry` instances retain their previous behaviour.
- Replaced eight residual `org.jetbrains.annotations.NotNull` /
  `@Nullable` usages with `lombok.NonNull` (where the surrounding
  file already used Lombok) or removed them entirely (private
  methods and test fixtures). `org.jetbrains:annotations` is no
  longer on the runtime classpath after the Kotlin removal.

## v1.6.6 — 2026-05-31

**First Maven Central release.** GraphCompose now ships under
`io.github.demchaav:graph-compose:1.6.6` — note the **hyphenated**
artifactId, chosen for readability ahead of the Central debut. The
release adds publishable sources/javadoc jars, GPG-signed artefacts,
a binary-compatibility gate against v1.6.5, the metadata Maven
Central requires, and a substantial documentation polish for the
maturity / stability / migration story.

**Zero breaking changes from v1.6.5.** Existing JitPack callers continue
to resolve through the same coordinates (`com.github.DemchaAV:GraphCompose:v1.6.5`);
existing API surface compiles and runs unchanged (validated by the new
`japicmp` gate against the v1.6.5 baseline). New: the `@Beta`
annotation marker, the `@since 1.0.0` class-level Javadoc on
entry-point packages, and a curated docs pass (decision guide for
the two template surfaces, examples maturity index, explicit API
stability policy).

**Migration from v1.6.5:** no code changes required. Swap the
JitPack `<dependency>` for the Maven Central form
(`io.github.demchaav:graph-compose:1.6.6`). The legacy JitPack URL
keeps resolving for callers pinned to v1.6.5 and earlier.

### Build

- **Binary-compatibility gate against v1.6.5** (`japicmp` profile,
  Track E1). The new `binary-compat` CI job builds the artifact on every
  pull request and diffs it against `com.github.DemchaAV:GraphCompose:v1.6.5`
  pulled from JitPack. Binary-incompatible modifications to the public
  surface fail the build; source-incompatible changes are reported only
  (phased policy, will tighten after the 1.6.6 cut). Run locally with
  `./mvnw -DskipTests -P japicmp verify -pl .`; HTML/MD/XML reports
  land in `target/japicmp/`. JitPack repository is scoped to the
  `japicmp` profile, so downstream consumers do not inherit it.
- **Maven Central publish workflow** (Track D4). New
  [`.github/workflows/publish.yml`](.github/workflows/publish.yml) fires
  on the same `v*` tag push that triggers the existing
  `release.yml`. It re-runs `mvnw verify` at the tagged commit, imports
  the GPG key (Track D2) into the runner keyring, writes the
  `<server id="central">` credentials block into `~/.m2/settings.xml`
  via `actions/setup-java@v5`, then invokes
  `./mvnw -P release -Dgpg.skip=false deploy` — the
  `central-publishing-maven-plugin` (Track D3) uploads to Central and
  blocks until Sonatype's validator responds. Hyphenated tags
  (`-rc`, `-alpha`, `-beta`, `-snapshot`) are explicitly skipped — those
  ship only to JitPack and the GitHub Release pre-release surface.
  A `workflow_dispatch` input lets the maintainer re-publish an
  existing tag without re-cutting it if Central had a transient
  validator hiccup. The workflow is dormant until four GitHub repo
  secrets are wired: `MAVEN_GPG_PRIVATE_KEY`, `MAVEN_GPG_PASSPHRASE`,
  `CENTRAL_USERNAME`, `CENTRAL_TOKEN`.
- **`docs/contributing/release-process.md` updated** with the
  end-to-end Maven Central runbook (Track D4 docs). New § 2.C
  "One-time Maven Central setup (maintainer)" walks through GPG key
  generation, keyserver upload, Sonatype account / namespace
  verification, Central user-token generation, the four GitHub
  secrets, and the release-candidate dry-run strategy. § 2.B
  post-release checklist gains a new step 9 for the Central publish
  alongside the existing JitPack step.
- **Hosted Javadocs via `javadoc.io`** (Track H3). README's
  distribution-status note now points callers at
  [javadoc.io/doc/io.github.demchaav/graph-compose](https://javadoc.io/doc/io.github.demchaav/graph-compose),
  which auto-mirrors any artefact published to Maven Central within
  minutes — no separate hosting infrastructure required. The note
  also pins Maven Central as the going-forward primary distribution
  starting v1.6.6 (JitPack stays available alongside for existing
  callers). The full Central install snippet ("Central as primary,
  JitPack as fallback") lands in the v1.6.6 release-prep PR after the
  first Central publish proves the pipeline end-to-end.
- **`central-publishing-maven-plugin` in the `release` profile**
  (Track D3). Adds Sonatype's `central-publishing-maven-plugin` 0.7.0
  to the existing `release` profile as a packaging extension. Replaces
  the legacy `nexus-staging-maven-plugin` + manual staging-repository
  workflow with a single `deploy` call. Configuration:
  `publishingServerId=central` (matches the `<server id="central">`
  entry the publish workflow writes from `CENTRAL_USERNAME` /
  `CENTRAL_TOKEN` secrets), `autoPublish=false` (validation gate before
  the artefact goes live — flips to `true` once we're confident
  post-D4), `waitUntil=validated` (the build waits for Sonatype's
  validator so any rejection surfaces in the workflow run, not a
  silent stuck upload). Requires the `io.github.demchaav` namespace to
  be verified on `central.sonatype.com` (one-time human step via
  GitHub auth or DNS TXT record). The plugin loads inert until D4's
  workflow provides the credentials.
- **GPG signing in the `release` profile** (Track D2). Adds
  `maven-gpg-plugin` 3.2.7 to the existing `release` profile, binding
  to the `verify` phase to sign main / sources / javadoc / pom
  artefacts — Maven Central rejects unsigned uploads. **Off by
  default**: a new property `<gpg.skip>true</gpg.skip>` keeps local
  `mvn -P release package` runs working without a configured GPG key.
  The publish workflow (Track D4) flips it explicitly with
  `-Dgpg.skip=false` once the `MAVEN_GPG_PRIVATE_KEY` and
  `MAVEN_GPG_PASSPHRASE` secrets are wired. `gpgArguments` declares
  `--pinentry-mode loopback` so non-interactive CI runs accept the
  passphrase from `-Dgpg.passphrase` / `MAVEN_GPG_PASSPHRASE` without
  needing a TTY for `gpg-agent`.
- **`release` Maven profile with sources + javadoc jars** (Track D1).
  Activated with `-P release`, attaches `*-sources.jar` and
  `*-javadoc.jar` to the `package` phase via the standard
  `maven-source-plugin` (3.3.1) and `maven-javadoc-plugin` (3.12.0)
  configurations Maven Central requires. The Javadoc plugin runs with
  `doclint=none` and `failOnError=false` so Lombok-generated members
  and `@Internal` engine surface don't block a publish; warnings are
  surfaced quietly. Default `mvnw verify` still does not pay the
  ~30 s of extra packaging — the profile is off by default and turned
  on by `cut-release.ps1` (once Track D3's central-publishing plugin
  lands) and the publish workflow (Track D4).
- **SCM block canonicalised** in `pom.xml` (Track D1 polish). The
  Central metadata validator is strict about the `<scm>` block:
  `<connection>` now uses `scm:git:https://…` (HTTPS, not the legacy
  `git://` transport) and `<developerConnection>` now uses
  `scm:git:ssh://git@github.com/…` (the canonical SSH URL with the
  `git@` user, not the older `ssh://github.com:…` form). Matches the
  shape every Central artefact's POM carries.
- **New `benchmarks/README.md`** (Track B1). Honest framing for the
  manual benchmark layer ahead of the Maven Central debut: explicitly
  positions the harness as a smoke / diff / endurance tool — not a
  JMH-grade benchmark — and tells callers when *not* to use it
  (publishable performance claims, architectural decisions,
  cross-library comparisons that read too much into a single number).
  Documents the file-by-file role of each runner / report tool, the
  exact CI smoke invocation, and a "How to read a report" cheat sheet.
  Cross-links the planned JMH chain (Track C, B3 → B6 in 1.7.0) so a
  reader knows what's coming and how to identify "rigorous"
  measurements when they arrive.
- **Class-level `@since 1.0.0` Javadoc on the public entry-point
  surface** (Track H1). 26 public types in the canonical user-reached
  packages (`com.demcha.compose.GraphCompose`, `com.demcha.compose.document.api.{DocumentSession, DocumentPageSize, PageBackgroundFill}`,
  `com.demcha.compose.document.dsl.{DocumentDsl, RichText, Transformable}` plus all 19 DSL builders)
  now carry class-level `@since 1.0.0` Javadoc tags so callers can see
  the introduction version at IDE quick-doc / generated Javadoc time
  without trawling CHANGELOG history. New guard test
  `PublicApiSinceTagCoverageTest` source-scans the three entry-point
  roots and fails the build if a new public top-level type lands
  without a class-level `@since` tag; `internal/` sub-packages are
  excluded by convention (`InternalAnnotationCoverageTest` covers those).
  Method-level `@since` backfill for the ~380 public methods in these
  packages is intentionally out of scope here and tracked separately.
- **`maven-enforcer-plugin` gate** (Track E2). Binds three rules to the
  `validate` phase so the build refuses to start when a precondition is
  broken: `requireJavaVersion` (≥ 17 — the declared baseline, catches
  accidental JDK 11 / 15 attempts), `requireMavenVersion` (≥ 3.8.0 —
  the oldest version the planned central-publishing pipeline supports),
  and `requirePluginVersions` (every plugin must declare an explicit
  non-`LATEST` / non-`RELEASE` / non-`SNAPSHOT` version — the
  generalisation of the PR-7.1 exec-plugin drift lesson).
  Default-lifecycle plugins (`clean` / `install` / `site` / `resources` /
  `deploy`) are now pinned in a new `<pluginManagement>` block so
  `requirePluginVersions` has nothing to flag. Minimums and versions
  live in `<properties>` (`enforcer.requireMavenVersion`,
  `enforcer.requireJavaVersion`, `maven.enforcer.plugin.version`).
- **Parallel-session stress test** (Track I2). New
  `DocumentSessionParallelStressTest` drives 32 independent
  `DocumentSession` instances on a fixed-size thread pool through 4
  iterations and asserts (a) all parallel renders produce a layout-graph
  signature byte-equal to the sequential baseline — exercising the
  shared font registry, glyph cache, built-in node definitions, and
  shape-outline cache for race conditions; (b) every PDF output starts
  with the `%PDF` magic, is at least 256 bytes, and has size variance
  under 256 bytes across threads (catching corruption or rare
  non-determinism without locking exact byte counts that timestamps
  could drift). 128 + 128 = 256 renders complete in ~1.6 s locally, so
  the test does not bloat CI. The contract is that each
  `DocumentSession` is single-threaded but the process-wide machinery
  handles concurrent _independent_ sessions safely; this test pins that.
- **`no-poi` Maven profile + CI job** (Track I1). The `poi-ooxml`
  dependency is declared `<optional>true</optional>` so callers that
  render only PDFs don't pay the ~10 MB POI footprint; this PR adds a
  regression gate that proves it. Running `./mvnw -P no-poi test -pl .`
  excludes `poi-ooxml` (and its `poi` / `poi-ooxml-lite` transitives)
  from the surefire test classpath and sets the system property
  `no.poi=true`. DOCX-specific tests (`DocxSemanticBackendTest` and the
  one DOCX export in `DocumentSessionTest`) now carry
  `@DisabledIfSystemProperty(named = "no.poi", matches = "true")` and
  skip cleanly. The rest of the canonical suite (1029 tests, 4 skipped
  under `-P no-poi`) runs green without POI on the classpath. A new
  `no-poi-suite` CI job exercises the profile on every pull request.

### Public API

- **New `@Beta` annotation** (Track H2). Companion to the existing
  [`@Internal`](src/main/java/com/demcha/compose/document/api/Internal.java)
  marker:
  [`com.demcha.compose.document.api.Beta`](src/main/java/com/demcha/compose/document/api/Beta.java)
  signals an **Extension SPI** or **Experimental** surface — a
  deliberately-exposed seam library users can implement or call, but
  whose shape may still evolve between minor releases per the
  [API stability policy](docs/api-stability.md) § 1. First application:
  [`com.demcha.compose.document.layout.NodeDefinition`](src/main/java/com/demcha/compose/document/layout/NodeDefinition.java)
  — the canonical custom-node-type seam, carved out of the otherwise
  `@Internal` `document.layout` package. New
  `BetaAnnotationDocumentationTest` pins the annotation's retention /
  target / `@Documented`-ness / source-Javadoc contract in the same
  shape `InternalAnnotationDocumentationTest` already pins for
  `@Internal`. Additional Extension SPI surfaces (render-handler
  interfaces, fragment-payload interfaces) will gain the marker
  incrementally as their contract solidifies.

### Documentation

- **New flagship example: `EngineShowcase`** + **regenerated
  `assets/readme/repository_showcase_render.png` hero image** ahead of
  the Maven Central debut. A presentation audit before v1.6.6 flagged
  that the existing hero PDF was a dated single-page render and the
  GitHub Pages showcase had 20 broken asset paths (CV v2 migration
  added `-v2` suffixes that `docs/index.html` never picked up). Fixed
  in three commits: (a) `docs/index.html` path repair so every CV /
  cover-letter preview resolves; (b) new flagship
  `examples/.../flagships/EngineShowcase.java` renders a single-page
  cinematic brand promo — a navy + electric-orange composition with a
  rounded clip-frame hero (semantic-graph → polished-PDFs visual
  metaphor), a magazine-headline lockup ("Documents as code. /
  Cinematic by default."), three KPI cards (Templates v2 · 1,033
  tests · v1.6.6 Maven Central), a three-column capability grid
  (Semantic DSL · Deterministic Layout · Cinematic Themes), and a
  footer brand stripe — exercising `ShapeContainerNode` +
  `ClipPolicy.CLIP_PATH` for the hero frame, classpath-loaded image
  embedding (`examples/src/main/resources/engine-hero.png`),
  `softPanel(...)` + `accentLeft(...)` decorators on V2 sections, and
  mixed serif/sans typography; (c) page 1 rasterised to
  `assets/readme/repository_showcase_render.png` via the new persistent
  helper `com.demcha.examples.support.PdfPageRasterizer` (PDFBox-based,
  no external Ghostscript / ImageMagick dependency). The hero now
  reads as the engine's brand register rather than a Lorem-ipsum
  template render.
- **`docs/architecture/package-map.md` updated** alongside H2. A new
  intro paragraph documents the stability-marker convention (Stable
  default; engine packages are package-level `@Internal`; individual
  Extension SPI seams carved out of `@Internal` packages carry
  `@Beta`), and the `document.layout` row calls out `NodeDefinition`
  as the current `@Beta` seam.
- **`docs/api-stability.md` revised** alongside H2 — `@Beta` annotation
  reference cells in §1 are no longer hedged as "pending"; the
  associated quote block lists both annotations side-by-side with the
  guard tests that pin them.

### Engine internals (no behaviour change)

- **`RowSlots` helper extracted** from `LayoutCompiler` and
  `NodeDefinitionSupport`. The defence-in-depth `IllegalArgumentException`
  guard added in v1.6.5 (PR-7.3) for the row weights / children size
  mismatch lived as duplicated inline code at both engine call sites
  with no direct test — a future refactor could have silently deleted
  either copy. The validation now lives in
  `com.demcha.compose.document.layout.RowSlots#validateWeightsMatchChildren`
  (package-private), with `RowSlotsTest` driving it directly. Error
  message is unchanged. `GraphCompose.DocumentBuilder#pageBackgrounds(...)`
  Javadoc now spells out the empty-list-clears semantics in prose, not
  only in the `@param` line.

### Documentation

- **New decision guide: [`docs/templates/which-template-system.md`](docs/templates/which-template-system.md)**
  (Track G1). The repo ships two parallel canonical template surfaces —
  `cv.presets.*` (the "classic" v1.6 rebuild) and `cv.v2.presets.*` (the
  layered architecture, recommended) — under confusingly similar names.
  The new page pins the terminology once, gives a status matrix
  (Recommended / Supported / Legacy / Internal) for every template
  surface and the canonical DSL, walks a decision tree for new code, and
  provides a preset-by-preset migration table from `classic` to
  `layered` plus a 1.x → 2.0 deprecation inventory naming every type
  scheduled for removal. `CanonicalSurfaceGuardTest` allowlist updated
  so the deprecation-inventory section's literal mentions of
  `GraphCompose.pdf(...)`, `PdfComposer`, etc. don't trip the
  legacy-token scan (same allowlist class as the v1.5 → v1.6 migration
  log already in there).
- **`examples/README.md` reorganised by maturity** (Track G2). The
  gallery section was grouped by the GraphCompose release that
  introduced each example (Built-in templates / Cinematic v1.5 /
  v1.5 feature showcases / v1.6 feature showcases / Public-API
  surface / Production patterns / Operational documents) — useful
  history for maintainers, less useful for someone landing on the
  examples folder for the first time. The gallery now categorises
  by maturity / intent: **🚀 Start here**, **🧱 Core DSL**,
  **📋 Templates recommended**, **🔧 Advanced SPI**, **🗄️ Legacy**.
  All 26 examples retained their anchor IDs, so existing deep links
  continue to resolve; only the gallery index is restructured. A
  maturity legend introduces the five tiers and links to
  `docs/templates/which-template-system.md` for the V1 → V2 path that
  the **Legacy** tier points at.
- **New API stability policy: [`docs/api-stability.md`](docs/api-stability.md)**
  (Track G3). User-facing companion to
  [ADR-0003](docs/adr/0003-api-stability-and-internal-marker.md): pins
  the four stability tiers (**Stable**, **Extension SPI**, **Internal**,
  **Experimental**) with what each one promises in patch / minor /
  major releases, the sealed-hierarchy permit-list policy (additive
  variants must degrade gracefully without `default`-branch failures),
  the deprecation window (≥ 1 minor release with `@Deprecated`, removed
  in next major), a per-package tier-lookup table for the canonical
  surface plus the legacy packages headed for 2.0 removal, and an
  "anti-policy" section (no pixel-stable PDFs, no bit-stable artefact
  bytes, no sealed-permit exhaustiveness across minor releases for
  Stable hierarchies). `CanonicalSurfaceGuardTest` allowlist extended
  so the page can name `com.demcha.templates.*` / `com.demcha.compose.v2.*`
  and the legacy `pdf(Path)` factory in the package-tier and
  deprecation-example sections.

## v1.6.5 — 2026-05-30

### Templates v2

- Added the `CenteredHeadline` CV preset to the `cv/v2` layered
  template surface, including its isolated theme tokens, visual
  regression baselines, and reusable `Subheadline` /
  `SectionHeader.flatSpacedCaps` widget support.
- Added the **Mint Editorial** template set: a two-page, two-column
  editorial CV preset `MintEditorial` (centred spaced-caps masthead with
  a full-width mint accent rule; sidebar contact / interests / education /
  expertise / skill-bars / social beside a profile / experience / awards /
  references main column) and its paired `MintEditorialLetter`, both on
  `CvTheme.mintEditorial()` and with visual regression baselines.
- Added two reusable `cv/v2/widgets`: `SkillBar` (data-driven proficiency
  bar — spaced-caps label above a track with a level-positioned marker;
  no bar when the level is absent) and `IconTextRow` (inline icon + text
  row, optionally a single click target), with `WidgetSmokeTest` coverage.
- Added optional proficiency levels to `SkillGroup` via the new
  `CvSkill` record and `SkillsSection.Builder.leveledGroup(...)`. Fully
  backward-compatible: name-only skills carry no level and every existing
  name-based renderer is unaffected.
- Added `MintEditorial.Options` (and a matching `MintEditorialLetter.Options`)
  — an additive masthead colour API (accent, rule, name, and an optional
  full-width page-1 header band) whose defaults reproduce the stock render
  exactly, so the committed look and the parity baselines are unchanged.

### Public API

- **`PageBackgroundFill` band helpers.** Added `topBand`, `bottomBand`,
  `band`, `topBandPoints`, and `bandPoints` factory methods for full-width
  horizontal background bands (top, bottom, or arbitrary vertical offset;
  ratio- or point-based), complementing the existing column helpers and
  building on the v1.6.5 y-coordinate fix below.

### Bug fixes

- **`PageBackgroundFill` y-coordinate.** A partial-height page-background
  fill (`heightRatio < 1.0`) was painted from the page **bottom** upward
  instead of from the `yRatio` top edge the API documents, so a band with
  `yRatio = 0` rendered at the bottom of the page. Fills now convert the
  top-down ratios to the PDF bottom-up origin correctly
  (`y = (1 - yRatio - heightRatio) * pageHeight`); full-page and
  full-height column fills are unchanged. Adds top-/bottom-/mid-band
  regression tests.
- **`GraphCompose.document().pageBackgrounds(emptyList())` now actually
  clears.** The builder's Javadoc promised that an explicit empty list
  overrides any earlier `pageBackground(color)` on the same builder, but
  the implementation skipped empty lists, so `pageBackground(LIGHT_GRAY)`
  followed by `pageBackgrounds(List.of())` still emitted the grey
  background. The guard is removed; the empty list is now the documented
  clear. Adds a regression test.
- **`distributeRowSlotWidths` weights / children mismatch.** When a row
  was constructed with a `weights` list whose size did not match the
  number of children (only reachable by bypassing `RowBuilder` and
  building a `RowNode` directly), the engine's row distribution code
  walked off the end of the `weights` list with a raw
  `IndexOutOfBoundsException`. Both row-distribution call sites
  (`LayoutCompiler#distributeRowSlotWidths`, `NodeDefinitionSupport#measureRow`)
  now reject the mismatch with an `IllegalArgumentException` whose
  message names both sizes and the expected fix. `RowNode`'s canonical
  constructor already validated this at construction time; the new
  engine guards are defence-in-depth for any path that bypasses it
  (e.g. reflection-based deserialization). Adds regression tests for
  the canonical-constructor IAE and the `RowBuilder.build()` ISE.

### Build

- **`byte-buddy` is now `<scope>test</scope>`.** Mockito already excludes
  its transitive `byte-buddy` and the project pins a single version in a
  standalone dependency; that dependency was missing a scope, so the
  published POM advertised `byte-buddy` as a compile dependency even
  though no production code references it. Setting `<scope>test</scope>`
  keeps the version pin but keeps `byte-buddy` out of consumers' runtime
  classpath (`mvn dependency:tree` shows it only as `:test`).
- **CI `exec-maven-plugin` version drift removed.** The CI workflow's
  three benchmark steps invoked
  `org.codehaus.mojo:exec-maven-plugin:3.5.0:java` directly, while
  `benchmarks/pom.xml` already declared `exec-maven-plugin` at `3.6.3`
  for local runs — a silent version split between CI and local invocations
  that grew the surface area to keep aligned. CI now calls the configured
  plugin via `exec:java`, picking up the pinned `3.6.3` from
  `benchmarks/pom.xml`. No behaviour change; one fewer hardcoded version
  to bump.

## v1.6.4 — 2026-05-22

Bug fix + structured-block patch. Adds two new public Block types —
`WorkHistoryBlock` and `EducationBlock` — that let template authors
declare work-history and education entries with explicit (title,
organisation, date, description) / (degree, institution, year,
details) fields instead of relying on the legacy
`MultiParagraphBlock` pipe-separated string parser. Also closes a
Boxed Sections layout defect that bundled the date and description
into the right-aligned date column for any author-supplied line that
used an em-dash (`" — "`), en-dash (`" – "`), or contained
prose-shaped content the parser misread as a date. **No public API
break** — the sealed `Block` permit list grows from six to eight,
existing `MultiParagraphBlock` work-history strings continue to
parse, and the deprecated parser path stays in place for backward
compatibility.

### Templates — new structured blocks

- **`WorkHistoryBlock`.** New public record block carrying a list of
  `Item(title, organisation, date, description)` entries. The
  `BoxedSections` preset renders each item as a structured row:
  title bold on the left, date right-aligned on the same row,
  organisation italic on the next line under the title, and
  description as a full-width paragraph beneath. Other presets fall
  back to a single concatenated paragraph per item. Authors who use
  `WorkHistoryBlock` bypass the legacy
  `BoxedSections#parseWorkEntry` heuristic parser entirely.
- **`EducationBlock`.** New public record block carrying a list of
  `Item(degree, institution, year, details)` entries. Renders with
  the same structured layout as `WorkHistoryBlock` (degree bold
  left, year right, institution italic, details paragraph) so
  Education & Certifications sections visually match Professional
  Experience.
- **Sample data migrated.** `ExampleDataFactory.sampleCvSpecV2` now
  uses `WorkHistoryBlock` for Professional Experience and
  `EducationBlock` for Education & Certifications. The legacy
  `MultiParagraphBlock` pattern remains supported and is exercised
  by `PresetLayoutSnapshotTest` / `PresetVisualParityTest` to lock
  the backward-compat path.

### Templates — parser robustness (legacy path)

- **`parseWorkEntry` accepts em-dash and en-dash.** Used to split
  the post-pipe segment on ASCII `" - "` only; now tries `" — "`,
  `" – "`, and `" - "` in order, mirroring `splitHeading`. Authors
  who typed `"*2024-Present* — Led reusable document flows."` saw
  the whole tail collapse into the date column — this no longer
  happens.
- **`parseWorkEntry` rejects prose dressed up as a date.** The
  loose `looksLikeDate` check accepted any string containing a
  year and a hyphen anywhere, which caused education lines like
  `"... | 2019. First-class honours. Specialisation ..."` to
  parse as work entries (the hyphen inside `"First-class"` was
  enough to satisfy the heuristic). Parser now rejects post-pipe
  segments that contain sentence-ending punctuation (`.`, `:`,
  `;`) when no explicit date / description separator was found,
  letting these lines fall back to plain paragraph rendering.
  Marked `@Deprecated` with a `@deprecated` Javadoc pointing
  callers to `WorkHistoryBlock` / `EducationBlock`.
- **`parseProjectItem`** picks up the same em-dash / en-dash /
  ASCII separator set so future Project items typed with em-dash
  don't regress into "title only" rendering.

### Tests

- `BlockTest.blockSealingPermitsAllEightVariants` updated for the
  two new permitted block types.
- `PresetVisualGalleryTest.sampleSpec` migrated to
  `WorkHistoryBlock` so the visible "primary example" exercises the
  new structured shape.
- `PresetLayoutSnapshotTest` intentionally retained on
  `MultiParagraphBlock` to lock the legacy parser's behaviour.

## v1.6.3 — 2026-05-22

Bug fix patch. Closes two independent hyperlink clickable-area
defects that surfaced on CV gallery presets and made the LinkedIn /
GitHub contact rows hijack each other's clicks (paragraph-level
link path) or drift past their visible text (span-level link path
through multi-space separators). **No public API change** — engine,
DSL, themes, templates, and backend records all stay
source-compatible with v1.6.2.

### Engine

- **Paragraph-level link annotations now hug rendered text.**
  `PdfFixedLayoutBackend` used to emit a paragraph's `linkOptions`
  as a single rectangle covering the entire fragment box
  (`fragment.x()` + `fragment.width()`), ignoring `TextAlign.RIGHT`
  / `TextAlign.CENTER`. Stacked right-aligned contact paragraphs
  (e.g. one per LinkedIn / GitHub icon row in Timeline Minimal /
  Sidebar Portrait / Monogram Sidebar) therefore produced
  full-column-wide rects that overlapped the empty alignment gap of
  neighbouring rows — hovering over GitHub clicked the LinkedIn row.
  The backend now emits one per-line rect tight to `line.width()`
  positioned at the alignment-aware `lineX`, matching how
  inline-span links already worked. Span-level link emission, table
  / shape / barcode payload links, and bookmark anchoring are
  unchanged.
- **Glyph sanitizer preserves all author whitespace.**
  `PdfFont.sanitizeForRender` used to collapse any run of consecutive
  spaces into a single space, both for whitespace-only tokens (the
  `"   "` halves of a `"   |   "` separator) and for inter-word gaps
  in spaced-caps strings (`spacedUpper("ARTEM DEMCHYSHYN")` produces
  `"A R T E M   D E M C H Y S H Y N"` with deliberate triple-spaces
  between words). The collapse shrank the rendered glyph stream
  under measurement, drifting inline-link rectangles ~8pt per
  `"   |   "` separator past their visible labels and visually
  merging spaced-caps titles back into a single run (`"A R T E M D E
  M C H Y S H Y N"` — no word boundary). The sanitizer no longer
  collapses adjacent spaces; newlines / NBSP / non-tab control
  characters still resolve to a single space each, but author
  whitespace is now preserved verbatim so wrap geometry,
  link-rectangle emission, and `showText(...)` all see the same
  string. Layout snapshot baselines for five CV presets and one
  nested-list document widened to reflect the recovered whitespace —
  the deliberate visual change is the bug fix.

### Templates

- **Boxed Sections projects render as title + indented description.**
  The "Projects" module now renders each bullet-list or
  `IndentedBlock` item as two stacked paragraphs — bullet plus bold
  project name (with an optional tech-stack chunk in parentheses) on
  the first line, then a hanging-indented description below aligned
  to the project name (not the bullet). The previous single-line
  rendering ran the project name and description together. Bullet
  marker, hanging-indent, and surrounding modules are unchanged.
  Example data in `ExampleDataFactory.sampleCvSpecV2` and
  `PresetVisualGalleryTest` now ships tech-stack chunks (`"Java 21,
  PDFBox, Maven, JMH"`) so the gallery PDFs reflect the new layout.

### Tests

- New regression in `PdfFixedLayoutBackendFeaturesTest` —
  `shouldTightlyHugRightAlignedParagraphLinkRectangles` — stacks
  three right-aligned link paragraphs and asserts each clickable
  rect hugs its rendered label width (≤ 150pt), sits flush against
  the inner right margin, and does not overlap the Y-band of
  neighbouring rows.
- New regression in `PdfFixedLayoutBackendFeaturesTest` —
  `shouldKeepCenteredInlineLinkRectanglesAlignedAcrossMultiSpaceSeparators`
  — renders a centered contact line built with `"   |   "` separators
  and asserts the three resulting link rectangles preserve
  left-to-right order with non-overlapping X ranges and a sane
  per-separator gap (5..40pt), pinning the bug where collapsed
  whitespace pushed later rects past the line.
- New regression in `PdfFontSanitizerTest` —
  `sanitizeForRender_preservesWhitespaceOnlyTokensVerbatim` — pins
  the whitespace-only short-circuit so render width stays in
  lockstep with `getTextWidth` for tokenised contact-line
  separators.

## v1.6.2 — 2026-05-20

Robustness patch. Closes four engine defects surfaced while building
the Noir corporate CV example: any Unicode glyph that the active PDF
font cannot encode used to crash the whole render, rounded human
input for page sizes hit a 1e-6 capacity check, a `Row` inside a
`LayerStack` content layer was rejected by the validator, and the
existing exceptions did not point at a fix. **No public API change**
— engine, DSL, themes, templates, and backend records all stay
source-compatible with v1.6.1.

### Engine

- **Glyph sanitizer on every PDF text render path.** Any code point
  the resolved font cannot encode (arrows `U+2192`, bullets `U+25CF`,
  emoji, custom unicode) is now substituted with `?` instead of
  throwing `IllegalArgumentException` deep inside PDFBox `showText`.
  New `PdfFont.sanitizeForRender(TextStyle, String)` is the single
  entry point the paragraph / watermark / header-footer / table /
  block-text handlers route through; width measurement
  (`PdfFont.getTextWidth`) uses the same string so wrap geometry
  stays in lockstep with the bytes drawn. First substitution per
  unique `(font, codePoint)` emits a one-shot WARN through the new
  `GlyphFallbackLogger` (category
  `com.demcha.compose.engine.render.pdf.glyph-fallback`); subsequent
  substitutions are silent.
- **Page capacity rounding tolerance.** The full-page check in
  `LayoutCompiler` now uses a dedicated `CAPACITY_TOLERANCE = 0.5pt`
  (≤ 0.18 mm — visually indistinguishable) instead of the
  floating-point `EPS = 1e-6`. Authors who size content at the
  rounded `842.0pt` against the true A4 inner height `841.88977pt`
  no longer hit `AtomicNodeTooLargeException`; overflows of more
  than 1pt still throw. `EPS` stays as is for split / remaining-
  height decisions inside the splittable-leaf path.
- **`Row` allowed inside `LayerStack` content layer.** New private
  `FixedSlotKind { ROW_SLOT, STACK_LAYER_SLOT }` is threaded through
  `compileNodeInFixedSlot` and propagated down recursive calls.
  Validator at the row-in-fixed-slot guard now rejects nested
  horizontal rows only when the parent slot is a real row band; a
  `Row` directly inside a `LayerStack` layer rectangle (or any
  vertical descendant thereof) is now a normal column-row and
  compiles cleanly. Row-in-row still throws — the relaxation does
  not leak into the `ROW_SLOT` path.
- **Exception messages now include action verbs.** The five
  engine-thrown exception messages
  (`AtomicNodeTooLargeException` plus four `IllegalStateException`s
  in `LayoutCompiler`) now say what to try, not just which rule
  fired: "Reduce the node height, split content into multiple atomic
  blocks, or increase the page size"; "Wrap the inner row in a
  LayerStack layer (allowed since v1.6.2), or stack horizontal
  content as sections inside a vertical column"; etc. Existing
  substring-based test assertions stay green.

### Tests

- New `PdfFontSanitizerTest` pins the sanitizer contract: substitution
  policy for unsupported glyphs, ASCII pass-through, single + collapsed
  spaces, empty / null input, width consistency between bullet input
  and `?` substitute, and the direct `sanitizeByFont` escape hatch
  used by raw-`PDFont` helpers.
- New `LayerStackRowCompositionTest` pins both ends of the R3
  contract: three positive cases (row directly inside a layer, the
  Noir-CV shape with a dark band + sidebar/main row, row deep inside
  a layer through vertical sections) and one negative guard
  (row-inside-row still throws).
- `PaginationEdgeCaseTest` gains
  `atomicNodeHalfPointOverCapacityShouldFitWithinTolerance` and
  `atomicNodeClearlyOverCapacityShouldStillThrow` boundary cases for
  the new capacity tolerance.
- Three new visual regression demos write real PDFs under
  `target/visual-tests/` for manual review:
  `glyph-fallback/UnicodeFallback*.pdf` (paragraph + table +
  watermark + header/footer all with unsupported glyphs),
  `page-capacity/PageCapacityToleranceDemo.pdf` (842pt shape on A4),
  `layer-stack/LayerStackRowDemo.pdf` (full Noir-CV shape).
- New `DevelopTest` scratch test class under
  `src/test/java/com/demcha/testing/visual/` renders a minimal
  document for manual API experimentation; output lives at
  `target/visual-tests/develop/Develop.pdf`.
- One layout snapshot baseline
  (`document/nested_list_three_levels.json`) updated as a deliberate
  consequence of the new font-aware width measurement: `ListBuilder`
  default markers for deep nesting (`◦ U+25E6`, `▪ U+25AA`) are
  outside Helvetica's WinAnsi coverage, so they now substitute to
  `?` and widen the list rectangle. Follow-up tracked: ship safer
  ASCII / font-aware list marker defaults in v1.7.

### Documentation

- `README.md` gains a **Companion projects** section linking the
  experimental [`graphcompose-ai-flow`](https://github.com/DemchaAV/graphcompose-ai-flow)
  sister repo (independent codebase, separate lifecycle, no
  dependency from this repo).
- Maintainer email in `pom.xml` and `CODE_OF_CONDUCT.md` corrected
  from the non-existent `demchyshyn.artem@gmail.com` to the real
  inbox `demchishynartem@gmail.com`, so JitPack artifact metadata
  and CoC enforcement contact resolve.

## v1.6.1 — 2026-05-09

Maintenance + compatibility patch. Drops the Java 21 source/target
baseline to **Java 17+** so the library can ship into older
enterprise stacks without a fork, and refreshes test/build
dependencies. **No public API change** — engine, DSL, themes,
templates, and backend records all stay source-compatible with
v1.6.0; existing v1.6.0 callers compile and behave unchanged.

Co-developed with external contributor
[@jottinger](https://github.com/jottinger)
([#8](https://github.com/DemchaAV/GraphCompose/issues/8),
[#10](https://github.com/DemchaAV/GraphCompose/issues/10)).

### Toolchain

- **Java 17 baseline.** `<maven.compiler.release>` flips from `21`
  to `17` across `pom.xml`, `examples/pom.xml`, and `benchmarks/pom.xml`.
  Engine source loses the Java 21–only constructs
  (switch-with-type-patterns, switch-with-deconstruction,
  `List.getFirst()`, `Thread.threadId()`) in favour of Java 17
  –compatible forms. CI runs against Temurin JDK 17.
- **Dependency refresh + CVE pass.** Bumps Jackson `2.20.1 → 2.21.3`,
  Logback `1.5.18 → 1.5.32`, Lombok `1.18.38 → 1.18.46`, POI
  `5.4.0 → 5.5.1`, SnakeYAML `2.4 → 2.6`, AssertJ `3.27.3 → 3.27.6`,
  JUnit `5.12.2 → 5.14.4`, Mockito `5.20.0 → 5.23.0`. Adds explicit
  ByteBuddy `1.18.7` so Mockito works on the Java 25+ access rules.
  Maven plugin bumps: `maven-compiler-plugin 3.13 → 3.15`,
  `maven-surefire-plugin 3.2.5 → 3.5.5`, `exec-maven-plugin 3.5 → 3.6.2`.

### Looking ahead

Maven Central distribution
([#7](https://github.com/DemchaAV/GraphCompose/issues/7)) remains
on the **v1.7.0** roadmap alongside the JMH benchmark migration;
v1.6.1 stays on JitPack as a maintenance release.

---

## v1.6.0 — 2026-05-07

The "expressive" release. Closes the remaining canonical-vs-legacy
parity gaps for advanced authoring without architectural rollback.
Every new primitive ships through `DocumentNode + NodeDefinition +
render handler`. See [`docs/roadmaps/v1.6-roadmap.md`](docs/roadmaps/v1.6-roadmap.md)
for the phased plan, verification gates, and ADRs.

### Headline — "expressive"

- **Nested list ergonomics (Phase A — landed).**
  `ListBuilder.addItem(label, Consumer)` for builder-callback
  child scopes; per-depth marker cascade; mixed flat / nested
  authoring preserves source order. ADR 0012.
- **Composed table cell content (Phase B — landed).**
  `DocumentTableCell.node(DocumentNode)` accepts any composable
  canonical node as cell content (paragraphs, lists,
  layer-stacks, sub-tables) with two-pass measurement. ADR 0013.
- **Controlled free-canvas placement (Phase C — landed).**
  `CanvasLayerNode` — pixel-precise `(x, y)` placement of
  children inside a fixed-size bounding box, with `ClipPolicy`
  clipping and atomic pagination. ADR 0014.
- **Templates v2 preset library (committed).** Canonical CV /
  cover-letter / invoice / proposal surface rebuilt around four
  layers (theme tokens → layout slots → components + blocks →
  spec data); 14 CV presets and 14 paired cover-letter presets
  with one-liner `create(BusinessTheme)` factories, inline
  markdown, hyperlinks, and slot-based multi-column layouts.
  ADR 0011.
- **Architecture hardening (committed).** `@Internal` API
  stability marker, public `PdfFragmentRenderHandler` SPI,
  `DocumentRenderingException` on the convenience render path,
  thread-safety contract documented. ADRs 0003 + 0004.
- **Verify gate**: 819 / 0 / 0 / 0 (`mvnw verify`). 26 runnable
  examples regenerate cleanly through `GenerateAllExamples`.

### Architecture hardening (committed in v1.6 line, develop)

The architecture lane closes the highest-severity findings from the
post-1.5 audit. None of these change author-facing behaviour for
unmodified v1.5 code; they sharpen the public-vs-internal boundary,
open extension points, and split the load-bearing files. See
[`docs/roadmaps/migration-v1-5-to-v1-6.md`](docs/roadmaps/migration-v1-5-to-v1-6.md)
for the user-facing summary.

- **`@Internal` API stability marker.** New
  `com.demcha.compose.document.api.Internal` annotation
  (runtime-retained) marks `document.layout.*` and the
  `BuiltInNodeDefinitions` payload records as implementation detail.
  `InternalAnnotationCoverageTest` enforces propagation. ADR 0003
  records the boundary decision.
- **`DocumentRenderingException`** wraps the convenience render path:
  `buildPdf`, `writePdf`, `toPdfBytes`, and the AutoCloseable `close()`
  override no longer declare `throws Exception`. Lower-level backend
  SPIs continue to declare `throws Exception` on purpose.
- **Public PDF render handler SPI.** The
  `PdfFragmentRenderHandler` Javadoc is rewritten as an extension
  point and `PdfFixedLayoutBackend.Builder.addHandler(...)` is the
  new registration path. Custom handlers replace built-in defaults
  by `payloadType()`. ADR 0004 records the SPI shape.
- **Thread-safety contract** documented on
  `document.api/package-info.java` and
  `document.backend.fixed.pdf/package-info.java`.
- **DSL polish.** `DocumentDsl.text()` and `DocumentSession.builder()`
  aliases are `@Deprecated(forRemoval=true, since="1.6.0")`; prefer
  `paragraph()` and `dsl()` respectively.
- **PDF-typed chrome overloads on `DocumentSession`** — `metadata`,
  `watermark`, `protect`, `header`, `footer` accepting
  `Pdf*Options` — are `@Deprecated(forRemoval=true, since="1.6.0")`;
  the canonical backend-neutral overloads are unchanged.
- **`DocumentPalette.builder()`** replaces the positional
  `DocumentPalette.of(Color × 7)` factory; the old factory is
  `@Deprecated(forRemoval=true)`. `BusinessTheme.classic()/modern()/executive()`
  now use the builder. `IllegalStateException` from `build()` names
  every missing token in one message.
- **Targeted layout perf wins** (none alter output bytes):
  `LayoutCompiler.compositeDecorationFragments` /
  `compositeOverlayFragments` no longer wrap with `List.copyOf`,
  `stableZIndexOrder` short-circuits when every layer reports the
  same `zIndex`, `PdfRenderSession` keeps page surfaces in a
  `PDPageContentStream[]` (no `Integer.valueOf` autoboxing),
  `PdfFontLoader.THREAD_LOCAL_TTF_CACHE` is a bounded LRU
  (max 32 entries per thread). Duplicate
  `com.demcha.compose.font.Pdf_FontLoader` deleted.
- **Layout invariant tests.** `LayoutCompilerInvariantsTest` pins
  four scenarios that previously had only transitive snapshot
  coverage: page-advance on overflow, layer source-order under
  uniform `zIndex`, explicit `zIndex` ordering, equal-weight row
  slot distribution.
- **`BuiltInNodeDefinitions` split (Phase E.1).** All 15 built-in
  `NodeDefinition` implementations now live in
  `document.layout.definitions.*` (one file per node type):
  PageBreak, Spacer, Shape, Line, Ellipse, Image, Barcode,
  Container, Section, Row, LayerStack, ShapeContainer, Table,
  Paragraph, List. Shared inline helpers (`EPS`, transform
  wrapping, decoration / table / measurement adapters) live in
  `NodeDefinitionSupport`; the paragraph / list text-flow cluster
  (wrapping, markdown tokenisation, inline-run layout, split
  slicing) lives in the new `TextFlowSupport` helper.
  `BuiltInNodeDefinitions` drops from 3,037 to ~60 lines and now
  only exposes `registerDefaults(NodeRegistry)` as the single
  registration entry point.
- **`PlacementContext` strategy interface (Phase E.4).** A new
  `PlacementContext` sealed interface unifies the placement
  bookkeeping that `LayoutCompiler` helpers need (current page
  index, canvas, prepare/fragment contexts, target lists for placed
  nodes/fragments, and a `canAdvancePage()` / `advancePage()` /
  `touchPage()` strategy). `FixedSlotPlacementContext` pins the page
  for row slots, stacked layers, and atomic leaf placement;
  `MutatingPlacementContext` wraps the live `CompilerState` for
  callers that drive top-down page flow. The previously private
  inner `CompilerState` is lifted to a sibling package-private
  class. `placeStackLayer`, `placeAtomicLeafFragments`, and
  `compileNodeInFixedSlot` now take `PlacementContext` instead of
  six explicit parameters each. Pure refactor — no public API
  change, no behaviour change.
- **`DocumentSession` slim (Phase E.3).** New `SessionFontApi`
  facade (`session.fonts()`) groups
  `registerFontFamily(FontFamilyDefinition)` and
  `registerNodeDefinition(NodeDefinition)` alongside the existing
  `chrome()` and `layout()` facades. Page-background composition
  moves from a private inner method to a new
  `DocumentPageBackgrounds` utility. The four convenience PDF
  methods (`toPdfBytes`, `writePdf`, `buildPdf`, `buildPdf(Path)`)
  share a single `wrapPdfRendering` exception-mapping helper instead
  of repeating the same try/catch four times. Javadoc on the
  deprecated PDF-typed chrome overloads is compacted to a single
  `@deprecated` tag. `DocumentSession` drops from 1,024 to ~937
  lines without changing any public method signatures.

### Templates v2 restructure (committed in v1.6 line, develop)

The **biggest change in v1.6** — the canonical template surface
(CV, cover letter, invoice, proposal) was rewritten from the ground
up. Old positional / cinematic-monolith composers (`CvTemplateV1`,
`NordicCleanCvTemplate`, `InvoiceTemplateV2`, etc.) replaced with a
four-layer architecture: **Theme tokens → Layout slots → Components
+ Blocks → Spec data**, glued together by per-domain builders that
preset classes wrap into one-liner factories. The result is a
copy-and-tweak preset surface where adjusting one visual decision
takes one method change rather than a fork of a 600-line composer.

**New template package layout** (replaces legacy `templates/builtins`,
`templates/support/cv`, `templates/data/cv`, `templates/theme/CvTheme`):

```
templates/
  api/         DocumentTemplate<S>, SlotMap
  themes/      Spacing, Typography (token records)
  components/  Header, Module, MarkdownText
  blocks/      sealed Block hierarchy:
               ParagraphBlock, BulletListBlock, NumberedListBlock,
               IndentedBlock, KeyValueBlock, MultiParagraphBlock
  decorations/ Spacer, Divider, AccentStrip
  cv/
    layouts/   SingleColumn, TwoColumnSidebar, ThreeColumnMagazine
    presets/   14 flat copy-and-tweak preset classes
    builder/   CvBuilder
    spec/      CvSpec, CvHeader, CvModule
  coverletter/
    layouts/   LetterFormat
    presets/   14 paired letter presets (one per CV preset)
    builder/   CoverLetterBuilder
    spec/      CoverLetterSpec, CoverLetterHeader
  invoice/
    presets/   ModernInvoice (minimal v2 surface)
    builder/   InvoiceBuilder
    spec/      InvoiceSpec
  proposal/
    presets/   ModernProposal (minimal v2 surface)
    builder/   ProposalBuilder
    spec/      ProposalSpec
```

**14 CV presets**: `ModernProfessional`, `NordicClean`,
`ClassicSerif`, `CompactMono`, `Executive`, `EngineeringResume` (was
`TechLeadCvTemplate`), `TimelineMinimal`, `BoxedSections`,
`CenteredHeadline`, `BlueBanner`, `EditorialBlue`, `Panel` (was
`ProductLeaderCvTemplate`), `SidebarPortrait`, `MonogramSidebar`.
Each is one final class with one `create(BusinessTheme)` factory:

```java
import com.demcha.compose.document.templates.cv.presets.ModernProfessional;
import com.demcha.compose.document.templates.cv.spec.CvSpec;
import com.demcha.compose.document.theme.BusinessTheme;

DocumentTemplate<CvSpec> template = ModernProfessional.create(BusinessTheme.modern());
template.compose(session, mySpec);
```

**Inline markdown rich text** — body strings carrying
`**bold**` and `*italic*` markers render with proper
{@code DocumentTextDecoration} via the new
`templates.components.MarkdownText` parser. Lets an LLM emit a
resume bullet like `**Java 21**, SQL, Kotlin` and the preset
renders Java 21 in bold without separate inline-run construction.

**Active hyperlinks** — header email + LinkedIn / GitHub labels
become clickable mailto: / https: hyperlinks via
`DocumentLinkOptions` on per-run inline runs.

**Slot-based layouts** — multi-column CV presets
(`Panel`, `SidebarPortrait`, `MonogramSidebar`) declare named
slots (`MAIN`, `SIDEBAR`); a custom preset can rearrange which
modules go into which slot via `.place(slot, "Module Name", ...)`.

**Layout snapshot tests** lock the rendered tree of every preset
(28 baselines under
`src/test/resources/layout-snapshots/canonical-templates/cv-v2/`
and `.../coverletter-v2/`).

**Examples** — `CvTemplateGalleryFileExample` renders all 14 v2
CV presets to `examples/target/generated-pdfs/cv-<id>.pdf`; new
`CoverLetterTemplateGalleryFileExample` renders all 14 paired
letter presets to `cover-letter-<id>.pdf`.

**Migration**: legacy classes have been **deleted**, not
deprecated. Anyone on
`new CvTemplateV1()` / `new NordicCleanCvTemplate()` / etc. must
switch to the new factory:

| Old | New |
|---|---|
| `new CvTemplateV1()` | `ModernProfessional.create(BusinessTheme.modern())` |
| `new NordicCleanCvTemplate()` | `NordicClean.create(BusinessTheme.modern())` |
| `CvTheme.defaultTheme()` | `BusinessTheme.modern() + Spacing.compact()` |
| `CvTemplate` interface | `DocumentTemplate<CvSpec>` |

`InvoiceTemplateV2` and `ProposalTemplateV2` (cinematic) remain
in `templates/builtins/` as the recommended path for fully-styled
output; the new `ModernInvoice` / `ModernProposal` v2 presets
provide the canonical builder seam, with cinematic feature parity
landing in a follow-up.

#### Phase E.1 reopen — visual parity recovery (May 2026)

The first Phase E.1 pass shipped visually-broken renders.
Every CV preset rendered as a teal-tinted single-column
ModernProfessional clone — `NordicClean` lost its sidebar and
soft-tinted PROFILE panel, `BlueBanner` lost its full-width
section banners, `MonogramSidebar` lost its monogram badge,
`SidebarPortrait` lost its portrait sidebar, `ClassicSerif`
lost its two-page editorial structure, and so on. The
`ModernProfessionalVisualParityTest` was a smoke test
(`assertThat(output).exists()`) and `PresetLayoutSnapshotTest`
recorded baselines from the new (broken) v2 renders without
comparing against V1, so the regressions sailed through CI.

**Phase E.1 was reopened.** All 14 CV preset renders + 14
cover-letter pair renders were rebuilt against the V1 visual
references (committed `assets/readme/examples/cv-*.pdf` for
the 7 presets that had a baseline; V1 source code under
`docs/private/v1-reference/` — gitignored — for the rest).
The author-facing API stays stable; only the rendered output
changed.

- **Adaptive sidebar fill** — `SidebarPortrait` and
  `MonogramSidebar` size the trailing spacer dynamically
  from `canvas().innerHeight()` so the SIDEBAR_BG fill
  reaches the bottom of the page on A4 / Letter / smaller
  test fixtures without overflowing the row's page capacity.
- **`Header` API gained three fluent overrides** —
  `withNameStyle(DocumentTextStyle)`,
  `withContactStyle(DocumentTextStyle)`,
  `withLinkStyle(DocumentTextStyle)`. Required for V1-parity
  palette (e.g. slate-blue name + royal-blue underlined
  links for ModernProfessional) — the unstyled
  `Header.rightAligned` rendered names with the active
  `BusinessTheme`'s `h1()` colour instead.
- **`CvHeader.jobTitle` field added** for the subtitle
  rendered under the name by presets that surface it
  (EditorialBlue, Panel, SidebarPortrait, MonogramSidebar).
  Falls back to a placeholder string when the spec leaves it
  empty.
- **Markdown rendering routed through `MarkdownText.parse`
  in every CV / cover-letter preset paragraph body** so
  spec-author bold / italic markers (`**bold**` / `*italic*`)
  carry through to the rendered runs — previously the
  paragraphs stripped markdown.
- **Sample data factory** updated so `Education` / `Projects`
  use `MultiParagraphBlock` with markdown bold prefixes
  (`**MSc Computer Science** - University of Manchester | 2021`)
  rather than `IndentedBlock`'s multi-line shape, and
  `Additional Information` carries `KeyValueBlock` entries
  (Languages / Work Eligibility) for bold-key + plain-value
  rendering.
- **Snapshot baselines regenerated** — 28 `*.json` files
  under
  `src/test/resources/layout-snapshots/canonical-templates/cv-v2/`
  and `coverletter-v2/` updated to lock the V1-parity render
  in place.
- **Pixel-diff visual parity gate landed.**
  `PresetVisualParityTest` (one for CV, one for cover letters)
  rasterises each preset's PDF page 0 (and `classic_serif`'s
  page 1) via PDFBox `PDFRenderer` and asserts per-pixel diff
  against a checked-in baseline PNG with budget 2500 mismatched
  pixels at per-channel tolerance 8 (per
  `templates-restructure-plan.md` sec 6.2). 29 baselines under
  `src/test/resources/visual-baselines/{cv-v2,coverletter-v2}/`.
  Re-bless with `-Dgraphcompose.visual.approve=true`. The
  `PdfVisualRegression` harness was already built; the reopen
  plugged the 28 presets into it. The placeholder
  `ModernProfessionalVisualParityTest` smoke test is deleted.
  `mvnw verify` → 792 / 0 / 0 / 0.

**Tech debt** (deferred to v1.7 as Phase E.4): 13 of the 14
v2 CV presets are implemented as hand-coded
`DocumentTemplate` subclasses driving the canonical
PageFlow DSL directly (≈ 400-700 LOC each) rather than thin
recipes through the slot-based `CvBuilder`. This was an
explicit trade-off during the reopen — restoring V1 visual
fidelity required components the v2 library hadn't grown
yet (`Panel.softTinted`, `TwoColumnSidebar.tinted`,
`SectionStyle.uppercaseRule`, `WorkEntryRenderer`). The
component library extension + preset refactor is tracked in
`docs/private/templates-restructure-plan.md` Phase E.4 and
`docs/private/templates-v2-audit-remediation.md`.

### Feature scope

- **Nested list ergonomics (Phase A — landed).**
  `ListBuilder.addItem(String label, Consumer<ListBuilder> body)`
  appends a nested item with a builder-callback child scope. New
  `ListItem` record carries `(label, marker, children)`. `ListNode`
  gains a `nestedItems` component (record now has 12) and a
  back-compat 11-component constructor matching the v1.5 shape.
  Per-depth marker resolution: item-level marker wins, then
  `ListBuilder.markerFor(int depth, ListMarker)` overrides, then
  the built-in cascade (`•` → `◦` → `▪` → `·`). The internal
  `usedNestedAuthoring` flag preserves source order across mixed
  flat / nested entries — flat-only callers still get the v1.5
  flat `ListNode`. Layout flattens the tree depth-first into
  indent-prefixed paragraph fragments using non-breaking spaces
  (`U+00A0`) for the per-depth indent so the paragraph wrap
  pipeline preserves them. ADR
  [0012](docs/adr/0012-nested-list-evolution.md) records the
  `ListNode`-extension-vs-new-`NestedListNode` decision.
  Snapshot baseline:
  `src/test/resources/layout-snapshots/document/nested_list_three_levels.json`.
  `mvnw verify` → 804 / 0 / 0 / 0.
- **Composed table cell content (Phase B — landed).**
  `DocumentTableCell.node(DocumentNode)` factory accepts any
  composable canonical node as cell content; `DocumentTableCell`
  gains a 5th component `DocumentNode content` with explicit
  4-arg / 3-arg / 2-arg back-compat constructors so v1.5
  plain-text callers compile unchanged. `TableLayoutSupport`
  threads `PrepareContext` through `resolveTableLayout` and
  prepares each composed cell's child against the cell's
  resolved inner width before row-height resolution; the
  prepared height feeds the existing two-pass row-height pass.
  `FragmentContext` gains a default
  `emitChildFragments(PreparedNode, FragmentPlacement)` method
  that `DocumentLayoutPassContext` overrides to dispatch through
  the registered `NodeDefinition` — so any node type works
  inside a cell automatically (paragraph, list, layer-stack,
  sub-table). Pagination preserves row-by-row behaviour: a
  composed cell stays atomic on its row, and
  `sliceTablePreparedNode` subsets the prepared-content map to
  the slice's row range while keeping repeat-header keys
  intact. The PDF table render handler is unchanged: it still
  iterates `cell.lines()` (empty for composed cells) and the
  child fragments render through their own already-registered
  handlers. ADR
  [0013](docs/adr/0013-composed-table-cell.md) records the
  extend-vs-new-hierarchy and recursion-vs-special-case
  decisions. Snapshot baseline:
  `src/test/resources/layout-snapshots/document/table_cell_with_paragraph.json`.
  `mvnw verify` → 810 / 0 / 0 / 0.

### Feature scope (continued)

- **Controlled free-canvas (Phase C — landed).** New
  `CanvasLayerNode` atomic composite accepts children at
  explicit `(x, y)` pixel coordinates inside a fixed-size
  bounding box. Coordinates use the screen convention:
  `(0, 0)` is the canvas's top-left, positive `x` extends
  right, positive `y` extends downward. New `CanvasChild`
  record carries `(node, x, y)`. `CanvasLayerBuilder`
  exposes `position(child, x, y)`, `size(width, height)`,
  `clipPolicy(...)` and is plumbed through
  `AbstractFlowBuilder.addCanvas(width, height, Consumer)`.
  `CanvasLayerDefinition` reuses the existing
  `LayerStackNode` placement plumbing — every child anchors
  at `LayerAlign.TOP_LEFT` and the canvas's `(x, y)` maps
  one-to-one onto the stack layout's `(offsetX, offsetY)`.
  Pagination is atomic; clip policy defaults to
  `ClipPolicy.CLIP_BOUNDS` and reuses the
  `ShapeContainerNode` clipping pipeline. The canvas's
  measured size is explicit (independent of children) so
  the surrounding flow reserves a deterministic rectangle.
  ADR [0014](docs/adr/0014-controlled-absolute-placement.md)
  records why `CanvasLayerNode` is a separate node and why
  absolute placement is rejected as a global policy on
  `RowBuilder` / `SectionBuilder`. Snapshot baseline:
  `src/test/resources/layout-snapshots/document/canvas_layer_basic.json`.
  Showcase: `examples/.../CanvasLayerExample.java`.
  `mvnw verify` → 819 / 0 / 0 / 0.

### Deferred to v1.7

These were on the v1.6 stretch list and did not land in time;
they carry over to v1.7.

- **Phase D — Real PPTX semantic export.** Build out
  `PptxSemanticBackend` from the existing manifest skeleton to a
  working POI-based exporter (paragraphs → text boxes, tables →
  PowerPoint tables, sections → slides).
- **Phase E — Maven Central distribution.** Sonatype OSSRH + GPG
  signing + automated deployment on tag push. Primary install
  coordinates switch to `io.github.demchaav:graphcompose:1.7.0`;
  JitPack stays documented as a fallback.
- **Phase F — Benchmark infrastructure modernisation.** Replace the
  custom warmup / measurement harness with `org.openjdk.jmh` for
  JIT-aware measurement, dead-code elimination protection, and proper
  statistical output. Move the benchmark suite (currently in test
  scope: `CurrentSpeedBenchmark`, `ComparativeBenchmark`,
  `ScalabilityBenchmark`, `FullCvBenchmark`, `GraphComposeBenchmark`)
  into a separate `benchmarks/` Maven module mirroring the
  `examples/` pattern, with a self-executing JMH jar built via
  `maven-shade-plugin`. Add a standalone `layoutGraph()`-only
  scenario so the README can publish a true Layout-vs-Render table
  backed by independently measured values rather than stage
  breakdown subtractions. CI Performance Smoke Check switches to the
  new JMH jar; `scripts/run-benchmarks.ps1` becomes a thin wrapper so
  the documented workflow keeps working.

### Non-goals

- No revival of `GraphCompose.pdf(...)` or public `EntityManager`.
- No nested rows or nested tables inside `RowBuilder` (preserves
  pagination contract).
- No DOCX path-clipping or transform support (Apache POI limit).
- No deprecation of v1.4 / v1.5 public records — back-compat
  constructors stay.

---

## v1.5.1 — 2026-05-05

### Dependencies

- **PDFBox 3.0.7.** Bumped from 3.0.5 to 3.0.7 (Apache PDFBox patch
  release with upstream rendering and security fixes). No
  public-API impact for GraphCompose consumers.

### Tooling

- `ShapeContainerVisualRegressionTest` tolerates the cross-platform
  PDF font-rendering drift that surfaces between Windows-rendered
  baselines and the Linux CI runner (~1-2% pixel diff), via a
  calibrated `mismatchedPixelBudget` instead of bit-exact comparison.
- `DocumentationCoverageTest` no longer pins to the structural
  section anchors that the v1.5.0 README slim removed; the guard now
  scans the whole README for canonical-DSL coverage and
  legacy-API leakage in one whole-file pass.

This is a maintenance patch release. There are no public API
changes; v1.5.0 consumers can upgrade with no code changes.

---

## v1.5.0 — 2026-05-04

### Headline — "intuitive"

v1.5 keeps every v1.4 cinematic primitive and turns the canonical
authoring surface into a polished, theme-driven experience. Three new
visual feature pillars — **shape-as-container with clip path**,
**transforms (rotate / scale) + per-layer z-index**, and **advanced
tables** — combine with **two new cinematic templates**
(`InvoiceTemplateV2`, `ProposalTemplateV2`), a **`CvTheme` ↔
`BusinessTheme` bridge** (ADR 0002), six **modernised CV templates**,
and a documentation pass that covers every new primitive with a recipe
and a runnable example. Test count grew from 525 (v1.4.1) to 675 — an
extra +150 tests across the cinematic, transform, table, theme-bridge,
streaming, snapshot, CV-render, and Transformable-leaf-builder surfaces.

v1.5 is **fully source-compatible with v1.4**. Every public record
that grew a new field ships back-compat constructors that default the
new value, so v1.4 callers compile and behave unchanged. See
[`docs/roadmaps/migration-v1-4-to-v1-5.md`](docs/roadmaps/migration-v1-4-to-v1-5.md).

### Public API — visual primitives

- **Shape-as-container.** New `addCircle(diameter, fill, inside)`,
  `addEllipse(w, h, fill, inside)`, and `addContainer(...)` shortcuts
  on `AbstractFlowBuilder` build a `ShapeContainerNode` whose bounding
  box is dictated by a `ShapeOutline` (`Rectangle`,
  `RoundedRectangle`, `Ellipse`, plus a `circle(diameter)` factory).
  Children are clipped via the new `ClipPolicy` enum
  (`CLIP_PATH` — default — / `CLIP_BOUNDS` / `OVERFLOW_VISIBLE`). The
  PDF backend honours every clip policy via graphics-state
  `saveGraphicsState() + clip(path)` markers; the DOCX backend renders
  layers inline without the outline frame and logs a one-time
  `docx.export.shape-container-fallback` capability warning.
  `ShapeContainerBuilder` exposes the same nine-point alignment
  vocabulary as `LayerStackBuilder` plus `position(node, dx, dy,
  anchor)` for screen-space nudges.
- **Transforms (rotate / scale).** New
  `com.demcha.compose.document.style.DocumentTransform` value type
  with `rotate(deg)`, `scale(uniform)`, `scale(sx, sy)` factories
  plus `withRotation(...)` / `withScale(...)` axis-preserving copies
  and an `isIdentity()` helper. New
  `com.demcha.compose.document.dsl.Transformable<T>` mixin exposes
  `transform(...)`, `rotate(...)`, `scale(...)` as default methods.
  Every shape-shaped builder opts in: `ShapeContainerBuilder`,
  `ShapeBuilder`, `LineBuilder`, `EllipseBuilder`, `ImageBuilder`,
  `BarcodeBuilder`. `rotate(...).scale(...)` chain naturally and pivot
  around the placement centre. The PDF backend issues
  `saveGraphicsState() + cm(matrix)` around each transformed leaf
  (rotation is negated on the way out so the engine's clockwise
  convention matches PDF native counter-clockwise). Identity
  transforms short-circuit and emit no markers, so layout snapshots
  for default-configured nodes are byte-identical to v1.4.
- **Per-layer z-index.** `LayerStackNode.Layer` and shape-container
  layers gain `int zIndex` (default `0`).
  `LayerStackBuilder.layer(node, align, zIndex)` /
  `position(node, dx, dy, align, zIndex)` and the matching
  `ShapeContainerBuilder` overloads let a layer declared earlier draw
  on top of layers declared later. The layout compiler stable-sorts
  layers before render; equal `zIndex` keeps source order.

### Public API — advanced tables

- `DocumentTableCell.rowSpan(int)` mirrors the existing
  `colSpan(int)`. Cells compose freely:
  `DocumentTableCell.text("Tall").colSpan(2).rowSpan(3)`. The layout
  layer skips occupied grid positions when interpreting subsequent
  source rows; misalignments (missing cell, extra source cell,
  overlapping span, span exceeding remaining rows) raise precise
  diagnostics.
- `TableBuilder.zebra(odd, even)` paints alternating row fills.
  Available as `(DocumentTableStyle, DocumentTableStyle)` and as a
  `(DocumentColor, DocumentColor)` overload. Either argument may be
  `null` to skip painting that parity. Existing entries in the
  `rowStyles` map (`headerStyle(...)`, `rowStyle(idx, ...)`,
  `totalRow(...)`) always win over zebra alternation.
- `TableBuilder.totalRow(values)` adds a totals row with a default
  bold-on-grey-blue style; `totalRow(style, values)` is the
  customisable form.
- `TableBuilder.repeatHeader()` / `repeatHeader(rowCount)` re-emits
  the configured leading rows at the top of every continuation page
  when a table paginates. Default is `0` so existing tables paginate
  exactly as before.
- `TableBuilder.headerRow(values)` is a naming alias for
  `header(...)` so authors writing
  `headerRow(...).row(...).totalRow(...)` keep a parallel vocabulary.

### Public API — templates and themes

- **`InvoiceTemplateV2`** is the cinematic invoice counterpart to
  `InvoiceTemplateV1`. Two constructors: the no-arg form picks
  `BusinessTheme.modern()`, the one-arg
  `InvoiceTemplateV2(BusinessTheme)` accepts any theme. Hero
  `softPanel` carrying invoice number / dates / inline rich-text
  status, a two-column row with `From` / `Bill to` parties, themed
  line-items table with `headerStyle` / zebra / totals /
  `repeatHeader()`, and a footer row with `accentLeft` strips on the
  notes / payment-terms columns.
- **`ProposalTemplateV2`** is the proposal counterpart, sharing the
  same `BusinessTheme`-driven composition: hero panel rounded only on
  the right (via the new `DocumentCornerRadius.right(...)` form),
  themed executive-summary panel, sender / recipient parties row,
  sections rendered through `theme.text().h2()` headings, a timeline
  table (Phase / Duration / Details), and a pricing table (Item /
  Description / Amount) with `repeatHeader()`, zebra rows, and a
  total-pricing row anchored at the bottom via `totalRow(...)`.
- **`CvTheme.fromBusinessTheme(BusinessTheme)`** static factory
  derives a CV theme from a business theme (ADR 0002). The bridge
  maps palette / text-scale slots into `primaryColor` /
  `secondaryColor` / `bodyColor` / `accentColor` / `headerFont` /
  `bodyFont` / font sizes; CV-specific layout tokens (`spacing`,
  `moduleMargin`, `spacingModuleName`) keep the existing CV defaults.
  The ten existing CV templates and `CvTemplateV1` continue to work
  unchanged.
- **Six CV templates modernised** to v1.5 idioms:
  `BlueBannerCvTemplate`, `BoxedSectionsCvTemplate`,
  `CenteredHeadlineCvTemplate`, `MonogramSidebarCvTemplate`,
  `SidebarPortraitCvTemplate`, `TimelineMinimalCvTemplate`. Each
  gains a `(CvTheme)` constructor and keeps a no-arg one whose
  default theme matches the legacy palette/font choices, so default-
  constructed instances render identical-page-count PDFs to v1.4.
  `accentTop` / `accentBottom` replace the old
  `addLine(horizontal=innerWidth)` separators around section banners,
  and `softPanel(...)` collapses the
  `padding(asymmetric) + fillColor(...)` cascade.
- `InvoiceTemplateV1` and `ProposalTemplateV1` continue to ship
  side-by-side. Authors who want the cinematic look opt in by
  switching the type.

### Public API — DSL ergonomics (Phase A)

- `LayerStackBuilder` exposes nine alignment shortcuts (`topLeft`,
  `topCenter`, `topRight`, `centerLeft`, `center`, `centerRight`,
  `bottomLeft`, `bottomCenter`, `bottomRight`) on top of `back` /
  `center` so authors do not need to remember the full `LayerAlign`
  enum.
- `LayerStackBuilder.position(node, offsetX, offsetY, anchor)` nudges
  a layer from its anchor by an on-screen offset (positive `offsetX`
  = right, positive `offsetY` = down).
- `AbstractFlowBuilder` gains five convenience overloads on top of
  the v1.4 surface: `addShape(w, h, fill)`,
  `addEllipse(diameter, fill)`, `addEllipse(w, h, fill)`,
  `addCircle(diameter, fill)`, `addImage(data, w, h)`.
- `RowBuilder.spacing(double)` is the canonical name for horizontal
  child spacing; `RowBuilder.gap(double)` becomes a deprecated alias
  (`@Deprecated(since = "1.5.0")`) that delegates to `spacing(...)`.
- `RowBuilder.add(node)` validates the child type **eagerly** and
  raises `IllegalArgumentException` from the offending call site
  instead of deferring to `build()` and raising
  `IllegalStateException` later.
- `DocumentDsl.richText(Consumer<RichText>)` is a new callback entry
  point that builds a `RichText` run sequence in one fluent call.

### Architecture

- New `NodeDefinition.emitOverlayFragments(...)` hook complements the
  existing `emitFragments(...)`. It exists for paired begin/end
  marker pairs (clip-begin/end, transform-begin/end) so the layout
  compiler can emit a single flat fragment sequence
  `[transform-begin → outline → clip-begin → … layers … → clip-end →
  transform-end]` in one pass. Most node types inherit the empty
  default and need no changes.
- New marker payloads on `BuiltInNodeDefinitions`:
  `ShapeClipBeginPayload` / `ShapeClipEndPayload` (carry outline +
  policy + owner path), `TransformBeginPayload` /
  `TransformEndPayload`. PDF render handlers ship alongside:
  `PdfShapeClipBeginRenderHandler`, `PdfShapeClipEndRenderHandler`,
  `PdfTransformBeginRenderHandler`, `PdfTransformEndRenderHandler`,
  registered in `PdfFixedLayoutBackend.defaultHandlers()`.
- New `PaginationPolicy.SHAPE_ATOMIC` distinguishes shape-clipped
  atomicity from bbox-only `ATOMIC` for snapshots and render
  handlers. Oversized containers raise the existing
  `AtomicNodeTooLargeException` with the offending semantic name.
- `TableLayoutSupport` replaces the per-row `colSpan`-sum check with
  a unified cell-grid pre-pass driven by an occupancy mask. The new
  `buildLogicalRows(node, columnCount)` walks columns left-to-right,
  skipping positions covered by a prior row's spanning cell.
  `LogicalCell` carries the cell's full
  `(startRow, startColumn, colSpan, rowSpan, content)` extent.
  Row-height resolution is two-pass: single-row first, then spanning
  cells distribute deficit equally across covered rows.
- `TableResolvedCell` gains `double yOffset` (eighth field). Spanning
  cells use a NEGATIVE offset equal to the cumulative height of the
  rows below the starting row, so the cell's rectangle extends
  downward through the rows it merges instead of upward beyond the
  starting row. Both PDF row-render handlers honour the offset.
- `TableNode` gains a 12th field `int repeatedHeaderRowCount`
  (default `0`). `TableDefinition.split` honours the field: the tail
  slice is built with `prependHeaderRowCount = headerCount` so each
  continuation carries the header at the top.
- `LayoutCompiler.compileStackedLayer` and the STACK branch of
  `compileNodeInFixedSlot` compute a stable `iterationOrder`
  permutation via `stableZIndexOrder(...)` before iterating the
  layer list. Stable on ties → equal `zIndex` keeps source order.
- `BuiltInNodeDefinitions.PreparedStackLayout` gains a fourth list
  `zIndices: List<Integer>` populated by both
  `ShapeContainerDefinition` and `LayerStackDefinition`.
- New ADR `docs/adr/0001-shape-as-container.md` records the
  "separate semantic type" decision (rejected: a clip flag on the
  existing `LayerStackNode` record).
- New ADR `docs/adr/0002-theme-unification.md` records the phased
  approach to `CvTheme` ↔ `BusinessTheme` (rejected: a common
  `Theme` interface that loses CV-specific vocabulary).

### Examples

The runnable `examples/` module gains six new showcases hooked into
`GenerateAllExamples`:

- `ShapeContainerExample` — circles, ellipses, rounded cards with
  clipped layers (`ClipPolicy.CLIP_PATH`).
- `TransformsExample` — three-circle rotate row (15° / -15° / no
  tilt), three-card scale row (`scale(0.7)`, `scale(1.1, 0.85)`,
  identity), and a z-swap stage where a RED square declared first
  with `zIndex = 10` draws on top of a TEAL square declared second.
- `TableAdvancedExample` — hero callout, a 3-row spanning side note,
  and a 36-row invoice with bold-on-teal repeating header, zebra
  body rows, and a gold totals row.
- `CustomBusinessThemeExample` — a hand-built "Studio Emerald"
  `BusinessTheme` constructed from raw `DocumentPalette` /
  `SpacingScale` / `TextScale` / `TablePreset` records (no factory
  shortcut), feeding `InvoiceTemplateV2`.
- `HttpStreamingExample` — `writePdf(OutputStream)` for Servlet /
  S3 / GCS adopters. Includes a Spring Boot `@RestController`
  snippet in the Javadoc and a `TrackingOutputStream` test that
  proves the caller's stream is **not** closed.
- `LayoutSnapshotRegressionExample` — full
  compose → `layoutSnapshot()` → `LayoutSnapshotJson.toJson(...)`
  workflow with a copy-and-paste baseline / drift-report pattern,
  plus a pointer to the production
  `LayoutSnapshotAssertions.assertMatches(document, "...")` helper
  for in-test usage.
- `WeeklyScheduleFileExample` rewritten to delegate to a new reusable
  `examples/support/WeeklyScheduleRenderer`. The renderer's typed
  surface — `JobTitle` enum, `StaffMember` / `DayPlan` / `Shift`
  records, sealed-interface `Half` and `DayShift` types with factory
  methods (`DayShift.OFF`, `.acrossDay(start, end, ShiftType.STOCK)`,
  `.shifts(lunchStart, lunchEnd, dinnerStart, dinnerEnd)`,
  `.lunchOnly(...)`, `.dinnerOnly(...)`,
  `.halves(Half.shift(...), Half.STANDBY)`) — replaces the cryptic
  string tokens used previously. `Theme` (with `aurora()` default and
  a per-`ShiftStatus` colour map) and `Layout` (page size + margin +
  column widths) records keep every colour and dimension out of the
  renderer's static state, so re-skinning the schedule is a
  swap-one-record call. Auto-fills the seven day labels from a
  `LocalDate weekStart`, sorts staff by `JobTitle.ordinal()`, and
  emits a separator row at every job-title boundary so adding or
  removing a `StaffMember` never requires updating positional indices.
  The example file shrinks from ~700 lines of literal data to ~180
  lines of typed declarations.

### Documentation

- README quick-start refreshed to open with a
  `BusinessTheme.modern()`-driven hero (`softPanel` + `accentLeft` +
  `theme.text().h1()`); the plain-text DSL stays underneath for
  callers who do not want a theme.
- New "v1.5 sample renders (PDF)" section links six committed PDFs
  under `assets/readme/v1.5/` so the README works without running
  anything.
- New [`examples/README.md`](examples/README.md) examples gallery —
  every example listed with description, key code snippet, committed
  PDF preview, and source link, grouped by category (built-in
  templates / cinematic templates / v1.5 feature showcases / public-
  API surface / production patterns / operational documents).
  Committed PDF previews of all 22 examples live under
  [`assets/readme/examples/`](assets/readme/examples/) (whitelisted in
  `.gitignore`) so users can browse renders straight from GitHub
  without running anything.
- New [`docs/templates/v1-classic/authoring.md`](docs/templates/v1-classic/authoring.md) (~620
  lines) — the canonical cheatsheet covering builder hierarchy, a
  per-builder one-liner cheatsheet, a style-types reference, the
  theme system in 60 seconds, six golden patterns, ten anti-patterns,
  a 40-line `StatusReportTemplateV1` skeleton, and a "where to look
  next" map.
- New recipes:
  - [`docs/recipes/shape-as-container.md`](docs/recipes/shape-as-container.md)
  - [`docs/recipes/transforms.md`](docs/recipes/transforms.md)
  - [`docs/recipes/tables.md`](docs/recipes/tables.md) (row span /
    zebra / totals / repeating header)
  - [`docs/recipes/shapes.md`](docs/recipes/shapes.md) (filled cards,
    dividers, spacers, lines, ellipses, image fit, soft panels)
  - [`docs/recipes/extending.md`](docs/recipes/extending.md)
- [`docs/recipes.md`](docs/recipes.md) is now a pure index linking
  every topic-focused recipe page plus four 5-line "common DSL
  primitives" starter snippets.
- [`docs/architecture/canonical-legacy-parity.md`](docs/architecture/canonical-legacy-parity.md)
  gains a "Shape-as-container (clipped)" row recording the DOCX
  fallback rule.
- New [`docs/roadmaps/migration-v1-4-to-v1-5.md`](docs/roadmaps/migration-v1-4-to-v1-5.md)
  — fresh migration guide for v1.4 consumers.

### Performance — v1.5 baseline

`CurrentSpeedBenchmark` smoke profile (single-thread, 30 warmup +
100 measurement iterations per scenario) recorded on Java 21,
Windows 11. All five scenarios are well within healthy production
ranges.

| Scenario | Avg ms | p50 ms | p95 ms | Docs/sec | Peak MB |
|---|---:|---:|---:|---:|---:|
| `engine-simple`        |  2.25 |  1.96 |  4.20 | 444.60 |  22 |
| `invoice-template` (V1) | 13.39 | 13.12 | 17.55 |  74.67 | 182 |
| `cv-template` (V1)     |  6.94 |  6.58 | 10.18 | 144.02 |  78 |
| `proposal-template` (V1) | 15.77 | 15.50 | 18.31 |  63.43 | 182 |
| `feature-rich`         | 36.80 | 32.06 | 35.51 |  27.18 |  94 |

Stage breakdown (median ms per stage):

| Scenario | Compose | Layout | Render | Total |
|---|---:|---:|---:|---:|
| invoice-template       | 0.249 | 2.774 | 6.042 |  9.312 |
| cv-template            | 0.173 | 2.343 | 1.544 |  4.087 |
| proposal-template      | 0.256 | 8.715 | 5.345 | 14.563 |

The smoke profile is single-thread by design; throughput numbers
reflect "one document at a time" latency, not concurrent throughput.
The formal "no >5% regression" gate first activates between this
baseline and the next snapshot.

### Tests

- 675/675 green (was 525 on v1.4.1) — +150 new tests across:
  - shape-clip-path fragment ordering and pagination invariants
    (`ShapeContainerBuilderTest`, `ShapeContainerInvariantsTest`)
  - transform mixin contract and CTM checks
    (`DocumentTransformTest`, the
    `everyTransformBeginInArbitraryDocumentHasMatchingEndOnSamePage`
    architecture-guard test)
  - per-layer z-index ordering and stable-tie behaviour
    (`ShapeContainerZIndexDemoTest` plus the two zIndex cases on
    `ShapeContainerBuilderTest`)
  - table row-span / zebra / totals / repeating-header invariants
    (`TableBuilderRowSpanTest`, `TableBuilderZebraAndTotalsTest`,
    `TableBuilderRepeatHeaderTest`)
  - `InvoiceTemplateV2` / `ProposalTemplateV2` invariants and three-
    theme demo renders
    (`InvoiceTemplateV2Test`, `InvoiceTemplateV2DemoTest`,
    `ProposalTemplateV2Test`, `ProposalTemplateV2DemoTest`)
  - custom `BusinessTheme` end-to-end
    (`CustomBusinessThemeDemoTest`)
  - HTTP streaming contract (`HttpStreamingDemoTest` —
    no-close-on-caller invariant)
  - layout-snapshot determinism
    (`LayoutSnapshotRegressionDemoTest`)
  - `CvTheme.fromBusinessTheme` mapping
    (`CvThemeBusinessThemeAdapterTest`)
  - six modernised CV templates rendered to file at expected page
    counts (`CvTemplateRenderTest`)
  - `Transformable<T>` contract pinned for every leaf builder that
    opted in (`TransformableLeafBuildersTest`): default identity
    transform, `rotate(...)` / `scale(...)` propagation, identity
    short-circuit emits no markers, non-identity wraps the leaf
    payload with matching transform-begin / transform-end carrying the
    same owner path

### Migration from v1.4.x

- `RowBuilder.gap(double)` is deprecated in favour of
  `spacing(double)`. The deprecated alias still compiles; CV
  templates and runnable examples were migrated.
- `RowBuilder.add(node)` now throws `IllegalArgumentException`
  eagerly. Tests that asserted the deferred `IllegalStateException`
  in `build()` must switch their expectation.
- All other v1.4 record signatures stay backward-compatible:
  `LayerStackNode.Layer`, `ShapeContainerNode`, `TableNode`,
  `DocumentTableCell`, `TableResolvedCell`, and
  `BuiltInNodeDefinitions.PreparedStackLayout` ship new canonical
  constructors *and* preserve every existing constructor as a back-
  compat shim that defaults the new fields. `InvoiceTemplateV1` and
  `ProposalTemplateV1` ship side-by-side with the V2 templates;
  callers who want the cinematic look opt in by switching the type.

See [`docs/roadmaps/migration-v1-4-to-v1-5.md`](docs/roadmaps/migration-v1-4-to-v1-5.md)
for the full guide.

---

## v1.4.1 - 2026-04-27

### Documentation

- README rewrite for v1.4.0 dropped three structural sections (`## Table component`, `## Line primitive`, `## Architecture at a glance`) that the `DocumentationCoverageTest` guards baseline. CI flagged the regression on the `main` branch; v1.4.1 restores the sections (the table snippet now also points readers to the new column-span feature), keeps the canonical-DSL anti-patterns out of the snippets, and moves the architecture mermaid diagram back into its dedicated section.

### Tooling

- `examples/src/main/java/com/demcha/examples/GenerateAllExamples.java` now wires `CinematicProposalFileExample.generate()` into the orchestrator, so the runnable examples module produces all seven fixtures (including `project-proposal-cinematic.pdf`) used by the README visual previews.

This is a documentation-only patch release. There are no public API changes; v1.4.0 consumers can upgrade with no code changes.

---

## v1.4.0 - 2026-04-27

### Headline — "cinematic document engine"

v1.4 closes the visual-design gap that the previous releases left open. Tables can now span columns, layers can stack on top of each other, sections and pages carry semantic backgrounds, paragraphs accept fluent rich text, and the whole look-and-feel can be parametrised through a single `BusinessTheme`. The release also lands the visual-regression scaffolding required to keep README screenshots stable across refactors.

### Public API — semantic primitives

- `DocumentTableCell` is now a 3-field record (`lines`, `style`, `colSpan`). The new `colSpan(int)` factory plus `withColSpan(...)` on `TableCellContent` let one cell occupy several columns; sum-of-spans-per-row is validated by `TableLayoutSupport`. Border ownership and natural-width distribution understand spans (extra width is shared across `auto` columns inside the span; an all-fixed span throws when it cannot fit). Renderer code is unchanged — spanned cells emit a single `TableResolvedCell` with the merged width.
- new `LayerStackNode` + `LayerAlign` primitive composes children inside the same bounding box, in source order (first behind, last in front). Each layer carries one of nine alignments (`TOP_LEFT … BOTTOM_RIGHT`). Pagination is atomic. Backed by a new `Axis.STACK` in `CompositeLayoutSpec` and a `compileStackedLayer` branch in `LayoutCompiler`. DSL surface: `LayerStackBuilder` with `back(...)`, `center(...)`, `layer(node, align)`.
- `DocumentSession.pageBackground(DocumentColor | Color)` (and the matching `GraphCompose.DocumentBuilder` setter) injects a full-canvas `ShapeFragmentPayload` at the start of every page. Combine with `LayerStackNode` for cinematic hero pages without any backend changes.
- `AbstractFlowBuilder` gains semantic shortcuts on every flow / section / module: `band(color)`, `softPanel(color)` / `softPanel(color, radius, padding)`, and `accentLeft / accentRight / accentTop / accentBottom(color, width)`. They reuse the existing `fillColor`, `cornerRadius`, `padding`, and `DocumentBorders` plumbing — the new methods are sugar for designer-style flows.
- `RichText` fluent builder (`document.dsl.RichText`) plus `ParagraphBuilder.rich(...)` / `AbstractFlowBuilder.addRich(...)` cover the `Status: Pending` label/value pattern in one expression: `RichText.text("Status: ").bold("Pending").color("…", red).accent("…", brand)`. Includes `plain / bold / italic / boldItalic / underline / strikethrough / color / accent / size / style / link / append`.

### Public API — design tokens

- new `com.demcha.compose.document.theme` package — entirely on top of public document-level types, no engine leaks.
  - `DocumentPalette` — primary / accent / surface / surfaceMuted / textPrimary / textMuted / rule
  - `SpacingScale` — five-step `xs / sm / md / lg / xl` with monotonicity validation and `insetsXs() … insetsXl()` helpers
  - `TextScale` — `h1 / h2 / h3 / body / caption / label / accent` resolved styles
  - `TablePreset` — `defaultCellStyle / headerStyle / totalRowStyle / zebraStyle`
  - `BusinessTheme` — composes the four scales plus an optional page background, with three built-in presets (`classic()`, `modern()` cream paper + teal, `executive()` slate panels with Times-Roman headings) and immutable `withName / withPageBackground` forks

### Testing infrastructure

- `com.demcha.testing.visual.ImageDiff` — pixel-by-pixel comparison with per-channel tolerance and a red/grey diff image.
- `com.demcha.testing.visual.PdfVisualRegression` — renders PDF bytes to one PNG per page via `PdfRenderBridge` and compares against baselines under `src/test/resources/visual-baselines`. Approve mode (`-Dgraphcompose.visual.approve=true` or `GRAPHCOMPOSE_VISUAL_APPROVE=true`) writes new baselines; comparison failures drop `actual.png` and `diff.png` next to the baseline for inspection.
- 41 new tests across the cinematic surfaces (`TableColSpanIntegrationTest`, `TableBuilderColSpanTest`, `LayerStackBuilderTest`, `PageBackgroundTest`, `SectionPresetTest`, `RichTextTest`, `BusinessThemeTest`, `PdfVisualRegressionTest`). Total green count: **525**.

### Architecture

- `CompositeLayoutSpec.Axis.STACK` joins `VERTICAL` and `HORIZONTAL`. The compiler dispatches `STACK` to `compileStackedLayer`, which positions each child inside the stack box via per-layer alignment offsets and shares the same `compileNodeInFixedSlot` plumbing rows already use.
- table layout (`TableLayoutSupport`, test-side `TableBuilder`) was rewritten around a "logical cell" model: each authored cell is one `LogicalCell(startColumn, colSpan, content)` resolved against a `stylesGrid[row][col]` — the grid keeps existing border-ownership logic intact while letting render code keep emitting one `TableResolvedCell` per logical cell.
- `DocumentSession.layoutGraph()` now wraps `compiler.compile(...)` with `withPageBackgrounds(...)` so backends never need to know about the page-background option — they just iterate fragments as usual.

### Performance

- Cinematic features have negligible overhead: page-background injection is a single fragment per page; column spans, layer stacks, and themes do not change the number of emitted fragments. End-to-end template latency stays in the same envelope as v1.3 once JIT is warm.
- Full benchmark surface is now published in the README: `current-speed` (full profile) latency + per-stage breakdown, parallel throughput on the invoice template (1&rarr;8 threads), `scalability` suite (1&rarr;16 threads, 13.8&times; speedup at 16), 50-thread `stress` test (5,000 docs, 0 errors), and the `comparative` table against iText 5 and JasperReports.

### Documentation

- README rewritten around the cinematic v1.4 narrative: new sections for column spans, layer stacks, page background + section presets, rich text DSL, business themes, the visual-regression workflow, "Extending GraphCompose" guidance, and a refreshed Performance section sourced from `scripts/run-benchmarks.ps1`.

---

## v1.3.0 - 2026-04-27

### Public API

- `DocumentSession` now exposes ergonomic mutators for document-level PDF chrome: `metadata(...)`, `watermark(...)`, `protect(...)`, `header(...)`, `footer(...)`, and `clearHeadersAndFooters()`. Convenience entrypoints (`buildPdf`, `writePdf`, `toPdfBytes`) honour these options without having to build a `PdfFixedLayoutBackend` manually
- new horizontal layout primitive: `addRow(...)` on flows, sections, and modules creates a `RowNode` that arranges atomic children left-to-right with optional `weights(...)` and `gap(...)`. Rows are atomic blocks from the paginator's perspective
- `DocumentBorders` value type plus `borders(...)` on flows, sections, modules, and rows let you describe per-side strokes (top / right / bottom / left). Per-side borders override the uniform `stroke(...)` setting
- `ParagraphBuilder.autoSize(maxSize, minSize)` / `autoSize(DocumentTextAutoSize)` searches for the largest font size that fits the paragraph on a single line within the resolved inner width
- new `addLink(text, uri)` and `addLink(text, DocumentLinkOptions)` shortcuts on `AbstractFlowBuilder` for the common single-link case (was previously only available as a paragraph inline run)
- backend-neutral output options under `com.demcha.compose.document.output` (`DocumentMetadata`, `DocumentWatermark`, `DocumentProtection`, `DocumentHeaderFooter`, aggregated by `DocumentOutputOptions`). PDF and DOCX backends translate them; session-level metadata propagates to DOCX core properties as well as the PDF backend
- `DocxSemanticBackend` is now a functional Apache POI based backend that returns DOCX bytes (was a manifest-only skeleton); supports paragraphs, tables, images, spacers, page breaks, and document-level page geometry. Apache POI is declared optional, so consumers that only render PDFs do not pay the dependency cost

### Performance

- `PageBreaker.paginationPriority` pre-computes `(y, depth)` keys and uses `UUID.compareTo` for tie-breaks (no per-compare string allocation). Old comparator allocated a 36-character UUID string for every priority queue compare
- `Entity.getComponent` and `Entity.require` no longer issue per-call debug logging (even guarded `isDebugEnabled` calls cost a volatile read on Logback)
- table layout helpers (`resolveTableLayout`, `sliceTablePreparedNode`, ~350 lines) extracted into `TableLayoutSupport` for clarity
- end-to-end template rendering is 19–30 % faster than v1.2.0 on the canonical benchmark suite (`invoice-template`: 25.77 → 10.77 ms avg; `cv-template`: 17.89 → 6.50 ms avg; `proposal-template`: 24.77 → 13.44 ms avg)

### Benchmark methodology

- `CurrentSpeedBenchmark` smoke profile bumped from 2 / 5 → 30 / 100 warmup / measurement iterations so the JIT reaches a steady state and percentiles are statistically meaningful
- percentile calculation now uses linear interpolation between order statistics (`rank = (n-1) * p`) so p95 no longer collapses to max at small sample counts
- `System.gc()` plus a 50 ms sleep separates warmup from measurement, dropping run-to-run variance from 10–25 % to 2–5 %
- `peakHeapMb` reports the heap delta over the post-warmup baseline rather than absolute used heap
- a per-stage breakdown table (`compose / layout / render / total`) prints alongside the latency table so consumers can attribute regressions to engine layout vs PDFBox serialization
- smoke gate thresholds tightened from 800–2600 ms (effectively a no-op) to 8–100 ms (~3× the observed avg) — still safe against CI machine variance, now catches ≥50 % regressions
- the `ComparativeBenchmark` console table no longer wraps when library names exceed 20 characters

### Architecture

- `CompositeLayoutSpec` carries an explicit `Axis` (vertical / horizontal) and optional per-child weights; the layout compiler dispatches to a dedicated horizontal-row code path for `RowNode`
- `ShapeFragmentPayload` carries an optional `SideBorders` payload; `PdfShapeFragmentRenderHandler` draws each configured side stroke independently of the uniform rectangle stroke
- `SemanticExportContext` carries `DocumentOutputOptions` so semantic backends (DOCX, future PPTX) can apply metadata / chrome configured at the session level
- the unused engine `Button` renderable and the `ButtonBuilder` test-support factory entry were removed
- guide-line overlays now compute owner bounds across sub-fragments (e.g. table rows) and paint margin / padding once around the entire owning node instead of stacking dashed rectangles inside every row

### Documentation

- `docs/architecture/canonical-legacy-parity.md` is updated to reflect the v1.3 capabilities (rows, per-side borders, auto-size text, DOCX export)
- `docs/operations/benchmarks.md` documents the new smoke profile defaults, the GC stabilization point, the linear-interpolation percentile rule, and the stage-breakdown table
- `CONTRIBUTING.md` repository map and package list now describe the canonical functional layout (`document.layout`, `document.backend`, `document.output`) alongside the legacy ECS engine

---

## v1.2.0 - 2026-04-25

### Release identity

- the current canonical API cleanup is being released as **v1.2.0** to match the project's early maturity while still making `GraphCompose.document(...) -> DocumentSession -> DocumentDsl` the preferred authoring path
- Maven coordinates are `io.github.demchaav:graphcompose:1.2.0`; JitPack consumers continue to use `com.github.demchaav:GraphCompose:v1.2.0`
- consumers on `v1.1.x` should adopt the canonical `GraphCompose.document(...)` session-first path; the planned `docs/migration-v1-1-to-v1-2.md` was never written and the canonical surface has stabilised since

### Public API

- `DocumentSession` is now an `AutoCloseable` lifecycle owner: `close()` is idempotent, and authoring/rendering methods on a closed session fail fast with `IllegalStateException` instead of returning broken state
- empty document rendering (`writePdf` / `toPdfBytes` / `buildPdf`) now throws a domain-specific `IllegalStateException` instead of producing a zero-byte / zero-page PDF; add at least one root before rendering
- `DocumentPageSize` is the public page-size value; `GraphCompose.document(...).pageSize(PDRectangle)` was removed from the canonical API
- `DocumentSession#margin(Margin)` and `GraphCompose.DocumentBuilder#margin(Margin)` were removed from the canonical API; use `DocumentInsets` or `margin(top, right, bottom, left)` to keep authoring renderer-neutral
- PDF-specific metadata, protection, watermark, and header/footer options moved behind `PdfFixedLayoutBackend.builder()` instead of the canonical `GraphCompose` / `DocumentSession` surface
- `GraphCompose.document(...).guideLines(true)` and `DocumentSession.guideLines(true)` now enable debug guide-line overlays for `buildPdf`, `writePdf`, and `toPdfBytes` convenience output
- `DocumentSession.layoutSnapshot()` now returns public renderer-neutral `com.demcha.compose.document.snapshot.*` DTOs instead of engine debug types
- `BoxConstraints.natural(width)` is now the canonical natural-measurement factory; `unboundedHeight(width)` remains as a compatibility alias
- the public font registry no longer exposes the unadvertised `getPdfFont(...)` bridge; backend code resolves typed fonts through `getFont(..., PdfFont.class)`

### Architecture guards

- `PublicApiNoEngineLeakTest` baselines the inventory of `com.demcha.compose.engine.*` imports allowed in the public API surface — any new leak fails the build
- `SemanticLayerNoPdfBoxDependencyTest` keeps `document.node.*` free of direct PDFBox imports and pins the remaining `backend.fixed.pdf.options.*` references for Phase 3 cleanup
- `PdfBackendIsolationGuardTest` keeps PDFBox out of canonical API, DSL, semantic nodes, layout, snapshots, and non-PDF backend contracts

### Layout

- `PaginationEdgeCaseTest` adds focused regressions for exact-fit content, near-boundary float handling, leading / trailing page breaks, oversized atomic images, too-tall table rows, module splits with PDF chrome, and nested sections that paginate while preserving margin and padding

### Documentation

- new `docs/migration-v1-1-to-v1-2.md` outlines the move from older v1.1 usage patterns to the canonical session-first API
- new `docs/v1.2-roadmap.md` tracks the remaining stabilization work for the v1.2 release polish window
- `docs/contributing/release-process.md` now describes the current JitPack-first 1.x release flow and runnable examples verification
- user-facing docs now describe debug guide-line overlays through `GraphCompose.document(...).guideLines(true)` / `DocumentSession.guideLines(true)` and call out JitPack tag-cache handling during release verification

---

## v1.1.0 - 2026-04-13

### Highlights

- shifted the public built-in template narrative to `compose(DocumentSession, ...)`
- added document-level PDF features for richer real-world output
- moved the engine further away from PDF-centric internals through backend-neutral composition and render-handler seams
- strengthened architecture guard rails for template scene builders
- expanded visual testing and benchmark tooling for day-to-day development

### Added

- canonical `DocumentSession` contract as the primary composition seam
- layout snapshot extraction and JSON-based regression coverage for resolved document geometry
- runnable `examples/` module for CV, cover letter, invoice, proposal, and weekly schedule generation
- new built-in business templates and data models for invoice, proposal, and weekly schedule documents
- barcode support with QR, Code 128, and EAN-13 builders
- watermark support
- configurable headers and footers with page numbers and separators
- PDF bookmarks / outline generation
- document metadata support
- PDF protection hooks
- explicit page-break and divider builders
- visual showcase render tests for barcodes, QR codes, pagination, and document chrome
- current-speed benchmark suite
- benchmark JSON/CSV export and diff tooling
- one-command benchmark runner: [scripts/run-benchmarks.ps1](./scripts/run-benchmarks.ps1)

### Changed

- bumped the library release to `v1.1.0`
- updated README installation snippets to the new release version
- documented built-in templates as compose-first by default
- refreshed README visuals to show barcode/QR and compose-first template output
- added release-facing notes for the experimental live preview dev tool in test scope
- refreshed release documentation to point contributors at visual tests and benchmark workflows

### Architecture and CI

- engine-side text measurement and rendering dispatch are now more explicitly decoupled from PDFBox-specific implementation details
- added template boundary guard coverage so `*SceneBuilder` classes stay free of backend-specific PDFBox types
- split architecture/documentation guards into a dedicated CI job that can be required independently in branch protection

### Compatibility notes

- older tagged JitPack releases remain usable as long as consumers pin a specific version such as `v1.0.3`
- deprecated `render(...)` template adapters remain available for compatibility, but new docs and examples now prefer `compose(...)`
