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
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.blocks.Block;
import com.demcha.compose.document.templates.blocks.BulletListBlock;
import com.demcha.compose.document.templates.blocks.IndentedBlock;
import com.demcha.compose.document.templates.blocks.KeyValueBlock;
import com.demcha.compose.document.templates.blocks.MultiParagraphBlock;
import com.demcha.compose.document.templates.blocks.NumberedListBlock;
import com.demcha.compose.document.templates.blocks.ParagraphBlock;
import com.demcha.compose.document.templates.components.MarkdownText;
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
 * Templates v2 "Editorial Blue" CV preset.
 *
 * <p>Light editorial single-column CV with a centered name in deep navy
 * over a "Professional Title" subline, bright editorial blue accents
 * throughout, and section headers sandwiched between thin blue rules.
 * Skills render as a four-column table; Education / Projects /
 * Employment History render structured entries with bold leading
 * titles, bold accent dates, and italic muted subtitles.</p>
 */
public final class EditorialBlue {

    /** Stable template identifier. */
    public static final String ID = "editorial-blue";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Editorial Blue";

    /** Recommended page margin (in points). */
    public static final double RECOMMENDED_MARGIN = 28.0;

    /** Default subtitle rendered under the name when the spec carries none. */
    private static final String DEFAULT_SUBTITLE = "Professional";

    // V1 EditorialBlue palette tokens.
    private static final DocumentColor INK = DocumentColor.rgb(18, 31, 72);
    private static final DocumentColor BODY = DocumentColor.rgb(60, 72, 106);
    private static final DocumentColor ACCENT = DocumentColor.rgb(86, 136, 255);
    private static final DocumentColor MUTED = DocumentColor.rgb(150, 158, 178);
    private static final DocumentColor FOOTER_RULE = DocumentColor.rgb(193, 201, 211);
    private static final DocumentColor BORDER = DocumentColor.rgb(193, 201, 211);

    private static final FontName HEADER_FONT = FontName.HELVETICA_BOLD;
    private static final FontName BODY_FONT = FontName.HELVETICA;

    private static final int SKILL_COLUMNS = 4;

    /** Uniform rule thickness used across every section divider. */
    private static final double RULE_THICKNESS = 0.6;

    private static final List<String> SUMMARY_KEYS = List.of("summary", "professional summary", "profile");
    private static final List<String> EXPERIENCE_KEYS = List.of("experience", "employment");
    private static final List<String> EDUCATION_KEYS = List.of("education", "certifications");
    private static final List<String> PROJECTS_KEYS = List.of("projects");
    private static final List<String> SKILL_KEYS = List.of("technical skills", "skills");
    private static final List<String> ADDITIONAL_KEYS = List.of("additional information", "additional");

    private EditorialBlue() {
        // utility class — not instantiable
    }

    /**
     * Builds the {@code Editorial Blue} template.
     *
     * @param theme active business theme; the preset overrides palette
     *              and typography to V1 EditorialBlue tokens
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new EditorialBlueTemplate();
    }

    private static final class EditorialBlueTemplate implements DocumentTemplate<CvSpec> {

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

            double width = document.canvas().innerWidth();

            PageFlowBuilder pageFlow = document.dsl()
                    .pageFlow()
                    .name("EditorialBlueRoot")
                    .spacing(0);

            addHeader(pageFlow, spec.header(), width);

            addProfile(pageFlow, width, findModule(spec, SUMMARY_KEYS));
            addExperience(pageFlow, width, findModule(spec, EXPERIENCE_KEYS));
            addProjects(pageFlow, width, findModule(spec, PROJECTS_KEYS));
            addEducation(pageFlow, width, findModule(spec, EDUCATION_KEYS));
            addSkills(pageFlow, width, findModule(spec, SKILL_KEYS));
            addAdditional(pageFlow, width, findModule(spec, ADDITIONAL_KEYS));
            addFooter(pageFlow, width);

            pageFlow.build();
        }

        private void addHeader(PageFlowBuilder pageFlow, CvHeader header, double width) {
            pageFlow.addSection("EditorialBlueHeader", section -> {
                section.spacing(1)
                        .padding(new DocumentInsets(2, 0, 2, 0))
                        .addParagraph(paragraph -> paragraph
                                .text(safe(header == null ? "" : header.name())
                                        .toUpperCase(Locale.ROOT))
                                .textStyle(style(HEADER_FONT, 22.0,
                                        DocumentTextDecoration.BOLD, INK))
                                .align(TextAlign.CENTER)
                                .margin(DocumentInsets.zero()));
                String subtitle = header == null
                        ? DEFAULT_SUBTITLE
                        : (safe(header.jobTitle()).isBlank()
                                ? DEFAULT_SUBTITLE
                                : header.jobTitle());
                section.addParagraph(paragraph -> paragraph
                        .text(subtitle)
                        .textStyle(style(BODY_FONT, 10.0,
                                DocumentTextDecoration.DEFAULT, BODY))
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.top(1)));
                if (header != null) {
                    String meta = joinDash(safe(header.phone()), safe(header.address()));
                    if (!meta.isBlank()) {
                        section.addParagraph(paragraph -> paragraph
                                .text(meta)
                                .textStyle(style(BODY_FONT, 9.0,
                                        DocumentTextDecoration.DEFAULT, BODY))
                                .align(TextAlign.CENTER)
                                .margin(DocumentInsets.top(1)));
                    }
                    addLinkRow(section, header);
                }
            });
        }

        private void addLinkRow(SectionBuilder section, CvHeader header) {
            List<ContactPart> parts = new ArrayList<>();
            String email = safe(header.email());
            if (!email.isBlank()) {
                parts.add(new ContactPart(email, new DocumentLinkOptions("mailto:" + email)));
            }
            for (CvHeader.Link link : header.links()) {
                String label = safe(link.label());
                if (label.isBlank()) {
                    continue;
                }
                String url = safe(link.url());
                parts.add(new ContactPart(label, url.isBlank()
                        ? null
                        : new DocumentLinkOptions(url.trim())));
            }
            if (parts.isEmpty()) {
                return;
            }
            DocumentTextStyle meta = style(BODY_FONT, 9.0,
                    DocumentTextDecoration.DEFAULT, BODY);
            DocumentTextStyle link = style(BODY_FONT, 9.0,
                    DocumentTextDecoration.UNDERLINE, ACCENT);
            section.addParagraph(paragraph -> paragraph
                    .textStyle(meta)
                    .align(TextAlign.CENTER)
                    .margin(DocumentInsets.top(1))
                    .rich(rich -> {
                        for (int i = 0; i < parts.size(); i++) {
                            ContactPart part = parts.get(i);
                            if (part.linkOptions() != null) {
                                rich.with(part.text(), link, part.linkOptions());
                            } else {
                                rich.style(part.text(), meta);
                            }
                            if (i < parts.size() - 1) {
                                rich.style("   |   ", meta);
                            }
                        }
                    }));
        }

        private void addProfile(PageFlowBuilder pageFlow, double width, CvModule module) {
            String text = String.join(" ", moduleLines(module));
            if (text.isBlank()) {
                return;
            }
            sectionHeader(pageFlow, "EditorialBlueProfile", "PROFESSIONAL PROFILE", width, true);
            pageFlow.addSection("EditorialBlueProfileBody", section -> {
                section.spacing(0)
                        .padding(new DocumentInsets(8, 0, 0, 0));
                DocumentTextStyle base = style(BODY_FONT, 9.6,
                        DocumentTextDecoration.DEFAULT, BODY);
                section.addParagraph(paragraph -> paragraph
                        .textStyle(base)
                        .lineSpacing(1.6)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.zero())
                        .rich(rich -> appendMarkdown(rich, text, base)));
            });
        }

        private void addExperience(PageFlowBuilder pageFlow, double width, CvModule module) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            sectionHeader(pageFlow, "EditorialBlueExperience", "EMPLOYMENT HISTORY", width, false);
            pageFlow.addSection("EditorialBlueExperienceBody", section -> {
                section.spacing(4)
                        .padding(new DocumentInsets(8, 0, 0, 0));
                for (String line : lines) {
                    WorkEntry entry = parseWorkEntry(line);
                    if (entry == null) {
                        renderParagraph(section, line);
                        continue;
                    }
                    renderExperienceEntry(section, entry);
                }
            });
        }

        private void renderExperienceEntry(SectionBuilder section, WorkEntry entry) {
            DocumentTextStyle roleStyle = style(HEADER_FONT, 11.0,
                    DocumentTextDecoration.BOLD, INK);
            DocumentTextStyle dateStyle = style(HEADER_FONT, 11.0,
                    DocumentTextDecoration.BOLD, ACCENT);
            DocumentTextStyle subtitleStyle = style(BODY_FONT, 9.4,
                    DocumentTextDecoration.ITALIC, BODY);
            DocumentTextStyle bodyStyle = style(BODY_FONT, 9.4,
                    DocumentTextDecoration.DEFAULT, BODY);

            section.addParagraph(paragraph -> paragraph
                    .textStyle(roleStyle)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(2))
                    .rich(rich -> {
                        rich.style(stripBasicMarkdown(entry.title()), roleStyle);
                        if (!entry.date().isBlank()) {
                            rich.style(" ", roleStyle);
                            rich.style(stripBasicMarkdown(entry.date()), dateStyle);
                        }
                    }));
            if (!entry.subtitle().isBlank()) {
                section.addParagraph(paragraph -> paragraph
                        .textStyle(subtitleStyle)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.zero())
                        .rich(rich -> appendMarkdown(rich,
                                entry.subtitle(), subtitleStyle)));
            }
            if (!entry.description().isBlank()) {
                section.addParagraph(paragraph -> paragraph
                        .textStyle(bodyStyle)
                        .lineSpacing(1.6)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(1))
                        .rich(rich -> appendMarkdown(rich,
                                entry.description(), bodyStyle)));
            }
        }

        private void addProjects(PageFlowBuilder pageFlow, double width, CvModule module) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            sectionHeader(pageFlow, "EditorialBlueProjects", "PROJECTS", width, false);
            pageFlow.addSection("EditorialBlueProjectsBody", section -> {
                section.spacing(3)
                        .padding(new DocumentInsets(8, 0, 0, 0));
                for (String line : lines) {
                    renderProjectEntry(section, line);
                }
            });
        }

        private void renderProjectEntry(SectionBuilder section, String rawLine) {
            // Split on first " - " into title + description.
            String item = stripBasicMarkdown(safe(rawLine).trim());
            if (item.isBlank()) {
                return;
            }
            String workingTitle = item;
            String description = "";
            int dash = item.indexOf(" - ");
            if (dash > 0) {
                workingTitle = item.substring(0, dash).trim();
                description = item.substring(dash + 3).trim();
            }
            // Optional parenthetical context "Title (stack)" → italic stack subtitle.
            String workingStack = "";
            int parOpen = workingTitle.indexOf('(');
            int parClose = workingTitle.lastIndexOf(')');
            if (parOpen > 0 && parClose > parOpen) {
                workingStack = workingTitle.substring(parOpen + 1, parClose).trim();
                workingTitle = workingTitle.substring(0, parOpen).trim();
            }
            final String title = workingTitle;
            final String stack = workingStack;

            DocumentTextStyle titleStyle = style(HEADER_FONT, 10.6,
                    DocumentTextDecoration.BOLD, INK);
            DocumentTextStyle stackStyle = style(BODY_FONT, 9.2,
                    DocumentTextDecoration.ITALIC, BODY);
            DocumentTextStyle bodyStyle = style(BODY_FONT, 9.4,
                    DocumentTextDecoration.DEFAULT, BODY);

            section.addParagraph(paragraph -> paragraph
                    .text(title)
                    .textStyle(titleStyle)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(2)));
            if (!stack.isBlank()) {
                section.addParagraph(paragraph -> paragraph
                        .text(stack)
                        .textStyle(stackStyle)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.zero()));
            }
            if (!description.isBlank()) {
                String finalDesc = description;
                section.addParagraph(paragraph -> paragraph
                        .textStyle(bodyStyle)
                        .lineSpacing(1.5)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(1))
                        .rich(rich -> appendMarkdown(rich, finalDesc, bodyStyle)));
            }
        }

        private void addEducation(PageFlowBuilder pageFlow, double width, CvModule module) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            sectionHeader(pageFlow, "EditorialBlueEducation", "EDUCATION", width, false);
            pageFlow.addSection("EditorialBlueEducationBody", section -> {
                section.spacing(3)
                        .padding(new DocumentInsets(8, 0, 0, 0));
                for (String line : lines) {
                    renderEducationEntry(section, line);
                }
            });
        }

        private void renderEducationEntry(SectionBuilder section, String rawLine) {
            // Education shape: "**Title** - subtitle | date" or
            // "**Title** | date" or just "Title".
            String item = stripBasicMarkdown(safe(rawLine).trim());
            if (item.isBlank()) {
                return;
            }
            String title = item;
            String subtitle = "";
            String date = "";
            int dashIdx = item.indexOf(" - ");
            if (dashIdx > 0) {
                title = item.substring(0, dashIdx).trim();
                subtitle = item.substring(dashIdx + 3).trim();
            }
            int pipe = subtitle.lastIndexOf('|');
            if (pipe > 0) {
                date = subtitle.substring(pipe + 1).trim();
                subtitle = subtitle.substring(0, pipe).trim();
            } else {
                int pipeInTitle = title.lastIndexOf('|');
                if (pipeInTitle > 0) {
                    date = title.substring(pipeInTitle + 1).trim();
                    title = title.substring(0, pipeInTitle).trim();
                }
            }

            DocumentTextStyle titleStyle = style(HEADER_FONT, 10.6,
                    DocumentTextDecoration.BOLD, INK);
            DocumentTextStyle dateStyle = style(HEADER_FONT, 10.0,
                    DocumentTextDecoration.BOLD, ACCENT);
            DocumentTextStyle subtitleStyle = style(BODY_FONT, 9.2,
                    DocumentTextDecoration.ITALIC, BODY);

            String finalTitle = title;
            String finalDate = date;
            String finalSubtitle = subtitle;
            section.addParagraph(paragraph -> paragraph
                    .textStyle(titleStyle)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(2))
                    .rich(rich -> {
                        rich.style(finalTitle, titleStyle);
                        if (!finalDate.isBlank()) {
                            rich.style(" ", titleStyle);
                            rich.style(finalDate, dateStyle);
                        }
                    }));
            if (!finalSubtitle.isBlank()) {
                section.addParagraph(paragraph -> paragraph
                        .text(finalSubtitle)
                        .textStyle(subtitleStyle)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.zero()));
            }
        }

        private void addSkills(PageFlowBuilder pageFlow, double width, CvModule module) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            // Split each line on commas to get atomic skill tokens.
            List<String> tokens = new ArrayList<>();
            for (String line : lines) {
                String clean = stripBasicMarkdown(line);
                int colon = clean.indexOf(':');
                String values = colon >= 0 ? clean.substring(colon + 1) : clean;
                for (String token : values.split(",")) {
                    String t = token.trim();
                    if (!t.isBlank()) {
                        tokens.add(t);
                    }
                }
            }
            if (tokens.isEmpty()) {
                return;
            }
            // Pad to a multiple of SKILL_COLUMNS so the table fills evenly.
            while (tokens.size() % SKILL_COLUMNS != 0) {
                tokens.add("");
            }

            sectionHeader(pageFlow, "EditorialBlueSkills", "KEY SKILLS", width, false);
            pageFlow.addSection("EditorialBlueSkillsBody", section -> {
                section.spacing(0)
                        .padding(new DocumentInsets(8, 0, 0, 0));
                DocumentTextStyle cellStyle = style(BODY_FONT, 9.0,
                        DocumentTextDecoration.DEFAULT, BODY);
                DocumentTableStyle defaultCell = DocumentTableStyle.builder()
                        .padding(new DocumentInsets(5, 8, 5, 8))
                        .stroke(DocumentStroke.of(BORDER, 0.5))
                        .textStyle(cellStyle)
                        .build();
                section.addTable(table -> {
                    table.name("EditorialBlueSkillsTable")
                            .autoColumns(SKILL_COLUMNS)
                            .defaultCellStyle(defaultCell);
                    for (int i = 0; i < tokens.size(); i += SKILL_COLUMNS) {
                        String[] row = new String[SKILL_COLUMNS];
                        for (int c = 0; c < SKILL_COLUMNS; c++) {
                            String token = tokens.get(i + c);
                            row[c] = token.isBlank() ? "" : "• " + token;
                        }
                        table.row(row);
                    }
                });
            });
        }

        private void addAdditional(PageFlowBuilder pageFlow, double width, CvModule module) {
            if (module == null) {
                return;
            }
            sectionHeader(pageFlow, "EditorialBlueAdditional", "ADDITIONAL", width, false);
            pageFlow.addSection("EditorialBlueAdditionalBody", section -> {
                section.spacing(2)
                        .padding(new DocumentInsets(8, 0, 0, 0));
                renderBody(section, module.body());
            });
        }

        private void addFooter(PageFlowBuilder pageFlow, double width) {
            pageFlow.addLine(line -> line
                    .name("EditorialBlueFooterRule")
                    .horizontal(width)
                    .color(FOOTER_RULE)
                    .thickness(RULE_THICKNESS)
                    .margin(new DocumentInsets(6, 0, 0, 0)));
            pageFlow.addSection("EditorialBlueFooter", section -> section
                    .padding(new DocumentInsets(2, 0, 0, 0))
                    .addParagraph(paragraph -> paragraph
                            .text("References available upon request.")
                            .textStyle(style(BODY_FONT, 8.4,
                                    DocumentTextDecoration.ITALIC, MUTED))
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.top(2))));
        }

        private void sectionHeader(PageFlowBuilder pageFlow, String name, String title,
                                   double width, boolean withTopRule) {
            if (withTopRule) {
                pageFlow.addLine(line -> line
                        .name(name + "RuleTop")
                        .horizontal(width)
                        .color(ACCENT)
                        .thickness(RULE_THICKNESS)
                        .margin(new DocumentInsets(8, 0, 0, 0)));
            }
            // Equal vertical padding above and below the title so the
            // "PROFESSIONAL PROFILE" text sits centered between the two
            // rules with matching breathing room top and bottom.
            double titleGap = 6;
            pageFlow.addSection(name, section -> section
                    .spacing(0)
                    .padding(new DocumentInsets(titleGap, 0, titleGap, 0))
                    .addParagraph(paragraph -> paragraph
                            .text(title)
                            .textStyle(style(HEADER_FONT, 11.0,
                                    DocumentTextDecoration.BOLD, ACCENT))
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.zero())));
            pageFlow.addLine(line -> line
                    .name(name + "RuleBottom")
                    .horizontal(width)
                    .color(ACCENT)
                    .thickness(RULE_THICKNESS)
                    .margin(new DocumentInsets(0, 0, 0, 0)));
        }

        private void renderBody(SectionBuilder section, Block body) {
            if (body instanceof ParagraphBlock p) {
                renderParagraph(section, p.text());
            } else if (body instanceof MultiParagraphBlock m) {
                for (String line : m.paragraphs()) {
                    renderParagraph(section, line);
                }
            } else if (body instanceof BulletListBlock b) {
                for (String item : b.items()) {
                    renderParagraph(section, item);
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
                    renderKeyValueEntry(section, entry);
                }
            }
        }

        private void renderParagraph(SectionBuilder section, String rawLine) {
            String text = safe(rawLine).trim();
            if (text.isBlank()) {
                return;
            }
            DocumentTextStyle base = style(BODY_FONT, 9.4,
                    DocumentTextDecoration.DEFAULT, BODY);
            section.addParagraph(paragraph -> paragraph
                    .textStyle(base)
                    .lineSpacing(1.4)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(1))
                    .rich(rich -> appendMarkdown(rich, text, base)));
        }

        private void renderKeyValueEntry(SectionBuilder section, KeyValueBlock.Entry entry) {
            DocumentTextStyle base = style(BODY_FONT, 9.4,
                    DocumentTextDecoration.DEFAULT, BODY);
            DocumentTextStyle keyStyle = style(BODY_FONT, 9.4,
                    DocumentTextDecoration.BOLD, INK);
            section.addParagraph(paragraph -> paragraph
                    .textStyle(base)
                    .lineSpacing(1.4)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(1))
                    .rich(rich -> {
                        rich.style(safe(entry.key()) + ":", keyStyle);
                        rich.style(" ", base);
                        appendMarkdown(rich, safe(entry.value()), base);
                    }));
        }
    }

    // -- helpers ---------------------------------------------------------

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

    private static CvModule findModule(CvSpec spec, List<String> keys) {
        if (spec == null || spec.modules() == null) {
            return null;
        }
        for (CvModule module : spec.modules()) {
            String normalized = normalize(safe(module.name()) + " " + safe(module.title()));
            for (String key : keys) {
                if (normalized.contains(normalize(key))) {
                    return module;
                }
            }
        }
        return null;
    }

    private static List<String> moduleLines(CvModule module) {
        if (module == null) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        Block body = module.body();
        if (body instanceof ParagraphBlock p) {
            addLines(lines, p.text());
        } else if (body instanceof MultiParagraphBlock m) {
            m.paragraphs().forEach(line -> addLines(lines, line));
        } else if (body instanceof BulletListBlock b) {
            b.items().forEach(item -> addLines(lines, item));
        } else if (body instanceof NumberedListBlock n) {
            n.items().forEach(item -> addLines(lines, item));
        } else if (body instanceof IndentedBlock i) {
            i.items().forEach(item -> {
                String title = safe(item.title());
                String bodyText = safe(item.body());
                if (title.isBlank() && bodyText.isBlank()) {
                    return;
                }
                if (title.isBlank()) {
                    lines.add(bodyText);
                } else if (bodyText.isBlank()) {
                    lines.add(title);
                } else {
                    lines.add(title + " | " + bodyText);
                }
            });
        } else if (body instanceof KeyValueBlock kv) {
            kv.entries().forEach(entry -> addLines(lines, entry.key() + ": " + entry.value()));
        }
        return List.copyOf(lines);
    }

    private static WorkEntry parseWorkEntry(String raw) {
        String item = stripBasicMarkdown(safe(raw));
        int pipeIndex = item.indexOf('|');
        if (pipeIndex < 0) {
            return new WorkEntry(item, "", "", "");
        }
        String headingText = item.substring(0, pipeIndex).trim();
        String afterPipe = item.substring(pipeIndex + 1).trim();
        String date = afterPipe;
        String description = "";
        int dashIdx = afterPipe.indexOf(" - ");
        if (dashIdx > 0) {
            date = afterPipe.substring(0, dashIdx).trim();
            description = afterPipe.substring(dashIdx + 3).trim();
        }
        String title = headingText;
        String subtitle = "";
        int comma = headingText.indexOf(", ");
        if (comma > 0) {
            title = headingText.substring(0, comma).trim();
            subtitle = headingText.substring(comma + 2).trim();
        }
        return new WorkEntry(title, subtitle, date, description);
    }

    private static String stripBasicMarkdown(String value) {
        return safe(value)
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace("*", "")
                .replace("_", "");
    }

    private static String joinDash(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" - ");
            }
            sb.append(part.trim());
        }
        return sb.toString();
    }

    private static String normalize(String value) {
        StringBuilder builder = new StringBuilder();
        String safeValue = safe(value);
        for (int i = 0; i < safeValue.length(); i++) {
            char current = Character.toLowerCase(safeValue.charAt(i));
            if (Character.isLetterOrDigit(current)) {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void addLines(List<String> lines, String value) {
        for (String line : safe(value).split("\\R")) {
            String clean = line.trim();
            if (!clean.isBlank()) {
                lines.add(clean);
            }
        }
    }

    private record ContactPart(String text, DocumentLinkOptions linkOptions) {
    }

    private record WorkEntry(String title, String subtitle, String date, String description) {
    }
}
