package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.builtins.WeeklyScheduleTemplateV1;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

public final class WeeklyScheduleFileExample {

    private WeeklyScheduleFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("weekly-schedule.pdf");
        WeeklyScheduleTemplateV1 template = new WeeklyScheduleTemplateV1();

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4.landscape())
                .margin(18, 18, 18, 18)
                .create()) {
            template.compose(document, ExampleDataFactory.sampleWeeklySchedule());
            document.buildPdf();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
