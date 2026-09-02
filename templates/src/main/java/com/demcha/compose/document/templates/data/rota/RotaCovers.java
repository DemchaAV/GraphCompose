package com.demcha.compose.document.templates.data.rota;

import java.util.Objects;

/**
 * How many people a day is expecting, split across the two services.
 *
 * <p>Two fields and not one string. The counts arrive from the booking system
 * as a pair, and a rota that prints {@code "104 / 50"} leaves the reader to know
 * from somewhere else which half is which. Split, the preset can label them.</p>
 *
 * @param lunch  the earlier service's count, as printed
 * @param dinner the later service's count, as printed
 * @since 2.4.0
 */
public record RotaCovers(String lunch, String dinner) {

    /**
     * Normalizes optional fields.
     */
    public RotaCovers {
        lunch = Objects.requireNonNullElse(lunch, "");
        dinner = Objects.requireNonNullElse(dinner, "");
    }

    /**
     * Whether the day states either count.
     *
     * @return {@code true} when there is anything to print
     */
    public boolean isPresent() {
        return !lunch.isBlank() || !dinner.isBlank();
    }
}
