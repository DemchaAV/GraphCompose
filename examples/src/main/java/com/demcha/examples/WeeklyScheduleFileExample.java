package com.demcha.examples;

import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.templates.builtins.WeeklyScheduleTemplateV1;

import java.nio.file.Path;

public final class WeeklyScheduleFileExample {

    private WeeklyScheduleFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("weekly-schedule.pdf");
        new WeeklyScheduleTemplateV1().render(ExampleDataFactory.sampleWeeklySchedule(), outputFile);
        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
