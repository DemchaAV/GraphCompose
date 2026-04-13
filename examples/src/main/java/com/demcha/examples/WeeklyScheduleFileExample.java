package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.templates.builtins.WeeklyScheduleTemplateV1;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;

public final class WeeklyScheduleFileExample {

    private WeeklyScheduleFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("weekly-schedule.pdf");
        WeeklyScheduleTemplateV1 template = new WeeklyScheduleTemplateV1();

        try (DocumentComposer composer = GraphCompose.pdf(outputFile)
                .pageSize(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()))
                .margin(18, 18, 18, 18)
                .markdown(true)
                .create()) {
            template.compose(composer, ExampleDataFactory.sampleWeeklySchedule());
            composer.build();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
