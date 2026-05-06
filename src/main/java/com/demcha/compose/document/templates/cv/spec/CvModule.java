package com.demcha.compose.document.templates.cv.spec;

import com.demcha.compose.document.templates.blocks.Block;

import java.util.Objects;

/**
 * One named section of a {@link CvSpec} — heading title plus body
 * {@link Block}.
 *
 * <p>The {@code name} is the lookup key a preset references in its
 * slot placement (e.g.
 * {@code .place(SingleColumn.MAIN, "Professional Summary", ...)}).
 * The {@code title} is the rendered heading text — typically the same
 * as {@code name} but allowed to differ for localisation or
 * presentation overrides.</p>
 *
 * @param name semantic identifier used by presets to reference this
 *             module in slot placements (must not be blank)
 * @param title heading text rendered above the body (may be empty to
 *              suppress the heading row)
 * @param body  body content block (must not be null)
 */
public record CvModule(String name, String title, Block body) {

    /**
     * Compact constructor that rejects null and blank-name modules.
     *
     * @throws NullPointerException     if any field is null
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public CvModule {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    /**
     * Convenience factory that uses the same string for both
     * {@code name} and {@code title}.
     *
     * @param nameAndTitle non-blank module name (also used as heading)
     * @param body         body content block
     * @return new CV module
     */
    public static CvModule of(String nameAndTitle, Block body) {
        return new CvModule(nameAndTitle, nameAndTitle, body);
    }
}
