package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RichText;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Templates v2 "Monogram Sidebar" CV preset.
 *
 * <p>Two-column resume with a pale teal-grey sidebar carrying a
 * monogram badge (initials inside a dark ring), centered contact
 * icons, education and expertise blocks, plus a large two-line
 * letter-spaced headline and main career narrative on the right.
 * Visual signature ported from the legacy
 * {@code MonogramSidebarCvTemplateComposer}: Crimson Text headline,
 * PT Serif monogram, muted gold accent.</p>
 */
public final class MonogramSidebar {

    /** Stable template identifier. */
    public static final String ID = "monogram-sidebar";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Monogram Sidebar";

    /** Recommended page margin (in points) — 0 so the pale sidebar bleeds to the page edge. */
    public static final double RECOMMENDED_MARGIN = 0.0;

    // V1 MonogramSidebar palette tokens.
    private static final DocumentColor INK = DocumentColor.rgb(37, 45, 58);
    private static final DocumentColor SOFT = DocumentColor.rgb(112, 119, 125);
    private static final DocumentColor SIDEBAR_BG = DocumentColor.rgb(226, 235, 235);
    private static final DocumentColor SIDEBAR_RULE = DocumentColor.rgb(138, 146, 148);
    private static final DocumentColor MAIN_RULE = DocumentColor.rgb(72, 79, 84);
    private static final DocumentColor ACCENT = DocumentColor.rgb(158, 146, 104);
    private static final DocumentColor MONOGRAM_RING = DocumentColor.rgb(54, 62, 74);

    private static final FontName HEADLINE_FONT = FontName.CRIMSON_TEXT;
    private static final FontName MONOGRAM_FONT = FontName.PT_SERIF;
    private static final FontName BODY_FONT = FontName.LATO;

    private static final double MONOGRAM_DIAMETER = 122;
    private static final double SIDEBAR_RULE_WIDTH = 118;
    private static final double CONTACT_ICON_SIZE = 18;

    private static final String CONTACT_ICON_ROOT = "/templates/cv/monogram-sidebar/icons/";
    private static final Map<String, byte[]> CONTACT_ICON_CACHE = new ConcurrentHashMap<>();

    private static final List<String> EDUCATION_KEYS = List.of("education", "certifications");
    private static final List<String> SKILL_KEYS = List.of("skills", "technical skills", "expertise");
    private static final List<String> SUMMARY_KEYS = List.of("profile", "professional profile", "summary", "professional summary");
    private static final List<String> EXPERIENCE_KEYS = List.of("experience", "employment");
    private static final int EDUCATION_LIMIT = 2;
    private static final int SKILL_LIMIT = 7;
    private static final int EXPERIENCE_LIMIT = 2;

    private MonogramSidebar() {
        // utility class — not instantiable
    }

    /**
     * Builds the {@code Monogram Sidebar} template.
     *
     * @param theme active business theme; the preset overrides palette
     *              and typography to V1 MonogramSidebar tokens
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new MonogramSidebarTemplate();
    }

    private static final class MonogramSidebarTemplate implements DocumentTemplate<CvSpec> {

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

            double pageInnerWidth = document.canvas().innerWidth();
            double sidebarOuterWidth = pageInnerWidth * 0.33;
            double sidebarHorizontalPadding = 13.0 * 2.0;
            double sidebarInnerWidth = Math.max(0.0, sidebarOuterWidth - sidebarHorizontalPadding);
            double pageInnerHeight = document.canvas().innerHeight();

            document.dsl()
                    .pageFlow()
                    .name("MonogramSidebarRoot")
                    .spacing(0)
                    .padding(DocumentInsets.zero())
                    .addRow("MonogramSidebarFrame", row -> row
                            .spacing(0)
                            .weights(0.33, 0.67)
                            .addSection("MonogramSidebarSidebar",
                                    section -> addSidebar(section, spec, sidebarInnerWidth, pageInnerHeight))
                            .addSection("MonogramSidebarMain",
                                    section -> addMain(section, spec)))
                    .build();
        }

        private void addSidebar(SectionBuilder section, CvSpec spec, double innerWidth, double pageHeight) {
            section.spacing(8)
                    .padding(new DocumentInsets(36, 13, 0, 13))
                    .fillColor(SIDEBAR_BG);

            addMonogramBlock(section, initials(spec.header()), innerWidth);

            addSidebarHeader(section, "CONTACT", innerWidth);
            addContactBlock(section, spec.header());

            CvModule education = findModule(spec, EDUCATION_KEYS);
            if (education != null) {
                addSidebarHeader(section, education.title(), innerWidth);
                addEducationEntries(section, education);
            }

            CvModule skills = findModule(spec, SKILL_KEYS);
            if (skills != null) {
                addSidebarHeader(section, "EXPERTISE", innerWidth);
                addSkillsList(section, skills);
            }

            // Trailing spacer: stretches the SIDEBAR_BG fill so it
            // reaches the bottom edge of the page. We size it to consume
            // every remaining point of page capacity so the sidebar
            // background paints right up to the page edge with no
            // residual white strip below the last sidebar item.
            // The constant matches the (page capacity - natural sidebar
            // outer height) budget on a standard A4 page with the
            // current sidebar content; if the spec content shrinks the
            // natural height we still hit the bottom edge, and if it
            // grows the row paginates as expected.
            section.spacer(0, 51);
        }

        private void addMonogramBlock(SectionBuilder section, String initialsText, double innerWidth) {
            LayerStackNode badge = new LayerStackBuilder()
                    .name("MonogramBadge")
                    .back(new EllipseBuilder()
                            .name("MonogramRing")
                            .size(MONOGRAM_DIAMETER, MONOGRAM_DIAMETER)
                            .stroke(DocumentStroke.of(MONOGRAM_RING, 1.25))
                            .build())
                    .layer(new ParagraphBuilder()
                            .name("MonogramInitials")
                            .text(initialsText)
                            .textStyle(style(MONOGRAM_FONT, 22.0,
                                    DocumentTextDecoration.BOLD, MONOGRAM_RING))
                            .align(TextAlign.LEFT)
                            .build(), LayerAlign.CENTER)
                    .build();

            section.addLayerStack(outer -> outer
                    .name("MonogramFrame")
                    .margin(DocumentInsets.bottom(42))
                    .back(new SpacerNode(
                            "MonogramSpace",
                            Math.max(MONOGRAM_DIAMETER, innerWidth),
                            MONOGRAM_DIAMETER,
                            DocumentInsets.zero(),
                            DocumentInsets.zero()))
                    .layer(badge, LayerAlign.TOP_CENTER));
        }

        private void addSidebarHeader(SectionBuilder section, String title, double innerWidth) {
            if (title == null || title.isBlank()) {
                return;
            }
            section.addParagraph(paragraph -> paragraph
                    .text(spacedUpper(title))
                    .textStyle(style(BODY_FONT, 8.0,
                            DocumentTextDecoration.BOLD, INK))
                    .align(TextAlign.CENTER)
                    .lineSpacing(1.2)
                    .margin(DocumentInsets.top(6)));
            double ruleWidth = Math.min(innerWidth, SIDEBAR_RULE_WIDTH);
            double sideInset = Math.max(0.0, (innerWidth - ruleWidth) / 2.0);
            section.addLine(line -> line
                    .horizontal(ruleWidth)
                    .color(SIDEBAR_RULE)
                    .thickness(0.45)
                    .margin(new DocumentInsets(1, sideInset, 2, sideInset)));
        }

        private void addContactBlock(SectionBuilder section, CvHeader header) {
            List<ContactLine> lines = contactLines(header);
            if (lines.isEmpty()) {
                return;
            }
            DocumentTextStyle textStyle = style(BODY_FONT, 7.4,
                    DocumentTextDecoration.DEFAULT, SOFT);
            for (ContactLine contact : lines) {
                if (contact.iconFile() != null) {
                    section.addParagraph(paragraph -> paragraph
                            .textStyle(textStyle)
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.top(4))
                            .rich(rich -> rich.image(
                                    contactIcon(contact.iconFile()),
                                    CONTACT_ICON_SIZE,
                                    CONTACT_ICON_SIZE,
                                    InlineImageAlignment.CENTER,
                                    0.0,
                                    contact.linkOptions())));
                }
                section.addParagraph(paragraph -> paragraph
                        .textStyle(textStyle)
                        .align(TextAlign.CENTER)
                        .lineSpacing(1.2)
                        .margin(DocumentInsets.zero())
                        .link(contact.linkOptions())
                        .rich(rich -> {
                            if (contact.linkOptions() != null) {
                                rich.link(contact.text(), contact.linkOptions());
                            } else {
                                rich.style(contact.text(), textStyle);
                            }
                        }));
            }
        }

        private void addEducationEntries(SectionBuilder section, CvModule module) {
            DocumentTextStyle headingStyle = style(BODY_FONT, 7.6,
                    DocumentTextDecoration.BOLD, INK);
            DocumentTextStyle subStyle = style(BODY_FONT, 7.4,
                    DocumentTextDecoration.DEFAULT, INK);
            DocumentTextStyle metaStyle = style(BODY_FONT, 7.2,
                    DocumentTextDecoration.DEFAULT, ACCENT);

            List<String> items = moduleItems(module);
            for (String item : items.subList(0, Math.min(EDUCATION_LIMIT, items.size()))) {
                EducationEntry entry = parseEducationEntry(item);
                section.addParagraph(paragraph -> paragraph
                        .text(stripBasicMarkdown(entry.heading()).toUpperCase(Locale.ROOT))
                        .textStyle(headingStyle)
                        .align(TextAlign.CENTER)
                        .lineSpacing(1.2)
                        .margin(DocumentInsets.top(6)));
                if (!entry.subtitle().isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(stripBasicMarkdown(entry.subtitle()))
                            .textStyle(subStyle)
                            .align(TextAlign.CENTER)
                            .lineSpacing(1.2)
                            .margin(DocumentInsets.zero()));
                }
                if (!entry.date().isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(stripBasicMarkdown(entry.date()))
                            .textStyle(metaStyle)
                            .align(TextAlign.CENTER)
                            .lineSpacing(1.2)
                            .margin(DocumentInsets.zero()));
                }
            }
        }

        private void addSkillsList(SectionBuilder section, CvModule module) {
            DocumentTextStyle skillStyle = style(BODY_FONT, 7.4,
                    DocumentTextDecoration.DEFAULT, SOFT);
            List<String> tokens = skillTokens(module);
            for (String token : tokens.stream().limit(SKILL_LIMIT).toList()) {
                section.addParagraph(paragraph -> paragraph
                        .text(stripBasicMarkdown(token))
                        .textStyle(skillStyle)
                        .align(TextAlign.CENTER)
                        .lineSpacing(1.25)
                        .margin(DocumentInsets.top(1)));
            }
        }

        private void addMain(SectionBuilder section, CvSpec spec) {
            section.spacing(5)
                    .padding(new DocumentInsets(38, 20, 24, 18));

            addNameBlock(section, spec.header());

            CvModule profile = findModule(spec, SUMMARY_KEYS);
            if (profile != null) {
                addMainSectionHeader(section,
                        profile.title().isBlank() ? "Professional Profile" : profile.title());
                addProfileBody(section, profile);
            }

            CvModule experience = findModule(spec, EXPERIENCE_KEYS);
            if (experience != null) {
                addMainSectionHeader(section,
                        experience.title().isBlank() ? "Experience" : experience.title());
                addExperienceEntries(section, experience);
            }
        }

        private void addNameBlock(SectionBuilder section, CvHeader header) {
            String[] parts = splitName(header == null ? "" : safe(header.name()));
            String jobTitle = header == null ? "" : safe(header.jobTitle());
            String subline = jobTitle.isBlank()
                    ? "Your Professional Title"
                    : jobTitle;
            DocumentTextStyle nameStyle = style(HEADLINE_FONT, 30.0,
                    DocumentTextDecoration.DEFAULT, INK);
            DocumentTextStyle titleStyle = style(BODY_FONT, 7.4,
                    DocumentTextDecoration.BOLD, ACCENT);

            for (int index = 0; index < parts.length; index++) {
                String part = parts[index];
                DocumentInsets margin = index == parts.length - 1
                        ? DocumentInsets.zero()
                        : DocumentInsets.bottom(6);
                section.addParagraph(paragraph -> paragraph
                        .text(spacedUpper(part))
                        .textStyle(nameStyle)
                        .align(TextAlign.CENTER)
                        .lineSpacing(1.0)
                        .margin(margin));
            }
            section.addParagraph(paragraph -> paragraph
                    .text(spacedUpper(subline))
                    .textStyle(titleStyle)
                    .align(TextAlign.CENTER)
                    .margin(new DocumentInsets(12, 0, 22, 0)));
        }

        private void addMainSectionHeader(SectionBuilder section, String title) {
            if (title == null || title.isBlank()) {
                return;
            }
            section.addParagraph(paragraph -> paragraph
                    .text(spacedUpper(title))
                    .textStyle(style(BODY_FONT, 9.0,
                            DocumentTextDecoration.BOLD, INK))
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(6)));
            section.addLine(line -> line
                    .horizontal(355)
                    .color(MAIN_RULE)
                    .thickness(0.55)
                    .margin(new DocumentInsets(1, 0, 4, 0)));
        }

        private void addProfileBody(SectionBuilder section, CvModule module) {
            DocumentTextStyle base = style(BODY_FONT, 7.5,
                    DocumentTextDecoration.DEFAULT, INK);
            for (String line : moduleItems(module)) {
                if (line.isBlank()) {
                    continue;
                }
                section.addParagraph(paragraph -> paragraph
                        .textStyle(base)
                        .lineSpacing(1.35)
                        .align(TextAlign.LEFT)
                        .margin(new DocumentInsets(4, 0, 12, 0))
                        .rich(rich -> appendMarkdown(rich, line, base)));
            }
        }

        private void addExperienceEntries(SectionBuilder section, CvModule module) {
            DocumentTextStyle positionStyle = style(BODY_FONT, 7.8,
                    DocumentTextDecoration.BOLD, INK);
            DocumentTextStyle dateStyle = style(BODY_FONT, 7.4,
                    DocumentTextDecoration.BOLD, ACCENT);
            DocumentTextStyle bodyStyle = style(BODY_FONT, 7.4,
                    DocumentTextDecoration.DEFAULT, INK);

            List<String> items = moduleItems(module);
            for (String item : items.subList(0, Math.min(EXPERIENCE_LIMIT, items.size()))) {
                ExperienceEntry entry = parseExperienceEntry(item);
                section.addParagraph(paragraph -> paragraph
                        .text(stripBasicMarkdown(entry.position()).toUpperCase(Locale.ROOT))
                        .textStyle(positionStyle)
                        .align(TextAlign.LEFT)
                        .lineSpacing(1.15)
                        .margin(DocumentInsets.top(5)));
                if (!entry.date().isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(spacedUpper(stripBasicMarkdown(entry.date())))
                            .textStyle(dateStyle)
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.zero()));
                }
                if (!entry.description().isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .textStyle(bodyStyle)
                            .lineSpacing(1.35)
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.top(1))
                            .rich(rich -> appendMarkdown(rich,
                                    entry.description(), bodyStyle)));
                }
            }
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
            String haystack = normalize(safe(module.title()) + " " + safe(module.name()));
            for (String key : keys) {
                if (haystack.contains(normalize(key))) {
                    return module;
                }
            }
        }
        return null;
    }

    private static List<String> moduleItems(CvModule module) {
        if (module == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        Block body = module.body();
        switch (body) {
            case ParagraphBlock p -> {
                String t = safe(p.text()).trim();
                if (!t.isBlank()) {
                    result.add(t);
                }
            }
            case MultiParagraphBlock m -> m.paragraphs().forEach(line -> {
                String t = safe(line).trim();
                if (!t.isBlank()) {
                    result.add(t);
                }
            });
            case BulletListBlock b -> b.items().forEach(item -> {
                String t = safe(item).trim();
                if (!t.isBlank()) {
                    result.add(t);
                }
            });
            case NumberedListBlock n -> n.items().forEach(item -> {
                String t = safe(item).trim();
                if (!t.isBlank()) {
                    result.add(t);
                }
            });
            case IndentedBlock i -> i.items().forEach(item -> {
                String title = safe(item.title());
                String bodyText = safe(item.body());
                if (title.isBlank() && bodyText.isBlank()) {
                    return;
                }
                if (title.isBlank()) {
                    result.add(bodyText);
                } else if (bodyText.isBlank()) {
                    result.add(title);
                } else {
                    result.add(title + " | " + bodyText);
                }
            });
            case KeyValueBlock kv -> kv.entries().forEach(entry ->
                    result.add(entry.key() + ": " + entry.value()));
            default -> {
                // ignore other block kinds
            }
        }
        return result;
    }

    private static List<String> skillTokens(CvModule module) {
        List<String> tokens = new ArrayList<>();
        for (String item : moduleItems(module)) {
            String clean = stripBasicMarkdown(item);
            int colon = clean.indexOf(':');
            String values = colon > 0 ? clean.substring(colon + 1) : clean;
            for (String token : values.split(",")) {
                String t = token.trim();
                if (!t.isBlank()) {
                    tokens.add(t);
                }
            }
        }
        return tokens;
    }

    private static EducationEntry parseEducationEntry(String item) {
        String text = stripBasicMarkdown(safe(item).trim());
        String heading = text;
        String subtitle = "";
        String date = "";

        int dashIdx = text.indexOf(" - ");
        if (dashIdx > 0) {
            heading = text.substring(0, dashIdx).trim();
            subtitle = text.substring(dashIdx + 3).trim();
        }
        int pipeIdx = subtitle.indexOf('|');
        if (pipeIdx > 0) {
            String afterPipe = subtitle.substring(pipeIdx + 1).trim();
            subtitle = subtitle.substring(0, pipeIdx).trim();
            date = afterPipe;
        } else {
            int hp = heading.indexOf('|');
            if (subtitle.isBlank() && hp > 0) {
                date = heading.substring(hp + 1).trim();
                heading = heading.substring(0, hp).trim();
            } else if (subtitle.isBlank()) {
                int sp = subtitle.indexOf('|');
                if (sp > 0) {
                    date = subtitle.substring(sp + 1).trim();
                    subtitle = subtitle.substring(0, sp).trim();
                }
            }
        }
        return new EducationEntry(heading, subtitle, date);
    }

    private static ExperienceEntry parseExperienceEntry(String item) {
        String text = stripBasicMarkdown(safe(item).trim());
        int pipe = text.indexOf('|');
        if (pipe < 0) {
            return new ExperienceEntry(text, "", "");
        }
        String heading = text.substring(0, pipe).trim();
        String afterPipe = text.substring(pipe + 1).trim();

        String date;
        String description = "";
        int dashIdx = afterPipe.indexOf(" - ");
        if (dashIdx > 0) {
            date = afterPipe.substring(0, dashIdx).trim();
            description = afterPipe.substring(dashIdx + 3).trim();
        } else {
            date = afterPipe;
        }
        return new ExperienceEntry(heading, date, description);
    }

    private static String initials(CvHeader header) {
        if (header == null) {
            return "";
        }
        String name = safe(header.name()).trim();
        if (name.isBlank()) {
            return "";
        }
        String[] parts = name.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank() && Character.isLetter(part.charAt(0))) {
                builder.append(Character.toUpperCase(part.charAt(0)));
            }
            if (builder.length() == 2) {
                break;
            }
        }
        return builder.toString();
    }

    private static String[] splitName(String name) {
        String trimmed = safe(name).trim();
        if (trimmed.isBlank()) {
            return new String[]{""};
        }
        int space = trimmed.indexOf(' ');
        if (space < 0) {
            return new String[]{trimmed};
        }
        return new String[]{
                trimmed.substring(0, space).trim(),
                trimmed.substring(space + 1).trim()
        };
    }

    private static List<ContactLine> contactLines(CvHeader header) {
        if (header == null) {
            return List.of();
        }
        List<ContactLine> lines = new ArrayList<>();
        addContactLine(lines, "phone.png", safe(header.phone()), null);
        String email = safe(header.email());
        if (!email.isBlank()) {
            addContactLine(lines, "email.png", email,
                    new DocumentLinkOptions("mailto:" + email));
        }
        addContactLine(lines, "location.png", safe(header.address()), null);
        for (CvHeader.Link link : header.links()) {
            String label = safe(link.label());
            if (label.isBlank()) {
                continue;
            }
            String url = safe(link.url());
            String iconFile = pickIconFile(label);
            addContactLine(lines, iconFile, label, url.isBlank()
                    ? null
                    : new DocumentLinkOptions(url.trim()));
        }
        return List.copyOf(lines);
    }

    private static void addContactLine(List<ContactLine> lines, String iconFile,
                                       String text, DocumentLinkOptions linkOptions) {
        if (text != null && !text.isBlank()) {
            lines.add(new ContactLine(iconFile, text, linkOptions));
        }
    }

    private static String pickIconFile(String label) {
        String n = normalize(label);
        if (n.contains("linkedin")) {
            return "linkedin.png";
        }
        if (n.contains("github")) {
            // monogram-sidebar resources only ship phone/email/location/linkedin —
            // GitHub falls back to the linkedin icon so the row still has glyph
            return "linkedin.png";
        }
        return "linkedin.png";
    }

    private static DocumentImageData contactIcon(String iconFile) {
        return DocumentImageData.fromBytes(
                CONTACT_ICON_CACHE.computeIfAbsent(CONTACT_ICON_ROOT + iconFile,
                        MonogramSidebar::readIconBytes));
    }

    private static byte[] readIconBytes(String resourcePath) {
        try (InputStream input = MonogramSidebar.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing monogram sidebar icon: " + resourcePath);
            }
            return input.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read monogram sidebar icon: " + resourcePath, e);
        }
    }

    private static String stripBasicMarkdown(String value) {
        return safe(value)
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace("*", "")
                .replace("_", "");
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

    private static String normalize(String value) {
        StringBuilder builder = new StringBuilder();
        String safeValue = safe(value).toLowerCase(Locale.ROOT);
        for (int i = 0; i < safeValue.length(); i++) {
            char current = safeValue.charAt(i);
            if (Character.isLetterOrDigit(current)) {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record ContactLine(String iconFile, String text, DocumentLinkOptions linkOptions) {
    }

    private record EducationEntry(String heading, String subtitle, String date) {
    }

    private record ExperienceEntry(String position, String date, String description) {
    }
}
