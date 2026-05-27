package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.components.CvTextStyles;
import com.demcha.compose.document.templates.cv.v2.components.MarkdownInline;
import com.demcha.compose.document.templates.cv.v2.components.SectionLookup;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.data.CvEntry;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.data.CvLink;
import com.demcha.compose.document.templates.cv.v2.data.CvRow;
import com.demcha.compose.document.templates.cv.v2.data.CvSection;
import com.demcha.compose.document.templates.cv.v2.data.EntriesSection;
import com.demcha.compose.document.templates.cv.v2.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.v2.data.RowsSection;
import com.demcha.compose.document.templates.cv.v2.data.SkillGroup;
import com.demcha.compose.document.templates.cv.v2.data.SkillsSection;
import com.demcha.compose.document.templates.cv.v2.data.Slot;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.widgets.TimelineAxisWidget;

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
 * v2 port of the legacy "Timeline Minimal" CV preset.
 *
 * <p>Minimal two-column CV with a vertical timeline axis between the
 * sidebar (Education / Skills / Expertise / Languages / Interests /
 * References) and the main column (Professional Profile / Work
 * Experience). Visual signature ported from the v1
 * {@code TimelineMinimalCvTemplateComposer}: spaced caps name in
 * Barlow Condensed, contact stack with PNG icons, all-grey palette,
 * three timeline dots between four axis segments.</p>
 *
 * <p>The preset stays a thin orchestrator. The 3-column body layout
 * (sidebar / axis / main) and the contact icon row are preset-local
 * because no other v2 preset uses this visual today. Section bodies
 * are flattened to a list of lines via a preset-local helper so the
 * sidebar can apply per-module truncation limits — the canonical
 * shared dispatchers do not enforce that shape.</p>
 */
public final class TimelineMinimal {

    /** Stable template identifier. */
    public static final String ID = "timeline-minimal";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Timeline Minimal";

    /** Recommended page margin (in points) — matches V1 TimelineMinimal. */
    public static final double RECOMMENDED_MARGIN = 22.0;

    /** Diameter of each timeline marker; 4 segments + 3 markers by default. */
    private static final double TIMELINE_DOT = 7.0;

    /** Default total axis height — sized for a one-page CV. */
    private static final double TIMELINE_AXIS_HEIGHT = 620.0;

    /** Top inset before the first axis segment starts. */
    private static final double TIMELINE_TOP_PADDING = 28.0;

    /** Number of vertical line segments; markers between = segmentCount - 1. */
    private static final int TIMELINE_SEGMENT_COUNT = 4;

    /** Stroke thickness of every line segment. */
    private static final double TIMELINE_LINE_THICKNESS = 0.75;

    /** Stroke thickness of the marker outline. */
    private static final double TIMELINE_MARKER_STROKE = 0.8;

    private static final double CONTACT_ICON_SIZE = 10.5;
    private static final double CONTACT_ICON_BASELINE_OFFSET = -1.35;
    private static final String CONTACT_ICON_ROOT =
            "/templates/cv/timeline-minimal/icons/";
    private static final Map<String, byte[]> CONTACT_ICON_CACHE =
            new ConcurrentHashMap<>();

    private static final List<String> SUMMARY_KEYS =
            List.of("summary", "professional summary", "profile");
    private static final List<String> SKILL_KEYS =
            List.of("technical skills", "skills");
    private static final List<String> EDUCATION_KEYS =
            List.of("education", "certifications");
    private static final List<String> EXPERIENCE_KEYS =
            List.of("experience", "professional experience", "employment", "work");
    private static final List<String> PROJECT_KEYS =
            List.of("projects", "project");
    private static final List<String> ADDITIONAL_KEYS =
            List.of("additional information", "additional");

    private TimelineMinimal() {
    }

    /**
     * Builds the preset with its Timeline Minimal theme.
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.timelineMinimal());
    }

    /**
     * Builds the preset with a caller-supplied theme.
     */
    public static DocumentTemplate<CvDocument> create(CvTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new Template(theme);
    }

    private static final class Template implements DocumentTemplate<CvDocument> {

        private final CvTheme theme;

        Template(CvTheme theme) {
            this.theme = theme;
        }

        @Override
        public String id() {
            return ID;
        }

        @Override
        public String displayName() {
            return DISPLAY_NAME;
        }

        @Override
        public void compose(DocumentSession document, CvDocument doc) {
            Objects.requireNonNull(document, "document");
            Objects.requireNonNull(doc, "doc");

            double width = document.canvas().innerWidth();
            List<CvSection> sections = doc.sectionsIn(Slot.MAIN);

            document.dsl()
                    .pageFlow()
                    .name("CvV2TimelineMinimalRoot")
                    .spacing(theme.spacing().pageFlowSpacing())
                    .addRow("CvV2TimelineMinimalHeader", row -> row
                            .spacing(3)
                            .weights(1.00, 0.61)
                            .addSection("CvV2TimelineMinimalName",
                                    section -> addNameBlock(section, doc.identity()))
                            .addSection("CvV2TimelineMinimalContact",
                                    section -> addContact(section, doc.identity())))
                    .addLine(line -> line
                            .name("CvV2TimelineMinimalHeaderRule")
                            .horizontal(width)
                            .color(theme.palette().rule())
                            .thickness(theme.spacing().accentRuleWidth())
                            .margin(DocumentInsets.zero()))
                    .addRow("CvV2TimelineMinimalBody", row -> addBodyRow(row,
                            List.of(
                                    new ModulePlacement("Education",
                                            SectionLookup.firstMatching(sections,
                                                    EDUCATION_KEYS),
                                            5),
                                    new ModulePlacement("Skills",
                                            SectionLookup.firstMatching(sections,
                                                    SKILL_KEYS),
                                            6),
                                    new ModulePlacement("Expertise",
                                            SectionLookup.firstMatching(sections,
                                                    PROJECT_KEYS),
                                            3),
                                    new ModulePlacement("Languages",
                                            SectionLookup.firstMatching(sections,
                                                    ADDITIONAL_KEYS),
                                            3)),
                            List.of(
                                    new ModulePlacement("Professional Profile",
                                            SectionLookup.firstMatching(sections,
                                                    SUMMARY_KEYS),
                                            1),
                                    new ModulePlacement("Work Experience",
                                            SectionLookup.firstMatching(sections,
                                                    EXPERIENCE_KEYS),
                                            4)),
                            TIMELINE_AXIS_HEIGHT))
                    .build();
        }

        private void addNameBlock(SectionBuilder section, CvIdentity identity) {
            section.spacing(4)
                    .addParagraph(paragraph -> paragraph
                            .text(spacedUpper(identity.name().full()))
                            .textStyle(nameStyle())
                            .margin(DocumentInsets.zero()));
            String jobTitle = identity.jobTitle();
            if (!jobTitle.isBlank()) {
                section.addParagraph(paragraph -> paragraph
                        .text(jobTitle.toUpperCase(Locale.ROOT))
                        .textStyle(jobTitleStyle())
                        .margin(DocumentInsets.zero()));
            }
        }

        private void addBodyRow(RowBuilder row,
                                List<ModulePlacement> sidebarModules,
                                List<ModulePlacement> mainModules,
                                double axisHeight) {
            row.spacing(16)
                    .weights(0.74, 0.12, 1.74)
                    .addSection("CvV2TimelineMinimalSidebar", sidebar -> {
                        sidebar.spacing(10);
                        for (ModulePlacement placement : sidebarModules) {
                            addSidebarModule(sidebar, placement.title(),
                                    placement.section(), placement.limit());
                        }
                    })
                    .addSection("CvV2TimelineMinimalAxis", axis ->
                            TimelineAxisWidget.render(axis,
                                    timelineAxisStyle(), axisHeight))
                    .addSection("CvV2TimelineMinimalMain", main -> {
                        main.spacing(11);
                        for (ModulePlacement placement : mainModules) {
                            boolean bullets = placement.limit() > 1;
                            addMainModule(main, placement.title(),
                                    placement.section(), bullets, placement.limit());
                        }
                    });
        }

        private void addContact(SectionBuilder section, CvIdentity identity) {
            section.spacing(3);
            DocumentTextStyle textStyle = contactTextStyle();
            DocumentTextStyle fallbackIconStyle = fallbackIconStyle();
            for (ContactItem item : contactItems(identity)) {
                section.addParagraph(paragraph -> paragraph
                        .textStyle(textStyle)
                        .align(TextAlign.RIGHT)
                        .link(item.linkOptions())
                        .margin(DocumentInsets.zero())
                        .rich(rich -> {
                            rich.style(item.text(), textStyle);
                            rich.plain("  ");
                            if (item.iconFile() != null) {
                                rich.image(contactIcon(item.iconFile()),
                                        CONTACT_ICON_SIZE,
                                        CONTACT_ICON_SIZE,
                                        InlineImageAlignment.CENTER,
                                        CONTACT_ICON_BASELINE_OFFSET,
                                        item.linkOptions());
                            } else {
                                rich.style(item.fallbackIcon(),
                                        fallbackIconStyle);
                            }
                        }));
            }
        }

        private List<ContactItem> contactItems(CvIdentity identity) {
            if (identity == null) {
                return List.of();
            }
            List<ContactItem> items = new ArrayList<>();
            addContactItem(items, "LOC", "location.png",
                    identity.contact().address(), null);
            addContactItem(items, "TEL", "phone.png",
                    identity.contact().phone(), null);
            String email = identity.contact().email();
            if (!email.isBlank()) {
                addContactItem(items, "@", "email.png", email,
                        new DocumentLinkOptions("mailto:" + email));
            }
            for (CvLink link : identity.links()) {
                String label = link.label();
                if (label.isBlank()) {
                    continue;
                }
                String url = link.url();
                addContactItem(items, pickFallbackIcon(label),
                        pickIconFile(label), label,
                        url.isBlank()
                                ? null
                                : new DocumentLinkOptions(url.trim()));
            }
            return List.copyOf(items);
        }

        private static void addContactItem(List<ContactItem> items,
                                            String fallbackIcon,
                                            String iconFile,
                                            String text,
                                            DocumentLinkOptions linkOptions) {
            if (text != null && !text.isBlank()) {
                items.add(new ContactItem(fallbackIcon, iconFile, text,
                        linkOptions));
            }
        }

        private DocumentImageData contactIcon(String iconFile) {
            return DocumentImageData.fromBytes(
                    CONTACT_ICON_CACHE.computeIfAbsent(iconFile,
                            TimelineMinimal::readIconBytes));
        }

        private void addSidebarModule(SectionBuilder sidebar, String title,
                                      CvSection section, int limit) {
            List<String> lines = sectionLines(section);
            if (lines.isEmpty()) {
                return;
            }
            sidebar.addSection("CvV2TimelineMinimalSidebar"
                    + SectionLookup.normalize(title), block -> {
                block.spacing(6)
                        .addParagraph(paragraph -> paragraph
                                .text(title.toUpperCase(Locale.ROOT))
                                .textStyle(sidebarTitleStyle())
                                .margin(DocumentInsets.zero()));
                for (String line : lines.stream().limit(limit).toList()) {
                    block.addParagraph(paragraph -> paragraph
                            .text(excerpt(line, 76))
                            .textStyle(sidebarBodyStyle())
                            .lineSpacing(1)
                            .margin(DocumentInsets.zero()));
                }
                block.addLine(line -> line
                        .horizontal(118)
                        .color(theme.palette().rule())
                        .thickness(0.65)
                        .margin(DocumentInsets.top(5)));
            });
        }

        /**
         * Style applied to the central timeline axis. Drop a custom
         * {@link TimelineAxisWidget.Style} here (or expose it through
         * the theme) to swap the marker shape, sizing, or colours.
         */
        private TimelineAxisWidget.Style timelineAxisStyle() {
            return TimelineAxisWidget.Style.builder()
                    .marker(TimelineAxisWidget.Marker.CIRCLE)
                    .markerSize(TIMELINE_DOT)
                    .markerStroke(DocumentStroke.of(
                            theme.palette().banner(),
                            TIMELINE_MARKER_STROKE))
                    .segmentCount(TIMELINE_SEGMENT_COUNT)
                    .lineColor(theme.palette().rule())
                    .lineThickness(TIMELINE_LINE_THICKNESS)
                    .padding(new DocumentInsets(TIMELINE_TOP_PADDING, 0, 0, 0))
                    .build();
        }

        private void addMainModule(SectionBuilder main, String title,
                                   CvSection section, boolean bullets,
                                   int limit) {
            List<String> lines = sectionLines(section);
            if (lines.isEmpty()) {
                return;
            }
            main.addSection("CvV2TimelineMinimalMain"
                    + SectionLookup.normalize(title), block -> {
                block.spacing(5)
                        .addParagraph(paragraph -> paragraph
                                .text(title.toUpperCase(Locale.ROOT))
                                .textStyle(mainTitleStyle())
                                .margin(DocumentInsets.zero()));
                if (bullets) {
                    for (String line : lines.stream().limit(limit).toList()) {
                        block.addParagraph(paragraph -> paragraph
                                .text(excerpt(line, 136))
                                .textStyle(mainBulletStyle())
                                .lineSpacing(1.2)
                                .bulletOffset("-")
                                .margin(DocumentInsets.zero()));
                    }
                } else {
                    block.addParagraph(paragraph -> paragraph
                            .text(excerpt(lines.get(0), 245))
                            .textStyle(mainBodyStyle())
                            .lineSpacing(1.4)
                            .margin(DocumentInsets.zero()));
                }
                block.addLine(line -> line
                        .horizontal(300)
                        .color(theme.palette().rule())
                        .thickness(0.65)
                        .margin(DocumentInsets.top(6)));
            });
        }

        // -- style factories ---------------------------------------------

        private DocumentTextStyle nameStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    theme.typography().sizeHeadline(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }

        private DocumentTextStyle jobTitleStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    9.5,
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle contactTextStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.BOLD,
                    theme.palette().muted());
        }

        private DocumentTextStyle fallbackIconStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    8.0,
                    DocumentTextDecoration.BOLD,
                    theme.palette().muted());
        }

        private DocumentTextStyle sidebarTitleStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    theme.typography().sizeEntryTitle(),
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle sidebarBodyStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeEntrySubtitle(),
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle mainTitleStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    theme.typography().sizeBanner(),
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle mainBulletStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeBody(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }

        private DocumentTextStyle mainBodyStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeEntryDate(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }
    }

    // -- helpers -----------------------------------------------------------

    /**
     * Flattens a {@link CvSection} into a list of single-line strings
     * suitable for the truncation-driven sidebar / main rendering. v2
     * {@code SectionDispatcher} would produce richly-styled multi-paragraph
     * output, which is not what Timeline Minimal needs — its layout
     * relies on knowing the exact line count so per-module
     * {@code limit} can drop overflow without breaking the visual flow
     * around the fixed-height timeline axis.
     */
    private static List<String> sectionLines(CvSection section) {
        if (section == null || !SectionLookup.hasContent(section)) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        if (section instanceof ParagraphSection paragraph) {
            addLines(lines, paragraph.body());
        } else if (section instanceof SkillsSection skills) {
            for (SkillGroup group : skills.groups()) {
                String label = group.category();
                String body = group.skillsInline();
                if (label.isBlank() && body.isBlank()) {
                    continue;
                }
                if (label.isBlank()) {
                    lines.add(body);
                } else if (body.isBlank()) {
                    lines.add(label);
                } else {
                    lines.add(label + ": " + body);
                }
            }
        } else if (section instanceof EntriesSection entries) {
            for (CvEntry entry : entries.entries()) {
                String header = entry.title();
                String subtitle = entry.subtitle();
                String dates = entry.date();
                String body = entry.body();
                StringBuilder line = new StringBuilder();
                if (!header.isBlank()) {
                    line.append(header);
                }
                if (!subtitle.isBlank()) {
                    if (!line.isEmpty()) {
                        line.append(" | ");
                    }
                    line.append(subtitle);
                }
                if (!dates.isBlank()) {
                    if (!line.isEmpty()) {
                        line.append(" - ");
                    }
                    line.append(dates);
                }
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                }
                if (!body.isBlank()) {
                    addLines(lines, body);
                }
            }
        } else if (section instanceof RowsSection rows) {
            for (CvRow row : rows.rows()) {
                String label = row.label();
                String body = row.body();
                if (label.isBlank() && body.isBlank()) {
                    continue;
                }
                if (label.isBlank()) {
                    lines.add(body);
                } else if (body.isBlank()) {
                    lines.add(label);
                } else {
                    lines.add(label + ": " + body);
                }
            }
        }
        return List.copyOf(lines);
    }

    private static void addLines(List<String> lines, String value) {
        for (String line : safe(value).split("\\R")) {
            String clean = MarkdownInline.plainText(line).trim();
            if (!clean.isBlank()) {
                lines.add(clean);
            }
        }
    }

    private static byte[] readIconBytes(String iconFile) {
        try (InputStream input = TimelineMinimal.class.getResourceAsStream(
                CONTACT_ICON_ROOT + iconFile)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Missing timeline minimal contact icon: " + iconFile);
            }
            return input.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read timeline minimal contact icon: " + iconFile,
                    e);
        }
    }

    private static String pickIconFile(String label) {
        String normalized = SectionLookup.normalize(label);
        if (normalized.contains("linkedin")) {
            return "linkedin.png";
        }
        if (normalized.contains("github")) {
            return "github.png";
        }
        if (normalized.contains("dribbble")) {
            return "dribbble.png";
        }
        if (normalized.contains("google")) {
            return "google.png";
        }
        return null;
    }

    private static String pickFallbackIcon(String label) {
        String normalized = SectionLookup.normalize(label);
        if (normalized.contains("linkedin")) {
            return "in";
        }
        if (normalized.contains("github")) {
            return "GH";
        }
        return "@";
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

    private static String excerpt(String value, int maxChars) {
        String clean = MarkdownInline.plainText(value)
                .replaceAll("\\s+", " ").trim();
        if (clean.length() <= maxChars) {
            return clean;
        }
        int boundary = clean.lastIndexOf(' ', maxChars - 1);
        int end = boundary > maxChars / 2 ? boundary : maxChars - 1;
        return clean.substring(0, end).trim() + "...";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record ModulePlacement(String title, CvSection section, int limit) {
    }

    private record ContactItem(String fallbackIcon, String iconFile,
                               String text, DocumentLinkOptions linkOptions) {
    }
}
