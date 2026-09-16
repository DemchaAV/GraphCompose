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
 *   <li>For a CV preset, also classify it in the ATS block below, from a
 *       resume-parser check of its showcase sample. A CV card without a
 *       classification fails {@code ShowcaseAtsClassificationTest}.</li>
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
    /** Where the examples live in the repository; a card carries both this and the link built on it. */
    private static final String EX_PATH = "examples/src/main/java/com/demcha/examples";
    private static final String EX_BASE = GH_BASE + "/" + EX_PATH;

    /** Where every preset and every model a card can name lives. */
    private static final String TEMPLATES = "com.demcha.compose.document.templates.";

    /** What a reader needs on the classpath: a template card also needs the templates module. */
    private static final List<String> ENGINE_ONLY = List.of("graph-compose");
    private static final List<String> WITH_TEMPLATES = List.of("graph-compose", "graph-compose-templates");

    /** What a card is. A card renders a template preset, demonstrates a feature, or stands alone. */
    enum Kind {
        /** Renders one of the library's template presets. */
        PRESET,
        /** Demonstrates one engine or API feature. */
        FEATURE,
        /** A standalone composition — a flagship, or a demo that is neither of the above. */
        EXAMPLE
    }

    /**
     * One showcase card, as the register describes it.
     *
     * <p>{@code presetClass} and {@code dataModel} are filled only for a card whose example
     * builds exactly one preset — {@code null} on the rest, rather than a guess. {@code kind}
     * says what the card is and is independent of them: a feature card may well render a
     * preset ({@code invoice-http-stream} builds {@code ModernInvoice} to have something to
     * stream), and it stays a feature card.</p>
     *
     * @param title             the card's heading
     * @param description       the line under it
     * @param tags              the search chips, the card's category first
     * @param codeUrl           the source link, rooted at the branch or tag {@code GH_BASE} names
     * @param kind              what the card is
     * @param sourcePath        the same source, repo-relative, for whatever reads the file itself
     * @param requiredArtifacts the Maven artifacts a reader needs on the classpath to run it
     * @param presetClass       the preset the example builds, or {@code null} where it builds none
     * @param dataModel         the type that preset composes, or {@code null} with no preset
     * @param variantOf         the card this one re-renders with different options, or {@code null}
     */
    record Entry(String title, String description, List<String> tags, String codeUrl,
                 Kind kind, String sourcePath, List<String> requiredArtifacts,
                 String presetClass, String dataModel, String variantOf) {
    }

    /**
     * How a CV preset's showcase sample fared when resume parsers read it.
     *
     * <p>Two statuses earn the "ATS-friendly" badge on the site. The other two carry none:
     * a template that still needs a fix, and a design-first template whose sidebar, columns
     * or monogram — the point of the design — cost the parsers one of the checks the badge
     * stands for.</p>
     */
    enum AtsStatus {
        /** Every check passed in every parser. */
        ATS_CERTIFIED(true),
        /** Every check passed except where a parser's own proven limitation stops it. */
        ATS_COMPATIBLE_WITH_KNOWN_PARSER_LIMITATIONS(true),
        /** A check fails for a reason the template could fix. */
        NEEDS_TEMPLATE_FIX(false),
        /** A check fails because of the layout the design is built around. */
        DESIGN_FIRST(false);

        private final boolean badge;

        AtsStatus(boolean badge) {
            this.badge = badge;
        }

        /** Whether a preset with this status shows the "ATS-friendly" badge. */
        boolean earnsBadge() {
            return badge;
        }
    }

    /**
     * The classification behind a CV card.
     *
     * @param status           what the check concluded
     * @param tested           the parsers the showcase sample was read with
     * @param lastValidated    the ISO date of that check
     * @param knownLimitations what the parsers still get wrong, in words a reader can act on;
     *                         empty for a certified preset
     */
    record Ats(AtsStatus status, List<String> tested, String lastValidated,
               List<String> knownLimitations) {
    }

    /**
     * The parsers every CV showcase sample was read with, each with the text extractor it runs
     * on. Two of them extract with pdf.js, at different versions.
     */
    private static final List<String> ATS_PARSERS = List.of(
            "OpenResume parser (pdf.js 3.7.107)",
            "ATS Reader (pdfplumber 0.11.10)",
            "resume-parser-ats 1.2.3 (pdf-parse 1.1.4, pdf.js 1.10.100)");

    /** The day the CV showcase samples were last read with those parsers. */
    static final String ATS_LAST_VALIDATED = "2026-09-14";

    /** The page that states the claim, the two categories and the known limitations. */
    static final String ATS_DETAILS_URL =
            GH_BASE + "/docs/templates/v2-layered/using-templates.md#ats-friendly-presets";

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    private static final Map<String, Ats> ATS = new LinkedHashMap<>();

    static {
        // ===== Templates / CV (v2 layered) =====
        cv("cv-modern-professional-v2", "CvModernV2Example", "Modern Professional", "Clean single-column resume with a right-aligned slate-blue name and flat bright-blue section titles.", "minimal");
        cv("cv-nordic-clean-v2", "CvNordicCleanExample", "Nordic Clean", "Sidebar layout with soft-tinted PROFILE panel, Nordic palette, and bullet skill list.", "sidebar");
        cv("cv-classic-serif-v2", "CvClassicSerifExample", "Classic Serif", "Two-page editorial CV with Times-style serif headings and conservative grey rules.", "serif", "two-page");
        cv("cv-compact-mono-v2", "CvCompactMonoExample", "Compact Mono", "Single-column dense layout with monospace contact line — favourite for engineering roles.", "compact", "mono");
        cv("cv-executive-v2", "CvExecutiveExample", "Executive", "Slate palette with prominent name banner, formal tone, and weighted section dividers.", "executive");
        cv("cv-engineering-resume-v2", "CvEngineeringResumeExample", "Engineering Resume", "Tech-lead style layout with prominent skill matrix and stack tagging — was TechLead in v1.5.", "tech");
        cv("cv-timeline-minimal-v2", "CvTimelineMinimalExample", "Timeline Minimal", "Vertical timeline of roles with bullet markers and tight whitespace.", "timeline");
        cv("cv-boxed-sections-v2", "CvBoxedV2Example", "Boxed Sections", "Each section wrapped in a grey banner header — bold, structured feel.", "structured");
        cv("cv-centered-headline-v2", "CvCenteredHeadlineExample", "Centered Headline", "Centered name + role with full-width accent rules between sections.", "centered");
        cv("cv-blue-banner-v2", "CvBlueBannerExample", "Blue Banner", "Light-blue full-width section bands with high-contrast headings.", "banner", "blue");
        cv("cv-editorial-blue-v2", "CvEditorialBlueExample", "Editorial Blue", "Single-column editorial layout with a centred uppercase masthead, blue rules and a compact skills table.", "editorial", "blue");
        cv("cv-panel-v2", "CvPanelExample", "Panel", "Soft-tinted panels per section, Product-Leader feel — was ProductLeader in v1.5.", "panel");
        cv("cv-sidebar-portrait-v2", "CvSidebarPortraitExample", "Sidebar Portrait", "Edge-to-edge grey sidebar with portrait photo, contact stack, and skills.", "sidebar", "portrait");
        cv("cv-monogram-sidebar-v2", "CvMonogramSidebarExample", "Monogram Sidebar", "Sidebar with monogram badge, accent rule, and structured contact + skills column.", "sidebar", "monogram");
        cv("cv-minimal-underlined-v2", "CvMinimalUnderlinedExample", "Minimal Underlined", "Single-column layout with underlined section titles and tight whitespace — minimalist reference shape.", "minimal");
        cv("cv-mint-editorial-v2", "CvMintEditorialExample", "Mint Editorial", "Magazine-style editorial CV with mint accent palette and two-column body.", "editorial", "mint");
        cv("cv-mint-editorial-v2-custom", "CvMintEditorialCustomExample", "Mint Editorial (custom band)", "The same preset with one colour changed through its Options — a kraft-paper masthead band, everything else left at the preset's defaults.", "editorial", "mint");
        cv("cv-professional-sidebar-v2", "ProfessionalSidebarExample", "Professional Sidebar", "Navy monogram plate over a pale sidebar of contact marks, skill meters, an education rail and language ratings, beside a white column of profile, roles and projects.", "sidebar", "navy");
        cv("cv-navy-sidebar-v2", "NavySidebarExample", "Navy Sidebar", "Navy plate with a ringed portrait, contact marks, degrees, skills and languages, beside a white column of summary, badged sections and roles strung on a timeline rail.", "sidebar", "navy", "portrait");
        cv("cv-serif-headline-v2", "SerifHeadlineExample", "Serif Headline", "Volkhov masthead over a two-column body: roles on a timeline rail and marked project cards beside degrees and skill meters, closing with full-width certification and achievement bands.", "serif", "two-column");
        cv("cv-charcoal-gold-v2", "CharcoalGoldExample", "Charcoal Gold", "Charcoal sidebar with a ringed photograph, rated skills and languages, beside a paper column with a two-tone name, a dated experience rail and paired credential columns.", "sidebar", "photo", "gold");
        cv("cv-terracotta-rail-v2", "TerracottaRailExample", "Terracotta Rail", "Serif monogram over a terracotta rule, contact channels behind their marks and two bulleted lists, beside a letter-spaced masthead, roles on a ringed rail, a projects grid and the degrees.", "sidebar", "monogram", "terracotta");
        cv("cv-teal-pulse-v2", "TealPulseExample", "Teal Pulse", "Clinical sheet in five bands: a heart crossed by a pulse beside the name, a contact strip on rules, competencies beside the summary and roles, and a three-column closing band over a tracked tagline.", "clinical", "badges", "teal");
        cv("cv-slate-orange-v2", "SlateOrangeExample", "Slate Orange", "Full-bleed slate masthead with an orange monogram tile, over a sidebar of marked competencies, trophied achievements and rated languages beside a profile, a dated experience rail and a credentials footer.", "sidebar", "masthead", "orange");
        cv("cv-violet-grid-v2", "VioletGridExample", "Violet Grid", "Single-column sheet in bands: a two-tone name beside the contact list, a six-up grid of marked skills on dotted rules, a tools strip, a dated timeline, tinted project tiles and a closing quotation.", "single-column", "grid", "violet");
        cv("cv-orange-ops-v2", "OrangeOpsExample", "Orange Ops", "Operations sheet with a two-tone name over a slanted role bar and accent slashes, a contact strip on hairlines, and a sidebar of skills, achievement discs and credentials beside a profile, dated roles and a four-metric strip.", "sidebar", "metrics", "orange");
        cv("cv-midnight-navy-v2", "MidnightNavyExample", "Midnight Navy", "Full-height navy plate carrying an outlined monogram, a tracked role line, metered skills and dotted languages, beside a paper column with the summary, roles on a rail, achievement discs and divided certification columns.", "sidebar", "monogram", "navy");

        // ===== ATS classification of the CV presets =====
        // Every CV card carries one, read from a resume-parser check of its showcase sample.
        // Only the two earned statuses show the badge. Each badged preset's status is recorded in
        // the examples tests' ats-validated-samples.properties with the SHA-256 of the exact PDF
        // its check read, so a badge cannot outlive the sample it was earned on. A design-first
        // preset keeps its layout; its limitations say what that costs a parser.
        certified("cv-blue-banner-v2");
        certified("cv-boxed-sections-v2");
        certified("cv-centered-headline-v2");
        certified("cv-classic-serif-v2");
        certified("cv-editorial-blue-v2");
        certified("cv-executive-v2");
        certified("cv-minimal-underlined-v2");
        compatible("cv-modern-professional-v2",
                "ATS Reader does not recognise the multi-word headings \"Professional Experience\""
                        + " and \"Technical Skills\"; OpenResume and resume-parser-ats do.");
        designFirst("cv-charcoal-gold-v2",
                "ATS Reader interleaves the sidebar with the main column, so its reading order fails"
                        + " and it misses the Experience heading.",
                "All three parsers read only the first line of the two-line name.");
        designFirst("cv-compact-mono-v2",
                "ATS Reader interleaves the skills rail with the body, so its reading order fails"
                        + " and it misses the Experience and Skills headings.",
                "OpenResume reads only the first word of the name.");
        designFirst("cv-engineering-resume-v2",
                "ATS Reader merges the name with the contact stack beside it and reads a contact"
                        + " line as the name.",
                "ATS Reader interleaves the two body columns, so its reading order fails and it"
                        + " misses the Education heading.");
        designFirst("cv-midnight-navy-v2",
                "OpenResume and ATS Reader read the monogram letter as the name.",
                "The navy plate is stored before the body, so all three parsers read part of the"
                        + " page out of order.");
        designFirst("cv-mint-editorial-v2",
                "The two columns interleave, so reading order fails in all three parsers and ATS"
                        + " Reader misses the Experience and Skills headings.",
                "resume-parser-ats reads the degree line under Education as a heading and drops"
                        + " Education.",
                "OpenResume reads the role line as the name.");
        designFirst("cv-mint-editorial-v2-custom",
                "The two columns interleave, so reading order fails in all three parsers and ATS"
                        + " Reader misses the Experience and Skills headings.",
                "resume-parser-ats reads the degree line under Education as a heading and drops"
                        + " Education.",
                "OpenResume reads the role line as the name.");
        designFirst("cv-monogram-sidebar-v2",
                "OpenResume reads the monogram as the name, and resume-parser-ats the sidebar's"
                        + " contact heading.",
                "ATS Reader interleaves the sidebar with the body, so its reading order fails.");
        designFirst("cv-navy-sidebar-v2",
                "OpenResume reads the sidebar's contact heading as the name.",
                "ATS Reader interleaves the sidebar with the body, so its reading order fails.");
        designFirst("cv-nordic-clean-v2",
                "ATS Reader merges the name with the contact stack beside it and reads a contact"
                        + " line as the name.",
                "ATS Reader interleaves the columns, so its reading order fails.");
        designFirst("cv-orange-ops-v2",
                "ATS Reader interleaves the aside with the main column, so its reading order fails"
                        + " and it misses the Education and Skills headings.");
        designFirst("cv-panel-v2",
                "ATS Reader interleaves the side-by-side panel content, so its reading order fails"
                        + " and it misses the Experience heading.");
        designFirst("cv-professional-sidebar-v2",
                "OpenResume and resume-parser-ats read the sidebar's contact heading as the name.",
                "ATS Reader interleaves the sidebar with the body, so its reading order fails.");
        designFirst("cv-serif-headline-v2",
                "The two body columns interleave, so reading order fails in all three parsers and"
                        + " ATS Reader misses the Experience and Education headings.",
                "OpenResume and resume-parser-ats read the first skill-group caption as a new"
                        + " heading, which leaves Skills empty.");
        designFirst("cv-sidebar-portrait-v2",
                "ATS Reader interleaves the portrait sidebar with the body, so its reading order"
                        + " fails.");
        designFirst("cv-slate-orange-v2",
                "The Education and Certifications headings sit side by side and fuse into one"
                        + " line, so two of the three parsers miss Education.",
                "ATS Reader interleaves the two body columns, so its reading order fails and it"
                        + " misses the Experience and Skills headings.");
        designFirst("cv-teal-pulse-v2",
                "OpenResume reads the role line as the name, and the skill \"Patient Education\""
                        + " as the Education heading.",
                "ATS Reader interleaves the competencies column with the body, so its reading order"
                        + " fails and it misses the Education heading.");
        designFirst("cv-terracotta-rail-v2",
                "OpenResume and resume-parser-ats read the monogram as the name.",
                "ATS Reader interleaves the sidebar with the body, so its reading order fails and"
                        + " it misses the Education and Skills headings.");
        designFirst("cv-timeline-minimal-v2",
                "OpenResume and ATS Reader read the role line as the name.",
                "ATS Reader interleaves the columns, so its reading order fails and it misses the"
                        + " Education heading.");
        designFirst("cv-violet-grid-v2",
                "The Education and Languages headings sit side by side and fuse into one line, so"
                        + " no parser finds Education.",
                "The skills grid and the side-by-side band interleave, so reading order fails in"
                        + " all three parsers.",
                "ATS Reader merges the name with the contact list beside it and reads a contact"
                        + " line as the name.");

        // ===== Templates / Cover Letter (v2 layered, paired 1:1 with CV) =====
        // Registered directly: letter() points at the layered preset examples under
        // coverletter/v2, and this one sits a level up because it composes without a
        // preset at all.
        ENTRIES.put("cover-letter", entry("Cover Letter",
                "One page composed straight in the canonical DSL — section presets carry the hierarchy, no template involved.",
                withCategory("letter"),
                "templates/coverletter/CoverLetterFileExample", Kind.EXAMPLE, ENGINE_ONLY));
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
        invoice("invoice-consulting-v2", "v2/ConsultingInvoiceV2Example", "Consulting Invoice", "The ConsultingInvoice preset on the structured invoice model — brand lockup with the caller's logo, labelled masthead metadata, priced service lines with service periods, a totals stack and bank payment fields.", "invoice");
        invoice("invoice-luma-studio-v2", "v2/LumaStudioInvoiceV2Example", "Luma Studio Invoice", "The LumaStudioInvoice preset on the structured invoice model — a cream sidebar carrying the brand lockup and its ornament, a billed-to / shipped-to pair, priced service lines with a VAT column, and the notes and bank details above a sign-off band.", "invoice");
        invoice("invoice-payments-v2", "v2/PaymentsInvoiceV2Example", "Payments Invoice", "The PaymentsInvoice preset on the structured invoice model — a diagonal band crossing the masthead, a half-split issuer and metadata header, two addressed parties, marked service lines that repeat their header across pages, and a settlement row pairing bank details against the totals.", "invoice");
        invoice("invoice-workspace-v2", "v2/WorkspaceInvoiceV2Example", "Workspace Invoice", "The WorkspaceInvoice preset on the structured invoice model — a brand masthead over an accent bar, a half-split issuer and metadata header, two addressed parties on discs, service lines whose marks sit on coloured tiles, and a settlement row above a closing band.", "invoice");
        invoice("invoice-modern-v2", "v2/ModernInvoiceV2Example", "Modern Invoice", "The ModernInvoice preset composed straight from an InvoiceDocumentSpec — line items, totals and payment block driven by the BrandTheme rather than per-document styling.", "invoice");
        invoice("invoice-classic-v2", "v2/ClassicInvoiceV2Example", "Classic Invoice", "The ClassicInvoice preset — letterhead header band, TOTAL DUE hero strip, BILL TO / FROM columns, and a dedicated Summary table after the line items.", "invoice");

        // ===== Templates / Proposal =====
        proposal("proposal-cinematic", "ProposalCinematicFileExample", "Cinematic Proposal", "Layered ModernProposal layout with cover panel, hero spread, and rich typography.", "proposal", "cinematic");
        proposal("project-proposal-cinematic", "CinematicProposalFileExample", "Project Proposal (cinematic)", "End-to-end project proposal with mountain hero, scope panels, and pricing summary.", "proposal", "cinematic");
        proposal("proposal-modern-v2", "v2/ModernProposalV2Example", "Modern Proposal", "The ModernProposal preset composed straight from its document spec — cover, scope sections and pricing table themed through BrandTheme.", "proposal");
        proposal("proposal-editorial-v2", "v2/EditorialProposalV2Example", "Editorial Proposal", "The EditorialProposal preset — the same structured proposal document as Northline, set in a serif display face with an orange accent, a drawn brand mark and headings over accent rules.", "proposal");
        proposal("proposal-northline-v2", "v2/NorthlineProposalV2Example", "Northline Proposal", "The NorthlineProposal preset on the structured proposal model — brand header, stacked title, glance card, goal cells, numbered scope, phase grid, investment table and signing card across two pages.", "proposal");

        // ===== Templates / Receipt =====
        receipt("receipt-modern", "ModernReceiptExample", "Modern Receipt", "A settled transfer confirmation on the layered receipt family — hero amount with a status chip, payer/beneficiary panel, dotted-leader detail rows, a status timeline, and a footer pinned to the page bottom with a verification QR code.", "receipt", "qr");

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
        feature("text", "letter-spacing", "LetterSpacingExample", "Letter Spacing", "DocumentTextStyle.builder().letterSpacing(DocumentLetterSpacing.ofFontSize(0.18)) — real typographic tracking through PDF Tc, DrawingML spc and Word w:spacing, so wide caps still copy and search as the word they are, not as letters padded with spaces.", "text", "typography", "v2.4");
        feature("text", "section-presets", "SectionPresetsExample", "Section Presets", "Pre-baked section bands, accent strips, soft panels for templates.", "text", "sections");
        feature("barcodes", "barcode-showcase", "BarcodeShowcaseExample", "Barcodes & QR", "QR code, Code128, EAN-13, PDF417 — every supported barcode + per-barcode styling.", "barcodes", "qr");
        feature("chrome", "pdf-chrome", "PdfChromeExample", "PDF Chrome", "Headers, footers, watermarks, metadata, document protection / encryption. The footer names its font family — fontName(PT_SANS) keeps a Cyrillic page counter's letters — and reserveSpace(true) keeps the body clear of the band.", "chrome", "metadata", "watermark");
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
        feature("chrome", "page-zone", "PageZoneExample", "Page Zone", "DocumentPageZone.footer(height, page -> node) — a footer built from nodes: a notice, an inline chip, a real link annotation and page.pageNumber(), laid out by the same engine as the body and exported to DOCX as Word's live PAGE field. A running head shows the other half of the surface: appliesTo(!isFirst) keeps it off the cover, and its addPageReference resolves the appendix's page from the body's anchors.", "chrome", "footer", "v2.3.0");
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

        // ===== The preset behind each card, and the model it composes =====
        // Only cards whose example builds exactly one preset: 56 of the 117 registered here.
        // A feature card can be one of them — invoice-http-stream builds ModernInvoice to have
        // something worth streaming — so this pass is independent of the card's kind.
        preset("cv-blue-banner-v2", "cv.presets.BlueBanner", "cv.data.CvDocument");
        preset("cv-boxed-sections-v2", "cv.presets.BoxedSections", "cv.data.CvDocument");
        preset("cv-centered-headline-v2", "cv.presets.CenteredHeadline", "cv.data.CvDocument");
        preset("cv-charcoal-gold-v2", "cv.presets.CharcoalGold", "cv.data.CvDocument");
        preset("cv-classic-serif-v2", "cv.presets.ClassicSerif", "cv.data.CvDocument");
        preset("cv-compact-mono-v2", "cv.presets.CompactMono", "cv.data.CvDocument");
        preset("cv-editorial-blue-v2", "cv.presets.EditorialBlue", "cv.data.CvDocument");
        preset("cv-engineering-resume-v2", "cv.presets.EngineeringResume", "cv.data.CvDocument");
        preset("cv-executive-v2", "cv.presets.Executive", "cv.data.CvDocument");
        preset("cv-midnight-navy-v2", "cv.presets.MidnightNavy", "cv.data.CvDocument");
        preset("cv-minimal-underlined-v2", "cv.presets.MinimalUnderlined", "cv.data.CvDocument");
        preset("cv-mint-editorial-v2", "cv.presets.MintEditorial", "cv.data.CvDocument");
        preset("cv-mint-editorial-v2-custom", "cv.presets.MintEditorial", "cv.data.CvDocument",
                "cv-mint-editorial-v2");
        preset("cv-modern-professional-v2", "cv.presets.ModernProfessional", "cv.data.CvDocument");
        preset("cv-monogram-sidebar-v2", "cv.presets.MonogramSidebar", "cv.data.CvDocument");
        preset("cv-navy-sidebar-v2", "cv.presets.NavySidebar", "cv.data.CvDocument");
        preset("cv-nordic-clean-v2", "cv.presets.NordicClean", "cv.data.CvDocument");
        preset("cv-orange-ops-v2", "cv.presets.OrangeOps", "cv.data.CvDocument");
        preset("cv-panel-v2", "cv.presets.Panel", "cv.data.CvDocument");
        preset("cv-professional-sidebar-v2", "cv.presets.ProfessionalSidebar", "cv.data.CvDocument");
        preset("cv-serif-headline-v2", "cv.presets.SerifHeadline", "cv.data.CvDocument");
        preset("cv-sidebar-portrait-v2", "cv.presets.SidebarPortrait", "cv.data.CvDocument");
        preset("cv-slate-orange-v2", "cv.presets.SlateOrange", "cv.data.CvDocument");
        preset("cv-teal-pulse-v2", "cv.presets.TealPulse", "cv.data.CvDocument");
        preset("cv-terracotta-rail-v2", "cv.presets.TerracottaRail", "cv.data.CvDocument");
        preset("cv-timeline-minimal-v2", "cv.presets.TimelineMinimal", "cv.data.CvDocument");
        preset("cv-violet-grid-v2", "cv.presets.VioletGrid", "cv.data.CvDocument");

        preset("cover-letter-blue-banner-v2", "coverletter.presets.BlueBannerLetter", "coverletter.data.CoverLetterDocument");
        preset("cover-letter-boxed-sections-v2", "coverletter.presets.BoxedSectionsLetter", "coverletter.data.CoverLetterDocument");
        preset("cover-letter-centered-headline-v2", "coverletter.presets.CenteredHeadlineLetter", "coverletter.data.CoverLetterDocument");
        preset("cover-letter-classic-serif-v2", "coverletter.presets.ClassicSerifLetter", "coverletter.data.CoverLetterDocument");
        preset("cover-letter-compact-mono-v2", "coverletter.presets.CompactMonoLetter", "coverletter.data.CoverLetterDocument");
        preset("cover-letter-editorial-blue-v2", "coverletter.presets.EditorialBlueLetter", "coverletter.data.CoverLetterDocument");
        preset("cover-letter-engineering-resume-v2", "coverletter.presets.EngineeringResumeLetter", "coverletter.data.CoverLetterDocument");
        preset("cover-letter-executive-v2", "coverletter.presets.ExecutiveLetter", "coverletter.data.CoverLetterDocument");
        preset("cover-letter-mint-editorial-v2", "coverletter.presets.MintEditorialLetter", "coverletter.data.CoverLetterDocument");
        preset("cover-letter-modern-professional-v2", "coverletter.presets.ModernProfessionalLetter", "coverletter.data.CoverLetterDocument");
        preset("cover-letter-monogram-sidebar-v2", "coverletter.presets.MonogramSidebarLetter", "coverletter.data.CoverLetterDocument");
        preset("cover-letter-nordic-clean-v2", "coverletter.presets.NordicCleanLetter", "coverletter.data.CoverLetterDocument");
        preset("cover-letter-panel-v2", "coverletter.presets.PanelLetter", "coverletter.data.CoverLetterDocument");
        preset("cover-letter-sidebar-portrait-v2", "coverletter.presets.SidebarPortraitLetter", "coverletter.data.CoverLetterDocument");
        preset("cover-letter-timeline-minimal-v2", "coverletter.presets.TimelineMinimalLetter", "coverletter.data.CoverLetterDocument");

        preset("invoice-cinematic", "invoice.presets.ModernInvoice", "data.invoice.InvoiceDocumentSpec");
        preset("invoice-modern-v2", "invoice.presets.ModernInvoice", "data.invoice.InvoiceDocumentSpec");
        preset("invoice-classic-v2", "invoice.presets.ClassicInvoice", "data.invoice.InvoiceDocumentSpec");
        preset("invoice-consulting-v2", "invoice.presets.ConsultingInvoice", "data.invoice.StructuredInvoiceDocumentSpec");
        preset("invoice-luma-studio-v2", "invoice.presets.LumaStudioInvoice", "data.invoice.StructuredInvoiceDocumentSpec");
        preset("invoice-payments-v2", "invoice.presets.PaymentsInvoice", "data.invoice.StructuredInvoiceData");
        preset("invoice-workspace-v2", "invoice.presets.WorkspaceInvoice", "data.invoice.StructuredInvoiceData");
        preset("invoice-http-stream", "invoice.presets.ModernInvoice", "data.invoice.InvoiceDocumentSpec");
        preset("invoice-snapshot-regression", "invoice.presets.ModernInvoice", "data.invoice.InvoiceDocumentSpec");

        preset("proposal-cinematic", "proposal.presets.ModernProposal", "data.proposal.ProposalDocumentSpec");
        preset("proposal-modern-v2", "proposal.presets.ModernProposal", "data.proposal.ProposalDocumentSpec");
        preset("proposal-editorial-v2", "proposal.presets.EditorialProposal", "data.proposal.StructuredProposalDocumentSpec");
        preset("proposal-northline-v2", "proposal.presets.NorthlineProposal", "data.proposal.StructuredProposalDocumentSpec");

        preset("receipt-modern", "receipt.presets.ModernReceipt", "data.receipt.ReceiptDocumentSpec");
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

    /**
     * The ATS classification of a card.
     *
     * @param basename the basename of the PDF the card describes
     * @return the classification, or {@code null} for a card that is not a CV preset
     */
    static Ats ats(String basename) {
        return ATS.get(basename);
    }

    /** Every ATS classification, keyed like {@link #registeredEntries()}. */
    static Map<String, Ats> registeredAts() {
        return Map.copyOf(ATS);
    }

    private static void certified(String id) {
        classify(id, AtsStatus.ATS_CERTIFIED);
    }

    private static void compatible(String id, String... limitations) {
        classify(id, AtsStatus.ATS_COMPATIBLE_WITH_KNOWN_PARSER_LIMITATIONS, limitations);
    }

    private static void designFirst(String id, String... limitations) {
        classify(id, AtsStatus.DESIGN_FIRST, limitations);
    }

    private static void classify(String id, AtsStatus status, String... limitations) {
        ATS.put(id, new Ats(status, ATS_PARSERS, ATS_LAST_VALIDATED, List.of(limitations)));
    }

    static Entry lookup(String basename, String category, String group) {
        Entry e = ENTRIES.get(basename);
        if (e != null) {
            return e;
        }
        // Fallback: derive title from basename, generic description. Nothing is known about
        // what the document is, so it is an EXAMPLE naming no preset — never a guess at one.
        String title = capitalize(basename.replace('-', ' ').replace('_', ' '));
        String desc = "Generated showcase for " + category + " / " + group + ".";
        return new Entry(title, desc, List.of(category, group), EX_BASE,
                Kind.EXAMPLE, EX_PATH, ENGINE_ONLY, null, null, null);
    }

    static String groupLabel(String category, String group) {
        return switch (category + "/" + group) {
            case "templates/cv" -> "CV / Resume";
            case "templates/coverletter" -> "Cover Letter";
            case "templates/invoice" -> "Invoice";
            case "templates/proposal" -> "Proposal";
            case "templates/schedule" -> "Schedule";
            case "templates/receipt" -> "Payment Receipt";
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
        template(id, "cv", "templates/cv/v2/" + exampleClass, title, desc, tags);
    }

    private static void letter(String id, String exampleClass, String title, String desc, String... tags) {
        template(id, "letter", "templates/coverletter/v2/" + exampleClass, title, desc, tags);
    }

    private static void invoice(String id, String exampleClass, String title, String desc, String... tags) {
        template(id, "invoice", "templates/invoice/" + exampleClass, title, desc, tags);
    }

    private static void proposal(String id, String exampleClass, String title, String desc, String... tags) {
        template(id, "proposal", "templates/proposal/" + exampleClass, title, desc, tags);
    }

    private static void receipt(String id, String exampleClass, String title, String desc, String... tags) {
        template(id, "receipt", "templates/receipt/" + exampleClass, title, desc, tags);
    }

    private static void schedule(String id, String title, String desc, String... tags) {
        template(id, "schedule", "templates/schedule/WeeklyScheduleFileExample", title, desc, tags);
    }

    /**
     * A card in a template category. It is an {@code EXAMPLE} needing only the engine until the
     * preset pass says otherwise: three of these build no preset at all — the cover letter
     * composed straight in the DSL, the weekly schedule and the cinematic proposal — and a card
     * claiming a preset it does not render, or a module it never touches, is a lie a reader
     * pastes into their own pom.
     */
    private static void template(String id, String tag, String source, String title, String desc, String... tags) {
        ENTRIES.put(id, entry(title, desc, withCategory(tag, tags), source, Kind.EXAMPLE, ENGINE_ONLY));
    }

    private static void feature(String group, String id, String exampleClass, String title, String desc, String... tags) {
        ENTRIES.put(id, entry(title, desc, withCategory(group, tags),
                "features/" + group + "/" + exampleClass, Kind.FEATURE, ENGINE_ONLY));
    }

    private static void flagship(String id, String exampleClass, String title, String desc, String... tags) {
        ENTRIES.put(id, entry(title, desc, withCategory("flagship", tags),
                "flagships/" + exampleClass, Kind.EXAMPLE, ENGINE_ONLY));
    }

    /**
     * Names the preset a card's example builds, and the model that preset composes. Both are
     * written relative to {@link #TEMPLATES}, which is where every one of them lives.
     *
     * <p>Only a card whose example builds exactly one preset is listed. The pairing is held to
     * the source by {@code ShowcasePresetRegistrationTest}, which re-reads each example and
     * fails on a preset that is named here and not built there — or built there and missing
     * here. That check, not the spelling of these strings, is what keeps them true.</p>
     */
    private static void preset(String id, String presetClass, String dataModel) {
        preset(id, presetClass, dataModel, null);
    }

    /** As {@link #preset(String, String, String)}, for a card re-rendering another card's preset. */
    private static void preset(String id, String presetClass, String dataModel, String variantOf) {
        Entry card = ENTRIES.get(id);
        if (card == null) {
            throw new IllegalStateException("a preset is registered for a card that is not: " + id);
        }
        // Building a preset makes a card a preset card and needs the templates module, whichever
        // helper registered it — except a feature card, which stays one: invoice-http-stream
        // builds ModernInvoice only to have a document worth streaming.
        Kind kind = card.kind() == Kind.FEATURE ? Kind.FEATURE : Kind.PRESET;
        ENTRIES.put(id, new Entry(card.title(), card.description(), card.tags(), card.codeUrl(),
                kind, card.sourcePath(), WITH_TEMPLATES,
                TEMPLATES + presetClass, TEMPLATES + dataModel, variantOf));
    }

    /**
     * Builds a card from the one thing every helper knows: where its example lives. The link
     * and the repo-relative path are the same string, so they cannot drift apart.
     */
    private static Entry entry(String title, String desc, List<String> tags, String source,
                               Kind kind, List<String> artifacts) {
        String path = EX_PATH + "/" + source + ".java";
        return new Entry(title, desc, tags, GH_BASE + "/" + path, kind, path, artifacts, null, null, null);
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
