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

    private static final String GH_BASE = "https://github.com/DemchaAV/GraphCompose/blob/main";
    private static final String EX_BASE = GH_BASE + "/examples/src/main/java/com/demcha/examples";

    record Entry(String title, String description, List<String> tags, String codeUrl) {
    }

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        // ===== Templates / CV =====
        cv("cv-modern-professional", "Modern Professional", "Clean two-column resume with right-aligned header, tinted profile panel, and uppercase section headings.", "minimal");
        cv("cv-nordic-clean", "Nordic Clean", "Sidebar layout with soft-tinted PROFILE panel, Nordic palette, and bullet skill list.", "sidebar");
        cv("cv-classic-serif", "Classic Serif", "Two-page editorial CV with Times-style serif headings and conservative grey rules.", "serif", "two-page");
        cv("cv-compact-mono", "Compact Mono", "Single-column dense layout with monospace contact line — favourite for engineering roles.", "compact", "mono");
        cv("cv-executive", "Executive", "Slate palette with prominent name banner, formal tone, and weighted section dividers.", "executive");
        cv("cv-engineering-resume", "Engineering Resume", "Tech-lead style layout with prominent skill matrix and stack tagging — was TechLead in v1.5.", "tech");
        cv("cv-timeline-minimal", "Timeline Minimal", "Vertical timeline of roles with bullet markers and tight whitespace.", "timeline");
        cv("cv-boxed-sections", "Boxed Sections", "Each section wrapped in a grey banner header — bold, structured feel.", "structured");
        cv("cv-centered-headline", "Centered Headline", "Centered name + role with full-width accent rules between sections.", "centered");
        cv("cv-blue-banner", "Blue Banner", "Light-blue full-width section bands with high-contrast headings.", "banner", "blue");
        cv("cv-editorial-blue", "Editorial Blue", "Magazine-style editorial layout with two-column body and tinted skills table.", "editorial", "blue");
        cv("cv-panel", "Panel", "Soft-tinted panels per section, Product-Leader feel — was ProductLeader in v1.5.", "panel");
        cv("cv-sidebar-portrait", "Sidebar Portrait", "Edge-to-edge grey sidebar with portrait photo, contact stack, and skills.", "sidebar", "portrait");
        cv("cv-monogram-sidebar", "Monogram Sidebar", "Sidebar with monogram badge, accent rule, and structured contact + skills column.", "sidebar", "monogram");
        cv("cv-modern-professional", "Modern Professional CV", "Same data, ModernProfessional preset — single-file CvFileExample for canonical authoring.", "minimal");

        // ===== Templates / Cover Letter =====
        letter("cover-letter", "Cover Letter (canonical)", "Single-file canonical cover letter authored via CoverLetterFileExample.", "letter");
        letter("cover-letter-modern-professional", "Modern Professional letter", "Letter paired with the Modern Professional CV palette.");
        letter("cover-letter-nordic-clean", "Nordic Clean letter", "Letter paired with the Nordic Clean CV palette.");
        letter("cover-letter-classic-serif", "Classic Serif letter", "Letter with Times-style serif typography.");
        letter("cover-letter-compact-mono", "Compact Mono letter", "Letter with mono accent and compact spacing.");
        letter("cover-letter-executive", "Executive letter", "Slate-palette executive letter.");
        letter("cover-letter-engineering-resume", "Engineering letter", "Letter paired with EngineeringResume palette.");
        letter("cover-letter-timeline-minimal", "Timeline Minimal letter", "Letter with timeline-style minimal accents.");
        letter("cover-letter-boxed-sections", "Boxed Sections letter", "Letter with grey-banner section headings.");
        letter("cover-letter-centered-headline", "Centered Headline letter", "Letter with centered name + accent rules.");
        letter("cover-letter-blue-banner", "Blue Banner letter", "Letter paired with Blue Banner CV.");
        letter("cover-letter-editorial-blue", "Editorial Blue letter", "Editorial-magazine letter paired with Editorial Blue CV.");
        letter("cover-letter-panel", "Panel letter", "Letter paired with Panel CV soft-tinted panels.");
        letter("cover-letter-sidebar-portrait", "Sidebar Portrait letter", "Letter paired with Sidebar Portrait CV.");
        letter("cover-letter-monogram-sidebar", "Monogram Sidebar letter", "Letter paired with Monogram Sidebar CV.");

        // ===== Templates / Invoice =====
        invoice("invoice", "Invoice (canonical)", "Single-page invoice with line items, totals, and structured chrome — InvoiceTemplateV1.", "invoice");
        invoice("invoice-cinematic", "Cinematic Invoice", "Polished V2 invoice template with theme-driven layout, advanced tables, and totals.", "invoice", "cinematic");

        // ===== Templates / Proposal =====
        proposal("proposal", "Proposal (canonical)", "Multi-section proposal with cover, scope, deliverables, and pricing — ProposalTemplateV1.", "proposal");
        proposal("proposal-cinematic", "Cinematic Proposal", "Cinematic V2 proposal layout with cover panel, hero spread, and rich typography.", "proposal", "cinematic");
        proposal("project-proposal-cinematic", "Project Proposal (cinematic)", "End-to-end project proposal with mountain hero, scope panels, and pricing summary.", "proposal", "cinematic");

        // ===== Templates / Schedule =====
        schedule("weekly-schedule", "Weekly Schedule", "Multi-day weekly schedule with shift assignments, category fills, and repeated header.", "schedule", "table");

        // ===== Features =====
        feature("lists", "nested-list-showcase", "Nested Lists", "ListBuilder.addItem(label, Consumer) — depth cascade, per-depth markers, mixed flat / nested authoring.", "lists", "v1.6");
        feature("tables", "table-advanced", "Advanced Tables", "Row span, column span, zebra rows, total rows, and repeating headers across page breaks.", "tables", "pagination");
        feature("tables", "composed-table-cell-showcase", "Composed Table Cells", "DocumentTableCell.node(DocumentNode) — paragraphs, lists, sub-tables inside cells with two-pass measurement.", "tables", "v1.6");
        feature("canvas", "canvas-layer-showcase", "Canvas Layer (free-canvas)", "CanvasLayerNode — pixel-precise (x,y) placement of children inside a fixed bounding box.", "canvas", "v1.6", "absolute");
        feature("shapes", "shape-container", "Shape-as-Container", "Rounded rect, ellipse, circle containers with ClipPolicy and layered children.", "shapes", "clip");
        feature("transforms", "transforms", "Layers + Transforms", "rotate / scale on every leaf builder + LayerStack with explicit z-index.", "transforms", "layers");
        feature("text", "rich-text-showcase", "Rich Text", "Inline runs with bold / italic / colour / link options, markdown parsing.", "text", "rich");
        feature("text", "section-presets", "Section Presets", "Pre-baked section bands, accent strips, soft panels for templates.", "text", "sections");
        feature("themes", "invoice-custom-theme", "Custom BusinessTheme", "Authoring a custom palette + typography scale and feeding it through templates.", "themes");
        feature("barcodes", "barcode-showcase", "Barcodes & QR", "QR code, Code128, EAN-13, PDF417 — every supported barcode + per-barcode styling.", "barcodes", "qr");
        feature("chrome", "pdf-chrome", "PDF Chrome", "Headers, footers, watermarks, metadata, document protection / encryption.", "chrome", "metadata", "watermark");
        feature("streaming", "invoice-http-stream", "HTTP Streaming", "Stream PDF directly to a Servlet response with no buffering.", "streaming", "http");
        feature("snapshots", "invoice-snapshot-regression", "Layout Snapshots", "How LayoutSnapshotAssertions captures the resolved layout graph for regression testing.", "snapshots", "testing");

        // ===== Flagships =====
        flagship("master-showcase", "Master Showcase", "Kitchen-sink demo combining every primitive into a single document — the full GraphCompose surface.", "showcase");
        flagship("business-report", "Business Report Cover", "Flagship cover page with hero panel, KPI table, and accent strip — ready-to-ship template.", "showcase", "cover");
        flagship("module-first-profile", "Module-First Authoring", "Authoring style focused on declaring data modules first, layout second.", "authoring");
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

    private static void cv(String id, String title, String desc, String... tags) {
        ENTRIES.put(id, entry(title, desc, withCategory("cv", tags),
                EX_BASE + "/templates/cv/CvTemplateGalleryFileExample.java"));
    }

    private static void letter(String id, String title, String desc, String... tags) {
        ENTRIES.put(id, entry(title, desc, withCategory("letter", tags),
                EX_BASE + "/templates/coverletter/CoverLetterTemplateGalleryFileExample.java"));
    }

    private static void invoice(String id, String title, String desc, String... tags) {
        String file = id.contains("cinematic") ? "InvoiceCinematicFileExample" : "InvoiceFileExample";
        ENTRIES.put(id, entry(title, desc, withCategory("invoice", tags),
                EX_BASE + "/templates/invoice/" + file + ".java"));
    }

    private static void proposal(String id, String title, String desc, String... tags) {
        String file;
        if (id.equals("project-proposal-cinematic")) file = "CinematicProposalFileExample";
        else if (id.contains("cinematic")) file = "ProposalCinematicFileExample";
        else file = "ProposalFileExample";
        ENTRIES.put(id, entry(title, desc, withCategory("proposal", tags),
                EX_BASE + "/templates/proposal/" + file + ".java"));
    }

    private static void schedule(String id, String title, String desc, String... tags) {
        ENTRIES.put(id, entry(title, desc, withCategory("schedule", tags),
                EX_BASE + "/templates/schedule/WeeklyScheduleFileExample.java"));
    }

    private static void feature(String group, String id, String title, String desc, String... tags) {
        String file = switch (group) {
            case "lists" -> "lists/NestedListExample.java";
            case "tables" -> id.contains("composed") ? "tables/ComposedTableCellExample.java" : "tables/TableAdvancedExample.java";
            case "canvas" -> "canvas/CanvasLayerExample.java";
            case "shapes" -> "shapes/ShapeContainerExample.java";
            case "transforms" -> "transforms/TransformsExample.java";
            case "text" -> id.equals("section-presets") ? "text/SectionPresetsExample.java" : "text/RichTextShowcaseExample.java";
            case "themes" -> "themes/CustomBusinessThemeExample.java";
            case "barcodes" -> "barcodes/BarcodeShowcaseExample.java";
            case "chrome" -> "chrome/PdfChromeExample.java";
            case "streaming" -> "streaming/HttpStreamingExample.java";
            case "snapshots" -> "snapshots/LayoutSnapshotRegressionExample.java";
            default -> group + "/" + capitalize(id);
        };
        ENTRIES.put(id, entry(title, desc, withCategory(group, tags), EX_BASE + "/features/" + file));
    }

    private static void flagship(String id, String title, String desc, String... tags) {
        String file = switch (id) {
            case "master-showcase" -> "MasterShowcaseExample.java";
            case "business-report" -> "BusinessReportExample.java";
            case "module-first-profile" -> "ModuleFirstFileExample.java";
            default -> id + ".java";
        };
        ENTRIES.put(id, entry(title, desc, withCategory("flagship", tags),
                EX_BASE + "/flagships/" + file));
    }

    private static Entry entry(String title, String desc, List<String> tags, String code) {
        return new Entry(title, desc, tags, code);
    }

    private static List<String> withCategory(String category, String... extra) {
        List<String> tags = new java.util.ArrayList<>();
        tags.add(category);
        for (String t : extra) tags.add(t);
        return List.copyOf(tags);
    }
}
