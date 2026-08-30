package com.demcha.examples.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hand-curated showcase metadata for {@link ShowcaseSync}. Every
 * generated example PDF has a matching entry here that supplies the
 * card title, one-line description, search tags, and the GitHub
 * permalink to the source code. Examples without an entry fall back
 * to a sensible filename-derived default.
 *
 * <p>Adding a new example:</p>
 * <ol>
 *   <li>Place the example {@code .java} under the right category
 *       sub-package in {@code examples/.../com/demcha/examples/}.</li>
 *   <li>Make it write its PDF via
 *       {@code ExampleOutputPaths.prepare(category, fileName)} so
 *       the output lands under the matching subfolder.</li>
 *   <li>Wire it into {@code GenerateAllExamples.main}.</li>
 *   <li>Register the metadata entry below using the basename of the
 *       generated PDF as the key.</li>
 *   <li>Re-run {@code GenerateAllExamples} then {@code ShowcaseSync}.</li>
 * </ol>
 *
 * @author Artem Demchyshyn
 */
final class ShowcaseMetadata {

    // Tracks the branch / tag the site links into. While v1.6 is being
    // developed on `develop`, links resolve there (the reorg lives only
    // on develop). At release time switch this to the published tag
    // (e.g. "v1.6.0") so users browsing the deployed site land on the
    // exact source that produced the artefacts.
    private static final String GH_BASE = "https://github.com/DemchaAV/GraphCompose/blob/develop";
    private static final String EX_BASE = GH_BASE + "/examples/src/main/java/com/demcha/examples";

    record Entry(String title, String description, List<String> tags, String codeUrl) {
    }

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        // ===== Templates / CV (v2 layered) =====
        cv("cv-modern-professional-v2", "CvModernV2Example", "Modern Professional", "Clean two-column resume with right-aligned header, tinted profile panel, and uppercase section headings.", "minimal");
        cv("cv-nordic-clean-v2", "CvNordicCleanExample", "Nordic Clean", "Sidebar layout with soft-tinted PROFILE panel, Nordic palette, and bullet skill list.", "sidebar");
        cv("cv-classic-serif-v2", "CvClassicSerifExample", "Classic Serif", "Two-page editorial CV with Times-style serif headings and conservative grey rules.", "serif", "two-page");
        cv("cv-compact-mono-v2", "CvCompactMonoExample", "Compact Mono", "Single-column dense layout with monospace contact line — favourite for engineering roles.", "compact", "mono");
        cv("cv-executive-v2", "CvExecutiveExample", "Executive", "Slate palette with prominent name banner, formal tone, and weighted section dividers.", "executive");
        cv("cv-engineering-resume-v2", "CvEngineeringResumeExample", "Engineering Resume", "Tech-lead style layout with prominent skill matrix and stack tagging — was TechLead in v1.5.", "tech");
        cv("cv-timeline-minimal-v2", "CvTimelineMinimalExample", "Timeline Minimal", "Vertical timeline of roles with bullet markers and tight whitespace.", "timeline");
        cv("cv-boxed-sections-v2", "CvBoxedV2Example", "Boxed Sections", "Each section wrapped in a grey banner header — bold, structured feel.", "structured");
        cv("cv-centered-headline-v2", "CvCenteredHeadlineExample", "Centered Headline", "Centered name + role with full-width accent rules between sections.", "centered");
        cv("cv-blue-banner-v2", "CvBlueBannerExample", "Blue Banner", "Light-blue full-width section bands with high-contrast headings.", "banner", "blue");
        cv("cv-editorial-blue-v2", "CvEditorialBlueExample", "Editorial Blue", "Magazine-style editorial layout with two-column body and tinted skills table.", "editorial", "blue");
        cv("cv-panel-v2", "CvPanelExample", "Panel", "Soft-tinted panels per section, Product-Leader feel — was ProductLeader in v1.5.", "panel");
        cv("cv-sidebar-portrait-v2", "CvSidebarPortraitExample", "Sidebar Portrait", "Edge-to-edge grey sidebar with portrait photo, contact stack, and skills.", "sidebar", "portrait");
        cv("cv-monogram-sidebar-v2", "CvMonogramSidebarExample", "Monogram Sidebar", "Sidebar with monogram badge, accent rule, and structured contact + skills column.", "sidebar", "monogram");
        cv("cv-minimal-underlined-v2", "CvMinimalUnderlinedExample", "Minimal Underlined", "Single-column layout with underlined section titles and tight whitespace — minimalist reference shape.", "minimal");
        cv("cv-mint-editorial-v2", "CvMintEditorialExample", "Mint Editorial", "Magazine-style editorial CV with mint accent palette and two-column body.", "editorial", "mint");
        cv("cv-mint-editorial-v2-custom", "CvMintEditorialCustomExample", "Mint Editorial (custom band)", "The same preset with one colour changed through its Options — a kraft-paper masthead band, everything else left at the preset's defaults.", "editorial", "mint");

        // ===== Templates / Cover Letter (v2 layered, paired 1:1 with CV) =====
        // Registered directly: letter() points at the layered preset examples under
        // coverletter/v2, and this one sits a level up because it composes without a
        // preset at all.
        ENTRIES.put("cover-letter", entry("Cover Letter",
                "One page composed straight in the canonical DSL — section presets carry the hierarchy, no template involved.",
                withCategory("letter"),
                EX_BASE + "/templates/coverletter/CoverLetterFileExample.java"));
        letter("cover-letter-modern-professional-v2", "CvModernProfessionalLetterV2Example", "Modern Professional letter", "Letter paired with the Modern Professional CV palette.");
        letter("cover-letter-nordic-clean-v2", "CvNordicCleanLetterV2Example", "Nordic Clean letter", "Letter paired with the Nordic Clean CV palette.");
        letter("cover-letter-classic-serif-v2", "CvClassicSerifLetterV2Example", "Classic Serif letter", "Letter with Times-style serif typography.");
        letter("cover-letter-compact-mono-v2", "CvCompactMonoLetterV2Example", "Compact Mono letter", "Letter with mono accent and compact spacing.");
        letter("cover-letter-executive-v2", "CvExecutiveLetterV2Example", "Executive letter", "Slate-palette executive letter.");
        letter("cover-letter-engineering-resume-v2", "CvEngineeringResumeLetterV2Example", "Engineering letter", "Letter paired with EngineeringResume palette.");
        letter("cover-letter-timeline-minimal-v2", "CvTimelineMinimalLetterV2Example", "Timeline Minimal letter", "Letter with timeline-style minimal accents.");
        letter("cover-letter-boxed-sections-v2", "CvBoxedSectionsLetterV2Example", "Boxed Sections letter", "Letter with grey-banner section headings.");
        letter("cover-letter-centered-headline-v2", "CvCenteredHeadlineLetterV2Example", "Centered Headline letter", "Letter with centered name + accent rules.");
        letter("cover-letter-blue-banner-v2", "CvBlueBannerLetterV2Example", "Blue Banner letter", "Letter paired with Blue Banner CV.");
        letter("cover-letter-editorial-blue-v2", "CvEditorialBlueLetterV2Example", "Editorial Blue letter", "Editorial-magazine letter paired with Editorial Blue CV.");
        letter("cover-letter-panel-v2", "CvPanelLetterV2Example", "Panel letter", "Letter paired with Panel CV soft-tinted panels.");
        letter("cover-letter-sidebar-portrait-v2", "CvSidebarPortraitLetterV2Example", "Sidebar Portrait letter", "Letter paired with Sidebar Portrait CV.");
        letter("cover-letter-monogram-sidebar-v2", "CvMonogramSidebarLetterV2Example", "Monogram Sidebar letter", "Letter paired with Monogram Sidebar CV.");
        letter("cover-letter-mint-editorial-v2", "CvMintEditorialLetterV2Example", "Mint Editorial letter", "Letter paired with Mint Editorial CV — magazine-style mint accent.");

        // ===== Templates / Invoice =====
        invoice("invoice-cinematic", "InvoiceCinematicFileExample", "Cinematic Invoice", "Layered ModernInvoice preset with theme-driven layout, advanced tables, and totals.", "invoice", "cinematic");
        invoice("invoice-modern-v2", "v2/ModernInvoiceV2Example", "Modern Invoice", "The ModernInvoice preset composed straight from an InvoiceDocumentSpec — line items, totals and payment block driven by the BrandTheme rather than per-document styling.", "invoice");
        invoice("invoice-classic-v2", "v2/ClassicInvoiceV2Example", "Classic Invoice", "The ClassicInvoice preset — letterhead header band, TOTAL DUE hero strip, BILL TO / FROM columns, and a dedicated Summary table after the line items.", "invoice");

        // ===== Templates / Proposal =====
        proposal("proposal-cinematic", "ProposalCinematicFileExample", "Cinematic Proposal", "Layered ModernProposal layout with cover panel, hero spread, and rich typography.", "proposal", "cinematic");
        proposal("project-proposal-cinematic", "CinematicProposalFileExample", "Project Proposal (cinematic)", "End-to-end project proposal with mountain hero, scope panels, and pricing summary.", "proposal", "cinematic");
        proposal("proposal-modern-v2", "v2/ModernProposalV2Example", "Modern Proposal", "The ModernProposal preset composed straight from its document spec — cover, scope sections and pricing table themed through BrandTheme.", "proposal");

        // ===== Templates / Schedule =====
        schedule("weekly-schedule", "Weekly Schedule", "Multi-day weekly schedule with shift assignments, category fills, and repeated header.", "schedule", "table");

        // ===== Features =====
        feature("lists", "nested-list-showcase", "NestedListExample", "Nested Lists", "ListBuilder.addItem(label, Consumer) — depth cascade, per-depth markers, mixed flat / nested authoring.", "lists", "v1.6");
        feature("tables", "table-advanced", "TableAdvancedExample", "Advanced Tables", "Row span, column span, zebra rows, total rows, and repeating headers across page breaks.", "tables", "pagination");
        feature("tables", "composed-table-cell-showcase", "ComposedTableCellExample", "Composed Table Cells", "DocumentTableCell.node(DocumentNode) — paragraphs, lists, sub-tables inside cells with two-pass measurement.", "tables", "v1.6");
        feature("tables", "inline-code-column-wrap", "InlineCodeColumnWrapExample", "Inline Code in Narrow Columns", "Inline-code chips inside a fixed-width column — how a token too wide for its cell wraps instead of overflowing the column edge.", "tables", "text");
        feature("canvas", "canvas-layer-showcase", "CanvasLayerExample", "Canvas Layer (free-canvas)", "CanvasLayerNode — pixel-precise (x,y) placement of children inside a fixed bounding box.", "canvas", "v1.6", "absolute");
        feature("shapes", "shape-container", "ShapeContainerExample", "Shape-as-Container", "Rounded rect, ellipse, circle containers with ClipPolicy and layered children.", "shapes", "clip");
        feature("svg", "svg-icon-gallery", "SvgIconGalleryExample", "SVG Icon Gallery", "34 real-world multicolour svgrepo icons through SvgIcon.parse — native vector layers, the whole set 156 KB of sources.", "svg", "icons", "v1.8");
        feature("shapes", "vector-path", "VectorPathExample", "Vector Paths (Bézier)", "addPath(...) — free-form design shapes with native cubic Bézier curves: stroked waves, filled blobs, mixed line/curve ribbons. No tessellation.", "shapes", "bezier", "v1.8");
        feature("shapes", "photo-clip", "PhotoClipExample", "Photo Clip (silhouette)", "A raster photo clipped to a free-form silhouette — circle, SVG heart, star — via ShapeContainer.path(...) + ClipPolicy.CLIP_PATH; the image COVER-fills each box so the native-curve outline crops it crisply at any zoom.", "shapes", "clip", "v1.8");
        feature("layout", "block-align", "BlockAlignExample", "Block Alignment", "addAligned(align, node) / addSvgIcon(icon, w, align) — seat any fixed-size node left / centre / right across the content width.", "layout", "align", "v1.8");
        feature("transforms", "transforms", "TransformsExample", "Layers + Transforms", "rotate / scale on every leaf builder + LayerStack with explicit z-index.", "transforms", "layers");
        feature("text", "rich-text-showcase", "RichTextShowcaseExample", "Rich Text", "Inline runs with bold / italic / colour / link options, markdown parsing.", "text", "rich");
        feature("text", "arabic-article", "ArabicArticleExample", "Arabic Article", "A full right-to-left article: shaped Arabic joined by the engine, every line reordered, natural pagination onto a second page.", "text", "rtl", "arabic", "v2.2");
        feature("text", "hebrew-invoice", "HebrewInvoiceExample", "Hebrew Invoice", "A right-to-left invoice where every line mixes Hebrew with digits and Latin names — the case the bidirectional algorithm exists for.", "text", "rtl", "hebrew", "v2.2");
        feature("text", "world-scripts", "WorldScriptsExample", "World Scripts", "One card per bundled script — Arabic, Hebrew, Georgian, Armenian, Korean — each set in its own family with a line on what makes it non-obvious.", "text", "fonts", "v2.2");
        feature("text", "text-direction", "TextDirectionExample", "Text Direction", "ParagraphBuilder.direction(TextDirection) beside the call that produced each row — including the rows where direction and alignment disagree, and the mixed line where a Latin word and a number keep running forwards inside right-to-left text.", "text", "rtl", "v2.2");
        feature("text", "section-presets", "SectionPresetsExample", "Section Presets", "Pre-baked section bands, accent strips, soft panels for templates.", "text", "sections");
        feature("barcodes", "barcode-showcase", "BarcodeShowcaseExample", "Barcodes & QR", "QR code, Code128, EAN-13, PDF417 — every supported barcode + per-barcode styling.", "barcodes", "qr");
        feature("chrome", "pdf-chrome", "PdfChromeExample", "PDF Chrome", "Headers, footers, watermarks, metadata, document protection / encryption.", "chrome", "metadata", "watermark");
        feature("streaming", "invoice-http-stream", "HttpStreamingExample", "HTTP Streaming", "Stream PDF directly to a Servlet response with no buffering.", "streaming", "http");
        feature("snapshots", "invoice-snapshot-regression", "LayoutSnapshotRegressionExample", "Layout Snapshots", "How LayoutSnapshotAssertions captures the resolved layout graph for regression testing.", "snapshots", "testing");
        feature("docx", "word-export-companion", "WordExportExample", "Word Export (DOCX)", "DocxSemanticBackend — the same document as a fixed-layout PDF and an editable Word file; charts fall back to their data table.", "docx", "word", "export");
        feature("debug", "debug-overlay", "DebugOverlayExample", "Debug Overlay", "DocumentDebugOptions — guide lines plus semantic node-path labels on the rendered sheet; trace any misplaced block back to the builder call that authored it.", "debug", "labels", "v1.8");

        // --- feature examples added since v1.7 ---
        feature("shapes", "line-cap", "LineCapExample", "Line Caps & Dotted Lines", "addLine(...).lineCap(DocumentLineCap.ROUND) — round/square end-caps on plain lines; a ROUND cap on a near-zero dashed(0.1, 4) rule draws the classic dotted TOC leader.", "shapes", "lines", "v1.9");
        feature("shapes", "line-fill", "LineFillExample", "Fill Lines & Dot Leaders", "line().fill() — a rule or dotted leader stretches to the content width or its weighted row slot, drawing table-of-contents leaders without measuring the gap by hand.", "shapes", "layout", "v1.9");
        feature("text", "emoji-clip-path", "EmojiClipPathReportExample", "Emoji Clip-Path Report", "ParagraphBuilder.rich(r -> r.svgIcon(SvgIcon.parse(xml), 22)) — drops parsed SVG emoji glyphs inline inside table cells to audit which clip-path glyphs render.", "emoji", "inline", "v1.9");
        feature("text", "emoji-gallery", "EmojiGalleryExample", "Emoji Gallery", "inlineSvgIcon(SvgIcon, pt) — flows the full graph-compose-emoji set as inline vector glyphs, paginated across pages with SvgIcon.parse from the classpath index.", "emoji", "inline", "v1.9");
        feature("text", "emoji-shortcodes", "EmojiShortcodeExample", "Emoji Shortcodes", "RichText.emoji(\":rocket:\", size) — resolves a GitHub-style shortcode to an inline vector colour glyph on the text baseline; unknown codes fall back to literal text.", "emoji", "inline", "v1.9");
        feature("text", "emoji-svg-vs-png", "EmojiSvgVsPngExample", "Emoji SVG vs PNG", "RichText.svgIcon(SvgIcon, pt) draws the same starter emoji as crisp inline vector beside RichText.image(...) raster bytes from toImage(...), one glyph down two inline paths.", "emoji", "inline", "v1.9");
        feature("text", "inline-highlight-chips", "InlineHighlightExample", "Inline Highlight Chips", "RichText.highlight(text, style, bg, radius, padding) — baseline-flowing rounded chips, with code(text) and chip(text, fg, bg) shorthands; multi-word fills wrap per line fragment.", "inline", "richtext", "v1.9");
        feature("text", "inline-shapes", "InlineShapesExample", "Inline Shape Runs", "RichText.dot/arrow/chevron/checkbox/shape(ShapeOutline) — geometric figures drawn on the text baseline from geometry, no font glyphs, between text and as list bullets.", "inline", "richtext", "v1.7");
        feature("text", "inline-svg-icons", "InlineSvgIconExample", "Inline SVG Icons", "RichText.svgIcon(SvgIcon, size) — multi-colour vector glyphs (gradients included) drawn on the text baseline from SVG, not font glyphs; crisp at any zoom, the engine path for vector colour emoji.", "v1.9", "inline", "svg");
        feature("chrome", "page-numbering", "PageNumberingExample", "Page Numbering", "DocumentHeaderFooter.numbering(DocumentPageNumbering) — {page}/{pages} footer tokens with countFrom(2) leaving the cover uncounted and LOWER_ROMAN restyling the body.", "chrome", "footer", "v1.9");
        feature("chrome", "viewer-preferences", "ViewerPreferencesExample", "Viewer Preferences", "chrome().viewerPreferences(DocumentViewerPreferences) — opens the PDF with pageMode(USE_OUTLINES) showing the bookmark panel and displayDocTitle(true) in the title bar.", "chrome", "v1.9");
        feature("charts", "chart-showcase", "ChartShowcaseExample", "Vector Charts", "section.chart(ChartSpec, ChartStyle) — bar/line/area/pie/donut specs (ChartSpec.bar(), LineInterpolation.MONOTONE, SliceLabelMode) compiled into engine primitives, no chart-specific render code.", "charts", "data-viz", "v1.9");
        feature("layout", "content-bleed", "BleedExample", "Content Bleed", "section.bleedToEdge(TOP, LEFT, RIGHT) and bleed(DocumentBleed.of(...)) push a fill to the trimmed page edge while children stay in the content margin.", "layout", "v1.9", "bleed");
        feature("layout", "per-page-margin", "PerPageMarginExample", "Per-Page Margins", "document.pageMargins(List.of(PageMarginRule.page(1, DocumentInsets.zero()), PageMarginRule.from(2, ...))) — a full-bleed cover then wide book margins in one session.", "layout", "v1.9");
        feature("layout", "row-columns", "RowColumnsExample", "Row Columns", "columns(auto(), weight(1), auto()) sizes row cells as intrinsic or weighted shares; paired with line().fill() a dotted leader fills the gap for a measureless table-of-contents row.", "layout", "v1.9", "rows");
        feature("layout", "row-flex", "RowFlexExample", "Row Flex & Arrangement", "RowBuilder.pushRight()/flexSpacer() springs absorb leftover row width while arrangement(RowArrangement.SPACE_BETWEEN/CENTER) justifies content-sized children — no manual coordinates.", "layout", "v1.9", "row");
        feature("layout", "row-vertical-align", "RowVerticalAlignExample", "Row Vertical Align", "RowBuilder.verticalAlign(RowVerticalAlign) — seats a row's children TOP, CENTER, or BOTTOM within the band set by the tallest child, no manual coordinates.", "layout", "v1.9");
        feature("navigation", "container-bookmark", "ContainerBookmarkExample", "PDF Outline Bookmarks", "bookmark(new DocumentBookmarkOptions(title)) on a container flow — each section becomes a PDF outline entry targeting its start page, building a navigable viewer panel with no manual coordinates.", "navigation", "structure", "v1.9");
        feature("navigation", "in-pdf-navigation", "InPdfNavigationExample", "In-PDF Navigation", "anchor(...) plus linkTo(...) — named destinations and internal go-to links for a clickable table of contents and bidirectional footnotes, resolved in a deferred pass for forward references.", "navigation", "v1.9", "links");
        feature("navigation", "page-reference", "PageReferenceExample", "Page References", "addPageReference(anchor, style, align) — prints the resolved page an anchor(...) lands on for \"see page N\" cross-refs, in one authoring pass via a second layout pass.", "navigation", "v1.9", "structure");
        feature("navigation", "table-of-contents", "TocExample", "Table of Contents", "page.addTableOfContents(...) with entry(label, anchor) — clickable rows, a DocumentLeader.DOTS leader, and page numbers resolved automatically from the laid-out document.", "navigation", "structure", "v1.9");
        feature("structure", "multi-section-document", "MultiSectionExample", "Multi-Section Document", "GraphCompose.documents(out).section(cover).section(body) — concatenate independent DocumentSessions, each with its own page size and chrome, into one PDF with no external merge.", "structure", "v1.9");
        feature("title", "poetry-title", "PoetryTitlePageExample", "Poetry Title Page", "A centred title page with generous vertical rhythm — margins, alignment and spacing carrying the composition instead of decoration.", "title");
        feature("title", "book-template", "BookTemplateExample", "Book Template", "A book-style title page and chapters fronted by a clickable addTableOfContents(...); toc.entry(label, anchor) rows resolve each chapter's page in one pass with DocumentLeader.DOTS leaders.", "book", "toc", "v1.9");

        // ===== Flagships =====
        flagship("master-showcase", "MasterShowcaseExample", "Master Showcase", "Kitchen-sink demo combining every primitive into a single document — the full GraphCompose surface.", "showcase");
        flagship("business-report", "BusinessReportExample", "Business Report Cover", "Flagship cover page with hero panel, KPI table, and accent strip — ready-to-ship template.", "showcase", "cover");
        flagship("module-first-profile", "ModuleFirstFileExample", "Module-First Authoring", "Authoring style focused on declaring data modules first, layout second.", "authoring");
        flagship("twin-output", "TwinOutputExample", "Twin Output", "One 16:9 page written once and emitted twice from the same session — a print-ready PDF and a PowerPoint slide with identical geometry where text, panels, and vectors stay native, editable shapes.", "showcase", "flagship");
        flagship("engine-deck-v2", "EngineDeckV2Example", "Engine Deck — Module First", "The landscape deck the README banner is cut from: the 2.0 module graph, native vector charts, and comparative benchmark figures read from the committed snapshot at render time.", "showcase", "flagship");
        flagship("engine-deck", "EngineDeckExample", "Engine Deck", "Landscape flagship deck — hero banner, SVG-icon feature spreads, and benchmark tables and charts the engine renders from comparative data.", "showcase", "flagship");
        flagship("feature-catalog", "FeatureCatalogExample", "Feature Catalog", "A guided catalog of the engine's primitives, one section per capability, every heading registered as a PDF outline bookmark for a navigable index.", "showcase", "flagship");
        flagship("social-card", "SocialCardExample", "Social Preview Card", "The repository's 1280x640 social preview, itself a GraphCompose document — one sheet resolving into a portrait page and a 16:9 slide from the same content, so the card cannot drift from the palette and wordmark it is drawn with.", "showcase", "flagship");
        flagship("linkedin-carousel", "LinkedInCarouselExample", "LinkedIn Carousel", "A six-slide 4:5 carousel sized for a LinkedIn document post, typeset for a phone. Every figure is read at render time — the version from the filtered properties, the timings from the committed benchmark snapshot.", "showcase", "flagship");
        flagship("maven-banner", "MavenBannerPptxExample", "Maven Central Banner", "A five-slide brand deck emitted through the PPTX backend — gradient, rounded panels, native paths and text frames arriving in PowerPoint as an editable copy of the rendered pages, closing on Hebrew and Arabic laid out right to left.", "showcase", "flagship", "pptx");
        flagship("financial-report", "FinancialReportExample", "Financial Report", "A polished financial-report flagship — clipped-photo masthead, KPI tables, and vector charts combining the engine's data-viz and shape primitives.", "showcase", "flagship");
    }

    /**
     * The registered entries, keyed by the basename of the PDF they describe.
     *
     * <p>Exposed so a guard can check the register against the documents the runner
     * actually writes: {@link #lookup} falls back to a filename-derived card, so an
     * entry whose PDF is never generated costs nothing at runtime and shows up nowhere
     * — it is simply never read.</p>
     */
    static Map<String, Entry> registeredEntries() {
        return Map.copyOf(ENTRIES);
    }

    static Entry lookup(String basename, String category, String group) {
        Entry e = ENTRIES.get(basename);
        if (e != null) {
            return e;
        }
        // Fallback: derive title from basename, generic description.
        String title = capitalize(basename.replace('-', ' ').replace('_', ' '));
        String desc = "Generated showcase for " + category + " / " + group + ".";
        String code = EX_BASE; // category root is the closest we can guess
        return new Entry(title, desc, List.of(category, group), code);
    }

    static String groupLabel(String category, String group) {
        return switch (category + "/" + group) {
            case "templates/cv" -> "CV / Resume";
            case "templates/coverletter" -> "Cover Letter";
            case "templates/invoice" -> "Invoice";
            case "templates/proposal" -> "Proposal";
            case "templates/schedule" -> "Schedule";
            case "features/lists" -> "Lists & Bullets";
            case "features/tables" -> "Tables";
            case "features/canvas" -> "Canvas / Free Placement";
            case "features/shapes" -> "Shapes & Containers";
            case "features/transforms" -> "Transforms & Layers";
            case "features/text" -> "Rich Text";
            case "features/barcodes" -> "Barcodes & QR";
            case "features/themes" -> "Themes";
            case "features/chrome" -> "PDF Chrome (header / footer / watermark)";
            case "features/streaming" -> "Streaming & I/O";
            case "features/snapshots" -> "Snapshot Testing";
            case "features/svg" -> "SVG Import";
            case "features/layout" -> "Layout & Alignment";
            case "features/debug" -> "Debug & Diagnostics";
            case "features/charts" -> "Charts";
            case "features/navigation" -> "In-Document Navigation";
            case "features/structure" -> "Document Structure";
            case "features/title" -> "Title & Book Pages";
            case "features/docx" -> "Word Export (DOCX)";
            case "flagships/default" -> "Flagship Demos";
            default -> capitalize(group);
        };
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (char c : s.toCharArray()) {
            if (c == ' ' || c == '-' || c == '_') {
                sb.append(' ');
                nextUpper = true;
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static void cv(String id, String exampleClass, String title, String desc, String... tags) {
        ENTRIES.put(id, entry(title, desc, withCategory("cv", tags),
                EX_BASE + "/templates/cv/v2/" + exampleClass + ".java"));
    }

    private static void letter(String id, String exampleClass, String title, String desc, String... tags) {
        ENTRIES.put(id, entry(title, desc, withCategory("letter", tags),
                EX_BASE + "/templates/coverletter/v2/" + exampleClass + ".java"));
    }

    private static void invoice(String id, String exampleClass, String title, String desc, String... tags) {
        ENTRIES.put(id, entry(title, desc, withCategory("invoice", tags),
                EX_BASE + "/templates/invoice/" + exampleClass + ".java"));
    }

    private static void proposal(String id, String exampleClass, String title, String desc, String... tags) {
        ENTRIES.put(id, entry(title, desc, withCategory("proposal", tags),
                EX_BASE + "/templates/proposal/" + exampleClass + ".java"));
    }

    private static void schedule(String id, String title, String desc, String... tags) {
        ENTRIES.put(id, entry(title, desc, withCategory("schedule", tags),
                EX_BASE + "/templates/schedule/WeeklyScheduleFileExample.java"));
    }

    private static void feature(String group, String id, String exampleClass, String title, String desc, String... tags) {
        ENTRIES.put(id, entry(title, desc, withCategory(group, tags),
                EX_BASE + "/features/" + group + "/" + exampleClass + ".java"));
    }

    private static void flagship(String id, String exampleClass, String title, String desc, String... tags) {
        ENTRIES.put(id, entry(title, desc, withCategory("flagship", tags),
                EX_BASE + "/flagships/" + exampleClass + ".java"));
    }

    private static Entry entry(String title, String desc, List<String> tags, String code) {
        return new Entry(title, desc, tags, code);
    }

    /**
     * Prefixes the category onto an entry's own tags, keeping first-seen order
     * and dropping repeats. Most call sites pass their group again as a tag,
     * which used to render the same chip twice on a card.
     */
    private static List<String> withCategory(String category, String... extra) {
        java.util.Set<String> tags = new java.util.LinkedHashSet<>();
        tags.add(category);
        java.util.Collections.addAll(tags, extra);
        return List.copyOf(tags);
    }
}
