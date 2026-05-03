package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Saturday service schedule for the fictional <em>Bar Lumière</em> —
 * three management roles, four bartenders working the main bar shift,
 * and a three-person backyard BBQ crew that takes over once the
 * kitchen flips. Composed directly through the canonical DSL with
 * {@code BusinessTheme.modern()} so the rendered PDF looks closer to a
 * production-floor handout than a generic timetable.
 *
 * <p>Replace the staff arrays at the top of this file with your own
 * data; the layout and styling are fully theme-driven through
 * {@link BusinessTheme}.</p>
 */
public final class WeeklyScheduleFileExample {
    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(112, 116, 128);
    private static final DocumentColor BRAND = DocumentColor.rgb(20, 80, 95);
    private static final DocumentColor BRAND_DEEP = DocumentColor.rgb(14, 56, 70);
    private static final DocumentColor ACCENT = DocumentColor.rgb(196, 153, 76);
    private static final DocumentColor SOFT_PANEL = DocumentColor.rgb(248, 244, 234);
    private static final DocumentColor ZEBRA = DocumentColor.rgb(242, 236, 222);

    private static final String[][] MANAGEMENT = {
            // role, name, shift, contact, note
            {"Floor manager", "Aleksandr Petrenko", "16:00 - 23:30", "+44 20 7946 0234", "Owns service and customer escalations"},
            {"Shift lead", "Cassia Romero", "17:30 - 02:00", "+44 20 7946 0285", "Closes register, end-of-night cash drop"},
            {"General manager", "Tobias Wegner", "On call", "+44 20 7946 0301", "Reachable for exceptions only"}
    };

    private static final String[][] BARTENDERS = {
            // station, name, shift, signature drink, status
            {"Main bar A", "Eitan Brakha", "16:00 - 23:30", "Negroni, Old Fashioned", "Lead"},
            {"Main bar B", "Yuki Watanabe", "16:00 - 23:30", "Highballs, Whisky sours", "Confirmed"},
            {"Service bar", "Lillian Park", "18:00 - 02:00", "Martinis, Aperitivo", "Confirmed"},
            {"Floor / runner", "Marcus Reilly", "18:00 - 02:00", "Beers, low-ABV", "Confirmed"}
    };

    private static final String[][] BBQ = {
            // role, name, shift, station, note
            {"Pitmaster", "Diego Salinas", "20:00 - 01:00", "Backyard grill", "Brisket, ribs, chicken thighs"},
            {"Prep", "Anya Volkova", "18:30 - 23:30", "Backyard prep", "Marinades, sides, plating"},
            {"Runner / service", "Kris Bjornsen", "20:00 - 01:00", "Pass-through", "Grill / floor coordination"}
    };

    private WeeklyScheduleFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("weekly-schedule.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4.landscape())
                .pageBackground(THEME.pageBackground())
                .margin(34, 38, 34, 38)
                .create()) {

            document.pageFlow()
                    .name("BarLumiereSchedule")
                    .spacing(14)
                    // ───── Hero ─────
                    .addRow("HeroRow", row -> row
                            .spacing(18)
                            .weights(2, 1)
                            .addSection("Hero", section -> section
                                    .softPanel(THEME.palette().surfaceMuted(), 12, 18)
                                    .accentLeft(ACCENT, 5)
                                    .spacing(6)
                                    .addParagraph(p -> p
                                            .text("BAR LUMIÈRE")
                                            .textStyle(DocumentTextStyle.builder()
                                                    .fontName(FontName.HELVETICA_BOLD)
                                                    .size(9)
                                                    .color(MUTED)
                                                    .build())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text("Saturday service schedule")
                                            .textStyle(THEME.text().h1())
                                            .margin(DocumentInsets.zero()))
                                    .addRich(rich -> rich
                                            .plain("Three management roles, four bartenders on the main bar, and a three-person ")
                                            .accent("backyard BBQ crew", BRAND)
                                            .plain(" that takes over once the kitchen flips at 20:00.")))
                            .addSection("KeyFigures", section -> section
                                    .softPanel(DocumentColor.WHITE, 10, 14)
                                    .stroke(DocumentStroke.of(THEME.palette().rule(), 0.5))
                                    .accentTop(BRAND, 3)
                                    .spacing(4)
                                    .addParagraph(p -> p
                                            .text("Saturday 25 May 2026")
                                            .textStyle(DocumentTextStyle.builder()
                                                    .fontName(FontName.HELVETICA_BOLD)
                                                    .size(11)
                                                    .color(INK)
                                                    .build())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text("Service: 16:00 -02:00")
                                            .textStyle(body())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text("BBQ flip: 20:00")
                                            .textStyle(body())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text("Cash-up: 02:30")
                                            .textStyle(caption())
                                            .margin(DocumentInsets.zero()))))

                    .addTable(table -> {
                        baseTable(table, new double[]{130, 150, 110, 130, 200}, new String[]{"Role", "Name", "Shift", "Contact", "Note"});
                        table.zebra(DocumentColor.WHITE, ZEBRA);
                        for (String[] row : MANAGEMENT) {
                            table.row(row);
                        }
                    })

                    // ───── Bartenders — 4 stations ─────
                    .addParagraph(p -> p
                            .text("Main bar")
                            .textStyle(THEME.text().h2())
                            .margin(DocumentInsets.zero()))

                    .addTable(table -> {
                        baseTable(table, new double[]{130, 150, 120, 200, 100}, new String[]{"Station", "Bartender", "Shift", "Signature pours", "Status"});
                        table.zebra(DocumentColor.WHITE, ZEBRA);
                        for (String[] row : BARTENDERS) {
                            table.row(row);
                        }
                    })

                    // ───── BBQ team — 3 roles ─────
                    .addParagraph(p -> p
                            .text("Backyard BBQ")
                            .textStyle(THEME.text().h2())
                            .margin(DocumentInsets.zero()))

                    .addTable(table -> {
                        baseTable(table, new double[]{130, 150, 120, 130, 180}, new String[]{"Role", "Name", "Shift", "Station", "Note"});
                        table.zebra(SOFT_PANEL, ZEBRA);
                        for (String[] row : BBQ) {
                            table.row(row);
                        }
                    })


                    // ───── Footer notes ─────
                    .addRow("FooterRow", row -> row
                            .spacing(14)
                            .weights(2, 1, 1)
                            .addSection("Notes", section -> section
                                    .softPanel(DocumentColor.WHITE, 6, 10)
                                    .stroke(DocumentStroke.of(THEME.palette().rule(), 0.5))
                                    .accentLeft(ACCENT, 2)
                                    .spacing(2)
                                    .addParagraph(p -> p
                                            .text("Briefing & changeovers")
                                            .textStyle(headerStrong())
                                            .margin(DocumentInsets.zero()))
                                    .addRich(rich -> rich
                                            .plain("Pre-shift huddle ")
                                            .bold("15:45")
                                            .plain(" at the back-of-house corner. BBQ crew briefs separately at ")
                                            .bold("19:30")
                                            .plain("."))
                                    .addRich(rich -> rich
                                            .plain("Service bar takes over from main bar at ")
                                            .accent("23:30", BRAND)
                                            .plain("; runner moves to floor coverage.")))
                            .addSection("Allergens", section -> section
                                    .softPanel(DocumentColor.WHITE, 6, 10)
                                    .stroke(DocumentStroke.of(THEME.palette().rule(), 0.5))
                                    .accentLeft(BRAND, 2)
                                    .spacing(2)
                                    .addParagraph(p -> p
                                            .text("Allergens")
                                            .textStyle(headerStrong())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text("BBQ run uses peanut oil. Inform front-of-house at handover.")
                                            .textStyle(caption())
                                            .lineSpacing(1.4)
                                            .margin(DocumentInsets.zero())))
                            .addSection("ContactCard", section -> section
                                    .softPanel(DocumentColor.WHITE, 6, 10)
                                    .stroke(DocumentStroke.of(BRAND_DEEP, 0.8))
                                    .accentLeft(BRAND_DEEP, 3)
                                    .spacing(2)
                                    .addParagraph(p -> p
                                            .text("On-call escalation")
                                            .textStyle(DocumentTextStyle.builder()
                                                    .fontName(FontName.HELVETICA_BOLD)
                                                    .size(8.5)
                                                    .color(MUTED)
                                                    .build())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text("Tobias Wegner - GM")
                                            .textStyle(DocumentTextStyle.builder()
                                                    .fontName(FontName.HELVETICA_BOLD)
                                                    .size(11)
                                                    .color(BRAND_DEEP)
                                                    .build())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text("+44 20 7946 0301")
                                            .textStyle(DocumentTextStyle.builder()
                                                    .fontName(FontName.COURIER_BOLD)
                                                    .size(10.5)
                                                    .color(INK)
                                                    .build())
                                            .margin(DocumentInsets.zero()))))

                    .addSection("Footer", section -> section
                            .accentTop(THEME.palette().rule(), 0.6)
                            .padding(new DocumentInsets(8, 0, 0, 0))
                            .addRich(rich -> rich
                                    .plain("Composed with GraphCompose v1.5 - ")
                                    .style("examples/.../WeeklyScheduleFileExample.java", DocumentTextStyle.builder()
                                            .fontName(FontName.COURIER)
                                            .size(8)
                                            .color(MUTED)
                                            .build())))
                    .build();

            document.buildPdf();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }

    private static void sectionHeader(com.demcha.compose.document.dsl.SectionBuilder section,
                                      String title,
                                      String subtitle) {
        section
                .accentLeft(BRAND, 3)
                .padding(new DocumentInsets(0, 12, 0, 12))
                .spacing(2)
                .addParagraph(p -> p
                        .text(title)
                        .textStyle(THEME.text().h2())
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p
                        .text(subtitle)
                        .textStyle(caption())
                        .margin(DocumentInsets.zero()));
    }

    /**
     * Builds the columns from the supplied widths array — values > 0 map
     * to fixed-width columns, value 0 maps to {@code DocumentTableColumn.auto()}
     * which absorbs the remaining inner width on the row.
     */
    private static void baseTable(TableBuilder table,
                                  double[] columnWidths,
                                  String[] headers) {
        DocumentTableColumn[] columns = new DocumentTableColumn[columnWidths.length];
        for (int i = 0; i < columnWidths.length; i++) {
            columns[i] = columnWidths[i] > 0
                    ? DocumentTableColumn.fixed(columnWidths[i])
                    : DocumentTableColumn.auto();
        }
        table.columns(columns)
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(new DocumentInsets(7, 9, 7, 9))
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA)
                                .size(9.5)
                                .color(INK)
                                .build())
                        .stroke(DocumentStroke.of(THEME.palette().rule(), 0.4))
                        .build())
                .headerStyle(DocumentTableStyle.builder()
                        .padding(new DocumentInsets(8, 9, 8, 9))
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA_BOLD)
                                .size(9.5)
                                .color(DocumentColor.WHITE)
                                .build())
                        .fillColor(BRAND_DEEP)
                        .stroke(DocumentStroke.of(BRAND_DEEP, 0.4))
                        .build())
                .headerRow(headers);
    }

    private static DocumentTextStyle body() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(10)
                .color(INK)
                .build();
    }

    private static DocumentTextStyle caption() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(8.6)
                .color(MUTED)
                .build();
    }

    private static DocumentTextStyle headerStrong() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(8.5)
                .color(MUTED)
                .build();
    }
}
