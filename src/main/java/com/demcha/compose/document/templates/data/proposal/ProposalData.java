package com.demcha.compose.document.templates.data.proposal;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Display-oriented proposal document input.
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

    public static Builder builder() {
        return new Builder();
    }

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

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder proposalNumber(String proposalNumber) {
            this.proposalNumber = proposalNumber;
            return this;
        }

        public Builder preparedDate(String preparedDate) {
            this.preparedDate = preparedDate;
            return this;
        }

        public Builder validUntil(String validUntil) {
            this.validUntil = validUntil;
            return this;
        }

        public Builder projectTitle(String projectTitle) {
            this.projectTitle = projectTitle;
            return this;
        }

        public Builder executiveSummary(String executiveSummary) {
            this.executiveSummary = executiveSummary;
            return this;
        }

        public Builder sender(ProposalParty sender) {
            this.sender = sender;
            return this;
        }

        public Builder sender(Consumer<ProposalParty.Builder> spec) {
            ProposalParty.Builder builder = ProposalParty.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return sender(builder.build());
        }

        public Builder recipient(ProposalParty recipient) {
            this.recipient = recipient;
            return this;
        }

        public Builder recipient(Consumer<ProposalParty.Builder> spec) {
            ProposalParty.Builder builder = ProposalParty.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return recipient(builder.build());
        }

        public Builder sections(List<ProposalSection> sections) {
            this.sections.clear();
            if (sections != null) {
                this.sections.addAll(sections);
            }
            return this;
        }

        public Builder addSection(ProposalSection section) {
            this.sections.add(section);
            return this;
        }

        public Builder section(Consumer<ProposalSection.Builder> spec) {
            ProposalSection.Builder builder = ProposalSection.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return addSection(builder.build());
        }

        public Builder section(String title, String... paragraphs) {
            return addSection(ProposalSection.builder()
                    .title(title)
                    .paragraphs(paragraphs)
                    .build());
        }

        public Builder timeline(List<ProposalTimelineItem> timeline) {
            this.timeline.clear();
            if (timeline != null) {
                this.timeline.addAll(timeline);
            }
            return this;
        }

        public Builder addTimelineItem(ProposalTimelineItem item) {
            this.timeline.add(item);
            return this;
        }

        public Builder timelineItem(Consumer<ProposalTimelineItem.Builder> spec) {
            ProposalTimelineItem.Builder builder = ProposalTimelineItem.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return addTimelineItem(builder.build());
        }

        public Builder timelineItem(String phase, String duration, String details) {
            return addTimelineItem(new ProposalTimelineItem(phase, duration, details));
        }

        public Builder pricingRows(List<ProposalPricingRow> pricingRows) {
            this.pricingRows.clear();
            if (pricingRows != null) {
                this.pricingRows.addAll(pricingRows);
            }
            return this;
        }

        public Builder addPricingRow(ProposalPricingRow pricingRow) {
            this.pricingRows.add(pricingRow);
            return this;
        }

        public Builder pricingRow(Consumer<ProposalPricingRow.Builder> spec) {
            ProposalPricingRow.Builder builder = ProposalPricingRow.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return addPricingRow(builder.build());
        }

        public Builder pricingRow(String label, String description, String amount) {
            return addPricingRow(new ProposalPricingRow(label, description, amount, false));
        }

        public Builder emphasizedPricingRow(String label, String description, String amount) {
            return addPricingRow(new ProposalPricingRow(label, description, amount, true));
        }

        public Builder acceptanceTerms(List<String> acceptanceTerms) {
            this.acceptanceTerms.clear();
            if (acceptanceTerms != null) {
                this.acceptanceTerms.addAll(acceptanceTerms);
            }
            return this;
        }

        public Builder acceptanceTerm(String acceptanceTerm) {
            this.acceptanceTerms.add(acceptanceTerm);
            return this;
        }

        public Builder footerNote(String footerNote) {
            this.footerNote = footerNote;
            return this;
        }

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
