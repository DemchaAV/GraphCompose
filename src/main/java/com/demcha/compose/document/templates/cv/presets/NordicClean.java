package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
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
 * Templates v2 "Nordic Clean" CV preset.
 *
 * <p>Editorial one-page CV with a quiet teal palette: full-width
 * identity row above a soft-tinted PROFILE panel, then a two-column
 * body where the left rail carries Skills / Education / Additional in
 * a tinted side panel and the right column carries Experience plus
 * Selected Projects. Visual signature ported from the legacy
 * {@code NordicCleanCvTemplateComposer}: spaced caps name, accent strip
 * under the name, soft teal-tinted PROFILE strip, sidebar with subtle
 * fill, and uppercase section labels under thin horizontal rules.</p>
 *
 * <p>Because this layout is structurally richer than what
 * {@link com.demcha.compose.document.templates.cv.builder.CvBuilder}
 * exposes (slot-based assembly with one module style for everything),
 * the preset returns a hand-written {@link DocumentTemplate} that
 * drives the canonical {@code PageFlow} DSL directly. To customise,
 * copy this class and rewrite the row / section calls.</p>
 *
 * <p>The {@link CvSpec} provides standard module names matched
 * case-insensitively; the preset reaches into the spec by topic
 * keywords ({@code "summary"}, {@code "skills"}, {@code "education"},
 * {@code "additional"}, {@code "experience"}, {@code "projects"}) so
 * naming variants like "Professional Summary" / "Profile" or
 * "Technical Skills" / "Skills" all work without configuration.</p>
 *
 * @deprecated Superseded by the layered <code>…v2…</code> surface (the current
 *             standard). Kept for backward compatibility; scheduled for removal
 *             in a future major. See {@code docs/templates/v2-layered/} and
 *             {@link com.demcha.compose.document.templates.cv.v2.presets.NordicClean}.
 */
@Deprecated(since = "1.7.0", forRemoval = true)
public final class NordicClean {

    /** Stable template identifier. */
    public static final String ID = "nordic-clean";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Nordic Clean";

    /** Recommended page margin (in points) — matches V1 NordicClean. */
    public static final double RECOMMENDED_MARGIN = 18.0;

    // V1 NordicCleanCvTemplateComposer palette tokens.
    private static final DocumentColor INK = DocumentColor.rgb(18, 39, 52);
    private static final DocumentColor MUTED = DocumentColor.rgb(82, 104, 116);
    private static final DocumentColor ACCENT = DocumentColor.rgb(28, 128, 135);
    private static final DocumentColor ACCENT_SOFT = DocumentColor.rgb(226, 244, 245);
    private static final DocumentColor RAIL_FILL = DocumentColor.rgb(244, 249, 249);
    private static final DocumentColor RULE = DocumentColor.rgb(188, 219, 222);

    private static final FontName HEADER_FONT = FontName.BARLOW;
    private static final FontName BODY_FONT = FontName.LATO;

    /** Subtitle rendered under the identity name (V1 hardcoded). */
    private static final String SUBTITLE = "BACKEND JAVA DEVELOPER";

    private NordicClean() {
        // utility class — not instantiable
    }

    /**
     * Builds the {@code Nordic Clean} template.
     *
     * @param theme active business theme; the preset overrides palette
     *              and typography to V1 NordicClean tokens, so the
     *              result reads identically across BusinessTheme variants
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new NordicCleanTemplate();
    }

    private static final class NordicCleanTemplate implements DocumentTemplate<CvSpec> {

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
                    .name("NordicCleanRoot")
                    .spacing(7);

            addHeader(flow, spec.header());
            addProfile(flow, findModule(spec, "summary", "professional summary", "profile"));
            addBody(flow, spec);

            flow.build();
        }

        private void addHeader(PageFlowBuilder flow, CvHeader header) {
            flow.addRow("NordicCleanHeader", row -> row
                    .spacing(14)
                    .weights(1.2, 0.8)
                    .addSection("NordicCleanIdentity", identity -> identity
                            .spacing(3)
                            .padding(new DocumentInsets(1, 0, 2, 0))
                            .addParagraph(paragraph -> paragraph
                                    .text(safe(header == null ? "" : header.name())
                                            .toUpperCase(Locale.ROOT))
                                    .textStyle(style(HEADER_FONT, 27.0,
                                            DocumentTextDecoration.BOLD, INK))
                                    .autoSize(27.0, 22.0)
                                    .margin(DocumentInsets.zero()))
                            .addShape(shape -> shape
                                    .name("NordicCleanNameAccent")
                                    .size(64, 2.6)
                                    .fillColor(ACCENT)
                                    .cornerRadius(1.3)
                                    .margin(DocumentInsets.zero()))
                            .addParagraph(paragraph -> paragraph
                                    .text(SUBTITLE)
                                    .textStyle(style(BODY_FONT, 7.7,
                                            DocumentTextDecoration.BOLD, MUTED))
                                    .margin(DocumentInsets.zero())))
                    .addSection("NordicCleanContact",
                            contact -> addContactStack(contact, header)));
        }

        private void addProfile(PageFlowBuilder flow, CvModule module) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            flow.addSection("NordicCleanProfile", section -> section
                    .spacing(4)
                    .padding(new DocumentInsets(8, 10, 8, 10))
                    .fillColor(ACCENT_SOFT)
                    .accentLeft(ACCENT, 3.0)
                    .cornerRadius(DocumentCornerRadius.right(4))
                    .addParagraph(paragraph -> paragraph
                            .text("PROFILE")
                            .textStyle(style(HEADER_FONT, 8.0,
                                    DocumentTextDecoration.BOLD, ACCENT))
                            .margin(DocumentInsets.zero()))
                    .addParagraph(paragraph -> paragraph
                            .text(lines.get(0))
                            .textStyle(style(BODY_FONT, 7.85,
                                    DocumentTextDecoration.DEFAULT, INK))
                            .lineSpacing(1.25)
                            .margin(DocumentInsets.zero())));
        }

        private void addBody(PageFlowBuilder flow, CvSpec spec) {
            flow.addRow("NordicCleanBody", row -> row
                    .spacing(15)
                    .weights(0.72, 1.28)
                    .addSection("NordicCleanRail", rail -> {
                        rail.spacing(8)
                                .padding(new DocumentInsets(9, 10, 9, 10))
                                .fillColor(RAIL_FILL)
                                .stroke(DocumentStroke.of(RULE, 0.35))
                                .cornerRadius(4);
                        addSkills(rail, findModule(spec, "technical skills", "skills"));
                        addEducation(rail, findModule(spec, "education", "certifications"));
                        addSimpleLines(rail, "Additional",
                                findModule(spec, "additional information", "additional"), 2);
                    })
                    .addSection("NordicCleanMain", main -> {
                        main.spacing(9);
                        addExperience(main, findModule(spec, "experience", "employment"));
                        addProjects(main, findModule(spec, "projects"));
                    }));
        }

        private void addSkills(SectionBuilder parent, CvModule module) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            parent.addSection("NordicCleanSkills", section -> {
                addHeading(section, "Skills", 82);
                for (String line : lines.stream().limit(7).toList()) {
                    addLabelValueLine(section, line, 7.15, 1.05);
                }
            });
        }

        private void addEducation(SectionBuilder parent, CvModule module) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            parent.addSection("NordicCleanEducation", section -> {
                addHeading(section, "Education", 82);
                for (String line : lines.stream().limit(4).toList()) {
                    WorkEntry entry = parseWorkEntry(line);
                    section.addParagraph(paragraph -> paragraph
                            .textStyle(style(BODY_FONT, 7.05,
                                    DocumentTextDecoration.DEFAULT, INK))
                            .lineSpacing(1.05)
                            .margin(DocumentInsets.bottom(2))
                            .rich(rich -> {
                                rich.style(entry.title(), style(BODY_FONT, 7.05,
                                        DocumentTextDecoration.BOLD, INK));
                                if (!entry.subtitle().isBlank()) {
                                    rich.style(" / " + entry.subtitle(),
                                            style(BODY_FONT, 7.05,
                                                    DocumentTextDecoration.DEFAULT, MUTED));
                                }
                                if (!entry.date().isBlank()) {
                                    rich.style(" / " + entry.date(),
                                            style(BODY_FONT, 6.85,
                                                    DocumentTextDecoration.DEFAULT, MUTED));
                                }
                            }));
                }
            });
        }

        private void addSimpleLines(SectionBuilder parent, String title, CvModule module, int limit) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            parent.addSection("NordicClean" + normalize(title), section -> {
                addHeading(section, title, 82);
                for (String line : lines.stream().limit(limit).toList()) {
                    addLabelValueLine(section, line, 7.1, 1.05);
                }
            });
        }

        private void addExperience(SectionBuilder parent, CvModule module) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            parent.addSection("NordicCleanExperience", section -> {
                addHeading(section, "Experience", 130);
                for (String line : lines.stream().limit(2).toList()) {
                    WorkEntry entry = parseWorkEntry(line);
                    addWorkEntry(section, entry);
                }
            });
        }

        private void addProjects(SectionBuilder parent, CvModule module) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            parent.addSection("NordicCleanProjects", section -> {
                addHeading(section, "Selected Projects", 130);
                for (String line : lines.stream().limit(4).toList()) {
                    ProjectEntry project = parseProjectEntry(line);
                    section.addParagraph(paragraph -> paragraph
                            .textStyle(style(BODY_FONT, 7.25,
                                    DocumentTextDecoration.DEFAULT, INK))
                            .lineSpacing(1.08)
                            .margin(DocumentInsets.bottom(3))
                            .rich(rich -> {
                                rich.style(project.title(),
                                        style(BODY_FONT, 7.35,
                                                DocumentTextDecoration.BOLD, INK));
                                if (!project.context().isBlank()) {
                                    rich.style(" " + project.context(),
                                            style(BODY_FONT, 6.95,
                                                    DocumentTextDecoration.DEFAULT, MUTED));
                                }
                                rich.style(" - " + project.description(),
                                        style(BODY_FONT, 7.2,
                                                DocumentTextDecoration.DEFAULT, INK));
                            }));
                }
            });
        }

        private void addWorkEntry(SectionBuilder section, WorkEntry entry) {
            section.addParagraph(paragraph -> paragraph
                    .textStyle(style(BODY_FONT, 8.0,
                            DocumentTextDecoration.DEFAULT, INK))
                    .margin(DocumentInsets.zero())
                    .rich(rich -> {
                        rich.style(entry.title(),
                                style(BODY_FONT, 8.0,
                                        DocumentTextDecoration.BOLD, INK));
                        if (!entry.date().isBlank()) {
                            rich.style(" / " + entry.date(),
                                    style(BODY_FONT, 7.35,
                                            DocumentTextDecoration.BOLD, ACCENT));
                        }
                    }));
            if (!entry.subtitle().isBlank()) {
                section.addParagraph(paragraph -> paragraph
                        .text(entry.subtitle())
                        .textStyle(style(BODY_FONT, 7.2,
                                DocumentTextDecoration.DEFAULT, MUTED))
                        .margin(DocumentInsets.zero()));
            }
            if (!entry.description().isBlank()) {
                section.addParagraph(paragraph -> paragraph
                        .text(entry.description())
                        .textStyle(style(BODY_FONT, 7.45,
                                DocumentTextDecoration.DEFAULT, INK))
                        .lineSpacing(1.12)
                        .margin(DocumentInsets.bottom(5)));
            }
        }

        private void addHeading(SectionBuilder section, String title, double ruleWidth) {
            section.spacing(3)
                    .addParagraph(paragraph -> paragraph
                            .text(title.toUpperCase(Locale.ROOT))
                            .textStyle(style(HEADER_FONT, 7.6,
                                    DocumentTextDecoration.BOLD, ACCENT))
                            .margin(DocumentInsets.zero()))
                    .addLine(line -> line
                            .horizontal(ruleWidth)
                            .color(ACCENT)
                            .thickness(1.1)
                            .margin(DocumentInsets.bottom(2)));
        }

        private void addLabelValueLine(SectionBuilder section, String line,
                                       double size, double lineSpacing) {
            String clean = stripMarkdown(line);
            int colon = clean.indexOf(':');
            section.addParagraph(paragraph -> paragraph
                    .textStyle(style(BODY_FONT, size,
                            DocumentTextDecoration.DEFAULT, INK))
                    .lineSpacing(lineSpacing)
                    .margin(DocumentInsets.bottom(1.5))
                    .rich(rich -> {
                        if (colon > 0) {
                            rich.style(clean.substring(0, colon + 1),
                                    style(BODY_FONT, size,
                                            DocumentTextDecoration.BOLD, INK));
                            rich.style(" " + clean.substring(colon + 1).trim(),
                                    style(BODY_FONT, size,
                                            DocumentTextDecoration.DEFAULT, MUTED));
                        } else {
                            rich.style(clean,
                                    style(BODY_FONT, size,
                                            DocumentTextDecoration.DEFAULT, INK));
                        }
                    }));
        }

        private void addContactStack(SectionBuilder section, CvHeader header) {
            section.spacing(2)
                    .padding(new DocumentInsets(3, 0, 0, 0));
            DocumentTextStyle meta = style(BODY_FONT, 7.4,
                    DocumentTextDecoration.DEFAULT, MUTED);
            DocumentTextStyle link = style(BODY_FONT, 7.4,
                    DocumentTextDecoration.UNDERLINE, ACCENT);
            for (ContactPart part : contactParts(header)) {
                section.addParagraph(paragraph -> paragraph
                        .textStyle(part.linkOptions() == null ? meta : link)
                        .align(TextAlign.RIGHT)
                        .link(part.linkOptions())
                        .margin(DocumentInsets.zero())
                        .text(part.text()));
            }
        }
    }

    // -- helpers (in-line port of V1 ProfessionalCvTemplateSupport) --------

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

    private static WorkEntry parseWorkEntry(String raw) {
        String item = stripMarkdown(raw);
        int pipeIndex = item.indexOf('|');
        if (pipeIndex < 0) {
            return new WorkEntry(item, "", "", "");
        }

        String headingText = item.substring(0, pipeIndex).trim();
        String afterPipe = item.substring(pipeIndex + 1).trim();
        String date = afterPipe;
        String description = "";
        int dashIdx = afterPipe.indexOf(" - ");
        if (dashIdx > 0) {
            date = afterPipe.substring(0, dashIdx).trim();
            description = afterPipe.substring(dashIdx + 3).trim();
        }

        String title = headingText;
        String subtitle = "";
        int comma = headingText.indexOf(", ");
        if (comma > 0) {
            title = headingText.substring(0, comma).trim();
            subtitle = headingText.substring(comma + 2).trim();
        } else {
            for (String separator : List.of(" – ", " — ", " - ")) {
                int idx = headingText.indexOf(separator);
                if (idx > 0) {
                    title = headingText.substring(0, idx).trim();
                    subtitle = headingText.substring(idx + separator.length()).trim();
                    break;
                }
            }
        }
        return new WorkEntry(title, subtitle, date, description);
    }

    private static ProjectEntry parseProjectEntry(String raw) {
        String clean = stripMarkdown(raw).replaceAll("\\s+", " ").trim();
        String heading = clean;
        String description = "";
        for (String separator : List.of(" – ", " — ", " - ")) {
            int idx = clean.indexOf(separator);
            if (idx > 0) {
                heading = clean.substring(0, idx).trim();
                description = clean.substring(idx + separator.length()).trim();
                break;
            }
        }

        String title = heading;
        String context = "";
        int contextStart = heading.indexOf('(');
        int contextEnd = heading.lastIndexOf(')');
        if (contextStart > 0 && contextEnd > contextStart) {
            title = heading.substring(0, contextStart).trim();
            context = heading.substring(contextStart, contextEnd + 1).trim();
        }
        return new ProjectEntry(title, context, description);
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
        String safeValue = safe(value).toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < safeValue.length(); i++) {
            char current = safeValue.charAt(i);
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

    private record WorkEntry(String title, String subtitle, String date, String description) {
    }

    private record ProjectEntry(String title, String context, String description) {
    }
}
