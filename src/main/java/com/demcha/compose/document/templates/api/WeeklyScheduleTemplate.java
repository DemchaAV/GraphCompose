package com.demcha.compose.document.templates.api;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.data.schedule.WeeklyScheduleData;

/**
 * Canonical compose contract for reusable weekly schedule templates.
 *
 * <p><b>Responsibility:</b> define one reusable planning/schedule scene that
 * composes against a live {@link DocumentSession}.</p>
 *
 * <pre>{@code
 * WeeklyScheduleTemplate template = new WeeklyScheduleTemplateV1();
 * WeeklyScheduleData schedule = WeeklyScheduleData.builder()
 *         .title("Engineering Roster")
 *         .weekLabel("Week Of 20 Apr - 26 Apr 2026")
 *         .day("mon", "Monday", "Release prep", "delivery")
 *         .category("delivery", "DELIVERY", new Color(0, 173, 76), new Color(0, 110, 49))
 *         .person("artem", "ARTEM", 10)
 *         .assignment("artem", "mon", "delivery", ScheduleSlot.of("09:00", "17:00"))
 *         .build();
 *
 * try (DocumentSession document = GraphCompose.document(Path.of("schedule.pdf")).create()) {
 *     template.compose(document, schedule);
 *     document.buildPdf();
 * }
 * }</pre>
 */
public interface WeeklyScheduleTemplate {

    /**
     * Stable public template identifier.
     *
     * @return unique template id used by registries and integrations
     */
    String getTemplateId();

    /**
     * Human-readable display name.
     *
     * @return template display name
     */
    String getTemplateName();

    /**
     * Optional human-readable description.
     *
     * @return template description, or an empty string when omitted
     */
    default String getDescription() {
        return "";
    }

    /**
     * Composes a weekly schedule into a live document session.
     *
     * @param document active mutable document session receiving template nodes
     * @param data weekly schedule data
     * @throws NullPointerException if an implementation requires non-null inputs
     */
    void compose(DocumentSession document, WeeklyScheduleData data);
}
