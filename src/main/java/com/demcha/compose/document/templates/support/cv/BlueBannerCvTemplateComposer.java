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
import com.demcha.compose.font.FontName;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Conventional "blue banner" resume composer: a centered headline framed by
 * thin black rules, a pipe-delimited contact line, and one light-blue
 * banner per section bracketed by hairline black borders.
 *
 * <p>Mirrors the popular Word resume look where every module title sits in
 * a soft blue strip. Body content stays on the white page; work entries
 * lay out as bold position + right-aligned date with a regular
 * {@code Company, Location} subtitle and an optional description.</p>
 */
public final class BlueBannerCvTemplateComposer {
    private static final DocumentColor INK = DocumentColor.rgb(34, 34, 34);
    private static final DocumentColor SOFT = DocumentColor.rgb(85, 85, 85);
    private static final DocumentColor BANNER_BG = DocumentColor.rgb(196, 216, 234);
    private static final DocumentColor BANNER_RULE = DocumentColor.rgb(60, 70, 90);
    private static final DocumentColor FRAME_RULE = DocumentColor.rgb(34, 34, 34);
    private static final FontName HEADLINE_FONT = FontName.PT_SERIF;
    private static final FontName BODY_FONT = FontName.LATO;

    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        CvDocumentSpec spec = Objects.requireNonNull(documentSpec, "documentSpec");
        double innerWidth = document.canvas().innerWidth();

        PageFlowBuilder pageFlow = document.dsl()
                .pageFlow()
                .name("BlueBannerRoot")
                .spacing(6)
                .addSection("BlueBannerHeader", section -> addHeadline(section, spec.header()))
                .addLine(line -> line
                        .name("BlueBannerContactRuleTop")
                        .horizontal(innerWidth)
                        .color(FRAME_RULE)
                        .thickness(0.7)
                        .margin(new DocumentInsets(2, 0, 0, 0)))
                .addSection("BlueBannerContact", section -> addContactLine(section, spec.header()))
                .addLine(line -> line
                        .name("BlueBannerContactRuleBottom")
                        .horizontal(innerWidth)
                        .color(FRAME_RULE)
                        .thickness(0.7)
                        .margin(DocumentInsets.zero()));

        List<CvModule> modules = spec.modules() == null ? List.of() : spec.modules();
        for (int i = 0; i < modules.size(); i++) {
            final CvModule module = modules.get(i);
            final int index = i;
            pageFlow.addLine(line -> line
                    .name("BlueBannerRuleTop_" + index)
                    .horizontal(innerWidth)
                    .color(BANNER_RULE)
                    .thickness(0.7)
                    .margin(new DocumentInsets(4, 0, 0, 0)));
            pageFlow.addSection(
                    "BlueBannerBanner_" + index,
                    section -> addSectionBanner(section, safe(module.title())));
            pageFlow.addLine(line -> line
                    .name("BlueBannerRuleBottom_" + index)
                    .horizontal(innerWidth)
                    .color(BANNER_RULE)
                    .thickness(0.7)
                    .margin(DocumentInsets.zero()));
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
                        .textStyle(style(HEADLINE_FONT, 22, DocumentTextDecoration.BOLD, INK))
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()));
    }

    private void addContactLine(SectionBuilder section, Header header) {
        List<ContactPart> parts = contactParts(header);
        if (parts.isEmpty()) {
            return;
        }
        DocumentTextStyle textStyle = style(BODY_FONT, 8.6, DocumentTextDecoration.DEFAULT, INK);
        DocumentTextStyle separatorStyle = style(BODY_FONT, 8.6, DocumentTextDecoration.DEFAULT, SOFT);

        section.spacing(0)
                .padding(new DocumentInsets(2, 0, 2, 0))
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
                                    rich.style("  |  ", separatorStyle);
                                }
                            }
                        }));
    }

    private void addSectionBanner(SectionBuilder section, String title) {
        if (title == null || title.isBlank()) {
            return;
        }
        section.padding(new DocumentInsets(4, 0, 4, 0))
                .fillColor(BANNER_BG)
                .margin(DocumentInsets.zero())
                .addParagraph(paragraph -> paragraph
                        .text(spacedUpper(title))
                        .textStyle(style(BODY_FONT, 9.5, DocumentTextDecoration.BOLD, INK))
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()));
    }

    private void addModuleBody(SectionBuilder section, CvModule module) {
        section.spacing(4)
                .padding(new DocumentInsets(4, 4, 0, 4));
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
                .textStyle(style(BODY_FONT, 8.6, DocumentTextDecoration.DEFAULT, INK))
                .lineSpacing(1.5)
                .align(TextAlign.LEFT)
                .margin(DocumentInsets.top(2)));
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
                .textStyle(style(BODY_FONT, 8.6, DocumentTextDecoration.DEFAULT, INK))
                .lineSpacing(1.4)
                .align(TextAlign.LEFT)
                .margin(DocumentInsets.top(2)));
    }

    private void renderWorkEntry(SectionBuilder section, WorkEntry entry) {
        DocumentTextStyle positionStyle = style(BODY_FONT, 9.0, DocumentTextDecoration.BOLD, INK);
        DocumentTextStyle dateStyle = style(BODY_FONT, 8.8, DocumentTextDecoration.DEFAULT, INK);
        DocumentTextStyle subtitleStyle = style(BODY_FONT, 8.4, DocumentTextDecoration.ITALIC, SOFT);
        DocumentTextStyle bodyStyle = style(BODY_FONT, 8.6, DocumentTextDecoration.DEFAULT, INK);

        section.addRow("BlueBannerEntryHeader", row -> row
                .gap(8)
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
                    .lineSpacing(1.4)
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
