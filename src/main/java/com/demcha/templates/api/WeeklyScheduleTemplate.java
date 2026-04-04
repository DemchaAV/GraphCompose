package com.demcha.templates.api;

import com.demcha.templates.data.WeeklyScheduleData;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.nio.file.Path;

/**
 * Public contract for reusable weekly schedule PDF templates.
 */
public interface WeeklyScheduleTemplate {

    String getTemplateId();

    String getTemplateName();

    default String getDescription() {
        return "";
    }

    PDDocument render(WeeklyScheduleData data);

    PDDocument render(WeeklyScheduleData data, boolean guideLines);

    void render(WeeklyScheduleData data, Path path);

    void render(WeeklyScheduleData data, Path path, boolean guideLines);
}
