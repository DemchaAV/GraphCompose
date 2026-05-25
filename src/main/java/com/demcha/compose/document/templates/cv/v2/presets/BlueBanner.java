package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.components.MarkdownInline;
import com.demcha.compose.document.templates.cv.v2.components.ParagraphRenderer;
import com.demcha.compose.document.templates.cv.v2.components.RowRenderer;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.data.CvEntry;
import com.demcha.compose.document.templates.cv.v2.data.CvRow;
import com.demcha.compose.document.templates.cv.v2.data.CvSection;
import com.demcha.compose.document.templates.cv.v2.data.EntriesSection;
import com.demcha.compose.document.templates.cv.v2.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.v2.data.RowStyle;
import com.demcha.compose.document.templates.cv.v2.data.RowsSection;
import com.demcha.compose.document.templates.cv.v2.data.Slot;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.ContactLine;
import com.demcha.compose.document.templates.cv.v2.widgets.Headline;
import com.demcha.compose.document.templates.cv.v2.widgets.SectionHeader;
import com.demcha.compose.font.FontName;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * v2 port of the legacy "Blue Banner" CV preset.
 *
 * <p>Visual signature: centred PT-Serif spaced-caps name, compact
 * Lato contact row, full-width blue section banners sandwiched
 * between thin dark-blue rules, and dense white body blocks.</p>
 *
 * <p>Most of the preset is ordinary widget composition. The body
 * renderer is deliberately preset-local because Blue Banner differs
 * from the shared {@code EntryRenderer}: entry titles are uppercase,
 * dates are bold, subtitles are regular ink, and project rows render
 * without bullets.</p>
 */
public final class BlueBanner {

    /** Stable template identifier. */
    public static final String ID = "blue-banner";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Blue Banner";

    /** Recommended page margin (in points). */
    public static final double RECOMMENDED_MARGIN = 28.0;

    private static final double BANNER_RULE_WIDTH = 500.0;
    private static final double BANNER_RULE_HORIZONTAL_INSET = 18.0;

    private static final DocumentColor BANNER_TEXT =
            DocumentColor.rgb(22, 32, 48);

    private static final List<String> SUMMARY_KEYS =
            List.of("summary", "professional summary", "profile");
    private static final List<String> EXPERIENCE_KEYS =
            List.of("experience", "professional experience", "employment", "work");
    private static final List<String> EDUCATION_KEYS =
            List.of("education", "certifications");
    private static final List<String> SKILL_KEYS =
            List.of("technical skills", "skills");
    private static final List<String> ADDITIONAL_KEYS =
            List.of("additional information", "additional");

    private BlueBanner() {
    }

    /**
     * Builds the preset with the Blue Banner theme.
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.blueBanner());
    }

    /**
     * Builds the preset with a caller-supplied theme. The caller can
     * adjust palette, typography, spacing, or separator tokens without
     * changing the page-flow composition.
     */
    public static DocumentTemplate<CvDocument> create(CvTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new Template(theme);
    }

    private static final class Template implements DocumentTemplate<CvDocument> {

        private final CvTheme theme;

        Template(CvTheme theme) {
            this.theme = theme;
        }

        @Override
        public String id() {
            return ID;
        }

        @Override
        public String displayName() {
            return DISPLAY_NAME;
        }

        @Override
        public void compose(DocumentSession document, CvDocument doc) {
            Objects.requireNonNull(document, "document");
            Objects.requireNonNull(doc, "doc");

            DocumentTextStyle bannerTitleStyle = DocumentTextStyle.builder()
                    .fontName(theme.typography().bodyFont())
                    .size(theme.typography().sizeBanner())
                    .decoration(DocumentTextDecoration.BOLD)
                    .color(BANNER_TEXT)
                    .build();

            PageFlowBuilder pageFlow = document.dsl()
                    .pageFlow()
                    .name("CvV2BlueBannerRoot")
                    .spacing(theme.spacing().pageFlowSpacing())
                    .addSection("Header", section ->
                            Headline.spacedCentered(section,
                                    doc.identity().name(), theme))
                    .addSection("Contact", section ->
                            ContactLine.centered(section, doc.identity(), theme));

            List<CvSection> sections = orderedSections(doc);
            for (int i = 0; i < sections.size(); i++) {
                final CvSection sec = sections.get(i);
                final int idx = i;
                addBannerRule(pageFlow, "BlueBannerRuleTop_" + idx, theme, 3, 1);
                pageFlow.addSection("BlueBannerTitle_" + idx, host ->
                        SectionHeader.fullWidthBanner(host, sec.title(),
                                theme, bannerTitleStyle));
                addBannerRule(pageFlow, "BlueBannerRuleBottom_" + idx, theme, 1, 1);
                pageFlow.addSection("BlueBannerBody_" + idx, host ->
                        renderBody(host, sec, theme));
            }

            pageFlow.build();
        }

        private static void addBannerRule(PageFlowBuilder pageFlow, String name,
                                          CvTheme theme,
                                          double topMargin,
                                          double bottomMargin) {
            pageFlow.addLine(line -> line
                    .name(name)
                    .horizontal(BANNER_RULE_WIDTH)
                    .color(theme.palette().rule())
                    .thickness(theme.spacing().accentRuleWidth())
                    .margin(new DocumentInsets(
                            topMargin,
                            BANNER_RULE_HORIZONTAL_INSET,
                            bottomMargin,
                            BANNER_RULE_HORIZONTAL_INSET)));
        }
    }

    private static void renderBody(SectionBuilder host,
                                   CvSection section,
                                   CvTheme theme) {
        host.spacing(theme.spacing().sectionBodySpacing())
                .padding(theme.spacing().sectionBodyPadding());

        if (section instanceof ParagraphSection p) {
            ParagraphRenderer.render(host, p.body(), theme);
        } else if (section instanceof RowsSection r) {
            renderRows(host, r, theme);
        } else if (section instanceof EntriesSection e) {
            for (CvEntry entry : e.entries()) {
                renderEntry(host, entry, theme);
            }
        } else {
            throw new IllegalStateException(
                    "Unknown CvSection subtype: " + section.getClass().getName());
        }
    }

    private static void renderRows(SectionBuilder host,
                                   RowsSection section,
                                   CvTheme theme) {
        if (section.style() == RowStyle.BULLETED_STACKED) {
            for (CvRow row : section.rows()) {
                renderPlainProjectRow(host, row, theme);
            }
            return;
        }
        for (CvRow row : section.rows()) {
            RowRenderer.render(host, row, section.style(), theme);
        }
    }

    private static void renderPlainProjectRow(SectionBuilder host,
                                              CvRow row,
                                              CvTheme theme) {
        String label = row.label().trim();
        String body = row.body().trim();
        DocumentTextStyle labelStyle = theme.entryTitleStyle();
        DocumentTextStyle bodyStyle = theme.bodyStyle();

        host.addParagraph(p -> p
                .textStyle(bodyStyle)
                .lineSpacing(theme.typography().bodyLineSpacing())
                .align(TextAlign.LEFT)
                .margin(DocumentInsets.top((float) theme.spacing().paragraphMarginTop()))
                .rich(rich -> {
                    rich.style(stripBasicMarkdown(label), labelStyle);
                    if (!body.isBlank()) {
                        rich.style(" - ", bodyStyle);
                        MarkdownInline.append(rich, body, bodyStyle);
                    }
                }));
    }

    private static void renderEntry(SectionBuilder section,
                                    CvEntry entry,
                                    CvTheme theme) {
        DocumentTextStyle titleStyle = theme.entryTitleStyle();
        DocumentTextStyle dateStyle = style(theme.typography().bodyFont(),
                theme.typography().sizeEntryDate(),
                DocumentTextDecoration.BOLD,
                theme.palette().ink());
        DocumentTextStyle subtitleStyle = style(theme.typography().bodyFont(),
                theme.typography().sizeEntrySubtitle(),
                DocumentTextDecoration.DEFAULT,
                theme.palette().ink());
        DocumentTextStyle bodyStyle = theme.bodyStyle();

        section.addRow("BlueBannerEntryHeader", row -> row
                .spacing(theme.spacing().entryHeaderRowSpacing())
                .weights(theme.spacing().entryTitleWeight(),
                        theme.spacing().entryDateWeight())
                .addSection("Title", titleColumn -> titleColumn
                        .padding(DocumentInsets.zero())
                        .addParagraph(p -> p
                                .text(stripBasicMarkdown(entry.title())
                                        .toUpperCase(Locale.ROOT))
                                .textStyle(titleStyle)
                                .align(TextAlign.LEFT)
                                .margin(DocumentInsets.zero())))
                .addSection("Date", dateColumn -> dateColumn
                        .padding(DocumentInsets.zero())
                        .addParagraph(p -> p
                                .text(stripBasicMarkdown(entry.date()))
                                .textStyle(dateStyle)
                                .align(TextAlign.RIGHT)
                                .margin(DocumentInsets.zero()))));

        if (!entry.subtitle().isBlank()) {
            section.addParagraph(p -> p
                    .text(stripBasicMarkdown(entry.subtitle()))
                    .textStyle(subtitleStyle)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.zero()));
        }

        if (!entry.body().isBlank()) {
            renderBodyParagraph(section, entry.body(), bodyStyle,
                    theme.typography().bodyLineSpacing(),
                    DocumentInsets.top((float) theme.spacing().paragraphMarginTop()));
        }
    }

    private static void renderBodyParagraph(SectionBuilder host,
                                            String text,
                                            DocumentTextStyle style,
                                            double lineSpacing,
                                            DocumentInsets margin) {
        if (text == null || text.isBlank()) {
            return;
        }
        host.addParagraph(p -> p
                .textStyle(style)
                .lineSpacing(lineSpacing)
                .align(TextAlign.LEFT)
                .margin(margin)
                .rich(rich -> MarkdownInline.append(rich, text.trim(), style)));
    }

    private static DocumentTextStyle style(FontName font,
                                           double size,
                                           DocumentTextDecoration decoration,
                                           DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(font)
                .size(size)
                .decoration(decoration)
                .color(color)
                .build();
    }

    private static List<CvSection> orderedSections(CvDocument doc) {
        List<CvSection> sections = doc.sectionsIn(Slot.MAIN);
        List<CvSection> ordered = new ArrayList<>();
        addIfPresent(ordered, findSection(sections, SUMMARY_KEYS));
        addIfPresent(ordered, findSection(sections, EXPERIENCE_KEYS));
        addIfPresent(ordered, findSection(sections, EDUCATION_KEYS));
        addIfPresent(ordered, findSection(sections, SKILL_KEYS));
        addIfPresent(ordered, findSection(sections, ADDITIONAL_KEYS));
        for (CvSection section : sections) {
            addIfPresent(ordered, section);
        }
        return List.copyOf(ordered);
    }

    private static void addIfPresent(List<CvSection> sections, CvSection section) {
        if (section != null && !sections.contains(section)) {
            sections.add(section);
        }
    }

    private static CvSection findSection(List<CvSection> sections,
                                         List<String> keys) {
        for (CvSection section : sections) {
            String normalizedTitle = normalize(section.title());
            for (String key : keys) {
                if (normalizedTitle.contains(normalize(key))) {
                    return section;
                }
            }
        }
        return null;
    }

    private static String stripBasicMarkdown(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace("*", "")
                .replace("_", "");
    }

    private static String normalize(String value) {
        String safe = value == null ? "" : value;
        StringBuilder builder = new StringBuilder(safe.length());
        for (int i = 0; i < safe.length(); i++) {
            char current = Character.toLowerCase(safe.charAt(i));
            if (Character.isLetterOrDigit(current)) {
                builder.append(current);
            }
        }
        return builder.toString();
    }
}
