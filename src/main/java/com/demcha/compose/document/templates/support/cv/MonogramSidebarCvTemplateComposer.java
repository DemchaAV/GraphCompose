package com.demcha.compose.document.templates.support.cv;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.SpacerNode;
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
 * Two-column resume composer with a pale neutral sidebar, a monogram
 * circle, centered contact icons, education and expertise blocks, plus a
 * large two-line letter-spaced headline and main career narrative on the
 * right.
 */
public final class MonogramSidebarCvTemplateComposer {
    private static final DocumentColor INK = DocumentColor.rgb(37, 45, 58);
    private static final DocumentColor SOFT = DocumentColor.rgb(112, 119, 125);
    private static final DocumentColor MUTED = DocumentColor.rgb(128, 135, 139);
    private static final DocumentColor SIDEBAR_BG = DocumentColor.rgb(226, 235, 235);
    private static final DocumentColor SIDEBAR_RULE = DocumentColor.rgb(138, 146, 148);
    private static final DocumentColor MAIN_RULE = DocumentColor.rgb(72, 79, 84);
    private static final DocumentColor ACCENT = DocumentColor.rgb(158, 146, 104);
    private static final DocumentColor MONOGRAM_RING = DocumentColor.rgb(54, 62, 74);
    private static final FontName HEADLINE_FONT = FontName.CRIMSON_TEXT;
    private static final FontName MONOGRAM_FONT = FontName.PT_SERIF;
    private static final FontName BODY_FONT = FontName.LATO;
    private static final String CONTACT_ICON_ROOT = "/templates/cv/timeline-minimal/icons/";
    private static final Map<String, byte[]> CONTACT_ICON_CACHE = new ConcurrentHashMap<>();
    private static final List<String> EDUCATION_KEYS = List.of("education", "certifications");
    private static final List<String> SKILL_KEYS = List.of("skills", "technical skills", "key skills", "expertise");
    private static final List<String> SUMMARY_KEYS = List.of("profile", "professional profile", "summary", "professional summary", "overview");
    private static final List<String> EXPERIENCE_KEYS = List.of("experience", "employment", "work", "professional experience");
    private static final int EDUCATION_LIMIT = 2;
    private static final int SKILL_LIMIT = 7;
    private static final int EXPERIENCE_LIMIT = 2;
    private static final int DESCRIPTION_MAX_CHARS = 220;
    private static final int PROFILE_MAX_CHARS = 320;
    private static final double MONOGRAM_DIAMETER = 98;

    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        CvDocumentSpec spec = Objects.requireNonNull(documentSpec, "documentSpec");

        // Resolve usable widths once so children can fill the sidebar band
        // and center their decorative rules / monogram horizontally.
        double pageInnerWidth = document.canvas().innerWidth();
        double sidebarOuterWidth = pageInnerWidth * 0.33;
        double sidebarHorizontalPadding = 13.0 * 2.0;
        double sidebarInnerWidth = Math.max(0.0, sidebarOuterWidth - sidebarHorizontalPadding);

        document.dsl()
                .pageFlow()
                .name("MonogramSidebarRoot")
                .spacing(0)
                .padding(DocumentInsets.zero())
                .addRow("MonogramSidebarFrame", row -> row
                        .spacing(0)
                        .weights(0.33, 0.67)
                        .addSection("MonogramSidebarSidebar",
                                section -> addSidebar(section, spec, sidebarInnerWidth))
                        .addSection("MonogramSidebarMain", section -> addMain(section, spec)))
                .build();
    }

    private void addSidebar(SectionBuilder section, CvDocumentSpec spec, double innerWidth) {
        // Generous bottom padding so the pale grey banner reaches the
        // bottom edge of an A4 page; the row child does not stretch on
        // its own.
        section.spacing(8)
                .padding(new DocumentInsets(18, 13, 225, 13))
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
    }

    private void addMonogramBlock(SectionBuilder section, String initials, double innerWidth) {
        // Real overlay: the engine allows LayerStackNode inside row slots, so
        // the monogram is an atomic stack — circle ring as the back layer and
        // initials centered on the front. An outer stack with a transparent
        // full-width spacer centers the badge horizontally inside the sidebar
        // band without ad-hoc margin math.
        LayerStackNode badge = new LayerStackBuilder()
                .name("MonogramBadge")
                .back(new EllipseBuilder()
                        .name("MonogramRing")
                        .size(MONOGRAM_DIAMETER, MONOGRAM_DIAMETER)
                        .stroke(DocumentStroke.of(MONOGRAM_RING, 1.25))
                        .build())
                .layer(new ParagraphBuilder()
                        .name("MonogramInitials")
                        .text(initials)
                        .textStyle(style(MONOGRAM_FONT, 22, DocumentTextDecoration.BOLD, MONOGRAM_RING))
                        // align LEFT so the paragraph's natural width is the
                        // glyph width — otherwise CENTER alignment expands the
                        // paragraph to the entire available constraint and the
                        // badge stack inherits that width, pulling the circle
                        // to the left and the initials away from the centre.
                        .align(TextAlign.LEFT)
                        .build(), LayerAlign.CENTER)
                .build();

        section.addLayerStack(outer -> outer
                .name("MonogramFrame")
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
                .textStyle(style(BODY_FONT, 8.0, DocumentTextDecoration.BOLD, INK))
                .align(TextAlign.CENTER)
                .lineSpacing(1.2)
                .margin(DocumentInsets.top(6)));
        section.addLine(line -> line
                .horizontal(innerWidth)
                .color(SIDEBAR_RULE)
                .thickness(0.45)
                .margin(new DocumentInsets(1, 0, 2, 0)));
    }

    private void addContactBlock(SectionBuilder section, Header header) {
        List<ContactLine> lines = contactLines(header);
        if (lines.isEmpty()) {
            return;
        }
        DocumentTextStyle textStyle = style(BODY_FONT, 7.4, DocumentTextDecoration.DEFAULT, SOFT);
        for (ContactLine contact : lines) {
            if (contact.iconFile() != null) {
                section.addParagraph(paragraph -> paragraph
                        .textStyle(textStyle)
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.top(4))
                        .rich(rich -> rich.image(
                                contactIcon(contact.iconFile()),
                                9.0,
                                9.0,
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
        DocumentTextStyle headingStyle = style(BODY_FONT, 7.6, DocumentTextDecoration.BOLD, INK);
        DocumentTextStyle subStyle = style(BODY_FONT, 7.4, DocumentTextDecoration.DEFAULT, INK);
        DocumentTextStyle metaStyle = style(BODY_FONT, 7.2, DocumentTextDecoration.DEFAULT, ACCENT);

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
        DocumentTextStyle skillStyle = style(BODY_FONT, 7.4, DocumentTextDecoration.DEFAULT, SOFT);
        List<String> items = moduleItems(module);
        for (String item : items.subList(0, Math.min(SKILL_LIMIT, items.size()))) {
            String text = firstClauseOf(item);
            if (text.isBlank()) {
                continue;
            }
            section.addParagraph(paragraph -> paragraph
                    .text(stripBasicMarkdown(text))
                    .textStyle(skillStyle)
                    .align(TextAlign.CENTER)
                    .lineSpacing(1.25)
                    .margin(DocumentInsets.top(1)));
        }
    }

    private void addMain(SectionBuilder section, CvDocumentSpec spec) {
        section.spacing(5)
                .padding(new DocumentInsets(12, 20, 24, 18));

        addNameBlock(section, spec.header());

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
    }

    private void addNameBlock(SectionBuilder section, Header header) {
        String[] parts = splitName(name(header));
        DocumentTextStyle nameStyle = style(HEADLINE_FONT, 30, DocumentTextDecoration.DEFAULT, INK);
        DocumentTextStyle titleStyle = style(BODY_FONT, 7.4, DocumentTextDecoration.BOLD, ACCENT);

        for (String part : parts) {
            section.addParagraph(paragraph -> paragraph
                    .text(spacedUpper(part))
                    .textStyle(nameStyle)
                    .align(TextAlign.CENTER)
                    .lineSpacing(1.0)
                    .margin(DocumentInsets.zero()));
        }
        section.addParagraph(paragraph -> paragraph
                .text(spacedUpper("Your Professional Title"))
                .textStyle(titleStyle)
                .align(TextAlign.CENTER)
                .margin(DocumentInsets.zero()));
    }

    private void addMainSectionHeader(SectionBuilder section, String title) {
        if (title == null || title.isBlank()) {
            return;
        }
        section.addParagraph(paragraph -> paragraph
                .text(spacedUpper(title))
                .textStyle(style(BODY_FONT, 9.0, DocumentTextDecoration.BOLD, INK))
                .align(TextAlign.LEFT)
                .margin(DocumentInsets.top(6)));
        section.addLine(line -> line
                .horizontal(355)
                .color(MAIN_RULE)
                .thickness(0.55)
                .margin(new DocumentInsets(1, 0, 4, 0)));
    }

    private void addProfileBody(SectionBuilder section, CvModule module) {
        DocumentTextStyle bodyStyle = style(BODY_FONT, 7.5, DocumentTextDecoration.DEFAULT, INK);
        for (CvModule.BodyBlock block : module.bodyBlocks()) {
            if (block.kind() == CvModule.BodyKind.PARAGRAPH) {
                String text = safe(block.text()).trim();
                if (text.isBlank()) {
                    continue;
                }
                section.addParagraph(paragraph -> paragraph
                        .text(excerpt(stripBasicMarkdown(text), PROFILE_MAX_CHARS))
                        .textStyle(bodyStyle)
                        .lineSpacing(1.35)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(4)));
            } else if (block.kind() == CvModule.BodyKind.LIST) {
                List<String> items = block.items() == null ? List.of() : block.items();
                String joined = String.join(" ", items).trim();
                if (joined.isBlank()) {
                    continue;
                }
                section.addParagraph(paragraph -> paragraph
                        .text(excerpt(stripBasicMarkdown(joined), PROFILE_MAX_CHARS))
                        .textStyle(bodyStyle)
                        .lineSpacing(1.35)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(4)));
            }
        }
    }

    private void addExperienceEntries(SectionBuilder section, CvModule module) {
        DocumentTextStyle positionStyle = style(BODY_FONT, 7.8, DocumentTextDecoration.BOLD, INK);
        DocumentTextStyle dateStyle = style(BODY_FONT, 7.4, DocumentTextDecoration.BOLD, ACCENT);
        DocumentTextStyle bodyStyle = style(BODY_FONT, 7.4, DocumentTextDecoration.DEFAULT, INK);

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
                        .text(excerpt(entry.description(), DESCRIPTION_MAX_CHARS))
                        .textStyle(bodyStyle)
                        .lineSpacing(1.35)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(1)));
            }
        }
    }

    private CvModule findModule(CvDocumentSpec spec, List<String> keys) {
        if (spec.modules() == null) {
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
        int dashIdx = afterPipe.indexOf(" - ");
        if (dashIdx > 0) {
            date = afterPipe.substring(0, dashIdx).trim();
        } else {
            date = afterPipe;
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
        return new EducationEntry(heading, subtitle, looksLikeDate(date) ? date : "");
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

        // Use heading as position; "Company, Location" details fold into
        // the heading in the original CV mock — we keep the full heading
        // so it reads as "JOB POSITION HERE | COMPANY NAME" style.
        StringBuilder position = new StringBuilder(heading);
        return new ExperienceEntry(position.toString(), date, description);
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
        return List.copyOf(lines);
    }

    private void addContactLine(List<ContactLine> lines, String iconFile, String text, DocumentLinkOptions linkOptions) {
        if (text != null && !text.isBlank()) {
            lines.add(new ContactLine(iconFile, text, linkOptions));
        }
    }

    private DocumentImageData contactIcon(String iconFile) {
        return DocumentImageData.fromBytes(CONTACT_ICON_CACHE.computeIfAbsent(iconFile,
                MonogramSidebarCvTemplateComposer::readIconBytes));
    }

    private static byte[] readIconBytes(String iconFile) {
        try (InputStream input = MonogramSidebarCvTemplateComposer.class.getResourceAsStream(CONTACT_ICON_ROOT + iconFile)) {
            if (input == null) {
                throw new IllegalStateException("Missing monogram sidebar contact icon: " + iconFile);
            }
            return input.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read monogram sidebar contact icon: " + iconFile, e);
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

    private record ExperienceEntry(String position, String date, String description) {
    }

    private static String name(Header header) {
        return header == null ? "" : safe(header.getName());
    }

    private static String[] splitName(String fullName) {
        String safeName = safe(fullName).trim();
        if (safeName.isEmpty()) {
            return new String[]{""};
        }
        int space = safeName.indexOf(' ');
        if (space < 0) {
            return new String[]{safeName};
        }
        return new String[]{
                safeName.substring(0, space).trim(),
                safeName.substring(space + 1).trim()};
    }

    private static String initials(Header header) {
        String[] parts = splitName(name(header));
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                if (builder.length() > 0) {
                    builder.append(" | ");
                }
                builder.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return builder.length() == 0 ? "·" : builder.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String stripBasicMarkdown(String value) {
        return safe(value)
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace("*", "")
                .replace("_", "");
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
