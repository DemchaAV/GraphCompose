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
import com.demcha.compose.document.theme.BusinessTheme;
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
 * <p>Black separator rows split the team into management, main bar, and
 * bar back / BBQ groups. Composed end-to-end through the canonical DSL
 * with per-cell {@code DocumentTableCell.withStyle(...)} overrides on a
 * single 8-column table.</p>
 */
public final class WeeklyScheduleFileExample {

    // ──────────────────────── Theme + colours ────────────────────────

    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(112, 116, 128);
    private static final DocumentColor BRAND = DocumentColor.rgb(20, 80, 95);
    private static final DocumentColor LOGO_GOLD = DocumentColor.rgb(176, 138, 70);
    private static final DocumentColor RULE = DocumentColor.rgb(120, 120, 120);

    private static final DocumentColor REQUEST = DocumentColor.rgb(190, 190, 190);
    private static final DocumentColor OFF = DocumentColor.rgb(200, 40, 40);
    private static final DocumentColor HOL = DocumentColor.rgb(245, 200, 40);
    private static final DocumentColor STOCK = DocumentColor.rgb(105, 175, 85);
    private static final DocumentColor STANDBY = DocumentColor.rgb(206, 188, 230);
    private static final DocumentColor TRAINING = DocumentColor.rgb(240, 160, 80);
    private static final DocumentColor BARBACK = DocumentColor.rgb(175, 135, 100);
    private static final DocumentColor SEPARATOR = DocumentColor.rgb(20, 20, 20);

    // ──────────────────────── Schedule data ──────────────────────────

    private static final String[] DAYS = {
            "Monday 4th", "Tuesday 5th", "Wednesday 6th", "Thursday 7th",
            "Friday 8th", "Saturday 9th", "Sunday 10th"
    };

    private static final String[] DAY_NOTES = {
            "Bank Holiday",
            "Pianist 18:30",
            "TEAM DAY OUT",
            "Ex Hire 13PAX",
            "Pianist 19:00",
            "Ex Hire 44PAX",
            "Masterclass 4PAX"
    };

    private static final String[] COVERS_TOP = {"163", "21", "52", "35", "82", "131", "130"};
    private static final String[] COVERS_BOTTOM = {"35", "63", "74", "75", "78", "149", "36"};

    /**
     * Status code legend tokens used inside {@link #STAFF} entries:
     * <ul>
     *   <li>{@code OFF} — full red fill, no text.</li>
     *   <li>{@code HOL} — full yellow fill.</li>
     *   <li>{@code REQ} — gray "request" fill.</li>
     *   <li>{@code SBY} — light purple "standby" fill.</li>
     *   <li>{@code TRN} — orange "training" fill.</li>
     *   <li>{@code BB}  — brown "bar back" fill.</li>
     *   <li>{@code STOCK:11:00-18:00} — green stock-recon shift with time text.</li>
     *   <li>Plain {@code 11:00-16:00 / 16:00-22:00} — shift times on white.</li>
     *   <li>Empty string — empty white cell.</li>
     * </ul>
     */
    private static final String[][] STAFF = {
            // Group 1 — management
            {"SERGII",
                    "STOCK:09:00-18:00", "OFF", "OFF", "16:00-00:00", "OFF",
                    "11:00-16:00 / 16:00-22:00", "08:00-18:00"},
            {"MARK",
                    "16:00-00:00", "REQ", "OFF", "SBY / 16:00-00:00", "12:00-16:00 / 17:00-01:00",
                    "REQ", "12:00-16:00 / STOCK:17:00-00:00"},
            {"ALEX", "OFF", "OFF", "OFF", "OFF", "OFF", "OFF", "OFF"},
            // Group 2 — main bar
            {"BIANCA",
                    "12:00-20:00 / SBY", "OFF / 17:00-00:00", "OFF", "17:00-00:00 / SBY",
                    "SBY", "OFF", "09:00-20:00"},
            {"ARTEM",
                    "OFF", "STOCK:09:00-18:00 / SBY", "17:00-00:00 / OFF",
                    "16:00-22:00 / STOCK:09:00-16:00", "17:00-22:00 / 12:00-16:00",
                    "17:00-01:00 / REQ", "REQ"},
            {"KHARREN",
                    "09:00-18:00 / REQ", "REQ / 09:00-16:00",
                    "17:00-22:00 / STOCK:09:00-18:00", "OFF", "OFF",
                    "12:00-16:00 / 17:00-01:00", "14:00-16:00 / 17:00-00:00"},
            {"DARIIA",
                    "OFF", "OFF", "OFF", "18:00-01:00", "HOL", "HOL", "HOL"},
            {"VIOLETTA", "HOL", "HOL", "HOL", "HOL", "HOL", "HOL", "HOL"},
            // Group 3 — bar back
            {"PETER",
                    "OFF", "OFF", "OFF", "OFF", "OFF",
                    "12:00-16:00 / 17:00-01:00", "12:00-20:00"},
            {"DMYTRO",
                    "12:00-16:00 / 17:00-00:00", "OFF", "OFF", "17:00-00:00 / SBY",
                    "16:00-01:00", "TRN:12:00-16:00 / TRN:17:00-01:00",
                    "14:00-16:00 / 17:00-00:00"},
            {"DANIEL",
                    "12:00-20:00", "SBY / 17:00-00:00", "17:00-00:00",
                    "OFF", "12:00-16:00 / 17:00-01:00",
                    "14:00-16:00 / 17:00-01:00", "OFF"}
    };

    /**
     * After which staff index to insert a black separator row. The
     * groups are 0-2 (management), 3-7 (main bar), 8-10 (bar back).
     */
    private static final int[] SEPARATORS_AFTER = {2, 7};

    // ──────────────────────── Layout constants ───────────────────────

    private static final int TOTAL_COLUMNS = 8;
    private static final double NAME_COL_WIDTH = 84;
    private static final double DAY_COL_WIDTH = 100;

    private WeeklyScheduleFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("weekly-schedule.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4.landscape())
                .pageBackground(DocumentColor.WHITE)
                .margin(22, 24, 22, 24)
                .create()) {

            document.pageFlow()
                    .name("WeeklyShiftSchedule")
                    .spacing(6)

                    // Logo + week label
                    .addRow("LogoBand", row -> row
                            .spacing(14)
                            .weights(1, 4)
                            .addSection("Logo", section -> section
                                    .padding(new DocumentInsets(2, 0, 0, 0))
                                    .addParagraph(p -> p
                                            .text("ASHWOOD'S")
                                            .textStyle(DocumentTextStyle.builder()
                                                    .fontName(FontName.TIMES_BOLD)
                                                    .size(28)
                                                    .color(LOGO_GOLD)
                                                    .build())
                                            .margin(DocumentInsets.zero())))
                            .addSection("WeekLabel", section -> section
                                    .padding(new DocumentInsets(8, 0, 0, 0))
                                    .addParagraph(p -> p
                                            .text("Weekly Service Rota — week of Monday 4 May 2026")
                                            .textStyle(DocumentTextStyle.builder()
                                                    .fontName(FontName.HELVETICA)
                                                    .size(10.5)
                                                    .color(MUTED)
                                                    .build())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text("Bar floor + service · 11 staff · 7 days")
                                            .textStyle(DocumentTextStyle.builder()
                                                    .fontName(FontName.HELVETICA_BOLD)
                                                    .size(10)
                                                    .color(BRAND)
                                                    .build())
                                            .margin(DocumentInsets.zero()))))

                    // Status legend strip
                    .addRow("LegendStrip", row -> row
                            .spacing(6)
                            .addSection("Lr", section -> chip(section, "REQUEST", REQUEST, INK))
                            .addSection("Lo", section -> chip(section, "OFF", OFF, DocumentColor.WHITE))
                            .addSection("Lh", section -> chip(section, "HOL", HOL, INK))
                            .addSection("Ls", section -> chip(section, "STOCK", STOCK, DocumentColor.WHITE))
                            .addSection("Lsb", section -> chip(section, "Standby", STANDBY, INK))
                            .addSection("Lt", section -> chip(section, "Training", TRAINING, INK))
                            .addSection("Lbb", section -> chip(section, "Bar Back", BARBACK, DocumentColor.WHITE)))

                    // Main schedule table
                    .addTable(table -> {
                        DocumentTableColumn[] columns = new DocumentTableColumn[TOTAL_COLUMNS];
                        columns[0] = DocumentTableColumn.fixed(NAME_COL_WIDTH);
                        for (int i = 1; i < TOTAL_COLUMNS; i++) {
                            columns[i] = DocumentTableColumn.fixed(DAY_COL_WIDTH);
                        }
                        table.columns(columns)
                                .defaultCellStyle(emptyCellStyle())
                                .headerStyle(dayHeaderStyle());

                        // Day header row
                        List<DocumentTableCell> headerRow = new ArrayList<>();
                        headerRow.add(DocumentTableCell.text("").withStyle(blankHeaderCell()));
                        for (int d = 0; d < DAYS.length; d++) {
                            headerRow.add(DocumentTableCell.lines(DAYS[d], DAY_NOTES[d])
                                    .withStyle(dayHeaderStyle()));
                        }
                        table.rowCells(headerRow);

                        // COVERS row — two numbers per day on one line
                        List<DocumentTableCell> coversRow = new ArrayList<>();
                        coversRow.add(DocumentTableCell.text("COVERS").withStyle(coversLabelStyle()));
                        for (int d = 0; d < DAYS.length; d++) {
                            coversRow.add(DocumentTableCell
                                    .text(COVERS_TOP[d] + "  ·  " + COVERS_BOTTOM[d])
                                    .withStyle(coversCellStyle()));
                        }
                        table.rowCells(coversRow);

                        // Staff rows + group separators
                        for (int s = 0; s < STAFF.length; s++) {
                            table.rowCells(staffRow(STAFF[s]));
                            for (int sep : SEPARATORS_AFTER) {
                                if (sep == s) {
                                    table.rowCells(separatorRow());
                                }
                            }
                        }
                    })

                    // Footer notes
                    .addRow("FooterNotes", row -> row
                            .spacing(14)
                            .weights(1, 1)
                            .addSection("LeftNote", section -> section
                                    .padding(new DocumentInsets(8, 0, 0, 0))
                                    .addParagraph(p -> p
                                            .text("Sergii — stock recon")
                                            .textStyle(footerNoteStyle())
                                            .margin(DocumentInsets.zero())))
                            .addSection("RightNote", section -> section
                                    .padding(new DocumentInsets(8, 0, 0, 0))
                                    .addParagraph(p -> p
                                            .text("Dima — bartender support")
                                            .textStyle(footerNoteStyle())
                                            .margin(DocumentInsets.zero()))))

                    .addSection("BuildFooter", section -> section
                            .padding(new DocumentInsets(6, 0, 0, 0))
                            .addParagraph(p -> p
                                    .text("Composed with GraphCompose v1.5 — examples/.../WeeklyScheduleFileExample.java")
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

    // ──────────────────────── Cell helpers ───────────────────────────

    private static List<DocumentTableCell> staffRow(String[] entry) {
        List<DocumentTableCell> cells = new ArrayList<>();
        cells.add(DocumentTableCell.text(entry[0]).withStyle(nameCellStyle()));
        for (int i = 1; i < entry.length; i++) {
            cells.add(dayCell(entry[i]));
        }
        while (cells.size() < TOTAL_COLUMNS) {
            cells.add(DocumentTableCell.text("").withStyle(emptyCellStyle()));
        }
        return cells;
    }

    /**
     * Maps a day token to a {@link DocumentTableCell}. Tokens:
     * <ul>
     *   <li>{@code OFF | HOL | REQ | SBY | TRN | BB} — full coloured cell.</li>
     *   <li>{@code STOCK:HH:MM-HH:MM} — green-tinted shift (single line).</li>
     *   <li>{@code TRN:HH:MM-HH:MM} — orange-tinted training shift.</li>
     *   <li>{@code HH:MM-HH:MM / HH:MM-HH:MM} — two shift lines on white.</li>
     *   <li>{@code HH:MM-HH:MM / SBY} — first shift on white, second slot
     *       on standby colour, rendered as a single mixed cell.</li>
     *   <li>Empty string — empty white cell.</li>
     * </ul>
     */
    private static DocumentTableCell dayCell(String token) {
        if (token == null || token.isBlank()) {
            return DocumentTableCell.text("").withStyle(emptyCellStyle());
        }
        // Whole-day status fills.
        DocumentColor whole = wholeDayFill(token);
        if (whole != null) {
            return DocumentTableCell.text("").withStyle(statusFill(whole));
        }
        // Mixed token — split on " / " into 1-2 segments. Each segment can
        // be a status code (renders as coloured stripe label) or a time.
        // A whole-cell tint only applies when both halves share a status
        // code; otherwise the text renders on white.
        String[] segments = token.split(" / ");
        String[] lines = new String[segments.length];
        DocumentColor sharedFill = null;
        boolean firstSegment = true;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();
            DocumentColor segFill = null;
            String text = segment;
            if (segment.startsWith("STOCK:")) {
                segFill = STOCK;
                text = segment.substring("STOCK:".length());
            } else if (segment.startsWith("TRN:")) {
                segFill = TRAINING;
                text = segment.substring("TRN:".length());
            } else {
                DocumentColor solo = wholeDayFill(segment);
                if (solo != null) {
                    segFill = solo;
                    text = labelFor(segment);
                }
            }
            lines[i] = text;
            if (firstSegment) {
                sharedFill = segFill;
                firstSegment = false;
            } else if (segFill != sharedFill) {
                sharedFill = null;
            }
        }
        DocumentTableStyle cellStyle = DocumentTableStyle.builder()
                .padding(new DocumentInsets(4, 4, 4, 4))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA)
                        .size(7.6)
                        .color(sharedFill == null ? INK
                                : (sharedFill == OFF || sharedFill == STOCK || sharedFill == BARBACK
                                        ? DocumentColor.WHITE : INK))
                        .build())
                .stroke(DocumentStroke.of(RULE, 0.3))
                .fillColor(sharedFill)
                .lineSpacing(1.2)
                .build();
        return DocumentTableCell.lines(lines).withStyle(cellStyle);
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
                .padding(new DocumentInsets(2, 0, 2, 0))
                .fillColor(SEPARATOR)
                .stroke(DocumentStroke.of(SEPARATOR, 0.4))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA)
                        .size(2)
                        .color(SEPARATOR)
                        .build())
                .build();
        return List.of(DocumentTableCell.text("").withStyle(style).colSpan(TOTAL_COLUMNS));
    }

    // ──────────────────────── Legend chip ────────────────────────────

    private static void chip(com.demcha.compose.document.dsl.SectionBuilder section,
                             String label,
                             DocumentColor fill,
                             DocumentColor ink) {
        section
                .fillColor(fill)
                .stroke(DocumentStroke.of(SEPARATOR, 0.4))
                .padding(new DocumentInsets(4, 10, 4, 10))
                .addParagraph(p -> p
                        .text(label)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA_BOLD)
                                .size(8.5)
                                .color(ink)
                                .build())
                        .margin(DocumentInsets.zero()));
    }

    // ──────────────────────── Styles ─────────────────────────────────

    private static DocumentTableStyle dayHeaderStyle() {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(6, 4, 6, 4))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA_BOLD)
                        .size(9.5)
                        .color(INK)
                        .build())
                .stroke(DocumentStroke.of(RULE, 0.3))
                .lineSpacing(1.3)
                .build();
    }

    private static DocumentTableStyle blankHeaderCell() {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(4, 4, 4, 4))
                .stroke(DocumentStroke.of(RULE, 0.3))
                .fillColor(DocumentColor.WHITE)
                .build();
    }

    private static DocumentTableStyle statusFill(DocumentColor fill) {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(2, 2, 2, 2))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA)
                        .size(6)
                        .color(fill)
                        .build())
                .stroke(DocumentStroke.of(RULE, 0.3))
                .fillColor(fill)
                .build();
    }

    private static DocumentTableStyle emptyCellStyle() {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(4, 4, 4, 4))
                .stroke(DocumentStroke.of(RULE, 0.3))
                .fillColor(DocumentColor.WHITE)
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA)
                        .size(7.6)
                        .color(INK)
                        .build())
                .build();
    }

    private static DocumentTableStyle nameCellStyle() {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(5, 6, 5, 6))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA_BOLD)
                        .size(9)
                        .color(INK)
                        .build())
                .stroke(DocumentStroke.of(RULE, 0.3))
                .build();
    }

    private static DocumentTableStyle coversLabelStyle() {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(4, 6, 4, 6))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA_BOLD)
                        .size(8.5)
                        .color(MUTED)
                        .build())
                .stroke(DocumentStroke.of(RULE, 0.3))
                .build();
    }

    private static DocumentTableStyle coversCellStyle() {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(4, 4, 4, 4))
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA_BOLD)
                        .size(8.5)
                        .color(INK)
                        .build())
                .stroke(DocumentStroke.of(RULE, 0.3))
                .build();
    }

    private static DocumentTextStyle footerNoteStyle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(9.5)
                .color(INK)
                .build();
    }
}
