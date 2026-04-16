package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.WeeklyScheduleTemplate;
import com.demcha.compose.document.templates.data.WeeklyScheduleData;
import com.demcha.compose.document.templates.support.LegacyTemplateMappers;
import com.demcha.compose.document.templates.support.LegacyTemplateSessionRenderer;
import com.demcha.compose.document.templates.theme.WeeklyScheduleTheme;

import java.util.Objects;

/**
 * Canonical V2 implementation of the weekly schedule template.
 */
public final class WeeklyScheduleTemplateV1 implements WeeklyScheduleTemplate {
    private final com.demcha.templates.builtins.WeeklyScheduleTemplateV1 legacyBridge;

    public WeeklyScheduleTemplateV1() {
        this(null);
    }

    public WeeklyScheduleTemplateV1(WeeklyScheduleTheme theme) {
        this.legacyBridge = new com.demcha.templates.builtins.WeeklyScheduleTemplateV1(
                LegacyTemplateMappers.toLegacy(Objects.requireNonNullElseGet(theme, WeeklyScheduleTheme::defaultTheme)));
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
    public void compose(DocumentSession document, WeeklyScheduleData data) {
        LegacyTemplateSessionRenderer.renderInto(document, composer -> legacyBridge.compose(
                composer,
                LegacyTemplateMappers.toLegacy(data)));
    }
}
