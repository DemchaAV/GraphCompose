package com.demcha.compose.document.templates.proposal.spec;

import java.util.List;
import java.util.Objects;

/**
 * User-facing data record for a proposal in Templates v2.
 *
 * <p>Simplified compared to the legacy {@code ProposalData} —
 * captures the essentials (title, parties, content sections, pricing
 * rows) without the cinematic-presentation flags.</p>
 *
 * @param title       proposal title (required)
 * @param fromParty   issuing party (required)
 * @param toParty     receiving party (required)
 * @param sections    ordered content sections (heading + body)
 * @param pricingRows ordered pricing rows (label + value)
 * @param footerNote  optional closing line (may be empty)
 */
public record ProposalSpec(
        String title,
        Party fromParty,
        Party toParty,
        List<Section> sections,
        List<PricingRow> pricingRows,
        String footerNote) {

    /**
     * Compact constructor that defensively copies lists and
     * normalises null strings.
     *
     * @throws NullPointerException     if a required field is null
     * @throws IllegalArgumentException if {@code title} is blank
     */
    public ProposalSpec {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(fromParty, "fromParty");
        Objects.requireNonNull(toParty, "toParty");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        sections = sections == null ? List.of() : List.copyOf(sections);
        pricingRows = pricingRows == null ? List.of() : List.copyOf(pricingRows);
        footerNote = footerNote == null ? "" : footerNote;
    }

    /**
     * One proposal party (sender or recipient).
     *
     * @param name  legal / display name
     * @param email contact email (may be empty)
     * @param phone contact phone (may be empty)
     */
    public record Party(String name, String email, String phone) {

        /**
         * Compact constructor that normalises null strings and
         * rejects a blank name.
         *
         * @throws NullPointerException     if {@code name} is null
         * @throws IllegalArgumentException if {@code name} is blank
         */
        public Party {
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) {
                throw new IllegalArgumentException("name must not be blank");
            }
            email = email == null ? "" : email;
            phone = phone == null ? "" : phone;
        }
    }

    /**
     * One content section in the proposal.
     *
     * @param heading section heading
     * @param body    body text (may carry markdown markers)
     */
    public record Section(String heading, String body) {

        /**
         * Compact constructor that normalises null strings.
         */
        public Section {
            heading = heading == null ? "" : heading;
            body = body == null ? "" : body;
        }
    }

    /**
     * One pricing row — label + value pair appearing in the pricing
     * summary block.
     *
     * @param label       row label (e.g. "Discovery workshop")
     * @param value       row value (e.g. "GBP 1,450")
     * @param isHeadline  whether this row is the headline total
     *                    (rendered emphasised by the preset)
     */
    public record PricingRow(String label, String value, boolean isHeadline) {

        /**
         * Compact constructor that normalises null strings.
         */
        public PricingRow {
            label = label == null ? "" : label;
            value = value == null ? "" : value;
        }

        /**
         * Convenience factory for a non-headline row.
         *
         * @param label row label
         * @param value row value
         * @return pricing row with isHeadline = false
         */
        public static PricingRow of(String label, String value) {
            return new PricingRow(label, value, false);
        }

        /**
         * Convenience factory for the headline total row.
         *
         * @param label row label
         * @param value row value
         * @return pricing row with isHeadline = true
         */
        public static PricingRow headline(String label, String value) {
            return new PricingRow(label, value, true);
        }
    }

    /**
     * Returns a fluent builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Mutable builder for {@link ProposalSpec}.
     */
    public static final class Builder {
        private String title;
        private Party fromParty;
        private Party toParty;
        private final java.util.List<Section> sections = new java.util.ArrayList<>();
        private final java.util.List<PricingRow> pricingRows = new java.util.ArrayList<>();
        private String footerNote = "";

        private Builder() {
        }

        /**
         * Sets the proposal title.
         *
         * @param value title
         * @return this builder
         */
        public Builder title(String value) { this.title = value; return this; }
        /**
         * Sets the sender party.
         *
         * @param value sender party
         * @return this builder
         */
        public Builder fromParty(Party value) { this.fromParty = value; return this; }
        /**
         * Sets the receiving party.
         *
         * @param value receiving party
         * @return this builder
         */
        public Builder toParty(Party value) { this.toParty = value; return this; }
        /**
         * Adds a content section.
         *
         * @param section content section
         * @return this builder
         */
        public Builder section(Section section) { this.sections.add(section); return this; }
        /**
         * Adds a pricing row.
         *
         * @param row pricing row
         * @return this builder
         */
        public Builder pricingRow(PricingRow row) { this.pricingRows.add(row); return this; }
        /**
         * Sets the footer note.
         *
         * @param note footer note
         * @return this builder
         */
        public Builder footerNote(String note) { this.footerNote = note == null ? "" : note; return this; }

        /**
         * Builds an immutable {@link ProposalSpec}.
         *
         * @return new proposal spec
         */
        public ProposalSpec build() {
            return new ProposalSpec(title, fromParty, toParty,
                    sections, pricingRows, footerNote);
        }
    }
}
