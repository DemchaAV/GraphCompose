package com.demcha.templates.builtins;

import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.templates.WeeklyScheduleTheme;
import com.demcha.templates.api.WeeklyScheduleTemplate;
import com.demcha.templates.data.WeeklyScheduleData;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.util.Objects;

public class WeeklyScheduleTemplateV1 extends PdfTemplateAdapterSupport implements WeeklyScheduleTemplate {
    private static final float PAGE_MARGIN = 18f;

    private final WeeklyScheduleSceneBuilder sceneBuilder;

    public WeeklyScheduleTemplateV1() {
        this(null);
    }

    public WeeklyScheduleTemplateV1(WeeklyScheduleTheme theme) {
        WeeklyScheduleTheme resolvedTheme = Objects.requireNonNullElseGet(theme, WeeklyScheduleTheme::defaultTheme);
        this.sceneBuilder = new WeeklyScheduleSceneBuilder(resolvedTheme);
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

    @Override
    @Deprecated(forRemoval = false)
    public PDDocument render(WeeklyScheduleData data) {
        return render(data, false);
    }

    @Override
    @Deprecated(forRemoval = false)
    public PDDocument render(WeeklyScheduleData data, boolean guideLines) {
        return renderToDocument(
                guideLines,
                "Failed to generate weekly schedule",
                this::createComposer,
                composer -> compose(composer, data));
    }

    @Override
    @Deprecated(forRemoval = false)
    public void render(WeeklyScheduleData data, Path path) {
        render(data, path, false);
    }

    @Override
    @Deprecated(forRemoval = false)
    public void render(WeeklyScheduleData data, Path path, boolean guideLines) {
        renderToFile(
                path,
                guideLines,
                "Failed to generate weekly schedule",
                "Weekly schedule saved to {}",
                this::createComposer,
                composer -> compose(composer, data));
    }

    private PdfComposer createComposer(Path path, boolean guideLines) {
        return createPdfComposer(path, guideLines, landscapeA4(), PAGE_MARGIN);
    }

    @Override
    public void compose(DocumentComposer composer, WeeklyScheduleData data) {
        sceneBuilder.compose(composer, data);
    }

    private PDRectangle landscapeA4() {
        return new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    }
}
