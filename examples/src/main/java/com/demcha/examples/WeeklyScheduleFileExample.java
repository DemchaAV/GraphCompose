package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Bar / restaurant weekly shift schedule modelled after a real-world
 * floor-management board: staff names down the left, seven day columns
 * across the top, each day cell carrying either shift times (multi-line
 * text) or a colour-coded status fill (REQUEST / OFF / HOL / STOCK /
 * Standby / Training / Bar Back).
 *
 * <p>Section separator rows split the team into management, main bar, and
 * bar back / BBQ groups. Composed end-to-end through the canonical DSL
 * with per-cell {@code DocumentTableCell.withStyle(...)} overrides on a
 * single 8-column table.</p>
 */
public final class WeeklyScheduleFileExample {

    // ──────────────────────── Theme + colours ────────────────────────

    private static final DocumentColor PAGE = DocumentColor.rgb(250, 247, 241);
    private static final DocumentColor SURFACE = DocumentColor.rgb(255, 253, 248);
    private static final DocumentColor HEADER_FILL = DocumentColor.rgb(255, 253, 248);
    private static final DocumentColor NAME_FILL = DocumentColor.rgb(255, 253, 248);
    private static final DocumentColor COVERS_FILL = DocumentColor.rgb(255, 253, 248);
    private static final DocumentColor GRID = DocumentColor.rgb(224, 216, 202);
    private static final DocumentColor INK = DocumentColor.rgb(28, 31, 38);
    private static final DocumentColor MUTED = DocumentColor.rgb(93, 91, 86);
    private static final DocumentColor BRAND_DARK = DocumentColor.rgb(26, 35, 52);
    private static final DocumentColor LOGO_GOLD = DocumentColor.rgb(188, 136, 49);

    private static final DocumentColor REQUEST = DocumentColor.rgb(169, 169, 169);
    private static final DocumentColor OFF = DocumentColor.rgb(180, 20, 36);
    private static final DocumentColor HOL = DocumentColor.rgb(239, 174, 34);
    private static final DocumentColor STOCK = DocumentColor.rgb(22, 111, 86);
    private static final DocumentColor STANDBY = DocumentColor.rgb(176, 151, 208);
    private static final DocumentColor TRAINING = DocumentColor.rgb(239, 119, 27);
    private static final DocumentColor BARBACK = DocumentColor.rgb(165, 105, 45);
    private static final DocumentColor SEPARATOR = BRAND_DARK;

    // ──────────────────────── Schedule data ──────────────────────────

    private static final String[] DAYS = {
            "Monday 4th", "Tuesday 5th", "Wednesday 6th", "Thursday 7th",
            "Friday 8th", "Saturday 9th", "Sunday 10th"
    };

    private static final String[] DAY_NOTES = {
            "Bank Holiday Monday / Clean Crushed Ice Machine & Area",
            "Pianist 18:30 / Clean Crushed Ice Machine & Area",
            "TEAM DAY OUT / Motown GF / Pianist 18:30",
            "Ex Hire Terrace Dinner 13PAX / Pianist 18:30 / MGM meeting 3:30pm",
            "Pianist 19:00",
            "Ex Hire FF Lunch 44PAX / Pianist 19:00",
            "Masterclass 4PAX (2x2) / Line Check / Clean Cubed Ice Machine & Area"
    };

    private static final String[] COVERS_LUNCH = {"163", "21", "52", "35", "82", "131", "130"};
    private static final String[] COVERS_DINNER = {"35", "63", "74", "75", "78", "149", "36"};

    /**
     * Each day cell is laid out as four sub-columns:
     * {@code lunch-start | lunch-end | dinner-start | dinner-end}.
     * Tokens describe how those four sub-cells are filled:
     *
     * <ul>
     *   <li>Pure status code ({@code OFF / HOL / REQ / SBY / TRN / BB})
     *       alone collapses the whole day into one {@code colSpan(4)}
     *       coloured cell.</li>
     *   <li>{@code lunch / dinner} splits the day into two halves; each
     *       half is rendered into two sub-cells.</li>
     *   <li>Within a half:
     *       <ul>
     *         <li>{@code HH:MM-HH:MM} — start time in left cell, end time
     *             in right cell, both on white.</li>
     *         <li>{@code STOCK:HH:MM-HH:MM} — same but both cells share the
     *             green STOCK fill (the "stock recon" piece is highlighted).</li>
     *         <li>{@code TRN:HH:MM-HH:MM} — both cells share the orange
     *             TRAINING fill.</li>
     *         <li>{@code OFF / HOL / REQ / SBY / TRN / BB} — single
     *             {@code colSpan(2)} coloured cell with the status label.</li>
     *         <li>Empty string — single {@code colSpan(2)} empty cell.</li>
     *       </ul>
     *   </li>
     *   <li>If both halves are the same pure status code (e.g. {@code "OFF / OFF"}),
     *       the day collapses into a single {@code colSpan(4)} cell.</li>
     * </ul>
     */
    private static final String[][] STAFF = {
            // Group 1 — management
            {"AARON PARK",
                    "STOCK:09:00-18:00", "OFF", "OFF", " / 16:00-00:00",
                    "OFF", "11:00-16:00 / 16:00-22:00", "08:00-13:00 / 13:00-18:00"},
            {"BETH VANCE",
                    " / 16:00-00:00", "REQ / 17:00-00:00", "OFF", "SBY / 16:00-00:00",
                    "12:00-16:00 / 17:00-01:00", "REQ", "12:00-16:00 / STOCK:17:00-00:00"},
            {"CONNOR ITO", "OFF", "OFF", "OFF", "OFF", "OFF", "OFF", "OFF"},
            // Group 2 — main bar
            {"DIANA COLE",
                    "12:00-20:00 / SBY", "OFF / 17:00-00:00", "OFF", " / 17:00-00:00",
                    "SBY / SBY", "OFF", "09:00-14:00 / 14:00-20:00"},
            {"ELENA RIOS",
                    "OFF", "STOCK:09:00-12:00 / SBY", " / 17:00-00:00",
                    "STOCK:09:00-16:00 / 16:00-22:00", "12:00-16:00 / 17:00-22:00",
                    " / 17:00-01:00", "REQ"},
            {"FELIX WYNN",
                    "09:00-14:00 / REQ", "REQ / 09:00-16:00",
                    "STOCK:09:00-13:00 / 17:00-22:00", "OFF", "OFF",
                    "12:00-16:00 / 17:00-01:00", "14:00-16:00 / 17:00-00:00"},
            {"GRETA SAITO",
                    "OFF", "OFF", "OFF", " / 18:00-01:00", "HOL", "HOL", "HOL"},
            {"HUGO MENSAH", "HOL", "HOL", "HOL", "HOL", "HOL", "HOL", "HOL"},
            // Group 3 — bar back
            {"IRIS DALEY",
                    "OFF", "OFF", "OFF", "OFF", "OFF",
                    "12:00-16:00 / 17:00-01:00", "12:00-16:00 / 16:00-20:00"},
            {"JASPER LIN",
                    "12:00-16:00 / 17:00-00:00", "OFF", "OFF", " / 17:00-00:00",
                    " / 16:00-01:00", "TRN:12:00-16:00 / TRN:17:00-01:00",
                    "14:00-16:00 / 17:00-00:00"},
            {"KIRA VEGA",
                    "12:00-16:00 / 16:00-20:00", "SBY / 17:00-00:00", " / 17:00-00:00",
                    "OFF", "12:00-16:00 / 17:00-01:00",
                    "14:00-16:00 / 17:00-01:00", "OFF"}
    };

    /**
     * After which staff index to insert a section separator row. The
     * groups are 0-2 (management), 3-7 (main bar), 8-10 (bar back).
     */
    private static final int[] SEPARATORS_AFTER = {2, 7};

    // ──────────────────────── Layout constants ───────────────────────

    /**
     * Logical day count plus name column for the header band / legend
     * row (which still draw one cell per day at the row level).
     */
    private static final int LOGICAL_COLUMNS = 8;
    private static final double NAME_COL_WIDTH = 90;
    private static final double DAY_COL_WIDTH = 105;

    /**
     * Inside the schedule {@link #addTable} the day column splits into
     * four sub-cells: lunch-start | lunch-end | dinner-start | dinner-end.
     * Total columns = 1 (name) + 7 days * 4 sub-cells = 29.
     */
    private static final int TOTAL_COLUMNS = 1 + DAYS.length * 4;
    private static final double SUB_COL_WIDTH = DAY_COL_WIDTH / 4.0;

    private WeeklyScheduleFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("weekly-schedule.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.of(841.88977, 472))
                .pageBackground(PAGE)
                .margin(14, 8, 14, 8)
                .create()) {

            document.pageFlow()
                    .name("WeeklyShiftSchedule")
                    .spacing(3)

                    // Colour legend at the top — the schedule body relies on
                    // colour alone to read OFF / HOL / REQUEST / STOCK /
                    // Standby / Training / Bar Back, so this strip is the key.
                    .addRow("LegendStrip", row -> row
                            .spacing(6)
                            .weights(NAME_COL_WIDTH, DAY_COL_WIDTH, DAY_COL_WIDTH, DAY_COL_WIDTH,
                                    DAY_COL_WIDTH, DAY_COL_WIDTH, DAY_COL_WIDTH, DAY_COL_WIDTH)
                            .addSection("LegendBlank", section -> section
                                    .padding(new DocumentInsets(4, 0, 4, 0)))
                            .addSection("Lr", section -> chip(section, "REQUEST", REQUEST, DocumentColor.WHITE))
                            .addSection("Lo", section -> chip(section, "OFF", OFF, DocumentColor.WHITE))
                            .addSection("Lh", section -> chip(section, "HOL", HOL, DocumentColor.WHITE))
                            .addSection("Ls", section -> chip(section, "STOCK", STOCK, DocumentColor.WHITE))
                            .addSection("Lsb", section -> chip(section, "Standby", STANDBY, DocumentColor.WHITE))
                            .addSection("Lt", section -> chip(section, "Training", TRAINING, DocumentColor.WHITE))
                            .addSection("Lbb", section -> chip(section, "Bar Back", BARBACK, DocumentColor.WHITE)))

                    // Main schedule table — the AURORA brand cell + day-of-week
                    // header rows now live inside the table itself, sharing the
                    // schedule's grid lines instead of floating above it.
                    .addTable(table -> {
                        DocumentTableColumn[] columns = new DocumentTableColumn[TOTAL_COLUMNS];
                        columns[0] = DocumentTableColumn.fixed(NAME_COL_WIDTH);
                        for (int i = 1; i < TOTAL_COLUMNS; i++) {
                            columns[i] = DocumentTableColumn.fixed(SUB_COL_WIDTH);
                        }
                        table.columns(columns)
                                .defaultCellStyle(emptyCellStyle())
                                .headerStyle(dayHeaderStyle());

                        // Day-name header row — the AURORA brand cell on the
                        // left uses rowSpan(2) so it spans both this row and
                        // the day-note row beneath, anchoring the header band.
                        List<DocumentTableCell> dayNameRow = new ArrayList<>();
                        dayNameRow.add(DocumentTableCell.text("AURORA")
                                .withStyle(brandCellStyle()).rowSpan(2));
                        for (int d = 0; d < DAYS.length; d++) {
                            dayNameRow.add(DocumentTableCell.text(DAYS[d])
                                    .withStyle(dayNameCellStyle()).colSpan(4));
                        }
                        table.rowCells(dayNameRow);

                        // Day-note row — operational notes under each day name.
                        // The AURORA cell from the row above covers the name
                        // column, so this row only emits the seven note cells.
                        List<DocumentTableCell> dayNoteRow = new ArrayList<>();
                        for (int d = 0; d < DAY_NOTES.length; d++) {
                            dayNoteRow.add(DocumentTableCell.text(DAY_NOTES[d])
                                    .withStyle(dayNoteCellStyle()).colSpan(4));
                        }
                        table.rowCells(dayNoteRow);

                        // COVERS row — lunch covers and dinner covers each
                        // span half of a day's four sub-cells, so the
                        // numbers line up under the matching pair of
                        // shift sub-cells below.
                        List<DocumentTableCell> coversRow = new ArrayList<>();
                        coversRow.add(DocumentTableCell.text("COVERS")
                                .withStyle(coversLabelStyle()));
                        for (int d = 0; d < DAYS.length; d++) {
                            coversRow.add(DocumentTableCell.text(COVERS_LUNCH[d])
                                    .withStyle(coversCellStyle()).colSpan(2));
                            coversRow.add(DocumentTableCell.text(COVERS_DINNER[d])
                                    .withStyle(coversCellStyle()).colSpan(2));
                        }
                        table.rowCells(coversRow);

                        // Staff rows + group separators
                        for (int s = 0; s < STAFF.length; s++) {
                            table.rowCells(staffRow(STAFF[s]));
                            for (int sepIndex = 0; sepIndex < SEPARATORS_AFTER.length; sepIndex++) {
                                if (SEPARATORS_AFTER[sepIndex] == s) {
                                    table.rowCells(separatorRow());
                                }
                            }
                        }
                    })

                    // Footer notes
                    .addRow("FooterRule", row -> row
                            .spacing(8)
                            .weights(1, 0.12, 1)
                            .addSection("LeftRule", section -> section
                                    .padding(new DocumentInsets(10, 0, 0, 0))
                                    .addShape(shape -> shape.size(292, 0.6).fillColor(LOGO_GOLD).margin(DocumentInsets.zero())))
                            .addSection("Seal", section -> section
                                    .stroke(DocumentStroke.of(LOGO_GOLD, 0.5))
                                    .cornerRadius(9)
                                    .padding(new DocumentInsets(1, 0, 1, 0))
                                    .addParagraph(p -> p
                                            .text("S")
                                            .align(TextAlign.CENTER)
                                            .textStyle(DocumentTextStyle.builder()
                                                    .fontName(FontName.TIMES_BOLD)
                                                    .size(13)
                                                    .color(LOGO_GOLD)
                                                    .build())
                                            .margin(DocumentInsets.zero())))
                            .addSection("RightRule", section -> section
                                    .padding(new DocumentInsets(10, 0, 0, 0))
                                    .addShape(shape -> shape.size(292, 0.6).fillColor(LOGO_GOLD).margin(DocumentInsets.zero()))))

                    .addRow("FooterNotes", row -> row
                            .spacing(14)
                            .weights(1, 1)
                            .addSection("LeftNote", section -> section
                                    .padding(new DocumentInsets(2, 112, 0, 0))
                                    .addParagraph(p -> p
                                            .text("AARON — stock recon")
                                            .textStyle(footerNoteStyle(STOCK))
                                            .margin(DocumentInsets.zero())))
                            .addSection("RightNote", section -> section
                                    .padding(new DocumentInsets(2, 0, 0, 96))
                                    .addParagraph(p -> p
                                            .text("JASPER — training cover")
                                            .textStyle(footerNoteStyle(TRAINING))
                                            .align(TextAlign.RIGHT)
                                            .margin(DocumentInsets.zero()))))

                    .addSection("BuildFooter", section -> section
                                    .padding(new DocumentInsets(6, 0, 0, 0))
                            .addParagraph(p -> p
                                    .text("Composed with GraphCompose v1.5 - examples/.../WeeklyScheduleFileExample.java")
                                    .textStyle(DocumentTextStyle.builder()
                                            .fontName(FontName.COURIER)
                                            .size(7.5)
                                            .color(MUTED)
                                            .build())
                                    .margin(DocumentInsets.zero())))
                    .build();

            document.buildPdf();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }

    private static void logoBlock(com.demcha.compose.document.dsl.SectionBuilder section) {
        section
                .fillColor(SURFACE)
                .accentRight(GRID, 0.45)
                .padding(new DocumentInsets(5, 6, 8, 6))
                .spacing(2)
                .addParagraph(p -> p
                        .text("AURORA")
                        .align(TextAlign.CENTER)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.TIMES_BOLD)
                                .size(20)
                                .color(LOGO_GOLD)
                                .build())
                        .margin(DocumentInsets.zero()))
                .addShape(shape -> shape
                        .size(50, 0.6)
                        .fillColor(LOGO_GOLD)
                        .margin(new DocumentInsets(0, 14, 0, 14)))
                .addParagraph(p -> p
                        .text("*")
                        .align(TextAlign.CENTER)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.TIMES_BOLD)
                                .size(11)
                                .color(LOGO_GOLD)
                                .build())
                        .margin(DocumentInsets.zero()));
    }

    private static void dayHeader(com.demcha.compose.document.dsl.SectionBuilder section,
                                  String day,
                                  String note) {
        section
                .fillColor(SURFACE)
                .accentRight(GRID, 0.35)
                .padding(new DocumentInsets(5, 5, 7, 5))
                .spacing(3)
                .addParagraph(p -> p
                        .text(day)
                        .align(TextAlign.CENTER)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.TIMES_BOLD)
                                .size(12)
                                .color(INK)
                                .build())
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p
                        .text(note)
                        .align(TextAlign.CENTER)
                        .lineSpacing(1.2)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA_BOLD)
                                .size(5.9)
                                .color(INK)
                                .build())
                        .margin(DocumentInsets.zero()));
    }

    // ──────────────────────── Cell helpers ───────────────────────────

    /**
     * Builds one staff row: the name cell on the left, then four sub-cells
     * per day (lunch start, lunch end, dinner start, dinner end), with
     * coloured halves and full-day fills produced by {@link #dayCells}.
     */
    private static List<DocumentTableCell> staffRow(String[] entry) {
        List<DocumentTableCell> cells = new ArrayList<>();
        cells.add(DocumentTableCell.text(entry[0]).withStyle(nameCellStyle()));
        for (int i = 1; i < entry.length; i++) {
            cells.addAll(dayCells(entry[i]));
        }
        // Pad missing days so the row covers the full sub-cell grid.
        int columnsCovered = totalColumnsCovered(cells);
        while (columnsCovered < TOTAL_COLUMNS) {
            cells.add(DocumentTableCell.text("")
                    .withStyle(emptyCellStyle())
                    .colSpan(Math.min(4, TOTAL_COLUMNS - columnsCovered)));
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
     * Maps one day token to the list of cells that make up the day's
     * four sub-columns. Total {@code colSpan} of the returned cells is
     * always 4. See the {@link #STAFF} javadoc for the token grammar.
     */
    private static List<DocumentTableCell> dayCells(String token) {
        if (token == null || token.isBlank()) {
            return List.of(DocumentTableCell.text("")
                    .withStyle(emptyCellStyle()).colSpan(4));
        }

        int slash = token.indexOf('/');
        if (slash < 0) {
            // Single-token full-day shift — collapses to one merged cell.
            String single = token.trim();
            DocumentColor wholeFill = wholeDayFill(single);
            if (wholeFill != null) {
                return List.of(DocumentTableCell.text("")
                        .withStyle(statusFill(wholeFill)).colSpan(4));
            }
            // STOCK:X-Y / TRN:X-Y / X-Y as a cross-meal shift — render
            // as 4 sub-cells with start in the leftmost cell, end in the
            // rightmost cell, and a shared fill (if any) across the row.
            ParsedShift shift = parseShift(single);
            return shiftAcrossDay(shift);
        }

        String lunchToken = token.substring(0, slash).trim();
        String dinnerToken = token.substring(slash + 1).trim();

        // Both halves on the same pure status fill → merge to colSpan(4).
        DocumentColor lunchFill = wholeDayFill(lunchToken);
        DocumentColor dinnerFill = wholeDayFill(dinnerToken);
        if (lunchFill != null && lunchFill == dinnerFill) {
            return List.of(DocumentTableCell.text("")
                    .withStyle(statusFill(lunchFill)).colSpan(4));
        }

        List<DocumentTableCell> cells = new ArrayList<>();
        cells.addAll(segmentCells(lunchToken));
        cells.addAll(segmentCells(dinnerToken));
        return cells;
    }

    /**
     * Renders one half (lunch or dinner) into cells that together cover
     * {@code colSpan} of 2.
     */
    private static List<DocumentTableCell> segmentCells(String segment) {
        if (segment == null || segment.isBlank()) {
            return List.of(DocumentTableCell.text("")
                    .withStyle(emptyCellStyle()).colSpan(2));
        }
        DocumentColor fill = wholeDayFill(segment);
        if (fill != null) {
            return List.of(DocumentTableCell.text("")
                    .withStyle(statusFill(fill)).colSpan(2));
        }
        ParsedShift shift = parseShift(segment);
        DocumentTableStyle cellStyle = shift.fill == null
                ? shiftCellStyle(null)
                : shiftCellStyle(shift.fill);
        return List.of(
                DocumentTableCell.text(shift.start).withStyle(cellStyle),
                DocumentTableCell.text(shift.end).withStyle(cellStyle));
    }

    /**
     * Renders a cross-meal shift across the four sub-cells of a day.
     * The start time lands in the leftmost sub-cell, the end time in
     * the rightmost; the two middle cells stay empty. All four share
     * the shift's optional fill colour so the band reads as one
     * continuous painted strip across the day.
     */
    private static List<DocumentTableCell> shiftAcrossDay(ParsedShift shift) {
        DocumentTableStyle cellStyle = shiftCellStyle(shift.fill);
        return List.of(
                DocumentTableCell.text(shift.start).withStyle(cellStyle),
                DocumentTableCell.text("").withStyle(cellStyle),
                DocumentTableCell.text("").withStyle(cellStyle),
                DocumentTableCell.text(shift.end).withStyle(cellStyle));
    }

    private static DocumentTableStyle shiftCellStyle(DocumentColor fill) {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(6, 1, 6, 1))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA)
                        .size(6.8)
                        .color(fill == null ? INK : textOn(fill))
                        .build())
                .stroke(DocumentStroke.of(GRID, 0.3))
                .fillColor(fill)
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .build();
    }

    private record ParsedShift(String start, String end, DocumentColor fill) {
    }

    /**
     * Parses a shift token of the form {@code [STATUS:]HH:MM-HH:MM}.
     * Returns the start time, the end time, and the fill colour
     * associated with the status prefix (or {@code null} when no
     * prefix). Tokens without a {@code -} resolve to a single-time
     * pair where {@code end} is empty.
     */
    private static ParsedShift parseShift(String token) {
        DocumentColor fill = null;
        String body = token;
        if (token.startsWith("STOCK:")) {
            fill = STOCK;
            body = token.substring("STOCK:".length());
        } else if (token.startsWith("TRN:")) {
            fill = TRAINING;
            body = token.substring("TRN:".length());
        }
        int dash = body.indexOf('-');
        if (dash < 0) {
            return new ParsedShift(body, "", fill);
        }
        return new ParsedShift(body.substring(0, dash), body.substring(dash + 1), fill);
    }

    private static DocumentColor wholeDayFill(String code) {
        return switch (code) {
            case "OFF" -> OFF;
            case "HOL" -> HOL;
            case "REQ" -> REQUEST;
            case "SBY" -> STANDBY;
            case "TRN" -> TRAINING;
            case "BB" -> BARBACK;
            default -> null;
        };
    }

    private static String labelFor(String code) {
        return switch (code) {
            case "OFF" -> "OFF";
            case "HOL" -> "HOL";
            case "REQ" -> "REQUEST";
            case "SBY" -> "Standby";
            case "TRN" -> "Training";
            case "BB" -> "Bar Back";
            default -> code;
        };
    }

    private static List<DocumentTableCell> separatorRow() {
        DocumentTableStyle style = DocumentTableStyle.builder()
                .padding(new DocumentInsets(2.2, 0, 2.2, 0))
                .fillColor(SEPARATOR)
                .stroke(DocumentStroke.of(SEPARATOR, 0.3))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA)
                        .size(1)
                        .color(SEPARATOR)
                        .build())
                .build();
        return List.of(DocumentTableCell.text("").withStyle(style).colSpan(TOTAL_COLUMNS));
    }

    private static DocumentColor textOn(DocumentColor fill) {
        return fill == OFF || fill == STOCK || fill == BARBACK || fill == SEPARATOR
                ? DocumentColor.WHITE
                : INK;
    }

    // ──────────────────────── Legend chip ────────────────────────────

    private static void chip(com.demcha.compose.document.dsl.SectionBuilder section,
                             String label,
                             DocumentColor fill,
                             DocumentColor ink) {
        section
                .fillColor(fill)
                .stroke(DocumentStroke.of(GRID, 0.3))
                .padding(new DocumentInsets(5, 10, 5, 10))
                .addParagraph(p -> p
                        .text(label)
                        .align(TextAlign.CENTER)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA_BOLD)
                                .size(8.5)
                                .color(ink)
                                .build())
                        .margin(DocumentInsets.zero()));
    }

    // ──────────────────────── Styles ─────────────────────────────────

    private static DocumentTableStyle brandCellStyle() {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(8, 4, 8, 4))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.TIMES_BOLD)
                        .size(20)
                        .color(LOGO_GOLD)
                        .build())
                .stroke(DocumentStroke.of(GRID, 0.3))
                .fillColor(SURFACE)
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .build();
    }

    private static DocumentTableStyle dayNameCellStyle() {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(6, 4, 4, 4))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.TIMES_BOLD)
                        .size(11.5)
                        .color(INK)
                        .build())
                .stroke(DocumentStroke.of(GRID, 0.3))
                .fillColor(SURFACE)
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .build();
    }

    private static DocumentTableStyle dayNoteCellStyle() {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(2, 5, 6, 5))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA_BOLD)
                        .size(5.9)
                        .color(INK)
                        .build())
                .stroke(DocumentStroke.of(GRID, 0.3))
                .fillColor(SURFACE)
                .lineSpacing(1.25)
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .build();
    }

    private static DocumentTableStyle dayHeaderStyle() {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(6, 4, 6, 4))
                .fillColor(HEADER_FILL)
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA_BOLD)
                        .size(9.3)
                        .color(BRAND_DARK)
                        .build())
                .stroke(DocumentStroke.of(GRID, 0.3))
                .lineSpacing(1.3)
                .build();
    }

    private static DocumentTableStyle blankHeaderCell() {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(4, 4, 4, 4))
                .stroke(DocumentStroke.of(GRID, 0.3))
                .fillColor(NAME_FILL)
                .build();
    }

    private static DocumentTableStyle statusFill(DocumentColor fill) {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(8.5, 4, 8.5, 4))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA_BOLD)
                        .size(8.5)
                        .color(textOn(fill))
                        .build())
                .stroke(DocumentStroke.of(GRID, 0.3))
                .fillColor(fill)
                .build();
    }

    private static DocumentTableStyle emptyCellStyle() {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(6, 5, 6, 5))
                .stroke(DocumentStroke.of(GRID, 0.3))
                .fillColor(SURFACE)
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA)
                        .size(8)
                        .color(INK)
                        .build())
                .textAnchor(DocumentTableTextAnchor.CENTER_LEFT)
                .build();
    }

    private static DocumentTableStyle nameCellStyle() {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(7, 6, 7, 6))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA_BOLD)
                        .size(9.1)
                        .color(BRAND_DARK)
                        .build())
                .stroke(DocumentStroke.of(GRID, 0.3))
                .fillColor(NAME_FILL)
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .build();
    }

    private static DocumentTableStyle coversLabelStyle() {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(5, 6, 5, 6))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA_BOLD)
                        .size(8.8)
                        .color(DocumentColor.WHITE)
                        .build())
                .stroke(DocumentStroke.of(GRID, 0.3))
                .fillColor(BRAND_DARK)
                .lineSpacing(1.35)
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .build();
    }

    private static DocumentTableStyle coversCellStyle() {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(5, 4, 5, 4))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA_BOLD)
                        .size(8.8)
                        .color(INK)
                        .build())
                .stroke(DocumentStroke.of(GRID, 0.3))
                .fillColor(COVERS_FILL)
                .lineSpacing(1.35)
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .build();
    }

    private static DocumentTextStyle footerNoteStyle(DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(8.4)
                .color(color)
                .build();
    }
}
