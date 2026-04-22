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
                "Scott's Weekly Floor Schedule",
                "Week Of 30 Mar - 05 Apr 2026",
                List.of(
                        new ScheduleDay("mon", "Monday\n30th", "Clean crushed ice\nMachine & area", "request"),
                        new ScheduleDay("tue", "Tuesday\n31st", "Pianist 18:30\nClean ice machine", "off"),
                        new ScheduleDay("wed", "Wednesday\n1st", "Motown GF\nPianist FF", "hol"),
                        new ScheduleDay("thu", "Thursday\n2nd", "Terrace\ndinner\nMGM meeting\n3:30pm", "stock"),
                        new ScheduleDay("fri", "Friday\n3rd", "Good Friday\nPianist 19:00", "standby"),
                        new ScheduleDay("sat", "Saturday\n4th", "Masterclass 2x2PAX\nPianist 19:00", "training"),
                        new ScheduleDay("sun", "Sunday\n5th", "Easter Sunday\nFull stock take", "bar-back")),
                baseCategories(),
                List.of(
                        new ScheduleMetricRow("COVERS", List.of("27 / 37", "41 / 36", "30 / 29", "57 / 63", "46 / 71", "73 / 97", "155 / 26")),
                        new ScheduleMetricRow("TEAM FOCUS", List.of("Alex floor", "Glass count", "Vee lab", "Sergii lab", "Pianist", "Alex floor", "Sergii floor"))),
                List.of(
                        new SchedulePerson("sergii", "SERGII", 10),
                        new SchedulePerson("mark", "MARK", 20),
                        new SchedulePerson("alex", "ALEX", 30),
                        new SchedulePerson("bianca", "BIANCA", 40),
                        new SchedulePerson("artem", "ARTEM", 50),
                        new SchedulePerson("kharren", "KHARREN", 60),
                        new SchedulePerson("daria", "DARIA", 70),
                        new SchedulePerson("violetta", "VIOLETTA", 80),
                        new SchedulePerson("peter", "PETER", 90),
                        new SchedulePerson("dmytro", "DMYTRO", 100),
                        new SchedulePerson("daniel", "DANIEL", 110)),
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
        assignments.add(new ScheduleAssignment("alex", "sun", "masterclass", List.of(slot("18:00", "23:00")), "Guest setup"));

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
                new ScheduleAssignment("sergii", "mon", "stock", List.of(slot("09:00", "18:00")), ""),
                new ScheduleAssignment("sergii", "thu", "request", List.of(slot("09:00", "18:00")), ""),
                new ScheduleAssignment("sergii", "fri", "training", List.of(slot("08:00", "16:00"), slot("16:00", "22:00")), ""),
                new ScheduleAssignment("sergii", "sun", "standby", List.of(slot("12:00", "16:00"), slot("16:00", "22:00")), ""),
                new ScheduleAssignment("mark", "mon", "request", List.of(slot("16:00", "00:00")), ""),
                new ScheduleAssignment("mark", "tue", "off", List.of(slot("12:00", "18:00")), ""),
                new ScheduleAssignment("mark", "wed", "request", List.of(slot("16:00", "00:00")), ""),
                new ScheduleAssignment("mark", "fri", "standby", List.of(slot("16:00", "01:00")), ""),
                new ScheduleAssignment("mark", "sun", "bar-back", List.of(slot("17:00", "03:00")), ""),
                new ScheduleAssignment("alex", "tue", "off", List.of(slot("16:00", "00:00")), ""),
                new ScheduleAssignment("alex", "wed", "request", List.of(slot("16:00", "00:00")), ""),
                new ScheduleAssignment("alex", "thu", "request", List.of(slot("16:00", "00:00")), ""),
                new ScheduleAssignment("alex", "fri", "standby", List.of(slot("16:00", "22:00")), ""),
                new ScheduleAssignment("alex", "sat", "training", List.of(slot("12:00", "16:00"), slot("17:00", "01:00")), ""),
                new ScheduleAssignment("bianca", "thu", "standby", List.of(slot("16:00", "00:00")), ""),
                new ScheduleAssignment("bianca", "fri", "standby", List.of(slot("17:00", "01:00")), ""),
                new ScheduleAssignment("bianca", "sat", "training", List.of(slot("12:00", "16:00"), slot("17:00", "01:00")), ""),
                new ScheduleAssignment("bianca", "sun", "request", List.of(slot("12:00", "16:00"), slot("17:00", "00:00")), ""),
                new ScheduleAssignment("artem", "tue", "off", List.of(slot("17:00", "00:00")), ""),
                new ScheduleAssignment("artem", "wed", "request", List.of(slot("17:00", "00:00")), ""),
                new ScheduleAssignment("artem", "thu", "request", List.of(slot("17:00", "00:00")), ""),
                new ScheduleAssignment("artem", "fri", "standby", List.of(slot("17:00", "01:00")), ""),
                new ScheduleAssignment("artem", "sat", "training", List.of(slot("12:00", "16:00"), slot("17:00", "01:00")), ""),
                new ScheduleAssignment("kharren", "wed", "request", List.of(slot("09:00", "17:00")), ""),
                new ScheduleAssignment("kharren", "thu", "request", List.of(slot("09:00", "17:00")), ""),
                new ScheduleAssignment("kharren", "fri", "standby", List.of(slot("09:00", "17:00")), ""),
                new ScheduleAssignment("kharren", "sat", "training", List.of(slot("16:00", "22:00")), ""),
                new ScheduleAssignment("kharren", "sun", "request", List.of(slot("09:00", "16:00"), slot("17:00", "22:00")), ""),
                new ScheduleAssignment("daria", "sat", "training", List.of(slot("16:00", "01:00")), ""),
                new ScheduleAssignment("daria", "sun", "request", List.of(slot("12:00", "16:00"), slot("17:00", "00:00")), ""),
                new ScheduleAssignment("violetta", "tue", "request", List.of(slot("09:00", "17:00")), ""),
                new ScheduleAssignment("violetta", "wed", "request", List.of(slot("09:00", "18:00")), ""),
                new ScheduleAssignment("violetta", "fri", "standby", List.of(slot("16:00", "22:00")), ""),
                new ScheduleAssignment("violetta", "sat", "request", List.of(slot("09:00", "17:00")), ""),
                new ScheduleAssignment("peter", "fri", "standby", List.of(slot("16:00", "01:00")), ""),
                new ScheduleAssignment("peter", "sat", "training", List.of(slot("12:00", "16:00"), slot("17:00", "01:00")), ""),
                new ScheduleAssignment("dmytro", "mon", "request", List.of(slot("17:00", "00:00")), ""),
                new ScheduleAssignment("dmytro", "sat", "training", List.of(slot("12:00", "16:00"), slot("17:00", "01:00")), ""),
                new ScheduleAssignment("dmytro", "sun", "request", List.of(slot("12:00", "16:00"), slot("17:00", "00:00")), ""),
                new ScheduleAssignment("daniel", "tue", "request", List.of(slot("17:00", "00:00")), ""),
                new ScheduleAssignment("daniel", "wed", "request", List.of(slot("17:00", "00:00")), ""),
                new ScheduleAssignment("daniel", "thu", "standby", List.of(slot("17:00", "00:00")), ""),
                new ScheduleAssignment("daniel", "fri", "standby", List.of(slot("17:00", "01:00")), ""),
                new ScheduleAssignment("daniel", "sun", "request", List.of(slot("12:00", "16:00"), slot("17:00", "00:00")), ""));
    }

    private static ScheduleSlot slot(String start, String end) {
        return new ScheduleSlot(start, end);
    }
}
