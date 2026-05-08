package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
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
 * Templates v2 "Compact Mono" CV preset.
 *
 * <p>Compact technical CV variant with a dark command-bar header
 * (UPPERCASE name on a near-black panel with a left-aligned contact
 * line of cyan links over a soft gray meta), a dense skill rail on
 * the left (RAIL fill + ACCENT left rule, no-marker compact lists)
 * and a wider evidence column on the right (white cards with thin
 * RULE stroke). Visual signature ported from the legacy
 * {@code CompactMonoCvTemplateComposer}: IBM Plex Mono for headings,
 * Lato for body, teal accent.</p>
 *
 * <p>Returns a hand-written {@link DocumentTemplate} that drives the
 * canonical PageFlow DSL directly because the dark-header / two-column
 * structure is richer than what the slot-based
 * {@link com.demcha.compose.document.templates.cv.builder.CvBuilder}
 * abstraction exposes.</p>
 */
public final class CompactMono {

    /** Stable template identifier. */
    public static final String ID = "compact-mono";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Compact Mono";

    /** Recommended page margin (in points) — matches V1 CompactMono gallery. */
    public static final double RECOMMENDED_MARGIN = 20.0;

    // V1 CompactMonoCvTemplateComposer palette tokens.
    private static final DocumentColor INK = DocumentColor.rgb(28, 34, 42);
    private static final DocumentColor PAPER = DocumentColor.rgb(248, 250, 252);
    private static final DocumentColor RAIL = DocumentColor.rgb(236, 244, 242);
    private static final DocumentColor RULE = DocumentColor.rgb(188, 204, 215);
    private static final DocumentColor ACCENT = DocumentColor.rgb(0, 126, 151);
    private static final DocumentColor HEADER = DocumentColor.rgb(18, 24, 32);
    private static final DocumentColor HEADER_SOFT = DocumentColor.rgb(192, 207, 219);
    private static final DocumentColor LINK_CYAN = DocumentColor.rgb(108, 213, 222);
    private static final DocumentColor SEPARATOR_GRAY = DocumentColor.rgb(102, 117, 132);

    private static final FontName HEADER_FONT = FontName.IBM_PLEX_MONO;
    private static final FontName BODY_FONT = FontName.LATO;

    private CompactMono() {
        // utility class — not instantiable
    }

    /**
     * Builds the {@code Compact Mono} template.
     *
     * @param theme active business theme; the preset overrides palette
     *              and typography to V1 CompactMono tokens, so the
     *              result reads identically across BusinessTheme variants
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new CompactMonoTemplate();
    }

    private static final class CompactMonoTemplate implements DocumentTemplate<CvSpec> {

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
                    .name("CompactMonoRoot")
                    .spacing(9)
                    .fillColor(PAPER);

            addHeader(flow, spec.header(), document.canvas().innerWidth());
            flow.addRow("CompactMonoBody", row -> row
                    .spacing(14)
                    .weights(0.78, 1.62)
                    .addSection("CompactMonoRail", rail -> {
                        rail.spacing(8)
                                .padding(new DocumentInsets(11, 11, 13, 11))
                                .fillColor(RAIL)
                                .accentLeft(ACCENT, 3.0);
                        addRailModule(rail, "Skills",
                                findModule(spec, "technical skills", "skills"), true, 7);
                        addRailModule(rail, "Education",
                                findModule(spec, "education", "certifications"), false, 3);
                        addRailModule(rail, "Additional",
                                findModule(spec, "additional information", "additional"), false, 4);
                    })
                    .addSection("CompactMonoMain", main -> {
                        main.spacing(8);
                        addMainModule(main, "Profile",
                                findModule(spec, "summary", "professional summary", "profile"), 1);
                        addMainModule(main, "Experience",
                                findModule(spec, "experience", "employment"), 2);
                        addMainModule(main, "Selected Projects",
                                findModule(spec, "projects"), 4);
                    }));

            flow.build();
        }

        private void addHeader(PageFlowBuilder flow, CvHeader header, double width) {
            flow.addSection("CompactMonoHeader", section -> {
                section.spacing(4)
                        .padding(new DocumentInsets(13, 16, 14, 16))
                        .fillColor(HEADER)
                        .cornerRadius(3)
                        .addParagraph(paragraph -> paragraph
                                .text(safe(header == null ? "" : header.name())
                                        .toUpperCase(Locale.ROOT))
                                .textStyle(style(HEADER_FONT, 23.5,
                                        DocumentTextDecoration.BOLD, DocumentColor.WHITE))
                                .align(TextAlign.LEFT)
                                .margin(DocumentInsets.zero()));
                addContact(section, header);
                section.addLine(line -> line
                        .name("CompactMonoHeaderWidthRule")
                        .horizontal(Math.max(0, width - 32))
                        .color(HEADER)
                        .thickness(0.1)
                        .margin(DocumentInsets.zero()));
            });
        }

        private void addContact(SectionBuilder section, CvHeader header) {
            List<ContactPart> parts = contactParts(header);
            if (parts.isEmpty()) {
                return;
            }
            DocumentTextStyle meta = style(BODY_FONT, 8.3,
                    DocumentTextDecoration.DEFAULT, HEADER_SOFT);
            DocumentTextStyle link = style(BODY_FONT, 8.3,
                    DocumentTextDecoration.UNDERLINE, LINK_CYAN);
            DocumentTextStyle separator = style(BODY_FONT, 8.3,
                    DocumentTextDecoration.DEFAULT, SEPARATOR_GRAY);
            section.addParagraph(paragraph -> paragraph
                    .textStyle(meta)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.zero())
                    .rich(rich -> {
                        for (int index = 0; index < parts.size(); index++) {
                            ContactPart part = parts.get(index);
                            if (part.linkOptions() == null) {
                                rich.style(part.text(), meta);
                            } else {
                                rich.with(part.text(), link, part.linkOptions());
                            }
                            if (index < parts.size() - 1) {
                                rich.style("  /  ", separator);
                            }
                        }
                    }));
        }

        private void addRailModule(SectionBuilder parent, String title, CvModule module,
                                   boolean skillMode, int limit) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            parent.addSection("CompactMono" + normalize(title), section -> {
                section.spacing(4)
                        .padding(new DocumentInsets(0, 0, 2, 0));
                addModuleLabel(section, title, 8.0);
                List<String> display = lines.stream()
                        .limit(limit)
                        .map(skillMode
                                ? CompactMono::compactSkillLine
                                : CompactMono::stripMarkdown)
                        .filter(line -> !line.isBlank())
                        .toList();
                section.addList(list -> list
                        .items(display)
                        .noMarker()
                        .continuationIndent("  ")
                        .textStyle(style(BODY_FONT, 7.65,
                                DocumentTextDecoration.DEFAULT, INK))
                        .lineSpacing(0.95)
                        .itemSpacing(2.2)
                        .margin(DocumentInsets.zero()));
            });
        }

        private void addMainModule(SectionBuilder parent, String title, CvModule module, int limit) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            parent.addSection("CompactMono" + normalize(title), section -> {
                section.spacing(4)
                        .padding(new DocumentInsets(9, 10, 10, 11))
                        .fillColor(DocumentColor.WHITE)
                        .stroke(DocumentStroke.of(RULE, 0.35))
                        .cornerRadius(3);
                addModuleLabel(section, title, 8.4);
                for (String line : lines.stream().limit(limit).toList()) {
                    String text = stripMarkdown(line);
                    section.addParagraph(paragraph -> paragraph
                            .text(text)
                            .textStyle(style(BODY_FONT, 8.45,
                                    DocumentTextDecoration.DEFAULT, INK))
                            .lineSpacing(1.25)
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.zero()));
                }
            });
        }

        private void addModuleLabel(SectionBuilder section, String title, double size) {
            section.addShape(shape -> shape
                    .name("CompactMonoLabelTick")
                    .size(22, 2.2)
                    .fillColor(ACCENT)
                    .cornerRadius(1.1)
                    .margin(DocumentInsets.zero()));
            section.addParagraph(paragraph -> paragraph
                    .text(title.toUpperCase(Locale.ROOT))
                    .textStyle(style(HEADER_FONT, size,
                            DocumentTextDecoration.BOLD, ACCENT))
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.zero()));
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

    private static List<String> moduleLines(CvModule module) {
        if (module == null) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        Block body = module.body();
        if (body instanceof ParagraphBlock p) {
            addLines(lines, p.text());
        } else if (body instanceof MultiParagraphBlock m) {
            m.paragraphs().forEach(line -> addLines(lines, line));
        } else if (body instanceof BulletListBlock b) {
            b.items().forEach(item -> addLines(lines, item));
        } else if (body instanceof NumberedListBlock n) {
            n.items().forEach(item -> addLines(lines, item));
        } else if (body instanceof IndentedBlock i) {
            i.items().forEach(item -> {
                String title = safe(item.title());
                String bodyText = safe(item.body());
                if (title.isBlank() && bodyText.isBlank()) {
                    return;
                }
                if (title.isBlank()) {
                    lines.add(bodyText);
                } else if (bodyText.isBlank()) {
                    lines.add(title);
                } else {
                    lines.add(title + " | " + bodyText);
                }
            });
        } else if (body instanceof KeyValueBlock kv) {
            kv.entries().forEach(entry -> addLines(lines, entry.key() + ": " + entry.value()));
        }
        return List.copyOf(lines);
    }

    private static List<ContactPart> contactParts(CvHeader header) {
        if (header == null) {
            return List.of();
        }
        List<ContactPart> parts = new ArrayList<>();
        addPart(parts, safe(header.address()), null);
        addPart(parts, safe(header.phone()), null);
        String email = safe(header.email());
        if (!email.isBlank()) {
            addPart(parts, email, new DocumentLinkOptions("mailto:" + email));
        }
        for (CvHeader.Link link : header.links()) {
            addPart(parts, safe(link.label()), safe(link.url()).isBlank()
                    ? null
                    : new DocumentLinkOptions(link.url().trim()));
        }
        return List.copyOf(parts);
    }

    private static String compactSkillLine(String line) {
        String clean = stripMarkdown(line);
        int colon = clean.indexOf(':');
        if (colon < 0) {
            return clean;
        }
        String label = clean.substring(0, colon).trim();
        String[] tokens = clean.substring(colon + 1).split(",");
        List<String> picked = new ArrayList<>();
        for (String token : tokens) {
            String value = token.trim();
            if (!value.isBlank()) {
                picked.add(value);
            }
            if (picked.size() == 4) {
                break;
            }
        }
        return label + " :: " + String.join(", ", picked);
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

    private static void addLines(List<String> lines, String value) {
        for (String line : safe(value).split("\\R")) {
            String clean = stripMarkdown(line).trim();
            if (!clean.isBlank()) {
                lines.add(clean);
            }
        }
    }

    private static void addPart(List<ContactPart> parts, String text,
                                DocumentLinkOptions linkOptions) {
        if (text != null && !text.isBlank()) {
            parts.add(new ContactPart(text.trim(), linkOptions));
        }
    }

    private record ContactPart(String text, DocumentLinkOptions linkOptions) {
    }
}
