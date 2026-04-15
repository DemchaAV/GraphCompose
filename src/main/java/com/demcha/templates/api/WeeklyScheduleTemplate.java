package com.demcha.templates.api;

import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.templates.data.WeeklyScheduleData;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.nio.file.Path;

/**
 * Compose-first contract for reusable weekly schedule templates.
 * <p>
 * Implementations should compose scene structure through
 * {@link DocumentComposer}. The deprecated {@code render(...)} overloads remain
 * as PDF convenience adapters.
 * </p>
 *
 * @deprecated Use {@link com.demcha.compose.document.templates.api.WeeklyScheduleTemplate}
 *             instead.
 */
@Deprecated(forRemoval = false)
public interface WeeklyScheduleTemplate {

    String getTemplateId();

    String getTemplateName();

    default String getDescription() {
        return "";
    }

    /**
     * Composes this template into the provided document composer.
     */
    void compose(DocumentComposer composer, WeeklyScheduleData data);

    /**
     * Convenience PDF adapter for callers that still want a {@link PDDocument}.
     * Prefer {@link #compose(DocumentComposer, WeeklyScheduleData)} for new
     * integrations.
     */
    @Deprecated(forRemoval = false)
    PDDocument render(WeeklyScheduleData data);

    /**
     * Convenience PDF adapter for callers that still want a {@link PDDocument}.
     * Prefer {@link #compose(DocumentComposer, WeeklyScheduleData)} for new
     * integrations.
     */
    @Deprecated(forRemoval = false)
    PDDocument render(WeeklyScheduleData data, boolean guideLines);

    /**
     * Convenience PDF adapter for callers that still want file output written by
     * the template itself. Prefer
     * {@link #compose(DocumentComposer, WeeklyScheduleData)} for new
     * integrations.
     */
    @Deprecated(forRemoval = false)
    void render(WeeklyScheduleData data, Path path);

    /**
     * Convenience PDF adapter for callers that still want file output written by
     * the template itself. Prefer
     * {@link #compose(DocumentComposer, WeeklyScheduleData)} for new
     * integrations.
     */
    @Deprecated(forRemoval = false)
    void render(WeeklyScheduleData data, Path path, boolean guideLines);
}
