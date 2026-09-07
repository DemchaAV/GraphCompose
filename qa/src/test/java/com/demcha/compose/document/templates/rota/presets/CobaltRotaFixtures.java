package com.demcha.compose.document.templates.rota.presets;

import com.demcha.compose.document.templates.data.rota.RotaCovers;
import com.demcha.compose.document.templates.data.rota.RotaDay;
import com.demcha.compose.document.templates.data.rota.RotaFooter;
import com.demcha.compose.document.templates.data.rota.RotaGroup;
import com.demcha.compose.document.templates.data.rota.RotaLegend;
import com.demcha.compose.document.templates.data.rota.RotaShift;
import com.demcha.compose.document.templates.data.rota.RotaStaff;
import com.demcha.compose.document.templates.data.rota.RotaVenue;
import com.demcha.compose.document.templates.data.rota.RotaWeek;
import com.demcha.compose.document.templates.data.rota.ShiftStatus;
import com.demcha.compose.document.templates.data.rota.StructuredRotaData;
import com.demcha.compose.document.templates.data.rota.StructuredRotaDocumentSpec;

import java.util.List;

/**
 * Shared fixture data for the Cobalt rota gates — the SAME spec feeds the smoke,
 * layout-snapshot and pixel gates, so a geometry shift the pixel budget absorbs
 * still trips the exact snapshot, and vice versa.
 *
 * <p>The canonical fixture is the preset's reference content: a bar's week —
 * seven days with their notes and covers, seven documented statuses, and twelve
 * people in three bands, whose cells between them cover every shape a rota cell
 * has: a plain span of hours, a marked day, a stacked pair of two marked halves,
 * a marked half beside a plain one, and a day left blank.</p>
 *
 * <p><strong>Twelve people is the page.</strong> The sheet holds this many on
 * one landscape page and no more; a thirteenth takes a second page, which is the
 * preset's own behaviour and not a fault. Growing the fixture therefore changes
 * what both gates are measuring.</p>
 *
 * <p>Every name here — the venue, the people, what is on each night — is
 * invented. The shape is a real week's, because that is what the gates have to
 * measure against; the content is not anyone's.</p>
 */
final class CobaltRotaFixtures {

    private CobaltRotaFixtures() {
    }

    /** One week of a bar's rota. */
    static StructuredRotaDocumentSpec canonicalRota() {
        return StructuredRotaDocumentSpec.from(StructuredRotaData.builder()
                // A short wordmark on purpose: the lockup is sized by a fixed
                // fraction of its column, so a long name wraps rather than
                // shrinking to fit.
                .venue(new RotaVenue("QUAY", "QUAYSIDE BAR", "Quayside Bar"))
                .week(new RotaWeek("WEEK COMMENCING 31 AUGUST 2026", "31 Aug - 6 Sep 2026"))
                .days(List.of(
                        day("MONDAY", "31", "ST", "/Clean Crushed Ice Machine & Area",
                                "104", "50"),
                        day("TUESDAY", "1", "ST", "Pianist 18:30 / Clean Crushed Ice Machine "
                                + "& Area", "33", "57"),
                        day("WEDNESDAY", "2", "ND", "Private dinner 44 pax / Soul GF / "
                                + "Pianist 18:30", "32", "76"),
                        day("THURSDAY", "3", "RD", "Pianist 18:30 / Partner meeting 3:30pm",
                                "22", "48"),
                        day("FRIDAY", "4", "TH", "Pianist 19:00", "65", "112"),
                        day("SATURDAY", "5", "TH", "MC 10 pax / Pianist 19:00", "73", "112"),
                        day("SUNDAY", "6", "TH", "MC 2 pax // 60 pax dinner / FF ex hire / "
                                + "Clean Cubed Ice Machine & Area", "72", "83")))
                .legend(new RotaLegend("STATUS LEGEND", "COVERS", "L", "D", List.of(
                        new RotaLegend.Entry("REQUEST", ShiftStatus.REQUEST),
                        new RotaLegend.Entry("OFF", ShiftStatus.OFF),
                        new RotaLegend.Entry("HOL", ShiftStatus.HOLIDAY),
                        new RotaLegend.Entry("STOCK", ShiftStatus.STOCK),
                        new RotaLegend.Entry("Standby", ShiftStatus.STANDBY),
                        new RotaLegend.Entry("Training", ShiftStatus.TRAINING),
                        new RotaLegend.Entry("SUPPORT", ShiftStatus.SUPPORT))))
                .groups(List.of(
                        new RotaGroup("MANAGEMENT", "management", List.of(
                                staff("ROSA",
                                        one(RotaShift.strong("09:00-18:00", ShiftStatus.STOCK)),
                                        one(off()),
                                        one(hours("16:00-01:00")),
                                        one(hours("16:00-01:00")),
                                        one(off()),
                                        one(RotaShift.strong("08:00-18:00", ShiftStatus.STOCK)),
                                        split(RotaShift.strong("08:00-16:00", ShiftStatus.STOCK),
                                                RotaShift.soft("16:00-21:00",
                                                        ShiftStatus.STOCK))),
                                staff("TOMAS",
                                        one(hours("16:00-00:00")),
                                        one(hours("16:00-00:00")),
                                        one(off()),
                                        one(off()),
                                        split(hours("11:00-16:00"), hours("16:00-01:00")),
                                        one(hours("16:00-01:00")),
                                        one(hours("16:00-01:00"))))),
                        new RotaGroup("BARTENDERS", "bartenders", List.of(
                                staff("NADIA",
                                        split(RotaShift.strong("09:00-16:00", ShiftStatus.STOCK),
                                                RotaShift.soft("16:00-22:00",
                                                        ShiftStatus.STOCK)),
                                        one(off()),
                                        one(hours("16:00-00:00")),
                                        one(off()),
                                        one(RotaShift.strong("09:00-18:00", ShiftStatus.STOCK)),
                                        one(off()),
                                        one(RotaShift.strong("09:00-18:00", ShiftStatus.STOCK))),
                                staff("ELIAS",
                                        one(off()),
                                        one(RotaShift.strong("09:00-17:00", ShiftStatus.STOCK)),
                                        one(hours("16:00-00:00")),
                                        one(hours("16:00-00:00")),
                                        split(RotaShift.strong("12:00-16:00",
                                                        ShiftStatus.TRAINING),
                                                RotaShift.soft("16:00-22:00",
                                                        ShiftStatus.TRAINING)),
                                        split(RotaShift.strong("09:00-16:00", ShiftStatus.STOCK),
                                                RotaShift.soft("16:00-22:00",
                                                        ShiftStatus.STOCK)),
                                        one(off())),
                                staff("MIRELA",
                                        one(off()),
                                        split(RotaShift.strong("09:00-16:00", ShiftStatus.STOCK),
                                                RotaShift.soft("16:00-22:00",
                                                        ShiftStatus.STOCK)),
                                        split(RotaShift.strong("09:00-16:00", ShiftStatus.STOCK),
                                                RotaShift.soft("16:00-22:00",
                                                        ShiftStatus.STOCK)),
                                        one(hours("12:00-18:00")),
                                        one(hours("16:00-01:00")),
                                        one(RotaShift.strong("16:00-01:00",
                                                ShiftStatus.TRAINING)),
                                        one(off())),
                                staff("SOFIA",
                                        split(hours("12:00-16:00"), hours("16:00-00:00")),
                                        one(holiday()),
                                        one(holiday()),
                                        one(holiday()),
                                        one(holiday()),
                                        one(holiday()),
                                        one(holiday())),
                                staff("KAROL",
                                        one(holiday()),
                                        one(off()),
                                        one(hours("12:00-18:00")),
                                        one(RotaShift.strong("09:00-18:00", ShiftStatus.STOCK)),
                                        one(hours("16:00-01:00")),
                                        none(),
                                        none()),
                                staff("HENRIK",
                                        one(holiday()),
                                        one(hours("16:00-00:00")),
                                        one(off()),
                                        one(hours("16:00-01:00")),
                                        one(off()),
                                        split(RotaShift.strong("08:00-16:00", ShiftStatus.STOCK),
                                                RotaShift.soft("16:00-21:00",
                                                        ShiftStatus.STOCK)),
                                        split(hours("12:00-16:00"), hours("16:00-00:00"))),
                                staff("LUCIA",
                                        one(off()),
                                        one(off()),
                                        one(off()),
                                        one(RotaShift.strong("16:00-01:00",
                                                ShiftStatus.TRAINING)),
                                        one(RotaShift.strong("16:00-01:00",
                                                ShiftStatus.TRAINING)),
                                        one(RotaShift.strong("16:00-01:00",
                                                ShiftStatus.TRAINING)),
                                        one(hours("16:00-00:00"))))),
                        new RotaGroup("BARBACKS", "barbacks", List.of(
                                staff("OSKAR",
                                        one(holiday()),
                                        one(holiday()),
                                        one(holiday()),
                                        one(off()),
                                        split(hours("12:00-16:00"), hours("16:00-01:00")),
                                        split(hours("12:00-16:00"), hours("16:00-01:00")),
                                        split(hours("12:00-16:00"), hours("16:00-01:00"))),
                                staff("TEODOR",
                                        one(holiday()),
                                        one(off()),
                                        one(hours("16:00-00:00")),
                                        one(off()),
                                        one(hours("16:00-01:00")),
                                        split(hours("12:00-16:00"), hours("16:00-01:00")),
                                        split(hours("12:00-16:00"), hours("16:00-01:00"))),
                                staff("VESNA",
                                        split(hours("12:00-16:00"), hours("16:00-00:00")),
                                        one(hours("16:00-00:00")),
                                        one(hours("16:00-00:00")),
                                        split(hours("12:00-16:00"),
                                                RotaShift.strong("16:00-00:00",
                                                        ShiftStatus.TRAINING)),
                                        one(holiday()),
                                        one(holiday()),
                                        one(holiday()))))))
                .footer(new RotaFooter(
                        "Times shown in 24-hour format. Shift colours follow the status "
                                + "legend above."))
                .build());
    }

    private static RotaDay day(String name, String ordinal, String suffix, String note,
                               String lunch, String dinner) {
        return new RotaDay(name, ordinal, suffix, note, new RotaCovers(lunch, dinner));
    }

    @SafeVarargs
    private static RotaStaff staff(String name, List<RotaShift>... days) {
        return new RotaStaff(name, List.of(days));
    }

    private static List<RotaShift> one(RotaShift shift) {
        return List.of(shift);
    }

    private static List<RotaShift> split(RotaShift first, RotaShift second) {
        return List.of(first, second);
    }

    /** A day nobody is down for, which is a blank cell and not a missing one. */
    private static List<RotaShift> none() {
        return List.of();
    }

    private static RotaShift hours(String text) {
        return RotaShift.hours(text);
    }

    private static RotaShift off() {
        return RotaShift.strong("OFF", ShiftStatus.OFF);
    }

    private static RotaShift holiday() {
        return RotaShift.strong("HOL", ShiftStatus.HOLIDAY);
    }
}
