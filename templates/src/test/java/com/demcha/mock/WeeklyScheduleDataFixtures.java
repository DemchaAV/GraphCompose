package com.demcha.mock;

import com.demcha.compose.document.templates.data.schedule.ScheduleAssignment;
import com.demcha.compose.document.templates.data.schedule.ScheduleCategory;
import com.demcha.compose.document.templates.data.schedule.ScheduleDay;
import com.demcha.compose.document.templates.data.schedule.ScheduleMetricRow;
import com.demcha.compose.document.templates.data.schedule.SchedulePerson;
import com.demcha.compose.document.templates.data.schedule.ScheduleSlot;
import com.demcha.compose.document.templates.data.schedule.WeeklyScheduleData;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class WeeklyScheduleDataFixtures {

    private WeeklyScheduleDataFixtures() {
    }

    public static WeeklyScheduleData standardSchedule() {
        return new WeeklyScheduleData(
                "Quayside Weekly Floor Schedule",
                "Week Of 30 Mar - 05 Apr 2026",
                List.of(
                        new ScheduleDay("mon", "Monday\n30th", "Clean crushed ice\nMachine & area", "request"),
                        new ScheduleDay("tue", "Tuesday\n31st", "Pianist 18:30\nClean ice machine", "off"),
                        new ScheduleDay("wed", "Wednesday\n1st", "Soul GF\nPianist FF", "hol"),
                        new ScheduleDay("thu", "Thursday\n2nd", "Terrace\ndinner\nPartner meeting\n3:30pm", "stock"),
                        new ScheduleDay("fri", "Friday\n3rd", "Good Friday\nPianist 19:00", "standby"),
                        new ScheduleDay("sat", "Saturday\n4th", "Masterclass 2x2PAX\nPianist 19:00", "training"),
                        new ScheduleDay("sun", "Sunday\n5th", "Easter Sunday\nFull stock take", "bar-back")),
                baseCategories(),
                List.of(
                        new ScheduleMetricRow("COVERS", List.of("27 / 37", "41 / 36", "30 / 29", "57 / 63", "46 / 71", "73 / 97", "155 / 26")),
                        new ScheduleMetricRow("TEAM FOCUS", List.of("Iva floor", "Glass count", "Nils lab", "Rowan lab", "Pianist", "Iva floor", "Rowan floor"))),
                List.of(
                        new SchedulePerson("rowan", "ROWAN", 10),
                        new SchedulePerson("nils", "NILS", 20),
                        new SchedulePerson("iva", "IVA", 30),
                        new SchedulePerson("pilar", "PILAR", 40),
                        new SchedulePerson("omar", "OMAR", 50),
                        new SchedulePerson("lena", "LENA", 60),
                        new SchedulePerson("freya", "FREYA", 70),
                        new SchedulePerson("noor", "NOOR", 80),
                        new SchedulePerson("bowen", "BOWEN", 90),
                        new SchedulePerson("sasha", "SASHA", 100),
                        new SchedulePerson("hugo", "HUGO", 110)),
                baseAssignments(),
                List.of(
                        "Add or remove people by editing only the people and assignments lists.",
                        "Category colours and labels are driven entirely from the shared category catalog.")
        );
    }

    public static WeeklyScheduleData withoutMetricsOrFooter() {
        WeeklyScheduleData base = standardSchedule();
        return new WeeklyScheduleData(
                base.title(),
                base.weekLabel(),
                base.days(),
                base.categories(),
                List.of(),
                base.people(),
                base.assignments(),
                List.of()
        );
    }

    public static WeeklyScheduleData withAdditionalPerson() {
        WeeklyScheduleData base = standardSchedule();
        List<SchedulePerson> people = new ArrayList<>(base.people());
        people.add(new SchedulePerson("new-joiner", "NEW JOINER", 115));

        List<ScheduleAssignment> assignments = new ArrayList<>(base.assignments());
        assignments.add(new ScheduleAssignment("new-joiner", "fri", "standby", List.of(slot("18:00", "23:00")), "Shadow shift"));
        assignments.add(new ScheduleAssignment("new-joiner", "sat", "training", List.of(slot("14:00", "20:00")), ""));

        return new WeeklyScheduleData(
                base.title(),
                base.weekLabel(),
                base.days(),
                base.categories(),
                base.headerMetrics(),
                people,
                assignments,
                base.footerNotes()
        );
    }

    public static WeeklyScheduleData withAddedAndRemovedCategory() {
        WeeklyScheduleData base = standardSchedule();

        List<ScheduleCategory> categories = new ArrayList<>();
        for (ScheduleCategory category : base.categories()) {
            if (!category.id().equals("training")) {
                categories.add(category);
            }
        }
        categories.add(new ScheduleCategory("masterclass", "MASTERCLASS", new Color(255, 227, 163), Color.BLACK, new Color(196, 145, 16)));

        List<ScheduleAssignment> assignments = new ArrayList<>(base.assignments());
        assignments.add(new ScheduleAssignment("iva", "sun", "masterclass", List.of(slot("18:00", "23:00")), "Guest setup"));

        return new WeeklyScheduleData(
                base.title(),
                base.weekLabel(),
                base.days(),
                categories,
                base.headerMetrics(),
                base.people(),
                assignments,
                base.footerNotes()
        );
    }

    private static List<ScheduleCategory> baseCategories() {
        return List.of(
                new ScheduleCategory("request", "REQUEST", new Color(166, 166, 166), Color.BLACK, new Color(80, 80, 80)),
                new ScheduleCategory("off", "OFF", new Color(205, 0, 0), Color.BLACK, new Color(110, 0, 0)),
                new ScheduleCategory("hol", "HOL", new Color(243, 196, 54), Color.BLACK, new Color(176, 126, 6)),
                new ScheduleCategory("stock", "STOCK", new Color(0, 173, 76), Color.BLACK, new Color(0, 110, 49)),
                new ScheduleCategory("standby", "STANDBY", new Color(177, 132, 226), Color.BLACK, new Color(102, 71, 150)),
                new ScheduleCategory("training", "TRAINING", new Color(245, 131, 24), Color.BLACK, new Color(183, 82, 0)),
                new ScheduleCategory("bar-back", "BAR BACK", new Color(176, 132, 76), Color.BLACK, new Color(120, 88, 44)));
    }

    private static List<ScheduleAssignment> baseAssignments() {
        return List.of(
                new ScheduleAssignment("rowan", "mon", "stock", List.of(slot("09:00", "18:00")), ""),
                new ScheduleAssignment("rowan", "thu", "request", List.of(slot("09:00", "18:00")), ""),
                new ScheduleAssignment("rowan", "fri", "training", List.of(slot("08:00", "16:00"), slot("16:00", "22:00")), ""),
                new ScheduleAssignment("rowan", "sun", "standby", List.of(slot("12:00", "16:00"), slot("16:00", "22:00")), ""),
                new ScheduleAssignment("nils", "mon", "request", List.of(slot("16:00", "00:00")), ""),
                new ScheduleAssignment("nils", "tue", "off", List.of(slot("12:00", "18:00")), ""),
                new ScheduleAssignment("nils", "wed", "request", List.of(slot("16:00", "00:00")), ""),
                new ScheduleAssignment("nils", "fri", "standby", List.of(slot("16:00", "01:00")), ""),
                new ScheduleAssignment("nils", "sun", "bar-back", List.of(slot("17:00", "03:00")), ""),
                new ScheduleAssignment("iva", "tue", "off", List.of(slot("16:00", "00:00")), ""),
                new ScheduleAssignment("iva", "wed", "request", List.of(slot("16:00", "00:00")), ""),
                new ScheduleAssignment("iva", "thu", "request", List.of(slot("16:00", "00:00")), ""),
                new ScheduleAssignment("iva", "fri", "standby", List.of(slot("16:00", "22:00")), ""),
                new ScheduleAssignment("iva", "sat", "training", List.of(slot("12:00", "16:00"), slot("17:00", "01:00")), ""),
                new ScheduleAssignment("pilar", "thu", "standby", List.of(slot("16:00", "00:00")), ""),
                new ScheduleAssignment("pilar", "fri", "standby", List.of(slot("17:00", "01:00")), ""),
                new ScheduleAssignment("pilar", "sat", "training", List.of(slot("12:00", "16:00"), slot("17:00", "01:00")), ""),
                new ScheduleAssignment("pilar", "sun", "request", List.of(slot("12:00", "16:00"), slot("17:00", "00:00")), ""),
                new ScheduleAssignment("omar", "tue", "off", List.of(slot("17:00", "00:00")), ""),
                new ScheduleAssignment("omar", "wed", "request", List.of(slot("17:00", "00:00")), ""),
                new ScheduleAssignment("omar", "thu", "request", List.of(slot("17:00", "00:00")), ""),
                new ScheduleAssignment("omar", "fri", "standby", List.of(slot("17:00", "01:00")), ""),
                new ScheduleAssignment("omar", "sat", "training", List.of(slot("12:00", "16:00"), slot("17:00", "01:00")), ""),
                new ScheduleAssignment("lena", "wed", "request", List.of(slot("09:00", "17:00")), ""),
                new ScheduleAssignment("lena", "thu", "request", List.of(slot("09:00", "17:00")), ""),
                new ScheduleAssignment("lena", "fri", "standby", List.of(slot("09:00", "17:00")), ""),
                new ScheduleAssignment("lena", "sat", "training", List.of(slot("16:00", "22:00")), ""),
                new ScheduleAssignment("lena", "sun", "request", List.of(slot("09:00", "16:00"), slot("17:00", "22:00")), ""),
                new ScheduleAssignment("freya", "sat", "training", List.of(slot("16:00", "01:00")), ""),
                new ScheduleAssignment("freya", "sun", "request", List.of(slot("12:00", "16:00"), slot("17:00", "00:00")), ""),
                new ScheduleAssignment("noor", "tue", "request", List.of(slot("09:00", "17:00")), ""),
                new ScheduleAssignment("noor", "wed", "request", List.of(slot("09:00", "18:00")), ""),
                new ScheduleAssignment("noor", "fri", "standby", List.of(slot("16:00", "22:00")), ""),
                new ScheduleAssignment("noor", "sat", "request", List.of(slot("09:00", "17:00")), ""),
                new ScheduleAssignment("bowen", "fri", "standby", List.of(slot("16:00", "01:00")), ""),
                new ScheduleAssignment("bowen", "sat", "training", List.of(slot("12:00", "16:00"), slot("17:00", "01:00")), ""),
                new ScheduleAssignment("sasha", "mon", "request", List.of(slot("17:00", "00:00")), ""),
                new ScheduleAssignment("sasha", "sat", "training", List.of(slot("12:00", "16:00"), slot("17:00", "01:00")), ""),
                new ScheduleAssignment("sasha", "sun", "request", List.of(slot("12:00", "16:00"), slot("17:00", "00:00")), ""),
                new ScheduleAssignment("hugo", "tue", "request", List.of(slot("17:00", "00:00")), ""),
                new ScheduleAssignment("hugo", "wed", "request", List.of(slot("17:00", "00:00")), ""),
                new ScheduleAssignment("hugo", "thu", "standby", List.of(slot("17:00", "00:00")), ""),
                new ScheduleAssignment("hugo", "fri", "standby", List.of(slot("17:00", "01:00")), ""),
                new ScheduleAssignment("hugo", "sun", "request", List.of(slot("12:00", "16:00"), slot("17:00", "00:00")), ""));
    }

    private static ScheduleSlot slot(String start, String end) {
        return new ScheduleSlot(start, end);
    }
}
