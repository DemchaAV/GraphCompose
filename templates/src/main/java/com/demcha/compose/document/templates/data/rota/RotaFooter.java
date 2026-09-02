package com.demcha.compose.document.templates.data.rota;

import java.util.Objects;

/**
 * The standing note the rota closes with — the line about swaps, or notice, or
 * who to tell.
 *
 * @param note the note, as printed; blank when the rota closes with none
 * @since 2.4.0
 */
public record RotaFooter(String note) {

    /**
     * Normalizes optional fields.
     */
    public RotaFooter {
        note = Objects.requireNonNullElse(note, "");
    }

    /**
     * Whether there is anything to draw.
     *
     * @return {@code true} when the foot states a note
     */
    public boolean isPresent() {
        return !note.isBlank();
    }
}
