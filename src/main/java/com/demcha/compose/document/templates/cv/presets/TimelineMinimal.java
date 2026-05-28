package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineImageAlignment;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Templates v2 "Timeline Minimal" CV preset.
 *
 * <p>Minimal two-column CV with a vertical timeline axis between the
 * sidebar (Education / Skills / Expertise / Languages / Interests /
 * References) and the main column (Professional Profile / Work
 * Experience). Visual signature ported from the legacy
 * {@code TimelineMinimalCvTemplateComposer}: spaced caps name in
 * Barlow Condensed, contact stack with PNG icons, all-grey palette,
 * three timeline dots.</p>
 *
 * @deprecated Superseded by the layered <code>…v2…</code> surface (the current
 *             standard). Kept for backward compatibility; scheduled for removal
 *             in a future major. See {@code docs/templates/v2-layered/} and
 *             {@link com.demcha.compose.document.templates.cv.v2.presets.TimelineMinimal}.
 */
@Deprecated(since = "1.7.0", forRemoval = true)
public final class TimelineMinimal {

    /** Stable template identifier. */
    public static final String ID = "timeline-minimal";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Timeline Minimal";

    /** Recommended page margin (in points) — matches V1 TimelineMinimal gallery. */
    public static final double RECOMMENDED_MARGIN = 22.0;

    // V1 TimelineMinimalCvTemplateComposer palette tokens.
    private static final DocumentColor INK = DocumentColor.rgb(74, 74, 74);
    private static final DocumentColor SOFT = DocumentColor.rgb(122, 122, 122);
    private static final DocumentColor RULE = DocumentColor.rgb(195, 195, 195);
    private static final DocumentColor DOT = DocumentColor.rgb(170, 170, 170);

    private static final double TIMELINE_DOT = 7.0;
    private static final double TIMELINE_LINE_BOX = 1.0;
    private static final double TIMELINE_LINE_OFFSET = (TIMELINE_DOT - TIMELINE_LINE_BOX) / 2.0;

    private static final double CONTACT_ICON_SIZE = 10.5;
    private static final double CONTACT_ICON_BASELINE_OFFSET = -1.35;
    private static final String CONTACT_ICON_ROOT = "/templates/cv/timeline-minimal/icons/";
    private static final Map<String, byte[]> CONTACT_ICON_CACHE = new ConcurrentHashMap<>();

    private TimelineMinimal() {
        // utility class — not instantiable
    }

    /**
     * Builds the {@code Timeline Minimal} template.
     *
     * @param theme active business theme; the preset overrides palette
     *              and typography to V1 TimelineMinimal tokens
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new TimelineMinimalTemplate();
    }

    private static final class TimelineMinimalTemplate implements DocumentTemplate<CvSpec> {

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

            double width = document.canvas().innerWidth();

            document.dsl()
                    .pageFlow()
                    .name("TimelineMinimalRoot")
                    .spacing(12)
                    .addRow("TimelineMinimalHeader", row -> row
                            .spacing(3)
                            .weights(1.00, 0.61)
                            .addSection("TimelineMinimalName", section -> section
                                    .spacing(4)
                                    .addParagraph(paragraph -> paragraph
                                            .text(spacedUpper(name(spec.header())))
                                            .textStyle(style(FontName.BARLOW_CONDENSED, 28,
                                                    DocumentTextDecoration.DEFAULT, INK))
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(paragraph -> paragraph
                                            .text("PROFESSIONAL TITLE")
                                            .textStyle(style(FontName.BARLOW_CONDENSED, 9.5,
                                                    DocumentTextDecoration.BOLD, INK))
                                            .margin(DocumentInsets.zero())))
                            .addSection("TimelineMinimalContact",
                                    section -> addContact(section, spec.header())))
                    .addLine(line -> line
                            .name("TimelineMinimalHeaderRule")
                            .horizontal(width)
                            .color(RULE)
                            .thickness(0.8)
                            .margin(DocumentInsets.zero()))
                    .addRow("TimelineMinimalBody", row -> addBodyRow(row,
                            List.of(
                                    new ModulePlacement("Education",
                                            findModule(spec, "education", "certifications"), 5),
                                    new ModulePlacement("Skills",
                                            findModule(spec, "technical skills", "skills"), 6),
                                    new ModulePlacement("Expertise",
                                            findModule(spec, "projects"), 3),
                                    new ModulePlacement("Languages",
                                            findModule(spec, "additional information", "additional"), 3),
                                    new ModulePlacement("Interests",
                                            findModule(spec, "interests", "additional"), 4),
                                    new ModulePlacement("References",
                                            findModule(spec, "references", "contact"), 5)),
                            List.of(
                                    new ModulePlacement("Professional Profile",
                                            findModule(spec, "summary", "professional summary", "profile"), 1),
                                    new ModulePlacement("Work Experience",
                                            findModule(spec, "experience", "employment"), 4)),
                            620))
                    .build();
        }

        private void addBodyRow(RowBuilder row,
                                List<ModulePlacement> sidebarModules,
                                List<ModulePlacement> mainModules,
                                double axisHeight) {
            row.spacing(16)
                    .weights(0.74, 0.12, 1.74)
                    .addSection("TimelineMinimalSidebar", sidebar -> {
                        sidebar.spacing(10);
                        for (ModulePlacement placement : sidebarModules) {
                            addSidebarModule(sidebar, placement.title(),
                                    placement.module(), placement.limit());
                        }
                    })
                    .addSection("TimelineMinimalAxis", axis -> addTimelineAxis(axis, axisHeight))
                    .addSection("TimelineMinimalMain", main -> {
                        main.spacing(11);
                        for (ModulePlacement placement : mainModules) {
                            boolean bullets = placement.limit() > 1;
                            addMainModule(main, placement.title(), placement.module(),
                                    bullets, placement.limit());
                        }
                    });
        }

        private void addContact(SectionBuilder section, CvHeader header) {
            section.spacing(3);
            DocumentTextStyle textStyle = style(FontName.LATO, 7.8,
                    DocumentTextDecoration.BOLD, SOFT);
            DocumentTextStyle fallbackIconStyle = style(FontName.BARLOW_CONDENSED, 8.0,
                    DocumentTextDecoration.BOLD, SOFT);
            for (ContactLine line : contactLines(header)) {
                section.addParagraph(paragraph -> paragraph
                        .textStyle(textStyle)
                        .align(TextAlign.RIGHT)
                        .link(line.linkOptions())
                        .margin(DocumentInsets.zero())
                        .rich(rich -> {
                            rich.style(line.text(), textStyle);
                            rich.plain("  ");
                            if (line.iconFile() != null) {
                                rich.image(
                                        contactIcon(line.iconFile()),
                                        CONTACT_ICON_SIZE,
                                        CONTACT_ICON_SIZE,
                                        InlineImageAlignment.CENTER,
                                        CONTACT_ICON_BASELINE_OFFSET,
                                        line.linkOptions());
                            } else {
                                rich.style(line.fallbackIcon(), fallbackIconStyle);
                            }
                        }));
            }
        }

        private List<ContactLine> contactLines(CvHeader header) {
            if (header == null) {
                return List.of();
            }
            List<ContactLine> lines = new ArrayList<>();
            addContactLine(lines, "LOC", "location.png",
                    safe(header.address()), null);
            addContactLine(lines, "TEL", "phone.png",
                    safe(header.phone()), null);
            String email = safe(header.email());
            if (!email.isBlank()) {
                addContactLine(lines, "@", "email.png", email,
                        new DocumentLinkOptions("mailto:" + email));
            }
            for (CvHeader.Link link : header.links()) {
                String label = safe(link.label());
                if (label.isBlank()) {
                    continue;
                }
                String url = safe(link.url());
                String iconFile = pickIconFile(label);
                String fallback = pickFallbackIcon(label);
                addContactLine(lines, fallback, iconFile, label,
                        url.isBlank() ? null : new DocumentLinkOptions(url.trim()));
            }
            return List.copyOf(lines);
        }

        private void addContactLine(List<ContactLine> lines,
                                    String fallbackIcon,
                                    String iconFile,
                                    String text,
                                    DocumentLinkOptions linkOptions) {
            if (text != null && !text.isBlank()) {
                lines.add(new ContactLine(fallbackIcon, iconFile, text, linkOptions));
            }
        }

        private DocumentImageData contactIcon(String iconFile) {
            return DocumentImageData.fromBytes(
                    CONTACT_ICON_CACHE.computeIfAbsent(iconFile, TimelineMinimal::readIconBytes));
        }

        private void addSidebarModule(SectionBuilder sidebar, String title,
                                      CvModule module, int limit) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            sidebar.addSection("TimelineMinimalSidebar" + normalize(title), section -> {
                section.spacing(6)
                        .addParagraph(paragraph -> paragraph
                                .text(title.toUpperCase(Locale.ROOT))
                                .textStyle(style(FontName.BARLOW_CONDENSED, 12.5,
                                        DocumentTextDecoration.BOLD, INK))
                                .margin(DocumentInsets.zero()));
                for (String line : lines.stream().limit(limit).toList()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(excerpt(line, 76))
                            .textStyle(style(FontName.LATO, 7.5,
                                    DocumentTextDecoration.BOLD, INK))
                            .lineSpacing(1)
                            .margin(DocumentInsets.zero()));
                }
                section.addLine(line -> line
                        .horizontal(118)
                        .color(RULE)
                        .thickness(0.65)
                        .margin(DocumentInsets.top(5)));
            });
        }

        private void addTimelineAxis(SectionBuilder axis, double height) {
            double segment = height / 4;
            axis.spacing(0)
                    .padding(new DocumentInsets(28, 0, 0, 0))
                    .addLine(line -> timelineLine(line, segment))
                    .addCircle(TIMELINE_DOT, circle -> circle
                            .stroke(DocumentStroke.of(DOT, 0.8))
                            .fillColor(DocumentColor.WHITE))
                    .addLine(line -> timelineLine(line, segment))
                    .addCircle(TIMELINE_DOT, circle -> circle
                            .stroke(DocumentStroke.of(DOT, 0.8))
                            .fillColor(DocumentColor.WHITE))
                    .addLine(line -> timelineLine(line, segment))
                    .addCircle(TIMELINE_DOT, circle -> circle
                            .stroke(DocumentStroke.of(DOT, 0.8))
                            .fillColor(DocumentColor.WHITE))
                    .addLine(line -> timelineLine(line, segment));
        }

        private void timelineLine(LineBuilder line, double height) {
            line.vertical(height)
                    .color(RULE)
                    .thickness(0.75)
                    .margin(new DocumentInsets(0, 0, 0, TIMELINE_LINE_OFFSET));
        }

        private void addMainModule(SectionBuilder main, String title,
                                   CvModule module, boolean bullets, int limit) {
            List<String> lines = moduleLines(module);
            if (lines.isEmpty()) {
                return;
            }
            main.addSection("TimelineMinimalMain" + normalize(title), section -> {
                section.spacing(5)
                        .addParagraph(paragraph -> paragraph
                                .text(title.toUpperCase(Locale.ROOT))
                                .textStyle(style(FontName.BARLOW_CONDENSED, 13.5,
                                        DocumentTextDecoration.BOLD, INK))
                                .margin(DocumentInsets.zero()));
                if (bullets) {
                    for (String line : lines.stream().limit(limit).toList()) {
                        section.addParagraph(paragraph -> paragraph
                                .text(excerpt(line, 136))
                                .textStyle(style(FontName.LATO, 7.8,
                                        DocumentTextDecoration.DEFAULT, INK))
                                .lineSpacing(1.2)
                                .bulletOffset("-")
                                .margin(DocumentInsets.zero()));
                    }
                } else {
                    section.addParagraph(paragraph -> paragraph
                            .text(excerpt(lines.get(0), 245))
                            .textStyle(style(FontName.LATO, 7.9,
                                    DocumentTextDecoration.DEFAULT, INK))
                            .lineSpacing(1.4)
                            .margin(DocumentInsets.zero()));
                }
                section.addLine(line -> line
                        .horizontal(300)
                        .color(RULE)
                        .thickness(0.65)
                        .margin(DocumentInsets.top(6)));
            });
        }
    }

    // -- helpers ---------------------------------------------------------

    private static byte[] readIconBytes(String iconFile) {
        try (InputStream input = TimelineMinimal.class.getResourceAsStream(
                CONTACT_ICON_ROOT + iconFile)) {
            if (input == null) {
                throw new IllegalStateException("Missing timeline minimal contact icon: " + iconFile);
            }
            return input.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read timeline minimal contact icon: " + iconFile, e);
        }
    }

    private static String pickIconFile(String label) {
        String n = normalize(label);
        if (n.contains("linkedin")) {
            return "linkedin.png";
        }
        if (n.contains("github")) {
            return "github.png";
        }
        if (n.contains("dribbble")) {
            return "dribbble.png";
        }
        if (n.contains("google")) {
            return "google.png";
        }
        return null;
    }

    private static String pickFallbackIcon(String label) {
        String n = normalize(label);
        if (n.contains("linkedin")) {
            return "in";
        }
        if (n.contains("github")) {
            return "GH";
        }
        return "@";
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
            String haystack = normalize(safe(module.name()) + " " + safe(module.title()));
            for (String key : keys) {
                if (haystack.contains(normalize(key))) {
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

    private static String name(CvHeader header) {
        return header == null ? "" : safe(header.name());
    }

    private static String spacedUpper(String value) {
        String upper = safe(value).toUpperCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < upper.length(); i++) {
            char current = upper.charAt(i);
            builder.append(current);
            if (Character.isWhitespace(current)) {
                builder.append("  ");
            }
            if (Character.isLetter(current) && i + 1 < upper.length()
                    && Character.isLetter(upper.charAt(i + 1))) {
                builder.append(' ');
            }
        }
        return builder.toString();
    }

    private static String stripMarkdown(String value) {
        return safe(value)
                .replace("**", "")
                .replace("*", "")
                .replace("`", "")
                .replace("_", "");
    }

    private static String excerpt(String value, int maxChars) {
        String clean = stripMarkdown(value).replaceAll("\\s+", " ").trim();
        if (clean.length() <= maxChars) {
            return clean;
        }
        int boundary = clean.lastIndexOf(' ', maxChars - 1);
        int end = boundary > maxChars / 2 ? boundary : maxChars - 1;
        return clean.substring(0, end).trim() + "...";
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

    private record ModulePlacement(String title, CvModule module, int limit) {
    }

    private record ContactLine(String fallbackIcon, String iconFile, String text,
                               DocumentLinkOptions linkOptions) {
    }
}
