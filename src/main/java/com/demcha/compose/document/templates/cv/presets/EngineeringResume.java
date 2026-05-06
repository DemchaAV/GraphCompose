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
 * Templates v2 "Engineering Resume" CV preset.
 *
 * <p>Senior engineering CV with a full-width navy command header
 * (UPPERCASE name, subtitle, right-aligned contact stack with cyan-green
 * underlined links), a dark NAVY skill rail (Core Stack / Learning /
 * Details with green accent labels), and white evidence cards for
 * Leadership Experience plus Technical Evidence on the right. Visual
 * signature ported from the legacy {@code TechLeadCvTemplateComposer}:
 * Barlow headings, Lato body, navy primary, green accent.</p>
 */
public final class EngineeringResume {

    /** Stable template identifier. */
    public static final String ID = "engineering-resume";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Engineering Resume";

    /** Recommended page margin (in points) — matches V1 TechLead gallery. */
    public static final double RECOMMENDED_MARGIN = 20.0;

    // V1 TechLeadCvTemplateComposer palette tokens.
    private static final DocumentColor NAVY = DocumentColor.rgb(13, 32, 47);
    private static final DocumentColor NAVY_SOFT = DocumentColor.rgb(35, 56, 72);
    private static final DocumentColor INK = DocumentColor.rgb(32, 42, 55);
    private static final DocumentColor MUTED = DocumentColor.rgb(91, 105, 119);
    private static final DocumentColor GREEN = DocumentColor.rgb(27, 145, 104);
    private static final DocumentColor GREEN_SOFT = DocumentColor.rgb(232, 246, 239);
    private static final DocumentColor RULE = DocumentColor.rgb(190, 212, 204);

    // Specific lighter shades used inside the navy rail and contact column.
    private static final DocumentColor RAIL_TEXT = DocumentColor.rgb(220, 231, 236);
    private static final DocumentColor RAIL_DATE = DocumentColor.rgb(182, 201, 210);
    private static final DocumentColor SUBTITLE = DocumentColor.rgb(190, 209, 219);
    private static final DocumentColor CONTACT_META = DocumentColor.rgb(196, 211, 220);
    private static final DocumentColor CONTACT_LINK = DocumentColor.rgb(78, 207, 161);

    private static final FontName HEADER_FONT = FontName.BARLOW;
    private static final FontName BODY_FONT = FontName.LATO;

    /** Subtitle rendered under the identity name (V1 hardcoded). */
    private static final String SUBTITLE_TEXT = "SECURE BACKEND SYSTEMS / DELIVERY LEADERSHIP";

    private EngineeringResume() {
        // utility class — not instantiable
    }

    /**
     * Builds the {@code Engineering Resume} template.
     *
     * @param theme active business theme; the preset overrides palette
     *              and typography to V1 TechLead tokens
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new EngineeringResumeTemplate();
    }

    private static final class EngineeringResumeTemplate implements DocumentTemplate<CvSpec> {

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
                    .name("EngineeringResumeRoot")
                    .spacing(8);

            addHeader(flow, spec.header());
            addBody(flow, spec);

            flow.build();
        }

        private void addHeader(PageFlowBuilder flow, CvHeader header) {
            flow.addSection("EngineeringResumeHeader", section -> section
                    .spacing(5)
                    .padding(new DocumentInsets(13, 15, 13, 15))
                    .fillColor(NAVY)
                    .cornerRadius(DocumentCornerRadius.top(4))
                    .accentBottom(GREEN, 2.5)
                    .addRow("EngineeringResumeHeaderRow", row -> row
                            .spacing(12)
                            .weights(1.15, 0.85)
                            .addSection("EngineeringResumeIdentity", identity -> identity
                                    .padding(DocumentInsets.zero())
                                    .spacing(3)
                                    .addParagraph(paragraph -> paragraph
                                            .text(safe(header == null ? "" : header.name())
                                                    .toUpperCase(Locale.ROOT))
                                            .textStyle(style(HEADER_FONT, 24.5,
                                                    DocumentTextDecoration.BOLD, DocumentColor.WHITE))
                                            .autoSize(24.5, 19.0)
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(paragraph -> paragraph
                                            .text(SUBTITLE_TEXT)
                                            .textStyle(style(BODY_FONT, 7.6,
                                                    DocumentTextDecoration.BOLD, SUBTITLE))
                                            .margin(DocumentInsets.zero())))
                            .addSection("EngineeringResumeContact",
                                    contact -> addContactStack(contact, header))));
        }

        private void addBody(PageFlowBuilder flow, CvSpec spec) {
            flow.addRow("EngineeringResumeBody", row -> row
                    .spacing(14)
                    .weights(0.76, 1.64)
                    .addSection("EngineeringResumeRail", rail -> {
                        rail.spacing(8)
                                .padding(new DocumentInsets(10, 10, 11, 10))
                                .fillColor(NAVY)
                                .cornerRadius(DocumentCornerRadius.bottom(4))
                                .accentTop(GREEN, 2.0);
                        addSkills(rail, findModule(spec, "technical skills", "skills"));
                        addEducation(rail, findModule(spec, "education", "certifications"));
                        addAdditional(rail, findModule(spec, "additional information", "additional"));
                    })
                    .addSection("EngineeringResumeMain", main -> {
                        main.spacing(8);
                        addProfile(main, findModule(spec, "summary",
                                "professional summary", "profile"));
                        addExperience(main, findModule(spec, "experience", "employment"));
                        addProjects(main, findModule(spec, "projects"));
                    }));
        }

        private void addProfile(SectionBuilder parent, CvModule module) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            parent.addSection("EngineeringResumeProfile", section -> section
                    .spacing(4)
                    .padding(new DocumentInsets(8, 10, 8, 10))
                    .fillColor(GREEN_SOFT)
                    .accentLeft(GREEN, 3.0)
                    .cornerRadius(DocumentCornerRadius.right(4))
                    .addParagraph(paragraph -> paragraph
                            .text("ENGINEERING PROFILE")
                            .textStyle(style(HEADER_FONT, 8.0,
                                    DocumentTextDecoration.BOLD, GREEN))
                            .margin(DocumentInsets.zero()))
                    .addParagraph(paragraph -> paragraph
                            .text(stripMarkdown(lines.get(0)))
                            .textStyle(style(BODY_FONT, 7.75,
                                    DocumentTextDecoration.DEFAULT, INK))
                            .lineSpacing(1.2)
                            .margin(DocumentInsets.zero())));
        }

        private void addSkills(SectionBuilder parent, CvModule module) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            parent.addSection("EngineeringResumeSkills", section -> {
                addRailHeading(section, "Core Stack");
                for (String line : lines.stream().limit(7).toList()) {
                    String clean = stripMarkdown(line);
                    int colon = clean.indexOf(':');
                    section.addParagraph(paragraph -> paragraph
                            .textStyle(style(BODY_FONT, 6.95,
                                    DocumentTextDecoration.DEFAULT, DocumentColor.WHITE))
                            .lineSpacing(1.0)
                            .margin(DocumentInsets.bottom(1.8))
                            .rich(rich -> {
                                if (colon > 0) {
                                    rich.style(clean.substring(0, colon + 1),
                                            style(BODY_FONT, 6.95,
                                                    DocumentTextDecoration.BOLD, GREEN));
                                    rich.style(" " + compactValues(clean.substring(colon + 1), 5),
                                            style(BODY_FONT, 6.9,
                                                    DocumentTextDecoration.DEFAULT, RAIL_TEXT));
                                } else {
                                    rich.style(clean,
                                            style(BODY_FONT, 6.9,
                                                    DocumentTextDecoration.DEFAULT, DocumentColor.WHITE));
                                }
                            }));
                }
            });
        }

        private void addEducation(SectionBuilder parent, CvModule module) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            parent.addSection("EngineeringResumeEducation", section -> {
                addRailHeading(section, "Learning");
                for (String line : lines.stream().limit(4).toList()) {
                    WorkEntry entry = parseWorkEntry(line);
                    section.addParagraph(paragraph -> paragraph
                            .textStyle(style(BODY_FONT, 6.95,
                                    DocumentTextDecoration.DEFAULT, DocumentColor.WHITE))
                            .lineSpacing(1.0)
                            .margin(DocumentInsets.bottom(2.3))
                            .rich(rich -> {
                                rich.style(entry.title(),
                                        style(BODY_FONT, 6.95,
                                                DocumentTextDecoration.BOLD, DocumentColor.WHITE));
                                if (!entry.date().isBlank()) {
                                    rich.style(" / " + entry.date(),
                                            style(BODY_FONT, 6.7,
                                                    DocumentTextDecoration.DEFAULT, RAIL_DATE));
                                }
                            }));
                }
            });
        }

        private void addAdditional(SectionBuilder parent, CvModule module) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            parent.addSection("EngineeringResumeAdditional", section -> {
                addRailHeading(section, "Details");
                for (String line : lines.stream().limit(2).toList()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(stripMarkdown(line))
                            .textStyle(style(BODY_FONT, 6.95,
                                    DocumentTextDecoration.DEFAULT, RAIL_TEXT))
                            .lineSpacing(1.0)
                            .margin(DocumentInsets.bottom(1.8)));
                }
            });
        }

        private void addExperience(SectionBuilder parent, CvModule module) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            parent.addSection("EngineeringResumeExperience", section -> {
                addMainHeading(section, "Leadership Experience");
                for (String line : lines.stream().limit(2).toList()) {
                    WorkEntry entry = parseWorkEntry(line);
                    section.addSection("EngineeringResumeRoleCard", card -> {
                        card.spacing(3)
                                .padding(new DocumentInsets(6, 8, 6, 8))
                                .fillColor(DocumentColor.WHITE)
                                .stroke(DocumentStroke.of(RULE, 0.35))
                                .cornerRadius(DocumentCornerRadius.right(3))
                                .accentLeft(GREEN, 2.0);
                        addRoleHeader(card, entry);
                        if (!entry.subtitle().isBlank()) {
                            card.addParagraph(paragraph -> paragraph
                                    .text(entry.subtitle())
                                    .textStyle(style(BODY_FONT, 7.0,
                                            DocumentTextDecoration.DEFAULT, MUTED))
                                    .margin(DocumentInsets.zero()));
                        }
                        if (!entry.description().isBlank()) {
                            card.addParagraph(paragraph -> paragraph
                                    .text(entry.description())
                                    .textStyle(style(BODY_FONT, 7.25,
                                            DocumentTextDecoration.DEFAULT, INK))
                                    .lineSpacing(1.08)
                                    .margin(DocumentInsets.zero()));
                        }
                    });
                }
            });
        }

        private void addProjects(SectionBuilder parent, CvModule module) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            parent.addSection("EngineeringResumeProjects", section -> {
                addMainHeading(section, "Technical Evidence");
                for (String line : lines.stream().limit(4).toList()) {
                    ProjectEntry project = parseProjectEntry(line);
                    section.addSection("EngineeringResumeProjectCard", card -> card
                            .spacing(3)
                            .padding(new DocumentInsets(5, 8, 5, 8))
                            .fillColor(DocumentColor.WHITE)
                            .stroke(DocumentStroke.of(RULE, 0.3))
                            .cornerRadius(3)
                            .addParagraph(paragraph -> paragraph
                                    .textStyle(style(BODY_FONT, 7.2,
                                            DocumentTextDecoration.DEFAULT, INK))
                                    .lineSpacing(1.06)
                                    .margin(DocumentInsets.zero())
                                    .rich(rich -> {
                                        rich.style(project.title(),
                                                style(BODY_FONT, 7.35,
                                                        DocumentTextDecoration.BOLD, INK));
                                        if (!project.context().isBlank()) {
                                            rich.style(" " + project.context(),
                                                    style(BODY_FONT, 6.85,
                                                            DocumentTextDecoration.DEFAULT, GREEN));
                                        }
                                        rich.style(" - " + project.description(),
                                                style(BODY_FONT, 7.1,
                                                        DocumentTextDecoration.DEFAULT, INK));
                                    })));
                }
            });
        }

        private void addRoleHeader(SectionBuilder card, WorkEntry entry) {
            card.addParagraph(paragraph -> paragraph
                    .textStyle(style(BODY_FONT, 8.0,
                            DocumentTextDecoration.DEFAULT, INK))
                    .margin(DocumentInsets.zero())
                    .rich(rich -> {
                        rich.style(entry.title(),
                                style(BODY_FONT, 8.0,
                                        DocumentTextDecoration.BOLD, INK));
                        if (!entry.date().isBlank()) {
                            rich.style(" / " + entry.date(),
                                    style(BODY_FONT, 7.1,
                                            DocumentTextDecoration.BOLD, GREEN));
                        }
                    }));
        }

        private void addContactStack(SectionBuilder section, CvHeader header) {
            section.spacing(2)
                    .padding(DocumentInsets.zero());
            DocumentTextStyle meta = style(BODY_FONT, 7.2,
                    DocumentTextDecoration.DEFAULT, CONTACT_META);
            DocumentTextStyle link = style(BODY_FONT, 7.2,
                    DocumentTextDecoration.UNDERLINE, CONTACT_LINK);
            for (ContactPart part : contactParts(header)) {
                section.addParagraph(paragraph -> paragraph
                        .text(part.text())
                        .textStyle(part.linkOptions() == null ? meta : link)
                        .link(part.linkOptions())
                        .align(TextAlign.RIGHT)
                        .margin(DocumentInsets.zero()));
            }
        }

        private void addRailHeading(SectionBuilder section, String title) {
            section.spacing(3)
                    .addParagraph(paragraph -> paragraph
                            .text(title.toUpperCase(Locale.ROOT))
                            .textStyle(style(HEADER_FONT, 7.4,
                                    DocumentTextDecoration.BOLD, GREEN))
                            .margin(DocumentInsets.zero()))
                    .addLine(line -> line
                            .horizontal(82)
                            .color(NAVY_SOFT)
                            .thickness(0.8)
                            .margin(DocumentInsets.bottom(2)));
        }

        private void addMainHeading(SectionBuilder section, String title) {
            section.spacing(5)
                    .addParagraph(paragraph -> paragraph
                            .text(title.toUpperCase(Locale.ROOT))
                            .textStyle(style(HEADER_FONT, 7.8,
                                    DocumentTextDecoration.BOLD, GREEN))
                            .margin(DocumentInsets.zero()))
                    .addLine(line -> line
                            .horizontal(176)
                            .color(GREEN)
                            .thickness(1.0)
                            .margin(DocumentInsets.bottom(1)));
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
        switch (body) {
            case ParagraphBlock p -> addLines(lines, p.text());
            case MultiParagraphBlock m -> m.paragraphs().forEach(line -> addLines(lines, line));
            case BulletListBlock b -> b.items().forEach(item -> addLines(lines, item));
            case IndentedBlock i -> i.items().forEach(item -> {
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
            case KeyValueBlock kv -> kv.entries().forEach(entry ->
                    addLines(lines, entry.key() + ": " + entry.value()));
            default -> {
                // ignore other block kinds
            }
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

    private static String compactValues(String value, int maxItems) {
        String[] tokens = stripMarkdown(value).split(",");
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (String token : tokens) {
            String clean = token.trim();
            if (clean.isBlank()) {
                continue;
            }
            if (count > 0) {
                builder.append(", ");
            }
            builder.append(clean);
            count++;
            if (count == maxItems) {
                break;
            }
        }
        return builder.toString();
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

    private record WorkEntry(String title, String subtitle, String date, String description) {
    }

    private record ProjectEntry(String title, String context, String description) {
    }
}
