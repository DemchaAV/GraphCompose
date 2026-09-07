package com.demcha.compose.document.templates.data.rota;

import java.util.Objects;

/**
 * One day of the rota: its heading, whatever is happening on it, and how busy
 * it is expected to be.
 *
 * <p>The date is three fields rather than one because a heading sets them
 * differently: {@code MONDAY} large, {@code 31} beside it, and {@code ST}
 * smaller and raised. A preset that wants them joined can join them; one that
 * wants the suffix raised cannot split a string that arrived joined.</p>
 *
 * @param name          the weekday, as printed
 * @param ordinal       the day of the month, as printed
 * @param ordinalSuffix the ordinal's tail, cased as the sheet prints it —
 *                      {@code ST}, {@code nd} — which a design may set apart;
 *                      blank when it does not
 * @param note          what else is happening that day; blank when nothing is
 * @param covers        the day's expected covers; never {@code null}
 * @since 2.4.0
 */
public record RotaDay(String name, String ordinal, String ordinalSuffix, String note,
                      RotaCovers covers) {

    /**
     * Normalizes optional fields.
     */
    public RotaDay {
        name = Objects.requireNonNullElse(name, "");
        ordinal = Objects.requireNonNullElse(ordinal, "");
        ordinalSuffix = Objects.requireNonNullElse(ordinalSuffix, "");
        note = Objects.requireNonNullElse(note, "");
        covers = covers == null ? new RotaCovers("", "") : covers;
    }

    /**
     * A day with a heading and nothing else stated.
     *
     * @param name          the weekday
     * @param ordinal       the day of the month
     * @param ordinalSuffix the ordinal's tail
     */
    public RotaDay(String name, String ordinal, String ordinalSuffix) {
        this(name, ordinal, ordinalSuffix, "", null);
    }

}
