package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.LineBuilder;
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
 * Templates v2 "Centered Headline" CV preset.
 *
 * <p>Classic single-column resume with a centered spaced-caps name above
 * a "Professional Title" subline, thin separator rules above and below
 * the contact line, and section blocks made of bold work entries plus
 * body paragraphs. Visual signature ported from the legacy
 * {@code CenteredHeadlineCvTemplateComposer}: Poppins headline,
 * Lato body, dark grey ink, slim full-width rules.</p>
 *
 * <p>Inline markdown ({@code **bold**}, {@code *italic*}) is parsed
 * through the shared {@link MarkdownText} helper so spec authors can
 * carry inline emphasis in body text without preprocessing.</p>
 */
public final class CenteredHeadline {

    /** Stable template identifier. */
    public static final String ID = "centered-headline";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Centered Headline";

    /** Recommended page margin (in points). */
    public static final double RECOMMENDED_MARGIN = 28.0;

    // V1 CenteredHeadlineCvTemplateComposer palette tokens.
    private static final DocumentColor INK = DocumentColor.rgb(54, 54, 54);
    private static final DocumentColor HEADLINE = DocumentColor.rgb(70, 70, 70);
    private static final DocumentColor SOFT = DocumentColor.rgb(105, 105, 105);
    private static final DocumentColor RULE = DocumentColor.rgb(188, 188, 188);

    private static final FontName HEADLINE_FONT = FontName.POPPINS;
    private static final FontName BODY_FONT = FontName.LATO;

    private CenteredHeadline() {
        // utility class — not instantiable
    }

    /**
     * Builds the {@code Centered Headline} template.
     *
     * @param theme active business theme; the preset overrides palette
     *              and typography to V1 CenteredHeadline tokens
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new CenteredHeadlineTemplate();
    }

    private static final class CenteredHeadlineTemplate implements DocumentTemplate<CvSpec> {

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

            double ruleWidth = document.canvas().innerWidth();

            PageFlowBuilder pageFlow = document.dsl()
                    .pageFlow()
                    .name("CenteredHeadlineRoot")
                    .spacing(0)
                    .addSection("CenteredHeadlineHeader",
                            section -> addHeadline(section, spec.header()))
                    .addLine(line -> referenceRule(line,
                            "CenteredHeadlineHeaderRule", ruleWidth, 7, 0))
                    .addSection("CenteredHeadlineContact",
                            section -> addContactLine(section, spec.header()))
                    .addLine(line -> referenceRule(line,
                            "CenteredHeadlineContactRule", ruleWidth, 0, 13));

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
                            ruleWidth, 5, 12));
                }
            }

            pageFlow.build();
        }

        private void addHeadline(SectionBuilder section, CvHeader header) {
            section.spacing(4)
                    .padding(new DocumentInsets(8, 0, 0, 0))
                    .addParagraph(paragraph -> paragraph
                            .text(spacedUpper(name(header)))
                            .textStyle(style(HEADLINE_FONT, 24.0,
                                    DocumentTextDecoration.DEFAULT, HEADLINE))
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.zero()))
                    .addParagraph(paragraph -> paragraph
                            .text(spacedUpper("Professional Title"))
                            .textStyle(style(HEADLINE_FONT, 8.6,
                                    DocumentTextDecoration.DEFAULT, SOFT))
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.top(1)));
        }

        private void addContactLine(SectionBuilder section, CvHeader header) {
            List<ContactPart> parts = contactParts(header);
            if (parts.isEmpty()) {
                return;
            }
            DocumentTextStyle textStyle = style(BODY_FONT, 8.3,
                    DocumentTextDecoration.DEFAULT, SOFT);
            DocumentTextStyle separatorStyle = style(BODY_FONT, 8.3,
                    DocumentTextDecoration.DEFAULT, RULE);

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

        private void addModuleTitle(SectionBuilder section, String title) {
            section.addParagraph(paragraph -> paragraph
                    .text(spacedUpper(title))
                    .textStyle(style(BODY_FONT, 9.5,
                            DocumentTextDecoration.BOLD, SOFT))
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.zero()));
        }

        private void referenceRule(LineBuilder line,
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

        private void renderBody(SectionBuilder section, CvModule module) {
            if (module == null) {
                return;
            }
            section.spacing(4)
                    .padding(DocumentInsets.zero());
            Block body = module.body();
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
            DocumentTextStyle base = style(BODY_FONT, 8.7,
                    DocumentTextDecoration.DEFAULT, INK);
            section.addParagraph(paragraph -> paragraph
                    .textStyle(base)
                    .lineSpacing(1.45)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(3))
                    .rich(rich -> appendMarkdown(rich, text, base)));
        }

        private void renderBulletList(SectionBuilder section, List<String> items) {
            List<String> cleaned = items.stream()
                    .filter(item -> item != null && !item.isBlank())
                    .map(CenteredHeadline::stripBasicMarkdown)
                    .toList();
            if (cleaned.isEmpty()) {
                return;
            }
            section.addList(list -> list
                    .items(cleaned)
                    .bullet()
                    .textStyle(style(BODY_FONT, 8.7,
                            DocumentTextDecoration.DEFAULT, INK))
                    .lineSpacing(1.35)
                    .itemSpacing(2.0)
                    .margin(DocumentInsets.top(3)));
        }

        private void renderKeyValueEntry(SectionBuilder section, KeyValueBlock.Entry entry) {
            DocumentTextStyle base = style(BODY_FONT, 8.7,
                    DocumentTextDecoration.DEFAULT, INK);
            DocumentTextStyle keyStyle = style(BODY_FONT, 8.7,
                    DocumentTextDecoration.BOLD, INK);
            section.addParagraph(paragraph -> paragraph
                    .textStyle(base)
                    .lineSpacing(1.45)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(3))
                    .rich(rich -> {
                        rich.style(safe(entry.key()) + ":", keyStyle);
                        rich.style(" ", base);
                        appendMarkdown(rich, safe(entry.value()), base);
                    }));
        }

        private void renderWorkEntry(SectionBuilder section, WorkEntry entry) {
            DocumentTextStyle companyStyle = style(BODY_FONT, 8.8,
                    DocumentTextDecoration.BOLD, INK);
            DocumentTextStyle metaStyle = style(BODY_FONT, 8.8,
                    DocumentTextDecoration.DEFAULT, SOFT);
            DocumentTextStyle separatorStyle = style(BODY_FONT, 8.8,
                    DocumentTextDecoration.DEFAULT, RULE);
            DocumentTextStyle dateStyle = style(BODY_FONT, 8.6,
                    DocumentTextDecoration.DEFAULT, SOFT);
            DocumentTextStyle bodyStyle = style(BODY_FONT, 8.6,
                    DocumentTextDecoration.DEFAULT, INK);

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
                                        rich.style(stripBasicMarkdown(entry.headingMain()),
                                                companyStyle);
                                        if (!entry.headingSub().isBlank()) {
                                            rich.style("  |  ", separatorStyle);
                                            rich.style(stripBasicMarkdown(entry.headingSub()),
                                                    metaStyle);
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
                        .textStyle(bodyStyle)
                        .lineSpacing(1.35)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(2))
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
        HeadingParts heading = splitHeading(headingText);
        return new WorkEntry(heading.main(), heading.sub(), date, description);
    }

    private static HeadingParts splitHeading(String heading) {
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

    private record HeadingParts(String main, String sub) {
    }

    private record WorkEntry(String headingMain, String headingSub, String date, String description) {
    }
}
