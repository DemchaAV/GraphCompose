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
 * Two-page friendly resume composer that renders each section behind a soft
 * grey banner. Replicates the conventional "boxed-sections" Word resume:
 *
 * <ul>
 *   <li>Centered headline with a thin rule above and below the contact line.</li>
 *   <li>One pale grey banner per section, holding a centered letter-spaced title.</li>
 *   <li>Work entries laid out as bold position on the left, date range on the
 *       right, an italic {@code Company, Location} subtitle on the next line,
 *       optional description paragraph, and bullet items below.</li>
 *   <li>Plain modules (technical skills, projects, additional info) flow as
 *       body paragraphs without bullet markers.</li>
 * </ul>
 */
public final class BoxedSectionsCvTemplateComposer {
    private static final DocumentColor INK = DocumentColor.rgb(34, 34, 34);
    private static final DocumentColor SOFT = DocumentColor.rgb(85, 85, 85);
    private static final DocumentColor MUTED = DocumentColor.rgb(120, 120, 120);
    private static final DocumentColor RULE = DocumentColor.rgb(170, 170, 170);
    private static final DocumentColor BANNER = DocumentColor.rgb(220, 226, 230);
    private static final FontName HEADLINE_FONT = FontName.PT_SERIF;
    private static final FontName BODY_FONT = FontName.PT_SERIF;

    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        CvDocumentSpec spec = Objects.requireNonNull(documentSpec, "documentSpec");
        double innerWidth = document.canvas().innerWidth();

        PageFlowBuilder pageFlow = document.dsl()
                .pageFlow()
                .name("BoxedSectionsRoot")
                .spacing(7)
                .addSection("BoxedSectionsHeader", section -> addHeadline(section, spec.header()))
                .addLine(line -> line
                        .name("BoxedSectionsHeaderRule")
                        .horizontal(innerWidth)
                        .color(RULE)
                        .thickness(0.7)
                        .margin(DocumentInsets.zero()))
                .addSection("BoxedSectionsContact", section -> addContactLine(section, spec.header()))
                .addLine(line -> line
                        .name("BoxedSectionsContactRule")
                        .horizontal(innerWidth)
                        .color(RULE)
                        .thickness(0.7)
                        .margin(DocumentInsets.zero()));

        List<CvModule> modules = spec.modules() == null ? List.of() : spec.modules();
        for (int i = 0; i < modules.size(); i++) {
            final CvModule module = modules.get(i);
            final int index = i;
            pageFlow.addSection(
                    "BoxedSectionsBanner_" + index,
                    section -> addSectionBanner(section, safe(module.title())));
            pageFlow.addSection(
                    "BoxedSectionsBody_" + index,
                    section -> addModuleBody(section, module));
        }

        pageFlow.build();
    }

    private void addHeadline(SectionBuilder section, Header header) {
        section.spacing(2)
                .padding(new DocumentInsets(0, 0, 4, 0))
                .addParagraph(paragraph -> paragraph
                        .text(spacedUpper(name(header)))
                        .textStyle(style(HEADLINE_FONT, 21.5, DocumentTextDecoration.DEFAULT, INK))
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()));
    }

    private void addContactLine(SectionBuilder section, Header header) {
        List<ContactPart> parts = contactParts(header);
        if (parts.isEmpty()) {
            return;
        }
        DocumentTextStyle textStyle = style(BODY_FONT, 8.5, DocumentTextDecoration.DEFAULT, INK);
        DocumentTextStyle separatorStyle = style(BODY_FONT, 8.5, DocumentTextDecoration.DEFAULT, RULE);

        section.spacing(0)
                .padding(new DocumentInsets(4, 0, 4, 0))
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

    private void addSectionBanner(SectionBuilder section, String title) {
        if (title == null || title.isBlank()) {
            return;
        }
        section.padding(new DocumentInsets(5, 0, 5, 0))
                .fillColor(BANNER)
                .margin(DocumentInsets.top(4))
                .addParagraph(paragraph -> paragraph
                        .text(spacedUpper(title))
                        .textStyle(style(HEADLINE_FONT, 9.6, DocumentTextDecoration.BOLD, INK))
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
                    // Tables and dividers fall outside the banner template.
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
        DocumentTextStyle positionStyle = style(BODY_FONT, 9.2, DocumentTextDecoration.BOLD, INK);
        DocumentTextStyle dateStyle = style(BODY_FONT, 8.8, DocumentTextDecoration.DEFAULT, INK);
        DocumentTextStyle subtitleStyle = style(BODY_FONT, 8.4, DocumentTextDecoration.ITALIC, MUTED);
        DocumentTextStyle bodyStyle = style(BODY_FONT, 8.6, DocumentTextDecoration.DEFAULT, INK);

        section.addRow("BoxedSectionsEntryHeader", row -> row
                .spacing(8)
                .weights(1.0, 0.45)
                .addSection("Title", titleColumn -> titleColumn
                        .padding(DocumentInsets.zero())
                        .addParagraph(paragraph -> paragraph
                                .text(stripBasicMarkdown(entry.headingMain()))
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

        if (!entry.headingSub().isBlank()) {
            section.addParagraph(paragraph -> paragraph
                    .text(stripBasicMarkdown(entry.headingSub()))
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

        HeadingParts heading = splitHeading(headingText);
        return new WorkEntry(heading.main(), heading.sub(), date, description);
    }

    private static HeadingParts splitHeading(String heading) {
        // Try the typographic separators a resume tends to use, then fall
        // back to the first comma so positions like
        // "Operations Coordinator, BrightWave Services, Manchester, UK"
        // still produce a clean bold-position + italic-company split.
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
