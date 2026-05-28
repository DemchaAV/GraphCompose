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
 * Templates v2 "Classic Serif" CV preset.
 *
 * <p>Editorial serif CV with a measured cover page (centered spaced-caps
 * name above a thin rule, soft-cream profile band, and a two-column
 * feature grid pairing Core Skills + Education on the left with
 * Experience + Selected Projects on the right) followed by a quieter
 * linear detail page (Experience / Projects / Education / Additional).
 * Visual signature ported from the legacy
 * {@code ClassicSerifCvTemplateComposer}: PT Serif throughout, bronze
 * accent, soft tan rule, traditional editorial restraint.</p>
 *
 * <p>Returns a hand-written {@link DocumentTemplate} that drives the
 * canonical PageFlow DSL directly because the two-page cover/detail
 * structure is richer than what the slot-based
 * {@link com.demcha.compose.document.templates.cv.builder.CvBuilder}
 * abstraction exposes. To customise, copy this class and rewrite the
 * row / section calls.</p>
 *
 * @deprecated Superseded by the layered <code>…v2…</code> surface (the current
 *             standard). Kept for backward compatibility; scheduled for removal
 *             in a future major. See {@code docs/templates/v2-layered/} and
 *             {@link com.demcha.compose.document.templates.cv.v2.presets.ClassicSerif}.
 */
@Deprecated(since = "1.7.0", forRemoval = true)
public final class ClassicSerif {

    /** Stable template identifier. */
    public static final String ID = "classic-serif";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Classic Serif";

    /** Recommended page margin (in points) — matches V1 ClassicSerif gallery. */
    public static final double RECOMMENDED_MARGIN = 20.0;

    // V1 ClassicSerifCvTemplateComposer palette tokens.
    private static final DocumentColor INK = DocumentColor.rgb(45, 43, 40);
    private static final DocumentColor MUTED = DocumentColor.rgb(105, 101, 94);
    private static final DocumentColor RULE = DocumentColor.rgb(187, 177, 160);
    private static final DocumentColor SOFT_FILL = DocumentColor.rgb(250, 247, 241);
    private static final DocumentColor ACCENT = DocumentColor.rgb(126, 93, 52);

    private static final FontName HEADER_FONT = FontName.PT_SERIF;
    private static final FontName BODY_FONT = FontName.PT_SERIF;

    private ClassicSerif() {
        // utility class — not instantiable
    }

    /**
     * Builds the {@code Classic Serif} template.
     *
     * @param theme active business theme; the preset overrides palette
     *              and typography to V1 ClassicSerif tokens, so the
     *              result reads identically across BusinessTheme variants
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new ClassicSerifTemplate();
    }

    private static final class ClassicSerifTemplate implements DocumentTemplate<CvSpec> {

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
                    .name("ClassicSerifRoot")
                    .spacing(8);

            addHeader(flow, spec.header(), document.canvas().innerWidth());
            addSummary(flow, findModule(spec, "summary", "professional summary", "profile"));
            addFeatureRow(flow, spec);
            flow.addPageBreak(pb -> pb.name("ClassicSerifDetailsPage"));
            addLinearModule(flow, "Experience",
                    findModule(spec, "experience", "employment"));
            addLinearModule(flow, "Projects",
                    findModule(spec, "projects"));
            addLinearModule(flow, "Education",
                    findModule(spec, "education", "certifications"));
            addLinearModule(flow, "Additional",
                    findModule(spec, "additional information", "additional"));
            flow.build();
        }

        private void addHeader(PageFlowBuilder flow, CvHeader header, double width) {
            flow.addSection("ClassicSerifHeader", section -> {
                section.spacing(5)
                        .padding(new DocumentInsets(8, 0, 7, 0))
                        .addParagraph(paragraph -> paragraph
                                .text(spacedUpper(safe(header == null ? "" : header.name())))
                                .textStyle(style(HEADER_FONT, 27.0,
                                        DocumentTextDecoration.DEFAULT, INK))
                                .align(TextAlign.CENTER)
                                .margin(DocumentInsets.zero()))
                        .addLine(line -> line
                                .name("ClassicSerifHeaderRule")
                                .horizontal(width)
                                .color(RULE)
                                .thickness(0.65)
                                .margin(new DocumentInsets(1, 0, 0, 0)));
                addContact(section, header);
            });
        }

        private void addContact(SectionBuilder section, CvHeader header) {
            List<ContactPart> parts = contactParts(header);
            if (parts.isEmpty()) {
                return;
            }
            DocumentTextStyle meta = style(BODY_FONT, 8.7,
                    DocumentTextDecoration.DEFAULT, MUTED);
            DocumentTextStyle link = style(BODY_FONT, 8.7,
                    DocumentTextDecoration.UNDERLINE, ACCENT);
            DocumentTextStyle separator = style(BODY_FONT, 8.7,
                    DocumentTextDecoration.DEFAULT, RULE);
            section.addParagraph(paragraph -> paragraph
                    .textStyle(meta)
                    .align(TextAlign.CENTER)
                    .lineSpacing(1.15)
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
                                rich.style("   |   ", separator);
                            }
                        }
                    }));
        }

        private void addSummary(PageFlowBuilder flow, CvModule summary) {
            String text = String.join(" ", moduleLines(summary));
            if (text.isBlank()) {
                return;
            }
            flow.addSection("ClassicSerifSummary", section -> {
                section.spacing(5)
                        .padding(new DocumentInsets(12, 18, 13, 18))
                        .fillColor(SOFT_FILL)
                        .accentTop(ACCENT, 1.15)
                        .accentBottom(RULE, 0.45);
                addTitle(section, "Professional Profile", TextAlign.CENTER);
                section.addParagraph(paragraph -> paragraph
                        .text(stripMarkdown(text))
                        .textStyle(style(BODY_FONT, 9.8,
                                DocumentTextDecoration.DEFAULT, INK))
                        .lineSpacing(1.55)
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()));
            });
        }

        private void addFeatureRow(PageFlowBuilder flow, CvSpec spec) {
            flow.addRow("ClassicSerifFirstPageGrid", row -> row
                    .spacing(15)
                    .weights(1.0, 1.0)
                    .addSection("ClassicSerifSkillsColumn", left -> {
                        left.spacing(7);
                        addFeatureModule(left, "Core Skills",
                                findModule(spec, "technical skills", "skills"), 7);
                        addFeatureModule(left, "Education",
                                findModule(spec, "education", "certifications"), 3);
                    })
                    .addSection("ClassicSerifEvidenceColumn", right -> {
                        right.spacing(7);
                        addFeatureModule(right, "Experience",
                                findModule(spec, "experience", "employment"), 2);
                        addFeatureModule(right, "Selected Projects",
                                findModule(spec, "projects"), 3);
                    }));
        }

        private void addFeatureModule(SectionBuilder parent, String title,
                                      CvModule module, int limit) {
            List<String> lines = moduleLines(module).stream().limit(limit).toList();
            if (lines.isEmpty()) {
                return;
            }
            parent.addSection("ClassicSerifFeature" + normalize(title), section -> {
                section.spacing(4)
                        .padding(new DocumentInsets(9, 10, 10, 10))
                        .stroke(DocumentStroke.of(RULE, 0.35));
                addTitle(section, title, TextAlign.LEFT);
                section.addList(list -> list
                        .items(lines.stream()
                                .map(ClassicSerif::stripMarkdown)
                                .toList())
                        .bullet()
                        .textStyle(style(BODY_FONT, 8.65,
                                DocumentTextDecoration.DEFAULT, INK))
                        .lineSpacing(1.25)
                        .itemSpacing(2.0)
                        .margin(DocumentInsets.zero()));
            });
        }

        private void addLinearModule(PageFlowBuilder flow, String title, CvModule module) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            flow.addSection("ClassicSerif" + normalize(title), section -> {
                section.spacing(4)
                        .padding(new DocumentInsets(0, 0, 2, 0));
                addTitle(section, title, TextAlign.LEFT);
                section.addLine(line -> line
                        .name("ClassicSerif" + normalize(title) + "Rule")
                        .horizontal(72)
                        .color(ACCENT)
                        .thickness(1.0)
                        .margin(new DocumentInsets(0, 0, 2, 0)));
                for (String line : lines) {
                    renderLine(section, line);
                }
            });
        }

        private void renderLine(SectionBuilder section, String rawLine) {
            WorkEntry entry = parseWorkEntry(rawLine);
            if (entry == null) {
                section.addParagraph(paragraph -> paragraph
                        .text(stripMarkdown(rawLine))
                        .textStyle(style(BODY_FONT, 9.0,
                                DocumentTextDecoration.DEFAULT, INK))
                        .lineSpacing(1.35)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(1)));
                return;
            }

            section.addRow("ClassicSerifEntryHeader", row -> row
                    .spacing(12)
                    .weights(1.0, 0.36)
                    .addSection("Title", titleCol -> titleCol
                            .padding(DocumentInsets.zero())
                            .addParagraph(paragraph -> paragraph
                                    .text(stripMarkdown(entry.heading()))
                                    .textStyle(style(BODY_FONT, 9.2,
                                            DocumentTextDecoration.BOLD, INK))
                                    .align(TextAlign.LEFT)
                                    .margin(DocumentInsets.zero())))
                    .addSection("Date", dateCol -> dateCol
                            .padding(DocumentInsets.zero())
                            .addParagraph(paragraph -> paragraph
                                    .text(stripMarkdown(entry.date()))
                                    .textStyle(style(BODY_FONT, 8.7,
                                            DocumentTextDecoration.DEFAULT, MUTED))
                                    .align(TextAlign.RIGHT)
                                    .margin(DocumentInsets.zero()))));
            if (!entry.description().isBlank()) {
                section.addParagraph(paragraph -> paragraph
                        .text(stripMarkdown(entry.description()))
                        .textStyle(style(BODY_FONT, 8.8,
                                DocumentTextDecoration.DEFAULT, INK))
                        .lineSpacing(1.35)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(1)));
            }
        }

        private void addTitle(SectionBuilder section, String title, TextAlign align) {
            section.addParagraph(paragraph -> paragraph
                    .text(spacedUpper(title))
                    .textStyle(style(HEADER_FONT, 9.2,
                            DocumentTextDecoration.BOLD, ACCENT))
                    .align(align)
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
        addPart(parts, safe(header.phone()), null);
        String email = safe(header.email());
        if (!email.isBlank()) {
            addPart(parts, email, new DocumentLinkOptions("mailto:" + email));
        }
        addPart(parts, safe(header.address()), null);
        for (CvHeader.Link link : header.links()) {
            addPart(parts, safe(link.label()), safe(link.url()).isBlank()
                    ? null
                    : new DocumentLinkOptions(link.url().trim()));
        }
        return List.copyOf(parts);
    }

    private static WorkEntry parseWorkEntry(String item) {
        int pipeIndex = safe(item).indexOf('|');
        if (pipeIndex < 0) {
            return null;
        }
        String heading = item.substring(0, pipeIndex).trim();
        String afterPipe = item.substring(pipeIndex + 1).trim();
        int dash = afterPipe.indexOf(" - ");
        if (heading.isBlank() || dash <= 0) {
            return null;
        }
        String date = afterPipe.substring(0, dash).trim();
        String description = afterPipe.substring(dash + 3).trim();
        if (!date.matches(".*(\\d{4}|Present|present|Ongoing|ongoing).*")) {
            return null;
        }
        return new WorkEntry(heading, date, description);
    }

    private static String stripMarkdown(String value) {
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
        for (int index = 0; index < upper.length(); index++) {
            char current = upper.charAt(index);
            builder.append(current);
            if (Character.isLetterOrDigit(current)
                    && index + 1 < upper.length()
                    && Character.isLetterOrDigit(upper.charAt(index + 1))) {
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

    private record WorkEntry(String heading, String date, String description) {
    }
}
