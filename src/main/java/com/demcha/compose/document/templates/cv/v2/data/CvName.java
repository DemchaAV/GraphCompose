package com.demcha.compose.document.templates.cv.v2.data;

import java.util.Objects;
import java.util.Optional;

/**
 * Subject's legal name, split into first / last (required) and an
 * optional middle component.
 *
 * <p>Held as separate fields so consumers can render initials,
 * monogrammes, or alphabetical sort keys without re-parsing a joined
 * full-name string.</p>
 *
 * @param first  first name (required, non-blank)
 * @param middle optional middle name; never null, may be empty
 * @param last   family name (required, non-blank)
 */
public record CvName(String first, String middle, String last) {

    /**
     * Validates that {@code first} and {@code last} are non-null and
     * non-blank, and normalises a null {@code middle} to empty.
     */
    public CvName {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(last, "last");
        middle = middle == null ? "" : middle;
        if (first.isBlank()) {
            throw new IllegalArgumentException("first must not be blank");
        }
        if (last.isBlank()) {
            throw new IllegalArgumentException("last must not be blank");
        }
    }

    /**
     * Convenience constructor for the common first + last case.
     *
     * @param first first name (required, non-blank)
     * @param last  family name (required, non-blank)
     * @return a {@code CvName} with an empty middle component
     */
    public static CvName of(String first, String last) {
        return new CvName(first, "", last);
    }

    /**
     * Optional middle component, absent when blank.
     *
     * @return middle wrapped in {@link Optional#empty()} when blank,
     *         otherwise the middle name
     */
    public Optional<String> middleName() {
        return middle.isBlank() ? Optional.empty() : Optional.of(middle);
    }

    /**
     * Concatenates the present parts with single spaces in
     * first / middle / last order.
     *
     * @return the full name, middle part included only when present
     */
    public String full() {
        return middle.isBlank()
                ? first + " " + last
                : first + " " + middle + " " + last;
    }
}
