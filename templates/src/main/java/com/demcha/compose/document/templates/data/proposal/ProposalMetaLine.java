package com.demcha.compose.document.templates.data.proposal;

import java.util.List;
import java.util.Objects;

/**
 * A proposal's header facts.
 *
 * <p>Two designs' worth of shape, and they are not the same statement. A running
 * header names three things by role — who it is for, who prepared it, when — and
 * {@link #preparedFor()}, {@link #preparedBy()} and {@link #date()} carry those.
 * A sales proposal instead sets a row of marked tiles whose captions are the
 * document's own — a proposal number, a date, a validity, a team — and
 * {@link #entries()} carries those.</p>
 *
 * <p>Neither derives from the other: turning three roles into tiles would mean
 * inventing their captions, and reading roles back out of arbitrary tiles would
 * mean guessing which is which. A preset draws the one its design has, and a
 * document states the one it is written for.</p>
 *
 * @param preparedFor who the proposal is for, as a running header states it
 * @param preparedBy  who prepared it
 * @param date        when it was prepared
 * @param entries     the marked tiles a sales proposal sets instead; empty when
 *                    the design has none
 */
public record ProposalMetaLine(String preparedFor, String preparedBy, String date,
                               List<Entry> entries) {

    /**
     * Normalizes optional fields.
     */
    public ProposalMetaLine {
        preparedFor = Objects.requireNonNullElse(preparedFor, "");
        preparedBy = Objects.requireNonNullElse(preparedBy, "");
        date = Objects.requireNonNullElse(date, "");
        entries = List.copyOf(Objects.requireNonNullElse(entries, List.of()));
    }

    /**
     * Backward-compatible constructor for callers that predate the tiles.
     *
     * @param preparedFor who the proposal is for
     * @param preparedBy  who prepared it
     * @param date        when it was prepared
     */
    public ProposalMetaLine(String preparedFor, String preparedBy, String date) {
        this(preparedFor, preparedBy, date, List.of());
    }

    /**
     * The tiles alone, for a design that sets no running header.
     *
     * @param entries the marked tiles, in order
     */
    public ProposalMetaLine(List<Entry> entries) {
        this("", "", "", entries);
    }

    /**
     * One marked tile.
     *
     * @param icon  the mark the tile opens with; the token means something only
     *              to the preset that packages it, and a preset that draws no
     *              marks ignores it. Blank when absent
     * @param label the caption, as the sheet prints it
     * @param value the fact itself
     */
    public record Entry(String icon, String label, String value) {

        /**
         * Normalizes optional fields.
         */
        public Entry {
            icon = Objects.requireNonNullElse(icon, "");
            label = Objects.requireNonNullElse(label, "");
            value = Objects.requireNonNullElse(value, "");
        }
    }
}
