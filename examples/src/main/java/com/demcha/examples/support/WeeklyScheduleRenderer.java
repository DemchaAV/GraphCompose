package com.demcha.examples.support;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.font.FontName;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Reusable renderer for a bar / restaurant weekly shift schedule.
 *
 * <p>Pass a brand name, a week-start date, a flat staff list, per-day
 * metadata, a per-staff shift map (typed via {@link DayShift}), and
 * optionally a {@link Theme} / {@link Layout}; the renderer auto-fills
 * the date labels, sorts staff by {@link JobTitle} so groups appear in
 * a stable order, and inserts separator rows at every job-title
 * boundary. No string parsing, no positional bookkeeping, no
 * hardcoded colour table inside the renderer.</p>
 *
 * @author Artem Demchyshyn
 */
public final class WeeklyScheduleRenderer {

    // ───────────────────────── Public types ──────────────────────────

    /**
     * Job titles drive the row groups in the rendered table. The
     * declaration order also defines the rendered group order
     * (declared first ⇒ rendered first), and a separator row is
     * automatically inserted between adjacent groups.
     */
    public enum JobTitle {
        MANAGER("Management"),
        BARTENDER("Main Bar"),
        BAR_BACK("Bar Back");

        private final String groupHeading;

        JobTitle(String groupHeading) {
            this.groupHeading = groupHeading;
        }

        public String groupHeading() {
            return groupHeading;
        }
    }

    /** A staff member with their job title — one row in the schedule. */
    public record StaffMember(String name, JobTitle title) {
        public StaffMember {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(title, "title");
        }
    }

    /**
     * Per-day metadata: the operational note shown under the day name
     * and the lunch / dinner cover counts shown in the COVERS row.
     */
    public record DayPlan(String note, int coversLunch, int coversDinner) {
        public DayPlan {
            Objects.requireNonNull(note, "note");
        }
    }

    /**
     * Status fill — paints all sub-cells in one colour. Used both for
     * full-day fills ({@link DayShift#OFF} etc.) and for half-day
     * fills ({@link Half#STANDBY} etc.).
     */
    public enum ShiftStatus {
        OFF, HOLIDAY, REQUEST, STANDBY, TRAINING, BAR_BACK
    }

    /**
     * Optional tint applied to a working {@link Shift}. {@code STOCK}
     * paints the cell green (stock recon), {@code TRAINING} orange.
     * {@code NORMAL} leaves the shift cell un-tinted.
     */
    public enum ShiftType {
        NORMAL, STOCK, TRAINING
    }

    /** A working shift: start time, end time, and an optional tint. */
    public record Shift(String start, String end, ShiftType type) {
        public Shift {
            Objects.requireNonNull(start, "start");
            Objects.requireNonNull(end, "end");
            type = (type == null) ? ShiftType.NORMAL : type;
        }
        public static Shift of(String start, String end) {
            return new Shift(start, end, ShiftType.NORMAL);
        }
        public static Shift stock(String start, String end) {
            return new Shift(start, end, ShiftType.STOCK);
        }
        public static Shift training(String start, String end) {
            return new Shift(start, end, ShiftType.TRAINING);
        }
    }

    /**
     * Half-day cell — either a working {@link Shift}, a
     * {@link ShiftStatus} fill, or {@link #EMPTY} ("not working this
     * half"). Each {@code Half} renders as two sub-cells in the
     * schedule table ({@code colSpan(2)}).
     */
    public sealed interface Half {

        /** Empty half-day cell (the staff member isn't working this half). */
        Half EMPTY = new Empty();

        /** Wrap an existing {@link Shift}. */
        static Half shift(Shift shift) { return new Working(shift); }
        /** Normal working shift, no tint. */
        static Half shift(String start, String end) { return shift(Shift.of(start, end)); }
        /** Working shift painted with the STOCK tint. */
        static Half stock(String start, String end) { return shift(Shift.stock(start, end)); }
        /** Working shift painted with the TRAINING tint. */
        static Half training(String start, String end) { return shift(Shift.training(start, end)); }

        /** Half-day status fill (STANDBY / OFF / HOLIDAY / …). */
        static Half status(ShiftStatus status) { return new StatusFill(status); }

        /** Pre-built status halves — convenience constants. */
        Half OFF      = status(ShiftStatus.OFF);
        Half HOLIDAY  = status(ShiftStatus.HOLIDAY);
        Half REQUEST  = status(ShiftStatus.REQUEST);
        Half STANDBY  = status(ShiftStatus.STANDBY);
        Half TRAINING = status(ShiftStatus.TRAINING);
        Half BAR_BACK = status(ShiftStatus.BAR_BACK);

        record Working(Shift shift) implements Half {}
        record StatusFill(ShiftStatus status) implements Half {}
        record Empty() implements Half {}
    }

    /**
     * One day's assignment for one staff member. Either a full-day
     * status fill, a half-day pair (lunch + dinner), a cross-meal
     * shift painted across all four sub-cells, or empty.
     *
     * <p>The static constants ({@link #OFF}, {@link #HOLIDAY}, …) are
     * full-day status fills. The factory methods build half-day
     * combinations and cross-meal shifts:</p>
     *
     * <ul>
     *   <li>{@link #halves(Half, Half)} — explicit lunch + dinner
     *       (either side may be {@code null} ⇒ {@link Half#EMPTY})</li>
     *   <li>{@link #shifts(String, String, String, String)} —
     *       both halves as plain working shifts</li>
     *   <li>{@link #lunchOnly(String, String)},
     *       {@link #dinnerOnly(String, String)} — one half worked,
     *       the other empty</li>
     *   <li>{@link #acrossDay(String, String)} /
     *       {@link #acrossDay(String, String, ShiftType)} — cross-meal
     *       shift painted across all four sub-cells</li>
     * </ul>
     */
    public sealed interface DayShift {

        /** Empty day cell (no shift, no status fill). */
        DayShift NONE = new EmptyDay();

        /** Full-day status fill via the {@link ShiftStatus} enum. */
        static DayShift status(ShiftStatus status) { return new FullStatus(status); }

        DayShift OFF      = status(ShiftStatus.OFF);
        DayShift HOLIDAY  = status(ShiftStatus.HOLIDAY);
        DayShift REQUEST  = status(ShiftStatus.REQUEST);
        DayShift STANDBY  = status(ShiftStatus.STANDBY);
        DayShift TRAINING = status(ShiftStatus.TRAINING);
        DayShift BAR_BACK = status(ShiftStatus.BAR_BACK);

        /** Lunch + dinner halves; pass {@code null} for {@link Half#EMPTY}. */
        static DayShift halves(Half lunch, Half dinner) {
            return new Halves(
                    lunch == null ? Half.EMPTY : lunch,
                    dinner == null ? Half.EMPTY : dinner);
        }

        /** Two plain working shifts (lunch + dinner). */
        static DayShift shifts(String lunchStart, String lunchEnd,
                               String dinnerStart, String dinnerEnd) {
            return halves(Half.shift(lunchStart, lunchEnd),
                    Half.shift(dinnerStart, dinnerEnd));
        }

        /** Lunch shift only — dinner half is empty. */
        static DayShift lunchOnly(String start, String end) {
            return halves(Half.shift(start, end), Half.EMPTY);
        }

        /** Dinner shift only — lunch half is empty. */
        static DayShift dinnerOnly(String start, String end) {
            return halves(Half.EMPTY, Half.shift(start, end));
        }

        /** Cross-meal shift painted across all four sub-cells. */
        static DayShift acrossDay(String start, String end) {
            return new CrossMeal(Shift.of(start, end));
        }

        /** Cross-meal shift with a {@link ShiftType} tint (e.g. STOCK). */
        static DayShift acrossDay(String start, String end, ShiftType type) {
            return new CrossMeal(new Shift(start, end, type));
        }

        record FullStatus(ShiftStatus status) implements DayShift {}
        record Halves(Half lunch, Half dinner) implements DayShift {}
        record CrossMeal(Shift shift) implements DayShift {}
        record EmptyDay() implements DayShift {}
    }

    /**
     * Theme bundle — every colour the renderer reads. Build with
     * {@link #aurora()} for the cream-and-gold default, or supply
     * your own palette via the canonical constructor.
     */
    public record Theme(
            DocumentColor page,
            DocumentColor surface,
            DocumentColor grid,
            DocumentColor ink,
            DocumentColor muted,
            DocumentColor brandDark,
            DocumentColor brandAccent,
            Map<ShiftStatus, DocumentColor> statusFills,
            DocumentColor stockTint,
            DocumentColor trainingTint
    ) {
        public Theme {
            Objects.requireNonNull(page);
            Objects.requireNonNull(surface);
            Objects.requireNonNull(grid);
            Objects.requireNonNull(ink);
            Objects.requireNonNull(muted);
            Objects.requireNonNull(brandDark);
            Objects.requireNonNull(brandAccent);
            Objects.requireNonNull(statusFills);
            Objects.requireNonNull(stockTint);
            Objects.requireNonNull(trainingTint);
            statusFills = Map.copyOf(statusFills);
        }

        /** Look up the fill colour for a {@link ShiftStatus}. */
        public DocumentColor statusColor(ShiftStatus status) {
            DocumentColor c = statusFills.get(status);
            if (c == null) {
                throw new IllegalStateException("No fill defined for status " + status);
            }
            return c;
        }

        /** Look up the tint for a {@link ShiftType} (or {@code null} for NORMAL). */
        public DocumentColor shiftTint(ShiftType type) {
            return switch (type) {
                case NORMAL -> null;
                case STOCK -> stockTint;
                case TRAINING -> trainingTint;
            };
        }

        /** Default cream + gold palette ("AURORA"). */
        public static Theme aurora() {
            return new Theme(
                    DocumentColor.rgb(250, 247, 241),
                    DocumentColor.rgb(255, 253, 248),
                    DocumentColor.rgb(224, 216, 202),
                    DocumentColor.rgb(28, 31, 38),
                    DocumentColor.rgb(93, 91, 86),
                    DocumentColor.rgb(26, 35, 52),
                    DocumentColor.rgb(188, 136, 49),
                    Map.of(
                            ShiftStatus.OFF,      DocumentColor.rgb(180, 20, 36),
                            ShiftStatus.HOLIDAY,  DocumentColor.rgb(239, 174, 34),
                            ShiftStatus.REQUEST,  DocumentColor.rgb(169, 169, 169),
                            ShiftStatus.STANDBY,  DocumentColor.rgb(176, 151, 208),
                            ShiftStatus.TRAINING, DocumentColor.rgb(239, 119, 27),
                            ShiftStatus.BAR_BACK, DocumentColor.rgb(165, 105, 45)
                    ),
                    DocumentColor.rgb(22, 111, 86),
                    DocumentColor.rgb(239, 119, 27)
            );
        }
    }

    /**
     * Layout bundle — every dimension the renderer reads. Build with
     * {@link #landscape()} for the default landscape board, or supply
     * your own page size + column widths.
     */
    public record Layout(
            DocumentPageSize pageSize,
            DocumentInsets margin,
            double nameColWidth,
            double dayColWidth
    ) {
        public Layout {
            Objects.requireNonNull(pageSize);
            Objects.requireNonNull(margin);
            if (nameColWidth <= 0) throw new IllegalArgumentException("nameColWidth > 0");
            if (dayColWidth <= 0) throw new IllegalArgumentException("dayColWidth > 0");
        }

        /** Default landscape layout matching the AURORA demo board. */
        public static Layout landscape() {
            // The name column and the seven day columns are sized to fill the printable
            // width rather than picked: the page is 841.88977 wide, the side margins take
            // 8 each, and the columns divide what is left. The board is printed, so
            // leftover width is wasted paper — the previous 90 + 7 x 105 left 0.89pt of
            // it. The day columns also have to be wide enough for a bold day note, which
            // spans four fixed sub-columns and so cannot borrow width from a neighbour.
            double printable = PAGE_WIDTH - 2 * SIDE_MARGIN;
            double dayColumn = 105.5;
            return new Layout(
                    DocumentPageSize.of(PAGE_WIDTH, 472),
                    new DocumentInsets(BOTTOM_MARGIN, SIDE_MARGIN, BOTTOM_MARGIN, SIDE_MARGIN),
                    Math.floor((printable - DAYS_IN_WEEK * dayColumn) * 100) / 100,
                    dayColumn);
        }

        public double subColWidth() { return dayColWidth / 4.0; }
        public int totalColumns() { return 1 + DAYS_IN_WEEK * 4; }

        /** What the footer has to lay itself out inside. */
        public double printableWidth() {
            return pageSize.width() - margin.left() - margin.right();
        }

        /**
         * Half the footer rule: the printable width less the seal and the gap
         * on either side of it.
         *
         * <p>Equal weights already put the seal's <em>slot</em> on the centre
         * line — that part needs no arithmetic. What this buys is that the
         * rules fill the slots they are given: a shape narrower than its slot
         * is drawn at the slot's left edge, so a fixed width leaves the drawn
         * rule short on one side of the seal and short of the margin on the
         * other, and the foot reads as lopsided however centred the seal is.</p>
         */
        public double footerRuleWidth() {
            return (printableWidth() - SEAL_WIDTH - 2 * SEAL_GAP) / 2;
        }
    }

    // ───────────────────────── Constants ─────────────────────────────

    private static final int DAYS_IN_WEEK = 7;

    /** Landscape A4, the sheet the board is printed on. */
    private static final double PAGE_WIDTH = 841.88977;

    /** Left and right margin; the columns divide what is left. */
    private static final double SIDE_MARGIN = 8;

    /** Top and bottom margin; the footer zone has to clear the bottom one. */
    private static final double BOTTOM_MARGIN = 14;

    /** The seal that breaks the footer rule, and the air on either side of it. */
    private static final double SEAL_WIDTH = 44;
    private static final double SEAL_GAP = 8;

    /** Times-Bold, as the seal is set: the metrics the seal's geometry reads. */
    private static final double TIMES_BOLD_ASCENT = 0.683;
    private static final double TIMES_BOLD_DESCENT = 0.217;
    private static final double TIMES_BOLD_CAP_HEIGHT = 0.676;

    /** The seal's letter, and the point of air the pill keeps above and below it. */
    private static final double SEAL_LETTER_SIZE = 13;
    private static final double SEAL_PADDING_Y = 1;

    /**
     * How far the seal's letter drops to sit level in its pill.
     *
     * <p>A line box is tall enough for a descender and a capital has none, so
     * centring the box centres the unused space along with the letter and the
     * letter rides high. Dropping the box by half that unused space — the cap
     * height against what the ascent and descent leave — puts the ink on the
     * pill's centre line. It scales with the letter, but not with the font:
     * set the seal in something other than Times-Bold and these three metrics
     * have to change with it.</p>
     */
    private static final double SEAL_LETTER_DROP = SEAL_LETTER_SIZE
            * (TIMES_BOLD_CAP_HEIGHT - (TIMES_BOLD_ASCENT - TIMES_BOLD_DESCENT)) / 2;

    /** The pill: the letter's line box, plus its point of air top and bottom. */
    private static final double SEAL_HEIGHT =
            SEAL_LETTER_SIZE * (TIMES_BOLD_ASCENT + TIMES_BOLD_DESCENT) + 2 * SEAL_PADDING_Y;

    /** The hairline the seal breaks, and the drop that lands it on the seal's centre line. */
    private static final double FOOTER_RULE_THICKNESS = 0.6;
    private static final double FOOTER_RULE_DROP = (SEAL_HEIGHT - FOOTER_RULE_THICKNESS) / 2;

    /**
     * The build line under the rule, set in Courier, and the air between the
     * two. The depth reserved for it is the font's box rather than its metric
     * descent, because it is ink, not the line box, that has to stay out of
     * the margin, and a descender reaches past the line it is measured by.
     */
    private static final double COURIER_ASCENT = 0.629;
    private static final double COURIER_BOX_DESCENT = 0.250;
    private static final double BUILD_LINE_SIZE = 7.5;
    private static final double FOOTER_GAP_Y = 6;

    /**
     * Height reserved at the foot of every page.
     *
     * <p>A footer zone is measured from the foot of the <em>sheet</em>, not
     * from the bottom margin, and its content is laid out from the top of the
     * band down. So the band has to be the furniture plus the margin it must
     * not print into: at 34 — the furniture alone — the build line came within
     * three millimetres of the paper edge, inside the dead zone of most office
     * printers, and this board is printed.</p>
     */
    private static final double FOOTER_ZONE_HEIGHT = SEAL_HEIGHT + FOOTER_GAP_Y
            + BUILD_LINE_SIZE * (COURIER_ASCENT + COURIER_BOX_DESCENT) + BOTTOM_MARGIN;
    private static final DayShift[] EMPTY_WEEK = {
            DayShift.NONE, DayShift.NONE, DayShift.NONE, DayShift.NONE,
            DayShift.NONE, DayShift.NONE, DayShift.NONE
    };

    private WeeklyScheduleRenderer() {
    }

    // ───────────────────────── Render API ────────────────────────────

    /**
     * Render a weekly schedule using the default {@link Theme#aurora()}
     * and {@link Layout#landscape()}.
     */
    public static void renderTo(Path outputFile,
                                String brandName,
                                LocalDate weekStart,
                                List<StaffMember> staff,
                                List<DayPlan> week,
                                Map<String, DayShift[]> shifts) throws Exception {
        renderTo(outputFile, brandName, weekStart, staff, week, shifts,
                Theme.aurora(), Layout.landscape());
    }

    /**
     * Render a weekly schedule.
     *
     * @param outputFile target PDF; overwritten if it exists
     * @param brandName  short brand label shown in the top-left cell
     * @param weekStart  the day at the start of the displayed week
     * @param staff      staff members; auto-sorted by {@link JobTitle}
     *                   so groups are contiguous
     * @param week       per-day metadata; must contain exactly seven entries
     * @param shifts     per-staff {@link DayShift}[] keyed by name;
     *                   missing keys render as a row of empty cells,
     *                   {@code null} entries inside an array are
     *                   treated as {@link DayShift#NONE}
     * @param theme      colour palette
     * @param layout     page size + column widths
     */
    public static void renderTo(Path outputFile,
                                String brandName,
                                LocalDate weekStart,
                                List<StaffMember> staff,
                                List<DayPlan> week,
                                Map<String, DayShift[]> shifts,
                                Theme theme,
                                Layout layout) throws Exception {
        Objects.requireNonNull(outputFile, "outputFile");
        Objects.requireNonNull(brandName, "brandName");
        // The brand heads the board and its initial seals the foot, so a blank
        // one is a board with nobody's name on it rather than a default.
        if (brandName.isBlank()) {
            throw new IllegalArgumentException("brandName must not be blank");
        }
        Objects.requireNonNull(weekStart, "weekStart");
        Objects.requireNonNull(staff, "staff");
        Objects.requireNonNull(week, "week");
        Objects.requireNonNull(shifts, "shifts");
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(layout, "layout");
        if (week.size() != DAYS_IN_WEEK) {
            throw new IllegalArgumentException(
                    "week must have " + DAYS_IN_WEEK + " entries, got " + week.size());
        }

        // Auto-generate day labels: "Monday 4th", "Tuesday 5th", …
        String[] dayLabels = new String[DAYS_IN_WEEK];
        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            dayLabels[i] = formatDayLabel(weekStart.plusDays(i));
        }

        // Sort staff by job-title ordinal so groups are contiguous.
        List<StaffMember> sortedStaff = new ArrayList<>(staff);
        sortedStaff.sort(Comparator.comparingInt(s -> s.title().ordinal()));

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(layout.pageSize())
                .pageBackground(theme.page())
                .margin(layout.margin())
                .create()) {

            // The rule, its seal and the build line are page furniture, not
            // the last thing on the board: as a zone they are drawn against
            // the bottom margin, so a week that fills the sheet and a week
            // that half fills it both foot the same way.
            document.chrome().zone(DocumentPageZone.footer(FOOTER_ZONE_HEIGHT, page -> new SectionBuilder()
                    .name("FooterZone")
                    .spacing(6)
                    .addRow("FooterRule", row -> row
                            .spacing(SEAL_GAP)
                            // Equal weights on the two rules are what put the
                            // seal in the middle; the shapes then have to fill
                            // the slots the weights hand them.
                            .weights(layout.footerRuleWidth(), SEAL_WIDTH, layout.footerRuleWidth())
                            .addSection("LeftRule", section -> section
                                    .padding(new DocumentInsets(FOOTER_RULE_DROP, 0, 0, 0))
                                    .addShape(shape -> shape.size(layout.footerRuleWidth(), FOOTER_RULE_THICKNESS)
                                            .fillColor(theme.brandAccent()).margin(DocumentInsets.zero())))
                            .addSection("Seal", section -> section
                                    .stroke(DocumentStroke.of(theme.brandAccent(), 0.5))
                                    .cornerRadius(9)
                                    .padding(new DocumentInsets(SEAL_PADDING_Y + SEAL_LETTER_DROP, 0,
                                            SEAL_PADDING_Y - SEAL_LETTER_DROP, 0))
                                    .addParagraph(p -> p
                                            .text(sealLetter(brandName))
                                            .align(TextAlign.CENTER)
                                            .textStyle(DocumentTextStyle.builder()
                                                    .fontName(FontName.TIMES_ROMAN)
                                                    .decoration(DocumentTextDecoration.BOLD)
                                                    .size(SEAL_LETTER_SIZE)
                                                    .color(theme.brandAccent())
                                                    .build())
                                            .margin(DocumentInsets.zero())))
                            .addSection("RightRule", section -> section
                                    .padding(new DocumentInsets(FOOTER_RULE_DROP, 0, 0, 0))
                                    .addShape(shape -> shape.size(layout.footerRuleWidth(), FOOTER_RULE_THICKNESS)
                                            .fillColor(theme.brandAccent()).margin(DocumentInsets.zero()))))
                    // Left, against the same margin the board's first column
                    // starts on: it is a credit, not part of the device above
                    // it, and centring gave it a weight it should not carry.
                    .addParagraph(p -> p
                            .text("Composed with GraphCompose — examples/.../WeeklyScheduleRenderer.java")
                            .textStyle(DocumentTextStyle.builder()
                                    .fontName(FontName.COURIER)
                                    .size(7.5)
                                    .color(theme.muted())
                                    .build())
                            .margin(DocumentInsets.zero()))
                    .build()));

            document.pageFlow()
                    .name("WeeklyShiftSchedule")
                    .spacing(3)

                    // Colour legend at the top — the schedule body relies on
                    // colour alone, so this strip is the key.
                    .addRow("LegendStrip", row -> row
                            .spacing(6)
                            .weights(layout.nameColWidth(),
                                    layout.dayColWidth(), layout.dayColWidth(),
                                    layout.dayColWidth(), layout.dayColWidth(),
                                    layout.dayColWidth(), layout.dayColWidth(),
                                    layout.dayColWidth())
                            .addSection("LegendBlank", section -> section
                                    .padding(new DocumentInsets(4, 0, 4, 0)))
                            .addSection("Lr",  section -> chip(section, "REQUEST",  theme.statusColor(ShiftStatus.REQUEST),  theme))
                            .addSection("Lo",  section -> chip(section, "OFF",      theme.statusColor(ShiftStatus.OFF),      theme))
                            .addSection("Lh",  section -> chip(section, "HOL",      theme.statusColor(ShiftStatus.HOLIDAY),  theme))
                            .addSection("Ls",  section -> chip(section, "STOCK",    theme.stockTint(),                       theme))
                            .addSection("Lsb", section -> chip(section, "Standby",  theme.statusColor(ShiftStatus.STANDBY),  theme))
                            .addSection("Lt",  section -> chip(section, "Training", theme.statusColor(ShiftStatus.TRAINING), theme))
                            .addSection("Lbb", section -> chip(section, "Bar Back", theme.statusColor(ShiftStatus.BAR_BACK), theme)))

                    // Schedule table — brand cell + headers + COVERS + staff rows.
                    .addTable(table -> {
                        DocumentTableColumn[] columns = new DocumentTableColumn[layout.totalColumns()];
                        columns[0] = DocumentTableColumn.fixed(layout.nameColWidth());
                        for (int i = 1; i < columns.length; i++) {
                            columns[i] = DocumentTableColumn.fixed(layout.subColWidth());
                        }
                        table.columns(columns).defaultCellStyle(emptyCellStyle(theme));

                        // Day-name header row + brand cell on the left.
                        List<DocumentTableCell> dayNameRow = new ArrayList<>();
                        dayNameRow.add(DocumentTableCell.text(brandName)
                                .withStyle(brandCellStyle(theme)).rowSpan(2));
                        for (int d = 0; d < DAYS_IN_WEEK; d++) {
                            dayNameRow.add(DocumentTableCell.text(dayLabels[d])
                                    .withStyle(dayNameCellStyle(theme)).colSpan(4));
                        }
                        table.rowCells(dayNameRow);

                        // Day-note row. Notes are split at "/" delimiters so
                        // each fragment becomes its own line in the cell —
                        // table cells use the longest single line as their
                        // natural width, so splitting keeps long compound
                        // notes within the four sub-cell column budget.
                        List<DocumentTableCell> dayNoteRow = new ArrayList<>();
                        for (int d = 0; d < DAYS_IN_WEEK; d++) {
                            dayNoteRow.add(DocumentTableCell.lines(splitNote(week.get(d).note()))
                                    .withStyle(dayNoteCellStyle(theme)).colSpan(4));
                        }
                        table.rowCells(dayNoteRow);

                        // COVERS row — lunch + dinner counts per day.
                        List<DocumentTableCell> coversRow = new ArrayList<>();
                        coversRow.add(DocumentTableCell.text("COVERS")
                                .withStyle(coversLabelStyle(theme)));
                        for (int d = 0; d < DAYS_IN_WEEK; d++) {
                            coversRow.add(DocumentTableCell.text(String.valueOf(week.get(d).coversLunch()))
                                    .withStyle(coversCellStyle(theme)).colSpan(2));
                            coversRow.add(DocumentTableCell.text(String.valueOf(week.get(d).coversDinner()))
                                    .withStyle(coversCellStyle(theme)).colSpan(2));
                        }
                        table.rowCells(coversRow);

                        // Staff rows — separator auto-inserted at every JobTitle boundary.
                        JobTitle prevTitle = null;
                        for (StaffMember member : sortedStaff) {
                            if (prevTitle != null && prevTitle != member.title()) {
                                table.rowCells(separatorRow(theme, layout));
                            }
                            DayShift[] dayShifts = shifts.getOrDefault(member.name(), EMPTY_WEEK);
                            table.rowCells(staffRow(member.name(), dayShifts, theme, layout));
                            prevTitle = member.title();
                        }
                    })

                    .build();

            document.buildPdf();
        }
    }

    /**
     * The letter the footer seal carries: the board's own initial, so a
     * schedule printed for one venue is not sealed with another's.
     *
     * <p>Read as a code point rather than a {@code char} so a brand opening on
     * a character outside the basic plane is not sealed with half of it.</p>
     */
    private static String sealLetter(String brandName) {
        String trimmed = brandName.strip();
        return String.valueOf(Character.toChars(trimmed.codePointAt(0))).toUpperCase(Locale.ROOT);
    }

    /**
     * Split a day note like {@code "Bank Holiday Monday / Clean Crushed
     * Ice Machine & Area"} into its slash-separated fragments so each
     * fragment becomes its own line in the cell. Table cells measure
     * natural width as the longest single line, so this keeps long
     * compound notes within the column budget.
     */
    private static String[] splitNote(String note) {
        if (note == null || note.isBlank()) {
            return new String[] {""};
        }
        return note.split("\\s*/\\s*");
    }

    // ───────────────────────── Date formatting ───────────────────────

    /** Format a date like "Monday 4th". */
    private static String formatDayLabel(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        String dayName = dow.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return dayName + " " + ordinalSuffix(date.getDayOfMonth());
    }

    private static String ordinalSuffix(int day) {
        if (day >= 11 && day <= 13) {
            return day + "th";
        }
        return switch (day % 10) {
            case 1 -> day + "st";
            case 2 -> day + "nd";
            case 3 -> day + "rd";
            default -> day + "th";
        };
    }

    // ───────────────────────── Cell helpers ──────────────────────────

    private static List<DocumentTableCell> staffRow(String name, DayShift[] dayShifts,
                                                    Theme theme, Layout layout) {
        List<DocumentTableCell> cells = new ArrayList<>();
        cells.add(DocumentTableCell.text(name).withStyle(nameCellStyle(theme)));
        int days = Math.min(dayShifts.length, DAYS_IN_WEEK);
        for (int i = 0; i < days; i++) {
            cells.addAll(dayCells(dayShifts[i], theme));
        }
        // Pad missing days so the row always covers the full sub-cell grid.
        int columnsCovered = totalColumnsCovered(cells);
        while (columnsCovered < layout.totalColumns()) {
            cells.add(DocumentTableCell.text("")
                    .withStyle(emptyCellStyle(theme))
                    .colSpan(Math.min(4, layout.totalColumns() - columnsCovered)));
            columnsCovered = totalColumnsCovered(cells);
        }
        return cells;
    }

    private static int totalColumnsCovered(List<DocumentTableCell> cells) {
        int total = 0;
        for (DocumentTableCell cell : cells) {
            total += cell.colSpan();
        }
        return total;
    }

    /**
     * Maps one {@link DayShift} to the list of cells that fill the
     * day's four sub-columns. Total {@code colSpan} of the returned
     * cells is always 4.
     */
    private static List<DocumentTableCell> dayCells(DayShift dayShift, Theme theme) {
        if (dayShift == null || dayShift instanceof DayShift.EmptyDay) {
            return List.of(emptyDayCell(theme));
        }
        if (dayShift instanceof DayShift.FullStatus fs) {
            return List.of(fullStatusCell(fs.status(), theme));
        }
        if (dayShift instanceof DayShift.CrossMeal cm) {
            return shiftAcrossDay(cm.shift(), theme);
        }
        if (dayShift instanceof DayShift.Halves h) {
            return mergedHalfCells(h.lunch(), h.dinner(), theme);
        }
        throw new IllegalStateException("Unhandled DayShift variant: " + dayShift);
    }

    private static List<DocumentTableCell> mergedHalfCells(Half lunch, Half dinner, Theme theme) {
        // Both halves on the same status fill → merge to colSpan(4).
        if (lunch instanceof Half.StatusFill ls
                && dinner instanceof Half.StatusFill ds
                && ls.status() == ds.status()) {
            return List.of(fullStatusCell(ls.status(), theme));
        }
        List<DocumentTableCell> cells = new ArrayList<>();
        cells.addAll(halfCells(lunch, theme));
        cells.addAll(halfCells(dinner, theme));
        return cells;
    }

    private static List<DocumentTableCell> halfCells(Half half, Theme theme) {
        if (half instanceof Half.Empty) {
            return List.of(emptyHalfCell(theme));
        }
        if (half instanceof Half.StatusFill sf) {
            return List.of(statusHalfCell(sf.status(), theme));
        }
        if (half instanceof Half.Working w) {
            return shiftHalfCells(w.shift(), theme);
        }
        throw new IllegalStateException("Unhandled Half variant: " + half);
    }

    private static DocumentTableCell emptyDayCell(Theme theme) {
        return DocumentTableCell.text("").withStyle(emptyCellStyle(theme)).colSpan(4);
    }

    private static DocumentTableCell emptyHalfCell(Theme theme) {
        return DocumentTableCell.text("").withStyle(emptyCellStyle(theme)).colSpan(2);
    }

    private static DocumentTableCell fullStatusCell(ShiftStatus status, Theme theme) {
        return DocumentTableCell.text("").withStyle(statusFill(theme.statusColor(status), theme)).colSpan(4);
    }

    private static DocumentTableCell statusHalfCell(ShiftStatus status, Theme theme) {
        return DocumentTableCell.text("").withStyle(statusFill(theme.statusColor(status), theme)).colSpan(2);
    }

    private static List<DocumentTableCell> shiftHalfCells(Shift shift, Theme theme) {
        DocumentTableStyle style = shiftCellStyle(theme.shiftTint(shift.type()), theme);
        return List.of(
                DocumentTableCell.text(shift.start()).withStyle(style),
                DocumentTableCell.text(shift.end()).withStyle(style));
    }

    /**
     * Renders a cross-meal shift across the four sub-cells of a day.
     * Start in the leftmost cell, end in the rightmost; the two
     * middle cells stay empty. All four share the shift's tint so
     * the band reads as one continuous painted strip across the day.
     */
    private static List<DocumentTableCell> shiftAcrossDay(Shift shift, Theme theme) {
        DocumentTableStyle style = shiftCellStyle(theme.shiftTint(shift.type()), theme);
        return List.of(
                DocumentTableCell.text(shift.start()).withStyle(style),
                DocumentTableCell.text("").withStyle(style),
                DocumentTableCell.text("").withStyle(style),
                DocumentTableCell.text(shift.end()).withStyle(style));
    }

    private static List<DocumentTableCell> separatorRow(Theme theme, Layout layout) {
        DocumentTableStyle style = DocumentTableStyle.builder()
                .padding(new DocumentInsets(2.2, 0, 2.2, 0))
                .fillColor(theme.brandDark())
                .stroke(DocumentStroke.of(theme.brandDark(), 0.3))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA)
                        .size(1)
                        .color(theme.brandDark())
                        .build())
                .build();
        return List.of(DocumentTableCell.text("").withStyle(style).colSpan(layout.totalColumns()));
    }

    /** White text on dark or saturated fills, dark ink on light fills. */
    private static DocumentColor textOn(DocumentColor fill, Theme theme) {
        if (fill == null) return theme.ink();
        DocumentColor off = theme.statusFills().get(ShiftStatus.OFF);
        DocumentColor barBack = theme.statusFills().get(ShiftStatus.BAR_BACK);
        return (fill.equals(off) || fill.equals(theme.stockTint())
                || fill.equals(barBack) || fill.equals(theme.brandDark()))
                ? DocumentColor.WHITE
                : theme.ink();
    }

    private static void chip(SectionBuilder section, String label, DocumentColor fill, Theme theme) {
        section
                .fillColor(fill)
                .stroke(DocumentStroke.of(theme.grid(), 0.3))
                .padding(new DocumentInsets(5, 10, 5, 10))
                .addParagraph(p -> p
                        .text(label)
                        .align(TextAlign.CENTER)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA)
                                .decoration(DocumentTextDecoration.BOLD)
                                .size(8.5)
                                .color(DocumentColor.WHITE)
                                .build())
                        .margin(DocumentInsets.zero()));
    }

    // ───────────────────────── Styles (theme-aware) ──────────────────

    private static DocumentTableStyle brandCellStyle(Theme theme) {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(8, 4, 8, 4))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.TIMES_ROMAN)
                        .decoration(DocumentTextDecoration.BOLD)
                        .size(18)
                        .color(theme.brandAccent())
                        .build())
                .stroke(DocumentStroke.of(theme.grid(), 0.3))
                .fillColor(theme.surface())
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .build();
    }

    private static DocumentTableStyle dayNameCellStyle(Theme theme) {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(6, 4, 4, 4))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.TIMES_ROMAN)
                        .decoration(DocumentTextDecoration.BOLD)
                        .size(11.5)
                        .color(theme.ink())
                        .build())
                .stroke(DocumentStroke.of(theme.grid(), 0.3))
                .fillColor(theme.surface())
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .build();
    }

    private static DocumentTableStyle dayNoteCellStyle(Theme theme) {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(2, 5, 6, 5))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA)
                        .decoration(DocumentTextDecoration.BOLD)
                        // 5.9 bold overflows the four fixed sub-columns a note spans,
                        // and a spanned cell cannot borrow width from fixed neighbours.
                        .size(5.75)
                        .color(theme.ink())
                        .build())
                .stroke(DocumentStroke.of(theme.grid(), 0.3))
                .fillColor(theme.surface())
                .lineSpacing(1.25)
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .build();
    }

    private static DocumentTableStyle statusFill(DocumentColor fill, Theme theme) {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(8.5, 4, 8.5, 4))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA)
                        .decoration(DocumentTextDecoration.BOLD)
                        .size(8.5)
                        .color(textOn(fill, theme))
                        .build())
                .stroke(DocumentStroke.of(theme.grid(), 0.3))
                .fillColor(fill)
                .build();
    }

    private static DocumentTableStyle shiftCellStyle(DocumentColor fill, Theme theme) {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(6, 1, 6, 1))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA)
                        .size(6.8)
                        .color(fill == null ? theme.ink() : textOn(fill, theme))
                        .build())
                .stroke(DocumentStroke.of(theme.grid(), 0.3))
                .fillColor(fill)
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .build();
    }

    private static DocumentTableStyle emptyCellStyle(Theme theme) {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(6, 5, 6, 5))
                .stroke(DocumentStroke.of(theme.grid(), 0.3))
                .fillColor(theme.surface())
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA)
                        .size(8)
                        .color(theme.ink())
                        .build())
                .textAnchor(DocumentTableTextAnchor.CENTER_LEFT)
                .build();
    }

    private static DocumentTableStyle nameCellStyle(Theme theme) {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(7, 6, 7, 6))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA)
                        .decoration(DocumentTextDecoration.BOLD)
                        .size(9.1)
                        .color(theme.brandDark())
                        .build())
                .stroke(DocumentStroke.of(theme.grid(), 0.3))
                .fillColor(theme.surface())
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .build();
    }

    private static DocumentTableStyle coversLabelStyle(Theme theme) {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(5, 6, 5, 6))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA)
                        .decoration(DocumentTextDecoration.BOLD)
                        .size(8.8)
                        .color(DocumentColor.WHITE)
                        .build())
                .stroke(DocumentStroke.of(theme.grid(), 0.3))
                .fillColor(theme.brandDark())
                .lineSpacing(1.35)
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .build();
    }

    private static DocumentTableStyle coversCellStyle(Theme theme) {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(5, 4, 5, 4))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA)
                        .decoration(DocumentTextDecoration.BOLD)
                        .size(8.8)
                        .color(theme.ink())
                        .build())
                .stroke(DocumentStroke.of(theme.grid(), 0.3))
                .fillColor(theme.surface())
                .lineSpacing(1.35)
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .build();
    }
}
