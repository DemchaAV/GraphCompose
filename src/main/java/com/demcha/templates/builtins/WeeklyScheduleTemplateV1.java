package com.demcha.templates.builtins;

import com.demcha.compose.document.templates.support.LegacyTemplateMappers;
import com.demcha.compose.document.templates.support.SessionTemplateComposeTarget;
import com.demcha.compose.document.templates.support.WeeklyScheduleTemplateComposer;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.templates.WeeklyScheduleTheme;
import com.demcha.templates.api.WeeklyScheduleTemplate;
import com.demcha.templates.data.WeeklyScheduleData;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Deprecated bridge to the canonical weekly schedule template.
 */
@Deprecated(forRemoval = false)
public class WeeklyScheduleTemplateV1 extends PdfTemplateAdapterSupport implements WeeklyScheduleTemplate {
    private final WeeklyScheduleTemplateComposer composer;
    private final WeeklyScheduleSceneBuilder legacySceneBuilder;

    public WeeklyScheduleTemplateV1() {
        this(null);
    }

    public WeeklyScheduleTemplateV1(WeeklyScheduleTheme theme) {
        WeeklyScheduleTheme resolvedTheme = Objects.requireNonNullElseGet(theme, WeeklyScheduleTheme::defaultTheme);
        this.composer = new WeeklyScheduleTemplateComposer(
                Objects.requireNonNullElseGet(
                        LegacyTemplateMappers.toCanonical(resolvedTheme),
                        com.demcha.compose.document.templates.theme.WeeklyScheduleTheme::defaultTheme));
        this.legacySceneBuilder = new WeeklyScheduleSceneBuilder(resolvedTheme);
    }

    @Override
    public String getTemplateId() {
        return "weekly-schedule-v1";
    }

    @Override
    public String getTemplateName() {
        return "Weekly Schedule V1";
    }

    @Override
    public String getDescription() {
        return "A reusable landscape weekly roster with aligned day bands, metric rows, and a schedule matrix.";
    }

    @Deprecated(forRemoval = false)
    @Override
    public PDDocument render(WeeklyScheduleData data) {
        return render(data, false);
    }

    @Deprecated(forRemoval = false)
    @Override
    public PDDocument render(WeeklyScheduleData data, boolean guideLines) {
        return renderToDocumentSession(
                guideLines,
                "Failed to generate weekly schedule",
                landscapeA4(),
                18,
                18,
                18,
                18,
                session -> composer.compose(
                        new SessionTemplateComposeTarget(session),
                        LegacyTemplateMappers.toCanonical(data)));
    }

    @Deprecated(forRemoval = false)
    @Override
    public void render(WeeklyScheduleData data, Path path) {
        render(data, path, false);
    }

    @Deprecated(forRemoval = false)
    @Override
    public void render(WeeklyScheduleData data, Path path, boolean guideLines) {
        renderToFileSession(
                path,
                guideLines,
                "Failed to generate weekly schedule",
                "Weekly schedule saved to {}",
                landscapeA4(),
                18,
                18,
                18,
                18,
                session -> composer.compose(
                        new SessionTemplateComposeTarget(session),
                        LegacyTemplateMappers.toCanonical(data)));
    }

    @Override
    public void compose(DocumentComposer composer, WeeklyScheduleData data) {
        legacySceneBuilder.compose(composer, data);
    }

    private PDRectangle landscapeA4() {
        return new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    }
}
