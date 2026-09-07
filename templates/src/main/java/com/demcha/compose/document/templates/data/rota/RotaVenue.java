package com.demcha.compose.document.templates.data.rota;

import java.util.Objects;

/**
 * Who the rota is for: the mark it opens with and the name it signs off with.
 *
 * @param wordmark    the name as the lockup sets it
 * @param wordmarkSub the line under it — a site, a branch, a department; blank
 *                    when the lockup is one line
 * @param footerName  the name the foot repeats; blank when the foot names none
 * @since 2.4.0
 */
public record RotaVenue(String wordmark, String wordmarkSub, String footerName) {

    /**
     * Normalizes optional fields.
     */
    public RotaVenue {
        wordmark = Objects.requireNonNullElse(wordmark, "");
        wordmarkSub = Objects.requireNonNullElse(wordmarkSub, "");
        footerName = Objects.requireNonNullElse(footerName, "");
    }

    /**
     * Whether there is anything to draw.
     *
     * @return {@code true} when the venue states any of its three parts
     */
    public boolean isPresent() {
        return !wordmark.isBlank() || !wordmarkSub.isBlank() || !footerName.isBlank();
    }
}
