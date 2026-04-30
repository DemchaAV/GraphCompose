package com.demcha.compose.document.templates.support.cv;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Conventional "blue banner" resume composer modernised under the v1.5
 * cinematic stack: a {@link CvTheme}-driven body type, every accent strip
 * declared via {@code accentTop} / {@code accentBottom}, and each section
 * banner emitted as a single {@code softPanel}-backed section instead of
 * the original {@code addLine + addSection + addLine} triplet.
 *
 * <p>Mirrors the popular Word resume look where every module title sits
 * inside a soft blue strip. Body content stays on the white page; work
 * entries lay out as bold position + right-aligned date with a regular
 * {@code Company, Location} subtitle and an optional description.</p>
 *
 * <p>Pass any {@link CvTheme} (including
 * {@link CvTheme#fromBusinessTheme(com.demcha.compose.document.theme.BusinessTheme)})
 * to re-skin the body text without touching the banner identity.</p>
 *
 * @author Artem Demchyshyn
 */
public final class BlueBannerCvTemplateComposer {
    private static final DocumentColor BANNER_BG = DocumentColor.rgb(196, 216, 234);
    private static final DocumentColor BANNER_RULE = DocumentColor.rgb(60, 70, 90);
    private static final DocumentColor FRAME_RULE = DocumentColor.rgb(34, 34, 34);
    private static final FontName HEADLINE_FONT = FontName.PT_SERIF;
    private static final List<String> SUMMARY_KEYS = List.of("summary", "professional summary", "profile");
    private static final List<String> EXPERIENCE_KEYS = List.of("experience", "employment", "work");
    private static final List<String> EDUCATION_KEYS = List.of("education", "certifications");
    private static final List<String> SKILL_KEYS = List.of("technical skills", "skills");
    private static final List<String> ADDITIONAL_KEYS = List.of("additional information", "additional");

    private final CvTheme theme;

    /**
     * Default constructor — uses the conventional dark-grey ink + Lato
     * body theme that the original Word-style template shipped with.
     */
    public BlueBannerCvTemplateComposer() {
        this(defaultTheme());
    }

    /**
     * Constructs the composer with a custom {@link CvTheme}. Body
     * paragraphs, contact line, work descriptions, and link styling
     * follow the supplied theme; the blue section banner and the
     * dark-ink frame around the headline stay as the template's
     * visual identity regardless of theme.
     *
     * @param theme CV theme driving body type and accent colour
     */
    public BlueBannerCvTemplateComposer(CvTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        CvDocumentSpec spec = Objects.requireNonNull(documentSpec, "documentSpec");

        PageFlowBuilder pageFlow = document.dsl()
                .pageFlow()
                .name("BlueBannerRoot")
                .spacing(4)
                .addSection("BlueBannerHeader", section -> addHeadline(section, spec.header()))
                // Contact line is wrapped by hairline rules above and below the
                // pipe-delimited contact text. accentTop/Bottom replaces the
                // original addLine + addSection + addLine triplet so the rules
                // travel with the section across page breaks.
                .addSection("BlueBannerContact", section -> {
                    section.accentTop(FRAME_RULE, 0.7);
                    section.accentBottom(FRAME_RULE, 0.7);
                    addContactLine(section, spec.header());
                });

        List<CvModule> modules = orderedModules(spec);
        for (int i = 0; i < modules.size(); i++) {
            final CvModule module = modules.get(i);
            final int index = i;
            // Each section is a softPanel-style banner with thin top/bottom
            // accent rules. Single section instead of the original
            // addLine + addSection + addLine triple keeps z-order stable
            // across pagination.
            pageFlow.addSection(
                    "BlueBannerBanner_" + index,
                    section -> {
                        section.accentTop(BANNER_RULE, 0.7);
                        section.accentBottom(BANNER_RULE, 0.7);
                        addSectionBanner(section, safe(module.title()));
                    });
            pageFlow.addSection(
                    "BlueBannerBody_" + index,
                    section -> addModuleBody(section, module));
        }

        pageFlow.build();
    }

    private void addHeadline(SectionBuilder section, Header header) {
        section.spacing(0)
                .padding(new DocumentInsets(8, 0, 8, 0))
                .addParagraph(paragraph -> paragraph
                        .text(spacedUpper(name(header)))
                        .textStyle(style(HEADLINE_FONT, 20, DocumentTextDecoration.DEFAULT, ink()))
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()));
    }

    private void addContactLine(SectionBuilder section, Header header) {
        List<ContactPart> parts = contactParts(header);
        if (parts.isEmpty()) {
            return;
        }
        DocumentTextStyle textStyle = style(theme.bodyFont(), 7.5, DocumentTextDecoration.DEFAULT, ink());
        DocumentTextStyle separatorStyle = style(theme.bodyFont(), 7.5, DocumentTextDecoration.DEFAULT, soft());

        section.spacing(0)
                .padding(new DocumentInsets(1.5, 0, 1.5, 0))
                .addParagraph(paragraph -> paragraph
                        .textStyle(textStyle)
                        .align(TextAlign.CENTER)
                        .lineSpacing(1.3)
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
                                    rich.style("  |  ", separatorStyle);
                                }
                            }
                        }));
    }

    private void addSectionBanner(SectionBuilder section, String title) {
        if (title == null || title.isBlank()) {
            return;
        }
        // softPanel paints fill + uniform padding around the centred title;
        // the accentTop/Bottom rules above the section close the banner.
        section.softPanel(BANNER_BG, 0.0, 3.0)
                .margin(DocumentInsets.zero())
                .addParagraph(paragraph -> paragraph
                        .text(spacedUpper(title))
                        .textStyle(style(theme.bodyFont(), 7.6, DocumentTextDecoration.BOLD, ink()))
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()));
    }

    private void addModuleBody(SectionBuilder section, CvModule module) {
        section.spacing(3)
                .padding(new DocumentInsets(3, 4, 0, 4));
        for (CvModule.BodyBlock block : module.bodyBlocks()) {
            switch (block.kind()) {
                case PARAGRAPH -> renderParagraphBlock(section, block);
                case LIST -> renderListBlock(section, block);
                default -> {
                    // Banner template focuses on paragraphs and bullet lists.
                }
            }
        }
    }

    private void renderParagraphBlock(SectionBuilder section, CvModule.BodyBlock block) {
        String text = safe(block.text()).trim();
        if (text.isBlank()) {
            return;
        }
        section.addParagraph(paragraph -> paragraph
                .text(text)
                .textStyle(style(theme.bodyFont(), 7.7, DocumentTextDecoration.DEFAULT, ink()))
                .lineSpacing(1.3)
                .align(TextAlign.LEFT)
                .margin(DocumentInsets.top(1.2)));
    }

    private void renderListBlock(SectionBuilder section, CvModule.BodyBlock block) {
        List<String> items = block.items() == null ? List.of() : block.items();
        if (items.isEmpty()) {
            return;
        }
        for (String rawItem : items) {
            String item = safe(rawItem).trim();
            if (item.isBlank()) {
                continue;
            }
            WorkEntry entry = parseWorkEntry(item);
            if (entry != null) {
                renderWorkEntry(section, entry);
            } else {
                renderItemAsParagraph(section, item);
            }
        }
    }

    private void renderItemAsParagraph(SectionBuilder section, String item) {
        section.addParagraph(paragraph -> paragraph
                .text(item)
                .textStyle(style(theme.bodyFont(), 7.7, DocumentTextDecoration.DEFAULT, ink()))
                .lineSpacing(1.25)
                .align(TextAlign.LEFT)
                .margin(DocumentInsets.top(1.4)));
    }

    private void renderWorkEntry(SectionBuilder section, WorkEntry entry) {
        DocumentTextStyle positionStyle = style(theme.bodyFont(), 8.0, DocumentTextDecoration.BOLD, ink());
        DocumentTextStyle dateStyle = style(theme.bodyFont(), 7.7, DocumentTextDecoration.BOLD, ink());
        DocumentTextStyle subtitleStyle = style(theme.bodyFont(), 7.45, DocumentTextDecoration.DEFAULT, ink());
        DocumentTextStyle bodyStyle = style(theme.bodyFont(), 7.6, DocumentTextDecoration.DEFAULT, ink());

        // Two-column header — RowBuilder with weights is the canonical
        // v1.5 idiom for "label | metadata" pairs (RowBuilder rejects
        // nested rows + tables, so addSection is the canonical column).
        section.addRow("BlueBannerEntryHeader", row -> row
                .spacing(8)
                .weights(1.0, 0.4)
                .addSection("Title", titleColumn -> titleColumn
                        .padding(DocumentInsets.zero())
                        .addParagraph(paragraph -> paragraph
                                .text(stripBasicMarkdown(entry.position()).toUpperCase(Locale.ROOT))
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

        if (!entry.subtitle().isBlank()) {
            section.addParagraph(paragraph -> paragraph
                    .text(stripBasicMarkdown(entry.subtitle()))
                    .textStyle(subtitleStyle)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.zero()));
        }

        if (!entry.description().isBlank()) {
            section.addParagraph(paragraph -> paragraph
                    .text(entry.description())
                    .textStyle(bodyStyle)
                    .lineSpacing(1.25)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(1.4)));
        }
    }

    private List<CvModule> orderedModules(CvDocumentSpec spec) {
        List<CvModule> modules = spec.modules() == null ? List.of() : spec.modules();
        List<CvModule> ordered = new ArrayList<>();
        addIfPresent(ordered, findModule(modules, SUMMARY_KEYS));
        addIfPresent(ordered, findModule(modules, EXPERIENCE_KEYS));
        addIfPresent(ordered, findModule(modules, EDUCATION_KEYS));
        addIfPresent(ordered, findModule(modules, SKILL_KEYS));
        addIfPresent(ordered, findModule(modules, ADDITIONAL_KEYS));
        for (CvModule module : modules) {
            addIfPresent(ordered, module);
        }
        return List.copyOf(ordered);
    }

    private void addIfPresent(List<CvModule> modules, CvModule module) {
        if (module != null && !modules.contains(module)) {
            modules.add(module);
        }
    }

    private CvModule findModule(List<CvModule> modules, List<String> keys) {
        for (CvModule module : modules) {
            String normalized = normalize(safe(module.name()) + " " + safe(module.title()));
            for (String key : keys) {
                if (normalized.contains(normalize(key))) {
                    return module;
                }
            }
        }
        return null;
    }

    private WorkEntry parseWorkEntry(String item) {
        int pipeIndex = item.indexOf('|');
        if (pipeIndex < 0) {
            return null;
        }
        String headingText = item.substring(0, pipeIndex).trim();
        String afterPipe = item.substring(pipeIndex + 1).trim();
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

        String position = headingText;
        String subtitle = "";
        for (String separator : new String[]{" – ", " — ", " - "}) {
            int idx = headingText.indexOf(separator);
            if (idx > 0) {
                position = headingText.substring(0, idx).trim();
                subtitle = headingText.substring(idx + separator.length()).trim();
                break;
            }
        }
        if (subtitle.isBlank()) {
            int comma = headingText.indexOf(", ");
            if (comma > 0) {
                position = headingText.substring(0, comma).trim();
                subtitle = headingText.substring(comma + 2).trim();
            }
        }
        return new WorkEntry(position, subtitle, date, description);
    }

    private static boolean looksLikeDate(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.matches(".*(\\d{4}|present|current|now|ongoing).*")
                && (lower.contains("-") || lower.contains("–") || lower.contains("—") || lower.contains("to"));
    }

    private List<ContactPart> contactParts(Header header) {
        if (header == null) {
            return List.of();
        }
        List<ContactPart> parts = new ArrayList<>();
        addPart(parts, safe(header.getPhoneNumber()), null);
        if (header.getEmail() != null) {
            addPart(parts, emailDisplay(header.getEmail()), emailLink(header.getEmail()));
        }
        addPart(parts, safe(header.getAddress()), null);
        if (header.getLinkedIn() != null) {
            addPart(parts, linkDisplay(header.getLinkedIn()), linkOptions(header.getLinkedIn()));
        }
        if (header.getGitHub() != null) {
            addPart(parts, linkDisplay(header.getGitHub()), linkOptions(header.getGitHub()));
        }
        return List.copyOf(parts);
    }

    private void addPart(List<ContactPart> parts, String text, DocumentLinkOptions linkOptions) {
        if (text != null && !text.isBlank()) {
            parts.add(new ContactPart(text, linkOptions));
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

    /**
     * Returns the body / ink colour from the theme, falling back to the
     * conventional dark-grey if the theme defines a brighter body colour
     * (helps keep contrast on the blue banner area).
     */
    private DocumentColor ink() {
        return DocumentColor.of(theme.bodyColor());
    }

    private DocumentColor soft() {
        // Slightly muted variant of the body colour for separators and
        // italic subtitles. We re-derive from the theme so a swap of the
        // BusinessTheme palette propagates automatically.
        Color base = theme.bodyColor();
        int r = clamp(base.getRed() + 51);
        int g = clamp(base.getGreen() + 51);
        int b = clamp(base.getBlue() + 51);
        return DocumentColor.rgb(r, g, b);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /**
     * Default theme matching the original hand-tuned palette: dark-grey
     * ink (rgb 34,34,34), Lato body, Royal-Blue accent.
     */
    private static CvTheme defaultTheme() {
        return new CvTheme(
                new Color(34, 34, 34),
                new Color(34, 34, 34),
                new Color(34, 34, 34),
                new Color(40, 90, 200),
                FontName.PT_SERIF,
                FontName.LATO,
                22,
                9.5,
                8.6,
                4,
                Margin.top(2),
                0);
    }

    private record ContactPart(String text, DocumentLinkOptions linkOptions) {
    }

    private record WorkEntry(String position, String subtitle, String date, String description) {
    }

    private static String name(Header header) {
        return header == null ? "" : safe(header.getName());
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

    private static String normalize(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < safe(value).length(); i++) {
            char current = Character.toLowerCase(safe(value).charAt(i));
            if (Character.isLetterOrDigit(current)) {
                builder.append(current);
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

    private DocumentTextStyle style(FontName font, double size, DocumentTextDecoration decoration, DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(font)
                .size(size)
                .decoration(decoration)
                .color(color)
                .build();
    }
}
