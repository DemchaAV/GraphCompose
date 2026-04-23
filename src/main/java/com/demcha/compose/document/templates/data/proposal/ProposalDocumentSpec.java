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

        public Builder title(String title) {
            proposal.title(title);
            return this;
        }

        public Builder proposalNumber(String proposalNumber) {
            proposal.proposalNumber(proposalNumber);
            return this;
        }

        public Builder preparedDate(String preparedDate) {
            proposal.preparedDate(preparedDate);
            return this;
        }

        public Builder validUntil(String validUntil) {
            proposal.validUntil(validUntil);
            return this;
        }

        public Builder projectTitle(String projectTitle) {
            proposal.projectTitle(projectTitle);
            return this;
        }

        public Builder executiveSummary(String executiveSummary) {
            proposal.executiveSummary(executiveSummary);
            return this;
        }

        public Builder sender(ProposalParty sender) {
            proposal.sender(sender);
            return this;
        }

        public Builder sender(Consumer<ProposalParty.Builder> spec) {
            proposal.sender(spec);
            return this;
        }

        public Builder recipient(ProposalParty recipient) {
            proposal.recipient(recipient);
            return this;
        }

        public Builder recipient(Consumer<ProposalParty.Builder> spec) {
            proposal.recipient(spec);
            return this;
        }

        public Builder sections(List<ProposalSection> sections) {
            proposal.sections(sections);
            return this;
        }

        public Builder addSection(ProposalSection section) {
            proposal.addSection(Objects.requireNonNull(section, "section"));
            return this;
        }

        public Builder section(Consumer<ProposalSection.Builder> spec) {
            proposal.section(spec);
            return this;
        }

        public Builder section(String title, String... paragraphs) {
            proposal.section(title, paragraphs);
            return this;
        }

        public Builder timeline(List<ProposalTimelineItem> timeline) {
            proposal.timeline(timeline);
            return this;
        }

        public Builder addTimelineItem(ProposalTimelineItem item) {
            proposal.addTimelineItem(Objects.requireNonNull(item, "item"));
            return this;
        }

        public Builder timelineItem(Consumer<ProposalTimelineItem.Builder> spec) {
            proposal.timelineItem(spec);
            return this;
        }

        public Builder timelineItem(String phase, String duration, String details) {
            proposal.timelineItem(phase, duration, details);
            return this;
        }

        public Builder pricingRows(List<ProposalPricingRow> pricingRows) {
            proposal.pricingRows(pricingRows);
            return this;
        }

        public Builder addPricingRow(ProposalPricingRow pricingRow) {
            proposal.addPricingRow(Objects.requireNonNull(pricingRow, "pricingRow"));
            return this;
        }

        public Builder pricingRow(Consumer<ProposalPricingRow.Builder> spec) {
            proposal.pricingRow(spec);
            return this;
        }

        public Builder pricingRow(String label, String description, String amount) {
            proposal.pricingRow(label, description, amount);
            return this;
        }

        public Builder emphasizedPricingRow(String label, String description, String amount) {
            proposal.emphasizedPricingRow(label, description, amount);
            return this;
        }

        public Builder acceptanceTerms(List<String> acceptanceTerms) {
            proposal.acceptanceTerms(acceptanceTerms);
            return this;
        }

        public Builder acceptanceTerm(String acceptanceTerm) {
            proposal.acceptanceTerm(acceptanceTerm);
            return this;
        }

        public Builder footerNote(String footerNote) {
            proposal.footerNote(footerNote);
            return this;
        }

        public ProposalDocumentSpec build() {
            return new ProposalDocumentSpec(proposal.build());
        }
    }
}
