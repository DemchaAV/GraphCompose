/**
 * CV Templates v2 — author-facing API.
 *
 * <p>This package is the canonical pattern for building CV-style
 * documents on top of GraphCompose. Below is the conceptual model
 * every author should hold in their head before writing a template.</p>
 *
 * <h2>Four layers, one responsibility each</h2>
 *
 * <pre>
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │  presets/                                                   │
 *   │    BoxedSections      ← composition: data + theme + render  │
 *   │    MinimalUnderlined  ← another composition, same pieces    │
 *   └─────────────────────────────────────────────────────────────┘
 *           │ uses                              │ uses
 *           ▼                                   ▼
 *   ┌──────────────────────┐         ┌──────────────────────────┐
 *   │  components/         │         │  theme/                  │
 *   │    HeadlineRenderer  │ reads   │    CvPalette  (colours)  │
 *   │    ContactRenderer   │◀────────│    CvTypography (fonts)  │
 *   │    BannerRenderer    │         │    CvSpacing  (margins)  │
 *   │    RowRenderer       │         │    CvDecoration (glyphs) │
 *   │    EntryRenderer     │         │    CvTheme    (bundle)   │
 *   │    SectionDispatcher │         └──────────────────────────┘
 *   └──────────────────────┘
 *           │ renders
 *           ▼
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │  data/                                                      │
 *   │    CvIdentity   ← name, contact, optional links             │
 *   │    CvSection    ← sealed: Paragraph | Rows | Entries        │
 *   │    CvDocument   ← identity + ordered sections               │
 *   └─────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>What goes where — when you write your own template</h2>
 *
 * <dl>
 *   <dt><b>{@code data/}</b></dt>
 *   <dd>The author's content. <em>"What is on the CV?"</em>
 *       No colours, no fonts, no sizes. Pure records. If you're
 *       describing a person's job history, this is where you live.</dd>
 *
 *   <dt><b>{@code theme/}</b></dt>
 *   <dd>The cosmetic decisions. <em>"How does it look?"</em>
 *       Colours, fonts, sizes, margins, glyphs. Swap-able without
 *       touching renderers. If you want a navy theme or a different
 *       bullet character, this is where you live.</dd>
 *
 *   <dt><b>{@code components/}</b></dt>
 *   <dd>The reusable drawing primitives. <em>"How is a section
 *       row laid out?"</em> Each renderer takes a host
 *       {@code SectionBuilder}, a data record, and a {@code CvTheme}.
 *       You rarely write a new one — usually you compose the
 *       existing ones in a new preset.</dd>
 *
 *   <dt><b>{@code presets/}</b></dt>
 *   <dd>The compositions. <em>"In what order, what page flow?"</em>
 *       A preset picks a theme and orchestrates renderers. Writing
 *       a new visual style usually means writing a new preset —
 *       not a new renderer.</dd>
 * </dl>
 *
 * <h2>How to write your own template — 4 steps</h2>
 *
 * <h3>Step 1. Build the data</h3>
 *
 * <pre>{@code
 * CvDocument doc = CvDocument.builder()
 *     .identity(CvIdentity.builder()
 *         .name("Jane", "Doe")                       // required: first + last
 *         // .name("Jane", "Quinn", "Doe")            // optional middle
 *         .contact("+44 0", "j@d.com", "London, UK") // required triple
 *         .link("LinkedIn", "https://...")            // optional
 *         .link("GitHub",   "https://...")            // optional
 *         .build())
 *     .section(new ParagraphSection("Professional Summary",
 *         "Backend engineer with **5 years** of..."))
 *     .section(RowsSection.builder("Technical Skills", RowStyle.BULLETED)
 *         .row("Languages", "Java 21, Kotlin")
 *         .row("Testing",   "JUnit 5, AssertJ")
 *         .build())
 *     .section(EntriesSection.builder("Experience")
 *         .entry("Senior Engineer", "Acme Inc", "2022-Present", "Built ...")
 *         .build())
 *     .build();
 * }</pre>
 *
 * <h3>Step 2. Pick or build a theme</h3>
 *
 * <pre>{@code
 * // Take the classic look
 * CvTheme theme = CvTheme.boxedClassic();
 *
 * // ... or customise one piece, keep the rest
 * CvTheme custom = new CvTheme(
 *     CvPalette.classic(),
 *     CvTypography.classic(),
 *     CvSpacing.classic(),
 *     new CvDecoration("▶ ", "  ", "  ·  "));   // ▶ bullets, mid-dot separators
 * }</pre>
 *
 * <h3>Step 3. Hand to a preset</h3>
 *
 * <pre>{@code
 * DocumentTemplate<CvDocument> template = BoxedSections.create(custom);
 * }</pre>
 *
 * <h3>Step 4. Render</h3>
 *
 * <pre>{@code
 * try (DocumentSession session = GraphCompose.document(outputPath)
 *         .pageSize(DocumentPageSize.A4)
 *         .margin(28, 28, 28, 28)
 *         .create()) {
 *     template.compose(session, doc);
 *     session.buildPdf();
 * }
 * }</pre>
 *
 * <h2>Going further — extension recipes</h2>
 *
 * <p>See {@code AUTHORS.md} alongside this package for longer
 * recipes:</p>
 * <ul>
 *   <li>Swap bullet glyph / contact separator / stacked indent</li>
 *   <li>Build a new theme variant (navy / compact / serif)</li>
 *   <li>Write a brand-new preset that reuses existing renderers</li>
 *   <li>Add a brand-new section subtype (compile-checked dispatch)</li>
 * </ul>
 *
 * <h2>What this package is <em>not</em></h2>
 *
 * <ul>
 *   <li>An inheritance hierarchy. Records here are sealed; renderers
 *       are utility classes. <strong>Compose, don't subclass.</strong></li>
 *   <li>A free-form layout engine. The engine lives in
 *       {@code com.demcha.compose.document.engine.*}; this package
 *       is a thin author-facing layer on top of its public DSL.</li>
 *   <li>A replacement for v1 yet. The legacy
 *       {@code com.demcha.compose.document.templates.cv.*} surface is
 *       untouched; both pipelines coexist while v2 stabilises.</li>
 * </ul>
 */
package com.demcha.compose.document.templates.cv.v2;
