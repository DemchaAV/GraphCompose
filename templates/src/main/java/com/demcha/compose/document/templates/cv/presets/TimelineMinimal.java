package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.templates.core.identity.Link;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.text.TextStyles;
import com.demcha.compose.document.templates.core.text.MarkdownInline;
import com.demcha.compose.document.templates.cv.components.SectionAllocation;
import com.demcha.compose.document.templates.cv.components.SectionLookup;
import com.demcha.compose.document.templates.cv.data.*;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.core.identity.SvgGlyph;
import com.demcha.compose.document.templates.core.widgets.TimelineAxisWidget;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
 * are flattened to a list of lines via a preset-local helper, which is
 * the shape the two narrow columns and the fixed-height axis need — the
 * canonical shared dispatchers produce richly-styled multi-paragraph
 * output instead.</p>
 *
 * <h2>Nothing the CV carries is dropped</h2>
 *
 * <p>Sections are matched to modules by title keyword, and a keyword match
 * is a guess: a CV may hold a category this preset never heard of, or two
 * that answer to the same keywords. Both are routed rather than discarded —
 * {@link SectionAllocation} hands each section out once and returns the
 * rest, which land in the main column under their own headings. A module's
 * heading is the section's own title where it has one; the labels here are
 * only a fallback.</p>
 *
 * <p>Content longer than a page continues on the next one. The body is a
 * row, and a row is atomic — the paginator cannot break inside one — so the
 * preset estimates its own columns' heights through {@link ColumnPagination}
 * and emits one row per page, letting each finished row overflow naturally.
 * What it holds back for the masthead is measured from the identity, because
 * the contact stack grows a row per link.</p>
 */
public final class TimelineMinimal {

    /**
     * Stable template identifier.
     */
    public static final String ID = "timeline-minimal";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Timeline Minimal";

    /**
     * Recommended page margin (in points) — matches V1 TimelineMinimal.
     */
    public static final double RECOMMENDED_MARGIN = 22.0;

    /**
     * Diameter of each timeline marker; 4 segments + 3 markers by default.
     */
    private static final double TIMELINE_DOT = 7.0;

    /**
     * Default total axis height — sized for a one-page CV.
     */
    private static final double TIMELINE_AXIS_HEIGHT = 620.0;

    /**
     * Top inset before the first axis segment starts.
     */
    private static final double TIMELINE_TOP_PADDING = 28.0;

    /**
     * Number of vertical line segments; markers between = segmentCount - 1.
     */
    private static final int TIMELINE_SEGMENT_COUNT = 4;

    /**
     * Stroke thickness of every line segment.
     */
    private static final double TIMELINE_LINE_THICKNESS = 0.75;

    /**
     * Stroke thickness of the marker outline.
     */
    private static final double TIMELINE_MARKER_STROKE = 0.8;

    private static final double CONTACT_ICON_SIZE = 10.5;
    private static final double CONTACT_ICON_BASELINE_OFFSET = -1.35;
    private static final String CONTACT_ICON_ROOT =
            "/templates/cv/timeline-minimal/icons/";

    /**
     * Contact glyph fill — a dark slate that reads clearly on the white page.
     * Recolours the shared {@link SvgGlyph} silhouettes via
     * {@code rich.shape(...)}.
     */
    private static final DocumentColor ICON_COLOR = DocumentColor.rgb(58, 58, 58);

    /**
     * Shortest axis a continuation page may draw. Below this the four
     * segments and three markers stop reading as a timeline.
     */
    private static final double MIN_CONTINUATION_AXIS_HEIGHT = 120.0;

    /** Gap between the three body columns; also the row's own spacing. */
    private static final double BODY_COLUMN_GAP = 16.0;

    private static final double SIDEBAR_WEIGHT = 0.74;
    private static final double AXIS_WEIGHT = 0.12;
    private static final double MAIN_WEIGHT = 1.74;

    /** Gap between the stacked contact rows. */
    private static final double CONTACT_ROW_GAP = 3.0;

    /** Gap between the name and the job title beneath it. */
    private static final double NAME_BLOCK_GAP = 4.0;

    /** Job-title size; read both by the style and by the masthead measurement. */
    private static final double JOB_TITLE_SIZE = 9.5;

    /**
     * Slack added to the measured masthead height.
     *
     * <p>Covers what the arithmetic below does not model — a contact line long
     * enough to wrap, the rule's own thickness. An over-estimate costs
     * whitespace at the foot of a page; an under-estimate costs
     * {@code AtomicNodeTooLargeException}, because a row cannot be broken by
     * the paginator, so the error is deliberately taken on the safe side.</p>
     */
    private static final double MASTHEAD_SLACK = 18.0;

    /** Heading gap plus the trailing rule each sidebar block carries. */
    private static final double SIDEBAR_BLOCK_GAP = 22.0;

    /** Heading gap plus the trailing rule each main-column block carries. */
    private static final double MAIN_BLOCK_GAP = 24.0;

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
     *
     * @return ready-to-use template
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(BrandTheme.timelineMinimal());
    }

    /**
     * Builds the preset with a caller-supplied theme.
     *
     * @param theme active theme
     * @return ready-to-use template
     */
    public static DocumentTemplate<CvDocument> create(BrandTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new Template(theme);
    }

    private record Template(BrandTheme theme) implements DocumentTemplate<CvDocument> {

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
                SectionAllocation allocation =
                        SectionAllocation.of(doc.sectionsIn(Slot.MAIN));

                // Claim order decides which module wins when two lists could
                // match the same title, and every claim removes the section
                // from what falls through to the main column below.
                CvSection education = allocation.claim(SectionRole.EDUCATION, EDUCATION_KEYS);
                CvSection skills = allocation.claim(SectionRole.SKILLS, SKILL_KEYS);
                CvSection projects = allocation.claim(SectionRole.PROJECTS, PROJECT_KEYS);
                CvSection additional = allocation.claim(SectionRole.OTHER, ADDITIONAL_KEYS);
                CvSection summary = allocation.claim(SectionRole.SUMMARY, SUMMARY_KEYS);
                CvSection experience = allocation.claim(SectionRole.EXPERIENCE, EXPERIENCE_KEYS);

                List<ColumnPagination.Block> sidebar = modules(
                        module(education, "Education"),
                        module(skills, "Skills"),
                        module(projects, "Expertise"),
                        module(additional, "Languages"));

                List<ColumnPagination.Block> main = new ArrayList<>();
                addModule(main, prose(summary, "Professional Profile"));
                addModule(main, module(experience, "Work Experience"));
                // Whatever no module claimed — a user's own "Awards", a second
                // prose section — goes into the main column under its own
                // title rather than off the page. A paragraph keeps reading as
                // a paragraph: bulleting one line of prose because it arrived
                // through the leftover path would be a different kind of loss
                // than dropping it, but a loss all the same.
                for (CvSection leftover : allocation.remaining()) {
                    addModule(main, leftover instanceof ParagraphSection
                            ? prose(leftover, leftover.title())
                            : module(leftover, leftover.title()));
                }

                // Column inner widths: the row subtracts its gaps before the
                // weighted split, so derive them the same way or the wrap
                // estimate below is measuring a column that does not exist.
                double usable = Math.max(1.0, width - 2 * BODY_COLUMN_GAP);
                double weightSum = SIDEBAR_WEIGHT + AXIS_WEIGHT + MAIN_WEIGHT;
                double bodyBudget = Math.max(1.0,
                        document.availableHeight()
                        - mastheadHeight(doc.identity())
                        - theme.spacing().pageFlowSpacing() * 2);

                ColumnPagination sidebarMetrics = ColumnPagination.forColumn(
                        usable * SIDEBAR_WEIGHT / weightSum,
                        theme.typography().sizeEntrySubtitle(), 1.0,
                        theme.typography().sizeEntryTitle(),
                        SIDEBAR_BLOCK_GAP);
                ColumnPagination mainMetrics = ColumnPagination.forColumn(
                        usable * MAIN_WEIGHT / weightSum,
                        theme.typography().sizeBody(), 1.2,
                        theme.typography().sizeBanner(),
                        MAIN_BLOCK_GAP);

                List<List<ColumnPagination.Block>> sidebarPages =
                        sidebarMetrics.paginate(sidebar, bodyBudget);
                List<List<ColumnPagination.Block>> mainPages =
                        mainMetrics.paginate(main, bodyBudget);
                int pages = Math.max(1,
                        Math.max(sidebarPages.size(), mainPages.size()));

                PageFlowBuilder flow = document.dsl()
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
                                .margin(DocumentInsets.zero()));

                // One row per page. A row is atomic — the paginator cannot
                // break inside it — so the preset decides the page boundary
                // itself and lets each finished row overflow onto the next
                // page of its own accord.
                //
                // Overflow is enough to keep them apart, and deliberately so.
                // A slice closes early only when the next block will not fit
                // what is left of it, and that block then opens the following
                // slice — so two neighbours always add up to more than one
                // page and can never settle onto the same sheet. An explicit
                // page break would not improve on that and would cost a blank
                // one: a filled page has no room for the flow's own spacing,
                // so the cursor has already moved on by the time the break
                // lands, and the break moves it again.
                for (int page = 0; page < pages; page++) {
                    List<ColumnPagination.Block> sidebarPage = pageAt(sidebarPages, page);
                    List<ColumnPagination.Block> mainPage = pageAt(mainPages, page);
                    // The full-height axis is the preset's signature and stays
                    // on the opening page. A continuation page carrying three
                    // lines does not want 620pt of rule beside them, so there
                    // the axis follows the content.
                    double axisHeight = page == 0
                            ? TIMELINE_AXIS_HEIGHT
                            : continuationAxisHeight(sidebarPage, sidebarMetrics,
                                    mainPage, mainMetrics);
                    flow.addRow("CvV2TimelineMinimalBody" + page,
                            row -> addBodyRow(row, sidebarPage, mainPage,
                                    axisHeight));
                }
                flow.build();
            }

            private void addNameBlock(SectionBuilder section, CvIdentity identity) {
                section.spacing(NAME_BLOCK_GAP)
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
                                    List<ColumnPagination.Block> sidebarModules,
                                    List<ColumnPagination.Block> mainModules,
                                    double axisHeight) {
                row.spacing(BODY_COLUMN_GAP)
                        .weights(SIDEBAR_WEIGHT, AXIS_WEIGHT, MAIN_WEIGHT)
                        .addSection("CvV2TimelineMinimalSidebar", sidebar -> {
                            sidebar.spacing(10);
                            for (ColumnPagination.Block module : sidebarModules) {
                                addSidebarModule(sidebar, module);
                            }
                        })
                        .addSection("CvV2TimelineMinimalAxis", axis ->
                                TimelineAxisWidget.render(axis,
                                        timelineAxisStyle(), axisHeight))
                        .addSection("CvV2TimelineMinimalMain", main -> {
                            main.spacing(11);
                            for (ColumnPagination.Block module : mainModules) {
                                addMainModule(main, module);
                            }
                        });
            }

            private void addContact(SectionBuilder section, CvIdentity identity) {
                section.spacing(CONTACT_ROW_GAP);
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
                                    rich.shape(glyph(item.iconFile())
                                                    .outline(CONTACT_ICON_SIZE),
                                            ICON_COLOR, null,
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

            /**
             * Height the masthead takes off the page before the body row.
             *
             * <p>The header is a row, so its height is the taller of its two
             * columns: the name block on the left, the contact stack on the
             * right. The stack grows a row per link, which is why this is
             * measured rather than assumed — a fixed reserve is wrong in both
             * directions. Too small, and a CV with enough contact links pushes
             * the body row past the page into
             * {@code AtomicNodeTooLargeException}, since a row cannot be
             * broken. Too large, and every ordinary CV loses page space it
             * could have filled.</p>
             *
             * @param identity the header's data; {@code null} yields the rule alone
             * @return height in points, including the rule and its slack
             */
            private double mastheadHeight(CvIdentity identity) {
                double nameBlock = theme.typography().sizeHeadline()
                                   * ColumnPagination.FONT_LEADING_RATIO;
                if (identity != null && !identity.jobTitle().isBlank()) {
                    nameBlock += NAME_BLOCK_GAP
                                 + JOB_TITLE_SIZE * ColumnPagination.FONT_LEADING_RATIO;
                }
                int rows = contactItems(identity).size();
                double contactStack = rows == 0 ? 0.0
                        : rows * theme.typography().sizeContact()
                          * ColumnPagination.FONT_LEADING_RATIO
                          + (rows - 1) * CONTACT_ROW_GAP;
                return Math.max(nameBlock, contactStack)
                       + theme.spacing().accentRuleWidth() + MASTHEAD_SLACK;
            }

            private List<ContactItem> contactItems(CvIdentity identity) {
                if (identity == null) {
                    return List.of();
                }
                List<ContactItem> items = new ArrayList<>();
                addContactItem(items, "LOC", "location.svg",
                        identity.contact().address(), null);
                addContactItem(items, "TEL", "phone.svg",
                        identity.contact().phone(), null);
                String email = identity.contact().email();
                if (!email.isBlank()) {
                    addContactItem(items, "@", "email.svg", email,
                            new DocumentLinkOptions("mailto:" + email));
                }
                for (Link link : identity.links()) {
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

            private SvgGlyph glyph(String iconFile) {
                return SvgGlyph.fromResource(CONTACT_ICON_ROOT + iconFile);
            }

            private void addSidebarModule(SectionBuilder sidebar, ColumnPagination.Block module) {
                List<String> lines = module.lines();
                if (lines.isEmpty()) {
                    return;
                }
                String title = module.title();
                sidebar.addSection("CvV2TimelineMinimalSidebar"
                                   + SectionLookup.normalize(title), block -> {
                    block.spacing(6)
                            .addParagraph(paragraph -> paragraph
                                    .text(title.toUpperCase(Locale.ROOT))
                                    .textStyle(sidebarTitleStyle())
                                    .margin(DocumentInsets.zero()));
                    for (String line : lines) {
                        block.addParagraph(paragraph -> paragraph
                                .text(line)
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

            private void addMainModule(SectionBuilder main, ColumnPagination.Block module) {
                List<String> lines = module.lines();
                if (lines.isEmpty()) {
                    return;
                }
                String title = module.title();
                main.addSection("CvV2TimelineMinimalMain"
                                + SectionLookup.normalize(title), block -> {
                    block.spacing(5)
                            .addParagraph(paragraph -> paragraph
                                    .text(title.toUpperCase(Locale.ROOT))
                                    .textStyle(mainTitleStyle())
                                    .margin(DocumentInsets.zero()));
                    if (module.prose()) {
                        for (String line : lines) {
                            block.addParagraph(paragraph -> paragraph
                                    .text(line)
                                    .textStyle(mainBodyStyle())
                                    .lineSpacing(1.4)
                                    .margin(DocumentInsets.zero()));
                        }
                    } else {
                        for (String line : lines) {
                            block.addParagraph(paragraph -> paragraph
                                    .text(line)
                                    .textStyle(mainBulletStyle())
                                    .lineSpacing(1.2)
                                    .bulletOffset("-")
                                    .margin(DocumentInsets.zero()));
                        }
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
                return TextStyles.of(theme.typography().headlineFont(),
                        theme.typography().sizeHeadline(),
                        DocumentTextDecoration.DEFAULT,
                        theme.palette().ink());
            }

            private DocumentTextStyle jobTitleStyle() {
                return TextStyles.of(theme.typography().headlineFont(),
                        JOB_TITLE_SIZE,
                        DocumentTextDecoration.BOLD,
                        theme.palette().ink());
            }

            private DocumentTextStyle contactTextStyle() {
                return TextStyles.of(theme.typography().bodyFont(),
                        theme.typography().sizeContact(),
                        DocumentTextDecoration.BOLD,
                        theme.palette().muted());
            }

            private DocumentTextStyle fallbackIconStyle() {
                return TextStyles.of(theme.typography().headlineFont(),
                        8.0,
                        DocumentTextDecoration.BOLD,
                        theme.palette().muted());
            }

            private DocumentTextStyle sidebarTitleStyle() {
                return TextStyles.of(theme.typography().headlineFont(),
                        theme.typography().sizeEntryTitle(),
                        DocumentTextDecoration.BOLD,
                        theme.palette().ink());
            }

            private DocumentTextStyle sidebarBodyStyle() {
                return TextStyles.of(theme.typography().bodyFont(),
                        theme.typography().sizeEntrySubtitle(),
                        DocumentTextDecoration.BOLD,
                        theme.palette().ink());
            }

            private DocumentTextStyle mainTitleStyle() {
                return TextStyles.of(theme.typography().headlineFont(),
                        theme.typography().sizeBanner(),
                        DocumentTextDecoration.BOLD,
                        theme.palette().ink());
            }

            private DocumentTextStyle mainBulletStyle() {
                return TextStyles.of(theme.typography().bodyFont(),
                        theme.typography().sizeBody(),
                        DocumentTextDecoration.DEFAULT,
                        theme.palette().ink());
            }

            private DocumentTextStyle mainBodyStyle() {
                return TextStyles.of(theme.typography().bodyFont(),
                        theme.typography().sizeEntryDate(),
                        DocumentTextDecoration.DEFAULT,
                        theme.palette().ink());
            }
        }

    // -- helpers -----------------------------------------------------------

    /**
     * Flattens a {@link CvSection} into a list of single-line strings.
     *
     * <p>The shared {@code SectionDispatcher} would produce richly-styled
     * multi-paragraph output, which is not the shape this layout can work
     * with: knowing the line count is what lets the preset choose its own
     * page boundaries around the fixed-height axis. Counting lines is all it
     * is for — every line produced here is rendered, on this page or the
     * next.</p>
     */
    private static List<String> sectionLines(CvSection section) {
        if (!SectionLookup.hasContent(section)) {
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
        } else if (section instanceof ModuleSection module) {
            // A runtime module flattens the same way everything else does — one
            // line per item — because these lines are what the column
            // pagination measures. Rendering it through the shared dispatcher
            // instead would draw rich multi-paragraph output the estimator
            // never counted, and the axis row it lands in cannot page-break.
            for (CvItem item : module.items()) {
                StringBuilder line = new StringBuilder(MarkdownInline.plainText(item.title()));
                String meta = Stream.of(item.subtitle(), item.location(),
                                module.kind() == CvKind.ENTRIES_DATED ? item.period() : "")
                        .filter(value -> !value.isBlank())
                        .collect(Collectors.joining(" · "));
                if (!meta.isBlank()) {
                    line.append(" — ").append(meta);
                }
                if (!item.body().isEmpty()) {
                    line.append(": ").append(MarkdownInline.plainText(
                            String.join(" ", item.body())));
                }
                lines.add(line.toString());
            }
        } else if (section instanceof EntriesSection entries) {
            for (CvEntry entry : entries.entries()) {
                // Flattened single-line excerpt: strip inline markdown so a
                // [label](url) title/subtitle shows its label text — this preset
                // fuses and truncates the line, so it cannot carry a clickable link.
                String header = MarkdownInline.plainText(entry.title());
                String subtitle = MarkdownInline.plainText(entry.subtitle());
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

    private static String pickIconFile(String label) {
        String normalized = SectionLookup.normalize(label);
        if (normalized.contains("linkedin")) {
            return "linkedin.svg";
        }
        if (normalized.contains("github")) {
            return "github.svg";
        }
        if (normalized.contains("dribbble")) {
            return "dribbble.svg";
        }
        if (normalized.contains("google")) {
            return "google.svg";
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

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    /** A bulleted module, titled from the section or the preset's label. */
    private static ColumnPagination.Block module(CvSection section, String fallbackTitle) {
        return new ColumnPagination.Block(SectionAllocation.titleOr(section, fallbackTitle),
                sectionLines(section), false);
    }

    /** A running-text module — a profile or any other prose section. */
    private static ColumnPagination.Block prose(CvSection section, String fallbackTitle) {
        return new ColumnPagination.Block(SectionAllocation.titleOr(section, fallbackTitle),
                sectionLines(section), true);
    }

    private static List<ColumnPagination.Block> modules(ColumnPagination.Block... candidates) {
        List<ColumnPagination.Block> kept = new ArrayList<>();
        for (ColumnPagination.Block candidate : candidates) {
            addModule(kept, candidate);
        }
        return kept;
    }

    private static void addModule(List<ColumnPagination.Block> target, ColumnPagination.Block module) {
        if (!module.lines().isEmpty()) {
            target.add(module);
        }
    }

    private static List<ColumnPagination.Block> pageAt(List<List<ColumnPagination.Block>> pages, int index) {
        return index < pages.size() ? pages.get(index) : List.of();
    }

    /**
     * Axis height for a continuation page: as tall as the taller of the two
     * columns is estimated to be, so the rule ends where the content does.
     *
     * @return a height in points, never above {@link #TIMELINE_AXIS_HEIGHT}
     * and never so short the markers collide
     */
    private static double continuationAxisHeight(List<ColumnPagination.Block> sidebarPage,
                                                 ColumnPagination sidebarMetrics,
                                                 List<ColumnPagination.Block> mainPage,
                                                 ColumnPagination mainMetrics) {
        double tallest = Math.max(sidebarMetrics.pageHeight(sidebarPage),
                mainMetrics.pageHeight(mainPage));
        return Math.max(MIN_CONTINUATION_AXIS_HEIGHT,
                Math.min(TIMELINE_AXIS_HEIGHT, tallest));
    }


    private record ContactItem(String fallbackIcon, String iconFile,
                               String text, DocumentLinkOptions linkOptions) {
    }
}
