package com.demcha.compose.document.templates.data.proposal;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Display-oriented proposal document input.
 *
 * @param title display title
 * @param proposalNumber proposal number shown in the header
 * @param preparedDate proposal preparation date
 * @param validUntil optional validity date
 * @param projectTitle project title
 * @param executiveSummary summary paragraph
 * @param sender sender/provider party
 * @param recipient recipient/client party
 * @param sections proposal body sections
 * @param timeline timeline rows
 * @param pricingRows pricing rows
 * @param acceptanceTerms acceptance term rows
 * @param footerNote footer note rendered at the bottom
 */
public record ProposalData(
        String title,
        String proposalNumber,
        String preparedDate,
        String validUntil,
        String projectTitle,
        String executiveSummary,
        ProposalParty sender,
        ProposalParty recipient,
        List<ProposalSection> sections,
        List<ProposalTimelineItem> timeline,
        List<ProposalPricingRow> pricingRows,
        List<String> acceptanceTerms,
        String footerNote) {

    /**
     * Normalizes optional proposal fields and freezes collection inputs.
     */
    public ProposalData {
        title = Objects.requireNonNullElse(title, "Proposal");
        proposalNumber = Objects.requireNonNullElse(proposalNumber, "");
        preparedDate = Objects.requireNonNullElse(preparedDate, "");
        validUntil = Objects.requireNonNullElse(validUntil, "");
        projectTitle = Objects.requireNonNullElse(projectTitle, "");
        executiveSummary = Objects.requireNonNullElse(executiveSummary, "");
        sections = List.copyOf(Objects.requireNonNullElse(sections, List.of()));
        timeline = List.copyOf(Objects.requireNonNullElse(timeline, List.of()));
        pricingRows = List.copyOf(Objects.requireNonNullElse(pricingRows, List.of()));
        acceptanceTerms = List.copyOf(Objects.requireNonNullElse(acceptanceTerms, List.of()));
        footerNote = Objects.requireNonNullElse(footerNote, "");
    }

    /**
     * Starts a fluent proposal data builder.
     *
     * @return proposal data builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for complete proposal content.
     */
    public static final class Builder {
        private String title;
        private String proposalNumber;
        private String preparedDate;
        private String validUntil;
        private String projectTitle;
        private String executiveSummary;
        private ProposalParty sender;
        private ProposalParty recipient;
        private final List<ProposalSection> sections = new ArrayList<>();
        private final List<ProposalTimelineItem> timeline = new ArrayList<>();
        private final List<ProposalPricingRow> pricingRows = new ArrayList<>();
        private final List<String> acceptanceTerms = new ArrayList<>();
        private String footerNote;

        private Builder() {
        }

        /**
         * Sets the proposal title.
         *
         * @param title display title
         * @return this builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the proposal number.
         *
         * @param proposalNumber proposal number
         * @return this builder
         */
        public Builder proposalNumber(String proposalNumber) {
            this.proposalNumber = proposalNumber;
            return this;
        }

        /**
         * Sets the preparation date.
         *
         * @param preparedDate prepared date text
         * @return this builder
         */
        public Builder preparedDate(String preparedDate) {
            this.preparedDate = preparedDate;
            return this;
        }

        /**
         * Sets the validity date.
         *
         * @param validUntil valid-until date text
         * @return this builder
         */
        public Builder validUntil(String validUntil) {
            this.validUntil = validUntil;
            return this;
        }

        /**
         * Sets the project title.
         *
         * @param projectTitle project title
         * @return this builder
         */
        public Builder projectTitle(String projectTitle) {
            this.projectTitle = projectTitle;
            return this;
        }

        /**
         * Sets the executive summary paragraph.
         *
         * @param executiveSummary summary text
         * @return this builder
         */
        public Builder executiveSummary(String executiveSummary) {
            this.executiveSummary = executiveSummary;
            return this;
        }

        /**
         * Sets the sender/provider party.
         *
         * @param sender sender party
         * @return this builder
         */
        public Builder sender(ProposalParty sender) {
            this.sender = sender;
            return this;
        }

        /**
         * Builds and sets the sender/provider party.
         *
         * @param spec party builder callback
         * @return this builder
         */
        public Builder sender(Consumer<ProposalParty.Builder> spec) {
            ProposalParty.Builder builder = ProposalParty.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return sender(builder.build());
        }

        /**
         * Sets the recipient/client party.
         *
         * @param recipient recipient party
         * @return this builder
         */
        public Builder recipient(ProposalParty recipient) {
            this.recipient = recipient;
            return this;
        }

        /**
         * Builds and sets the recipient/client party.
         *
         * @param spec party builder callback
         * @return this builder
         */
        public Builder recipient(Consumer<ProposalParty.Builder> spec) {
            ProposalParty.Builder builder = ProposalParty.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return recipient(builder.build());
        }

        /**
         * Replaces all proposal sections.
         *
         * @param sections proposal sections
         * @return this builder
         */
        public Builder sections(List<ProposalSection> sections) {
            this.sections.clear();
            if (sections != null) {
                this.sections.addAll(sections);
            }
            return this;
        }

        /**
         * Appends a proposal section.
         *
         * @param section proposal section
         * @return this builder
         */
        public Builder addSection(ProposalSection section) {
            this.sections.add(section);
            return this;
        }

        /**
         * Builds and appends a proposal section.
         *
         * @param spec section builder callback
         * @return this builder
         */
        public Builder section(Consumer<ProposalSection.Builder> spec) {
            ProposalSection.Builder builder = ProposalSection.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return addSection(builder.build());
        }

        /**
         * Appends a proposal section from title and paragraph text.
         *
         * @param title section title
         * @param paragraphs section paragraphs
         * @return this builder
         */
        public Builder section(String title, String... paragraphs) {
            return addSection(ProposalSection.builder()
                    .title(title)
                    .paragraphs(paragraphs)
                    .build());
        }

        /**
         * Replaces all timeline rows.
         *
         * @param timeline timeline rows
         * @return this builder
         */
        public Builder timeline(List<ProposalTimelineItem> timeline) {
            this.timeline.clear();
            if (timeline != null) {
                this.timeline.addAll(timeline);
            }
            return this;
        }

        /**
         * Appends one timeline row.
         *
         * @param item timeline item
         * @return this builder
         */
        public Builder addTimelineItem(ProposalTimelineItem item) {
            this.timeline.add(item);
            return this;
        }

        /**
         * Builds and appends a timeline row.
         *
         * @param spec timeline builder callback
         * @return this builder
         */
        public Builder timelineItem(Consumer<ProposalTimelineItem.Builder> spec) {
            ProposalTimelineItem.Builder builder = ProposalTimelineItem.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return addTimelineItem(builder.build());
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
            return addTimelineItem(new ProposalTimelineItem(phase, duration, details));
        }

        /**
         * Replaces all pricing rows.
         *
         * @param pricingRows pricing rows
         * @return this builder
         */
        public Builder pricingRows(List<ProposalPricingRow> pricingRows) {
            this.pricingRows.clear();
            if (pricingRows != null) {
                this.pricingRows.addAll(pricingRows);
            }
            return this;
        }

        /**
         * Appends one pricing row.
         *
         * @param pricingRow pricing row
         * @return this builder
         */
        public Builder addPricingRow(ProposalPricingRow pricingRow) {
            this.pricingRows.add(pricingRow);
            return this;
        }

        /**
         * Builds and appends a pricing row.
         *
         * @param spec pricing row builder callback
         * @return this builder
         */
        public Builder pricingRow(Consumer<ProposalPricingRow.Builder> spec) {
            ProposalPricingRow.Builder builder = ProposalPricingRow.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return addPricingRow(builder.build());
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
            return addPricingRow(new ProposalPricingRow(label, description, amount, false));
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
            return addPricingRow(new ProposalPricingRow(label, description, amount, true));
        }

        /**
         * Replaces all acceptance terms.
         *
         * @param acceptanceTerms acceptance term rows
         * @return this builder
         */
        public Builder acceptanceTerms(List<String> acceptanceTerms) {
            this.acceptanceTerms.clear();
            if (acceptanceTerms != null) {
                this.acceptanceTerms.addAll(acceptanceTerms);
            }
            return this;
        }

        /**
         * Appends an acceptance term row.
         *
         * @param acceptanceTerm acceptance term text
         * @return this builder
         */
        public Builder acceptanceTerm(String acceptanceTerm) {
            this.acceptanceTerms.add(acceptanceTerm);
            return this;
        }

        /**
         * Sets the footer note.
         *
         * @param footerNote footer note text
         * @return this builder
         */
        public Builder footerNote(String footerNote) {
            this.footerNote = footerNote;
            return this;
        }

        /**
         * Builds immutable proposal data.
         *
         * @return proposal data
         */
        public ProposalData build() {
            return new ProposalData(
                    title,
                    proposalNumber,
                    preparedDate,
                    validUntil,
                    projectTitle,
                    executiveSummary,
                    sender,
                    recipient,
                    sections,
                    timeline,
                    pricingRows,
                    acceptanceTerms,
                    footerNote);
        }
    }
}
