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
import com.demcha.compose.document.style.DocumentStroke;
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
 * Templates v2 "Panel" CV preset.
 *
 * <p>Panel-led CV (V1 ProductLeaderCvTemplate) — every section sits in
 * its own white card with a thin teal stroke, a soft-rounded corner,
 * and a small accent strip under the UPPERCASE module title. The
 * page opens with a tinted teal header card holding the centered
 * name, contact line, and link line; below that, a full-width
 * Profile panel sits above a two-column row that pairs Skills +
 * Education on the left and Experience + Projects on the right; an
 * Additional panel closes the document. Visual signature ported from
 * {@code PanelCvTemplateComposer.Layout.stacked}: Poppins headlines,
 * Lato body, deep slate ink, teal accent.</p>
 */
public final class Panel {

    /** Stable template identifier. */
    public static final String ID = "panel";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Panel";

    /** Recommended page margin (in points) — matches V1 ProductLeader. */
    public static final double RECOMMENDED_MARGIN = 18.0;

    // V1 ProductLeader palette tokens.
    private static final DocumentColor HEADER_FILL = DocumentColor.rgb(231, 246, 244);
    private static final DocumentColor HEADER_TEXT = DocumentColor.rgb(20, 44, 66);
    private static final DocumentColor HEADER_META = DocumentColor.rgb(54, 68, 84);
    private static final DocumentColor SIDEBAR_FILL = DocumentColor.rgb(244, 249, 248);
    private static final DocumentColor PANEL_FILL = DocumentColor.WHITE;
    private static final DocumentColor PANEL_STROKE = DocumentColor.rgb(179, 214, 211);
    private static final DocumentColor ACCENT = DocumentColor.rgb(0, 128, 128);
    private static final DocumentColor BODY_TEXT = DocumentColor.rgb(54, 68, 84);

    private static final FontName HEADER_FONT = FontName.POPPINS;
    private static final FontName BODY_FONT = FontName.LATO;

    private static final double NAME_SIZE = 22.0;
    private static final double SECTION_SIZE = 10.4;
    private static final double BODY_SIZE = 9.4;
    private static final double META_SIZE = BODY_SIZE - 0.5;

    private static final double CORNER_RADIUS = 7.0;
    private static final double ACCENT_WIDTH = 54.0;
    private static final double ACCENT_HEIGHT = 2.2;

    private Panel() {
        // utility class — not instantiable
    }

    /**
     * Builds the {@code Panel} template.
     *
     * @param theme active business theme; the preset overrides palette
     *              and typography to V1 ProductLeader tokens
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new PanelTemplate();
    }

    private static final class PanelTemplate implements DocumentTemplate<CvSpec> {

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

            PageFlowBuilder flow = document.dsl()
                    .pageFlow()
                    .name("PanelRoot")
                    .spacing(10);

            addHeader(flow, spec.header());
            addModulePanel(flow, "Profile", "Profile",
                    findModule(spec, "summary", "professional summary", "profile"));
            flow.addRow("PanelStackedCards", row -> row
                    .spacing(12)
                    .weights(1.0, 1.0)
                    .addSection("PanelStackedLeft", left -> {
                        left.spacing(9);
                        addModulePanel(left, "Skills", "Skills",
                                findModule(spec, "technical skills", "skills"));
                        addModulePanel(left, "Education", "Education",
                                findModule(spec, "education", "certifications"));
                    })
                    .addSection("PanelStackedRight", right -> {
                        right.spacing(9);
                        addModulePanel(right, "Experience", "Experience",
                                findModule(spec, "experience", "employment"));
                        addModulePanel(right, "Projects", "Projects",
                                findModule(spec, "projects"));
                    }));
            addModulePanel(flow, "Additional", "Additional",
                    findModule(spec, "additional information", "additional"));

            flow.build();
        }

        private void addHeader(PageFlowBuilder flow, CvHeader header) {
            if (header == null) {
                return;
            }
            flow.addSection("PanelHeader", section -> {
                section.spacing(4)
                        .padding(new DocumentInsets(14, 16, 14, 16))
                        .fillColor(HEADER_FILL)
                        .stroke(DocumentStroke.of(PANEL_STROKE, 0.4))
                        .cornerRadius(CORNER_RADIUS)
                        .addParagraph(paragraph -> paragraph
                                .text(safe(header.name()).toUpperCase(Locale.ROOT))
                                .textStyle(style(HEADER_FONT, NAME_SIZE,
                                        DocumentTextDecoration.BOLD, HEADER_TEXT))
                                .align(TextAlign.CENTER)
                                .margin(DocumentInsets.zero()));
                if (!safe(header.jobTitle()).isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(header.jobTitle())
                            .textStyle(style(BODY_FONT, BODY_SIZE,
                                    DocumentTextDecoration.DEFAULT, HEADER_META))
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.zero()));
                }
                String contact = joinPipe(safe(header.address()), safe(header.phone()));
                if (!contact.isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(contact)
                            .textStyle(style(BODY_FONT, META_SIZE,
                                    DocumentTextDecoration.DEFAULT, HEADER_META))
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.zero()));
                }
                addLinkRow(section, header);
            });
        }

        private void addLinkRow(SectionBuilder section, CvHeader header) {
            List<ContactPart> parts = new ArrayList<>();
            String email = safe(header.email());
            if (!email.isBlank()) {
                parts.add(new ContactPart(email, new DocumentLinkOptions("mailto:" + email)));
            }
            for (CvHeader.Link link : header.links()) {
                String label = safe(link.label());
                if (label.isBlank()) {
                    continue;
                }
                String url = safe(link.url());
                parts.add(new ContactPart(label, url.isBlank()
                        ? null
                        : new DocumentLinkOptions(url.trim())));
            }
            if (parts.isEmpty()) {
                return;
            }
            DocumentTextStyle meta = style(BODY_FONT, META_SIZE,
                    DocumentTextDecoration.DEFAULT, HEADER_META);
            DocumentTextStyle link = style(BODY_FONT, META_SIZE,
                    DocumentTextDecoration.UNDERLINE, ACCENT);
            section.addParagraph(paragraph -> paragraph
                    .textStyle(meta)
                    .align(TextAlign.CENTER)
                    .margin(DocumentInsets.zero())
                    .rich(rich -> {
                        for (int i = 0; i < parts.size(); i++) {
                            ContactPart part = parts.get(i);
                            if (part.linkOptions() != null) {
                                rich.with(part.text(), link, part.linkOptions());
                            } else {
                                rich.style(part.text(), meta);
                            }
                            if (i < parts.size() - 1) {
                                rich.style(" | ", meta);
                            }
                        }
                    }));
        }

        private void addModulePanel(PageFlowBuilder flow, String name, String title,
                                    CvModule module) {
            flow.addSection("Panel" + normalize(name) + "Card",
                    section -> renderPanel(section, title, module));
        }

        private void addModulePanel(SectionBuilder parent, String name, String title,
                                    CvModule module) {
            parent.addSection("Panel" + normalize(name) + "Card",
                    section -> renderPanel(section, title, module));
        }

        private void renderPanel(SectionBuilder section, String title, CvModule module) {
            if (module == null) {
                return;
            }
            section.spacing(5)
                    .padding(new DocumentInsets(10, 10, 10, 10))
                    .fillColor(PANEL_FILL)
                    .stroke(DocumentStroke.of(PANEL_STROKE, 0.45))
                    .cornerRadius(CORNER_RADIUS)
                    .addParagraph(paragraph -> paragraph
                            .text(title.toUpperCase(Locale.ROOT))
                            .textStyle(style(HEADER_FONT, SECTION_SIZE,
                                    DocumentTextDecoration.BOLD, ACCENT))
                            .margin(DocumentInsets.zero()))
                    .addShape(shape -> shape
                            .name("Panel" + normalize(title) + "Accent")
                            .size(ACCENT_WIDTH, ACCENT_HEIGHT)
                            .fillColor(ACCENT)
                            .cornerRadius(ACCENT_HEIGHT / 2.0)
                            .margin(DocumentInsets.zero()));
            renderBody(section, module.body());
        }

        private void renderBody(SectionBuilder section, Block body) {
            if (body instanceof ParagraphBlock p) {
                renderParagraph(section, p.text());
            } else if (body instanceof MultiParagraphBlock m) {
                for (String line : m.paragraphs()) {
                    renderParagraph(section, line);
                }
            } else if (body instanceof BulletListBlock b) {
                renderBulletList(section, b.items());
            } else if (body instanceof NumberedListBlock n) {
                renderBulletList(section, n.items());
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
            DocumentTextStyle base = style(BODY_FONT, BODY_SIZE,
                    DocumentTextDecoration.DEFAULT, BODY_TEXT);
            section.addParagraph(paragraph -> paragraph
                    .textStyle(base)
                    .lineSpacing(1.2)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(1))
                    .rich(rich -> appendMarkdown(rich, text, base)));
        }

        private void renderBulletList(SectionBuilder section, List<String> items) {
            List<String> cleaned = items.stream()
                    .filter(item -> item != null && !item.isBlank())
                    .map(Panel::stripBasicMarkdown)
                    .toList();
            if (cleaned.isEmpty()) {
                return;
            }
            section.addList(list -> list
                    .items(cleaned)
                    .bullet()
                    .textStyle(style(BODY_FONT, BODY_SIZE,
                            DocumentTextDecoration.DEFAULT, BODY_TEXT))
                    .lineSpacing(1.2)
                    .itemSpacing(3.0)
                    .margin(DocumentInsets.top(1)));
        }

        private void renderKeyValueEntry(SectionBuilder section, KeyValueBlock.Entry entry) {
            DocumentTextStyle base = style(BODY_FONT, BODY_SIZE,
                    DocumentTextDecoration.DEFAULT, BODY_TEXT);
            DocumentTextStyle keyStyle = style(BODY_FONT, BODY_SIZE,
                    DocumentTextDecoration.BOLD, HEADER_TEXT);
            section.addParagraph(paragraph -> paragraph
                    .textStyle(base)
                    .lineSpacing(1.2)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(1))
                    .rich(rich -> {
                        rich.style(safe(entry.key()) + ":", keyStyle);
                        rich.style(" ", base);
                        appendMarkdown(rich, safe(entry.value()), base);
                    }));
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

    private static CvModule findModule(CvSpec spec, String... keys) {
        if (spec == null || spec.modules() == null) {
            return null;
        }
        for (CvModule module : spec.modules()) {
            String normalized = normalize(safe(module.name()) + " " + safe(module.title()));
            for (String key : keys) {
                if (normalized.contains(normalize(key))) {
                    return module;
                }
            }
        }
        return null;
    }

    private static String stripBasicMarkdown(String value) {
        return safe(value)
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace("*", "")
                .replace("_", "");
    }

    private static String joinPipe(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(part.trim());
        }
        return sb.toString();
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
}
