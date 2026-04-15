package com.demcha.compose.document.templates.api;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.data.WeeklyScheduleData;

/**
 * Canonical compose contract for reusable weekly schedule templates.
 */
public interface WeeklyScheduleTemplate {

    String getTemplateId();

    String getTemplateName();

    default String getDescription() {
        return "";
    }

    /**
     * Composes a weekly schedule into a live document session.
     *
     * @param document active mutable document session receiving template nodes
     * @param data weekly schedule data
     */
    void compose(DocumentSession document, WeeklyScheduleData data);
}
