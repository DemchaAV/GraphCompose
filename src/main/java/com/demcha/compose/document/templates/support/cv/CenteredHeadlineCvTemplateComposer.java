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
 * Classic single-column resume composer with a centered headline, separator
 * rules, a pipe-delimited contact line, and section blocks made of bold work
 * entries plus body paragraphs.
 *
 * <p>Each module is rendered as: an uppercase bold header, then either a
 * paragraph block or a list of items where every item is parsed
 * heuristically — entries shaped like {@code <heading> | <date> - <description>}
 * become two-column rows (bold company plus thin location on the left,
 * regular date on the right) followed by an optional description paragraph.
 * Thin full-width rules are drawn explicitly instead of as section borders so
 * the template matches the centered reference and avoids short underlines
 * under module titles.</p>
 */
public final class CenteredHeadlineCvTemplateComposer {
    private static final DocumentColor INK = DocumentColor.rgb(54, 54, 54);
    private static final DocumentColor HEADLINE = DocumentColor.rgb(70, 70, 70);
    private static final DocumentColor SOFT = DocumentColor.rgb(105, 105, 105);
    private static final DocumentColor RULE = DocumentColor.rgb(188, 188, 188);
    private static final FontName HEADLINE_FONT = FontName.POPPINS;

    private final CvTheme theme;

    public CenteredHeadlineCvTemplateComposer() {
        this(defaultTheme());
    }

    /**
     * Constructs the composer with a custom {@link CvTheme}. Body and
     * contact text follow the theme; the headline font and the
     * separator rule colour stay template-owned.
     *
     * @param theme CV theme driving body type and accent colour
     */
    public CenteredHeadlineCvTemplateComposer(CvTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        CvDocumentSpec spec = Objects.requireNonNull(documentSpec, "documentSpec");
        double ruleWidth = document.canvas().innerWidth();

        PageFlowBuilder pageFlow = document.dsl()
                .pageFlow()
                .name("CenteredHeadlineRoot")
                .spacing(0)
                .addSection("CenteredHeadlineHeader", section -> addHeadline(section, spec.header()))
                .addLine(line -> referenceRule(line, "CenteredHeadlineHeaderRule", ruleWidth, 7, 0))
                .addSection("CenteredHeadlineContact", section -> addContactLine(section, spec.header()))
                .addLine(line -> referenceRule(line, "CenteredHeadlineContactRule", ruleWidth, 0, 13));

        List<CvModule> modules = spec.modules() == null ? List.of() : spec.modules();
        for (int i = 0; i < modules.size(); i++) {
            final CvModule module = modules.get(i);
            final int index = i;
            String title = safe(module.title());
            if (title.isBlank()) {
                continue;
            }
            pageFlow.addSection(
                    "CenteredHeadlineModule" + normalize(title) + "_" + index,
                    section -> {
                        addModuleTitle(section, title);
                        renderBody(section, module);
                    });
            if (index < modules.size() - 1) {
                pageFlow.addLine(line -> referenceRule(line,
                        "CenteredHeadlineModuleRule" + index,
                        ruleWidth,
                        5,
                        12));
            }
        }

        pageFlow.build();
    }

    private void addModuleTitle(SectionBuilder section, String title) {
        section.addParagraph(paragraph -> paragraph
                .text(spacedUpper(title))
                .textStyle(style(theme.bodyFont(), 9.5, DocumentTextDecoration.BOLD, SOFT))
                .align(TextAlign.LEFT)
                .margin(DocumentInsets.zero()));
    }

    private void referenceRule(com.demcha.compose.document.dsl.LineBuilder line,
                               String name,
                               double width,
                               double top,
                               double bottom) {
        line.name(name)
                .horizontal(width)
                .color(RULE)
                .thickness(0.55)
                .margin(new DocumentInsets(top, 0, bottom, 0));
    }

    private void addHeadline(SectionBuilder section, Header header) {
        section.spacing(4)
                .padding(new DocumentInsets(8, 0, 0, 0))
                .addParagraph(paragraph -> paragraph
                        .text(spacedUpper(name(header)))
                        .textStyle(style(HEADLINE_FONT, 24.0, DocumentTextDecoration.DEFAULT, HEADLINE))
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()))
                .addParagraph(paragraph -> paragraph
                        .text(spacedUpper("Professional Title"))
                        .textStyle(style(HEADLINE_FONT, 8.6, DocumentTextDecoration.DEFAULT, SOFT))
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.top(1)));
    }

    private void addContactLine(SectionBuilder section, Header header) {
        List<ContactPart> parts = contactParts(header);
        if (parts.isEmpty()) {
            return;
        }
        DocumentTextStyle textStyle = style(theme.bodyFont(), 8.3, DocumentTextDecoration.DEFAULT, SOFT);
        DocumentTextStyle separatorStyle = style(theme.bodyFont(), 8.3, DocumentTextDecoration.DEFAULT, RULE);

        section.spacing(0)
                .padding(new DocumentInsets(7, 0, 7, 0))
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

    private void renderBody(SectionBuilder section, CvModule module) {
        if (module == null) {
            return;
        }
        section.spacing(4)
                .padding(DocumentInsets.zero());
        for (CvModule.BodyBlock block : module.bodyBlocks()) {
            switch (block.kind()) {
                case PARAGRAPH -> renderParagraphBlock(section, block);
                case LIST -> renderListBlock(section, block);
                default -> {
                    // Tables / dividers / page breaks fall outside the
                    // classic single-column layout. Skip them — the template
                    // stays predictable across CVs.
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
                .textStyle(style(theme.bodyFont(), 8.7, DocumentTextDecoration.DEFAULT, INK))
                .lineSpacing(1.45)
                .align(TextAlign.LEFT)
                .margin(DocumentInsets.top(3)));
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
                .textStyle(style(theme.bodyFont(), 8.7, DocumentTextDecoration.DEFAULT, INK))
                .lineSpacing(1.35)
                .align(TextAlign.LEFT)
                .margin(DocumentInsets.top(1)));
    }

    private void renderWorkEntry(SectionBuilder section, WorkEntry entry) {
        DocumentTextStyle companyStyle = style(theme.bodyFont(), 8.8, DocumentTextDecoration.BOLD, INK);
        DocumentTextStyle metaStyle = style(theme.bodyFont(), 8.8, DocumentTextDecoration.DEFAULT, SOFT);
        DocumentTextStyle separatorStyle = style(theme.bodyFont(), 8.8, DocumentTextDecoration.DEFAULT, RULE);
        DocumentTextStyle dateStyle = style(theme.bodyFont(), 8.6, DocumentTextDecoration.DEFAULT, SOFT);
        DocumentTextStyle bodyStyle = style(theme.bodyFont(), 8.6, DocumentTextDecoration.DEFAULT, INK);

        section.addRow("CenteredHeadlineWorkEntry", row -> row
                .spacing(8)
                .weights(1.0, 0.45)
                .addSection("Title", titleColumn -> titleColumn
                        .padding(DocumentInsets.zero())
                        .addParagraph(paragraph -> paragraph
                                .textStyle(companyStyle)
                                .align(TextAlign.LEFT)
                                .margin(DocumentInsets.zero())
                                .rich(rich -> {
                                    rich.style(stripBasicMarkdown(entry.headingMain()), companyStyle);
                                    if (!entry.headingSub().isBlank()) {
                                        rich.style("  |  ", separatorStyle);
                                        rich.style(stripBasicMarkdown(entry.headingSub()), metaStyle);
                                    }
                                })))
                .addSection("Date", dateColumn -> dateColumn
                        .padding(DocumentInsets.zero())
                        .addParagraph(paragraph -> paragraph
                                .text(stripBasicMarkdown(entry.date()))
                                .textStyle(dateStyle)
                                .align(TextAlign.RIGHT)
                                .margin(DocumentInsets.zero()))));

        if (!entry.description().isBlank()) {
            section.addParagraph(paragraph -> paragraph
                    .text(entry.description())
                    .textStyle(bodyStyle)
                    .lineSpacing(1.35)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(2)));
        }
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
        int dashIdx = indexOfFirst(afterPipe, " - ");
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

    private static int indexOfFirst(String haystack, String needle) {
        return haystack.indexOf(needle);
    }

    private static HeadingParts splitHeading(String heading) {
        // Try the typographic separators most commonly used in resume strings,
        // then fall back to the first comma so positions like
        // "Operations Coordinator, BrightWave Services, Manchester, UK" still
        // render with the leading role in bold and the rest in regular weight.
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

    private record ContactPart(String text, DocumentLinkOptions linkOptions) {
    }

    private record HeadingParts(String main, String sub) {
    }

    private record WorkEntry(String headingMain, String headingSub, String date, String description) {
    }

    private static String name(Header header) {
        return header == null ? "" : safe(header.getName());
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String stripBasicMarkdown(String value) {
        // Rich-text spans are rendered without markdown post-processing, so
        // strip the common emphasis markers manually for the pieces we feed
        // into the row layout. Body paragraphs continue to flow through the
        // markdown-aware path and keep *italic* / **bold** intact.
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

    private static CvTheme defaultTheme() {
        return new CvTheme(
                new Color(34, 34, 34),
                new Color(34, 34, 34),
                new Color(34, 34, 34),
                new Color(170, 170, 170),
                FontName.POPPINS,
                FontName.LATO,
                20.5,
                9.0,
                8.5,
                4,
                Margin.top(2),
                0);
    }
}
