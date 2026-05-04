package com.demcha.examples;

import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.examples.support.WeeklyScheduleRenderer;
import com.demcha.examples.support.WeeklyScheduleRenderer.DayPlan;
import com.demcha.examples.support.WeeklyScheduleRenderer.DayShift;
import com.demcha.examples.support.WeeklyScheduleRenderer.Half;
import com.demcha.examples.support.WeeklyScheduleRenderer.JobTitle;
import com.demcha.examples.support.WeeklyScheduleRenderer.ShiftType;
import com.demcha.examples.support.WeeklyScheduleRenderer.StaffMember;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Bar / restaurant weekly shift schedule rendered through the
 * canonical DSL via the reusable
 * {@link WeeklyScheduleRenderer}.
 *
 * <p>This file holds only the demo data: names + roles, a week-start
 * date, day notes, and a typed shift map. All the layout, styling,
 * column widths, and colour palette live behind
 * {@link WeeklyScheduleRenderer.Theme} and
 * {@link WeeklyScheduleRenderer.Layout} so nothing is hardcoded into
 * the call site — pass your own {@code Theme.aurora()} replacement to
 * re-skin the schedule.</p>
 *
 * <p>The {@link WeeklyScheduleRenderer.DayShift} type replaces the
 * earlier cryptic string tokens. Setting {@code Tuesday = DayShift.HOLIDAY}
 * paints the whole day with the HOLIDAY fill; setting
 * {@code DayShift.lunchOnly("12:00", "17:00")} renders a lunch shift
 * with an empty dinner half. Combinations like "lunch shift + dinner
 * standby" are expressed via
 * {@code DayShift.halves(Half.shift("12:00","16:00"), Half.STANDBY)}.</p>
 */
public final class WeeklyScheduleFileExample {

    /** Anchor for the seven displayed dates — typically a Monday. */
    private static final LocalDate WEEK_START = LocalDate.of(2026, 5, 4);

    /**
     * Flat staff registry. Add or remove a {@link StaffMember} here
     * and the renderer auto-sorts by {@link JobTitle} and re-emits
     * separator rows on every group boundary.
     */
    private static final List<StaffMember> STAFF = List.of(
            new StaffMember("AARON PARK",  JobTitle.MANAGER),
            new StaffMember("BETH VANCE",  JobTitle.MANAGER),
            new StaffMember("CONNOR ITO",  JobTitle.MANAGER),
            new StaffMember("DIANA COLE",  JobTitle.BARTENDER),
            new StaffMember("ELENA RIOS",  JobTitle.BARTENDER),
            new StaffMember("FELIX WYNN",  JobTitle.BARTENDER),
            new StaffMember("GRETA SAITO", JobTitle.BARTENDER),
            new StaffMember("HUGO MENSAH", JobTitle.BARTENDER),
            new StaffMember("IRIS DALEY",  JobTitle.BAR_BACK),
            new StaffMember("JASPER LIN",  JobTitle.BAR_BACK),
            new StaffMember("KIRA VEGA",   JobTitle.BAR_BACK)
    );

    /** Per-day metadata: header note + lunch / dinner cover counts. */
    private static final List<DayPlan> WEEK = List.of(
            new DayPlan("Bank Holiday Monday / Clean Crushed Ice Machine & Area", 163, 35),
            new DayPlan("Pianist 18:30 / Clean Crushed Ice Machine & Area", 21, 63),
            new DayPlan("TEAM DAY OUT / Motown GF / Pianist 18:30", 52, 74),
            new DayPlan("Ex Hire Terrace Dinner 13PAX / Pianist 18:30 / MGM meeting 3:30pm", 35, 75),
            new DayPlan("Pianist 19:00", 82, 78),
            new DayPlan("Ex Hire FF Lunch 44PAX / Pianist 19:00", 131, 149),
            new DayPlan("Masterclass 4PAX (2x2) / Line Check / Clean Cubed Ice Machine & Area", 130, 36)
    );

    /**
     * Per-staff weekly shifts, keyed by {@link StaffMember#name()}.
     * Each value holds seven {@link DayShift} entries (Mon → Sun in
     * {@link #WEEK_START} order). Names absent from this map render
     * as a row of empty cells; {@code null} entries inside an array
     * are treated as {@link DayShift#NONE}.
     */
    private static final Map<String, DayShift[]> SHIFTS = Map.ofEntries(
            // ── Management ─────────────────────────────────────────
            Map.entry("AARON PARK", new DayShift[] {
                    DayShift.acrossDay("09:00", "18:00", ShiftType.STOCK), // Mon — stock recon all day
                    DayShift.OFF,                                          // Tue
                    DayShift.OFF,                                          // Wed
                    DayShift.dinnerOnly("16:00", "00:00"),                 // Thu — dinner only
                    DayShift.OFF,                                          // Fri
                    DayShift.shifts("11:00", "16:00", "16:00", "22:00"),   // Sat
                    DayShift.shifts("08:00", "13:00", "13:00", "18:00")    // Sun
            }),
            Map.entry("BETH VANCE", new DayShift[] {
                    DayShift.dinnerOnly("16:00", "00:00"),
                    DayShift.halves(Half.REQUEST, Half.shift("17:00", "00:00")),
                    DayShift.OFF,
                    DayShift.halves(Half.STANDBY, Half.shift("16:00", "00:00")),
                    DayShift.shifts("12:00", "16:00", "17:00", "01:00"),
                    DayShift.REQUEST,
                    DayShift.halves(Half.shift("12:00", "16:00"), Half.stock("17:00", "00:00"))
            }),
            Map.entry("CONNOR ITO", new DayShift[] {
                    DayShift.OFF, DayShift.OFF, DayShift.OFF, DayShift.OFF,
                    DayShift.OFF, DayShift.OFF, DayShift.OFF
            }),
            // ── Main Bar ───────────────────────────────────────────
            Map.entry("DIANA COLE", new DayShift[] {
                    DayShift.halves(Half.shift("12:00", "20:00"), Half.STANDBY),
                    DayShift.halves(Half.OFF, Half.shift("17:00", "00:00")),
                    DayShift.OFF,
                    DayShift.dinnerOnly("17:00", "00:00"),
                    DayShift.STANDBY,                                       // both halves SBY → colSpan(4)
                    DayShift.OFF,
                    DayShift.shifts("09:00", "14:00", "14:00", "20:00")
            }),
            Map.entry("ELENA RIOS", new DayShift[] {
                    DayShift.OFF,
                    DayShift.halves(Half.stock("09:00", "12:00"), Half.STANDBY),
                    DayShift.dinnerOnly("17:00", "00:00"),
                    DayShift.halves(Half.stock("09:00", "16:00"), Half.shift("16:00", "22:00")),
                    DayShift.shifts("12:00", "16:00", "17:00", "22:00"),
                    DayShift.dinnerOnly("17:00", "01:00"),
                    DayShift.REQUEST
            }),
            Map.entry("FELIX WYNN", new DayShift[] {
                    DayShift.halves(Half.shift("09:00", "14:00"), Half.REQUEST),
                    DayShift.halves(Half.REQUEST, Half.shift("09:00", "16:00")),
                    DayShift.halves(Half.stock("09:00", "13:00"), Half.shift("17:00", "22:00")),
                    DayShift.OFF,
                    DayShift.OFF,
                    DayShift.shifts("12:00", "16:00", "17:00", "01:00"),
                    DayShift.shifts("14:00", "16:00", "17:00", "00:00")
            }),
            Map.entry("GRETA SAITO", new DayShift[] {
                    DayShift.OFF, DayShift.OFF, DayShift.OFF,
                    DayShift.dinnerOnly("18:00", "01:00"),
                    DayShift.HOLIDAY, DayShift.HOLIDAY, DayShift.HOLIDAY
            }),
            Map.entry("HUGO MENSAH", new DayShift[] {
                    DayShift.HOLIDAY, DayShift.HOLIDAY, DayShift.HOLIDAY, DayShift.HOLIDAY,
                    DayShift.HOLIDAY, DayShift.HOLIDAY, DayShift.HOLIDAY
            }),
            // ── Bar Back ───────────────────────────────────────────
            Map.entry("IRIS DALEY", new DayShift[] {
                    DayShift.OFF, DayShift.OFF, DayShift.OFF, DayShift.OFF, DayShift.OFF,
                    DayShift.shifts("12:00", "16:00", "17:00", "01:00"),
                    DayShift.shifts("12:00", "16:00", "16:00", "20:00")
            }),
            Map.entry("JASPER LIN", new DayShift[] {
                    DayShift.shifts("12:00", "16:00", "17:00", "00:00"),
                    DayShift.OFF, DayShift.OFF,
                    DayShift.dinnerOnly("17:00", "00:00"),
                    DayShift.dinnerOnly("16:00", "01:00"),
                    DayShift.halves(Half.training("12:00", "16:00"), Half.training("17:00", "01:00")),
                    DayShift.shifts("14:00", "16:00", "17:00", "00:00")
            }),
            Map.entry("KIRA VEGA", new DayShift[] {
                    DayShift.shifts("12:00", "16:00", "16:00", "20:00"),
                    DayShift.halves(Half.STANDBY, Half.shift("17:00", "00:00")),
                    DayShift.dinnerOnly("17:00", "00:00"),
                    DayShift.OFF,
                    DayShift.shifts("12:00", "16:00", "17:00", "01:00"),
                    DayShift.shifts("14:00", "16:00", "17:00", "01:00"),
                    DayShift.OFF
            })
    );

    private WeeklyScheduleFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("weekly-schedule.pdf");
        WeeklyScheduleRenderer.renderTo(outputFile, "AURORA", WEEK_START, STAFF, WEEK, SHIFTS);
        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
