package com.demcha.compose.document.templates.data.proposal;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Public compose-first proposal input.
 *
 * <p><b>Authoring role:</b> gives reusable proposal templates one stable
 * document-level object while preserving a natural business vocabulary:
 * project title, parties, sections, timeline, pricing, acceptance terms.</p>
 *
 * @param proposal normalized proposal content rendered by proposal templates
 * @author Artem Demchyshyn
 */
public record ProposalDocumentSpec(ProposalData proposal) {

    /**
     * Creates a normalized proposal document spec.
     */
    public ProposalDocumentSpec {
        proposal = proposal == null ? ProposalData.builder().build() : proposal;
    }

    /**
     * Wraps existing proposal data in the document-level spec expected by
     * canonical templates.
     *
     * @param proposal proposal data
     * @return document spec
     */
    public static ProposalDocumentSpec from(ProposalData proposal) {
        return new ProposalDocumentSpec(proposal);
    }

    /**
     * Starts a fluent proposal document builder.
     *
     * @return document builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for proposal document specs.
     */
    public static final class Builder {
        private final ProposalData.Builder proposal = ProposalData.builder();

        private Builder() {
        }

        /**
         * Sets the proposal title.
         *
         * @param title display title
         * @return this builder
         */
        public Builder title(String title) {
            proposal.title(title);
            return this;
        }

        /**
         * Sets the proposal number.
         *
         * @param proposalNumber proposal number
         * @return this builder
         */
        public Builder proposalNumber(String proposalNumber) {
            proposal.proposalNumber(proposalNumber);
            return this;
        }

        /**
         * Sets the preparation date.
         *
         * @param preparedDate prepared date text
         * @return this builder
         */
        public Builder preparedDate(String preparedDate) {
            proposal.preparedDate(preparedDate);
            return this;
        }

        /**
         * Sets the validity date.
         *
         * @param validUntil valid-until date text
         * @return this builder
         */
        public Builder validUntil(String validUntil) {
            proposal.validUntil(validUntil);
            return this;
        }

        /**
         * Sets the project title.
         *
         * @param projectTitle project title
         * @return this builder
         */
        public Builder projectTitle(String projectTitle) {
            proposal.projectTitle(projectTitle);
            return this;
        }

        /**
         * Sets the executive summary paragraph.
         *
         * @param executiveSummary summary text
         * @return this builder
         */
        public Builder executiveSummary(String executiveSummary) {
            proposal.executiveSummary(executiveSummary);
            return this;
        }

        /**
         * Sets the sender/provider party.
         *
         * @param sender sender party
         * @return this builder
         */
        public Builder sender(ProposalParty sender) {
            proposal.sender(sender);
            return this;
        }

        /**
         * Builds and sets the sender/provider party.
         *
         * @param spec party builder callback
         * @return this builder
         */
        public Builder sender(Consumer<ProposalParty.Builder> spec) {
            proposal.sender(spec);
            return this;
        }

        /**
         * Sets the recipient/client party.
         *
         * @param recipient recipient party
         * @return this builder
         */
        public Builder recipient(ProposalParty recipient) {
            proposal.recipient(recipient);
            return this;
        }

        /**
         * Builds and sets the recipient/client party.
         *
         * @param spec party builder callback
         * @return this builder
         */
        public Builder recipient(Consumer<ProposalParty.Builder> spec) {
            proposal.recipient(spec);
            return this;
        }

        /**
         * Replaces all proposal sections.
         *
         * @param sections proposal sections
         * @return this builder
         */
        public Builder sections(List<ProposalSection> sections) {
            proposal.sections(sections);
            return this;
        }

        /**
         * Appends a proposal section.
         *
         * @param section proposal section
         * @return this builder
         */
        public Builder addSection(ProposalSection section) {
            proposal.addSection(Objects.requireNonNull(section, "section"));
            return this;
        }

        /**
         * Builds and appends a proposal section.
         *
         * @param spec section builder callback
         * @return this builder
         */
        public Builder section(Consumer<ProposalSection.Builder> spec) {
            proposal.section(spec);
            return this;
        }

        /**
         * Appends a proposal section from title and paragraphs.
         *
         * @param title section title
         * @param paragraphs section paragraphs
         * @return this builder
         */
        public Builder section(String title, String... paragraphs) {
            proposal.section(title, paragraphs);
            return this;
        }

        /**
         * Replaces all timeline rows.
         *
         * @param timeline timeline rows
         * @return this builder
         */
        public Builder timeline(List<ProposalTimelineItem> timeline) {
            proposal.timeline(timeline);
            return this;
        }

        /**
         * Appends a timeline row.
         *
         * @param item timeline item
         * @return this builder
         */
        public Builder addTimelineItem(ProposalTimelineItem item) {
            proposal.addTimelineItem(Objects.requireNonNull(item, "item"));
            return this;
        }

        /**
         * Builds and appends a timeline row.
         *
         * @param spec timeline item builder callback
         * @return this builder
         */
        public Builder timelineItem(Consumer<ProposalTimelineItem.Builder> spec) {
            proposal.timelineItem(spec);
            return this;
        }

        /**
         * Appends a timeline row from display values.
         *
         * @param phase phase label
         * @param duration duration text
         * @param details supporting details
         * @return this builder
         */
        public Builder timelineItem(String phase, String duration, String details) {
            proposal.timelineItem(phase, duration, details);
            return this;
        }

        /**
         * Replaces all pricing rows.
         *
         * @param pricingRows pricing rows
         * @return this builder
         */
        public Builder pricingRows(List<ProposalPricingRow> pricingRows) {
            proposal.pricingRows(pricingRows);
            return this;
        }

        /**
         * Appends a pricing row.
         *
         * @param pricingRow pricing row
         * @return this builder
         */
        public Builder addPricingRow(ProposalPricingRow pricingRow) {
            proposal.addPricingRow(Objects.requireNonNull(pricingRow, "pricingRow"));
            return this;
        }

        /**
         * Builds and appends a pricing row.
         *
         * @param spec pricing row builder callback
         * @return this builder
         */
        public Builder pricingRow(Consumer<ProposalPricingRow.Builder> spec) {
            proposal.pricingRow(spec);
            return this;
        }

        /**
         * Appends a normal pricing row.
         *
         * @param label row label
         * @param description row description
         * @param amount amount text
         * @return this builder
         */
        public Builder pricingRow(String label, String description, String amount) {
            proposal.pricingRow(label, description, amount);
            return this;
        }

        /**
         * Appends an emphasized pricing row.
         *
         * @param label row label
         * @param description row description
         * @param amount amount text
         * @return this builder
         */
        public Builder emphasizedPricingRow(String label, String description, String amount) {
            proposal.emphasizedPricingRow(label, description, amount);
            return this;
        }

        /**
         * Replaces all acceptance terms.
         *
         * @param acceptanceTerms acceptance term rows
         * @return this builder
         */
        public Builder acceptanceTerms(List<String> acceptanceTerms) {
            proposal.acceptanceTerms(acceptanceTerms);
            return this;
        }

        /**
         * Appends an acceptance term.
         *
         * @param acceptanceTerm acceptance term text
         * @return this builder
         */
        public Builder acceptanceTerm(String acceptanceTerm) {
            proposal.acceptanceTerm(acceptanceTerm);
            return this;
        }

        /**
         * Sets the footer note.
         *
         * @param footerNote footer note text
         * @return this builder
         */
        public Builder footerNote(String footerNote) {
            proposal.footerNote(footerNote);
            return this;
        }

        /**
         * Builds the proposal document spec.
         *
         * @return proposal document spec
         */
        public ProposalDocumentSpec build() {
            return new ProposalDocumentSpec(proposal.build());
        }
    }
}
