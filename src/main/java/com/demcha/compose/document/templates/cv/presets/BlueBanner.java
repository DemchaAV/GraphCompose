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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Templates v2 "Blue Banner" CV preset.
 *
 * <p>Conventional Word-style resume with each module title rendered as
 * a soft blue full-width banner sandwiched between thin dark-blue
 * rules. Body content stays on the white page; work entries lay out as
 * UPPERCASE bold position + right-aligned bold date with a regular
 * {@code Company, Location} subtitle and an optional description.
 * Visual signature ported from the legacy
 * {@code BlueBannerCvTemplateComposer}: PT Serif headline, Lato body,
 * BANNER_BG (#7092BE), BANNER_RULE (#3A5276).</p>
 *
 * <p>Inline markdown ({@code **bold**}, {@code *italic*}) is parsed
 * through the shared {@link MarkdownText} helper.</p>
 *
 * @deprecated Superseded by the layered <code>…v2…</code> surface (the current
 *             standard). Kept for backward compatibility; scheduled for removal
 *             in a future major. See {@code docs/templates/v2-layered/} and
 *             {@link com.demcha.compose.document.templates.cv.v2.presets.BlueBanner}.
 */
@Deprecated(since = "1.7.0", forRemoval = true)
public final class BlueBanner {

    /** Stable template identifier. */
    public static final String ID = "blue-banner";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Blue Banner";

    /** Recommended page margin (in points). */
    public static final double RECOMMENDED_MARGIN = 28.0;

    private static final double BANNER_RULE_WIDTH = 500.0;
    private static final double BANNER_RULE_HORIZONTAL_INSET = 18.0;

    private static final DocumentColor INK = DocumentColor.rgb(20, 25, 35);
    private static final DocumentColor SOFT = DocumentColor.rgb(85, 85, 85);
    private static final DocumentColor BANNER_BG = DocumentColor.rgb(112, 146, 190);
    private static final DocumentColor BANNER_RULE = DocumentColor.rgb(58, 82, 118);
    private static final DocumentColor BANNER_TEXT = DocumentColor.rgb(22, 32, 48);

    private static final FontName HEADLINE_FONT = FontName.PT_SERIF;
    private static final FontName BODY_FONT = FontName.LATO;

    private static final List<String> SUMMARY_KEYS = List.of("summary", "professional summary", "profile");
    private static final List<String> EXPERIENCE_KEYS = List.of("experience", "employment", "work");
    private static final List<String> EDUCATION_KEYS = List.of("education", "certifications");
    private static final List<String> SKILL_KEYS = List.of("technical skills", "skills");
    private static final List<String> ADDITIONAL_KEYS = List.of("additional information", "additional");

    private BlueBanner() {
        // utility class — not instantiable
    }

    /**
     * Builds the {@code Blue Banner} template.
     *
     * @param theme active business theme; the preset overrides palette
     *              and typography to V1 BlueBanner tokens
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new BlueBannerTemplate();
    }

    private static final class BlueBannerTemplate implements DocumentTemplate<CvSpec> {

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
                    .name("BlueBannerRoot")
                    .spacing(4)
                    .addSection("BlueBannerHeader",
                            section -> addHeadline(section, spec.header()))
                    .addSection("BlueBannerContact",
                            section -> addContactLine(section, spec.header()));

            List<CvModule> modules = orderedModules(spec);
            for (int i = 0; i < modules.size(); i++) {
                final CvModule module = modules.get(i);
                final int index = i;
                addBannerRule(pageFlow, "BlueBannerRuleTop_" + index, 3, 1);
                pageFlow.addSection(
                        "BlueBannerBanner_" + index,
                        section -> addSectionBanner(section, safe(module.title())));
                addBannerRule(pageFlow, "BlueBannerRuleBottom_" + index, 1, 1);
                pageFlow.addSection(
                        "BlueBannerBody_" + index,
                        section -> addModuleBody(section, module));
            }

            pageFlow.build();
        }

        private void addBannerRule(PageFlowBuilder pageFlow, String name,
                                   double topMargin, double bottomMargin) {
            pageFlow.addLine(line -> line
                    .name(name)
                    .horizontal(BANNER_RULE_WIDTH)
                    .color(BANNER_RULE)
                    .thickness(0.55)
                    .margin(new DocumentInsets(
                            topMargin,
                            BANNER_RULE_HORIZONTAL_INSET,
                            bottomMargin,
                            BANNER_RULE_HORIZONTAL_INSET)));
        }

        private void addHeadline(SectionBuilder section, CvHeader header) {
            section.spacing(0)
                    .padding(new DocumentInsets(8, 0, 8, 0))
                    .addParagraph(paragraph -> paragraph
                            .text(spacedUpper(name(header)))
                            .textStyle(style(HEADLINE_FONT, 20.0,
                                    DocumentTextDecoration.DEFAULT, INK))
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.zero()));
        }

        private void addContactLine(SectionBuilder section, CvHeader header) {
            List<ContactPart> parts = contactParts(header);
            if (parts.isEmpty()) {
                return;
            }
            DocumentTextStyle textStyle = style(BODY_FONT, 7.5,
                    DocumentTextDecoration.DEFAULT, INK);
            DocumentTextStyle separatorStyle = style(BODY_FONT, 7.5,
                    DocumentTextDecoration.DEFAULT, SOFT);

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
            section.fillColor(BANNER_BG)
                    .padding(new DocumentInsets(3.2, 0, 3.2, 0))
                    .margin(DocumentInsets.zero())
                    .addParagraph(paragraph -> paragraph
                            .text(spacedUpper(title))
                            .textStyle(style(BODY_FONT, 7.3,
                                    DocumentTextDecoration.BOLD, BANNER_TEXT))
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.zero()));
        }

        private void addModuleBody(SectionBuilder section, CvModule module) {
            section.spacing(3)
                    .padding(new DocumentInsets(3, 4, 0, 4));
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
                renderBulletList(section, b.items());
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
            DocumentTextStyle base = style(BODY_FONT, 7.7,
                    DocumentTextDecoration.DEFAULT, INK);
            section.addParagraph(paragraph -> paragraph
                    .textStyle(base)
                    .lineSpacing(1.3)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(1.2))
                    .rich(rich -> appendMarkdown(rich, text, base)));
        }

        private void renderBulletList(SectionBuilder section, List<String> items) {
            List<String> cleaned = items.stream()
                    .filter(item -> item != null && !item.isBlank())
                    .map(BlueBanner::stripBasicMarkdown)
                    .toList();
            if (cleaned.isEmpty()) {
                return;
            }
            section.addList(list -> list
                    .items(cleaned)
                    .bullet()
                    .textStyle(style(BODY_FONT, 7.7,
                            DocumentTextDecoration.DEFAULT, INK))
                    .lineSpacing(1.25)
                    .itemSpacing(1.4)
                    .margin(DocumentInsets.top(1.2)));
        }

        private void renderKeyValueEntry(SectionBuilder section, KeyValueBlock.Entry entry) {
            DocumentTextStyle base = style(BODY_FONT, 7.7,
                    DocumentTextDecoration.DEFAULT, INK);
            DocumentTextStyle keyStyle = style(BODY_FONT, 7.7,
                    DocumentTextDecoration.BOLD, INK);
            section.addParagraph(paragraph -> paragraph
                    .textStyle(base)
                    .lineSpacing(1.3)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(1.2))
                    .rich(rich -> {
                        rich.style(safe(entry.key()) + ":", keyStyle);
                        rich.style(" ", base);
                        appendMarkdown(rich, safe(entry.value()), base);
                    }));
        }

        private void renderWorkEntry(SectionBuilder section, WorkEntry entry) {
            DocumentTextStyle positionStyle = style(BODY_FONT, 8.0,
                    DocumentTextDecoration.BOLD, INK);
            DocumentTextStyle dateStyle = style(BODY_FONT, 7.7,
                    DocumentTextDecoration.BOLD, INK);
            DocumentTextStyle subtitleStyle = style(BODY_FONT, 7.45,
                    DocumentTextDecoration.DEFAULT, INK);
            DocumentTextStyle bodyStyle = style(BODY_FONT, 7.6,
                    DocumentTextDecoration.DEFAULT, INK);

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
                        .textStyle(bodyStyle)
                        .lineSpacing(1.25)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(1.4))
                        .rich(rich -> appendMarkdown(rich,
                                entry.description(), bodyStyle)));
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

    private static List<CvModule> orderedModules(CvSpec spec) {
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

    private static void addIfPresent(List<CvModule> modules, CvModule module) {
        if (module != null && !modules.contains(module)) {
            modules.add(module);
        }
    }

    private static CvModule findModule(List<CvModule> modules, List<String> keys) {
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

    private record ContactPart(String text, DocumentLinkOptions linkOptions) {
    }

    private record WorkEntry(String position, String subtitle, String date, String description) {
    }
}
