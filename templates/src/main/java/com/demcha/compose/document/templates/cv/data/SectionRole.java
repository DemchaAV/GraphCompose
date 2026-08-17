package com.demcha.compose.document.templates.cv.data;

/**
 * What a {@link ModuleSection} <em>means</em>, stated by the author
 * instead of guessed from its heading.
 *
 * <p>Multi-column presets have to decide what belongs in a sidebar,
 * and until now they decided it by matching the section's title
 * against a list of English keywords each preset kept privately. A CV
 * whose headings read {@code "Ausbildung"} or {@code "Навыки"} matched
 * nothing, and a heading nobody anticipated was placed by whatever the
 * preset does with leftovers. The role carries that decision in the
 * data, where the author already knows the answer.</p>
 *
 * <p>It is deliberately separate from {@link CvKind}: the role says
 * what a section is, the kind says how it draws. A "Volunteering"
 * module shaped exactly like Education is
 * {@code role = OTHER, kind = ENTRIES_DATED} — a combination no single
 * enum could express without one constant per pairing.</p>
 *
 * <p>{@link #OTHER} is the honest default and is never a second-class
 * citizen: a preset that cannot place it by role falls back to the
 * heading the author wrote, in document order.</p>
 *
 * @since 2.3.0
 */
public enum SectionRole {

    /** Profile, objective, professional summary — the opening prose. */
    SUMMARY,

    /** Employment history. */
    EXPERIENCE,

    /** Degrees, certifications, courses. */
    EDUCATION,

    /** Technical or professional skills, however they are grouped. */
    SKILLS,

    /** Personal or professional projects. */
    PROJECTS,

    /** Spoken languages and proficiency. */
    LANGUAGES,

    /**
     * Anything else — awards, volunteering, publications, interests,
     * references, a section this catalogue has no name for. Carries no
     * placement hint, so presets fall back to the author's own
     * heading.
     */
    OTHER
}
