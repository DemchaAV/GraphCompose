package com.demcha.compose.document.templates.support.cv;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.data.common.EmailYaml;
import com.demcha.compose.document.templates.data.common.Header;
import com.demcha.compose.document.templates.data.common.LinkYml;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.data.cv.CvModule;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.font.FontName;

import java.awt.Color;
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
 * Two-column resume composer with a portrait sidebar on the left and the
 * main career narrative on the right.
 *
 * <p>The sidebar carries a circular photo placeholder, contact items with
 * inline icons (reused from the timeline-minimal template), and short
 * sections for education, skills, and languages. The main column hosts the
 * large serif name, the professional profile paragraph, and the experience
 * timeline of bold position + subtitle + description + bullets.</p>
 */
public final class SidebarPortraitCvTemplateComposer {
    // Body palette — used on the white main column.
    private static final DocumentColor INK = DocumentColor.rgb(34, 34, 34);
    private static final DocumentColor SOFT = DocumentColor.rgb(85, 85, 85);
    private static final DocumentColor MUTED = DocumentColor.rgb(120, 120, 120);
    // Light sidebar palette — soft grey banner that bleeds to the page
    // edges, dark text on the grey column, and discreet hairline rules
    // shared with the main section headers.
    private static final DocumentColor SIDEBAR_BG = DocumentColor.rgb(241, 240, 237);
    private static final DocumentColor SIDEBAR_INK = INK;
    private static final DocumentColor SIDEBAR_SOFT = SOFT;
    private static final DocumentColor SIDEBAR_RULE = DocumentColor.rgb(200, 200, 200);
    private static final DocumentColor ACCENT = DocumentColor.rgb(106, 106, 106);
    private static final DocumentColor RULE = DocumentColor.rgb(178, 178, 178);
    private static final DocumentColor PHOTO_RING = DocumentColor.rgb(208, 206, 202);
    private static final DocumentColor PHOTO_FILL = DocumentColor.rgb(218, 216, 211);
    private static final FontName DISPLAY_FONT = FontName.CRIMSON_TEXT;
    private static final double SIDEBAR_INNER_WIDTH = 156.4;
    private static final double PHOTO_DIAMETER = 98.0;
    private static final double HERO_TOP_OFFSET = 54.0;
    private static final String CONTACT_ICON_ROOT = "/templates/cv/sidebar-portrait/icons/";
    private static final Map<String, byte[]> CONTACT_ICON_CACHE = new ConcurrentHashMap<>();
    private static final List<String> EDUCATION_KEYS = List.of("education", "certifications");
    private static final List<String> SKILL_KEYS = List.of("skills", "technical skills", "key skills");
    private static final List<String> LANGUAGE_KEYS = List.of("languages", "language", "additional information", "additional");
    private static final List<String> SUMMARY_KEYS = List.of("profile", "professional profile", "summary", "professional summary", "overview");
    private static final List<String> EXPERIENCE_KEYS = List.of("experience", "employment", "work", "professional experience");
    private static final int EDUCATION_LIMIT = 2;
    private static final int SKILL_LIMIT = 5;
    private static final int LANGUAGE_LIMIT = 3;
    private static final int EXPERIENCE_LIMIT = 2;
    private static final int DESCRIPTION_MAX_CHARS = 260;
    private static final int PROFILE_MAX_CHARS = 430;

    private final CvTheme theme;

    public SidebarPortraitCvTemplateComposer() {
        this(defaultTheme());
    }

    /**
     * Constructs the composer with a custom {@link CvTheme}. The
     * sidebar background, photo ring, and serif headline font stay
     * template-owned; body type follows the theme.
     *
     * @param theme CV theme driving body type and accent colour
     */
    public SidebarPortraitCvTemplateComposer(CvTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        CvDocumentSpec spec = Objects.requireNonNull(documentSpec, "documentSpec");

        document.dsl()
                .pageFlow()
                .name("SidebarPortraitRoot")
                .spacing(0)
                .padding(DocumentInsets.zero())
                .addRow("SidebarPortraitBodyRow", row -> row
                        .spacing(0)
                        .weights(0.34, 0.66)
                        .addSection("SidebarPortraitBodySidebar",
                                section -> addSidebar(section, spec))
                        .addSection("SidebarPortraitBodyMain",
                                section -> {
                                    section.spacing(0)
                                            .padding(DocumentInsets.zero());
                                    addNameBlock(section, spec.header());
                                    addMain(section, spec);
                                }))
                .build();
    }

    private void addSidebar(SectionBuilder section, CvDocumentSpec spec) {
        section.spacing(9)
                .padding(new DocumentInsets(72, 20, 22.4, 26))
                .fillColor(SIDEBAR_BG);

        addPhotoBlock(section, spec.header());
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
    }

    private void addPhotoBlock(SectionBuilder section, Header header) {
        // CvDocumentSpec does not carry a portrait image yet; keep the
        // reference-inspired circular slot with candidate initials.
        double sideInset = Math.max(0.0, (SIDEBAR_INNER_WIDTH - PHOTO_DIAMETER) / 2.0);
        section.addCircle(PHOTO_DIAMETER, PHOTO_FILL, circle -> circle
                        .name("SidebarPortraitPhoto")
                        .stroke(DocumentStroke.of(PHOTO_RING, 0.8))
                        .margin(new DocumentInsets(0, sideInset, 17, sideInset))
                        .center(new ParagraphBuilder()
                                .name("SidebarPortraitPhotoInitials")
                                .text(initials(name(header)))
                                .textStyle(style(templateFont(), 24, DocumentTextDecoration.BOLD, ACCENT))
                                .align(TextAlign.LEFT)
                                .build()));
    }

    private void addContactBlock(SectionBuilder section, Header header) {
        List<ContactLine> lines = contactLines(header);
        if (lines.isEmpty()) {
            return;
        }
        DocumentTextStyle style = style(templateFont(), 8.3, DocumentTextDecoration.DEFAULT, SIDEBAR_INK);
        for (ContactLine contact : lines) {
            section.addParagraph(paragraph -> paragraph
                    .textStyle(style)
                    .align(TextAlign.LEFT)
                    .lineSpacing(1.35)
                    .margin(DocumentInsets.top(3))
                    .link(contact.linkOptions())
                    .rich(rich -> {
                        if (contact.iconFile() != null) {
                            rich.image(contactIcon(contact.iconFile()), 11.0, 11.0,
                                    InlineImageAlignment.CENTER, 0.0, contact.linkOptions());
                            rich.style("  ", style);
                        }
                        if (contact.linkOptions() != null) {
                            rich.link(contact.text(), contact.linkOptions());
                        } else {
                            rich.style(contact.text(), style);
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
                .textStyle(style(templateFont(), 10.8, DocumentTextDecoration.BOLD, SIDEBAR_INK))
                .align(TextAlign.LEFT)
                .margin(DocumentInsets.zero()));
    }

    private void addEducationEntries(SectionBuilder section, CvModule module) {
        List<String> items = moduleItems(module);
        for (String item : items.subList(0, Math.min(EDUCATION_LIMIT, items.size()))) {
            EducationEntry entry = parseEducationEntry(item);
            DocumentTextStyle headingStyle = style(templateFont(), 8.4, DocumentTextDecoration.BOLD, SIDEBAR_INK);
            DocumentTextStyle metaStyle = style(templateFont(), 7.8, DocumentTextDecoration.DEFAULT, SIDEBAR_SOFT);

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
        DocumentTextStyle skillStyle = style(templateFont(), 8.0, DocumentTextDecoration.DEFAULT, SIDEBAR_INK);
        List<String> items = moduleItems(module);
        for (String item : items.subList(0, Math.min(SKILL_LIMIT, items.size()))) {
            String text = firstClauseOf(item);
            if (text.isBlank()) {
                continue;
            }
            section.addParagraph(paragraph -> paragraph
                    .text(stripBasicMarkdown(text))
                    .textStyle(skillStyle)
                    .lineSpacing(1.35)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(3)));
        }
    }

    private void addLanguageList(SectionBuilder section, CvModule module) {
        DocumentTextStyle nameStyle = style(templateFont(), 8.1, DocumentTextDecoration.BOLD, SIDEBAR_INK);
        DocumentTextStyle metaStyle = style(templateFont(), 7.9, DocumentTextDecoration.DEFAULT, SIDEBAR_SOFT);
        List<String> items = languageItems(module);
        for (String item : items.subList(0, Math.min(LANGUAGE_LIMIT, items.size()))) {
            String text = stripBasicMarkdown(item);
            int pipe = text.indexOf('|');
            String name = pipe > 0 ? text.substring(0, pipe).trim() : text.trim();
            String level = pipe > 0 ? text.substring(pipe + 1).trim() : "";
            if (name.isBlank()) {
                continue;
            }
            section.addParagraph(paragraph -> paragraph
                    .textStyle(nameStyle)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(4))
                    .rich(rich -> {
                        rich.style(name.toUpperCase(Locale.ROOT), nameStyle);
                        if (!level.isBlank()) {
                            rich.style("  | " + level, metaStyle);
                        }
                    }));
        }
    }

    private void addMain(SectionBuilder section, CvDocumentSpec spec) {
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

            // Keep the row atomic and one-page: projects/additional modules can
            // be folded into experience by callers that need them here.
        });
    }

    private void addNameBlock(SectionBuilder section, Header header) {
        String name = name(header);
        section.addSection("SidebarPortraitHero", hero -> hero
                .fillColor(SIDEBAR_BG)
                .padding(new DocumentInsets(19, 34, 15, 34))
                .spacing(5)
                .margin(DocumentInsets.top(HERO_TOP_OFFSET))
                .addParagraph(paragraph -> paragraph
                        .text(name)
                        .textStyle(style(DISPLAY_FONT, 38, DocumentTextDecoration.BOLD, SIDEBAR_INK))
                        .align(TextAlign.CENTER)
                        .lineSpacing(1.0)
                        .margin(DocumentInsets.zero()))
                .addParagraph(paragraph -> paragraph
                        .text(spacedUpper("Your Professional Title Goes Here"))
                        .textStyle(style(templateFont(), 8.4, DocumentTextDecoration.DEFAULT, INK))
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero())));
    }

    private void addMainSectionHeader(SectionBuilder section, String title) {
        if (title == null || title.isBlank()) {
            return;
        }
        section.addParagraph(paragraph -> paragraph
                .text(spacedUpper(title))
                .textStyle(style(templateFont(), 12.0, DocumentTextDecoration.BOLD, INK))
                .align(TextAlign.LEFT)
                .margin(DocumentInsets.top(8)));
        section.addLine(line -> line
                .horizontal(346)
                .color(RULE)
                .thickness(0.55)
                .margin(new DocumentInsets(2, 0, 7, 0)));
    }

    private void addProfileBody(SectionBuilder section, CvModule module) {
        DocumentTextStyle bodyStyle = style(templateFont(), 9.4, DocumentTextDecoration.DEFAULT, INK);
        for (CvModule.BodyBlock block : module.bodyBlocks()) {
            if (block.kind() == CvModule.BodyKind.PARAGRAPH) {
                String text = safe(block.text()).trim();
                if (text.isBlank()) {
                    continue;
                }
                section.addParagraph(paragraph -> paragraph
                        .text(excerpt(text, PROFILE_MAX_CHARS))
                        .textStyle(bodyStyle)
                        .lineSpacing(1.35)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(2)));
            } else if (block.kind() == CvModule.BodyKind.LIST) {
                List<String> items = block.items() == null ? List.of() : block.items();
                String joined = String.join(" ", items).trim();
                if (joined.isBlank()) {
                    continue;
                }
                section.addParagraph(paragraph -> paragraph
                        .text(excerpt(joined, PROFILE_MAX_CHARS))
                        .textStyle(bodyStyle)
                        .lineSpacing(1.35)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(2)));
            }
        }
    }

    private void addExperienceEntries(SectionBuilder section, CvModule module) {
        DocumentTextStyle positionStyle = style(templateFont(), 10.0, DocumentTextDecoration.BOLD, INK);
        DocumentTextStyle subtitleStyle = style(templateFont(), 9.2, DocumentTextDecoration.DEFAULT, INK);
        DocumentTextStyle bodyStyle = style(templateFont(), 9.0, DocumentTextDecoration.DEFAULT, INK);

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
                        .text(excerpt(entry.description(), DESCRIPTION_MAX_CHARS))
                        .textStyle(bodyStyle)
                        .lineSpacing(1.35)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(2)));
            }
        }
    }

    private void renderGenericModule(SectionBuilder section, CvModule module) {
        DocumentTextStyle bodyStyle = style(theme.bodyFont(), 8.6, DocumentTextDecoration.DEFAULT, INK);
        for (CvModule.BodyBlock block : module.bodyBlocks()) {
            if (block.kind() == CvModule.BodyKind.PARAGRAPH) {
                String text = safe(block.text()).trim();
                if (!text.isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(text)
                            .textStyle(bodyStyle)
                            .lineSpacing(1.4)
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.top(2)));
                }
            } else if (block.kind() == CvModule.BodyKind.LIST) {
                List<String> items = block.items() == null ? List.of() : block.items();
                for (String item : items) {
                    if (item == null || item.isBlank()) {
                        continue;
                    }
                    section.addParagraph(paragraph -> paragraph
                            .text(item)
                            .textStyle(bodyStyle)
                            .lineSpacing(1.4)
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.top(2)));
                }
            }
        }
    }

    private boolean isCategorized(CvModule module) {
        return matchesAny(module, EDUCATION_KEYS)
                || matchesAny(module, SKILL_KEYS)
                || matchesAny(module, LANGUAGE_KEYS)
                || matchesAny(module, SUMMARY_KEYS)
                || matchesAny(module, EXPERIENCE_KEYS);
    }

    private CvModule findModule(CvDocumentSpec spec, List<String> keys) {
        if (spec.modules() == null) {
            return null;
        }
        for (CvModule module : spec.modules()) {
            if (matchesAny(module, keys)) {
                return module;
            }
        }
        return null;
    }

    private boolean matchesAny(CvModule module, List<String> keys) {
        if (module == null) {
            return false;
        }
        String haystack = normalize(safe(module.title()) + " " + safe(module.name()));
        for (String key : keys) {
            if (haystack.contains(normalize(key))) {
                return true;
            }
        }
        return false;
    }

    private List<String> moduleItems(CvModule module) {
        List<String> result = new ArrayList<>();
        if (module == null) {
            return result;
        }
        for (CvModule.BodyBlock block : module.bodyBlocks()) {
            if (block.kind() == CvModule.BodyKind.LIST) {
                List<String> items = block.items() == null ? List.of() : block.items();
                for (String item : items) {
                    if (item != null && !item.isBlank()) {
                        result.add(item);
                    }
                }
            } else if (block.kind() == CvModule.BodyKind.PARAGRAPH) {
                String text = safe(block.text()).trim();
                if (!text.isBlank()) {
                    result.add(text);
                }
            }
        }
        return result;
    }

    private List<String> languageItems(CvModule module) {
        List<String> result = new ArrayList<>();
        for (String item : moduleItems(module)) {
            String text = stripBasicMarkdown(item).trim();
            if (text.toLowerCase(Locale.ROOT).startsWith("languages:")) {
                String languages = text.substring("languages:".length()).trim();
                for (String part : languages.split(",")) {
                    if (!part.isBlank()) {
                        result.add(part.trim());
                    }
                }
            } else if (text.contains("|")) {
                result.add(text);
            }
        }
        return result;
    }

    private EducationEntry parseEducationEntry(String item) {
        String text = safe(item).trim();
        int pipe = text.indexOf('|');
        String heading = pipe > 0 ? text.substring(0, pipe).trim() : text;
        String afterPipe = pipe > 0 ? text.substring(pipe + 1).trim() : "";

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
            // Fallback for items shaped as "Title – Subtitle" without a pipe.
            String subtitle = "";
            for (String separator : new String[]{" – ", " — ", " - "}) {
                int idx = heading.indexOf(separator);
                if (idx > 0) {
                    subtitle = heading.substring(idx + separator.length()).trim();
                    heading = heading.substring(0, idx).trim();
                    break;
                }
            }
            return new EducationEntry(heading, subtitle, "");
        }

        String subtitle = "";
        for (String separator : new String[]{" – ", " — ", " - "}) {
            int idx = heading.indexOf(separator);
            if (idx > 0) {
                subtitle = heading.substring(idx + separator.length()).trim();
                heading = heading.substring(0, idx).trim();
                break;
            }
        }
        if (!description.isBlank()) {
            subtitle = subtitle.isBlank() ? description : (subtitle + ", " + description);
        }
        return new EducationEntry(heading, subtitle, date);
    }

    private ExperienceEntry parseExperienceEntry(String item) {
        String text = safe(item).trim();
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

        // Position bold: keep heading up to first separator; everything else
        // falls into the subtitle row beneath the position.
        String position = heading;
        String details = "";
        for (String separator : new String[]{" – ", " — ", " - "}) {
            int idx = heading.indexOf(separator);
            if (idx > 0) {
                position = heading.substring(0, idx).trim();
                details = heading.substring(idx + separator.length()).trim();
                break;
            }
        }
        if (details.isBlank()) {
            int comma = heading.indexOf(", ");
            if (comma > 0) {
                position = heading.substring(0, comma).trim();
                details = heading.substring(comma + 2).trim();
            }
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

    private static String firstClauseOf(String item) {
        String text = safe(item);
        int colon = text.indexOf(':');
        if (colon > 0 && colon < text.length() - 1) {
            return text.substring(0, colon).trim();
        }
        int comma = text.indexOf(',');
        if (comma > 0 && comma < 60) {
            return text.substring(0, comma).trim();
        }
        return text.trim();
    }

    private List<ContactLine> contactLines(Header header) {
        if (header == null) {
            return List.of();
        }
        List<ContactLine> lines = new ArrayList<>();
        addContactLine(lines, "phone.png", safe(header.getPhoneNumber()), null);
        if (header.getEmail() != null) {
            addContactLine(lines, "email.png", emailDisplay(header.getEmail()), emailLink(header.getEmail()));
        }
        addContactLine(lines, "location.png", safe(header.getAddress()), null);
        if (header.getLinkedIn() != null) {
            addContactLine(lines, "linkedin.png", linkDisplay(header.getLinkedIn()), linkOptions(header.getLinkedIn()));
        }
        if (header.getGitHub() != null) {
            addContactLine(lines, "github.png", linkDisplay(header.getGitHub()), linkOptions(header.getGitHub()));
        }
        return List.copyOf(lines);
    }

    private void addContactLine(List<ContactLine> lines, String iconFile, String text, DocumentLinkOptions linkOptions) {
        if (text != null && !text.isBlank()) {
            lines.add(new ContactLine(iconFile, text, linkOptions));
        }
    }

    private DocumentImageData contactIcon(String iconFile) {
        return DocumentImageData.fromBytes(CONTACT_ICON_CACHE.computeIfAbsent(iconFile,
                SidebarPortraitCvTemplateComposer::readIconBytes));
    }

    private static byte[] readIconBytes(String iconFile) {
        try (InputStream input = SidebarPortraitCvTemplateComposer.class.getResourceAsStream(CONTACT_ICON_ROOT + iconFile)) {
            if (input == null) {
                throw new IllegalStateException("Missing sidebar portrait contact icon: " + iconFile);
            }
            return input.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read sidebar portrait contact icon: " + iconFile, e);
        }
    }

    private DocumentLinkOptions emailLink(EmailYaml email) {
        String to = safe(email.getTo());
        return to.isBlank() ? null : new DocumentLinkOptions("mailto:" + to);
    }

    private DocumentLinkOptions linkOptions(LinkYml link) {
        if (link.getLinkUrl() == null || !link.getLinkUrl().isValid()) {
            return null;
        }
        return new DocumentLinkOptions(link.getLinkUrl().getUrl());
    }

    private String emailDisplay(EmailYaml email) {
        String displayText = safe(email.getDisplayText());
        return displayText.isBlank() ? safe(email.getTo()) : displayText;
    }

    private String linkDisplay(LinkYml link) {
        String displayText = safe(link.getDisplayText());
        if (!displayText.isBlank()) {
            return displayText;
        }
        return link.getLinkUrl() == null ? "" : safe(link.getLinkUrl().getUrl());
    }

    private record ContactLine(String iconFile, String text, DocumentLinkOptions linkOptions) {
    }

    private record EducationEntry(String heading, String subtitle, String date) {
    }

    private record ExperienceEntry(String position, String subtitle, String description) {
    }

    private static String name(Header header) {
        return header == null ? "" : safe(header.getName());
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String excerpt(String value, int maxChars) {
        String clean = safe(value).replaceAll("\\s+", " ").trim();
        if (clean.length() <= maxChars) {
            return clean;
        }
        int boundary = clean.lastIndexOf(' ', maxChars - 1);
        int end = boundary > maxChars / 2 ? boundary : maxChars - 1;
        return clean.substring(0, end).trim() + "…";
    }

    private static String stripBasicMarkdown(String value) {
        return safe(value)
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace("*", "")
                .replace("_", "");
    }

    private static String initials(String value) {
        String clean = safe(value).trim();
        if (clean.isBlank()) {
            return "";
        }
        String[] parts = clean.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank() && builder.length() < 2) {
                builder.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return builder.toString();
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
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isLetterOrDigit(current)) {
                builder.append(Character.toLowerCase(current));
            }
        }
        return builder.toString();
    }

    private DocumentTextStyle style(FontName font, double size, DocumentTextDecoration decoration, DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(font)
                .size(size)
                .decoration(decoration)
                .color(color)
                .build();
    }

    private FontName templateFont() {
        return theme.bodyFont();
    }

    private static CvTheme defaultTheme() {
        return new CvTheme(
                new Color(34, 34, 34),
                new Color(150, 150, 150),
                new Color(34, 34, 34),
                new Color(150, 150, 150),
                FontName.LATO,
                FontName.LATO,
                28,
                10.0,
                8.4,
                4,
                Margin.top(2),
                0);
    }
}
