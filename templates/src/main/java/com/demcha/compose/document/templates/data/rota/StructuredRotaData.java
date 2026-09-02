package com.demcha.compose.document.templates.data.rota;

import java.util.List;

/**
 * Display-oriented input for a staff rota: who works when, over one span of
 * days.
 *
 * <p>The shape is a grid the document owns rather than one the preset infers.
 * {@link #days()} is the columns — the sheet's days, in order — and every
 * {@link RotaStaff#days()} runs in that same order, so a cell is found by
 * position and a rota of five days is as ordinary as one of seven. Nothing here
 * says a week.</p>
 *
 * <p>Every component normalizes {@code null} to its empty form and freezes its
 * collections, so a partial rota composes without null checks in preset code.
 * The section records construct positionally; this builder is where a document
 * is assembled.</p>
 *
 * @param venue  who the rota is for
 * @param week   which span it covers
 * @param days   the day columns, in the order they are printed
 * @param legend what the marks mean, and the labels on the covers row
 * @param groups the bands of staff, in the order they are printed
 * @param footer the standing note the sheet closes with
 * @since 2.4.0
 */
public record StructuredRotaData(
        RotaVenue venue,
        RotaWeek week,
        List<RotaDay> days,
        RotaLegend legend,
        List<RotaGroup> groups,
        RotaFooter footer) {

    /**
     * Normalizes absent components to their empty forms and freezes the lists.
     */
    public StructuredRotaData {
        venue = venue == null ? new RotaVenue(null, null, null) : venue;
        week = week == null ? new RotaWeek(null, null) : week;
        days = List.copyOf(days == null ? List.<RotaDay>of() : days);
        legend = legend == null ? new RotaLegend(null, null, null, null, null) : legend;
        groups = List.copyOf(groups == null ? List.<RotaGroup>of() : groups);
        footer = footer == null ? new RotaFooter(null) : footer;
    }

    /**
     * Starts a fluent rota data builder.
     *
     * @return rota data builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for complete rota content.
     */
    public static final class Builder {

        private RotaVenue venue;
        private RotaWeek week;
        private List<RotaDay> days = List.of();
        private RotaLegend legend;
        private List<RotaGroup> groups = List.of();
        private RotaFooter footer;

        private Builder() {
        }

        /**
         * Sets who the rota is for.
         *
         * @param venue the venue block
         * @return this builder
         */
        public Builder venue(RotaVenue venue) {
            this.venue = venue;
            return this;
        }

        /**
         * Sets which span the rota covers.
         *
         * @param week the week block
         * @return this builder
         */
        public Builder week(RotaWeek week) {
            this.week = week;
            return this;
        }

        /**
         * Sets the day columns.
         *
         * @param days the days, in the order they are printed
         * @return this builder
         */
        public Builder days(List<RotaDay> days) {
            this.days = days;
            return this;
        }

        /**
         * Sets what the marks mean.
         *
         * @param legend the legend block
         * @return this builder
         */
        public Builder legend(RotaLegend legend) {
            this.legend = legend;
            return this;
        }

        /**
         * Sets the bands of staff.
         *
         * @param groups the bands, in the order they are printed
         * @return this builder
         */
        public Builder groups(List<RotaGroup> groups) {
            this.groups = groups;
            return this;
        }

        /**
         * Sets the standing note the sheet closes with.
         *
         * @param footer the footer block
         * @return this builder
         */
        public Builder footer(RotaFooter footer) {
            this.footer = footer;
            return this;
        }

        /**
         * Builds normalized rota data.
         *
         * @return rota data
         */
        public StructuredRotaData build() {
            return new StructuredRotaData(venue, week, days, legend, groups, footer);
        }
    }
}
