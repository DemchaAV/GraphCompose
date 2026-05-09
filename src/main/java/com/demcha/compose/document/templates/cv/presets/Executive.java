package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
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
 * Templates v2 "Executive" CV preset.
 *
 * <p>Polished business CV with restrained slate typography, a compact
 * left-aligned header (UPPERCASE name in deep slate, meta line, link
 * row, full-width muted rule below), and warm bronze module headings
 * over a single-column body. Visual signature ported from the legacy
 * {@code ExecutiveSlateCvTemplate}: Poppins for headings, Lato for
 * body, slate primary, bronze accent.</p>
 */
public final class Executive {

    /** Stable template identifier. */
    public static final String ID = "executive";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Executive";

    /** Recommended page margin (in points) — generous for an executive feel. */
    public static final double RECOMMENDED_MARGIN = 28.0;

    // V1 ExecutiveSlate palette tokens.
    private static final DocumentColor PRIMARY = DocumentColor.rgb(24, 35, 51);
    private static final DocumentColor BODY = DocumentColor.rgb(49, 58, 72);
    private static final DocumentColor ACCENT = DocumentColor.rgb(172, 112, 55);
    private static final DocumentColor MUTED_RULE = DocumentColor.rgb(193, 201, 211);

    private static final FontName HEADER_FONT = FontName.POPPINS;
    private static final FontName BODY_FONT = FontName.LATO;

    private static final double NAME_SIZE = 24.0;
    private static final double SECTION_SIZE = 10.8;
    private static final double BODY_SIZE = 9.5;
    private static final double META_SIZE = BODY_SIZE - 0.4;

    private Executive() {
        // utility class — not instantiable
    }

    /**
     * Builds the {@code Executive} template.
     *
     * @param theme active business theme; the preset overrides palette
     *              and typography to V1 ExecutiveSlate tokens, so the
     *              result reads identically across BusinessTheme variants
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new ExecutiveTemplate();
    }

    private static final class ExecutiveTemplate implements DocumentTemplate<CvSpec> {

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
                    .name("ExecutiveRoot")
                    .spacing(8);

            addHeader(flow, spec.header(), document.canvas().innerWidth());
            for (CvModule module : spec.modules()) {
                addModule(flow, module);
            }

            flow.build();
        }

        private void addHeader(PageFlowBuilder flow, CvHeader header, double width) {
            flow.addSection("ExecutiveHeader", section -> {
                section.spacing(2)
                        .padding(DocumentInsets.zero())
                        .addParagraph(paragraph -> paragraph
                                .text(safe(header == null ? "" : header.name())
                                        .toUpperCase(Locale.ROOT))
                                .textStyle(style(HEADER_FONT, NAME_SIZE,
                                        DocumentTextDecoration.BOLD, PRIMARY))
                                .align(TextAlign.LEFT)
                                .margin(DocumentInsets.zero()));
                String info = joinPipe(safe(header == null ? "" : header.address()),
                        safe(header == null ? "" : header.phone()));
                if (!info.isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(info)
                            .textStyle(style(BODY_FONT, META_SIZE,
                                    DocumentTextDecoration.DEFAULT, BODY))
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.top(2)));
                }
                addLinkRow(section, header);
                section.addLine(line -> line
                        .name("ExecutiveHeaderRule")
                        .horizontal(width)
                        .color(MUTED_RULE)
                        .thickness(1.1)
                        .margin(DocumentInsets.top(5)));
            });
        }

        private void addLinkRow(SectionBuilder section, CvHeader header) {
            List<ContactPart> parts = linkParts(header);
            if (parts.isEmpty()) {
                return;
            }
            DocumentTextStyle meta = style(BODY_FONT, BODY_SIZE,
                    DocumentTextDecoration.DEFAULT, BODY);
            DocumentTextStyle link = style(BODY_FONT, BODY_SIZE,
                    DocumentTextDecoration.UNDERLINE, ACCENT);
            section.addParagraph(paragraph -> paragraph
                    .textStyle(meta)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(1))
                    .rich(rich -> {
                        for (int index = 0; index < parts.size(); index++) {
                            ContactPart part = parts.get(index);
                            if (part.linkOptions() == null) {
                                rich.style(part.text(), meta);
                            } else {
                                rich.with(part.text(), link, part.linkOptions());
                            }
                            if (index < parts.size() - 1) {
                                rich.style(" | ", meta);
                            }
                        }
                    }));
        }

        private void addModule(PageFlowBuilder flow, CvModule module) {
            if (module == null) {
                return;
            }
            String title = module.title().isBlank() ? module.name() : module.title();
            flow.addSection("Executive" + normalize(title), section -> {
                section.spacing(3)
                        .padding(DocumentInsets.zero())
                        .addParagraph(paragraph -> paragraph
                                .text(safe(title).toUpperCase(Locale.ROOT))
                                .textStyle(style(HEADER_FONT, SECTION_SIZE,
                                        DocumentTextDecoration.BOLD, ACCENT))
                                .align(TextAlign.LEFT)
                                .margin(DocumentInsets.top(6)));
                renderBody(section, module.body());
            });
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
                renderNumberedList(section, n.items());
            } else if (body instanceof IndentedBlock i) {
                for (IndentedBlock.Item item : i.items()) {
                    String inline = (item.title().isBlank() ? "" : item.title())
                            + (item.title().isBlank() || item.body().isBlank() ? "" : " - ")
                            + (item.body().isBlank() ? "" : item.body());
                    renderParagraph(section, inline);
                }
            } else if (body instanceof KeyValueBlock kv) {
                for (KeyValueBlock.Entry entry : kv.entries()) {
                    renderParagraph(section, entry.key() + ": " + entry.value());
                }
            }
        }

        private void renderParagraph(SectionBuilder section, String rawLine) {
            if (rawLine == null || rawLine.isBlank()) {
                return;
            }
            section.addParagraph(paragraph -> paragraph
                    .text(stripMarkdown(rawLine))
                    .textStyle(style(BODY_FONT, BODY_SIZE,
                            DocumentTextDecoration.DEFAULT, BODY))
                    .lineSpacing(1.25)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(2)));
        }

        private void renderBulletList(SectionBuilder section, List<String> items) {
            List<String> cleaned = items.stream()
                    .filter(item -> item != null && !item.isBlank())
                    .map(Executive::stripMarkdown)
                    .toList();
            if (cleaned.isEmpty()) {
                return;
            }
            section.addList(list -> list
                    .items(cleaned)
                    .bullet()
                    .textStyle(style(BODY_FONT, BODY_SIZE,
                            DocumentTextDecoration.DEFAULT, BODY))
                    .lineSpacing(1.25)
                    .itemSpacing(2.0)
                    .margin(DocumentInsets.top(2)));
        }

        private void renderNumberedList(SectionBuilder section, List<String> items) {
            List<String> cleaned = items.stream()
                    .filter(item -> item != null && !item.isBlank())
                    .map(Executive::stripMarkdown)
                    .toList();
            if (cleaned.isEmpty()) {
                return;
            }
            section.addList(list -> list
                    .items(cleaned)
                    .marker("1.")
                    .textStyle(style(BODY_FONT, BODY_SIZE,
                            DocumentTextDecoration.DEFAULT, BODY))
                    .lineSpacing(1.25)
                    .itemSpacing(2.0)
                    .margin(DocumentInsets.top(2)));
        }
    }

    // -- helpers ---------------------------------------------------------

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

    private static List<ContactPart> linkParts(CvHeader header) {
        if (header == null) {
            return List.of();
        }
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
            parts.add(new ContactPart(label,
                    url.isBlank() ? null : new DocumentLinkOptions(url.trim())));
        }
        return List.copyOf(parts);
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

    private static String stripMarkdown(String value) {
        return safe(value)
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace("*", "")
                .replace("_", "");
    }

    private static String normalize(String value) {
        StringBuilder builder = new StringBuilder();
        String safeValue = safe(value);
        for (int index = 0; index < safeValue.length(); index++) {
            char current = Character.toLowerCase(safeValue.charAt(index));
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
