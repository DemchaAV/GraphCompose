package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.RichText;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
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
 * Templates v2 "Sidebar Portrait" CV preset.
 *
 * <p>Two-column resume with a soft-grey portrait sidebar on the left
 * (circular photo, contact stack with inline icons, Education / Key
 * Skills / Languages summary) and the main career narrative on the
 * right (large serif name, professional profile, experience timeline
 * of bold position + subtitle + description). Visual signature
 * ported from the legacy {@code SidebarPortraitCvTemplateComposer}:
 * Crimson Text serif for the hero name, Lato body, restrained grey
 * palette.</p>
 */
public final class SidebarPortrait {

    /** Stable template identifier. */
    public static final String ID = "sidebar-portrait";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Sidebar Portrait";

    /** Recommended page margin (in points) — V1 SidebarPortrait used 0 (sidebar bleeds to edge). */
    public static final double RECOMMENDED_MARGIN = 0.0;

    // V1 SidebarPortrait palette tokens.
    private static final DocumentColor INK = DocumentColor.rgb(34, 34, 34);
    private static final DocumentColor SOFT = DocumentColor.rgb(85, 85, 85);
    private static final DocumentColor SIDEBAR_BG = DocumentColor.rgb(241, 240, 237);
    private static final DocumentColor SIDEBAR_INK = INK;
    private static final DocumentColor SIDEBAR_SOFT = SOFT;
    private static final DocumentColor ACCENT = DocumentColor.rgb(106, 106, 106);
    private static final DocumentColor RULE = DocumentColor.rgb(178, 178, 178);

    private static final FontName DISPLAY_FONT = FontName.CRIMSON_TEXT;
    private static final FontName BODY_FONT = FontName.LATO;

    private static final double SIDEBAR_INNER_WIDTH = 156.4;
    private static final double PHOTO_DIAMETER = 98.0;
    /**
     * Vertical offset of the hero strip from the top of the main
     * column. Tuned so the hero strip's vertical centre lines up with
     * the photo's centre, producing the half-on-sidebar / half-on-hero
     * portrait effect.
     */
    private static final double HERO_TOP_OFFSET = 70.0;

    private static final String TEMPLATE_ASSET_ROOT = "/templates/cv/sidebar-portrait/";
    private static final String CONTACT_ICON_ROOT = TEMPLATE_ASSET_ROOT + "icons/";
    private static final String PORTRAIT_FILE = "portrait.png";

    private static final Map<String, byte[]> ASSET_CACHE = new ConcurrentHashMap<>();

    private static final List<String> EDUCATION_KEYS = List.of("education", "certifications");
    private static final List<String> SKILL_KEYS = List.of("skills", "technical skills");
    private static final List<String> LANGUAGE_KEYS = List.of("languages", "additional information", "additional");
    private static final List<String> SUMMARY_KEYS = List.of("profile", "professional profile", "summary", "professional summary");
    private static final List<String> EXPERIENCE_KEYS = List.of("experience", "employment");
    private static final int EDUCATION_LIMIT = 2;
    private static final int SKILL_LIMIT = 5;
    private static final int LANGUAGE_LIMIT = 3;
    private static final int EXPERIENCE_LIMIT = 2;

    private SidebarPortrait() {
        // utility class — not instantiable
    }

    /**
     * Builds the {@code Sidebar Portrait} template.
     *
     * @param theme active business theme; the preset overrides palette
     *              and typography to V1 SidebarPortrait tokens
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new SidebarPortraitTemplate();
    }

    private static final class SidebarPortraitTemplate implements DocumentTemplate<CvSpec> {

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

            // Ask the canvas for the page-content height so the sidebar
            // fill stretches all the way to the bottom edge instead of
            // ending at the last sidebar line.
            double pageHeight = document.canvas().innerHeight();

            document.dsl()
                    .pageFlow()
                    .name("SidebarPortraitRoot")
                    .spacing(0)
                    .padding(DocumentInsets.zero())
                    .addRow("SidebarPortraitBodyRow", row -> row
                            .spacing(0)
                            .weights(0.34, 0.66)
                            .addSection("SidebarPortraitBodySidebar",
                                    section -> addSidebar(section, spec, pageHeight))
                            .addSection("SidebarPortraitBodyMain",
                                    section -> {
                                        section.spacing(0)
                                                .padding(DocumentInsets.zero());
                                        addNameBlock(section, spec.header());
                                        addMain(section, spec);
                                    }))
                    .build();
        }

        private void addSidebar(SectionBuilder section, CvSpec spec, double pageHeight) {
            section.spacing(9)
                    .padding(new DocumentInsets(54, 20, 45.45, 26))
                    .fillColor(SIDEBAR_BG);

            addPhotoBlock(section);
            addContactBlock(section, spec.header());

            CvModule education = findModule(spec, EDUCATION_KEYS);
            if (education != null) {
                addSidebarHeader(section, "Education");
                addEducationEntries(section, education);
            }

            CvModule skills = findModule(spec, SKILL_KEYS);
            if (skills != null) {
                addSidebarHeader(section, "Key Skills");
                addSkillsList(section, skills);
            }

            CvModule languages = findModule(spec, LANGUAGE_KEYS);
            if (languages != null) {
                addSidebarHeader(section, "Languages");
                addLanguageList(section, languages);
            }

            // Trailing spacer: stretches the SIDEBAR_BG fill so it
            // reaches the bottom edge of the page. Sized adaptively
            // from the canvas innerHeight so the row honours page
            // capacity on whatever page size is active (A4 / Letter /
            // smaller test fixtures). The constant 787pt corresponds
            // to the natural sidebar outer height (photo + contact +
            // Education + Skills + Languages with the current sample
            // data) plus a 1pt safety margin against floating-point
            // rounding in the layout's row-capacity check.
            double maxStretch = Math.max(0.0, pageHeight - 820.0);
            if (maxStretch > 0.0) {
                section.spacer(0, maxStretch);
            }
        }

        private void addPhotoBlock(SectionBuilder section) {
            double sideInset = Math.max(0.0, (SIDEBAR_INNER_WIDTH - PHOTO_DIAMETER) / 2.0);
            section.addImage(image -> image
                    .name("SidebarPortraitPhoto")
                    .source(portraitImage())
                    .size(PHOTO_DIAMETER, PHOTO_DIAMETER)
                    .margin(new DocumentInsets(0, sideInset, 17, sideInset)));
        }

        private void addContactBlock(SectionBuilder section, CvHeader header) {
            List<ContactLine> lines = contactLines(header);
            if (lines.isEmpty()) {
                return;
            }
            DocumentTextStyle textStyle = style(BODY_FONT, 8.3,
                    DocumentTextDecoration.DEFAULT, SIDEBAR_INK);
            for (ContactLine contact : lines) {
                section.addParagraph(paragraph -> paragraph
                        .textStyle(textStyle)
                        .align(TextAlign.LEFT)
                        .lineSpacing(1.35)
                        .margin(DocumentInsets.top(3))
                        .link(contact.linkOptions())
                        .rich(rich -> {
                            if (contact.iconFile() != null) {
                                rich.image(contactIcon(contact.iconFile()), 10.0, 10.0,
                                        InlineImageAlignment.CENTER, 0.0, contact.linkOptions());
                                rich.style("  ", textStyle);
                            }
                            if (contact.linkOptions() != null) {
                                rich.link(contact.text(), contact.linkOptions());
                            } else {
                                rich.style(contact.text(), textStyle);
                            }
                        }));
            }
        }

        private void addSidebarHeader(SectionBuilder section, String title) {
            if (title == null || title.isBlank()) {
                return;
            }
            section.addLine(line -> line
                    .horizontal(50)
                    .color(ACCENT)
                    .thickness(0.75)
                    .margin(new DocumentInsets(12, 0, 7, 0)));
            section.addParagraph(paragraph -> paragraph
                    .text(spacedUpper(title))
                    .textStyle(style(BODY_FONT, 10.8,
                            DocumentTextDecoration.BOLD, SIDEBAR_INK))
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.zero()));
        }

        private void addEducationEntries(SectionBuilder section, CvModule module) {
            List<String> items = moduleItems(module);
            for (String item : items.subList(0, Math.min(EDUCATION_LIMIT, items.size()))) {
                EducationEntry entry = parseEducationEntry(item);
                DocumentTextStyle headingStyle = style(BODY_FONT, 8.4,
                        DocumentTextDecoration.BOLD, SIDEBAR_INK);
                DocumentTextStyle metaStyle = style(BODY_FONT, 7.8,
                        DocumentTextDecoration.DEFAULT, SIDEBAR_SOFT);

                section.addParagraph(paragraph -> paragraph
                        .text(stripBasicMarkdown(entry.heading()).toUpperCase(Locale.ROOT))
                        .textStyle(headingStyle)
                        .align(TextAlign.LEFT)
                        .lineSpacing(1.2)
                        .margin(DocumentInsets.top(6)));
                if (!entry.subtitle().isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(stripBasicMarkdown(entry.subtitle()))
                            .textStyle(metaStyle)
                            .align(TextAlign.LEFT)
                            .lineSpacing(1.2)
                            .margin(DocumentInsets.zero()));
                }
                if (!entry.date().isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(stripBasicMarkdown(entry.date()))
                            .textStyle(metaStyle)
                            .align(TextAlign.LEFT)
                            .lineSpacing(1.2)
                            .margin(DocumentInsets.zero()));
                }
            }
        }

        private void addSkillsList(SectionBuilder section, CvModule module) {
            DocumentTextStyle skillStyle = style(BODY_FONT, 8.0,
                    DocumentTextDecoration.DEFAULT, SIDEBAR_INK);
            List<String> tokens = skillTokens(module);
            for (String token : tokens.stream().limit(SKILL_LIMIT).toList()) {
                section.addParagraph(paragraph -> paragraph
                        .text(stripBasicMarkdown(token))
                        .textStyle(skillStyle)
                        .lineSpacing(1.35)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(3)));
            }
        }

        private void addLanguageList(SectionBuilder section, CvModule module) {
            DocumentTextStyle nameStyle = style(BODY_FONT, 8.1,
                    DocumentTextDecoration.BOLD, SIDEBAR_INK);
            DocumentTextStyle metaStyle = style(BODY_FONT, 7.9,
                    DocumentTextDecoration.DEFAULT, SIDEBAR_SOFT);
            List<String> items = languageItems(module);
            for (String item : items.subList(0, Math.min(LANGUAGE_LIMIT, items.size()))) {
                String text = stripBasicMarkdown(item);
                int paren = text.indexOf('(');
                String langName = paren > 0
                        ? text.substring(0, paren).trim()
                        : text.trim();
                String level = paren > 0
                        ? text.substring(paren).trim()
                        : "";
                if (langName.isBlank()) {
                    continue;
                }
                section.addParagraph(paragraph -> paragraph
                        .textStyle(nameStyle)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(4))
                        .rich(rich -> {
                            rich.style(langName.toUpperCase(Locale.ROOT), nameStyle);
                            if (!level.isBlank()) {
                                rich.style("  " + level, metaStyle);
                            }
                        }));
            }
        }

        private void addMain(SectionBuilder section, CvSpec spec) {
            section.addSection("SidebarPortraitContent", content -> {
                content.spacing(10)
                        .padding(new DocumentInsets(24, 34, 24, 34));

                CvModule profile = findModule(spec, SUMMARY_KEYS);
                if (profile != null) {
                    addMainSectionHeader(content, "Professional Profile");
                    addProfileBody(content, profile);
                }

                CvModule experience = findModule(spec, EXPERIENCE_KEYS);
                if (experience != null) {
                    addMainSectionHeader(content, "Experience");
                    addExperienceEntries(content, experience);
                }
            });
        }

        private void addNameBlock(SectionBuilder section, CvHeader header) {
            String displayName = header == null ? "" : safe(header.name());
            String jobTitle = header == null ? "" : safe(header.jobTitle());
            String subline = jobTitle.isBlank()
                    ? "Your Professional Title Goes Here"
                    : jobTitle;
            section.addSection("SidebarPortraitHero", hero -> hero
                    .fillColor(SIDEBAR_BG)
                    .padding(new DocumentInsets(8, 34, 6, 34))
                    .spacing(3)
                    .margin(DocumentInsets.top(HERO_TOP_OFFSET))
                    .addParagraph(paragraph -> paragraph
                            .text(displayName)
                            .textStyle(style(DISPLAY_FONT, 28.0,
                                    DocumentTextDecoration.BOLD, SIDEBAR_INK))
                            .align(TextAlign.CENTER)
                            .lineSpacing(1.0)
                            .margin(DocumentInsets.zero()))
                    .addParagraph(paragraph -> paragraph
                            .text(spacedUpper(subline))
                            .textStyle(style(BODY_FONT, 8.4,
                                    DocumentTextDecoration.DEFAULT, INK))
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.zero())));
        }

        private void addMainSectionHeader(SectionBuilder section, String title) {
            if (title == null || title.isBlank()) {
                return;
            }
            section.addParagraph(paragraph -> paragraph
                    .text(spacedUpper(title))
                    .textStyle(style(BODY_FONT, 12.0,
                            DocumentTextDecoration.BOLD, INK))
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(8)));
            section.addLine(line -> line
                    .horizontal(346)
                    .color(RULE)
                    .thickness(0.55)
                    .margin(new DocumentInsets(2, 0, 7, 0)));
        }

        private void addProfileBody(SectionBuilder section, CvModule module) {
            DocumentTextStyle base = style(BODY_FONT, 9.4,
                    DocumentTextDecoration.DEFAULT, INK);
            for (String line : moduleParagraphs(module)) {
                if (line.isBlank()) {
                    continue;
                }
                section.addParagraph(paragraph -> paragraph
                        .textStyle(base)
                        .lineSpacing(1.35)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(2))
                        .rich(rich -> appendMarkdown(rich, line, base)));
            }
        }

        private void addExperienceEntries(SectionBuilder section, CvModule module) {
            DocumentTextStyle positionStyle = style(BODY_FONT, 10.0,
                    DocumentTextDecoration.BOLD, INK);
            DocumentTextStyle subtitleStyle = style(BODY_FONT, 9.2,
                    DocumentTextDecoration.DEFAULT, INK);
            DocumentTextStyle bodyStyle = style(BODY_FONT, 9.0,
                    DocumentTextDecoration.DEFAULT, INK);

            List<String> items = moduleItems(module);
            for (String item : items.subList(0, Math.min(EXPERIENCE_LIMIT, items.size()))) {
                ExperienceEntry entry = parseExperienceEntry(item);
                section.addParagraph(paragraph -> paragraph
                        .text(stripBasicMarkdown(entry.position()).toUpperCase(Locale.ROOT))
                        .textStyle(positionStyle)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(8)));
                if (!entry.subtitle().isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(stripBasicMarkdown(entry.subtitle()))
                            .textStyle(subtitleStyle)
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.zero()));
                }
                if (!entry.description().isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .textStyle(bodyStyle)
                            .lineSpacing(1.35)
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.top(2))
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

    private static List<String> moduleParagraphs(CvModule module) {
        if (module == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        Block body = module.body();
        switch (body) {
            case ParagraphBlock p -> result.add(safe(p.text()).trim());
            case MultiParagraphBlock m -> {
                for (String line : m.paragraphs()) {
                    String t = safe(line).trim();
                    if (!t.isBlank()) {
                        result.add(t);
                    }
                }
            }
            default -> {
                for (String item : moduleItems(module)) {
                    result.add(item);
                }
            }
        }
        return result;
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

    private static List<String> languageItems(CvModule module) {
        List<String> result = new ArrayList<>();
        for (String item : moduleItems(module)) {
            String text = stripBasicMarkdown(item).trim();
            String lower = text.toLowerCase(Locale.ROOT);
            if (lower.startsWith("languages:")) {
                String langs = text.substring("languages:".length()).trim();
                for (String part : langs.split(",")) {
                    String p = part.trim();
                    if (!p.isBlank()) {
                        result.add(p);
                    }
                }
            } else if (text.contains("|") || text.contains("(")) {
                result.add(text);
            }
        }
        return result;
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
        } else if (subtitle.isBlank()) {
            int hp = heading.indexOf('|');
            if (hp > 0) {
                date = heading.substring(hp + 1).trim();
                heading = heading.substring(0, hp).trim();
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

        String position = heading;
        String details = "";
        int comma = heading.indexOf(", ");
        if (comma > 0) {
            position = heading.substring(0, comma).trim();
            details = heading.substring(comma + 2).trim();
        }
        StringBuilder subtitle = new StringBuilder();
        if (!details.isBlank()) {
            subtitle.append(details);
        }
        if (looksLikeDate(date)) {
            if (subtitle.length() > 0) {
                subtitle.append(" | ");
            }
            subtitle.append(date);
        }
        return new ExperienceEntry(position, subtitle.toString(), description);
    }

    private static boolean looksLikeDate(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.matches(".*(\\d{4}|present|current|now|ongoing).*")
                && (lower.contains("-") || lower.contains("–") || lower.contains("—") || lower.contains("to"));
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
            return "github.png";
        }
        if (n.contains("dribbble")) {
            return "dribbble.png";
        }
        if (n.contains("google")) {
            return "google.png";
        }
        return "linkedin.png";
    }

    private static DocumentImageData contactIcon(String iconFile) {
        return DocumentImageData.fromBytes(
                ASSET_CACHE.computeIfAbsent(CONTACT_ICON_ROOT + iconFile,
                        SidebarPortrait::readAssetBytes));
    }

    private static DocumentImageData portraitImage() {
        return DocumentImageData.fromBytes(
                ASSET_CACHE.computeIfAbsent(TEMPLATE_ASSET_ROOT + PORTRAIT_FILE,
                        SidebarPortrait::readAssetBytes));
    }

    private static byte[] readAssetBytes(String resourcePath) {
        try (InputStream input = SidebarPortrait.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing sidebar portrait asset: " + resourcePath);
            }
            return input.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read sidebar portrait asset: " + resourcePath, e);
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

    private record ExperienceEntry(String position, String subtitle, String description) {
    }
}
