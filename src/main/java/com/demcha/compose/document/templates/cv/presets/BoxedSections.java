package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.RichText;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.components.MarkdownText;
import com.demcha.compose.document.templates.blocks.Block;
import com.demcha.compose.document.templates.blocks.BulletListBlock;
import com.demcha.compose.document.templates.blocks.IndentedBlock;
import com.demcha.compose.document.templates.blocks.KeyValueBlock;
import com.demcha.compose.document.templates.blocks.MultiParagraphBlock;
import com.demcha.compose.document.templates.blocks.NumberedListBlock;
import com.demcha.compose.document.templates.blocks.ParagraphBlock;
import com.demcha.compose.document.templates.cv.spec.CvHeader;
import com.demcha.compose.document.templates.cv.spec.CvModule;
import com.demcha.compose.document.templates.cv.spec.CvSpec;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Templates v2 "Boxed Sections" CV preset.
 *
 * <p>Two-page friendly resume with a centered headline, thin rules above
 * and below the contact line, and one pale-grey banner per section
 * holding a spaced-caps title. Work entries render as bold position
 * left, date right, italic company/location subtitle, optional
 * description below. Visual signature ported from the legacy
 * {@code BoxedSectionsCvTemplateComposer}: PT Serif throughout, dark
 * grey ink, soft grey banner.</p>
 */
public final class BoxedSections {

    /** Stable template identifier. */
    public static final String ID = "boxed-sections";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Boxed Sections";

    /** Recommended page margin (in points). */
    public static final double RECOMMENDED_MARGIN = 28.0;

    // V1 BoxedSectionsCvTemplateComposer palette tokens.
    private static final DocumentColor INK = DocumentColor.rgb(34, 34, 34);
    private static final DocumentColor MUTED = DocumentColor.rgb(120, 120, 120);
    private static final DocumentColor RULE = DocumentColor.rgb(170, 170, 170);
    private static final DocumentColor BANNER = DocumentColor.rgb(220, 226, 230);

    private static final FontName HEADLINE_FONT = FontName.PT_SERIF;
    private static final FontName BODY_FONT = FontName.PT_SERIF;

    private BoxedSections() {
        // utility class — not instantiable
    }

    /**
     * Builds the {@code Boxed Sections} template.
     *
     * @param theme active business theme; the preset overrides palette
     *              and typography to V1 BoxedSections tokens
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new BoxedSectionsTemplate();
    }

    private static final class BoxedSectionsTemplate implements DocumentTemplate<CvSpec> {

        @Override
        public String id() {
            return ID;
        }

        @Override
        public String displayName() {
            return DISPLAY_NAME;
        }

        @Override
        public void compose(DocumentSession document, CvSpec spec) {
            Objects.requireNonNull(document, "document");
            Objects.requireNonNull(spec, "spec");

            PageFlowBuilder pageFlow = document.dsl()
                    .pageFlow()
                    .name("BoxedSectionsRoot")
                    .spacing(7)
                    .addSection("BoxedSectionsHeader", section -> {
                        section.accentBottom(RULE, 0.7);
                        addHeadline(section, spec.header());
                    })
                    .addSection("BoxedSectionsContact", section -> {
                        section.accentBottom(RULE, 0.7);
                        addContactLine(section, spec.header());
                    });

            List<CvModule> modules = spec.modules() == null ? List.of() : spec.modules();
            for (int i = 0; i < modules.size(); i++) {
                final CvModule module = modules.get(i);
                final int index = i;
                pageFlow.addSection(
                        "BoxedSectionsBanner_" + index,
                        section -> addSectionBanner(section, safe(module.title())));
                pageFlow.addSection(
                        "BoxedSectionsBody_" + index,
                        section -> addModuleBody(section, module));
            }

            pageFlow.build();
        }

        private void addHeadline(SectionBuilder section, CvHeader header) {
            section.spacing(2)
                    .padding(new DocumentInsets(0, 0, 4, 0))
                    .addParagraph(paragraph -> paragraph
                            .text(spacedUpper(name(header)))
                            .textStyle(style(HEADLINE_FONT, 21.5,
                                    DocumentTextDecoration.DEFAULT, INK))
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.zero()));
        }

        private void addContactLine(SectionBuilder section, CvHeader header) {
            List<ContactPart> parts = contactParts(header);
            if (parts.isEmpty()) {
                return;
            }
            DocumentTextStyle textStyle = style(BODY_FONT, 8.5,
                    DocumentTextDecoration.DEFAULT, INK);
            DocumentTextStyle separatorStyle = style(BODY_FONT, 8.5,
                    DocumentTextDecoration.DEFAULT, RULE);

            section.spacing(0)
                    .padding(new DocumentInsets(4, 0, 4, 0))
                    .addParagraph(paragraph -> paragraph
                            .textStyle(textStyle)
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.zero())
                            .rich(rich -> {
                                for (int i = 0; i < parts.size(); i++) {
                                    ContactPart part = parts.get(i);
                                    if (part.linkOptions() != null) {
                                        rich.link(part.text(), part.linkOptions());
                                    } else {
                                        rich.style(part.text(), textStyle);
                                    }
                                    if (i < parts.size() - 1) {
                                        rich.style("   |   ", separatorStyle);
                                    }
                                }
                            }));
        }

        private void addSectionBanner(SectionBuilder section, String title) {
            if (title == null || title.isBlank()) {
                return;
            }
            section.softPanel(BANNER, 0.0, 5.0)
                    .margin(DocumentInsets.top(4))
                    .addParagraph(paragraph -> paragraph
                            .text(spacedUpper(title))
                            .textStyle(style(HEADLINE_FONT, 9.6,
                                    DocumentTextDecoration.BOLD, INK))
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.zero()));
        }

        private void addModuleBody(SectionBuilder section, CvModule module) {
            section.spacing(4)
                    .padding(new DocumentInsets(4, 4, 0, 4));
            // Projects render with a dedicated two-line layout — bold
            // project name (with optional tech stack in parens) on the
            // first line behind a bullet, then a hanging-indented
            // description below — instead of the flat single-line bullet
            // used for general bullet lists. Matches the canonical CV
            // visual where "what the project is" stands apart from "what
            // it did". Honours both shapes the data layer ships: a
            // {@link BulletListBlock} with "**Name (tech)** - Description"
            // strings and an {@link IndentedBlock} with separate title /
            // body fields.
            if (isProjectsModule(module.title())) {
                if (module.body() instanceof BulletListBlock projects) {
                    for (String item : projects.items()) {
                        renderProjectItem(section, parseProjectItem(safe(item).trim()));
                    }
                    return;
                }
                if (module.body() instanceof IndentedBlock indented) {
                    for (IndentedBlock.Item item : indented.items()) {
                        renderProjectItem(section,
                                new ProjectParts(safe(item.title()).trim(),
                                        safe(item.body()).trim()));
                    }
                    return;
                }
            }
            renderBody(section, module.body());
        }

        private void renderBody(SectionBuilder section, Block body) {
            if (body instanceof ParagraphBlock p) {
                renderParagraph(section, p.text());
            } else if (body instanceof MultiParagraphBlock m) {
                for (String line : m.paragraphs()) {
                    WorkEntry entry = parseWorkEntry(line);
                    if (entry != null) {
                        renderWorkEntry(section, entry);
                    } else {
                        renderParagraph(section, line);
                    }
                }
            } else if (body instanceof BulletListBlock b) {
                for (String item : b.items()) {
                    WorkEntry entry = parseWorkEntry(item);
                    if (entry != null) {
                        renderWorkEntry(section, entry);
                    } else {
                        renderBulletItem(section, item);
                    }
                }
            } else if (body instanceof NumberedListBlock n) {
                for (String item : n.items()) {
                    renderParagraph(section, item);
                }
            } else if (body instanceof IndentedBlock i) {
                for (IndentedBlock.Item item : i.items()) {
                    String inline = (item.title().isBlank() ? "" : item.title())
                            + (item.title().isBlank() || item.body().isBlank() ? "" : " - ")
                            + (item.body().isBlank() ? "" : item.body());
                    renderParagraph(section, inline);
                }
            } else if (body instanceof KeyValueBlock kv) {
                for (KeyValueBlock.Entry entry : kv.entries()) {
                    renderParagraph(section, entry.key() + ": " + entry.value());
                }
            }
        }

        private void renderParagraph(SectionBuilder section, String rawLine) {
            String text = safe(rawLine).trim();
            if (text.isBlank()) {
                return;
            }
            DocumentTextStyle base = style(BODY_FONT, 8.6,
                    DocumentTextDecoration.DEFAULT, INK);
            section.addParagraph(paragraph -> paragraph
                    .textStyle(base)
                    .lineSpacing(1.4)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(2))
                    .rich(rich -> appendMarkdown(rich, text, base)));
        }

        /**
         * Renders a bullet-list item with a visible bullet glyph + a
         * hanging indent, so Technical Skills / capabilities lists in
         * BoxedSections read as a real bullet list (matching what
         * authors expect from BulletListBlock) instead of a stack of
         * unmarked paragraphs.
         */
        private void renderBulletItem(SectionBuilder section, String rawLine) {
            String text = safe(rawLine).trim();
            if (text.isBlank()) {
                return;
            }
            DocumentTextStyle base = style(BODY_FONT, 8.6,
                    DocumentTextDecoration.DEFAULT, INK);
            section.addParagraph(paragraph -> paragraph
                    .textStyle(base)
                    .lineSpacing(1.4)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(2))
                    .bulletOffset("• ")
                    .indentStrategy(DocumentTextIndent.ALL_LINES)
                    .rich(rich -> appendMarkdown(rich, text, base)));
        }

        /**
         * Renders one project entry as two stacked paragraphs:
         *
         * <pre>
         * • <b>Name</b> (tech stack)
         *   Description text wrapped under the title, hanging-indented
         *   so it lines up with the project name (not the bullet).
         * </pre>
         *
         * <p>Input format: {@code "**Name (tech)** - Description"}.
         * Both halves are optional — a project without a description
         * renders only the title line; a project without bold markers
         * around the name is treated as plain title text.</p>
         */
        private void renderProjectItem(SectionBuilder section, ProjectParts parts) {
            if (parts.name().isBlank() && parts.description().isBlank()) {
                return;
            }
            DocumentTextStyle base = style(BODY_FONT, 8.6,
                    DocumentTextDecoration.DEFAULT, INK);
            DocumentTextStyle nameStyle = style(BODY_FONT, 8.6,
                    DocumentTextDecoration.BOLD, INK);

            section.addParagraph(paragraph -> paragraph
                    .textStyle(base)
                    .lineSpacing(1.4)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(2))
                    .bulletOffset("• ")
                    .indentStrategy(DocumentTextIndent.ALL_LINES)
                    .rich(rich -> appendMarkdown(rich, parts.name(), nameStyle)));

            if (parts.description().isBlank()) {
                return;
            }
            // Two-space prefix matches the bullet+space width inside the
            // hanging-indent computation, so the description's first
            // glyph sits under the project name rather than the bullet.
            section.addParagraph(paragraph -> paragraph
                    .textStyle(base)
                    .lineSpacing(1.4)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.zero())
                    .bulletOffset("  ")
                    .indentStrategy(DocumentTextIndent.ALL_LINES)
                    .rich(rich -> appendMarkdown(rich, parts.description(), base)));
        }

        private void renderWorkEntry(SectionBuilder section, WorkEntry entry) {
            DocumentTextStyle positionStyle = style(BODY_FONT, 9.2,
                    DocumentTextDecoration.BOLD, INK);
            DocumentTextStyle dateStyle = style(BODY_FONT, 8.8,
                    DocumentTextDecoration.DEFAULT, INK);
            DocumentTextStyle subtitleStyle = style(BODY_FONT, 8.4,
                    DocumentTextDecoration.ITALIC, MUTED);
            DocumentTextStyle bodyStyle = style(BODY_FONT, 8.6,
                    DocumentTextDecoration.DEFAULT, INK);

            section.addRow("BoxedSectionsEntryHeader", row -> row
                    .spacing(8)
                    .weights(1.0, 0.45)
                    .addSection("Title", titleColumn -> titleColumn
                            .padding(DocumentInsets.zero())
                            .addParagraph(paragraph -> paragraph
                                    .text(stripBasicMarkdown(entry.headingMain()))
                                    .textStyle(positionStyle)
                                    .align(TextAlign.LEFT)
                                    .margin(DocumentInsets.zero())))
                    .addSection("Date", dateColumn -> dateColumn
                            .padding(DocumentInsets.zero())
                            .addParagraph(paragraph -> paragraph
                                    .text(stripBasicMarkdown(entry.date()))
                                    .textStyle(dateStyle)
                                    .align(TextAlign.RIGHT)
                                    .margin(DocumentInsets.zero()))));

            if (!entry.headingSub().isBlank()) {
                section.addParagraph(paragraph -> paragraph
                        .textStyle(subtitleStyle)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.zero())
                        .rich(rich -> appendMarkdown(rich,
                                entry.headingSub(), subtitleStyle)));
            }

            if (!entry.description().isBlank()) {
                section.addParagraph(paragraph -> paragraph
                        .textStyle(bodyStyle)
                        .lineSpacing(1.4)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(2))
                        .rich(rich -> appendMarkdown(rich,
                                entry.description(), bodyStyle)));
            }
        }
    }

    // -- helpers ---------------------------------------------------------

    /**
     * Appends {@code text} to {@code rich} parsing the inline markdown
     * markers ({@code **bold**}, {@code *italic*}, {@code _italic_}) so
     * spec authors can carry inline emphasis without preprocessing.
     * Plain text falls through unchanged.
     */
    private static void appendMarkdown(RichText rich, String text,
                                       DocumentTextStyle baseStyle) {
        if (text == null || text.isEmpty()) {
            return;
        }
        for (InlineRun run : MarkdownText.parse(text, baseStyle)) {
            if (!(run instanceof InlineTextRun textRun)) {
                continue;
            }
            DocumentTextStyle runStyle = textRun.textStyle() == null
                    ? baseStyle
                    : textRun.textStyle();
            rich.style(textRun.text(), runStyle);
        }
    }

    private static DocumentTextStyle style(FontName font, double size,
                                           DocumentTextDecoration decoration,
                                           DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(font)
                .size(size)
                .decoration(decoration)
                .color(color)
                .build();
    }

    private static List<ContactPart> contactParts(CvHeader header) {
        if (header == null) {
            return List.of();
        }
        List<ContactPart> parts = new ArrayList<>();
        addPart(parts, safe(header.phone()), null);
        String email = safe(header.email());
        if (!email.isBlank()) {
            addPart(parts, email, new DocumentLinkOptions("mailto:" + email));
        }
        addPart(parts, safe(header.address()), null);
        for (CvHeader.Link link : header.links()) {
            String label = safe(link.label());
            if (label.isBlank()) {
                continue;
            }
            String url = safe(link.url());
            addPart(parts, label, url.isBlank()
                    ? null
                    : new DocumentLinkOptions(url.trim()));
        }
        return List.copyOf(parts);
    }

    private static void addPart(List<ContactPart> parts, String text,
                                DocumentLinkOptions linkOptions) {
        if (text != null && !text.isBlank()) {
            parts.add(new ContactPart(text, linkOptions));
        }
    }

    private static WorkEntry parseWorkEntry(String item) {
        String clean = stripBasicMarkdown(safe(item).trim());
        int pipeIndex = clean.indexOf('|');
        if (pipeIndex < 0) {
            return null;
        }
        String headingText = clean.substring(0, pipeIndex).trim();
        String afterPipe = clean.substring(pipeIndex + 1).trim();
        if (headingText.isBlank() || afterPipe.isBlank()) {
            return null;
        }
        String date;
        String description = "";
        int dashIdx = afterPipe.indexOf(" - ");
        if (dashIdx > 0) {
            date = afterPipe.substring(0, dashIdx).trim();
            description = afterPipe.substring(dashIdx + 3).trim();
        } else {
            date = afterPipe;
        }
        if (!looksLikeDate(date)) {
            return null;
        }
        HeadingParts heading = splitHeading(headingText);
        return new WorkEntry(heading.main(), heading.sub(), date, description);
    }

    private static HeadingParts splitHeading(String heading) {
        for (String separator : new String[]{" – ", " — ", " - "}) {
            int idx = heading.indexOf(separator);
            if (idx > 0) {
                return new HeadingParts(
                        heading.substring(0, idx).trim(),
                        heading.substring(idx + separator.length()).trim());
            }
        }
        int comma = heading.indexOf(", ");
        if (comma > 0) {
            return new HeadingParts(
                    heading.substring(0, comma).trim(),
                    heading.substring(comma + 2).trim());
        }
        return new HeadingParts(heading.trim(), "");
    }

    private static boolean looksLikeDate(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.matches(".*(\\d{4}|present|current|now|ongoing).*")
                && (lower.contains("-") || lower.contains("–") || lower.contains("—") || lower.contains("to"));
    }

    private static String name(CvHeader header) {
        return header == null ? "" : safe(header.name());
    }

    private static String stripBasicMarkdown(String value) {
        return safe(value)
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace("*", "")
                .replace("_", "");
    }

    private static boolean isProjectsModule(String title) {
        if (title == null) {
            return false;
        }
        String normalized = title.toLowerCase(Locale.ROOT).trim();
        return normalized.equals("projects") || normalized.startsWith("projects ");
    }

    private static ProjectParts parseProjectItem(String item) {
        // Split on " - " (space-hyphen-space, mirroring WorkEntry parsing)
        // so an em-dash or hyphen inside the description is not eaten.
        // Falls back to "title only" when no separator is present.
        int sepIndex = item.indexOf(" - ");
        if (sepIndex <= 0) {
            return new ProjectParts(item.trim(), "");
        }
        String name = item.substring(0, sepIndex).trim();
        String description = item.substring(sepIndex + 3).trim();
        return new ProjectParts(name, description);
    }

    private record ProjectParts(String name, String description) {
    }

    private static String spacedUpper(String value) {
        String upper = safe(value).toUpperCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < upper.length(); i++) {
            char current = upper.charAt(i);
            builder.append(current);
            if (Character.isLetterOrDigit(current)
                    && i + 1 < upper.length()
                    && Character.isLetterOrDigit(upper.charAt(i + 1))) {
                builder.append(' ');
            } else if (Character.isWhitespace(current)) {
                builder.append("  ");
            }
        }
        return builder.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record ContactPart(String text, DocumentLinkOptions linkOptions) {
    }

    private record HeadingParts(String main, String sub) {
    }

    private record WorkEntry(String headingMain, String headingSub, String date, String description) {
    }
}
