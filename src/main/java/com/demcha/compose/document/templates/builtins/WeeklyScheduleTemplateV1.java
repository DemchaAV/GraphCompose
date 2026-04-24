package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.WeeklyScheduleTemplate;
import com.demcha.compose.document.templates.data.schedule.WeeklyScheduleDocumentSpec;
import com.demcha.compose.document.templates.support.common.SessionTemplateComposeTarget;
import com.demcha.compose.document.templates.support.common.TemplateLifecycleLog;
import com.demcha.compose.document.templates.support.schedule.WeeklyScheduleTemplateComposer;
import com.demcha.compose.document.templates.theme.WeeklyScheduleTheme;

import java.util.Objects;

/**
 * Canonical V2 implementation of the weekly schedule template.
 */
public final class WeeklyScheduleTemplateV1 implements WeeklyScheduleTemplate {
    private final WeeklyScheduleTemplateComposer composer;

    public WeeklyScheduleTemplateV1() {
        this(null);
    }

    public WeeklyScheduleTemplateV1(WeeklyScheduleTheme theme) {
        this.composer = new WeeklyScheduleTemplateComposer(
                Objects.requireNonNullElseGet(theme, WeeklyScheduleTheme::defaultTheme));
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
    public void compose(DocumentSession document, WeeklyScheduleDocumentSpec spec) {
        long startNanos = TemplateLifecycleLog.start(getTemplateId(), spec);
        try {
            composer.compose(new SessionTemplateComposeTarget(document), spec);
            TemplateLifecycleLog.success(getTemplateId(), spec, startNanos);
        } catch (RuntimeException | Error ex) {
            TemplateLifecycleLog.failure(getTemplateId(), spec, startNanos, ex);
            throw ex;
        }
    }
}
