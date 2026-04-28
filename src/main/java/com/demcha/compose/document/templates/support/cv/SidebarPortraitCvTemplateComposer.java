package com.demcha.compose.document.templates.support.cv;

import com.demcha.compose.document.api.DocumentSession;
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
    private static final DocumentColor SIDEBAR_BG = DocumentColor.rgb(244, 244, 244);
    private static final DocumentColor SIDEBAR_INK = INK;
    private static final DocumentColor SIDEBAR_SOFT = SOFT;
    private static final DocumentColor SIDEBAR_RULE = DocumentColor.rgb(200, 200, 200);
    private static final DocumentColor ACCENT = DocumentColor.rgb(150, 150, 150);
    private static final DocumentColor RULE = DocumentColor.rgb(190, 190, 190);
    private static final DocumentColor PHOTO_RING = DocumentColor.rgb(210, 210, 210);
    private static final DocumentColor PHOTO_FILL = DocumentColor.rgb(225, 225, 225);
    private static final FontName HEADLINE_FONT = FontName.CRIMSON_TEXT;
    private static final FontName SUBHEAD_FONT = FontName.PT_SERIF;
    private static final FontName BODY_FONT = FontName.LATO;
    private static final String CONTACT_ICON_ROOT = "/templates/cv/timeline-minimal/icons/";
    private static final Map<String, byte[]> CONTACT_ICON_CACHE = new ConcurrentHashMap<>();
    private static final List<String> EDUCATION_KEYS = List.of("education", "certifications");
    private static final List<String> SKILL_KEYS = List.of("skills", "technical skills", "key skills");
    private static final List<String> LANGUAGE_KEYS = List.of("languages", "language");
    private static final List<String> SUMMARY_KEYS = List.of("profile", "professional profile", "summary", "professional summary", "overview");
    private static final List<String> EXPERIENCE_KEYS = List.of("experience", "employment", "work", "professional experience");
    private static final int EDUCATION_LIMIT = 2;
    private static final int SKILL_LIMIT = 5;
    private static final int LANGUAGE_LIMIT = 3;
    private static final int EXPERIENCE_LIMIT = 2;
    private static final int DESCRIPTION_MAX_CHARS = 200;
    private static final int PROFILE_MAX_CHARS = 320;

    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        CvDocumentSpec spec = Objects.requireNonNull(documentSpec, "documentSpec");

        document.dsl()
                .pageFlow()
                .name("SidebarPortraitRoot")
                .spacing(0)
                .padding(DocumentInsets.zero())
                .addRow("SidebarPortraitHeaderRow", row -> row
                        .gap(0)
                        .weights(0.36, 0.64)
                        .fillColor(SIDEBAR_BG)
                        .addSection("SidebarPortraitHeaderPhoto",
                                section -> addPhotoBlock(section))
                        .addSection("SidebarPortraitHeaderName",
                                section -> addNameBlock(section, spec.header())))
                .addRow("SidebarPortraitBodyRow", row -> row
                        .gap(0)
                        .weights(0.36, 0.64)
                        .addSection("SidebarPortraitBodySidebar",
                                section -> addSidebar(section, spec))
                        .addSection("SidebarPortraitBodyMain",
                                section -> addMain(section, spec)))
                .build();
    }

    private void addSidebar(SectionBuilder section, CvDocumentSpec spec) {
        // The bottom padding is intentionally generous — paginators do not
        // stretch a row child to the page bottom, so we pad the sidebar
        // until its grey fill reaches the lower edge. The number is sized
        // for an A4 page with a typical ~110pt grey banner header above.
        section.spacing(8)
                .padding(new DocumentInsets(16, 14, 240, 14))
                .fillColor(SIDEBAR_BG);

        addContactBlock(section, spec.header());

        CvModule education = findModule(spec, EDUCATION_KEYS);
        if (education != null) {
            addSidebarHeader(section, education.title());
            addEducationEntries(section, education);
        }

        CvModule skills = findModule(spec, SKILL_KEYS);
        if (skills != null) {
            addSidebarHeader(section, skills.title());
            addSkillsList(section, skills);
        }

        CvModule languages = findModule(spec, LANGUAGE_KEYS);
        if (languages != null) {
            addSidebarHeader(section, languages.title());
            addLanguageList(section, languages);
        }
    }

    private void addPhotoBlock(SectionBuilder section) {
        // Photo lives inside the grey banner row alongside the name. The
        // CvDocumentSpec does not carry a portrait image, so the template
        // ships a soft grey circle that callers can overlay or swap.
        section.padding(new DocumentInsets(20, 0, 20, 22))
                .addCircle(72, circle -> circle
                        .name("SidebarPortraitPhoto")
                        .stroke(DocumentStroke.of(PHOTO_RING, 0.8))
                        .fillColor(PHOTO_FILL)
                        .margin(DocumentInsets.zero()));
    }

    private void addContactBlock(SectionBuilder section, Header header) {
        List<ContactLine> lines = contactLines(header);
        if (lines.isEmpty()) {
            return;
        }
        DocumentTextStyle style = style(BODY_FONT, 8.4, DocumentTextDecoration.DEFAULT, SIDEBAR_INK);
        for (ContactLine contact : lines) {
            section.addParagraph(paragraph -> paragraph
                    .textStyle(style)
                    .align(TextAlign.LEFT)
                    .lineSpacing(1.2)
                    .margin(DocumentInsets.zero())
                    .link(contact.linkOptions())
                    .rich(rich -> {
                        if (contact.iconFile() != null) {
                            rich.image(contactIcon(contact.iconFile()), 9.0, 9.0,
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
                .horizontal(40)
                .color(ACCENT)
                .thickness(1.0)
                .margin(new DocumentInsets(10, 0, 4, 0)));
        section.addParagraph(paragraph -> paragraph
                .text(title.toUpperCase(Locale.ROOT))
                .textStyle(style(SUBHEAD_FONT, 12.5, DocumentTextDecoration.BOLD, SIDEBAR_INK))
                .align(TextAlign.LEFT)
                .margin(DocumentInsets.zero()));
    }

    private void addEducationEntries(SectionBuilder section, CvModule module) {
        List<String> items = moduleItems(module);
        for (String item : items.subList(0, Math.min(EDUCATION_LIMIT, items.size()))) {
            EducationEntry entry = parseEducationEntry(item);
            DocumentTextStyle headingStyle = style(BODY_FONT, 8.6, DocumentTextDecoration.BOLD, SIDEBAR_INK);
            DocumentTextStyle metaStyle = style(BODY_FONT, 8.2, DocumentTextDecoration.DEFAULT, SIDEBAR_SOFT);

            section.addParagraph(paragraph -> paragraph
                    .text(stripBasicMarkdown(entry.heading()).toUpperCase(Locale.ROOT))
                    .textStyle(headingStyle)
                    .align(TextAlign.LEFT)
                    .lineSpacing(1.2)
                    .margin(DocumentInsets.top(4)));
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
        DocumentTextStyle skillStyle = style(BODY_FONT, 8.4, DocumentTextDecoration.DEFAULT, SIDEBAR_INK);
        List<String> items = moduleItems(module);
        for (String item : items.subList(0, Math.min(SKILL_LIMIT, items.size()))) {
            String text = firstClauseOf(item);
            if (text.isBlank()) {
                continue;
            }
            section.addParagraph(paragraph -> paragraph
                    .text(stripBasicMarkdown(text))
                    .textStyle(skillStyle)
                    .lineSpacing(1.3)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(2)));
        }
    }

    private void addLanguageList(SectionBuilder section, CvModule module) {
        DocumentTextStyle nameStyle = style(BODY_FONT, 8.6, DocumentTextDecoration.BOLD, SIDEBAR_INK);
        DocumentTextStyle metaStyle = style(BODY_FONT, 8.4, DocumentTextDecoration.DEFAULT, SIDEBAR_SOFT);
        List<String> items = moduleItems(module);
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
                    .margin(DocumentInsets.top(2))
                    .rich(rich -> {
                        rich.style(name.toUpperCase(Locale.ROOT), nameStyle);
                        if (!level.isBlank()) {
                            rich.style("  | " + level, metaStyle);
                        }
                    }));
        }
    }

    private void addMain(SectionBuilder section, CvDocumentSpec spec) {
        section.spacing(10)
                .padding(new DocumentInsets(20, 22, 24, 22));

        CvModule profile = findModule(spec, SUMMARY_KEYS);
        if (profile != null) {
            addMainSectionHeader(section, profile.title());
            addProfileBody(section, profile);
        }

        CvModule experience = findModule(spec, EXPERIENCE_KEYS);
        if (experience != null) {
            addMainSectionHeader(section, experience.title());
            addExperienceEntries(section, experience);
        }

        // Generic modules (projects, additional info) intentionally fall
        // outside this fixed-row layout: the page is sized for headline +
        // profile + experience, and overflowing the row would surface as
        // an "atomic node too large" error since side-bar rows do not
        // paginate. Callers wanting projects in this template should fold
        // them into the experience module ahead of time.
    }

    private void addNameBlock(SectionBuilder section, Header header) {
        String name = name(header);
        section.spacing(6)
                .padding(new DocumentInsets(34, 22, 20, 22))
                .addParagraph(paragraph -> paragraph
                        .text(name)
                        .textStyle(style(HEADLINE_FONT, 34, DocumentTextDecoration.DEFAULT, SIDEBAR_INK))
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.zero()))
                .addLine(line -> line
                        .horizontal(80)
                        .color(ACCENT)
                        .thickness(1.2)
                        .margin(new DocumentInsets(2, 0, 2, 0)))
                .addParagraph(paragraph -> paragraph
                        .text(spacedUpper("Your Professional Title Goes Here"))
                        .textStyle(style(BODY_FONT, 9.0, DocumentTextDecoration.BOLD, ACCENT))
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.zero()));
    }

    private void addMainSectionHeader(SectionBuilder section, String title) {
        if (title == null || title.isBlank()) {
            return;
        }
        section.addParagraph(paragraph -> paragraph
                .text(title.toUpperCase(Locale.ROOT))
                .textStyle(style(SUBHEAD_FONT, 13, DocumentTextDecoration.BOLD, INK))
                .align(TextAlign.LEFT)
                .margin(DocumentInsets.top(8)));
        section.addLine(line -> line
                .horizontal(48)
                .color(ACCENT)
                .thickness(1.2)
                .margin(new DocumentInsets(2, 0, 2, 0)));
    }

    private void addProfileBody(SectionBuilder section, CvModule module) {
        DocumentTextStyle bodyStyle = style(BODY_FONT, 8.8, DocumentTextDecoration.DEFAULT, INK);
        for (CvModule.BodyBlock block : module.bodyBlocks()) {
            if (block.kind() == CvModule.BodyKind.PARAGRAPH) {
                String text = safe(block.text()).trim();
                if (text.isBlank()) {
                    continue;
                }
                section.addParagraph(paragraph -> paragraph
                        .text(excerpt(text, PROFILE_MAX_CHARS))
                        .textStyle(bodyStyle)
                        .lineSpacing(1.5)
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
                        .lineSpacing(1.5)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(2)));
            }
        }
    }

    private void addExperienceEntries(SectionBuilder section, CvModule module) {
        DocumentTextStyle positionStyle = style(BODY_FONT, 9.4, DocumentTextDecoration.BOLD, INK);
        DocumentTextStyle subtitleStyle = style(BODY_FONT, 8.4, DocumentTextDecoration.DEFAULT, MUTED);
        DocumentTextStyle bodyStyle = style(BODY_FONT, 8.6, DocumentTextDecoration.DEFAULT, INK);

        List<String> items = moduleItems(module);
        for (String item : items.subList(0, Math.min(EXPERIENCE_LIMIT, items.size()))) {
            ExperienceEntry entry = parseExperienceEntry(item);
            section.addParagraph(paragraph -> paragraph
                    .text(stripBasicMarkdown(entry.position()).toUpperCase(Locale.ROOT))
                    .textStyle(positionStyle)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(6)));
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
                        .lineSpacing(1.4)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(2)));
            }
        }
    }

    private void renderGenericModule(SectionBuilder section, CvModule module) {
        DocumentTextStyle bodyStyle = style(BODY_FONT, 8.6, DocumentTextDecoration.DEFAULT, INK);
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
}
