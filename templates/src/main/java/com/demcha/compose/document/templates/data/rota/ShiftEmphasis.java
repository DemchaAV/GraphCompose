package com.demcha.compose.document.templates.data.rota;

/**
 * How much of a mark a shift makes on the cell it sits in.
 *
 * <p>A rota is read across a row at a glance, so not every entry can shout. The
 * document says which ones should: the day someone is off is the thing a reader
 * scans for, the hours they work are the thing they read once they have found
 * the person.</p>
 *
 * @since 2.4.0
 */
public enum ShiftEmphasis {

    /** The loudest form the preset has — a solid mark in the status's colour. */
    STRONG,

    /**
     * A quieter form of the same mark. Which entry gets it is the document's
     * decision and not its position: a split day is often loud then quiet, but
     * the reverse happens too, and so does loud twice.
     */
    SOFT,

    /** No mark at all: the text alone, on the cell. */
    PLAIN
}
