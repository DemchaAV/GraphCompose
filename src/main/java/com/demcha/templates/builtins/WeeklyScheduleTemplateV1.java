package com.demcha.templates.builtins;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.templates.WeeklyScheduleTheme;
import com.demcha.templates.api.WeeklyScheduleTemplate;
import com.demcha.templates.data.WeeklyScheduleData;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.util.Objects;

@Slf4j
public class WeeklyScheduleTemplateV1 implements WeeklyScheduleTemplate {
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
    public PDDocument render(WeeklyScheduleData data) {
        return render(data, false);
    }

    @Override
    public PDDocument render(WeeklyScheduleData data, boolean guideLines) {
        try {
            PdfComposer composer = createComposer(null, guideLines);
            sceneBuilder.design(composer, data);
            return composer.toPDDocument();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate weekly schedule", ex);
        }
    }

    @Override
    public void render(WeeklyScheduleData data, Path path) {
        render(data, path, false);
    }

    @Override
    public void render(WeeklyScheduleData data, Path path, boolean guideLines) {
        try (PdfComposer composer = createComposer(path, guideLines)) {
            sceneBuilder.design(composer, data);
            composer.build();
            log.info("Weekly schedule saved to {}", path.toAbsolutePath());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate weekly schedule", ex);
        }
    }

    private PdfComposer createComposer(Path path, boolean guideLines) {
        GraphCompose.PdfBuilder builder = path != null ? GraphCompose.pdf(path) : GraphCompose.pdf();
        return builder.pageSize(landscapeA4())
                .margin(PAGE_MARGIN, PAGE_MARGIN, PAGE_MARGIN, PAGE_MARGIN)
                .markdown(true)
                .guideLines(guideLines)
                .create();
    }

    private PDRectangle landscapeA4() {
        return new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    }
}
