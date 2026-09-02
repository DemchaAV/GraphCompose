package com.demcha.compose.document.templates.data.rota;

import java.util.Objects;

/**
 * One entry in one cell of a rota: what it says, what it means, and how loudly
 * it says it.
 *
 * <p>The text is what the reader sees — a span of hours where the day is
 * worked, or the word for the status where it is not. Text and status are kept
 * apart because they answer different questions: two sites may print
 * {@code "A/L"} and {@code "HOL"} for the same {@link ShiftStatus#HOLIDAY}, and
 * a preset that colours by status keeps working for both.</p>
 *
 * @param text     what the cell prints
 * @param status   what the entry means; {@code null} normalizes to
 *                 {@link ShiftStatus#NONE}
 * @param emphasis how much of a mark it makes; {@code null} normalizes to
 *                 {@link ShiftEmphasis#PLAIN}
 * @since 2.4.0
 */
public record RotaShift(String text, ShiftStatus status, ShiftEmphasis emphasis) {

    /**
     * Normalizes optional fields.
     */
    public RotaShift {
        text = Objects.requireNonNullElse(text, "");
        status = Objects.requireNonNullElse(status, ShiftStatus.NONE);
        emphasis = Objects.requireNonNullElse(emphasis, ShiftEmphasis.PLAIN);
    }

    /**
     * A worked span of hours, printed as it stands and marked in no way.
     *
     * @param text the hours as the rota prints them
     * @return the shift
     */
    public static RotaShift hours(String text) {
        return new RotaShift(text, ShiftStatus.NONE, ShiftEmphasis.PLAIN);
    }

    /**
     * An entry that carries a status, drawn as loudly as the preset draws one.
     *
     * @param text   what the cell prints
     * @param status what the entry means
     * @return the shift
     */
    public static RotaShift strong(String text, ShiftStatus status) {
        return new RotaShift(text, status, ShiftEmphasis.STRONG);
    }

    /**
     * The same entry, drawn quietly.
     *
     * <p>There is no single {@code marked(text, status)} factory that picks the
     * emphasis for you: a design that halves a day draws the two halves
     * differently, and a factory that chose {@link ShiftEmphasis#STRONG} for
     * both would be wrong in a way only a pixel diff catches.</p>
     *
     * @param text   what the cell prints
     * @param status what the entry means
     * @return the shift
     */
    public static RotaShift soft(String text, ShiftStatus status) {
        return new RotaShift(text, status, ShiftEmphasis.SOFT);
    }
}
