package com.demcha.compose.document.templates.cv.v2.data;

/**
 * Visual decoration toggle for a {@link RowsSection}. Selects how
 * each {@link CvRow} is laid out — bullet on/off, inline vs stacked.
 *
 * <p>This is the orthogonal "decoration" axis the v2 model factors
 * out of section types so that bulleted-skills, plain Additional
 * Information, and stacked Projects all share one {@link RowsSection}
 * record.</p>
 */
public enum RowStyle {

    /**
     * No bullet glyph. The label renders bold inline with a trailing
     * colon, followed by the body on the same line: <br>
     * {@code <b>Languages:</b> English (Fluent), German}.
     * Used by Additional Information style content.
     */
    PLAIN,

    /**
     * Bullet glyph + bold label + colon + body, inline: <br>
     * {@code • <b>Languages:</b> Java 21, Kotlin}.
     * Used by Technical Skills.
     */
    BULLETED,

    /**
     * Bullet glyph + bold label on line one, body indented on line
     * two: <br>
     * {@code • <b>Project name (tech stack)</b>} <br>
     * {@code   Description text wrapping under the project name}.
     * Used by Projects-style sections where the description is too
     * long to fit inline.
     */
    BULLETED_STACKED
}
