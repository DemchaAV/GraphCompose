package com.demcha.compose.document.templates.data.rota;

import java.util.List;
import java.util.Objects;

/**
 * The strip that tells a reader what the colours mean, and the labels on the
 * covers row beneath it.
 *
 * <p>The words are the document's and the colours are the preset's: an entry
 * pairs the word a site uses with the {@link ShiftStatus} it stands for, so a
 * rota that prints {@code A/L} and one that prints {@code HOL} both colour the
 * same.</p>
 *
 * @param label             the strip's own label; blank for a strip that
 *                          carries none
 * @param coversLabel       the label on the covers row
 * @param coversLunchLabel  the mark on the earlier service's count
 * @param coversDinnerLabel the mark on the later service's count
 * @param entries           the statuses the rota documents, in order
 * @since 2.4.0
 */
public record RotaLegend(String label, String coversLabel, String coversLunchLabel,
                         String coversDinnerLabel, List<Entry> entries) {

    /**
     * Normalizes optional fields and freezes the entry list.
     */
    public RotaLegend {
        label = Objects.requireNonNullElse(label, "");
        coversLabel = Objects.requireNonNullElse(coversLabel, "");
        coversLunchLabel = Objects.requireNonNullElse(coversLunchLabel, "");
        coversDinnerLabel = Objects.requireNonNullElse(coversDinnerLabel, "");
        entries = List.copyOf(Objects.requireNonNullElse(entries, List.of()));
    }

    /**
     * A legend with entries and no covers row.
     *
     * @param label   the strip's label
     * @param entries the statuses it documents
     */
    public RotaLegend(String label, List<Entry> entries) {
        this(label, "", "", "", entries);
    }

    /**
     * Whether there is anything to draw.
     *
     * @return {@code true} when the legend states a label or any entry
     */
    public boolean isPresent() {
        return !label.isBlank() || !entries.isEmpty();
    }

    /**
     * One swatch: the word a site prints, and the status whose colour it shows.
     *
     * @param label  the word, as printed
     * @param status the status it stands for; {@code null} normalizes to
     *               {@link ShiftStatus#NONE}
     */
    public record Entry(String label, ShiftStatus status) {

        /**
         * Normalizes optional fields.
         */
        public Entry {
            label = Objects.requireNonNullElse(label, "");
            status = Objects.requireNonNullElse(status, ShiftStatus.NONE);
        }
    }
}
